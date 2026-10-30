package com.pulse.player.playback

import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import android.os.SystemClock
import androidx.annotation.OptIn
import androidx.core.content.ContextCompat
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.random.Random

@OptIn(UnstableApi::class)
class PlayerController(
    private val ctx: Context,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main)
) {
    private var mc: MediaController? = null
    private var future: ListenableFuture<MediaController>? = null
    private var tickJob: Job? = null
    private var sleepJob: Job? = null
    private var reconnectJob: Job? = null
    private var snapshotDebounceJob: Job? = null
    private var attempts = 0

    private var queueModel = QueueModel(emptyList(), null)
    private val itemCache = mutableMapOf<String, MediaItem>()

    var onSnapshotChanged: ((QueueSnapshot) -> Unit)? = null

    private val _state = MutableStateFlow(PlaybackUiState())
    val state = _state.asStateFlow()

    private val _pos = MutableStateFlow(0L)
    val position = _pos.asStateFlow()

    private val _queue = MutableStateFlow<List<Track>>(emptyList())
    val queue = _queue.asStateFlow()

    init {
        scope.launch {
            PlaybackEngine.errors.current.collect { err ->
                _state.update { it.copy(trackError = err) }
            }
        }
        attach()
    }

    fun attach() {
        if (future != null) return
        val token = SessionToken(ctx, ComponentName(ctx, MusicPlaybackService::class.java))
        val f = MediaController.Builder(ctx, token).setListener(object : MediaController.Listener {
            override fun onDisconnected(controller: MediaController) {
                onLost()
            }
        }).buildAsync()
        future = f
        f.addListener({
            runCatching { f.get() }.onSuccess { c ->
                if (future === f) {
                    mc = c
                    attempts = 0
                    c.addListener(listener)
                    sync(c)
                    updateTicker(c.isPlaying)
                } else {
                    c.release()
                }
            }.onFailure {
                if (future === f) onLost()
            }
        }, ContextCompat.getMainExecutor(ctx))
    }

    fun detach() {
        reconnectJob?.cancel()
        tickJob?.cancel()
        snapshotDebounceJob?.cancel()
        mc?.removeListener(listener)
        future?.let { MediaController.releaseFuture(it) }
        future = null
        mc = null
    }

    private fun onLost() {
        detach()
        scheduleReconnect()
    }

    private fun scheduleReconnect() {
        if (attempts >= 5) {
            _state.update { it.copy(error = "Lost connection to the player. Reopen the app.") }
            return
        }
        val wait = 500L shl attempts++
        reconnectJob = scope.launch {
            delay(wait)
            attach()
        }
    }

    private fun scheduleSnapshotSave() {
        snapshotDebounceJob?.cancel()
        snapshotDebounceJob = scope.launch {
            delay(500)
            takeQueueSnapshot()?.let { onSnapshotChanged?.invoke(it) }
        }
    }

    private fun saveSnapshotNow() {
        snapshotDebounceJob?.cancel()
        takeQueueSnapshot()?.let { onSnapshotChanged?.invoke(it) }
    }

    private fun updateTicker(playing: Boolean) {
        tickJob?.cancel()
        if (!playing) return
        tickJob = scope.launch {
            var counter = 0
            while (isActive) {
                delay(250)
                mc?.let { _pos.value = it.currentPosition }
                counter++
                if (counter >= 60) { // 60 * 250ms = 15s periodic save while playing
                    counter = 0
                    saveSnapshotNow()
                }
            }
        }
    }

    private val listener = object : Player.Listener {
        override fun onEvents(p: Player, e: Player.Events) {
            sync(p)
            if (e.contains(Player.EVENT_IS_PLAYING_CHANGED)) {
                updateTicker(p.isPlaying)
                if (!p.isPlaying) {
                    saveSnapshotNow()
                }
            }
            if (e.contains(Player.EVENT_MEDIA_ITEM_TRANSITION)) {
                val newId = p.currentMediaItem?.mediaId
                if (newId != null && newId != queueModel.currentId) {
                    val active = queueModel.active
                    if (newId in active) {
                        // Current track advanced naturally or via seek
                        // Update current in model
                        if (queueModel.shuffleEnabled) {
                            queueModel.enableShuffleFrom(queueModel.shuffled, queueModel.seed, newId)
                        } else {
                            queueModel = QueueModel(queueModel.original, newId)
                        }
                    }
                }
                saveSnapshotNow()
            }
            if (e.contains(Player.EVENT_REPEAT_MODE_CHANGED)) {
                scheduleSnapshotSave()
            }
        }
    }

    private fun sync(c: Player) {
        val md = c.mediaMetadata
        val isSeekable = c.isCurrentMediaItemSeekable && c.duration != C.TIME_UNSET && c.duration > 0
        val curId = c.currentMediaItem?.mediaId
        _state.update {
            it.copy(
                mediaId = curId,
                title = md.title?.toString().orEmpty(),
                artist = md.artist?.toString().orEmpty(),
                album = md.albumTitle?.toString().orEmpty(),
                artworkUri = c.currentMediaItem?.localConfiguration?.uri?.toString(),
                isPlaying = c.isPlaying,
                playWhenReady = c.playWhenReady,
                buffering = c.playbackState == Player.STATE_BUFFERING,
                durationMs = if (c.duration == C.TIME_UNSET || c.duration < 0) 0L else c.duration,
                index = c.currentMediaItemIndex,
                queueSize = c.mediaItemCount,
                shuffle = queueModel.shuffleEnabled, // Media3 shuffle stays disabled
                repeat = c.repeatMode,
                speed = c.playbackParameters.speed,
                canSeek = isSeekable
            )
        }
        _queue.value = (0 until c.mediaItemCount).map { i ->
            val m = c.getMediaItemAt(i)
            Track(
                id = m.mediaId,
                uri = m.localConfiguration?.uri?.toString().orEmpty(),
                title = m.mediaMetadata.title?.toString().orEmpty(),
                artist = m.mediaMetadata.artist?.toString().orEmpty(),
                album = m.mediaMetadata.albumTitle?.toString().orEmpty(),
                durationMs = 0L,
                isFavorite = false
            )
        }
        _pos.value = c.currentPosition
    }

    private fun Track.toItem(): MediaItem = MediaItem.Builder()
        .setMediaId(id)
        .setUri(uri)
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(title)
                .setArtist(artist)
                .setAlbumTitle(album)
                .build()
        )
        .build()

    fun setQueueNotice(notice: String?) {
        _state.update { it.copy(queueNotice = notice) }
    }

    fun clearQueueNotice() {
        _state.update { it.copy(queueNotice = null) }
    }

    fun playTrack(t: Track) {
        itemCache[t.id] = t.toItem()
        queueModel = QueueModel(listOf(t.id), t.id)
        mc?.let {
            it.setMediaItem(t.toItem())
            it.prepare()
            it.play()
        }
        saveSnapshotNow()
    }

    fun playQueue(tracks: List<Track>, startIdx: Int = 0, shuffle: Boolean = false) {
        if (tracks.isEmpty()) return
        tracks.forEach { itemCache[it.id] = it.toItem() }
        val ids = tracks.map { it.id }.distinct()
        val startId = ids.getOrNull(startIdx) ?: ids.firstOrNull()
        queueModel = QueueModel(ids, startId)
        if (shuffle) {
            queueModel.enableShuffle(Random.nextLong())
        }
        val activeIds = queueModel.active
        val items = activeIds.mapNotNull { itemCache[it] }
        val activeIndex = activeIds.indexOf(startId).coerceAtLeast(0)
        mc?.let {
            it.setMediaItems(items, activeIndex, 0L)
            it.prepare()
            it.play()
        }
        _state.update { it.copy(shuffle = queueModel.shuffleEnabled) }
        saveSnapshotNow()
    }

    fun playExternal(uri: android.net.Uri) {
        val item = MediaItem.Builder()
            .setMediaId("ext:${uri.hashCode()}")
            .setUri(uri)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(uri.lastPathSegment ?: "External Audio")
                    .setArtist("External")
                    .build()
            )
            .build()
        mc?.let {
            it.setMediaItem(item)
            it.prepare()
            it.play()
        }
    }

    fun togglePlay() {
        mc?.let {
            if (it.isPlaying) {
                it.pause()
            } else {
                it.play()
            }
        }
    }

    fun next() {
        mc?.let {
            if (it.hasNextMediaItem()) {
                it.seekToNextMediaItem()
            }
        }
    }

    fun previous() {
        mc?.let {
            if (it.hasPreviousMediaItem()) {
                it.seekToPreviousMediaItem()
            } else {
                it.seekTo(0L)
            }
        }
    }

    fun seekTo(ms: Long): Boolean {
        val c = mc ?: return false
        val d = c.duration
        if (!c.isCurrentMediaItemSeekable || d == C.TIME_UNSET || d <= 0 || ms < 0 || ms > d) return false
        c.seekTo(ms)
        _pos.value = ms
        return true
    }

    fun skipBy(deltaMs: Long): Boolean {
        val c = mc ?: return false
        val d = c.duration
        if (d == C.TIME_UNSET || d <= 0) return false
        return seekTo((c.currentPosition + deltaMs).coerceIn(0, d))
    }

    fun toggleShuffle() {
        val c = mc ?: return
        val curId = queueModel.currentId ?: c.currentMediaItem?.mediaId ?: return
        if (queueModel.shuffleEnabled) {
            queueModel.disableShuffle()
        } else {
            queueModel.enableShuffle(Random.nextLong())
        }
        val order = queueModel.active.mapNotNull { itemCache[it] }
        if (order.isNotEmpty()) {
            c.reorderAroundCurrent(order, curId)
        }
        _state.update { it.copy(shuffle = queueModel.shuffleEnabled) }
        scheduleSnapshotSave()
    }

    fun cycleRepeat() {
        mc?.let {
            it.repeatMode = when (it.repeatMode) {
                Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                else -> Player.REPEAT_MODE_OFF
            }
        }
        scheduleSnapshotSave()
    }

    fun setPlaybackParameters(speed: Float, lockPitch: Boolean) {
        _state.update { it.copy(speed = speed, pitchLocked = lockPitch) }
        mc?.playbackParameters = PlaybackParameters(speed, if (lockPitch) 1f else speed)
    }

    fun setSpeed(s: Float) {
        val lock = _state.value.pitchLocked
        setPlaybackParameters(s, lock)
    }

    fun setPitchLocked(lock: Boolean) {
        val spd = _state.value.speed
        setPlaybackParameters(spd, lock)
    }

    fun setSkipSilence(enabled: Boolean) {
        _state.update { it.copy(skipSilence = enabled) }
        mc?.let { c ->
            val bundle = Bundle().apply { putBoolean("enabled", enabled) }
            c.sendCustomCommand(SessionCommand("pulse.SKIP_SILENCE", Bundle.EMPTY), bundle)
        }
    }

    fun playNext(t: Track) {
        itemCache[t.id] = t.toItem()
        queueModel.playNext(t.id)
        val c = mc ?: return
        val curId = queueModel.currentId ?: c.currentMediaItem?.mediaId
        if (curId != null) {
            val order = queueModel.active.mapNotNull { itemCache[it] }
            if (order.isNotEmpty()) {
                c.reorderAroundCurrent(order, curId)
            }
        } else {
            c.setMediaItems(queueModel.active.mapNotNull { itemCache[it] })
            c.prepare()
        }
        scheduleSnapshotSave()
    }

    fun addToQueue(t: Track): Boolean {
        val added = queueModel.append(t.id)
        if (!added) {
            _state.update { it.copy(queueNotice = "'${t.title}' is already in the queue") }
            return false
        }
        itemCache[t.id] = t.toItem()
        mc?.addMediaItem(t.toItem())
        scheduleSnapshotSave()
        return true
    }

    fun removeFromQueue(i: Int) {
        val active = queueModel.active
        val id = active.getOrNull(i) ?: return
        queueModel.remove(id)
        mc?.removeMediaItem(i)
        scheduleSnapshotSave()
    }

    fun moveInQueue(from: Int, to: Int) {
        queueModel.move(from, to)
        mc?.moveMediaItem(from, to)
        scheduleSnapshotSave()
    }

    fun clearQueue() {
        queueModel = QueueModel(emptyList(), null)
        itemCache.clear()
        mc?.clearMediaItems()
        scheduleSnapshotSave()
    }

    fun skipToQueueIndex(i: Int) {
        mc?.seekTo(i, 0L)
    }

    fun playAt(i: Int) {
        val c = mc ?: return
        if (i in 0 until c.mediaItemCount) {
            c.seekToDefaultPosition(i)
            c.play()
        }
    }

    fun removeAt(i: Int) {
        removeFromQueue(i)
    }

    fun startSleep(minutes: Int) {
        cancelSleep()
        if (::PlaybackEngine.isInitialized && PlaybackEngine.exo != null) {
            PlaybackEngine.sleep.start(minutes)
        }
        val end = SystemClock.elapsedRealtime() + minutes * 60_000L
        sleepJob = scope.launch {
            while (true) {
                val left = end - SystemClock.elapsedRealtime()
                _state.update { it.copy(sleepRemainingMs = left.coerceAtLeast(0)) }
                if (left <= 0) break
                delay(500)
            }
            _state.update { it.copy(sleepRemainingMs = 0) }
        }
    }

    fun sleepAtEndOfTrack() {
        cancelSleep()
        if (::PlaybackEngine.isInitialized && PlaybackEngine.exo != null) {
            PlaybackEngine.sleep.startEndOfTrack()
        }
        _state.update { it.copy(sleepEndOfTrack = true) }
    }

    fun cancelSleep() {
        sleepJob?.cancel()
        if (::PlaybackEngine.isInitialized && PlaybackEngine.exo != null) {
            PlaybackEngine.sleep.cancel()
        }
        _state.update { it.copy(sleepRemainingMs = 0, sleepEndOfTrack = false) }
    }

    fun retryError() = PlaybackEngine.errors.retry()
    fun skipError() = PlaybackEngine.errors.skip()

    fun takeQueueSnapshot(): QueueSnapshot? = mc?.let { c ->
        if (queueModel.original.isEmpty()) return null
        QueueSnapshot(
            original = queueModel.original,
            shuffled = queueModel.shuffled,
            currentId = queueModel.currentId ?: c.currentMediaItem?.mediaId,
            positionMs = c.currentPosition,
            repeat = c.repeatMode,
            shuffleEnabled = queueModel.shuffleEnabled,
            seed = queueModel.seed,
            savedAtMs = System.currentTimeMillis()
        )
    }

    fun snapshot(): Triple<List<String>, Int, Long>? = mc?.let { c ->
        Triple(
            queueModel.active,
            c.currentMediaItemIndex,
            c.currentPosition
        )
    }

    fun restore(ids: List<String>, index: Int, pos: Long, tracks: List<Track>) {
        val c = mc ?: return
        if (tracks.isEmpty()) return
        tracks.forEach { itemCache[it.id] = it.toItem() }
        val startId = ids.getOrNull(index) ?: ids.firstOrNull()
        queueModel = QueueModel(ids, startId)
        c.setMediaItems(tracks.map { it.toItem() }, index.coerceIn(0, tracks.lastIndex), pos)
        c.prepare()
    }

    fun restoreFromSnapshot(restored: Restored, tracksMap: Map<String, Track>) {
        val c = mc ?: return
        queueModel = restored.model
        val activeIds = queueModel.active
        val activeTracks = activeIds.mapNotNull { tracksMap[it] }
        if (activeTracks.isEmpty()) return
        activeTracks.forEach { itemCache[it.id] = it.toItem() }
        val curIndex = activeIds.indexOf(queueModel.currentId).coerceAtLeast(0)
        val items = activeTracks.map { it.toItem() }
        c.setMediaItems(items, curIndex, restored.positionMs)
        c.prepare()
        if (restored.startPaused) {
            c.pause()
        }
        _state.update { it.copy(shuffle = queueModel.shuffleEnabled) }
    }
}
