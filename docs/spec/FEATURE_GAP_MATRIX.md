# Feature Gap Matrix — Phase M0
> Every new requirement from the Unified Master Merge Specification mapped to phase, UI entry, dependencies, offline/online classification, and status.

Legend:
- **Classification:** LOCAL = works offline; ONLINE = requires network; LAN = local network only; HW = hardware-dependent; EXP = experimental
- **Priority:** P0 = core reliability, P1 = high-value local, P2 = optional/advanced, P3 = experimental
- **Status:** NEW = not yet started; PARTIAL = some foundation exists; READY = prerequisite met, can start now

---

## Theme and Design System (Phase M2)

| ID | Feature | Phase | Classification | Priority | UI Entry Point | Dependencies | Status |
|---|---|---|---|---|---|---|---|
| THM-01 | Schema v4 migration (genre, bitrate, sampleRate, etc.) | M1 | LOCAL | P0 | None (data layer) | `PulseDb`, migration test | NEW |
| THM-02 | Separate `colorSource` axis (Pulse palette / Material You / Artwork / Custom seed) | M2 | LOCAL | P0 | Settings → Appearance | `SettingsStore` new key | NEW |
| THM-03 | Material You wallpaper-derived dynamic color (Android 12+) | M2 | LOCAL | P1 | Settings → Appearance | `DynamicColorScheme`, API 31 check | NEW |
| THM-04 | Manual accent color / seed color picker | M2 | LOCAL | P1 | Settings → Appearance | `colorSource` key | NEW |
| THM-05 | Adjustable corner radius | M2 | LOCAL | P1 | Settings → Appearance | Design token system | NEW |
| THM-06 | Adjustable navigation-bar appearance | M2 | LOCAL | P1 | Settings → Appearance | Edge-to-edge APIs | NEW |
| THM-07 | Design token system (no hardcoded colors) | M2 | LOCAL | P0 | Global | All screens | NEW |
| THM-08 | Global visual audit (every screen in all theme/skin combos) | M2 | LOCAL | P0 | QA | All screens | NEW |
| THM-09 | Automatic theme switching on system schedule | M2 | LOCAL | P2 | Settings → Appearance | `AlarmManager` / `WorkManager` | NEW |
| THM-10 | New theme families: Material You, Expressive Material, AMOLED Pure Black, etc. | M2 | LOCAL | P1 | Settings → Appearance → Skins | `Color.kt` extension | NEW |
| THM-11 | Custom accent theme | M2 | LOCAL | P1 | Settings → Appearance | Seed color picker | NEW |

---

## Player Layout Registry (Phase M3)

| ID | Feature | Phase | Classification | Priority | UI Entry Point | Dependencies | Status |
|---|---|---|---|---|---|---|---|
| LAY-01 | Widescreen player layout (≥600dp) | M3 | LOCAL | P0 | Auto / Settings → Player | `PlayerLayoutMode` | NEW |
| LAY-02 | Player layout picker accessible from inside player | M3 | LOCAL | P0 | Player → layout icon | `PlayerLayoutBottomSheet` | PARTIAL |
| LAY-03 | Accurate layout preview in picker | M3 | LOCAL | P1 | Layout picker sheet | Preview composables | NEW |
| LAY-04 | Per-layout responsive behavior (portrait/landscape/tablet) | M3 | LOCAL | P0 | All layouts | `WindowSizeClass` | PARTIAL |
| LAY-05 | Atmospheric Queue — upper atmospheric section with procedural mist | M3 | LOCAL | P1 | Player theme / layout | `AtmosphericBackground` | PARTIAL |
| LAY-06 | Atmospheric Queue — lower "Up Next" light queue card | M3 | LOCAL | P1 | Atmospheric Queue layout | `QueuePanel` | PARTIAL |
| LAY-07 | Atmospheric Queue — amber `#D4AF37` active row, `ActiveAmberOnLight` on light card | M3 | LOCAL | P0 | Atmospheric Queue layout | Design tokens, C15 | PARTIAL |
| LAY-08 | Layout change does not restart playback | M3 | LOCAL | P0 | Layout picker | `PlayerController` | WORKING |
| LAY-09 | Persist selected layout | M3 | LOCAL | P0 | Settings | `SettingsStore` | WORKING |

---

## Live Button Arrangement Switcher (Phase M4)

