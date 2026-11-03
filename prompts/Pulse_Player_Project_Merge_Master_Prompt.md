# Pulse Player — Project Merge & Feature Expansion Master Prompt
## Google AI Studio implementation brief · Product, UX, feature and quality requirements only

> **Purpose:** Use this document as the authoritative product-level merge request for the existing Pulse Player Android project. It tells the AI implementation model what the app already is, what must remain intact, what to inspect in the two reference projects, and what additional capabilities, screens, themes, layouts, workflows and quality standards must be added.
>
> **Important:** This is a requirements and implementation-direction document, not a code-generation tutorial. Do not treat the examples as permission to replace the existing architecture blindly. Inspect the actual repository first, map every requirement to the existing app, and implement in safe, testable stages.

---

# 00 — START HERE

## 0.1 Your role

Act as a Principal Android Product Architect, Senior Kotlin/Jetpack Compose Engineer, Audio Playback Engineer, Accessibility Specialist and QA Lead.

You are modifying an **existing Android application named Pulse Player**. This is an enhancement and merge project, not a greenfield rebuild.

Your job is to combine:

1. Every working Pulse Player feature and design already present in the project.
2. The additional product capabilities and interaction patterns represented by **Namida**.
3. The additional product capabilities and Material You / Compose patterns represented by **PixelPlayer**.
4. The existing Pulse Player design pack and backup specifications.
5. The new Atmospheric Queue player layout and live playback-button arrangement switcher described in this document.
6. Additional production-quality improvements required to make the combined product coherent, reliable, accessible, responsive and maintainable.

**Never assume a feature is absent just because it is not mentioned in this document.** Inspect the actual source, screens, resources, database, settings, navigation, tests and current APK behavior before changing anything.

## 0.2 Project identity and existing technical direction

Pulse Player is a native Android music player. Its current specification describes:

- Kotlin as the application language.
- Jetpack Compose and Material 3 for the primary UI.
- AndroidX Media3 / ExoPlayer and a media-session playback service.
- Room for persistent library, favourites, playlists and listening history.
- DataStore for user settings.
- MVVM-style state management using StateFlow.
- Jetpack Glance for the home-screen widget.
- A single-Activity application structure.
- A local music library sourced from phone storage and user-selected SD-card/folder locations.
- No login requirement, no advertisements and no mandatory cloud account.
- A true-black AMOLED-friendly design direction, artwork-driven colors, custom gradients, player themes and skins.
- A strong preference for real functionality, no fake data, no dead controls and honest reporting of limitations.

Keep the current application ID/package if one already exists. Do not rename the app, reset its data model, replace the navigation system or migrate libraries merely because a reference project uses a different approach.

The reference PixelPlayer repository identifies itself as Kotlin + Jetpack Compose + Material Design 3. Namida is a Flutter application. Therefore, **PixelPlayer is the closer implementation-technology reference**, while Namida is primarily a feature, interaction and product-behavior reference. Do not port Flutter source directly into the Kotlin app.

## 0.3 Reference projects

Study the current public project material before implementation:

- Namida: https://github.com/namidaco/namida
- Namida documentation: https://docs.namida.app/
- PixelPlayer: https://github.com/PixelPlayerHQ/PixelPlayer

Use these projects to understand feature behavior, navigation, customization, interaction patterns and product scope. Do not blindly copy source files, dependencies, branding, logos, artwork, screenshots or proprietary assets.

Before reusing any source code or asset, inspect the repository's current LICENSE and applicable notices. If the license does not clearly permit the intended reuse, independently implement the same general capability and visual concept without copying protected code or assets. Preserve third-party notices where reuse is permitted. Do not claim that Pulse Player is an official Namida or PixelPlayer fork.

The repositories can change. Treat their current README, documentation, changelog and actual source as the reference of record. Record the date and commit/release inspected in the project audit.

## 0.4 The central product promise

Pulse Player must remain **local-first and usable as a complete music player without an account or an internet connection**.

The original project pack states that the app has no INTERNET permission and excludes streaming, accounts, cloud sync, online lyrics/artwork, AI services and other online dependencies. The new merge request asks for capabilities from projects that include online services.

Resolve this conflict as follows:

- Preserve a fully usable offline/local playback experience.
- Local library, playback, playlists, favourites, queue, history, statistics, local lyrics, themes, equalizer controls supported by the device, Classic backup and Full backup must not depend on a network.
- Add network-dependent features as **optional, separately controlled integrations** only when they are genuinely useful and technically supportable.
- Online integrations must be off by default, clearly disclosed, individually switchable, and never required for core playback.
- If the current build must remain strictly internet-permission-free, implement the local equivalents and document online integrations as unavailable in that build. Do not silently add INTERNET permission.
- If the product owner chooses to ship optional online integrations, update the product privacy statement, manifest, settings, permission rationale, network security policy and README honestly. Never continue describing that build as “no internet permission” or “100% offline.”
- Do not add ads, trackers, analytics SDKs, mandatory sign-in, forced cloud accounts or background data collection.

A setting named **Offline-only mode** must disable all network requests and prevent online integrations from starting. It must not break local playback, local library browsing, local backups or local statistics.

## 0.5 Non-negotiable preservation rules

1. Do not delete or replace any existing working screen, feature, player theme, skin, wallpaper, widget, backup format, database record or user preference.
2. Do not redesign the entire app without first documenting the current design and obtaining a clear mapping from old components to new components.
3. Add new designs as additional selectable choices. Keep existing choices available.
4. Do not change existing backup formats or their established import behavior.
5. Do not wipe or destructively recreate the Room database. Use explicit migrations and verify data preservation.
6. Do not move, rename or delete a user's music automatically. Any file copy, tag edit, delete or move operation must be a separate, explicit, confirmed user action.
7. Do not fabricate songs, artists, albums, artwork, play counts, listening time, network results or backup records.
8. Every visible action must work, be deliberately disabled with an explanation, or not be shown.
9. Never claim a feature is implemented because a settings row or decorative screen exists. A feature is complete only when its full user workflow works.
10. Keep all expensive file, database, image, archive and audio-analysis work off the main thread.
11. Use the installed project's actual dependency versions and APIs. Do not invent library APIs or upgrade dependencies without a compatibility reason.
12. No fake success messages, simulated playback, pretend network responses or placeholder functionality.
13. Do not claim a build, test, device check or repository push passed unless it actually ran and succeeded.
14. The existing project documentation and fix-pack conflict resolutions remain authoritative unless this document explicitly updates them.

## 0.6 First action: repository audit — do not code yet

Before making changes, inspect the complete existing project.

Create a **Current Pulse Player Inventory** containing:

- Package/application ID, SDK levels and dependency versions.
- Existing architecture, navigation and state-management approach.
- All screens, tabs, drawers, dialogs, sheets, widgets and settings pages.
- Every current player theme, player layout, skin theme, color mode and wallpaper.
- Every current playback feature and audio effect.
- Current media scanner, storage sources, permissions and SD-card handling.
- Existing Room entities, DAOs, migrations and DataStore keys.
- Current queue model, playback service, notification and media-session behavior.
- Existing favourites JSON backup and Full backup reference-format implementation.
- Existing lyrics support, if any.
- Existing statistics, recap, history and widget behavior.
- Existing test classes, test count and actual test status.
- Known bugs, broken buttons, incomplete screens and inconsistent theme colors.
- Existing documentation, README and `brain.md`.
- Current Git branch, uncommitted changes and repository remote state.

For each feature, assign one status:

- **WORKING** — verified in source and, where possible, by running the app.
- **PARTIAL** — some behavior exists, but the complete workflow is missing.
- **BROKEN** — exists but does not work correctly.
- **MISSING** — no implementation found.
- **UNVERIFIED** — source suggests it exists, but behavior could not be verified.

