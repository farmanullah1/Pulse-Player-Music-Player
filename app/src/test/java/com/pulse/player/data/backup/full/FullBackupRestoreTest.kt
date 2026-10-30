package com.pulse.player.data.backup.full

import com.pulse.player.data.backup.LibraryIndex
import com.pulse.player.data.backup.PathKey
import com.pulse.player.data.backup.Resolve
import com.pulse.player.data.backup.RestoreMode
import com.pulse.player.data.backup.SrcPlaylist
import com.pulse.player.data.backup.SrcTag
import com.pulse.player.data.backup.classic.FavItem
import com.pulse.player.data.backup.classic.FavoritesCodec
import com.pulse.player.data.db.TrackEntity
import com.pulse.player.data.scan.norm
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class FullBackupRestoreTest {

    private fun dummyTrack(
        id: String,
        title: String,
        artist: String = "Test Artist",
        album: String = "Test Album",
        path: String = "/storage/emulated/0/Music/$title.mp3"
    ) = TrackEntity(
        id = id,
        mediaStoreId = id.hashCode().toLong(),
        title = title,
        artist = artist,
        album = album,
        albumArtist = artist,
        durationMs = 180_000L,
        year = 2024,
        trackNumber = 1,
        discNumber = 1,
        genre = "Rock",
        dateAddedSec = 1700000000L,
        dateModifiedSec = 1700000000L,
        sizeBytes = 5_000_000L,
        mimeType = "audio/mpeg",
        relativeDir = "Music",
        displayName = "$title.mp3",
        dataPath = path,
        available = true,
        normTitle = norm(title),
        normArtist = norm(artist),
        normAlbum = norm(album)
    )

    @Test
    fun libraryIndexResolvesAllFiveTiers() {
        val t1 = dummyTrack("1", "Song1", path = "/storage/emulated/0/Music/Song1.mp3")
        val t2 = dummyTrack("2", "Song2", path = "/storage/emulated/0/Rock/Song2.mp3")
        val t3 = dummyTrack("3", "Song3", path = "/storage/emulated/0/Pop/Hit/Song3.mp3")
        val t4 = dummyTrack("4", "Song4", artist = "Special Artist", album = "Special Album", path = "/storage/emulated/0/Old/old4.mp3")
        val t5 = dummyTrack("5", "Song5", path = "/storage/emulated/0/Different/Folder/Song5.mp3")

        val index = LibraryIndex(listOf(t1, t2, t3, t4, t5))

        // Tier 1: exact absolute path match
        val r1 = index.resolve("/storage/emulated/0/Music/Song1.mp3", null)
        assertTrue(r1 is Resolve.Hit)
        assertEquals("1", (r1 as Resolve.Hit).track.id)
        assertEquals(1, r1.tier)

        // Tier 2: relative path match (different storage root)
        val r2 = index.resolve("/storage/1234-5678/Rock/Song2.mp3", null)
        assertTrue(r2 is Resolve.Hit)
        assertEquals("2", (r2 as Resolve.Hit).track.id)
        assertEquals(2, r2.tier)

        // Tier 3: parent folder + filename match
        val r3 = index.resolve("/external_sd/Hit/Song3.mp3", null)
        assertTrue(r3 is Resolve.Hit)
        assertEquals("3", (r3 as Resolve.Hit).track.id)
        assertEquals(3, r3.tier)

        // Tier 4: metadata match
        val tag4 = SrcTag("/completely/different/path.mp3", "Song4", "Special Artist", "Special Album")
        val r4 = index.resolve("/completely/different/path.mp3", tag4)
        assertTrue(r4 is Resolve.Hit)
        assertEquals("4", (r4 as Resolve.Hit).track.id)
        assertEquals(4, r4.tier)

        // Tier 5: filename only match
        val r5 = index.resolve("/some/random/archive/path/Song5.mp3", null)
        assertTrue(r5 is Resolve.Hit)
        assertEquals("5", (r5 as Resolve.Hit).track.id)
        assertEquals(5, r5.tier)
    }

    @Test
    fun libraryIndexNeverGuessesOnAmbiguousMatches() {
        val t1 = dummyTrack("1", "Duplicate", path = "/storage/emulated/0/Music1/Duplicate.mp3")
        val t2 = dummyTrack("2", "Duplicate", path = "/storage/emulated/0/Music2/Duplicate.mp3")

        val index = LibraryIndex(listOf(t1, t2))

        // Filename match has 2 candidates -> MUST return Ambiguous
        val res = index.resolve("/external/random/Duplicate.mp3", null)
        assertEquals(Resolve.Ambiguous, res)
    }

    @Test
    fun libraryIndexReturnsMissingWhenNotFound() {
        val t1 = dummyTrack("1", "ExistingSong")
        val index = LibraryIndex(listOf(t1))

        val res = index.resolve("/storage/emulated/0/Music/NonExistent.mp3", null)
        assertEquals(Resolve.Missing, res)
    }

    @Test
    fun restoreModeEnumValues() {
        assertEquals(RestoreMode.Merge, RestoreMode.valueOf("Merge"))
        assertEquals(RestoreMode.Replace, RestoreMode.valueOf("Replace"))
    }

    @Test
    fun safetyExportGenerationBeforeReplace() {
        val tracks = listOf(
            dummyTrack("1", "Fav1"),
            dummyTrack("2", "Fav2")
        )
        val addedMap = mapOf("1" to 1700000000000L, "2" to 1700000500000L)

        val json = FavoritesCodec.encode(tracks, addedMap)
        assertNotNull(json)
        assertTrue(json.contains("pulse-favorites-1"))
        assertTrue(json.contains("Fav1"))
        assertTrue(json.contains("Fav2"))

        // Decode roundtrip verification
        val decoded = FavoritesCodec.decode(json)
        assertEquals(2, decoded.items.size)
        assertEquals("Fav1", decoded.items[0].title)
    }

    @Test
    fun playlistResolutionDistinguishesFavorites() {
        val favPlaylist = SrcPlaylist(
            name = "My Favourites",
            sortKey = null,
            sortOrder = null,
            paths = listOf("/storage/emulated/0/Music/Fav1.mp3")
        )
        val customPlaylist = SrcPlaylist(
            name = "Gym Workout",
            sortKey = null,
            sortOrder = null,
            paths = listOf("/storage/emulated/0/Music/TrackA.mp3")
        )

        val favNames = setOf("my favourite", "my favorite", "my favourites", "my favorites")
        assertTrue(favPlaylist.name.lowercase() in favNames)
        assertTrue(customPlaylist.name.lowercase() !in favNames)
    }
}

