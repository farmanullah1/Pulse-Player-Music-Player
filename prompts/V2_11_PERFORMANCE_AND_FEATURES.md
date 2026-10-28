# V2-11 — Performance, Gap Fixes, Extra Features (with code)

> Read only this file for this step. It adds to files 01-09. Output complete files. Say honestly what you could not compile or test.
> Context: the owner's real library is **~13,500 songs** (mostly .m4a, many near-duplicate names). Design every list and query for that size.

## 1. Performance rules (must follow)
| Area | Rule |
|---|---|
| Lists | `LazyColumn(key = { it.id }, contentType = { "track" })`; UI model `@Immutable data class Track`; map `TrackWithFav -> Track` in the ViewModel with `.flowOn(Dispatchers.Default)`, not in composition |
| Recomposition | collect with `collectAsStateWithLifecycle()`; collect the position flow ONLY inside `SeekBar` and `MiniPlayer` progress line; use `derivedStateOf` for "is current row" |
| Position flow | emit every 250 ms only while playing and subscribed: `flow { while (true) { emit(p.currentPosition); delay(250) } }.distinctUntilChanged().stateIn(scope, SharingStarted.WhileSubscribed(5000), 0L)` |
| Artwork | rows request 128px, grid 256px, Now Playing 512px; `LruCache` 24MB; load in `produceState` keyed by uri so scrolled-away rows cancel; never decode on the main thread |
| Search | debounce 250 ms; Room `LIKE '%q%'` on `normTitle/normArtist/normAlbum`, `LIMIT 100`, run on IO; fine for 13k rows |
| Scan | read MediaStore in 500-row batches, `@Upsert`, never `deleteAll`; report progress at most every 250 ms |
| Startup | `PulseApp.onCreate` creates objects lazily (`by lazy`); no disk work before first frame; restore queue AFTER first frame |
| DB | indexes already in file 03; add `Index("normTitle")` only if profiling shows slow search; WAL is Room default |
| APK | R8 + resource shrinking ON in release; add `androidx.profileinstaller:profileinstaller:1.4.1` (use latest stable) and generate a Baseline Profile if time allows |
| Animation | only `graphicsLayer`/`drawBehind` based animation; no `Modifier.blur` on large areas; no animation while scrolling |
| Memory | never hold more than the visible + 2 screens of bitmaps; `Bitmap.Config.RGB_565` is NOT used for artwork (banding), but decode with `inSampleSize` |

## 2. Fix: scan flicker for big libraries (gap in file 03)
File 03 calls `markMediaStoreUnavailable()` BEFORE upserting, so for a moment all 13k songs are "unavailable" and the list flashes empty. Replace with a scan generation:
```kotlin
// TrackEntity: add  val scanGen: Long = 0
// Migration (Room version +1):
val MIGRATION_2_3 = object : Migration(2, 3) { override fun migrate(db: SupportSQLiteDatabase) {
    db.execSQL("ALTER TABLE tracks ADD COLUMN scanGen INTEGER NOT NULL DEFAULT 0") } }
// TrackDao
@Query("SELECT COALESCE(MAX(scanGen),0) FROM tracks") suspend fun maxGen(): Long
@Query("UPDATE tracks SET available = 0 WHERE id LIKE 'ms:%' AND scanGen < :gen") suspend fun markMsMissing(gen: Long)
@Query("UPDATE tracks SET available = 0 WHERE id LIKE 'saf:%' AND scanGen < :gen") suspend fun markSafMissing(gen: Long)
// Scanner: val gen = db.tracks().maxGen() + 1 ; every upserted TrackEntity gets scanGen = gen, available = true ;
// after a SUCCESSFUL full cursor pass (cursor non-null): db.tracks().markMsMissing(gen)
```
Also skip junk (required by V2_13): `if (durationMs <= 0 || sizeBytes < 1024) continue`. Keep favourites on unavailable tracks (never delete).
If the project did not add the backup migration yet, merge both migrations into one.

