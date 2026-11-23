package com.pulse.player.playback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DspMathTest {

    @Test
    fun `resampleBands upsamples 5-band profile to 10-band curve smoothly`() {
        val fiveBand = listOf(400, 0, -200, 300, 600)
        val tenBand = DspMath.resampleBands(fiveBand, 10)
        assertEquals(10, tenBand.size)
        assertEquals(400, tenBand.first())
        assertEquals(600, tenBand.last())
        assertTrue(tenBand.all { it in -1500..1500 })
    }

    @Test
    fun `resampleBands downsamples 10-band profile to 5-band fallback and clamps out-of-range`() {
        val extreme = listOf(3000, 1200, 600, 0, -400, -400, 0, 500, 1200, -3000)
        val five = DspMath.resampleBands(extreme, 5)
        assertEquals(5, five.size)
        assertEquals(1500, five.first())
        assertEquals(-1500, five.last())
    }

    @Test
    fun `resampleBands handles empty and single-element inputs cleanly`() {
        assertEquals(List(10) { 0 }, DspMath.resampleBands(emptyList(), 10))
        assertEquals(List(5) { 250 }, DspMath.resampleBands(listOf(250), 5))
        assertTrue(DspMath.resampleBands(listOf(100, 200), 0).isEmpty())
    }

    @Test
    fun `channelInputGainsDb attenuates opposite channel cleanly`() {
        val (centerL, centerR) = DspMath.channelInputGainsDb(0f)
        assertEquals(0f, centerL, 0.001f)
        assertEquals(0f, centerR, 0.001f)

        val (leftFocusL, leftFocusR) = DspMath.channelInputGainsDb(-1f, maxAttenuationDb = 18f)
        assertEquals(0f, leftFocusL, 0.001f)
        assertEquals(-18f, leftFocusR, 0.001f)

        val (rightFocusL, rightFocusR) = DspMath.channelInputGainsDb(0.5f, maxAttenuationDb = 18f)
        assertEquals(-9f, rightFocusL, 0.001f)
        assertEquals(0f, rightFocusR, 0.001f)
    }

    @Test
    fun `shouldBypassCrossfadeForAlbum triggers only when enabled and non-unknown same album`() {
        assertTrue(DspMath.shouldBypassCrossfadeForAlbum(true, "Dark Side of the Moon", "dark side of the moon "))
        assertFalse(DspMath.shouldBypassCrossfadeForAlbum(false, "Dark Side", "Dark Side"))
        assertFalse(DspMath.shouldBypassCrossfadeForAlbum(true, "Album A", "Album B"))
        assertFalse(DspMath.shouldBypassCrossfadeForAlbum(true, "Unknown album", "Unknown album"))
        assertFalse(DspMath.shouldBypassCrossfadeForAlbum(true, "", ""))
    }
}
