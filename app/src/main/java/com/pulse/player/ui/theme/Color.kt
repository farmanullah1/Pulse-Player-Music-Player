package com.pulse.player.ui.theme

import androidx.compose.ui.graphics.Color

// ─── Core Pulse palette ───────────────────────────────────────────────────────

object Pulse {
    val Black = Color(0xFF000000)
    val Bg = Color(0xFF121318)
    val Surface = Color(0xFF1A1B22)
    val Elevated = Color(0xFF242630)
    val SurfaceCard = Elevated
    val Outline = Color(0xFF363947)

    val TextHi = Color(0xFFF5F6FA)
    val TextMid = Color(0xFFC2C6D6)  // crisp AA contrast on dark charcoal
    val TextSecondary = TextMid
    val TextLow = Color(0xFF8F95AA)
    val TextMuted = TextLow
    val TextLight = TextMid

    val StudioAmber = Color(0xFFF2A93B)
    val StudioAmberLight = Color(0xFF9A5D00)
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
    /** Active queue row accent on dark surfaces — #D4AF37. Contrast >= 4.5:1 on dark bg. */
    val ActiveAmberDark = Color(0xFFD4AF37)
    /** Active queue row accent on the light queue card — #8A6D00. Contrast >= 4.7:1 on #F9F9FB. */
    val ActiveAmberOnLight = Color(0xFF8A6D00)

    // Signature gradients (use via Brush.linearGradient)
    val GradPrimary = listOf(Violet, Magenta)           // Play, Apply
    val GradCool    = listOf(Cyan, Violet)              // Favourites, links
    val GradWarm    = listOf(Orange, Magenta)           // Top tracks
    val GradFresh   = listOf(Mint, Cyan)                // Recently added
    val GradSunset  = listOf(Coral, Pink, Purple)
}

/** Curated seed accent options for Custom Seed color mode. */
data class SeedColorOption(val name: String, val darkArgb: Int, val lightArgb: Int)

val SeedPalettePresets = listOf(
    SeedColorOption("Electric Cyan",   0xFF00C3FF.toInt(), 0xFF006782.toInt()),
    SeedColorOption("Pulse Violet",    0xFF9B7BFF.toInt(), 0xFF5B3CE6.toInt()),
    SeedColorOption("Rose Magenta",    0xFFFF2A6D.toInt(), 0xFFC2185B.toInt()),
    SeedColorOption("Coral Ember",     0xFFFF7A59.toInt(), 0xFFD84315.toInt()),
    SeedColorOption("Botanical Mint",  0xFF3DDC97.toInt(), 0xFF007955.toInt()),
    SeedColorOption("Warm Amber",      0xFFFFB300.toInt(), 0xFF9A6700.toInt()),
    SeedColorOption("Royal Indigo",    0xFF658BFF.toInt(), 0xFF2D4FD9.toInt()),
    SeedColorOption("Ocean Teal",      0xFF26D7C4.toInt(), 0xFF006D67.toInt())
)