| ID | Feature | Phase | Classification | Priority | UI Entry Point | Dependencies | Status |
|---|---|---|---|---|---|---|---|
| BTN-01 | Button arrangement preset system (`ButtonPreset` data class) | M4 | LOCAL | P1 | Settings → Player → Button layout | New `com.pulse.player.ui.buttons` package | NEW |
| BTN-02 | Preset 1: Centered | M4 | LOCAL | P1 | Button picker | BTN-01 | NEW |
| BTN-03 | Preset 2: Split | M4 | LOCAL | P1 | Button picker | BTN-01 | NEW |
| BTN-04 | Preset 3: Minimal | M4 | LOCAL | P1 | Button picker | BTN-01 | NEW |
| BTN-05 | Preset 4: Waveform Below | M4 | LOCAL | P1 | Button picker | BTN-01 | NEW |
| BTN-06 | Preset 5: Queue Inline | M4 | LOCAL | P1 | Button picker | BTN-01 | NEW |
| BTN-07 | Preset 6: Expanded | M4 | LOCAL | P1 | Button picker | BTN-01 | NEW |
| BTN-08 | Preset 7: Compact | M4 | LOCAL | P1 | Button picker | BTN-01 | NEW |
| BTN-09 | Preset 8: Floating | M4 | LOCAL | P1 | Button picker | BTN-01 | NEW |
| BTN-10 | Preset 9: Symmetric | M4 | LOCAL | P1 | Button picker | BTN-01 | NEW |
| BTN-11 | Preset 10: Asymmetric | M4 | LOCAL | P1 | Button picker | BTN-01 | NEW |
| BTN-12 | Preset 11: Two-row | M4 | LOCAL | P1 | Button picker | BTN-01 | NEW |
| BTN-13 | Preset 12: Radial-inspired | M4 | LOCAL | P1 | Button picker | BTN-01 | NEW |
| BTN-14 | Live switch (no music restart) | M4 | LOCAL | P1 | Player + Settings | `PlayerController` | NEW |
| BTN-15 | Button picker accessible from inside player | M4 | LOCAL | P1 | Player toolbar | New bottom sheet | NEW |
| BTN-16 | Preview current track state in picker | M4 | LOCAL | P1 | Button picker | Playback state | NEW |
| BTN-17 | Persist selected preset | M4 | LOCAL | P1 | DataStore | `buttonPresetId` key | NEW |
| BTN-18 | Animated transitions between presets | M4 | LOCAL | P1 | Player | Compose animation | NEW |
| BTN-19 | Reduced-motion compliant transitions | M4 | LOCAL | P0 | Player | `reducedMotion` setting | NEW |
| BTN-20 | Reset to default preset action | M4 | LOCAL | P1 | Button picker | Settings | NEW |
| BTN-21 | Accessibility labels unchanged across presets | M4 | LOCAL | P0 | All button presets | Content descriptions | NEW |

---

## Library and Indexing (Phase M5)

