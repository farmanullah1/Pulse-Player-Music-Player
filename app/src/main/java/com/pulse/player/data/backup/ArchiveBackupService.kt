package com.pulse.player.data.backup

import android.content.Context
import android.net.Uri
import com.pulse.player.data.db.PulseDb
import com.pulse.player.data.settings.SettingsStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.time.Instant
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

class ArchiveBackupService(
    private val context: Context,
    private val db: PulseDb,
    private val settings: SettingsStore
) {

    suspend fun createArchive(
        targetUri: Uri,
        selectedCategories: Set<BackupCategory> = BackupCategory.values().toSet(),
        onProgress: (stage: String, fraction: Float) -> Unit = { _, _ -> }
    ): Result<LastBackupRecord> = withContext(Dispatchers.IO) {
        val tempZip = File.createTempFile("pulse_backup_export_", ".tmp", context.cacheDir)
        try {
            onProgress("Collecting metadata and library data...", 0.1f)
            val counts = mutableMapOf<String, Long>()
            val categoryInfos = mutableMapOf<String, ArchiveCategoryInfo>()
            val checksumMap = mutableMapOf<String, String>()
            val warnings = mutableListOf<String>()

            val fos = FileOutputStream(tempZip)
            val zos = ZipOutputStream(BufferedOutputStream(fos))

            // Helper to write an entry and track checksum
            fun writeJsonEntry(path: String, jsonString: String) {
                val bytes = jsonString.toByteArray(Charsets.UTF_8)
                val checksum = ArchiveValidator.sha256Hex(bytes)
                checksumMap[path] = checksum

                val entry = ZipEntry(path)
                entry.size = bytes.size.toLong()
                entry.time = System.currentTimeMillis()
                zos.putNextEntry(entry)
                zos.write(bytes)
                zos.closeEntry()
            }

            // 1. Favorites
            if (BackupCategory.FAVORITES in selectedCategories) {
                onProgress("Exporting Favourites...", 0.25f)
                val favTracks = db.favorites().allTracksOnce()
                val addedAtList = db.favorites().addedAtList().associate { it.trackId to it.addedAtMs }
                val itemsArr = JSONArray()
                favTracks.forEach { t ->
                    itemsArr.put(JSONObject().apply {
                        put("id", t.id)
                        put("title", t.title)
                        put("artist", t.artist)
                        put("album", t.album)
                        put("durationMs", t.durationMs)
                        put("displayName", t.displayName)
                        put("mimeType", t.mimeType ?: JSONObject.NULL)
                        put("sizeBytes", t.sizeBytes)
                        put("addedAtMs", addedAtList[t.id] ?: 0L)
                        put("normTitle", t.normTitle)
                        put("normArtist", t.normArtist)
                        put("normAlbum", t.normAlbum)
                    })
                }
                val favJson = JSONObject().apply {
                    put("schema", "pulseplayer.favorites")
                    put("version", 1)
                    put("count", favTracks.size)
                    put("items", itemsArr)
                }.toString(2)

                writeJsonEntry(BackupCategory.FAVORITES.internalPath, favJson)
                counts["favorites"] = favTracks.size.toLong()
                categoryInfos[BackupCategory.FAVORITES.id] = ArchiveCategoryInfo(
                    included = true,
                    path = BackupCategory.FAVORITES.internalPath,
                    recordCount = favTracks.size.toLong()
                )
            } else {
                categoryInfos[BackupCategory.FAVORITES.id] = ArchiveCategoryInfo(included = false)
            }

            // 2. Playlists
            if (BackupCategory.PLAYLISTS in selectedCategories) {
                onProgress("Exporting Playlists...", 0.40f)
                val allPlaylists = db.playlists().allPlaylistsOnce()
                val playlistArr = JSONArray()
                var totalTracksInPlaylists = 0L

                for (p in allPlaylists) {
                    val tracks = db.playlists().tracksForPlaylist(p.id)
                    totalTracksInPlaylists += tracks.size
                    val trackArr = JSONArray()
                    val trackIds = tracks.map { it.trackId }
                    val trackEntities = db.tracks().byIds(trackIds).associateBy { it.id }

                    tracks.forEach { pt ->
                        val ent = trackEntities[pt.trackId]
                        trackArr.put(JSONObject().apply {
                            put("trackId", pt.trackId)
                            put("position", pt.position)
                            put("addedAtMs", pt.addedAtMs)
                            if (ent != null) {
                                put("title", ent.title)
                                put("artist", ent.artist)
                                put("album", ent.album)
                                put("durationMs", ent.durationMs)
                                put("displayName", ent.displayName)
                                put("sizeBytes", ent.sizeBytes)
                            }
                        })
                    }

                    playlistArr.put(JSONObject().apply {
                        put("id", p.id)
                        put("name", p.name)
                        put("createdAtMs", p.createdAtMs)
                        put("tracks", trackArr)
                    })
                }

                val playlistJson = JSONObject().apply {
                    put("schema", "pulseplayer.playlists")
                    put("version", 1)
                    put("count", allPlaylists.size)
                    put("playlists", playlistArr)
                }.toString(2)

                writeJsonEntry(BackupCategory.PLAYLISTS.internalPath, playlistJson)
                counts["playlists"] = allPlaylists.size.toLong()
                categoryInfos[BackupCategory.PLAYLISTS.id] = ArchiveCategoryInfo(
                    included = true,
                    path = BackupCategory.PLAYLISTS.internalPath,
                    recordCount = allPlaylists.size.toLong()
                )
            } else {
                categoryInfos[BackupCategory.PLAYLISTS.id] = ArchiveCategoryInfo(included = false)
            }

            // 3. Listening Statistics
            if (BackupCategory.STATISTICS in selectedCategories) {
                onProgress("Exporting Listening Statistics...", 0.55f)
                val recentEvents = db.stats().getRecentEvents(1000)
                val eventsArr = JSONArray()
                recentEvents.forEach { ev ->
                    eventsArr.put(JSONObject().apply {
                        put("trackId", ev.trackId)
                        put("playedAtMs", ev.playedAtMs)
                        put("listenedMs", ev.listenedMs)
                        put("completed", ev.completed)
                    })
                }

                val statsJson = JSONObject().apply {
                    put("schema", "pulseplayer.statistics")
                    put("version", 1)
                    put("count", recentEvents.size)
                    put("events", eventsArr)
                }.toString(2)

                writeJsonEntry(BackupCategory.STATISTICS.internalPath, statsJson)
                counts["statistics"] = recentEvents.size.toLong()
                categoryInfos[BackupCategory.STATISTICS.id] = ArchiveCategoryInfo(
                    included = true,
                    path = BackupCategory.STATISTICS.internalPath,
                    recordCount = recentEvents.size.toLong()
                )
            } else {
                categoryInfos[BackupCategory.STATISTICS.id] = ArchiveCategoryInfo(included = false)
            }

            // 4. Settings
            if (BackupCategory.SETTINGS in selectedCategories) {
                onProgress("Exporting Settings...", 0.70f)
                val themeMode = settings.themeMode.first()
                val skinId = settings.skinId.first()
                val playerThemeId = settings.playerThemeId.first()
                val reduced = settings.reducedMotion.first()
                val fade = settings.fadeEnabled.first()
                val minDur = settings.minDurationSec.first()
                val safTrees = settings.safTrees.first()

                val settingsJson = JSONObject().apply {
                    put("schema", "pulseplayer.settings")
                    put("version", 1)
                    put("themeMode", themeMode.name)
                    put("skinId", skinId)
                    put("playerThemeId", playerThemeId)
                    put("reducedMotion", reduced)
                    put("fadeEnabled", fade)
                    put("minDurationSec", minDur)
                    put("safTrees", JSONArray(safTrees))
                }.toString(2)

                writeJsonEntry(BackupCategory.SETTINGS.internalPath, settingsJson)
                counts["settings"] = 1L
                categoryInfos[BackupCategory.SETTINGS.id] = ArchiveCategoryInfo(
                    included = true,
                    path = BackupCategory.SETTINGS.internalPath,
                    recordCount = 1L
                )
            } else {
                categoryInfos[BackupCategory.SETTINGS.id] = ArchiveCategoryInfo(included = false)
            }

            // 5. Music Tags / Track Metadata
            if (BackupCategory.MUSIC_TAGS in selectedCategories) {
                onProgress("Exporting Music Metadata Tags...", 0.80f)
                val allTracks = db.tracks().allOnce()
                val trackArr = JSONArray()
                allTracks.forEach { t ->
                    trackArr.put(JSONObject().apply {
                        put("id", t.id)
                        put("title", t.title)
                        put("artist", t.artist)
                        put("album", t.album)
                        put("albumArtist", t.albumArtist ?: JSONObject.NULL)
                        put("durationMs", t.durationMs)
                        put("sizeBytes", t.sizeBytes)
                        put("displayName", t.displayName)
                        put("trackNumber", t.trackNumber)
                        put("year", t.year)
                        put("folderPath", t.folderPath)
                    })
                }

                val tagsJson = JSONObject().apply {
                    put("schema", "pulseplayer.music_tags")
                    put("version", 1)
                    put("count", allTracks.size)
                    put("tracks", trackArr)
                }.toString(2)

                writeJsonEntry(BackupCategory.MUSIC_TAGS.internalPath, tagsJson)
                counts["tracks"] = allTracks.size.toLong()
                categoryInfos[BackupCategory.MUSIC_TAGS.id] = ArchiveCategoryInfo(
                    included = true,
                    path = BackupCategory.MUSIC_TAGS.internalPath,
                    recordCount = allTracks.size.toLong()
                )
            } else {
                categoryInfos[BackupCategory.MUSIC_TAGS.id] = ArchiveCategoryInfo(included = false)
            }

            // 6. Write Checksums
            val checksumsJson = JSONObject().apply {
                val obj = JSONObject()
                checksumMap.forEach { (path, hash) -> obj.put(path, hash) }
                put("checksums", obj)
            }.toString(2)

            val chkBytes = checksumsJson.toByteArray(Charsets.UTF_8)
            val chkEntry = ZipEntry("checksums.json")
            chkEntry.size = chkBytes.size.toLong()
            chkEntry.time = System.currentTimeMillis()
            zos.putNextEntry(chkEntry)
            zos.write(chkBytes)
            zos.closeEntry()

            // 7. Write Manifest
            val isGlobal = selectedCategories.size == BackupCategory.values().size
            val manifest = ArchiveManifest(
                createdAtUtc = Instant.now().toString(),
                backupMode = if (isGlobal) "MANUAL_GLOBAL" else "MANUAL_PARTIAL",
                backupScope = if (isGlobal) "GLOBAL" else "SELECTED_CATEGORIES",
                containsAudioFiles = false,
                counts = counts,
                categories = categoryInfos,
                warnings = warnings
            )

            val manifestJson = JSONObject().apply {
                put("formatId", manifest.formatId)
                put("formatVersion", manifest.formatVersion)
                put("appName", manifest.appName)
                put("appVersionName", manifest.appVersionName)
                put("appVersionCode", manifest.appVersionCode)
                put("createdAtUtc", manifest.createdAtUtc)
                put("backupMode", manifest.backupMode)
                put("backupScope", manifest.backupScope)
                put("containsAudioFiles", manifest.containsAudioFiles)
                put("deviceLabel", manifest.deviceLabel)

                val countsObj = JSONObject()
                manifest.counts.forEach { (k, v) -> countsObj.put(k, v) }
                put("counts", countsObj)

                val catObj = JSONObject()
                manifest.categories.forEach { (k, v) ->
                    catObj.put(k, JSONObject().apply {
                        put("included", v.included)
                        put("path", v.path ?: JSONObject.NULL)
                        put("recordCount", v.recordCount)
                        put("required", v.required)
                    })
                }
                put("categories", catObj)
                put("warnings", JSONArray(manifest.warnings))
            }.toString(2)

            val manBytes = manifestJson.toByteArray(Charsets.UTF_8)
            val manEntry = ZipEntry("manifest.json")
            manEntry.size = manBytes.size.toLong()
            manEntry.time = System.currentTimeMillis()
            zos.putNextEntry(manEntry)
            zos.write(manBytes)
            zos.closeEntry()

            zos.finish()
            zos.flush()
            zos.close()

            // 8. Self-verification of created archive
            onProgress("Verifying archive integrity and checksums...", 0.90f)
            val headerBytes = ByteArray(16)
            FileInputStream(tempZip).use { fis ->
                fis.read(headerBytes)
            }
            val detect = ArchiveValidator.detectSignature(headerBytes)
            if (detect !is ArchiveValidator.DetectionResult.ValidZipArchive) {
                return@withContext Result.failure(IllegalStateException("Archive generated has invalid signature."))
            }

            // Verify zip entries and checksums
            ZipFile(tempZip).use { zf ->
                val chkZipEntry = zf.getEntry("checksums.json") ?: return@withContext Result.failure(IllegalStateException("Missing checksums.json in archive"))
                val chkContent = zf.getInputStream(chkZipEntry).bufferedReader().readText()
                val chkObj = JSONObject(chkContent).getJSONObject("checksums")

                val keys = chkObj.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    val expectedDigest = chkObj.getString(key)
                    val entry = zf.getEntry(key) ?: return@withContext Result.failure(IllegalStateException("Payload file $key missing"))
                    val actualDigest = zf.getInputStream(entry).use { isStream -> ArchiveValidator.sha256Hex(isStream) }
                    if (!expectedDigest.equals(actualDigest, ignoreCase = true)) {
                        return@withContext Result.failure(IllegalStateException("Integrity checksum mismatch for $key"))
                    }
                }
            }

            // 9. Stream to targetUri via ContentResolver
            onProgress("Writing backup to destination...", 0.95f)
            val outStream = context.contentResolver.openOutputStream(targetUri)
                ?: return@withContext Result.failure(IllegalStateException("Could not open output destination"))

            outStream.use { os ->
                FileInputStream(tempZip).use { fis ->
                    fis.copyTo(os)
                }
                os.flush()
            }

            val fileName = targetUri.lastPathSegment?.substringAfterLast('/') ?: "PulsePlayer_Backup.musicbackup"
            val record = LastBackupRecord(
                timestampMs = System.currentTimeMillis(),
                fileName = fileName,
                fileSizeBytes = tempZip.length(),
                recordCounts = counts
            )
            onProgress("Backup completed successfully!", 1.0f)
            Result.success(record)
        } catch (t: Throwable) {
            Result.failure(t)
        } finally {
            if (tempZip.exists()) {
                tempZip.delete()
            }
        }
    }
}
