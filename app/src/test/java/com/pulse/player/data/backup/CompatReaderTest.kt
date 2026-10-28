package com.pulse.player.data.backup

import com.pulse.player.data.db.TrackEntity
import com.pulse.player.data.scan.norm
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class CompatReaderTest {

    @Test
    fun testSniff() {
        val zipHeader = byteArrayOf(0x50, 0x4B, 0x03, 0x04, 0x14, 0x00, 0x00, 0x00)
        assertEquals(Sniffed.ZIP, sniff(zipHeader))

        val emptyZipHeader = byteArrayOf(0x50, 0x4B, 0x05, 0x06, 0x00, 0x00, 0x00, 0x00)
        assertEquals(Sniffed.ZIP, sniff(emptyZipHeader))

        val wavHeader = byteArrayOf(
            'R'.code.toByte(), 'I'.code.toByte(), 'F'.code.toByte(), 'F'.code.toByte(),
            0x24, 0x00, 0x00, 0x00,
            'W'.code.toByte(), 'A'.code.toByte(), 'V'.code.toByte(), 'E'.code.toByte()
        )
        assertEquals(Sniffed.REAL_WAV, sniff(wavHeader))

        val jsonHeader = "{\"version\": 1}".toByteArray(Charsets.UTF_8)
        assertEquals(Sniffed.JSON, sniff(jsonHeader))

        val otherHeader = byteArrayOf(0x12, 0x34, 0x56, 0x78)
        assertEquals(Sniffed.OTHER, sniff(otherHeader))
    }

    @Test
    fun testSafeZipPathValidation() {
        // Test traversal rejection
        assertZipRejected("sub/../secret.txt") { "unsafe path" }
        assertZipRejected("/root/secret.txt") { "unsafe path" }
        assertZipRejected("windows\\style.txt") { "unsafe path" }
        assertZipRejected("nul\u0000byte.txt") { "unsafe path" }
    }

    @Test
    fun testSafeZipDuplicateEntriesRejected() {
        val tmp = File.createTempFile("dup_", ".zip")
        try {
            ZipOutputStream(FileOutputStream(tmp)).use { z ->
                z.putNextEntry(ZipEntry("same.txt"))
                z.write("1".toByteArray())
                z.closeEntry()
                z.putNextEntry(ZipEntry("same.txt"))
                z.write("2".toByteArray())
                z.closeEntry()
            }
            try {
                SafeZip(tmp)
                fail("Expected IOException for duplicate entries")
            } catch (e: IOException) {
                assertTrue(e.message?.contains("duplicate") == true)
            }
        } finally {
            tmp.delete()
        }
    }

    @Test
    fun testSafeZipRecognitionRule() {
        val tmp = File.createTempFile("recognize_", ".zip")
        try {
            ZipOutputStream(FileOutputStream(tmp)).use { z ->
                z.putNextEntry(ZipEntry(Compat.META))
                z.write("{\"songs\":1}".toByteArray())
                z.closeEntry()
                z.putNextEntry(ZipEntry(Compat.TAGS))
                z.write("[]".toByteArray())
                z.closeEntry()
            }
            SafeZip(tmp).use { sz ->
                assertTrue(sz.isRecognizedBackup())
            }
        } finally {
            tmp.delete()
        }

        val nonBackup = File.createTempFile("random_", ".zip")
        try {
            ZipOutputStream(FileOutputStream(nonBackup)).use { z ->
                z.putNextEntry(ZipEntry("hello.txt"))
                z.write("hello".toByteArray())
                z.closeEntry()
            }
            SafeZip(nonBackup).use { sz ->
                assertFalse(sz.isRecognizedBackup())
            }
        } finally {
            nonBackup.delete()
        }
    }

    @Test
    fun testMatcherTiers() {
        val tracks = listOf(
            createTestTrack(
                id = "ms:1",
                dataPath = "/storage/emulated/0/Music/Rock/Queen - Bohemian Rhapsody.mp3",
                title = "Bohemian Rhapsody",
                artist = "Queen",
                album = "A Night at the Opera"
            ),
            createTestTrack(
                id = "ms:2",
                dataPath = "/storage/emulated/0/Music/Pop/Thriller.mp3",
                title = "Thriller",
                artist = "Michael Jackson",
                album = "Thriller"
            ),
            createTestTrack(
                id = "ms:3",
                dataPath = "/storage/emulated/0/Downloads/Audio/DuplicateSong.mp3",
                title = "DuplicateSong",
                artist = "Artist A",
                album = "Album A"
            ),
            createTestTrack(
                id = "ms:4",
                dataPath = "/storage/emulated/0/Podcasts/DuplicateSong.mp3",
                title = "DuplicateSong",
                artist = "Artist B",
                album = "Album B"
            )
        )

        val index = LibraryIndex(tracks)

        // Tier 1: Exact path match
        val res1 = index.resolve("/storage/emulated/0/Music/Rock/Queen - Bohemian Rhapsody.mp3", null)
        assertTrue(res1 is Resolve.Hit)
        assertEquals("ms:1", (res1 as Resolve.Hit).track.id)
        assertEquals(1, res1.tier)

        // Tier 2: Storage-relative match (SD card -> internal storage)
        val res2 = index.resolve("/storage/1234-5678/Music/Rock/Queen - Bohemian Rhapsody.mp3", null)
        assertTrue(res2 is Resolve.Hit)
        assertEquals("ms:1", (res2 as Resolve.Hit).track.id)
        assertEquals(2, res2.tier)

        // Tier 3: Parent folder + file name
        val res3 = index.resolve("/old_phone/external/Music/Rock/Queen - Bohemian Rhapsody.mp3", null)
        assertTrue(res3 is Resolve.Hit)
        assertEquals("ms:1", (res3 as Resolve.Hit).track.id)
        assertEquals(3, res3.tier)

        // Tier 4: Tags match (<unknown> matches Unknown artist)
        val tagTrack = createTestTrack(
            id = "ms:5",
            dataPath = "/storage/emulated/0/Audio/track05.m4a",
            title = "Mystery Song",
            artist = "Unknown artist",
            album = "Unknown album"
        )
        val tagIndex = LibraryIndex(listOf(tagTrack))
        val srcTag = SrcTag(
            path = "/completely/different/path.m4a",
            title = "Mystery Song",
            artist = "<unknown>",
            album = "<unknown>"
        )
        val res4 = tagIndex.resolve("/completely/different/path.m4a", srcTag)
        assertTrue(res4 is Resolve.Hit)
        assertEquals("ms:5", (res4 as Resolve.Hit).track.id)
        assertEquals(4, (res4 as Resolve.Hit).tier)

        // Tier 5: Unique file name only
        val res5 = index.resolve("/somewhere/else/Thriller.mp3", null)
        assertTrue(res5 is Resolve.Hit)
        assertEquals("ms:2", (res5 as Resolve.Hit).track.id)
        assertEquals(5, res5.tier)

        // Ambiguous: multiple files with name DuplicateSong.mp3
        val resAmb = index.resolve("/random/DuplicateSong.mp3", null)
        assertEquals(Resolve.Ambiguous, resAmb)

        // Missing: track not found
        val resMiss = index.resolve("/random/NonExistent.mp3", null)
        assertEquals(Resolve.Missing, resMiss)
    }

    @Test
    fun testStreamingJsonHelpers() {
        val json = """
            [
              {"name": "Playlist 1", "count": 10, "valid": true},
              {"name": "Playlist 2", "count": 25, "valid": false}
            ]
        """.trimIndent()

        val parsed = mutableListOf<Map<String, Any?>>()
        val count = json.byteInputStream().forEachObject { parsed.add(it) }

        assertEquals(2, count)
        assertEquals("Playlist 1", parsed[0].str("name"))
        assertEquals(10, parsed[0].int("count"))
        assertEquals(true, parsed[0]["valid"])

        assertEquals("Playlist 2", parsed[1].str("name"))
        assertEquals(25, parsed[1].int("count"))
        assertEquals(false, parsed[1]["valid"])
    }

    private fun assertZipRejected(entryName: String, msgMatch: () -> String) {
        val tmp = File.createTempFile("unsafe_", ".zip")
        try {
            ZipOutputStream(FileOutputStream(tmp)).use { z ->
                z.putNextEntry(ZipEntry(entryName))
                z.write("test".toByteArray())
                z.closeEntry()
            }
            try {
                SafeZip(tmp)
                fail("Expected IOException for entry $entryName")
            } catch (e: IOException) {
                // Expected
            }
        } finally {
            tmp.delete()
        }
    }

    private fun createTestTrack(
        id: String,
        dataPath: String,
        title: String,
        artist: String,
        album: String
    ): TrackEntity = TrackEntity(
        id = id,
        uri = "content://media/external/audio/media/${id.substringAfter(':')}",
        displayName = dataPath.substringAfterLast('/'),
        title = title,
        artist = artist,
        album = album,
        albumArtist = artist,
        durationMs = 200_000L,
        sizeBytes = 5_000_000L,
        mimeType = "audio/mpeg",
        folderPath = dataPath.substringBeforeLast('/'),
        dateAddedMs = 1700000000000L,
        dateModifiedMs = 1700000000000L,
        trackNumber = 1,
        year = 2024,
        normTitle = norm(title),
        normArtist = norm(artist),
        normAlbum = norm(album),
        available = true,
        dataPath = dataPath,
        scanGen = 1L
    )
}

