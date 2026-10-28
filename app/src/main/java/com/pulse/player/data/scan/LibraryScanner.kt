package com.pulse.player.data.scan

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.documentfile.provider.DocumentFile
import com.pulse.player.data.db.PulseDb
import com.pulse.player.data.db.TrackEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

class LibraryScanner(private val ctx: Context, private val db: PulseDb) {
    private val _scanning = MutableStateFlow(false)
    val scanning = _scanning.asStateFlow()

    private val _progress = MutableStateFlow(0)
    val progress = _progress.asStateFlow()

    suspend fun scanMediaStore(minDurationMs: Long, excluded: Set<String>): Int = withContext(Dispatchers.IO) {
        if (_scanning.value) return@withContext 0
        _scanning.value = true
        _progress.value = 0
        try {
            val col = if (Build.VERSION.SDK_INT >= 29) {
                MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
            } else {
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            }

            val p = if (Build.VERSION.SDK_INT >= 29) {
                arrayOf(
                    MediaStore.Audio.Media._ID,
                    MediaStore.Audio.Media.TITLE,
                    MediaStore.Audio.Media.ARTIST,
                    MediaStore.Audio.Media.ALBUM,
                    MediaStore.Audio.Media.DURATION,
                    MediaStore.Audio.Media.SIZE,
                    MediaStore.Audio.Media.MIME_TYPE,
                    MediaStore.Audio.Media.DISPLAY_NAME,
                    MediaStore.Audio.Media.DATE_ADDED,
                    MediaStore.Audio.Media.DATE_MODIFIED,
                    MediaStore.Audio.Media.TRACK,
                    MediaStore.Audio.Media.YEAR,
                    MediaStore.Audio.Media.RELATIVE_PATH,
                    MediaStore.Audio.Media.DATA
                )
            } else {
                arrayOf(
                    MediaStore.Audio.Media._ID,
                    MediaStore.Audio.Media.TITLE,
                    MediaStore.Audio.Media.ARTIST,
                    MediaStore.Audio.Media.ALBUM,
                    MediaStore.Audio.Media.DURATION,
                    MediaStore.Audio.Media.SIZE,
                    MediaStore.Audio.Media.MIME_TYPE,
                    MediaStore.Audio.Media.DISPLAY_NAME,
                    MediaStore.Audio.Media.DATE_ADDED,
                    MediaStore.Audio.Media.DATE_MODIFIED,
                    MediaStore.Audio.Media.TRACK,
                    MediaStore.Audio.Media.YEAR,
                    MediaStore.Audio.Media.DATA
                )
            }

            val sel = "${MediaStore.Audio.Media.DURATION} >= ? AND ${MediaStore.Audio.Media.SIZE} >= 1024 AND " +
                    "${MediaStore.Audio.Media.IS_RINGTONE} = 0 AND ${MediaStore.Audio.Media.IS_NOTIFICATION} = 0 AND ${MediaStore.Audio.Media.IS_ALARM} = 0"
            val batch = ArrayList<TrackEntity>(500)
            var count = 0
            val gen = db.tracks().maxGen() + 1L

            ctx.contentResolver.query(col, p, sel, arrayOf(minDurationMs.toString()), null)?.use { c ->
                val iId = c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val iT = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val iAr = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val iAl = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val iD = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val iS = c.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
                val iM = c.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE)
                val iN = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
                val iDa = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
                val iDm = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_MODIFIED)
                val iTr = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TRACK)
                val iY = c.getColumnIndexOrThrow(MediaStore.Audio.Media.YEAR)
                val iR = if (Build.VERSION.SDK_INT >= 29) c.getColumnIndex(MediaStore.Audio.Media.RELATIVE_PATH) else -1
                val iData = c.getColumnIndex(MediaStore.Audio.Media.DATA)

