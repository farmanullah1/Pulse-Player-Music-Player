# 14 — Fix: Recoverable Scanning, Identity, SD/SAF/Copy, Queue Model, Permissions, Counts, Artwork

> Replaces/overrides `03_PROJECT_DATA_AND_SCAN.md` §3 (entities), §7 (scanner), §8 (permissions), §9 (artwork) and `04` §3 queue/snapshot code, `07` §7 SAF scan. Blueprint code: adapt imports and Room details to the project.

## 1. Scan model
```kotlin
enum class PartialReason { PermissionRevoked, Cancelled, QueryFailed, EmptyResultSuspicious, StorageUnavailable }
sealed interface ScanState {
    data object Idle : ScanState
    data class Scanning(val found: Int) : ScanState
    data object Reconciling : ScanState
    data class Success(val atMs: Long, val total: Int) : ScanState
    data class Partial(val reason: PartialReason, val lastGoodAtMs: Long?) : ScanState
    data class Failed(val message: String) : ScanState
}
```
Rules (all mandatory):
1. **During `Scanning` nothing is marked unavailable.** New/changed rows may be upserted (they exist), but availability of other rows is untouched.
2. Seen ids are **staged** in a `scan_seen` table. Only in `Reconciling`, after the cursor was fully iterated with no exception and no cancellation, one transaction recomputes availability from `scan_seen`.
3. Cancellation, exception, revoked permission or an unexpectedly empty result → `Partial(...)`; the previous availability snapshot stays exactly as it was.
4. Availability is **recomputed, never deleted**: favourites, playlist rows and play history keep their foreign keys when a file is missing.
5. The scan reads **all** audio with duration > 0. The minimum-duration setting is applied only in display queries (§9), so changing it never makes songs "unavailable".
6. `lastGoodScanAt` is persisted (DataStore) only on `Success`.

## 2. Identity model and the single schema migration
Every track has an explicit identity; `folderPath` is **display only** and is never a key.
```kotlin
// TrackEntity additions (fresh project: edit the entity; installed app: use the migration below)
@ColumnInfo(defaultValue = "'MEDIASTORE'") val sourceType: String = "MEDIASTORE",   // MEDIASTORE | SAF | COPIED
@ColumnInfo(defaultValue = "''") val stableId: String = "",                          // MediaStore _ID, SAF document id, or copy id
@ColumnInfo(defaultValue = "'external_primary'") val volumeId: String = "external_primary", // MediaStore VOLUME_NAME, or the volume part of a SAF document id
val folderUri: String? = null,                                                        // SAF tree uri; null for MediaStore
// primary key id = "ms:<_ID>"  |  "saf:<sha1(treeUri|documentId)>"  |  "copy:<sha1(destDocumentUri)>"   (never derived from a path or file size)

@Entity(tableName = "scan_seen") data class ScanSeenEntity(@PrimaryKey val id: String)
@Entity(tableName = "lyrics") data class LyricsEntity(@PrimaryKey val trackId: String, val text: String, val synced: Boolean, val importedAtMs: Long)
// PlayEventEntity additions (used by file 15 §1):
@ColumnInfo(defaultValue = "''") val zoneId: String = "", @ColumnInfo(defaultValue = "''") val monthKey: String = "",
@ColumnInfo(defaultValue = "0") val playedFraction: Float = 0f, @ColumnInfo(defaultValue = "0") val wasSkipped: Boolean = false,
@ColumnInfo(defaultValue = "1") val speedAvg: Float = 1f, @ColumnInfo(defaultValue = "1") val counted: Boolean = true
// Entity indices: tracks → Index("sourceType","volumeId"); play_events → Index("monthKey")

val MIGRATION_2_3 = object : Migration(2, 3) {          // files 11's 1→2 stays; skip both if the project is fresh
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE tracks ADD COLUMN sourceType TEXT NOT NULL DEFAULT 'MEDIASTORE'")
        db.execSQL("ALTER TABLE tracks ADD COLUMN stableId TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE tracks ADD COLUMN volumeId TEXT NOT NULL DEFAULT 'external_primary'")
        db.execSQL("ALTER TABLE tracks ADD COLUMN folderUri TEXT")
        db.execSQL("UPDATE tracks SET stableId = substr(id, 4) WHERE id LIKE 'ms:%'")
        db.execSQL("UPDATE tracks SET sourceType = 'SAF', stableId = substr(id, 5) WHERE id LIKE 'saf:%'")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_tracks_sourceType_volumeId ON tracks(sourceType, volumeId)")
        db.execSQL("CREATE TABLE IF NOT EXISTS scan_seen (id TEXT NOT NULL, PRIMARY KEY(id))")
        db.execSQL("CREATE TABLE IF NOT EXISTS lyrics (trackId TEXT NOT NULL, text TEXT NOT NULL, synced INTEGER NOT NULL, importedAtMs INTEGER NOT NULL, PRIMARY KEY(trackId))")
        db.execSQL("ALTER TABLE play_events ADD COLUMN zoneId TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE play_events ADD COLUMN monthKey TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE play_events ADD COLUMN playedFraction REAL NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE play_events ADD COLUMN wasSkipped INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE play_events ADD COLUMN speedAvg REAL NOT NULL DEFAULT 1")
        db.execSQL("ALTER TABLE play_events ADD COLUMN counted INTEGER NOT NULL DEFAULT 1")
        db.execSQL("UPDATE play_events SET monthKey = strftime('%Y%m', playedAtMs/1000, 'unixepoch', 'localtime')")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_play_events_monthKey ON play_events(monthKey)")
    }
}
```
Version becomes 3; commit the exported schema JSON; write a migration test (`MigrationTestHelper`) from 2→3 that checks existing favourites/playlists/events survive. **Before any migration**, copy `pulse.db` to `pulse.db.bak-v<old>` (recovery, file 17 §3).

