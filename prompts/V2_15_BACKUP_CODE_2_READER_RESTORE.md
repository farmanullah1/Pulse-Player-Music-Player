# V2-15 — Backup Code Part 2: Inspector, Parser, Matcher, Restorer

> Read V2_13 and V2_14 first. Same package `com.pulse.player.data.backup`. Output COMPLETE files. The ZIP is **untrusted input**: nothing here may change live data until parsing, matching and validation are finished.

## 0. Files
```
data/backup/BackupFiles.kt      (SAF -> bounded temp file, signature sniffing)
data/backup/BackupInspector.kt  (validate + preview, never mutates)
data/backup/ArchiveParser.kt    (stream-parse selected entries into Staged)
data/backup/TrackMatcher.kt
data/backup/BackupRestorer.kt   (apply in ONE Room transaction)
```

## 1. BackupFiles.kt
```kotlin
package com.pulse.player.data.backup
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import java.io.*

object BackupFiles {
    sealed interface Sniff { object Zip : Sniff; object Wav : Sniff; object Other : Sniff }

    fun sniff(head: ByteArray): Sniff = when {
        head.size >= 4 && head[0] == 0x50.toByte() && head[1] == 0x4B.toByte() &&
            ((head[2] == 3.toByte() && head[3] == 4.toByte()) || (head[2] == 5.toByte() && head[3] == 6.toByte())) -> Sniff.Zip
        head.size >= 12 && String(head, 0, 4, Charsets.ISO_8859_1) == "RIFF" && String(head, 8, 4, Charsets.ISO_8859_1) == "WAVE" -> Sniff.Wav
        else -> Sniff.Other
    }

    fun displayName(ctx: Context, uri: Uri): String = runCatching {
        ctx.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { if (it.moveToFirst()) it.getString(0) else null }
    }.getOrNull() ?: "backup"

    /** Copies the picked file into cacheDir/backup_tmp/in.zip with a hard size cap. Throws IOException with a friendly message. */
    fun copyToTemp(ctx: Context, uri: Uri): File {
        val dir = File(ctx.cacheDir, "backup_tmp").apply { mkdirs() }
        val out = File(dir, "in.zip").apply { delete() }
        val input = BufferedInputStream(ctx.contentResolver.openInputStream(uri) ?: throw IOException("Cannot open the file."), 64 * 1024)
        input.use { i ->
            i.mark(16); val head = ByteArray(16); val n = i.read(head); i.reset()
            when (sniff(head.copyOf(maxOf(n, 0)))) {
                Sniff.Wav -> throw IOException("This is a real audio (WAV) file, not a backup.")
                Sniff.Other -> throw IOException("This file is not a backup archive.")
                Sniff.Zip -> Unit
            }
            var total = 0L; val buf = ByteArray(64 * 1024)
            out.outputStream().use { o -> while (true) { val r = i.read(buf); if (r <= 0) break; total += r
                if (total > BackupFormat.MAX_TOTAL_BYTES) { out.delete(); throw IOException("The file is too large to be a backup.") }
                o.write(buf, 0, r) } }
        }
        return out
    }
}
```