                while (c.moveToNext()) {
                    val duration = c.getLong(iD)
                    val size = c.getLong(iS)
                    if (duration <= 0 || size < 1024) continue

                    val folder = if (iR >= 0) c.getString(iR).orEmpty().trimEnd('/') else "Music"
                    if (excluded.any { folder.startsWith(it, ignoreCase = true) }) continue

                    val id = c.getLong(iId)
                    val displayName = c.getString(iN).orEmpty()
                    val title = c.getString(iT).orEmpty().ifBlank {
                        displayName.substringBeforeLast('.')
                    }
                    val artist = c.getString(iAr).orEmpty().let {
                        if (it == "<unknown>" || it.isBlank()) "Unknown artist" else it
                    }
                    val album = c.getString(iAl).orEmpty().ifBlank { "Unknown album" }

                    val rawData = if (iData >= 0) c.getString(iData) else null
                    val dataPath = if (!rawData.isNullOrBlank()) {
                        rawData
                    } else {
                        val rel = if (iR >= 0) c.getString(iR).orEmpty().trim('/') else ""
                        if (rel.isNotBlank()) "/storage/emulated/0/$rel/$displayName" else "/storage/emulated/0/$displayName"
                    }

                    batch += TrackEntity(
                        id = "ms:$id",
                        uri = ContentUris.withAppendedId(col, id).toString(),
                        displayName = displayName,
                        title = title,
                        artist = artist,
                        album = album,
                        albumArtist = null,
                        durationMs = duration,
                        sizeBytes = size,
                        mimeType = c.getString(iM),
                        folderPath = folder,
                        dateAddedMs = c.getLong(iDa) * 1000,
                        dateModifiedMs = c.getLong(iDm) * 1000,
                        trackNumber = c.getInt(iTr) % 1000,
                        year = c.getInt(iY),
                        normTitle = norm(title),
                        normArtist = norm(artist),
                        normAlbum = norm(album),
                        available = true,
                        dataPath = dataPath,
                        scanGen = gen
                    )
                    count++
                    if (batch.size >= 500) {
                        db.tracks().upsertAll(batch.toList())
                        batch.clear()
                        _progress.value = count
                    }
                }
                if (batch.isNotEmpty()) {
                    db.tracks().upsertAll(batch.toList())
                }
                // Mark older MediaStore entries missing without full list flicker
                db.tracks().markMsMissing(gen)
                _progress.value = count
            }
            count
        } finally {
            _scanning.value = false
        }
    }

    suspend fun scanSafTree(treeUri: Uri, minDurationMs: Long): Int = withContext(Dispatchers.IO) {
        val root = DocumentFile.fromTreeUri(ctx, treeUri) ?: return@withContext 0
        var count = 0
        val batch = ArrayList<TrackEntity>()
        val gen = db.tracks().maxGen() + 1L

        fun scanDir(dir: DocumentFile, path: String) {
            dir.listFiles().forEach { file ->
                if (file.isDirectory) {
                    scanDir(file, "$path/${file.name.orEmpty()}")
                } else if (file.type?.startsWith("audio/") == true ||
                    file.name?.endsWith(".mp3", true) == true ||
                    file.name?.endsWith(".m4a", true) == true ||
                    file.name?.endsWith(".flac", true) == true ||
                    file.name?.endsWith(".wav", true) == true ||
                    file.name?.endsWith(".ogg", true) == true
                ) {
                    val size = file.length()
                    if (size < 1024) return@forEach

                    val name = file.name.orEmpty()
                    val title = name.substringBeforeLast('.')
                    val id = "saf:${file.uri.toString().hashCode()}"
                    val cleanPath = path.ifBlank { "Music" }.trim('/')
                    val dataPath = "/storage/emulated/0/$cleanPath/$name"

                    batch += TrackEntity(
                        id = id,
                        uri = file.uri.toString(),
                        displayName = name,
                        title = title,
                        artist = "Unknown artist",
                        album = cleanPath,
                        albumArtist = null,
                        durationMs = 0L,
                        sizeBytes = size,
                        mimeType = file.type,
                        folderPath = cleanPath,
                        dateAddedMs = file.lastModified(),
                        dateModifiedMs = file.lastModified(),
                        trackNumber = 0,
                        year = 0,
                        normTitle = norm(title),
                        normArtist = norm("Unknown artist"),
                        normAlbum = norm(cleanPath),
                        available = true,
                        dataPath = dataPath,
                        scanGen = gen
                    )
                    count++
                    if (batch.size >= 500) {
                        db.tracks().upsertAll(batch.toList())
                        batch.clear()
                    }
                }
            }
        }

        scanDir(root, root.name.orEmpty().ifBlank { "Music" })
        if (batch.isNotEmpty()) {
            db.tracks().upsertAll(batch)
        }
        db.tracks().markSafMissing(gen)
        count
    }
}
