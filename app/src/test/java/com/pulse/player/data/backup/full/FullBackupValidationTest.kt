package com.pulse.player.data.backup.full

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class FullBackupValidationTest {

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
    fun entryNameValidationRules() {
        // Valid entry names
        assertTrue(validateEntryName("metadata.inf"))
        assertTrue(validateEntryName("PlaybackRecords.json"))
        assertTrue(validateEntryName("Statistics/PlaybackRecords.json"))
        assertTrue(validateEntryName("Covers/MusicCoversBackupConfig.conf"))

        // Hostile entry names
        assertFalse(validateEntryName(""))
        assertFalse(validateEntryName("../evil.json"))
        assertFalse(validateEntryName("sub/../../evil.json"))
        assertFalse(validateEntryName("/abs/path.json"))
        assertFalse(validateEntryName("\\win\\path.json"))
        assertFalse(validateEntryName("path\\with\\backslash.json"))
        assertFalse(validateEntryName("C:file.json"))
        assertFalse(validateEntryName("file\u0000name.json"))
        assertFalse(validateEntryName("trailing/"))
        assertFalse(validateEntryName("control\u0001char.json"))
        assertFalse(validateEntryName("a".repeat(129))) // > 128 chars
    }

    @Test
    fun referenceArchiveValidationPasses() {
        val zipFile = tempFolder.newFile("reference_backup.wav")
        val meta = "version=1.0\nbackupTime=2026-10-28\n"
        val playback = """[{"song_id":1,"timestamp":1700000000000,"day":"20261028"}]"""

        createZip(
            zipFile,
            mapOf(
                "metadata.inf" to meta.toByteArray(Charsets.UTF_8),
                "Statistics/PlaybackRecords.json" to playback.toByteArray(Charsets.UTF_8)
            )
        )

        val result = FullBackupValidator.validate(zipFile)
        assertTrue(result is ValidationResult.Valid)
        val valid = result as ValidationResult.Valid
        assertEquals(ValidationTier.Reference, valid.tier)
    }

    @Test
    fun ownExportTierValidationWithValidManifest() {
        val zipFile = tempFolder.newFile("own_backup.wav")
        val metaBytes = "version=1.0\nbackupTime=2026-10-28\n".toByteArray(Charsets.UTF_8)
        val playbackBytes = """[{"song_id":1,"timestamp":1700000000000,"day":"20261028"}]""".toByteArray(Charsets.UTF_8)

        val manifest = JSONObject().apply {
            put("format", "pulse-full-1")
            put("createdAt", "2026-10-28T17:22:04Z")
            val entriesArray = JSONArray().apply {
                put(JSONObject().apply {
                    put("name", "metadata.inf")
                    put("size", metaBytes.size.toLong())
                    put("sha256", sha256(metaBytes))
                })
                put(JSONObject().apply {
                    put("name", "Statistics/PlaybackRecords.json")
                    put("size", playbackBytes.size.toLong())
                    put("sha256", sha256(playbackBytes))
                })
            }
            put("entries", entriesArray)
            put("counts", JSONObject().apply { put("events", 1) })
        }.toString()

        createZip(
            zipFile,
            mapOf(
                "metadata.inf" to metaBytes,
                "Statistics/PlaybackRecords.json" to playbackBytes,
                "pulse.manifest.json" to manifest.toByteArray(Charsets.UTF_8)
            )
        )

        val result = FullBackupValidator.validate(zipFile)
        assertTrue(result is ValidationResult.Valid)
        val valid = result as ValidationResult.Valid
        assertEquals(ValidationTier.OwnExport, valid.tier)
        assertEquals(1, valid.manifest?.counts?.get("events"))
    }

    @Test
    fun rejectsTamperedEntryInOwnExport() {
        val zipFile = tempFolder.newFile("tampered_backup.wav")
        val metaBytes = "version=1.0\nbackupTime=2026-10-28\n".toByteArray(Charsets.UTF_8)
        val originalPlayback = """[{"song_id":1,"timestamp":1700000000000,"day":"20261028"}]""".toByteArray(Charsets.UTF_8)
        val tamperedPlayback = """[{"song_id":2,"timestamp":1700000000000,"day":"20261028"}]""".toByteArray(Charsets.UTF_8)

        // Manifest declares hash of originalPlayback, but archive contains tamperedPlayback
        val manifest = JSONObject().apply {
            put("format", "pulse-full-1")
            put("createdAt", "2026-10-28T17:22:04Z")
            val entriesArray = JSONArray().apply {
                put(JSONObject().apply {
                    put("name", "Statistics/PlaybackRecords.json")
                    put("size", originalPlayback.size.toLong())
                    put("sha256", sha256(originalPlayback))
                })
            }
            put("entries", entriesArray)
        }.toString()

        createZip(
            zipFile,
            mapOf(
                "metadata.inf" to metaBytes,
                "Statistics/PlaybackRecords.json" to tamperedPlayback,
                "pulse.manifest.json" to manifest.toByteArray(Charsets.UTF_8)
            )
        )

        val result = FullBackupValidator.validate(zipFile)
        assertTrue(result is ValidationResult.Invalid)
        val invalid = result as ValidationResult.Invalid
        assertTrue(invalid.reason.contains("File was modified or damaged"))
    }

    @Test
    fun rejectsHostileZipSlipArchive() {
        val zipFile = tempFolder.newFile("hostile_slip.wav")
        createZip(
            zipFile,
            mapOf(
                "metadata.inf" to "version=1.0\n".toByteArray(),
                "../evil.json" to "{}".toByteArray()
            )
        )

        val result = FullBackupValidator.validate(zipFile)
        assertTrue(result is ValidationResult.Invalid)
        val invalid = result as ValidationResult.Invalid
        assertTrue(invalid.reason.contains("Invalid entry name"))
    }

    @Test
    fun rejectsArchiveWithTooManyEntries() {
        val zipFile = tempFolder.newFile("hostile_too_many.wav")
        // Maximum entries is 64. Create 65 entries.
        val entries = (1..65).associate { i ->
            "entry_$i.txt" to "data".toByteArray()
        }
        createZip(zipFile, entries)

        val result = FullBackupValidator.validate(zipFile)
        assertTrue(result is ValidationResult.Invalid)
        val invalid = result as ValidationResult.Invalid
        assertTrue(invalid.reason.contains("too many entries"))
    }

    @Test
    fun rejectsArchiveMissingMetadataInf() {
        val zipFile = tempFolder.newFile("no_meta.wav")
        createZip(
            zipFile,
            mapOf(
                "PlaybackRecords.json" to "[]".toByteArray()
            )
        )

        val result = FullBackupValidator.validate(zipFile)
        assertTrue(result is ValidationResult.Invalid)
        val invalid = result as ValidationResult.Invalid
        assertTrue(invalid.reason.contains("missing metadata.inf"))
    }

    @Test
    fun rejectsNonZipArchive() {
        val fakeWav = tempFolder.newFile("fake.wav")
        fakeWav.writeBytes("RIFF\u0000\u0000\u0000\u0000WAVEfmt ".toByteArray(Charsets.ISO_8859_1))

        val result = FullBackupValidator.validate(fakeWav)
        assertTrue(result is ValidationResult.Invalid)
        val invalid = result as ValidationResult.Invalid
        assertTrue(invalid.reason.contains("missing PK\\x03\\x04 signature"))
    }

    @Test
    fun snapshotHeaderCheck() {
        val validSqliteHeader = "SQLite format 3\u0000".toByteArray(Charsets.US_ASCII)
        assertTrue(checkSnapshotStream(validSqliteHeader.inputStream(), 100L))

        val invalidSqliteHeader = "Not SQLite format".toByteArray(Charsets.US_ASCII)
        assertFalse(checkSnapshotStream(invalidSqliteHeader.inputStream(), 100L))

        // Too small (< 16 bytes)
        assertFalse(checkSnapshotStream("short".toByteArray().inputStream(), 5L))
    }
}

