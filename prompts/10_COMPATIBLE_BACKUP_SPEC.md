# 10 — Compatible Backup & Restore (reads/writes the sample "ManualBackup_*.wav" format)

> **Extends the Pulse Player spec pack (00–09). Does not replace anything.** The existing favourites-JSON export/import (file 07) stays exactly as it is and is called **Classic backup** in the UI. This file adds a second method, **Full backup (reference format)**, that reads and writes the same structure as the user's real sample file.
>
> This file replaces the earlier ChatGPT "Dual Backup" prompt. That prompt invented its own `.musicbackup` format (`manifest.json`, `checksums.json`, …) instead of following the sample, so a real sample file would **not** have imported. **Do not use `.musicbackup`, `manifest.json` or `checksums.json`.** Follow only the format documented below.
>
> Read this file (format + rules), then `11_COMPATIBLE_BACKUP_CODE_AND_PROMPTS.md` (code + step prompts).

## 1. What the sample file really is (verified by opening it)
`ManualBackup_20261028_172204175.wav` is **not audio**. Its first bytes are `50 4B 03 04` ("PK") — it is a **standard ZIP** (deflate, UTF-8 names, no comment, no folder entries) renamed to `.wav`. It was made by another music player (versionName `4.4.1.189`, versionCode `189`) on an Infinix X6881. It contains **only metadata and statistics, no audio files**.

Entries, in the order the source app writes them:

| # | Entry | Format | What it holds (sample numbers) |
|---|-------|--------|-------------------------------|
| 1 | `settings.bak` | JSON object | Category flags `isBackupMusicTags, isBackupCovers, isBackupLyrics, isBackupPlaylists, isBackupSortSettings, isBackupScanHideSettings, isBackupMusicRecap` (booleans) + `device`, `versionName`, `versionCode` |
| 2 | `Covers/MusicCoversBackupConfig.conf` | JSON array, 13,509 items | `{"_data": <abs path>, "resource": null, "cover_url": null}` |
| 3 | `Covers/AlbumCoversBackupConfig.conf` | JSON array, 6,563 items | `{"album": <name>, "album_art": "content://media/external/audio/albumart/<id>", "resource": null, "modified_album_art": null}` |
| 4 | `MusicTagsBackupConfig.conf` | JSON array, 13,509 items | `{"_data": <abs path>, "title", "artist" ("<unknown>" when missing), "album", "track": int, "genres": null or string}` |
| 5 | `Lyrics/LyricsBackupConfig.conf` | JSON array | `[]` (empty in the sample; non-empty schema unknown) |
| 6 | `PlayLists/PlayListBackupConfig.conf` | JSON array, 41 playlists | `{"name", "sort": {"key": "playlistPlayOrder", "order": -1}, "cover": null, "musics": [{"path": <abs path>}, …]}`. The playlist named **`My Favourite`** is the favourites list (4,803 paths). The other 40 are mostly empty. |
| 7 | `SortBackupConfig.conf` | JSON object | Keys `All_Song, Directory_, Folder_Song, Album_, Album_Song, Artist_, Artist_Song, Genre_, Genre_Song`, each `{"key": <field>, "order": -1 or 1}`. `-1` = descending, `1` = ascending. Keys seen: `dateAdded, songCount, sortTrackNumber, numSongs`. |
| 8 | `ScanHiddenBackupConfig.conf` | JSON object | `size: -1`, `duration: -1`, `device`, `exclude: [{"_id": int, "data": <abs folder>, "type": 1}, …]` (17 folders), `hidden_songs: [{"path", "is_hidden": 0}, …]` (13,509). Paths may contain `\/` escapes — any JSON parser reads them. |
| 9–13 | `Statistics/snapshot_monthly_YYYYMM.db` | SQLite files | One per **completed** month (here 202605–202609). Tables below. |
| 14 | `Statistics/StatisticsSnapshotConfig.json` | JSON object | `{"snapshots": [{"type": "monthly", "value": "202605", "filename": "snapshot_monthly_202605.db"}, …], "initApplicationTimestamp": <epoch ms>}` |
| 15 | `Statistics/PlaybackRecords.json` | JSON array, 2,319 records | One object per play (keys in §2.3). It is a **superset** of the snapshots: the 5 snapshots hold 2,113 of them and the JSON also holds the 206 from the current month. No duplicate `(song_id, timestamp)` pairs. |
| 16 | `metadata.inf` | JSON object (written last) | `backupTime` (`yyyyMMdd_HHmmssSSS`, local time), `mode: "MANUAL_GLOBAL"`, `type: "GLOBAL"`, `isAutoBackup: false`, `deviceId`, `versionName`, `versionCode`, `settings: {…same 7 flags…}`, `songs` (13509), `playlists` (41) |

