# 07 — Favorites Export/Import (JSON) + Music Folder + SD Card

## 1. Goal (read carefully)
The JSON stores **which songs are favorites** (title, artist, album, duration, file name, size). It does **not** contain audio. To move to a new phone/tablet:

1. Old device → **Settings → Backup & Transfer → Export favorites** → save `PulsePlayer_Favorites_YYYY-MM-DD.json`.
2. Copy your music files **and** that JSON to the new device (USB, SD card, Quick Share...) — music into a `Music` folder.
3. New device → install app → grant audio access (or **Choose music folder**) → let it scan.
4. **Settings → Backup & Transfer → Import favorites** → pick the JSON → review the preview → **Apply**.

Matching never depends on identical folder paths. Nothing is moved, copied, renamed or deleted.

## 2. JSON schema (version 1)
```json
{
  "schema": "pulseplayer.favorites",
  "schemaVersion": 1,
  "app": "Pulse Player",
  "exportedAt": "<ISO-8601 UTC time of the real export>",
  "count": <number of items>,
  "items": [
    { "title": "...", "artist": "...", "album": "...", "durationMs": 0, "displayName": "file.mp3",
      "mimeType": "audio/mpeg", "sizeBytes": 0, "addedAtMs": 0,
      "normTitle": "...", "normArtist": "...", "normAlbum": "..." }
  ]
}
```
This is a shape illustration. The app must generate the file from the real `favorites` table only. Never ship sample data.

## 3. Codec (uses Android's built-in `org.json`, no extra library)
```kotlin
package com.pulse.player.data.favorites

data class FavItem(val title: String, val artist: String, val album: String, val durationMs: Long,
                   val displayName: String, val mimeType: String?, val sizeBytes: Long, val addedAtMs: Long,
                   val normTitle: String, val normArtist: String, val normAlbum: String)

object FavoritesCodec {
    const val SCHEMA = "pulseplayer.favorites"; const val VERSION = 1
    const val MAX_BYTES = 10 * 1024 * 1024; const val MAX_ITEMS = 50_000; private const val MAX_STR = 500

    fun encode(tracks: List<TrackEntity>, addedAt: Map<String, Long>): String {
        val arr = JSONArray()
        tracks.forEach { t -> arr.put(JSONObject().apply {
            put("title", t.title); put("artist", t.artist); put("album", t.album); put("durationMs", t.durationMs)
            put("displayName", t.displayName); put("mimeType", t.mimeType ?: JSONObject.NULL); put("sizeBytes", t.sizeBytes)
            put("addedAtMs", addedAt[t.id] ?: 0L); put("normTitle", t.normTitle); put("normArtist", t.normArtist); put("normAlbum", t.normAlbum) }) }
        return JSONObject().apply {
            put("schema", SCHEMA); put("schemaVersion", VERSION); put("app", "Pulse Player")
            put("exportedAt", Instant.now().toString()); put("count", tracks.size); put("items", arr) }.toString(2)
    }

    sealed interface Decoded { data class Ok(val items: List<FavItem>, val exportedAt: String?, val skipped: Int) : Decoded; data class Fail(val reason: String) : Decoded }

    fun decode(text: String): Decoded {
        val root = runCatching { JSONObject(text) }.getOrElse { return Decoded.Fail("This file is not valid JSON.") }
        if (root.optString("schema") != SCHEMA) return Decoded.Fail("This is not a Pulse Player favorites file.")
        if (root.optInt("schemaVersion", -1) != VERSION) return Decoded.Fail("Unsupported file version. Update the app and try again.")
        val arr = root.optJSONArray("items") ?: return Decoded.Fail("The file has no items list.")
        if (arr.length() > MAX_ITEMS) return Decoded.Fail("The file has too many items.")
        val out = ArrayList<FavItem>(arr.length()); var skipped = 0
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: run { skipped++; continue }
            val title = o.optString("title").take(MAX_STR); if (title.isBlank()) { skipped++; continue }
            val artist = o.optString("artist").take(MAX_STR); val album = o.optString("album").take(MAX_STR)
            out += FavItem(title, artist, album, o.optLong("durationMs", 0).coerceAtLeast(0), o.optString("displayName").take(MAX_STR),
                o.optString("mimeType").takeIf { it.isNotBlank() && it != "null" }, o.optLong("sizeBytes", 0).coerceAtLeast(0), o.optLong("addedAtMs", 0),
                norm(title), norm(artist), norm(album)) }          // re-normalize locally; never trust file's norm fields
        return Decoded.Ok(out, root.optString("exportedAt").takeIf { it.isNotBlank() }, skipped)
    }
}
```

