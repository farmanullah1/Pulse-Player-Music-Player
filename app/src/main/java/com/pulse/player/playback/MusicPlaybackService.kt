package com.pulse.player.playback

import android.app.PendingIntent
import android.content.Context
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
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.session.CommandButton
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.pulse.player.MainActivity
import com.pulse.player.PulseApp
import com.pulse.player.R
import com.pulse.player.data.scan.ArtworkLoader
import com.pulse.player.ui.widget.WidgetStateStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.guava.future
import kotlinx.coroutines.launch

@OptIn(UnstableApi::class)
class MusicPlaybackService : MediaLibraryService() {
    private var session: MediaLibrarySession? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var libraryResolver: MediaLibraryTreeResolver
    @Volatile
    private var favoriteIds: Set<String> = emptySet()

    companion object {
        const val ACTION_SKIP_SILENCE = "pulse.SKIP_SILENCE"
        const val ACTION_TOGGLE_FAVORITE = "pulse.TOGGLE_FAVORITE"
        const val ACTION_CYCLE_REPEAT = "pulse.CYCLE_REPEAT"
    }

    override fun onCreate() {
        super.onCreate()
        val app = application as PulseApp
        val db = app.db
        val settings = app.settings
        libraryResolver = MediaLibraryTreeResolver(RoomMediaBrowseDataSource(db, settings))

        val audioAttrs = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()

        val monoProcessorA = MonoDownmixAudioProcessor()
        val monoProcessorB = MonoDownmixAudioProcessor()
        AudioFx.onMonoModeChanged = { enabled ->
            monoProcessorA.monoEnabled = enabled
            monoProcessorB.monoEnabled = enabled
        }

        fun renderersWithMono(processor: MonoDownmixAudioProcessor) = object : DefaultRenderersFactory(this) {
            override fun buildAudioSink(
                context: Context,
                enableFloatOutput: Boolean,
                enableAudioTrackPlaybackParams: Boolean
            ): AudioSink = DefaultAudioSink.Builder(context)
                .setEnableFloatOutput(enableFloatOutput)
                .setEnableAudioTrackPlaybackParams(enableAudioTrackPlaybackParams)
                .setAudioProcessors(arrayOf(processor))
                .build()
        }

        val player = ExoPlayer.Builder(this, renderersWithMono(monoProcessorA))
            .setAudioAttributes(audioAttrs, /* handleAudioFocus = */ true)
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_LOCAL)
            .build()

        PlaybackEngine.attach(this, db, player) {
            ExoPlayer.Builder(this, renderersWithMono(monoProcessorB))
                .setAudioAttributes(audioAttrs, /* handleAudioFocus = */ false)
                .build()
        }

        serviceScope.launch {
            combine(settings.crossfadeSeconds, settings.crossfadeEqualPower) { secs, eqPower ->
                secs to eqPower
            }.collect { (secs, eqPower) ->
                PlaybackEngine.setCrossfadeSeconds(secs, eqPower)
            }
        }
        serviceScope.launch {
            settings.smartAlbumGapless.collect { PlaybackEngine.smartAlbumGapless = it }
        }
        serviceScope.launch {
            combine(settings.replayGainMode, settings.replayGainPreampDb) { mode, preamp ->
                mode to preamp
            }.collect { (mode, preamp) ->
                PlaybackEngine.setReplayGainConfig(mode, preamp)
            }
        }
        serviceScope.launch {
            settings.eqLimiterEnabled.collect { AudioFx.setLimiterEnabled(it) }
        }
        serviceScope.launch {
            settings.eqChannelBalance.collect { AudioFx.setChannelBalance(it) }
        }
        serviceScope.launch {
            settings.eqMonoMode.collect { AudioFx.setMonoMode(it) }
        }
        serviceScope.launch {
            settings.fadeEnabled.collect { PlaybackEngine.fadeEnabled = it }
        }
        serviceScope.launch {
            settings.sleepFadeSec.collect { PlaybackEngine.setSleepFadeDurationMs(it * 1000L) }
        }

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

        val callback = object : MediaLibrarySession.Callback {
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

            override fun onGetLibraryRoot(
                session: MediaLibrarySession,
                browser: MediaSession.ControllerInfo,
                params: LibraryParams?
            ): ListenableFuture<LibraryResult<MediaItem>> {
                return Futures.immediateFuture(LibraryResult.ofItem(libraryResolver.rootItem(), params))
            }

            override fun onGetChildren(
                session: MediaLibrarySession,
                browser: MediaSession.ControllerInfo,
                parentId: String,
                page: Int,
                pageSize: Int,
                params: LibraryParams?
            ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
                return serviceScope.future(Dispatchers.IO) {
                    val items = libraryResolver.getChildren(parentId, page, pageSize)
                    LibraryResult.ofItemList(ImmutableList.copyOf(items), params)
                }
            }

            override fun onGetItem(
                session: MediaLibrarySession,
                browser: MediaSession.ControllerInfo,
                mediaId: String
            ): ListenableFuture<LibraryResult<MediaItem>> {
                return serviceScope.future(Dispatchers.IO) {
                    val item = libraryResolver.getItem(mediaId)
                    if (item != null) {
                        LibraryResult.ofItem(item, null)
                    } else {
                        LibraryResult.ofError(androidx.media3.session.SessionError.ERROR_BAD_VALUE)
                    }
                }
            }

            override fun onSearch(
                session: MediaLibrarySession,
                browser: MediaSession.ControllerInfo,
                query: String,
                params: LibraryParams?
            ): ListenableFuture<LibraryResult<Void>> {
                return serviceScope.future(Dispatchers.IO) {
                    val results = libraryResolver.search(query)
                    session.notifySearchResultChanged(browser, query, results.size, params)
                    LibraryResult.ofVoid(params)
                }
            }

            override fun onGetSearchResult(
                session: MediaLibrarySession,
                browser: MediaSession.ControllerInfo,
                query: String,
                page: Int,
                pageSize: Int,
                params: LibraryParams?
            ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
                return serviceScope.future(Dispatchers.IO) {
                    val all = libraryResolver.search(query, limit = 150)
                    val safeSize = if (pageSize <= 0) all.size else pageSize
                    val from = (page.coerceAtLeast(0) * safeSize).coerceAtMost(all.size)
                    val to = (from + safeSize).coerceAtMost(all.size)
                    LibraryResult.ofItemList(ImmutableList.copyOf(all.subList(from, to)), params)
                }
            }

            override fun onAddMediaItems(
                mediaSession: MediaSession,
                controller: MediaSession.ControllerInfo,
                mediaItems: MutableList<MediaItem>
            ): ListenableFuture<List<MediaItem>> {
                return serviceScope.future(Dispatchers.IO) {
                    libraryResolver.resolvePlayableItems(mediaItems)
                }
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

        val builtSession = MediaLibrarySession.Builder(this, fadingPlayer, callback)
            .setSessionActivity(sessionActivity)
            .setBitmapLoader(artworkBitmapLoader)
            .build()
        builtSession.setCustomLayout(buildCustomLayout(fadingPlayer))
        session = builtSession

        serviceScope.launch {
            db.favorites().observeIds().collect { ids ->
                val favSet = ids.toSet()
                favoriteIds = favSet
                WidgetStateStore.updateFavoriteIds(applicationContext, favSet, serviceScope)
                session?.let { activeSession ->
                    activeSession.setCustomLayout(buildCustomLayout(activeSession.player))
                }
            }
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? = session

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
