# 05 — UI Code: Theme, App Shell, Components, Home, Songs, Now Playing

> Blueprint code. Keep imports correct for the installed Compose BOM. All UI reads state from ViewModels/`PlayerController`; composables never touch the DB directly.

## 1. Theme
```kotlin
package com.pulse.player.ui.theme

enum class ThemeMode { SYSTEM, DARK, AMOLED, LIGHT }
data class Skin(val id: String, val name: String, val colors: List<Color>)
val Skins = listOf(
    Skin("plum", "Plum", listOf(Pulse.PlumTop, Pulse.PlumBottom, Color.Black)),
    Skin("aurora", "Aurora", listOf(Color(0xFF3A1C71), Color(0xFF1B1B4B), Color.Black)),
    Skin("afterglow", "Afterglow", listOf(Color(0xFFFF2A6D), Color(0xFF5B1A58), Color(0xFF0A0612))),
    Skin("ocean", "Ocean", listOf(Color(0xFF0F4C75), Color(0xFF051937), Color.Black)),
    Skin("sunrise", "Sunrise", listOf(Color(0xFFFF8C78), Color(0xFFC2377A), Color(0xFF2B1055))),
    Skin("forest", "Forest", listOf(Color(0xFF0B3D2E), Color(0xFF03140F), Color.Black)),
    Skin("mono", "Mono", listOf(Color(0xFF2B2B2B), Color.Black)),
    Skin("amoled", "Pure black", listOf(Color.Black, Color.Black)))

private val DarkScheme = darkColorScheme(primary = Color.White, onPrimary = Color.Black, secondary = Pulse.Cyan,
    background = Pulse.Bg, surface = Pulse.Surface, surfaceVariant = Pulse.Elevated, onSurface = Color.White,
    onSurfaceVariant = Pulse.TextMid, outline = Pulse.Outline, error = Pulse.Error)
private val LightScheme = lightColorScheme(primary = Color(0xFF1A1625), onPrimary = Color.White, secondary = Color(0xFF0077A8),
    background = Color(0xFFFAF7FF), surface = Color.White, surfaceVariant = Color(0xFFF0EBFA), onSurface = Color(0xFF1A1625),
    onSurfaceVariant = Color(0xFF5E5870), outline = Color(0xFFD9D2EA), error = Color(0xFFD32F4F))

val LocalReducedMotion = staticCompositionLocalOf { false }
val LocalSkin = staticCompositionLocalOf { Skins[0] }
val LocalAccent = staticCompositionLocalOf { Pulse.Cyan }

@Composable fun PulseTheme(mode: ThemeMode, skin: Skin, reducedMotion: Boolean, accent: Color, content: @Composable () -> Unit) {
    val dark = when (mode) { ThemeMode.SYSTEM -> isSystemInDarkTheme(); ThemeMode.LIGHT -> false; else -> true }
    val scheme = if (dark) DarkScheme else LightScheme
    CompositionLocalProvider(LocalReducedMotion provides reducedMotion, LocalSkin provides skin, LocalAccent provides accent) {
        MaterialTheme(colorScheme = scheme, typography = PulseTypography, shapes = Shapes(
            small = RoundedCornerShape(10.dp), medium = RoundedCornerShape(16.dp), large = RoundedCornerShape(24.dp)), content = content)
    }
}
val PulseTypography = Typography(
    headlineMedium = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.Bold),
    titleLarge = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold),
    bodyMedium = TextStyle(fontSize = 14.sp), bodySmall = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Medium))

/** Screen background: skin gradient (or flat black in AMOLED/Light). */
@Composable fun AppBackground(mode: ThemeMode, modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    val skin = LocalSkin.current
    val bg = when {
        mode == ThemeMode.AMOLED -> Brush.verticalGradient(listOf(Color.Black, Color.Black))
        mode == ThemeMode.LIGHT || (mode == ThemeMode.SYSTEM && !isSystemInDarkTheme()) -> Brush.verticalGradient(listOf(Color(0xFFFAF7FF), Color(0xFFF0EBFA)))
        else -> Brush.verticalGradient(skin.colors) }
    Box(modifier.fillMaxSize().background(bg), content = content)
}
```

