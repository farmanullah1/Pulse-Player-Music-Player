package com.pulse.player.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import java.io.File

@Database(
    entities = [
        TrackEntity::class,
        ScanSeenEntity::class,
        LyricsEntity::class,
        FavoriteEntity::class,
        PlaylistEntity::class,
        PlaylistTrackEntity::class,
        PlayEventEntity::class,
        // Schema v4 new tables
        TrackTagEntity::class,
        TrackMoodEntity::class,
        TrackMetaEntity::class,
        TrackAudioProfileEntity::class,
        BookmarkEntity::class,
        ChapterEntity::class,
        SavedQueueEntity::class,
        SavedQueueItemEntity::class,
        SmartPlaylistEntity::class,
        ArtistImageEntity::class,
        SourceEntity::class,
        CacheEntryEntity::class,
        WaveformEntity::class
    ],
    version = 4,
    exportSchema = true
)
abstract class PulseDb : RoomDatabase() {
    abstract fun tracks(): TrackDao
    abstract fun scan(): ScanDao
    abstract fun favorites(): FavoriteDao
    abstract fun playlists(): PlaylistDao
    abstract fun stats(): StatsDao
    abstract fun lyrics(): LyricsDao
    // Schema v4 DAOs — added here; implementations below
    abstract fun trackMeta(): TrackMetaDao
    abstract fun trackTags(): TrackTagDao
    abstract fun savedQueues(): SavedQueueDao
    abstract fun smartPlaylists(): SmartPlaylistDao
    abstract fun artistImages(): ArtistImageDao
    abstract fun sources(): SourceDao
    abstract fun waveforms(): WaveformDao
    abstract fun bookmarks(): BookmarkDao

