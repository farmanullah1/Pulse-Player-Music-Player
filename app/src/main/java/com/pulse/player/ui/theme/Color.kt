package com.pulse.player.ui.theme

import androidx.compose.ui.graphics.Color

object Pulse {
    val Black = Color(0xFF000000)
    val Bg = Color(0xFF05040A)
    val Surface = Color(0xFF110F1C)
    val Elevated = Color(0xFF1A1729)
    val Outline = Color(0xFF2E2A42)

    val TextHi = Color.White
    val TextMid = Color(0xFFB4B0C8) // TextMid lifted from A0A0A0 for AA contrast on Surface
    val TextLow = Color(0xFF77738C)

    val Cyan = Color(0xFF00C3FF)
    val Violet = Color(0xFF9B7BFF)
    val Magenta = Color(0xFFFF2A6D)
    val Pink = Color(0xFFFF6DAA)
    val Coral = Color(0xFFFF8C78)
    val Mint = Color(0xFF3DDC97)
    val Orange = Color(0xFFFF9500)
    val Purple = Color(0xFFB36BFF)
    val Lime = Color(0xFFB6F24A)
    val Sun = Color(0xFFFFD166)
    val Error = Color(0xFFFF647C)

    val PlumTop = Color(0xFF2A0826)
    val PlumBottom = Color(0xFF120310)

    // signature gradients (use via Brush.linearGradient)
    val GradPrimary = listOf(Violet, Magenta)           // Play, APPLY alternative
    val GradCool    = listOf(Cyan, Violet)              // Favourites, links
    val GradWarm    = listOf(Orange, Magenta)           // Top tracks, flame
    val GradFresh   = listOf(Mint, Cyan)                // Recently added
    val GradSunset  = listOf(Coral, Pink, Purple)
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
    Skin("amoled", "Pure black", listOf(Color.Black, Color.Black)),
    Skin("neon", "Neon", listOf(Color(0xFF00C3FF), Color(0xFF9B7BFF), Color.Black)),
    Skin("candy", "Candy", listOf(Color(0xFFFF6DAA), Color(0xFFB36BFF), Color(0xFF120310))),
    Skin("lava", "Lava", listOf(Color(0xFFFF9500), Color(0xFFFF2A6D), Color(0xFF1A0508)))
)
