package com.pulse.player.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pulse.player.playback.AudioFx
import com.pulse.player.ui.components.LocalBottomOverlayPadding
import com.pulse.player.ui.components.PulseTopBar
import com.pulse.player.ui.theme.TabularMono
import com.pulse.player.ui.viewmodel.MainViewModel

@Composable
fun EqualizerScreen(vm: MainViewModel) {
    BackHandler { vm.back() }

    val fxStatus by vm.fxStatus.collectAsStateWithLifecycle()
    var enabled by remember { mutableStateOf(AudioFx.isMasterEnabled && AudioFx.eqSupported) }
    var selectedPreset by remember { mutableStateOf("Normal") }
    var bassVal by remember { mutableFloatStateOf(0f) }
    var virtVal by remember { mutableFloatStateOf(0f) }
    var selectedReverb by remember { mutableIntStateOf(0) }
    val bottomInset = LocalBottomOverlayPadding.current
    val hasAnyEffect = fxStatus.eqSupported || fxStatus.bassSupported || fxStatus.virtSupported || fxStatus.reverbSupported

    val chipColors = FilterChipDefaults.filterChipColors(
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        labelColor = MaterialTheme.colorScheme.onSurface,
        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
    )

    Column(Modifier.fillMaxSize()) {
        PulseTopBar(
            title = "Equalizer",
            onBack = vm::back,
            actions = {
                if (hasAnyEffect) {
                    Switch(
                        checked = enabled,
                        onCheckedChange = {
                            enabled = it
                            AudioFx.setEnabled(it)
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.primary)
                    )
                    Spacer(Modifier.width(8.dp))
                }
            }
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = bottomInset + 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (!hasAnyEffect) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Audio effects are unavailable on this device output or emulator HAL.",
                                color = MaterialTheme.colorScheme.onSurface,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Playback continues normally without DSP processing. You can check detailed effect status and logs in System Diagnostics.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall
                            )
                            Button(
                                onClick = { vm.openDiagnostics() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                )
                            ) {
                                Text("Open Diagnostics", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                return@LazyColumn
            }

            if (!enabled) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Equalizer bypassed (direct audio output)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(
                                onClick = {
                                    enabled = true
                                    AudioFx.setEnabled(true)
                                }
                            ) {
                                Text("Turn On", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            if (AudioFx.eqSupported) {
                item {
                    Column(modifier = Modifier.alpha(if (enabled) 1f else 0.55f)) {
                        Text("Presets", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                        Spacer(Modifier.height(8.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(AudioFx.presets.keys.toList()) { preset ->
                                FilterChip(
                                    selected = selectedPreset == preset,
                                    enabled = enabled,
                                    onClick = {
                                        selectedPreset = preset
                                        AudioFx.applyPreset(preset)
                                    },
                                    label = { Text(preset) },
                                    colors = chipColors
                                )
                            }
                        }
                    }
                }

                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f)),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.alpha(if (enabled) 1f else 0.55f)
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text("Bands", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                            Spacer(Modifier.height(16.dp))
                            val bandCount = AudioFx.bandCount.coerceAtLeast(5)
                            val (minLevel, maxLevel) = AudioFx.levelRange

                            for (b in 0 until bandCount) {
                                val freq = AudioFx.bandFreqHz(b).let {
                                    if (it >= 1000) "${it / 1000} kHz" else "$it Hz"
                                }
                                var level by remember(b, selectedPreset) {
                                    mutableFloatStateOf(AudioFx.bandLevel(b).toFloat())
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = freq,
                                        style = MaterialTheme.typography.bodySmall.merge(TabularMono),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.width(64.dp)
                                    )
                                    Slider(
                                        value = level,
                                        enabled = enabled,
                                        onValueChange = {
                                            level = it
                                            AudioFx.setBand(b, it.toInt())
                                        },
                                        valueRange = minLevel.toFloat()..maxLevel.toFloat(),
                                        modifier = Modifier.weight(1f),
                                        colors = SliderDefaults.colors(
                                            thumbColor = MaterialTheme.colorScheme.primary,
                                            activeTrackColor = MaterialTheme.colorScheme.primary
                                        )
                                    )
                                    Text(
                                        text = "${(level / 100).toInt()} dB",
                                        style = MaterialTheme.typography.bodySmall.merge(TabularMono),
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.width(52.dp),
                                        textAlign = TextAlign.End
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (AudioFx.bassSupported || AudioFx.virtSupported) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f)),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.alpha(if (enabled) 1f else 0.55f)
                    ) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            if (AudioFx.bassSupported) {
                                Column {
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Bass Boost", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                        Text("${(bassVal / 10).toInt()}%", style = MaterialTheme.typography.bodyMedium.merge(TabularMono), color = MaterialTheme.colorScheme.primary)
                                    }
                                    Slider(
                                        value = bassVal,
                                        enabled = enabled,
                                        onValueChange = {
                                            bassVal = it
                                            AudioFx.setBass(it.toInt())
                                        },
                                        valueRange = 0f..1000f,
                                        colors = SliderDefaults.colors(
                                            thumbColor = MaterialTheme.colorScheme.primary,
                                            activeTrackColor = MaterialTheme.colorScheme.primary
                                        )
                                    )
                                }
                            }
                            if (AudioFx.virtSupported) {
                                Column {
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Virtualizer (3D Surround)", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                        Text("${(virtVal / 10).toInt()}%", style = MaterialTheme.typography.bodyMedium.merge(TabularMono), color = MaterialTheme.colorScheme.primary)
                                    }
                                    Slider(
                                        value = virtVal,
                                        enabled = enabled,
                                        onValueChange = {
                                            virtVal = it
                                            AudioFx.setVirtualizer(it.toInt())
                                        },
                                        valueRange = 0f..1000f,
                                        colors = SliderDefaults.colors(
                                            thumbColor = MaterialTheme.colorScheme.primary,
                                            activeTrackColor = MaterialTheme.colorScheme.primary
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (AudioFx.reverbSupported) {
                item {
                    Column(modifier = Modifier.alpha(if (enabled) 1f else 0.55f)) {
                        Text("3D Reverb", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                        Spacer(Modifier.height(8.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(AudioFx.reverbNames.indices.toList()) { index ->
                                FilterChip(
                                    selected = selectedReverb == index,
                                    enabled = enabled,
                                    onClick = {
                                        selectedReverb = index
                                        AudioFx.setReverb(index)
                                    },
                                    label = { Text(AudioFx.reverbNames[index]) },
                                    colors = chipColors
                                )
                            }
                        }
                    }
                }
            }

            item {
                Button(
                    onClick = {
                        selectedPreset = "Flat"
                        AudioFx.applyPreset("Flat")
                        bassVal = 0f
                        AudioFx.setBass(0)
                        virtVal = 0f
                        AudioFx.setVirtualizer(0)
                        selectedReverb = 0
                        AudioFx.setReverb(0)
                        vm.snackbarMessage.value = "Equalizer reset to Flat"
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    Icon(Icons.Rounded.RestartAlt, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Reset Equalizer")
                }
            }
        }
    }
}
