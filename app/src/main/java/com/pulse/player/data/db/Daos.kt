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
    companion object {
        const val VISIBLE = "t.available = 1 AND t.durationMs >= :minMs AND NOT (t.sourceType = 'COPIED' AND EXISTS (SELECT 1 FROM tracks m WHERE m.sourceType = 'MEDIASTORE' AND m.available = 1 AND m.sizeBytes = t.sizeBytes AND m.normTitle = t.normTitle AND ABS(m.durationMs - t.durationMs) < 2000))"
    }

    @Query(
        "SELECT t.*, (f.trackId IS NOT NULL) AS isFavorite " +
        "FROM tracks t LEFT JOIN favorites f ON f.trackId = t.id " +
        "WHERE t.available = 1 AND t.durationMs >= :minMs AND NOT (t.sourceType = 'COPIED' AND EXISTS (SELECT 1 FROM tracks m WHERE m.sourceType = 'MEDIASTORE' AND m.available = 1 AND m.sizeBytes = t.sizeBytes AND m.normTitle = t.normTitle AND ABS(m.durationMs - t.durationMs) < 2000)) " +
        "ORDER BY t.title COLLATE NOCASE"
    )
    fun observeAll(minMs: Long): Flow<List<TrackWithFav>>

    @Query(
        "SELECT COUNT(*) FROM tracks t " +
        "WHERE t.available = 1 AND t.durationMs >= :minMs AND NOT (t.sourceType = 'COPIED' AND EXISTS (SELECT 1 FROM tracks m WHERE m.sourceType = 'MEDIASTORE' AND m.available = 1 AND m.sizeBytes = t.sizeBytes AND m.normTitle = t.normTitle AND ABS(m.durationMs - t.durationMs) < 2000))"
    )
    fun observeCount(minMs: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM tracks WHERE available = 1")
    fun observeCount(): Flow<Int>

    @Query(
        "SELECT t.*, (f.trackId IS NOT NULL) AS isFavorite FROM tracks t " +
        "LEFT JOIN favorites f ON f.trackId = t.id WHERE t.available = 0 " +
        "ORDER BY t.title COLLATE NOCASE"
    )
    fun observeMissing(): Flow<List<TrackWithFav>>

    @Query("SELECT COUNT(*) FROM tracks WHERE available = 0")
    fun observeMissingCount(): Flow<Int>

    @Query("SELECT * FROM tracks WHERE id IN (:ids)")
    suspend fun byIds(ids: List<String>): List<TrackEntity>

    @Query("SELECT * FROM tracks WHERE id = :id")
    suspend fun byId(id: String): TrackEntity?

    @Query("SELECT * FROM tracks")
    suspend fun allOnce(): List<TrackEntity>

    @Query("SELECT * FROM tracks WHERE available = 1")
    suspend fun availableOnce(): List<TrackEntity>

    @Upsert
    suspend fun upsertAll(items: List<TrackEntity>)

    @Query("SELECT COALESCE(MAX(scanGen), 0) FROM tracks")
    suspend fun maxGen(): Long

    @Query(
        "SELECT t.*, (f.trackId IS NOT NULL) AS isFavorite FROM tracks t " +
        "LEFT JOIN favorites f ON f.trackId = t.id " +
        "WHERE t.available = 1 AND NOT EXISTS (SELECT 1 FROM play_events e WHERE e.trackId = t.id) " +
        "ORDER BY t.dateAddedMs DESC LIMIT 500"
    )
    fun neverPlayed(): Flow<List<TrackWithFav>>

    @Query(
        "SELECT t.*, 1 AS isFavorite FROM favorites f " +
        "JOIN tracks t ON t.id = f.trackId " +
        "WHERE t.available = 1 AND NOT EXISTS (SELECT 1 FROM play_events e WHERE e.trackId = t.id AND e.playedAtMs > :since) " +
        "ORDER BY f.addedAtMs DESC"
    )
    fun forgottenFavorites(since: Long): Flow<List<TrackWithFav>>

    @Query(
        "SELECT t.*, (f.trackId IS NOT NULL) AS isFavorite FROM tracks t " +
        "LEFT JOIN favorites f ON f.trackId = t.id " +
        "WHERE t.available = 1 AND t.durationMs >= 420000 " +
        "ORDER BY t.durationMs DESC LIMIT 500"
    )
    fun longTracks(): Flow<List<TrackWithFav>>

    @Query(
        "SELECT normTitle, normArtist, COUNT(*) AS c FROM tracks " +
        "WHERE available = 1 AND normTitle != '' " +
        "GROUP BY normTitle, normArtist HAVING c > 1 " +
        "ORDER BY c DESC LIMIT 200"
    )
    suspend fun dupGroups(): List<DupGroup>

    @Query(
        "SELECT * FROM tracks " +
        "WHERE available = 1 AND normTitle = :t AND normArtist = :a " +
        "ORDER BY sizeBytes DESC"
    )
    suspend fun dupMembers(t: String, a: String): List<TrackEntity>
}

