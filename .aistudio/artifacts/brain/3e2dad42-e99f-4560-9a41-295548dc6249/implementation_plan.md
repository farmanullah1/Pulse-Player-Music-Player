# Classic Backup (Favourites JSON) Portable Export & Import with Download/Pulse Player Integration

An end-to-end enhancement of Pulse Player's portable favorites backup system that fixes the MediaStore primary directory error by targeting `Download/Pulse Player`, copies all favorite audio files alongside `pulseplayer.favorites.json` with progress tracking and deduplication, and provides seamless one-click auto-detect and SAF folder import.

## User Review & Critical Decisions

> [!IMPORTANT]
> The following product and technical decisions have been confirmed and integrated into this architecture:

- **Directory Targeting (`Download/Pulse Player`)**: Android 10+ (API 29+) MediaStore restricts `MediaStore.Files` to `Download` and `Documents`. Retargeting the primary backup destination from `Music/Pulse Player` to `Download/Pulse Player` via `MediaStore.Downloads` eliminates the `Primary directory Music not allowed for content://media/external/file` error while keeping both JSON and audio files in a single unified folder.
- **Audio Copy Deduplication**: During export, existing files in `Download/Pulse Player` with matching byte sizes are reused instead of being re-copied, saving battery and drastically reducing export time for recurring backups.
- **Dual Import Mode**: Users can either click **Auto-detect from Download/Pulse Player** for instant zero-picker restore on new devices, or use the **SAF Folder Picker** to select an external storage folder, USB drive, or SD card.
- **Full Backup Favorite Inclusion**: The Classic Backup queries all active favorites in Room (`PulseDb.favorites().allTracksOnce()`), automatically including tracks that originated from MediaStore, SAF folder scanning, or full backup restores.
- **Build Stabilization**: Remove conflicting product flavors (`offline`/`full`) in `app/build.gradle.kts` to resolve parallel KSP worker race conditions during Room schema verification and comply with AI Studio's single `app-debug.apk` requirement.

---

## 1. Overview & Core Concept

### What It Does
The Classic Backup feature enables users to take their favorite music and playback favorites metadata anywhere. When exported, the app:
1. Generates a standard `pulseplayer.favorites.json` file (v1 format) containing titles, artists, albums, durations, and original timestamps.
2. Creates byte-for-byte copies of all favorite audio files into the `Download/Pulse Player/` folder (or user-chosen SAF folder).
3. On a new device, scanning `Download/Pulse Player/` or importing the folder allows Pulse Player to auto-index the audio files into the local library and immediately reconstruct all favorites via confident/fuzzy matching.

### Target Audience & Persona
Music lovers who manage offline libraries, change devices, or want an independent, portable snapshot of their top tracks and favorite music files without needing cloud accounts.

### Key Value
- **Zero Loss**: If files are deleted from the main library, copies remain intact in `Download/Pulse Player/`.
- **Cross-Device Portability**: Copying the `Download/Pulse Player` folder to another phone allows instantaneous favorites restoration.
- **Rock-Solid Reliability**: Safe file stream handling, progress telemetry, and clear error reporting.

---

## 2. User Experience & Visual Design

### Key User Flows

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                            BACKUP & RESTORE SCREEN                          │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  Classic Backup (Favourites JSON)                                           │
│  Export your favorite songs & metadata to Download/Pulse Player             │
│                                                                             │
│  [ Export to Download/Pulse Player ]   [ Choose Custom Folder... ]           │
│                                                                             │
│  ─────────────────────────────────────────────────────────────────────────  │
│                                                                             │
│  Import Classic Backup                                                      │
│  Restore favorites from an exported folder or JSON file                     │
│                                                                             │
│  [ ✨ Auto-Detect (Download/Pulse Player) ]   [ Pick Folder via SAF... ]     │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

#### Export Flow
1. **Initiation**: User taps **Export to Download/Pulse Player** on the Classic Backup card.
2. **Progress Modal**: A Material 3 dialog appears with:
   - File title and artist currently being copied.
   - Animated `LinearProgressIndicator` (0% to 100%).
   - Step indicator (e.g. `Copying track 14 of 42 (33%)`).
   - Non-blocking asynchronous processing with cancellable coroutine.
3. **Completion**: Displays summary toast and card badge showing total tracks, newly copied tracks, reused duplicates, and any unreadable files.

#### Import Flow
1. **Auto-Detect Option**: Single tap on **Auto-Detect (Download/Pulse Player)** searches `Environment.DIRECTORY_DOWNLOADS/Pulse Player` for audio files and `pulseplayer.favorites.json`.
2. **Custom SAF Folder Option**: Lets the user choose an SD card, USB drive, or custom directory using Android's system document picker.
3. **Indexing & Match Preview**: Automatically scans and indexes the pasted audio files into the database, parses the JSON file, and opens the **Match Review Sheet** showing matched songs, ambiguous items, and missing files with one-tap restore.

### Visual Identity & Theme Tokens
- Surface Container: `MaterialTheme.colorScheme.surfaceContainer` with rounded 16.dp corners.
- Primary Action: `MaterialTheme.colorScheme.primary` filled button with download/folder icon.
- Progress Indicator: Accent primary color with smooth spring animations.
- Metric Chips: Tonal surface chips for `Copied`, `Reused`, and `Failed`.

