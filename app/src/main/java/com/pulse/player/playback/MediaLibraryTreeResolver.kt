package com.pulse.player.playback

import android.net.Uri
import android.os.Bundle
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.pulse.player.data.db.FtsQueryBuilder
import com.pulse.player.data.db.PlaylistEntity
import com.pulse.player.data.db.PulseDb
import com.pulse.player.data.db.TrackEntity
import com.pulse.player.data.settings.SettingsStore
import com.pulse.player.library.ArtistParser
import com.pulse.player.data.scan.norm
import kotlinx.coroutines.flow.first
import java.net.URLDecoder
import java.net.URLEncoder

interface MediaBrowseDataSource {
    suspend fun availableTracks(): List<TrackEntity>
    suspend fun trackById(id: String): TrackEntity?
    suspend fun tracksByIds(ids: List<String>): List<TrackEntity>
    suspend fun favoriteTracks(): List<TrackEntity>
    suspend fun recentTrackIds(limit: Int): List<String>
    suspend fun playlists(): List<PlaylistEntity>
    suspend fun playlistTrackIds(playlistId: Long): List<String>
    suspend fun ftsSearchIds(ftsQuery: String): List<String>
    suspend fun splitMultiArtistsEnabled(): Boolean
    suspend fun artistSplitExceptions(): Set<String>
}

class RoomMediaBrowseDataSource(
    private val db: PulseDb,
    private val settings: SettingsStore
) : MediaBrowseDataSource {
    override suspend fun availableTracks(): List<TrackEntity> = db.tracks().availableOnce()
    override suspend fun trackById(id: String): TrackEntity? = db.tracks().byId(id)
    override suspend fun tracksByIds(ids: List<String>): List<TrackEntity> =
        if (ids.isEmpty()) emptyList() else db.tracks().byIds(ids)

    override suspend fun favoriteTracks(): List<TrackEntity> = db.favorites().allTracksOnce()
    override suspend fun recentTrackIds(limit: Int): List<String> = db.stats().observeRecentIds(limit).first()
    override suspend fun playlists(): List<PlaylistEntity> = db.playlists().allPlaylistsOnce()
    override suspend fun playlistTrackIds(playlistId: Long): List<String> = db.playlists().trackIds(playlistId)
    override suspend fun ftsSearchIds(ftsQuery: String): List<String> =
        if (ftsQuery.isBlank()) emptyList() else db.tracks().searchFtsIds(ftsQuery)

    override suspend fun splitMultiArtistsEnabled(): Boolean = settings.splitMultiArtists.first()
    override suspend fun artistSplitExceptions(): Set<String> = settings.artistSplitExceptions.first()
}

class MediaLibraryTreeResolver(private val dataSource: MediaBrowseDataSource) {
    companion object {
        const val ROOT_ID = "pulse_root"
        const val RECENT_ID = "pulse_recent"
        const val FAVORITES_ID = "pulse_favorites"
        const val SONGS_ID = "pulse_songs"
        const val ALBUMS_ID = "pulse_albums"
        const val ARTISTS_ID = "pulse_artists"
        const val PLAYLISTS_ID = "pulse_playlists"

        private const val PREFIX_ALBUM = "album:"
        private const val PREFIX_ARTIST = "artist:"
        private const val PREFIX_PLAYLIST = "playlist:"
        private const val PREFIX_TRACK = "track:"
    }

    fun rootItem(): MediaItem = buildFolderItem(
        mediaId = ROOT_ID,
        title = "Pulse Music",
        mediaType = MediaMetadata.MEDIA_TYPE_FOLDER_MIXED,
        playable = false
    )

