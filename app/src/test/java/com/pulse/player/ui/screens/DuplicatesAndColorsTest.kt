package com.pulse.player.ui.screens

import com.pulse.player.data.db.TrackEntity
import com.pulse.player.ui.theme.Pulse
import com.pulse.player.ui.theme.Skins
import org.junit.Assert.assertEquals
import org.junit.Test

class DuplicatesAndColorsTest {

    private fun track(
        id: String,
        sizeBytes: Long,
        bitrate: Int = 0,
        codec: String = "MP3"
    ) = TrackEntity(
        id = id,
        uri = "content://media/$id",
        displayName = "$id.${codec.lowercase()}",
        title = "Sample Song",
        artist = "Sample Artist",
        album = "Sample Album",
        durationMs = 200_000L,
        sizeBytes = sizeBytes,
        folderPath = "/Music/Test",
        bitrate = bitrate,
        codec = codec
    )

    @Test
    fun `studio_amber is the default first skin with warm amber accent`() {
        val defaultSkin = Skins.first()
        assertEquals("studio_amber", defaultSkin.id)
        assertEquals("Studio Amber", defaultSkin.name)
        assertEquals(Pulse.StudioAmber, defaultSkin.accentDark)
        assertEquals(Pulse.StudioAmberLight, defaultSkin.accentLight)
    }

    @Test
    fun `selectBestMemberId prefers lossless codec then higher bitrate then size`() {
        val mp3Low = track("low", sizeBytes = 4_000_000L, bitrate = 128, codec = "MP3")
        val mp3High = track("high", sizeBytes = 8_000_000L, bitrate = 320, codec = "MP3")
        val flac = track("flac", sizeBytes = 25_000_000L, bitrate = 900, codec = "FLAC")

        assertEquals("high", selectBestMemberId(listOf(mp3Low, mp3High)))
        assertEquals("flac", selectBestMemberId(listOf(mp3Low, mp3High, flac)))
    }

    @Test
    fun `computeDuplicateStats sums copies and reclaimable bytes`() {
        val g1 = DupGroupWithMembers(
            title = "Song A",
            artist = "Artist",
            count = 2,
            members = listOf(
                track("a1", sizeBytes = 10_000_000L),
                track("a2", sizeBytes = 4_000_000L)
            )
        )
        val g2 = DupGroupWithMembers(
            title = "Song B",
            artist = "Artist",
            count = 3,
            members = listOf(
                track("b1", sizeBytes = 8_000_000L),
                track("b2", sizeBytes = 5_000_000L),
                track("b3", sizeBytes = 3_000_000L)
            )
        )

        val (totalCopies, reclaimable) = computeDuplicateStats(listOf(g1, g2))
        assertEquals(5, totalCopies)
        assertEquals(12_000_000L, reclaimable)
    }

    @Test
    fun `formatBytesMb formats megabytes and gigabytes cleanly`() {
        assertEquals("0.0 MB", formatBytesMb(0L))
        assertEquals("5.0 MB", formatBytesMb(5L * 1024L * 1024L))
        assertEquals("1.50 GB", formatBytesMb((1.5 * 1024 * 1024 * 1024).toLong()))
    }
}
