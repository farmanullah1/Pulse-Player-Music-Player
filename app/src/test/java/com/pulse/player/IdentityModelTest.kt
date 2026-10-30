package com.pulse.player

import com.pulse.player.data.db.IdentityModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class IdentityModelTest {

    @Test
    fun mediaStoreId_formatAndStability() {
        val id1 = IdentityModel.mediaStoreId(12345L)
        val id2 = IdentityModel.mediaStoreId(12345L)
        assertEquals("ms:12345", id1)
        assertEquals(id1, id2)
    }

    @Test
    fun safId_stableAcrossPathChanges() {
        val treeUri = "content://com.android.externalstorage.documents/tree/primary%3AMusic"
        val docId = "primary:Music/Artist/Song.mp3"

        val safId1 = IdentityModel.safId(treeUri, docId)
        val safId2 = IdentityModel.safId(treeUri, docId)

        assertEquals(safId1, safId2)
        assertTrue(safId1.startsWith("saf:"))
        // Ensure changing documentId produces different ID
        val safId3 = IdentityModel.safId(treeUri, "primary:Music/Artist/Other.mp3")
        assertNotEquals(safId1, safId3)
    }

    @Test
    fun copyId_formatAndStability() {
        val destUri = "content://com.android.externalstorage.documents/document/primary%3AMusic%2FCopy.mp3"
        val copyId1 = IdentityModel.copyId(destUri)
        val copyId2 = IdentityModel.copyId(destUri)

        assertEquals(copyId1, copyId2)
        assertTrue(copyId1.startsWith("copy:"))
    }

    @Test
    fun sha1_producesConsistentHex() {
        val hash = IdentityModel.sha1("hello world")
        assertEquals("2aae6c35c94fcfb415dbe95f408b9ce91ee846ed", hash)
    }
}

