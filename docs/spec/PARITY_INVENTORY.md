# Parity Inventory — Phase 9 Audit
> One row per feature / screen / skin / theme / layout identified from the upstream repositories and their published documentation.  
> Audited remotely on 2026-11-03: Namida `10123d91e2527682efc72d877603a121d85d8fbe`; PixelPlayer `63cb59b97aff1d60c5aa46afdf5ce489e632b267`. The removed local reference folder was not used as a source.  
> License findings are recorded in `docs/THIRD_PARTY_NOTICES.md`; Pulse Player re-implements concepts independently.

Format: `ID | Source | Name | Where it lives | What it does | Pulse equivalent | Status | Phase | Settings location`

---

## Namida Features

| ID | Source | Name | Where it lives | What it does | Pulse equivalent | Status | Phase | Settings location |
|---|---|---|---|---|---|---|---|---|
| NAM-L1 | Namida | TagLib tag editor | Library scan + editor screen | Read/write all embedded tags; confirm before write | Track details → Edit tags | NEW | M6 | Library → Tag editor |
| NAM-L2 | Namida | Artist/genre separators | Scan settings | Configurable delimiters; exceptions list | MISSING | NEW | M5 | Library → Separators |
| NAM-L3 | Namida | Duplicate prevention | Library screen | Hide duplicates (display-only); review screen | DuplicatesScreen (partial) | PARTIAL | M5 | Library → Duplicates |
| NAM-L4 | Namida | Min file size + duration filters | Library settings | Filters before indexing | Min duration exists; size MISSING | PARTIAL | M5 | Library → Filters |
| NAM-L5 | Namida | Folder include/exclude tree | Library settings | Per-folder include/exclude | excludedFolders exists; include-tree MISSING | PARTIAL | M5 | Library → Folders |
| NAM-L6 | Namida | Sort by any property | Sort sheet | 15+ sort keys per page | Partial (dateAdded, title) | PARTIAL | M5 | Library → Sorting |
| NAM-L7 | Namida | Folder cover + .info.txt | Artwork resolution | cover.jpg → embedded → generated | ArtworkLoader exists; folder cover MISSING | PARTIAL | M5 | Library → Sidecar |
| NAM-L8 | Namida | feat./ft. artist extraction | Scan parser | Creates artist links without altering tags | MISSING | NEW | M5 | Library → Separators |
| NAM-U1 | Namida | Dynamic colors from artwork | Theme system | Theme transitions 500ms; contrast-guarded | dynamicAccent exists (basic) | PARTIAL | M2 | Appearance → Themes |
| NAM-U2 | Namida | Additional library pages | Navigation | Genres, Queues, Most Played, Moods, Tags, Ratings, Languages, Stats | Most Played partial; rest MISSING | PARTIAL | M5 | Library → Pages |
| NAM-U3 | Namida | Waveform seekbar | Player controls | MediaCodec PCM decode → 200–400 peak buckets; per-track cache | WaveformSeekBar.kt (partial) | PARTIAL | M10 | Appearance → Seek bar style |
| NAM-U4 | Namida | Widescreen player layout | Player | Artwork left, controls/queue right at ≥600dp | MISSING | NEW | M3 | Layouts |
| NAM-U5 | Namida | Visualizers + animated backgrounds | Player/Home | Bars, wave, circle, dots; audio-reactive | AtmosphericBackground.kt (partial) | PARTIAL | M10 | Appearance → Visualizer |
| NAM-U6 | Namida | Extensive Settings customization | Settings | All axes configurable | Partial (8 settings sections vs 13) | PARTIAL | M13 | Settings |
| NAM-U7 | Namida | Animating artwork interactions | Player | Artwork tap/long-press; swipe gestures | MISSING | NEW | M10 | Gestures |
| NAM-P1 | Namida | Smart playlists (rule engine) | Playlists | All/any rules; live updates | MISSING | NEW | M6 | Playlists → Smart |
| NAM-P2 | Namida | Saved queue sessions | Queue | Save/restore named queue states | MISSING | NEW | M6 | Queue sheet |
| NAM-P3 | Namida | Repeat N times | Queue | Repeat current track N times before advancing | MISSING | NEW | M5 | Playback |
| NAM-P4 | Namida | Track Play Mode | Queue | Play selected/album/artist/genre/folder/results | MISSING | NEW | M5 | Playback |
| NAM-P5 | Namida | Insert after latest | Queue | Insert after most recently inserted item | MISSING | NEW | M5 | Playlists & Queue |
| NAM-S1 | Namida | Editable history | Statistics | Inspect, delete, import records | MISSING | NEW | M9 | History & Statistics |
| NAM-S2 | Namida | Daily/yearly/custom range stats | Statistics | Custom date range summaries | Monthly only | PARTIAL | M9 | History & Statistics |
| NAM-S3 | Namida | Listening streaks | Statistics | Derived from actual records | MISSING | NEW | M9 | History & Statistics |
| NAM-S4 | Namida | Your Year page | Statistics | Annual listening summary | MISSING | NEW | M9 | Recap |
| NAM-S5 | Namida | Shareable recap cards | Statistics | Generated locally via Compose Canvas | Exists (unverified) | UNVERIFIED | M9 | Recap |
| NAM-R1 | Namida | Smart Tracks (co-listen) | Recommendations | Frequently listened together | MISSING | NEW | M9 | For You |
| NAM-R2 | Namida | Daily Mix (local algorithm) | Recommendations | Based on user behavior, labeled as Smart Mix | MISSING | NEW | M9 | For You |
| NAM-R3 | Namida | Lost Memories | Recommendations | Same date in prior years | MISSING | NEW | M9 | For You |
| NAM-R4 | Namida | Underrated tracks | Recommendations | Low play count + positive rating | MISSING | NEW | M9 | For You |
| NAM-A1 | Namida | Lyrics editor | Lyrics | Create/edit text and timestamps | MISSING | NEW | M7 | Lyrics editor |
| NAM-A2 | Namida | Manual lyric offset | Lyrics | Adjust sync offset per track | MISSING | NEW | M7 | Lyrics |
| NAM-A3 | Namida | Adjacent .lrc auto-discovery | Lyrics | Load matching .lrc without import action | MISSING | NEW | M7 | Lyrics |
| NAM-A4 | Namida | LRCLIB online lookup | Lyrics (online) | Disabled in offline build | MISSING | NEW | M7/M11 | Settings → Lyrics |
| NAM-E1 | Namida | Per-track sound profile | Audio | Per-track EQ, speed, pitch, gain | MISSING | NEW | M8 | Track → Sound profile |
| NAM-E2 | Namida | ReplayGain | Audio | Track/album gain via AudioProcessor | MISSING | NEW | M8 | Settings → Audio |
| NAM-E3 | Namida | Pitch adjustment + 432Hz | Audio | Semitone controls, 432Hz mode | MISSING | NEW | M8 | Settings → Audio |
| NAM-E4 | Namida | AutoEQ profile import | Audio | Load AutoEQ profiles | MISSING | NEW | M8 | Settings → Audio |
| NAM-I1 | Namida | M3U/M3U8 import/export | Playlists | Standard playlist interchange | MISSING | NEW | M6 | Playlists |
| NAM-I2 | Namida | History import (Last.fm etc via export file) | History | Import from user-selected export file | MISSING | NEW | M9 | History & Statistics |

