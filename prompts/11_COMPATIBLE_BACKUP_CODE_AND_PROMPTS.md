# 11 — Compatible Backup: Code Blueprints + AI Studio Prompts

> Blueprint code for `10_COMPATIBLE_BACKUP_SPEC.md`. Package `com.pulse.player.data.backup`. Adapt imports and signatures to the project; if a snippet doesn't compile, fix it, don't drop the behavior. No new dependencies are needed: only `java.util.zip`, `android.util.JsonReader/JsonWriter`, `android.database.sqlite`, Room (already present).

## 1. Constants, sniffing, safe ZIP access
```kotlin
object Compat {
    const val SETTINGS = "settings.bak"
    const val MUSIC_COVERS = "Covers/MusicCoversBackupConfig.conf"
    const val ALBUM_COVERS = "Covers/AlbumCoversBackupConfig.conf"
    const val TAGS = "MusicTagsBackupConfig.conf"
    const val LYRICS = "Lyrics/LyricsBackupConfig.conf"
    const val PLAYLISTS = "PlayLists/PlayListBackupConfig.conf"
    const val SORT = "SortBackupConfig.conf"
    const val SCAN = "ScanHiddenBackupConfig.conf"
    const val STATS_CFG = "Statistics/StatisticsSnapshotConfig.json"
    const val PLAYBACK = "Statistics/PlaybackRecords.json"
    const val META = "metadata.inf"
    fun snapshot(yyyymm: String) = "Statistics/snapshot_monthly_$yyyymm.db"
    val SNAPSHOT_RE = Regex("^Statistics/snapshot_monthly_(\\d{6})\\.db$")
    val KNOWN = setOf(SETTINGS, MUSIC_COVERS, ALBUM_COVERS, TAGS, LYRICS, PLAYLISTS, SORT, SCAN, STATS_CFG, PLAYBACK)
    val FLAGS = listOf("isBackupMusicTags", "isBackupCovers", "isBackupLyrics", "isBackupPlaylists",
        "isBackupSortSettings", "isBackupScanHideSettings", "isBackupMusicRecap")
    const val MAX_FILE = 512L shl 20; const val MAX_ENTRY = 256L shl 20; const val MAX_TOTAL = 768L shl 20; const val MAX_ENTRIES = 64
}

enum class Sniffed { ZIP, REAL_WAV, JSON, OTHER }

fun sniff(ctx: Context, uri: Uri): Sniffed {
    val h = ByteArray(12); var n = 0
    ctx.contentResolver.openInputStream(uri)?.use { i -> while (n < 12) { val r = i.read(h, n, 12 - n); if (r < 0) break; n += r } }
    fun b(i: Int) = h[i].toInt() and 0xFF
    return when {
        n >= 4 && b(0) == 0x50 && b(1) == 0x4B && ((b(2) == 3 && b(3) == 4) || (b(2) == 5 && b(3) == 6)) -> Sniffed.ZIP
        n >= 12 && String(h, 0, 4, Charsets.ISO_8859_1) == "RIFF" && String(h, 8, 4, Charsets.ISO_8859_1) == "WAVE" -> Sniffed.REAL_WAV
        n >= 1 && (b(0) == '{'.code || b(0) == 0xEF) -> Sniffed.JSON      // 0xEF = UTF-8 BOM; confirm schema later
        else -> Sniffed.OTHER
    }
}

class LimitedStream(inp: InputStream, private val max: Long) : FilterInputStream(inp) {
    private var count = 0L
    private fun add(n: Int) { if (n > 0) { count += n; if (count > max) throw IOException("Backup entry is too large.") } }
    override fun read(): Int = super.read().also { if (it >= 0) add(1) }
    override fun read(b: ByteArray, off: Int, len: Int): Int = super.read(b, off, len).also { add(it) }
}

/** Copies the picked file into a bounded temp file; caller deletes it. */
suspend fun copyToTemp(ctx: Context, uri: Uri): File = withContext(Dispatchers.IO) {
    val f = File.createTempFile("restore_", ".zip", ctx.cacheDir)
    try {
        ctx.contentResolver.openInputStream(uri)!!.use { i -> f.outputStream().use { o ->
            val buf = ByteArray(64 * 1024); var total = 0L
            while (true) { val n = i.read(buf); if (n < 0) break; total += n
                if (total > Compat.MAX_FILE) throw IOException("The file is too large to be a backup.")
                o.write(buf, 0, n); ensureActive() } } }
        f
    } catch (e: Throwable) { f.delete(); throw e }
}

class SafeZip(val file: File) : Closeable {
    val zip = ZipFile(file)
    val names: Set<String>
    init {
        val seen = HashSet<String>(); var total = 0L; var count = 0
        for (e in zip.entries()) {
            if (++count > Compat.MAX_ENTRIES) throw IOException("The archive has too many entries.")
            val n = e.name
            if (n.startsWith("/") || n.contains("\\") || n.contains('\u0000') || n.split('/').any { it == ".." })
                throw IOException("The archive contains an unsafe path.")
            if (!seen.add(n)) throw IOException("The archive has duplicate entries.")
            if (!e.isDirectory) { if (e.size > Compat.MAX_ENTRY) throw IOException("An entry is too large."); total += maxOf(e.size, 0) }
        }
        if (total > Compat.MAX_TOTAL) throw IOException("The archive expands to too much data.")
        names = seen
    }
    fun has(name: String) = zip.getEntry(name) != null
    fun open(name: String): InputStream = LimitedStream(zip.getInputStream(zip.getEntry(name) ?: throw IOException("Missing $name")), Compat.MAX_ENTRY)
    override fun close() = zip.close()
}
```
Recognition rule: after `SafeZip(...)`, it is a backup **only if** `has(Compat.META)` and `names.any { it in Compat.KNOWN }`. Otherwise reject with "This ZIP is not a recognised backup".

