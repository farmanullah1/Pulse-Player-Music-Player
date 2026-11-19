package com.pulse.player.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pulse.player.ui.components.Artwork
import com.pulse.player.ui.components.LocalBottomOverlayPadding
import com.pulse.player.ui.components.UnifiedFilterBar
import com.pulse.player.ui.theme.LocalAlbumGridColumns
import com.pulse.player.ui.viewmodel.MainViewModel

@Immutable
data class AlbumItem(val name: String, val artist: String, val artUri: String?, val count: Int)

@Composable
fun AlbumsScreen(vm: MainViewModel) {
    val albums by vm.albums.collectAsStateWithLifecycle()
    val preferredCols = LocalAlbumGridColumns.current
    val bottomPad = LocalBottomOverlayPadding.current

    Column(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "${albums.size} Albums",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            UnifiedFilterBar(vm = vm, isTrackTab = false)
        }

        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
            val effectiveCols = if (maxWidth >= 600.dp) (preferredCols + 2).coerceAtMost(6) else preferredCols.coerceIn(2, 4)

            LazyVerticalGrid(
                columns = GridCells.Fixed(effectiveCols),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = bottomPad),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (albums.isEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }, contentType = "empty") {
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 48.dp, horizontal = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                Icons.Rounded.Album,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                                modifier = Modifier.size(48.dp)
                            )
                            Text(
                                "No albums match",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                "Adjust your active filters or scan your library from For You.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    items(albums, key = { it.name }, contentType = { "album" }) { album ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { vm.go(Screen.Detail("album", album.name)) }
                        ) {
                            Artwork(
                                uri = album.artUri,
                                size = 150.dp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1f),
                                radius = 16.dp,
                                px = 384
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = album.name,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${album.artist} · ${album.count} songs",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
