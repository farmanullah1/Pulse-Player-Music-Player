package com.pulse.player.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pulse.player.app
import com.pulse.player.data.db.PlaylistEntity
import com.pulse.player.data.db.TrackEntity
import com.pulse.player.data.favorites.FavItem
import com.pulse.player.data.favorites.FavoritesRepository
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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.ArrayDeque

enum class SortField { TITLE, ARTIST, DATE_ADDED, DURATION }

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application.app
    val player = app.player
    private val db = app.db
    val settings = app.settings
    private val scanner = app.scanner
    val favoritesRepo = app.favoritesRepo

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
    val topPicks = MutableStateFlow<List<Track>>(emptyList())
    val resume = MutableStateFlow<Track?>(null)

    init {
        refreshStats()
        restoreLastQueue()
    }

    fun refreshStats() = viewModelScope.launch(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val startOfDay = now - (now % 86_400_000L)
        val todayMs = db.stats().totalMs(startOfDay, now)
        todayMinutes.value = (todayMs / 60_000L).toInt()

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
}
