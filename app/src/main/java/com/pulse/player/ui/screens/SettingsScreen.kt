package com.pulse.player.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.Checkroom
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DashboardCustomize
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.ViewCarousel
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pulse.player.data.settings.ThemeMode
import com.pulse.player.BuildConfig
import com.pulse.player.ui.theme.Pulse
import com.pulse.player.ui.theme.Skins
import com.pulse.player.ui.viewmodel.MainViewModel
import android.widget.Toast
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(vm: MainViewModel) {
    val themeMode by vm.themeMode.collectAsStateWithLifecycle()
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
    val ctx = LocalContext.current
    var inputKey by remember { mutableStateOf("") }
    var showCleanConfirmDialog by remember { mutableStateOf(false) }
    var showHomeTabsDialog by remember { mutableStateOf(false) }

    val safLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            vm.addSafFolder(uri)
        }
    }

    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { vm.back() }) {
                Icon(Icons.Rounded.ArrowBack, "Back", tint = MaterialTheme.colorScheme.onSurface)
            }
            Spacer(Modifier.width(8.dp))
            Text("Settings", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                SettingsSectionHeader("Appearance")
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text("Theme Mode", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ThemeMode.entries.forEach { mode ->
                                FilterChip(
                                    selected = themeMode == mode,
                                    onClick = { vm.setThemeMode(mode) },
                                    label = { Text(mode.name) }
                                )
                            }
                        }

                        SettingsActionRow(
                            "Skin Theme",
                            "${Skins.firstOrNull { it.id == skinId }?.name ?: "Plum"} palette, wallpaper & atmosphere",
                            Icons.Rounded.Checkroom
                        ) { vm.go(Screen.SkinTheme) }
                        SettingsActionRow(
                            "Player Layout",
                            "${playerLayout.displayName} • ${PlayerLayoutMode.entries.size} switchable layouts",
                            Icons.Rounded.AutoAwesome
                        ) { vm.go(Screen.PlayerTheme) }
                        SettingsActionRow(
                            "Customize Player",
                            "Live editor for layouts, themes, 15 button presets & elements",
                            Icons.Rounded.DashboardCustomize
                        ) { vm.go(Screen.Customize) }
                        SettingsActionRow(
                            "Home Tabs Arrangement",
                            "Reorder or toggle For you, Songs, Playlists, Folders, Albums, Artists",
                            Icons.Rounded.ViewCarousel
                        ) { showHomeTabsDialog = true }

                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text("Reduced Motion", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                Text("Disables spinning disc, visualizer bars, and infinite animations", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(
                                checked = reducedMotion,
                                onCheckedChange = { vm.setReducedMotion(it) },
                                colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.primary)
                            )
                        }
                    }
                }
            }

            item {
                SettingsSectionHeader("Library")
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        SettingsActionRow("Rescan Library", "Scan phone storage for new audio", Icons.Rounded.Refresh) {
                            vm.scan()
                        }
                        SettingsActionRow("Choose Music Folder", "Pick a folder with Storage Access Framework", Icons.Rounded.FolderOpen) {
                            safLauncher.launch(null)
                        }
                        SettingsActionRow("Duplicate Finder", "Find duplicate songs across your library (list only)", Icons.Rounded.ContentCopy) {
                            vm.go(Screen.Duplicates)
                        }
                    }
                }
            }

            item {
                SettingsSectionHeader("Playback")
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        SettingsNavRow("Equalizer & Sound Effects", Icons.Rounded.Equalizer) {
                            vm.go(Screen.Equalizer)
                        }
                        SettingsActionRow(
                            title = "Sleep Timer",
                            subtitle = "Set countdown timer or stop at the end of the current track",
                            icon = Icons.Rounded.Bedtime,
                            onClick = { vm.openSleep() }
                        )

                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text("Audio Fades", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                Text("Smooth volume ramp on play/pause", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(
                                checked = fadeEnabled,
                                onCheckedChange = { vm.setFadeEnabled(it) },
                                colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.primary)
                            )
                        }

                        // Audiophile Crossfade Slider (0..12s)
                        Column(Modifier.fillMaxWidth()) {
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Track Crossfade", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                Text(
                                    if (crossfadeSeconds == 0) "Off" else "${crossfadeSeconds}s",
                                    style = MaterialTheme.typography.bodyMedium,
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

                        // Playback Speed Slider
                        Column(Modifier.fillMaxWidth()) {
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Playback Speed", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                Text(
                                    "%.2fx".format(playerState.speed),
                                    style = MaterialTheme.typography.bodyMedium,
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

                        // Pitch Lock Switch
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text("Lock Pitch", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                Text("Preserve natural voice pitch when altering speed", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(
                                checked = playerState.pitchLocked,
                                onCheckedChange = { vm.player.setPitchLocked(it) },
                                colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.primary)
                            )
                        }

                        // Stop After Current Track Switch
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text("Stop After Current Track", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                Text("Pause playback automatically when current song finishes", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(
                                checked = playerState.sleepEndOfTrack,
                                onCheckedChange = { enabled ->
                                    if (enabled) vm.player.sleepAtEndOfTrack() else vm.player.cancelSleep()
                                },
                                colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.primary)
                            )
                        }

                        // Skip Silence Switch
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text("Skip Silence", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                Text("Skip silent passages seamlessly during playback", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(
                                checked = playerState.skipSilence,
                                onCheckedChange = { vm.player.setSkipSilence(it) },
                                colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.primary)
                            )
                        }
                    }
                }
            }

            item {
                SettingsSectionHeader("History & Statistics")
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
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

            item {
                SettingsSectionHeader("Online & Privacy")
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
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

            item {
                SettingsSectionHeader("Backup & Transfer")
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        SettingsNavRow("Backup & Transfer Favourites", Icons.Rounded.Backup) {
                            vm.go(Screen.Backup)
                        }
                    }
                }
            }

            item {
                SettingsSectionHeader("Widgets")
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Widgets, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(16.dp))
                        Column {
                            Text("Home Screen Widget", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            Text("Long-press home screen → Widgets → Pulse Player. Supports 2x2, 4x1, and 4x2 responsive layouts.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            item {
                SettingsSectionHeader("AI Features & Studio")
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (!BuildConfig.NETWORK_FEATURES_INCLUDED) {
                            Text(
                                "AI transcription and music generation are not included in the offline build. Select a local track for playback and local lyrics tools instead.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            SettingsNavRow("Audio & Lyrics Transcribe (gemini-3.5-transcribe)", Icons.Rounded.Mic) {
                                vm.go(Screen.Transcribe)
                            }
                            SettingsNavRow("AI Music Studio (Google Lyria)", Icons.Rounded.AutoAwesome) {
                                vm.go(Screen.GenerateMusic)
                            }

                            Spacer(Modifier.height(4.dp))
                            Text("Gemini API Key", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                            Text(
                                "Used only for optional online transcription and Lyria music generation in the full build.",
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
                                    placeholder = { Text("Paste API Key...", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall) },
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
                                            Toast.makeText(ctx, "Gemini API Key saved!", Toast.LENGTH_SHORT).show()
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

            item {
                SettingsSectionHeader("System & Diagnostics")
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        SettingsActionRow(
                            title = "Diagnostics & Error Log",
                            subtitle = "Inspect audio hardware, effects status, permissions & error log",
                            icon = Icons.Rounded.Build,
                            onClick = { vm.openDiagnostics() }
                        )
                        Spacer(Modifier.height(8.dp))
                        SettingsActionRow(
                            title = "Clean Missing Tracks",
                            subtitle = if (missingCount > 0) "$missingCount missing track(s) ready to clean" else "All tracks are reachable on storage",
                            icon = Icons.Rounded.Delete,
                            onClick = {
                                if (missingCount > 0) {
                                    showCleanConfirmDialog = true
                                } else {
                                    Toast.makeText(ctx, "No missing tracks found in library", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    }
                }
            }

            item {
                SettingsSectionHeader("About")
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Info, null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(12.dp))
                            Text("Pulse Player v1.0.0", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        }
                        Text(
                            if (BuildConfig.NETWORK_FEATURES_INCLUDED) {
                                "Full build: optional online features are off by default. No tracking or ads. Built with Jetpack Compose & Media3."
                            } else {
                                "Offline build: no internet permission, no tracking, no ads. Built with Jetpack Compose & Media3."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
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
                            androidx.compose.material3.Checkbox(
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
                androidx.compose.material3.TextButton(onClick = { showHomeTabsDialog = false }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.primary)
                }
            },
            containerColor = MaterialTheme.colorScheme.surfaceVariant
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
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showCleanConfirmDialog = false
                        vm.cleanMissingTracks { deleted ->
                            Toast.makeText(ctx, "Cleaned $deleted missing track(s)", Toast.LENGTH_SHORT).show()
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
                androidx.compose.material3.TextButton(onClick = { showCleanConfirmDialog = false }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.primary)
                }
            },
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}

@Composable
fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(bottom = 6.dp)
    )
}

@Composable
fun SettingsNavRow(title: String, icon: ImageVector, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(16.dp))
        Text(title, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
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
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
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
fun SettingsActionRow(title: String, subtitle: String, icon: ImageVector, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(16.dp))
        Column {
            Text(title, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
