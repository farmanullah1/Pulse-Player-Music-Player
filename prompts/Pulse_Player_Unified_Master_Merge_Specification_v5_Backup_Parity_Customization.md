# Pulse Player --- Unified Master Merge & Implementation Specification

> **Expanded revision:** Annexes B–D add detailed UX, workflows, feature-completeness rules and no-dead-control requirements. Annex E adds the verified upstream feature-parity baseline, license-aware design integration, a complete in-player customization system and the definitive Classic Backup v2 contract. The document remains a requirements prompt, not application source code.

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

This file merges the supplied Pulse Player specifications into one
authoritative document. The main specification defines product behavior;
Annexes A–E add technical decisions, traceability IDs, implementation
prompts, detailed workflows, recovery rules and conflict resolutions.

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
-   **Annex B --- expanded UX and workflow contract:** the additional screen-by-screen visual direction, interaction states, complete feature workflows, full button rearrangement editor, recovery matrix, permission education, accessibility behavior, release gates and Google AI Studio operating rules.
-   **Annex C --- feature completeness and no-dead-control contract:** required components, real-data workflows, persistence, capability gates, error/recovery states, tests and Google AI Studio anti-placeholder execution rules.
-   **Annex D --- exhaustive feature-by-feature specification:** complete workflows and required UI/domain/data/platform components for the existing Pulse Player experience and every feature named in the merged catalogue, including a coverage register and final no-omission audit. Annex D expands requirements; it does not remove or weaken them.
-   **Annex E --- upstream parity, design integration, complete customizer and Classic Backup v2:** the verified feature catalogue from the current Namida and PixelPlayer public material, license-aware rules for independently recreating design concepts, an organized in-player customization system, and the definitive folder-plus-audio-copy backup/import contract. Annex E resolves and supersedes older contradictory wording only for those subjects.

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
8.  Annex D is the exhaustive feature-by-feature expansion of the product requirements. It adds implementation detail for baseline and merged capabilities; it does not remove or weaken any requirement in the main specification or Annexes A–C. Where Annex D makes a previously broad requirement more specific, use the more specific behavior while preserving existing working behavior and the explicit conflict resolutions in Annex A §3.
9.  Annex E is authoritative for the verified upstream parity baseline, license-aware design integration, the in-player customization editor and Classic Backup v2. It supersedes older contradictory statements on those exact subjects, including older descriptions that say Classic backup never copies audio or that the current Namida license is GPL-3.0. It does not authorize deletion of any existing Pulse Player feature, theme, skin, layout or setting.

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

The requested enhancement is a self-contained, user-visible folder backup. The JSON remains metadata-only; the music is copied as separate ordinary audio files beside it. Together, the JSON and those copies form the Classic backup set. This enhancement supersedes the older File 07 sentence saying that nothing is copied.

-   Use `Music/Pulse Player/` as the canonical destination.
-   Export every favourite record using the exact existing
    `pulseplayer.favorites` v1 schema and decoder-compatible key set.
-   Copy each available local favourite audio file byte-for-byte into
    that same folder only after explicit user confirmation. The JSON does
    not embed audio bytes; the folder contains both the `.json` file and
    the separate music files.
-   Never move or delete the original audio.
-   Avoid overwriting same-named files without a safe
    conflict-resolution flow.
-   Report copied, skipped, missing, duplicate and failed files.
-   Keep JSON and audio-copy operations recoverable.
-   Import by detecting the JSON and audio files the user has placed
    together in `Music/Pulse Player/` on the destination device; use the
    Storage Access Framework (SAF) folder picker for the JSON/tree grant
    on modern Android, with manual JSON selection as a fallback.
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

Perform a complete feature-by-feature parity audit against the current
Namida and PixelPlayer repositories and documentation, and implement
every compatible, approved capability that is in scope. Recreate useful
interaction patterns and broad design concepts through original Pulse
Player components; do not copy protected source, assets or exact screen
compositions. Preserve every existing Pulse Player feature and design.
**Namida** (`github.com/namidaco/namida`) is a Flutter music/video player
and a feature/interaction reference. **PixelPlayer**
(`github.com/PixelPlayerHQ/PixelPlayer`) is a Kotlin/Jetpack Compose
Android player and a closer implementation-technology and Material 3
Expressive design reference. Verify current repository state before work.

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

  C12               "No copyrighted images"       "Copy the exact designs"                             Recreate the **general visual concepts and interaction quality**
                                                                                                       as original Pulse Player UI. Do not make pixel-for-pixel clones
                                                                                                       or copy screen compositions, artwork, logos, fonts, icons,
                                                                                                       animations or source code unless the exact item is separately
                                                                                                       licensed for this use or written permission is obtained.
                                                                                                       Backgrounds stay original/procedural.

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
3.  **Licence check.** Record the exact repository URL, inspected commit/tag,
    inspection date, top-level licence, per-file exceptions, asset licences
    and every third-party library licence in `docs/THIRD_PARTY_NOTICES.md`.
    The current official Namida repository identifies a custom EULA, not
    GPL-3.0; the EULA limits use and prohibits rebranding/redistribution
    without permission. The current official PixelPlayer repository shows
    a Proprietary License; its README says only portions contributed before
    2026-05-12 remain MIT, subject to the relevant notices and proof that a
    specific file/commit is actually covered. Do not assume the full
    repositories, later commits, assets or screen designs are reusable.
    **Default policy: independently implement general feature behavior and
    broad UX concepts. Do not paste code, port assets or clone exact screens.**
    Obtain written permission and legal review before any exceptional reuse
    of restricted code, assets or distinctive protected material. Do not
    change Pulse Player's licence or distribution model implicitly.
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

The layout's presentation model must contain only the values needed to
render the current track, playback progress, transport state, repeat state,
favourite state, current tab, queue rows, artwork references and reduced-
motion preference. Queue rows must be presentation models mapped from the
existing authoritative queue items; do not replace or duplicate the
application's established Track entity. Keep all player actions as explicit
callbacks owned by the existing screen state holder. Collect high-frequency
playback progress only inside the progress component so it does not
recompose the entire player screen.

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
4.  Copy each available local favourite's original audio bytes in
    bounded streaming chunks to a unique safe filename (`name (1).ext`
    on collision; never overwrite). Preserve the original extension and
    byte content: do not transcode, normalize, trim, retag, move or delete
    the source. Show real file/byte progress and **Cancel** support. Use a
    temporary `.part` item and publish/rename it only after size and
    content verification. Deleted, unreadable, removed-media, revoked-
    permission, remote and stream-only sources are marked not copied; do
    not omit their favourite metadata from the JSON.
5.  After the final file names are known, write
    `PulsePlayer_Favorites_<date>.json` into the same folder using the
    unchanged v1 codec and exact v1 keys. For successfully copied items,
    set the existing v1 `displayName` field to that copy's final filename;
    keep all other v1 field meanings and values unchanged. For a favourite
    whose audio could not be copied, retain its original display name and
    metadata so it can still match an already-existing destination track.
    Write to a temporary JSON file, validate by decoding it, then publish
    the final `.json` file atomically. Do not add fields, change
    `schemaVersion`, or invent a v2 JSON schema.
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
2.  **Detection:** scan/index audio in `Music/Pulse Player/` through
    MediaStore and obtain access to the JSON through a SAF folder/document
    picker. Android 11+ MediaStore audio queries do not grant arbitrary
    JSON-file access. Offer a one-time **Choose Pulse Player folder**
    action using `ACTION_OPEN_DOCUMENT_TREE`, supply
    `DocumentsContract.EXTRA_INITIAL_URI` where supported to navigate
    toward the Music folder, and persist the read grant after the user
    selects the folder. Auto-detect the newest valid
    `PulsePlayer_Favorites_*.json` (or another JSON file that passes the
    v1 schema sniff) in that chosen folder. Never claim the picker is
    already inside that folder if Android does not honor the initial URI.
    Always retain **Choose a JSON file** as a fallback.
3.  Decode, scan or refresh the selected folder if required, then run
    the unchanged three-level matcher against available library tracks.
    Show the existing preview (will add / already favourite / needs
    choice / not found / invalid / duplicates). Clearly distinguish
    "JSON item found a matching track" from "audio copy is present in
    this backup folder". If files are still indexing, offer Scan now and
    rerun preview after the scan completes.
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

Use one typed application-error catalogue. Each error category must
carry a safe user-facing message, a non-sensitive diagnostic category,
the affected operation where appropriate, and one or more valid recovery
actions. Required categories include permission, storage, I/O, parsing,
playback, network, authentication and unsupported-capability failures.
Translate platform and library exceptions at subsystem boundaries; do not
leak raw exceptions into normal UI.

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

-   [ ] Every feature discovered in the current Namida and PixelPlayer
    repositories/docs/changelogs has an individual parity row, a truthful
    status and evidence; every transferable design concept is represented
    in the correct Pulse Player screen, component, navigation, skin, theme,
    player layout or customization setting. Non-theme features are not
    incorrectly stuffed into a theme picker; no existing item was removed.
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

---

# Annex B --- Expanded UX, Visual Design, Feature Workflows and Recovery Contract

> **Why this annex exists:** The master specification and Annex A define
> the product scope and major technical constraints. This annex removes
> the remaining ambiguity about what a user should see, what each feature
> does from start to finish, how every important state is presented, and
> how the new **Rearrange Player Buttons** feature must behave. It is a
> product and implementation-direction prompt, not application source
> code.
>
> **Authority:** Annex B is additive. Annex A's C1--C17 conflict
> resolutions remain authoritative for the exact conflicts they cover.
> The actual repository remains the source of truth for existing
> behavior. Never use an Annex B detail as permission to remove, reset or
> silently change a working feature.

## B0 --- Non-negotiable definition of a complete feature

A feature is not complete because a screen, switch, menu item or button is
visible. Every feature must have the following end-to-end contract:

1. **Discoverability:** The user can find it from the expected screen or
   Settings category. The entry point has a clear name and, where needed,
   a short explanation.
2. **Eligibility:** The app checks whether the capability is supported
   by the Android version, device, build flavor, audio route, permission
   state and service configuration before offering an enabled action.
3. **Input:** The user can provide the required selection, file, folder,
   rule, value or confirmation without ambiguity.
4. **Execution:** The operation uses the existing repository, playback
   owner, queue, storage workflow or integration boundary. It must not
   create parallel state or bypass existing safety policies.
5. **Progress:** Long-running work displays meaningful progress,
   current phase, counts and a safe cancel action where cancellation is
   possible.
6. **Success:** The result is reflected everywhere from the same source
   of truth. Show actual counts and the real outcome, not a generic
   "Done" message.
7. **Failure:** Failure produces a plain-language explanation, the
   affected operation, and an appropriate recovery action. A failed
   operation must not be reported as successful.
8. **Persistence:** State that users expect to survive navigation,
   process death or relaunch is stored and restored safely.
9. **Privacy:** The UI explains where data is stored and whether any
   data leaves the device. Optional remote services require explicit
   opt-in.
10. **Accessibility:** The feature works with TalkBack, large text,
    keyboard/focus navigation where relevant, and reduced motion.
11. **Verification:** A test proves the important behavior, including
    at least one failure or cancellation path.
12. **Documentation:** The feature is listed in the feature matrix as
    PRESENT/WORKING, PARTIAL, BROKEN, MISSING, UNVERIFIED, GATED or
    UNSUPPORTED-with-reason as appropriate.

No feature may be represented as available merely because its setting
can be toggled. If it is not implemented in the current build, label it
as not included, unsupported or planned; do not use a fake control.

## B1 --- Global visual design system: how Pulse Player should look

### B1.1 Product visual direction

The visual identity is **Pulse Player**: polished, atmospheric, artwork-led
and music-focused, with a strong AMOLED option. It should feel like a
purpose-built premium music application, not a generic settings demo or a
direct clone of either reference project.

Combine these principles:

- Keep the established Pulse Player visual language: true-black
  surfaces, rounded album artwork, expressive gradients, cyan
  favourites, orange apply/using actions where already established,
  restrained glass surfaces and clean typography.
- Use Material 3 and selectively use Material 3 Expressive ideas:
  dynamic colors, adaptive components, expressive but controlled shapes,
  clear selected states and meaningful motion.
- Let album artwork influence accent colors only when the user enables
  artwork-driven color. The extracted palette must be readable and
  stable; reject muddy, low-contrast or nearly identical colors.
- Use atmosphere and motion as supporting decoration. Artwork, title,
  artist, seek position and playback controls remain the primary content.
- Avoid visual overload. Do not animate every surface continuously.
- Maintain consistent component meaning. A filled button always means
  the primary action; tonal means secondary; text or outlined controls
  mean tertiary or low-emphasis actions.
- Preserve the nine existing player themes and all existing skins.
  New references-inspired choices are additions, not replacements.

### B1.2 Color and contrast behavior

Use semantic color roles rather than choosing colors independently on
each screen.

Required semantic roles:

- App background and elevated background.
- Primary, secondary and tertiary surfaces.
- Primary, secondary and disabled text.
- Primary, secondary and tertiary accent.
- Artwork-derived accent and muted artwork-derived accent.
- Favorite on/off.
- Playback active/inactive.
- Queue current, queued-next and unavailable.
- Focus, hover, pressed, selected and disabled state layers.
- Success, warning, error and informational state.
- Modal scrim, blur/glass surface and divider/outline.
- Atmospheric Queue's upper dark area and queue-card area.
- Active amber on dark and active amber on light.

Rules:

- Dark and AMOLED modes must not use washed-out grey text on black.
- Light mode must not use white icons on pale surfaces.
- A queue highlight must be distinguishable by at least two visual
  signals, such as color plus weight or color plus an indicator.
- The active queue amber is adapted to its surface; do not reuse a
  dark-surface amber foreground on a light card if contrast fails.
- Normal text must target at least 4.5:1 contrast; large text and
  meaningful icons must target at least 3:1, subject to the established
  contrast audit.
- Disabled controls remain understandable. Their disabled state must
  not be confused with an active or selected state.
- Color must never be the only signal for playing, selected, favorite,
  unavailable, error or success.
- Do not hardcode a color inside a feature screen when an equivalent
  theme token exists.
- A feature-specific accent is allowed only when documented in the
  color allow-list and tested on every supported surface.

### B1.3 Typography hierarchy

Use a restrained, consistent text hierarchy based on system typography
unless an intentionally licensed bundled font has been approved.

- Display/headline: current track title, major page title and hero
  content. Use sparingly.
- Title: section headings, album/playlist names and important cards.
- Body: track metadata, settings descriptions and normal content.
- Label: chips, small metadata, duration, counts and button labels.
- Numeric/tabular: playback times, queue counts and statistics where
  alignment helps.

Rules:

- Track title is visually stronger than artist, album or file metadata.
- Page title is stronger than a section label.
- Secondary metadata must remain legible and must not compete with
  track names.
- Use tabular numerals for elapsed/total time and key statistics where
  practical.
- Avoid using letter spacing, all-caps or tiny text as the only means
  of hierarchy.
- Never reduce text below an accessible size simply to preserve a
  decorative composition.
- Truncate only when the full value is available in an accessible
  description or a details screen.
- Long titles must not cover playback icons. Use measured layout,
  wrapping or a marquee only when reduced motion is off.

### B1.4 Spacing, shape and density

- Use a 4dp spacing system.
- Use consistent page gutters, normally 16dp on compact phones and
  adaptive wider gutters on expanded screens.
- Cards should use a consistent family of rounded corners; do not give
  every element a different radius.
- Use a large radius for hero/feature cards, medium radius for standard
  cards and smaller radius for compact thumbnails and chips.
- Keep album artwork square unless the actual content is intentionally
  wide.
- Standard list rows should have enough height for two lines of text,
  artwork and an accessible action button.
- Provide a compact density option, but do not shrink touch targets.
- Artwork should crop consistently and use the same fallback treatment
  across screens.
- Shadows and glow must remain subtle and must not be used to fake
  hierarchy that should come from spacing or contrast.

### B1.5 Button hierarchy and interaction appearance

Every button must have:

- A clear action name or accessible description.
- An icon that matches its meaning.
- A minimum 48dp interactive target.
- A visible pressed/focused/selected/disabled state.
- An explanatory disabled reason when the user may reasonably expect
  the action to work.
- A loading state for asynchronous actions.
- Protection against accidental repeated destructive actions.
- A consistent visual style for the same action class throughout the
  app.

Use icons consistently. Do not mix unrelated icon families or emoji.
Icon-only controls must expose a meaningful accessibility label.
Overflow menus must not become a hiding place for frequently used
actions that should remain directly accessible.

### B1.6 Motion and feedback

Use the app's shared motion scale and existing spring language. Suggested
motion roles:

- Quick press/selection feedback: approximately 100–150ms.
- Small control transitions: approximately 150–220ms.
- Sheet or layout transitions: approximately 220–320ms.
- Larger screen transitions: approximately 300–450ms.
- Staggered content entrances: first-load only and brief.

Rules:

- Every animation communicates a state change, navigation change or
  meaningful response.
- Do not animate a control in a way that makes its action hard to tap.
- No perpetual motion when the relevant content is off-screen.
- Respect system animation scale, reduced-motion and battery-saver.
- When reduced motion is enabled, use instant state changes or short
  fades rather than removing important visual state feedback.
- Haptics are optional, configurable and never the only feedback.
- A successful favorite toggle gives immediate visual feedback; the
  database write remains authoritative.
- A failed action must not leave the button showing a success state.

### B1.7 Standard visual states for every screen

Every screen and component must define all applicable states:

- **Initial/loading:** stable skeleton or progress indicator; do not
  display fake content that resembles real songs or counts.
- **Content:** real items, meaningful sorting and clear selection.
- **Empty:** explain why the list is empty and provide the next useful
  action.
- **Filtered empty:** distinguish "no items match this filter" from
  "there is no library content".
- **Permission limited:** explain what is accessible and offer a folder
  picker or Settings action.
- **Offline/unavailable:** preserve local content and identify the
  unavailable online or hardware capability.
- **Partial success:** show completed, skipped and failed counts.
- **Error:** explain the cause in normal language and provide safe
  recovery.
- **Disabled:** show why an action is disabled.
- **Selected/multi-select:** clear selection count and available batch
  actions.
- **Destructive confirmation:** identify exactly what will be removed
  or changed and whether the operation is reversible.

## B2 --- Screen-by-screen visual and interaction specification

### B2.1 App launch, first-run and main shell

**Visual structure**

- Use the existing application shell and navigation model.
- Keep the top app bar visually compact and aligned to the page content.
- The title and page actions must not compete for the same space.
- On compact phones, keep the tab row horizontally scrollable when
  necessary and preserve the mini-player above the bottom system inset.
- On medium widths, use the established navigation rail.
- On expanded widths, use the rail, central content and docked Now
  Playing panel where appropriate.
- The app shell must retain scroll position when changing between
  top-level destinations unless the user deliberately refreshes or
  resets the destination.

**First-run behavior**

- Explain what Pulse Player does: plays local music and can optionally
  access user-selected folders.
- Explain the core privacy position before optional connections are
  offered.
- Do not show an empty, dead dashboard with no next step.
- If audio permission is not granted, explain the limited mode and offer
  the system permission or SAF folder selection.
- If the library is empty, show an intentional onboarding card with
  Scan and Choose folder actions.
- Do not create a fake sample library in production.
- A dismissed onboarding hint must not reappear on every launch.
- The user can revisit onboarding and permission explanations from
  Settings.

### B2.2 For You / Home

**Visual hierarchy**

1. Top greeting or page title and the existing search/settings actions.
2. Resume/Continue card only when a real previous track or queue exists.
3. Two prominent actions: Shuffle and Play.
4. Compact utility tiles for Today's listening, Favorites, Sleep timer
   and Equalizer.
5. Recently added.
6. Top picks / most played when enough real history exists.
7. Smart Mix, Daily Mix, Lost Memories or other local collections only
   when enough real data exists.

**Behavior**

- Continue resumes the last saved queue and position in the established
  paused/restored state; it must not invent a new queue.
- Shuffle uses the current library scope and the authoritative queue
  model.
- Play follows the defined "start from first track" behavior or the
  current page's play context.
- Today's listening comes from session-based statistics and excludes
  paused/buffering time.
- Favorites count is the real count from the same source of truth as
  the Favorites page.
- Recently added sorts by the actual date-added field and uses stable
  tie-breaking.
- Top picks remain hidden until meaningful history exists.
- Smart collections must show a short "why this is here" explanation
  where useful.
- Tapping a tile opens the matching detail or tool, not an unrelated
  generic page.
- Each card has an intentional loading, empty and failure state.
- Refreshing Home must not launch multiple full-library scans.

**Visual behavior**

- Use a balanced card grid with consistent widths and a clear order.
- Hero content may use an artwork-derived gradient with a contrast
  scrim.
- Keep the screen scrollable and avoid nested horizontal/vertical
  scrolling conflicts.
- Do not place more than one dominant hero card above the primary
  actions on a compact phone.

### B2.3 Songs / Tracks

**Row appearance**

- Left: square artwork or a designed gradient fallback with a music
  icon.
- Centre: title on the first line; artist and album on the second line.
- Right: optional date/duration metadata where width allows, followed by
  a clear overflow action.
- Current playback item: subtle accent or playing bars; do not make the
  entire list flash.
- Favorite state: small, consistent indicator only if it helps scanning.
- Missing/unavailable track: visible but clearly muted and marked;
  do not silently delete it from favorites or playlists.

**Primary actions**

- Play the row.
- Play next.
- Add to queue.
- Add to playlist.
- Toggle favorite.
- Open details.
- Share the original file through the supported content URI.
- Set as ringtone when the platform grant and file type allow it.
- Add to or remove from a selection.
- Open the containing folder or album when possible.

**Sort and filter workflow**

- Sort menu shows the selected field and direction.
- Sort options include the available metadata fields in the feature
  catalogue.
- Each library page remembers its own sort selection.
- Filters are clearly marked and can be cleared individually or all at
  once.
- Minimum duration and file-size filters affect displayed results only
  unless a separately documented policy says otherwise.
- Hidden or excluded tracks must never be marked unavailable simply
  because a display filter hides them.

**Multi-select workflow**

- Long press enters selection mode.
- The header or contextual bar shows the selected count.
- Select all applies to the current scope, not the entire device, unless
  the interface explicitly says otherwise.
- Batch actions report how many selected items are eligible.
- If a subset cannot be modified, identify those items and the reason.
- Exiting selection mode must not accidentally play or clear the queue.

### B2.4 Albums, artists, genres, moods, tags and ratings

**Albums**

- Use an adaptive artwork grid with consistent square tiles.
- Each tile shows album title and album artist, with truncation.
- Detail page includes a large cover, album name, album artist, year
  where known, track count and a real track list.
- Play and Shuffle operate on the album's actual tracks in the intended
  order.
- Missing artwork uses the shared fallback.

**Artists**

- Show artist name, available image or initial-based fallback, and
  real track/album counts.
- Artist detail groups albums and tracks in a predictable order.
- Featured artists can appear as relationships while preserving the
  original title/tag data.
- Do not create duplicate artist entries due to case, accent or
  separator differences without applying the configured normalization.

**Genres, Moods, Tags, Ratings and Languages**

- These pages are populated from indexed or user-entered metadata.
- Show only values that exist in real library data.
- A category opens its matching filtered track list.
- The user can sort, search and multi-select in the same way as the
  Songs page where appropriate.
- Missing metadata must not be represented as an invented category.
- A no-metadata state must explain how to add tags, ratings or moods.

**Folders**

- Show source/folder identity, track count and storage source.
- Distinguish internal storage, removable storage, SAF-selected folder
  and copied source.
- Folder details may use an approved folder cover and `.info.txt`
  description.
- Excluded folders remain excluded from normal library results and
  have a clear management route in Settings.
- SD-card removal produces an unavailable-source state, not deletion
  of the user's history or playlists.

### B2.5 Search

**Search entry and results**

- Search is available from the main shell and relevant library pages.
- Show recent searches only when enabled; include a clear action.
- Search scope can include title, artist, album, filename, folder,
  genre, tags, mood, lyrics, playlists and other indexed fields.
- Results are grouped by content type with counts based on actual
  results.
- A selected result opens its detail or plays according to the current
  Track Play Mode.
- A search result must preserve the origin context so Back returns to
  the same query and scroll location.
- Search within an album, folder, artist or playlist must not silently
  expand to the whole library.

**No-result behavior**

- Distinguish no matches from a still-indexing library.
- Suggest removing filters, broadening the query or changing scope.
- Show spelling-tolerant alternatives only when they are grounded in
  actual library values.
- Never fabricate a suggested track.

### B2.6 Mini-player

**Appearance**

- Floating rounded surface above the bottom navigation inset.
- Left: small artwork/disc treatment with a fallback.
- Centre: track title and artist, with marquee only when required and
  reduced motion is off.
- Right: play/pause and queue or expand action.
- A narrow progress line sits on the lower edge.
- Use a restrained glass treatment that adapts to Light, Dark and
  AMOLED modes; text must remain readable over artwork.

**Behavior**

- Tap or swipe up opens the full Now Playing screen.
- Collapse returns to the previous screen and preserves its scroll
  position.
- Play/pause updates immediately from the shared playback state.
- The disc rotation runs only while playback is active and motion
  settings allow it.
- If there is no current track, do not show a fake mini-player.
- The mini-player must not cover the last list row or the system
  navigation area.

### B2.7 Now Playing: shared behavior for every theme and layout

Every Now Playing design must preserve the same essential controls and
meaning:

- Collapse/back.
- Current track title and artist.
- Artwork or selected visualizer/background treatment.
- Lyrics entry/view.
- Seek with elapsed and total time.
- Shuffle, previous, play/pause, next and repeat.
- Favorite.
- Sleep timer.
- Add to playlist.
- Queue access.
- Equalizer.
- Track information and overflow actions.

**Interaction requirements**

- Tapping seek updates playback through the safe seek path.
- While the user is dragging the seek bar, do not fight the user's
  finger with frequent external progress updates.
- Unknown duration disables seeking with a clear explanation.
- Previous/next respect the existing queue, play mode and repeat state.
- Favorite is persisted from the shared favorite state.
- Sleep timer shows whether it is inactive, scheduled by time or set to
  end of current track.
- Queue access always opens the same queue source used by the playback
  service.
- An overflow item must be hidden or clearly disabled when unavailable.
- Changing theme, player layout or button preset never creates a new
  player, seeks the track, resets the queue or pauses playback.

**Track title and metadata**

- Show the title as the dominant text.
- Artist is secondary; album, source or channel can be tertiary.
- Never substitute a file name for a known title unless the metadata
  title is genuinely missing.
- Long title and artist must not overlap the action row.
- Provide a full metadata detail view.

### B2.8 Queue sheet and queue pages

- Clearly label the current track and upcoming tracks.
- Show the actual queue count, not a stale cached count.
- Current row has an accessible "Now playing" state, not color alone.
- The row menu provides Play next, Remove, Favorite and Details when
  supported.
- Drag reorder uses a visible drag handle or clear long-press cue.
- Provide Move up/Move down alternatives for accessibility.
- Remove action updates the authoritative queue immediately and
  updates the service state safely.
- Clear Queue requires confirmation and explains whether the current
  track remains active.
- Saving a queue records a user-defined name and the ordered track
  identities.
- Restoring a saved queue shows unavailable or missing tracks before
  the user applies it.
- A queue with no upcoming items has a useful empty/end-of-queue
  state.
- Queue scrolling must not be reset by every progress tick or metadata
  refresh.

### B2.9 Playlists and Smart Playlist builder

**Static playlists**

