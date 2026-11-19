package com.pulse.player.ui.screens

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DashboardCustomize
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.FolderOff
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.ViewCarousel
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pulse.player.BuildConfig
import com.pulse.player.data.settings.FontStylePreset
import com.pulse.player.data.settings.ThemeMode
import com.pulse.player.ui.components.LocalBottomOverlayPadding
import com.pulse.player.ui.components.PulseTopBar
import com.pulse.player.ui.components.formatTime
import com.pulse.player.ui.theme.Skins
import com.pulse.player.ui.theme.TabularMono
import com.pulse.player.ui.viewmodel.MainViewModel
import kotlin.math.roundToInt

private enum class SettingsCategory(val label: String) {
    ALL("All"),
    APPEARANCE("Appearance"),
    LIBRARY("Library"),
    PLAYBACK("Playback"),
    TOOLS("Backup & Tools"),
    SYSTEM("System")
}

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

    var inputKey by remember { mutableStateOf("") }
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(SettingsCategory.ALL) }
    var showCleanConfirmDialog by remember { mutableStateOf(false) }
    var showHomeTabsDialog by remember { mutableStateOf(false) }

    BackHandler {
        if (searchQuery.isNotBlank() || selectedCategory != SettingsCategory.ALL) {
            searchQuery = ""
            selectedCategory = SettingsCategory.ALL
        } else {
            vm.back()
        }
    }

    val safLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            vm.addSafFolder(uri)
        }
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
        "excluded folders", "duplicate", "duplicates"
    )
    val showPlayback = matchesSection(
        SettingsCategory.PLAYBACK,
        "playback", "equalizer", "eq", "sound effects", "sleep timer", "fade",
        "crossfade", "speed", "pitch", "a-b loop", "loop", "audio controls",
        "stop after current track", "skip silence"
    )
    val showRecap = matchesSection(
        SettingsCategory.TOOLS,
        "history", "statistics", "stats", "recap", "listening time", "top tracks"
    )
    val showBackup = matchesSection(
        SettingsCategory.TOOLS,
        "backup", "transfer", "restore", "favourites", "favorites", "export", "import"
    )
    val showWidgets = matchesSection(
        SettingsCategory.TOOLS,
        "widget", "widgets", "home screen widget"
    )
    val showStudio = matchesSection(
        SettingsCategory.TOOLS,
        "voice", "lyrics", "transcribe", "music studio", "api key", "gemini"
    )
    val showPrivacy = matchesSection(
        SettingsCategory.SYSTEM,
        "online", "privacy", "offline", "network"
    )
    val showDiagnostics = matchesSection(
        SettingsCategory.SYSTEM,
        "system", "diagnostics", "error log", "clean missing tracks", "hardware", "permissions"
    )
    val showAbout = matchesSection(
        SettingsCategory.SYSTEM,
        "about", "version", "pulse player", "privacy"
    )
    val anySectionVisible = showAppearance || showLibrary || showPlayback || showRecap ||
        showBackup || showWidgets || showStudio || showPrivacy || showDiagnostics || showAbout

    val chipColors = FilterChipDefaults.filterChipColors(
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        labelColor = MaterialTheme.colorScheme.onSurface,
        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
    )

    Column(Modifier.fillMaxSize()) {
        PulseTopBar(title = "Settings", onBack = { vm.back() })

        // Search & Quick Category Jump Strip
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 6.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                placeholder = {
                    Text(
                        "Search settings, EQ, crossfade, themes…",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                leadingIcon = {
                    Icon(Icons.Rounded.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(48.dp)) {
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
                        onClick = { selectedCategory = category },
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
                Text(
                    text = "No matching settings",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Try searching for theme, equalizer, crossfade, folder, or backup.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                OutlinedButton(
                    onClick = {
                        searchQuery = ""
                        selectedCategory = SettingsCategory.ALL
                    },
                    shape = CircleShape
                ) {
                    Text("Reset Filters")
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = bottomInset + 20.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                if (showAppearance) {
                    item(key = "appearance") {
                        SettingsSectionHeader("Appearance")
                        SettingsSectionCard {
                            Text("Theme Mode", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(
                                    ThemeMode.SYSTEM to "System",
                                    ThemeMode.LIGHT to "Light",
                                    ThemeMode.DARK to "Dark",
                                    ThemeMode.AMOLED to "AMOLED"
                                ).forEach { (mode, label) ->
                                    val isSelected = themeMode == mode
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { vm.setThemeMode(mode) },
                                        label = { Text(label) },
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

                            Text("Typography Style", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FontStylePreset.entries.forEach { preset ->
                                    val isSelected = fontStyle == preset
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { vm.setFontStyle(preset) },
                                        label = { Text(preset.label) },
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

                            Text("Library Density", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(0 to "Compact", 1 to "Standard", 2 to "Comfortable").forEach { (d, label) ->
                                    val isSelected = listDensity == d
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { vm.setListDensity(d) },
                                        label = { Text(label) },
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

                            SettingsRowDivider()
                            SettingsActionRow(
                                title = "Skin & Wallpaper",
                                subtitle = "${Skins.firstOrNull { it.id == skinId }?.name ?: "Studio Amber"} palette & backdrop",
                                icon = Icons.Rounded.Palette
                            ) { vm.go(Screen.SkinTheme) }
                            SettingsRowDivider()
                            SettingsActionRow(
                                title = "Player Layout",
                                subtitle = "${playerLayout.displayName} · ${PlayerLayoutMode.entries.size} visual layouts",
                                icon = Icons.Rounded.AutoAwesome
                            ) { vm.go(Screen.PlayerTheme) }
                            SettingsRowDivider()
                            SettingsActionRow(
                                title = "Customize Player",
                                subtitle = "Layouts, themes, buttons, accents & home sections",
                                icon = Icons.Rounded.DashboardCustomize
                            ) { vm.go(Screen.Customize) }
                            SettingsRowDivider()
                            SettingsActionRow(
                                title = "Home Tabs",
                                subtitle = "Show, hide, or arrange library tabs (${homeTabsList.size} active)",
                                icon = Icons.Rounded.ViewCarousel
                            ) { showHomeTabsDialog = true }
                            SettingsRowDivider()
                            SettingsSwitchRow(
                                title = "Reduced Motion",
                                subtitle = "Turns off spinning artwork and continuous animations",
                                checked = reducedMotion,
                                onCheckedChange = { vm.setReducedMotion(it) }
                            )
                        }
                    }
                }

                if (showLibrary) {
                    item(key = "library") {
                        SettingsSectionHeader("Library")
                        SettingsSectionCard {
                            SettingsActionRow(
                                title = "Rescan Library",
                                subtitle = "Check device storage for new or updated audio",
                                icon = Icons.Rounded.Refresh
                            ) {
                                vm.scan()
                                vm.snackbarMessage.value = "Scanning library for audio files…"
                            }
                            SettingsRowDivider()
                            SettingsActionRow(
                                title = "Choose Music Folder",
                                subtitle = "Grant access to a custom folder on device or SD card",
                                icon = Icons.Rounded.FolderOpen
                            ) {
                                safLauncher.launch(null)
                            }
                            SettingsRowDivider()
                            val excludedSub = when {
                                hiddenFavorites.isNotEmpty() -> "${excludedFolders.size} excluded · ${hiddenFavorites.size} hidden favourite(s)"
                                excludedFolders.isEmpty() -> "Manage folders hidden from library scans"
                                else -> "${excludedFolders.size} folder(s) excluded"
                            }
                            val excludedBadge = when {
                                hiddenFavorites.isNotEmpty() -> "${excludedFolders.size} · ${hiddenFavorites.size}!"
                                excludedFolders.isNotEmpty() -> "${excludedFolders.size}"
                                else -> null
                            }
                            SettingsActionRow(
                                title = "Excluded Folders",
                                subtitle = excludedSub,
                                icon = Icons.Rounded.FolderOff,
                                badge = excludedBadge
                            ) {
                                vm.go(Screen.ExcludedFolders)
                            }
                            SettingsRowDivider()
                            SettingsActionRow(
                                title = "Duplicate Finder",
                                subtitle = "Compare audio quality, copy paths & spot extra copies",
                                icon = Icons.Rounded.ContentCopy
                            ) {
                                vm.go(Screen.Duplicates)
                            }
                        }
                    }
                }

                if (showPlayback) {
                    item(key = "playback") {
                        SettingsSectionHeader("Playback")
                        SettingsSectionCard {
                            SettingsActionRow(
                                title = "Equalizer & Sound Effects",
                                subtitle = "5-band EQ presets, bass boost, virtualizer & room reverb",
                                icon = Icons.Rounded.Equalizer,
                                badge = if (fxStatus.eqSupported && fxStatus.sessionId > 0) "Active" else null
                            ) {
                                vm.go(Screen.Equalizer)
                            }
                            SettingsRowDivider()
                            val loopOrPitchBadge = buildList {
                                if (playerState.speed != 1f) add("%.2fx".format(playerState.speed))
                                if (playerState.pitchSemitones != 0f) add("%+.0fst".format(playerState.pitchSemitones))
                                if (playerState.abLoopStartMs != null || playerState.abLoopEndMs != null) add("A-B")
                            }.joinToString(" · ").takeIf { it.isNotBlank() }
                            SettingsActionRow(
                                title = "Audio & Loop Controls",
                                subtitle = "Fine-tune playback speed, pitch shift & A-B section looping",
                                icon = Icons.Rounded.Speed,
                                badge = loopOrPitchBadge
                            ) {
                                vm.openAudioControls()
                            }
                            SettingsRowDivider()
                            val sleepStatus = when {
                                playerState.sleepRemainingMs > 0L -> formatTime(playerState.sleepRemainingMs)
                                playerState.sleepEndOfTrack -> "End of track"
                                else -> null
                            }
                            SettingsActionRow(
                                title = "Sleep Timer",
                                subtitle = "Set countdown timer or stop at the end of the current track",
                                icon = Icons.Rounded.Bedtime,
                                badge = sleepStatus,
                                onClick = { vm.openSleep() }
                            )
                            SettingsRowDivider()
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("Sleep Timer Fade-Out", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                Text("Gentle volume ramp before pausing playback", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Row(
                                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    listOf(0 to "Off", 5 to "5s", 10 to "10s", 20 to "20s").forEach { (sec, label) ->
                                        val isSelected = sleepFadeSec == sec
                                        FilterChip(
                                            selected = isSelected,
                                            onClick = { vm.setSleepFadeSec(sec) },
                                            label = { Text(label) },
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
                            SettingsRowDivider()
                            SettingsSwitchRow(
                                title = "Audio Fades",
                                subtitle = "Smooth volume ramp on play/pause",
                                checked = fadeEnabled,
                                onCheckedChange = { vm.setFadeEnabled(it) }
                            )
                            SettingsRowDivider()

                            Column(Modifier.fillMaxWidth()) {
                                Row(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Track Crossfade", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                    Text(
                                        if (crossfadeSeconds == 0) "Off" else "${crossfadeSeconds}s",
                                        style = MaterialTheme.typography.bodyMedium.merge(TabularMono),
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Slider(
                                    value = crossfadeSeconds.toFloat(),
                                    onValueChange = { vm.setCrossfadeSeconds(it.roundToInt()) },
                                    valueRange = 0f..12f,
                                    steps = 11,
                                    colors = SliderDefaults.colors(
                                        thumbColor = MaterialTheme.colorScheme.primary,
                                        activeTrackColor = MaterialTheme.colorScheme.primary,
                                        inactiveTrackColor = MaterialTheme.colorScheme.outlineVariant
                                    )
                                )
                            }

                            Column(Modifier.fillMaxWidth()) {
                                Row(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Playback Speed", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                    Text(
                                        "%.2fx".format(playerState.speed),
                                        style = MaterialTheme.typography.bodyMedium.merge(TabularMono),
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Slider(
                                    value = playerState.speed,
                                    onValueChange = { raw ->
                                        val stepped = (raw * 20f).roundToInt() / 20f
                                        vm.player.setSpeed(stepped.coerceIn(0.5f, 2.0f))
                                    },
                                    valueRange = 0.5f..2.0f,
                                    steps = 29,
                                    colors = SliderDefaults.colors(
                                        thumbColor = MaterialTheme.colorScheme.primary,
                                        activeTrackColor = MaterialTheme.colorScheme.primary,
                                        inactiveTrackColor = MaterialTheme.colorScheme.outlineVariant
                                    )
                                )
                            }

                            SettingsRowDivider()
                            SettingsSwitchRow(
                                title = "Lock Pitch",
                                subtitle = "Preserve natural voice pitch when altering speed",
                                checked = playerState.pitchLocked,
                                onCheckedChange = { vm.player.setPitchLocked(it) }
                            )
                            SettingsRowDivider()
                            SettingsSwitchRow(
                                title = "Stop After Current Track",
                                subtitle = "Pause playback automatically when current song finishes",
                                checked = playerState.sleepEndOfTrack,
                                onCheckedChange = { enabled ->
                                    if (enabled) vm.player.sleepAtEndOfTrack() else vm.player.cancelSleep()
                                }
                            )
                            SettingsRowDivider()
                            SettingsSwitchRow(
                                title = "Skip Silence",
                                subtitle = "Skip silent passages seamlessly during playback",
                                checked = playerState.skipSilence,
                                onCheckedChange = { vm.player.setSkipSilence(it) }
                            )
                        }
                    }
                }

                if (showRecap) {
                    item(key = "history") {
                        SettingsSectionHeader("History & Statistics")
                        SettingsSectionCard {
                            SettingsActionRow(
                                title = "Music Recap & Listening Stats",
                                subtitle = "View monthly listening recap, top tracks, total listening time and play trends",
                                icon = Icons.Rounded.History
                            ) {
                                vm.go(Screen.Recap)
                            }
                        }
                    }
                }

                if (showBackup) {
                    item(key = "backup") {
                        SettingsSectionHeader("Backup & Transfer")
                        SettingsSectionCard {
                            SettingsActionRow(
                                title = "Backup & Transfer",
                                subtitle = "Quick transfer for $favCount favourite(s) & full library archive",
                                icon = Icons.Rounded.Backup,
                                badge = if (favCount > 0) "$favCount favs" else null
                            ) {
                                vm.go(Screen.Backup)
                            }
                        }
                    }
                }

                if (showWidgets) {
                    item(key = "widgets") {
                        SettingsSectionHeader("Widgets")
                        SettingsSectionCard {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(RoundedCornerShape(11.dp))
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Rounded.Widgets, null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(20.dp))
                                }
                                Spacer(Modifier.width(14.dp))
                                Column {
                                    Text("Home Screen Widget", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                    Text(
                                        "Long-press home screen → Widgets → Pulse Player. Supports 2x2, 4x1, and 4x2 responsive layouts.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                if (showPrivacy) {
                    item(key = "privacy") {
                        SettingsSectionHeader("Online & Privacy")
                        SettingsSectionCard {
                            if (!BuildConfig.NETWORK_FEATURES_INCLUDED) {
                                Text(
                                    "Online integrations are not included in the offline build. Local playback, scanning, lyrics and backups remain available.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                SettingsSwitchRow(
                                    title = "Online features",
                                    subtitle = "Allow explicitly enabled providers to connect. Off by default.",
                                    checked = onlineFeaturesEnabled,
                                    onCheckedChange = vm::setOnlineFeaturesEnabled
                                )
                                SettingsRowDivider()
                                SettingsSwitchRow(
                                    title = "Offline-only mode",
                                    subtitle = "Blocks all network requests while preserving local features.",
                                    checked = offlineOnlyMode,
                                    onCheckedChange = vm::setOfflineOnlyMode
                                )
                            }
                        }
                    }
                }

                if (showStudio) {
                    item(key = "studio") {
                        SettingsSectionHeader("Voice & Studio")
                        SettingsSectionCard {
                            if (!BuildConfig.NETWORK_FEATURES_INCLUDED) {
                                Text(
                                    "Online transcription and music studio tools are disabled in this build. Local playback, scanning, lyrics, and backups remain ready to use.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                SettingsNavRow("Voice & Lyrics Transcribe", Icons.Rounded.Mic) {
                                    vm.go(Screen.Transcribe)
                                }
                                SettingsRowDivider()
                                SettingsNavRow("Music Studio", Icons.Rounded.AutoAwesome) {
                                    vm.go(Screen.GenerateMusic)
                                }
                                SettingsRowDivider()
                                Text("Studio API Key", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                                Text(
                                    "Optional key for online voice transcription and studio track creation.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = inputKey,
                                        onValueChange = { inputKey = it },
                                        placeholder = { Text("Paste API Key…", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall) },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                                        )
                                    )
                                    Button(
                                        onClick = {
                                            if (inputKey.isNotBlank()) {
                                                vm.setGeminiApiKey(inputKey)
                                                vm.snackbarMessage.value = "API key saved"
                                                inputKey = ""
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.primary,
                                            contentColor = MaterialTheme.colorScheme.onPrimary
                                        )
                                    ) {
                                        Text("Save", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }

                if (showDiagnostics) {
                    item(key = "diagnostics") {
                        SettingsSectionHeader("System & Diagnostics")
                        SettingsSectionCard {
                            SettingsActionRow(
                                title = "Diagnostics & Error Log",
                                subtitle = "Inspect audio hardware, effects status, permissions & error log",
                                icon = Icons.Rounded.Build,
                                onClick = { vm.openDiagnostics() }
                            )
                            SettingsRowDivider()
                            SettingsActionRow(
                                title = "Clean Missing Tracks",
                                subtitle = if (missingCount > 0) "$missingCount missing track(s) ready to clean" else "All tracks are reachable on storage",
                                icon = Icons.Rounded.Delete,
                                badge = if (missingCount > 0) "$missingCount" else null,
                                onClick = {
                                    if (missingCount > 0) {
                                        showCleanConfirmDialog = true
                                    } else {
                                        vm.snackbarMessage.value = "No missing tracks found in library"
                                    }
                                }
                            )
                        }
                    }
                }

                if (showAbout) {
                    item(key = "about") {
                        SettingsSectionHeader("About")
                        SettingsSectionCard {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(RoundedCornerShape(11.dp))
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Rounded.Info, null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(20.dp))
                                }
                                Spacer(Modifier.width(14.dp))
                                Column {
                                    Text("Pulse Player v1.0.0", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                    Text(
                                        if (BuildConfig.NETWORK_FEATURES_INCLUDED) {
                                            "Privacy-first local audio player with optional online studio tools off by default. No tracking or ads."
                                        } else {
                                            "Offline local audio player with zero tracking and zero ads."
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showHomeTabsDialog) {
        val allTabs = listOf("For you", "Songs", "Playlists", "Folders", "Albums", "Artists")
        var currentSelection by remember(homeTabsList) { mutableStateOf(homeTabsList.toSet()) }

        AlertDialog(
            onDismissRequest = { showHomeTabsDialog = false },
            title = { Text("Home Tabs Arrangement") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Select which tabs appear on the home screen:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    allTabs.forEach { tabName ->
                        val isChecked = tabName in currentSelection
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    currentSelection = if (isChecked) {
                                        if (currentSelection.size > 1) currentSelection - tabName else currentSelection
                                    } else {
                                        currentSelection + tabName
                                    }
                                }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = { check ->
                                    currentSelection = if (check) currentSelection + tabName else {
                                        if (currentSelection.size > 1) currentSelection - tabName else currentSelection
                                    }
                                }
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(tabName, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val ordered = allTabs.filter { it in currentSelection }
                        vm.setHomeTabs(ordered)
                        showHomeTabsDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text("Apply", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showHomeTabsDialog = false }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.primary)
                }
            },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    if (showCleanConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showCleanConfirmDialog = false },
            title = { Text("Clean Missing Tracks?") },
            text = {
                Text(
                    "This will remove $missingCount track(s) that are no longer accessible on storage from your library and playlists. Your original files on disk are not altered.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showCleanConfirmDialog = false
                        vm.cleanMissingTracks { deleted ->
                            vm.snackbarMessage.value = "Cleaned $deleted missing track(s)"
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Text("Clean", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCleanConfirmDialog = false }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.primary)
                }
            },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SettingsSectionCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            content = content
        )
    }
}

@Composable
private fun SettingsRowDivider() {
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
}

@Composable
fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
    )
}

@Composable
fun SettingsNavRow(
    title: String,
    icon: ImageVector,
    badge: String? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(14.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (!badge.isNullOrBlank()) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.padding(end = 6.dp)
            ) {
                Text(
                    text = badge,
                    style = MaterialTheme.typography.labelSmall.merge(TabularMono),
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
        }
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun SettingsSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.primary)
        )
    }
}

@Composable
fun SettingsActionRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    badge: String? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (!badge.isNullOrBlank()) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.padding(horizontal = 6.dp)
            ) {
                Text(
                    text = badge,
                    style = MaterialTheme.typography.labelSmall.merge(TabularMono),
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
        }
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
    }
}
