package com.pulse.player.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.sin

class WaveformSeekTest {

    private fun generateWaveBars(seedKey: String, barCount: Int): FloatArray {
        val hash = seedKey.hashCode().toLong()
        return FloatArray(barCount) { i ->
            val angle = i * 0.28f + (hash and 0xFF) * 0.05f
            val base = abs(sin(angle)) * 0.65f + abs(sin(angle * 2.3f)) * 0.35f
            base.coerceIn(0.12f, 1.0f)
        }
    }

    @Test
    fun waveformBarsCountMatchesRequested() {
        val bars = generateWaveBars("song_123", 48)
        assertEquals(48, bars.size)
    }

    @Test
    fun waveformBarsAreDeterministicForSameSeed() {
        val bars1 = generateWaveBars("track_abc", 32)
        val bars2 = generateWaveBars("track_abc", 32)

        for (i in bars1.indices) {
            assertEquals("Bars must be identical for same track id", bars1[i], bars2[i], 0.0001f)
        }
    }

    @Test
    fun waveformBarsStayWithinValidHeightRange() {
        val bars = generateWaveBars("random_track", 64)
        for (b in bars) {
            assertTrue("Wave bar must be >= 0.12f and <= 1.0f: $b", b in 0.12f..1.0f)
        }
    }
}

