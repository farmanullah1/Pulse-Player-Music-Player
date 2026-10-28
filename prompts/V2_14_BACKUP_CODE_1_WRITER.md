# V2-14 — Backup Code Part 1: DB changes, models, archive WRITER

> Read V2_13 first. Package `com.pulse.player.data.backup` unless the project uses another package. Blueprint code: fix imports/signatures so it compiles. Output COMPLETE files. Do not claim it compiles unless it did.

## 0. Files to create / edit in this part
```
data/db/Entities.kt      (edit: unique index on play_events)
data/db/Daos.kt          (edit: add queries below)
data/db/PulseDb.kt       (edit: version 2 + MIGRATION_1_2)
data/SettingsStore.kt    (edit: add members in section 2)
data/backup/BackupModels.kt
data/backup/PathKey.kt
data/backup/BackupWriter.kt
data/backup/SnapshotDbWriter.kt
```

## 1. Database changes (needed for safe re-import: no duplicate history)
```kotlin
// Entities.kt — replace PlayEventEntity
@Entity(tableName = "play_events",
    indices = [Index("playedAtMs"), Index("trackId"), Index(value = ["trackId", "playedAtMs"], unique = true)])
data class PlayEventEntity(@PrimaryKey(autoGenerate = true) val id: Long = 0, val trackId: String,
    val playedAtMs: Long, val listenedMs: Long, val completed: Boolean)

// PulseDb.kt
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("DELETE FROM play_events WHERE id NOT IN (SELECT MIN(id) FROM play_events GROUP BY trackId, playedAtMs)")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_play_events_trackId_playedAtMs ON play_events(trackId, playedAtMs)")
    }
}
// @Database(version = 2, ...)   and in PulseApp: Room.databaseBuilder(...).addMigrations(MIGRATION_1_2).build()
```
Also change the live recorder: `StatsDao.insert` becomes `@Insert(onConflict = OnConflictStrategy.IGNORE)`.

### DAO additions
```kotlin
// FavoriteDao
@Query("SELECT * FROM favorites") suspend fun allOnce(): List<FavoriteEntity>
// PlaylistDao
@Query("SELECT * FROM playlists ORDER BY createdAtMs") suspend fun allOnce(): List<PlaylistEntity>
@Query("SELECT * FROM playlist_tracks ORDER BY playlistId, position") suspend fun allTracksOnce(): List<PlaylistTrackEntity>
@Query("SELECT * FROM playlists WHERE name = :name COLLATE NOCASE LIMIT 1") suspend fun byName(name: String): PlaylistEntity?
@Query("SELECT trackId FROM playlist_tracks WHERE playlistId = :id") suspend fun trackIdsOf(id: Long): List<String>
@Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun addTracks(list: List<PlaylistTrackEntity>)
@Query("DELETE FROM playlist_tracks") suspend fun clearAllPlaylistTracks()
@Query("DELETE FROM playlists") suspend fun deleteAllPlaylists()
// StatsDao
@Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertIgnore(list: List<PlayEventEntity>): List<Long>
@Query("SELECT * FROM play_events WHERE id > :afterId ORDER BY id LIMIT :limit") suspend fun page(afterId: Long, limit: Int): List<PlayEventEntity>
@Query("SELECT COUNT(*) FROM play_events") suspend fun count(): Int
@Query("DELETE FROM play_events") suspend fun clearAll()
```

