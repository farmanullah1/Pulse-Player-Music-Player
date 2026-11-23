package com.pulse.player.playback

import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Pure-Kotlin DSP math helpers for 5-band ↔ 10-band EQ resampling,
 * stereo L/R balance attenuation, and Smart Album Gapless detection.
 */
object DspMath {
    val TEN_BAND_FREQUENCIES_HZ = intArrayOf(31, 62, 125, 250, 500, 1000, 2000, 4000, 8000, 16000)
    val TEN_BAND_CUTOFFS_HZ = floatArrayOf(45f, 90f, 180f, 360f, 720f, 1450f, 2900f, 5800f, 11600f, 20000f)

    /**
     * Smoothly resamples EQ band levels between any source band count (e.g. 5-band legacy)
     * and any target band count (e.g. 10-band DynamicsProcessing) using linear interpolation.
     */
    fun resampleBands(source: List<Int>, targetCount: Int, minMb: Int = -1500, maxMb: Int = 1500): List<Int> {
        if (targetCount <= 0) return emptyList()
        if (source.isEmpty()) return List(targetCount) { 0 }
        if (source.size == targetCount) return source.map { it.coerceIn(minMb, maxMb) }
        if (source.size == 1) return List(targetCount) { source[0].coerceIn(minMb, maxMb) }
        if (targetCount == 1) return listOf(source.first().coerceIn(minMb, maxMb))

        return List(targetCount) { i ->
            val pos = (i.toFloat() / (targetCount - 1).toFloat()) * (source.size - 1).toFloat()
            val loIdx = pos.toInt().coerceIn(0, source.lastIndex)
            val hiIdx = (loIdx + 1).coerceIn(0, source.lastIndex)
            val frac = (pos - loIdx).coerceIn(0f, 1f)
            val interpolated = source[loIdx] + (source[hiIdx] - source[loIdx]) * frac
            interpolated.roundToInt().coerceIn(minMb, maxMb)
        }
    }

    /**
     * Converts L/R balance in [-1f, +1f] to (leftGainDb, rightGainDb).
     * Centered (0f) returns (0f, 0f). Full left (-1f) attenuates right channel by -18 dB.
     */
    fun channelInputGainsDb(balance: Float, maxAttenuationDb: Float = 18f): Pair<Float, Float> {
        val b = balance.coerceIn(-1f, 1f)
        if (abs(b) < 0.01f) return 0f to 0f
        return if (b < 0f) {
            0f to (b * maxAttenuationDb)
        } else {
            (-b * maxAttenuationDb) to 0f
        }
    }

    /**
     * Bypasses crossfade when Smart Album Gapless is active and consecutive tracks share
     * the same non-empty album title (e.g. live concerts or continuous concept albums).
     */
    fun shouldBypassCrossfadeForAlbum(
        smartAlbumGapless: Boolean,
        currentAlbum: String?,
        nextAlbum: String?
    ): Boolean {
        if (!smartAlbumGapless) return false
        val cur = currentAlbum?.trim().orEmpty()
        val nxt = nextAlbum?.trim().orEmpty()
        if (cur.isEmpty() || nxt.isEmpty()) return false
        if (cur.equals("Unknown album", ignoreCase = true)) return false
        return cur.equals(nxt, ignoreCase = true)
    }
}
