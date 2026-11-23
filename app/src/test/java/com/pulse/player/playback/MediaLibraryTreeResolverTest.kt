package com.pulse.player.playback

import com.pulse.player.data.db.PlaylistEntity
import com.pulse.player.data.db.TrackEntity
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaLibraryTreeResolverTest {

    private class FakeDataSource(
        private val tracks: List<TrackEntity>,
        private val favoriteIds: Set<String> = emptySet(),
        private val recentIds: List<String> = emptyList(),
        private val playlistsList: List<PlaylistEntity> = emptyList(),
        private val playlistItems: Map<Long, List<String>> = emptyMap(),
        private val splitEnabled: Boolean = true,
        private val exceptions: Set<String> = emptySet()
    ) : MediaBrowseDataSource {
        override suspend fun availableTracks(): List<TrackEntity> = tracks.filter { it.available }
        override suspend fun trackById(id: String): TrackEntity? = tracks.firstOrNull { it.id == id }
        override suspend fun tracksByIds(ids: List<String>): List<TrackEntity> =
            tracks.filter { it.id in ids }
        override suspend fun favoriteTracks(): List<TrackEntity> =
            tracks.filter { it.id in favoriteIds }
        override suspend fun recentTrackIds(limit: Int): List<String> = recentIds.take(limit)
        override suspend fun playlists(): List<PlaylistEntity> = playlistsList
        override suspend fun playlistTrackIds(playlistId: Long): List<String> =
            playlistItems[playlistId].orEmpty()
        override suspend fun ftsSearchIds(ftsQuery: String): List<String> = emptyList()
        override suspend fun splitMultiArtistsEnabled(): Boolean = splitEnabled
        override suspend fun artistSplitExceptions(): Set<String> = exceptions
    }

    private fun entity(id: String, title: String, artist: String, album: String) = TrackEntity(
        id = id,
        uri = "content://media/$id",
        displayName = "$title.mp3",
        title = title,
        artist = artist,
        album = album,
        trackNumber = 1,
        available = true
    )

    @Test
    fun `browse root artists albums and resolvePlayableItems return consistent items`() = runTest {
        val sampleTracks = listOf(
            entity("t1", "Horizon", "Daft Punk feat. Julian", "RAM"),
            entity("t2", "Sunbeam", "Solar & Wave", "Daybreak"),
            entity("t3", "Nightfall", "Daft Punk", "Discovery")
        )
        val ds = FakeDataSource(
            tracks = sampleTracks,
            favoriteIds = setOf("t2"),
            recentIds = listOf("t3", "t1"),
            playlistsList = listOf(PlaylistEntity(id = 7L, name = "Road Trip", createdAtMs = 100L)),
            playlistItems = mapOf(7L to listOf("t2", "t3")),
            splitEnabled = true,
            exceptions = setOf("Solar & Wave")
        )
        val resolver = MediaLibraryTreeResolver(ds)

        val rootChildren = resolver.getChildren(MediaLibraryTreeResolver.ROOT_ID)
        assertEquals(6, rootChildren.size)

        val artists = resolver.getChildren(MediaLibraryTreeResolver.ARTISTS_ID)
        val artistTitles = artists.map { it.mediaMetadata.title.toString() }
        assertTrue("Daft Punk" in artistTitles)
        assertTrue("Julian" in artistTitles)
        assertTrue("Solar & Wave" in artistTitles)

        val daftNode = artists.first { it.mediaMetadata.title.toString() == "Daft Punk" }
        val daftTracks = resolver.getChildren(daftNode.mediaId)
        assertEquals(listOf("t1", "t3"), daftTracks.map { it.mediaId })

        val resolved = resolver.resolvePlayableItems(listOf(daftNode))
        assertEquals(listOf("t1", "t3"), resolved.map { it.mediaId })

        val paged = resolver.getChildren(MediaLibraryTreeResolver.SONGS_ID, page = 1, pageSize = 2)
        assertEquals(1, paged.size)

        val playlistTracks = resolver.getChildren("playlist:7")
        assertEquals(listOf("t2", "t3"), playlistTracks.map { it.mediaId })

        val found = resolver.search("Horizon")
        assertEquals(listOf("t1"), found.map { it.mediaId })
        assertNotNull(resolver.getItem(MediaLibraryTreeResolver.FAVORITES_ID))
    }
}
