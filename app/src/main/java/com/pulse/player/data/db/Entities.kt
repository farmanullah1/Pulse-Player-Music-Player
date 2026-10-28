package com.pulse.player.data.db

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "tracks",
    indices = [
        Index("title"),
        Index("artist"),
        Index("album"),
        Index("folderPath"),
        Index("dateAddedMs")
    ]
)
data class TrackEntity(
    @PrimaryKey val id: String, // "ms:<mediaStoreId>" or "saf:<hash>"
    val uri: String,
    val displayName: String,
    val title: String,
    val artist: String,
    val album: String,
    val albumArtist: String?,
    val durationMs: Long,
    val sizeBytes: Long,
    val mimeType: String?,
    val folderPath: String,
    val dateAddedMs: Long,
    val dateModifiedMs: Long,
    val trackNumber: Int,
    val year: Int,
    val normTitle: String,
    val normArtist: String,
    val normAlbum: String,
    val available: Boolean = true,
    val dataPath: String? = null,
    val scanGen: Long = 0
)

@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey val trackId: String,
    val addedAtMs: Long
)

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAtMs: Long,
    val sortKey: String? = null,
    val sortOrder: Int? = null
)

@Entity(
    tableName = "playlist_tracks",
    primaryKeys = ["playlistId", "trackId"],
    indices = [Index("trackId")]
)
data class PlaylistTrackEntity(
    val playlistId: Long,
    val trackId: String,
    val position: Int,
    val addedAtMs: Long
)

@Entity(
    tableName = "play_events",
    indices = [
        Index("playedAtMs"),
        Index("trackId"),
        Index(value = ["trackId", "playedAtMs"], unique = true)
    ]
)
data class PlayEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val trackId: String,
    val playedAtMs: Long,
    val listenedMs: Long,
    val completed: Boolean
)

data class TrackWithFav(
    @Embedded val track: TrackEntity,
    val isFavorite: Boolean
)

data class TrackStat(
    val trackId: String,
    val plays: Int,
    val ms: Long
)

data class ArtistStat(
    val artist: String,
    val plays: Int,
    val ms: Long
)

data class FavAdded(
    val trackId: String,
    val addedAtMs: Long
)

data class PlaybackRow(
    val trackId: String,
    val dataPath: String?,
    val playedAtMs: Long,
    val listenedMs: Long,
    val completed: Boolean
)

data class DupGroup(
    val normTitle: String,
    val normArtist: String,
    val c: Int
)