## 2. Reusable components

### Artwork (real artwork or a designed fallback)
```kotlin
@Composable fun Artwork(uri: String?, size: Dp, modifier: Modifier = Modifier, radius: Dp = 12.dp, px: Int = 256) {
    val ctx = LocalContext.current
    val bmp by produceState<Bitmap?>(null, uri) { value = uri?.let { ArtworkLoader.load(ctx, it, px) } }
    Box(modifier.size(size).clip(RoundedCornerShape(radius)), contentAlignment = Alignment.Center) {
        Crossfade(bmp, label = "art") { b ->
            if (b != null) Image(b.asImageBitmap(), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            else Box(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(Pulse.Violet, Pulse.Magenta))),
                contentAlignment = Alignment.Center) { Icon(Icons.Rounded.MusicNote, null, tint = Color.White.copy(0.9f), modifier = Modifier.size(size * 0.45f)) }
        }
    }
}
```

### Animated heart
```kotlin
@Composable fun HeartButton(on: Boolean, onToggle: () -> Unit, modifier: Modifier = Modifier) {
    val scale = remember { Animatable(1f) }
    LaunchedEffect(on) { if (on) { scale.animateTo(1.35f, spring(Spring.DampingRatioMediumBouncy)); scale.animateTo(1f, spring()) } }
    IconButton(onClick = onToggle, modifier = modifier.size(48.dp)) {
        Icon(if (on) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
            contentDescription = if (on) "Remove from favourites" else "Add to favourites",
            tint = if (on) Pulse.Cyan else Pulse.TextMid, modifier = Modifier.scale(scale.value))
    }
}
```

### Playing bars
```kotlin
@Composable fun PlayingBars(color: Color = Pulse.Cyan, active: Boolean = true) {
    val t = rememberInfiniteTransition(label = "bars")
    Row(Modifier.size(20.dp), horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.Bottom) {
        listOf(500, 650, 800).forEach { d ->
            val h by if (active && !LocalReducedMotion.current) t.animateFloat(0.25f, 1f, infiniteRepeatable(tween(d), RepeatMode.Reverse), label = "b") else remember { mutableStateOf(0.6f) }
            Box(Modifier.width(4.dp).fillMaxHeight(h).clip(RoundedCornerShape(2.dp)).background(color))
        }
    }
}
```

### Track row
```kotlin
@Composable fun TrackRow(t: Track, current: Boolean, playing: Boolean, selected: Boolean = false, selectMode: Boolean = false,
                         onClick: () -> Unit, onLongClick: () -> Unit, onMore: () -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().combinedClickable(onClick = onClick, onLongClick = onLongClick)
        .background(if (selected) Color.White.copy(0.08f) else Color.Transparent)
        .padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Box { Artwork(t.uri, 52.dp, radius = 10.dp, px = 128)
            if (current) Box(Modifier.matchParentSize().background(Color.Black.copy(0.45f), RoundedCornerShape(10.dp)), Alignment.Center) { PlayingBars(active = playing) } }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(t.title, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleMedium,
                color = if (current) Pulse.Cyan else MaterialTheme.colorScheme.onSurface)
            Text("${t.artist} · ${t.album}", maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (selectMode) Checkbox(selected, onCheckedChange = null)
        else {
            Text(formatDate(t.dateAddedMs), style = MaterialTheme.typography.bodySmall, color = Pulse.TextLow)
            IconButton(onClick = onMore) { Icon(Icons.Rounded.MoreVert, "More actions for ${t.title}") }
        }
    }
}
fun formatDate(ms: Long) = SimpleDateFormat("MM-dd", Locale.getDefault()).format(Date(ms))
fun formatTime(ms: Long): String { val s = (ms / 1000).toInt(); return "%d:%02d".format(s / 60, s % 60) }
```

### Pill buttons (Shuffle / Play)
```kotlin
@Composable fun PillButton(text: String, icon: ImageVector, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(onClick, modifier.height(48.dp), shape = CircleShape,
        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black)) {
        Icon(icon, null, Modifier.size(20.dp)); Spacer(Modifier.width(8.dp)); Text(text, fontWeight = FontWeight.Bold) }
}
```

