# Pulse Player — Ultimate Audio Experience, Audiophile Engine & Library Elevation

A comprehensive architectural and UX implementation plan to advance Pulse Player into an elite, audiophile-grade music station featuring crossfade and gapless playback, real-time audio waveform visualizer seekbars, an interactive synchronized LRC lyrics studio, built-in ID3 tag and artwork editor, rule-based smart playlists, and refined library swipe gesture ergonomics.

### User Review & Critical Decisions

> [!IMPORTANT]
> The user confirmed a unified elevation across **all major areas**: Audio Engine & Audiophile playback, Interactive Visualizers & Waveforms, Lyrics & Tag Studio, Smart Playlists & Auto-DJ, and Library swipe/folder UX ergonomics.

- **Crossfade & Gapless Engine**: Media3 `AudioTransitionController` will be upgraded with continuous volume crossfade curves (configurable 0s to 12s) between tracks, ensuring seamless gapless transitions for live albums and classical movements.
- **Waveform Visualizer Seekbar**: Replace flat progress bars across player layouts with a dynamic interactive audio waveform visualizer with peak amplitudes, scrub previews, and fluid touch scrubbing.
- **Synchronized LRC Lyrics Studio**: Built-in time-synced lyrics viewer with highlighted active line, tap-to-seek, and an inline timestamp editor for offline lyrics adjustment.
- **ID3 Tag & Cover Art Studio**: Built-in metadata editor to inspect and modify track title, artist, album, genre, year, track number, and pick custom cover art with Room database persistence.
- **Dynamic Smart Playlists**: Rule-driven automated playlists ("Top 25 Most Played", "Forgotten Favorites", "90s Hits", "Recently Added", "Workout Fast Tempo") that update reactively as listening history evolves.
- **Library Swipe Ergonomics & Tab Customization**: Spring-physics swipe-to-queue and swipe-to-favorite actions on list items, an expandable hierarchical folder tree browser, and customizable home tab ordering in Settings.

---

### 1. Overview & Core Concept

- **What It Delivers**:
  1. **Audiophile Playback**: Smooth parametric crossfading (1s–12s), zero-gap track transitions, and hardware DSP precision.
  2. **Visual & Tactile Immersion**: Real-time waveform amplitude seekbars on player surfaces with tactile haptic scrub feedback and beat visualizers.
  3. **LRC Lyrics & Metadata Freedom**: Read and edit synchronized `.lrc` files directly; update metadata and embed custom album art on-device without desktop software.
  4. **Smart Automation**: Auto-DJ smart playlist generation and intelligent queue mixing.
  5. **Ergonomic Library Controls**: Effortless swipe gestures (swipe right to queue next, swipe left to favorite) and customizable tab layouts.
- **Target Audience**: Music lovers, audiophiles, and power users who cherish their offline local library and demand deep customization, studio-quality playback, and clean Material 3 design.

---

### 2. User Experience & Visual Design

#### Key User Flows & Visual Identity
1. **Interactive Waveform Player**:
   - Now Playing surfaces showcase an organic waveform seekbar composed of 64–100 amplitude bars reflecting the track's dynamic density.
   - As audio plays, bars transition from glowing unplayed colors (`outlineVariant`) to radiant active accents (`primary` / `Pulse.Cyan`), accompanied by a smooth time-stamp bubble on touch drag.
2. **Synchronized LRC Lyrics Experience**:
   - Tapping the Lyrics tab reveals karaoke-style synced lyrics that auto-scroll with spring physics.
   - Active line renders with enhanced typographic scale (bold headline, glow accent) while past/future lines remain elegantly dimmed.
   - Tapping any lyric line instantly seeks playback to that exact second. A bottom edit sheet allows nudging timestamps by ±0.5s or pasting raw LRC text.
3. **Track Tag & Artwork Editor**:
   - Accessible via the track context menu (`Edit Tags`).
   - Clean Material 3 modal sheet to edit track title, artist, album, genre, year, and track number.
   - Includes a cover art card with one-tap Photo Picker integration (`PickVisualMedia`) to attach custom album covers.
