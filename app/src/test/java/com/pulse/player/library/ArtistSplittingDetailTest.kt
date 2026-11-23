package com.pulse.player.library

import com.pulse.player.playback.Track
import com.pulse.player.ui.viewmodel.EntitySortMode
import com.pulse.player.ui.viewmodel.LibraryBrowserCoordinator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ArtistSplittingDetailTest {

    private fun track(id: String, artist: String, album: String = "Single") = Track(
        id = id,
        uri = "content://media/$id",
        title = "Track $id",
        artist = artist,
        album = album,
        durationMs = 200_000L,
        isFavorite = false
    )

    @Test
    fun `buildArtistItems and tracksForArtist honor split toggle and user exceptions`() {
        val library = listOf(
            track("1", "Daft Punk feat. Pharrell Williams", "RAM"),
            track("2", "Tyler, The Creator", "IGOR"),
            track("3", "M & B", "Duo Tape"),
            track("4", "Daft Punk", "Discovery")
        )

        // With splitting ON and custom exception "M & B"
        val splitItems = LibraryBrowserCoordinator.buildArtistItems(
            list = library,
            sort = EntitySortMode.NAME_ASC,
            splitEnabled = true,
            userExceptions = setOf("M & B")
        )
        val splitNames = splitItems.map { it.name }
        assertTrue("Daft Punk" in splitNames)
        assertTrue("Pharrell Williams" in splitNames)
        assertTrue("Tyler, The Creator" in splitNames) // built-in exception
        assertTrue("M & B" in splitNames) // custom exception
        assertEquals(4, splitItems.size)

        val daftTracks = LibraryBrowserCoordinator.tracksForArtist(
            tracks = library,
            artistName = "Daft Punk",
            splitEnabled = true,
            userExceptions = setOf("M & B")
        )
        assertEquals(listOf("1", "4"), daftTracks.map { it.id })

        val duoTracks = LibraryBrowserCoordinator.tracksForArtist(
            tracks = library,
            artistName = "M & B",
            splitEnabled = true,
            userExceptions = setOf("M & B")
        )
        assertEquals(listOf("3"), duoTracks.map { it.id })

        // With splitting OFF: preserves raw credits without splitting feat.
        val unsplitItems = LibraryBrowserCoordinator.buildArtistItems(
            list = library,
            sort = EntitySortMode.NAME_ASC,
            splitEnabled = false,
            userExceptions = emptySet()
        )
        assertTrue(unsplitItems.any { it.name == "Daft Punk feat. Pharrell Williams" })
        assertEquals(listOf("4"), LibraryBrowserCoordinator.tracksForArtist(library, "Daft Punk", false, emptySet()).map { it.id })
    }
}