## 2. SettingsStore additions (paste inside the class; `K` is its private key object)
```kotlin
// add to object K:
val sortJson = stringPreferencesKey("sort_json"); val deviceId = stringPreferencesKey("device_label")
val lastBackup = stringPreferencesKey("last_backup_json")

suspend fun deviceLabel(): String = ctx.dataStore.data.first()[K.deviceId]
    ?: java.util.UUID.randomUUID().toString().take(16).also { id -> ctx.dataStore.edit { it[K.deviceId] = id } }
suspend fun sortJson(): String? = ctx.dataStore.data.first()[K.sortJson]
suspend fun setSortJson(s: String) { ctx.dataStore.edit { it[K.sortJson] = s } }
suspend fun excludedOnce(): Set<String> = ctx.dataStore.data.first()[K.excluded] ?: emptySet()
suspend fun setExcluded(s: Set<String>) { ctx.dataStore.edit { it[K.excluded] = s } }
suspend fun lastBackup(): String? = ctx.dataStore.data.first()[K.lastBackup]
suspend fun setLastBackup(json: String) { ctx.dataStore.edit { it[K.lastBackup] = json } }

/** Portable settings only. NEVER include wallpaper path, SAF tree URIs or queue (device specific). */
suspend fun appSettingsJson(): JSONObject { val p = ctx.dataStore.data.first(); return JSONObject().apply {
    put("themeMode", p[K.theme] ?: "DARK"); put("skinId", p[K.skin] ?: "plum"); put("playerTheme", p[K.player] ?: "classic")
    put("reducedMotion", p[K.reduced] ?: false); put("dynamicAccent", p[K.dynamic] ?: true)
    put("minDurationSec", p[K.minDur] ?: 30); put("fadePlayPause", p[K.fade] ?: true) } }

suspend fun applyAppSettings(o: JSONObject) { ctx.dataStore.edit { p ->     // validate every value, ignore unknown
    o.optString("themeMode").takeIf { v -> ThemeMode.values().any { it.name == v } }?.let { p[K.theme] = it }
    o.optString("skinId").takeIf { v -> Skins.any { it.id == v } }?.let { p[K.skin] = it }
    o.optString("playerTheme").takeIf { v -> PlayerThemes.any { it.id == v } }?.let { p[K.player] = it }
    if (o.has("reducedMotion")) p[K.reduced] = o.optBoolean("reducedMotion")
    if (o.has("dynamicAccent")) p[K.dynamic] = o.optBoolean("dynamicAccent")
    if (o.has("fadePlayPause")) p[K.fade] = o.optBoolean("fadePlayPause")
    if (o.has("minDurationSec")) p[K.minDur] = o.optInt("minDurationSec").coerceIn(0, 600) } }
```

## 3. BackupModels.kt
```kotlin
package com.pulse.player.data.backup
import java.security.MessageDigest

object BackupFormat {
    const val FORMAT_ID = "pulseplayer.backup"; const val FORMAT_VERSION = 1
    const val EXT = "pulsebackup"; const val EXT_WAV = "wav"; const val MIME = "application/zip"
    // reference-style entry names (exact, case-sensitive)
    const val SETTINGS_BAK = "settings.bak"; const val METADATA_INF = "metadata.inf"
    const val TAGS = "MusicTagsBackupConfig.conf"
    const val MUSIC_COVERS = "Covers/MusicCoversBackupConfig.conf"; const val ALBUM_COVERS = "Covers/AlbumCoversBackupConfig.conf"
    const val LYRICS = "Lyrics/LyricsBackupConfig.conf"; const val PLAYLISTS = "PlayLists/PlayListBackupConfig.conf"
    const val SORT = "SortBackupConfig.conf"; const val SCAN = "ScanHiddenBackupConfig.conf"
    const val STATS_CFG = "Statistics/StatisticsSnapshotConfig.json"; const val STATS_RECORDS = "Statistics/PlaybackRecords.json"
    fun snapshotName(yyyymm: String) = "Statistics/snapshot_monthly_$yyyymm.db"
    // Pulse-only
    const val X_MANIFEST = "PulseExtra/manifest.json"; const val X_CHECKSUMS = "PulseExtra/checksums.json"
    const val X_FAVORITES = "PulseExtra/favorites.json"; const val X_SETTINGS = "PulseExtra/app_settings.json"
    // limits (untrusted input)
    const val MAX_ENTRIES = 64; const val MAX_ENTRY_BYTES = 128L shl 20; const val MAX_TOTAL_BYTES = 256L shl 20; const val MAX_RATIO = 200L
    const val MAX_RECORDS = 2_000_000
}

enum class BackupCategory(val label: String, val supported: Boolean = true) {
    TAGS("Library snapshot (match hints)"), PLAYLISTS("Playlists"), FAVORITES("Favourites"), SORT("Sort settings"),
    SCAN("Excluded folders"), STATS("Listening history"), APP_SETTINGS("Pulse app settings"),
    COVERS("Custom covers", false), LYRICS("Lyrics", false)
}

data class ExportOptions(val categories: Set<BackupCategory>, val wavName: Boolean = false)
data class ExportResult(val fileName: String, val sizeBytes: Long, val counts: Map<String, Int>, val warnings: List<String>, val checksumsVerified: Boolean, val createdAtUtc: String)

fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }
fun newSha256(): MessageDigest = MessageDigest.getInstance("SHA-256")
fun stableLong(s: String): Long = (s.hashCode().toLong() and 0x7fffffffL) or ((s.length.toLong() and 0xffff) shl 31)
```

