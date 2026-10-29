package com.pulse.player.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pulse.player.app
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
import com.pulse.player.data.backup.copyToTemp
import com.pulse.player.data.backup.exportCompat
import com.pulse.player.data.backup.previewRestore
import com.pulse.player.data.backup.sniff
import com.pulse.player.data.backup.stage
import com.pulse.player.data.db.PlaylistEntity
import com.pulse.player.data.db.TrackEntity
import com.pulse.player.data.favorites.FavItem
import com.pulse.player.data.favorites.FavoritesRepository
import com.pulse.player.data.scan.SampleMusicHelper
import com.pulse.player.data.settings.ThemeMode
import com.pulse.player.playback.Track
import com.pulse.player.playback.toTrack
import com.pulse.player.ui.screens.Screen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.pulse.player.data.ai.AudioRecorderHelper
import com.pulse.player.data.ai.GeminiAudioClient
import com.pulse.player.data.ai.GeneratedMusicResult
import com.pulse.player.data.ai.TranscriptionResult
import com.pulse.player.data.scan.norm
import java.io.File
import java.util.ArrayDeque
import java.util.Calendar

sealed interface TranscriptionState {
    data object Idle : TranscriptionState
    data object Recording : TranscriptionState
    data class Transcribing(val progressText: String = "Transcribing with gemini-3.5-transcribe...") : TranscriptionState
    data class Success(val result: TranscriptionResult, val audioFile: File?, val timestamp: Long = System.currentTimeMillis()) : TranscriptionState
    data class Error(val message: String) : TranscriptionState
}

sealed interface MusicGenState {
    data object Idle : MusicGenState
    data class Generating(val prompt: String, val model: String) : MusicGenState
    data class Success(val result: GeneratedMusicResult, val track: Track) : MusicGenState
    data class Error(val message: String) : MusicGenState
}

