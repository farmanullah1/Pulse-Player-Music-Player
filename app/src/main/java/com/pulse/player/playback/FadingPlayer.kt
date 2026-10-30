package com.pulse.player.playback

import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.Player

class FadingPlayer(
    private val raw: Player,
    private val t: AudioTransitionController,
    private val sleep: SleepTimerEngine,
    private val fadeEnabled: () -> Boolean
) : ForwardingPlayer(raw) {

    override fun play() = startPlayback()

    override fun setPlayWhenReady(playWhenReady: Boolean) {
        if (playWhenReady) startPlayback() else pause()
    }

    private fun startPlayback() {
        if (t.activeReason == TransitionReason.SleepTimer) sleep.cancel()
        if (!fadeEnabled()) {
            t.restore()
            raw.play()
            return
        }
        when {
            t.activeReason == TransitionReason.PauseEnd -> {
                raw.play()
                t.fadeTo(1f, TransitionReason.PlayStart.defaultMs, TransitionReason.PlayStart)
            }
            !raw.isPlaying -> {
                t.snapTo(0f)
                raw.play()
                t.fadeTo(1f, TransitionReason.PlayStart.defaultMs, TransitionReason.PlayStart)
            }
            else -> t.fadeTo(1f, TransitionReason.PlayStart.defaultMs, TransitionReason.PlayStart)
        }
    }

    override fun pause() {
        if (!fadeEnabled() || !raw.isPlaying) {
            raw.pause()
            t.restore()
            return
        }
        t.fadeTo(0f, TransitionReason.PauseEnd.defaultMs, TransitionReason.PauseEnd) {
            raw.pause()
            t.restore()
        }
    }

    override fun stop() {
        raw.stop()
        t.restore()
    }

    override fun seekToNextMediaItem() = cut { raw.seekToNextMediaItem() }
    override fun seekToPreviousMediaItem() = cut { raw.seekToPreviousMediaItem() }

    private fun cut(op: () -> Unit) {
        if (!fadeEnabled() || !raw.isPlaying) {
            op()
            return
        }
        t.snapTo(0f)
        op()
        t.fadeTo(1f, TransitionReason.TrackChange.defaultMs, TransitionReason.TrackChange)
    }

    override fun setVolume(volume: Float) = t.setUserVolume(volume)
    override fun getVolume(): Float = t.userVolume
}