## 3. Device scan (MediaStore) — no file writes, no SAF
```kotlin
@Dao interface ScanDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun seen(ids: List<ScanSeenEntity>)
    @Query("DELETE FROM scan_seen") suspend fun clearSeen()
    @Query("UPDATE tracks SET available = 1 WHERE sourceType = 'MEDIASTORE' AND id IN (SELECT id FROM scan_seen)") suspend fun markSeenAvailable()
    @Query("UPDATE tracks SET available = 0 WHERE sourceType = 'MEDIASTORE' AND id NOT IN (SELECT id FROM scan_seen)") suspend fun markUnseenUnavailable()
    @Query("SELECT COUNT(*) FROM tracks WHERE sourceType = 'MEDIASTORE' AND available = 1") suspend fun availableMediaStore(): Int
}

class LibraryScanner(private val ctx: Context, private val db: PulseDb, private val settings: SettingsStore) {
    private val _state = MutableStateFlow<ScanState>(ScanState.Idle); val state = _state.asStateFlow()
    private val mutex = Mutex()

    suspend fun scanDevice(excluded: Set<String>): ScanState = withContext(Dispatchers.IO) {
        val lastGood = settings.lastGoodScanAt()
        fun partial(r: PartialReason) = ScanState.Partial(r, lastGood).also { _state.value = it }
        if (ContextCompat.checkSelfPermission(ctx, audioPermission()) != PackageManager.PERMISSION_GRANTED) return@withContext partial(PartialReason.PermissionRevoked)
        if (!mutex.tryLock()) return@withContext _state.value                      // a scan is already running
        try {
            _state.value = ScanState.Scanning(0); db.scan().clearSeen()
            val uri = if (Build.VERSION.SDK_INT >= 29) MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL) else MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            val sel = "${MediaStore.Audio.Media.DURATION} > 0 AND ${MediaStore.Audio.Media.SIZE} > 0 AND ${MediaStore.Audio.Media.IS_RINGTONE} = 0 AND ${MediaStore.Audio.Media.IS_NOTIFICATION} = 0 AND ${MediaStore.Audio.Media.IS_ALARM} = 0"
            val cursor = ctx.contentResolver.query(uri, projection(), sel, null, null) ?: return@withContext partial(PartialReason.QueryFailed)
            var found = 0; val rows = ArrayList<TrackEntity>(500); val seen = ArrayList<ScanSeenEntity>(500)
            cursor.use { c ->
                // ...resolve column indexes as in file 03 §7, plus VOLUME_NAME (API 29+) and DATA...
                while (c.moveToNext()) {
                    ensureActive()
                    val t = rowToTrack(c, excluded) ?: continue                       // excluded folders are skipped, so they become unavailable (hidden) after reconcile
                    rows += t; seen += ScanSeenEntity(t.id); found++
                    if (rows.size >= 500) { db.tracks().upsertAll(rows.toList()); db.scan().seen(seen.toList()); rows.clear(); seen.clear(); _state.value = ScanState.Scanning(found) }
                }
            }
            if (rows.isNotEmpty()) { db.tracks().upsertAll(rows.toList()); db.scan().seen(seen.toList()) }
            // the whole cursor was read with no exception → only now is it safe to reconcile
            _state.value = ScanState.Reconciling
            if (found == 0 && db.scan().availableMediaStore() > 0) return@withContext partial(PartialReason.EmptyResultSuspicious)   // never wipe a library on an empty answer
            db.withTransaction { db.scan().markSeenAvailable(); db.scan().markUnseenUnavailable() }
            val now = System.currentTimeMillis(); settings.setLastGoodScanAt(now)
            ScanState.Success(now, found).also { _state.value = it }
        } catch (e: CancellationException) { partial(PartialReason.Cancelled); throw e }
        catch (e: SecurityException) { partial(PartialReason.PermissionRevoked) }
        catch (e: Exception) { Log.w("Scan", "failed", e); partial(PartialReason.QueryFailed) }
        finally { withContext(NonCancellable) { db.scan().clearSeen() }; mutex.unlock() }
    }
}
```
`rowToTrack` fills `sourceType = "MEDIASTORE"`, `stableId = _ID`, `volumeId = VOLUME_NAME` (API 29+, else `"external_primary"`), `dataPath` (file 11 §7), `folderPath` for display. Remove `markMediaStoreUnavailable()` from `TrackDao` (file 03 §4) — **nothing else may set `available = 0`**.

