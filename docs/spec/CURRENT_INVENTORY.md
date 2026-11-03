# Current Pulse Player Inventory — Phase 9 Audit
> Generated: 2026-11-03 from the actual working tree.  
> Branch: `main`; remote: `origin/main`; working tree contains uncommitted schema/theme/audit work.  
> `assembleDebug` and `testDebugUnitTest` were attempted and blocked before compilation because SDK 35/build-tools licenses are not accepted in the configured SDK.

---

## 1. Build Configuration

| Field | Value |
|---|---|
| Application ID | `com.aistudio.pulseplayer.wvfykq` |
| Namespace | `com.pulse.player` |
| minSdk | 26 |
| compileSdk | 35 |
| targetSdk | 35 |
| versionCode | 2 |
| versionName | 1.0.1 |
| Build tools | AGP 8.8.2, Kotlin 2.1.10, KSP 2.1.10-1.0.29 |
| Java compatibility | 17 |

### Key Dependencies

| Library | Version |
|---|---|
| Compose BOM | 2024.10.01 |
| AndroidX Activity Compose | 1.9.3 |
| Lifecycle ViewModel/Runtime Compose | 2.8.7 |
| Core KTX | 1.15.0 |
| DocumentFile | 1.0.1 |
| Palette KTX | 1.0.0 |
| DataStore Preferences | 1.1.1 |
| Media3 (ExoPlayer, Session, UI) | 1.5.1 |
| Room (Runtime, KTX, Compiler) | 2.6.1 |
| Glance (AppWidget, Material3) | 1.1.1 |
| Coroutines Android | 1.9.0 |
| Coroutines Guava | 1.9.0 |
| OkHttp | 4.12.0 |
| JUnit | 4.13.2 |
| org.json | 20240303 |
| Coroutines Test | 1.9.0 |

---

## 2. Architecture Overview

- **Single Activity:** `MainActivity` hosts Compose UI. Navigation is stack-based via `ArrayDeque<Screen>` inside `MainViewModel`.
- **Single ViewModel:** `MainViewModel` (AndroidViewModel) owns all UI state: navigation, playback state, tracks, sorting, selection, favorites, playlists, lyrics, backup, AI, scan, settings.
- **Playback:** `MusicPlaybackService` (MediaSessionService, Media3). `PlaybackEngine` singleton coordinates `AudioTransitionController`, `FadingPlayer`, `SleepTimerEngine`, `ErrorPolicy`, `SessionTracker`, `AudioFx`, widget updates.
- **Data:** Room `PulseDb` (version 3), DataStore `pulse_settings`.
- **State management:** StateFlow throughout.
- **UI:** Jetpack Compose + Material3. `AppShell` handles responsive layout (drawer/phone, NavigationRail/expanded).

---

## 3. Screens and Navigation

| Screen | Status |
|---|---|
| `Home` → `HomeTabsScreen` (tabs: Songs, Albums, Artists, Playlists, Folders, For You) | WORKING |
| `Settings` → `SettingsScreen` | WORKING |
| `Equalizer` → `EqualizerScreen` | WORKING |
| `Recap` → `RecapScreen` | WORKING |
| `SkinTheme` → `SkinThemeScreen` | WORKING |
| `PlayerTheme` → `PlayerThemeScreen` | WORKING |
| `Backup` → `BackupScreen` | WORKING |
| `Search` → `SearchScreen` | WORKING |
| `Duplicates` → `DuplicatesScreen` | WORKING |
| `Transcribe` → `TranscribeScreen` | WORKING |
| `GenerateMusic` → `GenerateMusicScreen` | WORKING |
| `Detail(kind, key)` → `DetailScreen` (album/artist/folder/playlist/system) | WORKING |
| Now Playing → `NowPlayingScreen` (dispatches per `PlayerLayoutMode`) | WORKING |

---

## 4. Player Themes (12 total — 9 original + 3 new from prior merge)