### 1.1 Snapshot database schema (exact; all 5 snapshots identical, `user_version` 0)
```sql
CREATE TABLE statistics_playback(_id INT, song_id INT, timestamp INT, day TEXT, looper_count INT, source_type TEXT, source_id TEXT, play_duration INT, play_count INT, device TEXT, skip_count INT, extra_int_1 INT, extra_int_2 INT, extra_int_3 INT, extra_text_1 TEXT, extra_text_2 TEXT, extra_text_3 TEXT);
CREATE INDEX idx_song_day_source ON statistics_playback(song_id,day,source_type,source_id);
CREATE INDEX idx_statistics_timestamp ON statistics_playback(timestamp);
CREATE INDEX idx_statistics_day ON statistics_playback(day);
CREATE INDEX idx_statistics_song_id ON statistics_playback(song_id);

CREATE TABLE statistics_musics(_id INT, title TEXT, album_id INT, album TEXT, artist_id INT, artist TEXT, duration INT, _data TEXT, cover_url TEXT, is_hidden INT, genre_id, genre TEXT, data_modified INT, play_count, play_duration, skip_count, looper_count, source_type TEXT, source_id TEXT, device, timestamp, day TEXT, extra_int_1, extra_int_2, extra_int_3, extra_text_1, extra_text_2, extra_text_3);
CREATE INDEX idx_statistics_musics_timestamp ON statistics_musics(timestamp);
CREATE INDEX idx_statistics_musics_day ON statistics_musics(day);

CREATE TABLE statistics_albums(_id INT, album TEXT, artist TEXT, artist_id INT, album_art TEXT, modified_album_art TEXT, recorded_song_count, total_play_duration, total_play_count, timestamp, day TEXT);
CREATE INDEX idx_statistics_albums_timestamp ON statistics_albums(timestamp);
CREATE INDEX idx_statistics_albums_day ON statistics_albums(day);

CREATE TABLE statistics_artists(_id INT, artist TEXT, artist_art TEXT, recorded_song_count, recorded_album_count, total_play_duration, total_play_count, timestamp, day TEXT);
CREATE INDEX idx_statistics_artists_timestamp ON statistics_artists(timestamp);
CREATE INDEX idx_statistics_artists_day ON statistics_artists(day);

CREATE TABLE statistics_genres(_id INT, name TEXT, recorded_song_count, recorded_artist_count, recorded_album_count, total_play_duration, total_play_count, timestamp, day TEXT);
CREATE INDEX idx_statistics_genres_timestamp ON statistics_genres(timestamp);
CREATE INDEX idx_statistics_genres_day ON statistics_genres(day);
```
Plus Android's own `android_metadata(locale TEXT)` table (Android creates it automatically; never create it by hand). In the sample, `statistics_musics` has one row per (song, day) and `statistics_albums/artists` have one row per (name, day) with running totals.

### 1.2 `PlaybackRecords.json` record (exact key set, all 17 keys always present)
```json
{"song_id": 0, "timestamp": 0, "day": "YYYYMMDD", "looper_count": 0, "source_type": "", "source_id": "",
 "play_duration": 0, "play_count": 1, "device": "<phone model>", "skip_count": 0,
 "extra_int_1": 0, "extra_int_2": 0, "extra_int_3": 0, "extra_text_1": null, "extra_text_2": null, "extra_text_3": null,
 "_data": "<abs path>"}
```
This is a **shape illustration only** (never ship these values). `timestamp` = epoch milliseconds, `day` = local date `yyyyMMdd`, `play_duration` = milliseconds actually listened, `play_count` is mostly 1 (sometimes 2–3 = repeats). `song_id` is the **source phone's MediaStore-style id** (10 digits starting `1000…`): it is **not portable** and must never be used to find a track.

