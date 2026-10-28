# Google AI Studio Implementation Prompt

## Add a New Archive-Based Backup & Restore System Without Removing the Existing System

**Project:** Music Player -- MP3 Player (native Android app)\
**Implementation target:** Existing project; do not rebuild it from
scratch\
**Primary instruction:** Add a second, fully working backup-and-restore
method modeled on the supplied reference archive, while preserving and
continuing to support the app's existing/old backup-and-restore method.

------------------------------------------------------------------------

# 1. Mandatory Instructions for Gemini

You are working inside an existing native Android offline music player
project. Before editing code:

1.  Inspect the entire project structure and read the existing project
    specification, `brain.md` (if present), current backup/restore
    implementation, data models, persistence layer, storage-access code,
    playlist/favorites logic, settings, and tests.
2.  Identify the current/old backup format, its file extension, schema,
    UI entry points, compatibility behavior, and all data it currently
    saves/restores.
3.  Do not remove, replace, rename incompatibly, or silently change the
    old backup/restore method.
4.  Implement a **second backup mode**: a portable, structured archive
    backup inspired by the supplied sample file.
5.  Keep both modes accessible in the UI, with distinct labels and clear
    explanations.
6.  Reuse existing architecture and established dependencies where
    practical. Do not add a dependency unless it is needed and
    compatible with the project.
7.  Never pretend an operation succeeded. Report success only after the
    archive has been written, verified, and closed; report restore
    success only after validation and the restore transaction have
    completed.
8.  No dummy data, placeholder buttons, simulated progress, fake success
    messages, or decorative-only controls.
9.  Compile and test in manageable stages. Fix errors introduced by your
    changes before proceeding.
10. Update `brain.md`, the project documentation, and tests to reflect
    the final implementation.

**Do not implement any Private Vault behavior.** This is a separate
local offline music-player project. Do not add hidden vaults, decoy
modes, vault authentication, or unrelated carrier features.

------------------------------------------------------------------------

# 2. Reference File Analysis --- Follow the Actual Sample

The provided file is named `ManualBackup_20261028_172204175.wav`.
Inspection shows that its contents are **a ZIP archive, not WAV audio**,
despite the filename extension. It contains entries resembling the
following:

``` text
settings.bak
metadata.inf
MusicTagsBackupConfig.conf
Covers/
  MusicCoversBackupConfig.conf
  AlbumCoversBackupConfig.conf
Lyrics/
  LyricsBackupConfig.conf
PlayLists/
  PlayListBackupConfig.conf
SortBackupConfig.conf
ScanHiddenBackupConfig.conf
Statistics/
  StatisticsSnapshotConfig.json
  PlaybackRecords.json
  snapshot_monthly_YYYYMM.db
```

The archive's `metadata.inf` uses a structure similar to:

``` json
{
  "backupTime": "20261028_172204175",
  "mode": "MANUAL_GLOBAL",
  "type": "GLOBAL",
  "isAutoBackup": false,
  "deviceId": "device-specific-id",
  "versionName": "4.4.1.189",
  "versionCode": 189,
  "settings": {
    "isBackupMusicTags": true,
    "isBackupCovers": true,
    "isBackupLyrics": true,
    "isBackupPlaylists": true,
    "isBackupSortSettings": true,
    "isBackupScanHideSettings": true,
    "isBackupMusicRecap": true
  },
  "songs": 13509,
  "playlists": 41
}
```

The sample's `settings.bak` contains category flags and version/device
information. Other files hold music tag records, track-cover references,
album-cover references, playlist names/sorting/track paths, lyrics
backup data, sort preferences, scan/exclusion settings, playback
records, and monthly statistics snapshots.

The sample contains absolute paths such as `/storage/emulated/0/...` and
device-specific `content://` album-art references. **These paths and
identifiers must not be trusted as portable identifiers on another
device.** They may be retained as optional historical hints, but restore
must reconcile tracks against the target device's current media library.

The archive has no audio payload evident from the listed entries. Treat
this reference as a **settings/library-metadata backup**, not as a
package containing the user's actual MP3/M4A/FLAC/etc. files. Never
claim that restoring the archive recreates missing music files.

