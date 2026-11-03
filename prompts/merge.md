# 00 — START HERE: "Pulse Player" (Offline Music Player for Android)

> You (the AI coding model) are building a **native Android offline music player**. This pack has **18 files (00–17)**. Do **not** read them all at once: use the **Spec Index** (section 4) to read only the files for the current phase. Never write "rest unchanged" inside a file; if a file is too long for one reply, split it as `(part 1/2)`, `(part 2/2)`.

## 1. What the project is
**Pulse Player** is a fast, beautiful, 100% offline music player for audio files stored on the phone or SD card. No account, ads, internet, or streaming. It is a *music player*, not a vault: no hidden media, no PIN screens.

- **Language/UI:** Kotlin, Jetpack Compose, Material 3 (no XML layouts except widget/manifest resources)
- **Playback:** AndroidX Media3 (ExoPlayer + MediaSessionService)
- **Data:** Room (library, favorites, playlists, history), DataStore (settings)
- **Widgets:** Jetpack Glance
- **Architecture:** MVVM, single Activity, `StateFlow`
- **SDK versions:** `minSdk`, `compileSdk`, `targetSdk` live only in `gradle/libs.versions.toml` (version catalog). Never hard-code them elsewhere.
- **No INTERNET permission.** Ever.

## 2. Primary device (design for this first)
Infinix phone, XOS 14.5: 6.78" AMOLED, 1080x2436 (20:9), centered punch-hole camera, rounded corners, ~86% screen-to-body. Use true-black (`#000000`) AMOLED surfaces, `enableEdgeToEdge()`, and respect `WindowInsets.displayCutout`, status bar and gesture-nav insets. A **tablet** will come later: layouts must adapt through `WindowSizeClass` (see 02 and 05).

## 3. The pack at a glance (three layers)
**Layer 1 — Base app (files 00–09).** The original app: features, design system, data and scan, playback, UI code, player themes, Classic favourites backup, widgets/recap/README/icon, and the first set of AI Studio prompts.

**Layer 2 — Full backup (files 10–11).** A second backup/restore method based on the real sample `ManualBackup_20261028_172204175.wav` (a ZIP of JSON and SQLite files renamed `.wav`). It sits **next to** the Classic favourites backup; both must work. Labelled **"Full backup (reference format)"**.

**Layer 3 — Fix pack and new theme (files 12–17).** Fixes the review findings and adds the ninth player theme. **Where a fix-pack file disagrees with an older file, the fix-pack file wins.**

| # | File | Purpose |
|---|------|---------|
| 00 | `00_START_HERE.md` | This file: rules, pack map, Spec Index, build order |
| 01 | `01_PRODUCT_AND_FEATURES.md` | Every screen, feature, behavior |
| 02 | `02_DESIGN_SYSTEM.md` | Colors, gradients, icons, motion, responsive rules |
| 03 | `03_PROJECT_DATA_AND_SCAN.md` | Gradle, manifest, Room, MediaStore scanner, artwork (scan, permissions and artwork parts are overridden by 14) |
| 04 | `04_PLAYBACK_AND_AUDIO.md` | Service, controller, equalizer, sleep timer, fades (fades, sleep, queue and snapshot parts are overridden by 13 and 14) |
| 05 | `05_UI_CODE.md` | Theme, shell, components, Home, Songs, Now Playing |
| 06 | `06_PLAYER_THEMES_AND_SKINS.md` | Player-theme carousel (now 9 themes), skin theme, wallpapers |
| 07 | `07_FAVORITES_JSON_AND_MUSIC_FOLDER.md` | **Classic** favourites JSON export/import, Music folder, SD card (SAF scan part is overridden by 14 §4) |
| 08 | `08_WIDGETS_STATS_README_ICON.md` | Widgets, monthly recap, README, icon/favicon |
| 09 | `09_AI_STUDIO_PROMPTS.md` | Prompts 0–7 for the base app |
| 10 | `10_COMPATIBLE_BACKUP_SPEC.md` | **Full backup**: analysis of the sample `.wav`, entry table, mapping, matching tiers, restore rules |
| 11 | `11_COMPATIBLE_BACKUP_CODE_AND_PROMPTS.md` | Full backup code blueprints and prompts 8–12 (validation parts overridden by 15) |
| 12 | `12_FIX_SPEC_INDEX_AND_CONFLICTS.md` | Spec Index, resolution of 9 spec conflicts, canonical theme list, task sizing, temp-code rules |
| 13 | `13_FIX_AUDIO_AND_PLAYBACK.md` | Coordinated audio-transition controller (fades, sleep timer), controller attach/detach/reconnect, error policy, seek, ringtone pending action |
| 14 | `14_FIX_SCAN_QUEUE_PERMISSIONS.md` | Recoverable scan, identity model and migration, SD/SAF/copy workflows, queue-order model, permission state, counts, artwork cache |
| 15 | `15_FIX_STATS_AND_BACKUP.md` | Session-based statistics and time zones, hardened Full backup, Classic vs Full separation |
| 16 | `16_THEME_IMMERSIVE_ARTWORK_PLAYER.md` | Ninth theme "Immersive Artwork Player" and lyrics support |
| 17 | `17_TESTS_RECOVERY_AND_PROMPTS.md` | Test matrix, device checklist, recovery procedures, prompts 14–29, completion report |

## 4. Spec Index (which files to read per phase)
Create `docs/spec/SPEC_INDEX.md` with exactly this table. In every prompt, attach **`SPEC_INDEX.md` plus the files in the "Required files" column** — never "all files". Phases run in order.

| Phase | Goal | Required files |
|---|---|---|
| 0 | Audit and plan | `SPEC_INDEX.md`, 00, 12 |
| 1 | Foundation: schema, scan, permissions | 03, 14 |
| 2 | Playback engine: service, fades, queue, sessions | 04, 13, 14 §6–§7, 15 §1 |
| 3 | Theme, shell, main screens | 02, 05, 14 §8–§10 |
| 4 | Player themes and skins (9 themes) | 06, 16 |
| 5 | Classic favourites backup | 07 |
| 6 | Full backup (reference format) | 10, 11, 15 §2–§4 |
| 7 | Widgets, recap, icon, README | 08, 15 §1.4 |
| 8 | Hardening, tests, audit | 17 plus the files of every earlier phase the audit touches |

Prompts: **0–7** in file 09 (base app), **8–12** in file 11 (Full backup), **14–29** in file 17 (fix pack, new theme, tests, final audit). Where an old prompt names a file to read, also attach the fix-pack file listed for that phase in the table above.

## 5. Rules for the AI model (non-negotiable)
1. **One task at a time.** A task changes **at most 6 files** and covers one screen or one subsystem. Finish, build, then stop and report.
2. **Complete files.** No `// TODO`, no `...`, no "same as before". Split long files into labelled parts instead.
3. **Temporary code is allowed only if tagged** `// TODO-TEMP(<phase>): <removal condition>` and listed in `docs/TEMP_CODE.md`. The final audit fails if any tag remains. Temporary code must still be truthful.
4. **No fake data.** Never insert dummy songs, fake stats, or fake counts. Empty library = designed empty state with a Scan button.
5. **No dead buttons.** Every visible control must work, or be hidden, or be disabled with a short explanation.
6. **No emojis in the UI.** Use Material icons (`androidx.compose.material:material-icons-extended`) or the vector icons from file 02.
7. **Do not invent APIs.** If unsure about a Media3/Compose/Room signature for the installed version, check the project's dependency version and adapt. The snippets are blueprints; fix imports and signatures so the project compiles.
8. **Do not request `MANAGE_EXTERNAL_STORAGE`.** Use MediaStore + Storage Access Framework.
9. **Never move, rename, or delete the user's music.** Copying happens only through the explicit Copy action (file 14 §4.2).
10. **Keep disk, DB, JSON and artwork work off the main thread.**
11. **Global guard rules (file 12 §8):**
    - Only `AudioTransitionController` writes `player.volume`.
    - Only the scan reconcile step marks tracks unavailable.
    - Queue order lives in `QueueModel`; Media3's shuffle flag is never enabled.
    - A backup import never opens a database from inside an archive.
    - Only `SessionTracker` writes play history.
    - Themes are presentation only: no second player, queue, service or audio session.
12. **Update `brain.md`** after every task: done / tested / not done / known limits.
13. **Be honest about verification.** End every task with the CHANGED FILES manifest (file 12 §5) and the real result of `./gradlew assembleDebug` and the tests. If you cannot run something, write `NOT RUN`; never write PASS for something you did not run. If the build fails, fix compile errors before moving on.

## 6. Two backup methods (both must work, never mixed)
| | Classic favourites backup | Full backup (reference format) |
|---|---|---|
| Files | 07 | 10, 11, 15 |
| Content | favourites + added-at, one `.json` | up to 7 categories in a ZIP stored as `.wav` |
| Imports the sample `.wav`? | No | Yes |
| UI | Settings → Favourites backup | Settings → Full backup, with preview, Merge (default) / Replace |
The word "compatible" must not appear in the UI. Acceptance of an exported Full backup by the other app is **untested** and must be stated as such.

## 7. What was kept from the original prompt (all preserved)
Top tabs **For you / Songs / Playlists / Folders / Albums**; drawer; skin theme screen with color swatches and wallpapers; resume banner; Shuffle/Play pills; Recently added; Top picks; system playlists (My favourite, Recently added, Recently played, My top tracks); full Now Playing with Song/Lyrics toggle; mini player with spinning disc and marquee; equalizer; sleep timer; queue; **Player theme carousel** and the orange APPLY / USING button; monthly recap; widgets; ringtone; 3D reverb presets; bass boost; virtualizer.

**Player themes: 9 in total.** The canonical list (single source of truth, file 12 §4) is `classic, mountain, mesh, minimal, acoustic, vinyl, coast, starry, immersive`. The older "types A–E" were only layout families used while designing, not a count. The ninth, **Immersive Artwork Player** (blurred artwork background, marquee title, floating controls, orange-highlighted bottom queue panel, optional lyrics), is specified in file 16.

## 8. Improvements added on top (do implement)
**Design**
- Emojis replaced by consistent icons; true-black AMOLED base plus album-art-driven gradients.
- Spring animations, shared mini-player-to-player expand, heart "pop", animated equalizer bars, play/pause icon morph, staggered list entrance (first load only).
- "Special" badge on a few player themes instead of a crown paywall (no purchases; all themes are free).
- Wallpapers and "nature/ocean/stars" backgrounds are **drawn procedurally in code** (no copyrighted images, no network, tiny APK).

**Features**
- **Favorites Export/Import JSON** with import preview, merge/replace, and fuzzy matching that survives path changes (07).
- **Full backup (reference format)** with preview, merge default, one transaction, validation limits (10, 11, 15).
- **Create / choose a "Music" folder** (SAF) for copying songs to a new device; the app scans it (07, 14).
- **Recoverable scanning**, stable track identity, SD card removal handling, Missing songs filter (14).
- **Coordinated fades**: volume fade-in/out on play/pause, audio-focus changes and sleep-timer fade-out through one controller (13).
- **Explicit queue-order model** that survives shuffle toggles and restores safely after restart (14).
- **Session-based listening statistics** that ignore pause/buffering time, are speed-aware and time-zone safe (15).
- **Home-screen widget** (one responsive Glance widget that covers 2x2, 4x1, 4x2).
- Quick tiles on For you: Today's listening, Sleep timer, Favorites shortcut, Equalizer.
- A-Z fast scroller, multi-select, play next / add to queue, song details sheet.
- Play speed 0.5x–2.0x, "Stop after this track", reduced-motion and battery-saver switches.
- Min-duration filter (display only; it never makes songs "unavailable") and folder exclude list.
- Persist last queue and position (restored paused).
- Lyrics from a user-picked `.lrc` / `.txt` file (16).

**Performance**
- Room-indexed list queries, `LazyColumn` with stable keys, bounded LruCache for artwork with size-bucketed keys, 250 ms throttled progress, no whole-screen recomposition from the seek bar, blurred background computed once on a ~96 px bitmap and cached.

## 9. Honest limits to respect
- Equalizer/bass/virtualizer/reverb work only if the device exposes `android.media.audiofx`. Detect and hide unsupported controls.
- True crossfade between tracks is not reliable in stock Media3, so it is **not included**. Fades on play/pause/sleep/focus are.
- Playback of formats (.ape, .midi) depends on the device decoders; show a friendly error if a file can't play.
- Setting a ringtone needs the user to grant "Modify system settings" (`WRITE_SETTINGS`); it is handled as a pending action rechecked on resume.
- Android has no partial audio-library permission: "Limited" means only the folders the user chose via SAF.
- Nothing in this pack has been compiled or run by its author. Every PASS must come from a real run in the project.

## 10. Folder layout to create
```
app/src/main/java/com/pulse/player/
  PulseApp.kt  MainActivity.kt
  data/   (db, scan, repo, settings, backup/classic, backup/full, backup/common)
  playback/ (service, controller, audiofx, sleep, queue, stats)
  ui/     (theme, components, screens, themes, widget)
gradle/libs.versions.toml
brain.md  README.md
docs/ (spec/SPEC_INDEX.md, TEMP_CODE.md)
```
Use package `com.pulse.player` unless the existing project already has a package ID; if it does, keep that one.
# 01 — Product & Features (what every screen must do)

Build order priority: **P0** (must work) → **P1** (polish) → **P2** (only if time remains).

## 1. Navigation structure
- **Phone (Compact width):** Top bar → hamburger (drawer), tab row, action icons (Skin, Search, Settings). Below: pager content. Above bottom edge: floating **mini player**.
- **Tabs (scrollable):** For you · Songs · Playlists · Folders · Albums · Artists
- **Drawer items:** Equalizer, Sleep timer, Monthly recap, Favorites, Backup & Transfer, Skin theme, Player theme, Settings, About.
- **Tablet (Medium/Expanded width):** NavigationRail replaces drawer; content on the left, **Now Playing panel docked on the right** (Expanded only). Album grid uses adaptive columns (`GridCells.Adaptive(160.dp)`).
- Navigation is a simple state-based `sealed interface Screen` + `AnimatedContent` (no Navigation-Compose needed, fewer errors).

## 2. Screens

### 2.1 For you (P0)
1. **Resume banner** (only if history exists): "Continue {title}" + play button + subtle gradient from artwork.
2. **Quick actions:** pill buttons **Shuffle** and **Play** (Play = start at first song of library).
3. **Widget tiles** (2-column grid): *Today* (minutes listened today, real), *Favorites* (count + open), *Sleep timer* (open sheet), *Equalizer* (open).
4. **Recently added** (header + "New" badge + "Play all"): horizontal row of 12 newest tracks with square art.
5. **Top picks:** horizontal carousel of most-played tracks (hidden until play history exists).
6. Empty library → onboarding card: "Allow access and scan your music" with **Scan** and **Choose folder** buttons.

### 2.2 Songs (P0)
- Header: "{n} Songs" plus "Scanning…" chip while scanning.
- Controls: **Shuffle** and **Play** pills (white bg, black text), sort menu (Title, Artist, Date added, Duration, asc/desc), multi-select toggle.
- Row: rounded-square art 52dp (fallback: gradient + music-note icon), title (bold), "artist · album" gray, right side date added + 3-dot menu.
- 3-dot menu: Play next · Add to queue · Add to playlist · Favorite · Song details · Set as ringtone · Share file.
- Multi-select bar: Add to playlist, Favorite, Play next, Select all.
- A–Z fast scroller on the right edge (P1).

### 2.3 Playlists (P0)
- 2x2 grid of **system playlists**: My favourite (blue heart), Recently added (green note), Recently played (purple pie), My top tracks (orange flame). Show real counts.
- "My playlists (X)" + **+** button → dialog to name a playlist.
- Playlist detail: play, shuffle, reorder (drag handle or move up/down buttons), remove, rename, delete (confirm).

### 2.4 Folders (P0)
- List folders that contain audio (from `folderPath` grouped), with track counts. Tap → folder track list.
- Source chip: Internal / SD card / Chosen folder. Include "Choose music folder" (SAF) button.

### 2.5 Albums / Artists (P0)
- Albums: adaptive grid, square art, title, artist. Detail: header art + track list + Play/Shuffle.
- Artists: list with initials avatar gradient, "n tracks · n albums". Detail: albums + songs.

### 2.6 Search (P0)
- Full-screen search; 250 ms debounce; results in sections Songs / Albums / Artists / Playlists. Tap a song plays it in the context of the results.

### 2.7 Now Playing (P0)
- Header: collapse arrow, **Song | Lyrics** segmented toggle, 3-dot menu (Track info, Share, Equalizer, Set as ringtone, Player theme).
- Center: artwork with soft glow (or the selected **Player Theme** layout, file 06). Vinyl theme rotates only while playing.
- Title (bold white), artist (gray). Seek slider with elapsed/total time. Optional -10s/+10s buttons.
- Controls: Shuffle, Previous, **Play/Pause (large)**, Next, Repeat (off/all/one).
- Bottom row: Favorite heart (cyan `#00C3FF` when on), Sleep timer, Add to playlist, Queue, Equalizer.
- Queue sheet: current track highlighted with animated bars, remove (swipe or button), move up/down, clear (confirm).
- Lyrics: read `.lrc` next to the file (same name) via SAF/MediaStore when available; synced highlight. If none: "No lyrics found" + **Choose .lrc file**.

### 2.5b Mini player (P0)
Floating glass bar above bottom inset: spinning disc (rotates only when playing and reduced-motion off), marquee title, play/pause, queue icon. Swipe up or tap → expand Now Playing. Thin progress line at its bottom edge.

## 3. Settings (P0/P1)
- **Appearance:** theme mode (System / Dark / AMOLED / Light), Skin theme, Player theme, dynamic accent from artwork (switch), reduced motion (switch).
- **Library:** Rescan, Choose music folder, Exclude folders, minimum duration (0/15/30/60 s), show/hide hidden formats.
- **Playback:** fade on play/pause (switch), speed, stop after current track, pause on headset disconnect (always on).
- **Audio effects:** open Equalizer.
- **Backup & Transfer:** Export favorites (JSON), Import favorites (JSON), Create/choose Music folder, instructions card.
- **Widgets:** how to add the widget (text steps).
- **About:** version, "Offline, no internet permission" statement.

## 4. Feature behaviors that must be exact
| Feature | Required behavior |
|---|---|
| Favorite | Instant heart animation; persisted in Room; shown everywhere from the same Flow |
| Shuffle | True random order of the current queue; toggling off restores original order |
| Repeat | Off → All → One cycle; icon changes; One shows "1" badge |
| Sleep timer | Presets 15/30/45/60, custom, and "end of this track"; fade out over last 10 s; cancel button; remaining time shown |
| Headset unplug | Pause immediately (`setHandleAudioBecomingNoisy(true)`) |
| Phone call | Pause via audio focus, resume when focus returns |
| Notification | Media3 notification with prev/play/next; lock-screen controls via MediaSession |
| Ringtone | If `Settings.System.canWrite()` false → open `ACTION_MANAGE_WRITE_SETTINGS`; else set ringtone/notification/alarm via `RingtoneManager.setActualDefaultRingtoneUri` using the track's content URI |
| Missing file | Mark unavailable (greyed), never delete favorites |
| Queue restore | Save queue ids + index + position every 5 s while playing and on pause; restore as paused on launch |
| Recap | Computed from real `play_events`; hidden if no data |

## 5. Empty / error states (design all)
No permission · empty library · scanning · no search results · no lyrics · no favorites · equalizer unsupported · file unsupported · SD card removed. Each has an icon, one sentence, and one action button.

## 6. Out of scope (do not add)
Streaming, accounts, ads, analytics, cloud sync, online lyrics/art, audio conversion, tag editing, Android Auto/Wear.
# 02 — Design System (colorful, smooth, AMOLED-first)

Goal: a premium, vibrant but clean player. Artwork and track titles are the heroes. Gradients create atmosphere; they are never behind every button.

## 1. Color tokens
```kotlin
object Pulse {
    // Surfaces (AMOLED first)
    val Black      = Color(0xFF000000)
    val Bg         = Color(0xFF07060C)
    val Surface    = Color(0xFF12101C)
    val Elevated   = Color(0xFF1B1829)
    val Outline    = Color(0xFF2E2A42)
    // Text
    val TextHi     = Color(0xFFFFFFFF)
    val TextMid    = Color(0xFFA0A0A0)   // secondary per original spec
    val TextLow    = Color(0xFF6E6B80)
    // Accents
    val Cyan       = Color(0xFF00C3FF)   // favorite heart + highlights (original spec)
    val Violet     = Color(0xFF9B7BFF)
    val Magenta    = Color(0xFFFF2A6D)
    val Pink       = Color(0xFFFF6DAA)
    val Coral      = Color(0xFFFF8C78)
    val Mint       = Color(0xFF3DDC97)
    val Orange     = Color(0xFFFF9500)   // APPLY button (original spec)
    val Purple     = Color(0xFFB36BFF)
    val Error      = Color(0xFFFF647C)
    // Original dark-magenta background spec
    val PlumTop    = Color(0xFF2A0826)
    val PlumBottom = Color(0xFF120310)
}
```

## 2. Gradient presets (used for skins and backdrops)
| Name | Colors (top → bottom) |
|---|---|
| Plum (default) | `#2A0826` → `#120310` → `#000000` |
| Aurora | `#3A1C71` → `#1B1B4B` → `#00C3FF33` over black |
| Afterglow | `#FF2A6D` → `#5B1A58` → `#0A0612` |
| Ocean | `#0F4C75` → `#051937` |
| Sunrise | `#FF8C78` → `#C2377A` → `#2B1055` |
| Forest | `#0B3D2E` → `#03140F` |
| Mono | `#2B2B2B` → `#000000` |
| Mesh (Player theme B) | `#FF2A6D` → `#051937` |
| Dynamic | dominant artwork color (Palette), darkened to HSL lightness ≤ 0.22, → black |

Rules: foreground text over any gradient must be white and keep contrast ≥ 4.5:1 (darken the gradient, never lighten text). Never use color alone for state; pair with an icon change or label.

