package com.pulse.player.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SeekStyleTest {

    enum class TestSeekStyle { WAVEFORM, WAVY, LINEAR }

    private fun nextStyle(current: TestSeekStyle): TestSeekStyle =
        when (current) {
            TestSeekStyle.WAVEFORM -> TestSeekStyle.WAVY
            TestSeekStyle.WAVY -> TestSeekStyle.LINEAR
            TestSeekStyle.LINEAR -> TestSeekStyle.WAVEFORM
        }

    @Test
    fun seekStyleCyclesInCorrectOrder() {
        var style = TestSeekStyle.WAVEFORM
        style = nextStyle(style)
        assertEquals(TestSeekStyle.WAVY, style)

        style = nextStyle(style)
        assertEquals(TestSeekStyle.LINEAR, style)

        style = nextStyle(style)
        assertEquals(TestSeekStyle.WAVEFORM, style)
    }

    @Test
    fun swipeThresholdDetectsDirectionCorrectly() {
        val swipeThreshold = 80f

        fun resolveSwipeAction(dragDelta: Float): String? =
            when {
                dragDelta < -swipeThreshold -> "NEXT"
                dragDelta > swipeThreshold -> "PREVIOUS"
                else -> null
            }

        assertEquals("NEXT", resolveSwipeAction(-95f))
        assertEquals("PREVIOUS", resolveSwipeAction(110f))
        assertEquals(null, resolveSwipeAction(40f))
        assertEquals(null, resolveSwipeAction(-30f))
    }
}