enum class SortField { TITLE, ARTIST, DATE_ADDED, DURATION }

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application.app
    val player = app.player
    val db = app.db
    val settings = app.settings
    private val scanner = app.scanner
    val favoritesRepo = app.favoritesRepo
    val archiveBackup: ArchiveBackupService = app.archiveBackup
    val archiveRestore: ArchiveRestoreService = app.archiveRestore

    // Navigation
    private val backStack = ArrayDeque<Screen>().apply { push(Screen.Home) }
    private val _screen = MutableStateFlow<Screen>(Screen.Home)
    val screen: StateFlow<Screen> = _screen.asStateFlow()

    private val _nowPlayingOpen = MutableStateFlow(false)
    val nowPlayingOpen: StateFlow<Boolean> = _nowPlayingOpen.asStateFlow()

    fun go(s: Screen) {
        backStack.push(s)
        _screen.value = s
    }

    fun back(): Boolean {
        if (backStack.size > 1) {
            backStack.pop()
            _screen.value = backStack.peek() ?: Screen.Home
            return true
        }
        return false
    }

    fun openNowPlaying() { _nowPlayingOpen.value = true }
    fun closeNowPlaying() { _nowPlayingOpen.value = false }

    // Sheets & Dialogs
    val queueOpen = MutableStateFlow(false)
    val sleepOpen = MutableStateFlow(false)
    val addToPlaylistOpen = MutableStateFlow<Track?>(null)
    val trackMenuTrack = MutableStateFlow<Track?>(null)
    val createPlaylistOpen = MutableStateFlow(false)
    val importPreview = MutableStateFlow<FavoritesRepository.Preview?>(null)
    val songDetailsTrack = MutableStateFlow<Track?>(null)

    fun openQueue() { queueOpen.value = true }
    fun closeQueue() { queueOpen.value = false }
    fun openSleepSheet() { sleepOpen.value = true }
    fun openSleep() { openSleepSheet() }
    fun closeSleepSheet() { sleepOpen.value = false }
    fun openAddToPlaylist(t: Track? = null) { addToPlaylistOpen.value = t ?: currentPlayingTrack() }
    fun closeAddToPlaylist() { addToPlaylistOpen.value = null }
    fun openTrackMenu(t: Track) { trackMenuTrack.value = t }
    fun closeTrackMenu() { trackMenuTrack.value = null }

    // Settings
    val themeMode: StateFlow<ThemeMode> = settings.themeMode.stateIn(viewModelScope, SharingStarted.Eagerly, ThemeMode.DARK)
    val skinId: StateFlow<String> = settings.skinId.stateIn(viewModelScope, SharingStarted.Eagerly, "plum")
    val playerThemeId: StateFlow<String> = settings.playerThemeId.stateIn(viewModelScope, SharingStarted.Eagerly, "classic")
    val wallpaperPath: StateFlow<String?> = settings.wallpaperPath.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val reducedMotion: StateFlow<Boolean> = settings.reducedMotion.stateIn(viewModelScope, SharingStarted.Eagerly, false)
    val fadeEnabled: StateFlow<Boolean> = settings.fadeEnabled.stateIn(viewModelScope, SharingStarted.Eagerly, true)

    fun setThemeMode(m: ThemeMode) = viewModelScope.launch { settings.setTheme(m) }
    fun setSkin(id: String) = viewModelScope.launch { settings.setSkin(id) }
    fun applyPlayerTheme(id: String) = viewModelScope.launch { settings.setPlayerTheme(id) }
    fun setWallpaper(path: String?) = viewModelScope.launch { settings.setWallpaper(path) }
    fun setReducedMotion(v: Boolean) = viewModelScope.launch { settings.setReduced(v) }
    fun setFadeEnabled(v: Boolean) = viewModelScope.launch {
        settings.setFade(v)
        player.fadeEnabled = v
    }

    // Scanning & Tracks
    val scanning = scanner.scanning
    val scanProgress = scanner.progress

    val tracks: StateFlow<List<Track>> = db.tracks().observeAll(minMs = 0L)
        .map { list -> list.map { it.toTrack() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Sorting
    val sortField = MutableStateFlow(SortField.TITLE)
    val sortAscending = MutableStateFlow(true)

    val sortedTracks: StateFlow<List<Track>> = combine(tracks, sortField, sortAscending) { list, field, asc ->
        val sorted = when (field) {
            SortField.TITLE -> list.sortedBy { it.title.lowercase() }
            SortField.ARTIST -> list.sortedBy { it.artist.lowercase() }
            SortField.DATE_ADDED -> list.sortedByDescending { it.dateAddedMs }
            SortField.DURATION -> list.sortedByDescending { it.durationMs }
        }
        if (!asc && field != SortField.DATE_ADDED && field != SortField.DURATION) sorted.reversed() else sorted
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Selection
    val selected = MutableStateFlow<Set<String>>(emptySet())
    val selectMode = MutableStateFlow(false)

    fun toggleSelectMode() {
        selectMode.value = !selectMode.value
        if (!selectMode.value) selected.value = emptySet()
    }

    fun toggleSelect(id: String) {
        selected.update { if (id in it) it - id else it + id }
        if (!selectMode.value && selected.value.isNotEmpty()) selectMode.value = true
    }

    fun selectAll() {
        selected.value = tracks.value.map { it.id }.toSet()
    }

    fun clearSelect() {
        selected.value = emptySet()
        selectMode.value = false
    }

    // Favorites
    val favoriteCount = db.favorites().observeIds().map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val isCurrentFavorite: StateFlow<Boolean> = combine(player.state, db.favorites().observeIds()) { ps, favs ->
        ps.mediaId != null && ps.mediaId in favs
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun toggleFavorite(id: String) = viewModelScope.launch(Dispatchers.IO) {
        db.favorites().toggle(id, System.currentTimeMillis())
    }

    fun toggleFavoriteCurrent() {
        player.state.value.mediaId?.let { toggleFavorite(it) }
    }

    // Playlists
    val playlists: StateFlow<List<PlaylistEntity>> = db.playlists().observePlaylists()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Smart Playlists
    val neverPlayed: StateFlow<List<Track>> = db.tracks().neverPlayed()
        .map { list -> list.map { it.toTrack() } }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val forgottenFavorites: StateFlow<List<Track>> = db.tracks()
        .forgottenFavorites(since = System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000L)
        .map { list -> list.map { it.toTrack() } }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val longTracks: StateFlow<List<Track>> = db.tracks().longTracks()
        .map { list -> list.map { it.toTrack() } }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun createPlaylist(name: String) = viewModelScope.launch(Dispatchers.IO) {
        if (name.isNotBlank()) {
            db.playlists().create(PlaylistEntity(name = name.trim(), createdAtMs = System.currentTimeMillis()))
        }
    }

    fun deletePlaylist(id: Long) = viewModelScope.launch(Dispatchers.IO) {
        db.playlists().delete(id)
    }

    fun addTrackToPlaylist(playlistId: Long, trackId: String) = viewModelScope.launch(Dispatchers.IO) {
        val next = db.playlists().nextPos(playlistId)
        db.playlists().addTrack(
            com.pulse.player.data.db.PlaylistTrackEntity(
                playlistId = playlistId,
                trackId = trackId,
                position = next,
                addedAtMs = System.currentTimeMillis()
            )
        )
    }

    fun addSelectedToPlaylist(playlistId: Long) = viewModelScope.launch(Dispatchers.IO) {
        val sel = selected.value
        sel.forEach { trackId ->
            val next = db.playlists().nextPos(playlistId)
            db.playlists().addTrack(
                com.pulse.player.data.db.PlaylistTrackEntity(
                    playlistId = playlistId,
                    trackId = trackId,
                    position = next,
                    addedAtMs = System.currentTimeMillis()
                )
            )
        }
        clearSelect()
    }

    fun favoriteSelected() = viewModelScope.launch(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val favs = selected.value.map { com.pulse.player.data.db.FavoriteEntity(it, now) }
        db.favorites().addAll(favs)
        clearSelect()
    }

    // For You / Recap stats
    val todayMinutes = MutableStateFlow(0)
    val streakDays = MutableStateFlow(0)
    val topPicks = MutableStateFlow<List<Track>>(emptyList())
    val resume = MutableStateFlow<Track?>(null)

    // Last backup info
    val lastBackupJson = MutableStateFlow<String?>(null)

    init {
        refreshStats()
        restoreLastQueue()
        refreshLastBackup()
    }

    fun refreshLastBackup() = viewModelScope.launch {
        lastBackupJson.value = settings.lastBackup()
    }

    fun refreshStats() = viewModelScope.launch(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val startOfDay = now - (now % 86_400_000L)
        val todayMs = db.stats().totalMs(startOfDay, now)
        todayMinutes.value = (todayMs / 60_000L).toInt()

        val days = db.stats().playDays()
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
        val cal = java.util.Calendar.getInstance()
        val today = sdf.format(cal.time)
        cal.add(java.util.Calendar.DAY_OF_YEAR, -1)
        val yesterday = sdf.format(cal.time)

        val first = days.firstOrNull()
        var s = 0
        if (first == today || first == yesterday) {
            val daySet = days.toSet()
            val checkCal = java.util.Calendar.getInstance()
            if (first == yesterday) {
                checkCal.add(java.util.Calendar.DAY_OF_YEAR, -1)
            }
            while (true) {
                val dStr = sdf.format(checkCal.time)
                if (dStr in daySet) {
                    s++
                    checkCal.add(java.util.Calendar.DAY_OF_YEAR, -1)
                } else {
                    break
                }
            }
        }
        streakDays.value = s

        val topIds = db.stats().observeTopIds(10).first()
        if (topIds.isNotEmpty()) {
            val entities = db.tracks().byIds(topIds).associateBy { it.id }
            topPicks.value = topIds.mapNotNull { entities[it]?.let { e -> e.toTrackWithFav(false).toTrack() } }
        }

        val recentIds = db.stats().observeRecentIds(1).first()
        if (recentIds.isNotEmpty()) {
            db.tracks().byId(recentIds.first())?.let {
                resume.value = it.toTrackWithFav(false).toTrack()
            }
        }
    }

    private fun TrackEntity.toTrackWithFav(fav: Boolean) = com.pulse.player.data.db.TrackWithFav(this, fav)

    // Queue commands
    fun playQueue(tracks: List<Track>, startIndex: Int, shuffle: Boolean = false) {
        player.playQueue(tracks, startIndex, shuffle)
    }

    fun playNext(t: Track) { player.playNext(t) }
    fun addToQueue(t: Track) { player.addToQueue(t) }

    fun playExternal(uri: Uri) {
        player.playExternal(uri)
        openNowPlaying()
    }

    fun playTrackById(id: String) = viewModelScope.launch(Dispatchers.IO) {
        db.tracks().byId(id)?.let { entity ->
            withContext(Dispatchers.Main) {
                player.playTrack(entity.toTrackWithFav(false).toTrack())
                openNowPlaying()
            }
        }
    }

    fun playSelectedNext() {
        val sel = selected.value
        val items = tracks.value.filter { it.id in sel }
        items.forEach { player.playNext(it) }
        clearSelect()
    }

    private fun currentPlayingTrack(): Track? {
        val curId = player.state.value.mediaId ?: return null
        return tracks.value.firstOrNull { it.id == curId }
    }

    // Scan
    fun scan() = viewModelScope.launch {
        val minDur = settings.minDurationSec.first() * 1000L
        val excluded = settings.excludedFolders.first()
        scanner.scanMediaStore(minDur, excluded)
        val trees = settings.safTrees.first()
        trees.forEach { uriStr ->
            runCatching { scanner.scanSafTree(Uri.parse(uriStr), minDur) }
        }
        refreshStats()
    }

    fun addSafFolder(uri: Uri) = viewModelScope.launch {
        settings.addSafTree(uri.toString())
        scan()
    }
    fun scanTree(uri: Uri) = addSafFolder(uri)

    // Backup & Transfer
    val exportStatus = MutableStateFlow<String?>(null)

    fun exportFavorites(uri: Uri) = viewModelScope.launch {
        val res = favoritesRepo.export(uri)
        exportStatus.value = if (res.isSuccess) "Exported ${res.getOrNull()} favourites" else "Export failed: ${res.exceptionOrNull()?.message}"
    }

    fun previewFavoritesImport(uri: Uri, name: String) = viewModelScope.launch {
        val res = favoritesRepo.preview(uri, name)
        if (res.isSuccess) {
            importPreview.value = res.getOrNull()
        } else {
            exportStatus.value = "Import preview failed: ${res.exceptionOrNull()?.message}"
        }
    }

    fun applyImport(preview: FavoritesRepository.Preview, resolved: Map<FavItem, TrackEntity>, replace: Boolean) = viewModelScope.launch {
        val count = favoritesRepo.apply(preview, resolved, replace)
        exportStatus.value = "Imported $count favourites successfully"
        importPreview.value = null
    }

    // Compatible Archive Backup & Restore (Prompt 9, 10 & 11)
    val compatPreview = MutableStateFlow<CompatRestorePreview?>(null)
    val compatReport = MutableStateFlow<CompatRestoreReport?>(null)
    val compatSelectedCategories = MutableStateFlow<Set<String>>(Compat.FLAGS.toSet())
    val compatReplaceMode = MutableStateFlow(false)
    val compatExportCats = MutableStateFlow(ExportCats())
    val compatExportAsZip = MutableStateFlow(false)

    fun toggleCompatCategory(flag: String) {
        compatSelectedCategories.update { current ->
            if (flag in current) {
                if (current.size > 1) current - flag else current
            } else {
                current + flag
            }
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

    fun executeCompatExport(destUri: Uri) = viewModelScope.launch {
        archiveOperation.value = BackupOperationState(inProgress = true, stage = "Starting compatible backup export...")
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
            archiveOperation.value = BackupOperationState(
                inProgress = false,
                errorMessage = "Export failed: $err"
            )
        }
    }

    fun smartInspect(uri: Uri, fileName: String) = viewModelScope.launch {
        archiveOperation.value = BackupOperationState(inProgress = true, stage = "Sniffing backup format...")
        val sniffed = sniff(app, uri)
        when (sniffed) {
            Sniffed.REAL_WAV -> {
                archiveOperation.value = BackupOperationState(inProgress = false, errorMessage = "This is a real audio file, not a backup.")
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
                            archiveOperation.value = BackupOperationState(inProgress = false, errorMessage = "This ZIP is not a recognised backup.")
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
                    archiveOperation.value = BackupOperationState(inProgress = false, errorMessage = e.message ?: "Failed to read backup archive.")
                }
            }
            Sniffed.OTHER -> {
                archiveOperation.value = BackupOperationState(inProgress = false, errorMessage = "This file is not a recognised backup.")
            }
        }
    }

    fun inspectSampleReferenceBackup() = viewModelScope.launch {
        archiveOperation.value = BackupOperationState(inProgress = true, stage = "Loading reference sample backup...")
        try {
            val assetStream = app.assets.open("ManualBackup_20261028_172204175.wav")
            val tempFile = File.createTempFile("sample_ref_", ".wav", app.cacheDir)
            assetStream.use { input ->
                tempFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            val uri = Uri.fromFile(tempFile)
            smartInspect(uri, "ManualBackup_20261028_172204175.wav")
        } catch (e: Exception) {
            archiveOperation.value = BackupOperationState(inProgress = false, errorMessage = "Could not open reference backup: ${e.message}")
        }
    }

    fun executeCompatRestore(staged: StagedBackup, categories: Set<String>, replace: Boolean) = viewModelScope.launch {
        compatPreview.value = null
        archiveOperation.value = BackupOperationState(inProgress = true, stage = "Restoring data...")
        try {
            val report = applyRestore(app, db, settings, staged, RestoreOptions(categories, replace))
            archiveOperation.value = BackupOperationState(inProgress = false)
            compatReport.value = report
            refreshStats()
        } catch (e: Exception) {
            archiveOperation.value = BackupOperationState(inProgress = false, errorMessage = "Restore failed: ${e.message}")
        }
    }

    fun dismissCompatPreview() {
        compatPreview.value = null
    }

    fun dismissCompatReport() {
        compatReport.value = null
    }

    fun saveUnmatchedReport(destUri: Uri, lines: List<String>) = viewModelScope.launch {
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

    // Option B Portable Archive (Preserved for compatibility)
    val archiveOperation = MutableStateFlow(BackupOperationState())
    val archivePreview = MutableStateFlow<RestorePreview?>(null)
    val archiveReport = MutableStateFlow<RestoreReport?>(null)
    val lastBackupRecord = MutableStateFlow<LastBackupRecord?>(null)
    val selectedBackupCategories = MutableStateFlow<Set<BackupCategory>>(BackupCategory.values().toSet())
    val selectedRestoreConflictMode = MutableStateFlow(RestoreConflictMode.MERGE)

    fun toggleBackupCategory(cat: BackupCategory) {
        selectedBackupCategories.update { current ->
            if (cat in current) {
                if (current.size > 1) current - cat else current
            } else {
                current + cat
            }
        }
    }

    fun setConflictMode(mode: RestoreConflictMode) {
        selectedRestoreConflictMode.value = mode
    }

    fun createArchiveBackup(uri: Uri) = viewModelScope.launch {
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
            archiveOperation.value = BackupOperationState(
                inProgress = false,
                errorMessage = "Backup failed: $err"
            )
        }
    }

    fun inspectArchive(uri: Uri, fileName: String) = viewModelScope.launch {
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

    fun executeArchiveRestore(uri: Uri, fileName: String, categories: Set<BackupCategory>, mode: RestoreConflictMode) = viewModelScope.launch {
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

    fun dismissArchivePreview() {
        archivePreview.value = null
    }

    fun dismissArchiveReport() {
        archiveReport.value = null
    }

    fun seedSampleTrack() = viewModelScope.launch {
        SampleMusicHelper.installSampleTrack(getApplication(), db)
    }

    // Search
    val searchQuery = MutableStateFlow("")

    val searchResults: StateFlow<List<Track>> = combine(tracks, searchQuery) { list, q ->
        if (q.isBlank()) emptyList()
        else list.filter {
            it.title.contains(q, ignoreCase = true) ||
                    it.artist.contains(q, ignoreCase = true) ||
                    it.album.contains(q, ignoreCase = true)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Queue restore
    private fun restoreLastQueue() = viewModelScope.launch {
        val (ids, index, pos) = settings.queue.first()
        if (ids.isNotEmpty()) {
            val entities = db.tracks().byIds(ids).associateBy { it.id }
            val resolvedTracks = ids.mapNotNull { entities[it]?.let { e -> e.toTrackWithFav(false).toTrack() } }
            if (resolvedTracks.isNotEmpty()) {
                player.restore(ids, index, pos, resolvedTracks)
            }
        }
    }

    // AI Audio Transcription & Lyria Music Generation
    private val audioRecorder = AudioRecorderHelper(viewModelScope)
    val isRecording = audioRecorder.isRecording
    val recordingDurationSec = audioRecorder.durationSec
    val recordingAmplitude = audioRecorder.maxAmplitude
    private var currentRecordedFile: File? = null

    val transcriptionState = MutableStateFlow<TranscriptionState>(TranscriptionState.Idle)
    val musicGenState = MutableStateFlow<MusicGenState>(MusicGenState.Idle)

    fun startAudioRecording(): Boolean {
        val res = audioRecorder.start(getApplication())
        return if (res.isSuccess) {
            currentRecordedFile = res.getOrNull()
            transcriptionState.value = TranscriptionState.Recording
            true
        } else {
            val err = res.exceptionOrNull()?.message ?: "Failed to start microphone recording"
            transcriptionState.value = TranscriptionState.Error(err)
            false
        }
    }

    fun stopAndTranscribeAudio(customPrompt: String? = null) {
        val file = audioRecorder.stop() ?: currentRecordedFile
        if (file == null || !file.exists() || file.length() == 0L) {
            transcriptionState.value = TranscriptionState.Error("No audio was recorded.")
            return
        }
        viewModelScope.launch {
            transcriptionState.value = TranscriptionState.Transcribing("Transcribing audio with gemini-3.5-transcribe...")
            val apiKey = settings.effectiveGeminiApiKey()
            val result = GeminiAudioClient.transcribeAudio(file, apiKey, customPrompt)
            if (result.isSuccess) {
                transcriptionState.value = TranscriptionState.Success(result.getOrThrow(), file)
            } else {
                val err = result.exceptionOrNull()?.message ?: "Transcription failed"
                transcriptionState.value = TranscriptionState.Error(err)
            }
        }
    }

    fun cancelAudioRecording() {
        audioRecorder.cancel()
        currentRecordedFile = null
        transcriptionState.value = TranscriptionState.Idle
    }

    fun resetTranscriptionState() {
        transcriptionState.value = TranscriptionState.Idle
    }

    fun transcribeTrack(track: Track, customPrompt: String? = null) = viewModelScope.launch {
        transcriptionState.value = TranscriptionState.Transcribing("Transcribing '${track.title}' with gemini-3.5-transcribe...")
        val apiKey = settings.effectiveGeminiApiKey()
        val ctx = getApplication<Application>()
        val tempFile = File(ctx.cacheDir, "track_transcribe_${System.currentTimeMillis()}.mp3")
        val copied = runCatching {
            ctx.contentResolver.openInputStream(Uri.parse(track.uri))?.use { input ->
                tempFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            tempFile.exists() && tempFile.length() > 0
        }.getOrDefault(false)

        if (!copied) {
            transcriptionState.value = TranscriptionState.Error("Unable to read audio file for ${track.title}.")
            return@launch
        }

        val res = GeminiAudioClient.transcribeAudio(tempFile, apiKey, customPrompt)
        if (res.isSuccess) {
            transcriptionState.value = TranscriptionState.Success(res.getOrThrow(), tempFile)
        } else {
            transcriptionState.value = TranscriptionState.Error(res.exceptionOrNull()?.message ?: "Transcription failed")
        }
    }

    fun saveLyricsForTrack(trackId: String, lyricsText: String): Boolean {
        return runCatching {
            val ctx = getApplication<Application>()
            val dir = File(ctx.filesDir, "lyrics").apply { mkdirs() }
            val safeId = trackId.replace(Regex("[^a-zA-Z0-9._-]"), "_")
            File(dir, "$safeId.lrc").writeText(lyricsText)
            true
        }.getOrDefault(false)
    }

    fun generateMusic(prompt: String, model: String = "lyria-3-clip-preview") = viewModelScope.launch {
        musicGenState.value = MusicGenState.Generating(prompt, model)
        val apiKey = settings.effectiveGeminiApiKey()
        val ctx = getApplication<Application>()
        val res = GeminiAudioClient.generateMusic(ctx, prompt, model, apiKey)
        if (res.isSuccess) {
            val genResult = res.getOrThrow()
            val mmr = android.media.MediaMetadataRetriever()
            val durationMs = runCatching {
                mmr.setDataSource(genResult.file.absolutePath)
                mmr.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 30_000L
            }.getOrDefault(30_000L)
            runCatching { mmr.release() }

            val trackId = "gen:${genResult.file.name}"
            val uriStr = Uri.fromFile(genResult.file).toString()
            val now = System.currentTimeMillis()
            val entity = TrackEntity(
                id = trackId,
                uri = uriStr,
                displayName = genResult.file.name,
                title = genResult.title,
                artist = "Google Lyria ($model)",
                album = "AI Generated Tracks",
                albumArtist = "Google Lyria",
                durationMs = durationMs,
                sizeBytes = genResult.file.length(),
                mimeType = "audio/mp3",
                folderPath = genResult.file.parent ?: "ai_music",
                dateAddedMs = now,
                dateModifiedMs = now,
                trackNumber = 1,
                year = Calendar.getInstance().get(Calendar.YEAR),
                normTitle = norm(genResult.title),
                normArtist = norm("Google Lyria"),
                normAlbum = norm("AI Generated Tracks"),
                available = true,
                dataPath = genResult.file.absolutePath
            )
            db.tracks().upsertAll(listOf(entity))
            val newTrack = entity.toTrackWithFav(false).toTrack()
            musicGenState.value = MusicGenState.Success(genResult, newTrack)
        } else {
            val err = res.exceptionOrNull()?.message ?: "Music generation failed"
            musicGenState.value = MusicGenState.Error(err)
        }
    }

    fun resetMusicGenState() {
        musicGenState.value = MusicGenState.Idle
    }

    fun setGeminiApiKey(key: String) = viewModelScope.launch {
        settings.setGeminiApiKey(key)
    }
}

