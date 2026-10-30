package com.pulse.player.playback

import androidx.media3.common.Player
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SleepTimerEngine(
    private val scope: CoroutineScope,
    private val raw: Player,
    private val t: AudioTransitionController,
    private val clock: () -> Long = { System.currentTimeMillis() }
) {
    data class State(val remainingMs: Long = 0, val endOfTrack: Boolean = false)
    private val _state = MutableStateFlow(State())
    val state = _state.asStateFlow()

    private var job: Job? = null
    companion object { const val FADE_MS = 10_000L }

    fun start(minutes: Int) {
        cancel()
        val end = clock() + minutes * 60_000L
        job = scope.launch {
            var faded = false
            while (true) {
                val left = end - clock()
                _state.value = State(remainingMs = left.coerceAtLeast(0))
                if (left <= 0) break
                if (!faded && left <= FADE_MS) {
                    faded = true
                    t.fadeTo(0f, left, TransitionReason.SleepTimer)
                }
                delay(250)
            }
            raw.pause()
            t.restore()
            _state.value = State()
        }
    }

    fun startEndOfTrack() {
        cancel()
        raw.pauseAtEndOfMediaItems = true
        _state.value = State(endOfTrack = true)
    }

    fun onPausedAtEndOfItem() {
        if (_state.value.endOfTrack) {
            raw.pauseAtEndOfMediaItems = false
            t.restore()
            _state.value = State()
        }
    }

    fun cancel() {
        job?.cancel()
        job = null
        raw.pauseAtEndOfMediaItems = false
        t.restore()
        _state.value = State()
    }
}

