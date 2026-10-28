package com.pulse.player.playback

import android.content.ComponentName
import android.content.Context
import android.os.SystemClock
import androidx.core.content.ContextCompat
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.pulse.player.data.db.PlayEventEntity
import com.pulse.player.data.db.PulseDb
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PlayerController(private val ctx: Context, private val db: PulseDb) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var mc: MediaController? = null
    private val _state = MutableStateFlow(PlaybackUiState())
    val state = _state.asStateFlow()

    private val _pos = MutableStateFlow(0L)
    val position = _pos.asStateFlow() // only seek bars collect this

    private val _queue = MutableStateFlow<List<Track>>(emptyList())
    val queue = _queue.asStateFlow()

    private var sleepJob: Job? = null
    private var fadeJob: Job? = null
    private var listenedMs = 0L
    private var lastId: String? = null
    var fadeEnabled = true

    init {
        val token = SessionToken(ctx, ComponentName(ctx, MusicPlaybackService::class.java))
        val f = MediaController.Builder(ctx, token).buildAsync()
        f.addListener({
            runCatching {
                mc = f.get().also {
                    it.addListener(listener)
                    sync(it)
                }
            }
        }, ContextCompat.getMainExecutor(ctx))

        scope.launch {
            while (true) {
                delay(250)
                mc?.let {
                    if (it.isPlaying) {
                        _pos.value = it.currentPosition
                        listenedMs += 250
                    }
                }
            }
        }
    }

    private val listener = object : Player.Listener {
        override fun onEvents(p: Player, e: Player.Events) {
            sync(p as MediaController)
        }

        override fun onMediaItemTransition(item: MediaItem?, reason: Int) {
            logEvent(completed = reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO)
            lastId = item?.mediaId
            listenedMs = 0
            if (_state.value.sleepEndOfTrack && reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO) {
                mc?.pause()
                cancelSleep()
            }
        }

        override fun onPlayerError(e: PlaybackException) {
            _state.update { it.copy(error = "This file can't be played on this device.") }
        }
    }

    private fun logEvent(completed: Boolean) {
        val id = lastId ?: return
        val ms = listenedMs
        if (ms >= 30_000 || completed) {
            scope.launch(Dispatchers.IO) {
                db.stats().insert(
                    PlayEventEntity(
                        trackId = id,
                        playedAtMs = System.currentTimeMillis(),
                        listenedMs = ms,
                        completed = completed
                    )
                )
            }
        }
    }

    private fun sync(c: MediaController) {
        val md = c.mediaMetadata
        _state.update {
            it.copy(
                mediaId = c.currentMediaItem?.mediaId,
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
                shuffle = c.shuffleModeEnabled,
                repeat = c.repeatMode,
                speed = c.playbackParameters.speed
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

    // Commands
    fun playQueue(tracks: List<Track>, startIndex: Int, shuffle: Boolean = false) {
        val c = mc ?: return
        if (tracks.isEmpty()) return
        c.setMediaItems(tracks.map { it.toItem() }, startIndex.coerceIn(0, tracks.lastIndex), 0L)
        c.shuffleModeEnabled = shuffle
        c.prepare()
        fadeIn(c)
    }

    fun togglePlay() {
        val c = mc ?: return
        if (c.isPlaying) {
            fadePause(c)
        } else {
            if (c.playbackState == Player.STATE_IDLE) c.prepare()
            fadeIn(c)
        }
    }

    fun next() {
        mc?.seekToNextMediaItem()
    }

    fun previous() {
        mc?.let {
            if (it.currentPosition > 3000) it.seekTo(0) else it.seekToPreviousMediaItem()
        }
    }

    fun seekTo(ms: Long) {
        mc?.seekTo(ms)
        _pos.value = ms
    }

    fun skipBy(ms: Long) {
        mc?.let {
            val target = (it.currentPosition + ms).coerceIn(0, if (it.duration < 0) 0 else it.duration)
            seekTo(target)
        }
    }

    fun toggleShuffle() {
        mc?.let { it.shuffleModeEnabled = !it.shuffleModeEnabled }
    }

    fun cycleRepeat() {
        mc?.let {
            it.repeatMode = when (it.repeatMode) {
                Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                else -> Player.REPEAT_MODE_OFF
            }
        }
    }

    fun setSpeed(s: Float) {
        mc?.playbackParameters = PlaybackParameters(s, 1f)
    }

    fun playNext(t: Track) {
        mc?.let {
            val nextIndex = (it.currentMediaItemIndex + 1).coerceAtMost(it.mediaItemCount)
            it.addMediaItem(nextIndex, t.toItem())
        }
    }

    fun addToQueue(t: Track) {
        mc?.addMediaItem(t.toItem())
    }

    fun removeFromQueue(i: Int) {
        mc?.removeMediaItem(i)
    }

    fun moveInQueue(from: Int, to: Int) {
        mc?.moveMediaItem(from, to)
    }

    fun clearQueue() {
        mc?.clearMediaItems()
    }

    fun skipToQueueIndex(i: Int) {
        mc?.seekTo(i, 0L)
    }

    // Real audio fades
    private fun ramp(c: MediaController, from: Float, to: Float, ms: Long, done: () -> Unit = {}) {
        fadeJob?.cancel()
        fadeJob = scope.launch {
            val steps = 12
            for (i in 0..steps) {
                c.volume = from + (to - from) * i / steps
                delay(ms / steps)
            }
            done()
        }
    }

    private fun fadeIn(c: MediaController) {
        if (fadeEnabled) {
            c.volume = 0f
            c.play()
            ramp(c, 0f, 1f, 350)
        } else {
            c.volume = 1f
            c.play()
        }
    }

    private fun fadePause(c: MediaController) {
        if (fadeEnabled) {
            ramp(c, c.volume, 0f, 250) {
                c.pause()
                c.volume = 1f
            }
        } else {
            c.pause()
        }
    }

    // Sleep timer
    fun startSleep(minutes: Int) {
        cancelSleep()
        val end = SystemClock.elapsedRealtime() + minutes * 60_000L
        sleepJob = scope.launch {
            while (true) {
                val left = end - SystemClock.elapsedRealtime()
                _state.update { it.copy(sleepRemainingMs = left.coerceAtLeast(0)) }
                if (left <= 0) break
                if (left <= 10_000) {
                    mc?.let { it.volume = (left / 10_000f).coerceIn(0f, 1f) }
                }
                delay(500)
            }
            mc?.pause()
            mc?.volume = 1f
            _state.update { it.copy(sleepRemainingMs = 0) }
        }
    }

    fun sleepAtEndOfTrack() {
        cancelSleep()
        _state.update { it.copy(sleepEndOfTrack = true) }
    }

    fun cancelSleep() {
        sleepJob?.cancel()
        mc?.volume = 1f
        _state.update { it.copy(sleepRemainingMs = 0, sleepEndOfTrack = false) }
    }

    // Queue snapshot & restore
    fun snapshot(): Triple<List<String>, Int, Long>? = mc?.let { c ->
        Triple(
            (0 until c.mediaItemCount).map { c.getMediaItemAt(it).mediaId },
            c.currentMediaItemIndex,
            c.currentPosition
        )
    }

    fun restore(ids: List<String>, index: Int, pos: Long, tracks: List<Track>) {
        val c = mc ?: return
        if (tracks.isEmpty()) return
        c.setMediaItems(tracks.map { it.toItem() }, index.coerceIn(0, tracks.lastIndex), pos)
        c.prepare()
    }
}