## Reference compatibility requirements

-   Inspect archive contents by reading the file signature/content, not
    only the extension.
-   The supplied `.wav`-named sample must be recognized as a ZIP archive
    if the user selects it. Do not try to decode it as audio.
-   New backups should use a truthful extension such as `.musicbackup`
    (recommended) or `.zip`, with the chosen format clearly documented.
-   The importer may accept `.musicbackup`, `.zip`, and ZIP archives
    with a misleading extension when content sniffing confirms that they
    are valid archives. Do not accept arbitrary WAV audio as a backup.
-   Do not claim exact third-party application compatibility. The sample
    is a reference for structure and behavior; this app has its own
    schema. If compatibility with the source application is explicitly
    required, first determine whether the source schemas and semantics
    can be mapped reliably. Never silently reinterpret unknown fields.

------------------------------------------------------------------------

# 3. Required User Experience

In Settings, create a **Backup & Restore** section with two distinct
choices. Use labels appropriate to the existing UI design.

## Option A --- Existing Backup (Legacy)

-   Preserve the current option's name, behavior, format, and supported
    data as far as possible.
-   Existing backup files must remain restorable.
-   Existing users must not need to recreate their backups.
-   Do not automatically convert or delete legacy files.
-   Add a brief "Legacy / Existing format" description if needed to
    distinguish it.
-   If the current method already supports a particular data category,
    do not remove that capability.

## Option B --- Portable Archive Backup (New)

Description: "Create one portable backup file containing the selected
music-player data and settings. Your actual music files are not included
unless the existing app explicitly supports and the user explicitly
chooses a separate media-copy feature."

Provide these real actions:

-   **Create Archive Backup**
-   **Restore from Archive**
-   **Inspect Backup** (optional only if it can be implemented fully)
-   **Backup contents / categories**
-   **Last backup details**, sourced from a real completed operation
-   Clear progress, cancellation where safe, error details, and
    completion status

Both options must remain independently usable. Avoid confusing duplicate
buttons without explaining the difference.

## User-facing explanations

Before backup, explain which categories will be saved and whether audio
files themselves are excluded.

Before restore, show a preview of archive version, backup timestamp,
included categories, approximate record counts, and any validation
warnings.

Before applying a restore that replaces or overwrites existing
information, clearly explain the impact and require confirmation.

Use the app's established UI components, styling, navigation, and
accessibility conventions. Provide loading, empty, error, cancellation,
and success states. Do not use fake progress.

------------------------------------------------------------------------

# 4. Archive Format Specification

Implement a documented, versioned archive format. Use a stable internal
layout similar to this:

``` text
<timestamp>.musicbackup
├── manifest.json
├── metadata/
│   ├── music_tags.json
│   ├── track_covers.json
│   ├── album_covers.json
│   ├── lyrics.json
│   ├── playlists.json
│   ├── sort_settings.json
│   ├── scan_settings.json
│   ├── favorites.json
│   └── listening_statistics.json
├── statistics/
│   ├── playback_records.json
│   └── snapshots/
│       └── snapshot_monthly_YYYYMM.db
├── assets/
│   └── (only app-owned cover/lyric assets that the implementation can safely export)
└── checksums.json
```

This layout is a **target schema for this app**, not a claim that every
file exists in the supplied reference. Include only categories actually
supported by the project. If the app has no data for a category,
represent that honestly in the manifest rather than inventing fake
records.

You may adapt file names to the current data models, but document every
change and keep the schema version explicit.

## `manifest.json`

Include at least:

-   `formatId`: a unique string identifying this app's backup format.
-   `formatVersion`: integer schema version.
-   `appName`.
-   `appVersionName` and `appVersionCode` when available.
-   `createdAtUtc`: ISO-8601 UTC timestamp.
-   `backupMode`: `MANUAL_GLOBAL` for a manual full metadata/settings
    archive, or another documented value if partial backups are
    supported.
-   `backupScope`: e.g. `GLOBAL` or `SELECTED_CATEGORIES`.
-   `deviceLabel`: optional user-friendly label, not a hardware
    identifier.
