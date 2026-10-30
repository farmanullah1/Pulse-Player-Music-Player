package com.pulse.player.data.scan

import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.documentfile.provider.DocumentFile
import androidx.room.withTransaction
import com.pulse.player.data.db.IdentityModel
import com.pulse.player.data.db.PulseDb
import com.pulse.player.data.db.ScanSeenEntity
import com.pulse.player.data.db.TrackEntity
import com.pulse.player.data.settings.SettingsStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext

enum class PartialReason {
    PermissionRevoked,
    Cancelled,
    QueryFailed,
    EmptyResultSuspicious,
    StorageUnavailable
}

sealed interface ScanState {
    data object Idle : ScanState
    data class Scanning(val found: Int) : ScanState
    data object Reconciling : ScanState
    data class Success(val atMs: Long, val total: Int) : ScanState
    data class Partial(val reason: PartialReason, val lastGoodAtMs: Long?) : ScanState
    data class Failed(val message: String) : ScanState
}

fun audioPermission(): String = if (Build.VERSION.SDK_INT >= 33) {
    android.Manifest.permission.READ_MEDIA_AUDIO
} else {
    android.Manifest.permission.READ_EXTERNAL_STORAGE
}

class LibraryScanner(
    private val ctx: Context,
    private val db: PulseDb,
    private val settings: SettingsStore
) {
    private val _state = MutableStateFlow<ScanState>(ScanState.Idle)
    val state = _state.asStateFlow()

    val scanning = _state.map { it is ScanState.Scanning || it is ScanState.Reconciling }
    val progress = _state.map { if (it is ScanState.Scanning) it.found else 0 }

    private val mutex = Mutex()

    suspend fun scanDevice(excluded: Set<String>): ScanState = withContext(Dispatchers.IO) {
        val lastGood = settings.lastGoodScanAt()
        fun partial(r: PartialReason) = ScanState.Partial(r, lastGood).also { _state.value = it }

        if (ContextCompat.checkSelfPermission(ctx, audioPermission()) != PackageManager.PERMISSION_GRANTED) {
            return@withContext partial(PartialReason.PermissionRevoked)
        }
        if (!mutex.tryLock()) return@withContext _state.value

        try {
            _state.value = ScanState.Scanning(0)
            db.scan().clearSeen()

            val uri = if (Build.VERSION.SDK_INT >= 29) {
                MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
            } else {
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            }

            val sel = "${MediaStore.Audio.Media.DURATION} > 0 AND ${MediaStore.Audio.Media.SIZE} > 0 AND " +
                    "${MediaStore.Audio.Media.IS_RINGTONE} = 0 AND ${MediaStore.Audio.Media.IS_NOTIFICATION} = 0 AND " +
                    "${MediaStore.Audio.Media.IS_ALARM} = 0"

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
                    MediaStore.Audio.Media.DATA,
                    MediaStore.Audio.Media.VOLUME_NAME
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

            val cursor = ctx.contentResolver.query(uri, p, sel, null, null)
                ?: return@withContext partial(PartialReason.QueryFailed)

            var found = 0
            val rows = ArrayList<TrackEntity>(500)
            val seen = ArrayList<ScanSeenEntity>(500)

            cursor.use { c ->
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
                val iVol = if (Build.VERSION.SDK_INT >= 29) c.getColumnIndex(MediaStore.Audio.Media.VOLUME_NAME) else -1

                while (c.moveToNext()) {
                    ensureActive()
                    val id = c.getLong(iId)
                    val duration = c.getLong(iD)
                    val size = c.getLong(iS)
                    if (duration <= 0 || size <= 0) continue

                    val folder = if (iR >= 0) c.getString(iR).orEmpty().trimEnd('/') else "Music"
                    if (excluded.any { folder.startsWith(it, ignoreCase = true) }) continue

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

                    val volumeName = if (iVol >= 0) c.getString(iVol).orEmpty().ifBlank { "external_primary" } else "external_primary"
                    val trackId = IdentityModel.mediaStoreId(id)

                    val track = TrackEntity(
                        id = trackId,
                        uri = ContentUris.withAppendedId(uri, id).toString(),
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
                        scanGen = 0,
                        sourceType = "MEDIASTORE",
                        stableId = id.toString(),
                        volumeId = volumeName,
                        folderUri = null
                    )

                    rows += track
                    seen += ScanSeenEntity(trackId)
                    found++

                    if (rows.size >= 500) {
                        db.tracks().upsertAll(rows.toList())
                        db.scan().seen(seen.toList())
                        rows.clear()
                        seen.clear()
                        _state.value = ScanState.Scanning(found)
                    }
                }
            }

            if (rows.isNotEmpty()) {
                db.tracks().upsertAll(rows.toList())
                db.scan().seen(seen.toList())
            }

            _state.value = ScanState.Reconciling
            if (found == 0 && db.scan().availableMediaStore() > 0) {
                return@withContext partial(PartialReason.EmptyResultSuspicious)
            }

            db.withTransaction {
                db.scan().markSeenAvailable()
                db.scan().markUnseenUnavailable()
            }

            val now = System.currentTimeMillis()
            settings.setLastGoodScanAt(now)
            ScanState.Success(now, found).also { _state.value = it }
        } catch (e: CancellationException) {
            partial(PartialReason.Cancelled)
            throw e
        } catch (e: SecurityException) {
            partial(PartialReason.PermissionRevoked)
        } catch (e: Exception) {
            Log.w("LibraryScanner", "Scan failed", e)
            partial(PartialReason.QueryFailed)
        } finally {
            withContext(NonCancellable) {
                db.scan().clearSeen()
            }
            mutex.unlock()
        }
    }

    suspend fun scanMediaStore(minDurationMs: Long, excluded: Set<String>): Int {
        val result = scanDevice(excluded)
        return if (result is ScanState.Success) result.total else 0
    }

    suspend fun scanSafTree(treeUri: Uri, minDurationMs: Long): Int = withContext(Dispatchers.IO) {
        val root = DocumentFile.fromTreeUri(ctx, treeUri) ?: return@withContext 0
        var count = 0
        val batch = ArrayList<TrackEntity>()

        suspend fun scanDir(dir: DocumentFile, path: String) {
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
                    val docId = DocumentsContract.getDocumentId(file.uri)
                    val id = IdentityModel.safId(treeUri.toString(), docId)
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
                        scanGen = 0,
                        sourceType = "SAF",
                        stableId = docId,
                        volumeId = "external_primary",
                        folderUri = treeUri.toString()
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
        count
    }
}
