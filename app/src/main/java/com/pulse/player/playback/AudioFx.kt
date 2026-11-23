@file:Suppress("DEPRECATION")

package com.pulse.player.playback

import android.media.audiofx.AudioEffect
import android.media.audiofx.BassBoost
import android.media.audiofx.DynamicsProcessing
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.media.audiofx.PresetReverb
import android.media.audiofx.Virtualizer
import android.os.Build
import androidx.annotation.RequiresApi
import com.pulse.player.data.ErrorCategory
import com.pulse.player.data.ErrorDispatcher
import com.pulse.player.data.settings.SoundProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

data class AudioFxStatus(
    val eqSupported: Boolean = false,
    val tenBandSupported: Boolean = false,
    val limiterSupported: Boolean = false,
    val bassSupported: Boolean = false,
    val virtSupported: Boolean = false,
    val reverbSupported: Boolean = false,
    val loudnessSupported: Boolean = false,
    val monoSupported: Boolean = true,
    val sessionId: Int = 0
)

object AudioFx {
    private var dp: DynamicsProcessing? = null
    private var eq: Equalizer? = null
    private var bass: BassBoost? = null
    private var virt: Virtualizer? = null
    private var reverb: PresetReverb? = null
    private var loudness: LoudnessEnhancer? = null

    private var dpDisabled = false
    private var eqDisabled = false
    private var bassDisabled = false
    private var virtDisabled = false
    private var reverbDisabled = false
    private var loudnessDisabled = false
    private var pendingLoudnessGainMb = 0

    @Volatile
    var limiterEnabled: Boolean = true
        private set

    @Volatile
    var channelBalance: Float = 0f
        private set

    @Volatile
    var monoMode: Boolean = false
        private set

    var onMonoModeChanged: ((Boolean) -> Unit)? = null

    private val tenBandLevelsMb = IntArray(10) { 0 }

    private val _status = MutableStateFlow(AudioFxStatus())
    val status: StateFlow<AudioFxStatus> = _status.asStateFlow()

    var sessionId = 0
        private set

    private val availableEffectTypes: Set<UUID> by lazy {
        runCatching {
            AudioEffect.queryEffects()?.mapNotNull { it.type }?.toSet() ?: emptySet()
        }.getOrElse { emptySet() }
    }

    private fun isTypeAvailable(type: UUID): Boolean = type in availableEffectTypes

    val reverbNames = listOf("None", "Small room", "Medium room", "Large room", "Medium hall", "Large hall", "Plate")
    private val reverbPresets = shortArrayOf(
        PresetReverb.PRESET_NONE,
        PresetReverb.PRESET_SMALLROOM,
        PresetReverb.PRESET_MEDIUMROOM,
        PresetReverb.PRESET_LARGEROOM,
        PresetReverb.PRESET_MEDIUMHALL,
        PresetReverb.PRESET_LARGEHALL,
        PresetReverb.PRESET_PLATE
    )

    fun attach(id: Int) {
        if (id <= 0 || id == sessionId) return
        release()
        sessionId = id

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && !dpDisabled) {
            dp = runCatching { createDynamicsProcessing(id) }
                .onFailure { dpDisabled = true }
                .getOrNull()
        }

        if (dp == null && !eqDisabled && isTypeAvailable(AudioEffect.EFFECT_TYPE_EQUALIZER)) {
            eq = runCatching {
                Equalizer(0, id).apply { enabled = true }
            }.onFailure {
                eqDisabled = true
                ErrorDispatcher.post(
                    category = ErrorCategory.AUDIO_FX,
                    title = "Equalizer Unsupported",
                    message = "System hardware audio equalizer could not be initialized on this output.",
                    technicalDetails = it.message
                )
            }.getOrNull()
        }

        if (!bassDisabled && isTypeAvailable(AudioEffect.EFFECT_TYPE_BASS_BOOST)) {
            bass = runCatching {
                BassBoost(0, id).apply {
                    val targetStrength = (saved?.bass ?: 0).toShort()
                    if (targetStrength > 0) {
                        setStrength(targetStrength)
                        enabled = true
                    }
                }
            }.onFailure {
                bassDisabled = true
                ErrorDispatcher.post(
                    category = ErrorCategory.AUDIO_FX,
                    title = "Bass Boost Unsupported",
                    message = "Hardware bass boost is not supported on this device/HAL.",
                    technicalDetails = it.message
                )
            }.getOrNull()
        }

