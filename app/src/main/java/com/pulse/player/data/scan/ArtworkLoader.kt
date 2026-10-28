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

object ArtworkLoader {
    private val cache = object : LruCache<String, Bitmap>(24 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount
    }

    suspend fun load(ctx: Context, uri: String, px: Int): Bitmap? = withContext(Dispatchers.IO) {
        val key = "$uri@$px"
        cache.get(key)?.let { return@withContext it }

        val bmp = runCatching {
            if (Build.VERSION.SDK_INT >= 29) {
                ctx.contentResolver.loadThumbnail(Uri.parse(uri), Size(px, px), null)
            } else {
                val retriever = MediaMetadataRetriever()
                try {
                    retriever.setDataSource(ctx, Uri.parse(uri))
                    val picture = retriever.embeddedPicture
                    if (picture != null) {
                        BitmapFactory.decodeByteArray(picture, 0, picture.size)
                    } else null
                } finally {
                    retriever.release()
                }
            }
        }.getOrNull()

        if (bmp != null) {
            cache.put(key, bmp)
        }
        bmp
    }

    fun dominant(bmp: Bitmap): Int {
        val p = Palette.from(bmp).maximumColorCount(12).generate()
        return p.getDarkMutedColor(p.getMutedColor(p.getDominantColor(0xFF2A0826.toInt())))
    }
}
