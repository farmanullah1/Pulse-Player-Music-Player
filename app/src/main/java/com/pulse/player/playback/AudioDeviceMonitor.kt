package com.pulse.player.playback

import android.content.Context
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.Immutable
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

@Immutable
data class AudioOutputDevice(
    val key: String,
    val name: String,
    val typeLabel: String,
    val isExternal: Boolean
)

/**
 * Monitors active audio output routes (Bluetooth, USB DAC, wired headset, or built-in speaker)
 * using Android's AudioDeviceCallback with zero runtime Bluetooth permissions.
 */
object AudioDeviceMonitor {
    private val defaultSpeaker = AudioOutputDevice(
        key = "speaker:builtin",
        name = "Phone Speaker",
        typeLabel = "Speaker",
        isExternal = false
    )

    private val _currentDevice = MutableStateFlow(defaultSpeaker)
    val currentDevice: StateFlow<AudioOutputDevice> = _currentDevice.asStateFlow()

    private val _deviceConnectedEvents = MutableSharedFlow<AudioOutputDevice>(extraBufferCapacity = 4)
    val deviceConnectedEvents: SharedFlow<AudioOutputDevice> = _deviceConnectedEvents.asSharedFlow()

    private var registeredManager: AudioManager? = null
    private var callback: AudioDeviceCallback? = null

    fun start(ctx: Context) {
        if (callback != null) return
        val am = ctx.applicationContext.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
        registeredManager = am
        refreshCurrentDevice(am, emitConnectEvent = false)

        val cb = object : AudioDeviceCallback() {
            override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>) {
                val previous = _currentDevice.value
                val updated = refreshCurrentDevice(am, emitConnectEvent = false)
                if (updated.isExternal && updated.key != previous.key) {
                    _deviceConnectedEvents.tryEmit(updated)
                }
            }

            override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>) {
                refreshCurrentDevice(am, emitConnectEvent = false)
            }
        }
        callback = cb
        am.registerAudioDeviceCallback(cb, Handler(Looper.getMainLooper()))
    }

    fun stop() {
        val cb = callback ?: return
        runCatching { registeredManager?.unregisterAudioDeviceCallback(cb) }
        callback = null
        registeredManager = null
    }

    private fun refreshCurrentDevice(am: AudioManager, emitConnectEvent: Boolean): AudioOutputDevice {
        val outputs = runCatching { am.getDevices(AudioManager.GET_DEVICES_OUTPUTS) }.getOrNull().orEmpty()
        val picked = outputs
            .filter { it.isSink && priorityForType(it.type) < Int.MAX_VALUE }
            .minByOrNull { priorityForType(it.type) }
        val resolved = picked?.toAudioOutputDevice() ?: defaultSpeaker
        _currentDevice.value = resolved
        if (emitConnectEvent && resolved.isExternal) {
            _deviceConnectedEvents.tryEmit(resolved)
        }
        return resolved
    }

    private fun priorityForType(type: Int): Int = when (type) {
        AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
        AudioDeviceInfo.TYPE_BLE_HEADSET,
        AudioDeviceInfo.TYPE_BLE_SPEAKER,
        AudioDeviceInfo.TYPE_BLE_BROADCAST,
        AudioDeviceInfo.TYPE_HEARING_AID -> 1
        AudioDeviceInfo.TYPE_USB_HEADSET,
        AudioDeviceInfo.TYPE_USB_DEVICE,
        AudioDeviceInfo.TYPE_USB_ACCESSORY -> 2
        AudioDeviceInfo.TYPE_WIRED_HEADSET,
        AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
        AudioDeviceInfo.TYPE_LINE_ANALOG,
        AudioDeviceInfo.TYPE_LINE_DIGITAL -> 3
        AudioDeviceInfo.TYPE_HDMI,
        AudioDeviceInfo.TYPE_HDMI_ARC,
        AudioDeviceInfo.TYPE_HDMI_EARC,
        AudioDeviceInfo.TYPE_AUX_LINE -> 4
        AudioDeviceInfo.TYPE_BUILTIN_SPEAKER,
        AudioDeviceInfo.TYPE_BUILTIN_EARPIECE -> 5
        else -> Int.MAX_VALUE
    }

    private fun AudioDeviceInfo.toAudioOutputDevice(): AudioOutputDevice {
        val typeLabel = when (type) {
            AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
            AudioDeviceInfo.TYPE_BLE_HEADSET,
            AudioDeviceInfo.TYPE_BLE_SPEAKER,
            AudioDeviceInfo.TYPE_BLE_BROADCAST -> "Bluetooth"
            AudioDeviceInfo.TYPE_HEARING_AID -> "Hearing Aid"
            AudioDeviceInfo.TYPE_USB_HEADSET,
            AudioDeviceInfo.TYPE_USB_DEVICE,
            AudioDeviceInfo.TYPE_USB_ACCESSORY -> "USB Audio"
            AudioDeviceInfo.TYPE_WIRED_HEADSET,
            AudioDeviceInfo.TYPE_WIRED_HEADPHONES -> "Wired"
            AudioDeviceInfo.TYPE_LINE_ANALOG,
            AudioDeviceInfo.TYPE_LINE_DIGITAL,
            AudioDeviceInfo.TYPE_AUX_LINE -> "Line Out"
            AudioDeviceInfo.TYPE_HDMI,
            AudioDeviceInfo.TYPE_HDMI_ARC,
            AudioDeviceInfo.TYPE_HDMI_EARC -> "HDMI"
            else -> "Speaker"
        }
        val isExt = priorityForType(type) <= 4
        val rawName = productName?.toString()?.trim().orEmpty()
        val displayName = when {
            !isExt -> "Phone Speaker"
            rawName.isNotBlank() -> rawName
            else -> "$typeLabel Audio"
        }
        val key = if (!isExt) {
            defaultSpeaker.key
        } else {
            val slug = displayName.lowercase().replace(Regex("[^a-z0-9._-]"), "_")
            "${typeLabel.lowercase().replace(' ', '_')}:$slug"
        }
        return AudioOutputDevice(
            key = key,
            name = displayName,
            typeLabel = typeLabel,
            isExternal = isExt
        )
    }
}
