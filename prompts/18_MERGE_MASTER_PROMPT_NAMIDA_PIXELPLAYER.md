# 18 — MASTER MERGE PROMPT: Namida + PixelPlayer parity, Atmospheric Queue player, Live Button Layouts, Hardening

> **You are a Principal Android Architect (Jetpack Compose, Modern Android Development).** You are extending the existing app **Pulse Player** (package `com.pulse.player`, Kotlin, Compose, Material 3, Media3, Room, DataStore, Glance). The base pack is files `00`–`17`. **This file (18) is Layer 4.** Where file 18 disagrees with an older file, file 18 wins, but **only for the items listed in section 3 (Conflict resolutions)**. Everything else in 00–17 still holds, including: one task = at most 6 changed files, complete files only, no emojis in UI, no fake data, no dead buttons, honest verification (`NOT RUN` is allowed, an invented PASS is not), and the CHANGED FILES manifest (file 12 §5).
>
> **Never delete, overwrite or weaken an existing feature, skin, theme, layout, setting or test.** Additions only. If an existing test must change because a spec changed (for example the theme count), change it deliberately, say so in the manifest, and never delete a test to make a build pass.
>
> **How to use this file:** Do not paste it all at once. Section 17 contains the phases, the updated Spec Index rows and prompts 30–46. Attach `SPEC_INDEX.md`, this file, and the files named for the current phase.

---

## 0. Reading order and precedence
1. `SPEC_INDEX.md` (extend it with phases 9–17, section 17.1).
2. This file, sections 1–3 always; then only the sections named by the phase.
3. Older files only as named in the phase table.
Precedence: **file 18 (section 3 only) > fix pack 12–17 > base 00–11.**

## 1. Mission
Merge into Pulse Player **every feature** and **every design** of two reference apps, without losing anything Pulse Player already has:
- **Namida** (Flutter, `github.com/namidaco/namida`): a feature-heavy local + streaming music player with a Material 3 look.
- **PixelPlayer** (`github.com/PixelPlayerHQ/PixelPlayer`): a modern Compose player (the project must verify the language and structure by reading the code; treat it as a Compose-style reference).

Both live in the repository folder `New Player Concept/`. They are **references for behaviour and design, not dependencies.** Plus these additions requested by the owner:
1. New premier player layout **Atmospheric Queue** (dark mist upper half, light "Up Next" card lower half) — section 7.
2. **Live Button Layout Switcher** with a large set of presets usable while music plays — section 8.
3. **Player layout picker** reachable from the player while music plays — section 7.6.
4. **Classic favourites backup v2**: export into `Music/Pulse Player/` with the audio files copied — section 9.
5. Theme/colour correctness in every mode — section 10.
6. Full responsiveness — section 11.
7. Performance and polish — section 12.
8. Comprehensive error handling — section 13.
9. First-launch permission onboarding — section 14.
10. A detailed README and a final traceability audit — sections 16 and 18.

## 2. What Pulse Player already has (do not rebuild what exists; verify it, keep it, extend it)
**Phase 0 rule:** audit the real project first. For every row below, mark `PRESENT`, `PARTIAL` or `MISSING`. If `MISSING` or `PARTIAL`, build it from the file named in the last column **in addition to** the new work. Never rebuild a `PRESENT` item.

| Area | What exists (from the pack) | Spec file |
|---|---|---|
| Navigation | Single Activity, state-based `Screen` + `AnimatedContent`; top tabs **For you · Songs · Playlists · Folders · Albums · Artists**; drawer (phone), NavigationRail (medium), docked Now Playing panel (expanded) | 01, 05 |
| Home | Resume banner, Shuffle/Play pills, 4 widget tiles (Today, Favourites, Sleep timer, Equalizer), Recently added, Top picks, empty-library onboarding | 01, 05 |
| Songs | Header count, scanning chip, sort menu, multi-select, A–Z scroller, track menu (play next, queue, playlist, favourite, details, ringtone, share), Missing songs chip | 01, 05, 14 |
| Playlists / Folders / Albums / Artists / Search / Detail | System playlists (My favourite, Recently added, Recently played, My top tracks), user playlists (create, rename, reorder, delete), 250 ms debounced search in four sections | 01 |
| Now Playing | Song/Lyrics toggle, seek bar, transport, secondary row, queue sheet, sleep sheet, add-to-playlist sheet, mini player (spinning disc, marquee, progress line) | 01, 05 |
| Player themes (9) | `classic, mountain, mesh, minimal, acoustic, vinyl, coast, starry, immersive`; carousel with APPLY/USING; Special badge; default `classic` | 06, 12 §4, 16 |
| Skins (8) | Plum, Aurora, Afterglow, Ocean, Sunrise, Forest, Mono, Pure black; procedural wallpapers; custom image wallpaper copied into private storage | 05, 06 |
| Theme modes | System / Dark / AMOLED / Light; dynamic accent from artwork; reduced motion | 02, 05, 06 |
| Audio | One ExoPlayer in `MusicPlaybackService`; `AudioTransitionController`/`FadingPlayer`; `SleepTimerEngine`; `ErrorPolicy`; platform `AudioFx` (equalizer, bass boost, virtualizer, preset reverb) with capability checks; speed 0.5–2.0x; stop after this track; sleep timer with 10 s fade; headset-unplug pause | 04, 13 |
| Queue | `QueueModel` (original/shuffled order, seed, no Media3 shuffle), persistent snapshot restored paused | 14 |
| Library | Recoverable MediaStore scan, identity model (`ms:`/`saf:`/`copy:`), SAF folder scan, explicit Copy workflow, SD card handling, permission state machine, visible-count query, artwork cache | 14 |
| Statistics | `SessionTracker`, `StatsPolicy`, month keys, monthly recap, share card | 15, 08 |
| Backup | **Classic** favourites JSON (`pulseplayer.favorites` v1, fuzzy matcher) and **Full backup (reference format)** (ZIP-in-`.wav`) kept separate | 07, 10, 11, 15 |
| Lyrics | User-picked `.lrc`/`.txt` import, synced highlight | 16 |
| Other | Glance widget (2x2/4x1/4x2), ringtone with pending action, launcher icon, README | 08, 13 |

## 3. Conflict resolutions (apply exactly; this is where file 18 overrides older files)
| # | Old rule | New request | Resolution |
|---|---|---|---|
| C1 | "No INTERNET permission. Ever." (00 §1) | Streaming, LRCLIB, Deezer, AI, Cast, servers | **Two product flavors** in `app/build.gradle.kts`: `offline` (identical to today, **no INTERNET permission**, all network code excluded at build time) and `full` (adds `INTERNET`, `ACCESS_NETWORK_STATE`). All network code lives in package `com.pulse.player.net` behind **one `NetworkGateway`** and a **master switch "Online features" (default OFF)** plus a per-service switch. The README states the permission claim per flavor. `full` is the day-to-day build. |
| C2 | Out of scope: streaming, accounts, online lyrics/art, tag editing, Android Auto (01 §6) | In scope now | Allowed only as opt-in, gated features (section 15 and 17). **Still forbidden: ads, analytics, telemetry, crash uploads, tracking.** |
| C3 | Crossfade "not included" (00 §9) | Smart crossfade, beat matching, gapless | Implement as an **experimental, default-OFF** `CrossfadeEngine` inside `PlaybackEngine` (section 15.4). Gapless stays the default path. Honest label "Experimental". |
| C4 | Only `AudioTransitionController` writes `player.volume` | Crossfade needs two volumes; ReplayGain, per-track volume | The controller is extended to be the sole writer of **both** engine volumes. ReplayGain, per-track gain and loudness are applied by an `AudioProcessor` gain stage, **never** by writing `volume`. The grep gate becomes "no `.volume =` outside `AudioTransitionController`". |
| C5 | Only `SessionTracker` writes play history | History import (Last.fm, Spotify, ListenBrainz, YouTube), editable history | Live listening is still written **only** by `SessionTracker`. Imports and edits go through one `HistoryWriter` (single gate) and mark rows with a new `origin` column (`LIVE`, `IMPORT_LASTFM`, `IMPORT_SPOTIFY`, `IMPORT_LISTENBRAINZ`, `IMPORT_YOUTUBE`, `MANUAL`). Grep gate: only these two classes call `play_events` inserts. |
| C6 | "Themes are presentation only: no second player" | Crossfade engine has two decoders | The rule still applies to themes/layouts. The service has **one** `MediaSession` facade (`FadingPlayer`) and **one** owner (`PlaybackEngine`); the crossfade engine's second ExoPlayer is private to it and never exposed to UI. |
| C7 | Media3 shuffle never enabled | Namida Track Play Mode, insert-after-latest, repeat N times | All implemented in `QueueModel` and `PlayerController`; Media3 shuffle remains disabled. |
| C8 | Never move/rename/delete music; copy only through the explicit Copy action | Classic export copies favourites into `Music/Pulse Player/`; tag editor writes tags | Copy only after a confirmation dialog listing count and size (reuses file 14 §4.2). Tag editing modifies file contents **only** after an explicit confirm, with an optional "keep a backup copy" switch; never batch-edit silently. Sources are never moved or deleted. |
| C9 | No `MANAGE_EXTERNAL_STORAGE` | Folder detection, tag writes | Still forbidden. Use MediaStore (own files and `RELATIVE_PATH` queries), SAF persisted trees, and `MediaStore.createWriteRequest` for tag writes. |
| C10 | Unknown player-theme id falls back to `classic`; "exactly 9 themes" (12 §4, 17 `ThemeRegistryTest`) | New themes from Namida/PixelPlayer; Atmospheric Queue is the premier layout | Keep **two separate registries**: `PlayerThemes` (visual themes; default stays `classic`; count becomes N after this merge) and `PlayerLayoutMode` (structure of the player; default and unknown-fallback is `ATMOSPHERIC_QUEUE`). Update `ThemeRegistryTest` to the new canonical list and add `PlayerLayoutModeTest`. Update file 12 §4's table in the repo. |
| C11 | `Track` data class in file 04 | New spec shows a tiny `Track(id,title,artist,duration:String,isPlaying)` | Do **not** replace the existing `Track`. The layout uses its own `QueueRowUi(id,title,artist,durationLabel,isCurrent,artRef)` mapped from `QueueItem`. |
| C12 | "No copyrighted images" | "Copy the exact designs" | Recreate the **look** (layout, spacing, shape, colour logic, motion) in code from the references. Do not copy image assets, fonts or icons unless their licence allows it; do not copy source code verbatim (section 4). Backgrounds stay procedural. |
| C13 | `Visualizer` effect needs `RECORD_AUDIO` | Visualizers, particles | Do **not** request `RECORD_AUDIO`. Tap PCM in an `AudioProcessor` (peak + small FFT) and publish a throttled `AudioLevels` flow. |
| C14 | "No fake data" | `@Preview` needs sample data | Sample data is allowed **only** inside `@Preview` functions in a `preview/` source file never referenced by production code. |
| C15 | `#D4AF37` amber is mandated for the active queue row | Contrast rule ≥ 4.5:1 (file 02) | Use `#D4AF37` for the equalizer bars, a left accent bar and dark-surface text. On the light card use a darker amber token (`ActiveAmberOnLight`, about `#8A6D00`, about 4.7:1 on `#F9F9FB`; **verify with a contrast unit test**). The active row also uses bold weight and the animated bars as a second cue. |
| C16 | "Request permissions only on Scan" (03 §8) | Request everything on first launch | Replace with the onboarding flow in section 14 (rationale first, each step skippable, features degrade gracefully). |
| C17 | README says "no internet permission" | Flavors | README documents both flavors (section 16). |

