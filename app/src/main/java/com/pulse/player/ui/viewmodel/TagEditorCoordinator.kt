package com.pulse.player.ui.viewmodel

import android.content.Context
import android.content.IntentSender
import com.pulse.player.data.ErrorCategory
import com.pulse.player.data.ErrorDispatcher
import com.pulse.player.data.db.PulseDb
import com.pulse.player.data.scan.AudioTagEdit
import com.pulse.player.data.scan.AudioTagWriter
import com.pulse.player.data.scan.TagWriteResult
import com.pulse.player.data.scan.norm
import com.pulse.player.playback.Track
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class PendingAudioWriteRequest(
    val intentSender: IntentSender,
    val track: Track,
    val edit: AudioTagEdit,
    val onDone: (() -> Unit)?
)

class TagEditorCoordinator(
    private val ctx: Context,
    private val db: PulseDb,
    private val tracks: StateFlow<List<Track>>,
    private val selected: StateFlow<Set<String>>,
    private val clearSelect: () -> Unit,
    private val scope: CoroutineScope,
    private val onSnackbar: (String) -> Unit
) {
    val songDetailsTrack = MutableStateFlow<Track?>(null)
    val tagEditorTrack = MutableStateFlow<Track?>(null)
    val batchTagTracksOpen = MutableStateFlow<List<Track>?>(null)
    val pendingAudioWriteRequest = MutableStateFlow<PendingAudioWriteRequest?>(null)

    fun openTagEditor(t: Track?) { tagEditorTrack.value = t }
    fun closeTagEditor() { tagEditorTrack.value = null }

    fun openBatchTagEditor() {
        val sel = selected.value
        val targets = tracks.value.filter { it.id in sel }
        if (targets.isNotEmpty()) batchTagTracksOpen.value = targets
    }

    fun closeBatchTagEditor() { batchTagTracksOpen.value = null }

    fun clearPendingAudioWriteRequest() {
        pendingAudioWriteRequest.value = null
    }

    fun retryPendingAudioWrite(req: PendingAudioWriteRequest) = scope.launch(Dispatchers.IO) {
        pendingAudioWriteRequest.value = null
        val writeRes = AudioTagWriter.writeTags(ctx, req.track, req.edit)
        withContext(Dispatchers.Main) {
            when (writeRes) {
                is TagWriteResult.Success -> {
                    onSnackbar("Updated audio file & library tags")
                    req.onDone?.invoke()
                }
                is TagWriteResult.Unsupported -> {
                    onSnackbar(writeRes.reason)
                    req.onDone?.invoke()
                }
                is TagWriteResult.Error -> {
                    onSnackbar("File write failed (${writeRes.message}); saved in library")
                    req.onDone?.invoke()
                }
                is TagWriteResult.NeedsWritePermission -> {
                    onSnackbar("Write permission denied; changes saved to library only")
                    req.onDone?.invoke()
                }
            }
        }
    }

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
    ) = scope.launch(Dispatchers.IO) {
        val edit = AudioTagEdit(
            title = title.trim(),
            artist = artist.trim(),
            album = album.trim(),
            genre = genre.trim(),
            year = year,
            trackNumber = trackNumber
        )
        runCatching {
            db.tracks().updateTrackTags(
                id = id,
                title = edit.title,
                artist = edit.artist,
                album = edit.album,
                genre = edit.genre,
                year = edit.year,
                trackNumber = edit.trackNumber,
                normTitle = norm(edit.title),
                normArtist = norm(edit.artist),
                normAlbum = norm(edit.album)
            )
        }.onSuccess {
            if (!writeToFile) {
                withContext(Dispatchers.Main) {
                    onSnackbar("Track details updated")
                    onDone?.invoke()
                }
                return@onSuccess
            }
            val targetTrack = tracks.value.firstOrNull { it.id == id }
            if (targetTrack == null) {
                withContext(Dispatchers.Main) {
                    onSnackbar("Track details updated")
                    onDone?.invoke()
                }
                return@onSuccess
            }
            when (val writeRes = AudioTagWriter.writeTags(ctx, targetTrack, edit)) {
                is TagWriteResult.Success -> {
                    withContext(Dispatchers.Main) {
                        onSnackbar("Updated audio file & library tags")
                        onDone?.invoke()
                    }
                }
                is TagWriteResult.NeedsWritePermission -> {
                    withContext(Dispatchers.Main) {
                        pendingAudioWriteRequest.value = PendingAudioWriteRequest(
                            intentSender = writeRes.intentSender,
                            track = targetTrack,
                            edit = edit,
                            onDone = onDone
                        )
                    }
                }
                is TagWriteResult.Unsupported -> {
                    withContext(Dispatchers.Main) {
                        onSnackbar(writeRes.reason)
                        onDone?.invoke()
                    }
                }
                is TagWriteResult.Error -> {
                    withContext(Dispatchers.Main) {
                        onSnackbar("Saved to library (${writeRes.message})")
                        onDone?.invoke()
                    }
                }
            }
        }.onFailure { err ->
            ErrorDispatcher.post(
                category = ErrorCategory.STORAGE,
                title = "Tag Update Failed",
                message = err.message ?: "Failed to save track tags",
                technicalDetails = err.stackTraceToString()
            )
            withContext(Dispatchers.Main) {
                onSnackbar("Failed to update tags: ${err.message}")
            }
        }
    }

    fun batchUpdateTags(
        ids: List<String>,
        artist: String?,
        album: String?,
        genre: String?,
        year: Int?,
        onDone: (() -> Unit)? = null
    ) = scope.launch(Dispatchers.IO) {
        if (ids.isEmpty()) return@launch
        val cleanArtist = artist?.trim()?.takeIf { it.isNotEmpty() }
        val cleanAlbum = album?.trim()?.takeIf { it.isNotEmpty() }
        val cleanGenre = genre?.trim()?.takeIf { it.isNotEmpty() }
        val cleanYear = year?.takeIf { it > 0 }
        if (cleanArtist == null && cleanAlbum == null && cleanGenre == null && cleanYear == null) {
            withContext(Dispatchers.Main) {
                onSnackbar("Enter at least one field to update")
            }
            return@launch
        }
        runCatching {
            db.tracks().updateTagsBatch(
                ids = ids,
                artist = cleanArtist,
                normArtist = cleanArtist?.let(::norm),
                album = cleanAlbum,
                normAlbum = cleanAlbum?.let(::norm),
                genre = cleanGenre,
                year = cleanYear
            )
        }.onSuccess {
            withContext(Dispatchers.Main) {
                clearSelect()
                batchTagTracksOpen.value = null
                onSnackbar("Updated tags for ${ids.size} songs")
                onDone?.invoke()
            }
        }.onFailure { err ->
            ErrorDispatcher.post(
                category = ErrorCategory.STORAGE,
                title = "Batch Tag Update Failed",
                message = err.message ?: "Failed to update tags for selection",
                technicalDetails = err.stackTraceToString()
            )
            withContext(Dispatchers.Main) {
                onSnackbar("Batch update failed: ${err.message}")
            }
        }
    }
}
