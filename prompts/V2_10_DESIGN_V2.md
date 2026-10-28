# V2-10 — Design System V2 (more colour, smoother motion, tuned for the Infinix 20:9 AMOLED)

> This file OVERRIDES visual rules in `02_DESIGN_SYSTEM.md` where they differ. Everything not mentioned stays as in 02. Read only this file for this step. Output complete files.

## 1. Design direction: "Neon Aurora on true black"
- Base is `#000000` (AMOLED saves power, makes colour pop). Colour lives in **three places only**: (1) a soft animated aurora behind the Now Playing screen, (2) gradient accents on primary actions and tiles, (3) artwork-derived tint. Never put gradients behind every row.
- Every screen has ONE hero colour (taken from the current artwork when "Dynamic accent" is on, else the skin colour). Secondary accents are fixed.
- White text on dark always. Contrast >= 4.5:1. Never use colour alone for state (icon + label too).

## 2. Colour tokens V2 (replace the `Pulse` object colours)
```kotlin
object Pulse {
    val Black = Color(0xFF000000); val Bg = Color(0xFF05040A); val Surface = Color(0xFF110F1C); val Elevated = Color(0xFF1A1729); val Outline = Color(0xFF2E2A42)
    val TextHi = Color.White; val TextMid = Color(0xFFB4B0C8); val TextLow = Color(0xFF77738C)      // TextMid lifted from A0A0A0 for AA contrast on Surface
    val Cyan = Color(0xFF00C3FF); val Violet = Color(0xFF9B7BFF); val Magenta = Color(0xFFFF2A6D); val Pink = Color(0xFFFF6DAA)
    val Coral = Color(0xFFFF8C78); val Mint = Color(0xFF3DDC97); val Orange = Color(0xFFFF9500); val Purple = Color(0xFFB36BFF)
    val Lime = Color(0xFFB6F24A); val Sun = Color(0xFFFFD166); val Error = Color(0xFFFF647C)
    val PlumTop = Color(0xFF2A0826); val PlumBottom = Color(0xFF120310)
    // signature gradients (use via Brush.linearGradient)
    val GradPrimary = listOf(Violet, Magenta)           // Play, APPLY alternative
    val GradCool    = listOf(Cyan, Violet)              // Favourites, links
    val GradWarm    = listOf(Orange, Magenta)           // Top tracks, flame
    val GradFresh   = listOf(Mint, Cyan)                // Recently added
    val GradSunset  = listOf(Coral, Pink, Purple)
}
```
Tile colours (For you widgets and Playlists grid): Today = `GradFresh`, Favourites = `GradCool` (heart stays `#00C3FF` when on), Sleep = `listOf(Purple, Violet)`, Equalizer = `GradWarm`. System playlists: favourite = blue heart, recently added = green note, recently played = purple pie, top tracks = orange flame (unchanged from the original).

## 3. Shape, spacing, type (tightened)
- Grid 4dp. Screen padding 16dp (20dp at >=400dp width, 32dp on tablets). Section gap 24dp. Row height 64dp, touch target >= 48dp.
- Radii: artwork 12dp (list 10dp), cards 20dp, sheets 28dp, primary buttons full pill.
- Safe area for the **centred punch-hole**: top 24dp of the status area holds no controls; top bar content starts below `WindowInsets.statusBars`. Rounded corners: elements touching the screen edge get +4dp inner padding.
- Type: Roboto/system. Title 28sp Bold, section 20sp SemiBold, track 16sp Bold, meta 13sp. Numbers (time, counts) use `fontFeatureSettings = "tnum"` so digits do not jiggle.

## 4. Motion V2 (all durations in ms; all respect Reduced motion)
| What | Spec |
|---|---|
| Press feedback | `Modifier.pressScale()` (below): scale to 0.96 on press, spring back (dampingRatio 0.55, stiffness Medium) + ripple |
| Heart | scale 1 -> 1.4 -> 1 spring, colour tween 200, 6 tiny particles optional (skip when reduced) |
| Play/Pause | `AnimatedContent` icon morph, 160 |
| Mini -> Full player | container transform feel: slide up 320 `FastOutSlowIn` + artwork scales from 56dp to 300dp via shared `animateDpAsState`; backdrop colour tween 450 |
| Screen change | `fadeIn(220) + slideInHorizontally { it / 10 }` and exit `fadeOut(160)` |
| Tab indicator | offset spring, width tween |
| List first load | stagger 40ms, max 12 items, ONLY once per screen per app start |
| Now Playing backdrop | aurora blobs drift (below), cross-fade 500 on track change |
| Album art on track change | crossfade 300 + scale 0.96 -> 1 |
| Seek bar | thumb grows 12 -> 18dp while dragging, time pill follows thumb |
| Equalizer bars (playing) | 3-4 bars `infiniteTransition`, only on the current row, only while playing |
| Skeleton loading | shimmer 1200ms linear, only when data is actually loading |
Never animate during scroll; never run more than one infinite animation off-screen.

