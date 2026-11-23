package com.pulse.player.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.SettingsBackupRestore
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.Unarchive
import androidx.compose.material.icons.rounded.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.pulse.player.data.backup.BackupOperationState
import com.pulse.player.data.backup.CompatRestorePreview
import com.pulse.player.data.backup.CompatRestoreReport
import com.pulse.player.data.backup.ExportCats
import com.pulse.player.data.backup.StagedBackup
import com.pulse.player.ui.theme.Pulse
import com.pulse.player.ui.theme.TabularMono
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.Date
import java.util.Locale

internal fun LazyListScope.quickTransferTabItems(
    favCount: Int,
    archiveOp: BackupOperationState,
    onShowExportPreflight: () -> Unit,
    onExportCustomFolder: () -> Unit,
    onExportJsonOnly: () -> Unit,
    onImportAutoDetect: () -> Unit,
    onPickPortableFolder: () -> Unit,
    onImportJson: () -> Unit,
    onChooseMusicFolder: () -> Unit
) {
    item(key = "quick_transfer_overview") {
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        ) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(13.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Rounded.SwapHoriz,
                            null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            "Portable Favourites + Music",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            "pulseplayer.favorites v1 JSON · $favCount saved",
                            style = MaterialTheme.typography.labelSmall.merge(TabularMono),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Text(
                    text = "Creates Download/Pulse, saves the pulseplayer.favorites v1 JSON, and copies each favourite audio file beside it (reusing already-copied files). Move that folder to a new phone, then tap Auto-Detect.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = "Send from this device",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Button(
                    onClick = onShowExportPreflight,
                    enabled = favCount > 0 && !archiveOp.inProgress,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("portable_export_button"),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Icon(Icons.Rounded.Upload, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Export to Download/Pulse ($favCount)", fontWeight = FontWeight.Bold)
                }

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onExportCustomFolder,
                        enabled = favCount > 0 && !archiveOp.inProgress
                    ) {
                        Text(
                            "Export to custom folder…",
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    TextButton(
                        onClick = onExportJsonOnly,
                        enabled = favCount > 0 && !archiveOp.inProgress
                    ) {
                        Text(
                            "Export JSON only",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))

                Text(
                    text = "Receive on this device",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Button(
                    onClick = onImportAutoDetect,
                    enabled = !archiveOp.inProgress,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("portable_auto_detect_button"),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                ) {
                    Icon(Icons.Rounded.SettingsBackupRestore, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Auto-Detect from Download/Pulse", fontWeight = FontWeight.Bold)
                }

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = onPickPortableFolder,
                        enabled = !archiveOp.inProgress,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("portable_import_button"),
                        shape = CircleShape
                    ) {
                        Icon(Icons.Rounded.FolderOpen, null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(6.dp))
                        Text("Pick Folder", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = onImportJson,
                        enabled = !archiveOp.inProgress,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("legacy_import_button_secondary"),
                        shape = CircleShape
                    ) {
                        Icon(Icons.Rounded.Download, null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(6.dp))
                        Text("Import JSON", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    item(key = "dedicated_music_folder") {
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        ) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(11.dp))
                            .background(Pulse.Mint.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.FolderOpen, null, tint = Pulse.Mint, modifier = Modifier.size(20.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "Dedicated Music Folder",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Text(
                    text = "Link a folder (SAF) on internal storage or SD card where copied music lives. Pulse Music Player indexes it for offline playback.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedButton(
                    onClick = onChooseMusicFolder,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("choose_folder_button"),
                    shape = CircleShape
                ) {
                    Text("Choose Music Folder", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

internal fun LazyListScope.fullArchiveTabItems(
    lastBackupJson: String?,
    allTracksCount: Int,
    favCount: Int,
    allPlaylistsCount: Int,
    archiveOp: BackupOperationState,
    compatExportCats: ExportCats,
    compatExportAsZip: Boolean,
    onRestoreFullBackup: () -> Unit,
    onToggleExportCategory: (String) -> Unit,
    onSetExportAsZip: (Boolean) -> Unit,
    onCreateFullBackup: (String) -> Unit,
    onInspectSampleReference: () -> Unit
) {
    if (!lastBackupJson.isNullOrBlank()) {
        item(key = "last_verified_backup") {
            val lastRecord = runCatching { JSONObject(lastBackupJson) }.getOrNull()
            if (lastRecord != null) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, Pulse.Mint.copy(alpha = 0.35f))
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.History, null, tint = Pulse.Mint, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Last Verified Backup",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        val stamp = lastRecord.optString("backupTime", "")
                        val sizeBytes = lastRecord.optLong("sizeBytes", 0L)
                        val songs = lastRecord.optInt("songs", 0)
                        val lists = lastRecord.optInt("playlists", 0)
                        Text(
                            "Backup ID: $stamp (${sizeBytes / 1024} KB)",
                            style = MaterialTheme.typography.bodySmall.merge(TabularMono),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "Included: $songs songs with paths · $lists playlists",
                            style = MaterialTheme.typography.labelSmall.merge(TabularMono),
                            color = Pulse.Mint
                        )
                    }
                }
            }
        }
    }

    item(key = "full_archive_card") {
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        ) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(13.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Rounded.SettingsBackupRestore,
                            null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            "Full Library Archive",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            "Reference ZIP / .wav archive format",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Text(
                    text = "Create or restore a full archive saving favourites, playlists, listening history, sort settings, and excluded folders.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedButton(
                    onClick = onRestoreFullBackup,
                    enabled = !archiveOp.inProgress,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("compat_restore_button"),
                    shape = CircleShape
                ) {
                    Icon(Icons.Rounded.Download, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Text("Restore Full Backup (SAF)", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))

                Text(
                    "Categories to Include:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    CompatCategoryCheckbox("Music tags ($allTracksCount available songs)", compatExportCats.tags) {
                        onToggleExportCategory("tags")
                    }
                    CompatCategoryCheckbox("Playlists & Favourites ($favCount favourites, $allPlaylistsCount playlists)", compatExportCats.playlists) {
                        onToggleExportCategory("playlists")
                    }
                    CompatCategoryCheckbox("Listening history & monthly snapshots", compatExportCats.recap) {
                        onToggleExportCategory("recap")
                    }
                    CompatCategoryCheckbox("Sort settings", compatExportCats.sort) {
                        onToggleExportCategory("sort")
                    }
                    CompatCategoryCheckbox("Scan & excluded folder settings", compatExportCats.scan) {
                        onToggleExportCategory("scan")
                    }
                }

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Save as .zip instead", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                        Text(
                            if (compatExportAsZip) "Will create standard .zip" else "Default .wav (reference naming)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = compatExportAsZip,
                        onCheckedChange = onSetExportAsZip,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.primary,
                            checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                }

                if (!compatExportAsZip) {
                    Text(
                        "Notice: A .wav-named backup inside your Music folder can appear as an audio file in third-party media scanners. Keep backups in Documents or Backups.",
                        style = MaterialTheme.typography.labelSmall,
                        color = Pulse.Coral
                    )
                }

                Button(
                    onClick = {
                        val stamp = SimpleDateFormat("yyyyMMdd_HHmmssSSS", Locale.US).format(Date())
                        val ext = if (compatExportAsZip) "zip" else "wav"
                        onCreateFullBackup("ManualBackup_$stamp.$ext")
                    },
                    enabled = !archiveOp.inProgress,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("compat_create_backup_button"),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Icon(Icons.Rounded.Upload, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Create Full Backup", fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    item(key = "bundled_reference_fixture") {
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        ) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Info, null, tint = Pulse.Mint, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Bundled Reference Archive Fixture",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Text(
                    "Inspect the included reference archive: ManualBackup_20261028_172204175.wav (13,509 songs, 41 playlists, settings, history).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedButton(
                    onClick = onInspectSampleReference,
                    enabled = !archiveOp.inProgress,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("compat_inspect_sample_button"),
                    shape = CircleShape
                ) {
                    Icon(Icons.Rounded.Unarchive, null, modifier = Modifier.size(18.dp), tint = Pulse.Mint)
                    Spacer(Modifier.width(8.dp))
                    Text("Inspect Sample Reference Archive", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
internal fun BackupRestorePreviewDialog(
    preview: CompatRestorePreview,
    selectedCategories: Set<String>,
    replaceMode: Boolean,
    showReplaceConfirmDialog: Boolean,
    onDismissPreview: () -> Unit,
    onToggleCategory: (String) -> Unit,
    onSetReplaceMode: (Boolean) -> Unit,
    onRequestReplaceConfirm: () -> Unit,
    onDismissReplaceConfirm: () -> Unit,
    onExecuteRestore: (StagedBackup, Set<String>, Boolean) -> Unit
) {
    Dialog(onDismissRequest = onDismissPreview) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Unarchive, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                    Spacer(Modifier.width(12.dp))
                    Text("Backup Restore Preview", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                }

                val backupTime = preview.staged.meta["backupTime"] as? String ?: "Unknown time"
                val device = preview.staged.meta["deviceId"] as? String ?: "Unknown device"
                val ver = preview.staged.meta["versionName"] as? String ?: "1.0.0"
                Text("Backup Date: $backupTime", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Source: Device $device (v$ver)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            "Resolved: ${preview.favResolved} favourites, ${preview.playlistResolved} playlist tracks, ${preview.eventsResolved} play events",
                            style = MaterialTheme.typography.bodySmall,
                            color = Pulse.Mint,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Matches: exact ${preview.stats.tier[1]}, storage-rel ${preview.stats.tier[2]}, folder ${preview.stats.tier[3]}, tags ${preview.stats.tier[4]}, name ${preview.stats.tier[5]}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (preview.stats.ambiguous > 0 || preview.stats.missing > 0) {
                            Text(
                                "Unmatched: ${preview.stats.ambiguous} ambiguous, ${preview.stats.missing} not found on this device",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }

                Text("Categories to restore:", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                Column {
                    CompatCategoryCheckbox(
                        label = "Playlists & Favourites (${preview.staged.playlists.size} playlists)",
                        selected = "isBackupPlaylists" in selectedCategories,
                        onToggle = { onToggleCategory("isBackupPlaylists") }
                    )
                    CompatCategoryCheckbox(
                        label = "Listening History (${preview.staged.events.size} records)",
                        selected = "isBackupMusicRecap" in selectedCategories,
                        onToggle = { onToggleCategory("isBackupMusicRecap") }
                    )
                    CompatCategoryCheckbox(
                        label = "Sort Settings",
                        selected = "isBackupSortSettings" in selectedCategories,
                        onToggle = { onToggleCategory("isBackupSortSettings") }
                    )
                    CompatCategoryCheckbox(
                        label = "Excluded Folders (${preview.staged.excludeDirs.size} folders)",
                        selected = "isBackupScanHideSettings" in selectedCategories,
                        onToggle = { onToggleCategory("isBackupScanHideSettings") }
                    )
                }

                Text("Conflict Mode:", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = !replaceMode,
                        onClick = { onSetReplaceMode(false) },
                        colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary)
                    )
                    Text("Merge (default)", color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.width(16.dp))
                    RadioButton(
                        selected = replaceMode,
                        onClick = { onSetReplaceMode(true) },
                        colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.error)
                    )
                    Text("Replace selected", color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.bodySmall)
                }

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(
                        onClick = onDismissPreview,
                        modifier = Modifier.testTag("compat_restore_cancel_button")
                    ) {
                        Text("Cancel", color = MaterialTheme.colorScheme.onSurface)
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (replaceMode) onRequestReplaceConfirm()
                            else onExecuteRestore(preview.staged, selectedCategories, false)
                        },
                        modifier = Modifier.testTag("compat_restore_confirm_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (replaceMode) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                            contentColor = if (replaceMode) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Text(if (replaceMode) "Replace & Restore" else "Apply Restore", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (showReplaceConfirmDialog) {
        AlertDialog(
            onDismissRequest = onDismissReplaceConfirm,
            title = { Text("Confirm Replace Restore") },
            text = {
                Text("Replace mode will clear your existing data in the selected categories (favourites, playlists, or play history) before restoring. This cannot be undone. Are you sure?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDismissReplaceConfirm()
                        onExecuteRestore(preview.staged, selectedCategories, true)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Pulse.Coral, contentColor = Color.White)
                ) {
                    Text("Yes, Replace & Restore", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissReplaceConfirm) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurface)
                }
            },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            titleContentColor = MaterialTheme.colorScheme.error,
            textContentColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
internal fun BackupRestoreReportDialog(
    report: CompatRestoreReport,
    onDismiss: () -> Unit,
    onSaveUnmatched: (List<String>) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.CheckCircle, null, tint = Pulse.Mint)
                Spacer(Modifier.width(8.dp))
                Text("Restore Complete")
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                report.lines.forEach { line ->
                    Text("• $line", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
                }
                if (report.unmatchedPaths.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "${report.unmatchedPaths.size} tracks were unmatched or missing on this device.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                    Button(
                        onClick = { onSaveUnmatched(report.unmatchedPaths) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("compat_save_unmatched_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface, contentColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Rounded.Download, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Save Unmatched List (.txt)", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary)
            ) {
                Text("Done", fontWeight = FontWeight.Bold)
            }
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
internal fun ClassicExportPreflightDialog(
    favCount: Int,
    onDismiss: () -> Unit,
    onExportAll: () -> Unit,
    onExportJsonOnly: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Backup, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(8.dp))
                Text("Quick Transfer Preflight", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Your originals will stay where they are. Pulse Music Player will create separate copies in Download/Pulse.",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "• Favourites to export: $favCount\n• Destination: Download/Pulse/\n• Snapshot metadata: pulseplayer.favorites v1 format\n• Audio files: byte-for-byte independent copies (reuses matching files)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onExportAll,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text("Export Favorites & Music", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onExportJsonOnly) {
                    Text("JSON Only (Metadata)")
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
    )
}

@Composable
private fun CompatCategoryCheckbox(
    label: String,
    selected: Boolean,
    onToggle: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onToggle)
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = selected,
            onCheckedChange = { onToggle() },
            colors = CheckboxDefaults.colors(
                checkedColor = MaterialTheme.colorScheme.primary,
                uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant,
                checkmarkColor = MaterialTheme.colorScheme.onPrimary
            )
        )
        Spacer(Modifier.width(4.dp))
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
    }
}
