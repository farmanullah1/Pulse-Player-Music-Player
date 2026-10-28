# 09 — Prompts to paste into Google AI Studio (Gemini Flash)

## How to use
1. Put files `00`–`08` in the project under `docs/spec/` (or attach them in AI Studio).
2. Paste **one prompt at a time**. Attach only the files named in that prompt.
3. Wait for it to finish and build. If it errors, paste the exact error back with: *"Fix this compile error. Output the complete corrected file(s)."*
4. Only then send the next prompt.

> Note: AI Studio can write the code but may not be able to run a native Android build. Open the project in **Android Studio** to build/run on your phone. If the model can't compile, it must say so and list likely errors rather than claim success.

---

## PROMPT 0 — Read and plan (no code yet)
Read `00_START_HERE.md`, `01_PRODUCT_AND_FEATURES.md` and `02_DESIGN_SYSTEM.md` completely. You are building **Pulse Player**, a native Android offline music player (Kotlin, Compose, Media3, Room). Inspect the existing project files first and keep the existing package/application ID if there is one. Reply with: (1) what already exists, (2) your build plan using the 8 parts below, (3) any risks. Do not write feature code yet. Create `brain.md` with sections: Done, Tested, Not done, Known limits. Stop and wait.

## PROMPT 1 — Project foundation, database, scan
Read `03_PROJECT_DATA_AND_SCAN.md`. Implement it completely: Gradle setup, manifest (no INTERNET permission), Room entities/DAOs/database, `norm()`, `PulseApp`, `LibraryScanner` (MediaStore), permission flow, `ArtworkLoader`. Output **complete files**, not snippets. Add a temporary minimal screen that lists scanned song titles to prove the scan works with real files (remove it in part 3). No dummy data. Fix compile errors. Update `brain.md`. Stop.

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
