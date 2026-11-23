package com.pulse.player.playback

import androidx.compose.runtime.Immutable
import androidx.media3.common.Player
import com.pulse.player.data.db.TrackWithFav

@Immutable
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
    val mimeType: String? = null,
    val normTitle: String = "",
    val normArtist: String = "",
    val normAlbum: String = "",
    val genre: String = "",
    val year: Int = 0,
    val codec: String = "",
    val dataPath: String = "",
    val replayGainTrackDb: Float = 0f,
    val replayGainAlbumDb: Float = 0f
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
    mimeType = track.mimeType,
    normTitle = track.normTitle.ifEmpty { track.title.lowercase() },
    normArtist = track.normArtist.ifEmpty { track.artist.lowercase() },
    normAlbum = track.normAlbum.ifEmpty { track.album.lowercase() },
    genre = track.genre,
    year = track.year,
    codec = track.codec,
    dataPath = track.dataPath.orEmpty(),
    replayGainTrackDb = track.replayGainTrackDb,
    replayGainAlbumDb = track.replayGainAlbumDb
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
    mimeType = mimeType,
    normTitle = normTitle.ifEmpty { title.lowercase() },
    normArtist = normArtist.ifEmpty { artist.lowercase() },
    normAlbum = normAlbum.ifEmpty { album.lowercase() },
    genre = genre,
    year = year,
    codec = codec,
    dataPath = dataPath.orEmpty(),
    replayGainTrackDb = replayGainTrackDb,
    replayGainAlbumDb = replayGainAlbumDb
)

@Immutable
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
    val pitchSemitones: Float = 0f,
    val skipSilence: Boolean = false,
    val sleepRemainingMs: Long = 0,
    val sleepEndOfTrack: Boolean = false,
    val sleepFadeSec: Int = 10,
    val abLoopStartMs: Long? = null,
    val abLoopEndMs: Long? = null,
    val canSeek: Boolean = false,
    val error: String? = null,
    val trackError: ErrorPolicy.TrackError? = null,
    val queueNotice: String? = null,
    val appliedReplayGainDb: Float = 0f
) {
    val artRef: com.pulse.player.data.scan.ArtRef?
        get() = artworkUri?.let { com.pulse.player.data.scan.ArtRef(mediaId ?: it, it, 0L) }
}