## 2. Database changes (Room migration 1 → 2) — do not use destructive migration
```kotlin
// TrackEntity: add  val dataPath: String? = null
// PlaylistEntity: add  val sortKey: String? = null, val sortOrder: Int? = null
// PlayEventEntity: add unique index  indices = [Index("playedAtMs"), Index("trackId"), Index(value = ["trackId", "playedAtMs"], unique = true)]
// @Database(version = 2, ...)

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE tracks ADD COLUMN dataPath TEXT")
        db.execSQL("ALTER TABLE playlists ADD COLUMN sortKey TEXT")
        db.execSQL("ALTER TABLE playlists ADD COLUMN sortOrder INTEGER")
        // remove duplicate events before adding the unique index
        db.execSQL("DELETE FROM play_events WHERE id NOT IN (SELECT MIN(id) FROM play_events GROUP BY trackId, playedAtMs)")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_play_events_trackId_playedAtMs ON play_events(trackId, playedAtMs)")
    }
}
// Room.databaseBuilder(...).addMigrations(MIGRATION_1_2).build()   (exportSchema = true, commit the new schema JSON)
```
Scanner change (file 03 §7): add `MediaStore.Audio.Media.DATA` to the projection, read it into `dataPath`, and fall back to the composed path from spec §7 when blank. Existing rows get `dataPath` on the next **Rescan** (tell the user in the UI: "Rescan once so older entries gain their file path").

New DAO methods:
```kotlin
// TrackDao
@Query("SELECT * FROM tracks WHERE available = 1") suspend fun availableOnce(): List<TrackEntity>
// PlayEvent join for export / dedupe
data class PlaybackRow(val trackId: String, val dataPath: String?, val playedAtMs: Long, val listenedMs: Long, val completed: Boolean)
@Query("SELECT e.trackId AS trackId, t.dataPath AS dataPath, e.playedAtMs AS playedAtMs, e.listenedMs AS listenedMs, e.completed AS completed FROM play_events e JOIN tracks t ON t.id = e.trackId ORDER BY e.playedAtMs") suspend fun playbackRows(): List<PlaybackRow>   // StatsDao
@Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertEvents(items: List<PlayEventEntity>): List<Long>   // StatsDao; rowId -1 = ignored duplicate
@Query("DELETE FROM play_events") suspend fun clearEvents()                                                       // StatsDao
// PlaylistDao
@Query("SELECT * FROM playlists WHERE name = :name COLLATE NOCASE LIMIT 1") suspend fun byName(name: String): PlaylistEntity?
@Query("UPDATE playlists SET sortKey = :key, sortOrder = :order WHERE id = :id") suspend fun setSort(id: Long, key: String?, order: Int?)
@Query("SELECT trackId FROM playlist_tracks WHERE playlistId = :id ORDER BY position") suspend fun trackIds(id: Long): List<String>
```

## 3. Streaming JSON helpers
```kotlin
fun readValue(r: JsonReader): Any? = when (r.peek()) {
    JsonToken.NULL -> { r.nextNull(); null }
    JsonToken.BOOLEAN -> r.nextBoolean()
    JsonToken.NUMBER -> r.nextString().let { it.toLongOrNull() ?: it.toDoubleOrNull() }
    JsonToken.STRING -> r.nextString()
    JsonToken.BEGIN_ARRAY -> { val l = ArrayList<Any?>(); r.beginArray(); while (r.hasNext()) l += readValue(r); r.endArray(); l }
    JsonToken.BEGIN_OBJECT -> { val m = LinkedHashMap<String, Any?>(); r.beginObject(); while (r.hasNext()) m[r.nextName()] = readValue(r); r.endObject(); m }
    else -> { r.skipValue(); null }
}
/** Calls [onItem] for each object of a top-level JSON array, one at a time. Throws on non-array input. */
fun InputStream.forEachObject(onItem: (Map<String, Any?>) -> Unit): Int {
    var n = 0
    JsonReader(InputStreamReader(this, Charsets.UTF_8)).use { r ->
        r.beginArray()
        while (r.hasNext()) { @Suppress("UNCHECKED_CAST") onItem(readValue(r) as? Map<String, Any?> ?: throw IOException("Unexpected array item")); n++ }
        r.endArray()
    }
    return n
}
fun InputStream.readObject(): Map<String, Any?> = JsonReader(InputStreamReader(this, Charsets.UTF_8)).use { @Suppress("UNCHECKED_CAST") (readValue(it) as? Map<String, Any?> ?: throw IOException("Expected a JSON object")) }
fun Map<String, Any?>.str(k: String) = this[k] as? String
fun Map<String, Any?>.int(k: String, d: Int = 0) = (this[k] as? Number)?.toInt() ?: d
fun Map<String, Any?>.lng(k: String, d: Long = 0) = (this[k] as? Number)?.toLong() ?: d
```

