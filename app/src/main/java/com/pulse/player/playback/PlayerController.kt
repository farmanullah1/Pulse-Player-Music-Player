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
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import kotlin.math.pow
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
    private var queueLoadJob: Job? = null
    private var queueRebuildJob: Job? = null
    private var isChunkingQueue = false
    private var attempts = 0
    private var lastSnapshotSaveAtMs = 0L

    private var queueModel = QueueModel(emptyList(), null)
    private val itemCache = mutableMapOf<String, MediaItem>()
    private val trackLookup = mutableMapOf<String, Track>()

    var onSnapshotChanged: ((QueueSnapshot) -> Unit)? = null

    private val _state = MutableStateFlow(PlaybackUiState())
    val state = _state.asStateFlow()

    private val _pos = MutableStateFlow(0L)
    val position = _pos.asStateFlow()

    private val _queue = MutableStateFlow<List<Track>>(emptyList())
    val queue = _queue.asStateFlow()

    init {
        scope.launch {
            PlaybackEngine.errorEvents.collect { err ->
                _state.update { it.copy(trackError = err) }
            }
        }
        scope.launch {
            PlaybackEngine.appliedReplayGainDb.collect { gainDb ->
                _state.update { it.copy(appliedReplayGainDb = gainDb) }
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
                    sync(c, rebuildQueue = true)
                    updateTicker(shouldTick(c))
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
        queueLoadJob?.cancel()
        queueRebuildJob?.cancel()
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
            delay(800)
            saveSnapshotNow()
        }
    }

    private fun saveSnapshotNow() {
        snapshotDebounceJob?.cancel()
        lastSnapshotSaveAtMs = System.currentTimeMillis()
        takeQueueSnapshot()?.let { onSnapshotChanged?.invoke(it) }
    }

    private fun shouldTick(c: Player): Boolean =
        c.isPlaying || (c.playWhenReady && c.playbackState != Player.STATE_ENDED && c.playbackState != Player.STATE_IDLE)

    private fun updateTicker(active: Boolean) {
        if (!active) {
            tickJob?.cancel()
            tickJob = null
            return
        }
        if (tickJob?.isActive == true) return
        tickJob = scope.launch {
            while (isActive) {
                delay(200)
                val c = mc ?: break
                val pos = c.currentPosition.coerceAtLeast(0L)
                _pos.value = pos

                val st = _state.value
                val curItem = c.currentMediaItem
                val curId = curItem?.mediaId
                val curTitle = c.mediaMetadata.title?.toString().orEmpty()
                val rawDur = if (c.duration == C.TIME_UNSET || c.duration < 0) 0L else c.duration
                val fallbackDur = curId?.let { trackLookup[it]?.durationMs } ?: 0L
                val curDur = if (rawDur > 0L) rawDur else fallbackDur

                if (curId != st.mediaId ||
                    (curTitle.isNotBlank() && curTitle != st.title) ||
                    (curDur > 0L && curDur != st.durationMs) ||
                    c.isPlaying != st.isPlaying ||
                    c.currentMediaItemIndex != st.index
                ) {
                    sync(c, rebuildQueue = false)
                }

                val loopStart = st.abLoopStartMs
                val loopEnd = st.abLoopEndMs
                if (loopStart != null && loopEnd != null && loopEnd > loopStart && pos >= loopEnd) {
                    c.seekTo(loopStart)
                    _pos.value = loopStart
                }

                val now = System.currentTimeMillis()
                if (c.isPlaying && now - lastSnapshotSaveAtMs >= 15_000L) {
                    saveSnapshotNow()
                }
            }
        }
    }

    private val listener = object : Player.Listener {
        override fun onEvents(p: Player, e: Player.Events) {
            val rebuildQueue = e.contains(Player.EVENT_TIMELINE_CHANGED)
            sync(p, rebuildQueue = rebuildQueue)
            if (e.containsAny(
                    Player.EVENT_IS_PLAYING_CHANGED,
                    Player.EVENT_PLAYBACK_STATE_CHANGED,
                    Player.EVENT_PLAY_WHEN_READY_CHANGED
                )
            ) {
                updateTicker(shouldTick(p))
                if (!p.isPlaying && !p.playWhenReady) {
                    saveSnapshotNow()
                }
            }
            if (e.contains(Player.EVENT_MEDIA_ITEM_TRANSITION)) {
                val newId = p.currentMediaItem?.mediaId
                if (newId != null && newId != queueModel.currentId) {
                    val active = queueModel.active
                    if (newId in active) {
                        if (queueModel.shuffleEnabled) {
                            queueModel.enableShuffleFrom(queueModel.shuffled, queueModel.seed, newId)
                        } else {
                            queueModel = QueueModel(queueModel.original, newId)
                        }
                    }
                }
                updateTicker(shouldTick(p))
                scheduleSnapshotSave()
            }
            if (e.contains(Player.EVENT_REPEAT_MODE_CHANGED)) {
                scheduleSnapshotSave()
            }
        }
    }

    private fun sync(c: Player, rebuildQueue: Boolean = false) {
        val md = c.mediaMetadata
        val curItem = c.currentMediaItem
        val curId = curItem?.mediaId
        val rawDur = if (c.duration == C.TIME_UNSET || c.duration < 0) 0L else c.duration
        val cachedTrack = curId?.let { trackLookup[it] }
        val effectiveDur = if (rawDur > 0L) rawDur else (cachedTrack?.durationMs ?: 0L)
        val isSeekable = c.isCurrentMediaItemSeekable && effectiveDur > 0
        val title = md.title?.toString()?.takeIf { it.isNotBlank() } ?: cachedTrack?.title.orEmpty()
        val artist = md.artist?.toString()?.takeIf { it.isNotBlank() } ?: cachedTrack?.artist.orEmpty()
        val album = md.albumTitle?.toString()?.takeIf { it.isNotBlank() } ?: cachedTrack?.album.orEmpty()
        val artUri = curItem?.localConfiguration?.uri?.toString() ?: cachedTrack?.uri

        _state.update {
            it.copy(
                mediaId = curId,
                title = title,
                artist = artist,
                album = album,
                artworkUri = artUri,
                isPlaying = c.isPlaying,
                playWhenReady = c.playWhenReady,
                buffering = c.playbackState == Player.STATE_BUFFERING,
                durationMs = effectiveDur,
                index = c.currentMediaItemIndex,
                queueSize = c.mediaItemCount,
                shuffle = queueModel.shuffleEnabled, // Media3 shuffle stays disabled
                repeat = c.repeatMode,
                speed = c.playbackParameters.speed,
                abLoopStartMs = if (it.mediaId != curId) null else it.abLoopStartMs,
                abLoopEndMs = if (it.mediaId != curId) null else it.abLoopEndMs,
                canSeek = isSeekable
            )
        }
        if (rebuildQueue && !isChunkingQueue) {
            val count = c.mediaItemCount
            if (count == 0) {
                queueRebuildJob?.cancel()
                _queue.value = emptyList()
            } else {
                val items = ArrayList<MediaItem>(count)
                for (i in 0 until count) {
                    items += c.getMediaItemAt(i)
                }
                val lookupSnapshot = trackLookup.toMap()
                queueRebuildJob?.cancel()
                queueRebuildJob = scope.launch {
                    val rebuilt = withContext(Dispatchers.Default) {
                        items.map { m ->
                            val cached = lookupSnapshot[m.mediaId]
                            cached ?: Track(
                                id = m.mediaId,
                                uri = m.localConfiguration?.uri?.toString().orEmpty(),
                                title = m.mediaMetadata.title?.toString().orEmpty(),
                                artist = m.mediaMetadata.artist?.toString().orEmpty(),
                                album = m.mediaMetadata.albumTitle?.toString().orEmpty(),
                                durationMs = 0L,
                                isFavorite = false
                            )
                        }
                    }
                    _queue.value = rebuilt
                }
            }
        }
        _pos.value = c.currentPosition.coerceAtLeast(0L)
    }

    private fun Track.toItem(): MediaItem = MediaItem.Builder()
        .setMediaId(id)
        .setUri(uri)
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(title)
                .setArtist(artist)
                .setAlbumTitle(album)
                .setArtworkUri(uri.takeIf { it.isNotBlank() }?.let(android.net.Uri::parse))
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
        trackLookup[t.id] = t
        queueModel = QueueModel(listOf(t.id), t.id)
        _queue.value = listOf(t)
        mc?.let {
            it.setMediaItem(t.toItem())
            it.prepare()
            it.play()
        }
        scheduleSnapshotSave()
    }

    fun playQueue(tracks: List<Track>, startIdx: Int = 0, shuffle: Boolean = false) {
        if (tracks.isEmpty()) return
        queueLoadJob?.cancel()
        queueRebuildJob?.cancel()
        val snapshot = tracks.toList()
        queueLoadJob = scope.launch {
            val prepared = withContext(Dispatchers.Default) {
                val builtMap = HashMap<String, MediaItem>(snapshot.size)
                val trackMap = HashMap<String, Track>(snapshot.size)
                val ids = ArrayList<String>(snapshot.size)
                for (t in snapshot) {
                    ensureActive()
                    if (t.id !in builtMap) {
                        builtMap[t.id] = t.toItem()
                        trackMap[t.id] = t
                        ids += t.id
                    }
                }
                val startId = ids.getOrNull(startIdx) ?: ids.firstOrNull()
                val model = QueueModel(ids, startId)
                if (shuffle) {
                    model.enableShuffle(Random.nextLong())
                }
                val activeIds = model.active
                val items = activeIds.mapNotNull { builtMap[it] }
                val activeTracks = activeIds.mapNotNull { trackMap[it] }
                val activeIndex = activeIds.indexOf(startId).coerceAtLeast(0)
                Triple(model, Pair(builtMap, trackMap), Triple(items, activeTracks, activeIndex))
            }
            val (model, maps, payload) = prepared
            val (builtMap, trackMap) = maps
            val (items, activeTracks, activeIndex) = payload
            itemCache.putAll(builtMap)
            trackLookup.putAll(trackMap)
            queueModel = model
            _queue.value = activeTracks
            val safeIndex = if (activeTracks.isEmpty()) 0 else activeIndex.coerceIn(0, activeTracks.lastIndex)
            val initialTrack = activeTracks.getOrNull(safeIndex)
            _state.update {
                it.copy(
                    mediaId = initialTrack?.id ?: it.mediaId,
                    title = initialTrack?.title ?: it.title,
                    artist = initialTrack?.artist ?: it.artist,
                    album = initialTrack?.album ?: it.album,
                    artworkUri = initialTrack?.uri ?: it.artworkUri,
                    durationMs = initialTrack?.durationMs ?: it.durationMs,
                    index = safeIndex,
                    queueSize = activeTracks.size,
                    shuffle = model.shuffleEnabled
                )
            }
            val c = mc
            if (c != null && items.isNotEmpty()) {
                val itemSafeIndex = activeIndex.coerceIn(0, items.lastIndex)
                if (items.size <= 400) {
                    c.setMediaItems(items, itemSafeIndex, 0L)
                    c.prepare()
                    c.play()
                } else {
                    isChunkingQueue = true
                    try {
                        c.setMediaItems(listOf(items[itemSafeIndex]), 0, 0L)
                        c.prepare()
                        c.play()
                        var insertPos = 0
                        for (chunk in items.subList(0, itemSafeIndex).chunked(250)) {
                            ensureActive()
                            c.addMediaItems(insertPos, chunk)
                            insertPos += chunk.size
                            yield()
                        }
                        for (chunk in items.subList(itemSafeIndex + 1, items.size).chunked(250)) {
                            ensureActive()
                            c.addMediaItems(chunk)
                            yield()
                        }
                    } finally {
                        isChunkingQueue = false
                    }
                }
                updateTicker(true)
            }
            scheduleSnapshotSave()
        }
    }

    fun playExternal(uri: android.net.Uri) {
        val extId = "ext:${uri.hashCode()}"
        val extTitle = uri.lastPathSegment ?: "External Audio"
        val item = MediaItem.Builder()
            .setMediaId(extId)
            .setUri(uri)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(extTitle)
                    .setArtist("External")
                    .build()
            )
            .build()
        _state.update {
            it.copy(
                mediaId = extId,
                title = extTitle,
                artist = "External",
                album = "",
                artworkUri = null
            )
        }
        mc?.let {
            it.setMediaItem(item)
            it.prepare()
            it.play()
            updateTicker(true)
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

    fun togglePlayPause() = togglePlay()

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
        applyPlaybackParameters(speed, lockPitch, _state.value.pitchSemitones)
    }

    fun applyPlaybackParameters(speed: Float, lockPitch: Boolean, pitchSemitones: Float) {
        val safeSpeed = speed.coerceIn(0.5f, 2.0f)
        val safeSemi = pitchSemitones.coerceIn(-6f, 6f)
        val semiFactor = 2.0.pow(safeSemi.toDouble() / 12.0).toFloat()
        val basePitch = if (lockPitch) 1f else safeSpeed
        val finalPitch = (basePitch * semiFactor).coerceIn(0.4f, 2.5f)
        _state.update { it.copy(speed = safeSpeed, pitchLocked = lockPitch, pitchSemitones = safeSemi) }
        mc?.playbackParameters = PlaybackParameters(safeSpeed, finalPitch)
    }

    fun setSpeed(s: Float) {
        applyPlaybackParameters(s, _state.value.pitchLocked, _state.value.pitchSemitones)
    }

    fun setPitchLocked(lock: Boolean) {
        applyPlaybackParameters(_state.value.speed, lock, _state.value.pitchSemitones)
    }

    fun setPitchSemitones(semitones: Float) {
        applyPlaybackParameters(_state.value.speed, _state.value.pitchLocked, semitones)
    }

    fun setAbLoopA(posMs: Long? = null) {
        val pos = (posMs ?: mc?.currentPosition ?: _pos.value).coerceAtLeast(0L)
        _state.update { st ->
            val end = st.abLoopEndMs?.takeIf { it > pos + 300L }
            st.copy(abLoopStartMs = pos, abLoopEndMs = end)
        }
    }

    fun setAbLoopB(posMs: Long? = null) {
        val pos = (posMs ?: mc?.currentPosition ?: _pos.value).coerceAtLeast(0L)
        _state.update { st ->
            val start = st.abLoopStartMs ?: 0L
            if (pos <= start + 300L) {
                st.copy(queueNotice = "Loop point B must be after point A")
            } else {
                st.copy(abLoopStartMs = start, abLoopEndMs = pos)
            }
        }
    }

    fun clearAbLoop() {
        _state.update { it.copy(abLoopStartMs = null, abLoopEndMs = null) }
    }

    fun setSleepFadeSec(sec: Int) {
        val safe = sec.coerceIn(0, 30)
        _state.update { it.copy(sleepFadeSec = safe) }
        PlaybackEngine.setSleepFadeDurationMs(safe * 1000L)
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
        trackLookup[t.id] = t
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
        trackLookup[t.id] = t
        mc?.addMediaItem(t.toItem())
        scheduleSnapshotSave()
        return true
    }

    fun removeFromQueue(i: Int) {
        removeFromQueueWithUndo(i)
    }

    fun removeFromQueueWithUndo(i: Int): Track? {
        val active = queueModel.active
        val id = active.getOrNull(i) ?: return null
        val removedTrack = _queue.value.getOrNull(i) ?: trackLookup[id]
        queueModel.remove(id)
        mc?.let { c ->
            if (i in 0 until c.mediaItemCount) {
                c.removeMediaItem(i)
            }
        }
        _queue.update { cur ->
            if (i in cur.indices) cur.toMutableList().apply { removeAt(i) } else cur
        }
        scheduleSnapshotSave()
        return removedTrack
    }

    fun insertTrackAt(index: Int, track: Track) {
        itemCache[track.id] = track.toItem()
        trackLookup[track.id] = track
        val currentQueue = _queue.value
        val clamped = index.coerceIn(0, currentQueue.size)
        val newList = currentQueue.toMutableList().apply {
            if (none { it.id == track.id }) add(clamped, track)
        }
        val curId = queueModel.currentId ?: _state.value.mediaId
        queueModel = QueueModel(newList.map { it.id }, curId)
        _queue.value = newList
        mc?.let { c ->
            val safeIdx = clamped.coerceIn(0, c.mediaItemCount)
            c.addMediaItem(safeIdx, track.toItem())
        }
        scheduleSnapshotSave()
    }

    fun moveInQueue(from: Int, to: Int) {
        val current = _queue.value
        if (from == to || from !in current.indices || to !in current.indices) return
        queueModel.move(from, to)
        _queue.update { list ->
            if (from in list.indices && to in list.indices) {
                list.toMutableList().apply {
                    val moved = removeAt(from)
                    add(to, moved)
                }
            } else list
        }
        mc?.let { c ->
            if (from in 0 until c.mediaItemCount && to in 0 until c.mediaItemCount) {
                c.moveMediaItem(from, to)
            }
        }
        scheduleSnapshotSave()
    }

    fun shuffleUpcoming(): Int {
        val current = _queue.value
        val curIdx = _state.value.index.coerceIn(0, (current.lastIndex).coerceAtLeast(0))
        if (current.size - curIdx <= 2) return 0
        val head = current.subList(0, curIdx + 1)
        val tail = current.subList(curIdx + 1, current.size).shuffled(Random(System.currentTimeMillis()))
        val reordered = head + tail
        val curId = queueModel.currentId ?: reordered.getOrNull(curIdx)?.id
        queueModel = QueueModel(reordered.map { it.id }, curId)
        _queue.value = reordered
        mc?.let { c ->
            val tailItems = tail.map { it.toItem() }
            if (curIdx + 1 < c.mediaItemCount) {
                c.replaceMediaItems(curIdx + 1, c.mediaItemCount, tailItems)
            }
        }
        scheduleSnapshotSave()
        return tail.size
    }

    fun clearQueue() {
        queueModel = QueueModel(emptyList(), null)
        itemCache.clear()
        _queue.value = emptyList()
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
        if (PlaybackEngine.isAttached()) {
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
        if (PlaybackEngine.isAttached()) {
            PlaybackEngine.sleep.startEndOfTrack()
        }
        _state.update { it.copy(sleepEndOfTrack = true) }
    }

    fun cancelSleep() {
        sleepJob?.cancel()
        if (PlaybackEngine.isAttached()) {
            PlaybackEngine.sleep.cancel()
        }
        _state.update { it.copy(sleepRemainingMs = 0, sleepEndOfTrack = false) }
    }

    fun retryError() = PlaybackEngine.retryError()
    fun skipError() = PlaybackEngine.skipError()

    fun retry() = retryError()
    fun skip() = skipError()

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
        tracks.forEach {
            itemCache[it.id] = it.toItem()
            trackLookup[it.id] = it
        }
        val startId = ids.getOrNull(index) ?: ids.firstOrNull()
        queueModel = QueueModel(ids, startId)
        _queue.value = tracks
        c.setMediaItemsChunked(tracks.map { it.toItem() }, index.coerceIn(0, tracks.lastIndex), pos)
        c.prepare()
    }

    fun restoreFromSnapshot(restored: Restored, tracksMap: Map<String, Track>) {
        val c = mc ?: return
        queueModel = restored.model
        val activeIds = queueModel.active
        val activeTracks = activeIds.mapNotNull { tracksMap[it] }
        if (activeTracks.isEmpty()) return
        activeTracks.forEach {
            itemCache[it.id] = it.toItem()
            trackLookup[it.id] = it
        }
        val curIndex = activeIds.indexOf(queueModel.currentId).coerceAtLeast(0)
        val items = activeTracks.map { it.toItem() }
        _queue.value = activeTracks
        c.setMediaItemsChunked(items, curIndex, restored.positionMs)
        c.prepare()
        if (restored.startPaused) {
            c.pause()
        }
        _state.update { it.copy(shuffle = queueModel.shuffleEnabled) }
    }
}
