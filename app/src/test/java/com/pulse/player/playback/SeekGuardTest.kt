package com.pulse.player.playback

import androidx.media3.common.C
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SeekGuardTest {

    private fun validateSeek(
        isSeekable: Boolean,
        duration: Long,
        targetMs: Long
    ): Boolean {
        if (!isSeekable || duration == C.TIME_UNSET || duration <= 0 || targetMs < 0 || targetMs > duration) {
            return false
        }
        return true
    }

    private fun clampSkip(
        currentPosition: Long,
        deltaMs: Long,
        duration: Long
    ): Long? {
        if (duration == C.TIME_UNSET || duration <= 0) return null
        return (currentPosition + deltaMs).coerceIn(0L, duration)
    }

    @Test
    fun unknownDuration_returnsFalse() {
        assertFalse(validateSeek(isSeekable = true, duration = C.TIME_UNSET, targetMs = 5000L))
        assertFalse(validateSeek(isSeekable = true, duration = -1L, targetMs = 5000L))
        assertFalse(validateSeek(isSeekable = true, duration = 0L, targetMs = 0L))
    }

    @Test
    fun unseekableItem_returnsFalse() {
        assertFalse(validateSeek(isSeekable = false, duration = 180_000L, targetMs = 10_000L))
    }

    @Test
    fun outOfBounds_returnsFalse() {
        val duration = 120_000L
        assertFalse(validateSeek(isSeekable = true, duration = duration, targetMs = -1L))
        assertFalse(validateSeek(isSeekable = true, duration = duration, targetMs = 120_001L))
        assertTrue(validateSeek(isSeekable = true, duration = duration, targetMs = 0L))
        assertTrue(validateSeek(isSeekable = true, duration = duration, targetMs = duration))
        assertTrue(validateSeek(isSeekable = true, duration = duration, targetMs = 60_000L))
    }

    @Test
    fun relativeSkip_clampsToZeroAndDuration() {
        val duration = 100_000L

        // Backward 10s from 5s clamps to 0
        val clampedBack = clampSkip(currentPosition = 5000L, deltaMs = -10_000L, duration = duration)
        assertEquals(0L, clampedBack)

        // Forward 10s from 95s clamps to 100s
        val clampedForward = clampSkip(currentPosition = 95_000L, deltaMs = 10_000L, duration = duration)
        assertEquals(100_000L, clampedForward)

        // Forward 10s from 50s becomes 60s
        val normalForward = clampSkip(currentPosition = 50_000L, deltaMs = 10_000L, duration = duration)
        assertEquals(60_000L, normalForward)
    }
}

