package com.pulse.player.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.BookmarkAdd
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pulse.player.data.db.SavedQueueEntity
import com.pulse.player.playback.Track
import com.pulse.player.ui.components.Artwork
import com.pulse.player.ui.components.PlayingBars
import com.pulse.player.ui.components.formatTime
import com.pulse.player.ui.theme.TabularMono
import com.pulse.player.ui.viewmodel.MainViewModel
import kotlin.math.abs
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueueSheet(vm: MainViewModel) {
    val queue by vm.player.queue.collectAsStateWithLifecycle()
    val ps by vm.player.state.collectAsStateWithLifecycle()
    val savedQueues by vm.savedQueues.collectAsStateWithLifecycle()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var confirmClear by remember { mutableStateOf(false) }
    var savePlaylistOpen by remember { mutableStateOf(false) }
    var saveSnapshotOpen by remember { mutableStateOf(false) }
    var showSavedQueues by remember { mutableStateOf(false) }
    val haptics = LocalHapticFeedback.current
    val density = LocalDensity.current
    val rowStepPx = remember(density) { with(density) { 64.dp.toPx() } }

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
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "Play Queue (${queue.size})",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (queue.isNotEmpty()) {
                        Text(
                            text = "Swipe to remove · Drag handle to reorder",
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
                            shape = CircleShape,
                            modifier = Modifier.testTag("queue_confirm_clear")
                        ) { Text("Confirm Clear", fontWeight = FontWeight.SemiBold) }
                    } else {
                        TextButton(
                            onClick = { confirmClear = true },
                            modifier = Modifier.testTag("queue_clear_button")
                        ) { Text("Clear", color = MaterialTheme.colorScheme.error) }
                    }
                }
            }

            if (queue.isNotEmpty() || savedQueues.isNotEmpty()) {
                val chipColors = AssistChipDefaults.assistChipColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    labelColor = MaterialTheme.colorScheme.onSurface,
                    leadingIconContentColor = MaterialTheme.colorScheme.primary
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(top = 6.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (queue.isNotEmpty()) {
                        AssistChip(
                            onClick = { vm.shuffleUpcomingQueue() },
                            label = { Text("Shuffle Upcoming") },
                            leadingIcon = { Icon(Icons.Rounded.Shuffle, null, Modifier.size(16.dp)) },
                            colors = chipColors,
                            modifier = Modifier.testTag("queue_shuffle_upcoming")
                        )
                        AssistChip(
                            onClick = {
                                val cur = vm.currentPlayingTrack() ?: queue.getOrNull(ps.index)
                                if (cur != null) vm.playSimilarMix(cur)
                            },
                            label = { Text("Play Similar") },
                            leadingIcon = { Icon(Icons.Rounded.AutoAwesome, null, Modifier.size(16.dp)) },
                            colors = chipColors,
                            modifier = Modifier.testTag("queue_play_similar")
                        )
                        AssistChip(
                            onClick = { saveSnapshotOpen = true },
                            label = { Text("Save Snapshot") },
                            leadingIcon = { Icon(Icons.Rounded.BookmarkAdd, null, Modifier.size(16.dp)) },
                            colors = chipColors,
                            modifier = Modifier.testTag("queue_save_snapshot")
                        )
                        AssistChip(
                            onClick = { savePlaylistOpen = true },
                            label = { Text("Save as Playlist") },
                            leadingIcon = { Icon(Icons.AutoMirrored.Rounded.PlaylistAdd, null, Modifier.size(16.dp)) },
                            colors = chipColors,
                            modifier = Modifier.testTag("queue_save_playlist")
                        )
                    }
                    if (savedQueues.isNotEmpty()) {
                        FilterChip(
                            selected = showSavedQueues,
                            onClick = { showSavedQueues = !showSavedQueues },
                            label = { Text("Saved (${savedQueues.size})") },
                            leadingIcon = { Icon(Icons.Rounded.History, null, Modifier.size(16.dp)) },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                labelColor = MaterialTheme.colorScheme.onSurface,
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            modifier = Modifier.testTag("queue_toggle_saved")
                        )
                    }
                }
            }

            if (showSavedQueues && savedQueues.isNotEmpty()) {
                SavedQueuesStrip(
                    savedQueues = savedQueues,
                    onRestore = { id ->
                        vm.restoreSavedQueue(id)
                        showSavedQueues = false
                    },
                    onDelete = vm::deleteSavedQueue
                )
            }

            Spacer(Modifier.height(6.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            if (queue.isEmpty()) {
                Column(
                    Modifier.fillMaxWidth().height(200.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically)
                ) {
                    Icon(
                        Icons.AutoMirrored.Rounded.QueueMusic,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(40.dp)
                    )
                    Text("Queue is empty", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                    Text("Tap Play or Queue Next on any song.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(Modifier.fillMaxWidth().height(400.dp)) {
                    itemsIndexed(
                        queue,
                        key = { index, track -> "${track.id}_$index" },
                        contentType = { _, _ -> "queue_track" }
                    ) { index, track ->
                        QueueTrackRow(
                            track = track,
                            index = index,
                            maxIndex = queue.lastIndex,
                            isCurrent = index == ps.index,
                            isPlaying = ps.isPlaying,
                            rowStepPx = rowStepPx,
                            onSkipTo = { vm.player.skipToQueueIndex(index) },
                            onMove = { from, to ->
                                vm.player.moveInQueue(from, to)
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            },
                            onRemove = { idx ->
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                vm.removeFromQueueWithUndo(idx)
                            },
                            onSwipeThresholdHaptic = {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                        )
                    }
                }
            }
        }
    }

    if (saveSnapshotOpen) {
        QueueNamePromptDialog(
            title = "Save Queue Snapshot",
            label = "Snapshot name",
            initialValue = "Session Mix",
            confirmLabel = "Save Snapshot",
            onConfirm = { name ->
                vm.saveCurrentQueueSession(name)
                saveSnapshotOpen = false
            },
            onDismiss = { saveSnapshotOpen = false }
        )
    }

    if (savePlaylistOpen) {
        QueueNamePromptDialog(
            title = "Save Queue as Playlist",
            label = "Playlist name",
            initialValue = "Queue Mix",
            confirmLabel = "Save",
            onConfirm = { name ->
                vm.saveQueueAsPlaylist(name)
                savePlaylistOpen = false
            },
            onDismiss = { savePlaylistOpen = false }
        )
    }
}

@Composable
private fun SavedQueuesStrip(
    savedQueues: List<SavedQueueEntity>,
    onRestore: (Long) -> Unit,
    onDelete: (Long) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        savedQueues.take(5).forEach { sq ->
            Surface(
                onClick = { onRestore(sq.id) },
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 12.dp, top = 4.dp, bottom = 4.dp, end = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.PlayArrow, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Column(Modifier.weight(1f)) {
                        Text(sq.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("Track #${sq.currentIndex + 1} · ${formatTime(sq.positionMs)}", style = MaterialTheme.typography.labelSmall.merge(TabularMono), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = { onDelete(sq.id) }, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Rounded.DeleteOutline, "Delete saved queue ${sq.name}", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun QueueTrackRow(
    track: Track,
    index: Int,
    maxIndex: Int,
    isCurrent: Boolean,
    isPlaying: Boolean,
    rowStepPx: Float,
    onSkipTo: () -> Unit,
    onMove: (Int, Int) -> Unit,
    onRemove: (Int) -> Unit,
    onSwipeThresholdHaptic: () -> Unit
) {
    val currentIndex by rememberUpdatedState(index)
    val latestMaxIndex by rememberUpdatedState(maxIndex)
    var swipeX by remember(track.id, index) { mutableFloatStateOf(0f) }
    var dragYAcc by remember(track.id) { mutableFloatStateOf(0f) }
    var swipeTriggered by remember(track.id, index) { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .clip(RoundedCornerShape(12.dp))
    ) {
        if (abs(swipeX) > 12f) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(MaterialTheme.colorScheme.errorContainer)
                    .padding(horizontal = 16.dp),
                contentAlignment = if (swipeX > 0) Alignment.CenterStart else Alignment.CenterEnd
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Rounded.DeleteOutline, null, tint = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.size(18.dp))
                    Text("Remove", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onErrorContainer)
                }
            }
        }

        Row(
            Modifier
                .fillMaxWidth()
                .offset { IntOffset(swipeX.roundToInt(), 0) }
                .background(if (isCurrent) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
                .then(
                    if (isCurrent) Modifier.border(BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)), RoundedCornerShape(12.dp))
                    else Modifier
                )
                .pointerInput(track.id, index) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            if (abs(swipeX) > 130f) onRemove(currentIndex)
                            swipeX = 0f
                            swipeTriggered = false
                        },
                        onDragCancel = {
                            swipeX = 0f
                            swipeTriggered = false
                        },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            swipeX = (swipeX + dragAmount).coerceIn(-240f, 240f)
                            if (!swipeTriggered && abs(swipeX) > 130f) {
                                swipeTriggered = true
                                onSwipeThresholdHaptic()
                            } else if (swipeTriggered && abs(swipeX) <= 130f) {
                                swipeTriggered = false
                            }
                        }
                    )
                }
                .clickable(onClick = onSkipTo)
                .padding(horizontal = 4.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .pointerInput(track.id) {
                        detectVerticalDragGestures(
                            onDragStart = { dragYAcc = 0f },
                            onDragEnd = { dragYAcc = 0f },
                            onDragCancel = { dragYAcc = 0f },
                            onVerticalDrag = { change, dragAmount ->
                                change.consume()
                                dragYAcc += dragAmount
                                if (dragYAcc >= rowStepPx && currentIndex < latestMaxIndex) {
                                    dragYAcc -= rowStepPx
                                    onMove(currentIndex, currentIndex + 1)
                                } else if (dragYAcc <= -rowStepPx && currentIndex > 0) {
                                    dragYAcc += rowStepPx
                                    onMove(currentIndex, currentIndex - 1)
                                }
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.DragHandle, "Drag to reorder", tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f), modifier = Modifier.size(18.dp))
            }
            Box {
                Artwork(track.uri, 44.dp, radius = 8.dp, px = 128)
                if (isCurrent) {
                    Box(Modifier.matchParentSize().background(Color.Black.copy(0.45f), RoundedCornerShape(8.dp)), Alignment.Center) {
                        PlayingBars(active = isPlaying)
                    }
                }
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    track.title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                    color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
                Text(track.artist, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (index > 0) {
                IconButton(onClick = { onMove(index, index - 1) }, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Rounded.ArrowUpward, "Move up", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                }
            }
            if (index < maxIndex) {
                IconButton(onClick = { onMove(index, index + 1) }, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Rounded.ArrowDownward, "Move down", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                }
            }
            IconButton(onClick = { onRemove(index) }, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Rounded.Close, "Remove from queue", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun QueueNamePromptDialog(
    title: String,
    label: String,
    initialValue: String,
    confirmLabel: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var value by remember { mutableStateOf(initialValue) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        title = { Text(title) },
        text = { OutlinedTextField(value = value, onValueChange = { value = it }, label = { Text(label) }, singleLine = true) },
        confirmButton = { Button(onClick = { onConfirm(value) }, enabled = value.isNotBlank()) { Text(confirmLabel) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
