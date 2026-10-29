# PULSE PLAYER — DUAL BACKUP AND RESTORE SYSTEM
## Legacy Backup + Compatible `.wav`-Named Archive Backup

**Document type:** Implementation specification  
**Target application:** Pulse Player for Android  
**Implementation environment:** Google AI Studio  
**Priority:** High — data integrity and backward compatibility  
**Reference file:** `ManualBackup_20261028_172204175(1).wav`

---

## 1. Objective

Enhance the existing Pulse Player application with a second, fully functional backup and restore system based on the supplied reference archive.

The application must support both:

1. **Legacy Backup & Restore** — preserve the existing backup and restore functionality, formats, and user workflows.
2. **Compatible Archive Backup & Restore** — implement a new system capable of reading and creating backups in the format represented by the supplied `.wav`-named ZIP archive.

Both systems must coexist within the same application. Neither may silently replace, disable, corrupt, or reinterpret the other.

The new system should preserve the relevant music library metadata, playlists, artwork data, lyrics, sorting preferences, scan/hidden settings, and supported playback statistics.

The application must also be able to import the supplied sample archive, validate its contents, preview the data it can restore, and restore supported information safely.

**Do not build a superficial file-renaming feature. Implement a real archive parser, exporter, validator, and restoration workflow.**

## 2. Mandatory Reference-File Findings

The supplied file was inspected directly.

Its first bytes are `PK`, identifying a ZIP archive rather than a standard RIFF/WAVE audio file.

The archive contains these entries:

```text
settings.bak
Covers/MusicCoversBackupConfig.conf
Covers/AlbumCoversBackupConfig.conf
MusicTagsBackupConfig.conf
Lyrics/LyricsBackupConfig.conf
PlayLists/PlayListBackupConfig.conf
SortBackupConfig.conf
ScanHiddenBackupConfig.conf
Statistics/snapshot_monthly_202605.db
Statistics/snapshot_monthly_202606.db
Statistics/snapshot_monthly_202607.db
Statistics/snapshot_monthly_202608.db
Statistics/snapshot_monthly_202609.db
Statistics/StatisticsSnapshotConfig.json
Statistics/PlaybackRecords.json
metadata.inf
```

The sample metadata describes a global manual backup and reports 13,509 songs and 41 playlists. It also identifies the source application's version as `4.4.1.189`.

These counts describe the source archive. They must not be inserted into the application as hardcoded defaults or fabricated data.

The archive contains metadata and statistics files, including SQLite database snapshots. The actual audio files are not included as separate audio entries.

### Required interpretation

- Treat the `.wav` extension as a compatibility filename convention.
- Inspect the file's binary signature and structure rather than trusting its extension.
- Recognize the reference archive as ZIP-based.
- Preserve supported original archive paths and data representations when exporting the compatible format.
- Do not assume that the contents of every `.conf` or `.bak` file are JSON.
- Parse each file according to its actual content and verified schema.
- Inspect the full contents of the sample before finalizing parsers for the larger configuration files.
- Do not invent undocumented field meanings, data structures, database schemas, or compatibility guarantees.

The reference file is the source of truth for the archive structure. The existing Pulse Player project remains the source of truth for the application's own database and internal models.

---

## 3. Preserve the Existing Backup System

Before making any changes, inspect the complete current backup and restore implementation.

Identify:

- The current backup formats.
- Existing file extensions and MIME types.
- Existing JSON, ZIP, database, or other serialization formats.
- Which user data is included.
- How backup files are selected and imported.
- How the restore operation changes local data.
- Existing backup settings and UI entry points.
- Existing tests and migration behavior.

Do not delete or rewrite the legacy implementation without a demonstrated technical necessity.

### Legacy compatibility requirements

1. Existing backup files must remain importable.
2. Existing restore behavior must remain available.
3. Existing backup files must retain their original format unless the user explicitly chooses a conversion.
4. Adding the new archive format must not change the meaning of the legacy backup extension.
5. Legacy backups must not be accidentally passed to the new archive parser.
6. If the legacy format is also ZIP-based, distinguish formats through their internal manifest, metadata, or verified structure.
7. Existing backup settings must continue to work.
8. Add regression tests proving that previously supported backup files still restore correctly.

If the current project has a legacy backup format that differs from the one assumed here, preserve the actual implementation rather than introducing a fictional legacy format.

---

## 4. Backup and Restore UI

Add a clear backup management screen or extend the existing screen.

The screen must expose two separate options:

### Option A — Existing Backup

Display the existing backup option using its current name and behavior.

- Preserve the existing backup workflow.
- Preserve its supported output formats.
- Preserve its existing restore/import workflow.
- Do not silently change its extension or serialization.

