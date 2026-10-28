# MASTER IMPLEMENTATION PROMPT
# Offline Music Player — Reference-Informed Android Recreation

**Project type:** Native Android music player  
**Reference application:** Music Player & MP3 Player by InShot Inc.  
**Reference URL:** https://play.google.com/store/apps/details?id=musicplayer.musicapps.music.mp3player  
**Primary objective:** Build a complete, stable, attractive Android music player from scratch, reproducing the reference application's verified features and visual behavior as closely as reasonably possible.

---

# 1. ROLE AND IMPLEMENTATION MANDATE

Act as a senior Android engineer, mobile UX designer, audio-engineering specialist, accessibility engineer, database architect, and QA lead.

Build an actual installable Android application, not a design prototype.

The application must provide:

- Reliable offline playback of music stored on the device.
- Automatic media discovery and indexing.
- Browsing, searching, organizing, and sharing local audio.
- Playlists, favorites, queue management, and metadata editing.
- A configurable equalizer, bass enhancement, and supported audio effects.
- A complete Now Playing interface.
- Notification, lock-screen, headset, Bluetooth, and background playback controls.
- Theme selection and a coherent visual customization system.
- Sleep timer, ringtone setting, home-screen widgets, and lyrics support.
- Listening statistics, monthly recaps, shareable recap cards, and achievement badges.
- Optional advertising and a properly implemented premium entitlement system.
- Robust permission handling, data persistence, accessibility, and automated tests.

All features must be implemented end to end. Buttons, menus, sliders, dialogs, switches, cards, and navigation items must perform their advertised actions.

Do not create fake playback, hardcoded music collections, placeholder statistics presented as real data, decorative controls without functionality, simulated subscription success, or nonfunctional settings.

## 1.1 Mandatory reference-validation rule

Before implementing the interface:

1. Inspect the supplied Google Play listing and every available reference screenshot.
2. Install and inspect the reference application on a test device when legally and practically possible.
3. Record the application version, device resolution, density, Android version, system navigation mode, and theme configuration.
4. Inspect each reachable screen, menu, dialog, player state, theme selector, equalizer state, and relevant interaction.
5. Record measurements and colors from reference screenshots. Use image comparison tools to measure layouts, bounding boxes, typography, artwork placement, and spacing.
6. Build a reference inventory and screenshot baseline before finalizing the design.
7. Label every observation as verified, inferred, or not yet verified.

Do not claim that an arbitrary palette, layout, or animation is the original application's exact design without reference evidence.

Where the original cannot be inspected, implement the explicitly specified design system below as a consistent reference-inspired fallback. Record unresolved differences in a visual parity report.

## 1.2 Intellectual-property boundaries

Recreate general functionality, user flows, and visual behavior. Do not copy proprietary source code, private APIs, logos, licensed assets, or protected illustrations without authorization.

Use original application branding and properly licensed icons and artwork. The application must not impersonate the original publisher or imply official affiliation.

---

# 2. TECHNOLOGY STACK AND ARCHITECTURE

## 2.1 Required stack

Use native Android development with Kotlin.

| Area | Required technology |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose and Material 3 components, customized to the design system |
| Design system | Compose Material 3, custom theme tokens, reusable components |
| Playback | AndroidX Media3 ExoPlayer |
| Background playback | MediaSessionService and MediaSession |
| Media discovery | MediaStore and ContentResolver |
| Persisted library data | Room |
| Small preferences | DataStore Preferences |
| Dependency injection | Hilt |
| Asynchronous work | Kotlin Coroutines and Flow |
| Lifecycle | AndroidX Lifecycle |
| Navigation | Navigation Compose |
| Artwork loading | Coil or another maintained Android image-loading library |
| Widget | Glance or RemoteViews, depending on platform requirements |
| Purchases | Google Play Billing Library |
| Advertising | A maintained Google Mobile Ads SDK, isolated behind an interface |
| Serialization | Kotlin Serialization |
| Testing | JUnit, coroutine test utilities, Compose UI tests, Android instrumentation tests |
| Build system | Gradle Kotlin DSL and version catalog |

Select stable, mutually compatible dependency versions at implementation time. Check current Android documentation before choosing versions. Do not fabricate version numbers or use obsolete APIs.

## 2.2 Application structure

Use a modular, feature-oriented structure:

app/
  navigation/
  design/
    theme/
    components/
    typography/
    icons/
  core/
    model/
    database/
    datastore/
    media/
    permissions/
    audio/
    billing/
    ads/
    analytics/
    testing/
  feature/
    library/
    songs/
    artists/
    albums/
    folders/
    search/
    playlists/
    player/
    equalizer/
    lyrics/
    themes/
    settings/
    recap/
    sleep_timer/
    ringtone/
    premium/
  playback/
    service/
    session/
    notification/
    queue/
    audio_focus/
  widget/

Adapt this structure to the scale of the actual project. Do not generate dozens of empty modules merely to make the project look sophisticated.

Use clear boundaries:

- UI renders state and forwards user actions.
- ViewModels expose observable screen state and handle UI-level events.
- Repositories coordinate persistent storage and media discovery.
- PlaybackService owns the active player and media session.
- Media3 owns playback state and audio pipeline integration.
- Room stores playlists, favorites, user edits, listening sessions, and other application-owned data.
- DataStore stores lightweight preferences.
- Billing and advertising integrations remain replaceable and independently testable.

The interface must never directly create multiple competing player instances.

## 2.3 Android compatibility

- Set minSdk to API 26 unless an explicitly tested lower-API requirement justifies a different choice.
- Set compileSdk and targetSdk to the latest stable SDK supported by the chosen dependencies and current Google Play requirements.
- Test current Android releases and at least one older supported Android version.
- Support portrait, landscape where appropriate, tablets, foldables, edge-to-edge layouts, gesture navigation, and three-button navigation.
- Respect system insets and the on-screen keyboard.
- Avoid deprecated storage APIs and unnecessary broad permissions.

The final project must compile from a clean checkout.

---

# 3. REFERENCE FEATURE MATRIX

Implement the following feature groups and verify each independently.

| Feature | Required behavior |
|---|---|
| Local audio | Discover and play accessible local audio |
| Formats | MP3, MIDI, WAV, FLAC, AAC, APE, and additional supported formats |
| Playback | Play, pause, seek, previous, next |
| Playback modes | Sequential, shuffle, repeat-one, repeat-all, repeat-off |
| Background playback | Continue when the application is backgrounded |
| Notifications | Current track, artwork, playback controls |
| Lock screen | Media controls and metadata |
| Audio effects | Equalizer, bass boost, supported reverb |
| Library | Songs, artists, albums, folders, playlists |
| Search | Search songs and related metadata |
| Favorites | Persistent favorites collection |
| Playlists | Create, rename, reorder, edit, delete |
| Metadata | Edit supported audio tags and artwork |
| Timer | Stop playback after a specified duration |
| Ringtone | Set a supported local track as a ringtone when authorized |
| Widgets | Functional home-screen playback widgets |
| Lyrics | Display available embedded or imported lyrics |
| Artwork | Full-screen artwork and Now Playing presentation |
| Themes | Color palettes, player styles, light/dark/black modes |
| Recap | Actual monthly listening statistics |
| Sharing | Export recap cards and share supported tracks |
| Achievements | Deterministic badges earned from actual activity |
| Monetization | Optional ads and verifiable premium entitlement |
| Privacy | Local-first operation and transparent network use |