---

## PixelPlayer Features

| ID | Source | Name | Where it lives | What it does | Pulse equivalent | Status | Phase | Settings location |
|---|---|---|---|---|---|---|---|---|
| PIX-U1 | PixelPlayer | Material You / Material 3 Expressive design | Theme system | Wallpaper dynamic color; expressive motion | ThemeMode/Skins exist; Material You MISSING | PARTIAL | M2 | Appearance |
| PIX-U2 | PixelPlayer | Album-art-derived accent palette | Theme | Dynamic color from current artwork | dynamicAccent (basic) | PARTIAL | M2 | Appearance |
| PIX-U3 | PixelPlayer | AMOLED true-black mode | Theme | Pure black backgrounds | AMOLED ThemeMode exists | WORKING | — | Appearance |
| PIX-U4 | PixelPlayer | Adjustable corner radius | Customization | Global corner radius slider | MISSING | NEW | M2 | Appearance |
| PIX-U5 | PixelPlayer | Expressive animated transitions | UI | Expressive transitions; micro-interactions | Basic animations exist | PARTIAL | M2 | Appearance |
| PIX-U6 | PixelPlayer | Consistent component states | UI | Pressed/focused/selected/disabled/loading | Partial | PARTIAL | M2 | — |
| PIX-L1 | PixelPlayer | Multi-artist parsing | Library | Configurable delimiters | MISSING | NEW | M5 | Library → Separators |
| PIX-L2 | PixelPlayer | Album Artist grouping | Library | Separate from track Artist | Entity has albumArtist; grouping logic PARTIAL | PARTIAL | M5 | Library |
| PIX-L3 | PixelPlayer | Artist artwork cache + fallback | Library | LRU + persistent cache; Deezer optional | ArtworkLoader exists; Deezer MISSING | PARTIAL | M5/M11 | Connections |
| PIX-L4 | PixelPlayer | Cache size limit + clear | Library | Bounded LRU cache management | ArtworkLoader trim exists | PARTIAL | M5 | Library |
| PIX-P1 | PixelPlayer | Gapless playback | Playback | Validated gapless between tracks | Media3 supports; validation UNVERIFIED | UNVERIFIED | M8 | Playback |
| PIX-P2 | PixelPlayer | Configurable crossfade | Playback | Experimental, default OFF | MISSING | NEW | M8 | Playback |
| PIX-A1 | PixelPlayer | AI playlist generation | AI | Provider-neutral; local fallback | GenerateMusicScreen (Gemini) | PARTIAL | M11 | AI |
| PIX-C1 | PixelPlayer | Google Cast | Connectivity | Optional; full flavor only | MISSING | NEW | M11 | Connectivity |
| PIX-C2 | PixelPlayer | Android Auto | Connectivity | Optional; experimental | MISSING | NEW | M11 | Connectivity |
| PIX-C3 | PixelPlayer | Remote libraries (Subsonic, Jellyfin, etc.) | Connectivity | Optional; full flavor | MISSING | NEW | M11 | Sources |
| PIX-C4 | PixelPlayer | Scrobbling (Last.fm/ListenBrainz) | Connectivity | Optional; full flavor | MISSING | NEW | M11 | Connections |
| PIX-C5 | PixelPlayer | LAN sync / Listening Party | Connectivity | Optional; LAN only | MISSING | NEW | M11 | Connectivity |

---

## New Features from Merge Spec (not in either reference)

| ID | Source | Name | Phase | Status |
|---|---|---|---|---|
| SPEC-01 | Spec | Live button arrangement switcher (12+ presets) | M4 | NEW |
| SPEC-02 | Spec | Classic backup v2 (audio file copy) | M12 | NEW |
| SPEC-03 | Spec | Offline-only mode setting | M11 | NEW |
| SPEC-04 | Spec | Just-in-time permission onboarding | M13 | NEW |
| SPEC-05 | Spec | Privacy section in Settings | M13 | NEW |
| SPEC-06 | Spec | Capabilities registry | M13 | NEW |
| SPEC-07 | Spec | Widescreen / Atmospheric / Split-lyrics responsive layouts | M3 | PARTIAL |
| SPEC-08 | Spec | Settings IA restructuring (13 sections) | M13 | NEW |
| SPEC-09 | Spec | Schema v4 migration | M1 | NEW |
| SPEC-10 | Spec | HistoryWriter gate + origin column | M9 | NEW |
