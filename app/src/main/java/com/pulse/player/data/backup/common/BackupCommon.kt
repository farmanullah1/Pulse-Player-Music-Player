package com.pulse.player.data.backup.common

import java.io.FilterInputStream
import java.io.IOException
import java.io.InputStream
import java.security.MessageDigest

enum class BackupKind {
    Classic,
    Full,
    RealWav,
    Unknown
}

object BackupSniffer {
    fun sniff(bytes: ByteArray): BackupKind {
        fun b(i: Int) = bytes[i].toInt() and 0xFF
        if (bytes.size >= 4 && b(0) == 0x50 && b(1) == 0x4B &&
            ((b(2) == 3 && b(3) == 4) || (b(2) == 5 && b(3) == 6))
        ) {
            return BackupKind.Full
        }
        if (bytes.size >= 12 &&
            String(bytes, 0, 4, Charsets.ISO_8859_1) == "RIFF" &&
            String(bytes, 8, 4, Charsets.ISO_8859_1) == "WAVE"
        ) {
            return BackupKind.RealWav
        }
        val text = runCatching {
            val sample = bytes.take(2048).toByteArray()
            String(sample, Charsets.UTF_8).trim()
        }.getOrNull().orEmpty()

        if (text.startsWith("{") && text.contains("pulseplayer.favorites")) {
            return BackupKind.Classic
        }
        return BackupKind.Unknown
    }

    fun sniff(input: InputStream): BackupKind {
        val h = ByteArray(2048)
        var total = 0
        while (total < h.size) {
            val r = input.read(h, total, h.size - total)
            if (r < 0) break
            total += r
        }
        return sniff(h.copyOf(total))
    }
}

class LimitedStream(inp: InputStream, private val max: Long) : FilterInputStream(inp) {
    private var count = 0L

    private fun add(n: Int) {
        if (n > 0) {
            count += n
            if (count > max) throw IOException("Backup entry is too large ($count > $max bytes).")
        }
    }

    override fun read(): Int = super.read().also { if (it >= 0) add(1) }
    override fun read(b: ByteArray, off: Int, len: Int): Int = super.read(b, off, len).also { add(it) }
}

fun sha1(bytes: ByteArray): String {
    val md = MessageDigest.getInstance("SHA-1")
    val digest = md.digest(bytes)
    return digest.joinToString("") { "%02x".format(it) }
}

fun pathKey(path: String): String {
    return path.trim()
        .replace(Regex("^/(storage/emulated/\\d+|storage/[0-9A-Fa-f]{4}-[0-9A-Fa-f]{4}|sdcard|mnt/sdcard)/"), "/")
        .lowercase()
}