## 4. Path keys and the matcher (spec §5)
```kotlin
object PathKey {
    private val volume = Regex("^/(storage/emulated/\\d+|storage/[0-9A-Fa-f]{4}-[0-9A-Fa-f]{4}|sdcard|mnt/sdcard)/")
    fun nfc(s: String) = Normalizer.normalize(s.trim(), Normalizer.Form.NFC).lowercase(Locale.ROOT)
    fun abs(p: String) = nfc(p)
    fun rel(p: String) = nfc(volume.replaceFirst(p.trim(), ""))
    fun name(p: String) = nfc(p.substringAfterLast('/'))
    fun parentName(p: String): String { val s = p.trim().split('/'); return nfc(s.takeLast(2).joinToString("/")) }
}
fun unk(a: String?) = if (a.isNullOrBlank() || a == "<unknown>") "Unknown artist" else a
fun unkAlbum(a: String?) = if (a.isNullOrBlank() || a == "<unknown>") "Unknown album" else a

data class SrcTag(val path: String, val title: String, val artist: String, val album: String)
sealed interface Resolve {
    data class Hit(val track: TrackEntity, val tier: Int) : Resolve
    data object Ambiguous : Resolve
    data object Missing : Resolve
}

class LibraryIndex(tracks: List<TrackEntity>) {
    private val paths = tracks.filter { !it.dataPath.isNullOrBlank() }
    private val byAbs = paths.groupBy { PathKey.abs(it.dataPath!!) }
    private val byRel = paths.groupBy { PathKey.rel(it.dataPath!!) }
    private val byParent = paths.groupBy { PathKey.parentName(it.dataPath!!) }
    private val byName = paths.groupBy { PathKey.name(it.dataPath!!) }
    private val byMeta = tracks.groupBy { Triple(it.normTitle, it.normArtist, it.normAlbum) }

    fun resolve(srcPath: String, tag: SrcTag?): Resolve {
        val tiers = listOf(
            1 to byAbs[PathKey.abs(srcPath)], 2 to byRel[PathKey.rel(srcPath)], 3 to byParent[PathKey.parentName(srcPath)],
            4 to tag?.let { byMeta[Triple(norm(it.title), norm(unk(it.artist)), norm(unkAlbum(it.album)))] },
            5 to byName[PathKey.name(srcPath)])
        for ((tier, list) in tiers) {
            if (list == null || list.isEmpty()) continue
            return if (list.size == 1) Resolve.Hit(list[0], tier) else Resolve.Ambiguous   // never guess among several
        }
        return Resolve.Missing
    }
}
```
`norm()` is the helper from file 03 §6. Build **one** `LibraryIndex` per restore and one `Map<String, SrcTag>` (key `PathKey.abs(path)`) from the tags file.