Do not mark a feature WORKING merely because its name appears in code or settings.

Create a **Preservation Matrix** mapping every existing Pulse Player capability to its current screen/component and the planned destination after the merge. No existing capability may disappear from the matrix.

Then create a **Feature Gap Matrix** mapping every new requirement in this document to its implementation phase, UI entry point, dependencies, offline/online classification, tests and status.

Stop after the audit and plan. Do not start implementation until the audit has identified risks, conflicts and migration requirements.

---

# 01 — Existing Pulse Player: features and design that must be preserved

The following is the product baseline described in the original Pulse Player specification. Verify each item against the actual repository and preserve it if it exists. If a listed item has not yet been implemented, treat it as a required base feature rather than assuming it is already present.

## 1.1 Navigation and application shell

Preserve the existing primary navigation concept:

- For you
- Songs
- Playlists
- Folders
- Albums
- Artists

Preserve the drawer/navigation entry points for:

- Equalizer
- Sleep timer
- Monthly recap
- Favourites
- Backup & Transfer
- Skin theme
- Player theme
- Settings
- About

Maintain the responsive phone/tablet behavior already described in the original pack:

- Compact phone: top bar, drawer, tab navigation, content and floating mini-player.
- Medium width: NavigationRail and adaptive content.
- Expanded width: navigation rail, main content and a docked Now Playing panel where appropriate.

Do not remove the original top tabs, drawer, resume banner, Shuffle/Play pills, Recently added section, Top picks, system playlists or other established home content.

## 1.2 For You screen

Preserve and refine:

- Resume/Continue banner, only when genuine playback history exists.
- Quick Shuffle and Play actions.
- Real listening-time tile for today.
- Favourites shortcut and real count.
- Sleep timer shortcut.
- Equalizer shortcut.
- Recently added tracks.
- Top picks/most-played tracks when sufficient real history exists.
- Empty-library onboarding with Scan and Choose folder actions.
- No fabricated recommendations or placeholder counts.

## 1.3 Library screens

Preserve:

- Songs list and sorting.
- Albums grid and album details.
- Artists list and artist details.
- Folders list and folder details.
- Playlists list and playlist details.
- Search.
- Multi-select actions.
- Track details.
- Add to queue / Play next.
- Add to playlist.
- Favourite toggle.
- Share file.
- Ringtone action where supported.
- A–Z fast scroller if already implemented.
- Existing album-art fallback design and artwork cache.

## 1.4 Playback and Now Playing

Preserve:

- Background playback.
- MediaSession and notification controls.
- Lock-screen controls.
- Mini-player with artwork/disc animation and marquee title.
- Expand/collapse player transition.
- Song/Lyrics view toggle.
- Seek bar and elapsed/total time.
- Shuffle and repeat.
- Previous, play/pause and next.
- Favourite control.
- Sleep timer.
- Add to playlist.
- Queue.
- Equalizer.
- Track information and share actions.
- Player-theme carousel and its existing APPLY / USING state.
- Existing queue sheet, current-track highlight and queue operations.
- Existing playback position and queue restoration behavior.

The player must continue using one playback engine, one service/session and one authoritative queue model. A new visual layout must never create another player or another queue.

## 1.5 Existing player themes — preserve all nine

The current canonical list in the existing specification is:

1. `classic` — Classic
2. `mountain` — Mountain Horizon
3. `mesh` — Mesh Glass
4. `minimal` — Minimal Mood
5. `acoustic` — Acoustic
6. `vinyl` — Vinyl
7. `coast` — Coastal Ocean
8. `starry` — Starry Sky
9. `immersive` — Immersive Artwork Player

Keep their current IDs, visual identity, selection behavior and persistence. Do not rename IDs or silently replace previews. The original default is Classic unless the actual repository has already made a deliberate user-facing change; document any proposed default change before applying it.

The Immersive Artwork Player remains a presentation-only layout with blurred artwork, floating controls and a bottom queue panel. It must share the same playback state and controller as every other theme.

## 1.6 Existing design system

Preserve and refine the current AMOLED-first design language:

- True-black AMOLED option.
- Dark, light and system theme modes.
- Plum, Aurora, Afterglow, Ocean, Sunrise, Forest, Mono, Mesh and Dynamic gradient families.
- Artwork-driven dynamic accents.
- Cyan favourite accent and orange APPLY/USING action where already established.
- Rounded artwork, cards, bottom sheets and pill controls.
- 4dp spacing grid.
- 48dp minimum touch targets.
- Roboto/system typography unless a bundled font is deliberately introduced.
- Reduced-motion support.
- Existing spring animations, mini-player expansion, heart animation, playback icon morph, playing bars and theme transitions.

Do not globally replace the original design with a generic Material template. Material 3 should improve consistency and accessibility while retaining Pulse Player's visual identity.

## 1.7 Existing settings and local behaviors

Preserve existing settings for:

- Theme mode and skin.
- Player theme.
- Dynamic accent from artwork.
- Reduced motion.
- Library rescan.
- Folder selection and exclusion.
- Minimum duration filter.
- Playback fade behavior.
- Speed.
- Stop after current track.
- Pause on headset disconnect.
- Equalizer.
- Widgets.
- Backup & Transfer.
- About and offline/privacy statement.

Preserve these exact or equivalent baseline behaviors:

- Favourite changes persist immediately and update all screens from the same source of truth.
- Shuffle toggling off restores the intended original queue order.
- Repeat cycles Off → All → One.
- Sleep timer presets, custom duration and end-of-track option.
- Sleep timer fade-out behavior.
- Headset unplug pauses playback.
- Audio focus/call handling is stable.
- Missing files do not erase favourites.
- Queue restores paused after app relaunch.
- Recap is computed from real play history and hidden when there is no data.
- Unsupported audio effects are hidden or clearly disabled.
- Ringtone permission is requested only when the user invokes the ringtone action.

---

# 02 — Merge strategy and feature classification

## 2.1 Feature classification

Every requested feature must be assigned to one of these groups:

### A. Local core — must work offline

Examples: library scanning, folder filtering, playlists, favourites, queue, local search, local lyrics files, metadata display, local tag editing where supported, local statistics, themes, widgets, sleep timer, local backup/restore, local smart playlists and local recommendation algorithms.

### B. Optional online integration

Examples: LRCLIB lookup, Deezer artist artwork lookup, online AI playlist providers, YouTube-related functionality, scrobbling services, remote Subsonic/Jellyfin/WebDAV/SMB servers, Google Cast discovery where it requires network access, and cloud-hosted content.

These features must be isolated behind an integration layer, disabled by default and usable only after explicit user opt-in. If the shipped product must have no INTERNET permission, do not include active online clients in that build.

### C. Local-network integration

Examples: device-to-device sync and LAN listening party. These do not require a public cloud account, but they still require network communication and local-network permissions/behavior appropriate to Android versions. They must be opt-in, paired/authorized and disabled in Offline-only mode.

### D. Hardware-dependent audio capability

Examples: bit-perfect output, USB DAC exclusive/direct access, 32-bit float path, audio effects, output-device routing and hardware visualizers. Show only what the Android version, device, output route and audio engine can genuinely support.

### E. Platform-restricted / experimental

Examples: Android Auto, Wear OS, Chromecast, status-bar lyrics, sponsor-segment services, advanced video support, vocal removal and beat-matched crossfade. Do not fake support. Add only after platform compatibility, API availability, licensing and testing are established.

## 2.2 Capability registry

Create a central capability model so the UI can accurately distinguish:

- Available and enabled.
- Available but disabled by user.
- Requires permission.
- Requires optional integration setup.
- Unsupported by device.
- Unsupported by Android version.
- Experimental.
- Not included in this build.

Settings and screens must explain why a feature is unavailable. Never show a control that appears operational but does nothing.

## 2.3 Priority levels

- **P0 — Core reliability:** preservation, playback, library integrity, queue, themes, backup/restore, permission handling, accessibility, responsive layouts, no data loss.
- **P1 — High-value local features:** advanced library organization, smart playlists, enhanced search, lyrics editor/import, detailed statistics, button-layout presets, additional player layouts, audio profiles, local recommendations.
- **P2 — Optional integrations and advanced capabilities:** online lyrics/artwork, AI services, remote libraries, LAN sync/listening party, Cast, Android Auto, advanced audio routing, video support.
- **P3 — Experimental research:** beat matching, vocal removal, true bit-perfect/exclusive output, complex visualizers, advanced audio analysis. Only ship when validated.

Do not start P2/P3 while P0 functionality is broken.

---

# 03 — Namida feature expansion requirements

The following capabilities are requested from Namida's published feature set and documentation. Implement the local/offline-compatible parts as core features. Implement online and platform-specific parts only under the capability and privacy rules above.

## 3.1 Advanced library indexer

Add or enhance:

- Configurable scan roots: internal storage, removable SD card and user-selected SAF folders.
- Folder include and exclude rules.
- Duplicate-track detection using stable identifiers and layered matching.
- Minimum file-size filter.
- Minimum duration filter.
- Optional maximum duration filter.
- Exclude notification sounds, alarms, ringtones, voice notes or other categories by configurable rules.
- Hidden-file and hidden-folder handling with a clear default.
- Scan progress, current source, discovered count, updated count, skipped count, unreadable count and duplicate count.
- Recoverable scanning: interrupted or failed scans must not incorrectly mark the entire library missing.
- Per-source scan status and last successful scan.
- Rescan one folder/source without rescanning everything.
- Manual refresh and automatic refresh policy.
- SD-card removal and reinsertion handling.
- Stable identity across MediaStore, SAF and copied-file sources.
- A Missing/Unavailable filter that does not delete metadata, favourites or playlists.
- Explainable duplicate resolution: never silently discard one file because titles happen to match.
- Configurable filename-to-tag fallback when embedded metadata is missing.
- Configurable artist delimiters and smart parsing of multiple artists.
- Album Artist grouping distinct from track Artist.
- Extract featured artists such as feat., ft. and featuring into artist relationships without corrupting the original title.
- Artist and genre separator preferences.
- Sort by title, artist, album, album artist, genre, year, track number, disc number, duration, date added, date modified, play count, last played, rating, file size and folder.
- Ascending/descending order and stable tie-breaking.
- Persist sorting independently per library page.
- Folder cover discovery using `cover.jpg`, `folder.jpg` and supported embedded artwork.
- Optional folder information from `.info.txt`, with safe plain-text parsing only.
- Library counts must always agree with visible filters and current permission scope.

## 3.2 Expanded library pages

Add the following browse destinations while preserving all current tabs:

- Genres.
- Queues / saved queue sessions.
- Most Played.
- Moods.
- Tags.
- Ratings.
- Languages.
- Statistics.
- Recently played.
- Recently added.
- Missing songs.
- Optional Videos page only if local video support is intentionally implemented.

Every page needs:

- A clear title and count based on real data.
- Search/filter and sorting where useful.
- Empty, loading, permission-denied and error states.
- Consistent track rows and selection behavior.
- Contextual actions.
- Navigation back to the source context.
- Support for multi-select and global selection where appropriate.

Do not force every page into the top tab row. Use configurable navigation destinations or a Library section so the primary navigation remains uncluttered.

## 3.3 Metadata and Tag Editor

Add a real metadata editor, subject to Android storage access and format support:

- View all readable tags for a track.
- Edit title, artist, album, album artist, genre, year, track number, disc number, composer, lyricist, comment, grouping, mood, rating and supported custom fields.
- Edit embedded cover artwork where supported.
- Batch edit selected tracks.
- Preview changes before writing.
- Validate values and preserve unknown tags where the library permits.
- Show file format and which fields are writable.
- Clearly distinguish metadata stored in the Pulse Player database from tags actually written to the audio file.
- Confirm before writing to the original file.
- Never modify files automatically during scanning.
- Keep a recoverable before/after metadata snapshot where technically possible.
- Handle read-only, unsupported, protected and SAF documents with clear explanations.
- Use TagLib or another appropriately licensed and maintained library only after checking Android compatibility and license obligations.
- If a format cannot be safely written, show read-only metadata and explain the limitation instead of pretending the save succeeded.

## 3.4 Smart playlists and collections

Add rule-based smart playlists with:

- All/any rule grouping.
- Conditions for artist, album, album artist, genre, year, duration, rating, mood, tags, folder, date added, date played, play count, last played, favourite status and file availability.
- Numeric ranges and text contains/equals/starts-with conditions.
- Include/exclude rules.
- Sort order and result limit.
- Live updates when library metadata or listening history changes.
- Search within smart playlist results.
- Save, duplicate, rename, edit and delete.
- Convert a smart playlist to a static playlist.
- Show why a track matches, where useful.
- Rule validation and a useful empty state.

Provide built-in local smart collections such as:

- Recently added.
- Recently played.
- Most played.
- Never played.
- Short tracks.
- Long tracks.
- High-rated tracks.
- Favourites.
- Underrated tracks.
- Forgotten tracks.
- Recently rediscovered.
- Tracks from this day in previous years.

Do not label deterministic local recommendations as AI. Use **Smart Mix** or **Daily Mix** for local algorithmic playlists unless a real AI provider is connected and disclosed.

## 3.5 Local recommendation features

Implement a local recommendation engine using only the user's on-device library and play history:

- Smart Tracks: tracks frequently listened to alongside the current track.
- Similar tracks based on shared artists, albums, genres, tags, moods, ratings and listening patterns.
- Daily Mix generated from actual user behavior.
- Underrated tracks: low play count but positive rating or meaningful listening history.
- Lost Memories: tracks listened to around the same date in previous years.
- Release-year, mood, genre, time-range and rating-based mixes.
- Random discovery with user-configurable exclusions.
- Explain recommendation reasons in a simple “Why this track?” detail.
- Exclude unavailable tracks and respect explicit folder/library filters.
- Do not invent recommendations when the library/history is too small. Show a clear message explaining what data is needed.
- Provide a setting to disable personalization and clear recommendation-derived data.

## 3.6 Advanced queue and playback modes

Expand queue behavior with:

- Persistent named queue sessions.
- Save current queue, restore, rename and delete saved sessions.
- Drag-and-drop reorder with accessible move-up/move-down alternatives.
- Move to top/bottom.
- Play next.
- Add to queue.
- Insert after the most recently inserted item.
- Add multiple selected tracks in their selected order.
- Remove one or multiple tracks.
- Clear queue with confirmation.
- Scroll to current track.
- Shuffle that preserves a restorable base order.
- Repeat Off / All / One.
- Repeat current track N times before advancing.
- Play mode options:
  - Selected track only.
  - Current search results.
  - Current playlist.
  - Current album.
  - Current artist.
  - Current genre.
  - Current folder.
- Queue state must survive process death safely and restore paused.
- Queue operations must not restart the current song unless the user explicitly selects a new track.
- Queue state must have one source of truth. Do not enable Media3's independent shuffle if the project uses a separate queue-order model.

## 3.7 History and listening statistics

Provide editable, transparent listening history:

