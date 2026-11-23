package com.pulse.player.ui.viewmodel

import android.net.Uri
import com.pulse.player.data.db.PulseDb
import com.pulse.player.data.db.TrackWithFav
import com.pulse.player.library.FolderTreeBuilder
import com.pulse.player.playback.PlayerController
import com.pulse.player.playback.Track
import com.pulse.player.playback.toTrack
import com.pulse.player.ui.theme.immersive.QueueItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.random.Random

class QueuePlaybackCoordinator(
    private val db: PulseDb,
    private val player: PlayerController,
    private val tracks: StateFlow<List<Track>>,
    private val neverPlayed: StateFlow<List<Track>>,
    private val selected: StateFlow<Set<String>>,
    private val clearSelect: () -> Unit,
    private val splitMultiArtists: StateFlow<Boolean>,
    private val artistSplitExceptions: StateFlow<Set<String>>,
    private val folderTreeMode: StateFlow<Boolean>,
    private val scope: CoroutineScope,
    private val onSnackbar: (String) -> Unit,
    private val onSnackbarAction: (SnackbarActionEvent) -> Unit,
    private val onOpenNowPlaying: () -> Unit
) {
    val queueItems: StateFlow<List<QueueItem>> = player.queue.map { list ->
        list.map {
            QueueItem(
                mediaId = it.id,
                title = it.title,
                artist = it.artist,
                artUri = it.uri,
                durationMs = it.durationMs
            )
        }
    }.flowOn(Dispatchers.Default)
        .stateIn(scope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun playAt(i: Int) = player.playAt(i)
    fun removeAt(i: Int) = player.removeAt(i)
    fun toggleShuffle() = player.toggleShuffle()
    fun cycleRepeat() = player.cycleRepeat()
    fun retry() = player.retry()
    fun skip() = player.skip()

    fun playQueue(list: List<Track>, startIndex: Int, shuffle: Boolean = false) {
        player.playQueue(list, startIndex, shuffle)
    }

    fun playNext(t: Track) { player.playNext(t) }
    fun addToQueue(t: Track) { player.addToQueue(t) }

    fun playExternal(uri: Uri) {
        player.playExternal(uri)
        onOpenNowPlaying()
    }

    fun playTrackById(id: String) = scope.launch(Dispatchers.IO) {
        val entity = db.tracks().byId(id) ?: return@launch
        val isFav = db.favorites().isFavorite(id)
        val track = TrackWithFav(entity, isFav).toTrack()
        withContext(Dispatchers.Main) {
            player.playTrack(track)
            onOpenNowPlaying()
        }
    }

    fun removeFromQueueWithUndo(index: Int) {
        val removed = player.removeFromQueueWithUndo(index) ?: return
        onSnackbarAction(
            SnackbarActionEvent(
                message = "Removed '${removed.title}' from queue",
                actionLabel = "Undo",
                onAction = { player.insertTrackAt(index, removed) }
            )
        )
    }

    fun shuffleUpcomingQueue() {
        val count = player.shuffleUpcoming()
        onSnackbar(if (count > 0) "Shuffled $count upcoming tracks" else "Need more upcoming tracks to shuffle")
    }

    fun playAlbum(albumName: String, shuffle: Boolean = false) {
        val list = tracks.value.filter { it.album.equals(albumName, ignoreCase = true) }
        if (list.isNotEmpty()) player.playQueue(list, 0, shuffle)
    }

    fun queueAlbumNext(albumName: String) {
        val list = tracks.value.filter { it.album.equals(albumName, ignoreCase = true) }
        list.asReversed().forEach { player.playNext(it) }
        if (list.isNotEmpty()) onSnackbar("Queued ${list.size} tracks from $albumName")
    }

    fun playArtist(artistName: String, shuffle: Boolean = false) {
        val list = LibraryBrowserCoordinator.tracksForArtist(
            tracks = tracks.value,
            artistName = artistName,
            splitEnabled = splitMultiArtists.value,
            userExceptions = artistSplitExceptions.value
        )
        if (list.isNotEmpty()) player.playQueue(list, 0, shuffle)
    }

    fun queueArtistNext(artistName: String) {
        val list = LibraryBrowserCoordinator.tracksForArtist(
            tracks = tracks.value,
            artistName = artistName,
            splitEnabled = splitMultiArtists.value,
            userExceptions = artistSplitExceptions.value
        )
        list.asReversed().forEach { player.playNext(it) }
        if (list.isNotEmpty()) onSnackbar("Queued ${list.size} tracks by $artistName")
    }

    fun playFolder(folderPath: String, shuffle: Boolean = false, recursive: Boolean = folderTreeMode.value) {
        val list = FolderTreeBuilder.tracksUnderFolder(tracks.value, folderPath, recursive = recursive)
        if (list.isNotEmpty()) player.playQueue(list, 0, shuffle)
    }

    fun queueFolderNext(folderPath: String, recursive: Boolean = folderTreeMode.value) {
        val list = FolderTreeBuilder.tracksUnderFolder(tracks.value, folderPath, recursive = recursive)
        list.asReversed().forEach { player.playNext(it) }
        if (list.isNotEmpty()) onSnackbar("Queued ${list.size} tracks from folder")
    }

    fun playSelectedNext() {
        val sel = selected.value
        val items = tracks.value.filter { it.id in sel }
        items.asReversed().forEach { player.playNext(it) }
        onSnackbar("Queued ${items.size} selected track(s) next")
        clearSelect()
    }

    fun currentPlayingTrack(): Track? {
        val curId = player.state.value.mediaId ?: return null
        return tracks.value.firstOrNull { it.id == curId }
    }

    fun shuffleAll() {
        val list = tracks.value
        if (list.isEmpty()) {
            onSnackbar("No songs in library to shuffle")
            return
        }
        player.playQueue(list.shuffled(Random(System.currentTimeMillis())), 0)
    }

    fun shuffleFavorites() {
        val list = tracks.value.filter { it.isFavorite }
        if (list.isEmpty()) {
            onSnackbar("No favourites yet")
            return
        }
        player.playQueue(list.shuffled(Random(System.currentTimeMillis())), 0)
    }

    fun shuffleUnplayed() {
        val list = neverPlayed.value
        if (list.isEmpty()) {
            onSnackbar("All library tracks have been played!")
            return
        }
        player.playQueue(list.shuffled(Random(System.currentTimeMillis())), 0)
    }

    fun shuffleRecentlyAdded() {
        val list = tracks.value.sortedByDescending { it.dateAddedMs }.take(50)
        if (list.isEmpty()) {
            onSnackbar("No recently added songs")
            return
        }
        player.playQueue(list.shuffled(Random(System.currentTimeMillis())), 0)
    }
}
