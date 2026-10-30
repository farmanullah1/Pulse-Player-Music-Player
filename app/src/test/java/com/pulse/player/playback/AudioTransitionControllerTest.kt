package com.pulse.player.playback

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AudioTransitionControllerTest {

    @Test
    fun effectiveVolume_equalsUserVolumeMultipliedByFade() {
        val testScope = TestScope()
        var currentSinkVolume = -1f
        val controller = AudioTransitionController(
            scope = testScope,
            sink = { currentSinkVolume = it }
        )

        controller.setUserVolume(0.8f)
        assertEquals(0.8f, controller.userVolume, 0.001f)
        assertEquals(1.0f, controller.fadeMultiplier, 0.001f)
        assertEquals(0.8f, controller.effectiveVolume, 0.001f)
        assertEquals(0.8f, currentSinkVolume, 0.001f)

        controller.snapTo(0.5f)
        assertEquals(0.4f, controller.effectiveVolume, 0.001f)
        assertEquals(0.4f, currentSinkVolume, 0.001f)
    }

    @Test
    fun newFade_cancelsPreviousFade() = runTest {
        var currentSinkVolume = -1f
        var virtualTime = 0L
        val controller = AudioTransitionController(
            scope = this,
            clock = { virtualTime },
            sink = { currentSinkVolume = it }
        )

        var firstDone = false
        controller.fadeTo(0.0f, 1000L, TransitionReason.PauseEnd) {
            firstDone = true
        }

        // Advance 200ms
        virtualTime = 200L
        testScheduler.advanceTimeBy(200)

        // New fade requested
        var secondDone = false
        controller.fadeTo(1.0f, 500L, TransitionReason.PlayStart) {
            secondDone = true
        }

        // Advance beyond the first 1000ms duration
        virtualTime = 1200L
        testScheduler.advanceTimeBy(1000)

        // The first onDone must never have executed because it was cancelled
        assertEquals(false, firstDone)
        assertEquals(true, secondDone)
        assertEquals(1.0f, controller.fadeMultiplier, 0.001f)
    }

    @Test
    fun restore_returnsToUserVolume() {
        val testScope = TestScope()
        var currentSinkVolume = -1f
        val controller = AudioTransitionController(
            scope = testScope,
            sink = { currentSinkVolume = it }
        )

        controller.setUserVolume(0.75f)
        controller.snapTo(0.2f)
        assertEquals(0.15f, controller.effectiveVolume, 0.001f)

        controller.restore()
        assertEquals(1.0f, controller.fadeMultiplier, 0.001f)
        assertEquals(0.75f, controller.effectiveVolume, 0.001f)
        assertEquals(0.75f, currentSinkVolume, 0.001f)
        assertNull(controller.activeReason)
    }

    @Test
    fun userVolumeChange_duringFade_isRespected() = runTest {
        var currentSinkVolume = -1f
        var virtualTime = 0L
        val controller = AudioTransitionController(
            scope = this,
            clock = { virtualTime },
            sink = { currentSinkVolume = it }
        )

        controller.setUserVolume(1.0f)
        controller.fadeTo(0.0f, 1000L, TransitionReason.PauseEnd)

        // Mid-fade at 500ms
        virtualTime = 500L
        testScheduler.advanceTimeBy(500)

        // User changes hardware slider to 0.5
        controller.setUserVolume(0.5f)
        assertEquals(0.5f, controller.userVolume, 0.001f)
        assertTrue(controller.effectiveVolume <= 0.5f)

        // Finish fade
        virtualTime = 1000L
        testScheduler.advanceTimeBy(500)
        assertEquals(0.0f, controller.effectiveVolume, 0.001f)
    }
}

