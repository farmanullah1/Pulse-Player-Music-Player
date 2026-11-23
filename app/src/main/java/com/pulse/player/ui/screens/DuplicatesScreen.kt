package com.pulse.player.ui.screens

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.FolderOff
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.UnfoldLess
import androidx.compose.material.icons.rounded.UnfoldMore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pulse.player.data.ErrorCategory
import com.pulse.player.data.ErrorDispatcher
import com.pulse.player.data.db.TrackEntity
import com.pulse.player.playback.Track
import com.pulse.player.ui.components.LocalBottomOverlayPadding
import com.pulse.player.ui.components.PulseTopBar
import com.pulse.player.ui.components.formatTime
import com.pulse.player.ui.theme.Pulse
import com.pulse.player.ui.theme.TabularMono
import com.pulse.player.ui.viewmodel.MainViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

data class DupGroupWithMembers(
    val title: String,
    val artist: String,
    val count: Int,
    val members: List<TrackEntity>
)

internal enum class DuplicateSortMode(val label: String) {
    RECLAIMABLE_DESC("Largest Size"),
    COPIES_DESC("Most Copies"),
    TITLE_ASC("Title A–Z")
}

internal fun selectBestMemberId(members: List<TrackEntity>): String? {
    if (members.isEmpty()) return null
    val losslessCodecs = setOf("FLAC", "WAV", "ALAC", "APE", "DSD")
    return members.maxWithOrNull(
        compareBy<TrackEntity>(
            { it.codec.uppercase(Locale.US) in losslessCodecs },
            { it.bitrate },
            { it.sizeBytes }
        )
    )?.id
}

internal fun groupReclaimableBytes(members: List<TrackEntity>): Long {
    if (members.size <= 1) return 0L
    val total = members.sumOf { it.sizeBytes.coerceAtLeast(0L) }
    val largest = members.maxOf { it.sizeBytes.coerceAtLeast(0L) }
    return (total - largest).coerceAtLeast(0L)
}

internal fun computeDuplicateStats(groups: List<DupGroupWithMembers>): Pair<Int, Long> {
    val totalCopies = groups.sumOf { it.members.size.coerceAtLeast(it.count) }
    val reclaimable = groups.sumOf { groupReclaimableBytes(it.members) }
    return totalCopies to reclaimable
}

internal fun sortDuplicateGroups(
    groups: List<DupGroupWithMembers>,
    mode: DuplicateSortMode
): List<DupGroupWithMembers> = when (mode) {
    DuplicateSortMode.RECLAIMABLE_DESC -> groups.sortedByDescending { groupReclaimableBytes(it.members) }
    DuplicateSortMode.COPIES_DESC -> groups.sortedByDescending { it.count }
    DuplicateSortMode.TITLE_ASC -> groups.sortedBy { it.title.lowercase(Locale.US) }
}

internal fun formatBytesMb(bytes: Long): String {
    if (bytes <= 0L) return "0.0 MB"
    val mb = bytes / (1024.0 * 1024.0)
    return if (mb >= 1024.0) {
        String.format(Locale.US, "%.2f GB", mb / 1024.0)
    } else {
        String.format(Locale.US, "%.1f MB", mb)
    }
}

