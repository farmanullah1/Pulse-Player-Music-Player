package com.pulse.player.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DashboardCustomize
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.FolderOff
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.ViewCarousel
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SelectableChipColors
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pulse.player.BuildConfig
import com.pulse.player.data.AppError
import com.pulse.player.data.CrashRecord
import com.pulse.player.data.settings.FontStylePreset
import com.pulse.player.data.settings.ThemeMode
import com.pulse.player.ui.theme.Skins
import com.pulse.player.ui.viewmodel.MainViewModel

@Composable
internal fun SettingsAppearanceSection(
    vm: MainViewModel,
    themeMode: ThemeMode,
    fontStyle: FontStylePreset,
    listDensity: Int,
    skinId: String,
    playerLayout: PlayerLayoutMode,
    homeTabsCount: Int,
    reducedMotion: Boolean,
    chipColors: SelectableChipColors,
    onOpenHomeTabsDialog: () -> Unit
) {
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
            title = "Custom Player Studio",
            subtitle = "${playerLayout.displayName} · ${PlayerLayoutMode.entries.size} layouts & style studio",
            icon = Icons.Rounded.DashboardCustomize
        ) {
            vm.customizeSelectedTab.value = 0
            vm.go(Screen.Customize)
        }
        SettingsRowDivider()
        SettingsActionRow(
            title = "Home Tabs",
            subtitle = "Show, hide, or arrange library tabs ($homeTabsCount active)",
            icon = Icons.Rounded.ViewCarousel,
            onClick = onOpenHomeTabsDialog
        )
        SettingsRowDivider()
        SettingsSwitchRow(
            title = "Reduced Motion",
            subtitle = "Turns off spinning artwork and continuous animations",
            checked = reducedMotion,
            onCheckedChange = { vm.setReducedMotion(it) }
        )
    }
}

@Composable
internal fun SettingsLibrarySection(
    vm: MainViewModel,
    excludedFolders: Set<String>,
    hiddenFavoritesCount: Int,
    splitMultiArtists: Boolean,
    artistSplitExceptionsCount: Int,
    onPickSafFolder: () -> Unit,
    onOpenExceptionsDialog: () -> Unit
) {
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
            icon = Icons.Rounded.FolderOpen,
            onClick = onPickSafFolder
        )
        SettingsRowDivider()
        val excludedSub = when {
            hiddenFavoritesCount > 0 -> "${excludedFolders.size} excluded · $hiddenFavoritesCount hidden favourite(s)"
            excludedFolders.isEmpty() -> "Manage folders hidden from library scans"
            else -> "${excludedFolders.size} folder(s) excluded"
        }
        val excludedBadge = when {
            hiddenFavoritesCount > 0 -> "${excludedFolders.size} · $hiddenFavoritesCount!"
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
        SettingsRowDivider()
        SettingsSwitchRow(
            title = "Multi-Artist Splitting",
            subtitle = "Split featured & collaboration credits (feat., &, /, ;) into individual artists",
            checked = splitMultiArtists,
            onCheckedChange = vm::setSplitMultiArtists,
            switchModifier = Modifier.testTag("settings_split_artists_switch")
        )
        SettingsRowDivider()
        SettingsActionRow(
            title = "Artist Split Exceptions",
            subtitle = if (artistSplitExceptionsCount == 0) {
                "Keep duos or comma names intact (e.g. Tyler, The Creator)"
            } else {
                "$artistSplitExceptionsCount custom exception(s) configured"
            },
            icon = Icons.Rounded.Group,
            badge = if (artistSplitExceptionsCount > 0) "$artistSplitExceptionsCount" else null,
            onClick = onOpenExceptionsDialog
        )
    }
}

@Composable
internal fun SettingsToolsAndSystemCards(
    vm: MainViewModel,
    showRecap: Boolean,
    showBackup: Boolean,
    showWidgets: Boolean,
    showPrivacy: Boolean,
    showStudio: Boolean,
    showDiagnostics: Boolean,
    showAbout: Boolean,
    favCount: Int,
    onlineFeaturesEnabled: Boolean,
    offlineOnlyMode: Boolean,
    inputKey: String,
    onInputKeyChange: (String) -> Unit,
    crashRecords: List<CrashRecord>,
    recentErrors: List<AppError>,
    missingCount: Int,
    onOpenCleanDialog: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        if (showRecap) {
            Column {
                SettingsSectionHeader("History & Statistics")
                SettingsSectionCard {
                    SettingsActionRow(
                        title = "Music Recap & Listening Stats",
                        subtitle = "View monthly listening recap, daily charts, genres & listening rhythm",
                        icon = Icons.Rounded.History
                    ) { vm.go(Screen.Recap) }
                }
            }
        }
        if (showBackup) {
            Column {
                SettingsSectionHeader("Backup & Transfer")
                SettingsSectionCard {
                    SettingsActionRow(
                        title = "Backup & Transfer",
                        subtitle = "Quick transfer for $favCount favourite(s) & full library archive",
                        icon = Icons.Rounded.Backup,
                        badge = if (favCount > 0) "$favCount favs" else null
                    ) { vm.go(Screen.Backup) }
                }
            }
        }
        if (showWidgets) {
            Column {
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
                                "Long-press home screen → Widgets → Pulse Music Player. Supports compact 2x2, wide 4x1, and artwork 4x2 layouts with favourite toggle.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
        if (showPrivacy) {
            Column {
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
            Column {
                SettingsSectionHeader("Voice & Studio")
                SettingsSectionCard {
                    if (!BuildConfig.NETWORK_FEATURES_INCLUDED) {
                        Text(
                            "Online transcription and music studio tools are disabled in this build. Local playback, scanning, lyrics, and backups remain ready to use.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        SettingsNavRow("Voice & Lyrics Transcribe", Icons.Rounded.Mic) { vm.go(Screen.Transcribe) }
                        SettingsRowDivider()
                        SettingsNavRow("Music Studio", Icons.Rounded.AutoAwesome) { vm.go(Screen.GenerateMusic) }
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
                                onValueChange = onInputKeyChange,
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
                                        onInputKeyChange("")
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
            Column {
                SettingsSectionHeader("System & Diagnostics")
                SettingsSectionCard {
                    val diagBadge = when {
                        crashRecords.isNotEmpty() -> "${crashRecords.size}!"
                        recentErrors.isNotEmpty() -> "${recentErrors.size}"
                        else -> null
                    }
                    SettingsActionRow(
                        title = "Diagnostics & Error Log",
                        subtitle = "Inspect audio hardware, crash logs, permissions & error events",
                        icon = Icons.Rounded.Build,
                        badge = diagBadge,
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
                                onOpenCleanDialog()
                            } else {
                                vm.snackbarMessage.value = "No missing tracks found in library"
                            }
                        }
                    )
                }
            }
        }
        if (showAbout) {
            Column {
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
                            Text("Pulse Music Player v${BuildConfig.VERSION_NAME}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
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