## 4. Matcher (explainable, ranked)
```kotlin
sealed interface MatchResult {
    data class Matched(val item: FavItem, val track: TrackEntity, val level: String) : MatchResult      // "strong" | "high" | "medium"
    data class AlreadyFavorite(val item: FavItem, val track: TrackEntity) : MatchResult
    data class Ambiguous(val item: FavItem, val candidates: List<TrackEntity>) : MatchResult
    data class Missing(val item: FavItem) : MatchResult
    data class Duplicate(val item: FavItem) : MatchResult
}

class FavoritesMatcher(private val library: List<TrackEntity>, private val favoriteIds: Set<String>) {
    private val byTitleArtist = library.groupBy { it.normTitle to it.normArtist }
    private val byTitle = library.groupBy { it.normTitle }

    fun match(items: List<FavItem>): List<MatchResult> {
        val seen = HashSet<String>(); val out = ArrayList<MatchResult>(items.size)
        for (it in items) {
            val key = "${it.normTitle}|${it.normArtist}|${it.durationMs / 2000}"
            if (!seen.add(key)) { out += MatchResult.Duplicate(it); continue }
            // 1) strong: same file name + size + near duration
            val strong = library.filter { t -> t.sizeBytes == it.sizeBytes && it.sizeBytes > 0 && t.displayName.equals(it.displayName, true) && abs(t.durationMs - it.durationMs) <= 2000 }
            // 2) high: title + artist + duration within 2s
            val ta = byTitleArtist[it.normTitle to it.normArtist].orEmpty()
            val high = ta.filter { t -> abs(t.durationMs - it.durationMs) <= 2000 }
            // 3) medium: title + album, duration within 5s
            val medium = byTitle[it.normTitle].orEmpty().filter { t -> t.normAlbum == it.normAlbum && abs(t.durationMs - it.durationMs) <= 5000 }
            val (cands, level) = when { strong.size == 1 -> strong to "strong"; high.size == 1 -> high to "high"; high.isEmpty() && medium.size == 1 -> medium to "medium"
                else -> (strong.ifEmpty { high.ifEmpty { medium } }) to "multi" }
            out += when {
                cands.isEmpty() -> MatchResult.Missing(it)
                cands.size > 1 || level == "multi" -> MatchResult.Ambiguous(it, cands)
                cands[0].id in favoriteIds -> MatchResult.AlreadyFavorite(it, cands[0])
                else -> MatchResult.Matched(it, cands[0], level) }
        }
        return out
    }
}
```
Never auto-apply `Ambiguous`. A title-only match is never accepted.

## 5. Repository (export / preview / apply)
```kotlin
class FavoritesRepository(private val ctx: Context, private val db: PulseDb) {
    suspend fun export(uri: Uri): Result<Int> = withContext(Dispatchers.IO) { runCatching {
        val tracks = db.favorites().allTracksOnce()
        if (tracks.isEmpty()) error("You have no favourites to export yet.")
        val added = db.favorites().addedAtList().associate { it.trackId to it.addedAtMs }
        val json = FavoritesCodec.encode(tracks, added)
        ctx.contentResolver.openOutputStream(uri, "wt")!!.use { it.write(json.toByteArray(Charsets.UTF_8)); it.flush() }
        tracks.size } }

    data class Preview(val fileName: String, val exportedAt: String?, val results: List<MatchResult>, val invalid: Int)

    suspend fun preview(uri: Uri, fileName: String): Result<Preview> = withContext(Dispatchers.IO) { runCatching {
        val bytes = ctx.contentResolver.openInputStream(uri)!!.use { readLimited(it, FavoritesCodec.MAX_BYTES) }
        when (val d = FavoritesCodec.decode(bytes.toString(Charsets.UTF_8))) {
            is FavoritesCodec.Decoded.Fail -> error(d.reason)
            is FavoritesCodec.Decoded.Ok -> {
                val lib = db.tracks().allOnce().filter { it.available }
                val favIds = db.favorites().observeIds().first().toSet()
                Preview(fileName, d.exportedAt, FavoritesMatcher(lib, favIds).match(d.items), d.skipped) } } } }

    /** Applies only confident matches + user-resolved ambiguous picks. Atomic. Idempotent (IGNORE on conflict). */
    suspend fun apply(preview: Preview, resolved: Map<FavItem, TrackEntity>, replace: Boolean): Int = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val ids = preview.results.mapNotNull { (it as? MatchResult.Matched)?.track?.id } + resolved.values.map { it.id }
        db.withTransaction {
            if (replace) db.favorites().clear()
            db.favorites().addAll(ids.distinct().map { FavoriteEntity(it, now) })
        }
        ids.distinct().size
    }
    private fun readLimited(i: InputStream, max: Int): ByteArray { val o = ByteArrayOutputStream(); val b = ByteArray(8192); var n: Int; var t = 0
        while (i.read(b).also { n = it } > 0) { t += n; if (t > max) error("The file is too large."); o.write(b, 0, n) }; return o.toByteArray() }
}
```
Add to `FavoriteDao`: `@Query("SELECT trackId, addedAtMs FROM favorites") suspend fun addedAtList(): List<FavAdded>` and `data class FavAdded(val trackId: String, val addedAtMs: Long)`. Needs `androidx.room.withTransaction` (room-ktx).