- Track play events with timestamps and actual listened duration.
- Configurable minimum seconds or percentage required to count a play.
- Ignore paused and buffering time.
- Account for playback speed so statistics can distinguish media duration and real elapsed listening time.
- Avoid double-counting repeat-one loops or process recovery.
- Allow users to inspect, remove or import history records.
- Clearly explain that removing a history record does not delete the audio file.
- Daily, weekly, monthly, yearly and custom-range summaries.
- Total listening time, sessions, unique tracks, top songs, artists, albums, genres and folders.
- Listening streaks only when derived from actual records.
- Monthly recap and Your Year page.
- Charts with accessible textual summaries.
- Shareable recap cards generated locally.
- Optional privacy setting to hide listening statistics from widgets/lock-screen surfaces.
- Time-zone-safe aggregation and month boundaries.
- No fake statistics or seeded demo records.

## 3.8 Search improvements

Expand search to cover:

- Track title.
- Artist and featured artist.
- Album and album artist.
- Filename.
- Folder.
- Genre.
- Tags and mood.
- Lyrics when lyrics are locally available or explicitly cached.
- Playlist names and contents.
- Ratings and metadata fields where useful.

Support:

- Typo tolerance and ranked results.
- Normalization of case, accents and punctuation.
- Configurable search scope.
- Recent searches, clearable by the user.
- Search filters and sort.
- Search inside albums, artists, genres, playlists and folders.
- Search-to-play behavior that respects the selected Track Play Mode.
- A clear no-results state with suggestions to broaden the search.
- Debouncing without making the UI feel delayed.

## 3.9 Lyrics

Support local-first lyrics:

- Read adjacent `.lrc` and `.txt` files.
- Import a user-selected LRC/TXT file using SAF.
- Match lyric files safely to a track.
- Display plain lyrics.
- Display timestamp-synced LRC lyrics.
- Support word-synced formats such as TTML only if a reliable parser is implemented.
- Scroll synchronized lyrics and highlight the active line.
- Tap a lyric line to seek when timestamps are available.
- Manual lyric offset adjustment.
- Lyrics editor for text and timestamps.
- Create, edit, save, export and remove sidecar lyrics.
- Preserve original lyric files unless the user explicitly overwrites them.
- Handle missing, malformed, unsupported and empty lyrics.
- Lyrics must remain readable with every player theme and color mode.

Optional online lyrics lookup through LRCLIB or another provider must be disabled by default, require explicit consent and disclose the provider. If the build remains internet-permission-free, provide local file import only.

## 3.10 Sleep timer and pause scenarios

Extend the existing timer with:

- End of current track.
- End of current playlist/queue.
- After N tracks.
- After a selected duration.
- Fade-out duration.
- Pause when audio output reaches zero, if reliable and user-enabled.
- Pause on headset/Bluetooth disconnect.
- Pause on incoming call/audio focus loss.
- Configurable behavior after notification interruptions.
- Show remaining time in the player and notification where supported.
- Allow cancel, edit and resume.
- Prevent duplicate timers.
- Recover safely after process death.
- Never resume playback unexpectedly after a call or timer unless the user's chosen behavior explicitly allows it.

## 3.11 Audio and sound capabilities

Build a capability-aware audio settings area containing:

- Standard equalizer presets.
- Custom equalizer presets.
- Parametric EQ if the audio engine/device supports it.
- AutoEQ profile import only when a valid supported profile format is available.
- Bass enhancement.
- Reverb.
- Virtualizer / surround effect.
- Mono audio.
- Output-device information and routing where Android permits.
- Audio path viewer showing the known stages and clearly identifying unavailable/unknown stages.
- Playback speed.
- Pitch adjustment, including semitone controls.
- Optional 432 Hz tuning mode with clear explanation of what it changes.
- Loudness normalization / ReplayGain where supported.
- Per-track sound profile: EQ, volume, speed and pitch.
- Save, rename, duplicate and reset sound presets.
- Reset to safe defaults.
- Clipping/distortion warning for extreme EQ or gain settings.
- Device capability detection and lifecycle-safe effect release/recreation.

Important audio honesty:

- Do not promise bit-perfect output unless the actual playback path has been verified to preserve the source stream without resampling or DSP.
- Do not claim USB DAC exclusive mode or direct hardware access unless the Android device/API/driver and implementation genuinely support it.
- Do not claim 32-bit float output simply because an internal buffer uses float samples.
- Do not promise gapless playback, crossfade or beat matching on every device/codec. Test the actual implementation.
- Advanced effects such as vocal removal and 8D audio must be labelled experimental if they are not true, robust implementations. Never present a simple stereo pan effect as professional vocal removal.
- Unsupported controls must be hidden or disabled with a reason.

## 3.12 Visualizers and artwork interactions

Add optional, battery-aware visual features:

- Audio-reactive visualizer modes.
- Minimal waveform / spectrum / bars / circular visualizer styles.
- Animated background modes.
- Procedural wallpapers such as nature, ocean, stars, mesh and abstract gradients.
- Artwork particles or thumbnail motion reacting to audio intensity where reliable.
- Mini-player Party Mode with edge breathing effect.
- Artwork tap/long-press gestures.
- Artwork gesture customization.
- Static mode for battery saver.
- Reduced-motion compliance.
- Visualizer intensity and frame-rate controls.
- Stop rendering when the player is not visible or app is backgrounded unless required by a user-visible feature.
- No copyrighted wallpapers or scraped images bundled without permission.

## 3.13 Import/export and external history

Support local playlist interchange:

- Import/export M3U and M3U8 where practical.
- Preserve playlist order.
- Report missing paths and unmatched tracks.
- Allow the user to preview import results.
- Avoid overwriting existing playlists without confirmation.
- Support playlist artwork only when the format/source supports it.

Optional import of Spotify, Last.fm, ListenBrainz or YouTube history must be through user-selected export files or supported documented APIs. Do not ask for passwords or scrape private accounts. Show import preview, duplicate handling and a report of records that could not be matched. Keep this outside the offline core.

---

# 04 — PixelPlayer feature and design expansion requirements

## 4.1 Material You / Material 3 Expressive design

Add a new optional design family inspired by PixelPlayer's modern Material You approach:

- Wallpaper-derived dynamic color on supported Android versions.
- Album-art-derived accent palette.
- Manual accent color.
- Separate light and dark palettes.
- AMOLED true-black mode.
- Automatic theme switching based on system theme and optional schedule.
- Adjustable corner radius.
- Adjustable navigation-bar appearance.
- Consistent component shapes and elevation.
- Expressive but restrained transitions.
- Animated state changes and button micro-interactions.
- Consistent pressed, focused, selected, disabled and loading states.
- Accessible color contrast and touch targets.

Do not replace the existing Pulse palette. Add the new design family as selectable themes/skins. Keep the existing plum, cyan, violet, magenta, orange and other original color families.

## 4.2 PixelPlayer-style library organization

Add or refine:

- Multi-artist parsing with user-configurable delimiters.
- Album Artist grouping.
- Folder selection and exclusion.
- Full-text search.
- Multi-format local playback based on actual decoder support.
- Playlist management.
- Listening statistics.
- Local lyric display and editing.
- Artist artwork cache and fallback.
- LRU memory cache plus persistent cache where appropriate.
- Cache size limit and clear-cache action.
- Cache invalidation when artwork or metadata changes.

Online Deezer artist artwork lookup is optional and must not be required for artist pages. Local embedded artwork and cached artwork must continue to work offline.

## 4.3 Playback engine and media session

Preserve the current Media3 playback architecture if it is stable. Evaluate FFmpeg extension support only after checking:

- Current Media3 version compatibility.
- Supported Android ABIs.
- Decoder licensing and redistribution requirements.
- APK size and native-library impact.
- Security and malformed-media handling.
- Actual formats the extension adds beyond platform decoders.

Provide robust support for common formats such as MP3, FLAC, AAC/M4A, OGG/Opus and WAV where the installed decoder stack supports them. APE, MIDI and other less common formats must be capability-tested; never advertise universal format support without proof.

Maintain:

- Background playback.
- MediaSession.
- Notification controls.
- Lock-screen metadata.
- Queue reorder.
- Shuffle/repeat.
- Gapless playback where validated.
- Configurable transitions only where technically supported.
- Audio focus and noisy-output handling.
- Service reconnect and process-death recovery.

## 4.4 Optional AI playlist generation

Provide a provider-neutral design for AI playlist generation, but do not make the app dependent on a specific provider.

Requirements:

- Local playlist generation must remain available without AI.
- AI providers are optional and individually configured.
- Support provider selection only for providers whose current API and terms have been checked.
- API keys must be stored securely and never embedded in source code, logs, exported backups or crash messages.
- Explain what track metadata is sent to the provider.
- Send the minimum necessary metadata.
- Never upload audio files unless a separate explicit feature and consent are introduced.
- Provide prompt-based playlist intent such as “calm evening”, “workout”, “similar to this track” or “songs I have not heard recently”.
- Validate provider output against the local library; only add tracks that actually exist locally unless the user explicitly requests another source.
- Handle invalid IDs, duplicate results, unavailable tracks, rate limits, timeouts, provider errors and quota exhaustion.
- Provide a local fallback and clearly label AI-generated results.
- Include a “Delete API key” and “Clear AI history” action.
- AI functionality must be disabled in Offline-only mode.

## 4.5 Optional connectivity and device integrations

Evaluate and implement only after core app stability:

- Google Cast / Chromecast.
- Android Auto.
- Wear OS companion controls.
- Home-screen widgets via Glance.
- Optional status-bar lyrics if Android version and platform policy allow it.
- Optional external scrobbling integration.
- Optional remote libraries: Subsonic/Navidrome, Jellyfin, WebDAV and SMB.
- Optional LAN device sync.
- Optional LAN listening party.

Every integration needs:

- Dedicated setup and status screen.
- Clear permission and network explanation.
- Connection test.
- Authentication stored securely when applicable.
- Disconnect/revoke action.
- Retry and timeout policy.
- Offline fallback.
- No mandatory account.
- No silent background upload.
- No network calls when disabled or Offline-only mode is active.

Do not implement YouTube scraping, undocumented endpoints, DRM circumvention or other unstable/legally questionable integrations. Any YouTube-related feature must use an authorized, documented and policy-compliant source, or remain out of scope.

---

# 05 — New player layout: Atmospheric Queue

Add a new selectable player layout called **Atmospheric Queue**. It is a new layout, not a replacement for the existing nine themes.

## 5.1 Visual concept

A two-part Now Playing screen:

- Upper section: dark atmospheric forest/mist appearance with a layered background and a dark scrim.
- Lower section: light/off-white “Up Next” queue surface with rounded top corners.
- Clear contrast between the dark playback area and light queue area.
- Premium, calm, cinematic appearance.
- Track artwork and title remain the primary focus.
- No stock/copyrighted background image dependency. Prefer a lightweight procedural or generated-in-app mist treatment, with a static gradient fallback.

## 5.2 Upper playback section

Include:

- Collapse/down chevron.
- Song / Lyrics selector.
- Theme picker entry.
- Live button-layout picker entry.
- More options.
- Current track title and artist/channel.
- Album/track metadata when space allows.
- Progress slider with elapsed and total time.
- Shuffle.
- Previous.
- Large central play/pause control.
- Next.
- Repeat.
- Favourite.
- Sleep timer.
- Add to playlist.
- Queue jump.
- Equalizer.

Controls must connect to the existing playback state and actions. The displayed times, track title, artist and current state must be real.

## 5.3 Lower queue section

Include:

- “Up Next” heading.
- Accurate queue count.
- Scrollable queue.
- Artwork thumbnail or fallback.
- Active track highlight.
- Animated equalizer bars for current track only.
- Track title and artist.
- Per-track options.
- Tap-to-play.
- Drag-to-reorder if supported by the shared queue model.
- Remove and Play next actions.
- Empty queue state.
- Current-track-not-in-queue state if the model can represent it.

The active amber/gold accent may use `#D4AF37`, but must remain readable in both theme modes. The light queue card may be a defining style of this layout; all other screens and themes must still follow the selected app theme.

## 5.4 Responsive behavior

- Compact portrait: split layout with carefully measured upper and lower sections.
- Short screen / landscape: collapse artwork/background detail before hiding essential playback controls.
- Tablet / unfolded: use a two-column composition where playback occupies one panel and queue/lyrics the other.
- Large text: allow vertical scrolling or compact mode; never clip essential controls.
- Respect display cutout, status/navigation bars, gesture insets and rounded corners.
- All controls meet 48dp touch target.
- Reduced-motion mode disables continuous fog, equalizer and decorative animations without disabling playback.

## 5.5 Integration

- Add the layout to the existing layout registry/picker.
- Keep the nine existing themes selectable.
- Persist the user's choice.
- Provide an accurate preview.
- Reuse the same Now Playing state, queue model and playback controller.
- Do not create a second player or duplicate queue state.
- Add UI and state tests for layout switching, queue updates, theme changes and playback continuity.

---

# 06 — Live playback button arrangement switcher

Add a live customization feature that changes the arrangement of playback controls and secondary quick actions while a track is playing.

## 6.1 Preset arrangements

Provide at least 12 distinct, genuinely different arrangements:

1. Centered — primary play/pause centered, transport controls balanced around it.
2. Split — transport controls separated into left and right groups.
3. Minimal — essential transport controls only; secondary actions in an overflow/expand area.
4. Waveform Below — progress/waveform occupies its own row below the primary controls.
5. Queue Inline — compact queue preview integrated into the control area.
6. Expanded — larger controls and visible secondary actions.
7. Compact — reduced vertical footprint for small screens.
8. Floating — visually elevated central play/pause control.
9. Symmetric — mirrored left/right control placement.
10. Asymmetric — primary action emphasized with secondary controls offset.
11. Two-row — transport row plus secondary action row.
12. Radial-inspired — circular visual arrangement adapted to touch accessibility.

Add further presets only if each has a clear visual and interaction difference.

## 6.2 Behavior

- Switch from Now Playing without stopping or restarting music.
- Open the picker from the player itself.
- Open the same picker from Settings → Player controls / Layouts.
- Preview each arrangement with the current track state.
- Apply immediately or explicitly confirm, depending on existing design conventions.
- Persist selected arrangement.
- Restore it after app restart.
- Allow separate portrait and landscape choices if practical.
- Allow per-player-theme override only if it does not make settings confusing.
- Animate transitions smoothly.
- Respect reduced motion.
- Maintain consistent control semantics and accessibility labels.
- Do not move or remove the underlying actions; only change presentation and grouping.
- Provide Reset to default.

---

# 07 — Additional player and library quality features

Add the following high-value refinements where they do not conflict with existing behavior.

## 7.1 Track details and metadata insight

A comprehensive Track Details sheet should show:

- Title, artist, album and album artist.
- Duration, file size, format and bitrate/sample rate when available.
- Track/disc number and year.
- Genre, composer and other available tags.
- File location/source.
- Date added and modified when available.
- Play count and last played.
- Favourite state.
- Availability status.
- Audio capabilities and unsupported-format explanation.
- Artwork source.
- Lyrics availability.
- File actions supported by the granted storage access.

