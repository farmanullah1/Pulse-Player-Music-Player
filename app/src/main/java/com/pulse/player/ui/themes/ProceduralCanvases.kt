package com.pulse.player.ui.themes

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.pulse.player.playback.PlaybackUiState
import com.pulse.player.ui.components.Artwork
import com.pulse.player.ui.theme.LocalReducedMotion
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun StarField(modifier: Modifier = Modifier) {
    val reduced = LocalReducedMotion.current
    val stars = remember {
        List(90) { Triple(Random.nextFloat(), Random.nextFloat(), Random.nextFloat() * 1.6f + 0.4f) }
    }
    val twinkle by rememberInfiniteTransition(label = "tw").animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2400), RepeatMode.Reverse),
        label = "t"
    )
    Canvas(modifier) {
        if (size.width <= 1f || size.height <= 1f) return@Canvas
        stars.forEachIndexed { i, (x, y, r) ->
            drawCircle(
                color = Color.White.copy(alpha = if (reduced) 0.8f else if (i % 3 == 0) twinkle else 0.8f),
                radius = r.dp.toPx(),
                center = Offset(x * size.width, y * size.height)
            )
        }
    }
}

@Composable
fun WavesCanvas(modifier: Modifier = Modifier) {
    val reduced = LocalReducedMotion.current
    val phase by rememberInfiniteTransition(label = "w").animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(6000, easing = LinearEasing)),
        label = "p"
    )
    Canvas(modifier) {
        if (size.width <= 1f || size.height <= 1f) return@Canvas
        listOf(0.55f to 0.35f, 0.65f to 0.25f, 0.75f to 0.18f).forEachIndexed { i, (yy, a) ->
            val path = Path().apply {
                moveTo(0f, size.height)
                var x = 0f
                while (x <= size.width) {
                    lineTo(
                        x,
                        size.height * yy + 18.dp.toPx() * sin(x / size.width * 6f + (if (reduced) 0f else phase) + i)
                    )
                    x += 8f
                }
                lineTo(size.width, size.height)
                close()
            }
            drawPath(path, Color(0xFF00C3FF).copy(alpha = a))
        }
    }
}

@Composable
fun MountainSunset(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        if (size.width <= 1f || size.height <= 1f) return@Canvas
        drawRect(Brush.verticalGradient(listOf(Color(0xFFFF8C5A), Color(0xFFC2377A), Color(0xFF1B1030))))
        drawCircle(Color(0xFFFFD27A), (size.width * 0.12f).coerceAtLeast(1f), Offset(size.width * 0.7f, size.height * 0.45f))
        listOf(0.62f to Color(0xFF4B2A6B), 0.75f to Color(0xFF2B1B4A), 0.88f to Color(0xFF140C28)).forEach { (base, c) ->
            val p = Path().apply {
                moveTo(0f, size.height)
                lineTo(0f, size.height * base)
                var x = 0f
                var up = true
                val step = (size.width / 5f).coerceAtLeast(10f)
                while (x < size.width) {
                    x += step
                    lineTo(x, size.height * (base + if (up) -0.08f else 0.02f))
                    up = !up
                }
                lineTo(size.width, size.height)
                close()
            }
            drawPath(p, c)
        }
    }
}

@Composable
fun VinylDisc(s: PlaybackUiState, modifier: Modifier = Modifier) {
    val rot = remember { Animatable(0f) }
    val reduced = LocalReducedMotion.current
    LaunchedEffect(s.isPlaying, reduced) {
        if (s.isPlaying && !reduced) {
            while (true) rot.animateTo(rot.value + 360f, tween(6000, easing = LinearEasing))
        }
    }
    val arm by animateFloatAsState(if (s.isPlaying) 0f else -25f, spring(), label = "arm")
    Box(modifier.size(300.dp), Alignment.Center) {
        Canvas(Modifier.size(280.dp).graphicsLayer { rotationZ = rot.value }) {
            if (size.minDimension <= 1f) return@Canvas
            drawCircle(Color(0xFF0B0B0F))
            for (i in 1..9) {
                drawCircle(
                    Color.White.copy(0.05f),
                    size.minDimension / 2 * (0.35f + i * 0.065f),
                    style = Stroke(1.dp.toPx())
                )
            }
        }
        Artwork(s.artworkUri, 110.dp, Modifier.graphicsLayer { rotationZ = rot.value }, radius = 55.dp, px = 256)
        Canvas(
            Modifier.matchParentSize().graphicsLayer {
                rotationZ = arm
                transformOrigin = TransformOrigin(0.92f, 0.08f)
            }
        ) {
            if (size.width <= 1f || size.height <= 1f) return@Canvas
            drawLine(
                Color(0xFFDDDDDD),
                Offset(size.width * 0.92f, size.height * 0.08f),
                Offset(size.width * 0.55f, size.height * 0.5f),
                6.dp.toPx(),
                StrokeCap.Round
            )
        }
    }
}

@Composable
fun AcousticDisc(s: PlaybackUiState, modifier: Modifier = Modifier) {
    val rot = remember { Animatable(0f) }
    val reduced = LocalReducedMotion.current
    LaunchedEffect(s.isPlaying, reduced) {
        if (s.isPlaying && !reduced) {
            while (true) rot.animateTo(rot.value + 360f, tween(12000, easing = LinearEasing))
        }
    }
    Box(modifier.size(300.dp), Alignment.Center) {
        Canvas(Modifier.size(280.dp).graphicsLayer { rotationZ = rot.value }) {
            if (size.minDimension <= 1f) return@Canvas
            drawCircle(Brush.radialGradient(listOf(Color(0xFF6B3E26), Color(0xFF1E0F08))))
            for (i in 1..8) {
                drawCircle(
                    Color(0xFFB57E52).copy(alpha = 0.08f),
                    size.minDimension / 2 * (0.4f + i * 0.07f),
                    style = Stroke(1.5.dp.toPx())
                )
            }
        }
        Artwork(s.artworkUri, 120.dp, Modifier.graphicsLayer { rotationZ = rot.value }, radius = 60.dp, px = 256)
    }
}
