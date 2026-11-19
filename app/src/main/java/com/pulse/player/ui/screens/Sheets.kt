package com.pulse.player.ui.screens

import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pulse.player.data.ErrorCategory
import com.pulse.player.data.ErrorDispatcher
import com.pulse.player.data.backup.classic.FavoritesRepository
import com.pulse.player.data.backup.classic.MatchResult
import com.pulse.player.playback.RingtoneHelper
import com.pulse.player.playback.Track
import com.pulse.player.ui.components.Artwork
import com.pulse.player.ui.components.PlayingBars
import com.pulse.player.ui.components.formatTime
import com.pulse.player.ui.theme.TabularMono
import com.pulse.player.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueueSheet(vm: MainViewModel) {
    val queue by vm.player.queue.collectAsStateWithLifecycle()
    val ps by vm.player.state.collectAsStateWithLifecycle()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var confirmClear by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = { vm.closeQueue() },
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Play Queue (${queue.size})",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (queue.isNotEmpty()) {
                        Text(
                            text = "Tap to jump or reorder up/down",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (queue.isNotEmpty()) {
                    if (confirmClear) {
                        Button(
                            onClick = {
                                vm.player.clearQueue()
                                confirmClear = false
                                vm.snackbarMessage.value = "Queue cleared"
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                contentColor = MaterialTheme.colorScheme.onErrorContainer
                            ),
                            shape = CircleShape
                        ) {
                            Text("Confirm Clear", fontWeight = FontWeight.SemiBold)
                        }
                    } else {
                        TextButton(onClick = { confirmClear = true }) {
                            Text("Clear", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            if (queue.isEmpty()) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically)
                ) {
                    Icon(
                        Icons.Rounded.QueueMusic,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(40.dp)
                    )
                    Text("Queue is empty", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                    Text("Tap Play or Queue Next on any song.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    Modifier
                        .fillMaxWidth()
                        .height(400.dp)
                ) {
                    itemsIndexed(queue) { index, track ->
                        val isCurrent = index == ps.index
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp)
                                .background(
                                    if (isCurrent) MaterialTheme.colorScheme.surfaceContainerHigh else Color.Transparent,
                                    RoundedCornerShape(12.dp)
                                )
                                .then(
                                    if (isCurrent) Modifier.border(
                                        BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                                        RoundedCornerShape(12.dp)
                                    ) else Modifier
                                )
                                .clickable { vm.player.skipToQueueIndex(index) }
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box {
                                Artwork(track.uri, 44.dp, radius = 8.dp, px = 128)
                                if (isCurrent) {
                                    Box(
                                        Modifier
                                            .matchParentSize()
                                            .background(Color.Black.copy(0.45f), RoundedCornerShape(8.dp)),
                                        Alignment.Center
                                    ) {
                                        PlayingBars(active = ps.isPlaying)
                                    }
                                }
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    track.title,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    track.artist,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (index > 0) {
                                IconButton(
                                    onClick = { vm.player.moveInQueue(index, index - 1) },
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(
                                        Icons.Rounded.ArrowUpward,
                                        contentDescription = "Move up",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            if (index < queue.lastIndex) {
                                IconButton(
                                    onClick = { vm.player.moveInQueue(index, index + 1) },
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(
                                        Icons.Rounded.ArrowDownward,
                                        contentDescription = "Move down",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            IconButton(
                                onClick = { vm.player.removeFromQueue(index) },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(Icons.Rounded.Close, "Remove from queue", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SleepSheet(vm: MainViewModel) {
    val ps by vm.player.state.collectAsStateWithLifecycle()
    val sleepFadeSec by vm.sleepFadeSec.collectAsStateWithLifecycle()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var customMin by remember { mutableFloatStateOf(30f) }
    val chipColors = FilterChipDefaults.filterChipColors(
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        labelColor = MaterialTheme.colorScheme.onSurface,
        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
    )

    ModalBottomSheet(
        onDismissRequest = { vm.closeSleepSheet() },
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Sleep Timer", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
            if (ps.sleepRemainingMs > 0) {
                Text(
                    text = "Timer active: ${formatTime(ps.sleepRemainingMs)} remaining",
                    style = MaterialTheme.typography.bodyMedium.merge(TabularMono),
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            } else if (ps.sleepEndOfTrack) {
                Text("Will stop at the end of this track", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(15, 30, 45, 60).forEach { min ->
                    FilterChip(
                        selected = false,
                        onClick = {
                            vm.player.startSleep(min)
                            vm.closeSleepSheet()
                        },
                        label = { Text("${min}m") },
                        colors = chipColors
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Fade-out duration",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(0 to "Off", 5 to "5s", 10 to "10s", 20 to "20s").forEach { (sec, label) ->
                        FilterChip(
                            selected = sleepFadeSec == sec,
                            onClick = { vm.setSleepFadeSec(sec) },
                            label = { Text(label) },
                            colors = chipColors
                        )
                    }
                }
            }

            Column {
                Text("Custom: ${customMin.toInt()} minutes", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Slider(
                    value = customMin,
                    onValueChange = { customMin = it },
                    valueRange = 5f..120f,
                    steps = 22
                )
                Button(
                    onClick = {
                        vm.player.startSleep(customMin.toInt())
                        vm.closeSleepSheet()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary)
                ) {
                    Text("Start Timer (${customMin.toInt()} min)", fontWeight = FontWeight.Bold)
                }
            }

            Button(
                onClick = {
                    vm.player.sleepAtEndOfTrack()
                    vm.closeSleepSheet()
                },
                modifier = Modifier.fillMaxWidth(),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
            ) {
                Text("Stop after this track")
            }

            if (ps.sleepRemainingMs > 0 || ps.sleepEndOfTrack) {
                Button(
                    onClick = {
                        vm.player.cancelSleep()
                        vm.closeSleepSheet()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    )
                ) {
                    Text("Cancel Timer")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddToPlaylistSheet(vm: MainViewModel, track: Track) {
    val playlists by vm.playlists.collectAsStateWithLifecycle()
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = { vm.closeAddToPlaylist() },
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Add to Playlist", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
                TextButton(onClick = {
                    vm.closeAddToPlaylist()
                    vm.createPlaylistOpen.value = true
                }) {
                    Text("+ New", color = MaterialTheme.colorScheme.primary)
                }
            }
            Spacer(Modifier.height(12.dp))
            if (playlists.isEmpty()) {
                Text("No playlists yet. Create one to start organizing.", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 16.dp))
            } else {
                LazyColumn {
                    items(playlists.size) { i ->
                        val p = playlists[i]
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable {
                                    vm.addTrackToPlaylist(p.id, track.id)
                                    vm.snackbarMessage.value = "Added to ${p.name}"
                                    vm.closeAddToPlaylist()
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Rounded.PlaylistAdd, null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(16.dp))
                            Text(p.name, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackMenuSheet(vm: MainViewModel, track: Track) {
    val ctx = LocalContext.current
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = { vm.closeTrackMenu() },
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Artwork(track.uri, 48.dp, radius = 10.dp, px = 128)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(track.title, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Text("${track.artist} · ${track.album}", maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            val items = listOf(
                Triple("Play next", Icons.Rounded.QueueMusic) {
                    vm.playNext(track)
                    vm.snackbarMessage.value = "Playing next: ${track.title}"
                },
                Triple("Add to queue", Icons.Rounded.QueueMusic) {
                    vm.addToQueue(track)
                    vm.snackbarMessage.value = "Added to queue: ${track.title}"
                },
                Triple("Add to playlist", Icons.Rounded.PlaylistAdd) {
                    vm.openAddToPlaylist(track)
                },
                Triple(if (track.isFavorite) "Remove from favourites" else "Add to favourites", if (track.isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder) {
                    vm.toggleFavorite(track.id)
                },
                Triple("Song details", Icons.Rounded.Info) {
                    vm.songDetailsTrack.value = track
                },
                Triple("Edit tags & cover art", Icons.Rounded.Edit) {
                    vm.openTagEditor(track)
                },
                Triple("Set as ringtone", Icons.Rounded.Notifications) {
                    runCatching {
                        RingtoneHelper.setRingtone(ctx, Uri.parse(track.uri), RingtoneManager.TYPE_RINGTONE)
                    }.onSuccess { ok ->
                        if (ok) vm.snackbarMessage.value = "Ringtone updated"
                        else vm.snackbarMessage.value = "Could not set ringtone for this file"
                    }.onFailure { err ->
                        ErrorDispatcher.post(ErrorCategory.STORAGE, "Ringtone Error", err.message ?: "Failed to set ringtone", err.stackTraceToString())
                        vm.snackbarMessage.value = "Could not set ringtone"
                    }
                },
                Triple("Share file", Icons.Rounded.Share) {
                    runCatching {
                        val share = Intent(Intent.ACTION_SEND).apply {
                            type = "audio/*"
                            putExtra(Intent.EXTRA_STREAM, Uri.parse(track.uri))
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        ctx.startActivity(Intent.createChooser(share, "Share ${track.title}"))
                    }.onFailure { err ->
                        ErrorDispatcher.post(ErrorCategory.STORAGE, "Share Failed", err.message ?: "Unable to share track", err.stackTraceToString())
                        vm.snackbarMessage.value = "No app available to share this audio file"
                    }
                }
            )

            items.forEach { (label, icon, action) ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable {
                            action()
                            vm.closeTrackMenu()
                        }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(icon, null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(16.dp))
                    Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }
    }
}

@Composable
fun CreatePlaylistDialog(vm: MainViewModel) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = { vm.createPlaylistOpen.value = false },
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        title = { Text("New Playlist", color = MaterialTheme.colorScheme.onSurface) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Playlist name") },
                singleLine = true
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    vm.createPlaylist(name)
                    vm.createPlaylistOpen.value = false
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary)
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = { vm.createPlaylistOpen.value = false }) {
                Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    )
}

@Composable
fun ImportPreviewDialog(vm: MainViewModel, preview: FavoritesRepository.Preview) {
    var replace by remember { mutableStateOf(false) }
    val confidentCount = preview.results.count { it is MatchResult.Matched }
    val alreadyCount = preview.results.count { it is MatchResult.AlreadyFavorite }
    val missingCount = preview.results.count { it is MatchResult.Missing }

    AlertDialog(
        onDismissRequest = { vm.importPreview.value = null },
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        title = { Text("Import Favourites", color = MaterialTheme.colorScheme.onSurface) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("File: ${preview.fileName}", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                Text("Total items in file: ${preview.results.size}", color = MaterialTheme.colorScheme.onSurface)
                Text("• Ready to match: $confidentCount", color = MaterialTheme.colorScheme.primary)
                if (alreadyCount > 0) Text("• Already favourite: $alreadyCount", color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (missingCount > 0) Text("• Not found on device: $missingCount", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f))

                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    androidx.compose.material3.Checkbox(checked = replace, onCheckedChange = { replace = it })
                    Text("Replace existing favourites", color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { vm.applyImport(preview, emptyMap(), replace) },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary)
            ) {
                Text("Apply Import")
            }
        },
        dismissButton = {
            TextButton(onClick = { vm.importPreview.value = null }) {
                Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    )
}

@Composable
fun SongDetailsDialog(track: Track, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        title = { Text("Song Details", color = MaterialTheme.colorScheme.onSurface) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Title: ${track.title}", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                Text("Artist: ${track.artist}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Album: ${track.album}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (track.durationMs > 0) {
                    Text(
                        "Duration: ${formatTime(track.durationMs)}",
                        style = MaterialTheme.typography.bodyMedium.merge(TabularMono),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (track.folder.isNotBlank()) Text("Folder: ${track.folder}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (track.mimeType != null) Text("Format: ${track.mimeType}", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) { Text("Close") }
        }
    )
}