### Mini player (floating glass, spinning disc, marquee)
```kotlin
@Composable fun MiniPlayer(s: PlaybackUiState, pos: StateFlow<Long>, onToggle: () -> Unit, onQueue: () -> Unit, onExpand: () -> Unit, modifier: Modifier = Modifier) {
    val rot = remember { Animatable(0f) }; val reduced = LocalReducedMotion.current
    LaunchedEffect(s.isPlaying, reduced) { if (s.isPlaying && !reduced) while (true) rot.animateTo(rot.value + 360f, tween(8000, easing = LinearEasing)) }
    val position by pos.collectAsStateWithLifecycle()
    Surface(modifier.padding(horizontal = 12.dp).fillMaxWidth().height(64.dp).clickable(onClick = onExpand), shape = RoundedCornerShape(32.dp),
        color = Color(0xCC1B1829), tonalElevation = 6.dp) {
        Box { Row(Modifier.padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Artwork(s.artworkUri, 48.dp, Modifier.graphicsLayer { rotationZ = rot.value }, radius = 24.dp, px = 128)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(s.title, maxLines = 1, modifier = Modifier.basicMarquee(), fontWeight = FontWeight.Bold, color = Color.White)
                Text(s.artist, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall, color = Pulse.TextMid) }
            IconButton(onToggle) { AnimatedContent(s.isPlaying, label = "pp") { p -> Icon(if (p) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, if (p) "Pause" else "Play", tint = Color.White, modifier = Modifier.size(32.dp)) } }
            IconButton(onQueue) { Icon(Icons.Rounded.QueueMusic, "Queue", tint = Color.White) }
        }
        LinearProgressIndicator(progress = { if (s.durationMs > 0) position / s.durationMs.toFloat() else 0f },
            Modifier.align(Alignment.BottomCenter).padding(horizontal = 28.dp).fillMaxWidth().height(2.dp), color = Pulse.Cyan, trackColor = Color.Transparent) }
    }
}
```
Artwork `uri` for the player uses the **track URI** (loadThumbnail reads art from the audio file/MediaStore); in `PlaybackUiState.artworkUri` that is the media item URI as set in `sync()`.

## 3. App shell (phone + tablet)
```kotlin
sealed interface Screen { 
    data object Home : Screen; data object Settings : Screen; data object Equalizer : Screen; data object Recap : Screen
    data object SkinTheme : Screen; data object PlayerTheme : Screen; data object Backup : Screen; data object Search : Screen
    data class Detail(val kind: String, val key: String) : Screen          // album / artist / folder / playlist / system
}

@Composable fun AppShell(vm: MainViewModel, windowSize: WindowSizeClass) {
    val expandedPlayer by vm.nowPlayingOpen.collectAsStateWithLifecycle()
    val screen by vm.screen.collectAsStateWithLifecycle()
    val ps by vm.player.state.collectAsStateWithLifecycle()
    val wide = windowSize.widthSizeClass != WindowWidthSizeClass.Compact
    val dock = windowSize.widthSizeClass == WindowWidthSizeClass.Expanded
    val drawer = rememberDrawerState(DrawerValue.Closed); val scope = rememberCoroutineScope()

    BackHandler(expandedPlayer) { vm.closeNowPlaying() }
    BackHandler(!expandedPlayer && screen !is Screen.Home) { vm.back() }

    AppBackground(vm.themeMode.collectAsStateWithLifecycle().value) {
        Row(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
            if (wide) PulseRail(screen, vm::go)
            Box(Modifier.weight(1f)) {
                ModalNavigationDrawer(drawerContent = { PulseDrawer(vm::go) { scope.launch { drawer.close() } } }, drawerState = drawer, gesturesEnabled = !wide) {
                    AnimatedContent(screen, transitionSpec = {
                        (fadeIn(tween(240)) + slideInHorizontally(tween(240)) { it / 8 }) togetherWith fadeOut(tween(160)) }, label = "screen") { s ->
                        when (s) {
                            Screen.Home -> HomeTabsScreen(vm, onMenu = { scope.launch { drawer.open() } }, showMenu = !wide)
                            Screen.Settings -> SettingsScreen(vm); Screen.Equalizer -> EqualizerScreen(vm); Screen.Recap -> RecapScreen(vm)
                            Screen.SkinTheme -> SkinThemeScreen(vm); Screen.PlayerTheme -> PlayerThemeScreen(vm)
                            Screen.Backup -> BackupScreen(vm); Screen.Search -> SearchScreen(vm); is Screen.Detail -> DetailScreen(vm, s)
                        } }
                }
                if (ps.mediaId != null) MiniPlayer(ps, vm.player.position, vm.player::togglePlay, { vm.openQueue() }, { vm.openNowPlaying() },
                    Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 8.dp))
            }
            if (dock && ps.mediaId != null) NowPlayingPanel(vm, Modifier.width(380.dp).fillMaxHeight())   // docked on tablets
        }
        AnimatedVisibility(expandedPlayer && !dock, enter = slideInVertically(tween(300)) { it } + fadeIn(), exit = slideOutVertically(tween(260)) { it } + fadeOut()) { NowPlayingScreen(vm) }
    }
}
```
`MainViewModel` (AndroidViewModel) holds: `screen`, `nowPlayingOpen`, `themeMode`, `tracks` (StateFlow<List<Track>> from `db.tracks().observeAll(minMs)` mapped with `toTrack()`), `favorites`, `playlists`, `selected ids`, search query, and wrappers like `play(list, index)`, `toggleFavorite(id)`, `scan()`. A `go(Screen)` push/pop is a tiny back stack (`ArrayDeque<Screen>`).