Never invent missing metadata. Distinguish “unknown” from zero or empty.

## 7.2 Track actions and gestures

Provide configurable gestures:

- Swipe left/right on a track row.
- Long press for selection or contextual actions.
- Artwork tap and long press.
- Mini-player swipe/expand/collapse.
- Queue item drag reorder.

Let the user choose the actions for swipe directions where practical. Provide a visible alternative for every gesture so functionality is not gesture-only.

## 7.3 Playlist management

Support:

- Static playlists.
- Smart playlists.
- Custom order.
- Drag reorder.
- Playlist artwork.
- Playlist description.
- Rename, duplicate, delete.
- Add/remove tracks.
- Import/export M3U/M3U8.
- Merge playlists with preview.
- Duplicate-track handling.
- Empty and missing-track states.
- System playlists based on real data.

## 7.4 Home screen widget and quick settings

Preserve the current responsive Glance widget and improve it:

- 2×2, 4×1 and 4×2 responsive sizes.
- Artwork, title, artist and play/pause.
- Previous/next where space allows.
- Favourite if supported.
- Tap opens Now Playing.
- Proper updates when track changes.
- No excessive polling.
- Correct behavior after reboot and process death.

Optional quick settings tiles:

- Sleep timer.
- Equalizer.
- Favourites.
- Today's listening.
- Offline-only mode, if the build includes online integrations.

Tiles must be added by the user through Android's system UI. Do not pretend to auto-add them.

## 7.5 Notifications and lock screen

- Correct metadata and artwork.
- Play/pause, previous, next.
- Optional favourite action if Android/media-session support allows it.
- Stable behavior on Android 14+ foreground service rules and newer versions.
- Notification permission requested at the appropriate time.
- Do not request notification permission just to scan the library.
- Correct behavior when notification permission is denied; playback must still work where platform rules permit.

---

# 08 — Design system, themes, skins and layout registry

## 8.1 Separate concepts clearly

Do not mix these terms:

- **App theme:** global colors, typography, surfaces, shapes and component styling.
- **Skin:** a coordinated visual package that may include colors, gradients, backgrounds, icons and decorative treatments.
- **Player theme:** a complete Now Playing visual presentation.
- **Player layout:** the arrangement of player content and controls.
- **Button arrangement:** the placement of playback and quick-action buttons within a player layout.
- **Wallpaper/background:** a background treatment used by supported screens.

Each picker must use the correct category and explain what it changes.

## 8.2 New theme families to add

In addition to every existing theme, provide selectable choices for:

- Material You — wallpaper dynamic color.
- Artwork Dynamic — current album-art palette.
- Expressive Material — PixelPlayer-inspired expressive surfaces and motion.
- AMOLED Pure Black.
- Dark Plum.
- Light Warm.
- Ocean.
- Forest.
- Aurora.
- Mesh.
- Monochrome.
- Custom accent theme.

Do not duplicate themes that are already present. Map new options to existing equivalent IDs where appropriate and preserve all stored user selections.

## 8.3 Design tokens

Use a single consistent token system for:

- Backgrounds.
- Primary/secondary/tertiary surfaces.
- Text primary/secondary/disabled.
- Accent colors.
- Error/warning/success states.
- Dividers and outlines.
- Scrims and overlays.
- Playback active/inactive.
- Queue current/next states.
- Focus/pressed/selected states.
- Cards, sheets, buttons and navigation.

No screen should hardcode a color that breaks when the user changes theme. Intentional player-theme accents may be locally defined but must be contrast-tested and adapted to the background.

## 8.4 Global visual audit

Audit every:

- Screen.
- Dialog.
- Bottom sheet.
- Snackbar.
- Dropdown.
- Context menu.
- Widget.
- Notification surface where app-controlled.
- Empty state.
- Loading state.
- Disabled control.
- Selected item.
- Player theme.
- Queue item.
- Search field.
- Slider and switch.

Test all of them in Light, Dark, AMOLED, System, Dynamic and every custom skin.

## 8.5 Customization settings

Add user controls for:

- Corner radius.
- Artwork corner radius where appropriate.
- Navigation style.
- App accent.
- Dynamic wallpaper color.
- Dynamic album-art color.
- Gradient/background selection.
- Player background dimming.
- Artwork blur intensity.
- Player control size.
- Player button arrangement.
- Mini-player style.
- Animation intensity.
- Reduced motion.
- Visualizer mode and intensity.
- List density.
- Album grid density.
- Optional font scale override only if it respects system accessibility scale.

Every customization must have a preview, a reset action and persistent settings.

---

# 09 — Classic and Full backup: preserve and improve without mixing

There are two existing backup systems. They are separate products and must remain separate.

## 9.1 Classic favourites JSON backup

Preserve the exact `pulseplayer.favorites` v1 JSON format and existing fuzzy-matching behavior.

The requested enhancement is:

- Use `Music/Pulse Player/` as the suggested destination.
- Export favourites metadata to JSON.
- Copy each favourite audio file into that folder only after explicit user confirmation.
- Never move or delete the original audio.
- Avoid overwriting same-named files without a safe conflict-resolution flow.
- Report copied, skipped, missing, duplicate and failed files.
- Keep JSON and audio-copy operations recoverable.
- Import from a user-selected folder/document tree or selected JSON file.
- Preview matching results before applying.
- Support Merge and Replace only if the existing Classic specification supports those modes; do not silently alter v1 semantics.
- Match by exact stable identity/path first, then carefully ranked metadata matching.
- Never auto-select an ambiguous match.
- Report unmatched items.
- Handle partial copy, storage full, permission denial, corrupt JSON and unreadable files.

Do not change the v1 schema or make Classic backup import the Full backup `.wav` format.

## 9.2 Full backup — reference format

Preserve the existing Full backup reference-format design based on the user's sample `.wav` file, which is a ZIP archive renamed with a `.wav` extension.

Preserve the current rules:

- Separate screen/card and codec from Classic favourites backup.
- Import using SAF document picker with broad file selection.
- Detect file content by signature, not extension alone.
- Reject actual WAV audio as a backup.
- Validate ZIP structure and entry names safely.
- Never extract path-traversal entries.
- Enforce archive size, entry count, per-entry size and total decompressed size limits.
- Never open a database supplied inside the archive as a live database.
- Stage data, validate it, preview the changes and only then apply.
- Merge is the default restore behavior.
- Replace requires explicit confirmation.
- Apply changes transactionally and roll back on failure.
- Keep the sample's schema and field mapping exact where already specified.
- Do not invent a non-empty lyrics schema when the sample format does not define one.
- Never trust source-device artwork content URIs on another device.
- Preserve idempotent repeated imports.
- Keep user sample backups out of public Git repositories.
- State honestly that import of the reference format is supported by the specification, while acceptance of exported files by the other application remains unverified unless actually tested.

UI wording must be **“Full backup (reference format)”**. Do not use the word “compatible” as a user-facing promise.

## 9.3 Backup improvements

Add:

- Last successful backup details.
- File name, date/time, size and category counts.
- Export verification before recording a backup as successful.
- Restore preview with new, existing, skipped, unmatched and conflict counts.
- Clear error and recovery messages.
- Progress and cancellation where safe.
- Temporary-file cleanup.
- Free-space checks.
- Optional encryption only as a separately designed future capability with documented key recovery. Never add fake encryption or hard-code a key.
- Backup settings must not be included in a way that leaks API keys, credentials or secrets.

---

# 10 — Permissions, privacy and security

## 10.1 Do not request every permission on first launch

