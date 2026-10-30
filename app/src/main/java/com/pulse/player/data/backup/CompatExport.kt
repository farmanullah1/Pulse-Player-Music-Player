package com.pulse.player.data.backup

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import android.os.Build
import android.util.JsonWriter
import androidx.room.withTransaction
import com.pulse.player.data.backup.full.FullBackupValidator
import com.pulse.player.data.backup.full.ValidationResult
import com.pulse.player.data.db.PlaybackRow
import com.pulse.player.data.db.PlaylistEntity
import com.pulse.player.data.db.PulseDb
import com.pulse.player.data.db.TrackEntity
import com.pulse.player.data.settings.SettingsStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedOutputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

private data class CompatSnapshot(
    val tracks: List<TrackEntity>,
    val withPath: List<TrackEntity>,
    val fav: List<TrackEntity>,
    val lists: List<PlaylistEntity>,
    val rows: List<PlaybackRow>
)

data class ExportCats(
    val tags: Boolean = true,
    val covers: Boolean = true,
    val lyrics: Boolean = true,
    val playlists: Boolean = true,
    val sort: Boolean = true,
    val scan: Boolean = true,
    val recap: Boolean = true
)

suspend fun exportCompat(
    ctx: Context,
    db: PulseDb,
    settings: SettingsStore,
    cats: ExportCats,
    dest: Uri,
    onStage: (String) -> Unit
): Result<File> = withContext(Dispatchers.IO) {
    runCatching {
        val tmp = File.createTempFile("backup_", ".zip", ctx.cacheDir)
        val snapFiles = ArrayList<File>()
        try {
            val snapshot = db.withTransaction {
                val t = db.tracks().availableOnce()
                val wp = t.filter { !it.dataPath.isNullOrBlank() }
                val f = db.favorites().allTracksOnce()
                val l = db.playlists().allPlaylistsOnce()
                val r = db.stats().playbackRows().filter { !it.dataPath.isNullOrBlank() }
                CompatSnapshot(t, wp, f, l, r)
            }
            val tracks = snapshot.tracks
            val withPath = snapshot.withPath
            val fav = snapshot.fav
            val lists = snapshot.lists
            val rows = snapshot.rows
            val zone = ZoneId.systemDefault()
            val nowYm = YearMonth.now(zone)

            val entryHashes = mutableMapOf<String, Pair<Long, String>>()

            ZipOutputStream(BufferedOutputStream(FileOutputStream(tmp))).use { z ->
                suspend fun entry(name: String, write: suspend (OutputStream) -> Unit) {
                    onStage(name)
                    val baos = ByteArrayOutputStream()
                    write(baos)
                    val bytes = baos.toByteArray()
                    val md = MessageDigest.getInstance("SHA-256")
                    val sha = md.digest(bytes).joinToString("") { "%02x".format(it) }
                    entryHashes[name] = Pair(bytes.size.toLong(), sha)

                    z.putNextEntry(ZipEntry(name))
                    z.write(bytes)
                    z.closeEntry()
                }

                val compact = setOf(
                    Compat.TAGS,
                    Compat.MUSIC_COVERS,
                    Compat.ALBUM_COVERS,
                    Compat.PLAYLISTS,
                    Compat.PLAYBACK
                )

                suspend fun json(name: String, w: suspend JsonWriter.() -> Unit) = entry(name) { o ->
                    JsonWriter(OutputStreamWriter(o, Charsets.UTF_8)).apply {
                        if (name !in compact) setIndent("  ")
                        w()
                        flush()
                    }
                }

                val ver = ctx.packageManager.getPackageInfo(ctx.packageName, 0)
                val verName = ver.versionName ?: "1.0.0"
                val verCode = if (Build.VERSION.SDK_INT >= 28) ver.longVersionCode.toInt() else @Suppress("DEPRECATION") ver.versionCode
                val deviceId = settings.installId()

                val flagMap = mapOf(
                    "isBackupMusicTags" to cats.tags,
                    "isBackupCovers" to cats.covers,
                    "isBackupLyrics" to cats.lyrics,
                    "isBackupPlaylists" to cats.playlists,
                    "isBackupSortSettings" to cats.sort,
                    "isBackupScanHideSettings" to cats.scan,
                    "isBackupMusicRecap" to cats.recap
                )

                // 1. settings.bak
                json(Compat.SETTINGS) {
                    beginObject()
                    flagMap.forEach { (k, v) -> name(k).value(v) }
                    name("device").value(deviceId)
                    name("versionName").value(verName)
                    name("versionCode").value(verCode.toLong())
                    endObject()
                }

                // 2. Covers/MusicCoversBackupConfig.conf
                // 3. Covers/AlbumCoversBackupConfig.conf
                if (cats.covers) {
                    json(Compat.MUSIC_COVERS) {
                        beginArray()
                        withPath.forEach {
                            beginObject()
                            name("_data").value(it.dataPath)
                            name("resource").nullValue()
                            name("cover_url").nullValue()
                            endObject()
                        }
                        endArray()
                    }

                    json(Compat.ALBUM_COVERS) {
                        beginArray()
                        withPath.map { it.album }.distinct().forEach {
                            beginObject()
                            name("album").value(it)
                            name("album_art").nullValue()
                            name("resource").nullValue()
                            name("modified_album_art").nullValue()
                            endObject()
                        }
                        endArray()
                    }
                }

                // 4. MusicTagsBackupConfig.conf
                if (cats.tags) {
                    json(Compat.TAGS) {
                        beginArray()
                        withPath.forEach { t ->
                            beginObject()
                            name("_data").value(t.dataPath)
                            name("title").value(t.title)
                            name("artist").value(if (t.artist == "Unknown artist") "<unknown>" else t.artist)
                            name("album").value(t.album)
                            name("track").value(t.trackNumber.toLong())
                            name("genres").nullValue()
                            endObject()
                        }
                        endArray()
                    }
                }

                // 5. Lyrics/LyricsBackupConfig.conf
                if (cats.lyrics) {
                    json(Compat.LYRICS) {
                        beginArray()
                        endArray()
                    }
                }

                // 6. PlayLists/PlayListBackupConfig.conf
                if (cats.playlists) {
                    json(Compat.PLAYLISTS) {
                        beginArray()
                        fun writePl(name: String, key: String, order: Int, paths: List<String>) {
                            beginObject()
                            name("name").value(name)
                            name("sort").beginObject()
                                .name("key").value(key)
                                .name("order").value(order.toLong())
                                .endObject()
                            name("cover").nullValue()
                            name("musics").beginArray()
                            paths.forEach {
                                beginObject()
                                name("path").value(it)
                                endObject()
                            }
                            endArray()
                            endObject()
                        }

                        // My Favourite is written first (array order = our display order)
                        writePl("My Favourite", "playlistPlayOrder", -1, fav.mapNotNull { it.dataPath })

                        for (p in lists) {
                            val trackIds = db.playlists().trackIds(p.id)
                            val tracksById = db.tracks().byIds(trackIds).associateBy { it.id }
                            val paths = trackIds.mapNotNull { tracksById[it]?.dataPath }
                            writePl(p.name, p.sortKey ?: "playlistPlayOrder", p.sortOrder ?: -1, paths)
                        }
                        endArray()
                    }
                }

                // 7. SortBackupConfig.conf
                if (cats.sort) {
                    json(Compat.SORT) {
                        val raw = settings.sortRawOrNull()
                        beginObject()
                        for (k in listOf("All_Song", "Directory_", "Folder_Song", "Album_", "Album_Song", "Artist_", "Artist_Song", "Genre_", "Genre_Song")) {
                            val (key, order) = settings.sortFor(k, raw)
                            name(k).beginObject()
                                .name("key").value(key)
                                .name("order").value(order.toLong())
                                .endObject()
                        }
                        endObject()
                    }
                }

                // 8. ScanHiddenBackupConfig.conf
                if (cats.scan) {
                    json(Compat.SCAN) {
                        beginObject()
                        name("size").value(-1L)
                        name("duration").value(-1L)
                        name("device").value(deviceId)
                        name("exclude").beginArray()
                        settings.excludedFoldersNow().forEachIndexed { i, rel ->
                            beginObject()
                            name("_id").value((i + 1).toLong())
                            name("data").value("/storage/emulated/0/" + rel.trim('/'))
                            name("type").value(1L)
                            endObject()
                        }
                        endArray()
                        name("hidden_songs").beginArray()
                        withPath.forEach {
                            beginObject()
                            name("path").value(it.dataPath)
                            name("is_hidden").value(0L)
                            endObject()
                        }
                        endArray()
                        endObject()
                    }
                }

                // 9-15. Statistics (Snapshots DBs + Config + PlaybackRecords)
                if (cats.recap) {
                    val months = rows.groupBy {
                        YearMonth.from(Instant.ofEpochMilli(it.playedAtMs).atZone(zone))
                    }.filterKeys { it < nowYm }

                    for ((ym, list) in months.toSortedMap()) {
                        val snapDb = buildSnapshot(ctx, ym, list, Build.MODEL)
                        snapFiles += snapDb
                        val entryName = Compat.snapshot("%04d%02d".format(ym.year, ym.monthValue))
                        entry(entryName) { o ->
                            snapDb.inputStream().use { it.copyTo(o) }
                        }
                    }

                    json(Compat.STATS_CFG) {
                        beginObject()
                        name("snapshots").beginArray()
                        months.keys.sorted().forEach { ym ->
                            val v = "%04d%02d".format(ym.year, ym.monthValue)
                            beginObject()
                            name("type").value("monthly")
                            name("value").value(v)
                            name("filename").value("snapshot_monthly_$v.db")
                            endObject()
                        }
                        endArray()
                        name("initApplicationTimestamp").value(rows.firstOrNull()?.playedAtMs ?: System.currentTimeMillis())
                        endObject()
                    }

                    json(Compat.PLAYBACK) {
                        beginArray()
                        rows.forEach { r ->
                            val d = Instant.ofEpochMilli(r.playedAtMs).atZone(zone).toLocalDate()
                            val songId = r.trackId.substringAfter(':').toLongOrNull() ?: (r.trackId.hashCode().toLong() and 0x7fffffffL)
                            beginObject()
                            name("song_id").value(songId)
                            name("timestamp").value(r.playedAtMs)
                            name("day").value("%04d%02d%02d".format(d.year, d.monthValue, d.dayOfMonth))
                            name("looper_count").value(0L)
                            name("source_type").value("")
                            name("source_id").value("")
                            name("play_duration").value(r.listenedMs)
                            name("play_count").value(1L)
                            name("device").value(Build.MODEL)
                            name("skip_count").value(if (r.completed) 0L else 1L)
                            for (i in 1..3) name("extra_int_$i").value(0L)
                            for (i in 1..3) name("extra_text_$i").nullValue()
                            name("_data").value(r.dataPath)
                            endObject()
                        }
                        endArray()
                    }
                }

                // 16. metadata.inf
                val stamp = SimpleDateFormat("yyyyMMdd_HHmmssSSS", Locale.US).format(Date())
                json(Compat.META) {
                    beginObject()
                    name("backupTime").value(stamp)
                    name("mode").value("MANUAL_GLOBAL")
                    name("type").value("GLOBAL")
                    name("isAutoBackup").value(false)
                    name("deviceId").value(deviceId)
                    name("versionName").value(verName)
                    name("versionCode").value(verCode.toLong())
                    name("settings").beginObject()
                    flagMap.forEach { (k, v) -> name(k).value(v) }
                    endObject()
                    name("songs").value(withPath.size.toLong())
                    name("playlists").value((lists.size + 1).toLong())
                    endObject()
                }

                // 17. pulse.manifest.json (written LAST)
                val manifestObj = JSONObject().apply {
                    put("format", "pulse-full-1")
                    put("createdAt", Instant.now().toString())
                    val arr = JSONArray()
                    entryHashes.forEach { (name, info) ->
                        arr.put(JSONObject().apply {
                            put("name", name)
                            put("size", info.first)
                            put("sha256", info.second)
                        })
                    }
                    put("entries", arr)
                    put("counts", JSONObject().apply {
                        put("favorites", fav.size)
                        put("playlists", lists.size)
                        put("events", rows.size)
                    })
                }
                val manifestBytes = manifestObj.toString(2).toByteArray(Charsets.UTF_8)
                z.putNextEntry(ZipEntry("pulse.manifest.json"))
                z.write(manifestBytes)
                z.closeEntry()
            }

            // Self-check validation before writing to SAF destination
            val checkResult = FullBackupValidator.validate(tmp)
            if (checkResult is ValidationResult.Invalid) {
                error("Export self-check failed: ${checkResult.reason}")
            }

            ctx.contentResolver.openOutputStream(dest, "wt")!!.use { out ->
                tmp.inputStream().use { it.copyTo(out) }
                out.flush()
            }

            val copied = ctx.contentResolver.openInputStream(dest)!!.use { i ->
                var n = 0L
                val b = ByteArray(65536)
                while (true) {
                    val r = i.read(b)
                    if (r < 0) break
                    n += r
                }
                n
            }
            check(copied == tmp.length()) { "The saved file size does not match; backup may be truncated." }

            // Persist last backup record
            val stamp = SimpleDateFormat("yyyyMMdd_HHmmssSSS", Locale.US).format(Date())
            val recordJson = JSONObject().apply {
                put("timestampMs", System.currentTimeMillis())
                put("backupTime", stamp)
                put("sizeBytes", tmp.length())
                put("songs", withPath.size)
                put("playlists", lists.size + 1)
            }.toString()
            settings.setLastBackup(recordJson)

            tmp
        } finally {
            snapFiles.forEach { it.delete() }
        }
    }
}

