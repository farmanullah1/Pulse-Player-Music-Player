package com.pulse.player.data.backup

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.pulse.player.data.db.FavoriteEntity
import com.pulse.player.data.db.PlayEventEntity
import com.pulse.player.data.db.PlaylistEntity
import com.pulse.player.data.db.PlaylistTrackEntity
import com.pulse.player.data.db.PulseDb
import com.pulse.player.data.db.TrackEntity
import com.pulse.player.data.settings.SettingsStore
import com.pulse.player.data.settings.ThemeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipFile

class ArchiveRestoreService(
    private val context: Context,
    private val db: PulseDb,
    private val settings: SettingsStore
) {

    suspend fun inspect(uri: Uri, fileName: String): Result<RestorePreview> = withContext(Dispatchers.IO) {
        val tempFile = File.createTempFile("pulse_inspect_", ".tmp", context.cacheDir)
        try {
            val inStream = context.contentResolver.openInputStream(uri)
                ?: return@withContext Result.failure(IllegalStateException("Could not open selected file"))

            inStream.use { input ->
                FileOutputStream(tempFile).use { output ->
                    input.copyTo(output)
                }
            }

            // Sniff signature
            val headerBytes = ByteArray(16)
            FileInputStream(tempFile).use { it.read(headerBytes) }
            val detection = ArchiveValidator.detectSignature(headerBytes)

            when (detection) {
                is ArchiveValidator.DetectionResult.GenuineWavAudio -> {
                    return@withContext Result.failure(
                        IllegalArgumentException("The selected file is a genuine WAV audio file, not a backup archive.")
                    )
                }
                is ArchiveValidator.DetectionResult.Unknown -> {
                    return@withContext Result.failure(
                        IllegalArgumentException("The selected file is not a valid ZIP or .musicbackup archive (${detection.reason}).")
                    )
                }
                is ArchiveValidator.DetectionResult.ValidZipArchive -> {
                    // Valid ZIP container detected! Proceed with entry inspection
                }
            }

            var totalUncompressedBytes = 0L
            var entryCount = 0
            var manifestText: String? = null
            var checksumsText: String? = null
            val warnings = mutableListOf<String>()

            ZipFile(tempFile).use { zf ->
                val entries = zf.entries()
                while (entries.hasMoreElements()) {
                    val entry = entries.nextElement()
                    entryCount++
                    if (entryCount > ArchiveValidator.MAX_ENTRY_COUNT) {
                        return@withContext Result.failure(
                            SecurityException("Archive exceeds maximum allowed entry count (${ArchiveValidator.MAX_ENTRY_COUNT})")
                        )
                    }

                    val sanitized = ArchiveValidator.sanitizeEntryName(entry.name)
                        ?: return@withContext Result.failure(
                            SecurityException("Archive contains malicious or unsafe entry path: ${entry.name}")
                        )

                    if (entry.size > ArchiveValidator.MAX_ENTRY_SIZE_BYTES) {
                        return@withContext Result.failure(
                            SecurityException("Entry $sanitized exceeds maximum allowed size (25MB)")
                        )
                    }

                    if (!entry.isDirectory) {
                        totalUncompressedBytes += entry.size
                        if (totalUncompressedBytes > ArchiveValidator.MAX_TOTAL_UNCOMPRESSED_BYTES) {
                            return@withContext Result.failure(
                                SecurityException("Total archive size exceeds limit (100MB)")
                            )
                        }

                        if (sanitized == "manifest.json" || sanitized == "metadata.inf") {
                            manifestText = zf.getInputStream(entry).bufferedReader().readText()
                        } else if (sanitized == "checksums.json") {
                            checksumsText = zf.getInputStream(entry).bufferedReader().readText()
                        }
                    }
                }

                // Verify Checksums if checksums.json is present
                var checksumsVerified = false
                if (checksumsText != null) {
                    val chkObj = runCatching { JSONObject(checksumsText).getJSONObject("checksums") }.getOrNull()
                    if (chkObj != null) {
                        val keys = chkObj.keys()
                        var allOk = true
                        while (keys.hasNext()) {
                            val key = keys.next()
                            val expectedDigest = chkObj.getString(key)
                            val zipEntry = zf.getEntry(key)
                            if (zipEntry != null) {
                                val actualDigest = zf.getInputStream(zipEntry).use { isStream -> ArchiveValidator.sha256Hex(isStream) }
                                if (!expectedDigest.equals(actualDigest, ignoreCase = true)) {
                                    allOk = false
                                    warnings += "Integrity mismatch for $key"
                                }
                            }
                        }
                        checksumsVerified = allOk
                    }
                }

                // Parse or synthesize Manifest
                val manifest: ArchiveManifest
                if (manifestText != null) {
                    val root = JSONObject(manifestText)
                    val countsObj = root.optJSONObject("counts")
                    val countsMap = mutableMapOf<String, Long>()
                    countsObj?.keys()?.forEach { k -> countsMap[k] = countsObj.optLong(k, 0L) }

                    val catObj = root.optJSONObject("categories")
                    val catMap = mutableMapOf<String, ArchiveCategoryInfo>()
                    catObj?.keys()?.forEach { k ->
                        val c = catObj.optJSONObject(k)
                        if (c != null) {
                            catMap[k] = ArchiveCategoryInfo(
                                included = c.optBoolean("included", false),
                                path = c.optString("path", null),
                                recordCount = c.optLong("recordCount", 0L),
                                required = c.optBoolean("required", false)
                            )
                        }
                    }

                    manifest = ArchiveManifest(
                        formatId = root.optString("formatId", ArchiveManifest.FORMAT_ID),
                        formatVersion = root.optInt("formatVersion", ArchiveManifest.CURRENT_VERSION),
                        appName = root.optString("appName", "Pulse Player"),
                        createdAtUtc = root.optString("createdAtUtc", root.optString("backupTime", "Unknown")),
                        backupMode = root.optString("backupMode", "MANUAL_GLOBAL"),
                        backupScope = root.optString("backupScope", "GLOBAL"),
                        containsAudioFiles = root.optBoolean("containsAudioFiles", false),
                        counts = countsMap,
                        categories = catMap,
                        warnings = warnings
                    )
                } else {
                    // Fallback for archives without manifest: discover present entries
                    val catMap = mutableMapOf<String, ArchiveCategoryInfo>()
                    for (cat in BackupCategory.values()) {
                        val present = zf.getEntry(cat.internalPath) != null
                        catMap[cat.id] = ArchiveCategoryInfo(included = present, path = cat.internalPath)
                    }
                    manifest = ArchiveManifest(
                        createdAtUtc = "Discovered Archive",
                        counts = emptyMap(),
                        categories = catMap,
                        warnings = listOf("No manifest.json found; structure discovered from archive entries.")
                    )
                }

                val preview = RestorePreview(
                    fileName = fileName,
                    manifest = manifest,
                    isValid = warnings.none { it.startsWith("Integrity mismatch") },
                    validationMessage = if (warnings.isEmpty()) "Archive integrity and structure validated successfully."
                    else "Archive contains warnings: ${warnings.joinToString("; ")}",
                    detectedFormat = "Pulse Player Portable Archive (.musicbackup / ZIP)",
                    checksumsVerified = checksumsVerified,
                    totalUncompressedBytes = totalUncompressedBytes,
                    entryCount = entryCount,
                    warnings = warnings
                )

                Result.success(preview)
            }
        } catch (t: Throwable) {
            Result.failure(t)
        } finally {
            if (tempFile.exists()) tempFile.delete()
        }
    }

    suspend fun restore(
        uri: Uri,
        fileName: String,
        selectedCategories: Set<BackupCategory>,
        conflictMode: RestoreConflictMode,
        onProgress: (stage: String, fraction: Float) -> Unit = { _, _ -> }
    ): Result<RestoreReport> = withContext(Dispatchers.IO) {
        val tempFile = File.createTempFile("pulse_restore_", ".tmp", context.cacheDir)
        try {
            onProgress("Reading archive file...", 0.05f)
            val inStream = context.contentResolver.openInputStream(uri)
                ?: return@withContext Result.failure(IllegalStateException("Could not open archive input stream"))

            inStream.use { input ->
                FileOutputStream(tempFile).use { output ->
                    input.copyTo(output)
                }
            }

            val imported = mutableMapOf<String, Int>()
            val updated = mutableMapOf<String, Int>()
            val skipped = mutableMapOf<String, Int>()
            val warnings = mutableListOf<String>()
            val errors = mutableListOf<String>()
            var unmatchedCount = 0

            ZipFile(tempFile).use { zf ->
                onProgress("Resolving media library tracks...", 0.20f)
                val existingTracks = db.tracks().allOnce()
                val resolver = TrackMatchResolver(existingTracks)

                db.withTransaction {
                    // 1. Favorites
                    if (BackupCategory.FAVORITES in selectedCategories) {
                        onProgress("Restoring Favourites...", 0.35f)
                        val favEntry = zf.getEntry(BackupCategory.FAVORITES.internalPath)
                        if (favEntry != null) {
                            if (conflictMode == RestoreConflictMode.REPLACE_SELECTED_CATEGORIES) {
                                db.favorites().clear()
                            }

                            val text = zf.getInputStream(favEntry).bufferedReader().readText()
                            val root = JSONObject(text)
                            val items = root.optJSONArray("items") ?: JSONArray()
                            var favImported = 0
                            var favSkipped = 0

                            for (i in 0 until items.length()) {
                                val item = items.optJSONObject(i) ?: continue
                                val ref = BackupTrackRef(
                                    id = item.optString("id", null),
                                    title = item.optString("title", ""),
                                    artist = item.optString("artist", ""),
                                    album = item.optString("album", ""),
                                    durationMs = item.optLong("durationMs", 0L),
                                    displayName = item.optString("displayName", ""),
                                    sizeBytes = item.optLong("sizeBytes", 0L)
                                )

                                when (val match = resolver.resolve(ref)) {
                                    is TrackMatch.Matched -> {
                                        val addedAt = item.optLong("addedAtMs", System.currentTimeMillis())
                                        db.favorites().add(FavoriteEntity(match.target.id, addedAt))
                                        favImported++
                                    }
                                    is TrackMatch.Ambiguous,
                                    is TrackMatch.Unmatched -> {
                                        favSkipped++
                                        unmatchedCount++
                                    }
                                }
                            }
                            imported[BackupCategory.FAVORITES.displayName] = favImported
                            skipped[BackupCategory.FAVORITES.displayName] = favSkipped
                        }
                    }

                    // 2. Playlists
                    if (BackupCategory.PLAYLISTS in selectedCategories) {
                        onProgress("Restoring Playlists...", 0.55f)
                        val playlistEntry = zf.getEntry(BackupCategory.PLAYLISTS.internalPath)
                        if (playlistEntry != null) {
                            if (conflictMode == RestoreConflictMode.REPLACE_SELECTED_CATEGORIES) {
                                db.playlists().clearAllPlaylists()
                                db.playlists().clearAllPlaylistTracks()
                            }

                            val text = zf.getInputStream(playlistEntry).bufferedReader().readText()
                            val root = JSONObject(text)
                            val playlists = root.optJSONArray("playlists") ?: JSONArray()
                            var plImported = 0
                            var plSkipped = 0

                            for (i in 0 until playlists.length()) {
                                val plObj = playlists.optJSONObject(i) ?: continue
                                val name = plObj.optString("name", "Playlist").trim()
                                val createdAt = plObj.optLong("createdAtMs", System.currentTimeMillis())

                                val newPlId = db.playlists().create(PlaylistEntity(name = name, createdAtMs = createdAt))
                                plImported++

                                val trackArr = plObj.optJSONArray("tracks") ?: JSONArray()
                                for (j in 0 until trackArr.length()) {
                                    val tObj = trackArr.optJSONObject(j) ?: continue
                                    val ref = BackupTrackRef(
                                        id = tObj.optString("trackId", null),
                                        title = tObj.optString("title", ""),
                                        artist = tObj.optString("artist", ""),
                                        album = tObj.optString("album", ""),
                                        durationMs = tObj.optLong("durationMs", 0L),
                                        displayName = tObj.optString("displayName", ""),
                                        sizeBytes = tObj.optLong("sizeBytes", 0L)
                                    )

                                    when (val match = resolver.resolve(ref)) {
                                        is TrackMatch.Matched -> {
                                            val pos = tObj.optInt("position", j)
                                            val addedAt = tObj.optLong("addedAtMs", System.currentTimeMillis())
                                            db.playlists().addTrack(
                                                PlaylistTrackEntity(
                                                    playlistId = newPlId,
                                                    trackId = match.target.id,
                                                    position = pos,
                                                    addedAtMs = addedAt
                                                )
                                            )
                                        }
                                        else -> {
                                            plSkipped++
                                            unmatchedCount++
                                        }
                                    }
                                }
                            }
                            imported[BackupCategory.PLAYLISTS.displayName] = plImported
                            skipped[BackupCategory.PLAYLISTS.displayName] = plSkipped
                        }
                    }

                    // 3. Listening Statistics
                    if (BackupCategory.STATISTICS in selectedCategories) {
                        onProgress("Restoring Listening Statistics...", 0.75f)
                        val statsEntry = zf.getEntry(BackupCategory.STATISTICS.internalPath)
                        if (statsEntry != null) {
                            if (conflictMode == RestoreConflictMode.REPLACE_SELECTED_CATEGORIES) {
                                db.stats().clearAllEvents()
                            }

                            val text = zf.getInputStream(statsEntry).bufferedReader().readText()
                            val root = JSONObject(text)
                            val eventsArr = root.optJSONArray("events") ?: JSONArray()
                            val toInsert = mutableListOf<PlayEventEntity>()

                            for (i in 0 until eventsArr.length()) {
                                val evObj = eventsArr.optJSONObject(i) ?: continue
                                val refId = evObj.optString("trackId", "")
                                val match = resolver.resolve(BackupTrackRef(id = refId, title = ""))
                                val targetTrackId = if (match is TrackMatch.Matched) match.target.id else refId

                                toInsert.add(
                                    PlayEventEntity(
                                        trackId = targetTrackId,
                                        playedAtMs = evObj.optLong("playedAtMs", 0L),
                                        listenedMs = evObj.optLong("listenedMs", 0L),
                                        completed = evObj.optBoolean("completed", true)
                                    )
                                )
                            }
                            db.stats().insertAll(toInsert)
                            imported[BackupCategory.STATISTICS.displayName] = toInsert.size
                        }
                    }

                    // 4. Settings
                    if (BackupCategory.SETTINGS in selectedCategories) {
                        onProgress("Restoring Settings...", 0.90f)
                        val settingsEntry = zf.getEntry(BackupCategory.SETTINGS.internalPath)
                        if (settingsEntry != null) {
                            val text = zf.getInputStream(settingsEntry).bufferedReader().readText()
                            val root = JSONObject(text)

                            root.optString("themeMode", "").takeIf { it.isNotBlank() }?.let {
                                runCatching { ThemeMode.valueOf(it) }.getOrNull()?.let { m -> settings.setTheme(m) }
                            }
                            root.optString("skinId", "").takeIf { it.isNotBlank() }?.let { settings.setSkin(it) }
                            root.optString("playerThemeId", "").takeIf { it.isNotBlank() }?.let { settings.setPlayerTheme(it) }
                            if (root.has("reducedMotion")) settings.setReduced(root.optBoolean("reducedMotion", false))
                            if (root.has("fadeEnabled")) settings.setFade(root.optBoolean("fadeEnabled", true))
                            if (root.has("minDurationSec")) settings.setMinDuration(root.optInt("minDurationSec", 30))

                            imported[BackupCategory.SETTINGS.displayName] = 1
                        }
                    }
                } // End of Transaction
            }

            val totalRestored = imported.values.sum()
            onProgress("Restore complete!", 1.0f)
            val report = RestoreReport(
                success = true,
                fileName = fileName,
                conflictMode = conflictMode,
                importedCount = imported,
                updatedCount = updated,
                skippedCount = skipped,
                unmatchedTracks = unmatchedCount,
                totalRestored = totalRestored,
                warnings = warnings,
                errors = errors
            )
            Result.success(report)
        } catch (t: Throwable) {
            Result.failure(t)
        } finally {
            if (tempFile.exists()) tempFile.delete()
        }
    }
}
