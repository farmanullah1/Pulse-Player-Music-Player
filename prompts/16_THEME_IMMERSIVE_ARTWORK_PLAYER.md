# 16 — New Theme: Immersive Artwork Player

> The **ninth** player theme (`id = "immersive"`). Designed from the reference screenshot: a full-screen blurred copy of the album art as background, a small top bar, a marquee title flanked by shuffle and repeat, a seek bar with times, large floating transport controls, a row of secondary actions, and a **bottom queue panel** whose current row is highlighted in orange.
> **Presentation only.** It reads `PlaybackUiState`, calls the existing `PlayerController`/`MainViewModel` actions and adds **no** player, queue, service, audio session or second playback state (global rule 6, file 12 §8). It coexists with the other eight themes and is selected through the same `playerThemeId` key.

## 1. Layout (portrait, 6.78" AMOLED ≈ 412×915 dp)
```
┌───────────────────────────────┐
│ ⌄  (collapse)     Now playing  ⋮ │  PlayerTopBar            56dp, status-bar inset
│                                   │
│        ┌───────────────┐         │  Foreground artwork (optional, 64% width, 20dp radius)
│        │   artwork     │         │  — or lyrics when the Lyrics toggle is on
│        └───────────────┘         │
│ 🔀   Song title ◂marquee▸   🔁    │  SongInformation (artist below, centred)
│        Artist · Album            │
│ 1:12 ━━━━●━━━━━━━━━━━━━━ 3:45    │  PlaybackSeekBar
│   ⏮    ⏪10   ( ▶ )   10⏩   ⏭   │  PrimaryPlaybackControls (play = 72dp)
│  ♡    ⏰    ＋≡    ☰    ⚙       │  SecondaryPlayerActions
│ ───────────────────────────────  │
│ ▌Queue · all folder songs ⌃       │  QueuePanel (peek 160dp, expands to 70%)
│ [art] Current song (orange)  ▮▮▮ │  QueueTrackRow
│ [art] Next song                  │
└───────────────────────────────┘
```
Widths ≥ 600 dp (tablet / unfolded): two columns — left 55 % artwork + controls, right 45 % the queue/lyrics as a permanent panel (no bottom sheet).
Short screens (height < 640 dp or landscape phone): hide the foreground artwork, keep title + seek + controls + secondary actions, queue panel peeks 56 dp.

## 2. Registration (file 06 edits; also the single-source rows in file 12 §4)
```kotlin
enum class PlayerLayout { CLASSIC, SHEET_QUEUE, MESH_GLASS, MINIMAL_DARK, VINYL, ACOUSTIC, COAST, STARRY, IMMERSIVE }
// append to PlayerThemes (default stays "classic"; unknown ids already fall back to PlayerThemes[0]):
PlayerTheme("immersive", "Immersive Artwork Player", PlayerLayout.IMMERSIVE, listOf(Color(0xFF1B1B22), Color.Black))
// PlayerThemeHost: IMMERSIVE is full-bleed like A/B/C; add this branch BEFORE `else`:
PlayerLayout.IMMERSIVE -> Unit    // NowPlayingScreen renders ImmersivePlayerLayout(...) instead of the standard column
// NowPlayingScreen:
if (theme.layout == PlayerLayout.IMMERSIVE) ImmersivePlayerLayout(s, vm, onBack) else /* existing layouts */ ...
```
Theme selector (`ThemePreviewCard`) gets a preview branch (§8). README/UI strings say "9 player themes".