Window size: in `MainActivity.onCreate`: `enableEdgeToEdge(); setContent { val ws = calculateWindowSizeClass(this); PulseTheme(...) { AppShell(vm, ws) } }`.

## 4. Home with top tabs
```kotlin
val tabs = listOf("For you", "Songs", "Playlists", "Folders", "Albums", "Artists")

@Composable fun HomeTabsScreen(vm: MainViewModel, onMenu: () -> Unit, showMenu: Boolean) {
    val pager = rememberPagerState { tabs.size }; val scope = rememberCoroutineScope()
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            if (showMenu) IconButton(onMenu) { Icon(Icons.Rounded.Menu, "Open menu") }
            ScrollableTabRow(pager.currentPage, Modifier.weight(1f), containerColor = Color.Transparent, edgePadding = 8.dp, divider = {},
                indicator = { pos -> TabRowDefaults.SecondaryIndicator(Modifier.tabIndicatorOffset(pos[pager.currentPage]), color = Color.White) }) {
                tabs.forEachIndexed { i, t -> Tab(pager.currentPage == i, { scope.launch { pager.animateScrollToPage(i) } },
                    text = { Text(t, fontWeight = if (pager.currentPage == i) FontWeight.Bold else FontWeight.Medium, color = if (pager.currentPage == i) Color.White else Pulse.TextMid) }) } }
            IconButton({ vm.go(Screen.SkinTheme) }) { Icon(Icons.Rounded.AutoAwesome, "Skin theme") }
            IconButton({ vm.go(Screen.Search) }) { Icon(Icons.Rounded.Search, "Search") }
            IconButton({ vm.go(Screen.Settings) }) { Icon(Icons.Rounded.Settings, "Settings") }
        }
        HorizontalPager(pager, Modifier.weight(1f), beyondViewportPageCount = 1, key = { tabs[it] }) { page ->
            when (page) { 0 -> ForYouScreen(vm); 1 -> SongsScreen(vm); 2 -> PlaylistsScreen(vm); 3 -> FoldersScreen(vm); 4 -> AlbumsScreen(vm); else -> ArtistsScreen(vm) } }
    }
}
```

