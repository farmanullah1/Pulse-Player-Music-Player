package com.pulse.player.data.backup

import android.content.Context
import androidx.room.withTransaction
import com.pulse.player.data.db.FavoriteEntity
import com.pulse.player.data.db.PlayEventEntity
import com.pulse.player.data.db.PlaylistEntity
import com.pulse.player.data.db.PlaylistTrackEntity
import com.pulse.player.data.db.PulseDb
import com.pulse.player.data.db.TrackEntity
import com.pulse.player.data.settings.SettingsStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

data class MatchStats(
    val tier: IntArray = IntArray(6),
    var ambiguous: Int = 0,
    var missing: Int = 0
)

data class RestoreOptions(
    val categories: Set<String>,
    val replace: Boolean
)

data class CompatRestorePreview(
    val staged: StagedBackup,
    val stats: MatchStats,
    val favResolved: Int,
    val playlistResolved: Int,
    val eventsResolved: Int,
    val unmatched: List<String>
)

data class CompatRestoreReport(
    val lines: List<String>,
    val unmatchedPaths: List<String>
)

private val FAV_NAMES = setOf("my favourite", "my favorite", "my favourites", "my favorites")

suspend fun previewRestore(db: PulseDb, s: StagedBackup): CompatRestorePreview = withContext(Dispatchers.IO) {
    val idx = LibraryIndex(db.tracks().availableOnce())
    val stats = MatchStats()
    val unmatched = LinkedHashSet<String>()

    fun resolve(path: String): TrackEntity? = when (val r = idx.resolve(path, s.tags[PathKey.abs(path)])) {
        is Resolve.Hit -> {
            stats.tier[r.tier]++
            r.track
        }
        Resolve.Ambiguous -> {
            stats.ambiguous++
            unmatched += "AMBIGUOUS  $path"
            null
        }
        Resolve.Missing -> {
            stats.missing++
            unmatched += "NOT FOUND  $path"
            null
        }
    }

    var favResolved = 0
    var playlistResolved = 0
    for (p in s.playlists) {
        val ids = p.paths.mapNotNull { resolve(it)?.id }.distinct()
        if (p.name.lowercase(Locale.ROOT) in FAV_NAMES) {
            favResolved += ids.size
        } else {
            playlistResolved += ids.size
        }
    }

    var eventsResolved = 0
    for (e in s.events) {
        if (resolve(e.path) != null) {
            eventsResolved++
        }
    }

    CompatRestorePreview(
        staged = s,
        stats = stats,
        favResolved = favResolved,
        playlistResolved = playlistResolved,
        eventsResolved = eventsResolved,
        unmatched = unmatched.toList()
    )
}

suspend fun applyRestore(
    ctx: Context,
    db: PulseDb,
    settings: SettingsStore,
    s: StagedBackup,
    o: RestoreOptions
): CompatRestoreReport = withContext(Dispatchers.IO) {
    val idx = LibraryIndex(db.tracks().availableOnce())
    val lines = ArrayList<String>()
    val unmatched = LinkedHashSet<String>()
    val stats = MatchStats()

    fun resolve(path: String): TrackEntity? = when (val r = idx.resolve(path, s.tags[PathKey.abs(path)])) {
        is Resolve.Hit -> {
            stats.tier[r.tier]++
            r.track
        }
        Resolve.Ambiguous -> {
            stats.ambiguous++
            unmatched += "AMBIGUOUS  $path"
            null
        }
        Resolve.Missing -> {
            stats.missing++
            unmatched += "NOT FOUND  $path"
            null
        }
    }

    val now = System.currentTimeMillis()

    // 1. Stage in memory (no DB writes yet)
    val doPlaylists = "isBackupPlaylists" in o.categories
    val favIds = ArrayList<String>()
    val plans = ArrayList<Triple<SrcPlaylist, List<String>, Int>>()
    if (doPlaylists) {
        for (p in s.playlists) {
            val ids = p.paths.mapNotNull { resolve(it)?.id }.distinct()
            if (p.name.lowercase(Locale.ROOT) in FAV_NAMES) {
                favIds += ids
            } else {
                plans += Triple(p, ids, p.paths.size - ids.size)
            }
        }
    }

    val doStats = "isBackupMusicRecap" in o.categories
    val events = if (doStats) {
        s.events.mapNotNull { e ->
            resolve(e.path)?.let { t ->
                PlayEventEntity(
                    trackId = t.id,
                    playedAtMs = e.timestamp,
                    listenedMs = e.durationMs.coerceAtLeast(0),
                    completed = e.skipCount == 0
                )
            }
        }
    } else {
        emptyList()
    }

    // 2. Commit once inside a single Room transaction
    db.withTransaction {
        if (doPlaylists) {
            if (o.replace) {
                db.favorites().clear()
            }
            val base = now
            db.favorites().addAll(favIds.mapIndexed { i, id -> FavoriteEntity(id, base - i) })
            lines += "Favourites: ${favIds.size} resolved"

            for ((p, ids, _) in plans) {
                val existing = db.playlists().byName(p.name)
                val pid = existing?.id ?: db.playlists().create(PlaylistEntity(name = p.name, createdAtMs = now))
                if (existing != null && o.replace) {
                    db.playlists().clearTracks(pid)
                }
                var pos = db.playlists().nextPos(pid)
                for (id in ids) {
                    db.playlists().addTrack(PlaylistTrackEntity(pid, id, pos++, now))
                }
                db.playlists().setSort(pid, p.sortKey, p.sortOrder)
            }
            lines += "Playlists: ${plans.size} processed"
        }

        if (doStats) {
            if (o.replace) {
                db.stats().clearEvents()
            }
            val inserted = db.stats().insertEvents(events).count { it != -1L }
            lines += "Play history: $inserted new, ${events.size - inserted} already present"
        }
    }

    // 3. Settings updates (DataStore, safely after DB commit)
    if ("isBackupSortSettings" in o.categories && s.sortRaw != null) {
        settings.saveSortRaw(s.sortRaw)
        lines += "Sort settings applied"
    }

    if ("isBackupScanHideSettings" in o.categories) {
        val rel = s.excludeDirs.map { PathKey.rel(it).trim('/') }.filter { it.isNotEmpty() }
        settings.addExcludedFolders(rel.toSet())
        lines += "Excluded folders added: ${rel.size} (rescan to apply)"
        if (s.hiddenFlagged > 0) {
            lines += "Hidden songs not supported: ${s.hiddenFlagged} skipped"
        }
    }

    if ("isBackupMusicTags" in o.categories) {
        lines += "Tags: ${s.tagCount} read and used for matching (not written)"
    }
    if ("isBackupCovers" in o.categories) {
        lines += "Covers not applied: they are specific to the source device"
    }
    if ("isBackupLyrics" in o.categories) {
        lines += if (s.lyricsNonEmpty) "Lyrics skipped: format not recognised" else "Lyrics: none in backup"
    }

    lines += "Matched by path ${stats.tier[1]}, relative path ${stats.tier[2]}, folder+name ${stats.tier[3]}, tags ${stats.tier[4]}, name ${stats.tier[5]}; ambiguous ${stats.ambiguous}; not found ${stats.missing}"

    CompatRestoreReport(lines + s.warnings, unmatched.toList())
}
