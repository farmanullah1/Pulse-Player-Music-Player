package com.pulse.player.data.scan

import android.app.RecoverableSecurityException
import android.content.ContentValues
import android.content.Context
import android.content.IntentSender
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.pulse.player.playback.Track
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder

data class AudioTagEdit(
    val title: String,
    val artist: String,
    val album: String,
    val genre: String,
    val year: Int,
    val trackNumber: Int
)

sealed interface TagWriteResult {
    data object Success : TagWriteResult
    data class NeedsWritePermission(val intentSender: IntentSender) : TagWriteResult
    data class Unsupported(val reason: String) : TagWriteResult
    data class Error(val message: String) : TagWriteResult
}

/**
 * Pure-Kotlin binary tag patcher supporting MP3 (ID3v2.3/2.4 UTF-8 frames) and
 * FLAC (VORBIS_COMMENT metadata block). Preserves existing audio streams losslessly.
 */
object AudioBinaryTagPatcher {
    fun supportsExtension(ext: String): Boolean {
        val clean = ext.trim().lowercase()
        return clean == "mp3" || clean == "flac"
    }

    fun patchBytes(source: ByteArray, ext: String, edit: AudioTagEdit): ByteArray? {
        return when (ext.trim().lowercase()) {
            "mp3" -> patchMp3Id3v2(source, edit)
            "flac" -> patchFlacVorbis(source, edit)
            else -> null
        }
    }

    internal fun patchMp3Id3v2(source: ByteArray, edit: AudioTagEdit): ByteArray? {
        val hasId3 = source.size >= 10 &&
            source[0] == 'I'.code.toByte() &&
            source[1] == 'D'.code.toByte() &&
            source[2] == '3'.code.toByte()

        val oldTagEnd = if (hasId3) {
            val tagSize = decodeSynchsafe(source, 6)
            val footerPresent = (source[5].toInt() and 0x10) != 0
            (10 + tagSize + if (footerPresent) 10 else 0).coerceAtMost(source.size)
        } else {
            0
        }

        val preservedFrames = if (hasId3 && oldTagEnd > 10) {
            extractPreservedId3Frames(source, 10, oldTagEnd, source[3].toInt() and 0xFF)
        } else {
            emptyList()
        }

        val updatedFrames = buildList {
            add(buildId3TextFrame("TIT2", edit.title))
            add(buildId3TextFrame("TPE1", edit.artist))
            add(buildId3TextFrame("TALB", edit.album))
            if (edit.genre.isNotBlank()) add(buildId3TextFrame("TCON", edit.genre))
            if (edit.year > 0) add(buildId3TextFrame("TYER", edit.year.toString()))
            if (edit.trackNumber > 0) add(buildId3TextFrame("TRCK", edit.trackNumber.toString()))
            addAll(preservedFrames)
        }

        val bodyOut = ByteArrayOutputStream()
        for (frame in updatedFrames) {
            bodyOut.write(frame)
        }
        // Small padding (128 bytes) allows minor future in-place tweaks
        bodyOut.write(ByteArray(128))
        val bodyBytes = bodyOut.toByteArray()

        val result = ByteArrayOutputStream(10 + bodyBytes.size + (source.size - oldTagEnd))
        // Write ID3v2.3.0 header (broadest compatibility across Android MediaMetadataRetriever + desktop)
        result.write('I'.code)
        result.write('D'.code)
        result.write('3'.code)
        result.write(3)
        result.write(0)
        result.write(0)
        result.write(encodeSynchsafe(bodyBytes.size))
        result.write(bodyBytes)
        result.write(source, oldTagEnd, source.size - oldTagEnd)
        return result.toByteArray()
    }