## 5. Parsing the archive into a staged model
```kotlin
data class SrcPlaylist(val name: String, val sortKey: String?, val sortOrder: Int?, val paths: List<String>)
data class SrcEvent(val path: String, val timestamp: Long, val durationMs: Long, val skipCount: Int)
data class StagedBackup(
    val meta: Map<String, Any?>, val flags: Map<String, Boolean>,
    val tagCount: Int, val tags: Map<String, SrcTag>,
    val playlists: List<SrcPlaylist>, val events: List<SrcEvent>,
    val sortRaw: Map<String, Any?>?, val excludeDirs: List<String>, val hiddenFlagged: Int,
    val lyricsNonEmpty: Boolean, val snapshotsOk: Int, val snapshotsBad: Int, val warnings: List<String>)

suspend fun stage(ctx: Context, zip: SafeZip): StagedBackup = withContext(Dispatchers.IO) {
    val warnings = ArrayList<String>()
    val meta = zip.open(Compat.META).use { it.readObject() }
    val flags = Compat.FLAGS.associateWith { (meta["settings"] as? Map<*, *>)?.get(it) as? Boolean ?: false }
    val tags = HashMap<String, SrcTag>(); var tagCount = 0
    if (zip.has(Compat.TAGS)) tagCount = zip.open(Compat.TAGS).use { s -> s.forEachObject { o ->
        val p = o.str("_data") ?: return@forEachObject
        tags[PathKey.abs(p)] = SrcTag(p, o.str("title").orEmpty(), unk(o.str("artist")), unkAlbum(o.str("album"))) } }
    val playlists = ArrayList<SrcPlaylist>()
    if (zip.has(Compat.PLAYLISTS)) zip.open(Compat.PLAYLISTS).use { s -> s.forEachObject { o ->
        val name = o.str("name")?.trim().orEmpty(); if (name.isEmpty()) return@forEachObject
        val sort = o["sort"] as? Map<*, *>
        val paths = (o["musics"] as? List<*>)?.mapNotNull { (it as? Map<*, *>)?.get("path") as? String }.orEmpty()
        playlists += SrcPlaylist(name, sort?.get("key") as? String, (sort?.get("order") as? Number)?.toInt(), paths) } }
    val events = ArrayList<SrcEvent>()
    if (zip.has(Compat.PLAYBACK)) zip.open(Compat.PLAYBACK).use { s -> s.forEachObject { o ->
        val p = o.str("_data") ?: return@forEachObject; val ts = o.lng("timestamp"); if (ts <= 0) return@forEachObject
        events += SrcEvent(p, ts, o.lng("play_duration"), o.int("skip_count")) } }
    val sortRaw = if (zip.has(Compat.SORT)) zip.open(Compat.SORT).use { it.readObject() } else null
    var exclude = emptyList<String>(); var hidden = 0
    if (zip.has(Compat.SCAN)) zip.open(Compat.SCAN).use { s -> val o = s.readObject()
        exclude = (o["exclude"] as? List<*>)?.mapNotNull { (it as? Map<*, *>)?.get("data") as? String }.orEmpty()
        hidden = (o["hidden_songs"] as? List<*>)?.count { ((it as? Map<*, *>)?.get("is_hidden") as? Number)?.toInt() == 1 } ?: 0 }
    var lyricsNonEmpty = false
    if (zip.has(Compat.LYRICS)) zip.open(Compat.LYRICS).use { lyricsNonEmpty = it.forEachObject { } > 0 }
    var ok = 0; var bad = 0   // snapshots are NEVER opened as databases (file 15 §2.3): size + 16-byte header only
    zip.names.filter { Compat.SNAPSHOT_RE.matches(it) }.forEach { n ->
        val e = zip.entry(n)
        if (e != null && checkSnapshotEntry(zip.file, e)) ok++ else { bad++; warnings += "Snapshot $n failed the header check and was ignored." } }
    StagedBackup(meta, flags, tagCount, tags, playlists, events, sortRaw, exclude, hidden, lyricsNonEmpty, ok, bad, warnings)
}
```
Snapshots are header-checked and counted only; their data is restored from `PlaybackRecords.json` (spec §4, file 15 §2.3). Adapt `zip.entry/zip.file` to the `SafeZip` wrapper API.

