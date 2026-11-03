package com.pulse.player.playback

import com.pulse.player.data.db.PlayEventEntity
import com.pulse.player.data.db.PulseDb
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.Calendar

/**
 * The sole class permitted to insert records into `play_events` outside of live playback.
 *
 * This class is one of exactly two callers of `play_events` inserts (the other is
 * [SessionTracker] for live listening). This satisfies the grep gate from C5:
 * "only `SessionTracker` and `HistoryWriter` call `play_events` inserts."
 *
 * Use cases:
 * - Importing history from Last.fm, Spotify, ListenBrainz, YouTube (HIST-13)
 * - Manual history entries added or edited by the user (NAM-S1)
 * - Restoring play events from a Full backup (preserves `origin` column values)
 *
 * All records written here include a non-LIVE [origin] value so they can be
 * distinguished from live listening data and excluded from certain statistics
 * if the user wishes.
 */
class HistoryWriter(private val db: PulseDb) {

    /**
     * Allowed origin values for non-live history records.
     * The 'LIVE' origin is reserved exclusively for [SessionTracker].
     */
    enum class ImportOrigin(val code: String) {
        LAST_FM("IMPORT_LASTFM"),
        SPOTIFY("IMPORT_SPOTIFY"),
        LISTEN_BRAINZ("IMPORT_LISTENBRAINZ"),
        YOUTUBE("IMPORT_YOUTUBE"),
        MANUAL("MANUAL")
    }

    data class ImportRecord(
        val trackId: String,
        val playedAtMs: Long,
        val listenedMs: Long,
        val origin: ImportOrigin
    )

    data class ImportResult(
        val inserted: Int,
        val skipped: Int,         // duplicates or invalid records
        val unmatched: Int,       // track IDs not found in library
        val errors: List<String>
    )

    /**
     * Write imported history records to the database.
     *
     * - Records with a `LIVE` origin are silently rejected (that path is exclusive to SessionTracker).
     * - Records whose track ID does not exist in the library are counted as [ImportResult.unmatched].
     * - Duplicate records (same trackId + playedAtMs) are silently skipped (unique index on those columns).
     * - All inserts are batched in a single transaction.
     *
     * @param records   The import records to write.
     * @return          An [ImportResult] summarising what happened.
     */
    suspend fun importRecords(records: List<ImportRecord>): ImportResult = withContext(Dispatchers.IO) {
        var inserted = 0
        var skipped = 0
        var unmatched = 0
        val errors = mutableListOf<String>()

        // Pre-load valid track IDs for this import to avoid N+1 queries
        val allIds = db.tracks().availableOnce().map { it.id }.toSet()

        val batch = mutableListOf<PlayEventEntity>()
        val zoneId = try { ZoneId.systemDefault().id } catch (_: Exception) { "UTC" }

        for (record in records) {
            if (record.trackId.isBlank()) { skipped++; continue }
            if (!allIds.contains(record.trackId)) { unmatched++; continue }
            if (record.playedAtMs <= 0L)   { skipped++; continue }

            val monthKey = monthKeyFor(record.playedAtMs)

            batch += PlayEventEntity(
                trackId = record.trackId,
                playedAtMs = record.playedAtMs,
                listenedMs = maxOf(0L, record.listenedMs),
                completed = record.listenedMs > 0,
                zoneId = zoneId,
                monthKey = monthKey,
                playedFraction = 0f,
                wasSkipped = false,
                speedAvg = 1f,
                counted = record.listenedMs > 0,
                origin = record.origin.code
            )
        }

        if (batch.isNotEmpty()) {
            runCatching {
                // insertEvents uses IGNORE conflict strategy — duplicates silently skipped
                val rowIds = db.stats().insertEvents(batch)
                inserted = rowIds.count { it > 0 }
                skipped += rowIds.count { it <= 0 }
            }.onFailure { e ->
                errors += "Batch insert failed: ${e.message}"
            }
        }

        ImportResult(inserted, skipped, unmatched, errors)
    }

    /**
     * Insert a single manually-edited or manually-added record.
     * Returns true if the record was inserted, false if it was a duplicate.
     */
    suspend fun insertManual(
        trackId: String,
        playedAtMs: Long,
        listenedMs: Long
    ): Boolean = withContext(Dispatchers.IO) {
        val zoneId = try { ZoneId.systemDefault().id } catch (_: Exception) { "UTC" }
        val entity = PlayEventEntity(
            trackId = trackId,
            playedAtMs = playedAtMs,
            listenedMs = listenedMs,
            completed = listenedMs > 0,
            zoneId = zoneId,
            monthKey = monthKeyFor(playedAtMs),
            playedFraction = 0f,
            wasSkipped = false,
            speedAvg = 1f,
            counted = listenedMs > 0,
            origin = "MANUAL"
        )
        runCatching { db.stats().insert(entity); true }.getOrDefault(false)
    }

    private fun monthKeyFor(timestampMs: Long): String {
        return try {
            val cal = Calendar.getInstance().also { it.timeInMillis = timestampMs }
            "%04d%02d".format(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1)
        } catch (_: Exception) { "" }
    }
}