## 3. Blurred artwork background (one code path, cached, cheap)
No `RenderEffect`/`Modifier.blur` (API 31+ only, and expensive on a full screen). One approach for all API levels: downscale to ~96 px, box-blur once, cache by key, draw scaled up.
```kotlin
object BlurCache {
    private val cache = LruCache<String, Bitmap>(6)                       // ≤ 6 small bitmaps (~36 KB each)
    suspend fun get(key: String, src: Bitmap): Bitmap = cache.get(key) ?: withContext(Dispatchers.Default) {
        val small = Bitmap.createScaledBitmap(src, 96, 96, true)
        val out = boxBlur(small.copy(Bitmap.Config.ARGB_8888, true), radius = 8, passes = 3)
        cache.put(key, out); out }
    fun clear() = cache.evictAll()                                         // call from onTrimMemory
}
fun boxBlur(b: Bitmap, radius: Int, passes: Int): Bitmap {
    val w = b.width; val h = b.height; val px = IntArray(w * h); b.getPixels(px, 0, w, 0, 0, w, h)
    val tmp = IntArray(w * h)
    repeat(passes) { blurH(px, tmp, w, h, radius); blurH(tmp, px, h, w, radius, transpose = false) .also { /* see note */ } }
    b.setPixels(px, 0, w, 0, 0, w, h); return b
}
```
> Implementation note for the model: write a standard separable box blur (`horizontal(src→dst)`, `transpose`, `horizontal`, `transpose`) over the `IntArray`; the helper above only fixes the signature. Cover it with a unit test (a uniform-colour bitmap stays uniform; a half-black/half-white bitmap gets a mid-grey edge, no crash for radius ≥ width/2 — clamp the radius).

```kotlin
@Composable
fun ArtworkBackground(art: ArtRef?, fallback: List<Color>, dim: Float = 0.45f, modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    val bmp by produceState<ImageBitmap?>(null, art?.stableKey, art?.modifiedMs) {
        value = art?.let { ArtworkLoader.load(ctx, it, 96)?.let { b -> BlurCache.get("${it.stableKey}|${it.modifiedMs}", b).asImageBitmap() } } }
    Box(modifier.fillMaxSize().background(Brush.verticalGradient(fallback))) {            // fallback while loading or when there is no art
        Crossfade(bmp, tween(450), label = "blurbg") { b -> if (b != null) Image(b, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Black.copy(dim * 0.6f), Color.Black.copy(dim), Color.Black.copy(0.85f)))))   // keeps white text readable on any art
    }
}
```
Rules: the blur is computed once per `(track, modifiedMs)` off the main thread; a track change crossfades (450 ms; **0 ms when reduced motion is on**); art missing → the theme gradient and a generic note-icon artwork; never recompute per frame; the scrim guarantees ≥ 4.5:1 contrast for white text.

## 4. Components
Shared helper types: `s: PlaybackUiState`, `vm: MainViewModel` (exposes `controller`, `positionFlow`, `toggleFavorite`, `lyrics(trackId)`), colours from `Pulse` (file 02). All icons are the vector icons from file 02 §icons (no emoji).

### 4.1 Top bar
```kotlin
@Composable fun PlayerTopBar(onBack: () -> Unit, onMore: () -> Unit, modifier: Modifier = Modifier) = Row(modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.statusBars).height(56.dp).padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
    IconButton(onBack, Modifier.size(48.dp)) { Icon(PulseIcons.ChevronDown, "Close player", tint = Color.White) }
    Text("Now playing", Modifier.weight(1f), color = Color.White.copy(0.85f), style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center)
    IconButton(onMore, Modifier.size(48.dp)) { Icon(PulseIcons.MoreVert, "More options", tint = Color.White) }
}
```
`onMore` opens the existing track-options sheet (Add to playlist, Share, Set as ringtone, Delete…).