The old merge request says to request all permissions at first app open. Replace that behavior with a **just-in-time permission strategy**. Asking for every permission before the user understands the app creates a poor experience and may cause unnecessary denial.

On first launch:

1. Explain that Pulse Player needs access to audio files to build the local library.
2. Request the minimum media permission required for the Android version.
3. If the user denies it, show a useful permission state with Scan/Choose folder or Open Settings actions as appropriate.
4. Request notifications when the user starts or enables a feature that needs them, following Android platform rules.
5. Request Bluetooth or nearby-device permissions only when a specific feature needs them.
6. Request ringtone modification access only after the user selects Set as ringtone.
7. Request local-network access only when the user enables LAN sync/listening party.
8. Request network access only if optional online integrations are part of the selected build and the user enables them.
9. Do not request broad file-management access if MediaStore and SAF are sufficient.
10. Do not request contacts, location, microphone, camera or unrelated permissions for ordinary music playback.

## 10.2 Permission transparency

For every permission:

- Explain what it is used for.
- Explain whether it is optional.
- Explain what still works if denied.
- Provide a route to Android Settings if permanently denied.
- Monitor permission revocation while the app is running.
- Do not repeatedly prompt after denial.
- Do not block offline playback because an optional integration permission is missing.

## 10.3 Privacy controls

Add a Privacy section containing:

- Offline-only mode.
- Online integrations enabled/disabled state.
- Per-provider connection status.
- Clear online cache.
- Clear search history.
- Clear listening history.
- Clear recommendation data.
- Clear artwork cache.
- Export personal data.
- Disconnect service / remove credentials.
- Privacy summary explaining what stays on device and what is transmitted when optional services are enabled.

Do not include secrets in logs, analytics, crash reports, backups or share sheets.

---

# 11 — Responsiveness, accessibility and usability

Support:

- Small phones.
- Standard phones.
- Large phones.
- Foldables and unfolded layouts.
- Tablets.
- Landscape phones/tablets.
- Split-screen and multi-window where possible.
- Large system font sizes.
- Display scaling.
- Edge-to-edge system bars.
- Punch-hole/camera cutouts.
- Gesture and three-button navigation.

Test a representative width range from compact phone through tablet and a font-scale range up to 2.0.

Rules:

- No overlapping text, icons, buttons or cards.
- Do not shrink text below readable sizes to force a layout to fit.
- Use ellipsis only when the full text is available in a details view or accessible description.
- Prioritize essential playback controls.
- Hide or move secondary actions into an overflow area on narrow layouts.
- Ensure artwork remains correctly proportioned.
- Provide content descriptions and state descriptions.
- Support TalkBack navigation order.
- Do not use color as the only indicator of state.
- Provide keyboard/focus behavior for supported external keyboards.
- Provide accessible alternatives to swipe and drag gestures.
- Respect reduced motion and system animation scale.
- Maintain adequate contrast in every skin and player theme.

---

# 12 — Performance and stability

Optimize without sacrificing correctness:

- Fast cold start.
- No full-library query on the main thread.
- Indexed Room queries for common filters and sorting.
- Stable keys in lazy lists.
- Avoid whole-screen recomposition from seek progress.
- Throttle progress updates appropriately.
- Bounded artwork and blur caches.
- Cache keys must include stable identity and artwork modification/version information.
- Cancel obsolete image loads.
- Release audio effects and media resources correctly.
- Avoid repeated database writes for every progress tick.
- Avoid excessive widget refreshes.
- Avoid infinite animations when the relevant UI is not visible.
- Use battery-aware visualizer frame rates.
- Recover from service disconnects.
- Handle process death and app restart.
- Avoid duplicate playback services or audio sessions.
- Handle storage pressure and low-memory callbacks.
- Do not run background scans without a clear user-visible reason.
- Make scan and backup operations cancellable when safe.
- Avoid battery optimization exemption requests unless a real, user-visible requirement justifies them.

---

# 13 — Error handling and recovery

Every subsystem must have an explicit error policy.

Cover:

- Permission denial/revocation.
- Empty library.
- Interrupted scan.
- Partial scan.
- Unreadable folder.
- SD-card removed.
- Missing audio file.
- Unsupported decoder/format.
- Playback service failure.
- Audio focus failure.
- Equalizer unavailable.
- USB/output device unavailable.
- Lyrics missing/corrupt.
- Artwork missing/corrupt.
- Database migration failure.
- Database corruption.
- DataStore corruption.
- Backup export failure.
- Backup import failure.
- Corrupt or hostile archive.
- Duplicate backup files.
- Insufficient storage.
- SAF permission revoked.
- Network timeout/provider failure.
- AI quota/rate limit.
- LAN pairing failure.
- Cast/Android Auto unavailable.
- Widget update failure.

Every error must provide:

1. A concise human-readable explanation.
2. The affected item or operation where safe.
3. A practical recovery action.
4. A retry option when retry is safe.
5. No destructive automatic recovery.
6. A diagnostic detail that can be copied/shared by the user without leaking secrets.

Use consistent error states and avoid exposing raw stack traces to normal users.

---

# 14 — Settings information architecture

Consolidate all features into a coherent Settings structure. Do not create hidden or orphaned settings pages.

Recommended sections:

1. **Appearance**
   - App theme, skins, wallpaper, dynamic colors, corner radius, navigation style, list density, motion.

2. **Player**
   - Player theme, player layout, live button arrangement, artwork, queue panel, mini-player, gestures.

3. **Library**
   - Scan sources, folder include/exclude, file-size/duration filters, duplicate policy, artist parsing, sorting, hidden files, missing tracks.

4. **Playback**
   - Speed, pitch, repeat, play mode, transitions, fades, headset behavior, audio focus, resume behavior.

5. **Audio**
   - Equalizer, presets, bass, reverb, virtualizer, ReplayGain, mono, output path, per-track profiles, capability information.

6. **Lyrics**
   - Local lyric discovery, import, editor, synchronization offset, optional provider settings.

7. **Playlists & Queue**
   - Smart playlists, saved queues, queue insertion behavior, M3U import/export.

8. **History & Statistics**
   - Listen-count threshold, history management, recap, Your Year, privacy controls.

9. **Widgets & System**
   - Widget guidance, quick settings tiles, Android Auto/Cast status when available.

10. **Backup & Transfer**
    - Classic favourites JSON, Full backup (reference format), import/export reports, Music/Pulse Player folder workflow.

11. **Connections**
    - Optional integrations, remote libraries, LAN sync, listening party, AI providers. Hide this section or show an explanatory unavailable state in builds that do not include network functionality.

12. **Privacy**
    - Offline-only mode, data controls, permissions, cache, credentials.

13. **About**
    - Version, licenses, acknowledgements, privacy summary, known limitations and support diagnostics.

All new features must have a discoverable UI entry point. A setting must not exist only in documentation.

---

# 15 — Documentation and repository maintenance

Update documentation to reflect the actual implemented product:

- README with feature overview, screenshots only when genuine, build instructions, permission explanation, offline/online behavior, supported formats and known limitations.
- Feature matrix with WORKING / PARTIAL / BROKEN / MISSING / UNVERIFIED.
- Architecture overview.
- Navigation map.
- Theme/skin/player-layout catalogue.
- Optional integration list and privacy implications.
- Backup and restore guide distinguishing Classic and Full backup.
- Troubleshooting and recovery guide.
- Accessibility notes.
- Supported Android versions and tested devices.
- Third-party dependency and license notices.
- `brain.md` with Done / Tested / Not done / Known limits.
- `docs/spec/SPEC_INDEX.md` updated to include this merge request and the new phase files.
- `docs/TEMP_CODE.md` maintained and empty before release.

