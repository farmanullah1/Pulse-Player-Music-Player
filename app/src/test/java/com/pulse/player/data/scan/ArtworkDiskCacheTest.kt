package com.pulse.player.data.scan

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class ArtworkDiskCacheTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun diskCacheFileName_isDeterministicAndDistinctPerKey() {
        val refA = ArtRef("track_1", "content://media/1", 1000L)
        val refB = ArtRef("track_1", "content://media/1", 2000L)

        val k1 = ArtworkLoader.key(refA, 192)
        val k2 = ArtworkLoader.key(refA, 192)
        val kDifferentSize = ArtworkLoader.key(refA, 384)
        val kDifferentMod = ArtworkLoader.key(refB, 192)

        assertEquals(ArtworkLoader.diskCacheFileName(k1), ArtworkLoader.diskCacheFileName(k2))
        assertNotEquals(ArtworkLoader.diskCacheFileName(k1), ArtworkLoader.diskCacheFileName(kDifferentSize))
        assertNotEquals(ArtworkLoader.diskCacheFileName(k1), ArtworkLoader.diskCacheFileName(kDifferentMod))
        assertTrue(ArtworkLoader.diskCacheFileName(k1).endsWith(".jpg"))
    }

    @Test
    fun trimDiskCacheDir_evictsOldestFilesDownToTargetCap() {
        val dir = tempFolder.newFolder("art_cache")
        val old1 = File(dir, "old1.jpg").apply {
            writeBytes(ByteArray(40))
            setLastModified(1_000L)
        }
        val old2 = File(dir, "old2.jpg").apply {
            writeBytes(ByteArray(40))
            setLastModified(2_000L)
        }
        val newest = File(dir, "newest.jpg").apply {
            writeBytes(ByteArray(40))
            setLastModified(3_000L)
        }
        val tmpIgnored = File(dir, "writing.jpg.tmp").apply {
            writeBytes(ByteArray(100))
            setLastModified(500L)
        }

        // Total non-tmp is 120 bytes; cap = 100 bytes => trims down to <= 85 bytes (deletes old1 -> 80 bytes)
        ArtworkLoader.trimDiskCacheDir(dir, maxBytes = 100L)

        assertFalse(old1.exists())
        assertTrue(old2.exists())
        assertTrue(newest.exists())
        assertTrue(tmpIgnored.exists())
    }
}