### 4.2 Song information (marquee title between shuffle and repeat)
```kotlin
@Composable fun SongInformation(s: PlaybackUiState, onShuffle: () -> Unit, onRepeat: () -> Unit, reducedMotion: Boolean, modifier: Modifier = Modifier) = Column(modifier.fillMaxWidth().padding(horizontal = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onShuffle, Modifier.size(48.dp)) { Icon(PulseIcons.Shuffle, if (s.shuffle) "Shuffle on" else "Shuffle off", tint = if (s.shuffle) Pulse.Orange else Color.White.copy(0.8f)) }
        Text(s.title.ifBlank { "Unknown title" }, Modifier.weight(1f).then(if (reducedMotion) Modifier else Modifier.basicMarquee(iterations = Int.MAX_VALUE, initialDelayMillis = 1500, velocity = 36.dp)),
            color = Color.White, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold), maxLines = 1, overflow = if (reducedMotion) TextOverflow.Ellipsis else TextOverflow.Clip, textAlign = TextAlign.Center)
        IconButton(onRepeat, Modifier.size(48.dp)) { Icon(if (s.repeat == Player.REPEAT_MODE_ONE) PulseIcons.RepeatOne else PulseIcons.Repeat, "Repeat ${repeatLabel(s.repeat)}", tint = if (s.repeat != Player.REPEAT_MODE_OFF) Pulse.Orange else Color.White.copy(0.8f)) }
    }
    Text(listOf(s.artist, s.album).filter { it.isNotBlank() }.joinToString(" · ").ifBlank { "Unknown artist" }, color = Color.White.copy(0.7f), style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
}
```
`repeatLabel(mode)` returns "off / all / one". The marquee only runs when the title overflows (`basicMarquee` does that itself) and is disabled by reduced motion.

### 4.3 Seek bar
Uses the safe seeking from file 13 (`PlaybackUiState.canSeek`, `seekTo(ms): Boolean`).
```kotlin
@Composable fun PlaybackSeekBar(s: PlaybackUiState, pos: StateFlow<Long>, canSeek: Boolean, onSeek: (Long) -> Unit, modifier: Modifier = Modifier) {
    val p by pos.collectAsStateWithLifecycle(); var drag by remember { mutableStateOf<Float?>(null) }
    val dur = s.durationMs; val frac = drag ?: if (dur > 0) (p.toFloat() / dur).coerceIn(0f, 1f) else 0f
    Column(modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
        Slider(value = frac, onValueChange = { if (canSeek && dur > 0) drag = it }, onValueChangeFinished = { drag?.let { onSeek((it * dur).toLong()) }; drag = null }, enabled = canSeek && dur > 0,
            colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = Color.White, inactiveTrackColor = Color.White.copy(0.25f), disabledThumbColor = Color.White.copy(0.5f), disabledActiveTrackColor = Color.White.copy(0.35f)),
            modifier = Modifier.semantics { stateDescription = "${fmt(p)} of ${if (dur > 0) fmt(dur) else "unknown length"}" })
        Row(Modifier.fillMaxWidth()) { Text(fmt(((drag ?: frac) * dur).toLong().takeIf { dur > 0 } ?: p), color = Color.White.copy(0.8f), style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.weight(1f)); Text(if (dur > 0) fmt(dur) else "--:--", color = Color.White.copy(0.8f), style = MaterialTheme.typography.labelMedium) }
    }
}
```
Unknown duration (`dur <= 0`) → slider disabled, total shows `--:--`, ±10 s buttons stay enabled only when `canSeek`. The position flow is collected here only (the 250 ms ticker lives in the controller), so the rest of the screen does not recompose per tick.

### 4.4 Primary controls
```kotlin
@Composable fun PrimaryPlaybackControls(s: PlaybackUiState, canSeek: Boolean, onPrev: () -> Unit, onBack10: () -> Unit, onToggle: () -> Unit, onFwd10: () -> Unit, onNext: () -> Unit, modifier: Modifier = Modifier) =
    Row(modifier.fillMaxWidth().padding(horizontal = 12.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
        IconButton(onPrev, Modifier.size(56.dp)) { Icon(PulseIcons.SkipPrevious, "Previous song", Modifier.size(34.dp), Color.White) }
        IconButton(onBack10, Modifier.size(48.dp), enabled = canSeek) { Icon(PulseIcons.Replay10, "Back 10 seconds", Modifier.size(28.dp), Color.White.copy(if (canSeek) 0.9f else 0.35f)) }
        Box(Modifier.size(72.dp).clip(CircleShape).background(Color.White).clickable(onClickLabel = if (s.playWhenReady) "Pause" else "Play", role = Role.Button) { onToggle() }, contentAlignment = Alignment.Center) {
            Crossfade(s.playWhenReady, tween(120), label = "pp") { playing -> Icon(if (playing) PulseIcons.Pause else PulseIcons.Play, null, Modifier.size(38.dp), Color(0xFF14141A)) } }
        IconButton(onFwd10, Modifier.size(48.dp), enabled = canSeek) { Icon(PulseIcons.Forward10, "Forward 10 seconds", Modifier.size(28.dp), Color.White.copy(if (canSeek) 0.9f else 0.35f)) }
        IconButton(onNext, Modifier.size(56.dp)) { Icon(PulseIcons.SkipNext, "Next song", Modifier.size(34.dp), Color.White) }
    }
```
The play button reflects `playWhenReady` (so a buffering track shows "pause", not "play"); a small circular progress ring replaces the icon only while `s.buffering` and `playWhenReady`. All touch targets ≥ 48 dp. Play/pause goes through the controller, which goes through the fade path (file 13); the theme never touches volume.