| ID | Name | Layout Enum | Special |
|---|---|---|---|
| `classic` | Classic | CLASSIC | no |
| `mountain` | Mountain Horizon | SHEET_QUEUE | yes |
| `mesh` | Mesh Glass | MESH_GLASS | no |
| `minimal` | Minimal Mood | MINIMAL_DARK | no |
| `acoustic` | Acoustic | ACOUSTIC | yes |
| `vinyl` | Vinyl | VINYL | yes |
| `coast` | Coastal Ocean | COAST | no |
| `starry` | Starry Sky | STARRY | no |
| `immersive` | Immersive Artwork Player | IMMERSIVE | no |
| `pixel` | Pixel Expressive | PIXEL | yes |
| `namida` | Namida Waveform | NAMIDA | yes |
| `atmospheric_queue` | Atmospheric Queue | ATMOSPHERIC_QUEUE | yes |

Default player theme: `classic`. Default stored layout mode: `modern_art` (`SettingsStore` fallback). File 18 requires `ATMOSPHERIC_QUEUE` as the layout default/fallback; this is not applied yet.

**Status: UNVERIFIED** — All 12 registry entries and the carousel are present in source; the current working tree has not passed a build/test run in this environment.

---

## 5. Player Layout Modes (from `PlayerLayoutMode.kt`)

The project has a `PlayerLayoutMode` enum/registry separate from `PlayerThemes`. Layouts include:
- `modern_art` (ModernArtPlayerLayout)
- `atmospheric_queue` (AtmosphericQueuePlayerLayout)
- `minimalist` (MinimalistPlayerLayout)
- `split_lyrics` (SplitLyricsPlayerLayout)
- `vinyl_record` (VinylRecordPlayerLayout)
- `waveform` (WaveformPlayerLayout)
- Plus layouts from `ImmersivePlayerLayout`, `PixelPlayerLayout`, `NamidaPlayerLayout`

**Status: PARTIAL** — Layout picker exists (`PlayerLayoutBottomSheet`). Not all layouts may be fully implemented. The five-axis customization system (theme mode, skin, player theme, player layout mode, button layout preset) is partially in place.

---

## 6. Skins / Color Themes

From `Color.kt` — 15 skin definitions including:
- Plum, Aurora, Afterglow, Ocean, Sunrise, Forest, Mono, Pure Black (8 original)
- Additional skins from prior merge (7 more)

ThemeModes: `SYSTEM`, `DARK`, `AMOLED`, `LIGHT`

Dynamic accent from artwork: **WORKING** (DataStore key `dynamic_accent`, default ON)

**Status: UNVERIFIED** — Skin picker and wallpaper support exist in source; screenshot/device behavior is not verified here.

---

## 7. Room Database

**Version: 3** | **File: `pulse.db`**

### Entities

| Entity | Table | Status |
|---|---|---|
| `TrackEntity` | `tracks` | WORKING |
| `ScanSeenEntity` | `scan_seen` | WORKING |
| `LyricsEntity` | `lyrics` | WORKING |
| `FavoriteEntity` | `favorites` | WORKING |
| `PlaylistEntity` | `playlists` | WORKING |
| `PlaylistTrackEntity` | `playlist_tracks` | WORKING |
| `PlayEventEntity` | `play_events` | WORKING |

### Migrations

| Migration | Type | Status |
|---|---|---|
| 1→2 | Add `dataPath`, `scanGen`; add sort cols to playlists; fix unique index | WORKING |
| 2→3 | Add identity cols to tracks; create `scan_seen`, `lyrics`; add stats cols | WORKING |
| Pre-migration backup | Copies `pulse.db` + WAL/SHM before migration | WORKING |

**Critical finding:** `PulseApp.kt` still calls `.fallbackToDestructiveMigration()`, so the no-destructive-migration gate currently FAILS.

---

## 8. DataStore Keys (22 keys in `pulse_settings`)

