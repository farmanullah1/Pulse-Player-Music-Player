package com.pulse.player.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        TrackEntity::class,
        FavoriteEntity::class,
        PlaylistEntity::class,
        PlaylistTrackEntity::class,
        PlayEventEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class PulseDb : RoomDatabase() {
    abstract fun tracks(): TrackDao
    abstract fun favorites(): FavoriteDao
    abstract fun playlists(): PlaylistDao
    abstract fun stats(): StatsDao

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
    }
}