## 5. For you screen (widget tiles)
```kotlin
@Composable fun ForYouScreen(vm: MainViewModel) {
    val tracks by vm.tracks.collectAsStateWithLifecycle(); val ps by vm.player.state.collectAsStateWithLifecycle()
    val today by vm.todayMinutes.collectAsStateWithLifecycle(); val favCount by vm.favoriteCount.collectAsStateWithLifecycle()
    val recent = remember(tracks) { tracks.sortedByDescending { it.dateAddedMs }.take(12) }
    val top by vm.topPicks.collectAsStateWithLifecycle()
    if (tracks.isEmpty()) { EmptyLibrary(vm); return }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 120.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
        vm.resume.value?.let { r -> item { ResumeBanner(r) { vm.playQueue(listOf(r), 0) } } }
        item { Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            PillButton("Shuffle", Icons.Rounded.Shuffle, { vm.playQueue(tracks, Random.nextInt(tracks.size), shuffle = true) }, Modifier.weight(1f))
            PillButton("Play", Icons.Rounded.PlayArrow, { vm.playQueue(tracks, 0) }, Modifier.weight(1f)) } }
        item { Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                WidgetTile("Today", "$today min", Icons.Rounded.Headphones, listOf(Pulse.Violet, Pulse.Magenta), Modifier.weight(1f)) { vm.go(Screen.Recap) }
                WidgetTile("Favourites", "$favCount songs", Icons.Rounded.Favorite, listOf(Pulse.Cyan, Pulse.Violet), Modifier.weight(1f)) { vm.go(Screen.Detail("system", "favorites")) } }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                WidgetTile("Sleep timer", if (ps.sleepRemainingMs > 0) formatTime(ps.sleepRemainingMs) else "Off", Icons.Rounded.Bedtime, listOf(Pulse.Coral, Pulse.Pink), Modifier.weight(1f)) { vm.openSleepSheet() }
                WidgetTile("Equalizer", "Tune sound", Icons.Rounded.Equalizer, listOf(Pulse.Mint, Pulse.Cyan), Modifier.weight(1f)) { vm.go(Screen.Equalizer) } } } }
        item { SectionHeader("Recently added", badge = "New", action = "Play all") { vm.playQueue(recent, 0) } }
        item { LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) { items(recent, key = { it.id }) { t -> SquareCard(t, 132.dp) { vm.playQueue(recent, recent.indexOf(t)) } } } }
        if (top.isNotEmpty()) { item { SectionHeader("Top picks for you") }
            item { LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) { items(top, key = { it.id }) { t -> SquareCard(t, 168.dp) { vm.playQueue(top, top.indexOf(t)) } } } } }
    }
}

@Composable fun WidgetTile(title: String, value: String, icon: ImageVector, colors: List<Color>, modifier: Modifier, onClick: () -> Unit) {
    Box(modifier.height(96.dp).clip(RoundedCornerShape(20.dp)).background(Brush.linearGradient(colors.map { it.copy(alpha = 0.85f) })).clickable(onClick = onClick).padding(16.dp)) {
        Icon(icon, null, tint = Color.White.copy(0.9f), modifier = Modifier.align(Alignment.TopEnd).size(24.dp))
        Column(Modifier.align(Alignment.BottomStart)) { Text(title, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(0.85f)); Text(value, style = MaterialTheme.typography.titleMedium, color = Color.White) } }
}
@Composable fun SquareCard(t: Track, size: Dp, onClick: () -> Unit) {
    Column(Modifier.width(size).clickable(onClick = onClick)) { Artwork(t.uri, size, radius = 14.dp, px = 384)
        Spacer(Modifier.height(8.dp)); Text(t.title, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Bold, color = Color.White)
        Text(t.artist, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall, color = Pulse.TextMid) }
}
@Composable fun SectionHeader(text: String, badge: String? = null, action: String? = null, onAction: () -> Unit = {}) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(text, style = MaterialTheme.typography.titleLarge, color = Color.White)
        badge?.let { Spacer(Modifier.width(8.dp)); Box(Modifier.clip(CircleShape).background(Pulse.Magenta).padding(horizontal = 8.dp, vertical = 2.dp)) { Text(it, style = MaterialTheme.typography.bodySmall, color = Color.White) } }
        Spacer(Modifier.weight(1f)); action?.let { TextButton(onAction) { Text(it, color = Pulse.Cyan) } } }
}
```
`ResumeBanner`: rounded 20dp card, gradient, text "Continue {title}", inline circular play button. `EmptyLibrary`: icon + explanation + **Scan music** + **Choose folder** buttons that run the permission flow from file 03.