## 5. Code: reusable pieces
### 5.1 Press scale
```kotlin
fun Modifier.pressScale(enabled: Boolean = true): Modifier = composed {
    val reduced = LocalReducedMotion.current
    val src = remember { MutableInteractionSource() }; val pressed by src.collectIsPressedAsState()
    val s by animateFloatAsState(if (pressed && enabled && !reduced) 0.96f else 1f, spring(0.55f, Spring.StiffnessMedium), label = "press")
    this.interactionSource(src).graphicsLayer { scaleX = s; scaleY = s }
}
```
(`interactionSource` is a small helper: `fun Modifier.interactionSource(s: MutableInteractionSource) = this.then(Modifier.indication(s, ripple()))`. Use `clickable(interactionSource = src, indication = ripple())` directly on buttons when simpler.)

### 5.2 Aurora background (cheap: 3 radial gradients, one animation clock)
```kotlin
@Composable fun AuroraBackground(accent: Color, modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    val reduced = LocalReducedMotion.current
    val t by if (reduced) remember { mutableStateOf(0f) } else rememberInfiniteTransition(label = "aurora").animateFloat(0f, 1f, infiniteRepeatable(tween(14000, easing = LinearEasing), RepeatMode.Reverse), label = "t")
    val a1 by animateColorAsState(accent.darkened(0.55f), tween(500), label = "a1")
    val a2 by animateColorAsState(Pulse.Violet.copy(alpha = .55f), tween(500), label = "a2")
    Box(modifier.background(Color.Black).drawBehind {
        val w = size.width; val h = size.height
        drawRect(Brush.radialGradient(listOf(a1, Color.Transparent), Offset(w * (0.2f + 0.3f * t), h * 0.18f), w * 0.9f))
        drawRect(Brush.radialGradient(listOf(a2.copy(alpha = .35f), Color.Transparent), Offset(w * (0.9f - 0.3f * t), h * 0.55f), w * 0.8f))
        drawRect(Brush.verticalGradient(listOf(Color.Transparent, Color.Black), startY = h * 0.55f))
    }, content = content)
}
fun Color.darkened(f: Float) = Color(red * (1 - f), green * (1 - f), blue * (1 - f), alpha)
```
Use it as the Now Playing backdrop and (static, `t = 0`) behind the For you header. 3 draw calls, no blur, safe on mid-range GPUs. Do not use `Modifier.blur` on full-screen layers (slow on many phones).

### 5.3 Glow artwork
```kotlin
@Composable fun GlowArtwork(uri: String?, size: Dp, glow: Color, modifier: Modifier = Modifier) {
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Box(Modifier.matchParentSize().padding(8.dp).drawBehind { drawCircle(Brush.radialGradient(listOf(glow.copy(.55f), Color.Transparent)), radius = this.size.minDimension * .75f) })
        Artwork(uri, size * 0.9f, radius = 20.dp, px = 512)
    }
}
```

### 5.4 Gradient pill button (primary actions)
```kotlin
@Composable fun GradientPill(text: String, icon: ImageVector, colors: List<Color>, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.height(52.dp).clip(CircleShape).background(Brush.horizontalGradient(colors)).pressScale().clickable(onClick = onClick).padding(horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
        Icon(icon, null, tint = Color.White); Spacer(Modifier.width(8.dp)); Text(text, color = Color.White, fontWeight = FontWeight.Bold) }
}
```
The original spec asked for white Shuffle/Play pills with black text on the Songs screen: keep those exactly (white bg, black text). Gradient pills are for For you, Backup and dialogs.

### 5.5 Seek bar with time pill
Slider thumb 12dp (18dp while dragging), track 4dp, active track = accent gradient. Elapsed on the left, remaining or total on the right (tap toggles, saved). While dragging show a floating pill above the thumb with the target time. Position flows are throttled to 250 ms and collected only inside this composable (so the rest of Now Playing does not recompose).

