package com.pulse.player.playback

import com.pulse.player.data.db.PlayEventEntity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class SessionRecoveryTest {

    @Test
    fun recoversOpenSessionFromCheckpoint() = runTest {
        val prefs = FakeSharedPreferences()
        // Format: "$id|$startedWall|$durationMs|$realMs|$mediaMs"
        prefs.edit().putString("open", "track99|1700000000000|180000|40000|40000").apply()

        val inserted = mutableListOf<PlayEventEntity>()
        val testDispatcher = UnconfinedTestDispatcher(testScheduler)
        val tracker = SessionTracker(
            prefs = prefs,
            zone = { ZoneId.of("UTC") },
            scope = TestScope(testDispatcher),
            eventWriter = { inserted.add(it) }
        )

        tracker.recoverOpenSession()

        assertEquals(1, inserted.size)
        val event = inserted.first()
        assertEquals("track99", event.trackId)
        assertEquals(1700000000000L, event.playedAtMs)
        assertEquals(40000L, event.listenedMs)
        assertTrue(event.counted)
        assertEquals("UTC", event.zoneId)
        // Checkpoint must be cleared after recovery
        assertNull(prefs.getString("open", null))
    }

    @Test
    fun ignoresShortSessionOnRecovery() = runTest {
        val prefs = FakeSharedPreferences()
        prefs.edit().putString("open", "track1|1700000000000|180000|1500|1500").apply()

        val inserted = mutableListOf<PlayEventEntity>()
        val tracker = SessionTracker(
            prefs = prefs,
            eventWriter = { inserted.add(it) }
        )

        tracker.recoverOpenSession()

        assertTrue(inserted.isEmpty())
        assertNull(prefs.getString("open", null))
    }

    @Test
    fun corruptCheckpointHandledSafely() = runTest {
        val prefs = FakeSharedPreferences()
        prefs.edit().putString("open", "corrupt|data").apply()

        val inserted = mutableListOf<PlayEventEntity>()
        val tracker = SessionTracker(
            prefs = prefs,
            eventWriter = { inserted.add(it) }
        )

        tracker.recoverOpenSession()

        assertTrue(inserted.isEmpty())
        assertNull(prefs.getString("open", null))
    }

    @Test
    fun activeSessionWritesRunningCheckpoint() = runTest {
        val prefs = FakeSharedPreferences()
        var elapsed = 50_000L
        val tracker = SessionTracker(
            prefs = prefs,
            elapsedClock = { elapsed },
            wallClock = { 1700000000000L }
        )

        tracker.onTrackStarted("trackLive", dur = 120_000L, playing = true, speed = 1f)
        elapsed += 15_000L // 15s elapsed while running
        tracker.checkpoint()

        val open = prefs.getString("open", null)
        assertTrue(open != null && open.startsWith("trackLive|1700000000000|120000|15000|15000"))
    }
}

