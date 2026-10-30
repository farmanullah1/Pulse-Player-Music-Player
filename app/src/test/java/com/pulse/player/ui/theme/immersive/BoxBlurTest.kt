package com.pulse.player.ui.theme.immersive

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BoxBlurTest {

    @Test
    fun uniformColorArrayStaysUniformAfterBlur() {
        val w = 16
        val h = 16
        val uniformColor = (0xFF shl 24) or (0x4A shl 16) or (0x8B shl 8) or 0xCD
        val src = IntArray(w * h) { uniformColor }
        val dst = IntArray(w * h)

        blurH(src, dst, w, h, radius = 4)

        for (i in dst.indices) {
            assertEquals("Pixel at $i should remain uniform", uniformColor, dst[i])
        }
    }

    @Test
    fun halfBlackHalfWhiteGetsMidGreyEdge() {
        val w = 16
        val h = 8
        val black = 0xFF000000.toInt()
        val white = 0xFFFFFFFF.toInt()
        val src = IntArray(w * h) { i ->
            val x = i % w
            if (x < 8) black else white
        }
        val dst = IntArray(w * h)

        blurH(src, dst, w, h, radius = 3)

        // Across each row, the boundary pixels (x=7, x=8) should be blurred to mid-grey
        for (y in 0 until h) {
            val leftOfBoundary = dst[y * w + 7]
            val rightOfBoundary = dst[y * w + 8]

            val redLeft = (leftOfBoundary ushr 16) and 0xFF
            val redRight = (rightOfBoundary ushr 16) and 0xFF

            // Instead of pure 0 or 255, values should be around mid-range
            assertTrue("Left boundary should be blurred: $redLeft", redLeft in 30..150)
            assertTrue("Right boundary should be blurred: $redRight", redRight in 100..225)
        }
    }

    @Test
    fun excessiveRadiusClampsSafelyWithoutCrashing() {
        val w = 10
        val h = 10
        val src = IntArray(w * h) { 0xFF112233.toInt() }
        val dst = IntArray(w * h)

        // radius 50 is far larger than w/2; should clamp to (w-1)/2 = 4 and not throw IndexOutOfBounds
        blurH(src, dst, w, h, radius = 50)
        assertEquals(0xFF112233.toInt(), dst[0])
    }

    @Test
    fun zeroOrNegativeRadiusLeavesArrayUnchanged() {
        val w = 8
        val h = 8
        val src = IntArray(w * h) { it }
        val dst = IntArray(w * h)

        blurH(src, dst, w, h, radius = 0)
        for (i in src.indices) {
            assertEquals(src[i], dst[i])
        }

        blurH(src, dst, w, h, radius = -5)
        for (i in src.indices) {
            assertEquals(src[i], dst[i])
        }
    }

    @Test
    fun transposeCorrectlySwapsCoordinates() {
        val w = 4
        val h = 3
        val src = IntArray(w * h) { i ->
            val x = i % w
            val y = i / w
            y * 10 + x
        }
        val dst = IntArray(w * h)

        transpose(src, dst, w, h)

        // After transpose: new width is h (3), new height is w (4)
        for (y in 0 until h) {
            for (x in 0 until w) {
                val orig = src[y * w + x]
                val transposed = dst[x * h + y]
                assertEquals(orig, transposed)
            }
        }
    }

    @Test
    fun fullPassPreservesUniformPixels() {
        val w = 12
        val h = 12
        val color = 0xFF55AAFF.toInt()
        val px = IntArray(w * h) { color }
        val tmp = IntArray(w * h)

        repeat(3) {
            blurH(px, tmp, w, h, radius = 4)
            transpose(tmp, px, w, h)
            blurH(px, tmp, h, w, radius = 4)
            transpose(tmp, px, h, w)
        }

        for (p in px) {
            assertEquals(color, p)
        }
    }
}

