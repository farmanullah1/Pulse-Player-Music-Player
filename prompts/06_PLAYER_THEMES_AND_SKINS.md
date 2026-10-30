# 06 — Player Themes (A–E carousel), Skin Theme, Wallpapers, Settings Store

All visuals are **drawn in code** (Canvas/gradients). No downloaded photos, no network, no copyright issues. "Special" badge = purely decorative label (no purchases).

## 1. Settings store (DataStore)
```kotlin
val Context.dataStore by preferencesDataStore("pulse_settings")
class SettingsStore(private val ctx: Context) {
    private object K {
        val theme = stringPreferencesKey("theme_mode"); val skin = stringPreferencesKey("skin_id"); val player = stringPreferencesKey("player_theme")
        val wallpaper = stringPreferencesKey("wallpaper_path"); val reduced = booleanPreferencesKey("reduced_motion")
        val dynamic = booleanPreferencesKey("dynamic_accent"); val minDur = intPreferencesKey("min_duration_s")
        val fade = booleanPreferencesKey("fade_play_pause"); val excluded = stringSetPreferencesKey("excluded_folders")
        val safTrees = stringSetPreferencesKey("saf_trees"); val queueIds = stringPreferencesKey("queue_ids")
        val queueIndex = intPreferencesKey("queue_index"); val queuePos = longPreferencesKey("queue_pos")
        val eq = stringPreferencesKey("eq_json")
    }
    val themeMode = ctx.dataStore.data.map { runCatching { ThemeMode.valueOf(it[K.theme] ?: "DARK") }.getOrDefault(ThemeMode.DARK) }
    val skinId = ctx.dataStore.data.map { it[K.skin] ?: "plum" }
    val playerThemeId = ctx.dataStore.data.map { it[K.player] ?: "classic" }
    val wallpaperPath = ctx.dataStore.data.map { it[K.wallpaper] }
    val reducedMotion = ctx.dataStore.data.map { it[K.reduced] ?: false }
    val dynamicAccent = ctx.dataStore.data.map { it[K.dynamic] ?: true }
    val minDurationSec = ctx.dataStore.data.map { it[K.minDur] ?: 30 }
    val fadeEnabled = ctx.dataStore.data.map { it[K.fade] ?: true }
    val excludedFolders = ctx.dataStore.data.map { it[K.excluded] ?: emptySet() }
    val safTrees = ctx.dataStore.data.map { it[K.safTrees] ?: emptySet() }
    suspend fun setTheme(m: ThemeMode) = ctx.dataStore.edit { it[K.theme] = m.name }
    suspend fun setSkin(id: String) = ctx.dataStore.edit { it[K.skin] = id; it.remove(K.wallpaper) }
    suspend fun setPlayerTheme(id: String) = ctx.dataStore.edit { it[K.player] = id }
    suspend fun setWallpaper(path: String?) = ctx.dataStore.edit { if (path == null) it.remove(K.wallpaper) else it[K.wallpaper] = path }
    suspend fun setReduced(v: Boolean) = ctx.dataStore.edit { it[K.reduced] = v }
    suspend fun setDynamic(v: Boolean) = ctx.dataStore.edit { it[K.dynamic] = v }
    suspend fun setMinDuration(s: Int) = ctx.dataStore.edit { it[K.minDur] = s }
    suspend fun setFade(v: Boolean) = ctx.dataStore.edit { it[K.fade] = v }
    suspend fun addSafTree(uri: String) = ctx.dataStore.edit { it[K.safTrees] = (it[K.safTrees] ?: emptySet()) + uri }
    suspend fun saveQueue(ids: List<String>, index: Int, pos: Long) = ctx.dataStore.edit { it[K.queueIds] = ids.joinToString("\n"); it[K.queueIndex] = index; it[K.queuePos] = pos }
    val queue = ctx.dataStore.data.map { Triple((it[K.queueIds] ?: "").split("\n").filter(String::isNotBlank), it[K.queueIndex] ?: 0, it[K.queuePos] ?: 0L) }
}
```

