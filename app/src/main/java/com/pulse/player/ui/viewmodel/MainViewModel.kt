package com.pulse.player.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.palette.graphics.Palette
import com.pulse.player.app
import com.pulse.player.data.AppError
import com.pulse.player.data.CrashLogStore
import com.pulse.player.data.CrashRecord
import com.pulse.player.data.ErrorCategory
import com.pulse.player.data.ErrorDispatcher
import com.pulse.player.data.backup.BackupCategory
import com.pulse.player.data.backup.BackupOperationState
import com.pulse.player.data.backup.CompatRestorePreview
import com.pulse.player.data.backup.CompatRestoreReport
import com.pulse.player.data.backup.ExportCats
import com.pulse.player.data.backup.LastBackupRecord
import com.pulse.player.data.backup.RestoreConflictMode
import com.pulse.player.data.backup.RestorePreview
import com.pulse.player.data.backup.RestoreReport
import com.pulse.player.data.backup.StagedBackup
import com.pulse.player.data.backup.classic.FavoritesRepository
import com.pulse.player.data.db.BookmarkEntity
import com.pulse.player.data.db.GenreStat
import com.pulse.player.data.db.PlaylistEntity
import com.pulse.player.data.db.SavedQueueEntity
import com.pulse.player.data.db.SmartPlaylistEntity
import com.pulse.player.data.db.TrackEntity
import com.pulse.player.data.db.TrackWithFav
import com.pulse.player.data.favorites.FavItem
import com.pulse.player.data.lyrics.LrcLine
import com.pulse.player.data.scan.ArtworkLoader
import com.pulse.player.data.scan.SampleMusicHelper
import com.pulse.player.data.scan.ScanState
import com.pulse.player.data.scan.norm
import com.pulse.player.data.settings.ColorSource
import com.pulse.player.data.settings.DeviceDspRule
import com.pulse.player.data.settings.FontStylePreset
import com.pulse.player.data.settings.ReplayGainMode
import com.pulse.player.data.settings.SeekStylePreset
import com.pulse.player.data.settings.SoundProfile
import com.pulse.player.data.settings.ThemeMode
import com.pulse.player.library.FolderTreeState
import com.pulse.player.library.SmartPlaylistEngine
import com.pulse.player.net.GeneratedMusicResult
import com.pulse.player.net.NetworkGateway
import com.pulse.player.net.TranscriptionResult
import com.pulse.player.playback.AudioFx
import com.pulse.player.playback.AudioFxStatus
import com.pulse.player.playback.AudioOutputDevice
import com.pulse.player.playback.LoudnessScanState
import com.pulse.player.playback.PlaybackEngine
import com.pulse.player.playback.Track
import com.pulse.player.playback.toTrack
import com.pulse.player.ui.screens.AlbumItem
import com.pulse.player.ui.screens.ArtistItem
import com.pulse.player.ui.screens.FolderItem
import com.pulse.player.ui.screens.PlayerLayoutMode
import com.pulse.player.ui.screens.Screen
import com.pulse.player.ui.theme.immersive.QueueItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.ArrayDeque
import java.util.Calendar
import com.pulse.player.ui.theme.Pulse as PulseColors

sealed interface TranscriptionState {
    data object Idle : TranscriptionState
    data object Recording : TranscriptionState
    data class Transcribing(val progressText: String = "Transcribing audio & syncing lyrics…") : TranscriptionState
    data class Success(val result: TranscriptionResult, val audioFile: File?, val timestamp: Long = System.currentTimeMillis()) : TranscriptionState
    data class Error(val message: String) : TranscriptionState
}

data class SnackbarActionEvent(
    val message: String,
    val actionLabel: String? = null,
    val onAction: (() -> Unit)? = null
)

sealed interface MusicGenState {
    data object Idle : MusicGenState
    data class Generating(val prompt: String, val model: String) : MusicGenState
    data class Success(val result: GeneratedMusicResult, val track: Track) : MusicGenState
    data class Error(val message: String) : MusicGenState
}

enum class SortField(val label: String) {
    TITLE("Title"),
    ARTIST("Artist"),
    DATE_ADDED("Date Added"),
    DURATION("Duration")
}

enum class EntitySortMode(val label: String) {
    NAME_ASC("Name (A–Z)"),
    NAME_DESC("Name (Z–A)"),
    COUNT_DESC("Most Songs")
}

enum class SearchScope(val label: String) {
    ALL("All"),
    SONGS("Songs"),
    ARTISTS("Artists"),
    ALBUMS("Albums"),
    FOLDERS("Folders"),
    FAVORITES("Favourites")
}

data class LibraryFilterState(
    val favoritesOnly: Boolean = false,
    val hiResOnly: Boolean = false,
    val longOnly: Boolean = false,
    val unplayedOnly: Boolean = false
) {
    val activeCount: Int
        get() = listOf(favoritesOnly, hiResOnly, longOnly, unplayedOnly).count { it }
}

