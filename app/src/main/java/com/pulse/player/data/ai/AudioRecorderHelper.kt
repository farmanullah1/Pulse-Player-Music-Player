package com.pulse.player.data.ai

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

class AudioRecorderHelper(private val scope: CoroutineScope) {
    private var recorder: MediaRecorder? = null
    private var currentFile: File? = null
    private var timerJob: Job? = null

    private val _isRecording = MutableStateFlow(false)
    val isRecording = _isRecording.asStateFlow()

    private val _durationSec = MutableStateFlow(0)
    val durationSec = _durationSec.asStateFlow()

    private val _maxAmplitude = MutableStateFlow(0)
    val maxAmplitude = _maxAmplitude.asStateFlow()

    fun start(context: Context): Result<File> = runCatching {
        stop()
        val dir = File(context.cacheDir, "recordings").apply { mkdirs() }
        val file = File(dir, "record_${System.currentTimeMillis()}.m4a")
        currentFile = file

        val rec = if (Build.VERSION.SDK_INT >= 31) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }

        rec.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioEncodingBitRate(128000)
            setAudioSamplingRate(44100)
            setOutputFile(file.absolutePath)
            prepare()
            start()
        }
        recorder = rec
        _isRecording.value = true
        _durationSec.value = 0

        timerJob = scope.launch(Dispatchers.Default) {
            var sec = 0
            while (isActive && _isRecording.value) {
                delay(100)
                val amp = runCatching { recorder?.maxAmplitude ?: 0 }.getOrDefault(0)
                _maxAmplitude.value = amp
                delay(900)
                sec++
                _durationSec.value = sec
            }
        }

        file
    }

    fun stop(): File? {
        timerJob?.cancel()
        timerJob = null
        val file = currentFile
        try {
            recorder?.apply {
                runCatching { stop() }
                runCatching { release() }
            }
        } catch (e: Exception) {
            Log.e("AudioRecorderHelper", "Error stopping recorder", e)
        } finally {
            recorder = null
            _isRecording.value = false
            _maxAmplitude.value = 0
        }
        return file?.takeIf { it.exists() && it.length() > 0 }
    }

    fun cancel() {
        val file = stop()
        file?.delete()
        currentFile = null
        _durationSec.value = 0
    }
}
