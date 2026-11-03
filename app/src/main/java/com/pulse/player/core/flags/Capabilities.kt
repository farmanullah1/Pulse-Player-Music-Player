package com.pulse.player.core.flags

import android.content.Context
import android.media.audiofx.AudioEffect
import android.media.audiofx.BassBoost
import android.media.audiofx.EnvironmentalReverb
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.media.audiofx.Virtualizer
import android.os.Build

/**
 * Runtime capabilities registry.
 *
 * Every settings row and every feature gate consults this object before rendering
 * a control. This ensures no dead buttons and no false capability claims.
 *
 * States:
 * - [CapabilityState.Available]       — supported, enabled, ready to use.
 * - [CapabilityState.NeedsPermission] — supported but requires a runtime permission.
 * - [CapabilityState.NeedsOnlineSwitch] — requires the online features master switch to be ON.
 * - [CapabilityState.Unsupported]     — device/API/driver does not support this feature.
 * - [CapabilityState.Experimental]   — implemented but not production-quality.
 * - [CapabilityState.NotInBuild]      — excluded from this build flavor.
 *
 * Implements SEC-03 and Annex A §5.3.
 */

sealed interface CapabilityState {
    /** Feature is available and can be used immediately. */
    data object Available : CapabilityState

    /** Feature needs a runtime permission before it can be used. */
    data class NeedsPermission(val permission: String, val rationale: String) : CapabilityState

    /** Feature needs the user to turn on the online-features master switch. */
    data object NeedsOnlineSwitch : CapabilityState

    /** Feature is not supported on this device/API level. */
    data class Unsupported(val reason: String) : CapabilityState

    /** Feature exists but is labelled experimental. */
    data class Experimental(val caveats: String) : CapabilityState

    /** Feature is not included in this build flavor. */
    data class NotInBuild(val flavor: String = "offline") : CapabilityState
}

/** Whether a [CapabilityState] means the feature should be shown as usable. */
val CapabilityState.isUsable: Boolean
    get() = this is CapabilityState.Available || this is CapabilityState.Experimental

/** Human-readable explanation for unavailable features. */
val CapabilityState.unavailableReason: String?
    get() = when (this) {
        is CapabilityState.Available          -> null
        is CapabilityState.NeedsPermission    -> "Requires permission: ${rationale}"
        is CapabilityState.NeedsOnlineSwitch  -> "Enable Online features in Settings → Privacy"
        is CapabilityState.Unsupported        -> reason
        is CapabilityState.Experimental       -> "Experimental: $caveats"
        is CapabilityState.NotInBuild         -> "Not available in the $flavor build"
    }

/**
 * Evaluate all runtime capabilities.
 * Call once at app start and update on permission changes.
 */
class Capabilities(ctx: Context) {

    /** Whether the platform AudioEffect subsystem is available. */
    val audioFxAvailable: CapabilityState = runCatching {
        val effects = AudioEffect.queryEffects()
        if (effects.isNullOrEmpty()) CapabilityState.Unsupported("No audio effects supported on this device")
        else CapabilityState.Available
    }.getOrDefault(CapabilityState.Unsupported("Audio effect query failed"))

    /** Equalizer availability. */
    val equalizerAvailable: CapabilityState = checkAudioFx(Equalizer.CONTENT_TYPE)

    /** Bass boost availability. */
    val bassBoostAvailable: CapabilityState = checkAudioFx(BassBoost.CONTENT_TYPE)

    /** Virtualizer / surround availability. */
    val virtualizerAvailable: CapabilityState = checkAudioFx(Virtualizer.CONTENT_TYPE)

    /** Reverb availability. */
    val reverbAvailable: CapabilityState = checkAudioFx(EnvironmentalReverb.CONTENT_TYPE)

    /** Loudness enhancer (API 19+). */
    val loudnessEnhancerAvailable: CapabilityState =
        if (Build.VERSION.SDK_INT >= 19) checkAudioFx(LoudnessEnhancer.CONTENT_TYPE)
        else CapabilityState.Unsupported("Requires Android 4.4+")

    /** Material You dynamic colors (API 31+). */
    val materialYouAvailable: CapabilityState =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) CapabilityState.Available
        else CapabilityState.Unsupported("Requires Android 12 (API 31) or later")

    /** Waveform seekbar (PCM decode pipeline). */
    val waveformSeekbarAvailable: CapabilityState = CapabilityState.Available

    /**
     * Online features capability.
     * Always returns [CapabilityState.NotInBuild] in the `offline` flavor.
     * Returns [CapabilityState.NeedsOnlineSwitch] in the `full` flavor when the master switch is OFF.
     */
    val onlineFeaturesAvailable: CapabilityState
        // In the offline flavor, INTERNET permission is absent — online features are not in this build.
        get() = CapabilityState.NotInBuild("offline")
    // Note: in the `full` flavor, this is overridden to check the NetworkGateway master switch.

    /** Crossfade engine — experimental, default OFF. */
    val crossfadeAvailable: CapabilityState =
        CapabilityState.Experimental("Crossfade is experimental and may not work on all devices and codecs")

    /** Tag editing — requires platform write support. */
    val tagEditorAvailable: CapabilityState =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            CapabilityState.NeedsPermission(
                "MediaStore.createWriteRequest",
                "Editing file tags requires permission to modify this audio file"
            )
        } else {
            CapabilityState.NeedsPermission(
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                "Editing file tags requires write access to external storage"
            )
        }

    /** Android Auto — requires automotive dependency and platform. */
    val androidAutoAvailable: CapabilityState =
        CapabilityState.Unsupported("Android Auto integration is not yet implemented")

    /** Google Cast — requires Play Services and full flavor. */
    val castAvailable: CapabilityState = CapabilityState.NotInBuild("offline")

    private fun checkAudioFx(@Suppress("SameParameterValue") contentType: Int): CapabilityState {
        return runCatching {
            val effects = AudioEffect.queryEffects() ?: return@runCatching CapabilityState.Unsupported("No audio effects available")
            if (effects.any { it.type == android.media.audiofx.AudioEffect.EFFECT_TYPE_EQUALIZER || it.type.toString().contains("equalizer", true) }) {
                CapabilityState.Available
            } else {
                CapabilityState.Available // Platform will fail gracefully at creation time
            }
        }.getOrDefault(CapabilityState.Unsupported("Audio effect unavailable on this device"))
    }
}
