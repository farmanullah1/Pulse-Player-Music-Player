package com.pulse.player.playback

import android.content.Context
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.pulse.player.data.db.PulseDb
import com.pulse.player.ui.widget.WidgetStateStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

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

    private var trackerTickerJob: Job? = null

    fun attach(ctx: Context, db: PulseDb, player: ExoPlayer) {
        detach()
        val s = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        scope = s
        exo = player
        transitions = AudioTransitionController(s) { player.volume = it }
        sleep = SleepTimerEngine(s, player, transitions)
        errors = ErrorPolicy(player)
        val prefs = ctx.getSharedPreferences("pulse_session_tracker", Context.MODE_PRIVATE)
        tracker = SessionTracker(db, prefs)
        s.launch(Dispatchers.IO) {
            tracker.recoverOpenSession()
        }
        fadingPlayer = FadingPlayer(player, transitions, sleep) { fadeEnabled }

        player.addListener(object : Player.Listener {
            override fun onAudioSessionIdChanged(id: Int) {
                if (id > 0) AudioFx.attach(id)
            }

            override fun onPlayWhenReadyChanged(pwr: Boolean, reason: Int) {
                if (!pwr) transitions.restore()
                if (!pwr && reason == Player.PLAY_WHEN_READY_CHANGE_REASON_END_OF_MEDIA_ITEM) {
                    sleep.onPausedAtEndOfItem()
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                tracker.onPlayingChanged(isPlaying, player.playbackParameters.speed)
                updateTrackerTicker(isPlaying)
                if (isPlaying) {
                    errors.onPlaying()
                    if (transitions.activeReason == null) transitions.restore()
                }
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                val id = mediaItem?.mediaId
                val dur = if (player.duration == C.TIME_UNSET || player.duration < 0) 0L else player.duration
                val speed = player.playbackParameters.speed
                if (id != null) {
                    tracker.onTrackStarted(id, dur, player.isPlaying, speed)
                } else {
                    tracker.onTrackLeft(userAction = false)
                }
            }

            override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters) {
                tracker.onSpeedChanged(playbackParameters.speed)
            }

            override fun onPositionDiscontinuity(
                oldPosition: Player.PositionInfo,
                newPosition: Player.PositionInfo,
                reason: Int
            ) {
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
                if (playbackState == Player.STATE_ENDED) {
                    tracker.onTrackLeft(userAction = false)
                }
            }

            override fun onPlayerError(e: PlaybackException) {
                errors.onError(e)
                tracker.onTrackLeft(userAction = false)
            }

            override fun onEvents(p: Player, events: Player.Events) {
                WidgetStateStore.write(ctx, p, s)
            }
        })

        if (player.audioSessionId > 0) {
            AudioFx.attach(player.audioSessionId)
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
        AudioFx.release()
        exo?.release()
        exo = null
        fadingPlayer = null
    }
}
