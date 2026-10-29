package com.pulse.player.data.scan

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.util.LruCache
import android.util.Size
import androidx.palette.graphics.Palette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object ArtworkLoader {
    private val cache = object : LruCache<String, Bitmap>(24 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount
    }

    // Cache URIs confirmed to have no artwork to eliminate redundant JNI MediaMetadataRetriever calls
    private val noArtworkCache = LruCache<String, Boolean>(2000)

    suspend fun load(ctx: Context, uri: String, px: Int): Bitmap? = withContext(Dispatchers.IO) {
        val key = "$uri@$px"
        cache.get(key)?.let { return@withContext it }
        if (noArtworkCache.get(key) == true) {
            return@withContext null
        }

        val parsed = runCatching { Uri.parse(uri) }.getOrNull()
        if (parsed == null) {
            noArtworkCache.put(key, true)
            return@withContext null
        }

        // 1. For local file paths, check for companion album art (e.g. pulse_horizon.jpg, cover.jpg)
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
                        decodeSampledBitmap(candidate.absolutePath, px, px)
                    }.getOrNull()
                    if (decoded != null) {
                        cache.put(key, decoded)
                        return@withContext decoded
                    }
                }
            }

            // WAV and raw PCM files do not carry embedded ID3 APIC frames. Skip MediaMetadataRetriever to avoid JNI error logs.
            val ext = file.extension.lowercase()
            if (ext in listOf("wav", "wave", "pcm")) {
                noArtworkCache.put(key, true)
                return@withContext null
            }
        }

        // 2. Query MediaStore thumbnail on Android 10+ for content URIs
        val bmp = runCatching {
            if (Build.VERSION.SDK_INT >= 29 && parsed.scheme == "content") {
                runCatching {
                    ctx.contentResolver.loadThumbnail(parsed, Size(px, px), null)
                }.getOrNull()
            } else {
                null
            }
        }.getOrNull()

        if (bmp != null) {
            cache.put(key, bmp)
            return@withContext bmp
        }

        // 3. Fallback to MediaMetadataRetriever with safety guards
        val retrievedBmp = runCatching {
            val retriever = MediaMetadataRetriever()
            try {
                if (filePath != null) {
                    retriever.setDataSource(filePath)
                } else {
                    retriever.setDataSource(ctx, parsed)
                }
                val picture = runCatching { retriever.embeddedPicture }.getOrNull()
                if (picture != null && picture.isNotEmpty()) {
                    val opts = BitmapFactory.Options().apply {
                        inJustDecodeBounds = true
                    }
                    BitmapFactory.decodeByteArray(picture, 0, picture.size, opts)
                    val sample = maxOf(1, minOf(opts.outWidth / px, opts.outHeight / px))
                    val decodeOpts = BitmapFactory.Options().apply {
                        inSampleSize = sample
                    }
                    BitmapFactory.decodeByteArray(picture, 0, picture.size, decodeOpts)
                } else null
            } finally {
                runCatching { retriever.release() }
            }
        }.getOrNull()

        if (retrievedBmp != null) {
            cache.put(key, retrievedBmp)
            retrievedBmp
        } else {
            noArtworkCache.put(key, true)
            null
        }
    }

    private fun decodeSampledBitmap(path: String, reqWidth: Int, reqHeight: Int): Bitmap? {
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeFile(path, options)
        var inSampleSize = 1
        if (options.outHeight > reqHeight || options.outWidth > reqWidth) {
            val halfHeight = options.outHeight / 2
            val halfWidth = options.outWidth / 2
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2
            }
        }
        val decodeOptions = BitmapFactory.Options().apply {
            this.inSampleSize = inSampleSize
        }
        return BitmapFactory.decodeFile(path, decodeOptions)
    }

    fun dominant(bmp: Bitmap): Int {
        val p = Palette.from(bmp).maximumColorCount(12).generate()
        return p.getDarkMutedColor(p.getMutedColor(p.getDominantColor(0xFF2A0826.toInt())))
    }
}
