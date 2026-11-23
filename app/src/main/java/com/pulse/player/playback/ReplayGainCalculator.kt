package com.pulse.player.playback

import com.pulse.player.data.settings.ReplayGainMode
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin

data class ReplayGainAdjustment(
    val volumeScale: Float,
    val loudnessBoostMb: Int,
    val appliedDb: Float
)

data class CrossfadeGains(
    val outgoing: Float,
    val incoming: Float
)

object ReplayGainCalculator {
    private val gainNumberRegex = Regex("""([+-]?\d+(?:[.,]\d+)?)""")

    fun parseGainDb(raw: String?): Float? {
        if (raw.isNullOrBlank()) return null
        val match = gainNumberRegex.find(raw) ?: return null
        val normalized = match.groupValues[1].replace(',', '.')
        val value = normalized.toFloatOrNull() ?: return null
        if (value.isNaN() || value.isInfinite()) return null
        return value.coerceIn(-24f, 24f)
    }

    fun compute(
        mode: ReplayGainMode,
        trackGainDb: Float,
        albumGainDb: Float,
        preampDb: Float
    ): ReplayGainAdjustment {
        if (mode == ReplayGainMode.OFF) {
            return ReplayGainAdjustment(volumeScale = 1f, loudnessBoostMb = 0, appliedDb = 0f)
        }
        val baseDb = when (mode) {
            ReplayGainMode.TRACK -> if (trackGainDb != 0f) trackGainDb else albumGainDb
            ReplayGainMode.ALBUM -> if (albumGainDb != 0f) albumGainDb else trackGainDb
            ReplayGainMode.OFF -> 0f
        }
        if (baseDb == 0f && preampDb == 0f) {
            return ReplayGainAdjustment(volumeScale = 1f, loudnessBoostMb = 0, appliedDb = 0f)
        }
        val effectiveDb = (baseDb + preampDb).coerceIn(-18f, 6f)
        return if (effectiveDb <= 0f) {
            val scale = 10f.pow(effectiveDb / 20f).coerceIn(0.05f, 1f)
            ReplayGainAdjustment(
                volumeScale = scale,
                loudnessBoostMb = 0,
                appliedDb = effectiveDb
            )
        } else {
            // Positive gain uses hardware LoudnessEnhancer millibels with limiter headroom cap
            val boostMb = (effectiveDb * 100f).roundToInt().coerceIn(0, 600)
            ReplayGainAdjustment(
                volumeScale = 1f,
                loudnessBoostMb = boostMb,
                appliedDb = boostMb / 100f
            )
        }
    }

    fun crossfadeGains(progress: Float, equalPower: Boolean = true): CrossfadeGains {
        val t = progress.coerceIn(0f, 1f)
        return if (equalPower) {
            val angle = t * (PI / 2.0)
            CrossfadeGains(
                outgoing = cos(angle).toFloat().coerceIn(0f, 1f),
                incoming = sin(angle).toFloat().coerceIn(0f, 1f)
            )
        } else {
            CrossfadeGains(
                outgoing = (1f - t).coerceIn(0f, 1f),
                incoming = t.coerceIn(0f, 1f)
            )
        }
    }
}
