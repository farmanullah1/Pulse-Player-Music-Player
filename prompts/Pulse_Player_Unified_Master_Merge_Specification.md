# Pulse Player --- Unified Master Merge & Implementation Specification

## Google AI Studio implementation brief · Product, UX, feature and quality requirements only

> **Purpose:** Use this document as the authoritative product-level
> merge request for the existing Pulse Player Android project. It tells
> the AI implementation model what the app already is, what must remain
> intact, what to inspect in the two reference projects, and what
> additional capabilities, screens, themes, layouts, workflows and
> quality standards must be added.
>
> **Important:** This is a requirements and implementation-direction
> document, not a code-generation tutorial. Do not treat the examples as
> permission to replace the existing architecture blindly. Inspect the
> actual repository first, map every requirement to the existing app,
> and implement in safe, testable stages.

## Unified document structure, authority and merge rules

This file merges the two supplied Pulse Player specifications into one
authoritative document. It is intentionally organized in two layers so
that product requirements remain readable while the more prescriptive
technical decisions, traceability IDs, implementation prompts and
conflict resolutions remain precise.

-   **Main specification --- sections 00--19:** the complete
    product-level brief. It defines the existing Pulse Player baseline,
    preservation requirements, Namida and PixelPlayer feature expansion,
    Atmospheric Queue, button layouts, design system, backup, privacy,
    accessibility, performance, error handling, documentation, phases
    and acceptance criteria.
-   **Annex A --- detailed technical merge:** the complete technical
    implementation layer from the second source. It adds exact conflict
    resolutions, architecture and schema guidance, capability IDs,
    detailed component contracts, the 15 button presets, Classic backup
    v2, permission onboarding, performance/error gates, phased Google AI
    Studio prompts 30--46 and final traceability requirements.

### Authority and conflict handling

1.  Preserve the complete existing Pulse Player project and all working
    features. Neither source authorizes a greenfield rewrite.
2.  Annex A §3 is the explicit conflict-resolution table. Its C1--C17
    decisions override older requirements **only for the exact subjects
    listed in that table**.
3.  For all other subjects, the main specification and the existing
    project specifications remain in force. Annex A adds implementation
    detail; it does not silently cancel unrelated requirements.
4.  Where the main specification describes a capability at product level
    and Annex A describes the same capability in greater technical
    detail, implement the detailed Annex A behavior while preserving the
    main specification's user-facing intent.
5.  Where a repository audit finds that the real project differs from
    either document, do not guess or overwrite it. Record the
    difference, preserve working behavior, identify the migration, and
    request owner approval for destructive or product-policy decisions.
6.  Section references inside Annex A (for example, "§7" or "§15.9")
    refer to Annex A's own numbered sections unless they explicitly name
    a base specification file such as file 14 or file 17.
7.  Do not treat the two layers as two separate prompts to paste
    independently. This unified document is the single source of
    requirements. Use the phase prompts in Annex A §17.3 in controlled,
    incremental tasks.

### Important merged decisions at a glance

-   The app remains Pulse Player, native Android, Kotlin and Jetpack
    Compose; the actual repository and dependency versions remain
    authoritative.
-   Keep the original nine player themes and all existing skins. Player
    themes, player layout modes, colour/skin themes and button
    arrangements are separate customization axes.
-   Atmospheric Queue is an additional premier player layout and the
    default/fallback **layout mode** as specified by Annex A; it does
    not replace the Classic player theme or any existing player theme.
-   Keep the existing single playback owner, MediaSession facade and
    authoritative queue model. Layouts and button presets are
    presentation/configuration only.
-   Keep Classic favourites backup and Full reference-format backup
    separate. Annex A adds Classic backup v2 with explicit audio-file
    copying; it does not change or replace the Full backup format.
-   The offline build remains genuinely offline. Annex A resolves
    online-feature conflicts with separate `offline` and `full` product
    flavors, a master online switch defaulting OFF, and per-service
    switches.
-   Never ship ads, trackers, telemetry, crash uploads, fake data, dead
    controls, silent file modifications or unverified hardware claims.
-   Start with the repository audit. Do not implement features until the
    audit, preservation matrix, parity inventory, licensing review and
    migration plan are complete.

------------------------------------------------------------------------

------------------------------------------------------------------------

# 00 --- START HERE

## 0.1 Your role

Act as a Principal Android Product Architect, Senior Kotlin/Jetpack
Compose Engineer, Audio Playback Engineer, Accessibility Specialist and
QA Lead.

You are modifying an **existing Android application named Pulse
Player**. This is an enhancement and merge project, not a greenfield
rebuild.

Your job is to combine:

1.  Every working Pulse Player feature and design already present in the
    project.
2.  The additional product capabilities and interaction patterns
    represented by **Namida**.
3.  The additional product capabilities and Material You / Compose
    patterns represented by **PixelPlayer**.
4.  The existing Pulse Player design pack and backup specifications.
5.  The new Atmospheric Queue player layout and live playback-button
    arrangement switcher described in this document.
6.  Additional production-quality improvements required to make the
    combined product coherent, reliable, accessible, responsive and
    maintainable.

**Never assume a feature is absent just because it is not mentioned in
this document.** Inspect the actual source, screens, resources,
database, settings, navigation, tests and current APK behavior before
changing anything.

## 0.2 Project identity and existing technical direction

Pulse Player is a native Android music player. Its current specification
describes:

-   Kotlin as the application language.
-   Jetpack Compose and Material 3 for the primary UI.
-   AndroidX Media3 / ExoPlayer and a media-session playback service.
-   Room for persistent library, favourites, playlists and listening
    history.
-   DataStore for user settings.
-   MVVM-style state management using StateFlow.
-   Jetpack Glance for the home-screen widget.
-   A single-Activity application structure.
-   A local music library sourced from phone storage and user-selected
    SD-card/folder locations.
-   No login requirement, no advertisements and no mandatory cloud
    account.
-   A true-black AMOLED-friendly design direction, artwork-driven
    colors, custom gradients, player themes and skins.
-   A strong preference for real functionality, no fake data, no dead
    controls and honest reporting of limitations.

Keep the current application ID/package if one already exists. Do not
rename the app, reset its data model, replace the navigation system or
migrate libraries merely because a reference project uses a different
approach.

The reference PixelPlayer repository identifies itself as Kotlin +
Jetpack Compose + Material Design 3. Namida is a Flutter application.
Therefore, **PixelPlayer is the closer implementation-technology
reference**, while Namida is primarily a feature, interaction and
product-behavior reference. Do not port Flutter source directly into the
Kotlin app.

## 0.3 Reference projects

Study the current public project material before implementation:

-   Namida: https://github.com/namidaco/namida
-   Namida documentation: https://docs.namida.app/
-   PixelPlayer: https://github.com/PixelPlayerHQ/PixelPlayer

Use these projects to understand feature behavior, navigation,
customization, interaction patterns and product scope. Do not blindly
copy source files, dependencies, branding, logos, artwork, screenshots
or proprietary assets.

Before reusing any source code or asset, inspect the repository's
current LICENSE and applicable notices. If the license does not clearly
permit the intended reuse, independently implement the same general
capability and visual concept without copying protected code or assets.
Preserve third-party notices where reuse is permitted. Do not claim that
Pulse Player is an official Namida or PixelPlayer fork.

The repositories can change. Treat their current README, documentation,
changelog and actual source as the reference of record. Record the date
and commit/release inspected in the project audit.

## 0.4 The central product promise

Pulse Player must remain **local-first and usable as a complete music
player without an account or an internet connection**.

The original project pack states that the app has no INTERNET permission
and excludes streaming, accounts, cloud sync, online lyrics/artwork, AI
services and other online dependencies. The new merge request asks for
capabilities from projects that include online services.

Resolve this conflict as follows:

-   Preserve a fully usable offline/local playback experience.
-   Local library, playback, playlists, favourites, queue, history,
    statistics, local lyrics, themes, equalizer controls supported by
    the device, Classic backup and Full backup must not depend on a
    network.
-   Add network-dependent features as **optional, separately controlled
    integrations** only when they are genuinely useful and technically
    supportable.
-   Online integrations must be off by default, clearly disclosed,
    individually switchable, and never required for core playback.
-   If the current build must remain strictly internet-permission-free,
    implement the local equivalents and document online integrations as
    unavailable in that build. Do not silently add INTERNET permission.
-   If the product owner chooses to ship optional online integrations,
    update the product privacy statement, manifest, settings, permission
    rationale, network security policy and README honestly. Never
    continue describing that build as "no internet permission" or "100%
    offline."
-   Do not add ads, trackers, analytics SDKs, mandatory sign-in, forced
    cloud accounts or background data collection.

A setting named **Offline-only mode** must disable all network requests
and prevent online integrations from starting. It must not break local
playback, local library browsing, local backups or local statistics.

## 0.5 Non-negotiable preservation rules

1.  Do not delete or replace any existing working screen, feature,
    player theme, skin, wallpaper, widget, backup format, database
    record or user preference.
2.  Do not redesign the entire app without first documenting the current
    design and obtaining a clear mapping from old components to new
    components.
3.  Add new designs as additional selectable choices. Keep existing
    choices available.
4.  Do not change existing backup formats or their established import
    behavior.
5.  Do not wipe or destructively recreate the Room database. Use
    explicit migrations and verify data preservation.
6.  Do not move, rename or delete a user's music automatically. Any file
    copy, tag edit, delete or move operation must be a separate,
    explicit, confirmed user action.
7.  Do not fabricate songs, artists, albums, artwork, play counts,
    listening time, network results or backup records.
8.  Every visible action must work, be deliberately disabled with an
    explanation, or not be shown.
9.  Never claim a feature is implemented because a settings row or
    decorative screen exists. A feature is complete only when its full
    user workflow works.
10. Keep all expensive file, database, image, archive and audio-analysis
    work off the main thread.
11. Use the installed project's actual dependency versions and APIs. Do
    not invent library APIs or upgrade dependencies without a
    compatibility reason.
12. No fake success messages, simulated playback, pretend network
    responses or placeholder functionality.
13. Do not claim a build, test, device check or repository push passed
    unless it actually ran and succeeded.
14. The existing project documentation and fix-pack conflict resolutions
    remain authoritative unless this document explicitly updates them.

## 0.6 First action: repository audit --- do not code yet

Before making changes, inspect the complete existing project.

Create a **Current Pulse Player Inventory** containing:

-   Package/application ID, SDK levels and dependency versions.
-   Existing architecture, navigation and state-management approach.
-   All screens, tabs, drawers, dialogs, sheets, widgets and settings
    pages.
-   Every current player theme, player layout, skin theme, color mode
    and wallpaper.
-   Every current playback feature and audio effect.
-   Current media scanner, storage sources, permissions and SD-card
    handling.
-   Existing Room entities, DAOs, migrations and DataStore keys.
-   Current queue model, playback service, notification and
    media-session behavior.
-   Existing favourites JSON backup and Full backup reference-format
    implementation.
-   Existing lyrics support, if any.
-   Existing statistics, recap, history and widget behavior.
-   Existing test classes, test count and actual test status.
-   Known bugs, broken buttons, incomplete screens and inconsistent
    theme colors.
-   Existing documentation, README and `brain.md`.
-   Current Git branch, uncommitted changes and repository remote state.

For each feature, assign one status:

-   **WORKING** --- verified in source and, where possible, by running
    the app.
-   **PARTIAL** --- some behavior exists, but the complete workflow is
    missing.
-   **BROKEN** --- exists but does not work correctly.
-   **MISSING** --- no implementation found.
-   **UNVERIFIED** --- source suggests it exists, but behavior could not
    be verified.

Do not mark a feature WORKING merely because its name appears in code or
settings.

Create a **Preservation Matrix** mapping every existing Pulse Player
capability to its current screen/component and the planned destination
after the merge. No existing capability may disappear from the matrix.

Then create a **Feature Gap Matrix** mapping every new requirement in
this document to its implementation phase, UI entry point, dependencies,
offline/online classification, tests and status.

Stop after the audit and plan. Do not start implementation until the
audit has identified risks, conflicts and migration requirements.

------------------------------------------------------------------------

# 01 --- Existing Pulse Player: features and design that must be preserved

The following is the product baseline described in the original Pulse
Player specification. Verify each item against the actual repository and
preserve it if it exists. If a listed item has not yet been implemented,
treat it as a required base feature rather than assuming it is already
present.

## 1.1 Navigation and application shell

Preserve the existing primary navigation concept:

-   For you
-   Songs
-   Playlists
-   Folders
-   Albums
-   Artists

Preserve the drawer/navigation entry points for:

-   Equalizer
-   Sleep timer
-   Monthly recap
-   Favourites
-   Backup & Transfer
-   Skin theme
-   Player theme
-   Settings
-   About

Maintain the responsive phone/tablet behavior already described in the
original pack:

-   Compact phone: top bar, drawer, tab navigation, content and floating
    mini-player.
-   Medium width: NavigationRail and adaptive content.
-   Expanded width: navigation rail, main content and a docked Now
    Playing panel where appropriate.

Do not remove the original top tabs, drawer, resume banner, Shuffle/Play
pills, Recently added section, Top picks, system playlists or other
established home content.

## 1.2 For You screen

Preserve and refine:

-   Resume/Continue banner, only when genuine playback history exists.
-   Quick Shuffle and Play actions.
-   Real listening-time tile for today.
-   Favourites shortcut and real count.
-   Sleep timer shortcut.
-   Equalizer shortcut.
-   Recently added tracks.
-   Top picks/most-played tracks when sufficient real history exists.
-   Empty-library onboarding with Scan and Choose folder actions.
-   No fabricated recommendations or placeholder counts.

## 1.3 Library screens

Preserve:

-   Songs list and sorting.
-   Albums grid and album details.
-   Artists list and artist details.
-   Folders list and folder details.
-   Playlists list and playlist details.
-   Search.
-   Multi-select actions.
-   Track details.
-   Add to queue / Play next.
-   Add to playlist.
-   Favourite toggle.
-   Share file.
-   Ringtone action where supported.
-   A--Z fast scroller if already implemented.
-   Existing album-art fallback design and artwork cache.

## 1.4 Playback and Now Playing

Preserve:

-   Background playback.
-   MediaSession and notification controls.
-   Lock-screen controls.
-   Mini-player with artwork/disc animation and marquee title.
-   Expand/collapse player transition.
-   Song/Lyrics view toggle.
-   Seek bar and elapsed/total time.
-   Shuffle and repeat.
-   Previous, play/pause and next.
-   Favourite control.
-   Sleep timer.
-   Add to playlist.
-   Queue.
-   Equalizer.
-   Track information and share actions.
-   Player-theme carousel and its existing APPLY / USING state.
-   Existing queue sheet, current-track highlight and queue operations.
-   Existing playback position and queue restoration behavior.

The player must continue using one playback engine, one service/session
and one authoritative queue model. A new visual layout must never create
another player or another queue.

## 1.5 Existing player themes --- preserve all nine

The current canonical list in the existing specification is:

1.  `classic` --- Classic
2.  `mountain` --- Mountain Horizon
3.  `mesh` --- Mesh Glass
4.  `minimal` --- Minimal Mood
5.  `acoustic` --- Acoustic
6.  `vinyl` --- Vinyl
7.  `coast` --- Coastal Ocean
8.  `starry` --- Starry Sky
9.  `immersive` --- Immersive Artwork Player

Keep their current IDs, visual identity, selection behavior and
persistence. Do not rename IDs or silently replace previews. The
original default is Classic unless the actual repository has already
made a deliberate user-facing change; document any proposed default
change before applying it.

The Immersive Artwork Player remains a presentation-only layout with
blurred artwork, floating controls and a bottom queue panel. It must
share the same playback state and controller as every other theme.

## 1.6 Existing design system

Preserve and refine the current AMOLED-first design language:

-   True-black AMOLED option.
-   Dark, light and system theme modes.
-   Plum, Aurora, Afterglow, Ocean, Sunrise, Forest, Mono, Mesh and
    Dynamic gradient families.
-   Artwork-driven dynamic accents.
-   Cyan favourite accent and orange APPLY/USING action where already
    established.
-   Rounded artwork, cards, bottom sheets and pill controls.
-   4dp spacing grid.
-   48dp minimum touch targets.
-   Roboto/system typography unless a bundled font is deliberately
    introduced.
-   Reduced-motion support.
-   Existing spring animations, mini-player expansion, heart animation,
    playback icon morph, playing bars and theme transitions.

Do not globally replace the original design with a generic Material
template. Material 3 should improve consistency and accessibility while
retaining Pulse Player's visual identity.

## 1.7 Existing settings and local behaviors

Preserve existing settings for:

-   Theme mode and skin.
-   Player theme.
-   Dynamic accent from artwork.
-   Reduced motion.
-   Library rescan.
-   Folder selection and exclusion.
-   Minimum duration filter.
-   Playback fade behavior.
-   Speed.
-   Stop after current track.
-   Pause on headset disconnect.
-   Equalizer.
-   Widgets.
-   Backup & Transfer.
-   About and offline/privacy statement.

Preserve these exact or equivalent baseline behaviors:

-   Favourite changes persist immediately and update all screens from
    the same source of truth.
-   Shuffle toggling off restores the intended original queue order.
-   Repeat cycles Off → All → One.
-   Sleep timer presets, custom duration and end-of-track option.
-   Sleep timer fade-out behavior.
-   Headset unplug pauses playback.
-   Audio focus/call handling is stable.
-   Missing files do not erase favourites.
-   Queue restores paused after app relaunch.
-   Recap is computed from real play history and hidden when there is no
    data.
-   Unsupported audio effects are hidden or clearly disabled.
-   Ringtone permission is requested only when the user invokes the
    ringtone action.

------------------------------------------------------------------------

# 02 --- Merge strategy and feature classification

## 2.1 Feature classification

Every requested feature must be assigned to one of these groups:

### A. Local core --- must work offline

Examples: library scanning, folder filtering, playlists, favourites,
queue, local search, local lyrics files, metadata display, local tag
editing where supported, local statistics, themes, widgets, sleep timer,
local backup/restore, local smart playlists and local recommendation
algorithms.

### B. Optional online integration

Examples: LRCLIB lookup, Deezer artist artwork lookup, online AI
playlist providers, YouTube-related functionality, scrobbling services,
remote Subsonic/Jellyfin/WebDAV/SMB servers, Google Cast discovery where
it requires network access, and cloud-hosted content.

These features must be isolated behind an integration layer, disabled by
default and usable only after explicit user opt-in. If the shipped
product must have no INTERNET permission, do not include active online
clients in that build.

### C. Local-network integration

Examples: device-to-device sync and LAN listening party. These do not
require a public cloud account, but they still require network
communication and local-network permissions/behavior appropriate to
Android versions. They must be opt-in, paired/authorized and disabled in
Offline-only mode.

### D. Hardware-dependent audio capability

Examples: bit-perfect output, USB DAC exclusive/direct access, 32-bit
float path, audio effects, output-device routing and hardware
visualizers. Show only what the Android version, device, output route
and audio engine can genuinely support.

### E. Platform-restricted / experimental

Examples: Android Auto, Wear OS, Chromecast, status-bar lyrics,
sponsor-segment services, advanced video support, vocal removal and
beat-matched crossfade. Do not fake support. Add only after platform
compatibility, API availability, licensing and testing are established.

## 2.2 Capability registry

Create a central capability model so the UI can accurately distinguish:

-   Available and enabled.
-   Available but disabled by user.
-   Requires permission.
-   Requires optional integration setup.
-   Unsupported by device.
-   Unsupported by Android version.
-   Experimental.
-   Not included in this build.

Settings and screens must explain why a feature is unavailable. Never
show a control that appears operational but does nothing.

## 2.3 Priority levels

-   **P0 --- Core reliability:** preservation, playback, library
    integrity, queue, themes, backup/restore, permission handling,
    accessibility, responsive layouts, no data loss.
-   **P1 --- High-value local features:** advanced library organization,
    smart playlists, enhanced search, lyrics editor/import, detailed
    statistics, button-layout presets, additional player layouts, audio
    profiles, local recommendations.
-   **P2 --- Optional integrations and advanced capabilities:** online
    lyrics/artwork, AI services, remote libraries, LAN sync/listening
    party, Cast, Android Auto, advanced audio routing, video support.
-   **P3 --- Experimental research:** beat matching, vocal removal, true
    bit-perfect/exclusive output, complex visualizers, advanced audio
    analysis. Only ship when validated.

Do not start P2/P3 while P0 functionality is broken.

------------------------------------------------------------------------

# 03 --- Namida feature expansion requirements

The following capabilities are requested from Namida's published feature
set and documentation. Implement the local/offline-compatible parts as
core features. Implement online and platform-specific parts only under
the capability and privacy rules above.

## 3.1 Advanced library indexer

Add or enhance:

