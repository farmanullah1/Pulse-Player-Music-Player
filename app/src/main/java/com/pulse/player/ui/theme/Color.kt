package com.pulse.player.ui.theme

import androidx.compose.ui.graphics.Color

// ─── Core Pulse palette ───────────────────────────────────────────────────────

object Pulse {
    val Black = Color(0xFF000000)
    val Bg = Color(0xFF05040A)
    val Surface = Color(0xFF110F1C)
    val Elevated = Color(0xFF1A1729)
    val SurfaceCard = Elevated
    val Outline = Color(0xFF2E2A42)

    val TextHi = Color.White
    val TextMid = Color(0xFFB4B0C8)  // lifted for AA contrast on Surface
    val TextSecondary = TextMid
    val TextLow = Color(0xFF77738C)
    val TextMuted = TextLow
    val TextLight = TextMid

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

    // Favourite/Cyan accent
    val FavoriteCyan = Cyan

    // Apply / Using action accent
    val ApplyOrange = Orange

    // Atmospheric Queue amber accents (C15)
    /** Active queue row accent on dark surfaces — #D4AF37. Contrast ≥ 4.5:1 on dark bg. */
    val ActiveAmberDark = Color(0xFFD4AF37)
    /** Active queue row accent on the light queue card — #8A6D00. Contrast ≥ 4.7:1 on #F9F9FB. */
    val ActiveAmberOnLight = Color(0xFF8A6D00)

    // Signature gradients (use via Brush.linearGradient)
    val GradPrimary = listOf(Violet, Magenta)           // Play, APPLY
    val GradCool    = listOf(Cyan, Violet)              // Favourites, links
    val GradWarm    = listOf(Orange, Magenta)           // Top tracks
    val GradFresh   = listOf(Mint, Cyan)                // Recently added
    val GradSunset  = listOf(Coral, Pink, Purple)
}

// ─── Design tokens (single source of truth — no screen should hardcode a color)─

/**
 * Design tokens consumed by all composables via [LocalPulseTokens].
 * Each field maps to a semantic role. Theme composable creates concrete instances;
 * never hardcode hex values in screens or components.
 */
data class PulseTokens(
    // Backgrounds
    val bgPrimary: Color,
    val bgSecondary: Color,
    val bgElevated: Color,
    val bgCard: Color,

    // Text
    val textPrimary: Color,
    val textSecondary: Color,
    val textDisabled: Color,

    // Accent
    val accent: Color,
    val accentOnSurface: Color,

    // Status
    val error: Color,
    val warning: Color,
    val success: Color,

    // Dividers / outlines
    val divider: Color,
    val outline: Color,

    // Scrims
    val scrim: Color,

    // Playback state
    val playbackActive: Color,
    val playbackInactive: Color,

    // Queue
    val queueCurrent: Color,
    val queueNext: Color,

    // Interactive states
    val focusedOverlay: Color,
    val pressedOverlay: Color,
    val selectedOverlay: Color,

    // Navigation
    val navSelected: Color,
    val navUnselected: Color,

    // Player
    val playerSurface: Color,
    val playerOverlay: Color,

    // Amber (Atmospheric Queue)
    val activeAmber: Color,
    val activeAmberOnLight: Color,

    // Favorite / Apply
    val favoriteCyan: Color,
    val applyOrange: Color
) {
    companion object {
        val Dark = PulseTokens(
            bgPrimary = Pulse.Bg,
            bgSecondary = Pulse.Surface,
            bgElevated = Pulse.Elevated,
            bgCard = Pulse.Elevated,
            textPrimary = Pulse.TextHi,
            textSecondary = Pulse.TextMid,
            textDisabled = Pulse.TextLow,
            accent = Pulse.Cyan,
            accentOnSurface = Pulse.Cyan,
            error = Pulse.Error,
            warning = Pulse.Orange,
            success = Pulse.Mint,
            divider = Pulse.Outline.copy(alpha = 0.6f),
            outline = Pulse.Outline,
            scrim = Color.Black.copy(alpha = 0.55f),
            playbackActive = Pulse.Cyan,
            playbackInactive = Pulse.TextLow,
            queueCurrent = Pulse.Cyan.copy(alpha = 0.15f),
            queueNext = Color.Transparent,
            focusedOverlay = Color.White.copy(alpha = 0.08f),
            pressedOverlay = Color.White.copy(alpha = 0.12f),
            selectedOverlay = Pulse.Cyan.copy(alpha = 0.15f),
            navSelected = Pulse.Cyan,
            navUnselected = Pulse.TextLow,
            playerSurface = Pulse.Surface,
            playerOverlay = Color.Black.copy(alpha = 0.4f),
            activeAmber = Pulse.ActiveAmberDark,
            activeAmberOnLight = Pulse.ActiveAmberOnLight,
            favoriteCyan = Pulse.FavoriteCyan,
            applyOrange = Pulse.ApplyOrange
        )

        val Amoled = Dark.copy(
            bgPrimary = Color.Black,
            bgSecondary = Color(0xFF0A0A0A),
            bgElevated = Color(0xFF111111),
            bgCard = Color(0xFF111111),
            playerSurface = Color.Black
        )

        val Light = PulseTokens(
            bgPrimary = Color(0xFFFAF7FF),
            bgSecondary = Color.White,
            bgElevated = Color(0xFFF0EBFA),
            bgCard = Color.White,
            textPrimary = Color(0xFF1A1625),
            textSecondary = Color(0xFF5E5870),
            textDisabled = Color(0xFFAEA8BE),
            accent = Color(0xFF006782),
            accentOnSurface = Color(0xFF006782),
            error = Color(0xFFD32F4F),
            warning = Color(0xFFE65100),
            success = Color(0xFF2E7D32),
            divider = Color(0xFFD9D2EA),
            outline = Color(0xFFD9D2EA),
            scrim = Color.Black.copy(alpha = 0.4f),
            playbackActive = Color(0xFF006782),
            playbackInactive = Color(0xFFAEA8BE),
            queueCurrent = Color(0xFF006782).copy(alpha = 0.12f),
            queueNext = Color.Transparent,
            focusedOverlay = Color.Black.copy(alpha = 0.05f),
            pressedOverlay = Color.Black.copy(alpha = 0.08f),
            selectedOverlay = Color(0xFF006782).copy(alpha = 0.12f),
            navSelected = Color(0xFF006782),
            navUnselected = Color(0xFF5E5870),
            playerSurface = Color.White,
            playerOverlay = Color.Black.copy(alpha = 0.2f),
            activeAmber = Pulse.ActiveAmberOnLight,   // Light mode: use the high-contrast token
            activeAmberOnLight = Pulse.ActiveAmberOnLight,
            favoriteCyan = Color(0xFF006782),
            applyOrange = Color(0xFFE65100)
        )
    }
}