        if (!virtDisabled && isTypeAvailable(AudioEffect.EFFECT_TYPE_VIRTUALIZER)) {
            virt = runCatching {
                Virtualizer(0, id).apply {
                    val targetStrength = (saved?.virt ?: 0).toShort()
                    if (targetStrength > 0) {
                        setStrength(targetStrength)
                        enabled = true
                    }
                }
            }.onFailure {
                virtDisabled = true
                ErrorDispatcher.post(
                    category = ErrorCategory.AUDIO_FX,
                    title = "Virtualizer Unsupported",
                    message = "Hardware audio virtualizer is not supported on this device/HAL.",
                    technicalDetails = it.message
                )
            }.getOrNull()
        }

        if (!reverbDisabled && isTypeAvailable(AudioEffect.EFFECT_TYPE_PRESET_REVERB)) {
            reverb = runCatching {
                PresetReverb(0, 0).apply {
                    val targetIdx = saved?.reverb ?: 0
                    if (targetIdx in 1 until reverbPresets.size) {
                        preset = reverbPresets[targetIdx]
                        enabled = true
                    }
                }
            }.onFailure {
                reverbDisabled = true
                ErrorDispatcher.post(
                    category = ErrorCategory.AUDIO_FX,
                    title = "Reverb Unsupported",
                    message = "Hardware reverb is not supported on this device/HAL.",
                    technicalDetails = it.message
                )
            }.getOrNull()
        }

        if (!loudnessDisabled && isTypeAvailable(AudioEffect.EFFECT_TYPE_LOUDNESS_ENHANCER)) {
            loudness = runCatching {
                LoudnessEnhancer(id).apply {
                    if (pendingLoudnessGainMb > 0) {
                        setTargetGain(pendingLoudnessGainMb)
                        enabled = true
                    } else {
                        enabled = false
                    }
                }
            }.onFailure {
                loudnessDisabled = true
            }.getOrNull()
        }

        saved?.let { applySaved(it) }
        if (dp != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            applyBalanceAndLimiterDp()
        }

        _status.value = AudioFxStatus(
            eqSupported = eq != null || dp != null,
            tenBandSupported = dp != null,
            limiterSupported = dp != null,
            bassSupported = bass != null,
            virtSupported = virt != null,
            reverbSupported = reverb != null,
            loudnessSupported = loudness != null,
            monoSupported = true,
            sessionId = id
        )
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun createDynamicsProcessing(sessionId: Int): DynamicsProcessing {
        val config = DynamicsProcessing.Config.Builder(
            DynamicsProcessing.VARIANT_FAVOR_FREQUENCY_RESOLUTION,
            2,
            false, 0,
            false, 0,
            true, 10,
            true
        ).build()
        return DynamicsProcessing(0, sessionId, config).apply {
            for (band in 0 until 10) {
                val eqBand = DynamicsProcessing.EqBand(true, DspMath.TEN_BAND_CUTOFFS_HZ[band], tenBandLevelsMb[band] / 100f)
                setPostEqBandAllChannelsTo(band, eqBand)
            }
            val limiter = DynamicsProcessing.Limiter(true, limiterEnabled, 0, 1f, 60f, 4f, -0.5f, 0f)
            setLimiterAllChannelsTo(limiter)
            enabled = masterEnabled
        }
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun applyBalanceAndLimiterDp() {
        val engine = dp ?: return
        runCatching {
            val (leftDb, rightDb) = DspMath.channelInputGainsDb(channelBalance)
            engine.setInputGainbyChannel(0, leftDb)
            engine.setInputGainbyChannel(1, rightDb)
            val limiter = DynamicsProcessing.Limiter(true, limiterEnabled, 0, 1f, 60f, 4f, -0.5f, 0f)
            engine.setLimiterAllChannelsTo(limiter)
        }
    }

    fun release() {
        runCatching { dp?.release() }
        listOf(eq, bass, virt, reverb, loudness).forEach { runCatching { it?.release() } }
        dp = null
        eq = null
        bass = null
        virt = null
        reverb = null
        loudness = null
        sessionId = 0
        _status.value = AudioFxStatus()
    }

    fun setReplayGainBoostMb(gainMb: Int) {
        val safeMb = gainMb.coerceIn(0, 600)
        pendingLoudnessGainMb = safeMb
        if (loudnessDisabled || sessionId <= 0) return
        if (loudness == null && safeMb > 0 && isTypeAvailable(AudioEffect.EFFECT_TYPE_LOUDNESS_ENHANCER)) {
            loudness = runCatching { LoudnessEnhancer(sessionId) }
                .onFailure { loudnessDisabled = true }
                .getOrNull()
        }
        runCatching {
            loudness?.let {
                it.setTargetGain(safeMb)
                it.enabled = safeMb > 0
            }
        }
    }

    fun setLimiterEnabled(enabled: Boolean) {
        limiterEnabled = enabled
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            applyBalanceAndLimiterDp()
        }
    }

