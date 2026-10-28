# Changelog

All notable changes to the **Pulse Player** project are documented in this file.

## [1.0.0] - 2026-10-28

### Added
- **Complete Offline Native Architecture**:
  - Jetpack Compose M3 UI with 100% offline playback.
  - Zero internet permissions (`android.permission.INTERNET` omitted by design).
  - Media3 / ExoPlayer `MediaSessionService` foreground playback engine with audio focus, noisy receiver, and rich system media controls.
  - SQLite/Room database with entities for tracks, playlists, favorites, and play event statistics.
- **Library Scanning & Management**:
  - MediaStore scanner with instant audio discovery and relative path extraction.
  - Storage Access Framework (SAF) folder picker for custom music directories (e.g., SD card or custom `Music` folder).
  - Minimum track duration filter to ignore short sound clips and notification tones.
  - Folder exclusion rules.
- **Audio & Playback Features**:
  - Equalizer with 5-band frequency adjustment, preset selector (Rock, Pop, Jazz, Classical, Flat), bass boost, virtualizer, and 3D reverb presets.
  - Hardware audiofx capability checks hiding unsupported controls on specific device chipsets.
  - Real volume fades on play/pause and smooth 10-second volume fade-out on sleep timer expiration.
  - Persistent queue and last-played position across app restarts.
  - System ringtone setter with `WRITE_SETTINGS` permission flow.
  - Embedded sample music generator (`SampleMusicHelper`) synthesizing a 44.1kHz 16-bit stereo PCM WAV track ("Pulse Horizon") for zero-dependency first-boot testing.
- **AMOLED-First Design & Theming**:
  - True-black (`#000000`) surfaces tailored for Infinix AMOLED displays.
  - 8 distinct UI skin themes (Plum, Aurora, Afterglow, Ocean, Sunrise, Forest, Mono, Pure black).
  - 8 procedural canvas player themes (StarField, WavesCanvas, MountainSunset, VinylDisc, AcousticDisc, etc.).
  - Custom gallery photo wallpaper support with private storage caching.
  - Dynamic edge-to-edge window insets handling for punch-hole cameras and gesture navigation.
  - Canonical responsive layout: compact drawer + mini player for phones, navigation rail + docked Now Playing panel for tablets.
- **Dual Backup & Restore Engine**:
  - **Option A (Legacy JSON)**: Preserves `pulseplayer.favorites` v1 format with fuzzy track matcher.
  - **Option B (Portable Archive)**: `.musicbackup` (ZIP) format with magic byte sniffing (detects ZIP even if disguised as `.wav`), safe ZIP-slip path sanitization, size/entry threshold protection, SHA-256 integrity checksums, versioned `manifest.json`, multi-tier track reconciliation, and Room transaction-safe restore in `MERGE` or `REPLACE` modes.
- **Homescreen Widgets & Insights**:
  - Jetpack Glance app widget supporting responsive 2x2, 4x1, and 4x2 configurations.
  - Monthly Recap dashboard highlighting top tracks, top artists, listening streaks, and shareable summary cards.
- **Testing**:
  - Comprehensive unit test suite covering `ArchiveValidator`, `TrackMatchResolver`, and `FavoritesCodec`.
