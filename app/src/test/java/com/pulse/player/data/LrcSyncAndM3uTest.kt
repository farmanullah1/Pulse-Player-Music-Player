package com.pulse.player.data

import com.pulse.player.data.lyrics.Lrc
import com.pulse.player.data.lyrics.LrcLine
import com.pulse.player.data.playlists.M3uPlaylistCodec
import com.pulse.player.playback.Track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class LrcSyncAndM3uTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun lrc_formatShiftLeadOffsetAndSerializeRoundTrip() {
        assertEquals("[01:05.34]", Lrc.formatTimestamp(65_340L))

        val original = listOf(
            LrcLine(timeMs = 5_000L, text = "First line"),
            LrcLine(timeMs = 12_500L, text = "Second line"),
            LrcLine(timeMs = -1L, text = "Plain credit")
        )
        val shifted = Lrc.applyPlaybackLeadOffset(original, playbackLeadOffsetMs = 1_500L)
        val serialized = Lrc.serialize(shifted)
        val reparsed = Lrc.parse(serialized)

        assertEquals(2, reparsed.size)
        assertEquals(3_500L, reparsed[0].timeMs)
        assertEquals("First line", reparsed[0].text)
        assertEquals(11_000L, reparsed[1].timeMs)
        assertEquals("Second line", reparsed[1].text)
        assertTrue(serialized.endsWith("Plain credit"))
    }

    @Test
    fun lrc_discoverSidecarFindsMatchingLrcFile() {
        val dir = tempFolder.newFolder("album_dir")
        val audioFile = File(dir, "01 - Sunrise.flac").apply { writeText("audio") }
        val lrcFile = File(dir, "01 - Sunrise.lrc").apply { writeText("[00:01.00]Hello") }

        val discovered = Lrc.discoverSidecarLrc(audioFile.absolutePath)
        assertNotNull(discovered)
        assertEquals(lrcFile.absolutePath, discovered?.absolutePath)
    }

    @Test
    fun m3uCodec_exportParseAndMultiTierResolveMatches() {
        val library = listOf(
            Track(
                id = "1",
                uri = "content://media/1",
                title = "Midnight City",
                artist = "M83",
                album = "Hurry Up",
                durationMs = 243_000L,
                isFavorite = true,
                dataPath = "/storage/emulated/0/Music/M83/01 Midnight City.mp3"
            ),
            Track(
                id = "2",
                uri = "content://media/2",
                title = "Starlight",
                artist = "Muse",
                album = "Black Holes",
                durationMs = 240_000L,
                isFavorite = false,
                dataPath = "/storage/emulated/0/Music/Muse/Starlight.flac"
            )
        )

        val exported = M3uPlaylistCodec.exportM3u8("Night Drive", library)
        assertTrue(exported.startsWith("#EXTM3U"))
        assertTrue(exported.contains("#PLAYLIST:Night Drive"))

        val (parsedName, parsedEntries) = M3uPlaylistCodec.parse(exported, "Fallback")
        assertEquals("Night Drive", parsedName)
        assertEquals(2, parsedEntries.size)

        val directMatch = M3uPlaylistCodec.resolveMatches(parsedName, parsedEntries, library)
        assertEquals(2, directMatch.matchedTracks.size)
        assertTrue(directMatch.unmatchedLocations.isEmpty())

        // Cross-device filename + artist/title fallback + unmatched tracking
        val crossDeviceM3u = """
            #EXTM3U
            #PLAYLIST:Cross Device
            #EXTINF:243,Different Tag - Ignored
            D:\OtherPC\Music\01 Midnight City.mp3
            #EXTINF:240,Muse - Starlight
            /Users/other/Music/renamed_track.ogg
            #EXTINF:180,Ghost Artist - Missing Song
            /nowhere/missing.mp3
        """.trimIndent()
        val (cdName, cdEntries) = M3uPlaylistCodec.parse(crossDeviceM3u)
        val resolved = M3uPlaylistCodec.resolveMatches(cdName, cdEntries, library)
        assertEquals("Cross Device", resolved.playlistName)
        assertEquals(listOf("1", "2"), resolved.matchedTracks.map { it.id })
        assertEquals(listOf("/nowhere/missing.mp3"), resolved.unmatchedLocations)
    }
}
