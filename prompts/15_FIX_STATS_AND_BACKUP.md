# 15 — Fix: Session-Based Statistics and Hardened Full Backup

> Overrides: `04` §play-history capture, `08` recap queries, `10` §restore rules, `11` §5 (validation), §7 (export/restore). Uses the schema additions from file 14 §2. Blueprint code: adapt to the project.

---
## PART A — Statistics sessions (replaces the old "log on track end" code)

### 1.1 What counts as listening time
A **listening session** is one continuous stretch in which a single track was actually playing audio.
- Measured with a **monotonic clock** (`SystemClock.elapsedRealtime()`), never `System.currentTimeMillis()` (wall-clock changes, time-zone and DST changes must not alter durations).
- Time counts only while `isPlaying == true` (`playWhenReady && state == READY && no suppression`). Buffering, paused, ended and audio-focus-lost time is **not** counted.
- Real listened time is **speed-aware**: a segment of wall time `dt` at speed `s` adds `dt` to `listenedRealMs` and `dt * s` to `mediaMs`. Recap "time listened" uses `listenedRealMs`; "played fraction" uses `mediaMs / durationMs`.
- A session is closed (and written) on: pause, track change, seek discontinuity that changes the track, stop, error, queue end, service destroy, sleep-timer stop.
- Seeking inside the same track does **not** end a session; media time keeps accumulating only for segments actually played (a seek never adds the skipped distance).

### 1.2 Count and skip policy (one place, constants)
```kotlin
object StatsPolicy {
    const val COUNT_MIN_REAL_MS = 30_000L            // a play counts after 30 s of real listening…
    const val COUNT_MIN_FRACTION = 0.5f              // …or after half the track, whichever is smaller
    const val SKIP_MAX_FRACTION = 0.25f              // left via next/previous/pick before 25% and before 30 s → skipped
    const val IGNORE_BELOW_REAL_MS = 2_000L          // sessions shorter than this are discarded (accidental taps)
    fun counted(real: Long, media: Long, dur: Long) = real >= minOf(COUNT_MIN_REAL_MS, (dur * COUNT_MIN_FRACTION).toLong().coerceAtLeast(5_000L))
    fun skipped(real: Long, media: Long, dur: Long, userLeft: Boolean) = userLeft && !counted(real, media, dur) && media < dur * SKIP_MAX_FRACTION
}
```
- **Skipped tracks** are stored (`counted = false`, `wasSkipped = true`) so the skip count can be exported, but they never add to "plays", "top songs" or listening totals except in the explicit "skipped" statistic.
- **Repeat-one** loops: each loop is its own session (the item restarts at position 0 — detect with `onPositionDiscontinuity(REASON_AUTO_TRANSITION)` with the same media id).
- Partial listens that are not skips (paused at 40 %, app killed) are stored with `counted` by the formula above.