## 4. Research protocol and licensing gate (do this before building any parity feature)
1. **Read the two reference folders and the upstream repos.** Do not rely on the feature lists in this file alone; they came from the owner's description and may be incomplete or out of date.
2. Produce `docs/spec/PARITY_INVENTORY.md` with one row per feature **and** per screen/skin/theme/layout found: `ID | Source app | Name | Where it lives (file/class) | What it does | Pulse equivalent | Status (PRESENT/PARTIAL/NEW) | Phase | Settings location`. Start from the ID tables in section 6 and **add rows you find** that are not listed. Do not drop any.
3. **Licence check.** Record each reference app's licence and each third-party library's licence in `docs/THIRD_PARTY_NOTICES.md`. Namida is licensed under a strong copyleft licence (verify: GPL-3.0). Copying its **code** into Pulse Player would oblige Pulse Player to the same licence. **Default policy: re-implement from observed behaviour and from the design, do not paste code.** If the owner wants to adopt a copyleft licence for the whole app, that is an owner decision recorded in the README; stop and ask before copying any code.
4. Streaming/downloading from video sites and SponsorBlock-style features can violate a platform's terms and app-store policies. Build them last, behind the `full` flavor and their own switch, and record the legal decision in `docs/DECISIONS.md` (section 15.9). Do not build them silently.
5. Anything you cannot find in the references or cannot implement on Android: do **not** hide it. Show it in Settings as disabled with a one-line reason, and list it in the final report.

## 5. Architecture additions
### 5.1 Four independent customisation axes (never mix them)
| Axis | What it controls | Picker | Persisted key |
|---|---|---|---|
| **Theme mode / colour source** | Light / Dark / AMOLED / System, plus colour source: *Pulse palette*, *Material You (wallpaper)*, *Artwork-driven*, *Custom seed colour* | Settings → Appearance → Themes | `themeMode`, `colorSource`, `seedColor` |
| **Skin** | App background gradient/wallpaper (8 existing + all new ones) | Skin theme screen | `skinId`, `wallpaper` |
| **Player theme** | Visual treatment of the Now Playing artwork area (9 existing + new) | Player theme carousel | `playerThemeId` |
| **Player layout mode** | Structure of the whole player screen (Atmospheric Queue, Classic, Widescreen, Waveform, …) | Layout picker sheet in the player + Settings → Layouts | `playerLayoutMode` |
| **Button layout preset** | Arrangement of transport and quick-action buttons | Button-layout picker sheet in the player + Settings → Layouts | `buttonPresetId` (+ optional per-layout override map) |
Every picker shows live previews. Applying one never restarts playback.

### 5.2 Packages (add; do not move existing)
`com.pulse.player.net` (gateway, lrclib, deezer, ai, sources, party) · `.audio` (processors, parametric EQ, crossfade, replaygain, audio-path) · `.library` (separators, tag editor, smart playlists, search index, metadata) · `.ui.layouts` · `.ui.buttons` · `.ui.tokens` · `.core.error` · `.core.flags`.

### 5.3 Capability registry (no dead buttons)
A `Capabilities` object evaluated at runtime: API level, `audiofx` availability, USB/bit-perfect support, float-output support, network flavor, Google Play services (Cast), TagLib binding loaded, battery-exemption allowed. Every setting row asks the registry. States: `Available`, `NeedsPermission`, `NeedsOnlineSwitch`, `Unsupported(reason)`. Unsupported rows are **visible, disabled, with the reason**.

### 5.4 Source abstraction
`MusicSource` interface (`id, type, displayName, list(), open(trackId), capabilities`). Implementations: `MediaStoreSource`, `SafSource`, `CopiedSource` (exist), then `SubsonicSource`, `JellyfinSource`, `WebDavSource`, `SmbSource`, `YouTubeSource` (phase 16). Extend the identity model (file 14 §2) with `sourceType` values `SUBSONIC, JELLYFIN, WEBDAV, SMB, YT` and ids `sub:<server>:<id>`, `jf:<server>:<id>`, `dav:<sha1>`, `smb:<sha1>`, `yt:<videoId>`. **Reconcile availability only within one source** (the scan rule from file 14 §1 applies per source).

### 5.5 Schema version 4 (one migration `MIGRATION_3_4`, exported schema JSON, migration test, pre-migration DB copy as in file 17 §3)
Adjust names to the project; keep defaults on every new column.
- `tracks`: add `genre`, `genresRaw`, `albumArtistNorm`, `year` (exists), `bitrate`, `sampleRate`, `bitDepth`, `codec`, `replayGainTrackDb`, `replayGainAlbumDb`, `bpm`, `language`, `hasEmbeddedLyrics`.
- New: `track_tags(trackId, tag)`, `track_moods(trackId, mood)`, `track_meta(trackId, rating 0..5, language, note)`, `track_audio_profile(trackId, speed, pitchSemitones, gainDb, eqJson, presetId)`, `bookmarks(id, trackId, positionMs, label, createdAtMs)`, `chapters(trackId, startMs, title)`, `saved_queues(id, name, createdAtMs, currentIndex, positionMs)` + `saved_queue_items(queueId, position, trackId)`, `smart_playlists(id, name, rulesJson, createdAtMs)`, `artist_images(artistNorm, url, filePath, fetchedAtMs, source)`, `sources(id, type, name, baseUrl, authRef, enabled, lastSyncMs)`, `cache_entries(trackId, filePath, bytes, priority, lastUsedMs)`, `waveforms(trackKey, peaksBlobPath, version)`.
- `play_events`: add `origin TEXT NOT NULL DEFAULT 'LIVE'`.
- `lyrics`: add `source TEXT NOT NULL DEFAULT 'IMPORT'`, `format TEXT NOT NULL DEFAULT 'LRC'` (`LRC`, `TTML`, `PLAIN`, `WORD_LRC`).
- Secrets (server passwords, AI keys, tokens) are **never** in Room or plain DataStore: use a Keystore-backed encrypted store.

### 5.6 Settings information architecture (all new features live here; nothing is hidden elsewhere)
Settings → **Appearance** (theme mode, colour source, seed colour, skin, player theme, player layout, button layout, corner radius, navigation style, wallpaper, animated background, visualizer, particles, party-mode edge glow, reduced motion, battery-saver visuals) · **Library** (scan, folders and exclusions, size/duration filters, duplicates, artist/genre separators, feat./ft. extraction, album-artist grouping, default sorts, page/tab order and visibility, folder images and `.info.txt`, tag editor) · **Playback** (gapless, fade, crossfade, skip silence, speed, pitch, repeat-N, track play mode, insert behaviour, sleep timer modes, pausing scenarios, persistent queues, history rules) · **Sound** (effects, parametric EQ, AutoEQ import, presets, ReplayGain, loudness enhancer, mono, output device, bit-perfect/USB/float, audio path viewer) · **Lyrics** (sources and priority, online fetch, editor defaults) · **Gestures** (swipe actions, artwork gestures, layout-cycle gesture) · **Online & Privacy** (master switch, per-service switches, data-saver, what each service sends) · **Sources** (servers, YouTube, cache) · **AI** (provider, key, consent preview) · **Connectivity** (Cast, Android Auto, widgets, Listening Party, local sync) · **Stats & History** (rules, import, export, edit) · **Backup & Transfer** (Classic, Full backup) · **Permissions** (status of every permission with a fix button) · **About** (version, flavor, licences).

## 6. Feature catalogue (traceability IDs; every ID must end the project as Implemented, Gated, or Unsupported-with-reason)
Legend — Priority: **P0** core, **P1** important, **P2** advanced/optional. Phase numbers refer to section 17. "Limit" = honest device/legal constraint to show in the UI.

### 6.1 Namida — Library and indexing
| ID | Feature | Build approach in Pulse Player | Settings home | Ph | Pri | Limit |
|---|---|---|---|---|---|---|
| NAM-L1 | Indexer + **tag editor** (TagLib) | TagLib binding (choose an actively maintained Android binding; verify licence); read all tags into `tracks`; editor screen with title/artist/album/album artist/genre/year/track/lyrics/artwork; writes via `MediaStore.createWriteRequest`; confirm dialog; optional backup copy; rescans the file afterwards | Library → Tag editor | 12 | P1 | Needs write grant; some formats read-only |
| NAM-L2 | Artists and genres **separators** | Configurable delimiter list (`;`, `,`, `/`, `&`, ` feat. `…) + exceptions list ("AC/DC", "Tyler, The Creator"); a track can belong to several artists/genres; applied at index time and re-runnable | Library → Separators | 12 | P1 | |
| NAM-L3 | **Prevent duplicate tracks** | Duplicate = same normalised title + artist + duration within 2 s (+ optional size); setting "hide duplicates" is display-only (never marks unavailable); duplicates screen to review | Library → Duplicates | 12 | P1 | |
| NAM-L4 | Minimum file size and duration filters | Display-only filters (extends the existing min-duration); size in KB | Library → Filters | 12 | P0 | |
| NAM-L5 | Folders-based library with excluded folders | Exists; add per-folder include/exclude tree, "show only these folders" mode | Library → Folders | 12 | P0 | |
| NAM-L6 | Sort by almost any track/album property | Sort sheet with title, artist, album, album artist, genre, year, duration, size, bitrate, date added, date modified, play count, last played, rating, BPM, file name, folder; asc/desc; remembered per page | Library → Sorting | 12 | P0 | |
| NAM-L7 | Folder images (`cover.jpg`, `folder.jpg`…) and `.info.txt` | Artwork resolution order: embedded → folder image → generated; `<name>.info.txt` sidecar (read the Namida source for the exact format, document it in `docs/INFO_TXT.md`) | Library → Sidecar files | 12 | P2 | |
| NAM-L8 | Extract feat./ft. artists into Artists tab | Parser on title/artist; creates artist links without altering file tags; toggle | Library → Separators | 12 | P1 | |