A feature is complete only when its normal flow, persistence, error handling, and tests are implemented.

---

# 4. VISUAL DESIGN SYSTEM

## 4.1 Design objective

Create a refined music-player interface with clear hierarchy, immersive artwork, minimal clutter, polished transitions, readable metadata, and consistent interactive components.

Prioritize the reference application's observed layout when actual screenshots are available. Do not force the fallback measurements below onto a reference screen that clearly differs.

Avoid generic dashboard layouts, excessive cards, excessive gradients, random accent colors, oversized typography, excessive rounded containers, and decorative controls that reduce usable space.

## 4.2 Fallback visual palette

The following is a proposed implementation palette, not a claim about the reference application's original hexadecimal values.

### Dark theme

| Token | Hex | Use |
|---|---|---|
| Background | #101014 | Main screen background |
| Surface | #19191F | Elevated surfaces |
| Surface variant | #24242C | Rows, secondary containers |
| Primary | #B9A4FF | Active controls and primary accent |
| Primary pressed | #9B82F2 | Pressed controls |
| Secondary | #A5D8D6 | Secondary highlights |
| Text primary | #F7F5FC | Main text |
| Text secondary | #B7B3C3 | Supporting text |
| Divider | #34333D | Subtle separators |
| Success | #73D6A0 | Successful actions |
| Warning | #F2C879 | Cautions |
| Error | #FF8D98 | Destructive errors |
| Scrim | #000000 at 55% | Modal backgrounds |

### Light theme

| Token | Hex | Use |
|---|---|---|
| Background | #FAF9FD | Main screen background |
| Surface | #FFFFFF | Cards and toolbars |
| Surface variant | #F0EDF6 | Secondary containers |
| Primary | #6650A4 | Primary accent |
| Primary pressed | #4F3A8A | Pressed controls |
| Secondary | #426A68 | Secondary actions |
| Text primary | #1C1B22 | Main text |
| Text secondary | #625F6C | Supporting text |
| Divider | #E3DFEA | Separators |
| Success | #176B42 | Success feedback |
| Warning | #805B00 | Warning feedback |
| Error | #B3261E | Errors |

### Black / AMOLED theme

| Token | Hex | Use |
|---|---|---|
| Background | #000000 | Main background |
| Surface | #050505 | Minimal elevated regions |
| Surface variant | #111111 | Secondary containers |
| Primary | #C4B2FF | Active controls |
| Text primary | #FFFFFF | Main text |
| Text secondary | #B9B9C2 | Supporting text |
| Divider | #282828 | Subtle separators |

Implement semantic theme tokens rather than scattering hexadecimal values throughout composables.

Allow the reference-inspection phase to replace the proposed palette with measured reference values.

## 4.3 Theme engine

Support:

- System default.
- Light mode.
- Dark mode.
- True black / AMOLED mode.
- Multiple color themes.
- Multiple player presentation styles.
- Persisted user theme selection.
- Immediate preview before applying a theme.
- Restore-defaults action.
- Premium-only themes when the premium entitlement is active.
- Clear locked-theme messaging and an accessible upgrade route.

Define an extensible theme model containing:

- Theme identifier.
- Display name.
- Background, surface, primary, secondary, and text colors.
- Player artwork presentation.
- Progress bar appearance.
- Selected navigation state.
- Icon treatment.
- Premium requirement.
- Preview asset identifier, where applicable.

Do not use remotely downloaded themes as a requirement for basic functionality.

## 4.4 Typography

Use Roboto or the current system sans-serif family by default. Use a different licensed typeface only if the reference inspection demonstrates a meaningful match benefit.

Proposed typography scale:

| Role | Size | Weight |
|---|---:|---|
| Large screen title | 28sp | Bold |
| Screen title | 24sp | SemiBold |
| Section heading | 20sp | SemiBold |
| Track title | 16sp | Medium |
| Body | 14sp | Normal |
| Supporting metadata | 12sp | Normal |
| Compact labels | 11sp | Medium |
| Player title | 20sp | SemiBold |
| Player artist | 14sp | Normal |
| Timer / elapsed time | 12sp | Medium |

Use scalable text units. Verify large font settings, font padding, line wrapping, and truncation.

## 4.5 Spacing, sizing, and shapes

Use a 4dp base spacing grid.

- Tiny gap: 4dp.
- Compact gap: 8dp.
- Standard gap: 12dp.
- Section gap: 16dp.
- Large section gap: 24dp.
- Screen horizontal padding: typically 16dp.
- Standard list row: approximately 64–72dp.
- Artwork corner radius: 12–20dp depending on the verified design.
- Compact chip radius: 8–12dp.
- Modal corner radius: 24dp where consistent with platform conventions.
- Minimum touch target: 48dp by 48dp.
- Icon sizes: 20–24dp for ordinary controls and 28–32dp for prominent playback controls.

These are starting tokens, not immutable reference measurements.

Avoid excessive shadows. Use tonal elevation and restrained outlines. In the AMOLED theme, favor black surfaces and subtle separators rather than large gray shadows.

## 4.6 Icons and artwork

Use Material Symbols or a consistent licensed icon family.

Define and use one icon mapping for:

- Play, pause, previous, next.
- Shuffle and repeat.
- Favorite.
- Queue.
- More actions.
- Search.
- Equalizer.
- Playlist.
- Folder.
- Artist.
- Album.
- Lyrics.
- Timer.
- Theme.
- Settings.
- Share.
- Download or local-file actions only where relevant.
- Error, permission, and empty states.

Do not mix filled, outlined, emoji, and unrelated third-party icons arbitrarily.

Artwork rules:

1. Prefer embedded album art when present.
2. Fall back to imported artwork when available.
3. Otherwise display a designed, theme-compatible placeholder.
4. Cache artwork thumbnails efficiently.
5. Preserve aspect ratio and crop consistently.
6. Never show broken image icons for missing artwork.
7. Handle corrupted, oversized, and unsupported artwork safely.

---

# 5. NAVIGATION AND SCREEN INVENTORY

Implement the following screen destinations and secondary surfaces:

1. Home / Library.
2. All Songs.
3. Artists.
4. Artist Details.
5. Albums.
6. Album Details.
7. Folders.
8. Folder Contents.
9. Playlists.
10. Playlist Details.
11. Favorites.
12. Search.
13. Now Playing.
14. Queue.
15. Equalizer.
16. Theme Gallery.
17. Lyrics.
18. Monthly Recap.
19. Recap Details.
20. Achievements.
21. Sleep Timer.
22. Ringtone Confirmation.
23. Tag Editor.
24. Settings.
25. Premium / Subscription.
26. Permission Education.
27. First-Run Onboarding.
28. Error and recovery surfaces.

Not every destination needs a permanent navigation-bar item. Use the reference app's observed navigation model where available.

## 5.1 Navigation behavior

- Maintain a single source of truth for the active playback queue.
- Preserve playback when navigating between screens.
- Preserve meaningful scroll positions when returning to library screens.
- Support Android system Back and predictive Back where available.
- Prevent duplicate destinations from repeated taps.
- Preserve search query and filters when returning from a result.
- Open the current track in Now Playing when the mini-player is selected.
- Ensure the mini-player does not cover the last library item or navigation controls.
- Use a compact mini-player when a track is loaded.
- Hide the mini-player when no track is selected unless the reference requires a different empty-state design.

---

# 6. HOME AND MUSIC LIBRARY

## 6.1 Home screen hierarchy

Implement a library-first screen that provides immediate access to music and playback.

The screen may contain:

1. Top app bar.
2. Search action.
3. Overflow menu or settings action.
4. Library category tabs or navigation.
5. Optional recently played or favorite shortcuts.
6. Track list or selected category content.
7. Persistent mini-player.
8. Bottom navigation, if validated by the reference.
9. Reserved advertising area in the free tier, where it does not obscure controls.

Use reference screenshots to determine the actual order, selected category, toolbar dimensions, and tab placement.

## 6.2 Library categories

### Songs

Display:

- Track title.
- Artist.
- Optional album.
- Duration.
- Artwork thumbnail.
- Favorite state.
- Overflow action.

Support sorting by:

- Title.
- Artist.
- Album.
- Duration.
- Recently added.
- Recently played.
- Date modified where available.

Allow ascending and descending order where meaningful.

### Artists

Group tracks by normalized artist metadata.

Artist rows display artwork or a consistent placeholder, artist name, and available track count.

Artist Details displays the artist's tracks and available album groupings.

### Albums

Display album artwork, album title, artist, and track count.

Album Details shows the available album metadata and tracks in a sensible album-disc order.

### Folders

Display real accessible directories or logical folder groupings derived from MediaStore.

Show the folder name and track count. Opening a folder displays its available audio files.

Do not assume that filesystem paths are available for every MediaStore item. Use content URIs and document-provider URIs where appropriate.

### Playlists

Show user-created playlists, Favorites, and any additional supported playlist groupings.

Each user-created playlist supports:

- Create.
- Rename.
- Add tracks.
- Remove tracks.
- Reorder tracks.
- Play.
- Shuffle.
- Share a playlist description or supported track list.
- Delete with confirmation.

### Favorites

Favorites are persistent, user-controlled membership records. Toggling a favorite must update the UI immediately and survive application restarts.

## 6.3 Track-row interaction

- Tap a track to start or resume the expected playback queue.
- Tap the artwork only if the reference assigns a distinct action.
- Use the overflow menu for secondary actions.
- Support long-press multi-selection if implemented consistently.
- Provide accessible labels for each icon.
- Prevent a row action from accidentally triggering another row action.
- Update the current-track indicator when playback advances.
- Keep list animations short and avoid unnecessary reordering.

## 6.4 Library empty states

When no audio is found:

- Explain that no local music was discovered.
- Provide a permission or folder-selection action when relevant.
- Provide a rescan action.
- Explain supported file sources.
- Do not invent sample tracks.
- Do not show a fake track count.

When only inaccessible or unsupported files exist, distinguish the problem from an empty library.

---

# 7. MEDIA DISCOVERY AND INDEXING

## 7.1 Initial scan

After obtaining the appropriate permission:

1. Query MediaStore for accessible audio items.
2. Filter out invalid or unavailable entries.
3. Resolve metadata using MediaStore fields and, where needed, supported metadata extractors.
4. Deduplicate records.
5. Build or update the local index.
6. Extract artwork lazily.
7. Update observable library state.
8. Show scan progress only when useful.
9. Allow cancellation of expensive optional rescans.
10. Persist application-owned user metadata separately from system-derived metadata.

Use MediaStore as the primary source for shared device media.

Use the Storage Access Framework when a user explicitly chooses an additional directory or document tree.

Do not recursively scan the entire filesystem using broad storage privileges merely to simplify implementation.

## 7.2 Media inclusion rules

- Support accessible local audio files.
- Respect MediaStore visibility and user-selected folder permissions.
- Exclude items marked as non-music or ignored according to a documented policy.
- Support user-configurable folder exclusions where feasible.
- Avoid automatically including notification sounds and extremely short audio clips in music views unless the user opts in.
- Reconcile file changes, deletions, renamed tracks, and revoked URI permissions.
- Avoid retaining stale database rows after media disappears.
- Do not delete user files when removing them from the library.

## 7.3 Scanning strategy

Use background dispatchers for disk-intensive work.

- Debounce repeated media-change events.
- Use batched database operations.
- Do not decode every artwork image during startup.
- Expose scan progress through Flow.
- Handle device restarts and process death.
- Avoid repeatedly rescanning unchanged collections.
- Keep the UI responsive during large imports.
- Make error handling per-file so one malformed track does not abort the entire scan.

---

# 8. PLAYBACK ENGINE

## 8.1 Core controls

Implement:

- Play.
- Pause.
- Resume.
- Previous.
- Next.
- Seek.
- Seek forward and backward.
- Elapsed time.
- Total duration.
- Progress indicator.
- Shuffle.
- Repeat off.
- Repeat one.
- Repeat all.
- Queue management.
- Play next.
- Add to queue.
- Remove from queue.
- Clear queue with confirmation.
- Optional crossfade only if implemented correctly and exposed as a real setting.

Persist suitable playback preferences and recover gracefully from process death.

Do not persist transient decoder state as if it could always be restored. Restore a meaningful queue and position where possible.

## 8.2 Playback ordering

Sequential mode plays the queue in order.

Shuffle mode uses a randomized order without unnecessary immediate repetition. Keep the user's intended queue recoverable and maintain a separate shuffle order if required.

Repeat-one restarts the active track.

Repeat-all advances to the beginning after the final track.

Repeat-off stops after the final track.

Previous-button behavior must be predictable: when playback has progressed beyond a configurable or conventional threshold, restart the current track; otherwise move to the previous track when available.

If a queue contains a single track, handle next and previous consistently without crashes.

## 8.3 Playback formats

Required format targets:

- MP3.
- MIDI.
- WAV.
- FLAC.
- AAC.
- APE.

Also support common additional formats when the chosen playback engine and available decoders permit them.

Important implementation rule: a file extension alone does not guarantee that the device can decode a file.