| ID | Feature | Phase | Classification | Priority | UI Entry Point | Dependencies | Status |
|---|---|---|---|---|---|---|---|
| LIB-01 | `genre`, `bitrate`, `sampleRate`, `bitDepth`, `codec`, `replayGain*`, `bpm`, `language`, `hasEmbeddedLyrics` in `TrackEntity` | M1 | LOCAL | P0 | Data layer (schema v4) | Migration `MIGRATION_3_4` | NEW |
| LIB-02 | Artist delimiter / featured artist parsing | M5 | LOCAL | P1 | Settings → Library → Separators | `Norm.kt` extension | NEW |
| LIB-03 | Album Artist grouping (distinct from track Artist) | M5 | LOCAL | P1 | Artists tab, Detail | `normAlbumArtist` in TrackEntity | PARTIAL |
| LIB-04 | Minimum file size filter | M5 | LOCAL | P0 | Settings → Library → Filters | Schema v4 | NEW |
| LIB-05 | Genre library page | M5 | LOCAL | P0 | Library tabs → Genres | Schema v4, new DAO query | NEW |
| LIB-06 | Moods library page | M5 | LOCAL | P1 | Library → More → Moods | `track_moods` table | NEW |
| LIB-07 | Tags library page | M5 | LOCAL | P1 | Library → More → Tags | `track_tags` table | NEW |
| LIB-08 | Ratings (1–5 stars) library page | M5 | LOCAL | P1 | Library → More → Ratings | `track_meta` table | NEW |
| LIB-09 | Languages library page | M5 | LOCAL | P1 | Library → More → Languages | Schema v4 `language` col | NEW |
| LIB-10 | Most Played page (range chips: today/week/month/year/all/custom) | M5 | LOCAL | P0 | Library tabs → Most Played | `StatsDao` | PARTIAL |
| LIB-11 | Saved queues page (`saved_queues` table) | M5 | LOCAL | P0 | Library → Queues | `saved_queues` schema | NEW |
| LIB-12 | Stats library page | M5 | LOCAL | P0 | Library → Stats / `RecapScreen` | `StatsDao` | PARTIAL |
| LIB-13 | Sort by play count, rating, BPM, bitrate, language, filename | M5 | LOCAL | P0 | Sort sheet | Schema v4, `StatsDao` | NEW |
| LIB-14 | Rescan one folder without full rescan | M5 | LOCAL | P0 | Library → Sources | `SafScanner`, `LibraryScanner` | NEW |
| LIB-15 | Per-source scan status + last scan timestamp | M5 | LOCAL | P0 | Library → Sources | `SettingsStore` / `sources` table | NEW |
| LIB-16 | Folder cover discovery (cover.jpg, folder.jpg) | M5 | LOCAL | P2 | Automatic | `ArtworkLoader` | NEW |
| LIB-17 | `.info.txt` sidecar parsing | M5 | LOCAL | P2 | Automatic | New parser | NEW |
| LIB-18 | Configurable filename-to-tag fallback | M5 | LOCAL | P1 | Settings → Library | `LibraryScanner` | NEW |
| LIB-19 | Page order and visibility configurable | M5 | LOCAL | P1 | Settings → Library → Pages | Navigation system | NEW |
| LIB-20 | Track tags `track_tags(trackId, tag)` table | M5 | LOCAL | P1 | Data layer | Schema v4 | NEW |
| LIB-21 | Track moods `track_moods(trackId, mood)` table | M5 | LOCAL | P1 | Data layer | Schema v4 | NEW |
| LIB-22 | Track meta `track_meta(trackId, rating, language, note)` table | M5 | LOCAL | P1 | Data layer | Schema v4 | NEW |
| LIB-23 | Source abstraction `MusicSource` interface | M5 | LOCAL | P0 | Data layer | New `library` package | NEW |
| LIB-24 | `sources` table (for remote sources, phase M11) | M5 | LOCAL | P0 | Data layer | Schema v4 | NEW |

---

## Metadata and Playlists (Phase M6)

| ID | Feature | Phase | Classification | Priority | UI Entry Point | Dependencies | Status |
|---|---|---|---|---|---|---|---|
| META-01 | Tag editor (view + edit title/artist/album/year/etc.) | M6 | LOCAL | P1 | Track details → Edit tags | TagLib binding, `MediaStore.createWriteRequest` | NEW |
| META-02 | Batch tag editing | M6 | LOCAL | P1 | Multi-select → Edit tags | META-01 | NEW |
| META-03 | Before/after metadata snapshot | M6 | LOCAL | P1 | Tag editor | File backup toggle | NEW |
| META-04 | Tag editor supports cover artwork editing | M6 | LOCAL | P2 | Tag editor | META-01 | NEW |
| META-05 | Smart playlists (`smart_playlists` table, rule engine) | M6 | LOCAL | P1 | Playlists → New smart playlist | New `library.SmartPlaylistEngine` | NEW |
| META-06 | Built-in smart collections (Recently added, Most played, etc.) | M6 | LOCAL | P1 | Playlists → Smart | META-05 | NEW |
| META-07 | M3U/M3U8 playlist import/export | M6 | LOCAL | P1 | Playlists → Import/Export | SAF, `PlaylistDao` | NEW |
| META-08 | Saved queue sessions (`saved_queues` + `saved_queue_items` tables) | M6 | LOCAL | P1 | Queue sheet → Save queue | Schema v4 | NEW |
| META-09 | Playlist artwork | M6 | LOCAL | P2 | Playlist detail | `ArtworkLoader` | NEW |
| META-10 | Playlist description | M6 | LOCAL | P2 | Playlist detail | `PlaylistEntity` extension | NEW |
| META-11 | Merge playlists with preview | M6 | LOCAL | P2 | Playlists multi-select | `PlaylistDao` | NEW |
| META-12 | Track audio profile (`track_audio_profile` table: speed, pitch, gainDb, eqJson, presetId) | M6 | LOCAL | P1 | Track → Sound profile | Schema v4 | NEW |
| META-13 | Bookmarks (`bookmarks` table) | M6 | LOCAL | P2 | Player → Bookmark | Schema v4 | NEW |
| META-14 | Chapters (`chapters` table) | M6 | LOCAL | P2 | Player → Chapters | Schema v4 | NEW |

