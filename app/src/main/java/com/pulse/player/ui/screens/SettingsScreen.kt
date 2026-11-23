package com.pulse.player.ui.screens

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pulse.player.ui.components.LocalBottomOverlayPadding
import com.pulse.player.ui.components.PulseTopBar
import com.pulse.player.ui.viewmodel.MainViewModel

@Composable
fun SettingsScreen(vm: MainViewModel) {
    val themeMode by vm.themeMode.collectAsStateWithLifecycle()
    val fontStyle by vm.fontStyle.collectAsStateWithLifecycle()
    val listDensity by vm.listDensity.collectAsStateWithLifecycle()
    val reducedMotion by vm.reducedMotion.collectAsStateWithLifecycle()
    val fadeEnabled by vm.fadeEnabled.collectAsStateWithLifecycle()
    val skinId by vm.skinId.collectAsStateWithLifecycle()
    val playerLayout by vm.activePlayerLayout.collectAsStateWithLifecycle()
    val onlineFeaturesEnabled by vm.onlineFeaturesEnabled.collectAsStateWithLifecycle()
    val offlineOnlyMode by vm.offlineOnlyMode.collectAsStateWithLifecycle()
    val playerState by vm.player.state.collectAsStateWithLifecycle()
    val missingCount by vm.missingCount.collectAsStateWithLifecycle()
    val crossfadeSeconds by vm.crossfadeSeconds.collectAsStateWithLifecycle()
    val homeTabsList by vm.homeTabsList.collectAsStateWithLifecycle()
    val sleepFadeSec by vm.sleepFadeSec.collectAsStateWithLifecycle()
    val excludedFolders by vm.excludedFolders.collectAsStateWithLifecycle()
    val hiddenFavorites by vm.hiddenFavorites.collectAsStateWithLifecycle()
    val favCount by vm.favoriteCount.collectAsStateWithLifecycle()
    val fxStatus by vm.fxStatus.collectAsStateWithLifecycle()
    val replayGainMode by vm.replayGainMode.collectAsStateWithLifecycle()
    val replayGainPreampDb by vm.replayGainPreampDb.collectAsStateWithLifecycle()
    val crossfadeEqualPower by vm.crossfadeEqualPower.collectAsStateWithLifecycle()
    val smartAlbumGapless by vm.smartAlbumGapless.collectAsStateWithLifecycle()
    val autoResumeOnDeviceConnect by vm.autoResumeOnDeviceConnect.collectAsStateWithLifecycle()
    val drivingKeepScreenOn by vm.drivingKeepScreenOn.collectAsStateWithLifecycle()
    val crashRecords by vm.crashRecords.collectAsStateWithLifecycle()
    val recentErrors by vm.recentErrors.collectAsStateWithLifecycle()
    val splitMultiArtists by vm.splitMultiArtists.collectAsStateWithLifecycle()
    val artistSplitExceptions by vm.artistSplitExceptions.collectAsStateWithLifecycle()

    var inputKey by remember { mutableStateOf("") }
    val searchQuery by vm.settingsSearchQuery.collectAsStateWithLifecycle()
    val selectedCategoryName by vm.settingsCategoryName.collectAsStateWithLifecycle()
    val selectedCategory = SettingsCategory.entries.firstOrNull { it.name == selectedCategoryName } ?: SettingsCategory.ALL
    var showCleanConfirmDialog by remember { mutableStateOf(false) }
    var showHomeTabsDialog by remember { mutableStateOf(false) }
    var showExceptionsDialog by remember { mutableStateOf(false) }

    BackHandler {
        if (searchQuery.isNotBlank() || selectedCategory != SettingsCategory.ALL) {
            vm.settingsSearchQuery.value = ""
            vm.settingsCategoryName.value = SettingsCategory.ALL.name
        } else {
            vm.back()
        }
    }

    val safLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) vm.addSafFolder(uri)
    }

    val bottomInset = LocalBottomOverlayPadding.current
    val normalizedQuery = searchQuery.trim().lowercase()

    fun matchesSection(category: SettingsCategory, vararg keywords: String): Boolean {
        val catOk = selectedCategory == SettingsCategory.ALL || selectedCategory == category
        if (!catOk) return false
        if (normalizedQuery.isEmpty()) return true
        return keywords.any { it.contains(normalizedQuery, ignoreCase = true) }
    }

    val showAppearance = matchesSection(
        SettingsCategory.APPEARANCE,
        "appearance", "theme mode", "dark", "light", "amoled", "system",
        "typography", "font", "density", "compact", "comfortable",
        "skin", "wallpaper", "player layout", "customize", "home tabs", "reduced motion"
    )
    val showLibrary = matchesSection(
        SettingsCategory.LIBRARY,
        "library", "rescan", "scan", "folder", "music folder", "sd card",
        "excluded folders", "duplicate", "duplicates", "artist", "split", "exceptions"
    )
    val showPlayback = matchesSection(
        SettingsCategory.PLAYBACK,
        "playback", "equalizer", "eq", "sound effects", "sleep timer", "fade",
        "crossfade", "equal power", "gapless", "driving", "speed", "pitch", "a-b loop", "loop", "audio controls",
        "stop after current track", "skip silence", "replaygain", "loudness", "preamp", "analyze",
        "bluetooth", "headset", "device profile", "auto-resume", "bookmarks", "limiter", "mono", "balance"
    )
    val showRecap = matchesSection(SettingsCategory.TOOLS, "history", "statistics", "stats", "recap", "listening time", "top tracks")
    val showBackup = matchesSection(SettingsCategory.TOOLS, "backup", "transfer", "restore", "favourites", "favorites", "export", "import")
    val showWidgets = matchesSection(SettingsCategory.TOOLS, "widget", "widgets", "home screen widget")
    val showStudio = matchesSection(SettingsCategory.TOOLS, "voice", "lyrics", "transcribe", "music studio", "api key", "gemini")
    val showPrivacy = matchesSection(SettingsCategory.SYSTEM, "online", "privacy", "offline", "network")
    val showDiagnostics = matchesSection(SettingsCategory.SYSTEM, "system", "diagnostics", "error log", "clean missing tracks", "hardware", "permissions")
    val showAbout = matchesSection(SettingsCategory.SYSTEM, "about", "version", "pulse player", "pulse music player", "privacy")

    val anyToolsOrSystemVisible = showRecap || showBackup || showWidgets || showPrivacy || showStudio || showDiagnostics || showAbout
    val anySectionVisible = showAppearance || showLibrary || showPlayback || anyToolsOrSystemVisible

    val chipColors = FilterChipDefaults.filterChipColors(
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        labelColor = MaterialTheme.colorScheme.onSurface,
        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
    )

    Column(Modifier.fillMaxSize()) {
        PulseTopBar(title = "Settings", onBack = { vm.back() })

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 6.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { vm.settingsSearchQuery.value = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .testTag("settings_search_input"),
                placeholder = {
                    Text(
                        "Search settings, loudness, artists, EQ…",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { vm.settingsSearchQuery.value = "" }, modifier = Modifier.size(48.dp)) {
                            Icon(Icons.Rounded.Clear, contentDescription = "Clear search", tint = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                },
                singleLine = true,
                shape = CircleShape,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                )
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SettingsCategory.entries.forEach { category ->
                    val isSelected = selectedCategory == category
                    FilterChip(
                        selected = isSelected,
                        onClick = { vm.settingsCategoryName.value = category.name },
                        label = { Text(category.label) },
                        colors = chipColors,
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = MaterialTheme.colorScheme.outlineVariant,
                            selectedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                        )
                    )
                }
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))

        if (!anySectionVisible) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically)
            ) {
                Icon(
                    imageVector = Icons.Rounded.SearchOff,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                    modifier = Modifier.size(48.dp)
                )
                Text("No matching settings", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Text(
                    "Try searching for theme, equalizer, loudness, crossfade, artist, or backup.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                OutlinedButton(
                    onClick = {
                        vm.settingsSearchQuery.value = ""
                        vm.settingsCategoryName.value = SettingsCategory.ALL.name
                    },
                    shape = CircleShape,
                    modifier = Modifier.heightIn(min = 48.dp).testTag("settings_reset_filters_button")
                ) {
                    Text("Reset Filters")
                }
            }
        } else {
            LazyColumn(
                state = vm.lazyListStateFor("settings"),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = bottomInset + 20.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                if (showAppearance) {
                    item(key = "appearance") {
                        SettingsAppearanceSection(
                            vm = vm,
                            themeMode = themeMode,
                            fontStyle = fontStyle,
                            listDensity = listDensity,
                            skinId = skinId,
                            playerLayout = playerLayout,
                            homeTabsCount = homeTabsList.size,
                            reducedMotion = reducedMotion,
                            chipColors = chipColors,
                            onOpenHomeTabsDialog = { showHomeTabsDialog = true }
                        )
                    }
                }
                if (showLibrary) {
                    item(key = "library") {
                        SettingsLibrarySection(
                            vm = vm,
                            excludedFolders = excludedFolders,
                            hiddenFavoritesCount = hiddenFavorites.size,
                            splitMultiArtists = splitMultiArtists,
                            artistSplitExceptionsCount = artistSplitExceptions.size,
                            onPickSafFolder = { safLauncher.launch(null) },
                            onOpenExceptionsDialog = { showExceptionsDialog = true }
                        )
                    }
                }
                if (showPlayback) {
                    item(key = "playback") {
                        SettingsPlaybackSection(
                            vm = vm,
                            fxStatus = fxStatus,
                            playerState = playerState,
                            sleepFadeSec = sleepFadeSec,
                            fadeEnabled = fadeEnabled,
                            crossfadeSeconds = crossfadeSeconds,
                            crossfadeEqualPower = crossfadeEqualPower,
                            smartAlbumGapless = smartAlbumGapless,
                            replayGainMode = replayGainMode,
                            replayGainPreampDb = replayGainPreampDb,
                            autoResumeOnDeviceConnect = autoResumeOnDeviceConnect,
                            drivingKeepScreenOn = drivingKeepScreenOn,
                            chipColors = chipColors
                        )
                    }
                }
                if (anyToolsOrSystemVisible) {
                    item(key = "tools_and_system") {
                        SettingsToolsAndSystemCards(
                            vm = vm,
                            showRecap = showRecap,
                            showBackup = showBackup,
                            showWidgets = showWidgets,
                            showPrivacy = showPrivacy,
                            showStudio = showStudio,
                            showDiagnostics = showDiagnostics,
                            showAbout = showAbout,
                            favCount = favCount,
                            onlineFeaturesEnabled = onlineFeaturesEnabled,
                            offlineOnlyMode = offlineOnlyMode,
                            inputKey = inputKey,
                            onInputKeyChange = { inputKey = it },
                            crashRecords = crashRecords,
                            recentErrors = recentErrors,
                            missingCount = missingCount,
                            onOpenCleanDialog = { showCleanConfirmDialog = true }
                        )
                    }
                }
            }
        }
    }

    if (showHomeTabsDialog) {
        HomeTabsDialog(
            homeTabsList = homeTabsList,
            onApply = { ordered ->
                vm.setHomeTabs(ordered)
                showHomeTabsDialog = false
            },
            onDismiss = { showHomeTabsDialog = false }
        )
    }

    if (showCleanConfirmDialog) {
        CleanMissingTracksDialog(
            missingCount = missingCount,
            onConfirm = {
                showCleanConfirmDialog = false
                vm.cleanMissingTracks { deleted ->
                    vm.snackbarMessage.value = "Cleaned $deleted missing track(s)"
                }
            },
            onDismiss = { showCleanConfirmDialog = false }
        )
    }

    if (showExceptionsDialog) {
        ArtistSplitExceptionsDialog(
            vm = vm,
            exceptions = artistSplitExceptions,
            onDismiss = { showExceptionsDialog = false }
        )
    }
}