### Option B — Compatible Archive Backup

Suggested label: **Compatible Archive Backup**

Description:

`Create or restore a backup compatible with the supplied ZIP-based .wav archive format.`

Provide the following actions:

- Create compatible backup.
- Restore compatible backup.
- Select backup file.
- Inspect backup details.
- Validate backup.
- Preview restore.
- Confirm restore.
- View restore results.

If the existing design supports it, display the detected format, backup date, source version, library counts, and available data categories.

Use the existing application design system and theme architecture. Do not introduce a separate, inconsistent visual design for the backup screen.

### File extensions

Support the `.wav` extension as a filename convention for the compatible archive when required.

For example:

`PulsePlayer_Backup_20261029.wav`

The file's contents must still be a correctly generated ZIP archive when using this compatibility format.

Do not use an actual audio encoder to create the backup.

Do not overwrite a valid audio file just because it has a `.wav` extension.

When importing a `.wav` file, identify the actual format from its signature and verified structure before deciding which parser to use.

---

## 5. Compatible Archive Format

Implement a dedicated archive reader and writer.

Recommended conceptual components:

- `BackupFormatDetector`
- `CompatibleArchiveReader`
- `CompatibleArchiveWriter`
- `CompatibleBackupValidator`
- `CompatibleRestoreCoordinator`
- `BackupPreviewBuilder`
- `BackupResultReporter`

Use the existing project architecture where equivalent components already exist. Avoid unnecessary duplicate classes.

### 5.1 Archive structure

The writer must reproduce the verified directory and file naming conventions of the supplied sample when creating a backup intended for compatibility with that format.

The known paths include:

```text
metadata.inf
settings.bak
MusicTagsBackupConfig.conf
Covers/MusicCoversBackupConfig.conf
Covers/AlbumCoversBackupConfig.conf
Lyrics/LyricsBackupConfig.conf
PlayLists/PlayListBackupConfig.conf
SortBackupConfig.conf
ScanHiddenBackupConfig.conf
Statistics/StatisticsSnapshotConfig.json
Statistics/PlaybackRecords.json
Statistics/snapshot_monthly_YYYYMM.db
```

The list above describes observed entries, not permission to generate empty files indiscriminately.

Create only the entries required by the selected backup options and supported by the actual data model.

Where compatibility requires particular files to exist, reproduce the observed behavior only after verifying their expected contents and semantics.

### 5.2 Metadata

The sample's `metadata.inf` is JSON despite its extension.

Its observed fields include:

- `backupTime`
- `mode`
- `type`
- `isAutoBackup`
- `deviceId`
- `versionName`
- `versionCode`
- `settings`
- `songs`
- `playlists`

The nested `settings` object includes flags for music tags, covers, lyrics, playlists, sorting, scan/hidden settings, and music recap.

Requirements:

- Generate metadata from the actual backup operation.
- Use the correct timestamp format required by the compatible format.
- Use actual counts from the exported data.
- Set backup mode and type correctly.
- Reflect the selected backup options accurately.
- Do not hardcode the reference device ID or reference library counts.
- Do not assume every field is mandatory or every flag has the same meaning in all source versions.
- Validate required fields and types before restoration.
- Preserve unknown metadata fields when safe and necessary for forward compatibility.

Treat device identifiers and other identifying values as sensitive. Preserve or regenerate them according to verified compatibility requirements and the application's privacy design.

### 5.3 Settings file

The observed `settings.bak` contains backup flags and source version/device metadata.

Implement a serializer and parser based on the verified structure.

Ensure that the metadata and settings file do not contradict each other. For example, a feature marked as excluded must not be represented as successfully backed up without a documented reason.

### 5.4 Music tags

Implement export and import for the verified structure in `MusicTagsBackupConfig.conf`.

The sample's file is large and cannot safely be assumed to follow a particular schema based only on its filename.

Before implementing the parser:

1. Inspect representative beginning, middle, and ending sections.
2. Determine the serialization format.
3. Identify the record boundaries and stable identifiers.
4. Identify how paths, titles, artists, albums, durations, and other fields are represented.
5. Determine how missing or duplicate tracks are handled.
6. Document the observed schema and unsupported cases.

Map external records into the existing Pulse Player data model.

Do not blindly replace all local song metadata with source data if the local library uses a different representation.

### 5.5 Artwork

Implement support for both observed files:

- `Covers/MusicCoversBackupConfig.conf`
- `Covers/AlbumCoversBackupConfig.conf`

Inspect their actual contents and referenced data before deciding how to restore them.

Requirements:

- Preserve valid artwork data and its mapping to tracks or albums.
- Avoid associating artwork with the wrong song.
- Validate referenced artwork identifiers.
- Avoid duplicate cache records.
- Handle missing, corrupt, or unsupported artwork gracefully.
- Rebuild local artwork indexes if the architecture requires it.

Do not assume that artwork is embedded in these files or stored as separate archive entries until the format is verified.

### 5.6 Playlists

Implement `PlayLists/PlayListBackupConfig.conf` according to its actual schema.

Preserve, where represented:

- Playlist names.
- Playlist identifiers.
- Track membership.
- Track ordering.
- Duplicate entries if supported.
- Empty playlists.
- Relevant playlist metadata.

Resolve playlist tracks using stable identifiers and verified matching rules.

If source track IDs are not valid in the target installation, use a documented matching strategy based on available metadata.

Do not silently discard unresolved tracks. Report them in the restore summary and preserve them when the data model supports unresolved playlist entries.

### 5.7 Lyrics

Implement `Lyrics/LyricsBackupConfig.conf` according to the verified format.

The supplied sample's lyrics file contains `[]`, representing an empty JSON array. This does not mean all future compatible backups must contain no lyrics.

Requirements:

- Export real lyrics when available and included.
- Restore valid lyrics into the correct track.
- Preserve supported timing information for synchronized lyrics.
- Validate associations before applying lyrics.
- Treat an empty collection as a valid empty result.
- Distinguish missing lyrics from a malformed lyrics file.

### 5.8 Sorting and scan/hidden settings

Support:

- `SortBackupConfig.conf`
- `ScanHiddenBackupConfig.conf`

The sorting file is JSON in the supplied sample. It contains sorting keys and sort orders for several library categories.

Requirements:

- Validate each sorting category and key.
- Map settings to categories actually supported by Pulse Player.
- Preserve compatible values.
- Safely handle unknown categories.
- Avoid crashing if the target version does not support a source setting.
- Restore scan and hidden-folder settings only after validating their schema and understanding their behavior.

Do not overwrite unrelated application preferences during restoration.

### 5.9 Statistics

The sample includes:

- `Statistics/StatisticsSnapshotConfig.json`
- `Statistics/PlaybackRecords.json`
- Monthly SQLite database snapshots under `Statistics/`

Implement support for these only after examining their actual schemas.

Requirements:

- Validate JSON before import.
- Validate SQLite databases using SQLite integrity checks.
- Verify database schema compatibility.
- Do not assume the source statistics database schema matches Pulse Player's Room schema.
- Never overwrite the active database directly with an unverified foreign database.
- Use migrations or explicit mapping between compatible schemas.
- Preserve playback records and historical snapshots where supported.
- Preserve dates, counts, and relationships accurately.
- Detect duplicate records using stable keys or documented matching rules.
- Handle unsupported statistics versions without corrupting existing statistics.

If the source schema is incompatible, report which statistics can be restored and which cannot. Do not claim that unsupported statistics were restored.

---

## 6. Format Detection and Import Routing

Build a deterministic format-detection process.

The extension is a hint, not proof of file type.

### Detection sequence

1. Open the selected file using Android's Storage Access Framework.
2. Read a bounded number of leading bytes.
3. Identify the actual container format.
4. If the file is a ZIP archive, inspect its entry list.
5. Look for the verified compatible-format signature, including expected paths and metadata.
6. If the file matches the new format, route it to the compatible archive reader.
7. If it matches a known legacy format, route it to the legacy importer.
8. If the format is recognized but incomplete or unsupported, show a specific error.
9. If the file is an actual WAV audio file, do not treat it as a backup merely because it has the expected extension.
10. Never guess a format and immediately overwrite user data.

If the existing legacy backup format is also ZIP-based, identify it through verified internal structure and version metadata rather than the ZIP signature alone.

Provide a clear error for malformed archives, unsupported versions, missing required entries, and unrecognized files.

---

## 7. Backup Creation Workflow

When the user chooses Compatible Archive Backup:

1. Read the current application state through the existing repositories.
2. Determine which backup categories the user selected.
3. Collect the corresponding data consistently.
4. Create a new archive in a temporary destination.
5. Serialize each entry using the verified compatible schema.
6. Generate correct metadata and settings.
7. Include statistics snapshots only when supported and selected.
8. Validate all generated entries.
9. Verify that the resulting archive can be opened and parsed.
10. Write the completed archive to the user-selected destination.
11. Report success only after the output is complete and validated.

Use Android's Storage Access Framework for user-selected destinations and imports.

Avoid hardcoded external-storage paths.