| Key | Type | Default | Purpose |
|---|---|---|---|
| `theme_mode` | String | DARK | ThemeMode |
| `skin_id` | String | plum | Skin ID |
| `player_theme` | String | classic | Player theme ID |
| `player_layout_mode` | String | modern_art | Layout mode |
| `wallpaper_path` | String? | null | Custom wallpaper |
| `reduced_motion` | Boolean | false | Disable animations |
| `dynamic_accent` | Boolean | true | Artwork-driven accent |
| `min_duration_s` | Int | 30 | Min track duration filter |
| `fade_play_pause` | Boolean | true | Fade on play/pause |
| `excluded_folders` | StringSet | empty | Excluded scan folders |
| `saf_trees` | StringSet | empty | SAF tree URIs |
| `queue_ids` | String | "" | Queue track IDs |
| `queue_index` | Int | 0 | Queue position |
| `queue_pos` | Long | 0 | Playback position ms |
| `eq_preset` | String | — | EQ preset |
| `sort_raw` | String | null | Per-screen sort JSON |
| `song_sort_key` | String | dateAdded | Songs sort key |
| `song_sort_order` | Int | -1 | Songs sort order |
| `install_id` | String | — | Device install ID |
| `last_backup_json` | String | null | Last backup metadata |
| `gemini_api_key` | String? | null | Gemini API key |
| `last_good_scan_at` | Long | null | Last successful scan |
| `audio_permission_asked` | Boolean | false | Permission onboarding |

---

## 9. Playback Features

| Feature | Status |
|---|---|
| Background playback | WORKING |
| MediaSession + notification controls | WORKING |
| Lock-screen controls | WORKING |
| Mini-player (spinning disc, marquee, progress) | WORKING |
| Expand/collapse player transition | WORKING |
| Song/Lyrics toggle | WORKING |
| Seek bar (elapsed/total) | WORKING |
| Shuffle (QueueModel, seeded Fisher-Yates) | WORKING |
| Repeat Off/All/One | WORKING |
| Previous/Play/Pause/Next | WORKING |
| Favourite control | WORKING |
| Sleep timer (presets, end-of-track, 10s fade) | WORKING |
| Add to playlist | WORKING |
| Queue sheet | WORKING |
| Equalizer (AudioFx, capability-aware) | WORKING |
| Track information/share | WORKING |
| Player theme carousel (APPLY/USING) | WORKING |
| Queue operations (reorder, remove) | WORKING |
| Playback position + queue restore paused | WORKING |
| Audio focus / noisy-output handling | WORKING |
| Service reconnect with exponential backoff | WORKING |
| FadingPlayer (smoothstep volume fades) | WORKING |
| AudioTransitionController (single volume writer) | WORKING |
| Error handling (MISSING_FILE, UNSUPPORTED, etc.) | WORKING |
| Speed control (0.5–2.0×) | WORKING |
| Stop after current track | WORKING |
| Headset-unplug pause | WORKING |
| Media3 shuffle permanently disabled | WORKING |

---

## 10. Library / Scanner Features

| Feature | Status |
|---|---|
| MediaStore scan (recoverable, gen-based) | WORKING |
| SAF folder scan | WORKING |
| SD-card mount/unmount handling (`StorageReceiver`) | WORKING |
| Minimum duration filter | WORKING |
| Folder exclude/include | WORKING |
| Identity model (ms:/saf:/copy:) | WORKING |
| Explicit Copy workflow (`MusicCopier`) | WORKING |
| Permission state machine (`PermissionMonitor`) | WORKING |
| VISIBLE query (hides COPIED duplicates) | WORKING |
| Duplicate detection (normTitle + normArtist) | WORKING |
| Missing songs chip/banner | WORKING |
| Artwork cache (bucketed: 96/192/384/768) | WORKING |
| ArtworkLoader (LRU cache, trim on low memory) | WORKING |
| Minimum file size filter | MISSING |
| Artist delimiter / featured artist parsing | MISSING |
| Album Artist grouping (entity field exists, logic partial) | PARTIAL |
| Genre field (entity has `year`, but no `genre` column yet) | MISSING |
| Rescan one folder without full rescan | MISSING |
| Per-source scan status | MISSING |
| Configurable filename-to-tag fallback | MISSING |
| Sort by play count / rating / BPM | MISSING |
| Folder cover discovery (cover.jpg / folder.jpg) | MISSING |
| `.info.txt` sidecar parsing | MISSING |

---

## 11. Search