- Creation asks for a name and optionally a description/artwork.
- Rename and edit are non-destructive.
- Reorder is explicit and persists.
- Duplicate creates a new playlist and does not mutate the source.
- Delete requires confirmation; deleting a playlist does not delete
  audio files.
- Remove track from playlist is distinct from deleting a track from
  storage.
- Import/export M3U or M3U8 validates paths and reports unavailable
  entries.

**Smart Playlist builder**

Present a readable rule-building interface rather than a raw JSON or
technical expression editor.

1. User enters playlist name and optional description.
2. User chooses whether all rules or any rules must match.
3. User adds one or more rule rows.
4. Each rule selects a metadata field, operator and value.
5. Numeric fields support greater-than, less-than and range.
6. Text fields support contains, equals, starts with and does not
   contain when valid.
7. Date fields support before, after and relative time ranges.
8. Users can add exclusion groups and sort rules.
9. A preview shows the current match count and sample matching tracks.
10. User saves the rule set and chooses whether it updates dynamically.
11. Invalid or contradictory rules are explained before save.
12. Editing a smart playlist never deletes the underlying tracks.

The detail view explains why a selected track matches the playlist,
where practical. Conversion to static playlist makes a snapshot copy;
it must not destroy the smart playlist.

### B2.10 Lyrics experience

**Lyrics view**

- Use a readable line width and spacing.
- Current synchronized line receives the active highlight.
- Upcoming and previous lines remain visible enough to provide context.
- Tapping a timestamped line seeks to that point.
- Manual offset control is available from a clear menu or editor action.
- Scrolling manually must not be immediately overridden by automatic
  follow; provide an explicit resume-follow action.
- Plain lyrics have no fake timestamps or animated sync state.
- Missing lyrics show "No lyrics available" and offer local import or
  online lookup only when the build and setting allow it.

**Lyrics import/editor workflow**

- Let the user choose a local LRC/TXT file through SAF.
- Show the matched track before applying.
- If matching is ambiguous, ask the user to choose; do not attach to
  multiple tracks silently.
- Editor supports plain-text edit and synchronized timestamp editing
  only for formats the parser can safely preserve.
- Save creates a validated local lyrics record or file using the
  documented storage strategy.
- Keep the original file or previous lyrics available until the new
  version has been verified.
- Corrupt or unsupported formats are shown as read-only with an
  explanation rather than partially rewritten.

**Online lyrics**

- Must be behind the full build flavor, Offline-only master switch and
  individual provider switch.
- Show the provider and request status.
- User can preview what query/metadata is sent where practical.
- Do not send local file paths or unrelated library contents.
- Cache only the minimum useful response and allow the cache to be
  cleared.

### B2.11 Equalizer and audio settings

**Equalizer screen**

- Show a clear header with current output route and whether the effect
  is available.
- Show preset selection and custom/manual mode.
- If a parametric equalizer is supported, show frequency, gain and Q
  values with readable labels and reset controls.
- If only a platform equalizer is available, display the available
  bands and ranges accurately.
- Show Bass Boost, Virtualizer and Reverb only if the platform exposes
  usable controls.
- Show current on/off state and a Reset button.
- Provide a short explanation when an effect is unsupported or
  controlled by another app.
- Do not claim bit-perfect, exclusive, float or direct USB output
  without verifying the actual output path.

**Per-track audio profile**

- A per-track override must visibly indicate that it differs from the
  global setting.
- User can enable/disable individual profile components.
- Reset track profile restores inherited settings, not factory-resets
  the global equalizer.
- Profile values must be validated and applied without writing
  settings into another track's profile.
- Track switch restores the correct profile without an audible volume
  spike.

**Audio path information**

- Present source decoder, sample rate/format, processing/effect stages
  and output route only when known.
- Unknown values must say "Unavailable" or "Not reported".
- Distinguish desired format from actual output format.
- Do not state that a hardware path is bit-perfect unless the complete
  path is proven.

### B2.12 Statistics, history and recaps

- Show a clear date range and timezone context.
- Daily, weekly, monthly, yearly and custom range selectors must use
  the same session-based statistics policy.
- Show total listening time, unique tracks, total sessions and top
  artists/tracks only when real data exists.
- Charts must include an accessible text summary.
- Empty statistics explain that playback history is not yet sufficient.
- Imported history must be visibly labelled by origin and must not be
  silently mixed with live-session records.
- Removing an event changes statistics but never deletes the audio.
- Monthly and yearly recap cards must display the actual period covered.
- Share cards are generated locally, and private fields can be hidden.
- Timezone changes must not shift an event into the wrong day or month.

### B2.13 Settings

**Visual structure**

- Use grouped setting categories with descriptive titles.
- Each setting has a name and concise description.
- Related advanced controls are nested under a relevant group, not
  scattered across unrelated screens.
- Use a search field to locate settings by feature name and synonym.
- Each row shows its current state clearly.
- Advanced features may be expanded/collapsed but must remain
  discoverable through Settings search.
- Unsupported or gated features remain visible only where useful and
  show the reason they cannot be enabled.
- A destructive setting action uses a confirmation dialog.
- A setting that changes the current player appearance updates the
  preview immediately and persists only after the user's chosen apply
  behavior.

**Required settings group details**

- Appearance: theme mode, color source, seed color, skin, wallpaper,
  player theme, player layout, button arrangement, corner radius,
  navigation style, list density, animation, reduced motion.
- Library: scan sources, folder inclusion/exclusion, filters,
  duplicates, parsing, artist/genre separators, album-artist rules,
  page visibility/order, folder cover rules and tag editor.
- Playback: gapless, fade, crossfade where eligible, speed, pitch,
  repeat-N, play mode, insertion behavior, sleep timer, pause
  scenarios, queue persistence and history thresholds.
- Sound: equalizer, effects, AutoEQ, ReplayGain, loudness, mono,
  output route, per-track profiles and audio-path details.
- Lyrics: local import, file matching, parser support, editor,
  synchronization offset and optional providers.
- Gestures: row swipe, artwork gestures, mini-player gestures,
  layout-cycle gesture and haptic feedback.
- Online & Privacy: master switch, provider switches, data-saver,
  offline-only state, transmitted-data explanation and cache clearing.
- Sources: remote source setup, server status, cache priority and
  download state where applicable.
- AI: selected provider, encrypted key status, consent and preview of
  the information submitted.
- Connectivity: Cast, Android Auto, Wear OS, local sync, listening
  party and widgets, each capability-gated.
- Stats & History: threshold, origin filters, edit/import/export,
  privacy and recap controls.
- Backup & Transfer: Classic favourites, Full backup (reference
  format), export/restore history and clear result reports.
- Permissions: current permission status, purpose, denial consequence
  and fix action.
- About: version, build flavor, licence notices, known limits and
  privacy summary.

### B2.14 Backup & Transfer screen

Keep Classic and Full backup as separate cards or clearly separated
sections. Never label them as one combined backup.

**Each card must show**

- What it contains.
- Which formats it imports/exports.
- Last successful operation.
- Current permission requirement.
- Estimated size when it can be calculated.
- Export/import actions.
- A link to an explanation or help sheet.

**During an operation**

- Show the current phase: preparation, validation, copying/staging,
  verification, applying or cleanup.
- Show progress in real units where possible.
- Provide Cancel only when the current phase can safely stop.
- If cancellation occurs, state what was completed and what was
  retained.
- Do not report success until final verification completes.

**After an operation**

- Show added, updated, already present, skipped, unmatched, duplicate
  and failed counts as applicable.
- Show the output location or selected destination.
- Provide a safe Retry failed action when possible.
- Provide a copyable diagnostic summary without private paths or
  secrets.
- Do not automatically launch or open exported files.

## B3 --- Expanded feature workflow contracts

### B3.1 Library scan and indexing workflow

1. User opens Scan or chooses a music folder.
2. Explain the difference between device media permission and a
   user-selected SAF folder.
3. Ask the user to select the scan scope. Do not silently scan
   inaccessible or excluded locations.
4. Before scan, show which sources are included and which are excluded.
5. Start scan on a background worker and show source-specific progress.
6. Show discovered, added, updated, unchanged, skipped, duplicate and
   unreadable counts.
7. Let the user cancel safely. Cancellation keeps already committed
   valid results and does not mark unscanned tracks as missing.
8. Reconcile unavailable tracks only after a complete successful scan
   of that same source and scope.
9. Show a result summary with a route to review duplicates and
   unreadable files.
10. A later scan must be idempotent: it must not duplicate tracks or
    increment play counts.

**Scan failure rules**

- One unreadable folder must not fail the entire scan.
- A removed SD card must not invalidate unrelated internal storage.
- A revoked SAF grant must produce a recoverable folder-permission
  state.
- A database write failure must not leave an inconsistent visible
  count.
- If a scan is interrupted, mark it interrupted and offer Resume or
  Restart; never pretend it completed.

### B3.2 Duplicate detection workflow

- First detect exact stable identity matches.
- Then compare normalized metadata and duration within the configured
  tolerance.
- Consider file size and other available signals when useful.
- Never auto-merge tracks solely because they share a title.
- A duplicate review screen shows both candidate files, source,
  duration, size, format and path label (with sensitive path detail
  hidden by default).
- User can mark a preferred copy, keep both or exclude one from
  display.
- Exclusion is a visibility decision, not file deletion or
  unavailability.
- Favourites, playlists, history and saved queues must be reconciled
  carefully if the user explicitly chooses a canonical record.
- Provide Undo where the underlying decision can safely be reversed.

### B3.3 Tag editor workflow

1. Open Track Details and choose Edit tags, or select multiple tracks
   and choose Batch edit.
2. Read the original metadata and identify which fields are writable
   for the selected format and storage location.
3. Show a clear editing form with current values.
4. Unsupported fields are read-only with a reason.
5. Batch editing shows which values will be applied to all selected
   items and allows mixed values to remain unchanged.
6. Validate values, track/disc numbers, year and artwork size/type.
7. Show a before/after preview.
8. Explain that saving modifies the audio file's embedded metadata,
   not merely the app database.
9. Ask for explicit confirmation immediately before writing.
10. Offer an optional pre-edit backup copy when the storage workflow
    permits it.
11. If Android requires a user approval dialog for write access, launch
    that approval and re-check the grant when returning.
12. Write to a temporary or recoverable state if the format allows,
    verify the result by reading the tags back, then reconcile the
    library record.
13. On failure, preserve the original file, retain the unsaved form and
    show Retry or Cancel.
14. Never modify tags during scan or import without explicit user
    action.

### B3.4 Track Play Mode and queue insertion workflow

- Track Play Mode is selected from a clearly labelled playback
  context menu.
- Modes include selected track only, current search results, playlist,
  album, artist, genre and folder where supported.
- The selected mode is visible while active and can be reset to the
  default.
- Play next inserts immediately after the current track.
- Insert-after-latest-inserted keeps multiple inserted tracks in the
  user's selected order rather than reversing them.
- Add to queue appends according to the selected insertion policy.
- Repeat N times is visibly distinct from Repeat One and Repeat All;
  show remaining count where practical.
- Queue persistence stores stable track identities, order, current
  index and position; restore paused and explain missing entries.
- Media3's independent shuffle must remain disabled when QueueModel is
  the source of ordering.

### B3.5 Smart Tracks, Daily Mix and Lost Memories

- Use only real library metadata and local listening history unless a
  clearly disclosed AI provider is enabled.
- Smart Tracks must explain its basis, for example "Often played with
  this track" or "Shares artists and mood tags".
- Daily Mix should be reproducible for the current date or clearly
  explain when regenerated.
- Lost Memories should show the exact previous-year date range being
  compared.
- Exclude unavailable tracks, user-hidden folders and disallowed
  categories.
- Allow the user to refresh, save as playlist, tune exclusions and
  disable recommendations.
- If insufficient history exists, explain what activity is needed
  instead of filling the page with random samples.
- Never label deterministic local algorithms as AI.

### B3.6 Listening history import

- Import source must be selected and identified before parsing.
- Show supported import formats and required fields.
- Preview parsed events, date range, timezone assumptions and matching
  confidence.
- Clearly identify duplicates and records that cannot be matched.
- User chooses Merge or Cancel; any replacement behavior requires a
  separate explicit action.
- Imported events retain their origin and must not be counted as live
  sessions.
- Import is idempotent, transactional and recoverable.
- Never invent timestamps, durations or tracks when the source data
  omits them.

### B3.7 Local and remote library sources

**Local sources**

- MediaStore, SAF-selected trees and app-managed copied files remain
  first-class sources.
- Source permissions and availability are tracked independently.
- Reconciliation is per source; failure in one source must not mark
  another source's tracks missing.

**Remote sources**

- A remote source is added only after explicit user setup.
- Show server type, display name, address, last successful connection
  and authentication state.
- Store secrets in the protected credential store, not Room, logs,
  plain settings or backup files.
- Connection testing must not automatically import or download the
  entire server library.
- Sync allows the user to choose what metadata and playlists are
  imported.
- Offline cache shows total size, priority, availability and last use.
- Removing a source disconnects it; it does not delete local music or
  unrelated user playlists without a separate confirmation.
- Offline-only mode prevents the remote source from making new
  requests. Previously cached local content remains available.

### B3.8 Downloads and cache management

- Downloads exist only in a build and integration where they are
  legally and technically permitted.
- Each download shows the source, chosen quality/format, destination,
  estimated size and any metadata that will be written.
- The user must choose whether to download audio-only or video when
  both are supported.
- Download queue supports pause, resume, cancel and retry when the
  provider supports them.
- Partial downloads use temporary names and are never shown as
  completed tracks.
- Cache eviction follows user-visible priorities and never removes
  protected items without explicit user consent.
- Storage-full errors offer a review of cache usage and an option to
  free space, not an automatic destructive cleanup.
- Chapter splitting and sponsor-segment removal remain gated behind
  legal/provider review and a clear preview.

### B3.9 AI playlist generation

- AI playlist generation is optional and disabled unless the user
  chooses a supported provider and enables online features.
- User selects a goal such as mood, activity, genre, era or a natural
  language request.
- Before sending a request, show a preview of the exact information
  sent: ideally selected metadata summaries, not the whole library.
- API keys are entered by the user and stored in an encrypted
  Keystore-backed store.
- Never hard-code keys or place them in source control, Room, backup
  exports, analytics or logs.
- Provider response is treated as untrusted input and validated
  against actual local tracks.
- Unknown tracks in the response are rejected or reported; do not add
  phantom songs.
- Show the generated track list and let the user review, edit, save or
  discard it.
- Handle invalid key, quota, network timeout, malformed response,
  provider refusal and unsupported model with distinct explanations.
- Provide a local rule-based alternative when online AI is off, and
  label it Smart Mix rather than AI.

### B3.10 Listening Party and LAN sync

- These features are optional, local-network or online capabilities;
  they are not part of core offline playback.
- Pairing must use an explicit confirmation and an understandable
  device identity.
- The host controls the authoritative shared queue; guests cannot
  silently overwrite it.
- Show connection state, host/guest role, latency or synchronization
  status where measurable, and a leave/disconnect action.
- Never expose the user's full library or credentials by default.
- Share only selected playlist/queue metadata required for the session.
- Reconnection must not create duplicate participants or duplicate
  queue entries.
- A user can stop sharing immediately.
- LAN sync uses additive or conflict-preview behavior by default; it
  must not wipe the other device's data.
- Offline-only mode disables network transport and returns to local
  playback without losing the current local queue.

### B3.11 Video-related capabilities

- Video playback is separate from the core local audio player and must
  be optional.
- If local related-video matching is built, match using documented
  normalized metadata and allow the user to choose among multiple
  matches.
- Do not replace the local audio file with a remote video stream
  without user consent.
- Video gestures must have visible alternatives and adjustable
  sensitivity.
- Volume, brightness, seek, speed and fullscreen gestures must not
  conflict with vertical scrolling or system gestures.
- Subtitle/caption support must identify the actual supported format.
- If video support is not included in a build, hide the entry or show
  a clear "Not included in this build" explanation.



### B3.12 Additional upstream features discovered during the current reference review

Add these items to `docs/spec/PARITY_INVENTORY.md` even if the owner did not
name them individually. They are drawn from the current public Namida
documentation/release material and PixelPlayer repository description.
They remain subject to the offline, legal, privacy and capability rules
already defined.

**NAM-ADD-01 — Playback lock and long-press transport shortcuts**

- Add an optional Player Lock action for users who want to prevent
  accidental screen touches while the player is open.
- When locked, preserve essential visibility of the current track and
  playback state while disabling ordinary layout-changing and
  seek-changing touches.
- Provide a clear, deliberate unlock gesture or button that remains
  accessible and cannot be triggered accidentally.
- Lock state is a temporary UI interaction state, not a device lock,
  PIN or security feature. Never advertise it as privacy protection.
- Long-press shortcuts may be offered on supported transport controls:
  for example, restart current track, temporary 2x speed, rewind or
  fast-forward. Each shortcut must have a visible hint in a tooltip or
  help sheet and a setting to disable it.
- A long press must not conflict with drag handles, button rearranging,
  TalkBack, or the standard tap action.
- The temporary 2x speed shortcut must restore the previous speed when
  the user releases it, loses focus or leaves the player; it must not
  overwrite the saved per-track or global speed.
- If a shortcut is unavailable in the current player layout, show it in
  the player overflow or disable it with a reason.

**NAM-ADD-02 — Configurable home-screen widget**

- Keep the existing responsive Glance widget and its existing sizes.
- Add a widget configuration screen or configuration flow when Android
  launches widget setup.
- Allow the user to choose artwork visibility, background treatment,
  corner style, primary accent, displayed metadata, available
  controls and whether the widget opens Now Playing or the library.
- The user may choose a compact control set for smaller widget sizes.
- The widget must adapt to the actual allocated size and must never
  display controls that do not fit.
- Widget configuration is independent from the in-app player theme and
  button arrangement.
- Multiple widget instances may have independent visual configuration
  if the existing widget architecture supports instance-specific
  preferences.
- A widget must continue to reflect the same playback service state.
  Configuration must not create a second player or duplicate
  playback state.
- Provide a safe default configuration if the user skips setup.
- Widget update failures must not interrupt playback.

**NAM-ADD-03 — Video subtitles and captions**

- If optional local/remote video playback is implemented, identify
  supported subtitle/caption formats explicitly.
- Candidate formats include SRT, SSA/ASS and SBV in addition to any
  formats already supported by the chosen player.
- Do not claim support until parser and rendering behavior are tested
  with fixtures.
- Allow the user to select an external subtitle file where supported.
- Display subtitle language and source where known.
- Provide subtitle enable/disable, track selection, timing offset and
  readable style controls where supported.
- Subtitle parsing errors must not stop audio playback or corrupt the
  video item.
- Keep subtitle support out of the core local-audio experience if the
  optional video subsystem is not included.

**NAM-ADD-04 — Lyrics notification and external Now Playing events**

- Investigate status/notification lyrics only where Android's current
  notification, media-session and foreground-service rules permit.
- Do not promise that arbitrary text can be permanently placed in the
  status bar; use a supported notification or other platform surface
  and describe it accurately.
- Allow the user to turn lyrics notifications off independently from
  playback notifications.
- Investigate optional Now Playing broadcasts or a webhook/event
  endpoint for Tasker, scripts and scrobblers only in the full build and
  behind an explicit integration switch.
- Before enabling an external event destination, explain which fields
  may be sent, such as title, artist, album, playback state and
  timestamp.
- Never include full file paths, credentials or the user's whole
  library in broadcasts.
- Provide a way to disable a destination and clear its configuration.
- Local playback must remain fully functional when all external event
  integrations are disabled.

**NAM-ADD-05 — Playlist import from remote music servers**

- Where Subsonic/Jellyfin sources are included, allow the user to
  browse and explicitly import selected server playlists.
- Show server identity and playlist name in the import preview.
- Map tracks to existing local/remote identities using stable source
  IDs where available.
- Show matched, already present, unmatched and duplicate entries
  before applying.
- Do not replace a local playlist with a remote playlist unless the
  user explicitly chooses Replace and confirms it.
- Default behavior is additive Merge.
- A server disconnect must not delete playlists already imported into
  Pulse Player.
- Imported playlists retain source metadata so the user can understand
  where they originated.

**NAM-ADD-06 — Copy and move operations**

- The current Pulse Player preservation rules prohibit moving,
  renaming or deleting original audio as an automatic side effect.
- Explicit Copy workflows are allowed where the specification already
  defines them.
- The upstream reference's copy/move capability must be recorded in the
  parity inventory. **Move is not part of the current approved scope**
  because the existing owner rules prohibit moving original files.
- Do not implement Move unless the owner separately changes that policy
  in writing and approves a dedicated workflow with preview, source
  and destination verification, rollback/recovery, SAF handling and
  per-file confirmation.
- Never treat a copy as a move or delete the source after a successful
  copy.

**PIX-ADD-01 — Material 3 Expressive adaptation**

- Use expressive components as a design reference, not a reason to
  replace the Pulse Player component system wholesale.
- Evaluate expressive shapes, selected-state motion, dynamic color and
  clear large touch targets in the existing app shell.
- Keep the visual hierarchy and color tokens consistent with B1.
- Any expressive transition must respect reduced motion and must not
  slow navigation or playback control.

Every item above must receive a parity ID, implementation phase,
settings destination, status and verification note. Unsupported or
excluded items stay visible in the final traceability report with a
truthful reason.

## B4 --- Rearrange Player Buttons: complete feature specification

> This is a priority owner-requested feature. It is more than selecting
> one of several fixed presets. Users must be able to choose a preset
> quickly and open an editor to rearrange the playback and quick-action
> buttons themselves, while music continues playing.

### B4.1 Naming and conceptual separation

Use these user-facing labels consistently:

- **Player Layout:** the overall structure of the Now Playing screen.
- **Player Theme:** the visual treatment of the player artwork/background
  area.
- **Button Arrangement:** where playback and quick-action buttons sit.
- **Rearrange Buttons:** the editing action that opens the custom
  arrangement editor.

Do not use "theme", "layout" and "button arrangement" interchangeably.
The three pickers must remain separate and must explain their effect.

### B4.2 Entry points

The same editor/picker is reachable from:

1. Now Playing, through the existing layout/customize control.
2. A clearly named "Rearrange Buttons" action in the player overflow
   menu.
3. Settings → Appearance → Button arrangement.
4. A contextual edit action in the button-arrangement picker.

When opened from Now Playing, it must keep the current track visible
and must not stop, pause, seek or replace the queue.

### B4.3 Picker screen / bottom sheet

The picker has two clear tabs or sections:

- **Presets:** the existing 15 stable presets, each with a rendered
  miniature preview and a concise description.
- **Custom:** the user's saved arrangements, including edit, rename,
  duplicate, delete and create actions.

Each preview must represent the actual arrangement, not a static
decorative picture. Show the selected preset with a clear selected
state and a visible Apply/Using equivalent consistent with the existing
player-theme carousel convention.

The picker must also show:

- Current player layout compatibility.
- Whether the preset is recommended for the current width/orientation.
- Whether a preset is unavailable and why.
- A Reset to default action.
- A route to open the full editor.

Applying a preset is immediate and non-destructive. The picker closes
only according to the established interaction convention; do not
surprise the user by navigating away from Now Playing.

### B4.4 Rearrange Buttons editor visual design

The editor should look like a focused visual customizer:

**Header**
- Title: "Rearrange Buttons".
- Subtitle: "Move, show or hide player controls".
- Back/close action.
- Undo action while unsaved changes exist.
- Reset action.
- A clear Save or Apply action when the editing model requires it.

**Preview canvas**
- Shows a representative Now Playing player surface using the active
  player layout and selected theme.
- Uses the current real playback state where possible.
- Preview controls do not trigger playback while in edit mode.
- The preview shows drop zones and insertion indicators during a drag.
- The preview adapts to portrait/landscape and the user's selected
  screen profile.
- The preview is not a screenshot; it must reflect the current
  arrangement model.

**Button library**
Organize available buttons into readable groups:

- Transport: Previous, Play/Pause, Next, Shuffle, Repeat.
- Progress: Seek bar, elapsed time and total time. The seek bar is a
  structural control, not an ordinary icon button.
- Quick actions: Favorite, Sleep Timer, Add to Playlist, Queue,
  Equalizer and Lyrics.
- Playback extras: Back 10 seconds, Forward 10 seconds, Playback
  Speed, Pitch, Repeat N and Track Play Mode.
- Connectivity extras: Cast, Output Device and Android Auto only when
  capability-gated.
- Optional information controls: Track Details and Share.

Each item shows its icon, display name, short purpose and capability
state. An unavailable capability must not be draggable into an active
position as if it works.

### B4.5 Editing gestures and actions

The user can:

- Drag a button to a new allowed position.
- Reorder buttons within the same group.
- Move a button between permitted groups when that group supports it.
- Hide optional buttons from the direct surface.
- Restore hidden buttons from the button library.
- Choose icon-only or icon-plus-label presentation where space allows.
- Choose a compact or expanded control size within accessibility
  limits.
- Restore the selected preset's original arrangement.
- Undo the last edit.
- Save the current changes as a named custom arrangement.
- Duplicate an existing preset into a custom arrangement.
- Rename a custom arrangement.
- Delete a custom arrangement after confirmation.
- Reset all custom arrangements only after explicit confirmation.

Drag-and-drop behavior:

- Begin drag only after a clear long-press or drag-handle gesture.
- Show a floating preview of the dragged button.
- Highlight valid drop zones.
- Show a visible insertion marker.
- Auto-scroll a scrollable editor list when dragging near its edge.
- Do not let a drag escape into artwork gestures, page navigation or
  system back gestures.
- On invalid drop, animate the item back to its original position and
  explain why it cannot be placed there.
- Provide Move up / Move down controls for TalkBack and users who
  cannot drag.
- Do not make drag-and-drop the only way to edit order.

Visibility behavior:

- Hiding a button removes it only from the direct control surface.
- The hidden action remains available in a predictable More/overflow
  area or the button library.
- A capability becoming unavailable while selected must move to a
  disabled explanatory state or overflow; it must not become a dead
  button.
- Re-enabling a button restores it to the last valid position when
  possible.

### B4.6 Mandatory controls and safety constraints

The following controls must remain readily accessible in every
arrangement:

- Play/Pause.
- Previous and Next, either directly visible or in a clearly reachable
  playback overflow if screen constraints require it.
- Seek/progress where the track is seekable.
- Queue access.
- Favorite, unless the owner deliberately chooses to make it an
  optional direct action; it must remain one-step accessible.

Play/Pause is always visually dominant and must have at least a 56dp
touch target. Every other interactive button must keep at least a 48dp
touch target. A compact preset may reduce visual icon size but may not
reduce the touch target below the accessibility minimum.

The arrangement system must never:

- Remove an underlying playback capability.
- Change the meaning of an existing button.
- Place the same action twice unless the user explicitly creates a
  supported duplicate shortcut.
- Allow two controls to overlap.
- Put an active control inside a hinge, cutout, gesture edge or
  inaccessible region.
- Shrink controls below minimum touch dimensions.
- hide all routes to a core playback action.
- Change the playback queue or state during customization.

If a selected preset cannot fit the current device size, move the
lowest-priority optional actions to More and preserve the user's order
when they return to a larger layout.

### B4.7 Preset behavior and recommendations

Keep all 15 stable preset identifiers defined in Annex A. Each preset
must differ in actual placement or grouping, not only in color or
button size.

Add the following usability refinements:

- The default `centered` preset remains the safe fallback for unknown
  button-preset IDs.
- A preset preview must be generated from the same arrangement rules
  as the real player.
- Show "Best for one-handed use", "Best for small screens",
  "Best for landscape" or similar guidance only when that claim is
  true for the arrangement.
