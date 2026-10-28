# V2-13 — Backup & Restore: Spec (read this BEFORE the backup code files 14, 15, 16)

> Audience: Gemini Flash in AI Studio. Read this file only. Do not read files 14-16 until this one is done.
> This replaces the vague "dual backup" prompt. Everything here comes from inspecting the REAL sample `ManualBackup_20261028_172204175.wav`.

## 1. What the sample file really is
- Named `.wav`, but the bytes start with `PK` = a **ZIP archive**. It is NOT audio. (Two uploaded copies were byte-identical.)
- 16 entries, ~12 MB uncompressed. It holds **metadata only, no audio**.
- Paths inside are absolute (`/storage/emulated/0/...`) and ids are device-specific. Never trust them on another device; use them only as **hints** for matching.

| Entry in ZIP | Real shape (verified) | Pulse Player maps it to |
|---|---|---|
| `metadata.inf` | JSON object: `backupTime` ("yyyyMMdd_HHmmssSSS"), `mode`="MANUAL_GLOBAL", `type`="GLOBAL", `isAutoBackup`, `deviceId`, `versionName`, `versionCode`, `settings{isBackupMusicTags,isBackupCovers,isBackupLyrics,isBackupPlaylists,isBackupSortSettings,isBackupScanHideSettings,isBackupMusicRecap}`, `songs`(int), `playlists`(int) | Preview header + category flags |
| `settings.bak` | JSON: same 7 flags + `device`, `versionName`, `versionCode` | Ignored on import (duplicate of metadata) |
| `MusicTagsBackupConfig.conf` | JSON **array** of `{_data, title, artist, album, track(int), genres(null or string)}`; artist is `"<unknown>"` when missing | Library snapshot. Import: used only as **match hints**. Never overwrites tags (Pulse does not edit tags) |
| `Covers/MusicCoversBackupConfig.conf` | array of `{_data, resource, cover_url}` (all null in sample) | Export: nulls. Import: ignored, reported "custom covers not supported" if any non-null |
| `Covers/AlbumCoversBackupConfig.conf` | array of `{album, album_art("content://media/..."), resource, modified_album_art}` | Same: never follow `content://` from a file |
| `Lyrics/LyricsBackupConfig.conf` | JSON array, `[]` in sample | Export `[]`. Import: if non-empty, skip + warn (schema unknown) |
| `PlayLists/PlayListBackupConfig.conf` | array of `{name, sort{key:"playlistPlayOrder",order:-1}, cover(null), musics:[{path}]}`. One playlist is named **"My Favourite"** | Playlist named My Favourite (case-insensitive) = **Favorites**. Others = user playlists |
| `SortBackupConfig.conf` | object keyed `All_Song, Directory_, Folder_Song, Album_, Album_Song, Artist_, Artist_Song, Genre_, Genre_Song` each `{key, order(1|-1)}` | `All_Song` -> Songs sort. Others kept verbatim in `sort_json` setting for round-trip |
| `ScanHiddenBackupConfig.conf` | object `{size:-1, duration:-1, device, exclude:[{_id,data,type}], hidden_songs:[{path,is_hidden}]}` | `exclude[].data` -> `excludedFolders` (strip `/storage/<vol>/`). `size/duration` -1 = no filter. `hidden_songs` = unsupported, count + warn |
| `Statistics/PlaybackRecords.json` | array of `{song_id, timestamp(ms), day("yyyyMMdd"), looper_count, source_type, source_id, play_duration(ms), play_count, device, skip_count, extra_*, _data}` | -> `play_events` (`playedAtMs=timestamp`, `listenedMs=play_duration`, `completed = skip_count==0`) |
| `Statistics/StatisticsSnapshotConfig.json` | `{snapshots:[{type:"monthly",value:"202605",filename}], initApplicationTimestamp}` | Export writes it. Import ignores (records file is canonical) |
| `Statistics/snapshot_monthly_YYYYMM.db` | SQLite, `PRAGMA user_version=1`, tables below | Export writes valid ones. Import **never opens or replaces a DB from the archive** |

Snapshot DB tables (exact): `android_metadata(locale)`, `statistics_playback(_id,song_id,timestamp,day,looper_count,source_type,source_id,play_duration,play_count,device,skip_count,extra_int_1..3,extra_text_1..3)`, `statistics_musics(_id,title,album_id,album,artist_id,artist,duration,_data,cover_url,is_hidden,genre_id,genre,data_modified,play_count,play_duration,skip_count,looper_count,source_type,source_id,device,timestamp,day,extra_int_1..3,extra_text_1..3)`, `statistics_albums`, `statistics_artists`, `statistics_genres` plus indexes on timestamp/day/song_id.

## 2. The two backup types the app must have (both independent)
| | **Type 1 — Favourites JSON** (file 07, keep unchanged) | **Type 2 — Full Backup archive** (this spec) |
|---|---|---|
| File | `PulsePlayer_Favorites_YYYY-MM-DD.json` | `PulseBackup_YYYYMMDD_HHmmssSSS.pulsebackup` (or `.wav`-named ZIP, see 4) |
| Holds | favorites only | favorites, playlists, sort, scan settings, listening history, tag snapshot, app settings |
| Import accepts | JSON schema `pulseplayer.favorites` v1 | ZIP from Pulse **and** the reference-style `.wav`/`.zip` (like your sample) |
| Never contains | audio | audio |

UI: Settings -> **Backup & Transfer** shows two cards: "Favourites file (JSON)" and "Full backup (archive)". Each with its own Export / Import buttons and one-sentence explanation. Never merge them.