    suspend fun getChildren(parentId: String, page: Int = 0, pageSize: Int = 100): List<MediaItem> {
        val raw = when {
            parentId == ROOT_ID -> listOf(
                buildFolderItem(RECENT_ID, "Recently Played", MediaMetadata.MEDIA_TYPE_FOLDER_MIXED),
                buildFolderItem(FAVORITES_ID, "Favourites", MediaMetadata.MEDIA_TYPE_FOLDER_MIXED),
                buildFolderItem(SONGS_ID, "All Songs", MediaMetadata.MEDIA_TYPE_FOLDER_MIXED),
                buildFolderItem(ALBUMS_ID, "Albums", MediaMetadata.MEDIA_TYPE_FOLDER_ALBUMS),
                buildFolderItem(ARTISTS_ID, "Artists", MediaMetadata.MEDIA_TYPE_FOLDER_ARTISTS),
                buildFolderItem(PLAYLISTS_ID, "Playlists", MediaMetadata.MEDIA_TYPE_FOLDER_PLAYLISTS)
            )
            parentId == RECENT_ID -> {
                val ids = dataSource.recentTrackIds(50)
                val map = dataSource.tracksByIds(ids).associateBy { it.id }
                ids.mapNotNull { map[it] }.map(::toPlayableMediaItem)
            }
            parentId == FAVORITES_ID -> {
                dataSource.favoriteTracks().filter { it.available }.map(::toPlayableMediaItem)
            }
            parentId == SONGS_ID -> {
                dataSource.availableTracks()
                    .sortedBy { it.title.lowercase() }
                    .map(::toPlayableMediaItem)
            }
            parentId == ALBUMS_ID -> {
                dataSource.availableTracks()
                    .groupBy { it.album.ifBlank { "Unknown Album" } }
                    .entries
                    .sortedBy { it.key.lowercase() }
                    .map { (albumName, tracks) ->
                        buildFolderItem(
                            mediaId = "$PREFIX_ALBUM${URLEncoder.encode(albumName, "UTF-8")}",
                            title = albumName,
                            subtitle = "${tracks.size} songs · ${tracks.first().artist}",
                            mediaType = MediaMetadata.MEDIA_TYPE_ALBUM,
                            artworkUri = tracks.firstOrNull { it.uri.isNotBlank() }?.uri
                        )
                    }
            }
            parentId == ARTISTS_ID -> {
                val tracks = dataSource.availableTracks()
                val splitEnabled = dataSource.splitMultiArtistsEnabled()
                val exceptions = ArtistParser.DEFAULT_EXCEPTIONS + dataSource.artistSplitExceptions().map { norm(it) }
                val grouped = LinkedHashMap<String, MutableList<TrackEntity>>()
                for (t in tracks) {
                    val names = if (splitEnabled) {
                        ArtistParser.split(t.artist.ifBlank { "Unknown Artist" }, exceptions = exceptions)
                            .ifEmpty { listOf("Unknown Artist") }
                    } else {
                        listOf(t.artist.ifBlank { "Unknown Artist" })
                    }
                    for (name in names) {
                        grouped.getOrPut(name) { mutableListOf() }.add(t)
                    }
                }
                grouped.entries
                    .sortedBy { it.key.lowercase() }
                    .map { (artistName, list) ->
                        buildFolderItem(
                            mediaId = "$PREFIX_ARTIST${URLEncoder.encode(artistName, "UTF-8")}",
                            title = artistName,
                            subtitle = "${list.size} songs",
                            mediaType = MediaMetadata.MEDIA_TYPE_ARTIST,
                            artworkUri = list.firstOrNull { it.uri.isNotBlank() }?.uri
                        )
                    }
            }
            parentId == PLAYLISTS_ID -> {
                dataSource.playlists().map { pl ->
                    buildFolderItem(
                        mediaId = "$PREFIX_PLAYLIST${pl.id}",
                        title = pl.name,
                        subtitle = "Playlist",
                        mediaType = MediaMetadata.MEDIA_TYPE_PLAYLIST
                    )
                }
            }
            parentId.startsWith(PREFIX_ALBUM) -> {
                val albumName = URLDecoder.decode(parentId.removePrefix(PREFIX_ALBUM), "UTF-8")
                dataSource.availableTracks()
                    .filter { it.album.ifBlank { "Unknown Album" }.equals(albumName, ignoreCase = true) }
                    .sortedWith(compareBy<TrackEntity> { it.trackNumber }.thenBy { it.title.lowercase() })
                    .map(::toPlayableMediaItem)
            }
            parentId.startsWith(PREFIX_ARTIST) -> {
                val artistName = URLDecoder.decode(parentId.removePrefix(PREFIX_ARTIST), "UTF-8")
                val splitEnabled = dataSource.splitMultiArtistsEnabled()
                val exceptions = ArtistParser.DEFAULT_EXCEPTIONS + dataSource.artistSplitExceptions().map { norm(it) }
                dataSource.availableTracks()
                    .filter { t ->
                        if (splitEnabled) {
                            ArtistParser.split(t.artist, exceptions = exceptions)
                                .any { it.equals(artistName, ignoreCase = true) }
                        } else {
                            t.artist.equals(artistName, ignoreCase = true)
                        }
                    }
                    .sortedBy { it.title.lowercase() }
                    .map(::toPlayableMediaItem)
            }
            parentId.startsWith(PREFIX_PLAYLIST) -> {
                val playlistId = parentId.removePrefix(PREFIX_PLAYLIST).toLongOrNull() ?: return emptyList()
                val ids = dataSource.playlistTrackIds(playlistId)
                val map = dataSource.tracksByIds(ids).associateBy { it.id }
                ids.mapNotNull { map[it] }.map(::toPlayableMediaItem)
            }
            else -> emptyList()
        }

        if (pageSize <= 0 || page < 0) return raw
        val from = page * pageSize
        if (from >= raw.size) return emptyList()
        val to = (from + pageSize).coerceAtMost(raw.size)
        return raw.subList(from, to)
    }

