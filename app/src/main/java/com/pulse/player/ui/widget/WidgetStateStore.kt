package com.pulse.player.ui.widget

import android.content.Context
import android.graphics.Bitmap
import androidx.media3.common.Player
import com.pulse.player.data.scan.ArtworkLoader
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

object WidgetStateStore {
    private const val PREFS = "widget_state"

    fun write(ctx: Context, p: Player, scope: CoroutineScope) {
        val md = p.mediaMetadata
        val artPath = File(ctx.cacheDir, "widget_art.png")
        scope.launch(Dispatchers.IO) {
            val uri = p.currentMediaItem?.localConfiguration?.uri?.toString()
            if (uri != null) {
                ArtworkLoader.load(ctx, uri, 160)?.let { b ->
                    artPath.outputStream().use { b.compress(Bitmap.CompressFormat.PNG, 90, it) }
                }
            } else {
                artPath.delete()
            }
            ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putString("title", md.title?.toString())
                .putString("artist", md.artist?.toString())
                .putBoolean("playing", p.isPlaying)
                .putBoolean("has", p.mediaItemCount > 0)
                .putLong("pos", p.currentPosition)
                .putLong("dur", if (p.duration < 0) 0L else p.duration)
                .apply()
            runCatching {
                PlayerWidget().updateAll(ctx)
            }
        }
    }

    fun read(ctx: Context) = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