4. **Smart Playlists Hub**:
   - A dedicated tab and card collection featuring live-updating playlists with animated badges:
     - ⚡ *Top 25 Heavy Rotation* (sorted by play count)
     - ⏳ *Forgotten Favorites* (favorited songs unplayed in >30 days)
     - 🆕 *Fresh Finds* (added in the last 14 days)
     - 🚀 *Quick Mix / Auto-DJ* (one-tap instant mix matching current song's genre and tempo)
5. **Swipe Gestures & Library Ergonomics**:
   - Swiping right on any song reveals a vibrant primary pill: "Play Next in Queue" with subtle haptic vibration.
   - Swiping left reveals an amber/rose pill: "Toggle Favorite".
   - Settings → Appearance now offers a "Customize Home Tabs" order editor to toggle and reorder library tabs.

---

### 3. Key Product Decisions & Trade-Offs

- **Decision 1: Parametric Audio Crossfading vs. Abrupt Track Switches**
  - *Chosen Approach*: Implement a dual-stage volume ramp curve inside `AudioTransitionController` that fades out the outgoing track while cueing and ramping in the incoming track when nearing track completion.
  - *Why*: Eliminates awkward silence and audio pops between tracks, providing a radio/club-style continuous flow.
  - *Fallback*: Users can adjust crossfade duration from 0s (instant gapless) to 12s in Settings → Playback.
- **Decision 2: Waveform Peak Generation & Storage**
  - *Chosen Approach*: Lightweight 64-point amplitude fingerprint generation cached in the existing Room `waveforms` table (`WaveformEntity`).
  - *Why*: Instant seekbar rendering without re-decoding audio on every playback start, preserving battery life.
- **Decision 3: Zero-Permission Photo Picker for Tag Artwork**
  - *Chosen Approach*: Android system Photo Picker (`ActivityResultContracts.PickVisualMedia`) storing cached art in app-private storage, referenced via Room `TrackMetaEntity`.
  - *Why*: Fully compliant with Google Play storage policies without requesting broad filesystem permissions.

---

### 4. Technical Architecture & Data Strategy

```
┌────────────────────────────────────────────────────────────────────────┐
│                          Pulse Player Engine                           │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │
        ┌───────────────────────────┼───────────────────────────┐
        ▼                           ▼                           ▼
┌───────────────┐           ┌───────────────┐           ┌───────────────┐
│PlaybackEngine │           │   PulseDb     │           │  SettingsStore│
│- Crossfader   │           │- SmartPlaylists│          │- CrossfadeSecs│
│- WaveformStore│           │- TrackMetaDao │           │- TabOrderList │
│- FadingPlayer │           │- WaveformDao  │           │- SwipeActions │
└───────┬───────┘           └───────┬───────┘           └───────┬───────┘
        │                           │                           │
        └───────────────────────────┼───────────────────────────┘
                                    │
                                    ▼
┌───────────────────────────────────────────────────────────────────────┐
│                       ViewModel & Domain Layer                        │
│   (MainViewModel, LibraryScanner, SmartPlaylistEngine, LyricsEngine)  │
└───────────────────────────────────┬───────────────────────────────────┘
                                    │
        ┌───────────────────────────┼───────────────────────────┐
        ▼                           ▼                           ▼
┌───────────────────┐       ┌───────────────────┐       ┌───────────────────┐
│ NowPlayingScreen  │       │ Synchronized LRC  │       │ SmartPlaylists &  │
│ - WaveformSeekBar │       │ - Synced Lyrics   │       │ Tag Editor Sheet  │
│ - Crossfade status│       │ - Time-tag editor │       │ - Photo Picker art│
└───────────────────┘       └───────────────────┘       └───────────────────┘
```

#### Staged Implementation Slices

1. **Slice 1: Crossfade Engine & Waveform Visualizer Seekbar**:
   - Extend `AudioTransitionController` with configurable crossfading (0s–12s) and gapless playback.
   - Build `WaveformSeekBar.kt` with live amplitude bar rendering, drag scrub preview, and contrast-hardened Dark/Light mode tokens.
2. **Slice 2: Synchronized LRC Lyrics Studio**:
   - Enhance `LyricsEngine.kt` to parse and interpolate synchronized `.lrc` timestamps.
   - Build active-line highlighting with auto-scroll, tap-to-seek, and an inline timestamp offset editor.
3. **Slice 3: ID3 Tag Editor & Artwork Picker**:
   - Create `TagEditorSheet.kt` to update track titles, artists, albums, genres, and track numbers in Room.
   - Integrate `PickVisualMedia` contract to attach custom cover art smoothly.
4. **Slice 4: Dynamic Smart Playlists & Auto-DJ Quick Mix**:
   - Implement `SmartPlaylistEngine.kt` querying Room stats for "Top 25", "Forgotten Favorites", "Fresh Finds", and "Genre Quick Mix".
   - Expose smart playlist cards in `PlaylistsScreen` with dedicated icons and live counts.
5. **Slice 5: Library Swipe Gestures & Tab Customization**:
   - Implement spring-animated swipe-to-queue and swipe-to-favorite in `TrackRow.kt`.
   - Add Home tab visibility and order settings in `SettingsScreen.kt`.
6. **Slice 6: Verification & Compilation**:
   - Run `compile_applet` and full unit test suites (`testOfflineDebugUnitTest`, `testFullDebugUnitTest`).
