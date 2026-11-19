package com.pulse.player.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.HourglassEmpty
import androidx.compose.material.icons.rounded.LibraryAdd
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.PlaylistPlay
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pulse.player.data.db.PlaylistEntity
import com.pulse.player.ui.components.LocalBottomOverlayPadding
import com.pulse.player.ui.components.UnifiedFilterBar
import com.pulse.player.ui.theme.LocalDarkTheme
import com.pulse.player.ui.theme.Pulse
import com.pulse.player.ui.viewmodel.EntitySortMode
import com.pulse.player.ui.viewmodel.MainViewModel

@Composable
fun PlaylistsScreen(vm: MainViewModel) {
    val playlists by vm.playlists.collectAsStateWithLifecycle()
    val entitySort by vm.entitySortMode.collectAsStateWithLifecycle()
    val favCount by vm.favoriteCount.collectAsStateWithLifecycle()
    val tracks by vm.tracks.collectAsStateWithLifecycle()
    val neverPlayed by vm.neverPlayed.collectAsStateWithLifecycle()
    val forgottenFavs by vm.forgottenFavorites.collectAsStateWithLifecycle()
    val longTracks by vm.longTracks.collectAsStateWithLifecycle()
    val bottomPad = LocalBottomOverlayPadding.current

    var pendingDelete by remember { mutableStateOf<PlaylistEntity?>(null) }

    val sortedPlaylists = remember(playlists, entitySort) {
        when (entitySort) {
            EntitySortMode.NAME_ASC -> playlists.sortedBy { it.name.lowercase() }
            EntitySortMode.NAME_DESC -> playlists.sortedByDescending { it.name.lowercase() }
            EntitySortMode.COUNT_DESC -> playlists.sortedByDescending { it.createdAtMs }
        }
    }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 12.dp, bottom = bottomPad + 72.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Playlists",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    UnifiedFilterBar(vm = vm, isTrackTab = false)
                }
            }

            item {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("System Playlists", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
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
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Smart Playlists & Auto-DJ", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        SystemPlaylistCard(
                            title = "Fresh Finds",
                            subtitle = "Quick Mix",
                            icon = Icons.Rounded.LibraryAdd,
                            gradient = listOf(Color(0xFF004D40), Color(0xFF00BFA5)),
                            modifier = Modifier.weight(1f)
                        ) {
                            vm.go(Screen.Detail("system", "fresh_finds"))
                        }
                        SystemPlaylistCard(
                            title = "Auto-DJ Mix",
                            subtitle = "Smart Discovery",
                            icon = Icons.Rounded.LocalFireDepartment,
                            gradient = listOf(Color(0xFF4A148C), Color(0xFFAB47BC)),
                            modifier = Modifier.weight(1f)
                        ) {
                            vm.go(Screen.Detail("system", "quick_mix"))
                        }
                    }

                    if (neverPlayed.isNotEmpty()) {
                        SystemPlaylistCard(
                            title = "Never played",
                            subtitle = "${neverPlayed.size} songs",
                            icon = Icons.Rounded.HourglassEmpty,
                            gradient = listOf(Color(0xFF1B1B4B), Pulse.Cyan),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            vm.go(Screen.Detail("system", "never_played"))
                        }
                    }
                    if (forgottenFavs.isNotEmpty()) {
                        SystemPlaylistCard(
                            title = "Forgotten favourites (30 days)",
                            subtitle = "${forgottenFavs.size} songs",
                            icon = Icons.Rounded.FavoriteBorder,
                            gradient = listOf(Color(0xFF5B1A58), Pulse.Pink),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            vm.go(Screen.Detail("system", "forgotten_favorites"))
                        }
                    }
                    if (longTracks.isNotEmpty()) {
                        SystemPlaylistCard(
                            title = "Long tracks (7 min+)",
                            subtitle = "${longTracks.size} songs",
                            icon = Icons.Rounded.Timer,
                            gradient = listOf(Color(0xFF2B1055), Pulse.Purple),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            vm.go(Screen.Detail("system", "long_tracks"))
                        }
                    }
                }
            }

            item {
                Text(
                    text = "My Playlists (${sortedPlaylists.size})",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            if (sortedPlaylists.isEmpty()) {
                item {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f))
                            .clickable { vm.createPlaylistOpen.value = true }
                            .padding(vertical = 28.dp, horizontal = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            "No custom playlists yet",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            "Tap here or press + to organize your favourite mixes.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(sortedPlaylists, key = { it.id }) { playlist ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { vm.go(Screen.Detail("playlist", playlist.id.toString())) }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.surface),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.PlaylistPlay, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                        }
                        Spacer(Modifier.width(16.dp))
                        Column(Modifier.weight(1f)) {
                            Text(playlist.name, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                            Text("Tap to open & reorder", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(onClick = { pendingDelete = playlist }, modifier = Modifier.size(48.dp)) {
                            Icon(Icons.Rounded.Delete, "Delete playlist", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { vm.createPlaylistOpen.value = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = bottomPad, end = 16.dp),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ) {
            Icon(Icons.Rounded.Add, "New playlist")
        }
    }

    pendingDelete?.let { target ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            title = { Text("Delete \"${target.name}\"?") },
            text = { Text("This removes the playlist entry. Your audio files on storage stay untouched.") },
            confirmButton = {
                Button(
                    onClick = {
                        vm.deletePlaylist(target.id)
                        vm.snackbarMessage.value = "Deleted playlist '${target.name}'"
                        pendingDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("Cancel") }
            }
        )
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
    val isDark = LocalDarkTheme.current
    val baseAccent = gradient.lastOrNull() ?: MaterialTheme.colorScheme.primary
    val accent = if (isDark) baseAccent else lerp(baseAccent, MaterialTheme.colorScheme.onSurface, 0.22f)

    Box(
        modifier = modifier
            .height(106.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .border(
                BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = if (isDark) 0.7f else 0.85f)
                ),
                RoundedCornerShape(20.dp)
            )
            .clickable(onClick = onClick)
            .padding(14.dp)
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(34.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(accent.copy(alpha = if (isDark) 0.18f else 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(18.dp)
            )
        }
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(end = 36.dp)
        ) {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}