// ─── Design tokens (single source of truth — no screen should hardcode a color)─

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
            accent = Pulse.StudioAmber,
            accentOnSurface = Pulse.StudioAmber,
            error = Pulse.Error,
            warning = Pulse.Orange,
            success = Pulse.Mint,
            divider = Pulse.Outline.copy(alpha = 0.6f),
            outline = Pulse.Outline,
            scrim = Color.Black.copy(alpha = 0.55f),
            playbackActive = Pulse.Cyan,
            playbackInactive = Pulse.TextLow,
            queueCurrent = Pulse.Cyan.copy(alpha = 0.16f),
            queueNext = Color.Transparent,
            focusedOverlay = Color.White.copy(alpha = 0.08f),
            pressedOverlay = Color.White.copy(alpha = 0.12f),
            selectedOverlay = Pulse.Cyan.copy(alpha = 0.16f),
            navSelected = Pulse.Cyan,
            navUnselected = Pulse.TextMid,
            playerSurface = Pulse.Surface,
            playerOverlay = Color.Black.copy(alpha = 0.45f),
            activeAmber = Pulse.ActiveAmberDark,
            activeAmberOnLight = Pulse.ActiveAmberOnLight,
            favoriteCyan = Pulse.FavoriteCyan,
            applyOrange = Pulse.ApplyOrange
        )

        val Amoled = Dark.copy(
            bgPrimary = Color.Black,
            bgSecondary = Color(0xFF0D0E12),
            bgElevated = Color(0xFF16181F),
            bgCard = Color(0xFF13151B),
            outline = Color(0xFF2D313E),
            divider = Color(0xFF222530),
            playerSurface = Color.Black
        )

        val Light = PulseTokens(
            bgPrimary = Color(0xFFF7F5FC),
            bgSecondary = Color(0xFFFFFFFF),
            bgElevated = Color(0xFFEEEAF7),
            bgCard = Color(0xFFFFFFFF),
            textPrimary = Color(0xFF181523),
            textSecondary = Color(0xFF524C63),
            textDisabled = Color(0xFF8C85A0),
            accent = Color(0xFF006782),
            accentOnSurface = Color(0xFF006782),
            error = Color(0xFFC62846),
            warning = Color(0xFFD84315),
            success = Color(0xFF1E7E45),
            divider = Color(0xFFDAD4EA),
            outline = Color(0xFFC9C0DF),
            scrim = Color.Black.copy(alpha = 0.38f),
            playbackActive = Color(0xFF006782),
            playbackInactive = Color(0xFF8C85A0),
            queueCurrent = Color(0xFF006782).copy(alpha = 0.12f),
            queueNext = Color.Transparent,
            focusedOverlay = Color.Black.copy(alpha = 0.05f),
            pressedOverlay = Color.Black.copy(alpha = 0.08f),
            selectedOverlay = Color(0xFF006782).copy(alpha = 0.12f),
            navSelected = Color(0xFF006782),
            navUnselected = Color(0xFF524C63),
            playerSurface = Color.White,
            playerOverlay = Color.Black.copy(alpha = 0.18f),
            activeAmber = Pulse.ActiveAmberOnLight,
            activeAmberOnLight = Pulse.ActiveAmberOnLight,
            favoriteCyan = Color(0xFF006782),
            applyOrange = Color(0xFFD84315)
        )
    }
}

// ─── Skin definitions ─────────────────────────────────────────────────────────

data class Skin(
    val id: String,
    val name: String,
    val colors: List<Color>,
    val lightColors: List<Color> = listOf(Color(0xFFF7F4FD), Color(0xFFECE6F8)),
    val accentDark: Color = Pulse.Cyan,
    val accentLight: Color = Color(0xFF006782),
    val isDaylight: Boolean = false
)

