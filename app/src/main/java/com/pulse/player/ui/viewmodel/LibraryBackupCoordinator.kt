package com.pulse.player.ui.viewmodel

import android.app.Application
import android.content.Intent
import android.net.Uri
import com.pulse.player.data.ErrorCategory
import com.pulse.player.data.ErrorDispatcher
import com.pulse.player.data.backup.ArchiveBackupService
import com.pulse.player.data.backup.ArchiveRestoreService
import com.pulse.player.data.backup.BackupCategory
import com.pulse.player.data.backup.BackupOperationState
import com.pulse.player.data.backup.Compat
import com.pulse.player.data.backup.CompatRestorePreview
import com.pulse.player.data.backup.CompatRestoreReport
import com.pulse.player.data.backup.ExportCats
import com.pulse.player.data.backup.LastBackupRecord
import com.pulse.player.data.backup.RestoreConflictMode
import com.pulse.player.data.backup.RestoreOptions
import com.pulse.player.data.backup.RestorePreview
import com.pulse.player.data.backup.RestoreReport
import com.pulse.player.data.backup.SafeZip
import com.pulse.player.data.backup.Sniffed
import com.pulse.player.data.backup.StagedBackup
import com.pulse.player.data.backup.applyRestore
import com.pulse.player.data.backup.classic.FavoritesRepository
import com.pulse.player.data.backup.copyToTemp
import com.pulse.player.data.backup.exportCompat
import com.pulse.player.data.backup.previewRestore
import com.pulse.player.data.backup.sniff
import com.pulse.player.data.backup.stage
import com.pulse.player.data.db.PulseDb
import com.pulse.player.data.db.TrackEntity
import com.pulse.player.data.db.TrackWithFav
import com.pulse.player.data.favorites.FavItem
import com.pulse.player.data.settings.SettingsStore
import com.pulse.player.playback.LoudnessScanState
import com.pulse.player.playback.LoudnessScanner
import com.pulse.player.playback.Track
import com.pulse.player.playback.toTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class LibraryBackupCoordinator(
    private val app: Application,
    private val db: PulseDb,
    private val settings: SettingsStore,
    private val favoritesRepo: FavoritesRepository,
    private val archiveBackup: ArchiveBackupService,
    private val archiveRestore: ArchiveRestoreService,
    private val scope: CoroutineScope,
    private val onSnackbar: (String) -> Unit
) {
    // Stats & Recap
    val todayMinutes = MutableStateFlow(0)
    val streakDays = MutableStateFlow(0)
    val topPicks = MutableStateFlow<List<Track>>(emptyList())
    val resume = MutableStateFlow<Track?>(null)

    // ReplayGain On-Device Loudness Scan
    private val _loudnessScanState = MutableStateFlow<LoudnessScanState>(LoudnessScanState.Idle)
    val loudnessScanState: StateFlow<LoudnessScanState> = _loudnessScanState.asStateFlow()
    private var loudnessScanJob: Job? = null

    fun startLoudnessScan() {
        if (loudnessScanJob?.isActive == true) return
        loudnessScanJob = scope.launch {
            val result = LoudnessScanner.scanMissingTracks(app, db) { progress ->
                _loudnessScanState.value = progress
            }
            if (result is LoudnessScanState.Completed) {
                val msg = if (result.updatedCount > 0) {
                    "Analyzed loudness for ${result.updatedCount} track(s)"
                } else {
                    "All tracks already have ReplayGain loudness tags"
                }
                onSnackbar(msg)
            }
        }
    }

    fun cancelLoudnessScan() {
        loudnessScanJob?.cancel()
        loudnessScanJob = null
        val prev = _loudnessScanState.value
        val count = (prev as? LoudnessScanState.Scanning)?.processed ?: 0
        _loudnessScanState.value = LoudnessScanState.Cancelled(count)
    }

    fun resetLoudnessScanState() {
        if (loudnessScanJob?.isActive != true) {
            _loudnessScanState.value = LoudnessScanState.Idle
        }
    }

    fun refreshStats() = scope.launch(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val startOfDay = now - (now % 86_400_000L)
        val todayMs = db.stats().totalMs(startOfDay, now)
        todayMinutes.value = (todayMs / 60_000L).toInt()

        val days = db.stats().playDays()
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val cal = Calendar.getInstance()
        val today = sdf.format(cal.time)
        cal.add(Calendar.DAY_OF_YEAR, -1)
        val yesterday = sdf.format(cal.time)

        val first = days.firstOrNull()
        var s = 0
        if (first == today || first == yesterday) {
            val daySet = days.toSet()
            val checkCal = Calendar.getInstance()
            if (first == yesterday) {
                checkCal.add(Calendar.DAY_OF_YEAR, -1)
            }
            while (true) {
                val dStr = sdf.format(checkCal.time)
                if (dStr in daySet) {
                    s++
                    checkCal.add(Calendar.DAY_OF_YEAR, -1)
                } else {
                    break
                }
            }
        }
        streakDays.value = s

        val topIds = db.stats().observeTopIds(10).first()
        if (topIds.isNotEmpty()) {
            val entities = db.tracks().byIds(topIds).associateBy { it.id }
            topPicks.value = topIds.mapNotNull { entities[it]?.let { e -> TrackWithFav(e, false).toTrack() } }
        }

        val recentIds = db.stats().observeRecentIds(1).first()
        if (recentIds.isNotEmpty()) {
            db.tracks().byId(recentIds.first())?.let {
                resume.value = TrackWithFav(it, false).toTrack()
            }
        }
    }

    fun cleanMissingTracks(onDone: ((Int) -> Unit)? = null) = scope.launch(Dispatchers.IO) {
        val purgedPlaylists = db.playlists().purgeMissingFromPlaylists()
        val deleted = db.tracks().deleteMissingTracks()
        ErrorDispatcher.post(
            category = ErrorCategory.STORAGE,
            title = "Missing Tracks Cleaned",
            message = "Removed $deleted unavailable track(s) from library and $purgedPlaylists from playlists."
        )
        withContext(Dispatchers.Main) {
            onDone?.invoke(deleted)
        }
    }

    // Backups
    val lastBackupJson = MutableStateFlow<String?>(null)
    val exportStatus = MutableStateFlow<String?>(null)
    val importPreview = MutableStateFlow<FavoritesRepository.Preview?>(null)

    val compatPreview = MutableStateFlow<CompatRestorePreview?>(null)
    val compatReport = MutableStateFlow<CompatRestoreReport?>(null)
    val compatSelectedCategories = MutableStateFlow<Set<String>>(Compat.FLAGS.toSet())
    val compatReplaceMode = MutableStateFlow(false)
    val compatExportCats = MutableStateFlow(ExportCats())
    val compatExportAsZip = MutableStateFlow(false)

    val archiveOperation = MutableStateFlow(BackupOperationState())
    val archivePreview = MutableStateFlow<RestorePreview?>(null)
    val archiveReport = MutableStateFlow<RestoreReport?>(null)
    val lastBackupRecord = MutableStateFlow<LastBackupRecord?>(null)
    val selectedBackupCategories = MutableStateFlow<Set<BackupCategory>>(BackupCategory.values().toSet())
    val selectedRestoreConflictMode = MutableStateFlow(RestoreConflictMode.MERGE)

    fun refreshLastBackup() = scope.launch {
        lastBackupJson.value = settings.lastBackup()
    }

    fun exportFavorites(uri: Uri) = scope.launch {
        val res = favoritesRepo.export(uri)
        if (res.isSuccess) {
            exportStatus.value = "Exported ${res.getOrNull()} favourites"
        } else {
            val err = res.exceptionOrNull()
            val msg = "Export failed: ${err?.message ?: "Unknown error"}"
            exportStatus.value = msg
            ErrorDispatcher.post(ErrorCategory.BACKUP, "Favourites Export Failed", msg, err?.stackTraceToString())
        }
    }

    fun exportFavoritesToPulseFolder(customTreeUri: Uri? = null) = scope.launch {
        archiveOperation.value = BackupOperationState(
            inProgress = true,
            stage = "Initializing Classic Backup...",
            progressFraction = 0f
        )
        val result = favoritesRepo.exportToPortableFolder(customTreeUri) { current, total, title, fraction ->
            archiveOperation.value = BackupOperationState(
                inProgress = true,
                stage = "Copying ($current/$total): $title",
                progressFraction = fraction
            )
        }
        result.fold(
            onSuccess = { report ->
                val msg = buildString {
                    append("Saved ${report.favorites} favourites to ${report.location}. ")
                    append("Copied ${report.copied} music files")
                    if (report.reused > 0) append(", reused ${report.reused} existing copies")
                    if (report.failed.isNotEmpty()) append(". ${report.failed.size} files could not be copied")
                    append(".")
                }
                exportStatus.value = msg
                archiveOperation.value = BackupOperationState(inProgress = false, completedMessage = msg)
            },
            onFailure = { err ->
                val msg = "Portable backup failed: ${err.message}"
                exportStatus.value = msg
                archiveOperation.value = BackupOperationState(inProgress = false, errorMessage = msg)
                ErrorDispatcher.post(ErrorCategory.BACKUP, "Classic Backup Failed", msg, err.stackTraceToString())
            }
        )
    }

    fun previewFavoritesImport(uri: Uri, name: String) = scope.launch {
        val res = favoritesRepo.preview(uri, name)
        if (res.isSuccess) {
            importPreview.value = res.getOrNull()
        } else {
            val err = res.exceptionOrNull()
            val msg = "Import preview failed: ${err?.message ?: "Unknown error"}"
            exportStatus.value = msg
            ErrorDispatcher.post(ErrorCategory.BACKUP, "Import Preview Failed", msg, err?.stackTraceToString())
        }
    }

    fun previewFavoritesFolder(uri: Uri) = scope.launch {
        val resolver = app.contentResolver
        runCatching {
            resolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val res = favoritesRepo.previewPortableFolder(uri)
        if (res.isSuccess) {
            importPreview.value = res.getOrNull()
            exportStatus.value = "Folder scanned. Review the fuzzy matches before importing."
        } else {
            val err = res.exceptionOrNull()
            val msg = "Folder import failed: ${err?.message ?: "Unknown error"}"
            exportStatus.value = msg
            ErrorDispatcher.post(ErrorCategory.BACKUP, "Folder Import Failed", msg, err?.stackTraceToString())
        }
    }

    fun importFavoritesFromDownloadFolder() = scope.launch {
        archiveOperation.value = BackupOperationState(inProgress = true, stage = "Scanning Download/Pulse...", progressFraction = 0f)
        val res = favoritesRepo.previewDownloadFolder()
        archiveOperation.value = BackupOperationState(inProgress = false)
        if (res.isSuccess) {
            importPreview.value = res.getOrNull()
            exportStatus.value = "Download/Pulse scanned. Review fuzzy matches before importing."
        } else {
            val ex = res.exceptionOrNull()
            val err = "Auto-detect failed: ${ex?.message ?: "Unknown error"}"
            exportStatus.value = err
            archiveOperation.value = BackupOperationState(inProgress = false, errorMessage = err)
            ErrorDispatcher.post(ErrorCategory.BACKUP, "Auto-Detect Failed", err, ex?.stackTraceToString())
        }
    }

    fun applyImport(preview: FavoritesRepository.Preview, resolved: Map<FavItem, TrackEntity>, replace: Boolean) = scope.launch {
        val count = favoritesRepo.apply(preview, resolved, replace)
        exportStatus.value = "Imported $count favourites successfully"
        importPreview.value = null
    }

    fun toggleCompatCategory(flag: String) {
        compatSelectedCategories.update { current ->
            if (flag in current) if (current.size > 1) current - flag else current else current + flag
        }
    }

    fun setCompatReplaceMode(replace: Boolean) {
        compatReplaceMode.value = replace
    }

    fun toggleExportCategory(key: String) {
        compatExportCats.update { cur ->
            when (key) {
                "tags" -> cur.copy(tags = !cur.tags)
                "covers" -> cur.copy(covers = !cur.covers)
                "lyrics" -> cur.copy(lyrics = !cur.lyrics)
                "playlists" -> cur.copy(playlists = !cur.playlists)
                "sort" -> cur.copy(sort = !cur.sort)
                "scan" -> cur.copy(scan = !cur.scan)
                "recap" -> cur.copy(recap = !cur.recap)
                else -> cur
            }
        }
    }

    fun setCompatExportAsZip(asZip: Boolean) {
        compatExportAsZip.value = asZip
    }

    fun executeCompatExport(destUri: Uri) = scope.launch {
        archiveOperation.value = BackupOperationState(inProgress = true, stage = "Starting full backup export...")
        val res = exportCompat(
            ctx = app,
            db = db,
            settings = settings,
            cats = compatExportCats.value,
            dest = destUri,
            onStage = { stageName ->
                archiveOperation.value = BackupOperationState(inProgress = true, stage = "Writing $stageName...")
            }
        )
        if (res.isSuccess) {
            val file = res.getOrNull()
            archiveOperation.value = BackupOperationState(
                inProgress = false,
                completedMessage = "Backup saved successfully (${file?.name ?: "backup"})"
            )
            refreshLastBackup()
        } else {
            val err = res.exceptionOrNull()?.message ?: "Unknown error"
            archiveOperation.value = BackupOperationState(inProgress = false, errorMessage = "Export failed: $err")
            ErrorDispatcher.post(ErrorCategory.BACKUP, "Export Failed", "Could not export backup: $err", res.exceptionOrNull()?.stackTraceToString())
        }
    }

    fun smartInspect(uri: Uri, fileName: String) = scope.launch {
        archiveOperation.value = BackupOperationState(inProgress = true, stage = "Sniffing backup format...")
        when (sniff(app, uri)) {
            Sniffed.REAL_WAV -> {
                val msg = "This is a real audio file, not a backup."
                archiveOperation.value = BackupOperationState(inProgress = false, errorMessage = msg)
                ErrorDispatcher.post(ErrorCategory.BACKUP, "Invalid Backup Format", msg, "Sniffed: REAL_WAV")
            }
            Sniffed.JSON -> {
                archiveOperation.value = BackupOperationState(inProgress = false)
                previewFavoritesImport(uri, fileName)
            }
            Sniffed.ZIP -> {
                archiveOperation.value = BackupOperationState(inProgress = true, stage = "Reading backup archive...")
                try {
                    val temp = copyToTemp(app, uri)
                    try {
                        val sz = SafeZip(temp)
                        if (!sz.isRecognizedBackup()) {
                            val msg = "This ZIP is not a recognised backup."
                            archiveOperation.value = BackupOperationState(inProgress = false, errorMessage = msg)
                            ErrorDispatcher.post(ErrorCategory.BACKUP, "Invalid Backup Archive", msg)
                            return@launch
                        }
                        val staged = stage(app, sz)
                        sz.close()
                        val preview = previewRestore(db, staged)
                        archiveOperation.value = BackupOperationState(inProgress = false)
                        compatPreview.value = preview
                    } finally {
                        temp.delete()
                    }
                } catch (e: Exception) {
                    val msg = e.message ?: "Failed to read backup archive."
                    archiveOperation.value = BackupOperationState(inProgress = false, errorMessage = msg)
                    ErrorDispatcher.post(ErrorCategory.BACKUP, "Backup Archive Error", msg, e.stackTraceToString())
                }
            }
            Sniffed.OTHER -> {
                val msg = "This file is not a recognised backup."
                archiveOperation.value = BackupOperationState(inProgress = false, errorMessage = msg)
                ErrorDispatcher.post(ErrorCategory.BACKUP, "Invalid Backup Format", msg)
            }
        }
    }

    fun inspectSampleReferenceBackup() = scope.launch {
        archiveOperation.value = BackupOperationState(inProgress = true, stage = "Loading reference sample backup...")
        try {
            val assetStream = app.assets.open("ManualBackup_20261028_172204175.wav")
            val tempFile = File.createTempFile("sample_ref_", ".wav", app.cacheDir)
            assetStream.use { input ->
                tempFile.outputStream().use { output -> input.copyTo(output) }
            }
            smartInspect(Uri.fromFile(tempFile), "ManualBackup_20261028_172204175.wav")
        } catch (e: Exception) {
            val msg = "Could not open reference backup: ${e.message}"
            archiveOperation.value = BackupOperationState(inProgress = false, errorMessage = msg)
            ErrorDispatcher.post(ErrorCategory.BACKUP, "Sample Backup Error", msg, e.stackTraceToString())
        }
    }

    fun executeCompatRestore(staged: StagedBackup, categories: Set<String>, replace: Boolean) = scope.launch {
        compatPreview.value = null
        archiveOperation.value = BackupOperationState(inProgress = true, stage = "Restoring data...")
        try {
            val report = applyRestore(app, db, settings, staged, RestoreOptions(categories, replace))
            archiveOperation.value = BackupOperationState(inProgress = false)
            compatReport.value = report
            refreshStats()
        } catch (e: Exception) {
            val msg = "Restore failed: ${e.message}"
            archiveOperation.value = BackupOperationState(inProgress = false, errorMessage = msg)
            ErrorDispatcher.post(ErrorCategory.BACKUP, "Restore Failed", msg, e.stackTraceToString())
        }
    }

    fun dismissCompatPreview() { compatPreview.value = null }
    fun dismissCompatReport() { compatReport.value = null }

    fun saveUnmatchedReport(destUri: Uri, lines: List<String>) = scope.launch {
        try {
            app.contentResolver.openOutputStream(destUri)?.use { out ->
                out.write(lines.joinToString("\n").toByteArray(Charsets.UTF_8))
                out.flush()
            }
            archiveOperation.value = BackupOperationState(inProgress = false, completedMessage = "Unmatched list saved successfully.")
        } catch (e: Exception) {
            archiveOperation.value = BackupOperationState(inProgress = false, errorMessage = "Failed to save unmatched list: ${e.message}")
        }
    }

    fun toggleBackupCategory(cat: BackupCategory) {
        selectedBackupCategories.update { current ->
            if (cat in current) if (current.size > 1) current - cat else current else current + cat
        }
    }

    fun setConflictMode(mode: RestoreConflictMode) {
        selectedRestoreConflictMode.value = mode
    }

    fun createArchiveBackup(uri: Uri) = scope.launch {
        archiveOperation.value = BackupOperationState(inProgress = true, stage = "Starting archive backup...")
        val res = archiveBackup.createArchive(
            targetUri = uri,
            selectedCategories = selectedBackupCategories.value,
            onProgress = { stage, frac ->
                archiveOperation.value = BackupOperationState(inProgress = true, stage = stage, progressFraction = frac)
            }
        )
        if (res.isSuccess) {
            val rec = res.getOrNull()
            lastBackupRecord.value = rec
            archiveOperation.value = BackupOperationState(
                inProgress = false,
                completedMessage = "Backup saved successfully (${rec?.fileName}, ${rec?.recordCounts?.values?.sum() ?: 0} records)"
            )
        } else {
            val err = res.exceptionOrNull()?.message ?: "Unknown error"
            archiveOperation.value = BackupOperationState(inProgress = false, errorMessage = "Backup failed: $err")
        }
    }

    fun inspectArchive(uri: Uri, fileName: String) = scope.launch {
        archiveOperation.value = BackupOperationState(inProgress = true, stage = "Inspecting archive structure and checksums...")
        val res = archiveRestore.inspect(uri, fileName)
        archiveOperation.value = BackupOperationState(inProgress = false)
        if (res.isSuccess) {
            archivePreview.value = res.getOrNull()
        } else {
            val err = res.exceptionOrNull()?.message ?: "Inspection failed"
            archiveOperation.value = BackupOperationState(inProgress = false, errorMessage = err)
        }
    }

    fun executeArchiveRestore(uri: Uri, fileName: String, categories: Set<BackupCategory>, mode: RestoreConflictMode) = scope.launch {
        archivePreview.value = null
        archiveOperation.value = BackupOperationState(inProgress = true, stage = "Restoring from archive...")
        val res = archiveRestore.restore(
            uri = uri,
            fileName = fileName,
            selectedCategories = categories,
            conflictMode = mode,
            onProgress = { stage, frac ->
                archiveOperation.value = BackupOperationState(inProgress = true, stage = stage, progressFraction = frac)
            }
        )
        archiveOperation.value = BackupOperationState(inProgress = false)
        if (res.isSuccess) {
            archiveReport.value = res.getOrNull()
            refreshStats()
        } else {
            val err = res.exceptionOrNull()?.message ?: "Restore failed"
            archiveOperation.value = BackupOperationState(inProgress = false, errorMessage = err)
        }
    }

    fun dismissArchivePreview() { archivePreview.value = null }
    fun dismissArchiveReport() { archiveReport.value = null }
}
