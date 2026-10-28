package com.pulse.player.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        TrackEntity::class,
        FavoriteEntity::class,
        PlaylistEntity::class,
        PlaylistTrackEntity::class,
        PlayEventEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class PulseDb : RoomDatabase() {
    abstract fun tracks(): TrackDao
    abstract fun favorites(): FavoriteDao
    abstract fun playlists(): PlaylistDao
    abstract fun stats(): StatsDao
}
