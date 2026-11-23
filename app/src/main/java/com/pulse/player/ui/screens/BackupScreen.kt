package com.pulse.player.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.SettingsBackupRestore
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pulse.player.ui.components.LocalBottomOverlayPadding
import com.pulse.player.ui.components.PulseTopBar
import com.pulse.player.ui.theme.Pulse
import com.pulse.player.ui.theme.TabularMono
import com.pulse.player.ui.viewmodel.MainViewModel
import java.time.LocalDate

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BackupScreen(vm: MainViewModel) {
    BackHandler { vm.back() }
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

    val selectedTab by vm.backupSelectedTab.collectAsStateWithLifecycle()
    var showReplaceConfirmDialog by remember { mutableStateOf(false) }
    var showClassicExportPreflight by remember { mutableStateOf(false) }
    var pendingUnmatchedList by remember { mutableStateOf<List<String>?>(null) }

    val compatExportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        if (uri != null) vm.executeCompatExport(uri)
    }

    val safBackupPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val fileName = uri.lastPathSegment?.substringAfterLast('/') ?: "backup"
            vm.smartInspect(uri, fileName)
        }
    }

    val saveUnmatchedLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/plain")
    ) { uri ->
        if (uri != null && pendingUnmatchedList != null) {
            vm.saveUnmatchedReport(uri, pendingUnmatchedList!!)
        }
    }

    val legacyExportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) vm.exportFavorites(uri)
    }

    val legacyImportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val fileName = uri.lastPathSegment?.substringAfterLast('/') ?: "PulsePlayer_Favorites.json"
            vm.previewFavoritesImport(uri, fileName)
        }
    }

    val portableFolderImportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) vm.previewFavoritesFolder(uri)
    }

    val portableCustomFolderExportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            runCatching {
                ctx.contentResolver.takePersistableUriPermission(
                    uri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            }
            vm.exportFavoritesToPulseFolder(uri)
        }
    }

    val legacyWritePermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) vm.exportFavoritesToPulseFolder()
        else vm.exportStatus.value = "Storage permission is needed to create a portable backup on Android 9 and older."
    }

    fun exportPortableBackup() {
        if (Build.VERSION.SDK_INT < 29 &&
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.WRITE_EXTERNAL_STORAGE) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            legacyWritePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        } else {
            vm.exportFavoritesToPulseFolder()
        }
    }

    val safFolderLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            vm.addSafFolder(uri)
            vm.snackbarMessage.value = "Folder added to music library"
        }
    }

    val bottomInset = LocalBottomOverlayPadding.current

    Column(Modifier.fillMaxSize()) {
        PulseTopBar(
            title = "Backup & Transfer",
            subtitle = "Quick favourites transfer & full library archive",
            onBack = vm::back,
            backTestTag = "backup_back_button"
        )

        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.primary,
            indicator = { tabPositions ->
                if (selectedTab < tabPositions.size) {
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            },
            divider = {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
            }
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { vm.backupSelectedTab.value = 0 },
                modifier = Modifier.testTag("backup_tab_quick_transfer"),
                text = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Rounded.SwapHoriz, null, modifier = Modifier.size(18.dp))
                        Text("Quick Transfer", fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Medium)
                    }
                }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { vm.backupSelectedTab.value = 1 },
                modifier = Modifier.testTag("backup_tab_full_archive"),
                text = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Rounded.SettingsBackupRestore, null, modifier = Modifier.size(18.dp))
                        Text("Full Archive", fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Medium)
                    }
                }
            )
        }

        LazyColumn(
            state = vm.lazyListStateFor("backup_$selectedTab"),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = bottomInset + 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (archiveOp.inProgress) {
                item(key = "op_progress") {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        shape = RoundedCornerShape(18.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
                    ) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.5.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    text = archiveOp.stage,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                if (archiveOp.progressFraction > 0f) {
                                    Text(
                                        text = "${(archiveOp.progressFraction * 100).toInt()}%",
                                        style = MaterialTheme.typography.labelMedium.merge(TabularMono),
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            if (archiveOp.progressFraction > 0f) {
                                LinearProgressIndicator(
                                    progress = { archiveOp.progressFraction },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(CircleShape),
                                    color = MaterialTheme.colorScheme.primary,
                                    trackColor = MaterialTheme.colorScheme.surface
                                )
                            }
                        }
                    }
                }
            }

            if (archiveOp.completedMessage != null) {
                item(key = "op_completed") {
                    Surface(
                        color = Pulse.Mint.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Pulse.Mint.copy(alpha = 0.35f))
                    ) {
                        Row(
                            Modifier.fillMaxWidth().padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Rounded.CheckCircle, null, tint = Pulse.Mint, modifier = Modifier.size(22.dp))
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
                item(key = "op_error") {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.45f))
                    ) {
                        Row(
                            Modifier.fillMaxWidth().padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Rounded.ErrorOutline, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(22.dp))
                            Spacer(Modifier.width(12.dp))
                            Text(
                                text = archiveOp.errorMessage.orEmpty(),
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }

            if (exportStatus != null) {
                item(key = "export_status") {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f))
                    ) {
                        Text(
                            text = exportStatus.orEmpty(),
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            if (selectedTab == 0) {
                quickTransferTabItems(
                    favCount = favCount,
                    archiveOp = archiveOp,
                    onShowExportPreflight = { showClassicExportPreflight = true },
                    onExportCustomFolder = { portableCustomFolderExportLauncher.launch(null) },
                    onExportJsonOnly = { legacyExportLauncher.launch("PulsePlayer_Favorites_${LocalDate.now()}.json") },
                    onImportAutoDetect = { vm.importFavoritesFromDownloadFolder() },
                    onPickPortableFolder = { portableFolderImportLauncher.launch(null) },
                    onImportJson = { legacyImportLauncher.launch(arrayOf("application/json", "*/*")) },
                    onChooseMusicFolder = { safFolderLauncher.launch(null) }
                )
            } else {
                fullArchiveTabItems(
                    lastBackupJson = lastBackupJson,
                    allTracksCount = allTracks.size,
                    favCount = favCount,
                    allPlaylistsCount = allPlaylists.size,
                    archiveOp = archiveOp,
                    compatExportCats = compatExportCats,
                    compatExportAsZip = compatExportAsZip,
                    onRestoreFullBackup = { safBackupPickerLauncher.launch(arrayOf("*/*")) },
                    onToggleExportCategory = vm::toggleExportCategory,
                    onSetExportAsZip = vm::setCompatExportAsZip,
                    onCreateFullBackup = { filename -> compatExportLauncher.launch(filename) },
                    onInspectSampleReference = vm::inspectSampleReferenceBackup
                )
            }
        }
    }

    compatPreview?.let { preview ->
        val selectedCategories by vm.compatSelectedCategories.collectAsStateWithLifecycle()
        val replaceMode by vm.compatReplaceMode.collectAsStateWithLifecycle()
        BackupRestorePreviewDialog(
            preview = preview,
            selectedCategories = selectedCategories,
            replaceMode = replaceMode,
            showReplaceConfirmDialog = showReplaceConfirmDialog,
            onDismissPreview = vm::dismissCompatPreview,
            onToggleCategory = vm::toggleCompatCategory,
            onSetReplaceMode = vm::setCompatReplaceMode,
            onRequestReplaceConfirm = { showReplaceConfirmDialog = true },
            onDismissReplaceConfirm = { showReplaceConfirmDialog = false },
            onExecuteRestore = vm::executeCompatRestore
        )
    }

    compatReport?.let { report ->
        BackupRestoreReportDialog(
            report = report,
            onDismiss = vm::dismissCompatReport,
            onSaveUnmatched = { unmatched ->
                pendingUnmatchedList = unmatched
                saveUnmatchedLauncher.launch("unmatched_tracks.txt")
            }
        )
    }

    if (showClassicExportPreflight) {
        ClassicExportPreflightDialog(
            favCount = favCount,
            onDismiss = { showClassicExportPreflight = false },
            onExportAll = {
                showClassicExportPreflight = false
                exportPortableBackup()
            },
            onExportJsonOnly = {
                showClassicExportPreflight = false
                legacyExportLauncher.launch("PulsePlayer_Favorites_${LocalDate.now()}.json")
            }
        )
    }
}