### 6.2 Namida — Look and feel
| ID | Feature | Build approach | Settings home | Ph | Pri | Limit |
|---|---|---|---|---|---|---|
| NAM-U1 | Material 3-like theme + dynamic colours from current artwork | New `colorSource` options; transitions animate (500 ms); contrast-guarded | Appearance → Themes | 10 | P0 | |
| NAM-U2 | Pages: Home, Tracks, Albums, Artists, **Genres**, Playlists, Folders, **Queues**, **Most Played**, **Moods**, **Tags**, **Ratings**, **Languages**, **Stats** | Existing tabs stay. Add pages: Genres, Queues (saved sessions), Most Played (range chips: today/week/month/year/all/custom), Moods, Tags, Ratings (1–5 stars), Languages, Stats. Page order and visibility configurable; unused pages live in a "More" drawer section | Library → Pages | 12 | P0/P1 | Moods/Tags/Ratings/Languages are user data (empty until used) |
| NAM-U3 | **Waveform seekbar** | Decode PCM once in the background (`MediaCodec`/Media3 extractor) into ~200–400 peak buckets, cache per track in `waveforms`; seek bar variant draws bars, played part highlighted; falls back to normal slider while generating or if decode fails | Appearance → Seek bar style | 11 | P1 | Generation is per-track and takes time; show progress |
| NAM-U4 | **Widescreen player layout** | `PlayerLayoutMode.WIDESCREEN` (artwork left, controls/queue right) used automatically at width ≥ 600 dp or chosen manually | Layouts | 11 | P0 | |
| NAM-U5 | **Visualizers**, animated backgrounds, custom wallpapers | Audio tap (C13) → `AudioLevels`; visualizer styles: bars, wave, circle, dots; animated backgrounds procedural; custom wallpapers exist | Appearance → Visualizer | 11 | P1 | Off when reduced motion or battery saver |
| NAM-U6 | Extensive customisation in Settings | Section 5.6 | Settings | 9 | P0 | |
| NAM-U7 | **Animating thumbnail & particles** reacting to audio peak | Artwork scale/glow pulse and particle canvas driven by `AudioLevels` | Appearance → Particles | 11 | P2 | |
| NAM-U8 | **Miniplayer Party Mode** (edge breathing effect) | Mini player border glow that breathes with the beat; toggle | Appearance → Party mode | 11 | P2 | |
| NAM-U9 | Swipe actions and artwork gestures | Configurable per side (play next, add to queue, favourite, add to playlist, delete from playlist, select); artwork: swipe = next/previous, double-tap = ±10 s, long-press = options | Gestures | 12 | P1 | Layout-cycle gesture must not clash (section 7.6) |

### 6.3 Namida — Sound
| ID | Feature | Build approach | Settings home | Ph | Pri | Limit |
|---|---|---|---|---|---|---|
| NAM-S1 | **Parametric equalizer + AutoEQ import** | `ParametricEqProcessor` (cascaded biquads: peaking, low/high shelf, low/high pass; up to 10 bands; preamp). Import AutoEQ "ParametricEQ.txt" (`Filter n: ON PK Fc … Hz Gain … dB Q …`, `Preamp: … dB`) and GraphicEQ text; preview curve; save presets. Mutually exclusive with platform `AudioFx` EQ (toggle, to avoid double processing) | Sound → Parametric EQ | 13 | P1 | CPU cost; disabled in bit-perfect mode |
| NAM-S2 | **Bit-perfect, USB DAC direct, exclusive mode, 32-bit float** | Float: `DefaultRenderersFactory.setEnableAudioFloatOutput(true)`. Bit-perfect/exclusive: Android 14+ `AudioManager` preferred mixer attributes with bit-perfect behaviour, only if the routed device supports it; show detected capability | Sound → Output | 13 | P2 | **Android 14+ and supporting hardware only**; otherwise visible, disabled, reason shown. Disables all effects while active (state it) |
| NAM-S3 | **Audio path viewer** | Live list: source file (codec, sample rate, bit depth, channels, bitrate) → decoder → each active processor (EQ, gain, crossfade, effects) → output sink → device (type, name, rate). Built from Media3 analytics + `AudioManager.getDevices` | Sound → Audio path | 13 | P1 | |
| NAM-S4 | Output device selection, **mono audio** | `setPreferredAudioDevice(AudioDeviceInfo)` (API 23+); mono via a `ChannelMixingAudioProcessor` | Sound → Output | 13 | P1 | Bluetooth names need `BLUETOOTH_CONNECT` (API 31+) |
| NAM-S5 | Effects: **bass enhancer, reverb, virtual surround, 8D audio, vocal remover** | Bass: platform `BassBoost` plus optional low-shelf processor. Reverb: platform preset reverb exists; add a processor reverb with size/damping (P2). Virtual surround: platform `Virtualizer` plus crossfeed processor. 8D: LFO auto-pan processor (period 2–20 s, depth). Vocal remover: mid-side centre cancellation processor | Sound → Effects | 13 | P1/P2 | Quality varies; labelled "approximate" |
| NAM-S6 | **Smart crossfade with beat matching**, gapless, play/pause fade, **skip silence** | Gapless: default behaviour (verify gapless flags per format). Fade: exists. Skip silence: `ExoPlayer.setSkipSilenceEnabled`. Crossfade + beat matching: section 15.4 | Playback → Transitions | 13 | P1/P2 | Experimental |
| NAM-S7 | **Speed and pitch** (semitones, 432 Hz) and **loudness enhancer** | Independent speed (0.25–3.0x) and pitch (-12…+12 semitones; "432 Hz" preset = ratio 432/440); platform `LoudnessEnhancer` | Playback → Speed and pitch | 13 | P1 | |
| NAM-S8 | **Per-track** speed, pitch, volume, equalizer; sound presets | `track_audio_profile`; applied on track transition through `PlaybackEngine`; "Save for this track" in the player menu; global sound presets (named bundles) | Sound → Presets | 13 | P1 | |
| NAM-S9 | **ReplayGain** | Read `REPLAYGAIN_*` tags; track/album mode, preamp, clipping guard; applied via gain `AudioProcessor` (C4) | Sound → ReplayGain | 13 | P1 | Needs tags; "no ReplayGain tags" count shown |

### 6.4 Namida — Lyrics and smart features
| ID | Feature | Build approach | Settings home | Ph | Pri | Limit |
|---|---|---|---|---|---|---|
| NAM-Y1 | Lyrics: synced, plain, **word-synced** (LRC enhanced / TTML) + **editor** | Parsers for LRC, enhanced LRC (`<mm:ss.xx>`), TTML; editor with tap-to-timestamp while playing; saves to `lyrics` (and optionally a `.lrc` sidecar via SAF). Source priority: embedded tag → sidecar → saved → LRCLIB (opt-in) → import | Lyrics | 12 | P1 | |
| NAM-Y2 | **Smart playlists** with flexible rules, usable in search | Rule tree (AND/OR groups) over: title/artist/album/genre/tag/mood/rating/language/year/duration/play count/last played/date added/folder/favourite; compiled to SQL; live-updating; searchable via `smart:` prefix | Playlists | 12 | P1 | |
| NAM-Y3 | **Listening Party** (queue sync with friends, online or LAN) | LAN only by default (section 16.3). Online variant requires a relay the owner provides: not built unless specified | Connectivity | 16 | P2 | Needs `NEARBY_WIFI_DEVICES` (API 33) |
| NAM-Y4 | **Local network sync** of app data (history, playlists, favourites, queues, lyrics) | Pairing code, encrypted transport, per-category merge preview, same rules as Full backup (file 15 §2.4) | Connectivity | 16 | P2 | |
| NAM-Y5 | **Bookmarks and chapters** | `bookmarks` table; list in player menu; chapters from ID3 `CHAP`, MP4 chapters; chapter list sheet | Playback | 12 | P1 | |
| NAM-Y6 | **Stats charts and "Your Year"** | Compose-canvas charts from `play_events` (time per day/week/month, top tracks/artists/albums/genres, hours heat-map, streaks); "Your Year" story pages with a share card (extends recap, file 08) | Stats & History | 12 | P1 | Counts come from `counted = 1` sessions |
| NAM-Y7 | **Search** by title, artist, album, filename, lyrics with **typo tolerance** | Room FTS5/FTS4 index over normalised fields + lyrics; fuzzy fallback (trigram or Levenshtein ≤ 2) ranked after exact; 250 ms debounce | Library → Search | 12 | P0 | |
| NAM-Y8 | **Persistent queues** | Save/load named queue sessions (the "Queues" page); autosave of the last queue exists (file 14 §7) | Playback | 12 | P1 | |
| NAM-Y9 | **Flexible history** | Editable (delete/add/edit entries), import, export; counting rules configurable (min seconds, min percentage) — the constants in `StatsPolicy` become settings with safe bounds | Stats & History | 12 | P1 | |
| NAM-Y10 | **Most Played** in any time range | Page + playlist; range chips and custom range | Library → Pages | 12 | P0 | |
| NAM-Y11 | Custom playlist order and artworks; **M3U** and server auto-import | Playlist cover from a picked image (copied to private storage) or auto collage; M3U/M3U8 import/export through SAF; map entries with the Classic matcher tiers | Playlists | 12 | P1 | |
| NAM-Y12 | **Global tracks selection** | Selection persists across pages with one floating selection bar (play, queue, playlist, favourite, tag, delete from playlist) | Gestures | 12 | P1 | |
| NAM-Y13 | **Sleep timer** by tracks or minutes; **pausing scenarios** (calls, notifications, volume 0) | Add "after N tracks" mode to `SleepTimerEngine`; scenario toggles: pause on call (focus), pause or duck on notification, pause when media volume is 0 and resume when raised | Playback → Sleep and pausing | 13 | P1 | |
| NAM-Y14 | **Smort Tracks** and **Lost Memories** | Smart generated mixes from local stats: "Smort" (verify the exact Namida meaning in its source), "Lost Memories" (tracks you played on this date in earlier years, or loved but not heard for N months) | For you | 12 | P2 | Hidden until data exists |
| NAM-Y15 | **Track Play Mode** | When a track is tapped, the generated queue is one of: selected only / current list or search results / its album / first artist's tracks / first genre's tracks | Playback → Track play mode | 12 | P1 | |
| NAM-Y16 | **Insert after latest inserted** | `QueueModel` remembers the last "play next" inserted id; new inserts go after it; toggle between this and "directly after current" | Playback → Queue | 12 | P1 | |
| NAM-Y17 | **Repeat for N times** | `RepeatState(mode, remaining)`; repeat button long-press sets N (2–99); badge shows remaining; sessions count each loop (file 15 §1.2) | Playback → Repeat | 12 | P1 | |
| NAM-Y18 | **Import history** from YouTube, Last.fm, Spotify, ListenBrainz | File-based importers first (Takeout JSON, Spotify extended history JSON, ListenBrainz export, Last.fm export CSV/JSON), API-based optional behind the online switch; all rows through `HistoryWriter` with `origin`; match to the library by the tier rules (file 10 §5); preview with counts before commit; unmatched list | Stats & History | 15 | P2 | |

