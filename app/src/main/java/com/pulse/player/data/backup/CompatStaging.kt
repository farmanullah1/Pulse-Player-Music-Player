package com.pulse.player.data.backup

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

data class SrcPlaylist(
    val name: String,
    val sortKey: String?,
    val sortOrder: Int?,
    val paths: List<String>
)

data class SrcEvent(
    val path: String,
    val timestamp: Long,
    val durationMs: Long,
    val skipCount: Int
)

data class StagedBackup(
    val meta: Map<String, Any?>,
    val flags: Map<String, Boolean>,
    val tagCount: Int,
    val tags: Map<String, SrcTag>,
    val playlists: List<SrcPlaylist>,
    val events: List<SrcEvent>,
    val sortRaw: Map<String, Any?>?,
    val excludeDirs: List<String>,
    val hiddenFlagged: Int,
    val lyricsNonEmpty: Boolean,
    val snapshotsOk: Int,
    val snapshotsBad: Int,
    val warnings: List<String>
)

suspend fun stage(ctx: Context, zip: SafeZip): StagedBackup = withContext(Dispatchers.IO) {
    val warnings = ArrayList<String>()
    val meta = zip.open(Compat.META).use { it.readObject() }
    val flags = Compat.FLAGS.associateWith {
        (meta["settings"] as? Map<*, *>)?.get(it) as? Boolean ?: false
    }

    val tags = HashMap<String, SrcTag>()
    var tagCount = 0
    if (zip.has(Compat.TAGS)) {
        tagCount = zip.open(Compat.TAGS).use { s ->
            s.forEachObject { o ->
                val p = o.str("_data") ?: return@forEachObject
                tags[PathKey.abs(p)] = SrcTag(
                    path = p,
                    title = o.str("title").orEmpty(),
                    artist = unk(o.str("artist")),
                    album = unkAlbum(o.str("album"))
                )
            }
        }
    }

    val playlists = ArrayList<SrcPlaylist>()
    if (zip.has(Compat.PLAYLISTS)) {
        zip.open(Compat.PLAYLISTS).use { s ->
            s.forEachObject { o ->
                val name = o.str("name")?.trim().orEmpty()
                if (name.isEmpty()) return@forEachObject
                val sort = o["sort"] as? Map<*, *>
                val paths = (o["musics"] as? List<*>)?.mapNotNull {
                    (it as? Map<*, *>)?.get("path") as? String
                }.orEmpty()
                playlists += SrcPlaylist(
                    name = name,
                    sortKey = sort?.get("key") as? String,
                    sortOrder = (sort?.get("order") as? Number)?.toInt(),
                    paths = paths
                )
            }
        }
    }

    val events = ArrayList<SrcEvent>()
    if (zip.has(Compat.PLAYBACK)) {
        zip.open(Compat.PLAYBACK).use { s ->
            s.forEachObject { o ->
                val p = o.str("_data") ?: return@forEachObject
                val ts = o.lng("timestamp")
                if (ts <= 0) return@forEachObject
                events += SrcEvent(
                    path = p,
                    timestamp = ts,
                    durationMs = o.lng("play_duration"),
                    skipCount = o.int("skip_count")
                )
            }
        }
    }

    val sortRaw = if (zip.has(Compat.SORT)) zip.open(Compat.SORT).use { it.readObject() } else null

    var exclude = emptyList<String>()
    var hidden = 0
    if (zip.has(Compat.SCAN)) {
        zip.open(Compat.SCAN).use { s ->
            val o = s.readObject()
            exclude = (o["exclude"] as? List<*>)?.mapNotNull {
                (it as? Map<*, *>)?.get("data") as? String
            }.orEmpty()
            hidden = (o["hidden_songs"] as? List<*>)?.count {
                ((it as? Map<*, *>)?.get("is_hidden") as? Number)?.toInt() == 1
            } ?: 0
        }
    }

    var lyricsNonEmpty = false
    if (zip.has(Compat.LYRICS)) {
        zip.open(Compat.LYRICS).use { s ->
            lyricsNonEmpty = s.forEachObject { } > 0
        }
    }

    var ok = 0
    var bad = 0
    val snapEntries = zip.names.filter { Compat.SNAPSHOT_RE.matches(it) }
    for (n in snapEntries) {
        val tmp = File.createTempFile("snap_", ".db", ctx.cacheDir)
        try {
            zip.open(n).use { i -> tmp.outputStream().use { o -> i.copyTo(o) } }
            SQLiteDatabase.openDatabase(tmp.path, null, SQLiteDatabase.OPEN_READONLY).use { d ->
                d.rawQuery("SELECT COUNT(*) FROM statistics_playback", null).use { c ->
                    c.moveToFirst()
                }
            }
            ok++
        } catch (e: Exception) {
            bad++
            warnings += "Snapshot $n could not be read and was skipped."
        } finally {
            tmp.delete()
        }
    }

    StagedBackup(
        meta = meta,
        flags = flags,
        tagCount = tagCount,
        tags = tags,
        playlists = playlists,
        events = events,
        sortRaw = sortRaw,
        excludeDirs = exclude,
        hiddenFlagged = hidden,
        lyricsNonEmpty = lyricsNonEmpty,
        snapshotsOk = ok,
        snapshotsBad = bad,
        warnings = warnings
    )
}

