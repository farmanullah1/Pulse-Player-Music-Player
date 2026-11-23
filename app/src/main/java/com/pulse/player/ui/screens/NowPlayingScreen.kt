package com.pulse.player.ui.screens

import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.DashboardCustomize
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.FastForward
import androidx.compose.material.icons.rounded.FastRewind
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pulse.player.data.scan.ArtworkLoader
import com.pulse.player.playback.PlaybackUiState
import com.pulse.player.playback.Track
import com.pulse.player.ui.components.AudioSignalPathBadge
import com.pulse.player.ui.components.AuroraBackground
import com.pulse.player.ui.components.HeartButton
import com.pulse.player.ui.components.PlaybackErrorBanner
import com.pulse.player.ui.components.formatTime
import com.pulse.player.ui.screens.layouts.AtmosphericQueuePlayerLayout
import com.pulse.player.ui.screens.layouts.AtmosphericQueueUiState
import com.pulse.player.ui.screens.layouts.Track as QueueTrack
import com.pulse.player.ui.theme.TabularMono
import com.pulse.player.ui.themes.PlayerLayout
import com.pulse.player.ui.themes.PlayerThemes
import com.pulse.player.ui.viewmodel.MainViewModel
import kotlinx.coroutines.delay

@Composable
fun NowPlayingScreen(vm: MainViewModel) {
    BackHandler { vm.closeNowPlaying() }

    val s by vm.player.state.collectAsStateWithLifecycle()
    val themeId by vm.playerThemeId.collectAsStateWithLifecycle()
    val theme = PlayerThemes.firstOrNull { it.id == themeId } ?: PlayerThemes[0]

    if (PlayerLayoutMode.fromId(themeId) == PlayerLayoutMode.ATMOSPHERIC_QUEUE &&
        theme.layout == PlayerLayout.ATMOSPHERIC_QUEUE
    ) {
        AtmosphericQueuePlayer(vm = vm, s = s, onBack = vm::closeNowPlaying)
        return
    }
    if (theme.layout == PlayerLayout.IMMERSIVE) {
        com.pulse.player.ui.theme.immersive.ImmersivePlayerLayout(s = s, vm = vm, onBack = vm::closeNowPlaying)
        return
    }
    if (theme.layout == PlayerLayout.PIXEL) {
        com.pulse.player.ui.theme.pixel.PixelPlayerLayout(s = s, vm = vm, onBack = vm::closeNowPlaying)
        return
    }
    if (theme.layout == PlayerLayout.NAMIDA) {
        com.pulse.player.ui.theme.namida.NamidaPlayerLayout(s = s, vm = vm, onBack = vm::closeNowPlaying)
        return
    }

    val ctx = LocalContext.current
    val fav by vm.isCurrentFavorite.collectAsStateWithLifecycle()
    val artworkAccent by vm.artworkAccent.collectAsStateWithLifecycle()
    val seekStyle by vm.seekStyle.collectAsStateWithLifecycle()
    val hiddenElements by vm.hiddenPlayerElements.collectAsStateWithLifecycle()
    val customButtonOrder by vm.customButtonOrder.collectAsStateWithLifecycle()
    val backdropBlur by vm.playerBackdropBlur.collectAsStateWithLifecycle()
    val backdropDim by vm.playerBackdropDim.collectAsStateWithLifecycle()
    val seekbarHeight by vm.playerSeekbarHeight.collectAsStateWithLifecycle()
    val queue by vm.player.queue.collectAsStateWithLifecycle()
    var showLyrics by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(s.mediaId) {
        s.artworkUri?.let { ArtworkLoader.load(ctx, it, 128) }
    }

    val seekHeightDp = when (seekbarHeight) {
        "COMPACT" -> 34.dp
        "BOLD" -> 56.dp
        else -> 44.dp
    }
    val audioEffectsActive = s.speed != 1f || s.pitchSemitones != 0f || s.abLoopStartMs != null || s.abLoopEndMs != null
    val abStartFrac = s.abLoopStartMs?.takeIf { s.durationMs > 0L }?.let { (it.toFloat() / s.durationMs).coerceIn(0f, 1f) }
    val abEndFrac = s.abLoopEndMs?.takeIf { s.durationMs > 0L }?.let { (it.toFloat() / s.durationMs).coerceIn(0f, 1f) }

    AuroraBackground(accent = artworkAccent, modifier = Modifier.fillMaxSize()) {
        Box(
            Modifier
                .fillMaxSize()
                .then(
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && backdropBlur > 0.05f) {
                        Modifier.blur((backdropBlur.coerceIn(0f, 1f) * 26f).dp)
                    } else Modifier
                )
                .background(
                    Brush.verticalGradient(
                        listOf(
                            artworkAccent.copy(alpha = backdropBlur.coerceIn(0f, 1f) * 0.14f),
                            Color.Black.copy(alpha = ((backdropDim - 0.15f).coerceIn(0f, 0.7f) * 0.55f))
                        )
                    )
                )
        )
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .pointerInput(Unit) {
                        detectVerticalDragGestures { _, dragAmount ->
                            if (dragAmount > 25f) vm.closeNowPlaying()
                        }
                    },
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = vm::closeNowPlaying, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Rounded.KeyboardArrowDown, "Collapse player", tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(30.dp))
                }
                Spacer(Modifier.weight(1f))
                if ("lyrics_preview" !in hiddenElements) {
                    SongLyricsSegment(showLyrics) { showLyrics = it }
                }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = { vm.openAudioControls() }, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Rounded.Speed, "Audio & Loop Controls", tint = if (audioEffectsActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                }
                IconButton(
                    onClick = {
                        vm.closeNowPlaying()
                        vm.go(Screen.Customize)
                    },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(Icons.Rounded.DashboardCustomize, "Customize Player", tint = MaterialTheme.colorScheme.onSurface)
                }
                PlayerTopMenu(vm)
            }

            Spacer(Modifier.height(12.dp))

            if (s.trackError != null) {
                PlaybackErrorBanner(
                    error = s.trackError!!,
                    onRetry = vm.player::retry,
                    onSkip = vm.player::skip,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            if (showLyrics) {
                LyricsView(vm = vm, mediaId = s.mediaId, modifier = Modifier.weight(1f))
            } else {
                StageWithDoubleTapFeedback(
                    vm = vm,
                    s = s,
                    fav = fav,
                    artworkAccent = artworkAccent,
                    modifier = Modifier.weight(1f).fillMaxWidth()
                )
            }

            Spacer(Modifier.height(14.dp))
            Text(s.title.ifBlank { "No track selected" }, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(4.dp))
            Text(s.artist.ifBlank { "Unknown artist" }, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)

            NowPlayingStatusBadges(vm = vm, s = s, fav = fav, hiddenElements = hiddenElements, audioEffectsActive = audioEffectsActive)

            Spacer(Modifier.height(14.dp))
            SeekBar(
                position = vm.player.position,
                duration = s.durationMs,
                isPlaying = s.isPlaying,
                trackKey = s.mediaId ?: "default",
                seekStyle = seekStyle,
                onSelectStyle = vm::setSeekStyle,
                showLabels = "waveform_seek" !in hiddenElements,
                seekBarHeight = seekHeightDp,
                abStartFraction = abStartFrac,
                abEndFraction = abEndFrac,
                onSeek = vm.player::seekTo
            )

            Spacer(Modifier.height(14.dp))
            TransportRow(s = s, p = vm.player)
            Spacer(Modifier.height(16.dp))

            NowPlayingQuickActionsRow(
                vm = vm,
                s = s,
                fav = fav,
                showLyrics = showLyrics,
                audioEffectsActive = audioEffectsActive,
                customButtonOrder = customButtonOrder,
                hiddenElements = hiddenElements,
                onToggleLyrics = { showLyrics = !showLyrics }
            )

            val nextTrack = queue.getOrNull(s.index + 1)
            if ("queue_preview" !in hiddenElements && nextTrack != null) {
                Spacer(Modifier.height(6.dp))
                Surface(
                    onClick = { vm.openQueue() },
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.QueueMusic, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                        Text("Up next: ${nextTrack.title}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun StageWithDoubleTapFeedback(
    vm: MainViewModel,
    s: PlaybackUiState,
    fav: Boolean,
    artworkAccent: Color,
    modifier: Modifier = Modifier
) {
    val tapHaptic = LocalHapticFeedback.current
    var tapFeedbackLabel by remember { mutableStateOf<String?>(null) }
    var tapFeedbackNonce by remember { mutableIntStateOf(0) }
    LaunchedEffect(tapFeedbackNonce) {
        if (tapFeedbackNonce > 0) {
            delay(750)
            tapFeedbackLabel = null
        }
    }

    BoxWithConstraints(
        modifier = modifier.pointerInput(s.mediaId, s.canSeek) {
            detectTapGestures(
                onDoubleTap = { offset ->
                    val w = size.width.toFloat().coerceAtLeast(1f)
                    when {
                        offset.x < w * 0.34f -> if (vm.player.skipBy(-10_000L)) {
                            tapHaptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            tapFeedbackLabel = "-10s"
                            tapFeedbackNonce++
                        }
                        offset.x > w * 0.66f -> if (vm.player.skipBy(10_000L)) {
                            tapHaptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            tapFeedbackLabel = "+10s"
                            tapFeedbackNonce++
                        }
                        else -> {
                            tapHaptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            vm.toggleFavoriteCurrent()
                            tapFeedbackLabel = if (!fav) "Favourited" else "Unfavourited"
                            tapFeedbackNonce++
                        }
                    }
                }
            )
        },
        contentAlignment = Alignment.Center
    ) {
        PlayerThemeHost(vm, s, artworkAccent)
        AnimatedVisibility(
            visible = tapFeedbackLabel != null,
            enter = fadeIn(tween(120)) + scaleIn(initialScale = 0.85f, animationSpec = tween(120)),
            exit = fadeOut(tween(180)) + scaleOut(targetScale = 0.9f, animationSpec = tween(180)),
            modifier = Modifier.align(Alignment.Center)
        ) {
            val label = tapFeedbackLabel.orEmpty()
            val icon = when (label) {
                "-10s" -> Icons.Rounded.FastRewind
                "+10s" -> Icons.Rounded.FastForward
                else -> Icons.Rounded.Favorite
            }
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.92f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                tonalElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    Text(label, style = MaterialTheme.typography.titleSmall.merge(TabularMono), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }
    }
}

@Composable
private fun NowPlayingStatusBadges(
    vm: MainViewModel,
    s: PlaybackUiState,
    fav: Boolean,
    hiddenElements: Set<String>,
    audioEffectsActive: Boolean
) {
    val sleepVisible = (s.sleepRemainingMs > 0 || s.sleepEndOfTrack) && "sleep_timer" !in hiddenElements
    if ("signal_path" !in hiddenElements || sleepVisible || audioEffectsActive || s.appliedReplayGainDb != 0f) {
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            if ("signal_path" !in hiddenElements) {
                AudioSignalPathBadge(
                    track = Track(
                        id = s.mediaId ?: "current",
                        uri = s.artworkUri ?: "",
                        title = s.title,
                        artist = s.artist,
                        album = s.album,
                        durationMs = s.durationMs,
                        isFavorite = fav
                    ),
                    eqActive = true
                )
            }
            if (s.appliedReplayGainDb != 0f) {
                Surface(onClick = { vm.go(Screen.Equalizer) }, shape = CircleShape, color = MaterialTheme.colorScheme.secondaryContainer) {
                    Text(
                        text = "RG %+.1f dB".format(s.appliedReplayGainDb),
                        style = MaterialTheme.typography.labelSmall.merge(TabularMono),
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
            if (audioEffectsActive) {
                Surface(onClick = vm::openAudioControls, shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
                    Text(
                        text = buildList {
                            if (s.speed != 1f) add("%.2fx".format(s.speed))
                            if (s.pitchSemitones != 0f) add("%+.0fst".format(s.pitchSemitones))
                            if (s.abLoopStartMs != null || s.abLoopEndMs != null) add("A-B")
                        }.joinToString(" · "),
                        style = MaterialTheme.typography.labelSmall.merge(TabularMono),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
            if (sleepVisible) {
                Surface(onClick = vm::openSleepSheet, shape = CircleShape, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Rounded.Bedtime, "Sleep timer active", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(13.dp))
                        Text(
                            text = if (s.sleepEndOfTrack) "End of track" else formatTime(s.sleepRemainingMs),
                            style = MaterialTheme.typography.labelSmall.merge(TabularMono),
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NowPlayingQuickActionsRow(
    vm: MainViewModel,
    s: PlaybackUiState,
    fav: Boolean,
    showLyrics: Boolean,
    audioEffectsActive: Boolean,
    customButtonOrder: List<String>,
    hiddenElements: Set<String>,
    onToggleLyrics: () -> Unit
) {
    val quickActions = remember(customButtonOrder, hiddenElements) {
        val supported = listOf("Favorite", "Sleep Timer", "Add to Playlist", "Queue", "Equalizer", "Lyrics", "Speed / Pitch")
        val ordered = customButtonOrder.filter { it in supported }
        val base = if (ordered.isNotEmpty()) ordered else listOf("Favorite", "Sleep Timer", "Add to Playlist", "Queue", "Equalizer")
        base.filter { action -> !(action == "Favorite" && "favorite_heart" in hiddenElements) }
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
        quickActions.forEach { action ->
            when (action) {
                "Favorite" -> HeartButton(on = fav, onToggle = { vm.toggleFavoriteCurrent() })
                "Sleep Timer" -> IconButton(onClick = { vm.openSleepSheet() }, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Rounded.Bedtime, "Sleep timer", tint = if (s.sleepRemainingMs > 0 || s.sleepEndOfTrack) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                }
                "Add to Playlist" -> IconButton(onClick = { vm.openAddToPlaylist() }, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.AutoMirrored.Rounded.PlaylistAdd, "Add to playlist", tint = MaterialTheme.colorScheme.onSurface)
                }
                "Queue" -> IconButton(onClick = { vm.openQueue() }, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.AutoMirrored.Rounded.QueueMusic, "Queue", tint = MaterialTheme.colorScheme.onSurface)
                }
                "Equalizer" -> IconButton(onClick = { vm.go(Screen.Equalizer) }, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Rounded.Equalizer, "Equalizer", tint = MaterialTheme.colorScheme.onSurface)
                }
                "Lyrics" -> IconButton(onClick = onToggleLyrics, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Rounded.Lyrics, "Toggle lyrics", tint = if (showLyrics) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                }
                "Speed / Pitch" -> IconButton(onClick = { vm.openAudioControls() }, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Rounded.Speed, "Audio & Loop Controls", tint = if (audioEffectsActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                }
            }
        }
    }
}

@Composable
private fun AtmosphericQueuePlayer(vm: MainViewModel, s: PlaybackUiState, onBack: () -> Unit) {
    val position by vm.player.position.collectAsStateWithLifecycle()
    val favorite by vm.isCurrentFavorite.collectAsStateWithLifecycle()
    val queue by vm.player.queue.collectAsStateWithLifecycle()
    val library by vm.tracks.collectAsStateWithLifecycle()
    val splitUpperRatio by vm.splitPlayerUpperRatio.collectAsStateWithLifecycle()
    val coverShape by vm.playerCoverShape.collectAsStateWithLifecycle()
    val backdropBlur by vm.playerBackdropBlur.collectAsStateWithLifecycle()
    val backdropDim by vm.playerBackdropDim.collectAsStateWithLifecycle()
    val artworkGlow by vm.playerArtworkGlow.collectAsStateWithLifecycle()
    val swipeSkip by vm.playerSwipeSkip.collectAsStateWithLifecycle()
    val seekbarHeight by vm.playerSeekbarHeight.collectAsStateWithLifecycle()
    val cornerRadiusDp by vm.cornerRadiusDp.collectAsStateWithLifecycle()
    var showLyrics by rememberSaveable { mutableStateOf(false) }

    val libraryById = remember(library) { library.associateBy { it.id } }
    val currentStored = remember(s.mediaId, libraryById) { s.mediaId?.let { libraryById[it] } }
    val currentQueued = remember(s.mediaId, queue) { queue.firstOrNull { it.id == s.mediaId } }
    val resolvedCodec = remember(currentStored?.codec, currentStored?.uri, currentQueued?.uri, s.artworkUri) {
        val rawCodec = currentStored?.codec?.trim().orEmpty()
        if (rawCodec.isNotEmpty()) rawCodec.uppercase() else {
            val uriName = (currentStored?.uri ?: currentQueued?.uri ?: s.artworkUri).orEmpty()
                .substringAfterLast('/').substringBefore('?').lowercase()
            when {
                uriName.endsWith(".flac") -> "FLAC"
                uriName.endsWith(".wav") -> "WAV"
                uriName.endsWith(".m4a") || uriName.endsWith(".aac") -> "AAC"
                uriName.endsWith(".ogg") -> "OGG"
                uriName.endsWith(".opus") -> "OPUS"
                uriName.endsWith(".mp3") -> "MP3"
                else -> ""
            }
        }
    }
    val queueTracks = remember(queue, libraryById, s.mediaId) {
        queue.map { queued ->
            val stored = libraryById[queued.id]
            QueueTrack(
                id = queued.id,
                title = queued.title,
                artist = queued.artist,
                duration = when {
                    stored != null && stored.durationMs > 0 -> formatTime(stored.durationMs)
                    queued.durationMs > 0 -> formatTime(queued.durationMs)
                    else -> "--:--"
                },
                isPlaying = queued.id == s.mediaId,
                album = stored?.album ?: queued.album,
                artworkUri = stored?.uri ?: queued.uri
            )
        }.ifEmpty {
            listOfNotNull(s.mediaId?.let { id ->
                QueueTrack(id = id, title = s.title, artist = s.artist, duration = if (s.durationMs > 0) formatTime(s.durationMs) else "--:--", isPlaying = true, album = s.album, artworkUri = s.artworkUri)
            })
        }
    }
    val progress = if (s.durationMs > 0) (position.toFloat() / s.durationMs.toFloat()).coerceIn(0f, 1f) else 0f
    val abStartFrac = s.abLoopStartMs?.takeIf { s.durationMs > 0L }?.let { (it.toFloat() / s.durationMs).coerceIn(0f, 1f) }
    val abEndFrac = s.abLoopEndMs?.takeIf { s.durationMs > 0L }?.let { (it.toFloat() / s.durationMs).coerceIn(0f, 1f) }

    AtmosphericQueuePlayerLayout(
        uiState = AtmosphericQueueUiState(
            title = s.title, artist = s.artist, artworkUri = s.artworkUri,
            elapsedTime = formatTime(position),
            totalDuration = if (s.durationMs > 0) formatTime(s.durationMs) else "--:--",
            progress = progress, isPlaying = s.isPlaying, isFavorite = favorite,
            shuffleEnabled = s.shuffle, repeatMode = s.repeat, queue = queueTracks,
            error = s.trackError, sleepActive = s.sleepRemainingMs > 0 || s.sleepEndOfTrack,
            audioEffectsActive = s.speed != 1f || s.pitchSemitones != 0f || s.abLoopStartMs != null || s.abLoopEndMs != null,
            codec = resolvedCodec, speed = s.speed, pitchSemitones = s.pitchSemitones,
            abStartFraction = abStartFrac, abEndFraction = abEndFrac,
            sleepRemainingMs = s.sleepRemainingMs, sleepEndOfTrack = s.sleepEndOfTrack
        ),
        showLyrics = showLyrics,
        onSelectTab = { showLyrics = it },
        lyricsContent = { LyricsView(vm = vm, mediaId = s.mediaId, onDarkSurface = true, modifier = Modifier.fillMaxSize()) },
        onPlayPauseClick = vm.player::togglePlay, onPreviousClick = vm.player::previous, onNextClick = vm.player::next,
        onSeek = { fraction -> if (s.durationMs > 0 && s.canSeek) vm.player.seekTo((fraction * s.durationMs).toLong()) },
        onTrackSelect = { track -> queue.indexOfFirst { it.id == track.id }.takeIf { it >= 0 }?.let(vm.player::playAt) },
        onToggleFavorite = vm::toggleFavoriteCurrent, onToggleShuffle = vm::toggleShuffle, onCycleRepeat = vm::cycleRepeat,
        onOpenTheme = vm::openPlayerLayoutPicker, onCollapse = onBack, onOpenMenu = vm::openTrackOptions,
        onTrackOptions = { track -> queue.firstOrNull { it.id == track.id }?.let(vm::openTrackMenu) },
        onOpenSleep = vm::openSleepSheet, onAddToPlaylist = { vm.openAddToPlaylist() }, onOpenQueue = vm::openQueue,
        onOpenEqualizer = { vm.go(Screen.Equalizer) }, onOpenAudioControls = vm::openAudioControls,
        onBrowseLibrary = { vm.selectHomeTab("Songs") }, onRetry = vm.player::retry, onSkip = vm.player::skip,
        upperRatio = splitUpperRatio, onSplitRatioChange = vm::setSplitPlayerUpperRatio,
        coverShape = coverShape, backdropBlur = backdropBlur, backdropDim = backdropDim,
        artworkGlow = artworkGlow, swipeSkip = swipeSkip, seekbarHeight = seekbarHeight, cornerRadiusDp = cornerRadiusDp
    )
}

@Composable
fun NowPlayingPanel(vm: MainViewModel, modifier: Modifier = Modifier) {
    val s by vm.player.state.collectAsStateWithLifecycle()
    val fav by vm.isCurrentFavorite.collectAsStateWithLifecycle()
    val artworkAccent by vm.artworkAccent.collectAsStateWithLifecycle()
    val seekStyle by vm.seekStyle.collectAsStateWithLifecycle()
    val audioEffectsActive = s.speed != 1f || s.pitchSemitones != 0f || s.abLoopStartMs != null || s.abLoopEndMs != null
    val abStartFrac = s.abLoopStartMs?.takeIf { s.durationMs > 0L }?.let { (it.toFloat() / s.durationMs).coerceIn(0f, 1f) }
    val abEndFrac = s.abLoopEndMs?.takeIf { s.durationMs > 0L }?.let { (it.toFloat() / s.durationMs).coerceIn(0f, 1f) }

    Surface(modifier = modifier, color = MaterialTheme.colorScheme.surface, tonalElevation = 8.dp) {
        Column(
            Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Now Playing", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(24.dp))
            Box(Modifier.weight(1f), Alignment.Center) { PlayerThemeHost(vm, s, artworkAccent) }
            Spacer(Modifier.height(16.dp))
            Text(s.title, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(s.artist, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(16.dp))
            SeekBar(
                position = vm.player.position,
                duration = s.durationMs,
                isPlaying = s.isPlaying,
                trackKey = s.mediaId ?: "default",
                seekStyle = seekStyle,
                onSelectStyle = vm::setSeekStyle,
                abStartFraction = abStartFrac,
                abEndFraction = abEndFrac,
                onSeek = vm.player::seekTo
            )
            Spacer(Modifier.height(8.dp))
            TransportRow(s, vm.player)
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                HeartButton(fav, { vm.toggleFavoriteCurrent() })
                IconButton(onClick = { vm.openQueue() }, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.AutoMirrored.Rounded.QueueMusic, "Queue", tint = MaterialTheme.colorScheme.onSurface)
                }
                IconButton(onClick = { vm.openAudioControls() }, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Rounded.Speed, "Audio & Loop Controls", tint = if (audioEffectsActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                }
                IconButton(onClick = { vm.go(Screen.Equalizer) }, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Rounded.Equalizer, "Equalizer", tint = MaterialTheme.colorScheme.onSurface)
                }
            }
        }
    }
}