### 6.5 Namida — Streaming and YouTube (all behind flavor `full` + "Online features" + service switch; see section 15.9 legal gate)
| ID | Feature | Build approach | Ph | Pri |
|---|---|---|---|---|
| NAM-V1 | Best video/audio quality, audio only, **data saver** | Quality policy object (Wi-Fi vs mobile, audio-only toggle); respects `ConnectivityManager` metered state | 16 | P2 |
| NAM-V2 | **Multiple profiles sign-in**; feed, subscriptions, playlists, notifications | Profile model; account data kept in encrypted store; decision required (15.9) | 16 | P2 |
| NAM-V3 | **Multi-library** Local / YT / Subsonic / Jellyfin / WebDAV / SMB with **offline caching** | `MusicSource` implementations (5.4); Subsonic and Jellyfin REST; WebDAV (PROPFIND); SMB via an LGPL-compatible library; unified library view with a source filter chip; per-source sync and reconcile | 16 | P1/P2 |
| NAM-V4 | **Video view** with gestures (volume, brightness, seek, fullscreen, speed) | Media3 `PlayerView` surface for video items; gesture layer; same single playback service | 16 | P2 |
| NAM-V5 | **SponsorBlock**, Return YouTube Dislike, segments and heatmap | Public APIs behind the gateway; skip/mute segments by category; heatmap on the seek bar | 16 | P2 |
| NAM-V6 | **Subtitles and captions** | Media3 text tracks; style options | 16 | P2 |
| NAM-V7 | **Downloads, caching, offline playback**, cache priority | Download manager (`DATA_SYNC` foreground service type), app-owned storage, LRU with priorities (`cache_entries`), pause/resume | 16 | P1 |
| NAM-V8 | Split downloads by chapters; remove sponsor segments | Post-process step using chapters and segments | 16 | P2 |
| NAM-V9 | Download **tags config and filename builder** | Template editor (`%artist% - %title%`), tag mapping, preview | 16 | P2 |
| NAM-V10 | **Auto title/artist/album extraction** from video titles | Rule-based parser (patterns, "Artist - Title", brackets, "(Official Video)"), editable preview | 16 | P2 |

### 6.6 PixelPlayer
| ID | Feature | Build approach | Settings home | Ph | Pri | Limit |
|---|---|---|---|---|---|---|
| PIX-U1 | **Material You** dynamic colour from wallpaper | `dynamicDarkColorScheme/dynamicLightColorScheme` on API 31+; below 31 the option is disabled with a reason | Appearance → Themes | 10 | P0 | API 31+ |
| PIX-U2 | Smooth animations and **micro-interactions on every button** | Shared `PulseClickable` modifier (scale spring 0.96, ripple, haptic optional) used by all interactive components; heart pop, play/pause morph; respects reduced motion | Appearance | 10 | P0 | |
| PIX-U3 | **Adjustable corner radius** and navigation bar | `LocalCornerScale` (0.0–2.0) feeds `Shapes` and component radii; **navigation style**: Top tabs (default, existing), Bottom bar, Floating bottom bar, Rail | Appearance → Shape and navigation | 10 | P1 | Rail forced on width ≥ 840 dp unless overridden |
| PIX-U4 | Dark/Light with auto/manual switching | Exists (System/Dark/AMOLED/Light); add schedule option (follow time of day) | Appearance → Themes | 10 | P1 | |
| PIX-U5 | **Album art colour extraction** | Exists (Palette); upgrade to a quantiser-based scheme with tonal palette for container colours | Appearance | 10 | P1 | |
| PIX-P1 | Media3 ExoPlayer with **FFmpeg** support | Optional decoder extension for extra formats (APE, DSD, etc.). Pick a Maven-available Media3 FFmpeg extension or build one; note extra APK size and LGPL obligations; build as an optional flavor dimension or a feature toggle | Playback → Decoders | 13 | P2 | Large APK impact; verify licence |
| PIX-P2 | **Drag-and-drop queue reordering** | Long-press drag handle in queue sheet/panel; updates `QueueModel.move` | Playback → Queue | 12 | P0 | |
| PIX-P3 | Gapless and **custom transition/crossfade configuration** | Duration slider 0–12 s, curve (linear/equal-power/S), "only when same album gapless"; same engine as NAM-S6 | Playback → Transitions | 13 | P1 | |
| PIX-L1 | Formats MP3, FLAC, AAC, OGG, WAV and more | Verify scanner extension/MIME list; show "format not supported by this device" card | Library | 12 | P0 | |
| PIX-L2 | Browse by Songs, Albums, Artists, **Genres**, Folders | Genres page (NAM-U2) | Library → Pages | 12 | P0 | |
| PIX-L3 | **Smart artist parsing with configurable delimiters** and **album-artist grouping** | Same engine as NAM-L2; grouping toggle: by album artist vs by track artist | Library → Separators | 12 | P1 | |
| PIX-L4 | Folder filtering (choose scan directories) | Exists via exclusions and SAF trees; add an "allow list" mode | Library → Folders | 12 | P0 | |
| PIX-D1 | Full-text search | NAM-Y7 | | 12 | P0 | |
| PIX-D2 | **Daily Mix** (AI-powered personalised playlist) | **Local first:** generated from play history, favourites, genres, recency decay, with a seeded daily refresh and diversity rules. **AI optional:** the same mix refined by the AI provider (15.6) | For you | 12/15 | P1 | AI part needs online + key |
| PIX-D3 | Playlists, **statistics** | Exist; extend with NAM-Y6 | | 12 | P0 | |
| PIX-Y1 | **Synchronised LRC via LRCLIB** | `LrclibClient` (get by signature: title, artist, album, duration; search fallback); cache result in `lyrics` with `source = LRCLIB`; opt-in per service; manual "Fetch lyrics" button plus optional auto-fetch on Wi-Fi | Lyrics | 15 | P1 | Needs online |
| PIX-Y2 | **Lyrics editing**, scrolling lyrics display | NAM-Y1; smooth centred auto-scroll with tap-to-seek line | Lyrics | 12 | P1 | |
| PIX-A1 | **Deezer artist images** with memory LRU + DB caching and fallback icons | `DeezerArtistClient` (public search endpoint), result cached in `artist_images` and image files in private storage; never re-fetch before TTL (30 days); gradient initials fallback (exists) | Online & Privacy | 15 | P1 | Needs online |
| PIX-C1 | **Chromecast / Google Cast** | Media3 Cast module and `CastPlayer`; because the files are local, serve them from a token-protected local HTTP server on the LAN while casting; hide the Cast button if Google Play services are missing | Connectivity | 15 | P2 | Needs Play services + online flavor |
| PIX-C2 | **Android Auto** | Convert the service to `MediaLibraryService` with a browse tree (Recent, Favourites, Playlists, Albums, Artists, Folders); `automotive_app_desc.xml` | Connectivity | 15 | P1 | Needs a head unit or Desktop Head Unit to verify |
| PIX-C3 | **Glance home-screen widgets** | Exists (one responsive widget); add 2 extra styles (compact art-centred, controls-only) selectable per widget instance | Connectivity → Widgets | 15 | P1 | |
| PIX-X1 | **Tag editor** (MP3, FLAC, M4A) | NAM-L1 | Library | 12 | P1 | |
| PIX-X2 | **AI playlist generation** (Gemini, DeepSeek, OpenAI, others) | Provider interface `AiProvider` (`generatePlaylist(prompt, libraryDigest)`); user-supplied keys; consent screen; only metadata is sent (section 15.6) | AI | 15 | P2 | Needs online + key |
| PIX-X3 | FFmpeg, background playback with full media session | Exists; keep notification, lock screen, Auto metadata consistent | | 13 | P0 | |

### 6.7 Design inventory (skins, themes, layouts)
Everything below is **added to** the existing pickers; nothing existing is removed. **The names are provisional; the final list is whatever Phase 9's inventory finds in the two apps' code.**
| Picker | Existing (keep) | Add from Namida | Add from PixelPlayer | Add from this brief |
|---|---|---|---|---|
| Skins | Plum, Aurora, Afterglow, Ocean, Sunrise, Forest, Mono, Pure black | Every Namida background/wallpaper/animated background found in the inventory (procedural re-creations) | Every PixelPlayer background/tonal-surface look found | Forest Mist (matches Atmospheric), Material You tonal skin, Artwork-driven skin |
| Themes (colour) | System, Dark, AMOLED, Light + dynamic accent | Namida's artwork-driven scheme, its accent presets | Material You, tonal palettes, custom seed colour, adjustable corner radius | Time-of-day schedule |
| Player themes | classic, mountain, mesh, minimal, acoustic, vinyl, coast, starry, immersive | Every distinct Namida player look found | Every distinct PixelPlayer player look found | none |
| Layout modes | Current Now Playing structures | Standard, Widescreen, Waveform, Queue-side variants found | Expanding sheet, full-screen artwork and similar found | **ATMOSPHERIC_QUEUE** (premier) |
| Button presets | Current centred arrangement | Any control arrangement found | Any control arrangement found | The 15 presets in section 8 |

