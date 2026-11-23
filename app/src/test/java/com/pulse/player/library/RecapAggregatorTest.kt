package com.pulse.player.library

import com.pulse.player.data.db.PlayEventEntity
import com.pulse.player.data.db.TrackEntity
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.ZoneOffset

class RecapAggregatorTest {

    private fun epochMs(year: Int, month: Int, day: Int, hour: Int): Long =
        LocalDateTime.of(year, month, day, hour, 15).toInstant(ZoneOffset.UTC).toEpochMilli()

    private fun entity(id: String, genre: String) = TrackEntity(
        id = id,
        uri = "content://media/$id",
        displayName = "$id.mp3",
        title = "Title $id",
        artist = "Artist",
        album = "Album",
        genre = genre
    )

    @Test
    fun `aggregate computes daily points peak day active days circadian slots and top genres`() {
        val month = YearMonth.of(2025, 3)
        val tracks = mapOf(
            "t1" to entity("t1", "Synthwave; Electronic"),
            "t2" to entity("t2", "Jazz / Lo-Fi")
        )
        val events = listOf(
            PlayEventEntity(trackId = "t1", playedAtMs = epochMs(2025, 3, 2, 8), listenedMs = 120_000L, completed = true, counted = true),   // morning
            PlayEventEntity(trackId = "t1", playedAtMs = epochMs(2025, 3, 5, 14), listenedMs = 300_000L, completed = true, counted = true),  // afternoon
            PlayEventEntity(trackId = "t2", playedAtMs = epochMs(2025, 3, 5, 20), listenedMs = 240_000L, completed = true, counted = true),  // evening
            PlayEventEntity(trackId = "t2", playedAtMs = epochMs(2025, 3, 19, 23), listenedMs = 180_000L, completed = true, counted = true), // night
            // Uncounted skip should be ignored
            PlayEventEntity(trackId = "t1", playedAtMs = epochMs(2025, 3, 19, 10), listenedMs = 600_000L, completed = false, counted = false)
        )

        val result = RecapAggregator.aggregate(month, events, tracks, ZoneOffset.UTC)

        assertEquals(31, result.dailyPoints.size)
        assertEquals(5, result.peakDayOfMonth)
        assertEquals(540_000L, result.peakDayMs)
        assertEquals(3, result.activeDaysCount)

        assertEquals(120_000L, result.circadian.morningMs)
        assertEquals(300_000L, result.circadian.afternoonMs)
        assertEquals(240_000L, result.circadian.eveningMs)
        assertEquals(180_000L, result.circadian.nightMs)
        assertEquals("Afternoon Groove (12PM–5PM)", result.circadian.dominantLabel())

        // Both Synthwave & Electronic appeared in t1 (420_000ms total); Jazz & Lo-Fi also tied at 420_000ms
        assertEquals(4, result.topGenres.size)
        assertEquals(420_000L, result.topGenres.first().totalMs)
    }
}