    private fun extractPreservedId3Frames(
        source: ByteArray,
        start: Int,
        end: Int,
        majorVersion: Int
    ): List<ByteArray> {
        if (majorVersion !in 3..4) return emptyList()
        val replacedIds = setOf("TIT2", "TPE1", "TALB", "TCON", "TYER", "TDRC", "TRCK")
        val kept = mutableListOf<ByteArray>()
        var pos = start
        while (pos + 10 <= end) {
            if (source[pos] == 0.toByte()) break
            val id = String(source, pos, 4, Charsets.ISO_8859_1)
            if (!id.all { it in 'A'..'Z' || it in '0'..'9' }) break
            val frameSize = if (majorVersion == 4) {
                decodeSynchsafe(source, pos + 4)
            } else {
                ByteBuffer.wrap(source, pos + 4, 4).order(ByteOrder.BIG_ENDIAN).int
            }
            if (frameSize <= 0 || pos + 10 + frameSize > end) break
            if (id !in replacedIds) {
                val frameBytes = source.copyOfRange(pos, pos + 10 + frameSize)
                // Normalize synchsafe size if converting from v2.4 -> v2.3
                if (majorVersion == 4) {
                    val be = ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putInt(frameSize).array()
                    System.arraycopy(be, 0, frameBytes, 4, 4)
                }
                kept.add(frameBytes)
            }
            pos += 10 + frameSize
        }
        return kept
    }

    private fun buildId3TextFrame(frameId: String, text: String): ByteArray {
        val textBytes = text.toByteArray(Charsets.UTF_16)
        val payloadSize = 1 + textBytes.size // encoding byte 0x01 (UTF-16 with BOM) + UTF-16 payload
        val out = ByteArray(10 + payloadSize)
        val idBytes = frameId.toByteArray(Charsets.ISO_8859_1)
        System.arraycopy(idBytes, 0, out, 0, 4)
        val sizeBe = ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putInt(payloadSize).array()
        System.arraycopy(sizeBe, 0, out, 4, 4)
        out[8] = 0
        out[9] = 0
        out[10] = 1 // UTF-16 with BOM (universal across ID3v2.3 & v2.4)
        System.arraycopy(textBytes, 0, out, 11, textBytes.size)
        return out
    }