## 3. Fix: persist the Songs sort the way the backup expects
Backup stores sort as `{ "All_Song": {"key":"dateAdded","order":-1}, ... }` (reference shape). Songs sort keys: `title, artist, dateAdded, duration`; order `1` ascending, `-1` descending.
```kotlin
data class SortPref(val key: String, val order: Int)
class SortPrefs(private val s: SettingsStore) {
    suspend fun get(screen: String): SortPref { val o = runCatching { JSONObject(s.sortJson() ?: "{}") }.getOrDefault(JSONObject())
        return o.optJSONObject(screen)?.let { SortPref(it.optString("key", "dateAdded"), if (it.optInt("order", -1) == 1) 1 else -1) } ?: SortPref("dateAdded", -1) }
    suspend fun set(screen: String, p: SortPref) { val o = runCatching { JSONObject(s.sortJson() ?: "{}") }.getOrDefault(JSONObject())
        o.put(screen, JSONObject().put("key", p.key).put("order", p.order)); s.setSortJson(o.toString()) }
}
```
Songs screen uses `"All_Song"`; Albums `"Album_"`; Artists `"Artist_"`. The UI sort menu reads/writes through `SortPrefs`.

## 4. Fix list (small gaps found while auditing the earlier files)
1. **Back handling**: `BackHandler(enabled = nowPlayingExpanded) { collapse() }` and for every non-root `Screen`; predictive back animation allowed.
2. **Notification permission**: request `POST_NOTIFICATIONS` (API 33+) on first Play, not at launch. If denied, playback still works; show a one-time info chip.
3. **Open with Pulse** (expected from a music player): manifest
```xml
<intent-filter><action android:name="android.intent.action.VIEW"/><category android:name="android.intent.category.DEFAULT"/>
  <category android:name="android.intent.category.BROWSABLE"/><data android:scheme="content"/><data android:scheme="file"/>
  <data android:mimeType="audio/*"/><data android:mimeType="application/ogg"/></intent-filter>
```
`MainActivity.onNewIntent`/`onCreate`: if `intent.action == ACTION_VIEW && intent.data != null` -> `vm.playExternal(uri)` (build a single `MediaItem` from the URI; do NOT insert it in the library table). Clear the intent after handling.
4. **Widget deep link**: handle `intent.action == "pulse.PLAY_TRACK"` with extra `id` -> `vm.playTrackById(id)` (file V2_12).
5. **Zero-duration / broken files**: Player error listener shows a snackbar "Can't play this file" and skips to the next after 800 ms; mark the track `unplayable` in memory for the session.
6. **Process death**: queue restore (file 04 section 7) runs in the service, not only the Activity; UI state re-derives from the controller.
7. **Foreground service start rule** (Android 12+): the widget calls a `MediaController`; if the service is not running Android may refuse to start playback from the background. Show nothing fake: if `connect` fails, open the app instead (`actionStartActivity`).
8. **Sleep timer** survives rotation (state in service or ViewModel, not in a composable).
9. **Audio focus**: `setAudioAttributes(attrs, true)` plus `setHandleAudioBecomingNoisy(true)` already in 04; also duck instead of pause for short notification sounds (Media3 default behaviour; do not override).
10. **Reduced motion**: read system `Settings.Global.ANIMATOR_DURATION_SCALE == 0f` once at start and OR it with the in-app switch.
11. **Excluded folders**: after changing the list, offer "Rescan now" (do not rescan silently).
12. **Hidden formats switch**: hide `.opus/.awb/.mid` etc. only in UI queries, never delete rows.

## 5. Extra features (implement in this order; each must fully work)

