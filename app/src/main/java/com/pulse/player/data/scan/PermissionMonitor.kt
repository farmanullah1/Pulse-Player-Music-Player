package com.pulse.player.data.scan

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.pulse.player.data.settings.SettingsStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class PermissionState { Granted, Limited, Denied, PermanentlyDenied }

class PermissionMonitor(private val ctx: Context, private val settings: SettingsStore) {
    private val _state = MutableStateFlow(PermissionState.Denied)
    val state = _state.asStateFlow()

    /** Call on Activity.onResume and right after a permission result. */
    suspend fun refresh(activity: Activity?) {
        val granted = ContextCompat.checkSelfPermission(ctx, audioPermission()) == PackageManager.PERMISSION_GRANTED
        val saf = ctx.contentResolver.persistedUriPermissions.any { it.isReadPermission }
        val asked = settings.audioPermissionAsked()
        _state.value = when {
            granted -> PermissionState.Granted
            saf -> PermissionState.Limited
            asked && activity != null && !ActivityCompat.shouldShowRequestPermissionRationale(activity, audioPermission()) -> PermissionState.PermanentlyDenied
            else -> PermissionState.Denied
        }
    }
}