## 3. Shape, spacing, type
- 4dp grid: 4, 8, 12, 16, 20, 24, 32.
- Screen horizontal padding **16dp** (20dp on width ≥ 400dp, 32dp on tablets). **Every** text and element sits inside this padding; sections separated by 24dp.
- Artwork radius 12dp (lists 10dp), cards 20dp, bottom sheets 28dp, primary buttons = full pill (`CircleShape`).
- Min touch target 48dp.
- Font: Roboto/system (or "Inter" only if bundled). Sizes: screen title 28sp Bold, section 20sp SemiBold, track title 16sp Bold, metadata 13sp Regular/Medium. Respect font scale.

## 4. Icons (Material icons-extended; replace every emoji)
Menu `Menu` · Skin `Checkroom`/`AutoAwesome` · Search `Search` · Settings `Settings` · Play `PlayArrow` · Pause `Pause` · Next `SkipNext` · Prev `SkipPrevious` · Shuffle `Shuffle` · Repeat `Repeat`/`RepeatOne` · Heart `Favorite`/`FavoriteBorder` · Queue `QueueMusic` · Add to playlist `PlaylistAdd` · Equalizer `Equalizer`/`Tune` · Sleep `Bedtime`/`Timer` · Lyrics `Lyrics` · Folder `Folder` · Album `Album` · Artist `Person` · Recently added `LibraryAdd` · Recently played `History` · Top tracks `LocalFireDepartment` · More `MoreVert` · Back `ArrowBack` · Collapse `KeyboardArrowDown` · Replay10 `Replay10` · Forward10 `Forward10` · Export `Upload` · Import `Download` · Check `Check` · Special badge `WorkspacePremium` · Fallback art `MusicNote`.

## 5. Motion spec
| Interaction | Animation |
|---|---|
| Tap feedback | ripple + `scale 0.96` spring (stiffness Medium) |
| Heart | scale 1 → 1.35 → 1 spring, color tween 200 ms |
| Play/Pause | `AnimatedContent` icon crossfade + scale 160 ms |
| Mini → Full player | `AnimatedContent` slide up 300 ms + artwork fade; backdrop color tween 400 ms |
| Tab change | pager swipe + indicator offset spring |
| Screen change | fade+slide 240 ms (`slideInHorizontally { it / 8 } + fadeIn`) |
| Lists | first-load stagger (max 12 items, 40 ms apart); never animate on scroll |
| Playing indicator | 3 animated bars (`rememberInfiniteTransition`), only on the current row while playing |
| Vinyl / disc | rotate 360° per 8 s while playing; pauses (keeps angle) when paused |
| Backdrop | cross-fade between old/new dynamic gradient 500 ms |

**Reduced motion:** a setting (and system animator scale = 0) disables disc rotation, bars, stagger and infinite animations. All features still work.

## 6. Responsive rules (use `WindowSizeClass`)
- Compact (<600dp): single column, drawer, floating mini player, full-screen Now Playing.
- Medium (600–839dp): NavigationRail, two-column For you widgets, album grid adaptive 160dp.
- Expanded (≥840dp): rail + content + docked Now Playing panel (width 380dp), songs list and queue side by side.
- Always keep artwork **square**; only grow columns, never stretch. Test widths 320, 393, 430, 600, 840, 1280 dp and font scale 1.0/1.3/2.0.
- Insets: `Modifier.windowInsetsPadding(WindowInsets.safeDrawing)` on root; mini player adds `navigationBarsPadding()`; the last list item gets bottom padding = mini player height + 16dp so nothing is hidden.

## 7. Theme modes
System / Dark (plum gradient) / AMOLED (pure black, no gradient except Now Playing) / Light (warm off-white `#FAF7FF`, white cards, text `#1A1625`, accents desaturated ~15%). Light mode is designed, not inverted.

## 8. Visual quality checklist
Consistent padding · no clipped text at 2.0 font scale · no gradient hurting contrast · mini player never covers last row · punch-hole camera area free of controls (top 24dp safe) · rounded screen corners respected (extra 4dp inner padding for edge-hugging elements).

# 03 — Project Setup, Room Database, MediaStore Scan, Artwork

> Blueprint code. Keep package `com.pulse.player` (or the existing package). Fix imports/versions to match the installed Android Studio. Use the **latest stable** versions of each library; the ones below are minimum-known-good.

## 1. Gradle (app/build.gradle.kts essentials)
```kotlin
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")   // Kotlin 2.x
    id("com.google.devtools.ksp")
}
android {
    namespace = "com.pulse.player"
    compileSdk = libs.versions.compileSdk.get().toInt()
    defaultConfig { applicationId = "com.pulse.player"; minSdk = libs.versions.minSdk.get().toInt(); targetSdk = libs.versions.targetSdk.get().toInt(); versionCode = 1; versionName = "1.0.0" }
    buildFeatures { compose = true }
    buildTypes { release { isMinifyEnabled = true; isShrinkResources = true
        proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro") } }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
}
dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.10.01"))
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material3:material3-window-size-class")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.documentfile:documentfile:1.0.1")
    implementation("androidx.palette:palette-ktx:1.0.0")
    implementation("androidx.datastore:datastore-preferences:1.1.1")
    val media3 = "1.4.1"
    implementation("androidx.media3:media3-exoplayer:$media3")
    implementation("androidx.media3:media3-session:$media3")
    implementation("androidx.media3:media3-ui:$media3")
    val room = "2.6.1"
    implementation("androidx.room:room-runtime:$room"); implementation("androidx.room:room-ktx:$room")
    ksp("androidx.room:room-compiler:$room")
    implementation("androidx.glance:glance-appwidget:1.1.1")
    implementation("androidx.glance:glance-material3:1.1.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-guava:1.9.0")
}
```
No Coil, Hilt or Retrofit: fewer moving parts for a smaller model. Dependencies are created by a tiny manual graph (`PulseApp`).

## 2. AndroidManifest.xml
```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <uses-permission android:name="android.permission.READ_MEDIA_AUDIO" />
    <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" android:maxSdkVersion="32" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK" />
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
    <uses-permission android:name="android.permission.WAKE_LOCK" />
    <uses-permission android:name="android.permission.WRITE_SETTINGS" /> <!-- ringtone only -->
    <!-- NO INTERNET PERMISSION -->
    <application
        android:name=".PulseApp" android:label="Pulse Player"
        android:icon="@mipmap/ic_launcher" android:roundIcon="@mipmap/ic_launcher_round"
        android:theme="@style/Theme.Pulse" android:supportsRtl="true">
        <activity android:name=".MainActivity" android:exported="true" android:launchMode="singleTask"
            android:windowSoftInputMode="adjustResize"
            android:configChanges="orientation|screenSize|smallestScreenSize|screenLayout|keyboardHidden">
            <intent-filter><action android:name="android.intent.action.MAIN"/><category android:name="android.intent.category.LAUNCHER"/></intent-filter>
        </activity>
        <service android:name=".playback.MusicPlaybackService" android:exported="true"
            android:foregroundServiceType="mediaPlayback">
            <intent-filter><action android:name="androidx.media3.session.MediaSessionService"/></intent-filter>
        </service>
        <receiver android:name=".ui.widget.PlayerWidgetReceiver" android:exported="true">
            <intent-filter><action android:name="android.appwidget.action.APPWIDGET_UPDATE"/></intent-filter>
            <meta-data android:name="android.appwidget.provider" android:resource="@xml/player_widget_info"/>
        </receiver>
    </application>
</manifest>
```
`res/values/themes.xml`: `<style name="Theme.Pulse" parent="android:Theme.Material.NoActionBar"><item name="android:windowBackground">#000000</item><item name="android:statusBarColor">@android:color/transparent</item><item name="android:navigationBarColor">@android:color/transparent</item></style>`

## 3. Room entities
```kotlin
package com.pulse.player.data.db
import androidx.room.*

@Entity(tableName = "tracks", indices = [Index("title"), Index("artist"), Index("album"), Index("folderPath"), Index("dateAddedMs")])
data class TrackEntity(
    @PrimaryKey val id: String,              // "ms:<mediaStoreId>" or "saf:<hash>"
    val uri: String, val displayName: String,
    val title: String, val artist: String, val album: String, val albumArtist: String?,
    val durationMs: Long, val sizeBytes: Long, val mimeType: String?,
    val folderPath: String, val dateAddedMs: Long, val dateModifiedMs: Long,
    val trackNumber: Int, val year: Int,
    val normTitle: String, val normArtist: String, val normAlbum: String,
    val available: Boolean = true
)
@Entity(tableName = "favorites") data class FavoriteEntity(@PrimaryKey val trackId: String, val addedAtMs: Long)
@Entity(tableName = "playlists") data class PlaylistEntity(@PrimaryKey(autoGenerate = true) val id: Long = 0, val name: String, val createdAtMs: Long)
@Entity(tableName = "playlist_tracks", primaryKeys = ["playlistId", "trackId"], indices = [Index("trackId")])
data class PlaylistTrackEntity(val playlistId: Long, val trackId: String, val position: Int, val addedAtMs: Long)
@Entity(tableName = "play_events", indices = [Index("playedAtMs"), Index("trackId")])
data class PlayEventEntity(@PrimaryKey(autoGenerate = true) val id: Long = 0, val trackId: String, val playedAtMs: Long, val listenedMs: Long, val completed: Boolean)

data class TrackWithFav(@Embedded val track: TrackEntity, val isFavorite: Boolean)
data class TrackStat(val trackId: String, val plays: Int, val ms: Long)
data class ArtistStat(val artist: String, val plays: Int, val ms: Long)
```

## 4. DAOs
```kotlin
@Dao interface TrackDao {
    @Query("SELECT t.*, (f.trackId IS NOT NULL) AS isFavorite FROM tracks t LEFT JOIN favorites f ON f.trackId = t.id WHERE t.available = 1 AND t.durationMs >= :minMs ORDER BY t.title COLLATE NOCASE")
    fun observeAll(minMs: Long): Flow<List<TrackWithFav>>
    @Query("SELECT * FROM tracks WHERE id IN (:ids)") suspend fun byIds(ids: List<String>): List<TrackEntity>
    @Query("SELECT * FROM tracks WHERE id = :id") suspend fun byId(id: String): TrackEntity?
    @Query("SELECT * FROM tracks") suspend fun allOnce(): List<TrackEntity>
    @Query("SELECT COUNT(*) FROM tracks WHERE available = 1") fun observeCount(): Flow<Int>
    @Upsert suspend fun upsertAll(items: List<TrackEntity>)
    @Query("UPDATE tracks SET available = 0 WHERE id LIKE 'ms:%'") suspend fun markMediaStoreUnavailable()
    @Query("UPDATE tracks SET available = 0 WHERE id LIKE 'saf:%'") suspend fun markSafUnavailable()
}
@Dao interface FavoriteDao {
    @Query("SELECT trackId FROM favorites") fun observeIds(): Flow<List<String>>
    @Query("SELECT t.*, 1 AS isFavorite FROM favorites f JOIN tracks t ON t.id = f.trackId ORDER BY f.addedAtMs DESC") fun observeTracks(): Flow<List<TrackWithFav>>
    @Query("SELECT t.* FROM favorites f JOIN tracks t ON t.id = f.trackId ORDER BY f.addedAtMs DESC") suspend fun allTracksOnce(): List<TrackEntity>
    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE trackId = :id)") suspend fun isFavorite(id: String): Boolean
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun add(f: FavoriteEntity)
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun addAll(f: List<FavoriteEntity>)
    @Query("DELETE FROM favorites WHERE trackId = :id") suspend fun remove(id: String)
    @Query("DELETE FROM favorites") suspend fun clear()
    @Transaction suspend fun toggle(id: String, now: Long) { if (isFavorite(id)) remove(id) else add(FavoriteEntity(id, now)) }
}
@Dao interface PlaylistDao {
    @Query("SELECT * FROM playlists ORDER BY createdAtMs") fun observePlaylists(): Flow<List<PlaylistEntity>>
    @Query("SELECT COUNT(*) FROM playlist_tracks WHERE playlistId = :id") fun observeCount(id: Long): Flow<Int>
    @Query("SELECT t.*, (f.trackId IS NOT NULL) AS isFavorite FROM playlist_tracks p JOIN tracks t ON t.id = p.trackId LEFT JOIN favorites f ON f.trackId = t.id WHERE p.playlistId = :id ORDER BY p.position")
    fun observeTracks(id: Long): Flow<List<TrackWithFav>>
    @Insert suspend fun create(p: PlaylistEntity): Long
    @Query("UPDATE playlists SET name = :name WHERE id = :id") suspend fun rename(id: Long, name: String)
    @Query("DELETE FROM playlists WHERE id = :id") suspend fun deletePlaylist(id: Long)
    @Query("DELETE FROM playlist_tracks WHERE playlistId = :id") suspend fun clearTracks(id: Long)
    @Query("SELECT COALESCE(MAX(position), -1) + 1 FROM playlist_tracks WHERE playlistId = :id") suspend fun nextPos(id: Long): Int
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun addTrack(x: PlaylistTrackEntity)
    @Query("DELETE FROM playlist_tracks WHERE playlistId = :id AND trackId = :trackId") suspend fun removeTrack(id: Long, trackId: String)
    @Transaction suspend fun delete(id: Long) { clearTracks(id); deletePlaylist(id) }
}
@Dao interface StatsDao {
    @Insert suspend fun insert(e: PlayEventEntity)
    @Query("SELECT COALESCE(SUM(listenedMs),0) FROM play_events WHERE playedAtMs BETWEEN :from AND :to") suspend fun totalMs(from: Long, to: Long): Long
    @Query("SELECT trackId, COUNT(*) AS plays, SUM(listenedMs) AS ms FROM play_events WHERE playedAtMs BETWEEN :from AND :to GROUP BY trackId ORDER BY plays DESC LIMIT :limit")
    suspend fun topTracks(from: Long, to: Long, limit: Int): List<TrackStat>
    @Query("SELECT t.artist AS artist, COUNT(*) AS plays, SUM(e.listenedMs) AS ms FROM play_events e JOIN tracks t ON t.id = e.trackId WHERE e.playedAtMs BETWEEN :from AND :to GROUP BY t.artist ORDER BY ms DESC LIMIT :limit")
    suspend fun topArtists(from: Long, to: Long, limit: Int): List<ArtistStat>
    @Query("SELECT trackId FROM play_events GROUP BY trackId ORDER BY MAX(playedAtMs) DESC LIMIT :limit") fun observeRecentIds(limit: Int): Flow<List<String>>
    @Query("SELECT trackId FROM play_events GROUP BY trackId ORDER BY COUNT(*) DESC LIMIT :limit") fun observeTopIds(limit: Int): Flow<List<String>>
}
@Database(entities = [TrackEntity::class, FavoriteEntity::class, PlaylistEntity::class, PlaylistTrackEntity::class, PlayEventEntity::class], version = 1, exportSchema = true)
abstract class PulseDb : RoomDatabase() {
    abstract fun tracks(): TrackDao; abstract fun favorites(): FavoriteDao
    abstract fun playlists(): PlaylistDao; abstract fun stats(): StatsDao
}
```
Never use `fallbackToDestructiveMigration()`. Add real migrations when the version changes.

## 5. App graph (manual DI)
```kotlin
class PulseApp : Application() {
    lateinit var db: PulseDb; lateinit var settings: SettingsStore
    lateinit var scanner: LibraryScanner; lateinit var player: PlayerController
    override fun onCreate() {
        super.onCreate()
        db = Room.databaseBuilder(this, PulseDb::class.java, "pulse.db").build()
        settings = SettingsStore(this)
        scanner = LibraryScanner(this, db)
        player = PlayerController(this, db)
    }
}
val Context.app get() = applicationContext as PulseApp
```

## 6. Normalization helper
```kotlin
import java.text.Normalizer
fun norm(s: String?): String =
    Normalizer.normalize((s ?: "").trim(), Normalizer.Form.NFKC).lowercase().replace(Regex("\\s+"), " ")
```

## 7. Library scanner (MediaStore)
```kotlin
class LibraryScanner(private val ctx: Context, private val db: PulseDb) {
    private val _scanning = MutableStateFlow(false); val scanning = _scanning.asStateFlow()
    private val _progress = MutableStateFlow(0); val progress = _progress.asStateFlow()

    suspend fun scanMediaStore(minDurationMs: Long, excluded: Set<String>): Int = withContext(Dispatchers.IO) {
        if (_scanning.value) return@withContext 0
        _scanning.value = true; _progress.value = 0
        try {
            val col = if (Build.VERSION.SDK_INT >= 29) MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
                      else MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            val p = arrayOf(MediaStore.Audio.Media._ID, MediaStore.Audio.Media.TITLE, MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ALBUM, MediaStore.Audio.Media.DURATION, MediaStore.Audio.Media.SIZE,
                MediaStore.Audio.Media.MIME_TYPE, MediaStore.Audio.Media.DISPLAY_NAME, MediaStore.Audio.Media.DATE_ADDED,
                MediaStore.Audio.Media.DATE_MODIFIED, MediaStore.Audio.Media.TRACK, MediaStore.Audio.Media.YEAR,
                MediaStore.Audio.Media.RELATIVE_PATH)   // API 29+; on 26-28 the column is missing → handled below
            val sel = "${MediaStore.Audio.Media.DURATION} >= ? AND ${MediaStore.Audio.Media.SIZE} > 0 AND ${MediaStore.Audio.Media.IS_RINGTONE} = 0 AND ${MediaStore.Audio.Media.IS_NOTIFICATION} = 0 AND ${MediaStore.Audio.Media.IS_ALARM} = 0"
            val batch = ArrayList<TrackEntity>(500); var count = 0
            val projection = if (Build.VERSION.SDK_INT >= 29) p else p.dropLast(1).toTypedArray()
            ctx.contentResolver.query(col, projection, sel, arrayOf(minDurationMs.toString()), null)?.use { c ->
                val iId = c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val iT = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val iAr = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val iAl = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val iD = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val iS = c.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
                val iM = c.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE)
                val iN = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
                val iDa = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
                val iDm = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_MODIFIED)
                val iTr = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TRACK)
                val iY = c.getColumnIndexOrThrow(MediaStore.Audio.Media.YEAR)
                val iR = c.getColumnIndex(MediaStore.Audio.Media.RELATIVE_PATH)
                db.tracks().markMediaStoreUnavailable()
                while (c.moveToNext()) {
                    val folder = if (iR >= 0) c.getString(iR).orEmpty().trimEnd('/') else "Music"
                    if (excluded.any { folder.startsWith(it, ignoreCase = true) }) continue
                    val id = c.getLong(iId)
                    val title = c.getString(iT).orEmpty().ifBlank { c.getString(iN).orEmpty().substringBeforeLast('.') }
                    val artist = c.getString(iAr).orEmpty().let { if (it == "<unknown>" || it.isBlank()) "Unknown artist" else it }
                    val album = c.getString(iAl).orEmpty().ifBlank { "Unknown album" }
                    batch += TrackEntity(
                        id = "ms:$id", uri = ContentUris.withAppendedId(col, id).toString(), displayName = c.getString(iN).orEmpty(),
                        title = title, artist = artist, album = album, albumArtist = null,
                        durationMs = c.getLong(iD), sizeBytes = c.getLong(iS), mimeType = c.getString(iM), folderPath = folder,
                        dateAddedMs = c.getLong(iDa) * 1000, dateModifiedMs = c.getLong(iDm) * 1000,
                        trackNumber = c.getInt(iTr) % 1000, year = c.getInt(iY),
                        normTitle = norm(title), normArtist = norm(artist), normAlbum = norm(album))
                    count++
                    if (batch.size >= 500) { db.tracks().upsertAll(batch.toList()); batch.clear(); _progress.value = count }
                }
                if (batch.isNotEmpty()) db.tracks().upsertAll(batch.toList())
                _progress.value = count
            }
            count
        } finally { _scanning.value = false }
    }
}
```
Notes for the model: `IS_ALARM` etc. are valid MediaStore columns. `markMediaStoreUnavailable()` runs before upserts so deleted files become unavailable while favorites stay. If `query` returns null, report an error state (permission likely missing) and do not mark everything unavailable (guard: only call `markMediaStoreUnavailable()` after a non-null cursor, as above).

## 8. Permissions helper (Compose)
```kotlin
fun audioPermission() = if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_AUDIO else Manifest.permission.READ_EXTERNAL_STORAGE

@Composable fun rememberAudioPermissionLauncher(onResult: (Boolean) -> Unit) =
    rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission(), onResult)
```
Flow: user taps **Scan** → show rationale text → request → on grant call `scanMediaStore()` → on deny show "Open settings" button (`Settings.ACTION_APPLICATION_DETAILS_SETTINGS`). Request `POST_NOTIFICATIONS` (API 33+) the first time playback starts.

## 9. Artwork loader (no library needed, cached, off main thread)
```kotlin
object ArtworkLoader {
    private val cache = object : LruCache<String, Bitmap>(24 * 1024 * 1024) { override fun sizeOf(k: String, v: Bitmap) = v.byteCount }
    suspend fun load(ctx: Context, uri: String, px: Int): Bitmap? = withContext(Dispatchers.IO) {
        val key = "$uri@$px"; cache.get(key)?.let { return@withContext it }
        val bmp = runCatching {
            if (Build.VERSION.SDK_INT >= 29) ctx.contentResolver.loadThumbnail(Uri.parse(uri), Size(px, px), null)
            else MediaMetadataRetriever().use { r ->
                r.setDataSource(ctx, Uri.parse(uri))
                r.embeddedPicture?.let { BitmapFactory.decodeByteArray(it, 0, it.size) }
            }
        }.getOrNull()
        if (bmp != null) cache.put(key, bmp); bmp
    }
    fun dominant(bmp: Bitmap): Int { val p = Palette.from(bmp).maximumColorCount(12).generate()
        return p.getDarkMutedColor(p.getMutedColor(p.getDominantColor(0xFF2A0826.toInt()))) }
}
```
(`MediaMetadataRetriever.use` requires API 29 `AutoCloseable`; for API 26–28 wrap with `try/finally { r.release() }`.)

## 9b. Acceptance for this part
App launches, asks permission only on Scan, real songs appear (log the count), favorites table exists, rescan keeps favorites, nothing on the main thread, `brain.md` updated.
# 04 — Playback, Controller, Equalizer, Sleep Timer, Fades

Rule: **one** ExoPlayer, owned by the service. The UI talks to it only through `MediaController` (class `PlayerController`).

