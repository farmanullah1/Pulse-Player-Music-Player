package com.pulse.player.playback

import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.provider.Settings

sealed interface RingtoneResult {
    data object Done : RingtoneResult
    data class NeedsPermission(val uri: String, val type: Int) : RingtoneResult
    data class Failed(val reason: String) : RingtoneResult
}

object RingtoneHelper {
    fun trySetRingtone(ctx: Context, uri: Uri, type: Int): RingtoneResult {
        if (!Settings.System.canWrite(ctx)) {
            val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS)
                .setData(Uri.parse("package:${ctx.packageName}"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            ctx.startActivity(intent)
            return RingtoneResult.NeedsPermission(uri.toString(), type)
        }
        return runCatching {
            RingtoneManager.setActualDefaultRingtoneUri(ctx, type, uri)
            RingtoneResult.Done
        }.getOrElse {
            RingtoneResult.Failed("Couldn't use this file as a ringtone.")
        }
    }

    fun setRingtone(ctx: Context, uri: Uri, type: Int): Boolean {
        return trySetRingtone(ctx, uri, type) is RingtoneResult.Done
    }
}