-   `counts`: real counts of exported songs, playlists, favorites,
    lyrics, and statistics records where available.
-   `categories`: map of each category to `included`, `recordCount` when
    meaningful, and its internal archive path.
-   `containsAudioFiles`: must be `false` unless actual media files are
    explicitly included through a separate, implemented, user-consented
    feature.
-   `warnings`: actual warnings generated during export.
-   `minimumRestoreVersion` if the app can enforce it reliably.

Never export passwords, authentication tokens, private keys, session
secrets, or unrelated app-private information. Avoid stable device
identifiers unless essential and justified; a human-readable device
label or generated backup ID is preferable.

## Checksums and integrity

-   Calculate a cryptographic SHA-256 checksum for every exported
    payload entry, excluding the checksum file itself to avoid a
    self-reference.
-   Store the relative entry path and digest in `checksums.json`.
-   Verify checksums during inspection and restore.
-   Reject missing required files, malformed manifests, duplicate
    conflicting entry names, traversal paths, absolute paths, and
    suspicious ZIP entries.
-   Enforce sensible limits for entry count, per-entry uncompressed
    size, and total uncompressed size. Prevent ZIP bombs and memory
    exhaustion.
-   Never extract arbitrary archive paths directly to filesystem
    locations.
-   Stream large entries; do not load the entire backup into memory.
-   Ensure export writes to a temporary destination first,
    flushes/closes it, verifies the result, and only then exposes it as
    a completed backup. Clean up partial temporary output on failure.
-   Use Android's Storage Access Framework (SAF) or the project's
    established document picker so the user chooses where the backup is
    saved. Do not request broad storage permissions just to save a
    backup.

## Backward and forward compatibility

-   Unknown optional fields must be ignored safely.
-   Unknown required format versions must be rejected with a useful
    message and no partial restore.
-   Include migration logic when the archive schema changes in future
    releases.
-   Do not assume the source sample's `.conf`/`.inf` files have the same
    schema as this app's JSON. Write explicit adapters only where their
    semantics are understood.
-   Keep a compatibility layer for the new format separate from the
    legacy backup parser.

------------------------------------------------------------------------

# 5. Backup Categories and Their Semantics

Inspect the actual project first and include only supported categories.
Map equivalent existing models into these logical categories:

1.  **Music tags / metadata** --- title, artist, album, track number,
    genre, year, and other fields the app manages.
2.  **Track covers** --- custom artwork references and/or app-owned
    artwork assets that can actually be exported.
3.  **Album covers** --- album-level artwork overrides and associated
    metadata.
4.  **Lyrics** --- saved lyrics and supported timing/synchronization
    metadata.
5.  **Playlists** --- playlist IDs/names, order, sort settings, and
    track references.
6.  **Favorites** --- favorite membership and enough metadata to
    reconcile tracks on another device.
7.  **Sort settings** --- per-screen or per-category sort key and order.
8.  **Scan settings** --- included/excluded folders, hidden-folder
    preferences, filters, and scan behavior where supported.
9.  **Listening statistics / recap** --- playback history, play counts,
    durations, skips, monthly summaries, and app-owned snapshots when
    supported.
10. **Backup metadata** --- schema version, timestamp, counts, category
    flags, and warnings.

Do not overwrite the device's music files or Android MediaStore records.
This is a player-data restore, not an audio-file restore.

## Artwork and lyrics

-   Prefer app-owned artwork files or portable content that the app can
    legally and technically export.
-   Do not rely on source-device `content://media/...` URIs remaining
    valid on another device.
-   For non-portable external references, preserve enough metadata to
    mark the artwork as unresolved and let the app regenerate it or
    prompt the user.
-   Restore only assets that pass size/type validation.
-   Lyrics should be associated with a stable track fingerprint or
    metadata key, not only a source absolute path.

## Playlist and favorite semantics

-   Export ordered track references and metadata needed for
    reconciliation.
-   Preserve playlist names, track order, sort order, and duplicate
    tracks where the app allows them.
