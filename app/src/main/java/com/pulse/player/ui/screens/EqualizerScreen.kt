package com.pulse.player.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.BluetoothAudio
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Speaker
import androidx.compose.material.icons.rounded.Usb
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pulse.player.data.settings.DeviceDspRule
import com.pulse.player.data.settings.ReplayGainMode
import com.pulse.player.playback.AudioFx
import com.pulse.player.ui.components.LocalBottomOverlayPadding
import com.pulse.player.ui.components.PulseTopBar
import com.pulse.player.ui.theme.TabularMono
import com.pulse.player.ui.viewmodel.MainViewModel

@Composable
fun EqualizerScreen(vm: MainViewModel) {
    BackHandler { vm.back() }

    val fxStatus by vm.fxStatus.collectAsStateWithLifecycle()
    val soundProfiles by vm.soundProfiles.collectAsStateWithLifecycle()
    val activeProfileId by vm.activeSoundProfileId.collectAsStateWithLifecycle()
    val currentDevice by vm.currentAudioDevice.collectAsStateWithLifecycle()
    val deviceProfileMap by vm.deviceProfileMap.collectAsStateWithLifecycle()
    val deviceDspRules by vm.deviceDspRules.collectAsStateWithLifecycle()
    val playerState by vm.player.state.collectAsStateWithLifecycle()
    val channelBalance by vm.eqChannelBalance.collectAsStateWithLifecycle()
    val monoMode by vm.eqMonoMode.collectAsStateWithLifecycle()
    val limiterEnabled by vm.eqLimiterEnabled.collectAsStateWithLifecycle()

    var enabled by remember { mutableStateOf(AudioFx.isMasterEnabled && AudioFx.eqSupported) }
    var selectedPreset by remember { mutableStateOf("Normal") }
    var bassVal by remember { mutableFloatStateOf(AudioFx.currentBass().toFloat()) }
    var virtVal by remember { mutableFloatStateOf(AudioFx.currentVirtualizer().toFloat()) }
    var selectedReverb by remember { mutableIntStateOf(AudioFx.currentReverb()) }
    var saveProfileOpen by remember { mutableStateOf(false) }
    var linkDeviceDialogOpen by remember { mutableStateOf(false) }

    val bandCount = if (fxStatus.tenBandSupported) 10 else AudioFx.bandCount.coerceAtLeast(5)
    val bandLevels = remember(bandCount) {
        mutableStateListOf<Float>().apply {
            for (b in 0 until bandCount) {
                add(AudioFx.bandLevel(b).toFloat())
            }
        }
    }

    LaunchedEffect(activeProfileId, selectedPreset, fxStatus.sessionId, fxStatus.tenBandSupported) {
        val current = AudioFx.currentBandLevels()
        if (current.isNotEmpty()) {
            for (b in 0 until minOf(bandCount, current.size)) {
                bandLevels[b] = current[b].toFloat()
            }
        }
        bassVal = AudioFx.currentBass().toFloat()
        virtVal = AudioFx.currentVirtualizer().toFloat()
        selectedReverb = AudioFx.currentReverb()
    }

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
            subtitle = when {
                !enabled -> "Bypassed"
                fxStatus.tenBandSupported -> "10-Band Precision DSP & Sound Profiles"
                else -> "Live DSP Curve & Sound Profiles"
            },
            onBack = vm::back,
            actions = {
                if (hasAnyEffect) {
                    Switch(
                        checked = enabled,
                        onCheckedChange = {
                            enabled = it
                            AudioFx.setEnabled(it)
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.testTag("eq_master_switch")
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
            item(key = "active_output_device_card") {
                val activeRule = deviceDspRules[currentDevice.key]
                val linkedProfileId = activeRule?.profileId ?: deviceProfileMap[currentDevice.key]
                val linkedProfile = linkedProfileId?.let { id -> soundProfiles.firstOrNull { it.id == id } }
                val deviceIcon = when (currentDevice.typeLabel) {
                    "Bluetooth", "Hearing Aid" -> Icons.Rounded.BluetoothAudio
                    "USB Audio" -> Icons.Rounded.Usb
                    "Wired", "Line Out" -> Icons.Rounded.Headphones
                    else -> Icons.Rounded.Speaker
                }
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f)),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = deviceIcon,
                                contentDescription = currentDevice.typeLabel,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = currentDevice.name,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = buildString {
                                    append(currentDevice.typeLabel)
                                    append(" · ")
                                    append(linkedProfile?.let { "Linked: ${it.name}" } ?: "No linked profile")
                                    activeRule?.crossfadeSecOverride?.let { append(" · CF ${it}s") }
                                    activeRule?.replayGainModeOverride?.let { append(" · ${it.label} RG") }
                                    activeRule?.monoModeOverride?.let { append(if (it) " · Mono" else " · Stereo") }
                                    if (playerState.appliedReplayGainDb != 0f) {
                                        append(" · RG %+.1f dB".format(playerState.appliedReplayGainDb))
                                    }
                                },
                                style = MaterialTheme.typography.labelSmall.merge(TabularMono),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        FilledTonalButton(
                            onClick = { linkDeviceDialogOpen = true },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("eq_link_device_button")
                        ) {
                            Text(
                                text = if (activeRule != null || linkedProfile != null) "Configure" else "Link Rule",
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                }
            }

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

            // Saved & Built-in Sound Profiles (EQ + Bass + Surround + Reverb + Speed/Pitch)
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                "Sound Profiles",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                "Combines EQ, Bass, Virtualizer, Reverb & Speed",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        FilledTonalButton(
                            onClick = { saveProfileOpen = true },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("eq_save_profile_button")
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Save Current", style = MaterialTheme.typography.labelMedium)
                        }
                    }

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(soundProfiles, key = { it.id }) { profile ->
                            val isSelected = activeProfileId == profile.id
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                                    else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f)
                                ),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable {
                                        enabled = true
                                        vm.applySoundProfile(profile)
                                        for (b in 0 until minOf(bandCount, profile.bandLevels.size)) {
                                            bandLevels[b] = profile.bandLevels[b].toFloat()
                                        }
                                        bassVal = profile.bassStrength.toFloat()
                                        virtVal = profile.virtualizerStrength.toFloat()
                                        selectedReverb = profile.reverbIndex
                                    }
                                    .testTag("sound_profile_${profile.id}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(
                                        start = 12.dp,
                                        end = if (profile.isBuiltIn) 12.dp else 4.dp,
                                        top = 10.dp,
                                        bottom = 10.dp
                                    ),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        Icons.Rounded.GraphicEq,
                                        contentDescription = null,
                                        tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Column {
                                        Text(
                                            text = profile.name,
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                        )
                                        val bassPct = (profile.bassStrength / 10).coerceIn(0, 100)
                                        val meta = buildString {
                                            append("Bass $bassPct%")
                                            if (profile.speed != 1f) append(" · ${"%.2fx".format(profile.speed)}")
                                        }
                                        Text(
                                            text = meta,
                                            style = MaterialTheme.typography.labelSmall.merge(TabularMono),
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.78f)
                                            else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    if (!profile.isBuiltIn) {
                                        IconButton(
                                            onClick = { vm.deleteSoundProfile(profile.id) },
                                            modifier = Modifier.size(48.dp)
                                        ) {
                                            Icon(
                                                Icons.Rounded.DeleteOutline,
                                                contentDescription = "Delete ${profile.name}",
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (AudioFx.eqSupported) {
                item {
                    Column(modifier = Modifier.alpha(if (enabled) 1f else 0.55f)) {
                        Text("Hardware Presets", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                        Spacer(Modifier.height(8.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(AudioFx.presets.keys.toList()) { preset ->
                                FilterChip(
                                    selected = selectedPreset == preset,
                                    enabled = enabled,
                                    onClick = {
                                        selectedPreset = preset
                                        AudioFx.applyPreset(preset)
                                        val refreshed = AudioFx.currentBandLevels()
                                        for (b in 0 until minOf(bandCount, refreshed.size)) {
                                            bandLevels[b] = refreshed[b].toFloat()
                                        }
                                    },
                                    label = { Text(preset) },
                                    colors = chipColors
                                )
                            }
                        }
                    }
                }

                item {
                    val (minLevel, maxLevel) = AudioFx.levelRange
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f)),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.alpha(if (enabled) 1f else 0.55f)
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Frequency Response Curve", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.secondaryContainer
                                ) {
                                    Text(
                                        text = if (fxStatus.tenBandSupported) "10-Band DSP" else "${bandCount}-Band EQ",
                                        style = MaterialTheme.typography.labelSmall.merge(TabularMono),
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }
                            Spacer(Modifier.height(10.dp))

                            EqCurveCanvas(
                                bandLevels = bandLevels.toList(),
                                minLevel = minLevel.toFloat(),
                                maxLevel = maxLevel.toFloat(),
                                accent = MaterialTheme.colorScheme.primary,
                                gridColor = MaterialTheme.colorScheme.outlineVariant,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(128.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(MaterialTheme.colorScheme.surfaceContainerLow)
                                    .padding(horizontal = 12.dp, vertical = 14.dp)
                            )

                            Spacer(Modifier.height(16.dp))
                            Text("Bands", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            Spacer(Modifier.height(8.dp))

                            for (b in 0 until bandCount) {
                                val freq = AudioFx.bandFreqHz(b).let {
                                    if (it >= 1000) "${it / 1000} kHz" else "$it Hz"
                                }
                                val level = bandLevels.getOrElse(b) { 0f }

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
                                        onValueChange = { nextVal ->
                                            if (b < bandLevels.size) bandLevels[b] = nextVal
                                            AudioFx.setBand(b, nextVal.toInt())
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

            item(key = "output_stage_controls") {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f)),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Text(
                            text = "Output Stage & Spatial",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (fxStatus.limiterSupported) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(Modifier.weight(1f).padding(end = 12.dp)) {
                                    Text("Clip-Guard Limiter (-0.5 dB)", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                                    Text(
                                        "Prevents digital clipping when boosting bass or EQ bands",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = limiterEnabled,
                                    onCheckedChange = vm::setEqLimiterEnabled,
                                    modifier = Modifier.testTag("eq_limiter_switch")
                                )
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(Modifier.weight(1f).padding(end = 12.dp)) {
                                Text("Mono Downmix", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                                Text(
                                    "Sums left & right stereo channels for single-earbud listening",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = monoMode,
                                onCheckedChange = vm::setEqMonoMode,
                                modifier = Modifier.testTag("eq_mono_switch")
                            )
                        }
                        if (fxStatus.tenBandSupported) {
                            Column(Modifier.fillMaxWidth()) {
                                val balanceLabel = when {
                                    kotlin.math.abs(channelBalance) < 0.04f -> "Center"
                                    channelBalance < 0f -> "L ${(kotlin.math.abs(channelBalance) * 100).toInt()}%"
                                    else -> "R ${(channelBalance * 100).toInt()}%"
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("L / R Channel Balance", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text(
                                            text = balanceLabel,
                                            style = MaterialTheme.typography.bodyMedium.merge(TabularMono),
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Bold
                                        )
                                        if (kotlin.math.abs(channelBalance) >= 0.04f) {
                                            TextButton(
                                                onClick = { vm.setEqChannelBalance(0f) },
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                            ) {
                                                Text("Center")
                                            }
                                        }
                                    }
                                }
                                Slider(
                                    value = channelBalance,
                                    onValueChange = { raw ->
                                        val snapped = if (kotlin.math.abs(raw) < 0.05f) 0f else raw
                                        vm.setEqChannelBalance(snapped)
                                    },
                                    valueRange = -1f..1f,
                                    modifier = Modifier.testTag("eq_balance_slider"),
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

            item {
                Button(
                    onClick = {
                        selectedPreset = "Flat"
                        AudioFx.applyPreset("Flat")
                        for (b in bandLevels.indices) bandLevels[b] = 0f
                        bassVal = 0f
                        AudioFx.setBass(0)
                        virtVal = 0f
                        AudioFx.setVirtualizer(0)
                        selectedReverb = 0
                        AudioFx.setReverb(0)
                        vm.setEqChannelBalance(0f)
                        vm.setEqMonoMode(false)
                        vm.setEqLimiterEnabled(true)
                        vm.snackbarMessage.value = "Equalizer reset to Flat"
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("eq_reset_button"),
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

    if (linkDeviceDialogOpen) {
        val existingRule = deviceDspRules[currentDevice.key]
        var selectedProfileId by remember(currentDevice.key, linkDeviceDialogOpen) {
            mutableStateOf(existingRule?.profileId ?: deviceProfileMap[currentDevice.key])
        }
        var crossfadeOverride by remember(currentDevice.key, linkDeviceDialogOpen) {
            mutableStateOf(existingRule?.crossfadeSecOverride)
        }
        var rgOverride by remember(currentDevice.key, linkDeviceDialogOpen) {
            mutableStateOf(existingRule?.replayGainModeOverride)
        }
        var monoOverride by remember(currentDevice.key, linkDeviceDialogOpen) {
            mutableStateOf(existingRule?.monoModeOverride)
        }

        AlertDialog(
            onDismissRequest = { linkDeviceDialogOpen = false },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            title = { Text("Auto-DSP Rule: ${currentDevice.name}") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        "Automatically apply EQ profile and DSP overrides whenever this ${currentDevice.typeLabel.lowercase()} output connects.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text("Sound Profile", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Surface(
                        onClick = { selectedProfileId = null },
                        shape = RoundedCornerShape(12.dp),
                        color = if (selectedProfileId == null) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                    ) {
                        Text(
                            text = "Keep current profile",
                            modifier = Modifier.fillMaxWidth().padding(10.dp),
                            fontWeight = if (selectedProfileId == null) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedProfileId == null) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    soundProfiles.forEach { profile ->
                        val isLinked = selectedProfileId == profile.id
                        Surface(
                            onClick = { selectedProfileId = profile.id },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isLinked) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                        ) {
                            Text(
                                text = profile.name,
                                modifier = Modifier.fillMaxWidth().padding(10.dp),
                                fontWeight = if (isLinked) FontWeight.Bold else FontWeight.Normal,
                                color = if (isLinked) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Text("Crossfade Override", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf<Pair<Int?, String>>(
                            null to "Global",
                            0 to "Off",
                            2 to "2s",
                            4 to "4s",
                            6 to "6s",
                            8 to "8s"
                        ).forEach { (sec, label) ->
                            FilterChip(
                                selected = crossfadeOverride == sec,
                                onClick = { crossfadeOverride = sec },
                                label = { Text(label) },
                                colors = chipColors
                            )
                        }
                    }

                    Text("ReplayGain Override", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = rgOverride == null,
                            onClick = { rgOverride = null },
                            label = { Text("Global") },
                            colors = chipColors
                        )
                        ReplayGainMode.entries.forEach { mode ->
                            FilterChip(
                                selected = rgOverride == mode,
                                onClick = { rgOverride = mode },
                                label = { Text(mode.label) },
                                colors = chipColors
                            )
                        }
                    }

                    Text("Channel Mode Override", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf<Pair<Boolean?, String>>(
                            null to "Global",
                            false to "Stereo",
                            true to "Mono"
                        ).forEach { (mono, label) ->
                            FilterChip(
                                selected = monoOverride == mono,
                                onClick = { monoOverride = mono },
                                label = { Text(label) },
                                colors = chipColors
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        vm.saveDeviceDspRule(
                            DeviceDspRule(
                                deviceKey = currentDevice.key,
                                profileId = selectedProfileId,
                                crossfadeSecOverride = crossfadeOverride,
                                replayGainModeOverride = rgOverride,
                                monoModeOverride = monoOverride
                            )
                        )
                        linkDeviceDialogOpen = false
                    },
                    modifier = Modifier.testTag("eq_save_device_rule_button")
                ) {
                    Text("Save Rule")
                }
            },
            dismissButton = {
                Row {
                    if (existingRule != null) {
                        TextButton(onClick = {
                            vm.saveDeviceDspRule(DeviceDspRule(deviceKey = currentDevice.key))
                            linkDeviceDialogOpen = false
                        }) {
                            Text("Clear", color = MaterialTheme.colorScheme.error)
                        }
                    }
                    TextButton(onClick = { linkDeviceDialogOpen = false }) {
                        Text("Cancel")
                    }
                }
            }
        )
    }

    if (saveProfileOpen) {
        var profileName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { saveProfileOpen = false },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            title = { Text("Save Sound Profile") },
            text = {
                OutlinedTextField(
                    value = profileName,
                    onValueChange = { profileName = it },
                    label = { Text("Profile name (e.g. Gym Bass)") },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        vm.saveCurrentAsSoundProfile(profileName)
                        saveProfileOpen = false
                    },
                    enabled = profileName.isNotBlank()
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { saveProfileOpen = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun EqCurveCanvas(
    bandLevels: List<Float>,
    minLevel: Float,
    maxLevel: Float,
    accent: Color,
    gridColor: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        if (w <= 0f || h <= 0f || bandLevels.isEmpty()) return@Canvas

        val range = (maxLevel - minLevel).coerceAtLeast(1f)
        val zeroNorm = ((0f - minLevel) / range).coerceIn(0f, 1f)
        val zeroY = h * (1f - zeroNorm)

        // Horizontal reference lines (+max, 0dB, -min)
        drawLine(
            color = gridColor.copy(alpha = 0.25f),
            start = Offset(0f, 0f),
            end = Offset(w, 0f),
            strokeWidth = 1.dp.toPx()
        )
        drawLine(
            color = gridColor.copy(alpha = 0.6f),
            start = Offset(0f, zeroY),
            end = Offset(w, zeroY),
            strokeWidth = 1.5.dp.toPx()
        )
        drawLine(
            color = gridColor.copy(alpha = 0.25f),
            start = Offset(0f, h),
            end = Offset(w, h),
            strokeWidth = 1.dp.toPx()
        )

        val count = bandLevels.size
        val points = bandLevels.mapIndexed { idx, lvl ->
            val x = if (count == 1) w / 2f else w * (idx.toFloat() / (count - 1))
            val norm = ((lvl - minLevel) / range).coerceIn(0f, 1f)
            val y = h * (1f - norm)
            Offset(x, y)
        }

        val linePath = Path().apply {
            moveTo(points.first().x, points.first().y)
            for (i in 0 until points.lastIndex) {
                val p0 = points[i]
                val p1 = points[i + 1]
                val midX = (p0.x + p1.x) / 2f
                cubicTo(midX, p0.y, midX, p1.y, p1.x, p1.y)
            }
        }

        val fillPath = Path().apply {
            addPath(linePath)
            lineTo(points.last().x, h)
            lineTo(points.first().x, h)
            close()
        }

        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(accent.copy(alpha = 0.34f), accent.copy(alpha = 0.03f)),
                startY = 0f,
                endY = h
            )
        )

        drawPath(
            path = linePath,
            color = accent,
            style = Stroke(
                width = 3.dp.toPx(),
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )

        points.forEach { pt ->
            drawCircle(
                color = accent.copy(alpha = 0.25f),
                radius = 7.dp.toPx(),
                center = pt
            )
            drawCircle(
                color = accent,
                radius = 4.dp.toPx(),
                center = pt
            )
        }
    }
}
