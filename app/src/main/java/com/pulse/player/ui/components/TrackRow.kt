package com.pulse.player.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.pulse.player.playback.Track
import com.pulse.player.ui.theme.Pulse
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TrackRow(
    t: Track,
    current: Boolean,
    playing: Boolean,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    selectMode: Boolean = false,
    onSwipeRightQueue: (() -> Unit)? = null,
    onSwipeLeftFavorite: (() -> Unit)? = null,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onMore: () -> Unit
) {
    val offsetX = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else Color.Transparent)
    ) {
        // Swipe background hints
        if (!selectMode && (onSwipeRightQueue != null || onSwipeLeftFavorite != null)) {
            val curOffset = offsetX.value
            if (curOffset > 20f) {
                // Swiping right: Queue hint
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                        .padding(start = 24.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Icon(
                        Icons.Rounded.QueueMusic,
                        contentDescription = "Queue next",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            } else if (curOffset < -20f) {
                // Swiping left: Favorite hint
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(MaterialTheme.colorScheme.error.copy(alpha = 0.2f))
                        .padding(end = 24.dp),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Icon(
                        Icons.Rounded.Favorite,
                        contentDescription = "Favorite",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                .pointerInput(selectMode, t.id) {
                    if (!selectMode && (onSwipeRightQueue != null || onSwipeLeftFavorite != null)) {
                        detectHorizontalDragGestures(
                            onDragEnd = {
                                val cur = offsetX.value
                                scope.launch {
                                    if (cur > 140f && onSwipeRightQueue != null) {
                                        onSwipeRightQueue()
                                    } else if (cur < -140f && onSwipeLeftFavorite != null) {
                                        onSwipeLeftFavorite()
                                    }
                                    offsetX.animateTo(0f)
                                }
                            },
                            onDragCancel = {
                                scope.launch { offsetX.animateTo(0f) }
                            },
                            onHorizontalDrag = { _, dragAmount ->
                                scope.launch {
                                    offsetX.snapTo((offsetX.value + dragAmount).coerceIn(-200f, 200f))
                                }
                            }
                        )
                    }
                }
                .combinedClickable(onClick = onClick, onLongClick = onLongClick)
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
        Box {
            Artwork(t.uri, 52.dp, radius = 10.dp, px = 128)
            if (current) {
                Box(
                    Modifier
                        .matchParentSize()
                        .background(Color.Black.copy(0.45f), RoundedCornerShape(10.dp)),
                    Alignment.Center
                ) {
                    PlayingBars(active = playing)
                }
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = t.title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.titleMedium,
                color = if (current) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "${t.artist} · ${t.album}",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (selectMode) {
            Checkbox(checked = selected, onCheckedChange = { onClick() })
        } else {
            if (t.dateAddedMs > 0) {
                Text(
                    text = formatDate(t.dateAddedMs),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
            IconButton(onClick = onMore) {
                Icon(
                    Icons.Rounded.MoreVert,
                    "More actions for ${t.title}",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
}
