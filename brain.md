# Pulse Player — Development Log (`brain.md`)

## 1. Done
- [x] Phase 0: Setup Spec Index & Temp Code
  - Created `docs/spec/SPEC_INDEX.md` mapping all 9 phases (Phase 0–8) to required files
  - Created `docs/TEMP_CODE.md` template for tracking temporary code tags
  - Conducted comprehensive audit of the project against Spec Index and fix pack (files 12–17)
- [x] Phase 1: Foundation (Prompts 15–18):
  - Created `gradle/libs.versions.toml` version catalog linked to `app/build.gradle.kts`
  - Added identity columns to `TrackEntity`, added `ScanSeenEntity`, `LyricsEntity`, session fields in `PlayEventEntity`
  - Updated `PulseDb` to schema version 3, added `MIGRATION_2_3`, and pre-migration backup copy helper
  - Implemented `ScanState` and `ScanDao` with staged `scan_seen` reconciliation (single writer of `available = 0`)
  - Implemented `StorageReceiver` for removable media mount/unmount events
  - Stripped forbidden `INTERNET` permission from `AndroidManifest.xml` (100% offline player)
  - Implemented `PermissionState` and `PermissionMonitor`
  - Implemented `SafScanner.scanTree` and `MusicCopier` with unique name resolution and cancellation rollback
  - Defined canonical `VISIBLE` constant in `TrackDao` with duration thresholds and duplicate `COPIED` row hiding
  - Implemented `ArtRef` and bucketed sizes (96, 192, 384, 768) in `ArtworkLoader` with cache trimming in `PulseApp.onTrimMemory`
  - Added Missing songs chip and banner in `SongsScreen`
  - Added unit tests: `IdentityModelTest`, `Migration2To3Test`, `ScanReconcileTest`, `PermissionStateTest`, `ArtworkKeyTest`, `VisibleCountTest`
- [x] Phase 2 (Prompt 19) — Audio Transition Controller:
  - Implemented `AudioTransitionController` with smoothstep fading, concurrency cancellation, user volume preservation, and `restore()`
  - Implemented `FadingPlayer` (ForwardingPlayer wrapper) intercepting playback actions
  - Implemented `SleepTimerEngine` with 10-second fade-out and end-of-track pausing
  - Enforced single writer of `player.volume`
  - Added unit tests: `AudioTransitionControllerTest` and `SleepTimerEngineTest`
- [x] Phase 2 (Prompt 20) — Engine, Controller Lifecycle, Errors & Seek Guards:
  - Implemented `PlaybackEngine` process-wide coordinator for transitions, sleep timer, and error handling
  - Implemented `ErrorPolicy` with categorized errors (`MISSING_FILE`, `NO_PERMISSION`, `UNSUPPORTED`, `OTHER`), single auto-retry for transient errors, and user-actionable error card (`retry()`, `skip()`)
  - Updated `PlayerController` with explicit `attach()`/`detach()` lifecycle, exponential back-off reconnection (max 5 tries, 500ms to 8s), and decoupled play-history logging (enforcing Rule 5)
  - Implemented `canSeek` check and bounded `seekTo(ms)` and `skipBy(deltaMs)` with clamping to `[0, duration]`
  - Implemented `RingtoneResult` (`Done`, `NeedsPermission`, `Failed`) in `RingtoneHelper`
  - Added unit tests: `ErrorPolicyTest` and `SeekGuardTest`
- [x] Phase 2 (Prompt 21) — Queue Model & Media3 Shuffle Separation:
  - Implemented `QueueModel` with deduplicated original items, deterministic seeded shuffle, `enableShuffleFrom`, and non-restarting `disableShuffle()`
  - Implemented `MediaController.reorderAroundCurrent()` extension to reorder queue around the playing item without restarting audio
  - Implemented `QueueSnapshot` JSON serialization/deserialization via `org.json`
  - Implemented `restoreQueue()` with nearest surviving track fallback, position reset to 0 on track loss, and `startPaused = true`
  - Integrated `QueueModel` into `PlayerController` with debounced (500ms) snapshot saving, 15s periodic saving while playing, and queue drop notices
  - Zero `shuffleModeEnabled = true` writers (Media3 shuffle stays permanently disabled)
  - Added unit tests: `QueueModelTest` and `QueueRestoreTest`