## 2. Player theme model
```kotlin
enum class PlayerLayout { CLASSIC, SHEET_QUEUE, MESH_GLASS, MINIMAL_DARK, VINYL, ACOUSTIC, COAST, STARRY, IMMERSIVE }
data class PlayerTheme(val id: String, val name: String, val layout: PlayerLayout, val colors: List<Color>, val special: Boolean = false)

val PlayerThemes = listOf(
    PlayerTheme("classic", "Classic", PlayerLayout.CLASSIC, listOf(Pulse.PlumTop, Pulse.PlumBottom)),
    PlayerTheme("mountain", "Mountain Horizon", PlayerLayout.SHEET_QUEUE, listOf(Color(0xFFFF8C5A), Color(0xFF6B2C5E), Color(0xFF1B1030)), special = true),   // Type A
    PlayerTheme("mesh", "Mesh Glass", PlayerLayout.MESH_GLASS, listOf(Pulse.Magenta, Color(0xFF051937))),                                                    // Type B
    PlayerTheme("minimal", "Minimal Mood", PlayerLayout.MINIMAL_DARK, listOf(Color(0xFF2A2A2E), Color.Black)),                                              // Type C
    PlayerTheme("acoustic", "Acoustic", PlayerLayout.ACOUSTIC, listOf(Color(0xFF6B3E26), Color(0xFF1E0F08)), special = true),                               // Type D
    PlayerTheme("vinyl", "Vinyl", PlayerLayout.VINYL, listOf(Color(0xFF3A2A7A), Color(0xFF0E0A24)), special = true),                                       // Type D
    PlayerTheme("coast", "Coastal Ocean", PlayerLayout.COAST, listOf(Color(0xFF0F4C75), Color(0xFF051937))),                                                // Type E
    PlayerTheme("starry", "Starry Sky", PlayerLayout.STARRY, listOf(Color(0xFF0A1030), Color(0xFF2A3350))),                                                // Type E
    PlayerTheme("immersive", "Immersive Artwork Player", PlayerLayout.IMMERSIVE, listOf(Color(0xFF1B1B22), Color.Black)))   // file 16
```
Layout summary (implement each as a composable `@Composable fun XLayout(s: PlaybackUiState, vm: MainViewModel, theme: PlayerTheme)` rendered inside `PlayerThemeHost`):
- **CLASSIC:** rounded-square artwork (≈ 70% width, glow shadow), details below (file 05).
- **A SHEET_QUEUE:** top 55% = sunset-mountain canvas backdrop (below), title/controls over it; bottom 45% = rounded white (`#FFFFFF`, 28dp top radius) sheet showing the real upcoming queue (`LazyColumn` of art, title, artist, tiny "HD" badge when `mimeType` is flac/wav).
- **B MESH_GLASS:** magenta→deep-blue mesh (two radial brushes over a vertical gradient), frosted glass artwork card, floating time pill ("1:16 / 3:54"), circular **-10s / +10s** buttons flanking the seek bar, secondary icon row centered.
- **C MINIMAL_DARK:** charcoal→black; artwork card left-aligned, title left, Equalizer and Queue icons right of the title, time pill centered, large white circular play/pause.
- **D VINYL:** frosted blue-purple; black vinyl disc (grooves via concentric circles) with artwork as center label, rotates while playing; tonearm drawn as a rotated rounded line that swings in (−25° → 0°) when playing. **ACOUSTIC:** warm brown gradient, circular artwork frame with wood-grain lines (Canvas sine strokes).
- **E COAST:** animated waves canvas + floating square art + dark frosted lower panel. **STARRY:** star-field canvas, lower slate frosted panel with a "Next song" card (the real next queue item; hidden if none). A thumbnail row of 4 background presets (gradient/waves/stars/mountain) at the bottom swaps the backdrop instantly and is saved.

### PlayerThemeHost
```kotlin
@Composable fun PlayerThemeHost(vm: MainViewModel, s: PlaybackUiState) {
    val id by vm.playerThemeId.collectAsStateWithLifecycle(); val theme = PlayerThemes.firstOrNull { it.id == id } ?: PlayerThemes[0]
    Crossfade(theme, animationSpec = tween(400), label = "ptheme") { t -> when (t.layout) {
        PlayerLayout.VINYL -> VinylDisc(s); PlayerLayout.ACOUSTIC -> AcousticDisc(s)
        PlayerLayout.COAST -> Box { WavesCanvas(Modifier.matchParentSize()); Artwork(s.artworkUri, 260.dp, radius = 16.dp, px = 512) }
        PlayerLayout.STARRY -> Box { StarField(Modifier.matchParentSize()); Artwork(s.artworkUri, 260.dp, radius = 16.dp, px = 512) }
        else -> Artwork(s.artworkUri, 300.dp, Modifier.shadow(24.dp, RoundedCornerShape(16.dp), spotColor = t.colors.first()), radius = 16.dp, px = 512)
    } }
}
```
(For full-bleed layouts A, C, E, B, `NowPlayingScreen` uses `theme.colors` as its background brush instead of the dominant-color gradient. The model should implement this by letting `PlayerThemeHost` also expose `theme.colors` through a `LocalPlayerTheme` provider.)

