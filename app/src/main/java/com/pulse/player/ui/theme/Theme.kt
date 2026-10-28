package com.pulse.player.ui.theme

import android.graphics.BitmapFactory
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
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.pulse.player.data.settings.ThemeMode
import com.pulse.player.ui.themes.MountainSunset
import com.pulse.player.ui.themes.StarField
import com.pulse.player.ui.themes.WavesCanvas
import java.io.File

private val DarkScheme = darkColorScheme(
    primary = Color.White,
    onPrimary = Color.Black,
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
    primary = Color(0xFF1A1625),
    onPrimary = Color.White,
    secondary = Color(0xFF0077A8),
    background = Color(0xFFFAF7FF),
    surface = Color.White,
    surfaceVariant = Color(0xFFF0EBFA),
    onSurface = Color(0xFF1A1625),
    onSurfaceVariant = Color(0xFF5E5870),
    outline = Color(0xFFD9D2EA),
    error = Color(0xFFD32F4F)
)

val LocalReducedMotion = staticCompositionLocalOf { false }
val LocalSkin = staticCompositionLocalOf { Skins[0] }
val LocalAccent = staticCompositionLocalOf { Pulse.Cyan }

@Composable
fun PulseTheme(
    mode: ThemeMode,
    skin: Skin,
    reducedMotion: Boolean,
    accent: Color,
    content: @Composable () -> Unit
) {
    val dark = when (mode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        else -> true
    }
    val scheme = if (dark) DarkScheme else LightScheme
    CompositionLocalProvider(
        LocalReducedMotion provides reducedMotion,
        LocalSkin provides skin,
        LocalAccent provides accent
    ) {
        MaterialTheme(
            colorScheme = scheme,
            typography = PulseTypography,
            shapes = Shapes(
                small = RoundedCornerShape(10.dp),
                medium = RoundedCornerShape(16.dp),
                large = RoundedCornerShape(24.dp)
            ),
            content = content
        )
    }
}

/** Screen background: skin gradient, wallpaper, or flat AMOLED black. */
@Composable
fun AppBackground(
    mode: ThemeMode,
    wallpaperPath: String? = null,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val skin = LocalSkin.current
    val bgBrush = when {
        mode == ThemeMode.AMOLED -> Brush.verticalGradient(listOf(Color.Black, Color.Black))
        mode == ThemeMode.LIGHT || (mode == ThemeMode.SYSTEM && !isSystemInDarkTheme()) ->
            Brush.verticalGradient(listOf(Color(0xFFFAF7FF), Color(0xFFF0EBFA)))
        else -> Brush.verticalGradient(skin.colors)
    }

    Box(modifier.fillMaxSize().background(bgBrush)) {
        if (wallpaperPath != null && mode != ThemeMode.AMOLED) {
            when {
                wallpaperPath == "preset:starry" -> StarField(Modifier.fillMaxSize())
                wallpaperPath == "preset:waves" -> WavesCanvas(Modifier.fillMaxSize())
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