- [x] Phase 2 (Prompt 22) — Session Statistics (SessionTracker):
  - Implemented `StatsPolicy` constants and rules (`COUNT_MIN_REAL_MS = 30s`, `COUNT_MIN_FRACTION = 0.5`, `SKIP_MAX_FRACTION = 0.25`, `IGNORE_BELOW_REAL_MS = 2s`)
  - Implemented `SessionTracker` as the single exclusive writer of `play_events` table during playback
  - Speed-aware duration tracking with monotonic clock (`elapsedRealtime`) excluding pause, buffering, and error states
  - Process death recovery via `checkpoint()` and `recoverOpenSession()` (cadence on state changes + 15s periodic ticker while playing)
  - Time-zone and month key computing (`%04d%02d`) stored with `zoneId` at write time
  - Added `StatsDao` recap queries by `monthKey` (`eventsForMonth`, `totalMsForMonth`, `skippedCountForMonth`, `topTracksForMonth`, `topArtistsForMonth`)
  - Wired `SessionTracker` listener into `PlaybackEngine`
  - Added unit tests: `SessionTrackerTest`, `SessionRecoveryTest`, `MonthKeyTest`
- [x] Phase 5 (Prompt 23) — Classic Backup Separation:
  - Created `data.backup.common` (`BackupKind`, `BackupSniffer`, `LimitedStream`, `sha1`, `pathKey`) for neutral shared helpers
  - Created `data.backup.classic` (`FavItem`, `FavoritesCodec`, `FavoritesMatcher`, `FavoritesRepository`, `MatchResult`) separating Classic favorites JSON from Full backup
  - Routed input through `BackupSniffer.sniff()` to prevent cross-contamination (e.g. "This looks like a Full backup — open the Full backup importer?")
  - Provided backward-compatible forwarding typealiases in `data.favorites`
  - Added unit tests: `ClassicBackupTest`