### 5.1 A-Z fast scroller (Songs, Albums, Artists)
```kotlin
@Composable fun AlphaScroller(firstIndex: Map<Char, Int>, listState: LazyListState, modifier: Modifier = Modifier) {
    val letters = remember(firstIndex) { ('A'..'Z').filter { it in firstIndex } + if ('#' in firstIndex) listOf('#') else emptyList() }
    val scope = rememberCoroutineScope(); var active by remember { mutableStateOf<Char?>(null) }
    var h by remember { mutableIntStateOf(1) }
    fun jump(y: Float) { val i = ((y / h) * letters.size).toInt().coerceIn(0, letters.lastIndex); val c = letters[i]
        if (c != active) { active = c; scope.launch { listState.scrollToItem(firstIndex.getValue(c)) } } }
    Box(modifier.fillMaxHeight().width(28.dp).onSizeChanged { h = it.height.coerceAtLeast(1) }
        .pointerInput(letters) { detectVerticalDragGestures(onDragStart = { jump(it.y) }, onDragEnd = { active = null }, onDragCancel = { active = null }) { c, _ -> jump(c.position.y) } }
        .pointerInput(letters) { detectTapGestures { jump(it.y); active = null } }) {
        Column(Modifier.fillMaxHeight(), verticalArrangement = Arrangement.SpaceEvenly, horizontalAlignment = Alignment.CenterHorizontally) {
            letters.forEach { Text(it.toString(), fontSize = 10.sp, color = if (it == active) Pulse.Cyan else Pulse.TextMid) } } }
    active?.let { Box(Modifier.align(Alignment.CenterStart).offset(x = (-64).dp).size(56.dp).clip(CircleShape).background(Brush.linearGradient(Pulse.GradPrimary)), Alignment.Center) { Text(it.toString(), fontSize = 24.sp, fontWeight = FontWeight.Bold) } }
}
fun firstLetter(title: String): Char = title.trim().firstOrNull()?.uppercaseChar()?.let { if (it in 'A'..'Z') it else '#' } ?: '#'
// firstIndex = songs.withIndex().groupBy { firstLetter(it.value.title) }.mapValues { it.value.first().index }   (compute in the ViewModel, only when sorted by title)
```
Show it only when sort key is title or artist. Add haptic tick on letter change.

