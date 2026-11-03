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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.pulse.player.data.settings.ColorSource
import com.pulse.player.data.settings.ThemeMode
import com.pulse.player.ui.themes.MountainSunset
import com.pulse.player.ui.themes.StarField
import com.pulse.player.ui.themes.WavesCanvas
import java.io.File

// ─── Material 3 color schemes ─────────────────────────────────────────────────

private val DarkScheme = darkColorScheme(
    primary = Pulse.Cyan,
    onPrimary = Color.Black,
    primaryContainer = Pulse.Elevated,
    onPrimaryContainer = Color.White,
    secondary = Pulse.Cyan,
    background = Pulse.Bg,
    surface = Pulse.Surface,
    surfaceVariant = Pulse.Elevated,
    onSurface = Color.White,
    onSurfaceVariant = Pulse.TextMid,
    outline = Pulse.Outline,
    error = Pulse.Error
)

private val LightScheme = lightColorScheme(
    primary = Color(0xFF006782),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFB9E9FF),
    onPrimaryContainer = Color(0xFF001F29),
    secondary = Color(0xFF0077A8),
    background = Color(0xFFFAF7FF),
    surface = Color.White,
    surfaceVariant = Color(0xFFF0EBFA),
    onSurface = Color(0xFF1A1625),
    onSurfaceVariant = Color(0xFF5E5870),
    outline = Color(0xFFD9D2EA),
    error = Color(0xFFD32F4F)
)

// ─── Composition locals ───────────────────────────────────────────────────────

val LocalReducedMotion = staticCompositionLocalOf { false }
val LocalSkin = staticCompositionLocalOf { Skins[0] }
val LocalAccent = staticCompositionLocalOf { Pulse.Cyan }
val LocalPulseTokens = staticCompositionLocalOf { PulseTokens.Dark }

// ─── PulseTheme ───────────────────────────────────────────────────────────────

/**
 * Root theme composable. Accepts all five independent customization axes:
 * - [mode] — ThemeMode (SYSTEM / DARK / AMOLED / LIGHT)
 * - [colorSource] — where the accent color comes from
 * - [seedColor] — ARGB int for custom seed (used when colorSource = CUSTOM_SEED)
 * - [artworkAccent] — color extracted from current album art (used when colorSource = ARTWORK_DRIVEN)
 * - [skin] — app background gradient/wallpaper
 * - [cornerRadiusDp] — global corner radius 0–32 dp
 * - [reducedMotion] — disables decorative animations
 */
@Composable
fun PulseTheme(
    mode: ThemeMode,
    skin: Skin = Skins[0],
    reducedMotion: Boolean = false,
    accent: Color = Pulse.Cyan,
    colorSource: ColorSource = ColorSource.PULSE_PALETTE,
    seedColor: Int = 0,
    artworkAccent: Color = Pulse.Cyan,
    cornerRadiusDp: Int = 16,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val dark = when (mode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT  -> false
        else             -> true
    }

    // Resolve M3 color scheme from colorSource
    val scheme = when {
        colorSource == ColorSource.MATERIAL_YOU && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        else -> if (dark) DarkScheme else LightScheme
    }

    // Resolve effective accent color
    val resolvedAccent: Color = when (colorSource) {
        ColorSource.PULSE_PALETTE  -> accent
        ColorSource.ARTWORK_DRIVEN -> artworkAccent
        ColorSource.CUSTOM_SEED    -> if (seedColor != 0) Color(seedColor) else accent
        ColorSource.MATERIAL_YOU   -> scheme.primary
    }

    // Select PulseTokens based on mode
    val tokens = when {
        mode == ThemeMode.AMOLED -> PulseTokens.Amoled.copy(
            accent = resolvedAccent, accentOnSurface = resolvedAccent,
            playbackActive = resolvedAccent, navSelected = resolvedAccent,
            selectedOverlay = resolvedAccent.copy(alpha = 0.15f)
        )
        dark -> PulseTokens.Dark.copy(
            accent = resolvedAccent, accentOnSurface = resolvedAccent,
            playbackActive = resolvedAccent, navSelected = resolvedAccent,
            selectedOverlay = resolvedAccent.copy(alpha = 0.15f)
        )
        else -> PulseTokens.Light.copy(
            accent = resolvedAccent, accentOnSurface = resolvedAccent,
            playbackActive = resolvedAccent, navSelected = resolvedAccent,
            selectedOverlay = resolvedAccent.copy(alpha = 0.12f)
        )
    }

    val radius = cornerRadiusDp.coerceIn(0, 32)

    CompositionLocalProvider(
        LocalReducedMotion provides reducedMotion,
        LocalSkin provides skin,
        LocalAccent provides resolvedAccent,
        LocalPulseTokens provides tokens
    ) {
        MaterialTheme(
            colorScheme = scheme,
            typography = PulseTypography,
            shapes = Shapes(
                extraSmall = RoundedCornerShape((radius / 4).dp),
                small      = RoundedCornerShape((radius * 5 / 8).dp),
                medium     = RoundedCornerShape(radius.dp),
                large      = RoundedCornerShape((radius * 3 / 2).coerceAtMost(32).dp),
                extraLarge = RoundedCornerShape((radius * 2).coerceAtMost(32).dp)
            ),
            content = content
        )
    }
}

// ─── AppBackground ────────────────────────────────────────────────────────────

/** Screen background: skin gradient, wallpaper, or flat AMOLED black. */
@Composable
fun AppBackground(
    mode: ThemeMode,
    modifier: Modifier = Modifier,
    wallpaperPath: String? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val skin = LocalSkin.current
    val bgBrush = when {
        mode == ThemeMode.AMOLED -> Brush.verticalGradient(listOf(Color.Black, Color.Black))
        mode == ThemeMode.LIGHT || (mode == ThemeMode.SYSTEM && !isSystemInDarkTheme()) ->
            Brush.verticalGradient(listOf(MaterialTheme.colorScheme.background, MaterialTheme.colorScheme.surfaceVariant))
        else -> Brush.verticalGradient(skin.colors)
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
                        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.45f)))
                    }
                }
            }
        }
        content()
    }
}
