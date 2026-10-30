package com.pulse.player.data.scan

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.DocumentsContract
import androidx.documentfile.provider.DocumentFile
import com.pulse.player.data.db.IdentityModel
import com.pulse.player.data.db.PulseDb
import com.pulse.player.data.db.TrackEntity
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream

data class CopyReport(
    val copied: Int,
    val skipped: Int,
    val failed: List<String>
)

object MusicCopier {

    suspend fun copyTracksToDestination(
        ctx: Context,
        db: PulseDb,
        destTreeUri: Uri,
        sourceUris: List<Uri>,
        onProgress: (copied: Int, total: Int) -> Unit = { _, _ -> }
    ): CopyReport = withContext(Dispatchers.IO) {
        val destDir = DocumentFile.fromTreeUri(ctx, destTreeUri)
            ?: return@withContext CopyReport(0, 0, listOf("Destination folder not accessible"))

        var copied = 0
        var skipped = 0
        val failed = mutableListOf<String>()
        val total = sourceUris.size

        for (srcUri in sourceUris) {
            ensureActive()
            var createdDocUri: Uri? = null
            try {
                val displayName = queryDisplayName(ctx, srcUri) ?: "Track_${System.currentTimeMillis()}.mp3"
                val mime = ctx.contentResolver.getType(srcUri) ?: "audio/mpeg"

                // Unique name resolution: e.g., name (1).ext
                val targetName = findUniqueName(destDir, displayName)
                val targetDoc = destDir.createFile(mime, targetName)
                    ?: throw IllegalStateException("Could not create document $targetName")
                createdDocUri = targetDoc.uri

                // Stream copy in 64 KB chunks
                ctx.contentResolver.openInputStream(srcUri)?.use { input: InputStream ->
                    ctx.contentResolver.openOutputStream(createdDocUri)?.use { output: OutputStream ->
                        val buffer = ByteArray(64 * 1024)
                        var bytesRead: Int
                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            ensureActive()
                            output.write(buffer, 0, bytesRead)
                        }
                        output.flush()
                    }
                } ?: throw IllegalStateException("Could not open source stream for $srcUri")

                val copyId = IdentityModel.copyId(createdDocUri.toString())
                val track = readCopiedMetadata(ctx, createdDocUri, copyId, targetName, mime, destTreeUri)
                db.tracks().upsertAll(listOf(track))

                copied++
                onProgress(copied, total)
            } catch (ce: CancellationException) {
                // Delete partial file on cancellation
                createdDocUri?.let { uri ->
                    runCatching { DocumentsContract.deleteDocument(ctx.contentResolver, uri) }
                }
                throw ce
            } catch (e: Exception) {
                createdDocUri?.let { uri ->
                    runCatching { DocumentsContract.deleteDocument(ctx.contentResolver, uri) }
                }
                failed.add("${srcUri.lastPathSegment ?: "File"}: ${e.message}")
            }
        }

        CopyReport(copied, skipped, failed)
    }

    private fun findUniqueName(dir: DocumentFile, baseName: String): String {
        val dot = baseName.lastIndexOf('.')
        val name = if (dot > 0) baseName.substring(0, dot) else baseName
        val ext = if (dot > 0) baseName.substring(dot) else ""

        var candidate = baseName
        var counter = 1
        while (dir.findFile(candidate) != null) {
            candidate = "$name ($counter)$ext"
            counter++
        }
        return candidate
    }

    private fun queryDisplayName(ctx: Context, uri: Uri): String? {
        val proj = arrayOf(android.provider.OpenableColumns.DISPLAY_NAME)
        return ctx.contentResolver.query(uri, proj, null, null, null)?.use { c ->
            if (c.moveToFirst()) c.getString(0) else null
        }
    }

    private fun readCopiedMetadata(
        ctx: Context,
        uri: Uri,
        copyId: String,
        displayName: String,
        mime: String,
        destTreeUri: Uri
    ): TrackEntity {
        var title = displayName.substringBeforeLast('.')
        var artist = "Unknown artist"
        var album = "Music"
        var durationMs = 0L
        var trackNumber = 0
        var year = 0
        var sizeBytes = 0L

        try {
            ctx.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                sizeBytes = pfd.statSize
                val mmr = MediaMetadataRetriever()
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
                mmr.release()
            }
        } catch (_: Exception) {}

        return TrackEntity(
            id = copyId,
            uri = uri.toString(),
            displayName = displayName,
            title = title,
            artist = artist,
            album = album,
            albumArtist = null,
            durationMs = durationMs,
            sizeBytes = sizeBytes,
            mimeType = mime,
            folderPath = "Music",
            dateAddedMs = System.currentTimeMillis(),
            dateModifiedMs = System.currentTimeMillis(),
            trackNumber = trackNumber,
            year = year,
            normTitle = norm(title),
            normArtist = norm(artist),
            normAlbum = norm(album),
            available = true,
            dataPath = null,
            scanGen = 0,
            sourceType = "COPIED",
            stableId = copyId,
            volumeId = "external_primary",
            folderUri = destTreeUri.toString()
        )
    }
}

