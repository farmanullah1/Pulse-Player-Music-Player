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
import androidx.compose.material.icons.rounded.Checkroom
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Widgets
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
import com.pulse.player.ui.theme.Pulse
import com.pulse.player.ui.viewmodel.MainViewModel
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(vm: MainViewModel) {
    val themeMode by vm.themeMode.collectAsStateWithLifecycle()
    val reducedMotion by vm.reducedMotion.collectAsStateWithLifecycle()
    val fadeEnabled by vm.fadeEnabled.collectAsStateWithLifecycle()
    val playerState by vm.player.state.collectAsStateWithLifecycle()

    val safLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            vm.scanTree(uri)
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
                Icon(Icons.Rounded.ArrowBack, "Back", tint = Color.White)
            }
            Spacer(Modifier.width(8.dp))
            Text("Settings", style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.Bold)
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                SettingsSectionHeader("Appearance")
                Card(
                    colors = CardDefaults.cardColors(containerColor = Pulse.Surface),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text("Theme Mode", style = MaterialTheme.typography.titleMedium, color = Color.White)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ThemeMode.entries.forEach { mode ->
                                FilterChip(
                                    selected = themeMode == mode,
                                    onClick = { vm.setThemeMode(mode) },
                                    label = { Text(mode.name) }
                                )
                            }
                        }

                        SettingsNavRow("Skin Theme", Icons.Rounded.Checkroom) { vm.go(Screen.SkinTheme) }
                        SettingsNavRow("Player Theme", Icons.Rounded.AutoAwesome) { vm.go(Screen.PlayerTheme) }

                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text("Reduced Motion", fontWeight = FontWeight.Bold, color = Color.White)
                                Text("Disables spinning disc, visualizer bars, and infinite animations", style = MaterialTheme.typography.bodySmall, color = Pulse.TextMid)
                            }
                            Switch(
                                checked = reducedMotion,
                                onCheckedChange = { vm.setReducedMotion(it) },
                                colors = SwitchDefaults.colors(checkedThumbColor = Pulse.Cyan)
                            )
                        }
                    }
                }
            }

            item {
                SettingsSectionHeader("Library")
                Card(
                    colors = CardDefaults.cardColors(containerColor = Pulse.Surface),
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
                    colors = CardDefaults.cardColors(containerColor = Pulse.Surface),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text("Audio Fades", fontWeight = FontWeight.Bold, color = Color.White)
                                Text("Smooth volume ramp on play/pause", style = MaterialTheme.typography.bodySmall, color = Pulse.TextMid)
                            }
                            Switch(
                                checked = fadeEnabled,
                                onCheckedChange = { vm.setFadeEnabled(it) },
                                colors = SwitchDefaults.colors(checkedThumbColor = Pulse.Cyan)
                            )
                        }

                        // Playback Speed Slider
                        Column(Modifier.fillMaxWidth()) {
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Playback Speed", fontWeight = FontWeight.Bold, color = Color.White)
                                Text(
                                    "%.2fx".format(playerState.speed),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Pulse.Cyan,
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
                                    thumbColor = Pulse.Cyan,
                                    activeTrackColor = Pulse.Cyan,
                                    inactiveTrackColor = Pulse.Outline
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
                                Text("Lock Pitch", fontWeight = FontWeight.Bold, color = Color.White)
                                Text("Preserve natural voice pitch when altering speed", style = MaterialTheme.typography.bodySmall, color = Pulse.TextMid)
                            }
                            Switch(
                                checked = playerState.pitchLocked,
                                onCheckedChange = { vm.player.setPitchLocked(it) },
                                colors = SwitchDefaults.colors(checkedThumbColor = Pulse.Cyan)
                            )
                        }

                        // Stop After Current Track Switch
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text("Stop After Current Track", fontWeight = FontWeight.Bold, color = Color.White)
                                Text("Pause playback automatically when current song finishes", style = MaterialTheme.typography.bodySmall, color = Pulse.TextMid)
                            }
                            Switch(
                                checked = playerState.sleepEndOfTrack,
                                onCheckedChange = { enabled ->
                                    if (enabled) vm.player.sleepAtEndOfTrack() else vm.player.cancelSleep()
                                },
                                colors = SwitchDefaults.colors(checkedThumbColor = Pulse.Cyan)
                            )
                        }

                        // Skip Silence Switch
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text("Skip Silence", fontWeight = FontWeight.Bold, color = Color.White)
                                Text("Skip silent passages seamlessly during playback", style = MaterialTheme.typography.bodySmall, color = Pulse.TextMid)
                            }
                            Switch(
                                checked = playerState.skipSilence,
                                onCheckedChange = { vm.player.setSkipSilence(it) },
                                colors = SwitchDefaults.colors(checkedThumbColor = Pulse.Cyan)
                            )
                        }

                        SettingsNavRow("Equalizer & Sound Effects", Icons.Rounded.Equalizer) {
                            vm.go(Screen.Equalizer)
                        }
                    }
                }
            }

            item {
                SettingsSectionHeader("Backup & Transfer")
                Card(
                    colors = CardDefaults.cardColors(containerColor = Pulse.Surface),
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
                    colors = CardDefaults.cardColors(containerColor = Pulse.Surface),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Widgets, null, tint = Pulse.Cyan)
                        Spacer(Modifier.width(16.dp))
                        Column {
                            Text("Home Screen Widget", fontWeight = FontWeight.Bold, color = Color.White)
                            Text("Long-press home screen → Widgets → Pulse Player. Supports 2x2, 4x1, and 4x2 responsive layouts.", style = MaterialTheme.typography.bodySmall, color = Pulse.TextMid)
                        }
                    }
                }
            }

            item {
                SettingsSectionHeader("About")
                Card(
                    colors = CardDefaults.cardColors(containerColor = Pulse.Surface),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Info, null, tint = Pulse.Cyan)
                            Spacer(Modifier.width(12.dp))
                            Text("Pulse Player v1.0.0", fontWeight = FontWeight.Bold, color = Color.White)
                        }
                        Text("100% Offline Music Player for Android. Zero internet permission, zero tracking, zero ads. Built with Jetpack Compose & Media3.", style = MaterialTheme.typography.bodySmall, color = Pulse.TextMid)
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsSectionHeader(title: String) {
    Text(title, style = MaterialTheme.typography.titleMedium, color = Pulse.Cyan, modifier = Modifier.padding(bottom = 6.dp))
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
        Icon(icon, null, tint = Color.White, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(16.dp))
        Text(title, style = MaterialTheme.typography.bodyMedium, color = Color.White, fontWeight = FontWeight.SemiBold)
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
        Icon(icon, null, tint = Color.White, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(16.dp))
        Column {
            Text(title, style = MaterialTheme.typography.bodyMedium, color = Color.White, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Pulse.TextMid)
        }
    }
}