## 4. PathKey.kt (shared by writer + matcher)
```kotlin
package com.pulse.player.data.backup
import com.pulse.player.data.db.TrackEntity
import java.util.Locale

object PathKey {
    private val volumePrefix = Regex("^/storage/[^/]+/")
    fun parentOf(p: String) = p.replace('\\', '/').substringBeforeLast('/', "")
    fun nameOf(p: String) = p.replace('\\', '/').substringAfterLast('/')
    private fun folder(f: String) = f.replace('\\', '/').replace(volumePrefix, "").trim('/').lowercase(Locale.ROOT)
    /** key from a reference/absolute path */
    fun fromPath(path: String) = nameOf(path).lowercase(Locale.ROOT) + "|" + folder(parentOf(path))
    /** key from a Pulse track (folderPath is MediaStore RELATIVE_PATH without trailing slash) */
    fun fromTrack(t: TrackEntity) = t.displayName.lowercase(Locale.ROOT) + "|" + folder(t.folderPath)
    /** best-effort absolute path for export. It is only a HINT. */
    fun exportPath(t: TrackEntity) = "/storage/emulated/0/" + t.folderPath.trim('/') + "/" + t.displayName
    fun songId(t: TrackEntity): Long = t.id.substringAfter(':').toLongOrNull() ?: stableLong(t.id)
}
```

