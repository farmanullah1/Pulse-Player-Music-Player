package com.pulse.player.ui.theme.immersive

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pulse.player.ui.components.Artwork
import com.pulse.player.ui.theme.Pulse
import kotlin.math.sin

data class QueueItem(
    val mediaId: String,
    val title: String,
    val artist: String,
    val artUri: String?,
    val durationMs: Long
)

@Composable
fun EqualizerBars(color: Color, modifier: Modifier = Modifier) {
    val reduced = rememberReducedMotion()
    val transition = rememberInfiniteTransition(label = "eq_bars")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "eq_phase"
    )

    Canvas(modifier = modifier) {
        val barCount = 3
        val spacing = 2.dp.toPx()
        val totalSpacing = (barCount - 1) * spacing
        val barWidth = ((size.width - totalSpacing) / barCount).coerceAtLeast(2f)

        for (i in 0 until barCount) {
            val fraction = if (reduced) {
                when (i) {
                    0 -> 0.4f
                    1 -> 0.9f
                    else -> 0.6f
                }
            } else {
                (sin(phase + i * 1.2f) + 1f) / 2f
            }
            val barHeight = (size.height * (0.25f + fraction * 0.75f)).coerceAtLeast(3f)
            val x = i * (barWidth + spacing)
            val y = size.height - barHeight

            drawRoundRect(
                color = color,
                topLeft = Offset(x, y),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
            )
        }
    }
}

@Composable
fun QueueTrackRow(
    t: QueueItem,
    current: Boolean,
    playing: Boolean,
    onClick: () -> Unit,
    onRemove: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .clickable(onClick = onClick)
            .background(if (current) Pulse.Orange.copy(0.14f) else Color.Transparent)
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .semantics(mergeDescendants = true) {
                if (current) stateDescription = "Now playing"
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Artwork(uri = t.artUri, size = 48.dp, radius = 8.dp, px = 96)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = t.title,
                color = if (current) Pulse.Orange else Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontWeight = if (current) FontWeight.SemiBold else FontWeight.Normal
            )
            Text(
                text = t.artist,
                color = Color.White.copy(0.65f),
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (current && playing) {
            EqualizerBars(Pulse.Orange, Modifier.size(20.dp))
        } else {
            Text(
                text = fmt(t.durationMs),
                color = Color.White.copy(0.6f),
                style = MaterialTheme.typography.labelSmall
            )
        }
        if (onRemove != null && !current) {
            IconButton(onClick = onRemove, modifier = Modifier.size(40.dp)) {
                Icon(
                    PulseIcons.Close,
                    contentDescription = "Remove ${t.title} from queue",
                    tint = Color.White.copy(0.6f)
                )
            }
        }
    }
}

@Composable
fun QueuePanel(
    queue: List<QueueItem>,
    currentIndex: Int,
    playing: Boolean,
    expanded: Boolean,
    onToggle: () -> Unit,
    onPlayAt: (Int) -> Unit,
    onRemove: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    LaunchedEffect(currentIndex, expanded) {
        if (currentIndex in queue.indices) {
            listState.animateScrollToItem((currentIndex - 1).coerceAtLeast(0))
        }
    }

    Surface(
        modifier = modifier,
        color = Color(0xE61B1B22),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggle)
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Queue · ${queue.size} songs",
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = if (expanded) PulseIcons.ChevronDown else PulseIcons.ChevronUp,
                    contentDescription = if (expanded) "Collapse queue" else "Expand queue",
                    tint = Color.White
                )
            }
            if (queue.isEmpty()) {
                Text(
                    text = "The queue is empty",
                    modifier = Modifier.padding(24.dp),
                    color = Color.White.copy(0.6f)
                )
            } else {
                LazyColumn(state = listState) {
                    itemsIndexed(queue, key = { _, q -> q.mediaId }) { i, q ->
                        QueueTrackRow(
                            t = q,
                            current = i == currentIndex,
                            playing = playing,
                            onClick = { onPlayAt(i) },
                            onRemove = { onRemove(i) }
                        )
                    }
                }
            }
        }
    }
}

