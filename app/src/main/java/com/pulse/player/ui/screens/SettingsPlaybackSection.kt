package com.pulse.player.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SelectableChipColors
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pulse.player.data.settings.ReplayGainMode
import com.pulse.player.playback.AudioFxStatus
import com.pulse.player.playback.LoudnessScanState
import com.pulse.player.playback.PlaybackUiState
import com.pulse.player.ui.components.formatTime
import com.pulse.player.ui.theme.TabularMono
import com.pulse.player.ui.viewmodel.MainViewModel
import kotlin.math.roundToInt

@Composable
internal fun SettingsPlaybackSection(
    vm: MainViewModel,
    fxStatus: AudioFxStatus,
    playerState: PlaybackUiState,
    sleepFadeSec: Int,
    fadeEnabled: Boolean,
    crossfadeSeconds: Int,
    crossfadeEqualPower: Boolean,
    smartAlbumGapless: Boolean,
    replayGainMode: ReplayGainMode,
    replayGainPreampDb: Float,
    autoResumeOnDeviceConnect: Boolean,
    drivingKeepScreenOn: Boolean,
    chipColors: SelectableChipColors
) {
    SettingsSectionHeader("Playback")
    SettingsSectionCard {
        SettingsActionRow(
            title = "Equalizer & Sound Effects",
            subtitle = "EQ presets, bass boost, virtualizer, reverb & device profiles",
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
            subtitle = "Fine-tune playback speed, pitch shift, bookmarks & A-B looping",
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
                ),
                modifier = Modifier.testTag("settings_crossfade_slider")
            )
            SettingsSwitchRow(
                title = "Equal-Power Crossfade",
                subtitle = "Uses cosine/sine curve so perceived loudness stays level across transitions",
                checked = crossfadeEqualPower,
                onCheckedChange = vm::setCrossfadeEqualPower
            )
            SettingsSwitchRow(
                title = "Smart Album Gapless",
                subtitle = "Bypass crossfade automatically between consecutive tracks of the same album",
                checked = smartAlbumGapless,
                onCheckedChange = vm::setSmartAlbumGapless
            )
        }
        SettingsRowDivider()

        // ReplayGain Loudness Leveling
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("ReplayGain Leveling", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Text(
                        text = if (replayGainMode != ReplayGainMode.OFF && playerState.appliedReplayGainDb != 0f) {
                            "Active adjustment: %+.1f dB".format(playerState.appliedReplayGainDb)
                        } else {
                            "Balances loudness across tracks using embedded ReplayGain tags"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (replayGainMode != ReplayGainMode.OFF && playerState.appliedReplayGainDb != 0f) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    ReplayGainMode.OFF to "Off",
                    ReplayGainMode.TRACK to "Track",
                    ReplayGainMode.ALBUM to "Album"
                ).forEach { (mode, label) ->
                    val isSelected = replayGainMode == mode
                    FilterChip(
                        selected = isSelected,
                        onClick = { vm.setReplayGainMode(mode) },
                        label = { Text(label) },
                        colors = chipColors,
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = MaterialTheme.colorScheme.outlineVariant,
                            selectedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.testTag("replaygain_mode_${mode.name.lowercase()}")
                    )
                }
            }
            if (replayGainMode != ReplayGainMode.OFF) {
                Column(Modifier.fillMaxWidth()) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("ReplayGain Preamp", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                        Text(
                            "%+.1f dB".format(replayGainPreampDb),
                            style = MaterialTheme.typography.bodyMedium.merge(TabularMono),
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Slider(
                        value = replayGainPreampDb,
                        onValueChange = { raw ->
                            val stepped = (raw * 2f).roundToInt() / 2f
                            vm.setReplayGainPreampDb(stepped)
                        },
                        valueRange = -6f..6f,
                        steps = 23,
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary,
                            inactiveTrackColor = MaterialTheme.colorScheme.outlineVariant
                        ),
                        modifier = Modifier.testTag("settings_replaygain_preamp_slider")
                    )
                }
            }

            val missingRgCount by vm.missingReplayGainCount.collectAsStateWithLifecycle()
            val loudnessScanState by vm.loudnessScanState.collectAsStateWithLifecycle()
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceContainer
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = "On-Device Loudness Scanner",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            val statusText = when (val st = loudnessScanState) {
                                LoudnessScanState.Idle -> if (missingRgCount > 0) {
                                    "$missingRgCount track(s) missing ReplayGain tags"
                                } else {
                                    "All tracks have loudness metadata"
                                }
                                is LoudnessScanState.Scanning ->
                                    "Analyzing ${st.processed + 1}/${st.total}: ${st.currentTitle}"
                                is LoudnessScanState.Completed ->
                                    "Updated ${st.updatedCount} track(s)" + if (st.failedCount > 0) " (${st.failedCount} skipped)" else ""
                                is LoudnessScanState.Cancelled ->
                                    "Cancelled after ${st.processedCount} track(s)"
                            }
                            Text(
                                text = statusText,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    when (val st = loudnessScanState) {
                        is LoudnessScanState.Scanning -> {
                            val progress = if (st.total > 0) st.processed.toFloat() / st.total.toFloat() else 0f
                            LinearProgressIndicator(
                                progress = { progress.coerceIn(0f, 1f) },
                                modifier = Modifier.fillMaxWidth(),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                OutlinedButton(
                                    onClick = { vm.cancelLoudnessScan() },
                                    shape = CircleShape,
                                    modifier = Modifier.testTag("settings_cancel_loudness_button")
                                ) {
                                    Text("Cancel")
                                }
                            }
                        }
                        else -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (loudnessScanState !is LoudnessScanState.Idle) {
                                    OutlinedButton(
                                        onClick = { vm.resetLoudnessScanState() },
                                        shape = CircleShape
                                    ) {
                                        Text("Clear")
                                    }
                                }
                                Button(
                                    onClick = { vm.startLoudnessScan() },
                                    enabled = missingRgCount > 0,
                                    shape = CircleShape,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    ),
                                    modifier = Modifier.testTag("settings_scan_loudness_button")
                                ) {
                                    androidx.compose.material3.Icon(
                                        Icons.Rounded.GraphicEq,
                                        contentDescription = null
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = if (missingRgCount > 0) "Analyze Track Loudness" else "Library Leveled",
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        SettingsRowDivider()

        SettingsSwitchRow(
            title = "Auto-Resume on Audio Connect",
            subtitle = "Resume paused queue automatically when headphones or Bluetooth connect",
            checked = autoResumeOnDeviceConnect,
            onCheckedChange = vm::setAutoResumeOnDeviceConnect
        )
        SettingsRowDivider()
        SettingsSwitchRow(
            title = "Driving Mode Keep Screen On",
            subtitle = "Keep display awake while Driving Mode overlay is open",
            checked = drivingKeepScreenOn,
            onCheckedChange = vm::setDrivingKeepScreenOn
        )
        SettingsRowDivider()

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
