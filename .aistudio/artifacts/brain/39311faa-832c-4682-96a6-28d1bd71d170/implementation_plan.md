# Implementation Plan - Atmospheric Split Queue Player Screen

Implement a production-grade, two-tier split screen player with a dark atmospheric forest/mist upper section and a light theme Up Next queue lower section. Integrate this design both as a premier player layout in the live layout switcher and as an architectural reference implementation with state hoisting and Android Studio previews.

---

## User Review Required

> [!NOTE]
> The screen strictly follows the layout specifications:
> - **Upper Section**: Dark atmospheric view (forest/mist theme) with gradient overlay, top bar (collapse chevron, Song/Lyrics pill, shirt theme toggle, MoreVert menu, layout picker), center track details and slider, horizontal playback controls with circular semi-transparent pause button, and secondary quick-action bar (heart, sleep timer, playlist add, queue, equalizer).
> - **Lower Section**: Light theme card (`RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)`, off-white surface) with fixed scrollable `LazyColumn` for Up Next tracks, featuring amber/golden-yellow highlighting and active animated equalizer indicators for the current track.

---

## Proposed Changes

### 1. Model & Domain Integration

#### `app/src/main/java/com/pulse/player/ui/screens/PlayerLayoutMode.kt`
- Add `ATMOSPHERIC_QUEUE` ("Atmospheric Queue", subtitle: "Dark mist overlay & light queue card", icon: `Icons.Rounded.Splitscreen`) to `PlayerLayoutMode`.
- Set `ATMOSPHERIC_QUEUE` as the default or premier selectable layout mode in `PlayerLayoutMode.fromId()`.

---

### 2. Atmospheric Background & Assets

#### `app/src/main/java/com/pulse/player/ui/components/AtmosphericBackground.kt`
- Create a high-fidelity atmospheric forest/mist visual component with layered organic fog shaders/canvases and dark semi-transparent contrast gradients, ensuring optimal legibility for white text and controls.

---

### 3. Screen & Layout Implementation

#### `app/src/main/java/com/pulse/player/ui/screens/layouts/AtmosphericQueuePlayerLayout.kt`
Create a stateless, production-ready composable implementation:
- **Upper Section (Weight ~1.1f to 1.25f)**:
  - Atmospheric dark background.
  - **Top Bar**: Downward chevron, centered "Song" / "Lyrics" segment, shirt theme toggle icon (`Icons.Rounded.Checkroom`), layout switcher icon (`Icons.Rounded.DashboardCustomize`), and three-dot `MoreVert` menu.
  - **Center Details & Progress**:
    - Bold white centered track title.
    - Translucent medium subtitle/artist text.
    - Thin slider flanked by elapsed time ("0:06") and total duration ("4:22").
  - **Playback Controls Row**:
    - Shuffle toggle, previous track button, large circular semi-transparent pause button (64dp) with white icon, next track button, repeat toggle.
  - **Secondary Action Bar**:
    - Heart/favorite toggle, sleep timer, add to playlist, queue jump, equalizer sliders icon.
- **Lower Section (Weight ~0.9f to 1.0f)**:
  - Surface with `RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)` and light container color (`Color(0xFFF9F9FB)`).
  - Header: "Up Next" with track count badge.
  - Scrollable `LazyColumn`:
    - Each track item has:
      - Left: 8.dp rounded thumbnail slot with album art or active amber equalizer animated bars for currently playing song.
      - Center: Bold track title (styled in amber/golden-yellow `#D4AF37` when active, dark charcoal `#1C1B1F` when idle) and muted gray artist name.
      - Right: `MoreVert` options button with >= 48dp touch target.
- **Stateless Architecture**:
  - Expose clean state object and lambda callbacks (`onPlayPauseClick`, `onPreviousClick`, `onNextClick`, `onSeek`, `onTrackSelect`, `onToggleFavorite`, `onToggleShuffle`, `onCycleRepeat`, `onOpenTheme`, `onOpenPicker`, `onCollapse`).
- **Android Studio Previews**:
  - Include `@Preview` with pre-populated sample tracks (e.g., "Hurts So Good (Lyric...)", "AstridSofficialVEVO", etc.).

---

### 4. Integration into NowPlayingScreen & ViewModels

#### `app/src/main/java/com/pulse/player/ui/screens/NowPlayingScreen.kt`
- Add `PlayerLayoutMode.ATMOSPHERIC_QUEUE` case in the player layout switcher.
- Wire up the active queue tracks from `MainViewModel` (or `db.tracks().observeAll()`) directly into the lower section list.

---

## Verification Plan

### Automated Tests
- Run `gradle :app:testDebugUnitTest` to verify all 137 unit tests pass.
- Update `PlayerLayoutModeTest.kt` to cover `ATMOSPHERIC_QUEUE`.

### Build Compilation
- Run `compile_applet` tool to confirm successful APK build with zero compilation errors.

### Manual Verification
1. Launch Now Playing screen and verify the atmospheric forest/mist background and split view.
2. Confirm the top bar shows chevron, Song/Lyrics toggle, shirt theme icon, layout switcher, and menu.
3. Verify the circular pause button and secondary action row operate smoothly.
4. Verify the lower light card displays the Up Next queue with golden-yellow active item and animated equalizer.
5. Tap different tracks in the queue list to verify instantaneous track switching.
6. Test layout switcher button and swipe gestures to cycle between all player styles.