## 2. BackupInspector.kt
```kotlin
package com.pulse.player.data.backup
import android.content.Context
import android.util.JsonReader
import android.util.JsonToken
import org.json.JSONObject
import java.io.*
import java.util.zip.*

enum class ArchiveKind { PULSE, REFERENCE }
data class RestorePreview(
    val kind: ArchiveKind, val fileName: String, val createdAt: String?, val appVersion: String?,
    val songs: Int, val playlists: Int, val historyRecords: Int, val favourites: Int,
    val present: Set<BackupCategory>, val checksumsVerified: Boolean, val checksumNote: String,
    val warnings: List<String>, val entryCount: Int)
sealed interface InspectResult { data class Ok(val preview: RestorePreview) : InspectResult; data class Rejected(val reason: String) : InspectResult }

class LimitedInputStream(i: InputStream, private var left: Long) : FilterInputStream(i) {
    override fun read(): Int { val b = super.read(); if (b >= 0 && --left < 0) throw IOException("Entry is larger than allowed."); return b }
    override fun read(b: ByteArray, off: Int, len: Int): Int { val n = super.read(b, off, len); if (n > 0) { left -= n; if (left < 0) throw IOException("Entry is larger than allowed.") }; return n }
}

/** Entry-name safety (top-level so it is unit-testable). */
internal fun isSafeEntryName(n: String) = n.isNotEmpty() && n.length <= 200 && !n.startsWith("/") && !n.contains('\\') && !n.contains('\u0000') &&
    !n.contains(':') && n.split('/').none { it == ".." || it == "." }

class BackupInspector(private val ctx: Context) {
    fun inspectFile(file: File, displayName: String = file.name): InspectResult = try {
        ZipFile(file).use { zf -> inspectZip(zf, displayName) }
    } catch (e: ZipException) { InspectResult.Rejected("The file is damaged or is not a valid archive.") }
      catch (e: IOException) { InspectResult.Rejected(e.message ?: "The file could not be read.") }

    private fun inspectZip(zf: ZipFile, name: String): InspectResult {
        val entries = zf.entries().toList()
        if (entries.size > BackupFormat.MAX_ENTRIES) return InspectResult.Rejected("The archive has too many entries.")
        val seen = HashSet<String>(); val files = ArrayList<ZipEntry>(); var declared = 0L
        for (e in entries) {
            if (!isSafeEntryName(e.name)) return InspectResult.Rejected("The archive contains an unsafe path. Nothing was changed.")
            if (!seen.add(e.name)) return InspectResult.Rejected("The archive contains duplicate entries.")
            if (e.isDirectory) continue
            if (e.size > BackupFormat.MAX_ENTRY_BYTES) return InspectResult.Rejected("An entry is too large.")
            if (e.size > 1_000_000 && e.compressedSize > 0 && e.size / e.compressedSize > BackupFormat.MAX_RATIO) return InspectResult.Rejected("Suspicious compression ratio.")
            declared += maxOf(e.size, 0); files += e
        }
        if (declared > BackupFormat.MAX_TOTAL_BYTES) return InspectResult.Rejected("The archive is too large when unpacked.")
        val names = files.map { it.name }.toSet()
        val kind = when {
            BackupFormat.X_MANIFEST in names -> ArchiveKind.PULSE
            BackupFormat.METADATA_INF in names && BackupFormat.PLAYLISTS in names -> ArchiveKind.REFERENCE
            else -> return InspectResult.Rejected("This archive is not a Pulse Player or reference-style music backup.")
        }
        val warnings = ArrayList<String>(); val budget = longArrayOf(0L)
        var verified = false; var note: String
        var created: String? = null; var appVer: String? = null
        val meta = runCatching { JSONObject(readSmall(zf, BackupFormat.METADATA_INF)) }.getOrNull()
        if (kind == ArchiveKind.PULSE) {
            val m = runCatching { JSONObject(readSmall(zf, BackupFormat.X_MANIFEST)) }.getOrElse { return InspectResult.Rejected("The backup manifest is damaged.") }
            if (m.optString("formatId") != BackupFormat.FORMAT_ID) return InspectResult.Rejected("Unknown backup format.")
            if (m.optInt("formatVersion", -1) !in 1..BackupFormat.FORMAT_VERSION) return InspectResult.Rejected("This backup was made by a newer version. Update the app and try again.")
            if (m.optBoolean("containsAudioFiles", false)) return InspectResult.Rejected("Backups that contain audio files are not supported.")
            created = m.optString("createdAtUtc").takeIf { it.isNotBlank() }; appVer = m.optString("appVersionName").takeIf { it.isNotBlank() }
            if (BackupFormat.X_CHECKSUMS !in names) return InspectResult.Rejected("The checksum list is missing.")
            val sums = runCatching { JSONObject(readSmall(zf, BackupFormat.X_CHECKSUMS)).getJSONObject("files") }.getOrElse { return InspectResult.Rejected("The checksum list is damaged.") }
            val listed = sums.keys().asSequence().toSet()
            val unexpected = names - listed - BackupFormat.X_CHECKSUMS
            if (unexpected.isNotEmpty()) return InspectResult.Rejected("The archive contains files that are not covered by its checksums.")
            for (n in listed) { val e = zf.getEntry(n) ?: return InspectResult.Rejected("A listed file is missing from the archive.")
                val got = try { sha256(zf, e, budget) } catch (ex: IOException) { return InspectResult.Rejected(ex.message ?: "Read error.") }
                if (!got.equals(sums.getString(n), true)) return InspectResult.Rejected("Checksum mismatch: the backup is corrupt or was modified.") }
            verified = true; note = "Checksums verified"
        } else {
            created = meta?.optString("backupTime")?.takeIf { it.isNotBlank() }; appVer = meta?.optString("versionName")?.takeIf { it.isNotBlank() }
            note = "Reference-style archive: it has no checksums, so integrity could not be verified"
            warnings += "This archive was not created by Pulse Player. Only matching data is imported; unknown parts are skipped."
        }
        // counts (stream, bounded)
        val pls = runCatching { scanPlaylists(zf) }.getOrElse { return InspectResult.Rejected("The playlist data is damaged.") }
        val favCount = pls.firstOrNull { it.first.equals("My Favourite", true) }?.second ?: 0
        val records = runCatching { countArray(zf, BackupFormat.STATS_RECORDS) }.getOrDefault(0)
        val songs = meta?.optInt("songs", 0) ?: runCatching { countArray(zf, BackupFormat.TAGS) }.getOrDefault(0)
        val present = buildSet {
            if (names.contains(BackupFormat.TAGS) && (zf.getEntry(BackupFormat.TAGS)?.size ?: 0L) > 4) add(BackupCategory.TAGS)
            if (pls.any { !it.first.equals("My Favourite", true) }) add(BackupCategory.PLAYLISTS)
            if (favCount > 0) add(BackupCategory.FAVORITES)
            if ((zf.getEntry(BackupFormat.SORT)?.size ?: 0L) > 10) add(BackupCategory.SORT)
            if ((zf.getEntry(BackupFormat.SCAN)?.size ?: 0L) > 40) add(BackupCategory.SCAN)
            if (records > 0) add(BackupCategory.STATS)
            if (kind == ArchiveKind.PULSE && (zf.getEntry(BackupFormat.X_SETTINGS)?.size ?: 0L) > 4) add(BackupCategory.APP_SETTINGS)
        }
        return InspectResult.Ok(RestorePreview(kind, name, created, appVer, songs, pls.count { !it.first.equals("My Favourite", true) },
            records, favCount, present, verified, note, warnings, files.size))
    }

    private fun readSmall(zf: ZipFile, name: String, max: Long = 2L shl 20): String {
        val e = zf.getEntry(name) ?: throw IOException("Missing $name")
        return LimitedInputStream(zf.getInputStream(e), max).use { it.readBytes().toString(Charsets.UTF_8) }
    }
    private fun sha256(zf: ZipFile, e: ZipEntry, budget: LongArray): String {
        val md = newSha256(); val buf = ByteArray(64 * 1024)
        LimitedInputStream(zf.getInputStream(e), BackupFormat.MAX_ENTRY_BYTES).use { s -> while (true) { val n = s.read(buf); if (n <= 0) break
            md.update(buf, 0, n); budget[0] += n; if (budget[0] > BackupFormat.MAX_TOTAL_BYTES) throw IOException("The archive is too large when unpacked.") } }
        return md.digest().toHex()
    }
    internal fun countArray(zf: ZipFile, name: String): Int {
        val e = zf.getEntry(name) ?: return 0; var n = 0
        JsonReader(InputStreamReader(LimitedInputStream(zf.getInputStream(e), BackupFormat.MAX_ENTRY_BYTES), Charsets.UTF_8)).use { r ->
            if (r.peek() != JsonToken.BEGIN_ARRAY) return 0; r.beginArray(); while (r.hasNext()) { r.skipValue(); if (++n > BackupFormat.MAX_RECORDS) throw IOException("Too many records.") } }
        return n
    }
    /** (name, trackCount) for each playlist, streaming. */
    internal fun scanPlaylists(zf: ZipFile): List<Pair<String, Int>> {
        val e = zf.getEntry(BackupFormat.PLAYLISTS) ?: return emptyList(); val out = ArrayList<Pair<String, Int>>()
        JsonReader(InputStreamReader(LimitedInputStream(zf.getInputStream(e), BackupFormat.MAX_ENTRY_BYTES), Charsets.UTF_8)).use { r ->
            r.beginArray(); while (r.hasNext()) { var nm = ""; var c = 0; r.beginObject()
                while (r.hasNext()) when (r.nextName()) {
                    "name" -> nm = if (r.peek() == JsonToken.NULL) { r.nextNull(); "" } else r.nextString()
                    "musics" -> { r.beginArray(); while (r.hasNext()) { r.skipValue(); c++ }; r.endArray() }
                    else -> r.skipValue() }
                r.endObject(); out += nm to c; if (out.size > 5000) throw IOException("Too many playlists.") }
            r.endArray() }
        return out
    }
}
```