### 4.5 Secondary actions
```kotlin
@Composable fun SecondaryPlayerActions(isFav: Boolean, sleepActive: Boolean, onFav: () -> Unit, onSleep: () -> Unit, onAddToPlaylist: () -> Unit, onQueue: () -> Unit, onTune: () -> Unit, modifier: Modifier = Modifier) =
    Row(modifier.fillMaxWidth().padding(horizontal = 20.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
        ActionIcon(if (isFav) PulseIcons.HeartFilled else PulseIcons.Heart, if (isFav) "Remove from favourites" else "Add to favourites", if (isFav) Pulse.Orange else Color.White, onFav)
        ActionIcon(PulseIcons.Alarm, if (sleepActive) "Sleep timer active" else "Sleep timer", if (sleepActive) Pulse.Orange else Color.White, onSleep)
        ActionIcon(PulseIcons.PlaylistAdd, "Add to playlist", Color.White, onAddToPlaylist)
        ActionIcon(PulseIcons.Queue, "Open queue", Color.White, onQueue)
        ActionIcon(PulseIcons.Tune, "Equalizer and sound", Color.White, onTune)
    }
@Composable private fun ActionIcon(icon: ImageVector, label: String, tint: Color, onClick: () -> Unit) = IconButton(onClick, Modifier.size(48.dp)) { Icon(icon, label, tint = tint) }
```
`onFav` = `vm.toggleFavorite(mediaId)`; `onSleep` opens the existing sleep sheet (file 04, which now uses the controller, file 13); `onQueue` expands the QueuePanel; `onTune` navigates to the Equalizer screen.

