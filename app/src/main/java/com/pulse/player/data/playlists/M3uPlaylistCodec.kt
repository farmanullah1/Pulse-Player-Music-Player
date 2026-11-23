package com.pulse.player.data.playlists

import com.pulse.player.data.scan.norm
import com.pulse.player.playback.Track
import java.net.URLDecoder

data class M3uEntry(
    val title: String,
    val artist: String?,
    val durationSec: Int,
    val location: String
)

data class M3uImportResult(
    val playlistName: String,
    val entries: List<M3uEntry>,
    val matchedTracks: List<Track>,
    val unmatchedLocations: List<String>
)

object M3uPlaylistCodec {

    fun exportM3u8(playlistName: String, tracks: List<Track>): String = buildString {
        appendLine("#EXTM3U")
        val cleanName = playlistName.replace('\n', ' ').replace('\r', ' ').trim()
        if (cleanName.isNotBlank()) {
            appendLine("#PLAYLIST:$cleanName")
        }
        for (t in tracks) {
            val sec = (t.durationMs / 1000L).coerceAtLeast(0L)
            val artistPart = t.artist.takeIf { it.isNotBlank() && !it.equals("Unknown artist", ignoreCase = true) }
            val extTitle = if (artistPart != null) "$artistPart - ${t.title}" else t.title
            appendLine("#EXTINF:$sec,$extTitle")
            val target = t.dataPath.ifBlank { t.uri }
            appendLine(target)
        }
    }

    fun parse(content: String, fallbackName: String = "Imported Playlist"): Pair<String, List<M3uEntry>> {
        var playlistName = fallbackName.trim().ifBlank { "Imported Playlist" }
        var pendingDuration = -1
        var pendingArtist: String? = null
        var pendingTitle: String? = null
        val entries = ArrayList<M3uEntry>()

        for (rawLine in content.lineSequence()) {
            val line = rawLine.trim().trimStart('\uFEFF')
            if (line.isBlank()) continue
            if (line.startsWith("#PLAYLIST:", ignoreCase = true)) {
                val parsed = line.substringAfter(':').trim()
                if (parsed.isNotBlank()) playlistName = parsed
                continue
            }
            if (line.startsWith("#EXTINF:", ignoreCase = true)) {
                val meta = line.substringAfter(':')
                val durPart = meta.substringBefore(',').trim()
                pendingDuration = durPart.toIntOrNull() ?: -1
                val displayPart = meta.substringAfter(',', "").trim()
                if (displayPart.contains(" - ")) {
                    pendingArtist = displayPart.substringBefore(" - ").trim().takeIf { it.isNotBlank() }
                    pendingTitle = displayPart.substringAfter(" - ").trim().takeIf { it.isNotBlank() }
                } else {
                    pendingArtist = null
                    pendingTitle = displayPart.takeIf { it.isNotBlank() }
                }
                continue
            }
            if (line.startsWith("#")) continue

            val fallbackStem = line
                .substringAfterLast('/')
                .substringAfterLast('\\')
                .substringBeforeLast('.')
                .ifBlank { line }
            entries += M3uEntry(
                title = pendingTitle ?: fallbackStem,
                artist = pendingArtist,
                durationSec = pendingDuration,
                location = line
            )
            pendingDuration = -1
            pendingArtist = null
            pendingTitle = null
        }
        return playlistName to entries
    }

    fun resolveMatches(
        playlistName: String,
        entries: List<M3uEntry>,
        library: List<Track>
    ): M3uImportResult {
        if (entries.isEmpty() || library.isEmpty()) {
            return M3uImportResult(
                playlistName = playlistName,
                entries = entries,
                matchedTracks = emptyList(),
                unmatchedLocations = entries.map { it.location }
            )
        }

        val byUri = HashMap<String, Track>(library.size)
        val byPath = HashMap<String, Track>(library.size)
        val byFileName = HashMap<String, MutableList<Track>>(library.size)
        val byArtistTitle = HashMap<String, MutableList<Track>>(library.size)
        val byTitle = HashMap<String, MutableList<Track>>(library.size)

        for (t in library) {
            if (t.uri.isNotBlank()) byUri.putIfAbsent(t.uri.lowercase(), t)
            if (t.dataPath.isNotBlank()) {
                val normPath = t.dataPath.replace('\\', '/').lowercase()
                byPath.putIfAbsent(normPath, t)
                val fname = normPath.substringAfterLast('/')
                if (fname.isNotBlank()) {
                    byFileName.getOrPut(fname) { mutableListOf() }.add(t)
                }
            }
            val nTitle = t.normTitle.ifBlank { norm(t.title) }
            val nArtist = t.normArtist.ifBlank { norm(t.artist) }
            if (nTitle.isNotBlank()) {
                byArtistTitle.getOrPut("$nArtist|$nTitle") { mutableListOf() }.add(t)
                byTitle.getOrPut(nTitle) { mutableListOf() }.add(t)
            }
        }

        val matched = ArrayList<Track>(entries.size)
        val matchedIds = HashSet<String>(entries.size)
        val unmatched = ArrayList<String>()

        for (entry in entries) {
            val candidate = findMatch(entry, byUri, byPath, byFileName, byArtistTitle, byTitle)
            if (candidate != null) {
                if (matchedIds.add(candidate.id)) {
                    matched += candidate
                }
            } else {
                unmatched += entry.location
            }
        }

        return M3uImportResult(
            playlistName = playlistName,
            entries = entries,
            matchedTracks = matched,
            unmatchedLocations = unmatched
        )
    }

    private fun findMatch(
        entry: M3uEntry,
        byUri: Map<String, Track>,
        byPath: Map<String, Track>,
        byFileName: Map<String, List<Track>>,
        byArtistTitle: Map<String, List<Track>>,
        byTitle: Map<String, List<Track>>
    ): Track? {
        val rawLoc = entry.location.trim()
        val decodedLoc = runCatching {
            if (rawLoc.startsWith("file://", ignoreCase = true)) {
                URLDecoder.decode(rawLoc.removePrefix("file://"), "UTF-8")
            } else {
                rawLoc
            }
        }.getOrDefault(rawLoc)

        byUri[rawLoc.lowercase()]?.let { return it }

        val unixLoc = decodedLoc.replace('\\', '/').lowercase()
        byPath[unixLoc]?.let { return it }

        val fileName = unixLoc.substringAfterLast('/')
        val fileCandidates = byFileName[fileName]
        if (!fileCandidates.isNullOrEmpty()) {
            if (fileCandidates.size == 1) return fileCandidates.first()
            if (entry.durationSec > 0) {
                val targetMs = entry.durationSec * 1000L
                return fileCandidates.minByOrNull { kotlin.math.abs(it.durationMs - targetMs) }
            }
            return fileCandidates.first()
        }

        val nTitle = norm(entry.title)
        val nArtist = entry.artist?.let(::norm).orEmpty()
        if (nTitle.isNotBlank() && nArtist.isNotBlank()) {
            byArtistTitle["$nArtist|$nTitle"]?.firstOrNull()?.let { return it }
        }
        if (nTitle.isNotBlank()) {
            val candidates = byTitle[nTitle]
            if (!candidates.isNullOrEmpty()) {
                if (candidates.size == 1) return candidates.first()
                if (entry.durationSec > 0) {
                    val targetMs = entry.durationSec * 1000L
                    candidates.minByOrNull { kotlin.math.abs(it.durationMs - targetMs) }?.let { return it }
                }
            }
        }
        return null
    }
}