-   Configurable scan roots: internal storage, removable SD card and
    user-selected SAF folders.
-   Folder include and exclude rules.
-   Duplicate-track detection using stable identifiers and layered
    matching.
-   Minimum file-size filter.
-   Minimum duration filter.
-   Optional maximum duration filter.
-   Exclude notification sounds, alarms, ringtones, voice notes or other
    categories by configurable rules.
-   Hidden-file and hidden-folder handling with a clear default.
-   Scan progress, current source, discovered count, updated count,
    skipped count, unreadable count and duplicate count.
-   Recoverable scanning: interrupted or failed scans must not
    incorrectly mark the entire library missing.
-   Per-source scan status and last successful scan.
-   Rescan one folder/source without rescanning everything.
-   Manual refresh and automatic refresh policy.
-   SD-card removal and reinsertion handling.
-   Stable identity across MediaStore, SAF and copied-file sources.
-   A Missing/Unavailable filter that does not delete metadata,
    favourites or playlists.
-   Explainable duplicate resolution: never silently discard one file
    because titles happen to match.
-   Configurable filename-to-tag fallback when embedded metadata is
    missing.
-   Configurable artist delimiters and smart parsing of multiple
    artists.
-   Album Artist grouping distinct from track Artist.
-   Extract featured artists such as feat., ft. and featuring into
    artist relationships without corrupting the original title.
-   Artist and genre separator preferences.
-   Sort by title, artist, album, album artist, genre, year, track
    number, disc number, duration, date added, date modified, play
    count, last played, rating, file size and folder.
-   Ascending/descending order and stable tie-breaking.
-   Persist sorting independently per library page.
-   Folder cover discovery using `cover.jpg`, `folder.jpg` and supported
    embedded artwork.
-   Optional folder information from `.info.txt`, with safe plain-text
    parsing only.
-   Library counts must always agree with visible filters and current
    permission scope.

## 3.2 Expanded library pages

Add the following browse destinations while preserving all current tabs:

-   Genres.
-   Queues / saved queue sessions.
-   Most Played.
-   Moods.
-   Tags.
-   Ratings.
-   Languages.
-   Statistics.
-   Recently played.
-   Recently added.
-   Missing songs.
-   Optional Videos page only if local video support is intentionally
    implemented.

Every page needs:

-   A clear title and count based on real data.
-   Search/filter and sorting where useful.
-   Empty, loading, permission-denied and error states.
-   Consistent track rows and selection behavior.
-   Contextual actions.
-   Navigation back to the source context.
-   Support for multi-select and global selection where appropriate.

Do not force every page into the top tab row. Use configurable
navigation destinations or a Library section so the primary navigation
remains uncluttered.

## 3.3 Metadata and Tag Editor

Add a real metadata editor, subject to Android storage access and format
support:

-   View all readable tags for a track.
-   Edit title, artist, album, album artist, genre, year, track number,
    disc number, composer, lyricist, comment, grouping, mood, rating and
    supported custom fields.
-   Edit embedded cover artwork where supported.
-   Batch edit selected tracks.
-   Preview changes before writing.
-   Validate values and preserve unknown tags where the library permits.
-   Show file format and which fields are writable.
-   Clearly distinguish metadata stored in the Pulse Player database
    from tags actually written to the audio file.
-   Confirm before writing to the original file.
-   Never modify files automatically during scanning.
-   Keep a recoverable before/after metadata snapshot where technically
    possible.
-   Handle read-only, unsupported, protected and SAF documents with
    clear explanations.
-   Use TagLib or another appropriately licensed and maintained library
    only after checking Android compatibility and license obligations.
-   If a format cannot be safely written, show read-only metadata and
    explain the limitation instead of pretending the save succeeded.

## 3.4 Smart playlists and collections

Add rule-based smart playlists with:

-   All/any rule grouping.
-   Conditions for artist, album, album artist, genre, year, duration,
    rating, mood, tags, folder, date added, date played, play count,
    last played, favourite status and file availability.
-   Numeric ranges and text contains/equals/starts-with conditions.
-   Include/exclude rules.
-   Sort order and result limit.
-   Live updates when library metadata or listening history changes.
-   Search within smart playlist results.
-   Save, duplicate, rename, edit and delete.
-   Convert a smart playlist to a static playlist.
-   Show why a track matches, where useful.
-   Rule validation and a useful empty state.

Provide built-in local smart collections such as:

-   Recently added.
-   Recently played.
-   Most played.
-   Never played.
-   Short tracks.
-   Long tracks.
-   High-rated tracks.
-   Favourites.
-   Underrated tracks.
-   Forgotten tracks.
-   Recently rediscovered.
-   Tracks from this day in previous years.

Do not label deterministic local recommendations as AI. Use **Smart
Mix** or **Daily Mix** for local algorithmic playlists unless a real AI
provider is connected and disclosed.

## 3.5 Local recommendation features

Implement a local recommendation engine using only the user's on-device
library and play history:

-   Smart Tracks: tracks frequently listened to alongside the current
    track.
-   Similar tracks based on shared artists, albums, genres, tags, moods,
    ratings and listening patterns.
-   Daily Mix generated from actual user behavior.
-   Underrated tracks: low play count but positive rating or meaningful
    listening history.
-   Lost Memories: tracks listened to around the same date in previous
    years.
-   Release-year, mood, genre, time-range and rating-based mixes.
-   Random discovery with user-configurable exclusions.
-   Explain recommendation reasons in a simple "Why this track?" detail.
-   Exclude unavailable tracks and respect explicit folder/library
    filters.
-   Do not invent recommendations when the library/history is too small.
    Show a clear message explaining what data is needed.
-   Provide a setting to disable personalization and clear
    recommendation-derived data.

## 3.6 Advanced queue and playback modes

Expand queue behavior with:

-   Persistent named queue sessions.
-   Save current queue, restore, rename and delete saved sessions.
-   Drag-and-drop reorder with accessible move-up/move-down
    alternatives.
-   Move to top/bottom.
-   Play next.
-   Add to queue.
-   Insert after the most recently inserted item.
-   Add multiple selected tracks in their selected order.
-   Remove one or multiple tracks.
-   Clear queue with confirmation.
-   Scroll to current track.
-   Shuffle that preserves a restorable base order.
-   Repeat Off / All / One.
-   Repeat current track N times before advancing.
-   Play mode options:
    -   Selected track only.
    -   Current search results.
    -   Current playlist.
    -   Current album.
    -   Current artist.
    -   Current genre.
    -   Current folder.
-   Queue state must survive process death safely and restore paused.
-   Queue operations must not restart the current song unless the user
    explicitly selects a new track.
-   Queue state must have one source of truth. Do not enable Media3's
    independent shuffle if the project uses a separate queue-order
    model.

## 3.7 History and listening statistics

Provide editable, transparent listening history:

-   Track play events with timestamps and actual listened duration.
-   Configurable minimum seconds or percentage required to count a play.
-   Ignore paused and buffering time.
-   Account for playback speed so statistics can distinguish media
    duration and real elapsed listening time.
-   Avoid double-counting repeat-one loops or process recovery.
-   Allow users to inspect, remove or import history records.
-   Clearly explain that removing a history record does not delete the
    audio file.
-   Daily, weekly, monthly, yearly and custom-range summaries.
-   Total listening time, sessions, unique tracks, top songs, artists,
    albums, genres and folders.
-   Listening streaks only when derived from actual records.
-   Monthly recap and Your Year page.
-   Charts with accessible textual summaries.
-   Shareable recap cards generated locally.
-   Optional privacy setting to hide listening statistics from
    widgets/lock-screen surfaces.
-   Time-zone-safe aggregation and month boundaries.
-   No fake statistics or seeded demo records.

## 3.8 Search improvements

Expand search to cover:

-   Track title.
-   Artist and featured artist.
-   Album and album artist.
-   Filename.
-   Folder.
-   Genre.
-   Tags and mood.
-   Lyrics when lyrics are locally available or explicitly cached.
-   Playlist names and contents.
-   Ratings and metadata fields where useful.

Support:

-   Typo tolerance and ranked results.
-   Normalization of case, accents and punctuation.
-   Configurable search scope.
-   Recent searches, clearable by the user.
-   Search filters and sort.
-   Search inside albums, artists, genres, playlists and folders.
-   Search-to-play behavior that respects the selected Track Play Mode.
-   A clear no-results state with suggestions to broaden the search.
-   Debouncing without making the UI feel delayed.

## 3.9 Lyrics

Support local-first lyrics:

-   Read adjacent `.lrc` and `.txt` files.
-   Import a user-selected LRC/TXT file using SAF.
-   Match lyric files safely to a track.
-   Display plain lyrics.
-   Display timestamp-synced LRC lyrics.
-   Support word-synced formats such as TTML only if a reliable parser
    is implemented.
-   Scroll synchronized lyrics and highlight the active line.
-   Tap a lyric line to seek when timestamps are available.
-   Manual lyric offset adjustment.
-   Lyrics editor for text and timestamps.
-   Create, edit, save, export and remove sidecar lyrics.
-   Preserve original lyric files unless the user explicitly overwrites
    them.
-   Handle missing, malformed, unsupported and empty lyrics.
-   Lyrics must remain readable with every player theme and color mode.

Optional online lyrics lookup through LRCLIB or another provider must be
disabled by default, require explicit consent and disclose the provider.
If the build remains internet-permission-free, provide local file import
only.

## 3.10 Sleep timer and pause scenarios

Extend the existing timer with:

-   End of current track.
-   End of current playlist/queue.
-   After N tracks.
-   After a selected duration.
-   Fade-out duration.
-   Pause when audio output reaches zero, if reliable and user-enabled.
-   Pause on headset/Bluetooth disconnect.
-   Pause on incoming call/audio focus loss.
-   Configurable behavior after notification interruptions.
-   Show remaining time in the player and notification where supported.
-   Allow cancel, edit and resume.
-   Prevent duplicate timers.
-   Recover safely after process death.
-   Never resume playback unexpectedly after a call or timer unless the
    user's chosen behavior explicitly allows it.

## 3.11 Audio and sound capabilities

Build a capability-aware audio settings area containing:

-   Standard equalizer presets.
-   Custom equalizer presets.
-   Parametric EQ if the audio engine/device supports it.
-   AutoEQ profile import only when a valid supported profile format is
    available.
-   Bass enhancement.
-   Reverb.
-   Virtualizer / surround effect.
-   Mono audio.
-   Output-device information and routing where Android permits.
-   Audio path viewer showing the known stages and clearly identifying
    unavailable/unknown stages.
-   Playback speed.
-   Pitch adjustment, including semitone controls.
-   Optional 432 Hz tuning mode with clear explanation of what it
    changes.
-   Loudness normalization / ReplayGain where supported.
-   Per-track sound profile: EQ, volume, speed and pitch.
-   Save, rename, duplicate and reset sound presets.
-   Reset to safe defaults.
-   Clipping/distortion warning for extreme EQ or gain settings.
-   Device capability detection and lifecycle-safe effect
    release/recreation.

Important audio honesty:

-   Do not promise bit-perfect output unless the actual playback path
    has been verified to preserve the source stream without resampling
    or DSP.
-   Do not claim USB DAC exclusive mode or direct hardware access unless
    the Android device/API/driver and implementation genuinely support
    it.
-   Do not claim 32-bit float output simply because an internal buffer
    uses float samples.
-   Do not promise gapless playback, crossfade or beat matching on every
    device/codec. Test the actual implementation.
-   Advanced effects such as vocal removal and 8D audio must be labelled
    experimental if they are not true, robust implementations. Never
    present a simple stereo pan effect as professional vocal removal.
-   Unsupported controls must be hidden or disabled with a reason.

## 3.12 Visualizers and artwork interactions

Add optional, battery-aware visual features:

-   Audio-reactive visualizer modes.
-   Minimal waveform / spectrum / bars / circular visualizer styles.
-   Animated background modes.
-   Procedural wallpapers such as nature, ocean, stars, mesh and
    abstract gradients.
-   Artwork particles or thumbnail motion reacting to audio intensity
    where reliable.
-   Mini-player Party Mode with edge breathing effect.
-   Artwork tap/long-press gestures.
-   Artwork gesture customization.
-   Static mode for battery saver.
-   Reduced-motion compliance.
-   Visualizer intensity and frame-rate controls.
-   Stop rendering when the player is not visible or app is backgrounded
    unless required by a user-visible feature.
-   No copyrighted wallpapers or scraped images bundled without
    permission.

## 3.13 Import/export and external history

Support local playlist interchange:

-   Import/export M3U and M3U8 where practical.
-   Preserve playlist order.
-   Report missing paths and unmatched tracks.
-   Allow the user to preview import results.
-   Avoid overwriting existing playlists without confirmation.
-   Support playlist artwork only when the format/source supports it.

Optional import of Spotify, Last.fm, ListenBrainz or YouTube history
must be through user-selected export files or supported documented APIs.
Do not ask for passwords or scrape private accounts. Show import
preview, duplicate handling and a report of records that could not be
matched. Keep this outside the offline core.

------------------------------------------------------------------------

# 04 --- PixelPlayer feature and design expansion requirements

## 4.1 Material You / Material 3 Expressive design

Add a new optional design family inspired by PixelPlayer's modern
Material You approach:

-   Wallpaper-derived dynamic color on supported Android versions.
-   Album-art-derived accent palette.
-   Manual accent color.
-   Separate light and dark palettes.
-   AMOLED true-black mode.
-   Automatic theme switching based on system theme and optional
    schedule.
-   Adjustable corner radius.
-   Adjustable navigation-bar appearance.
-   Consistent component shapes and elevation.
-   Expressive but restrained transitions.
-   Animated state changes and button micro-interactions.
-   Consistent pressed, focused, selected, disabled and loading states.
-   Accessible color contrast and touch targets.

Do not replace the existing Pulse palette. Add the new design family as
selectable themes/skins. Keep the existing plum, cyan, violet, magenta,
orange and other original color families.

## 4.2 PixelPlayer-style library organization

Add or refine:

-   Multi-artist parsing with user-configurable delimiters.
-   Album Artist grouping.
-   Folder selection and exclusion.
-   Full-text search.
-   Multi-format local playback based on actual decoder support.
-   Playlist management.
-   Listening statistics.
-   Local lyric display and editing.
-   Artist artwork cache and fallback.
-   LRU memory cache plus persistent cache where appropriate.
-   Cache size limit and clear-cache action.
-   Cache invalidation when artwork or metadata changes.

Online Deezer artist artwork lookup is optional and must not be required
for artist pages. Local embedded artwork and cached artwork must
continue to work offline.

## 4.3 Playback engine and media session

Preserve the current Media3 playback architecture if it is stable.
Evaluate FFmpeg extension support only after checking:

-   Current Media3 version compatibility.
-   Supported Android ABIs.
-   Decoder licensing and redistribution requirements.
-   APK size and native-library impact.
-   Security and malformed-media handling.
-   Actual formats the extension adds beyond platform decoders.

Provide robust support for common formats such as MP3, FLAC, AAC/M4A,
OGG/Opus and WAV where the installed decoder stack supports them. APE,
MIDI and other less common formats must be capability-tested; never
advertise universal format support without proof.

Maintain:

-   Background playback.
-   MediaSession.
-   Notification controls.
-   Lock-screen metadata.
-   Queue reorder.
-   Shuffle/repeat.
-   Gapless playback where validated.
-   Configurable transitions only where technically supported.
-   Audio focus and noisy-output handling.
-   Service reconnect and process-death recovery.

## 4.4 Optional AI playlist generation

Provide a provider-neutral design for AI playlist generation, but do not
make the app dependent on a specific provider.

Requirements:

-   Local playlist generation must remain available without AI.
-   AI providers are optional and individually configured.
-   Support provider selection only for providers whose current API and
    terms have been checked.
-   API keys must be stored securely and never embedded in source code,
    logs, exported backups or crash messages.
-   Explain what track metadata is sent to the provider.
-   Send the minimum necessary metadata.
-   Never upload audio files unless a separate explicit feature and
    consent are introduced.
-   Provide prompt-based playlist intent such as "calm evening",
    "workout", "similar to this track" or "songs I have not heard
    recently".
-   Validate provider output against the local library; only add tracks
    that actually exist locally unless the user explicitly requests
    another source.
-   Handle invalid IDs, duplicate results, unavailable tracks, rate
    limits, timeouts, provider errors and quota exhaustion.
-   Provide a local fallback and clearly label AI-generated results.
-   Include a "Delete API key" and "Clear AI history" action.
-   AI functionality must be disabled in Offline-only mode.

## 4.5 Optional connectivity and device integrations

Evaluate and implement only after core app stability:

-   Google Cast / Chromecast.
-   Android Auto.
-   Wear OS companion controls.
-   Home-screen widgets via Glance.
-   Optional status-bar lyrics if Android version and platform policy
    allow it.
-   Optional external scrobbling integration.
-   Optional remote libraries: Subsonic/Navidrome, Jellyfin, WebDAV and
    SMB.
-   Optional LAN device sync.
-   Optional LAN listening party.

Every integration needs:

-   Dedicated setup and status screen.
-   Clear permission and network explanation.
-   Connection test.
-   Authentication stored securely when applicable.
-   Disconnect/revoke action.
-   Retry and timeout policy.
-   Offline fallback.
-   No mandatory account.
-   No silent background upload.
-   No network calls when disabled or Offline-only mode is active.

Do not implement YouTube scraping, undocumented endpoints, DRM
circumvention or other unstable/legally questionable integrations. Any
YouTube-related feature must use an authorized, documented and
policy-compliant source, or remain out of scope.

------------------------------------------------------------------------

# 05 --- New player layout: Atmospheric Queue

Add a new selectable player layout called **Atmospheric Queue**. It is a
new layout, not a replacement for the existing nine themes.

## 5.1 Visual concept

A two-part Now Playing screen:

-   Upper section: dark atmospheric forest/mist appearance with a
    layered background and a dark scrim.
-   Lower section: light/off-white "Up Next" queue surface with rounded
    top corners.
-   Clear contrast between the dark playback area and light queue area.
-   Premium, calm, cinematic appearance.
-   Track artwork and title remain the primary focus.
-   No stock/copyrighted background image dependency. Prefer a
    lightweight procedural or generated-in-app mist treatment, with a
    static gradient fallback.

## 5.2 Upper playback section

Include:

-   Collapse/down chevron.
-   Song / Lyrics selector.
-   Theme picker entry.
-   Live button-layout picker entry.
-   More options.
-   Current track title and artist/channel.
-   Album/track metadata when space allows.
-   Progress slider with elapsed and total time.
-   Shuffle.
-   Previous.
-   Large central play/pause control.
-   Next.
-   Repeat.
-   Favourite.
-   Sleep timer.
-   Add to playlist.
-   Queue jump.
-   Equalizer.

Controls must connect to the existing playback state and actions. The
displayed times, track title, artist and current state must be real.

## 5.3 Lower queue section

Include:

-   "Up Next" heading.
-   Accurate queue count.
-   Scrollable queue.
-   Artwork thumbnail or fallback.
-   Active track highlight.
-   Animated equalizer bars for current track only.
-   Track title and artist.
-   Per-track options.
-   Tap-to-play.
-   Drag-to-reorder if supported by the shared queue model.
-   Remove and Play next actions.
-   Empty queue state.
-   Current-track-not-in-queue state if the model can represent it.

The active amber/gold accent may use `#D4AF37`, but must remain readable
in both theme modes. The light queue card may be a defining style of
this layout; all other screens and themes must still follow the selected
app theme.

## 5.4 Responsive behavior

-   Compact portrait: split layout with carefully measured upper and
    lower sections.
-   Short screen / landscape: collapse artwork/background detail before
    hiding essential playback controls.
-   Tablet / unfolded: use a two-column composition where playback
    occupies one panel and queue/lyrics the other.
-   Large text: allow vertical scrolling or compact mode; never clip
    essential controls.
-   Respect display cutout, status/navigation bars, gesture insets and
    rounded corners.
-   All controls meet 48dp touch target.
-   Reduced-motion mode disables continuous fog, equalizer and
    decorative animations without disabling playback.

## 5.5 Integration

-   Add the layout to the existing layout registry/picker.
-   Keep the nine existing themes selectable.
-   Persist the user's choice.
-   Provide an accurate preview.
-   Reuse the same Now Playing state, queue model and playback
    controller.
-   Do not create a second player or duplicate queue state.
-   Add UI and state tests for layout switching, queue updates, theme
    changes and playback continuity.

------------------------------------------------------------------------

# 06 --- Live playback button arrangement switcher

Add a live customization feature that changes the arrangement of
playback controls and secondary quick actions while a track is playing.

## 6.1 Preset arrangements

Provide at least 12 distinct, genuinely different arrangements:

1.  Centered --- primary play/pause centered, transport controls
    balanced around it.
2.  Split --- transport controls separated into left and right groups.
3.  Minimal --- essential transport controls only; secondary actions in
    an overflow/expand area.
