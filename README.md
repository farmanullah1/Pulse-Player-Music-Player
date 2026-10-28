<p align="center"><img src="docs/assets/favicon.svg" width="96" alt="Pulse Player icon"></p>

# Pulse Player

A fast, colorful, 100% offline music player for Android. Built with Kotlin, Jetpack Compose, Media3, Room and Glance.

## Highlights
- **100% Offline**: No network permission, no tracking, no ads, no account required.
- **Top Tabs**: For you, Songs, Playlists, Folders, Albums, Artists, plus global debounced search.
- **8 Custom Player Themes**: Carousel with Classic, Mountain Horizon, Mesh Glass, Minimal Mood, Acoustic, Vinyl (with spinning arm and vinyl grooves), Coastal Ocean, and Starry Sky.
- **Skin Themes & Procedural Wallpapers**: Plum, Aurora, Afterglow, Ocean, Sunrise, Forest, Mono, and Pure Black AMOLED, plus custom photo backdrops.
- **Equalizer & Effects**: Real hardware-backed equalizer sliders, bass boost, 3D virtualizer, and reverb presets (capability-checked).
- **Sleep Timer & Fades**: Sleep timer with automatic 10-second volume fade-out and "stop after this track" option.
- **Favourites Export/Import (JSON)**: Easily backup and transfer your favorites across devices with intelligent fuzzy matching.
- **Home Screen Widget**: Responsive Jetpack Glance widget (2x2, 4x1, and 4x2 layouts).
- **Monthly Recap**: On-device listening statistics, top songs and artists, badges, and shareable card.
- **Adaptive Layout**: Supports handheld phones with floating mini player and tablets with NavigationRail and docked Now Playing panel.
- **Accessibility & Motion**: Complies with M3 touch target guidelines; includes reduced motion mode.

## Move to a New Phone or Tablet
1. Settings → Backup & Transfer → Export favourites (`PulsePlayer_Favorites_YYYY-MM-DD.json`).
2. Copy your music files and the JSON file to your new device.
3. Open Pulse Player on the new device → Allow music scan (or Choose music folder).
4. Settings → Backup & Transfer → Import favourites → Review match preview → Apply.

## Build Requirements
- Android SDK 35+
- Kotlin 2.1+ / Java 17+
- AGP 8.8+ / Gradle 8.10+