---

## Search and Lyrics (Phase M7)

| ID | Feature | Phase | Classification | Priority | UI Entry Point | Dependencies | Status |
|---|---|---|---|---|---|---|---|
| SRCH-01 | Typo-tolerant search (normalized, ranked) | M7 | LOCAL | P1 | `SearchScreen` | New `library.SearchIndex` | NEW |
| SRCH-02 | Recent searches (clearable) | M7 | LOCAL | P1 | `SearchScreen` | `SettingsStore` / new key | NEW |
| SRCH-03 | Search by tags/mood/genre | M7 | LOCAL | P1 | `SearchScreen` | Schema v4 | NEW |
| SRCH-04 | Search inside lyrics (locally cached) | M7 | LOCAL | P2 | `SearchScreen` | `LyricsDao` | NEW |
| SRCH-05 | Search filters (type, scope) | M7 | LOCAL | P1 | `SearchScreen` | NEW |
| LYR-01 | Adjacent `.lrc` file auto-discovery | M7 | LOCAL | P1 | Automatic | `LrcParser` extension | NEW |
| LYR-02 | Tap lyric line to seek | M7 | LOCAL | P1 | Lyrics view | `PlayerController.seekTo()` | PARTIAL |
| LYR-03 | Manual lyric offset adjustment | M7 | LOCAL | P1 | Lyrics view | New offset slider | NEW |
| LYR-04 | Lyrics editor (text + timestamps) | M7 | LOCAL | P1 | Lyrics view → Edit | New composable | NEW |
| LYR-05 | Export/save sidecar `.lrc` | M7 | LOCAL | P1 | Lyrics editor | SAF write | NEW |
| LYR-06 | `source` and `format` columns in `lyrics` table | M7 | LOCAL | P0 | Data layer (schema v4) | Migration | NEW |
| LYR-07 | TTML / word-synced lyrics support | M7 | LOCAL | P2 | Lyrics display | New parser | NEW |
| LYR-08 | Online LRCLIB lookup (full flavor only) | M7 | ONLINE | P2 | Settings → Lyrics → Online fetch | `NetworkGateway` | NEW |

---

## Audio Enhancements (Phase M8)

| ID | Feature | Phase | Classification | Priority | UI Entry Point | Dependencies | Status |
|---|---|---|---|---|---|---|---|
| AUD-01 | Custom EQ presets (save, rename, duplicate, reset) | M8 | LOCAL | P1 | Settings → Audio | `AudioFx` extension | PARTIAL |
| AUD-02 | Parametric EQ | M8 | HW | P2 | Settings → Audio | Device capability check | NEW |
| AUD-03 | AutoEQ profile import | M8 | LOCAL | P2 | Settings → Audio | File parser | NEW |
| AUD-04 | ReplayGain (track/album gain) | M8 | LOCAL | P1 | Settings → Audio | `AudioProcessor` gain stage (C4) | NEW |
| AUD-05 | Loudness normalization | M8 | HW | P2 | Settings → Audio | Capability check | NEW |
| AUD-06 | Mono audio | M8 | LOCAL | P1 | Settings → Audio | `AudioFx` | NEW |
| AUD-07 | Pitch adjustment (semitone controls) | M8 | LOCAL | P1 | Settings → Audio | Media3 `PlaybackParameters` | NEW |
| AUD-08 | 432 Hz tuning mode | M8 | LOCAL | P2 | Settings → Audio | Pitch shift | NEW |
| AUD-09 | Per-track sound profile (EQ, speed, pitch, gain) | M8 | LOCAL | P1 | Track → Sound profile | `track_audio_profile` table | NEW |
| AUD-10 | Audio path viewer | M8 | LOCAL | P1 | Settings → Audio | `AudioSignalPathBadge` | PARTIAL |
| AUD-11 | Clipping/distortion warning | M8 | LOCAL | P1 | Settings → Audio | Gain computation | NEW |
| AUD-12 | Crossfade engine (experimental, default OFF) | M8 | EXP | P2 | Settings → Playback | C3: `CrossfadeEngine` | NEW |
| AUD-13 | Skip silence | M8 | LOCAL | P2 | Settings → Playback | `skipSilenceEnabled` (exists as SessionCommand) | PARTIAL |

