package com.pulse.player.ui.theme

import android.graphics.BitmapFactory
import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.pulse.player.data.settings.ColorSource
import com.pulse.player.data.settings.FontStylePreset
import com.pulse.player.data.settings.ThemeMode
import com.pulse.player.ui.themes.MountainSunset
import com.pulse.player.ui.themes.StarField
import com.pulse.player.ui.themes.WavesCanvas
import java.io.File

// ─── Composition locals ───────────────────────────────────────────────────────

val LocalReducedMotion = staticCompositionLocalOf { false }
val LocalSkin = staticCompositionLocalOf { Skins[0] }
val LocalAccent = staticCompositionLocalOf { Pulse.StudioAmber }
val LocalPulseTokens = staticCompositionLocalOf { PulseTokens.Dark }
val LocalDarkTheme = staticCompositionLocalOf { true }
val LocalFontStyle = staticCompositionLocalOf { FontStylePreset.MODERN }
val LocalListDensity = staticCompositionLocalOf { 1 }
val LocalAlbumGridColumns = staticCompositionLocalOf { 2 }

// ─── Dynamic color scheme builder ─────────────────────────────────────────────

/**
 * Ensures any dynamic/extracted accent color achieves readable contrast against
 * either dark surfaces (lifted luminance) or light surfaces (deepened luminance).
 */
fun normalizeAccentForMode(raw: Color, dark: Boolean): Color {
    val lum = raw.luminance()
    return if (dark) {
        when {
            lum < 0.25f -> lerp(raw, Color.White, 0.42f)
            lum < 0.42f -> lerp(raw, Color.White, 0.2f)
            else -> raw
        }
    } else {
        when {
            lum > 0.35f -> lerp(raw, Color(0xFF0F131A), 0.52f)
            lum > 0.22f -> lerp(raw, Color(0xFF0F131A), 0.28f)
            else -> raw
        }
    }
}

