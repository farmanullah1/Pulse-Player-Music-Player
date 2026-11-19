package com.pulse.player.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pulse.player.playback.PlaybackUiState
import com.pulse.player.ui.theme.LocalReducedMotion
import com.pulse.player.ui.theme.Pulse
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.abs

@Composable
fun MiniPlayer(
    s: PlaybackUiState,
    pos: StateFlow<Long>,
    onToggle: () -> Unit,
    onQueue: () -> Unit,
    onExpand: () -> Unit,
    modifier: Modifier = Modifier,
    onNext: (() -> Unit)? = null,
    onPrevious: (() -> Unit)? = null,
    artworkAccent: Color = MaterialTheme.colorScheme.primary,
    isFavorite: Boolean = false,
    onToggleFavorite: (() -> Unit)? = null
) {
    val rot = remember { Animatable(0f) }
    val reduced = LocalReducedMotion.current
    val haptic = LocalHapticFeedback.current
    var dragDx by remember { mutableFloatStateOf(0f) }
    var dragDy by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(s.isPlaying, reduced) {
        if (s.isPlaying && !reduced) {
            while (true) rot.animateTo(rot.value + 360f, tween(8000, easing = LinearEasing))
        }
    }
    val position by pos.collectAsStateWithLifecycle()

    val targetTint = if (s.artworkUri.isNullOrBlank()) {
        MaterialTheme.colorScheme.primary
    } else {
        artworkAccent
    }
    val effectiveTint by animateColorAsState(
        targetValue = targetTint,
        animationSpec = tween(if (reduced) 0 else 420),
        label = "mini_player_tint"
    )
    val containerColor = lerp(MaterialTheme.colorScheme.surfaceContainerHigh, effectiveTint, 0.14f)
    val borderColor = if (s.trackError != null) {
        MaterialTheme.colorScheme.error.copy(alpha = 0.65f)
    } else {
        effectiveTint.copy(alpha = 0.30f)
    }

    Surface(
        modifier = modifier
            .padding(horizontal = 12.dp)
            .fillMaxWidth()
            .height(68.dp)
            .testTag("mini_player_bar")
            .pointerInput(s.mediaId, onNext, onPrevious, onExpand) {
                detectDragGestures(
                    onDragStart = {
                        dragDx = 0f
                        dragDy = 0f
                    },
                    onDragEnd = {
                        if (abs(dragDy) > abs(dragDx) && dragDy < -56f) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onExpand()
                        } else if (abs(dragDx) >= abs(dragDy)) {
                            if (dragDx < -70f && onNext != null) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onNext()
                            } else if (dragDx > 70f && onPrevious != null) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onPrevious()
                            }
                        }
                        dragDx = 0f
                        dragDy = 0f
                    },
                    onDragCancel = {
                        dragDx = 0f
                        dragDy = 0f
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        dragDx += dragAmount.x
                        dragDy += dragAmount.y
                    }
                )
            }
            .clickable(onClick = onExpand),
        shape = RoundedCornerShape(24.dp),
        color = containerColor,
        border = BorderStroke(1.dp, borderColor),
        tonalElevation = 2.dp,
        shadowElevation = 2.dp
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val showInlineQueue = maxWidth >= 372.dp
            Box(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = 10.dp, end = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Artwork(
                        s.artworkUri,
                        46.dp,
                        Modifier.graphicsLayer { rotationZ = rot.value },
                        radius = 23.dp,
                        px = 128
                    )
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = s.title.ifBlank { "Nothing playing" },
                            maxLines = 1,
                            modifier = Modifier.basicMarquee(),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (s.trackError != null) {
                                Icon(
                                    imageVector = Icons.Rounded.ErrorOutline,
                                    contentDescription = "Playback error",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                            }
                            Text(
                                text = if (s.trackError != null) {
                                    s.trackError.message
                                } else {
                                    s.artist.ifBlank { "Select a song to start" }
                                },
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (s.trackError != null) {
                                    MaterialTheme.colorScheme.error
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }
                    }

                    if (onToggleFavorite != null && s.mediaId != null) {
                        IconButton(
                            onClick = onToggleFavorite,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("mini_player_favorite_button")
                        ) {
                            Icon(
                                imageVector = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                                contentDescription = if (isFavorite) "Remove from favorites" else "Add to favorites",
                                tint = if (isFavorite) Pulse.Coral else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(21.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = onToggle,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("mini_player_play_pause_button")
                    ) {
                        AnimatedContent(s.isPlaying, label = "pp") { p ->
                            Icon(
                                imageVector = if (p) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                contentDescription = if (p) "Pause" else "Play",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }

                    if (onNext != null) {
                        IconButton(
                            onClick = onNext,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("mini_player_next_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.SkipNext,
                                contentDescription = "Next track",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    if (showInlineQueue) {
                        IconButton(
                            onClick = onQueue,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("mini_player_queue_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.QueueMusic,
                                contentDescription = "Open queue",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }

                LinearProgressIndicator(
                    progress = {
                        if (s.durationMs > 0) (position / s.durationMs.toFloat()).coerceIn(0f, 1f) else 0f
                    },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 24.dp)
                        .fillMaxWidth()
                        .height(2.5.dp),
                    color = if (s.trackError != null) MaterialTheme.colorScheme.error else effectiveTint,
                    trackColor = effectiveTint.copy(alpha = 0.16f)
                )
            }
        }
    }
}
