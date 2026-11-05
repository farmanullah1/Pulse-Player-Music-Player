package com.pulse.player.data.backup.classic

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import androidx.documentfile.provider.DocumentFile
import androidx.room.withTransaction
import com.pulse.player.data.backup.common.BackupKind
import com.pulse.player.data.backup.common.BackupSniffer
import com.pulse.player.data.backup.common.LimitedStream
import com.pulse.player.data.db.FavoriteEntity
import com.pulse.player.data.db.PulseDb
import com.pulse.player.data.db.TrackEntity
import com.pulse.player.data.scan.SafScanner
import com.pulse.player.data.scan.norm
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.time.Instant
import kotlin.math.abs

data class FavItem(
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val displayName: String,
    val mimeType: String?,
    val sizeBytes: Long,
    val addedAtMs: Long,
    val normTitle: String,
    val normArtist: String,
    val normAlbum: String
)

object FavoritesCodec {
    const val SCHEMA = "pulseplayer.favorites"
    const val VERSION = 1
    const val MAX_BYTES = 10 * 1024 * 1024L
    const val MAX_ITEMS = 50_000
    private const val MAX_STR = 500

    fun encode(tracks: List<TrackEntity>, addedAt: Map<String, Long>): String {
        val arr = JSONArray()
        tracks.forEach { t ->
            arr.put(JSONObject().apply {
                put("title", t.title)
                put("artist", t.artist)
                put("album", t.album)
                put("durationMs", t.durationMs)
                put("displayName", t.displayName)
                put("mimeType", t.mimeType ?: JSONObject.NULL)
                put("sizeBytes", t.sizeBytes)
                put("addedAtMs", addedAt[t.id] ?: 0L)
                put("normTitle", t.normTitle)
                put("normArtist", t.normArtist)
                put("normAlbum", t.normAlbum)
            })
        }
        return JSONObject().apply {
            put("schema", SCHEMA)
            put("schemaVersion", VERSION)
            put("app", "Pulse Player")
            put("exportedAt", Instant.now().toString())
            put("count", tracks.size)
            put("items", arr)
        }.toString(2)
    }

    sealed interface Decoded {
        data class Ok(val items: List<FavItem>, val exportedAt: String?, val skipped: Int) : Decoded
        data class Fail(val reason: String) : Decoded
    }

    fun decode(text: String): Decoded {
        val root = runCatching { JSONObject(text) }.getOrElse {
            return Decoded.Fail("This file is not valid JSON.")
        }
        if (root.optString("schema") != SCHEMA) {
            return Decoded.Fail("This is not a Pulse Player favorites file.")
        }
        if (root.optInt("schemaVersion", -1) != VERSION) {
            return Decoded.Fail("Unsupported file version. Update the app and try again.")
        }
        val arr = root.optJSONArray("items") ?: return Decoded.Fail("The file has no items list.")
        if (arr.length() > MAX_ITEMS) return Decoded.Fail("The file has too many items.")
        val out = ArrayList<FavItem>(arr.length())
        var skipped = 0
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i)
            if (o == null) {
                skipped++
                continue
            }
            val title = o.optString("title").take(MAX_STR)
            if (title.isBlank()) { skipped++; continue }
            val artist = o.optString("artist").take(MAX_STR)
            val album = o.optString("album").take(MAX_STR)
            out += FavItem(
                title = title,
                artist = artist,
                album = album,
                durationMs = o.optLong("durationMs", 0).coerceAtLeast(0),
                displayName = o.optString("displayName").take(MAX_STR),
                mimeType = o.optString("mimeType").takeIf { it.isNotBlank() && it != "null" },
                sizeBytes = o.optLong("sizeBytes", 0).coerceAtLeast(0),
                addedAtMs = o.optLong("addedAtMs", 0),
                normTitle = norm(title),
                normArtist = norm(artist),
                normAlbum = norm(album)
            )
        }
        return Decoded.Ok(out, root.optString("exportedAt").takeIf { it.isNotBlank() }, skipped)
    }
}

sealed interface MatchResult {
    data class Matched(val item: FavItem, val track: TrackEntity, val level: String) : MatchResult
    data class AlreadyFavorite(val item: FavItem, val track: TrackEntity) : MatchResult
    data class Ambiguous(val item: FavItem, val candidates: List<TrackEntity>) : MatchResult
    data class Missing(val item: FavItem) : MatchResult
    data class Duplicate(val item: FavItem) : MatchResult
}

