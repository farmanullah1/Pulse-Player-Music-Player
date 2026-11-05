package com.pulse.player.ui.screens

import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import com.pulse.player.ui.components.PlaybackErrorBanner
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.DashboardCustomize
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.Player
import com.pulse.player.data.lyrics.Lrc
import com.pulse.player.data.lyrics.LrcLine
import com.pulse.player.data.scan.ArtworkLoader
import com.pulse.player.playback.PlaybackUiState
import com.pulse.player.playback.PlayerController
import com.pulse.player.playback.RingtoneHelper
import com.pulse.player.playback.Track
import com.pulse.player.ui.components.Artwork
import com.pulse.player.ui.components.AudioSignalPathBadge
import com.pulse.player.ui.components.AuroraBackground
import com.pulse.player.ui.components.GlowArtwork
import com.pulse.player.ui.components.HeartButton
import com.pulse.player.ui.components.WaveformSeekBar
import com.pulse.player.ui.components.WavyMusicSlider
import com.pulse.player.ui.components.formatTime
import com.pulse.player.ui.theme.Pulse
import com.pulse.player.ui.screens.layouts.AtmosphericQueuePlayerLayout
import com.pulse.player.ui.screens.layouts.AtmosphericQueueUiState
import com.pulse.player.ui.screens.layouts.Track as QueueTrack
import com.pulse.player.ui.themes.AcousticDisc
import com.pulse.player.ui.themes.MountainSunset
import com.pulse.player.ui.themes.PlayerLayout
import com.pulse.player.ui.themes.PlayerThemes
import com.pulse.player.ui.themes.StarField
import com.pulse.player.ui.themes.VinylDisc
import com.pulse.player.ui.themes.WavesCanvas
import com.pulse.player.ui.viewmodel.MainViewModel
import java.io.File
import kotlinx.coroutines.flow.StateFlow

