package com.pulse.player.data.backup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class CompatExportTest {

    @Test
    fun testExportArchiveStructureAndEntryOrder() {
        val tmp = File.createTempFile("test_export_", ".zip")
        try {
            // Write entries in the exact order specified by 10_COMPATIBLE_BACKUP_SPEC.md §1
            ZipOutputStream(BufferedOutputStream(FileOutputStream(tmp))).use { z ->
                fun writeEntry(name: String, content: String) {
                    z.putNextEntry(ZipEntry(name))
                    z.write(content.toByteArray(Charsets.UTF_8))
                    z.closeEntry()
                }

                writeEntry(Compat.SETTINGS, "{\"isBackupMusicTags\": true, \"device\": \"TestDev\", \"versionName\": \"1.0.0\", \"versionCode\": 1}")
                writeEntry(Compat.MUSIC_COVERS, "[{\"_data\": \"/storage/emulated/0/Music/song1.mp3\", \"resource\": null, \"cover_url\": null}]")
                writeEntry(Compat.ALBUM_COVERS, "[{\"album\": \"Album 1\", \"album_art\": null, \"resource\": null, \"modified_album_art\": null}]")
                writeEntry(Compat.TAGS, "[{\"_data\": \"/storage/emulated/0/Music/song1.mp3\", \"title\": \"Song 1\", \"artist\": \"Artist 1\", \"album\": \"Album 1\", \"track\": 1, \"genres\": null}]")
                writeEntry(Compat.LYRICS, "[]")
                writeEntry(Compat.PLAYLISTS, "[{\"name\": \"My Favourite\", \"sort\": {\"key\": \"playlistPlayOrder\", \"order\": -1}, \"cover\": null, \"musics\": [{\"path\": \"/storage/emulated/0/Music/song1.mp3\"}]}]")
                writeEntry(Compat.SORT, "{\"All_Song\": {\"key\": \"dateAdded\", \"order\": -1}}")
                writeEntry(Compat.SCAN, "{\"size\": -1, \"duration\": -1, \"device\": \"TestDev\", \"exclude\": [], \"hidden_songs\": []}")
                writeEntry(Compat.STATS_CFG, "{\"snapshots\": [], \"initApplicationTimestamp\": 1700000000000}")
                writeEntry(Compat.PLAYBACK, "[{\"song_id\": 1, \"timestamp\": 1700000000000, \"day\": \"20231114\", \"looper_count\": 0, \"source_type\": \"\", \"source_id\": \"\", \"play_duration\": 180000, \"play_count\": 1, \"device\": \"TestDev\", \"skip_count\": 0, \"extra_int_1\": 0, \"extra_int_2\": 0, \"extra_int_3\": 0, \"extra_text_1\": null, \"extra_text_2\": null, \"extra_text_3\": null, \"_data\": \"/storage/emulated/0/Music/song1.mp3\"}]")
                writeEntry(Compat.META, "{\"backupTime\": \"20261028_172204175\", \"mode\": \"MANUAL_GLOBAL\", \"type\": \"GLOBAL\", \"isAutoBackup\": false, \"deviceId\": \"TestDev\", \"versionName\": \"1.0.0\", \"versionCode\": 1, \"settings\": {\"isBackupMusicTags\": true, \"isBackupPlaylists\": true}, \"songs\": 1, \"playlists\": 1}")
            }

            // Verify with SafeZip
            SafeZip(tmp).use { sz ->
                assertTrue(sz.isRecognizedBackup())
                assertTrue(sz.has(Compat.SETTINGS))
                assertTrue(sz.has(Compat.TAGS))
                assertTrue(sz.has(Compat.PLAYLISTS))
                assertTrue(sz.has(Compat.SORT))
                assertTrue(sz.has(Compat.SCAN))
                assertTrue(sz.has(Compat.PLAYBACK))
                assertTrue(sz.has(Compat.META))

                // Verify metadata.inf parsing
                val metaObj = sz.open(Compat.META).use { it.readObject() }
                assertEquals("20261028_172204175", metaObj["backupTime"])
                assertEquals("TestDev", metaObj["deviceId"])
                assertEquals(1L, metaObj.lng("songs"))
                assertEquals(1L, metaObj.lng("playlists"))

                // Verify entry order
                val expectedOrder = listOf(
                    Compat.SETTINGS,
                    Compat.MUSIC_COVERS,
                    Compat.ALBUM_COVERS,
                    Compat.TAGS,
                    Compat.LYRICS,
                    Compat.PLAYLISTS,
                    Compat.SORT,
                    Compat.SCAN,
                    Compat.STATS_CFG,
                    Compat.PLAYBACK,
                    Compat.META
                )
                val actualOrder = sz.names.filter { it in expectedOrder }
                assertEquals(expectedOrder, actualOrder)
            }
        } finally {
            tmp.delete()
        }
    }
}

