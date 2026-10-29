package com.pulse.player.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pulse.player.playback.Track
import com.pulse.player.ui.theme.Pulse

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TrackRow(
    t: Track,
    current: Boolean,
    playing: Boolean,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    selectMode: Boolean = false,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onMore: () -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .background(if (selected) Color.White.copy(0.08f) else Color.Transparent)
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
                color = if (current) Pulse.Cyan else MaterialTheme.colorScheme.onSurface
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
                    color = Pulse.TextLow
                )
            }
            IconButton(onClick = onMore) {
                Icon(Icons.Rounded.MoreVert, "More actions for ${t.title}")
            }
        }
    }
}
