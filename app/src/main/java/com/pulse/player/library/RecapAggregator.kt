package com.pulse.player.library

import com.pulse.player.data.db.PlayEventEntity
import com.pulse.player.data.db.TrackEntity
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId

data class DailyListenPoint(
    val dayOfMonth: Int,
    val totalMs: Long
)

data class CircadianBreakdown(
    val morningMs: Long,   // 05:00 - 11:59
    val afternoonMs: Long, // 12:00 - 16:59
    val eveningMs: Long,   // 17:00 - 21:59
    val nightMs: Long      // 22:00 - 04:59
) {
    val totalMs: Long get() = morningMs + afternoonMs + eveningMs + nightMs

    fun dominantLabel(): String {
        val maxVal = maxOf(morningMs, afternoonMs, eveningMs, nightMs)
        if (maxVal <= 0L) return "Balanced Listener"
        return when (maxVal) {
            morningMs -> "Morning Riser (5AM–12PM)"
            afternoonMs -> "Afternoon Groove (12PM–5PM)"
            eveningMs -> "Evening Unwind (5PM–10PM)"
            else -> "Night Owl (10PM–5AM)"
        }
    }
}

data class GenreListenStat(
    val genre: String,
    val totalMs: Long,
    val plays: Int
)

data class RecapAnalytics(
    val dailyPoints: List<DailyListenPoint>,
    val peakDayOfMonth: Int?,
    val peakDayMs: Long,
    val activeDaysCount: Int,
    val circadian: CircadianBreakdown,
    val topGenres: List<GenreListenStat>
)

object RecapAggregator {
    fun aggregate(
        month: YearMonth,
        events: List<PlayEventEntity>,
        tracksById: Map<String, TrackEntity>,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): RecapAnalytics {
        val daysInMonth = month.lengthOfMonth()
        val dailyMs = LongArray(daysInMonth + 1)

        var morning = 0L
        var afternoon = 0L
        var evening = 0L
        var night = 0L

        val genreTotals = LinkedHashMap<String, Pair<Long, Int>>()

        for (ev in events) {
            if (!ev.counted || ev.listenedMs <= 0L) continue
            val zdt = Instant.ofEpochMilli(ev.playedAtMs).atZone(zoneId)
            if (zdt.year == month.year && zdt.monthValue == month.monthValue) {
                val dom = zdt.dayOfMonth
                if (dom in 1..daysInMonth) {
                    dailyMs[dom] += ev.listenedMs
                }
            }
            when (zdt.hour) {
                in 5..11 -> morning += ev.listenedMs
                in 12..16 -> afternoon += ev.listenedMs
                in 17..21 -> evening += ev.listenedMs
                else -> night += ev.listenedMs
            }

            val rawGenre = tracksById[ev.trackId]?.genre?.trim().orEmpty()
            if (rawGenre.isNotBlank()) {
                val splitGenres = ArtistParser.splitGenres(rawGenre).ifEmpty { listOf(rawGenre) }
                for (g in splitGenres) {
                    val prev = genreTotals[g] ?: (0L to 0)
                    genreTotals[g] = (prev.first + ev.listenedMs) to (prev.second + 1)
                }
            }
        }

        val dailyPoints = (1..daysInMonth).map { day ->
            DailyListenPoint(dayOfMonth = day, totalMs = dailyMs[day])
        }
        val peak = dailyPoints.maxByOrNull { it.totalMs }
        val activeDays = dailyPoints.count { it.totalMs > 0L }

        val topGenres = genreTotals.entries
            .map { GenreListenStat(genre = it.key, totalMs = it.value.first, plays = it.value.second) }
            .sortedByDescending { it.totalMs }
            .take(5)

        return RecapAnalytics(
            dailyPoints = dailyPoints,
            peakDayOfMonth = peak?.takeIf { it.totalMs > 0L }?.dayOfMonth,
            peakDayMs = peak?.totalMs ?: 0L,
            activeDaysCount = activeDays,
            circadian = CircadianBreakdown(morning, afternoon, evening, night),
            topGenres = topGenres
        )
    }
}
