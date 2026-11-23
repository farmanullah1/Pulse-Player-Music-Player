package com.pulse.player.ui.viewmodel

import com.pulse.player.data.db.FtsQueryBuilder
import com.pulse.player.data.db.PulseDb
import com.pulse.player.data.scan.norm
import com.pulse.player.data.settings.SettingsStore
import com.pulse.player.playback.Track
import com.pulse.player.ui.screens.AlbumItem
import com.pulse.player.ui.screens.ArtistItem
import com.pulse.player.ui.screens.FolderItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(FlowPreview::class)
class SearchCoordinator(
    private val db: PulseDb,
    private val settings: SettingsStore,
    tracks: StateFlow<List<Track>>,
    albums: StateFlow<List<AlbumItem>>,
    artists: StateFlow<List<ArtistItem>>,
    folders: StateFlow<List<FolderItem>>,
    sortField: StateFlow<SortField>,
    sortAscending: StateFlow<Boolean>,
    private val sortTrackList: (List<Track>, SortField, Boolean) -> List<Track>,
    private val scope: CoroutineScope
) {
    val searchQuery = MutableStateFlow("")
    val searchScope = MutableStateFlow(SearchScope.ALL)
    val recentSearches: StateFlow<List<String>> = settings.recentSearches
        .stateIn(scope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setSearchScope(scopeValue: SearchScope) {
        searchScope.value = scopeValue
    }

    fun commitRecentSearch(query: String = searchQuery.value) {
        val clean = query.trim()
        if (clean.length >= 2) {
            scope.launch { settings.addRecentSearch(clean) }
        }
    }

    fun removeRecentSearch(query: String) {
        scope.launch { settings.removeRecentSearch(query) }
    }

    fun clearRecentSearches() {
        scope.launch { settings.clearRecentSearches() }
    }

    private fun Track.matchesTokens(tokens: List<String>, activeScope: SearchScope): Boolean {
        if (tokens.isEmpty()) return true
        val titleVal = normTitle.ifEmpty { title.lowercase() }
        val artistVal = normArtist.ifEmpty { artist.lowercase() }
        val albumVal = normAlbum.ifEmpty { album.lowercase() }
        val genreVal = genre.lowercase()
        val folderVal = folder.lowercase()
        return tokens.all { tok ->
            when (activeScope) {
                SearchScope.SONGS -> titleVal.contains(tok)
                SearchScope.ARTISTS -> artistVal.contains(tok)
                SearchScope.ALBUMS -> albumVal.contains(tok)
                SearchScope.FOLDERS -> folderVal.contains(tok)
                SearchScope.ALL, SearchScope.FAVORITES ->
                    titleVal.contains(tok) || artistVal.contains(tok) ||
                        albumVal.contains(tok) || genreVal.contains(tok) || folderVal.contains(tok)
            }
        }
    }

    private suspend fun queryFtsIds(rawQuery: String): Set<String> {
        val ftsQuery = FtsQueryBuilder.buildPrefixQuery(rawQuery)
        if (ftsQuery.isEmpty()) return emptySet()
        return withContext(Dispatchers.IO) {
            runCatching { db.tracks().searchFtsIds(ftsQuery).toHashSet() }
                .getOrDefault(emptySet())
        }
    }

    val searchResults: StateFlow<List<Track>> = combine(
        tracks,
        searchQuery.debounce(100),
        searchScope,
        sortField,
        sortAscending
    ) { list, q, activeScope, field, asc ->
        if (q.isBlank() && activeScope != SearchScope.FAVORITES) {
            emptyList()
        } else {
            val tokens = norm(q).split(' ').filter { it.isNotBlank() }
            val base = when (activeScope) {
                SearchScope.FAVORITES -> list.filter { it.isFavorite }
                else -> list
            }
            if (tokens.isEmpty()) {
                sortTrackList(base, field, asc)
            } else {
                val ftsIds = if (activeScope == SearchScope.ALL || activeScope == SearchScope.SONGS) {
                    queryFtsIds(q)
                } else {
                    emptySet()
                }
                val ftsMatched = if (ftsIds.isNotEmpty() && activeScope == SearchScope.ALL) {
                    base.filter { it.id in ftsIds }
                } else {
                    emptyList()
                }
                val substringMatched = base.filter { it.id !in ftsIds && it.matchesTokens(tokens, activeScope) }
                val combined = if (activeScope == SearchScope.SONGS) {
                    val ftsSongMatches = base.filter { it.id in ftsIds && it.matchesTokens(tokens, SearchScope.SONGS) }
                    ftsSongMatches + substringMatched
                } else {
                    ftsMatched + substringMatched
                }
                sortTrackList(combined.distinctBy { it.id }, field, asc)
            }
        }
    }.conflate()
        .flowOn(Dispatchers.Default)
        .stateIn(scope, SharingStarted.WhileSubscribed(5000), emptyList())

    val searchArtistResults: StateFlow<List<ArtistItem>> = combine(artists, searchQuery.debounce(100)) { list, q ->
        if (q.isBlank()) emptyList() else {
            val tokens = q.trim().lowercase().split(' ').filter { it.isNotBlank() }
            list.filter { item -> tokens.all { item.name.contains(it, ignoreCase = true) } }.take(15)
        }
    }.flowOn(Dispatchers.Default).stateIn(scope, SharingStarted.WhileSubscribed(5000), emptyList())

    val searchAlbumResults: StateFlow<List<AlbumItem>> = combine(albums, searchQuery.debounce(100)) { list, q ->
        if (q.isBlank()) emptyList() else {
            val tokens = q.trim().lowercase().split(' ').filter { it.isNotBlank() }
            list.filter { item ->
                tokens.all { item.name.contains(it, ignoreCase = true) || item.artist.contains(it, ignoreCase = true) }
            }.take(15)
        }
    }.flowOn(Dispatchers.Default).stateIn(scope, SharingStarted.WhileSubscribed(5000), emptyList())

    val searchFolderResults: StateFlow<List<FolderItem>> = combine(folders, searchQuery.debounce(100)) { list, q ->
        if (q.isBlank()) emptyList() else {
            val tokens = q.trim().lowercase().split(' ').filter { it.isNotBlank() }
            list.filter { item ->
                tokens.all { item.name.contains(it, ignoreCase = true) || item.path.contains(it, ignoreCase = true) }
            }.take(15)
        }
    }.flowOn(Dispatchers.Default).stateIn(scope, SharingStarted.WhileSubscribed(5000), emptyList())

    val searchScopeCounts: StateFlow<Map<SearchScope, Int>> = combine(
        tracks,
        searchQuery.debounce(100),
        searchArtistResults,
        searchAlbumResults,
        searchFolderResults
    ) { list, q, matchedArtists, matchedAlbums, matchedFolders ->
        val favCount = list.count { it.isFavorite }
        if (q.isBlank()) {
            mapOf(SearchScope.FAVORITES to favCount)
        } else {
            val tokens = norm(q).split(' ').filter { it.isNotBlank() }
            val ftsIds = queryFtsIds(q)
            val songMatches = list.count { it.matchesTokens(tokens, SearchScope.SONGS) }
            val allMatches = if (ftsIds.isNotEmpty()) {
                list.count { it.id in ftsIds || it.matchesTokens(tokens, SearchScope.ALL) }
            } else {
                list.count { it.matchesTokens(tokens, SearchScope.ALL) }
            }
            val favMatches = list.count { it.isFavorite && it.matchesTokens(tokens, SearchScope.FAVORITES) }
            mapOf(
                SearchScope.ALL to allMatches,
                SearchScope.SONGS to songMatches,
                SearchScope.ARTISTS to matchedArtists.size,
                SearchScope.ALBUMS to matchedAlbums.size,
                SearchScope.FOLDERS to matchedFolders.size,
                SearchScope.FAVORITES to favMatches
            )
        }
    }.flowOn(Dispatchers.Default).stateIn(scope, SharingStarted.WhileSubscribed(5000), emptyMap())
}
