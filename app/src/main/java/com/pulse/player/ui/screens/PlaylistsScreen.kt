package com.pulse.player.ui.screens

import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistPlay
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.FileOpen
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.HourglassEmpty
import androidx.compose.material.icons.rounded.LibraryAdd
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pulse.player.data.db.PlaylistEntity
import com.pulse.player.data.db.SmartPlaylistEntity
import com.pulse.player.ui.components.LocalBottomOverlayPadding
import com.pulse.player.ui.components.UnifiedFilterBar
import com.pulse.player.ui.theme.LocalDarkTheme
import com.pulse.player.ui.theme.Pulse
import com.pulse.player.ui.theme.TabularMono
import com.pulse.player.ui.viewmodel.EntitySortMode
import com.pulse.player.ui.viewmodel.MainViewModel

@Composable
fun PlaylistsScreen(vm: MainViewModel) {
    val playlists by vm.playlists.collectAsStateWithLifecycle()
    val customSmartPlaylists by vm.customSmartPlaylists.collectAsStateWithLifecycle()
    val genres by vm.genres.collectAsStateWithLifecycle()
    val decades by vm.decades.collectAsStateWithLifecycle()
    val entitySort by vm.entitySortMode.collectAsStateWithLifecycle()
    val favCount by vm.favoriteCount.collectAsStateWithLifecycle()
    val tracks by vm.tracks.collectAsStateWithLifecycle()
    val neverPlayed by vm.neverPlayed.collectAsStateWithLifecycle()
    val forgottenFavs by vm.forgottenFavorites.collectAsStateWithLifecycle()
    val longTracks by vm.longTracks.collectAsStateWithLifecycle()
    val bottomPad = LocalBottomOverlayPadding.current

    val ctx = LocalContext.current
    var pendingDelete by remember { mutableStateOf<PlaylistEntity?>(null) }

    val importM3uLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val resolvedName = runCatching {
                ctx.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
                    if (c.moveToFirst()) c.getString(0) else null
                }
            }.getOrNull() ?: uri.lastPathSegment?.substringAfterLast('/') ?: "Imported Playlist"
            vm.importM3uPlaylist(uri, resolvedName)
        }
    }

    val sortedPlaylists = remember(playlists, entitySort) {
        when (entitySort) {
            EntitySortMode.NAME_ASC -> playlists.sortedBy { it.name.lowercase() }
            EntitySortMode.NAME_DESC -> playlists.sortedByDescending { it.name.lowercase() }
            EntitySortMode.COUNT_DESC -> playlists.sortedByDescending { it.createdAtMs }
        }
    }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            state = vm.lazyListStateFor("playlists"),
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

            if (genres.isNotEmpty() || decades.isNotEmpty()) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Browse by Genre & Decade",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        if (genres.isNotEmpty()) {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(genres, key = { it.genre }) { g ->
                                    AssistChip(
                                        onClick = { vm.go(Screen.Detail("genre", g.genre)) },
                                        label = { Text("${g.genre} · ${g.trackCount}", maxLines = 1) },
                                        colors = AssistChipDefaults.assistChipColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                            labelColor = MaterialTheme.colorScheme.onSurface
                                        ),
                                        modifier = Modifier.testTag("genre_chip_${g.genre}")
                                    )
                                }
                            }
                        }
                        if (decades.isNotEmpty()) {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(decades, key = { it.decade }) { d ->
                                    AssistChip(
                                        onClick = { vm.go(Screen.Detail("decade", d.decade.toString())) },
                                        label = { Text("${d.label} · ${d.trackCount}", maxLines = 1) },
                                        colors = AssistChipDefaults.assistChipColors(
                                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
                                            labelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                        ),
                                        modifier = Modifier.testTag("decade_chip_${d.decade}")
                                    )
                                }
                            }
                        }
                    }
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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Smart Playlists & Auto-DJ", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                        OutlinedButton(
                            onClick = { vm.openSmartPlaylistBuilder() },
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag("create_smart_playlist_button"),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Smart Rule", fontWeight = FontWeight.SemiBold)
                        }
                    }
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

                    customSmartPlaylists.forEach { sp ->
                        SmartRulePlaylistCard(
                            playlist = sp,
                            onOpen = { vm.go(Screen.Detail("smart_custom", sp.id.toString())) },
                            onEdit = { vm.openSmartPlaylistBuilder(sp) }
                        )
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "My Playlists (${sortedPlaylists.size})",
                        style = MaterialTheme.typography.titleMedium.merge(TabularMono),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    OutlinedButton(
                        onClick = {
                            importM3uLauncher.launch(
                                arrayOf(
                                    "audio/x-mpegurl",
                                    "application/vnd.apple.mpegurl",
                                    "audio/mpegurl",
                                    "text/plain",
                                    "*/*"
                                )
                            )
                        },
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("import_m3u_button"),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.FileOpen,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("Import .m3u8", fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            if (sortedPlaylists.isEmpty()) {
                item {
                    Surface(
                        onClick = { vm.createPlaylistOpen.value = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .testTag("empty_create_playlist_card"),
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f))
                    ) {
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 28.dp, horizontal = 20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                "No custom playlists yet",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                "Tap here or press + to organize your favourite mixes.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(sortedPlaylists, key = { it.id }) { playlist ->
                    Surface(
                        onClick = { vm.go(Screen.Detail("playlist", playlist.id.toString())) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 14.dp, end = 6.dp, top = 12.dp, bottom = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.AutoMirrored.Rounded.PlaylistPlay, null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(26.dp))
                            }
                            Spacer(Modifier.width(14.dp))
                            Column(Modifier.weight(1f)) {
                                Text(playlist.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                                Text("Tap to open & reorder", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            IconButton(onClick = { pendingDelete = playlist }, modifier = Modifier.size(48.dp)) {
                                Icon(Icons.Rounded.Delete, "Delete playlist", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { vm.createPlaylistOpen.value = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = bottomPad, end = 16.dp)
                .testTag("new_playlist_fab"),
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

@Composable
private fun SmartRulePlaylistCard(
    playlist: SmartPlaylistEntity,
    onOpen: () -> Unit,
    onEdit: () -> Unit
) {
    Surface(
        onClick = onOpen,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("smart_playlist_card_${playlist.id}"),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 14.dp, end = 6.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Rounded.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = playlist.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = playlist.limitCount?.let { "Custom Smart Rule · Max $it tracks" } ?: "Custom Smart Rule",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
            IconButton(
                onClick = onEdit,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("edit_smart_playlist_${playlist.id}")
            ) {
                Icon(
                    Icons.Rounded.Edit,
                    contentDescription = "Edit smart playlist rules",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
