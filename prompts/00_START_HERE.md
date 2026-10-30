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
