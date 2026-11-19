package com.pulse.player.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.MusicOff
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Reorder
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pulse.player.playback.Track
import com.pulse.player.ui.components.Artwork
import com.pulse.player.ui.components.LocalBottomOverlayPadding
import com.pulse.player.ui.components.PillButton
import com.pulse.player.ui.components.PulseTopBar
import com.pulse.player.ui.components.TrackRow
import com.pulse.player.ui.components.UnifiedFilterBar
import com.pulse.player.ui.viewmodel.MainViewModel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlin.random.Random

@Composable
fun DetailScreen(vm: MainViewModel, detail: Screen.Detail) {
    val allTracks by vm.tracks.collectAsStateWithLifecycle()
    val neverPlayed by vm.neverPlayed.collectAsStateWithLifecycle()
    val forgottenFavs by vm.forgottenFavorites.collectAsStateWithLifecycle()
    val longTracks by vm.longTracks.collectAsStateWithLifecycle()
    val topPlayed by vm.topPlayedTracks.collectAsStateWithLifecycle()
    val recentlyPlayed by vm.recentlyPlayedTracks.collectAsStateWithLifecycle()
    val filterState by vm.filterState.collectAsStateWithLifecycle()
    val sortField by vm.sortField.collectAsStateWithLifecycle()
    val sortAsc by vm.sortAscending.collectAsStateWithLifecycle()
    val bottomPad = LocalBottomOverlayPadding.current

    val isPlaylist = detail.kind == "playlist"
    val playlistId = remember(detail) { if (isPlaylist) detail.key.toLongOrNull() else null }
    val playlistName by remember(playlistId) {
        if (playlistId != null) vm.playlistName(playlistId) else flowOf("Playlist")
    }.collectAsStateWithLifecycle(initialValue = "Playlist")
    val playlistTracks by remember(playlistId) {
        if (playlistId != null) vm.playlistTracks(playlistId) else flowOf(emptyList())
    }.collectAsStateWithLifecycle(initialValue = emptyList())

    val playbackStatus by remember(vm) {
        vm.player.state.map { it.mediaId to it.isPlaying }.distinctUntilChanged()
    }.collectAsStateWithLifecycle(initialValue = null to false)
    val (currentMediaId, isPlaying) = playbackStatus

    var reorderMode by remember(detail) { mutableStateOf(false) }
    var renameDialogOpen by remember { mutableStateOf(false) }

    val (title, subtitle, baseList) = remember(
        detail,
        allTracks,
        neverPlayed,
        forgottenFavs,
        longTracks,
        topPlayed,
        recentlyPlayed,
        playlistName,
        playlistTracks
    ) {
        when (detail.kind) {
            "playlist" -> Triple(playlistName, "${playlistTracks.size} songs · Custom playlist", playlistTracks)
            "album" -> {
                val matches = allTracks.filter { it.album.equals(detail.key, ignoreCase = true) }
                Triple(detail.key, "${matches.size} songs · ${matches.firstOrNull()?.artist.orEmpty()}", matches)
            }
            "artist" -> {
                val matches = allTracks.filter { it.artist.equals(detail.key, ignoreCase = true) }
                Triple(detail.key, "${matches.size} songs", matches)
            }
            "folder" -> {
                val matches = allTracks.filter { it.folder.equals(detail.key, ignoreCase = true) }
                Triple(detail.key.substringAfterLast('/'), "${matches.size} songs in ${detail.key}", matches)
            }
            "system" -> {
                when (detail.key) {
                    "favorites" -> {
                        val matches = allTracks.filter { it.isFavorite }
                        Triple("My Favourites", "${matches.size} songs", matches)
                    }
                    "recent_added" -> {
                        val matches = allTracks.sortedByDescending { it.dateAddedMs }.take(50)
                        Triple("Recently Added", "${matches.size} songs", matches)
                    }
                    "top_tracks" -> Triple("Most Played", "${topPlayed.size} songs", topPlayed)
                    "recently_played" -> Triple("Recently Played", "${recentlyPlayed.size} songs", recentlyPlayed)
                    "fresh_finds" -> {
                        val matches = allTracks.sortedByDescending { it.dateAddedMs }.take(25)
                        Triple("Fresh Finds (Quick Mix)", "${matches.size} songs", matches)
                    }
                    "quick_mix" -> {
                        val shuffled = allTracks.shuffled(Random(42)).take(30)
                        Triple("Auto-DJ Quick Mix", "${shuffled.size} songs", shuffled)
                    }
                    "never_played" -> Triple("Never Played", "${neverPlayed.size} songs", neverPlayed)
                    "forgotten_favorites" -> Triple("Forgotten Favourites (30 days)", "${forgottenFavs.size} songs", forgottenFavs)
                    "long_tracks" -> Triple("Long Tracks (7 min+)", "${longTracks.size} songs", longTracks)
                    else -> Triple("Playlist", "${allTracks.size} songs", allTracks)
                }
            }
            else -> Triple("Playlist", "${allTracks.size} songs", allTracks)
        }
    }

    val neverPlayedIds = remember(neverPlayed) { neverPlayed.mapTo(HashSet()) { it.id } }
    val displayList: List<Track> = remember(baseList, filterState, sortField, sortAsc, reorderMode, isPlaylist, neverPlayedIds) {
        if (reorderMode && isPlaylist) {
            baseList
        } else {
            val filtered = if (filterState.activeCount == 0) {
                baseList
            } else {
                baseList.filter { t ->
                    (!filterState.favoritesOnly || t.isFavorite) &&
                        (!filterState.hiResOnly || vm.isHiResTrack(t)) &&
                        (!filterState.longOnly || t.durationMs >= 420_000L) &&
                        (!filterState.unplayedOnly || t.id in neverPlayedIds)
                }
            }
            if (isPlaylist) filtered else vm.sortTrackList(filtered, sortField, sortAsc)
        }
    }

    BackHandler {
        if (reorderMode) reorderMode = false else vm.back()
    }

    Column(Modifier.fillMaxSize()) {
        PulseTopBar(
            title = title,
            subtitle = if (reorderMode) "Reorder tracks" else subtitle,
            onBack = {
                if (reorderMode) reorderMode = false else vm.back()
            },
            actions = {
                if (isPlaylist && playlistId != null) {
                    IconButton(onClick = { renameDialogOpen = true }, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Rounded.Edit, contentDescription = "Rename playlist", tint = MaterialTheme.colorScheme.onSurface)
                    }
                    IconButton(onClick = { reorderMode = !reorderMode }, modifier = Modifier.size(48.dp)) {
                        Icon(
                            imageVector = if (reorderMode) Icons.Rounded.Check else Icons.Rounded.Reorder,
                            contentDescription = if (reorderMode) "Finish reordering" else "Reorder tracks",
                            tint = if (reorderMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = bottomPad)
        ) {
            if (!reorderMode) {
                item(contentType = "header") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp, bottom = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Artwork(baseList.firstOrNull()?.uri, 164.dp, radius = 20.dp, px = 512)
                        Spacer(Modifier.height(14.dp))
                        Text(
                            text = title,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 20.dp)
                        )
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 20.dp)
                        )
                        Spacer(Modifier.height(16.dp))

                        if (displayList.isNotEmpty()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                PillButton(
                                    text = "Play",
                                    icon = Icons.Rounded.PlayArrow,
                                    onClick = { vm.player.playQueue(displayList, 0) },
                                    modifier = Modifier.weight(1f)
                                )
                                PillButton(
                                    text = "Shuffle",
                                    icon = Icons.Rounded.Shuffle,
                                    onClick = {
                                        val shuffled = displayList.shuffled(Random(System.currentTimeMillis()))
                                        vm.player.playQueue(shuffled, 0)
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Spacer(Modifier.height(12.dp))
                        }

                        UnifiedFilterBar(vm = vm, isTrackTab = !isPlaylist)
                    }
                }
            } else {
                item(contentType = "reorder_banner") {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "Use the arrows to move tracks up or down, or remove items from this playlist.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }

            if (displayList.isEmpty()) {
                item(contentType = "empty") {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 36.dp, horizontal = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Rounded.MusicOff,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                            modifier = Modifier.size(44.dp)
                        )
                        Text(
                            text = if (baseList.isEmpty()) "No songs here yet" else "No tracks match filters",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = when {
                                detail.kind == "system" && (detail.key == "top_tracks" || detail.key == "recently_played") ->
                                    "Play songs in your library and your history will build here automatically."
                                baseList.isNotEmpty() ->
                                    "Try clearing your active filters to see all ${baseList.size} tracks."
                                else ->
                                    "Songs matching this collection will show up here."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else if (reorderMode && playlistId != null) {
                itemsIndexed(displayList, key = { _, it -> it.id }) { index, track ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
                            .padding(start = 10.dp, top = 6.dp, bottom = 6.dp, end = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Artwork(track.uri, 42.dp, radius = 8.dp, px = 128)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = track.title,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = track.artist,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(
                            onClick = {
                                if (index > 0) {
                                    val ids = displayList.map { it.id }.toMutableList()
                                    val moved = ids.removeAt(index)
                                    ids.add(index - 1, moved)
                                    vm.reorderPlaylistTracks(playlistId, ids)
                                }
                            },
                            enabled = index > 0,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(Icons.Rounded.ArrowUpward, "Move up")
                        }
                        IconButton(
                            onClick = {
                                if (index < displayList.lastIndex) {
                                    val ids = displayList.map { it.id }.toMutableList()
                                    val moved = ids.removeAt(index)
                                    ids.add(index + 1, moved)
                                    vm.reorderPlaylistTracks(playlistId, ids)
                                }
                            },
                            enabled = index < displayList.lastIndex,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(Icons.Rounded.ArrowDownward, "Move down")
                        }
                        IconButton(
                            onClick = {
                                vm.removeTrackFromPlaylist(playlistId, track.id)
                                vm.snackbarMessage.value = "Removed '${track.title}' from playlist"
                            },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(Icons.Rounded.Close, "Remove from playlist", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            } else {
                itemsIndexed(displayList, key = { _, it -> it.id }, contentType = { _, _ -> "track" }) { index, track ->
                    TrackRow(
                        t = track,
                        current = track.id == currentMediaId,
                        playing = isPlaying,
                        onSwipeRightQueue = {
                            vm.playNext(track)
                            vm.snackbarMessage.value = "Queued next: ${track.title}"
                        },
                        onSwipeLeftFavorite = {
                            vm.toggleFavorite(track.id)
                            vm.snackbarMessage.value = if (!track.isFavorite) "Added to favourites" else "Removed from favourites"
                        },
                        onClick = { vm.playQueue(displayList, index) },
                        onLongClick = { vm.toggleSelect(track.id) },
                        onMore = { vm.openTrackMenu(track) }
                    )
                }
            }
        }
    }

    if (renameDialogOpen && playlistId != null) {
        var newName by remember(playlistName) { mutableStateOf(playlistName) }
        AlertDialog(
            onDismissRequest = { renameDialogOpen = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("Rename Playlist") },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("Playlist name") },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        vm.renamePlaylist(playlistId, newName)
                        renameDialogOpen = false
                    },
                    enabled = newName.isNotBlank()
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { renameDialogOpen = false }) { Text("Cancel") }
            }
        )
    }
}
