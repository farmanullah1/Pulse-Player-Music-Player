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
        PlayEventEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class PulseDb : RoomDatabase() {
    abstract fun tracks(): TrackDao
    abstract fun scan(): ScanDao
    abstract fun favorites(): FavoriteDao
    abstract fun playlists(): PlaylistDao
    abstract fun stats(): StatsDao
    abstract fun lyrics(): LyricsDao

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