## 3. ArchiveParser.kt (stream parse -> Staged; no DB access)
```kotlin
package com.pulse.player.data.backup
import android.util.JsonReader
import android.util.JsonToken
import com.pulse.player.data.favorites.FavItem
import com.pulse.player.data.norm
import org.json.JSONObject
import java.io.*
import java.util.zip.ZipFile

data class RefTag(val path: String, val title: String, val artist: String, val album: String)
data class RefPlaylist(val name: String, val paths: List<String>)
data class RefRecord(val path: String, val timestamp: Long, val playMs: Long, val skip: Int)
class Staged(val tags: List<RefTag>, val playlists: List<RefPlaylist>, val records: List<RefRecord>,
             val sort: JSONObject?, val excluded: Set<String>, val appSettings: JSONObject?, val pulseFavs: List<FavItem>, val warnings: List<String>)

object ArchiveParser {
    private fun rd(zf: ZipFile, name: String) = zf.getEntry(name)?.let {
        JsonReader(InputStreamReader(LimitedInputStream(zf.getInputStream(it), BackupFormat.MAX_ENTRY_BYTES), Charsets.UTF_8)) }
    private fun JsonReader.str(): String? = if (peek() == JsonToken.NULL) { nextNull(); null } else if (peek() == JsonToken.STRING) nextString() else { skipValue(); null }
    private fun JsonReader.lng(): Long = when (peek()) { JsonToken.NUMBER -> nextLong(); JsonToken.STRING -> nextString().toLongOrNull() ?: 0L; else -> { skipValue(); 0L } }
    private fun clean(s: String?, max: Int = 500) = (s ?: "").take(max)

    fun parse(zf: ZipFile, cats: Set<BackupCategory>, wantHints: Boolean): Staged {
        val warn = ArrayList<String>()
        val tags = ArrayList<RefTag>(); val pls = ArrayList<RefPlaylist>(); val recs = ArrayList<RefRecord>(); val favs = ArrayList<FavItem>()
        val needTags = wantHints || BackupCategory.TAGS in cats
        if (needTags) rd(zf, BackupFormat.TAGS)?.use { r -> r.beginArray()
            while (r.hasNext()) { var p = ""; var t = ""; var a = ""; var al = ""; r.beginObject()
                while (r.hasNext()) when (r.nextName()) { "_data" -> p = clean(r.str(), 1000); "title" -> t = clean(r.str()); "artist" -> a = clean(r.str()); "album" -> al = clean(r.str()); else -> r.skipValue() }
                r.endObject(); if (p.isNotBlank() && tags.size < BackupFormat.MAX_RECORDS) tags += RefTag(p, t, a, al) }
            r.endArray() }
        if (BackupCategory.PLAYLISTS in cats || BackupCategory.FAVORITES in cats) rd(zf, BackupFormat.PLAYLISTS)?.use { r -> r.beginArray()
            while (r.hasNext()) { var n = ""; val paths = ArrayList<String>(); r.beginObject()
                while (r.hasNext()) when (r.nextName()) {
                    "name" -> n = clean(r.str(), 200).trim()
                    "musics" -> { r.beginArray(); while (r.hasNext()) { r.beginObject(); while (r.hasNext()) { if (r.nextName() == "path") r.str()?.let { paths += it.take(1000) } else r.skipValue() }; r.endObject() }; r.endArray() }
                    else -> r.skipValue() }
                r.endObject(); pls += RefPlaylist(n, paths) }
            r.endArray() }
        if (BackupCategory.STATS in cats) rd(zf, BackupFormat.STATS_RECORDS)?.use { r -> r.beginArray()
            while (r.hasNext()) { var p = ""; var ts = 0L; var pd = 0L; var sk = 0; r.beginObject()
                while (r.hasNext()) when (r.nextName()) { "_data" -> p = clean(r.str(), 1000); "timestamp" -> ts = r.lng(); "play_duration" -> pd = r.lng(); "skip_count" -> sk = r.lng().toInt(); else -> r.skipValue() }
                r.endObject(); if (p.isNotBlank() && recs.size < BackupFormat.MAX_RECORDS) recs += RefRecord(p, ts, pd, sk) }
            r.endArray() }
        var sort: JSONObject? = null
        if (BackupCategory.SORT in cats) zf.getEntry(BackupFormat.SORT)?.let { e ->
            val o = runCatching { JSONObject(LimitedInputStream(zf.getInputStream(e), 1L shl 20).use { it.readBytes().toString(Charsets.UTF_8) }) }.getOrNull()
            if (o != null) sort = JSONObject().also { out -> o.keys().asSequence().take(64).forEach { k ->
                val v = o.optJSONObject(k); if (v != null && v.optString("key").length in 1..40 && v.optInt("order") in listOf(-1, 1)) out.put(k.take(40), JSONObject().put("key", v.getString("key")).put("order", v.getInt("order"))) } } }
        val excluded = LinkedHashSet<String>()
        if (BackupCategory.SCAN in cats) zf.getEntry(BackupFormat.SCAN)?.let { e ->
            val o = runCatching { JSONObject(LimitedInputStream(zf.getInputStream(e), 4L shl 20).use { it.readBytes().toString(Charsets.UTF_8) }) }.getOrNull()
            o?.optJSONArray("exclude")?.let { a -> for (i in 0 until minOf(a.length(), 200)) { val d = a.optJSONObject(i)?.optString("data").orEmpty()
                val rel = d.replace('\\', '/').replace(Regex("^/storage/[^/]+/"), "").trim('/')
                if (rel.isNotEmpty() && !rel.contains("..") && rel.length < 400) excluded += rel } }
            if ((o?.optJSONArray("hidden_songs")?.length() ?: 0) > 0) warn += "Hidden-song lists are not supported and were skipped." }
        var app: JSONObject? = null
        if (BackupCategory.APP_SETTINGS in cats) zf.getEntry(BackupFormat.X_SETTINGS)?.let { e -> app = runCatching { JSONObject(LimitedInputStream(zf.getInputStream(e), 1L shl 20).use { it.readBytes().toString(Charsets.UTF_8) }) }.getOrNull() }
        if (BackupCategory.FAVORITES in cats) rd(zf, BackupFormat.X_FAVORITES)?.use { r -> r.beginArray()
            while (r.hasNext()) { var t = ""; var a = ""; var al = ""; var d = 0L; var dn = ""; var sz = 0L; var ad = 0L; r.beginObject()
                while (r.hasNext()) when (r.nextName()) { "title" -> t = clean(r.str()); "artist" -> a = clean(r.str()); "album" -> al = clean(r.str())
                    "durationMs" -> d = r.lng(); "displayName" -> dn = clean(r.str()); "sizeBytes" -> sz = r.lng(); "addedAtMs" -> ad = r.lng(); else -> r.skipValue() }
                r.endObject(); if (t.isNotBlank() && favs.size < 100_000) favs += FavItem(t, a, al, d, dn, null, sz, ad, norm(t), norm(a), norm(al)) }
            r.endArray() }
        if ((zf.getEntry(BackupFormat.LYRICS)?.size ?: 0L) > 4) warn += "Lyrics data was found but lyrics are not supported yet; skipped."
        return Staged(tags, pls, recs, sort, excluded, app, favs, warn)
    }
}
```

