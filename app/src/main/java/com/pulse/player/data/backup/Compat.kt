package com.pulse.player.data.backup

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.Closeable
import java.io.File
import java.io.FilterInputStream
import java.io.IOException
import java.io.InputStream
import java.util.zip.ZipFile

object Compat {
    const val SETTINGS = "settings.bak"
    const val MUSIC_COVERS = "Covers/MusicCoversBackupConfig.conf"
    const val ALBUM_COVERS = "Covers/AlbumCoversBackupConfig.conf"
    const val TAGS = "MusicTagsBackupConfig.conf"
    const val LYRICS = "Lyrics/LyricsBackupConfig.conf"
    const val PLAYLISTS = "PlayLists/PlayListBackupConfig.conf"
    const val SORT = "SortBackupConfig.conf"
    const val SCAN = "ScanHiddenBackupConfig.conf"
    const val STATS_CFG = "Statistics/StatisticsSnapshotConfig.json"
    const val PLAYBACK = "Statistics/PlaybackRecords.json"
    const val META = "metadata.inf"

    fun snapshot(yyyymm: String) = "Statistics/snapshot_monthly_$yyyymm.db"
    val SNAPSHOT_RE = Regex("^Statistics/snapshot_monthly_(\\d{6})\\.db$")

    val KNOWN = setOf(
        SETTINGS,
        MUSIC_COVERS,
        ALBUM_COVERS,
        TAGS,
        LYRICS,
        PLAYLISTS,
        SORT,
        SCAN,
        STATS_CFG,
        PLAYBACK
    )

    val FLAGS = listOf(
        "isBackupMusicTags",
        "isBackupCovers",
        "isBackupLyrics",
        "isBackupPlaylists",
        "isBackupSortSettings",
        "isBackupScanHideSettings",
        "isBackupMusicRecap"
    )

    const val MAX_FILE = 512L shl 20 // 512 MB
    const val MAX_ENTRY = 256L shl 20 // 256 MB
    const val MAX_TOTAL = 768L shl 20 // 768 MB
    const val MAX_ENTRIES = 64
}

enum class Sniffed { ZIP, REAL_WAV, JSON, OTHER }

fun sniff(bytes: ByteArray): Sniffed {
    fun b(i: Int) = bytes[i].toInt() and 0xFF
    return when {
        bytes.size >= 4 && b(0) == 0x50 && b(1) == 0x4B &&
                ((b(2) == 3 && b(3) == 4) || (b(2) == 5 && b(3) == 6)) -> Sniffed.ZIP
        bytes.size >= 12 && String(bytes, 0, 4, Charsets.ISO_8859_1) == "RIFF" &&
                String(bytes, 8, 4, Charsets.ISO_8859_1) == "WAVE" -> Sniffed.REAL_WAV
        bytes.size >= 1 && (b(0) == '{'.code || b(0) == 0xEF) -> Sniffed.JSON // 0xEF = UTF-8 BOM
        else -> Sniffed.OTHER
    }
}

fun sniff(ctx: Context, uri: Uri): Sniffed {
    val h = ByteArray(12)
    var n = 0
    ctx.contentResolver.openInputStream(uri)?.use { i ->
        while (n < 12) {
            val r = i.read(h, n, 12 - n)
            if (r < 0) break
            n += r
        }
    }
    return sniff(h.copyOf(n))
}

class LimitedStream(inp: InputStream, private val max: Long) : FilterInputStream(inp) {
    private var count = 0L

    private fun add(n: Int) {
        if (n > 0) {
            count += n
            if (count > max) throw IOException("Backup entry is too large.")
        }
    }

    override fun read(): Int = super.read().also { if (it >= 0) add(1) }
    override fun read(b: ByteArray, off: Int, len: Int): Int = super.read(b, off, len).also { add(it) }
}

/** Copies the picked file into a bounded temp file; caller deletes it. */
suspend fun copyToTemp(ctx: Context, uri: Uri): File = withContext(Dispatchers.IO) {
    val f = File.createTempFile("restore_", ".zip", ctx.cacheDir)
    try {
        ctx.contentResolver.openInputStream(uri)!!.use { i ->
            f.outputStream().use { o ->
                val buf = ByteArray(64 * 1024)
                var total = 0L
                while (true) {
                    val n = i.read(buf)
                    if (n < 0) break
                    total += n
                    if (total > Compat.MAX_FILE) throw IOException("The file is too large to be a backup.")
                    o.write(buf, 0, n)
                    ensureActive()
                }
            }
        }
        f
    } catch (e: Throwable) {
        f.delete()
        throw e
    }
}

class SafeZip(val file: File) : Closeable {
    val zip = ZipFile(file)
    val names: Set<String>

    init {
        val seen = HashSet<String>()
        var total = 0L
        var count = 0
        for (e in zip.entries()) {
            if (++count > Compat.MAX_ENTRIES) throw IOException("The archive has too many entries.")
            val n = e.name
            if (n.startsWith("/") || n.contains("\\") || n.contains('\u0000') || n.split('/').any { it == ".." }) {
                throw IOException("The archive contains an unsafe path.")
            }
            if (!seen.add(n)) throw IOException("The archive has duplicate entries.")
            if (!e.isDirectory) {
                if (e.size > Compat.MAX_ENTRY) throw IOException("An entry is too large.")
                total += maxOf(e.size, 0)
            }
        }
        if (total > Compat.MAX_TOTAL) throw IOException("The archive expands to too much data.")
        names = seen
    }

    fun has(name: String): Boolean = zip.getEntry(name) != null

    fun open(name: String): InputStream {
        val entry = zip.getEntry(name) ?: throw IOException("Missing $name")
        return LimitedStream(zip.getInputStream(entry), Compat.MAX_ENTRY)
    }

    fun isRecognizedBackup(): Boolean = has(Compat.META) && names.any { it in Compat.KNOWN }

    override fun close() = zip.close()
}