## 1. UI-facing models
```kotlin
data class Track(val id: String, val uri: String, val title: String, val artist: String, val album: String,
                 val durationMs: Long, val isFavorite: Boolean, val folder: String = "", val dateAddedMs: Long = 0)
fun TrackWithFav.toTrack() = Track(track.id, track.uri, track.title, track.artist, track.album, track.durationMs, isFavorite, track.folderPath, track.dateAddedMs)

data class PlaybackUiState(
    val mediaId: String? = null, val title: String = "", val artist: String = "", val album: String = "", val artworkUri: String? = null,
    val isPlaying: Boolean = false, val playWhenReady: Boolean = false, val buffering: Boolean = false,
    val durationMs: Long = 0, val index: Int = -1, val queueSize: Int = 0,
    val shuffle: Boolean = false, val repeat: Int = Player.REPEAT_MODE_OFF, val speed: Float = 1f,
    val sleepRemainingMs: Long = 0, val sleepEndOfTrack: Boolean = false, val error: String? = null
)
```

## 2. Playback service
```kotlin
package com.pulse.player.playback

class MusicPlaybackService : MediaSessionService() {
    private var session: MediaSession? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onCreate() {
        super.onCreate()
        val player = ExoPlayer.Builder(this)
            .setAudioAttributes(AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MUSIC).build(), /* handleAudioFocus = */ true)
            .setHandleAudioBecomingNoisy(true)          // pause on headset/Bluetooth disconnect
            .setWakeMode(C.WAKE_MODE_LOCAL)
            .build()
        player.addListener(object : Player.Listener {
            override fun onAudioSessionIdChanged(audioSessionId: Int) { AudioFx.attach(audioSessionId) }
            override fun onEvents(p: Player, events: Player.Events) { WidgetStateStore.write(this@MusicPlaybackService, p, scope) }
            override fun onPlayerError(error: PlaybackException) {
                // Skip a broken file but never loop forever
                if (player.hasNextMediaItem()) { player.seekToNextMediaItem(); player.prepare() }
            }
        })
        AudioFx.attach(player.audioSessionId)
        session = MediaSession.Builder(this, player).build()
    }
    override fun onGetSession(info: MediaSession.ControllerInfo): MediaSession? = session
    override fun onTaskRemoved(rootIntent: Intent?) {
        val p = session?.player
        if (p == null || !p.playWhenReady || p.mediaItemCount == 0) stopSelf()   // keep playing if user is listening
    }
    override fun onDestroy() {
        scope.cancel(); AudioFx.release()
        session?.run { player.release(); release() }; session = null
        super.onDestroy()
    }
}
```
Notification: Media3 `MediaSessionService` creates it automatically (prev / play-pause / next) and lock-screen controls come from the session. Provide a small notification icon `@drawable/ic_notification` (vector note). Request `POST_NOTIFICATIONS` on API 33+.

## 3. PlayerController (UI side)
```kotlin
class PlayerController(private val ctx: Context, private val db: PulseDb) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var mc: MediaController? = null
    private val _state = MutableStateFlow(PlaybackUiState()); val state = _state.asStateFlow()
    private val _pos = MutableStateFlow(0L); val position = _pos.asStateFlow()          // only seek bars collect this
    private val _queue = MutableStateFlow<List<Track>>(emptyList()); val queue = _queue.asStateFlow()
    private var sleepJob: Job? = null; private var fadeJob: Job? = null
    private var listenedMs = 0L; private var lastId: String? = null

    init {
        val token = SessionToken(ctx, ComponentName(ctx, MusicPlaybackService::class.java))
        val f = MediaController.Builder(ctx, token).buildAsync()
        f.addListener({
            mc = f.get().also { it.addListener(listener); sync(it) }
        }, ContextCompat.getMainExecutor(ctx))
        scope.launch { while (true) { delay(250)
            mc?.let { if (it.isPlaying) { _pos.value = it.currentPosition; listenedMs += 250 } } } }
    }

    private val listener = object : Player.Listener {
        override fun onEvents(p: Player, e: Player.Events) { sync(p as MediaController) }
        override fun onMediaItemTransition(item: MediaItem?, reason: Int) {
            logEvent(completed = reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO)
            lastId = item?.mediaId; listenedMs = 0
            if (_state.value.sleepEndOfTrack && reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO) { mc?.pause(); cancelSleep() }
        }
        override fun onPlayerError(e: PlaybackException) { _state.update { it.copy(error = "This file can't be played on this device.") } }
    }
    private fun logEvent(completed: Boolean) {
        val id = lastId ?: return; val ms = listenedMs
        if (ms >= 30_000 || completed) scope.launch(Dispatchers.IO) { db.stats().insert(PlayEventEntity(trackId = id, playedAtMs = System.currentTimeMillis(), listenedMs = ms, completed = completed)) }
    }
    private fun sync(c: MediaController) {
        val md = c.mediaMetadata
        _state.update { it.copy(
            mediaId = c.currentMediaItem?.mediaId, title = md.title?.toString().orEmpty(), artist = md.artist?.toString().orEmpty(),
            album = md.albumTitle?.toString().orEmpty(), artworkUri = c.currentMediaItem?.localConfiguration?.uri?.toString(),
            isPlaying = c.isPlaying, playWhenReady = c.playWhenReady, buffering = c.playbackState == Player.STATE_BUFFERING,
            durationMs = if (c.duration == C.TIME_UNSET) 0 else c.duration, index = c.currentMediaItemIndex, queueSize = c.mediaItemCount,
            shuffle = c.shuffleModeEnabled, repeat = c.repeatMode, speed = c.playbackParameters.speed) }
        _queue.value = (0 until c.mediaItemCount).map { i -> c.getMediaItemAt(i).let { m ->
            Track(m.mediaId, m.localConfiguration?.uri.toString(), m.mediaMetadata.title.toString(), m.mediaMetadata.artist.toString(),
                  m.mediaMetadata.albumTitle.toString(), m.mediaMetadata.durationMs ?: 0, false) } }
        _pos.value = c.currentPosition
    }

    private fun Track.toItem() = MediaItem.Builder().setMediaId(id).setUri(uri)
        .setMediaMetadata(MediaMetadata.Builder().setTitle(title).setArtist(artist).setAlbumTitle(album).setDurationMs(durationMs).build()).build()

    // ---- commands (all main-thread) ----
    fun playQueue(tracks: List<Track>, startIndex: Int, shuffle: Boolean = false) {
        val c = mc ?: return; if (tracks.isEmpty()) return
        c.setMediaItems(tracks.map { it.toItem() }, startIndex.coerceIn(0, tracks.lastIndex), 0L)
        c.shuffleModeEnabled = shuffle; c.prepare(); fadeIn(c)
    }
    fun togglePlay() { val c = mc ?: return; if (c.isPlaying) fadePause(c) else { if (c.playbackState == Player.STATE_IDLE) c.prepare(); fadeIn(c) } }
    fun next() { mc?.seekToNextMediaItem() }
    fun previous() { mc?.let { if (it.currentPosition > 3000) it.seekTo(0) else it.seekToPreviousMediaItem() } }
    fun seekTo(ms: Long) { mc?.seekTo(ms); _pos.value = ms }
    fun skipBy(ms: Long) { mc?.let { seekTo((it.currentPosition + ms).coerceIn(0, it.duration.coerceAtLeast(0))) } }
    fun toggleShuffle() { mc?.let { it.shuffleModeEnabled = !it.shuffleModeEnabled } }
    fun cycleRepeat() { mc?.let { it.repeatMode = when (it.repeatMode) { Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL; Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE; else -> Player.REPEAT_MODE_OFF } } }
    fun setSpeed(s: Float) { mc?.playbackParameters = PlaybackParameters(s, 1f) }          // pitch preserved
    fun playNext(t: Track) { mc?.let { it.addMediaItem((it.currentMediaItemIndex + 1).coerceAtMost(it.mediaItemCount), t.toItem()) } }
    fun addToQueue(t: Track) { mc?.addMediaItem(t.toItem()) }
    fun removeFromQueue(i: Int) { mc?.removeMediaItem(i) }
    fun moveInQueue(from: Int, to: Int) { mc?.moveMediaItem(from, to) }
    fun clearQueue() { mc?.clearMediaItems() }
    fun skipToQueueIndex(i: Int) { mc?.seekTo(i, 0L) }

    // ---- real audio fades (volume ramp) ----
    private fun ramp(c: MediaController, from: Float, to: Float, ms: Long, done: () -> Unit = {}) {
        fadeJob?.cancel(); fadeJob = scope.launch {
            val steps = 12; for (i in 0..steps) { c.volume = from + (to - from) * i / steps; delay(ms / steps) }; done() }
    }
    private fun fadeIn(c: MediaController) { if (fadeEnabled) { c.volume = 0f; c.play(); ramp(c, 0f, 1f, 350) } else { c.volume = 1f; c.play() } }
    private fun fadePause(c: MediaController) { if (fadeEnabled) ramp(c, c.volume, 0f, 250) { c.pause(); c.volume = 1f } else c.pause() }
    var fadeEnabled = true

    // ---- sleep timer ----
    fun startSleep(minutes: Int) {
        cancelSleep(); val end = SystemClock.elapsedRealtime() + minutes * 60_000L
        sleepJob = scope.launch {
            while (true) { val left = end - SystemClock.elapsedRealtime()
                _state.update { it.copy(sleepRemainingMs = left.coerceAtLeast(0)) }
                if (left <= 0) break
                if (left <= 10_000) mc?.let { it.volume = (left / 10_000f).coerceIn(0f, 1f) }   // 10 s fade-out
                delay(500) }
            mc?.pause(); mc?.volume = 1f; _state.update { it.copy(sleepRemainingMs = 0) }
        }
    }
    fun sleepAtEndOfTrack() { cancelSleep(); _state.update { it.copy(sleepEndOfTrack = true) } }
    fun cancelSleep() { sleepJob?.cancel(); mc?.volume = 1f; _state.update { it.copy(sleepRemainingMs = 0, sleepEndOfTrack = false) } }

    // ---- queue persistence ----
    fun snapshot(): Triple<List<String>, Int, Long>? = mc?.let { c -> Triple((0 until c.mediaItemCount).map { c.getMediaItemAt(it).mediaId }, c.currentMediaItemIndex, c.currentPosition) }
    suspend fun restore(ids: List<String>, index: Int, pos: Long, resolve: suspend (List<String>) -> List<Track>) {
        val tracks = resolve(ids); val c = mc ?: return; if (tracks.isEmpty()) return
        c.setMediaItems(tracks.map { it.toItem() }, index.coerceIn(0, tracks.lastIndex), pos); c.prepare()   // stays paused
    }
}
```
Mini-note: `MediaMetadata.durationMs` is a `Long?`. Use `?: 0L`.

## 4. Audio effects (equalizer, bass, virtualizer, reverb) — capability-checked
```kotlin
object AudioFx {
    private var eq: Equalizer? = null; private var bass: BassBoost? = null
    private var virt: Virtualizer? = null; private var reverb: PresetReverb? = null
    var sessionId = 0; private set
    val reverbNames = listOf("None", "Small room", "Medium room", "Large room", "Medium hall", "Large hall", "Plate")
    private val reverbPresets = shortArrayOf(PresetReverb.PRESET_NONE, PresetReverb.PRESET_SMALLROOM, PresetReverb.PRESET_MEDIUMROOM,
        PresetReverb.PRESET_LARGEROOM, PresetReverb.PRESET_MEDIUMHALL, PresetReverb.PRESET_LARGEHALL, PresetReverb.PRESET_PLATE)

    fun attach(id: Int) {
        if (id == 0 || id == sessionId) return; release(); sessionId = id
        eq = runCatching { Equalizer(0, id).apply { enabled = true } }.getOrNull()
        bass = runCatching { BassBoost(0, id).apply { enabled = true } }.getOrNull()
        virt = runCatching { Virtualizer(0, id).apply { enabled = true } }.getOrNull()
        reverb = runCatching { PresetReverb(0, id).apply { enabled = true } }.getOrNull()
        saved?.let { applySaved(it) }
    }
    fun release() { listOf(eq, bass, virt, reverb).forEach { runCatching { it?.release() } }; eq = null; bass = null; virt = null; reverb = null; sessionId = 0 }

    val eqSupported get() = eq != null
    val bandCount get() = eq?.numberOfBands?.toInt() ?: 0
    val levelRange get() = eq?.bandLevelRange?.let { it[0].toInt() to it[1].toInt() } ?: (-1500 to 1500)   // millibels
    fun bandFreqHz(b: Int) = (eq?.getCenterFreq(b.toShort()) ?: 0) / 1000
    fun bandLevel(b: Int) = eq?.getBandLevel(b.toShort())?.toInt() ?: 0
    fun setBand(b: Int, mb: Int) { eq?.setBandLevel(b.toShort(), mb.toShort()) }
    val bassSupported get() = bass?.strengthSupported == true
    fun setBass(permille: Int) { bass?.setStrength(permille.toShort()) }          // 0..1000
    val virtSupported get() = virt?.strengthSupported == true
    fun setVirtualizer(permille: Int) { virt?.setStrength(permille.toShort()) }
    val reverbSupported get() = reverb != null
    fun setReverb(index: Int) { reverb?.preset = reverbPresets[index] }

    // Presets: levels expressed as fractions (-1..1) of the supported range, mapped to however many bands exist.
    val presets: Map<String, FloatArray> = mapOf(
        "Normal" to floatArrayOf(0f, 0f, 0f, 0f, 0f), "Classical" to floatArrayOf(0.4f, 0.2f, -0.1f, 0.3f, 0.4f),
        "Dance" to floatArrayOf(0.6f, 0.3f, 0f, 0.3f, 0.5f), "Flat" to floatArrayOf(0f, 0f, 0f, 0f, 0f),
        "Folk" to floatArrayOf(0.2f, 0f, 0.1f, 0.3f, 0.1f), "Heavy Metal" to floatArrayOf(0.5f, 0.2f, 0.6f, 0.4f, 0.2f),
        "Hip Hop" to floatArrayOf(0.6f, 0.4f, 0f, 0.1f, 0.3f), "Jazz" to floatArrayOf(0.3f, 0.1f, -0.2f, 0.2f, 0.4f),
        "Pop" to floatArrayOf(-0.2f, 0.2f, 0.4f, 0.2f, -0.2f), "Rock" to floatArrayOf(0.5f, 0.2f, -0.1f, 0.3f, 0.5f))
    fun applyPreset(name: String) {
        val p = presets[name] ?: return; val (lo, hi) = levelRange; val n = bandCount; if (n == 0) return
        for (b in 0 until n) { val f = p[(b * (p.size - 1)) / (n - 1).coerceAtLeast(1)]; setBand(b, (if (f >= 0) f * hi else -f * lo).toInt()) }
    }
    var saved: FxSettings? = null
    data class FxSettings(val bands: List<Int>, val bass: Int, val virt: Int, val reverb: Int)
    private fun applySaved(s: FxSettings) { s.bands.forEachIndexed { i, v -> if (i < bandCount) setBand(i, v) }; setBass(s.bass); setVirtualizer(s.virt); setReverb(s.reverb) }
}
```
Because the service and UI run in the same process, `AudioFx` is a shared singleton. The Equalizer screen **must** hide any section whose `...Supported` flag is false and show "Not supported on this device". Persist settings (bands list, bass, virt, reverb, preset name) in DataStore and set `AudioFx.saved` at app start so it re-applies on attach.

## 5. Equalizer screen requirements
Master on/off switch · preset chips (horizontal scroll) · one vertical slider per real band (label e.g. "250 Hz", range in dB = mb/100) · Bass boost knob/slider · Virtualizer slider · Reverb chips (names above) · Reset button. Changes apply live. Slider state hoisted in a ViewModel; do not write to DataStore on every drag tick (write on `onValueChangeFinished`).

## 6. Ringtone helper
```kotlin
fun setRingtone(ctx: Context, uri: Uri, type: Int /* RingtoneManager.TYPE_RINGTONE | TYPE_NOTIFICATION | TYPE_ALARM */): Boolean {
    if (!Settings.System.canWrite(ctx)) {
        ctx.startActivity(Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:${ctx.packageName}")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); return false }
    return runCatching { RingtoneManager.setActualDefaultRingtoneUri(ctx, type, uri); true }.getOrDefault(false)
}
```
Show a toast ("Ringtone set" / "Couldn't set ringtone"). Content URIs from MediaStore work; SAF URIs may not — then show "Can't use this file as ringtone".

## 7. Saving the queue
In `MainActivity`, `lifecycleScope` collects `player.state` and every 5 s while `isPlaying` plus on pause calls `player.snapshot()` and stores ids/index/position in DataStore. On launch, if the library is loaded, call `restore(...)` with `resolve = { ids -> db.tracks().byIds(ids) ordered as ids }`.

## 8. Acceptance for this part
Song plays from the list; plays with screen off; notification and lock-screen controls work; unplug headset → pauses; sleep timer fades and pauses; speed change works; EQ hides unsupported controls; only one player instance; `brain.md` updated.
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
# 17 — Tests, Recovery Procedures, Step Prompts, Completion Report

> Use with `SPEC_INDEX.md` (file 12 §2). Phase 8 attaches this file plus the files of the phases it audits. Nothing in this pack has been compiled or run by the author; every PASS must come from a real run in your project.

## 1. Test matrix
Unit = JVM (`src/test`), Instrumented = device/emulator (`src/androidTest`). Each row names the test class to create.
| Area (file) | Test class | Type | Must prove |
|---|---|---|---|
| Fades (13) | `AudioTransitionControllerTest` | Unit (fake clock/sink) | effective volume = user × fade; a new fade cancels the old; `restore()` returns to user volume; cancel/error/lifecycle restore; user volume change during a fade is respected |
| Sleep timer (13) | `SleepTimerEngineTest` | Unit | timer fade uses the controller; cancel mid-fade restores volume; end-of-track mode pauses at item end only |
| Error policy (13) | `ErrorPolicyTest` | Unit | one auto retry for generic errors only; MISSING_FILE / NO_PERMISSION / UNSUPPORTED go straight to the error card; skip advances |
| Reconnect (13) | `ControllerReconnectTest` | Instrumented | kill service → controller reconnects with back-off (max 5), UI state recovers, no crash on detach |
| Seek (13) | `SeekGuardTest` | Unit | unknown duration → `seekTo` returns false; ±10 s clamps to [0, duration] |
| Ringtone (13) | `RingtonePendingActionTest` | Instrumented | no WRITE_SETTINGS → pending action stored; on resume it rechecks and completes or reports once |
| Scan (14) | `ScanReconcileTest` | Instrumented (Room in-memory) | interrupted scan leaves availability untouched; empty result with existing library → Partial; success marks unseen unavailable; favourites survive |
| Identity (14) | `IdentityModelTest` | Unit | ids `ms:`/`saf:`/`copy:` stable across size/mtime change; folderPath never used as key |
| Migration (14) | `Migration2To3Test` | Instrumented (`MigrationTestHelper`) | data survives, new columns have defaults, `monthKey` back-filled |
| SAF (14) | `SafScanTest` | Instrumented | unreadable tree → Partial, no row changes; reconcile only per tree |
| Queue (14) | `QueueModelTest` | Unit | shuffle keeps current first; disable shuffle returns to original order successor; seed deterministic; playNext/append/remove/move; no duplicates |
| Queue restore (14) | `QueueRestoreTest` | Unit | current removed → nearest survivor, position 0, paused; empty → null; corrupt JSON ignored |
| Permission (14) | `PermissionStateTest` | Unit | Denied → Granted → PermanentlyDenied → Limited transitions |
| Counts (14) | `VisibleCountTest` | Instrumented | header count equals list size at min-duration 0/30/60 s; COPIED duplicate hidden |
| Artwork (14) | `ArtworkKeyTest` | Unit | modifiedMs change → new key; size buckets 96/192/384/768 |
| Stats (15) | `SessionTrackerTest` | Unit | pause/buffer excluded; speed-aware real vs media time; skip rule; repeat-one loops; <2 s ignored |
| Stats recovery (15) | `SessionRecoveryTest` | Instrumented | checkpoint present at start → one event written once |
| Months (15) | `MonthKeyTest` | Unit | zone change after write never moves the event |
| Backup validate (15) | `FullBackupValidationTest` | Unit | each hostile archive in file 15 §3 rejected with zero writes; sample `.wav` accepted |
| Backup restore (15) | `FullBackupRestoreTest` | Instrumented | preview diff counts; Merge idempotent; Replace needs confirm; failure rolls back; no snapshot DB is ever opened |
| Manifest (15) | `OwnExportRoundTripTest` | Instrumented | export → self-check → import verifies SHA-256; one flipped byte fails |
| Classic backup (07) | `ClassicBackupTest` | Unit | JSON round-trip; matcher never guesses ambiguous |
| LRC (16) | `LrcParserTest` | Unit | multi-stamp lines, `.5`/`.50`/`.500` fractions, metadata tags ignored, binary search active line, plain text → null |
| Blur (16) | `BoxBlurTest` | Unit | uniform stays uniform; large radius clamps; no crash on 1×1 |
| Immersive UI (16) | `ImmersivePlayerTest` | Compose UI test | theme switch keeps playback; queue row marked current; disabled seek at unknown duration; tablet layout shows permanent panel |
| Themes (06/16) | `ThemeRegistryTest` | Unit | exactly 9 ids in the canonical order; unknown id → classic |

**Fixtures:** `sample/ManualBackup_20261028_172204175.wav` is the user's personal file — keep it out of public repos; tests read it from a local-only path and are skipped (`assumeTrue`) when absent. Hostile archives are built in code inside the test with `ZipOutputStream`.

## 2. Manual device checklist (on the Infinix 6.78" phone and, later, the tablet)
1. Fresh install → permission Denied screen → Allow → library appears; deny twice → PermanentlyDenied screen with **Open settings**.
2. Scan, then swipe the app away mid-scan → reopen: library unchanged.
3. Play, background, lock screen; notification and headset buttons pause/play with a fade; no volume jump after 20 pause/play taps in a row.
4. Sleep timer 1 min: fades, pauses, volume afterwards equals the slider.
5. Shuffle on/off during playback: no restart of the current song.
6. Force-stop during playback → relaunch: queue restored **paused** on the same song.
7. Import the sample `.wav` (Full backup) → preview → Merge → counts; import again → "0 changes".
8. Export a Full backup, then import it back; export Classic JSON and import it back.
9. Cycle all 9 player themes while a song plays; Immersive in portrait, landscape, split-screen; TalkBack announces buttons and "Now playing".
10. Turn animations off in Developer options → no marquee, no crossfade.
11. Airplane mode, SD card removed/inserted, permission revoked from Settings while the app is open.

