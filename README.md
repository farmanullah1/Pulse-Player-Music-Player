<div align="center">

<img src="docs/assets/favicon.svg" width="96" alt="Pulse Player icon">

# Pulse Player

### A private, expressive, offline-first music player for Android

Built with **Kotlin**, **Jetpack Compose**, **Material 3**, **Media3**, **Room**, and **Jetpack Glance**.

<p>
  <img src="https://img.shields.io/badge/platform-Android-3DDC84?style=flat-square&logo=android&logoColor=white" alt="Android">
  <img src="https://img.shields.io/badge/Kotlin-2.1%2B-7F52FF?style=flat-square&logo=kotlin&logoColor=white" alt="Kotlin">
  <img src="https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4?style=flat-square&logo=jetpackcompose&logoColor=white" alt="Jetpack Compose">
  <img src="https://img.shields.io/badge/privacy-100%25%20offline-00C3FF?style=flat-square" alt="Offline">
</p>

</div>

## ✨ Why Pulse Player?

Pulse Player combines a fast local library with expressive player screens inspired by modern Android music-player patterns. It keeps the original Pulse experience while adding selectable layouts, atmospheric visuals, powerful queue controls, and portable backups.

- **No internet permission, ads, tracking, or cloud account**
- **Material 3 light, dark, AMOLED, and system appearance modes**
- **Responsive phone, foldable, tablet, and large-screen layouts**
- **Safe library scanning with MediaStore and Storage Access Framework support**

## 🎵 Features at a Glance

| Area | Included |
| --- | --- |
| Library | For You, Songs, Playlists, Folders, Albums, Artists, search, sorting, duplicate finder |
| Playback | Media3 playback, queue editing, shuffle, repeat, sleep timer, fade-out, resume playback |
| Player designs | 12 selectable layouts with previews and persistent settings |
| Visual customization | 15 skins, procedural wallpapers, custom wallpapers, reduced-motion mode |
| Audio | Hardware-aware equalizer, bass boost, virtualizer, reverb, signal-path details |
| Backup | Portable favourites folder, v1 JSON compatibility, fuzzy matching, full reference archive backup |
| Insights | Monthly recap, listening streaks, top tracks, session tracking, shareable recap card |
| Extras | 2×2, 4×1, and 4×2 home-screen widgets; offline sample track; lyrics import |

## 🎨 Player Layouts & Skins

Open **Settings → Player Layout** to switch layouts without changing the library or queue:

1. Classic
2. Mountain Horizon
3. Mesh Glass
4. Minimal Mood
5. Acoustic
6. Vinyl
7. Coastal Ocean
8. Starry Sky
9. Immersive Artwork
10. Pixel Expressive
11. Namida Waveform
12. Atmospheric Queue

The original Pulse layouts remain available. Pixel-style expressive controls, Namida-inspired waveform seeking, and the forest/mist queue player are integrated into the same state-hoisted playback system.

Open **Settings → Skin Theme** for:

- Plum, Aurora, Afterglow, Ocean, Sunrise, Forest, Mono, Pure Black AMOLED, Neon, Candy, and Lava
- Pixel Material You, Namida Amber, Namida Cyberpunk, and Atmospheric Mist
- Starry, waves, and sunset procedural wallpapers
- Custom photo wallpapers with readable overlays

The active Material 3 color scheme is used for navigation, text, controls, cards, dialogs, progress indicators, and empty states so light and dark modes remain readable throughout the app.

## 💾 Portable Classic Backup

Classic Backup preserves the existing `pulseplayer.favorites` **v1 JSON format** while making the backup self-contained.

### Export

1. Open **Settings → Backup & Restore → Classic Backup**.
2. Tap **Export to Music/Pulse Player**.
3. Pulse Player creates:

   ```text
   Music/
   └── Pulse Player/
       ├── pulseplayer.favorites.json
       ├── favourite-song-1.mp3
       ├── favourite-song-2.flac
       └── ...
   ```

4. The JSON keeps the v1 schema and metadata. Audio files are copied beside it so the favourites survive deletion or movement of the originals.

Existing copies with matching sizes are reused. Files that cannot be read are reported without invalidating the JSON export.

### Import on another device

1. Copy or paste the complete `Pulse Player` folder into the device’s `Music` directory.
2. Open **Settings → Backup & Restore**.
3. Tap **Import Folder** and select the `Pulse Player` folder.
4. Pulse Player scans the pasted audio, previews fuzzy matches, and lets you merge or replace favourites.

Matching uses filename and size when available, then title, artist, album, and duration. Ambiguous or missing tracks are shown instead of guessed.

JSON-only import/export remains available for compatibility with older backups.

## 🛡️ Full Backup & Restore

The Full Backup system supports the reference archive format, including `.wav`-named ZIP archives:

- Magic-byte detection for ZIP, JSON, and real WAV files
- ZIP-slip and archive-size protection
- Metadata, playlists, settings, tags, lyrics, scan exclusions, and listening history
- Deterministic five-tier track resolution
- Atomic Room transactions
- Merge and explicit replace modes
- Unmatched-track report after restore
- SHA-256 manifest verification for generated archives

## 📱 Responsive Design

Pulse Player adapts to available width:

- **Compact phones:** top tabs, drawer navigation, floating mini-player, full-screen Now Playing
- **Medium screens and foldables:** flexible content spacing and adaptive grids
- **Expanded tablets:** NavigationRail, wider library cards, and docked Now Playing panel
- **Large layouts:** constrained content widths prevent stretched controls and improve readability

All important controls maintain Material touch-target guidance, and reduced-motion mode disables continuous visual animations.

## 🔐 Permissions & Privacy

Pulse Player requests only permissions needed for local playback and notifications:

- Audio-library access for MediaStore scanning
- Notification permission on Android 13+
- Storage write permission only on Android 9 and older when creating a portable backup
- User-selected SAF folder access for restricted or removable storage

No audio, metadata, or usage statistics leave the device.

## 🧱 Project Structure

```text
app/src/main/java/com/pulse/player/
├── data/       Room database, scanning, settings, backup, lyrics, AI helpers
├── playback/   Media3 controller, queue, transitions, effects, statistics
└── ui/
    ├── components/
    ├── screens/
    ├── theme/
    └── themes/  Player layout registry and previews
```

## 🛠️ Build Requirements

- Android Studio Ladybug or newer
- Android SDK 35+
- JDK 17+
- Kotlin 2.1+
- Gradle 8.10+

### Compile

```bash
./gradlew :app:compileDebugKotlin
```

Windows PowerShell:

```powershell
.\gradlew.bat :app:compileDebugKotlin
```

### Run unit tests

```bash
./gradlew :app:testDebugUnitTest
```

The test suite covers JSON codecs, fuzzy matching, layout/theme registries, backup validation, queue transitions, sleep timers, and listening statistics.

## 📄 License & Contributions

Keep changes focused, preserve existing player behavior, and run the relevant Gradle checks before opening a pull request. Do not add credentials, local SDK paths, generated build output, or private media files to the repository.