- Use Media3's supported decoders for the common formats.
- Evaluate Media3 extensions or a separately maintained decoder for formats not supported by the selected configuration.
- Verify licensing and redistribution obligations before bundling native codecs.
- Treat MIDI as a distinct synthesis/playback requirement. Do not assume that every Media3 audio configuration plays MIDI files.
- If an optional codec is unavailable, show a precise unsupported-format message.
- Do not claim universal format compatibility without tests.

Test at least one valid fixture and one invalid fixture for every required format.

## 8.4 Audio quality

- Play the source audio at the quality supported by the decoder and output device.
- Avoid unnecessary resampling or lossy transcoding.
- Respect platform audio output behavior.
- Preserve channel count where supported.
- Handle variable bit rate and malformed metadata.
- Avoid claiming that a software equalizer improves source resolution.
- Do not promise bit-perfect output on Android devices without verifying the complete output path.
- Expose advanced audio controls only when supported by the active audio session.

## 8.5 Audio focus and interruptions

Handle:

- Incoming and outgoing phone calls.
- Navigation or notification audio interruptions.
- Headphone disconnection.
- Bluetooth device connection and disconnection.
- Wired headset plug and unplug.
- Another app taking audio focus.
- Audio becoming noisy.
- Audio route changes.
- Media becoming unavailable.
- User pausing from the notification shade.
- App process recreation.

Request and abandon audio focus appropriately.

Pause or duck according to interruption type and user settings.

Do not automatically resume in situations where doing so would surprise the user or conflict with platform expectations.

When headphones are disconnected, pause by default.

Keep playback in the media service, not in an Activity or composable lifecycle.

## 8.6 Background playback service

Implement a MediaSessionService with:

- A single player instance.
- Correct media session lifecycle.
- Foreground playback notification.
- Notification channel.
- Playback state propagation.
- Media metadata.
- Artwork.
- Previous, play/pause, and next actions.
- System lock-screen controls.
- Bluetooth and headset control support.
- Audio focus integration.
- Safe service shutdown.

Ensure playback continues while navigating away from the app or turning off the display.

The app must not keep a foreground service running unnecessarily when nothing is playing.

---

# 9. NOW PLAYING SCREEN

## 9.1 Layout hierarchy

Create a dedicated full-screen playback experience.

Suggested hierarchy:

1. System inset-aware top region.
2. Back or collapse control.
3. Queue or more-actions control.
4. Prominent artwork.
5. Track title.
6. Artist and album metadata.
7. Favorite action.
8. Progress slider.
9. Elapsed and remaining/total time labels.
10. Previous, play/pause, and next controls.
11. Shuffle and repeat controls.
12. Optional lyrics, equalizer, or queue entry points.
13. Optional bottom contextual actions.

Use the reference screenshot to determine exact control order, relative proportions, artwork size, background treatment, and button styles.

## 9.2 Artwork presentation

Provide:

- Default square album artwork.
- Full-screen artwork presentation.
- Correct aspect ratio.
- Smooth image loading.
- Consistent fallback artwork.
- Optional background tint derived from artwork when enabled.
- No blocking artwork decode on the main thread.

If artwork-based background colors are used, preserve text contrast and respect the selected theme.

## 9.3 Player interactions

- Tap play/pause to control the active player.
- Drag the progress slider to seek.
- Update time labels during playback.
- Avoid continuously rebuilding the whole screen every few milliseconds.
- Use throttled progress state appropriate to UI precision.
- Show shuffle and repeat selection states clearly.
- Make the active state understandable without color alone.
- Allow opening the queue.
- Allow access to supported lyrics.
- Allow opening the equalizer for the active audio session.
- Provide a functional overflow menu.

## 9.4 Mini-player

Display current artwork, title, artist, and play/pause control.

- Tap the body to open Now Playing.
- Tap play/pause to change playback state.
- Keep its state synchronized with the media session.
- Handle long track names gracefully.
- Provide a meaningful empty state or hide it when no track is loaded.
- Ensure it remains above content but below system-critical overlays.

---

# 10. EQUALIZER AND AUDIO EFFECTS

## 10.1 Equalizer screen

Provide:

- Equalizer enable/disable toggle.
- Frequency-band controls.
- Preset selection.
- Bass boost.
- Reverb or environment effect when supported.
- Reset action.
- Current audio-session status.
- Clear indication when an effect is unavailable.

Suggested preset labels:

- Normal.
- Classical.
- Dance.
- Flat.
- Folk.
- Heavy Metal.
- Hip Hop.
- Jazz.
- Pop.
- Rock.
- Custom.

The actual available presets must be derived from the active equalizer implementation. Do not display unsupported preset options as functional.

## 10.2 Frequency bands

Query the platform equalizer for the actual band count, center frequencies, and valid level range.

Display each band as an accessible slider with a frequency label, such as 60 Hz, 230 Hz, 910 Hz, 3.6 kHz, or 14 kHz, only when those frequencies match the active engine.

Do not hardcode five bands if the active effect provides a different configuration.

Persist user-defined band levels and restore them safely.

## 10.3 Bass boost and reverb

Use platform audio effects or a verified supported audio-processing implementation.

- Respect the effect's permitted strength range.
- Attach effects to the correct audio session.
- Release resources when no longer needed.
- Prevent duplicate effect instances.
- Handle effect conflicts and device-specific failures.
- Persist settings even when the effect cannot currently be activated.
- Explain when an effect is unsupported.
- Avoid applying the same effect twice through both the player and platform.

## 10.4 Equalizer acceptance criteria

- All controls read actual active effect state.
- Moving a slider changes the actual effect where supported.
- Preset selection updates the relevant bands.
- Reset restores a known valid configuration.
- Settings survive a normal restart.
- Unsupported hardware or effects produce a recoverable message.
- No UI control claims to be active when the underlying effect failed.

---

# 11. PLAYLISTS, QUEUE, AND FAVORITES

## 11.1 Playlist creation

Support a dialog or screen containing:

- Playlist name.
- Optional description.
- Optional initial track selection.
- Save and cancel actions.
- Duplicate-name validation according to a documented policy.

A newly created playlist must be stored in Room immediately after a successful save.

## 11.2 Playlist management

Support:

- Open.
- Play all.
- Shuffle all.
- Add songs.
- Remove songs.
- Reorder songs.
- Rename.
- Delete.
- Show track count and aggregate duration when calculable.

Use stable track IDs and ordered playlist membership.

Deleting a playlist must never delete the source audio files.

## 11.3 Queue management

The queue is a runtime playback structure, not simply a playlist.

- Reflect Media3's current timeline.
- Highlight the current item.
- Permit moving items where supported.
- Permit removing items.
- Allow play-next actions.
- Preserve sensible queue state during navigation.
- Avoid duplicate playback commands when the UI and media session both receive events.

---

# 12. SEARCH

## 12.1 Searchable fields

Search across:

- Track title.
- Artist.
- Album.
- Album artist.
- Genre where available.
- Folder display name.
- Playlist names.
- Relevant metadata fields.

Use case-insensitive matching and sensible whitespace normalization.

Do not require internet connectivity.

## 12.2 Search interface

Include:

- Search input.
- Clear action.
- Back navigation.
- Query persistence while navigating results.
- Results grouped by type when appropriate.
- Empty results state.
- Recent searches only if the feature is actually implemented.
- Accessible result labels.

Debounce queries to avoid excessive database work.

Opening a search result must trigger the correct playback or navigation action.

---

# 13. TAG EDITOR AND FILE METADATA

Implement an editing interface for supported metadata:

- Title.
- Artist.
- Album.
- Album artist.
- Genre.
- Track number.
- Disc number where supported.
- Year or release date where supported.
- Embedded lyrics where supported.
- Album artwork where supported.

## 13.1 Safe metadata writes

Metadata editing must respect Android storage restrictions.

1. Read metadata from the accessible source.
2. Present editable fields.
3. Validate inputs.
4. Request user consent when Android requires it for a write operation.
5. Perform the actual write using a suitable metadata library.
6. Re-read the metadata.
7. Refresh MediaStore or the relevant index as necessary.
8. Update the application database.
9. Report success only after a successful write.

Do not merely change Room metadata and claim the original file was edited.

If the selected format cannot be written safely, offer a clear limitation and retain a separate application-level override only if explicitly labeled as such.

Never overwrite an audio file without a recoverable write strategy.

---

# 14. LYRICS SUPPORT

Support lyrics from:

- Embedded metadata when available.
- A compatible local sidecar file when accessible.
- User-imported lyrics.
- User-entered lyrics, stored with an explicit ownership policy.

Do not scrape lyrics from websites without appropriate authorization.

## 14.1 Lyrics UI

Provide:

- Scrollable lyrics.
- Empty state.
- Track association.
- Optional timed-lyrics highlighting.
- Text-size support.
- High-contrast rendering.
- Clear loading and parse-error states.

For timed lyrics, parse supported timestamp formats and synchronize against actual playback position.

Avoid excessive updates that trigger unnecessary full-screen recomposition.

If lyrics are unavailable, show a useful message and available import or edit actions rather than invented lyrics.

---

# 15. SLEEP TIMER

Implement a dedicated sleep-timer interface.

Support:

- Preset durations.
- Custom duration.
- Start timer.
- Pause or resume timer where appropriate.
- Cancel timer.
- Visible remaining time.
- Optional fade-out, if implemented using a real audio-volume ramp.

At expiration:

1. Trigger the defined timer action.
2. Stop or pause playback according to the selected mode.
3. Clear the active timer state.
4. Update the interface and notification if relevant.
5. Persist only the state necessary for safe recovery.

Use lifecycle-safe timing. Do not rely on an Activity remaining alive.

If the process is killed, the timer must either recover accurately or explicitly follow a documented limitation. Never claim that a timer remains active when it has been lost.

Do not request exact alarms or battery-optimization exemptions by default. Use the least privileged mechanism that meets the product requirements.

---

# 16. SET AS RINGTONE

Provide a track action called Set as ringtone.

Implementation requirements:

- Verify that the selected item is accessible and suitable.
- Request only the permissions or user confirmation needed by the Android version and destination.
- Handle WRITE_SETTINGS special access only if required by the chosen implementation.
- Use the appropriate ringtone APIs.
- Avoid copying or overwriting protected system files.
- Handle scoped storage and URI access correctly.
- Offer a clear success or failure message.
- Provide a fallback explanation when the device manufacturer restricts the operation.

Do not report success until the ringtone operation actually succeeds.

---

# 17. HOME-SCREEN WIDGETS

Provide a functional widget, using Glance or RemoteViews as appropriate.

The widget should display:

- Current track title.
- Artist.
- Artwork or fallback image.
- Previous action if supported.
- Play/pause.
- Next action.
- A tap target that opens the app.

Requirements:

- Read current playback state from the media session or a reliable shared state layer.
- Send actual playback commands.
- Update when tracks change.
- Respect widget resize and launcher limitations.
- Avoid unnecessary periodic background work.
- Handle no active track.
- Use correct pending-intent mutability flags.
- Remain functional after process recreation.

Do not implement widget controls as decorative images.

---

# 18. MONTHLY MUSIC RECAP

This feature must use genuine, locally recorded listening activity.

Do not generate fake statistics or fill missing months with invented activity.

## 18.1 Listening history

Record listening sessions with:

- Track ID.
- Start timestamp.
- End timestamp.
- Actual listened duration.
- Session completion reason.
- Whether playback was interrupted.
- Relevant metadata snapshot when necessary to preserve historical reporting.

Accumulate listened time only while playback is genuinely active and advancing.

Do not count paused time or time spent with a stopped player.

Define a minimum meaningful listening interval to avoid inflating statistics from accidental short starts. Make this rule consistent and testable.

## 18.2 Monthly summary

Display:

- Total listening time.
- Number of distinct tracks listened to.
- Most-played songs.
- Top artists.
- Top albums.
- Listening-day count.
- Longest listening session if meaningful.
- Earned achievements.
- Month-over-month comparison only when comparable historical data exists.

Use the device's local calendar and timezone for month boundaries. Handle timezone changes without double-counting sessions.

For each statistic, define whether it uses play count, listening duration, or distinct sessions.

## 18.3 Recap screen

Create a visually rich, vertically navigable recap:

1. Month and year.
2. Listening-time highlight.
3. Top song.
4. Top artist.
5. Top album.
6. Supporting statistics.
7. Achievement cards.
8. Share recap action.

Use the actual theme system. Cards must remain readable in light, dark, and AMOLED themes.

## 18.4 Shareable recap cards

Generate a real image or shareable export containing actual statistics.

- Use Android's appropriate image rendering and sharing APIs.
- Write exports to a controlled cache or user-selected destination.
- Use a FileProvider for secure file sharing where applicable.
- Grant temporary URI permissions.
- Do not expose unrestricted file paths.
- Provide a useful fallback when the month contains no listening data.
- Do not insert advertisements into exported personal statistics without explicit product and privacy review.

## 18.5 Achievements

Define deterministic, locally calculated achievements, such as:

- First listening session.
- First hour of listening.
- Ten distinct tracks played.
- Listening on multiple days.
- Discovering several artists.
- Reaching a defined cumulative listening milestone.

Each achievement needs:

- Stable ID.
- Name.
- Description.
- Unlock threshold.
- Unlock timestamp.
- Optional artwork.
- Accessibility description.

Never award an achievement solely because a screen was opened.

---

# 19. ADS, PREMIUM, AND PURCHASES

The reference listing indicates advertising and in-app purchases. The exact subscription prices, benefits, and current premium restrictions must be verified separately.

## 19.1 Free tier

The free version should retain the essential music-player experience.

Potential ad placements:

- A reserved banner area on selected non-player library screens.
- Occasional clearly identified native placements where permitted.

Rules:

- Never interrupt playback with an interstitial ad.
- Never place an invisible ad target over navigation controls.
- Never cover track rows or playback controls.
- Never make ads resemble music-library items.
- Respect consent requirements and applicable privacy regulations.
- Provide a test-ad configuration for development.
- Do not show fake ads or simulated revenue events.

## 19.2 Premium tier

Provide a premium screen with a clear comparison between free and paid features.

Proposed premium benefits, subject to product validation:

- Remove advertisements.
- Unlock additional themes.
- Unlock additional player styles.
- Unlock optional advanced customization.

Do not place essential playback functionality behind premium unless the actual product requirements explicitly call for it.

## 19.3 Billing implementation

- Use Google Play Billing.
- Verify purchase state using the official purchase flow.
- Support purchase acknowledgement.
- Restore purchases.
- Handle pending, cancelled, expired, refunded, and failed states.
- Reconcile entitlement state at appropriate lifecycle events.
- Keep premium checks behind a dedicated entitlement interface.
- Do not treat a local boolean as proof of purchase.
- Handle offline entitlement checks conservatively and document limitations.
- Use test products and license testers during QA.
- Never hardcode a successful purchase response.

If the product is a one-time purchase rather than a subscription, model it correctly. Do not invent a recurring subscription solely because the prompt mentions premium.

---

# 20. DATA STORAGE AND DATABASE SCHEMA

Use MediaStore as the source of truth for device-managed audio metadata and Room for application-owned information.

Do not duplicate every media file or copy the entire music collection into the app's private directory.

## 20.1 Track representation

A track record should contain fields equivalent to:

- Stable application track ID.
- MediaStore ID or persisted content URI.
- Display title.
- Artist.
- Album.
- Album artist.
- Duration in milliseconds.
- MIME type.
- File size.
- Date added.
- Date modified.
- Track number.
- Disc number where available.
- Genre where available.
- Folder display name.
- Artwork reference.
- Availability status.
- Optional application-owned metadata overrides.

Prefer content URIs over raw filesystem paths.

Track identifiers must survive routine refreshes when the underlying media remains identifiable.

## 20.2 Room entities

Implement appropriate normalized entities.

### PlaylistEntity

- id: Long primary key.
- name: String.
- description: String nullable.
- createdAt: Long.
- updatedAt: Long.
- sortOrder: Int.

### PlaylistTrackEntity

- playlistId: Long.
- trackId: String.
- position: Int.
- addedAt: Long.

Use a composite primary key or a stable membership identifier. Add indexes for playlist ID, track ID, and ordering queries.

### FavoriteEntity

- trackId: String primary key.
- addedAt: Long.

### PlaybackHistoryEntity

- id: Long primary key.
- trackId: String.
- startedAt: Long.
- endedAt: Long nullable.
- listenedDurationMs: Long.
- completionReason: String.
- sessionMetadataJson: String nullable, if needed.

### ThemePreferenceEntity or DataStore keys

Persist selected theme mode, color theme, player style, and customizations.

Prefer DataStore for lightweight settings rather than creating a database entity for every preference.

### TrackOverrideEntity

- trackId: String primary key.
- titleOverride: String nullable.
- artistOverride: String nullable.
- albumOverride: String nullable.
- genreOverride: String nullable.
- artworkOverrideUri: String nullable.
- updatedAt: Long.

Use overrides only when the application cannot write source metadata or when the product explicitly supports application-only editing.

### UserSettings

Store suitable settings in DataStore:

- Theme mode.
- Color theme.
- Player style.
- Shuffle default.
- Repeat default.
- Last library category.
- Audio effect preferences.
- Sleep timer preferences.
- Library scan configuration.
- Recap preferences.
- Premium entitlement cache metadata, not purchase proof.

### AchievementEntity

- achievementId: String primary key.
- unlockedAt: Long.
- progressValue: Long.
- lastEvaluatedAt: Long.

## 20.3 Database integrity

- Add foreign keys where they make sense.
- Define deletion behavior explicitly.
- Use database migrations.
- Never drop user playlists or listening history during ordinary upgrades.
- Use transactions for multi-row playlist changes.
- Prevent duplicate favorite records.
- Handle removed tracks without deleting unrelated playlist history unnecessarily.
- Test upgrades from every supported database version.
- Provide an explicit clear-history action if appropriate.
- Explain what clearing history removes.

---

# 21. ANDROID PERMISSIONS AND PRIVACY

Request permissions just in time and explain why they are needed.

## 21.1 Media permissions

Use the permission appropriate to the supported Android version and intended media access, including READ_MEDIA_AUDIO on applicable Android versions.

For older supported versions, implement the relevant legacy media-read permission.

Use the Storage Access Framework for optional user-selected directories.

Do not request MANAGE_EXTERNAL_STORAGE for ordinary music playback.

## 21.2 Notifications and background playback

- Declare the media playback foreground-service permissions required by the target Android version.
- Use the correct foreground-service type.
- Request POST_NOTIFICATIONS on applicable Android versions when appropriate to the product experience.
- Handle notification denial without breaking all in-app playback controls.
- Follow current Android background-execution and foreground-service rules.

## 21.3 Optional permissions

Request special permissions only when the corresponding feature is invoked.

Examples include:

- System write settings for supported ringtone operations, if needed.
- User-approved access to selected document trees.
- Any additional narrowly scoped permission genuinely required by an implemented feature.

Do not request microphone, contacts, location, camera, or broad storage access without a clearly justified implemented feature.

## 21.4 Privacy model

The core music library and playback should work offline.

- Do not upload local audio files.
- Do not transmit full filesystem paths.
- Do not collect unrelated personal information.
- Keep listening history local by default.
- Make network-dependent ads and billing optional to playback.
- Document third-party SDK data practices.
- Provide clear privacy information.
- Avoid unnecessary analytics.
- Provide an option to clear local listening history where appropriate.

Test airplane mode, network failures, and unavailable ad services.

---

# 22. SETTINGS SCREEN

Organize settings into clear groups.

### Playback

- Default shuffle behavior.
- Default repeat behavior.
- Headphone-disconnect behavior.
- Resume behavior.
- Optional crossfade, if supported.
- Audio effects shortcut.

### Appearance

- System, light, dark, and AMOLED themes.
- Color theme.
- Player style.
- Artwork presentation.
- Optional animation preferences.
- Reset appearance.

### Library

- Rescan.
- Folder inclusion/exclusion.
- Hidden or ignored audio handling.
- Sorting preferences.
- Metadata refresh.
- Media indexing status.

### Lyrics and recap

- Lyrics text size.
- Recap availability.
- Listening history controls.
- Clear listening history, where provided.

### Notifications and widgets

- Widget help.
- Notification behavior where configurable.
- Lock-screen metadata behavior, within platform limitations.

### Storage and privacy