@Composable
fun NowPlayingScreen(vm: MainViewModel) {
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
        com.pulse.player.ui.theme.immersive.ImmersivePlayerLayout(
            s = s,
            vm = vm,
            onBack = vm::closeNowPlaying
        )
        return
    }

    if (theme.layout == PlayerLayout.PIXEL) {
        com.pulse.player.ui.theme.pixel.PixelPlayerLayout(
            s = s,
            vm = vm,
            onBack = vm::closeNowPlaying
        )
        return
    }

    if (theme.layout == PlayerLayout.NAMIDA) {
        com.pulse.player.ui.theme.namida.NamidaPlayerLayout(
            s = s,
            vm = vm,
            onBack = vm::closeNowPlaying
        )
        return
    }

    val ctx = LocalContext.current
    val fav by vm.isCurrentFavorite.collectAsStateWithLifecycle()
    var dominantColor by remember { mutableIntStateOf(0xFF2A0826.toInt()) }
    var showLyrics by remember { mutableStateOf(false) }

    LaunchedEffect(s.mediaId) {
        s.artworkUri?.let { u ->
            ArtworkLoader.load(ctx, u, 128)
        }
    }

    AuroraBackground(
        accent = Color(dominantColor),
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .pointerInput(Unit) {
                        detectVerticalDragGestures(
                            onDragEnd = {},
                            onVerticalDrag = { _, dragAmount ->
                                if (dragAmount > 25f) {
                                    vm.closeNowPlaying()
                                }
                            }
                        )
                    },
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = vm::closeNowPlaying) {
                    Icon(
                        imageVector = Icons.Rounded.KeyboardArrowDown,
                        contentDescription = "Collapse player",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(32.dp)
                    )
                }
                Spacer(Modifier.weight(1f))
                SongLyricsSegment(showLyrics) { showLyrics = it }
                Spacer(Modifier.weight(1f))
                IconButton(
                    onClick = {
                        vm.closeNowPlaying()
                        vm.go(Screen.Customize)
                    }
                ) {
                    Icon(
                        imageVector = Icons.Rounded.DashboardCustomize,
                        contentDescription = "Customize Player",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                PlayerTopMenu(vm)
            }

            Spacer(Modifier.height(16.dp))

            if (s.trackError != null) {
                PlaybackErrorBanner(
                    error = s.trackError!!,
                    onRetry = vm.player::retry,
                    onSkip = vm.player::skip,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            if (showLyrics) {
                LyricsView(
                    vm = vm,
                    mediaId = s.mediaId,
                    modifier = Modifier.weight(1f)
                )
            } else {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .pointerInput(s.mediaId) {
                            detectTapGestures(
                                onDoubleTap = {
                                    vm.toggleFavoriteCurrent()
                                }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    PlayerThemeHost(vm, s, Color(dominantColor))
                }
            }

            Spacer(Modifier.height(16.dp))

            Text(
                text = s.title.ifBlank { "No track selected" },
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = s.artist.ifBlank { "Unknown artist" },
                style = MaterialTheme.typography.bodyMedium,
                color = Pulse.TextMid,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(8.dp))

            AudioSignalPathBadge(
                track = Track(
                    id = s.mediaId ?: "current",
                    title = s.title,
                    artist = s.artist,
                    uri = s.artworkUri ?: ""
                ),
                eqActive = true
            )

            Spacer(Modifier.height(14.dp))

            SeekBar(
                position = vm.player.position,
                duration = s.durationMs,
                isPlaying = s.isPlaying,
                trackKey = s.mediaId ?: "default",
                onSeek = vm.player::seekTo
            )

            Spacer(Modifier.height(16.dp))

            TransportRow(s = s, p = vm.player)

            Spacer(Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                HeartButton(
                    on = fav,
                    onToggle = { vm.toggleFavoriteCurrent() }
                )
                IconButton(onClick = { vm.openSleepSheet() }) {
                    Icon(Icons.Rounded.Bedtime, "Sleep timer", tint = MaterialTheme.colorScheme.onSurface)
                }
                IconButton(onClick = { vm.openAddToPlaylist() }) {
                    Icon(Icons.Rounded.PlaylistAdd, "Add to playlist", tint = MaterialTheme.colorScheme.onSurface)
                }
                IconButton(onClick = { vm.openQueue() }) {
                    Icon(Icons.Rounded.QueueMusic, "Queue", tint = MaterialTheme.colorScheme.onSurface)
                }
                IconButton(onClick = { vm.go(Screen.Equalizer) }) {
                    Icon(Icons.Rounded.Equalizer, "Equalizer", tint = MaterialTheme.colorScheme.onSurface)
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun AtmosphericQueuePlayer(
    vm: MainViewModel,
    s: PlaybackUiState,
    onBack: () -> Unit
) {
    val position by vm.player.position.collectAsStateWithLifecycle()
    val favorite by vm.isCurrentFavorite.collectAsStateWithLifecycle()
    val queue by vm.player.queue.collectAsStateWithLifecycle()
    val library by vm.tracks.collectAsStateWithLifecycle()

    val libraryById = remember(library) { library.associateBy { it.id } }
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
                isPlaying = queued.id == s.mediaId
            )
        }.ifEmpty {
            listOfNotNull(s.mediaId?.let { id ->
                QueueTrack(
                    id = id,
                    title = s.title,
                    artist = s.artist,
                    duration = if (s.durationMs > 0) formatTime(s.durationMs) else "--:--",
                    isPlaying = true
                )
            })
        }
    }
    val progress = if (s.durationMs > 0) {
        (position.toFloat() / s.durationMs.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }

    AtmosphericQueuePlayerLayout(
        uiState = AtmosphericQueueUiState(
            title = s.title,
            artist = s.artist,
            elapsedTime = formatTime(position),
            totalDuration = if (s.durationMs > 0) formatTime(s.durationMs) else "--:--",
            progress = progress,
            isPlaying = s.isPlaying,
            isFavorite = favorite,
            shuffleEnabled = s.shuffle,
            repeatMode = s.repeat,
            queue = queueTracks,
            error = s.trackError
        ),
        onPlayPauseClick = vm.player::togglePlay,
        onPreviousClick = vm.player::previous,
        onNextClick = vm.player::next,
        onSeek = { fraction ->
            if (s.durationMs > 0 && s.canSeek) vm.player.seekTo((fraction * s.durationMs).toLong())
        },
        onTrackSelect = { track ->
            queue.indexOfFirst { it.id == track.id }
                .takeIf { it >= 0 }
                ?.let(vm.player::playAt)
        },
        onToggleFavorite = vm::toggleFavoriteCurrent,
        onToggleShuffle = vm::toggleShuffle,
        onCycleRepeat = vm::cycleRepeat,
        onOpenTheme = { vm.go(Screen.SkinTheme) },
        onOpenPicker = {
            vm.closeNowPlaying()
            vm.go(Screen.Customize)
        },
        onCollapse = onBack,
        onOpenMenu = vm::openTrackOptions,
        onTrackOptions = { track ->
            queue.firstOrNull { it.id == track.id }?.let(vm::openTrackMenu)
        },
        onOpenSleep = vm::openSleepSheet,
        onAddToPlaylist = { vm.openAddToPlaylist() },
        onOpenQueue = vm::openQueue,
        onOpenEqualizer = { vm.go(Screen.Equalizer) },
        onRetry = vm.player::retry,
        onSkip = vm.player::skip
    )
}

@Composable
fun NowPlayingPanel(vm: MainViewModel, modifier: Modifier = Modifier) {
    val s by vm.player.state.collectAsStateWithLifecycle()
    val fav by vm.isCurrentFavorite.collectAsStateWithLifecycle()

    Surface(
        modifier = modifier,
        color = Pulse.Surface,
        tonalElevation = 8.dp
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Now Playing", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(24.dp))
            Box(Modifier.weight(1f), Alignment.Center) {
                PlayerThemeHost(vm, s, Pulse.Cyan)
            }
            Spacer(Modifier.height(16.dp))
            Text(
                text = s.title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(text = s.artist, style = MaterialTheme.typography.bodySmall, color = Pulse.TextMid)
            Spacer(Modifier.height(16.dp))
            SeekBar(
                position = vm.player.position,
                duration = s.durationMs,
                isPlaying = s.isPlaying,
                trackKey = s.mediaId ?: "default",
                onSeek = vm.player::seekTo
            )
            Spacer(Modifier.height(8.dp))
            TransportRow(s, vm.player)
            Spacer(Modifier.height(8.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                HeartButton(fav, { vm.toggleFavoriteCurrent() })
                IconButton(onClick = { vm.openQueue() }) {
                    Icon(Icons.Rounded.QueueMusic, "Queue", tint = MaterialTheme.colorScheme.onSurface)
                }
                IconButton(onClick = { vm.go(Screen.Equalizer) }) {
                    Icon(Icons.Rounded.Equalizer, "Equalizer", tint = MaterialTheme.colorScheme.onSurface)
                }
            }
        }
    }
}

@Composable
fun PlayerThemeHost(vm: MainViewModel, s: PlaybackUiState, glowColor: Color = Pulse.Cyan) {
    val id by vm.playerThemeId.collectAsStateWithLifecycle()
    val theme = PlayerThemes.firstOrNull { it.id == id } ?: PlayerThemes[0]
    val haptic = LocalHapticFeedback.current
    var dragAccumulated by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = Modifier.pointerInput(s.currentMediaId) {
            detectHorizontalDragGestures(
                onDragStart = { dragAccumulated = 0f },
                onDragEnd = {
                    if (dragAccumulated < -80f) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        vm.playerController.next()
                    } else if (dragAccumulated > 80f) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        vm.playerController.previous()
                    }
                    dragAccumulated = 0f
                },
                onDragCancel = { dragAccumulated = 0f },
                onHorizontalDrag = { _, dragAmount ->
                    dragAccumulated += dragAmount
                }
            )
        },
        contentAlignment = Alignment.Center
    ) {
        Crossfade(theme, animationSpec = tween(400), label = "ptheme") { t ->
            when (t.layout) {
                PlayerLayout.VINYL -> VinylDisc(s)
                PlayerLayout.ACOUSTIC -> AcousticDisc(s)
                PlayerLayout.COAST -> Box(Modifier.size(280.dp), Alignment.Center) {
                    WavesCanvas(Modifier.fillMaxSize())
                    Artwork(s.artworkUri, 220.dp, radius = 16.dp, px = 512)
                }
                PlayerLayout.STARRY -> Box(Modifier.size(280.dp), Alignment.Center) {
                    StarField(Modifier.fillMaxSize())
                    Artwork(s.artworkUri, 220.dp, radius = 16.dp, px = 512)
                }
                PlayerLayout.SHEET_QUEUE -> Box(Modifier.size(280.dp), Alignment.Center) {
                    MountainSunset(Modifier.fillMaxSize())
                    Artwork(s.artworkUri, 220.dp, radius = 16.dp, px = 512)
                }
                else -> GlowArtwork(
                    uri = s.artworkUri,
                    size = 280.dp,
                    glow = glowColor
                )
            }
        }
    }
}

@Composable
fun SongLyricsSegment(showLyrics: Boolean, onSelect: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Pulse.Elevated)
            .padding(4.dp)
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(if (!showLyrics) Color.White else Color.Transparent)
                .clickable { onSelect(false) }
                .padding(horizontal = 14.dp, vertical = 6.dp)
        ) {
            Text(
                "Song",
                fontWeight = FontWeight.Bold,
                color = if (!showLyrics) Color.Black else Pulse.TextMid,
                style = MaterialTheme.typography.bodySmall
            )
        }
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(if (showLyrics) Color.White else Color.Transparent)
                .clickable { onSelect(true) }
                .padding(horizontal = 14.dp, vertical = 6.dp)
        ) {
            Text(
                "Lyrics",
                fontWeight = FontWeight.Bold,
                color = if (showLyrics) Color.Black else Pulse.TextMid,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
fun LyricsView(
    vm: MainViewModel,
    mediaId: String?,
    modifier: Modifier = Modifier
) {
    val ctx = LocalContext.current
    var lrcLines by remember { mutableStateOf<List<LrcLine>>(emptyList()) }
    val listState = rememberLazyListState()
    val currentPosMs by vm.player.position.collectAsStateWithLifecycle()

    fun loadStoredLyrics(id: String?) {
        if (id.isNullOrBlank()) {
            lrcLines = emptyList()
            return
        }
        val safeId = id.replace(Regex("[^a-zA-Z0-9._-]"), "_")
        val lrcFile = File(File(ctx.filesDir, "lyrics"), "$safeId.lrc")
        if (lrcFile.exists() && lrcFile.length() > 0) {
            runCatching {
                val text = lrcFile.readText()
                val parsed = Lrc.parse(text)
                lrcLines = if (parsed.isNotEmpty()) {
                    parsed
                } else {
                    text.lineSequence()
                        .map { it.trim() }
                        .filter { it.isNotBlank() }
                        .map { LrcLine(-1L, it) }
                        .toList()
                }
            }
        } else {
            lrcLines = emptyList()
        }
    }

    LaunchedEffect(mediaId) {
        loadStoredLyrics(mediaId)
    }

    val lrcPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            runCatching {
                ctx.contentResolver.openInputStream(uri)?.use { stream ->
                    val text = stream.bufferedReader().use { it.readText().take(512 * 1024) }
                    val dir = File(ctx.filesDir, "lyrics").apply { mkdirs() }
                    val targetId = mediaId ?: return@runCatching
                    val safeId = targetId.replace(Regex("[^a-zA-Z0-9._-]"), "_")
                    File(dir, "$safeId.lrc").writeText(text)
                    val parsed = Lrc.parse(text)
                    lrcLines = if (parsed.isNotEmpty()) {
                        parsed
                    } else {
                        text.lineSequence()
                            .map { it.trim() }
                            .filter { it.isNotBlank() }
                            .map { LrcLine(-1L, it) }
                            .toList()
                    }
                }
            }
        }
    }

    var lrcOffsetMs by remember(mediaId) { mutableStateOf(0L) }

    val activeIdx = remember(lrcLines, currentPosMs, lrcOffsetMs) {
        val effectivePos = (currentPosMs + lrcOffsetMs).coerceAtLeast(0L)
        if (lrcLines.isNotEmpty() && lrcLines.first().timeMs >= 0) {
            Lrc.activeIndex(lrcLines, effectivePos)
        } else {
            -1
        }
    }

    LaunchedEffect(activeIdx) {
        if (activeIdx >= 0 && !listState.isScrollInProgress) {
            listState.animateScrollToItem(activeIdx)
        }
    }

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        if (lrcLines.isEmpty()) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("No lyrics found for this track", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { lrcPicker.launch(arrayOf("*/*", "text/*")) },
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text("Choose .lrc")
                    }
                    Button(
                        onClick = {
                            vm.closeNowPlaying()
                            vm.go(Screen.Transcribe)
                        },
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Icon(Icons.Rounded.AutoAwesome, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Transcribe (AI)", fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            Column(Modifier.fillMaxSize()) {
                // Inline Sync Offset Nudger
                if (lrcLines.any { it.timeMs >= 0 }) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Sync Offset: ${if (lrcOffsetMs >= 0) "+" else ""}${lrcOffsetMs / 1000f}s",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.width(8.dp))
                        Button(
                            onClick = { lrcOffsetMs -= 500L },
                            shape = CircleShape,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        ) {
                            Text("-0.5s", style = MaterialTheme.typography.labelSmall)
                        }
                        Spacer(Modifier.width(4.dp))
                        Button(
                            onClick = { lrcOffsetMs = 0L },
                            shape = CircleShape,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        ) {
                            Text("Reset", style = MaterialTheme.typography.labelSmall)
                        }
                        Spacer(Modifier.width(4.dp))
                        Button(
                            onClick = { lrcOffsetMs += 500L },
                            shape = CircleShape,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        ) {
                            Text("+0.5s", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }

                LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    itemsIndexed(lrcLines) { idx, line ->
                        val isActive = idx == activeIdx
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                    else Color.Transparent
                                )
                                .clickable(enabled = line.timeMs >= 0) {
                                    val target = (line.timeMs - lrcOffsetMs).coerceAtLeast(0L)
                                    vm.player.seekTo(target)
                                }
                                .padding(vertical = 10.dp, horizontal = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = line.text,
                                style = if (isActive) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
                                color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                fontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.Normal,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PlayerTopMenu(vm: MainViewModel) {
    var expanded by remember { mutableStateOf(false) }
    val ctx = LocalContext.current
    val s by vm.player.state.collectAsStateWithLifecycle()

    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(Icons.Rounded.MoreVert, "More actions", tint = MaterialTheme.colorScheme.onSurface)
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(Pulse.Elevated)
        ) {
            DropdownMenuItem(
                text = { Text("Edit Track Tags & Art") },
                onClick = {
                    expanded = false
                    vm.openTagEditor()
                }
            )
            DropdownMenuItem(
                text = { Text("Customize Player") },
                onClick = {
                    expanded = false
                    vm.closeNowPlaying()
                    vm.go(Screen.Customize)
                }
            )
            DropdownMenuItem(
                text = { Text("Player Theme") },
                onClick = {
                    expanded = false
                    vm.go(Screen.PlayerTheme)
                }
            )
            DropdownMenuItem(
                text = { Text("Equalizer") },
                onClick = {
                    expanded = false
                    vm.go(Screen.Equalizer)
                }
            )
            DropdownMenuItem(
                text = { Text("Set as Ringtone") },
                onClick = {
                    expanded = false
                    s.artworkUri?.let {
                        RingtoneHelper.setRingtone(ctx, Uri.parse(it), RingtoneManager.TYPE_RINGTONE)
                    }
                }
            )
            DropdownMenuItem(
                text = { Text("Share Track") },
                onClick = {
                    expanded = false
                    s.artworkUri?.let {
                        val share = Intent(Intent.ACTION_SEND).apply {
                            type = "audio/*"
                            putExtra(Intent.EXTRA_STREAM, Uri.parse(it))
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        ctx.startActivity(Intent.createChooser(share, "Share ${s.title}"))
                    }
                }
            )
        }
    }
}

enum class SeekStyle { WAVEFORM, WAVY, LINEAR }

@Composable
fun SeekBar(
    position: StateFlow<Long>,
    duration: Long,
    isPlaying: Boolean = true,
    trackKey: String = "default",
    onSeek: (Long) -> Unit
) {
    val pos by position.collectAsStateWithLifecycle()
    var drag by remember { mutableStateOf<Float?>(null) }
    var currentStyle by remember { mutableStateOf(SeekStyle.WAVEFORM) }
    val rawProgress = if (duration > 0) pos / duration.toFloat() else 0f
    val safeProgress = if (rawProgress.isNaN() || rawProgress.isInfinite()) 0f else rawProgress.coerceIn(0f, 1f)
    val value = drag ?: safeProgress

    Column(Modifier.fillMaxWidth()) {
        when (currentStyle) {
            SeekStyle.WAVEFORM -> {
                WaveformSeekBar(
                    value = value,
                    onValueChange = { drag = it },
                    onValueChangeFinished = {
                        drag?.let { onSeek((it * duration).toLong()) }
                        drag = null
                    },
                    isPlaying = isPlaying,
                    seedKey = trackKey,
                    modifier = Modifier.fillMaxWidth().height(44.dp)
                )
            }
            SeekStyle.WAVY -> {
                WavyMusicSlider(
                    value = value,
                    onValueChange = { drag = it },
                    onValueChangeFinished = {
                        drag?.let { onSeek((it * duration).toLong()) }
                        drag = null
                    },
                    isPlaying = isPlaying,
                    modifier = Modifier.fillMaxWidth().height(44.dp)
                )
            }
            SeekStyle.LINEAR -> {
                Slider(
                    value = value,
                    onValueChange = { drag = it },
                    onValueChangeFinished = {
                        drag?.let { onSeek((it * duration).toLong()) }
                        drag = null
                    },
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    ),
                    modifier = Modifier.fillMaxWidth().height(44.dp)
                )
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                formatTime(((drag ?: value) * duration).toLong()),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(Modifier.weight(1f))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable {
                        currentStyle = when (currentStyle) {
                            SeekStyle.WAVEFORM -> SeekStyle.WAVY
                            SeekStyle.WAVY -> SeekStyle.LINEAR
                            SeekStyle.LINEAR -> SeekStyle.WAVEFORM
                        }
                    }
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(
                    text = when (currentStyle) {
                        SeekStyle.WAVEFORM -> "Waveform"
                        SeekStyle.WAVY -> "Wavy"
                        SeekStyle.LINEAR -> "Classic"
                    },
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(Modifier.weight(1f))
            Text(
                formatTime(duration),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
fun TransportRow(s: PlaybackUiState, p: PlayerController) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = p::toggleShuffle) {
            Icon(
                imageVector = Icons.Rounded.Shuffle,
                contentDescription = "Shuffle ${if (s.shuffle) "on" else "off"}",
                tint = if (s.shuffle) Pulse.Cyan else Color.White
            )
        }
        IconButton(onClick = p::previous, modifier = Modifier.size(56.dp)) {
            Icon(
                imageVector = Icons.Rounded.SkipPrevious,
                contentDescription = "Previous",
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(36.dp)
            )
        }
        FilledIconButton(
            onClick = p::togglePlay,
            modifier = Modifier.size(76.dp),
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = Color.White,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            AnimatedContent(
                targetState = s.isPlaying,
                transitionSpec = { (scaleIn() + fadeIn()) togetherWith (scaleOut() + fadeOut()) },
                label = "main_play"
            ) { playing ->
                Icon(
                    imageVector = if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = if (playing) "Pause" else "Play",
                    modifier = Modifier.size(40.dp)
                )
            }
        }
        IconButton(onClick = p::next, modifier = Modifier.size(56.dp)) {
            Icon(
                imageVector = Icons.Rounded.SkipNext,
                contentDescription = "Next",
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(36.dp)
            )
        }
        IconButton(onClick = p::cycleRepeat) {
            Icon(
                imageVector = if (s.repeat == Player.REPEAT_MODE_ONE) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
                contentDescription = "Repeat mode",
                tint = if (s.repeat != Player.REPEAT_MODE_OFF) Pulse.Cyan else Color.White
            )
        }
    }
}
