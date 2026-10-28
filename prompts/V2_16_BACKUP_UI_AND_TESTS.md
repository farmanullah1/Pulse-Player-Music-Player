# V2-16 — Backup UI (both types), ViewModel, Tests

> Read V2_13, V2_14, V2_15 first. Output complete files. Every button must work or be disabled with a reason. No fake progress: progress values come from `BackupWriter` / `BackupRestorer` callbacks only.

## 0. Files
```
ui/screens/BackupScreen.kt      (replaces the Backup & Transfer screen from file 07 §6; keeps the Favourites JSON card)
ui/BackupViewModel.kt
src/test/.../PathKeyTest.kt, TrackMatcherTest.kt, SniffTest.kt, SafeNameTest.kt     (plain JVM)
src/androidTest/.../ArchiveRoundTripTest.kt, ArchiveRejectTest.kt, RestoreIdempotentTest.kt   (needs device/emulator)
```
Navigation: `Screen.Backup` already exists in the shell (drawer item "Backup & Transfer" and Settings row). Do not add a second entry.

## 1. SAF quirks (important, these cause real bugs)
- Create file: use `ActivityResultContracts.CreateDocument("application/octet-stream")`. With `application/zip` some providers append `.zip` and you get `x.pulsebackup.zip`.
- Pick file: `ActivityResultContracts.OpenDocument()` with `arrayOf("*/*")`. Do NOT filter by MIME: a `.wav`-named ZIP would be hidden by an `audio/*` or `application/zip` filter.
- Never request storage permissions for backup. SAF only.

## 2. BackupViewModel.kt
```kotlin
package com.pulse.player.ui
import android.app.Application
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pulse.player.PulseApp
import com.pulse.player.data.backup.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.File

sealed interface BackupUi {
    object Idle : BackupUi
    data class Working(val title: String, val fraction: Float, val step: String, val cancellable: Boolean = true) : BackupUi
    data class ExportDone(val r: ExportResult) : BackupUi
    data class Preview(val p: RestorePreview, val file: File, val name: String) : BackupUi
    data class RestoreDone(val r: RestoreReport) : BackupUi
    data class Error(val message: String) : BackupUi
}

class BackupViewModel(app: Application) : AndroidViewModel(app) {
    private val a = app as PulseApp
    private val writer = BackupWriter(app, a.db, a.settings)
    private val restorer = BackupRestorer(app, a.db, a.settings)
    private val _ui = MutableStateFlow<BackupUi>(BackupUi.Idle); val ui = _ui.asStateFlow()
    private val _last = MutableStateFlow<String?>(null); val lastBackupJson = _last.asStateFlow()
    val exportCats = MutableStateFlow(setOf(BackupCategory.TAGS, BackupCategory.PLAYLISTS, BackupCategory.FAVORITES,
        BackupCategory.SORT, BackupCategory.SCAN, BackupCategory.STATS, BackupCategory.APP_SETTINGS))
    val wavName = MutableStateFlow(false)
    private var job: Job? = null
    init { viewModelScope.launch { _last.value = a.settings.lastBackup() } }

    fun toggleCat(c: BackupCategory) { if (c.supported) exportCats.update { if (c in it) it - c else it + c } }
    fun suggestedName(): String = writer.suggestedName(ExportOptions(exportCats.value, wavName.value))

    fun startExport(dest: Uri) {
        job?.cancel()
        job = viewModelScope.launch {
            _ui.value = BackupUi.Working("Creating backup", 0f, "Starting")
            val opts = ExportOptions(exportCats.value, wavName.value)
            try {
                val r = writer.export(dest, opts) { f, s -> _ui.value = BackupUi.Working("Creating backup", f, s, cancellable = f < 0.88f) }
                _ui.value = r.fold(
                    { ok -> val real = DocumentFile.fromSingleUri(getApplication(), dest)?.name ?: ok.fileName
                        _last.value = a.settings.lastBackup(); BackupUi.ExportDone(ok.copy(fileName = real)) },
                    { BackupUi.Error(it.message ?: "Backup failed") })
            } catch (e: CancellationException) { _ui.value = BackupUi.Idle; throw e }
        }
    }

    fun pickForRestore(uri: Uri) {
        job?.cancel()
        job = viewModelScope.launch {
            _ui.value = BackupUi.Working("Checking file", 0.1f, "Reading file")
            val ctx = getApplication<Application>(); val name = BackupFiles.displayName(ctx, uri)
            val res = withContext(Dispatchers.IO) { runCatching { BackupFiles.copyToTemp(ctx, uri) }
                .mapCatching { f -> f to BackupInspector(ctx).inspectFile(f, name) } }
            res.onFailure { _ui.value = BackupUi.Error(it.message ?: "Could not read the file.") }
            res.onSuccess { (f, r) -> when (r) {
                is InspectResult.Ok -> _ui.value = BackupUi.Preview(r.preview, f, name)
                is InspectResult.Rejected -> { f.delete(); _ui.value = BackupUi.Error(r.reason + " Nothing was changed.") } } }
        }
    }

    fun restore(p: BackupUi.Preview, opts: RestoreOptions) {
        job?.cancel()
        job = viewModelScope.launch {
            _ui.value = BackupUi.Working("Restoring", 0f, "Starting", cancellable = true)
            try {
                val r = restorer.restore(p.file, p.name, opts) { f, s -> _ui.value = BackupUi.Working("Restoring", f, s, cancellable = f < 0.58f) }
                _ui.value = r.fold({ BackupUi.RestoreDone(it) }, { BackupUi.Error(it.message ?: "Restore failed") })
            } catch (e: CancellationException) { _ui.value = BackupUi.Idle; throw e }
            finally { p.file.delete() }
        }
    }
    fun cancel() { job?.cancel(); (ui.value as? BackupUi.Preview)?.file?.delete(); _ui.value = BackupUi.Idle }
    fun dismiss() { (ui.value as? BackupUi.Preview)?.file?.delete(); _ui.value = BackupUi.Idle }
}
```
Cancellation notes: before the DB transaction starts a cancel changes nothing; once the transaction starts (`fraction >= 0.6`) the cancel button is hidden because the transaction is short and atomic.

