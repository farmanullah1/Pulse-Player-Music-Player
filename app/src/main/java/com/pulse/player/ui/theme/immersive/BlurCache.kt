package com.pulse.player.ui.theme.immersive

import android.graphics.Bitmap
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object BlurCache {
    private val cache = LruCache<String, Bitmap>(6)

    suspend fun get(key: String, src: Bitmap): Bitmap = cache.get(key) ?: withContext(Dispatchers.Default) {
        val small = Bitmap.createScaledBitmap(src, 96, 96, true)
        val out = boxBlur(small.copy(Bitmap.Config.ARGB_8888, true), radius = 8, passes = 3)
        cache.put(key, out)
        out
    }

    fun clear() {
        cache.evictAll()
    }
}

fun boxBlur(b: Bitmap, radius: Int, passes: Int): Bitmap {
    val w = b.width
    val h = b.height
    if (w <= 0 || h <= 0 || radius <= 0 || passes <= 0) return b
    val px = IntArray(w * h)
    b.getPixels(px, 0, w, 0, 0, w, h)
    val tmp = IntArray(w * h)

    repeat(passes) {
        blurH(px, tmp, w, h, radius)
        transpose(tmp, px, w, h)
        blurH(px, tmp, h, w, radius)
        transpose(tmp, px, h, w)
    }

    b.setPixels(px, 0, w, 0, 0, w, h)
    return b
}

internal fun blurH(src: IntArray, dst: IntArray, w: Int, h: Int, radius: Int) {
    val r = radius.coerceIn(0, maxOf(0, (w - 1) / 2))
    if (r == 0) {
        System.arraycopy(src, 0, dst, 0, w * h)
        return
    }
    val div = 2 * r + 1

    for (y in 0 until h) {
        val rowOffset = y * w
        var aSum = 0
        var rSum = 0
        var gSum = 0
        var bSum = 0

        // Initial window sum for x = 0
        for (i in -r..r) {
            val p = src[rowOffset + i.coerceIn(0, w - 1)]
            aSum += (p ushr 24) and 0xFF
            rSum += (p ushr 16) and 0xFF
            gSum += (p ushr 8) and 0xFF
            bSum += p and 0xFF
        }

        for (x in 0 until w) {
            val a = aSum / div
            val red = rSum / div
            val g = gSum / div
            val b = bSum / div
            dst[rowOffset + x] = (a shl 24) or (red shl 16) or (g shl 8) or b

            val nextX = (x + r + 1).coerceIn(0, w - 1)
            val prevX = (x - r).coerceIn(0, w - 1)
            val pNext = src[rowOffset + nextX]
            val pPrev = src[rowOffset + prevX]

            aSum += ((pNext ushr 24) and 0xFF) - ((pPrev ushr 24) and 0xFF)
            rSum += ((pNext ushr 16) and 0xFF) - ((pPrev ushr 16) and 0xFF)
            gSum += ((pNext ushr 8) and 0xFF) - ((pPrev ushr 8) and 0xFF)
            bSum += (pNext and 0xFF) - (pPrev and 0xFF)
        }
    }
}

internal fun transpose(src: IntArray, dst: IntArray, w: Int, h: Int) {
    for (y in 0 until h) {
        val rowOffset = y * w
        for (x in 0 until w) {
            dst[x * h + y] = src[rowOffset + x]
        }
    }
}