- Recommendations are advisory only. Do not automatically change a
  user's selected preset when the device rotates.
- If the active Player Layout cannot support a particular preset,
  explain the incompatibility and offer a compatible alternative.
- Preset changes animate without interrupting playback.
- A reset returns to the default arrangement, not to a different
  player theme or player layout.

### B4.8 Custom arrangement persistence

- Preserve the current global selected arrangement.
- Optionally allow distinct choices for portrait and landscape.
- Optionally allow a per-Player-Layout arrangement override, but expose
  this only if the Settings UI can explain it clearly.
- Store stable button identifiers and arrangement order, not screen
  coordinates tied to one device size.
- Recalculate placement for the current width, height, font scale,
  orientation and layout mode.
- Preserve user custom arrangements during application upgrades.
- Migrate unknown or removed button identifiers safely into the
  available-button library or More menu.
- Do not discard custom arrangements on a theme change.
- Limit user-created arrangements to the supported custom slots
  specified by the current product registry; if more are supported,
  explain and test the new limit.

### B4.9 Button arrangement settings

Settings → Appearance → Button arrangement must include:

- Current arrangement name and miniature preview.
- Change preset.
- Rearrange Buttons.
- Edit custom arrangement.
- Separate portrait/landscape setting when implemented.
- Per-layout override setting when implemented.
- Reset selected arrangement.
- Reset all custom arrangements.
- Reduced-motion behavior is inherited from global settings.
- Haptic feedback preference if the app supports it.

Settings changes must be reflected immediately in Now Playing without
restarting the app.

### B4.10 Button arrangement acceptance tests

The complete feature passes only when:

- All 15 presets appear with accurate miniature previews.
- Each preset has a unique stable ID.
- Unknown IDs safely fall back to `centered`.
- User can reorder at least the supported transport and quick-action
  buttons.
- User can hide and restore optional buttons.
- User can save, rename, duplicate and delete custom arrangements
  within the product's supported custom slots.
- Reset and Undo behave predictably.
- Portrait and landscape adaptation do not lose the saved order.
- No button action is duplicated, lost or made unreachable.
- No control rectangles intersect at tested widths and font scales.
- Applying an arrangement while playing does not change playback
  state, queue order, position, audio focus or service lifecycle.
- Configuration survives process death and app restart.
- TalkBack users can reorder using non-drag controls.
- Reduced motion disables the arrangement transition without
  disabling edit functionality.

## B5 --- Expanded error handling and recovery matrix

### B5.1 Error presentation rules

Use the least disruptive UI that still makes the problem clear:

- **Inline field message:** invalid value in an editor or setting.
- **Snackbar:** brief, recoverable action result with one primary
  action such as Undo or Retry.
- **Persistent banner:** a continuing condition such as lost folder
  permission, disabled online services or partial scan.
- **Blocking dialog:** confirmation or an operation that cannot
  safely continue without user choice.
- **Dedicated error/result card:** playback failure, restore failure,
  corrupt backup, source connection failure or other substantive
  operation result.

Do not show multiple snackbars for the same underlying event. Do not
show raw stack traces, internal class names, database statements,
authorization headers, access tokens or private full paths.

### B5.2 Error taxonomy and required context

For every error, record:

- Subsystem and operation.
- User-safe category.
- Whether the operation changed persistent state.
- Whether it is safe to retry.
- Whether partial work remains.
- Recovery options available for the current permission/capability
  state.
- Diagnostic reference or local error identifier.
- Whether the issue is expected (for example, user cancellation) or
  unexpected.

Cancellation is not automatically an error. It must be reported as
cancelled, with an accurate summary of completed work.

### B5.3 Subsystem recovery table

| Subsystem | Failure examples | User-facing response | Recovery and safety requirement |
|---|---|---|---|
| First launch | Media permission denied | Explain that Pulse Player cannot scan all device audio yet | Choose folders with SAF, retry permission or open system settings |
| SAF folders | Grant revoked, folder moved, inaccessible provider | Identify the unavailable folder without exposing a private path | Re-select folder; do not mark unrelated sources missing |
| Scan | Interrupted, unreadable folder, database write error | Show partial scan summary and exact stage | Resume/restart; only fully scanned source may be reconciled |
| SD card | Removed during scan or playback | Explain that removable storage is unavailable | Reinsert or choose another source; preserve metadata/history |
| Duplicates | Ambiguous matching candidates | Explain that multiple files may represent the same song | Ask user to choose; never silently discard a file |
| Playback | Unsupported format, file deleted, decoder error | Identify the track and explain that it cannot currently play | Retry once when transient; skip or choose another file; preserve queue |
| Playback service | Controller detached, service recreated, player lost | Show reconnecting state where possible | Reattach to the existing service; restore queue paused; no duplicate service |
| Audio focus | Another app takes audio focus | Explain pause/duck behavior only when useful | Follow platform audio-focus rules; do not repeatedly force resume |
| Audio effects | Equalizer/effect unsupported or route changed | Show which effect is unavailable for this route | Disable only that effect; keep playback running |
| USB/output | Device disconnected or exclusive path unavailable | Return to normal output when safe and tell user | Re-evaluate capabilities; do not claim the route is active |
| Lyrics | Missing, corrupt or wrong-track match | Explain whether no lyrics were found or parsing failed | Choose another file/provider; preserve previous valid lyrics |
| Artwork | Missing, unreadable, timeout or invalid image | Use shared fallback artwork | Retry only when useful; avoid blocking playback |
| Tag editor | Read-only file, denied write grant, unsupported field | Identify the field/file limitation | Keep edits in the form; request write approval only when saving |
| Playlist | Invalid M3U entry, missing file, duplicate entry | Show import counts and affected entries | Import valid items; let user review unresolved paths |
| Statistics | Invalid timezone/import event | Identify records that cannot be interpreted | Preview and skip invalid records; never fabricate missing dates |
| Classic backup | No folder grant, missing file, full storage, partial copy | Give exact counts and safe next action | JSON-only export, choose another folder, retry failed files or clean partials |
| Full backup | Invalid signature, corrupt ZIP, unsafe path, schema mismatch | State why the backup was rejected | Do not extract/apply; choose another file; preserve current database |
| Database | Migration fails, corruption detected | Explain that local data needs protection | Preserve a pre-migration copy; stop destructive migration; follow documented recovery |
| DataStore | Settings read failure | Restore safe defaults only for affected preference where possible | Preserve the previous data file for diagnostics; do not reset the library |
| Network | Offline, timeout, HTTP error, TLS failure | Identify provider and connection condition | Retry idempotent request; respect Offline-only and Data Saver |
| Authentication | Expired token or invalid credentials | Ask user to reconnect the named provider | Remove or replace credentials securely; never log secrets |
| AI | Invalid key, quota, refusal, malformed output | Explain provider response category | Edit key/request, wait for quota reset, or use local Smart Mix |
| Remote source | Server unavailable or certificate error | Identify source connection state | Retry, edit connection or disconnect; cached tracks remain local |
| Download/cache | Interrupted download, low space, invalid item | Show item and bytes completed where safe | Resume or delete partial file after confirmation; never mark partial as complete |
| Cast/Auto/Wear | Device missing or connection lost | Show disconnected state | Re-discover or continue on phone; keep local queue intact |
| Widget | Update or provider failure | Do not interrupt playback; update on next safe opportunity | Refresh state from playback service; avoid tight polling loops |
| Button editor | Invalid drop, unavailable action, incompatible preset | Explain the allowed destination or limitation | Return button to prior position; offer compatible preset or overflow |
| Customization | Saved preset references removed button ID | Explain that an unavailable control was moved | Preserve known order and move unsupported item into More |
| Storage | Low disk space or read-only volume | Show required and available space when known | Free space manually, choose another destination or cancel safely |

### B5.4 Retry policy

- Retry only operations that are safe and idempotent.
- A transient playback failure may retry once, then offer Skip or
  another track.
- Network GET requests may use bounded exponential backoff; do not
  retry POST/write operations blindly.
- Import and restore retries must use transaction or idempotency
  safeguards.
- File copy retries must detect completed verified files and must not
  create duplicate partial outputs.
- Never retry a permission denial in a loop.
- Never automatically repeat a destructive user operation.
- Retry buttons must be disabled while the same operation is running.

### B5.5 Diagnostics and privacy

- Diagnostic summaries are generated locally.
- Redact account tokens, API keys, passwords, full local file paths,
  remote URLs containing credentials and personal filenames where
  appropriate.
- Show a short error identifier that can be included in a support
  report.
- Detailed logs are local and opt-in to export.
- No crash telemetry or diagnostic upload is permitted unless a
  separately approved product decision changes the privacy policy.
- The UI must explain that sharing a diagnostic report is a user action.

## B6 --- Permission and first-run education: precise behavior

The app may introduce all permission categories in a short education
flow, but this does **not** mean that every Android permission dialog
should appear on the first launch.

Use this policy:

1. On first launch, explain the app and the local-library access choice.
2. Request audio access only when the user asks to scan the device or
   otherwise enters a flow that needs the permission.
3. Allow a SAF folder-only experience where supported if broad audio
   library access is denied.
4. Request notification permission when the user starts or enables a
   feature for which the permission is relevant, subject to Android
   version behavior.
5. Request Bluetooth permission only when a named Bluetooth-dependent
   capability needs it.
6. Request ringtone write access only after the user chooses the
   ringtone action.
7. Request nearby-device/local-network permission only when the user
   enables LAN sync, listening party or a related feature.
8. Do not prompt for battery optimization exemption by default. Show
   an optional device-specific guidance page only when there is a
   demonstrated background-playback problem.
9. `INTERNET` is a manifest permission in the `full` flavor, not a
   runtime dialog. The Online features master switch remains OFF until
   the user explicitly enables it.
10. Never request `RECORD_AUDIO`, `MANAGE_EXTERNAL_STORAGE`, location,
    contacts or unrelated permissions for ordinary music playback.
11. Permission denial never deletes data or resets settings.
12. Permission revocation while the app is running must be detected
    before the next dependent operation.

Each permission explanation must state:

- What capability uses it.
- Why Pulse Player needs it.
- Whether it is optional.
- What continues to work if denied.
- How to grant it later.
- Whether the user can choose a narrower SAF grant instead.

## B7 --- Security, data integrity and privacy expansion

- Treat audio metadata, filenames, playlist files, lyric files,
  downloaded content, archive entries, remote API responses and AI
  output as untrusted input.
- Validate file type by content where possible, not extension alone.
- Restrict archive paths to safe relative entries and enforce
  decompression limits.
- Never execute imported content.
- Never open a database from a user-provided backup as the live
  application database.
- Keep secrets outside normal settings and database tables.
- Use encrypted credential storage and clear credentials on explicit
  disconnect.
- Do not include secrets in logs, exports, screenshots or diagnostics.
- Keep local data local unless the user explicitly enables a service
  that requires transmission.
- Every online integration must show provider identity, purpose and
  what user data is sent.
- The master Online features switch is a global kill switch. Disabling
  it cancels pending work where safe and prevents new network requests.
- Offline-only mode takes precedence over per-provider switches.
- Cache clearing must distinguish artwork, lyrics, remote media and
  temporary files so the user knows what is removed.
- A backup or restore must never overwrite the only copy of user data
  without a confirmed and validated recovery point.
- Do not move or delete original audio files as a side effect of
  scanning, deduplication, playlist editing, backup or restore.

## B8 --- Performance and stability additions

Prioritize perceptual responsiveness and reliable playback over
decorative complexity.

- The app shell and primary controls should appear before optional
  online services, deep statistics or waveform generation initialize.
- Scanning, tag reading/writing, lyrics parsing, waveform generation,
  backup, archive validation, artwork decoding and statistics
  aggregation run off the main thread.
- Keep progress updates throttled and meaningful.
- Playback progress must not recompose every library row or the whole
  Now Playing screen.
- Cancel obsolete artwork, lyric and search requests when the user
  changes track/query.
- Bound caches by memory and disk usage and expose cache clearing in
  Settings.
- Do not generate every waveform during first launch. Generate
  lazily or in a cancellable background queue.
- Keep only one expensive audio-analysis job active unless device
  capability and measured performance justify more.
- Avoid full-screen blur recalculation on every animation frame.
- Pause visualizers and background animation when the relevant surface
  is not visible.
- Keep widget updates event-driven and avoid polling.
- Release audio effects, observers, receivers and media resources when
  their owner is destroyed or the service is stopped.
- Recover playback and queue state after process death without
  creating a second media session.
- Test long sessions, repeated track skipping, external route changes,
  storage pressure, low-memory callbacks and orientation changes.

## B9 --- Accessibility and inclusive interaction details

- Support TalkBack semantics for every icon-only action, queue row,
  toggle, slider, selected tab and error/recovery control.
- Announce state changes that materially matter: playback state,
  favorite state, queue addition/removal, scan completion, import
  result and timer activation.
- Avoid announcing every playback progress tick.
- Maintain logical focus order when a sheet opens, closes or changes
  layout.
- Return focus to the element that opened a dialog or sheet when it
  closes.
- Provide keyboard/focus activation for supported external keyboards
  and D-pad navigation.
- Provide non-gesture alternatives for swipe actions, drag reorder and
  artwork gestures.
- Ensure touch targets do not overlap even when their visual icons are
  close together.
- Respect system font scale up to the required test range; allow
  content to scroll instead of clipping.
- Do not communicate status through sound or haptics alone.
- Captions, lyrics and audio descriptions are not a substitute for
  accessible controls.
- Reduced-motion mode must retain meaningful state changes and all
  functionality.

## B10 --- Expanded verification and release gates

### B10.1 Test layers

**Unit tests**

- Queue ordering, shuffle restoration, repeat modes and repeat-N.
- Track identity and duplicate matching.
- Library filters and per-source reconciliation.
- Artist/genre normalization and featured-artist extraction.
- Smart playlist rule evaluation.
- Statistics thresholds, speed handling, session boundaries and
  timezone behavior.
- Lyrics parsing and matching.
- Backup parsing, schema validation, matching and idempotency.
- Error mapping and recovery-action presence.
- Button preset registry, custom arrangement persistence, valid
  placement and adaptive overflow priorities.
- Contrast calculations and theme-token mappings.

**Integration tests**

- Room migrations from every supported schema.
- Scan interruption and source permission revocation.
- Playback service attach/detach/reconnect.
- Queue persistence and restore.
- Audio effect creation/release and route change.
- Classic export/import with copied audio files.
- Full backup validation and transactional restore.
- Local lyrics import and editor save.
- Online service gating and Offline-only request prevention.
- Remote source authentication and cache behavior.

**Compose UI tests**

- Home, Songs, Albums, Artists, Folders, Playlists and Search.
- Now Playing in all existing themes/layouts.
- Atmospheric Queue in portrait, landscape and tablet.
- Player layout picker and button arrangement picker.
- Rearrange Buttons drag and non-drag editing.
- Queue reorder/remove/clear.
- Smart playlist builder.
- Equalizer capability-disabled state.
- Settings search and category navigation.
- Permission denied and permanently denied states.
- Backup progress, partial result and error result screens.
- Large font, empty, loading and filtered-empty states.

**Device/manual tests**

- Primary Infinix 20:9 AMOLED phone.
- A compact 320dp-width device/emulator.
- A standard 360–430dp phone.
- A landscape phone with short height.
- A tablet with expanded width.
- A foldable in book and tabletop posture where available.
- Android versions covering the app's declared minimum and target.
- Bluetooth headset connect/disconnect and route changes.
- Notification permission allowed and denied.
- SD card/SAF folder access and revocation.
- Process death while scanning, playing and exporting.

### B10.2 Visual regression matrix

For every important screen, check:

- Light.
- Dark.
- AMOLED.
- System-selected theme.
- Artwork-driven accent.
- Wallpaper dynamic color where supported.
- Every retained skin.
- Every retained player theme.
- Atmospheric Queue.
- Compact and expanded button arrangements.
- Normal and large font scale.
- Reduced-motion enabled.

At minimum, screenshot or manually record Home, Songs, Album Detail, Artist Detail,
Search, Now Playing, Queue, Lyrics, Equalizer, Settings, Smart Playlist editor,
Backup & Transfer, Permission state and error result.

### B10.3 Required viewport and font checks

Check widths of approximately 320, 360, 393, 430, 600, 840 and 1280dp;
heights down to approximately 280dp in landscape; and font scales of
1.0, 1.3 and 2.0. These are test checkpoints, not permission to
hard-code layouts to those sizes.

Pass conditions:

- No overlapping text or controls.
- No clipped essential playback action.
- No button placed beneath a hinge, cutout or system gesture region.
- No hidden action without an alternative route.
- No unexpected reset of queue, selection, query or scroll position.
- No unhandled screen rotation or process recreation.
- No misleading truncated value where the full value is needed to
  make a decision.

### B10.4 Definition of Done for every implementation task

A task is complete only when:

1. Its acceptance criteria are met.
2. Existing affected behavior remains intact.
3. The relevant tests were run and results are real.
4. The changed-file manifest is accurate.
5. No temporary code is left untracked.
6. No dead control or fake result was introduced.
7. Any remaining limitation is documented.
8. The next task is not started until the current task's build/test
   result and status are reported.

## B11 --- Reference-app research and design translation protocol

Re-check the source repositories and documentation before each major
parity phase because upstream features, licenses and implementation
details can change.


**Reference snapshot for this document revision — 3 November 2026**

- The Namida documentation landing page currently identifies its
  documentation version as **v8.5.0**. Its public feature material
  includes advanced indexing/tagging, a waveform seekbar, visualizers,
  advanced audio controls, lyrics formats, smart playlists, persistent
  queues, server libraries, listening-party/local sync concepts, and
  system integrations. Its latest release material also highlights
  playback lock/long-press actions, improved search and tag editing,
  custom widget configuration, and imported history/playlists. Re-check
  the current upstream release before each relevant implementation
  phase rather than treating this snapshot as permanent.
- The current PixelPlayer repository describes itself as an Android
  player built with Kotlin, Jetpack Compose and Material 3 Expressive.
  Its current repository README presents dynamic color, queue
  management, local library organization, lyrics, equalizer presets,
  artwork caching, Cast and optional AI playlist functionality. Read
  the current repository LICENSE and third-party notices at the exact
  commit before any reuse; do not assume that all historical or current
  source files share the same license.
- This document does not guarantee that every listed reference
  capability can be shipped in Pulse Player. Legal, platform, build
  flavor, hardware, privacy and owner-preservation gates still apply.

Current public reference entry points:

- Namida repository: https://github.com/namidaco/namida
- Namida documentation: https://docs.namida.app/
- PixelPlayer repository: https://github.com/PixelPlayerHQ/PixelPlayer

Research rules:

- Namida is a Flutter-based music/video player and is a product,
  feature, behavior and design reference. Do not port Flutter source
  directly into Pulse Player.
- PixelPlayer identifies itself as Kotlin, Jetpack Compose and Material
  Design 3. It is the closer technology and modern Compose UI reference,
  but the existing Pulse Player architecture still takes precedence.
- Read current README, changelog, documentation, source structure and
  LICENSE at a pinned commit or release before relying on a feature or
  copying any code.
- Record the inspected date, branch/tag, commit identifier and license
  conclusion in the parity inventory.
- Do not assume a feature listed in an older user prompt is still
  present, unchanged or supported upstream.
- Recreate general interaction and visual ideas independently unless
  reuse is explicitly permitted by the exact license and all notices are
  preserved.
- Do not reuse branding, logos, screenshots, icons, illustrations,
  bundled artwork or fonts without appropriate rights.
- Treat YouTube extraction, downloads, sponsor-segment modification,
  account sign-in and other third-party platform integrations as
  separate legal and policy review items. Do not build them by default
  merely because the reference app supports them.
- Every reference feature discovered upstream must be added to the parity
  inventory, even if it was not listed in this document. Classify it as
  Implemented, Gated, Unsupported-with-reason or intentionally out of
  scope, with owner-facing explanation.

## B12 --- Google AI Studio operating instructions

Use this document as the source of truth, but do not attempt to implement
all of it in one response.

### B12.1 Before each task

- Read the relevant Spec Index row and only the required source files.
- Read the current implementation of every affected screen, model,
  repository, service, database entity and test.
- Report the task objective, current behavior, desired behavior,
  affected user flows, risks and acceptance criteria before changing
  code.
- Identify any conflict with the real repository or another specification.
- Do not ask the owner to repeat information already present in the
  specification.
- Do not make a destructive or privacy-policy decision by guessing.

### B12.2 During each task

- Change no more than the established file limit for one task unless
  the owner explicitly approves a larger task.
- Keep the existing package, architecture, dependency versions and
  single playback owner.
- Make complete, compiling changes; do not leave fake handlers,
  no-op callbacks, placeholder results or silent catches.
- Keep the UI, state, repository and persistence responsibilities
  consistent with the current project.
- Do not create new global state when the current source of truth can be
  reused.
- Do not use a new third-party dependency until its maintenance,
  Android compatibility, binary size, security and license have been
  checked.
- Do not weaken tests to make a feature appear complete.
- Do not change unrelated screens or formatting while fixing one
  subsystem.
- If a requested capability cannot be implemented honestly, show the
  reason and classify it rather than simulating it.

### B12.3 At the end of each task

Report:

- Task ID and objective.
- Changed files and why each changed.
- Existing features explicitly preserved.
- New behavior completed.
- Tests run, with exact commands and actual results.
- Build result, or `NOT RUN` with reason.
- Device/manual checks performed, or `NOT RUN`.
- Known limitations and recovery behavior.
- Remaining work and next task ID.
- Any required decision from the owner.

Never claim that an APK was installed, a screen was inspected, a test
passed, a commit was created or a push succeeded unless that action
actually occurred.

### B12.4 Context recovery

If the model loses context:

1. Re-read this master document's authority rules.
2. Read `docs/spec/SPEC_INDEX.md`.
3. Read the last completion report and `brain.md`.
4. Inspect Git status and the current diff.
5. Identify the last task that was genuinely completed.
6. Do not repeat completed migrations or overwrite uncommitted owner
   changes.
7. Continue from the next verified task, not from a guessed phase.

## B13 --- Final owner-facing product acceptance

The merged Pulse Player is acceptable only when all of the following
are true or are explicitly listed as gated/unsupported with a truthful
reason:

- The original app remains recognizable and every existing working
  feature, theme, skin, player layout, widget and backup format is
  preserved.
- Existing local playback and library management work without an
  account or network.
- The offline build contains no active network capability and no
  INTERNET permission, as defined by Annex A.
- The full build keeps online features disabled until explicit
  opt-in.
- Every feature has a clear screen, workflow, persistence rule,
  disabled state, error state and recovery behavior.
- Every screen has a coherent visual hierarchy and works in Light,
  Dark, AMOLED and supported dynamic-color modes.
- All player themes and player layouts use the same playback state,
  media session and authoritative queue.
- Atmospheric Queue is available as the premier/default Player Layout
  Mode according to Annex A, without replacing the Classic Player
  Theme.
- All 15 button arrangements work and the user can open Rearrange
  Buttons to reorder, hide, restore and save custom arrangements.
- No button becomes dead, unreachable or misleading after a layout
  change, capability change, orientation change or settings migration.
- Classic favourites backup v1 compatibility is preserved and the
  requested v2 audio-copy workflow is safe and recoverable.
- Full backup (reference format) remains separate and transactional.
- No scan, import, restore, tag edit, cache clear or file copy silently
  deletes or moves original audio.
- Permissions are requested just in time with clear rationale and a
  narrower SAF option where appropriate.
- Privacy, licensing, legal restrictions and online data disclosure
  are documented accurately.
- Performance, accessibility, responsiveness and error recovery pass
  the specified gates.
- Tests, build results, known limitations and feature traceability are
  reported honestly.

## B14 --- Final response format required from Google AI Studio

At the end of the entire project, provide a concise but complete
completion report with these headings:

1. **Repository and build identity** — package, build flavor, version,
   SDK range and branch.
2. **Preservation audit** — existing features/themes/layouts preserved,
   with any exceptions explained.
3. **Feature traceability** — every requested ID marked Implemented,
   Gated or Unsupported-with-reason.
4. **Design and responsiveness** — screens/themes/layouts tested,
   viewport sizes and font scales.
5. **Button arrangement** — 15 presets, custom editor, save/restore,
   accessibility and live-playback test result.
6. **Backup and data integrity** — Classic and Full backup results,
   migration status and recovery tests.
7. **Privacy and permissions** — offline/full flavor behavior,
   permissions and enabled providers.
8. **Automated verification** — exact test/build commands and actual
   result for each.
9. **Manual verification** — devices/emulators and scenarios actually
   checked.
10. **Known limitations** — hardware, Android, network, licensing or
    platform restrictions.
11. **Changed files and Git state** — accurate manifest, commit and
    push result, or `NOT RUN`.
12. **Remaining owner decisions** — list only decisions that genuinely
    block completion.

Do not use "complete", "production-ready", "fully supported" or
"100% parity" unless the traceability and verification evidence
supports the claim.
---

# Annex C --- Feature Completeness, Required Components and No-Dead-Control Implementation Contract

> **This annex is mandatory and applies to every feature in sections 6, 15, Annex A, Annex B, the original files 00–17, and every feature discovered in Namida or PixelPlayer.** It closes the gap between a feature being mentioned or represented by a visible control and that feature being genuinely implemented. If an item is not feasible, it must be explicitly marked `GATED` or `UNSUPPORTED` with a specific reason and a useful disabled-state explanation. Do not silently omit it, replace it with a decorative control, or claim it is complete.

## C0 --- The non-negotiable rule: a feature is a working vertical slice

A feature is **not implemented** merely because it has a button, settings row, screen title, mock preview, navigation destination, placeholder card, hard-coded sample, or callback that does nothing. A feature is implemented only when the full path below works with real app state and real user data:

1. **Discoverability:** a user can find it from the agreed screen and Settings location; the entry point is labelled clearly.
2. **Capability check:** the app checks Android version, device support, permissions, build flavor, online master switch, provider configuration, and required dependencies before enabling the action.
3. **Input:** the user can provide or select the necessary real input; validation explains invalid or missing input before work starts.
4. **Domain operation:** the action calls a real service/use case/repository and performs the intended operation. It must not only navigate, animate, update a local label, or show a success toast.
5. **State:** loading, progress, success, partial success, failure, cancellation and empty states are represented accurately.
6. **Persistence:** settings and user-created data survive process death and relaunch where persistence is promised. Use the existing Room/DataStore/MediaStore/SAF/Keystore architecture and migration rules.
7. **Result:** the user can inspect, play, edit, export, undo, retry, share or otherwise use the output as appropriate to the feature.
8. **Recovery:** errors have a meaningful next step; retry does not duplicate work or corrupt data.
9. **Accessibility:** semantics, labels, focus, keyboard/switch navigation, touch size, contrast and large-font behavior are supported.
10. **Verification:** unit, integration, migration, UI and manual tests prove the behavior. A feature without evidence is not reported as complete.

For every feature, implement and verify its **happy path**, **empty path**, **invalid-input path**, **permission-denied path**, **unsupported-device/build path**, **offline path** (where relevant), **cancellation path**, **process-recreation path**, and **failure/retry path**.

### C0.1 Required feature contract record

Before implementation, add one row to `docs/spec/FEATURE_COMPLETENESS_MATRIX.md` for each feature ID, including subfeatures where they have independent settings or user outcomes. Each row must contain:

`Feature ID | User outcome | Entry point | UI components | Domain/use-case | Data source | Persisted state | Required permissions/capabilities | Online/flavor gate | Loading/progress | Empty state | Success result | Error/recovery | Accessibility | Tests | Status | Evidence/known limits`.