## 4. Three separate workflows (never merge them)
| Workflow | Trigger | Reads | Writes |
|---|---|---|---|
| **Device scan** | Scan / Rescan button, app start (throttled), storage mounted/unmounted broadcast | MediaStore only | Database rows only |
| **SAF folder scan** | User picks/re-picks a folder, Rescan | The chosen tree via `DocumentsContract` | Database rows only (never files) |
| **Copy to Music folder** | The user taps **Copy** and confirms a dialog listing what will be copied | Selected source files/tracks | Files in a user-chosen destination tree; new rows `sourceType = COPIED` |
Selecting a folder never copies anything. Scanning never writes files. Copying is never started by scans.

### 4.1 SAF folder scan (replaces file 07 §7 scan code)
```kotlin
suspend fun scanTree(ctx: Context, db: PulseDb, treeUri: Uri, state: MutableStateFlow<ScanState>): ScanState = withContext(Dispatchers.IO) {
    val persisted = ctx.contentResolver.persistedUriPermissions.any { it.uri == treeUri && it.isReadPermission }
    if (!persisted) return@withContext ScanState.Partial(PartialReason.StorageUnavailable, null)       // banner: "Re-select this folder"
    val seen = HashSet<String>(); val rows = ArrayList<TrackEntity>()
    fun walk(docId: String, display: String) {
        val children = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, docId)
        val cols = arrayOf(Document.COLUMN_DOCUMENT_ID, Document.COLUMN_DISPLAY_NAME, Document.COLUMN_MIME_TYPE, Document.COLUMN_SIZE, Document.COLUMN_LAST_MODIFIED)
        ctx.contentResolver.query(children, cols, null, null, null)?.use { c -> while (c.moveToNext()) {
            val id = c.getString(0); val name = c.getString(1) ?: continue; val mime = c.getString(2)
            if (mime == Document.MIME_TYPE_DIR) { walk(id, "$display/$name"); continue }
            if (!isAudio(name, mime)) continue
            val tid = "saf:" + sha1("$treeUri|$id")                                                       // stable: survives size/mtime changes
            seen += tid
            val existing = db.tracks().byId(tid)
            if (existing != null && existing.sizeBytes == c.getLong(3) && existing.dateModifiedMs == c.getLong(4)) { /* unchanged: skip metadata read */ }
            else readMetadata(ctx, DocumentsContract.buildDocumentUriUsingTree(treeUri, id), tid, id, display, c)?.let { rows += it }
        } } ?: throw IOException("Folder unreadable")
    }
    try {
        walk(DocumentsContract.getTreeDocumentId(treeUri), treeName(treeUri))
        db.withTransaction { db.tracks().upsertAll(rows); db.tracks().markSafUnseenForTree(treeUri.toString(), seen.toList()) }   // reconcile only after a full successful walk
        ScanState.Success(System.currentTimeMillis(), seen.size)
    } catch (e: CancellationException) { throw e }
    catch (e: Exception) { ScanState.Partial(PartialReason.StorageUnavailable, null) }                     // card removed / permission lost: nothing is changed
}
```
`markSafUnseenForTree` is a chunked update (SQLite parameter limit): stage ids in `scan_seen` exactly like §3 and run `UPDATE tracks SET available = (id IN (SELECT id FROM scan_seen)) WHERE sourceType='SAF' AND folderUri = :tree`. `volumeId` = the part of the tree document id before `:` (`primary`, `1A2B-3C4D`). Request **read-only** persistable permission for scanning (`FLAG_GRANT_READ_URI_PERMISSION`); never assume a path exists; keep the grant list under the platform limit (release grants of folders the user removes: `releasePersistableUriPermission`).