| Feature | Status |
|---|---|
| Search across songs, albums, artists, playlists, folders | WORKING |
| 250ms debounced search | WORKING |
| Search inside album/artist/etc detail | PARTIAL |
| Typo tolerance | MISSING |
| Ranked results | MISSING |
| Recent searches | MISSING |
| Search by lyrics | MISSING |
| Search by tags/mood/genre | MISSING |

---

## 12. Lyrics

| Feature | Status |
|---|---|
| Import `.lrc`/`.txt` via SAF | WORKING |
| LRC parser (timestamp sync, binary search) | WORKING |
| Display synced lyrics with active line highlight | WORKING |
| Plain text display | WORKING |
| Tap to seek by lyric line | UNVERIFIED |
| Lyrics editor (create/edit timestamps) | MISSING |
| Manual lyric offset adjustment | MISSING |
| Export/save sidecar lyrics | MISSING |
| Adjacent `.lrc` file auto-discovery | MISSING |
| Online LRCLIB lookup | MISSING (by design in offline build) |

---

## 13. Statistics and History

| Feature | Status |
|---|---|
| Play event recording (SessionTracker) | WORKING |
| Speed-aware listen time | WORKING |
| Counted / skipped flags | WORKING |
| Monthly recap screen | WORKING |
| Monthly top tracks/artists | WORKING |
| Today's listening time tile | WORKING |
| Process death recovery (checkpoint) | WORKING |
| Time-zone aware month keys | WORKING |
| Shareable recap cards (local) | UNVERIFIED |
| Editable history (delete/import records) | MISSING |
| Daily/weekly/yearly/custom range summaries | PARTIAL (monthly only confirmed) |
| Listening streaks | MISSING |
| Your Year page | MISSING |
| History import (Last.fm, Spotify etc) | MISSING |

---

## 14. Backup Systems

| Feature | Status |
|---|---|
| **Classic backup** — `pulseplayer.favorites` v1 JSON | WORKING |
| Classic fuzzy matcher | WORKING |
| Classic import/export | WORKING |
| `BackupSniffer` preventing cross-contamination | WORKING |
| **Full backup** — ZIP-in-`.wav` reference format | WORKING |
| Full backup validation (path traversal, limits) | WORKING |
| Full backup merge/replace restore | WORKING |
| Full backup self-check (SHA-256 manifest) | WORKING |
| Classic v2: copy audio files to `Music/Pulse Player/` | MISSING |
| Backup progress/cancellation | MISSING |
| Free-space checks before backup | MISSING |
| Last backup metadata display | PARTIAL |

---

## 15. Settings Structure

Confirmed Settings sections from `SettingsScreen.kt`:
- **Appearance** — theme mode, skins, wallpaper, dynamic colors, reduced motion
- **Library** — min duration, excluded folders, SAF folders, min file size (partial)
- **Playback** — fade, stop after track
- **Backup** — Classic and Full backup entry points
- **Widgets** — Glance widget guidance
- **AI** — Gemini API key, Transcribe, Generate Music

**Missing from spec's recommended Settings IA:**
- Detailed Audio section (EQ profiles, per-track sound profiles, ReplayGain, etc.) — PARTIAL (EqualizerScreen exists)
- Lyrics section (dedicated) — MISSING
- Playlists & Queue section — MISSING
- History & Statistics section — MISSING
- Connections section — MISSING
- Privacy section — MISSING
- Full About section — PARTIAL

---

## 16. Widget

| Feature | Status |
|---|---|
| Glance widget (2×2, 4×1, 4×2) | WORKING |
| Artwork, title, artist, play/pause | WORKING |
| Previous/Next | WORKING |
| Tap opens Now Playing | WORKING |
| Widget state updates on track change | WORKING |
| Correct behavior after reboot | UNVERIFIED |
| Favorite action in widget | MISSING |

---

## 17. Permissions and AndroidManifest

Confirmed permissions (no INTERNET permission):
- `READ_MEDIA_AUDIO` / `READ_EXTERNAL_STORAGE` (API < 33)
- `FOREGROUND_SERVICE` + `FOREGROUND_SERVICE_MEDIA_PLAYBACK`
- `WAKE_LOCK`
- `RECEIVE_BOOT_COMPLETED`
- `WRITE_SETTINGS` (for ringtone, not normal)

