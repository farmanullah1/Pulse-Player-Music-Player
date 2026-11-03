# Architecture and Product Decisions — Pulse Player
> Created: Phase M0  
> Update this file whenever a significant technical or product-policy decision is made.

---

## D01 — Product Flavors (C1)
**Decision:** The required end state remains two Gradle product flavors:
- `offline` — identical to the current build; **no INTERNET permission** ever added; all network code excluded at compile time via build flags.
- `full` — adds `INTERNET` and `ACCESS_NETWORK_STATE`; all online features behind `NetworkGateway` master switch (default OFF) and per-service switches.

**Rationale:** The original spec required no INTERNET permission. The merge spec adds optional online features. Both requirements are satisfied by separate flavors.

**Current status:** FOUNDATION IMPLEMENTED. `offline` resolves to an explicit unsupported gateway and contains no network client dependency or network permission. `full` resolves to the OkHttp-backed gateway; requests remain blocked until the Online features switch is enabled and are blocked again by Offline-only mode. Per-service switches and secure API-key storage remain later work.

---

## D02 — No Namida/PixelPlayer Code Copying
**Decision:** All features inspired by Namida and PixelPlayer are independently re-implemented in Kotlin/Compose. No source files, icon assets, fonts, or screenshots from either reference project are copied into Pulse Player.

**Rationale:** The inspected Namida repository uses a restrictive EULA and PixelPlayer uses a proprietary personal/non-commercial license with an older MIT contribution notice. Neither is treated as a code dependency. All parity features are clean-room implementations from observed behavior and design.

---

## D03 — Schema Version 4 Migration
**Decision:** A single `MIGRATION_3_4` migration will add all new columns and tables required by the merge spec. All new columns use `DEFAULT` values so existing rows are preserved. A pre-migration backup copy of `pulse.db` is made before the migration runs (existing behavior extended).

**Tables added:** `track_tags`, `track_moods`, `track_meta`, `track_audio_profile`, `bookmarks`, `chapters`, `saved_queues`, `saved_queue_items`, `smart_playlists`, `artist_images`, `sources`, `cache_entries`, `waveforms`.

**Columns added to `tracks`:** `genre`, `genresRaw`, `albumArtistNorm`, `bitrate`, `sampleRate`, `bitDepth`, `codec`, `replayGainTrackDb`, `replayGainAlbumDb`, `bpm`, `language`, `hasEmbeddedLyrics`.

**Columns added to `play_events`:** `origin TEXT NOT NULL DEFAULT 'LIVE'`.

**Columns added to `lyrics`:** `source TEXT NOT NULL DEFAULT 'IMPORT'`, `format TEXT NOT NULL DEFAULT 'LRC'`.

---

## D04 — Five Customization Axes (C10, Annex A §5.1)
**Decision:** Five independent customization axes, each with its own DataStore key and picker:
1. **Theme mode / color source** — `themeMode`, `colorSource`, `seedColor`
2. **Skin** — `skinId`, `wallpaper`
3. **Player theme** — `playerThemeId`
4. **Player layout mode** — `playerLayoutMode` (default fallback: `ATMOSPHERIC_QUEUE`)
5. **Button layout preset** — `buttonPresetId`

Player theme registry and player layout registry are kept separate. `ThemeRegistryTest` will be updated to the new canonical list when themes are added.

---

## D05 — Single Playback Owner (C6)
**Decision:** The crossfade engine's second ExoPlayer (Phase M8/AUD-12) is private to `CrossfadeEngine` and never exposed to the UI. There is one `MediaSession` facade (`FadingPlayer`) and one owner (`PlaybackEngine`). No layout or theme creates a second player or second queue.

---

## D06 — History Writer Gate (C5)
**Decision:** Live listening is written only by `SessionTracker`. All history imports and manual edits go through a new `HistoryWriter` class. Both are the only callers of `play_events` inserts. The grep gate is updated to: "only `SessionTracker` and `HistoryWriter` call `play_events` inserts."

A new `origin` column (`LIVE`, `IMPORT_LASTFM`, `IMPORT_SPOTIFY`, `IMPORT_LISTENBRAINZ`, `IMPORT_YOUTUBE`, `MANUAL`) distinguishes record sources.

---

## D07 — No MANAGE_EXTERNAL_STORAGE (C9)
**Decision:** `MANAGE_EXTERNAL_STORAGE` is forbidden. Tag writes use `MediaStore.createWriteRequest`. File copies use `MusicCopier` with SAF persisted trees. No broad storage management permission is ever requested.

---

## D08 — AudioProcessor Gain Stage for ReplayGain (C4)
**Decision:** ReplayGain, per-track gain, and loudness normalization are applied by an `AudioProcessor` gain stage, never by writing `player.volume`. `AudioTransitionController` remains the sole writer of `player.volume`. The grep gate is updated accordingly.

---

## D09 — Visualizer via AudioProcessor PCM Tap (C13)
**Decision:** The visualizer reads PCM data by tapping the `AudioProcessor` pipeline (peak + small FFT), publishing a throttled `AudioLevels` flow. `RECORD_AUDIO` permission is never requested for the visualizer.

---

## D10 — YouTube and SponsorBlock
**Decision:** YouTube scraping, undocumented endpoints, and SponsorBlock features are **out of scope** unless the owner explicitly approves after reviewing terms-of-service and app-store policy implications. They will be listed as disabled in Settings with a clear reason if a UI entry point is created.

---

## D11 — Tag Editor Library Selection
**Decision:** Pending evaluation. A TagLib Android binding will be selected in Phase M6 after checking: Android ABI compatibility, license (LGPL-2.1 for TagLib itself — acceptable for linking, but verify the specific Android binding's license), binary size impact, and security posture on malformed files. Fallback: read-only metadata display if no safe binding is available, with a clear explanation.

---

## D12 — Ambient/Atmospheric Background
**Decision:** The Atmospheric Queue layout uses procedural/generated mist treatment (existing `AtmosphericBackground.kt` component). No stock or copyrighted background images are bundled. A static gradient fallback is provided for reduced-motion and battery-saver modes.

---

## D13 — `ActiveAmberOnLight` Color Token (C15)
**Decision:** 
- Dark surfaces: `#D4AF37` for equalizer bars, active queue row accent bar, and dark-surface text.
- Light queue card: `ActiveAmberOnLight` ≈ `#8A6D00` (contrast ratio ≥ 4.7:1 on `#F9F9FB` — must be verified with a contrast unit test).
- Active row also uses bold weight and animated equalizer bars as a second visual cue (not color-only).