## 6. Songs screen
```kotlin
@Composable fun SongsScreen(vm: MainViewModel) {
    val tracks by vm.sortedTracks.collectAsStateWithLifecycle(); val ps by vm.player.state.collectAsStateWithLifecycle()
    val scanning by vm.scanning.collectAsStateWithLifecycle(); val selected by vm.selected.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    Box { LazyColumn(Modifier.fillMaxSize(), listState, contentPadding = PaddingValues(bottom = 120.dp)) {
        item { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("${tracks.size} Songs", style = MaterialTheme.typography.titleLarge, color = Color.White)
                if (scanning) { Spacer(Modifier.width(8.dp)); Text("(Scanning…)", color = Pulse.Cyan, style = MaterialTheme.typography.bodySmall) }
                Spacer(Modifier.weight(1f)); SortMenu(vm); IconButton({ vm.toggleSelectMode() }) { Icon(Icons.Rounded.Checklist, "Select songs") } }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                PillButton("Shuffle", Icons.Rounded.Shuffle, { vm.playQueue(tracks, Random.nextInt(tracks.size), true) }, Modifier.weight(1f))
                PillButton("Play", Icons.Rounded.PlayArrow, { vm.playQueue(tracks, 0) }, Modifier.weight(1f)) } } }
        itemsIndexed(tracks, key = { _, t -> t.id }) { i, t ->
            TrackRow(t, t.id == ps.mediaId, ps.isPlaying, t.id in selected, vm.selectMode.value,
                onClick = { if (vm.selectMode.value) vm.toggleSelect(t.id) else vm.playQueue(tracks, i) },
                onLongClick = { vm.toggleSelect(t.id) }, onMore = { vm.openTrackMenu(t) }, modifier = Modifier.animateItem()) } }
        AlphabetScroller(tracks.map { it.title }, listState, Modifier.align(Alignment.CenterEnd))   // P1
    }
}
```
`AlphabetScroller`: vertical column of letters A–Z + `#`, drag/tap maps to the first index whose title starts with that letter → `listState.scrollToItem(index + 1)`. Hide when fewer than 50 songs.

Track menu (`ModalBottomSheet`): Play next · Add to queue · Add to playlist · Favourite/Unfavourite · Song details (title, artist, album, duration, size, format, folder) · Set as ringtone / notification / alarm · Share file (`ACTION_SEND` with the content URI + `FLAG_GRANT_READ_URI_PERMISSION`).

