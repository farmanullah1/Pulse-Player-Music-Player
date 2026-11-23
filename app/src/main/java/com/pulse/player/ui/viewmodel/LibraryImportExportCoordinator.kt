package com.pulse.player.ui.viewmodel

import android.content.Context
import android.net.Uri
import com.pulse.player.data.ErrorCategory
import com.pulse.player.data.ErrorDispatcher
import com.pulse.player.data.backup.common.LimitedStream
import com.pulse.player.data.db.PlaylistEntity
import com.pulse.player.data.db.PlaylistTrackEntity
import com.pulse.player.data.db.PulseDb
import com.pulse.player.data.db.SavedQueueEntity
import com.pulse.player.data.playlists.M3uPlaylistCodec
import com.pulse.player.playback.PlayerController
import com.pulse.player.playback.Track
import com.pulse.player.playback.toTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LibraryImportExportCoordinator(
    private val ctx: Context,
    private val db: PulseDb,
    private val player: PlayerController,
    private val scope: CoroutineScope,
    private val onSnackbar: (String) -> Unit
) {
    val savedQueues: StateFlow<List<SavedQueueEntity>> = db.savedQueues().allQueues()
        .stateIn(scope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun saveCurrentQueueSession(name: String) = scope.launch(Dispatchers.IO) {
        val clean = name.trim()
        val currentQueue = player.queue.value
        if (clean.isBlank() || currentQueue.isEmpty()) return@launch
        val ids = currentQueue.map { it.id }
        val index = player.state.value.index.coerceIn(0, (ids.size - 1).coerceAtLeast(0))
        val posMs = player.position.value.coerceAtLeast(0L)
        db.savedQueues().save(clean, ids, index, posMs)
        withContext(Dispatchers.Main) {
            onSnackbar("Saved queue snapshot '$clean' (${ids.size} tracks)")
        }
    }

    fun restoreSavedQueue(id: Long) = scope.launch(Dispatchers.IO) {
        val queueMeta = db.savedQueues().queueById(id) ?: return@launch
        val rawIds = db.savedQueues().trackIds(id)
        if (rawIds.isEmpty()) {
            withContext(Dispatchers.Main) { onSnackbar("Saved queue has no tracks") }
            return@launch
        }
        val entities = db.tracks().byIds(rawIds).associateBy { it.id }
        val resolvedTracks = rawIds.mapNotNull { entities[it]?.toTrack() }
        if (resolvedTracks.isEmpty()) {
            withContext(Dispatchers.Main) { onSnackbar("Tracks from '${queueMeta.name}' are unavailable") }
            return@launch
        }
        val resolvedIds = resolvedTracks.map { it.id }
        val safeIndex = queueMeta.currentIndex.coerceIn(0, resolvedTracks.lastIndex)
        withContext(Dispatchers.Main) {
            player.restore(resolvedIds, safeIndex, queueMeta.positionMs.coerceAtLeast(0L), resolvedTracks)
            onSnackbar("Restored '${queueMeta.name}' (${resolvedTracks.size} tracks)")
        }
    }

    fun deleteSavedQueue(id: Long) = scope.launch(Dispatchers.IO) {
        db.savedQueues().delete(id)
    }

    fun exportPlaylistM3u8(playlistId: Long, playlistName: String, destUri: Uri) = scope.launch(Dispatchers.IO) {
        runCatching {
            val playlistTracks = db.playlists().observeTracks(playlistId).first().map { it.toTrack() }
            val m3uText = M3uPlaylistCodec.exportM3u8(playlistName, playlistTracks)
            ctx.contentResolver.openOutputStream(destUri)?.use { out ->
                out.write(m3uText.toByteArray(Charsets.UTF_8))
                out.flush()
            } ?: error("Unable to open destination output stream")
            withContext(Dispatchers.Main) {
                onSnackbar("Exported '$playlistName' (${playlistTracks.size} tracks) to .m3u8")
            }
        }.onFailure { err ->
            ErrorDispatcher.post(
                category = ErrorCategory.STORAGE,
                title = "Playlist Export Failed",
                message = err.message ?: "Could not export M3U8 playlist.",
                technicalDetails = err.stackTraceToString()
            )
            withContext(Dispatchers.Main) {
                onSnackbar("Failed to export playlist: ${err.message}")
            }
        }
    }

    fun importM3uPlaylist(uri: Uri, libraryTracks: List<Track>, fallbackName: String = "Imported Playlist") =
        scope.launch(Dispatchers.IO) {
            runCatching {
                val stream = ctx.contentResolver.openInputStream(uri)
                    ?: error("Could not open playlist file")
                val text = LimitedStream(stream, 1024 * 1024L).reader(Charsets.UTF_8).use { it.readText() }
                val cleanFallback = fallbackName.substringBeforeLast('.').ifBlank { "Imported Playlist" }
                val (parsedName, entries) = M3uPlaylistCodec.parse(text, cleanFallback)
                if (entries.isEmpty()) {
                    withContext(Dispatchers.Main) { onSnackbar("No tracks found in playlist file") }
                    return@launch
                }
                val result = M3uPlaylistCodec.resolveMatches(parsedName, entries, libraryTracks)
                if (result.matchedTracks.isEmpty()) {
                    withContext(Dispatchers.Main) {
                        onSnackbar("None of the ${entries.size} entries matched library songs")
                    }
                    return@launch
                }

                val existing = db.playlists().byName(result.playlistName)
                val finalName = if (existing == null) result.playlistName else "${result.playlistName} (Imported)"
                val now = System.currentTimeMillis()
                val newPlaylistId = db.playlists().create(PlaylistEntity(name = finalName, createdAtMs = now))
                val items = result.matchedTracks.mapIndexed { idx, track ->
                    PlaylistTrackEntity(
                        playlistId = newPlaylistId,
                        trackId = track.id,
                        position = idx,
                        addedAtMs = now
                    )
                }
                db.playlists().insertPlaylistTracks(items)
                withContext(Dispatchers.Main) {
                    val skipped = result.unmatchedLocations.size
                    val msg = if (skipped > 0) {
                        "Imported '$finalName' (${items.size} matched, $skipped unmatched)"
                    } else {
                        "Imported '$finalName' (${items.size} tracks)"
                    }
                    onSnackbar(msg)
                }
            }.onFailure { err ->
                ErrorDispatcher.post(
                    category = ErrorCategory.STORAGE,
                    title = "Playlist Import Failed",
                    message = err.message ?: "Could not import M3U/M3U8 playlist.",
                    technicalDetails = err.stackTraceToString()
                )
                withContext(Dispatchers.Main) {
                    onSnackbar("Failed to import playlist: ${err.message}")
                }
            }
        }
}
