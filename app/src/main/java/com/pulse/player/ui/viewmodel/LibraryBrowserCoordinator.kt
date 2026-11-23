package com.pulse.player.ui.viewmodel

import com.pulse.player.data.db.GenreStat
import com.pulse.player.data.db.PulseDb
import com.pulse.player.data.db.SmartPlaylistEntity
import com.pulse.player.data.scan.ArtworkLoader
import com.pulse.player.data.scan.norm
import com.pulse.player.data.settings.SettingsStore
import com.pulse.player.library.ArtistParser
import com.pulse.player.library.FolderTreeBuilder
import com.pulse.player.library.FolderTreeState
import com.pulse.player.library.SmartPlaylistEngine
import com.pulse.player.playback.PlayerController
import com.pulse.player.playback.Track
import com.pulse.player.ui.components.firstLetter
import com.pulse.player.ui.screens.AlbumItem
import com.pulse.player.ui.screens.ArtistItem
import com.pulse.player.ui.screens.FolderItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs

data class DecadeStat(
    val decade: Int,
    val label: String,
    val trackCount: Int,
    val totalMs: Long,
    val artUri: String?
)

class LibraryBrowserCoordinator(
    private val db: PulseDb,
    private val settings: SettingsStore,
    private val player: PlayerController,
    val tracks: StateFlow<List<Track>>,
    neverPlayedIds: StateFlow<Set<String>>,
    missingTracks: StateFlow<List<Track>>,
    private val scope: CoroutineScope,
    private val onSnackbar: (String) -> Unit
) {
    private val smartEngine = SmartPlaylistEngine(db)

    val sortField = MutableStateFlow(SortField.TITLE)
    val sortAscending = MutableStateFlow(true)
    val entitySortMode = MutableStateFlow(EntitySortMode.NAME_ASC)
    val filterState = MutableStateFlow(LibraryFilterState())

    val splitMultiArtists: StateFlow<Boolean> = settings.splitMultiArtists
        .stateIn(scope, SharingStarted.Eagerly, true)

    val artistSplitExceptions: StateFlow<Set<String>> = settings.artistSplitExceptions
        .stateIn(scope, SharingStarted.Eagerly, emptySet())

    val folderViewTreeMode: StateFlow<Boolean> = settings.folderViewTreeMode
        .stateIn(scope, SharingStarted.Eagerly, false)

    private val _currentFolderPath = MutableStateFlow<String?>(null)
    val currentFolderPath: StateFlow<String?> = _currentFolderPath.asStateFlow()

    fun setSplitMultiArtists(enabled: Boolean) = scope.launch {
        settings.setSplitMultiArtists(enabled)
    }

    fun addArtistSplitException(name: String) = scope.launch {
        settings.addArtistSplitException(name)
        onSnackbar("Added artist split exception '${name.trim()}'")
    }

    fun removeArtistSplitException(name: String) = scope.launch {
        settings.removeArtistSplitException(name)
    }

    fun setFolderViewTreeMode(enabled: Boolean) = scope.launch {
        settings.setFolderViewTreeMode(enabled)
    }

    fun navigateFolderTree(path: String?) {
        _currentFolderPath.value = path
    }

    fun navigateFolderTreeUp() {
        val state = folderTreeState.value
        val cur = state.currentPath
        if (cur.isBlank() || cur.equals(state.rootPath, ignoreCase = true)) {
            _currentFolderPath.value = null
            return
        }
        val parent = cur.substringBeforeLast('/', "")
        _currentFolderPath.value = if (parent.length < state.rootPath.length) state.rootPath else parent
    }

    fun setSortField(field: SortField) {
        if (sortField.value == field) {
            sortAscending.value = !sortAscending.value
        } else {
            sortField.value = field
            sortAscending.value = field != SortField.DATE_ADDED && field != SortField.DURATION
        }
    }

    fun toggleSortOrder() {
        sortAscending.value = !sortAscending.value
    }

    fun setEntitySortMode(mode: EntitySortMode) {
        entitySortMode.value = mode
    }

    fun toggleFilterFavorites() = filterState.update { it.copy(favoritesOnly = !it.favoritesOnly) }
    fun toggleFilterHiRes() = filterState.update { it.copy(hiResOnly = !it.hiResOnly) }
    fun toggleFilterLong() = filterState.update { it.copy(longOnly = !it.longOnly) }
    fun toggleFilterUnplayed() = filterState.update { it.copy(unplayedOnly = !it.unplayedOnly) }
    fun resetFilters() { filterState.value = LibraryFilterState() }

    fun isHiResTrack(t: Track): Boolean {
        val mime = t.mimeType.orEmpty()
        val codec = t.codec
        val uri = t.uri
        return codec.equals("FLAC", true) || codec.equals("WAV", true) || codec.equals("ALAC", true) ||
            mime.contains("flac", true) || mime.contains("wav", true) || mime.contains("alac", true) ||
            uri.endsWith(".flac", true) || uri.endsWith(".wav", true) || uri.endsWith(".alac", true)
    }

    val filteredTracks: StateFlow<List<Track>> = combine(tracks, filterState, neverPlayedIds) { list, filter, unplayed ->
        if (filter.activeCount == 0) {
            list
        } else {
            list.filter { t ->
                (!filter.favoritesOnly || t.isFavorite) &&
                    (!filter.hiResOnly || isHiResTrack(t)) &&
                    (!filter.longOnly || t.durationMs >= 420_000L) &&
                    (!filter.unplayedOnly || t.id in unplayed)
            }
        }
    }.flowOn(Dispatchers.Default)
        .stateIn(scope, SharingStarted.WhileSubscribed(5000), emptyList())

    val sortedTracks: StateFlow<List<Track>> = combine(filteredTracks, sortField, sortAscending) { list, field, asc ->
        sortTrackList(list, field, asc)
    }.flowOn(Dispatchers.Default)
        .stateIn(scope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun sortTrackList(list: List<Track>, field: SortField, asc: Boolean): List<Track> {
        val sorted = when (field) {
            SortField.TITLE -> list.sortedBy { it.normTitle.ifEmpty { it.title.lowercase() } }
            SortField.ARTIST -> list.sortedBy { it.normArtist.ifEmpty { it.artist.lowercase() } }
            SortField.DATE_ADDED -> list.sortedBy { it.dateAddedMs }
            SortField.DURATION -> list.sortedBy { it.durationMs }
        }
        return if (asc) sorted else sorted.reversed()
    }

    private fun buildAlphabetIndex(list: List<Track>): Map<Char, Int> = buildMap {
        list.forEachIndexed { index, track ->
            val letter = firstLetter(track.title)
            if (letter !in this) put(letter, index)
        }
    }

    val songsAlphabetIndex: StateFlow<Map<Char, Int>> = sortedTracks
        .map { buildAlphabetIndex(it) }
        .flowOn(Dispatchers.Default)
        .stateIn(scope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val missingAlphabetIndex: StateFlow<Map<Char, Int>> = missingTracks
        .map { buildAlphabetIndex(it) }
        .flowOn(Dispatchers.Default)
        .stateIn(scope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val albums: StateFlow<List<AlbumItem>> = combine(filteredTracks, entitySortMode) { list, sort ->
        val grouped = list.groupBy { it.album.ifBlank { "Unknown Album" } }
            .map { (name, group) ->
                val firstWithArt = group.firstOrNull { it.uri.isNotBlank() }
                AlbumItem(name, group.first().artist, firstWithArt?.uri, group.size)
            }
        when (sort) {
            EntitySortMode.NAME_ASC -> grouped.sortedBy { it.name.lowercase() }
            EntitySortMode.NAME_DESC -> grouped.sortedByDescending { it.name.lowercase() }
            EntitySortMode.COUNT_DESC -> grouped.sortedByDescending { it.count }
        }
    }.flowOn(Dispatchers.Default)
        .stateIn(scope, SharingStarted.WhileSubscribed(5000), emptyList())

    val artists: StateFlow<List<ArtistItem>> = combine(
        filteredTracks,
        entitySortMode,
        splitMultiArtists,
        artistSplitExceptions
    ) { list, sort, splitEnabled, userExceptions ->
        buildArtistItems(list, sort, splitEnabled, userExceptions)
    }.flowOn(Dispatchers.Default)
        .stateIn(scope, SharingStarted.WhileSubscribed(5000), emptyList())

    val folders: StateFlow<List<FolderItem>> = combine(filteredTracks, entitySortMode) { list, sort ->
        val grouped = list.groupBy { it.folder.ifBlank { "Music" } }
            .map { (path, group) ->
                FolderItem(path = path, name = path.substringAfterLast('/'), count = group.size)
            }
        when (sort) {
            EntitySortMode.NAME_ASC -> grouped.sortedBy { it.name.lowercase() }
            EntitySortMode.NAME_DESC -> grouped.sortedByDescending { it.name.lowercase() }
            EntitySortMode.COUNT_DESC -> grouped.sortedByDescending { it.count }
        }
    }.flowOn(Dispatchers.Default)
        .stateIn(scope, SharingStarted.WhileSubscribed(5000), emptyList())

    val folderTreeState: StateFlow<FolderTreeState> = combine(filteredTracks, _currentFolderPath) { list, path ->
        FolderTreeBuilder.browseAt(list, path)
    }.flowOn(Dispatchers.Default)
        .stateIn(
            scope,
            SharingStarted.WhileSubscribed(5000),
            FolderTreeState("", "", emptyList(), emptyList(), emptyList())
        )

    val genres: StateFlow<List<GenreStat>> = db.tracks().observeGenres()
        .stateIn(scope, SharingStarted.WhileSubscribed(5000), emptyList())

    val decades: StateFlow<List<DecadeStat>> = filteredTracks
        .map { list ->
            list.filter { it.year in 1900..2100 }
                .groupBy { (it.year / 10) * 10 }
                .entries
                .sortedByDescending { it.key }
                .map { (decade, group) ->
                    DecadeStat(
                        decade = decade,
                        label = "${decade}s",
                        trackCount = group.size,
                        totalMs = group.sumOf { it.durationMs.coerceAtLeast(0L) },
                        artUri = group.firstOrNull { it.uri.isNotBlank() }?.uri
                    )
                }
        }.flowOn(Dispatchers.Default)
        .stateIn(scope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentTracks: StateFlow<List<Track>> = tracks
        .map { list -> list.sortedByDescending { it.dateAddedMs }.take(12) }
        .flowOn(Dispatchers.Default)
        .stateIn(scope, SharingStarted.WhileSubscribed(5000), emptyList())

    val topPlayedTracks: StateFlow<List<Track>> = combine(db.stats().observeTopIds(25), tracks) { ids, all ->
        if (ids.isEmpty()) emptyList() else {
            val map = all.associateBy { it.id }
            ids.mapNotNull { map[it] }
        }
    }.flowOn(Dispatchers.Default)
        .stateIn(scope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentlyPlayedTracks: StateFlow<List<Track>> = combine(db.stats().observeRecentIds(30), tracks) { ids, all ->
        if (ids.isEmpty()) emptyList() else {
            val map = all.associateBy { it.id }
            ids.mapNotNull { map[it] }
        }
    }.flowOn(Dispatchers.Default)
        .stateIn(scope, SharingStarted.WhileSubscribed(5000), emptyList())

    val customSmartPlaylists: StateFlow<List<SmartPlaylistEntity>> = db.smartPlaylists().all()
        .stateIn(scope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun observeSmartPlaylistTracks(smartId: Long): Flow<List<Track>> = combine(
        db.smartPlaylists().all(),
        tracks
    ) { allDefs, _ ->
        val def = allDefs.firstOrNull { it.id == smartId } ?: return@combine emptyList()
        smartEngine.evaluate(def, minDurationMs = 0L)
    }.flowOn(Dispatchers.IO)

    suspend fun previewSmartRuleMatchCount(
        rules: SmartPlaylistEngine.SmartRules,
        limitCount: Int? = null
    ): Int {
        if (rules.conditions.isEmpty()) return 0
        val temp = SmartPlaylistEntity(
            name = "preview",
            rulesJson = smartEngine.rulesToJson(rules),
            createdAtMs = 0L,
            limitCount = limitCount
        )
        return smartEngine.evaluate(temp, minDurationMs = 0L).size
    }

    fun saveSmartPlaylist(
        id: Long?,
        name: String,
        rules: SmartPlaylistEngine.SmartRules,
        sortKey: String?,
        sortOrder: Int?,
        limitCount: Int?,
        onDone: (() -> Unit)? = null
    ) = scope.launch(Dispatchers.IO) {
        val clean = name.trim()
        if (clean.isBlank() || rules.conditions.isEmpty()) return@launch
        val json = smartEngine.rulesToJson(rules)
        if (id != null && id > 0L) {
            db.smartPlaylists().update(id, clean, json, sortKey, sortOrder, limitCount)
        } else {
            db.smartPlaylists().insert(
                SmartPlaylistEntity(
                    name = clean,
                    rulesJson = json,
                    createdAtMs = System.currentTimeMillis(),
                    sortKey = sortKey,
                    sortOrder = sortOrder,
                    limitCount = limitCount
                )
            )
        }
        withContext(Dispatchers.Main) {
            onSnackbar("Saved smart playlist '$clean'")
            onDone?.invoke()
        }
    }

    fun deleteSmartPlaylist(id: Long) = scope.launch(Dispatchers.IO) {
        db.smartPlaylists().delete(id)
        withContext(Dispatchers.Main) {
            onSnackbar("Deleted smart playlist")
        }
    }

    fun playSimilarMix(seed: Track) {
        val library = tracks.value
        if (library.isEmpty()) {
            onSnackbar("Library is empty")
            return
        }
        val seedGenre = seed.genre.trim().lowercase()
        val seedArtist = seed.artist.trim().lowercase()
        val seedDecade = if (seed.year in 1900..2100) (seed.year / 10) * 10 else -1
        val candidates = library
            .asSequence()
            .filter { it.id != seed.id }
            .map { candidate ->
                var score = 0
                if (seedGenre.isNotEmpty() && candidate.genre.trim().lowercase() == seedGenre) score += 45
                if (seedArtist.isNotEmpty() && candidate.artist.trim().lowercase() == seedArtist) score += 25
                if (seedDecade > 0 && candidate.year in 1900..2100 && ((candidate.year / 10) * 10) == seedDecade) score += 15
                if (seed.durationMs > 0L && candidate.durationMs > 0L) {
                    val diff = abs(candidate.durationMs - seed.durationMs).toFloat() / seed.durationMs.toFloat()
                    if (diff <= 0.2f) score += 10
                }
                if (candidate.isFavorite) score += 5
                candidate to score
            }
            .sortedWith(compareByDescending<Pair<Track, Int>> { it.second }.thenBy { it.first.title.lowercase() })
            .take(24)
            .map { it.first }
            .toList()
        val mix = listOf(seed) + candidates
        player.playQueue(mix, 0, shuffle = false)
        onSnackbar("Playing Similar Mix (${mix.size} songs)")
    }

    fun removeDeletedDuplicates(ids: List<String>, onDone: (() -> Unit)? = null) = scope.launch(Dispatchers.IO) {
        if (ids.isEmpty()) return@launch
        val deleted = db.tracks().deleteByIds(ids)
        db.playlists().purgeMissingFromPlaylists()
        ArtworkLoader.clearCache()
        withContext(Dispatchers.Main) {
            onSnackbar("Removed $deleted duplicate track(s)")
            onDone?.invoke()
        }
    }

    companion object {
        fun buildArtistItems(
            list: List<Track>,
            sort: EntitySortMode,
            splitEnabled: Boolean,
            userExceptions: Set<String>
        ): List<ArtistItem> {
            val effectiveExceptions = ArtistParser.DEFAULT_EXCEPTIONS + userExceptions.map { norm(it) }
            val byKey = LinkedHashMap<String, Pair<String, MutableList<Track>>>()
            for (track in list) {
                val raw = track.artist.ifBlank { "Unknown Artist" }
                val names = if (splitEnabled) {
                    ArtistParser.split(raw, exceptions = effectiveExceptions).ifEmpty { listOf("Unknown Artist") }
                } else {
                    listOf(raw)
                }
                for (name in names) {
                    val key = name.lowercase()
                    val existing = byKey[key]
                    if (existing == null) {
                        byKey[key] = name to mutableListOf(track)
                    } else {
                        existing.second.add(track)
                    }
                }
            }
            val items = byKey.values.map { (displayName, group) ->
                val albumCount = group.mapTo(HashSet()) { it.album }.size
                ArtistItem(displayName, group.size, albumCount)
            }
            return when (sort) {
                EntitySortMode.NAME_ASC -> items.sortedBy { it.name.lowercase() }
                EntitySortMode.NAME_DESC -> items.sortedByDescending { it.name.lowercase() }
                EntitySortMode.COUNT_DESC -> items.sortedByDescending { it.trackCount }
            }
        }

        fun tracksForArtist(
            tracks: List<Track>,
            artistName: String,
            splitEnabled: Boolean,
            userExceptions: Set<String>
        ): List<Track> {
            if (!splitEnabled) {
                return tracks.filter { it.artist.equals(artistName, ignoreCase = true) }
            }
            val effectiveExceptions = ArtistParser.DEFAULT_EXCEPTIONS + userExceptions.map { norm(it) }
            return tracks.filter { track ->
                ArtistParser.split(track.artist, exceptions = effectiveExceptions)
                    .any { it.equals(artistName, ignoreCase = true) }
            }
        }
    }
}