- Database or cache information where useful.
- Clear artwork cache.
- Clear local listening history.
- Privacy policy.
- Permissions management shortcuts.

### Premium

- Current entitlement.
- Available products.
- Restore purchases.
- Manage subscription when applicable.

### About

- App version.
- Open-source licenses.
- Privacy policy.
- Support route.
- Legal information.

Every setting must correspond to real application state.

---

# 23. RESPONSIVE DESIGN AND ACCESSIBILITY

## 23.1 Phone layouts

- Support compact and large phone screens.
- Avoid clipping on short displays.
- Use scrolling for content that cannot fit.
- Keep important playback controls reachable.
- Account for cutouts and system insets.
- Keep bottom actions above gesture navigation areas.
- Adapt Now Playing artwork to available height.

## 23.2 Tablet and foldable layouts

Use adaptive layouts rather than stretching the phone interface across the entire display.

Possible adaptations:

- Wider library columns.
- Side navigation or navigation rail.
- Two-pane artist and album details.
- Larger queue panels.
- Constrained artwork width.
- More visible track metadata.
- Responsive spacing based on window size classes.

Respect fold and hinge geometry. Do not place interactive controls across a separating hinge.

## 23.3 Accessibility

- Maintain accessible contrast.
- Use at least 48dp touch targets.
- Provide semantic labels for icon-only controls.
- Expose playback state to screen readers.
- Ensure keyboard and D-pad navigation where appropriate.
- Support system font scaling.
- Avoid conveying selected states through color alone.
- Provide visible focus indicators.
- Respect reduced-motion preferences where practical.
- Keep touch gestures optional when an accessible button can perform the same action.
- Test TalkBack navigation and announcement order.

---

# 24. MOTION AND MICROINTERACTIONS

Use restrained, purposeful motion.

Proposed motion tokens:

- Quick feedback: 100–150 ms.
- Standard transitions: 180–250 ms.
- Modal entry: 200–280 ms.
- Artwork transition: approximately 250–350 ms.
- Small spring-based interaction: only where it improves usability.

Suggested easing: standard Material easing curves or a documented equivalent.

Implement:

- Immediate play/pause visual feedback.
- Smooth progress movement.
- Subtle favorite-state feedback.
- Consistent dialog transitions.
- Artwork loading transitions.
- Theme previews.
- Queue reordering feedback.
- Recap-card transitions.

Do not animate every row unnecessarily.

Avoid transitions that delay playback commands. Motion must never be required to understand the state.

---

# 25. ERROR HANDLING AND EDGE CASES

Handle each of the following explicitly.

### Permission denied

Explain what the permission enables. Offer a retry or a limited-access alternative. Do not loop permission prompts.

### Empty library

Show a useful empty state and real scanning actions.

### Unsupported format

Name the file or format where safe and explain the limitation.

### Corrupted audio

Skip or report the failed item without crashing the queue.

### Deleted or moved track

Refresh the index and handle stale URIs.

### Permission revoked

Stop accessing the revoked source, update the library, and explain how to restore access.

### Duplicate tracks

Avoid accidental duplicate indexing. Do not silently merge different files merely because their titles match.

### No album artwork

Use a coherent fallback.

### Missing lyrics

Show a real empty state.

### Playback interrupted

Follow audio-focus and interruption rules.

### Bluetooth disconnection

Handle route changes and update playback state.

### Audio effect unavailable

Keep the screen functional and report the limitation.

### Database migration failure

Provide a safe recovery plan. Never silently wipe playlists or history.

### Billing unavailable

Keep the free offline player usable.

### Advertising unavailable

Reserve or collapse the ad region safely without blocking controls.

### Empty recap month

Show a truthful empty state.

### Very large music library

Paginate or use lazy lists, indexed queries, and background indexing.

### Rapid user actions

Serialize or otherwise safely coordinate player commands and playlist mutations.

### Low memory

Release artwork bitmaps, unnecessary caches, and inactive resources.

### Process death

Recover persistent preferences, queue state where feasible, and ongoing service state according to Android lifecycle rules.

---

# 26. PERFORMANCE AND BATTERY TARGETS

Use the following as engineering targets, measured on documented test hardware rather than guaranteed results across every Android device.

| Metric | Initial target |
|---|---|
| Cold launch to first interactive screen | p95 ≤ 2.5 seconds |
| Warm launch to interactive screen | p95 ≤ 1.0 second |
| Playback start after selecting an indexed local track | Typically ≤ 1.0 second for supported files on reference hardware |
| Main-thread blocking | No avoidable disk I/O or decoding |
| Large library scrolling | Smooth scrolling under normal test conditions |
| Idle playback UI updates | No unnecessary full-screen recomposition |
| Background behavior | No continuous unnecessary scanning |
| Artwork memory | Bounded thumbnail cache |
| Battery | No aggressive polling or unnecessary wake locks |
| Database access | Indexed, batched, off-main-thread |
| Media scanning | Incremental and cancellable where practical |

Measure performance using repeatable test procedures and realistic collections.

Record device model, Android version, track count, file types, cache state, and measurement methodology.

Do not claim performance targets have passed without evidence.

---

# 27. TESTING AND QUALITY ASSURANCE

## 27.1 Unit tests

Test:

- Playback-mode transitions.
- Shuffle queue behavior.
- Repeat behavior.
- Playlist ordering.
- Favorite persistence.
- Search normalization.
- Media deduplication.
- Listening-duration accumulation.
- Month-boundary calculations.
- Achievement thresholds.
- Theme preference persistence.
- Metadata validation.
- Entitlement-state transitions.
- Sleep-timer state transitions.

Use a fake clock for time-dependent tests.

## 27.2 Integration tests

Test:

- MediaStore indexing.
- Room persistence.
- DataStore restoration.
- MediaSessionService integration.
- Notification action routing.
- Queue restoration.
- Equalizer availability.
- Metadata editing where supported.
- Billing response handling.
- Shareable recap export.
- Widget action dispatch.

## 27.3 UI tests

Test:

- First-run permission flow.
- Empty library.
- Track playback.
- Mini-player.
- Now Playing controls.
- Search results.
- Playlist creation and editing.
- Theme changes.
- Equalizer state.
- Sleep timer.
- Lyrics empty state.
- Monthly recap.
- Premium screen.
- Permission denial and retry.
- Font scaling.
- Dark and light themes.
- Landscape and tablet layouts.

## 27.4 Audio-format test matrix

Include real, legally usable test fixtures for MP3, MIDI, WAV, FLAC, AAC, and APE.

For each fixture, verify:

1. File discovery.
2. Metadata parsing.
3. Playback initialization.
4. Audible playback on a suitable test device.
5. Seeking where the format supports it.
6. Duration reporting where available.
7. Pause and resume.
8. Next-track transition.
9. Error handling for malformed files.

Do not treat a file-opening test as proof of successful audio decoding.

## 27.5 Visual regression testing

