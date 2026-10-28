package com.pulse.player.ui.screens

import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pulse.player.data.favorites.MatchResult
import com.pulse.player.playback.RingtoneHelper
import com.pulse.player.playback.Track
import com.pulse.player.ui.components.Artwork
import com.pulse.player.ui.components.PlayingBars
import com.pulse.player.ui.components.formatTime
import com.pulse.player.ui.theme.Pulse
import com.pulse.player.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueueSheet(vm: MainViewModel) {
    val queue by vm.player.queue.collectAsStateWithLifecycle()
    val ps by vm.player.state.collectAsStateWithLifecycle()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = { vm.closeQueue() },
        sheetState = sheetState,
        containerColor = Pulse.Surface
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Play Queue (${queue.size})",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White
                )
                if (queue.isNotEmpty()) {
                    TextButton(onClick = { vm.player.clearQueue() }) {
                        Text("Clear", color = Pulse.Error)
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            HorizontalDivider(color = Pulse.Outline)
            if (queue.isEmpty()) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Queue is empty", color = Pulse.TextMid)
                }
            } else {
                LazyColumn(
                    Modifier
                        .fillMaxWidth()
                        .height(380.dp)
                ) {
                    itemsIndexed(queue) { index, track ->
                        val isCurrent = index == ps.index
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable { vm.player.skipToQueueIndex(index) }
                                .padding(vertical = 8.dp),
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
                                    color = if (isCurrent) Pulse.Cyan else Color.White
                                )
                                Text(
                                    track.artist,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Pulse.TextMid
                                )
                            }
                            IconButton(onClick = { vm.player.removeFromQueue(index) }) {
                                Icon(Icons.Rounded.Close, "Remove", tint = Pulse.TextLow)
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
    val sheetState = rememberModalBottomSheetState()
    var customMin by remember { mutableFloatStateOf(30f) }

    ModalBottomSheet(
        onDismissRequest = { vm.closeSleepSheet() },
        sheetState = sheetState,
        containerColor = Pulse.Surface
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Sleep Timer", style = MaterialTheme.typography.titleLarge, color = Color.White)
            if (ps.sleepRemainingMs > 0) {
                Text(
                    text = "Timer active: ${formatTime(ps.sleepRemainingMs)} remaining",
                    color = Pulse.Cyan,
                    fontWeight = FontWeight.Bold
                )
            } else if (ps.sleepEndOfTrack) {
                Text("Will stop at the end of this track", color = Pulse.Cyan, fontWeight = FontWeight.Bold)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(15, 30, 45, 60).forEach { min ->
                    FilterChip(
                        selected = false,
                        onClick = {
                            vm.player.startSleep(min)
                            vm.closeSleepSheet()
                        },
                        label = { Text("${min}m") }
                    )
                }
            }

            Column {
                Text("Custom: ${customMin.toInt()} minutes", style = MaterialTheme.typography.bodySmall, color = Pulse.TextMid)
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
                    colors = ButtonDefaults.buttonColors(containerColor = Pulse.Cyan, contentColor = Color.Black)
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
                colors = ButtonDefaults.buttonColors(containerColor = Pulse.Elevated, contentColor = Color.White)
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
                    colors = ButtonDefaults.buttonColors(containerColor = Pulse.Error.copy(alpha = 0.2f), contentColor = Pulse.Error)
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
    val ctx = LocalContext.current
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = { vm.closeAddToPlaylist() },
        sheetState = sheetState,
        containerColor = Pulse.Surface
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Add to Playlist", style = MaterialTheme.typography.titleLarge, color = Color.White)
                TextButton(onClick = {
                    vm.closeAddToPlaylist()
                    vm.createPlaylistOpen.value = true
                }) {
                    Text("+ New", color = Pulse.Cyan)
                }
            }
            Spacer(Modifier.height(12.dp))
            if (playlists.isEmpty()) {
                Text("No playlists yet. Create one!", color = Pulse.TextMid, modifier = Modifier.padding(vertical = 16.dp))
            } else {
                LazyColumn {
                    items(playlists.size) { i ->
                        val p = playlists[i]
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable {
                                    vm.addTrackToPlaylist(p.id, track.id)
                                    Toast.makeText(ctx, "Added to ${p.name}", Toast.LENGTH_SHORT).show()
                                    vm.closeAddToPlaylist()
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Rounded.PlaylistAdd, null, tint = Pulse.Cyan)
                            Spacer(Modifier.width(16.dp))
                            Text(p.name, style = MaterialTheme.typography.titleMedium, color = Color.White)
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
        containerColor = Pulse.Surface
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Artwork(track.uri, 48.dp, radius = 10.dp, px = 128)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(track.title, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Bold, color = Color.White)
                    Text("${track.artist} · ${track.album}", maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall, color = Pulse.TextMid)
                }
            }
            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = Pulse.Outline)

            val items = listOf(
                Triple("Play next", Icons.Rounded.QueueMusic) {
                    vm.playNext(track)
                    Toast.makeText(ctx, "Will play next", Toast.LENGTH_SHORT).show()
                },
                Triple("Add to queue", Icons.Rounded.QueueMusic) {
                    vm.addToQueue(track)
                    Toast.makeText(ctx, "Added to queue", Toast.LENGTH_SHORT).show()
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
                Triple("Set as ringtone", Icons.Rounded.Notifications) {
                    val ok = RingtoneHelper.setRingtone(ctx, Uri.parse(track.uri), RingtoneManager.TYPE_RINGTONE)
                    if (ok) Toast.makeText(ctx, "Ringtone updated", Toast.LENGTH_SHORT).show()
                },
                Triple("Share file", Icons.Rounded.Share) {
                    val share = Intent(Intent.ACTION_SEND).apply {
                        type = "audio/*"
                        putExtra(Intent.EXTRA_STREAM, Uri.parse(track.uri))
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    ctx.startActivity(Intent.createChooser(share, "Share ${track.title}"))
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
                    Icon(icon, null, tint = Color.White, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(16.dp))
                    Text(label, style = MaterialTheme.typography.bodyMedium, color = Color.White)
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
        containerColor = Pulse.Surface,
        title = { Text("New Playlist", color = Color.White) },
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
                colors = ButtonDefaults.buttonColors(containerColor = Pulse.Cyan, contentColor = Color.Black)
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = { vm.createPlaylistOpen.value = false }) {
                Text("Cancel", color = Pulse.TextMid)
            }
        }
    )
}

@Composable
fun ImportPreviewDialog(vm: MainViewModel, preview: com.pulse.player.data.favorites.FavoritesRepository.Preview) {
    var replace by remember { mutableStateOf(false) }
    val confidentCount = preview.results.count { it is MatchResult.Matched }
    val alreadyCount = preview.results.count { it is MatchResult.AlreadyFavorite }
    val missingCount = preview.results.count { it is MatchResult.Missing }

    AlertDialog(
        onDismissRequest = { vm.importPreview.value = null },
        containerColor = Pulse.Surface,
        title = { Text("Import Favourites", color = Color.White) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("File: ${preview.fileName}", color = Pulse.TextMid, style = MaterialTheme.typography.bodySmall)
                Text("Total items in file: ${preview.results.size}", color = Color.White)
                Text("• Ready to match: $confidentCount", color = Pulse.Cyan)
                if (alreadyCount > 0) Text("• Already favourite: $alreadyCount", color = Pulse.TextMid)
                if (missingCount > 0) Text("• Not found on device: $missingCount", color = Pulse.TextLow)

                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    androidx.compose.material3.Checkbox(checked = replace, onCheckedChange = { replace = it })
                    Text("Replace existing favourites", color = Color.White, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { vm.applyImport(preview, emptyMap(), replace) },
                colors = ButtonDefaults.buttonColors(containerColor = Pulse.Cyan, contentColor = Color.Black)
            ) {
                Text("Apply Import")
            }
        },
        dismissButton = {
            TextButton(onClick = { vm.importPreview.value = null }) {
                Text("Cancel", color = Pulse.TextMid)
            }
        }
    )
}

@Composable
fun SongDetailsDialog(track: Track, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Pulse.Surface,
        title = { Text("Song Details", color = Color.White) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Title: ${track.title}", color = Color.White, fontWeight = FontWeight.Bold)
                Text("Artist: ${track.artist}", color = Pulse.TextMid)
                Text("Album: ${track.album}", color = Pulse.TextMid)
                if (track.durationMs > 0) Text("Duration: ${formatTime(track.durationMs)}", color = Pulse.TextMid)
                if (track.folder.isNotBlank()) Text("Folder: ${track.folder}", color = Pulse.TextMid)
                if (track.mimeType != null) Text("Format: ${track.mimeType}", color = Pulse.TextMid)
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) { Text("Close") }
        }
    )
}
