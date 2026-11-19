package com.pulse.player.library

import com.pulse.player.data.scan.ArtworkLoader
import com.pulse.player.data.scan.norm
import com.pulse.player.playback.QueueModel
import com.pulse.player.playback.Track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HighScalePerformanceTest {

    @Test
    fun norm_collapsesWhitespaceAndNormalizesCase() {
        assertEquals("bohemian rhapsody", norm("  Bohemian   \t  Rhapsody \n "))
        assertEquals("cafe", norm("  CAFÉ ".replace("É", "e")))
        assertEquals("", norm(null))
    }

    @Test
    fun searchIndex_searchesLargeLibraryAccuratelyAndFast() {
        val count = 20_000
        val tracks = List(count) { i ->
            val title = if (i == 15_432) "Midnight Echoes" else "Track Number $i"
            val artist = if (i == 15_432) "Synth Horizon" else "Artist ${i % 250}"
            val album = "Album ${i % 100}"
            Track(
                id = "ms:$i",
                uri = "content://media/$i",
                title = title,
                artist = artist,
                album = album,
                durationMs = 180_000L,
                isFavorite = i % 10 == 0,
                normTitle = title.lowercase(),
                normArtist = artist.lowercase(),
                normAlbum = album.lowercase()
            )
        }

        val startNs = System.nanoTime()
        val results = SearchIndex.search(tracks, "Midnight Echoes", maxResults = 50)
        val elapsedMs = (System.nanoTime() - startNs) / 1_000_000L

        assertTrue("Expected target track in search results", results.isNotEmpty())
        assertEquals("ms:15432", results.first().track.id)
        assertTrue("Search over 20,000 tracks should complete well under 2s (took ${elapsedMs}ms)", elapsedMs < 2_000L)
    }

    @Test
    fun queueModel_handles25kTracksShufflePreservingCurrent() {
        val ids = List(25_000) { "ms:$it" }
        val model = QueueModel(ids, "ms:12345")
        model.enableShuffle(99L)

        assertEquals(25_000, model.active.size)
        assertEquals("ms:12345", model.active.first())
        assertEquals(0, model.currentIndex)

        model.disableShuffle()
        assertEquals(25_000, model.active.size)
        assertEquals(12_345, model.currentIndex)
    }

    @Test
    fun artworkLoader_cacheMissChecksAreSafeAndNull() {
        ArtworkLoader.clearCache()
        assertNull(ArtworkLoader.getCached("content://media/unknown", 128))
        assertFalse(ArtworkLoader.hasNoArtwork("content://media/unknown"))
    }
}
