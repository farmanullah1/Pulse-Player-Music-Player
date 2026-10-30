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
