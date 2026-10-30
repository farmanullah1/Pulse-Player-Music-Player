package com.pulse.player.data.scan

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.pulse.player.app
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class StorageReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        if (action == Intent.ACTION_MEDIA_MOUNTED ||
            action == Intent.ACTION_MEDIA_UNMOUNTED ||
            action == Intent.ACTION_MEDIA_EJECT
        ) {
            val app = context.app
            CoroutineScope(Dispatchers.IO).launch {
                val excluded = app.settings.excludedFolders.first()
                app.scanner.scanDevice(excluded)
            }
        }
    }
}

