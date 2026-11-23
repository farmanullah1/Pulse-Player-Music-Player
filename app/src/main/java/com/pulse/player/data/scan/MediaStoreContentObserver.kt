package com.pulse.player.data.scan

import android.content.Context
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import com.pulse.player.data.settings.SettingsStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Watches Android MediaStore audio URI changes and triggers a debounced incremental
 * delta scan so newly downloaded or deleted songs appear automatically without
 * forcing expensive startup full scans.
 */
class MediaStoreContentObserver(
    private val ctx: Context,
    private val scope: CoroutineScope,
    private val scanner: LibraryScanner,
    private val settings: SettingsStore
) : ContentObserver(Handler(Looper.getMainLooper())) {

    private var debounceJob: Job? = null
    private var registered = false

    fun register() {
        if (registered) return
        runCatching {
            ctx.contentResolver.registerContentObserver(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                true,
                this
            )
            registered = true
        }
    }

    fun unregister() {
        debounceJob?.cancel()
        debounceJob = null
        if (!registered) return
        runCatching {
            ctx.contentResolver.unregisterContentObserver(this)
        }
        registered = false
    }

    override fun onChange(selfChange: Boolean) {
        onChange(selfChange, null)
    }

    override fun onChange(selfChange: Boolean, uri: Uri?) {
        if (ContextCompat.checkSelfPermission(ctx, audioPermission()) != PackageManager.PERMISSION_GRANTED) {
            return
        }
        debounceJob?.cancel()
        debounceJob = scope.launch {
            delay(1500L)
            val excluded = runCatching { settings.excludedFoldersNow() }.getOrDefault(emptySet())
            scanner.scanDevice(excluded)
        }
    }
}