    companion object {

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE tracks ADD COLUMN dataPath TEXT")
                db.execSQL("ALTER TABLE tracks ADD COLUMN scanGen INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE playlists ADD COLUMN sortKey TEXT")
                db.execSQL("ALTER TABLE playlists ADD COLUMN sortOrder INTEGER")
                // Remove duplicate events before creating the unique index
                db.execSQL(
                    "DELETE FROM play_events WHERE id NOT IN " +
                    "(SELECT MIN(id) FROM play_events GROUP BY trackId, playedAtMs)"
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS index_play_events_trackId_playedAtMs " +
                    "ON play_events(trackId, playedAtMs)"
                )
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE tracks ADD COLUMN sourceType TEXT NOT NULL DEFAULT 'MEDIASTORE'")
                db.execSQL("ALTER TABLE tracks ADD COLUMN stableId TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE tracks ADD COLUMN volumeId TEXT NOT NULL DEFAULT 'external_primary'")
                db.execSQL("ALTER TABLE tracks ADD COLUMN folderUri TEXT")
                db.execSQL("UPDATE tracks SET stableId = substr(id, 4) WHERE id LIKE 'ms:%'")
                db.execSQL("UPDATE tracks SET sourceType = 'SAF', stableId = substr(id, 5) WHERE id LIKE 'saf:%'")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_tracks_sourceType_volumeId ON tracks(sourceType, volumeId)")
                db.execSQL("CREATE TABLE IF NOT EXISTS scan_seen (id TEXT NOT NULL, PRIMARY KEY(id))")
                db.execSQL("CREATE TABLE IF NOT EXISTS lyrics (trackId TEXT NOT NULL, text TEXT NOT NULL, synced INTEGER NOT NULL, importedAtMs INTEGER NOT NULL, PRIMARY KEY(trackId))")
                db.execSQL("ALTER TABLE play_events ADD COLUMN zoneId TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE play_events ADD COLUMN monthKey TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE play_events ADD COLUMN playedFraction REAL NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE play_events ADD COLUMN wasSkipped INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE play_events ADD COLUMN speedAvg REAL NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE play_events ADD COLUMN counted INTEGER NOT NULL DEFAULT 1")
                db.execSQL("UPDATE play_events SET monthKey = strftime('%Y%m', playedAtMs/1000, 'unixepoch', 'localtime')")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_play_events_monthKey ON play_events(monthKey)")
            }
        }

        /**
         * Migration 3 → 4: Unified Master Merge schema additions.
         * All new columns use DEFAULT values so existing rows are preserved.
         * No columns are dropped or renamed.
         */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // ── tracks: new metadata columns ──────────────────────────
                db.execSQL("ALTER TABLE tracks ADD COLUMN genre TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE tracks ADD COLUMN genresRaw TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE tracks ADD COLUMN albumArtistNorm TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE tracks ADD COLUMN bitrate INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE tracks ADD COLUMN sampleRate INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE tracks ADD COLUMN bitDepth INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE tracks ADD COLUMN codec TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE tracks ADD COLUMN replayGainTrackDb REAL NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE tracks ADD COLUMN replayGainAlbumDb REAL NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE tracks ADD COLUMN bpm REAL NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE tracks ADD COLUMN language TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE tracks ADD COLUMN hasEmbeddedLyrics INTEGER NOT NULL DEFAULT 0")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_tracks_genre ON tracks(genre)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_tracks_language ON tracks(language)")

                // ── lyrics: source and format columns ─────────────────────
                db.execSQL("ALTER TABLE lyrics ADD COLUMN source TEXT NOT NULL DEFAULT 'IMPORT'")
                db.execSQL("ALTER TABLE lyrics ADD COLUMN format TEXT NOT NULL DEFAULT 'LRC'")

                // ── play_events: origin column (C5 — HistoryWriter gate) ──
                db.execSQL("ALTER TABLE play_events ADD COLUMN origin TEXT NOT NULL DEFAULT 'LIVE'")

                // ── New tables ────────────────────────────────────────────
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS track_tags (" +
                    "trackId TEXT NOT NULL, " +
                    "tag TEXT NOT NULL, " +
                    "PRIMARY KEY(trackId, tag))"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_track_tags_tag ON track_tags(tag)")

                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS track_moods (" +
                    "trackId TEXT NOT NULL, " +
                    "mood TEXT NOT NULL, " +
                    "PRIMARY KEY(trackId, mood))"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_track_moods_mood ON track_moods(mood)")

                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS track_meta (" +
                    "trackId TEXT NOT NULL, " +
                    "rating INTEGER NOT NULL DEFAULT 0, " +
                    "language TEXT NOT NULL DEFAULT '', " +
                    "note TEXT NOT NULL DEFAULT '', " +
                    "PRIMARY KEY(trackId))"
                )

                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS track_audio_profile (" +
                    "trackId TEXT NOT NULL, " +
                    "speed REAL NOT NULL DEFAULT 1.0, " +
                    "pitchSemitones REAL NOT NULL DEFAULT 0, " +
                    "gainDb REAL NOT NULL DEFAULT 0, " +
                    "eqJson TEXT NOT NULL DEFAULT '', " +
                    "presetId TEXT NOT NULL DEFAULT '', " +
                    "PRIMARY KEY(trackId))"
                )

                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS bookmarks (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "trackId TEXT NOT NULL, " +
                    "positionMs INTEGER NOT NULL, " +
                    "label TEXT NOT NULL, " +
                    "createdAtMs INTEGER NOT NULL)"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_bookmarks_trackId ON bookmarks(trackId)")

                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS chapters (" +
                    "trackId TEXT NOT NULL, " +
                    "startMs INTEGER NOT NULL, " +
                    "title TEXT NOT NULL, " +
                    "PRIMARY KEY(trackId, startMs))"
                )

                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS saved_queues (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "name TEXT NOT NULL, " +
                    "createdAtMs INTEGER NOT NULL, " +
                    "currentIndex INTEGER NOT NULL DEFAULT 0, " +
                    "positionMs INTEGER NOT NULL DEFAULT 0)"
                )

                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS saved_queue_items (" +
                    "queueId INTEGER NOT NULL, " +
                    "position INTEGER NOT NULL, " +
                    "trackId TEXT NOT NULL, " +
                    "PRIMARY KEY(queueId, position))"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_saved_queue_items_queueId ON saved_queue_items(queueId)")

                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS smart_playlists (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "name TEXT NOT NULL, " +
                    "rulesJson TEXT NOT NULL, " +
                    "createdAtMs INTEGER NOT NULL, " +
                    "sortKey TEXT, " +
                    "sortOrder INTEGER, " +
                    "limitCount INTEGER)"
                )

                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS artist_images (" +
                    "artistNorm TEXT NOT NULL, " +
                    "url TEXT NOT NULL DEFAULT '', " +
                    "filePath TEXT NOT NULL DEFAULT '', " +
                    "fetchedAtMs INTEGER NOT NULL DEFAULT 0, " +
                    "source TEXT NOT NULL DEFAULT 'LOCAL', " +
                    "PRIMARY KEY(artistNorm))"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_artist_images_artistNorm ON artist_images(artistNorm)")

                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS sources (" +
                    "id TEXT NOT NULL, " +
                    "type TEXT NOT NULL, " +
                    "name TEXT NOT NULL, " +
                    "baseUrl TEXT NOT NULL, " +
                    "authRef TEXT NOT NULL DEFAULT '', " +
                    "enabled INTEGER NOT NULL DEFAULT 0, " +
                    "lastSyncMs INTEGER NOT NULL DEFAULT 0, " +
                    "PRIMARY KEY(id))"
                )

                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS cache_entries (" +
                    "trackId TEXT NOT NULL, " +
                    "filePath TEXT NOT NULL, " +
                    "bytes INTEGER NOT NULL, " +
                    "priority INTEGER NOT NULL DEFAULT 0, " +
                    "lastUsedMs INTEGER NOT NULL DEFAULT 0, " +
                    "PRIMARY KEY(trackId))"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_cache_entries_lastUsedMs ON cache_entries(lastUsedMs)")

                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS waveforms (" +
                    "trackKey TEXT NOT NULL, " +
                    "peaksBlobPath TEXT NOT NULL, " +
                    "version INTEGER NOT NULL DEFAULT 1, " +
                    "PRIMARY KEY(trackKey))"
                )
            }
        }

        fun backupDatabaseBeforeMigration(ctx: Context, oldVersion: Int) {
            try {
                val dbFile = ctx.getDatabasePath("pulse.db")
                if (dbFile != null && dbFile.exists()) {
                    val bak = File(dbFile.parentFile, "pulse.db.bak-v$oldVersion")
                    dbFile.copyTo(bak, overwrite = true)
                    val wal = File(dbFile.parentFile, "pulse.db-wal")
                    if (wal.exists()) wal.copyTo(File(dbFile.parentFile, "pulse.db-wal.bak-v$oldVersion"), overwrite = true)
                    val shm = File(dbFile.parentFile, "pulse.db-shm")
                    if (shm.exists()) shm.copyTo(File(dbFile.parentFile, "pulse.db-shm.bak-v$oldVersion"), overwrite = true)
                }
            } catch (_: Exception) {}
        }
    }
}
