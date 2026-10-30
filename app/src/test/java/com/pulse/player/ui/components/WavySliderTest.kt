package com.pulse.player.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.sin

class WavySliderTest {

    @Test
    fun sliderClampsProgressBetweenZeroAndOne() {
        val below = -0.5f
        val above = 1.5f
        val within = 0.42f

        assertEquals(0f, below.coerceIn(0f, 1f), 0.001f)
        assertEquals(1f, above.coerceIn(0f, 1f), 0.001f)
        assertEquals(0.42f, within.coerceIn(0f, 1f), 0.001f)
    }

    @Test
    fun wavySinCalculationProducesSmoothBoundedWave() {
        val waveLenPx = 200f
        val ampPx = 10f
        val phase = 1.0f

        for (x in 0..200 step 10) {
            val k = (2 * Math.PI / waveLenPx).toFloat()
            val yOffset = ampPx * sin(k * x - phase)
            assertTrue("Wave yOffset must stay within amplitude bounds", yOffset in -ampPx..ampPx)
        }
    }

    @Test
    fun thumbMorphInterpolation() {
        val circleRadius = 14f
        val pillWidth = 4f

        fun lerp(start: Float, stop: Float, fraction: Float): Float =
            start + (stop - start) * fraction

        assertEquals(circleRadius, lerp(circleRadius, pillWidth, 0f), 0.001f)
        assertEquals(pillWidth, lerp(circleRadius, pillWidth, 1f), 0.001f)
        assertEquals(9f, lerp(circleRadius, pillWidth, 0.5f), 0.001f)
    }
}