    internal fun patchFlacVorbis(source: ByteArray, edit: AudioTagEdit): ByteArray? {
        if (source.size < 42 ||
            source[0] != 'f'.code.toByte() ||
            source[1] != 'L'.code.toByte() ||
            source[2] != 'a'.code.toByte() ||
            source[3] != 'C'.code.toByte()
        ) {
            return null
        }

        data class Block(val type: Int, val data: ByteArray)
        val blocks = mutableListOf<Block>()
        var pos = 4
        var lastBlock = false
        while (!lastBlock && pos + 4 <= source.size) {
            val header = source[pos].toInt() and 0xFF
            lastBlock = (header and 0x80) != 0
            val blockType = header and 0x7F
            val length = ((source[pos + 1].toInt() and 0xFF) shl 16) or
                ((source[pos + 2].toInt() and 0xFF) shl 8) or
                (source[pos + 3].toInt() and 0xFF)
            pos += 4
            if (pos + length > source.size) return null
            blocks.add(Block(blockType, source.copyOfRange(pos, pos + length)))
            pos += length
        }
        val audioFramesOffset = pos

        val managedKeys = setOf("TITLE", "ARTIST", "ALBUM", "GENRE", "DATE", "YEAR", "TRACKNUMBER")
        var vendor = "Pulse Music Player"
        val preservedComments = mutableListOf<String>()

        val existingVorbis = blocks.firstOrNull { it.type == 4 }
        if (existingVorbis != null && existingVorbis.data.size >= 8) {
            val buf = ByteBuffer.wrap(existingVorbis.data).order(ByteOrder.LITTLE_ENDIAN)
            val vendorLen = buf.int.coerceIn(0, existingVorbis.data.size - 4)
            val vBytes = ByteArray(vendorLen)
            buf.get(vBytes)
            vendor = String(vBytes, Charsets.UTF_8).ifBlank { vendor }
            if (buf.remaining() >= 4) {
                val count = buf.int.coerceAtLeast(0)
                for (i in 0 until count) {
                    if (buf.remaining() < 4) break
                    val len = buf.int
                    if (len < 0 || len > buf.remaining()) break
                    val cBytes = ByteArray(len)
                    buf.get(cBytes)
                    val comment = String(cBytes, Charsets.UTF_8)
                    val key = comment.substringBefore('=').uppercase()
                    if (key !in managedKeys) {
                        preservedComments.add(comment)
                    }
                }
            }
        }

        val nextComments = buildList {
            add("TITLE=${edit.title}")
            add("ARTIST=${edit.artist}")
            add("ALBUM=${edit.album}")
            if (edit.genre.isNotBlank()) add("GENRE=${edit.genre}")
            if (edit.year > 0) add("DATE=${edit.year}")
            if (edit.trackNumber > 0) add("TRACKNUMBER=${edit.trackNumber}")
            addAll(preservedComments)
        }

        val vorbisBytes = encodeVorbisComment(vendor, nextComments)
        val filteredBlocks = blocks.filter { it.type != 4 && it.type != 1 } + listOf(Block(4, vorbisBytes))

        val out = ByteArrayOutputStream(source.size + 512)
        out.write(byteArrayOf('f'.code.toByte(), 'L'.code.toByte(), 'a'.code.toByte(), 'C'.code.toByte()))
        filteredBlocks.forEachIndexed { idx, block ->
            val isLast = idx == filteredBlocks.lastIndex
            val headerByte = (if (isLast) 0x80 else 0x00) or (block.type and 0x7F)
            out.write(headerByte)
            val len = block.data.size
            out.write((len ushr 16) and 0xFF)
            out.write((len ushr 8) and 0xFF)
            out.write(len and 0xFF)
            out.write(block.data)
        }
        out.write(source, audioFramesOffset, source.size - audioFramesOffset)
        return out.toByteArray()
    }

    private fun encodeVorbisComment(vendor: String, comments: List<String>): ByteArray {
        val vendorBytes = vendor.toByteArray(Charsets.UTF_8)
        val commentBytes = comments.map { it.toByteArray(Charsets.UTF_8) }
        val total = 4 + vendorBytes.size + 4 + commentBytes.sumOf { 4 + it.size }
        val buf = ByteBuffer.allocate(total).order(ByteOrder.LITTLE_ENDIAN)
        buf.putInt(vendorBytes.size)
        buf.put(vendorBytes)
        buf.putInt(commentBytes.size)
        for (c in commentBytes) {
            buf.putInt(c.size)
            buf.put(c)
        }
        return buf.array()
    }

    private fun encodeSynchsafe(value: Int): ByteArray = byteArrayOf(
        ((value ushr 21) and 0x7F).toByte(),
        ((value ushr 14) and 0x7F).toByte(),
        ((value ushr 7) and 0x7F).toByte(),
        (value and 0x7F).toByte()
    )

    private fun decodeSynchsafe(bytes: ByteArray, offset: Int): Int {
        return ((bytes[offset].toInt() and 0x7F) shl 21) or
            ((bytes[offset + 1].toInt() and 0x7F) shl 14) or
            ((bytes[offset + 2].toInt() and 0x7F) shl 7) or
            (bytes[offset + 3].toInt() and 0x7F)
    }
}

