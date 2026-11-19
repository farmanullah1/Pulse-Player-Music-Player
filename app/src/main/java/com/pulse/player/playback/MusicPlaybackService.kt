package com.pulse.player.playback

import android.app.PendingIntent
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.BitmapLoader
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.CommandButton
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.pulse.player.MainActivity
import com.pulse.player.PulseApp
import com.pulse.player.R
import com.pulse.player.data.scan.ArtworkLoader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.guava.future
import kotlinx.coroutines.launch

@OptIn(UnstableApi::class)
class MusicPlaybackService : MediaSessionService() {
    private var session: MediaSession? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    @Volatile
    private var favoriteIds: Set<String> = emptySet()

    companion object {
        const val ACTION_SKIP_SILENCE = "pulse.SKIP_SILENCE"
        const val ACTION_TOGGLE_FAVORITE = "pulse.TOGGLE_FAVORITE"
        const val ACTION_CYCLE_REPEAT = "pulse.CYCLE_REPEAT"
    }

    override fun onCreate() {
        super.onCreate()
        val db = (application as PulseApp).db

        val player = ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                /* handleAudioFocus = */ true
            )
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_LOCAL)
            .build()

        PlaybackEngine.attach(this, db, player)

        val openNowPlayingIntent = Intent(this, MainActivity::class.java).apply {
            action = "pulse.OPEN_NOW_PLAYING"
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val sessionActivity = PendingIntent.getActivity(
            this,
            1001,
            openNowPlayingIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val artworkBitmapLoader = object : BitmapLoader {
            override fun supportsMimeType(mimeType: String): Boolean = true

            override fun decodeBitmap(data: ByteArray): ListenableFuture<Bitmap> {
                return serviceScope.future(Dispatchers.IO) {
                    BitmapFactory.decodeByteArray(data, 0, data.size)
                        ?: throw IllegalStateException("Failed to decode notification artwork")
                }
            }

            override fun loadBitmap(uri: Uri): ListenableFuture<Bitmap> {
                return serviceScope.future(Dispatchers.IO) {
                    ArtworkLoader.load(applicationContext, uri.toString(), 384)
                        ?: throw IllegalStateException("No artwork available for $uri")
                }
            }
        }

        val skipSilenceCmd = SessionCommand(ACTION_SKIP_SILENCE, Bundle.EMPTY)
        val toggleFavoriteCmd = SessionCommand(ACTION_TOGGLE_FAVORITE, Bundle.EMPTY)
        val cycleRepeatCmd = SessionCommand(ACTION_CYCLE_REPEAT, Bundle.EMPTY)

        fun buildCustomLayout(p: Player): List<CommandButton> {
            val currentId = p.currentMediaItem?.mediaId
            val isFav = currentId != null && currentId in favoriteIds
            val favBtn = CommandButton.Builder(
                if (isFav) CommandButton.ICON_HEART_FILLED else CommandButton.ICON_HEART_UNFILLED
            )
                .setDisplayName(if (isFav) "Unfavorite" else "Favorite")
                .setSessionCommand(toggleFavoriteCmd)
                .setEnabled(currentId != null)
                .build()

            val repeatIcon = when (p.repeatMode) {
                Player.REPEAT_MODE_ONE -> CommandButton.ICON_REPEAT_ONE
                Player.REPEAT_MODE_ALL -> CommandButton.ICON_REPEAT_ALL
                else -> CommandButton.ICON_REPEAT_OFF
            }
            val repeatLabel = when (p.repeatMode) {
                Player.REPEAT_MODE_ONE -> "Repeat One"
                Player.REPEAT_MODE_ALL -> "Repeat All"
                else -> "Repeat Off"
            }
            val repeatBtn = CommandButton.Builder(repeatIcon)
                .setDisplayName(repeatLabel)
                .setSessionCommand(cycleRepeatCmd)
                .setEnabled(true)
                .build()

            return listOf(favBtn, repeatBtn)
        }

        val callback = object : MediaSession.Callback {
            override fun onConnect(
                session: MediaSession,
                controller: MediaSession.ControllerInfo
            ): MediaSession.ConnectionResult {
                val connectionResult = super.onConnect(session, controller)
                val sessionCommands = connectionResult.availableSessionCommands.buildUpon()
                    .add(skipSilenceCmd)
                    .add(toggleFavoriteCmd)
                    .add(cycleRepeatCmd)
                    .build()
                return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                    .setAvailableSessionCommands(sessionCommands)
                    .setAvailablePlayerCommands(connectionResult.availablePlayerCommands)
                    .setCustomLayout(buildCustomLayout(session.player))
                    .build()
            }

            override fun onCustomCommand(
                session: MediaSession,
                controller: MediaSession.ControllerInfo,
                customCommand: SessionCommand,
                args: Bundle
            ): ListenableFuture<SessionResult> {
                when (customCommand.customAction) {
                    ACTION_SKIP_SILENCE -> {
                        val enabled = args.getBoolean("enabled", false)
                        player.skipSilenceEnabled = enabled
                        return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                    }
                    ACTION_TOGGLE_FAVORITE -> {
                        val currentId = session.player.currentMediaItem?.mediaId
                        if (currentId != null) {
                            serviceScope.launch(Dispatchers.IO) {
                                db.favorites().toggle(currentId, System.currentTimeMillis())
                            }
                        }
                        return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                    }
                    ACTION_CYCLE_REPEAT -> {
                        val nextMode = when (player.repeatMode) {
                            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                            else -> Player.REPEAT_MODE_OFF
                        }
                        player.repeatMode = nextMode
                        session.setCustomLayout(buildCustomLayout(session.player))
                        return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                    }
                }
                return super.onCustomCommand(session, controller, customCommand, args)
            }
        }

        val fadingPlayer = PlaybackEngine.fadingPlayer!!
        fadingPlayer.addListener(object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                session?.setCustomLayout(buildCustomLayout(fadingPlayer))
            }

            override fun onRepeatModeChanged(repeatMode: Int) {
                session?.setCustomLayout(buildCustomLayout(fadingPlayer))
            }
        })

        setMediaNotificationProvider(
            DefaultMediaNotificationProvider.Builder(this)
                .build()
                .apply { setSmallIcon(R.drawable.ic_notification) }
        )

        val builtSession = MediaSession.Builder(this, fadingPlayer)
            .setSessionActivity(sessionActivity)
            .setBitmapLoader(artworkBitmapLoader)
            .setCallback(callback)
            .build()
        builtSession.setCustomLayout(buildCustomLayout(fadingPlayer))
        session = builtSession

        serviceScope.launch {
            db.favorites().observeIds().collect { ids ->
                favoriteIds = ids.toSet()
                session?.let { activeSession ->
                    activeSession.setCustomLayout(buildCustomLayout(activeSession.player))
                }
            }
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = session

    override fun onTaskRemoved(rootIntent: Intent?) {
        val p = PlaybackEngine.exo
        if (p == null || !p.playWhenReady || p.mediaItemCount == 0) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        serviceScope.cancel()
        session?.release()
        session = null
        PlaybackEngine.detach()
        super.onDestroy()
    }
}