## 5. Queue panel (shared by this theme and Type A — extract once)
```kotlin
@Composable fun QueueTrackRow(t: QueueItem, current: Boolean, playing: Boolean, onClick: () -> Unit, onRemove: (() -> Unit)?, modifier: Modifier = Modifier) =
    Row(modifier.fillMaxWidth().heightIn(min = 64.dp).clickable(onClick = onClick).background(if (current) Pulse.Orange.copy(0.14f) else Color.Transparent).padding(horizontal = 16.dp, vertical = 8.dp).semantics(mergeDescendants = true) { if (current) stateDescription = "Now playing" },
        verticalAlignment = Alignment.CenterVertically) {
        Artwork(t.artUri, 48.dp, radius = 8.dp, px = 96); Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(t.title, color = if (current) Pulse.Orange else Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = if (current) FontWeight.SemiBold else FontWeight.Normal)
            Text(t.artist, color = Color.White.copy(0.65f), style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis) }
        if (current && playing) EqualizerBars(Pulse.Orange, Modifier.size(20.dp)) else Text(fmt(t.durationMs), color = Color.White.copy(0.6f), style = MaterialTheme.typography.labelSmall)
        if (onRemove != null && !current) IconButton(onRemove, Modifier.size(40.dp)) { Icon(PulseIcons.Close, "Remove ${t.title} from queue", tint = Color.White.copy(0.6f)) }
    }

@Composable fun QueuePanel(queue: List<QueueItem>, currentIndex: Int, playing: Boolean, expanded: Boolean, onToggle: () -> Unit, onPlayAt: (Int) -> Unit, onRemove: (Int) -> Unit, modifier: Modifier = Modifier) {
    val listState = rememberLazyListState()
    LaunchedEffect(currentIndex, expanded) { if (currentIndex in queue.indices) listState.animateScrollToItem((currentIndex - 1).coerceAtLeast(0)) }
    Surface(modifier, color = Color(0xE61B1B22), shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)) {
        Column {
            Row(Modifier.fillMaxWidth().clickable(onClick = onToggle).padding(horizontal = 20.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Queue · ${queue.size} songs", color = Color.White, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                Icon(if (expanded) PulseIcons.ChevronDown else PulseIcons.ChevronUp, if (expanded) "Collapse queue" else "Expand queue", tint = Color.White) }
            if (queue.isEmpty()) Text("The queue is empty", Modifier.padding(24.dp), color = Color.White.copy(0.6f))
            else LazyColumn(state = listState) { itemsIndexed(queue, key = { _, q -> q.mediaId }) { i, q -> QueueTrackRow(q, i == currentIndex, playing, { onPlayAt(i) }, { onRemove(i) }) } }
        }
    }
}
```
`QueueItem(mediaId, title, artist, artUri, durationMs)` is built from the `QueueModel` active order (file 14 §6), not from Media3's shuffle flag. `key = mediaId` is safe because a queue never holds a track twice. Removing the current row is not offered here. Expand/collapse height animates between 160 dp and 70 % of the screen (`animateDpAsState`, `snap()` when reduced motion). Type A's white sheet keeps its colours and calls the same `QueueTrackRow` through a `colors` parameter if you refactor (optional; do not break Type A).

## 6. Lyrics
### 6.1 Data
The `lyrics` table (file 14 §2) stores one text per track: `LyricsEntity(trackId, text, synced, importedAtMs)`. **No lyrics are downloaded** (the app is offline); lyrics come only from a file the user picks.
```kotlin
@Dao interface LyricsDao {
    @Query("SELECT * FROM lyrics WHERE trackId = :id") fun observe(id: String): Flow<LyricsEntity?>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun put(l: LyricsEntity)
    @Query("DELETE FROM lyrics WHERE trackId = :id") suspend fun remove(id: String)
}
```
### 6.2 LRC parser
```kotlin
data class LyricLine(val timeMs: Long, val text: String)
object LrcParser {
    private val tag = Regex("""\[(\d{1,3}):(\d{2})(?:[.:](\d{1,3}))?]""")
    fun parse(raw: String): List<LyricLine>? {                       // null → not synced (plain text)
        val out = ArrayList<LyricLine>()
        raw.lineSequence().forEach { line ->
            val stamps = tag.findAll(line).toList(); if (stamps.isEmpty()) return@forEach
            val text = line.substring(stamps.last().range.last + 1).trim()
            stamps.forEach { m -> val min = m.groupValues[1].toLong(); val sec = m.groupValues[2].toLong(); val frac = m.groupValues[3]
                val ms = if (frac.isEmpty()) 0L else (frac.padEnd(3, '0').take(3)).toLong()
                out += LyricLine(min * 60_000 + sec * 1000 + ms, text) } }
        return out.sortedBy { it.timeMs }.takeIf { it.isNotEmpty() }
    }
    fun activeIndex(lines: List<LyricLine>, posMs: Long): Int { var lo = 0; var hi = lines.lastIndex; var ans = -1
        while (lo <= hi) { val mid = (lo + hi) ushr 1; if (lines[mid].timeMs <= posMs) { ans = mid; lo = mid + 1 } else hi = mid - 1 }; return ans }   // binary search
}
```
Import: `OpenDocument` with `audio/*`-free MIME types `["text/plain", "application/octet-stream"]` and extension check `.lrc`/`.txt`; read up to **256 KB** with `LimitedStream`, decode UTF-8 (strip BOM), reject binary (NUL bytes). `synced = LrcParser.parse(text) != null`. Metadata tags like `[ar:..]` are ignored by the regex because they do not match the time pattern.
### 6.3 UI
```kotlin
@Composable fun LyricsContent(lyrics: LyricsEntity?, posMs: Long, onImport: () -> Unit, modifier: Modifier = Modifier) {
    when {
        lyrics == null -> Column(modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) { Text("No lyrics for this song", color = Color.White.copy(0.8f)); Spacer(Modifier.height(12.dp)); FilledTonalButton(onImport) { Text("Import .lrc or .txt") } }
        lyrics.synced -> { val lines = remember(lyrics.trackId, lyrics.importedAtMs) { LrcParser.parse(lyrics.text).orEmpty() }; val active = LrcParser.activeIndex(lines, posMs); val st = rememberLazyListState()
            LaunchedEffect(active) { if (active >= 0) st.animateScrollToItem((active - 2).coerceAtLeast(0)) }
            LazyColumn(modifier, state = st, contentPadding = PaddingValues(vertical = 80.dp)) { itemsIndexed(lines) { i, l -> Text(l.text.ifBlank { "♪" }, Modifier.fillMaxWidth().padding(horizontal = 28.dp, vertical = 8.dp), textAlign = TextAlign.Center, color = if (i == active) Color.White else Color.White.copy(0.45f), fontSize = if (i == active) 22.sp else 18.sp, fontWeight = if (i == active) FontWeight.Bold else FontWeight.Normal) } } }
        else -> LazyColumn(modifier, contentPadding = PaddingValues(24.dp)) { item { Text(lyrics.text, color = Color.White.copy(0.9f), lineHeight = 26.sp, textAlign = TextAlign.Center) } }
    }
}
```
A **Lyrics** toggle (a `FilterChip` row under the top bar: "Artwork | Lyrics") swaps the foreground artwork for `LyricsContent`; the choice is remembered in memory for the session only (not persisted). Lyrics keyed by the track `id` (identity model, file 14 §2) survive rescans. Deleting lyrics is in the "More" sheet. Synced lyrics are matched to the position flow; seeking updates the highlight immediately.

