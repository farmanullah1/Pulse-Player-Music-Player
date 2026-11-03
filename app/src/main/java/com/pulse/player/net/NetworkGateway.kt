package com.pulse.player.net

import android.content.Context
import java.io.File

data class TranscriptionResult(
    val text: String,
    val modelUsed: String
)

data class GeneratedMusicResult(
    val file: File,
    val title: String,
    val description: String,
    val prompt: String,
    val model: String
)

/** The only application boundary for optional network-backed features. */
interface NetworkGateway {
    suspend fun transcribeAudio(
        audioFile: File,
        apiKey: String,
        customPrompt: String? = null
    ): Result<TranscriptionResult>

    suspend fun generateMusic(
        context: Context,
        prompt: String,
        modelName: String,
        apiKey: String
    ): Result<GeneratedMusicResult>
}

/** Used by the offline flavor and by builds where online access is disabled. */
class OfflineNetworkGateway : NetworkGateway {
    private fun unavailable(): Result<Nothing> = Result.failure(
        UnsupportedOperationException("Online features are not included in the offline build.")
    )

    override suspend fun transcribeAudio(
        audioFile: File,
        apiKey: String,
        customPrompt: String?
    ): Result<TranscriptionResult> = unavailable()

    override suspend fun generateMusic(
        context: Context,
        prompt: String,
        modelName: String,
        apiKey: String
    ): Result<GeneratedMusicResult> = unavailable()
}
