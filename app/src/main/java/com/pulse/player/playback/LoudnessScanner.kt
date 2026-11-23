package com.pulse.player.playback

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import android.os.Build
import com.pulse.player.data.db.PulseDb
import com.pulse.player.data.db.TrackEntity
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.log10
import kotlin.math.sqrt

sealed interface LoudnessScanState {
    data object Idle : LoudnessScanState
    data class Scanning(val currentTitle: String, val processed: Int, val total: Int) : LoudnessScanState
    data class Completed(val updatedCount: Int, val failedCount: Int) : LoudnessScanState
    data class Cancelled(val processedCount: Int) : LoudnessScanState
}

object LoudnessMath {
    const val REFERENCE_LUFS = -18.0f
    const val MIN_GAIN_DB = -12.0f
    const val MAX_GAIN_DB = 9.0f

    fun meanSquareToLufs(meanSquare: Double): Float {
        if (meanSquare <= 1e-9 || meanSquare.isNaN() || meanSquare.isInfinite()) return -70.0f
        val rms = sqrt(meanSquare)
        // Approximate K-weighted integrated LUFS relative to 16-bit full scale
        return (20.0 * log10(rms)).toFloat().coerceIn(-70.0f, 0.0f)
    }

    fun pcmSamplesToLufs(samples: ShortArray, count: Int = samples.size): Float {
        val n = count.coerceAtMost(samples.size)
        if (n <= 0) return -70.0f
        var sumSq = 0.0
        for (i in 0 until n) {
            val normalized = samples[i] / 32768.0
            sumSq += normalized * normalized
        }
        return meanSquareToLufs(sumSq / n.toDouble())
    }

    fun lufsToReplayGainDb(lufs: Float): Float {
        if (lufs <= -65.0f || lufs.isNaN() || lufs.isInfinite()) return 0.0f
        return (REFERENCE_LUFS - lufs).coerceIn(MIN_GAIN_DB, MAX_GAIN_DB)
    }

    fun computeAlbumGainDb(trackGainsDb: List<Float>): Float {
        val valid = trackGainsDb.filter { it != 0f && !it.isNaN() && !it.isInfinite() }
        if (valid.isEmpty()) return 0f
        return (valid.sum() / valid.size.toFloat()).coerceIn(MIN_GAIN_DB, MAX_GAIN_DB)
    }
}

object LoudnessScanner {
    private const val MAX_SAMPLE_DURATION_US = 45_000_000L

    suspend fun scanMissingTracks(
        ctx: Context,
        db: PulseDb,
        onProgress: (LoudnessScanState) -> Unit
    ): LoudnessScanState = withContext(Dispatchers.IO) {
        val candidates = db.tracks().tracksMissingReplayGainOnce()
        if (candidates.isEmpty()) {
            val done = LoudnessScanState.Completed(updatedCount = 0, failedCount = 0)
            onProgress(done)
            return@withContext done
        }

        val total = candidates.size
        var updated = 0
        var failed = 0
        val measuredByTrackId = HashMap<String, Float>(total)

        try {
            candidates.forEachIndexed { index, track ->
                currentCoroutineContext().ensureActive()
                onProgress(
                    LoudnessScanState.Scanning(
                        currentTitle = track.title.ifBlank { track.displayName },
                        processed = index,
                        total = total
                    )
                )

                val lufs = runCatching {
                    measureTrackLufs(ctx, track)
                }.getOrNull()

                if (lufs != null && lufs > -65f) {
                    val gainDb = LoudnessMath.lufsToReplayGainDb(lufs)
                    measuredByTrackId[track.id] = gainDb
                    db.tracks().updateReplayGain(track.id, gainDb, track.replayGainAlbumDb)
                    updated++
                } else {
                    failed++
                }
            }

            // Compute album-level averages for scanned tracks sharing the same album
            if (measuredByTrackId.isNotEmpty()) {
                val availableTracks = db.tracks().availableOnce()
                val byAlbum = availableTracks
                    .filter { it.album.isNotBlank() }
                    .groupBy { it.album.lowercase() }
                for ((_, group) in byAlbum) {
                    val hasNewlyMeasured = group.any { it.id in measuredByTrackId }
                    if (!hasNewlyMeasured) continue
                    val gains = group.mapNotNull { entity ->
                        measuredByTrackId[entity.id] ?: entity.replayGainTrackDb.takeIf { it != 0f }
                    }
                    val albumGain = LoudnessMath.computeAlbumGainDb(gains)
                    if (albumGain != 0f) {
                        for (entity in group) {
                            val trackGain = measuredByTrackId[entity.id] ?: entity.replayGainTrackDb
                            db.tracks().updateReplayGain(entity.id, trackGain, albumGain)
                        }
                    }
                }
                PlaybackEngine.invalidateReplayGainCache()
            }

            val completed = LoudnessScanState.Completed(updatedCount = updated, failedCount = failed)
            onProgress(completed)
            completed
        } catch (cancel: CancellationException) {
            onProgress(LoudnessScanState.Cancelled(processedCount = updated))
            throw cancel
        }
    }

