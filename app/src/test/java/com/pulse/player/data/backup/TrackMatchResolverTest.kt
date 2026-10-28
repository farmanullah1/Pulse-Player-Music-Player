package com.pulse.player.data.backup

import com.pulse.player.data.db.TrackEntity
import com.pulse.player.data.scan.norm
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TrackMatchResolverTest {

    private val track1 = TrackEntity(
        id = "ms:1001",
        uri = "content://media/external/audio/media/1001",
        displayName = "Starlight.mp3",
        title = "Starlight",
        artist = "Muse",
        album = "Black Holes and Revelations",
        albumArtist = "Muse",
        durationMs = 240_000L,
        sizeBytes = 8_000_000L,
        mimeType = "audio/mpeg",
        folderPath = "/Music",
        dateAddedMs = 1000L,
        dateModifiedMs = 1000L,
        trackNumber = 2,
        year = 2006,
        normTitle = norm("Starlight"),
        normArtist = norm("Muse"),
        normAlbum = norm("Black Holes and Revelations"),
        available = true
    )

    private val track2 = TrackEntity(
        id = "ms:1002",
        uri = "content://media/external/audio/media/1002",
        displayName = "pulse_horizon.wav",
        title = "Pulse Horizon",
        artist = "Pulse Studio",
        album = "Neon Drift",
        albumArtist = "Pulse Studio",
        durationMs = 15_000L,
        sizeBytes = 2_646_044L,
        mimeType = "audio/x-wav",
        folderPath = "/Music",
        dateAddedMs = 2000L,
        dateModifiedMs = 2000L,
        trackNumber = 1,
        year = 2026,
        normTitle = norm("Pulse Horizon"),
        normArtist = norm("Pulse Studio"),
        normAlbum = norm("Neon Drift"),
        available = true
    )

    private val resolver = TrackMatchResolver(listOf(track1, track2))

    @Test
    fun testExactIdMatch() {
        val query = BackupTrackRef(
            id = "ms:1001",
            title = "Different Title"
        )
        val match = resolver.resolve(query)
        assertTrue(match is TrackMatch.Matched)
        assertEquals(track1.id, (match as TrackMatch.Matched).target.id)
        assertEquals(TrackMatch.Confidence.EXACT_ID, match.confidence)
    }

    @Test
    fun testMetadataExactMatchWithDurationTolerance() {
        val query = BackupTrackRef(
            id = "legacy_id_9999",
            title = "Starlight",
            artist = "Muse",
            durationMs = 241_000L // 1 second difference (within 2500ms)
        )
        val match = resolver.resolve(query)
        assertTrue(match is TrackMatch.Matched)
        assertEquals(track1.id, (match as TrackMatch.Matched).target.id)
        assertEquals(TrackMatch.Confidence.METADATA_EXACT, match.confidence)
    }

    @Test
    fun testDisplayNameAndSizeMatch() {
        val query = BackupTrackRef(
            title = "Unknown",
            displayName = "pulse_horizon.wav",
            sizeBytes = 2_646_044L
        )
        val match = resolver.resolve(query)
        assertTrue(match is TrackMatch.Matched)
        assertEquals(track2.id, (match as TrackMatch.Matched).target.id)
        assertEquals(TrackMatch.Confidence.FILENAME_SIZE, match.confidence)
    }

    @Test
    fun testUnmatchedTrack() {
        val query = BackupTrackRef(
            title = "Non Existent Track",
            artist = "Ghost Artist",
            durationMs = 999_999L
        )
        val match = resolver.resolve(query)
        assertTrue(match is TrackMatch.Unmatched)
    }
}