class FavoritesMatcher(
    private val library: List<TrackEntity>,
    private val favoriteIds: Set<String>
) {
    private val byTitleArtist = library.groupBy { it.normTitle to it.normArtist }
    private val byTitle = library.groupBy { it.normTitle }

    fun match(items: List<FavItem>): List<MatchResult> {
        val seen = HashSet<String>()
        val out = ArrayList<MatchResult>(items.size)

        for (it in items) {
            val key = "${it.normTitle}|${it.normArtist}|${it.durationMs / 2000}"
            if (!seen.add(key)) {
                out += MatchResult.Duplicate(it)
                continue
            }

            // 1) strong: same file name + size + near duration
            val strong = library.filter { t ->
                t.sizeBytes == it.sizeBytes && it.sizeBytes > 0 &&
                        t.displayName.equals(it.displayName, true) &&
                        abs(t.durationMs - it.durationMs) <= 2000
            }

            // 2) high: title + artist + duration within 2s
            val ta = byTitleArtist[it.normTitle to it.normArtist].orEmpty()
            val high = ta.filter { t -> abs(t.durationMs - it.durationMs) <= 2000 }

            // 3) medium: title + album, duration within 5s
            val medium = byTitle[it.normTitle].orEmpty().filter { t ->
                t.normAlbum == it.normAlbum && abs(t.durationMs - it.durationMs) <= 5000
            }

            val (cands, level) = when {
                strong.size == 1 -> strong to "strong"
                high.size == 1 -> high to "high"
                high.isEmpty() && medium.size == 1 -> medium to "medium"
                else -> (strong.ifEmpty { high.ifEmpty { medium } }) to "multi"
            }

            out += when {
                cands.isEmpty() -> MatchResult.Missing(it)
                cands.size > 1 || level == "multi" -> MatchResult.Ambiguous(it, cands)
                cands[0].id in favoriteIds -> MatchResult.AlreadyFavorite(it, cands[0])
                else -> MatchResult.Matched(it, cands[0], level)
            }
        }
        return out
    }
}

class FavoritesRepository(private val ctx: Context, private val db: PulseDb) {
    companion object {
        const val PORTABLE_FOLDER_NAME = "Pulse Player"
        const val PORTABLE_JSON_NAME = "pulseplayer.favorites.json"
        private const val PORTABLE_RELATIVE_PATH = "Download/$PORTABLE_FOLDER_NAME/"
    }

    data class PortableExportResult(
        val favorites: Int,
        val copied: Int,
        val reused: Int,
        val failed: List<String>,
        val location: String
    )

