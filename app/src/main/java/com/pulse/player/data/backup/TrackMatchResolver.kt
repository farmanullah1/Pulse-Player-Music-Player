package com.pulse.player.data.backup

import com.pulse.player.data.db.TrackEntity
import com.pulse.player.data.scan.norm
import kotlin.math.abs

data class BackupTrackRef(
    val id: String? = null,
    val title: String,
    val artist: String = "",
    val album: String = "",
    val durationMs: Long = 0L,
    val displayName: String = "",
    val sizeBytes: Long = 0L
)

sealed interface TrackMatch {
    data class Matched(val target: TrackEntity, val confidence: Confidence) : TrackMatch
    data class Ambiguous(val candidates: List<TrackEntity>) : TrackMatch
    object Unmatched : TrackMatch

    enum class Confidence {
        EXACT_ID,
        METADATA_EXACT,
        FILENAME_SIZE,
        METADATA_ALBUM
    }
}

class TrackMatchResolver(private val library: List<TrackEntity>) {

    private val idMap = library.associateBy { it.id }
    private val titleArtistMap = library.groupBy { "${it.normTitle}|${it.normArtist}" }
    private val displayNameSizeMap = library.groupBy { "${it.displayName.lowercase().trim()}|${it.sizeBytes}" }
    private val titleAlbumMap = library.groupBy { "${it.normTitle}|${it.normAlbum}" }

    fun resolve(ref: BackupTrackRef): TrackMatch {
        // Tier 1: Exact stable ID
        if (!ref.id.isNullOrBlank()) {
            val byId = idMap[ref.id]
            if (byId != null) {
                return TrackMatch.Matched(byId, TrackMatch.Confidence.EXACT_ID)
            }
        }

        val qTitle = norm(ref.title)
        val qArtist = norm(ref.artist)
        val qAlbum = norm(ref.album)

        // Tier 2: Normalized Title + Artist + Duration (tolerance 2500ms)
        if (qTitle.isNotBlank()) {
            val key = "$qTitle|$qArtist"
            val candidates = titleArtistMap[key].orEmpty()
            val durationMatched = candidates.filter { c ->
                if (ref.durationMs <= 0 || c.durationMs <= 0) true
                else abs(c.durationMs - ref.durationMs) <= 2500L
            }
            if (durationMatched.size == 1) {
                return TrackMatch.Matched(durationMatched.first(), TrackMatch.Confidence.METADATA_EXACT)
            } else if (durationMatched.size > 1) {
                return TrackMatch.Ambiguous(durationMatched)
            }
        }

        // Tier 3: Display Name + File Size
        if (ref.displayName.isNotBlank() && ref.sizeBytes > 0) {
            val key = "${ref.displayName.lowercase().trim()}|${ref.sizeBytes}"
            val candidates = displayNameSizeMap[key].orEmpty()
            if (candidates.size == 1) {
                return TrackMatch.Matched(candidates.first(), TrackMatch.Confidence.FILENAME_SIZE)
            } else if (candidates.size > 1) {
                return TrackMatch.Ambiguous(candidates)
            }
        }

        // Tier 4: Normalized Title + Album + Duration (tolerance 4000ms)
        if (qTitle.isNotBlank() && qAlbum.isNotBlank()) {
            val key = "$qTitle|$qAlbum"
            val candidates = titleAlbumMap[key].orEmpty()
            val durationMatched = candidates.filter { c ->
                if (ref.durationMs <= 0 || c.durationMs <= 0) true
                else abs(c.durationMs - ref.durationMs) <= 4000L
            }
            if (durationMatched.size == 1) {
                return TrackMatch.Matched(durationMatched.first(), TrackMatch.Confidence.METADATA_ALBUM)
            } else if (durationMatched.size > 1) {
                return TrackMatch.Ambiguous(durationMatched)
            }
        }

        return TrackMatch.Unmatched
    }
}
