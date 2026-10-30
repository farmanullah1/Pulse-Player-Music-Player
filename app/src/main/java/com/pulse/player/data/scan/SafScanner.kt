package com.pulse.player.data.scan

import android.content.Context
import android.database.Cursor
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.DocumentsContract.Document
import androidx.room.withTransaction
import com.pulse.player.data.db.IdentityModel
import com.pulse.player.data.db.PulseDb
import com.pulse.player.data.db.ScanSeenEntity
import com.pulse.player.data.db.TrackEntity
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext
import java.io.IOException

object SafScanner {

    fun isAudio(name: String, mime: String?): Boolean {
        if (mime != null && mime.startsWith("audio/")) return true
        val lower = name.lowercase()
        return lower.endsWith(".mp3") || lower.endsWith(".m4a") || lower.endsWith(".flac") ||
                lower.endsWith(".wav") || lower.endsWith(".ogg") || lower.endsWith(".aac") ||
                lower.endsWith(".opus")
    }

    private fun treeName(treeUri: Uri): String {
        val docId = DocumentsContract.getTreeDocumentId(treeUri)
        return docId.substringAfterLast(':').ifBlank { "Music" }
    }

    private fun readMetadata(
        ctx: Context,
        docUri: Uri,
        tid: String,
        docId: String,
        folderDisplay: String,
        c: Cursor,
        treeUri: Uri
    ): TrackEntity? {
        val name = c.getString(1) ?: return null
        val mime = c.getString(2)
        val size = c.getLong(3)
        val lastMod = c.getLong(4)
        if (size <= 0) return null

        var title = name.substringBeforeLast('.')
        var artist = "Unknown artist"
        var album = folderDisplay.ifBlank { "Music" }
        var durationMs = 0L
        var trackNumber = 0
        var year = 0

        try {
            val mmr = MediaMetadataRetriever()
            ctx.contentResolver.openFileDescriptor(docUri, "r")?.use { pfd ->
                mmr.setDataSource(pfd.fileDescriptor)
                val mTitle = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                if (!mTitle.isNullOrBlank()) title = mTitle
                val mArtist = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                if (!mArtist.isNullOrBlank()) artist = mArtist
                val mAlbum = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
                if (!mAlbum.isNullOrBlank()) album = mAlbum
                val mDur = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                if (!mDur.isNullOrBlank()) durationMs = mDur.toLongOrNull() ?: 0L
                val mTrk = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER)
                if (!mTrk.isNullOrBlank()) trackNumber = (mTrk.toIntOrNull() ?: 0) % 1000
                val mYear = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_YEAR)
                if (!mYear.isNullOrBlank()) year = mYear.toIntOrNull() ?: 0
            }
            mmr.release()
        } catch (_: Exception) {}

        val volumeId = docId.substringBefore(':', "external_primary").ifBlank { "external_primary" }

        return TrackEntity(
            id = tid,
            uri = docUri.toString(),
            displayName = name,
            title = title,
            artist = artist,
            album = album,
            albumArtist = null,
            durationMs = durationMs,
            sizeBytes = size,
            mimeType = mime,
            folderPath = folderDisplay,
            dateAddedMs = lastMod,
            dateModifiedMs = lastMod,
            trackNumber = trackNumber,
            year = year,
            normTitle = norm(title),
            normArtist = norm(artist),
            normAlbum = norm(album),
            available = true,
            dataPath = null,
            scanGen = 0,
            sourceType = "SAF",
            stableId = docId,
            volumeId = volumeId,
            folderUri = treeUri.toString()
        )
    }

    suspend fun scanTree(
        ctx: Context,
        db: PulseDb,
        treeUri: Uri,
        state: MutableStateFlow<ScanState>? = null
    ): ScanState = withContext(Dispatchers.IO) {
        val persisted = ctx.contentResolver.persistedUriPermissions.any { it.uri == treeUri && it.isReadPermission }
        if (!persisted) {
            val partial = ScanState.Partial(PartialReason.StorageUnavailable, null)
            state?.value = partial
            return@withContext partial
        }

        val seen = HashSet<String>()
        val seenEntities = ArrayList<ScanSeenEntity>()
        val rows = ArrayList<TrackEntity>()

        fun walk(docId: String, display: String) {
            val children = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, docId)
            val cols = arrayOf(
                Document.COLUMN_DOCUMENT_ID,
                Document.COLUMN_DISPLAY_NAME,
                Document.COLUMN_MIME_TYPE,
                Document.COLUMN_SIZE,
                Document.COLUMN_LAST_MODIFIED
            )
            ctx.contentResolver.query(children, cols, null, null, null)?.use { c ->
                while (c.moveToNext()) {
                    val id = c.getString(0) ?: continue
                    val name = c.getString(1) ?: continue
                    val mime = c.getString(2)
                    if (mime == Document.MIME_TYPE_DIR) {
                        walk(id, "$display/$name")
                        continue
                    }
                    if (!isAudio(name, mime)) continue
                    val tid = IdentityModel.safId(treeUri.toString(), id)
                    seen += tid
                    seenEntities += ScanSeenEntity(tid)

                    val existing = db.tracks().byId(tid)
                    if (existing != null && existing.sizeBytes == c.getLong(3) && existing.dateModifiedMs == c.getLong(4)) {
                        // Unchanged: skip metadata re-read
                    } else {
                        val docUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, id)
                        readMetadata(ctx, docUri, tid, id, display, c, treeUri)?.let { rows += it }
                    }
                }
            } ?: throw IOException("Folder unreadable")
        }

        try {
            walk(DocumentsContract.getTreeDocumentId(treeUri), treeName(treeUri))
            db.withTransaction {
                if (rows.isNotEmpty()) {
                    db.tracks().upsertAll(rows)
                }
                db.scan().seen(seenEntities)
                db.scan().markSafUnseenForTree(treeUri.toString())
                db.scan().clearSeen()
            }
            val res = ScanState.Success(System.currentTimeMillis(), seen.size)
            state?.value = res
            res
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            val partial = ScanState.Partial(PartialReason.StorageUnavailable, null)
            state?.value = partial
            partial
        }
    }
}