data class SmartPlaylistBuilderTarget(
    val existing: SmartPlaylistEntity? = null
)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application.app
    val player = app.player
    val db = app.db
    val settings = app.settings
    private val networkGateway: NetworkGateway = app.networkGateway
    private val scanner = app.scanner
    val favoritesRepo = app.favoritesRepo

    // Navigation & Scroll/Filter Preservation
    private val backStack = ArrayDeque<Screen>().apply { push(Screen.Home) }
    private val _screen = MutableStateFlow<Screen>(Screen.Home)
    val screen: StateFlow<Screen> = _screen.asStateFlow()

    private val _navForward = MutableStateFlow(true)
    val navForward: StateFlow<Boolean> = _navForward.asStateFlow()

    private val _nowPlayingOpen = MutableStateFlow(false)
    val nowPlayingOpen: StateFlow<Boolean> = _nowPlayingOpen.asStateFlow()

    val activeHomeTab = MutableStateFlow("For you")
    val settingsSearchQuery = MutableStateFlow("")
    val settingsCategoryName = MutableStateFlow("ALL")
    val customizeSelectedTab = MutableStateFlow(0)
    val backupSelectedTab = MutableStateFlow(0)

    private val listStates = object : LinkedHashMap<String, LazyListState>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, LazyListState>?): Boolean = size > 24
    }
    private val gridStates = object : LinkedHashMap<String, LazyGridState>(8, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, LazyGridState>?): Boolean = size > 12
    }

    fun lazyListStateFor(key: String): LazyListState = synchronized(listStates) {
        listStates.getOrPut(key) { LazyListState() }
    }

    fun lazyGridStateFor(key: String): LazyGridState = synchronized(gridStates) {
        gridStates.getOrPut(key) { LazyGridState() }
    }

    fun selectHomeTab(name: String) {
        activeHomeTab.value = name
        _nowPlayingOpen.value = false
        if (_screen.value !is Screen.Home) {
            _navForward.value = true
            backStack.clear()
            backStack.push(Screen.Home)
            _screen.value = Screen.Home
        }
    }

    fun go(s: Screen) {
        _nowPlayingOpen.value = false
        _navForward.value = true
        if (s is Screen.Home) {
            backStack.clear()
            backStack.push(Screen.Home)
            _screen.value = Screen.Home
            return
        }
        if (_screen.value == s) return
        backStack.push(s)
        _screen.value = s
    }

    fun back(): Boolean {
        if (backStack.size > 1) {
            _navForward.value = false
            backStack.pop()
            _screen.value = backStack.peek() ?: Screen.Home
            return true
        }
        return false
    }

    fun openNowPlaying() { _nowPlayingOpen.value = true }
    fun closeNowPlaying() { _nowPlayingOpen.value = false }

    // Feedback & Overlay Sheets
    val snackbarMessage = MutableStateFlow<String?>(null)
    fun clearSnackbarMessage() { snackbarMessage.value = null }
    private val _snackbarActions = MutableSharedFlow<SnackbarActionEvent>(extraBufferCapacity = 4)
    val snackbarActions: SharedFlow<SnackbarActionEvent> = _snackbarActions.asSharedFlow()

    val drivingModeOpen = MutableStateFlow(false)
    fun openDrivingMode() {
        _nowPlayingOpen.value = false
        drivingModeOpen.value = true
    }
    fun closeDrivingMode() { drivingModeOpen.value = false }

    val smartPlaylistBuilderTarget = MutableStateFlow<SmartPlaylistBuilderTarget?>(null)
    fun openSmartPlaylistBuilder(existing: SmartPlaylistEntity? = null) {
        smartPlaylistBuilderTarget.value = SmartPlaylistBuilderTarget(existing)
    }
    fun closeSmartPlaylistBuilder() { smartPlaylistBuilderTarget.value = null }

    val playerLayoutPickerOpen = MutableStateFlow(false)
    fun openPlayerLayoutPicker() { playerLayoutPickerOpen.value = true }
    fun closePlayerLayoutPicker() { playerLayoutPickerOpen.value = false }

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
    fun selectAll(ids: Collection<String> = tracks.value.map { it.id }) { selected.value = ids.toSet() }
    fun clearSelect() {
        selected.value = emptySet()
        selectMode.value = false
    }

    // Coordinators
    private val fxCoordinator = PlaybackFxCoordinator(settings, player, viewModelScope) { snackbarMessage.value = it }
    private val lyricsCoordinator = LyricsBookmarkCoordinator(app, db, viewModelScope) { snackbarMessage.value = it }
    private val importExportCoordinator = LibraryImportExportCoordinator(app, db, player, viewModelScope) { snackbarMessage.value = it }
    private val backupCoordinator = LibraryBackupCoordinator(
        app = app,
        db = db,
        settings = settings,
        favoritesRepo = favoritesRepo,
        archiveBackup = app.archiveBackup,
        archiveRestore = app.archiveRestore,
        scope = viewModelScope,
        onSnackbar = { snackbarMessage.value = it }
    )

    // Library & Tracks
    val scanning: StateFlow<Boolean> = scanner.scanning.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val scanProgress: StateFlow<Int> = scanner.progress.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
    val scanState: StateFlow<ScanState> = scanner.state
    val audioPermissionDenied = MutableStateFlow(false)
    val scanWarningDismissed = MutableStateFlow(false)
    fun setAudioPermissionDenied(denied: Boolean) { audioPermissionDenied.value = denied }
    fun dismissScanWarning() { scanWarningDismissed.value = true }

    val tracks: StateFlow<List<Track>> = db.tracks().observeAll(minMs = 0L)
        .conflate().map { list -> list.map { it.toTrack() } }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalLibraryMs: StateFlow<Long> = tracks
        .map { list -> list.sumOf { it.durationMs.coerceAtLeast(0L) } }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    val missingTracks: StateFlow<List<Track>> = db.tracks().observeMissing()
        .conflate().map { list -> list.map { it.toTrack() } }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val missingCount: StateFlow<Int> = db.tracks().observeMissingCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val showMissing = MutableStateFlow(false)
    fun toggleShowMissing() { showMissing.value = !showMissing.value }

    private val neverPlayedIds: StateFlow<Set<String>> = db.tracks().neverPlayed()
        .map { list -> list.mapTo(HashSet()) { it.track.id } }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

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

    private val browserCoordinator = LibraryBrowserCoordinator(
        db = db,
        settings = settings,
        player = player,
        tracks = tracks,
        neverPlayedIds = neverPlayedIds,
        missingTracks = missingTracks,
        scope = viewModelScope,
        onSnackbar = { snackbarMessage.value = it }
    )

    private val tagEditorCoordinator = TagEditorCoordinator(
        ctx = app,
        db = db,
        tracks = tracks,
        selected = selected,
        clearSelect = { clearSelect() },
        scope = viewModelScope,
        onSnackbar = { snackbarMessage.value = it }
    )

    private val queueCoordinator = QueuePlaybackCoordinator(
        db = db,
        player = player,
        tracks = tracks,
        neverPlayed = neverPlayed,
        selected = selected,
        clearSelect = { clearSelect() },
        splitMultiArtists = browserCoordinator.splitMultiArtists,
        artistSplitExceptions = browserCoordinator.artistSplitExceptions,
        folderTreeMode = browserCoordinator.folderViewTreeMode,
        scope = viewModelScope,
        onSnackbar = { snackbarMessage.value = it },
        onSnackbarAction = { _snackbarActions.tryEmit(it) },
        onOpenNowPlaying = { openNowPlaying() }
    )

    private val searchCoordinator = SearchCoordinator(
        db = db,
        settings = settings,
        tracks = tracks,
        albums = browserCoordinator.albums,
        artists = browserCoordinator.artists,
        folders = browserCoordinator.folders,
        sortField = browserCoordinator.sortField,
        sortAscending = browserCoordinator.sortAscending,
        sortTrackList = { list, field, asc -> browserCoordinator.sortTrackList(list, field, asc) },
        scope = viewModelScope
    )

    // Settings Flows
    val themeMode: StateFlow<ThemeMode> = settings.themeMode.stateIn(viewModelScope, SharingStarted.Eagerly, ThemeMode.DARK)
    val skinId: StateFlow<String> = settings.skinId.stateIn(viewModelScope, SharingStarted.Eagerly, "studio_amber")
    val playerThemeId: StateFlow<String> = settings.playerThemeId.stateIn(viewModelScope, SharingStarted.Eagerly, "atmospheric_queue")
    val activePlayerLayout: StateFlow<PlayerLayoutMode> = settings.playerLayoutId
        .map { PlayerLayoutMode.fromId(it) }.stateIn(viewModelScope, SharingStarted.Eagerly, PlayerLayoutMode.ATMOSPHERIC_QUEUE)
    val wallpaperPath: StateFlow<String?> = settings.wallpaperPath.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val reducedMotion: StateFlow<Boolean> = settings.reducedMotion.stateIn(viewModelScope, SharingStarted.Eagerly, false)
    val fadeEnabled: StateFlow<Boolean> = settings.fadeEnabled.stateIn(viewModelScope, SharingStarted.Eagerly, true)
    val colorSource: StateFlow<ColorSource> = settings.colorSource.stateIn(viewModelScope, SharingStarted.Eagerly, ColorSource.PULSE_PALETTE)
    val seedColor: StateFlow<Int> = settings.seedColor.stateIn(viewModelScope, SharingStarted.Eagerly, 0)
    val cornerRadiusDp: StateFlow<Int> = settings.cornerRadiusDp.stateIn(viewModelScope, SharingStarted.Eagerly, 16)
    val buttonPresetId: StateFlow<String> = settings.buttonPresetId.stateIn(viewModelScope, SharingStarted.Eagerly, "centered")
    val onlineFeaturesEnabled: StateFlow<Boolean> = settings.onlineFeaturesEnabled.stateIn(viewModelScope, SharingStarted.Eagerly, false)
    val offlineOnlyMode: StateFlow<Boolean> = settings.offlineOnlyMode.stateIn(viewModelScope, SharingStarted.Eagerly, false)
    val animationIntensity: StateFlow<Int> = settings.animationIntensity.stateIn(viewModelScope, SharingStarted.Eagerly, 2)
    val listDensity: StateFlow<Int> = settings.listDensity.stateIn(viewModelScope, SharingStarted.Eagerly, 1)
    val albumGridColumns: StateFlow<Int> = settings.albumGridColumns.stateIn(viewModelScope, SharingStarted.Eagerly, 2)
    val minFileSizeKb: StateFlow<Int> = settings.minFileSizeKb.stateIn(viewModelScope, SharingStarted.Eagerly, 0)
    val crossfadeSeconds: StateFlow<Int> = settings.crossfadeSeconds.stateIn(viewModelScope, SharingStarted.Eagerly, 0)
    val homeTabsList: StateFlow<List<String>> = settings.homeTabsList.stateIn(
        viewModelScope, SharingStarted.Eagerly, listOf("For you", "Songs", "Playlists", "Folders", "Albums", "Artists")
    )
    val fontStyle: StateFlow<FontStylePreset> = settings.fontStyle.stateIn(viewModelScope, SharingStarted.Eagerly, FontStylePreset.MODERN)
    val seekStyle: StateFlow<SeekStylePreset> = settings.seekStyle.stateIn(viewModelScope, SharingStarted.Eagerly, SeekStylePreset.WAVEFORM)
    val hiddenPlayerElements: StateFlow<Set<String>> = settings.hiddenPlayerElements.stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())
    val customButtonOrder: StateFlow<List<String>> = settings.customButtonOrder.stateIn(
        viewModelScope, SharingStarted.Eagerly, listOf("Shuffle", "Previous", "Play/Pause", "Next", "Repeat", "Favorite", "Queue", "More")
    )
    val forYouSectionOrder: StateFlow<List<String>> = settings.forYouSectionOrder.stateIn(
        viewModelScope, SharingStarted.Eagerly, listOf("resume", "actions", "widgets", "recent", "top_picks")
    )
    val hiddenForYouSections: StateFlow<Set<String>> = settings.hiddenForYouSections.stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())
    val sleepFadeSec: StateFlow<Int> = settings.sleepFadeSec.stateIn(viewModelScope, SharingStarted.Eagerly, 10)
    val splitPlayerUpperRatio: StateFlow<Float> = settings.splitPlayerUpperRatio.stateIn(viewModelScope, SharingStarted.Eagerly, 0.40f)
    val playerCoverShape: StateFlow<String> = settings.playerCoverShape.stateIn(viewModelScope, SharingStarted.Eagerly, "ROUNDED")
    val playerBackdropBlur: StateFlow<Float> = settings.playerBackdropBlur.stateIn(viewModelScope, SharingStarted.Eagerly, 0.60f)
    val playerBackdropDim: StateFlow<Float> = settings.playerBackdropDim.stateIn(viewModelScope, SharingStarted.Eagerly, 0.55f)
    val playerArtworkGlow: StateFlow<Boolean> = settings.playerArtworkGlow.stateIn(viewModelScope, SharingStarted.Eagerly, true)
    val playerSwipeSkip: StateFlow<Boolean> = settings.playerSwipeSkip.stateIn(viewModelScope, SharingStarted.Eagerly, true)
    val playerSeekbarHeight: StateFlow<String> = settings.playerSeekbarHeight.stateIn(viewModelScope, SharingStarted.Eagerly, "MEDIUM")
    val excludedFolders: StateFlow<Set<String>> = settings.excludedFolders.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())
    val artworkAccent = MutableStateFlow(PulseColors.StudioAmber)

    fun setThemeMode(m: ThemeMode) = viewModelScope.launch { settings.setTheme(m) }
    fun setSkin(id: String) = viewModelScope.launch { settings.setSkin(id) }
    fun applyPlayerTheme(id: String) = viewModelScope.launch {
        settings.setPlayerTheme(id)
        settings.setPlayerLayout(PlayerLayoutMode.fromId(id).id)
    }
    fun setPlayerLayout(mode: PlayerLayoutMode) = viewModelScope.launch {
        settings.setPlayerLayout(mode.id)
        settings.setPlayerTheme(mode.id)
    }
    fun nextPlayerLayout() = setPlayerLayout(PlayerLayoutMode.next(activePlayerLayout.value))
    fun previousPlayerLayout() = setPlayerLayout(PlayerLayoutMode.previous(activePlayerLayout.value))
    fun setWallpaper(path: String?) = viewModelScope.launch { settings.setWallpaper(path) }
    fun setReducedMotion(v: Boolean) = viewModelScope.launch { settings.setReduced(v) }
    fun setFadeEnabled(v: Boolean) = viewModelScope.launch { settings.setFade(v); PlaybackEngine.fadeEnabled = v }
    fun setSleepFadeSec(sec: Int) = viewModelScope.launch {
        val safe = sec.coerceIn(0, 30)
        settings.setSleepFadeSec(safe)
        player.setSleepFadeSec(safe)
    }
    fun setSplitPlayerUpperRatio(ratio: Float) = viewModelScope.launch { settings.setSplitPlayerUpperRatio(ratio.coerceIn(0.35f, 0.72f)) }
    fun setPlayerCoverShape(shape: String) = viewModelScope.launch { settings.setPlayerCoverShape(shape) }
    fun setPlayerBackdropBlur(blur: Float) = viewModelScope.launch { settings.setPlayerBackdropBlur(blur) }
    fun setPlayerBackdropDim(dim: Float) = viewModelScope.launch { settings.setPlayerBackdropDim(dim) }
    fun setPlayerArtworkGlow(enabled: Boolean) = viewModelScope.launch { settings.setPlayerArtworkGlow(enabled) }
    fun setPlayerSwipeSkip(enabled: Boolean) = viewModelScope.launch { settings.setPlayerSwipeSkip(enabled) }
    fun setPlayerSeekbarHeight(height: String) = viewModelScope.launch { settings.setPlayerSeekbarHeight(height) }
    fun setColorSource(s: ColorSource) = viewModelScope.launch { settings.setColorSource(s) }
    fun setSeedColor(argb: Int) = viewModelScope.launch { settings.setSeedColor(argb) }
    fun setCornerRadius(dp: Int) = viewModelScope.launch { settings.setCornerRadius(dp) }
    fun setButtonPreset(id: String) = viewModelScope.launch { settings.setButtonPreset(id) }
    fun setOnlineFeaturesEnabled(v: Boolean) = viewModelScope.launch { settings.setOnlineFeaturesEnabled(v) }
    fun setOfflineOnlyMode(v: Boolean) = viewModelScope.launch { settings.setOfflineOnlyMode(v) }
    fun setAnimationIntensity(v: Int) = viewModelScope.launch { settings.setAnimationIntensity(v) }
    fun setListDensity(v: Int) = viewModelScope.launch { settings.setListDensity(v) }
    fun setAlbumGridColumns(v: Int) = viewModelScope.launch { settings.setAlbumGridColumns(v) }
    fun setMinFileSizeKb(v: Int) = viewModelScope.launch { settings.setMinFileSizeKb(v) }
    fun setCrossfadeSeconds(sec: Int) = viewModelScope.launch {
        val s = sec.coerceIn(0, 12)
        settings.setCrossfadeSeconds(s)
        PlaybackEngine.setCrossfadeSeconds(s, fxCoordinator.crossfadeEqualPower.value)
    }
    fun setHomeTabs(tabs: List<String>) = viewModelScope.launch { settings.setHomeTabs(tabs) }
    fun setFontStyle(preset: FontStylePreset) = viewModelScope.launch { settings.setFontStyle(preset) }
    fun setSeekStyle(preset: SeekStylePreset) = viewModelScope.launch { settings.setSeekStyle(preset) }
    fun setPlayerElementVisible(key: String, visible: Boolean) = viewModelScope.launch { settings.setPlayerElementVisible(key, visible) }
    fun clearHiddenPlayerElements() = viewModelScope.launch { settings.clearHiddenPlayerElements() }
    fun setCustomButtonOrder(order: List<String>) = viewModelScope.launch { settings.setCustomButtonOrder(order) }
    fun setForYouSectionOrder(order: List<String>) = viewModelScope.launch { settings.setForYouSectionOrder(order) }
    fun setForYouSectionVisible(key: String, visible: Boolean) = viewModelScope.launch { settings.setForYouSectionVisible(key, visible) }
    fun resetForYouLayout() = viewModelScope.launch { settings.resetForYouLayout() }

    // Diagnostics & Fx Delegates
    val diagnosticsOpen = MutableStateFlow(false)
    val recentErrors: StateFlow<List<AppError>> = ErrorDispatcher.recentErrors
    val crashRecords: StateFlow<List<CrashRecord>> = CrashLogStore.crashes
    val pendingRecoveryCrash: StateFlow<CrashRecord?> = CrashLogStore.pendingRecoveryCrash
    val fxStatus: StateFlow<AudioFxStatus> = AudioFx.status
    val customSoundProfiles: StateFlow<List<SoundProfile>> = fxCoordinator.customSoundProfiles
    val soundProfiles: StateFlow<List<SoundProfile>> = fxCoordinator.soundProfiles
    val activeSoundProfileId: StateFlow<String> = fxCoordinator.activeSoundProfileId
    val currentAudioDevice: StateFlow<AudioOutputDevice> = fxCoordinator.currentAudioDevice
    val deviceProfileMap: StateFlow<Map<String, String>> = fxCoordinator.deviceProfileMap
    val deviceDspRules: StateFlow<Map<String, DeviceDspRule>> = fxCoordinator.deviceDspRules
    val replayGainMode: StateFlow<ReplayGainMode> = fxCoordinator.replayGainMode
    val replayGainPreampDb: StateFlow<Float> = fxCoordinator.replayGainPreampDb
    val crossfadeEqualPower: StateFlow<Boolean> = fxCoordinator.crossfadeEqualPower
    val autoResumeOnDeviceConnect: StateFlow<Boolean> = fxCoordinator.autoResumeOnDeviceConnect
    val smartAlbumGapless: StateFlow<Boolean> = fxCoordinator.smartAlbumGapless
    val eqChannelBalance: StateFlow<Float> = fxCoordinator.eqChannelBalance
    val eqMonoMode: StateFlow<Boolean> = fxCoordinator.eqMonoMode
    val eqLimiterEnabled: StateFlow<Boolean> = fxCoordinator.eqLimiterEnabled
    val drivingKeepScreenOn: StateFlow<Boolean> = fxCoordinator.drivingKeepScreenOn

    fun applySoundProfile(profile: SoundProfile) = fxCoordinator.applySoundProfile(profile)
    fun saveCurrentAsSoundProfile(name: String) = fxCoordinator.saveCurrentAsSoundProfile(name)
    fun deleteSoundProfile(id: String) = fxCoordinator.deleteSoundProfile(id)
    fun assignProfileToCurrentDevice(profileId: String?) = fxCoordinator.assignProfileToCurrentDevice(profileId)
    fun saveDeviceDspRule(rule: DeviceDspRule) = fxCoordinator.saveDeviceDspRule(rule)
    fun setReplayGainMode(mode: ReplayGainMode) = fxCoordinator.setReplayGainMode(mode)
    fun setReplayGainPreampDb(db: Float) = fxCoordinator.setReplayGainPreampDb(db)
    fun setCrossfadeEqualPower(enabled: Boolean) = fxCoordinator.setCrossfadeEqualPower(enabled)
    fun setSmartAlbumGapless(enabled: Boolean) = fxCoordinator.setSmartAlbumGapless(enabled)
    fun setEqChannelBalance(balance: Float) = fxCoordinator.setEqChannelBalance(balance)
    fun setEqMonoMode(enabled: Boolean) = fxCoordinator.setEqMonoMode(enabled)
    fun setEqLimiterEnabled(enabled: Boolean) = fxCoordinator.setEqLimiterEnabled(enabled)
    fun setDrivingKeepScreenOn(enabled: Boolean) = fxCoordinator.setDrivingKeepScreenOn(enabled)
    fun setAutoResumeOnDeviceConnect(enabled: Boolean) = fxCoordinator.setAutoResumeOnDeviceConnect(enabled)
    fun openDiagnostics() { diagnosticsOpen.value = true }
    fun closeDiagnostics() { diagnosticsOpen.value = false }
    fun clearErrorLog() { ErrorDispatcher.clearLog() }
    fun dismissCrashRecovery() = CrashLogStore.acknowledgeRecovery()
    fun openCrashLogs() { CrashLogStore.acknowledgeRecovery(); diagnosticsOpen.value = true }
    fun clearCrashLogs() = CrashLogStore.clearCrashes()
    fun formatCrashReport(crash: CrashRecord): String = CrashLogStore.formatCrashReport(crash)

    // Dialogs & Tag Editor Delegates
    val queueOpen = MutableStateFlow(false)
    val sleepOpen = MutableStateFlow(false)
    val audioControlsOpen = MutableStateFlow(false)
    val sortFilterSheetOpen = MutableStateFlow(false)
    val addToPlaylistOpen = MutableStateFlow<Track?>(null)
    val trackMenuTrack = MutableStateFlow<Track?>(null)
    val createPlaylistOpen = MutableStateFlow(false)
    val batchAddToPlaylistOpen = MutableStateFlow<List<String>?>(null)
    val songDetailsTrack: MutableStateFlow<Track?> = tagEditorCoordinator.songDetailsTrack
    val tagEditorTrack: MutableStateFlow<Track?> = tagEditorCoordinator.tagEditorTrack
    val batchTagTracksOpen: MutableStateFlow<List<Track>?> = tagEditorCoordinator.batchTagTracksOpen
    val pendingAudioWriteRequest: StateFlow<PendingAudioWriteRequest?> = tagEditorCoordinator.pendingAudioWriteRequest
    fun openQueue() { queueOpen.value = true }
    fun closeQueue() { queueOpen.value = false }
    fun openSleepSheet() { sleepOpen.value = true }
    fun openSleep() { openSleepSheet() }
    fun closeSleepSheet() { sleepOpen.value = false }
    fun openAudioControls() { audioControlsOpen.value = true }
    fun closeAudioControls() { audioControlsOpen.value = false }
    fun openSortFilterSheet() { sortFilterSheetOpen.value = true }
    fun closeSortFilterSheet() { sortFilterSheetOpen.value = false }
    fun openAddToPlaylist(t: Track? = null) { addToPlaylistOpen.value = t ?: currentPlayingTrack() }
    fun closeAddToPlaylist() { addToPlaylistOpen.value = null }
    fun openBatchAddToPlaylist() { selected.value.toList().takeIf { it.isNotEmpty() }?.let { batchAddToPlaylistOpen.value = it } }
    fun closeBatchAddToPlaylist() { batchAddToPlaylistOpen.value = null }
    fun openTrackMenu(t: Track) { trackMenuTrack.value = t }
    fun closeTrackMenu() { trackMenuTrack.value = null }
    fun openTrackOptions() { currentPlayingTrack()?.let { openTrackMenu(it) } }
    fun openTagEditor(t: Track? = null) = tagEditorCoordinator.openTagEditor(t ?: currentPlayingTrack())
    fun closeTagEditor() = tagEditorCoordinator.closeTagEditor()
    fun openBatchTagEditor() = tagEditorCoordinator.openBatchTagEditor()
    fun closeBatchTagEditor() = tagEditorCoordinator.closeBatchTagEditor()
    fun clearPendingAudioWriteRequest() = tagEditorCoordinator.clearPendingAudioWriteRequest()
    fun retryPendingAudioWrite(req: PendingAudioWriteRequest) = tagEditorCoordinator.retryPendingAudioWrite(req)
    fun updateTrackTags(
        id: String,
        title: String,
        artist: String,
        album: String,
        genre: String,
        year: Int,
        trackNumber: Int,
        writeToFile: Boolean = false,
        onDone: (() -> Unit)? = null
    ) = tagEditorCoordinator.updateTrackTags(id, title, artist, album, genre, year, trackNumber, writeToFile, onDone)
    fun batchUpdateTags(ids: List<String>, artist: String?, album: String?, genre: String?, year: Int?, onDone: (() -> Unit)? = null) =
        tagEditorCoordinator.batchUpdateTags(ids, artist, album, genre, year, onDone)

    // Library Browser & Artist/Folder Tree Delegates
    val sortField: StateFlow<SortField> = browserCoordinator.sortField
    val sortAscending: StateFlow<Boolean> = browserCoordinator.sortAscending
    val entitySortMode: StateFlow<EntitySortMode> = browserCoordinator.entitySortMode
    val filterState: StateFlow<LibraryFilterState> = browserCoordinator.filterState
    val splitMultiArtists: StateFlow<Boolean> = browserCoordinator.splitMultiArtists
    val artistSplitExceptions: StateFlow<Set<String>> = browserCoordinator.artistSplitExceptions
    val folderViewTreeMode: StateFlow<Boolean> = browserCoordinator.folderViewTreeMode
    val currentFolderPath: StateFlow<String?> = browserCoordinator.currentFolderPath
    val folderTreeState: StateFlow<FolderTreeState> = browserCoordinator.folderTreeState
    val filteredTracks: StateFlow<List<Track>> = browserCoordinator.filteredTracks
    val sortedTracks: StateFlow<List<Track>> = browserCoordinator.sortedTracks
    val songsAlphabetIndex: StateFlow<Map<Char, Int>> = browserCoordinator.songsAlphabetIndex
    val missingAlphabetIndex: StateFlow<Map<Char, Int>> = browserCoordinator.missingAlphabetIndex
    val albums: StateFlow<List<AlbumItem>> = browserCoordinator.albums
    val artists: StateFlow<List<ArtistItem>> = browserCoordinator.artists
    val folders: StateFlow<List<FolderItem>> = browserCoordinator.folders
    val genres: StateFlow<List<GenreStat>> = browserCoordinator.genres
    val decades: StateFlow<List<DecadeStat>> = browserCoordinator.decades
    val recentTracks: StateFlow<List<Track>> = browserCoordinator.recentTracks
    val topPlayedTracks: StateFlow<List<Track>> = browserCoordinator.topPlayedTracks
    val recentlyPlayedTracks: StateFlow<List<Track>> = browserCoordinator.recentlyPlayedTracks
    val customSmartPlaylists: StateFlow<List<SmartPlaylistEntity>> = browserCoordinator.customSmartPlaylists

    fun setSortField(field: SortField) = browserCoordinator.setSortField(field)
    fun toggleSortOrder() = browserCoordinator.toggleSortOrder()
    fun setEntitySortMode(mode: EntitySortMode) = browserCoordinator.setEntitySortMode(mode)
    fun toggleFilterFavorites() = browserCoordinator.toggleFilterFavorites()
    fun toggleFilterHiRes() = browserCoordinator.toggleFilterHiRes()
    fun toggleFilterLong() = browserCoordinator.toggleFilterLong()
    fun toggleFilterUnplayed() = browserCoordinator.toggleFilterUnplayed()
    fun resetFilters() = browserCoordinator.resetFilters()
    fun isHiResTrack(t: Track): Boolean = browserCoordinator.isHiResTrack(t)
    fun sortTrackList(list: List<Track>, field: SortField, asc: Boolean): List<Track> = browserCoordinator.sortTrackList(list, field, asc)
    fun setSplitMultiArtists(enabled: Boolean) = browserCoordinator.setSplitMultiArtists(enabled)
    fun addArtistSplitException(name: String) = browserCoordinator.addArtistSplitException(name)
    fun removeArtistSplitException(name: String) = browserCoordinator.removeArtistSplitException(name)
    fun setFolderViewTreeMode(enabled: Boolean) = browserCoordinator.setFolderViewTreeMode(enabled)
    fun navigateFolderTree(path: String?) = browserCoordinator.navigateFolderTree(path)
    fun navigateFolderTreeUp() = browserCoordinator.navigateFolderTreeUp()
    fun observeSmartPlaylistTracks(smartId: Long): Flow<List<Track>> = browserCoordinator.observeSmartPlaylistTracks(smartId)
    suspend fun previewSmartRuleMatchCount(rules: SmartPlaylistEngine.SmartRules, limitCount: Int? = null): Int =
        browserCoordinator.previewSmartRuleMatchCount(rules, limitCount)
    fun saveSmartPlaylist(id: Long?, name: String, rules: SmartPlaylistEngine.SmartRules, sortKey: String?, sortOrder: Int?, limitCount: Int?, onDone: (() -> Unit)? = null) =
        browserCoordinator.saveSmartPlaylist(id, name, rules, sortKey, sortOrder, limitCount, onDone)
    fun deleteSmartPlaylist(id: Long) = browserCoordinator.deleteSmartPlaylist(id)
    fun playSimilarMix(seed: Track) = browserCoordinator.playSimilarMix(seed)
    fun removeDeletedDuplicates(ids: List<String>, onDone: (() -> Unit)? = null) = browserCoordinator.removeDeletedDuplicates(ids, onDone)

    // Favorites & Playlists
    val favoriteCount = db.favorites().observeIds().map { it.size }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
    val isCurrentFavorite: StateFlow<Boolean> = combine(player.state, db.favorites().observeIds()) { ps, favs ->
        ps.mediaId != null && ps.mediaId in favs
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val hiddenFavorites: StateFlow<List<TrackWithFav>> = combine(db.favorites().observeTracks(), excludedFolders) { favTracks, excluded ->
        favTracks.filter { item ->
            !item.track.available || excluded.any { ex -> item.track.folderPath.startsWith(ex, ignoreCase = true) }
        }
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun toggleFavorite(id: String) = viewModelScope.launch(Dispatchers.IO) { db.favorites().toggle(id, System.currentTimeMillis()) }
    fun toggleFavoriteCurrent() { player.state.value.mediaId?.let { toggleFavorite(it) } }
    fun isFavorite(id: String?): Flow<Boolean> = if (id == null) flowOf(false) else db.favorites().observeIds().map { id in it }

    val playlists: StateFlow<List<PlaylistEntity>> = db.playlists().observePlaylists().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    fun playlistName(id: Long): Flow<String> = db.playlists().observeById(id).map { it?.name ?: "Playlist" }
    fun playlistTracks(id: Long): Flow<List<Track>> = db.playlists().observeTracks(id).map { list -> list.map { it.toTrack() } }.flowOn(Dispatchers.Default)
    fun createPlaylist(name: String) = viewModelScope.launch(Dispatchers.IO) {
        if (name.isNotBlank()) db.playlists().create(PlaylistEntity(name = name.trim(), createdAtMs = System.currentTimeMillis()))
    }
    fun renamePlaylist(id: Long, name: String) = viewModelScope.launch(Dispatchers.IO) {
        val clean = name.trim()
        if (clean.isNotBlank()) {
            db.playlists().rename(id, clean)
            withContext(Dispatchers.Main) { snackbarMessage.value = "Renamed playlist to '$clean'" }
        }
    }
    fun deletePlaylist(id: Long) = viewModelScope.launch(Dispatchers.IO) { db.playlists().delete(id) }
    fun addTrackToPlaylist(playlistId: Long, trackId: String) = viewModelScope.launch(Dispatchers.IO) {
        val next = db.playlists().nextPos(playlistId)
        db.playlists().addTrack(com.pulse.player.data.db.PlaylistTrackEntity(playlistId, trackId, next, System.currentTimeMillis()))
    }
    fun removeTrackFromPlaylist(playlistId: Long, trackId: String) = viewModelScope.launch(Dispatchers.IO) { db.playlists().removeTrack(playlistId, trackId) }
    fun reorderPlaylistTracks(playlistId: Long, orderedIds: List<String>) = viewModelScope.launch(Dispatchers.IO) { db.playlists().reorderTracks(playlistId, orderedIds) }
    fun addSelectedToPlaylist(playlistId: Long, ids: List<String> = selected.value.toList()) = viewModelScope.launch(Dispatchers.IO) {
        if (ids.isEmpty()) return@launch
        ids.forEach { trackId ->
            val next = db.playlists().nextPos(playlistId)
            db.playlists().addTrack(com.pulse.player.data.db.PlaylistTrackEntity(playlistId, trackId, next, System.currentTimeMillis()))
        }
        withContext(Dispatchers.Main) { batchAddToPlaylistOpen.value = null; clearSelect(); snackbarMessage.value = "Added ${ids.size} track(s) to playlist" }
    }
    fun saveQueueAsPlaylist(name: String) = viewModelScope.launch(Dispatchers.IO) {
        val clean = name.trim()
        val currentQueue = player.queue.value
        if (clean.isBlank() || currentQueue.isEmpty()) return@launch
        val now = System.currentTimeMillis()
        val playlistId = db.playlists().create(PlaylistEntity(name = clean, createdAtMs = now))
        currentQueue.forEachIndexed { idx, track ->
            db.playlists().addTrack(com.pulse.player.data.db.PlaylistTrackEntity(playlistId, track.id, idx, now))
        }
        withContext(Dispatchers.Main) { snackbarMessage.value = "Saved ${currentQueue.size} tracks as '$clean'" }
    }
    fun favoriteSelected() = viewModelScope.launch(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val favs = selected.value.map { com.pulse.player.data.db.FavoriteEntity(it, now) }
        db.favorites().addAll(favs)
        withContext(Dispatchers.Main) { snackbarMessage.value = "Added ${favs.size} track(s) to favourites"; clearSelect() }
    }

    // Lyrics, Bookmarks & Queue Export
    fun lyrics(trackId: String?) = lyricsCoordinator.lyrics(trackId)
    fun observeResolvedLyrics(trackId: String?, dataPath: String?): Flow<ResolvedLyrics> = lyricsCoordinator.observeResolvedLyrics(trackId, dataPath)
    fun importLyrics(trackId: String, uri: Uri) = lyricsCoordinator.importLyrics(trackId, uri)
    fun saveSyncedLyrics(trackId: String, lines: List<LrcLine>) = lyricsCoordinator.saveSyncedLyrics(trackId, lines)
    fun deleteLyrics(trackId: String) = lyricsCoordinator.deleteLyrics(trackId)
    fun saveLyricsForTrack(trackId: String, lyricsText: String): Boolean = lyricsCoordinator.saveLyricsForTrack(trackId, lyricsText)
    fun bookmarksFor(trackId: String?): Flow<List<BookmarkEntity>> = lyricsCoordinator.bookmarksFor(trackId)
    fun addBookmark(trackId: String, positionMs: Long, label: String = "") = lyricsCoordinator.addBookmark(trackId, positionMs, label)
    fun deleteBookmark(id: Long) = lyricsCoordinator.deleteBookmark(id)
    val savedQueues: StateFlow<List<SavedQueueEntity>> = importExportCoordinator.savedQueues
    fun saveCurrentQueueSession(name: String) = importExportCoordinator.saveCurrentQueueSession(name)
    fun restoreSavedQueue(id: Long) = importExportCoordinator.restoreSavedQueue(id)
    fun deleteSavedQueue(id: Long) = importExportCoordinator.deleteSavedQueue(id)
    fun exportPlaylistM3u8(playlistId: Long, playlistName: String, destUri: Uri) = importExportCoordinator.exportPlaylistM3u8(playlistId, playlistName, destUri)
    fun importM3uPlaylist(uri: Uri, fallbackName: String = "Imported Playlist") = importExportCoordinator.importM3uPlaylist(uri, tracks.value, fallbackName)

    fun exportLrcToUri(destUri: Uri, lines: List<LrcLine>) = viewModelScope.launch(Dispatchers.IO) {
        if (lines.isEmpty()) return@launch
        runCatching {
            val content = com.pulse.player.data.lyrics.Lrc.serialize(lines)
            app.contentResolver.openOutputStream(destUri)?.use { out ->
                out.write(content.toByteArray(Charsets.UTF_8))
                out.flush()
            } ?: throw IllegalStateException("Cannot open destination document")
        }.onSuccess {
            withContext(Dispatchers.Main) { snackbarMessage.value = "Exported .lrc lyrics file" }
        }.onFailure { err ->
            withContext(Dispatchers.Main) { snackbarMessage.value = "Failed to export .lrc: ${err.message}" }
        }
    }

    // Queue & Playback Commands
    val queueItems: StateFlow<List<QueueItem>> = queueCoordinator.queueItems
    fun playAt(i: Int) = queueCoordinator.playAt(i)
    fun removeAt(i: Int) = queueCoordinator.removeAt(i)
    fun toggleShuffle() = queueCoordinator.toggleShuffle()
    fun cycleRepeat() = queueCoordinator.cycleRepeat()
    fun retry() = queueCoordinator.retry()
    fun skip() = queueCoordinator.skip()
    fun playQueue(tracks: List<Track>, startIndex: Int, shuffle: Boolean = false) = queueCoordinator.playQueue(tracks, startIndex, shuffle)
    fun playNext(t: Track) = queueCoordinator.playNext(t)
    fun addToQueue(t: Track) = queueCoordinator.addToQueue(t)
    fun playExternal(uri: Uri) = queueCoordinator.playExternal(uri)
    fun playTrackById(id: String) = queueCoordinator.playTrackById(id)
    fun removeFromQueueWithUndo(index: Int) = queueCoordinator.removeFromQueueWithUndo(index)
    fun shuffleUpcomingQueue() = queueCoordinator.shuffleUpcomingQueue()
    fun playAlbum(albumName: String, shuffle: Boolean = false) = queueCoordinator.playAlbum(albumName, shuffle)
    fun queueAlbumNext(albumName: String) = queueCoordinator.queueAlbumNext(albumName)
    fun playArtist(artistName: String, shuffle: Boolean = false) = queueCoordinator.playArtist(artistName, shuffle)
    fun queueArtistNext(artistName: String) = queueCoordinator.queueArtistNext(artistName)
    fun playFolder(folderPath: String, shuffle: Boolean = false, recursive: Boolean = folderViewTreeMode.value) =
        queueCoordinator.playFolder(folderPath, shuffle, recursive)
    fun queueFolderNext(folderPath: String, recursive: Boolean = folderViewTreeMode.value) =
        queueCoordinator.queueFolderNext(folderPath, recursive)
    fun playSelectedNext() = queueCoordinator.playSelectedNext()
    fun currentPlayingTrack(): Track? = queueCoordinator.currentPlayingTrack()
    fun shuffleAll() = queueCoordinator.shuffleAll()
    fun shuffleFavorites() = queueCoordinator.shuffleFavorites()
    fun shuffleUnplayed() = queueCoordinator.shuffleUnplayed()
    fun shuffleRecentlyAdded() = queueCoordinator.shuffleRecentlyAdded()

    // Scan & Exclusions
    fun scan() = viewModelScope.launch {
        scanWarningDismissed.value = false
        val minDur = settings.minDurationSec.first() * 1000L
        val excluded = settings.excludedFolders.first()
        scanner.scanMediaStore(minDur, excluded)
        val trees = settings.safTrees.first()
        trees.forEach { uriStr ->
            runCatching { scanner.scanSafTree(Uri.parse(uriStr), minDur) }.onFailure { err ->
                ErrorDispatcher.post(ErrorCategory.SCAN, "Folder Scan Failed", err.message ?: "Unable to scan linked music folder.", err.stackTraceToString())
            }
        }
        refreshStats()
    }
    fun addSafFolder(uri: Uri) = viewModelScope.launch { settings.addSafTree(uri.toString()); scan() }
    fun scanTree(uri: Uri) = addSafFolder(uri)
    fun excludeFolder(path: String) = viewModelScope.launch {
        if (path.isBlank()) return@launch
        settings.addExcludedFolders(setOf(path))
        snackbarMessage.value = "Excluded ${path.substringAfterLast('/')}"
        scan()
    }
    fun unexcludeFolder(path: String) = viewModelScope.launch {
        settings.removeExcludedFolder(path)
        snackbarMessage.value = "Restored ${path.substringAfterLast('/')}"
        scan()
    }
    fun clearExcludedFolders() = viewModelScope.launch {
        settings.clearExcludedFolders()
        snackbarMessage.value = "Cleared excluded folders"
        scan()
    }
    fun seedSampleTrack() = viewModelScope.launch { SampleMusicHelper.installSampleTrack(getApplication(), db) }

    // Backup, Stats & Loudness Scan Delegates
    val todayMinutes: StateFlow<Int> = backupCoordinator.todayMinutes
    val streakDays: StateFlow<Int> = backupCoordinator.streakDays
    val topPicks: StateFlow<List<Track>> = backupCoordinator.topPicks
    val resume: StateFlow<Track?> = backupCoordinator.resume
    val loudnessScanState: StateFlow<LoudnessScanState> = backupCoordinator.loudnessScanState
    val missingReplayGainCount: StateFlow<Int> = tracks
        .map { list -> list.count { it.replayGainTrackDb == 0f } }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
    fun startLoudnessScan() = backupCoordinator.startLoudnessScan()
    fun cancelLoudnessScan() = backupCoordinator.cancelLoudnessScan()
    fun resetLoudnessScanState() = backupCoordinator.resetLoudnessScanState()
    fun refreshStats() = backupCoordinator.refreshStats()
    fun cleanMissingTracks(onDone: ((Int) -> Unit)? = null) = backupCoordinator.cleanMissingTracks(onDone)
    val lastBackupJson: StateFlow<String?> = backupCoordinator.lastBackupJson
    val exportStatus: MutableStateFlow<String?> = backupCoordinator.exportStatus
    val importPreview: StateFlow<FavoritesRepository.Preview?> = backupCoordinator.importPreview
    fun dismissImportPreview() { backupCoordinator.importPreview.value = null }
    val compatPreview: StateFlow<CompatRestorePreview?> = backupCoordinator.compatPreview
    val compatReport: StateFlow<CompatRestoreReport?> = backupCoordinator.compatReport
    val compatSelectedCategories: StateFlow<Set<String>> = backupCoordinator.compatSelectedCategories
    val compatReplaceMode: StateFlow<Boolean> = backupCoordinator.compatReplaceMode
    val compatExportCats: StateFlow<ExportCats> = backupCoordinator.compatExportCats
    val compatExportAsZip: StateFlow<Boolean> = backupCoordinator.compatExportAsZip
    val archiveOperation: StateFlow<BackupOperationState> = backupCoordinator.archiveOperation
    val archivePreview: StateFlow<RestorePreview?> = backupCoordinator.archivePreview
    val archiveReport: StateFlow<RestoreReport?> = backupCoordinator.archiveReport
    val lastBackupRecord: StateFlow<LastBackupRecord?> = backupCoordinator.lastBackupRecord
    val selectedBackupCategories: StateFlow<Set<BackupCategory>> = backupCoordinator.selectedBackupCategories
    val selectedRestoreConflictMode: StateFlow<RestoreConflictMode> = backupCoordinator.selectedRestoreConflictMode
    fun refreshLastBackup() = backupCoordinator.refreshLastBackup()
    fun exportFavorites(uri: Uri) = backupCoordinator.exportFavorites(uri)
    fun exportFavoritesToPulseFolder(customTreeUri: Uri? = null) = backupCoordinator.exportFavoritesToPulseFolder(customTreeUri)
    fun previewFavoritesImport(uri: Uri, name: String) = backupCoordinator.previewFavoritesImport(uri, name)
    fun previewFavoritesFolder(uri: Uri) = backupCoordinator.previewFavoritesFolder(uri)
    fun importFavoritesFromDownloadFolder() = backupCoordinator.importFavoritesFromDownloadFolder()
    fun applyImport(preview: FavoritesRepository.Preview, resolved: Map<FavItem, TrackEntity>, replace: Boolean) =
        backupCoordinator.applyImport(preview, resolved, replace)
    fun toggleCompatCategory(flag: String) = backupCoordinator.toggleCompatCategory(flag)
    fun setCompatReplaceMode(replace: Boolean) = backupCoordinator.setCompatReplaceMode(replace)
    fun toggleExportCategory(key: String) = backupCoordinator.toggleExportCategory(key)
    fun setCompatExportAsZip(asZip: Boolean) = backupCoordinator.setCompatExportAsZip(asZip)
    fun executeCompatExport(destUri: Uri) = backupCoordinator.executeCompatExport(destUri)
    fun smartInspect(uri: Uri, fileName: String) = backupCoordinator.smartInspect(uri, fileName)
    fun inspectSampleReferenceBackup() = backupCoordinator.inspectSampleReferenceBackup()
    fun executeCompatRestore(staged: StagedBackup, categories: Set<String>, replace: Boolean) =
        backupCoordinator.executeCompatRestore(staged, categories, replace)
    fun dismissCompatPreview() = backupCoordinator.dismissCompatPreview()
    fun dismissCompatReport() = backupCoordinator.dismissCompatReport()
    fun saveUnmatchedReport(destUri: Uri, lines: List<String>) = backupCoordinator.saveUnmatchedReport(destUri, lines)
    fun toggleBackupCategory(cat: BackupCategory) = backupCoordinator.toggleBackupCategory(cat)
    fun setConflictMode(mode: RestoreConflictMode) = backupCoordinator.setConflictMode(mode)
    fun createArchiveBackup(uri: Uri) = backupCoordinator.createArchiveBackup(uri)
    fun inspectArchive(uri: Uri, fileName: String) = backupCoordinator.inspectArchive(uri, fileName)
    fun executeArchiveRestore(uri: Uri, fileName: String, categories: Set<BackupCategory>, mode: RestoreConflictMode) =
        backupCoordinator.executeArchiveRestore(uri, fileName, categories, mode)
    fun dismissArchivePreview() = backupCoordinator.dismissArchivePreview()
    fun dismissArchiveReport() = backupCoordinator.dismissArchiveReport()

    // Search
    val searchQuery: MutableStateFlow<String> = searchCoordinator.searchQuery
    val searchScope: MutableStateFlow<SearchScope> = searchCoordinator.searchScope
    val recentSearches: StateFlow<List<String>> = searchCoordinator.recentSearches
    fun setSearchScope(scope: SearchScope) = searchCoordinator.setSearchScope(scope)
    fun commitRecentSearch(query: String = searchQuery.value) = searchCoordinator.commitRecentSearch(query)
    fun removeRecentSearch(query: String) = searchCoordinator.removeRecentSearch(query)
    fun clearRecentSearches() = searchCoordinator.clearRecentSearches()
    val searchResults: StateFlow<List<Track>> = searchCoordinator.searchResults
    val searchArtistResults: StateFlow<List<ArtistItem>> = searchCoordinator.searchArtistResults
    val searchAlbumResults: StateFlow<List<AlbumItem>> = searchCoordinator.searchAlbumResults
    val searchFolderResults: StateFlow<List<FolderItem>> = searchCoordinator.searchFolderResults
    val searchScopeCounts: StateFlow<Map<SearchScope, Int>> = searchCoordinator.searchScopeCounts

    // AI Lab
    val transcriptionState = MutableStateFlow<TranscriptionState>(TranscriptionState.Idle)
    val musicGenState = MutableStateFlow<MusicGenState>(MusicGenState.Idle)
    fun resetTranscriptionState() { transcriptionState.value = TranscriptionState.Idle }
    fun resetMusicGenState() { musicGenState.value = MusicGenState.Idle }
    fun setGeminiApiKey(key: String) = viewModelScope.launch { settings.setGeminiApiKey(key) }

    fun transcribeTrack(track: Track, customPrompt: String? = null) = viewModelScope.launch {
        transcriptionState.value = TranscriptionState.Transcribing("Transcribing '${track.title}'...")
        val apiKey = settings.effectiveGeminiApiKey()
        val ctx = getApplication<Application>()
        val tempFile = File(ctx.cacheDir, "track_transcribe_${System.currentTimeMillis()}.mp3")
        val copied = runCatching {
            ctx.contentResolver.openInputStream(Uri.parse(track.uri))?.use { input ->
                tempFile.outputStream().use { output -> input.copyTo(output) }
            }
            tempFile.exists() && tempFile.length() > 0
        }.getOrDefault(false)
        if (!copied) {
            val msg = "Unable to read audio file for ${track.title}."
            transcriptionState.value = TranscriptionState.Error(msg)
            ErrorDispatcher.post(ErrorCategory.STORAGE, "Audio Read Failed", msg)
            return@launch
        }
        val res = networkGateway.transcribeAudio(tempFile, apiKey, customPrompt)
        if (res.isSuccess) {
            transcriptionState.value = TranscriptionState.Success(res.getOrThrow(), tempFile)
        } else {
            val ex = res.exceptionOrNull()
            val msg = ex?.message ?: "Transcription failed"
            transcriptionState.value = TranscriptionState.Error(msg)
            ErrorDispatcher.post(ErrorCategory.NETWORK, "Transcription Failed", msg, ex?.stackTraceToString())
        }
    }

    fun generateMusic(prompt: String, model: String = "lyria-3-clip-preview") = viewModelScope.launch {
        musicGenState.value = MusicGenState.Generating(prompt, model)
        val apiKey = settings.effectiveGeminiApiKey()
        val ctx = getApplication<Application>()
        val res = networkGateway.generateMusic(ctx, prompt, model, apiKey)
        if (res.isSuccess) {
            val genResult = res.getOrThrow()
            val mmr = android.media.MediaMetadataRetriever()
            val durationMs = runCatching {
                mmr.setDataSource(genResult.file.absolutePath)
                mmr.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 30_000L
            }.getOrDefault(30_000L)
            runCatching { mmr.release() }
            val now = System.currentTimeMillis()
            val entity = TrackEntity(
                id = "gen:${genResult.file.name}",
                uri = Uri.fromFile(genResult.file).toString(),
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
            musicGenState.value = MusicGenState.Success(genResult, TrackWithFav(entity, false).toTrack())
        } else {
            val ex = res.exceptionOrNull()
            val err = ex?.message ?: "Music generation failed"
            musicGenState.value = MusicGenState.Error(err)
            ErrorDispatcher.post(ErrorCategory.NETWORK, "Music Generation Failed", err, ex?.stackTraceToString())
        }
    }

    init {
        player.onSnapshotChanged = { snap ->
            viewModelScope.launch(Dispatchers.IO) {
                val activeIds = if (snap.shuffleEnabled) snap.shuffled else snap.original
                val idx = activeIds.indexOf(snap.currentId).coerceAtLeast(0)
                settings.saveQueue(activeIds, idx, snap.positionMs)
            }
        }
        viewModelScope.launch { settings.sleepFadeSec.collect { sec -> player.setSleepFadeSec(sec) } }
        viewModelScope.launch {
            player.state.map { it.artworkUri }.distinctUntilChanged().collectLatest { artUri ->
                if (artUri.isNullOrBlank()) {
                    artworkAccent.value = PulseColors.StudioAmber
                    return@collectLatest
                }
                val extracted = withContext(Dispatchers.Default) {
                    val bmp = runCatching { ArtworkLoader.load(app, artUri, 96) }.getOrNull() ?: return@withContext null
                    val palette = runCatching { Palette.from(bmp).generate() }.getOrNull() ?: return@withContext null
                    val argb = palette.getVibrantColor(palette.getLightVibrantColor(palette.getDominantColor(0)))
                    if (argb != 0) Color(argb) else null
                }
                artworkAccent.value = extracted ?: PulseColors.StudioAmber
            }
        }
        refreshStats()
        viewModelScope.launch {
            val (ids, index, pos) = settings.queue.first()
            if (ids.isNotEmpty()) {
                val entities = db.tracks().byIds(ids).associateBy { it.id }
                val resolvedTracks = ids.mapNotNull { entities[it]?.let { e -> TrackWithFav(e, false).toTrack() } }
                if (resolvedTracks.isNotEmpty()) player.restore(ids, index, pos, resolvedTracks)
            }
        }
        refreshLastBackup()
    }
}
