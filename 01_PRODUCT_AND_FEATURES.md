# 01 — Product & Features (what every screen must do)

Build order priority: **P0** (must work) → **P1** (polish) → **P2** (only if time remains).

## 1. Navigation structure
- **Phone (Compact width):** Top bar → hamburger (drawer), tab row, action icons (Skin, Search, Settings). Below: pager content. Above bottom edge: floating **mini player**.
- **Tabs (scrollable):** For you · Songs · Playlists · Folders · Albums · Artists
- **Drawer items:** Equalizer, Sleep timer, Monthly recap, Favorites, Backup & Transfer, Skin theme, Player theme, Settings, About.
- **Tablet (Medium/Expanded width):** NavigationRail replaces drawer; content on the left, **Now Playing panel docked on the right** (Expanded only). Album grid uses adaptive columns (`GridCells.Adaptive(160.dp)`).
- Navigation is a simple state-based `sealed interface Screen` + `AnimatedContent` (no Navigation-Compose needed, fewer errors).

## 2. Screens

### 2.1 For you (P0)
1. **Resume banner** (only if history exists): "Continue {title}" + play button + subtle gradient from artwork.
2. **Quick actions:** pill buttons **Shuffle** and **Play** (Play = start at first song of library).
3. **Widget tiles** (2-column grid): *Today* (minutes listened today, real), *Favorites* (count + open), *Sleep timer* (open sheet), *Equalizer* (open).
4. **Recently added** (header + "New" badge + "Play all"): horizontal row of 12 newest tracks with square art.
5. **Top picks:** horizontal carousel of most-played tracks (hidden until play history exists).
6. Empty library → onboarding card: "Allow access and scan your music" with **Scan** and **Choose folder** buttons.

### 2.2 Songs (P0)
- Header: "{n} Songs" plus "Scanning…" chip while scanning.
- Controls: **Shuffle** and **Play** pills (white bg, black text), sort menu (Title, Artist, Date added, Duration, asc/desc), multi-select toggle.
- Row: rounded-square art 52dp (fallback: gradient + music-note icon), title (bold), "artist · album" gray, right side date added + 3-dot menu.
- 3-dot menu: Play next · Add to queue · Add to playlist · Favorite · Song details · Set as ringtone · Share file.
- Multi-select bar: Add to playlist, Favorite, Play next, Select all.
- A–Z fast scroller on the right edge (P1).

### 2.3 Playlists (P0)
- 2x2 grid of **system playlists**: My favourite (blue heart), Recently added (green note), Recently played (purple pie), My top tracks (orange flame). Show real counts.
- "My playlists (X)" + **+** button → dialog to name a playlist.
- Playlist detail: play, shuffle, reorder (drag handle or move up/down buttons), remove, rename, delete (confirm).

### 2.4 Folders (P0)
- List folders that contain audio (from `folderPath` grouped), with track counts. Tap → folder track list.
- Source chip: Internal / SD card / Chosen folder. Include "Choose music folder" (SAF) button.

### 2.5 Albums / Artists (P0)
- Albums: adaptive grid, square art, title, artist. Detail: header art + track list + Play/Shuffle.
- Artists: list with initials avatar gradient, "n tracks · n albums". Detail: albums + songs.

### 2.6 Search (P0)
- Full-screen search; 250 ms debounce; results in sections Songs / Albums / Artists / Playlists. Tap a song plays it in the context of the results.

### 2.7 Now Playing (P0)
- Header: collapse arrow, **Song | Lyrics** segmented toggle, 3-dot menu (Track info, Share, Equalizer, Set as ringtone, Player theme).
- Center: artwork with soft glow (or the selected **Player Theme** layout, file 06). Vinyl theme rotates only while playing.
- Title (bold white), artist (gray). Seek slider with elapsed/total time. Optional -10s/+10s buttons.
- Controls: Shuffle, Previous, **Play/Pause (large)**, Next, Repeat (off/all/one).
- Bottom row: Favorite heart (cyan `#00C3FF` when on), Sleep timer, Add to playlist, Queue, Equalizer.
- Queue sheet: current track highlighted with animated bars, remove (swipe or button), move up/down, clear (confirm).
- Lyrics: read `.lrc` next to the file (same name) via SAF/MediaStore when available; synced highlight. If none: "No lyrics found" + **Choose .lrc file**.

### 2.5b Mini player (P0)
Floating glass bar above bottom inset: spinning disc (rotates only when playing and reduced-motion off), marquee title, play/pause, queue icon. Swipe up or tap → expand Now Playing. Thin progress line at its bottom edge.

## 3. Settings (P0/P1)
- **Appearance:** theme mode (System / Dark / AMOLED / Light), Skin theme, Player theme, dynamic accent from artwork (switch), reduced motion (switch).
- **Library:** Rescan, Choose music folder, Exclude folders, minimum duration (0/15/30/60 s), show/hide hidden formats.
- **Playback:** fade on play/pause (switch), speed, stop after current track, pause on headset disconnect (always on).
- **Audio effects:** open Equalizer.
- **Backup & Transfer:** Export favorites (JSON), Import favorites (JSON), Create/choose Music folder, instructions card.
- **Widgets:** how to add the widget (text steps).
- **About:** version, "Offline, no internet permission" statement.

## 4. Feature behaviors that must be exact
| Feature | Required behavior |
|---|---|
| Favorite | Instant heart animation; persisted in Room; shown everywhere from the same Flow |
| Shuffle | True random order of the current queue; toggling off restores original order |
| Repeat | Off → All → One cycle; icon changes; One shows "1" badge |
| Sleep timer | Presets 15/30/45/60, custom, and "end of this track"; fade out over last 10 s; cancel button; remaining time shown |
| Headset unplug | Pause immediately (`setHandleAudioBecomingNoisy(true)`) |
| Phone call | Pause via audio focus, resume when focus returns |
| Notification | Media3 notification with prev/play/next; lock-screen controls via MediaSession |
| Ringtone | If `Settings.System.canWrite()` false → open `ACTION_MANAGE_WRITE_SETTINGS`; else set ringtone/notification/alarm via `RingtoneManager.setActualDefaultRingtoneUri` using the track's content URI |
| Missing file | Mark unavailable (greyed), never delete favorites |
| Queue restore | Save queue ids + index + position every 5 s while playing and on pause; restore as paused on launch |
| Recap | Computed from real `play_events`; hidden if no data |

## 5. Empty / error states (design all)
No permission · empty library · scanning · no search results · no lyrics · no favorites · equalizer unsupported · file unsupported · SD card removed. Each has an icon, one sentence, and one action button.

## 6. Out of scope (do not add)
Streaming, accounts, ads, analytics, cloud sync, online lyrics/art, audio conversion, tag editing, Android Auto/Wear.