-   Do not drop unmatched tracks silently.
-   Restore matched tracks and report unmatched tracks in a clear
    summary.
-   Preserve existing favorites/playlists unless the user explicitly
    chooses a replacement or merge mode.

## Statistics semantics

-   Import only the statistic categories supported by the app's current
    data model.
-   Avoid importing duplicate playback events on repeated restore. Use
    stable record IDs or a documented deduplication key.
-   Preserve timestamps and time-zone semantics.
-   Validate database snapshots before applying them. Never replace the
    live database file with an unvalidated arbitrary file.
-   If a snapshot cannot be safely migrated, skip it with a clear report
    while restoring other valid categories only if the user has chosen
    partial restore and the operation can remain consistent.

------------------------------------------------------------------------

# 6. Restore Workflow --- Must Be Safe and Predictable

Implement this sequence:

1.  User chooses **Restore from Archive**.
2.  Open SAF file picker.
3.  Inspect the file signature and archive structure.
4.  Parse the manifest without applying changes.
5.  Validate format ID/version, archive paths, sizes, checksums,
    required entries, and category schemas.
6.  Show a restore preview: backup date, app/schema version, categories,
    counts, warnings, missing assets, and whether audio files are
    included.
7.  Let the user select categories to restore, limited to those actually
    present and supported.
8.  Offer clear conflict behavior:
    -   **Merge**: retain existing data and add/update matching imported
        items.
    -   **Replace selected categories**: replace only the chosen
        categories after explicit confirmation.
    -   If a category cannot safely support both modes, provide only the
        safe mode and explain why.
9.  Reconcile track references with the current library.
10. Stage parsed and normalized data in temporary storage.
11. Run schema validation and consistency checks.
12. Apply changes using database transactions where supported.
13. Do not delete existing data before the replacement payload is
    validated and staged.
14. On failure, roll back database changes and preserve the previous
    state as far as the architecture permits.
15. Refresh Room flows, repositories, caches, media-library indexes, and
    UI state.
16. Display a real restore report with imported, updated, skipped,
    unmatched, duplicate, and failed counts.

If the archive is corrupt, unsupported, truncated, or malicious, reject
it before any live data changes. If the user cancels during validation,
do not mutate the live data. If cancellation happens during a
transaction, roll back where possible.

## Track matching on the receiving device

Never rely solely on the old `_data` absolute path or source-device
MediaStore ID. Use a deterministic matching strategy, for example:

1.  Exact stable app-owned track ID only when the ID is truly portable.
2.  Strong fingerprint based on normalized metadata and duration.
3.  Normalized title + artist + album + duration tolerance.
4.  Filename + parent-folder hint as a lower-confidence fallback.

If file hashes are already available, use them; do not force a full hash
of every audio file if it would cause long, unexpected battery/storage
costs. Make expensive fingerprinting opt-in or incremental.

-   Normalize Unicode and whitespace consistently.
-   Treat missing artist/album metadata carefully.
-   Do not auto-assign low-confidence ambiguous matches.
-   If multiple target tracks match, ask the user or mark the reference
    unresolved.
-   Provide an unmatched-track report and safe remapping options.
-   The app must never move, rename, delete, or modify user audio files
    during metadata restore.

------------------------------------------------------------------------

# 7. Existing Legacy Backup/Restore --- Preservation Rules

This requirement is mandatory.

-   Find the exact old implementation and write tests describing its
    current supported behavior before modifying it.
-   Keep old backup creation and old restore import working.
-   Keep old file extensions and formats accepted.
-   Do not reinterpret legacy backups as the new archive format based
    only on their extension.
-   Use explicit format detection, preferably by content signature plus
    schema.
-   If old backups share a ZIP container, distinguish them by their
    internal manifest/signature and known legacy entry structure.
-   If legacy backups are not ZIP files, retain their existing parser
    and writer unchanged except for necessary bug fixes.
-   Do not make legacy backup creation call the new archive writer
    unless the user deliberately selects the new option.
-   Add regression tests for old backup export/import, including a
    representative existing backup fixture if one can be safely
    included.
-   If the current old method has known limitations, document them; do
    not silently remove behavior.