    suspend fun export(uri: Uri): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val tracks = db.favorites().allTracksOnce()
            if (tracks.isEmpty()) error("You have no favourites to export yet.")
            val added = db.favorites().addedAtList().associate { it.trackId to it.addedAtMs }
            val json = FavoritesCodec.encode(tracks, added)
            ctx.contentResolver.openOutputStream(uri, "wt")!!.use {
                it.write(json.toByteArray(Charsets.UTF_8))
                it.flush()
            }
            tracks.size
        }
    }

    /** Creates the portable Download/Pulse Player backup without changing v1 JSON. */
    suspend fun exportToPortableFolder(
        customTreeUri: Uri? = null,
        onProgress: ((current: Int, total: Int, title: String, fraction: Float) -> Unit)? = null
    ): Result<PortableExportResult> = withContext(Dispatchers.IO) {
        runCatching {
            val tracks = db.favorites().allTracksOnce()
            if (tracks.isEmpty()) error("You have no favourites to back up yet.")
            val added = db.favorites().addedAtList().associate { it.trackId to it.addedAtMs }
            val json = FavoritesCodec.encode(tracks, added)
            if (customTreeUri != null) {
                exportToCustomSafTree(customTreeUri, tracks, json, onProgress)
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                exportToMediaStore(tracks, json, onProgress)
            } else {
                exportToLegacyDownloadFolder(tracks, json, onProgress)
            }
        }
    }

    private fun exportToCustomSafTree(
        treeUri: Uri,
        tracks: List<TrackEntity>,
        json: String,
        onProgress: ((Int, Int, String, Float) -> Unit)?
    ): PortableExportResult {
        val root = DocumentFile.fromTreeUri(ctx, treeUri)
            ?: error("Selected destination folder is not accessible.")
        // Write or overwrite pulseplayer.favorites.json
        val existingJson = root.findFile(PORTABLE_JSON_NAME)
        val jsonDoc = existingJson ?: root.createFile("application/json", PORTABLE_JSON_NAME)
            ?: error("Couldn't create $PORTABLE_JSON_NAME in the chosen folder.")
        ctx.contentResolver.openOutputStream(jsonDoc.uri, "wt")?.use { out ->
            out.write(json.toByteArray(Charsets.UTF_8))
            out.flush()
        } ?: error("Failed to write favorites metadata to destination folder.")

        var copied = 0
        var reused = 0
        val failed = mutableListOf<String>()

        tracks.forEachIndexed { index, track ->
            val fraction = (index.toFloat() / tracks.size.coerceAtLeast(1))
            val title = track.displayName.ifBlank { track.title }
            onProgress?.invoke(index + 1, tracks.size, title, fraction)
            try {
                val fileName = safeFileName(track.displayName.ifBlank { "${track.title}.mp3" })
                val existingDoc = root.findFile(fileName)
                if (existingDoc != null && track.sizeBytes > 0L && existingDoc.length() == track.sizeBytes) {
                    reused++
                } else {
                    val destDoc = existingDoc ?: root.createFile(track.mimeType ?: "audio/mpeg", fileName)
                        ?: error("Couldn't create file $fileName in folder")
                    ctx.contentResolver.openInputStream(Uri.parse(track.uri))?.use { input ->
                        ctx.contentResolver.openOutputStream(destDoc.uri, "wt")?.use { output ->
                            input.copyTo(output, 64 * 1024)
                            output.flush()
                        } ?: error("couldn't open destination stream")
                    } ?: error("source audio file unavailable")
                    copied++
                }
            } catch (e: Exception) {
                failed += "${track.displayName.ifBlank { track.title }}: ${e.message ?: "copy failed"}"
            }
        }
        onProgress?.invoke(tracks.size, tracks.size, "Complete", 1.0f)
        return PortableExportResult(tracks.size, copied, reused, failed, root.name ?: "Custom Folder")
    }

    private fun exportToMediaStore(
        tracks: List<TrackEntity>,
        json: String,
        onProgress: ((Int, Int, String, Float) -> Unit)?
    ): PortableExportResult {
        val resolver = ctx.contentResolver
        writeMediaStoreJson(resolver, json)
        var copied = 0
        var reused = 0
        val failed = mutableListOf<String>()
        tracks.forEachIndexed { index, track ->
            val fraction = (index.toFloat() / tracks.size.coerceAtLeast(1))
            val title = track.displayName.ifBlank { track.title }
            onProgress?.invoke(index + 1, tracks.size, title, fraction)
            try {
                when (copyAudioToMediaStore(resolver, track)) {
                    CopyOutcome.COPIED -> copied++
                    CopyOutcome.REUSED -> reused++
                }
            } catch (e: Exception) {
                failed += "${track.displayName.ifBlank { track.title }}: ${e.message ?: "copy failed"}"
            }
        }
        onProgress?.invoke(tracks.size, tracks.size, "Complete", 1.0f)
        return PortableExportResult(tracks.size, copied, reused, failed, "Download/$PORTABLE_FOLDER_NAME")
    }

    private fun exportToLegacyDownloadFolder(
        tracks: List<TrackEntity>,
        json: String,
        onProgress: ((Int, Int, String, Float) -> Unit)?
    ): PortableExportResult {
        if (ctx.checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            error("Storage write permission is required on Android 9 and older.")
        }
        val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), PORTABLE_FOLDER_NAME)
        if (!dir.exists() && !dir.mkdirs()) error("Couldn't create Download/$PORTABLE_FOLDER_NAME.")
        File(dir, PORTABLE_JSON_NAME).writeText(json, Charsets.UTF_8)

        var copied = 0
        var reused = 0
        val failed = mutableListOf<String>()
        tracks.forEachIndexed { index, track ->
            val fraction = (index.toFloat() / tracks.size.coerceAtLeast(1))
            val title = track.displayName.ifBlank { track.title }
            onProgress?.invoke(index + 1, tracks.size, title, fraction)
            try {
                val name = safeFileName(track.displayName.ifBlank { "${track.title}.mp3" })
                val destination = File(dir, name)
                if (destination.exists() && track.sizeBytes > 0 && destination.length() == track.sizeBytes) {
                    reused++
                } else {
                    ctx.contentResolver.openInputStream(Uri.parse(track.uri))?.use { input ->
                        FileOutputStream(destination).use { output -> input.copyTo(output, 64 * 1024) }
                    } ?: error("source file unavailable")
                    copied++
                }
            } catch (e: Exception) {
                failed += "${track.displayName.ifBlank { track.title }}: ${e.message ?: "copy failed"}"
            }
        }
        onProgress?.invoke(tracks.size, tracks.size, "Complete", 1.0f)
        return PortableExportResult(tracks.size, copied, reused, failed, dir.path)
    }

    private enum class CopyOutcome { COPIED, REUSED }

    private fun copyAudioToMediaStore(resolver: android.content.ContentResolver, track: TrackEntity): CopyOutcome {
        val originalName = safeFileName(track.displayName.ifBlank { "${track.title}.mp3" })
        val existing = findMediaStoreFile(resolver, originalName)
        if (existing != null && track.sizeBytes > 0L && querySize(resolver, existing) == track.sizeBytes) {
            return CopyOutcome.REUSED
        }
        val name = uniqueMediaStoreName(resolver, originalName)
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, track.mimeType ?: "audio/mpeg")
            put(MediaStore.MediaColumns.RELATIVE_PATH, PORTABLE_RELATIVE_PATH)
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Downloads.EXTERNAL_CONTENT_URI
        } else {
            MediaStore.Files.getContentUri("external")
        }
        val uri = resolver.insert(collection, values)
            ?: resolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values)
            ?: error("couldn't create $name in Download/$PORTABLE_FOLDER_NAME")
        try {
            resolver.openInputStream(Uri.parse(track.uri))?.use { input ->
                resolver.openOutputStream(uri)?.use { output -> input.copyTo(output, 64 * 1024) }
                    ?: error("couldn't open destination")
            } ?: error("source file unavailable")
            resolver.update(uri, ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }, null, null)
            return CopyOutcome.COPIED
        } catch (e: Exception) {
            resolver.delete(uri, null, null)
            throw e
        }
    }

    private fun writeMediaStoreJson(resolver: android.content.ContentResolver, json: String) {
        findMediaStoreFile(resolver, PORTABLE_JSON_NAME)?.let { resolver.delete(it, null, null) }
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, PORTABLE_JSON_NAME)
            put(MediaStore.MediaColumns.MIME_TYPE, "application/json")
            put(MediaStore.MediaColumns.RELATIVE_PATH, PORTABLE_RELATIVE_PATH)
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Downloads.EXTERNAL_CONTENT_URI
        } else {
            MediaStore.Files.getContentUri("external")
        }
        val uri = resolver.insert(collection, values)
            ?: error("couldn't create $PORTABLE_JSON_NAME in Download/$PORTABLE_FOLDER_NAME")
        try {
            resolver.openOutputStream(uri)?.use { it.write(json.toByteArray(Charsets.UTF_8)) }
                ?: error("couldn't open backup file")
            resolver.update(uri, ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }, null, null)
        } catch (e: Exception) {
            resolver.delete(uri, null, null)
            throw e
        }
    }

    private fun findMediaStoreFile(resolver: android.content.ContentResolver, name: String): Uri? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
        val collection = MediaStore.Downloads.EXTERNAL_CONTENT_URI
        resolver.query(
            collection,
            arrayOf(MediaStore.MediaColumns._ID),
            "${MediaStore.MediaColumns.RELATIVE_PATH}=? AND ${MediaStore.MediaColumns.DISPLAY_NAME}=?",
            arrayOf(PORTABLE_RELATIVE_PATH, name),
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) return ContentUris.withAppendedId(collection, cursor.getLong(0))
        }
        val files = MediaStore.Files.getContentUri("external")
        resolver.query(
            files,
            arrayOf(MediaStore.MediaColumns._ID),
            "${MediaStore.MediaColumns.RELATIVE_PATH}=? AND ${MediaStore.MediaColumns.DISPLAY_NAME}=?",
            arrayOf(PORTABLE_RELATIVE_PATH, name),
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) return ContentUris.withAppendedId(files, cursor.getLong(0))
        }
        return null
    }

    private fun querySize(resolver: android.content.ContentResolver, uri: Uri): Long =
        resolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use { c ->
            if (c.moveToFirst() && !c.isNull(0)) c.getLong(0) else -1L
        } ?: -1L

    private fun uniqueMediaStoreName(resolver: android.content.ContentResolver, original: String): String {
        val dot = original.lastIndexOf('.')
        val stem = if (dot > 0) original.substring(0, dot) else original
        val ext = if (dot > 0) original.substring(dot) else ""
        var candidate = original
        var n = 1
        while (findMediaStoreFile(resolver, candidate) != null) {
            candidate = "$stem ($n)$ext"
            n++
        }
        return candidate
    }

    private fun safeFileName(name: String): String = name
        .replace(Regex("[\\\\/:*?\"<>|]"), "_")
        .trim()
        .ifBlank { "PulsePlayer_Track.mp3" }

    /** Auto-detects and previews a classic backup in Download/Pulse Player. */
    suspend fun previewDownloadFolder(): Result<Preview> = withContext(Dispatchers.IO) {
        runCatching {
            val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), PORTABLE_FOLDER_NAME)
            var jsonBytes: ByteArray? = null
            var jsonFileName = PORTABLE_JSON_NAME

            // 1. Try reading directly from File if accessible
            if (dir.exists()) {
                val directFile = File(dir, PORTABLE_JSON_NAME)
                if (directFile.exists() && directFile.canRead()) {
                    jsonBytes = directFile.readBytes()
                } else {
                    val candidate = dir.listFiles()?.firstOrNull { f ->
                        val lower = f.name.lowercase()
                        lower.endsWith(".json") && (lower.contains("favorite") || lower.contains("pulseplayer"))
                    }
                    if (candidate != null && candidate.canRead()) {
                        jsonBytes = candidate.readBytes()
                        jsonFileName = candidate.name
                    }
                }
            }

            // 2. Try MediaStore if not found on filesystem
            if (jsonBytes == null) {
                val jsonUri = findMediaStoreFile(ctx.contentResolver, PORTABLE_JSON_NAME)
                if (jsonUri != null) {
                    ctx.contentResolver.openInputStream(jsonUri)?.use { stream ->
                        jsonBytes = readLimited(stream, FavoritesCodec.MAX_BYTES)
                    }
                }
            }

            if (jsonBytes == null) {
                error("No $PORTABLE_JSON_NAME found in Download/$PORTABLE_FOLDER_NAME. Please ensure your backup files are placed in Download/$PORTABLE_FOLDER_NAME, or use the Folder Picker.")
            }

            // Index any audio files present in Download/Pulse Player
            if (dir.exists() && dir.isDirectory) {
                scanDirectoryAudioFiles(dir)
            }

            val sniffed = BackupSniffer.sniff(jsonBytes!!)
            if (sniffed == BackupKind.Full) {
                error("This looks like a Full backup — open the Full backup importer?")
            }
            if (sniffed == BackupKind.RealWav) {
                error("This is an audio WAV file, not a Pulse Player favorites backup.")
            }
            when (val d = FavoritesCodec.decode(jsonBytes!!.toString(Charsets.UTF_8))) {
                is FavoritesCodec.Decoded.Fail -> error(d.reason)
                is FavoritesCodec.Decoded.Ok -> {
                    val lib = db.tracks().allOnce().filter { it.available }
                    val favIds = db.favorites().observeIds().first().toSet()
                    Preview(jsonFileName, d.exportedAt, FavoritesMatcher(lib, favIds).match(d.items), d.skipped)
                }
            }
        }
    }

    private suspend fun scanDirectoryAudioFiles(dir: File) {
        try {
            val audioFiles = dir.listFiles()?.filter { file ->
                !file.isDirectory && SafScanner.isAudio(file.name, null)
            }.orEmpty()

            if (audioFiles.isEmpty()) return

            val mmr = android.media.MediaMetadataRetriever()
            val scanned = mutableListOf<TrackEntity>()
            val scanGen = System.currentTimeMillis()

            for (file in audioFiles) {
                try {
                    val size = file.length()
                    if (size <= 0) continue
                    mmr.setDataSource(file.absolutePath)
                    val title = mmr.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_TITLE)
                        ?.ifBlank { null } ?: file.nameWithoutExtension
                    val artist = mmr.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_ARTIST)
                        ?.ifBlank { null } ?: "Unknown artist"
                    val album = mmr.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_ALBUM)
                        ?.ifBlank { null } ?: PORTABLE_FOLDER_NAME
                    val albumArtist = mmr.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST)
                    val durationMs = mmr.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)
                        ?.toLongOrNull() ?: 0L
                    val trackNumber = mmr.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER)
                        ?.toIntOrNull() ?: 0
                    val year = mmr.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_YEAR)
                        ?.toIntOrNull() ?: 0
                    val genre = mmr.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_GENRE)
                        ?.ifBlank { "" } ?: ""

                    val fileUri = Uri.fromFile(file).toString()
                    val trackId = "file:" + file.absolutePath.hashCode()

                    scanned += TrackEntity(
                        id = trackId,
                        uri = fileUri,
                        displayName = file.name,
                        title = title,
                        artist = artist,
                        album = album,
                        albumArtist = albumArtist,
                        durationMs = durationMs,
                        sizeBytes = size,
                        mimeType = "audio/" + file.extension.lowercase(),
                        folderPath = dir.absolutePath,
                        dateAddedMs = file.lastModified(),
                        dateModifiedMs = file.lastModified(),
                        trackNumber = trackNumber,
                        year = year,
                        normTitle = norm(title),
                        normArtist = norm(artist),
                        normAlbum = norm(album),
                        available = true,
                        dataPath = file.absolutePath,
                        scanGen = scanGen,
                        sourceType = "FILE",
                        stableId = file.name,
                        volumeId = "download_primary",
                        genre = genre
                    )
                } catch (_: Exception) {}
            }
            mmr.release()

            if (scanned.isNotEmpty()) {
                db.withTransaction {
                    db.tracks().upsertAll(scanned)
                }
            }
        } catch (_: Exception) {}
    }

    data class Preview(
        val fileName: String,
        val exportedAt: String?,
        val results: List<MatchResult>,
        val invalid: Int
    )

    suspend fun preview(uri: Uri, fileName: String): Result<Preview> = withContext(Dispatchers.IO) {
        runCatching {
            val bytes = ctx.contentResolver.openInputStream(uri)!!.use {
                readLimited(it, FavoritesCodec.MAX_BYTES)
            }
            val sniffed = BackupSniffer.sniff(bytes)
            if (sniffed == BackupKind.Full) {
                error("This looks like a Full backup — open the Full backup importer?")
            }
            if (sniffed == BackupKind.RealWav) {
                error("This is an audio WAV file, not a Pulse Player favorites backup.")
            }
            when (val d = FavoritesCodec.decode(bytes.toString(Charsets.UTF_8))) {
                is FavoritesCodec.Decoded.Fail -> error(d.reason)
                is FavoritesCodec.Decoded.Ok -> {
                    val lib = db.tracks().allOnce().filter { it.available }
                    val favIds = db.favorites().observeIds().first().toSet()
                    Preview(fileName, d.exportedAt, FavoritesMatcher(lib, favIds).match(d.items), d.skipped)
                }
            }
        }
    }

    /** Indexes pasted audio in a selected Pulse Player folder before fuzzy matching its JSON. */
    suspend fun previewPortableFolder(treeUri: Uri): Result<Preview> = withContext(Dispatchers.IO) {
        runCatching {
            val root = DocumentFile.fromTreeUri(ctx, treeUri)
                ?: error("That folder is no longer accessible.")
            val jsonFile = root.listFiles().firstOrNull { file ->
                val name = file.name.orEmpty().lowercase()
                !file.isDirectory && (
                    name == PORTABLE_JSON_NAME.lowercase() ||
                        (name.endsWith(".json") && name.contains("favorite"))
                    )
            } ?: error("No pulseplayer.favorites JSON file was found in this folder.")

            val scan = SafScanner.scanTree(ctx, db, treeUri)
            if (scan is com.pulse.player.data.scan.ScanState.Partial) {
                error("Couldn't read the folder. Grant Pulse Player access and try again.")
            }
            preview(jsonFile.uri, jsonFile.name ?: PORTABLE_JSON_NAME).getOrThrow()
        }
    }

    /** Applies only confident matches + user-resolved ambiguous picks. Atomic and idempotent. */
    suspend fun apply(preview: Preview, resolved: Map<FavItem, TrackEntity>, replace: Boolean): Int =
        withContext(Dispatchers.IO) {
            val now = System.currentTimeMillis()
            val ids = preview.results.mapNotNull { (it as? MatchResult.Matched)?.track?.id } +
                    resolved.values.map { it.id }
            val distinctIds = ids.distinct()
            db.withTransaction {
                if (replace) db.favorites().clear()
                db.favorites().addAll(distinctIds.map { FavoriteEntity(it, now) })
            }
            distinctIds.size
        }

    private fun readLimited(i: InputStream, max: Long): ByteArray {
        val o = ByteArrayOutputStream()
        LimitedStream(i, max).use { limited ->
            val b = ByteArray(8192)
            var n: Int
            while (limited.read(b).also { n = it } > 0) {
                o.write(b, 0, n)
            }
        }
        return o.toByteArray()
    }
}
