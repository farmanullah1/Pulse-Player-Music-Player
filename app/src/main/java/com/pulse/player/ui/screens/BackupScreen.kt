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
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.SettingsBackupRestore
import androidx.compose.material.icons.rounded.Unarchive
import androidx.compose.material.icons.rounded.Upload
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pulse.player.data.backup.BackupCategory
import com.pulse.player.data.backup.RestoreConflictMode
import com.pulse.player.data.backup.RestorePreview
import com.pulse.player.data.backup.RestoreReport
import com.pulse.player.ui.theme.Pulse
import com.pulse.player.ui.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BackupScreen(vm: MainViewModel) {
    val favCount by vm.favoriteCount.collectAsStateWithLifecycle()
    val exportStatus by vm.exportStatus.collectAsStateWithLifecycle()
    val archiveOp by vm.archiveOperation.collectAsStateWithLifecycle()
    val archivePreview by vm.archivePreview.collectAsStateWithLifecycle()
    val archiveReport by vm.archiveReport.collectAsStateWithLifecycle()
    val lastBackup by vm.lastBackupRecord.collectAsStateWithLifecycle()
    val selectedCats by vm.selectedBackupCategories.collectAsStateWithLifecycle()
    val conflictMode by vm.selectedRestoreConflictMode.collectAsStateWithLifecycle()
    val ctx = LocalContext.current

    var pendingRestoreUri by remember { mutableStateOf<Uri?>(null) }
    var pendingRestoreFileName by remember { mutableStateOf("") }

    // Launchers for Legacy JSON
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

    // Launchers for Portable Archive (.musicbackup / ZIP)
    val archiveCreateLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        if (uri != null) {
            vm.createArchiveBackup(uri)
        }
    }

    val archiveRestoreLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val fileName = uri.lastPathSegment?.substringAfterLast('/') ?: "PulsePlayer_Backup.musicbackup"
            pendingRestoreUri = uri
            pendingRestoreFileName = fileName
            vm.inspectArchive(uri, fileName)
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
            IconButton(onClick = vm::back) {
                Icon(Icons.Rounded.ArrowBack, "Back", tint = Color.White)
            }
            Spacer(Modifier.width(8.dp))
            Text("Backup & Restore", style = MaterialTheme.typography.titleLarge, color = Color.White)
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
                            color = Pulse.Cyan,
                            modifier = Modifier.padding(16.dp),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // OPTION B: Portable Archive Backup (New System)
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
                                    .background(Pulse.Cyan.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Rounded.Archive, null, tint = Pulse.Cyan, modifier = Modifier.size(24.dp))
                            }
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text("Option B — Portable Archive Backup", style = MaterialTheme.typography.titleMedium, color = Color.White)
                                Text("Structured .musicbackup / ZIP format", style = MaterialTheme.typography.labelSmall, color = Pulse.Cyan)
                            }
                        }

                        Text(
                            text = "Creates a single verified archive containing your selected library metadata, playlists, playback records, and player settings. Music audio files are not copied into the archive.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Pulse.TextMid
                        )

                        Text("Select Categories to Include:", style = MaterialTheme.typography.labelMedium, color = Color.White, fontWeight = FontWeight.Bold)

                        // Categories Selection
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            BackupCategory.values().forEach { cat ->
                                val isSelected = cat in selectedCats
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { vm.toggleBackupCategory(cat) }
                                        .padding(horizontal = 4.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = isSelected,
                                        onCheckedChange = { vm.toggleBackupCategory(cat) },
                                        colors = CheckboxDefaults.colors(
                                            checkedColor = Pulse.Cyan,
                                            checkmarkColor = Color.Black
                                        )
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(cat.displayName, style = MaterialTheme.typography.bodyMedium, color = if (isSelected) Color.White else Pulse.TextMid)
                                }
                            }
                        }

                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = {
                                    archiveCreateLauncher.launch("PulseBackup_${LocalDate.now()}.musicbackup")
                                },
                                enabled = !archiveOp.inProgress && selectedCats.isNotEmpty(),
                                modifier = Modifier.weight(1f),
                                shape = CircleShape,
                                colors = ButtonDefaults.buttonColors(containerColor = Pulse.Cyan, contentColor = Color.Black)
                            ) {
                                Icon(Icons.Rounded.Upload, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Create Archive", fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = {
                                    archiveRestoreLauncher.launch(arrayOf("*/*", "application/zip", "application/octet-stream"))
                                },
                                enabled = !archiveOp.inProgress,
                                modifier = Modifier.weight(1f),
                                shape = CircleShape
                            ) {
                                Icon(Icons.Rounded.Download, null, tint = Pulse.Cyan, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Restore Archive", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Last Archive Backup Record
            if (lastBackup != null) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Pulse.Elevated),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.History, null, tint = Pulse.Mint, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Last Archive Backup", style = MaterialTheme.typography.titleSmall, color = Color.White)
                            }
                            val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(lastBackup!!.timestampMs))
                            Text("File: ${lastBackup!!.fileName} (${lastBackup!!.fileSizeBytes / 1024} KB)", style = MaterialTheme.typography.bodySmall, color = Pulse.TextMid)
                            Text("Saved at: $dateStr", style = MaterialTheme.typography.bodySmall, color = Pulse.TextMid)
                            val countsStr = lastBackup!!.recordCounts.entries.joinToString(" • ") { "${it.key}: ${it.value}" }
                            if (countsStr.isNotBlank()) {
                                Text(countsStr, style = MaterialTheme.typography.labelSmall, color = Pulse.Mint)
                            }
                        }
                    }
                }
            }

            // OPTION A: Existing Backup (Legacy Favourites)
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
                                Icon(Icons.Rounded.SettingsBackupRestore, null, tint = Pulse.Violet, modifier = Modifier.size(24.dp))
                            }
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text("Option A — Existing Backup (Legacy)", style = MaterialTheme.typography.titleMedium, color = Color.White)
                                Text("Lightweight pulseplayer.favorites JSON format", style = MaterialTheme.typography.labelSmall, color = Pulse.Violet)
                            }
                        }

                        Text(
                            text = "Preserves compatibility with existing Pulse Player backups. Exports and imports your $favCount favourites in a single human-readable JSON file.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Pulse.TextMid
                        )

                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = {
                                    legacyExportLauncher.launch("PulsePlayer_Favorites_${LocalDate.now()}.json")
                                },
                                enabled = favCount > 0 && !archiveOp.inProgress,
                                modifier = Modifier.weight(1f),
                                shape = CircleShape,
                                colors = ButtonDefaults.buttonColors(containerColor = Pulse.Violet, contentColor = Color.White)
                            ) {
                                Text(if (favCount > 0) "Export ($favCount)" else "No Favourites", fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = {
                                    legacyImportLauncher.launch(arrayOf("application/json", "*/*"))
                                },
                                enabled = !archiveOp.inProgress,
                                modifier = Modifier.weight(1f),
                                shape = CircleShape
                            ) {
                                Text("Import JSON", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Music Library Folder Selection (SAF)
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
                            text = "Select a custom folder where you keep audio files. Pulse Player will index and keep it available offline.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Pulse.TextMid
                        )
                        OutlinedButton(
                            onClick = { safFolderLauncher.launch(null) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = CircleShape
                        ) {
                            Text("Choose Music Folder", color = Color.White)
                        }
                    }
                }
            }
        }
    }

    // Modal: Restore Preview Dialog
    archivePreview?.let { preview ->
        var selectedMode by remember { mutableStateOf(RestoreConflictMode.MERGE) }
        val manifestCategories = preview.manifest.categories.filter { it.value.included }
        var activeRestoreCats by remember {
            mutableStateOf(
                BackupCategory.values().filter { it.id in manifestCategories.keys }.toSet()
            )
        }

        Dialog(onDismissRequest = vm::dismissArchivePreview) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Pulse.Elevated,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Unarchive, null, tint = Pulse.Cyan, modifier = Modifier.size(28.dp))
                        Spacer(Modifier.width(12.dp))
                        Text("Archive Restore Preview", style = MaterialTheme.typography.titleMedium, color = Color.White)
                    }

                    Text("File: ${preview.fileName}", style = MaterialTheme.typography.bodySmall, color = Pulse.TextMid)
                    Text("Created: ${preview.manifest.createdAtUtc}", style = MaterialTheme.typography.bodySmall, color = Pulse.TextMid)

                    // Integrity check pill
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (preview.checksumsVerified) Pulse.Mint.copy(alpha = 0.15f) else Pulse.Coral.copy(alpha = 0.15f))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            if (preview.checksumsVerified) Icons.Rounded.CheckCircle else Icons.Rounded.WarningAmber,
                            null,
                            tint = if (preview.checksumsVerified) Pulse.Mint else Pulse.Coral,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (preview.checksumsVerified) "SHA-256 Checksums Verified" else "Integrity Warning",
                            color = if (preview.checksumsVerified) Pulse.Mint else Pulse.Coral,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }

                    Text("Conflict Resolution:", style = MaterialTheme.typography.labelMedium, color = Color.White, fontWeight = FontWeight.Bold)
                    Column {
                        Row(
                            Modifier.fillMaxWidth().clickable { selectedMode = RestoreConflictMode.MERGE },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedMode == RestoreConflictMode.MERGE,
                                onClick = { selectedMode = RestoreConflictMode.MERGE },
                                colors = RadioButtonDefaults.colors(selectedColor = Pulse.Cyan)
                            )
                            Spacer(Modifier.width(4.dp))
                            Column {
                                Text("Merge", color = Color.White, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                Text("Retain existing data and add imported items", color = Pulse.TextMid, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(
                            Modifier.fillMaxWidth().clickable { selectedMode = RestoreConflictMode.REPLACE_SELECTED_CATEGORIES },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedMode == RestoreConflictMode.REPLACE_SELECTED_CATEGORIES,
                                onClick = { selectedMode = RestoreConflictMode.REPLACE_SELECTED_CATEGORIES },
                                colors = RadioButtonDefaults.colors(selectedColor = Pulse.Coral)
                            )
                            Spacer(Modifier.width(4.dp))
                            Column {
                                Text("Replace Selected", color = Color.White, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                Text("Overwrite chosen categories with archive data", color = Pulse.TextMid, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }

                    Text(
                        "Note: Actual MP3/audio files are not modified or moved during restore. Tracks are safely matched with your media library.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Pulse.TextMid
                    )

                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = vm::dismissArchivePreview) {
                            Text("Cancel", color = Color.White)
                        }
                        Spacer(Modifier.width(8.dp))
                        Button(
                            onClick = {
                                pendingRestoreUri?.let { uri ->
                                    vm.executeArchiveRestore(
                                        uri = uri,
                                        fileName = pendingRestoreFileName,
                                        categories = activeRestoreCats,
                                        mode = selectedMode
                                    )
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Pulse.Cyan, contentColor = Color.Black)
                        ) {
                            Text("Apply Restore", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // Modal: Restore Report Dialog
    archiveReport?.let { report ->
        AlertDialog(
            onDismissRequest = vm::dismissArchiveReport,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.CheckCircle, null, tint = Pulse.Mint)
                    Spacer(Modifier.width(8.dp))
                    Text("Restore Complete")
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Archive: ${report.fileName}", fontWeight = FontWeight.Bold)
                    Text("Mode: ${if (report.conflictMode == RestoreConflictMode.MERGE) "Merge" else "Replace"}")
                    Text("Total items restored: ${report.totalRestored}")
                    if (report.importedCount.isNotEmpty()) {
                        report.importedCount.forEach { (cat, count) ->
                            Text("• $cat: $count restored", color = Pulse.Mint)
                        }
                    }
                    if (report.unmatchedTracks > 0) {
                        Text("• Unmatched tracks: ${report.unmatchedTracks} (songs not found in current library)", color = Pulse.TextMid)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = vm::dismissArchiveReport,
                    colors = ButtonDefaults.buttonColors(containerColor = Pulse.Cyan, contentColor = Color.Black)
                ) {
                    Text("Done")
                }
            },
            containerColor = Pulse.Elevated,
            titleContentColor = Color.White,
            textContentColor = Color.White
        )
    }
}
