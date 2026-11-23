package com.pulse.player.playback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class LoudnessMathTest {

    @Test
    fun `meanSquareToLufs handles silence and clipping bounds`() {
        assertEquals(-70.0f, LoudnessMath.meanSquareToLufs(0.0), 0.01f)
        assertEquals(-70.0f, LoudnessMath.meanSquareToLufs(Double.NaN), 0.01f)
        assertEquals(0.0f, LoudnessMath.meanSquareToLufs(1.0), 0.01f)
        // -20 dB RMS -> meanSquare = 0.01 -> ~ -20.0 LUFS
        assertEquals(-20.0f, LoudnessMath.meanSquareToLufs(0.01), 0.1f)
    }

    @Test
    fun `pcmSamplesToLufs measures synthetic wave accurately`() {
        val silence = ShortArray(1024) { 0 }
        assertEquals(-70.0f, LoudnessMath.pcmSamplesToLufs(silence), 0.01f)

        // Constant amplitude at half full scale (~16384 -> -6.02 dBFS)
        val halfScale = ShortArray(1024) { 16384 }
        val lufs = LoudnessMath.pcmSamplesToLufs(halfScale)
        assertEquals(-6.02f, lufs, 0.2f)
    }

    @Test
    fun `lufsToReplayGainDb references -18 LUFS and clamps to -12 to +9 dB`() {
        assertEquals(0.0f, LoudnessMath.lufsToReplayGainDb(-18.0f), 0.01f)
        assertEquals(0.0f, LoudnessMath.lufsToReplayGainDb(-66.0f), 0.01f)
        assertEquals(0.0f, LoudnessMath.lufsToReplayGainDb(Float.NaN), 0.01f)
        assertEquals(-12.0f, LoudnessMath.lufsToReplayGainDb(-1.0f), 0.01f)
        assertEquals(9.0f, LoudnessMath.lufsToReplayGainDb(-35.0f), 0.01f)
        assertEquals(4.5f, LoudnessMath.lufsToReplayGainDb(-22.5f), 0.01f)
    }

    @Test
    fun `computeAlbumGainDb ignores zeroes and averages measured gains`() {
        assertEquals(0.0f, LoudnessMath.computeAlbumGainDb(emptyList()), 0.01f)
        assertEquals(0.0f, LoudnessMath.computeAlbumGainDb(listOf(0f, 0f)), 0.01f)

        val avg = LoudnessMath.computeAlbumGainDb(listOf(-4.0f, 0.0f, -2.0f))
        assertTrue(abs(avg - (-3.0f)) < 0.01f)
    }
}