### 4.2 Copy to Music folder (explicit action)
- Needs a destination tree with **write** permission (separate picker, `FLAG_GRANT_WRITE_URI_PERMISSION`).
- For each source (from `OpenMultipleDocuments` or selected tracks): create the document with `DocumentsContract.createDocument` using a unique name (`name (1).ext` instead of overwriting), stream-copy in 64 KB chunks, update a real progress count, support cancel (delete the partial destination file), then insert the new row as `COPIED` (`id = "copy:" + sha1(destUri)`) with metadata read from the copy.
- Report: copied / skipped (existing name) / failed with reasons. Never move or delete the source.
- Display rule: a `COPIED` row is hidden if an equal `MEDIASTORE` row exists (same size, normalised title, duration within 2 s) so a later device scan doesn't show duplicates (§9 query).

## 5. Deleted files, revoked permission, removable storage
- **Deleted file:** the next successful scan marks it `available = 0`. It leaves the default library but appears under **Missing songs** (a filter on the Songs screen). Favourites/playlists/history rows stay.
- **Permission revoked:** `scanDevice` returns `Partial(PermissionRevoked)` without touching availability; a banner explains and links to settings (§8). Re-granting + the next scan restores visibility.
- **SD card removed/inserted:** register a receiver for `ACTION_MEDIA_MOUNTED`, `ACTION_MEDIA_UNMOUNTED`, `ACTION_MEDIA_EJECT` (or `StorageManager.registerStorageVolumeCallback`) and trigger a device scan (and re-scan of SAF trees on that volume). Because §3 reconciles from the staged ids, only tracks of the missing volume become unavailable; they return automatically after re-insertion. SAF errors on a volume mark only that tree `Partial(StorageUnavailable)`.

## 6. Queue-order model (Media3's shuffle flag is never enabled)
```kotlin
class QueueModel(original: List<String>, startId: String?) {
    var original: List<String> = original.distinct(); private set          // each track at most once in a queue
    var shuffled: List<String> = emptyList(); private set
    var shuffleEnabled = false; private set
    var seed = 0L; private set
    var currentId: String? = startId ?: original.firstOrNull()
    val active: List<String> get() = if (shuffleEnabled) shuffled else original
    val currentIndex: Int get() = active.indexOf(currentId)

    fun enableShuffle(newSeed: Long) {                                       // deterministic for a given seed
        seed = newSeed; shuffleEnabled = true
        shuffled = listOfNotNull(currentId) + (original - setOfNotNull(currentId)).shuffled(Random(newSeed))
    }
    fun disableShuffle() { shuffleEnabled = false; shuffled = emptyList() }  // current track is found again in `original`; playback never restarts
    fun nextId(repeat: Int): String? { val a = active; val i = currentIndex; if (a.isEmpty() || i < 0) return null
        return when { repeat == Player.REPEAT_MODE_ONE -> currentId; i + 1 < a.size -> a[i + 1]; repeat == Player.REPEAT_MODE_ALL -> a.first(); else -> null } }
    fun previousId(): String? = active.getOrNull(currentIndex - 1)
    fun playNext(id: String) { original = original.insertAfter(currentId, id); if (shuffleEnabled) shuffled = shuffled.insertAfter(currentId, id) }
    fun append(id: String): Boolean { if (id in original) return false; original = original + id; if (shuffleEnabled) shuffled = shuffled + id; return true }
    fun remove(id: String): String? {                                         // returns the new current id when the current track was removed
        val wasCurrent = id == currentId; val idx = currentIndex
        original = original - id; shuffled = shuffled - id
        if (wasCurrent) currentId = active.getOrNull(idx) ?: active.lastOrNull()
        return if (wasCurrent) currentId else null }
    fun move(from: Int, to: Int) { if (shuffleEnabled) shuffled = shuffled.moved(from, to) else original = original.moved(from, to) }
    private fun List<String>.insertAfter(anchor: String?, id: String): List<String> { val l = this - id; val i = l.indexOf(anchor); return l.toMutableList().apply { add(if (i < 0) 0 else i + 1, id) } }
    private fun List<String>.moved(f: Int, t: Int) = toMutableList().apply { add(t.coerceIn(0, size), removeAt(f)) }
}
```
Applying an order to Media3 **without restarting the current item**:
```kotlin
fun MediaController.reorderAroundCurrent(order: List<MediaItem>, currentId: String) {
    val cur = currentMediaItemIndex; val idx = order.indexOfFirst { it.mediaId == currentId }; require(idx >= 0)
    if (cur < mediaItemCount - 1) removeMediaItems(cur + 1, mediaItemCount)
    if (cur > 0) removeMediaItems(0, cur)
    addMediaItems(0, order.subList(0, idx)); addMediaItems(order.subList(idx + 1, order.size))      // the playing item is never touched
}
```
`PlayerController` keeps one `QueueModel` and a `Map<String, MediaItem>`; `toggleShuffle()` = `enableShuffle(Random.nextLong())` or `disableShuffle()` then `reorderAroundCurrent(...)`. `PlaybackUiState.shuffle` comes from the model. `playQueue(tracks, start, shuffle)` builds the model, applies the active order and starts at the current track. Adding a track already in the queue is ignored with a message; `playNext` of an existing track moves it.

