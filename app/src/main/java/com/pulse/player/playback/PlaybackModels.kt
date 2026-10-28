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
    val sleepRemainingMs: Long = 0,
    val sleepEndOfTrack: Boolean = false,
    val error: String? = null
)