4.  Waveform Below --- progress/waveform occupies its own row below the
    primary controls.
5.  Queue Inline --- compact queue preview integrated into the control
    area.
6.  Expanded --- larger controls and visible secondary actions.
7.  Compact --- reduced vertical footprint for small screens.
8.  Floating --- visually elevated central play/pause control.
9.  Symmetric --- mirrored left/right control placement.
10. Asymmetric --- primary action emphasized with secondary controls
    offset.
11. Two-row --- transport row plus secondary action row.
12. Radial-inspired --- circular visual arrangement adapted to touch
    accessibility.

Add further presets only if each has a clear visual and interaction
difference.

## 6.2 Behavior

-   Switch from Now Playing without stopping or restarting music.
-   Open the picker from the player itself.
-   Open the same picker from Settings → Player controls / Layouts.
-   Preview each arrangement with the current track state.
-   Apply immediately or explicitly confirm, depending on existing
    design conventions.
-   Persist selected arrangement.
-   Restore it after app restart.
-   Allow separate portrait and landscape choices if practical.
-   Allow per-player-theme override only if it does not make settings
    confusing.
-   Animate transitions smoothly.
-   Respect reduced motion.
-   Maintain consistent control semantics and accessibility labels.
-   Do not move or remove the underlying actions; only change
    presentation and grouping.
-   Provide Reset to default.

------------------------------------------------------------------------

# 07 --- Additional player and library quality features

Add the following high-value refinements where they do not conflict with
existing behavior.

## 7.1 Track details and metadata insight

A comprehensive Track Details sheet should show:

-   Title, artist, album and album artist.
-   Duration, file size, format and bitrate/sample rate when available.
-   Track/disc number and year.
-   Genre, composer and other available tags.
-   File location/source.
-   Date added and modified when available.
-   Play count and last played.
-   Favourite state.
-   Availability status.
-   Audio capabilities and unsupported-format explanation.
-   Artwork source.
-   Lyrics availability.
-   File actions supported by the granted storage access.

Never invent missing metadata. Distinguish "unknown" from zero or empty.

## 7.2 Track actions and gestures

Provide configurable gestures:

-   Swipe left/right on a track row.
-   Long press for selection or contextual actions.
-   Artwork tap and long press.
-   Mini-player swipe/expand/collapse.
-   Queue item drag reorder.

Let the user choose the actions for swipe directions where practical.
Provide a visible alternative for every gesture so functionality is not
gesture-only.

## 7.3 Playlist management

Support:

-   Static playlists.
-   Smart playlists.
-   Custom order.
-   Drag reorder.
-   Playlist artwork.
-   Playlist description.
-   Rename, duplicate, delete.
-   Add/remove tracks.
-   Import/export M3U/M3U8.
-   Merge playlists with preview.
-   Duplicate-track handling.
-   Empty and missing-track states.
-   System playlists based on real data.

## 7.4 Home screen widget and quick settings

Preserve the current responsive Glance widget and improve it:

-   2×2, 4×1 and 4×2 responsive sizes.
-   Artwork, title, artist and play/pause.
-   Previous/next where space allows.
-   Favourite if supported.
-   Tap opens Now Playing.
-   Proper updates when track changes.
-   No excessive polling.
-   Correct behavior after reboot and process death.

Optional quick settings tiles:

-   Sleep timer.
-   Equalizer.
-   Favourites.
-   Today's listening.
-   Offline-only mode, if the build includes online integrations.

Tiles must be added by the user through Android's system UI. Do not
pretend to auto-add them.

## 7.5 Notifications and lock screen

-   Correct metadata and artwork.
-   Play/pause, previous, next.
-   Optional favourite action if Android/media-session support allows
    it.
-   Stable behavior on Android 14+ foreground service rules and newer
    versions.
-   Notification permission requested at the appropriate time.
-   Do not request notification permission just to scan the library.
-   Correct behavior when notification permission is denied; playback
    must still work where platform rules permit.

------------------------------------------------------------------------

# 08 --- Design system, themes, skins and layout registry

## 8.1 Separate concepts clearly

Do not mix these terms:

-   **App theme:** global colors, typography, surfaces, shapes and
    component styling.
-   **Skin:** a coordinated visual package that may include colors,
    gradients, backgrounds, icons and decorative treatments.
-   **Player theme:** a complete Now Playing visual presentation.
-   **Player layout:** the arrangement of player content and controls.
-   **Button arrangement:** the placement of playback and quick-action
    buttons within a player layout.
-   **Wallpaper/background:** a background treatment used by supported
    screens.

Each picker must use the correct category and explain what it changes.

## 8.2 New theme families to add

In addition to every existing theme, provide selectable choices for:

-   Material You --- wallpaper dynamic color.
-   Artwork Dynamic --- current album-art palette.
-   Expressive Material --- PixelPlayer-inspired expressive surfaces and
    motion.
-   AMOLED Pure Black.
-   Dark Plum.
-   Light Warm.
-   Ocean.
-   Forest.
-   Aurora.
-   Mesh.
-   Monochrome.
-   Custom accent theme.

Do not duplicate themes that are already present. Map new options to
existing equivalent IDs where appropriate and preserve all stored user
selections.

## 8.3 Design tokens

Use a single consistent token system for:

-   Backgrounds.
-   Primary/secondary/tertiary surfaces.
-   Text primary/secondary/disabled.
-   Accent colors.
-   Error/warning/success states.
-   Dividers and outlines.
-   Scrims and overlays.
-   Playback active/inactive.
-   Queue current/next states.
-   Focus/pressed/selected states.
-   Cards, sheets, buttons and navigation.

No screen should hardcode a color that breaks when the user changes
theme. Intentional player-theme accents may be locally defined but must
be contrast-tested and adapted to the background.

## 8.4 Global visual audit

Audit every:

-   Screen.
-   Dialog.
-   Bottom sheet.
-   Snackbar.
-   Dropdown.
-   Context menu.
-   Widget.
-   Notification surface where app-controlled.
-   Empty state.
-   Loading state.
-   Disabled control.
-   Selected item.
-   Player theme.
-   Queue item.
-   Search field.
-   Slider and switch.

Test all of them in Light, Dark, AMOLED, System, Dynamic and every
custom skin.

## 8.5 Customization settings

Add user controls for:

-   Corner radius.
-   Artwork corner radius where appropriate.
-   Navigation style.
-   App accent.
-   Dynamic wallpaper color.
-   Dynamic album-art color.
-   Gradient/background selection.
-   Player background dimming.
-   Artwork blur intensity.
-   Player control size.
-   Player button arrangement.
-   Mini-player style.
-   Animation intensity.
-   Reduced motion.
-   Visualizer mode and intensity.
-   List density.
-   Album grid density.
-   Optional font scale override only if it respects system
    accessibility scale.

Every customization must have a preview, a reset action and persistent
settings.

------------------------------------------------------------------------

# 09 --- Classic and Full backup: preserve and improve without mixing

There are two existing backup systems. They are separate products and
must remain separate.

## 9.1 Classic favourites JSON backup

Preserve the exact `pulseplayer.favorites` v1 JSON format and existing
fuzzy-matching behavior.

The requested enhancement is:

-   Use `Music/Pulse Player/` as the suggested destination.
-   Export favourites metadata to JSON.
-   Copy each favourite audio file into that folder only after explicit
    user confirmation.
-   Never move or delete the original audio.
-   Avoid overwriting same-named files without a safe
    conflict-resolution flow.
-   Report copied, skipped, missing, duplicate and failed files.
-   Keep JSON and audio-copy operations recoverable.
-   Import from a user-selected folder/document tree or selected JSON
    file.
-   Preview matching results before applying.
-   Support Merge and Replace only if the existing Classic specification
    supports those modes; do not silently alter v1 semantics.
-   Match by exact stable identity/path first, then carefully ranked
    metadata matching.
-   Never auto-select an ambiguous match.
-   Report unmatched items.
-   Handle partial copy, storage full, permission denial, corrupt JSON
    and unreadable files.

Do not change the v1 schema or make Classic backup import the Full
backup `.wav` format.

## 9.2 Full backup --- reference format

Preserve the existing Full backup reference-format design based on the
user's sample `.wav` file, which is a ZIP archive renamed with a `.wav`
extension.

Preserve the current rules:

-   Separate screen/card and codec from Classic favourites backup.
-   Import using SAF document picker with broad file selection.
-   Detect file content by signature, not extension alone.
-   Reject actual WAV audio as a backup.
-   Validate ZIP structure and entry names safely.
-   Never extract path-traversal entries.
-   Enforce archive size, entry count, per-entry size and total
    decompressed size limits.
-   Never open a database supplied inside the archive as a live
    database.
-   Stage data, validate it, preview the changes and only then apply.
-   Merge is the default restore behavior.
-   Replace requires explicit confirmation.
-   Apply changes transactionally and roll back on failure.
-   Keep the sample's schema and field mapping exact where already
    specified.
-   Do not invent a non-empty lyrics schema when the sample format does
    not define one.
-   Never trust source-device artwork content URIs on another device.
-   Preserve idempotent repeated imports.
-   Keep user sample backups out of public Git repositories.
-   State honestly that import of the reference format is supported by
    the specification, while acceptance of exported files by the other
    application remains unverified unless actually tested.

UI wording must be **"Full backup (reference format)"**. Do not use the
word "compatible" as a user-facing promise.

## 9.3 Backup improvements

Add:

-   Last successful backup details.
-   File name, date/time, size and category counts.
-   Export verification before recording a backup as successful.
-   Restore preview with new, existing, skipped, unmatched and conflict
    counts.
-   Clear error and recovery messages.
-   Progress and cancellation where safe.
-   Temporary-file cleanup.
-   Free-space checks.
-   Optional encryption only as a separately designed future capability
    with documented key recovery. Never add fake encryption or hard-code
    a key.
-   Backup settings must not be included in a way that leaks API keys,
    credentials or secrets.

------------------------------------------------------------------------

# 10 --- Permissions, privacy and security

## 10.1 Do not request every permission on first launch

The old merge request says to request all permissions at first app open.
Replace that behavior with a **just-in-time permission strategy**.
Asking for every permission before the user understands the app creates
a poor experience and may cause unnecessary denial.

On first launch:

1.  Explain that Pulse Player needs access to audio files to build the
    local library.
2.  Request the minimum media permission required for the Android
    version.
3.  If the user denies it, show a useful permission state with
    Scan/Choose folder or Open Settings actions as appropriate.
4.  Request notifications when the user starts or enables a feature that
    needs them, following Android platform rules.
5.  Request Bluetooth or nearby-device permissions only when a specific
    feature needs them.
6.  Request ringtone modification access only after the user selects Set
    as ringtone.
7.  Request local-network access only when the user enables LAN
    sync/listening party.
8.  Request network access only if optional online integrations are part
    of the selected build and the user enables them.
9.  Do not request broad file-management access if MediaStore and SAF
    are sufficient.
10. Do not request contacts, location, microphone, camera or unrelated
    permissions for ordinary music playback.

## 10.2 Permission transparency

For every permission:

-   Explain what it is used for.
-   Explain whether it is optional.
-   Explain what still works if denied.
-   Provide a route to Android Settings if permanently denied.
-   Monitor permission revocation while the app is running.
-   Do not repeatedly prompt after denial.
-   Do not block offline playback because an optional integration
    permission is missing.

## 10.3 Privacy controls

Add a Privacy section containing:

-   Offline-only mode.
-   Online integrations enabled/disabled state.
-   Per-provider connection status.
-   Clear online cache.
-   Clear search history.
-   Clear listening history.
-   Clear recommendation data.
-   Clear artwork cache.
-   Export personal data.
-   Disconnect service / remove credentials.
-   Privacy summary explaining what stays on device and what is
    transmitted when optional services are enabled.

Do not include secrets in logs, analytics, crash reports, backups or
share sheets.

------------------------------------------------------------------------

# 11 --- Responsiveness, accessibility and usability

Support:

-   Small phones.
-   Standard phones.
-   Large phones.
-   Foldables and unfolded layouts.
-   Tablets.
-   Landscape phones/tablets.
-   Split-screen and multi-window where possible.
-   Large system font sizes.
-   Display scaling.
-   Edge-to-edge system bars.
-   Punch-hole/camera cutouts.
-   Gesture and three-button navigation.

Test a representative width range from compact phone through tablet and
a font-scale range up to 2.0.

Rules:

-   No overlapping text, icons, buttons or cards.
-   Do not shrink text below readable sizes to force a layout to fit.
-   Use ellipsis only when the full text is available in a details view
    or accessible description.
-   Prioritize essential playback controls.
-   Hide or move secondary actions into an overflow area on narrow
    layouts.
-   Ensure artwork remains correctly proportioned.
-   Provide content descriptions and state descriptions.
-   Support TalkBack navigation order.
-   Do not use color as the only indicator of state.
-   Provide keyboard/focus behavior for supported external keyboards.
-   Provide accessible alternatives to swipe and drag gestures.
-   Respect reduced motion and system animation scale.
-   Maintain adequate contrast in every skin and player theme.

------------------------------------------------------------------------

# 12 --- Performance and stability

Optimize without sacrificing correctness:

-   Fast cold start.
-   No full-library query on the main thread.
-   Indexed Room queries for common filters and sorting.
-   Stable keys in lazy lists.
-   Avoid whole-screen recomposition from seek progress.
-   Throttle progress updates appropriately.
-   Bounded artwork and blur caches.
-   Cache keys must include stable identity and artwork
    modification/version information.
-   Cancel obsolete image loads.
-   Release audio effects and media resources correctly.
-   Avoid repeated database writes for every progress tick.
-   Avoid excessive widget refreshes.
-   Avoid infinite animations when the relevant UI is not visible.
-   Use battery-aware visualizer frame rates.
-   Recover from service disconnects.
-   Handle process death and app restart.
-   Avoid duplicate playback services or audio sessions.
-   Handle storage pressure and low-memory callbacks.
-   Do not run background scans without a clear user-visible reason.
-   Make scan and backup operations cancellable when safe.
-   Avoid battery optimization exemption requests unless a real,
    user-visible requirement justifies them.

------------------------------------------------------------------------

# 13 --- Error handling and recovery

Every subsystem must have an explicit error policy.

Cover:

-   Permission denial/revocation.
-   Empty library.
-   Interrupted scan.
-   Partial scan.
-   Unreadable folder.
-   SD-card removed.
-   Missing audio file.
-   Unsupported decoder/format.
-   Playback service failure.
-   Audio focus failure.
-   Equalizer unavailable.
-   USB/output device unavailable.
-   Lyrics missing/corrupt.
-   Artwork missing/corrupt.
-   Database migration failure.
-   Database corruption.
-   DataStore corruption.
-   Backup export failure.
-   Backup import failure.
-   Corrupt or hostile archive.
-   Duplicate backup files.
-   Insufficient storage.
-   SAF permission revoked.
-   Network timeout/provider failure.
-   AI quota/rate limit.
-   LAN pairing failure.
-   Cast/Android Auto unavailable.
-   Widget update failure.

Every error must provide:

1.  A concise human-readable explanation.
2.  The affected item or operation where safe.
3.  A practical recovery action.
4.  A retry option when retry is safe.
5.  No destructive automatic recovery.
6.  A diagnostic detail that can be copied/shared by the user without
    leaking secrets.

Use consistent error states and avoid exposing raw stack traces to
normal users.

------------------------------------------------------------------------

# 14 --- Settings information architecture

Consolidate all features into a coherent Settings structure. Do not
create hidden or orphaned settings pages.

Recommended sections:

1.  **Appearance**
    -   App theme, skins, wallpaper, dynamic colors, corner radius,
        navigation style, list density, motion.
2.  **Player**
    -   Player theme, player layout, live button arrangement, artwork,
        queue panel, mini-player, gestures.
3.  **Library**
    -   Scan sources, folder include/exclude, file-size/duration
        filters, duplicate policy, artist parsing, sorting, hidden
        files, missing tracks.
4.  **Playback**
    -   Speed, pitch, repeat, play mode, transitions, fades, headset
        behavior, audio focus, resume behavior.
5.  **Audio**
    -   Equalizer, presets, bass, reverb, virtualizer, ReplayGain, mono,
        output path, per-track profiles, capability information.
6.  **Lyrics**
    -   Local lyric discovery, import, editor, synchronization offset,
        optional provider settings.
7.  **Playlists & Queue**
    -   Smart playlists, saved queues, queue insertion behavior, M3U
        import/export.
8.  **History & Statistics**
    -   Listen-count threshold, history management, recap, Your Year,
        privacy controls.
9.  **Widgets & System**
    -   Widget guidance, quick settings tiles, Android Auto/Cast status
        when available.
10. **Backup & Transfer**
    -   Classic favourites JSON, Full backup (reference format),
        import/export reports, Music/Pulse Player folder workflow.
11. **Connections**
    -   Optional integrations, remote libraries, LAN sync, listening
        party, AI providers. Hide this section or show an explanatory
        unavailable state in builds that do not include network
        functionality.
12. **Privacy**
    -   Offline-only mode, data controls, permissions, cache,
        credentials.
13. **About**
    -   Version, licenses, acknowledgements, privacy summary, known
        limitations and support diagnostics.

All new features must have a discoverable UI entry point. A setting must
not exist only in documentation.

------------------------------------------------------------------------

# 15 --- Documentation and repository maintenance

Update documentation to reflect the actual implemented product:

-   README with feature overview, screenshots only when genuine, build
    instructions, permission explanation, offline/online behavior,
    supported formats and known limitations.
-   Feature matrix with WORKING / PARTIAL / BROKEN / MISSING /
    UNVERIFIED.
-   Architecture overview.
-   Navigation map.
-   Theme/skin/player-layout catalogue.
-   Optional integration list and privacy implications.
-   Backup and restore guide distinguishing Classic and Full backup.
-   Troubleshooting and recovery guide.
-   Accessibility notes.
-   Supported Android versions and tested devices.
-   Third-party dependency and license notices.
-   `brain.md` with Done / Tested / Not done / Known limits.
-   `docs/spec/SPEC_INDEX.md` updated to include this merge request and
    the new phase files.
-   `docs/TEMP_CODE.md` maintained and empty before release.

Never add fake badges, fake screenshots, invented test counts,
unverified compatibility claims or a license that has not been selected.

------------------------------------------------------------------------

# 16 --- Suggested implementation phases

Do not attempt the entire merge in one huge code-generation response.
Work in controlled phases. The original project's Spec Index and
fix-pack rules still apply. Add these merge phases to the index.

  -----------------------------------------------------------------------
  Phase                   Goal                    Main scope
  ----------------------- ----------------------- -----------------------
  M0                      Audit and merge plan    Inventory, preservation
                                                  matrix, gap matrix,
                                                  licensing, conflicts

  M1                      Stability baseline      Build, tests, data
                                                  backup, regressions,
                                                  broken existing
                                                  features

  M2                      Theme architecture      Theme tokens, registry,
                                                  global color audit,
                                                  Material You family

  M3                      Player layout registry  Atmospheric Queue,
                                                  layout previews,
                                                  responsive player host

  M4                      Live button             12+ presets,
                          arrangements            persistence, animation,
                                                  accessibility

  M5                      Library/indexing        Advanced scan filters,
                                                  artist parsing, genres,
                                                  tags, sorting

  M6                      Metadata and playlists  Tag editor, smart
                                                  playlists, M3U, saved
                                                  queue sessions

  M7                      Search and lyrics       Typo-tolerant search,
                                                  local lyrics, editor
                                                  and sync

  M8                      Audio enhancements      EQ profiles, per-track
                                                  settings, ReplayGain,
                                                  capability viewer

  M9                      History and discovery   Session stats, Daily
                                                  Mix, Smart Tracks, Lost
                                                  Memories

  M10                     Visual and interaction  Visualizers,
                          polish                  wallpapers, gestures,
                                                  widgets and quick tiles

  M11                     Optional integrations   Online lyrics/art, AI,
                                                  remote libraries, LAN,
                                                  Cast/Auto where
                                                  approved

  M12                     Backup hardening        Classic copy workflow
                                                  and Full
                                                  reference-format
                                                  verification

  M13                     Security, accessibility Permission audit,
                          and performance         privacy, responsive
                                                  tests, recovery

  M14                     Final acceptance        Full regression,
                                                  documentation, release
                                                  readiness and honest
                                                  report
  -----------------------------------------------------------------------

Each phase must be subdivided into small tasks. A task should normally
touch no more than six files and one subsystem. If a task requires more,
split it and explain the dependency.

After every task:

-   List changed files.
-   Explain behavior added or changed.
-   State tests actually run.
-   State build result.
-   State manual checks not yet performed.
-   Update `brain.md`.
-   Stop and wait for the next task if the workflow requires user
    approval.

------------------------------------------------------------------------

