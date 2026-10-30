package com.pulse.player.playback

import androidx.media3.common.Player
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class QueueModelTest {

    @Test
    fun distinctOriginalTracks() {
        val model = QueueModel(listOf("t1", "t2", "t1", "t3", "t2"), "t1")
        assertEquals(listOf("t1", "t2", "t3"), model.original)
    }

    @Test
    fun enableShuffleKeepsCurrentFirst() {
        val list = listOf("a", "b", "c", "d", "e")
        val model = QueueModel(list, "c")
        model.enableShuffle(12345L)

        assertTrue(model.shuffleEnabled)
        assertEquals("c", model.shuffled.first())
        assertEquals(5, model.shuffled.size)
        assertEquals(setOf("a", "b", "c", "d", "e"), model.shuffled.toSet())
        assertEquals("c", model.active.first())
        assertEquals(0, model.currentIndex)
    }

    @Test
    fun shuffleIsDeterministicForSeed() {
        val list = listOf("1", "2", "3", "4", "5", "6", "7", "8")
        val model1 = QueueModel(list, "3")
        model1.enableShuffle(999L)

        val model2 = QueueModel(list, "3")
        model2.enableShuffle(999L)

        assertEquals(model1.shuffled, model2.shuffled)

        val model3 = QueueModel(list, "3")
        model3.enableShuffle(888L)
        assertFalse(model1.shuffled == model3.shuffled)
    }

    @Test
    fun disableShuffleReturnsToOriginalSuccessor() {
        val list = listOf("a", "b", "c", "d")
        val model = QueueModel(list, "b")
        model.enableShuffle(42L)

        assertTrue(model.shuffleEnabled)
        assertEquals("b", model.currentId)

        model.disableShuffle()

        assertFalse(model.shuffleEnabled)
        assertEquals(list, model.active)
        assertEquals("b", model.currentId)
        assertEquals("c", model.nextId(Player.REPEAT_MODE_OFF))
    }

    @Test
    fun nextIdRespectsRepeatModes() {
        val list = listOf("1", "2", "3")
        val model = QueueModel(list, "3") // at the end

        assertNull(model.nextId(Player.REPEAT_MODE_OFF))
        assertEquals("1", model.nextId(Player.REPEAT_MODE_ALL))
        assertEquals("3", model.nextId(Player.REPEAT_MODE_ONE))
    }

    @Test
    fun previousIdReturnsPredecessor() {
        val list = listOf("x", "y", "z")
        val model = QueueModel(list, "y")
        assertEquals("x", model.previousId())

        val atFirst = QueueModel(list, "x")
        assertNull(atFirst.previousId())
    }

    @Test
    fun playNextInsertsAfterCurrent() {
        val model = QueueModel(listOf("a", "b", "c"), "a")
        model.playNext("x")
        assertEquals(listOf("a", "x", "b", "c"), model.original)

        // playNext of an existing item moves it right after current
        model.playNext("c")
        assertEquals(listOf("a", "c", "x", "b"), model.original)
    }

    @Test
    fun appendAddsToBackAndRejectsDuplicates() {
        val model = QueueModel(listOf("a", "b"), "a")
        assertTrue(model.append("c"))
        assertEquals(listOf("a", "b", "c"), model.original)

        assertFalse(model.append("b"))
        assertEquals(listOf("a", "b", "c"), model.original)
    }

    @Test
    fun removeTrackAdvancesIfCurrent() {
        val model = QueueModel(listOf("a", "b", "c"), "b")
        val newCurrent = model.remove("b")
        assertEquals("c", newCurrent)
        assertEquals("c", model.currentId)
        assertEquals(listOf("a", "c"), model.original)

        // Remove last item when it is current
        val newCurLast = model.remove("c")
        assertEquals("a", newCurLast)
        assertEquals("a", model.currentId)
    }

    @Test
    fun moveReordersItems() {
        val model = QueueModel(listOf("a", "b", "c", "d"), "a")
        model.move(0, 2)
        assertEquals(listOf("b", "c", "a", "d"), model.original)
    }
}

