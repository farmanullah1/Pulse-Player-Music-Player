package com.pulse.player.library

import com.pulse.player.data.scan.norm
import com.pulse.player.playback.Track

/**
 * In-memory ranked search over the local library.
 *
 * Implements SRCH-01 (typo-tolerant, ranked results) with:
 * - Exact match → highest rank
 * - Prefix match → high rank
 * - Contains (word boundary) → medium rank
 * - Trigram similarity → low rank (typo tolerance)
 *
 * All matching is case-insensitive and accent-normalized using [norm].
 *
 * This is an additive feature (SRCH-01–SRCH-05); does not replace the existing
 * search flow in SearchScreen — it provides a ranked result provider.
 */
object SearchIndex {

    data class SearchResult(
        val track: Track,
        val score: Float,
        val matchedFields: Set<String>
    )

    /**
     * Search [tracks] for [query] across the configured fields.
     * Returns results sorted by score descending with a minimum score threshold.
     *
     * @param tracks     Candidate tracks to search.
     * @param query      The user's search query.
     * @param maxResults Maximum number of results to return.
     * @param scoreMin   Minimum score threshold (0.0–1.0).
     */
    fun search(
        tracks: List<Track>,
        query: String,
        maxResults: Int = 200,
        scoreMin: Float = 0.1f
    ): List<SearchResult> {
        if (query.isBlank()) return emptyList()
        val q = norm(query)
        if (q.isBlank()) return emptyList()

        val qTokens = q.split(Regex("\\s+")).filter { it.length >= 2 }

        return tracks
            .mapNotNull { track ->
                val (score, fields) = scoreTrack(track, q, qTokens)
                if (score >= scoreMin) SearchResult(track, score, fields) else null
            }
            .sortedByDescending { it.score }
            .take(maxResults)
    }

    private fun scoreTrack(track: Track, query: String, queryTokens: List<String>): Pair<Float, Set<String>> {
        val candidates = mapOf(
            "title"  to norm(track.title),
            "artist" to norm(track.artist),
            "album"  to norm(track.album)
        )

        var totalScore = 0f
        val matchedFields = mutableSetOf<String>()

        for ((field, value) in candidates) {
            val fieldScore = scoreField(query, queryTokens, value)
            if (fieldScore > 0f) {
                // Weight title higher than artist/album
                val weight = if (field == "title") 1.0f else 0.7f
                totalScore += fieldScore * weight
                matchedFields.add(field)
            }
        }

        return totalScore to matchedFields
    }

    private fun scoreField(query: String, queryTokens: List<String>, value: String): Float {
        if (value.isBlank()) return 0f

        // 1. Exact match
        if (value == query) return 1.0f

        // 2. Prefix match
        if (value.startsWith(query)) return 0.9f

        // 3. Contains full query
        if (value.contains(query)) return 0.75f

        // 4. All tokens present (any order)
        if (queryTokens.isNotEmpty() && queryTokens.all { value.contains(it) }) return 0.6f

        // 5. Any token is a prefix of a word in value
        val valueWords = value.split(Regex("\\s+"))
        val prefixMatches = queryTokens.count { t -> valueWords.any { w -> w.startsWith(t) } }
        if (prefixMatches > 0) return 0.5f * (prefixMatches.toFloat() / queryTokens.size)

        // 6. Trigram similarity for typo tolerance
        val similarity = trigramSimilarity(query, value)
        if (similarity >= 0.4f) return similarity * 0.4f

        return 0f
    }

    /**
     * Simple trigram Jaccard similarity between two strings.
     * Returns 0.0–1.0.
     */
    private fun trigramSimilarity(a: String, b: String): Float {
        if (a.length < 3 || b.length < 3) return 0f
        val aGrams = trigrams(a)
        val bGrams = trigrams(b)
        if (aGrams.isEmpty() || bGrams.isEmpty()) return 0f
        val intersection = (aGrams intersect bGrams).size
        val union = (aGrams union bGrams).size
        return if (union == 0) 0f else intersection.toFloat() / union.toFloat()
    }

    private fun trigrams(s: String): Set<String> {
        if (s.length < 3) return emptySet()
        return (0..s.length - 3).map { s.substring(it, it + 3) }.toSet()
    }
}
