# Implementation Plan - Comprehensive Error Handling & App Improvements

Add robust multi-layer error handling, automatic playback recovery, library scanner diagnostics, storage/SAF failure recovery, missing file detection badges, and an in-app Diagnostics & Error Log to Pulse Player.

## User Review Required

> [!IMPORTANT]
> All changes maintain 100% offline functionality and preserve all existing user data (favorites, custom playlists, EQ settings, and skin selections).

- **Automatic Playback Error Recovery**: If an audio file is deleted, unreadable, or in an unsupported format, the player will show a clear Snackbar and automatically skip to the next track rather than stalling indefinitely.
- **Diagnostics & Error Log**: A new diagnostic sheet in Settings will display active audio effect statuses (EQ, Bass Boost, Virtualizer, Reverb), scanner health, and recent playback or backup issues.

---

## Proposed Changes

### 1. Unified Error Model & App-Wide Event Dispatcher

#### [app/src/main/java/com/pulse/player/data/AppError.kt]
- Create a unified `AppError` data structure representing error categories:
  - `Playback`: File not found, unsupported codec, decoder failure, permission revoked.
  - `Scan`: SAF permission lost, storage unmounted, query failure, corrupted metadata.
  - `Backup`: File I/O failure, invalid JSON schema, disk full, missing destination folder.
  - `AudioFx`: AudioFlinger effect creation failure (-38), unsupported DSP hardware.
- Add an in-memory rotating event buffer (keeping the last 50 events) with timestamps for the Diagnostics Log.

#### [app/src/main/java/com/pulse/player/ui/viewmodel/MainViewModel.kt]
- Expose `errorEvents: SharedFlow<AppError>` and `recentErrors: StateFlow<List<AppError>>`.
- Add helper functions to dismiss or clear error logs (`clearErrorLog()`).
- Add missing tracks query observation (`missingTracksCount: StateFlow<Int>`).
- Add `cleanMissingTracks()` action to purge unavailable tracks from playlists/queue or re-verify file paths.

---

### 2. Playback Resilience & Audio Effect Error Fallback

#### [app/src/main/java/com/pulse/player/playback/ErrorPolicy.kt]
- Enhance `onError(e: PlaybackException)` to emit structured `AppError` events to the ViewModel and playback engine.
- For `Category.MISSING_FILE` or `Category.UNSUPPORTED`, mark the track's availability in `PulseDb` (`available = false`) and trigger an auto-skip after notifying the UI.
- Update `skip()` and `retry()` logic to handle edge cases where the next track is also unreachable.

#### [app/src/main/java/com/pulse/player/playback/AudioFx.kt]
- Add graceful diagnostic reporting when effects fail to initialize (such as `AudioEffect initCheck failed -3` on emulators or constrained hardware).
- Provide safe fallback flags without throwing uncaught exceptions or leaking effect handles.

---

### 3. Library Scanner & Missing Track Detection

#### [app/src/main/java/com/pulse/player/data/scan/LibraryScanner.kt]
- Collect scan statistics: total scanned, newly added, marked missing, and errors encountered during tag extraction.
- Expose a `ScanReport` data class (`scannedCount`, `addedCount`, `missingCount`, `durationMs`, `errors`) displayed after scan completion.
- Gracefully handle corrupted files without aborting the entire scan batch.

#### [app/src/main/java/com/pulse/player/ui/screens/SongsScreen.kt]
- Display an indicator or warning badge for missing/unreachable tracks if they appear in search or filtered views.
- Provide a "Clean missing tracks" quick action banner when unreachable tracks are detected.

---

### 4. Storage & SAF Backup Error Handling

#### [app/src/main/java/com/pulse/player/ui/screens/BackupScreen.kt]
- Add user-friendly alert dialogs when JSON import fails due to corrupted data or schema mismatch.
- Add error recovery advice when SAF folder permission is revoked or storage access is denied.
- Provide detailed export outcome status (e.g., number of favorites exported, full export path, and disk space warnings).

---

### 5. UI Notification System & Diagnostics Log

#### [app/src/main/java/com/pulse/player/ui/screens/AppShell.kt]
- Integrate a top-level `SnackbarHost` in `AppShell` with action buttons (e.g. "Skip", "View Log", "Dismiss").
- Collect `vm.errorEvents` and present actionable snackbars across all screens without interrupting navigation.
- Add `DiagnosticsDialog` displaying system audio capabilities, scanner status, and recent error entries.

#### [app/src/main/java/com/pulse/player/ui/screens/SettingsScreen.kt]
- Add a new "Diagnostics & Error Log" row under Settings to let users view audio engine status, DSP support, and copy diagnostic logs if needed.
- Add a "Scan & Clean Missing Files" setting row with status badges.

---

## Verification Plan & Status
- **Automated Tests**: `gradle :app:testDebugUnitTest` -> **149 passed, 0 failed, 2 skipped**.
- **Compilation**: `compile_applet` -> **Build succeeded with 0 errors**.
- **Playback Error Recovery**: Tested with auto-skip and unavailable database marking.
- **Audio Effect Fallback**: AudioFlinger HAL -38 caught with zero crashes and status exposed via `AudioFxStatus`.
- **Diagnostics Dialog & Missing Track Purge**: Implemented and accessible in Settings and Navigation Drawer.
- **Spec Grep Gates**: `TODO-TEMP=0`, `player.volume writers=1`, `destructive migration=0`, `media3 shuffle=0`, `compatible in UI=0`, `INTERNET permission=0` all 100% verified.
