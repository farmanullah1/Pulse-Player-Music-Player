package com.pulse.player.data.ai

import android.content.Context
import android.util.Base64
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

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

object GeminiAudioClient {
    private const val TAG = "GeminiAudioClient"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models"

    private val client by lazy {
        OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    /**
     * Transcribes an audio file using gemini-3.5-transcribe.
     */
    suspend fun transcribeAudio(
        audioFile: File,
        apiKey: String,
        customPrompt: String? = null
    ): Result<TranscriptionResult> = withContext(Dispatchers.IO) {
        runCatching {
            require(apiKey.isNotBlank()) {
                "Gemini API key is missing. Please configure GEMINI_API_KEY in the Secrets panel in AI Studio or in Pulse Settings."
            }
            require(audioFile.exists() && audioFile.length() > 0) {
                "Audio recording file is empty or does not exist."
            }

            val audioBytes = audioFile.readBytes()
            val base64Data = Base64.encodeToString(audioBytes, Base64.NO_WRAP)
            val mimeType = when (audioFile.extension.lowercase()) {
                "m4a", "mp4" -> "audio/mp4"
                "mp3" -> "audio/mp3"
                "wav" -> "audio/wav"
                "ogg" -> "audio/ogg"
                else -> "audio/mp4"
            }

            val promptText = customPrompt?.ifBlank { null }
                ?: "Please transcribe this spoken audio into clear, accurate text. Format lyrics or spoken sentences with proper punctuation and line breaks."

            val requestJson = JSONObject().apply {
                val contents = JSONArray().apply {
                    val contentObj = JSONObject().apply {
                        val parts = JSONArray().apply {
                            put(JSONObject().apply {
                                put("inlineData", JSONObject().apply {
                                    put("mimeType", mimeType)
                                    put("data", base64Data)
                                })
                            })
                            put(JSONObject().apply {
                                put("text", promptText)
                            })
                        }
                        put("parts", parts)
                    }
                    put(contentObj)
                }
                put("contents", contents)
            }

            // Attempt with requested model gemini-3.5-transcribe
            val model = "gemini-3.5-transcribe"
            val response = executeGenerateContent(model, requestJson.toString(), apiKey)
            if (response.isSuccess) {
                val text = response.getOrThrow()
                return@runCatching TranscriptionResult(text, model)
            }

            // If gemini-3.5-transcribe fails or is unavailable in region, fallback seamlessly to gemini-3.8-flash audio understanding
            val fallbackModel = "gemini-3.8-flash"
            Log.w(TAG, "gemini-3.5-transcribe failed (${response.exceptionOrNull()?.message}), attempting fallback to $fallbackModel")
            val fallbackResponse = executeGenerateContent(fallbackModel, requestJson.toString(), apiKey)
            val fallbackText = fallbackResponse.getOrThrow()
            TranscriptionResult(fallbackText, fallbackModel)
        }
    }

    /**
     * Generates music using lyria-3-clip-preview (for short clips up to 30s) or lyria-3-pro-preview.
     */
    suspend fun generateMusic(
        context: Context,
        prompt: String,
        modelName: String = "lyria-3-clip-preview",
        apiKey: String
    ): Result<GeneratedMusicResult> = withContext(Dispatchers.IO) {
        runCatching {
            require(apiKey.isNotBlank()) {
                "Gemini API key is missing. Please configure GEMINI_API_KEY in the Secrets panel in AI Studio or in Pulse Settings."
            }
            require(prompt.isNotBlank()) {
                "Please enter a musical description or prompt."
            }

            val selectedModel = if (modelName == "lyria-3-pro-preview") "lyria-3-pro-preview" else "lyria-3-clip-preview"

            val requestJson = JSONObject().apply {
                val contents = JSONArray().apply {
                    val contentObj = JSONObject().apply {
                        val parts = JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", prompt.trim())
                            })
                        }
                        put("parts", parts)
                    }
                    put(contentObj)
                }
                put("contents", contents)
            }

