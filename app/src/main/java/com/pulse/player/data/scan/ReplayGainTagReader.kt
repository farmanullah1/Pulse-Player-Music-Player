package com.pulse.player.data.scan

import android.content.Context
import android.net.Uri
import com.pulse.player.playback.ReplayGainCalculator
import java.io.File
import java.io.InputStream

data class ReplayGainTags(
    val trackGainDb: Float = 0f,
    val albumGainDb: Float = 0f
)

/**
 * Lightweight, allocation-conscious header parser for ReplayGain tags across
 * MP3 (ID3v2 TXXX, including UTF-16), FLAC/OGG/Opus (Vorbis Comment), and M4A (iTunes freeform).
 * Only reads the first [MAX_HEADER_BYTES] so incremental library scans stay fast.
 */
object ReplayGainTagReader {
    private const val MAX_HEADER_BYTES = 96 * 1024
    private val TRACK_KEYS = listOf("REPLAYGAIN_TRACK_GAIN", "replaygain_track_gain")
    private val ALBUM_KEYS = listOf("REPLAYGAIN_ALBUM_GAIN", "replaygain_album_gain")

    fun readFromUriOrPath(ctx: Context, uri: Uri, dataPath: String?): ReplayGainTags {
        val bytes = runCatching {
            if (!dataPath.isNullOrBlank()) {
                val f = File(dataPath)
                if (f.exists() && f.canRead()) {
                    return@runCatching f.inputStream().use { readHead(it) }
                }
            }
            ctx.contentResolver.openInputStream(uri)?.use { readHead(it) }
        }.getOrNull() ?: return ReplayGainTags()
        return parseFromBytes(bytes)
    }

    private fun readHead(input: InputStream): ByteArray {
        val buf = ByteArray(MAX_HEADER_BYTES)
        var offset = 0
        while (offset < buf.size) {
            val read = input.read(buf, offset, buf.size - offset)
            if (read <= 0) break
            offset += read
        }
        return if (offset == buf.size) buf else buf.copyOf(offset)
    }

    fun parseFromBytes(bytes: ByteArray): ReplayGainTags {
        if (bytes.isEmpty()) return ReplayGainTags()
        // Fast pre-check for ASCII 'gain' (covers both ASCII and UTF-16LE/BE interleaved bytes roughly;
        // normalize to clean ASCII window for resilient tag matching across ID3 UTF-16 / Vorbis / MP4).
        val trackDb = findGainForKey(bytes, TRACK_KEYS) ?: 0f
        val albumDb = findGainForKey(bytes, ALBUM_KEYS) ?: 0f
        return ReplayGainTags(trackGainDb = trackDb, albumGainDb = albumDb)
    }

    private fun findGainForKey(bytes: ByteArray, keys: List<String>): Float? {
        // 1. Direct ASCII / UTF-8 match (Vorbis comment, ID3v2 ISO-8859-1/UTF-8, MP4 mean/name)
        for (key in keys) {
            val pattern = key.toByteArray(Charsets.US_ASCII)
            val idx = indexOfBytes(bytes, pattern, ignoreAsciiCase = true)
            if (idx >= 0) {
                extractValueWindow(bytes, idx + pattern.size)?.let { return it }
            }
        }
        // 2. UTF-16LE (common in ID3v2 TXXX frames written by foobar2000 / MusicBrainz Picard / Mp3tag)
        val upperKey = keys.first()
        val utf16Le = upperKey.toByteArray(Charsets.UTF_16LE)
        val idxLe = indexOfBytes(bytes, utf16Le, ignoreAsciiCase = false)
        if (idxLe >= 0) {
            extractValueWindow(bytes, idxLe + utf16Le.size, utf16 = true)?.let { return it }
        }
        return null
    }

    private fun extractValueWindow(bytes: ByteArray, start: Int, utf16: Boolean = false): Float? {
        if (start >= bytes.size) return null
        val limit = (start + 96).coerceAtMost(bytes.size)
        val sb = StringBuilder(32)
        var i = start
        var seenContent = false
        while (i < limit) {
            val b = bytes[i].toInt() and 0xFF
            if (b == 0) {
                i++
                // Null separates key and value in ID3v2 TXXX; skip leading nulls before value starts
                if (!seenContent) continue
                // In UTF-16, a single 0x00 follows each ASCII char; two consecutive 0x00 end the string
                if (utf16 && i < limit && (bytes[i].toInt() and 0xFF) != 0) continue
                break
            }
            if (b == '='.code || b == ':'.code || b == ' '.code) {
                if (!seenContent) {
                    i++
                    continue
                }
            }
            val ch = b.toChar()
            if (ch.isDigit() || ch == '+' || ch == '-' || ch == '.' || ch == ',' ||
                ch == 'd' || ch == 'D' || ch == 'b' || ch == 'B' || ch == ' '
            ) {
                seenContent = true
                sb.append(ch)
            } else if (seenContent) {
                break
            } else if (i - start > 36) {
                // Skip past MP4 atom header bytes ('data' + 8 bytes) if searching inside MP4 freeform atom
                if (sb.isEmpty() && ch.isLetter()) {
                    // reset if scanning atom prefix
                }
            }
            i++
        }
        val raw = sb.toString().trim()
        return if (raw.isEmpty()) null else ReplayGainCalculator.parseGainDb(raw)
    }

    private fun indexOfBytes(haystack: ByteArray, needle: ByteArray, ignoreAsciiCase: Boolean): Int {
        if (needle.isEmpty() || haystack.size < needle.size) return -1
        val maxStart = haystack.size - needle.size
        for (i in 0..maxStart) {
            var match = true
            for (j in needle.indices) {
                val a = haystack[i + j]
                val b = needle[j]
                if (a != b) {
                    if (!ignoreAsciiCase || toAsciiLower(a) != toAsciiLower(b)) {
                        match = false
                        break
                    }
                }
            }
            if (match) return i
        }
        return -1
    }

    private fun toAsciiLower(b: Byte): Int {
        val v = b.toInt() and 0xFF
        return if (v in 'A'.code..'Z'.code) v + 32 else v
    }
}