## 3. Recovery procedures (implement and test each)
| Failure | Required behaviour |
|---|---|
| **Interrupted scan** | Next launch shows `Partial(Cancelled)` banner with **Rescan**; library unchanged; `scan_seen` cleared in `finally`. |
| **Failed migration** | Before opening Room at a new version, copy `pulse.db` (+ `-wal`, `-shm`) to `pulse.db.bak-v<old>`. If Room throws on open, show a recovery screen: **Restore previous database** (copies the backup back, keeps the old app version behaviour) or **Export what is readable** (Classic JSON of favourites if readable) — never `fallbackToDestructiveMigration()`. |
| **Failed restore (Full)** | Transaction rolled back, message "Nothing was changed", temp copy deleted, preview kept so the user can retry or change categories. |
| **Failed export** | Partial output deleted; temp file deleted; error names the step (write / self-check). |
| **Corrupt queue snapshot** | Ignore and delete the key; start with an empty queue; no crash. |
| **DataStore corruption** | Provide `corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() }`; defaults apply (theme classic, EQ flat); the user is told "Settings were reset". |
| **Lost SAF permission** | Tree marked `Partial(StorageUnavailable)`, banner "Re-select this folder"; its rows stay (available as of the last good scan). |
| **Playback error** | File 13 policy: one retry, then error card with **Retry / Skip**; three consecutive failures pause playback with a summary instead of looping through the queue. |
| **Process death mid-listen** | `SessionTracker.recoverOpenSession()` runs at service/app start (≤ 15 s lost). |
| **Low storage** | Export/copy check free space first and refuse with a message; the database is never written below 50 MB free. |

## 4. Step prompts (task-sized; continue after prompt 13 of file 09)
Every prompt starts with: *"Attach `SPEC_INDEX.md` and the files listed. Read only those. If a phase file and a fix-pack file disagree, the fix-pack file wins. Change at most 6 files. Finish with the CHANGED FILES manifest (file 12 §5). Never write PASS for something you did not run."*

**PROMPT 14 — Phase 0 audit (files 00, 12).** "Audit the project against the Spec Index. List missing/incorrect pieces per phase. Do not change code. Output a table: item, file, phase, severity."

**PROMPT 15 — Schema and identity (03, 14 §1–§2).** "Add the identity columns, `scan_seen`, `lyrics`, play-event fields, version 3 and `MIGRATION_2_3`, pre-migration DB copy, exported schema JSON, `Migration2To3Test`, `IdentityModelTest`. Put SDK numbers in the version catalog (file 12 §6.1)."

**PROMPT 16 — Recoverable scanner (14 §1, §3, §5).** "Implement `ScanState`, `ScanDao`, `LibraryScanner.scanDevice`, banners, the mount/unmount receiver and `ScanReconcileTest`. Remove every other writer of `available = 0`."

**PROMPT 17 — SAF scan, copy, permissions (14 §4, §8).** "Implement `scanTree`, the explicit Copy flow, `PermissionMonitor`, the permission screens and tests."

**PROMPT 18 — Counts and artwork (14 §9–§10).** "Implement the shared `VISIBLE` query constant, Missing songs chip, `ArtRef` + bucketed `ArtworkLoader` keys, `onTrimMemory`, tests."

**PROMPT 19 — Audio transition controller (13 §1–§3).** "Implement `AudioTransitionController`, `FadingPlayer`, `SleepTimerEngine`; make the service build the player through `FadingPlayer`; add the three unit tests. Grep must show no other `player.volume =` writer."

**PROMPT 20 — Engine, controller lifecycle, errors (13 remainder).** "Implement `PlaybackEngine`, `ErrorPolicy`, `PlayerController` attach/detach/reconnect, seek guards, error card, ringtone pending action, tests."

**PROMPT 21 — Queue model (14 §6–§7).** "Implement `QueueModel`, `reorderAroundCurrent`, snapshot save/restore, notice, `QueueModelTest`, `QueueRestoreTest`. Media3 shuffle must stay disabled."

**PROMPT 22 — Session statistics (15 Part A).** "Implement `StatsPolicy`, `SessionTracker`, listener wiring, recovery, recap queries by `monthKey`, tests. `SessionTracker` is the only writer of `play_events`."

**PROMPT 23 — Classic backup separation (07, 15 Part C).** "Move the Classic codec/screens to `data.backup.classic`, add `sniff` routing and `ClassicBackupTest`."

**PROMPT 24 — Full backup validation (10, 11, 15 §2.1–§2.3).** "Implement `BackupLimits`, entry-name validation, tiered validation, header-only snapshot check, manifest; remove any SQLite-open of archive entries. Tests: `FullBackupValidationTest` including every hostile archive."

**PROMPT 25 — Full backup preview, merge, export (11, 15 §2.4–§2.5).** "Implement `stage()`/`RestorePreview` screen, Merge/Replace, single transaction, safety export, export + self-check, label 'Full backup (reference format)', tests."

**PROMPT 26 — Immersive components (16 §3–§5).** "Add `BlurCache`, `ArtworkBackground`, top bar, `SongInformation`, `PlaybackSeekBar`, controls, secondary actions, `QueueTrackRow`/`QueuePanel`, `BoxBlurTest`."

**PROMPT 27 — Immersive theme assembly and lyrics (16 §2, §6–§9).** "Add `PlayerLayout.IMMERSIVE`, the `PlayerThemes` entry, `ImmersivePlayerLayout`, preview card, `LrcParser`, `LyricsDao`, import, `LrcParserTest`, `ThemeRegistryTest`, `ImmersivePlayerTest`."

**PROMPT 28 — Spec conflicts cleanup (12 §3, §6).** "Apply the README/prompt/label edits, create `docs/TEMP_CODE.md`, confirm 'Nine themes'."

**PROMPT 29 — Final audit (17).** "Run the whole test matrix and the build. Grep for `TODO-TEMP`, `player.volume`, `available = 0`, `openDatabase`, `fallbackToDestructiveMigration`, `shuffleModeEnabled = true`, the word 'compatible' in UI strings. Fill the completion report (§5). Fix nothing silently: list every failure."

## 5. Completion report (the final reply of PROMPT 29, exactly this shape)
```
COMPLETION REPORT
Build:        ./gradlew assembleDebug   → PASS | FAIL (first error)
Lint:         ./gradlew lintDebug       → PASS | FAIL | NOT RUN
Unit tests:   ./gradlew testDebugUnitTest → X passed / Y failed / Z skipped
Device tests: ./gradlew connectedDebugAndroidTest → X / Y / Z | NOT RUN (no device)
Manual checklist (§2): items done 1–11 → PASS | FAIL | NOT DONE (one line each)
Grep gates:   TODO-TEMP=0  player.volume writers=1  available=0 writers=1  openDatabase on archive=0  destructive migration=0  media3 shuffle=0  "compatible" in UI=0
Themes:       9 selectable, default classic
Known gaps:   <list>
Not verified: acceptance of the exported Full backup by the other app
```
Rules: numbers come from real output; "NOT RUN" is allowed, an invented PASS is not; each FAIL lists the exact test/step and the cause if known.

## 6. TEMP_CODE.md usage
Create `docs/TEMP_CODE.md` from file 12 §7 in Phase 0. Any stub gets `// TODO-TEMP(<phase>): <removal condition>` and a table row. PROMPT 29 fails if a row is not marked removed or a tag remains in the source.
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
# 13 — Fix: Coordinated Audio Transitions, Sleep Timer, Controller Lifecycle, Errors, Seek, Ringtone

> Replaces `04_PLAYBACK_AND_AUDIO.md` §2 (service), §3 (controller) and the fade/sleep code in it. `AudioFx` (04 §4), the equalizer screen (04 §5) and the ringtone helper idea (04 §6) stay, except where changed below. Blueprint code: fix imports/signatures for the installed Media3.

## 1. The design in one picture
- The **service side owns everything that changes sound or ends a session**: `AudioTransitionController` (all fades), `SleepTimerEngine`, `ErrorPolicy`, `SessionTracker` (file 15) — held in a process-wide `PlaybackEngine` singleton, because the UI and the service share one process (same pattern as `AudioFx`).
- Every command from every source (app, notification, lock screen, headset button, widget) reaches ExoPlayer through a `FadingPlayer` wrapper handed to `MediaSession`. So **one code path** decides fades; nothing else writes `volume`.
- The UI `PlayerController` is a thin, disposable client: it attaches/detaches with the activity and reconnects if the service dies.
- Volume is **never persisted**. A fresh ExoPlayer always starts at user volume 1.0 with fade multiplier 1.0, so killing the service mid-fade can never leave the player muted.

## 2. AudioTransitionController
```kotlin
package com.pulse.player.playback

enum class TransitionReason(val defaultMs: Long) {
    PlayStart(350), PauseEnd(250), SleepTimer(10_000), AudioFocusLoss(150), AudioFocusGain(300), TrackChange(200) }

/** The ONLY writer of player volume. effective = userVolume * fadeMultiplier. */
class AudioTransitionController(
    private val scope: CoroutineScope,
    private val clock: () -> Long = { SystemClock.elapsedRealtime() },
    private val sink: (Float) -> Unit,                       // sink = { exo.volume = it }
) {
    var userVolume = 1f; private set                         // what the user chose; fades never touch it
    var fadeMultiplier = 1f; private set                     // temporary, 0..1
    var activeReason: TransitionReason? = null; private set
    val effectiveVolume get() = (userVolume * fadeMultiplier).coerceIn(0f, 1f)
    private var job: Job? = null
    private fun publish() = sink(effectiveVolume)

    fun setUserVolume(v: Float) { userVolume = v.coerceIn(0f, 1f); publish() }   // does not restart or cancel a fade

    /** One transition at a time. A new request cancels the old one and starts from the CURRENT audible level. */
    fun fadeTo(target: Float, durationMs: Long, reason: TransitionReason, onDone: (() -> Unit)? = null) {
        job?.cancel()
        val from = fadeMultiplier; val to = target.coerceIn(0f, 1f)
        if (durationMs <= 0 || from == to) { fadeMultiplier = to; activeReason = null; publish(); onDone?.invoke(); return }
        activeReason = reason
        job = scope.launch {
            val start = clock()
            while (true) {
                val t = ((clock() - start) / durationMs.toFloat()).coerceIn(0f, 1f)
                fadeMultiplier = from + (to - from) * (t * t * (3 - 2 * t))   // smoothstep
                publish()
                if (t >= 1f) break
                delay(16)
            }
            activeReason = null
            onDone?.invoke()                                  // only runs if NOT cancelled
        }
    }
    fun snapTo(multiplier: Float) { job?.cancel(); activeReason = null; fadeMultiplier = multiplier.coerceIn(0f, 1f); publish() }
    /** Call on completion, cancel, error, pause, stop, focus loss, service destroy. Always leaves effective == userVolume. */
    fun restore() = snapTo(1f)
}
```
Rules: a fade-out that ends in "pause" passes the pause as `onDone`, so if a newer transition cancels it the pause is **not** executed (no half-faded stop). After any actual pause/stop, call `restore()`.

## 3. FadingPlayer (every command goes through it)
```kotlin
class FadingPlayer(private val raw: ExoPlayer, private val t: AudioTransitionController,
                   private val sleep: SleepTimerEngine, private val fadeEnabled: () -> Boolean) : ForwardingPlayer(raw) {

    override fun play() = startPlayback()
    override fun setPlayWhenReady(playWhenReady: Boolean) { if (playWhenReady) startPlayback() else pause() }

    private fun startPlayback() {
        if (t.activeReason == TransitionReason.SleepTimer) sleep.cancel()           // user wants to keep listening: cancels the timer and restores volume
        if (!fadeEnabled()) { t.restore(); raw.play(); return }
        when {
            t.activeReason == TransitionReason.PauseEnd -> { raw.play(); t.fadeTo(1f, TransitionReason.PlayStart.defaultMs, TransitionReason.PlayStart) } // fade back in from the audible level, never from zero
            !raw.isPlaying -> { t.snapTo(0f); raw.play(); t.fadeTo(1f, TransitionReason.PlayStart.defaultMs, TransitionReason.PlayStart) }
            else -> t.fadeTo(1f, TransitionReason.PlayStart.defaultMs, TransitionReason.PlayStart)
        }
    }
    override fun pause() {
        if (!fadeEnabled() || !raw.isPlaying) { raw.pause(); t.restore(); return }
        t.fadeTo(0f, TransitionReason.PauseEnd.defaultMs, TransitionReason.PauseEnd) { raw.pause(); t.restore() }
    }
    override fun stop() { raw.stop(); t.restore() }
    override fun seekToNextMediaItem() = cut { raw.seekToNextMediaItem() }
    override fun seekToPreviousMediaItem() = cut { raw.seekToPreviousMediaItem() }
    /** Executes the skip immediately (never drops a tap), starting silent to avoid a pop, then fades in. */
    private fun cut(op: () -> Unit) {
        if (!fadeEnabled() || !raw.isPlaying) { op(); return }
        t.snapTo(0f); op(); t.fadeTo(1f, TransitionReason.TrackChange.defaultMs, TransitionReason.TrackChange)
    }
    override fun setVolume(volume: Float) = t.setUserVolume(volume)
    override fun getVolume(): Float = t.userVolume
}
```
If the installed Media3 reports that some `ForwardingPlayer` members are `final`/different, adapt, but keep the rule: **play, pause, setPlayWhenReady, stop, next, previous, volume all go through the controller.**

## 4. Sleep timer (service side, same controller)
```kotlin
class SleepTimerEngine(private val scope: CoroutineScope, private val raw: ExoPlayer,
                       private val t: AudioTransitionController, private val clock: () -> Long = { SystemClock.elapsedRealtime() }) {
    data class State(val remainingMs: Long = 0, val endOfTrack: Boolean = false)
    val state = MutableStateFlow(State())
    private var job: Job? = null
    companion object { const val FADE_MS = 10_000L }

    fun start(minutes: Int) {
        cancel()
        val end = clock() + minutes * 60_000L
        job = scope.launch {
            var faded = false
            while (true) {
                val left = end - clock(); state.value = State(left.coerceAtLeast(0))
                if (left <= 0) break
                if (!faded && left <= FADE_MS) { faded = true; t.fadeTo(0f, left, TransitionReason.SleepTimer) }   // same controller as every other fade
                delay(250)
            }
            raw.pause(); t.restore(); state.value = State()
        }
    }
    /** "End of this track": ExoPlayer itself pauses when the current item ends, so the next song never starts. */
    fun startEndOfTrack() { cancel(); raw.pauseAtEndOfMediaItems = true; state.value = State(endOfTrack = true) }
    fun onPausedAtEndOfItem() { if (state.value.endOfTrack) { raw.pauseAtEndOfMediaItems = false; t.restore(); state.value = State() } }
    fun cancel() { job?.cancel(); job = null; raw.pauseAtEndOfMediaItems = false; t.restore(); state.value = State() }   // volume back to full immediately
}
```
`onPausedAtEndOfItem()` is called from the engine listener when `onPlayWhenReadyChanged(false, PLAY_WHEN_READY_CHANGE_REASON_END_OF_MEDIA_ITEM)` fires. Policy: pressing play while the final fade runs **cancels** the timer (documented in the UI as "Sleep timer cancelled").

## 5. PlaybackEngine and service (replaces 04 §2)
```kotlin
object PlaybackEngine {
    var scope: CoroutineScope? = null; private set
    var exo: ExoPlayer? = null; private set
    lateinit var transitions: AudioTransitionController private set
    lateinit var sleep: SleepTimerEngine private set
    lateinit var errors: ErrorPolicy private set
    lateinit var sessions: SessionTracker private set                       // file 15 §1
    var fadingPlayer: FadingPlayer? = null; private set
    @Volatile var fadeEnabled = true                                          // set from SettingsStore

    fun attach(ctx: Context, db: PulseDb, player: ExoPlayer) {
        detach()
        val s = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate); scope = s; exo = player
        transitions = AudioTransitionController(s) { player.volume = it }
        sleep = SleepTimerEngine(s, player, transitions)
        errors = ErrorPolicy(player)
        sessions = SessionTracker(ctx.applicationContext, db, player)
        fadingPlayer = FadingPlayer(player, transitions, sleep) { fadeEnabled }
        player.addListener(object : Player.Listener {
            override fun onAudioSessionIdChanged(id: Int) { AudioFx.attach(id) }
            override fun onPlayWhenReadyChanged(pwr: Boolean, reason: Int) {
                if (!pwr) transitions.restore()                                 // a stopped/paused player never stays faded (focus loss, noisy, end of item, user)
                if (!pwr && reason == Player.PLAY_WHEN_READY_CHANGE_REASON_END_OF_MEDIA_ITEM) sleep.onPausedAtEndOfItem()
            }
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (isPlaying) { errors.onPlaying(); if (transitions.activeReason == null) transitions.restore() }   // plays that bypassed the wrapper are not faded
            }
            override fun onPlayerError(e: PlaybackException) { errors.onError(e) }
            override fun onEvents(p: Player, events: Player.Events) { WidgetStateStore.write(ctx, p, s) }
        })
        AudioFx.attach(player.audioSessionId)
    }
    fun detach() {
        if (exo == null) return
        transitions.restore()                                                  // before release; also never persisted anywhere
        sessions.closeNow("service-destroyed")
        scope?.cancel(); scope = null
        AudioFx.release(); exo?.release(); exo = null; fadingPlayer = null
    }
}

class MusicPlaybackService : MediaSessionService() {
    private var session: MediaSession? = null
    override fun onCreate() {
        super.onCreate()
        val exo = ExoPlayer.Builder(this)
            .setAudioAttributes(AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MUSIC).build(), true)
            .setHandleAudioBecomingNoisy(true).setWakeMode(C.WAKE_MODE_LOCAL).build()
        PlaybackEngine.attach(this, (application as PulseApp).db, exo)
        session = MediaSession.Builder(this, PlaybackEngine.fadingPlayer!!).build()
    }
    override fun onGetSession(info: MediaSession.ControllerInfo) = session
    override fun onTaskRemoved(rootIntent: Intent?) {
        val p = PlaybackEngine.exo
        if (p == null || !p.playWhenReady || p.mediaItemCount == 0) stopSelf()
    }
    override fun onDestroy() { session?.release(); session = null; PlaybackEngine.detach(); super.onDestroy() }
}
```
Hooks the review asked for → where they are handled: player attached/detached = `attach/detach`; service created/destroyed = `onCreate/onDestroy`; audio focus gained/lost = `onPlayWhenReadyChanged` + `onIsPlayingChanged`; activity stopped = only the UI client detaches, the engine keeps running; user volume = `setVolume → setUserVolume`.

## 6. Error policy (one retry, then an actionable card)
```kotlin
class ErrorPolicy(private val raw: ExoPlayer) {
    enum class Category { MISSING_FILE, NO_PERMISSION, UNSUPPORTED, OTHER }
    data class TrackError(val mediaId: String?, val title: String, val category: Category, val message: String)
    val current = MutableStateFlow<TrackError?>(null)
    private val retries = HashMap<String, Int>()

    private fun category(e: PlaybackException) = when (e.errorCode) {
        PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND -> Category.MISSING_FILE
        PlaybackException.ERROR_CODE_IO_NO_PERMISSION -> Category.NO_PERMISSION
        PlaybackException.ERROR_CODE_DECODER_INIT_FAILED, PlaybackException.ERROR_CODE_DECODING_FAILED,
        PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED, PlaybackException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED -> Category.UNSUPPORTED
        else -> Category.OTHER }
    fun onError(e: PlaybackException) {
        val item = raw.currentMediaItem; val id = item?.mediaId ?: "?"; val cat = category(e)
        val n = retries.getOrDefault(id, 0)
        if (n < 1 && cat == Category.OTHER) { retries[id] = n + 1; raw.prepare(); return }      // at most ONE automatic retry, only for unknown errors
        raw.pause(); PlaybackEngine.transitions.restore()
        current.value = TrackError(id, item?.mediaMetadata?.title?.toString().orEmpty(), cat, when (cat) {
            Category.MISSING_FILE -> "This file can't be found. It may have been moved or the SD card removed."
            Category.NO_PERMISSION -> "Pulse Player can't read this file. Check the audio permission."
            Category.UNSUPPORTED -> "This format can't be played on this device."
            Category.OTHER -> "This track failed to play." })
    }
    fun onPlaying() { raw.currentMediaItem?.mediaId?.let { retries.remove(it) }; current.value = null }
    fun retry() { current.value?.mediaId?.let { retries.remove(it) }; current.value = null; raw.prepare(); raw.play() }
    fun skip() { current.value = null; if (raw.hasNextMediaItem()) { raw.seekToNextMediaItem(); raw.prepare(); raw.play() } }
}
```
No auto-skip loops: after the card appears nothing advances until the user taps **Retry** or **Skip**. UI: an error card above the controls showing track title, message and the two buttons (collected from `PlaybackEngine.errors.current`).

