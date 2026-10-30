package com.pulse.player.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pulse.player.ui.theme.Pulse
import kotlin.math.abs
import kotlin.math.sin

/**
 * Interactive Audio Waveform Seekbar adapted from Namida.
 * Renders multi-bar waveform profile with active/inactive coloring, touch seeking,
 * and live playback energy modulation.
 */
@Composable
fun WaveformSeekBar(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isPlaying: Boolean = true,
    seedKey: String = "default",
    onValueChangeFinished: (() -> Unit)? = null,
    activeColor: Color = Pulse.Orange,
    inactiveColor: Color = Color.White.copy(alpha = 0.25f),
    barCount: Int = 48,
    barSpacing: Dp = 3.dp,
    height: Dp = 48.dp
) {
    var isDragging by remember { mutableStateOf(false) }
    var dragFraction by remember { mutableFloatStateOf(0f) }

    val currentFraction = if (isDragging) dragFraction else value.coerceIn(0f, 1f)

    // Pre-calculate deterministic waveform bar heights for this track
    val waveBars = remember(seedKey, barCount) {
        val hash = seedKey.hashCode().toLong()
        FloatArray(barCount) { i ->
            val angle = i * 0.28f + (hash and 0xFF) * 0.05f
            val base = abs(sin(angle)) * 0.65f + abs(sin(angle * 2.3f)) * 0.35f
            base.coerceIn(0.12f, 1.0f)
        }
    }

    val energyTransition = rememberInfiniteTransition(label = "waveform_pulse")
    val energyPhase by energyTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "energy"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .semantics {
                progressBarRangeInfo = ProgressBarRangeInfo(currentFraction, 0f..1f)
            }
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                detectTapGestures(
                    onPress = { offset ->
                        val w = size.width
                        if (w > 0) {
                            val target = (offset.x / w).coerceIn(0f, 1f)
                            isDragging = true
                            dragFraction = target
                            onValueChange(target)
                            tryAwaitRelease()
                            isDragging = false
                            onValueChangeFinished?.invoke()
                        }
                    }
                )
            }
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                detectDragGestures(
                    onDragStart = { offset ->
                        isDragging = true
                        val w = size.width
                        if (w > 0) {
                            dragFraction = (offset.x / w).coerceIn(0f, 1f)
                            onValueChange(dragFraction)
                        }
                    },
                    onDragEnd = {
                        isDragging = false
                        onValueChangeFinished?.invoke()
                    },
                    onDragCancel = {
                        isDragging = false
                        onValueChangeFinished?.invoke()
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        val w = size.width
                        if (w > 0) {
                            val newFrac = (change.position.x / w).coerceIn(0f, 1f)
                            dragFraction = newFrac
                            onValueChange(newFrac)
                        }
                    }
                )
            }
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val width = size.width
            val totalHeight = size.height
            val centerY = totalHeight / 2f
            val spacingPx = barSpacing.toPx()
            val totalSpacing = (barCount - 1) * spacingPx
            val barWidth = ((width - totalSpacing) / barCount).coerceAtLeast(2f)

            val playheadX = width * currentFraction

            for (i in 0 until barCount) {
                val x = i * (barWidth + spacingPx)
                val isPlayed = (x + barWidth / 2f) <= playheadX

                val rawHeightFraction = waveBars[i]
                // Subtle live energy modulation on played bars when playing
                val modulation = if (isPlaying && isPlayed) {
                    0.85f + 0.15f * sin(energyPhase + i * 0.5f)
                } else 1.0f

                val barHeight = (totalHeight * rawHeightFraction * modulation).coerceAtLeast(4f)
                val y = centerY - barHeight / 2f

                drawRoundRect(
                    color = if (isPlayed) activeColor else inactiveColor,
                    topLeft = Offset(x, y),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
                )
            }

            // Playhead indicator vertical line
            drawLine(
                color = activeColor,
                start = Offset(playheadX, 0f),
                end = Offset(playheadX, totalHeight),
                strokeWidth = 2.dp.toPx()
            )
        }
    }
}

