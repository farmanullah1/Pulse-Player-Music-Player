package com.pulse.player.playback

import android.content.SharedPreferences
import com.pulse.player.data.db.PlayEventEntity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId

class FakeSharedPreferences : SharedPreferences {
    val map = mutableMapOf<String, Any?>()

    override fun getString(key: String?, defValue: String?): String? = map[key] as? String ?: defValue
    override fun getStringSet(key: String?, defValues: Set<String>?): Set<String>? =
        (map[key] as? Set<*>)?.filterIsInstance<String>()?.toSet() ?: defValues
    override fun getInt(key: String?, defValue: Int): Int = map[key] as? Int ?: defValue
    override fun getLong(key: String?, defValue: Long): Long = map[key] as? Long ?: defValue
    override fun getFloat(key: String?, defValue: Float): Float = map[key] as? Float ?: defValue
    override fun getBoolean(key: String?, defValue: Boolean): Boolean = map[key] as? Boolean ?: defValue
    override fun contains(key: String?): Boolean = map.containsKey(key)
    override fun edit(): SharedPreferences.Editor = Editor()
    override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}
    override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}

    inner class Editor : SharedPreferences.Editor {
        private val temp = mutableMapOf<String, Any?>()
        private var clear = false

        override fun putString(key: String?, value: String?): SharedPreferences.Editor { temp[key ?: ""] = value; return this }
        override fun putStringSet(key: String?, values: Set<String>?): SharedPreferences.Editor { temp[key ?: ""] = values; return this }
        override fun putInt(key: String?, value: Int): SharedPreferences.Editor { temp[key ?: ""] = value; return this }
        override fun putLong(key: String?, value: Long): SharedPreferences.Editor { temp[key ?: ""] = value; return this }
        override fun putFloat(key: String?, value: Float): SharedPreferences.Editor { temp[key ?: ""] = value; return this }
        override fun putBoolean(key: String?, value: Boolean): SharedPreferences.Editor { temp[key ?: ""] = value; return this }
        override fun remove(key: String?): SharedPreferences.Editor { temp[key ?: ""] = null; return this }
        override fun clear(): SharedPreferences.Editor { clear = true; return this }
        override fun commit(): Boolean { apply(); return true }
        override fun apply() {
            if (clear) map.clear()
            temp.forEach { (k, v) -> if (v == null) map.remove(k) else map[k] = v }
        }
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class SessionTrackerTest {

    @Test
    fun statsPolicyCountedRule() {
        // Track duration 180s (3 minutes). Counted requires 30s.
        assertFalse(StatsPolicy.counted(real = 29_000L, media = 29_000L, dur = 180_000L))
        assertTrue(StatsPolicy.counted(real = 30_000L, media = 30_000L, dur = 180_000L))

        // Short track 20s. 50% is 10s. Counted requires min(30s, max(5s, 10s)) = 10s.
        assertFalse(StatsPolicy.counted(real = 9_000L, media = 9_000L, dur = 20_000L))
        assertTrue(StatsPolicy.counted(real = 10_000L, media = 10_000L, dur = 20_000L))
    }

    @Test
    fun statsPolicySkippedRule() {
        // Left by user before 25% of 100s track (i.e. media < 25s) and not counted
        assertTrue(StatsPolicy.skipped(real = 10_000L, media = 10_000L, dur = 100_000L, userLeft = true))

        // Natural end / unprompted end is not a skip
        assertFalse(StatsPolicy.skipped(real = 10_000L, media = 10_000L, dur = 100_000L, userLeft = false))

        // Left after 30s (counted = true) is not a skip
        assertFalse(StatsPolicy.skipped(real = 35_000L, media = 35_000L, dur = 100_000L, userLeft = true))
    }

    @Test
    fun ignoresAccidentalTapsUnderThreshold() = runTest {
        val inserted = mutableListOf<PlayEventEntity>()
        var currentTime = 10_000L
        val testDispatcher = UnconfinedTestDispatcher(testScheduler)
        val tracker = SessionTracker(
            prefs = FakeSharedPreferences(),
            elapsedClock = { currentTime },
            wallClock = { 1700000000000L },
            scope = TestScope(testDispatcher),
            eventWriter = { inserted.add(it) }
        )

        tracker.onTrackStarted("track1", dur = 100_000L, playing = true, speed = 1f)
        currentTime += 1_500L // 1.5s is below IGNORE_BELOW_REAL_MS (2s)
        tracker.onTrackLeft(userAction = true)

        assertTrue(inserted.isEmpty())
    }

    @Test
    fun recordsSpeedAwareListeningSession() = runTest {
        val inserted = mutableListOf<PlayEventEntity>()
        var currentTime = 1_000L
        val testDispatcher = UnconfinedTestDispatcher(testScheduler)
        val tracker = SessionTracker(
            prefs = FakeSharedPreferences(),
            zone = { ZoneId.of("UTC") },
            elapsedClock = { currentTime },
            wallClock = { 1700000000000L },
            scope = TestScope(testDispatcher),
            eventWriter = { inserted.add(it) }
        )

        // Listen for 20s real time at 1.5x speed
        tracker.onTrackStarted("trackA", dur = 60_000L, playing = true, speed = 1.5f)
        currentTime += 20_000L // 20s wall time
        tracker.onTrackLeft(userAction = false)

        assertEquals(1, inserted.size)
        val event = inserted.first()
        assertEquals("trackA", event.trackId)
        assertEquals(20_000L, event.listenedMs)
        assertEquals(1.5f, event.speedAvg, 0.01f)
        // Media time is 20s * 1.5 = 30s, which is 50% of 60s track -> counted = true
        assertTrue(event.counted)
        assertFalse(event.wasSkipped)
    }

    @Test
    fun pauseAndResumeDoesNotAccumulatePausedTime() = runTest {
        val inserted = mutableListOf<PlayEventEntity>()
        var currentTime = 100_000L
        val testDispatcher = UnconfinedTestDispatcher(testScheduler)
        val tracker = SessionTracker(
            prefs = FakeSharedPreferences(),
            zone = { ZoneId.of("UTC") },
            elapsedClock = { currentTime },
            wallClock = { 1700000000000L },
            scope = TestScope(testDispatcher),
            eventWriter = { inserted.add(it) }
        )

        tracker.onTrackStarted("trackB", dur = 120_000L, playing = true, speed = 1.0f)
        currentTime += 10_000L // 10s played

        tracker.onPlayingChanged(playing = false, speed = 1.0f) // Paused
        currentTime += 50_000L // Paused for 50s (should not be counted)

        tracker.onPlayingChanged(playing = true, speed = 1.0f) // Resumed
        currentTime += 10_000L // Another 10s played

        tracker.onTrackLeft(userAction = false)

        assertEquals(1, inserted.size)
        val event = inserted.first()
        assertEquals(20_000L, event.listenedMs) // Only the 10s + 10s played time
    }
}