@Dao
interface ScanDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun seen(ids: List<ScanSeenEntity>)

    @Query("DELETE FROM scan_seen")
    suspend fun clearSeen()

    @Query("UPDATE tracks SET available = 1 WHERE sourceType = 'MEDIASTORE' AND id IN (SELECT id FROM scan_seen)")
    suspend fun markSeenAvailable()

    @Query("UPDATE tracks SET available = 0 WHERE sourceType = 'MEDIASTORE' AND id NOT IN (SELECT id FROM scan_seen)")
    suspend fun markUnseenUnavailable()

    @Query("UPDATE tracks SET available = (id IN (SELECT id FROM scan_seen)) WHERE sourceType = 'SAF' AND folderUri = :tree")
    suspend fun markSafUnseenForTree(tree: String)

    @Query("SELECT COUNT(*) FROM tracks WHERE sourceType = 'MEDIASTORE' AND available = 1")
    suspend fun availableMediaStore(): Int
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

    @Query("SELECT * FROM playlists ORDER BY createdAtMs")
    suspend fun allPlaylistsOnce(): List<PlaylistEntity>

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

    @Query("SELECT * FROM playlists ORDER BY createdAtMs")
    suspend fun allPlaylistsOnce(): List<PlaylistEntity>

    @Query("SELECT * FROM playlist_tracks WHERE playlistId = :id ORDER BY position")
    suspend fun tracksForPlaylist(id: Long): List<PlaylistTrackEntity>

    @Query("DELETE FROM playlists")
    suspend fun clearAllPlaylists()

    @Query("DELETE FROM playlist_tracks")
    suspend fun clearAllPlaylistTracks()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylists(items: List<PlaylistEntity>): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylistTracks(items: List<PlaylistTrackEntity>)

    @Query("SELECT * FROM playlists WHERE name = :name COLLATE NOCASE LIMIT 1")
    suspend fun byName(name: String): PlaylistEntity?

    @Query("UPDATE playlists SET sortKey = :key, sortOrder = :order WHERE id = :id")
    suspend fun setSort(id: Long, key: String?, order: Int?)

    @Query("SELECT trackId FROM playlist_tracks WHERE playlistId = :id ORDER BY position")
    suspend fun trackIds(id: Long): List<String>

    @Transaction
    suspend fun delete(id: Long) {
        clearTracks(id)
        deletePlaylist(id)
    }
}

@Dao
interface StatsDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(e: PlayEventEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(events: List<PlayEventEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertEvents(items: List<PlayEventEntity>): List<Long>

    @Query("SELECT * FROM play_events ORDER BY playedAtMs DESC LIMIT :limit")
    suspend fun getRecentEvents(limit: Int): List<PlayEventEntity>

    @Query("SELECT * FROM play_events WHERE monthKey = :monthKey ORDER BY playedAtMs DESC")
    suspend fun eventsForMonth(monthKey: String): List<PlayEventEntity>

    @Query("SELECT COALESCE(SUM(listenedMs), 0) FROM play_events WHERE monthKey = :monthKey AND counted = 1")
    suspend fun totalMsForMonth(monthKey: String): Long

    @Query("SELECT COUNT(*) FROM play_events WHERE monthKey = :monthKey AND wasSkipped = 1")
    suspend fun skippedCountForMonth(monthKey: String): Int

    @Query(
        "SELECT trackId, COUNT(*) AS plays, SUM(listenedMs) AS ms FROM play_events " +
        "WHERE monthKey = :monthKey AND counted = 1 " +
        "GROUP BY trackId ORDER BY plays DESC LIMIT :limit"
    )
    suspend fun topTracksForMonth(monthKey: String, limit: Int): List<TrackStat>

    @Query(
        "SELECT t.artist AS artist, COUNT(*) AS plays, SUM(e.listenedMs) AS ms " +
        "FROM play_events e JOIN tracks t ON t.id = e.trackId " +
        "WHERE e.monthKey = :monthKey AND e.counted = 1 " +
        "GROUP BY t.artist ORDER BY ms DESC LIMIT :limit"
    )
    suspend fun topArtistsForMonth(monthKey: String, limit: Int): List<ArtistStat>

    @Query("DELETE FROM play_events")
    suspend fun clearAllEvents()

    @Query("DELETE FROM play_events")
    suspend fun clearEvents()

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

    @Query(
        "SELECT e.trackId AS trackId, t.dataPath AS dataPath, e.playedAtMs AS playedAtMs, " +
        "e.listenedMs AS listenedMs, e.completed AS completed " +
        "FROM play_events e JOIN tracks t ON t.id = e.trackId ORDER BY e.playedAtMs"
    )
    suspend fun playbackRows(): List<PlaybackRow>

    @Query("SELECT DISTINCT date(playedAtMs / 1000, 'unixepoch', 'localtime') AS d FROM play_events ORDER BY d DESC LIMIT 400")
    suspend fun playDays(): List<String>
}

@Dao
interface LyricsDao {
    @Query("SELECT * FROM lyrics WHERE trackId = :id")
    fun observe(id: String): Flow<LyricsEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun put(l: LyricsEntity)

    @Query("DELETE FROM lyrics WHERE trackId = :id")
    suspend fun remove(id: String)
}
