package com.pulse.player.playback

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.pulse.player.data.db.PulseDb
import com.pulse.player.data.settings.ReplayGainMode
import com.pulse.player.ui.widget.WidgetStateStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(UnstableApi::class)
object PlaybackEngine {
    var scope: CoroutineScope? = null
        private set
    var exo: ExoPlayer? = null
        private set
    lateinit var transitions: AudioTransitionController
        private set
    lateinit var sleep: SleepTimerEngine
        private set
    lateinit var errors: ErrorPolicy
        private set
    lateinit var tracker: SessionTracker
        private set
    var fadingPlayer: FadingPlayer? = null
        private set

    @Volatile var fadeEnabled = true
    @Volatile var crossfadeMs: Long = 0L
    @Volatile var crossfadeEqualPower: Boolean = true
    @Volatile var smartAlbumGapless: Boolean = true
    @Volatile var sleepFadeMs: Long = SleepTimerEngine.FADE_MS
    @Volatile var replayGainMode: ReplayGainMode = ReplayGainMode.OFF
    @Volatile var replayGainPreampDb: Float = 0f

    val errorEvents = MutableStateFlow<ErrorPolicy.TrackError?>(null)
    val appliedReplayGainDb = MutableStateFlow(0f)

    private var dbRef: PulseDb? = null
    private var secondaryDeckFactory: (() -> ExoPlayer)? = null
    private var deckB: ExoPlayer? = null
    private var deckBTargetIndex: Int = C.INDEX_UNSET
    private var deckBTargetId: String? = null
    private var crossfadeJob: Job? = null
    private var isCrossfadeHandoff: Boolean = false
    private var trackerTickerJob: Job? = null
    private var replayGainJob: Job? = null