## 3. Our archive layout = reference layout + one extra folder
Write **every reference entry with the exact same name and JSON shape** (so a file made by Pulse imports in Pulse and is structurally the same as the sample), plus:
```
PulseExtra/manifest.json     (formatId "pulseplayer.backup", formatVersion 1, counts, warnings, containsAudioFiles:false)
PulseExtra/checksums.json    ({"algorithm":"SHA-256","files":{"<entry>":"<hex>"}})  covers every other entry
PulseExtra/favorites.json    (same item shape as file 07 FavItem list)
PulseExtra/app_settings.json (theme, skin, player theme, reduced motion, fade, min duration, speed...)
```
Honesty rules: we do NOT claim the original third-party app can read `PulseExtra`, nor that its files restore into it. Only claim: "Pulse Player reads this layout".

## 4. Extension policy
- Default export name: `.pulsebackup` (truthful, MIME `application/zip` for the SAF picker).
- Optional switch "Name it .wav (compat)": same ZIP bytes, `.wav` name. Show the warning: "A .wav-named backup inside your Music folder can show up as a broken song. Keep it outside Music."
- **Scanner fix (required):** in MediaStore and SAF scans skip any item whose duration <= 0 or whose size < 1 KB, even if min-duration setting is 0.
- Import accepts any file; decides by **content sniffing**: first 4 bytes `50 4B 03 04` (or `50 4B 05 06`), then opens as ZIP, then requires (`PulseExtra/manifest.json`) OR (`metadata.inf` AND `PlayLists/PlayListBackupConfig.conf`). A real WAV starts with `RIFF....WAVE` -> reject: "This is an audio file, not a backup."

## 5. Categories (user can untick any)
`TAGS` (library snapshot, hints), `PLAYLISTS`, `FAVORITES`, `SORT`, `SCAN` (excluded folders), `STATS` (history + monthly snapshots), `APP_SETTINGS` (Pulse only), `COVERS`/`LYRICS` (always empty arrays; shown greyed "Not supported yet"). Flags map to `metadata.inf.settings`: TAGS->isBackupMusicTags, COVERS->isBackupCovers, LYRICS->isBackupLyrics, PLAYLISTS->isBackupPlaylists, SORT->isBackupSortSettings, SCAN->isBackupScanHideSettings, STATS->isBackupMusicRecap.

## 6. Track matching (reference paths -> this device)
Input: a path like `/storage/emulated/0/snaptube/download/SnapTube Audio/x.m4a`, optional title/artist/album from the tags file, optional duration (not in the sample).
1. `pathKey` = lowercase `displayName` + `|` + lowercase parent folder with `^/storage/[^/]+/` removed, trailing `/` removed. Pulse tracks give the same key from `folderPath` + `displayName` (MediaStore RELATIVE_PATH, e.g. `snaptube/download/SnapTube Audio/`). Exactly one track -> **PATH** (high).
2. Else same `displayName` only, one track -> **NAME** (medium). More than one -> ambiguous.
3. Else `norm(title)+norm(artist)+norm(album)` from the tag hint equals exactly one track -> **TAGS** (medium). Artist `<unknown>` counts as "Unknown artist".
4. Else unmatched. Never title-only. Never auto-pick among several.
Result enum: `PATH, NAME, TAGS, AMBIGUOUS, MISSING`. Restore applies PATH/NAME/TAGS; reports the rest.
Perf: build `HashMap<String, List<TrackEntity>>` indexes once (13k tracks ok). Never nested loops.

## 7. Restore semantics
- Preview first: shows archive kind (Pulse / Reference-style), backup time, app version, counts, which categories exist, warnings, checksum status ("verified" / "no checksums in this file").
- Mode **Merge** (default): add playlists missing by name (same name -> add missing tracks, keep order of existing), add favorites, add play events not already present (dedup key `(trackId, playedAtMs)`), settings: only fill keys the user chooses.
- Mode **Replace selected categories** (double confirm): replaces only ticked categories, inside one Room transaction, after the data is fully parsed and matched (staged in memory/disk). Failure -> rollback, old data intact.
- Play-event dedup requires a UNIQUE index on `play_events(trackId, playedAtMs)`: Room **migration 1->2** (see file 14 section 1).
- Report after restore: imported / updated / skipped / unmatched / ambiguous per category. Never say "restored everything" if anything is missing. Unmatched list is viewable and copyable.
- Music files: never touched.

## 8. Safety limits (ZIP is untrusted input)
Max entries 64; max single entry 128 MB; max total 256 MB; max compression ratio 200x per entry; entry names: no `..`, no leading `/`, no `\`, no NUL; no duplicate names; stream-count bytes while reading (do not trust ZIP header sizes); never extract to disk paths; never follow `content://` or file paths from JSON; never open archive `.db` files as live DB; temp files only in `cacheDir/backup_tmp/`, deleted in `finally`.

## 9. Acceptance
1. Select your sample `.wav` -> preview shows 13,509 songs / 41 playlists / 2,319 history records, "Reference-style archive, no checksums".
2. Restore (Merge) on a device where those files exist -> playlists + favorites + history appear; unmatched count shown honestly.
3. Export from Pulse -> reopen with Inspect -> "checksums verified".
4. A real WAV file is rejected. A truncated ZIP is rejected. Nothing changes on rejection.
5. Importing the same archive twice adds 0 new events and 0 duplicate playlist tracks.
6. `brain.md` updated. Tests in file 16 pass (say if not run).
