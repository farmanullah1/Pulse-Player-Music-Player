package com.pulse.player.library

import com.pulse.player.data.db.FtsQueryBuilder
import com.pulse.player.data.db.TrackEntity
import com.pulse.player.library.SmartPlaylistEngine.Condition
import com.pulse.player.library.SmartPlaylistEngine.Match
import com.pulse.player.library.SmartPlaylistEngine.Operator
import com.pulse.player.library.SmartPlaylistEngine.SmartField
import com.pulse.player.library.SmartPlaylistEngine.SmartRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class SmartPlaylistAndFtsTest {

    private fun sampleTrack(
        id: String,
        title: String,
        artist: String = "Artist",
        genre: String = "Rock",
        codec: String = "MP3",
        year: Int = 2020,
        durationMs: Long = 210_000L
    ) = TrackEntity(
        id = id,
        uri = "content://media/$id",
        displayName = "$title.${codec.lowercase()}",
        title = title,
        artist = artist,
        album = "Album",
        durationMs = durationMs,
        sizeBytes = 5_000_000L,
        folderPath = "/Music/$artist",
        dateAddedMs = 1_000_000L,
        year = year,
        genre = genre,
        codec = codec,
        normTitle = title.lowercase(),
        normArtist = artist.lowercase(),
        normAlbum = "album"
    )

    @Test
    fun `FtsQueryBuilder tokenizes punctuation and builds prefix query`() {
        assertEquals("", FtsQueryBuilder.buildPrefixQuery("   "))
        assertEquals("pink* floyd*", FtsQueryBuilder.buildPrefixQuery("pink floyd"))
        assertEquals("AC* DC* Live*", FtsQueryBuilder.buildPrefixQuery("AC/DC (Live)!"))
    }

    @Test
    fun `rulesToJson and rulesFromJson preserve match mode and BETWEEN bounds`() {
        val rules = SmartRules(
            match = Match.ANY,
            conditions = listOf(
                Condition(SmartField.CODEC, Operator.CONTAINS, "flac"),
                Condition(SmartField.YEAR, Operator.BETWEEN, "1990", "1999")
            )
        )
        val json = SmartPlaylistEngine.rulesToJson(rules)
        val parsed = SmartPlaylistEngine.rulesFromJson(json)
        assertNotNull(parsed)
        assertEquals(Match.ANY, parsed!!.match)
        assertEquals(2, parsed.conditions.size)
        assertEquals("1990", parsed.conditions[1].value)
        assertEquals("1999", parsed.conditions[1].value2)
    }

    @Test
    fun `evaluateInMemory filters by ALL vs ANY with CODEC, RATING, and PLAY_COUNT`() {
        val library = listOf(
            sampleTrack("1", "Aurora", genre = "Ambient", codec = "FLAC", year = 2022),
            sampleTrack("2", "Subway", genre = "Rock", codec = "MP3", year = 1995),
            sampleTrack("3", "Dawn", genre = "Jazz", codec = "WAV", year = 2024)
        )
        val ratings = mapOf("1" to 5, "2" to 4, "3" to 2)
        val playCounts = mapOf("1" to 8, "2" to 1, "3" to 12)

        val allMatch = SmartPlaylistEngine.evaluateInMemory(
            rules = SmartRules(
                Match.ALL,
                listOf(
                    Condition(SmartField.CODEC, Operator.EQUALS, "flac"),
                    Condition(SmartField.RATING, Operator.GREATER_THAN, "4")
                )
            ),
            tracks = library,
            favoriteIds = setOf("1"),
            playCountMap = playCounts,
            ratingMap = ratings
        )
        assertEquals(listOf("1"), allMatch.map { it.id })

        val anyLosslessOrHighPlay = SmartPlaylistEngine.evaluateInMemory(
            rules = SmartRules(
                Match.ANY,
                listOf(
                    Condition(SmartField.CODEC, Operator.CONTAINS, "wav"),
                    Condition(SmartField.YEAR, Operator.BETWEEN, "1990", "1999")
                )
            ),
            tracks = library,
            playCountMap = playCounts,
            sortKey = "playCount",
            sortOrder = -1,
            limitCount = 1
        )
        assertEquals(listOf("3"), anyLosslessOrHighPlay.map { it.id })
    }
}
