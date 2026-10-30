package com.pulse.player.data.db

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.security.MessageDigest

object IdentityModel {
    fun mediaStoreId(id: Long): String = "ms:$id"
    fun safId(treeUri: String, docId: String): String = "saf:${sha1("$treeUri|$docId")}"
    fun copyId(destUri: String): String = "copy:${sha1(destUri)}"

    fun sha1(s: String): String {
        val md = MessageDigest.getInstance("SHA-1")
        return md.digest(s.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
    }
}

@Entity(
    tableName = "tracks",
    indices = [
        Index("title"),
        Index("artist"),
        Index("album"),
        Index("folderPath"),
        Index("dateAddedMs"),
        Index("sourceType", "volumeId")
    ]
)
data class TrackEntity(
    @PrimaryKey val id: String, // "ms:<mediaStoreId>" or "saf:<sha1>" or "copy:<sha1>"
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
    val scanGen: Long = 0,
    @ColumnInfo(defaultValue = "'MEDIASTORE'") val sourceType: String = "MEDIASTORE", // MEDIASTORE | SAF | COPIED
    @ColumnInfo(defaultValue = "''") val stableId: String = "", // MediaStore _ID, SAF document id, or copy id
    @ColumnInfo(defaultValue = "'external_primary'") val volumeId: String = "external_primary", // MediaStore VOLUME_NAME, or volume of SAF doc
    val folderUri: String? = null // SAF tree uri; null for MediaStore
)

@Entity(tableName = "scan_seen")
data class ScanSeenEntity(
    @PrimaryKey val id: String
)

@Entity(tableName = "lyrics")
data class LyricsEntity(
    @PrimaryKey val trackId: String,
    val text: String,
    val synced: Boolean,
    val importedAtMs: Long
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
        Index("monthKey"),
        Index(value = ["trackId", "playedAtMs"], unique = true)
    ]
)
data class PlayEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val trackId: String,
    val playedAtMs: Long,
    val listenedMs: Long,
    val completed: Boolean,
    @ColumnInfo(defaultValue = "''") val zoneId: String = "",
    @ColumnInfo(defaultValue = "''") val monthKey: String = "",
    @ColumnInfo(defaultValue = "0") val playedFraction: Float = 0f,
    @ColumnInfo(defaultValue = "0") val wasSkipped: Boolean = false,
    @ColumnInfo(defaultValue = "1") val speedAvg: Float = 1f,
    @ColumnInfo(defaultValue = "1") val counted: Boolean = true
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
