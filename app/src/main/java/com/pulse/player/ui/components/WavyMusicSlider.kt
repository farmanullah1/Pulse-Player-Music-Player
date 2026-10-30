package com.pulse.player.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.sin

/**
 * Expressive Wavy Music Slider adapted from PixelPlayer.
 * Features dynamic sine-wave animation while playing, flattening to a straight line when paused or scrubbed,
 * and morphing thumb between circle and vertical pill.
 */
@Composable
fun WavyMusicSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isPlaying: Boolean = true,
    onValueChangeFinished: (() -> Unit)? = null,
    activeTrackColor: Color = MaterialTheme.colorScheme.primary,
    inactiveTrackColor: Color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
    thumbColor: Color = MaterialTheme.colorScheme.primary,
    waveAmplitude: Dp = 4.dp,
    waveLength: Dp = 72.dp,
    trackHeight: Dp = 4.dp
) {
    var isDragging by remember { mutableStateOf(false) }
    var dragFraction by remember { mutableFloatStateOf(0f) }

    val currentFraction = if (isDragging) dragFraction else value.coerceIn(0f, 1f)

    val waveTransition = rememberInfiniteTransition(label = "wavy_slider_phase")
    val phase by waveTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    val targetAmplitude = if (isPlaying && !isDragging && enabled) 1f else 0f
    val animatedAmplitude by animateFloatAsState(
        targetValue = targetAmplitude,
        animationSpec = tween(350, easing = FastOutSlowInEasing),
        label = "amplitude"
    )

    val thumbInteractionFraction by animateFloatAsState(
        targetValue = if (isDragging) 1f else 0f,
        animationSpec = tween(200, easing = FastOutSlowInEasing),
        label = "thumb_morph"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(36.dp)
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
            val height = size.height
            val centerY = height / 2f
            val thumbX = width * currentFraction
            val strokeWidthPx = trackHeight.toPx()
            val ampPx = waveAmplitude.toPx() * animatedAmplitude
            val waveLenPx = waveLength.toPx()

            // 1. Inactive track (straight line from thumbX to width)
            if (thumbX < width) {
                drawLine(
                    color = inactiveTrackColor,
                    start = Offset(thumbX, centerY),
                    end = Offset(width, centerY),
                    strokeWidth = strokeWidthPx,
                    cap = StrokeCap.Round
                )
            }

            // 2. Active track (wavy curve from 0 to thumbX)
            if (thumbX > 0f) {
                if (ampPx > 0.5f) {
                    val path = Path()
                    path.moveTo(0f, centerY)
                    val step = 4f
                    var x = 0f
                    while (x <= thumbX) {
                        val k = (2 * PI / waveLenPx).toFloat()
                        val y = centerY + ampPx * sin(k * x - phase)
                        path.lineTo(x, y)
                        x += step
                    }
                    path.lineTo(thumbX, centerY)

                    drawPath(
                        path = path,
                        color = activeTrackColor,
                        style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
                    )
                } else {
                    drawLine(
                        color = activeTrackColor,
                        start = Offset(0f, centerY),
                        end = Offset(thumbX, centerY),
                        strokeWidth = strokeWidthPx,
                        cap = StrokeCap.Round
                    )
                }
            }

            // 3. Thumb (morphs from circle radius 7dp to vertical pill 20dp high, 4dp wide)
            val thumbRadiusPx = 7.dp.toPx()
            val pillWidthPx = 4.dp.toPx()
            val pillHeightPx = 22.dp.toPx()

            val curThumbWidth = androidx.compose.ui.util.lerp(thumbRadiusPx * 2, pillWidthPx, thumbInteractionFraction)
            val curThumbHeight = androidx.compose.ui.util.lerp(thumbRadiusPx * 2, pillHeightPx, thumbInteractionFraction)
            val curCornerRadius = androidx.compose.ui.util.lerp(thumbRadiusPx, pillWidthPx / 2f, thumbInteractionFraction)

            drawRoundRect(
                color = thumbColor,
                topLeft = Offset(thumbX - curThumbWidth / 2f, centerY - curThumbHeight / 2f),
                size = Size(curThumbWidth, curThumbHeight),
                cornerRadius = CornerRadius(curCornerRadius, curCornerRadius)
            )
        }
    }
}

