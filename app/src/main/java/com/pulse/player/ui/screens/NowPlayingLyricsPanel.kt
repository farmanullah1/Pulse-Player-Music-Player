package com.pulse.player.ui.screens

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.FileOpen
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pulse.player.data.lyrics.Lrc
import com.pulse.player.data.lyrics.LrcLine
import com.pulse.player.ui.components.formatTime
import com.pulse.player.ui.theme.Pulse
import com.pulse.player.ui.theme.TabularMono
import com.pulse.player.ui.viewmodel.MainViewModel
import com.pulse.player.ui.viewmodel.ResolvedLyrics

@Composable
fun LyricsView(
    vm: MainViewModel,
    mediaId: String?,
    modifier: Modifier = Modifier,
    onDarkSurface: Boolean = false
) {
    val allTracks by vm.tracks.collectAsStateWithLifecycle()
    val currentTrack = remember(mediaId, allTracks) {
        mediaId?.let { id -> allTracks.firstOrNull { it.id == id } }
    }
    val resolvedFlow = remember(mediaId, currentTrack?.dataPath) {
        vm.observeResolvedLyrics(mediaId, currentTrack?.dataPath)
    }
    val resolved by resolvedFlow.collectAsStateWithLifecycle(initialValue = ResolvedLyrics())
    val lrcLines = resolved.lines
    val listState = rememberLazyListState()
    val currentPosMs by vm.player.position.collectAsStateWithLifecycle()

    val primaryAccent = if (onDarkSurface) Color(0xFFBBD8C8) else MaterialTheme.colorScheme.primary
    val secondaryInk = if (onDarkSurface) Color.White.copy(alpha = 0.78f) else MaterialTheme.colorScheme.onSurfaceVariant
    val pillBg = if (onDarkSurface) Color.White.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceContainerHigh

    val latestLines by rememberUpdatedState(lrcLines)

    val lrcPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        val targetId = mediaId
        if (uri != null && !targetId.isNullOrBlank()) {
            vm.importLyrics(targetId, uri)
        }
    }

    val lrcExporter = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { destUri ->
        if (destUri != null && latestLines.isNotEmpty()) {
            vm.exportLrcToUri(destUri, latestLines)
        }
    }

    var lrcOffsetMs by remember(mediaId) { mutableLongStateOf(0L) }
    var tapSyncActive by remember(mediaId) { mutableStateOf(false) }
    var tapSyncCursor by remember(mediaId) { mutableIntStateOf(0) }
    val draftLines = remember(mediaId) { mutableStateListOf<LrcLine>() }

    BackHandler(enabled = tapSyncActive) {
        tapSyncActive = false
    }

    val activeIdx = remember(lrcLines, currentPosMs, lrcOffsetMs, tapSyncActive) {
        if (tapSyncActive) return@remember -1
        val effectivePos = (currentPosMs + lrcOffsetMs).coerceAtLeast(0L)
        if (lrcLines.any { it.timeMs >= 0L }) {
            Lrc.activeIndex(lrcLines, effectivePos)
        } else {
            -1
        }
    }

    LaunchedEffect(activeIdx, tapSyncActive) {
        if (!tapSyncActive && activeIdx >= 0 && !listState.isScrollInProgress) {
            listState.animateScrollToItem(activeIdx)
        }
    }

    LaunchedEffect(tapSyncCursor, tapSyncActive) {
        if (tapSyncActive && tapSyncCursor in draftLines.indices) {
            listState.animateScrollToItem(tapSyncCursor)
        }
    }

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        if (lrcLines.isEmpty() && !tapSyncActive) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(24.dp)
            ) {
                Text("No lyrics found for this track", color = secondaryInk, textAlign = TextAlign.Center)
                Text(
                    "Place a matching .lrc file beside the audio file or import one below.",
                    style = MaterialTheme.typography.bodySmall,
                    color = secondaryInk.copy(alpha = 0.72f),
                    textAlign = TextAlign.Center
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { lrcPicker.launch(arrayOf("*/*", "text/*")) },
                        shape = CircleShape,
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("lyrics_import_empty_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = pillBg,
                            contentColor = primaryAccent
                        )
                    ) {
                        Icon(Icons.Rounded.FileOpen, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Import .lrc")
                    }
                    Button(
                        onClick = {
                            vm.closeNowPlaying()
                            vm.go(Screen.Transcribe)
                        },
                        shape = CircleShape,
                        modifier = Modifier.heightIn(min = 48.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Icon(Icons.Rounded.AutoAwesome, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Auto-Transcribe", fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else if (tapSyncActive) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = "Tap-to-Sync Studio",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = primaryAccent
                        )
                        Text(
                            text = "Line ${(tapSyncCursor + 1).coerceAtMost(draftLines.size)} of ${draftLines.size}",
                            style = MaterialTheme.typography.labelSmall.merge(TabularMono),
                            color = secondaryInk
                        )
                    }
                    TextButton(
                        onClick = { tapSyncActive = false },
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("lyrics_tap_sync_cancel")
                    ) {
                        Icon(Icons.Rounded.Close, null, tint = secondaryInk, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Cancel", color = secondaryInk)
                    }
                    Button(
                        onClick = {
                            val id = mediaId
                            if (!id.isNullOrBlank()) {
                                vm.saveSyncedLyrics(id, draftLines.toList())
                            }
                            lrcOffsetMs = 0L
                            tapSyncActive = false
                        },
                        shape = CircleShape,
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("lyrics_tap_sync_save"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Pulse.Mint,
                            contentColor = Color.Black
                        )
                    ) {
                        Icon(Icons.Rounded.Check, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Save Sync", fontWeight = FontWeight.Bold)
                    }
                }

                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    itemsIndexed(draftLines) { idx, line ->
                        val isTarget = idx == tapSyncCursor
                        Surface(
                            onClick = { tapSyncCursor = idx },
                            shape = RoundedCornerShape(14.dp),
                            color = if (isTarget) primaryAccent.copy(alpha = 0.22f) else pillBg.copy(alpha = 0.65f),
                            border = BorderStroke(
                                if (isTarget) 1.5.dp else 1.dp,
                                if (isTarget) primaryAccent else Color.Transparent
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = if (line.timeMs >= 0L) Lrc.formatTimestamp(line.timeMs) else "[--:--.--]",
                                    style = MaterialTheme.typography.labelSmall.merge(TabularMono),
                                    color = if (line.timeMs >= 0L) primaryAccent else secondaryInk.copy(alpha = 0.6f),
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = line.text,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (isTarget) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isTarget) primaryAccent else secondaryInk,
                                    modifier = Modifier.weight(1f),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = {
                            if (tapSyncCursor > 0) tapSyncCursor -= 1
                        },
                        enabled = tapSyncCursor > 0,
                        shape = CircleShape,
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("lyrics_tap_sync_undo")
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.Undo, contentDescription = "Previous line", tint = secondaryInk)
                        Spacer(Modifier.width(4.dp))
                        Text("Step Back", color = secondaryInk)
                    }
                    Button(
                        onClick = {
                            if (tapSyncCursor in draftLines.indices) {
                                val stamp = currentPosMs.coerceAtLeast(0L)
                                draftLines[tapSyncCursor] = draftLines[tapSyncCursor].copy(timeMs = stamp)
                                if (tapSyncCursor < draftLines.lastIndex) {
                                    tapSyncCursor += 1
                                }
                            }
                        },
                        enabled = tapSyncCursor in draftLines.indices,
                        shape = CircleShape,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 50.dp)
                            .testTag("lyrics_tap_sync_stamp"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Icon(Icons.Rounded.TouchApp, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "Stamp Line (${formatTime(currentPosMs)})",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelLarge.merge(TabularMono)
                        )
                    }
                }
            }
        } else {
            Column(Modifier.fillMaxSize()) {
                // Source badge + Import / Tap-to-Sync toolbar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    resolved.sourceLabel?.let { source ->
                        Surface(
                            shape = CircleShape,
                            color = pillBg
                        ) {
                            Text(
                                text = source,
                                style = MaterialTheme.typography.labelSmall,
                                color = primaryAccent,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                    FilterChip(
                        selected = false,
                        onClick = { lrcPicker.launch(arrayOf("*/*", "text/*")) },
                        label = { Text("Import .lrc") },
                        leadingIcon = { Icon(Icons.Rounded.FileOpen, null, modifier = Modifier.size(15.dp)) },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = pillBg,
                            labelColor = secondaryInk,
                            iconColor = primaryAccent
                        ),
                        modifier = Modifier.testTag("lyrics_replace_lrc_chip")
                    )
                    FilterChip(
                        selected = false,
                        onClick = {
                            draftLines.clear()
                            draftLines.addAll(lrcLines)
                            tapSyncCursor = activeIdx.coerceAtLeast(0).coerceIn(0, (draftLines.size - 1).coerceAtLeast(0))
                            tapSyncActive = true
                        },
                        label = { Text("Tap Sync") },
                        leadingIcon = { Icon(Icons.Rounded.TouchApp, null, modifier = Modifier.size(15.dp)) },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = pillBg,
                            labelColor = primaryAccent,
                            iconColor = primaryAccent
                        ),
                        modifier = Modifier.testTag("lyrics_tap_sync_chip")
                    )
                    FilterChip(
                        selected = false,
                        onClick = {
                            val base = (currentTrack?.title ?: "lyrics")
                                .replace(Regex("[^a-zA-Z0-9._ -]"), "_")
                                .trim()
                                .ifBlank { "lyrics" }
                            lrcExporter.launch("$base.lrc")
                        },
                        label = { Text("Export .lrc") },
                        leadingIcon = { Icon(Icons.Rounded.Download, null, modifier = Modifier.size(15.dp)) },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = pillBg,
                            labelColor = secondaryInk,
                            iconColor = primaryAccent
                        ),
                        modifier = Modifier.testTag("lyrics_export_lrc_chip")
                    )
                }

                // Inline Sync Offset Nudger + Persistent Save
                if (resolved.isSynced) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Offset ${if (lrcOffsetMs >= 0) "+" else ""}${"%.1f".format(lrcOffsetMs / 1000f)}s",
                            style = MaterialTheme.typography.labelSmall.merge(TabularMono),
                            color = secondaryInk
                        )
                        Button(
                            onClick = { lrcOffsetMs -= 500L },
                            shape = CircleShape,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = pillBg,
                                contentColor = secondaryInk
                            )
                        ) {
                            Text("-0.5s", style = MaterialTheme.typography.labelSmall.merge(TabularMono))
                        }
                        Button(
                            onClick = { lrcOffsetMs = 0L },
                            shape = CircleShape,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = pillBg,
                                contentColor = secondaryInk
                            )
                        ) {
                            Text("Reset", style = MaterialTheme.typography.labelSmall)
                        }
                        Button(
                            onClick = { lrcOffsetMs += 500L },
                            shape = CircleShape,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = pillBg,
                                contentColor = secondaryInk
                            )
                        ) {
                            Text("+0.5s", style = MaterialTheme.typography.labelSmall.merge(TabularMono))
                        }
                        if (lrcOffsetMs != 0L) {
                            Button(
                                onClick = {
                                    val id = mediaId
                                    if (!id.isNullOrBlank()) {
                                        val updated = Lrc.applyPlaybackLeadOffset(lrcLines, lrcOffsetMs)
                                        vm.saveSyncedLyrics(id, updated)
                                        lrcOffsetMs = 0L
                                    }
                                },
                                shape = CircleShape,
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Pulse.Mint,
                                    contentColor = Color.Black
                                ),
                                modifier = Modifier.testTag("lyrics_save_offset_button")
                            ) {
                                Text("Save Offset", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    itemsIndexed(lrcLines) { idx, line ->
                        val isActive = idx == activeIdx
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isActive) primaryAccent.copy(alpha = 0.16f)
                                    else Color.Transparent
                                )
                                .clickable(enabled = line.timeMs >= 0) {
                                    val target = (line.timeMs - lrcOffsetMs).coerceAtLeast(0L)
                                    vm.player.seekTo(target)
                                }
                                .padding(vertical = 10.dp, horizontal = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = line.text,
                                style = if (isActive) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
                                color = if (isActive) primaryAccent else secondaryInk.copy(alpha = 0.72f),
                                fontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.Normal,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}