## 7. PlayerController (UI client) with explicit lifecycle (replaces 04 §3 plumbing)
```kotlin
class PlayerController(private val ctx: Context, private val db: PulseDb, private val scope: CoroutineScope) {
    private var future: ListenableFuture<MediaController>? = null
    private var mc: MediaController? = null
    private var tickJob: Job? = null; private var reconnectJob: Job? = null; private var attempts = 0
    private val _state = MutableStateFlow(PlaybackUiState()); val state = _state.asStateFlow()
    private val _pos = MutableStateFlow(0L); val position = _pos.asStateFlow()
    // _queue, sync(), Track.toItem() and the queue commands stay as in file 04 §3 (fades/sleep/logEvent removed)

    fun attach() {                                             // call from Activity.onStart
        if (future != null) return
        val token = SessionToken(ctx, ComponentName(ctx, MusicPlaybackService::class.java))
        val f = MediaController.Builder(ctx, token).setListener(object : MediaController.Listener {
            override fun onDisconnected(controller: MediaController) { onLost() } }).buildAsync()
        future = f
        f.addListener({
            runCatching { f.get() }.onSuccess { c ->
                if (future === f) { mc = c; attempts = 0; c.addListener(listener); sync(c); updateTicker(c.isPlaying) } else c.release()
            }.onFailure { if (future === f) onLost() }
        }, ContextCompat.getMainExecutor(ctx))
        scope.launch { PlaybackEngine.sleep.state.collect { s -> _state.update { it.copy(sleepRemainingMs = s.remainingMs, sleepEndOfTrack = s.endOfTrack) } } }   // see note
    }
    fun detach() {                                             // call from Activity.onStop; playback continues in the service
        reconnectJob?.cancel(); tickJob?.cancel()
        mc?.removeListener(listener); future?.let { MediaController.releaseFuture(it) }; future = null; mc = null
    }
    private fun onLost() { detach(); scheduleReconnect() }
    private fun scheduleReconnect() {
        if (attempts >= 5) { _state.update { it.copy(error = "Lost connection to the player. Reopen the app.") }; return }
        val wait = 500L shl attempts++; reconnectJob = scope.launch { delay(wait); attach() }
    }
    private fun updateTicker(playing: Boolean) {               // runs only while playing and attached
        tickJob?.cancel()
        if (!playing) return
        tickJob = scope.launch { while (isActive) { delay(250); mc?.let { _pos.value = it.currentPosition } } }
    }
    private val listener = object : Player.Listener {
        override fun onEvents(p: Player, e: Player.Events) { sync(p as MediaController); if (e.contains(Player.EVENT_IS_PLAYING_CHANGED)) updateTicker(p.isPlaying) }
    }
}
```
Note: collect the sleep/error flows once per controller (start the collectors in `init`, not in every `attach`) — keep a `Job` and cancel it in a `release()` called from `PulseApp` shutdown, so reconnects don't create duplicate collectors. `PulseApp` creates the controller but no longer connects in `init`. `MainActivity`: `onStart { app.player.attach() }`, `onStop { app.player.detach() }`.
`togglePlay/next/previous` simply call `mc.play()/pause()/seekToNext…`; the service-side `FadingPlayer` does the fades. Remove `fadeEnabled`, `ramp`, sleep code and `listenedMs` from the controller. Settings → `PlaybackEngine.fadeEnabled`.

## 8. Seek handling
```kotlin
// PlaybackUiState gets: val canSeek: Boolean = false   (sync(): c.isCurrentMediaItemSeekable && c.duration != C.TIME_UNSET && c.duration > 0)
fun seekTo(ms: Long): Boolean {
    val c = mc ?: return false; val d = c.duration
    if (!c.isCurrentMediaItemSeekable || d == C.TIME_UNSET || ms < 0 || ms > d) return false      // reject invalid targets
    c.seekTo(ms); _pos.value = ms; return true
}
fun skipBy(deltaMs: Long): Boolean {
    val c = mc ?: return false; val d = c.duration; if (d == C.TIME_UNSET) return false
    return seekTo((c.currentPosition + deltaMs).coerceIn(0, d))                                    // relative seeks clamp, then validate
}
```
UI: when `!state.canSeek`, show the position only, disable the slider and the −10 s/+10 s buttons (with a content description "Seeking unavailable"); the seek bar never divides by a zero duration.

## 9. Ringtone as a pending user action (replaces the Boolean helper in 04 §6)
```kotlin
sealed interface RingtoneResult {
    data object Done : RingtoneResult
    data class NeedsPermission(val uri: String, val type: Int) : RingtoneResult     // the user was sent to the system screen: NOT a failure
    data class Failed(val reason: String) : RingtoneResult
}
fun trySetRingtone(ctx: Context, uri: Uri, type: Int): RingtoneResult {
    if (!Settings.System.canWrite(ctx)) {
        ctx.startActivity(Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:${ctx.packageName}")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        return RingtoneResult.NeedsPermission(uri.toString(), type) }
    return runCatching { RingtoneManager.setActualDefaultRingtoneUri(ctx, type, uri); RingtoneResult.Done }
        .getOrElse { RingtoneResult.Failed("Couldn't use this file as a ringtone.") }
}
```
ViewModel keeps `pendingRingtone` (saved in `SavedStateHandle`) and shows a banner "Allow 'Modify system settings' to finish setting your ringtone · Cancel". In the activity/screen: `LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { vm.recheckPendingRingtone() }`: if pending exists and `canWrite` is now true → apply it, clear it, toast the real result; if still not allowed → keep the banner. A SAF URI that cannot be a ringtone returns `Failed` with that message.

## 10. Acceptance (all must be shown by tests in file 17)
- Starting a track while a pause fade-out runs ends with exactly one fade-in from the audible level; volume never reaches 0 because of it.
- Cancelling the sleep timer during its fade returns effective volume to `userVolume` immediately.
- Destroying the service during a fade leaves nothing persisted; a new service starts at 1.0.
- Rapid next/next/next executes every skip.
- A missing file shows the error card with no skip loop; a transient error retries once.
- `controller.detach()` → no leaked listeners/jobs; killing the service → controller reconnects (≤ 5 tries) and shows an error after that.
- No file other than `AudioTransitionController` assigns `volume`.
# 12 — Fix Pack Overview: Spec Index, Conflicts, Task Sizing

> **Fix pack for Pulse Player (files 12–17). It extends files 00–11 and changes only what is listed here.** It fixes the review findings (audio fades, scanning, queue, backup, statistics, hardening, spec conflicts) and adds one new player theme, **Immersive Artwork Player**, designed from the reference screenshot. Nothing existing is removed.
>
> Read this file first. Then use the **Spec Index** below to know which files each phase needs.

## 1. What the fix pack contains
| File | Covers |
|---|---|
| `12_FIX_SPEC_INDEX_AND_CONFLICTS.md` | This file: index, conflict resolutions, task sizing, temp-code rules |
| `13_FIX_AUDIO_AND_PLAYBACK.md` | Coordinated fades, sleep timer, controller attach/detach/reconnect, error policy, seek, ringtone pending action |
| `14_FIX_SCAN_QUEUE_PERMISSIONS.md` | Recoverable scan, identity model, SD/SAF/copy workflows, queue-order model, permission state, counts, artwork cache |
| `15_FIX_STATS_AND_BACKUP.md` | Session-based statistics and time zones, hardened Full backup, both backups kept separate |
| `16_THEME_IMMERSIVE_ARTWORK_PLAYER.md` | The ninth player theme and its lyrics support |
| `17_TESTS_RECOVERY_AND_PROMPTS.md` | Test matrix, recovery procedures, step prompts, completion report |

Review items are covered as follows: 1.2 → 13; 1.3, 1.6 → 14; 1.4 → 14; 1.5 → 15; 1.7 → 15; 1.8 table → 13/14/15; 1.9 conflicts → this file; 1.10 → 16; missing tests and recovery → 17.

## 2. Spec Index (create `docs/spec/SPEC_INDEX.md` with exactly this content)
Every prompt says "Read the files in the **Required files** column for this phase" — never "read all files". Always attach `SPEC_INDEX.md` plus the listed files. Phases run in order.

| Phase | Goal | Required files (attach all) |
|---|---|---|
| 0 | Audit and plan | `SPEC_INDEX.md`, 00, 12 |
| 1 | Foundation: schema, scan, permissions | 03, 14 |
| 2 | Playback engine: service, fades, queue, sessions | 04, 13, 14 §6–§7, 15 §1 |
| 3 | Theme, shell, main screens | 02, 05, 14 §8–§10 |
| 4 | Player themes and skins (9 themes) | 06, 16 |
| 5 | Classic favourites backup | 07 |
| 6 | Full backup (reference format) | 10, 11, 15 §2–§4 |
| 7 | Widgets, recap, icon, README | 08, 15 §1.4 |
| 8 | Hardening, tests, audit | 17 plus the files of every earlier phase the audit touches |

Rules: (a) if a phase file and a fix-pack file disagree, **the fix-pack file wins**; (b) a phase may only change files listed in its changed-file manifest (§5).

## 3. Conflict resolutions (apply exactly)
| # | Conflict | Resolution |
|---|---|---|
| 1 | "5 player themes A–E" vs "8 in the README" | The pack really defines **8** themes, not the 7 names in the review (Compact, Card-based, … do not exist here). The canonical list is **9 themes** (8 existing + the new one): `classic, mountain, mesh, minimal, acoustic, vinyl, coast, starry, immersive`. "Types A–E" were only layout families used while designing; they are not a count. Every document, code reference and UI string uses the table in §4. |
| 2 | `targetSdk = latest stable` vs hard-coded 35 | Resolve once in `gradle/libs.versions.toml`: `compileSdk`, `targetSdk`, `minSdk = 26` as version-catalog entries used by `app/build.gradle.kts`. Prose says "the SDK versions in the version catalog". Do not hard-code SDK numbers elsewhere. |
| 3 | "Read all files in order" vs "attach only the named files" | Replaced by the Spec Index in §2. |
| 4 | "No placeholder behaviour" vs "create a temporary screen" | Temporary code is allowed only if tagged `// TODO-TEMP(<phase>): <removal condition>` and listed in `docs/TEMP_CODE.md`. The final audit fails if any `TODO-TEMP` remains. Temporary code must still be truthful (no fake data). |
| 5 | Classic vs Full backup mixing | Separate codecs, files, preview flows, screens/cards and tests (file 15 §4). |
| 6 | "Compatible" claimed before interoperability is tested | The full backup is labelled **"Full backup (reference format)"**: *imports the reference format, exports a best-effort file in the same layout; acceptance by the other app is untested.* The word "compatible" must not appear in the UI. File names `10_COMPATIBLE_…` and `11_COMPATIBLE_…` are kept for continuity only. |
| 7 | "Always output complete files" vs huge phases | Tasks are split to the sizes in §5, each with a changed-file manifest and a build checkpoint. |
| 8 | Backup hardening (checksums, manifest) vs the real sample, which has **no** manifest or checksums | Two honest validation tiers, defined in file 15 §2. Reference-format files get structural + CRC + count sanity checks; files written by this app additionally carry a checksum manifest. The sample must still import. |
| 9 | "Never open a database supplied in an archive" vs file 11 opening snapshot `.db` files read-only | Snapshots are **never opened as databases**. They are checked by size and the 16-byte SQLite header only (file 15 §2.3). File 11 §5 is corrected accordingly (see §6 below). |

## 4. Canonical player theme list (single source of truth)
| # | `id` | Name | Layout enum | Preview behaviour | Special badge |
|---|---|---|---|---|---|
| 1 | `classic` | Classic | `CLASSIC` | rounded artwork + glow | no |
| 2 | `mountain` | Mountain Horizon | `SHEET_QUEUE` | sunset canvas + white queue sheet | yes |
| 3 | `mesh` | Mesh Glass | `MESH_GLASS` | magenta→blue mesh, glass card, ±10 s | no |
| 4 | `minimal` | Minimal Mood | `MINIMAL_DARK` | charcoal card layout | no |
| 5 | `acoustic` | Acoustic | `ACOUSTIC` | wood-grain disc | yes |
| 6 | `vinyl` | Vinyl | `VINYL` | record + tonearm | yes |
| 7 | `coast` | Coastal Ocean | `COAST` | animated waves | no |
| 8 | `starry` | Starry Sky | `STARRY` | star field + next-song card | no |
| 9 | `immersive` | Immersive Artwork Player | `IMMERSIVE` | blurred artwork + floating controls + bottom queue panel (file 16) | no |

Rules: the default stays `classic`; theme choice is stored only in the existing `playerThemeId` DataStore key; unknown/missing ids fall back to `classic`; update the README template wording from "8 player themes" to "9 player themes" and file 06's acceptance line to "Nine themes selectable".

## 5. Task sizing (replace "complete files, one phase at a time")
- One prompt = **one task** of at most **6 changed files** and roughly one screen or one subsystem. Prompts in file 17 are already split this way.
- Every task ends with a **changed-file manifest** in this exact form:
```
CHANGED FILES
- app/src/main/java/.../Foo.kt  (new | modified)  — why
BUILD: ./gradlew assembleDebug  → PASS | FAIL (paste the first error)
TESTS: <names run> → PASS | FAIL | NOT RUN
NOT DONE: <list>
```
- If a file is too long for one reply, output it in order as `Foo.kt (part 1/2)`, `(part 2/2)` and say so; never write "rest unchanged" inside a file.
- Never claim PASS for a build or test that was not run. "NOT RUN" is an acceptable, honest value.

## 6. Edits to earlier files (do these in the project, not just in the docs)
1. **03 §1 Gradle**: replace `compileSdk = 35` / `targetSdk = 35` / `minSdk = 26` with `libs.versions.compileSdk.get().toInt()` etc., defined in `gradle/libs.versions.toml` (`compileSdk`, `targetSdk`, `minSdk`).
2. **06**: acceptance line "Eight themes" → "Nine themes"; add the `immersive` entry from file 16 §7 to `PlayerThemes`.
3. **08 §4 README**: "8 player themes" → "9 player themes".
4. **09 prompts**: replace "Attach only the files named" with "Attach `SPEC_INDEX.md` and the files listed for that phase"; temporary screen in PROMPT 1 must carry the `TODO-TEMP` tag.
5. **10/11**: rename the UI label "Full backup (compatible)" → "Full backup (reference format)"; apply the corrected snapshot validation in file 15 §2.3; the export rows use the session fields from file 15 §1 (`wasSkipped` → `skip_count`, `listenedMs` → `play_duration`).
6. **11 §2 migration**: superseded by the single migration in file 14 §2. For a project that has **not** been installed yet, edit the entities directly and delete the old app from the phone; for an installed app use the migration.

## 7. TEMP_CODE.md template
```
# Temporary code (must be empty before release)
| Tag | File | Why it exists | Removal condition | Removed? |
|-----|------|---------------|-------------------|----------|
```

## 8. Global rules added by this fix pack
1. Nothing outside `AudioTransitionController` may write `player.volume` (file 13).
2. Nothing may mark tracks unavailable except the scan **reconcile step** (file 14).
3. Queue order lives in `QueueModel`; Media3's own shuffle flag is never enabled (file 14).
4. Backup import never opens a database from inside an archive (file 15).
5. Play history is written only by `SessionTracker` (file 15).
6. Themes are presentation only: no second player, queue, service or audio session (file 16).
# 11 — Compatible Backup: Code Blueprints + AI Studio Prompts

> Blueprint code for `10_COMPATIBLE_BACKUP_SPEC.md`. Package `com.pulse.player.data.backup`. Adapt imports and signatures to the project; if a snippet doesn't compile, fix it, don't drop the behavior. No new dependencies are needed: only `java.util.zip`, `android.util.JsonReader/JsonWriter`, `android.database.sqlite`, Room (already present).

## 1. Constants, sniffing, safe ZIP access
```kotlin
object Compat {
    const val SETTINGS = "settings.bak"
    const val MUSIC_COVERS = "Covers/MusicCoversBackupConfig.conf"
    const val ALBUM_COVERS = "Covers/AlbumCoversBackupConfig.conf"
    const val TAGS = "MusicTagsBackupConfig.conf"
    const val LYRICS = "Lyrics/LyricsBackupConfig.conf"
    const val PLAYLISTS = "PlayLists/PlayListBackupConfig.conf"
    const val SORT = "SortBackupConfig.conf"
    const val SCAN = "ScanHiddenBackupConfig.conf"
    const val STATS_CFG = "Statistics/StatisticsSnapshotConfig.json"
    const val PLAYBACK = "Statistics/PlaybackRecords.json"
    const val META = "metadata.inf"
    fun snapshot(yyyymm: String) = "Statistics/snapshot_monthly_$yyyymm.db"
    val SNAPSHOT_RE = Regex("^Statistics/snapshot_monthly_(\\d{6})\\.db$")
    val KNOWN = setOf(SETTINGS, MUSIC_COVERS, ALBUM_COVERS, TAGS, LYRICS, PLAYLISTS, SORT, SCAN, STATS_CFG, PLAYBACK)
    val FLAGS = listOf("isBackupMusicTags", "isBackupCovers", "isBackupLyrics", "isBackupPlaylists",
        "isBackupSortSettings", "isBackupScanHideSettings", "isBackupMusicRecap")
    const val MAX_FILE = 512L shl 20; const val MAX_ENTRY = 256L shl 20; const val MAX_TOTAL = 768L shl 20; const val MAX_ENTRIES = 64
}

enum class Sniffed { ZIP, REAL_WAV, JSON, OTHER }

fun sniff(ctx: Context, uri: Uri): Sniffed {
    val h = ByteArray(12); var n = 0
    ctx.contentResolver.openInputStream(uri)?.use { i -> while (n < 12) { val r = i.read(h, n, 12 - n); if (r < 0) break; n += r } }
    fun b(i: Int) = h[i].toInt() and 0xFF
    return when {
        n >= 4 && b(0) == 0x50 && b(1) == 0x4B && ((b(2) == 3 && b(3) == 4) || (b(2) == 5 && b(3) == 6)) -> Sniffed.ZIP
        n >= 12 && String(h, 0, 4, Charsets.ISO_8859_1) == "RIFF" && String(h, 8, 4, Charsets.ISO_8859_1) == "WAVE" -> Sniffed.REAL_WAV
        n >= 1 && (b(0) == '{'.code || b(0) == 0xEF) -> Sniffed.JSON      // 0xEF = UTF-8 BOM; confirm schema later
        else -> Sniffed.OTHER
    }
}

class LimitedStream(inp: InputStream, private val max: Long) : FilterInputStream(inp) {
    private var count = 0L
    private fun add(n: Int) { if (n > 0) { count += n; if (count > max) throw IOException("Backup entry is too large.") } }
    override fun read(): Int = super.read().also { if (it >= 0) add(1) }
    override fun read(b: ByteArray, off: Int, len: Int): Int = super.read(b, off, len).also { add(it) }
}

/** Copies the picked file into a bounded temp file; caller deletes it. */
suspend fun copyToTemp(ctx: Context, uri: Uri): File = withContext(Dispatchers.IO) {
    val f = File.createTempFile("restore_", ".zip", ctx.cacheDir)
    try {
        ctx.contentResolver.openInputStream(uri)!!.use { i -> f.outputStream().use { o ->
            val buf = ByteArray(64 * 1024); var total = 0L
            while (true) { val n = i.read(buf); if (n < 0) break; total += n
                if (total > Compat.MAX_FILE) throw IOException("The file is too large to be a backup.")
                o.write(buf, 0, n); ensureActive() } } }
        f
    } catch (e: Throwable) { f.delete(); throw e }
}

class SafeZip(val file: File) : Closeable {
    val zip = ZipFile(file)
    val names: Set<String>
    init {
        val seen = HashSet<String>(); var total = 0L; var count = 0
        for (e in zip.entries()) {
            if (++count > Compat.MAX_ENTRIES) throw IOException("The archive has too many entries.")
            val n = e.name
            if (n.startsWith("/") || n.contains("\\") || n.contains('\u0000') || n.split('/').any { it == ".." })
                throw IOException("The archive contains an unsafe path.")
            if (!seen.add(n)) throw IOException("The archive has duplicate entries.")
            if (!e.isDirectory) { if (e.size > Compat.MAX_ENTRY) throw IOException("An entry is too large."); total += maxOf(e.size, 0) }
        }
        if (total > Compat.MAX_TOTAL) throw IOException("The archive expands to too much data.")
        names = seen
    }
    fun has(name: String) = zip.getEntry(name) != null
    fun open(name: String): InputStream = LimitedStream(zip.getInputStream(zip.getEntry(name) ?: throw IOException("Missing $name")), Compat.MAX_ENTRY)
    override fun close() = zip.close()
}
```
Recognition rule: after `SafeZip(...)`, it is a backup **only if** `has(Compat.META)` and `names.any { it in Compat.KNOWN }`. Otherwise reject with "This ZIP is not a recognised backup".

## 2. Database changes (Room migration 1 → 2) — do not use destructive migration
```kotlin
// TrackEntity: add  val dataPath: String? = null
// PlaylistEntity: add  val sortKey: String? = null, val sortOrder: Int? = null
// PlayEventEntity: add unique index  indices = [Index("playedAtMs"), Index("trackId"), Index(value = ["trackId", "playedAtMs"], unique = true)]
// @Database(version = 2, ...)

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE tracks ADD COLUMN dataPath TEXT")
        db.execSQL("ALTER TABLE playlists ADD COLUMN sortKey TEXT")
        db.execSQL("ALTER TABLE playlists ADD COLUMN sortOrder INTEGER")
        // remove duplicate events before adding the unique index
        db.execSQL("DELETE FROM play_events WHERE id NOT IN (SELECT MIN(id) FROM play_events GROUP BY trackId, playedAtMs)")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_play_events_trackId_playedAtMs ON play_events(trackId, playedAtMs)")
    }
}
// Room.databaseBuilder(...).addMigrations(MIGRATION_1_2).build()   (exportSchema = true, commit the new schema JSON)
```
Scanner change (file 03 §7): add `MediaStore.Audio.Media.DATA` to the projection, read it into `dataPath`, and fall back to the composed path from spec §7 when blank. Existing rows get `dataPath` on the next **Rescan** (tell the user in the UI: "Rescan once so older entries gain their file path").

New DAO methods:
```kotlin
// TrackDao
@Query("SELECT * FROM tracks WHERE available = 1") suspend fun availableOnce(): List<TrackEntity>
// PlayEvent join for export / dedupe
data class PlaybackRow(val trackId: String, val dataPath: String?, val playedAtMs: Long, val listenedMs: Long, val completed: Boolean)
@Query("SELECT e.trackId AS trackId, t.dataPath AS dataPath, e.playedAtMs AS playedAtMs, e.listenedMs AS listenedMs, e.completed AS completed FROM play_events e JOIN tracks t ON t.id = e.trackId ORDER BY e.playedAtMs") suspend fun playbackRows(): List<PlaybackRow>   // StatsDao
@Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertEvents(items: List<PlayEventEntity>): List<Long>   // StatsDao; rowId -1 = ignored duplicate
@Query("DELETE FROM play_events") suspend fun clearEvents()                                                       // StatsDao
// PlaylistDao
@Query("SELECT * FROM playlists WHERE name = :name COLLATE NOCASE LIMIT 1") suspend fun byName(name: String): PlaylistEntity?
@Query("UPDATE playlists SET sortKey = :key, sortOrder = :order WHERE id = :id") suspend fun setSort(id: Long, key: String?, order: Int?)
@Query("SELECT trackId FROM playlist_tracks WHERE playlistId = :id ORDER BY position") suspend fun trackIds(id: Long): List<String>
```

