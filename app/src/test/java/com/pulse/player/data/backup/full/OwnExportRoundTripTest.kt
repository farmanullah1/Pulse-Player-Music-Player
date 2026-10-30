package com.pulse.player.data.backup.full

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class OwnExportRoundTripTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private fun sha256(bytes: ByteArray): String {
        val md = MessageDigest.getInstance("SHA-256")
        return md.digest(bytes).joinToString("") { "%02x".format(it) }
    }

    private fun createZip(file: File, entries: Map<String, ByteArray>) {
        ZipOutputStream(FileOutputStream(file)).use { zos ->
            entries.forEach { (name, bytes) ->
                zos.putNextEntry(ZipEntry(name))
                zos.write(bytes)
                zos.closeEntry()
            }
        }
    }

    @Test
    fun ownExportSnapshotWithManifestPassesValidation() {
        val exportFile = tempFolder.newFile("ManualBackup_20261028_120000000.wav")

        val metaBytes = """
            backupTime=20261028_120000000
            mode=MANUAL_GLOBAL
            type=GLOBAL
            isAutoBackup=false
            deviceId=test-device
            versionName=1.0.0
            versionCode=1
            songs=5
            playlists=2
        """.trimIndent().toByteArray(Charsets.UTF_8)

        val playlistsBytes = """
            [
              {
                "name": "Favorites",
                "tracks": ["/storage/emulated/0/Music/Song1.mp3"]
              }
            ]
        """.trimIndent().toByteArray(Charsets.UTF_8)

        val playbackBytes = """
            [
              {
                "song_id": 100,
                "timestamp": 1700000000000,
                "day": "20261028",
                "play_duration": 180000,
                "play_count": 1,
                "skip_count": 0,
                "_data": "/storage/emulated/0/Music/Song1.mp3"
              }
            ]
        """.trimIndent().toByteArray(Charsets.UTF_8)

        val manifestObj = JSONObject().apply {
            put("format", "pulse-full-1")
            put("createdAt", "2026-10-28T12:00:00Z")
            val arr = JSONArray().apply {
                put(JSONObject().apply {
                    put("name", "metadata.inf")
                    put("size", metaBytes.size.toLong())
                    put("sha256", sha256(metaBytes))
                })
                put(JSONObject().apply {
                    put("name", "Playlists.json")
                    put("size", playlistsBytes.size.toLong())
                    put("sha256", sha256(playlistsBytes))
                })
                put(JSONObject().apply {
                    put("name", "Statistics/PlaybackRecords.json")
                    put("size", playbackBytes.size.toLong())
                    put("sha256", sha256(playbackBytes))
                })
            }
            put("entries", arr)
            put("counts", JSONObject().apply {
                put("favorites", 1)
                put("playlists", 1)
                put("events", 1)
            })
        }
        val manifestBytes = manifestObj.toString(2).toByteArray(Charsets.UTF_8)

        createZip(
            exportFile,
            mapOf(
                "metadata.inf" to metaBytes,
                "Playlists.json" to playlistsBytes,
                "Statistics/PlaybackRecords.json" to playbackBytes,
                "pulse.manifest.json" to manifestBytes
            )
        )

        // Validate the exported backup
        val result = FullBackupValidator.validate(exportFile)
        assertTrue("Expected valid own export, got: $result", result is ValidationResult.Valid)
        val valid = result as ValidationResult.Valid
        assertEquals(ValidationTier.OwnExport, valid.tier)
        assertEquals(1, valid.manifest?.counts?.get("favorites"))
        assertEquals(1, valid.manifest?.counts?.get("playlists"))
        assertEquals(1, valid.manifest?.counts?.get("events"))
    }

    @Test
    fun ownExportWithMismatchedChecksumFailsValidation() {
        val exportFile = tempFolder.newFile("corrupted_backup.wav")

        val metaBytes = "backupTime=20261028_120000000\n".toByteArray(Charsets.UTF_8)
        val playbackBytes = """[{"song_id":1}]""".toByteArray(Charsets.UTF_8)

        // Manifest deliberately has wrong hash for PlaybackRecords.json
        val manifestObj = JSONObject().apply {
            put("format", "pulse-full-1")
            put("createdAt", "2026-10-28T12:00:00Z")
            val arr = JSONArray().apply {
                put(JSONObject().apply {
                    put("name", "metadata.inf")
                    put("size", metaBytes.size.toLong())
                    put("sha256", sha256(metaBytes))
                })
                put(JSONObject().apply {
                    put("name", "Statistics/PlaybackRecords.json")
                    put("size", playbackBytes.size.toLong())
                    put("sha256", "0000000000000000000000000000000000000000000000000000000000000000") // Fake hash
                })
            }
            put("entries", arr)
            put("counts", JSONObject().apply { put("events", 1) })
        }
        val manifestBytes = manifestObj.toString(2).toByteArray(Charsets.UTF_8)

        createZip(
            exportFile,
            mapOf(
                "metadata.inf" to metaBytes,
                "Statistics/PlaybackRecords.json" to playbackBytes,
                "pulse.manifest.json" to manifestBytes
            )
        )

        val result = FullBackupValidator.validate(exportFile)
        assertTrue("Expected Invalid result for checksum mismatch, got: $result", result is ValidationResult.Invalid)
        val invalid = result as ValidationResult.Invalid
        assertTrue(invalid.reason.contains("checksum mismatch") || invalid.reason.contains("manifest"))
    }
}