The UI should make the distinction obvious, for example:

-   **Existing Backup (Legacy)** --- uses the current backup format.
-   **Portable Archive Backup (New)** --- creates a structured
    `.musicbackup` archive.

Use the project's current wording if the app already has established
labels; the above are suggested labels, not permission to rename
unrelated settings.

------------------------------------------------------------------------

# 8. Architecture and Implementation Expectations

Adapt names to the actual project; do not blindly paste classes if
equivalent architecture already exists.

A suitable separation is:

``` text
backup/
  BackupRestoreCoordinator
  LegacyBackupService
  ArchiveBackupService
  ArchiveInspector
  ArchiveManifest
  ArchiveFormatVersion
  ArchiveValidator
  ArchiveChecksumService
  BackupCategory
  BackupExportOptions
  RestoreOptions
  RestorePreview
  RestoreReport
  TrackReference
  TrackMatchResolver
  BackupStorageGateway
  RestoreStagingStore
```

Responsibilities:

-   `LegacyBackupService`: preserve the current legacy format and
    behavior.
-   `ArchiveBackupService`: export the new versioned archive.
-   `ArchiveInspector`: inspect without mutating app state.
-   `ArchiveValidator`: validate manifest, entry paths, sizes,
    checksums, and payload schemas.
-   `TrackMatchResolver`: resolve imported references to the current
    local library.
-   `RestoreStagingStore`: prepare normalized data before committing.
-   `BackupRestoreCoordinator`: orchestrate UI-independent operations,
    progress, cancellation, and reports.
-   `BackupStorageGateway`: use SAF URIs and streams without assuming a
    filesystem path exists.

Use Kotlin coroutines and the project's existing lifecycle-aware state
handling. Long-running compression, checksum, parsing, and restore work
must not block the main thread. Emit real progress based on actual
completed bytes/entries or stages. Avoid holding database transactions
open while parsing a large ZIP; stage first, then commit.

If the app uses Room, use DAO/repository transactions for database
updates. Preserve foreign-key relationships and database invariants. Do
not overwrite Room's database file while it is open. If a database
snapshot must be imported, use a supported migration/restore process
with close, validate, backup-current-state, replace, reopen, and
rollback safeguards---or skip snapshot replacement if that process
cannot be made safe.

Use dependency injection and testable interfaces consistent with the
existing project. Do not create a parallel architecture that duplicates
the entire repository layer.

------------------------------------------------------------------------

# 9. Suggested Kotlin Blueprints

These examples are architectural guides, not a substitute for adapting
to the existing codebase.

## 9.1 Data model

``` kotlin
@kotlinx.serialization.Serializable
data class ArchiveManifest(
    val formatId: String = "offline-music-player-backup",
    val formatVersion: Int,
    val appName: String,
    val appVersionName: String? = null,
    val appVersionCode: Long? = null,
    val createdAtUtc: String,
    val backupMode: String = "MANUAL_GLOBAL",
    val backupScope: String = "GLOBAL",
    val containsAudioFiles: Boolean = false,
    val counts: Map<String, Long> = emptyMap(),
    val categories: Map<String, ArchiveCategoryInfo>,
    val warnings: List<String> = emptyList()
)

@kotlinx.serialization.Serializable
data class ArchiveCategoryInfo(
    val included: Boolean,
    val path: String? = null,
    val recordCount: Long? = null,
    val required: Boolean = false
)

enum class RestoreConflictMode {
    MERGE,
    REPLACE_SELECTED_CATEGORIES
}

data class RestoreReport(
    val imported: Map<String, Int>,
    val updated: Map<String, Int>,
    val skipped: Map<String, Int>,
    val unmatchedTracks: Int,
    val warnings: List<String>,
    val errors: List<String>
)
```

Adapt serialization annotations to the project's existing JSON library.
Do not introduce Kotlin Serialization if the app already uses a suitable
maintained JSON library and changing dependencies is unnecessary.

## 9.2 Content signature and ZIP validation

