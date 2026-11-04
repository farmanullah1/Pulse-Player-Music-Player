package com.pulse.player.playback

import androidx.media3.common.Player
import com.pulse.player.data.db.TrackWithFav

data class Track(
    val id: String,
    val uri: String,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val isFavorite: Boolean,
    val folder: String = "",
    val dateAddedMs: Long = 0,
    val mimeType: String? = null
)

fun TrackWithFav.toTrack() = Track(
    id = track.id,
    uri = track.uri,
    title = track.title,
    artist = track.artist,
    album = track.album,
    durationMs = track.durationMs,
    isFavorite = isFavorite,
    folder = track.folderPath,
    dateAddedMs = track.dateAddedMs,
    mimeType = track.mimeType
)

fun com.pulse.player.data.db.TrackEntity.toTrack(isFavorite: Boolean = false) = Track(
    id = id,
    uri = uri,
    title = title,
    artist = artist,
    album = album,
    durationMs = durationMs,
    isFavorite = isFavorite,
    folder = folderPath,
    dateAddedMs = dateAddedMs,
    mimeType = mimeType
)

data class PlaybackUiState(
    val mediaId: String? = null,
    val title: String = "",
    val artist: String = "",
    val album: String = "",
    val artworkUri: String? = null,
    val isPlaying: Boolean = false,
    val playWhenReady: Boolean = false,
    val buffering: Boolean = false,
    val durationMs: Long = 0,
    val index: Int = -1,
    val queueSize: Int = 0,
    val shuffle: Boolean = false,
    val repeat: Int = Player.REPEAT_MODE_OFF,
    val speed: Float = 1f,
    val pitchLocked: Boolean = true,
    val skipSilence: Boolean = false,
    val sleepRemainingMs: Long = 0,
    val sleepEndOfTrack: Boolean = false,
    val canSeek: Boolean = false,
    val error: String? = null,
    val trackError: ErrorPolicy.TrackError? = null,
    val queueNotice: String? = null
) {
    val artRef: com.pulse.player.data.scan.ArtRef?
        get() = artworkUri?.let { com.pulse.player.data.scan.ArtRef(mediaId ?: it, it, 0L) }
}