    private val gainCache = object : LinkedHashMap<String, Pair<Float, Float>>(128, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Pair<Float, Float>>?): Boolean =
            size > 256
    }

    fun isAttached(): Boolean = exo != null && ::sleep.isInitialized && ::errors.isInitialized

    fun setSleepFadeDurationMs(ms: Long) {
        sleepFadeMs = ms.coerceAtLeast(0L)
        if (::sleep.isInitialized) {
            sleep.fadeDurationMs = sleepFadeMs
        }
    }

    fun setCrossfadeSeconds(secs: Int, equalPower: Boolean = crossfadeEqualPower) {
        crossfadeMs = secs.coerceIn(0, 12) * 1000L
        crossfadeEqualPower = equalPower
        if (crossfadeMs < 1000L) {
            cancelCrossfadeDeck(restoreVolume = true)
        } else {
            exo?.let { updateCrossfadeMonitor(it.isPlaying) }
        }
    }

    fun setReplayGainConfig(mode: ReplayGainMode, preampDb: Float) {
        replayGainMode = mode
        replayGainPreampDb = preampDb.coerceIn(-6f, 6f)
        applyReplayGainForTrack(exo?.currentMediaItem?.mediaId)
    }

    fun invalidateReplayGainCache(trackId: String? = null) {
        synchronized(gainCache) {
            if (trackId == null) gainCache.clear() else gainCache.remove(trackId)
        }
        if (trackId == null || trackId == exo?.currentMediaItem?.mediaId) {
            applyReplayGainForTrack(exo?.currentMediaItem?.mediaId)
        }
    }

    fun startSleep(minutes: Int) {
        if (::sleep.isInitialized && exo != null) sleep.start(minutes)
    }

    fun startSleepEndOfTrack() {
        if (::sleep.isInitialized && exo != null) sleep.startEndOfTrack()
    }

    fun cancelSleep() {
        if (::sleep.isInitialized && exo != null) sleep.cancel()
    }

    fun retryError() {
        if (::errors.isInitialized) errors.retry()
    }

    fun skipError() {
        if (::errors.isInitialized) errors.skip()
    }

    fun attach(
        ctx: Context,
        db: PulseDb,
        player: ExoPlayer,
        deckFactory: (() -> ExoPlayer)? = null
    ) {
        detach()
        val s = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        scope = s
        dbRef = db
        exo = player
        secondaryDeckFactory = deckFactory
        transitions = AudioTransitionController(s) { player.volume = it }
        sleep = SleepTimerEngine(s, player, transitions).apply {
            fadeDurationMs = sleepFadeMs
        }
        errors = ErrorPolicy(player) { trackId ->
            s.launch(Dispatchers.IO) {
                db.tracks().markUnavailable(trackId)
            }
        }
        s.launch {
            errors.current.collect { errorEvents.value = it }
        }
        val prefs = ctx.getSharedPreferences("pulse_session_tracker", Context.MODE_PRIVATE)
        tracker = SessionTracker(db, prefs)
        s.launch(Dispatchers.IO) {
            tracker.recoverOpenSession()
        }
        fadingPlayer = FadingPlayer(player, transitions, sleep) { fadeEnabled }

        player.addListener(object : Player.Listener {
            override fun onAudioSessionIdChanged(id: Int) {
                if (id > 0) AudioFx.attach(id)
                applyReplayGainForTrack(player.currentMediaItem?.mediaId)
            }

            override fun onPlayWhenReadyChanged(pwr: Boolean, reason: Int) {
                if (!pwr) {
                    cancelCrossfadeDeck(restoreVolume = false)
                    transitions.restore()
                }
                if (!pwr && reason == Player.PLAY_WHEN_READY_CHANGE_REASON_END_OF_MEDIA_ITEM) {
                    sleep.onPausedAtEndOfItem()
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                tracker.onPlayingChanged(isPlaying, player.playbackParameters.speed)
                updateTrackerTicker(isPlaying)
                updateCrossfadeMonitor(isPlaying)
                if (isPlaying) {
                    errors.onPlaying()
                    if (transitions.activeReason == null && deckB == null) transitions.restore()
                } else {
                    cancelCrossfadeDeck(restoreVolume = true)
                }
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                val id = mediaItem?.mediaId
                val dur = if (player.duration == C.TIME_UNSET || player.duration < 0) 0L else player.duration
                val speed = player.playbackParameters.speed
                if (!isCrossfadeHandoff) {
                    cancelCrossfadeDeck(restoreVolume = true)
                }
                applyReplayGainForTrack(id)
                if (id != null) {
                    tracker.onTrackStarted(id, dur, player.isPlaying, speed)
                    if (!isCrossfadeHandoff && crossfadeMs > 0 && player.isPlaying) {
                        transitions.snapTo(0.25f)
                        transitions.fadeTo(1f, crossfadeMs.coerceAtMost(1500L), TransitionReason.TrackChange)
                    }
                } else {
                    tracker.onTrackLeft(userAction = false)
                }
            }

            override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters) {
                tracker.onSpeedChanged(playbackParameters.speed)
                deckB?.playbackParameters = playbackParameters
            }

            override fun onPositionDiscontinuity(
                oldPosition: Player.PositionInfo,
                newPosition: Player.PositionInfo,
                reason: Int
            ) {
                if (!isCrossfadeHandoff && reason == Player.DISCONTINUITY_REASON_SEEK) {
                    cancelCrossfadeDeck(restoreVolume = true)
                }
                if (reason == Player.DISCONTINUITY_REASON_SEEK && oldPosition.mediaItemIndex != newPosition.mediaItemIndex) {
                    tracker.onTrackLeft(userAction = true)
                } else if (reason == Player.DISCONTINUITY_REASON_AUTO_TRANSITION && oldPosition.mediaItemIndex == newPosition.mediaItemIndex) {
                    val id = player.currentMediaItem?.mediaId
                    if (id != null) {
                        val dur = if (player.duration == C.TIME_UNSET || player.duration < 0) 0L else player.duration
                        tracker.onTrackStarted(id, dur, player.isPlaying, player.playbackParameters.speed)
                    }
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED || playbackState == Player.STATE_IDLE) {
                    cancelCrossfadeDeck(restoreVolume = true)
                }
                if (playbackState == Player.STATE_ENDED) {
                    tracker.onTrackLeft(userAction = false)
                }
            }

            override fun onPlayerError(e: PlaybackException) {
                cancelCrossfadeDeck(restoreVolume = true)
                errors.onError(e)
                tracker.onTrackLeft(userAction = false)
                com.pulse.player.data.ErrorDispatcher.post(
                    com.pulse.player.data.ErrorCategory.PLAYBACK,
                    "Playback Error",
                    e.localizedMessage ?: "Unable to play this track.",
                    e.stackTraceToString()
                )
            }

            override fun onEvents(p: Player, events: Player.Events) {
                if (events.containsAny(
                        Player.EVENT_MEDIA_ITEM_TRANSITION,
                        Player.EVENT_IS_PLAYING_CHANGED,
                        Player.EVENT_MEDIA_METADATA_CHANGED,
                        Player.EVENT_PLAYBACK_STATE_CHANGED,
                        Player.EVENT_TIMELINE_CHANGED
                    )
                ) {
                    WidgetStateStore.write(ctx, p, s)
                }
            }
        })

        if (player.audioSessionId > 0) {
            AudioFx.attach(player.audioSessionId)
        }
        applyReplayGainForTrack(player.currentMediaItem?.mediaId)
    }

    private fun applyReplayGainForTrack(trackId: String?) {
        replayGainJob?.cancel()
        if (!::transitions.isInitialized) return
        if (trackId.isNullOrBlank() || replayGainMode == ReplayGainMode.OFF) {
            transitions.setReplayGainScale(1f)
            AudioFx.setReplayGainBoostMb(0)
            appliedReplayGainDb.value = 0f
            return
        }
        val cached = synchronized(gainCache) { gainCache[trackId] }
        if (cached != null) {
            applyComputedReplayGain(cached.first, cached.second)
            return
        }
        val db = dbRef ?: return
        replayGainJob = scope?.launch {
            val entity = withContext(Dispatchers.IO) { runCatching { db.tracks().byId(trackId) }.getOrNull() }
            if (exo?.currentMediaItem?.mediaId != trackId) return@launch
            val pair = (entity?.replayGainTrackDb ?: 0f) to (entity?.replayGainAlbumDb ?: 0f)
            synchronized(gainCache) { gainCache[trackId] = pair }
            applyComputedReplayGain(pair.first, pair.second)
        }
    }

    private fun applyComputedReplayGain(trackDb: Float, albumDb: Float) {
        val adj = ReplayGainCalculator.compute(
            mode = replayGainMode,
            trackGainDb = trackDb,
            albumGainDb = albumDb,
            preampDb = replayGainPreampDb
        )
        transitions.setReplayGainScale(adj.volumeScale)
        AudioFx.setReplayGainBoostMb(adj.loudnessBoostMb)
        appliedReplayGainDb.value = adj.appliedDb
    }

    private fun replayGainScaleForCached(trackId: String?): Float {
        if (trackId.isNullOrBlank() || replayGainMode == ReplayGainMode.OFF) return 1f
        val pair = synchronized(gainCache) { gainCache[trackId] } ?: return 1f
        return ReplayGainCalculator.compute(
            mode = replayGainMode,
            trackGainDb = pair.first,
            albumGainDb = pair.second,
            preampDb = replayGainPreampDb
        ).volumeScale
    }

    private fun updateCrossfadeMonitor(playing: Boolean) {
        crossfadeJob?.cancel()
        crossfadeJob = null
        if (!playing || crossfadeMs < 1000L || secondaryDeckFactory == null) {
            cancelCrossfadeDeck(restoreVolume = true)
            return
        }
        crossfadeJob = scope?.launch {
            while (isActive) {
                val p = exo ?: break
                val cfMs = crossfadeMs
                if (!p.isPlaying || cfMs < 1000L) {
                    cancelCrossfadeDeck(restoreVolume = true)
                    break
                }
                val dur = p.duration
                val curItem = p.currentMediaItem
                val nextIdx = p.nextMediaItemIndex
                val nextItem = if (nextIdx != C.INDEX_UNSET && nextIdx in 0 until p.mediaItemCount) {
                    p.getMediaItemAt(nextIdx)
                } else {
                    null
                }
                val sameAlbumGapless = nextItem != null && DspMath.shouldBypassCrossfadeForAlbum(
                    smartAlbumGapless = smartAlbumGapless,
                    currentAlbum = curItem?.mediaMetadata?.albumTitle?.toString(),
                    nextAlbum = nextItem.mediaMetadata.albumTitle?.toString()
                )
                val canCrossfade = !sameAlbumGapless &&
                    dur != C.TIME_UNSET &&
                    dur > cfMs * 2L &&
                    nextItem != null &&
                    p.repeatMode != Player.REPEAT_MODE_ONE &&
                    (!::sleep.isInitialized || transitions.activeReason != TransitionReason.SleepTimer)

                if (!canCrossfade) {
                    if (deckB != null) cancelCrossfadeDeck(restoreVolume = true)
                    delay(250L)
                    continue
                }

                val remaining = (dur - p.currentPosition).coerceAtLeast(0L)
                val nextId = nextItem!!.mediaId

                // Pre-buffer secondary deck shortly before the crossfade window opens
                if (remaining <= cfMs + 1600L && remaining > 0L) {
                    if (deckB != null && (deckBTargetIndex != nextIdx || deckBTargetId != nextId)) {
                        cancelCrossfadeDeck(restoreVolume = true)
                    }
                    if (deckB == null) {
                        val factory = secondaryDeckFactory
                        if (factory != null) {
                            val b = runCatching {
                                factory().apply {
                                    volume = 0f
                                    playbackParameters = p.playbackParameters
                                    setMediaItem(nextItem)
                                    prepare()
                                    playWhenReady = false
                                }
                            }.getOrNull()
                            if (b != null) {
                                deckB = b
                                deckBTargetIndex = nextIdx
                                deckBTargetId = nextId
                                // Warm ReplayGain lookup for the upcoming track
                                val db = dbRef
                                if (db != null && replayGainMode != ReplayGainMode.OFF &&
                                    synchronized(gainCache) { gainCache[nextId] } == null
                                ) {
                                    launch(Dispatchers.IO) {
                                        val row = runCatching { db.tracks().byId(nextId) }.getOrNull()
                                        if (row != null) {
                                            synchronized(gainCache) {
                                                gainCache[nextId] = row.replayGainTrackDb to row.replayGainAlbumDb
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else if (deckB != null) {
                    cancelCrossfadeDeck(restoreVolume = true)
                }

                if (remaining <= cfMs && deckB != null) {
                    val b = deckB!!
                    if (!b.isPlaying && (b.playbackState == Player.STATE_READY || b.playbackState == Player.STATE_BUFFERING)) {
                        b.volume = 0f
                        b.play()
                    }
                    val progress = ((cfMs - remaining).toFloat() / cfMs.toFloat()).coerceIn(0f, 1f)
                    val gains = ReplayGainCalculator.crossfadeGains(progress, crossfadeEqualPower)
                    transitions.snapTo(gains.outgoing)
                    val incomingScale = replayGainScaleForCached(nextId)
                    b.volume = (transitions.userVolume * gains.incoming * incomingScale).coerceIn(0f, 1f)

                    if (progress >= 0.985f || remaining <= 55L) {
                        isCrossfadeHandoff = true
                        try {
                            val handoffPos = b.currentPosition.coerceAtLeast(0L)
                            val targetIndex = deckBTargetIndex
                            runCatching {
                                b.pause()
                                b.stop()
                                b.release()
                            }
                            deckB = null
                            deckBTargetIndex = C.INDEX_UNSET
                            deckBTargetId = null

                            if (targetIndex in 0 until p.mediaItemCount) {
                                p.seekTo(targetIndex, handoffPos)
                            }
                            transitions.snapTo(1f)
                            applyReplayGainForTrack(p.currentMediaItem?.mediaId)
                        } finally {
                            isCrossfadeHandoff = false
                        }
                        delay(250L)
                        continue
                    }
                    delay(40L)
                } else {
                    delay(180L)
                }
            }
        }
    }

    private fun cancelCrossfadeDeck(restoreVolume: Boolean) {
        val b = deckB
        if (b != null) {
            runCatching {
                b.pause()
                b.stop()
                b.release()
            }
            deckB = null
        }
        deckBTargetIndex = C.INDEX_UNSET
        deckBTargetId = null
        if (restoreVolume && ::transitions.isInitialized && transitions.activeReason == null && transitions.fadeMultiplier < 1f) {
            transitions.snapTo(1f)
        }
    }

    private fun updateTrackerTicker(playing: Boolean) {
        trackerTickerJob?.cancel()
        if (!playing) return
        trackerTickerJob = scope?.launch {
            while (isActive) {
                delay(15_000L)
                if (::tracker.isInitialized) {
                    tracker.checkpoint()
                }
            }
        }
    }

    fun detach() {
        if (exo == null) return
        crossfadeJob?.cancel()
        crossfadeJob = null
        replayGainJob?.cancel()
        replayGainJob = null
        cancelCrossfadeDeck(restoreVolume = false)
        trackerTickerJob?.cancel()
        trackerTickerJob = null
        if (::tracker.isInitialized) {
            tracker.onDestroy()
        }
        if (::transitions.isInitialized) {
            transitions.restore()
        }
        scope?.cancel()
        scope = null
        dbRef = null
        secondaryDeckFactory = null
        AudioFx.release()
        exo?.release()
        exo = null
        fadingPlayer = null
        appliedReplayGainDb.value = 0f
    }
}