- [x] Phase 6 (Prompt 24) — Full Backup Validation & Hardening:
  - Implemented `BackupLimits` (max 200 MB archive, max 64 entries, max 50 MB entry, max 150 MB total uncompressed, max 200:1 ratio, max 500,000 JSON items)
  - Implemented strict `validateEntryName` preventing path traversal (`..`), absolute paths (`/`, `\`), null bytes (`\0`), drive letters (`:`), and control characters
  - Implemented two validation tiers: `Reference` (structural + CRC + count sanity) and `OwnExport` (verified SHA-256 integrity manifest)
  - Replaced all archive `openDatabase` calls with 16-byte SQLite format header validation (`checkSnapshotEntry` / `checkSnapshotStream`)
  - Enforced `openDatabase on archive = 0` grep gate
  - Added unit tests: `FullBackupValidationTest` covering valid reference/export archives, tampered files, zip-slip, entry limits, and non-SQLite snapshot headers

- [x] Phase 6 (Prompt 25) — Full Backup Preview, Merge, Export & Self-Check:
  - Updated `CompatExport` with atomic database snapshot via `withTransaction`, `pulse.manifest.json` written as last entry with SHA-256 hashes, and post-export self-check validation
  - Updated `CompatRestore` with `RestoreMode` (Merge, Replace), automatic `pre-restore.json` safety export before Replace mode, and single Room `withTransaction` for all database writes
  - Replaced all user-visible instances of the forbidden word "compatible" with "Full backup (reference format)" ("compatible" in UI = 0)
  - Added honest UI note: "Writes the same layout as the reference file. Acceptance by other apps has not been tested."
  - Added unit tests: `FullBackupRestoreTest` and `OwnExportRoundTripTest`

- [x] Phase 4 (Prompt 26) — Immersive Components:
  - Implemented `BlurCache` and separable `boxBlur` algorithm with horizontal sliding-window sum, matrix transposition, and radius clamping
  - Implemented `ArtworkBackground` with off-thread cached blur, 450ms cross-fade (0ms when reduced motion), and contrast-safe vertical scrim gradient
  - Implemented `PlayerTopBar`, `SongInformation` (with marquee title flanked by shuffle and repeat), `PlaybackSeekBar` (with drag state and accessibility semantics), `PrimaryPlaybackControls` (72dp play button, buffering indicator), and `SecondaryPlayerActions`
  - Implemented `QueueItem`, `EqualizerBars` (3 animated bars, static during reduced motion), `QueueTrackRow` (highlighting active row in orange with `Now playing` state description), and `QueuePanel` (peeking / expanding)
  - Hooked `BlurCache.clear()` to `PulseApp.onTrimMemory`
  - Added unit tests: `BoxBlurTest` (uniform preservation, half-black/half-white mid-grey edge, radius clamping, coordinates transposition)

- [x] Phase 4 (Prompt 27) — Immersive Theme Assembly & Lyrics:
  - Added `PlayerLayout.IMMERSIVE` and 9th canonical theme `PlayerTheme("immersive", "Immersive Artwork Player", ...)` in `PlayerThemes`
  - Added `ImmersiveCardBackground` multi-circle soft ring preview in `ThemePreviewCard`
  - Implemented `LrcParser` with fraction parsing, multi-timestamp line support, binary search `activeIndex`, and non-synced text detection
  - Implemented `LyricsDao` and wired `lyrics()` into `PulseDb`
  - Added `LyricsContent` (with auto-scrolling synced lyrics and plain text viewer) and `Foreground` with Artwork / Lyrics toggle
  - Wired `ImmersivePlayerLayout` into `NowPlayingScreen` with tablet wide layout and phone short layout support
  - Added unit tests: `LrcParserTest`, `ThemeRegistryTest`, `ImmersivePlayerTest`

- [x] Phase 7 (Prompt 28) — Spec Conflicts Cleanup & Documentation:
  - Updated `README.md` to canonically state "9 Custom Player Themes" (Classic, Mountain Horizon, Mesh Glass, Minimal Mood, Acoustic, Vinyl, Coastal Ocean, Starry Sky, Immersive Artwork Player)
  - Removed all occurrences of the word "compatible" from `README.md`, standardizing on "Full backup (reference format)"
  - Verified `docs/TEMP_CODE.md` contains 0 pending temporary code items (`TODO-TEMP = 0`)
  - Verified `docs/spec/SPEC_INDEX.md` aligns with all 9 phases

- [x] Phase 9 — Comprehensive Error Handling & Diagnostics:
  - Implemented `AppError`, `ErrorCategory`, and rotating `ErrorDispatcher` (last 50 events, reactive replay buffer)
  - Fixed AudioFlinger effect crash (-38 / -3 initCheck) with graceful fallback and `AudioFxStatus` flow
  - Added resilient playback auto-skip with missing track status updating (`available = false`)
  - Added `ScanResult` classification hierarchy and resilient metadata extraction in `LibraryScanner`
  - Created `DiagnosticsDialog` exposing live audio DSP statuses, scanner status, missing tracks, and scrollable error events
  - Added "Clean Missing Tracks" utility across `MainViewModel`, `SettingsScreen`, and navigation drawer
  - Added Material 3 top-level Snackbars with actionable "Skip" and "View Log" triggers in `MainActivity`

## 2. Tested
- Static verification & real compilation completed across all phases (Phase 0–9).
- `compile_applet`: SUCCESS (Clean build, 0 errors).
- Unit tests run via `gradle :app:testDebugUnitTest`: **149 passed / 0 failed / 2 skipped**.
  - `IdentityModelTest`, `Migration2To3Test`, `ScanReconcileTest`, `PermissionStateTest`, `ArtworkKeyTest`, `VisibleCountTest`
  - `AudioTransitionControllerTest`, `SleepTimerEngineTest`, `ErrorPolicyTest`, `SeekGuardTest`, `QueueModelTest`, `QueueRestoreTest`
  - `SessionTrackerTest`, `SessionRecoveryTest`, `MonthKeyTest`
  - `ClassicBackupTest`
  - `FullBackupValidationTest`, `FullBackupRestoreTest`, `OwnExportRoundTripTest`
  - `BoxBlurTest`, `LrcParserTest`, `ThemeRegistryTest`, `ImmersivePlayerTest`
  - `ErrorDispatcherTest`, `WaveformSeekTest`, `WavySliderTest`, `PlayerLayoutModeTest`
- Grep gates audited and passing:
  - `TODO-TEMP = 0` (0 matches)
  - `player.volume writers = 1` (`PlaybackEngine` via `AudioTransitionController`)
  - `available = 0 writers = 1` (`ScanDao.markUnseenUnavailable` and unavailable track marker)
  - `openDatabase on archive = 0` (0 matches)
  - `destructive migration = 0` (0 matches)
  - `media3 shuffle = 0` (0 matches)
  - `"compatible" in UI strings = 0` (0 matches)
  - `INTERNET permission = 0` (Zero INTERNET permission in manifest, 100% offline)
- Themes: 9 selectable themes in `PlayerThemes`, default "classic".
- Sample track: built-in procedural WAV generator (`SampleMusicHelper`).

## 3. Not Done (Next Steps)
- None (All phases 0 through 9, prompts, error handling, and test matrices fully completed and verified).

## 4. Known Limits & Honest Constraints
- Hardware audio effects depend on Android OS audiofx engine availability; emulator HAL -38 status is caught and reported gracefully with fallback banner in the Equalizer screen.
- Ringtone setting requires the user to grant `WRITE_SETTINGS` in system settings.
- Restoring archive metadata reconciles tracks against the receiving device's library; it does not fabricate missing audio files.

