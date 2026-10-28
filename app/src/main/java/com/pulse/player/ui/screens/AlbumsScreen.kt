package com.pulse.player.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pulse.player.ui.components.Artwork
import com.pulse.player.ui.theme.Pulse
import com.pulse.player.ui.viewmodel.MainViewModel

data class AlbumItem(val name: String, val artist: String, val artUri: String?, val count: Int)

@Composable
fun AlbumsScreen(vm: MainViewModel) {
    val tracks by vm.tracks.collectAsStateWithLifecycle()
    val albums = remember(tracks) {
        tracks.groupBy { it.album.ifBlank { "Unknown Album" } }
            .map { (name, list) ->
                val firstWithArt = list.firstOrNull { it.uri.isNotBlank() }
                AlbumItem(name, list.first().artist, firstWithArt?.uri, list.size)
            }
            .sortedBy { it.name.lowercase() }
    }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(150.dp),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 120.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Text("${albums.size} Albums", style = MaterialTheme.typography.titleLarge, color = Color.White)
        }

        if (albums.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No albums found", color = Pulse.TextMid)
                }
            }
        } else {
            items(albums, key = { it.name }) { album ->
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
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "${album.artist} · ${album.count} songs",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodySmall,
                        color = Pulse.TextMid
                    )
                }
            }
        }
    }
}