private fun buildColorScheme(
    dark: Boolean,
    amoled: Boolean,
    accent: Color,
    skin: Skin
): ColorScheme {
    val bg = when {
        amoled -> Color.Black
        dark -> Pulse.Bg
        else -> skin.lightColors.firstOrNull() ?: Color(0xFFF7F6FA)
    }
    val surface = when {
        amoled -> Color(0xFF0D0E12)
        dark -> Pulse.Surface
        else -> Color.White
    }
    val surfaceVariant = when {
        amoled -> Color(0xFF16181F)
        dark -> Pulse.Elevated
        else -> skin.lightColors.lastOrNull() ?: Color(0xFFEEEAF7)
    }
    val onPrimary = if (accent.luminance() > 0.56f) Color(0xFF0A0D12) else Color.White

    return if (dark) {
        val primaryContainer = lerp(surfaceVariant, accent, 0.28f)
        val secondary = lerp(accent, Pulse.Violet, 0.35f)
        val tertiary = lerp(accent, Pulse.Mint, 0.45f)
        darkColorScheme(
            primary = accent,
            onPrimary = onPrimary,
            primaryContainer = primaryContainer,
            onPrimaryContainer = Pulse.TextHi,
            secondary = secondary,
            onSecondary = if (secondary.luminance() > 0.56f) Color(0xFF0A0D12) else Color.White,
            secondaryContainer = lerp(surfaceVariant, secondary, 0.24f),
            onSecondaryContainer = Pulse.TextHi,
            tertiary = tertiary,
            onTertiary = if (tertiary.luminance() > 0.56f) Color(0xFF0A0D12) else Color.White,
            tertiaryContainer = lerp(surfaceVariant, tertiary, 0.24f),
            onTertiaryContainer = Pulse.TextHi,
            background = bg,
            onBackground = Pulse.TextHi,
            surface = surface,
            onSurface = Pulse.TextHi,
            surfaceVariant = surfaceVariant,
            surfaceContainerLowest = if (amoled) Color.Black else Color(0xFF0C0D12),
            surfaceContainerLow = if (amoled) Color(0xFF090A0D) else Color(0xFF15171E),
            surfaceContainer = surface,
            surfaceContainerHigh = if (amoled) Color(0xFF1C1E26) else Color(0xFF282B36),
            surfaceContainerHighest = if (amoled) Color(0xFF232630) else Color(0xFF313544),
            surfaceBright = if (amoled) Color(0xFF252832) else Color(0xFF2E3240),
            surfaceDim = bg,
            surfaceTint = accent,
            onSurfaceVariant = Pulse.TextMid,
            inverseSurface = Pulse.TextHi,
            inverseOnSurface = Color(0xFF14151B),
            inversePrimary = lerp(accent, Color.Black, 0.35f),
            outline = if (amoled) Color(0xFF343846) else Pulse.Outline,
            outlineVariant = if (amoled) Color(0xFF222530) else Color(0xFF2A2E3B),
            scrim = Color.Black,
            error = Pulse.Error,
            onError = Color(0xFF1D0209),
            errorContainer = Color(0xFF4A1020),
            onErrorContainer = Color(0xFFFFD9DF)
        )
    } else {
        val primaryContainer = lerp(Color.White, accent, 0.16f)
        val secondary = lerp(accent, Color(0xFF4A3D8F), 0.3f)
        val tertiary = lerp(accent, Color(0xFF007955), 0.4f)
        lightColorScheme(
            primary = accent,
            onPrimary = onPrimary,
            primaryContainer = primaryContainer,
            onPrimaryContainer = Color(0xFF141122),
            secondary = secondary,
            onSecondary = Color.White,
            secondaryContainer = lerp(Color.White, secondary, 0.14f),
            onSecondaryContainer = Color(0xFF1A1625),
            tertiary = tertiary,
            onTertiary = Color.White,
            tertiaryContainer = lerp(Color.White, tertiary, 0.16f),
            onTertiaryContainer = Color(0xFF0C2019),
            background = bg,
            onBackground = Color(0xFF181523),
            surface = surface,
            onSurface = Color(0xFF181523),
            surfaceVariant = surfaceVariant,
            surfaceContainerLowest = Color.White,
            surfaceContainerLow = lerp(bg, Color.White, 0.5f),
            surfaceContainer = lerp(bg, surfaceVariant, 0.45f),
            surfaceContainerHigh = lerp(surfaceVariant, Color.White, 0.35f),
            surfaceContainerHighest = surfaceVariant,
            surfaceBright = Color.White,
            surfaceDim = surfaceVariant,
            surfaceTint = accent,
            onSurfaceVariant = Color(0xFF524C63),
            inverseSurface = Color(0xFF22202E),
            inverseOnSurface = Color(0xFFF5F4FA),
            inversePrimary = lerp(accent, Color.White, 0.35f),
            outline = Color(0xFFC9C0DF),
            outlineVariant = Color(0xFFE3DDF0),
            scrim = Color.Black,
            error = Color(0xFFC62846),
            onError = Color.White,
            errorContainer = Color(0xFFFFDADE),
            onErrorContainer = Color(0xFF41000E)
        )
    }
}

// ─── PulseTheme ───────────────────────────────────────────────────────────────