### Consistent snapshots

Ensure that backup data comes from a consistent view of the library.

If a full database transaction cannot span all categories, use a documented snapshot strategy that avoids inconsistent track references, playlist membership, and metadata.

Do not block the main thread while generating a large backup.

Show meaningful progress for large exports, with cancellation where safely supported.

If cancellation occurs, remove incomplete temporary output and leave the existing backup files untouched.

---

## 8. Restore Workflow

Restoration must be a controlled process rather than a series of immediate database writes.

### Phase A — Select and inspect

The user selects a file through Android's Storage Access Framework.

Display:

- Detected format.
- Backup timestamp, if available.
- Source version.
- Backup type.
- Available categories.
- Song and playlist counts, when reliable.
- Whether statistics snapshots exist.
- Any validation warnings.

Do not assume that counts in metadata equal the number of valid records. Verify counts where practical.

### Phase B — Validate

Before changing application data:

- Check archive integrity.
- Reject unsafe archive entry paths.
- Reject path traversal attempts such as `../`.
- Reject duplicate or conflicting entry paths where ambiguous.
- Enforce reasonable limits on expanded size, entry count, file size, and compression ratios.
- Detect corrupted or truncated entries.
- Validate required files and their types.
- Parse all supported data.
- Validate database files separately.
- Check supported format versions.
- Build a restore plan.

Treat every archive entry as untrusted input.

Do not extract the archive indiscriminately into an application directory.

### Phase C — Preview

Show a preview of the data to be restored.

Where supported, allow the user to select categories such as:

- Music tags.
- Artwork.
- Lyrics.
- Playlists.
- Sorting settings.
- Scan/hidden settings.
- Playback statistics.

Do not offer category toggles for data that the archive does not contain.

Explain the consequences of the selected restore mode.

### Phase D — Choose restore behavior

Provide clearly defined behavior based on the application's existing capabilities:

**Merge:** Add or update compatible data while retaining unrelated local records.

**Replace selected categories:** Replace only the categories explicitly selected by the user.

**Cancel:** Make no changes.

If the existing application uses different restore modes, preserve those modes and document how they interact with the new format.

Never imply that a restore is a merge if it actually deletes or replaces existing records.

### Phase E — Commit safely

1. Preserve the current data or establish a reliable rollback strategy.
2. Apply changes through existing repositories and database transactions where possible.
3. Restore data in dependency-safe order.
4. Rebuild indexes or caches as required.
5. Validate the resulting data.
6. Commit only when the selected operation succeeds.
7. Roll back failed transactional changes.
8. Restore the previous valid state if a multi-stage operation fails and rollback is supported.
9. Release temporary resources.
10. Display a detailed restore report.

Do not leave half-restored playlists, corrupted databases, or inconsistent references after an error.

---

## 9. Track Matching and Missing Audio

The supplied reference archive does not include audio files as separate entries.

Therefore, importing this archive must not be represented as restoring the actual music files.

Use the existing music library and MediaStore/SAF integration to resolve metadata to available audio tracks.

Implement matching in a controlled order based on the information actually present, for example:

1. A stable application-independent track identifier, if one is genuinely available.
2. A normalized content URI or path where meaningful on the same device.
3. A verified content fingerprint, if already available and appropriate.
4. A carefully defined metadata match using title, artist, album, and other available fields.

Do not rely on source-device paths as universally valid identifiers.

Do not assume two songs with the same title and artist are necessarily the same recording.

If a track cannot be resolved:

- Preserve the backup record if supported.
- Mark the record as unresolved when appropriate.
- Report the issue to the user.
- Avoid silently linking it to an unrelated song.

Restoring metadata must not delete, move, or modify audio files unless the user explicitly selects a separate supported operation.

---

## 10. Security and Privacy

The backup system must be local-first and must not upload the user's library to a server.

Requirements:

- No new internet permission solely for backup.
- No analytics or telemetry that transmits library details.
- Use Android's Storage Access Framework.
- Validate all archive entries and serialized content.
- Prevent path traversal and unsafe extraction.
- Avoid executing scripts or code found in archives.
- Do not trust archive metadata.
- Do not overwrite active database files directly.
- Use bounded resource consumption to prevent archive bombs.
- Do not log full file contents, sensitive identifiers, or private music metadata unnecessarily.
- Clean up temporary files safely.
- Do not expose backup contents through unintended public storage locations.

If encryption is supported by the existing backup architecture, preserve its documented behavior. Do not claim that the compatible archive is encrypted unless the format actually provides encryption.

Do not add incompatible encryption to an archive that must remain readable by the source application.

---

## 11. Versioning and Compatibility