    fun setChannelBalance(balance: Float) {
        channelBalance = balance.coerceIn(-1f, 1f)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            applyBalanceAndLimiterDp()
        }
    }

    fun setMonoMode(enabled: Boolean) {
        monoMode = enabled
        onMonoModeChanged?.invoke(enabled)
    }

    val eqSupported: Boolean get() = (dp != null && !dpDisabled) || (!eqDisabled && eq != null)
    val tenBandSupported: Boolean get() = dp != null && !dpDisabled
    val bandCount: Int
        get() = when {
            dp != null -> 10
            eq != null -> eq!!.numberOfBands.toInt()
            else -> 0
        }
    val levelRange: Pair<Int, Int>
        get() = if (dp != null) {
            -1500 to 1500
        } else {
            eq?.bandLevelRange?.let { it[0].toInt() to it[1].toInt() } ?: (-1500 to 1500)
        }

    fun bandFreqHz(b: Int): Int = when {
        dp != null -> DspMath.TEN_BAND_FREQUENCIES_HZ.getOrElse(b) { 1000 }
        else -> runCatching { (eq?.getCenterFreq(b.toShort()) ?: 0) / 1000 }.getOrDefault(0)
    }

    fun bandLevel(b: Int): Int = when {
        dp != null -> tenBandLevelsMb.getOrElse(b) { 0 }
        else -> runCatching { eq?.getBandLevel(b.toShort())?.toInt() ?: 0 }.getOrDefault(0)
    }

    fun setBand(b: Int, mb: Int) {
        val (lo, hi) = levelRange
        val safeMb = mb.coerceIn(lo, hi)
        if (dp != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            if (b in 0 until 10) {
                tenBandLevelsMb[b] = safeMb
                runCatching {
                    val eqBand = DynamicsProcessing.EqBand(true, DspMath.TEN_BAND_CUTOFFS_HZ[b], safeMb / 100f)
                    dp?.setPostEqBandAllChannelsTo(b, eqBand)
                }
            }
        } else {
            runCatching { eq?.setBandLevel(b.toShort(), safeMb.toShort()) }
        }
    }

    val bassSupported: Boolean get() = !bassDisabled && (bass?.strengthSupported == true || (bass == null && isTypeAvailable(AudioEffect.EFFECT_TYPE_BASS_BOOST)))
    fun setBass(permille: Int) {
        if (bass == null && !bassDisabled && isTypeAvailable(AudioEffect.EFFECT_TYPE_BASS_BOOST) && sessionId > 0) {
            bass = runCatching { BassBoost(0, sessionId) }.onFailure { bassDisabled = true }.getOrNull()
        }
        runCatching {
            bass?.let {
                it.setStrength(permille.toShort())
                it.enabled = permille > 0
            }
        }
    }

    val virtSupported: Boolean get() = !virtDisabled && (virt?.strengthSupported == true || (virt == null && isTypeAvailable(AudioEffect.EFFECT_TYPE_VIRTUALIZER)))
    fun setVirtualizer(permille: Int) {
        if (virt == null && !virtDisabled && isTypeAvailable(AudioEffect.EFFECT_TYPE_VIRTUALIZER) && sessionId > 0) {
            virt = runCatching { Virtualizer(0, sessionId) }.onFailure { virtDisabled = true }.getOrNull()
        }
        runCatching {
            virt?.let {
                it.setStrength(permille.toShort())
                it.enabled = permille > 0
            }
        }
    }

    private var masterEnabled = true
    val isMasterEnabled: Boolean get() = masterEnabled

    fun setEnabled(enabled: Boolean) {
        masterEnabled = enabled
        runCatching { dp?.enabled = enabled }
        runCatching { eq?.enabled = enabled }
        runCatching { bass?.enabled = enabled && (bass?.roundedStrength ?: 0) > 0 }
        runCatching { virt?.enabled = enabled && (virt?.roundedStrength ?: 0) > 0 }
        runCatching { reverb?.enabled = enabled && (reverb?.preset ?: PresetReverb.PRESET_NONE) != PresetReverb.PRESET_NONE }
    }

    val reverbSupported: Boolean get() = !reverbDisabled && (reverb != null || isTypeAvailable(AudioEffect.EFFECT_TYPE_PRESET_REVERB))
    fun setReverb(index: Int) {
        if (index in reverbPresets.indices) {
            if (reverb == null && index > 0 && !reverbDisabled && isTypeAvailable(AudioEffect.EFFECT_TYPE_PRESET_REVERB)) {
                reverb = runCatching { PresetReverb(0, 0) }.onFailure { reverbDisabled = true }.getOrNull()
            }
            runCatching {
                reverb?.let {
                    it.preset = reverbPresets[index]
                    it.enabled = masterEnabled && index > 0
                }
            }
        }
    }

    val presets: Map<String, FloatArray> = mapOf(
        "Normal" to floatArrayOf(0f, 0f, 0f, 0f, 0f),
        "Classical" to floatArrayOf(0.4f, 0.2f, -0.1f, 0.3f, 0.4f),
        "Dance" to floatArrayOf(0.6f, 0.3f, 0f, 0.3f, 0.5f),
        "Flat" to floatArrayOf(0f, 0f, 0f, 0f, 0f),
        "Folk" to floatArrayOf(0.2f, 0f, 0.1f, 0.3f, 0.1f),
        "Heavy Metal" to floatArrayOf(0.5f, 0.2f, 0.6f, 0.4f, 0.2f),
        "Hip Hop" to floatArrayOf(0.6f, 0.4f, 0f, 0.1f, 0.3f),
        "Jazz" to floatArrayOf(0.3f, 0.1f, -0.2f, 0.2f, 0.4f),
        "Pop" to floatArrayOf(-0.2f, 0.2f, 0.4f, 0.2f, -0.2f),
        "Rock" to floatArrayOf(0.5f, 0.2f, -0.1f, 0.3f, 0.5f)
    )

    fun applyPreset(name: String) {
        val p = presets[name] ?: return
        val (lo, hi) = levelRange
        val fiveBandMb = p.map { f -> (if (f >= 0) f * hi else -f * lo).toInt() }
        applyBandLevels(fiveBandMb)
    }

    fun currentBandLevels(): List<Int> {
        val n = bandCount
        if (n <= 0) return List(10) { 0 }
        return (0 until n).map { bandLevel(it) }
    }

    fun applyBandLevels(levels: List<Int>) {
        val n = bandCount
        if (n <= 0 || levels.isEmpty()) return
        val (lo, hi) = levelRange
        val mapped = DspMath.resampleBands(levels, n, lo, hi)
        for (b in 0 until n) {
            setBand(b, mapped[b])
        }
    }

    fun currentBass(): Int = runCatching { bass?.roundedStrength?.toInt() ?: 0 }.getOrDefault(0)
    fun currentVirtualizer(): Int = runCatching { virt?.roundedStrength?.toInt() ?: 0 }.getOrDefault(0)
    fun currentReverb(): Int {
        val currentPreset = runCatching { reverb?.preset }.getOrNull() ?: return 0
        return reverbPresets.indexOf(currentPreset).coerceAtLeast(0)
    }

    val builtInProfiles: List<SoundProfile> = listOf(
        SoundProfile("builtin_flat", "Flat Studio", listOf(0, 0, 0, 0, 0), bassStrength = 0, virtualizerStrength = 0, reverbIndex = 0, isBuiltIn = true),
        SoundProfile("builtin_warm", "Studio Warm", listOf(450, 280, 60, 180, 260), bassStrength = 280, virtualizerStrength = 160, reverbIndex = 0, isBuiltIn = true),
        SoundProfile("builtin_vocal", "Vocal Clarity", listOf(-180, 120, 480, 420, 180), bassStrength = 0, virtualizerStrength = 120, reverbIndex = 0, isBuiltIn = true),
        SoundProfile("builtin_bass", "Deep Bass", listOf(720, 480, 80, 150, 240), bassStrength = 680, virtualizerStrength = 250, reverbIndex = 0, isBuiltIn = true),
        SoundProfile("builtin_night", "Late Night", listOf(240, 150, 220, 120, -180), bassStrength = 180, virtualizerStrength = 100, reverbIndex = 1, isBuiltIn = true),
        SoundProfile("builtin_live", "Live Hall", listOf(320, 140, -120, 260, 440), bassStrength = 220, virtualizerStrength = 520, reverbIndex = 4, isBuiltIn = true)
    )

    var saved: FxSettings? = null
    data class FxSettings(val bands: List<Int>, val bass: Int, val virt: Int, val reverb: Int)

    private fun applySaved(s: FxSettings) {
        applyBandLevels(s.bands)
        setBass(s.bass)
        setVirtualizer(s.virt)
        setReverb(s.reverb)
    }
}
