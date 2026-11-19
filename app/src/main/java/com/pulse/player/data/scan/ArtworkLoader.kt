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
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.io.File

data class ArtRef(val stableKey: String, val uri: String, val modifiedMs: Long)

object ArtworkLoader {
    val sizes = intArrayOf(96, 192, 384, 768)
    private val SAFE_KEY_REGEX = Regex("[^a-zA-Z0-9._-]")
    private val decodeSemaphore = Semaphore(4)

    fun bucket(px: Int): Int = sizes.firstOrNull { it >= px } ?: sizes.last()

    private val cache = object : LruCache<String, Bitmap>(
        maxOf(8 shl 20, (Runtime.getRuntime().maxMemory() / 16).toInt())
    ) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount
    }

    private val noArtworkCache = LruCache<String, Boolean>(16_384)

    fun key(r: ArtRef, px: Int): String = "${r.stableKey}|${r.modifiedMs}|${bucket(px)}"

    private fun noArtKey(r: ArtRef): String = "${r.stableKey}|${r.modifiedMs}"

    fun getCached(r: ArtRef, px: Int): Bitmap? = cache.get(key(r, px))

    fun getCached(uri: String, px: Int): Bitmap? = getCached(ArtRef(uri, uri, 0L), px)

    fun hasNoArtwork(r: ArtRef): Boolean = noArtworkCache.get(noArtKey(r)) == true

    fun hasNoArtwork(uri: String): Boolean = hasNoArtwork(ArtRef(uri, uri, 0L))

    suspend fun load(ctx: Context, r: ArtRef, px: Int): Bitmap? = withContext(Dispatchers.IO) {
        val b = bucket(px)
        val k = key(r, px)
        val nk = noArtKey(r)
        cache.get(k)?.let { return@withContext it }
        if (noArtworkCache.get(nk) == true) return@withContext null

        ensureActive()
        val parsed = runCatching { Uri.parse(r.uri) }.getOrNull()
        if (parsed == null) {
            noArtworkCache.put(nk, true)
            return@withContext null
        }

        val preferredConfig = if (b <= 384) Bitmap.Config.RGB_565 else Bitmap.Config.ARGB_8888

        // Check user-attached custom cover art in app filesDir
        val safeKey = r.stableKey.replace(SAFE_KEY_REGEX, "_")
        val customArtFile = File(File(ctx.filesDir, "custom_art"), "$safeKey.jpg")
        if (customArtFile.exists() && customArtFile.length() > 0) {
            val customBmp = runCatching {
                decodeSampledBitmap(customArtFile.absolutePath, b, b, preferredConfig)
            }.getOrNull()
            if (customBmp != null) {
                cache.put(k, customBmp)
                return@withContext customBmp
            }
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
                ensureActive()
                if (candidate.exists() && candidate.length() > 0) {
                    val decoded = runCatching {
                        decodeSampledBitmap(candidate.absolutePath, b, b, preferredConfig)
                    }.getOrNull()
                    if (decoded != null) {
                        cache.put(k, decoded)
                        return@withContext decoded
                    }
                }
            }

            val ext = file.extension.lowercase()
            if (ext == "wav" || ext == "wave" || ext == "pcm") {
                noArtworkCache.put(nk, true)
                return@withContext null
            }
        }

        decodeSemaphore.withPermit {
            ensureActive()
            cache.get(k)?.let { return@withPermit it }

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
                return@withPermit contentBmp
            }

            ensureActive()

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
                        opts.inPreferredConfig = preferredConfig
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
                noArtworkCache.put(nk, true)
                null
            }
        }
    }

    fun clearCache() {
        cache.evictAll()
        noArtworkCache.evictAll()
    }

    // Compatibility method
    suspend fun load(ctx: Context, uri: String, px: Int): Bitmap? =
        load(ctx, ArtRef(uri, uri, 0L), px)

    fun trim(level: Int) {
        if (level >= ComponentCallbacks2.TRIM_MEMORY_BACKGROUND) {
            cache.evictAll()
            noArtworkCache.evictAll()
        } else if (level >= ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN) {
            cache.trimToSize(cache.size() / 2)
        }
    }

    private fun decodeSampledBitmap(
        path: String,
        reqWidth: Int,
        reqHeight: Int,
        preferredConfig: Bitmap.Config = Bitmap.Config.RGB_565
    ): Bitmap? {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, options)
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)
        options.inPreferredConfig = preferredConfig
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

    fun dominant(bmp: Bitmap): Int {
        var rTotal = 0L
        var gTotal = 0L
        var bTotal = 0L
        var count = 0
        val stepX = maxOf(1, bmp.width / 16)
        val stepY = maxOf(1, bmp.height / 16)
        for (x in 0 until bmp.width step stepX) {
            for (y in 0 until bmp.height step stepY) {
                val pixel = bmp.getPixel(x, y)
                rTotal += (pixel shr 16) and 0xFF
                gTotal += (pixel shr 8) and 0xFF
                bTotal += pixel and 0xFF
                count++
            }
        }
        if (count == 0) return 0xFF2A0826.toInt()
        val r = (rTotal / count).toInt()
        val g = (gTotal / count).toInt()
        val b = (bTotal / count).toInt()
        return (0xFF shl 24) or (r shl 16) or (g shl 8) or b
    }
}
