package com.pulse.player.data.backup

import com.pulse.player.data.backup.full.BackupLimits
import com.pulse.player.data.backup.full.validateEntryName
import java.io.InputStream
import java.security.MessageDigest

object ArchiveValidator {

    const val MAX_ENTRY_COUNT = BackupLimits.MAX_ENTRIES
    const val MAX_ENTRY_SIZE_BYTES = BackupLimits.MAX_ENTRY_BYTES
    const val MAX_TOTAL_UNCOMPRESSED_BYTES = BackupLimits.MAX_TOTAL_UNCOMPRESSED

    sealed interface DetectionResult {
        object ValidZipArchive : DetectionResult
        object GenuineWavAudio : DetectionResult
        data class Unknown(val reason: String) : DetectionResult
    }

    /**
     * Inspects the first 16 bytes of the stream or byte array to detect ZIP magic bytes
     * or genuine RIFF WAVE audio header.
     */
    fun detectSignature(headerBytes: ByteArray): DetectionResult {
        if (headerBytes.size < 4) {
            return DetectionResult.Unknown("File header too short (< 4 bytes)")
        }

        // Check for genuine WAV audio: "RIFF" .... "WAVE"
        if (headerBytes.size >= 12 &&
            headerBytes[0] == 'R'.code.toByte() &&
            headerBytes[1] == 'I'.code.toByte() &&
            headerBytes[2] == 'F'.code.toByte() &&
            headerBytes[3] == 'F'.code.toByte() &&
            headerBytes[8] == 'W'.code.toByte() &&
            headerBytes[9] == 'A'.code.toByte() &&
            headerBytes[10] == 'V'.code.toByte() &&
            headerBytes[11] == 'E'.code.toByte()
        ) {
            return DetectionResult.GenuineWavAudio
        }

        // Check for ZIP magic: PK\x03\x04 or PK\x05\x06 (empty) or PK\x07\x08 (spanned)
        if (headerBytes[0] == 0x50.toByte() && headerBytes[1] == 0x4B.toByte()) {
            val b2 = headerBytes[2].toInt() and 0xFF
            val b3 = headerBytes[3].toInt() and 0xFF
            if ((b2 == 0x03 && b3 == 0x04) ||
                (b2 == 0x05 && b3 == 0x06) ||
                (b2 == 0x07 && b3 == 0x08)
            ) {
                return DetectionResult.ValidZipArchive
            }
        }

        return DetectionResult.Unknown("Unrecognized signature (${headerBytes.take(4).map { "%02X".format(it) }.joinToString(" ")})")
    }

    /**
     * Checks if the entry name conforms to security rules.
     */
    fun sanitizeEntryName(name: String): String? {
        if (!validateEntryName(name)) return null
        return name.replace('\\', '/')
    }

    /**
     * Compute SHA-256 hex digest for a byte array.
     */
    fun sha256Hex(bytes: ByteArray): String {
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)
        return digest.joinToString("") { "%02x".format(it) }
    }

    /**
     * Compute SHA-256 hex digest from an InputStream, streaming in chunks.
     */
    fun sha256Hex(stream: InputStream): String {
        val md = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(8192)
        var read: Int
        while (stream.read(buffer).also { read = it } != -1) {
            md.update(buffer, 0, read)
        }
        return md.digest().joinToString("") { "%02x".format(it) }
    }
}
