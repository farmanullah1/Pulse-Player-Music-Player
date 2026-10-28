package com.pulse.player.data.favorites

import com.pulse.player.data.db.TrackEntity
import com.pulse.player.data.scan.norm
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FavoritesCodecTest {

    @Test
    fun testEncodeAndDecodeLegacyFavorites() {
        val track = TrackEntity(
            id = "ms:42",
            uri = "content://media/external/audio/media/42",
            displayName = "Bohemian_Rhapsody.mp3",
            title = "Bohemian Rhapsody",
            artist = "Queen",
            album = "A Night at the Opera",
            albumArtist = "Queen",
            durationMs = 354_000L,
            sizeBytes = 12_000_000L,
            mimeType = "audio/mpeg",
            folderPath = "/Music/Rock",
            dateAddedMs = 1700000000L,
            dateModifiedMs = 1700000000L,
            trackNumber = 11,
            year = 1975,
            normTitle = norm("Bohemian Rhapsody"),
            normArtist = norm("Queen"),
            normAlbum = norm("A Night at the Opera"),
            available = true
        )

        val addedAtMap = mapOf("ms:42" to 1700000500L)
        val json = FavoritesCodec.encode(listOf(track), addedAtMap)

        assertTrue(json.contains("pulseplayer.favorites"))
        assertTrue(json.contains("Bohemian Rhapsody"))

        val decoded = FavoritesCodec.decode(json)
        assertTrue(decoded is FavoritesCodec.Decoded.Ok)

        val ok = decoded as FavoritesCodec.Decoded.Ok
        assertEquals(1, ok.items.size)
        val item = ok.items.first()
        assertEquals("Bohemian Rhapsody", item.title)
        assertEquals("Queen", item.artist)
        assertEquals(354_000L, item.durationMs)
    }

    @Test
    fun testRejectInvalidJson() {
        val result = FavoritesCodec.decode("Not a valid json string {")
        assertTrue(result is FavoritesCodec.Decoded.Fail)
    }

    @Test
    fun testRejectWrongSchema() {
        val result = FavoritesCodec.decode("""{"schema": "unsupported.schema", "schemaVersion": 1}""")
        assertTrue(result is FavoritesCodec.Decoded.Fail)
    }
}
