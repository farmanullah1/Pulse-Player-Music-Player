package com.pulse.player.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.SettingsBackupRestore
import androidx.compose.material.icons.rounded.Unarchive
import androidx.compose.material.icons.rounded.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pulse.player.ui.theme.Pulse
import com.pulse.player.ui.viewmodel.MainViewModel
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BackupScreen(vm: MainViewModel) {
    val favCount by vm.favoriteCount.collectAsStateWithLifecycle()
    val allTracks by vm.tracks.collectAsStateWithLifecycle()
    val allPlaylists by vm.playlists.collectAsStateWithLifecycle()
    val exportStatus by vm.exportStatus.collectAsStateWithLifecycle()
    val archiveOp by vm.archiveOperation.collectAsStateWithLifecycle()
    val compatPreview by vm.compatPreview.collectAsStateWithLifecycle()
    val compatReport by vm.compatReport.collectAsStateWithLifecycle()
    val compatExportCats by vm.compatExportCats.collectAsStateWithLifecycle()
    val compatExportAsZip by vm.compatExportAsZip.collectAsStateWithLifecycle()
    val lastBackupJson by vm.lastBackupJson.collectAsStateWithLifecycle()
    val ctx = LocalContext.current

    var showReplaceConfirmDialog by remember { mutableStateOf(false) }
    var pendingUnmatchedList by remember { mutableStateOf<List<String>?>(null) }

    // Launcher for Full Backup Export
    val compatExportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        if (uri != null) {
            vm.executeCompatExport(uri)
        }
    }

    // Launcher for SAF Picker (routes JSON -> Classic, ZIP -> Full, WAV audio -> Error)
    val safBackupPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val fileName = uri.lastPathSegment?.substringAfterLast('/') ?: "backup"
            vm.smartInspect(uri, fileName)
        }
    }

    // Launcher for saving unmatched text file
    val saveUnmatchedLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/plain")
    ) { uri ->
        if (uri != null && pendingUnmatchedList != null) {
            vm.saveUnmatchedReport(uri, pendingUnmatchedList!!)
        }
    }

    // Launchers for Option A (Legacy JSON Favourites)
    val legacyExportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            vm.exportFavorites(uri)
        }
    }

    val legacyImportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val fileName = uri.lastPathSegment?.substringAfterLast('/') ?: "PulsePlayer_Favorites.json"
            vm.previewFavoritesImport(uri, fileName)
        }
    }

    val safFolderLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            vm.addSafFolder(uri)
            Toast.makeText(ctx, "Folder added to music library", Toast.LENGTH_SHORT).show()
        }
    }

    Column(Modifier.fillMaxSize()) {
        // App Bar
        Row(
            Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = vm::back,
                modifier = Modifier.testTag("backup_back_button")
            ) {
                Icon(Icons.Rounded.ArrowBack, "Back", tint = Color.White)
            }
            Spacer(Modifier.width(8.dp))
            Column {
                Text("Backup & Restore", style = MaterialTheme.typography.titleLarge, color = Color.White)
                Text("Dual System: Favourites JSON & Full Reference Backup", style = MaterialTheme.typography.labelSmall, color = Pulse.TextMid)
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Operation Progress Banner
            if (archiveOp.inProgress) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Pulse.Elevated),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.5.dp,
                                    color = Pulse.Cyan
                                )
                                Spacer(Modifier.width(12.dp))
                                Text(archiveOp.stage, color = Color.White, fontWeight = FontWeight.SemiBold)
                            }
                            if (archiveOp.progressFraction > 0f) {
                                LinearProgressIndicator(
                                    progress = { archiveOp.progressFraction },
                                    modifier = Modifier.fillMaxWidth().clip(CircleShape),
                                    color = Pulse.Cyan,
                                    trackColor = Pulse.Surface
                                )
                            }
                        }
                    }
                }
            }

            // Operation Result Banners
            if (archiveOp.completedMessage != null) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Pulse.Mint.copy(alpha = 0.15f)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Rounded.CheckCircle, null, tint = Pulse.Mint, modifier = Modifier.size(24.dp))
                            Spacer(Modifier.width(12.dp))
                            Text(
                                text = archiveOp.completedMessage.orEmpty(),
                                color = Pulse.Mint,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }

            if (archiveOp.errorMessage != null) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Pulse.Coral.copy(alpha = 0.15f)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Rounded.ErrorOutline, null, tint = Pulse.Coral, modifier = Modifier.size(24.dp))
                            Spacer(Modifier.width(12.dp))
                            Text(
                                text = archiveOp.errorMessage.orEmpty(),
                                color = Pulse.Coral,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }

            if (exportStatus != null) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Pulse.Elevated),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            text = exportStatus.orEmpty(),
                            color = Color.White,
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            // Last Verified Backup Card (if available)
            if (!lastBackupJson.isNullOrBlank()) {
                item {
                    val lastRecord = runCatching { JSONObject(lastBackupJson!!) }.getOrNull()
                    if (lastRecord != null) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Pulse.Elevated),
                            shape = RoundedCornerShape(18.dp)
                        ) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Rounded.History, null, tint = Pulse.Mint, modifier = Modifier.size(20.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Last Verified Backup", style = MaterialTheme.typography.titleSmall, color = Color.White)
                                }
                                val stamp = lastRecord.optString("backupTime", "")
                                val sizeBytes = lastRecord.optLong("sizeBytes", 0L)
                                val songs = lastRecord.optInt("songs", 0)
                                val lists = lastRecord.optInt("playlists", 0)
                                Text("Backup ID: $stamp (${sizeBytes / 1024} KB)", style = MaterialTheme.typography.bodySmall, color = Pulse.TextMid)
                                Text("Included: $songs songs with paths • $lists playlists", style = MaterialTheme.typography.labelSmall, color = Pulse.Mint)
                            }
                        }
                    }
                }
            }

            // ==========================================
            // OPTION B: FULL ARCHIVE BACKUP (REFERENCE FORMAT)
            // ==========================================
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Pulse.Elevated),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Pulse.Cyan.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Rounded.SettingsBackupRestore, null, tint = Pulse.Cyan, modifier = Modifier.size(24.dp))
                            }
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "OPTION B",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Pulse.Cyan,
                                        modifier = Modifier
                                            .background(Pulse.Cyan.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text("Full Backup (Reference Format)", style = MaterialTheme.typography.titleMedium, color = Color.White)
                                }
                                Text("Reference archive layout", style = MaterialTheme.typography.labelSmall, color = Pulse.Cyan)
                            }
                        }

                        Text(
                            text = "Create or restore a full backup using the reference ZIP-based .wav archive layout. Saves favourites, playlists, listening history, sort settings, and excluded folders.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Pulse.TextMid
                        )

                        // Bundled Reference Fixture Inspection Button (Prompt 12 & 13)
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Pulse.Surface),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Rounded.Info, null, tint = Pulse.Mint, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Bundled Reference Archive Fixture", style = MaterialTheme.typography.titleSmall, color = Color.White)
                                }
                                Text(
                                    "Inspect the included reference archive: ManualBackup_20261028_172204175.wav (13,509 songs, 41 playlists, settings, history).",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Pulse.TextMid
                                )
                                Button(
                                    onClick = { vm.inspectSampleReferenceBackup() },
                                    enabled = !archiveOp.inProgress,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("compat_inspect_sample_button"),
                                    shape = CircleShape,
                                    colors = ButtonDefaults.buttonColors(containerColor = Pulse.Mint, contentColor = Color.Black)
                                ) {
                                    Icon(Icons.Rounded.Unarchive, null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Inspect Sample Reference Archive", fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        // Restore Full Backup (SAF Picker)
                        OutlinedButton(
                            onClick = { safBackupPickerLauncher.launch(arrayOf("*/*")) },
                            enabled = !archiveOp.inProgress,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("compat_restore_button"),
                            shape = CircleShape
                        ) {
                            Icon(Icons.Rounded.Download, null, tint = Pulse.Cyan)
                            Spacer(Modifier.width(8.dp))
                            Text("Restore Full Backup (SAF)", color = Color.White, fontWeight = FontWeight.Bold)
                        }

                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Full Backup Categories:",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )

                        // Category Checkboxes with Live Counts
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            CompatCategoryCheckbox(
                                label = "Music tags (${allTracks.size} available songs)",
                                flag = "tags",
                                selected = compatExportCats.tags,
                                onToggle = { vm.toggleExportCategory("tags") }
                            )
                            CompatCategoryCheckbox(
                                label = "Playlists & Favourites ($favCount favourites, ${allPlaylists.size} playlists)",
                                flag = "playlists",
                                selected = compatExportCats.playlists,
                                onToggle = { vm.toggleExportCategory("playlists") }
                            )
                            CompatCategoryCheckbox(
                                label = "Listening history & monthly snapshots",
                                flag = "recap",
                                selected = compatExportCats.recap,
                                onToggle = { vm.toggleExportCategory("recap") }
                            )
                            CompatCategoryCheckbox(
                                label = "Sort settings",
                                flag = "sort",
                                selected = compatExportCats.sort,
                                onToggle = { vm.toggleExportCategory("sort") }
                            )
                            CompatCategoryCheckbox(
                                label = "Scan & excluded folder settings",
                                flag = "scan",
                                selected = compatExportCats.scan,
                                onToggle = { vm.toggleExportCategory("scan") }
                            )
                        }

                        // Switch: Save as .zip instead of .wav
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text("Save as .zip instead", style = MaterialTheme.typography.bodyMedium, color = Color.White)
                                Text(
                                    if (compatExportAsZip) "Will create standard .zip" else "Default .wav (reference naming)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Pulse.TextMid
                                )
                            }
                            Switch(
                                checked = compatExportAsZip,
                                onCheckedChange = { vm.setCompatExportAsZip(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Pulse.Cyan,
                                    checkedTrackColor = Pulse.Cyan.copy(alpha = 0.5f)
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
                                compatExportLauncher.launch("ManualBackup_$stamp.$ext")
                            },
                            enabled = !archiveOp.inProgress,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("compat_create_backup_button"),
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(containerColor = Pulse.Cyan, contentColor = Color.Black)
                        ) {
                            Icon(Icons.Rounded.Upload, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Create Full Backup", fontWeight = FontWeight.Bold)
                        }

                        Text(
                            text = "Writes the same layout as the reference file. Acceptance by other apps has not been tested.",
                            style = MaterialTheme.typography.labelSmall,
                            color = Pulse.TextMid
                        )
                    }
                }
            }

            // ==========================================
            // OPTION A: EXISTING BACKUP (FAVORITES JSON)
            // ==========================================
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Pulse.Surface),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Pulse.Violet.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Rounded.History, null, tint = Pulse.Violet, modifier = Modifier.size(24.dp))
                            }
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "OPTION A",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Pulse.Violet,
                                        modifier = Modifier
                                            .background(Pulse.Violet.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text("Existing Backup (Favorites JSON)", style = MaterialTheme.typography.titleMedium, color = Color.White)
                                }
                                Text("pulseplayer.favorites JSON format", style = MaterialTheme.typography.labelSmall, color = Pulse.Violet)
                            }
                        }

                        Text(
                            text = "Preserve the existing backup workflow. Exports and imports your favorites in a human-readable JSON file. Lightweight and dedicated strictly to your favorite tracks.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Pulse.TextMid
                        )

                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = {
                                    legacyExportLauncher.launch("PulsePlayer_Favorites_${LocalDate.now()}.json")
                                },
                                enabled = favCount > 0 && !archiveOp.inProgress,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("legacy_export_button"),
                                shape = CircleShape,
                                colors = ButtonDefaults.buttonColors(containerColor = Pulse.Violet, contentColor = Color.White)
                            ) {
                                Icon(Icons.Rounded.Upload, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(if (favCount > 0) "Export ($favCount)" else "No Favs", fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = {
                                    legacyImportLauncher.launch(arrayOf("application/json", "*/*"))
                                },
                                enabled = !archiveOp.inProgress,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("legacy_import_button"),
                                shape = CircleShape
                            ) {
                                Icon(Icons.Rounded.Download, null, modifier = Modifier.size(18.dp), tint = Pulse.Violet)
                                Spacer(Modifier.width(6.dp))
                                Text("Import JSON", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Dedicated Music Folder (SAF)
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Pulse.Surface),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.FolderOpen, null, tint = Pulse.Mint, modifier = Modifier.size(24.dp))
                            Spacer(Modifier.width(10.dp))
                            Text("Dedicated Music Folder", style = MaterialTheme.typography.titleMedium, color = Color.White)
                        }
                        Text(
                            text = "Select a folder (SAF) where you store copied music files. Pulse Player scans and indexes it for offline playback.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Pulse.TextMid
                        )
                        OutlinedButton(
                            onClick = { safFolderLauncher.launch(null) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("choose_folder_button"),
                            shape = CircleShape
                        ) {
                            Text("Choose Music Folder", color = Color.White)
                        }
                    }
                }
            }
        }
    }

    // Modal: Full Backup Restore Preview Dialog (Prompt 10)
    compatPreview?.let { preview ->
        val selectedCategories by vm.compatSelectedCategories.collectAsStateWithLifecycle()
        val replaceMode by vm.compatReplaceMode.collectAsStateWithLifecycle()

        Dialog(onDismissRequest = vm::dismissCompatPreview) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Pulse.Elevated,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Unarchive, null, tint = Pulse.Cyan, modifier = Modifier.size(28.dp))
                        Spacer(Modifier.width(12.dp))
                        Text("Backup Restore Preview", style = MaterialTheme.typography.titleMedium, color = Color.White)
                    }

                    // Metadata banner
                    val backupTime = preview.staged.meta["backupTime"] as? String ?: "Unknown time"
                    val device = preview.staged.meta["deviceId"] as? String ?: "Unknown device"
                    val ver = preview.staged.meta["versionName"] as? String ?: "1.0.0"
                    Text("Backup Date: $backupTime", style = MaterialTheme.typography.bodySmall, color = Pulse.TextMid)
                    Text("Source: Device $device (v$ver)", style = MaterialTheme.typography.bodySmall, color = Pulse.TextMid)

                    // Resolution Stats Breakdown
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Pulse.Surface),
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
                                color = Pulse.TextMid
                            )
                            if (preview.stats.ambiguous > 0 || preview.stats.missing > 0) {
                                Text(
                                    "Unmatched: ${preview.stats.ambiguous} ambiguous, ${preview.stats.missing} not found on this device",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Pulse.Coral
                                )
                            }
                        }
                    }

                    // Category selection checkboxes
                    Text("Categories to restore:", style = MaterialTheme.typography.labelMedium, color = Color.White, fontWeight = FontWeight.Bold)
                    Column {
                        CompatCategoryCheckbox(
                            label = "Playlists & Favourites (${preview.staged.playlists.size} playlists)",
                            flag = "isBackupPlaylists",
                            selected = "isBackupPlaylists" in selectedCategories,
                            onToggle = { vm.toggleCompatCategory("isBackupPlaylists") }
                        )
                        CompatCategoryCheckbox(
                            label = "Listening History (${preview.staged.events.size} records)",
                            flag = "isBackupMusicRecap",
                            selected = "isBackupMusicRecap" in selectedCategories,
                            onToggle = { vm.toggleCompatCategory("isBackupMusicRecap") }
                        )
                        CompatCategoryCheckbox(
                            label = "Sort Settings",
                            flag = "isBackupSortSettings",
                            selected = "isBackupSortSettings" in selectedCategories,
                            onToggle = { vm.toggleCompatCategory("isBackupSortSettings") }
                        )
                        CompatCategoryCheckbox(
                            label = "Excluded Folders (${preview.staged.excludeDirs.size} folders)",
                            flag = "isBackupScanHideSettings",
                            selected = "isBackupScanHideSettings" in selectedCategories,
                            onToggle = { vm.toggleCompatCategory("isBackupScanHideSettings") }
                        )
                    }

                    // Conflict resolution
                    Text("Conflict Mode:", style = MaterialTheme.typography.labelMedium, color = Color.White, fontWeight = FontWeight.Bold)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = !replaceMode,
                            onClick = { vm.setCompatReplaceMode(false) },
                            colors = RadioButtonDefaults.colors(selectedColor = Pulse.Cyan)
                        )
                        Text("Merge (default)", color = Color.White, style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.width(16.dp))
                        RadioButton(
                            selected = replaceMode,
                            onClick = { vm.setCompatReplaceMode(true) },
                            colors = RadioButtonDefaults.colors(selectedColor = Pulse.Coral)
                        )
                        Text("Replace selected", color = Color.White, style = MaterialTheme.typography.bodySmall)
                    }

                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(
                            onClick = vm::dismissCompatPreview,
                            modifier = Modifier.testTag("compat_restore_cancel_button")
                        ) {
                            Text("Cancel", color = Color.White)
                        }
                        Spacer(Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (replaceMode) {
                                    showReplaceConfirmDialog = true
                                } else {
                                    vm.executeCompatRestore(preview.staged, selectedCategories, false)
                                }
                            },
                            modifier = Modifier.testTag("compat_restore_confirm_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (replaceMode) Pulse.Coral else Pulse.Cyan,
                                contentColor = Color.Black
                            )
                        ) {
                            Text(if (replaceMode) "Replace & Restore" else "Apply Restore", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Replace confirmation dialog
        if (showReplaceConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showReplaceConfirmDialog = false },
                title = { Text("Confirm Replace Restore") },
                text = {
                    Text(
                        "Replace mode will clear your existing data in the selected categories (favourites, playlists, or play history) before restoring. This cannot be undone. Are you sure?"
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showReplaceConfirmDialog = false
                            vm.executeCompatRestore(preview.staged, selectedCategories, true)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Pulse.Coral, contentColor = Color.White)
                    ) {
                        Text("Yes, Replace & Restore", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showReplaceConfirmDialog = false }) {
                        Text("Cancel", color = Color.White)
                    }
                },
                containerColor = Pulse.Elevated,
                titleContentColor = Pulse.Coral,
                textContentColor = Color.White
            )
        }
    }

    // Modal: Full Backup Restore Report Dialog (Prompt 10)
    compatReport?.let { report ->
        AlertDialog(
            onDismissRequest = vm::dismissCompatReport,
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
                        Text("• $line", style = MaterialTheme.typography.bodySmall, color = Color.White)
                    }
                    if (report.unmatchedPaths.isNotEmpty()) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "${report.unmatchedPaths.size} tracks were unmatched or missing on this device.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Pulse.Coral
                        )
                        Button(
                            onClick = {
                                pendingUnmatchedList = report.unmatchedPaths
                                saveUnmatchedLauncher.launch("unmatched_tracks.txt")
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("compat_save_unmatched_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = Pulse.Surface, contentColor = Pulse.Cyan)
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
                    onClick = vm::dismissCompatReport,
                    colors = ButtonDefaults.buttonColors(containerColor = Pulse.Cyan, contentColor = Color.Black)
                ) {
                    Text("Done", fontWeight = FontWeight.Bold)
                }
            },
            containerColor = Pulse.Elevated,
            titleContentColor = Color.White,
            textContentColor = Color.White
        )
    }
}

@Composable
private fun CompatCategoryCheckbox(
    label: String,
    flag: String,
    selected: Boolean,
    onToggle: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = selected,
            onCheckedChange = { onToggle() },
            colors = CheckboxDefaults.colors(
                checkedColor = Pulse.Cyan,
                uncheckedColor = Pulse.TextMid,
                checkmarkColor = Color.Black
            )
        )
        Spacer(Modifier.width(4.dp))
        Text(label, style = MaterialTheme.typography.bodySmall, color = Color.White)
    }
}