## 3. Streaming JSON helpers
```kotlin
fun readValue(r: JsonReader): Any? = when (r.peek()) {
    JsonToken.NULL -> { r.nextNull(); null }
    JsonToken.BOOLEAN -> r.nextBoolean()
    JsonToken.NUMBER -> r.nextString().let { it.toLongOrNull() ?: it.toDoubleOrNull() }
    JsonToken.STRING -> r.nextString()
    JsonToken.BEGIN_ARRAY -> { val l = ArrayList<Any?>(); r.beginArray(); while (r.hasNext()) l += readValue(r); r.endArray(); l }
    JsonToken.BEGIN_OBJECT -> { val m = LinkedHashMap<String, Any?>(); r.beginObject(); while (r.hasNext()) m[r.nextName()] = readValue(r); r.endObject(); m }
    else -> { r.skipValue(); null }
}
/** Calls [onItem] for each object of a top-level JSON array, one at a time. Throws on non-array input. */
fun InputStream.forEachObject(onItem: (Map<String, Any?>) -> Unit): Int {
    var n = 0
    JsonReader(InputStreamReader(this, Charsets.UTF_8)).use { r ->
        r.beginArray()
        while (r.hasNext()) { @Suppress("UNCHECKED_CAST") onItem(readValue(r) as? Map<String, Any?> ?: throw IOException("Unexpected array item")); n++ }
        r.endArray()
    }
    return n
}
fun InputStream.readObject(): Map<String, Any?> = JsonReader(InputStreamReader(this, Charsets.UTF_8)).use { @Suppress("UNCHECKED_CAST") (readValue(it) as? Map<String, Any?> ?: throw IOException("Expected a JSON object")) }
fun Map<String, Any?>.str(k: String) = this[k] as? String
fun Map<String, Any?>.int(k: String, d: Int = 0) = (this[k] as? Number)?.toInt() ?: d
fun Map<String, Any?>.lng(k: String, d: Long = 0) = (this[k] as? Number)?.toLong() ?: d
```

## 4. Path keys and the matcher (spec §5)
```kotlin
object PathKey {
    private val volume = Regex("^/(storage/emulated/\\d+|storage/[0-9A-Fa-f]{4}-[0-9A-Fa-f]{4}|sdcard|mnt/sdcard)/")
    fun nfc(s: String) = Normalizer.normalize(s.trim(), Normalizer.Form.NFC).lowercase(Locale.ROOT)
    fun abs(p: String) = nfc(p)
    fun rel(p: String) = nfc(volume.replaceFirst(p.trim(), ""))
    fun name(p: String) = nfc(p.substringAfterLast('/'))
    fun parentName(p: String): String { val s = p.trim().split('/'); return nfc(s.takeLast(2).joinToString("/")) }
}
fun unk(a: String?) = if (a.isNullOrBlank() || a == "<unknown>") "Unknown artist" else a
fun unkAlbum(a: String?) = if (a.isNullOrBlank() || a == "<unknown>") "Unknown album" else a

data class SrcTag(val path: String, val title: String, val artist: String, val album: String)
sealed interface Resolve {
    data class Hit(val track: TrackEntity, val tier: Int) : Resolve
    data object Ambiguous : Resolve
    data object Missing : Resolve
}

class LibraryIndex(tracks: List<TrackEntity>) {
    private val paths = tracks.filter { !it.dataPath.isNullOrBlank() }
    private val byAbs = paths.groupBy { PathKey.abs(it.dataPath!!) }
    private val byRel = paths.groupBy { PathKey.rel(it.dataPath!!) }
    private val byParent = paths.groupBy { PathKey.parentName(it.dataPath!!) }
    private val byName = paths.groupBy { PathKey.name(it.dataPath!!) }
    private val byMeta = tracks.groupBy { Triple(it.normTitle, it.normArtist, it.normAlbum) }

    fun resolve(srcPath: String, tag: SrcTag?): Resolve {
        val tiers = listOf(
            1 to byAbs[PathKey.abs(srcPath)], 2 to byRel[PathKey.rel(srcPath)], 3 to byParent[PathKey.parentName(srcPath)],
            4 to tag?.let { byMeta[Triple(norm(it.title), norm(unk(it.artist)), norm(unkAlbum(it.album)))] },
            5 to byName[PathKey.name(srcPath)])
        for ((tier, list) in tiers) {
            if (list == null || list.isEmpty()) continue
            return if (list.size == 1) Resolve.Hit(list[0], tier) else Resolve.Ambiguous   // never guess among several
        }
        return Resolve.Missing
    }
}
```
`norm()` is the helper from file 03 §6. Build **one** `LibraryIndex` per restore and one `Map<String, SrcTag>` (key `PathKey.abs(path)`) from the tags file.

## 5. Parsing the archive into a staged model
```kotlin
data class SrcPlaylist(val name: String, val sortKey: String?, val sortOrder: Int?, val paths: List<String>)
data class SrcEvent(val path: String, val timestamp: Long, val durationMs: Long, val skipCount: Int)
data class StagedBackup(
    val meta: Map<String, Any?>, val flags: Map<String, Boolean>,
    val tagCount: Int, val tags: Map<String, SrcTag>,
    val playlists: List<SrcPlaylist>, val events: List<SrcEvent>,
    val sortRaw: Map<String, Any?>?, val excludeDirs: List<String>, val hiddenFlagged: Int,
    val lyricsNonEmpty: Boolean, val snapshotsOk: Int, val snapshotsBad: Int, val warnings: List<String>)

suspend fun stage(ctx: Context, zip: SafeZip): StagedBackup = withContext(Dispatchers.IO) {
    val warnings = ArrayList<String>()
    val meta = zip.open(Compat.META).use { it.readObject() }
    val flags = Compat.FLAGS.associateWith { (meta["settings"] as? Map<*, *>)?.get(it) as? Boolean ?: false }
    val tags = HashMap<String, SrcTag>(); var tagCount = 0
    if (zip.has(Compat.TAGS)) tagCount = zip.open(Compat.TAGS).use { s -> s.forEachObject { o ->
        val p = o.str("_data") ?: return@forEachObject
        tags[PathKey.abs(p)] = SrcTag(p, o.str("title").orEmpty(), unk(o.str("artist")), unkAlbum(o.str("album"))) } }
    val playlists = ArrayList<SrcPlaylist>()
    if (zip.has(Compat.PLAYLISTS)) zip.open(Compat.PLAYLISTS).use { s -> s.forEachObject { o ->
        val name = o.str("name")?.trim().orEmpty(); if (name.isEmpty()) return@forEachObject
        val sort = o["sort"] as? Map<*, *>
        val paths = (o["musics"] as? List<*>)?.mapNotNull { (it as? Map<*, *>)?.get("path") as? String }.orEmpty()
        playlists += SrcPlaylist(name, sort?.get("key") as? String, (sort?.get("order") as? Number)?.toInt(), paths) } }
    val events = ArrayList<SrcEvent>()
    if (zip.has(Compat.PLAYBACK)) zip.open(Compat.PLAYBACK).use { s -> s.forEachObject { o ->
        val p = o.str("_data") ?: return@forEachObject; val ts = o.lng("timestamp"); if (ts <= 0) return@forEachObject
        events += SrcEvent(p, ts, o.lng("play_duration"), o.int("skip_count")) } }
    val sortRaw = if (zip.has(Compat.SORT)) zip.open(Compat.SORT).use { it.readObject() } else null
    var exclude = emptyList<String>(); var hidden = 0
    if (zip.has(Compat.SCAN)) zip.open(Compat.SCAN).use { s -> val o = s.readObject()
        exclude = (o["exclude"] as? List<*>)?.mapNotNull { (it as? Map<*, *>)?.get("data") as? String }.orEmpty()
        hidden = (o["hidden_songs"] as? List<*>)?.count { ((it as? Map<*, *>)?.get("is_hidden") as? Number)?.toInt() == 1 } ?: 0 }
    var lyricsNonEmpty = false
    if (zip.has(Compat.LYRICS)) zip.open(Compat.LYRICS).use { lyricsNonEmpty = it.forEachObject { } > 0 }
    var ok = 0; var bad = 0   // snapshots are NEVER opened as databases (file 15 §2.3): size + 16-byte header only
    zip.names.filter { Compat.SNAPSHOT_RE.matches(it) }.forEach { n ->
        val e = zip.entry(n)
        if (e != null && checkSnapshotEntry(zip.file, e)) ok++ else { bad++; warnings += "Snapshot $n failed the header check and was ignored." } }
    StagedBackup(meta, flags, tagCount, tags, playlists, events, sortRaw, exclude, hidden, lyricsNonEmpty, ok, bad, warnings)
}
```
Snapshots are header-checked and counted only; their data is restored from `PlaybackRecords.json` (spec §4, file 15 §2.3). Adapt `zip.entry/zip.file` to the `SafeZip` wrapper API.

## 6. Dry-run preview and apply (one transaction)
```kotlin
data class MatchStats(var tier: IntArray = IntArray(6), var ambiguous: Int = 0, var missing: Int = 0)
data class RestoreOptions(val categories: Set<String>, val replace: Boolean)   // category names = Compat.FLAGS entries
data class RestoreReport(val lines: List<String>, val unmatchedPaths: List<String>)

private val FAV_NAMES = setOf("my favourite", "my favorite", "my favourites", "my favorites")

suspend fun applyRestore(ctx: Context, db: PulseDb, settings: SettingsStore, s: StagedBackup, o: RestoreOptions): RestoreReport = withContext(Dispatchers.IO) {
    val idx = LibraryIndex(db.tracks().availableOnce())
    val lines = ArrayList<String>(); val unmatched = LinkedHashSet<String>(); val stats = MatchStats()
    fun resolve(path: String): TrackEntity? = when (val r = idx.resolve(path, s.tags[PathKey.abs(path)])) {
        is Resolve.Hit -> { stats.tier[r.tier]++; r.track }
        Resolve.Ambiguous -> { stats.ambiguous++; unmatched += "AMBIGUOUS  $path"; null }
        Resolve.Missing -> { stats.missing++; unmatched += "NOT FOUND  $path"; null } }
    val now = System.currentTimeMillis()

    // ---- stage (no DB writes yet) ----
    val doPlaylists = "isBackupPlaylists" in o.categories
    val favIds = ArrayList<String>(); val plans = ArrayList<Triple<SrcPlaylist, List<String>, Int>>()
    if (doPlaylists) for (p in s.playlists) {
        val ids = p.paths.mapNotNull { resolve(it)?.id }.distinct()
        if (p.name.lowercase(Locale.ROOT) in FAV_NAMES) favIds += ids else plans += Triple(p, ids, p.paths.size - ids.size) }
    val doStats = "isBackupMusicRecap" in o.categories
    val events = if (doStats) s.events.mapNotNull { e -> resolve(e.path)?.let { t ->
        PlayEventEntity(trackId = t.id, playedAtMs = e.timestamp, listenedMs = e.durationMs.coerceAtLeast(0), completed = e.skipCount == 0) } } else emptyList()

    // ---- commit once ----
    db.withTransaction {
        if (doPlaylists) {
            if (o.replace) db.favorites().clear()
            val base = now
            db.favorites().addAll(favIds.mapIndexed { i, id -> FavoriteEntity(id, base - i) })      // array order = newest-first display order
            lines += "Favourites: ${favIds.size} resolved"
            for ((p, ids, _) in plans) {
                val existing = db.playlists().byName(p.name)
                val pid = existing?.id ?: db.playlists().create(PlaylistEntity(name = p.name, createdAtMs = now))
                if (existing != null && o.replace) db.playlists().clearTracks(pid)
                var pos = db.playlists().nextPos(pid)
                for (id in ids) db.playlists().addTrack(PlaylistTrackEntity(pid, id, pos++, now))   // IGNORE on duplicates
                db.playlists().setSort(pid, p.sortKey, p.sortOrder) }
            lines += "Playlists: ${plans.size} processed"
        }
        if (doStats) {
            if (o.replace) db.stats().clearEvents()
            val inserted = db.stats().insertEvents(events).count { it != -1L }
            lines += "Play history: $inserted new, ${events.size - inserted} already present"
        }
    }
    // ---- settings (DataStore, after the DB commit) ----
    if ("isBackupSortSettings" in o.categories && s.sortRaw != null) { settings.saveSortRaw(s.sortRaw); lines += "Sort settings applied" }
    if ("isBackupScanHideSettings" in o.categories) {
        val rel = s.excludeDirs.map { PathKey.rel(it).trim('/') }.filter { it.isNotEmpty() }
        settings.addExcludedFolders(rel.toSet()); lines += "Excluded folders added: ${rel.size} (rescan to apply)"
        if (s.hiddenFlagged > 0) lines += "Hidden songs not supported: ${s.hiddenFlagged} skipped" }
    if ("isBackupMusicTags" in o.categories) lines += "Tags: ${s.tagCount} read and used for matching (not written)"
    if ("isBackupCovers" in o.categories) lines += "Covers not applied: they are specific to the source device"
    if ("isBackupLyrics" in o.categories) lines += if (s.lyricsNonEmpty) "Lyrics skipped: format not recognised" else "Lyrics: none in backup"
    lines += "Matched by path ${stats.tier[1]}, relative path ${stats.tier[2]}, folder+name ${stats.tier[3]}, tags ${stats.tier[4]}, name ${stats.tier[5]}; ambiguous ${stats.ambiguous}; not found ${stats.missing}"
    RestoreReport(lines + s.warnings, unmatched.toList())
}
```
Notes for the model: `resolve()` is called once per path occurrence, so counts are per reference. Add to `SettingsStore`: `saveSortRaw(Map<String, Any?>)` (store the JSON string under key `sort_raw`; apply `All_Song` to the Songs-tab sort: `dateAdded→DATE_ADDED`, `title`, `artist`, `duration`; unknown keys are kept raw only) and `addExcludedFolders(Set<String>)`. The preview uses the same staging and a **dry-run** resolve with no writes (extract the stage/resolve part into a function used by both).

## 7. Export (writer)
```kotlin
data class ExportCats(val tags: Boolean, val covers: Boolean, val lyrics: Boolean, val playlists: Boolean, val sort: Boolean, val scan: Boolean, val recap: Boolean)

suspend fun exportCompat(ctx: Context, db: PulseDb, settings: SettingsStore, cats: ExportCats, dest: Uri, onStage: (String) -> Unit): Result<File> = withContext(Dispatchers.IO) { runCatching {
    val tmp = File.createTempFile("backup_", ".zip", ctx.cacheDir)
    val snapFiles = ArrayList<File>()
    try {
        val tracks = db.tracks().availableOnce()
        val withPath = tracks.filter { !it.dataPath.isNullOrBlank() }
        val warnings = ArrayList<String>()
        if (withPath.size < tracks.size) warnings += "${tracks.size - withPath.size} tracks have no file path yet; rescan the library, then back up again."
        val fav = db.favorites().allTracksOnce()
        val lists = db.playlists().observePlaylists().first()
        val rows = db.stats().playbackRows().filter { !it.dataPath.isNullOrBlank() }
        val zone = ZoneId.systemDefault(); val nowYm = YearMonth.now(zone)
        ZipOutputStream(BufferedOutputStream(FileOutputStream(tmp))).use { z ->
            fun entry(name: String, write: (OutputStream) -> Unit) { onStage(name); z.putNextEntry(ZipEntry(name)); write(z); z.closeEntry() }
            val compact = setOf(Compat.TAGS, Compat.MUSIC_COVERS, Compat.ALBUM_COVERS, Compat.PLAYLISTS, Compat.PLAYBACK)   // the sample writes these without indentation
            fun json(name: String, w: JsonWriter.() -> Unit) = entry(name) { o ->
                JsonWriter(OutputStreamWriter(o, Charsets.UTF_8)).apply { if (name !in compact) setIndent("  "); w(); flush() } }   // flush, never close (keeps the ZIP open)
            val ver = ctx.packageManager.getPackageInfo(ctx.packageName, 0)
            val verName = ver.versionName ?: "1.0.0"; val verCode = if (Build.VERSION.SDK_INT >= 28) ver.longVersionCode.toInt() else @Suppress("DEPRECATION") ver.versionCode
            val deviceId = settings.installId()          // random per-install id, NOT ANDROID_ID/IMEI
            val flagMap = mapOf("isBackupMusicTags" to cats.tags, "isBackupCovers" to cats.covers, "isBackupLyrics" to cats.lyrics,
                "isBackupPlaylists" to cats.playlists, "isBackupSortSettings" to cats.sort, "isBackupScanHideSettings" to cats.scan, "isBackupMusicRecap" to cats.recap)

            json(Compat.SETTINGS) { beginObject(); flagMap.forEach { (k, v) -> name(k).value(v) }; name("device").value(deviceId); name("versionName").value(verName); name("versionCode").value(verCode.toLong()); endObject() }
            if (cats.covers) {
                json(Compat.MUSIC_COVERS) { beginArray(); withPath.forEach { beginObject(); name("_data").value(it.dataPath); name("resource").nullValue(); name("cover_url").nullValue(); endObject() }; endArray() }
                json(Compat.ALBUM_COVERS) { beginArray(); withPath.map { it.album }.distinct().forEach { beginObject(); name("album").value(it); name("album_art").nullValue(); name("resource").nullValue(); name("modified_album_art").nullValue(); endObject() }; endArray() } }
            if (cats.tags) json(Compat.TAGS) { beginArray(); withPath.forEach { t -> beginObject(); name("_data").value(t.dataPath); name("title").value(t.title)
                name("artist").value(if (t.artist == "Unknown artist") "<unknown>" else t.artist); name("album").value(t.album); name("track").value(t.trackNumber.toLong()); name("genres").nullValue(); endObject() }; endArray() }
            if (cats.lyrics) json(Compat.LYRICS) { beginArray(); endArray() }
            if (cats.playlists) json(Compat.PLAYLISTS) { beginArray()
                fun pl(name: String, key: String, order: Int, paths: List<String>) { beginObject(); name("name").value(name); name("sort").beginObject().name("key").value(key).name("order").value(order.toLong()).endObject()
                    name("cover").nullValue(); name("musics").beginArray(); paths.forEach { beginObject(); name("path").value(it); endObject() }; endArray(); endObject() }
                pl("My Favourite", "playlistPlayOrder", -1, fav.mapNotNull { it.dataPath })
                for (p in lists) pl(p.name, p.sortKey ?: "playlistPlayOrder", p.sortOrder ?: -1, db.tracks().byIds(db.playlists().trackIds(p.id)).let { m -> val by = m.associateBy { it.id }; db.playlists().trackIds(p.id).mapNotNull { by[it]?.dataPath } })
                endArray() }
            if (cats.sort) json(Compat.SORT) { val raw = settings.sortRawOrNull(); beginObject()
                for (k in listOf("All_Song", "Directory_", "Folder_Song", "Album_", "Album_Song", "Artist_", "Artist_Song", "Genre_", "Genre_Song")) {
                    val (key, order) = settings.sortFor(k, raw)                      // known keys from our prefs, others from raw, else the sample defaults
                    name(k).beginObject().name("key").value(key).name("order").value(order.toLong()).endObject() }
                endObject() }
            if (cats.scan) json(Compat.SCAN) { beginObject(); name("size").value(-1L); name("duration").value(-1L); name("device").value(deviceId)
                name("exclude").beginArray(); settings.excludedFoldersNow().forEachIndexed { i, rel -> beginObject(); name("_id").value((i + 1).toLong()); name("data").value("/storage/emulated/0/" + rel.trim('/')); name("type").value(1L); endObject() }; endArray()
                name("hidden_songs").beginArray(); withPath.forEach { beginObject(); name("path").value(it.dataPath); name("is_hidden").value(0L); endObject() }; endArray(); endObject() }
            if (cats.recap) {
                val months = rows.groupBy { YearMonth.from(Instant.ofEpochMilli(it.playedAtMs).atZone(zone)) }.filterKeys { it < nowYm }
                for ((ym, list) in months.toSortedMap()) { val f = buildSnapshot(ctx, ym, list, Build.MODEL); snapFiles += f
                    entry(Compat.snapshot("%04d%02d".format(ym.year, ym.monthValue))) { o -> f.inputStream().use { it.copyTo(o) } } }
                json(Compat.STATS_CFG) { beginObject(); name("snapshots").beginArray()
                    months.keys.sorted().forEach { ym -> val v = "%04d%02d".format(ym.year, ym.monthValue); beginObject(); name("type").value("monthly"); name("value").value(v); name("filename").value("snapshot_monthly_$v.db"); endObject() }
                    endArray(); name("initApplicationTimestamp").value(rows.firstOrNull()?.playedAtMs ?: System.currentTimeMillis()); endObject() }
                json(Compat.PLAYBACK) { beginArray(); rows.forEach { r -> val d = Instant.ofEpochMilli(r.playedAtMs).atZone(zone).toLocalDate()
                    beginObject(); name("song_id").value(r.trackId.substringAfter(':').toLongOrNull() ?: (r.trackId.hashCode().toLong() and 0x7fffffff))
                    name("timestamp").value(r.playedAtMs); name("day").value("%04d%02d%02d".format(d.year, d.monthValue, d.dayOfMonth)); name("looper_count").value(0L)
                    name("source_type").value(""); name("source_id").value(""); name("play_duration").value(r.listenedMs); name("play_count").value(1L); name("device").value(Build.MODEL)
                    name("skip_count").value(if (r.completed) 0L else 1L); for (i in 1..3) name("extra_int_$i").value(0L); for (i in 1..3) name("extra_text_$i").nullValue()
                    name("_data").value(r.dataPath); endObject() }; endArray() } }
            val stamp = SimpleDateFormat("yyyyMMdd_HHmmssSSS", Locale.US).format(Date())
            json(Compat.META) { beginObject(); name("backupTime").value(stamp); name("mode").value("MANUAL_GLOBAL"); name("type").value("GLOBAL"); name("isAutoBackup").value(false)
                name("deviceId").value(deviceId); name("versionName").value(verName); name("versionCode").value(verCode.toLong())
                name("settings").beginObject(); flagMap.forEach { (k, v) -> name(k).value(v) }; endObject()
                name("songs").value(withPath.size.toLong()); name("playlists").value((lists.size + 1).toLong()); endObject() }
        }
        // ---- verify the finished file BEFORE copying it out ----
        SafeZip(tmp).use { v -> check(v.has(Compat.META)) { "Verification failed: metadata missing." }
            v.open(Compat.META).use { it.readObject() }
            if (cats.playlists) check(v.has(Compat.PLAYLISTS)); if (cats.recap) check(v.has(Compat.PLAYBACK)) }
        ctx.contentResolver.openOutputStream(dest, "wt")!!.use { out -> tmp.inputStream().use { it.copyTo(out) }; out.flush() }
        val copied = ctx.contentResolver.openInputStream(dest)!!.use { i -> var n = 0L; val b = ByteArray(65536); while (true) { val r = i.read(b); if (r < 0) break; n += r }; n }
        check(copied == tmp.length()) { "The saved file size does not match; the backup may be incomplete." }
        tmp
    } finally { snapFiles.forEach { it.delete() } }     // keep tmp until the caller reads its size; caller deletes it
} }
```
`buildSnapshot(ctx, ym, rows, model)`: create `File.createTempFile("snap_", ".db", ctx.cacheDir)`, open with `SQLiteDatabase.openOrCreateDatabase(file, null)`, `execSQL` each statement of spec §1.1 (**not** `android_metadata`), insert `statistics_playback` rows (`_id` = 1..n, columns as in §1.2), one `statistics_musics` row per (track, day) (title/album/artist/duration/`_data`/`play_count`/`play_duration` aggregated, others null/0), one `statistics_albums` and `statistics_artists` row per (name, day) with `_id` = stable positive hash of the name, `recorded_song_count`, `total_play_duration`, `total_play_count`; leave `statistics_genres` empty. Wrap inserts in `beginTransaction()/setTransactionSuccessful()/endTransaction()`, then `close()` before zipping so no `-journal` file remains. Aggregates are best-effort; the file must open in SQLite with all five tables and indexes.

