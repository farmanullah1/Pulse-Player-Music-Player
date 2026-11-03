# Preservation Matrix — Phase M0
> Every existing Pulse Player capability mapped to current location and post-merge destination.  
> No existing capability may disappear from this matrix.

---

| # | Capability | Current Screen / Component | Current Status | Post-Merge Destination | Risk |
|---|---|---|---|---|---|
| P01 | Primary navigation (For You, Songs, Playlists, Folders, Albums, Artists) | `HomeTabsScreen`, `AppShell` | WORKING | Preserved in `HomeTabsScreen`; new library pages added to configurable "More" drawer | Low |
| P02 | Drawer navigation (phone) | `AppShell` | WORKING | Preserved; new drawer items added for new destinations | Low |
| P03 | NavigationRail (medium/expanded) | `AppShell` | WORKING | Preserved | Low |
| P04 | Docked Now Playing panel (expanded) | `AppShell` | WORKING | Preserved | Low |
| P05 | Screen back stack | `MainViewModel` | WORKING | Preserved | Low |
| P06 | Resume/Continue banner | `ForYouScreen` | WORKING | Preserved | Low |
| P07 | Quick Shuffle/Play actions | `ForYouScreen` | WORKING | Preserved | Low |
| P08 | Today's listening time tile | `ForYouScreen` | WORKING | Preserved | Low |
| P09 | Favourites shortcut and count | `ForYouScreen` | WORKING | Preserved | Low |
| P10 | Sleep timer shortcut | `ForYouScreen` | WORKING | Preserved | Low |
| P11 | Equalizer shortcut | `ForYouScreen` | WORKING | Preserved | Low |
| P12 | Recently added tracks | `ForYouScreen` | WORKING | Preserved | Low |
| P13 | Top picks / most played | `ForYouScreen` | WORKING | Preserved | Low |
| P14 | Empty-library onboarding (Scan, Choose folder) | `ForYouScreen` | WORKING | Preserved | Low |
| P15 | Songs list (sort, multi-select, A–Z scroller) | `SongsScreen` | WORKING | Preserved; sort extended | Low |
| P16 | Missing songs chip/banner | `SongsScreen` | WORKING | Preserved | Low |
| P17 | Track context menu (play next, queue, playlist, fav, details, ringtone, share) | `Sheets.kt` / `TrackRow.kt` | WORKING | Preserved; extended with new actions | Low |
| P18 | Albums grid and detail | `AlbumsScreen`, `DetailScreen` | WORKING | Preserved | Low |
| P19 | Artists list and detail | `ArtistsScreen`, `DetailScreen` | WORKING | Preserved | Low |
| P20 | Folders list and detail | `FoldersScreen`, `DetailScreen` | WORKING | Preserved | Low |
| P21 | Playlists list and detail | `PlaylistsScreen`, `DetailScreen` | WORKING | Preserved | Low |
| P22 | Search (4 sections, 250ms debounce) | `SearchScreen` | WORKING | Preserved; extended | Low |
| P23 | System playlists (Favourites, Recently added, Recently played, Top tracks) | `PlaylistsScreen` | WORKING | Preserved | Low |
| P24 | User playlists (create, rename, reorder, delete) | `PlaylistsScreen` | WORKING | Preserved | Low |
| P25 | Background playback | `MusicPlaybackService` | WORKING | Preserved | Low |
| P26 | MediaSession + notification | `MusicPlaybackService` | WORKING | Preserved | Low |
| P27 | Lock-screen controls | `MusicPlaybackService` | WORKING | Preserved | Low |
| P28 | Mini-player (spinning disc, marquee, progress) | `MiniPlayer.kt` | WORKING | Preserved | Low |
| P29 | Expand/collapse player transition | `NowPlayingScreen` | WORKING | Preserved | Low |
| P30 | Song/Lyrics toggle | `NowPlayingScreen` | WORKING | Preserved | Low |
| P31 | Seek bar (elapsed/total) | `PlaybackSeekBar`, all layouts | WORKING | Preserved | Low |
| P32 | Shuffle + Repeat (Off/All/One) | `QueueModel`, all layouts | WORKING | Preserved | Low |
| P33 | Previous / Play-Pause / Next | All layouts | WORKING | Preserved | Low |
| P34 | Favourite toggle in player | All layouts | WORKING | Preserved | Low |
| P35 | Sleep timer (sheet, presets, end-of-track) | `SleepTimerEngine`, `Sheets.kt` | WORKING | Preserved; extended with new modes | Low |
| P36 | Add to playlist sheet | `Sheets.kt` | WORKING | Preserved | Low |
| P37 | Queue sheet | `Sheets.kt` / `QueuePanel.kt` | WORKING | Preserved | Low |
| P38 | Equalizer screen | `EqualizerScreen` | WORKING | Preserved; extended | Low |
| P39 | Track details/share | `Sheets.kt` | WORKING | Preserved; extended | Low |
| P40 | Player theme carousel (APPLY/USING, Special badge) | `PlayerThemeScreen` | WORKING | Preserved; new themes added | Low |
| P41 | Queue reorder / remove | `QueuePanel.kt` | WORKING | Preserved | Low |
| P42 | Playback position + queue restore (paused) | `QueueSnapshot`, `SettingsStore` | WORKING | Preserved | Low |
| P43 | 12 player themes (classic...atmospheric_queue) | `PlayerThemes.kt` | WORKING | Preserved; count will grow | Medium (ThemeRegistryTest must update) |
| P44 | Player layout registry + picker | `PlayerLayoutMode.kt`, `PlayerLayoutBottomSheet.kt` | WORKING | Extended with new layouts | Low |
| P45 | Atmospheric Queue layout | `AtmosphericQueuePlayerLayout.kt` | WORKING | Preserved | Low |
| P46 | Minimalist layout | `MinimalistPlayerLayout.kt` | WORKING | Preserved | Low |
| P47 | Modern Art layout | `ModernArtPlayerLayout.kt` | WORKING | Preserved | Low |
| P48 | Split Lyrics layout | `SplitLyricsPlayerLayout.kt` | WORKING | Preserved | Low |
| P49 | Vinyl Record layout | `VinylRecordPlayerLayout.kt` | WORKING | Preserved | Low |
| P50 | Waveform layout | `WaveformPlayerLayout.kt` | WORKING | Preserved | Low |
| P51 | Immersive layout | `ImmersivePlayerLayout.kt` | WORKING | Preserved | Low |
| P52 | Pixel Expressive layout | `PixelPlayerLayout.kt` | WORKING | Preserved | Low |
| P53 | Namida Waveform layout | `NamidaPlayerLayout.kt` | WORKING | Preserved | Low |
| P54 | 15 skin themes (Plum, Aurora, Afterglow, Ocean, Sunrise, Forest, Mono, Pure Black + 7 more) | `SkinThemeScreen`, `Color.kt` | WORKING | Preserved; new skins additive | Low |
| P55 | ThemeMode: SYSTEM/DARK/AMOLED/LIGHT | `SettingsStore`, `Theme.kt` | WORKING | Preserved; new color sources added as separate axis | Low |
| P56 | Dynamic accent from artwork | `SettingsStore`, `Theme.kt` | WORKING | Preserved as one `colorSource` option | Low |
| P57 | Reduced motion setting | `SettingsStore` | WORKING | Preserved | Low |
| P58 | Library scan (MediaStore + SAF, recoverable) | `LibraryScanner`, `SafScanner` | WORKING | Preserved; extended with new filters | Low |
| P59 | Folder exclude/include | `SettingsStore`, `SettingsScreen` | WORKING | Preserved | Low |
| P60 | SAF folder selection | `SettingsStore` | WORKING | Preserved | Low |
| P61 | SD-card mount/unmount handling | `StorageReceiver` | WORKING | Preserved | Low |
| P62 | Minimum duration filter | `SettingsStore` | WORKING | Preserved | Low |
| P63 | Identity model (ms:/saf:/copy:) | `IdentityModel`, `TrackEntity` | WORKING | Preserved; extended with new source types | Low |
| P64 | Duplicate detection (normTitle + normArtist) | `TrackDao`, `DuplicatesScreen` | WORKING | Preserved | Low |
| P65 | Missing-file preservation (favorites/playlists kept) | `ScanDao`, scan logic | WORKING | Preserved | Low |
| P66 | Artwork cache (LRU, bucketed, trim on low memory) | `ArtworkLoader`, `BlurCache`, `PulseApp` | WORKING | Preserved | Low |
| P67 | SessionTracker (single writer of play_events) | `SessionTracker` | WORKING | Preserved; `origin` column added via migration | Medium (migration needed) |
| P68 | Monthly recap | `RecapScreen`, `StatsDao` | WORKING | Preserved; extended | Low |
| P69 | Classic backup v1 JSON (pulseplayer.favorites) | `ClassicBackup.kt`, `FavoritesRepository` | WORKING | Preserved; Classic v2 audio copy added alongside | Low |
| P70 | Full backup reference format (ZIP-in-.wav) | `ArchiveBackupService`, `FullBackupValidator` | WORKING | Preserved | Low |
| P71 | BackupSniffer (no cross-contamination) | `BackupCommon.kt` | WORKING | Preserved | Low |
| P72 | LRC lyrics (import, synced display, parser) | `LrcParser`, `LyricsDao`, `LyricsContent` | WORKING | Preserved; extended with editor and new formats | Low |
| P73 | Glance widget (2×2, 4×1, 4×2) | `PlayerWidget.kt` | WORKING | Preserved; improved | Low |
| P74 | Widget state updates | `WidgetStateStore.kt` | WORKING | Preserved | Low |
| P75 | FadingPlayer / AudioTransitionController (single volume writer) | `FadingPlayer.kt`, `AudioTransitionController.kt` | WORKING | Preserved; extended for ReplayGain via AudioProcessor gain stage | Medium (C4 in spec) |
| P76 | QueueModel (seeded shuffle, no Media3 shuffle) | `QueueModel.kt` | WORKING | Preserved; extended with new modes | Low |
| P77 | SleepTimerEngine (10s fade, end-of-track) | `SleepTimerEngine.kt` | WORKING | Preserved; extended with new timer modes | Low |
| P78 | ErrorPolicy (MISSING_FILE, UNSUPPORTED, etc.) | `ErrorPolicy.kt` | WORKING | Preserved; extended | Low |
| P79 | AudioFx (capability-aware equalizer, bass, virtualizer, reverb) | `AudioFx.kt` | WORKING | Preserved; extended with more effects | Low |
| P80 | Speed control (0.5–2.0×) | Playback settings | WORKING | Preserved | Low |
| P81 | Headset-unplug pause | `MusicPlaybackService` | WORKING | Preserved | Low |
| P82 | Stop after current track | Playback settings | WORKING | Preserved | Low |
| P83 | Ringtone action (permission-safe) | `RingtoneHelper.kt` | WORKING | Preserved | Low |
| P84 | Gemini AI transcription / music generation | `TranscribeScreen`, `GenerateMusicScreen`, `GeminiAudioClient` | WORKING | Preserved; AI features gated in new Settings IA | Low |
| P85 | Per-screen sort persistence (sort_raw JSON) | `SettingsStore.saveSortRaw()` | WORKING | Preserved | Low |
| P86 | Playlist sort persistence | `PlaylistEntity.sortKey/sortOrder` | WORKING | Preserved | Low |
| P87 | Permission monitoring | `PermissionMonitor` | WORKING | Preserved; extended for new permissions | Low |
| P88 | Pre-migration DB backup | `PulseDb.backupDatabaseBeforeMigration()` | WORKING | Preserved for schema v4 migration | Low |
| P89 | `docs/TEMP_CODE.md` (empty, 0 TODO-TEMP) | `docs/TEMP_CODE.md` | WORKING | Maintained | Low |
| P90 | `docs/spec/SPEC_INDEX.md` | `docs/spec/SPEC_INDEX.md` | WORKING | Updated with M0–M14 phases | Low |
| P91 | `brain.md` development log | `brain.md` | WORKING | Updated after each phase | Low |
| P92 | Grep gates (9 confirmed gates) | Code / CI | WORKING | Preserved and extended | Low |
| P93 | `BNO INTERNET permission` (offline build) | `AndroidManifest.xml` | WORKING | Preserved in `offline` flavor; `full` flavor adds INTERNET (C1) | Medium (flavor split needed) |
| P94 | No destructive migrations | Room schema | WORKING | Preserved; schema v4 migration uses `ALTER TABLE ADD COLUMN ... DEFAULT` | Low |
| P95 | `AudioSignalPathBadge` | `AudioSignalPathBadge.kt` | WORKING | Preserved | Low |
| P96 | `WaveformSeekBar` component | `WaveformSeekBar.kt` | PARTIAL | Extended with full PCM decode pipeline | Medium |
| P97 | `AlphabetScroller` (A–Z fast scroll) | `AlphabetScroller.kt` | WORKING | Preserved | Low |
| P98 | `AtmosphericBackground` component | `AtmosphericBackground.kt` | WORKING | Preserved | Low |
| P99 | ProceduralCanvases (star field, waves, etc.) | `ProceduralCanvases.kt` | WORKING | Preserved | Low |
| P100 | Sample music helper (test data, no production use) | `SampleMusicHelper.kt` | WORKING | Preserved | Low |