## 2. Hard facts that shape the design
1. **Nothing in this format identifies a song portably except its path and its tags.** Playlists and hidden-song lists contain **only absolute paths**. Tags carry title/artist/album/track. There is **no duration, size or hash**. So matching = path first, then metadata (see §5).
2. **Paths are phone-specific**: `/storage/emulated/0/...` (internal) or `/storage/XXXX-XXXX/...` (SD card). On another device or after files move they will not exist. Matching must tolerate that and **report** what could not be matched.
3. **The source library is wider than Pulse Player's.** The sample lists 13,509 files including notification sounds, WhatsApp voice notes and ringtones. Pulse Player's scan skips ringtones/notifications/alarms and clips shorter than the minimum duration, so many entries will legitimately not match. The restore report must say so ("not in this app's library — filtered or missing"), not call it a failure.
4. **Covers are not portable**: album `content://media/external/audio/albumart/<id>` URIs belong to the source phone; every `resource`/`cover_url`/`modified_album_art` in the sample is `null`. Never trust or open them.
5. **Lyrics schema is unknown** (the sample's file is `[]`). Do not guess a non-empty schema: import only an empty array; for a non-empty array skip and report "lyrics format not recognised".
6. `My Favourite` = favourites. Pulse Player keeps favourites in its own `favorites` table, not as a playlist, so this one playlist is routed there (§4).
7. **Compatibility honesty.** Reading this sample is fully specified above and testable. Writing a file the *other* app will accept is a best effort that follows the same layout; it cannot be proven here. The UI and `brain.md` must say: "Imports the original file format; exported files follow the same layout; acceptance by the other app is untested."

## 3. UI (extends the Backup & Transfer screen from file 07)
Two clearly separated cards plus one smart entry:
- **Classic backup (favourites JSON)** — unchanged from file 07. Label it "Classic · favourites only".
- **Full backup (reference format)** — "Saves favourites, playlists, play history, sort and scan settings in one file you can move to another phone. Music files are **not** included."
  - Category checkboxes mirroring the 7 flags: Music tags · Covers · Lyrics · Playlists & favourites · Sort settings · Scan & hidden settings · Listening history (recap). Each shows its real record count.
  - **Create backup** → SAF `CreateDocument`, suggested name `ManualBackup_<yyyyMMdd_HHmmssSSS>.wav` (same naming as the sample) with a switch "Save as .zip instead". Use MIME `application/octet-stream` and show the name the provider actually saved.
  - **Restore backup** → SAF `OpenDocument` with `*/*` (providers label this file as audio; never filter by MIME).
  - **Last backup** card (file name, time, size, counts) written **only after** a verified write.
- **Smart restore button** (optional but recommended): one picker that sniffs the file and routes it — favourites JSON → Classic restore; ZIP → Full restore; real WAV audio → "This is an audio file, not a backup."
Restore screen: detected format, source app version + backup time (`metadata.inf`), which categories are present with counts, warnings, category checkboxes, **Merge** (default) or **Replace selected**, a confirmation dialog for Replace, progress, and the final report (§6).

## 4. Mapping: sample categories ↔ Pulse Player data
| Flag / file | Export (Pulse Player → file) | Restore (file → Pulse Player) |
|---|---|---|
| Music tags (`MusicTagsBackupConfig.conf`) | One item per available track: `_data` = absolute path (see §7), title, artist (`"<unknown>"` when ours is "Unknown artist"), album, `track`, `genres: null` | **Used to improve matching** (path → title/artist/album hints). Pulse Player does not edit tags, so nothing is written back; report "read N tags, used for matching". |
| Covers (both files) | One item per track with `resource: null, cover_url: null`; one per album with `album_art: null, resource: null, modified_album_art: null` (never export our `content://` ids) | Parsed and **ignored**; report "covers are device-specific and were not applied". |
| Lyrics | `[]` | Empty array accepted; non-empty skipped with a report. |
| Playlists | `My Favourite` first (our favourites, array order = our display order), then every user playlist: `sort` = our stored sort (default `{"key":"playlistPlayOrder","order":-1}`), `cover: null`, `musics` in position order | `My Favourite` (case-insensitive; also "My favorite", "My favorites") → favourites table. Others → playlists by name. Array order is preserved. |
| Sort settings | `All_Song` from the Songs-tab sort; any raw keys we received earlier are written back unchanged; the rest default as in the sample | Known keys applied (`All_Song`: dateAdded/title/artist/duration/…), **all** raw keys stored in DataStore so a later export round-trips them. |
| Scan & hidden | `size: -1`, `duration: -1`, `device` = model label, `exclude` from our excluded folders (`_id` sequential, `type: 1`), `hidden_songs` = every track with `is_hidden: 0` | `exclude` → our excluded folders (convert absolute path to the relative form our scanner uses, §7). `size`/`duration`: unit unknown → **not applied**, reported. `hidden_songs` with `is_hidden: 1` → skipped and reported (feature not supported). |
| Listening history | `PlaybackRecords.json` = every row of `play_events` joined to its track path; plus one monthly snapshot `.db` for each **completed** month that has events, plus `StatisticsSnapshotConfig.json` | Import `PlaybackRecords.json` (the superset). Snapshots are **validated only** (SQLite header + opened read-only from a temp copy), never merged into the live database and never used to replace a database file. |

## 5. Track matching (target device)
Index the current library once (hash maps), then resolve each source path in this order; **stop at the first tier that yields exactly one track**:
1. **Exact path** (case-insensitive, Unicode NFC).
2. **Storage-relative path**: strip the volume prefix (`/storage/emulated/<n>/`, `/storage/XXXX-XXXX/`, `/sdcard/`) and compare the rest — handles SD card ↔ internal and different user ids.
3. **Parent folder name + file name** (last two path segments).
4. **Tags**: normalised title + artist + album from the sample's tag entry for that path (treat `<unknown>`/blank artist as "Unknown artist", blank album as "Unknown album"). Requires title match **and** artist/album match (unknown matches unknown).
5. **File name only**, accepted only if exactly one track has that name.
If a tier yields **more than one** track → mark **ambiguous** and do not guess; list it in the report. If nothing matches → **unmatched** with a reason (`not in library` / `filtered by scan rules` cannot be told apart reliably — just say "not found on this device").
Never use `song_id`. Never claim a match for title-only.

## 6. Restore rules (safe, predictable)
1. **Sniff** the file's first bytes: `50 4B 03 04` (also `50 4B 05 06`) = ZIP; `52 49 46 46 .... 57 41 56 45` (RIFF…WAVE) = real WAV audio → reject; `{` = JSON (Classic). Ignore the extension and MIME type.
2. Validate as **untrusted**: copy the stream to a bounded temp file (max 512 MB), open with `ZipFile`; max 64 entries; declared and **actually read** bytes per entry ≤ 256 MB and total ≤ 768 MB; reject absolute paths, `..`, `\`, NUL, duplicate names; never extract to disk except the snapshot `.db` copies in `cacheDir`. Stream-parse JSON (`android.util.JsonReader`); don't build one giant string.
3. The archive is accepted as a backup only if it has `metadata.inf` **and** at least one known config entry from §1. Unknown extra entries are ignored. A missing optional category is "not included". Missing `metadata.inf` → reject ("not a recognised backup").
4. **Preview before any change** (no DB writes): backup time, source version, counts per category (`songs`, `playlists` from `metadata.inf`, real counted arrays), warnings, and a dry-run match count (matched / ambiguous / unmatched).
5. **Merge** (default): add what is missing, keep everything else. **Replace selected**: for each ticked category replace our data, only after a confirmation dialog, and only categories that actually exist in the file. Replace applies to favourites, playlists and play history only; never touches tracks or music files.
6. **Stage first, commit once**: parse and resolve everything in memory, then apply in **one Room transaction** (`db.withTransaction`). Failure or cancel before commit = zero changes.
7. **No duplicates on repeat**: play events dedupe on `(trackId, playedAtMs)` via a unique index (migration in file 11 §2); playlist tracks already have a composite primary key; favourites use `IGNORE`. Restoring the same file twice must change nothing the second time.
8. Never move, rename, delete or modify audio files. Never open any path or `content://` URI from the file.
9. **Real report** (shown after commit): per category imported / already present / skipped, plus matched-by-tier counts, **ambiguous**, **unmatched** (offer "Save unmatched list" as a plain text file via SAF), and warnings. Never print "restored successfully" if anything was unmatched without saying how many.

## 7. Paths in Pulse Player
Pulse Player's tracks currently store `RELATIVE_PATH` + `displayName`, not an absolute path. The compatible format needs absolute paths, so:
- Add `tracks.dataPath TEXT NULL` filled by the scanner from `MediaStore.Audio.Media.DATA` (deprecated but still returned for files the app can read). If it is null/blank, **compose** it: for the primary volume `"/storage/emulated/0/" + RELATIVE_PATH + DISPLAY_NAME`; for other volumes use `MediaStore.Audio.Media.VOLUME_NAME` → `"/storage/<VOLUME_NAME uppercase>/" + …`. For SAF-scanned tracks use the document's decoded path when derivable, else leave null and **omit** those tracks from path-based exports (count them in warnings).
- Our excluded-folder setting stores the **relative** form the scanner compares (`Notifications`, `Android/media/com.whatsapp/WhatsApp/Media/WhatsApp Audio`). Convert absolute ↔ relative with the same prefix stripping as §5 tier 2.

## 8. Acceptance (use the user's real sample on the user's phone)
The sample was made on the same phone model, so many `/storage/emulated/0/...` paths should match exactly.
- Preview of the sample shows: tags 13,509 · playlists 41 · favourites entries 4,803 · play records 2,319 · snapshots 5 (all valid) · lyrics 0 · exclude folders 17.
- Real `.wav` audio and a random ZIP without `metadata.inf` are rejected.
- Restore (Merge) then restore again → the second report shows 0 new favourites, 0 new playlist tracks, 0 new play events.
- Export → file reopens as ZIP with the entries of §1 in that order; `metadata.inf` counts equal real counts; importing that exported file back into the same app (Merge) changes nothing.
- Classic favourites export/import from file 07 still works, byte-for-byte unchanged behaviour.
- **Keep the user's real sample out of Git** (add `ManualBackup_*` and `*.wav` to `.gitignore`); unit tests use a tiny fixture ZIP **generated in test code** with invented songs, never the real file.