## 7. Queue snapshot (identity-safe)
```kotlin
@Serializable data class QueueSnapshot(val original: List<String>, val shuffled: List<String>, val currentId: String?, val positionMs: Long,
                                       val repeat: Int, val shuffleEnabled: Boolean, val seed: Long, val savedAtMs: Long)
// stored as ONE JSON string in DataStore (use org.json; no new dependency)
data class Restored(val model: QueueModel, val positionMs: Long, val startPaused: Boolean)

fun restoreQueue(s: QueueSnapshot, availableIds: Set<String>): Restored? {
    val orig = s.original.filter { it in availableIds }; if (orig.isEmpty()) return null
    val m = QueueModel(orig, null)
    val activeBefore = if (s.shuffleEnabled) s.shuffled else s.original
    val stillThere = s.currentId in availableIds
    val newCurrent = if (stillThere) s.currentId else {
        // nearest surviving track at or after the old index in the old active order, else the last survivor before it
        val i = activeBefore.indexOf(s.currentId).coerceAtLeast(0)
        activeBefore.drop(i).firstOrNull { it in availableIds } ?: activeBefore.take(i).lastOrNull { it in availableIds } ?: orig.first() }
    m.currentId = newCurrent
    if (s.shuffleEnabled) { m.enableShuffleFrom(s.shuffled.filter { it in availableIds }, s.seed, newCurrent) }
    return Restored(m, if (stillThere) s.positionMs else 0L, startPaused = true)       // always restored paused; never silently a different track at the old position
}
```
`enableShuffleFrom` sets `shuffled` to the saved (filtered) order with `newCurrent` moved to the front if it changed. Save the snapshot on: pause, track change, shuffle/repeat/queue edits (debounced 500 ms), and every 15 s while playing — **never per UI frame**. If the JSON is corrupt or empty, ignore it and start with an empty queue (no crash). Show a one-line notice when tracks were dropped ("3 songs in your last queue are unavailable").

## 8. Permission state
```kotlin
enum class PermissionState { Granted, Limited, Denied, PermanentlyDenied }

class PermissionMonitor(private val ctx: Context, private val settings: SettingsStore) {
    private val _state = MutableStateFlow(PermissionState.Denied); val state = _state.asStateFlow()
    /** Call on Activity.onResume and right after a permission result. */
    suspend fun refresh(activity: Activity?) {
        val granted = ContextCompat.checkSelfPermission(ctx, audioPermission()) == PackageManager.PERMISSION_GRANTED
        val saf = ctx.contentResolver.persistedUriPermissions.any { it.isReadPermission }
        val asked = settings.audioPermissionAsked()
        _state.value = when {
            granted -> PermissionState.Granted
            saf -> PermissionState.Limited                                    // only the folders the user chose are visible
            asked && activity != null && !ActivityCompat.shouldShowRequestPermissionRationale(activity, audioPermission()) -> PermissionState.PermanentlyDenied
            else -> PermissionState.Denied }
    }
}
```
UI: **Denied** → explanation + **Allow access** (requests, then `setAudioPermissionAsked(true)`). **PermanentlyDenied** → "Allow music access in Settings" + **Open settings** (`ACTION_APPLICATION_DETAILS_SETTINGS`). **Limited** → banner "Only the folders you chose are visible", the list of chosen folders and **Choose another folder** / **Allow audio access**. (Android has no partial audio-library permission; "Limited" means folder-only access.) **Granted** after revocation mid-session → the next `refresh` shows the banner and the scanner returns `Partial(PermissionRevoked)`; the old library stays visible.

