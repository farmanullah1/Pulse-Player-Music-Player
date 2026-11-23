package com.pulse.player.library

import com.pulse.player.data.db.PulseDb
import com.pulse.player.data.db.SmartPlaylistEntity
import com.pulse.player.data.db.TrackEntity
import com.pulse.player.playback.Track
import com.pulse.player.playback.toTrack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * Evaluates smart playlist rules against the live library.
 *
 * Rule schema (JSON stored in `smart_playlists.rulesJson`):
 *
 * ```json
 * {
 *   "match": "ALL" | "ANY",
 *   "conditions": [
 *     { "field": "artist", "op": "contains", "value": "..." },
 *     { "field": "play_count", "op": "greater_than", "value": "5" },
 *     ...
 *   ]
 * }
 * ```
 *
 * Supported fields: title, artist, album, albumArtist, genre, year, durationMs, sizeBytes,
 *   rating, playCount, lastPlayedMs, dateAddedMs, folder, language, bpm,
 *   isFavorite, isAvailable
 *
 * Supported operators: equals, not_equals, contains, not_contains, starts_with,
 *   greater_than, less_than, between, is_empty, is_not_empty
 *
 * Implements META-05 and META-06.
 */
class SmartPlaylistEngine(private val db: PulseDb) {

    // ─── Rule models ──────────────────────────────────────────────────────────

    enum class Match { ALL, ANY }

    enum class SmartField {
        TITLE, ARTIST, ALBUM, ALBUM_ARTIST, GENRE, CODEC, YEAR, DURATION_MS, SIZE_BYTES,
        RATING, PLAY_COUNT, LAST_PLAYED_MS, DATE_ADDED_MS, FOLDER, LANGUAGE, BPM,
        IS_FAVORITE, IS_AVAILABLE
    }

    enum class Operator {
        EQUALS, NOT_EQUALS, CONTAINS, NOT_CONTAINS, STARTS_WITH,
        GREATER_THAN, LESS_THAN, BETWEEN, IS_EMPTY, IS_NOT_EMPTY
    }

    data class Condition(
        val field: SmartField,
        val op: Operator,
        val value: String = "",
        val value2: String = "" // used for BETWEEN
    )

    data class SmartRules(
        val match: Match,
        val conditions: List<Condition>
    )

    // ─── Built-in collections ─────────────────────────────────────────────────

    /** Returns the built-in system smart collections as (name, rules) pairs. */
    fun builtInCollections(): List<Pair<String, SmartRules>> = companionBuiltInCollections()

    fun rulesFromJson(json: String): SmartRules? = Companion.rulesFromJson(json)

    fun rulesToJson(rules: SmartRules): String = Companion.rulesToJson(rules)

    // ─── Evaluation ───────────────────────────────────────────────────────────

    /**
     * Evaluate a smart playlist against the current library and history.
     * Returns tracks as [Track] models sorted/limited per the playlist definition.
     */
    suspend fun evaluate(
        entity: SmartPlaylistEntity,
        minDurationMs: Long = 30_000L
    ): List<Track> = withContext(Dispatchers.IO) {
        val rules = rulesFromJson(entity.rulesJson) ?: return@withContext emptyList()
        if (rules.conditions.isEmpty()) return@withContext emptyList()

        val allTracks = db.tracks().availableOnce().filter { it.durationMs >= minDurationMs }
        val favoriteIds = db.favorites().addedAtList().mapTo(HashSet()) { it.trackId }
        val recentEvents = db.stats().getRecentEvents(10000)
        val playCountMap = recentEvents
            .groupBy { it.trackId }
            .mapValues { it.value.count { ev -> ev.counted } }
        val lastPlayedMap = recentEvents
            .groupBy { it.trackId }
            .mapValues { it.value.maxOf { ev -> ev.playedAtMs } }
        val ratingMap = db.trackMeta().allOnce().associate { it.trackId to it.rating }

        evaluateInMemory(
            rules = rules,
            tracks = allTracks,
            favoriteIds = favoriteIds,
            playCountMap = playCountMap,
            lastPlayedMap = lastPlayedMap,
            ratingMap = ratingMap,
            sortKey = entity.sortKey,
            sortOrder = entity.sortOrder,
            limitCount = entity.limitCount
        )
    }

