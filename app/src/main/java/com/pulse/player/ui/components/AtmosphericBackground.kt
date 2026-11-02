package com.pulse.player.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.pulse.player.ui.theme.LocalReducedMotion

/** A dependency-free forest/mist backdrop that stays legible behind controls. */
@Composable
fun AtmosphericBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val reducedMotion = LocalReducedMotion.current
    val transition = rememberInfiniteTransition(label = "atmospheric-mist")
    val drift by if (reducedMotion) {
        androidx.compose.runtime.remember { androidx.compose.runtime.mutableFloatStateOf(0.5f) }
    } else {
        transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(16_000, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "mist-drift"
        )
    }

    Box(modifier = modifier) {
        Canvas(Modifier.fillMaxSize()) {
            drawAtmosphere(drift)
        }
        content()
    }
}

private fun DrawScope.drawAtmosphere(drift: Float) {
    val w = size.width
    val h = size.height
    drawRect(
        brush = Brush.verticalGradient(
            listOf(Color(0xFF102D2B), Color(0xFF071817), Color(0xFF020706))
        )
    )

    // Soft moonlight and distant fog create depth without requiring a bitmap asset.
    drawCircle(
        brush = Brush.radialGradient(
            listOf(Color(0xFFBBD8C8).copy(alpha = 0.22f), Color.Transparent),
            radius = w * 0.46f
        ),
        radius = w * 0.46f,
        center = Offset(w * (0.72f + drift * 0.04f), h * 0.18f)
    )
    drawFogBand(h * 0.34f, h * 0.16f, w * (0.30f + drift * 0.06f), Color(0xFFB8D2C5), 0.12f)
    drawFogBand(h * 0.54f, h * 0.20f, w * (0.62f - drift * 0.08f), Color(0xFF8EAFA1), 0.10f)

    // Layered evergreen silhouettes; the bottom gradient keeps white text readable.
    val treeColor = Color(0xFF061211).copy(alpha = 0.80f)
    for (i in 0..8) {
        val x = w * (i / 8f) + (drift - 0.5f) * 18f
        val treeHeight = h * (0.28f + (i % 3) * 0.055f)
        drawTree(x, h * 0.82f, treeHeight, w * 0.10f, treeColor)
    }
    drawRect(
        brush = Brush.verticalGradient(
            0f to Color.Transparent,
            0.55f to Color.Black.copy(alpha = 0.18f),
            1f to Color.Black.copy(alpha = 0.78f)
        )
    )
}

private fun DrawScope.drawFogBand(y: Float, height: Float, x: Float, color: Color, alpha: Float) {
    drawOval(
        brush = Brush.radialGradient(listOf(color.copy(alpha = alpha), Color.Transparent)),
        topLeft = Offset(x - size.width * 0.48f, y - height / 2f),
        size = Size(size.width * 0.96f, height)
    )
}

private fun DrawScope.drawTree(x: Float, baseY: Float, height: Float, width: Float, color: Color) {
    val path = Path().apply {
        moveTo(x, baseY - height)
        lineTo(x - width * 0.42f, baseY - height * 0.56f)
        lineTo(x - width * 0.18f, baseY - height * 0.56f)
        lineTo(x - width * 0.52f, baseY - height * 0.25f)
        lineTo(x - width * 0.20f, baseY - height * 0.25f)
        lineTo(x - width * 0.62f, baseY)
        lineTo(x + width * 0.62f, baseY)
        lineTo(x + width * 0.20f, baseY - height * 0.25f)
        lineTo(x + width * 0.52f, baseY - height * 0.25f)
        lineTo(x + width * 0.18f, baseY - height * 0.56f)
        lineTo(x + width * 0.42f, baseY - height * 0.56f)
        close()
    }
    drawPath(path, color)
    drawLine(color.copy(alpha = 0.3f), Offset(x, baseY - height * 0.2f), Offset(x, baseY), strokeWidth = 2f)
}