## 6. Dry-run preview and apply (one transaction)
```kotlin
data class MatchStats(var tier: IntArray = IntArray(6), var ambiguous: Int = 0, var missing: Int = 0)
data class RestoreOptions(val categories: Set<String>, val replace: Boolean)   // category names = Compat.FLAGS entries
data class RestoreReport(val lines: List<String>, val unmatchedPaths: List<String>)

private val FAV_NAMES = setOf("my favourite", "my favorite", "my favourites", "my favorites")

suspend fun applyRestore(ctx: Context, db: PulseDb, settings: SettingsStore, s: StagedBackup, o: RestoreOptions): RestoreReport = withContext(Dispatchers.IO) {
    val idx = LibraryIndex(db.tracks().availableOnce())
    val lines = ArrayList<String>(); val unmatched = LinkedHashSet<String>(); val stats = MatchStats()
    fun resolve(path: String): TrackEntity? = when (val r = idx.resolve(path, s.tags[PathKey.abs(path)])) {
        is Resolve.Hit -> { stats.tier[r.tier]++; r.track }
        Resolve.Ambiguous -> { stats.ambiguous++; unmatched += "AMBIGUOUS  $path"; null }
        Resolve.Missing -> { stats.missing++; unmatched += "NOT FOUND  $path"; null } }
    val now = System.currentTimeMillis()

    // ---- stage (no DB writes yet) ----
    val doPlaylists = "isBackupPlaylists" in o.categories
    val favIds = ArrayList<String>(); val plans = ArrayList<Triple<SrcPlaylist, List<String>, Int>>()
    if (doPlaylists) for (p in s.playlists) {
        val ids = p.paths.mapNotNull { resolve(it)?.id }.distinct()
        if (p.name.lowercase(Locale.ROOT) in FAV_NAMES) favIds += ids else plans += Triple(p, ids, p.paths.size - ids.size) }
    val doStats = "isBackupMusicRecap" in o.categories
    val events = if (doStats) s.events.mapNotNull { e -> resolve(e.path)?.let { t ->
        PlayEventEntity(trackId = t.id, playedAtMs = e.timestamp, listenedMs = e.durationMs.coerceAtLeast(0), completed = e.skipCount == 0) } } else emptyList()

    // ---- commit once ----
    db.withTransaction {
        if (doPlaylists) {
            if (o.replace) db.favorites().clear()
            val base = now
            db.favorites().addAll(favIds.mapIndexed { i, id -> FavoriteEntity(id, base - i) })      // array order = newest-first display order
            lines += "Favourites: ${favIds.size} resolved"
            for ((p, ids, _) in plans) {
                val existing = db.playlists().byName(p.name)
                val pid = existing?.id ?: db.playlists().create(PlaylistEntity(name = p.name, createdAtMs = now))
                if (existing != null && o.replace) db.playlists().clearTracks(pid)
                var pos = db.playlists().nextPos(pid)
                for (id in ids) db.playlists().addTrack(PlaylistTrackEntity(pid, id, pos++, now))   // IGNORE on duplicates
                db.playlists().setSort(pid, p.sortKey, p.sortOrder) }
            lines += "Playlists: ${plans.size} processed"
        }
        if (doStats) {
            if (o.replace) db.stats().clearEvents()
            val inserted = db.stats().insertEvents(events).count { it != -1L }
            lines += "Play history: $inserted new, ${events.size - inserted} already present"
        }
    }
    // ---- settings (DataStore, after the DB commit) ----
    if ("isBackupSortSettings" in o.categories && s.sortRaw != null) { settings.saveSortRaw(s.sortRaw); lines += "Sort settings applied" }
    if ("isBackupScanHideSettings" in o.categories) {
        val rel = s.excludeDirs.map { PathKey.rel(it).trim('/') }.filter { it.isNotEmpty() }
        settings.addExcludedFolders(rel.toSet()); lines += "Excluded folders added: ${rel.size} (rescan to apply)"
        if (s.hiddenFlagged > 0) lines += "Hidden songs not supported: ${s.hiddenFlagged} skipped" }
    if ("isBackupMusicTags" in o.categories) lines += "Tags: ${s.tagCount} read and used for matching (not written)"
    if ("isBackupCovers" in o.categories) lines += "Covers not applied: they are specific to the source device"
    if ("isBackupLyrics" in o.categories) lines += if (s.lyricsNonEmpty) "Lyrics skipped: format not recognised" else "Lyrics: none in backup"
    lines += "Matched by path ${stats.tier[1]}, relative path ${stats.tier[2]}, folder+name ${stats.tier[3]}, tags ${stats.tier[4]}, name ${stats.tier[5]}; ambiguous ${stats.ambiguous}; not found ${stats.missing}"
    RestoreReport(lines + s.warnings, unmatched.toList())
}
```
Notes for the model: `resolve()` is called once per path occurrence, so counts are per reference. Add to `SettingsStore`: `saveSortRaw(Map<String, Any?>)` (store the JSON string under key `sort_raw`; apply `All_Song` to the Songs-tab sort: `dateAdded→DATE_ADDED`, `title`, `artist`, `duration`; unknown keys are kept raw only) and `addExcludedFolders(Set<String>)`. The preview uses the same staging and a **dry-run** resolve with no writes (extract the stage/resolve part into a function used by both).