Never add fake badges, fake screenshots, invented test counts, unverified compatibility claims or a license that has not been selected.

---

# 16 — Suggested implementation phases

Do not attempt the entire merge in one huge code-generation response. Work in controlled phases. The original project's Spec Index and fix-pack rules still apply. Add these merge phases to the index.

| Phase | Goal | Main scope |
|---|---|---|
| M0 | Audit and merge plan | Inventory, preservation matrix, gap matrix, licensing, conflicts |
| M1 | Stability baseline | Build, tests, data backup, regressions, broken existing features |
| M2 | Theme architecture | Theme tokens, registry, global color audit, Material You family |
| M3 | Player layout registry | Atmospheric Queue, layout previews, responsive player host |
| M4 | Live button arrangements | 12+ presets, persistence, animation, accessibility |
| M5 | Library/indexing | Advanced scan filters, artist parsing, genres, tags, sorting |
| M6 | Metadata and playlists | Tag editor, smart playlists, M3U, saved queue sessions |
| M7 | Search and lyrics | Typo-tolerant search, local lyrics, editor and sync |
| M8 | Audio enhancements | EQ profiles, per-track settings, ReplayGain, capability viewer |
| M9 | History and discovery | Session stats, Daily Mix, Smart Tracks, Lost Memories |
| M10 | Visual and interaction polish | Visualizers, wallpapers, gestures, widgets and quick tiles |
| M11 | Optional integrations | Online lyrics/art, AI, remote libraries, LAN, Cast/Auto where approved |
| M12 | Backup hardening | Classic copy workflow and Full reference-format verification |
| M13 | Security, accessibility and performance | Permission audit, privacy, responsive tests, recovery |
| M14 | Final acceptance | Full regression, documentation, release readiness and honest report |

Each phase must be subdivided into small tasks. A task should normally touch no more than six files and one subsystem. If a task requires more, split it and explain the dependency.

After every task:

- List changed files.
- Explain behavior added or changed.
- State tests actually run.
- State build result.
- State manual checks not yet performed.
- Update `brain.md`.
- Stop and wait for the next task if the workflow requires user approval.

---

# 17 — Verification and acceptance criteria

The merge is not complete until the following are verified against the real project.

## 17.1 Preservation

- Every existing Pulse Player feature is accounted for in the Preservation Matrix.
- Existing screens and navigation remain accessible.
- Existing nine player themes remain selectable.
- Existing skin themes and wallpapers remain selectable.
- Existing Classic backup format still imports and exports correctly.
- Existing Full backup reference format still imports correctly.
- Existing user settings and library data survive migrations.
- No duplicate playback engine, queue, service or audio session has been introduced.

## 17.2 New features

- Every approved Namida/PixelPlayer feature is classified and tracked.
- Every local-core feature has a complete workflow.
- Optional online/LAN features are disabled by default and privacy-disclosed.
- Unsupported hardware features are accurately labelled.
- New pages are reachable.
- New settings are discoverable.
- Smart playlists update from real data.
- Search returns real ranked results.
- Lyrics work with local files.
- Statistics are session-based and accurate.
- Queue persistence and ordering are stable.
- Metadata editor writes only supported fields and reports failures.
- Button arrangement picker has at least 12 working presets.
- Atmospheric Queue is selectable and functional.

## 17.3 Themes and responsiveness

- Every screen works in Light, Dark, AMOLED, System and supported Dynamic modes.
- No hardcoded color leaks remain.
- Every player theme has a correct preview and active state.
- Atmospheric Queue remains legible.
- No overlap on compact phones, large phones, foldables, tablets or landscape.
- Large font scale does not hide essential controls.
- Reduced motion disables decorative animation but preserves function.
- TalkBack labels and navigation are usable.

## 17.4 Playback and audio

- Playback continues when switching player layouts.
- Playback continues when changing themes.
- Queue reorder does not unexpectedly restart the current song.
- Shuffle/repeat state remains correct.
- Sleep timer and fades do not fight each other.
- Audio effects are released correctly.
- Unsupported formats show a useful error.
- Background service and notification controls recover after lifecycle changes.
- No volume controller bypasses the established audio transition owner.

## 17.5 Data and backup

- No destructive migration.
- Scan interruption does not mark all tracks missing.
- Missing files do not delete favourites or playlists.
- Classic JSON v1 schema remains unchanged.
- Classic copied audio files never replace originals.
- Full backup import validates before applying.
- Full backup restore is idempotent where expected.
- Corrupt archives do not mutate the database.
- Secrets are excluded from backup.
- Personal sample backup files are excluded from Git.

## 17.6 Quality gates

Run the actual project commands supported by its Gradle setup, including:

- Debug build.
- Unit tests.
- Instrumented tests where available.
- Lint/static analysis.
- Database migration tests.
- Compose UI tests.
- Backup validation and round-trip tests.
- Manual device checks on the primary Infinix phone.
- Responsive checks on emulator sizes and tablet configuration.
- Airplane-mode checks.
- Permission-denial and permission-revocation checks.
- SD-card removal/reinsertion checks.
- Low-storage and interrupted-operation checks.

The original specification mentions 137 existing unit tests. First inspect the current test suite and confirm the actual baseline count. Do not assume 137 is still correct. All existing tests must continue to pass, and new tests must be added for new functionality.

If Google AI Studio cannot run Gradle, do not claim that the build passed. Provide the exact commands for Android Studio/terminal verification and mark the build/test status **NOT RUN**.

---

# 18 — Final delivery report

At the end, provide a truthful report with these sections:

1. **Existing app audit:** what was already present and what was broken or incomplete.
2. **Preserved features:** list and status.
3. **New features implemented:** grouped by subsystem.
4. **New themes, skins and layouts:** names and descriptions.
5. **Optional integrations:** implemented, disabled, excluded or awaiting approval.
6. **Offline/privacy behavior:** exact permissions and network behavior of the delivered build.
7. **Database changes:** migrations and data-preservation evidence.
8. **Backup verification:** Classic and Full backup results separately.
9. **Tests:** exact commands and actual PASS/FAIL/NOT RUN status.
10. **Build:** exact command and actual result.
11. **Manual device checks:** completed and not completed.
12. **Known limitations:** hardware, format, API, licensing and platform limits.
13. **Changed files:** complete manifest.
14. **Remaining work:** no vague “everything else” statements.

Do not push to Git or overwrite remote history without explicit authorization. Before any commit/push, inspect the branch, status, remotes and diff. Never force-push. If the user has already explicitly instructed a push for this task, push only after all quality gates pass and report the exact commit.

---

# 19 — Master instruction to Google AI Studio

You are not being asked to create a new generic music player. You are being asked to **upgrade and merge the existing Pulse Player project**.

First inspect the current project and its specifications. Record what already exists. Preserve all working functionality and all established designs. Then compare the project with the Namida and PixelPlayer reference capabilities listed here. Add missing features as new, integrated, selectable functionality.

Do not confuse a feature list with implementation. A feature is complete only when its user workflow, data persistence, error handling, permissions, responsive UI and tests are complete.

Do not remove an existing theme to make room for a new one. Do not replace the current player with Atmospheric Queue. Add Atmospheric Queue as another choice. Do not replace existing button controls; add the live arrangement picker. Do not merge Classic and Full backup formats. Do not claim online features work in a build that has no network permission. Do not claim hardware audio capabilities without testing the real device path.

The result must feel like one coherent, premium Pulse Player application—not two apps pasted together. Keep the original Pulse Player identity while adding the strongest useful ideas from Namida and PixelPlayer.

**Start with Phase M0 only. Audit, inventory, identify conflicts and produce the plan. Do not write implementation code until the audit is complete.**
