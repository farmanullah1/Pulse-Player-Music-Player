package com.pulse.player.playback

import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.PresetReverb
import android.media.audiofx.Virtualizer

object AudioFx {
    private var eq: Equalizer? = null
    private var bass: BassBoost? = null
    private var virt: Virtualizer? = null
    private var reverb: PresetReverb? = null
    var sessionId = 0
        private set

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
        if (id == 0 || id == sessionId) return
        release()
        sessionId = id
        eq = runCatching { Equalizer(0, id).apply { enabled = true } }.getOrNull()
        bass = runCatching { BassBoost(0, id).apply { enabled = true } }.getOrNull()
        virt = runCatching { Virtualizer(0, id).apply { enabled = true } }.getOrNull()
        reverb = runCatching { PresetReverb(0, id).apply { enabled = true } }.getOrNull()
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

    val eqSupported get() = eq != null
    val bandCount get() = eq?.numberOfBands?.toInt() ?: 0
    val levelRange get() = eq?.bandLevelRange?.let { it[0].toInt() to it[1].toInt() } ?: (-1500 to 1500)

    fun bandFreqHz(b: Int) = (eq?.getCenterFreq(b.toShort()) ?: 0) / 1000
    fun bandLevel(b: Int) = eq?.getBandLevel(b.toShort())?.toInt() ?: 0
    fun setBand(b: Int, mb: Int) { eq?.setBandLevel(b.toShort(), mb.toShort()) }

    val bassSupported get() = bass?.strengthSupported == true
    fun setBass(permille: Int) { runCatching { bass?.setStrength(permille.toShort()) } }

    val virtSupported get() = virt?.strengthSupported == true
    fun setVirtualizer(permille: Int) { runCatching { virt?.setStrength(permille.toShort()) } }

    val reverbSupported get() = reverb != null
    fun setReverb(index: Int) {
        if (index in reverbPresets.indices) {
            runCatching { reverb?.preset = reverbPresets[index] }
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
