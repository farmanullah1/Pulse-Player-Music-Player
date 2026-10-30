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
