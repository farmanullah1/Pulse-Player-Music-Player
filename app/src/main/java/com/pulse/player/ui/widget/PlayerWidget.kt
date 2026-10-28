package com.pulse.player.ui.widget

import android.content.ComponentName
import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.color.ColorProvider
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
import androidx.media3.session.SessionToken
import com.pulse.player.MainActivity
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
            val size = LocalSize.current
            val has = sp.getBoolean("has", false)
            val playing = sp.getBoolean("playing", false)
            val title = sp.getString("title", null) ?: "Nothing playing"
            val artist = sp.getString("artist", null).orEmpty()

            val bgModifier = GlanceModifier
                .fillMaxSize()
                .background(ColorProvider(day = androidx.compose.ui.graphics.Color(0xFF14101F), night = androidx.compose.ui.graphics.Color(0xFF14101F)))
                .cornerRadius(24.dp)
                .padding(12.dp)
                .clickable(actionStartActivity<MainActivity>())

            when {
                size.width < 200.dp -> {
                    // 2x2
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
                                modifier = GlanceModifier.size(56.dp)
                            )
                        }
                        Spacer(GlanceModifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val playRes = if (playing) R.drawable.ic_w_pause else R.drawable.ic_w_play
                            Image(
                                provider = ImageProvider(playRes),
                                contentDescription = if (playing) "Pause" else "Play",
                                modifier = GlanceModifier
                                    .size(36.dp)
                                    .clickable(actionRunCallback<PlayPauseCb>())
                            )
                            Spacer(GlanceModifier.width(8.dp))
                            Image(
                                provider = ImageProvider(R.drawable.ic_w_next),
                                contentDescription = "Next",
                                modifier = GlanceModifier
                                    .size(36.dp)
                                    .clickable(actionRunCallback<NextCb>())
                            )
                        }
                    }
                }
                size.height < 150.dp -> {
                    // 4x1
                    Row(
                        modifier = bgModifier,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (artBitmap != null) {
                            Image(
                                provider = ImageProvider(artBitmap),
                                contentDescription = "Artwork",
                                modifier = GlanceModifier.size(54.dp).cornerRadius(12.dp)
                            )
                        } else {
                            Image(
                                provider = ImageProvider(R.drawable.ic_w_note),
                                contentDescription = "Artwork",
                                modifier = GlanceModifier.size(54.dp)
                            )
                        }
                        Spacer(GlanceModifier.width(12.dp))
                        Column(modifier = GlanceModifier.defaultWeight()) {
                            Text(
                                text = title,
                                maxLines = 1,
                                style = TextStyle(
                                    color = ColorProvider(day = androidx.compose.ui.graphics.Color.White, night = androidx.compose.ui.graphics.Color.White),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            )
                            Text(
                                text = artist.ifBlank { "Pulse Player" },
                                maxLines = 1,
                                style = TextStyle(
                                    color = ColorProvider(day = androidx.compose.ui.graphics.Color(0xFFA0A0A0), night = androidx.compose.ui.graphics.Color(0xFFA0A0A0)),
                                    fontSize = 12.sp
                                )
                            )
                        }
                        Image(
                            provider = ImageProvider(R.drawable.ic_w_prev),
                            contentDescription = "Previous",
                            modifier = GlanceModifier
                                .size(36.dp)
                                .clickable(actionRunCallback<PrevCb>())
                        )
                        Spacer(GlanceModifier.width(4.dp))
                        val playRes = if (playing) R.drawable.ic_w_pause else R.drawable.ic_w_play
                        Image(
                            provider = ImageProvider(playRes),
                            contentDescription = if (playing) "Pause" else "Play",
                            modifier = GlanceModifier
                                .size(36.dp)
                                .clickable(actionRunCallback<PlayPauseCb>())
                        )
                        Spacer(GlanceModifier.width(4.dp))
                        Image(
                            provider = ImageProvider(R.drawable.ic_w_next),
                            contentDescription = "Next",
                            modifier = GlanceModifier
                                .size(36.dp)
                                .clickable(actionRunCallback<NextCb>())
                        )
                    }
                }
                else -> {
                    // 4x2
                    Column(modifier = bgModifier) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (artBitmap != null) {
                                Image(
                                    provider = ImageProvider(artBitmap),
                                    contentDescription = "Artwork",
                                    modifier = GlanceModifier.size(76.dp).cornerRadius(14.dp)
                                )
                            } else {
                                Image(
                                    provider = ImageProvider(R.drawable.ic_w_note),
                                    contentDescription = "Artwork",
                                    modifier = GlanceModifier.size(76.dp)
                                )
                            }
                            Spacer(GlanceModifier.width(12.dp))
                            Column(modifier = GlanceModifier.defaultWeight()) {
                                Text(
                                    text = title,
                                    maxLines = 1,
                                    style = TextStyle(
                                        color = ColorProvider(day = androidx.compose.ui.graphics.Color.White, night = androidx.compose.ui.graphics.Color.White),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                )
                                Spacer(GlanceModifier.height(4.dp))
                                Text(
                                    text = artist.ifBlank { "Pulse Player" },
                                    maxLines = 1,
                                    style = TextStyle(
                                        color = ColorProvider(day = androidx.compose.ui.graphics.Color(0xFFA0A0A0), night = androidx.compose.ui.graphics.Color(0xFFA0A0A0)),
                                        fontSize = 13.sp
                                    )
                                )
                            }
                        }
                        Spacer(GlanceModifier.defaultWeight())
                        Row(
                            modifier = GlanceModifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Image(
                                provider = ImageProvider(R.drawable.ic_w_prev),
                                contentDescription = "Previous",
                                modifier = GlanceModifier
                                    .size(44.dp)
                                    .padding(4.dp)
                                    .clickable(actionRunCallback<PrevCb>())
                            )
                            Spacer(GlanceModifier.width(16.dp))
                            val playRes = if (playing) R.drawable.ic_w_pause else R.drawable.ic_w_play
                            Image(
                                provider = ImageProvider(playRes),
                                contentDescription = if (playing) "Pause" else "Play",
                                modifier = GlanceModifier
                                    .size(48.dp)
                                    .padding(4.dp)
                                    .clickable(actionRunCallback<PlayPauseCb>())
                            )
                            Spacer(GlanceModifier.width(16.dp))
                            Image(
                                provider = ImageProvider(R.drawable.ic_w_next),
                                contentDescription = "Next",
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