### 1.3 SessionTracker (the only writer of play history)
```kotlin
class SessionTracker(private val db: PulseDb, private val prefs: SharedPreferences, private val zone: () -> ZoneId = { ZoneId.systemDefault() }) {
    private var trackId: String? = null; private var durationMs = 0L
    private var segStart = 0L; private var segSpeed = 1f; private var running = false
    private var realMs = 0L; private var mediaMs = 0L; private var startedWall = 0L; private var speedWeighted = 0.0

    /** Called from the service's Player.Listener. `userAction` is true when the change came from next/previous/pick. */
    fun onTrackStarted(id: String, dur: Long, playing: Boolean, speed: Float) { close(userLeft = false); trackId = id; durationMs = dur; realMs = 0; mediaMs = 0; speedWeighted = 0.0; startedWall = System.currentTimeMillis(); if (playing) resume(speed) ; checkpoint() }
    fun onPlayingChanged(playing: Boolean, speed: Float) { if (playing) resume(speed) else pause() ; checkpoint() }
    fun onSpeedChanged(speed: Float) { if (running) { pause(); resume(speed) } else segSpeed = speed }
    fun onTrackLeft(userAction: Boolean) = close(userLeft = userAction)
    fun onDestroy() = close(userLeft = false)

    private fun resume(speed: Float) { if (running) return; running = true; segSpeed = speed; segStart = SystemClock.elapsedRealtime() }
    private fun pause() { if (!running) return; val dt = (SystemClock.elapsedRealtime() - segStart).coerceAtLeast(0); realMs += dt; mediaMs += (dt * segSpeed).toLong(); speedWeighted += dt * segSpeed; running = false }

    private fun close(userLeft: Boolean) {
        val id = trackId ?: return; pause()
        val real = realMs; val media = mediaMs; trackId = null; clearCheckpoint()
        if (real < StatsPolicy.IGNORE_BELOW_REAL_MS) return
        val z = zone(); val dt = Instant.ofEpochMilli(startedWall).atZone(z)
        val e = PlayEventEntity(trackId = id, playedAtMs = startedWall, listenedMs = real, zoneId = z.id,
            monthKey = "%04d%02d".format(dt.year, dt.monthValue),
            playedFraction = if (durationMs > 0) (media.toFloat() / durationMs).coerceIn(0f, 1f) else 0f,
            wasSkipped = StatsPolicy.skipped(real, media, durationMs, userLeft), speedAvg = if (real > 0) (media.toFloat() / real) else 1f,
            counted = StatsPolicy.counted(real, media, durationMs))
        CoroutineScope(Dispatchers.IO + NonCancellable).launch { db.events().insert(e) }       // short insert; survives service teardown
    }

    // ---- process-death recovery ----
    private fun checkpoint() { val id = trackId ?: return; if (running) { val dt = SystemClock.elapsedRealtime() - segStart; /* include running segment */ prefs.edit().putString("open", "$id|$startedWall|$durationMs|${realMs + dt}|${mediaMs + (dt * segSpeed).toLong()}").apply() } else prefs.edit().putString("open", "$id|$startedWall|$durationMs|$realMs|$mediaMs").apply() }
    private fun clearCheckpoint() = prefs.edit().remove("open").apply()
    /** Call once at service/app start. A checkpoint that is still present means the process died mid-session. */
    suspend fun recoverOpenSession() { val s = prefs.getString("open", null) ?: return; clearCheckpoint(); val p = s.split("|"); if (p.size != 5) return
        val real = p[3].toLongOrNull() ?: return; val media = p[4].toLongOrNull() ?: return; val dur = p[2].toLongOrNull() ?: 0L; val start = p[1].toLongOrNull() ?: return
        if (real < StatsPolicy.IGNORE_BELOW_REAL_MS) return
        val z = ZoneId.systemDefault(); val dt = Instant.ofEpochMilli(start).atZone(z)
        db.events().insert(PlayEventEntity(trackId = p[0], playedAtMs = start, listenedMs = real, zoneId = z.id, monthKey = "%04d%02d".format(dt.year, dt.monthValue), playedFraction = if (dur > 0) (media.toFloat() / dur).coerceIn(0f, 1f) else 0f, wasSkipped = false, speedAvg = if (real > 0) media.toFloat() / real else 1f, counted = StatsPolicy.counted(real, media, dur))) }
}
```
Checkpoint cadence: on every state change plus every 15 s while playing (the service ticker from file 13). The checkpoint can lose at most ~15 s after a crash; that is documented, not hidden.