## 3. Procedural backgrounds
```kotlin
@Composable fun StarField(modifier: Modifier = Modifier) {
    val reduced = LocalReducedMotion.current
    val stars = remember { List(90) { Triple(Random.nextFloat(), Random.nextFloat(), Random.nextFloat() * 1.6f + 0.4f) } }
    val twinkle by rememberInfiniteTransition(label = "tw").animateFloat(0.4f, 1f, infiniteRepeatable(tween(2400), RepeatMode.Reverse), label = "t")
    Canvas(modifier) { stars.forEachIndexed { i, (x, y, r) ->
        drawCircle(Color.White.copy(alpha = if (reduced) 0.8f else if (i % 3 == 0) twinkle else 0.8f), r.dp.toPx(), Offset(x * size.width, y * size.height)) } }
}
@Composable fun WavesCanvas(modifier: Modifier = Modifier) {
    val reduced = LocalReducedMotion.current
    val phase by rememberInfiniteTransition(label = "w").animateFloat(0f, (2 * Math.PI).toFloat(), infiniteRepeatable(tween(6000, easing = LinearEasing)), label = "p")
    Canvas(modifier) { listOf(0.55f to 0.35f, 0.65f to 0.25f, 0.75f to 0.18f).forEachIndexed { i, (yy, a) ->
        val path = Path().apply { moveTo(0f, size.height); var x = 0f
            while (x <= size.width) { lineTo(x, size.height * yy + 18.dp.toPx() * sin(x / size.width * 6f + (if (reduced) 0f else phase) + i)); x += 8f }
            lineTo(size.width, size.height); close() }
        drawPath(path, Color(0xFF00C3FF).copy(alpha = a)) } }
}
@Composable fun MountainSunset(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        drawRect(Brush.verticalGradient(listOf(Color(0xFFFF8C5A), Color(0xFFC2377A), Color(0xFF2B1055))))
        drawCircle(Color(0xFFFFD27A), size.width * 0.12f, Offset(size.width * 0.7f, size.height * 0.45f))
        listOf(0.62f to Color(0xFF4B2A6B), 0.75f to Color(0xFF2B1B4A), 0.88f to Color(0xFF140C28)).forEach { (base, c) ->
            val p = Path().apply { moveTo(0f, size.height); lineTo(0f, size.height * base)
                var x = 0f; var up = true
                while (x < size.width) { x += size.width / 5; lineTo(x, size.height * (base + if (up) -0.08f else 0.02f)); up = !up }
                lineTo(size.width, size.height); close() }
            drawPath(p, c) } }
}
@Composable fun VinylDisc(s: PlaybackUiState) {
    val rot = remember { Animatable(0f) }; val reduced = LocalReducedMotion.current
    LaunchedEffect(s.isPlaying, reduced) { if (s.isPlaying && !reduced) while (true) rot.animateTo(rot.value + 360f, tween(6000, easing = LinearEasing)) }
    val arm by animateFloatAsState(if (s.isPlaying) 0f else -25f, spring(), label = "arm")
    Box(Modifier.size(300.dp), Alignment.Center) {
        Canvas(Modifier.size(280.dp).graphicsLayer { rotationZ = rot.value }) {
            drawCircle(Color(0xFF0B0B0F)); for (i in 1..9) drawCircle(Color.White.copy(0.05f), size.minDimension / 2 * (0.35f + i * 0.065f), style = Stroke(1.dp.toPx())) }
        Artwork(s.artworkUri, 110.dp, Modifier.graphicsLayer { rotationZ = rot.value }, radius = 55.dp, px = 256)
        Canvas(Modifier.matchParentSize().graphicsLayer { rotationZ = arm; transformOrigin = TransformOrigin(0.92f, 0.08f) }) {
            drawLine(Color(0xFFDDDDDD), Offset(size.width * 0.92f, size.height * 0.08f), Offset(size.width * 0.55f, size.height * 0.5f), 6.dp.toPx(), StrokeCap.Round) }
    }
}
```
`AcousticDisc`: circular artwork with a warm gradient ring and 12 Canvas wood-grain sine strokes; same rotation logic (slower, 12 s). Use similar code.