``` kotlin
private fun hasZipSignature(prefix: ByteArray): Boolean {
    // Standard ZIP signatures begin with PK (common local-file, empty-archive,
    // and spanning signatures vary). Confirm with ZipFile/ZipInputStream as well.
    return prefix.size >= 4 &&
        prefix[0] == 0x50.toByte() &&
        prefix[1] == 0x4B.toByte() &&
        (
            prefix[2] == 0x03.toByte() && prefix[3] == 0x04.toByte() ||
            prefix[2] == 0x05.toByte() && prefix[3] == 0x06.toByte() ||
            prefix[2] == 0x07.toByte() && prefix[3] == 0x08.toByte()
        )
}
```

A signature check is only the first filter. A valid ZIP signature does
not prove that the archive is safe or that it belongs to this app. Use a
robust archive reader and validate every entry and the manifest. When
working from a SAF URI, stream/copy to a bounded temporary file if
random access is required, and clean it up afterward.

## 9.3 Path validation

For every archive entry:

-   Reject absolute paths, drive-letter paths, `..` traversal
    components, NUL characters, and unexpected separators.
-   Normalize and validate entry names before reading.
-   Do not extract entries to arbitrary destinations.
-   Reject duplicate entry names that could cause ambiguity.
-   Enforce entry-count and expanded-size limits.
-   Read entries as streams and never trust ZIP-reported sizes alone;
    count actual bytes as they are read.

## 9.4 Export algorithm

``` text
createArchiveBackup(destinationUri, selectedCategories):
    collect a consistent snapshot of selected app-owned data
    validate each exported record
    create manifest with real counts and warnings
    serialize payloads to a temporary archive
    compute SHA-256 for each payload entry
    write checksums manifest
    finalize and close archive
    reopen archive and validate manifest + checksums
    copy/commit completed archive to chosen SAF destination safely
    report success only after verification and close succeed
    on failure: remove temporary output and report the actual error
```

Do not serialize the full music library into a single giant in-memory
string. Stream data or use bounded chunks where practical.

## 9.5 Restore algorithm

``` text
inspectArchive(uri):
    open file through SAF
    verify ZIP content and archive limits
    parse manifest
    validate schema/version/path safety/checksums
    parse selected category metadata without mutating live data
    return preview + warnings

restoreArchive(uri, options):
    inspect and validate archive again (do not trust stale preview)
    stage selected categories in temporary storage
    resolve track references against current media library
    calculate merge/replace operations and conflicts
    require confirmation for destructive replacement
    apply staged database updates transactionally
    refresh repositories/caches/indexes
    return a detailed RestoreReport
```

Never rely on a UI preview as the only validation step; the archive
could be changed between preview and restore.

------------------------------------------------------------------------

# 10. User Flow and UI Details

Implement using the existing Compose/Material design system.

## Backup screen

Show two clearly separated cards/sections:

### Existing Backup (Legacy)

-   One-sentence description of the current format.
-   Existing backup and restore controls.
-   Existing behavior and compatibility retained.

### Portable Archive Backup (New)

-   Description that the archive stores selected player data/settings,
    not the audio files.
-   Category selection.
-   "Create Archive Backup" button.
-   Destination picker.
-   Real progress and cancellation when safe.
-   Result: filename, creation time, file size, categories, and
    validation status.

Do not show "Last backup successful" based on a saved preference alone.
Only show a success record after a real completed operation.

## Restore screen

-   File picker.
-   File type/content recognition.
-   Backup details preview.
-   Validation/checksum status.
-   Category selector.
-   Merge vs replace selection when supported.
-   Explicit warning before replacing selected data.
-   Restore progress.
-   Final report with:
    -   imported and updated records,
    -   unmatched tracks,
    -   skipped or duplicate records,
    -   missing artwork/lyrics,
    -   unsupported categories,
    -   warnings and recoverable errors.

Make the labels understandable to a non-technical user. Accessibility
labels, talkback semantics, contrast, and touch targets must follow
existing project standards.

------------------------------------------------------------------------

# 11. Security, Privacy, and Reliability

-   Backup is local-only unless the user chooses a cloud-backed SAF
    provider; do not add network uploads.