---

## 3. Key Product Decisions & Trade-Offs

### Decision 1: Targeting `Download/Pulse Player` over `Music/Pulse Player`
- **Chosen Approach**: Use `MediaStore.Downloads` on Android 10+ (Q+) with `RELATIVE_PATH = "Download/Pulse Player/"`, and `Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)` on Android 9 and older.
- **Why**: Android's scoped storage rules strictly forbid non-audio MIME types (such as `application/json`) in `content://media/external/audio/` and reject primary directory `Music` for `MediaStore.Files`. `Download` natively allows mixed media types (both audio files and JSON metadata).
- **Trade-Off**: Users find their backup in `Download/Pulse Player` rather than `Music/Pulse Player`, which is standard for downloaded and exported backups on Android.

### Decision 2: Deduplication by File Size
- **Chosen Approach**: Before copying an audio file to `Download/Pulse Player`, query existing files with the same sanitized name. If a file exists and its size matches `track.sizeBytes`, mark it as `REUSED` and skip the byte copy.
- **Why**: When exporting a 500-song library repeatedly, re-copying gigabytes of data is slow and burns flash storage cycles. Deduplication makes subsequent backups finish in seconds.

### Decision 3: Build & Flavor Simplification
- **Chosen Approach**: Unify Gradle product flavors into a single standard debug build in `app/build.gradle.kts`, providing `NetworkGateway` directly and outputting `app/build/outputs/apk/debug/app-debug.apk`.
- **Why**: Eliminates parallel KSP compilation collisions on Room schema files and complies with AI Studio's expected APK location.

---

## 4. Technical Architecture & Data Strategy

### System Architecture & Data Flow

```
┌──────────────────────────────────────────────────────────────────────────────┐
│                                UI LAYER                                      │
│  BackupScreen.kt ─── MainViewModel.kt ─── ExportProgressState                │
└──────────────────────────────────────┬───────────────────────────────────────┘
                                       │
                                       ▼
┌──────────────────────────────────────────────────────────────────────────────┐
│                            BACKUP DOMAIN LAYER                               │
│                                                                              │
│  ClassicBackup.kt (FavoritesRepository)                                      │
│   ├── exportToDownloadFolder(onProgress)                                     │
│   ├── exportToCustomSafTree(treeUri, onProgress)                             │
│   ├── previewDownloadFolder()                                                │
│   └── previewPortableFolder(treeUri)                                         │
│                                                                              │
│  FavoritesCodec.kt (pulseplayer.favorites v1 encode/decode)                  │
│  FavoritesMatcher.kt (fuzzy matching: exact ID > norm Title/Artist > Levenshtein)
└──────────────────────────────────────┬───────────────────────────────────────┘
                                       │
                                       ▼
┌──────────────────────────────────────────────────────────────────────────────┐
│                           STORAGE & PERSISTENCE                              │
│                                                                              │
│  PulseDb (Room)                        Android Scoped Storage / MediaStore   │
│   ├── favorites().allTracksOnce()       ├── MediaStore.Downloads (Android Q+) │
│   └── tracks().allOnce()                └── SAF DocumentFile / Legacy File   │
└──────────────────────────────────────────────────────────────────────────────┘
```

### Component & State Breakdown

1. **`FavoritesRepository` (`ClassicBackup.kt`)**:
   - `PORTABLE_FOLDER_NAME = "Pulse Player"`
   - `PORTABLE_RELATIVE_PATH = "Download/Pulse Player/"`
   - `PORTABLE_JSON_NAME = "pulseplayer.favorites.json"`
   - `exportToDownloadFolder(onProgress: (current, total, title, fraction) -> Unit)`
   - `previewDownloadFolder(): Result<Preview>`
   - `previewPortableFolder(treeUri: Uri): Result<Preview>`
2. **`MainViewModel.kt`**:
   - `classicExportProgress: StateFlow<ExportProgressState?>`
   - `exportClassicPortable(customUri: Uri? = null)`
   - `importClassicFromDownloadFolder()`
3. **`BackupScreen.kt`**:
   - Classic Backup Card with dedicated "Export to Download/Pulse Player" and "Auto-Detect" buttons.
   - Progress dialog with active filename and progress bar.
   - Detailed result snackbar / dialog reporting copied vs. reused counts.

---

## 5. Verification Plan

### Automated & Compilation Verification
1. **Build Verification**: Run `compile_applet` to confirm single-flavor debug configuration compiles cleanly and generates `app-debug.apk`.
2. **Unit Tests**: Run `ClassicBackupTest`, `FavoritesCodecTest`, and `TrackMatchResolverTest` via `gradle :app:testDebugUnitTest`.

### Functional User Journey Verification
1. **Export Execution**: Verify favorites are encoded to `pulseplayer.favorites.json` and audio files are copied to `Download/Pulse Player/`.
2. **Progress Feedback**: Confirm the progress dialog updates smoothly with track name, count, and percentage.
3. **Deduplication Check**: Run export a second time and verify all tracks report as `reused` without file duplication.
4. **Auto-Detect Import**: Verify the app discovers the folder, matches audio files, and accurately restores favorites.