## 7. Now Playing screen (default layout; themes in file 06 replace the middle area)
```kotlin
@Composable fun NowPlayingScreen(vm: MainViewModel) {
    val s by vm.player.state.collectAsStateWithLifecycle(); val ctx = LocalContext.current
    val fav by vm.isCurrentFavorite.collectAsStateWithLifecycle()
    var dominant by remember { mutableStateOf(0xFF2A0826.toInt()) }
    LaunchedEffect(s.mediaId) { s.artworkUri?.let { u -> ArtworkLoader.load(ctx, u, 128)?.let { dominant = ArtworkLoader.dominant(it) } } }
    val top by animateColorAsState(Color(dominant).darken(0.55f), tween(500), label = "bg")
    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(top, Color(0xFF0A0612), Color.Black)))) {
        Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).padding(horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(vm::closeNowPlaying) { Icon(Icons.Rounded.KeyboardArrowDown, "Collapse player", Modifier.size(32.dp)) }
                Spacer(Modifier.weight(1f)); SongLyricsToggle(vm); Spacer(Modifier.weight(1f))
                IconButton({ vm.openPlayerMenu() }) { Icon(Icons.Rounded.MoreVert, "More") } }
            Spacer(Modifier.height(16.dp))
            PlayerThemeHost(vm, s)                                              // file 06: artwork / vinyl / mesh etc.
            Spacer(Modifier.height(24.dp))
            Text(s.title, style = MaterialTheme.typography.headlineMedium, color = Color.White, maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
            Text(s.artist, style = MaterialTheme.typography.bodyMedium, color = Pulse.TextMid, maxLines = 1)
            Spacer(Modifier.height(16.dp))
            SeekBar(vm.player.position, s.durationMs, onSeek = vm.player::seekTo)
            Spacer(Modifier.height(8.dp))
            TransportRow(s, vm.player)
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                HeartButton(fav, { vm.toggleFavoriteCurrent() })
                IconButton({ vm.openSleepSheet() }) { Icon(Icons.Rounded.Bedtime, "Sleep timer", tint = if (s.sleepRemainingMs > 0 || s.sleepEndOfTrack) Pulse.Cyan else Color.White) }
                IconButton({ vm.openAddToPlaylist() }) { Icon(Icons.Rounded.PlaylistAdd, "Add to playlist") }
                IconButton({ vm.openQueue() }) { Icon(Icons.Rounded.QueueMusic, "Queue") }
                IconButton({ vm.go(Screen.Equalizer) }) { Icon(Icons.Rounded.Equalizer, "Equalizer") } }
            Spacer(Modifier.height(16.dp))
        }
    }
}
fun Color.darken(f: Float) = Color(red * (1 - f), green * (1 - f), blue * (1 - f), alpha)

@Composable fun SeekBar(position: StateFlow<Long>, duration: Long, onSeek: (Long) -> Unit) {
    val pos by position.collectAsStateWithLifecycle()           // only this composable recomposes ~4x/s
    var drag by remember { mutableStateOf<Float?>(null) }
    val value = drag ?: if (duration > 0) pos / duration.toFloat() else 0f
    Column { Slider(value.coerceIn(0f, 1f), { drag = it }, onValueChangeFinished = { drag?.let { onSeek((it * duration).toLong()) }; drag = null },
            colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = Color.White, inactiveTrackColor = Color.White.copy(0.25f)))
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp)) { Text(formatTime(((drag ?: value) * duration).toLong()), color = Pulse.TextMid, style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.weight(1f)); Text(formatTime(duration), color = Pulse.TextMid, style = MaterialTheme.typography.bodySmall) } }
}

@Composable fun TransportRow(s: PlaybackUiState, p: PlayerController) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        IconButton(p::toggleShuffle) { Icon(Icons.Rounded.Shuffle, "Shuffle ${if (s.shuffle) "on" else "off"}", tint = if (s.shuffle) Pulse.Cyan else Color.White) }
        IconButton(p::previous, Modifier.size(56.dp)) { Icon(Icons.Rounded.SkipPrevious, "Previous", Modifier.size(36.dp)) }
        FilledIconButton(p::togglePlay, Modifier.size(76.dp), colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color.White, contentColor = Color.Black)) {
            AnimatedContent(s.isPlaying, transitionSpec = { (scaleIn() + fadeIn()) togetherWith (scaleOut() + fadeOut()) }, label = "main") { pl ->
                Icon(if (pl) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, if (pl) "Pause" else "Play", Modifier.size(40.dp)) } }
        IconButton(p::next, Modifier.size(56.dp)) { Icon(Icons.Rounded.SkipNext, "Next", Modifier.size(36.dp)) }
        IconButton(p::cycleRepeat) { Icon(if (s.repeat == Player.REPEAT_MODE_ONE) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat, "Repeat mode",
            tint = if (s.repeat != Player.REPEAT_MODE_OFF) Pulse.Cyan else Color.White) }
    }
}
```
`NowPlayingPanel` (tablet dock) = same content without the collapse arrow. Provide a `QueueSheet`, `SleepSheet` (chips 15/30/45/60, custom slider, "End of this track", Cancel), and `AddToPlaylistSheet` as `ModalBottomSheet`s hosted in `AppShell` and driven by `vm` flags.

## 8. Other screens (build with the same patterns)
Playlists (system 2x2 grid + user list + create dialog), Folders (group by `folderPath`), Albums (`LazyVerticalGrid(GridCells.Adaptive(150.dp))`), Artists (list), DetailScreen (header + actions + list), SearchScreen (debounced), SettingsScreen (sections from file 01 §3), RecapScreen (file 08).

## 9. Acceptance for this part
All six tabs render real data; mini player appears and expands; heart persists; seek works; tablet width shows rail + docked player; reduced-motion switch stops disc/bars; no emoji anywhere; `brain.md` updated.