    fun evaluateInMemory(
        rules: SmartRules,
        tracks: List<TrackEntity>,
        favoriteIds: Set<String> = emptySet(),
        playCountMap: Map<String, Int> = emptyMap(),
        lastPlayedMap: Map<String, Long> = emptyMap(),
        ratingMap: Map<String, Int> = emptyMap(),
        sortKey: String? = null,
        sortOrder: Int? = null,
        limitCount: Int? = null
    ): List<Track> = Companion.evaluateInMemory(
        rules = rules,
        tracks = tracks,
        favoriteIds = favoriteIds,
        playCountMap = playCountMap,
        lastPlayedMap = lastPlayedMap,
        ratingMap = ratingMap,
        sortKey = sortKey,
        sortOrder = sortOrder,
        limitCount = limitCount
    )

    companion object {
        private fun thirtyDaysAgo(): Long = System.currentTimeMillis() - 30L * 24 * 3600 * 1000

        fun companionBuiltInCollections(): List<Pair<String, SmartRules>> = listOf(
            "Recently Added" to SmartRules(
                Match.ALL,
                listOf(Condition(SmartField.DATE_ADDED_MS, Operator.GREATER_THAN, thirtyDaysAgo().toString()))
            ),
            "Never Played" to SmartRules(
                Match.ALL,
                listOf(Condition(SmartField.PLAY_COUNT, Operator.EQUALS, "0"))
            ),
            "Favourites" to SmartRules(
                Match.ALL,
                listOf(Condition(SmartField.IS_FAVORITE, Operator.EQUALS, "true"))
            ),
            "Lossless Mix" to SmartRules(
                Match.ANY,
                listOf(
                    Condition(SmartField.CODEC, Operator.CONTAINS, "flac"),
                    Condition(SmartField.CODEC, Operator.CONTAINS, "wav"),
                    Condition(SmartField.CODEC, Operator.CONTAINS, "alac")
                )
            ),
            "Short Tracks" to SmartRules(
                Match.ALL,
                listOf(Condition(SmartField.DURATION_MS, Operator.LESS_THAN, "120000"))
            ),
            "Long Tracks" to SmartRules(
                Match.ALL,
                listOf(Condition(SmartField.DURATION_MS, Operator.GREATER_THAN, "420000"))
            ),
            "High Rated" to SmartRules(
                Match.ALL,
                listOf(Condition(SmartField.RATING, Operator.GREATER_THAN, "3"))
            )
        )

        fun rulesFromJson(json: String): SmartRules? = runCatching {
            val obj = JSONObject(json)
            val match = Match.valueOf(obj.optString("match", "ALL"))
            val arr = obj.getJSONArray("conditions")
            val conditions = (0 until arr.length()).mapNotNull { i ->
                val c = arr.getJSONObject(i)
                runCatching {
                    Condition(
                        field = SmartField.valueOf(c.getString("field")),
                        op = Operator.valueOf(c.getString("op")),
                        value = c.optString("value", ""),
                        value2 = c.optString("value2", "")
                    )
                }.getOrNull()
            }
            SmartRules(match, conditions)
        }.getOrNull()

        fun rulesToJson(rules: SmartRules): String {
            val arr = JSONArray()
            rules.conditions.forEach { c ->
                val obj = JSONObject()
                obj.put("field", c.field.name)
                obj.put("op", c.op.name)
                obj.put("value", c.value)
                if (c.value2.isNotEmpty()) obj.put("value2", c.value2)
                arr.put(obj)
            }
            return JSONObject().apply {
                put("match", rules.match.name)
                put("conditions", arr)
            }.toString()
        }

        fun evaluateInMemory(
            rules: SmartRules,
            tracks: List<TrackEntity>,
            favoriteIds: Set<String> = emptySet(),
            playCountMap: Map<String, Int> = emptyMap(),
            lastPlayedMap: Map<String, Long> = emptyMap(),
            ratingMap: Map<String, Int> = emptyMap(),
            sortKey: String? = null,
            sortOrder: Int? = null,
            limitCount: Int? = null
        ): List<Track> {
            if (rules.conditions.isEmpty()) return emptyList()
            val matching = tracks.filter { track ->
                val passes = rules.conditions.map { cond ->
                    evaluateCondition(cond, track, favoriteIds, playCountMap, lastPlayedMap, ratingMap)
                }
                when (rules.match) {
                    Match.ALL -> passes.all { it }
                    Match.ANY -> passes.any { it }
                }
            }

            val sorted = when (sortKey) {
                "title" -> matching.sortedBy { it.title.lowercase() }
                "artist" -> matching.sortedBy { it.artist.lowercase() }
                "dateAdded" -> matching.sortedByDescending { it.dateAddedMs }
                "playCount" -> matching.sortedByDescending { playCountMap[it.id] ?: 0 }
                "lastPlayed" -> matching.sortedByDescending { lastPlayedMap[it.id] ?: 0L }
                "duration" -> matching.sortedByDescending { it.durationMs }
                "year" -> matching.sortedByDescending { it.year }
                else -> matching.sortedByDescending { it.dateAddedMs }
            }.let { list ->
                if ((sortOrder ?: -1) > 0) list.reversed() else list
            }

            val limited = limitCount?.let { sorted.take(it) } ?: sorted
            return limited.map { it.toTrack(isFavorite = it.id in favoriteIds) }
        }

        private fun evaluateCondition(
            cond: Condition,
            track: TrackEntity,
            favoriteIds: Set<String>,
            playCountMap: Map<String, Int>,
            lastPlayedMap: Map<String, Long>,
            ratingMap: Map<String, Int>
        ): Boolean {
            val strVal = cond.value.lowercase()
            val numVal = cond.value.toLongOrNull()
            val numVal2 = cond.value2.toLongOrNull()

            return when (cond.field) {
                SmartField.TITLE        -> matchStr(cond.op, track.title.lowercase(), strVal)
                SmartField.ARTIST       -> matchStr(cond.op, track.artist.lowercase(), strVal)
                SmartField.ALBUM        -> matchStr(cond.op, track.album.lowercase(), strVal)
                SmartField.ALBUM_ARTIST -> matchStr(cond.op, (track.albumArtist ?: "").lowercase(), strVal)
                SmartField.GENRE        -> matchStr(cond.op, track.genre.lowercase(), strVal)
                SmartField.CODEC        -> matchStr(cond.op, track.codec.lowercase(), strVal)
                SmartField.LANGUAGE     -> matchStr(cond.op, track.language.lowercase(), strVal)
                SmartField.FOLDER       -> matchStr(cond.op, track.folderPath.lowercase(), strVal)
                SmartField.YEAR         -> numVal?.let { matchNum(cond.op, track.year.toLong(), it, numVal2) } ?: false
                SmartField.DURATION_MS  -> numVal?.let { matchNum(cond.op, track.durationMs, it, numVal2) } ?: false
                SmartField.SIZE_BYTES   -> numVal?.let { matchNum(cond.op, track.sizeBytes, it, numVal2) } ?: false
                SmartField.BPM          -> numVal?.let { matchNum(cond.op, track.bpm.toLong(), it, numVal2) } ?: false
                SmartField.DATE_ADDED_MS -> numVal?.let { matchNum(cond.op, track.dateAddedMs, it, numVal2) } ?: false
                SmartField.PLAY_COUNT   -> numVal?.let { matchNum(cond.op, (playCountMap[track.id] ?: 0).toLong(), it, numVal2) } ?: false
                SmartField.LAST_PLAYED_MS -> numVal?.let { matchNum(cond.op, lastPlayedMap[track.id] ?: 0L, it, numVal2) } ?: false
                SmartField.RATING       -> numVal?.let { matchNum(cond.op, (ratingMap[track.id] ?: 0).toLong(), it, numVal2) } ?: false
                SmartField.IS_FAVORITE  -> {
                    val isFav = favoriteIds.contains(track.id)
                    when (cond.op) {
                        Operator.EQUALS -> isFav == (cond.value.lowercase() == "true")
                        else -> false
                    }
                }
                SmartField.IS_AVAILABLE -> {
                    val isAvail = track.available
                    when (cond.op) {
                        Operator.EQUALS -> isAvail == (cond.value.lowercase() == "true")
                        else -> false
                    }
                }
            }
        }

        private fun matchStr(op: Operator, value: String, target: String): Boolean = when (op) {
            Operator.EQUALS       -> value == target
            Operator.NOT_EQUALS   -> value != target
            Operator.CONTAINS     -> value.contains(target)
            Operator.NOT_CONTAINS -> !value.contains(target)
            Operator.STARTS_WITH  -> value.startsWith(target)
            Operator.IS_EMPTY     -> value.isBlank()
            Operator.IS_NOT_EMPTY -> value.isNotBlank()
            else                  -> false
        }

        private fun matchNum(op: Operator, value: Long, target: Long, target2: Long?): Boolean = when (op) {
            Operator.EQUALS       -> value == target
            Operator.NOT_EQUALS   -> value != target
            Operator.GREATER_THAN -> value > target
            Operator.LESS_THAN    -> value < target
            Operator.BETWEEN      -> target2 != null && value in target..target2
            else                  -> false
        }
    }
}
