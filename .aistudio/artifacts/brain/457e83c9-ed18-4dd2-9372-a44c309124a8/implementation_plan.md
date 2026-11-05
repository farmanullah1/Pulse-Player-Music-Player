# Classic Backup (Favourites JSON) Portable Folder & Parity Implementation Plan

Enhance the Classic Backup (Favourites JSON) feature to create an independent, portable backup inside `Music/Pulse Player`. It preserves the `pulseplayer.favorites` v1 JSON schema with fuzzy matching, includes all favorites (both original device tracks and tracks restored/imported from Full Backups), duplicates audio files safely into the folder without modifying originals, supports both automated MediaStore/filesystem creation and SAF folder selection, and provides live dual progress indicators (top banner and in-card progress bar) with robust error reporting.

---

## User Review & Critical Decisions

> [!IMPORTANT]
> The following product and architectural decisions were confirmed via interactive clarification:
>
> - **Progress Feedback**: Dual progress feedback — both a top operation banner and a live in-card progress bar during export showing active track copying, total count, and fractional percentage.
> - **Destination Access**: Both automated storage access (`MediaStore` on Android 10+ and standard public `Music/Pulse Player` on Android 9 and older) and SAF folder picker fallback if specialized permissions or custom storage locations are preferred.
> - **Duplicate Handling**: Safe deduplication — checks filename and byte size in `Music/Pulse Player`; if an identical copy already exists, it is reused rather than redundantly re-copied.
> - **Full Backup Parity**: Favourites restored or imported via Full Backup (with `sourceType = 'COPIED'`) are fully included in the Classic Backup export alongside standard library favorites.
> - **Zero Interference**: The Full Backup system (`CompatExport`, `CompatRestore`, `ArchiveValidator`) remains intact and unmodified.

---

## 1. Overview & Core Concept

- **What It Does**:
  1. **Export**: Gathers all current favorite tracks (including tracks imported/copied from Full Backups), generates the standard `pulseplayer.favorites.json` (schema v1), and copies each favorite audio file into `Music/Pulse Player`. Original audio files in their initial folders are completely untouched.
  2. **Import**: On any new or existing device, the user can select the `Music/Pulse Player` folder (or individual JSON file). The app scans/indexes the audio files and executes 5-tier fuzzy matching to restore all favorites, resolving strong matches, already-favorite tracks, and missing files with graceful reporting.
- **Key Value**: Provides an open, cross-device portable backup where user favorite music remains fully playable, backed up, and protected even if original music is deleted elsewhere.

---

## 2. User Experience & Visual Design

```
┌──────────────────────────────────────────────────────────────┐
│  ← Backup & Restore                                          │
├──────────────────────────────────────────────────────────────┤
│  [ Progress Banner: Copying track 14 of 48... (29%) ]         │
│  [==========================>                              ] │
├──────────────────────────────────────────────────────────────┤
│  ┌────────────────────────────────────────────────────────┐  │
│  │ ★ Classic Favourites & Portable Music Folder           │  │
│  │ Saves pulseplayer.favorites v1 JSON & copies all       │  │
│  │ favorite audio files to Music/Pulse Player.            │  │
│  │                                                        │  │
│  │ [ ↑ Export to Music/Pulse Player (48) ]                │  │
│  │                                                        │  │
│  │ ── Live Export Progress ─────────────────────────────  │  │
│  │ Copying: Bohemian Rhapsody.mp3 (14 / 48)               │  │
│  │ [████████████████░░░░░░░░░░░░░░░░░░░░░░░░] 29%         │  │
│  │                                                        │  │
│  │ [ 📁 Import Folder ]       [ ⬇ Import JSON ]           │  │
│  │                                                        │  │
│  │            Export JSON copy elsewhere                  │  │
│  └────────────────────────────────────────────────────────┘  │
└──────────────────────────────────────────────────────────────┘
```

### Key User Flows
1. **Preflight & Initiation**: User taps **Export to Music/Pulse Player**. A preflight dialog discloses that originals remain in place, and copies are safely placed in `Music/Pulse Player`.
2. **Export with Real-Time Progress**:
   - The user selects "Create Full Backup" (music + JSON).
   - The UI displays live progress in both the top `archiveOp` banner and an in-card progress bar, updating after each audio track copy.
   - Any individual failed file copies are logged in a structured report without aborting the rest of the batch.