-   Explain that selecting a cloud-drive provider can send the chosen
    archive to that provider. The app itself should not silently upload
    anything.
-   Do not include secrets, credentials, access tokens, or unrelated
    private files.
-   Do not follow paths or URIs from the archive to read arbitrary local
    files.
-   Treat archive content as untrusted input.
-   Limit expanded archive size, entry count, nesting, and individual
    entry size.
-   Validate JSON/schema types and record counts before use.
-   Handle invalid encodings, truncated entries, duplicate JSON keys
    where the parser supports detection, malformed records, and
    unsupported versions gracefully.
-   Do not expose raw stack traces or sensitive filesystem paths in the
    UI. Keep actionable, redacted logs.
-   Keep user-created backup files untouched unless the user explicitly
    selects an existing destination and confirms overwrite.
-   Use atomic temporary-file patterns where supported.
-   Never overwrite the current backup with a partially written archive.
-   If encryption is offered, implement real authenticated encryption
    with a documented key flow. Do not invent a fake "encrypted"
    checkbox. If no secure, tested encryption design is implemented,
    clearly state that the archive is not encrypted and do not claim
    otherwise.
-   Check Android scoped-storage behavior and SAF provider limitations.
    Do not request `MANAGE_EXTERNAL_STORAGE` merely for backup
    import/export.

------------------------------------------------------------------------

# 12. Testing Requirements --- No Exceptions

Write and run automated tests appropriate to the current project.

## Legacy regression tests

-   Existing legacy backup can still be created.
-   Existing legacy backup can still be restored.
-   Existing known legacy fixture parses as before.
-   Legacy format detection does not accidentally route the file through
    the new parser.
-   Legacy category behavior remains intact.

## New archive export tests

-   Correct manifest and format version are written.
-   Included categories and counts match actual records.
-   Excluded categories are not secretly exported.
-   Audio-file inclusion flag is truthful.
-   ZIP structure is valid and reopens successfully.
-   Checksums match the written payload.
-   Export to a SAF URI succeeds.
-   Destination failure, full storage, cancellation, and I/O errors do
    not leave a falsely successful backup.
-   Large datasets do not require loading all content into memory.

## New archive import tests

-   Valid archive preview works without changing live data.
-   Valid archive restores selected categories.
-   Merge preserves existing unrelated records.
-   Replace changes only selected categories after confirmation.
-   Invalid checksum is rejected.
-   Truncated/corrupt archive is rejected.
-   Unsupported major format version is rejected.
-   Missing optional category is handled correctly.
-   Malformed required payload is rejected before mutation.
-   ZIP traversal, absolute paths, duplicate names, and oversized
    entries are rejected.
-   Archive renamed `.wav` is recognized as ZIP only when the ZIP
    structure and app format validate.
-   Genuine WAV audio is rejected as an archive.
-   Repeated restore does not duplicate playback history or playlist
    tracks unexpectedly.
-   Database failure rolls back and leaves existing state usable.
-   Cancellation leaves no partial live restore.

## Track reconciliation tests

-   Same track at a new path matches successfully.
-   Unicode and whitespace variations are normalized appropriately.
-   Missing metadata does not cause unsafe matches.
-   Ambiguous matches are reported instead of silently choosing one.
-   Missing tracks are retained as unresolved playlist/favorite entries
    if the app's data model supports it, or reported clearly if not.
-   Source MediaStore IDs and `content://` album-art URIs are not
    assumed to work on the target device.

## Manual device checks

Test on a real Android device and emulator where practical:

1.  Create both legacy and archive backups.
2.  Restore both using their respective options.
3.  Export archive to internal storage and an SD card/document provider
    where available.
4.  Transfer the archive to another device.
5.  Scan music on the receiving device and restore
    favorites/playlists/metadata.
6.  Test a large library.
7.  Test storage permission denial, provider cancellation, low storage,
    malformed archive, and app process interruption.
8.  Confirm no audio files are deleted or modified.
9.  Confirm restored data appears in the UI after repositories/caches
    refresh.

Do not mark tests as passed unless they were actually run. Distinguish
automated tests, manual checks, and untested cases in the final report.

