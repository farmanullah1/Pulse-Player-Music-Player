package com.pulse.player.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.FolderOff
import androidx.compose.material.icons.rounded.HeartBroken
import androidx.compose.material.icons.rounded.RestoreFromTrash
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pulse.player.ui.components.LocalBottomOverlayPadding
import com.pulse.player.ui.components.PulseTopBar
import com.pulse.player.ui.theme.Pulse
import com.pulse.player.ui.theme.TabularMono
import com.pulse.player.ui.viewmodel.MainViewModel
import java.util.Locale

private val NOISY_FOLDER_HINTS = listOf("whatsapp", "record", "ringtone", "alarm", "notification", "voice", "call")

@Composable
fun ExcludedFoldersScreen(vm: MainViewModel) {
    val excluded by vm.excludedFolders.collectAsStateWithLifecycle()
    val hiddenFavorites by vm.hiddenFavorites.collectAsStateWithLifecycle()
    val activeFolders by vm.folders.collectAsStateWithLifecycle()
    val sorted = remember(excluded) { excluded.sortedBy { it.lowercase(Locale.US) } }
    val bottomPad = LocalBottomOverlayPadding.current

    var showClearConfirm by remember { mutableStateOf(false) }
    var showExcludePicker by remember { mutableStateOf(false) }

    BackHandler { vm.back() }

    Column(Modifier.fillMaxSize()) {
        PulseTopBar(
            title = "Excluded Folders",
            subtitle = buildString {
                append(if (sorted.isEmpty()) "No folders hidden" else "${sorted.size} hidden from scans")
                if (hiddenFavorites.isNotEmpty()) append(" · ${hiddenFavorites.size} hidden fav(s)")
            },
            onBack = { vm.back() },
            actions = {
                if (activeFolders.isNotEmpty()) {
                    IconButton(
                        onClick = { showExcludePicker = true },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("add_excluded_folder_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = "Exclude a folder",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                if (sorted.isNotEmpty()) {
                    TextButton(onClick = { showClearConfirm = true }) {
                        Text("Clear All", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        )

        if (sorted.isEmpty() && hiddenFavorites.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically)
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.FolderOff,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                }
                Text(
                    text = "No excluded folders",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Exclude noisy folders like voice memos, WhatsApp audio, or ringtones to keep your music library clean.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                if (activeFolders.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Button(
                        onClick = { showExcludePicker = true },
                        shape = CircleShape,
                        modifier = Modifier.heightIn(min = 48.dp)
                    ) {
                        Icon(Icons.Rounded.Add, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Choose Folder to Exclude", fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = bottomPad + 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (hiddenFavorites.isNotEmpty()) {
                    item(key = "hidden_favorites_banner") {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            shape = RoundedCornerShape(20.dp),
                            border = BorderStroke(1.dp, Pulse.Coral.copy(alpha = 0.45f))
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(RoundedCornerShape(11.dp))
                                            .background(Pulse.Coral.copy(alpha = 0.16f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Rounded.HeartBroken, null, tint = Pulse.Coral, modifier = Modifier.size(20.dp))
                                    }
                                    Spacer(Modifier.width(12.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(
                                            "Excluded & Hidden Favorites (${hiddenFavorites.size})",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            "Favorited songs hidden by an excluded folder or missing from storage.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                hiddenFavorites.forEach { item ->
                                    val t = item.track
                                    val matchedFolder = sorted.firstOrNull { ex ->
                                        t.folderPath.startsWith(ex, ignoreCase = true)
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(14.dp),
                                        color = MaterialTheme.colorScheme.surface
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 12.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(Modifier.weight(1f)) {
                                                Text(
                                                    text = t.title.ifBlank { t.displayName },
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = if (matchedFolder != null) {
                                                        "${t.artist} · in ${matchedFolder.substringAfterLast('/')}"
                                                    } else {
                                                        "${t.artist} · File missing on storage"
                                                    },
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                            Spacer(Modifier.width(8.dp))
                                            if (matchedFolder != null) {
                                                OutlinedButton(
                                                    onClick = { vm.unexcludeFolder(matchedFolder) },
                                                    shape = RoundedCornerShape(12.dp),
                                                    modifier = Modifier.heightIn(min = 48.dp)
                                                ) {
                                                    Text("Restore", style = MaterialTheme.typography.labelMedium)
                                                }
                                            } else {
                                                IconButton(
                                                    onClick = { vm.toggleFavorite(t.id) },
                                                    modifier = Modifier.size(48.dp)
                                                ) {
                                                    Icon(
                                                        Icons.Rounded.FavoriteBorder,
                                                        contentDescription = "Remove missing favorite",
                                                        tint = MaterialTheme.colorScheme.error
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                if (sorted.isNotEmpty()) {
                    item(key = "excluded_section_label") {
                        Text(
                            text = "Hidden Folders (${sorted.size})",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }

                items(sorted, key = { it }) { folderPath ->
                    val hiddenFavCount = hiddenFavorites.count {
                        it.track.folderPath.startsWith(folderPath, ignoreCase = true)
                    }
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.FolderOff,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(Modifier.width(14.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = folderPath.substringAfterLast('/').ifBlank { folderPath },
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = folderPath,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (hiddenFavCount > 0) {
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = "$hiddenFavCount hidden favourite(s) inside",
                                        style = MaterialTheme.typography.labelSmall.merge(TabularMono),
                                        color = Pulse.Coral,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                            Spacer(Modifier.width(8.dp))
                            OutlinedButton(
                                onClick = { vm.unexcludeFolder(folderPath) },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.heightIn(min = 48.dp)
                            ) {
                                Icon(Icons.Rounded.RestoreFromTrash, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Restore")
                            }
                        }
                    }
                }
            }
        }
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text("Restore All Excluded Folders?") },
            text = {
                Text(
                    "All ${sorted.size} excluded folder(s) will be included in the next library scan.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showClearConfirm = false
                        vm.clearExcludedFolders()
                    }
                ) {
                    Text("Restore All", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) {
                    Text("Cancel")
                }
            },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    }

    if (showExcludePicker) {
        val candidates = remember(activeFolders, sorted) {
            activeFolders
                .filterNot { f -> sorted.any { ex -> f.path.equals(ex, ignoreCase = true) } }
                .sortedWith(
                    compareByDescending<FolderItem> { item ->
                        NOISY_FOLDER_HINTS.any { hint -> item.path.contains(hint, ignoreCase = true) }
                    }.thenBy { it.name.lowercase(Locale.US) }
                )
        }
        AlertDialog(
            onDismissRequest = { showExcludePicker = false },
            title = { Text("Exclude a Folder") },
            text = {
                if (candidates.isEmpty()) {
                    Text(
                        "No active library folders available to exclude.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 360.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(candidates, key = { it.path }) { folder ->
                            val isSuggested = NOISY_FOLDER_HINTS.any { folder.path.contains(it, ignoreCase = true) }
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surface,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable {
                                        vm.excludeFolder(folder.path)
                                        showExcludePicker = false
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Rounded.Folder,
                                        null,
                                        tint = if (isSuggested) Pulse.Coral else MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(Modifier.width(10.dp))
                                    Column(Modifier.weight(1f)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                folder.name.ifBlank { folder.path },
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f, fill = false)
                                            )
                                            if (isSuggested) {
                                                Surface(
                                                    shape = CircleShape,
                                                    color = Pulse.Coral.copy(alpha = 0.16f)
                                                ) {
                                                    Text(
                                                        "Suggested",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = Pulse.Coral,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                                                    )
                                                }
                                            }
                                        }
                                        Text(
                                            "${folder.count} tracks · ${folder.path}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showExcludePicker = false }) {
                    Text("Close")
                }
            },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    }
}
