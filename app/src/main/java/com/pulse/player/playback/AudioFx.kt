package com.pulse.player.playback

import android.media.audiofx.AudioEffect
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.PresetReverb
import android.media.audiofx.Virtualizer
import java.util.UUID

object AudioFx {
    private var eq: Equalizer? = null
    private var bass: BassBoost? = null
    private var virt: Virtualizer? = null
    private var reverb: PresetReverb? = null

    private var eqDisabled = false
    private var bassDisabled = false
    private var virtDisabled = false
    private var reverbDisabled = false

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

        if (!eqDisabled && isTypeAvailable(AudioEffect.EFFECT_TYPE_EQUALIZER)) {
            eq = runCatching {
                Equalizer(0, id).apply { enabled = true }
            }.onFailure { eqDisabled = true }.getOrNull()
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
            }.onFailure { bassDisabled = true }.getOrNull()
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
            }.onFailure { virtDisabled = true }.getOrNull()
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
            }.onFailure { reverbDisabled = true }.getOrNull()
        }

        saved?.let { applySaved(it) }
    }

    fun release() {
        listOf(eq, bass, virt, reverb).forEach { runCatching { it?.release() } }
        eq = null
        bass = null
        virt = null
        reverb = null
        sessionId = 0
    }

    val eqSupported: Boolean get() = !eqDisabled && eq != null
    val bandCount: Int get() = eq?.numberOfBands?.toInt() ?: 0
    val levelRange: Pair<Int, Int> get() = eq?.bandLevelRange?.let { it[0].toInt() to it[1].toInt() } ?: (-1500 to 1500)

    fun bandFreqHz(b: Int): Int = runCatching { (eq?.getCenterFreq(b.toShort()) ?: 0) / 1000 }.getOrDefault(0)
    fun bandLevel(b: Int): Int = runCatching { eq?.getBandLevel(b.toShort())?.toInt() ?: 0 }.getOrDefault(0)
    fun setBand(b: Int, mb: Int) { runCatching { eq?.setBandLevel(b.toShort(), mb.toShort()) } }

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

    val reverbSupported: Boolean get() = !reverbDisabled && (reverb != null || isTypeAvailable(AudioEffect.EFFECT_TYPE_PRESET_REVERB))
    fun setReverb(index: Int) {
        if (index in reverbPresets.indices) {
            if (reverb == null && index > 0 && !reverbDisabled && isTypeAvailable(AudioEffect.EFFECT_TYPE_PRESET_REVERB)) {
                reverb = runCatching { PresetReverb(0, 0) }.onFailure { reverbDisabled = true }.getOrNull()
            }
            runCatching {
                reverb?.let {
                    it.preset = reverbPresets[index]
                    it.enabled = index > 0
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
        val n = bandCount
        if (n == 0) return
        for (b in 0 until n) {
            val f = p[(b * (p.size - 1)) / (n - 1).coerceAtLeast(1)]
            setBand(b, (if (f >= 0) f * hi else -f * lo).toInt())
        }
    }

    var saved: FxSettings? = null
    data class FxSettings(val bands: List<Int>, val bass: Int, val virt: Int, val reverb: Int)

    private fun applySaved(s: FxSettings) {
        s.bands.forEachIndexed { i, v -> if (i < bandCount) setBand(i, v) }
        setBass(s.bass)
        setVirtualizer(s.virt)
        setReverb(s.reverb)
    }
}
