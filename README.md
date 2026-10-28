<p align="center"><img src="docs/assets/favicon.svg" width="96" alt="Pulse Player icon"></p>

# Pulse Player

A fast, colorful, 100% offline music player for Android. Built with Kotlin, Jetpack Compose, AndroidX Media3 (ExoPlayer), Room, and Jetpack Glance.

## Highlights
- **100% Offline**: No network permission, no tracking, no ads, no cloud accounts.
- **Top Tabs**: For you, Songs, Playlists, Folders, Albums, Artists, plus global debounced search.
- **8 Custom Player Themes**: Interactive carousel featuring Classic, Mountain Horizon, Mesh Glass, Minimal Mood, Acoustic, Vinyl (with spinning arm and vinyl grooves), Coastal Ocean, and Starry Sky.
- **Skin Themes & Procedural Wallpapers**: Plum, Aurora, Afterglow, Ocean, Sunrise, Forest, Mono, and Pure Black AMOLED, plus custom photo backdrops.
- **Equalizer & Effects**: Real hardware-backed equalizer sliders, bass boost, 3D virtualizer, and reverb presets (capability-checked).
- **Sleep Timer & Fades**: Sleep timer with automatic 10-second volume fade-out and "stop after this track" option.
- **Synthesized Offline Sample Track**: Built-in 44.1kHz 16-bit stereo PCM WAV generator ("Pulse Horizon" by Pulse Studio) allows immediate testing of playback, themes, visualizers, and equalizer without needing manual file transfer.
- **Dual Backup & Restore System**:
  - **Option A (Legacy JSON)**: Preserves `pulseplayer.favorites` v1 format with fuzzy matching.
  - **Option B (Portable Archive)**: `.musicbackup` (ZIP) format with magic byte sniffing (detects ZIP even if masked as `.wav`), ZIP-slip sanitization, SHA-256 checksums, versioned `manifest.json`, multi-tier track reconciliation, and Room transaction-safe restore in `MERGE` or `REPLACE` modes.
- **Home Screen Widget**: Responsive Jetpack Glance widget supporting 2x2, 4x1, and 4x2 layouts.
- **Monthly Recap**: On-device listening statistics, top songs and artists, badges, and shareable card.
- **Adaptive Layout**: Supports handheld phones with floating mini player and tablets with NavigationRail and docked Now Playing panel.
- **Accessibility & Motion**: Complies with M3 touch target guidelines; includes reduced motion mode.

## Move to a New Phone or Tablet

### Option A: Favorites JSON
1. Settings → Backup & Restore → **Export Favourites (JSON)**.
2. Copy your audio files and the JSON file to your new device.
3. Open Pulse Player on the new device → Allow music scan (or Choose music folder).
4. Settings → Backup & Restore → **Import Favourites** → Review match preview → Apply.

### Option B: Portable Archive (.musicbackup)
1. Settings → Backup & Restore → **Create Backup Archive**.
2. Select desired categories (Favorites, Playlists, Settings, Stats).
3. Transfer the resulting `.musicbackup` archive to the new device.
4. Settings → Backup & Restore → **Restore from Archive** → Select archive file → Choose `Merge` or `Replace` mode → Restore.

## Build & Test Requirements
- Android SDK 35+
- Kotlin 2.1+ / Java 17+
- AGP 8.8+ / Gradle 8.10+

### Running Unit Tests
To run local JVM unit tests for archive validation, track reconciliation, and JSON codecs:
```bash
gradle :app:testDebugUnitTest
```