## 7. Export (writer)
```kotlin
data class ExportCats(val tags: Boolean, val covers: Boolean, val lyrics: Boolean, val playlists: Boolean, val sort: Boolean, val scan: Boolean, val recap: Boolean)

suspend fun exportCompat(ctx: Context, db: PulseDb, settings: SettingsStore, cats: ExportCats, dest: Uri, onStage: (String) -> Unit): Result<File> = withContext(Dispatchers.IO) { runCatching {
    val tmp = File.createTempFile("backup_", ".zip", ctx.cacheDir)
    val snapFiles = ArrayList<File>()
    try {
        val tracks = db.tracks().availableOnce()
        val withPath = tracks.filter { !it.dataPath.isNullOrBlank() }
        val warnings = ArrayList<String>()
        if (withPath.size < tracks.size) warnings += "${tracks.size - withPath.size} tracks have no file path yet; rescan the library, then back up again."
        val fav = db.favorites().allTracksOnce()
        val lists = db.playlists().observePlaylists().first()
        val rows = db.stats().playbackRows().filter { !it.dataPath.isNullOrBlank() }
        val zone = ZoneId.systemDefault(); val nowYm = YearMonth.now(zone)
        ZipOutputStream(BufferedOutputStream(FileOutputStream(tmp))).use { z ->
            fun entry(name: String, write: (OutputStream) -> Unit) { onStage(name); z.putNextEntry(ZipEntry(name)); write(z); z.closeEntry() }
            val compact = setOf(Compat.TAGS, Compat.MUSIC_COVERS, Compat.ALBUM_COVERS, Compat.PLAYLISTS, Compat.PLAYBACK)   // the sample writes these without indentation
            fun json(name: String, w: JsonWriter.() -> Unit) = entry(name) { o ->
                JsonWriter(OutputStreamWriter(o, Charsets.UTF_8)).apply { if (name !in compact) setIndent("  "); w(); flush() } }   // flush, never close (keeps the ZIP open)
            val ver = ctx.packageManager.getPackageInfo(ctx.packageName, 0)
            val verName = ver.versionName ?: "1.0.0"; val verCode = if (Build.VERSION.SDK_INT >= 28) ver.longVersionCode.toInt() else @Suppress("DEPRECATION") ver.versionCode
            val deviceId = settings.installId()          // random per-install id, NOT ANDROID_ID/IMEI
            val flagMap = mapOf("isBackupMusicTags" to cats.tags, "isBackupCovers" to cats.covers, "isBackupLyrics" to cats.lyrics,
                "isBackupPlaylists" to cats.playlists, "isBackupSortSettings" to cats.sort, "isBackupScanHideSettings" to cats.scan, "isBackupMusicRecap" to cats.recap)

            json(Compat.SETTINGS) { beginObject(); flagMap.forEach { (k, v) -> name(k).value(v) }; name("device").value(deviceId); name("versionName").value(verName); name("versionCode").value(verCode.toLong()); endObject() }
            if (cats.covers) {
                json(Compat.MUSIC_COVERS) { beginArray(); withPath.forEach { beginObject(); name("_data").value(it.dataPath); name("resource").nullValue(); name("cover_url").nullValue(); endObject() }; endArray() }
                json(Compat.ALBUM_COVERS) { beginArray(); withPath.map { it.album }.distinct().forEach { beginObject(); name("album").value(it); name("album_art").nullValue(); name("resource").nullValue(); name("modified_album_art").nullValue(); endObject() }; endArray() } }
            if (cats.tags) json(Compat.TAGS) { beginArray(); withPath.forEach { t -> beginObject(); name("_data").value(t.dataPath); name("title").value(t.title)
                name("artist").value(if (t.artist == "Unknown artist") "<unknown>" else t.artist); name("album").value(t.album); name("track").value(t.trackNumber.toLong()); name("genres").nullValue(); endObject() }; endArray() }
            if (cats.lyrics) json(Compat.LYRICS) { beginArray(); endArray() }
            if (cats.playlists) json(Compat.PLAYLISTS) { beginArray()
                fun pl(name: String, key: String, order: Int, paths: List<String>) { beginObject(); name("name").value(name); name("sort").beginObject().name("key").value(key).name("order").value(order.toLong()).endObject()
                    name("cover").nullValue(); name("musics").beginArray(); paths.forEach { beginObject(); name("path").value(it); endObject() }; endArray(); endObject() }
                pl("My Favourite", "playlistPlayOrder", -1, fav.mapNotNull { it.dataPath })
                for (p in lists) pl(p.name, p.sortKey ?: "playlistPlayOrder", p.sortOrder ?: -1, db.tracks().byIds(db.playlists().trackIds(p.id)).let { m -> val by = m.associateBy { it.id }; db.playlists().trackIds(p.id).mapNotNull { by[it]?.dataPath } })
                endArray() }
            if (cats.sort) json(Compat.SORT) { val raw = settings.sortRawOrNull(); beginObject()
                for (k in listOf("All_Song", "Directory_", "Folder_Song", "Album_", "Album_Song", "Artist_", "Artist_Song", "Genre_", "Genre_Song")) {
                    val (key, order) = settings.sortFor(k, raw)                      // known keys from our prefs, others from raw, else the sample defaults
                    name(k).beginObject().name("key").value(key).name("order").value(order.toLong()).endObject() }
                endObject() }
            if (cats.scan) json(Compat.SCAN) { beginObject(); name("size").value(-1L); name("duration").value(-1L); name("device").value(deviceId)
                name("exclude").beginArray(); settings.excludedFoldersNow().forEachIndexed { i, rel -> beginObject(); name("_id").value((i + 1).toLong()); name("data").value("/storage/emulated/0/" + rel.trim('/')); name("type").value(1L); endObject() }; endArray()
                name("hidden_songs").beginArray(); withPath.forEach { beginObject(); name("path").value(it.dataPath); name("is_hidden").value(0L); endObject() }; endArray(); endObject() }
            if (cats.recap) {
                val months = rows.groupBy { YearMonth.from(Instant.ofEpochMilli(it.playedAtMs).atZone(zone)) }.filterKeys { it < nowYm }
                for ((ym, list) in months.toSortedMap()) { val f = buildSnapshot(ctx, ym, list, Build.MODEL); snapFiles += f
                    entry(Compat.snapshot("%04d%02d".format(ym.year, ym.monthValue))) { o -> f.inputStream().use { it.copyTo(o) } } }
                json(Compat.STATS_CFG) { beginObject(); name("snapshots").beginArray()
                    months.keys.sorted().forEach { ym -> val v = "%04d%02d".format(ym.year, ym.monthValue); beginObject(); name("type").value("monthly"); name("value").value(v); name("filename").value("snapshot_monthly_$v.db"); endObject() }
                    endArray(); name("initApplicationTimestamp").value(rows.firstOrNull()?.playedAtMs ?: System.currentTimeMillis()); endObject() }
                json(Compat.PLAYBACK) { beginArray(); rows.forEach { r -> val d = Instant.ofEpochMilli(r.playedAtMs).atZone(zone).toLocalDate()
                    beginObject(); name("song_id").value(r.trackId.substringAfter(':').toLongOrNull() ?: (r.trackId.hashCode().toLong() and 0x7fffffff))
                    name("timestamp").value(r.playedAtMs); name("day").value("%04d%02d%02d".format(d.year, d.monthValue, d.dayOfMonth)); name("looper_count").value(0L)
                    name("source_type").value(""); name("source_id").value(""); name("play_duration").value(r.listenedMs); name("play_count").value(1L); name("device").value(Build.MODEL)
                    name("skip_count").value(if (r.completed) 0L else 1L); for (i in 1..3) name("extra_int_$i").value(0L); for (i in 1..3) name("extra_text_$i").nullValue()
                    name("_data").value(r.dataPath); endObject() }; endArray() } }
            val stamp = SimpleDateFormat("yyyyMMdd_HHmmssSSS", Locale.US).format(Date())
            json(Compat.META) { beginObject(); name("backupTime").value(stamp); name("mode").value("MANUAL_GLOBAL"); name("type").value("GLOBAL"); name("isAutoBackup").value(false)
                name("deviceId").value(deviceId); name("versionName").value(verName); name("versionCode").value(verCode.toLong())
                name("settings").beginObject(); flagMap.forEach { (k, v) -> name(k).value(v) }; endObject()
                name("songs").value(withPath.size.toLong()); name("playlists").value((lists.size + 1).toLong()); endObject() }
        }
        // ---- verify the finished file BEFORE copying it out ----
        SafeZip(tmp).use { v -> check(v.has(Compat.META)) { "Verification failed: metadata missing." }
            v.open(Compat.META).use { it.readObject() }
            if (cats.playlists) check(v.has(Compat.PLAYLISTS)); if (cats.recap) check(v.has(Compat.PLAYBACK)) }
        ctx.contentResolver.openOutputStream(dest, "wt")!!.use { out -> tmp.inputStream().use { it.copyTo(out) }; out.flush() }
        val copied = ctx.contentResolver.openInputStream(dest)!!.use { i -> var n = 0L; val b = ByteArray(65536); while (true) { val r = i.read(b); if (r < 0) break; n += r }; n }
        check(copied == tmp.length()) { "The saved file size does not match; the backup may be incomplete." }
        tmp
    } finally { snapFiles.forEach { it.delete() } }     // keep tmp until the caller reads its size; caller deletes it
} }
```
`buildSnapshot(ctx, ym, rows, model)`: create `File.createTempFile("snap_", ".db", ctx.cacheDir)`, open with `SQLiteDatabase.openOrCreateDatabase(file, null)`, `execSQL` each statement of spec §1.1 (**not** `android_metadata`), insert `statistics_playback` rows (`_id` = 1..n, columns as in §1.2), one `statistics_musics` row per (track, day) (title/album/artist/duration/`_data`/`play_count`/`play_duration` aggregated, others null/0), one `statistics_albums` and `statistics_artists` row per (name, day) with `_id` = stable positive hash of the name, `recorded_song_count`, `total_play_duration`, `total_play_count`; leave `statistics_genres` empty. Wrap inserts in `beginTransaction()/setTransactionSuccessful()/endTransaction()`, then `close()` before zipping so no `-journal` file remains. Aggregates are best-effort; the file must open in SQLite with all five tables and indexes.