A row cannot be `Implemented` if any required cell is blank. A parent feature cannot be `Implemented` while a promised child feature is missing. If a feature is split into milestones, mark each child independently and show the parent as `Partial` until all required children are done.

### C0.2 Real-data and fake-data prohibition

- Production screens must render from repositories, flows and persisted state. Never ship sample songs, fake statistics, fabricated lyrics, pretend network responses, mock AI results, placeholder server content or hard-coded queue rows.
- Preview fixtures are allowed only in preview/test-only source sets and must not be reachable from production dependency graphs.
- Do not create fake success messages when an operation did not complete. A network timeout is not a successful lyrics fetch; a copied filename is not proof that the bytes were copied; a queued task is not proof that playback began.
- If a service cannot be configured, show a setup action. If a capability is unsupported, show the reason and the closest supported alternative without pretending equivalence.

## C1 --- Required implementation components by responsibility

Use the current project architecture and names where they already exist. The names below describe required responsibilities, not permission to duplicate existing classes or create parallel playback/database owners.

### C1.1 Presentation layer

Every interactive screen or sheet must have:

- A screen/state holder (existing ViewModel or equivalent) exposing immutable UI state and one-way user actions.
- A reusable screen-level scaffold with correct insets, system bars, scroll behavior and adaptive layout.
- Reusable loading, empty, error, permission-required, offline, unsupported and partial-result components.
- Reusable confirmation, destructive-action, progress, result-summary and undo patterns.
- A semantic title and content descriptions for icon-only actions; tooltips where helpful.
- Stable item keys for lazy lists and grids; list item actions must operate on stable IDs, not display positions.
- Lifecycle-aware collection and cancellation of screen-scoped work. Long-running playback, downloads, indexing and sync must not be owned by a disposable composable.

### C1.2 Domain and data layer

Every feature that changes or retrieves data must use:

- A typed domain model and validated input model.
- A repository boundary for persistence or external source access.
- A use case/interactor for meaningful business operations, especially destructive, multi-step or reusable operations.
- Explicit result types distinguishing success, no result, partial success, recoverable error, permanent error, permission requirement and unsupported capability.
- A transaction or staged commit for multi-record changes; rollback or recovery journal for operations that span files and database state.
- Stable IDs and source identity; never rely on title/artist strings as unique identity.
- Background dispatch for file, database, network, waveform, image and audio-analysis work. Keep the main thread free of blocking I/O and heavy decoding.
- Cancellation support and idempotency for retryable jobs.

### C1.3 Platform and integration layer

- One playback owner and one MediaSession facade as specified in the conflict resolutions. UI layouts must never instantiate their own players.
- One queue authority (`QueueModel`/`PlayerController`); do not maintain competing UI and Media3 queue orders.
- One network gateway for all online services; enforce the master online switch and per-service switch centrally.
- One capability registry used by every setting, menu item and action. Never show an enabled action that later silently fails because a known capability is absent.
- Keystore-backed storage for secrets; never store provider passwords, API keys or refresh tokens in plaintext Room/DataStore/logs/backups.
- WorkManager or the project's established durable-work mechanism for deferrable downloads, scans, sync and imports. Respect foreground-service rules for user-visible ongoing work.
- SAF/MediaStore APIs for shared storage. Never request broad storage access merely to simplify implementation.

## C2 --- Shared interaction and component standards

### C2.1 Every control must have a defined contract

For each button, icon button, menu item, slider, chip, switch, tab, drag handle, row, gesture and notification action, document:

- Visible label and semantic/accessibility label.
- What exact action it performs.
- Which state it reads and which state it changes.
- Whether it is enabled, disabled, loading or hidden under each capability state.
- Whether the action is reversible; if destructive, its confirmation and undo behavior.
- What feedback appears after success, partial success or failure.
- Whether the state is persisted and when persistence is committed.
- What happens on repeated taps, double taps, rapid gestures and process recreation.

A control without a real action must not be rendered as an active control. If it is a future capability, show it as disabled with a reason only if the product specification requires discoverability; otherwise omit it from the active UI and list it in the feature matrix.

### C2.2 Shared UI building blocks

Create or reuse consistent components for:

- `PrimaryActionButton`, `SecondaryActionButton`, `TonalActionButton`, `IconActionButton`, `DestructiveActionButton`.
- `SectionHeader`, `TrackRow`, `AlbumGridItem`, `ArtistRow`, `FolderRow`, `PlaylistRow`, `QueueTrackRow`.
- `ArtworkContainer` with loading, missing, corrupt and fallback artwork states.
- `PlaybackProgress` with elapsed/remaining time, seek accessibility and drag preview.
- `MiniPlayer`, `TransportControls`, `QuickActionBar`, `QueueCountBadge`.
- `SearchField`, `FilterChipGroup`, `SortMenu`, `MultiSelectToolbar`, `SelectionCount`.
- `PermissionExplanation`, `CapabilityUnavailable`, `OfflineBanner`, `InlineError`, `RetryAction`.
- `OperationProgressSheet`, `ImportSummary`, `ExportSummary`, `ConflictResolutionDialog`, `UndoSnackbar`.
- `EmptyLibraryState`, `EmptySearchState`, `EmptyQueueState`, `EmptyPlaylistState`, `NoLyricsState`.

Components must accept theme tokens and real UI models. Do not create one-off hard-coded variants that break global theme fixes.

### C2.3 Interaction timing and duplicate-action protection

- Disable or debounce a submit action while the same non-idempotent operation is running; show progress instead of allowing duplicate export/import/tag-write/playlist-generation jobs.
- For reversible actions such as favourite toggles, update optimistically only when rollback is supported; restore prior state and show an error if persistence fails.
- For playback controls, keep the UI synchronized to the authoritative player state, not to the last button tapped.
- For drag-and-drop, expose a clear drag handle and preserve a usable non-drag alternative (Move up/down or accessible reorder dialog).
- Use haptics only when enabled by system/app settings; do not make haptics the only feedback.

## C3 --- Feature-by-feature complete behavior contracts

The following contracts supplement, not replace, the feature catalogue. If a source app contains an additional behavior, add it to this section and the traceability matrix rather than assuming the list is exhaustive.

### C3.1 Library scanning, indexing and folder management

**Required components:** scan coordinator, source-specific scanner, MediaStore/SAF adapters, metadata reader, identity resolver, duplicate detector, scan progress state, permission state machine, excluded-folder manager, scan history/result screen, retry/rescan action.

**Complete workflow:**
1. User opens Library settings or the empty-library CTA and chooses Scan library / Add folder.
2. Explain the scan scope and permissions before requesting them. Offer folder-picker/SAF selection when appropriate.
3. Scan on a background worker with cancellable progress: discovered, inspected, added, updated, skipped, duplicate candidates and errors.
4. Read supported metadata and artwork without blocking UI. Preserve source identity (`ms:`, `saf:`, copied and future source IDs).
5. Apply configured minimum size/duration, excluded folders, hidden-folder rules and duplicate policy. Filtering must not delete files or mark a still-existing file as missing.
6. Commit results transactionally in batches. Reconcile missing entries only within the source being scanned; do not mark files from an unscanned source unavailable.
7. Show a summary with counts and actionable error rows. A failed item can be retried independently; the whole scan can be safely rerun.
8. Persist scan settings and last successful scan metadata. On process death, the worker can resume or report a safe interrupted state.

**Must support:** manual rescan, optional scheduled rescan only if requested, scan cancellation, excluded folders, minimum duration/size, duplicate review, folder images (`cover.jpg`), `.info.txt`, artist/genre separators, feat./ft. parsing, album-artist grouping and source-specific availability.

**Failure cases:** revoked permission, removable SD card removed, SAF URI access revoked, corrupt metadata, unreadable file, malformed tags, database failure, low storage, interrupted scan. Never erase a valid existing library because one scan failed.

### C3.2 Track metadata, tags and file editing

**Required components:** metadata editor form, field validation, supported-format capability map, write-permission request, explicit confirmation, optional pre-edit copy, write adapter, post-write reread/rescan, per-file result list.

Fields include title, primary and additional artists, album, album artist, genre, year/date, track/disc number, composer where supported, BPM, language, rating, tags, mood, comment/note, lyrics and embedded artwork when the format/library supports them.

The editor must distinguish **library-only metadata** from **embedded file tags**. Explain which fields will change in the file and which are app-only. Before writing, show the exact files and fields affected, request the platform's write grant, validate the selected format, and ask for confirmation. Support single-file and explicitly selected batch edits, never silent batch writes. If a write fails halfway through a batch, show per-file results and never report the entire batch as successful. After a successful write, reread metadata and refresh all affected screens and search indexes.

### C3.3 Search, filters, sort, selection and bulk actions

**Required components:** indexed query repository, debounced search UI, filter model, sort descriptor, search suggestions/recent queries, multi-select state, bulk-action confirmation and result summary.

Search must cover the promised fields (title, artist, album, filename, lyrics and tags where indexed), normalize case/diacritics, support typo tolerance if specified, and return stable IDs. Search results must update when the index changes. Filters and sorting must compose rather than overwrite each other. Every sort must have a deterministic tie-breaker. Multi-select must survive scrolling and support select all in the current scope, clear selection, and bulk favourite/playlist/queue/share/delete-from-library actions as appropriate. Removing a track from the library must never delete the underlying audio file unless a separate explicit file-delete workflow is used.

### C3.4 Playback engine, queue, transport and background playback

**Required components:** single playback service/engine, MediaSession, notification, audio focus handler, noisy-route handler, player state flow, queue controller, persistent queue snapshot, playback error policy, headset/Bluetooth integration and foreground-service lifecycle.

**Complete behavior:** play, pause, previous, next, seek, play selected, play album/artist/genre/folder/search results, shuffle, repeat off/one/all/N times, insert next, append, insert after latest inserted, remove, clear with confirmation, save queue, restore queue, jump to current, reorder, and preserve the intended original order when shuffle is active. Queue actions must use stable track IDs. Queue persistence must restore paused unless the original spec explicitly defines another safe behavior. Handle audio focus loss/gain, calls, route changes, Bluetooth disconnect, headset unplug, notification dismissal, process death and missing source files. The media notification and lock-screen controls must reflect the same authoritative state as the app.

**Playback error behavior:** distinguish unsupported codec, decoder initialization failure, file missing, access revoked, corrupt/zero-byte file, network unavailable, DRM/stream restriction, output-device failure and transient engine error. Offer retry, skip, choose another source, request access or remove the unavailable queue entry as appropriate. Never loop infinitely on a permanently failing track.

### C3.5 Audio processing, equalizer and advanced sound

**Required components:** capability-aware audio effect controller, effect chain state, preset repository, per-track profile repository, parameter validation, audio-path diagnostics and reset-to-default behavior.

Implement each exposed effect as a functioning audio operation or clearly gate it. Include band controls with frequency/gain/Q where parametric EQ is promised; preset selection and save/rename/delete; import/export of supported AutoEQ profiles; bass enhancement; reverb; virtualizer/surround; mono; output selection; ReplayGain; per-track speed, pitch, gain and EQ; loudness controls; and an audio path viewer. Explain device/effect-session limitations. When the Android device or output does not support an effect, show it as unavailable rather than displaying a working-looking slider that has no effect.

Every parameter must have a safe range, unit, default, persisted key and audible/observable effect test where feasible. Switching tracks must load the correct per-track profile and restore global defaults for tracks without overrides. The audio path viewer must report actual negotiated/output information only; never invent bit depth, sample rate or bit-perfect status.

### C3.6 Gapless, crossfade, beat matching, fade and silence skipping

Treat gapless playback and crossfade as distinct modes. Gapless is the normal transition where supported. Crossfade is experimental and default OFF. Provide duration, enable switch, preview/test and clear incompatibility messaging. Beat matching must be described as an estimate and only enabled when BPM/analysis confidence is sufficient. The transition controller remains the sole owner of engine volume ramps. Test transitions with short tracks, long tracks, pause/resume, seek, queue changes, focus loss, repeat, shuffle and errors. Skip-silence must not cut speech/music unexpectedly; provide sensitivity/threshold settings only if implemented and a quick bypass.

### C3.7 Lyrics: import, fetch, display, sync and editing

**Required components:** lyrics repository, format parser, source priority resolver, online provider adapter, local import picker, sync timeline, word-sync renderer where supported, editor, save/export action and source attribution.

Support plain text, LRC, word-synced LRC and TTML only to the extent each parser is implemented. Display current line in sync with playback position; support manual scroll, auto-follow toggle, tap-a-line-to-seek, font size, alignment and translation/dual-language only if real translation data exists. Fetch online lyrics only with Online features and Lyrics provider enabled; match by title/artist/duration and let the user reject incorrect matches. Show source and fetched time. Editor must support timestamp adjustment, add/remove lines, offset all timestamps, preview sync and save locally; never overwrite an imported source without confirmation. No-result state offers import/search-again/manual-create, not fabricated lyrics.

### C3.8 Artwork and artist imagery

**Required components:** embedded-art extractor, local artwork cache, optional online artwork provider, image loader with memory/disk cache, fallback renderer, attribution/clear-cache controls.

Prioritize embedded artwork, then user-selected local artwork, then enabled online provider, then deterministic fallback. Cache by stable track/artist identity and artwork version. Handle huge images, malformed image data, no connection, provider rate limits and stale URLs. Provide a way to clear online cache without deleting embedded tags. Never hotlink arbitrary untrusted image paths into production UI without validation.

### C3.9 Playlists, smart playlists, mixes and recommendations

**Required components:** playlist repository, editor, reorder controller, rule builder, rule evaluator, recommendation engine/provider abstraction, generated-playlist review screen and playlist artwork editor.

Manual playlists must support create, rename, add/remove tracks, reorder, custom artwork, duplicate playlist, delete with confirmation, import/export supported formats and preserve ordering. Smart playlists must support rule groups, AND/OR logic, field/operator/value validation, preview count, save, edit and deterministic refresh. A smart playlist must re-evaluate against real library data and update when relevant metadata changes. Daily Mix/AI recommendations must show why a track was suggested when possible, allow refresh and feedback (like/dislike/exclude), avoid duplicates, respect explicit filters and never claim AI personalization if it is only random sorting. If no AI provider is enabled, a local rules-based mode must be labelled accurately.

### C3.10 Favourites, ratings, moods, tags, Most Played and Smart Tracks

**Required components:** stable favourite/rating/tag/mood repositories, track detail editor, query/ranking logic, time-range selector and empty-state guidance.

Favourites must toggle consistently from all screens and persist immediately. Ratings must validate 0–5 and be distinct from Android media ratings unless synchronized intentionally. Tags and moods must be user-editable and searchable. Most Played must define the qualifying play threshold and time window, handle imported history separately from live events, and sort deterministically. Smart Tracks / Lost Memories must have documented eligibility rules, a preview/result list and user controls to exclude tracks; do not invent nostalgic narratives or fabricate listening history.

### C3.11 Listening history, statistics and Your Year

**Required components:** session tracker, history writer for imports/manual edits, stats query layer, time-range controls, chart components, export and privacy/delete controls.

Record a play according to the configured minimum seconds/percentage threshold and avoid counting rapid skips as completed listens. Keep live events separate from imported/manual records using origin markers. Allow history edit/import only through the designated writer. Statistics must derive from stored events and state the selected time range/timezone. Charts need accessible text summaries and empty states. Your Year must only display metrics that exist in the database; if history coverage is partial, say so. Import must deduplicate and provide a preview, mapping report and rollback/undo where possible.

### C3.12 Bookmarks, chapters, repeat-N and sleep timer

Bookmarks must store stable track identity, millisecond position, label and timestamp; selecting one seeks to the saved position. Chapters must be parsed/imported from real metadata or created by the user; chapter selection seeks accurately. Repeat-N must decrement only after a completed track iteration and reset predictably when track/queue changes. Sleep timer must support the specified time/track-count conditions, show remaining time, persist only if intended, allow cancel/extend and fade safely without fighting pause or audio focus logic. Pause scenarios (calls, notification conditions, volume zero) must be independently configurable and clearly described.

### C3.13 Downloads, offline cache and remote sources

**Required components:** source connector, credential setup, secure secret store, connectivity monitor, download/cache scheduler, storage quota manager, cache priority model, download list, retry/cancel/delete-cache controls and source health status.

Each source (Subsonic, Jellyfin, WebDAV, SMB, YouTube or other approved source) requires a complete add/edit/test/remove workflow, server URL validation, authentication, secure credential storage, pagination, browsing, playback, error mapping and disconnect handling. Downloads must expose queued/downloading/paused/complete/failed states, byte progress, remaining/total size where known, destination/cache policy, retry and cancel. Offline playback must resolve to a completed local cache entry and must not silently attempt network streaming. Cache eviction must respect pinned/offline-priority items and never delete original local music. If the source provider has no supported API or its terms prohibit the intended operation, gate it and document the reason.

### C3.14 Online video, YouTube, SponsorBlock, dislike signals and subtitles

Treat these as legally and technically gated integrations, not assumed universal capabilities. Before implementing, verify current provider API access, terms, app-store policy, authentication requirements and source repository license. Every visible capability needs a working provider adapter, clear source attribution, consent and a data-use explanation. Video controls must support actual playback surface gestures for seek/brightness/volume/fullscreen/speed and have accessible alternatives. Sponsor segments must be fetched from a legitimate supported source, shown on a timeline and skippable only when enabled; dislike signals must not be presented as official if they come from a third-party estimate. Subtitles require actual subtitle tracks/files, language selection, timing, styling and no-subtitle state. Never scrape, bypass DRM, evade access controls or imply an unsupported download is available.

### C3.15 AI provider setup and AI features

**Required components:** provider registry, secure API-key entry, connectivity test, request preview/consent, prompt/data minimization, timeout/cancellation, rate-limit handling, response schema validation, result review and provider disable/delete controls.

Support only providers whose current APIs, SDKs, terms and authentication are verified. Users choose provider/model and can inspect what metadata or prompt will be sent before the first request. Do not send audio files, full library data, personal identifiers or listening history unless a clearly scoped feature requires it and the user explicitly consents. Parse structured results defensively, validate every suggested track against the local library, remove duplicates and show unmatched suggestions. If the provider fails, offer retry or local rules-based alternative and clearly label it. Never fabricate generated results or claim that a remote model ran when it did not.

### C3.16 Listening Party and local network sync

**Required components:** session host/join flow, pairing code/QR, authenticated transport, protocol/version negotiation, queue synchronization, host/guest role state, connection indicator, latency handling, leave/end controls and conflict policy.

Explain whether audio itself is streamed or only playback position/queue metadata is synchronized. Keep synchronized track identity mapped to each participant's own library; show missing-track status and allow local matching. Define host authority, guest permissions, seek/skip permissions, drift correction and reconnect behavior. LAN sync must preview data categories before transfer, authenticate peers, encrypt transport where feasible, resolve conflicts deterministically and support cancel/rollback. Never expose a local server to the public internet by default.

### C3.17 Chromecast, Android Auto and widgets

Cast must discover supported devices, connect/disconnect, show route state, transfer playback only when supported and handle session loss. Android Auto must expose only approved driving-safe media browsing and transport controls, with stable media IDs and proper media-session integration. Glance widgets must update from real playback/library state, provide correct 2x2/4x1/4x2 layouts, handle no active track and stale process state, and route each tap to a real deep link/action. Test lock screen, background, reboot and widget resize/update behavior. Unsupported Google Play Services or device features must be explained.

### C3.18 Theme, skin, player theme, layout and visualizer

Keep theme mode/color source, skin, player theme, player layout and button preset as separate persisted axes. Each picker must preview the actual appearance using current app state and apply immediately without restarting playback. Verify all combinations that are supported; if combinations are constrained, document the rule and prevent invalid combinations. Wallpaper import must copy to app-private storage and handle permission loss, decode failure and oversized images. Visualizer/particles must consume permitted audio-analysis data without requesting microphone access; throttle updates and disable animation in reduced-motion/battery-saver modes. Every theme must cover dialogs, sheets, menus, text fields, disabled/pressed/focused states, system bars, widgets where applicable and error surfaces—not just the home/player background.

### C3.19 Custom button arrangements and recommendations

In addition to Annex B4, recommendations must be explainable and reversible. The app may recommend a preset based on screen size, user-selected use case (minimal, accessibility, one-handed, queue-focused) or observed usage only if that usage analysis is transparent and local. Do not silently rearrange controls. Show a before/after preview and require Apply. A recommendation must never remove mandatory transport controls or reduce touch targets below 48dp. Include reset-to-recommended and reset-to-default separately. Save named custom presets, duplicate, rename, delete with confirmation and export/import if promised. Validate imported layouts against current app capabilities and migrate unknown button IDs safely.

### C3.20 Backup, restore, import/export and migration

**Required components:** backup manifest/version, content inventory, SAF destination picker, preflight size estimate, progress, checksum/validation, conflict resolver, staged restore, rollback journal, import summary and recovery log.

Before export, show included categories, estimated size, destination and whether audio bytes are included. Before restore, show source, backup version, categories, conflict counts and destructive implications. Never overwrite current data without explicit selection and confirmation. Validate manifest, schema, file lengths and checksums before committing. Restore in a staged transaction; if interrupted, keep the previous database usable and offer resume/rollback. Classic favourites v1 must remain readable; v2 may add copied audio without changing v1 JSON compatibility. Full backup remains separate. Report per-item results and preserve unknown future fields where safe.

### C3.21 Permissions, privacy and online controls

Each permission request must explain the feature that needs it, request only at the point of need (except the explicitly defined onboarding education sequence), and offer a skip/continue-limited path. Denial must not crash or trap the user. Permanent denial should link to app settings with a clear explanation. Network access is not a runtime permission; the offline flavor must exclude network permission/code as specified. Online master switch OFF must prevent all network requests, including background refresh, image/lyrics fetch, AI, sync and telemetry (telemetry remains forbidden). Per-service switches must be enforced centrally. Show connected accounts, stored credentials and a clear disconnect/delete-credential action.

## C4 --- State model and lifecycle requirements

Every long-running or externally dependent operation must have a finite, explicit state model. Minimum common states:

`Idle → Validating → AwaitingPermission/Configuration → Queued → Running → Committing → Succeeded | PartiallySucceeded | Failed | Cancelled`.

Not every operation uses every state, but no operation may jump to `Succeeded` before durable commit. Progress may be indeterminate when exact progress is unknowable; do not invent percentages. Persist durable job identifiers and enough checkpoints to recover or safely restart. A retry must be idempotent or guarded by a unique operation key. A cancellation must stop future work and leave data in a valid state; if an atomic rollback is impossible, clearly report which items completed and which did not.

For screen state, model at least:

- Initial/loading state (including restoration from persisted state).
- Content state.
- Empty state with a next action.
- Error state with retry and/or recovery.
- Permission-required state.
- Unsupported or unavailable state.
- Offline/remote-unavailable state where relevant.
- Partial-result state where some items succeeded.

## C5 --- Data integrity, migrations and destructive-operation rules

- Database schema changes require version increments, exported schema snapshots, explicit migrations, migration tests from every supported prior version and a backup/rollback plan.
- Never use destructive database fallback in production.
- Never infer file deletion from a failed scan or inaccessible folder.
- File copy, tag write, restore, cache eviction and bulk changes require preflight validation and clear scope.
- Destructive actions require a confirmation that names the affected object/count and explains reversibility. Use undo for reversible actions; require stronger confirmation for permanent deletion.
- File operations must verify destination availability, free space estimate, read/write permission and actual byte count. Use temporary files plus atomic rename where supported. Do not report success until the destination can be reopened and validated.
- Avoid duplicate import by stable IDs, content hashes or documented matching rules. Fuzzy matching must present ambiguous matches for user review instead of silently choosing a potentially wrong song.
- Preserve forward compatibility: unknown settings and backup fields should not crash parsing; apply safe defaults and report unsupported entries.

## C6 --- Error message quality contract

Every user-facing error must answer, in plain language:

1. **What happened?** State the operation and affected item in human terms.
2. **Why might it have happened?** Give a concise cause only when known; do not speculate as fact.
3. **What can I do now?** Provide one primary recovery action and optional secondary actions.
4. **What was preserved?** State whether existing data/files/settings remain safe, if known.
5. **Can I retry?** Explain whether retry is safe and whether it will duplicate anything.

Avoid raw stack traces, exception class names, HTTP status codes or filesystem paths in normal UI. Keep technical diagnostics in local logs with redaction. Never log API keys, passwords, tokens, full private file contents or unnecessary personal metadata. Provide a copy-diagnostics action only if it strips secrets and requires user initiation.

## C7 --- Feature recommendation and discovery requirements

- Settings must contain every feature that the master specification says belongs in Settings, grouped under the information architecture in section 5.6. Searchable settings are strongly preferred as the catalogue grows.
- Use descriptive names, short explanations, current values and capability status. Do not put advanced features in an undiscoverable overflow menu.
- Each feature's settings row opens a real detail screen or performs a real action. A toggle must change the underlying service behavior, not merely its own displayed state.
- Provide contextual entry points where useful (e.g. equalizer from Now Playing, lyrics from player tabs, button arrangement from player controls) while retaining the canonical Settings entry.
- For complex features, include a first-use explanation, a preview/example using non-production preview data only, and a link to the relevant setting.
- Do not overload the main screen with every advanced feature. Keep the primary music-listening flow simple and reveal advanced controls contextually.

## C8 --- Required tests for every feature ID

Each feature ID must have a test plan containing the relevant tests below. Not every test type applies to every feature, but the reason for any omission must be stated.

| Test layer | Minimum expectation |
|---|---|
| Domain/unit | Validation, rules, sorting, matching, state transitions, idempotency, error mapping and edge cases |
| Repository/integration | Real persistence, source adapter, transaction, cache behavior and failure mapping |
| Migration | Upgrade from supported schema versions; existing data remains intact |
| UI | Entry point, enabled/disabled state, loading, empty, success, error and accessibility semantics |
| Lifecycle | Rotation, process death/recreation, background/foreground, cancellation and restoration |
| Platform | Permission grant/deny/permanent deny, API-level differences, storage/SAF and audio focus where relevant |
| Network | Offline, timeout, rate limit, invalid credentials, malformed response and master-switch enforcement where relevant |
| Data integrity | No accidental file deletion/overwrite, no duplicate imports, partial failure reporting and rollback |
| Performance | Large library/list, memory, startup, scrolling and background-work impact where relevant |
| Manual acceptance | A written reproducible scenario and actual device/emulator result |

A test that merely checks a button exists or that a screen opens is not sufficient evidence for feature completion.

## C9 --- Google AI Studio execution protocol: build complete features, not visual shells

For every implementation prompt/phase:

1. Read the relevant base spec, merge spec, feature matrix rows and existing source files before editing.
2. Inspect the actual repository. Do not assume that a described feature exists or is missing; audit it and mark `PRESENT`, `PARTIAL` or `MISSING`.
3. Select a bounded vertical slice. Follow the repository's task-size/file-count constraints. Do not generate a giant speculative rewrite.
4. State the exact user outcome and acceptance tests before implementation.
5. Trace the path from UI event → ViewModel/state holder → use case/controller → repository/platform service → persisted/observable result → UI feedback.
6. Implement all layers required for the selected feature in the same phase or explicitly split the feature and mark it partial. Do not create a UI control first and leave the backend for an unspecified future task.
7. Wire every visible control to a real action. Search for empty callbacks, placeholder lambdas, TODOs, hard-coded sample state, fake success and unreachable settings.
8. Add tests before declaring the slice complete. Run only commands that exist in the repository; report commands and actual outputs accurately.
9. Update the feature matrix, Spec Index, changed-file manifest, decisions and docs in the same task when required.
10. End with: what works, how to use it, what persisted, tests run and results, known limits, remaining child features and exact next phase.