------------------------------------------------------------------------

# 13. Implementation Plan --- Work in Phases

Use these phases to avoid large, hard-to-debug changes.

## Phase 1 --- Audit and baseline

-   Inspect current backup implementation and app architecture.
-   Document the old format and supported categories.
-   Add or run baseline legacy tests.
-   Write a short implementation plan listing exact files to modify.

**Stop condition:** Do not change behavior until the legacy path is
understood.

## Phase 2 --- Data schema and archive writer

-   Add manifest/category models.
-   Add archive writer and checksum generation.
-   Export actual supported app data.
-   Add export tests.

**Stop condition:** A generated archive reopens and validates before UI
integration.

## Phase 3 --- Inspector and safe validator

-   Detect ZIP by content signature.
-   Parse manifest and category previews.
-   Validate entries, checksums, schema, sizes, and paths.
-   Add malicious/corrupt fixture tests.

**Stop condition:** Invalid archives are rejected without mutating app
data.

## Phase 4 --- Restore staging and reconciliation

-   Implement staging.
-   Match track references to current local media.
-   Implement merge and safe category replacement.
-   Use transactions and rollback protections.
-   Add restore tests.

**Stop condition:** A valid test backup restores correctly, and failed
restore leaves existing data intact.

## Phase 5 --- UI integration

-   Add the new archive method beside the old one.
-   Implement destination/file pickers, category selection, preview,
    progress, warnings, and reports.
-   Preserve existing labels and flows where required.
-   Verify accessibility and responsive layouts.

**Stop condition:** Both methods are usable from the app UI.

## Phase 6 --- Verification and documentation

-   Run unit tests, instrumented tests where available, lint, and build.
-   Fix regressions.
-   Update `brain.md` and README.
-   Record exact supported archive version, categories, limitations,
    test outcomes, and migration notes.
-   Review changes for accidental removal of old functionality.

**Stop condition:** The build succeeds, required tests run, and
remaining limitations are explicitly documented.

------------------------------------------------------------------------

# 14. Definition of Done

Do not declare completion unless all applicable items are true:

-   [ ] Existing legacy backup creation still works.
-   [ ] Existing legacy restore still works.
-   [ ] A distinct archive backup option exists.
-   [ ] A distinct archive restore option exists.
-   [ ] The new archive has a documented, versioned manifest.
-   [ ] Real supported app data is exported; no dummy records.
-   [ ] Checksums and archive validation work.
-   [ ] The sample's ZIP content is recognized even though the filename
    ends in `.wav`.
-   [ ] A real WAV audio file is not treated as a backup.
-   [ ] Preview is available before restore.
-   [ ] Merge and/or replace behavior is explicit and tested.
-   [ ] Track paths and MediaStore IDs are reconciled safely.
-   [ ] Missing tracks and artwork are reported honestly.
-   [ ] No music files are falsely claimed to be included.
-   [ ] Restore uses staging and transactional safeguards.
-   [ ] UI progress and success/failure states reflect actual
    operations.
-   [ ] Unit tests and legacy regression tests have been run.
-   [ ] Build/lint results are reported truthfully.
-   [ ] Documentation and `brain.md` are updated.
-   [ ] No unrelated features or existing functionality were removed.

------------------------------------------------------------------------

# 15. Exact Final Instruction to Google AI Studio

Implement this specification in the existing project. Start by auditing
the repository and reporting the current legacy backup format, its code
entry points, data categories, and regression risks. Then implement the
phases in order.

Do not merely produce a plan or code snippets: make the actual project
changes. Do not overwrite the legacy implementation with the new format.
The old method and new archive method must both work independently.

After each phase: 1. List files changed and why. 2. Run relevant
tests/build checks. 3. Report real results and remaining issues. 4.
Update `brain.md` with completed work, test evidence, current phase, and
next steps.

If a feature cannot be implemented safely because the current schema or
architecture does not support it, do not fake it. Explain the exact
limitation, implement the safest supported behavior, and document the
gap. Preserve all existing music-player functionality unrelated to
backup and restore.
