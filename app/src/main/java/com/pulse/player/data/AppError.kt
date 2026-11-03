package com.pulse.player.data

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

enum class ErrorCategory {
    PLAYBACK,
    SCAN,
    BACKUP,
    AUDIO_FX,
    STORAGE
}

data class AppError(
    val id: String = UUID.randomUUID().toString(),
    val timestampMs: Long = System.currentTimeMillis(),
    val category: ErrorCategory,
    val title: String,
    val message: String,
    val technicalDetails: String? = null,
    val actionable: Boolean = true
)

object ErrorDispatcher {
    private const val MAX_LOG_SIZE = 50

    private val _recentErrors = MutableStateFlow<List<AppError>>(emptyList())
    val recentErrors = _recentErrors.asStateFlow()

    private val _events = MutableSharedFlow<AppError>(replay = 1, extraBufferCapacity = 16)
    val events = _events.asSharedFlow()

    fun post(error: AppError) {
        val current = _recentErrors.value.toMutableList()
        current.add(0, error)
        if (current.size > MAX_LOG_SIZE) {
            current.removeAt(current.size - 1)
        }
        _recentErrors.value = current
        _events.tryEmit(error)
    }

    fun post(
        category: ErrorCategory,
        title: String,
        message: String,
        technicalDetails: String? = null
    ) {
        post(
            AppError(
                category = category,
                title = title,
                message = message,
                technicalDetails = technicalDetails
            )
        )
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    fun clearLog() {
        _recentErrors.value = emptyList()
        _events.resetReplayCache()
    }
}