3. **Import on New Device**:
   - User copies or moves `Music/Pulse Player` to a new device.
   - User taps **Import Folder** in Pulse Player and picks `Music/Pulse Player`.
   - Pulse Player scans and indexes the local tracks via `SafScanner`, loads `pulseplayer.favorites.json`, runs `FavoritesMatcher`, and displays a clear preview of matched tracks before committing.

---

## 3. Key Product Decisions & Trade-Offs

- **Decision 1: Streamed Copy with Real-Time Progress Callback**
  - *Chosen Approach*: `exportToPortableFolder` takes a progress callback `(current: Int, total: Int, trackTitle: String, progressFraction: Float) -> Unit` and updates the ViewModel state flow.
  - *Why*: Large audio libraries take time to copy. A live progress indicator prevents perceived freezes and provides transparency.
- **Decision 2: Universal Favorites Query (`allTracksOnce`)**
  - *Chosen Approach*: Favorites query in `FavoriteDao` joins `favorites` with `tracks` table without filtering out `sourceType = 'COPIED'`. Tracks imported from Full Backups are thus seamlessly exported into Classic Backups.
- **Decision 3: MediaStore + SAF Hybrid Creation**
  - *Chosen Approach*: On Android 10+ (API 29+), uses `MediaStore.Audio.Media.EXTERNAL_CONTENT_URI` with `RELATIVE_PATH = "Music/Pulse Player/"` and `IS_PENDING` flags. On API 28 and older, uses `Environment.getExternalStoragePublicDirectory`. If MediaStore writes are restricted or the user specifies a custom folder, SAF tree folder export is available as a fallback.

---

## 4. Technical Architecture & Component Structure

```
┌──────────────────────────────────────────────────────────────┐
│                      UI Presentation                         │
│  BackupScreen.kt ───(collects)──► MainViewModel              │
│  • Top Banner & ProgressIndicator                            │
│  • In-Card Live Progress (Fraction & Track Title)            │
│  • Preflight Modal & Error Reporting                         │
└───────────────────────────────┬──────────────────────────────┘
                                │ launches with progress callback
┌───────────────────────────────▼──────────────────────────────┐
│               ClassicBackup.kt (Coordinator)                 │
│  ┌────────────────────────┐    ┌──────────────────────────┐  │
│  │ FavoritesCodec         │    │ FavoritesMatcher         │  │
│  │ (v1 JSON encode/decode)│    │ (5-tier fuzzy match)     │  │
│  └───────────┬────────────┘    └─────────────▲────────────┘  │
│              │                               │               │
│  ┌───────────▼───────────────────────────────┴────────────┐  │
│  │ exportToPortableFolder(onProgress)                     │  │
│  │ 1. Fetch all favorites (MediaStore + Full Backup copies)│  │
│  │ 2. Write pulseplayer.favorites.json                     │  │
│  │ 3. Copy audio files (dedupe: skip identical length)    │  │
│  │ 4. Safely close streams & clear IS_PENDING             │  │
│  │ previewPortableFolder(treeUri)                         │  │
│  │ 1. Index audio files via SafScanner                   │  │
│  │ 2. Parse pulseplayer.favorites.json                    │  │
│  │ 3. Match against library index                        │  │
│  └────────────────────────────────────────────────────────┘  │
└───────────────────────────────┬──────────────────────────────┘
                                │ Room DB queries / inserts
┌───────────────────────────────▼──────────────────────────────┐
│              PulseDb (TrackDao, FavoriteDao)                 │
└──────────────────────────────────────────────────────────────┘
```

---

## 5. Verification Plan

1. **Unit Testing (`:app:testFullDebugUnitTest` and `:app:testOfflineDebugUnitTest`)**:
   - Verify `ClassicBackupTest` round-trip JSON serialization and fuzzy matcher ranking.
   - Add unit tests verifying progress callback emission and deduplication logic (identical file sizes skipped/reused).
2. **Compilation & Build**:
   - Run `gradle :app:compileFullDebugKotlin :app:compileOfflineDebugKotlin`.
   - Run `compile_applet` to confirm clean builds.
3. **End-to-End CUJ Check**:
   - Export favorites with sample track. Verify `Music/Pulse Player` contains `pulseplayer.favorites.json` and the `.mp3` copy.
   - Verify that favorites originating from Full Backup are included in the export.
   - Verify that folder import reads the files and restores the favorites list.
