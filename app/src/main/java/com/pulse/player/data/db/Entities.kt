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
        Index("sourceType", "volumeId"),
        Index("genre"),
        Index("language")
    ]
)
data class TrackEntity(
    @PrimaryKey val id: String, // "ms:<mediaStoreId>" or "saf:<sha1>" or "copy:<sha1>"
    val uri: String,
    val displayName: String,
    val title: String,
    val artist: String,
    val album: String,
    val albumArtist: String? = null,
    val durationMs: Long = 0,
    val sizeBytes: Long = 0,
    val mimeType: String? = null,
    val folderPath: String = "",
    val dateAddedMs: Long = 0,
    val dateModifiedMs: Long = 0,
    val trackNumber: Int = 0,
    val year: Int = 0,
    val normTitle: String = "",
    val normArtist: String = "",
    val normAlbum: String = "",
    val available: Boolean = true,
    val dataPath: String? = null,
    val scanGen: Long = 0,
    @ColumnInfo(defaultValue = "'MEDIASTORE'") val sourceType: String = "MEDIASTORE", // MEDIASTORE | SAF | COPIED
    @ColumnInfo(defaultValue = "''") val stableId: String = "", // MediaStore _ID, SAF document id, or copy id
    @ColumnInfo(defaultValue = "'external_primary'") val volumeId: String = "external_primary", // MediaStore VOLUME_NAME, or volume of SAF doc
    val folderUri: String? = null, // SAF tree uri; null for MediaStore

    // --- Schema v4 additions ---
    @ColumnInfo(defaultValue = "''") val genre: String = "",
    @ColumnInfo(defaultValue = "''") val genresRaw: String = "",       // raw multi-value genre string
    @ColumnInfo(defaultValue = "''") val albumArtistNorm: String = "", // normalised albumArtist for grouping
    @ColumnInfo(defaultValue = "0")  val bitrate: Int = 0,             // kbps
    @ColumnInfo(defaultValue = "0")  val sampleRate: Int = 0,          // Hz
    @ColumnInfo(defaultValue = "0")  val bitDepth: Int = 0,            // bits per sample; 0 = unknown
    @ColumnInfo(defaultValue = "''") val codec: String = "",           // e.g. "FLAC", "MP3", "AAC"
    @ColumnInfo(defaultValue = "0")  val replayGainTrackDb: Float = 0f,
    @ColumnInfo(defaultValue = "0")  val replayGainAlbumDb: Float = 0f,
    @ColumnInfo(defaultValue = "0")  val bpm: Float = 0f,
    @ColumnInfo(defaultValue = "''") val language: String = "",
    @ColumnInfo(defaultValue = "0")  val hasEmbeddedLyrics: Boolean = false
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
    val importedAtMs: Long,
    // Schema v4 additions
    @ColumnInfo(defaultValue = "'IMPORT'")  val source: String = "IMPORT",  // IMPORT | EMBEDDED | ONLINE
    @ColumnInfo(defaultValue = "'LRC'")     val format: String = "LRC"       // LRC | TTML | PLAIN | WORD_LRC
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
    @ColumnInfo(defaultValue = "1") val counted: Boolean = true,
    // Schema v4: history import origin tracking (C5)
    @ColumnInfo(defaultValue = "'LIVE'") val origin: String = "LIVE" // LIVE | IMPORT_LASTFM | IMPORT_SPOTIFY | IMPORT_LISTENBRAINZ | IMPORT_YOUTUBE | MANUAL
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

// ─── Schema v4 new tables ────────────────────────────────────────────────────

/** User-defined tags attached to a track (many-to-many). */
@Entity(
    tableName = "track_tags",
    primaryKeys = ["trackId", "tag"],
    indices = [Index("tag")]
)
data class TrackTagEntity(
    val trackId: String,
    val tag: String
)

/** Mood labels attached to a track (many-to-many). */
@Entity(
    tableName = "track_moods",
    primaryKeys = ["trackId", "mood"],
    indices = [Index("mood")]
)
data class TrackMoodEntity(
    val trackId: String,
    val mood: String
)

/** Supplemental per-track metadata editable by the user. */
@Entity(tableName = "track_meta")
data class TrackMetaEntity(
    @PrimaryKey val trackId: String,
    @ColumnInfo(defaultValue = "0")  val rating: Int = 0,    // 0–5 stars
    @ColumnInfo(defaultValue = "''") val language: String = "",
    @ColumnInfo(defaultValue = "''") val note: String = ""
)

/**
 * Per-track audio playback profile (speed, pitch, gain, EQ settings).
 * Loaded by PlaybackEngine when a track starts, applied non-destructively.
 */
@Entity(tableName = "track_audio_profile")
data class TrackAudioProfileEntity(
    @PrimaryKey val trackId: String,
    @ColumnInfo(defaultValue = "1.0") val speed: Float = 1.0f,
    @ColumnInfo(defaultValue = "0")   val pitchSemitones: Float = 0f,
    @ColumnInfo(defaultValue = "0")   val gainDb: Float = 0f,
    @ColumnInfo(defaultValue = "''")  val eqJson: String = "",      // serialized EQ band values
    @ColumnInfo(defaultValue = "''")  val presetId: String = ""     // named preset reference
)

/** Playback position bookmarks (user-created). */
@Entity(
    tableName = "bookmarks",
    indices = [Index("trackId")]
)
data class BookmarkEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val trackId: String,
    val positionMs: Long,
    val label: String,
    val createdAtMs: Long
)