---

## History and Discovery (Phase M9)

| ID | Feature | Phase | Classification | Priority | UI Entry Point | Dependencies | Status |
|---|---|---|---|---|---|---|---|
| HIST-01 | Editable history (inspect, delete, import records) | M9 | LOCAL | P1 | Settings → History & Statistics | `StatsDao` extension | NEW |
| HIST-02 | History `origin` column (`LIVE`, `IMPORT_*`, `MANUAL`) | M9 | LOCAL | P0 | Data layer (schema v4) | Migration (C5) | NEW |
| HIST-03 | Daily/weekly/yearly/custom-range statistics | M9 | LOCAL | P1 | Stats page | `StatsDao` extended | PARTIAL |
| HIST-04 | Listening streaks | M9 | LOCAL | P2 | Stats page | `StatsDao.playDays()` | NEW |
| HIST-05 | Your Year page | M9 | LOCAL | P2 | Home or Recap | `StatsDao` aggregation | NEW |
| HIST-06 | Local recommendation: Smart Tracks (co-listen) | M9 | LOCAL | P1 | For You | New `library.RecommendationEngine` | NEW |
| HIST-07 | Similar tracks (artist/album/genre/tags/history) | M9 | LOCAL | P1 | Track detail | `RecommendationEngine` | NEW |
| HIST-08 | Daily Mix (local algorithm, not AI) | M9 | LOCAL | P1 | For You | `RecommendationEngine` | NEW |
| HIST-09 | Lost Memories (same date in prior years) | M9 | LOCAL | P1 | For You | `StatsDao`, `RecommendationEngine` | NEW |
| HIST-10 | Underrated tracks | M9 | LOCAL | P1 | For You | `RecommendationEngine` | NEW |
| HIST-11 | "Why this track?" explanation | M9 | LOCAL | P2 | Recommendation item | `RecommendationEngine` | NEW |
| HIST-12 | Disable personalization / clear recommendation data | M9 | LOCAL | P1 | Settings → History | DataStore key | NEW |
| HIST-13 | History import (Last.fm, Spotify, ListenBrainz) via user-selected export file | M9 | LOCAL | P2 | Settings → History | `HistoryWriter` (C5) | NEW |
| HIST-14 | Shareable recap cards (generated locally) | M9 | LOCAL | P1 | Recap screen | Compose Canvas export | UNVERIFIED |

---

## Visual and Interaction Polish (Phase M10)

| ID | Feature | Phase | Classification | Priority | UI Entry Point | Dependencies | Status |
|---|---|---|---|---|---|---|---|
| VIS-01 | Audio-reactive visualizer (bars, wave, circle, dots) | M10 | LOCAL | P1 | Settings → Appearance → Visualizer | `AudioLevels` flow (C13) | NEW |
| VIS-02 | Animated backgrounds (procedural) | M10 | LOCAL | P1 | Settings → Appearance | `ProceduralCanvases` | PARTIAL |
| VIS-03 | PCM tap in `AudioProcessor` for `AudioLevels` flow (no RECORD_AUDIO) | M10 | LOCAL | P1 | Playback engine | C13 resolution | NEW |
| VIS-04 | Mini-player Party Mode (edge breathing effect) | M10 | LOCAL | P2 | Settings → Appearance | `MiniPlayer` | NEW |
| VIS-05 | Configurable swipe gestures on track row | M10 | LOCAL | P1 | Settings → Gestures | `TrackRow` | NEW |
| VIS-06 | Artwork tap/long-press gestures | M10 | LOCAL | P1 | Settings → Gestures | Player layouts | NEW |
| VIS-07 | Full waveform seekbar PCM decode pipeline | M10 | LOCAL | P1 | Settings → Player → Seek bar style | `WaveformSeekBar`, `waveforms` table | PARTIAL |
| VIS-08 | Battery-aware visualizer (off in battery saver / reduced motion) | M10 | LOCAL | P0 | Automatic | `PowerManager` | NEW |
| VIS-09 | Quick settings tiles (Sleep timer, EQ, Favourites, Today, Offline mode) | M10 | LOCAL | P2 | Android system UI | `TileService` | NEW |
| VIS-10 | Animation intensity setting | M10 | LOCAL | P1 | Settings → Appearance | New DataStore key | NEW |
| VIS-11 | List density and album grid density settings | M10 | LOCAL | P1 | Settings → Appearance | New DataStore keys | NEW |