## 6. UI: Backup & Transfer screen
Sections as cards (16dp padding, 20dp radius):
1. **Export favourites** — shows real count ("42 favourites"). Button launches:
```kotlin
val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri -> uri?.let { vm.exportFavorites(it) } }
export.launch("PulsePlayer_Favorites_${LocalDate.now()}.json")
```
Disable (with hint) if count is 0. Show success only after the stream closes: "Exported 42 favourites".
2. **Import favourites** —
```kotlin
val import = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> uri?.let { vm.previewImport(it, displayNameOf(it)) } }
import.launch(arrayOf("application/json", "text/json", "application/octet-stream", "text/plain"))
```
Shows the **preview sheet**: file name, export date, counts — *Will add* (n), *Already favourite* (n), *Need your choice* (n), *Not found on this device* (n), *Skipped/invalid* (n), *Duplicates in file* (n). Tabs/list to review Ambiguous (pick a candidate or skip) and Missing (list). Radio: **Merge (recommended)** / **Replace favourites** (extra confirmation dialog: "This replaces your current favourites list. Music files and playlists are not affected."). Buttons **Apply** / **Cancel**. Cancel changes nothing. Result card: "Added 38. 4 not found — copy those songs to this device, rescan, and import again." Never say "all restored" if any are missing.
3. **Transfer guide** card with the 4 steps from §1.
4. **Music folder** card (see §7).

ViewModel: `import preview` held as `StateFlow<Preview?>`; on error show a snackbar with the plain reason (no stack traces).

## 7. Music folder & SD card
Facts the model must respect:
- Android 11+ blocks choosing the **root** of internal storage or SD card with the folder picker, and blocks `Download`/`Android/data`. The standard `Music` folder is selectable.
- MediaStore already sees audio on internal storage and SD cards after the system indexes them, so most users only need to paste files into `Music` and tap **Rescan**.

**Create-folder helper** (user picks a parent, app creates `Music` if it does not exist — never auto, never silent):
```kotlin
val pickTree = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { tree ->
    if (tree != null) {
        ctx.contentResolver.takePersistableUriPermission(tree, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        vm.onMusicFolderChosen(tree) } }
// vm.onMusicFolderChosen: 
suspend fun ensureMusicFolder(ctx: Context, tree: Uri): DocumentFile? = withContext(Dispatchers.IO) {
    val root = DocumentFile.fromTreeUri(ctx, tree) ?: return@withContext null
    if (root.name.equals("Music", true)) root else (root.findFile("Music")?.takeIf { it.isDirectory } ?: root.createDirectory("Music"))
}
```
After creation, show a dialog: "Folder ready: **Music** — copy your songs and the favourites JSON here, then open Settings → Rescan." Save the tree URI via `settings.addSafTree()` and include it in scans.

**SAF scan** (for folders MediaStore does not surface):
```kotlin
suspend fun scanTree(ctx: Context, treeUri: Uri, minMs: Long): List<TrackEntity> = withContext(Dispatchers.IO) {
    val out = ArrayList<TrackEntity>(); val exts = setOf("mp3","flac","wav","aac","ogg","m4a","opus","wma","ape","mid","midi")
    fun walk(dir: DocumentFile, path: String) { for (f in dir.listFiles()) {
        if (f.isDirectory) walk(f, path + "/" + (f.name ?: "")) else {
            val name = f.name ?: continue; if (name.substringAfterLast('.', "").lowercase() !in exts) continue
            val r = MediaMetadataRetriever()
            try { r.setDataSource(ctx, f.uri)
                val dur = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L; if (dur < minMs) continue
                val title = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE) ?: name.substringBeforeLast('.')
                val artist = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST) ?: "Unknown artist"
                val album = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM) ?: "Unknown album"
                out += TrackEntity("saf:${f.uri.hashCode()}_${f.length()}", f.uri.toString(), name, title, artist, album, null, dur, f.length(), null, path,
                    f.lastModified(), f.lastModified(), 0, 0, norm(title), norm(artist), norm(album))
            } catch (_: Exception) {} finally { r.release() } } } }
    DocumentFile.fromTreeUri(ctx, treeUri)?.let { walk(it, it.name ?: "Music") }; out }
```
Run in batches with progress and cancellation; call `markSafUnavailable()` only after a successful full traversal.

## 8. Tests the model must add
- `FavoritesCodecTest`: encode→decode round trip; invalid JSON; wrong schema; unsupported version; over-size; skipped items; extra unknown fields ignored.
- `FavoritesMatcherTest`: moved path still matches; same title different artist is not matched; two versions → Ambiguous; duplicates in file; import twice → no duplicate favorites; Replace only affects favorites.
- Cancel/failed import leaves favorites unchanged.

## 9. Acceptance for this part
Real export file opens as valid JSON with real data; import on a second install after copying songs restores favorites; preview and counts are truthful; cancel is safe; no music files are touched; `brain.md` updated.
