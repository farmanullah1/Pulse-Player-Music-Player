package com.pulse.player.playback

import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.provider.Settings

object RingtoneHelper {
    fun setRingtone(ctx: Context, uri: Uri, type: Int): Boolean {
        if (!Settings.System.canWrite(ctx)) {
            val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS)
                .setData(Uri.parse("package:${ctx.packageName}"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            ctx.startActivity(intent)
            return false
        }
        return runCatching {
            RingtoneManager.setActualDefaultRingtoneUri(ctx, type, uri)
            true
        }.getOrDefault(false)
    }
}