@Composable
fun PulseTheme(
    mode: ThemeMode,
    skin: Skin = Skins[0],
    reducedMotion: Boolean = false,
    accent: Color = Pulse.StudioAmber,
    colorSource: ColorSource = ColorSource.PULSE_PALETTE,
    seedColor: Int = 0,
    artworkAccent: Color = Pulse.StudioAmber,
    cornerRadiusDp: Int = 16,
    fontStyle: FontStylePreset = FontStylePreset.MODERN,
    listDensity: Int = 1,
    albumGridColumns: Int = 2,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val dark = when (mode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT  -> false
        else             -> true
    }
    val amoled = mode == ThemeMode.AMOLED

    // Resolve accent color cleanly for the active light/dark polarity
    val rawAccent: Color = when (colorSource) {
        ColorSource.PULSE_PALETTE  -> if (dark) skin.accentDark else skin.accentLight
        ColorSource.ARTWORK_DRIVEN -> normalizeAccentForMode(artworkAccent, dark)
        ColorSource.CUSTOM_SEED    -> {
            val base = if (seedColor != 0) Color(seedColor) else accent
            normalizeAccentForMode(base, dark)
        }
        ColorSource.MATERIAL_YOU   -> if (dark) skin.accentDark else skin.accentLight
    }

    val scheme = when {
        colorSource == ColorSource.MATERIAL_YOU && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val dynamic = if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            if (amoled) {
                dynamic.copy(
                    background = Color.Black,
                    surface = Color(0xFF0A0A0C),
                    surfaceVariant = Color(0xFF14141A),
                    surfaceContainerHigh = Color(0xFF1A1A22),
                    surfaceContainerLow = Color(0xFF060608)
                )
            } else {
                dynamic
            }
        }
        else -> buildColorScheme(dark = dark, amoled = amoled, accent = rawAccent, skin = skin)
    }

    val resolvedAccent = scheme.primary

    val tokens = when {
        amoled -> PulseTokens.Amoled.copy(
            accent = resolvedAccent,
            accentOnSurface = resolvedAccent,
            playbackActive = resolvedAccent,
            navSelected = resolvedAccent,
            selectedOverlay = resolvedAccent.copy(alpha = 0.16f),
            queueCurrent = resolvedAccent.copy(alpha = 0.15f)
        )
        dark -> PulseTokens.Dark.copy(
            accent = resolvedAccent,
            accentOnSurface = resolvedAccent,
            playbackActive = resolvedAccent,
            navSelected = resolvedAccent,
            selectedOverlay = resolvedAccent.copy(alpha = 0.15f),
            queueCurrent = resolvedAccent.copy(alpha = 0.15f)
        )
        else -> PulseTokens.Light.copy(
            bgPrimary = scheme.background,
            bgSecondary = scheme.surface,
            bgElevated = scheme.surfaceVariant,
            accent = resolvedAccent,
            accentOnSurface = resolvedAccent,
            playbackActive = resolvedAccent,
            navSelected = resolvedAccent,
            selectedOverlay = resolvedAccent.copy(alpha = 0.12f),
            queueCurrent = resolvedAccent.copy(alpha = 0.12f)
        )
    }

    val radius = cornerRadiusDp.coerceIn(0, 32)
    val typography = remember(fontStyle) { pulseTypography(fontStyle) }

    CompositionLocalProvider(
        LocalReducedMotion provides reducedMotion,
        LocalSkin provides skin,
        LocalAccent provides resolvedAccent,
        LocalPulseTokens provides tokens,
        LocalDarkTheme provides dark,
        LocalFontStyle provides fontStyle,
        LocalListDensity provides listDensity,
        LocalAlbumGridColumns provides albumGridColumns
    ) {
        MaterialTheme(
            colorScheme = scheme,
            typography = typography,
            shapes = Shapes(
                extraSmall = RoundedCornerShape((radius / 4).dp),
                small      = RoundedCornerShape((radius * 5 / 8).dp),
                medium     = RoundedCornerShape(radius.dp),
                large      = RoundedCornerShape((radius * 3 / 2).coerceAtMost(32).dp),
                extraLarge = RoundedCornerShape((radius * 2).coerceAtMost(32).dp)
            )
        ) {
            CompositionLocalProvider(LocalContentColor provides scheme.onBackground) {
                content()
            }
        }
    }
}

// ─── AppBackground ────────────────────────────────────────────────────────────

/** Screen background: skin gradient (light or dark variant), wallpaper, or flat AMOLED black. */
@Composable
fun AppBackground(
    mode: ThemeMode,
    modifier: Modifier = Modifier,
    wallpaperPath: String? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val skin = LocalSkin.current
    val dark = LocalDarkTheme.current
    val bgBrush = when {
        mode == ThemeMode.AMOLED -> Brush.verticalGradient(listOf(Color.Black, Color.Black))
        !dark -> Brush.verticalGradient(skin.lightColors)
        else -> Brush.verticalGradient(
            listOf(
                lerp(Pulse.Bg, skin.colors.firstOrNull() ?: Pulse.Bg, 0.38f),
                Pulse.Bg
            )
        )
    }

    Box(modifier.fillMaxSize().background(bgBrush)) {
        if (wallpaperPath != null && mode != ThemeMode.AMOLED) {
            when {
                wallpaperPath == "preset:starry"   -> StarField(Modifier.fillMaxSize())
                wallpaperPath == "preset:waves"    -> WavesCanvas(Modifier.fillMaxSize())
                wallpaperPath == "preset:mountain" -> MountainSunset(Modifier.fillMaxSize())
                File(wallpaperPath).exists() -> {
                    val bmp = remember(wallpaperPath) {
                        runCatching { BitmapFactory.decodeFile(wallpaperPath) }.getOrNull()
                    }
                    if (bmp != null) {
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        val scrimColor = if (dark) Color.Black.copy(alpha = 0.56f) else Color.White.copy(alpha = 0.65f)
                        Box(Modifier.fillMaxSize().background(scrimColor))
                    }
                }
            }
        }
        CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onBackground) {
            content()
        }
    }
}
