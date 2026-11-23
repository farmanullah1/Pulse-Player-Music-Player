package com.pulse.player.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.SelectAll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pulse.player.ui.theme.TabularMono

@Composable
fun BulkSelectionBar(
    selectedCount: Int,
    onSelectAll: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onBatchEditTags: () -> Unit,
    onPlayNext: () -> Unit,
    onFavorite: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hasSelection = selectedCount > 0
    val activeTint = MaterialTheme.colorScheme.primary
    val disabledTint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)),
        tonalElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$selectedCount selected",
                style = MaterialTheme.typography.titleSmall.merge(TabularMono),
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 4.dp, end = 6.dp)
            )
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onSelectAll,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("bulk_select_all")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.SelectAll,
                        contentDescription = "Select all",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                IconButton(
                    onClick = onAddToPlaylist,
                    enabled = hasSelection,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("bulk_add_playlist")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.PlaylistAdd,
                        contentDescription = "Add selected to playlist",
                        tint = if (hasSelection) activeTint else disabledTint
                    )
                }
                IconButton(
                    onClick = onBatchEditTags,
                    enabled = hasSelection,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("bulk_edit_tags")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Edit,
                        contentDescription = "Batch edit tags",
                        tint = if (hasSelection) activeTint else disabledTint
                    )
                }
                IconButton(
                    onClick = onPlayNext,
                    enabled = hasSelection,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("bulk_play_next")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.QueueMusic,
                        contentDescription = "Play selected next",
                        tint = if (hasSelection) activeTint else disabledTint
                    )
                }
                IconButton(
                    onClick = onFavorite,
                    enabled = hasSelection,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("bulk_favorite")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Favorite,
                        contentDescription = "Favourite selected",
                        tint = if (hasSelection) activeTint else disabledTint
                    )
                }
                IconButton(
                    onClick = onClose,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("bulk_close")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Exit selection mode",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