## 3. BackupScreen.kt
```kotlin
package com.pulse.player.ui.screens
// imports: androidx.compose.*, material3, material icons extended (Backup, Restore, Upload, Download, Shield, History, Folder, Check, Warning, Close), lifecycle compose

@Composable fun BackupScreen(onBack: () -> Unit, vm: BackupViewModel = viewModel()) {
    val ui by vm.ui.collectAsStateWithLifecycle(); val cats by vm.exportCats.collectAsStateWithLifecycle()
    val wav by vm.wavName.collectAsStateWithLifecycle(); val last by vm.lastBackupJson.collectAsStateWithLifecycle()
    val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { it?.let(vm::startExport) }
    val pick = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { it?.let(vm::pickForRestore) }

    Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) { IconButton(onBack) { Icon(Icons.Rounded.ArrowBack, "Back") }
            Text("Backup & Transfer", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold) }
        Spacer(Modifier.height(16.dp))

        // ---- Card 1: Favourites file (JSON) -> keep the existing composable from file 07 unchanged
        FavoritesJsonCard()
        Spacer(Modifier.height(24.dp))

        // ---- Card 2: Full backup (archive)
        GradientCard(listOf(Pulse.Violet.copy(.35f), Pulse.Magenta.copy(.25f))) {
            Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Rounded.Shield, null, tint = Color.White); Spacer(Modifier.width(8.dp))
                Text("Full backup (archive)", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
            Text("One file with your playlists, favourites, listening history and settings. Your music files are NOT included. The file is not encrypted.",
                color = Pulse.TextMid, modifier = Modifier.padding(top = 8.dp))
            Spacer(Modifier.height(12.dp)); Text("Include", fontWeight = FontWeight.SemiBold)
            BackupCategory.values().forEach { c -> Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable(enabled = c.supported) { vm.toggleCat(c) }, verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = c in cats && c.supported, onCheckedChange = { vm.toggleCat(c) }, enabled = c.supported)
                Column { Text(c.label); if (!c.supported) Text("Not supported yet", style = MaterialTheme.typography.labelSmall, color = Pulse.TextLow) } } }
            Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) { Text("Name the file .wav (compatibility)")
                    Text("Same ZIP inside. Keep it outside your Music folder or it may show as a broken song.", style = MaterialTheme.typography.labelSmall, color = Pulse.TextMid) }
                Switch(wav, { vm.wavName.value = it }) }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button({ export.launch(vm.suggestedName()) }, enabled = cats.any { it.supported } && ui is BackupUi.Idle, shape = CircleShape, modifier = Modifier.weight(1f).height(52.dp)) {
                    Icon(Icons.Rounded.Upload, null); Spacer(Modifier.width(8.dp)); Text("Create backup") }
                OutlinedButton({ pick.launch(arrayOf("*/*")) }, enabled = ui is BackupUi.Idle, shape = CircleShape, modifier = Modifier.weight(1f).height(52.dp)) {
                    Icon(Icons.Rounded.Download, null); Spacer(Modifier.width(8.dp)); Text("Restore") } }
            last?.let { LastBackupLine(it) }          // parse JSON {file,size,time,counts}; shown ONLY from a completed export
        }
        Spacer(Modifier.height(24.dp)); TransferGuideCard(); Spacer(Modifier.height(96.dp))
    }
    BackupDialogs(ui, vm)
}

@Composable fun GradientCard(colors: List<Color>, content: @Composable ColumnScope.() -> Unit) =
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Brush.linearGradient(colors)).background(Pulse.Surface.copy(.6f)).padding(16.dp), content = content)
```
`LastBackupLine`: parses the stored JSON with `org.json`, shows file name, `Formatter.formatShortFileSize`, local time, and counts. If parsing fails show nothing.
`TransferGuideCard`: the 4 steps from file 07 §1, plus a 5th: "On the new device open Settings -> Backup & Transfer -> Restore, pick the backup, check the preview, tap Restore."