## 7. Assembly
```kotlin
@Composable fun ImmersivePlayerLayout(s: PlaybackUiState, vm: MainViewModel, onBack: () -> Unit) {
    val reduced = rememberReducedMotion()                                  // see §9
    val cfg = LocalConfiguration.current; val wide = cfg.screenWidthDp >= 600; val short = cfg.screenHeightDp < 640
    var showLyrics by rememberSaveable { mutableStateOf(false) }; var queueOpen by rememberSaveable { mutableStateOf(false) }
    val queue by vm.queueItems.collectAsStateWithLifecycle(); val fav by vm.isFavorite(s.mediaId).collectAsStateWithLifecycle(false)
    val lyrics by vm.lyrics(s.mediaId).collectAsStateWithLifecycle(null); val canSeek = s.canSeek && s.durationMs > 0
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { u -> u?.let { vm.importLyrics(s.mediaId, it) } }
    Box(Modifier.fillMaxSize()) {
        ArtworkBackground(s.artRef, PlayerThemes.first { it.id == "immersive" }.colors, modifier = Modifier.fillMaxSize())
        if (wide) Row(Modifier.fillMaxSize()) {
            Column(Modifier.weight(0.55f).fillMaxHeight()) { PlayerTopBar(onBack, vm::openTrackOptions); Foreground(s, showLyrics, lyrics, vm, importLauncher, Modifier.weight(1f)); Controls(s, vm, canSeek, fav, reduced) }
            QueuePanel(queue, s.index, s.isPlaying, true, {}, vm::playAt, vm::removeAt, Modifier.weight(0.45f).fillMaxHeight())
        } else Column(Modifier.fillMaxSize().padding(bottom = if (queueOpen) 0.dp else 160.dp)) {
            PlayerTopBar(onBack, vm::openTrackOptions)
            if (!short) Foreground(s, showLyrics, lyrics, vm, importLauncher, Modifier.weight(1f).padding(vertical = 8.dp)) else Spacer(Modifier.weight(1f))
            Controls(s, vm, canSeek, fav, reduced)
        }
        if (!wide) QueuePanel(queue, s.index, s.isPlaying, queueOpen, { queueOpen = !queueOpen }, vm::playAt, vm::removeAt,
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().then(if (queueOpen) Modifier.fillMaxHeight(0.7f) else Modifier.height(160.dp)).navigationBarsPadding())
        s.error?.let { ErrorCard(it, onRetry = vm::retry, onSkip = vm::skip, Modifier.align(Alignment.Center)) }          // file 13 error card
    }
}
```
`Controls(...)` composes `SongInformation`, `PlaybackSeekBar`, `PrimaryPlaybackControls`, `SecondaryPlayerActions` (wire `onShuffle = vm::toggleShuffle`, `onRepeat = vm::cycleRepeat`, etc.). `Foreground` shows rounded artwork (64 % width, `aspectRatio(1f)`, 20 dp radius, shadow) or the lyrics panel with the Artwork/Lyrics chips. `vm.queueItems` is a `StateFlow<List<QueueItem>>` derived from the `QueueModel`; `vm.playAt(i)` seeks the controller to item `i`.
Back handling: `BackHandler(enabled = queueOpen) { queueOpen = false }` first, then the normal collapse.