### C9.1 Mandatory anti-placeholder audit

Before marking any task complete, search the changed files and relevant call graph for:

- `TODO`, `FIXME`, `NotImplementedError`, placeholder return values and empty function bodies.
- `onClick = {}`, no-op callbacks, fake navigation destinations and controls with no state effect.
- Hard-coded sample tracks, fake URLs, demo responses and generated success messages.
- Settings switches that persist a boolean but do not alter runtime behavior.
- Buttons that only show a toast or dialog when the specified feature requires a real operation.
- UI-only queue/order/favourite state that is not connected to the authoritative repository/controller.
- Catch-all exception handlers that swallow errors or return success/default data without reporting failure.
- Background tasks that are not uniquely identified, cancellable or safe to retry.

A search result is a review prompt, not automatic proof of a defect; inspect each occurrence. No known no-op or placeholder may remain in a feature reported as implemented.

### C9.2 Required per-phase implementation report

For every phase, report:

- Feature IDs addressed and previous status.
- Existing implementation reused.
- UI components and exact entry points added/changed.
- Domain/use-case/repository/platform components added/changed.
- Real data source and persistence behavior.
- Permissions/capabilities/flavor gates.
- Loading, empty, success, partial, error and recovery states implemented.
- Accessibility behavior.
- Tests added, tests run and exact results.
- Remaining limitations and child features still marked partial/gated.
- Changed-file manifest and confirmation that unrelated features were preserved.

## C10 --- Final release gate: no feature is accepted on appearance alone

The app may be called complete only when:

- Every feature ID from all source specifications and reference inventories has a completed matrix row.
- Every promised feature is `Implemented`, or is explicitly `Gated` / `Unsupported-with-reason` and surfaced honestly in the UI/report.
- Every visible control has a verified real behavior or a clear disabled reason.
- All screens have real-data loading, empty, error and success states as applicable.
- All long-running tasks are cancellable, recoverable and safe to retry.
- All persisted state survives the promised lifecycle and migrations.
- No data/file operation silently destroys or overwrites user content.
- No secrets or private data leak into logs, backups or unauthorized network requests.
- All required automated and manual tests have actual results; unrun tests are marked `NOT RUN`.
- The final feature traceability audit contains evidence, not only claims.

**Instruction to Google AI Studio:** When uncertain whether a requirement has enough detail, do not implement a superficial approximation. Inspect the source projects and this specification, add the missing behavior to `FEATURE_COMPLETENESS_MATRIX.md`, and implement the full vertical slice. If the source or platform does not support it, state that limitation and gate the feature rather than shipping a dead button.


# Annex D --- Exhaustive Feature-by-Feature Product, UI, Logic and Component Specification

> **Mandatory companion to Annexes A, B and C.** Annex D decomposes every feature already named in the base specification and merged feature catalogue into a user-visible workflow and a complete implementation contract. It is not permission to remove a feature because it is difficult. Every row must be audited against the real repository, implemented and tested, or explicitly classified as `GATED` / `UNSUPPORTED` with the exact reason and user-facing explanation. `PARTIAL` is not a final release status.

## D0 --- How to use this annex

1. Read the preservation and authority rules first. Inspect the current app and record its real behavior before changing any screen, data model, player service or setting.
2. For each ID in this annex, record whether it already exists, is incomplete, is new, or is platform-dependent. Do not rebuild a working feature just to match the proposed component names.
3. Reuse the existing implementation where it is correct. Add only missing layers. There must still be one source of truth for every shared concern: one playback owner, one queue authority, one library repository, one settings source, one theme registry and one capability registry.
4. Build complete vertical slices. A new screen is not complete until it reads real data, carries out its domain action, persists promised state, presents every required state, and passes the listed tests.
5. Use actual repository names and architecture. Component names in this annex describe responsibilities and expected visible parts; they do not require duplicate ViewModels, services, repositories or player engines.
6. Preserve existing IDs and serialized values for player themes, skins, settings, layouts, buttons, playlists and backup formats. Add migrations and aliases when a rename is unavoidable.
7. The final feature matrix must include every ID below and every additional child feature found during the repository/reference audit. A parent is `Implemented` only when all mandatory children are implemented.

### D0.1 Standard implementation record for every feature

Each feature must have: **(a)** one or more entry points; **(b)** its actual screen, sheet, dialog, row, menu or widget components; **(c)** a state holder and immutable UI state; **(d)** domain action/use case; **(e)** repository or platform adapter; **(f)** real input and validation; **(g)** persisted state and migration; **(h)** capability and permission checks; **(i)** loading/progress, empty, success, partial, failure, unavailable and cancellation presentation as applicable; **(j)** undo/rollback or explicit irreversibility; **(k)** accessibility behavior; **(l)** tests and evidence. The implementation must identify which of these does not apply and why; it may not leave cells silently blank.

### D0.2 Common operation-state contract

Use accurate named states: `Idle`, `Validating`, `AwaitingPermission`, `AwaitingSetup`, `Queued`, `Running`, `Committing`, `Succeeded`, `PartiallySucceeded`, `Failed`, `Cancelled`, `Unsupported`. Progress is determinate only when measured. A job cannot report success before its result is durably committed and verifiable. Retrying must not duplicate data or repeat an already committed destructive action. Long-running work must survive configuration changes and recover after process recreation where promised.

## D1 --- Existing Pulse Player shell and everyday experience

### D1.1 App launch, first run and returning-user restoration (`BASE-APP-01`)

**Required visible parts:** launch/splash surface, first-run education, library-empty state, permission rationale, folder picker entry, scan progress view, restore/recovery messaging, and the normal home shell.

**Required behavior:** On first launch, explain that Pulse Player is local-first and does not require an account. Show why audio/library access is requested before requesting it. Let the user choose a permitted music folder or scan an eligible media collection. Do not start network activity. On returning launch, restore theme, skin, player theme, layout, button preset, navigation order, filters, playback queue and saved position from their authoritative stores. Restore playback paused; never auto-resume unless the user's explicit existing preference allows it. Resume the last screen only if its data still exists and its navigation state is safe to restore.

**Failure and edge cases:** first-run permission denied; permanent denial; no files; empty selected folder; SD card removed; prior scan incomplete; migration needed; database recovery required; app upgraded from old settings; corrupt preference value; unsupported OS. Provide a path to continue in limited mode and to revisit setup from Settings.

**Verification:** fresh install; upgrade with populated library; denied and revoked permission; SD card removed/reinserted; process kill during scan; corrupted optional setting; app restart with a pending queue.

### D1.2 Application shell, navigation and adaptive structure (`BASE-APP-02`)

Preserve the current top tabs, drawer, navigation structure, resume banner and all existing destinations unless the repository audit documents a necessary migration. Add the expanded library destinations without crowding the primary navigation. Use a bottom navigation bar or compact tabs on narrow phones, NavigationRail on medium layouts and a rail-plus-content / docked player arrangement on expanded layouts. Keep the chosen navigation mode consistent with the user's explicit preference except where a layout is physically impossible.

**Components:** app scaffold, top app bar, navigation tabs/rail/drawer, destination content host, mini-player dock, global snackbar host, modal bottom-sheet host, adaptive insets handler and navigation state holder.

**Behavior:** preserve scroll state per destination; deep links return to the correct parent; back closes a sheet/dialog before leaving a screen; switching tabs does not restart playback or discard selection unexpectedly; window resizing and fold/unfold do not reset state. Every destination has a stable route and semantic title.

**Tests:** navigation restoration, deep links, back-stack behavior, 320dp/360dp/600dp/840dp/large width, landscape, split screen, fold/unfold, large font and RTL.

### D1.3 For You / Home (`BASE-HOME-01`)

The Home screen must be composed from real library and playback data. Its sections are independently hidden when no meaningful data exists; do not fill empty sections with demo content.

- **Resume / Continue banner:** show only when there is a valid last-played track or restorable listening context. Show artwork, title, artist and progress. Tap resumes from the stored position; a missing/unavailable item opens a clear recovery choice and must not erase its history.
- **Quick Shuffle:** starts a randomized queue from the currently eligible library scope, respecting excluded folders, unavailable tracks, explicit filters and current play mode. Show the number of eligible tracks when useful. Empty eligible set gives a helpful explanation.
- **Play:** starts the selected/default library scope using the app's documented ordering; repeated taps must not create duplicate queues.
- **Listening time tile:** uses actual qualifying listening events for the local day and clearly defines the counting rule. A missing record is zero only when data is genuinely known; unavailable history is not silently shown as zero.
- **Favourites shortcut:** opens the real favourites collection and count; count changes update immediately across the app.
- **Sleep timer shortcut:** opens the existing timer controls and displays remaining time/active end condition while running.
- **Equalizer shortcut:** opens the current route's supported audio controls, not an invented generic EQ screen.
- **Recently added:** uses date-added metadata and a defined limit/sort. Track actions remain available.
- **Top picks / most played:** uses verified listening history and minimum-data threshold; do not invent ranking before enough data exists.
- **System playlists / Smart Mix cards:** each card must navigate to a real query/result and include a meaningful empty state.
- **Empty-library onboarding:** show Scan library and Choose folders actions, the source/permission explanation and supported audio-format hints.

**Tests:** all cards with zero, one and many records; deleted file; filters changed; day boundary/time zone; favourites updated from another screen; sleep timer active; no listening history.

### D1.4 Reusable track/album/artist/folder/playlist items (`BASE-UI-ROWS`)

A track row must support artwork/fallback, title, artist, optional secondary metadata, duration where appropriate, playing/paused indicator, selected/favourite state where requested and a contextual menu. It must operate on a stable track ID, never its current list index. Row tap behavior depends on the active Track Play Mode and source context. Long-press enters multi-select or opens the documented contextual action flow consistently. Album and artist cards expose their actual metadata/counts and route to detail screens. Folder rows display source availability and cannot imply that a revoked path is accessible. Playlist rows show name, track count and optional artwork/description.

All row types need loading/placeholder, missing-artwork, unavailable-source, selected, disabled, pressed and error states. Lazy list keys must remain stable when sorting or filtering. Every contextual menu action must be implemented and permission-gated.

### D1.5 Library page family (`BASE-LIB-01` through `BASE-LIB-15`)

Implement each page as a real query over the same library repository. Do not create independent copies of track records for each tab.

- **Songs/Tracks:** all eligible tracks; sort, filter, search, play mode, selection and contextual actions.
- **Albums:** album grid/list, album artwork, album artist, year and track count; detail screen with disc/track ordering, incomplete-album handling and album actions.
- **Artists:** artist grouping, featured-artist rules, artist image/fallback, album list and tracks; distinguish track artist from album artist.
- **Folders:** configured scan roots and descendants; show included/excluded status, unavailable source and folder-specific actions.
- **Playlists:** static and smart collections, sorted independently, with create/import/export actions.
- **Genres:** many-to-many track relationship; unknown genre is not presented as a fabricated genre.
- **Queues:** saved queue sessions with name, save date, track count, restore, rename and delete.
- **Most Played:** range filters (today/week/month/year/all/custom) and documented tie-breakers.
- **Moods:** user-defined or tag-derived groupings; empty until data/rules exist.
- **Tags:** user-defined tags with create, rename, assign, remove and filter behavior.
- **Ratings:** 1–5 star or the existing rating model; define unrated separately from zero.
- **Languages:** extracted/entered language metadata only; unknown language stays unknown.
- **Statistics:** opens the real analytics pages described in D6.
- **Recently Played:** sort by qualifying last-played event; deduplicate display by track while retaining event history.
- **Recently Added:** sort by first indexed/added time, with re-scan behavior defined.
- **Missing Songs:** show unavailable references without deleting favourites/playlists/history. Provide locate/relink/remove-reference actions with confirmation.
- **Optional Videos:** do not show this destination unless local video playback and its full media controls are actually implemented.

For every page, provide accurate count, search/filter as appropriate, sort menu, empty state, permission-limited state, error/retry, selection, contextual actions and return-to-source navigation. Page visibility and ordering are configurable and persist. Pages that have no user data remain empty, not populated with samples.

### D1.6 Selection, contextual actions and bulk operation (`BASE-LIB-SELECT`)

Selection mode must expose selected count, select all within the current result scope, clear selection, cancel selection and a contextual action bar. Bulk actions include only operations safe for every selected item. Before a bulk operation, show target count, unsupported items and the planned outcome. Partial success reports each category and lets the user inspect failures. Operations must be stable-ID based and cancellable when possible.

Supported actions, when applicable: Play, Play next, Add to queue, Add to playlist, Favourite/Unfavourite, Tag, Rate, Edit metadata, Share, Export, Remove from playlist, Hide from display, Locate file, and Delete only when the product explicitly supports deletion and confirms it. Never conflate removing a library reference with deleting the underlying audio file.

### D1.7 Track details and file information (`BASE-TRACK-DETAILS`)

Open from row overflow and Now Playing. Show actual title, artists, album/album artist, duration, file size, container/codec, bitrate/sample rate/channel count where available, track/disc number, year, genre, composer, comment and custom tags; file path/source; date added/modified; play count/last played; favourite/rating/tags/moods; file availability; artwork source/cache status; lyrics source/availability; audio-route capability and supported actions. Each unknown value is labelled Unknown/Not available, not converted to zero or a guessed value.

Actions must include only supported functions: edit tags, replace artwork, open containing folder/source, share file, copy path/metadata, add to playlist, play next, favourite, ringtone, locate missing source. File details must be read-only unless an explicit edit flow is entered. Protect sensitive file paths when sharing diagnostics.

## D2 --- Library discovery, indexing, metadata and search

### D2.1 Scanner and indexer (`NAM-L1`, `NAM-L4`, `NAM-L5`, `PIX-L4`)

**Setup UI:** source list, Add folder/source, include/exclude tree, allow-list mode, hidden-file option, media categories, minimum/maximum duration, minimum file size, excluded categories, parser settings, refresh policy and source-level last-scan status.

**Scan UI:** source name/path (with privacy-safe display), phase, current item where appropriate, determinate or indeterminate progress, discovered/added/updated/skipped/duplicate/unreadable counts, Pause/Cancel, and final summary. Never fabricate percent complete. A cancelled/failed scan must preserve the last known-good index and must not mark every unvisited file missing.

**Domain behavior:** discover media using permitted MediaStore and SAF access; persist source grants; handle internal storage and removable SD; perform incremental scanning; compare stable document/media identity and file metadata; detect changed files; parse embedded metadata; create or update records transactionally; keep user-created metadata/history/favourites/playlists if a file temporarily disappears; support scan all, refresh, rescan one source and rescan one folder. Distinguish inaccessible source from confirmed deleted file.

**Filters:** minimum and optional maximum duration, minimum file size, audio/video type, hidden files/folders, notification/alarm/ringtone/voice-note exclusions and user-defined rules. These are library visibility/indexing rules, not silent file deletion. Changing a filter must not erase user data. Show whether filtered items remain indexed but hidden.

**Source management:** rename display label, grant/revoke access, include/exclude, rescan, inspect error, remove source reference. Removing a source cannot delete audio. If an SD card becomes unavailable, retain identity and mark source unavailable; recover when reattached.

**Tests:** interrupted scans; same file discovered under two roots; renamed file; file changed; inaccessible folder; SD removal; revoked SAF permission; zero-byte/corrupt media; large library; filters; duplicate names; repeated scan idempotence; app killed during index commit.

### D2.2 Duplicate detection and review (`NAM-L3`)

