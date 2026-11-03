package com.pulse.player.library

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ArtistParserTest {

    @Test
    fun singleArtistNotSplit() {
        val result = ArtistParser.split("Taylor Swift")
        assertEquals(listOf("Taylor Swift"), result)
    }

    @Test
    fun splitBySemicolon() {
        val result = ArtistParser.split("Artist A; Artist B; Artist C")
        assertEquals(listOf("Artist A", "Artist B", "Artist C"), result)
    }

    @Test
    fun splitByFeat() {
        val result = ArtistParser.split("Main Artist feat. Featured Artist")
        assertEquals(listOf("Main Artist", "Featured Artist"), result)
    }

    @Test
    fun splitByFeatWithDot() {
        val result = ArtistParser.split("DJ Mix ft. Singer")
        assertEquals(listOf("DJ Mix", "Singer"), result)
    }

    @Test
    fun splitByAmpersand() {
        val result = ArtistParser.split("Simon & Garfunkel")
        // Simon & Garfunkel is in the default exceptions list
        assertEquals(listOf("Simon & Garfunkel"), result)
    }

    @Test
    fun exceptionAcDcNotSplit() {
        val result = ArtistParser.split("AC/DC")
        assertEquals(listOf("AC/DC"), result)
    }

    @Test
    fun exceptionTylerNotSplit() {
        val result = ArtistParser.split("Tyler, The Creator")
        assertEquals(listOf("Tyler, The Creator"), result)
    }

    @Test
    fun multipleArtistsWithComma() {
        val result = ArtistParser.split("Artist A, Artist B")
        assertEquals(listOf("Artist A", "Artist B"), result)
    }

    @Test
    fun extractFeaturedFromTitle() {
        val (cleanTitle, featured) = ArtistParser.extractFeatured("Song Name (feat. Guest Artist)")
        assertEquals("Song Name", cleanTitle)
        assertEquals(listOf("Guest Artist"), featured)
    }

    @Test
    fun extractFeaturedWithFt() {
        val (cleanTitle, featured) = ArtistParser.extractFeatured("Track Title ft. Second Artist")
        assertEquals("Track Title", cleanTitle)
        assertEquals(listOf("Second Artist"), featured)
    }

    @Test
    fun noFeaturedReturnsOriginal() {
        val (cleanTitle, featured) = ArtistParser.extractFeatured("Clean Song Title")
        assertEquals("Clean Song Title", cleanTitle)
        assertTrue(featured.isEmpty())
    }

    @Test
    fun splitGenresBySemicolon() {
        val result = ArtistParser.splitGenres("Rock;Pop;Alternative")
        assertEquals(listOf("Rock", "Pop", "Alternative"), result)
    }

    @Test
    fun splitGenresByComma() {
        val result = ArtistParser.splitGenres("Hip-Hop, R&B, Soul")
        assertEquals(listOf("Hip-Hop", "R&B", "Soul"), result)
    }

    @Test
    fun emptyInputReturnsEmpty() {
        assertTrue(ArtistParser.split("").isEmpty())
        assertTrue(ArtistParser.split("  ").isEmpty())
        assertTrue(ArtistParser.splitGenres("").isEmpty())
    }

    @Test
    fun duplicatesRemovedFromSplit() {
        val result = ArtistParser.split("Artist A; Artist A; Artist B")
        assertEquals(2, result.size)
        assertFalse(result.groupBy { it }.any { it.value.size > 1 })
    }
}