# 17 --- Verification and acceptance criteria

The merge is not complete until the following are verified against the
real project.

## 17.1 Preservation

-   Every existing Pulse Player feature is accounted for in the
    Preservation Matrix.
-   Existing screens and navigation remain accessible.
-   Existing nine player themes remain selectable.
-   Existing skin themes and wallpapers remain selectable.
-   Existing Classic backup format still imports and exports correctly.
-   Existing Full backup reference format still imports correctly.
-   Existing user settings and library data survive migrations.
-   No duplicate playback engine, queue, service or audio session has
    been introduced.

## 17.2 New features

-   Every approved Namida/PixelPlayer feature is classified and tracked.
-   Every local-core feature has a complete workflow.
-   Optional online/LAN features are disabled by default and
    privacy-disclosed.
-   Unsupported hardware features are accurately labelled.
-   New pages are reachable.
-   New settings are discoverable.
-   Smart playlists update from real data.
-   Search returns real ranked results.
-   Lyrics work with local files.
-   Statistics are session-based and accurate.
-   Queue persistence and ordering are stable.
-   Metadata editor writes only supported fields and reports failures.
-   Button arrangement picker has at least 12 working presets.
-   Atmospheric Queue is selectable and functional.

## 17.3 Themes and responsiveness

-   Every screen works in Light, Dark, AMOLED, System and supported
    Dynamic modes.
-   No hardcoded color leaks remain.
-   Every player theme has a correct preview and active state.
-   Atmospheric Queue remains legible.
-   No overlap on compact phones, large phones, foldables, tablets or
    landscape.
-   Large font scale does not hide essential controls.
-   Reduced motion disables decorative animation but preserves function.
-   TalkBack labels and navigation are usable.

## 17.4 Playback and audio

-   Playback continues when switching player layouts.
-   Playback continues when changing themes.
-   Queue reorder does not unexpectedly restart the current song.
-   Shuffle/repeat state remains correct.
-   Sleep timer and fades do not fight each other.
-   Audio effects are released correctly.
-   Unsupported formats show a useful error.
-   Background service and notification controls recover after lifecycle
    changes.
-   No volume controller bypasses the established audio transition
    owner.

## 17.5 Data and backup

-   No destructive migration.
-   Scan interruption does not mark all tracks missing.
-   Missing files do not delete favourites or playlists.
-   Classic JSON v1 schema remains unchanged.
-   Classic copied audio files never replace originals.
-   Full backup import validates before applying.
-   Full backup restore is idempotent where expected.
-   Corrupt archives do not mutate the database.
-   Secrets are excluded from backup.
-   Personal sample backup files are excluded from Git.

## 17.6 Quality gates

Run the actual project commands supported by its Gradle setup,
including:

-   Debug build.
-   Unit tests.
-   Instrumented tests where available.
-   Lint/static analysis.
-   Database migration tests.
-   Compose UI tests.
-   Backup validation and round-trip tests.
-   Manual device checks on the primary Infinix phone.
-   Responsive checks on emulator sizes and tablet configuration.
-   Airplane-mode checks.
-   Permission-denial and permission-revocation checks.
-   SD-card removal/reinsertion checks.
-   Low-storage and interrupted-operation checks.

The original specification mentions 137 existing unit tests. First
inspect the current test suite and confirm the actual baseline count. Do
not assume 137 is still correct. All existing tests must continue to
pass, and new tests must be added for new functionality.

If Google AI Studio cannot run Gradle, do not claim that the build
passed. Provide the exact commands for Android Studio/terminal
verification and mark the build/test status **NOT RUN**.

------------------------------------------------------------------------

# 18 --- Final delivery report

At the end, provide a truthful report with these sections:

1.  **Existing app audit:** what was already present and what was broken
    or incomplete.
2.  **Preserved features:** list and status.
3.  **New features implemented:** grouped by subsystem.
4.  **New themes, skins and layouts:** names and descriptions.
5.  **Optional integrations:** implemented, disabled, excluded or
    awaiting approval.
6.  **Offline/privacy behavior:** exact permissions and network behavior
    of the delivered build.
7.  **Database changes:** migrations and data-preservation evidence.
8.  **Backup verification:** Classic and Full backup results separately.
9.  **Tests:** exact commands and actual PASS/FAIL/NOT RUN status.
10. **Build:** exact command and actual result.
11. **Manual device checks:** completed and not completed.
12. **Known limitations:** hardware, format, API, licensing and platform
    limits.
13. **Changed files:** complete manifest.
14. **Remaining work:** no vague "everything else" statements.

Do not push to Git or overwrite remote history without explicit
authorization. Before any commit/push, inspect the branch, status,
remotes and diff. Never force-push. If the user has already explicitly
instructed a push for this task, push only after all quality gates pass
and report the exact commit.

------------------------------------------------------------------------

# 19 --- Master instruction to Google AI Studio

You are not being asked to create a new generic music player. You are
being asked to **upgrade and merge the existing Pulse Player project**.

First inspect the current project and its specifications. Record what
already exists. Preserve all working functionality and all established
designs. Then compare the project with the Namida and PixelPlayer
reference capabilities listed here. Add missing features as new,
integrated, selectable functionality.

Do not confuse a feature list with implementation. A feature is complete
only when its user workflow, data persistence, error handling,
permissions, responsive UI and tests are complete.

Do not remove an existing theme to make room for a new one. Do not
replace the current player with Atmospheric Queue. Add Atmospheric Queue
as another choice. Do not replace existing button controls; add the live
arrangement picker. Do not merge Classic and Full backup formats. Do not
claim online features work in a build that has no network permission. Do
not claim hardware audio capabilities without testing the real device
path.

The result must feel like one coherent, premium Pulse Player
application---not two apps pasted together. Keep the original Pulse
Player identity while adding the strongest useful ideas from Namida and
PixelPlayer.

**Start with Phase M0 only. Audit, inventory, identify conflicts and
produce the plan. Do not write implementation code until the audit is
complete.**

------------------------------------------------------------------------

# Annex A --- Detailed Technical Merge, Conflict Resolutions and Implementation Phases

> **You are a Principal Android Architect (Jetpack Compose, Modern
> Android Development).** You are extending the existing app **Pulse
> Player** (package `com.pulse.player`, Kotlin, Compose, Material 3,
> Media3, Room, DataStore, Glance), not replacing it. This annex is the
> detailed technical implementation layer of the unified master
> specification. Annex A §3 is authoritative only for its explicit
> conflict resolutions; all other main-specification requirements remain
> in force. Keep the existing task-size, complete-file, no-emoji,
> no-fake-data, no-dead-button, honest-verification and CHANGED FILES
> manifest rules from the existing project specifications.
>
> **Never delete, overwrite or weaken an existing feature, skin, theme,
> layout, setting or test.** Additions only. If an existing test must
> change because a spec changed (for example the theme count), change it
> deliberately, say so in the manifest, and never delete a test to make
> a build pass.
>
> **How to use this annex:** Do not paste the entire specification into
> one implementation request. Annex A §17 contains the phases, updated
> Spec Index rows and prompts 30--46. Attach the unified master
> specification, `SPEC_INDEX.md`, and only the project files named for
> the current phase.

------------------------------------------------------------------------

> **Integration note:** This annex preserves the detailed technical
> requirements and conflict table from the second supplied document. Its
> numbered sections are local to Annex A. The main specification's
> sections 00--19 remain the product-level requirements. Apply Annex A
> §3 only where it explicitly resolves a conflict; otherwise combine
> both layers without deleting requirements.

------------------------------------------------------------------------

## 0. Reading order and precedence

1.  `SPEC_INDEX.md` (extend it with phases 9--17, section 17.1).
2.  This file, sections 1--3 always; then only the sections named by the
    phase.
3.  Older files only as named in the phase table. Precedence: **Annex A
    (section 3 only) \> fix pack 12--17 \> base 00--11.**

## 1. Mission

Merge into Pulse Player **every feature** and **every design** of two
reference apps, without losing anything Pulse Player already has: -
**Namida** (Flutter, `github.com/namidaco/namida`): a feature-heavy
local + streaming music player with a Material 3 look. - **PixelPlayer**
(`github.com/PixelPlayerHQ/PixelPlayer`): a modern Compose player (the
project must verify the language and structure by reading the code;
treat it as a Compose-style reference).

Both live in the repository folder `New Player Concept/`. They are
**references for behaviour and design, not dependencies.** Plus these
additions requested by the owner: 1. New premier player layout
**Atmospheric Queue** (dark mist upper half, light "Up Next" card lower
half) --- section 7. 2. **Live Button Layout Switcher** with a large set
of presets usable while music plays --- section 8. 3. **Player layout
picker** reachable from the player while music plays --- section 7.6. 4.
**Classic favourites backup v2**: export into `Music/Pulse Player/` with
the audio files copied --- section 9. 5. Theme/colour correctness in
every mode --- section 10. 6. Full responsiveness --- section 11. 7.
Performance and polish --- section 12. 8. Comprehensive error handling
--- section 13. 9. First-launch permission onboarding --- section 14.
10. A detailed README and a final traceability audit --- sections 16 and
18.

## 2. What Pulse Player already has (do not rebuild what exists; verify it, keep it, extend it)

**Phase 0 rule:** audit the real project first. For every row below,
mark `PRESENT`, `PARTIAL` or `MISSING`. If `MISSING` or `PARTIAL`, build
it from the file named in the last column **in addition to** the new
work. Never rebuild a `PRESENT` item.

  --------------------------------------------------------------------------------------------------------------------------------
  Area                    What exists (from the pack)                                                      Spec file
  ----------------------- -------------------------------------------------------------------------------- -----------------------
  Navigation              Single Activity, state-based `Screen` + `AnimatedContent`; top tabs **For you ·  01, 05
                          Songs · Playlists · Folders · Albums · Artists**; drawer (phone), NavigationRail 
                          (medium), docked Now Playing panel (expanded)                                    

  Home                    Resume banner, Shuffle/Play pills, 4 widget tiles (Today, Favourites, Sleep      01, 05
                          timer, Equalizer), Recently added, Top picks, empty-library onboarding           

  Songs                   Header count, scanning chip, sort menu, multi-select, A--Z scroller, track menu  01, 05, 14
                          (play next, queue, playlist, favourite, details, ringtone, share), Missing songs 
                          chip                                                                             

  Playlists / Folders /   System playlists (My favourite, Recently added, Recently played, My top tracks), 01
  Albums / Artists /      user playlists (create, rename, reorder, delete), 250 ms debounced search in     
  Search / Detail         four sections                                                                    

  Now Playing             Song/Lyrics toggle, seek bar, transport, secondary row, queue sheet, sleep       01, 05
                          sheet, add-to-playlist sheet, mini player (spinning disc, marquee, progress      
                          line)                                                                            

  Player themes (9)       `classic, mountain, mesh, minimal, acoustic, vinyl, coast, starry, immersive`;   06, 12 §4, 16
                          carousel with APPLY/USING; Special badge; default `classic`                      

  Skins (8)               Plum, Aurora, Afterglow, Ocean, Sunrise, Forest, Mono, Pure black; procedural    05, 06
                          wallpapers; custom image wallpaper copied into private storage                   

  Theme modes             System / Dark / AMOLED / Light; dynamic accent from artwork; reduced motion      02, 05, 06

  Audio                   One ExoPlayer in `MusicPlaybackService`;                                         04, 13
                          `AudioTransitionController`/`FadingPlayer`; `SleepTimerEngine`; `ErrorPolicy`;   
                          platform `AudioFx` (equalizer, bass boost, virtualizer, preset reverb) with      
                          capability checks; speed 0.5--2.0x; stop after this track; sleep timer with 10 s 
                          fade; headset-unplug pause                                                       

  Queue                   `QueueModel` (original/shuffled order, seed, no Media3 shuffle), persistent      14
                          snapshot restored paused                                                         

  Library                 Recoverable MediaStore scan, identity model (`ms:`/`saf:`/`copy:`), SAF folder   14
                          scan, explicit Copy workflow, SD card handling, permission state machine,        
                          visible-count query, artwork cache                                               

  Statistics              `SessionTracker`, `StatsPolicy`, month keys, monthly recap, share card           15, 08

  Backup                  **Classic** favourites JSON (`pulseplayer.favorites` v1, fuzzy matcher) and      07, 10, 11, 15
                          **Full backup (reference format)** (ZIP-in-`.wav`) kept separate                 

  Lyrics                  User-picked `.lrc`/`.txt` import, synced highlight                               16

  Other                   Glance widget (2x2/4x1/4x2), ringtone with pending action, launcher icon, README 08, 13
  --------------------------------------------------------------------------------------------------------------------------------

## 3. Conflict resolutions (apply exactly; this is where Annex A overrides older files)

  -------------------------------------------------------------------------------------------------------------------------------------------------------------------
  \#                Old rule                      New request                                          Resolution
  ----------------- ----------------------------- ---------------------------------------------------- --------------------------------------------------------------
  C1                "No INTERNET permission.      Streaming, LRCLIB, Deezer, AI, Cast, servers         **Two product flavors** in `app/build.gradle.kts`: `offline`
                    Ever." (00 §1)                                                                     (identical to today, **no INTERNET permission**, all network
                                                                                                       code excluded at build time) and `full` (adds `INTERNET`,
                                                                                                       `ACCESS_NETWORK_STATE`). All network code lives in package
                                                                                                       `com.pulse.player.net` behind **one `NetworkGateway`** and a
                                                                                                       **master switch "Online features" (default OFF)** plus a
                                                                                                       per-service switch. The README states the permission claim per
                                                                                                       flavor. `full` is the day-to-day build.

  C2                Out of scope: streaming,      In scope now                                         Allowed only as opt-in, gated features (section 15 and 17).
                    accounts, online lyrics/art,                                                       **Still forbidden: ads, analytics, telemetry, crash uploads,
                    tag editing, Android Auto (01                                                      tracking.**
                    §6)                                                                                

  C3                Crossfade "not included" (00  Smart crossfade, beat matching, gapless              Implement as an **experimental, default-OFF**
                    §9)                                                                                `CrossfadeEngine` inside `PlaybackEngine` (section 15.4).
                                                                                                       Gapless stays the default path. Honest label "Experimental".

  C4                Only                          Crossfade needs two volumes; ReplayGain, per-track   The controller is extended to be the sole writer of **both**
                    `AudioTransitionController`   volume                                               engine volumes. ReplayGain, per-track gain and loudness are
                    writes `player.volume`                                                             applied by an `AudioProcessor` gain stage, **never** by
                                                                                                       writing `volume`. The grep gate becomes "no `.volume =`
                                                                                                       outside `AudioTransitionController`".

  C5                Only `SessionTracker` writes  History import (Last.fm, Spotify, ListenBrainz,      Live listening is still written **only** by `SessionTracker`.
                    play history                  YouTube), editable history                           Imports and edits go through one `HistoryWriter` (single gate)
                                                                                                       and mark rows with a new `origin` column (`LIVE`,
                                                                                                       `IMPORT_LASTFM`, `IMPORT_SPOTIFY`, `IMPORT_LISTENBRAINZ`,
                                                                                                       `IMPORT_YOUTUBE`, `MANUAL`). Grep gate: only these two classes
                                                                                                       call `play_events` inserts.

  C6                "Themes are presentation      Crossfade engine has two decoders                    The rule still applies to themes/layouts. The service has
                    only: no second player"                                                            **one** `MediaSession` facade (`FadingPlayer`) and **one**
                                                                                                       owner (`PlaybackEngine`); the crossfade engine's second
                                                                                                       ExoPlayer is private to it and never exposed to UI.

  C7                Media3 shuffle never enabled  Namida Track Play Mode, insert-after-latest, repeat  All implemented in `QueueModel` and `PlayerController`; Media3
                                                  N times                                              shuffle remains disabled.

  C8                Never move/rename/delete      Classic export copies favourites into                Copy only after a confirmation dialog listing count and size
                    music; copy only through the  `Music/Pulse Player/`; tag editor writes tags        (reuses file 14 §4.2). Tag editing modifies file contents
                    explicit Copy action                                                               **only** after an explicit confirm, with an optional "keep a
                                                                                                       backup copy" switch; never batch-edit silently. Sources are
                                                                                                       never moved or deleted.

  C9                No `MANAGE_EXTERNAL_STORAGE`  Folder detection, tag writes                         Still forbidden. Use MediaStore (own files and `RELATIVE_PATH`
                                                                                                       queries), SAF persisted trees, and
                                                                                                       `MediaStore.createWriteRequest` for tag writes.

  C10               Unknown player-theme id falls New themes from Namida/PixelPlayer; Atmospheric      Keep **two separate registries**: `PlayerThemes` (visual
                    back to `classic`; "exactly 9 Queue is the premier layout                          themes; default stays `classic`; count becomes N after this
                    themes" (12 §4, 17                                                                 merge) and `PlayerLayoutMode` (structure of the player;
                    `ThemeRegistryTest`)                                                               default and unknown-fallback is `ATMOSPHERIC_QUEUE`). Update
                                                                                                       `ThemeRegistryTest` to the new canonical list and add
                                                                                                       `PlayerLayoutModeTest`. Update file 12 §4's table in the repo.

  C11               `Track` data class in file 04 New spec shows a tiny                                Do **not** replace the existing `Track`. The layout uses its
                                                  `Track(id,title,artist,duration:String,isPlaying)`   own
                                                                                                       `QueueRowUi(id,title,artist,durationLabel,isCurrent,artRef)`
                                                                                                       mapped from `QueueItem`.

  C12               "No copyrighted images"       "Copy the exact designs"                             Recreate the **look** (layout, spacing, shape, colour logic,
                                                                                                       motion) in code from the references. Do not copy image assets,
                                                                                                       fonts or icons unless their licence allows it; do not copy
                                                                                                       source code verbatim (section 4). Backgrounds stay procedural.

  C13               `Visualizer` effect needs     Visualizers, particles                               Do **not** request `RECORD_AUDIO`. Tap PCM in an
                    `RECORD_AUDIO`                                                                     `AudioProcessor` (peak + small FFT) and publish a throttled
                                                                                                       `AudioLevels` flow.

  C14               "No fake data"                `@Preview` needs sample data                         Sample data is allowed **only** inside `@Preview` functions in
                                                                                                       a `preview/` source file never referenced by production code.

  C15               `#D4AF37` amber is mandated   Contrast rule ≥ 4.5:1 (file 02)                      Use `#D4AF37` for the equalizer bars, a left accent bar and
                    for the active queue row                                                           dark-surface text. On the light card use a darker amber token
                                                                                                       (`ActiveAmberOnLight`, about `#8A6D00`, about 4.7:1 on
                                                                                                       `#F9F9FB`; **verify with a contrast unit test**). The active
                                                                                                       row also uses bold weight and the animated bars as a second
                                                                                                       cue.

  C16               "Request permissions only on  Request everything on first launch                   Replace with the onboarding flow in section 14 (rationale
                    Scan" (03 §8)                                                                      first, each step skippable, features degrade gracefully).

  C17               README says "no internet      Flavors                                              README documents both flavors (section 16).
                    permission"                                                                        
  -------------------------------------------------------------------------------------------------------------------------------------------------------------------

## 4. Research protocol and licensing gate (do this before building any parity feature)

1.  **Read the two reference folders and the upstream repos.** Do not
    rely on the feature lists in this file alone; they came from the
    owner's description and may be incomplete or out of date.
2.  Produce `docs/spec/PARITY_INVENTORY.md` with one row per feature
    **and** per screen/skin/theme/layout found:
    `ID | Source app | Name | Where it lives (file/class) | What it does | Pulse equivalent | Status (PRESENT/PARTIAL/NEW) | Phase | Settings location`.
    Start from the ID tables in section 6 and **add rows you find** that
    are not listed. Do not drop any.
3.  **Licence check.** Record each reference app's licence and each
    third-party library's licence in `docs/THIRD_PARTY_NOTICES.md`.
    Namida is licensed under a strong copyleft licence (verify:
    GPL-3.0). Copying its **code** into Pulse Player would oblige Pulse
    Player to the same licence. **Default policy: re-implement from
    observed behaviour and from the design, do not paste code.** If the
    owner wants to adopt a copyleft licence for the whole app, that is
    an owner decision recorded in the README; stop and ask before
    copying any code.
4.  Streaming/downloading from video sites and SponsorBlock-style
    features can violate a platform's terms and app-store policies.
    Build them last, behind the `full` flavor and their own switch, and
    record the legal decision in `docs/DECISIONS.md` (section 15.9). Do
    not build them silently.
5.  Anything you cannot find in the references or cannot implement on
    Android: do **not** hide it. Show it in Settings as disabled with a
    one-line reason, and list it in the final report.

## 5. Architecture additions

