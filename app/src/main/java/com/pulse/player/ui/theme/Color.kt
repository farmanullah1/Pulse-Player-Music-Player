package com.pulse.player.ui.theme

import androidx.compose.ui.graphics.Color

object Pulse {
    // Surfaces (AMOLED first)
    val Black = Color(0xFF000000)
    val Bg = Color(0xFF07060C)
    val Surface = Color(0xFF12101C)
    val Elevated = Color(0xFF1B1829)
    val Outline = Color(0xFF2E2A42)

    // Text
    val TextHi = Color(0xFFFFFFFF)
    val TextMid = Color(0xFFA0A0A0)
    val TextLow = Color(0xFF6E6B80)

    // Accents
    val Cyan = Color(0xFF00C3FF) // favorite heart + highlights
    val Violet = Color(0xFF9B7BFF)
    val Magenta = Color(0xFFFF2A6D)
    val Pink = Color(0xFFFF6DAA)
    val Coral = Color(0xFFFF8C78)
    val Mint = Color(0xFF3DDC97)
    val Orange = Color(0xFFFF9500) // APPLY button
    val Purple = Color(0xFFB36BFF)
    val Error = Color(0xFFFF647C)

    // Original dark-magenta background spec
    val PlumTop = Color(0xFF2A0826)
    val PlumBottom = Color(0xFF120310)
}

data class Skin(val id: String, val name: String, val colors: List<Color>)

val Skins = listOf(
    Skin("plum", "Plum", listOf(Pulse.PlumTop, Pulse.PlumBottom, Color.Black)),
    Skin("aurora", "Aurora", listOf(Color(0xFF3A1C71), Color(0xFF1B1B4B), Color.Black)),
    Skin("afterglow", "Afterglow", listOf(Color(0xFFFF2A6D), Color(0xFF5B1A58), Color(0xFF0A0612))),
    Skin("ocean", "Ocean", listOf(Color(0xFF0F4C75), Color(0xFF051937), Color.Black)),
    Skin("sunrise", "Sunrise", listOf(Color(0xFFFF8C78), Color(0xFFC2377A), Color(0xFF2B1055))),
    Skin("forest", "Forest", listOf(Color(0xFF0B3D2E), Color(0xFF03140F), Color.Black)),
    Skin("mono", "Mono", listOf(Color(0xFF2B2B2B), Color.Black)),
    Skin("amoled", "Pure black", listOf(Color.Black, Color.Black))
)