## 7. New premier layout: ATMOSPHERIC_QUEUE
### 7.1 Model
File `ui/screens/PlayerLayoutMode.kt` (or the project's equivalent; first locate the real file in Phase 9): add enum entry `ATMOSPHERIC_QUEUE` — display name **"Atmospheric Queue"**, subtitle **"Dark mist overlay & light queue card"**, icon `Icons.Rounded.Splitscreen`. `PlayerLayoutMode.fromId(id)` returns `ATMOSPHERIC_QUEUE` for null/unknown ids. It is listed **first** in the picker with a "New" badge. Existing modes keep their ids and behaviour. Stored under `playerLayoutMode`. Update `PlayerLayoutModeTest`: every entry has a unique id, a non-blank name/subtitle, `fromId` round-trips every id, unknown → `ATMOSPHERIC_QUEUE`, picker order is stable.

### 7.2 Background component
File `ui/components/AtmosphericBackground.kt`: a Compose component drawn with `Canvas` (and an optional AGSL `RuntimeShader` path on API 33+ with an automatic Canvas fallback). Content: forest/mist atmosphere — a deep green-black vertical gradient, 3–5 layered soft fog bands made of overlapping radial gradients that drift slowly at different speeds, faint pine-silhouette ridges at the bottom of the upper section drawn with `Path`, and a dark semi-transparent scrim gradient on top so white text always has ≥ 4.5:1 contrast. Rules: procedural only (no photos); static when reduced motion or battery saver or when playback is paused (draw one frame); cache the static layers into one `ImageBitmap` once per size; never recompute per frame; optional tint from the artwork's dominant colour (subtle, capped) when `colorSource = Artwork-driven`. Provides `AtmosphericBackground(modifier, tint, animate, content)`.

### 7.3 Screen structure
File `ui/screens/layouts/AtmosphericQueuePlayerLayout.kt`. One **stateless** composable; a `Column(fillMaxSize)` split by weights (sizes are for a 20:9 phone; they must still work everywhere, see 7.5).

**Upper section — weight 1.1f–1.25f (use 1.2f)**, on `AtmosphericBackground`, content inside `safeDrawing` insets:
1. **Top bar** (min height 56 dp, items ≥ 48 dp): left `IconButton` chevron down (`KeyboardArrowDown`, `onCollapse`); centre text tab switcher **Song** (bold, active, underline or pill) and **Lyrics** (muted); right three icons in this order: theme (`Icons.Rounded.Checkroom`, `onOpenTheme`), layout switcher (`Icons.Rounded.DashboardCustomize`, `onOpenPicker`), overflow (`MoreVert`, opens the track-options sheet). Tabs and right icons never overlap: below 360 dp width the Song/Lyrics switcher shrinks to icon-sized chips; if still too tight the theme icon moves into the overflow menu (priority: chevron > tabs > layout picker > overflow > theme).
2. **Details block** (weighted spacer above and below so it floats in the middle): title (headline, bold, white, centred, max 2 lines, ellipsis; marquee when reduced motion is off and it overflows), artist/subtitle (medium, white at 70 % alpha, 1 line). With the Lyrics tab active this block collapses to a single title line and the lyrics panel (file 16 §6) fills the space.
3. **Progress**: thin `Slider` (track height ≈ 4 dp, 48 dp touch height) flanked by elapsed time on the left and total on the right (`formatTime`, tabular digits). Uses the existing `PlaybackSeekBar` safe-seek rules (disabled and `--:--` when duration is unknown). When `seekBarStyle = Waveform`, the waveform seekbar (NAM-U3) replaces the slider.
4. **Playback controls row**: shuffle toggle, previous, **centre play/pause (64 dp circle, dark semi-transparent container `Color.Black` at ~45 % alpha with a faint white 1 dp ring, white icon 32 dp)**, next, repeat toggle (off/all/one, N-times badge). The arrangement can be replaced by any button preset (section 8); this is the `CENTERED` default.
5. **Secondary action bar**: favourite (heart pop), sleep timer, add to playlist, queue jump (scrolls and focuses the current row in the lower card), equalizer (opens the Equalizer screen).

**Lower section — weight 0.9f–1.0f (use 0.95f)**: `Surface(shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))`, container colour from theme token `queueCardContainer` (light mode `#F9F9FB`; dark mode a dark elevated surface, **not** a bright white slab; see C15 and section 10), on it:
- Header row: "Up Next" (title style) + count badge (rounded chip with the queue size) + an optional "Clear" text button that opens the existing confirm dialog.
- `LazyColumn` (stable keys = media id, `contentPadding` bottom = system inset + 16 dp). Each row (min height 64 dp):
  - Left slot: 48 dp rounded-square thumbnail (corner **8 dp**, scaled by the corner-radius setting) with artwork or the gradient fallback. For the **current** track the slot shows animated amber equalizer bars over a scrim (static icon when paused or reduced motion).
  - Centre: bold title (active → amber token, idle → `onSurface` / charcoal `#1C1B1F` on light), artist in muted `onSurfaceVariant` below; both 1 line with ellipsis.
  - Right: `MoreVert` `IconButton` (≥ 48 dp) opening the row menu (play next, remove from queue, favourite, details).
  - Tap = play this item instantly (`onTrackSelect(index)`); drag handle appears on long-press for reorder (PIX-P2); swipe actions per the Gestures setting.
  - Auto-scroll so the current row is visible with one row of context above it when the track changes, unless the user is scrolling.

### 7.4 State and callbacks (stateless, hoisted)
```kotlin
data class QueueRowUi(val id: String, val title: String, val artist: String, val durationLabel: String,
                      val isCurrent: Boolean, val art: ArtRef?)          // NOT the existing Track class (C11)
data class AtmosphericPlayerUi(
    val title: String, val subtitle: String, val elapsedMs: Long, val durationMs: Long, val canSeek: Boolean,
    val isPlaying: Boolean, val shuffleOn: Boolean, val repeatMode: Int, val repeatRemaining: Int?,
    val isFavorite: Boolean, val sleepActive: Boolean, val activeTab: PlayerTab /* SONG | LYRICS */,
    val queue: List<QueueRowUi>, val currentIndex: Int, val art: ArtRef?, val reducedMotion: Boolean)
// callbacks (all lambdas, no ViewModel inside the composable):
onPlayPauseClick, onPreviousClick, onNextClick, onSeek: (Long) -> Unit, onTrackSelect: (Int) -> Unit,
onToggleFavorite, onToggleShuffle, onCycleRepeat, onOpenTheme, onOpenPicker, onCollapse,
onTabChange: (PlayerTab) -> Unit, onSleep, onAddToPlaylist, onQueueJump, onEqualizer, onMore, onRowMore: (Int) -> Unit,
onMoveQueueItem: (Int, Int) -> Unit, onRemoveQueueItem: (Int) -> Unit
```
The position flow is collected **only** inside the progress composable (no whole-screen recomposition per tick).

### 7.5 Responsiveness of this layout
| Condition | Behaviour |
|---|---|
| Compact portrait, 20:9 | As specified (1.2f / 0.95f) |
| Compact portrait, short (< 640 dp tall) | Hide subtitle first, then shrink the secondary bar to icons only, then reduce the queue card to a peeking header (expand on tap) |
| Landscape or width ≥ 600 dp | Two columns: left = upper section (controls), right = the queue card full height (no rounded top; rounded start corners) |
| Width ≥ 840 dp (docked panel) | Docked Now Playing panel uses this layout in its single-column form |
| Foldable table-top posture | Controls in the bottom half, artwork/lyrics in the top half (section 11) |
| Font scale 2.0 | Titles stay 2 lines max; icon rows wrap by spacing, never clip; the screen scrolls only inside the queue and lyrics |

### 7.6 Layout picker and live switching (while music is playing)
- The layout switcher icon opens a **bottom sheet** with live thumbnails of every `PlayerLayoutMode`; tapping one applies it immediately with a crossfade (300 ms; instant when reduced motion). Playback, queue and position are untouched. Selected mode is persisted and restored on launch.
- Also reachable from Settings → Appearance → Layouts, and from the player overflow menu.
- **Swipe cycling:** a horizontal swipe on the **top bar / drag-handle strip** cycles to the next/previous layout mode (haptic tick, snackbar "Layout: <name>" with Undo). It must not clash with artwork swipe-to-skip: the gesture lives only on that strip, and can be disabled in Settings → Gestures.
- Every layout mode exposes the same minimum: play/pause, previous, next, seek, favourite, queue access. A layout that cannot show something provides it through its overflow menu.

## 8. Live Button Layout Switcher
### 8.1 Concepts
Buttons are grouped: **Transport** = shuffle, previous, play/pause, next, repeat; **Quick actions** = favourite, sleep timer, add to playlist, queue, equalizer; **Extras** (available in some presets) = back 10 s, forward 10 s, speed, lyrics, cast, timer. A **preset** only decides arrangement, sizes, labels and grouping; it never changes what a button does.

### 8.2 Presets (provide all 15; ids are stable strings)
| # | id | Arrangement |
|---|---|---|
| 1 | `centered` | **Default / current.** Transport row centred; quick actions in one row below |
| 2 | `split` | Transport centred under the seek bar; quick actions split left and right of the seek bar line (two on each side, queue centred above) |
| 3 | `minimal` | Only previous, play/pause, next visible; everything else in an overflow chip |
| 4 | `waveform_below` | Waveform seekbar on top, transport directly beneath, quick actions as a slim icon strip under that |
| 5 | `queue_inline` | Quick actions merge into the queue header; transport sits at the bottom of the upper section |
| 6 | `expanded` | Large buttons with text labels, two rows (transport; actions with captions) |
| 7 | `compact` | One row of seven small buttons, secondary ones in overflow below 360 dp |
| 8 | `floating` | A rounded glass pill floating over the artwork/bottom edge containing transport; actions in a second pill |
| 9 | `symmetric` | `favourite · shuffle · previous · PLAY · next · repeat · queue` mirrored around the play button |
| 10 | `asymmetric` | Large play button on the left, previous/next stacked beside it, actions on the right column |
| 11 | `two_row_grid` | 5 + 5 grid, equal cells, icon-only |
| 12 | `radial` | Play at the centre of an arc; previous, next, shuffle, repeat on the arc; actions in a row under |
| 13 | `thumb_zone` | Everything in the bottom third for one-handed use; seek bar above controls |
| 14 | `side_rail` | Vertical rail of buttons on the screen edge (used by default in landscape) |
| 15 | `chips` | Labelled chips (Shuffle, Repeat, Sleep, Queue, EQ) plus a large play button |
Presets 5 and 14 require a layout mode that has a queue header / a side area; if the active layout cannot host a preset, the picker shows it disabled with the reason and the layout falls back to `centered`.

### 8.3 Rules
- Play/pause is always visible and ≥ 56 dp; every button ≥ 48 dp; labels never wrap into overlap (labels hide before icons shrink).
- **Overlap prevention:** the arrangement is a pure function `arrange(preset, widthDp, heightDp, fontScale) -> ButtonPlan`; if the plan does not fit, buttons move to an overflow menu in this fixed priority (kept last → first dropped): play/pause, next, previous, favourite, queue, shuffle, repeat, sleep, add to playlist, equalizer, extras. Unit-test the function at widths 320, 360, 393, 430, 600, 840 dp and font scales 1.0/1.3/2.0 (no two rects intersect).
- Switching animates (`AnimatedContent` + `animateContentSize`, 250 ms; instant under reduced motion) and never interrupts playback.
- Picker: a bottom sheet with live thumbnails (real mini-renders, no images), reachable from the layout icon in the player (a second tab "Buttons" next to "Layouts") and from Settings → Appearance → Layouts. Selecting persists `buttonPresetId` immediately and is restored on launch. Optional (P1): a per-layout override map so each player layout can remember its own preset; a "Reset to default" action.
- P2: **Customise** mode — long-press a button to hide it or drag to reorder within its group; stored as a user preset `custom_1…3`.
- Tests: `ButtonPlanTest` (fit, priority, min sizes), `ButtonPresetRegistryTest` (15 unique ids, default exists, unknown → `centered`), a Compose UI test that switching presets mid-playback keeps `isPlaying`.

## 9. Classic favourites backup v2 (folder `Music/Pulse Player/`)
The Classic backup stays **separate** from the Full backup (file 15 Part C) and keeps the **`pulseplayer.favorites` v1 JSON format exactly** (same keys, same `schemaVersion = 1`, same decoder tolerance, same three-level fuzzy matcher with ambiguity never guessed). v2 changes only **where the files go** and **what is copied**.

### 9.1 Export (Settings → Backup & Transfer → Classic → Export favourites)
1. Count favourites; if zero, message and stop.
2. **Pre-flight dialog** (explicit confirmation, rule C8): "Export N favourites. Copies M audio files (X MB) into Music/Pulse Player. Originals are not changed." Shows free space, with a hard stop and an explanation if space is insufficient (need size + 10 % margin; also refuse if the database volume would drop below 50 MB, file 17 §3).
3. Create the folder: API 29+ insert through `MediaStore` with `RELATIVE_PATH = "Music/Pulse Player/"` (no broad permission needed for files the app creates); API 26–28 needs `WRITE_EXTERNAL_STORAGE` (request it at that moment with a rationale). Folder already exists = reuse it.
4. Copy each favourite's audio file in 64 KB chunks to a unique name (`name (1).ext` on collision; never overwrite), with real progress (file k of M, bytes), **cancel** support (delete the partial file), pause on low battery optional. Sources that are not readable (deleted, SD card removed, permission lost, non-copyable remote/stream item) are skipped and recorded.
5. Write `PulsePlayer_Favorites_<date>.json` (the v1 codec) into the same folder, written to a temp file first, then moved in. The JSON's `displayName` is the **final copied file name** so import matches by file name + size strongly.
6. **Self-check:** re-decode the written JSON, and verify each copied file exists with the expected size. Show the result card: copied n, skipped n (with reasons), failed n, JSON path, a "Retry failed" button, and a "Share folder contents" option.
7. Copied tracks may be indexed by the media scanner as normal music in `Music/Pulse Player/`. Because the originals still exist, the duplicate-hiding rule (file 14 §9) and NAM-L3 must keep the library from showing doubles (the copy of a track the user already has should be hidden as a duplicate, not counted twice).

### 9.2 Import (Settings → Backup & Transfer → Classic → Import favourites)
1. On the new device the user pastes the music files and the JSON into `Music/Pulse Player/`.
2. **Detection:** run a device scan; check MediaStore for audio with `RELATIVE_PATH LIKE 'Music/Pulse Player/%'`. A JSON file placed by the user is **not** visible to the app through MediaStore on Android 11+ (no broad file access), so the app asks **once** for access to that folder via the SAF folder picker with the initial location pre-set to `Music/Pulse Player` (persisted read grant), then auto-detects the newest `PulsePlayer_Favorites_*.json` (or any file that passes the v1 schema sniff) there. On API ≤ 29 it may read directly. The **file picker remains available** as a fallback ("Choose a JSON file").
3. Decode, then run the matcher against the library (`available = 1`); show the existing preview (will add / already favourite / needs choice / not found / invalid / duplicates) with a note if the audio files in the folder are not yet scanned ("N songs from the folder are still being scanned. Scan now?").
4. Apply Merge (default) or Replace (confirm), atomically, idempotent.
5. Result card with the exact counts and a "Not found" list with advice: "Copy those songs into Music/Pulse Player, rescan, import again."

### 9.3 Error matrix (each row: message, recovery action, test)
| Case | Message (plain language) | Recovery |
|---|---|---|
| Permission denied (storage write / folder access) | "Pulse Player needs permission to save into Music/Pulse Player." | Re-request; if permanently denied, Open settings; or "Export JSON only" |
| Storage full / insufficient space | "Not enough space: need X, have Y." | Free-space hint, "Export JSON only", "Choose fewer songs" (select a subset) |
| Folder cannot be created | "Couldn't create the Pulse Player folder." | Retry; "Choose another folder" (SAF) |
| Unreadable/missing source file | "N songs couldn't be copied (file missing or unreadable)." | Show list; "Continue without them" / "Retry" |
| Duplicate names | Silent unique naming, mentioned in the report ("renamed n files") | none needed |
| Partial copy / app killed mid-copy | On next launch: "An export was interrupted." | **Resume** (skip files already complete by size) or **Clean up** partial files; never leave a half-written file named like a finished one (write `.part`, rename on success) |
| Corrupt JSON | "This favourites file is damaged and can't be read." | Pick another file; show line/position hint without paths |
| Wrong/unsupported schema | "This isn't a Pulse Player favourites file (or it is a newer version)." | Offer the Full backup importer if sniff says so |
| Folder empty / no JSON found | "No favourites file found in Music/Pulse Player." | Choose file manually; show the 3-step guide |
| No matching songs | "None of the songs in the file were found on this device." | Show guide + rescan button |
| Cancelled by user | "Export cancelled. N files were copied." | Keep or delete copied files (user's choice) |
Tests: `ClassicBackupV2Test` (JSON byte-compatible with v1 fixtures; unique naming; `.part` handling; space check; resume), instrumented export to a temp MediaStore folder, import from a simulated folder.

## 10. Theme correctness (dark, light, AMOLED, every skin and theme)
1. **Token system:** all colours come from `PulseColors` (extend `MaterialTheme.colorScheme` with `extendedColors`: `queueCardContainer`, `onQueueCard`, `activeAmber`, `activeAmberOnLight`, `scrim`, `glass`, `success`, `warning`, plus state layers: hovered, pressed, focused, disabled). Components read tokens only.
2. **Audit every surface:** screens, dialogs, bottom sheets, snackbars, menus, tooltips, switches, sliders, chips, tab rows, text fields, progress indicators, notifications' small icons, widgets, share cards, overlays, the splash/window background and system bar icon colours (set status/nav bar appearance per mode).
3. **Detection:** add a CI-style grep gate `Color(0x` / `Color.White` / `Color.Black` / hex strings **outside** `ui/theme` and `ui/tokens` and procedural art files must be 0, except an allow-list file `docs/COLOR_ALLOWLIST.md` (intentional accents: procedural backgrounds, the amber highlight, brand gradients on artwork fallbacks). Each exception has a reason.
4. **Contrast tests:** unit tests compute WCAG contrast for the token pairs in every mode (text on surface, on queue card, amber on card, icons on glass) and assert ≥ 4.5:1 for text and ≥ 3:1 for large text/icons.
5. **Screenshot matrix** (Compose screenshot tests or a manual checklist if not available): Home, Songs, Now Playing (Atmospheric + Classic), Queue sheet, Settings, Equalizer, Dialogs, Snackbar × {Light, Dark, AMOLED} × {2 skins} × {2 player themes}.
6. Mode switching is live (no restart), animated 300 ms, and keeps scroll positions.

## 11. Responsiveness
- **Window size classes** (`WindowSizeClass`) plus **`androidx.window`** (`WindowInfoTracker`) for foldable posture (table-top, book) and hinge-aware layout: never place controls across a hinge.
- Support widths 320–1280+ dp, heights down to 280 dp (landscape phone), font scale up to 2.0, display size changes, split-screen and freeform; handle configuration changes without recreating playback UI state (the activity already declares `configChanges`).
- Insets: cutouts (`displayCutout`), status/navigation bars, gesture insets (`systemGestures`), IME; edge-to-edge everywhere; no interactive element inside the 24 dp punch-hole zone or the rounded-corner zone.
- **No-overlap policy:** every screen defines a priority list (primary → secondary). Layout code uses `weight`, `FlowRow`, `BoxWithConstraints` and the `arrange()` functions; when space runs out the lowest-priority item is truncated (text) or moved to an overflow menu (icons). Provide a reusable `AdaptiveRow` that implements this.
- Tablet: rail + content + docked player (exists), two-pane Settings, adaptive grids, two-column queue/lyrics.
- **Test pass** at 320, 360, 393, 430, 600, 840, 1280 dp, landscape, fold half-open; record results in `docs/TESTING.md`; automated `AdaptiveLayoutTest` for the `arrange()` functions.

## 12. Performance targets and work
| Area | Work | Target (measure, then report real numbers or `NOT RUN`) |
|---|---|---|
| Startup | Defer non-critical init (AudioFx, widgets, network, Cast) off the main thread; `Application.onCreate` < 100 ms of work; **Baseline Profile** + startup profile via Macrobenchmark; App Startup library for lazy init | Cold start to first frame ≤ 800 ms on the primary device |
| Scrolling | Stable keys, `derivedStateOf`, no allocations in item scopes, immutable UI models (`@Immutable`/`@Stable`), `contentType` on lazy items, avoid nested scrolling; compose compiler metrics reviewed | No jank frames > 16 ms in Songs with 10 000 tracks (JankStats/Macrobenchmark) |
| Images | Size-bucketed cache (exists), downsampled decode, off-main loading, placeholder crossfade only on first load; blurred backgrounds computed once | Peak art cache ≤ 1/16 max heap |
| Playback stability | Foreground service with correct types; wake lock policy; `onTaskRemoved`; audio focus; process-death recovery (exists); the crossfade/analysis workers have back-pressure | No ANR; no crash in a 2-hour soak test |
| Memory | Leak checks (LeakCanary in debug only), release listeners on detach (file 13 §7), bounded caches, waveform generation one at a time | No growth after 50 track skips |
| DB | Indices for new tables; FTS; batch inserts; `Flow` conflation | Search ≤ 100 ms at 20 000 tracks |
| Build | R8 full mode, resource shrinking, split flavors (`offline` has no network libs) | Report APK sizes per flavor |
| Polish | Shared motion tokens (durations 120/200/300/500 ms, spring specs), consistent button hierarchy (filled = primary, tonal = secondary, text = tertiary), type scale review, spacing grid 4 dp, iconography pass | Visual checklist in `docs/TESTING.md` |
Button colour rule: every button style derives from tokens, has a defined disabled state with ≥ 3:1 contrast, and the same style means the same action class across themes.

## 13. Error-handling framework
```kotlin
sealed interface AppError {
  val userMessage: UiText; val recovery: List<RecoveryAction>; val cause: Throwable?
  data class Permission(val which: PermissionKind, val permanentlyDenied: Boolean, ...) : AppError
  data class Storage(val kind: StorageProblem /* Full, Unavailable, ReadOnly, NotFound */, ...) : AppError
  data class Io(val op: IoOp, ...) : AppError
  data class Parse(val what: String, ...) : AppError            // corrupt JSON/LRC/XML
  data class Playback(val category: ErrorPolicy.Category, ...) : AppError
  data class Network(val kind: NetKind /* Offline, Timeout, Http(code), Blocked, Metered */, ...) : AppError
  data class Auth(...) : AppError ; data class Unsupported(val reason: String, ...) : AppError
}
```
- One `ErrorReporter` turns every failure into an `AppError` and publishes it to a `UiEvents` flow: **snackbar** (transient, with one action), **banner** (persistent, state-based, e.g. permission/partial scan), or **error card/dialog** (blocking, e.g. playback, restore). Messages are plain language, never stack traces or file paths, and each has a **recovery path** (Retry, Open settings, Choose another folder, Skip, Cancel, Undo, Learn more).
- Rules: no `catch (e: Exception) {}` swallowing (grep gate); every `suspend` I/O function returns `Result`/`AppResult` or throws a typed exception mapped at the boundary; `CancellationException` is always rethrown; coroutine exception handlers on app scopes; logs go to local `Log` only (no uploads) with a debug-only in-app log viewer that can be exported manually.
- **Network:** `NetworkGateway` is the only place that creates HTTP clients: timeouts (connect 10 s, read 20 s), retries with exponential back-off for idempotent GETs (max 3), offline detection (no call when offline, queue for later where sensible), metered-network respect (data saver), TLS only (cleartext allowed only for user-added LAN servers after an explicit warning), response-size caps, JSON validated before use, user-visible "Online features are off" state instead of silent failure.
- **Coverage table to complete in the final audit:** Classic export/import (9.3) · Full backup (file 15) · permissions (14) · scan and SAF · tag writes (denied, read-only, unsupported format, interrupted) · playback (file 13 policy) · lyrics fetch/parse · artwork fetch · AI (invalid key, quota, refusal, malformed output) · server sources (auth failure, TLS, timeout) · downloads (space, interrupted, resume) · storage removed · widget/Cast/Auto connection loss.
- Every error type has a unit test that asserts message + recovery actions exist.

## 14. First-launch permission onboarding
A short, skippable-per-step flow shown on the **first launch only** (re-runnable from Settings → Permissions). Each step: icon, one-sentence reason, **Allow** and **Not now**. Denial never blocks the app; features degrade with an explanation and a fix button.
| Step | Permission | Needed for | When denied |
|---|---|---|---|
| 1 Music access | `READ_MEDIA_AUDIO` (API 33+) / `READ_EXTERNAL_STORAGE` (≤ 32) | Finding songs | Limited mode: folder picker only (SAF); banner on Home |
| 2 Notifications | `POST_NOTIFICATIONS` (API 33+) | Playback controls, downloads | Playback still works; lock-screen controls may be reduced; banner |
| 3 Nearby Bluetooth | `BLUETOOTH_CONNECT` (API 31+) | Showing connected Bluetooth output names, headset events | Output list shows generic names |
| 4 Battery | `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` flow | Reliable background playback on aggressive OEM skins (Infinix XOS) | Show an OEM-specific hint page; **optional**, never forced, and note the store-policy sensitivity |
| 5 Network (informational) | `INTERNET` is a normal permission (no prompt) in the `full` flavor | Online features | The "Online features" master switch stays OFF until the user turns it on; the screen explains each service |
Requested **later, in context**, not at first launch: `WRITE_SETTINGS` (ringtone, pending action file 13 §9), `NEARBY_WIFI_DEVICES` (API 33, Listening Party/LAN sync), `WRITE_EXTERNAL_STORAGE` (API ≤ 28, Classic export), SAF folder grants, `createWriteRequest` (tag edits). **Never requested:** `RECORD_AUDIO`, `MANAGE_EXTERNAL_STORAGE`, location, contacts, phone state. The Permissions settings page lists every permission with status and a fix button; a `PermissionMonitor` (file 14 §8) is extended with Notifications, Bluetooth, Battery states. Tests: `PermissionStateTest` additions, UI test for denied/permanently denied paths.

## 15. Detailed specifications for the larger new subsystems
### 15.1 Library intelligence (Phase 12)
- **Separators engine:** `ArtistParser.split(raw, delimiters, exceptions, extractFeat): List<String>` with unit tests (`"A feat. B"`, `"A & B"`, `"AC/DC"`, `"Tyler, The Creator"`, `"A x B"`); genre splitting similarly. Results cached in a join table so queries stay fast; re-run on settings change in a background worker with progress.
- **Metadata screens:** Moods, Tags, Ratings, Languages are managed in the track details sheet and bulk via multi-select; pages list items grouped with counts; language can be auto-suggested from lyrics/tags but never auto-applied silently.
- **Search:** FTS index columns: title, artist, album, album artist, genre, file name, lyrics; query parser supports `artist:`, `album:`, `genre:`, `tag:`, `mood:`, `rating:>=4`, `year:`, `smart:`; typo tolerance fallback; results grouped (Songs, Albums, Artists, Playlists, Lyrics matches with a snippet).
- **Smart playlists:** rules JSON schema versioned; evaluation off the main thread; preview count while editing; export/import as JSON; never writes to files.
- **Daily Mix (local):** deterministic seeded generator: candidate pool = favourites + frequently played + similar genre/artist neighbours + a recency-decayed rediscovery slice; avoid repeating the same artist more than twice in a row; refresh at local midnight; shown on For you.
- **Stats/Your Year:** all read from `play_events` with `counted = 1`; charts: line (time per day), bar (top lists), heat-map (hour × weekday), donut (genres), streak counter; year selector; share card bitmap (extends file 08).
- **Queues:** page lists saved queues with name, count, date; "Save current queue", "Load" (replace or append), "Rename", "Delete"; loading never starts playback until the user taps Play (restored paused).
- **Gestures/selection:** one `SelectionState` hoisted at app level for global selection.

### 15.2 Lyrics (Phases 12 and 15)
Parsers: LRC (multi-stamp), enhanced LRC (word stamps), TTML (word spans), plain. Display modes: line-synced, word-synced highlight (karaoke fill), plain scroll. Editor: shows lines, play/pause, "stamp this line" at the current position, nudge ±100 ms, global offset, save as database entry or sidecar `.lrc`. LRCLIB fetch: exact-match request (title, artist, album, duration ±2 s), else search; prefers synced; stores result with `source = LRCLIB`; the user can delete or replace; fetch is manual unless auto-fetch is on (Wi-Fi only default). Limits: file size ≤ 256 KB (file 16 §6); NUL bytes rejected.

### 15.3 Sound engine (Phase 13)
- Insert a custom `AudioProcessor` chain into the ExoPlayer `DefaultAudioSink` in this fixed order: **gain (ReplayGain + per-track) → parametric EQ → effects (bass shelf, surround/crossfeed, 8D, vocal remover, mono) → level tap (for visualizers)**. Each processor is bypassed when its setting is off (zero cost). Processing uses float samples and a soft limiter at the end to avoid clipping.
- Platform `AudioFx` stays for devices where it works; the settings page shows which engine is active and prevents double-EQ.
- **Audio path viewer** reads `onAudioSinkInputFormatChanged`, `onAudioDecoderInitialized`, processor states and the routed `AudioDeviceInfo`.
- Bit-perfect/exclusive/USB (NAM-S2): capability-gated, and when active it bypasses the whole processor chain and says so.
- Per-track profile applied at `onMediaItemTransition` through `PlaybackEngine`; restored to global values when leaving the track.

### 15.4 Smart crossfade and beat matching (experimental, default OFF)
- `CrossfadeEngine` inside `PlaybackEngine` owns a second ExoPlayer (same audio attributes, **no** extra MediaSession, no extra notification). It pre-buffers the next item; at `duration − fadeMs` it starts the incoming player and ramps both volumes **through `AudioTransitionController`** (equal-power curve). On completion the engines swap roles; the `FadingPlayer` facade and `QueueModel` see one continuous queue.
- Guards: not used for repeat-one, for tracks shorter than 2 × fade, when the next item is unavailable, when skip-silence is on (conflict note), in bit-perfect mode, or when the user manually skips (manual skip uses the short `TrackChange` cut from file 13).
- **Beat matching (P2):** background BPM analysis (onset autocorrelation on a downsampled mono buffer) cached in `tracks.bpm`; when both BPMs are known and within ±6 %, the incoming track's playback speed is nudged (≤ ±4 %) during the overlap and returned to 1.0 afterwards; downbeat alignment is best-effort. If analysis is missing or confidence is low, plain equal-power crossfade is used. Label: "Experimental — results vary".
- `SessionTracker` treats the overlap as a track change at the midpoint; `SleepTimerEngine` and `ErrorPolicy` are tested against the engine swap (`CrossfadeEngineTest`, `ControllerReconnectTest` additions).

### 15.5 Android Auto, Cast, widgets (Phase 15)
Convert `MusicPlaybackService` to `MediaLibraryService` while preserving the current session behaviour (all existing service tests must pass unchanged). Browse tree nodes: Recent, Favourites, Playlists, Albums, Artists, Folders, Most played. Voice search maps to the FTS search. Cast: local HTTP server (token in the path, bound to the Wi-Fi interface, stopped when casting ends), `CastPlayer` swap-in/out keeping the queue; hide the button with no Play services. Widgets: three styles selectable in a configuration activity; each uses the existing `WidgetStateStore`.

### 15.6 AI features (Phase 15, `full` flavor, opt-in)
Providers behind `AiProvider`: Gemini, OpenAI-compatible (covers DeepSeek and others with a base URL), and a "Custom endpoint". User pastes their own key; it is stored encrypted and never logged or included in backups. **Consent screen** before the first call shows exactly what will be sent (a preview of the digest: titles, artists, albums, genres, play counts, a prompt) and what is **never** sent (file paths, audio, favourites file, lyrics, personal data); the user can reduce the digest size. The response must be JSON; validate against a schema; map suggested tracks to the library by the matcher tiers; unmatched suggestions are shown as "not in your library" (never invented as playable). Errors: invalid key, quota/rate limit, refusal, malformed output, timeout — each with retry/back-off and a clear message. Daily Mix works fully without AI.

### 15.7 Online artwork (Deezer) and LRCLIB
Both go through `NetworkGateway`; both honour the master switch, the per-service switch, data saver and a polite request rate (≤ 1 req/s, local queue). Results are cached (Room + files + in-memory LRU) with TTLs; failures cache a negative result for 24 h; the UI never blocks on the network and always has the fallback artwork.

### 15.8 History import (Phase 15)
Importers parse files streamed through `JsonReader`/CSV with the same limits as file 15 §2.2 (size, entries, strings, timestamps), map to library tracks using the tier rules, produce a preview (matched / ambiguous / unmatched / duplicates), then commit in **one transaction** through `HistoryWriter` with `origin` set; importing the same file twice changes nothing (dedupe key trackId + playedAtMs). Rows from imports show a small origin badge in the history editor.

### 15.9 Sources and streaming legal gate (Phase 16)
Before building `YouTubeSource`, SponsorBlock, Return YouTube Dislike, video downloads, sign-in: write `docs/DECISIONS.md` with (a) terms-of-service and store-policy risk, (b) which extractor library and its licence, (c) the owner's explicit go/no-go. If no-go, mark NAM-V1/V2/V4–V10 `Unsupported(reason: "Not included in this build")`, visible in Settings. Subsonic, Jellyfin, WebDAV and SMB are ordinary self-hosted protocols and proceed normally.

## 16. Documentation deliverables
**README.md** (detailed and visually pleasing): banner (SVG/PNG made from the existing icon assets or procedural), badges (platform, Kotlin, Compose, minSdk from the version catalog, licence, build status placeholder), screenshot **placeholders** with file names in `docs/screenshots/` (never fake screenshots), feature list by section mirroring Settings IA, "Two flavors" table (offline vs full, permissions per flavor), requirements, build/run commands (`./gradlew assembleOfflineDebug assembleFullDebug testDebugUnitTest lintDebug`), architecture diagram (Mermaid) with the four customisation axes, module/package map, data flow (service ⇄ controller ⇄ UI), backup guide (Classic folder flow and Full backup), privacy statement (what is stored, what is sent and only when the user enables it), troubleshooting (permissions, OEM battery settings, SD card), contribution guide (branching, commit style, task size, tests, no emojis in UI, token rules, how to add a theme/layout/button preset), roadmap (the Gated and Unsupported items), licences and acknowledgements (Namida, PixelPlayer, libraries) linked to `THIRD_PARTY_NOTICES.md`. Also update `CHANGELOG.md`, `docs/PRIVACY.md`, `docs/TESTING.md`, `docs/spec/SPEC_INDEX.md`, and file 12 §4's theme table.
**Git:** commit per task with a message in the form `phase<N>: <summary>`; push **only** when `assembleDebug` and unit tests pass for that task; never push secrets (`local.properties`, keys, the user's `.wav` sample, music files).

## 17. Phases, Spec Index and prompts
### 17.1 Append to `docs/spec/SPEC_INDEX.md`
| Phase | Goal | Required files |
|---|---|---|
| 9 | Merge groundwork: inventory, licences, capability registry, schema v4, settings IA, error framework | 18 §1–§6, §13; 03, 14 §1–§2 |
| 10 | Tokens, theme fixes, colour sources, corner radius, navigation styles, micro-interactions | 18 §5.1, §10, §6.6 (PIX-U); 02, 05 |
| 11 | Player layouts, Atmospheric Queue, live button presets, waveform seekbar, visualizer, particles | 18 §7, §8, §6.2; 05, 06, 16 |
| 12 | Library intelligence, new pages, tag editor, search, smart playlists, stats, queues, gestures | 18 §6.1, §6.4, §15.1–§15.2; 14, 15 |
| 13 | Sound engine and transitions | 18 §6.3, §15.3, §15.4; 04, 13 |
| 14 | Classic backup v2, permission onboarding, error-handling sweep | 18 §9, §13, §14; 07, 14 §8, 15 Part C |
| 15 | Online layer: LRCLIB, Deezer, AI, history imports, Android Auto, Cast, widgets | 18 §15.5–§15.8, §3 C1; 08, 13 |
| 16 | Sources, downloads, LAN party/sync, gated streaming | 18 §5.4, §6.5, §15.9; 14 |
| 17 | Responsiveness, performance, README, final traceability audit | 18 §11, §12, §16, §18; 17 |
Existing phase files keep working; fix-pack files still win over base files.

### 17.2 Rules that apply to every prompt below
Start with: *"Attach `SPEC_INDEX.md`, file 18 and the files listed. Read only those. Fix-pack files win over base files; file 18 wins only in section 3. Change at most 6 files. Do not remove or weaken any existing feature, skin, theme, layout, setting or test. Finish with the CHANGED FILES manifest (file 12 §5) and real build/test results; write NOT RUN for anything you did not run. Update `brain.md`."* If a task needs more than 6 files, split it and say so.

### 17.3 Prompts (continue numbering after prompt 29)
**PROMPT 30 — Phase 9a: audit + inventory.** "Audit the project against file 18 §2 (PRESENT/PARTIAL/MISSING per row). Read `New Player Concept/Namida` and `New Player Concept/PixelPlayer` and their upstream repos; determine PixelPlayer's language and structure. Produce `docs/spec/PARITY_INVENTORY.md` (every feature, screen, skin, theme, layout; start from §6 IDs, add what you find), `docs/THIRD_PARTY_NOTICES.md` (licences) and `docs/DECISIONS.md` (legal gate, flavor decision). No code changes. Stop and report the licence findings."
**PROMPT 31 — Phase 9b: foundations.** "Add product flavors `offline`/`full`, `Capabilities`, `AppError` + `ErrorReporter` + `UiEvents`, the settings IA skeleton (every section from §5.6 with rows for every catalogue ID, showing `Unsupported`/`Planned`-free truthful states: only implemented rows are enabled), and `MIGRATION_3_4` with exported schema, pre-migration copy and `Migration3To4Test`. Update `ThemeRegistryTest`/add `PlayerLayoutModeTest` skeleton."
**PROMPT 32 — Phase 10a: tokens and theme audit.** "Introduce `PulseColors` extended tokens, replace hardcoded colours across all screens/dialogs/sheets/snackbars/system bars, add the colour allow-list, contrast unit tests and the grep gate. Report every file touched."
**PROMPT 33 — Phase 10b: colour sources, shape, navigation, micro-interactions.** "Implement Material You, artwork-driven and seed-colour sources, time-of-day schedule, `LocalCornerScale`, navigation styles (top tabs default, bottom, floating, rail), `PulseClickable`. Add the new skins (Forest Mist, Material You tonal, Artwork-driven) to the Skin picker. Nothing existing removed."
**PROMPT 34 — Phase 11a: Atmospheric Queue.** "Implement §7.1–§7.5: `PlayerLayoutMode.ATMOSPHERIC_QUEUE`, `AtmosphericBackground`, `AtmosphericQueuePlayerLayout`, `QueueRowUi` mapping, previews (preview-only sample data 'Hurts So Good (Lyric…)' / 'AstridSofficialVEVO'), wiring into `NowPlayingScreen` from the real queue, `PlayerLayoutModeTest`. Verify the light/dark queue-card tokens and the amber contrast test."
**PROMPT 35 — Phase 11b: layout picker and live button presets.** "Implement §7.6 and §8: layout picker sheet with live thumbnails, swipe-strip cycling, `ButtonPlan`/`arrange()` for all 15 presets, the Buttons tab in the picker, persistence of `playerLayoutMode`/`buttonPresetId`, tests (`ButtonPlanTest`, `ButtonPresetRegistryTest`, UI test)."
**PROMPT 36 — Phase 11c: waveform, visualizer, particles, party mode.** "Implement the audio level tap, waveform generation/cache and seekbar style, visualizer styles, particles and party-mode glow; all off under reduced motion/battery saver. No `RECORD_AUDIO`."
**PROMPT 37 — Phase 11d: Namida/PixelPlayer designs.** "From the inventory, re-create each additional skin, player theme and layout mode in code and register them in the pickers (one batch of at most 6 files per reply; continue until the inventory design rows are all done). Keep ids stable; update registry tests."
**PROMPT 38 — Phase 12a: metadata and library.** "Schema-backed separators, feat. extraction, duplicates, filters, sorting options, folder images/`.info.txt`, ratings/moods/tags/languages, and the new pages (Genres, Queues, Most Played, Moods, Tags, Ratings, Languages, Stats) with page order/visibility settings."
**PROMPT 39 — Phase 12b: tag editor.** "TagLib-based reader and editor with `createWriteRequest`, confirmation, optional backup copy, post-write rescan, error cases and tests."
**PROMPT 40 — Phase 12c: search, smart playlists, Daily Mix, stats, queues, history, gestures.** "Implement NAM-Y2, Y5–Y12, Y14–Y17 and PIX-D2/P2 per §15.1 (split across two replies if needed)."
**PROMPT 41 — Phase 13: sound engine.** "Implement the processor chain, parametric EQ + AutoEQ import, effects, ReplayGain, per-track profiles, speed/pitch, output device/mono, audio path viewer, sleep timer by tracks and pausing scenarios, `CrossfadeEngine` (experimental), capability-gated bit-perfect/float. Grep gate: no `.volume =` outside `AudioTransitionController`."
**PROMPT 42 — Phase 14a: Classic backup v2.** "Implement §9 end to end with the error matrix and tests. Keep the v1 JSON byte-compatible."
**PROMPT 43 — Phase 14b: permissions and error sweep.** "Implement §14 onboarding and the Permissions page; convert every I/O boundary to `AppError`; add the coverage table results and tests."
**PROMPT 44 — Phase 15: online layer.** "Implement `NetworkGateway`, LRCLIB, Deezer, AI providers with the consent screen, history importers, Android Auto (`MediaLibraryService`), Cast, extra widgets (split across replies)."
**PROMPT 45 — Phase 16: sources and LAN.** "Implement `MusicSource` for Subsonic, Jellyfin, WebDAV, SMB, download manager and cache, LAN sync and Listening Party; implement streaming features only if `DECISIONS.md` says go."
**PROMPT 46 — Phase 17: final audit and delivery.** "Run: `./gradlew assembleOfflineDebug assembleFullDebug testDebugUnitTest lintDebug` (and `connectedDebugAndroidTest` if a device exists). **All 137+ existing tests must still pass**; report exact counts. Run the grep gates (section 18.2). Fill the traceability table (section 18.1) with real statuses. Write README and docs (§16). Only if build and tests pass: commit and push. Fix nothing silently; list every failure."

## 18. Final traceability audit and completion report
### 18.1 Traceability table (the final reply of prompt 46)
One row per ID in section 6 plus every ID added by the inventory: `ID | Status (IMPLEMENTED / GATED(reason) / UNSUPPORTED(reason shown in Settings) / NOT DONE) | Where in Settings | Test | Evidence`. **Hidden is not a valid status.** Also one row per skin, player theme, layout mode and button preset (counts must match the registries).

### 18.2 Grep and gate list (extends file 17 §5)
`TODO-TEMP`=0 · `.volume =` outside `AudioTransitionController`=0 · `available = 0` writers=1 · `openDatabase` on archive=0 · `fallbackToDestructiveMigration`=0 · Media3 `shuffleModeEnabled = true`=0 · `play_events` inserts outside `SessionTracker`/`HistoryWriter`=0 · `RECORD_AUDIO` / `MANAGE_EXTERNAL_STORAGE` in manifests=0 · `INTERNET` in `offline` flavor manifest=0 · network classes referenced outside `net` package=0 · hardcoded colours outside allow-list=0 · empty `catch` blocks=0 · word "compatible" in UI strings=0 · emojis in strings=0.

### 18.3 Completion report v2 (exact shape)
```
COMPLETION REPORT v2
Builds:       assembleOfflineDebug → PASS|FAIL   assembleFullDebug → PASS|FAIL   (APK sizes)
Lint:         PASS|FAIL|NOT RUN
Unit tests:   X passed / Y failed / Z skipped   (existing 137 all present: yes|no)
Device tests: X / Y / Z | NOT RUN
Manual checklist: file 17 §2 items 1–11 + new: Atmospheric layout, layout cycling, 15 button presets, Classic v2 export/import, onboarding, light/dark/AMOLED sweep, widths 320–1280, fold/landscape
Grep gates:   (section 18.2, numbers)
Counts:       skins N, player themes N, layout modes N, button presets 15
Traceability: IMPLEMENTED a / GATED b / UNSUPPORTED c / NOT DONE d  (list b, c, d)
Performance:  cold start, jank, search latency, memory (real numbers or NOT RUN)
Licences:     reference-app licence decision recorded: yes|no
Not verified: Android Auto head unit, Cast receiver, bit-perfect on real DAC, acceptance of exported Full backup by the other app, anything marked NOT RUN
```
Rules: numbers come from real output; an invented PASS fails the audit; every `NOT DONE` carries a reason and a next step.

## 19. Acceptance checklist (all must be true, or listed as Gated/Unsupported with a reason)
- [ ] Every Namida feature (6.1–6.5) and every PixelPlayer feature (6.6) has a Settings entry and a truthful status.
- [ ] Every Namida/PixelPlayer design appears in the Skins, Themes, Layouts or Player-theme picker; no existing item was removed.
- [ ] `ATMOSPHERIC_QUEUE` exists, is the first and fallback layout mode, and its tests pass.
- [ ] Layout picker and 15 button presets work during playback, animate, persist and restore.
- [ ] Classic backup v2 writes JSON + audio copies to `Music/Pulse Player/`, imports with fuzzy matching, and handles every row of 9.3.
- [ ] No colour leaks in any mode (grep gate + contrast tests + screenshot matrix).
- [ ] No overlap at 320–1280 dp, landscape, fold, font scale 2.0.
- [ ] Performance work done with real measurements or `NOT RUN`.
- [ ] Error framework covers every I/O and network boundary with recovery actions.
- [ ] First-launch onboarding requests only what is needed, with rationale and graceful denial.
- [ ] README and docs written; licences recorded.
- [ ] Both flavors build; `offline` has no INTERNET permission.
- [ ] All pre-existing tests still pass; new tests added; commit and push only after green.
