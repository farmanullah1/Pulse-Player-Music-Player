package com.pulse.player

import com.pulse.player.data.scan.ArtRef
import com.pulse.player.data.scan.ArtworkLoader
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class ArtworkKeyTest {

    @Test
    fun sizeBucketing_mapsToCanonicalSizes() {
        assertEquals(96, ArtworkLoader.bucket(50))
        assertEquals(96, ArtworkLoader.bucket(96))
        assertEquals(192, ArtworkLoader.bucket(100))
        assertEquals(192, ArtworkLoader.bucket(192))
        assertEquals(384, ArtworkLoader.bucket(256))
        assertEquals(384, ArtworkLoader.bucket(384))
        assertEquals(768, ArtworkLoader.bucket(512))
        assertEquals(768, ArtworkLoader.bucket(1024))
    }

    @Test
    fun key_changesWhenModifiedMsChanges() {
        val ref1 = ArtRef(stableKey = "ms:100", uri = "content://media/100", modifiedMs = 1000L)
        val ref2 = ArtRef(stableKey = "ms:100", uri = "content://media/100", modifiedMs = 2000L)

        val key1 = ArtworkLoader.key(ref1, 96)
        val key2 = ArtworkLoader.key(ref2, 96)

        assertEquals("ms:100|1000|96", key1)
        assertEquals("ms:100|2000|96", key2)
        assertNotEquals(key1, key2)
    }

    @Test
    fun key_stableForSameRefAndBucket() {
        val ref = ArtRef(stableKey = "saf:abc", uri = "content://doc/1", modifiedMs = 5000L)
        val keyA = ArtworkLoader.key(ref, 120)
        val keyB = ArtworkLoader.key(ref, 180)
        assertEquals(keyA, keyB) // Both bucket to 192
        assertEquals("saf:abc|5000|192", keyA)
    }
}

