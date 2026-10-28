package com.pulse.player.data.favorites

import com.pulse.player.data.db.TrackEntity
import kotlin.math.abs

sealed interface MatchResult {
    data class Matched(val item: FavItem, val track: TrackEntity, val level: String) : MatchResult // "strong" | "high" | "medium"
    data class AlreadyFavorite(val item: FavItem, val track: TrackEntity) : MatchResult
    data class Ambiguous(val item: FavItem, val candidates: List<TrackEntity>) : MatchResult
    data class Missing(val item: FavItem) : MatchResult
    data class Duplicate(val item: FavItem) : MatchResult
}

class FavoritesMatcher(
    private val library: List<TrackEntity>,
    private val favoriteIds: Set<String>
) {
    private val byTitleArtist = library.groupBy { it.normTitle to it.normArtist }
    private val byTitle = library.groupBy { it.normTitle }

    fun match(items: List<FavItem>): List<MatchResult> {
        val seen = HashSet<String>()
        val out = ArrayList<MatchResult>(items.size)

        for (it in items) {
            val key = "${it.normTitle}|${it.normArtist}|${it.durationMs / 2000}"
            if (!seen.add(key)) {
                out += MatchResult.Duplicate(it)
                continue
            }

            // 1) strong: same file name + size + near duration
            val strong = library.filter { t ->
                t.sizeBytes == it.sizeBytes && it.sizeBytes > 0 &&
                        t.displayName.equals(it.displayName, true) &&
                        abs(t.durationMs - it.durationMs) <= 2000
            }

            // 2) high: title + artist + duration within 2s
            val ta = byTitleArtist[it.normTitle to it.normArtist].orEmpty()
            val high = ta.filter { t -> abs(t.durationMs - it.durationMs) <= 2000 }

            // 3) medium: title + album, duration within 5s
            val medium = byTitle[it.normTitle].orEmpty().filter { t ->
                t.normAlbum == it.normAlbum && abs(t.durationMs - it.durationMs) <= 5000
            }

            val (cands, level) = when {
                strong.size == 1 -> strong to "strong"
                high.size == 1 -> high to "high"
                high.isEmpty() && medium.size == 1 -> medium to "medium"
                else -> (strong.ifEmpty { high.ifEmpty { medium } }) to "multi"
            }

            out += when {
                cands.isEmpty() -> MatchResult.Missing(it)
                cands.size > 1 || level == "multi" -> MatchResult.Ambiguous(it, cands)
                cands[0].id in favoriteIds -> MatchResult.AlreadyFavorite(it, cands[0])
                else -> MatchResult.Matched(it, cands[0], level)
            }
        }
        return out
    }
}
