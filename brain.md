# Pulse Player — Development Log (`brain.md`)

## 1. Done
- [x] Initial setup: `metadata.json`, `settings.gradle.kts`, `build.gradle.kts`, `app/build.gradle.kts`
- [x] Moved prompt specification markdown files to `/prompts/`
- [x] `AndroidManifest.xml` with offline permissions (`READ_MEDIA_AUDIO`, `READ_EXTERNAL_STORAGE`, `FOREGROUND_SERVICE_MEDIA_PLAYBACK`, `POST_NOTIFICATIONS`, `WAKE_LOCK`, `WRITE_SETTINGS`), no internet permission
- [x] Resources: `strings.xml`, `themes.xml`, `player_widget_info.xml`, `file_paths.xml`, vector widget & notification icons
- [x] Custom adaptive launcher icon (`ic_launcher.xml`, `ic_launcher_round.xml`, `ic_launcher_background.xml`, `ic_launcher_foreground.xml`) and PNG rasters for `mdpi`, `hdpi`, `xhdpi`, `xxhdpi`, `xxxhdpi`
- [x] Data Layer:
  - Room entities: `TrackEntity`, `FavoriteEntity`, `PlaylistEntity`, `PlaylistTrackEntity`, `PlayEventEntity`, `TrackWithFav`, `TrackStat`, `ArtistStat`, `FavAdded`
  - Room DAOs: `TrackDao`, `FavoriteDao`, `PlaylistDao`, `StatsDao`
  - Database: `PulseDb`
  - SettingsStore: DataStore preferences (theme mode, skin, player theme, wallpaper, reduced motion, fade, min duration, excluded folders, queue persistence)
  - Scanner: `LibraryScanner` (MediaStore audio scan + SAF document tree scan, duration filtering, relative path extraction)
  - Helper & Artwork: `norm()` string normalizer, `ArtworkLoader` with LruCache & Palette
  - Favorites JSON: `FavoritesCodec`, `FavoritesMatcher` (strong, high, medium fuzzy matching), `FavoritesRepository` (atomic apply, merge/replace, preview)
- [x] Playback Layer:
  - `MusicPlaybackService` with `ExoPlayer`, `MediaSession`, `AudioFx` attachment, `WidgetStateStore` updates, noisy handling
  - `PlayerController` with MediaController sync, real volume fades (in/pause), sleep timer with 10s fade-out, queue management, snapshot & restore
  - `AudioFx` capability-checked equalizer, bass boost, virtualizer, reverb, and presets
  - `RingtoneHelper` with `WRITE_SETTINGS` system settings flow
- [x] UI Design System:
  - `Pulse` color palette (AMOLED Black, Plum gradients, Cyan accents)
  - 8 `Skins` (Plum, Aurora, Afterglow, Ocean, Sunrise, Forest, Mono, Pure black)
  - `PulseTypography` & M3 `Shapes`
  - `AppBackground` supporting procedural backgrounds, custom gallery wallpaper, and AMOLED black
  - `LocalSkin`, `LocalAccent`, `LocalReducedMotion`
- [x] UI Components:
  - `Artwork` with thumbnail caching and vector fallback
  - `HeartButton` with bouncy spring animation
  - `PlayingBars` with infinite height animation (respects reduced motion)
  - `TrackRow` with combined clickable, artwork, date, and 3-dot menu
  - `PillButton` (Shuffle / Play)
  - `MiniPlayer` with spinning disc, marquee text, progress bar, queue and play/pause controls
  - `SeekBar` throttled progress without full recomposition
  - `AlphabetScroller` fast index scroller
  - `WidgetTile`, `SquareCard`, `SectionHeader`
- [x] UI Screens:
  - `AppShell` with WindowSizeClass (Drawer + MiniPlayer for phone, NavigationRail + docked NowPlayingPanel for tablet)
  - `HomeTabsScreen` with 6 tabs (For you, Songs, Playlists, Folders, Albums, Artists) and top actions
  - `ForYouScreen` with resume banner, widget tiles, recently added row, top picks, and designed empty state
  - `SongsScreen` with sorting, multi-select bar, and fast scroller
  - `PlaylistsScreen` with 2x2 system playlists grid and custom user playlists
  - `FoldersScreen` with audio folder grouping and SAF folder picker
  - `AlbumsScreen` with adaptive grid and square artwork
  - `ArtistsScreen` with initial gradient avatars
  - `DetailScreen` for albums, artists, folders, playlists, and system lists
  - `SearchScreen` with debounced search and contextual playback
  - `NowPlayingScreen` with Song/Lyrics toggle, `.lrc` reader, player top menu, seek bar, transport, and bottom sheet shortcuts
  - `NowPlayingPanel` docked tablet view
  - 8 `PlayerThemes` with procedural canvases (`StarField`, `WavesCanvas`, `MountainSunset`, `VinylDisc`, `AcousticDisc`)
  - `PlayerThemeScreen` with swipeable carousel and APPLY/USING button
  - `SkinThemeScreen` with color swatches, procedural presets, and photo picker
  - `SettingsScreen` covering Appearance, Library, Playback, Backup, Widgets, and About
  - `EqualizerScreen` with master toggle, preset chips, real band sliders, bass boost, virtualizer, and reverb
  - `RecapScreen` with monthly listening stats, badges, and share card
  - `BackupScreen` with JSON export/import and step-by-step instructions
  - `Sheets` & `Dialogs`: QueueSheet, SleepSheet, AddToPlaylistSheet, TrackMenuSheet, SongDetailsDialog, CreatePlaylistDialog, ImportPreviewDialog
- [x] Glance Widget:
  - `PlayerWidget` with responsive 2x2, 4x1, and 4x2 layouts
  - `PlayerWidgetReceiver`, `WidgetControls`, `PlayPauseCb`, `NextCb`, `PrevCb`, `WidgetStateStore`
- [x] `PulseApp` application class with single-instance DI graph
- [x] `MainActivity` with edge-to-edge, window size calculation, notification permission request, and periodic queue snapshot
- [x] `README.md` and `docs/assets/favicon.svg`

## 2. Tested
- Code generation verified across all data, playback, UI, and widget layers
- Gradle compilation with `compile_applet` passed successfully (assembleDebug created APK)

## 3. In Progress
- Completed all tasks from 00_START_HERE.md and moved prompt specs to /prompts/

## 4. Known Limits & Honest Constraints
- Hardware audio effects (equalizer bands, bass boost, virtualizer, reverb) depend on Android OS audiofx engine availability; unsupported features are cleanly hidden.
- Ringtone setting requires the user to grant `WRITE_SETTINGS` in system settings.
