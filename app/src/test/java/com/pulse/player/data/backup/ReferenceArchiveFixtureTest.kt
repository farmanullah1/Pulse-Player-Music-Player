package com.pulse.player.data.backup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ReferenceArchiveFixtureTest {

    private fun findReferenceArchive(): File {
        val candidates = listOf(
            File("src/main/assets/ManualBackup_20261028_172204175.wav"),
            File("../ManualBackup_20261028_172204175.wav"),
            File("ManualBackup_20261028_172204175.wav"),
            File("/ManualBackup_20261028_172204175.wav")
        )
        return candidates.firstOrNull { it.exists() && it.length() > 0 }
            ?: throw IllegalStateException("Reference archive ManualBackup_20261028_172204175.wav not found")
    }

    @Test
    fun testReferenceArchiveDetectionAndSignature() {
        val ref = findReferenceArchive()
        assertTrue("Reference file should exist", ref.exists())
        assertTrue("Reference file must be non-empty", ref.length() > 1_000_000L)

        // Read leading 12 bytes
        val header = ByteArray(12)
        ref.inputStream().use { it.read(header) }
        val sniffed = sniff(header)
        assertEquals("Reference .wav must be recognized as ZIP container", Sniffed.ZIP, sniffed)
    }

    @Test
    fun testReferenceArchiveEntriesAndMetadata() {
        val ref = findReferenceArchive()
        SafeZip(ref).use { sz ->
            assertTrue("Archive must be recognized as valid backup", sz.isRecognizedBackup())
            assertTrue("Archive must contain metadata.inf", sz.has(Compat.META))
            assertTrue("Archive must contain settings.bak", sz.has(Compat.SETTINGS))
            assertTrue("Archive must contain MusicTags", sz.has(Compat.TAGS))
            assertTrue("Archive must contain PlayLists", sz.has(Compat.PLAYLISTS))
            assertTrue("Archive must contain Sort", sz.has(Compat.SORT))
            assertTrue("Archive must contain Scan", sz.has(Compat.SCAN))
            assertTrue("Archive must contain Lyrics", sz.has(Compat.LYRICS))
            assertTrue("Archive must contain Covers", sz.has(Compat.MUSIC_COVERS))
            assertTrue("Archive must contain Album Covers", sz.has(Compat.ALBUM_COVERS))
            assertTrue("Archive must contain Statistics Config", sz.has(Compat.STATS_CFG))
            assertTrue("Archive must contain Playback records", sz.has(Compat.PLAYBACK))

            // Verify metadata.inf parsed values from the real archive
            val meta = sz.open(Compat.META).use { it.readObject() }
            assertEquals("20261028_172204175", meta.str("backupTime"))
            assertEquals("89d041cac22faf88", meta.str("deviceId"))
            assertEquals("4.4.1.189", meta.str("versionName"))
            assertEquals(189, meta.int("versionCode"))
            assertEquals(13509L, meta.lng("songs"))
            assertEquals(41L, meta.lng("playlists"))

            // Verify settings.bak
            val settings = sz.open(Compat.SETTINGS).use { it.readObject() }
            assertEquals("89d041cac22faf88", settings.str("device"))
            assertEquals("4.4.1.189", settings.str("versionName"))
            assertEquals(true, settings["isBackupMusicTags"])
            assertEquals(true, settings["isBackupPlaylists"])

            // Verify empty lyrics array handled cleanly
            val lyrics = sz.open(Compat.LYRICS).use { it.bufferedReader().readText().trim() }
            assertEquals("[]", lyrics)

            // Verify SortBackupConfig
            val sort = sz.open(Compat.SORT).use { it.readObject() }
            assertNotNull(sort["All_Song"])
            assertNotNull(sort["Album_"])
            assertNotNull(sort["Artist_"])

            // Verify Playlists schema
            var playlistCount = 0
            sz.open(Compat.PLAYLISTS).use { stream ->
                playlistCount = stream.forEachObject { p ->
                    assertNotNull("Playlist must have name", p.str("name"))
                    assertNotNull("Playlist must have musics array", p["musics"])
                }
            }
            assertEquals(41, playlistCount)

            // Verify StatisticsSnapshotConfig
            val statsConfig = sz.open(Compat.STATS_CFG).use { it.readObject() }
            assertEquals(1778269955077L, statsConfig.lng("initApplicationTimestamp"))
            val snapshots = statsConfig["snapshots"] as? List<*>
            assertNotNull(snapshots)
            assertEquals(5, snapshots!!.size)
        }
    }
}