### 5.1 Four independent customisation axes (never mix them)

  -----------------------------------------------------------------------------
  Axis              What it controls     Picker            Persisted key
  ----------------- -------------------- ----------------- --------------------
  **Theme mode /    Light / Dark /       Settings →        `themeMode`,
  colour source**   AMOLED / System,     Appearance →      `colorSource`,
                    plus colour source:  Themes            `seedColor`
                    *Pulse palette*,                       
                    *Material You                          
                    (wallpaper)*,                          
                    *Artwork-driven*,                      
                    *Custom seed colour*                   

  **Skin**          App background       Skin theme screen `skinId`,
                    gradient/wallpaper                     `wallpaper`
                    (8 existing + all                      
                    new ones)                              

  **Player theme**  Visual treatment of  Player theme      `playerThemeId`
                    the Now Playing      carousel          
                    artwork area (9                        
                    existing + new)                        

  **Player layout   Structure of the     Layout picker     `playerLayoutMode`
  mode**            whole player screen  sheet in the      
                    (Atmospheric Queue,  player + Settings 
                    Classic, Widescreen, → Layouts         
                    Waveform, ...)                         

  **Button layout   Arrangement of       Button-layout     `buttonPresetId` (+
  preset**          transport and        picker sheet in   optional per-layout
                    quick-action buttons the player +      override map)
                                         Settings →        
                                         Layouts           
  -----------------------------------------------------------------------------

Every picker shows live previews. Applying one never restarts playback.

### 5.2 Packages (add; do not move existing)

`com.pulse.player.net` (gateway, lrclib, deezer, ai, sources, party) ·
`.audio` (processors, parametric EQ, crossfade, replaygain, audio-path)
· `.library` (separators, tag editor, smart playlists, search index,
metadata) · `.ui.layouts` · `.ui.buttons` · `.ui.tokens` · `.core.error`
· `.core.flags`.

### 5.3 Capability registry (no dead buttons)

A `Capabilities` object evaluated at runtime: API level, `audiofx`
availability, USB/bit-perfect support, float-output support, network
flavor, Google Play services (Cast), TagLib binding loaded,
battery-exemption allowed. Every setting row asks the registry. States:
`Available`, `NeedsPermission`, `NeedsOnlineSwitch`,
`Unsupported(reason)`. Unsupported rows are **visible, disabled, with
the reason**.

### 5.4 Source abstraction

`MusicSource` interface
(`id, type, displayName, list(), open(trackId), capabilities`).
Implementations: `MediaStoreSource`, `SafSource`, `CopiedSource`
(exist), then `SubsonicSource`, `JellyfinSource`, `WebDavSource`,
`SmbSource`, `YouTubeSource` (phase 16). Extend the identity model (file
14 §2) with `sourceType` values `SUBSONIC, JELLYFIN, WEBDAV, SMB, YT`
and ids `sub:<server>:<id>`, `jf:<server>:<id>`, `dav:<sha1>`,
`smb:<sha1>`, `yt:<videoId>`. **Reconcile availability only within one
source** (the scan rule from file 14 §1 applies per source).

### 5.5 Schema version 4 (one migration `MIGRATION_3_4`, exported schema JSON, migration test, pre-migration DB copy as in file 17 §3)

Adjust names to the project; keep defaults on every new column. -
`tracks`: add `genre`, `genresRaw`, `albumArtistNorm`, `year` (exists),
`bitrate`, `sampleRate`, `bitDepth`, `codec`, `replayGainTrackDb`,
`replayGainAlbumDb`, `bpm`, `language`, `hasEmbeddedLyrics`. - New:
`track_tags(trackId, tag)`, `track_moods(trackId, mood)`,
`track_meta(trackId, rating 0..5, language, note)`,
`track_audio_profile(trackId, speed, pitchSemitones, gainDb, eqJson, presetId)`,
`bookmarks(id, trackId, positionMs, label, createdAtMs)`,
`chapters(trackId, startMs, title)`,
`saved_queues(id, name, createdAtMs, currentIndex, positionMs)` +
`saved_queue_items(queueId, position, trackId)`,
`smart_playlists(id, name, rulesJson, createdAtMs)`,
`artist_images(artistNorm, url, filePath, fetchedAtMs, source)`,
`sources(id, type, name, baseUrl, authRef, enabled, lastSyncMs)`,
`cache_entries(trackId, filePath, bytes, priority, lastUsedMs)`,
`waveforms(trackKey, peaksBlobPath, version)`. - `play_events`: add
`origin TEXT NOT NULL DEFAULT 'LIVE'`. - `lyrics`: add
`source TEXT NOT NULL DEFAULT 'IMPORT'`,
`format TEXT NOT NULL DEFAULT 'LRC'` (`LRC`, `TTML`, `PLAIN`,
`WORD_LRC`). - Secrets (server passwords, AI keys, tokens) are **never**
in Room or plain DataStore: use a Keystore-backed encrypted store.

### 5.6 Settings information architecture (all new features live here; nothing is hidden elsewhere)

Settings → **Appearance** (theme mode, colour source, seed colour, skin,
player theme, player layout, button layout, corner radius, navigation
style, wallpaper, animated background, visualizer, particles, party-mode
edge glow, reduced motion, battery-saver visuals) · **Library** (scan,
folders and exclusions, size/duration filters, duplicates, artist/genre
separators, feat./ft. extraction, album-artist grouping, default sorts,
page/tab order and visibility, folder images and `.info.txt`, tag
editor) · **Playback** (gapless, fade, crossfade, skip silence, speed,
pitch, repeat-N, track play mode, insert behaviour, sleep timer modes,
pausing scenarios, persistent queues, history rules) · **Sound**
(effects, parametric EQ, AutoEQ import, presets, ReplayGain, loudness
enhancer, mono, output device, bit-perfect/USB/float, audio path viewer)
· **Lyrics** (sources and priority, online fetch, editor defaults) ·
**Gestures** (swipe actions, artwork gestures, layout-cycle gesture) ·
**Online & Privacy** (master switch, per-service switches, data-saver,
what each service sends) · **Sources** (servers, YouTube, cache) ·
**AI** (provider, key, consent preview) · **Connectivity** (Cast,
Android Auto, widgets, Listening Party, local sync) · **Stats &
History** (rules, import, export, edit) · **Backup & Transfer**
(Classic, Full backup) · **Permissions** (status of every permission
with a fix button) · **About** (version, flavor, licences).

## 6. Feature catalogue (traceability IDs; every ID must end the project as Implemented, Gated, or Unsupported-with-reason)

Legend --- Priority: **P0** core, **P1** important, **P2**
advanced/optional. Phase numbers refer to section 17. "Limit" = honest
device/legal constraint to show in the UI.

### 6.1 Namida --- Library and indexing

  ----------------------------------------------------------------------------------------------------------------------
  ID         Feature            Build approach in Pulse Player            Settings     Ph         Pri        Limit
                                                                          home                               
  ---------- ------------------ ----------------------------------------- ------------ ---------- ---------- -----------
  NAM-L1     Indexer + **tag    TagLib binding (choose an actively        Library →    12         P1         Needs write
             editor** (TagLib)  maintained Android binding; verify        Tag editor                         grant; some
                                licence); read all tags into `tracks`;                                       formats
                                editor screen with                                                           read-only
                                title/artist/album/album                                                     
                                artist/genre/year/track/lyrics/artwork;                                      
                                writes via                                                                   
                                `MediaStore.createWriteRequest`; confirm                                     
                                dialog; optional backup copy; rescans the                                    
                                file afterwards                                                              

  NAM-L2     Artists and genres Configurable delimiter list (`;`, `,`,    Library →    12         P1         
             **separators**     `/`, `&`, `feat.`...) + exceptions list   Separators                         
                                ("AC/DC", "Tyler, The Creator"); a track                                     
                                can belong to several artists/genres;                                        
                                applied at index time and re-runnable                                        

  NAM-L3     **Prevent          Duplicate = same normalised title +       Library →    12         P1         
             duplicate tracks** artist + duration within 2 s (+ optional  Duplicates                         
                                size); setting "hide duplicates" is                                          
                                display-only (never marks unavailable);                                      
                                duplicates screen to review                                                  

  NAM-L4     Minimum file size  Display-only filters (extends the         Library →    12         P0         
             and duration       existing min-duration); size in KB        Filters                            
             filters                                                                                         

  NAM-L5     Folders-based      Exists; add per-folder include/exclude    Library →    12         P0         
             library with       tree, "show only these folders" mode      Folders                            
             excluded folders                                                                                

  NAM-L6     Sort by almost any Sort sheet with title, artist, album,     Library →    12         P0         
             track/album        album artist, genre, year, duration,      Sorting                            
             property           size, bitrate, date added, date modified,                                    
                                play count, last played, rating, BPM,                                        
                                file name, folder; asc/desc; remembered                                      
                                per page                                                                     

  NAM-L7     Folder images      Artwork resolution order: embedded →      Library →    12         P2         
             (`cover.jpg`,      folder image → generated;                 Sidecar                            
             `folder.jpg`...)   `<name>.info.txt` sidecar (read the       files                              
             and `.info.txt`    Namida source for the exact format,                                          
                                document it in `docs/INFO_TXT.md`)                                           

  NAM-L8     Extract feat./ft.  Parser on title/artist; creates artist    Library →    12         P1         
             artists into       links without altering file tags; toggle  Separators                         
             Artists tab                                                                                     
  ----------------------------------------------------------------------------------------------------------------------

### 6.2 Namida --- Look and feel

  ------------------------------------------------------------------------------------------------------------------------------------
  ID         Feature            Build approach                       Settings     Ph         Pri        Limit
                                                                     home                               
  ---------- ------------------ ------------------------------------ ------------ ---------- ---------- ------------------------------
  NAM-U1     Material 3-like    New `colorSource` options;           Appearance → 10         P0         
             theme + dynamic    transitions animate (500 ms);        Themes                             
             colours from       contrast-guarded                                                        
             current artwork                                                                            

  NAM-U2     Pages: Home,       Existing tabs stay. Add pages:       Library →    12         P0/P1      Moods/Tags/Ratings/Languages
             Tracks, Albums,    Genres, Queues (saved sessions),     Pages                              are user data (empty until
             Artists,           Most Played (range chips:                                               used)
             **Genres**,        today/week/month/year/all/custom),                                      
             Playlists,         Moods, Tags, Ratings (1--5 stars),                                      
             Folders,           Languages, Stats. Page order and                                        
             **Queues**, **Most visibility configurable; unused                                         
             Played**,          pages live in a "More" drawer                                           
             **Moods**,         section                                                                 
             **Tags**,                                                                                  
             **Ratings**,                                                                               
             **Languages**,                                                                             
             **Stats**                                                                                  

  NAM-U3     **Waveform         Decode PCM once in the background    Appearance → 11         P1         Generation is per-track and
             seekbar**          (`MediaCodec`/Media3 extractor) into Seek bar                           takes time; show progress
                                \~200--400 peak buckets, cache per   style                              
                                track in `waveforms`; seek bar                                          
                                variant draws bars, played part                                         
                                highlighted; falls back to normal                                       
                                slider while generating or if decode                                    
                                fails                                                                   

  NAM-U4     **Widescreen       `PlayerLayoutMode.WIDESCREEN`        Layouts      11         P0         
             player layout**    (artwork left, controls/queue right)                                    
                                used automatically at width ≥ 600 dp                                    
                                or chosen manually                                                      

  NAM-U5     **Visualizers**,   Audio tap (C13) → `AudioLevels`;     Appearance → 11         P1         Off when reduced motion or
             animated           visualizer styles: bars, wave,       Visualizer                         battery saver
             backgrounds,       circle, dots; animated backgrounds                                      
             custom wallpapers  procedural; custom wallpapers exist                                     

  NAM-U6     Extensive          Section 5.6                          Settings     9          P0         
             customisation in                                                                           
             Settings                                                                                   

  NAM-U7     **Animating        Artwork scale/glow pulse and         Appearance → 11         P2         
             thumbnail &        particle canvas driven by            Particles                          
             particles**        `AudioLevels`                                                           
             reacting to audio                                                                          
             peak                                                                                       

  NAM-U8     **Miniplayer Party Mini player border glow that         Appearance → 11         P2         
             Mode** (edge       breathes with the beat; toggle       Party mode                         
             breathing effect)                                                                          

  NAM-U9     Swipe actions and  Configurable per side (play next,    Gestures     12         P1         Layout-cycle gesture must not
             artwork gestures   add to queue, favourite, add to                                         clash (section 7.6)
                                playlist, delete from playlist,                                         
                                select); artwork: swipe =                                               
                                next/previous, double-tap = ±10 s,                                      
                                long-press = options                                                    
  ------------------------------------------------------------------------------------------------------------------------------------

### 6.3 Namida --- Sound

  --------------------------------------------------------------------------------------------------------------------------------------------------
  ID         Feature          Build approach                                               Settings home Ph         Pri        Limit
  ---------- ---------------- ------------------------------------------------------------ ------------- ---------- ---------- ---------------------
  NAM-S1     **Parametric     `ParametricEqProcessor` (cascaded biquads: peaking, low/high Sound →       13         P1         CPU cost; disabled in
             equalizer +      shelf, low/high pass; up to 10 bands; preamp). Import AutoEQ Parametric EQ                       bit-perfect mode
             AutoEQ import**  "ParametricEQ.txt" (`Filter n: ON PK Fc … Hz Gain … dB Q …`,                                     
                              `Preamp: … dB`) and GraphicEQ text; preview curve; save                                          
                              presets. Mutually exclusive with platform `AudioFx` EQ                                           
                              (toggle, to avoid double processing)                                                             

  NAM-S2     **Bit-perfect,   Float:                                                       Sound →       13         P2         **Android 14+ and
             USB DAC direct,  `DefaultRenderersFactory.setEnableAudioFloatOutput(true)`.   Output                              supporting hardware
             exclusive mode,  Bit-perfect/exclusive: Android 14+ `AudioManager` preferred                                      only**; otherwise
             32-bit float**   mixer attributes with bit-perfect behaviour, only if the                                         visible, disabled,
                              routed device supports it; show detected capability                                              reason shown.
                                                                                                                               Disables all effects
                                                                                                                               while active (state
                                                                                                                               it)

  NAM-S3     **Audio path     Live list: source file (codec, sample rate, bit depth,       Sound → Audio 13         P1         
             viewer**         channels, bitrate) → decoder → each active processor (EQ,    path                                
                              gain, crossfade, effects) → output sink → device (type,                                          
                              name, rate). Built from Media3 analytics +                                                       
                              `AudioManager.getDevices`                                                                        

  NAM-S4     Output device    `setPreferredAudioDevice(AudioDeviceInfo)` (API 23+); mono   Sound →       13         P1         Bluetooth names need
             selection,       via a `ChannelMixingAudioProcessor`                          Output                              `BLUETOOTH_CONNECT`
             **mono audio**                                                                                                    (API 31+)

  NAM-S5     Effects: **bass  Bass: platform `BassBoost` plus optional low-shelf           Sound →       13         P1/P2      Quality varies;
             enhancer,        processor. Reverb: platform preset reverb exists; add a      Effects                             labelled
             reverb, virtual  processor reverb with size/damping (P2). Virtual surround:                                       "approximate"
             surround, 8D     platform `Virtualizer` plus crossfeed processor. 8D: LFO                                         
             audio, vocal     auto-pan processor (period 2--20 s, depth). Vocal remover:                                       
             remover**        mid-side centre cancellation processor                                                           

  NAM-S6     **Smart          Gapless: default behaviour (verify gapless flags per         Playback →    13         P1/P2      Experimental
             crossfade with   format). Fade: exists. Skip silence:                         Transitions                         
             beat matching**, `ExoPlayer.setSkipSilenceEnabled`. Crossfade + beat                                              
             gapless,         matching: section 15.4                                                                           
             play/pause fade,                                                                                                  
             **skip silence**                                                                                                  

  NAM-S7     **Speed and      Independent speed (0.25--3.0x) and pitch (-12...+12          Playback →    13         P1         
             pitch**          semitones; "432 Hz" preset = ratio 432/440); platform        Speed and                           
             (semitones, 432  `LoudnessEnhancer`                                           pitch                               
             Hz) and                                                                                                           
             **loudness                                                                                                        
             enhancer**                                                                                                        

  NAM-S8     **Per-track**    `track_audio_profile`; applied on track transition through   Sound →       13         P1         
             speed, pitch,    `PlaybackEngine`; "Save for this track" in the player menu;  Presets                             
             volume,          global sound presets (named bundles)                                                             
             equalizer; sound                                                                                                  
             presets                                                                                                           

  NAM-S9     **ReplayGain**   Read `REPLAYGAIN_*` tags; track/album mode, preamp, clipping Sound →       13         P1         Needs tags; "no
                              guard; applied via gain `AudioProcessor` (C4)                ReplayGain                          ReplayGain tags"
                                                                                                                               count shown
  --------------------------------------------------------------------------------------------------------------------------------------------------

### 6.4 Namida --- Lyrics and smart features

  ----------------------------------------------------------------------------------------------------------------------------------------------------------------
  ID         Feature           Build approach                                                         Settings home  Ph         Pri        Limit
  ---------- ----------------- ---------------------------------------------------------------------- -------------- ---------- ---------- -----------------------
  NAM-Y1     Lyrics: synced,   Parsers for LRC, enhanced LRC (`<mm:ss.xx>`), TTML; editor with        Lyrics         12         P1         
             plain,            tap-to-timestamp while playing; saves to `lyrics` (and optionally a                                         
             **word-synced**   `.lrc` sidecar via SAF). Source priority: embedded tag → sidecar →                                          
             (LRC enhanced /   saved → LRCLIB (opt-in) → import                                                                            
             TTML) +                                                                                                                       
             **editor**                                                                                                                    

  NAM-Y2     **Smart           Rule tree (AND/OR groups) over:                                        Playlists      12         P1         
             playlists** with  title/artist/album/genre/tag/mood/rating/language/year/duration/play                                        
             flexible rules,   count/last played/date added/folder/favourite; compiled to SQL;                                             
             usable in search  live-updating; searchable via `smart:` prefix                                                               

  NAM-Y3     **Listening       LAN only by default (section 16.3). Online variant requires a relay    Connectivity   16         P2         Needs
             Party** (queue    the owner provides: not built unless specified                                                              `NEARBY_WIFI_DEVICES`
             sync with                                                                                                                     (API 33)
             friends, online                                                                                                               
             or LAN)                                                                                                                       

  NAM-Y4     **Local network   Pairing code, encrypted transport, per-category merge preview, same    Connectivity   16         P2         
             sync** of app     rules as Full backup (file 15 §2.4)                                                                         
             data (history,                                                                                                                
             playlists,                                                                                                                    
             favourites,                                                                                                                   
             queues, lyrics)                                                                                                               

  NAM-Y5     **Bookmarks and   `bookmarks` table; list in player menu; chapters from ID3 `CHAP`, MP4  Playback       12         P1         
             chapters**        chapters; chapter list sheet                                                                                

  NAM-Y6     **Stats charts    Compose-canvas charts from `play_events` (time per day/week/month, top Stats &        12         P1         Counts come from
             and "Your Year"** tracks/artists/albums/genres, hours heat-map, streaks); "Your Year"    History                              `counted = 1` sessions
                               story pages with a share card (extends recap, file 08)                                                      

  NAM-Y7     **Search** by     Room FTS5/FTS4 index over normalised fields + lyrics; fuzzy fallback   Library →      12         P0         
             title, artist,    (trigram or Levenshtein ≤ 2) ranked after exact; 250 ms debounce       Search                               
             album, filename,                                                                                                              
             lyrics with                                                                                                                   
             **typo                                                                                                                        
             tolerance**                                                                                                                   

  NAM-Y8     **Persistent      Save/load named queue sessions (the "Queues" page); autosave of the    Playback       12         P1         
             queues**          last queue exists (file 14 §7)                                                                              

  NAM-Y9     **Flexible        Editable (delete/add/edit entries), import, export; counting rules     Stats &        12         P1         
             history**         configurable (min seconds, min percentage) --- the constants in        History                              
                               `StatsPolicy` become settings with safe bounds                                                              

  NAM-Y10    **Most Played**   Page + playlist; range chips and custom range                          Library →      12         P0         
             in any time range                                                                        Pages                                

  NAM-Y11    Custom playlist   Playlist cover from a picked image (copied to private storage) or auto Playlists      12         P1         
             order and         collage; M3U/M3U8 import/export through SAF; map entries with the                                           
             artworks; **M3U** Classic matcher tiers                                                                                       
             and server                                                                                                                    
             auto-import                                                                                                                   

  NAM-Y12    **Global tracks   Selection persists across pages with one floating selection bar (play, Gestures       12         P1         
             selection**       queue, playlist, favourite, tag, delete from playlist)                                                      

  NAM-Y13    **Sleep timer**   Add "after N tracks" mode to `SleepTimerEngine`; scenario toggles:     Playback →     13         P1         
             by tracks or      pause on call (focus), pause or duck on notification, pause when media Sleep and                            
             minutes;          volume is 0 and resume when raised                                     pausing                              
             **pausing                                                                                                                     
             scenarios**                                                                                                                   
             (calls,                                                                                                                       
             notifications,                                                                                                                
             volume 0)                                                                                                                     

  NAM-Y14    **Smort Tracks**  Smart generated mixes from local stats: "Smort" (verify the exact      For you        12         P2         Hidden until data
             and **Lost        Namida meaning in its source), "Lost Memories" (tracks you played on                                        exists
             Memories**        this date in earlier years, or loved but not heard for N months)                                            

  NAM-Y15    **Track Play      When a track is tapped, the generated queue is one of: selected only / Playback →     12         P1         
             Mode**            current list or search results / its album / first artist's tracks /   Track play                           
                               first genre's tracks                                                   mode                                 

  NAM-Y16    **Insert after    `QueueModel` remembers the last "play next" inserted id; new inserts   Playback →     12         P1         
             latest inserted** go after it; toggle between this and "directly after current"          Queue                                

  NAM-Y17    **Repeat for N    `RepeatState(mode, remaining)`; repeat button long-press sets N        Playback →     12         P1         
             times**           (2--99); badge shows remaining; sessions count each loop (file 15      Repeat                               
                               §1.2)                                                                                                       

  NAM-Y18    **Import          File-based importers first (Takeout JSON, Spotify extended history     Stats &        15         P2         
             history** from    JSON, ListenBrainz export, Last.fm export CSV/JSON), API-based         History                              
             YouTube, Last.fm, optional behind the online switch; all rows through `HistoryWriter`                                         
             Spotify,          with `origin`; match to the library by the tier rules (file 10 §5);                                         
             ListenBrainz      preview with counts before commit; unmatched list                                                           
  ----------------------------------------------------------------------------------------------------------------------------------------------------------------