### 5.2 Synced lyrics (.lrc)
```kotlin
data class LrcLine(val timeMs: Long, val text: String)
object Lrc {
    private val tag = Regex("""\[(\d{1,3}):(\d{2})(?:[.:](\d{1,3}))?]""")
    fun parse(src: String): List<LrcLine> = buildList {
        for (raw in src.lineSequence()) { val ms = tag.findAll(raw).toList(); if (ms.isEmpty()) continue
            val text = raw.substring(ms.last().range.last + 1).trim()
            for (m in ms) { val min = m.groupValues[1].toLong(); val sec = m.groupValues[2].toLong(); val frac = m.groupValues[3]
                val msv = if (frac.isEmpty()) 0L else frac.padEnd(3, '0').take(3).toLong(); add(LrcLine(min * 60_000 + sec * 1000 + msv, text)) } }
    }.sortedBy { it.timeMs }
    fun activeIndex(lines: List<LrcLine>, posMs: Long): Int { var lo = 0; var hi = lines.lastIndex; var ans = -1
        while (lo <= hi) { val mid = (lo + hi) ushr 1; if (lines[mid].timeMs <= posMs) { ans = mid; lo = mid + 1 } else hi = mid - 1 }; return ans }
}
```
Where lyrics come from (no network): (a) user taps **Choose .lrc file** -> `OpenDocument` -> copy text (max 512 KB) to `filesDir/lyrics/<trackId>.lrc`; (b) if the music folder was chosen via SAF tree, look for `<name>.lrc` beside the file. If a file has no timestamps show it as plain scrolling text. Lyrics view: active line large + accent colour, others dimmed, auto-scroll to keep active line at 35% height, tap a line = seek there. Empty state: "No lyrics found" + button. (The reference backup's lyrics file is empty; lyrics are not part of the archive in v1.)

### 5.3 Smart playlists (read-only, live)
```kotlin
@Query("SELECT t.*, (f.trackId IS NOT NULL) AS isFavorite FROM tracks t LEFT JOIN favorites f ON f.trackId=t.id WHERE t.available=1 AND NOT EXISTS (SELECT 1 FROM play_events e WHERE e.trackId=t.id) ORDER BY t.dateAddedMs DESC LIMIT 500")
fun neverPlayed(): Flow<List<TrackWithFav>>
@Query("SELECT t.*, 1 AS isFavorite FROM favorites f JOIN tracks t ON t.id=f.trackId WHERE t.available=1 AND NOT EXISTS (SELECT 1 FROM play_events e WHERE e.trackId=t.id AND e.playedAtMs > :since) ORDER BY f.addedAtMs DESC")
fun forgottenFavorites(since: Long): Flow<List<TrackWithFav>>
@Query("SELECT t.*, (f.trackId IS NOT NULL) AS isFavorite FROM tracks t LEFT JOIN favorites f ON f.trackId=t.id WHERE t.available=1 AND t.durationMs >= 420000 ORDER BY t.durationMs DESC LIMIT 500")
fun longTracks(): Flow<List<TrackWithFav>>
```
Show them as extra cards under the 4 system playlists: "Never played", "Forgotten favourites (30 days)", "Long tracks (7 min+)". Hide a card when empty. Counts are real.

### 5.4 Duplicate finder (list only; never deletes)
```kotlin
data class DupGroup(val normTitle: String, val normArtist: String, val c: Int)
@Query("SELECT normTitle, normArtist, COUNT(*) AS c FROM tracks WHERE available=1 AND normTitle != '' GROUP BY normTitle, normArtist HAVING c > 1 ORDER BY c DESC LIMIT 200") suspend fun dupGroups(): List<DupGroup>
@Query("SELECT * FROM tracks WHERE available=1 AND normTitle=:t AND normArtist=:a ORDER BY sizeBytes DESC") suspend fun dupMembers(t: String, a: String): List<TrackEntity>
```
Screen under Settings -> Library: groups with their file names, size, folder, duration; actions per track: Play, Share, "Open folder name" (copy path text). **No delete button** (rule: never delete the user's music). Explain: "Remove duplicates in your file manager, then Rescan."

### 5.5 Playback speed, pitch, stop-after-track, skip silence
- Speed 0.5-2.0 in 0.05 steps + pitch lock switch: `controller.setPlaybackParameters(PlaybackParameters(speed, if (lockPitch) 1f else speed))`.
- Stop after current track: flag in the service; `onMediaItemTransition` with reason AUTO and flag set -> `pause()` + clear flag; a chip shows it is armed.
- Skip silence: `ExoPlayer.setSkipSilenceEnabled(bool)` lives on the service's player; expose it through a custom `SessionCommand("pulse.SKIP_SILENCE")`. Verify the exact Media3 signatures for the installed version; if unsupported, hide the switch.

### 5.6 Listening streak + insights (real data only)
`streakDays`: consecutive local days (ending today or yesterday) that have at least one play event. Query distinct days: `SELECT DISTINCT date(playedAtMs/1000,'unixepoch','localtime') d FROM play_events ORDER BY d DESC LIMIT 400`, compute in Kotlin. Show on For you tile "Today" subtitle ("5 day streak") and in the Recap screen. Hide when 0. Recap badges (extra): "Night Owl" (>=40% after 22:00 or before 05:00), "100 Hours Club", "On Repeat" (a song >= 30 plays), "Explorer" (>= 100 different songs in the month).

### 5.7 Queue and gestures
Swipe row right = play next, left = add to queue (haptic + Undo snackbar); drag handles in queue; "Save queue as playlist" button; "Clear queue" confirm. Now Playing double-tap = favourite.

### 5.8 Share
Share song file via `ACTION_SEND` with its content URI (`FLAG_GRANT_READ_URI_PERMISSION`, MIME from the track). Share recap card (file 08).

## 6. Out of scope (still)
Streaming, accounts, ads, analytics, cloud sync, online lyrics/art, tag editing, audio conversion, Android Auto/Wear, deleting audio files, true crossfade (not reliable in stock Media3; say so, keep fades).

## 7. Acceptance
13k-song list scrolls without jank (no frame drops attributable to our code in a release build; say "NOT MEASURED" if you cannot measure); no flash during rescan; sort persists and appears in `SortBackupConfig` form; A-Z scroller works; LRC parser has unit tests (offsets, multi-tag lines, empty text); smart playlists show real counts; duplicates screen has no delete; Open-with works for one `.mp3`; `brain.md` updated.
