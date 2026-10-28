package com.pulse.player.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.LibraryAdd
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.PlaylistPlay
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pulse.player.ui.theme.Pulse
import com.pulse.player.ui.viewmodel.MainViewModel

@Composable
fun PlaylistsScreen(vm: MainViewModel) {
    val playlists by vm.playlists.collectAsStateWithLifecycle()
    val favCount by vm.favoriteCount.collectAsStateWithLifecycle()
    val tracks by vm.tracks.collectAsStateWithLifecycle()

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                Text("System Playlists", style = MaterialTheme.typography.titleLarge, color = Color.White)
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        SystemPlaylistCard(
                            title = "My favourite",
                            subtitle = "$favCount songs",
                            icon = Icons.Rounded.Favorite,
                            gradient = listOf(Color(0xFF0F4C75), Pulse.Cyan),
                            modifier = Modifier.weight(1f)
                        ) {
                            vm.go(Screen.Detail("system", "favorites"))
                        }
                        SystemPlaylistCard(
                            title = "Recently added",
                            subtitle = "${tracks.size.coerceAtMost(30)} songs",
                            icon = Icons.Rounded.LibraryAdd,
                            gradient = listOf(Color(0xFF0B3D2E), Pulse.Mint),
                            modifier = Modifier.weight(1f)
                        ) {
                            vm.go(Screen.Detail("system", "recent_added"))
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        SystemPlaylistCard(
                            title = "Recently played",
                            subtitle = "History",
                            icon = Icons.Rounded.History,
                            gradient = listOf(Color(0xFF3A1C71), Pulse.Violet),
                            modifier = Modifier.weight(1f)
                        ) {
                            vm.go(Screen.Detail("system", "recently_played"))
                        }
                        SystemPlaylistCard(
                            title = "My top tracks",
                            subtitle = "Most played",
                            icon = Icons.Rounded.LocalFireDepartment,
                            gradient = listOf(Color(0xFFFF5722), Pulse.Coral),
                            modifier = Modifier.weight(1f)
                        ) {
                            vm.go(Screen.Detail("system", "top_tracks"))
                        }
                    }
                }
            }

            item {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("My Playlists (${playlists.size})", style = MaterialTheme.typography.titleLarge, color = Color.White)
                }
            }

            if (playlists.isEmpty()) {
                item {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No custom playlists yet. Tap + to create one.", color = Pulse.TextMid)
                    }
                }
            } else {
                items(playlists, key = { it.id }) { playlist ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Pulse.Surface)
                            .clickable { vm.go(Screen.Detail("playlist", playlist.id.toString())) }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Pulse.Elevated),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.PlaylistPlay, null, tint = Pulse.Cyan, modifier = Modifier.size(28.dp))
                        }
                        Spacer(Modifier.width(16.dp))
                        Column(Modifier.weight(1f)) {
                            Text(playlist.name, style = MaterialTheme.typography.titleMedium, color = Color.White)
                            Text("Playlist", style = MaterialTheme.typography.bodySmall, color = Pulse.TextMid)
                        }
                        IconButton(onClick = { vm.deletePlaylist(playlist.id) }) {
                            Icon(Icons.Rounded.Delete, "Delete playlist", tint = Pulse.TextLow)
                        }
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { vm.createPlaylistOpen.value = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 76.dp, end = 20.dp),
            containerColor = Pulse.Cyan,
            contentColor = Color.Black
        ) {
            Icon(Icons.Rounded.Add, "New playlist")
        }
    }
}

@Composable
fun SystemPlaylistCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    gradient: List<Color>,
    modifier: Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(110.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.linearGradient(gradient))
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(28.dp)
        )
        Column(Modifier.align(Alignment.BottomStart)) {
            Text(title, fontWeight = FontWeight.Bold, color = Color.White, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(0.85f))
        }
    }
}