### Dialogs (state driven by `BackupUi`)
```kotlin
@Composable fun BackupDialogs(ui: BackupUi, vm: BackupViewModel) {
    when (ui) {
        is BackupUi.Working -> AlertDialog(onDismissRequest = {}, title = { Text(ui.title) },
            text = { Column { LinearProgressIndicator(progress = { ui.fraction }, Modifier.fillMaxWidth()); Spacer(Modifier.height(8.dp)); Text(ui.step) } },
            confirmButton = {}, dismissButton = { if (ui.cancellable) TextButton(vm::cancel) { Text("Cancel") } })
        is BackupUi.ExportDone -> AlertDialog(onDismissRequest = vm::dismiss, icon = { Icon(Icons.Rounded.Check, null, tint = Pulse.Mint) },
            title = { Text("Backup saved") }, confirmButton = { TextButton(vm::dismiss) { Text("Done") } },
            text = { Column { Text(ui.r.fileName, fontWeight = FontWeight.SemiBold); Text(Formatter.formatShortFileSize(LocalContext.current, ui.r.sizeBytes))
                Text("Checksums verified. ${ui.r.counts.entries.joinToString(" · ") { "${it.value} ${it.key}" }}")
                ui.r.warnings.forEach { Text(it, color = Pulse.Orange) } } })
        is BackupUi.Error -> AlertDialog(onDismissRequest = vm::dismiss, icon = { Icon(Icons.Rounded.Warning, null, tint = Pulse.Error) },
            title = { Text("That did not work") }, text = { Text(ui.message) }, confirmButton = { TextButton(vm::dismiss) { Text("OK") } })
        is BackupUi.Preview -> RestorePreviewSheet(ui, vm)
        is BackupUi.RestoreDone -> RestoreReportSheet(ui.r, vm::dismiss)
        BackupUi.Idle -> Unit
    }
}
```

### RestorePreviewSheet (ModalBottomSheet)
Content, top to bottom (16dp padding, 12dp gaps):
1. Title "Restore preview". Chip: `Pulse backup` or `Reference-style archive`.
2. File name, backup time (parse `yyyyMMdd_HHmmssSSS` or ISO; if parsing fails show the raw string), app version.
3. Counts row: `{songs} songs · {playlists} playlists · {favourites} favourites · {historyRecords} history records`. Label them "in the backup".
4. Integrity line: `checksumsVerified` -> green "Checksums verified"; else amber with `checksumNote`.
5. Warnings list (amber).
6. "What to restore" checkboxes: only categories in `preview.present` (all ticked by default except APP_SETTINGS when kind is REFERENCE: not present anyway). Each row shows its label.
7. Radio: **Merge (recommended)** "Keeps what you have and adds what is missing." / **Replace selected** "Replaces the ticked parts with the backup." Choosing Replace and pressing Restore opens a confirm dialog: "Replace? This removes your current {labels} and uses the backup instead. Music files are not touched. This cannot be undone." with buttons **Replace** (red) and **Cancel**.
8. Buttons: **Restore** (disabled when nothing ticked) and **Cancel** (calls `vm.dismiss()` which deletes the temp file; nothing changes).
9. Small print: "Songs are matched by file name and folder, then by title/artist/album. Songs that are not on this device are listed afterwards."

### RestoreReportSheet
Per category row: name, `imported`, `updated`, `skipped (already there)`, `not found`, `ambiguous`. Under it: warnings and, if `unmatchedPaths` is not empty, an expandable list "Songs not found on this device ({n})" showing only the file name part (`PathKey.nameOf`) and a **Copy list** button (`ClipboardManager`). Headline rule: if any `unmatched` or `ambiguous` > 0, headline is "Restored with some songs missing"; never "Everything restored". Hint line: "Copy those songs to this device, rescan, then restore again. Already-restored items are skipped."

## 4. Scanner guard (required, from V2_13 section 4)
In `LibraryScanner.scanMediaStore` (and SAF scan) add: `if (durationMs <= 0 || sizeBytes < 1024) continue`. Add a unit test for the predicate `isPlayableCandidate(duration, size)`.