### 6.5 Namida --- Streaming and YouTube (all behind flavor `full` + "Online features" + service switch; see section 15.9 legal gate)

  -------------------------------------------------------------------------------------------
  ID             Feature              Build approach            Ph             Pri
  -------------- -------------------- ------------------------- -------------- --------------
  NAM-V1         Best video/audio     Quality policy object     16             P2
                 quality, audio only, (Wi-Fi vs mobile,                        
                 **data saver**       audio-only toggle);                      
                                      respects                                 
                                      `ConnectivityManager`                    
                                      metered state                            

  NAM-V2         **Multiple profiles  Profile model; account    16             P2
                 sign-in**; feed,     data kept in encrypted                   
                 subscriptions,       store; decision required                 
                 playlists,           (15.9)                                   
                 notifications                                                 

  NAM-V3         **Multi-library**    `MusicSource`             16             P1/P2
                 Local / YT /         implementations (5.4);                   
                 Subsonic / Jellyfin  Subsonic and Jellyfin                    
                 / WebDAV / SMB with  REST; WebDAV (PROPFIND);                 
                 **offline caching**  SMB via an                               
                                      LGPL-compatible library;                 
                                      unified library view with                
                                      a source filter chip;                    
                                      per-source sync and                      
                                      reconcile                                

  NAM-V4         **Video view** with  Media3 `PlayerView`       16             P2
                 gestures (volume,    surface for video items;                 
                 brightness, seek,    gesture layer; same                      
                 fullscreen, speed)   single playback service                  

  NAM-V5         **SponsorBlock**,    Public APIs behind the    16             P2
                 Return YouTube       gateway; skip/mute                       
                 Dislike, segments    segments by category;                    
                 and heatmap          heatmap on the seek bar                  

  NAM-V6         **Subtitles and      Media3 text tracks; style 16             P2
                 captions**           options                                  

  NAM-V7         **Downloads,         Download manager          16             P1
                 caching, offline     (`DATA_SYNC` foreground                  
                 playback**, cache    service type), app-owned                 
                 priority             storage, LRU with                        
                                      priorities                               
                                      (`cache_entries`),                       
                                      pause/resume                             

  NAM-V8         Split downloads by   Post-process step using   16             P2
                 chapters; remove     chapters and segments                    
                 sponsor segments                                              

  NAM-V9         Download **tags      Template editor           16             P2
                 config and filename  (`%artist% - %title%`),                  
                 builder**            tag mapping, preview                     

  NAM-V10        **Auto               Rule-based parser         16             P2
                 title/artist/album   (patterns, "Artist -                     
                 extraction** from    Title", brackets,                        
                 video titles         "(Official Video)"),                     
                                      editable preview                         
  -------------------------------------------------------------------------------------------

### 6.6 PixelPlayer

  --------------------------------------------------------------------------------------------------------------------------------------
  ID         Feature                Build approach                                     Settings home  Ph         Pri        Limit
  ---------- ---------------------- -------------------------------------------------- -------------- ---------- ---------- ------------
  PIX-U1     **Material You**       `dynamicDarkColorScheme/dynamicLightColorScheme`   Appearance →   10         P0         API 31+
             dynamic colour from    on API 31+; below 31 the option is disabled with a Themes                               
             wallpaper              reason                                                                                  

  PIX-U2     Smooth animations and  Shared `PulseClickable` modifier (scale spring     Appearance     10         P0         
             **micro-interactions   0.96, ripple, haptic optional) used by all                                              
             on every button**      interactive components; heart pop, play/pause                                           
                                    morph; respects reduced motion                                                          

  PIX-U3     **Adjustable corner    `LocalCornerScale` (0.0--2.0) feeds `Shapes` and   Appearance →   10         P1         Rail forced
             radius** and           component radii; **navigation style**: Top tabs    Shape and                            on width ≥
             navigation bar         (default, existing), Bottom bar, Floating bottom   navigation                           840 dp
                                    bar, Rail                                                                               unless
                                                                                                                            overridden

  PIX-U4     Dark/Light with        Exists (System/Dark/AMOLED/Light); add schedule    Appearance →   10         P1         
             auto/manual switching  option (follow time of day)                        Themes                               

  PIX-U5     **Album art colour     Exists (Palette); upgrade to a quantiser-based     Appearance     10         P1         
             extraction**           scheme with tonal palette for container colours                                         

  PIX-P1     Media3 ExoPlayer with  Optional decoder extension for extra formats (APE, Playback →     13         P2         Large APK
             **FFmpeg** support     DSD, etc.). Pick a Maven-available Media3 FFmpeg   Decoders                             impact;
                                    extension or build one; note extra APK size and                                         verify
                                    LGPL obligations; build as an optional flavor                                           licence
                                    dimension or a feature toggle                                                           

  PIX-P2     **Drag-and-drop queue  Long-press drag handle in queue sheet/panel;       Playback →     12         P0         
             reordering**           updates `QueueModel.move`                          Queue                                

  PIX-P3     Gapless and **custom   Duration slider 0--12 s, curve                     Playback →     13         P1         
             transition/crossfade   (linear/equal-power/S), "only when same album      Transitions                          
             configuration**        gapless"; same engine as NAM-S6                                                         

  PIX-L1     Formats MP3, FLAC,     Verify scanner extension/MIME list; show "format   Library        12         P0         
             AAC, OGG, WAV and more not supported by this device" card                                                      

  PIX-L2     Browse by Songs,       Genres page (NAM-U2)                               Library →      12         P0         
             Albums, Artists,                                                          Pages                                
             **Genres**, Folders                                                                                            

  PIX-L3     **Smart artist parsing Same engine as NAM-L2; grouping toggle: by album   Library →      12         P1         
             with configurable      artist vs by track artist                          Separators                           
             delimiters** and                                                                                               
             **album-artist                                                                                                 
             grouping**                                                                                                     

  PIX-L4     Folder filtering       Exists via exclusions and SAF trees; add an "allow Library →      12         P0         
             (choose scan           list" mode                                         Folders                              
             directories)                                                                                                   

  PIX-D1     Full-text search       NAM-Y7                                                            12         P0         

  PIX-D2     **Daily Mix**          **Local first:** generated from play history,      For you        12/15      P1         AI part
             (AI-powered            favourites, genres, recency decay, with a seeded                                        needs
             personalised playlist) daily refresh and diversity rules. **AI                                                 online + key
                                    optional:** the same mix refined by the AI                                              
                                    provider (15.6)                                                                         

  PIX-D3     Playlists,             Exist; extend with NAM-Y6                                         12         P0         
             **statistics**                                                                                                 

  PIX-Y1     **Synchronised LRC via `LrclibClient` (get by signature: title, artist,   Lyrics         15         P1         Needs online
             LRCLIB**               album, duration; search fallback); cache result in                                      
                                    `lyrics` with `source = LRCLIB`; opt-in per                                             
                                    service; manual "Fetch lyrics" button plus                                              
                                    optional auto-fetch on Wi-Fi                                                            

  PIX-Y2     **Lyrics editing**,    NAM-Y1; smooth centred auto-scroll with            Lyrics         12         P1         
             scrolling lyrics       tap-to-seek line                                                                        
             display                                                                                                        

  PIX-A1     **Deezer artist        `DeezerArtistClient` (public search endpoint),     Online &       15         P1         Needs online
             images** with memory   result cached in `artist_images` and image files   Privacy                              
             LRU + DB caching and   in private storage; never re-fetch before TTL (30                                       
             fallback icons         days); gradient initials fallback (exists)                                              

  PIX-C1     **Chromecast / Google  Media3 Cast module and `CastPlayer`; because the   Connectivity   15         P2         Needs Play
             Cast**                 files are local, serve them from a token-protected                                      services +
                                    local HTTP server on the LAN while casting; hide                                        online
                                    the Cast button if Google Play services are                                             flavor
                                    missing                                                                                 

  PIX-C2     **Android Auto**       Convert the service to `MediaLibraryService` with  Connectivity   15         P1         Needs a head
                                    a browse tree (Recent, Favourites, Playlists,                                           unit or
                                    Albums, Artists, Folders);                                                              Desktop Head
                                    `automotive_app_desc.xml`                                                               Unit to
                                                                                                                            verify

  PIX-C3     **Glance home-screen   Exists (one responsive widget); add 2 extra styles Connectivity → 15         P1         
             widgets**              (compact art-centred, controls-only) selectable    Widgets                              
                                    per widget instance                                                                     

  PIX-X1     **Tag editor** (MP3,   NAM-L1                                             Library        12         P1         
             FLAC, M4A)                                                                                                     

  PIX-X2     **AI playlist          Provider interface `AiProvider`                    AI             15         P2         Needs
             generation** (Gemini,  (`generatePlaylist(prompt, libraryDigest)`);                                            online + key
             DeepSeek, OpenAI,      user-supplied keys; consent screen; only metadata                                       
             others)                is sent (section 15.6)                                                                  

  PIX-X3     FFmpeg, background     Exists; keep notification, lock screen, Auto                      13         P0         
             playback with full     metadata consistent                                                                     
             media session                                                                                                  
  --------------------------------------------------------------------------------------------------------------------------------------

### 6.7 Design inventory (skins, themes, layouts)

Everything below is **added to** the existing pickers; nothing existing
is removed. **The names are provisional; the final list is whatever
Phase 9's inventory finds in the two apps' code.** \| Picker \| Existing
(keep) \| Add from Namida \| Add from PixelPlayer \| Add from this brief
\| \|---\|---\|---\|---\|---\| \| Skins \| Plum, Aurora, Afterglow,
Ocean, Sunrise, Forest, Mono, Pure black \| Every Namida
background/wallpaper/animated background found in the inventory
(procedural re-creations) \| Every PixelPlayer background/tonal-surface
look found \| Forest Mist (matches Atmospheric), Material You tonal
skin, Artwork-driven skin \| \| Themes (colour) \| System, Dark, AMOLED,
Light + dynamic accent \| Namida's artwork-driven scheme, its accent
presets \| Material You, tonal palettes, custom seed colour, adjustable
corner radius \| Time-of-day schedule \| \| Player themes \| classic,
mountain, mesh, minimal, acoustic, vinyl, coast, starry, immersive \|
Every distinct Namida player look found \| Every distinct PixelPlayer
player look found \| none \| \| Layout modes \| Current Now Playing
structures \| Standard, Widescreen, Waveform, Queue-side variants found
\| Expanding sheet, full-screen artwork and similar found \|
**ATMOSPHERIC_QUEUE** (premier) \| \| Button presets \| Current centred
arrangement \| Any control arrangement found \| Any control arrangement
found \| The 15 presets in section 8 \|

## 7. New premier layout: ATMOSPHERIC_QUEUE

### 7.1 Model

File `ui/screens/PlayerLayoutMode.kt` (or the project's equivalent;
first locate the real file in Phase 9): add enum entry
`ATMOSPHERIC_QUEUE` --- display name **"Atmospheric Queue"**, subtitle
**"Dark mist overlay & light queue card"**, icon
`Icons.Rounded.Splitscreen`. `PlayerLayoutMode.fromId(id)` returns
`ATMOSPHERIC_QUEUE` for null/unknown ids. It is listed **first** in the
picker with a "New" badge. Existing modes keep their ids and behaviour.
Stored under `playerLayoutMode`. Update `PlayerLayoutModeTest`: every
entry has a unique id, a non-blank name/subtitle, `fromId` round-trips
every id, unknown → `ATMOSPHERIC_QUEUE`, picker order is stable.

### 7.2 Background component

File `ui/components/AtmosphericBackground.kt`: a Compose component drawn
with `Canvas` (and an optional AGSL `RuntimeShader` path on API 33+ with
an automatic Canvas fallback). Content: forest/mist atmosphere --- a
deep green-black vertical gradient, 3--5 layered soft fog bands made of
overlapping radial gradients that drift slowly at different speeds,
faint pine-silhouette ridges at the bottom of the upper section drawn
with `Path`, and a dark semi-transparent scrim gradient on top so white
text always has ≥ 4.5:1 contrast. Rules: procedural only (no photos);
static when reduced motion or battery saver or when playback is paused
(draw one frame); cache the static layers into one `ImageBitmap` once
per size; never recompute per frame; optional tint from the artwork's
dominant colour (subtle, capped) when `colorSource = Artwork-driven`.
Provides `AtmosphericBackground(modifier, tint, animate, content)`.

### 7.3 Screen structure

File `ui/screens/layouts/AtmosphericQueuePlayerLayout.kt`. One
**stateless** composable; a `Column(fillMaxSize)` split by weights
(sizes are for a 20:9 phone; they must still work everywhere, see 7.5).

**Upper section --- weight 1.1f--1.25f (use 1.2f)**, on
`AtmosphericBackground`, content inside `safeDrawing` insets: 1. **Top
bar** (min height 56 dp, items ≥ 48 dp): left `IconButton` chevron down
(`KeyboardArrowDown`, `onCollapse`); centre text tab switcher **Song**
(bold, active, underline or pill) and **Lyrics** (muted); right three
icons in this order: theme (`Icons.Rounded.Checkroom`, `onOpenTheme`),
layout switcher (`Icons.Rounded.DashboardCustomize`, `onOpenPicker`),
overflow (`MoreVert`, opens the track-options sheet). Tabs and right
icons never overlap: below 360 dp width the Song/Lyrics switcher shrinks
to icon-sized chips; if still too tight the theme icon moves into the
overflow menu (priority: chevron \> tabs \> layout picker \> overflow \>
theme). 2. **Details block** (weighted spacer above and below so it
floats in the middle): title (headline, bold, white, centred, max 2
lines, ellipsis; marquee when reduced motion is off and it overflows),
artist/subtitle (medium, white at 70 % alpha, 1 line). With the Lyrics
tab active this block collapses to a single title line and the lyrics
panel (file 16 §6) fills the space. 3. **Progress**: thin `Slider`
(track height ≈ 4 dp, 48 dp touch height) flanked by elapsed time on the
left and total on the right (`formatTime`, tabular digits). Uses the
existing `PlaybackSeekBar` safe-seek rules (disabled and `--:--` when
duration is unknown). When `seekBarStyle = Waveform`, the waveform
seekbar (NAM-U3) replaces the slider. 4. **Playback controls row**:
shuffle toggle, previous, **centre play/pause (64 dp circle, dark
semi-transparent container `Color.Black` at \~45 % alpha with a faint
white 1 dp ring, white icon 32 dp)**, next, repeat toggle (off/all/one,
N-times badge). The arrangement can be replaced by any button preset
(section 8); this is the `CENTERED` default. 5. **Secondary action
bar**: favourite (heart pop), sleep timer, add to playlist, queue jump
(scrolls and focuses the current row in the lower card), equalizer
(opens the Equalizer screen).

**Lower section --- weight 0.9f--1.0f (use 0.95f)**:
`Surface(shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))`,
container colour from theme token `queueCardContainer` (light mode
`#F9F9FB`; dark mode a dark elevated surface, **not** a bright white
slab; see C15 and section 10), on it: - Header row: "Up Next" (title
style) + count badge (rounded chip with the queue size) + an optional
"Clear" text button that opens the existing confirm dialog. -
`LazyColumn` (stable keys = media id, `contentPadding` bottom = system
inset + 16 dp). Each row (min height 64 dp): - Left slot: 48 dp
rounded-square thumbnail (corner **8 dp**, scaled by the corner-radius
setting) with artwork or the gradient fallback. For the **current**
track the slot shows animated amber equalizer bars over a scrim (static
icon when paused or reduced motion). - Centre: bold title (active →
amber token, idle → `onSurface` / charcoal `#1C1B1F` on light), artist
in muted `onSurfaceVariant` below; both 1 line with ellipsis. - Right:
`MoreVert` `IconButton` (≥ 48 dp) opening the row menu (play next,
remove from queue, favourite, details). - Tap = play this item instantly
(`onTrackSelect(index)`); drag handle appears on long-press for reorder
(PIX-P2); swipe actions per the Gestures setting. - Auto-scroll so the
current row is visible with one row of context above it when the track
changes, unless the user is scrolling.

### 7.4 State and callbacks (stateless, hoisted)

``` kotlin
data class QueueRowUi(val id: String, val title: String, val artist: String, val durationLabel: String,
                      val isCurrent: Boolean, val art: ArtRef?)          // NOT the existing Track class (C11)
data class AtmosphericPlayerUi(
    val title: String, val subtitle: String, val elapsedMs: Long, val durationMs: Long, val canSeek: Boolean,
    val isPlaying: Boolean, val shuffleOn: Boolean, val repeatMode: Int, val repeatRemaining: Int?,
    val isFavorite: Boolean, val sleepActive: Boolean, val activeTab: PlayerTab /* SONG | LYRICS */,
    val queue: List<QueueRowUi>, val currentIndex: Int, val art: ArtRef?, val reducedMotion: Boolean)
// callbacks (all lambdas, no ViewModel inside the composable):
onPlayPauseClick, onPreviousClick, onNextClick, onSeek: (Long) -> Unit, onTrackSelect: (Int) -> Unit,
onToggleFavorite, onToggleShuffle, onCycleRepeat, onOpenTheme, onOpenPicker, onCollapse,
onTabChange: (PlayerTab) -> Unit, onSleep, onAddToPlaylist, onQueueJump, onEqualizer, onMore, onRowMore: (Int) -> Unit,
onMoveQueueItem: (Int, Int) -> Unit, onRemoveQueueItem: (Int) -> Unit
```

The position flow is collected **only** inside the progress composable
(no whole-screen recomposition per tick).

### 7.5 Responsiveness of this layout

  -----------------------------------------------------------------------
  Condition                           Behaviour
  ----------------------------------- -----------------------------------
  Compact portrait, 20:9              As specified (1.2f / 0.95f)

  Compact portrait, short (\< 640 dp  Hide subtitle first, then shrink
  tall)                               the secondary bar to icons only,
                                      then reduce the queue card to a
                                      peeking header (expand on tap)

  Landscape or width ≥ 600 dp         Two columns: left = upper section
                                      (controls), right = the queue card
                                      full height (no rounded top;
                                      rounded start corners)

  Width ≥ 840 dp (docked panel)       Docked Now Playing panel uses this
                                      layout in its single-column form

  Foldable table-top posture          Controls in the bottom half,
                                      artwork/lyrics in the top half
                                      (section 11)

  Font scale 2.0                      Titles stay 2 lines max; icon rows
                                      wrap by spacing, never clip; the
                                      screen scrolls only inside the
                                      queue and lyrics
  -----------------------------------------------------------------------

### 7.6 Layout picker and live switching (while music is playing)

-   The layout switcher icon opens a **bottom sheet** with live
    thumbnails of every `PlayerLayoutMode`; tapping one applies it
    immediately with a crossfade (300 ms; instant when reduced motion).
    Playback, queue and position are untouched. Selected mode is
    persisted and restored on launch.
-   Also reachable from Settings → Appearance → Layouts, and from the
    player overflow menu.
