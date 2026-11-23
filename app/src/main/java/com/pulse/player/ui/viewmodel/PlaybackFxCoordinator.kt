package com.pulse.player.ui.viewmodel

import com.pulse.player.data.settings.DeviceDspRule
import com.pulse.player.data.settings.ReplayGainMode
import com.pulse.player.data.settings.SettingsStore
import com.pulse.player.data.settings.SoundProfile
import com.pulse.player.playback.AudioDeviceMonitor
import com.pulse.player.playback.AudioFx
import com.pulse.player.playback.AudioOutputDevice
import com.pulse.player.playback.PlaybackEngine
import com.pulse.player.playback.PlayerController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PlaybackFxCoordinator(
    private val settings: SettingsStore,
    private val player: PlayerController,
    private val scope: CoroutineScope,
    private val onSnackbar: (String) -> Unit
) {
    val customSoundProfiles: StateFlow<List<SoundProfile>> = settings.customSoundProfiles
        .stateIn(scope, SharingStarted.WhileSubscribed(5000), emptyList())

    val soundProfiles: StateFlow<List<SoundProfile>> = customSoundProfiles
        .map { custom -> AudioFx.builtInProfiles + custom }
        .stateIn(scope, SharingStarted.WhileSubscribed(5000), AudioFx.builtInProfiles)

    val activeSoundProfileId: StateFlow<String> = settings.activeSoundProfileId
        .stateIn(scope, SharingStarted.WhileSubscribed(5000), "builtin_flat")

    val currentAudioDevice: StateFlow<AudioOutputDevice> = AudioDeviceMonitor.currentDevice

    val deviceProfileMap: StateFlow<Map<String, String>> = settings.deviceProfileMap
        .stateIn(scope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val deviceDspRules: StateFlow<Map<String, DeviceDspRule>> = settings.deviceDspRules
        .stateIn(scope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val replayGainMode: StateFlow<ReplayGainMode> = settings.replayGainMode
        .stateIn(scope, SharingStarted.WhileSubscribed(5000), ReplayGainMode.OFF)

    val replayGainPreampDb: StateFlow<Float> = settings.replayGainPreampDb
        .stateIn(scope, SharingStarted.WhileSubscribed(5000), 0f)

    val crossfadeEqualPower: StateFlow<Boolean> = settings.crossfadeEqualPower
        .stateIn(scope, SharingStarted.WhileSubscribed(5000), true)

    val autoResumeOnDeviceConnect: StateFlow<Boolean> = settings.autoResumeOnDeviceConnect
        .stateIn(scope, SharingStarted.WhileSubscribed(5000), false)

    val smartAlbumGapless: StateFlow<Boolean> = settings.smartAlbumGapless
        .stateIn(scope, SharingStarted.WhileSubscribed(5000), true)

    val eqChannelBalance: StateFlow<Float> = settings.eqChannelBalance
        .stateIn(scope, SharingStarted.WhileSubscribed(5000), 0f)

    val eqMonoMode: StateFlow<Boolean> = settings.eqMonoMode
        .stateIn(scope, SharingStarted.WhileSubscribed(5000), false)

    val eqLimiterEnabled: StateFlow<Boolean> = settings.eqLimiterEnabled
        .stateIn(scope, SharingStarted.WhileSubscribed(5000), true)

    val drivingKeepScreenOn: StateFlow<Boolean> = settings.drivingKeepScreenOn
        .stateIn(scope, SharingStarted.WhileSubscribed(5000), true)

    private var lastAutoAppliedDeviceKey: String? = null

    init {
        scope.launch {
            combine(settings.replayGainMode, settings.replayGainPreampDb) { mode, preamp ->
                mode to preamp
            }.distinctUntilChanged().collect { (mode, preamp) ->
                PlaybackEngine.setReplayGainConfig(mode, preamp)
            }
        }
        scope.launch {
            combine(settings.crossfadeSeconds, settings.crossfadeEqualPower) { secs, eqPower ->
                secs to eqPower
            }.distinctUntilChanged().collect { (secs, eqPower) ->
                PlaybackEngine.setCrossfadeSeconds(secs, eqPower)
            }
        }
        scope.launch {
            combine(
                AudioDeviceMonitor.currentDevice,
                settings.deviceDspRules,
                soundProfiles
            ) { device, rulesMap, allProfiles ->
                Triple(device, rulesMap[device.key], allProfiles)
            }.distinctUntilChanged().collect { (device, rule, allProfiles) ->
                if (rule == null) {
                    lastAutoAppliedDeviceKey = device.key
                    return@collect
                }
                if (device.key != lastAutoAppliedDeviceKey) {
                    lastAutoAppliedDeviceKey = device.key
                    applyDeviceDspRule(rule, allProfiles, device.name)
                }
            }
        }
        scope.launch {
            AudioDeviceMonitor.deviceConnectedEvents.collect { device ->
                val rule = deviceDspRules.value[device.key]
                if (rule != null) {
                    applyDeviceDspRule(rule, soundProfiles.value, device.name)
                }
                if (autoResumeOnDeviceConnect.value &&
                    !player.state.value.isPlaying &&
                    player.queue.value.isNotEmpty()
                ) {
                    player.togglePlay()
                }
            }
        }
    }

    private suspend fun applyDeviceDspRule(
        rule: DeviceDspRule,
        allProfiles: List<SoundProfile>,
        deviceName: String
    ) {
        val appliedParts = mutableListOf<String>()
        rule.profileId?.let { id ->
            val match = allProfiles.firstOrNull { it.id == id }
            if (match != null && match.id != activeSoundProfileId.value) {
                applySoundProfileInternal(match, silent = true)
                appliedParts.add(match.name)
            }
        }
        rule.crossfadeSecOverride?.let { cf ->
            settings.setCrossfadeSeconds(cf)
            PlaybackEngine.setCrossfadeSeconds(cf, crossfadeEqualPower.value)
            appliedParts.add("CF ${cf}s")
        }
        rule.replayGainModeOverride?.let { rg ->
            settings.setReplayGainMode(rg)
            PlaybackEngine.setReplayGainConfig(rg, replayGainPreampDb.value)
            appliedParts.add("RG ${rg.label}")
        }
        rule.monoModeOverride?.let { mono ->
            AudioFx.setMonoMode(mono)
            settings.setEqMonoMode(mono)
            appliedParts.add(if (mono) "Mono" else "Stereo")
        }
        rule.channelBalanceOverride?.let { bal ->
            val safe = bal.coerceIn(-1f, 1f)
            AudioFx.setChannelBalance(safe)
            settings.setEqChannelBalance(safe)
        }
        if (appliedParts.isNotEmpty()) {
            onSnackbar("DSP preset loaded for $deviceName: ${appliedParts.joinToString(" · ")}")
        }
    }

    fun applySoundProfile(profile: SoundProfile) = scope.launch {
        applySoundProfileInternal(profile, silent = false)
    }

    private suspend fun applySoundProfileInternal(profile: SoundProfile, silent: Boolean) {
        AudioFx.setEnabled(true)
        AudioFx.applyBandLevels(profile.bandLevels)
        AudioFx.setBass(profile.bassStrength)
        AudioFx.setVirtualizer(profile.virtualizerStrength)
        AudioFx.setReverb(profile.reverbIndex)
        AudioFx.setLimiterEnabled(profile.limiterEnabled)
        AudioFx.setChannelBalance(profile.channelBalance)
        AudioFx.setMonoMode(profile.monoMode)
        settings.setEqLimiterEnabled(profile.limiterEnabled)
        settings.setEqChannelBalance(profile.channelBalance)
        settings.setEqMonoMode(profile.monoMode)
        player.applyPlaybackParameters(profile.speed, player.state.value.pitchLocked, profile.pitchSemitones)
        settings.setActiveSoundProfileId(profile.id)
        if (!silent) {
            onSnackbar("Applied '${profile.name}' profile")
        }
    }

    fun saveCurrentAsSoundProfile(name: String) = scope.launch {
        val clean = name.trim()
        if (clean.isBlank()) return@launch
        val ps = player.state.value
        val profile = SoundProfile(
            id = "custom_${System.currentTimeMillis()}",
            name = clean,
            bandLevels = AudioFx.currentBandLevels(),
            bassStrength = AudioFx.currentBass(),
            virtualizerStrength = AudioFx.currentVirtualizer(),
            reverbIndex = AudioFx.currentReverb(),
            speed = ps.speed,
            pitchSemitones = ps.pitchSemitones,
            isBuiltIn = false,
            limiterEnabled = AudioFx.limiterEnabled,
            channelBalance = AudioFx.channelBalance,
            monoMode = AudioFx.monoMode
        )
        settings.saveCustomSoundProfile(profile)
        onSnackbar("Saved sound profile '$clean'")
    }

    fun deleteSoundProfile(id: String) = scope.launch {
        settings.deleteCustomSoundProfile(id)
        onSnackbar("Removed sound profile")
    }

    fun assignProfileToCurrentDevice(profileId: String?) = scope.launch {
        val device = currentAudioDevice.value
        settings.setDeviceProfile(device.key, profileId)
        val profileName = profileId?.let { id -> soundProfiles.value.firstOrNull { it.id == id }?.name }
        if (profileName == null) {
            onSnackbar("Cleared linked profile for ${device.name}")
        } else {
            onSnackbar("Linked '$profileName' to ${device.name}")
        }
    }

    fun saveDeviceDspRule(rule: DeviceDspRule) = scope.launch {
        settings.setDeviceDspRule(rule)
        val current = currentAudioDevice.value
        if (rule.deviceKey == current.key) {
            applyDeviceDspRule(rule, soundProfiles.value, current.name)
        }
        onSnackbar("Saved DSP rule for ${current.name}")
    }

    fun setReplayGainMode(mode: ReplayGainMode) = scope.launch {
        settings.setReplayGainMode(mode)
        PlaybackEngine.setReplayGainConfig(mode, replayGainPreampDb.value)
    }

    fun setReplayGainPreampDb(db: Float) = scope.launch {
        val safe = db.coerceIn(-6f, 6f)
        settings.setReplayGainPreampDb(safe)
        PlaybackEngine.setReplayGainConfig(replayGainMode.value, safe)
    }

    fun setCrossfadeEqualPower(enabled: Boolean) = scope.launch {
        settings.setCrossfadeEqualPower(enabled)
        PlaybackEngine.setCrossfadeSeconds(
            (PlaybackEngine.crossfadeMs / 1000L).toInt(),
            enabled
        )
    }

    fun setSmartAlbumGapless(enabled: Boolean) = scope.launch {
        settings.setSmartAlbumGapless(enabled)
        PlaybackEngine.smartAlbumGapless = enabled
    }

    fun setEqChannelBalance(balance: Float) = scope.launch {
        val safe = balance.coerceIn(-1f, 1f)
        AudioFx.setChannelBalance(safe)
        settings.setEqChannelBalance(safe)
    }

    fun setEqMonoMode(enabled: Boolean) = scope.launch {
        AudioFx.setMonoMode(enabled)
        settings.setEqMonoMode(enabled)
    }

    fun setEqLimiterEnabled(enabled: Boolean) = scope.launch {
        AudioFx.setLimiterEnabled(enabled)
        settings.setEqLimiterEnabled(enabled)
    }

    fun setDrivingKeepScreenOn(enabled: Boolean) = scope.launch {
        settings.setDrivingKeepScreenOn(enabled)
    }

    fun setAutoResumeOnDeviceConnect(enabled: Boolean) = scope.launch {
        settings.setAutoResumeOnDeviceConnect(enabled)
    }
}