@Composable
fun DuplicatesScreen(vm: MainViewModel) {
    BackHandler { vm.back() }

    val ctx = LocalContext.current
    var loading by remember { mutableStateOf(true) }
    var groups by remember { mutableStateOf<List<DupGroupWithMembers>>(emptyList()) }
    var sortMode by remember { mutableStateOf(DuplicateSortMode.RECLAIMABLE_DESC) }
    var refreshNonce by remember { mutableIntStateOf(0) }
    var expandedKeys by remember { mutableStateOf<Set<String>>(emptySet()) }
    var selectedForDelete by remember { mutableStateOf<Set<String>>(emptySet()) }
    var pendingDeleteIds by remember { mutableStateOf<List<String>>(emptyList()) }
    var legacyConfirmOpen by remember { mutableStateOf(false) }

    val deleteRequestLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        val targetIds = pendingDeleteIds
        pendingDeleteIds = emptyList()
        if (result.resultCode == Activity.RESULT_OK && targetIds.isNotEmpty()) {
            vm.removeDeletedDuplicates(targetIds) {
                selectedForDelete = selectedForDelete - targetIds.toSet()
                refreshNonce++
            }
        } else {
            vm.snackbarMessage.value = "Deletion cancelled"
        }
    }

    fun groupKey(g: DupGroupWithMembers): String =
        "${g.title}|${g.artist}|${g.members.firstOrNull()?.id.orEmpty()}"

    LaunchedEffect(refreshNonce) {
        loading = true
        withContext(Dispatchers.IO) {
            val db = vm.db
            val rawGroups = db.tracks().dupGroups()
            val list = rawGroups.map { g ->
                val members = db.tracks().dupMembers(g.normTitle, g.normArtist)
                val displayTitle = members.firstOrNull()?.title.orEmpty().ifBlank { g.normTitle }
                val displayArtist = members.firstOrNull()?.artist.orEmpty().ifBlank { g.normArtist }
                DupGroupWithMembers(
                    title = displayTitle,
                    artist = displayArtist,
                    count = g.c,
                    members = members
                )
            }.filter { it.members.size > 1 }
            val defaultExpanded = sortDuplicateGroups(list, sortMode)
                .take(2)
                .map(::groupKey)
                .toSet()
            withContext(Dispatchers.Main) {
                groups = list
                expandedKeys = defaultExpanded
                loading = false
            }
        }
    }

    val sortedGroups = remember(groups, sortMode) { sortDuplicateGroups(groups, sortMode) }
    val (totalCopies, totalReclaimableBytes) = remember(groups) { computeDuplicateStats(groups) }
    val allExpanded = sortedGroups.isNotEmpty() && expandedKeys.size >= sortedGroups.size
    val bottomInset = LocalBottomOverlayPadding.current
    val allMembersById = remember(groups) { groups.flatMap { it.members }.associateBy { it.id } }
    val selectedBytes = remember(selectedForDelete, allMembersById) {
        selectedForDelete.sumOf { id -> (allMembersById[id]?.sizeBytes ?: 0L).coerceAtLeast(0L) }
    }

    fun triggerDeleteSelected() {
        val targets = selectedForDelete.mapNotNull { allMembersById[it] }
        val mediaStoreTargets = targets.filter { it.uri.startsWith("content://") }
        if (mediaStoreTargets.isEmpty()) {
            vm.snackbarMessage.value = "No removable MediaStore files selected"
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val uris = mediaStoreTargets.map { Uri.parse(it.uri) }
            runCatching {
                pendingDeleteIds = mediaStoreTargets.map { it.id }
                val pi = MediaStore.createDeleteRequest(ctx.contentResolver, uris)
                deleteRequestLauncher.launch(IntentSenderRequest.Builder(pi.intentSender).build())
            }.onFailure { err ->
                pendingDeleteIds = emptyList()
                vm.snackbarMessage.value = err.message ?: "Unable to request OS file deletion"
            }
        } else {
            pendingDeleteIds = mediaStoreTargets.map { it.id }
            legacyConfirmOpen = true
        }
    }

    Column(Modifier.fillMaxSize()) {
        PulseTopBar(
            title = "Duplicate Finder",
            subtitle = if (!loading && groups.isNotEmpty()) {
                "${groups.size} groups · ${formatBytesMb(totalReclaimableBytes)} reclaimable"
            } else {
                null
            },
            onBack = { vm.back() }
        )

        if (loading) {
            Box(Modifier.fillMaxSize(), Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else if (sortedGroups.isEmpty()) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        Icons.Rounded.CheckCircle,
                        null,
                        tint = Pulse.Mint,
                        modifier = Modifier.size(52.dp)
                    )
                    Text(
                        "No duplicate songs found",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Your library is clean. Every title and artist combination is unique.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = bottomInset + 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item(key = "summary_header") {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        shape = RoundedCornerShape(18.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    ) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Rounded.Info,
                                        null,
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        "${groups.size} duplicate groups · $totalCopies total files",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        "Reclaimable space: ${formatBytesMb(totalReclaimableBytes)} (keeping best copy)",
                                        style = MaterialTheme.typography.bodySmall.merge(TabularMono),
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                            Text(
                                "Review paths, auto-select lower-quality copies, or delete chosen files via system permission.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        val lowerQualityIds = groups.flatMap { g ->
                                            val best = selectBestMemberId(g.members)
                                            g.members.filter { it.id != best && it.uri.startsWith("content://") }.map { it.id }
                                        }.toSet()
                                        selectedForDelete = lowerQualityIds
                                        expandedKeys = sortedGroups.map(::groupKey).toSet()
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 48.dp)
                                        .testTag("dup_auto_select_lower_quality")
                                ) {
                                    Icon(Icons.Rounded.AutoFixHigh, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Select Lower-Quality", maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                                if (selectedForDelete.isNotEmpty()) {
                                    Button(
                                        onClick = { triggerDeleteSelected() },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.error,
                                            contentColor = MaterialTheme.colorScheme.onError
                                        ),
                                        modifier = Modifier
                                            .heightIn(min = 48.dp)
                                            .testTag("dup_delete_selected_button")
                                    ) {
                                        Icon(Icons.Rounded.DeleteOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("Delete (${selectedForDelete.size} · ${formatBytesMb(selectedBytes)})")
                                    }
                                }
                            }
                            if (selectedForDelete.isNotEmpty()) {
                                TextButton(
                                    onClick = { selectedForDelete = emptySet() },
                                    modifier = Modifier.align(Alignment.End)
                                ) {
                                    Text("Clear selection")
                                }
                            }
                        }
                    }
                }

                item(key = "sort_chips") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        DuplicateSortMode.entries.forEach { mode ->
                            val selected = sortMode == mode
                            FilterChip(
                                selected = selected,
                                onClick = { sortMode = mode },
                                label = { Text(mode.label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                        }
                        AssistChip(
                            onClick = {
                                expandedKeys = if (allExpanded) {
                                    emptySet()
                                } else {
                                    sortedGroups.map(::groupKey).toSet()
                                }
                            },
                            label = { Text(if (allExpanded) "Collapse All" else "Expand All") },
                            leadingIcon = {
                                Icon(
                                    if (allExpanded) Icons.Rounded.UnfoldLess else Icons.Rounded.UnfoldMore,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                labelColor = MaterialTheme.colorScheme.onSurface,
                                leadingIconContentColor = MaterialTheme.colorScheme.primary
                            ),
                            border = AssistChipDefaults.assistChipBorder(
                                enabled = true,
                                borderColor = MaterialTheme.colorScheme.outlineVariant
                            ),
                            modifier = Modifier.testTag("dup_expand_all_chip")
                        )
                    }
                }

                items(sortedGroups, key = { groupKey(it) }) { group ->
                    val gKey = groupKey(group)
                    val isExpanded = gKey in expandedKeys
                    val bestId = remember(group.members) { selectBestMemberId(group.members) }
                    val bestTrack = remember(group.members, bestId) { group.members.firstOrNull { it.id == bestId } }
                    val extraBytes = remember(group.members) { groupReclaimableBytes(group.members) }

                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    ) {
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        expandedKeys = if (isExpanded) expandedKeys - gKey else expandedKeys + gKey
                                    }
                                    .padding(horizontal = 4.dp, vertical = 4.dp)
                                    .testTag("dup_group_header"),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        text = group.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    val bestCodec = bestTrack?.codec?.takeIf { it.isNotBlank() }?.uppercase(Locale.US)
                                    val subLine = if (bestCodec != null) {
                                        "${group.artist} · Best: $bestCodec"
                                    } else {
                                        group.artist
                                    }
                                    Text(
                                        text = subLine,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Spacer(Modifier.width(8.dp))
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primaryContainer
                                ) {
                                    Text(
                                        "${group.count} · +${formatBytesMb(extraBytes)}",
                                        style = MaterialTheme.typography.labelSmall.merge(TabularMono),
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                                Spacer(Modifier.width(4.dp))
                                Icon(
                                    imageVector = if (isExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                                    contentDescription = if (isExpanded) "Collapse duplicate group" else "Expand duplicate group",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            AnimatedVisibility(visible = isExpanded) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    group.members.forEach { track ->
                                val isBest = track.id == bestId
                                val qualityBits = buildList {
                                    add(formatTime(track.durationMs))
                                    add(formatBytesMb(track.sizeBytes))
                                    if (track.codec.isNotBlank()) add(track.codec.uppercase(Locale.US))
                                    val kbps = if (track.bitrate > 10_000) track.bitrate / 1000 else track.bitrate
                                    if (kbps > 0) add("${kbps}kbps")
                                }.joinToString(" · ")

                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    border = if (isBest) {
                                        BorderStroke(1.dp, Pulse.Mint.copy(alpha = 0.45f))
                                    } else {
                                        null
                                    }
                                ) {
                                    Column(
                                        modifier = Modifier.padding(start = 12.dp, end = 6.dp, top = 10.dp, bottom = 6.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Checkbox(
                                                checked = track.id in selectedForDelete,
                                                onCheckedChange = { checked ->
                                                    selectedForDelete = if (checked) {
                                                        selectedForDelete + track.id
                                                    } else {
                                                        selectedForDelete - track.id
                                                    }
                                                },
                                                modifier = Modifier.testTag("dup_checkbox_${track.id}")
                                            )
                                            Text(
                                                text = track.displayName,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f)
                                            )
                                            if (isBest) {
                                                Surface(
                                                    shape = CircleShape,
                                                    color = Pulse.Mint.copy(alpha = 0.16f)
                                                ) {
                                                    Text(
                                                        "Best Copy",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Pulse.Mint,
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                        }

                                        Text(
                                            text = qualityBits,
                                            style = MaterialTheme.typography.labelSmall.merge(TabularMono),
                                            color = MaterialTheme.colorScheme.primary
                                        )

                                        Text(
                                            text = track.folderPath,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.End
                                        ) {
                                            IconButton(
                                                modifier = Modifier
                                                    .size(48.dp)
                                                    .testTag("dup_play_button"),
                                                onClick = {
                                                    vm.player.playTrack(
                                                        Track(
                                                            id = track.id,
                                                            uri = track.uri,
                                                            title = track.title,
                                                            artist = track.artist,
                                                            album = track.album,
                                                            durationMs = track.durationMs,
                                                            folder = track.folderPath,
                                                            isFavorite = false
                                                        )
                                                    )
                                                }
                                            ) {
                                                Icon(Icons.Rounded.PlayArrow, "Play track", tint = MaterialTheme.colorScheme.primary)
                                            }

                                            IconButton(
                                                modifier = Modifier
                                                    .size(48.dp)
                                                    .testTag("dup_copy_button"),
                                                onClick = {
                                                    val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                    val clip = ClipData.newPlainText("track_path", track.dataPath ?: track.folderPath)
                                                    cm.setPrimaryClip(clip)
                                                    vm.snackbarMessage.value = "Path copied to clipboard"
                                                }
                                            ) {
                                                Icon(Icons.Rounded.ContentCopy, "Copy path", tint = MaterialTheme.colorScheme.onSurface)
                                            }

                                            if (track.folderPath.isNotBlank()) {
                                                IconButton(
                                                    modifier = Modifier
                                                        .size(48.dp)
                                                        .testTag("dup_exclude_button"),
                                                    onClick = {
                                                        vm.excludeFolder(track.folderPath)
                                                        groups = groups.mapNotNull { g ->
                                                            val remaining = g.members.filterNot {
                                                                it.folderPath.equals(track.folderPath, ignoreCase = true)
                                                            }
                                                            if (remaining.size > 1) g.copy(count = remaining.size, members = remaining) else null
                                                        }
                                                    }
                                                ) {
                                                    Icon(Icons.Rounded.FolderOff, "Exclude folder", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                                }
                                            }

                                            IconButton(
                                                modifier = Modifier
                                                    .size(48.dp)
                                                    .testTag("dup_share_button"),
                                                onClick = {
                                                    runCatching {
                                                        val share = Intent(Intent.ACTION_SEND).apply {
                                                            type = track.mimeType ?: "audio/*"
                                                            putExtra(Intent.EXTRA_STREAM, Uri.parse(track.uri))
                                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                        }
                                                        ctx.startActivity(Intent.createChooser(share, "Share ${track.title}"))
                                                    }.onFailure { err ->
                                                        ErrorDispatcher.post(
                                                            ErrorCategory.STORAGE,
                                                            "Unable to share track",
                                                            err.message ?: "No compatible app found to share track.",
                                                            err.stackTraceToString()
                                                        )
                                                        vm.snackbarMessage.value = "Unable to share track"
                                                    }
                                                }
                                            ) {
                                                Icon(Icons.Rounded.Share, "Share track", tint = MaterialTheme.colorScheme.onSurface)
                                            }
                                        }
                                    }
                                }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (legacyConfirmOpen) {
        AlertDialog(
            onDismissRequest = {
                legacyConfirmOpen = false
                pendingDeleteIds = emptyList()
            },
            title = { Text("Delete ${pendingDeleteIds.size} duplicate file(s)?") },
            text = { Text("This permanently deletes the selected audio files from device storage.") },
            confirmButton = {
                Button(
                    onClick = {
                        val targets = pendingDeleteIds.mapNotNull { allMembersById[it] }
                        val deletedIds = mutableListOf<String>()
                        targets.forEach { t ->
                            val rows = runCatching { ctx.contentResolver.delete(Uri.parse(t.uri), null, null) }.getOrDefault(0)
                            if (rows > 0) deletedIds += t.id
                        }
                        legacyConfirmOpen = false
                        pendingDeleteIds = emptyList()
                        if (deletedIds.isNotEmpty()) {
                            vm.removeDeletedDuplicates(deletedIds) {
                                selectedForDelete = selectedForDelete - deletedIds.toSet()
                                refreshNonce++
                            }
                        } else {
                            vm.snackbarMessage.value = "Could not delete selected files on this OS version"
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        legacyConfirmOpen = false
                        pendingDeleteIds = emptyList()
                    }
                ) { Text("Cancel") }
            }
        )
    }
}