    suspend fun getItem(mediaId: String): MediaItem? {
        return when {
            mediaId == ROOT_ID -> rootItem()
            mediaId == RECENT_ID -> buildFolderItem(RECENT_ID, "Recently Played", MediaMetadata.MEDIA_TYPE_FOLDER_MIXED)
            mediaId == FAVORITES_ID -> buildFolderItem(FAVORITES_ID, "Favourites", MediaMetadata.MEDIA_TYPE_FOLDER_MIXED)
            mediaId == SONGS_ID -> buildFolderItem(SONGS_ID, "All Songs", MediaMetadata.MEDIA_TYPE_FOLDER_MIXED)
            mediaId == ALBUMS_ID -> buildFolderItem(ALBUMS_ID, "Albums", MediaMetadata.MEDIA_TYPE_FOLDER_ALBUMS)
            mediaId == ARTISTS_ID -> buildFolderItem(ARTISTS_ID, "Artists", MediaMetadata.MEDIA_TYPE_FOLDER_ARTISTS)
            mediaId == PLAYLISTS_ID -> buildFolderItem(PLAYLISTS_ID, "Playlists", MediaMetadata.MEDIA_TYPE_FOLDER_PLAYLISTS)
            mediaId.startsWith(PREFIX_TRACK) -> {
                val realId = mediaId.removePrefix(PREFIX_TRACK)
                dataSource.trackById(realId)?.let(::toPlayableMediaItem)
            }
            else -> dataSource.trackById(mediaId)?.let(::toPlayableMediaItem)
        }
    }

    suspend fun search(query: String, limit: Int = 50): List<MediaItem> {
        val clean = query.trim()
        if (clean.isBlank()) return emptyList()
        val ftsQuery = FtsQueryBuilder.buildPrefixQuery(clean)
        val ftsIds = runCatching { dataSource.ftsSearchIds(ftsQuery) }.getOrDefault(emptyList())
        val tracks = if (ftsIds.isNotEmpty()) {
            val map = dataSource.tracksByIds(ftsIds).associateBy { it.id }
            ftsIds.mapNotNull { map[it] }.filter { it.available }
        } else {
            val lower = clean.lowercase()
            dataSource.availableTracks().filter { t ->
                t.title.lowercase().contains(lower) ||
                    t.artist.lowercase().contains(lower) ||
                    t.album.lowercase().contains(lower) ||
                    t.genre.lowercase().contains(lower)
            }
        }
        return tracks.take(limit).map(::toPlayableMediaItem)
    }

    suspend fun resolvePlayableItems(requestedItems: List<MediaItem>): List<MediaItem> {
        val out = mutableListOf<MediaItem>()
        for (item in requestedItems) {
            val id = item.mediaId
            when {
                item.localConfiguration != null -> out.add(item)
                id.startsWith(PREFIX_ALBUM) ||
                    id.startsWith(PREFIX_ARTIST) ||
                    id.startsWith(PREFIX_PLAYLIST) ||
                    id == RECENT_ID ||
                    id == FAVORITES_ID ||
                    id == SONGS_ID -> {
                    out.addAll(getChildren(id, page = 0, pageSize = 500))
                }
                else -> {
                    val realId = id.removePrefix(PREFIX_TRACK)
                    dataSource.trackById(realId)?.let { out.add(toPlayableMediaItem(it)) }
                }
            }
        }
        return out
    }

    fun toPlayableMediaItem(track: TrackEntity): MediaItem {
        val metadata = MediaMetadata.Builder()
            .setTitle(track.title)
            .setArtist(track.artist)
            .setAlbumTitle(track.album)
            .setGenre(track.genre.takeIf { it.isNotBlank() })
            .setTrackNumber(track.trackNumber.takeIf { it > 0 })
            .setReleaseYear(track.year.takeIf { it > 0 })
            .setIsBrowsable(false)
            .setIsPlayable(true)
            .setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC)
            .setArtworkUri(track.uri.takeIf { it.isNotBlank() }?.let(Uri::parse))
            .build()
        return MediaItem.Builder()
            .setMediaId(track.id)
            .setUri(track.uri)
            .setMediaMetadata(metadata)
            .build()
    }

    private fun buildFolderItem(
        mediaId: String,
        title: String,
        mediaType: Int,
        playable: Boolean = false,
        subtitle: String? = null,
        artworkUri: String? = null
    ): MediaItem {
        val meta = MediaMetadata.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setIsBrowsable(true)
            .setIsPlayable(playable)
            .setMediaType(mediaType)
            .setArtworkUri(artworkUri?.let(Uri::parse))
            .setExtras(Bundle.EMPTY)
            .build()
        return MediaItem.Builder()
            .setMediaId(mediaId)
            .setMediaMetadata(meta)
            .build()
    }
}