---

## Optional Online Integrations (Phase M11)

| ID | Feature | Phase | Classification | Priority | UI Entry Point | Dependencies | Status |
|---|---|---|---|---|---|---|---|
| NET-01 | Product flavors: `offline` (no INTERNET) and `full` (adds INTERNET) | M11 | ONLINE | P0 | `app/build.gradle.kts` | C1 | NEW |
| NET-02 | `NetworkGateway` + master "Online features" switch (default OFF) | M11 | ONLINE | P0 | Settings → Privacy | New `com.pulse.player.net` package | NEW |
| NET-03 | Per-service switches | M11 | ONLINE | P0 | Settings → Connections | `NetworkGateway` | NEW |
| NET-04 | Offline-only mode setting (disables all network) | M11 | ONLINE | P0 | Settings → Privacy | `NetworkGateway` | NEW |
| NET-05 | LRCLIB online lyrics lookup (full flavor, off by default) | M11 | ONLINE | P2 | Settings → Lyrics | `NetworkGateway` | NEW |
| NET-06 | Deezer artist artwork lookup (full flavor, off by default) | M11 | ONLINE | P2 | Settings → Connections | `NetworkGateway` | NEW |
| NET-07 | AI playlist generation (Gemini, provider-neutral, off by default) | M11 | ONLINE | P2 | Settings → AI | Existing Gemini client | PARTIAL |
| NET-08 | Scrobbling integration (Last.fm/ListenBrainz) | M11 | ONLINE | P2 | Settings → Connections | `NetworkGateway` | NEW |
| NET-09 | Google Cast (full flavor) | M11 | ONLINE | P2 | Settings → Connectivity | Play Services | NEW |
| NET-10 | Remote library: Subsonic/Navidrome | M11 | ONLINE | P2 | Settings → Sources | `SubsonicSource` | NEW |
| NET-11 | Remote library: Jellyfin | M11 | ONLINE | P2 | Settings → Sources | `JellyfinSource` | NEW |
| NET-12 | LAN sync | M11 | LAN | P2 | Settings → Connectivity | Local-network permissions | NEW |
| NET-13 | LAN listening party | M11 | LAN | P2 | Settings → Connectivity | Local-network permissions | NEW |
| NET-14 | Keystore-backed encrypted secret store (server passwords, API keys, tokens) | M11 | LOCAL | P0 | Data layer | Android Keystore | NEW |
| NET-15 | History import via supported documented APIs (Spotify/Last.fm export files) | M9 | LOCAL | P2 | Settings → History | `HistoryWriter` | NEW |
| NET-16 | Android Auto integration | M11 | EXP | P2 | Settings → Connectivity | `automotive` dependency | NEW |
| NET-17 | Status-bar lyrics (Android version permitting) | M11 | EXP | P2 | Settings → Connectivity | Platform check | NEW |

---

## Backup Hardening (Phase M12)

| ID | Feature | Phase | Classification | Priority | UI Entry Point | Dependencies | Status |
|---|---|---|---|---|---|---|---|
| BAK-01 | Classic v2: copy audio files to `Music/Pulse Player/` | M12 | LOCAL | P1 | Backup → Classic → Export with audio | `MusicCopier`, confirmation dialog | NEW |
| BAK-02 | Classic export conflict resolution (same-named files) | M12 | LOCAL | P1 | Classic v2 export | `MusicCopier` | NEW |
| BAK-03 | Classic import from user-selected folder/JSON | M12 | LOCAL | P1 | Backup → Classic → Import | `CompatRestore` | PARTIAL |
| BAK-04 | Classic import preview (matching results before apply) | M12 | LOCAL | P1 | Classic import dialog | `FavoritesMatcher` | PARTIAL |
| BAK-05 | Backup progress and cancellation | M12 | LOCAL | P1 | Backup screens | Coroutine cancellation | NEW |
| BAK-06 | Free-space check before backup | M12 | LOCAL | P0 | Pre-backup validation | `StatFs` | NEW |
| BAK-07 | Last backup details (filename, date/time, size, category counts) | M12 | LOCAL | P1 | Backup screen | `SettingsStore.lastBackup()` | PARTIAL |
| BAK-08 | Restore preview (new/existing/skipped/conflict counts) | M12 | LOCAL | P1 | Full backup restore | `CompatRestore` | PARTIAL |
| BAK-09 | Temporary file cleanup after backup/restore | M12 | LOCAL | P0 | Automatic | `ArchiveBackupService` | NEW |
| BAK-10 | Schema v4 backup compatibility (export includes new tables) | M12 | LOCAL | P0 | Data layer | Schema v4 migration | NEW |

