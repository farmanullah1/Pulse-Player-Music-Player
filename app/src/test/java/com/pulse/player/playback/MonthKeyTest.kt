package com.pulse.player.playback

import com.pulse.player.data.db.PlayEventEntity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class MonthKeyTest {

    @Test
    fun formatsMonthKeyCorrectly() {
        val instant = Instant.parse("2026-10-29T12:00:00Z")
        val z = ZoneId.of("UTC")
        val dt = instant.atZone(z)
        val monthKey = "%04d%02d".format(dt.year, dt.monthValue)
        assertEquals("202610", monthKey)

        val janInstant = Instant.parse("2027-01-05T08:00:00Z")
        val janDt = janInstant.atZone(z)
        val janKey = "%04d%02d".format(janDt.year, janDt.monthValue)
        assertEquals("202701", janKey)
    }

    @Test
    fun monthKeyRespectsEventZoneAtWriteTime() = runTest {
        // Timestamp: 2026-10-31 23:30:00 UTC
        // In UTC, this is still October 2026 -> 202610
        // In Tokyo (UTC+9), this is 2026-11-01 08:30:00 -> 202611
        val timestamp = Instant.parse("2026-10-31T23:30:00Z").toEpochMilli()

        val utcEvents = mutableListOf<PlayEventEntity>()
        val testDispatcher = UnconfinedTestDispatcher(testScheduler)
        val utcTracker = SessionTracker(
            prefs = FakeSharedPreferences(),
            zone = { ZoneId.of("UTC") },
            elapsedClock = { 0L },
            wallClock = { timestamp },
            scope = TestScope(testDispatcher),
            eventWriter = { utcEvents.add(it) }
        )

        utcTracker.onTrackStarted("track1", dur = 60_000L, playing = true, speed = 1f)
        utcTracker.onTrackLeft(userAction = false)

        assertEquals("202610", utcEvents.first().monthKey)
        assertEquals("UTC", utcEvents.first().zoneId)

        val tokyoEvents = mutableListOf<PlayEventEntity>()
        val tokyoTracker = SessionTracker(
            prefs = FakeSharedPreferences(),
            zone = { ZoneId.of("Asia/Tokyo") },
            elapsedClock = { 0L },
            wallClock = { timestamp },
            scope = TestScope(testDispatcher),
            eventWriter = { tokyoEvents.add(it) }
        )

        tokyoTracker.onTrackStarted("track1", dur = 60_000L, playing = true, speed = 1f)
        tokyoTracker.onTrackLeft(userAction = false)

        assertEquals("202611", tokyoEvents.first().monthKey)
        assertEquals("Asia/Tokyo", tokyoEvents.first().zoneId)
    }
}

