package com.pulse.player.ui.screens

import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FolderOff
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.HourglassEmpty
import androidx.compose.material.icons.rounded.LibraryAdd
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.heightIn
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pulse.player.data.scan.PartialReason
import com.pulse.player.data.scan.ScanState
import com.pulse.player.data.scan.audioPermission
import com.pulse.player.playback.Track
import com.pulse.player.ui.components.Artwork
import com.pulse.player.ui.components.ErrorBannerCard
import com.pulse.player.ui.components.LocalBottomOverlayPadding
import com.pulse.player.ui.components.PillButton
import com.pulse.player.ui.components.SectionHeader
import com.pulse.player.ui.components.SquareCard
import com.pulse.player.ui.components.WidgetTile
import com.pulse.player.ui.components.formatTime
import com.pulse.player.ui.theme.Pulse
import com.pulse.player.ui.theme.TabularMono
import com.pulse.player.ui.viewmodel.MainViewModel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

@Composable
fun ForYouScreen(vm: MainViewModel) {
    val tracks by vm.tracks.collectAsStateWithLifecycle()
    val totalLibraryMs by vm.totalLibraryMs.collectAsStateWithLifecycle()
    val sleepRemainingMs by remember(vm) {
        vm.player.state.map { it.sleepRemainingMs }.distinctUntilChanged()
    }.collectAsStateWithLifecycle(initialValue = 0L)
    val today by vm.todayMinutes.collectAsStateWithLifecycle()
    val streak by vm.streakDays.collectAsStateWithLifecycle()
    val favCount by vm.favoriteCount.collectAsStateWithLifecycle()
    val scanning by vm.scanning.collectAsStateWithLifecycle(initialValue = false)
    val scanProgress by vm.scanProgress.collectAsStateWithLifecycle(initialValue = 0)
    val scanState by vm.scanState.collectAsStateWithLifecycle()
    val audioPermissionDenied by vm.audioPermissionDenied.collectAsStateWithLifecycle()
    val scanWarningDismissed by vm.scanWarningDismissed.collectAsStateWithLifecycle()
    val recent by vm.recentTracks.collectAsStateWithLifecycle()
    val top by vm.topPicks.collectAsStateWithLifecycle()
    val resumeTrack by vm.resume.collectAsStateWithLifecycle()
    val sectionOrder by vm.forYouSectionOrder.collectAsStateWithLifecycle()
    val hiddenSections by vm.hiddenForYouSections.collectAsStateWithLifecycle()
    val missingCount by vm.missingCount.collectAsStateWithLifecycle()
    val crashRecords by vm.crashRecords.collectAsStateWithLifecycle()
    val recentErrors by vm.recentErrors.collectAsStateWithLifecycle()
    val excludedFolders by vm.excludedFolders.collectAsStateWithLifecycle()
    val hiddenFavorites by vm.hiddenFavorites.collectAsStateWithLifecycle()
    val bottomPad = LocalBottomOverlayPadding.current

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        vm.setAudioPermissionDenied(!isGranted)
        if (isGranted) vm.scan()
    }

    if (tracks.isEmpty()) {
        EmptyLibraryCard(
            vm = vm,
            scanning = scanning,
            foundCount = scanProgress,
            audioPermissionDenied = audioPermissionDenied,
            onRequestPermission = { permissionLauncher.launch(audioPermission()) },
            bottomPad = bottomPad
        )
        return
    }

    val visibleSections = remember(sectionOrder, hiddenSections) {
        sectionOrder.filter { it !in hiddenSections }
    }

    LazyColumn(
        state = vm.lazyListStateFor("for_you"),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 12.dp, bottom = bottomPad),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        if (audioPermissionDenied) {
            item(key = "perm_banner") {
                Box(Modifier.padding(horizontal = 16.dp)) {
                    ErrorBannerCard(
                        icon = Icons.Rounded.LockOpen,
                        title = "Audio access needed",
                        message = "Grant media audio permission so Pulse Music Player can keep your library updated.",
                        primaryLabel = "Grant Access",
                        onPrimary = { permissionLauncher.launch(audioPermission()) }
                    )
                }
            }
        } else if (scanState is ScanState.Failed) {
            val err = scanState as ScanState.Failed
            item(key = "scan_err_banner") {
                Box(Modifier.padding(horizontal = 16.dp)) {
                    ErrorBannerCard(
                        icon = Icons.Rounded.ErrorOutline,
                        title = "Library scan issue",
                        message = err.message,
                        primaryLabel = "Retry Scan",
                        onPrimary = { vm.scan() },
                        secondaryLabel = "Diagnostics",
                        onSecondary = { vm.openDiagnostics() }
                    )
                }
            }
        } else if (scanState is ScanState.Partial && !scanWarningDismissed) {
            val partial = scanState as ScanState.Partial
            val partialMessage = when (partial.reason) {
                PartialReason.PermissionRevoked -> "Audio permission was revoked mid-scan. Grant permission and retry."
                PartialReason.Cancelled -> "The scan was interrupted before completing."
                PartialReason.QueryFailed -> "MediaStore returned an incomplete response. Previous index was kept."
                PartialReason.EmptyResultSuspicious -> "MediaStore returned no tracks unexpectedly; previous library was preserved."
                PartialReason.StorageUnavailable -> "External or SAF folder storage was temporarily unavailable."
            }
            item(key = "scan_partial_banner") {
                Box(Modifier.padding(horizontal = 16.dp)) {
                    ErrorBannerCard(
                        isError = false,
                        icon = Icons.Rounded.Warning,
                        title = "Library scan partially completed",
                        message = partialMessage,
                        primaryLabel = "Retry",
                        onPrimary = { vm.scan() },
                        secondaryLabel = "Dismiss",
                        onSecondary = { vm.dismissScanWarning() }
                    )
                }
            }
        }

        item(key = "library_stats_summary") {
            Box(Modifier.padding(horizontal = 16.dp)) {
                LibraryStatsSummaryCard(
                    trackCount = tracks.size,
                    totalDurationMs = totalLibraryMs,
                    favoriteCount = favCount,
                    todayMinutes = today,
                    streakDays = streak,
                    crashCount = crashRecords.size,
                    errorCount = recentErrors.size,
                    missingCount = missingCount,
                    excludedCount = excludedFolders.size,
                    hiddenFavCount = hiddenFavorites.size,
                    onOpenCrashLogs = { vm.openCrashLogs() },
                    onOpenDiagnostics = { vm.openDiagnostics() },
                    onOpenMissing = {
                        vm.showMissing.value = true
                        vm.selectHomeTab("Songs")
                    },
                    onOpenExcluded = { vm.go(Screen.ExcludedFolders) }
                )
            }
        }

        visibleSections.forEach { key ->
            when (key) {
                "resume" -> resumeTrack?.let { r ->
                    item(key = "sec_resume") {
                        Box(Modifier.padding(horizontal = 16.dp)) {
                            ResumeBanner(r) { vm.playQueue(listOf(r), 0) }
                        }
                    }
                }
                "actions" -> item(key = "sec_actions") {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            PillButton(
                                text = "Shuffle All",
                                icon = Icons.Rounded.Shuffle,
                                onClick = { vm.shuffleAll() },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("for_you_shuffle_all_button")
                            )
                            PillButton(
                                text = "Play All",
                                icon = Icons.Rounded.PlayArrow,
                                onClick = {
                                    if (tracks.isNotEmpty()) {
                                        vm.playQueue(tracks, 0)
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("for_you_play_all_button")
                            )
                        }
                        QuickShuffleShortcutsRow(vm)
                    }
                }
                "widgets" -> item(key = "sec_widgets") {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            WidgetTile(
                                title = "Today",
                                value = "$today min",
                                subtitle = if (streak > 0) "$streak day streak" else "Listening recap",
                                icon = Icons.Rounded.Headphones,
                                colors = listOf(Pulse.Violet, Pulse.Magenta),
                                modifier = Modifier.weight(1f)
                            ) {
                                vm.go(Screen.Recap)
                            }
                            WidgetTile(
                                title = "Favourites",
                                value = "$favCount songs",
                                subtitle = "Liked tracks",
                                icon = Icons.Rounded.Favorite,
                                colors = listOf(Pulse.Cyan, Pulse.Violet),
                                modifier = Modifier.weight(1f)
                            ) {
                                vm.go(Screen.Detail("system", "favorites"))
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            WidgetTile(
                                title = "Sleep timer",
                                value = if (sleepRemainingMs > 0) formatTime(sleepRemainingMs) else "Off",
                                subtitle = if (sleepRemainingMs > 0) "Active" else "Set countdown",
                                icon = Icons.Rounded.Bedtime,
                                colors = listOf(Pulse.Coral, Pulse.Pink),
                                modifier = Modifier.weight(1f)
                            ) {
                                vm.openSleepSheet()
                            }
                            WidgetTile(
                                title = "Equalizer",
                                value = "Tune sound",
                                subtitle = "5-band EQ & bass",
                                icon = Icons.Rounded.Equalizer,
                                colors = listOf(Pulse.Mint, Pulse.Cyan),
                                modifier = Modifier.weight(1f)
                            ) {
                                vm.go(Screen.Equalizer)
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            WidgetTile(
                                title = "Voice & Lyrics",
                                value = "Transcribe",
                                subtitle = "Audio to synced text",
                                icon = Icons.Rounded.Mic,
                                colors = listOf(Pulse.Coral, Pulse.Magenta),
                                modifier = Modifier.weight(1f)
                            ) {
                                vm.go(Screen.Transcribe)
                            }
                            WidgetTile(
                                title = "Music Studio",
                                value = "Create Track",
                                subtitle = "Original audio ideas",
                                icon = Icons.Rounded.AutoAwesome,
                                colors = listOf(Pulse.Cyan, Pulse.Violet),
                                modifier = Modifier.weight(1f)
                            ) {
                                vm.go(Screen.GenerateMusic)
                            }
                        }
                    }
                }
                "recent" -> if (recent.isNotEmpty()) {
                    item(key = "sec_recent_header") {
                        Box(Modifier.padding(horizontal = 16.dp)) {
                            SectionHeader("Recently added", badge = "New", action = "Play all") {
                                vm.playQueue(recent, 0)
                            }
                        }
                    }
                    item(key = "sec_recent_list") {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(recent, key = { it.id }) { t ->
                                SquareCard(t, 132.dp) {
                                    vm.playQueue(recent, recent.indexOf(t))
                                }
                            }
                        }
                    }
                }
                "top_picks" -> if (top.isNotEmpty()) {
                    item(key = "sec_top_header") {
                        Box(Modifier.padding(horizontal = 16.dp)) {
                            SectionHeader("Top picks for you")
                        }
                    }
                    item(key = "sec_top_list") {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(top, key = { it.id }) { t ->
                                SquareCard(t, 168.dp) {
                                    vm.playQueue(top, top.indexOf(t))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LibraryStatsSummaryCard(
    trackCount: Int,
    totalDurationMs: Long,
    favoriteCount: Int,
    todayMinutes: Int,
    streakDays: Int,
    crashCount: Int,
    errorCount: Int,
    missingCount: Int,
    excludedCount: Int,
    hiddenFavCount: Int,
    onOpenCrashLogs: () -> Unit,
    onOpenDiagnostics: () -> Unit,
    onOpenMissing: () -> Unit,
    onOpenExcluded: () -> Unit
) {
    val totalMin = (totalDurationMs / 60_000L).coerceAtLeast(0L)
    val hrs = totalMin / 60L
    val mins = totalMin % 60L
    val durationLabel = if (hrs > 0) "${hrs}h ${mins}m" else "${mins}m"
    val hasHealthAlerts = crashCount > 0 || errorCount > 0 || missingCount > 0 || excludedCount > 0 || hiddenFavCount > 0

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f)),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatPill(label = "Library", value = "$trackCount", sub = durationLabel, modifier = Modifier.weight(1f))
                StatPill(label = "Favourites", value = "$favoriteCount", sub = "saved", modifier = Modifier.weight(1f))
                StatPill(
                    label = "Today",
                    value = "${todayMinutes}m",
                    sub = if (streakDays > 0) "$streakDays day streak" else "local play",
                    modifier = Modifier.weight(1f)
                )
            }
            if (hasHealthAlerts) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (crashCount > 0) {
                        AssistChip(
                            onClick = onOpenCrashLogs,
                            label = { Text("$crashCount Crash Log${if (crashCount == 1) "" else "s"}", style = MaterialTheme.typography.labelSmall.merge(TabularMono)) },
                            leadingIcon = { Icon(Icons.Rounded.BugReport, null, Modifier.size(16.dp)) },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                labelColor = MaterialTheme.colorScheme.onErrorContainer,
                                leadingIconContentColor = MaterialTheme.colorScheme.error
                            ),
                            border = null,
                            modifier = Modifier.testTag("health_crash_chip")
                        )
                    }
                    if (errorCount > 0) {
                        AssistChip(
                            onClick = onOpenDiagnostics,
                            label = { Text("$errorCount Alert${if (errorCount == 1) "" else "s"}", style = MaterialTheme.typography.labelSmall.merge(TabularMono)) },
                            leadingIcon = { Icon(Icons.Rounded.Warning, null, Modifier.size(16.dp)) },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = MaterialTheme.colorScheme.surface,
                                labelColor = MaterialTheme.colorScheme.onSurface,
                                leadingIconContentColor = Pulse.Coral
                            ),
                            border = AssistChipDefaults.assistChipBorder(enabled = true, borderColor = MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.testTag("health_errors_chip")
                        )
                    }
                    if (missingCount > 0) {
                        AssistChip(
                            onClick = onOpenMissing,
                            label = { Text("$missingCount Missing", style = MaterialTheme.typography.labelSmall.merge(TabularMono)) },
                            leadingIcon = { Icon(Icons.Rounded.ErrorOutline, null, Modifier.size(16.dp)) },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = MaterialTheme.colorScheme.surface,
                                labelColor = MaterialTheme.colorScheme.onSurface,
                                leadingIconContentColor = MaterialTheme.colorScheme.primary
                            ),
                            border = AssistChipDefaults.assistChipBorder(enabled = true, borderColor = MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.testTag("health_missing_chip")
                        )
                    }
                    if (excludedCount > 0 || hiddenFavCount > 0) {
                        val exclText = if (hiddenFavCount > 0) {
                            "$excludedCount Excluded · $hiddenFavCount Fav Hidden"
                        } else {
                            "$excludedCount Excluded"
                        }
                        AssistChip(
                            onClick = onOpenExcluded,
                            label = { Text(exclText, style = MaterialTheme.typography.labelSmall.merge(TabularMono)) },
                            leadingIcon = { Icon(Icons.Rounded.FolderOff, null, Modifier.size(16.dp)) },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = MaterialTheme.colorScheme.surface,
                                labelColor = MaterialTheme.colorScheme.onSurface,
                                leadingIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            border = AssistChipDefaults.assistChipBorder(enabled = true, borderColor = MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.testTag("health_excluded_chip")
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatPill(label: String, value: String, sub: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.merge(TabularMono),
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = sub,
            style = MaterialTheme.typography.labelSmall.merge(TabularMono),
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun QuickShuffleShortcutsRow(vm: MainViewModel) {
    val chipColors = AssistChipDefaults.assistChipColors(
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        labelColor = MaterialTheme.colorScheme.onSurface,
        leadingIconContentColor = MaterialTheme.colorScheme.primary
    )
    val chipBorder = AssistChipDefaults.assistChipBorder(
        enabled = true,
        borderColor = MaterialTheme.colorScheme.outlineVariant
    )
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        AssistChip(
            onClick = { vm.shuffleFavorites() },
            label = { Text("Shuffle Favourites") },
            leadingIcon = { Icon(Icons.Rounded.Favorite, null, Modifier.size(16.dp)) },
            colors = chipColors,
            border = chipBorder
        )
        AssistChip(
            onClick = { vm.shuffleUnplayed() },
            label = { Text("Unplayed Mix") },
            leadingIcon = { Icon(Icons.Rounded.HourglassEmpty, null, Modifier.size(16.dp)) },
            colors = chipColors,
            border = chipBorder
        )
        AssistChip(
            onClick = { vm.shuffleRecentlyAdded() },
            label = { Text("Recently Added") },
            leadingIcon = { Icon(Icons.Rounded.LibraryAdd, null, Modifier.size(16.dp)) },
            colors = chipColors,
            border = chipBorder
        )
    }
}

@Composable
fun ResumeBanner(track: Track, onPlay: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .border(
                BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f)),
                RoundedCornerShape(20.dp)
            )
            .clickable(onClick = onPlay)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Artwork(track.uri, 54.dp, radius = 12.dp, px = 160)
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = "Continue listening",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = track.title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = track.artist,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.PlayArrow,
                    contentDescription = "Play",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

@Composable
fun EmptyLibraryCard(
    vm: MainViewModel,
    scanning: Boolean = false,
    foundCount: Int = 0,
    audioPermissionDenied: Boolean = false,
    onRequestPermission: (() -> Unit)? = null,
    bottomPad: androidx.compose.ui.unit.Dp = 24.dp
) {
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        vm.setAudioPermissionDenied(!isGranted)
        if (isGranted) vm.scan()
    }
    val safLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            vm.addSafFolder(uri)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = bottomPad.coerceAtLeast(24.dp)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .border(
                    BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f)),
                    RoundedCornerShape(24.dp)
                )
                .padding(24.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Headphones, null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(32.dp))
            }
            Text(
                text = when {
                    scanning -> "Scanning your music library…"
                    audioPermissionDenied -> "Audio permission needed"
                    else -> "Your music library is empty"
                },
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Text(
                text = if (scanning) {
                    if (foundCount > 0) "Found $foundCount tracks so far. Pulse Music Player is still checking your storage." else "Pulse Music Player is checking your storage. This can take a moment on a large library."
                } else {
                    "Pulse Music Player plays local audio stored on your phone or SD card. Grant audio access or choose a Music folder to begin."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            if (scanning) {
                androidx.compose.material3.LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.outlineVariant
                )
            }
            Spacer(Modifier.height(4.dp))
            Button(
                onClick = { vm.seedSampleTrack() },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .testTag("empty_sample_button"),
                shape = CircleShape,
                enabled = !scanning,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Icon(Icons.Rounded.PlayArrow, null)
                Spacer(Modifier.width(8.dp))
                Text("Load Sample Track (Pulse Horizon)", fontWeight = FontWeight.Bold)
            }
            Button(
                onClick = {
                    if (onRequestPermission != null) {
                        onRequestPermission()
                    } else {
                        val perm = if (Build.VERSION.SDK_INT >= 33) {
                            android.Manifest.permission.READ_MEDIA_AUDIO
                        } else {
                            android.Manifest.permission.READ_EXTERNAL_STORAGE
                        }
                        permissionLauncher.launch(perm)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .testTag("empty_scan_button"),
                shape = CircleShape,
                enabled = !scanning,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                Icon(Icons.Rounded.Refresh, null)
                Spacer(Modifier.width(8.dp))
                Text("Scan Music Library", fontWeight = FontWeight.Bold)
            }
            OutlinedButton(
                onClick = { safLauncher.launch(null) },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .testTag("empty_folder_button"),
                shape = CircleShape,
                enabled = !scanning
            ) {
                Icon(Icons.Rounded.FolderOpen, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text("Choose Music Folder", color = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}
