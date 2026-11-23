package com.pulse.player.data.scan

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

class AudioBinaryTagPatcherTest {

    @Test
    fun `supportsExtension allows mp3 and flac only`() {
        assertTrue(AudioBinaryTagPatcher.supportsExtension("mp3"))
        assertTrue(AudioBinaryTagPatcher.supportsExtension("FLAC"))
        assertFalse(AudioBinaryTagPatcher.supportsExtension("ogg"))
        assertFalse(AudioBinaryTagPatcher.supportsExtension("m4a"))
    }

    @Test
    fun `patchMp3Id3v2 writes ID3v2_3 frames preserves APIC and keeps audio payload intact`() {
        val unknownApicFrame = byteArrayOf(
            'A'.code.toByte(), 'P'.code.toByte(), 'I'.code.toByte(), 'C'.code.toByte(),
            0, 0, 0, 4, // size = 4
            0, 0,
            1, 2, 3, 4
        )
        val oldTitleFrame = byteArrayOf(
            'T'.code.toByte(), 'I'.code.toByte(), 'T'.code.toByte(), '2'.code.toByte(),
            0, 0, 0, 4,
            0, 0,
            0, 'O'.code.toByte(), 'l'.code.toByte(), 'd'.code.toByte()
        )
        val tagBody = oldTitleFrame + unknownApicFrame
        val mp3AudioPayload = byteArrayOf(0xFF.toByte(), 0xFB.toByte(), 0x90.toByte(), 0x64, 0x11, 0x22, 0x33)

        val synchSize = byteArrayOf(
            ((tagBody.size ushr 21) and 0x7F).toByte(),
            ((tagBody.size ushr 14) and 0x7F).toByte(),
            ((tagBody.size ushr 7) and 0x7F).toByte(),
            (tagBody.size and 0x7F).toByte()
        )
        val original = ByteArrayOutputStream().apply {
            write(byteArrayOf('I'.code.toByte(), 'D'.code.toByte(), '3'.code.toByte(), 3, 0, 0))
            write(synchSize)
            write(tagBody)
            write(mp3AudioPayload)
        }.toByteArray()

        val edit = AudioTagEdit(
            title = "Midnight Pulse",
            artist = "Nova",
            album = "Afterhours",
            genre = "Synthwave",
            year = 2025,
            trackNumber = 4
        )
        val patched = AudioBinaryTagPatcher.patchBytes(original, "mp3", edit)
        assertNotNull(patched)
        patched!!

        // Header checks
        assertEquals('I'.code.toByte(), patched[0])
        assertEquals('D'.code.toByte(), patched[1])
        assertEquals('3'.code.toByte(), patched[2])
        assertEquals(3.toByte(), patched[3])

        val newTagSize = ((patched[6].toInt() and 0x7F) shl 21) or
            ((patched[7].toInt() and 0x7F) shl 14) or
            ((patched[8].toInt() and 0x7F) shl 7) or
            (patched[9].toInt() and 0x7F)
        val audioSlice = patched.copyOfRange(10 + newTagSize, patched.size)
        assertArrayEquals(mp3AudioPayload, audioSlice)

        val tagSliceStr = String(patched.copyOfRange(10, 10 + newTagSize), Charsets.ISO_8859_1)
        assertTrue(tagSliceStr.contains("APIC"))
        assertTrue(tagSliceStr.contains("TIT2"))
        assertTrue(tagSliceStr.contains("TPE1"))
        assertTrue(tagSliceStr.contains("TYER"))
    }

    @Test
    fun `patchFlacVorbis updates standard comments preserves replaygain and keeps audio frames`() {
        val streamInfo = ByteArray(34) { 0x42 }
        val customComment = "REPLAYGAIN_TRACK_GAIN=-5.20 dB".toByteArray(Charsets.UTF_8)
        val oldTitleComment = "TITLE=Old Song".toByteArray(Charsets.UTF_8)
        val vendor = "PulseTest".toByteArray(Charsets.UTF_8)
        val vorbisBuf = ByteBuffer.allocate(4 + vendor.size + 4 + 4 + oldTitleComment.size + 4 + customComment.size)
            .order(ByteOrder.LITTLE_ENDIAN)
        vorbisBuf.putInt(vendor.size)
        vorbisBuf.put(vendor)
        vorbisBuf.putInt(2)
        vorbisBuf.putInt(oldTitleComment.size)
        vorbisBuf.put(oldTitleComment)
        vorbisBuf.putInt(customComment.size)
        vorbisBuf.put(customComment)
        val vorbisData = vorbisBuf.array()

        val flacAudioPayload = byteArrayOf(0xFF.toByte(), 0xF8.toByte(), 0x55, 0x66, 0x77)
        val original = ByteArrayOutputStream().apply {
            write(byteArrayOf('f'.code.toByte(), 'L'.code.toByte(), 'a'.code.toByte(), 'C'.code.toByte()))
            // Block 0: STREAMINFO (type 0, not last)
            write(0x00)
            write(0); write(0); write(streamInfo.size)
            write(streamInfo)
            // Block 1: VORBIS_COMMENT (type 4, last = 0x84)
            write(0x84)
            write((vorbisData.size ushr 16) and 0xFF)
            write((vorbisData.size ushr 8) and 0xFF)
            write(vorbisData.size and 0xFF)
            write(vorbisData)
            write(flacAudioPayload)
        }.toByteArray()

        val edit = AudioTagEdit(
            title = "Neon Skyline",
            artist = "Aurora Trio",
            album = "Night Drive",
            genre = "Electronic",
            year = 2024,
            trackNumber = 2
        )
        val patched = AudioBinaryTagPatcher.patchBytes(original, "flac", edit)
        assertNotNull(patched)
        patched!!

        assertEquals('f'.code.toByte(), patched[0])
        assertEquals('L'.code.toByte(), patched[1])
        assertEquals('a'.code.toByte(), patched[2])
        assertEquals('C'.code.toByte(), patched[3])

        // Walk blocks to locate end of metadata
        var pos = 4
        var last = false
        while (!last) {
            val h = patched[pos].toInt() and 0xFF
            last = (h and 0x80) != 0
            val len = ((patched[pos + 1].toInt() and 0xFF) shl 16) or
                ((patched[pos + 2].toInt() and 0xFF) shl 8) or
                (patched[pos + 3].toInt() and 0xFF)
            pos += 4 + len
        }
        assertArrayEquals(flacAudioPayload, patched.copyOfRange(pos, patched.size))

        val metaText = String(patched.copyOfRange(0, pos), Charsets.UTF_8)
        assertTrue(metaText.contains("TITLE=Neon Skyline"))
        assertTrue(metaText.contains("ARTIST=Aurora Trio"))
        assertTrue(metaText.contains("REPLAYGAIN_TRACK_GAIN=-5.20 dB"))
        assertFalse(metaText.contains("TITLE=Old Song"))
    }

    @Test
    fun `patchBytes rejects malformed headers or unsupported extensions`() {
        val edit = AudioTagEdit("A", "B", "C", "", 0, 0)
        assertNull(AudioBinaryTagPatcher.patchBytes(byteArrayOf(1, 2, 3), "flac", edit))
        assertNull(AudioBinaryTagPatcher.patchBytes(byteArrayOf(1, 2, 3), "wav", edit))
    }
}