Settings helpers to add (file 06 `SettingsStore`): `installId()` (random 16-hex string created once, stored), `sortRawOrNull()`, `sortFor(key, raw)`, `excludedFoldersNow()`, `saveSortRaw()`, `addExcludedFolders()`.

## 8. UI wiring (Backup & Transfer screen)
```kotlin
val create = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri -> uri?.let { vm.exportCompat(it) } }
create.launch("ManualBackup_${SimpleDateFormat("yyyyMMdd_HHmmssSSS", Locale.US).format(Date())}.${if (asZip) "zip" else "wav"}")
val pick = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> uri?.let { vm.inspectBackup(it) } }
pick.launch(arrayOf("*/*"))
```
`vm.inspectBackup(uri)`: `sniff()` → `REAL_WAV` ⇒ error "This is an audio file, not a backup." · `JSON` ⇒ hand to the Classic restore from file 07 · `ZIP` ⇒ `copyToTemp` → `SafeZip` → recognition rule → `stage()` → dry-run preview state (**no writes**); delete the temp file on every exit path. `vm.restore(options)` re-opens/stages again (never trust a stale preview) then calls `applyRestore`. Use `StateFlow<BackupUiState>` with `Idle / Working(stage) / Preview(..) / Done(report) / Error(message)`. Show real stage names (the entry being written/read) instead of fake percentages. Error text must be plain ("This file is not a recognised backup"), never stack traces or file paths.

## 9. Tests to write (generate the fixture in code — never use the real sample)
- `sniff`: ZIP bytes → ZIP; RIFF/WAVE header → REAL_WAV; `{` → JSON; random bytes → OTHER; a `.wav`-named ZIP is ZIP.
- `SafeZip`: rejects `../x`, `/abs`, backslash, duplicate names, > MAX_ENTRIES; an entry whose real bytes exceed the limit throws.
- Fixture ZIP with invented data: 3 tracks, 2 playlists (one `My Favourite`), 4 events, sort, scan. Staging counts equal the fixture; `My Favourite` → favourites; other playlist → playlist.
- Matcher: tier 1 exact; SD-card path ↔ internal path matches via tier 2; moved folder matches via tier 3/4; two same-named files → Ambiguous; unknown file → Missing; `<unknown>` artist matches "Unknown artist".
- Apply: Merge twice → second run adds 0 favourites/tracks/events; Replace changes only ticked categories; a forced exception inside the transaction leaves the DB unchanged; Classic JSON tests from file 07 still pass.
- Export → `SafeZip` reopens it; entry order matches spec §1; `metadata.inf.songs` equals real count; export → import into a fresh in-memory DB → same favourites/playlists.

## 10. Prompts for Google AI Studio (paste one at a time; attach only the files named)

### PROMPT 8 — Audit and schema migration
Read `10_COMPATIBLE_BACKUP_SPEC.md` and `11_COMPATIBLE_BACKUP_CODE_AND_PROMPTS.md` §2. First report: where the existing Classic backup (favourites JSON, file 07) lives, and confirm it must not change. Then implement the Room migration 1→2 (`dataPath`, playlist sort columns, unique index on `play_events`), the scanner change that fills `dataPath`, and the new DAO methods. Do **not** use destructive migration. Add a migration test. Build and fix errors. Update `brain.md`. Stop.

### PROMPT 9 — Reader (sniff, safe ZIP, staging, matcher)
Read `10_…SPEC.md` §1–§6 and `11_…` §1, §3, §4, §5. Implement `Compat`, `sniff`, `copyToTemp`, `LimitedStream`, `SafeZip`, the JSON helpers, `PathKey`, `LibraryIndex`, `stage()`. Write the unit tests from §9 for sniff, SafeZip, staging and matching using a ZIP fixture generated in test code. No UI yet. Run the tests, report real results, update `brain.md`. Stop.

### PROMPT 10 — Restore (preview + apply + UI)
Read `10_…SPEC.md` §3, §4, §6 and `11_…` §6, §8. Implement the dry-run preview, `applyRestore` in one Room transaction, the SettingsStore helpers (`saveSortRaw`, `addExcludedFolders`), the restore UI (detected format, preview, categories, Merge/Replace with confirmation, real report, "Save unmatched list"), and the smart picker that routes JSON → Classic, ZIP → Full, real WAV → error. Add the apply tests from §9 (repeat-restore idempotency, replace scope, rollback). Update `brain.md`. Stop.

### PROMPT 11 — Export
Read `10_…SPEC.md` §1, §2, §4 and `11_…` §7, §8. Implement `exportCompat` (entry order exactly as in spec §1, `metadata.inf` last), `buildSnapshot`, the SettingsStore helpers, and the Create-backup UI (category checkboxes with real counts, `.wav` default name with a "Save as .zip" switch, show the name the provider actually saved, "Last backup" card written only after verification). Add the export tests from §9, including export→import round trip. Update `brain.md`. Stop.

### PROMPT 12 — Verify against the real sample and audit
Re-read `10_…SPEC.md` §8. On a real device, with the user's real sample file (**never commit it**), report PASS / FAIL / NOT VERIFIED for each acceptance line, with real numbers from the preview and the restore report. Confirm Classic backup/restore is unchanged. List what could not be tested (for example acceptance of your exported file by the original app) and write it into `brain.md` and the README under "Backup & restore". Do not claim anything that was not run.
# 10 — Compatible Backup & Restore (reads/writes the sample "ManualBackup_*.wav" format)

> **Extends the Pulse Player spec pack (00–09). Does not replace anything.** The existing favourites-JSON export/import (file 07) stays exactly as it is and is called **Classic backup** in the UI. This file adds a second method, **Full backup (reference format)**, that reads and writes the same structure as the user's real sample file.
>
> This file replaces the earlier ChatGPT "Dual Backup" prompt. That prompt invented its own `.musicbackup` format (`manifest.json`, `checksums.json`, …) instead of following the sample, so a real sample file would **not** have imported. **Do not use `.musicbackup`, `manifest.json` or `checksums.json`.** Follow only the format documented below.
>
> Read this file (format + rules), then `11_COMPATIBLE_BACKUP_CODE_AND_PROMPTS.md` (code + step prompts).

## 1. What the sample file really is (verified by opening it)
`ManualBackup_20261028_172204175.wav` is **not audio**. Its first bytes are `50 4B 03 04` ("PK") — it is a **standard ZIP** (deflate, UTF-8 names, no comment, no folder entries) renamed to `.wav`. It was made by another music player (versionName `4.4.1.189`, versionCode `189`) on an Infinix X6881. It contains **only metadata and statistics, no audio files**.

Entries, in the order the source app writes them:

| # | Entry | Format | What it holds (sample numbers) |
|---|-------|--------|-------------------------------|
| 1 | `settings.bak` | JSON object | Category flags `isBackupMusicTags, isBackupCovers, isBackupLyrics, isBackupPlaylists, isBackupSortSettings, isBackupScanHideSettings, isBackupMusicRecap` (booleans) + `device`, `versionName`, `versionCode` |
| 2 | `Covers/MusicCoversBackupConfig.conf` | JSON array, 13,509 items | `{"_data": <abs path>, "resource": null, "cover_url": null}` |
| 3 | `Covers/AlbumCoversBackupConfig.conf` | JSON array, 6,563 items | `{"album": <name>, "album_art": "content://media/external/audio/albumart/<id>", "resource": null, "modified_album_art": null}` |
| 4 | `MusicTagsBackupConfig.conf` | JSON array, 13,509 items | `{"_data": <abs path>, "title", "artist" ("<unknown>" when missing), "album", "track": int, "genres": null or string}` |
| 5 | `Lyrics/LyricsBackupConfig.conf` | JSON array | `[]` (empty in the sample; non-empty schema unknown) |
| 6 | `PlayLists/PlayListBackupConfig.conf` | JSON array, 41 playlists | `{"name", "sort": {"key": "playlistPlayOrder", "order": -1}, "cover": null, "musics": [{"path": <abs path>}, …]}`. The playlist named **`My Favourite`** is the favourites list (4,803 paths). The other 40 are mostly empty. |
| 7 | `SortBackupConfig.conf` | JSON object | Keys `All_Song, Directory_, Folder_Song, Album_, Album_Song, Artist_, Artist_Song, Genre_, Genre_Song`, each `{"key": <field>, "order": -1 or 1}`. `-1` = descending, `1` = ascending. Keys seen: `dateAdded, songCount, sortTrackNumber, numSongs`. |
| 8 | `ScanHiddenBackupConfig.conf` | JSON object | `size: -1`, `duration: -1`, `device`, `exclude: [{"_id": int, "data": <abs folder>, "type": 1}, …]` (17 folders), `hidden_songs: [{"path", "is_hidden": 0}, …]` (13,509). Paths may contain `\/` escapes — any JSON parser reads them. |
| 9–13 | `Statistics/snapshot_monthly_YYYYMM.db` | SQLite files | One per **completed** month (here 202605–202609). Tables below. |
| 14 | `Statistics/StatisticsSnapshotConfig.json` | JSON object | `{"snapshots": [{"type": "monthly", "value": "202605", "filename": "snapshot_monthly_202605.db"}, …], "initApplicationTimestamp": <epoch ms>}` |
| 15 | `Statistics/PlaybackRecords.json` | JSON array, 2,319 records | One object per play (keys in §2.3). It is a **superset** of the snapshots: the 5 snapshots hold 2,113 of them and the JSON also holds the 206 from the current month. No duplicate `(song_id, timestamp)` pairs. |
| 16 | `metadata.inf` | JSON object (written last) | `backupTime` (`yyyyMMdd_HHmmssSSS`, local time), `mode: "MANUAL_GLOBAL"`, `type: "GLOBAL"`, `isAutoBackup: false`, `deviceId`, `versionName`, `versionCode`, `settings: {…same 7 flags…}`, `songs` (13509), `playlists` (41) |

### 1.1 Snapshot database schema (exact; all 5 snapshots identical, `user_version` 0)
```sql
CREATE TABLE statistics_playback(_id INT, song_id INT, timestamp INT, day TEXT, looper_count INT, source_type TEXT, source_id TEXT, play_duration INT, play_count INT, device TEXT, skip_count INT, extra_int_1 INT, extra_int_2 INT, extra_int_3 INT, extra_text_1 TEXT, extra_text_2 TEXT, extra_text_3 TEXT);
CREATE INDEX idx_song_day_source ON statistics_playback(song_id,day,source_type,source_id);
CREATE INDEX idx_statistics_timestamp ON statistics_playback(timestamp);
CREATE INDEX idx_statistics_day ON statistics_playback(day);
CREATE INDEX idx_statistics_song_id ON statistics_playback(song_id);

CREATE TABLE statistics_musics(_id INT, title TEXT, album_id INT, album TEXT, artist_id INT, artist TEXT, duration INT, _data TEXT, cover_url TEXT, is_hidden INT, genre_id, genre TEXT, data_modified INT, play_count, play_duration, skip_count, looper_count, source_type TEXT, source_id TEXT, device, timestamp, day TEXT, extra_int_1, extra_int_2, extra_int_3, extra_text_1, extra_text_2, extra_text_3);
CREATE INDEX idx_statistics_musics_timestamp ON statistics_musics(timestamp);
CREATE INDEX idx_statistics_musics_day ON statistics_musics(day);

CREATE TABLE statistics_albums(_id INT, album TEXT, artist TEXT, artist_id INT, album_art TEXT, modified_album_art TEXT, recorded_song_count, total_play_duration, total_play_count, timestamp, day TEXT);
CREATE INDEX idx_statistics_albums_timestamp ON statistics_albums(timestamp);
CREATE INDEX idx_statistics_albums_day ON statistics_albums(day);

CREATE TABLE statistics_artists(_id INT, artist TEXT, artist_art TEXT, recorded_song_count, recorded_album_count, total_play_duration, total_play_count, timestamp, day TEXT);
CREATE INDEX idx_statistics_artists_timestamp ON statistics_artists(timestamp);
CREATE INDEX idx_statistics_artists_day ON statistics_artists(day);

CREATE TABLE statistics_genres(_id INT, name TEXT, recorded_song_count, recorded_artist_count, recorded_album_count, total_play_duration, total_play_count, timestamp, day TEXT);
CREATE INDEX idx_statistics_genres_timestamp ON statistics_genres(timestamp);
CREATE INDEX idx_statistics_genres_day ON statistics_genres(day);
```
Plus Android's own `android_metadata(locale TEXT)` table (Android creates it automatically; never create it by hand). In the sample, `statistics_musics` has one row per (song, day) and `statistics_albums/artists` have one row per (name, day) with running totals.

### 1.2 `PlaybackRecords.json` record (exact key set, all 17 keys always present)
```json
{"song_id": 0, "timestamp": 0, "day": "YYYYMMDD", "looper_count": 0, "source_type": "", "source_id": "",
 "play_duration": 0, "play_count": 1, "device": "<phone model>", "skip_count": 0,
 "extra_int_1": 0, "extra_int_2": 0, "extra_int_3": 0, "extra_text_1": null, "extra_text_2": null, "extra_text_3": null,
 "_data": "<abs path>"}
```
This is a **shape illustration only** (never ship these values). `timestamp` = epoch milliseconds, `day` = local date `yyyyMMdd`, `play_duration` = milliseconds actually listened, `play_count` is mostly 1 (sometimes 2–3 = repeats). `song_id` is the **source phone's MediaStore-style id** (10 digits starting `1000…`): it is **not portable** and must never be used to find a track.