**INTERNET permission: absent** — confirmed in the current manifest. The project nevertheless contains an OkHttp Gemini client and has no `offline`/`full` flavor split or `NetworkGateway` boundary yet.

---

## 18. Existing Test Classes (40 JVM test files; no androidTest source set found)

| Test Class | Subsystem |
|---|---|
| `IdentityModelTest` | Identity model |
| `Migration2To3Test` | DB migration |
| `ScanReconcileTest` | Scan reconciliation |
| `PermissionStateTest` | Permission state |
| `ArtworkKeyTest` | Artwork cache keys |
| `VisibleCountTest` | Track visibility query |
| `AudioTransitionControllerTest` | Fade controller |
| `SleepTimerEngineTest` | Sleep timer |
| `ErrorPolicyTest` | Error policy |
| `SeekGuardTest` | Seek bounds |
| `QueueModelTest` | Queue model |
| `QueueRestoreTest` | Queue restore |
| `SessionTrackerTest` | Statistics tracker |
| `SessionRecoveryTest` | Process death recovery |
| `MonthKeyTest` | Month key computation |
| `ClassicBackupTest` | Classic backup |
| `FullBackupValidationTest` | Full backup validation |
| `FullBackupRestoreTest` | Full backup restore |
| `OwnExportRoundTripTest` | Export round-trip |
| `BoxBlurTest` | Blur algorithm |
| `LrcParserTest` | Lyrics parser |
| `ThemeRegistryTest` | Player theme registry |
| `ImmersivePlayerTest` | Immersive layout |
| (12 more from prior phases) | Various |

**Actual source count:** 40 JVM test files; no `app/src/androidTest` directory was found. `./gradlew testDebugUnitTest` was attempted on 2026-11-03 and blocked before compilation by unaccepted SDK 35 licenses.

---

## 19. Known Issues and Incomplete Areas

1. **`player_layout_mode` default stored as `"modern_art"`** but file 18 requires `ATMOSPHERIC_QUEUE` as default and unknown fallback. Discrepancy to resolve.
2. **No `genre` column in `TrackEntity`** — spec requires genre field in schema v4.
3. **No `bitrate`, `sampleRate`, `bitDepth`, `codec`, `replayGain*`, `bpm`, `language`, `hasEmbeddedLyrics`** columns — required by spec's schema v4.
4. **No smart playlists table** — required for §3.4.
5. **No `track_tags`, `track_moods`, `track_meta`, `track_audio_profile`, `saved_queues`, `artist_images`, `sources`, `cache_entries`, `waveforms`** tables — required by spec.
6. **`play_events` missing `origin` column** — required for history import (C5).
7. **`lyrics` missing `source` and `format` columns** — required for multi-format lyrics.
8. **No product flavors** — spec requires `offline` and `full` flavors (C1). Currently one build, no INTERNET permission, but online Gemini code is still compiled into it.
9. **No live button-layout preset system** — 12+ presets required.
10. **Classic v2 audio copy feature** is MISSING.
11. **No Capabilities registry** — spec §5.3 requires it.
12. **No `NetworkGateway` or `com.pulse.player.net` package** — required for full flavor.
13. **Lyrics editor** is MISSING.
14. **Tag editor** (`NAM-L1`) is MISSING.
15. **Smart playlists** are MISSING.
16. **Advanced search** (typo-tolerant, ranked) is MISSING.
17. **Waveform seekbar** (`NAM-U3`) — `WaveformSeekBar.kt` exists but full PCM decode pipeline may be PARTIAL.
18. **Settings IA** does not match the spec's 13-section structure.
19. **`RECORD_AUDIO` is declared in the manifest and requested by transcription UI**, contrary to the file 18 visualizer rule.
20. **Classic Backup v2 is not implemented:** current Classic backup remains JSON/folder workflow without verified audio-copy export/import.
21. **Current build/test result is blocked by SDK license acceptance**, so source-level statuses remain UNVERIFIED where runtime evidence is required.