### 5.6 Shimmer + empty states
`Modifier.shimmer()` = animated horizontal gradient over `Surface`. Used only for the Songs list while the first scan has no rows yet. Each empty state: 72dp icon in a gradient circle, one sentence, one button. List: no permission, empty library, scanning, no results, no lyrics, no favourites, equalizer unsupported, unsupported file, SD card removed, **backup rejected** (file 16).

## 6. Screen polish checklist (do all)
1. **Top bar**: hamburger left, scrollable tabs centre with gradient underline (Violet->Magenta, 3dp, rounded), Skin / Search / Settings icons right. Title text never overlaps the camera cut-out.
2. **For you**: header greeting by time of day ("Good evening"), resume banner with artwork-tinted gradient + animated play ring, Shuffle/Play pills, 2x2 live tiles (Today minutes, Favourites count, Sleep timer remaining if active, Equalizer preset name), "Recently added", "Top picks" (hidden until history exists), "Because you liked" (artist of the most-played track, hidden if < 3 songs).
3. **Songs**: sticky header with count and "Scanning..." chip, A-Z fast scroller (file 11), swipe a row right = Play next, left = Add to queue (with haptic + snackbar Undo), long press = multi-select.
4. **Playlists**: 2x2 gradient system cards with real counts; user playlists with 2x2 artwork mosaic; reorder with drag handles.
5. **Folders / Albums / Artists**: as 01; artist avatar = initials on a gradient chosen by hash of the name (stable).
6. **Now Playing**: aurora backdrop, glow artwork, swipe left/right on artwork = next/previous, swipe down = collapse, double-tap artwork = favourite (heart pop), long-press title = copy "title - artist".
7. **Mini player**: glass (`Surface` at 85% alpha + 1dp `Outline` border), progress hairline in accent gradient, swipe horizontally = next/previous, swipe up = expand, bottom inset respected, never covers the last list row (list bottom padding = mini height + 16dp).
8. **Settings**: grouped cards with icons, switches with gradient track when on, live preview of theme change.
9. **Backup & Transfer**: file 16.
10. Haptics: `HapticFeedbackType.LongPress` on long-press and drag start, `TextHandleMove` on seek ticks (>= 5 s steps). Respect system haptics setting.

## 7. Skins V2 (10 gradients; swatches are circles; active = white ring + Check icon)
Plum `#2A0826->#120310->#000`, Aurora `#3A1C71->#1B1B4B->#000`, Afterglow `#FF2A6D->#5B1A58->#0A0612`, Ocean `#0F4C75->#051937`, Sunrise `#FF8C78->#C2377A->#2B1055`, Forest `#0B3D2E->#03140F`, Mono `#2B2B2B->#000`, **Neon** `#00C3FF->#9B7BFF->#000`, **Candy** `#FF6DAA->#B36BFF->#120310`, **Lava** `#FF9500->#FF2A6D->#1A0508`. Skin backgrounds are used at max 60% alpha over black so text contrast holds.

## 8. Responsive (unchanged breakpoints, sharper rules)
Compact <600dp: drawer + floating mini player + full-screen Now Playing. Medium 600-839dp: NavigationRail, 2-column For you, album grid `Adaptive(160.dp)`. Expanded >=840dp: rail + content + docked Now Playing panel 380dp, songs and queue side by side. Content max width 840dp on very wide screens, centred. Artwork always square. Use `BoxWithConstraints`/`WindowSizeClass`, never device model checks. On foldables listen to `WindowLayoutInfo` only if already a dependency; otherwise just use width class. Test widths 320, 393, 412, 600, 840, 1280 dp and font scale 1.0 / 1.3 / 2.0.

## 9. Icons (Material icons-extended, no emoji anywhere)
Same list as 02 plus: `Shield` (backup), `Restore`, `Backup`, `Widgets`, `Palette`, `GraphicEq`, `Speed`, `NightsStay`, `Whatshot`, `Insights`, `DragHandle`, `SwapVert`, `SkipNext`, `Share`, `ContentCopy`, `Warning`, `CheckCircle`, `Info`. The launcher/favicon sound-wave icon is in file 17.

## 10. Accessibility
`contentDescription` on every icon button; `Role.Button`/`semantics` on custom clickables; `liveRegion` for the sleep timer countdown; contrast checks on gradients; no motion-only information; support font scale 2.0 with wrapping (never fixed-height text rows; use `heightIn(min = ..)`).

## 11. Acceptance
Screens match this file; true-black base; aurora only on Now Playing (+ static on For you); every press has scale feedback; reduced motion kills infinite animations; no emoji; no clipped text at 2.0 font scale; mini player never hides the last row; punch-hole area free of controls. Update `brain.md`.