fun buildSnapshot(
    ctx: Context,
    ym: YearMonth,
    rows: List<PlaybackRow>,
    model: String
): File {
    val f = File.createTempFile("snap_${ym.year}${ym.monthValue}_", ".db", ctx.cacheDir)
    val db = SQLiteDatabase.openOrCreateDatabase(f, null)
    try {
        db.execSQL("CREATE TABLE statistics_playback(_id INT, song_id INT, timestamp INT, day TEXT, looper_count INT, source_type TEXT, source_id TEXT, play_duration INT, play_count INT, device TEXT, skip_count INT, extra_int_1 INT, extra_int_2 INT, extra_int_3 INT, extra_text_1 TEXT, extra_text_2 TEXT, extra_text_3 TEXT);")
        db.execSQL("CREATE INDEX idx_song_day_source ON statistics_playback(song_id,day,source_type,source_id);")
        db.execSQL("CREATE INDEX idx_statistics_timestamp ON statistics_playback(timestamp);")
        db.execSQL("CREATE INDEX idx_statistics_day ON statistics_playback(day);")
        db.execSQL("CREATE INDEX idx_statistics_song_id ON statistics_playback(song_id);")

        db.execSQL("CREATE TABLE statistics_musics(_id INT, title TEXT, album_id INT, album TEXT, artist_id INT, artist TEXT, duration INT, _data TEXT, cover_url TEXT, is_hidden INT, genre_id, genre TEXT, data_modified INT, play_count, play_duration, skip_count, looper_count, source_type TEXT, source_id TEXT, device, timestamp, day TEXT, extra_int_1, extra_int_2, extra_int_3, extra_text_1, extra_text_2, extra_text_3);")
        db.execSQL("CREATE INDEX idx_statistics_musics_timestamp ON statistics_musics(timestamp);")
        db.execSQL("CREATE INDEX idx_statistics_musics_day ON statistics_musics(day);")

        db.execSQL("CREATE TABLE statistics_albums(_id INT, album TEXT, artist TEXT, artist_id INT, album_art TEXT, modified_album_art TEXT, recorded_song_count, total_play_duration, total_play_count, timestamp, day TEXT);")
        db.execSQL("CREATE INDEX idx_statistics_albums_timestamp ON statistics_albums(timestamp);")
        db.execSQL("CREATE INDEX idx_statistics_albums_day ON statistics_albums(day);")

        db.execSQL("CREATE TABLE statistics_artists(_id INT, artist TEXT, artist_art TEXT, recorded_song_count, recorded_album_count, total_play_duration, total_play_count, timestamp, day TEXT);")
        db.execSQL("CREATE INDEX idx_statistics_artists_timestamp ON statistics_artists(timestamp);")
        db.execSQL("CREATE INDEX idx_statistics_artists_day ON statistics_artists(day);")

        db.execSQL("CREATE TABLE statistics_genres(_id INT, name TEXT, recorded_song_count, recorded_artist_count, recorded_album_count, total_play_duration, total_play_count, timestamp, day TEXT);")
        db.execSQL("CREATE INDEX idx_statistics_genres_timestamp ON statistics_genres(timestamp);")
        db.execSQL("CREATE INDEX idx_statistics_genres_day ON statistics_genres(day);")

        db.beginTransaction()
        try {
            val zone = ZoneId.systemDefault()
            var rowId = 1
            for (r in rows) {
                val d = Instant.ofEpochMilli(r.playedAtMs).atZone(zone).toLocalDate()
                val dayStr = "%04d%02d%02d".format(d.year, d.monthValue, d.dayOfMonth)
                val songId = r.trackId.substringAfter(':').toLongOrNull() ?: (r.trackId.hashCode().toLong() and 0x7fffffffL)
                val skip = if (r.completed) 0 else 1

                db.execSQL(
                    "INSERT INTO statistics_playback VALUES (?, ?, ?, ?, 0, '', '', ?, 1, ?, ?, 0, 0, 0, null, null, null)",
                    arrayOf(rowId++, songId, r.playedAtMs, dayStr, r.listenedMs, model, skip)
                )
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    } finally {
        db.close()
    }
    return f
}

