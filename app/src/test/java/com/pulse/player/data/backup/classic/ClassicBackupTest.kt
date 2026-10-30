package com.pulse.player.data.backup.classic

import com.pulse.player.data.backup.common.BackupKind
import com.pulse.player.data.backup.common.BackupSniffer
import com.pulse.player.data.db.TrackEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ClassicBackupTest {

    private fun sampleTrack(
        id: String,
        title: String,
        artist: String,
        album: String,
        durationMs: Long,
        displayName: String,
        sizeBytes: Long
    ) = TrackEntity(
        id = id,
        uri = "content://media/external/audio/media/$id",
        title = title,
        artist = artist,
        album = album,
        durationMs = durationMs,
        displayName = displayName,
        sizeBytes = sizeBytes,
        dateAddedMs = 1700000000000L,
        normTitle = title.lowercase(),
        normArtist = artist.lowercase(),
        normAlbum = album.lowercase(),
        available = true
    )

    @Test
    fun codecEncodeDecodeRoundTrip() {
        val tracks = listOf(
            sampleTrack("t1", "Bohemian Rhapsody", "Queen", "A Night at the Opera", 354000L, "queen_bohemian.mp3", 8500000L),
            sampleTrack("t2", "Imagine", "John Lennon", "Imagine", 183000L, "imagine.flac", 15000000L)
        )
        val addedMap = mapOf("t1" to 1700000010000L, "t2" to 1700000020000L)

        val json = FavoritesCodec.encode(tracks, addedMap)
        assertTrue(json.contains("\"schema\": \"pulseplayer.favorites\""))
        assertTrue(json.contains("\"schemaVersion\": 1"))
        assertTrue(json.contains("\"app\": \"Pulse Player\""))

        val decoded = FavoritesCodec.decode(json)
        assertTrue(decoded is FavoritesCodec.Decoded.Ok)
        val ok = decoded as FavoritesCodec.Decoded.Ok

        assertEquals(2, ok.items.size)
        assertEquals("Bohemian Rhapsody", ok.items[0].title)
        assertEquals("Queen", ok.items[0].artist)
        assertEquals(354000L, ok.items[0].durationMs)
        assertEquals(1700000010000L, ok.items[0].addedAtMs)

        assertEquals("Imagine", ok.items[1].title)
        assertEquals(15000000L, ok.items[1].sizeBytes)
        assertEquals(1700000020000L, ok.items[1].addedAtMs)
    }

    @Test
    fun snifferDistinguishesFullAndClassicBackups() {
        // ZIP / Full backup header
        val zipBytes = byteArrayOf(0x50, 0x4B, 0x03, 0x04, 0x00, 0x00)
        assertEquals(BackupKind.Full, BackupSniffer.sniff(zipBytes))

        // Classic Favorites JSON
        val jsonBytes = """{"schema": "pulseplayer.favorites", "schemaVersion": 1, "items": []}""".toByteArray(Charsets.UTF_8)
        assertEquals(BackupKind.Classic, BackupSniffer.sniff(jsonBytes))

        // Real WAV audio file
        val riffWav = "RIFF\u0000\u0000\u0000\u0000WAVEfmt ".toByteArray(Charsets.ISO_8859_1)
        assertEquals(BackupKind.RealWav, BackupSniffer.sniff(riffWav))

        // Random / Unknown
        val randomBytes = byteArrayOf(0x12, 0x34, 0x56, 0x78)
        assertEquals(BackupKind.Unknown, BackupSniffer.sniff(randomBytes))
    }

    @Test
    fun matcherRanksMatchesAccurately() {
        val t1 = sampleTrack("t1", "Stairway to Heaven", "Led Zeppelin", "Led Zeppelin IV", 482000L, "stairway.mp3", 10000000L)
        val t2 = sampleTrack("t2", "Hotel California", "Eagles", "Hotel California", 391000L, "hotel_california.mp3", 9000000L)
        val t3 = sampleTrack("t3", "Comfortably Numb", "Pink Floyd", "The Wall", 382000L, "numb.mp3", 8800000L)

        val library = listOf(t1, t2, t3)
        val favIds = setOf("t2") // Hotel California is already in favorites

        val matcher = FavoritesMatcher(library, favIds)

        val items = listOf(
            // Strong match for t1
            FavItem("Stairway to Heaven", "Led Zeppelin", "Led Zeppelin IV", 482000L, "stairway.mp3", null, 10000000L, 0L, "stairway to heaven", "led zeppelin", "led zeppelin iv"),
            // Already favorite for t2
            FavItem("Hotel California", "Eagles", "Hotel California", 391000L, "hotel_california.mp3", null, 9000000L, 0L, "hotel california", "eagles", "hotel california"),
            // Missing track
            FavItem("Unknown Track", "Unknown Artist", "Unknown Album", 200000L, "unknown.mp3", null, 5000000L, 0L, "unknown track", "unknown artist", "unknown album"),
            // Duplicate of t1
            FavItem("Stairway to Heaven", "Led Zeppelin", "Led Zeppelin IV", 482000L, "stairway.mp3", null, 10000000L, 0L, "stairway to heaven", "led zeppelin", "led zeppelin iv")
        )

        val results = matcher.match(items)
        assertEquals(4, results.size)

        assertTrue(results[0] is MatchResult.Matched)
        assertEquals("t1", (results[0] as MatchResult.Matched).track.id)
        assertEquals("strong", (results[0] as MatchResult.Matched).level)

        assertTrue(results[1] is MatchResult.AlreadyFavorite)
        assertEquals("t2", (results[1] as MatchResult.AlreadyFavorite).track.id)

        assertTrue(results[2] is MatchResult.Missing)

        assertTrue(results[3] is MatchResult.Duplicate)
    }

    @Test
    fun rejectsMalformedOrIncompatibleJson() {
        val fail1 = FavoritesCodec.decode("{ not json }")
        assertTrue(fail1 is FavoritesCodec.Decoded.Fail)

        val fail2 = FavoritesCodec.decode("""{"schema": "wrong.schema", "schemaVersion": 1}""")
        assertTrue(fail2 is FavoritesCodec.Decoded.Fail)

        val fail3 = FavoritesCodec.decode("""{"schema": "pulseplayer.favorites", "schemaVersion": 999}""")
        assertTrue(fail3 is FavoritesCodec.Decoded.Fail)
    }
}

