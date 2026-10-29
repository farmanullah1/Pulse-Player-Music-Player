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
        val title = md.title?.toString()
        val artist = md.artist?.toString()
        val isPlaying = p.isPlaying
        val has = p.mediaItemCount > 0
        val pos = p.currentPosition
        val dur = if (p.duration < 0) 0L else p.duration
        val uri = p.currentMediaItem?.localConfiguration?.uri?.toString()
        val artPath = File(ctx.cacheDir, "widget_art.png")

        scope.launch(Dispatchers.IO) {
            if (uri != null) {
                ArtworkLoader.load(ctx, uri, 160)?.let { b ->
                    runCatching {
                        artPath.outputStream().use { b.compress(Bitmap.CompressFormat.PNG, 90, it) }
                    }
                }
            } else {
                artPath.delete()
            }
            ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putString("title", title)
                .putString("artist", artist)
                .putBoolean("playing", isPlaying)
                .putBoolean("has", has)
                .putLong("pos", pos)
                .putLong("dur", dur)
                .apply()
            runCatching {
                PlayerWidget().updateAll(ctx)
            }
        }
    }

    fun read(ctx: Context) = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
