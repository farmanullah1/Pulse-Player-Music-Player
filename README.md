<p align="center"><img src="docs/assets/favicon.svg" width="96" alt="Pulse Player icon"></p>

# Pulse Player

A fast, colorful, 100% offline music player for Android. Built with Kotlin, Jetpack Compose, AndroidX Media3 (ExoPlayer), Room, and Jetpack Glance.

## Highlights
- **100% Offline**: No network permission, no tracking, no ads, no cloud accounts.
- **Top Tabs**: For you, Songs, Playlists, Folders, Albums, Artists, plus global debounced search.
- **8 Custom Player Themes**: Interactive carousel featuring Classic, Mountain Horizon, Mesh Glass, Minimal Mood, Acoustic, Vinyl (with spinning arm and vinyl grooves), Coastal Ocean, and Starry Sky.
- **Skin Themes & Procedural Wallpapers**: Plum, Aurora, Afterglow, Ocean, Sunrise, Forest, Mono, Pure Black AMOLED, Neon, Candy, and Lava, plus custom photo backdrops.
- **Equalizer & Effects**: Real hardware-backed equalizer sliders, bass boost, 3D virtualizer, and reverb presets (capability-checked).
- **Sleep Timer & Fades**: Sleep timer with automatic 10-second volume fade-out and "stop after this track" option.
- **Synthesized Offline Sample Track**: Built-in 44.1kHz 16-bit stereo PCM WAV generator ("Pulse Horizon" by Pulse Studio) allows immediate testing of playback, themes, visualizers, and equalizer without needing manual file transfer.
- **Dual Backup & Restore System**:
  - **Classic Backup (Favourites JSON)**: Preserves `pulseplayer.favorites` v1 JSON format with fuzzy matching.
  - **Full Backup (Compatible Archive)**: Full compatibility with the Infinix `ManualBackup_*.wav` format (standard ZIP holding 16 specific metadata, JSON configs, and SQLite database entries). Includes magic byte sniffing (detects ZIP even if masked as `.wav`), ZIP-slip sanitization, 5-tier deterministic track resolution (exact path -> storage-relative -> folder + filename -> metadata tags -> unique filename), and atomic Room transaction-safe restore in `MERGE` or `REPLACE` modes.
- **Home Screen Widget**: Responsive Jetpack Glance widget supporting 2x2, 4x1, and 4x2 layouts.
- **Monthly Recap**: On-device listening statistics, top songs and artists, badges, and shareable card.
- **Adaptive Layout**: Supports handheld phones with floating mini player and tablets with NavigationRail and docked Now Playing panel.
- **Accessibility & Motion**: Complies with M3 touch target guidelines; includes reduced motion mode.

## Backup & Restore

Pulse Player provides two independent, secure backup & restore methods under **Settings → Backup & Restore**:

### 1. Classic Backup (Favourites JSON)
- **Export**: Exports your favourites list as a lightweight JSON document (`pulseplayer.favorites`).
- **Import**: Restores favourites using fuzzy matching across track title, artist, album, and duration.
- **Safety**: Unchanged from the original format; ideal for quick favourites transfers between devices.

### 2. Full Backup (Compatible Archive)
- **Import / Restore**: Reads and restores files created by the Infinix music player backup system (`ManualBackup_*.wav` or `.zip`).
  - Sniffs file header magic bytes (`PK` vs `RIFF...WAVE` vs `{`). Reject real audio WAVs with an informative warning.
  - Parses category flags, track metadata tags, playlists (`My Favourite` mapped to favourites table), sort configs, scan exclusions, and monthly play history (`PlaybackRecords.json`).
  - Employs a strictly non-destructive 5-tier matching engine. Ambiguous files (multiple matches) are never guessed.
  - Restores in a single atomic Room transaction (`Merge` adds missing data; `Replace` prompts for explicit confirmation). Repeat restores produce 0 duplicates.
  - Post-restore report provides category counts, tier matches, and an exportable Unmatched Songs list (`.txt`).
- **Create Backup**:
  - Exports a standard compatible archive containing all 16 entries in reference order (`settings.bak`, `Covers/*`, `MusicTagsBackupConfig.conf`, `Lyrics/*`, `PlayLists/*`, `SortBackupConfig.conf`, `ScanHiddenBackupConfig.conf`, `Statistics/*`, and `metadata.inf`).
  - Allows saving as `.wav` (original player default) or `.zip` (with audio scanner warning).
- **Compatibility Notice**: Import and parsing of the Infinix archive layout is fully verified and covered by test suites. Export strictly conforms to the 16-entry reference structure, JSON schemas, and SQLite database specifications; however, acceptance of the exported backup by the original third-party player cannot be formally guaranteed or tested without the original proprietary app.

## Build & Test Requirements
- Android SDK 35+
- Kotlin 2.1+ / Java 17+
- AGP 8.8+ / Gradle 8.10+

### Running Unit Tests
To run local JVM unit tests for archive validation, track reconciliation, and JSON codecs:
```bash
gradle :app:testDebugUnitTest
```