val Skins = listOf(
    // Default Studio Charcoal & Warm Amber skin
    Skin("studio_amber", "Studio Amber",
        colors = listOf(Color(0xFF1B1C22), Color(0xFF101116), Color(0xFF08090C)),
        lightColors = listOf(Color(0xFFFAF8F4), Color(0xFFF0EAE0)),
        accentDark = Pulse.StudioAmber, accentLight = Pulse.StudioAmberLight
    ),
    // Original 8 skins — IDs unchanged
    Skin("plum", "Plum",
        colors = listOf(Pulse.PlumTop, Pulse.PlumBottom, Color.Black),
        lightColors = listOf(Color(0xFFFBF5FA), Color(0xFFF2E4F1)),
        accentDark = Pulse.Cyan, accentLight = Color(0xFF8B2E6B)
    ),
    Skin("aurora", "Aurora",
        colors = listOf(Color(0xFF3A1C71), Color(0xFF1B1B4B), Color.Black),
        lightColors = listOf(Color(0xFFF4F1FC), Color(0xFFE4DEFA)),
        accentDark = Pulse.Violet, accentLight = Color(0xFF5336B8)
    ),
    Skin("afterglow", "Afterglow",
        colors = listOf(Color(0xFFFF2A6D), Color(0xFF5B1A58), Color(0xFF0A0612)),
        lightColors = listOf(Color(0xFFFFF3F7), Color(0xFFFDE0EB)),
        accentDark = Pulse.Pink, accentLight = Color(0xFFC2185B)
    ),
    Skin("ocean", "Ocean",
        colors = listOf(Color(0xFF0F4C75), Color(0xFF051937), Color.Black),
        lightColors = listOf(Color(0xFFF0F8FC), Color(0xFFDCEEF8)),
        accentDark = Pulse.Cyan, accentLight = Color(0xFF006782)
    ),
    Skin("sunrise", "Sunrise",
        colors = listOf(Color(0xFFFF8C78), Color(0xFFC2377A), Color(0xFF2B1055)),
        lightColors = listOf(Color(0xFFFFF6F0), Color(0xFFFCE4D6)),
        accentDark = Pulse.Coral, accentLight = Color(0xFFC8462A)
    ),
    Skin("forest", "Forest",
        colors = listOf(Color(0xFF0B3D2E), Color(0xFF03140F), Color.Black),
        lightColors = listOf(Color(0xFFF1F9F5), Color(0xFFDDEEE4)),
        accentDark = Pulse.Mint, accentLight = Color(0xFF14724E)
    ),
    Skin("mono", "Mono",
        colors = listOf(Color(0xFF2B2B2B), Color.Black),
        lightColors = listOf(Color(0xFFF7F7F8), Color(0xFFEAECEF)),
        accentDark = Color(0xFFE2E2EA), accentLight = Color(0xFF2C2C34)
    ),
    Skin("amoled", "Pure Black",
        colors = listOf(Color.Black, Color.Black),
        lightColors = listOf(Color(0xFFFAFAFC), Color(0xFFF0F0F5)),
        accentDark = Pulse.Cyan, accentLight = Color(0xFF006782)
    ),

    // Additive studio & player skins — IDs unchanged
    Skin("neon", "Neon",
        colors = listOf(Color(0xFF00C3FF), Color(0xFF9B7BFF), Color.Black),
        lightColors = listOf(Color(0xFFF0F9FF), Color(0xFFE4EAFF)),
        accentDark = Pulse.Cyan, accentLight = Color(0xFF0066CC)
    ),
    Skin("candy", "Candy",
        colors = listOf(Color(0xFFFF6DAA), Color(0xFFB36BFF), Color(0xFF120310)),
        lightColors = listOf(Color(0xFFFFF4FA), Color(0xFFF8E1F4)),
        accentDark = Pulse.Pink, accentLight = Color(0xFFB02272)
    ),
    Skin("lava", "Lava",
        colors = listOf(Color(0xFFFF9500), Color(0xFFFF2A6D), Color(0xFF1A0508)),
        lightColors = listOf(Color(0xFFFFF6EF), Color(0xFFFDE3D4)),
        accentDark = Pulse.Orange, accentLight = Color(0xFFD04A00)
    ),
    Skin("pixel_material", "Pixel Material",
        colors = listOf(Color(0xFF295A7E), Color(0xFF132F47), Color.Black),
        lightColors = listOf(Color(0xFFF1F6FB), Color(0xFFDCE9F5)),
        accentDark = Color(0xFF7CC6FF), accentLight = Color(0xFF1B5B8A)
    ),
    Skin("namida_amber", "Namida Amber",
        colors = listOf(Color(0xFFF1D6B6), Color(0xFF463E35), Color(0xFF1E1A17)),
        lightColors = listOf(Color(0xFFFCF7F0), Color(0xFFF4E6D4)),
        accentDark = Color(0xFFF1D6B6), accentLight = Color(0xFF8A5A19)
    ),
    Skin("namida_neon", "Cyberpunk",
        colors = listOf(Color(0xFF00F0FF), Color(0xFF7B55C1), Color(0xFF0D0B14)),
        lightColors = listOf(Color(0xFFF1FBFF), Color(0xFFE2E8FD)),
        accentDark = Color(0xFF00F0FF), accentLight = Color(0xFF006782)
    ),
    Skin("atmospheric_mist", "Atmospheric Mist",
        colors = listOf(Color(0xFF183B38), Color(0xFF071312), Color.Black),
        lightColors = listOf(Color(0xFFF2F9F7), Color(0xFFDCECE8)),
        accentDark = Color(0xFF70D8C2), accentLight = Color(0xFF116B5B)
    ),
    Skin("dark_plum", "Dark Plum",
        colors = listOf(Color(0xFF1A0230), Color(0xFF0D0118), Color.Black),
        lightColors = listOf(Color(0xFFF7F2FC), Color(0xFFEADDF7)),
        accentDark = Pulse.Violet, accentLight = Color(0xFF6B2CB8)
    ),
    Skin("light_warm", "Warm Linen",
        colors = listOf(Color(0xFF46392E), Color(0xFF241C16), Color(0xFF120E0B)),
        lightColors = listOf(Color(0xFFFFF8EE), Color(0xFFF9EAD6)),
        accentDark = Color(0xFFFFBE76), accentLight = Color(0xFF9C5214),
        isDaylight = true
    ),
    Skin("expressive_mat", "Expressive Indigo",
        colors = listOf(Color(0xFF1A237E), Color(0xFF283593), Color(0xFF0D1342)),
        lightColors = listOf(Color(0xFFF2F4FC), Color(0xFFE0E5FA)),
        accentDark = Color(0xFF8C9EFF), accentLight = Color(0xFF303F9F)
    ),
    Skin("monochrome", "Monochrome",
        colors = listOf(Color(0xFF212121), Color(0xFF1A1A1A), Color.Black),
        lightColors = listOf(Color(0xFFF6F6F7), Color(0xFFE8E8EC)),
        accentDark = Color(0xFFE0E0E5), accentLight = Color(0xFF2F2F37)
    ),

    // Dedicated Daylight Light skins
    Skin("daylight_mist", "Daylight Mist",
        colors = listOf(Color(0xFF243447), Color(0xFF141E2B), Color(0xFF0A0F16)),
        lightColors = listOf(Color(0xFFF7FAFD), Color(0xFFE5EFF8)),
        accentDark = Color(0xFF64B5F6), accentLight = Color(0xFF0B5C8E),
        isDaylight = true
    ),
    Skin("soft_lavender", "Soft Lavender",
        colors = listOf(Color(0xFF3B2C59), Color(0xFF1D1530), Color(0xFF0C0816)),
        lightColors = listOf(Color(0xFFFAF7FF), Color(0xFFECE3FA)),
        accentDark = Color(0xFFB39DDB), accentLight = Color(0xFF5D3BA8),
        isDaylight = true
    ),
    Skin("sky_studio", "Sky Studio",
        colors = listOf(Color(0xFF113F59), Color(0xFF082233), Color(0xFF030D14)),
        lightColors = listOf(Color(0xFFF2F9FF), Color(0xFFD9EEFF)),
        accentDark = Color(0xFF4FC3F7), accentLight = Color(0xFF006494),
        isDaylight = true
    ),
    Skin("botanical_sage", "Botanical Sage",
        colors = listOf(Color(0xFF1F3B2E), Color(0xFF0E2118), Color(0xFF060F0B)),
        lightColors = listOf(Color(0xFFF4FAF5), Color(0xFFDFEFE3)),
        accentDark = Color(0xFF6AD49F), accentLight = Color(0xFF1B6E47),
        isDaylight = true
    )
)