Wiring in the service listener (file 13): `onMediaItemTransition` → `tracker.onTrackStarted(...)` (with the **previous** item's session closed first, so history is captured **before** player/UI state is replaced); `onIsPlayingChanged` → `onPlayingChanged`; `onPlaybackParametersChanged` → `onSpeedChanged`; `onPositionDiscontinuity(old, new, reason)` with `REASON_SEEK` and a different item → `onTrackLeft(userAction = true)`; `onPlayerError`, `onPlaybackStateChanged(ENDED)` → `onTrackLeft(false)`; `onDestroy` → `tracker.onDestroy()`.

### 1.4 Time zones and months
- `monthKey` is computed **once at write time** in the user's zone at that moment and stored with `zoneId`. Recap for month `YYYYMM` queries `WHERE monthKey = :key` — travelling or DST later never moves an old event into another month.
- Day boundaries for "listening by day" use the stored `zoneId` of each event: `date(playedAtMs/1000,'unixepoch', ...)` is **not** used; instead group in Kotlin with `Instant.atZone(ZoneId.of(e.zoneId))` for the selected month only (≤ a few thousand rows).
- Counted-only queries: totals use `WHERE counted = 1`; top songs `GROUP BY trackId ORDER BY COUNT(*) DESC` with `counted = 1`; skip stats use `wasSkipped = 1`.
- Mature events are never deleted when a file is missing (history keeps its track row, file 14 §1.4); the recap shows "(file missing)" in grey.

---
## PART B — Hardened Full backup (reference format)

### 2.1 Two validation tiers (resolves conflict #8)
| Tier | Applies to | Checks |
|---|---|---|
| **Reference file** (no manifest, e.g. the sample `.wav`) | any archive that lacks `pulse.manifest.json` | structural + ZIP CRC + count sanity (§2.2). Shown with the note "Checksums unavailable for this file type — verified structurally." |
| **Own export** | archive contains `pulse.manifest.json` | everything above **plus** SHA-256 per entry from the manifest |
The extra manifest entry is an unknown file to the reference app; whether that app ignores it is **unverified** (it is only added when the user leaves "Include integrity manifest" ON, default ON, and can be turned off for maximum layout fidelity).
```json
// pulse.manifest.json
{ "format": "pulse-full-1", "createdAt": "2026-10-28T17:22:04Z", "entries": [ {"name":"PlaybackRecords.json","size":1234,"sha256":"…"} ], "counts": {"favorites":12,"playlists":3,"events":340} }
```

### 2.2 Validation limits and rules (before any parsing)
```kotlin
object BackupLimits {
    const val MAX_ARCHIVE_BYTES = 200L shl 20        // whole file
    const val MAX_ENTRIES = 64
    const val MAX_ENTRY_BYTES = 50L shl 20           // uncompressed, per entry
    const val MAX_TOTAL_UNCOMPRESSED = 150L shl 20
    const val MAX_RATIO = 200                         // uncompressed/compressed (zip-bomb guard)
    const val MAX_JSON_ITEMS = 500_000
    val ALLOWED_NAMES = setOf("metadata.inf","PlaybackRecords.json","PlaybackRecords_MF.json" /* …exact names from file 10 §entry table… */, "pulse.manifest.json")
}
fun validateEntryName(n: String): Boolean =
    n.isNotEmpty() && n.length <= 128 && !n.startsWith("/") && !n.startsWith("\\") && !n.contains("..") && !n.contains('\\') && !n.contains(':') && !n.contains('\u0000') && n.none { it.code < 32 } && !n.endsWith("/")
```
Rules, in order (stop at the first failure and show the reason, nothing is written):
1. File size ≤ `MAX_ARCHIVE_BYTES`; first 4 bytes are `PK\x03\x04`.
2. Open with `ZipFile` on the **temp copy** (never on the SAF stream directly). Entry count ≤ `MAX_ENTRIES`.
3. Every entry: `validateEntryName`, not a directory, **name must be in `ALLOWED_NAMES` or the `*.db`/`*.json` pattern documented in file 10** (anything else is ignored, never extracted), `size` and `compressedSize` known and within limits, ratio ≤ `MAX_RATIO`, running total ≤ `MAX_TOTAL_UNCOMPRESSED`. Declared sizes are not trusted: reading goes through `LimitedStream` (file 11 §1) which aborts when the real byte count exceeds the limit.
4. **Never extract to disk by entry name.** JSON is parsed from the stream in memory (`JsonReader`); snapshot `.db` entries are read as bytes only for the header check; nothing is written under a name taken from the archive.
5. ZIP CRC: read each used entry fully once (CRC is verified by `ZipFile` when the stream is read to its end); a `ZipException` = corrupt → abort.
6. `metadata.inf`: required keys present, category flags are `0/1`, version parses; unknown versions → warn, continue best-effort.
7. Count sanity: every JSON array size ≤ `MAX_JSON_ITEMS`; every string ≤ 4 KB; reject NUL characters; numbers in range (play counts ≥ 0, timestamps between 1970 and now + 1 day).
8. Own export only: manifest SHA-256 per entry must match; a mismatch aborts with "File was modified or damaged".

### 2.3 Snapshot database entries are never opened
Replace file 11 §5's "open the snapshot read-only with SQLiteDatabase" with:
```kotlin
fun checkSnapshotEntry(zip: ZipFile, e: ZipEntry): Boolean {
    if (e.size !in 100L..MAX_ENTRY_BYTES) return false
    val head = ByteArray(16); zip.getInputStream(e).use { it.readFully(head) }
    return String(head, Charsets.US_ASCII) == "SQLite format 3\u0000"
}
```
Snapshot data is **not imported from the `.db` files at all**: `PlaybackRecords.json` is a superset of the snapshots (file 10), so restoring uses the JSON only. A snapshot that fails the header check produces a warning and is ignored; it never blocks the import. No `SQLiteDatabase.openDatabase`, no `ATTACH`, no copying an archive DB over the app DB — anywhere.

### 2.4 Preview, merge default, one transaction
Restore is a **two-step flow**: `stage()` (parse + validate + match, no database writes) → **Preview screen** → `apply()`.
```kotlin
data class RestorePreview(
    val tier: ValidationTier, val categories: Map<Category, Int>,           // items found per category
    val favorites: Diff, val playlists: Diff, val events: Diff, val tags: Diff,
    val matched: Int, val ambiguous: Int, val unmatched: Int, val warnings: List<String>)
data class Diff(val add: Int, val update: Int, val same: Int, val skip: Int)
enum class RestoreMode { Merge, Replace }           // default = Merge
```
- The preview lists, per category, a checkbox (default ON for categories present), and the counts "will add / update / unchanged / skipped". Unmatched and ambiguous items are listed (first 50) and are **skipped, never guessed** (file 10 matching tiers).
- **Merge (default):** adds missing favourites, adds playlists (name clash → "Name (2)"), unions tags, adds events not already present (key = trackId + playedAtMs). Never deletes.
- **Replace** (explicit, behind a confirm dialog "This replaces your current <category>"): only for the checked categories; still inside the transaction.
- `apply()` runs **one** `db.withTransaction { ... }` for all selected categories; any exception rolls back everything and shows "Nothing was changed". Settings (DataStore) are written only after the transaction commits.
- Before `apply()` in Replace mode, write an automatic **safety export** (Classic JSON of favourites + playlists names) to the app's private files directory, kept as `pre-restore.json`, so the user can undo manually.
- Large restores batch inserts (500 per statement group) inside the same transaction, report progress, and are cancellable **before** `apply()` begins (not mid-transaction).

### 2.5 Export (own file)
- Written with `ZipOutputStream` to a SAF `CreateDocument` URI via a temp file first, then streamed to the URI; entry names are constants (never user data); `pulse.manifest.json` written last with SHA-256 computed while writing.
- Export reads from a **consistent snapshot**: one `db.withTransaction { read all }`.
- After writing, **re-validate** the produced file with §2.2 (self-check). If the self-check fails, delete the output and show an error.
- File name: `ManualBackup_yyyyMMdd_HHmmssSSS.wav` (same pattern as the sample); the share sheet MIME is `application/octet-stream`.
- Honest label under the button: "Writes the same layout as the reference file. Acceptance by other apps has not been tested."

---
## PART C — Classic vs Full: strictly separate (conflict #5)
| | Classic favourites backup | Full backup (reference format) |
|---|---|---|
| Code | `FavoritesJsonCodec`, `FavoritesMatcher` (file 07) | `FullBackupReader/Writer`, `SafeZip`, `LibraryIndex` (file 11) |
| Package | `data.backup.classic` | `data.backup.full` |
| Content | favourites + added-at, one `.json` | up to 7 categories in a ZIP-in-`.wav` |
| Validation | JSON schema check (file 07) | §2.2 |
| UI | Settings → **Favourites backup** card | Settings → **Full backup** card, own preview screen |
| Tests | `ClassicBackupTest` | `FullBackupValidationTest`, `FullBackupRestoreTest` |
`sniff()` routes a picked file by content to the right flow and, if the user opened the wrong card, offers "This looks like a Full backup — open the Full backup importer?" It never imports silently through the other path. Neither package imports from the other; shared helpers live in `data.backup.common` (`LimitedStream`, `sha1`, `PathKey`).

---
## 3. Acceptance
- The real sample `.wav` shows a preview (reference tier note), imports in Merge mode, and importing it twice changes nothing the second time.
- A zip with `../evil.json`, a `/abs` name, 10 000 entries, an entry declared 1 KB but streaming 60 MB, a ratio-1000 entry, a corrupt CRC and a renamed JSON-as-`.wav` are each rejected with a specific message and **zero** database writes.
- An own export re-imports with the manifest verified; flipping one byte in `PlaybackRecords.json` fails the SHA-256 check.
- Killing the app during `apply()` leaves the database either fully before or fully after.
- A 10-minute listen with 2 minutes paused, 1 minute buffering and speed 1.5 for 3 minutes records `listenedMs ≈ 7 min` real, not 10, and `speedAvg` between 1 and 1.5.
- A skip at 8 s stores `wasSkipped = true, counted = false`; it does not appear in "top songs".
- Force-stop mid-song, relaunch → one recovered event exists with the checkpointed time (≤ 15 s lost).
- Changing the phone's time zone after listening does not move the event to another month in the recap.