## 4. TrackMatcher.kt
```kotlin
package com.pulse.player.data.backup
import com.pulse.player.data.db.TrackEntity
import com.pulse.player.data.norm
import java.util.Locale

enum class MatchLevel { PATH, NAME, TAGS, AMBIGUOUS, MISSING }
data class Match(val track: TrackEntity?, val level: MatchLevel)

class TrackMatcher(library: List<TrackEntity>, hints: List<RefTag>) {
    private val byPath = library.groupBy { PathKey.fromTrack(it) }
    private val byName = library.groupBy { it.displayName.lowercase(Locale.ROOT) }
    private val byTags = library.groupBy { tk(it.normTitle, it.normArtist, it.normAlbum) }
    private val hint = HashMap<String, RefTag>(hints.size * 2).also { m -> hints.forEach { m[PathKey.fromPath(it.path)] = it } }
    private val cache = HashMap<String, Match>()
    private fun tk(t: String, a: String, al: String) = "$t|$a|$al"
    private fun fixArtist(a: String) = if (a.isBlank() || a == "<unknown>") "Unknown artist" else a

    fun match(path: String): Match = cache.getOrPut(path) {
        val key = PathKey.fromPath(path)
        byPath[key]?.let { return@getOrPut if (it.size == 1) Match(it[0], MatchLevel.PATH) else Match(null, MatchLevel.AMBIGUOUS) }
        byName[PathKey.nameOf(path).lowercase(Locale.ROOT)]?.let { return@getOrPut if (it.size == 1) Match(it[0], MatchLevel.NAME) else Match(null, MatchLevel.AMBIGUOUS) }
        hint[key]?.let { h -> if (h.title.isNotBlank()) byTags[tk(norm(h.title), norm(fixArtist(h.artist)), norm(h.album))]?.let {
            return@getOrPut if (it.size == 1) Match(it[0], MatchLevel.TAGS) else Match(null, MatchLevel.AMBIGUOUS) } }
        Match(null, MatchLevel.MISSING)
    }
}
```
Never match by title alone. `Ambiguous` and `Missing` are reported, not applied.

