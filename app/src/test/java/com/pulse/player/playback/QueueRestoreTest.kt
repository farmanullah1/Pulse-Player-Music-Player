package com.pulse.player.playback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class QueueRestoreTest {

    @Test
    fun restoreQueuePreservesPositionWhenCurrentAvailable() {
        val snapshot = QueueSnapshot(
            original = listOf("t1", "t2", "t3"),
            shuffled = emptyList(),
            currentId = "t2",
            positionMs = 45000L,
            repeat = 0,
            shuffleEnabled = false,
            seed = 0L,
            savedAtMs = 1700000000000L
        )

        val restored = restoreQueue(snapshot, setOf("t1", "t2", "t3"))
        assertNotNull(restored)
        assertEquals("t2", restored!!.model.currentId)
        assertEquals(45000L, restored.positionMs)
        assertTrue(restored.startPaused)
    }

    @Test
    fun restoreQueueFallsBackToNearestSurvivorWhenCurrentDeleted() {
        // Active order was t1, t2, t3, t4. Current was t2, but t2 was deleted.
        val snapshot = QueueSnapshot(
            original = listOf("t1", "t2", "t3", "t4"),
            shuffled = emptyList(),
            currentId = "t2",
            positionMs = 30000L,
            repeat = 0,
            shuffleEnabled = false,
            seed = 0L,
            savedAtMs = 1700000000000L
        )

        // Only t1, t3, t4 survive. Nearest survivor at or after index 1 is t3.
        val restored = restoreQueue(snapshot, setOf("t1", "t3", "t4"))
        assertNotNull(restored)
        assertEquals("t3", restored!!.model.currentId)
        assertEquals(0L, restored.positionMs) // Never silently play a different track at old position
        assertTrue(restored.startPaused)
    }

    @Test
    fun restoreQueueFallsBackToPriorSurvivorWhenNoSubsequentSurvivor() {
        // Current was t3 (the last item). Only t1 and t2 survive.
        val snapshot = QueueSnapshot(
            original = listOf("t1", "t2", "t3"),
            shuffled = emptyList(),
            currentId = "t3",
            positionMs = 60000L,
            repeat = 0,
            shuffleEnabled = false,
            seed = 0L,
            savedAtMs = 1700000000000L
        )

        val restored = restoreQueue(snapshot, setOf("t1", "t2"))
        assertNotNull(restored)
        assertEquals("t2", restored!!.model.currentId)
        assertEquals(0L, restored.positionMs)
        assertTrue(restored.startPaused)
    }

    @Test
    fun restoreQueueReturnsNullWhenAllTracksUnavailable() {
        val snapshot = QueueSnapshot(
            original = listOf("t1", "t2"),
            shuffled = emptyList(),
            currentId = "t1",
            positionMs = 1000L,
            repeat = 0,
            shuffleEnabled = false,
            seed = 0L,
            savedAtMs = 1700000000000L
        )

        val restored = restoreQueue(snapshot, emptySet())
        assertNull(restored)
    }

    @Test
    fun queueSnapshotJsonRoundTrip() {
        val originalSnapshot = QueueSnapshot(
            original = listOf("trackA", "trackB", "trackC"),
            shuffled = listOf("trackB", "trackC", "trackA"),
            currentId = "trackB",
            positionMs = 123456L,
            repeat = 1,
            shuffleEnabled = true,
            seed = 987654321L,
            savedAtMs = 1711122233344L
        )

        val json = originalSnapshot.toJson()
        val parsed = QueueSnapshot.fromJson(json)

        assertNotNull(parsed)
        assertEquals(originalSnapshot.original, parsed!!.original)
        assertEquals(originalSnapshot.shuffled, parsed.shuffled)
        assertEquals(originalSnapshot.currentId, parsed.currentId)
        assertEquals(originalSnapshot.positionMs, parsed.positionMs)
        assertEquals(originalSnapshot.repeat, parsed.repeat)
        assertEquals(originalSnapshot.shuffleEnabled, parsed.shuffleEnabled)
        assertEquals(originalSnapshot.seed, parsed.seed)
        assertEquals(originalSnapshot.savedAtMs, parsed.savedAtMs)
    }

    @Test
    fun corruptJsonReturnsNullWithoutCrashing() {
        assertNull(QueueSnapshot.fromJson(null))
        assertNull(QueueSnapshot.fromJson(""))
        assertNull(QueueSnapshot.fromJson("   "))
        assertNull(QueueSnapshot.fromJson("{ not valid json }"))
        assertNull(QueueSnapshot.fromJson("{\"unrelated\": 42}"))
    }

    @Test
    fun noticeFormat() {
        assertEquals("1 song in your last queue is unavailable", formatDroppedTracksNotice(1))
        assertEquals("3 songs in your last queue are unavailable", formatDroppedTracksNotice(3))
        assertNull(formatDroppedTracksNotice(0))
        assertNull(formatDroppedTracksNotice(-1))
    }
}

