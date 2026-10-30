package com.pulse.player.playback

import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class ErrorPolicy(private val raw: Player) {
    enum class Category { MISSING_FILE, NO_PERMISSION, UNSUPPORTED, OTHER }
    data class TrackError(val mediaId: String?, val title: String, val category: Category, val message: String)

    private val _current = MutableStateFlow<TrackError?>(null)
    val current = _current.asStateFlow()

    private val retries = HashMap<String, Int>()

    fun category(e: PlaybackException): Category = when (e.errorCode) {
        PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND -> Category.MISSING_FILE
        PlaybackException.ERROR_CODE_IO_NO_PERMISSION -> Category.NO_PERMISSION
        PlaybackException.ERROR_CODE_DECODER_INIT_FAILED,
        PlaybackException.ERROR_CODE_DECODING_FAILED,
        PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED,
        PlaybackException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED -> Category.UNSUPPORTED
        else -> Category.OTHER
    }

    fun onError(e: PlaybackException) {
        val item = raw.currentMediaItem
        val id = item?.mediaId ?: "?"
        val cat = category(e)
        val n = retries.getOrDefault(id, 0)

        // At most one automatic retry, only for unknown errors
        if (n < 1 && cat == Category.OTHER) {
            retries[id] = n + 1
            raw.prepare()
            return
        }

        raw.pause()
        PlaybackEngine.transitions.restore()
        _current.value = TrackError(
            mediaId = id,
            title = item?.mediaMetadata?.title?.toString().orEmpty(),
            category = cat,
            message = when (cat) {
                Category.MISSING_FILE -> "This file can't be found. It may have been moved or the SD card removed."
                Category.NO_PERMISSION -> "Pulse Player can't read this file. Check the audio permission."
                Category.UNSUPPORTED -> "This format can't be played on this device."
                Category.OTHER -> "This track failed to play."
            }
        )
    }

    fun onPlaying() {
        raw.currentMediaItem?.mediaId?.let { retries.remove(it) }
        _current.value = null
    }

    fun retry() {
        _current.value?.mediaId?.let { retries.remove(it) }
        _current.value = null
        raw.prepare()
        raw.play()
    }

    fun skip() {
        _current.value = null
        if (raw.hasNextMediaItem()) {
            raw.seekToNextMediaItem()
            raw.prepare()
            raw.play()
        }
    }
}