## 5. BackupRestorer.kt
```kotlin
package com.pulse.player.data.backup
import android.content.Context
import androidx.room.withTransaction
import com.pulse.player.data.SettingsStore
import com.pulse.player.data.db.*
import com.pulse.player.data.favorites.FavoritesMatcher
import com.pulse.player.data.favorites.MatchResult
import kotlinx.coroutines.*
import org.json.JSONObject
import java.io.File
import java.util.zip.ZipFile

data class RestoreOptions(val categories: Set<BackupCategory>, val replace: Boolean)
data class CatReport(var imported: Int = 0, var updated: Int = 0, var skipped: Int = 0, var unmatched: Int = 0, var ambiguous: Int = 0, var note: String? = null)
data class RestoreReport(val perCategory: Map<BackupCategory, CatReport>, val unmatchedPaths: List<String>, val warnings: List<String>)

class BackupRestorer(private val ctx: Context, private val db: PulseDb, private val settings: SettingsStore) {

    suspend fun restore(file: File, displayName: String, opts: RestoreOptions, progress: (Float, String) -> Unit): Result<RestoreReport> =
        withContext(Dispatchers.IO) { try {
            progress(0.02f, "Validating backup again")
            val insp = BackupInspector(ctx).inspectFile(file, displayName)      // never trust the earlier preview
            if (insp is InspectResult.Rejected) return@withContext Result.failure(Exception(insp.reason))
            val preview = (insp as InspectResult.Ok).preview
            val cats = opts.categories.filter { it.supported && it in preview.present }.toSet()
            require(cats.isNotEmpty()) { "Nothing selected to restore." }
            val report = LinkedHashMap<BackupCategory, CatReport>().also { m -> cats.forEach { m[it] = CatReport() } }
            val unmatched = LinkedHashSet<String>(); val warnings = ArrayList<String>(preview.warnings)

            progress(0.10f, "Reading backup")
            val staged = ZipFile(file).use { ArchiveParser.parse(it, cats, wantHints = true) }
            warnings += staged.warnings; coroutineContext.ensureActive()

            progress(0.30f, "Matching songs on this device")
            val library = db.tracks().allOnce().filter { it.available }
            if (library.isEmpty() && cats.any { it in listOf(BackupCategory.PLAYLISTS, BackupCategory.FAVORITES, BackupCategory.STATS) })
                return@withContext Result.failure(Exception("Your library is empty. Scan your music first, then restore."))
            val matcher = TrackMatcher(library, staged.tags)
            fun resolve(paths: List<String>, rep: CatReport): List<TrackEntity> { val out = ArrayList<TrackEntity>()
                for (p in paths) { val m = matcher.match(p); when (m.level) {
                    MatchLevel.PATH, MatchLevel.NAME, MatchLevel.TAGS -> out += m.track!!
                    MatchLevel.AMBIGUOUS -> { rep.ambiguous++; if (unmatched.size < 500) unmatched += p }
                    MatchLevel.MISSING -> { rep.unmatched++; if (unmatched.size < 500) unmatched += p } } }
                return out }

            // stage everything in memory BEFORE touching the database
            val favRep = report[BackupCategory.FAVORITES]; val favTracks: List<TrackEntity> = if (favRep != null) {
                val ref = staged.playlists.filter { it.name.equals("My Favourite", true) }.flatMap { it.paths }
                val byPath = resolve(ref, favRep)
                val extra = if (staged.pulseFavs.isNotEmpty()) FavoritesMatcher(library, emptySet()).match(staged.pulseFavs).mapNotNull { (it as? MatchResult.Matched)?.track } else emptyList()
                (byPath + extra).distinctBy { it.id } } else emptyList()
            val plRep = report[BackupCategory.PLAYLISTS]
            val stagedPl: List<Pair<String, List<TrackEntity>>> = if (plRep != null)
                staged.playlists.filter { !it.name.equals("My Favourite", true) }.map { (it.name.ifBlank { "Imported playlist" }.take(100)) to resolve(it.paths, plRep).distinctBy { t -> t.id } } else emptyList()
            val statRep = report[BackupCategory.STATS]; val now = System.currentTimeMillis()
            val stagedEv: List<PlayEventEntity> = if (statRep != null) staged.records.mapNotNull { r ->
                if (r.timestamp < 946_684_800_000L || r.timestamp > now + 86_400_000L) { statRep.skipped++; return@mapNotNull null }
                val m = matcher.match(r.path); val t = m.track
                if (t == null) { if (m.level == MatchLevel.AMBIGUOUS) statRep.ambiguous++ else statRep.unmatched++; null }
                else PlayEventEntity(trackId = t.id, playedAtMs = r.timestamp, listenedMs = r.playMs.coerceIn(0, 86_400_000L), completed = r.skip == 0) } else emptyList()
            coroutineContext.ensureActive()

            progress(0.60f, "Applying changes")
            db.withTransaction {
                if (favRep != null) { val existing = db.favorites().allOnce().map { it.trackId }.toSet()
                    if (opts.replace) db.favorites().clear()
                    val toAdd = favTracks.filter { opts.replace || it.id !in existing }
                    db.favorites().addAll(toAdd.map { FavoriteEntity(it.id, now) }); favRep.imported = toAdd.size; favRep.skipped = favTracks.size - toAdd.size }
                if (plRep != null) { if (opts.replace) { db.playlists().clearAllPlaylistTracks(); db.playlists().deleteAllPlaylists() }
                    for ((name, tracks) in stagedPl) { val ex = db.playlists().byName(name)
                        if (ex == null) { val id = db.playlists().create(PlaylistEntity(name = name, createdAtMs = now))
                            db.playlists().addTracks(tracks.mapIndexed { i, t -> PlaylistTrackEntity(id, t.id, i, now) }); plRep.imported++ }
                        else { val have = db.playlists().trackIdsOf(ex.id).toSet(); var pos = db.playlists().nextPos(ex.id)
                            val add = tracks.filter { it.id !in have }; db.playlists().addTracks(add.map { PlaylistTrackEntity(ex.id, it.id, pos++, now) })
                            if (add.isNotEmpty()) plRep.updated++ else plRep.skipped++ } } }
                if (statRep != null) { if (opts.replace) db.stats().clearAll()
                    for (chunk in stagedEv.chunked(500)) { val ids = db.stats().insertIgnore(chunk); val ok = ids.count { it != -1L }; statRep.imported += ok; statRep.skipped += chunk.size - ok } }
            }
            progress(0.90f, "Applying settings")
            report[BackupCategory.SORT]?.let { rep -> staged.sort?.let { s ->
                val old = runCatching { JSONObject(settings.sortJson() ?: "{}") }.getOrDefault(JSONObject())
                val base = if (opts.replace) JSONObject() else old; s.keys().forEach { k -> base.put(k, s.get(k)) }
                settings.setSortJson(base.toString()); rep.imported = s.length() } ?: run { rep.skipped = 1 } }
            report[BackupCategory.SCAN]?.let { rep -> val cur = if (opts.replace) emptySet() else settings.excludedOnce()
                val merged = cur + staged.excluded; settings.setExcluded(merged); rep.imported = (merged - cur).size
                rep.note = "Excluded folders apply after the next rescan." }
            report[BackupCategory.APP_SETTINGS]?.let { rep -> staged.appSettings?.let { settings.applyAppSettings(it); rep.imported = 1 } ?: run { rep.skipped = 1 } }
            report[BackupCategory.TAGS]?.note = "Used as matching hints only. Song tags on this device were not changed."
            progress(1f, "Done")
            Result.success(RestoreReport(report, unmatched.toList(), warnings))
        } catch (e: CancellationException) { throw e
        } catch (e: Throwable) { Result.failure(Exception(e.message ?: "Restore failed. Your data was not changed.", e))
        } finally { /* caller deletes the temp file */ } }
}
```
Rules the model must keep:
1. `db.withTransaction` wraps ALL database writes; the DataStore writes (sort, excluded, app settings) happen after and are reported separately. Say so in `brain.md`.
2. If any exception happens before/inside the transaction, the DB is unchanged (Room rolls back).
3. `Replace` is only allowed after the confirmation dialog (file 16).
4. After restore, no manual refresh is required: Room `Flow`s update the UI. Still call `LibraryScanner` only if `SCAN` was restored and the user taps Rescan.
5. Do not use `song_id` from the archive for matching.

## 6. Acceptance for this part
Parsing the real sample: 13,509 tags, 41 playlists, 2,319 records parse without OOM; a ZIP with `../x` entry is rejected; a WAV is rejected; a modified Pulse archive fails the checksum; restoring twice adds 0 new events and 0 new playlist tracks; library-empty restore is refused with a clear message. Update `brain.md`.