## 9. Counts and filters must match the visible library
```kotlin
// in TrackDao: one constant used by every query so they can never drift
private const val VISIBLE = "t.available = 1 AND t.durationMs >= :minMs AND NOT (t.sourceType = 'COPIED' AND EXISTS (SELECT 1 FROM tracks m WHERE m.sourceType = 'MEDIASTORE' AND m.available = 1 AND m.sizeBytes = t.sizeBytes AND m.normTitle = t.normTitle AND ABS(m.durationMs - t.durationMs) < 2000))"
@Query("SELECT t.*, (f.trackId IS NOT NULL) AS isFavorite FROM tracks t LEFT JOIN favorites f ON f.trackId = t.id WHERE $VISIBLE ORDER BY t.title COLLATE NOCASE") fun observeAll(minMs: Long): Flow<List<TrackWithFav>>
@Query("SELECT COUNT(*) FROM tracks t WHERE $VISIBLE") fun observeCount(minMs: Long): Flow<Int>
@Query("SELECT t.*, (f.trackId IS NOT NULL) AS isFavorite FROM tracks t LEFT JOIN favorites f ON f.trackId = t.id WHERE t.available = 0 ORDER BY t.title COLLATE NOCASE") fun observeMissing(): Flow<List<TrackWithFav>>
```
The header "N Songs" uses `observeCount(minMs)`; a **Missing songs (n)** chip on the Songs screen shows `observeMissing()` and explains "These files weren't found on the last scan. Your favourites and playlists keep them."

## 10. Artwork cache
```kotlin
data class ArtRef(val stableKey: String, val uri: String, val modifiedMs: Long)       // stableKey = track id
object ArtworkLoader {
    private val sizes = intArrayOf(96, 192, 384, 768)
    private fun bucket(px: Int) = sizes.firstOrNull { it >= px } ?: sizes.last()      // consistent sizes → fewer variants, no key explosion
    private val cache = object : LruCache<String, Bitmap>(maxOf(8 shl 20, (Runtime.getRuntime().maxMemory() / 16).toInt())) { override fun sizeOf(k: String, v: Bitmap) = v.byteCount }
    fun key(r: ArtRef, px: Int) = "${r.stableKey}|${r.modifiedMs}|${bucket(px)}"       // a changed file (modifiedMs) can never serve stale art
    suspend fun load(ctx: Context, r: ArtRef, px: Int): Bitmap? = withContext(Dispatchers.IO) {
        val b = bucket(px); val k = key(r, px); cache.get(k)?.let { return@withContext it }
        val bmp = runCatching { /* loadThumbnail / MediaMetadataRetriever exactly as file 03 §9, using size b */ null as Bitmap? }.getOrNull()
        if (bmp != null) cache.put(k, bmp); bmp }
    fun trim(level: Int) { if (level >= ComponentCallbacks2.TRIM_MEMORY_BACKGROUND) cache.evictAll() else if (level >= ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN) cache.trimToSize(cache.size() / 2) }
}
```
Call `ArtworkLoader.trim(level)` from `Application.onTrimMemory`. Never call `Bitmap.recycle()` on cached bitmaps (Compose may still draw them). `Track` and `PlaybackUiState` carry `modifiedMs` (put it in `MediaMetadata.extras` in `toItem()` and read it in `sync()`), and `Artwork(...)` takes an `ArtRef`. A rescan that finds a different `dateModifiedMs` produces a new key, which is the invalidation.

## 11. Acceptance
- Interrupt a scan (kill the app, turn off permission, cancel) → the library looks exactly as before.
- Unmount the SD card → only that volume's tracks go to **Missing**; remount + scan → they return; favourites never changed.
- Revoke audio permission → banner, old library visible, no changes; grant + scan → normal.
- Songs header count equals the number of visible rows at every minimum-duration setting.
- Shuffle on, play 5, shuffle off → next track is the original-order successor; current track never restarts.
- Queue restore with the current track deleted → paused on the nearest survivor, position 0, notice shown.