Implement explicit format-version handling.

The source application's version is not necessarily the backup schema version.

Maintain separate concepts for:

- Source application version.
- Backup format version, when defined.
- Pulse Player internal database version.
- Per-category serialization version, if needed.

Requirements:

- Accept verified supported versions.
- Reject or safely handle unsupported versions.
- Use migrations when a documented migration is possible.
- Preserve unknown optional fields where safe.
- Avoid destructive defaults for unknown fields.
- Do not silently interpret a newer schema as an older one.
- Document all verified compatibility limitations.

Do not assume that creating the same filenames guarantees compatibility with the application that produced the reference file.

---

## 12. Tests and Acceptance Criteria

Add unit tests, integration tests, and regression tests.

Use the supplied sample archive as a read-only fixture. Never overwrite it during testing.

### Reference archive tests

- The `.wav`-named ZIP archive is detected correctly.
- `metadata.inf` is parsed correctly.
- `settings.bak` is parsed correctly.
- Every observed archive path is recognized.
- Empty lyrics are handled correctly.
- Sorting settings are parsed correctly.
- The playlist data is parsed according to its actual schema.
- Artwork and music-tag data are parsed according to verified schemas.
- Statistics files are validated without modifying the original archive.
- The sample's source counts are read as metadata rather than hardcoded application defaults.

### Export tests

- A generated compatible backup has the required verified structure.
- The `.wav` extension does not alter the ZIP contents.
- The generated archive can be reopened and validated.
- Metadata reflects the actual exported categories and counts.
- A second application-level import can read the generated backup.
- No temporary or incomplete files are reported as successful backups.

### Restore tests

- Merge behavior preserves unrelated local data.
- Replace mode affects only the selected categories.
- Cancel makes no data changes.
- Invalid entries are rejected.
- A corrupt database snapshot does not overwrite the active database.
- Failed restores leave the previous valid state intact wherever rollback is supported.
- Unresolved audio references are reported.
- Duplicate playlists and records are handled according to documented rules.
- Missing categories do not erase local data accidentally.

### Legacy regression tests

- Existing legacy backups remain restorable.
- Existing legacy backup creation still works.
- Existing UI entry points remain available.
- No existing data model or playback functionality is unintentionally broken.

### Platform tests

Test with:

- Small and large libraries.
- Large archive entries.
- Insufficient storage.
- User cancellation.
- Revoked document access.
- Corrupted archives.
- Unknown versions.
- Duplicate filenames.
- Malformed JSON.
- Invalid SQLite databases.
- App process interruption.
- Android lifecycle changes.

Record which tests passed, failed, or could not be executed.

---

## 13. Implementation Instructions for Google AI Studio

Before coding:

1. Read the complete existing Pulse Player backup and restore implementation.
2. Inspect the attached reference archive, including representative portions of every major data file.
3. Identify the exact legacy format and all supported workflows.
4. Document the verified new archive schema.
5. Compare the source schema with the existing Room entities and repositories.
6. Identify data that can be restored directly, data requiring mapping, and data that cannot be restored safely.
7. Produce a concise implementation plan with affected files and tests.

Then implement the complete feature in the existing project.

Do not stop after producing a design, a sample archive writer, or a parser that only reads metadata.

Implement the complete supported workflow: creation, format detection, validation, preview, restore, error handling, rollback, and reporting.

Do not invent undocumented archive schemas. Where the supplied sample does not establish a schema, inspect the file further before implementing it. If compatibility cannot be verified for a particular category, report that limitation rather than pretending it works.

Do not generate dummy data or hardcode sample counts.

Do not replace the existing backup implementation with a simplified version.

Do not claim third-party compatibility merely because the app can open the archive it created itself.

After implementation:

- Build the application.
- Run the relevant tests.
- Fix compilation errors and regressions.
- Verify both backup systems independently.
- Verify that the reference archive is detected and parsed.
- Test an export-import round trip.
- Review the final changes for data-loss risks.
- Update project documentation.

Produce a completion report listing:

1. Files created or modified.
2. Legacy functionality preserved.
3. New archive features implemented.
4. Reference archive compatibility results.
5. Tests executed and their actual results.
6. Categories successfully restored.
7. Unsupported or partially supported categories.
8. Known limitations and any remaining work.

### Final acceptance rule

The feature is complete only when the existing legacy backup and restore system still works, the new compatible archive system works independently, the supplied `.wav`-named ZIP archive can be safely inspected and restored to the extent supported by its verified schema, and generated archives have been validated against the expected compatibility requirements.

Never sacrifice existing user data or claim successful restoration when the underlying data was not actually restored.