/** Chapter markers embedded in or alongside a track. */
@Entity(
    tableName = "chapters",
    primaryKeys = ["trackId", "startMs"]
)
data class ChapterEntity(
    val trackId: String,
    val startMs: Long,
    val title: String
)

/** Named saved queue sessions (header row). */
@Entity(tableName = "saved_queues")
data class SavedQueueEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAtMs: Long,
    val currentIndex: Int = 0,
    val positionMs: Long = 0L
)

/** Individual track entries within a saved queue session. */
@Entity(
    tableName = "saved_queue_items",
    primaryKeys = ["queueId", "position"],
    indices = [Index("queueId")]
)
data class SavedQueueItemEntity(
    val queueId: Long,
    val position: Int,
    val trackId: String
)

/**
 * Smart playlist definition. `rulesJson` serialises the rule tree using
 * a simple JSON schema defined in SmartPlaylistEngine.
 */
@Entity(tableName = "smart_playlists")
data class SmartPlaylistEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val rulesJson: String,          // SmartPlaylistRules serialised to JSON
    val createdAtMs: Long,
    val sortKey: String? = null,
    val sortOrder: Int? = null,
    val limitCount: Int? = null     // null = no limit
)

/**
 * Cached artist artwork (local file path or remote URL).
 * Source distinguishes embedded, Deezer, local folder, etc.
 */
@Entity(
    tableName = "artist_images",
    indices = [Index("artistNorm")]
)
data class ArtistImageEntity(
    @PrimaryKey val artistNorm: String,
    val url: String = "",
    val filePath: String = "",
    val fetchedAtMs: Long = 0L,
    @ColumnInfo(defaultValue = "'LOCAL'") val source: String = "LOCAL" // LOCAL | DEEZER | EMBEDDED
)

/**
 * Remote music source registry (Subsonic, Jellyfin, WebDAV, SMB, etc.).
 * Credentials are NOT stored here — they go into the Keystore-backed store.
 * `authRef` is an opaque key used to retrieve credentials from the secure store.
 */
@Entity(tableName = "sources")
data class SourceEntity(
    @PrimaryKey val id: String,
    val type: String,           // SUBSONIC | JELLYFIN | WEBDAV | SMB
    val name: String,
    val baseUrl: String,
    val authRef: String = "",   // Keystore entry alias for credentials
    val enabled: Boolean = false,
    val lastSyncMs: Long = 0L
)

/**
 * Offline cache entries for remote tracks (full flavor only).
 * Manages which tracks are locally cached and their priority/recency.
 */
@Entity(
    tableName = "cache_entries",
    indices = [Index("lastUsedMs")]
)
data class CacheEntryEntity(
    @PrimaryKey val trackId: String,
    val filePath: String,
    val bytes: Long,
    val priority: Int = 0,
    val lastUsedMs: Long = 0L
)

/**
 * Pre-computed waveform peak data for a track.
 * `peaksBlobPath` points to a local file containing serialised float peaks.
 * `version` increments when source audio changes and peaks need recomputing.
 */
@Entity(tableName = "waveforms")
data class WaveformEntity(
    @PrimaryKey val trackKey: String, // stable track id
    val peaksBlobPath: String,
    val version: Int = 1
)

// ─── Schema v4 query result helpers ──────────────────────────────────────────

data class GenreStat(
    val genre: String,
    val trackCount: Int,
    val totalMs: Long
)

data class LanguageCount(
    val language: String,
    val cnt: Int
)