## 2. Hard facts that shape the design
1. **Nothing in this format identifies a song portably except its path and its tags.** Playlists and hidden-song lists contain **only absolute paths**. Tags carry title/artist/album/track. There is **no duration, size or hash**. So matching = path first, then metadata (see §5).
2. **Paths are phone-specific**: `/storage/emulated/0/...` (internal) or `/storage/XXXX-XXXX/...` (SD card). On another device or after files move they will not exist. Matching must tolerate that and **report** what could not be matched.
3. **The source library is wider than Pulse Player's.** The sample lists 13,509 files including notification sounds, WhatsApp voice notes and ringtones. Pulse Player's scan skips ringtones/notifications/alarms and clips shorter than the minimum duration, so many entries will legitimately not match. The restore report must say so ("not in this app's library — filtered or missing"), not call it a failure.
4. **Covers are not portable**: album `content://media/external/audio/albumart/<id>` URIs belong to the source phone; every `resource`/`cover_url`/`modified_album_art` in the sample is `null`. Never trust or open them.
5. **Lyrics schema is unknown** (the sample's file is `[]`). Do not guess a non-empty schema: import only an empty array; for a non-empty array skip and report "lyrics format not recognised".
6. `My Favourite` = favourites. Pulse Player keeps favourites in its own `favorites` table, not as a playlist, so this one playlist is routed there (§4).
7. **Compatibility honesty.** Reading this sample is fully specified above and testable. Writing a file the *other* app will accept is a best effort that follows the same layout; it cannot be proven here. The UI and `brain.md` must say: "Imports the original file format; exported files follow the same layout; acceptance by the other app is untested."

## 3. UI (extends the Backup & Transfer screen from file 07)
Two clearly separated cards plus one smart entry:
- **Classic backup (favourites JSON)** — unchanged from file 07. Label it "Classic · favourites only".
- **Full backup (reference format)** — "Saves favourites, playlists, play history, sort and scan settings in one file you can move to another phone. Music files are **not** included."
  - Category checkboxes mirroring the 7 flags: Music tags · Covers · Lyrics · Playlists & favourites · Sort settings · Scan & hidden settings · Listening history (recap). Each shows its real record count.
  - **Create backup** → SAF `CreateDocument`, suggested name `ManualBackup_<yyyyMMdd_HHmmssSSS>.wav` (same naming as the sample) with a switch "Save as .zip instead". Use MIME `application/octet-stream` and show the name the provider actually saved.
  - **Restore backup** → SAF `OpenDocument` with `*/*` (providers label this file as audio; never filter by MIME).
  - **Last backup** card (file name, time, size, counts) written **only after** a verified write.
- **Smart restore button** (optional but recommended): one picker that sniffs the file and routes it — favourites JSON → Classic restore; ZIP → Full restore; real WAV audio → "This is an audio file, not a backup."
Restore screen: detected format, source app version + backup time (`metadata.inf`), which categories are present with counts, warnings, category checkboxes, **Merge** (default) or **Replace selected**, a confirmation dialog for Replace, progress, and the final report (§6).

## 4. Mapping: sample categories ↔ Pulse Player data
| Flag / file | Export (Pulse Player → file) | Restore (file → Pulse Player) |
|---|---|---|
| Music tags (`MusicTagsBackupConfig.conf`) | One item per available track: `_data` = absolute path (see §7), title, artist (`"<unknown>"` when ours is "Unknown artist"), album, `track`, `genres: null` | **Used to improve matching** (path → title/artist/album hints). Pulse Player does not edit tags, so nothing is written back; report "read N tags, used for matching". |
| Covers (both files) | One item per track with `resource: null, cover_url: null`; one per album with `album_art: null, resource: null, modified_album_art: null` (never export our `content://` ids) | Parsed and **ignored**; report "covers are device-specific and were not applied". |
| Lyrics | `[]` | Empty array accepted; non-empty skipped with a report. |
| Playlists | `My Favourite` first (our favourites, array order = our display order), then every user playlist: `sort` = our stored sort (default `{"key":"playlistPlayOrder","order":-1}`), `cover: null`, `musics` in position order | `My Favourite` (case-insensitive; also "My favorite", "My favorites") → favourites table. Others → playlists by name. Array order is preserved. |
| Sort settings | `All_Song` from the Songs-tab sort; any raw keys we received earlier are written back unchanged; the rest default as in the sample | Known keys applied (`All_Song`: dateAdded/title/artist/duration/…), **all** raw keys stored in DataStore so a later export round-trips them. |
| Scan & hidden | `size: -1`, `duration: -1`, `device` = model label, `exclude` from our excluded folders (`_id` sequential, `type: 1`), `hidden_songs` = every track with `is_hidden: 0` | `exclude` → our excluded folders (convert absolute path to the relative form our scanner uses, §7). `size`/`duration`: unit unknown → **not applied**, reported. `hidden_songs` with `is_hidden: 1` → skipped and reported (feature not supported). |
| Listening history | `PlaybackRecords.json` = every row of `play_events` joined to its track path; plus one monthly snapshot `.db` for each **completed** month that has events, plus `StatisticsSnapshotConfig.json` | Import `PlaybackRecords.json` (the superset). Snapshots are **validated only** (SQLite header + opened read-only from a temp copy), never merged into the live database and never used to replace a database file. |

## 5. Track matching (target device)
Index the current library once (hash maps), then resolve each source path in this order; **stop at the first tier that yields exactly one track**:
1. **Exact path** (case-insensitive, Unicode NFC).
2. **Storage-relative path**: strip the volume prefix (`/storage/emulated/<n>/`, `/storage/XXXX-XXXX/`, `/sdcard/`) and compare the rest — handles SD card ↔ internal and different user ids.
3. **Parent folder name + file name** (last two path segments).
4. **Tags**: normalised title + artist + album from the sample's tag entry for that path (treat `<unknown>`/blank artist as "Unknown artist", blank album as "Unknown album"). Requires title match **and** artist/album match (unknown matches unknown).
5. **File name only**, accepted only if exactly one track has that name.
If a tier yields **more than one** track → mark **ambiguous** and do not guess; list it in the report. If nothing matches → **unmatched** with a reason (`not in library` / `filtered by scan rules` cannot be told apart reliably — just say "not found on this device").
Never use `song_id`. Never claim a match for title-only.

## 6. Restore rules (safe, predictable)
1. **Sniff** the file's first bytes: `50 4B 03 04` (also `50 4B 05 06`) = ZIP; `52 49 46 46 .... 57 41 56 45` (RIFF…WAVE) = real WAV audio → reject; `{` = JSON (Classic). Ignore the extension and MIME type.
2. Validate as **untrusted**: copy the stream to a bounded temp file (max 512 MB), open with `ZipFile`; max 64 entries; declared and **actually read** bytes per entry ≤ 256 MB and total ≤ 768 MB; reject absolute paths, `..`, `\`, NUL, duplicate names; never extract to disk except the snapshot `.db` copies in `cacheDir`. Stream-parse JSON (`android.util.JsonReader`); don't build one giant string.
3. The archive is accepted as a backup only if it has `metadata.inf` **and** at least one known config entry from §1. Unknown extra entries are ignored. A missing optional category is "not included". Missing `metadata.inf` → reject ("not a recognised backup").
4. **Preview before any change** (no DB writes): backup time, source version, counts per category (`songs`, `playlists` from `metadata.inf`, real counted arrays), warnings, and a dry-run match count (matched / ambiguous / unmatched).
5. **Merge** (default): add what is missing, keep everything else. **Replace selected**: for each ticked category replace our data, only after a confirmation dialog, and only categories that actually exist in the file. Replace applies to favourites, playlists and play history only; never touches tracks or music files.
6. **Stage first, commit once**: parse and resolve everything in memory, then apply in **one Room transaction** (`db.withTransaction`). Failure or cancel before commit = zero changes.
7. **No duplicates on repeat**: play events dedupe on `(trackId, playedAtMs)` via a unique index (migration in file 11 §2); playlist tracks already have a composite primary key; favourites use `IGNORE`. Restoring the same file twice must change nothing the second time.
8. Never move, rename, delete or modify audio files. Never open any path or `content://` URI from the file.
9. **Real report** (shown after commit): per category imported / already present / skipped, plus matched-by-tier counts, **ambiguous**, **unmatched** (offer "Save unmatched list" as a plain text file via SAF), and warnings. Never print "restored successfully" if anything was unmatched without saying how many.

## 7. Paths in Pulse Player
Pulse Player's tracks currently store `RELATIVE_PATH` + `displayName`, not an absolute path. The compatible format needs absolute paths, so:
- Add `tracks.dataPath TEXT NULL` filled by the scanner from `MediaStore.Audio.Media.DATA` (deprecated but still returned for files the app can read). If it is null/blank, **compose** it: for the primary volume `"/storage/emulated/0/" + RELATIVE_PATH + DISPLAY_NAME`; for other volumes use `MediaStore.Audio.Media.VOLUME_NAME` → `"/storage/<VOLUME_NAME uppercase>/" + …`. For SAF-scanned tracks use the document's decoded path when derivable, else leave null and **omit** those tracks from path-based exports (count them in warnings).
- Our excluded-folder setting stores the **relative** form the scanner compares (`Notifications`, `Android/media/com.whatsapp/WhatsApp/Media/WhatsApp Audio`). Convert absolute ↔ relative with the same prefix stripping as §5 tier 2.

## 8. Acceptance (use the user's real sample on the user's phone)
The sample was made on the same phone model, so many `/storage/emulated/0/...` paths should match exactly.
- Preview of the sample shows: tags 13,509 · playlists 41 · favourites entries 4,803 · play records 2,319 · snapshots 5 (all valid) · lyrics 0 · exclude folders 17.
- Real `.wav` audio and a random ZIP without `metadata.inf` are rejected.
- Restore (Merge) then restore again → the second report shows 0 new favourites, 0 new playlist tracks, 0 new play events.
- Export → file reopens as ZIP with the entries of §1 in that order; `metadata.inf` counts equal real counts; importing that exported file back into the same app (Merge) changes nothing.
- Classic favourites export/import from file 07 still works, byte-for-byte unchanged behaviour.
- **Keep the user's real sample out of Git** (add `ManualBackup_*` and `*.wav` to `.gitignore`); unit tests use a tiny fixture ZIP **generated in test code** with invented songs, never the real file.
# 09 — Prompts to paste into Google AI Studio (Gemini Flash)

## How to use
1. Put files `00`–`08` in the project under `docs/spec/` (or attach them in AI Studio).
2. Paste **one prompt at a time**. Attach `SPEC_INDEX.md` (file 12 §2) and the files listed for that phase. If a phase file and a fix-pack file (12–17) disagree, the fix-pack file wins.
3. Wait for it to finish and build. If it errors, paste the exact error back with: *"Fix this compile error. Output the complete corrected file(s)."*
4. Only then send the next prompt.

> Note: AI Studio can write the code but may not be able to run a native Android build. Open the project in **Android Studio** to build/run on your phone. If the model can't compile, it must say so and list likely errors rather than claim success.

---

## PROMPT 0 — Read and plan (no code yet)
Read `00_START_HERE.md`, `01_PRODUCT_AND_FEATURES.md` and `02_DESIGN_SYSTEM.md` completely. You are building **Pulse Player**, a native Android offline music player (Kotlin, Compose, Media3, Room). Inspect the existing project files first and keep the existing package/application ID if there is one. Reply with: (1) what already exists, (2) your build plan using the 8 parts below, (3) any risks. Do not write feature code yet. Create `brain.md` with sections: Done, Tested, Not done, Known limits. Stop and wait.

## PROMPT 1 — Project foundation, database, scan
Read `03_PROJECT_DATA_AND_SCAN.md`. Implement it completely: Gradle setup, manifest (no INTERNET permission), Room entities/DAOs/database, `norm()`, `PulseApp`, `LibraryScanner` (MediaStore), permission flow, `ArtworkLoader`. Output **complete files**, not snippets. Add a temporary minimal screen tagged `// TODO-TEMP(phase1): remove when Home/Songs exist` and listed in `docs/TEMP_CODE.md`, that lists scanned song titles to prove the scan works with real files (remove it in part 3). No dummy data. Fix compile errors. Update `brain.md`. Stop.

## PROMPT 2 — Playback
Read `04_PLAYBACK_AND_AUDIO.md`. Implement `MusicPlaybackService`, `PlayerController`, `AudioFx`, sleep timer, fades, queue persistence, ringtone helper. Wire the temporary list so tapping a song plays it with background playback and notification controls. One player instance only. Output complete files. Update `brain.md`. Stop.

## PROMPT 3 — Theme, shell and main screens
Read `02_DESIGN_SYSTEM.md` and `05_UI_CODE.md`. Implement the theme, skins, `AppBackground`, components (Artwork, HeartButton, PlayingBars, TrackRow, PillButton, MiniPlayer), `AppShell` (phone + tablet by WindowSizeClass), `MainViewModel`, Home tabs, For you, Songs, Playlists, Folders, Albums, Artists, Search, Now Playing, queue/sleep/add-to-playlist sheets, Settings, Equalizer screen. Replace the temporary list. No emojis; use Material icons. Every button must work. Output complete files in small batches (max 4 files per reply). Update `brain.md`. Stop.

## PROMPT 4 — Player themes and skins
Read `06_PLAYER_THEMES_AND_SKINS.md`. Implement `SettingsStore`, the 8 player themes with their layouts, procedural backgrounds, the carousel screen with APPLY/USING, the Skin theme screen with custom wallpaper copy-to-private-storage, and dynamic accent. Respect reduced motion. Output complete files. Update `brain.md`. Stop.

## PROMPT 5 — Favourites JSON, Music folder
Read `07_FAVORITES_JSON_AND_MUSIC_FOLDER.md`. Implement codec, matcher, repository, Backup & Transfer screen (export, import with preview, merge/replace, ambiguous resolution), Music folder creation via SAF and SAF scanning. Add unit tests listed in §8. The JSON must be generated from real favorites only. Output complete files. Update `brain.md`. Stop.

## PROMPT 6 — Widgets, recap, icon, README
Read `08_WIDGETS_STATS_README_ICON.md`. Implement the Glance widget with the state bridge, callbacks and receiver, vector widget icons, recap logic and screen with share card, adaptive launcher icon, `favicon.svg`, `README.md`, `CHANGELOG.md`, `.gitignore`. Output complete files. Update `brain.md`. Stop.

## PROMPT 7 — Final audit and fixes
Re-read `00_START_HERE.md` and `01_PRODUCT_AND_FEATURES.md`. Audit the whole project: dead buttons, placeholder/TODO code, fake data, emoji in UI, INTERNET permission, network libraries, main-thread disk or JSON work, duplicate player instances, unhandled permission denial, clipped text at large font scale, mini player covering the last list row, tablet layout, reduced motion. **Fix** everything you find. Then report each requirement from files 00–08 as PASS / FAIL / NOT VERIFIED with a short reason. Do not claim anything was tested that was not. Update `brain.md`.

---

## Continuation prompt (if the model loses context)
Continue the **Pulse Player** Android project. Read `00_START_HERE.md` and `brain.md` first. Tell me: the last completed part, the current build status, files changed, unresolved errors, and the next incomplete item. Then do **one** bounded piece of work, update `brain.md`, and stop.

## Error-fix prompt
Here is the exact compile error from Android Studio: <paste>. Check the dependency versions in `build.gradle.kts` and the imports. Output the **complete corrected file(s)** only. Do not change unrelated code.

## Quality prompt (use after everything works)
Polish pass: improve spacing consistency (16dp gutters, 24dp section gaps), tune animation timings to the design system, make sure first-load list stagger runs only once, verify all icon buttons have contentDescription, and re-test large font scale. Output only the changed files.
# 08 — Widgets (Glance), Monthly Recap, README, Icon & Favicon

## 1. Home-screen widget (one responsive widget = 2x2, 4x1, 4x2)

### 1.1 State bridge (service writes, widget reads)
```kotlin
object WidgetStateStore {
    private const val PREFS = "widget_state"
    fun write(ctx: Context, p: Player, scope: CoroutineScope) {
        val md = p.mediaMetadata
        val artPath = File(ctx.cacheDir, "widget_art.png")
        scope.launch(Dispatchers.IO) {
            val uri = p.currentMediaItem?.localConfiguration?.uri?.toString()
            if (uri != null) ArtworkLoader.load(ctx, uri, 160)?.let { b -> artPath.outputStream().use { b.compress(Bitmap.CompressFormat.PNG, 90, it) } } else artPath.delete()
            ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putString("title", md.title?.toString()).putString("artist", md.artist?.toString())
                .putBoolean("playing", p.isPlaying).putBoolean("has", p.mediaItemCount > 0)
                .putLong("pos", p.currentPosition).putLong("dur", p.duration.coerceAtLeast(0)).apply()
            PlayerWidget().updateAll(ctx)
        }
    }
    fun read(ctx: Context) = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
```
Throttle: the service listener already fires only on player events (not every frame). Do not update the widget on position ticks.

### 1.2 Widget
```kotlin
class PlayerWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Responsive(setOf(DpSize(110.dp, 110.dp), DpSize(250.dp, 110.dp), DpSize(250.dp, 200.dp)))
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val sp = WidgetStateStore.read(context)
        val art = File(context.cacheDir, "widget_art.png").takeIf { it.exists() }?.let { BitmapFactory.decodeFile(it.path) }
        provideContent {
            val size = LocalSize.current
            val has = sp.getBoolean("has", false); val playing = sp.getBoolean("playing", false)
            val title = sp.getString("title", null) ?: "Nothing playing"; val artist = sp.getString("artist", null).orEmpty()
            val bg = GlanceModifier.fillMaxSize().background(ColorProvider(Color(0xFF14101F))).cornerRadius(24.dp).padding(12.dp)
                .clickable(actionStartActivity<MainActivity>())
            when {
                size.width < 200.dp -> Column(bg, horizontalAlignment = Alignment.CenterHorizontally) {         // 2x2
                    Art(art, 64.dp); Spacer(GlanceModifier.height(6.dp)); Row { Ctl((if (playing) R.drawable.ic_w_pause else R.drawable.ic_w_play), PlayPauseCb::class); Ctl(R.drawable.ic_w_next, NextCb::class) } }
                size.height < 150.dp -> Row(bg, verticalAlignment = Alignment.CenterVertically) {               // 4x1
                    Art(art, 56.dp); Spacer(GlanceModifier.width(10.dp))
                    Column(GlanceModifier.defaultWeight()) { Title(title); Sub(artist) }
                    Ctl(R.drawable.ic_w_prev, PrevCb::class); Ctl((if (playing) R.drawable.ic_w_pause else R.drawable.ic_w_play), PlayPauseCb::class); Ctl(R.drawable.ic_w_next, NextCb::class) }
                else -> Column(bg) {                                                                             // 4x2
                    Row(verticalAlignment = Alignment.CenterVertically) { Art(art, 84.dp); Spacer(GlanceModifier.width(12.dp)); Column { Title(title); Sub(artist) } }
                    Spacer(GlanceModifier.defaultWeight())
                    Row(GlanceModifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        Ctl(R.drawable.ic_w_prev, PrevCb::class); Ctl((if (playing) R.drawable.ic_w_pause else R.drawable.ic_w_play), PlayPauseCb::class); Ctl(R.drawable.ic_w_next, NextCb::class) } }
            }
        }
    }
}
```
Helpers (write them so they compile with Glance 1.1): `Art(bitmap?, size)` → `Image(ImageProvider(bitmap))` with `cornerRadius(12.dp)` or a gradient fallback `ic_w_note`; `Title/Sub` → `Text` with `TextStyle(color = ColorProvider(Color.White), fontWeight = Bold, fontSize = 14.sp)` and maxLines 1; `Ctl(iconRes, callback)` → `Image(provider = ImageProvider(iconRes), contentDescription = ..., modifier = GlanceModifier.size(40.dp).padding(6.dp).clickable(actionRunCallback(callback.java)))`. Replace `(if (playing) R.drawable.ic_w_pause else R.drawable.ic_w_play)` with `if (playing) R.drawable.ic_w_pause else R.drawable.ic_w_play`.
Create vector drawables via Android Studio: `ic_w_play`, `ic_w_pause`, `ic_w_next`, `ic_w_prev`, `ic_w_note` (white, 24dp). When `has == false` show only the title "Nothing playing" and a "Open app" tap — truthful empty state.

### 1.3 Callbacks and receiver
```kotlin
object WidgetControls {
    suspend fun send(ctx: Context, block: (MediaController) -> Unit) = withContext(Dispatchers.Main) {
        val token = SessionToken(ctx, ComponentName(ctx, MusicPlaybackService::class.java))
        val c = MediaController.Builder(ctx, token).buildAsync().await()
        block(c); delay(300); c.release()
    }
}
class PlayPauseCb : ActionCallback { override suspend fun onAction(c: Context, g: GlanceId, p: ActionParameters) { WidgetControls.send(c) { if (it.isPlaying) it.pause() else it.play() } } }
class NextCb : ActionCallback { override suspend fun onAction(c: Context, g: GlanceId, p: ActionParameters) { WidgetControls.send(c) { it.seekToNextMediaItem() } } }
class PrevCb : ActionCallback { override suspend fun onAction(c: Context, g: GlanceId, p: ActionParameters) { WidgetControls.send(c) { it.seekToPreviousMediaItem() } } }
class PlayerWidgetReceiver : GlanceAppWidgetReceiver() { override val glanceAppWidget: GlanceAppWidget = PlayerWidget() }
```
`res/xml/player_widget_info.xml`:
```xml
<appwidget-provider xmlns:android="http://schemas.android.com/apk/res/android"
    android:minWidth="110dp" android:minHeight="110dp" android:minResizeWidth="110dp" android:minResizeHeight="40dp"
    android:targetCellWidth="4" android:targetCellHeight="2" android:resizeMode="horizontal|vertical"
    android:updatePeriodMillis="0" android:widgetCategory="home_screen" android:previewLayout="@layout/widget_preview" />
```
(Provide a simple `widget_preview.xml` or drop `previewLayout`.) Sizes are launcher-dependent: say "target configuration", not a guarantee. Settings has a "Widgets" card explaining: long-press home screen → Widgets → Pulse Player.

## 2. Monthly recap (real data only)
```kotlin
data class Recap(val totalMs: Long, val topTracks: List<Pair<TrackEntity, Int>>, val topArtists: List<ArtistStat>, val badges: List<String>)

suspend fun buildRecap(db: PulseDb, month: YearMonth): Recap? {
    val zone = ZoneId.systemDefault()
    val from = month.atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
    val to = month.plusMonths(1).atDay(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1
    val total = db.stats().totalMs(from, to); if (total <= 0) return null
    val top = db.stats().topTracks(from, to, 5); val tracks = db.tracks().byIds(top.map { it.trackId }).associateBy { it.id }
    val badges = buildList {
        if (total >= 100 * 3_600_000L) add("100 Hours Club")
        if (db.stats().nightMs(from, to) >= total * 0.4) add("Night Owl Listener")     // add DAO: SUM over events where hour(playedAtMs) in 0..4 or >=22
        if (top.firstOrNull()?.plays ?: 0 >= 30) add("On Repeat") }
    return Recap(total, top.mapNotNull { t -> tracks[t.trackId]?.let { it to t.plays } }, db.stats().topArtists(from, to, 5), badges)
}
```
Night-owl DAO sketch: `SELECT COALESCE(SUM(listenedMs),0) FROM play_events WHERE playedAtMs BETWEEN :from AND :to AND (CAST(strftime('%H', playedAtMs/1000, 'unixepoch', 'localtime') AS INTEGER) >= 22 OR CAST(strftime('%H', playedAtMs/1000, 'unixepoch', 'localtime') AS INTEGER) < 5)`.

**Recap screen:** month switcher (< Oct 2026 >), big total ("12 h 40 min"), Top 5 songs with art and play counts, Top 5 artists, badges as gradient chips, **Share** button: render a 1080x1350 bitmap of the card with `Canvas`/`ImageBitmap` (gradient + text), save to `cacheDir/recap.png`, share via `FileProvider` + `ACTION_SEND` (`image/png`). If no events for the month: empty state "Listen to some music and your recap will appear here."
FileProvider needs `<provider android:name="androidx.core.content.FileProvider" android:authorities="${applicationId}.files" android:exported="false" android:grantUriPermissions="true">` with `res/xml/file_paths.xml` (`<cache-path name="cache" path="."/>`).

## 3. App icon and favicon
- **Android adaptive icon** (required): `mipmap-anydpi-v26/ic_launcher.xml` with `ic_launcher_foreground` (vector) and `ic_launcher_background` (vector gradient) + monochrome layer for themed icons. Design: rounded dark square, a stylized sound-wave of 5 vertical rounded bars in a violet→cyan→pink gradient. Keep within the 66dp safe zone.
- **Foreground vector** (`res/drawable/ic_launcher_foreground.xml`, 108dp):
```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android" android:width="108dp" android:height="108dp" android:viewportWidth="108" android:viewportHeight="108">
    <path android:fillColor="#9B7BFF" android:pathData="M34,48 h6 a3,3 0 0 1 3,3 v6 a3,3 0 0 1 -3,3 h-6 a3,3 0 0 1 -3,-3 v-6 a3,3 0 0 1 3,-3z"/>
    <path android:fillColor="#4DD9F5" android:pathData="M44,38 h6 a3,3 0 0 1 3,3 v26 a3,3 0 0 1 -3,3 h-6 a3,3 0 0 1 -3,-3 v-26 a3,3 0 0 1 3,-3z"/>
    <path android:fillColor="#FF6DAA" android:pathData="M54,30 h6 a3,3 0 0 1 3,3 v42 a3,3 0 0 1 -3,3 h-6 a3,3 0 0 1 -3,-3 v-42 a3,3 0 0 1 3,-3z"/>
    <path android:fillColor="#4DD9F5" android:pathData="M64,38 h6 a3,3 0 0 1 3,3 v26 a3,3 0 0 1 -3,3 h-6 a3,3 0 0 1 -3,-3 v-26 a3,3 0 0 1 3,-3z"/>
    <path android:fillColor="#9B7BFF" android:pathData="M74,48 h6 a3,3 0 0 1 3,3 v6 a3,3 0 0 1 -3,3 h-6 a3,3 0 0 1 -3,-3 v-6 a3,3 0 0 1 3,-3z"/>
</vector>
```
Background: `#0B0816` solid (or a vector gradient `#2A0826`→`#0B0816`). Monochrome layer: same shapes in one color.
- **`favicon.svg`** (for README/GitHub Pages; not a replacement for launcher icons):
```svg
<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 64 64">
  <defs><linearGradient id="g" x1="0" y1="0" x2="1" y2="1"><stop offset="0" stop-color="#9B7BFF"/><stop offset="0.5" stop-color="#4DD9F5"/><stop offset="1" stop-color="#FF6DAA"/></linearGradient></defs>
  <rect width="64" height="64" rx="14" fill="#0B0816"/>
  <g fill="url(#g)"><rect x="12" y="26" width="6" height="12" rx="3"/><rect x="21" y="18" width="6" height="28" rx="3"/><rect x="30" y="10" width="6" height="44" rx="3"/><rect x="39" y="18" width="6" height="28" rx="3"/><rect x="48" y="26" width="6" height="12" rx="3"/></g>
</svg>
```
Also generate `docs/assets/banner.png` placeholder only if the model can create a real image; otherwise omit it from the README.

## 4. README.md template (fill with verified facts only)
````markdown
<p align="center"><img src="docs/assets/favicon.svg" width="96" alt="Pulse Player icon"></p>

# Pulse Player

A fast, colorful, 100% offline music player for Android. Built with Kotlin, Jetpack Compose, Media3, Room and Glance.

## Highlights
- Plays local audio (formats supported by your device), background playback, notification and lock-screen controls
- Tabs: For you, Songs, Playlists, Folders, Albums, Artists; global search
- 9 player themes with a swipeable preview carousel; skin themes and custom wallpapers
- Equalizer, bass boost, virtualizer, reverb (only where the device supports them)
- Sleep timer with fade-out, playback speed, queue management
- Favourites and playlists stored on-device
- **Favourites export/import (JSON)** for moving to a new phone or tablet
- Home-screen widget, monthly recap with shareable card
- Phone and tablet layouts; reduced-motion mode
- No ads, no accounts, **no internet permission**

## Move to a new phone or tablet
1. Settings → Backup & Transfer → Export favourites
2. Copy your music and the JSON file to the new device (into a `Music` folder)
3. Open the app → Scan (or Choose music folder)
4. Settings → Backup & Transfer → Import favourites → review → Apply
The JSON contains song metadata only, not audio.

## Build
Open in Android Studio (latest stable), sync Gradle, run on a device/emulator with Android 8.0+ (API 26).
`./gradlew assembleDebug` · `./gradlew testDebugUnitTest` · `./gradlew lintDebug`

## Permissions
Audio read access (to find your music), notifications (playback controls), optional "Modify system settings" (ringtone only). No network.

## Verified status / known limitations
(Fill from `brain.md`.) Effects depend on device support; some formats need device decoders.

## License
Choose a license before publishing.
````

## 5. Other repo files to create
`brain.md`, `CHANGELOG.md`, `.gitignore` (build/, .gradle/, local.properties, *.jks, exported JSON, music files), `docs/PRIVACY.md` (offline statement), `docs/TESTING.md`.

## 6. Acceptance for this part
Widget can be added, shows real current track, buttons control playback, shows "Nothing playing" otherwise; recap computed from real events; launcher icon renders in round/square/themed masks; README claims only what was verified; `brain.md` updated.
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
