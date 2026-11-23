package com.pulse.player.ui.widget

import android.content.ComponentName
import android.content.Context
import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.media3.session.MediaController
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionToken
import com.pulse.player.MainActivity
import com.pulse.player.PulseApp
import com.pulse.player.R
import com.pulse.player.playback.MusicPlaybackService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.withContext
import java.io.File

class PlayerWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Responsive(
        setOf(
            DpSize(110.dp, 110.dp),
            DpSize(250.dp, 110.dp),
            DpSize(250.dp, 200.dp)
        )
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val sp = WidgetStateStore.read(context)
        val artFile = File(context.cacheDir, "widget_art.png")
        val artBitmap = if (artFile.exists()) {
            runCatching { BitmapFactory.decodeFile(artFile.path) }.getOrNull()
        } else null

        provideContent {
            GlanceTheme {
                val size = LocalSize.current
                val has = sp.getBoolean("has", false)
                val playing = sp.getBoolean("playing", false)
                val isFavorite = sp.getBoolean("isFavorite", false)
                val title = sp.getString("title", null) ?: "Nothing playing"
                val artist = sp.getString("artist", null).orEmpty()
                val pos = sp.getLong("pos", 0L).coerceAtLeast(0L)
                val dur = sp.getLong("dur", 0L).coerceAtLeast(0L)
                val progress = if (dur > 0L) (pos.toFloat() / dur.toFloat()).coerceIn(0f, 1f) else 0f

                val bgModifier = GlanceModifier
                    .fillMaxSize()
                    .background(GlanceTheme.colors.widgetBackground)
                    .cornerRadius(24.dp)
                    .padding(12.dp)
                    .clickable(actionStartActivity<MainActivity>())

                val iconTint = ColorFilter.tint(GlanceTheme.colors.onSurface)
                val primaryTint = ColorFilter.tint(GlanceTheme.colors.primary)
                val favRes = if (isFavorite) R.drawable.ic_w_fav_filled else R.drawable.ic_w_fav_outline
                val favTint = if (isFavorite) primaryTint else ColorFilter.tint(GlanceTheme.colors.onSurfaceVariant)

                when {
                    size.width < 200.dp -> {
                        // 2x2 compact
                        Column(
                            modifier = bgModifier,
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (artBitmap != null) {
                                Image(
                                    provider = ImageProvider(artBitmap),
                                    contentDescription = "Artwork",
                                    modifier = GlanceModifier.size(56.dp).cornerRadius(12.dp)
                                )
                            } else {
                                Image(
                                    provider = ImageProvider(R.drawable.ic_w_note),
                                    contentDescription = "Artwork",
                                    colorFilter = primaryTint,
                                    modifier = GlanceModifier.size(56.dp)
                                )
                            }
                            Spacer(GlanceModifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                val playRes = if (playing) R.drawable.ic_w_pause else R.drawable.ic_w_play
                                Image(
                                    provider = ImageProvider(playRes),
                                    contentDescription = if (playing) "Pause" else "Play",
                                    colorFilter = iconTint,
                                    modifier = GlanceModifier
                                        .size(36.dp)
                                        .clickable(actionRunCallback<PlayPauseCb>())
                                )
                                Spacer(GlanceModifier.width(8.dp))
                                Image(
                                    provider = ImageProvider(R.drawable.ic_w_next),
                                    contentDescription = "Next",
                                    colorFilter = iconTint,
                                    modifier = GlanceModifier
                                        .size(36.dp)
                                        .clickable(actionRunCallback<NextCb>())
                                )
                            }
                        }
                    }
                    size.height < 150.dp -> {
                        // 4x1 wide strip
                        Column(
                            modifier = bgModifier,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = GlanceModifier.fillMaxWidth().defaultWeight(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (artBitmap != null) {
                                    Image(
                                        provider = ImageProvider(artBitmap),
                                        contentDescription = "Artwork",
                                        modifier = GlanceModifier.size(50.dp).cornerRadius(12.dp)
                                    )
                                } else {
                                    Image(
                                        provider = ImageProvider(R.drawable.ic_w_note),
                                        contentDescription = "Artwork",
                                        colorFilter = primaryTint,
                                        modifier = GlanceModifier.size(50.dp)
                                    )
                                }
                                Spacer(GlanceModifier.width(10.dp))
                                Column(modifier = GlanceModifier.defaultWeight()) {
                                    Text(
                                        text = title,
                                        maxLines = 1,
                                        style = TextStyle(
                                            color = GlanceTheme.colors.onSurface,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                    )
                                    Text(
                                        text = artist.ifBlank { "Pulse Music" },
                                        maxLines = 1,
                                        style = TextStyle(
                                            color = GlanceTheme.colors.onSurfaceVariant,
                                            fontSize = 12.sp
                                        )
                                    )
                                }
                                if (has) {
                                    Image(
                                        provider = ImageProvider(favRes),
                                        contentDescription = if (isFavorite) "Unfavorite" else "Favorite",
                                        colorFilter = favTint,
                                        modifier = GlanceModifier
                                            .size(32.dp)
                                            .padding(4.dp)
                                            .clickable(actionRunCallback<ToggleFavoriteCb>())
                                    )
                                }
                                Image(
                                    provider = ImageProvider(R.drawable.ic_w_prev),
                                    contentDescription = "Previous",
                                    colorFilter = iconTint,
                                    modifier = GlanceModifier
                                        .size(34.dp)
                                        .clickable(actionRunCallback<PrevCb>())
                                )
                                val playRes = if (playing) R.drawable.ic_w_pause else R.drawable.ic_w_play
                                Image(
                                    provider = ImageProvider(playRes),
                                    contentDescription = if (playing) "Pause" else "Play",
                                    colorFilter = primaryTint,
                                    modifier = GlanceModifier
                                        .size(36.dp)
                                        .clickable(actionRunCallback<PlayPauseCb>())
                                )
                                Image(
                                    provider = ImageProvider(R.drawable.ic_w_next),
                                    contentDescription = "Next",
                                    colorFilter = iconTint,
                                    modifier = GlanceModifier
                                        .size(34.dp)
                                        .clickable(actionRunCallback<NextCb>())
                                )
                            }
                            if (has && dur > 0L) {
                                Spacer(GlanceModifier.height(4.dp))
                                LinearProgressIndicator(
                                    progress = progress,
                                    modifier = GlanceModifier.fillMaxWidth().height(3.dp).cornerRadius(2.dp),
                                    color = GlanceTheme.colors.primary,
                                    backgroundColor = GlanceTheme.colors.surfaceVariant
                                )
                            }
                        }
                    }
                    else -> {
                        // 4x2 expanded
                        Column(modifier = bgModifier) {
                            Row(
                                modifier = GlanceModifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (artBitmap != null) {
                                    Image(
                                        provider = ImageProvider(artBitmap),
                                        contentDescription = "Artwork",
                                        modifier = GlanceModifier.size(72.dp).cornerRadius(14.dp)
                                    )
                                } else {
                                    Image(
                                        provider = ImageProvider(R.drawable.ic_w_note),
                                        contentDescription = "Artwork",
                                        colorFilter = primaryTint,
                                        modifier = GlanceModifier.size(72.dp)
                                    )
                                }
                                Spacer(GlanceModifier.width(12.dp))
                                Column(modifier = GlanceModifier.defaultWeight()) {
                                    Text(
                                        text = title,
                                        maxLines = 1,
                                        style = TextStyle(
                                            color = GlanceTheme.colors.onSurface,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp
                                        )
                                    )
                                    Spacer(GlanceModifier.height(4.dp))
                                    Text(
                                        text = artist.ifBlank { "Pulse Music" },
                                        maxLines = 1,
                                        style = TextStyle(
                                            color = GlanceTheme.colors.onSurfaceVariant,
                                            fontSize = 13.sp
                                        )
                                    )
                                }
                                if (has) {
                                    Image(
                                        provider = ImageProvider(favRes),
                                        contentDescription = if (isFavorite) "Unfavorite" else "Favorite",
                                        colorFilter = favTint,
                                        modifier = GlanceModifier
                                            .size(40.dp)
                                            .padding(6.dp)
                                            .clickable(actionRunCallback<ToggleFavoriteCb>())
                                    )
                                }
                            }
                            Spacer(GlanceModifier.defaultWeight())
                            if (has && dur > 0L) {
                                LinearProgressIndicator(
                                    progress = progress,
                                    modifier = GlanceModifier.fillMaxWidth().height(4.dp).cornerRadius(2.dp),
                                    color = GlanceTheme.colors.primary,
                                    backgroundColor = GlanceTheme.colors.surfaceVariant
                                )
                                Spacer(GlanceModifier.height(10.dp))
                            }
                            Row(
                                modifier = GlanceModifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Image(
                                    provider = ImageProvider(R.drawable.ic_w_prev),
                                    contentDescription = "Previous",
                                    colorFilter = iconTint,
                                    modifier = GlanceModifier
                                        .size(44.dp)
                                        .padding(4.dp)
                                        .clickable(actionRunCallback<PrevCb>())
                                )
                                Spacer(GlanceModifier.width(18.dp))
                                val playRes = if (playing) R.drawable.ic_w_pause else R.drawable.ic_w_play
                                Image(
                                    provider = ImageProvider(playRes),
                                    contentDescription = if (playing) "Pause" else "Play",
                                    colorFilter = primaryTint,
                                    modifier = GlanceModifier
                                        .size(48.dp)
                                        .padding(4.dp)
                                        .clickable(actionRunCallback<PlayPauseCb>())
                                )
                                Spacer(GlanceModifier.width(18.dp))
                                Image(
                                    provider = ImageProvider(R.drawable.ic_w_next),
                                    contentDescription = "Next",
                                    colorFilter = iconTint,
                                    modifier = GlanceModifier
                                        .size(44.dp)
                                        .padding(4.dp)
                                        .clickable(actionRunCallback<NextCb>())
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

object WidgetControls {
    suspend fun send(ctx: Context, block: (MediaController) -> Unit) = withContext(Dispatchers.Main) {
        val token = SessionToken(ctx, ComponentName(ctx, MusicPlaybackService::class.java))
        val c = MediaController.Builder(ctx, token).buildAsync().await()
        block(c)
        delay(300)
        c.release()
    }
}

class PlayPauseCb : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        WidgetControls.send(context) { if (it.isPlaying) it.pause() else it.play() }
    }
}

class NextCb : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        WidgetControls.send(context) { it.seekToNextMediaItem() }
    }
}

class PrevCb : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        WidgetControls.send(context) { it.seekToPreviousMediaItem() }
    }
}

class ToggleFavoriteCb : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val mediaId = WidgetStateStore.read(context).getString("mediaId", null) ?: return
        withContext(Dispatchers.IO) {
            val app = context.applicationContext as? PulseApp
            app?.db?.favorites()?.toggle(mediaId, System.currentTimeMillis())
        }
        runCatching {
            WidgetControls.send(context) { controller ->
                controller.sendCustomCommand(
                    SessionCommand(MusicPlaybackService.ACTION_TOGGLE_FAVORITE, Bundle.EMPTY),
                    Bundle.EMPTY
                )
            }
        }
        runCatching {
            PlayerWidget().updateAll(context)
        }
    }
}
