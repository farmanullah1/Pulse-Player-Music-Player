package com.pulse.player.data.backup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream

class ArchiveValidatorTest {

    @Test
    fun testDetectZipSignature() {
        // Standard ZIP magic bytes: PK\x03\x04
        val zipHeader = byteArrayOf(0x50, 0x4B, 0x03, 0x04, 0x14, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00)
        val result = ArchiveValidator.detectSignature(zipHeader)
        assertTrue(result is ArchiveValidator.DetectionResult.ValidZipArchive)
    }

    @Test
    fun testDetectGenuineWavAudio() {
        // Genuine WAV header: RIFF <size> WAVE
        val wavHeader = byteArrayOf(
            'R'.code.toByte(), 'I'.code.toByte(), 'F'.code.toByte(), 'F'.code.toByte(),
            0x24, 0x00, 0x00, 0x00,
            'W'.code.toByte(), 'A'.code.toByte(), 'V'.code.toByte(), 'E'.code.toByte()
        )
        val result = ArchiveValidator.detectSignature(wavHeader)
        assertTrue(result is ArchiveValidator.DetectionResult.GenuineWavAudio)
    }

    @Test
    fun testDetectDisguisedZipNamedWav() {
        // Even if a file was named ManualBackup_20261028_172204175.wav, its content is ZIP
        val zipHeader = byteArrayOf(0x50, 0x4B, 0x03, 0x04, 0x0A, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00)
        val result = ArchiveValidator.detectSignature(zipHeader)
        assertTrue(result is ArchiveValidator.DetectionResult.ValidZipArchive)
    }

    @Test
    fun testDetectUnknownSignature() {
        val randomHeader = byteArrayOf(0x12, 0x34, 0x56, 0x78)
        val result = ArchiveValidator.detectSignature(randomHeader)
        assertTrue(result is ArchiveValidator.DetectionResult.Unknown)
    }

    @Test
    fun testSanitizeEntryNameRejectsZipSlip() {
        assertNull(ArchiveValidator.sanitizeEntryName("../evil.txt"))
        assertNull(ArchiveValidator.sanitizeEntryName("dir/../../evil.txt"))
        assertNull(ArchiveValidator.sanitizeEntryName("/absolute/path.json"))
        assertNull(ArchiveValidator.sanitizeEntryName("C:\\Windows\\System32"))
        assertNull(ArchiveValidator.sanitizeEntryName("foo\u0000bar.txt"))

        assertEquals("metadata/favorites.json", ArchiveValidator.sanitizeEntryName("metadata/favorites.json"))
        assertEquals("manifest.json", ArchiveValidator.sanitizeEntryName("manifest.json"))
    }

    @Test
    fun testSha256Hex() {
        val input = "Hello Pulse Player".toByteArray(Charsets.UTF_8)
        val hash = ArchiveValidator.sha256Hex(input)
        assertEquals(64, hash.length)

        val streamHash = ArchiveValidator.sha256Hex(ByteArrayInputStream(input))
        assertEquals(hash, streamHash)
    }
}
