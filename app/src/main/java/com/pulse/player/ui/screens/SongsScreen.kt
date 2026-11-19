package com.pulse.player.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material.icons.rounded.SelectAll
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pulse.player.ui.components.AlphaScroller
import com.pulse.player.ui.components.LocalBottomOverlayPadding
import com.pulse.player.ui.components.PillButton
import com.pulse.player.ui.components.TrackRow
import com.pulse.player.ui.components.UnifiedFilterBar
import com.pulse.player.ui.viewmodel.MainViewModel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlin.random.Random

@Composable
fun SongsScreen(vm: MainViewModel) {
    val tracks by vm.sortedTracks.collectAsStateWithLifecycle()
    val missingTracks by vm.missingTracks.collectAsStateWithLifecycle()
    val songsAlphabetIndex by vm.songsAlphabetIndex.collectAsStateWithLifecycle()
    val missingAlphabetIndex by vm.missingAlphabetIndex.collectAsStateWithLifecycle()
    val missingCount by vm.missingCount.collectAsStateWithLifecycle()
    val showMissing by vm.showMissing.collectAsStateWithLifecycle()
    val playbackStatus by remember(vm) {
        vm.player.state.map { it.mediaId to it.isPlaying }.distinctUntilChanged()
    }.collectAsStateWithLifecycle(initialValue = null to false)
    val (currentMediaId, isPlaying) = playbackStatus
    val scanning by vm.scanning.collectAsStateWithLifecycle(initialValue = false)
    val scanProgress by vm.scanProgress.collectAsStateWithLifecycle(initialValue = 0)
    val selected by vm.selected.collectAsStateWithLifecycle()
    val selectMode by vm.selectMode.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val bottomPad = LocalBottomOverlayPadding.current

    val displayTracks = if (showMissing) missingTracks else tracks
    val alphabetIndex = if (showMissing) missingAlphabetIndex else songsAlphabetIndex

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = listState,
            contentPadding = PaddingValues(bottom = if (selectMode) bottomPad + 76.dp else bottomPad)
        ) {
            item(contentType = "header") {
                Column(
                    modifier = Modifier.padding(top = 12.dp, bottom = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (showMissing) "$missingCount Missing Songs" else "${tracks.size} Songs",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (scanning) {
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "(Scanning…)",
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Spacer(Modifier.weight(1f))
                        IconButton(onClick = { vm.toggleSelectMode() }, modifier = Modifier.size(48.dp)) {
                            Icon(
                                imageVector = Icons.Rounded.Checklist,
                                contentDescription = "Select songs",
                                tint = if (selectMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    UnifiedFilterBar(
                        vm = vm,
                        isTrackTab = true,
                        leadingContent = {
                            if (missingCount > 0) {
                                FilterChip(
                                    selected = showMissing,
                                    onClick = { vm.toggleShowMissing() },
                                    label = {
                                        Text(if (showMissing) "Library" else "Missing ($missingCount)")
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                        labelColor = MaterialTheme.colorScheme.onSurface,
                                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    ),
                                    border = FilterChipDefaults.filterChipBorder(
                                        enabled = true,
                                        selected = showMissing,
                                        borderColor = MaterialTheme.colorScheme.outlineVariant,
                                        selectedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                                    )
                                )
                            }
                        }
                    )

                    if (scanning) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = if (scanProgress > 0) "Scanning your library" else "Finishing library scan",
                                        color = MaterialTheme.colorScheme.onSurface,
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                    Spacer(Modifier.weight(1f))
                                    Text(
                                        text = if (scanProgress > 0) "$scanProgress found" else "Please wait…",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                }
                                LinearProgressIndicator(
                                    modifier = Modifier.fillMaxWidth(),
                                    color = MaterialTheme.colorScheme.primary,
                                    trackColor = MaterialTheme.colorScheme.outlineVariant
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        PillButton(
                            text = "Shuffle",
                            icon = Icons.Rounded.Shuffle,
                            onClick = {
                                if (displayTracks.isNotEmpty()) {
                                    vm.playQueue(displayTracks, Random.nextInt(displayTracks.size), shuffle = true)
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )
                        PillButton(
                            text = "Play",
                            icon = Icons.Rounded.PlayArrow,
                            onClick = {
                                if (displayTracks.isNotEmpty()) {
                                    vm.playQueue(displayTracks, 0)
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    if (showMissing && missingCount > 0) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                        ) {
                            Text(
                                text = "These files weren't found on the last scan. Your favourites and playlists keep them.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }
            }

            if (displayTracks.isEmpty() && !scanning) {
                item(contentType = "empty") {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp, horizontal = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            Icons.Rounded.MusicNote,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = if (showMissing) "No missing songs" else "No matching songs",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (showMissing) "Every track in your library is available on storage." else "Try adjusting your filters or scan your library from For You.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                itemsIndexed(
                    items = displayTracks,
                    key = { _, t -> t.id },
                    contentType = { _, _ -> "track" }
                ) { index, track ->
                    TrackRow(
                        t = track,
                        current = track.id == currentMediaId,
                        playing = isPlaying,
                        selected = track.id in selected,
                        selectMode = selectMode,
                        onSwipeRightQueue = {
                            vm.playNext(track)
                            vm.snackbarMessage.value = "Queued next: ${track.title}"
                        },
                        onSwipeLeftFavorite = {
                            vm.toggleFavorite(track.id)
                            vm.snackbarMessage.value = if (!track.isFavorite) "Added to favourites" else "Removed from favourites"
                        },
                        onClick = {
                            if (selectMode) {
                                vm.toggleSelect(track.id)
                            } else {
                                vm.playQueue(displayTracks, index)
                            }
                        },
                        onLongClick = { vm.toggleSelect(track.id) },
                        onMore = { vm.openTrackMenu(track) }
                    )
                }
            }
        }

        AlphaScroller(
            firstIndex = alphabetIndex,
            listState = listState,
            modifier = Modifier.align(Alignment.CenterEnd)
        )

        if (selectMode) {
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = bottomPad, start = 16.dp, end = 16.dp)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                tonalElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("${selected.size} selected", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                    Row {
                        IconButton(onClick = { vm.selectAll(displayTracks.map { it.id }) }, modifier = Modifier.size(48.dp)) {
                            Icon(Icons.Rounded.SelectAll, "Select all", tint = MaterialTheme.colorScheme.onSurface)
                        }
                        IconButton(onClick = { vm.openBatchTagEditor() }, enabled = selected.isNotEmpty(), modifier = Modifier.size(48.dp)) {
                            Icon(
                                Icons.Rounded.Edit,
                                "Batch edit tags",
                                tint = if (selected.isNotEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                            )
                        }
                        IconButton(onClick = { vm.playSelectedNext() }, enabled = selected.isNotEmpty(), modifier = Modifier.size(48.dp)) {
                            Icon(
                                Icons.Rounded.QueueMusic,
                                "Play next",
                                tint = if (selected.isNotEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                            )
                        }
                        IconButton(onClick = { vm.favoriteSelected() }, enabled = selected.isNotEmpty(), modifier = Modifier.size(48.dp)) {
                            Icon(
                                Icons.Rounded.Favorite,
                                "Favorite",
                                tint = if (selected.isNotEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                            )
                        }
                        IconButton(onClick = { vm.clearSelect() }, modifier = Modifier.size(48.dp)) {
                            Icon(Icons.Rounded.Close, "Close select", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}
