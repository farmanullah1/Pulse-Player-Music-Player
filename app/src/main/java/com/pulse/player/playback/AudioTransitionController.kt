package com.pulse.player.playback

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class TransitionReason(val defaultMs: Long) {
    PlayStart(350),
    PauseEnd(250),
    SleepTimer(10_000),
    AudioFocusLoss(150),
    AudioFocusGain(300),
    TrackChange(200)
}

/** The ONLY writer of player volume. effective = userVolume * fadeMultiplier. */
class AudioTransitionController(
    private val scope: CoroutineScope,
    private val clock: () -> Long = { System.currentTimeMillis() },
    private val sink: (Float) -> Unit // sink = { exo.volume = it }
) {
    var userVolume = 1f
        private set
    var fadeMultiplier = 1f
        private set
    var activeReason: TransitionReason? = null
        private set
    val effectiveVolume: Float
        get() = (userVolume * fadeMultiplier).coerceIn(0f, 1f)

    private var job: Job? = null

    private fun publish() = sink(effectiveVolume)

    fun setUserVolume(v: Float) {
        userVolume = v.coerceIn(0f, 1f)
        publish()
    }

    /** One transition at a time. A new request cancels the old one and starts from the CURRENT audible level. */
    fun fadeTo(target: Float, durationMs: Long, reason: TransitionReason, onDone: (() -> Unit)? = null) {
        job?.cancel()
        val from = fadeMultiplier
        val to = target.coerceIn(0f, 1f)
        if (durationMs <= 0 || from == to) {
            fadeMultiplier = to
            activeReason = null
            publish()
            onDone?.invoke()
            return
        }
        activeReason = reason
        job = scope.launch {
            val start = clock()
            while (true) {
                val t = ((clock() - start) / durationMs.toFloat()).coerceIn(0f, 1f)
                fadeMultiplier = from + (to - from) * (t * t * (3 - 2 * t)) // smoothstep
                publish()
                if (t >= 1f) break
                delay(16)
            }
            activeReason = null
            onDone?.invoke() // only runs if NOT cancelled
        }
    }

    fun snapTo(multiplier: Float) {
        job?.cancel()
        activeReason = null
        fadeMultiplier = multiplier.coerceIn(0f, 1f)
        publish()
    }

    /** Call on completion, cancel, error, pause, stop, focus loss, service destroy. Always leaves effective == userVolume. */
    fun restore() = snapTo(1f)
}