## 8. Theme preview card
In `ThemePreviewCard`, branch on `PlayerLayout.IMMERSIVE`: dark gradient base with a blurred multi-colour circle (three overlapping `drawCircle` with `BlurMaskFilter`-free soft alpha rings), a rounded square "artwork" in the upper middle, a white title bar with two small orange dots at its ends (shuffle/repeat), a thin progress line, a row of five dots (controls), and a rounded bottom panel strip with one orange row. Uses `theme.colors`; no image resources, no network.

## 9. Accessibility, reduced motion, errors
```kotlin
@Composable fun rememberReducedMotion(): Boolean { val ctx = LocalContext.current
    return remember { Settings.Global.getFloat(ctx.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f } }
```
- Reduced motion (animator scale 0): no marquee, no background crossfade, queue height snaps, `EqualizerBars` becomes a static icon.
- Every icon has a `contentDescription`; toggles expose state in the description; the current queue row exposes "Now playing"; seek bar exposes position text; minimum touch target 48 dp.
- Text scale up to 200 %: title uses `maxLines = 1`; secondary rows wrap their icons only by shrinking spacing, never clipping; the screen scrolls only inside the queue/lyrics, never as a whole.
- Contrast: white on the dark scrim ≥ 4.5:1; orange (`Pulse.Orange`) is used for the current/active state **together with** a second cue (bold text / icon shape), never as the only indicator.
- Empty/error states: no artwork → fallback art + gradient; no title → "Unknown title"; no queue → "The queue is empty"; playback error → `ErrorCard` with **Retry** and **Skip** (file 13); lyrics missing → import prompt; lyrics file invalid → snackbar "Couldn't read that file".
- Performance: only `PlaybackSeekBar` collects the position flow; the blur is cached; the queue list uses stable keys; no allocation in draw scopes.

## 10. Acceptance
- Selecting **Immersive Artwork Player** changes only the Now Playing look; switching back to any other theme keeps the same queue, position and playing state with no restart.
- All nine themes are selectable; the default is still Classic; an unknown stored id shows Classic.
- Background follows the current artwork within ~0.5 s, never blocks scrolling; with no artwork the gradient is shown.
- Long titles scroll (unless reduced motion); the current queue row is orange and the equalizer bars animate only while playing.
- Seek bar is disabled and shows `--:--` for unknown duration; ±10 s respect `canSeek`.
- Synced lyrics highlight the correct line after seeking; a plain `.txt` shows as static text; a binary file is rejected.
- Tablet layout shows a permanent queue panel; a short phone hides the foreground artwork and still fits every control.