## 5. BackupWriter.kt
```kotlin
package com.pulse.player.data.backup

import android.content.Context
import android.net.Uri
import android.os.Build
import android.util.JsonWriter
import com.pulse.player.data.SettingsStore
import com.pulse.player.data.db.*
import kotlinx.coroutines.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.*
import java.security.DigestOutputStream
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

private class NonClosing(o: OutputStream) : FilterOutputStream(o) {
    override fun write(b: ByteArray, off: Int, len: Int) { out.write(b, off, len) }
    override fun close() { flush() }
}

class BackupWriter(private val ctx: Context, private val db: PulseDb, private val settings: SettingsStore) {

    fun suggestedName(o: ExportOptions): String =
        "PulseBackup_" + SimpleDateFormat("yyyyMMdd_HHmmssSSS", Locale.US).format(Date()) + "." + (if (o.wavName) BackupFormat.EXT_WAV else BackupFormat.EXT)

    private class Zipper(val zip: ZipOutputStream) {
        val sums = LinkedHashMap<String, String>()
        fun put(name: String, write: (OutputStream) -> Unit) {
            zip.putNextEntry(ZipEntry(name))
            val md = newSha256(); val out = DigestOutputStream(NonClosing(zip), md)
            write(out); out.flush(); zip.closeEntry(); sums[name] = md.digest().toHex()
        }
        fun json(name: String, block: (JsonWriter) -> Unit) = put(name) { o ->
            val w = JsonWriter(OutputStreamWriter(o, Charsets.UTF_8)); block(w); w.flush() }   // never close w (would close zip)
        fun text(name: String, s: String) = put(name) { it.write(s.toByteArray(Charsets.UTF_8)) }
    }

    suspend fun export(dest: Uri, opts: ExportOptions, onProgress: (Float, String) -> Unit): Result<ExportResult> =
        withContext(Dispatchers.IO) {
            val tmpDir = File(ctx.cacheDir, "backup_tmp").apply { deleteRecursively(); mkdirs() }
            val tmp = File(tmpDir, "out.zip")
            try {
                val res = runCatching { build(tmp, tmpDir, opts, onProgress) }.getOrThrow()
                coroutineContext.ensureActive(); onProgress(0.80f, "Verifying backup")
                val check = BackupInspector(ctx).inspectFile(tmp)       // file 15
                require(check is InspectResult.Ok) { "Verification failed: " + ((check as? InspectResult.Rejected)?.reason ?: "unknown") }
                require(check.preview.checksumsVerified) { "Checksum verification failed" }
                coroutineContext.ensureActive(); onProgress(0.90f, "Saving to chosen location")
                ctx.contentResolver.openOutputStream(dest, "wt")!!.use { o -> tmp.inputStream().use { it.copyTo(o, 64 * 1024) }; o.flush() }
                val written = androidx.documentfile.provider.DocumentFile.fromSingleUri(ctx, dest)?.length() ?: -1L
                require(written == tmp.length()) { "The saved file size does not match (disk full or provider error)." }
                onProgress(1f, "Done")
                val r = res.copy(sizeBytes = tmp.length(), checksumsVerified = true)
                settings.setLastBackup(JSONObject().put("file", r.fileName).put("size", r.sizeBytes).put("time", r.createdAtUtc)
                    .put("counts", JSONObject(r.counts as Map<*, *>)).toString())
                Result.success(r)
            } catch (e: CancellationException) {
                runCatching { android.provider.DocumentsContract.deleteDocument(ctx.contentResolver, dest) }; throw e
            } catch (e: Throwable) {
                runCatching { android.provider.DocumentsContract.deleteDocument(ctx.contentResolver, dest) }   // do not leave a partial file
                Result.failure(Exception(e.message ?: "Backup failed", e))
            } finally { tmpDir.deleteRecursively() }
        }

    private suspend fun build(tmp: File, tmpDir: File, o: ExportOptions, progress: (Float, String) -> Unit): ExportResult {
        val cats = o.categories
        val warnings = ArrayList<String>(); val counts = LinkedHashMap<String, Int>()
        val now = Instant.now().toString()
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmssSSS", Locale.US).format(Date())
        progress(0.02f, "Reading library")
        val tracks = db.tracks().allOnce().filter { it.available }
        val byId = tracks.associateBy { it.id }
        val favs = db.favorites().allOnce()
        val pls = db.playlists().allOnce(); val plTracks = db.playlists().allTracksOnce().groupBy { it.playlistId }
        counts["songs"] = tracks.size

        ZipOutputStream(BufferedOutputStream(FileOutputStream(tmp), 64 * 1024)).use { zip ->
            zip.setLevel(6); val z = Zipper(zip)

            // metadata.inf + settings.bak (reference shape)
            val flags = JSONObject().put("isBackupMusicTags", BackupCategory.TAGS in cats).put("isBackupCovers", false)
                .put("isBackupLyrics", false).put("isBackupPlaylists", BackupCategory.PLAYLISTS in cats || BackupCategory.FAVORITES in cats)
                .put("isBackupSortSettings", BackupCategory.SORT in cats).put("isBackupScanHideSettings", BackupCategory.SCAN in cats)
                .put("isBackupMusicRecap", BackupCategory.STATS in cats)
            val ver = runCatching { ctx.packageManager.getPackageInfo(ctx.packageName, 0) }.getOrNull()
            val vName = ver?.versionName ?: "1.0.0"; val vCode = if (Build.VERSION.SDK_INT >= 28) ver?.longVersionCode ?: 1L else 1L
            val label = settings.deviceLabel()
            z.text(BackupFormat.METADATA_INF, JSONObject().put("backupTime", stamp).put("mode", "MANUAL_GLOBAL").put("type", "GLOBAL")
                .put("isAutoBackup", false).put("deviceId", label).put("versionName", vName).put("versionCode", vCode)
                .put("settings", flags).put("songs", tracks.size).put("playlists", pls.size).toString(2))
            z.text(BackupFormat.SETTINGS_BAK, JSONObject(flags.toString()).put("device", label).put("versionName", vName).put("versionCode", vCode).toString(2))
            coroutineContext.ensureActive()

            // tags (library snapshot, used as hints on import)
            progress(0.10f, "Writing library snapshot")
            if (BackupCategory.TAGS in cats) z.json(BackupFormat.TAGS) { w -> w.beginArray()
                for (t in tracks) { w.beginObject().name("_data").value(PathKey.exportPath(t)).name("title").value(t.title)
                    .name("artist").value(if (t.artist == "Unknown artist") "<unknown>" else t.artist).name("album").value(t.album)
                    .name("track").value(t.trackNumber.toLong()).name("genres").nullValue().endObject() }
                w.endArray() } else z.text(BackupFormat.TAGS, "[]")
            z.text(BackupFormat.MUSIC_COVERS, "[]"); z.text(BackupFormat.ALBUM_COVERS, "[]"); z.text(BackupFormat.LYRICS, "[]")

            // playlists (+ favourites as "My Favourite", exactly like the reference layout)
            progress(0.20f, "Writing playlists")
            var plCount = 0
            z.json(BackupFormat.PLAYLISTS) { w -> w.beginArray()
                if (BackupCategory.FAVORITES in cats) { plCount++
                    writePlaylist(w, "My Favourite", favs.sortedByDescending { it.addedAtMs }.mapNotNull { byId[it.trackId] }) }
                if (BackupCategory.PLAYLISTS in cats) for (p in pls) { if (p.name.equals("My Favourite", true)) continue; plCount++
                    writePlaylist(w, p.name, (plTracks[p.id] ?: emptyList()).sortedBy { it.position }.mapNotNull { byId[it.trackId] }) }
                w.endArray() }
            counts["playlists"] = pls.size; counts["favorites"] = if (BackupCategory.FAVORITES in cats) favs.size else 0
            coroutineContext.ensureActive()

            // sort + scan
            val sortRaw = settings.sortJson()
            z.text(BackupFormat.SORT, if (BackupCategory.SORT in cats) (sortRaw ?: defaultSort()) else "{}")
            val excluded = settings.excludedOnce()
            z.text(BackupFormat.SCAN, JSONObject().put("size", -1).put("duration", -1).put("device", label).apply {
                val ex = JSONArray(); if (BackupCategory.SCAN in cats) excluded.sorted().forEachIndexed { i, f ->
                    ex.put(JSONObject().put("_id", i + 1).put("data", "/storage/emulated/0/" + f.trim('/')).put("type", 1)) }
                put("exclude", ex); put("hidden_songs", JSONArray()) }.toString(2))
            counts["excludedFolders"] = if (BackupCategory.SCAN in cats) excluded.size else 0

            // statistics
            progress(0.30f, "Writing listening history")
            val snapshotFiles = ArrayList<Pair<String, File>>(); var evCount = 0; var skippedEv = 0
            if (BackupCategory.STATS in cats) {
                val zone = java.time.ZoneId.systemDefault(); val dayFmt = java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd")
                val perMonth = java.util.TreeMap<String, MutableList<PlayEventEntity>>()
                z.json(BackupFormat.STATS_RECORDS) { w -> w.beginArray(); var after = 0L
                    while (true) { val page = kotlinx.coroutines.runBlocking { db.stats().page(after, 2000) }; if (page.isEmpty()) break
                        for (e in page) { after = e.id; val t = byId[e.trackId]; if (t == null) { skippedEv++; continue }
                            val day = java.time.Instant.ofEpochMilli(e.playedAtMs).atZone(zone).toLocalDate().format(dayFmt)
                            writeRecord(w, t, e, day); evCount++; perMonth.getOrPut(day.substring(0, 6)) { ArrayList() }.add(e) } }
                    w.endArray() }
                progress(0.45f, "Writing monthly snapshots")
                for ((ym, list) in perMonth) { coroutineContext.ensureActive()
                    val f = File(tmpDir, "snapshot_monthly_$ym.db"); SnapshotDbWriter.write(f, list, byId); snapshotFiles += ym to f }
                z.json(BackupFormat.STATS_CFG) { w -> w.beginObject().name("snapshots").beginArray()
                    for ((ym, _) in snapshotFiles) w.beginObject().name("type").value("monthly").name("value").value(ym).name("filename").value("snapshot_monthly_$ym.db").endObject()
                    w.endArray().name("initApplicationTimestamp").value(System.currentTimeMillis()).endObject() }
                for ((ym, f) in snapshotFiles) z.put(BackupFormat.snapshotName(ym)) { out -> f.inputStream().use { it.copyTo(out, 64 * 1024) } }
                if (skippedEv > 0) warnings += "$skippedEv history records skipped because their song is no longer in the library."
            } else { z.text(BackupFormat.STATS_RECORDS, "[]"); z.text(BackupFormat.STATS_CFG, """{"snapshots":[],"initApplicationTimestamp":0}""") }
            counts["historyRecords"] = evCount
            coroutineContext.ensureActive()

            // Pulse extras
            progress(0.60f, "Writing Pulse data")
            z.json(BackupFormat.X_FAVORITES) { w -> w.beginArray()
                if (BackupCategory.FAVORITES in cats) for (f in favs) { val t = byId[f.trackId] ?: continue
                    w.beginObject().name("title").value(t.title).name("artist").value(t.artist).name("album").value(t.album)
                        .name("durationMs").value(t.durationMs).name("displayName").value(t.displayName).name("sizeBytes").value(t.sizeBytes)
                        .name("addedAtMs").value(f.addedAtMs).endObject() }
                w.endArray() }
            z.text(BackupFormat.X_SETTINGS, if (BackupCategory.APP_SETTINGS in cats) settings.appSettingsJson().toString(2) else "{}")
            val catInfo = JSONObject().also { c -> BackupCategory.values().forEach { k -> c.put(k.name, JSONObject().put("included", k in cats && k.supported)) } }
            z.text(BackupFormat.X_MANIFEST, JSONObject().put("formatId", BackupFormat.FORMAT_ID).put("formatVersion", BackupFormat.FORMAT_VERSION)
                .put("appName", "Pulse Player").put("appVersionName", vName).put("createdAtUtc", now).put("backupMode", "MANUAL_GLOBAL")
                .put("deviceLabel", Build.MODEL).put("containsAudioFiles", false).put("counts", JSONObject(counts as Map<*, *>))
                .put("categories", catInfo).put("warnings", JSONArray(warnings)).toString(2))
            // checksums LAST, covering every other entry
            z.text(BackupFormat.X_CHECKSUMS, JSONObject().put("algorithm", "SHA-256").put("files", JSONObject(z.sums as Map<*, *>)).toString(2))
        }
        return ExportResult(suggestedNameStub, 0, counts, warnings, false, now)
    }
    private val suggestedNameStub = "backup"     // real file name is chosen by the SAF picker; UI shows DocumentFile name

    private fun writePlaylist(w: JsonWriter, name: String, tracks: List<TrackEntity>) {
        w.beginObject().name("name").value(name)
        w.name("sort").beginObject().name("key").value("playlistPlayOrder").name("order").value(-1L).endObject()
        w.name("cover").nullValue().name("musics").beginArray()
        for (t in tracks) w.beginObject().name("path").value(PathKey.exportPath(t)).endObject()
        w.endArray().endObject()
    }
    private fun writeRecord(w: JsonWriter, t: TrackEntity, e: PlayEventEntity, day: String) {
        w.beginObject().name("song_id").value(PathKey.songId(t)).name("timestamp").value(e.playedAtMs).name("day").value(day)
            .name("looper_count").value(0L).name("source_type").value("").name("source_id").value("")
            .name("play_duration").value(e.listenedMs).name("play_count").value(1L).name("device").value(Build.MODEL)
            .name("skip_count").value(if (e.completed) 0L else 1L)
            .name("extra_int_1").value(0L).name("extra_int_2").value(0L).name("extra_int_3").value(0L)
            .name("extra_text_1").nullValue().name("extra_text_2").nullValue().name("extra_text_3").nullValue()
            .name("_data").value(PathKey.exportPath(t)).endObject()
    }
    private fun defaultSort() = """{"All_Song":{"key":"dateAdded","order":-1},"Album_":{"key":"songCount","order":-1},"Artist_":{"key":"songCount","order":-1}}"""
}
```
Notes for the model:
- `runBlocking` inside the JSON lambda is used only because `JsonWriter` callbacks are not suspend. If you prefer, load events into a list first (events are small compared with the library) and remove `runBlocking`.
- `exportPath()` is `/storage/emulated/0/<RELATIVE_PATH><name>`. On an SD card the real path differs; the matcher ignores the volume prefix, so this is fine.
- The success dialog must show the file name returned by `DocumentFile.fromSingleUri(...).name`, not `suggestedNameStub`. Fix `ExportResult.fileName` in the ViewModel after export returns.