-   **Swipe cycling:** a horizontal swipe on the **top bar / drag-handle
    strip** cycles to the next/previous layout mode (haptic tick,
    snackbar "Layout: `<name>`{=html}" with Undo). It must not clash
    with artwork swipe-to-skip: the gesture lives only on that strip,
    and can be disabled in Settings → Gestures.
-   Every layout mode exposes the same minimum: play/pause, previous,
    next, seek, favourite, queue access. A layout that cannot show
    something provides it through its overflow menu.

## 8. Live Button Layout Switcher

### 8.1 Concepts

Buttons are grouped: **Transport** = shuffle, previous, play/pause,
next, repeat; **Quick actions** = favourite, sleep timer, add to
playlist, queue, equalizer; **Extras** (available in some presets) =
back 10 s, forward 10 s, speed, lyrics, cast, timer. A **preset** only
decides arrangement, sizes, labels and grouping; it never changes what a
button does.

### 8.2 Presets (provide all 15; ids are stable strings)

  -----------------------------------------------------------------------------------------------------------------
  \#                      id                      Arrangement
  ----------------------- ----------------------- -----------------------------------------------------------------
  1                       `centered`              **Default / current.** Transport row centred; quick actions in
                                                  one row below

  2                       `split`                 Transport centred under the seek bar; quick actions split left
                                                  and right of the seek bar line (two on each side, queue centred
                                                  above)

  3                       `minimal`               Only previous, play/pause, next visible; everything else in an
                                                  overflow chip

  4                       `waveform_below`        Waveform seekbar on top, transport directly beneath, quick
                                                  actions as a slim icon strip under that

  5                       `queue_inline`          Quick actions merge into the queue header; transport sits at the
                                                  bottom of the upper section

  6                       `expanded`              Large buttons with text labels, two rows (transport; actions with
                                                  captions)

  7                       `compact`               One row of seven small buttons, secondary ones in overflow below
                                                  360 dp

  8                       `floating`              A rounded glass pill floating over the artwork/bottom edge
                                                  containing transport; actions in a second pill

  9                       `symmetric`             `favourite · shuffle · previous · PLAY · next · repeat · queue`
                                                  mirrored around the play button

  10                      `asymmetric`            Large play button on the left, previous/next stacked beside it,
                                                  actions on the right column

  11                      `two_row_grid`          5 + 5 grid, equal cells, icon-only

  12                      `radial`                Play at the centre of an arc; previous, next, shuffle, repeat on
                                                  the arc; actions in a row under

  13                      `thumb_zone`            Everything in the bottom third for one-handed use; seek bar above
                                                  controls

  14                      `side_rail`             Vertical rail of buttons on the screen edge (used by default in
                                                  landscape)

  15                      `chips`                 Labelled chips (Shuffle, Repeat, Sleep, Queue, EQ) plus a large
                                                  play button
  -----------------------------------------------------------------------------------------------------------------

Presets 5 and 14 require a layout mode that has a queue header / a side
area; if the active layout cannot host a preset, the picker shows it
disabled with the reason and the layout falls back to `centered`.

### 8.3 Rules

-   Play/pause is always visible and ≥ 56 dp; every button ≥ 48 dp;
    labels never wrap into overlap (labels hide before icons shrink).
-   **Overlap prevention:** the arrangement is a pure function
    `arrange(preset, widthDp, heightDp, fontScale) -> ButtonPlan`; if
    the plan does not fit, buttons move to an overflow menu in this
    fixed priority (kept last → first dropped): play/pause, next,
    previous, favourite, queue, shuffle, repeat, sleep, add to playlist,
    equalizer, extras. Unit-test the function at widths 320, 360, 393,
    430, 600, 840 dp and font scales 1.0/1.3/2.0 (no two rects
    intersect).
-   Switching animates (`AnimatedContent` + `animateContentSize`, 250
    ms; instant under reduced motion) and never interrupts playback.
-   Picker: a bottom sheet with live thumbnails (real mini-renders, no
    images), reachable from the layout icon in the player (a second tab
    "Buttons" next to "Layouts") and from Settings → Appearance →
    Layouts. Selecting persists `buttonPresetId` immediately and is
    restored on launch. Optional (P1): a per-layout override map so each
    player layout can remember its own preset; a "Reset to default"
    action.
-   P2: **Customise** mode --- long-press a button to hide it or drag to
    reorder within its group; stored as a user preset `custom_1…3`.
-   Tests: `ButtonPlanTest` (fit, priority, min sizes),
    `ButtonPresetRegistryTest` (15 unique ids, default exists, unknown →
    `centered`), a Compose UI test that switching presets mid-playback
    keeps `isPlaying`.

## 9. Classic favourites backup v2 (folder `Music/Pulse Player/`)

The Classic backup stays **separate** from the Full backup (file 15 Part
C) and keeps the **`pulseplayer.favorites` v1 JSON format exactly**
(same keys, same `schemaVersion = 1`, same decoder tolerance, same
three-level fuzzy matcher with ambiguity never guessed). v2 changes only
**where the files go** and **what is copied**.

### 9.1 Export (Settings → Backup & Transfer → Classic → Export favourites)

1.  Count favourites; if zero, message and stop.
2.  **Pre-flight dialog** (explicit confirmation, rule C8): "Export N
    favourites. Copies M audio files (X MB) into Music/Pulse Player.
    Originals are not changed." Shows free space, with a hard stop and
    an explanation if space is insufficient (need size + 10 % margin;
    also refuse if the database volume would drop below 50 MB, file 17
    §3).
3.  Create the folder: API 29+ insert through `MediaStore` with
    `RELATIVE_PATH = "Music/Pulse Player/"` (no broad permission needed
    for files the app creates); API 26--28 needs
    `WRITE_EXTERNAL_STORAGE` (request it at that moment with a
    rationale). Folder already exists = reuse it.
4.  Copy each favourite's audio file in 64 KB chunks to a unique name
    (`name (1).ext` on collision; never overwrite), with real progress
    (file k of M, bytes), **cancel** support (delete the partial file),
    pause on low battery optional. Sources that are not readable
    (deleted, SD card removed, permission lost, non-copyable
    remote/stream item) are skipped and recorded.
5.  Write `PulsePlayer_Favorites_<date>.json` (the v1 codec) into the
    same folder, written to a temp file first, then moved in. The JSON's
    `displayName` is the **final copied file name** so import matches by
    file name + size strongly.
6.  **Self-check:** re-decode the written JSON, and verify each copied
    file exists with the expected size. Show the result card: copied n,
    skipped n (with reasons), failed n, JSON path, a "Retry failed"
    button, and a "Share folder contents" option.
7.  Copied tracks may be indexed by the media scanner as normal music in
    `Music/Pulse Player/`. Because the originals still exist, the
    duplicate-hiding rule (file 14 §9) and NAM-L3 must keep the library
    from showing doubles (the copy of a track the user already has
    should be hidden as a duplicate, not counted twice).

### 9.2 Import (Settings → Backup & Transfer → Classic → Import favourites)

1.  On the new device the user pastes the music files and the JSON into
    `Music/Pulse Player/`.
2.  **Detection:** run a device scan; check MediaStore for audio with
    `RELATIVE_PATH LIKE 'Music/Pulse Player/%'`. A JSON file placed by
    the user is **not** visible to the app through MediaStore on Android
    11+ (no broad file access), so the app asks **once** for access to
    that folder via the SAF folder picker with the initial location
    pre-set to `Music/Pulse Player` (persisted read grant), then
    auto-detects the newest `PulsePlayer_Favorites_*.json` (or any file
    that passes the v1 schema sniff) there. On API ≤ 29 it may read
    directly. The **file picker remains available** as a fallback
    ("Choose a JSON file").
