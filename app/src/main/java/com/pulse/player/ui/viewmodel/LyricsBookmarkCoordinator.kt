package com.pulse.player.ui.viewmodel

import android.content.Context
import android.net.Uri
import com.pulse.player.data.ErrorCategory
import com.pulse.player.data.ErrorDispatcher
import com.pulse.player.data.backup.common.LimitedStream
import com.pulse.player.data.db.BookmarkEntity
import com.pulse.player.data.db.LyricsEntity
import com.pulse.player.data.db.PulseDb
import com.pulse.player.data.lyrics.Lrc
import com.pulse.player.data.lyrics.LrcLine
import com.pulse.player.data.lyrics.LrcParser
import com.pulse.player.ui.components.formatTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

data class ResolvedLyrics(
    val lines: List<LrcLine> = emptyList(),
    val sourceLabel: String? = null
) {
    val isSynced: Boolean
        get() = lines.any { it.timeMs >= 0L }
}

class LyricsBookmarkCoordinator(
    private val ctx: Context,
    private val db: PulseDb,
    private val scope: CoroutineScope,
    private val onSnackbar: (String) -> Unit
) {
    private val refreshTick = MutableStateFlow(0)

    fun lyrics(trackId: String?): Flow<LyricsEntity?> =
        if (trackId == null) flowOf(null) else db.lyrics().observe(trackId)

    fun observeResolvedLyrics(trackId: String?, dataPath: String?): Flow<ResolvedLyrics> {
        if (trackId.isNullOrBlank()) return flowOf(ResolvedLyrics())
        return combine(db.lyrics().observe(trackId), refreshTick) { entity, _ ->
            if (entity != null && entity.text.isNotBlank()) {
                val parsed = parseFlexibleLines(entity.text)
                if (parsed.isNotEmpty()) {
                    val label = if (parsed.any { it.timeMs >= 0L }) "Synced" else "Imported"
                    return@combine ResolvedLyrics(parsed, label)
                }
            }

            val localFile = localLrcFile(trackId)
            if (localFile.exists() && localFile.length() > 0L) {
                val text = runCatching { localFile.readText() }.getOrNull().orEmpty()
                val parsed = parseFlexibleLines(text)
                if (parsed.isNotEmpty()) {
                    return@combine ResolvedLyrics(parsed, "Local .lrc")
                }
            }

            val resolvedPath = dataPath?.takeIf { it.isNotBlank() }
                ?: runCatching { db.tracks().byId(trackId)?.dataPath }.getOrNull()
            val sidecar = Lrc.discoverSidecarLrc(resolvedPath)
            if (sidecar != null) {
                val text = runCatching { sidecar.readText() }.getOrNull().orEmpty()
                val parsed = parseFlexibleLines(text)
                if (parsed.isNotEmpty()) {
                    return@combine ResolvedLyrics(parsed, "Sidecar .lrc")
                }
            }

            ResolvedLyrics()
        }.flowOn(Dispatchers.IO)
    }

    fun importLyrics(trackId: String, uri: Uri) = scope.launch(Dispatchers.IO) {
        runCatching {
            val stream = ctx.contentResolver.openInputStream(uri) ?: return@runCatching
            val text = LimitedStream(stream, 512 * 1024L).reader().use { it.readText() }
            if (text.contains('\u0000')) {
                withContext(Dispatchers.Main) { onSnackbar("Couldn't read that file") }
                return@runCatching
            }
            val parsed = LrcParser.parse(text)
            db.lyrics().put(
                LyricsEntity(
                    trackId = trackId,
                    text = text,
                    synced = parsed != null,
                    importedAtMs = System.currentTimeMillis()
                )
            )
            localLrcFile(trackId).apply {
                parentFile?.mkdirs()
                writeText(text)
            }
            refreshTick.value += 1
            withContext(Dispatchers.Main) {
                onSnackbar(if (parsed != null) "Imported synced .lrc (${parsed.size} lines)" else "Imported plain lyrics")
            }
        }.onFailure { err ->
            ErrorDispatcher.post(
                category = ErrorCategory.STORAGE,
                title = "Lyrics Import Failed",
                message = "Couldn't read that lyrics file.",
                technicalDetails = err.stackTraceToString()
            )
            withContext(Dispatchers.Main) { onSnackbar("Couldn't read that file") }
        }
    }

    fun saveSyncedLyrics(trackId: String, lines: List<LrcLine>) = scope.launch(Dispatchers.IO) {
        if (trackId.isBlank() || lines.isEmpty()) return@launch
        runCatching {
            val serialized = Lrc.serialize(lines)
            val hasTimestamps = lines.any { it.timeMs >= 0L }
            db.lyrics().put(
                LyricsEntity(
                    trackId = trackId,
                    text = serialized,
                    synced = hasTimestamps,
                    importedAtMs = System.currentTimeMillis()
                )
            )
            localLrcFile(trackId).apply {
                parentFile?.mkdirs()
                writeText(serialized)
            }
            refreshTick.value += 1
            withContext(Dispatchers.Main) { onSnackbar("Saved synced lyrics") }
        }.onFailure { err ->
            ErrorDispatcher.post(
                category = ErrorCategory.STORAGE,
                title = "Failed to Save Lyrics",
                message = err.message ?: "Unable to write synced lyrics.",
                technicalDetails = err.stackTraceToString()
            )
            withContext(Dispatchers.Main) { onSnackbar("Couldn't save lyrics") }
        }
    }

    fun saveLyricsForTrack(trackId: String, lyricsText: String): Boolean {
        return runCatching {
            localLrcFile(trackId).apply {
                parentFile?.mkdirs()
                writeText(lyricsText)
            }
            val parsed = LrcParser.parse(lyricsText)
            scope.launch(Dispatchers.IO) {
                db.lyrics().put(
                    LyricsEntity(
                        trackId = trackId,
                        text = lyricsText,
                        synced = parsed != null,
                        importedAtMs = System.currentTimeMillis()
                    )
                )
                refreshTick.value += 1
            }
            true
        }.getOrDefault(false)
    }

    fun deleteLyrics(trackId: String) = scope.launch(Dispatchers.IO) {
        db.lyrics().remove(trackId)
        runCatching { localLrcFile(trackId).delete() }
        refreshTick.value += 1
        withContext(Dispatchers.Main) { onSnackbar("Cleared lyrics for track") }
    }

    // Bookmarks
    fun bookmarksFor(trackId: String?): Flow<List<BookmarkEntity>> =
        if (trackId.isNullOrBlank()) flowOf(emptyList()) else db.bookmarks().forTrack(trackId)

    fun addBookmark(trackId: String, positionMs: Long, label: String = "") = scope.launch(Dispatchers.IO) {
        val safePos = positionMs.coerceAtLeast(0L)
        val cleanLabel = label.trim().ifBlank { "Moment @ ${formatTime(safePos)}" }
        db.bookmarks().add(
            BookmarkEntity(
                trackId = trackId,
                positionMs = safePos,
                label = cleanLabel,
                createdAtMs = System.currentTimeMillis()
            )
        )
        withContext(Dispatchers.Main) {
            onSnackbar("Saved bookmark at ${formatTime(safePos)}")
        }
    }

    fun deleteBookmark(id: Long) = scope.launch(Dispatchers.IO) {
        db.bookmarks().remove(id)
    }

    private fun localLrcFile(trackId: String): File {
        val dir = File(ctx.filesDir, "lyrics")
        val safeId = trackId.replace(Regex("[^a-zA-Z0-9._-]"), "_")
        return File(dir, "$safeId.lrc")
    }

    private fun parseFlexibleLines(text: String): List<LrcLine> {
        val parsed = Lrc.parse(text)
        if (parsed.isNotEmpty()) return parsed
        return text.lineSequence()
            .map { it.trim() }
            .filter { it.isNotBlank() && !it.startsWith("[ar:", true) && !it.startsWith("[ti:", true) && !it.startsWith("[al:", true) }
            .map { LrcLine(-1L, it) }
            .toList()
    }
}
