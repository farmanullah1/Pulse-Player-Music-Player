package com.pulse.player.ui.screens

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
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pulse.player.playback.AudioFx
import com.pulse.player.ui.theme.Pulse
import com.pulse.player.ui.viewmodel.MainViewModel

@Composable
fun EqualizerScreen(vm: MainViewModel) {
    var enabled by remember { mutableStateOf(AudioFx.eqSupported) }
    var selectedPreset by remember { mutableStateOf("Normal") }
    var bassVal by remember { mutableFloatStateOf(0f) }
    var virtVal by remember { mutableFloatStateOf(0f) }
    var selectedReverb by remember { mutableStateOf(0) }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = vm::back) {
                Icon(Icons.Rounded.ArrowBack, "Back", tint = Color.White)
            }
            Spacer(Modifier.width(8.dp))
            Text("Equalizer", style = MaterialTheme.typography.titleLarge, color = Color.White)
            Spacer(Modifier.weight(1f))
            Switch(
                checked = enabled,
                onCheckedChange = { enabled = it },
                colors = SwitchDefaults.colors(checkedThumbColor = Pulse.Cyan)
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            if (!AudioFx.eqSupported && !AudioFx.bassSupported && !AudioFx.virtSupported) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Pulse.Surface),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Text(
                            text = "Audio effects are not supported on this device/emulator.",
                            color = Pulse.TextMid,
                            modifier = Modifier.padding(24.dp)
                        )
                    }
                }
                return@LazyColumn
            }

            if (AudioFx.eqSupported) {
                item {
                    Text("Presets", style = MaterialTheme.typography.titleMedium, color = Color.White)
                    Spacer(Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(AudioFx.presets.keys.toList()) { preset ->
                            FilterChip(
                                selected = selectedPreset == preset,
                                onClick = {
                                    selectedPreset = preset
                                    AudioFx.applyPreset(preset)
                                },
                                label = { Text(preset) }
                            )
                        }
                    }
                }

                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Pulse.Surface),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text("Bands", style = MaterialTheme.typography.titleMedium, color = Color.White)
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
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Pulse.TextMid,
                                        modifier = Modifier.width(64.dp)
                                    )
                                    Slider(
                                        value = level,
                                        onValueChange = {
                                            level = it
                                            AudioFx.setBand(b, it.toInt())
                                        },
                                        valueRange = minLevel.toFloat()..maxLevel.toFloat(),
                                        modifier = Modifier.weight(1f),
                                        colors = SliderDefaults.colors(thumbColor = Pulse.Cyan, activeTrackColor = Pulse.Cyan)
                                    )
                                    Text(
                                        text = "${(level / 100).toInt()} dB",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.White,
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
                        colors = CardDefaults.cardColors(containerColor = Pulse.Surface),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            if (AudioFx.bassSupported) {
                                Column {
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Bass Boost", fontWeight = FontWeight.Bold, color = Color.White)
                                        Text("${(bassVal / 10).toInt()}%", color = Pulse.Cyan)
                                    }
                                    Slider(
                                        value = bassVal,
                                        onValueChange = {
                                            bassVal = it
                                            AudioFx.setBass(it.toInt())
                                        },
                                        valueRange = 0f..1000f,
                                        colors = SliderDefaults.colors(thumbColor = Pulse.Cyan, activeTrackColor = Pulse.Cyan)
                                    )
                                }
                            }
                            if (AudioFx.virtSupported) {
                                Column {
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Virtualizer (3D Surround)", fontWeight = FontWeight.Bold, color = Color.White)
                                        Text("${(virtVal / 10).toInt()}%", color = Pulse.Cyan)
                                    }
                                    Slider(
                                        value = virtVal,
                                        onValueChange = {
                                            virtVal = it
                                            AudioFx.setVirtualizer(it.toInt())
                                        },
                                        valueRange = 0f..1000f,
                                        colors = SliderDefaults.colors(thumbColor = Pulse.Cyan, activeTrackColor = Pulse.Cyan)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (AudioFx.reverbSupported) {
                item {
                    Text("3D Reverb", style = MaterialTheme.typography.titleMedium, color = Color.White)
                    Spacer(Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(AudioFx.reverbNames.indices.toList()) { index ->
                            FilterChip(
                                selected = selectedReverb == index,
                                onClick = {
                                    selectedReverb = index
                                    AudioFx.setReverb(index)
                                },
                                label = { Text(AudioFx.reverbNames[index]) }
                            )
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
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = Pulse.Elevated, contentColor = Color.White)
                ) {
                    Icon(Icons.Rounded.RestartAlt, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Reset Equalizer")
                }
            }
        }
    }
}
