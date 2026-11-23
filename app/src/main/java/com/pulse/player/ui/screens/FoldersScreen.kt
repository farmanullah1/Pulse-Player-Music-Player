package com.pulse.player.ui.screens

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.PlaylistPlay
import androidx.compose.material.icons.rounded.AccountTree
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.FolderOff
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pulse.player.ui.components.LocalBottomOverlayPadding
import com.pulse.player.ui.components.TrackRow
import com.pulse.player.ui.components.UnifiedFilterBar
import com.pulse.player.ui.theme.TabularMono
import com.pulse.player.ui.viewmodel.MainViewModel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

@Immutable
data class FolderItem(val path: String, val name: String, val count: Int)

@Composable
fun FoldersScreen(vm: MainViewModel) {
    val folders by vm.folders.collectAsStateWithLifecycle()
    val excludedFolders by vm.excludedFolders.collectAsStateWithLifecycle()
    val treeMode by vm.folderViewTreeMode.collectAsStateWithLifecycle()
    val treeState by vm.folderTreeState.collectAsStateWithLifecycle()
    val selected by vm.selected.collectAsStateWithLifecycle()
    val selectMode by vm.selectMode.collectAsStateWithLifecycle()
    val playbackStatus by remember(vm) {
        vm.player.state.map { it.mediaId to it.isPlaying }.distinctUntilChanged()
    }.collectAsStateWithLifecycle(initialValue = null to false)
    val (currentMediaId, isPlaying) = playbackStatus
    val bottomPad = LocalBottomOverlayPadding.current
    var pendingExclude by remember { mutableStateOf<FolderItem?>(null) }

    val canNavigateUp = treeMode &&
        treeState.currentPath.isNotBlank() &&
        !treeState.currentPath.equals(treeState.rootPath, ignoreCase = true)

    BackHandler(enabled = canNavigateUp) {
        vm.navigateFolderTreeUp()
    }

    val safLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) vm.addSafFolder(uri)
    }

    LazyColumn(
        state = vm.lazyListStateFor("folders"),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 12.dp, bottom = bottomPad),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item(contentType = "header") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (treeMode) {
                            "${treeState.subfolders.size} Folders · ${treeState.directTracks.size} Songs"
                        } else {
                            "${folders.size} Folders"
                        },
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Button(
                        onClick = { safLauncher.launch(null) },
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Rounded.FolderOpen, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Add Folder", fontWeight = FontWeight.Bold)
                    }
                }
                UnifiedFilterBar(
                    vm = vm,
                    isTrackTab = false,
                    trailingContent = {
                        FilterChip(
                            selected = treeMode,
                            onClick = { vm.setFolderViewTreeMode(!treeMode) },
                            leadingIcon = { Icon(Icons.Rounded.AccountTree, null, Modifier.size(16.dp)) },
                            label = { Text(if (treeMode) "Tree" else "Flat") },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                labelColor = MaterialTheme.colorScheme.onSurface,
                                iconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = treeMode,
                                borderColor = MaterialTheme.colorScheme.outlineVariant,
                                selectedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                            ),
                            modifier = Modifier.testTag("folders_tree_mode_chip")
                        )
                        val hasExcluded = excludedFolders.isNotEmpty()
                        FilterChip(
                            selected = hasExcluded,
                            onClick = { vm.go(Screen.ExcludedFolders) },
                            leadingIcon = { Icon(Icons.Rounded.FolderOff, null, Modifier.size(16.dp)) },
                            label = {
                                Text(if (!hasExcluded) "Excluded" else "Excluded (${excludedFolders.size})")
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                labelColor = MaterialTheme.colorScheme.onSurface,
                                iconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = hasExcluded,
                                borderColor = MaterialTheme.colorScheme.outlineVariant,
                                selectedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                            )
                        )
                    }
                )
                if (treeMode && treeState.breadcrumbs.isNotEmpty()) {
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("folder_breadcrumbs_row"),
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        itemsIndexed(treeState.breadcrumbs, key = { idx, b -> "${idx}_${b.path}" }) { idx, crumb ->
                            val isLast = idx == treeState.breadcrumbs.lastIndex
                            FilterChip(
                                selected = isLast,
                                onClick = { vm.navigateFolderTree(crumb.path) },
                                label = {
                                    Text(
                                        text = crumb.label,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    labelColor = MaterialTheme.colorScheme.onSurface,
                                    selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            )
                            if (!isLast) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        if (treeMode) {
            if (treeState.subfolders.isEmpty() && treeState.directTracks.isEmpty()) {
                item(contentType = "empty_tree") {
                    EmptyFoldersCard(safLauncherClick = { safLauncher.launch(null) })
                }
            } else {
                items(treeState.subfolders, key = { "tree_${it.path}" }, contentType = { "folder" }) { node ->
                    val folderItem = FolderItem(path = node.path, name = node.name, count = node.totalCount)
                    FolderNodeRow(
                        folder = folderItem,
                        subtitle = if (node.directCount > 0 && node.directCount != node.totalCount) {
                            "${node.totalCount} songs (${node.directCount} direct) · ${node.path}"
                        } else {
                            "${node.totalCount} songs · ${node.path}"
                        },
                        onClick = { vm.navigateFolderTree(node.path) },
                        onPlay = { vm.playFolder(node.path, shuffle = false, recursive = true) },
                        onShuffle = { vm.playFolder(node.path, shuffle = true, recursive = true) },
                        onQueueNext = { vm.queueFolderNext(node.path, recursive = true) },
                        onExclude = { pendingExclude = folderItem }
                    )
                }
                itemsIndexed(
                    treeState.directTracks,
                    key = { _, track -> "tree_track_${track.id}" },
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
                                vm.playQueue(treeState.directTracks, index)
                            }
                        },
                        onLongClick = { vm.toggleSelect(track.id) },
                        onMore = { vm.openTrackMenu(track) }
                    )
                }
            }
        } else {
            if (folders.isEmpty()) {
                item(contentType = "empty") {
                    EmptyFoldersCard(safLauncherClick = { safLauncher.launch(null) })
                }
            } else {
                items(folders, key = { it.path }, contentType = { "folder" }) { folder ->
                    FolderNodeRow(
                        folder = folder,
                        subtitle = "${folder.count} songs · ${folder.path}",
                        onClick = { vm.go(Screen.Detail("folder", folder.path)) },
                        onPlay = { vm.playFolder(folder.path, shuffle = false) },
                        onShuffle = { vm.playFolder(folder.path, shuffle = true) },
                        onQueueNext = { vm.queueFolderNext(folder.path) },
                        onExclude = { pendingExclude = folder }
                    )
                }
            }
        }
    }

    pendingExclude?.let { target ->
        AlertDialog(
            onDismissRequest = { pendingExclude = null },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            title = { Text("Exclude \"${target.name}\"?") },
            text = { Text("Tracks in ${target.path} will be hidden during future scans. You can restore them anytime from Excluded Folders.") },
            confirmButton = {
                Button(
                    onClick = {
                        vm.excludeFolder(target.path)
                        pendingExclude = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) { Text("Exclude") }
            },
            dismissButton = {
                TextButton(onClick = { pendingExclude = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun EmptyFoldersCard(safLauncherClick: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp, horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            Icons.Rounded.FolderOpen,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
            modifier = Modifier.size(48.dp)
        )
        Text(
            "No music folders yet",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            "Tap Add Folder above to include audio stored anywhere on your device.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun FolderNodeRow(
    folder: FolderItem,
    subtitle: String,
    onClick: () -> Unit,
    onPlay: () -> Unit,
    onShuffle: () -> Unit,
    onQueueNext: () -> Unit,
    onExclude: () -> Unit
) {
    var menuOpen by remember(folder.path) { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.Folder, null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(28.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = folder.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.merge(TabularMono),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Box {
            IconButton(
                onClick = { menuOpen = true },
                modifier = Modifier
                    .size(48.dp)
                    .testTag("folder_menu_${folder.name}")
            ) {
                Icon(
                    imageVector = Icons.Rounded.MoreVert,
                    contentDescription = "Folder options for ${folder.name}",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            DropdownMenu(
                expanded = menuOpen,
                onDismissRequest = { menuOpen = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Play Folder") },
                    leadingIcon = { Icon(Icons.Rounded.PlayArrow, null) },
                    onClick = {
                        menuOpen = false
                        onPlay()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Shuffle Folder") },
                    leadingIcon = { Icon(Icons.Rounded.Shuffle, null) },
                    onClick = {
                        menuOpen = false
                        onShuffle()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Queue Next") },
                    leadingIcon = { Icon(Icons.AutoMirrored.Rounded.PlaylistPlay, null) },
                    onClick = {
                        menuOpen = false
                        onQueueNext()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Exclude Folder") },
                    leadingIcon = { Icon(Icons.Rounded.FolderOff, null) },
                    onClick = {
                        menuOpen = false
                        onExclude()
                    }
                )
            }
        }
    }
}