    private fun measureTrackLufs(ctx: Context, track: TrackEntity): Float? {
        val extractor = MediaExtractor()
        var codec: MediaCodec? = null
        try {
            val dataPath = track.dataPath?.takeIf { it.isNotBlank() && java.io.File(it).canRead() }
            if (dataPath != null) {
                extractor.setDataSource(dataPath)
            } else {
                extractor.setDataSource(ctx, Uri.parse(track.uri), null)
            }

            var audioTrackIndex = -1
            var inputFormat: MediaFormat? = null
            for (i in 0 until extractor.trackCount) {
                val fmt = extractor.getTrackFormat(i)
                val mime = fmt.getString(MediaFormat.KEY_MIME) ?: continue
                if (mime.startsWith("audio/")) {
                    audioTrackIndex = i
                    inputFormat = fmt
                    break
                }
            }
            if (audioTrackIndex < 0 || inputFormat == null) return null

            extractor.selectTrack(audioTrackIndex)
            val mime = inputFormat.getString(MediaFormat.KEY_MIME) ?: return null
            val durationUs = if (inputFormat.containsKey(MediaFormat.KEY_DURATION)) {
                inputFormat.getLong(MediaFormat.KEY_DURATION)
            } else {
                track.durationMs * 1000L
            }
            // Start 15% into longer tracks to skip quiet intros and capture representative loudness
            if (durationUs > MAX_SAMPLE_DURATION_US * 2) {
                extractor.seekTo((durationUs * 0.15).toLong(), MediaExtractor.SEEK_TO_CLOSEST_SYNC)
            }

            codec = MediaCodec.createDecoderByType(mime)
            codec.configure(inputFormat, null, null, 0)
            codec.start()

            val bufferInfo = MediaCodec.BufferInfo()
            var sawInputEos = false
            var sawOutputEos = false
            var sumSq = 0.0
            var sampleCount = 0L
            var decodedDurationUs = 0L
            val startPtsUs = if (durationUs > MAX_SAMPLE_DURATION_US * 2) (durationUs * 0.15).toLong() else 0L
            var encoding = android.media.AudioFormat.ENCODING_PCM_16BIT

            while (!sawOutputEos && decodedDurationUs < MAX_SAMPLE_DURATION_US) {
                if (!sawInputEos) {
                    val inIdx = codec.dequeueInputBuffer(10_000L)
                    if (inIdx >= 0) {
                        val inBuf: ByteBuffer? = codec.getInputBuffer(inIdx)
                        val sampleSize = if (inBuf != null) extractor.readSampleData(inBuf, 0) else -1
                        if (sampleSize < 0) {
                            codec.queueInputBuffer(inIdx, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            sawInputEos = true
                        } else {
                            val pts = extractor.sampleTime.coerceAtLeast(0L)
                            codec.queueInputBuffer(inIdx, 0, sampleSize, pts, 0)
                            extractor.advance()
                        }
                    }
                }

                val outIdx = codec.dequeueOutputBuffer(bufferInfo, 10_000L)
                when {
                    outIdx >= 0 -> {
                        val outBuf = codec.getOutputBuffer(outIdx)
                        if (outBuf != null && bufferInfo.size > 0) {
                            outBuf.position(bufferInfo.offset)
                            outBuf.limit(bufferInfo.offset + bufferInfo.size)
                            outBuf.order(ByteOrder.LITTLE_ENDIAN)
                            if (encoding == android.media.AudioFormat.ENCODING_PCM_FLOAT) {
                                while (outBuf.remaining() >= 4) {
                                    val s = outBuf.float.toDouble().coerceIn(-1.0, 1.0)
                                    sumSq += s * s
                                    sampleCount++
                                }
                            } else {
                                while (outBuf.remaining() >= 2) {
                                    val s = outBuf.short.toDouble() / 32768.0
                                    sumSq += s * s
                                    sampleCount++
                                }
                            }
                            if (bufferInfo.presentationTimeUs > startPtsUs) {
                                decodedDurationUs = bufferInfo.presentationTimeUs - startPtsUs
                            }
                        }
                        codec.releaseOutputBuffer(outIdx, false)
                        if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                            sawOutputEos = true
                        }
                    }
                    outIdx == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        val outFmt = codec.outputFormat
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N &&
                            outFmt.containsKey(MediaFormat.KEY_PCM_ENCODING)
                        ) {
                            encoding = outFmt.getInteger(MediaFormat.KEY_PCM_ENCODING)
                        }
                    }
                }
            }

            if (sampleCount == 0L) return null
            return LoudnessMath.meanSquareToLufs(sumSq / sampleCount.toDouble())
        } finally {
            runCatching { codec?.stop() }
            runCatching { codec?.release() }
            runCatching { extractor.release() }
        }
    }
}
