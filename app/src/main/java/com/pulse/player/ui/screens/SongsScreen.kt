package com.pulse.player.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material.icons.rounded.SelectAll
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.Sort
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pulse.player.ui.components.AlphabetScroller
import com.pulse.player.ui.components.PillButton
import com.pulse.player.ui.components.TrackRow
import com.pulse.player.ui.theme.Pulse
import com.pulse.player.ui.viewmodel.MainViewModel
import com.pulse.player.ui.viewmodel.SortField
import kotlin.random.Random

@Composable
fun SongsScreen(vm: MainViewModel) {
    val tracks by vm.sortedTracks.collectAsStateWithLifecycle()
    val missingTracks by vm.missingTracks.collectAsStateWithLifecycle()
    val missingCount by vm.missingCount.collectAsStateWithLifecycle()
    val showMissing by vm.showMissing.collectAsStateWithLifecycle()
    val ps by vm.player.state.collectAsStateWithLifecycle()
    val scanning by vm.scanning.collectAsStateWithLifecycle()
    val selected by vm.selected.collectAsStateWithLifecycle()
    val selectMode by vm.selectMode.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    val displayTracks = if (showMissing) missingTracks else tracks

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = listState,
            contentPadding = PaddingValues(bottom = if (selectMode) 160.dp else 120.dp)
        ) {
            item {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (showMissing) "$missingCount Missing Songs" else "${tracks.size} Songs",
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White
                        )
                        if (scanning) {
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "(Scanning…)",
                                color = Pulse.Cyan,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Spacer(Modifier.weight(1f))
                        SortMenu(vm)
                        IconButton(onClick = { vm.toggleSelectMode() }) {
                            Icon(
                                imageVector = Icons.Rounded.Checklist,
                                contentDescription = "Select songs",
                                tint = if (selectMode) Pulse.Cyan else Color.White
                            )
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
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

                    if (missingCount > 0) {
                        androidx.compose.material3.FilterChip(
                            selected = showMissing,
                            onClick = { vm.toggleShowMissing() },
                            label = {
                                Text(
                                    text = if (showMissing) "Show library songs" else "Missing songs ($missingCount)",
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        )
                        if (showMissing) {
                            Surface(
                                color = Pulse.SurfaceCard,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "These files weren't found on the last scan. Your favourites and playlists keep them.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Pulse.TextSecondary,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }
                    }
                }
            }

            itemsIndexed(displayTracks, key = { _, t -> t.id }) { index, track ->
                TrackRow(
                    t = track,
                    current = track.id == ps.mediaId,
                    playing = ps.isPlaying,
                    selected = track.id in selected,
                    selectMode = selectMode,
                    onClick = {
                        if (selectMode) {
                            vm.toggleSelect(track.id)
                        } else {
                            vm.playQueue(displayTracks, index)
                        }
                    },
                    onLongClick = { vm.toggleSelect(track.id) },
                    onMore = { vm.openTrackMenu(track) },
                    modifier = Modifier.animateItem()
                )
            }
        }

        AlphabetScroller(
            titles = displayTracks.map { it.title },
            listState = listState,
            modifier = Modifier.align(Alignment.CenterEnd)
        )

        if (selectMode) {
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 76.dp, start = 16.dp, end = 16.dp)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = Pulse.Elevated,
                tonalElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("${selected.size} selected", color = Color.White, fontWeight = FontWeight.Bold)
                    Row {
                        IconButton(onClick = { vm.selectAll() }) {
                            Icon(Icons.Rounded.SelectAll, "Select all", tint = Color.White)
                        }
                        IconButton(onClick = { vm.playSelectedNext() }, enabled = selected.isNotEmpty()) {
                            Icon(Icons.Rounded.QueueMusic, "Play next", tint = if (selected.isNotEmpty()) Pulse.Cyan else Pulse.TextLow)
                        }
                        IconButton(onClick = { vm.favoriteSelected() }, enabled = selected.isNotEmpty()) {
                            Icon(Icons.Rounded.Favorite, "Favorite", tint = if (selected.isNotEmpty()) Pulse.Cyan else Pulse.TextLow)
                        }
                        IconButton(onClick = { vm.clearSelect() }) {
                            Icon(Icons.Rounded.Close, "Close select", tint = Pulse.TextMid)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SortMenu(vm: MainViewModel) {
    var expanded by remember { mutableStateOf(false) }
    val currentField by vm.sortField.collectAsStateWithLifecycle()
    val asc by vm.sortAscending.collectAsStateWithLifecycle()

    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(Icons.Rounded.Sort, "Sort options", tint = Color.White)
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(Pulse.Elevated)
        ) {
            DropdownMenuItem(
                text = { Text("Title ${if (currentField == SortField.TITLE) if (asc) "↑" else "↓" else ""}") },
                onClick = {
                    if (currentField == SortField.TITLE) vm.sortAscending.value = !asc
                    else { vm.sortField.value = SortField.TITLE; vm.sortAscending.value = true }
                    expanded = false
                }
            )
            DropdownMenuItem(
                text = { Text("Artist ${if (currentField == SortField.ARTIST) if (asc) "↑" else "↓" else ""}") },
                onClick = {
                    if (currentField == SortField.ARTIST) vm.sortAscending.value = !asc
                    else { vm.sortField.value = SortField.ARTIST; vm.sortAscending.value = true }
                    expanded = false
                }
            )
            DropdownMenuItem(
                text = { Text("Date Added ${if (currentField == SortField.DATE_ADDED) "↓" else ""}") },
                onClick = {
                    vm.sortField.value = SortField.DATE_ADDED
                    vm.sortAscending.value = false
                    expanded = false
                }
            )
            DropdownMenuItem(
                text = { Text("Duration ${if (currentField == SortField.DURATION) "↓" else ""}") },
                onClick = {
                    vm.sortField.value = SortField.DURATION
                    vm.sortAscending.value = false
                    expanded = false
                }
            )
        }
    }
}
