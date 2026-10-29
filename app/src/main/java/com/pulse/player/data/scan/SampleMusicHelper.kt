package com.pulse.player.data.scan

import android.content.Context
import android.net.Uri
import com.pulse.player.data.db.PulseDb
import com.pulse.player.data.db.TrackEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

object SampleMusicHelper {

    const val SAMPLE_TRACK_ID = "sample:pulse_horizon"

    /**
     * Synthesizes a melodic 15-second 44.1kHz 16-bit stereo PCM WAV file
     * with an electronic chord progression and warm synth arpeggio ("Pulse Horizon").
     */
    suspend fun installSampleTrack(context: Context, db: PulseDb): TrackEntity = withContext(Dispatchers.IO) {
        val sampleDir = File(context.filesDir, "sample_tracks")
        if (!sampleDir.exists()) {
            sampleDir.mkdirs()
        }
        val wavFile = File(sampleDir, "pulse_horizon.wav")
        val tmpFile = File(sampleDir, "pulse_horizon.wav.tmp")
        if (!wavFile.exists() || wavFile.length() < 100_000L) {
            runCatching {
                generateMelodicWav(tmpFile, durationSeconds = 15)
                if (tmpFile.exists() && tmpFile.length() > 100_000L) {
                    if (wavFile.exists()) wavFile.delete()
                    tmpFile.renameTo(wavFile)
                }
            }
        }

        val track = TrackEntity(
            id = SAMPLE_TRACK_ID,
            uri = Uri.fromFile(wavFile).toString(),
            displayName = "pulse_horizon.wav",
            title = "Pulse Horizon",
            artist = "Pulse Studio",
            album = "Neon Drift",
            albumArtist = "Pulse Studio",
            durationMs = 15_000L,
            sizeBytes = if (wavFile.exists()) wavFile.length() else 2646044L,
            mimeType = "audio/x-wav",
            folderPath = sampleDir.absolutePath,
            dateAddedMs = System.currentTimeMillis(),
            dateModifiedMs = System.currentTimeMillis(),
            trackNumber = 1,
            year = 2026,
            normTitle = norm("Pulse Horizon"),
            normArtist = norm("Pulse Studio"),
            normAlbum = norm("Neon Drift"),
            available = true
        )

        db.tracks().upsertAll(listOf(track))
        track
    }

    private fun generateMelodicWav(outputFile: File, durationSeconds: Int) {
        val sampleRate = 44100
        val numChannels = 2 // Stereo
        val bitsPerSample = 16
        val totalFrames = sampleRate * durationSeconds
        val dataSizeBytes = totalFrames * numChannels * (bitsPerSample / 8)

        FileOutputStream(outputFile).use { fos ->
            BufferedOutputStream(fos).use { bos ->
                // Write 44-byte standard RIFF WAVE header
                val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)
                header.put("RIFF".toByteArray(Charsets.US_ASCII))
                header.putInt(36 + dataSizeBytes)
                header.put("WAVE".toByteArray(Charsets.US_ASCII))
                header.put("fmt ".toByteArray(Charsets.US_ASCII))
                header.putInt(16) // Subchunk1Size for PCM
                header.putShort(1.toShort()) // AudioFormat = 1 (PCM)
                header.putShort(numChannels.toShort())
                header.putInt(sampleRate)
                header.putInt(sampleRate * numChannels * (bitsPerSample / 8)) // ByteRate
                header.putShort((numChannels * (bitsPerSample / 8)).toShort()) // BlockAlign
                header.putShort(bitsPerSample.toShort())
                header.put("data".toByteArray(Charsets.US_ASCII))
                header.putInt(dataSizeBytes)

                bos.write(header.array())

        // Musical progression: C4 -> G3 -> A3 -> F3 (Synth chords + arpeggiator)
        // Frequencies in Hz
        val chords = listOf(
            listOf(261.63, 329.63, 392.00, 523.25), // C major (C4, E4, G4, C5)
            listOf(196.00, 246.94, 293.66, 392.00), // G major (G3, B3, D4, G4)
            listOf(220.00, 261.63, 329.63, 440.00), // A minor (A3, C4, E4, A4)
            listOf(174.61, 220.00, 261.63, 349.23)  // F major (F3, A3, C4, F4)
        )

        val chordDurationFrames = sampleRate * 3 // 3 seconds per chord
        val buffer = ByteArray(4096)
        var bufferIndex = 0

        for (frame in 0 until totalFrames) {
            val t = frame.toDouble() / sampleRate
            val chordIndex = ((frame / chordDurationFrames) % chords.size)
            val currentChord = chords[chordIndex]
            val chordProgress = (frame % chordDurationFrames).toDouble() / chordDurationFrames

            // Arpeggio note selector (16th notes: 4 notes per second)
            val arpIndex = ((t * 4).toInt() % currentChord.size)
            val arpFreq = currentChord[arpIndex]

            // Bass note (root)
            val rootFreq = currentChord[0] / 2.0

            // Synthesis with rich harmonics and gentle envelope
            val arpEnv = exp(-4.0 * ((t * 4) % 1.0)) // per-note decay
            val arpWave = (sin(2.0 * PI * arpFreq * t) + 0.5 * sin(4.0 * PI * arpFreq * t)) * arpEnv * 0.35

            val bassWave = (sin(2.0 * PI * rootFreq * t) + 0.3 * sin(4.0 * PI * rootFreq * t)) * 0.30

            // Soft pad chord background
            var padWave = 0.0
            for (f in currentChord) {
                padWave += sin(2.0 * PI * f * t) * 0.05
            }

            // Overall fade in/out
            val fadeIn = (frame.toDouble() / (sampleRate * 0.5)).coerceIn(0.0, 1.0)
            val fadeOut = ((totalFrames - frame).toDouble() / (sampleRate * 1.0)).coerceIn(0.0, 1.0)
            val masterVol = fadeIn * fadeOut

            val mixed = ((arpWave + bassWave + padWave) * masterVol * 0.75).coerceIn(-1.0, 1.0)

            // Convert to 16-bit signed PCM
            val sampleShort = (mixed * 32767.0).toInt().toShort()

            // Left channel
            buffer[bufferIndex++] = (sampleShort.toInt() and 0xFF).toByte()
            buffer[bufferIndex++] = ((sampleShort.toInt() shr 8) and 0xFF).toByte()

            // Right channel with slight stereo pan/delay for spatial depth
            val rightShort = ((mixed * 0.95) * 32767.0).toInt().toShort()
            buffer[bufferIndex++] = (rightShort.toInt() and 0xFF).toByte()
            buffer[bufferIndex++] = ((rightShort.toInt() shr 8) and 0xFF).toByte()

            if (bufferIndex >= buffer.size) {
                bos.write(buffer, 0, bufferIndex)
                bufferIndex = 0
            }
        }

                if (bufferIndex > 0) {
                    bos.write(buffer, 0, bufferIndex)
                }

                bos.flush()
            }
        }
    }
}