// ─── Skin definitions ─────────────────────────────────────────────────────────

data class Skin(val id: String, val name: String, val colors: List<Color>)

val Skins = listOf(
    // Original 8 skins — IDs unchanged
    Skin("plum",              "Plum",              listOf(Pulse.PlumTop, Pulse.PlumBottom, Color.Black)),
    Skin("aurora",            "Aurora",            listOf(Color(0xFF3A1C71), Color(0xFF1B1B4B), Color.Black)),
    Skin("afterglow",         "Afterglow",         listOf(Color(0xFFFF2A6D), Color(0xFF5B1A58), Color(0xFF0A0612))),
    Skin("ocean",             "Ocean",             listOf(Color(0xFF0F4C75), Color(0xFF051937), Color.Black)),
    Skin("sunrise",           "Sunrise",           listOf(Color(0xFFFF8C78), Color(0xFFC2377A), Color(0xFF2B1055))),
    Skin("forest",            "Forest",            listOf(Color(0xFF0B3D2E), Color(0xFF03140F), Color.Black)),
    Skin("mono",              "Mono",              listOf(Color(0xFF2B2B2B), Color.Black)),
    Skin("amoled",            "Pure Black",        listOf(Color.Black, Color.Black)),

    // 7 skins from prior merge — IDs unchanged
    Skin("neon",              "Neon",              listOf(Color(0xFF00C3FF), Color(0xFF9B7BFF), Color.Black)),
    Skin("candy",             "Candy",             listOf(Color(0xFFFF6DAA), Color(0xFFB36BFF), Color(0xFF120310))),
    Skin("lava",              "Lava",              listOf(Color(0xFFFF9500), Color(0xFFFF2A6D), Color(0xFF1A0508))),
    Skin("pixel_material",    "Pixel Material You",listOf(Color(0xFF295A7E), Color(0xFF132F47), Color.Black)),
    Skin("namida_amber",      "Namida Amber",      listOf(Color(0xFFF1D6B6), Color(0xFF463E35), Color(0xFF1E1A17))),
    Skin("namida_neon",       "Namida Cyberpunk",  listOf(Color(0xFF00F0FF), Color(0xFF7B55C1), Color(0xFF0D0B14))),
    Skin("atmospheric_mist",  "Atmospheric Mist",  listOf(Color(0xFF183B38), Color(0xFF071312), Color.Black)),

    // New skins from merge spec §8.2 — additive, no existing IDs changed
    Skin("dark_plum",         "Dark Plum",         listOf(Color(0xFF1A0230), Color(0xFF0D0118), Color.Black)),
    Skin("light_warm",        "Light Warm",        listOf(Color(0xFFFFF3E0), Color(0xFFFFE0B2))),
    Skin("expressive_mat",    "Expressive Material",listOf(Color(0xFF1A237E), Color(0xFF283593), Color(0xFF0D1342))),
    Skin("monochrome",        "Monochrome",        listOf(Color(0xFF212121), Color(0xFF1A1A1A), Color.Black)),
    // Material You and Artwork Dynamic are not static gradients — handled in Theme.kt via DynamicColorScheme
)