## 6. SnapshotDbWriter.kt (valid reference-shaped monthly SQLite snapshot)
```kotlin
package com.pulse.player.data.backup
import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import android.os.Build
import com.pulse.player.data.db.*
import java.io.File

object SnapshotDbWriter {
    private val DDL = listOf(
        "CREATE TABLE android_metadata (locale TEXT)",
        "CREATE TABLE statistics_playback(_id INT,song_id INT,timestamp INT,day TEXT,looper_count INT,source_type TEXT,source_id TEXT,play_duration INT,play_count INT,device TEXT,skip_count INT,extra_int_1 INT,extra_int_2 INT,extra_int_3 INT,extra_text_1 TEXT,extra_text_2 TEXT,extra_text_3 TEXT)",
        "CREATE INDEX idx_song_day_source ON statistics_playback(song_id,day,source_type,source_id)",
        "CREATE INDEX idx_statistics_timestamp ON statistics_playback(timestamp)",
        "CREATE INDEX idx_statistics_day ON statistics_playback(day)",
        "CREATE INDEX idx_statistics_song_id ON statistics_playback(song_id)",
        "CREATE TABLE statistics_musics(_id INT,title TEXT,album_id INT,album TEXT,artist_id INT,artist TEXT,duration INT,_data TEXT,cover_url TEXT,is_hidden INT,genre_id,genre TEXT,data_modified INT,play_count,play_duration,skip_count,looper_count,source_type TEXT,source_id TEXT,device,timestamp,day TEXT,extra_int_1,extra_int_2,extra_int_3,extra_text_1,extra_text_2,extra_text_3)",
        "CREATE TABLE statistics_albums(_id INT,album TEXT,artist TEXT,artist_id INT,album_art TEXT,modified_album_art TEXT,recorded_song_count,total_play_duration,total_play_count,timestamp,day TEXT)",
        "CREATE TABLE statistics_artists(_id INT,artist TEXT,artist_art TEXT,recorded_song_count,recorded_album_count,total_play_duration,total_play_count,timestamp,day TEXT)",
        "CREATE TABLE statistics_genres(_id INT,name TEXT,recorded_song_count,recorded_artist_count,recorded_album_count,total_play_duration,total_play_count,timestamp,day TEXT)")

    fun write(file: File, events: List<PlayEventEntity>, byId: Map<String, TrackEntity>) {
        file.delete()
        val d = SQLiteDatabase.openOrCreateDatabase(file, null)
        try {
            d.beginTransaction()
            try {
                DDL.forEach(d::execSQL); d.execSQL("INSERT INTO android_metadata VALUES('en_US')")
                val zone = java.time.ZoneId.systemDefault(); val fmt = java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd")
                var rowId = 0L; val perTrack = LinkedHashMap<String, MutableList<PlayEventEntity>>()
                for (e in events) { val t = byId[e.trackId] ?: continue
                    val day = java.time.Instant.ofEpochMilli(e.playedAtMs).atZone(zone).toLocalDate().format(fmt)
                    d.insert("statistics_playback", null, ContentValues().apply {
                        put("_id", ++rowId); put("song_id", PathKey.songId(t)); put("timestamp", e.playedAtMs); put("day", day)
                        put("looper_count", 0); put("source_type", ""); put("source_id", ""); put("play_duration", e.listenedMs)
                        put("play_count", 1); put("device", Build.MODEL); put("skip_count", if (e.completed) 0 else 1)
                        put("extra_int_1", 0); put("extra_int_2", 0); put("extra_int_3", 0) })
                    perTrack.getOrPut(t.id) { ArrayList() }.add(e) }
                val artists = HashMap<String, LongArray>(); val albums = HashMap<String, LongArray>()     // [plays, ms]
                for ((id, list) in perTrack) { val t = byId[id]!!; val last = list.maxOf { it.playedAtMs }
                    val ms = list.sumOf { it.listenedMs }
                    d.insert("statistics_musics", null, ContentValues().apply {
                        put("_id", PathKey.songId(t)); put("title", t.title); put("album_id", stableLong(t.album)); put("album", t.album)
                        put("artist_id", stableLong(t.artist)); put("artist", t.artist); put("duration", t.durationMs); put("_data", PathKey.exportPath(t))
                        put("is_hidden", 0); put("data_modified", t.dateModifiedMs / 1000); put("play_count", list.size); put("play_duration", ms)
                        put("skip_count", list.count { !it.completed }); put("looper_count", 0); put("source_type", ""); put("source_id", "")
                        put("device", Build.MODEL); put("timestamp", last); put("day", java.time.Instant.ofEpochMilli(last).atZone(zone).toLocalDate().format(fmt))
                        put("extra_int_1", 0); put("extra_int_2", 0); put("extra_int_3", 0) })
                    artists.getOrPut(t.artist) { LongArray(2) }.also { it[0] += list.size.toLong(); it[1] += ms }
                    albums.getOrPut(t.album + "\u0000" + t.artist) { LongArray(2) }.also { it[0] += list.size.toLong(); it[1] += ms } }
                val ts = events.maxOf { it.playedAtMs }; val day = java.time.Instant.ofEpochMilli(ts).atZone(zone).toLocalDate().format(fmt)
                for ((a, v) in artists) d.insert("statistics_artists", null, ContentValues().apply {
                    put("_id", stableLong(a)); put("artist", a); put("recorded_song_count", 1); put("recorded_album_count", 1)
                    put("total_play_duration", v[1]); put("total_play_count", v[0]); put("timestamp", ts); put("day", day) })
                for ((k, v) in albums) { val (al, ar) = k.split("\u0000"); d.insert("statistics_albums", null, ContentValues().apply {
                    put("_id", stableLong(k)); put("album", al); put("artist", ar); put("artist_id", stableLong(ar)); put("recorded_song_count", 1)
                    put("total_play_duration", v[1]); put("total_play_count", v[0]); put("timestamp", ts); put("day", day) }) }
                d.version = 1; d.setTransactionSuccessful()
            } finally { d.endTransaction() }
        } finally { d.close() }
    }
}
```
`recorded_song_count` above is a simplification (1). State this in `brain.md` under Known limits.

## 7. Acceptance for this part
Export produces a ZIP that reopens; `metadata.inf` counts equal real counts; `Statistics/snapshot_monthly_YYYYMM.db` opens in a DB viewer with real rows; `PulseExtra/checksums.json` lists every other entry; no audio inside; temp files deleted; on failure no half-written file remains. Update `brain.md`.
