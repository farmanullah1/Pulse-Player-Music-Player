package com.pulse.player.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface TrackDao {
    @Query(
        "SELECT t.*, (f.trackId IS NOT NULL) AS isFavorite " +
        "FROM tracks t LEFT JOIN favorites f ON f.trackId = t.id " +
        "WHERE t.available = 1 AND t.durationMs >= :minMs " +
        "ORDER BY t.title COLLATE NOCASE"
    )
    fun observeAll(minMs: Long): Flow<List<TrackWithFav>>

    @Query("SELECT * FROM tracks WHERE id IN (:ids)")
    suspend fun byIds(ids: List<String>): List<TrackEntity>

    @Query("SELECT * FROM tracks WHERE id = :id")
    suspend fun byId(id: String): TrackEntity?

    @Query("SELECT * FROM tracks")
    suspend fun allOnce(): List<TrackEntity>

    @Query("SELECT COUNT(*) FROM tracks WHERE available = 1")
    fun observeCount(): Flow<Int>

    @Upsert
    suspend fun upsertAll(items: List<TrackEntity>)

    @Query("UPDATE tracks SET available = 0 WHERE id LIKE 'ms:%'")
    suspend fun markMediaStoreUnavailable()

    @Query("UPDATE tracks SET available = 0 WHERE id LIKE 'saf:%'")
    suspend fun markSafUnavailable()
}

@Dao
interface FavoriteDao {
    @Query("SELECT trackId FROM favorites")
    fun observeIds(): Flow<List<String>>

    @Query("SELECT trackId, addedAtMs FROM favorites")
    suspend fun addedAtList(): List<FavAdded>

    @Query(
        "SELECT t.*, 1 AS isFavorite FROM favorites f " +
        "JOIN tracks t ON t.id = f.trackId " +
        "ORDER BY f.addedAtMs DESC"
    )
    fun observeTracks(): Flow<List<TrackWithFav>>

    @Query(
        "SELECT t.* FROM favorites f " +
        "JOIN tracks t ON t.id = f.trackId " +
        "ORDER BY f.addedAtMs DESC"
    )
    suspend fun allTracksOnce(): List<TrackEntity>

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE trackId = :id)")
    suspend fun isFavorite(id: String): Boolean

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun add(f: FavoriteEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addAll(f: List<FavoriteEntity>)

    @Query("DELETE FROM favorites WHERE trackId = :id")
    suspend fun remove(id: String)

    @Query("DELETE FROM favorites")
    suspend fun clear()

    @Transaction
    suspend fun toggle(id: String, now: Long) {
        if (isFavorite(id)) remove(id) else add(FavoriteEntity(id, now))
    }
}

@Dao
interface PlaylistDao {
    @Query("SELECT * FROM playlists ORDER BY createdAtMs")
    fun observePlaylists(): Flow<List<PlaylistEntity>>

    @Query("SELECT COUNT(*) FROM playlist_tracks WHERE playlistId = :id")
    fun observeCount(id: Long): Flow<Int>

    @Query(
        "SELECT t.*, (f.trackId IS NOT NULL) AS isFavorite FROM playlist_tracks p " +
        "JOIN tracks t ON t.id = p.trackId " +
        "LEFT JOIN favorites f ON f.trackId = t.id " +
        "WHERE p.playlistId = :id ORDER BY p.position"
    )
    fun observeTracks(id: Long): Flow<List<TrackWithFav>>

    @Insert
    suspend fun create(p: PlaylistEntity): Long

    @Query("UPDATE playlists SET name = :name WHERE id = :id")
    suspend fun rename(id: Long, name: String)

    @Query("DELETE FROM playlists WHERE id = :id")
    suspend fun deletePlaylist(id: Long)

    @Query("DELETE FROM playlist_tracks WHERE playlistId = :id")
    suspend fun clearTracks(id: Long)

    @Query("SELECT COALESCE(MAX(position), -1) + 1 FROM playlist_tracks WHERE playlistId = :id")
    suspend fun nextPos(id: Long): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addTrack(x: PlaylistTrackEntity)

    @Query("DELETE FROM playlist_tracks WHERE playlistId = :id AND trackId = :trackId")
    suspend fun removeTrack(id: Long, trackId: String)

    @Transaction
    suspend fun delete(id: Long) {
        clearTracks(id)
        deletePlaylist(id)
    }
}

@Dao
interface StatsDao {
    @Insert
    suspend fun insert(e: PlayEventEntity)

    @Query("SELECT COALESCE(SUM(listenedMs), 0) FROM play_events WHERE playedAtMs BETWEEN :from AND :to")
    suspend fun totalMs(from: Long, to: Long): Long

    @Query(
        "SELECT trackId, COUNT(*) AS plays, SUM(listenedMs) AS ms FROM play_events " +
        "WHERE playedAtMs BETWEEN :from AND :to " +
        "GROUP BY trackId ORDER BY plays DESC LIMIT :limit"
    )
    suspend fun topTracks(from: Long, to: Long, limit: Int): List<TrackStat>

    @Query(
        "SELECT t.artist AS artist, COUNT(*) AS plays, SUM(e.listenedMs) AS ms " +
        "FROM play_events e JOIN tracks t ON t.id = e.trackId " +
        "WHERE e.playedAtMs BETWEEN :from AND :to " +
        "GROUP BY t.artist ORDER BY ms DESC LIMIT :limit"
    )
    suspend fun topArtists(from: Long, to: Long, limit: Int): List<ArtistStat>

    @Query("SELECT trackId FROM play_events GROUP BY trackId ORDER BY MAX(playedAtMs) DESC LIMIT :limit")
    fun observeRecentIds(limit: Int): Flow<List<String>>

    @Query("SELECT trackId FROM play_events GROUP BY trackId ORDER BY COUNT(*) DESC LIMIT :limit")
    fun observeTopIds(limit: Int): Flow<List<String>>

    @Query(
        "SELECT COALESCE(SUM(listenedMs), 0) FROM play_events " +
        "WHERE playedAtMs BETWEEN :from AND :to AND (" +
        "CAST(strftime('%H', playedAtMs / 1000, 'unixepoch', 'localtime') AS INTEGER) >= 22 OR " +
        "CAST(strftime('%H', playedAtMs / 1000, 'unixepoch', 'localtime') AS INTEGER) < 5)"
    )
    suspend fun nightMs(from: Long, to: Long): Long
}
