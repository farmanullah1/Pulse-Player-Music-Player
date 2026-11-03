package com.pulse.player.core.error

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/** A user-facing failure with a stable category and a recoverable action. */
sealed interface AppError {
    val message: String
    val actionLabel: String?

    data class Storage(
        override val message: String,
        override val actionLabel: String? = "Try again",
        val cause: Throwable? = null
    ) : AppError

    data class Permission(
        override val message: String,
        override val actionLabel: String? = "Open settings"
    ) : AppError

    data class Playback(
        override val message: String,
        override val actionLabel: String? = "Retry"
    ) : AppError

    data class Backup(
        override val message: String,
        override val actionLabel: String? = "Try again",
        val cause: Throwable? = null
    ) : AppError

    data class Unsupported(
        override val message: String,
        override val actionLabel: String? = null
    ) : AppError
}

sealed interface UiEvent {
    data class Error(val error: AppError) : UiEvent
    data class Message(val text: String) : UiEvent
}

/** Process-wide event bus for transient UI feedback from repositories/services. */
object UiEvents {
    private val _events = MutableSharedFlow<UiEvent>(extraBufferCapacity = 32)
    val events: SharedFlow<UiEvent> = _events.asSharedFlow()

    fun emit(event: UiEvent) {
        _events.tryEmit(event)
    }
}

/** Converts technical failures into safe, user-readable events. */
object ErrorReporter {
    fun report(error: AppError) {
        UiEvents.emit(UiEvent.Error(error))
    }

    fun reportStorage(message: String, cause: Throwable? = null) {
        report(AppError.Storage(message, cause = cause))
    }

    fun reportBackup(message: String, cause: Throwable? = null) {
        report(AppError.Backup(message, cause = cause))
    }
}