Capture reference and implementation screenshots with the same:

- Device resolution.
- Display density.
- Font scale.
- System navigation mode.
- Theme.
- Status bar and inset conditions.
- Track metadata.
- Artwork.
- Playback state.
- Scroll position.

Compare:

- Screen structure.
- Element positions.
- Typography.
- Color.
- Artwork scale.
- Icon shape and stroke.
- Spacing.
- Elevation.
- Dialog sizing.
- Navigation behavior.

Use image-diff or perceptual comparison tools. Mask unavoidable dynamic system areas consistently and investigate substantial mismatches.

Do not call the result pixel-perfect based solely on a visual impression.

---

# 28. ACCEPTANCE CRITERIA

The implementation is not complete until the following criteria are demonstrated.

### A. Playback

- A supported local audio track plays, pauses, resumes, seeks, and stops correctly.
- Next and previous operate on the actual queue.
- Shuffle and repeat modes work.
- Playback continues after the app is backgrounded.
- Media notification controls work.
- Audio interruptions are handled without crashes.

### B. Library

- Accessible device audio is indexed.
- Songs, artists, albums, folders, and playlists are navigable.
- Search returns matching results.
- Favorites persist.
- Playlist edits persist.
- Deleted files do not remain playable as valid tracks.

### C. Audio effects

- The equalizer reflects actual supported bands.
- Valid adjustments affect playback.
- Presets and custom settings behave correctly.
- Unsupported effects fail gracefully.

### D. Themes

- Light, dark, and black modes work.
- Selected themes persist after restart.
- Text and controls remain readable.
- Locked themes use a real entitlement check.

### E. Secondary features

- Sleep timer performs its actual playback action.
- Ringtone setting follows platform requirements.
- Widget controls change playback.
- Lyrics display real available content.
- Monthly recaps use recorded listening data.
- Sharing produces a valid shareable file.
- Achievements unlock from real activity.

### F. Monetization

- Test purchases follow the actual billing flow.
- Premium access reflects validated entitlement.
- Restore purchases works.
- Ads cannot intercept playback controls.
- Offline playback remains available when the network is unavailable.

### G. Data safety

- No local audio files are uploaded.
- Storage permissions are narrowly scoped.
- Database migrations preserve user data.
- Metadata writes respect user consent.
- Logs do not expose unnecessary personal paths or metadata.

### H. Production build

- Clean build succeeds.
- No critical runtime exceptions remain.
- No fake data appears in production.
- Automated tests pass.
- Dependency and license review is complete.
- Release configuration does not contain test ads or fake purchase behavior.
- The app can be installed and tested on a physical Android device.

---

# 29. IMPLEMENTATION PHASES

Complete the project in the following order.

## Phase 1 — Reference research and project foundation

- Inspect the reference listing and screenshots.
- Build the screen inventory.
- Record verified and unverified behavior.
- Establish the project structure.
- Configure Gradle and dependencies.
- Define the initial design tokens.
- Create the build and test baseline.

Deliverable: buildable native Android foundation and reference specification.

## Phase 2 — Media discovery and playback

- Implement MediaStore indexing.
- Implement Room and DataStore.
- Build the playback service and media session.
- Implement queue, play/pause, seeking, and playback modes.
- Add notification and lock-screen controls.
- Handle audio focus and interruptions.

Deliverable: reliable offline playback.

## Phase 3 — Library and player UI

- Build Home and category browsing.
- Build search.
- Build playlists and favorites.
- Build Now Playing and mini-player.
- Add queue management.
- Add loading, empty, and error states.

Deliverable: complete core user experience.

## Phase 4 — Audio and customization

- Implement the equalizer.
- Integrate supported bass boost and reverb.
- Build theme engine.
- Add player styles.
- Add full-screen artwork.

Deliverable: functioning audio customization and theming.

## Phase 5 — Additional features

- Implement lyrics.
- Implement sleep timer.
- Implement ringtone setting.
- Implement widgets.
- Implement safe metadata editing.
- Implement listening history and monthly recap.
- Implement recap sharing and achievements.

Deliverable: complete secondary feature set.

## Phase 6 — Monetization and hardening

- Integrate billing.
- Integrate consent-compliant advertising.
- Validate entitlement handling.
- Review permissions and privacy.
- Test migrations and process recreation.
- Profile performance and battery usage.

Deliverable: production-ready application candidate.

## Phase 7 — Reference parity and release verification

- Compare every available reference screen.
- Fix layout and interaction discrepancies.
- Run automated and manual test suites.
- Verify required audio formats on real devices.
- Build the signed release artifact using secure signing practices.
- Produce a final parity and QA report.

Deliverable: tested release candidate and implementation documentation.

---

# 30. GOOGLE AI STUDIO EXECUTION INSTRUCTIONS

You must treat this document as the project's implementation specification.

1. Inspect the current project files before modifying them.
2. Determine whether the existing project is native Android, web-based, or a hybrid. If it cannot meet the requirements, create or migrate to a native Android Kotlin project.
3. Preserve useful existing work, but do not preserve broken architecture merely for convenience.
4. Implement real playback and persistent data before cosmetic polish.
5. Build reusable UI components from the design system.
6. Implement each feature with its underlying state, persistence, permissions, error handling, and tests.
7. Verify library operations against real device media.
8. Never substitute demo tracks for the user's library.
9. Never report an unsupported audio effect or format as working.
10. Never simulate purchase success.
11. Never report metadata writes or ringtone changes as successful before verifying the operation.
12. Do not leave TODOs, placeholder screens, mock services, or fake data in production paths.
13. If an external service, SDK, hardware capability, or permission is unavailable, implement a graceful limitation and document it.
14. Compile and run the project when the environment permits.
15. Fix build errors, runtime errors, and failing tests before moving to the next phase.
16. Do not claim that an emulator or physical-device test passed unless it was actually run.
17. Update documentation as implementation decisions are made.
18. Do not stop after producing a plan or UI mockup. Continue through implementation and verification within the available environment.

## Required project documentation

Create:

- README.md
- IMPLEMENTATION_PLAN.md
- ARCHITECTURE.md
- DESIGN_SYSTEM.md
- FEATURE_MATRIX.md
- PERMISSIONS_AND_PRIVACY.md
- DATABASE_SCHEMA.md
- TESTING.md
- REFERENCE_PARITY_REPORT.md
- CHANGELOG.md

Document build commands, prerequisites, permission behavior, supported formats, known limitations, testing instructions, and release steps.

## Definition of done

The project is complete only when the core offline player works, the library is populated from real device media, playback remains functional in the background, persistent features work, all implemented controls perform real actions, supported formats have been tested, and the remaining limitations are documented.

Do not prioritize a screenshot-perfect shell over a working audio engine. First establish functional correctness, then refine visual parity systematically.

Begin by inspecting the existing project and reference assets. Report the actual environment and any missing reference material, then proceed through implementation in the specified phases.