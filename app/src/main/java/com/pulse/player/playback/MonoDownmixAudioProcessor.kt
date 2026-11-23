package com.pulse.player.playback

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import java.nio.ByteBuffer

/**
 * Real-time 16-bit stereo-to-mono downmix processor attached to ExoPlayer's AudioSink.
 * Can be toggled mid-playback without tearing down or flushing the pipeline.
 */
@UnstableApi
class MonoDownmixAudioProcessor : BaseAudioProcessor() {
    @Volatile
    var monoEnabled: Boolean = false

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        return if (inputAudioFormat.encoding == C.ENCODING_PCM_16BIT && inputAudioFormat.channelCount == 2) {
            inputAudioFormat
        } else {
            AudioProcessor.AudioFormat.NOT_SET
        }
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val remaining = inputBuffer.remaining()
        if (remaining == 0) return
        val out = replaceOutputBuffer(remaining)
        if (!monoEnabled) {
            out.put(inputBuffer)
            out.flip()
            return
        }
        while (inputBuffer.remaining() >= 4) {
            val left = inputBuffer.short.toInt()
            val right = inputBuffer.short.toInt()
            val mono = ((left + right) / 2).coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            out.putShort(mono)
            out.putShort(mono)
        }
        if (inputBuffer.hasRemaining()) {
            out.put(inputBuffer)
        }
        out.flip()
    }
}
