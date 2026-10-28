# 00 — START HERE: "Pulse Player" (Offline Music Player for Android)

> You (the AI coding model) are building a **native Android offline music player**. Read this file fully, then read the numbered files **one at a time, in order**. Do not skip a file. Do not summarize code as "rest unchanged" — always output complete files.

## 1. What the project is
**Pulse Player** is a fast, beautiful, 100% offline music player for audio files stored on the phone or SD card. No account, ads, internet, or streaming. It is a *music player*, not a vault: no hidden media, no PIN screens.

- **Language/UI:** Kotlin, Jetpack Compose, Material 3 (no XML layouts except widget/manifest resources)
- **Playback:** AndroidX Media3 (ExoPlayer + MediaSessionService)
- **Data:** Room (library, favorites, playlists, history), DataStore (settings)
- **Widgets:** Jetpack Glance
- **Architecture:** MVVM, single Activity, `StateFlow`
- **minSdk 26**, targetSdk = latest stable installed in Android Studio
- **No INTERNET permission.** Ever.

## 2. Primary device (design for this first)
Infinix phone, XOS 14.5: 6.78" AMOLED, 1080x2436 (20:9), centered punch-hole camera, rounded corners, ~86% screen-to-body. Use true-black (`#000000`) AMOLED surfaces, `enableEdgeToEdge()`, and respect `WindowInsets.displayCutout`, status bar and gesture-nav insets. A **tablet** will come later: layouts must adapt through `WindowSizeClass` (see 02 and 05).

## 3. File order (each file is small on purpose)
| # | File | Purpose |
|---|------|---------|
| 00 | `00_START_HERE.md` | This file: rules, improvements, build order |
| 01 | `01_PRODUCT_AND_FEATURES.md` | Every screen, feature, behavior |
| 02 | `02_DESIGN_SYSTEM.md` | Colors, gradients, icons, motion, responsive rules |
| 03 | `03_PROJECT_DATA_AND_SCAN.md` | Gradle, manifest, Room, MediaStore scanner, artwork |
| 04 | `04_PLAYBACK_AND_AUDIO.md` | Service, controller, equalizer, sleep timer, fades |
| 05 | `05_UI_CODE.md` | Theme, shell, components, Home, Songs, Now Playing |
| 06 | `06_PLAYER_THEMES_AND_SKINS.md` | Player-theme carousel (A–E), skin theme, wallpapers |
| 07 | `07_FAVORITES_JSON_AND_MUSIC_FOLDER.md` | Export/import favorites JSON, Music folder, SD card |
| 08 | `08_WIDGETS_STATS_README_ICON.md` | Widgets, monthly recap, README, icon/favicon |
| 09 | `09_AI_STUDIO_PROMPTS.md` | Exact prompts to paste, step by step |

## 4. Rules for the AI model (non-negotiable)
1. **One part at a time.** Finish, build, then stop and report. Never try to produce the whole app in one answer.
2. **Complete files only.** No `// TODO`, no `...`, no "same as before".
3. **No fake data.** Never insert dummy songs, fake stats, or fake counts. Empty library = designed empty state with a Scan button.
4. **No dead buttons.** Every visible control must work, or be hidden, or be disabled with a short explanation.
5. **No emojis in the UI.** Use Material icons (`androidx.compose.material:material-icons-extended`) everywhere.
6. **Do not invent APIs.** If unsure about a Media3/Compose/Room signature for the installed version, check the project's dependency version and adapt. The snippets in these files are blueprints; fix imports and signatures so the project compiles.
7. **Do not request `MANAGE_EXTERNAL_STORAGE`.** Use MediaStore + Storage Access Framework.
8. **Never move, rename, or delete the user's music.**
9. **Keep disk, DB, JSON and artwork work off the main thread.**
10. **Update `brain.md`** after every part: done / tested / not done / known limits.
11. If the build fails, fix compile errors before moving on. If you cannot run a build, say so honestly and list the exact errors you expect the user to see.

## 5. What was kept from the original prompt (all preserved)
Top tabs **For you / Songs / Playlists / Folders / Albums**; drawer; skin theme screen with color swatches and wallpapers; resume banner; Shuffle/Play pills; Recently added; Top picks; system playlists (My favourite, Recently added, Recently played, My top tracks); full Now Playing with Song/Lyrics toggle; mini player with spinning disc and marquee; equalizer; sleep timer; queue; **Player theme carousel with 5 theme types (A–E)** and the orange APPLY / USING button; monthly recap; widgets; ringtone; 3D reverb presets; bass boost; virtualizer.

## 6. Improvements added on top (do implement)
**Design**
- Emojis replaced by consistent icons; true-black AMOLED base plus album-art-driven gradients.
- Spring animations, shared mini-player-to-player expand, heart "pop", animated equalizer bars, play/pause icon morph, staggered list entrance (first load only).
- "Special" badge on a few player themes instead of a crown paywall (there are no purchases; all themes are free).
- Wallpapers and "nature/ocean/stars" backgrounds are **drawn procedurally in code** (no copyrighted images, no network, tiny APK).

**Features**
- **Favorites Export/Import JSON** with import preview, merge/replace, and fuzzy matching that survives path changes (file 07).
- **Create / choose a "Music" folder** (SAF) for copying songs to a new device; app scans it.
- **Home-screen widget** (one responsive Glance widget that covers 2x2, 4x1, 4x2).
- Quick tiles on For you: Today's listening, Sleep timer, Favorites shortcut, Equalizer.
- A-Z fast scroller, multi-select, play next / add to queue, song details sheet.
- Volume fade-in/out on play/pause and sleep-timer fade-out (real audio fade, not a UI trick).
- Play speed 0.5x–2.0x, "Stop after this track", reduced-motion and battery-saver switches.
- Min-duration filter (hide short clips) and folder exclude list.
- Persist last queue and position.

**Performance**
- Room-indexed list queries, `LazyColumn` with stable keys, bounded LruCache for artwork, 250 ms throttled progress, no whole-screen recomposition from the seek bar.

## 7. Honest limits to respect
- Equalizer/bass/virtualizer/reverb work only if the device exposes `android.media.audiofx`. Detect and hide unsupported controls.
- True crossfade between tracks is not reliable in stock Media3, so it is **not included**. Fades on play/pause/sleep are.
- Playback of formats (.ape, .midi) depends on the device decoders; show a friendly error if a file can't play.
- Setting a ringtone needs the user to grant "Modify system settings" (`WRITE_SETTINGS`).

## 8. Folder layout to create
```
app/src/main/java/com/pulse/player/
  PulseApp.kt  MainActivity.kt
  data/   (db, scan, repo, settings, favorites)
  playback/ (service, controller, audiofx, sleep)
  ui/     (theme, components, screens, themes, widget)
brain.md  README.md  docs/
```
Use package `com.pulse.player` unless the existing project already has a package ID; if it does, keep that one.