## 5. Tests
### Plain JVM (no Android classes)
```kotlin
class SniffTest { @Test fun zip() = assertTrue(BackupFiles.sniff(byteArrayOf(0x50,0x4B,3,4,0,0,0,0,0,0,0,0)) is BackupFiles.Sniff.Zip)
  @Test fun wav() = assertTrue(BackupFiles.sniff("RIFF....WAVEfmt ".toByteArray(Charsets.ISO_8859_1)) is BackupFiles.Sniff.Wav)
  @Test fun random() = assertTrue(BackupFiles.sniff(ByteArray(16)) is BackupFiles.Sniff.Other) }
class SafeNameTest {
  @Test fun bad() { listOf("../a", "/etc/x", "a\\b", "a/../b", "C:/x", "a\u0000b").forEach { assertFalse(it, isSafeEntryName(it)) } }
  @Test fun good() { listOf("settings.bak", "Covers/AlbumCoversBackupConfig.conf", "PulseExtra/manifest.json").forEach { assertTrue(it, isSafeEntryName(it)) } } }
class PathKeyTest {
  @Test fun volumeIgnored() = assertEquals(PathKey.fromPath("/storage/emulated/0/Music/a.mp3"), PathKey.fromPath("/storage/1234-ABCD/Music/a.mp3"))
  @Test fun trackKey() { val t = track(displayName = "a.mp3", folderPath = "Music/Rock"); assertEquals(PathKey.fromPath("/storage/emulated/0/Music/Rock/a.mp3"), PathKey.fromTrack(t)) } }
class TrackMatcherTest {
  @Test fun movedVolumeMatchesByPath() {...}                    // path on other volume still PATH
  @Test fun sameNameTwoFoldersIsAmbiguousNotGuessed() {...}     // NAME with 2 candidates -> AMBIGUOUS, track == null
  @Test fun titleOnlyNeverMatches() {...}                       // different artist+album -> MISSING
  @Test fun tagFallbackWorksWhenRenamed() {...}                 // file renamed, tag hint matches -> TAGS
  @Test fun unknownArtistNormalised() {...}                     // "<unknown>" == "Unknown artist" }
```
`isSafeEntryName` is a top-level function (file 15) so it is testable without a Context. Fill in the `...` bodies fully; do not leave them empty.

### Instrumented (device/emulator; uses `android.util.JsonReader`, SQLite, Room in-memory)
Create fixtures **in code** with `ZipOutputStream` (do NOT ship the user's real backup: it contains a private song list). Helper `buildReferenceZip(playlists, records, tags)` writes `metadata.inf`, `PlayLists/PlayListBackupConfig.conf`, `MusicTagsBackupConfig.conf`, `Statistics/PlaybackRecords.json` with 3 tracks.
1. `ArchiveRoundTripTest`: seed in-memory DB (3 tracks, 1 favourite, 1 playlist, 5 events) -> `BackupWriter.export` to a temp Uri -> `BackupInspector` returns `Ok` with `checksumsVerified` and matching counts -> clear DB -> `BackupRestorer.restore(Merge)` -> DB equals the seed.
2. `ArchiveRejectTest`: WAV bytes rejected; truncated ZIP rejected; ZIP with `../evil` rejected; Pulse ZIP with one byte changed in `PlayLists/...` rejected (checksum); `formatVersion = 99` rejected; `containsAudioFiles = true` rejected. After each, DB row counts unchanged.
3. `RestoreIdempotentTest`: restore the same reference-style ZIP twice -> second run has `imported == 0` for STATS and playlists, `skipped > 0`.
4. `ReplaceOnlySelectedTest`: Replace with only FAVORITES ticked leaves playlists and events untouched.
5. `UnmatchedReportTest`: archive references a song not in the library -> report `unmatched == 1`, nothing invented.
6. `ReferenceSampleShapeTest`: parse a tiny reference-shaped ZIP that includes `<unknown>` artist, `genres: null`, `cover: null`, and `hidden_songs` -> parser does not crash; hidden songs produce the warning.
Run with `./gradlew testDebugUnitTest connectedDebugAndroidTest`. If the model cannot run instrumented tests, it must write them anyway and mark them **NOT RUN** in `brain.md`.

## 6. Acceptance
Both cards visible; Favourites JSON still works exactly as in file 07; archive export writes a verified file; picking the user's real `.wav` backup shows a truthful preview ("Reference-style archive", 13,509 songs, 41 playlists, 2,319 records, no checksums warning); Replace requires the confirm dialog; Cancel/Dismiss leaves data unchanged; the report never overstates; temp files are removed; `brain.md` updated with PASS / FAIL / NOT RUN per test.