---

## Security, Accessibility and Performance (Phase M13)

| ID | Feature | Phase | Classification | Priority | UI Entry Point | Dependencies | Status |
|---|---|---|---|---|---|---|---|
| SEC-01 | Just-in-time permission strategy (first launch onboarding) | M13 | LOCAL | P0 | First launch | `PermissionMonitor` | PARTIAL |
| SEC-02 | Privacy section in Settings | M13 | LOCAL | P0 | Settings → Privacy | New screen | NEW |
| SEC-03 | Capabilities registry (`Capabilities` object, runtime evaluation) | M13 | LOCAL | P0 | Global / Settings | New `core.flags` package | NEW |
| SEC-04 | TalkBack navigation order audit | M13 | LOCAL | P0 | All screens | Content descriptions | NEW |
| SEC-05 | Contrast audit (all themes, WCAG AA) | M13 | LOCAL | P0 | QA | Design tokens | NEW |
| SEC-06 | Responsive layout audit (compact → tablet, font scale 2.0) | M13 | LOCAL | P0 | QA | `WindowSizeClass` | PARTIAL |
| SEC-07 | Permission transparency UI (each permission explained) | M13 | LOCAL | P0 | Onboarding + Settings → Permissions | `PermissionMonitor` | NEW |
| SEC-08 | Database migration test for schema v4 | M13 | LOCAL | P0 | Test | `MIGRATION_3_4` | NEW |
| SEC-09 | `PlayerLayoutModeTest` (new layout registry test) | M13 | LOCAL | P0 | Test | C10 | NEW |
| SEC-10 | Update `ThemeRegistryTest` to new canonical theme count | M13 | LOCAL | P0 | Test | C10 | NEW |
| SEC-11 | Amber contrast unit test (`ActiveAmberOnLight` ≥ 4.5:1) | M13 | LOCAL | P0 | Test | C15 | NEW |
| SEC-12 | `MIGRATION_3_4` exported schema JSON | M13 | LOCAL | P0 | Schemas folder | KSP Room | NEW |
| SEC-13 | Error handling policy for all new subsystems | M13 | LOCAL | P0 | Global | `ErrorPolicy` | NEW |
| SEC-14 | Settings IA restructuring (13 sections from spec §14) | M13 | LOCAL | P0 | `SettingsScreen` | All new features | NEW |
| SEC-15 | Secrets excluded from Room / DataStore (use Keystore) | M13 | LOCAL | P0 | Data layer | Android Keystore | NEW |

---

## Documentation (Phase M14)

| ID | Feature | Phase | Classification | Priority | UI Entry Point | Dependencies | Status |
|---|---|---|---|---|---|---|---|
| DOC-01 | README update (both flavors, feature matrix, build instructions, permissions) | M14 | LOCAL | P0 | Repository | All phases | NEW |
| DOC-02 | `docs/spec/PARITY_INVENTORY.md` | M0 | LOCAL | P0 | Repository | Reference app audit | NEW |
| DOC-03 | `docs/THIRD_PARTY_NOTICES.md` | M0 | LOCAL | P0 | Repository | License check | NEW |
| DOC-04 | `docs/DECISIONS.md` | M0 | LOCAL | P0 | Repository | Owner decisions | NEW |
| DOC-05 | `docs/INFO_TXT.md` (Namida sidecar format) | M5 | LOCAL | P2 | Repository | Namida source analysis | NEW |
| DOC-06 | Architecture overview | M14 | LOCAL | P0 | Repository | All phases | NEW |
| DOC-07 | Navigation map | M14 | LOCAL | P0 | Repository | M3, M5 | NEW |
| DOC-08 | Theme/skin/player-layout catalogue | M14 | LOCAL | P0 | Repository | M2, M3 | NEW |
| DOC-09 | Backup and restore guide | M14 | LOCAL | P0 | Repository | M12 | NEW |
| DOC-10 | `brain.md` updated per phase | All | LOCAL | P0 | Repository | Each phase | ONGOING |