3.  Decode, then run the matcher against the library (`available = 1`);
    show the existing preview (will add / already favourite / needs
    choice / not found / invalid / duplicates) with a note if the audio
    files in the folder are not yet scanned ("N songs from the folder
    are still being scanned. Scan now?").
4.  Apply Merge (default) or Replace (confirm), atomically, idempotent.
5.  Result card with the exact counts and a "Not found" list with
    advice: "Copy those songs into Music/Pulse Player, rescan, import
    again."

### 9.3 Error matrix (each row: message, recovery action, test)

  -----------------------------------------------------------------------
  Case                    Message (plain          Recovery
                          language)               
  ----------------------- ----------------------- -----------------------
  Permission denied       "Pulse Player needs     Re-request; if
  (storage write / folder permission to save into permanently denied,
  access)                 Music/Pulse Player."    Open settings; or
                                                  "Export JSON only"

  Storage full /          "Not enough space: need Free-space hint,
  insufficient space      X, have Y."             "Export JSON only",
                                                  "Choose fewer songs"
                                                  (select a subset)

  Folder cannot be        "Couldn't create the    Retry; "Choose another
  created                 Pulse Player folder."   folder" (SAF)

  Unreadable/missing      "N songs couldn't be    Show list; "Continue
  source file             copied (file missing or without them" / "Retry"
                          unreadable)."           

  Duplicate names         Silent unique naming,   none needed
                          mentioned in the report 
                          ("renamed n files")     

  Partial copy / app      On next launch: "An     **Resume** (skip files
  killed mid-copy         export was              already complete by
                          interrupted."           size) or **Clean up**
                                                  partial files; never
                                                  leave a half-written
                                                  file named like a
                                                  finished one (write
                                                  `.part`, rename on
                                                  success)

  Corrupt JSON            "This favourites file   Pick another file; show
                          is damaged and can't be line/position hint
                          read."                  without paths

  Wrong/unsupported       "This isn't a Pulse     Offer the Full backup
  schema                  Player favourites file  importer if sniff says
                          (or it is a newer       so
                          version)."              

  Folder empty / no JSON  "No favourites file     Choose file manually;
  found                   found in Music/Pulse    show the 3-step guide
                          Player."                

  No matching songs       "None of the songs in   Show guide + rescan
                          the file were found on  button
                          this device."           

  Cancelled by user       "Export cancelled. N    Keep or delete copied
                          files were copied."     files (user's choice)
  -----------------------------------------------------------------------

Tests: `ClassicBackupV2Test` (JSON byte-compatible with v1 fixtures;
unique naming; `.part` handling; space check; resume), instrumented
export to a temp MediaStore folder, import from a simulated folder.

## 10. Theme correctness (dark, light, AMOLED, every skin and theme)

1.  **Token system:** all colours come from `PulseColors` (extend
    `MaterialTheme.colorScheme` with `extendedColors`:
    `queueCardContainer`, `onQueueCard`, `activeAmber`,
    `activeAmberOnLight`, `scrim`, `glass`, `success`, `warning`, plus
    state layers: hovered, pressed, focused, disabled). Components read
    tokens only.
2.  **Audit every surface:** screens, dialogs, bottom sheets, snackbars,
    menus, tooltips, switches, sliders, chips, tab rows, text fields,
    progress indicators, notifications' small icons, widgets, share
    cards, overlays, the splash/window background and system bar icon
    colours (set status/nav bar appearance per mode).
3.  **Detection:** add a CI-style grep gate `Color(0x` / `Color.White` /
    `Color.Black` / hex strings **outside** `ui/theme` and `ui/tokens`
    and procedural art files must be 0, except an allow-list file
    `docs/COLOR_ALLOWLIST.md` (intentional accents: procedural
    backgrounds, the amber highlight, brand gradients on artwork
    fallbacks). Each exception has a reason.
4.  **Contrast tests:** unit tests compute WCAG contrast for the token
    pairs in every mode (text on surface, on queue card, amber on card,
    icons on glass) and assert ≥ 4.5:1 for text and ≥ 3:1 for large
    text/icons.
5.  **Screenshot matrix** (Compose screenshot tests or a manual
    checklist if not available): Home, Songs, Now Playing (Atmospheric +
    Classic), Queue sheet, Settings, Equalizer, Dialogs, Snackbar ×
    {Light, Dark, AMOLED} × {2 skins} × {2 player themes}.
6.  Mode switching is live (no restart), animated 300 ms, and keeps
    scroll positions.

## 11. Responsiveness

-   **Window size classes** (`WindowSizeClass`) plus
    **`androidx.window`** (`WindowInfoTracker`) for foldable posture
    (table-top, book) and hinge-aware layout: never place controls
    across a hinge.
-   Support widths 320--1280+ dp, heights down to 280 dp (landscape
    phone), font scale up to 2.0, display size changes, split-screen and
    freeform; handle configuration changes without recreating playback
    UI state (the activity already declares `configChanges`).
-   Insets: cutouts (`displayCutout`), status/navigation bars, gesture
    insets (`systemGestures`), IME; edge-to-edge everywhere; no
    interactive element inside the 24 dp punch-hole zone or the
    rounded-corner zone.
-   **No-overlap policy:** every screen defines a priority list (primary
    → secondary). Layout code uses `weight`, `FlowRow`,
    `BoxWithConstraints` and the `arrange()` functions; when space runs
    out the lowest-priority item is truncated (text) or moved to an
    overflow menu (icons). Provide a reusable `AdaptiveRow` that
    implements this.
-   Tablet: rail + content + docked player (exists), two-pane Settings,
    adaptive grids, two-column queue/lyrics.
-   **Test pass** at 320, 360, 393, 430, 600, 840, 1280 dp, landscape,
    fold half-open; record results in `docs/TESTING.md`; automated
    `AdaptiveLayoutTest` for the `arrange()` functions.

## 12. Performance targets and work

  --------------------------------------------------------------------------------
  Area                    Work                        Target (measure, then report
                                                      real numbers or `NOT RUN`)
  ----------------------- --------------------------- ----------------------------
  Startup                 Defer non-critical init     Cold start to first frame ≤
                          (AudioFx, widgets, network, 800 ms on the primary device
                          Cast) off the main thread;  
                          `Application.onCreate` \<   
                          100 ms of work; **Baseline  
                          Profile** + startup profile 
                          via Macrobenchmark; App     
                          Startup library for lazy    
                          init                        

  Scrolling               Stable keys,                No jank frames \> 16 ms in
                          `derivedStateOf`, no        Songs with 10 000 tracks
                          allocations in item scopes, (JankStats/Macrobenchmark)
                          immutable UI models         
                          (`@Immutable`/`@Stable`),   
                          `contentType` on lazy       
                          items, avoid nested         
                          scrolling; compose compiler 
                          metrics reviewed            

  Images                  Size-bucketed cache         Peak art cache ≤ 1/16 max
                          (exists), downsampled       heap
                          decode, off-main loading,   
                          placeholder crossfade only  
                          on first load; blurred      
                          backgrounds computed once   

  Playback stability      Foreground service with     No ANR; no crash in a 2-hour
                          correct types; wake lock    soak test
                          policy; `onTaskRemoved`;    
                          audio focus; process-death  
                          recovery (exists); the      
                          crossfade/analysis workers  
                          have back-pressure          

  Memory                  Leak checks (LeakCanary in  No growth after 50 track
                          debug only), release        skips
                          listeners on detach (file   
                          13 §7), bounded caches,     
                          waveform generation one at  
                          a time                      

  DB                      Indices for new tables;     Search ≤ 100 ms at 20 000
                          FTS; batch inserts; `Flow`  tracks
                          conflation                  

  Build                   R8 full mode, resource      Report APK sizes per flavor
                          shrinking, split flavors    
                          (`offline` has no network   
                          libs)                       

  Polish                  Shared motion tokens        Visual checklist in
                          (durations 120/200/300/500  `docs/TESTING.md`
                          ms, spring specs),          
                          consistent button hierarchy 
                          (filled = primary, tonal =  
                          secondary, text =           
                          tertiary), type scale       
                          review, spacing grid 4 dp,  
                          iconography pass            
  --------------------------------------------------------------------------------

Button colour rule: every button style derives from tokens, has a
defined disabled state with ≥ 3:1 contrast, and the same style means the
same action class across themes.

## 13. Error-handling framework

``` kotlin
sealed interface AppError {
  val userMessage: UiText; val recovery: List<RecoveryAction>; val cause: Throwable?
  data class Permission(val which: PermissionKind, val permanentlyDenied: Boolean, ...) : AppError
  data class Storage(val kind: StorageProblem /* Full, Unavailable, ReadOnly, NotFound */, ...) : AppError
  data class Io(val op: IoOp, ...) : AppError
  data class Parse(val what: String, ...) : AppError            // corrupt JSON/LRC/XML
  data class Playback(val category: ErrorPolicy.Category, ...) : AppError
  data class Network(val kind: NetKind /* Offline, Timeout, Http(code), Blocked, Metered */, ...) : AppError
  data class Auth(...) : AppError ; data class Unsupported(val reason: String, ...) : AppError
}
```

-   One `ErrorReporter` turns every failure into an `AppError` and
    publishes it to a `UiEvents` flow: **snackbar** (transient, with one
    action), **banner** (persistent, state-based,
    e.g. permission/partial scan), or **error card/dialog** (blocking,
    e.g. playback, restore). Messages are plain language, never stack
    traces or file paths, and each has a **recovery path** (Retry, Open
    settings, Choose another folder, Skip, Cancel, Undo, Learn more).
-   Rules: no `catch (e: Exception) {}` swallowing (grep gate); every
    `suspend` I/O function returns `Result`/`AppResult` or throws a
    typed exception mapped at the boundary; `CancellationException` is
    always rethrown; coroutine exception handlers on app scopes; logs go
    to local `Log` only (no uploads) with a debug-only in-app log viewer
    that can be exported manually.
-   **Network:** `NetworkGateway` is the only place that creates HTTP
    clients: timeouts (connect 10 s, read 20 s), retries with
    exponential back-off for idempotent GETs (max 3), offline detection
    (no call when offline, queue for later where sensible),
    metered-network respect (data saver), TLS only (cleartext allowed
    only for user-added LAN servers after an explicit warning),
    response-size caps, JSON validated before use, user-visible "Online
    features are off" state instead of silent failure.
-   **Coverage table to complete in the final audit:** Classic
    export/import (9.3) · Full backup (file 15) · permissions (14) ·
    scan and SAF · tag writes (denied, read-only, unsupported format,
    interrupted) · playback (file 13 policy) · lyrics fetch/parse ·
    artwork fetch · AI (invalid key, quota, refusal, malformed output) ·
    server sources (auth failure, TLS, timeout) · downloads (space,
    interrupted, resume) · storage removed · widget/Cast/Auto connection
    loss.
-   Every error type has a unit test that asserts message + recovery
    actions exist.

## 14. First-launch permission onboarding

A short, skippable-per-step flow shown on the **first launch only**
(re-runnable from Settings → Permissions). Each step: icon, one-sentence
reason, **Allow** and **Not now**. Denial never blocks the app; features
degrade with an explanation and a fix button. \| Step \| Permission \|
Needed for \| When denied \| \|---\|---\|---\|---\| \| 1 Music access \|
`READ_MEDIA_AUDIO` (API 33+) / `READ_EXTERNAL_STORAGE` (≤ 32) \| Finding
songs \| Limited mode: folder picker only (SAF); banner on Home \| \| 2
Notifications \| `POST_NOTIFICATIONS` (API 33+) \| Playback controls,
downloads \| Playback still works; lock-screen controls may be reduced;
banner \| \| 3 Nearby Bluetooth \| `BLUETOOTH_CONNECT` (API 31+) \|
Showing connected Bluetooth output names, headset events \| Output list
shows generic names \| \| 4 Battery \|
`REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` flow \| Reliable background
playback on aggressive OEM skins (Infinix XOS) \| Show an OEM-specific
hint page; **optional**, never forced, and note the store-policy
sensitivity \| \| 5 Network (informational) \| `INTERNET` is a normal
permission (no prompt) in the `full` flavor \| Online features \| The
"Online features" master switch stays OFF until the user turns it on;
the screen explains each service \| Requested **later, in context**, not
at first launch: `WRITE_SETTINGS` (ringtone, pending action file 13 §9),
`NEARBY_WIFI_DEVICES` (API 33, Listening Party/LAN sync),
`WRITE_EXTERNAL_STORAGE` (API ≤ 28, Classic export), SAF folder grants,
`createWriteRequest` (tag edits). **Never requested:** `RECORD_AUDIO`,
`MANAGE_EXTERNAL_STORAGE`, location, contacts, phone state. The
Permissions settings page lists every permission with status and a fix
button; a `PermissionMonitor` (file 14 §8) is extended with
Notifications, Bluetooth, Battery states. Tests: `PermissionStateTest`
additions, UI test for denied/permanently denied paths.

## 15. Detailed specifications for the larger new subsystems

### 15.1 Library intelligence (Phase 12)

-   **Separators engine:**
    `ArtistParser.split(raw, delimiters, exceptions, extractFeat): List<String>`
    with unit tests (`"A feat. B"`, `"A & B"`, `"AC/DC"`,
    `"Tyler, The Creator"`, `"A x B"`); genre splitting similarly.
    Results cached in a join table so queries stay fast; re-run on
    settings change in a background worker with progress.
-   **Metadata screens:** Moods, Tags, Ratings, Languages are managed in
    the track details sheet and bulk via multi-select; pages list items
    grouped with counts; language can be auto-suggested from lyrics/tags
    but never auto-applied silently.
-   **Search:** FTS index columns: title, artist, album, album artist,
    genre, file name, lyrics; query parser supports `artist:`, `album:`,
    `genre:`, `tag:`, `mood:`, `rating:>=4`, `year:`, `smart:`; typo
    tolerance fallback; results grouped (Songs, Albums, Artists,
    Playlists, Lyrics matches with a snippet).
-   **Smart playlists:** rules JSON schema versioned; evaluation off the
    main thread; preview count while editing; export/import as JSON;
    never writes to files.
-   **Daily Mix (local):** deterministic seeded generator: candidate
    pool = favourites + frequently played + similar genre/artist
    neighbours + a recency-decayed rediscovery slice; avoid repeating
    the same artist more than twice in a row; refresh at local midnight;
    shown on For you.
-   **Stats/Your Year:** all read from `play_events` with `counted = 1`;
    charts: line (time per day), bar (top lists), heat-map (hour ×
    weekday), donut (genres), streak counter; year selector; share card
    bitmap (extends file 08).
-   **Queues:** page lists saved queues with name, count, date; "Save
    current queue", "Load" (replace or append), "Rename", "Delete";
    loading never starts playback until the user taps Play (restored
    paused).
-   **Gestures/selection:** one `SelectionState` hoisted at app level
    for global selection.

### 15.2 Lyrics (Phases 12 and 15)

Parsers: LRC (multi-stamp), enhanced LRC (word stamps), TTML (word
spans), plain. Display modes: line-synced, word-synced highlight
(karaoke fill), plain scroll. Editor: shows lines, play/pause, "stamp
this line" at the current position, nudge ±100 ms, global offset, save
as database entry or sidecar `.lrc`. LRCLIB fetch: exact-match request
(title, artist, album, duration ±2 s), else search; prefers synced;
stores result with `source = LRCLIB`; the user can delete or replace;
fetch is manual unless auto-fetch is on (Wi-Fi only default). Limits:
file size ≤ 256 KB (file 16 §6); NUL bytes rejected.

### 15.3 Sound engine (Phase 13)

-   Insert a custom `AudioProcessor` chain into the ExoPlayer
    `DefaultAudioSink` in this fixed order: **gain (ReplayGain +
    per-track) → parametric EQ → effects (bass shelf,
    surround/crossfeed, 8D, vocal remover, mono) → level tap (for
    visualizers)**. Each processor is bypassed when its setting is off
    (zero cost). Processing uses float samples and a soft limiter at the
    end to avoid clipping.
-   Platform `AudioFx` stays for devices where it works; the settings
    page shows which engine is active and prevents double-EQ.
-   **Audio path viewer** reads `onAudioSinkInputFormatChanged`,
    `onAudioDecoderInitialized`, processor states and the routed
    `AudioDeviceInfo`.
-   Bit-perfect/exclusive/USB (NAM-S2): capability-gated, and when
    active it bypasses the whole processor chain and says so.
-   Per-track profile applied at `onMediaItemTransition` through
    `PlaybackEngine`; restored to global values when leaving the track.

### 15.4 Smart crossfade and beat matching (experimental, default OFF)

-   `CrossfadeEngine` inside `PlaybackEngine` owns a second ExoPlayer
    (same audio attributes, **no** extra MediaSession, no extra
    notification). It pre-buffers the next item; at `duration − fadeMs`
    it starts the incoming player and ramps both volumes **through
    `AudioTransitionController`** (equal-power curve). On completion the
    engines swap roles; the `FadingPlayer` facade and `QueueModel` see
    one continuous queue.
-   Guards: not used for repeat-one, for tracks shorter than 2 × fade,
    when the next item is unavailable, when skip-silence is on (conflict
    note), in bit-perfect mode, or when the user manually skips (manual
    skip uses the short `TrackChange` cut from file 13).
-   **Beat matching (P2):** background BPM analysis (onset
    autocorrelation on a downsampled mono buffer) cached in
    `tracks.bpm`; when both BPMs are known and within ±6 %, the incoming
    track's playback speed is nudged (≤ ±4 %) during the overlap and
    returned to 1.0 afterwards; downbeat alignment is best-effort. If
    analysis is missing or confidence is low, plain equal-power
    crossfade is used. Label: "Experimental --- results vary".
-   `SessionTracker` treats the overlap as a track change at the
    midpoint; `SleepTimerEngine` and `ErrorPolicy` are tested against
    the engine swap (`CrossfadeEngineTest`, `ControllerReconnectTest`
    additions).

### 15.5 Android Auto, Cast, widgets (Phase 15)

Convert `MusicPlaybackService` to `MediaLibraryService` while preserving
the current session behaviour (all existing service tests must pass
unchanged). Browse tree nodes: Recent, Favourites, Playlists, Albums,
Artists, Folders, Most played. Voice search maps to the FTS search.
Cast: local HTTP server (token in the path, bound to the Wi-Fi
interface, stopped when casting ends), `CastPlayer` swap-in/out keeping
the queue; hide the button with no Play services. Widgets: three styles
selectable in a configuration activity; each uses the existing
`WidgetStateStore`.

### 15.6 AI features (Phase 15, `full` flavor, opt-in)

Providers behind `AiProvider`: Gemini, OpenAI-compatible (covers
DeepSeek and others with a base URL), and a "Custom endpoint". User
pastes their own key; it is stored encrypted and never logged or
included in backups. **Consent screen** before the first call shows
exactly what will be sent (a preview of the digest: titles, artists,
albums, genres, play counts, a prompt) and what is **never** sent (file
paths, audio, favourites file, lyrics, personal data); the user can
reduce the digest size. The response must be JSON; validate against a
schema; map suggested tracks to the library by the matcher tiers;
unmatched suggestions are shown as "not in your library" (never invented
as playable). Errors: invalid key, quota/rate limit, refusal, malformed
output, timeout --- each with retry/back-off and a clear message. Daily
Mix works fully without AI.

### 15.7 Online artwork (Deezer) and LRCLIB

Both go through `NetworkGateway`; both honour the master switch, the
per-service switch, data saver and a polite request rate (≤ 1 req/s,
local queue). Results are cached (Room + files + in-memory LRU) with
TTLs; failures cache a negative result for 24 h; the UI never blocks on
the network and always has the fallback artwork.

### 15.8 History import (Phase 15)

Importers parse files streamed through `JsonReader`/CSV with the same
limits as file 15 §2.2 (size, entries, strings, timestamps), map to
library tracks using the tier rules, produce a preview (matched /
ambiguous / unmatched / duplicates), then commit in **one transaction**
through `HistoryWriter` with `origin` set; importing the same file twice
changes nothing (dedupe key trackId + playedAtMs). Rows from imports
show a small origin badge in the history editor.

### 15.9 Sources and streaming legal gate (Phase 16)

Before building `YouTubeSource`, SponsorBlock, Return YouTube Dislike,
video downloads, sign-in: write `docs/DECISIONS.md` with (a)
terms-of-service and store-policy risk, (b) which extractor library and
its licence, (c) the owner's explicit go/no-go. If no-go, mark
NAM-V1/V2/V4--V10 `Unsupported(reason: "Not included in this build")`,
visible in Settings. Subsonic, Jellyfin, WebDAV and SMB are ordinary
self-hosted protocols and proceed normally.

## 16. Documentation deliverables

**README.md** (detailed and visually pleasing): banner (SVG/PNG made
from the existing icon assets or procedural), badges (platform, Kotlin,
Compose, minSdk from the version catalog, licence, build status
placeholder), screenshot **placeholders** with file names in
`docs/screenshots/` (never fake screenshots), feature list by section
mirroring Settings IA, "Two flavors" table (offline vs full, permissions
per flavor), requirements, build/run commands
(`./gradlew assembleOfflineDebug assembleFullDebug testDebugUnitTest lintDebug`),
architecture diagram (Mermaid) with the four customisation axes,
module/package map, data flow (service ⇄ controller ⇄ UI), backup guide
(Classic folder flow and Full backup), privacy statement (what is
stored, what is sent and only when the user enables it), troubleshooting
(permissions, OEM battery settings, SD card), contribution guide
(branching, commit style, task size, tests, no emojis in UI, token
rules, how to add a theme/layout/button preset), roadmap (the Gated and
Unsupported items), licences and acknowledgements (Namida, PixelPlayer,
libraries) linked to `THIRD_PARTY_NOTICES.md`. Also update
`CHANGELOG.md`, `docs/PRIVACY.md`, `docs/TESTING.md`,
`docs/spec/SPEC_INDEX.md`, and file 12 §4's theme table. **Git:** commit
per task with a message in the form `phase<N>: <summary>`; push **only**
when `assembleDebug` and unit tests pass for that task; never push
secrets (`local.properties`, keys, the user's `.wav` sample, music
files).

## 17. Phases, Spec Index and prompts

### 17.1 Append to `docs/spec/SPEC_INDEX.md`

  -----------------------------------------------------------------------
  Phase                   Goal                    Required files
  ----------------------- ----------------------- -----------------------
  9                       Merge groundwork:       18 §1--§6, §13; 03, 14
                          inventory, licences,    §1--§2
                          capability registry,    
                          schema v4, settings IA, 
                          error framework         

  10                      Tokens, theme fixes,    18 §5.1, §10, §6.6
                          colour sources, corner  (PIX-U); 02, 05
                          radius, navigation      
                          styles,                 
                          micro-interactions      

  11                      Player layouts,         18 §7, §8, §6.2; 05,
                          Atmospheric Queue, live 06, 16
                          button presets,         
                          waveform seekbar,       
                          visualizer, particles   

  12                      Library intelligence,   18 §6.1, §6.4,
                          new pages, tag editor,  §15.1--§15.2; 14, 15
                          search, smart           
                          playlists, stats,       
                          queues, gestures        

  13                      Sound engine and        18 §6.3, §15.3, §15.4;
                          transitions             04, 13

  14                      Classic backup v2,      18 §9, §13, §14; 07, 14
                          permission onboarding,  §8, 15 Part C
                          error-handling sweep    

  15                      Online layer: LRCLIB,   18 §15.5--§15.8, §3 C1;
                          Deezer, AI, history     08, 13
                          imports, Android Auto,  
                          Cast, widgets           

  16                      Sources, downloads, LAN 18 §5.4, §6.5, §15.9;
                          party/sync, gated       14
                          streaming               

  17                      Responsiveness,         18 §11, §12, §16, §18;
                          performance, README,    17
                          final traceability      
                          audit                   
  -----------------------------------------------------------------------

Existing phase files keep working; fix-pack files still win over base
files.

### 17.2 Rules that apply to every prompt below

Start with: *"Attach the unified master specification, `SPEC_INDEX.md`
and the files listed. Read only those. Fix-pack files win over base
files; Annex A wins only in section 3. Change at most 6 files. Do not
remove or weaken any existing feature, skin, theme, layout, setting or
test. Finish with the CHANGED FILES manifest (file 12 §5) and real
build/test results; write NOT RUN for anything you did not run. Update
`brain.md`."* If a task needs more than 6 files, split it and say so.

### 17.3 Prompts (continue numbering after prompt 29)

**PROMPT 30 --- Phase 9a: audit + inventory.** "Audit the project
against Annex A §2 (PRESENT/PARTIAL/MISSING per row). Read
`New Player Concept/Namida` and `New Player Concept/PixelPlayer` and
their upstream repos; determine PixelPlayer's language and structure.
Produce `docs/spec/PARITY_INVENTORY.md` (every feature, screen, skin,
theme, layout; start from §6 IDs, add what you find),
`docs/THIRD_PARTY_NOTICES.md` (licences) and `docs/DECISIONS.md` (legal
gate, flavor decision). No code changes. Stop and report the licence
findings." **PROMPT 31 --- Phase 9b: foundations.** "Add product flavors
`offline`/`full`, `Capabilities`, `AppError` + `ErrorReporter` +
`UiEvents`, the settings IA skeleton (every section from §5.6 with rows
for every catalogue ID, showing `Unsupported`/`Planned`-free truthful
states: only implemented rows are enabled), and `MIGRATION_3_4` with
exported schema, pre-migration copy and `Migration3To4Test`. Update
`ThemeRegistryTest`/add `PlayerLayoutModeTest` skeleton." **PROMPT 32
--- Phase 10a: tokens and theme audit.** "Introduce `PulseColors`
extended tokens, replace hardcoded colours across all
screens/dialogs/sheets/snackbars/system bars, add the colour allow-list,
contrast unit tests and the grep gate. Report every file touched."
**PROMPT 33 --- Phase 10b: colour sources, shape, navigation,
micro-interactions.** "Implement Material You, artwork-driven and
seed-colour sources, time-of-day schedule, `LocalCornerScale`,
navigation styles (top tabs default, bottom, floating, rail),
`PulseClickable`. Add the new skins (Forest Mist, Material You tonal,
Artwork-driven) to the Skin picker. Nothing existing removed." **PROMPT
34 --- Phase 11a: Atmospheric Queue.** "Implement §7.1--§7.5:
`PlayerLayoutMode.ATMOSPHERIC_QUEUE`, `AtmosphericBackground`,
`AtmosphericQueuePlayerLayout`, `QueueRowUi` mapping, previews
(preview-only sample data 'Hurts So Good (Lyric...)' /
'AstridSofficialVEVO'), wiring into `NowPlayingScreen` from the real
queue, `PlayerLayoutModeTest`. Verify the light/dark queue-card tokens
and the amber contrast test." **PROMPT 35 --- Phase 11b: layout picker
and live button presets.** "Implement §7.6 and §8: layout picker sheet
with live thumbnails, swipe-strip cycling, `ButtonPlan`/`arrange()` for
all 15 presets, the Buttons tab in the picker, persistence of
`playerLayoutMode`/`buttonPresetId`, tests (`ButtonPlanTest`,
`ButtonPresetRegistryTest`, UI test)." **PROMPT 36 --- Phase 11c:
waveform, visualizer, particles, party mode.** "Implement the audio
level tap, waveform generation/cache and seekbar style, visualizer
styles, particles and party-mode glow; all off under reduced
motion/battery saver. No `RECORD_AUDIO`." **PROMPT 37 --- Phase 11d:
Namida/PixelPlayer designs.** "From the inventory, re-create each
additional skin, player theme and layout mode in code and register them
in the pickers (one batch of at most 6 files per reply; continue until
the inventory design rows are all done). Keep ids stable; update
registry tests." **PROMPT 38 --- Phase 12a: metadata and library.**
"Schema-backed separators, feat. extraction, duplicates, filters,
sorting options, folder images/`.info.txt`,
ratings/moods/tags/languages, and the new pages (Genres, Queues, Most
Played, Moods, Tags, Ratings, Languages, Stats) with page
order/visibility settings." **PROMPT 39 --- Phase 12b: tag editor.**
"TagLib-based reader and editor with `createWriteRequest`, confirmation,
optional backup copy, post-write rescan, error cases and tests."
**PROMPT 40 --- Phase 12c: search, smart playlists, Daily Mix, stats,
queues, history, gestures.** "Implement NAM-Y2, Y5--Y12, Y14--Y17 and
PIX-D2/P2 per §15.1 (split across two replies if needed)." **PROMPT 41
--- Phase 13: sound engine.** "Implement the processor chain, parametric
EQ + AutoEQ import, effects, ReplayGain, per-track profiles,
speed/pitch, output device/mono, audio path viewer, sleep timer by
tracks and pausing scenarios, `CrossfadeEngine` (experimental),
capability-gated bit-perfect/float. Grep gate: no `.volume =` outside
`AudioTransitionController`." **PROMPT 42 --- Phase 14a: Classic backup
v2.** "Implement §9 end to end with the error matrix and tests. Keep the
v1 JSON byte-compatible." **PROMPT 43 --- Phase 14b: permissions and
error sweep.** "Implement §14 onboarding and the Permissions page;
convert every I/O boundary to `AppError`; add the coverage table results
and tests." **PROMPT 44 --- Phase 15: online layer.** "Implement
`NetworkGateway`, LRCLIB, Deezer, AI providers with the consent screen,
history importers, Android Auto (`MediaLibraryService`), Cast, extra
widgets (split across replies)." **PROMPT 45 --- Phase 16: sources and
LAN.** "Implement `MusicSource` for Subsonic, Jellyfin, WebDAV, SMB,
download manager and cache, LAN sync and Listening Party; implement
streaming features only if `DECISIONS.md` says go." **PROMPT 46 ---
Phase 17: final audit and delivery.** "Run:
`./gradlew assembleOfflineDebug assembleFullDebug testDebugUnitTest lintDebug`
(and `connectedDebugAndroidTest` if a device exists). **All 137+
existing tests must still pass**; report exact counts. Run the grep
gates (section 18.2). Fill the traceability table (section 18.1) with
real statuses. Write README and docs (§16). Only if build and tests
pass: commit and push. Fix nothing silently; list every failure."

## 18. Final traceability audit and completion report

### 18.1 Traceability table (the final reply of prompt 46)

One row per ID in section 6 plus every ID added by the inventory:
`ID | Status (IMPLEMENTED / GATED(reason) / UNSUPPORTED(reason shown in Settings) / NOT DONE) | Where in Settings | Test | Evidence`.
**Hidden is not a valid status.** Also one row per skin, player theme,
layout mode and button preset (counts must match the registries).

### 18.2 Grep and gate list (extends file 17 §5)

`TODO-TEMP`=0 · `.volume =` outside `AudioTransitionController`=0 ·
`available = 0` writers=1 · `openDatabase` on archive=0 ·
`fallbackToDestructiveMigration`=0 · Media3
`shuffleModeEnabled = true`=0 · `play_events` inserts outside
`SessionTracker`/`HistoryWriter`=0 · `RECORD_AUDIO` /
`MANAGE_EXTERNAL_STORAGE` in manifests=0 · `INTERNET` in `offline`
flavor manifest=0 · network classes referenced outside `net` package=0 ·
hardcoded colours outside allow-list=0 · empty `catch` blocks=0 · word
"compatible" in UI strings=0 · emojis in strings=0.

### 18.3 Completion report v2 (exact shape)

    COMPLETION REPORT v2
    Builds:       assembleOfflineDebug → PASS|FAIL   assembleFullDebug → PASS|FAIL   (APK sizes)
    Lint:         PASS|FAIL|NOT RUN
    Unit tests:   X passed / Y failed / Z skipped   (existing 137 all present: yes|no)
    Device tests: X / Y / Z | NOT RUN
    Manual checklist: file 17 §2 items 1–11 + new: Atmospheric layout, layout cycling, 15 button presets, Classic v2 export/import, onboarding, light/dark/AMOLED sweep, widths 320–1280, fold/landscape
    Grep gates:   (section 18.2, numbers)
    Counts:       skins N, player themes N, layout modes N, button presets 15
    Traceability: IMPLEMENTED a / GATED b / UNSUPPORTED c / NOT DONE d  (list b, c, d)
    Performance:  cold start, jank, search latency, memory (real numbers or NOT RUN)
    Licences:     reference-app licence decision recorded: yes|no
    Not verified: Android Auto head unit, Cast receiver, bit-perfect on real DAC, acceptance of exported Full backup by the other app, anything marked NOT RUN

Rules: numbers come from real output; an invented PASS fails the audit;
every `NOT DONE` carries a reason and a next step.

## 19. Acceptance checklist (all must be true, or listed as Gated/Unsupported with a reason)

-   [ ] Every Namida feature (6.1--6.5) and every PixelPlayer feature
    (6.6) has a Settings entry and a truthful status.
-   [ ] Every Namida/PixelPlayer design appears in the Skins, Themes,
    Layouts or Player-theme picker; no existing item was removed.
-   [ ] `ATMOSPHERIC_QUEUE` exists, is the first and fallback layout
    mode, and its tests pass.
-   [ ] Layout picker and 15 button presets work during playback,
    animate, persist and restore.
-   [ ] Classic backup v2 writes JSON + audio copies to
    `Music/Pulse Player/`, imports with fuzzy matching, and handles
    every row of 9.3.
-   [ ] No colour leaks in any mode (grep gate + contrast tests +
    screenshot matrix).
-   [ ] No overlap at 320--1280 dp, landscape, fold, font scale 2.0.
-   [ ] Performance work done with real measurements or `NOT RUN`.
-   [ ] Error framework covers every I/O and network boundary with
    recovery actions.
-   [ ] First-launch onboarding requests only what is needed, with
    rationale and graceful denial.
-   [ ] README and docs written; licences recorded.
-   [ ] Both flavors build; `offline` has no INTERNET permission.
-   [ ] All pre-existing tests still pass; new tests added; commit and
    push only after green.
