package com.pulse.player.data.scan

import android.content.ComponentCallbacks2
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.util.LruCache
import android.util.Size
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

data class ArtRef(val stableKey: String, val uri: String, val modifiedMs: Long)

object ArtworkLoader {
    val sizes = intArrayOf(96, 192, 384, 768)

    fun bucket(px: Int): Int = sizes.firstOrNull { it >= px } ?: sizes.last()

    private val cache = object : LruCache<String, Bitmap>(
        maxOf(8 shl 20, (Runtime.getRuntime().maxMemory() / 16).toInt())
    ) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount
    }

    private val noArtworkCache = LruCache<String, Boolean>(2000)

    fun key(r: ArtRef, px: Int): String = "${r.stableKey}|${r.modifiedMs}|${bucket(px)}"

    suspend fun load(ctx: Context, r: ArtRef, px: Int): Bitmap? = withContext(Dispatchers.IO) {
        val b = bucket(px)
        val k = key(r, px)
        cache.get(k)?.let { return@withContext it }
        if (noArtworkCache.get(k) == true) return@withContext null

        val parsed = runCatching { Uri.parse(r.uri) }.getOrNull()
        if (parsed == null) {
            noArtworkCache.put(k, true)
            return@withContext null
        }

        val filePath = when {
            parsed.scheme == "file" -> parsed.path
            parsed.path?.startsWith("/") == true -> parsed.path
            else -> null
        }

        if (filePath != null) {
            val file = File(filePath)
            val parent = file.parentFile
            val baseName = file.nameWithoutExtension

            val candidateCovers = listOf(
                File(parent, "$baseName.jpg"),
                File(parent, "$baseName.png"),
                File(parent, "$baseName.jpeg"),
                File(parent, "cover.jpg"),
                File(parent, "cover.png"),
                File(parent, "albumart.jpg"),
                File(parent, "folder.jpg")
            )

            for (candidate in candidateCovers) {
                if (candidate.exists() && candidate.length() > 0) {
                    val decoded = runCatching {
                        decodeSampledBitmap(candidate.absolutePath, b, b)
                    }.getOrNull()
                    if (decoded != null) {
                        cache.put(k, decoded)
                        return@withContext decoded
                    }
                }
            }

            val ext = file.extension.lowercase()
            if (ext in listOf("wav", "wave", "pcm")) {
                noArtworkCache.put(k, true)
                return@withContext null
            }
        }

        // Try ContentResolver thumbnail
        val contentBmp = runCatching {
            if (Build.VERSION.SDK_INT >= 29 && parsed.scheme == "content") {
                runCatching {
                    ctx.contentResolver.loadThumbnail(parsed, Size(b, b), null)
                }.getOrNull()
            } else {
                null
            }
        }.getOrNull()

        if (contentBmp != null) {
            cache.put(k, contentBmp)
            return@withContext contentBmp
        }

        // Try MediaMetadataRetriever
        val retrievedBmp = runCatching {
            val retriever = MediaMetadataRetriever()
            try {
                if (filePath != null) {
                    retriever.setDataSource(filePath)
                } else {
                    ctx.contentResolver.openFileDescriptor(parsed, "r")?.use { pfd ->
                        retriever.setDataSource(pfd.fileDescriptor)
                    }
                }
                val rawArt = retriever.embeddedPicture
                if (rawArt != null && rawArt.isNotEmpty()) {
                    val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    BitmapFactory.decodeByteArray(rawArt, 0, rawArt.size, opts)
                    opts.inSampleSize = calculateInSampleSize(opts, b, b)
                    opts.inJustDecodeBounds = false
                    BitmapFactory.decodeByteArray(rawArt, 0, rawArt.size, opts)
                } else null
            } finally {
                retriever.release()
            }
        }.getOrNull()

        if (retrievedBmp != null) {
            cache.put(k, retrievedBmp)
            retrievedBmp
        } else {
            noArtworkCache.put(k, true)
            null
        }
    }

    // Compatibility method
    suspend fun load(ctx: Context, uri: String, px: Int): Bitmap? =
        load(ctx, ArtRef(uri, uri, 0L), px)

    fun trim(level: Int) {
        if (level >= ComponentCallbacks2.TRIM_MEMORY_BACKGROUND) {
            cache.evictAll()
        } else if (level >= ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN) {
            cache.trimToSize(cache.size() / 2)
        }
    }

    private fun decodeSampledBitmap(path: String, reqWidth: Int, reqHeight: Int): Bitmap? {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, options)
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)
        options.inJustDecodeBounds = false
        return BitmapFactory.decodeFile(path, options)
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val (height: Int, width: Int) = options.outHeight to options.outWidth
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }
}