Settings helpers to add (file 06 `SettingsStore`): `installId()` (random 16-hex string created once, stored), `sortRawOrNull()`, `sortFor(key, raw)`, `excludedFoldersNow()`, `saveSortRaw()`, `addExcludedFolders()`.

## 8. UI wiring (Backup & Transfer screen)
```kotlin
val create = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri -> uri?.let { vm.exportCompat(it) } }
create.launch("ManualBackup_${SimpleDateFormat("yyyyMMdd_HHmmssSSS", Locale.US).format(Date())}.${if (asZip) "zip" else "wav"}")
val pick = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> uri?.let { vm.inspectBackup(it) } }
pick.launch(arrayOf("*/*"))
```
`vm.inspectBackup(uri)`: `sniff()` → `REAL_WAV` ⇒ error "This is an audio file, not a backup." · `JSON` ⇒ hand to the Classic restore from file 07 · `ZIP` ⇒ `copyToTemp` → `SafeZip` → recognition rule → `stage()` → dry-run preview state (**no writes**); delete the temp file on every exit path. `vm.restore(options)` re-opens/stages again (never trust a stale preview) then calls `applyRestore`. Use `StateFlow<BackupUiState>` with `Idle / Working(stage) / Preview(..) / Done(report) / Error(message)`. Show real stage names (the entry being written/read) instead of fake percentages. Error text must be plain ("This file is not a recognised backup"), never stack traces or file paths.

