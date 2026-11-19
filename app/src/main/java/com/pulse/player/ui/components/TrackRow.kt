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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
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
import com.pulse.player.ui.theme.LocalListDensity
import com.pulse.player.ui.theme.TabularMono
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
    val swipeDirection by remember {
        derivedStateOf {
            val v = offsetX.value
            when {
                v > 20f -> 1
                v < -20f -> -1
                else -> 0
            }
        }
    }
    val density = LocalListDensity.current
    val vPad = when (density) {
        0 -> 5.dp
        2 -> 12.dp
        else -> 8.dp
    }
    val artSize = when (density) {
        0 -> 44.dp
        2 -> 58.dp
        else -> 52.dp
    }
    val subtitle = remember(t.artist, t.album) { "${t.artist} · ${t.album}" }
    val formattedDate = remember(t.dateAddedMs) { if (t.dateAddedMs > 0) formatDate(t.dateAddedMs) else null }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else Color.Transparent)
    ) {
        // Swipe background hints
        if (!selectMode && swipeDirection != 0 && (onSwipeRightQueue != null || onSwipeLeftFavorite != null)) {
            if (swipeDirection > 0) {
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
            } else if (swipeDirection < 0) {
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
                .background(
                    if (swipeDirection != 0) MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)
                    else Color.Transparent
                )
                .padding(horizontal = 16.dp, vertical = vPad),
            verticalAlignment = Alignment.CenterVertically
        ) {
        Box {
            Artwork(t.uri, artSize, radius = 10.dp, px = 128)
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
                text = subtitle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (selectMode) {
            Checkbox(checked = selected, onCheckedChange = { onClick() })
        } else {
            if (formattedDate != null) {
                Text(
                    text = formattedDate,
                    style = MaterialTheme.typography.bodySmall.merge(TabularMono),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f)
                )
            }
            IconButton(
                onClick = onMore,
                modifier = Modifier.size(48.dp)
            ) {
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