Use layered matching: normalized title + normalized artist + duration tolerance (the catalogue's default tolerance is two seconds) and optional size/hash/metadata evidence. A duplicate candidate is not automatically the same file. Display both paths, source, size, duration, tags, availability and match confidence/reason. Actions: keep both, hide one from library view, mark as preferred, relink metadata/reference or remove a library reference. Hiding is display-only and must not set the file unavailable. Never delete audio or merge play history without explicit selection. Provide undo where possible and log the user's resolution.

### D2.3 Artist/genre parsing and album grouping (`NAM-L2`, `NAM-L8`, `PIX-L3`)

Provide a settings editor for enabled delimiters, separator precedence, exceptions (e.g. names that contain commas or ampersands), `feat.`/`ft.` extraction and album-artist grouping preference. Preview sample strings before applying. Parse into normalized relationships without altering original embedded tags. Store original raw tag separately from parsed relationship values. Re-index or rebuild relationships with visible progress and rollback if parsing fails. One track may relate to multiple artists/genres; featured artist must not be appended twice. Album grouping can switch between album artist and track artist without modifying tags.

### D2.4 Sorting, filtering and A–Z navigation (`NAM-L6`)

Sort fields: title, artist, album, album artist, genre, year, duration, file size, bitrate, sample rate where available, track/disc number, date added, date modified, play count, last played, rating, BPM when measured, filename and folder. Include ascending/descending, stable secondary order, null/unknown placement and locale-aware text ordering. Persist sort independently per page and playlist where relevant. Filter chips must show active filters and provide Clear all. A–Z scroller must map to current filtered data and provide an accessible alphabet index. Sorting must not mutate saved playlist order unless the user explicitly changes playlist order.

### D2.5 Folder covers and sidecar metadata (`NAM-L7`)

Resolve folder image with a documented precedence: valid embedded artwork from folder tracks / supported folder image names such as `cover.jpg` or `folder.jpg` / configured sidecar image / generated initials or gradient fallback. Avoid repeated directory-wide decoding. Validate MIME/size/dimensions; handle corrupt or unsupported artwork. Read `.info.txt` only as bounded plain text with documented field grammar, size/encoding limits and no HTML/script execution. Document its exact supported syntax in `docs/INFO_TXT.md`. Provide cache invalidation when a file changes and per-source rescan controls.

### D2.6 Audio format recognition and decoder capability (`PIX-L1`, `PIX-P1`)

Recognize the format by actual content/decoder support rather than extension alone. Common target formats MP3, FLAC, AAC/M4A, OGG/Opus and WAV are not to be advertised until verified against the project's installed decoder stack. Optional FFmpeg extension must be a separate, license-reviewed capability with ABI, APK-size, security and redistribution checks. Less common formats (APE, DSD, MIDI and others) need explicit device/build tests. On unsupported files, show the format and explain whether it is unsupported by this build, decoder, damaged or inaccessible. Failed decoder initialization must not crash playback or block library browsing.

### D2.7 Search (`NAM-Y7`, `PIX-D1`)

**Search screen components:** query field with clear/voice-input only if supported, recent searches, scope selector, filter chips, result tabs/groups, sort controls, result rows, no-results help and loading state. Search fields include title, filename, artist/featured artist, album/album artist, folder, genre, tags, mood, playlist name/content, rating, composer and lyrics only when indexed local/cached lyrics are available.

Normalize case, diacritics, punctuation and whitespace. Typo tolerance and ranking must be deterministic and testable. Debounce query updates while preserving immediate clear/cancel. Recent searches are optional, local, clearable and disabled by privacy preference. Search scope can be all library, current page, album, artist, folder, genre, playlist or lyrics. Search-to-play must respect Track Play Mode. Empty/no-result states offer scope/filter adjustments without inventing results. For large libraries, use indexed queries/paging and avoid full-library work on the main thread.

### D2.8 Metadata editor and batch tag writing (`NAM-L1`, `PIX-X1`)

**Entry:** track details, row overflow, album/artist bulk selection or Settings → Library → Tag editor. **Editor fields:** title, track artist, album, album artist, genre, year, track number, disc number, composer, lyricist, comment, grouping, mood, rating, lyrics and supported custom fields; embedded artwork where the format permits. Display source format, tag version/fields, read/write support, permission state and whether value currently comes from the file or Pulse Player database.

**Workflow:** load tags; edit fields with validation; preview before/after and affected tracks; choose database-only update versus writing to the original file where supported; show explicit permission grant/OS confirmation; optionally create recoverable backup; perform write; verify by reopening tags; refresh MediaStore/index record; update affected screens/caches; show actual result. Batch editing must list all affected files and identify format-specific unsupported fields before commit. Preserve unknown tags when the writer supports it. A failed/partial write must say which file/fields changed and provide restore/retry guidance. Scanning must never modify tags automatically.

**Safety:** no silent overwrite; no write to read-only or unsupported formats; use a tested TagLib or alternative with compatible license and Android support; handle SAF and MediaStore write requests; write atomically/backup where feasible. If the app cannot prove that file tags changed, label the operation failed or unverified—not saved.

## D3 --- Playback, transport, player service and queue

### D3.1 Core playback and session (`BASE-PLAY-01`, `PIX-X3`)

Use one playback engine and one foreground/media playback service as required by the current Android rules and project architecture. Use one MediaSession (or the existing approved session facade) and one queue authority. The Now Playing screen, mini-player, notification, lock screen, widget, Android Auto and Cast controller must all read/write the same playback state. Media3 remains the baseline unless repository audit proves a safe reason to change it.

**Playback state:** current media ID, source, title/artist/artwork, playing/buffering/paused/ended/error state, duration, current position, playback speed/pitch, repeat/shuffle, audio route, queue index and error details. Expose updates lifecycle-safely and avoid excessive polling. Seek must clamp to valid duration and handle unknown/live duration. On decoder or source error, preserve queue and expose retry/skip/remove options.

**Lifecycle:** handle app background, service recreation, task removal, process death, audio focus, noisy output, headset/Bluetooth controls, incoming call and route changes. Persist enough state to restore queue and position paused. Avoid double service/player creation. Release resources exactly once.

**Media formats:** only display format support when verified by decoder capability. Unsupported/decode-failed item should not terminate the service; offer skip, retry and inspect format.

### D3.2 Now Playing shared information and transport (`BASE-PLAY-02`)

Every player theme/layout must provide title/artist, current artwork/fallback, playback state, elapsed/total time when known, seek control, previous, play/pause, next, shuffle, repeat, favourite and queue access. Secondary actions include timer, add-to-playlist, equalizer, lyrics/song toggle, track details/share and theme/layout/button pickers as configured. Controls may move into overflow when compact, but the six essential actions mandated by Annex A/§7.6 remain reachable and never disappear.

**Seek bar:** shows actual position; dragging has a preview value separate from committed position; release seeks once; cancellation restores displayed position; unknown duration has a safe unavailable state. Optional waveform is described in D8.

**Player expansion:** mini-player expands/collapses without creating a second player. Transition must preserve current state and respect reduced motion. Marquee title must stop or slow when accessibility focus is on the text.

**Song/Lyrics toggle:** opens song artwork/metadata or lyrics state for the current track. If no lyrics exist, keep the user on a clear no-lyrics state with Import / Edit / Fetch (only when allowed) actions.

### D3.3 Mini-player (`BASE-PLAY-MINI`)

Display current artwork/fallback, title, artist if space, play/pause and documented next/expand actions. Reflect external control changes immediately. Artwork/disc and marquee animation obey reduced-motion and battery saver. Gestures must have visible alternatives and must not swallow system back or accessibility gestures. When no track is active, either hide the mini-player or show the established empty state; never show stale metadata.

### D3.4 Queue and saved queue sessions (`NAM-Q1`, `PIX-P2`)

**Queue sheet/panel:** current/up-next sections, accurate item count, stable rows, current-track indicator, drag handle, scroll-to-current, add/remove/move controls, per-item menu and empty state. Tap plays the selected item and updates the one queue authority. `Play next` inserts at the defined next position; add to queue appends according to the established policy. Bulk add preserves selection order. Move to top/bottom uses the model's stable IDs.

**Reordering:** long-press/drag must support auto-scroll and prevent accidental tap; accessible Move up/Move down alternatives are mandatory. A move updates both visual list and actual playback order. It must not restart the current song.

**Queue controls:** clear queue requires confirmation with item count; remove one/many; save as named queue; restore; rename; duplicate if supported; delete saved session; current queue not saved automatically unless user preference explicitly says so. Saved sessions include stable track references and a clear missing-track resolution path.

**Shuffle:** create a shuffled presentation/order while retaining an original/base order for restore. Turning shuffle off restores the intended base order and does not lose new insertions. Shuffle must have deterministic test hooks. Do not enable a second independent Media3 shuffle algorithm.

**Repeat:** Off → All → One in the baseline cycle. Any additional Repeat-N mode must be clearly distinguished from standard repeat and persist only as intended. Repeat-One must not double-count history incorrectly.

**Track Play Mode:** Selected track only, current search results, playlist, album, artist, genre or folder. Show active mode/context; changing context follows a documented policy and does not unexpectedly append unrelated tracks.

**Persistence:** restore paused after relaunch; restore stable IDs and current index; gracefully handle missing/removed files; do not erase queue due to a temporary permission/source issue.

### D3.5 Previous/next semantics and track insertion

Define previous behavior based on elapsed threshold and whether the current item was started from history/search/queue. Next skips unavailable tracks using a bounded loop; if all remaining items are unavailable, stop and explain. Repeated next/previous must not enqueue duplicate operations. Play next and add to queue must give feedback, update queue count and undo where feasible. If a tapped track is already queued, the action must follow a documented duplicate policy (allow duplicate, focus existing, or prevent duplicate) and give feedback.

### D3.6 Playback controls in notifications and lock screen (`BASE-MEDIA-UI`)

Notification shows current metadata/artwork, playback state, supported transport actions and optional timer state; it must update on track changes and not retain stale content. Lock-screen metadata and media controls must use the same MediaSession. Actions received while app UI is closed must update queue/history correctly. Notification permission denial on modern Android versions must produce a Settings explanation without blocking local playback. Test Bluetooth/headset actions, system media controls, process recreation and multiple audio apps competing for focus.

## D4 --- Favourites, playlist management, tags and recommendations

### D4.1 Favourites (`BASE-FAV-01`)

One source of truth for favourite status, persisted immediately. Toggle available in track row, Now Playing, details, widget only if supported and contextual menus. All surfaces update through the same observable state. Favourite count and collection contents remain consistent. Support empty favourites, sort/search, multi-select remove and backup/restore. Undo for accidental unfavourite where practical. Import must deduplicate by stable identity and report unmatched tracks without creating fake files.

### D4.2 Static playlists (`BASE-PL-STATIC`)

Create, name, describe, rename, duplicate, reorder tracks, add/remove tracks, multi-select add, change artwork where supported, sort, search within, share metadata, import/export, merge with preview, and delete with confirmation. Preserve custom order and duplicate-track policy. A track removed from a playlist is not deleted from the library. Missing tracks remain visibly unresolved and can be relinked or removed from that playlist. Playlist details show count, duration when fully known, artwork and created/modified information based on actual data.

### D4.3 Smart playlists (`NAM-SMART-01`)

Builder must contain rule groups with All/Any logic; add/edit/remove/reorder rules; include/exclude; field/operator/value editors; numeric/date ranges; sort and result limit; live preview count; Save, Save as, Duplicate, Rename, Edit and Delete. Supported fields include artist, album, album artist, genre, year, duration, rating, mood, tags, folder, date added, date played, play count, last played, favourite and file availability. Operators must match field type. Validate incomplete/contradictory rules before save. Show why a track matches where practical. Updating library/history triggers refresh without corrupting playlist identity. Convert to static playlist with an explicit snapshot time and preview.

### D4.4 Built-in collections and local recommendation engine (`NAM-SMART-02`, `PIX-D2`)

Implement Recently Added, Recently Played, Most Played, Never Played, Short Tracks, Long Tracks, High Rated, Favourites, Underrated, Forgotten/Lost Memories, Recently Rediscovered, This Day in Previous Years, Smart Tracks, Similar Tracks, Daily Mix and random discovery where enough real data exists. Each collection must have a documented query/algorithm, minimum-data rule, refresh cadence, eligible track filters, duplicate/diversity policy and explainable reason. All results come from the device's real library and local history. No fake AI label. Allow opt-out of personalization and clear derived recommendation data. No network is needed for the local version.

### D4.5 AI playlist as a separate refinement (`PIX-X2`)

AI is optional and never required for Daily Mix or any local playlist. The AI screen needs provider/model selection, secure key entry, key validation/test, connection state, consent/data preview, prompt editor with examples, generation progress/cancel, parsed result preview, matched/unmatched/duplicate counts, track selection, save-to-playlist and clear-history/delete-key controls. Send only minimum metadata and only after consent. Validate returned IDs/names against local library; never add non-local tracks unless the user explicitly selects a supported external source. Handle malformed response, prompt injection in remote content, rate limits, quota, timeout, network loss and provider changes. Label the playlist as AI-assisted and name the provider/model when useful. Local fallback is clearly labelled as local, not a successful AI result.

### D4.6 Tags, moods and ratings (`BASE-TAGS`)

Provide create/rename/delete with reference counts and confirmation, assign/remove to one or many tracks, search/filter by tag/mood/rating, bulk updates, and a clear unrated/unknown state. Deleting a tag or mood removes only the relationship unless the user chooses to delete associated metadata. Ratings must preserve their scale and mapping during import/export/backup. User-defined values are local and exportable in full backup.

## D5 --- Listening history, statistics and recaps

### D5.1 Play-event recorder (`NAM-H1`)

Record stable track ID, source identity, start/end timestamps, actual listened duration, media position range, speed and completion reason. Count plays only after the configured minimum seconds or percentage; exclude buffering and paused time. Define how seeking, repeat-one, partial playback, scrubbing and source failures affect a play. Avoid duplicate events when service reconnects or process restores. Store local timestamps/UTC boundaries consistently and aggregate by the user's configured time zone.

### D5.2 History browser and editing (`NAM-H2`)

Provide chronological history with date/range filters, track/artist/album grouping, search, play-again, remove-one, remove-range and clear-all with explicit confirmation. Removing an event never deletes the file, favourite, playlist entry or all-time data unless the user explicitly chooses the separate derived-data option. Imported records must be labelled with their import source and matching confidence.

### D5.3 Statistics and charts (`NAM-H3`)

Provide today/week/month/year/custom range; total listening time; number of sessions; unique tracks; top tracks/artists/albums/genres/folders; play counts; time-of-day patterns; streaks only where records justify them; and optional comparison to previous period. Charts include accessible text summary, empty state and clear date/time-zone basis. Missing or insufficient records must not be filled by estimates. Export/share recap cards locally only after preview. Privacy settings control display on widgets/lock screen but do not falsify stored history.

### D5.4 Monthly recap and Your Year (`NAM-H4`)

Generate from real history; show period boundaries, top tracks/artists/genres, time listened and other metrics only when data exists. Explain small sample sizes. Hide recap when no qualifying history exists. Share action generates a local image/card with user preview, privacy options and no hidden upload. Rebuilding after deleting/importing history must update aggregates deterministically.

## D6 --- Lyrics system

### D6.1 Local lyrics discovery/import (`NAM-Y1`)

Find adjacent `.lrc` / `.txt` according to documented file matching rules, and allow SAF file selection/import. Show source path/provider and whether lyrics are plain, line-synced or word-synced. Resolve ambiguity when more than one candidate exists; do not attach lyrics to the wrong track based on title alone. Preserve the original source file unless the user explicitly replaces/deletes it. Unsupported encoding/format must yield a useful error and preserve raw file.

### D6.2 Lyrics display and synchronization (`NAM-Y2`, `PIX-Y2`)

Show plain lyrics with readable line wrapping and scroll, or timestamped lines with centered active-line highlight and auto-scroll. Synchronization uses current playback position and the selected offset. Tap a timed line to seek; do not make plain text seekable. Pause, seek, speed changes and track transitions must update active line correctly. Auto-scroll can be disabled; manual scroll must not be forced back until the documented resume behavior. Provide no-lyrics, loading, malformed, stale and offline states. Respect all player themes, dark/light contrast, font scaling, landscape and screen readers.

### D6.3 Lyrics editor and timing (`NAM-Y3`)

Edit plain text, line timestamps and offsets; create timestamps during playback if supported; validate syntax and monotonic/order behavior; preview sync; save to a sidecar or supported embedded field with an explicit target. Undo edits before commit and preserve prior version where feasible. Export/import LRC/TXT through SAF. Never overwrite embedded lyrics without a clear warning. If a format cannot be safely written, offer sidecar export.

### D6.4 Online LRCLIB/provider lookup (`PIX-Y1`)

Only under full flavor + master Online Features switch + per-provider switch + consent. Manual Fetch Lyrics is primary; optional Wi-Fi-only auto-fetch must be separately enabled. Match by title, artist, album and duration; search fallback must show candidate confirmation when ambiguous. Cache provider result with source and fetch time. Rate-limit, timeout and network failures are visible; cache expiry and deletion are configurable. Online lookup must not run in offline build or when disabled. Do not replace existing manually edited lyrics without confirmation.

## D7 --- Timer, pause behavior and external interruptions

### D7.1 Sleep timer (`BASE-TIMER-01`)

Timer screen/sheet offers presets, custom duration, end of current track, end of current queue/playlist, after N tracks and fade-out duration. Show remaining time, active condition, pause/resume/edit/cancel controls, and notification/Now Playing indicator where supported. Only one active timer unless the product explicitly defines multiple independent timers. Persist enough state to recover after process death and reboot according to documented policy; use monotonic time where needed to prevent wall-clock changes extending or shortening a timer incorrectly. On completion, stop/pause/fade exactly once and report completion. Never resume unexpectedly.

### D7.2 Fade behavior and stop after current (`BASE-TIMER-02`)

Fade curve/duration is validated; fade applies to the actual player volume and does not mutate system volume. If a seek or track transition occurs during fade, define whether fade is cancelled or continues. Stop-after-current prevents next track advancement and handles repeat/shuffle consistently. Expose current setting and a quick disable. Unsupported fade control is disabled with explanation, not cosmetic.

### D7.3 Audio focus, call, headset and Bluetooth behavior (`BASE-INTERRUPT-01`)

Handle transient/permanent audio focus loss, incoming/outgoing call interruptions, headphone unplug/noisy route, Bluetooth disconnect, output device switch and notification interruptions using Android callbacks. Settings define user policy only where OS behavior permits. Pause on headset disconnect by default as specified by the baseline; never auto-resume after call or route reconnect unless explicitly chosen. Avoid duplicate pause/resume callbacks. Show a brief explanatory message only when useful and respect DND/system policy.

## D8 --- Audio settings, EQ and playback processing

### D8.1 Equalizer (`NAM-S1`)

Screen contains capability/route label, master enable, frequency bands, per-band gain, preset selector, save-as, rename, duplicate, delete, reset and clipping warning. Persist presets and selected preset. Changing output route/player session rebinds the effect safely. Unsupported effect or route displays exact unavailable state. Validate gains/ranges from actual engine/effect implementation. Avoid claiming the values are applied if platform API rejected the change.

### D8.2 Bass boost, reverb, virtualizer/surround and mono (`NAM-S2`)

Each effect has an independent capability check, enable state, parameter controls and reset. Do not show sliders for an effect not instantiated. Recreate/release effects safely when session/route changes. Effects must not stack duplicate instances on resume. Explain when multiple effects compete or a device controls the effect externally.

### D8.3 Parametric EQ and AutoEQ profile import (`NAM-S3`)

Parametric EQ only when the DSP chain supports it. Show band type, frequency, gain and Q with validation and stable ordering; provide an A/B toggle and response visualization only if it corresponds to applied values. AutoEQ import validates file type, profile schema and supported bands, previews changes and does not silently overwrite the current profile. Unknown profile fields are reported.

### D8.4 Playback speed, pitch and tuning (`NAM-S4`)

Speed and pitch are separate when the engine supports independent control. Provide slider/numeric reset and sensible step increments. Show current value on Now Playing and optional quick action. Semitone changes must map correctly to pitch ratio. Optional 432 Hz mode must explain the actual signal-processing change and must not be advertised as a special mastering or fidelity improvement. Restore defaults and per-track profiles consistently.

### D8.5 ReplayGain/loudness normalization and per-track profiles (`NAM-S5`)

Read supported ReplayGain/R128 metadata or compute only when explicitly implemented. Explain whether gain is track/album mode and prevent clipping. Per-track profile can contain EQ/volume/speed/pitch only for capabilities genuinely supported; apply when track changes and restore global profile when leaving track-specific mode. Do not modify source files. Reset and disable profiles are available.

### D8.6 Gapless, transitions, crossfade and beat matching (`NAM-S6`, `PIX-P3`)

Gapless is separate from crossfade. Expose crossfade duration (0–12 seconds per catalogue), curve selection (linear/equal-power/S where implemented), same-album gapless preference, and an experimental toggle for beat matching. Show capability and codec/device limitations. Transition must not cut the wrong track, skip a queue item or break repeat modes. Analyze BPM/key in background and cache with file identity; label estimated/unknown values. Beat matching is off by default, must be previewable and immediately disableable. Do not claim beat matching if the engine merely overlaps tracks. Test short tracks, seek near end, manual next, pause, route change, repeat-one/all, buffering and unsupported codecs.

### D8.7 Audio path viewer and hardware-specific functions (`NAM-S7`)

Show known stages (source format, decoder, DSP/effects, resampler if known, output format/route) and distinguish measured values from unknowns. Output-device page shows current route and supported controls. Bit-perfect/direct/USB DAC/exclusive/32-bit float claims require measured end-to-end verification. Hide unavailable controls or mark with exact reason. Do not infer output sample format solely from internal buffer type.

## D9 --- Artwork, waveform, visualizers and animation

### D9.1 Artwork pipeline (`PIX-A1`, `NAM-ART-01`)

Prioritize embedded/local artwork, then user-supplied local artwork, supported folder images, optionally permitted online artist image, then a generated initials/gradient fallback. Store thumbnail/full-size variants in bounded caches with memory LRU and persistent cache; key by stable media/artwork identity and invalidate on tag/source changes. Handle missing, corrupt, oversized, unsupported and slow-decoding images. No repeated network fetch before configured cache TTL. Clear-cache action reports bytes/items and never deletes original image files.

### D9.2 Album-art gestures and image actions (`NAM-ART-02`)

Tap, long-press and swipe actions must be user-configurable where promised. Default actions are documented; every gesture has an accessible visible equivalent in menus. Avoid conflicts with system navigation, player layout swipe switching and track-row interactions. Image viewer must support loading, zoom only if included, share/save only when permitted, replace artwork with explicit confirmation, and return to prior playback state.

### D9.3 Waveform generation and seek bar (`NAM-U3`)

Decode/analyze audio off the main thread into approximately 200–400 peak buckets as specified. Cache per track using media identity and decoder version. Show generation state and progress only when measurable. While unavailable/generating/error, use the normal seek slider. Tapping/dragging waveform maps accurately to playback duration; played portion is visually distinct; unknown duration disables seeking appropriately. Cancel work when track/source changes and avoid repeated generation. Verify cache invalidation after file changes.

### D9.4 Visualizers (`NAM-U5`)

Modes: bars, waveform, circle, dots and any additional mode discovered in the actual project/reference audit. Include enable/disable, mode picker, intensity, frame rate and battery-aware behavior. Use permitted audio-level/tap data; do not request microphone permission just to animate the player's own output. Pause/release analysis when not visible or when playback stops unless a documented feature requires otherwise. Reduced motion/battery saver uses static or off mode. If audio tap unavailable, show static fallback and reason.

### D9.5 Animated backgrounds, procedural wallpapers and particles (`NAM-U5`, `NAM-U7`)

Provide procedural backgrounds/skins (e.g. nature/forest, ocean, stars, mesh and abstract gradients) and user-imported wallpaper only where implemented. Do not bundle unlicensed copyrighted assets. Control animation intensity, speed and frame rate; stop when off-screen; obey reduced-motion and battery saver. Artwork particles/thumbnail pulse use actual audio intensity and must be subtle, throttled and disabled when unsupported. Mini-player Party Mode edge breathing must have a clear toggle and not obscure text/controls. Custom wallpaper import copies to app-private storage, validates dimensions/size/format, handles permission revocation and provides remove/restore-default actions.

## D10 --- Themes, skins, layouts and button arrangements

### D10.1 Keep customization axes independent (`BASE-THEME-AXES`)

Persist these as separate settings: app theme mode (System/Light/Dark/AMOLED), color source/palette (Pulse palette, dynamic wallpaper, artwork, manual accent, scheduled palette), skin/background family, player theme, player layout mode, navigation style, corner radius and playback button arrangement. Changing one axis must not silently reset another. If some combinations are unsupported, explain the exact compatibility rule and provide a valid fallback.

### D10.2 Existing nine player themes (`BASE-PLAYER-THEMES`)

Keep IDs, preview, behavior and persistence for all nine: `classic` / Classic; `mountain` / Mountain Horizon; `mesh` / Mesh Glass; `minimal` / Minimal Mood; `acoustic` / Acoustic; `vinyl` / Vinyl; `coast` / Coastal Ocean; `starry` / Starry Sky; `immersive` / Immersive Artwork Player. The actual repository screenshot and implementation are the visual source of truth; do not invent a new look and claim it preserves the old one.

For **each theme individually**, audit and supply: preview thumbnail; Now Playing composition; artwork crop/blur behavior; title/artist typography; progress/seek style; transport/action placement; queue access; lyric display; dark/light/AMOLED treatment; empty/missing-art treatment; focused/pressed/disabled/loading states; small-phone/large-font/landscape/tablet behavior; reduced-motion version; persisted selection; switching test while music is playing. The same playback actions and queue model must work in all nine. Immersive remains a presentation-only blurred-artwork/floating-controls/bottom-queue look.

### D10.3 App color themes, skins and dynamic palette (`BASE-SKINS`)

Preserve System, Dark, AMOLED, Light and existing Plum, Aurora, Afterglow, Ocean, Sunrise, Forest, Mono, Mesh and Dynamic gradient families. Add optional Material You tonal surfaces, artwork-derived palettes, manual seed color and time-of-day schedule only as additive choices. Dynamic extraction uses real artwork and contrast safeguards. Color palette must be applied consistently to text, surfaces, icons, progress, chips, sheets, dialog, menu, text fields, errors, disabled and focused states, navigation bars, widgets and notifications where supported. Accent contrast and favourite/apply states preserve existing meaning. Preview uses actual user state and apply/reset actions. Theme changes update immediately and persist; no app restart or playback restart.

### D10.4 Corner radius, navigation and motion (`PIX-U3`, `PIX-U4`, `PIX-U5`)

Corner radius slider supports the documented 0.0–2.0 scale or a clearly labelled supported range and previews affected components; never cause clipping or unreadable shapes. Navigation choices: existing top tabs (default), bottom bar, floating bottom bar and rail, with adaptive forced behavior at the specified wide breakpoint unless the user-selected override is valid. Dark/light schedule follows local time and handles time-zone changes. Album-art palette extraction uses validated image sampling and updates when artwork changes.

### D10.5 Player layout registry (`NAM-U4`, `BASE-PLAYER-LAYOUTS`)

Register all existing layout modes and additive modes found by audit. Required modes from the merged catalogue include Standard/current arrangement, `WIDESCREEN` (artwork left, controls/queue right; automatic at width ≥600dp or manually selected where specified), Waveform layout/seek style where distinct, Immersive Artwork presentation where represented as layout, and `ATMOSPHERIC_QUEUE`. Do not confuse player theme with layout mode. Each layout needs stable ID, display name, subtitle, icon, accurate thumbnail preview, capability compatibility, selection persistence and unknown-ID fallback. Live switch must not change track, position, queue, speed or audio effects. The player switcher bottom sheet and Settings → Appearance → Layouts must use the same registry. Swipe cycling only on the designated top-bar/drag-handle strip; never conflict with artwork swipe-to-skip.

### D10.6 Atmospheric Queue (`BASE-ATMOSPHERIC-QUEUE`)

Upper panel: procedural dark forest/mist background with static gradient fallback; dark scrim; collapse control; Song/Lyrics switch; theme/layout/button picker entries; overflow; track title/artist/metadata; real progress; shuffle, previous, play/pause, next, repeat; favourite; timer; add-to-playlist; queue jump; EQ. Lower panel: light/off-white queue card with rounded top corners; Up Next title/count; scrollable queue; thumbnails/fallback; active item; active equalizer bars only for current track; per-track options; tap-to-play; reorder/remove/play-next; empty queue and current-not-in-queue states. Keep amber accent readable and honor app accessibility. Compact portrait uses split; short/landscape prioritizes essential playback controls; tablet/foldable can use two columns; large text must scroll rather than clip. Reduced motion makes fog/visualizer/equalizer static without affecting playback.

### D10.7 Player button arrangement switcher (`BASE-BUTTON-LAYOUTS`)

Implement all **15 canonical stable IDs** from Annex A §8.2: `centered`, `split`, `minimal`, `waveform_below`, `queue_inline`, `expanded`, `compact`, `floating`, `symmetric`, `asymmetric`, `two_row_grid`, `radial`, `thumb_zone`, `side_rail`, `chips`. Each must be visually and spatially distinct, not merely recolored. The arrangement changes placement, size, grouping and labels only; it never changes action semantics.

**Picker/editor components:** live thumbnail grid/list, preset name and description, compatibility reason, current/preview indicator, Apply/Cancel, Reset to default, and optional Custom editor. The custom editor provides available-button tray, placed-button canvas, grouping, drag handles, move earlier/later alternatives, row/column assignment, size/label visibility, reset, duplicate/rename/delete and validation. Before applying, preview with the current real track state. Custom preset must not remove mandatory controls.

**Mandatory safety:** play/pause always visible and at least 56dp; every target at least 48dp; no overlapping controls; labels hide before icons shrink; actions with no space move to overflow; disabled capabilities are not made active by arrangement. `queue_inline` and `side_rail` must be disabled with reason when the active layout cannot host them. Recommendation feature explains why a preset is recommended, previews before/after and requires Apply. No silent rearrangement. Save named presets and restore separately to recommended/default.

### D10.8 Motion and animations (`BASE-MOTION`)

Preserve existing spring animations, mini-player expansion, heart/favourite feedback, play/pause morph, playing bars, theme transition and layout transitions. Provide reduced-motion global handling that stops or replaces continuous/decorative motion while preserving state feedback and core interaction. Respect Android animator scale, battery saver and accessibility settings. Animation must not block controls or cause repeated operations.

## D11 --- Settings, privacy, permissions and information architecture

### D11.1 Settings home and categories (`BASE-SETTINGS`)

Settings home must be searchable or clearly categorized. Required destinations: Appearance; Player; Player controls/layouts; Library; Playback; Audio/EQ; Lyrics; Widgets; Backup & Transfer; Connectivity/Online Features when included; AI when included; Permissions; Privacy; About. Every setting needs title, description, current value, default, persistence, reset behavior, capability requirement and effect scope. Use a single settings repository/DataStore source of truth; do not duplicate settings per screen.

### D11.2 Permission center and first-run education (`BASE-PERMISSIONS`)

Provide a status screen showing permission, why required, which features depend on it, current grant state, request action and Open system settings action when permanently denied. Request at point of use or in the explicitly documented first-run education. Do not request broad storage permission when SAF/MediaStore is sufficient. Permission denial produces limited mode and does not trap the user. Distinguish Android version restrictions, one-time access, revoked access and unavailable source. Ringtone permission only when ringtone action invoked. Notification permission is separate from media playback. Every permission-specific feature must be tested granted/denied/revoked.

### D11.3 Offline-only, online master switch and privacy (`BASE-ONLINE-PRIVACY`)

Offline build must not include online clients/manifest permission when required by product direction. Full build's master switch defaults OFF and blocks requests at the shared gateway; per-provider toggles also block calls. No telemetry, tracking or ads. Explain what metadata leaves the device for each optional provider, why, when and how to delete cached data/credentials. Provide account disconnect, revoke and credential deletion. Network disabled means no background retries or automatic image/lyrics/AI/scrobble/remote-source fetch. LAN features must also be disabled under the agreed Offline-only definition unless user explicitly enables local network capability with separate disclosure.

### D11.4 About, licenses and diagnostics (`BASE-ABOUT`)

Show app version/build flavor, Android/device diagnostics needed for support, third-party notices/licenses, offline/online disclosure, project links and privacy statement. Diagnostics must be user-triggered, previewable and redact file paths, personal content, secrets and API keys by default. No automatic crash uploads. Copy/share diagnostic report only after user review.

## D12 --- Backup, restore, import and export

### D12.1 Classic favourites backup v1 compatibility and v2 (`BASE-BACKUP-CLASSIC`)

Keep the exact `pulseplayer.favorites` v1 JSON schema and established three-level fuzzy matcher unchanged. Classic Backup v2 stores the v1 JSON plus separate byte-for-byte audio copies in `Music/Pulse Player/` as defined by Annexes A and E; do not add a v2 JSON schema or embed audio in JSON. Classic remains separate from Full backup. Export flow: preflight favourites/copyable files/space, obtain explicit confirmation, safely copy local audio without changing originals, write the v1 JSON with `displayName` mapped to final copied filenames, verify every file and JSON, then show a per-item summary. Import flow: ask the user to place both audio files and JSON in that folder, obtain SAF folder/document access, scan/index audio, decode v1 JSON, preview matches/ambiguities/missing files, ask Merge/Replace policy, and apply favourites atomically. Audio files are never deleted by import. Report per-item errors and retain older backups.

### D12.2 Full backup and restore (`BASE-BACKUP-FULL`)

Follow the original full-backup reference format exactly; do not merge it with Classic favourites. Inventory must state whether settings, database, playlists, favourites, history, tags/ratings/moods, lyrics, artwork, queue sessions and app-managed files are included. Music bytes are included only if the existing Full format explicitly says so. Include manifest/schema/app version and checksums. Export to user-selected location with progress and verified output. Restore preflight shows source/version/content categories/conflicts/destructive implications. Stage data and validate checksums before commit. Create rollback point/pre-restore database backup. Restore in safe order; map stable IDs; preserve unknown future fields where safe; resolve conflicts explicitly; no partial destructive overwrite. If interrupted, resume or rollback with clear status. Test restore on a clean install and populated install from every supported prior schema.

### D12.3 General import/export (`BASE-IMPORT-EXPORT`)

Support M3U/M3U8 import/export where practical; preserve order, paths, encoding and supported metadata. Show missing/unmatched paths and allow manual matching. Import preview lists new playlist, merge/replace choices and duplicates. Never overwrite without confirmation. External history imports (Spotify, Last.fm, ListenBrainz, YouTube exports) require user-selected export files or documented APIs; no password collection or private-account scraping. Preview counts and match confidence, mark origin and show unmatched records. Export only user-selected data and provide privacy warning for listening history.

### D12.4 Ringtone action (`BASE-RINGTONE`)

Entry only from supported track context menu/details. Check format, duration/platform constraints and permission. Show the selected excerpt/file and destination; ask confirmation before writing/setting system ringtone. Use the correct Android API and verify the result. Permission is requested only after the user invokes the action. On unsupported format or denied permission, preserve source and explain alternatives. Never rename/move original track silently.

## D13 --- Optional integrations and platform-specific features

### D13.1 Remote libraries: Subsonic/Navidrome, Jellyfin, WebDAV and SMB (`INT-REMOTE`)

Each provider gets its own setup screen: server address, supported auth fields, TLS/security explanation, connection test, capability summary, browse hierarchy, connection status, disconnect and delete credentials. Validate URL/protocol; never disable certificate validation silently. Credentials/tokens stored in Keystore-backed secure storage, excluded from logs and backups unless separately encrypted and consented. Remote tracks need stable source IDs and offline/streaming status. Handle pagination, authentication expiry, server unreachable, unsupported API, rate limits and partial listing. Do not claim an offline remote track until a local verified cache exists. Respect master/per-service switches.

### D13.2 Download manager and cache (`INT-DOWNLOAD`)

Only download from a source whose API/terms permit it. Show queue, destination/cache policy, file name, progress bytes, total bytes where known, pause/resume/cancel/retry, integrity verification, completed/failed state and offline availability. Cancelled/failed partial files are cleaned or resumable safely. Never overwrite source files. Cache clear shows items/size and excludes pinned/offline-priority items. Never delete original local music during cache eviction. Offline playback resolves to verified local cache without silently attempting remote streaming.

### D13.3 Online artwork and LRCLIB (`PIX-A1`, `PIX-Y1`)

Separate provider setup and consent; only fetch when master/per-service switches and build flavor allow. Provide attribution/source details, cache TTL, manual refresh and clear-cache. Local artwork and sidecar/embedded metadata remain usable offline. Failed online lookup must retain current artwork/lyrics and expose retry; it must not replace with blank data.

### D13.4 YouTube/video, SponsorBlock, dislike signals and subtitles (`INT-VIDEO`)

Do not ship scraping, undocumented endpoints, DRM bypass, authentication circumvention or unsupported download options. Only implement a legally reviewed, documented and policy-compliant source/provider. If included, provide source attribution, consent, provider setup, actual player surface, transport/seek, brightness/volume/fullscreen/speed controls and accessible alternatives. Sponsor segments require a legitimate supported source, timeline visualization, skip toggle and clear source provenance. Third-party dislike estimates must never be labelled official. Subtitle system requires actual file/embedded track discovery, language selector, timing, styling, offset, no-subtitle state and malformed-file handling. Video tab/action is absent or clearly gated when these contracts are not met.

### D13.5 LAN sync and Listening Party (`INT-LAN`)

Provide host/create and join session, code/QR pairing, peer identity and trust confirmation, protocol/version negotiation, authenticated encrypted transport where supported, connection status, host/guest roles, permissions, queue sync, mapped local track identity, missing-track matching, playback position, drift handling, reconnect and leave/end session. State clearly whether audio itself is streamed or only queue/playback metadata is synchronized. Host authority and allowed guest operations (play/pause/seek/skip) must be configurable. Preview categories before transferring library/history/settings. Do not expose a public internet server by default. Conflict and disconnect must not corrupt local queue or history.

### D13.6 Chromecast / Google Cast (`PIX-C1`)

Only show Cast action when required Play Services/SDK and network capability exist. Discover devices; show device name/route; connect/disconnect; transfer playback only when supported; route state/volume; handle device loss/session end. For local files, any LAN HTTP serving must use scoped, short-lived, token-protected access and stop when casting ends. No unauthenticated open server. Explain unsupported device/build conditions. Test local file access, connection timeout, route changes, playback seek, pause/resume and disconnect.

### D13.7 Android Auto (`PIX-C2`)

Use approved media browsing/session APIs and driving-safe templates. Browse tree should include Recent, Favourites, Playlists, Albums, Artists and Folders only where stable and performant. Provide stable media IDs, correct metadata/artwork and transport controls; no complex text-entry or visual customization while driving. Configure automotive metadata/resources as required by project/Android version. Validate with Desktop Head Unit and actual vehicle where possible. Unsupported browse item or unavailable source must give a safe result.

### D13.8 Wear OS (`INT-WEAR`)

If included, companion controls must reflect the same session state and support play/pause/next/previous and supported browse/action scope. Define whether phone connectivity is required; show disconnected status; do not claim standalone playback unless audio source, storage and session architecture actually support it. Keep battery and sync scope bounded. Gate if not implemented/tested.

### D13.9 Glance widgets (`PIX-C3`, `BASE-WIDGET`)

Support the existing widget and add the specified variants: responsive/default, compact artwork-centered and controls-only (plus documented 2x2/4x1/4x2 sizes as appropriate). Each widget instance can independently choose style if promised. Show real artwork/title/artist/progress/playing state and controls; no-track state has an open-app action. Widget taps route to valid deep links. Handle stale app process state, update limits, resize, theme, transparent backgrounds and missing artwork. Avoid expensive scans/network in widget update. Widget actions call the same player controller. Test reboot, process kill, playback from notification, resize and multiple widget instances.

### D13.10 Quick Settings tile and status-bar lyrics (`BASE-QUICKSETTINGS`, `INT-STATUS-LYRICS`)

Quick Settings tile reports actual playback state, supports only its declared action, handles service absence and does not claim a state change before the controller confirms it. Status-bar lyrics are platform/policy gated; provide permission/explanation if needed, privacy control, timing, hide/stop, no-lyrics and Android-version compatibility. If restricted or unavailable, omit the active UI and record gated status.

### D13.11 External scrobbling (`INT-SCROBBLE`)

Optional provider setup, secure credential/token storage, account test, per-service enable, consent/data preview, scrobble eligibility threshold, retry queue and disconnect/delete. Respect pause/skip/repeat and avoid duplicate scrobbles on reconnect. Never upload when master/per-service switch is off. Show last sync and errors. No credentials in logs or backups.

## D14 --- Required shared components and domain responsibilities

Reuse existing equivalents; the following inventory is a completeness checklist, not a request to create duplicate classes.

### D14.1 Presentation components

- App scaffold, adaptive navigation, top bar, mini-player dock, Now Playing host.
- `TrackRow`, `AlbumCard`, `ArtistRow`, `FolderRow`, `PlaylistRow`, `QueueTrackRow`, `LibraryPageHeader`.
- Search field, sort sheet, filter chip group, scope picker, selection toolbar and count.
- Artwork container with loading/missing/corrupt/fallback state.
- Transport controls, seek/waveform control, playback state indicator, queue count, favourite button.
- Player theme/layout/button preset picker and custom arrangement editor.
- Lyrics viewer, no-lyrics state, sync offset control and editor.
- EQ band controls, effect toggles, audio-route/capability card and applied-state indicator.
- Visualizer canvas/surface and static/reduced-motion fallback.
- Smart playlist rule builder and recommendation reason card.
- History list, statistics summary/chart with accessible text and recap preview.
- Scanner/source manager, progress/report, duplicate review, tag editor/preview.
- Backup/restore preflight, progress, conflict resolver, summary and rollback/recovery UI.
- Integration setup/connection cards, network/permission consent, pairing/remote status.
- Shared loading, empty, error, partial-result, permission, offline, unsupported, confirmation, progress, undo and retry components.

### D14.2 Domain/data/platform responsibilities

- Library/source repository and scanner/indexer.
- Media metadata/tag reader/writer adapter.
- Duplicate matcher and source resolver.
- Search/index query engine.
- Playback controller/service, MediaSession facade and single queue model.
- Playlist and saved queue repositories; smart playlist evaluator.
- Favourite/rating/tag/mood repository.
- Play-event recorder, history repository and statistics aggregator.
- Lyrics repository/parser/provider adapter.
- Timer/interruptions coordinator.
- Audio capability detector, effect/session manager, DSP configuration and route monitor.
- Artwork/waveform/visualizer data pipelines and bounded caches.
- Theme/skin/player-theme/layout/button registry and settings repository.
- Backup/restore manager, manifest/checksum/migration/rollback service.
- Permission/capability registry and centralized network gateway.
- Remote provider adapters, AI provider interface, LAN session service, Cast/Auto/Widget integrations only where included.

### D14.3 Cross-cutting nonfunctional components

All components must use typed IDs, validated models, theme tokens, lifecycle-aware state collection, cancellation and stable list keys. I/O and decoding run off the main thread. Secrets use secure storage. File operations validate permission/free space and verify bytes. Every operation reports a typed success/partial/failure/cancelled/unsupported result. Accessibility semantics, keyboard/focus navigation, large-font layouts and minimum targets are mandatory.

## D15 --- Feature ID coverage register

Create `docs/spec/FEATURE_COMPLETENESS_MATRIX.md` and include at least the following ID groups. Split child capabilities further whenever they have different entry points, gates, user outcomes or persistence.

| ID group | Required feature coverage |
|---|---|
| `BASE-APP-*`, `BASE-HOME-*`, `BASE-UI-*` | Launch, onboarding, shell/navigation, adaptive layouts, For You sections, every row/card, all library pages, selection and track details |
| `BASE-LIB-*`, `NAM-L1`–`NAM-L8`, `PIX-L1`–`PIX-L4` | Scanner/sources, all filters, duplicate review, sorting, folder metadata/artwork, artist/genre parsing, formats, page organization |
| `NAM-Y7`, `PIX-D1` | Search fields, scopes, normalization, typo tolerance, recent history, filters, no-results and search-to-play |
| `BASE-PLAY-*`, `BASE-MEDIA-*`, `NAM-Q1`, `PIX-P2`, `PIX-X3` | Playback service, MediaSession, notifications, lock screen, mini-player, Now Playing, all transport, queue/reorder/shuffle/repeat/play modes and recovery |
| `BASE-FAV-*`, `BASE-PL-*`, `BASE-TAGS`, `NAM-SMART-*`, `PIX-D2`, `PIX-X2` | Favourites, static/smart playlists, tags/moods/ratings, built-in collections, local recommendations and optional AI playlist refinement |
| `NAM-H*` | Play event criteria, history browser/import/editing, stats ranges/charts, monthly and yearly recaps |
| `NAM-Y*`, `PIX-Y1`, `PIX-Y2` | Local lyrics, formats, sync, editor, offsets, online fetch/cache and provider controls |
| `BASE-TIMER-*`, `BASE-INTERRUPT-*` | Sleep timer modes, fade, stop after current, audio focus, calls, headset/Bluetooth behavior |
| `NAM-S*`, `PIX-P1`, `PIX-P3` | EQ, presets, advanced effects, speed/pitch/tuning, ReplayGain, per-track profiles, transitions, gapless, crossfade, beat-match and audio path |
| `NAM-ART-*`, `NAM-U3`, `NAM-U5`, `NAM-U7` | Artwork discovery/cache, gestures, waveform generation/seek, visualizers, backgrounds/particles/wallpapers |
| `BASE-THEME-*`, `BASE-PLAYER-*`, `NAM-U1`–`NAM-U7`, `PIX-U*` | Nine existing player themes, app palettes/skins, Material/dynamic color, navigation/corners/motion, all player layouts and Atmospheric Queue |
| `BASE-BUTTON-*` | All 15 button presets, custom editor, recommendations, validation, persistence and reset |
| `BASE-SETTINGS-*`, `BASE-PERMISSIONS-*`, `BASE-ONLINE-*`, `BASE-ABOUT-*` | Every existing and new setting, permission center, offline/online gates, privacy, licenses and diagnostics |
| `BASE-BACKUP-*`, `BASE-IMPORT-*`, `BASE-RINGTONE` | Classic v1/v2, Full backup, restore, M3U/M3U8, history import/export and ringtone workflow |
| `INT-*`, `PIX-C1`–`PIX-C3`, `PIX-A1` | Remote sources, downloads/cache, legal video gates, LAN/listening party, Cast, Android Auto, Wear, widgets, Quick Settings, status lyrics, scrobbling, online artwork |

For every row, status must be one of: `Implemented`, `Gated`, `Unsupported-with-reason`. During development, `Not started`, `In progress` and `Blocked` may be used, but they cannot be presented as completed at release. `Gated` must state the gate (flavor, OS version, permission, provider setup, device capability, legal/license review or external hardware). `Unsupported` must describe why and the closest honest alternative. Attach test names, screenshots/video where appropriate, build variant, device/API and commit evidence.

## D16 --- Final no-omission implementation and audit rules

Before implementation:

- Enumerate every currently visible control from the running app and source, including overflow menus, gestures, widgets, notification actions and hidden settings.
- Enumerate every feature ID in sections 00–19 and Annexes A–E, then merge with the actual upstream feature inventory. Do not stop at this written list if repository inspection reveals more.
- Create a screen-to-feature map and data-flow map. Identify shared owners and migration impact.
- Record which features already work; capture regression evidence before editing.
- Resolve licensing, Android API, provider terms and flavor gates before promising platform-specific features.

During implementation:

- Work in small phases and implement a feature's complete vertical slice before moving on.
- Do not add a button before the destination/action, state, repository and capability gate exist.
- Every enabled control must perform its documented operation. A disabled control must explain the reason and any prerequisite. A hidden control must be listed in the matrix with its status.
- Never use mock/sample content in production. No fake network, AI, lyrics, stats, artwork or success state.
- Do not remove existing settings or alter IDs without migration. Do not reset user data to simplify testing.
- Update `FEATURE_COMPLETENESS_MATRIX.md`, settings documentation, architecture diagrams, tests and user documentation with each phase.

Before release, run an automated and manual audit:

1. Tap every visible control in every screen and every overflow menu. Verify the domain effect and persisted result, not merely navigation or animation.
2. Test every setting changed, restored after process death and reset to default.
3. Test every feature in supported and unsupported capability states, including denied/revoked permissions, offline mode, missing provider, missing source and unsupported device.
4. Test destructive actions, rollback, undo, duplicate submit, cancellation, partial success and process kill during operation.
5. Test all nine player themes, every existing and new skin, every supported layout, all 15 button presets, custom button arrangements, custom player elements and all app color modes against real playback.
6. Test library scan, playback, queue, favorites, playlists, history and backups with a populated pre-upgrade database and actual files; verify no data loss.
7. Confirm that no action reports success before work commits and that no dead control remains active.
8. Publish the final matrix with `Implemented`, `Gated` or `Unsupported-with-reason` for every ID. List limitations visibly. Do not claim feature parity unless each required feature has evidence.

**Release rule:** A beautiful screen with unimplemented behavior is a failed feature. A visible button is only the entry point. The deliverable is the entire user outcome, its real components, data/domain/platform implementation, persistence, permission/capability logic, feedback, recovery and verified tests.

------------------------------------------------------------------------

# Annex E --- Verified Upstream Parity, Original Design Integration, Complete In-Player Customization and Classic Backup v2

> **Purpose and precedence:** This annex adds a verified source-based parity baseline and makes the owner-requested customization and backup behaviors unambiguous. It is additive to the whole master specification. It preserves all existing Pulse Player features, screens, the nine player themes, all current skins, layouts, player controls, settings and backup formats. For conflicts about upstream feature scope, license interpretation, design integration, in-player customization and Classic Backup v2, this annex is authoritative.

## E0 --- Non-negotiable merge promise

1. Do not create a replacement app or a new visual identity that removes Pulse Player's existing identity. Keep the existing package/application ID, navigation, data, working playback engine and all existing user-facing features unless the actual repository proves an item is dead or duplicate; even then, record it and request owner approval before removal.
2. Do not claim that every feature from either reference app has been copied until the actual source repositories, current documentation, settings, changelogs and release notes have been audited and every feature has evidence in `docs/spec/PARITY_INVENTORY.md`.
3. The goal is **functional feature parity where technically, legally and product-policy compatible**, plus original Pulse Player designs that capture useful interaction and visual concepts. It is not a pixel-perfect clone or a fork of either reference app.
4. Every upstream item must end in one explicit state: `PRESENT`, `PARTIAL`, `NEW`, `GATED`, `UNSUPPORTED` or `EXCLUDED-WITH-REASON`. No upstream capability may silently disappear from the matrix.
5. Existing Pulse Player features and reference-app features remain distinct records even when they share one implementation. A single reusable implementation can satisfy multiple parity rows only when the actual user workflow and acceptance criteria are met for each row.

## E1 --- Source and license verification snapshot

The following is the external-reference snapshot checked on **2026-11-03**. Re-check upstream repositories immediately before implementation because both projects change over time. This snapshot is a starting catalogue, not proof that the local Pulse Player project already implements any item.

| Reference | Source inspected | Current public description / version evidence | License status found in official material | Product implication |
|---|---|---|---|---|
| Namida | [Official GitHub repository](https://github.com/namidaco/namida), [official documentation](https://docs.namida.app/), [features page](https://docs.namida.app/features/) | Official docs displayed v8.5.0 and were updated 2026-10-27; repository README lists library/indexing, look and feel, sound, YouTube/streaming, system integration and other features. | Current repository states a custom EULA. Do **not** describe the current project as GPL-3.0. EULA restricts use to personal use and prohibits commercial use, redistribution/rebranding and use of code beyond personal use or contribution, except portions expressly marked under other licenses. | Treat feature descriptions as research inputs. Do not copy the application, code, assets, brand or exact distinctive screen designs into a redistributed Pulse Player build without written permission and legal review. |
| PixelPlayer | [Official GitHub repository](https://github.com/PixelPlayerHQ/PixelPlayer), [README](https://github.com/PixelPlayerHQ/PixelPlayer#readme), [CHANGELOG](https://github.com/PixelPlayerHQ/PixelPlayer/blob/master/CHANGELOG.md), [LICENSE](https://github.com/PixelPlayerHQ/PixelPlayer/blob/master/LICENSE) | README describes a Kotlin/Jetpack Compose Android music player. Public changelog/release material includes 0.7.0-beta dated 2026-05-25 and later-added Wear OS, AI and Jellyfin capabilities. | Current repository says Proprietary License. README states portions contributed before 2026-05-12 remain MIT; that does not make later code, the whole repository, UI assets or branding MIT. | Use as a technology and feature reference. Do not copy proprietary code, assets, brand or exact distinctive UI. Any proposed reuse of a specific pre-cutoff MIT contribution requires commit/file-level proof, notices and dependency review. |

### E1.1 Required license corrections

- Replace any obsolete statement that the current Namida repository is GPL-3.0 with the current custom-EULA finding above. Do not rely on historical license states.
- PixelPlayer's proprietary license states personal, non-commercial study/review/use and prohibits redistribution or derivative works without written permission; commercial use and app-store redistribution are specifically restricted. The README's pre-2026-05-12 MIT exception must be proven for every reused file and contribution; do not infer that the full codebase is MIT.
- Do not use either project's logos, app names as Pulse Player branding, screenshots, icon packs, artwork, wallpapers, fonts, proprietary assets or source code unless the exact material is demonstrably licensed for the intended distribution or permission is obtained.
- General functional ideas and broad interaction patterns can be independently implemented. Recreate them in Pulse Player's own component library, artwork, typography, shapes, color tokens, transitions and navigation. Do not reproduce a screen pixel-for-pixel or make the product confusingly similar in branding.
- This section is product/engineering guidance, not legal advice. For a commercial/public release, obtain a license review of the actual files, dependencies, assets and applicable platform/provider terms.

### E1.2 Required source audit

Google AI Studio must inspect, as available: repository README, root and nested LICENSE/NOTICE files, documentation pages, release changelog, settings catalogue, every screen/navigation destination, every feature implementation and platform gate, and all design/theme/layout registries. For each upstream project, record branch, commit SHA or release tag, inspection date, relevant source file/class or documentation URL, and whether the observed feature is stable, experimental, planned or platform-specific. A README bullet alone is not proof that a feature is implemented in the current branch.

Create or update `docs/spec/PARITY_INVENTORY.md` with one row per capability and a linked child row for each independently testable workflow. Use columns:

`ID | Source | Feature | Source evidence (URL/file/class) | What the user can do | Pulse screen/entry | Existing Pulse equivalent | Missing work | Status | Platform/license/provider gate | Settings destination | Test/evidence`

Do not collapse multiple distinct capabilities into a single row merely to reduce the number of rows.

## E2 --- Namida feature-parity baseline

The following catalogue reflects the official Namida README and official documentation categories visible in the 2026-11-03 snapshot. Expand it with every additional item found in the actual source, docs, settings and changelog. Each numbered item below must become its own row or child rows in `PARITY_INVENTORY.md`.

### E2.1 Library, scanning and metadata

- Folder-based library and multiple selected music roots; excluded folders and per-folder indexing behavior.
- Indexer controls, scan progress, refresh/rescan, minimum file-size and minimum-duration filters.
- Duplicate-track prevention and safe duplicate grouping; never delete files automatically.
- Track/album/folder/artist/genre browsing; artist and genre separators; multi-artist parsing and featured-artist extraction.
- Sort by supported track and album properties with ascending/descending order and stable tie-breaking.
- Tag editor with reliable metadata read/write, suggested existing tag values, validation, safe write confirmation and post-write rescan.
- Folder artwork such as `cover.jpg` and optional folder information text such as `.info.txt`, subject to safe file handling.
- Multi-selection across library/search/folder contexts and a unified selected-items action bar.

### E2.2 Pages, library organization and discovery

- Home, Tracks, Albums, Artists, Genres, Playlists, Folders and Queues.
- Most Played / Top tracks, Moods, Tags, Ratings, Languages, Statistics and any extra collection pages found in source.
- Configurable home/library section order, visibility and section-level actions.
- Track/album/artist/folder details and contextual navigation between related entities.
- Persistent queues and saved queue sessions.
- Search over title, artist, album, filename, lyrics and other supported metadata, with typo-tolerant matching, filters, sorting and recent-search behavior when present.
- Recommendations based on listening patterns, co-listening, release date, time range, rating, mood and random modes when supported by source.
- Lost Memories / historical rediscovery (tracks listened to around the same time in earlier years).

### E2.3 Playback, queue and player interaction

- Waveform seekbar with real track waveform data; do not fake a waveform when the pipeline has not generated one.
- Widescreen player layout; adaptive single/split layouts and all player variations found in source.
- Configurable track play mode when launching from search or a collection: selected track only, search results, album, first artist/genre or equivalent supported modes.
- Insert-after-latest-inserted queue behavior, correct ordering for consecutive insertions and queue persistence.
- Repeat N times in addition to repeat-off/one/all, plus independent shuffle toggle semantics.
- Swipe actions for tracks/videos and artwork tap/long-press gestures, all configurable and conflict-free.
- Long-press playback shortcuts and queue manipulation where supported by the source.
- Pause/stop scenarios and interruptions, integrated with the same playback owner rather than a second player.

### E2.4 Sound and audio output

- Parametric equalizer and AutoEQ profile import, with validation and supported-band constraints.
- Equalizer/effect presets and per-track speed, pitch, volume and equalizer profiles.
- Bass enhancement, reverb, virtual surround, 8D-style processing and vocal-removal effects, only when a real DSP implementation and capability gate exist.
- Gapless playback, play/pause fade, skip silence and configurable transitions.
- Smart crossfade with beat matching as an experimental, measurable capability; never promise beat-match on unsupported formats/devices.
- Speed and pitch control, semitone adjustments and 432 Hz tuning where actually supported.
- Loudness enhancer and ReplayGain normalization, with clipping protection and clear precedence between per-track and global settings.
- Output-device selection, mono audio, audio-path diagnostics and device capability explanation.
- Bit-perfect/USB DAC/direct/exclusive mode/32-bit float only when verified on the target Android API, device and audio route; unsupported devices must show why.

### E2.5 Lyrics, video, subtitles and external sources

- Plain, synchronized LRC and word-synchronized lyrics; local discovery, network provider adapters if enabled, cache, manual editing and line/word timing tools.
- Lyrics offset, scrolling, current-line emphasis, text size/alignment, translation and romanization when available.
- Video subtitles/captions with supported subtitle formats, selection, sync offset and fallback.
- Local music-linked videos and independent video library playback where implemented; safe filename/metadata matching.
- Video gestures for volume, brightness, seek, fullscreen and playback speed; preserve original aspect ratio and provide touch-accessible controls.
- YouTube/online video, audio-only/data-saver modes, profiles, feed, subscriptions, playlists, notifications and quality choices only behind explicit `full` flavor, consent and service/legal gates.
- SponsorBlock/segment/heatmap/dislike-related service integration only after legal/provider policy review and with a real implementation.
- Multi-library/remote sources such as Subsonic, Jellyfin, WebDAV and SMB; remote browse, authentication, reconnect, offline cache, cache priority and per-source removal semantics.
- Downloads, chapter splitting, sponsor-segment removal, file naming/tag templates and download queue only where terms and distribution policies allow.

### E2.6 Playlists, history, statistics and sync

- Static playlists, custom order/artwork, M3U/M3U8 import and export, auto-import rules and remote playlist import where available.
- Smart playlists with a rule builder, all/any logic, nested conditions if supported, sorting/limits and a preview of matched tracks.
- Flexible editable/importable listening history with a clear listen-count threshold based on seconds and/or percentage.
- Most-played ranges, listening charts, monthly statistics and a Year/Your Year recap based only on real play events.
- Importers for supported Spotify, ListenBrainz, YouTube history or other external history formats only when the input format is actually supported and consent is explicit.
- Cross-device app-data sync for supported collections (history, playlists, favorites, queues, lyrics, settings and related state), with non-destructive merge semantics and conflict handling.
- Listening Party synchronized queue/session with host/guest roles, local network and online variants only when a secure real implementation is available.
- Bookmarks and chapters for tracks/videos; add, rename, seek, reorder and delete with persistent storage.

### E2.7 System integration, widgets and automation

- Home-screen widget customization, size/responsive behavior and functional transport controls.
- Quick Settings tiles, Android Auto and Wear OS integration only for supported OS/device APIs and after actual emulator/device testing.
- Status-bar lyrics where technically allowed, notification/media-session actions and lock-screen controls.
- Now Playing broadcasts, scrobblers, Tasker/webhook/script integrations with explicit user controls and privacy descriptions.
- Desktop-only items (system tray, global hotkeys, keyboard shortcuts, floating lyrics window) must be classified `NOT-APPLICABLE-ANDROID` in the Android matrix, not shown as fake Android controls.

### E2.8 Visualizer, motion and customization

- Audio-reactive thumbnail animation, particles/edge breathing/miniplayer party effects and waveform/visualizer modes, driven by actual playback audio analysis without `RECORD_AUDIO` when the internal PCM/audio processor can provide the signal.
- Animated backgrounds, user-selectable wallpapers and artwork-driven dynamic colors with user-controlled intensity and reduced-motion alternatives.
- Theme and customization pages; expose every real customization option in a searchable, logically grouped settings destination.

## E3 --- PixelPlayer feature-parity baseline

The following catalogue reflects the official PixelPlayer README and public changelog/release material available in the 2026-11-03 snapshot. Expand it with new items discovered in the actual repository. PixelPlayer is the closer Android/Compose reference, but implementation details still must be checked against the user's existing project architecture.

### E3.1 Material and visual system

- Material You/dynamic wallpaper colors on supported Android versions with a manual fallback below the required API.
- Album-art-derived color scheme with caching, stable color sampling and contrast correction.
- Dark/light/automatic theme modes.
- Corner-radius/shape customization, navigation-bar presentation and adaptive navigation.
- Material 3 Expressive-style surfaces, expressive state changes, motion, transitions and micro-interactions recreated with Pulse Player's own token system.
- Accessible focus, touch targets, typography scaling, reduced-motion behavior and small-screen scaling.

### E3.2 Playback and library

- Multi-format playback (including MP3, FLAC, AAC, OGG, WAV and other formats supported by the actual decoder stack).
- Background playback and proper MediaSession integration.
- Queue drag/reorder, shuffle and repeat states, gapless playback and configurable crossfade transitions.
- Browse Songs, Albums, Artists, Genres and Folders with configurable multi-artist parsing and album-artist grouping.
- Folder filtering and full-text library search.
- Playlists, listening statistics and Daily Mix / personalized discovery.
- Tag editor with supported read/write formats and safe metadata write flow.

### E3.3 Lyrics, artwork and online metadata

- Synchronized lyrics lookup and display, manual editing and scrolling synchronization.
- Lyrics translation, text alignment and romanization only where a real provider/parser supports it.
- Artist image/artwork provider integration, cache lifecycle, memory LRU cache and database-backed cache with offline fallback.
- Honest fallback art when the provider is unavailable; never show a fake remote image or claim an image was fetched when it was not.

### E3.4 Connectivity, AI and new integrations

- Chromecast/Cast discovery, connection state, route selection and remote playback controls with clear disconnect/failure states.
- Android Auto, marked planned/unsupported unless the exact upstream implementation and Pulse Android Auto service are actually available and verified.
- Home-screen Glance widgets.
- AI playlist creation with provider setup for supported services (Gemini, DeepSeek, OpenAI or other providers only if actually implemented); user API credentials must be encrypted and never logged.
- Remote library integration such as Jellyfin and Navidrome/server album synchronization where supported by the inspected source.
- Wear OS transfer/local playback/queue synchronization/remote control if present in current source, gated by Wear project/module and device verification.
- Telegram or other messaging integrations if present in current changelog/source, gated by explicit account/token setup, privacy disclosure and provider stability.

## E4 --- Design integration without deleting Pulse Player's design

### E4.1 Separate design dimensions (never merge these into one picker)

Maintain independent registries and preferences for:

1. **App Theme Mode:** System, Light, Dark, AMOLED and time-based modes where supported.
2. **App Skin / Color Palette:** all existing Pulse skins plus any independently designed new palettes.
3. **Dynamic Color Source:** static brand palette, Android wallpaper colors, album-art colors or user-selected seed color.
4. **Player Theme:** all nine existing player themes (`classic`, `mountain`, `mesh`, `minimal`, `acoustic`, `vinyl`, `coast`, `starry`, `immersive`) plus new original variants. Never delete or rename an existing stable ID.
5. **Player Layout:** structure and placement of Now Playing content (Classic, compact, portrait, split, widescreen, Atmospheric Queue and additional verified layouts). This is not a player theme.
6. **Button Arrangement:** order, visibility, grouping and presentation of playback/action buttons. This is not a player layout.
7. **Player Elements:** optional content blocks such as waveform, lyrics preview, queue preview, visualizer, timer chip and route/status labels.
8. **Navigation Style:** bottom navigation, top tabs, rail, floating navigation or other supported styles.
9. **Home/Library Section Layout:** reorderable, hideable and addable library sections/cards/collections.
10. **Motion and Gestures:** animation intensity, reduced motion and configurable action gestures.

Changing one axis must not unexpectedly reset another. Store each preference independently and migrate existing values without loss.

### E4.2 Preserve all existing visual catalogue items

Before adding anything, read the actual project registries and record all existing entries. At minimum, this master spec already identifies these skin palettes: Plum, Aurora, Afterglow, Ocean, Sunrise, Forest, Mono and Pure Black. It also identifies the nine player themes listed above. The real project may contain more; the repository wins if it has additional working options. Keep all of them.

New independently authored visual options may include Material You/Tonal, Forest Mist, artwork-driven, expressive surfaces, high-contrast, minimal, glass-like or ambient/animated variants. Add only after the existing design is captured in screenshots and registry tests. These are Pulse Player designs inspired by broad concepts, not imported PixelPlayer/Namida theme files.

### E4.3 Design quality requirements

- Use one coherent Pulse Player design system: color tokens, semantic surface roles, typography scale, spacing scale, shape scale, elevation, focus/pressed/disabled states, icon sizing and motion tokens.
- Artwork-driven colors must be sampled from real local art, cached, adjusted for contrast and fall back to the selected skin when artwork is missing or extraction fails.
- Material You dynamic colors must be OS-gated and previewed before application. Do not make dynamic colors a dependency for the offline build.
- Every theme/skin preview must render actual reusable components, not only a colored card with a title. Preview Home, library rows/cards, mini-player, Now Playing, queue, dialogs and Settings.
- Each player theme/layout must keep its essential information and controls; compact modes may relocate secondary actions into a clearly labeled overflow but must not make actions unreachable.
- Support portrait, landscape, split-screen, fold/unfold, small widths, large font scales and TalkBack.
- Provide reduced-motion equivalents for animated wallpapers, particles, marquee and transitions.
- A theme change must never change playback, queue order, scan state or backup state.
- A layout change must never stop playback or lose seek position.

## E5 --- Complete in-player Customize experience

This is a first-class feature, not a hidden developer option. The user must be able to personalize the player while a track is playing and immediately see the result. It combines the existing 15 presets with a full editor for buttons and content elements.

### E5.1 Entry points and navigation

Provide one clearly visible **Customize** control in Now Playing (icon plus accessible label/tooltip), a player overflow item, and Settings → Appearance & Layouts → Customize Player. All entry points open the same customization model and preserve the current playback session.

The adaptive editor has these tabs/sections:

- **Layouts** — choose the overall Now Playing structure and preview portrait/landscape variants.
- **Player Themes** — choose the visual style of the artwork/background section; includes all nine original themes.
- **Buttons** — choose one of the 15 presets or open the custom rearrangement editor.
- **Elements** — add, remove, show/hide, reorder and configure player content blocks.
- **Colors & Skins** — select app skin, theme mode and dynamic color source without confusing these with player themes.
- **Home & Library** — customize home sections, page order, visible library destinations, card density and list/grid presentation.

On narrow phones use a full-height bottom sheet or dedicated screen with scrollable tabs; on tablets/foldables use a two-pane editor with catalog left and live preview right. Maintain back navigation and a persistent current-track mini preview.

### E5.2 Live preview and edit safety

- Preview the current real Now Playing state when safe; otherwise use an explicitly labelled local preview model that is never written to history or the real queue.
- Tapping a control inside edit preview must not trigger playback; use drag handles and explicit edit-mode semantics.
- Preview must update immediately as the user changes the layout, buttons, elements, skin or dimensions.
- Keep the currently playing audio active, do not re-create the player engine, and do not reset playback position.
- Provide Undo, Redo where practical, Reset this tab, Reset all customization and Apply/Save semantics. Destructive reset requires confirmation.
- Provide a before/after comparison for recommendations and a clear Cancel action.
- Persist successful changes immediately or on explicit Apply according to the editor's documented transaction; never display an unsaved state as saved.

### E5.3 Button catalog and add-button workflow

Retain all 15 stable button presets from Annex A. Add a visible **Add button** action to the custom editor. The user chooses from a grouped catalog of built-in actions; adding a control creates a real connection to the actual app action, not a decorative element.

Catalog groups must include:

- **Core transport:** Previous, Play/Pause, Next, Shuffle, Repeat.
- **Progress/navigation:** Seek/waveform seekbar, elapsed time, total duration, chapter/bookmark navigation when applicable. The seekbar is a structural control, not an ordinary icon button.
- **Library actions:** Favorite, Add to Playlist, Track Details, Open Album, Open Artist, Open Folder, Share.
- **Playback tools:** Queue, Lyrics, Equalizer, Sound Effects, Speed, Pitch, Repeat N, Track Play Mode, Sleep Timer, Stop After Current.
- **Output/connectivity:** Output Device, Cast, Android Auto, LAN/Listening Party controls only when the relevant capability is implemented and enabled.
- **Visual controls:** Visualizer, Artwork/Background mode, Lyrics display mode, Player Layout switcher and Customize.

Each catalog item displays icon, name, one-line description, capability state and whether it is already placed. If unavailable, show the reason and prerequisite; do not let the user add an item that appears functional when its capability is absent.

When the user selects **Add button**, show a placement preview with valid drop zones and options to add to the primary row, secondary row, side rail, quick-action strip or overflow (only where the selected layout supports that slot). Confirm the button appears in the live preview and performs the documented action in normal mode.

### E5.4 Rearrange, show/hide and group buttons

- Drag buttons using a visible handle; support touch, mouse/trackpad and keyboard-accessible Move earlier/Move later controls.
- Reorder within a group and move between compatible groups/slots.
- Show insertion indicators, valid/invalid drop feedback, auto-scroll in long catalogs and haptic feedback if enabled.
- Hide optional buttons and restore them from the catalog. Hidden actions remain discoverable in the editor and, where appropriate, the overflow menu.
- Allow icon-only or icon-plus-label display, allowed size variants, button spacing, row/column placement and alignment. Do not reduce touch targets below 48dp.
- Preserve stable action IDs, not localized button labels, in saved configurations.
- Keep Previous, Play/Pause, Next, Seek and the minimum favorite/queue access reachable according to the player's essential-control policy. A user may move an essential control to a compatible always-reachable position, but may not make the app impossible to operate.
- Unsupported actions must be absent from active slots or visibly disabled with a reason; never leave a dead hit target.

### E5.5 Player Elements catalog and add-element workflow

Add a separate **Add element** workflow; do not treat content elements as buttons. The catalog includes, when supported by the chosen layout:

- Artwork/cover image; title; artist; album; album artist; release year; genre; file/format/bitrate detail.
- Waveform seekbar; standard progress slider; elapsed/remaining/total time.
- Lyrics preview/current line; lyric translation/romanization indicator.
- Up Next / queue preview; next-track card; queue count.
- Visualizer/particles/animated background/ambient artwork effect.
- Favorite status; rating; mood/tag; bookmark/chapter information.
- Sleep-timer countdown; playback speed/pitch; audio output route; equalizer/effect status; shuffle/repeat status.
- Connection state for Cast, Android Auto, remote source or Listening Party only when supported.

Each element has an actual data source and a defined unavailable/empty/loading state. The user may add an element, remove it, hide it, reorder it, choose an allowed size/density, alignment and style, or restore defaults. Elements are not allowed to invent data: missing lyrics must show the real empty state, unavailable output route must be disabled, and a waveform must be marked loading/unavailable until generated.

### E5.6 Preset and custom-template management

- Keep all existing 15 presets with stable IDs and distinct accurate previews.
- Provide purpose labels such as Centered, Minimal, One-Handed, Queue-Focused, Compact, Wide or Accessibility only where the actual arrangement matches the label.
- Allow users to apply a preset, duplicate it as a custom preset, rename, edit, save, reset, export and import custom presets if that export/import capability is implemented.
- Store a custom layout by stable button/element IDs, group/slot, order, visibility and allowed style options.
- Validate imported preset schema, unknown IDs, incompatible slots, duplicate IDs and mandatory controls. Migrate unknown actions safely; never crash or silently drop the whole customization.
- Permit per-layout button/element overrides, so a widescreen layout can have a different arrangement than portrait, while retaining a global fallback.
- Provide Reset to current preset and Reset to app default as distinct actions.

### E5.7 Recommendations

Provide an optional **Recommend layout** action. User chooses a goal such as Minimal, One-Handed, Queue-Focused, Large Controls/Accessibility or Information-Rich; the engine selects from existing presets or recommends a custom arrangement based on available screen width/orientation and the selected goal. If on-device usage signals are used, they must remain local, transparent and opt-in. Show a before/after preview and require Apply. Never silently rearrange the player. Recommendations cannot hide essential controls, violate minimum touch sizes or select unavailable integrations.

### E5.8 Home and library organization

Add an **Edit Home** and **Organize Library** experience inspired by reorderable library section concepts, implemented with Pulse Player's own UI:

- Add/remove/hide/reorder supported home sections and shortcuts.
- Reorder visible library tabs/pages without deleting their underlying data.
- Add collection shortcuts such as Recently Added, Most Played, Recently Played, Moods, Ratings, Tags, Folders, Genres, Artists, Albums and Playlists where the data page exists.
- Configure list/grid mode, grid columns, row density, artwork size, section count/limit and sort order.
- Provide drag handle plus accessible Move up/Move down controls, live preview, undo and reset.
- Preserve required navigation entries and ensure hidden pages remain reachable from the Library/More destination.
- Do not invent an empty page as if a feature exists; a page can only be added after its actual data/query/UI workflow is implemented.

## E6 --- Settings information architecture and organization

Settings must be coherent and searchable rather than an unstructured dump of toggles. Use these top-level groups, preserving every existing setting and adding every new feature setting under the correct owner:

| Settings group | Required contents and subsections |
|---|---|
| Appearance & Layouts | App theme mode, skins, dynamic colors, seed color, corner scale, navigation style, home sections, library organization, player themes, player layouts, button arrangements, player elements, artwork/backgrounds, wallpapers, visualizers, animation/reduced motion, density and typography. |
| Playback | Start/resume behavior, queue, shuffle/repeat, track play mode, transitions, gapless, crossfade, silence skip, fade, speed/pitch, per-track profiles, audio focus, interruptions and playback gestures. |
| Sound & Output | Equalizer, AutoEQ, effect presets, individual effects, ReplayGain/loudness, output device, mono, audio path, device capability and audio diagnostics. |
| Library & Indexer | Music folders, scan schedule, refresh, excluded folders, file size/duration limits, duplicate handling, sorting, formats, tag editor and folder artwork. |
| Search & Discovery | Search fields/scopes, typo tolerance, recent searches, recommendations, Daily Mix, smart playlists and discovery pages. |
| Lyrics & Video | Lyrics providers/cache/format, editor, sync offset, display, translations, subtitles and optional local/online video. |
| Playlists, History & Stats | Static/smart playlists, imports/exports, queue sessions, history threshold, history import, statistics, monthly/yearly recap and bookmarks. |
| Backup & Transfer | Classic favourites backup, backup folder, audio-copy status, import/restore preview, Full backup (reference format), M3U/history transfer and diagnostics. |
| Integrations | Cast, Android Auto, Wear OS, widgets, Quick Settings, remote libraries, LAN sync, Listening Party, AI providers, scrobblers and automation; each with capability and privacy status. |
| Permissions & Privacy | Granted/denied permissions, SAF grants, online master switch, per-service switches, local data controls, diagnostics/export and privacy policy. |
| Accessibility | Font scaling, contrast, reduced motion, button sizes, TalkBack labels, haptics, gesture alternatives and screen-reader order. |
| About & Diagnostics | Version/build flavor, changelog, licenses/notices, help, logs, feature status matrix, decoder/provider information and report-export action. |

Every setting row must include a clear title, short explanation, current value/state, supported range/options, persistence rule, reset behavior and any OS/device/provider/legal prerequisite. A disabled row states why and how to enable it, if possible. Use grouped subpages, search and deep links; do not put every control on one enormous page.

## E7 --- Definitive Classic Backup v2 product contract

### E7.1 User-facing outcome

Classic Backup v2 creates one portable folder at:

`Music/Pulse Player/`

That folder contains:

- One or more timestamped `.json` favourites files using the exact `pulseplayer.favorites` **v1** format.
- Separate byte-for-byte copies of all local favourite audio files that Pulse Player can read and that the user confirms copying.

The JSON file is metadata and matching information; it does **not** contain embedded audio. The folder as a whole is the backup set. The original source audio is never moved, modified or deleted.

### E7.2 Exact JSON compatibility contract

- Keep `schema` equal to `pulseplayer.favorites`.
- Keep `schemaVersion` equal to integer `1`.
- Keep the exact existing v1 top-level keys and item keys from File 07: `app`, `exportedAt`, `count`, `items`; per item `title`, `artist`, `album`, `durationMs`, `displayName`, `mimeType`, `sizeBytes`, `addedAtMs`, `normTitle`, `normArtist`, `normAlbum`.
- Keep the existing UTF-8 JSON structure, decoder tolerance, `MAX_BYTES`, `MAX_ITEMS`, string limits and normalization behavior defined in File 07/the current codec. Do not add a v2 key, change field types, rename keys or change the schema version.
- The existing `displayName` field may contain the final audio-copy filename generated by the backup. This uses an already existing v1 field and enables the existing strong match tier (filename + size + near duration); it is not a schema change.
- The existing `sizeBytes`, `durationMs`, MIME type and normalized title/artist/album fields retain their established meaning. The audio copy is byte-for-byte, so source and copy size remain equal.
- Include a JSON item for **every favourite** in the database, including items whose audio cannot be copied. For successfully copied items, `displayName` identifies the final copied filename. For uncopied items, retain the original display name and metadata, report the audio-copy limitation, and allow the matcher to find a corresponding file already present elsewhere on the destination device.
- Do not insert absolute paths, storage URI values, source-device IDs or new copy-status fields into the v1 JSON. Store export diagnostics in the app's local result/report state, not in the interchange schema.

### E7.3 Export screen and workflow

Entry: Settings → Backup & Transfer → Classic favourites → **Create/Export backup**. Also allow the action from the Favourites page overflow if the app already uses that entry pattern.

1. Read the actual favourites table in stable order and capture the `addedAtMs` value for each favourite. If there are no favourites, show the empty-state explanation and do not create an empty fake success.
2. Resolve each favourite to its best available local audio source. Distinguish readable local files, missing/unavailable files, revoked access, SD/removable storage, remote server items and online/stream-only favourites. Do not assume every favourite has a local file.
3. Show a preflight screen with favourite count, local files available to copy, files not copyable with reason counts, estimated total bytes, free destination space, existing folder contents and expected resulting backup size. State clearly: "Your originals will stay where they are. Pulse Player will create separate copies in Music/Pulse Player."
4. Require explicit confirmation before copying audio. Offer **Create full backup**, **Choose favourites**, and **JSON only** as clear options. JSON-only is a deliberate fallback and must be labeled as metadata-only; it must not be described as a complete music-file backup.
5. Ensure/create the public folder `Music/Pulse Player/`. Use MediaStore `RELATIVE_PATH` on Android 10+ for app-created public media. Use SAF/appropriate scoped access on older Android versions; do not request `MANAGE_EXTERNAL_STORAGE`. Ask for only the permissions/access required for the operation. Reuse an existing folder without clearing it.
6. Check free space before starting. Require at least the estimated copy size plus a 10% safety margin and honor the existing database-volume safety threshold from the project. If space is insufficient, stop before copying and offer fewer selected files or JSON-only export.
7. Copy each supported favourite audio file byte-for-byte using streaming I/O. Preserve the original file extension and raw encoded content. Do not transcode, normalize, re-encode, rewrite tags, change timestamps unnecessarily, rename the original or move/delete the source. Sanitize the destination filename to prevent path traversal and invalid names.
8. Use unique destination names. If the file already exists and can be verified as the same audio (compare available identity metadata plus a streaming digest), reuse it and record `reused`; if it is a different file with the same name, create a collision-safe name such as `filename (1).ext`. Never silently overwrite an existing backup file.
9. Write copies to temporary names such as `.part`; verify the copied byte count and available content digest; only then publish the final filename. If Android MediaStore is used, use its pending/publish lifecycle where applicable. A half-written file must never look like a complete audio file.
10. Once final filenames are known, write a timestamped JSON snapshot using the exact v1 codec. For successfully copied items, set the existing `displayName` field to the destination filename. Write to a temporary JSON file, reopen and decode it, validate `count` against `items`, then publish the `.json` file atomically. JSON export is last so filenames match the copied media.
11. Verify each copied media item exists, has the expected byte count and can be reopened; verify the JSON can be re-read and decoded. Do not report success before all committed operations finish.
12. Show a result page with counts for: favourites represented in JSON, copied, reused, skipped/not copyable, failed, JSON file name/location, total bytes, and elapsed time. Provide Retry failed, View skipped items, Open folder, Share JSON, Share backup folder contents and Done actions where supported.
13. Keep previous timestamped backups unless the user explicitly deletes them. Provide a cleanup action with a preview and confirmation; never clear the folder automatically on a new export.

### E7.4 Destination-device import workflow

1. On the new device, the user installs Pulse Player, creates/opens `Music/Pulse Player/` if needed, and copies **both the music files and a valid JSON file** into that folder using USB/MTP, SD card, a file manager or another transfer method. The user should copy the folder contents together, not just the JSON, when they want the audio archive.
2. Settings → Backup & Transfer → Classic favourites → **Import backup** explains the three steps: (a) put the `.json` and music files in `Music/Pulse Player/`; (b) grant folder access/scan; (c) review and apply favourites.
3. Request access via SAF `ACTION_OPEN_DOCUMENT_TREE` or a document picker. Use an initial URI hint for `Music` when supported, but do not assume Android's provider will honor it. Persist the read grant only after user selection. No broad file permission is required.
4. Scan audio in `Music/Pulse Player/` through MediaStore or the selected SAF tree and refresh the library. JSON discovery uses SAF, not an assumption that MediaStore can query arbitrary JSON files. Find timestamped `PulsePlayer_Favorites_*.json` snapshots and any valid v1 JSON file in the selected folder. If several are present, default to the newest valid `exportedAt`/filename but let the user choose another.
5. If the folder contains no JSON, display a precise empty state with a Choose JSON file action and a short instruction to copy the JSON. Also keep backward compatibility with older Classic v1 JSON-only backups selected from any user-chosen location: they must still decode and use the existing matcher even though they do not include audio copies. If the selected folder contains JSON but no audio files, state that the favourites list can still be imported by matching to music already on the device, but the folder does not currently contain audio copies.
6. Decode with the existing v1 codec and run the original three-level fuzzy matcher without changing its established thresholds: strong = filename + size + duration within 2 seconds; high = normalized title + artist + duration within 2 seconds; medium = normalized title + album + duration within 5 seconds, only when stronger tiers did not yield a unique match. Title-only matching is never accepted. Multiple candidates remain Ambiguous and require user choice.
7. Make an import preview with: will be added, already favourite, ambiguous/needs user choice, not found, duplicate JSON records, invalid/skipped records and copied-file-found status. Show matched track name, artist, album, duration, destination path/folder label and confidence level so the user can confirm the right file.
8. Merge is the default; Replace is available only with an explicit destructive confirmation and must clear only favourites, not audio files, playlists, history or other app data. Apply atomically and idempotently. A repeated import must not create duplicate favourites.
9. The result page reports each category and provides filters for Not found/Ambiguous/Already favourite. For not-found rows show: "Copy the missing music files into Music/Pulse Player, rescan, and preview the import again." Never claim that a music file was restored just because its favorite metadata was imported.

### E7.5 Independence, library scanning and duplicate behavior

- A copied audio file in `Music/Pulse Player/` must remain playable when the original source is deleted, moved, the SD card is removed or the original folder becomes unavailable. Verify this with a real test: export, confirm the copy exists, remove/unmount the original source in a test environment, rescan and play the copy.
- The scanner must include `Music/Pulse Player/` as a valid music location; do not exclude it by default.
- If the original source and backup copy both exist, group them as one logical track when a reliable identity match is available. Do not display two indistinguishable tracks, count two plays/favorites or create duplicate playlist entries. Keep both file URIs as source candidates. Prefer the user's original active source when available if that is the established source-selection policy; automatically fall back to the `Music/Pulse Player/` copy when the original becomes unavailable. Never delete either file during duplicate grouping.
- Expose a track detail indicator such as **Backup copy available** with an action to open/reveal the copy location. This must use real indexed source metadata.
- If identity is uncertain, do not silently merge unrelated recordings. Present duplicate review or keep separate logical items.
- Import must not require the original device's file paths. Matching is path-independent and relies on the v1 metadata, final copied filename, size/duration and the existing fuzzy matcher.

### E7.6 Permissions and Android storage compatibility

- Android 10+ app-created media uses MediaStore and `RELATIVE_PATH = Music/Pulse Player/` where appropriate. The app does not need broad storage access to create media it owns.
- Importing a user-created/copied JSON file requires SAF document/tree access. A MediaStore audio query does not grant general access to JSON.
- For older Android versions, use the narrowest supported SAF or legacy storage path and explain any runtime permission at the moment the user invokes export; do not front-load broad permission requests.
- Persist SAF read access only when supported and selected by the user; handle revoked grants and missing removable storage with clear recovery.
- No `MANAGE_EXTERNAL_STORAGE`. Do not access arbitrary private app folders or silently scan unrelated directories.

### E7.7 Error and recovery cases

At minimum implement explicit message + recovery + test for: no favourites; no local files; missing source; unreadable file; revoked URI/SAF access; SD card removed; insufficient space; folder creation denied; duplicate filenames; same file already present; corrupt/oversized JSON; unsupported schema; multiple snapshots; JSON absent; audio absent; some items not copied; copy interrupted/app killed; JSON write failure after media copy; process killed during import apply; ambiguous fuzzy match; repeated import; storage permission revoked; user cancels; destination read-only.

Recovery rules:

- On interrupted export, detect `.part` items and offer Resume or Clean up. Never use a partial file as a match candidate.
- If audio copies succeeded but JSON write failed, preserve the verified copies, explain that the metadata file was not completed, and offer Retry JSON generation; do not delete successful copies automatically.
- If JSON succeeded but some files failed, preserve the JSON record for every favourite and show an explicit partial-backup warning with retry controls.
- If the user cancels, stop new work, finish/close the current I/O safely, and ask whether to keep verified copies or remove only files created by this incomplete operation. Never remove pre-existing files.
- If import apply fails, roll back the database transaction and keep the user's files/JSON untouched.

### E7.8 Required Classic backup tests

Unit tests: exact old v1 JSON fixture compatibility (including JSON-only backups created before v2); exact top-level and per-item keys; schema remains version 1; all favourites represented; copied `displayName` maps to final filenames; unchanged fuzzy matcher thresholds; filename collisions; same-copy reuse; ambiguous match never auto-applies; missing files; unsupported/remote favorites; safe filenames; JSON size/item limits; no new schema fields; idempotent Merge; Replace clears favorites only; transactional rollback.

Instrumented/device tests: create folder on supported APIs; export multiple real audio formats; verify source bytes unchanged; verify byte-for-byte copies; run out-of-space and permission-denied simulations; kill/resume during copy; copy backup folder through a test transfer path to a second device/emulator; import from SAF tree; rescan and play a copied track after its original path is made unavailable; verify duplicate grouping; verify that the Full backup reference format remains separate and unchanged.

Acceptance: Classic Backup v2 is complete only when the JSON and audio copies are both real files in `Music/Pulse Player/`, the JSON remains v1-compatible, a new device can import using that folder, fuzzy matching remains safe, and copied audio remains playable independently of the original source.

## E8 --- Required architecture/components for the complete customization system

Reuse actual existing equivalents where possible. The feature should have distinct models and owners for:

- `PlayerCustomizationState`: selected theme/layout/skin/button preset/element preset and per-layout overrides.
- `PlayerButtonCatalog`: built-in action IDs, grouping, capability requirements, default labels/icons and execution routes.
- `PlayerElementCatalog`: element IDs, data bindings, supported layouts, size/alignment options and empty/loading behavior.
- `ButtonArrangementEditorState`: placed, hidden, available, dragged and selected items; undo/redo and validation errors.
- `PlayerLayoutRegistry`, `PlayerThemeRegistry`, `SkinRegistry`, `ButtonPresetRegistry`, `PlayerElementRegistry`, `NavigationStyleRegistry` and `HomeSectionRegistry`.
- `CustomizationRepository`: stable-ID persistence, migrations, per-layout overrides, preset management and import/export validation.
- `PlayerCustomizeRoute` and reusable Compose components for editor tabs, live preview canvas, button catalog, element catalog, drag handles, drop zones, option panels, recommendation preview and reset confirmations.
- `ClassicBackupManager`: folder resolver, media copy worker, v1 JSON codec adapter, SAF access manager, fuzzy matcher, backup preview/apply, integrity verifier, progress reporter, cancellation/resume and partial-operation journal.

These are responsibilities, not instructions to duplicate classes if the project already has an equivalent. Keep business logic outside Composables. Use lifecycle-aware state, stable keys, background I/O, structured cancellation and typed errors.

## E9 --- New traceability IDs and acceptance

Add at least these IDs to `docs/spec/FEATURE_COMPLETENESS_MATRIX.md` and `docs/spec/PARITY_INVENTORY.md`; create a child row for every sub-feature listed in E2/E3:

| ID group | Required coverage |
|---|---|
| `NAM-EXT-LIB-*` | All Namida library/indexer/folder/tagging/duplicate/sort/multi-select capabilities. |
| `NAM-EXT-UI-*` | All pages, home/library organization, visualizers, wallpapers, gesture and player-layout options. |
| `NAM-EXT-AUDIO-*` | Every equalizer, effect, output, ReplayGain, speed/pitch, gapless/crossfade and audio-path capability. |
| `NAM-EXT-MEDIA-*` | Lyrics, local video, subtitles, YouTube, downloads, cache, remote sources and provider gates. |
| `NAM-EXT-DATA-*` | Playlists, history, stats, recaps, smart playlists, search, bookmarks, queues, importers, sync and Listening Party. |
| `NAM-EXT-SYSTEM-*` | Widgets, Quick Settings, Auto, Wear, status lyrics, broadcasts, Tasker, scrobblers and platform-specific exclusions. |
| `PIX-EXT-UI-*` | Dynamic colors, theme modes, shapes, navigation, animation, expressive components and responsive behavior. |
| `PIX-EXT-PLAY-*` | Formats, MediaSession, background playback, queue, shuffle/repeat, gapless and crossfade. |
| `PIX-EXT-LIB-*` | Library pages, multi-artist parsing, album-artist grouping, folder filtering and search. |
| `PIX-EXT-DISC-*` | Daily Mix/AI discovery, playlists and statistics. |
| `PIX-EXT-LYRICS-*` | Sync, edit, translate, alignment and romanization. |
| `PIX-EXT-ART-*` | Artist artwork provider, caches and fallbacks. |
| `PIX-EXT-INT-*` | Cast, Auto, widgets, TagLib editor, AI providers, Jellyfin/Navidrome, Wear and messaging integrations. |
| `CUSTOMIZE-*` | Layout/theme/skin/button/element tabs, Add button, Add element, button drag/reorder/hide/restore, layout recommendations, home section organization, preview, persistence, migration and reset. |
| `CLASSIC-V2-*` | Folder creation, exact v1 JSON, copied audio, verification, SAF import, fuzzy matching, recovery, source independence and duplicate handling. |

Before release, the report must list the total upstream rows inspected, implemented, partial, gated, unsupported and excluded. Never report 100% parity merely because all current prompt bullets are represented; prove each row with actual app behavior and tests.

## E10 --- Google AI Studio execution order for this annex

1. **Audit only:** inspect actual Pulse Player source, current upstream repositories and project license files. Produce preservation matrix, upstream parity inventory, design registry screenshots and license notice. Do not make code changes in this stage.
2. **Correct source/spec conflicts:** fix obsolete license claims, identify any Classic backup code that still follows JSON-only behavior and produce a data migration plan without changing the v1 JSON contract.
3. **Classic Backup v2:** implement folder creation, byte copies, v1 JSON, progress/cancel/resume, SAF import, matching preview, atomic apply and all required tests. Verify original audio is untouched and the copied file plays when its old source disappears.
4. **Customization model:** define registries, stable IDs, state model, persistence, per-layout overrides and migration tests. Keep the nine existing themes, existing skins and 15 presets.
5. **Customize UI:** build editor tabs, button catalog, Add button, drag/reorder/hide/restore, element catalog/Add element, home/library organizer, live preview and recommendation flow. Wire each item to the actual domain action/data source.
6. **Parity implementation:** implement upstream feature rows in dependency order, one end-to-end vertical slice at a time. Keep offline and full flavors/gates as already specified.
7. **Visual integration:** apply original Pulse Player design concepts to the existing screen architecture and all new pages; verify Light/Dark/AMOLED, dynamic colors, responsive layouts and reduced motion.
8. **Release audit:** run every feature row and every control, attach screenshots/build/test evidence and report all GATED/UNSUPPORTED/EXCLUDED items. Never say a feature was copied or implemented based only on a visible menu entry.

**Final owner promise:** Pulse Player must become a complete, organized, customizable music player that preserves its original identity and data while adding the approved functionality found in Namida and PixelPlayer. Every button, element, screen, layout, setting, backup operation and integration must perform a real, verified task.