## 9. Tests to write (generate the fixture in code — never use the real sample)
- `sniff`: ZIP bytes → ZIP; RIFF/WAVE header → REAL_WAV; `{` → JSON; random bytes → OTHER; a `.wav`-named ZIP is ZIP.
- `SafeZip`: rejects `../x`, `/abs`, backslash, duplicate names, > MAX_ENTRIES; an entry whose real bytes exceed the limit throws.
- Fixture ZIP with invented data: 3 tracks, 2 playlists (one `My Favourite`), 4 events, sort, scan. Staging counts equal the fixture; `My Favourite` → favourites; other playlist → playlist.
- Matcher: tier 1 exact; SD-card path ↔ internal path matches via tier 2; moved folder matches via tier 3/4; two same-named files → Ambiguous; unknown file → Missing; `<unknown>` artist matches "Unknown artist".
- Apply: Merge twice → second run adds 0 favourites/tracks/events; Replace changes only ticked categories; a forced exception inside the transaction leaves the DB unchanged; Classic JSON tests from file 07 still pass.
- Export → `SafeZip` reopens it; entry order matches spec §1; `metadata.inf.songs` equals real count; export → import into a fresh in-memory DB → same favourites/playlists.

## 10. Prompts for Google AI Studio (paste one at a time; attach only the files named)

### PROMPT 8 — Audit and schema migration
Read `10_COMPATIBLE_BACKUP_SPEC.md` and `11_COMPATIBLE_BACKUP_CODE_AND_PROMPTS.md` §2. First report: where the existing Classic backup (favourites JSON, file 07) lives, and confirm it must not change. Then implement the Room migration 1→2 (`dataPath`, playlist sort columns, unique index on `play_events`), the scanner change that fills `dataPath`, and the new DAO methods. Do **not** use destructive migration. Add a migration test. Build and fix errors. Update `brain.md`. Stop.

### PROMPT 9 — Reader (sniff, safe ZIP, staging, matcher)
Read `10_…SPEC.md` §1–§6 and `11_…` §1, §3, §4, §5. Implement `Compat`, `sniff`, `copyToTemp`, `LimitedStream`, `SafeZip`, the JSON helpers, `PathKey`, `LibraryIndex`, `stage()`. Write the unit tests from §9 for sniff, SafeZip, staging and matching using a ZIP fixture generated in test code. No UI yet. Run the tests, report real results, update `brain.md`. Stop.

### PROMPT 10 — Restore (preview + apply + UI)
Read `10_…SPEC.md` §3, §4, §6 and `11_…` §6, §8. Implement the dry-run preview, `applyRestore` in one Room transaction, the SettingsStore helpers (`saveSortRaw`, `addExcludedFolders`), the restore UI (detected format, preview, categories, Merge/Replace with confirmation, real report, "Save unmatched list"), and the smart picker that routes JSON → Classic, ZIP → Full, real WAV → error. Add the apply tests from §9 (repeat-restore idempotency, replace scope, rollback). Update `brain.md`. Stop.

### PROMPT 11 — Export
Read `10_…SPEC.md` §1, §2, §4 and `11_…` §7, §8. Implement `exportCompat` (entry order exactly as in spec §1, `metadata.inf` last), `buildSnapshot`, the SettingsStore helpers, and the Create-backup UI (category checkboxes with real counts, `.wav` default name with a "Save as .zip" switch, show the name the provider actually saved, "Last backup" card written only after verification). Add the export tests from §9, including export→import round trip. Update `brain.md`. Stop.

### PROMPT 12 — Verify against the real sample and audit
Re-read `10_…SPEC.md` §8. On a real device, with the user's real sample file (**never commit it**), report PASS / FAIL / NOT VERIFIED for each acceptance line, with real numbers from the preview and the restore report. Confirm Classic backup/restore is unchanged. List what could not be tested (for example acceptance of your exported file by the original app) and write it into `brain.md` and the README under "Backup & restore". Do not claim anything that was not run.