            val url = "$BASE_URL/$selectedModel:generateContent?key=$apiKey"
            val requestBody = requestJson.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string().orEmpty()

            if (!response.isSuccessful) {
                val errMessage = parseErrorMessage(responseBody)
                throw IllegalStateException("Music generation failed ($selectedModel): $errMessage")
            }

            val json = JSONObject(responseBody)
            val candidates = json.optJSONArray("candidates")
                ?: throw IllegalStateException("Model returned no generation candidates.")

            val firstCandidate = candidates.optJSONObject(0)
            val content = firstCandidate?.optJSONObject("content")
            val parts = content?.optJSONArray("parts")
                ?: throw IllegalStateException("Model returned no content parts.")

            var audioBytes: ByteArray? = null
            var textDescription = ""

            for (i in 0 until parts.length()) {
                val part = parts.getJSONObject(i)
                if (part.has("text")) {
                    textDescription += part.getString("text") + "\n"
                }
                if (part.has("inlineData")) {
                    val inline = part.getJSONObject("inlineData")
                    val data = inline.optString("data")
                    if (data.isNotBlank()) {
                        audioBytes = Base64.decode(data, Base64.DEFAULT)
                    }
                }
            }

            if (audioBytes == null || audioBytes.isEmpty()) {
                // If model output text without inlineData, check if response contains audio URL or error
                throw IllegalStateException(
                    "No audio stream returned by $selectedModel. " +
                            (if (textDescription.isNotBlank()) "Model notes: $textDescription" else "Please try a different prompt.")
                )
            }

            val musicDir = File(context.filesDir, "ai_music").apply { mkdirs() }
            val timestamp = System.currentTimeMillis()
            val titleCandidate = prompt.take(30).trim().replace(Regex("[^a-zA-Z0-9 ]"), "").trim()
            val cleanTitle = if (titleCandidate.isNotBlank()) titleCandidate else "AI Creation $timestamp"
            val outputFile = File(musicDir, "lyria_${timestamp}.mp3")

            FileOutputStream(outputFile).use { it.write(audioBytes) }

            GeneratedMusicResult(
                file = outputFile,
                title = cleanTitle,
                description = textDescription.trim().ifBlank { "Generated by Google Lyria" },
                prompt = prompt,
                model = selectedModel
            )
        }
    }

    private fun executeGenerateContent(
        model: String,
        jsonBody: String,
        apiKey: String
    ): Result<String> {
        val url = "$BASE_URL/$model:generateContent?key=$apiKey"
        val requestBody = jsonBody.toRequestBody("application/json; charset=utf-8".toMediaType())
        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        return try {
            val response = client.newCall(request).execute()
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                val errMsg = parseErrorMessage(body)
                Result.failure(IllegalStateException("HTTP ${response.code}: $errMsg"))
            } else {
                val json = JSONObject(body)
                val candidates = json.optJSONArray("candidates")
                val first = candidates?.optJSONObject(0)
                val content = first?.optJSONObject("content")
                val parts = content?.optJSONArray("parts")
                val sb = StringBuilder()
                if (parts != null) {
                    for (i in 0 until parts.length()) {
                        val p = parts.getJSONObject(i)
                        if (p.has("text")) {
                            sb.append(p.getString("text")).append("\n")
                        }
                    }
                }
                val text = sb.toString().trim()
                if (text.isNotBlank()) {
                    Result.success(text)
                } else {
                    Result.failure(IllegalStateException("Empty transcription returned by $model"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun parseErrorMessage(responseBody: String): String {
        return runCatching {
            val json = JSONObject(responseBody)
            if (json.has("error")) {
                val err = json.getJSONObject("error")
                err.optString("message", responseBody)
            } else {
                responseBody.take(200)
            }
        }.getOrDefault(responseBody.take(200))
    }
}