object AudioTagWriter {
    fun writeTags(ctx: Context, track: Track, edit: AudioTagEdit): TagWriteResult {
        val ext = detectExtension(track)
        if (!AudioBinaryTagPatcher.supportsExtension(ext)) {
            return TagWriteResult.Unsupported("File write-back currently supports MP3 and FLAC; saved to library database.")
        }

        val uri = runCatching { Uri.parse(track.uri) }.getOrNull()
        val dataPath = track.dataPath.takeIf { it.isNotBlank() }

        // Attempt 1: direct local file write if app has filesystem write access
        if (dataPath != null) {
            val file = File(dataPath)
            if (file.exists() && file.canRead() && file.canWrite()) {
                val patched = runCatching {
                    val original = file.readBytes()
                    AudioBinaryTagPatcher.patchBytes(original, ext, edit)
                }.getOrNull()
                if (patched != null) {
                    val written = runCatching {
                        RandomAccessFile(file, "rw").use { raf ->
                            raf.setLength(0L)
                            raf.write(patched)
                        }
                        true
                    }.getOrDefault(false)
                    if (written) {
                        if (uri != null) updateMediaStoreColumns(ctx, uri, edit)
                        return TagWriteResult.Success
                    }
                }
            }
        }

        // Attempt 2: ContentResolver read+write descriptor (works after SAF or createWriteRequest consent)
        if (uri != null) {
            try {
                val original = ctx.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    ?: return TagWriteResult.Error("Unable to open audio stream")
                val patched = AudioBinaryTagPatcher.patchBytes(original, ext, edit)
                    ?: return TagWriteResult.Unsupported("Container not supported for binary tag patch")
                ctx.contentResolver.openOutputStream(uri, "rwt")?.use { out ->
                    out.write(patched)
                    out.flush()
                } ?: ctx.contentResolver.openFileDescriptor(uri, "rw")?.use { pfd ->
                    java.io.FileOutputStream(pfd.fileDescriptor).use { fos ->
                        fos.channel.truncate(0L)
                        fos.write(patched)
                        fos.flush()
                    }
                } ?: return TagWriteResult.Unsupported("Read-only storage URI; saved in library database.")
                updateMediaStoreColumns(ctx, uri, edit)
                return TagWriteResult.Success
            } catch (sec: SecurityException) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && track.id.startsWith("ms:")) {
                    val pending = runCatching {
                        MediaStore.createWriteRequest(ctx.contentResolver, listOf(uri))
                    }.getOrNull()
                    if (pending != null) {
                        return TagWriteResult.NeedsWritePermission(pending.intentSender)
                    }
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && sec is RecoverableSecurityException) {
                    return TagWriteResult.NeedsWritePermission(sec.userAction.actionIntent.intentSender)
                }
                return TagWriteResult.Unsupported("Permission required by storage provider; saved in library database.")
            } catch (t: Throwable) {
                return TagWriteResult.Error(t.message ?: "Failed writing metadata to audio file")
            }
        }

        return TagWriteResult.Unsupported("Audio file is read-only; saved in library database.")
    }

    private fun updateMediaStoreColumns(ctx: Context, uri: Uri, edit: AudioTagEdit) {
        if (uri.scheme != "content") return
        runCatching {
            val values = ContentValues().apply {
                put(MediaStore.Audio.Media.TITLE, edit.title)
                put(MediaStore.Audio.Media.ARTIST, edit.artist)
                put(MediaStore.Audio.Media.ALBUM, edit.album)
                if (edit.year > 0) put(MediaStore.Audio.Media.YEAR, edit.year)
                if (edit.trackNumber > 0) put(MediaStore.Audio.Media.TRACK, edit.trackNumber)
            }
            ctx.contentResolver.update(uri, values, null, null)
        }
    }

    private fun detectExtension(track: Track): String {
        val fromPath = track.dataPath.substringAfterLast('.', "").lowercase()
        if (fromPath.isNotBlank() && fromPath.length <= 5) return fromPath
        val fromUri = track.uri.substringAfterLast('.', "").substringBefore('?').lowercase()
        if (fromUri.isNotBlank() && fromUri.length <= 5) return fromUri
        val mime = track.mimeType.orEmpty().lowercase()
        return when {
            "mpeg" in mime || "mp3" in mime -> "mp3"
            "flac" in mime -> "flac"
            else -> ""
        }
    }
}