## 4. Player theme carousel screen
```kotlin
@Composable fun PlayerThemeScreen(vm: MainViewModel) {
    val used by vm.playerThemeId.collectAsStateWithLifecycle(); val pager = rememberPagerState(PlayerThemes.indexOfFirst { it.id == used }.coerceAtLeast(0)) { PlayerThemes.size }
    val cur = PlayerThemes[pager.currentPage]; val bgTop by animateColorAsState(cur.colors.first(), tween(500), label = "bg")
    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(bgTop.copy(0.9f), Color.Black)))) {
        Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
            Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) { IconButton(vm::back) { Icon(Icons.Rounded.ArrowBack, "Back") }
                Text("Player theme", style = MaterialTheme.typography.titleLarge, color = Color.White) }
            HorizontalPager(pager, Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 56.dp), pageSpacing = 16.dp) { page ->
                val offset = (pager.currentPage - page) + pager.currentPageOffsetFraction
                val scale = lerp(0.85f, 1.05f, 1f - offset.absoluteValue.coerceIn(0f, 1f))
                ThemePreviewCard(PlayerThemes[page], Modifier.graphicsLayer { scaleX = scale; scaleY = scale; alpha = lerp(0.6f, 1f, 1f - offset.absoluteValue.coerceIn(0f, 1f)) }) }
            val isUsing = cur.id == used
            Button({ vm.applyPlayerTheme(cur.id) }, Modifier.align(Alignment.CenterHorizontally).padding(24.dp).height(52.dp).widthIn(min = 200.dp), shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = if (isUsing) Color.White.copy(0.18f) else Pulse.Orange, contentColor = Color.White)) {
                if (isUsing) { Icon(Icons.Rounded.Check, null); Spacer(Modifier.width(8.dp)); Text("USING", fontWeight = FontWeight.Bold) }
                else { if (cur.special) { Icon(Icons.Rounded.WorkspacePremium, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)) }; Text("APPLY", fontWeight = FontWeight.Bold) } }
        }
    }
}
```
`ThemePreviewCard`: 9:16-ish rounded card (28dp) drawing a miniature of that layout using the same background (gradient/Canvas) + a small fake-free preview: gradient art square (icon only), a title bar placeholder shape, and 5 control dots. Spring-based swipe is provided by the pager (`flingBehavior = PagerDefaults.flingBehavior(pager, snapAnimationSpec = spring())`).

## 5. Skin theme screen
- Header: back arrow + "Skin theme".
- **Colors** grid (`LazyVerticalGrid(GridCells.Adaptive(64.dp))`): circular swatches from `Skins` (Brush.linearGradient), active one has a white ring + `Check` icon. Tap → `settings.setSkin(id)`.
- **Wallpapers** tabs: All · Gradients · Nature · Night · Minimal. Wallpapers are procedural presets (`MountainSunset`, `WavesCanvas` over ocean gradient, `StarField` over night gradient, mesh gradients) shown as 3:4 thumbnails; tap to apply (`settings.setWallpaper("preset:starry")`).
- **Custom image** card with `+`: 
```kotlin
val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
    if (uri != null) scope.launch(Dispatchers.IO) {
        val out = File(ctx.filesDir, "wallpaper.jpg")
        ctx.contentResolver.openInputStream(uri)?.use { i -> out.outputStream().use { o -> i.copyTo(o) } }   // copy into private storage
        app.settings.setWallpaper(out.absolutePath) } }
// launch: picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
```
Downscale the copy to max 1600 px (`BitmapFactory.Options.inSampleSize`) before saving to keep memory low. `AppBackground` draws the wallpaper (image with a 45% black scrim, or the procedural preset) behind content when set.

## 6. Dynamic accent
If `dynamicAccent` is on, `LocalAccent` = vibrant color from the current artwork (Palette `getVibrantColor`) with min luminance fix; used for slider highlight and selection. Fallback `Pulse.Cyan`. Respect contrast.

## 7. Acceptance for this part
Nine themes selectable (the ninth, "immersive", comes from file 16 §2); carousel scales center card; APPLY → USING works and persists after restart; Vinyl rotates only while playing; reduced motion stops all infinite animations; custom wallpaper survives restart; `brain.md` updated.
