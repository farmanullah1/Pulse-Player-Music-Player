package com.pulse.player.ui.screens.layouts

import android.graphics.Bitmap
import android.os.Build
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.automirrored.rounded.PlaylistPlay
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Checkroom
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import com.pulse.player.data.scan.ArtworkLoader
import com.pulse.player.playback.ErrorPolicy
import com.pulse.player.ui.components.Artwork
import com.pulse.player.ui.components.AtmosphericBackground
import com.pulse.player.ui.components.PlaybackErrorBanner
import com.pulse.player.ui.components.formatTime
import com.pulse.player.ui.theme.LocalReducedMotion
import com.pulse.player.ui.theme.TabularMono
import kotlin.math.abs
import kotlin.math.roundToInt

internal const val DEFAULT_UPPER_SPLIT_RATIO = 0.40f
internal const val MIN_UPPER_SPLIT_RATIO = 0.35f
internal const val MAX_UPPER_SPLIT_RATIO = 0.72f

internal fun clampSplitUpperRatio(ratio: Float): Float =
    ratio.coerceIn(MIN_UPPER_SPLIT_RATIO, MAX_UPPER_SPLIT_RATIO)

internal fun snapSplitUpperRatio(ratio: Float): Pair<Float, Boolean> {
    val detents = listOf(0.40f, 0.50f, 0.60f)
    val snapped = detents.firstOrNull { abs(ratio - it) < 0.022f }
    val finalRatio = clampSplitUpperRatio(snapped ?: ratio)
    return finalRatio to (snapped != null)
}

private val ActiveAmber = Color(0xFFE89B25)
private val FavoriteRose = Color(0xFFFF5E7E)

/** Queue item model used by the split-screen player. */
data class Track(
    val id: String,
    val title: String,
    val artist: String,
    val duration: String,
    val isPlaying: Boolean = false,
    val album: String = "",
    val artworkUri: String? = null
)

data class AtmosphericQueueUiState(
    val title: String,
    val artist: String,
    val artworkUri: String? = null,
    val elapsedTime: String,
    val totalDuration: String,
    val progress: Float,
    val isPlaying: Boolean,
    val isFavorite: Boolean,
    val shuffleEnabled: Boolean,
    val repeatMode: Int = Player.REPEAT_MODE_OFF,
    val queue: List<Track> = emptyList(),
    val error: ErrorPolicy.TrackError? = null,
    val sleepActive: Boolean = false,
    val audioEffectsActive: Boolean = false,
    val codec: String = "",
    val speed: Float = 1f,
    val pitchSemitones: Float = 0f,
    val abStartFraction: Float? = null,
    val abEndFraction: Float? = null,
    val sleepRemainingMs: Long = 0L,
    val sleepEndOfTrack: Boolean = false
)

/**
 * Customizable split-screen player (default 40% top controls / 60% bottom queue sheet):
 * dark dimmed blurred album-art backdrop on top paired with a draggable 24dp rounded-top
 * queue sheet adapting between #FFFFFF in light mode and dark charcoal in dark mode.
 */
@Composable
fun AtmosphericQueuePlayerLayout(
    uiState: AtmosphericQueueUiState,
    onPlayPauseClick: () -> Unit,
    onPreviousClick: () -> Unit,
    onNextClick: () -> Unit,
    onSeek: (Float) -> Unit,
    onTrackSelect: (Track) -> Unit,
    onToggleFavorite: () -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    onOpenTheme: () -> Unit,
    onCollapse: () -> Unit,
    modifier: Modifier = Modifier,
    onOpenMenu: () -> Unit = {},
    onTrackOptions: (Track) -> Unit = {},
    onOpenSleep: () -> Unit = {},
    onAddToPlaylist: () -> Unit = {},
    onOpenQueue: () -> Unit = {},
    onOpenEqualizer: () -> Unit = {},
    onOpenAudioControls: () -> Unit = {},
    onBrowseLibrary: () -> Unit = {},
    onRetry: () -> Unit = {},
    onSkip: () -> Unit = {},
    upperRatio: Float = DEFAULT_UPPER_SPLIT_RATIO,
    onSplitRatioChange: (Float) -> Unit = {},
    showLyrics: Boolean = false,
    onSelectTab: (Boolean) -> Unit = {},
    lyricsContent: @Composable () -> Unit = {},
    coverShape: String = "ROUNDED",
    backdropBlur: Float = 0.60f,
    backdropDim: Float = 0.55f,
    artworkGlow: Boolean = true,
    swipeSkip: Boolean = true,
    seekbarHeight: String = "MEDIUM",
    cornerRadiusDp: Int = 16
) {
    val reducedMotion = LocalReducedMotion.current
    val haptic = LocalHapticFeedback.current
    var liveUpperRatio by remember { mutableFloatStateOf(upperRatio.coerceIn(MIN_UPPER_SPLIT_RATIO, MAX_UPPER_SPLIT_RATIO)) }
    var isDraggingSplit by remember { mutableStateOf(false) }

    LaunchedEffect(upperRatio, isDraggingSplit) {
        if (!isDraggingSplit) {
            liveUpperRatio = upperRatio.coerceIn(MIN_UPPER_SPLIT_RATIO, MAX_UPPER_SPLIT_RATIO)
        }
    }

    val animatedUpperRatio by animateFloatAsState(
        targetValue = liveUpperRatio,
        animationSpec = if (isDraggingSplit || reducedMotion) snap() else spring(stiffness = Spring.StiffnessMediumLow),
        label = "split-screen-ratio"
    )
    val clampedUpperRatio = animatedUpperRatio.coerceIn(MIN_UPPER_SPLIT_RATIO, MAX_UPPER_SPLIT_RATIO)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color(0xFF0A0B0F),
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { padding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFF0A0B0F))
        ) {
            val totalHeightPx = with(LocalDensity.current) { maxHeight.toPx() }.coerceAtLeast(1f)

            // Extend dimmed album-art backdrop cleanly behind the 24dp rounded top corners at any split ratio.
            SplitPlayerBackdrop(
                artworkUri = uiState.artworkUri,
                backdropBlur = backdropBlur,
                backdropDim = backdropDim,
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight((clampedUpperRatio + 0.08f).coerceIn(0.46f, 1f))
            )

            Column(modifier = Modifier.fillMaxSize()) {
                UpperPlayerSection(
                    uiState = uiState,
                    showLyrics = showLyrics,
                    onSelectTab = onSelectTab,
                    lyricsContent = lyricsContent,
                    onPlayPauseClick = onPlayPauseClick,
                    onPreviousClick = onPreviousClick,
                    onNextClick = onNextClick,
                    onSeek = onSeek,
                    onToggleFavorite = onToggleFavorite,
                    onToggleShuffle = onToggleShuffle,
                    onCycleRepeat = onCycleRepeat,
                    onOpenTheme = onOpenTheme,
                    onOpenMenu = onOpenMenu,
                    onCollapse = onCollapse,
                    onOpenSleep = onOpenSleep,
                    onAddToPlaylist = onAddToPlaylist,
                    onOpenQueue = onOpenQueue,
                    onOpenEqualizer = onOpenEqualizer,
                    onOpenAudioControls = onOpenAudioControls,
                    onRetry = onRetry,
                    onSkip = onSkip,
                    coverShape = coverShape,
                    artworkGlow = artworkGlow,
                    swipeSkip = swipeSkip,
                    seekbarHeight = seekbarHeight,
                    cornerRadiusDp = cornerRadiusDp,
                    modifier = Modifier
                        .weight(clampedUpperRatio)
                        .fillMaxWidth()
                )

                QueueSection(
                    tracks = uiState.queue,
                    upperRatio = clampedUpperRatio,
                    isDraggingSplit = isDraggingSplit,
                    onTrackSelect = onTrackSelect,
                    onTrackOptions = onTrackOptions,
                    onOpenQueue = onOpenQueue,
                    onBrowseLibrary = onBrowseLibrary,
                    onSplitDragStart = { isDraggingSplit = true },
                    onSplitDragDelta = { dragAmountPx ->
                        val next = (liveUpperRatio + dragAmountPx / totalHeightPx)
                            .coerceIn(MIN_UPPER_SPLIT_RATIO, MAX_UPPER_SPLIT_RATIO)
                        liveUpperRatio = next
                    },
                    onSplitDragEnd = {
                        isDraggingSplit = false
                        val (finalRatio, didSnap) = snapSplitUpperRatio(liveUpperRatio)
                        if (didSnap) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                        liveUpperRatio = finalRatio
                        onSplitRatioChange(finalRatio)
                    },
                    onResetOrCycleRatio = { targetRatio ->
                        isDraggingSplit = false
                        val bounded = targetRatio.coerceIn(MIN_UPPER_SPLIT_RATIO, MAX_UPPER_SPLIT_RATIO)
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        liveUpperRatio = bounded
                        onSplitRatioChange(bounded)
                    },
                    modifier = Modifier
                        .weight((1f - clampedUpperRatio).coerceAtLeast(0.28f))
                        .fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun SplitPlayerBackdrop(
    artworkUri: String?,
    modifier: Modifier = Modifier,
    backdropBlur: Float = 0.60f,
    backdropDim: Float = 0.55f
) {
    val ctx = LocalContext.current
    val loadSize = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) 256 else 96
    val cached = remember(artworkUri, loadSize) { artworkUri?.let { ArtworkLoader.getCached(it, loadSize) } }
    val knownMissing = remember(artworkUri) { artworkUri == null || ArtworkLoader.hasNoArtwork(artworkUri) }
    val artworkBitmap by produceState<Bitmap?>(cached, artworkUri, loadSize) {
        value = if (cached != null) {
            cached
        } else if (!knownMissing && artworkUri != null) {
            ArtworkLoader.load(ctx, artworkUri, loadSize)
        } else {
            null
        }
    }
    val clampedBlur = backdropBlur.coerceIn(0f, 1f)
    val blurRadiusDp = (clampedBlur * 44f).dp
    val clampedDim = backdropDim.coerceIn(0.15f, 0.85f)

    Box(modifier = modifier) {
        AtmosphericBackground(modifier = Modifier.fillMaxSize()) {}

        Crossfade(
            targetState = artworkBitmap,
            animationSpec = tween(450),
            label = "split-artwork-backdrop"
        ) { bmp ->
            if (bmp != null) {
                Image(
                    bitmap = bmp.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    alpha = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) 1f else (1f - clampedBlur * 0.28f),
                    modifier = Modifier
                        .fillMaxSize()
                        .then(
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && blurRadiusDp > 0.5.dp) {
                                Modifier.blur(blurRadiusDp, BlurredEdgeTreatment.Rectangle)
                            } else {
                                Modifier
                            }
                        )
                )
            }
        }

        // Rich dark gradient scrim guarantees white icon & text legibility over any cover art.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Black.copy(alpha = (clampedDim * 0.92f).coerceIn(0.22f, 0.78f)),
                        0.45f to Color(0xFF0A0B0F).copy(alpha = (clampedDim + 0.08f).coerceIn(0.30f, 0.86f)),
                        1f to Color(0xFF07080B).copy(alpha = (clampedDim + 0.32f).coerceIn(0.68f, 0.96f))
                    )
                )
        )
    }
}

@Composable
private fun UpperPlayerSection(
    uiState: AtmosphericQueueUiState,
    showLyrics: Boolean,
    onSelectTab: (Boolean) -> Unit,
    lyricsContent: @Composable () -> Unit,
    onPlayPauseClick: () -> Unit,
    onPreviousClick: () -> Unit,
    onNextClick: () -> Unit,
    onSeek: (Float) -> Unit,
    onToggleFavorite: () -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    onOpenTheme: () -> Unit,
    onOpenMenu: () -> Unit,
    onCollapse: () -> Unit,
    onOpenSleep: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onOpenQueue: () -> Unit,
    onOpenEqualizer: () -> Unit,
    onOpenAudioControls: () -> Unit,
    onRetry: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier,
    coverShape: String = "ROUNDED",
    artworkGlow: Boolean = true,
    swipeSkip: Boolean = true,
    seekbarHeight: String = "MEDIUM",
    cornerRadiusDp: Int = 16
) {
    val reducedMotion = LocalReducedMotion.current
    val haptic = LocalHapticFeedback.current
    var verticalDragAccumulated by remember { mutableFloatStateOf(0f) }
    var collapseTriggered by remember { mutableStateOf(false) }

    BoxWithConstraints(
        modifier = modifier
            .statusBarsPadding()
            .padding(horizontal = 16.dp)
    ) {
        val upperMaxHeight = maxHeight
        val isCompactUpper = upperMaxHeight < 290.dp
        val isUltraCompactUpper = upperMaxHeight < 252.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = if (isCompactUpper) 0.dp else 2.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. Top Navigation Bar: [v] [Song | Lyrics] [Shirt] [More] with pull-down to collapse
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = if (isCompactUpper) 48.dp else 50.dp)
                    .pointerInput(Unit) {
                    detectVerticalDragGestures(
                        onDragStart = {
                            verticalDragAccumulated = 0f
                            collapseTriggered = false
                        },
                        onVerticalDrag = { _, dragAmount ->
                            if (!collapseTriggered) {
                                verticalDragAccumulated += dragAmount
                                if (verticalDragAccumulated > 48f) {
                                    collapseTriggered = true
                                    onCollapse()
                                }
                            }
                        },
                        onDragEnd = { verticalDragAccumulated = 0f },
                        onDragCancel = { verticalDragAccumulated = 0f }
                    )
                },
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onCollapse,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("minimize_player_button")
            ) {
                Icon(
                    imageVector = Icons.Rounded.KeyboardArrowDown,
                    contentDescription = "Minimize player",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }

            SongLyricsTabs(
                showLyrics = showLyrics,
                onSelectTab = onSelectTab,
                modifier = Modifier.weight(1f)
            )

            IconButton(
                onClick = onOpenTheme,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("player_theme_button")
            ) {
                Icon(
                    imageVector = Icons.Rounded.Checkroom,
                    contentDescription = "Player theme & layout",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
            IconButton(
                onClick = onOpenMenu,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("player_menu_button")
            ) {
                Icon(
                    imageVector = Icons.Rounded.MoreVert,
                    contentDescription = "More player options",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        if (showLyrics) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(vertical = 2.dp)
            ) {
                lyricsContent()
            }
        } else {
            var swipeAccumulated by remember { mutableFloatStateOf(0f) }
            var swipeHandled by remember { mutableStateOf(false) }
            var showInlineThumb by remember { mutableStateOf(true) }

            // Dynamic middle stage: expands hero artwork + pills when split is dragged down,
            // and compacts smoothly at the default 40% top height so controls never crowd.
            val stageGestureMod = if (swipeSkip) {
                Modifier.pointerInput(uiState.title, uiState.artist, swipeSkip) {
                    detectHorizontalDragGestures(
                        onDragStart = {
                            swipeAccumulated = 0f
                            swipeHandled = false
                        },
                        onHorizontalDrag = { change, dragAmount ->
                            if (!swipeHandled) {
                                swipeAccumulated += dragAmount
                                if (swipeAccumulated <= -84f) {
                                    swipeHandled = true
                                    change.consume()
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onNextClick()
                                } else if (swipeAccumulated >= 84f) {
                                    swipeHandled = true
                                    change.consume()
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onPreviousClick()
                                }
                            }
                        },
                        onDragEnd = { swipeAccumulated = 0f },
                        onDragCancel = { swipeAccumulated = 0f }
                    )
                }
            } else {
                Modifier
            }
            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .then(stageGestureMod),
                contentAlignment = Alignment.Center
            ) {
                val availableHeight = maxHeight
                val hasLargeStage = availableHeight >= 110.dp && uiState.error == null
                val hasPillRoom = availableHeight >= 34.dp
                showInlineThumb = !hasLargeStage
                val artSize = when {
                    availableHeight >= 175.dp -> 118.dp
                    availableHeight >= 135.dp -> 88.dp
                    else -> 68.dp
                }
                val stageRadius = when (coverShape) {
                    "CIRCLE", "VINYL_DISC" -> artSize / 2
                    "SQUARE" -> 4.dp
                    "POSTER" -> 6.dp
                    else -> cornerRadiusDp.coerceIn(8, 24).dp
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(if (availableHeight < 140.dp) 4.dp else 10.dp, Alignment.CenterVertically)
                ) {
                    if (uiState.error != null) {
                        PlaybackErrorBanner(
                            error = uiState.error,
                            onRetry = onRetry,
                            onSkip = onSkip
                        )
                    } else if (hasLargeStage) {
                        Box(
                            modifier = Modifier
                                .size(artSize)
                                .shadow(
                                    elevation = if (artworkGlow) 20.dp else 12.dp,
                                    shape = RoundedCornerShape(stageRadius),
                                    ambientColor = if (artworkGlow) ActiveAmber else Color.Black,
                                    spotColor = if (artworkGlow) ActiveAmber else Color.Black
                                )
                                .clip(RoundedCornerShape(stageRadius))
                                .border(
                                    width = if (artworkGlow) 1.5.dp else 1.dp,
                                    color = if (artworkGlow) ActiveAmber.copy(alpha = 0.48f) else Color.White.copy(alpha = 0.16f),
                                    shape = RoundedCornerShape(stageRadius)
                                )
                                .pointerInput(uiState.isFavorite) {
                                    detectTapGestures(
                                        onDoubleTap = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            onToggleFavorite()
                                        }
                                    )
                                }
                                .testTag("artwork_card")
                        ) {
                            Artwork(
                                uri = uiState.artworkUri,
                                size = artSize,
                                radius = stageRadius,
                                px = 320
                            )
                        }
                    }

                    if (hasPillRoom) {
                        AudioStatusPills(
                            uiState = uiState,
                            onOpenAudioControls = onOpenAudioControls,
                            onOpenSleep = onOpenSleep
                        )
                    }
                }
            }

            // 2. Track Title & Mode Row: [Shuffle]  (Mini Art at 40% split) + Title / Artist  [Repeat]
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onToggleShuffle,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("shuffle_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Shuffle,
                        contentDescription = if (uiState.shuffleEnabled) "Shuffle on" else "Shuffle off",
                        tint = if (uiState.shuffleEnabled) Color.White else Color.White.copy(alpha = 0.45f),
                        modifier = Modifier.size(22.dp)
                    )
                }

                if (showInlineThumb) {
                    val inlineThumbRadius = when (coverShape) {
                        "CIRCLE", "VINYL_DISC" -> 20.dp
                        "SQUARE" -> 3.dp
                        "POSTER" -> 5.dp
                        else -> (cornerRadiusDp / 2).coerceIn(6, 14).dp
                    }
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(inlineThumbRadius))
                            .border(
                                width = if (artworkGlow) 1.2.dp else 1.dp,
                                color = if (artworkGlow) ActiveAmber.copy(alpha = 0.45f) else Color.White.copy(alpha = 0.18f),
                                shape = RoundedCornerShape(inlineThumbRadius)
                            )
                            .pointerInput(uiState.isFavorite) {
                                detectTapGestures(
                                    onDoubleTap = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        onToggleFavorite()
                                    }
                                )
                            }
                            .testTag("artwork_card")
                    ) {
                        Artwork(
                            uri = uiState.artworkUri,
                            size = 40.dp,
                            radius = inlineThumbRadius,
                            px = 128
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = if (showInlineThumb) 2.dp else 10.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = uiState.title.ifBlank { "No track selected" },
                        color = Color.White,
                        fontSize = if (showInlineThumb) 17.sp else 19.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(if (reducedMotion) Modifier else Modifier.basicMarquee())
                    )
                    Spacer(Modifier.height(1.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = uiState.artist.ifBlank { "Unknown artist" },
                            color = Color.White.copy(alpha = 0.74f),
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (showInlineThumb && uiState.codec.isNotBlank()) {
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.14f))
                                    .clickable(onClick = onOpenAudioControls)
                                    .padding(horizontal = 6.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = uiState.codec,
                                    color = ActiveAmber,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.labelSmall.merge(TabularMono)
                                )
                            }
                        }
                    }
                }

                IconButton(
                    onClick = onCycleRepeat,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("repeat_button")
                ) {
                    Icon(
                        imageVector = if (uiState.repeatMode == Player.REPEAT_MODE_ONE) {
                            Icons.Rounded.RepeatOne
                        } else {
                            Icons.Rounded.Repeat
                        },
                        contentDescription = when (uiState.repeatMode) {
                            Player.REPEAT_MODE_ONE -> "Repeat one active"
                            Player.REPEAT_MODE_ALL -> "Repeat all active"
                            else -> "Repeat off"
                        },
                        tint = if (uiState.repeatMode != Player.REPEAT_MODE_OFF) {
                            Color.White
                        } else {
                            Color.White.copy(alpha = 0.45f)
                        },
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            if (!isCompactUpper) {
                Spacer(Modifier.height(4.dp))
            }

            // 3. Full-width thin Audio Progress Scrubber with edge-safe thumb and timestamps below
            ThinWhiteScrubber(
                progress = uiState.progress,
                elapsedTime = uiState.elapsedTime,
                totalDuration = uiState.totalDuration,
                abStartFraction = uiState.abStartFraction,
                abEndFraction = uiState.abEndFraction,
                onSeek = onSeek,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                compact = isCompactUpper,
                seekbarHeight = seekbarHeight
            )

            if (!isCompactUpper) {
                Spacer(Modifier.height(2.dp))
            }
        }

        // 4. Primary Playback Controls: [|<]  ( Play/Pause )  [>|]
        PlaybackControls(
            isPlaying = uiState.isPlaying,
            playButtonSize = when {
                isUltraCompactUpper -> 48.dp
                isCompactUpper -> 52.dp
                else -> 58.dp
            },
            onPlayPauseClick = onPlayPauseClick,
            onPreviousClick = onPreviousClick,
            onNextClick = onNextClick
        )

        if (!isCompactUpper) {
            Spacer(Modifier.height(2.dp))
        }

        // 5. Secondary Utility Icon Bar: 5 evenly spaced white icons
        QuickActions(
            uiState = uiState,
            onToggleFavorite = onToggleFavorite,
            onOpenSleep = onOpenSleep,
            onAddToPlaylist = onAddToPlaylist,
            onOpenQueue = onOpenQueue,
            onOpenEqualizer = onOpenEqualizer
        )

        if (!isCompactUpper) {
            Spacer(Modifier.height(2.dp))
        }
        }
    }
}

@Composable
private fun AudioStatusPills(
    uiState: AtmosphericQueueUiState,
    onOpenAudioControls: () -> Unit,
    onOpenSleep: () -> Unit
) {
    val speedActive = uiState.speed != 1f ||
        uiState.pitchSemitones != 0f ||
        uiState.abStartFraction != null ||
        uiState.abEndFraction != null
    val speedLabel = buildList {
        if (uiState.speed != 1f) add("%.2fx".format(uiState.speed)) else add("1.0x")
        if (uiState.pitchSemitones != 0f) add("%+.0fst".format(uiState.pitchSemitones))
        if (uiState.abStartFraction != null || uiState.abEndFraction != null) add("A-B")
    }.joinToString(" · ")

    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (uiState.codec.isNotBlank()) {
            Surface(
                shape = CircleShape,
                color = Color.White.copy(alpha = 0.10f),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.14f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.GraphicEq,
                        contentDescription = null,
                        tint = ActiveAmber,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = uiState.codec,
                        color = Color.White.copy(alpha = 0.90f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.labelSmall.merge(TabularMono)
                    )
                }
            }
        }

        Surface(
            onClick = onOpenAudioControls,
            shape = CircleShape,
            color = if (speedActive) ActiveAmber.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.10f),
            border = BorderStroke(
                width = 1.dp,
                color = if (speedActive) ActiveAmber.copy(alpha = 0.55f) else Color.White.copy(alpha = 0.14f)
            ),
            modifier = Modifier.testTag("audio_controls_pill")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Speed,
                    contentDescription = "Playback speed and loop controls",
                    tint = if (speedActive) ActiveAmber else Color.White.copy(alpha = 0.88f),
                    modifier = Modifier.size(13.dp)
                )
                Text(
                    text = speedLabel,
                    color = if (speedActive) Color(0xFFFFD18A) else Color.White.copy(alpha = 0.90f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.labelSmall.merge(TabularMono)
                )
            }
        }

        if (uiState.sleepActive) {
            val sleepLabel = if (uiState.sleepEndOfTrack) {
                "End of track"
            } else {
                formatTime(uiState.sleepRemainingMs.coerceAtLeast(0L))
            }
            Surface(
                onClick = onOpenSleep,
                shape = CircleShape,
                color = ActiveAmber.copy(alpha = 0.22f),
                border = BorderStroke(1.dp, ActiveAmber.copy(alpha = 0.55f)),
                modifier = Modifier.testTag("sleep_timer_pill")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Bedtime,
                        contentDescription = "Sleep timer countdown",
                        tint = ActiveAmber,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = sleepLabel,
                        color = Color(0xFFFFD18A),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.labelSmall.merge(TabularMono)
                    )
                }
            }
        }
    }
}

@Composable
private fun SongLyricsTabs(
    showLyrics: Boolean,
    onSelectTab: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .heightIn(min = 48.dp)
                .clip(RoundedCornerShape(12.dp))
                .clickable(role = Role.Tab) { onSelectTab(false) }
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .testTag("song_tab"),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Song",
                color = if (!showLyrics) Color.White else Color.White.copy(alpha = 0.55f),
                fontSize = 16.sp,
                fontWeight = if (!showLyrics) FontWeight.Bold else FontWeight.Medium
            )
        }
        Text(
            text = "|",
            color = Color.White.copy(alpha = 0.36f),
            fontSize = 15.sp,
            modifier = Modifier.padding(horizontal = 2.dp)
        )
        Box(
            modifier = Modifier
                .heightIn(min = 48.dp)
                .clip(RoundedCornerShape(12.dp))
                .clickable(role = Role.Tab) { onSelectTab(true) }
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .testTag("lyrics_tab"),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Lyrics",
                color = if (showLyrics) Color.White else Color.White.copy(alpha = 0.55f),
                fontSize = 16.sp,
                fontWeight = if (showLyrics) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}

private fun xToTrackFraction(x: Float, widthPx: Int, thumbRadiusPx: Float): Float {
    val start = thumbRadiusPx
    val end = (widthPx.toFloat() - thumbRadiusPx).coerceAtLeast(start + 1f)
    return ((x - start) / (end - start)).coerceIn(0f, 1f)
}

private fun parseDurationStringMs(text: String): Long {
    val parts = text.trim().split(':').mapNotNull { it.toLongOrNull() }
    return when (parts.size) {
        2 -> (parts[0] * 60L + parts[1]) * 1000L
        3 -> (parts[0] * 3600L + parts[1] * 60L + parts[2]) * 1000L
        else -> 0L
    }
}

@Composable
private fun ThinWhiteScrubber(
    progress: Float,
    elapsedTime: String,
    totalDuration: String,
    abStartFraction: Float?,
    abEndFraction: Float?,
    onSeek: (Float) -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    seekbarHeight: String = "MEDIUM"
) {
    var dragFraction by remember { mutableStateOf<Float?>(null) }
    var lastDetent by remember { mutableStateOf<String?>(null) }
    val haptics = LocalHapticFeedback.current
    val displayProgress = (dragFraction ?: progress).coerceIn(0f, 1f)
    val totalMs = remember(totalDuration) { parseDurationStringMs(totalDuration) }
    val currentMs = remember(elapsedTime) { parseDurationStringMs(elapsedTime) }

    fun updateDrag(frac: Float) {
        val clamped = frac.coerceIn(0f, 1f)
        dragFraction = clamped
        val detent = when {
            clamped <= 0.008f -> "start"
            clamped >= 0.992f -> "end"
            abStartFraction != null && abs(clamped - abStartFraction) < 0.015f -> "ab_a"
            abEndFraction != null && abs(clamped - abEndFraction) < 0.015f -> "ab_b"
            else -> null
        }
        if (detent != null && detent != lastDetent) {
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
        lastDetent = detent
    }

    val thumbRadiusDp = when (seekbarHeight) {
        "COMPACT" -> 5.5.dp
        "BOLD" -> 8.dp
        else -> 6.5.dp
    }
    val strokeDp = when (seekbarHeight) {
        "COMPACT" -> 2.dp
        "BOLD" -> 4.dp
        else -> 2.75.dp
    }
    val scrubberHeightDp = when (seekbarHeight) {
        "COMPACT" -> if (compact) 22.dp else 28.dp
        "BOLD" -> if (compact) 28.dp else 38.dp
        else -> if (compact) 24.dp else 32.dp
    }
    val thumbRadiusPx = with(LocalDensity.current) { thumbRadiusDp.toPx() }

    Column(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(scrubberHeightDp)
                .pointerInput(thumbRadiusPx) {
                    detectTapGestures { offset ->
                        if (size.width > 0) {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onSeek(xToTrackFraction(offset.x, size.width, thumbRadiusPx))
                        }
                    }
                }
                .pointerInput(thumbRadiusPx) {
                    detectHorizontalDragGestures(
                        onDragStart = { offset ->
                            if (size.width > 0) {
                                updateDrag(xToTrackFraction(offset.x, size.width, thumbRadiusPx))
                            }
                        },
                        onHorizontalDrag = { change, _ ->
                            change.consume()
                            if (size.width > 0) {
                                updateDrag(xToTrackFraction(change.position.x, size.width, thumbRadiusPx))
                            }
                        },
                        onDragEnd = {
                            dragFraction?.let(onSeek)
                            dragFraction = null
                            lastDetent = null
                        },
                        onDragCancel = {
                            dragFraction = null
                            lastDetent = null
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val trackY = size.height / 2f
                val strokePx = strokeDp.toPx()
                val thumbRadius = thumbRadiusDp.toPx()
                val trackStart = thumbRadius
                val trackEnd = (size.width - thumbRadius).coerceAtLeast(trackStart)
                val trackSpan = (trackEnd - trackStart).coerceAtLeast(0f)
                val activeX = trackStart + trackSpan * displayProgress

                // Unplayed translucent gray track kept inside thumb margins so 0%/100% never clip
                drawLine(
                    color = Color.White.copy(alpha = 0.25f),
                    start = Offset(trackStart, trackY),
                    end = Offset(trackEnd, trackY),
                    strokeWidth = strokePx,
                    cap = StrokeCap.Round
                )

                // A-B loop range highlight when configured
                if (abStartFraction != null && abEndFraction != null && abEndFraction > abStartFraction) {
                    val abStartX = trackStart + trackSpan * abStartFraction.coerceIn(0f, 1f)
                    val abEndX = trackStart + trackSpan * abEndFraction.coerceIn(0f, 1f)
                    drawLine(
                        color = ActiveAmber.copy(alpha = 0.55f),
                        start = Offset(abStartX, trackY),
                        end = Offset(abEndX, trackY),
                        strokeWidth = strokePx * 1.4f,
                        cap = StrokeCap.Round
                    )
                }

                // Solid white played track
                if (activeX > trackStart) {
                    drawLine(
                        color = Color.White,
                        start = Offset(trackStart, trackY),
                        end = Offset(activeX, trackY),
                        strokeWidth = strokePx,
                        cap = StrokeCap.Round
                    )
                }

                // A-B endpoint tick dots
                abStartFraction?.let { frac ->
                    val markerX = trackStart + trackSpan * frac.coerceIn(0f, 1f)
                    drawCircle(
                        color = ActiveAmber,
                        radius = 3.2.dp.toPx(),
                        center = Offset(markerX, trackY)
                    )
                }
                abEndFraction?.let { frac ->
                    val markerX = trackStart + trackSpan * frac.coerceIn(0f, 1f)
                    drawCircle(
                        color = ActiveAmber,
                        radius = 3.2.dp.toPx(),
                        center = Offset(markerX, trackY)
                    )
                }

                // Solid white circular thumb handle centered cleanly within canvas edges
                drawCircle(
                    color = if (dragFraction != null) ActiveAmber else Color.White,
                    radius = if (dragFraction != null) thumbRadius * 1.25f else thumbRadius,
                    center = Offset(activeX, trackY)
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val activeDrag = dragFraction
            val previewText = if (activeDrag != null && totalMs > 0L) {
                val targetMs = (activeDrag * totalMs).toLong()
                val deltaMs = targetMs - currentMs
                val sign = if (deltaMs >= 0L) "+" else "-"
                "${formatTime(targetMs)} ($sign${formatTime(abs(deltaMs))})"
            } else {
                elapsedTime
            }
            Text(
                text = previewText,
                color = if (activeDrag != null) Color(0xFFFFD18A) else Color.White.copy(alpha = 0.68f),
                fontSize = 12.sp,
                fontWeight = if (activeDrag != null) FontWeight.Bold else FontWeight.Normal,
                style = MaterialTheme.typography.labelSmall.merge(TabularMono)
            )
            Text(
                text = totalDuration,
                color = Color.White.copy(alpha = 0.68f),
                fontSize = 12.sp,
                style = MaterialTheme.typography.labelSmall.merge(TabularMono)
            )
        }
    }
}

@Composable
private fun PlaybackControls(
    isPlaying: Boolean,
    playButtonSize: androidx.compose.ui.unit.Dp = 58.dp,
    onPlayPauseClick: () -> Unit,
    onPreviousClick: () -> Unit,
    onNextClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onPreviousClick,
            modifier = Modifier
                .size(52.dp)
                .testTag("previous_button")
        ) {
            Icon(
                imageVector = Icons.Rounded.SkipPrevious,
                contentDescription = "Previous track",
                tint = Color.White,
                modifier = Modifier.size(32.dp)
            )
        }

        FilledIconButton(
            onClick = onPlayPauseClick,
            modifier = Modifier
                .size(playButtonSize)
                .testTag("play_pause_button"),
            shape = CircleShape,
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = Color.White.copy(alpha = 0.16f),
                contentColor = Color.White
            )
        ) {
            AnimatedContent(
                targetState = isPlaying,
                transitionSpec = {
                    (fadeIn(tween(150)) + scaleIn(initialScale = 0.85f, animationSpec = tween(150)))
                        .togetherWith(fadeOut(tween(120)) + scaleOut(targetScale = 0.85f, animationSpec = tween(120)))
                },
                label = "play-pause-morph"
            ) { playing ->
                Icon(
                    imageVector = if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = if (playing) "Pause" else "Play",
                    modifier = Modifier.size(32.dp)
                )
            }
        }

        IconButton(
            onClick = onNextClick,
            modifier = Modifier
                .size(56.dp)
                .testTag("next_button")
        ) {
            Icon(
                imageVector = Icons.Rounded.SkipNext,
                contentDescription = "Next track",
                tint = Color.White,
                modifier = Modifier.size(34.dp)
            )
        }
    }
}

@Composable
private fun QuickActions(
    uiState: AtmosphericQueueUiState,
    onToggleFavorite: () -> Unit,
    onOpenSleep: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onOpenQueue: () -> Unit,
    onOpenEqualizer: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 1st: Outlined white Heart when off, filled rose-pink Heart when favorited
        IconButton(
            onClick = onToggleFavorite,
            modifier = Modifier
                .size(48.dp)
                .testTag("favorite_button")
        ) {
            Icon(
                imageVector = if (uiState.isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                contentDescription = if (uiState.isFavorite) "Remove from favorites" else "Add to favorites",
                tint = if (uiState.isFavorite) FavoriteRose else Color.White,
                modifier = Modifier.size(23.dp)
            )
        }
        // 2nd: Alarm clock outline icon (Sleep Timer)
        IconButton(
            onClick = onOpenSleep,
            modifier = Modifier
                .size(48.dp)
                .testTag("sleep_button")
        ) {
            Icon(
                imageVector = Icons.Outlined.Alarm,
                contentDescription = "Sleep timer",
                tint = if (uiState.sleepActive) ActiveAmber else Color.White,
                modifier = Modifier.size(23.dp)
            )
        }
        // 3rd: Horizontal lines with a plus sign (Add to Playlist)
        IconButton(
            onClick = onAddToPlaylist,
            modifier = Modifier
                .size(48.dp)
                .testTag("add_to_playlist_button")
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.PlaylistAdd,
                contentDescription = "Add to playlist",
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
        // 4th: Horizontal lines with a small play icon (Queue List)
        IconButton(
            onClick = onOpenQueue,
            modifier = Modifier
                .size(48.dp)
                .testTag("queue_button")
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.PlaylistPlay,
                contentDescription = "Queue list",
                tint = Color.White,
                modifier = Modifier.size(25.dp)
            )
        }
        // 5th: Two vertical audio slider controls (Equalizer)
        IconButton(
            onClick = onOpenEqualizer,
            modifier = Modifier
                .size(48.dp)
                .testTag("equalizer_button")
        ) {
            Icon(
                imageVector = Icons.Rounded.Tune,
                contentDescription = "Equalizer",
                tint = Color.White,
                modifier = Modifier
                    .size(23.dp)
                    .graphicsLayer { rotationZ = 90f }
            )
        }
    }
}

@Composable
private fun QueueSection(
    tracks: List<Track>,
    upperRatio: Float,
    isDraggingSplit: Boolean,
    onTrackSelect: (Track) -> Unit,
    onTrackOptions: (Track) -> Unit,
    onOpenQueue: () -> Unit,
    onBrowseLibrary: () -> Unit,
    onSplitDragStart: () -> Unit,
    onSplitDragDelta: (Float) -> Unit,
    onSplitDragEnd: () -> Unit,
    onResetOrCycleRatio: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val surfaceColor = Color(0xFFFFFFFF)
    val titleColor = Color(0xFF18181B)
    val subtitleColor = Color(0xFF6B7280)
    val activeTileBg = Color(0xFFFFF8E8)
    val activeRowBg = ActiveAmber.copy(alpha = 0.10f)
    val activeRowBorder = ActiveAmber.copy(alpha = 0.26f)
    val menuIconTint = Color(0xFF9CA3AF)
    val dividerColor = Color.Black.copy(alpha = 0.06f)

    val listState = rememberLazyListState()
    val activeIndex = remember(tracks) { tracks.indexOfFirst { it.isPlaying } }
    val upperPercent = (upperRatio * 100f).roundToInt().coerceIn(35, 72)
    val sheetPercent = 100 - upperPercent

    LaunchedEffect(activeIndex) {
        if (activeIndex >= 0 && !listState.isScrollInProgress) {
            listState.animateScrollToItem((activeIndex - 1).coerceAtLeast(0))
        }
    }

    Surface(
        modifier = modifier,
        color = surfaceColor,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        tonalElevation = 0.dp,
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
        ) {
            // Draggable split-ratio handle & "Up Next" sheet header
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onDoubleTap = {
                                onResetOrCycleRatio(DEFAULT_UPPER_SPLIT_RATIO)
                            }
                        )
                    }
                    .pointerInput(Unit) {
                        detectVerticalDragGestures(
                            onDragStart = { onSplitDragStart() },
                            onVerticalDrag = { change, dragAmount ->
                                change.consume()
                                onSplitDragDelta(dragAmount)
                            },
                            onDragEnd = { onSplitDragEnd() },
                            onDragCancel = { onSplitDragEnd() }
                        )
                    }
                    .padding(top = 10.dp)
                    .testTag("queue_split_drag_handle"),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .width(if (isDraggingSplit) 48.dp else 36.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(
                            if (isDraggingSplit) ActiveAmber else subtitleColor.copy(alpha = 0.36f)
                        )
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 6.dp, top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Up Next",
                        color = titleColor,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(subtitleColor.copy(alpha = 0.14f))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "${tracks.size}",
                            color = subtitleColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            style = MaterialTheme.typography.labelSmall.merge(TabularMono)
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    // Interactive split ratio badge: shows live ratio while dragging and cycles 40/60 -> 50/50 -> 60/40 on tap
                    Surface(
                        onClick = {
                            val nextPreset = when {
                                upperPercent < 46 -> 0.50f
                                upperPercent < 56 -> 0.60f
                                else -> 0.40f
                            }
                            onResetOrCycleRatio(nextPreset)
                        },
                        shape = CircleShape,
                        color = if (isDraggingSplit) ActiveAmber.copy(alpha = 0.22f) else subtitleColor.copy(alpha = 0.10f),
                        border = if (isDraggingSplit) BorderStroke(1.dp, ActiveAmber.copy(alpha = 0.55f)) else null,
                        modifier = Modifier.testTag("split_ratio_cycle_pill")
                    ) {
                        Text(
                            text = "$upperPercent / $sheetPercent",
                            color = if (isDraggingSplit) ActiveAmber else subtitleColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.labelSmall.merge(TabularMono),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    TextButton(
                        onClick = onOpenQueue,
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("open_full_queue_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.QueueMusic,
                            contentDescription = null,
                            tint = ActiveAmber,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "Manage",
                            color = ActiveAmber,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                HorizontalDivider(color = dividerColor, thickness = 0.5.dp)
            }

            if (tracks.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Your queue is empty",
                            color = subtitleColor,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        TextButton(
                            onClick = onBrowseLibrary,
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag("browse_library_button")
                        ) {
                            Text("Browse songs", color = ActiveAmber, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = 6.dp, bottom = 12.dp)
                ) {
                    itemsIndexed(tracks, key = { index, track -> "${track.id}_$index" }) { _, track ->
                        QueueTrackRow(
                            track = track,
                            titleColor = titleColor,
                            subtitleColor = subtitleColor,
                            activeTileBg = activeTileBg,
                            activeRowBg = activeRowBg,
                            activeRowBorder = activeRowBorder,
                            menuIconTint = menuIconTint,
                            onClick = { onTrackSelect(track) },
                            onOptions = { onTrackOptions(track) }
                        )
                    }

                    if (tracks.size <= 2) {
                        item(key = "short_queue_hint") {
                            ShortQueueHintCard(
                                titleColor = titleColor,
                                subtitleColor = subtitleColor,
                                dividerColor = dividerColor,
                                onBrowseLibrary = onBrowseLibrary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QueueTrackRow(
    track: Track,
    titleColor: Color,
    subtitleColor: Color,
    activeTileBg: Color,
    activeRowBg: Color,
    activeRowBorder: Color,
    menuIconTint: Color,
    onClick: () -> Unit,
    onOptions: () -> Unit
) {
    val subtitleText = remember(track.artist, track.album, track.duration) {
        val parts = listOfNotNull(
            track.artist.takeIf { it.isNotBlank() },
            track.album.takeIf { it.isNotBlank() }
        )
        if (parts.isNotEmpty()) parts.joinToString(" - ") else track.duration
    }

    val rowShape = RoundedCornerShape(14.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 2.dp)
            .clip(rowShape)
            .then(
                if (track.isPlaying) {
                    Modifier
                        .background(activeRowBg)
                        .border(1.dp, activeRowBorder, rowShape)
                } else {
                    Modifier
                }
            )
            .clickable(onClick = onClick)
            .padding(start = 8.dp, end = 2.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (track.isPlaying) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(activeTileBg),
                contentAlignment = Alignment.Center
            ) {
                EqualizerIndicator(color = ActiveAmber)
            }
        } else {
            Artwork(
                uri = track.artworkUri,
                size = 48.dp,
                radius = 8.dp,
                px = 128
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp)
        ) {
            Text(
                text = track.title,
                color = if (track.isPlaying) ActiveAmber else titleColor,
                fontSize = 15.sp,
                fontWeight = if (track.isPlaying) FontWeight.SemiBold else FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = subtitleText,
                color = subtitleColor,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (track.duration.isNotBlank()) {
            Text(
                text = track.duration,
                color = if (track.isPlaying) ActiveAmber else subtitleColor,
                fontSize = 12.sp,
                fontWeight = if (track.isPlaying) FontWeight.Medium else FontWeight.Normal,
                style = MaterialTheme.typography.labelSmall.merge(TabularMono),
                modifier = Modifier.padding(end = 2.dp)
            )
        }

        IconButton(
            onClick = onOptions,
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.MoreVert,
                contentDescription = "Track options",
                tint = menuIconTint,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun ShortQueueHintCard(
    titleColor: Color,
    subtitleColor: Color,
    dividerColor: Color,
    onBrowseLibrary: () -> Unit
) {
    Surface(
        onClick = onBrowseLibrary,
        shape = RoundedCornerShape(14.dp),
        color = subtitleColor.copy(alpha = 0.06f),
        border = BorderStroke(1.dp, dividerColor),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .testTag("browse_library_button")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(ActiveAmber.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.LibraryMusic,
                    contentDescription = null,
                    tint = ActiveAmber,
                    modifier = Modifier.size(18.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Queue ends after this track",
                    color = titleColor,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Tap to browse songs and queue up more tracks",
                    color = subtitleColor,
                    fontSize = 11.sp
                )
            }
            Text(
                text = "Browse",
                color = ActiveAmber,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun EqualizerIndicator(color: Color) {
    val reducedMotion = LocalReducedMotion.current
    val transition = rememberInfiniteTransition(label = "queue-equalizer")
    Row(
        modifier = Modifier.size(22.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.Bottom
    ) {
        val staticFractions = listOf(0.45f, 0.90f, 0.65f)
        listOf(420, 600, 500).forEachIndexed { index, duration ->
            val heightFraction by if (reducedMotion) {
                remember { mutableFloatStateOf(staticFractions[index]) }
            } else {
                transition.animateFloat(
                    initialValue = 0.28f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(tween(duration), RepeatMode.Reverse),
                    label = "bar-$index"
                )
            }
            Box(
                modifier = Modifier
                    .width(3.5.dp)
                    .height((18 * heightFraction).dp)
                    .clip(RoundedCornerShape(1.5.dp))
                    .background(color)
            )
        }
    }
}

private val previewTracks = listOf(
    Track("1", "SEM DEMORA - Ultra Slowed", "MXXNAVRA, SIXTINAGER", "2:18", isPlaying = false, album = "Single"),
    Track("2", "Everything Is Romantic", "Orchestra Club, Lewis Hanton", "2:21", isPlaying = true, album = "Brat"),
    Track("3", "Mortals Funk Remix", "LXNGVX, Warriyo", "3:12", isPlaying = false, album = "Mortals Funk Remix"),
    Track("4", "Raga of Revenge", "Anirudh Ravichander", "3:45", isPlaying = false, album = "DC")
)

@Preview(showBackground = true, device = "spec:width=412dp,height=915dp,dpi=420")
@Composable
private fun AtmosphericQueuePlayerLayoutPreview() {
    AtmosphericQueuePlayerLayout(
        uiState = AtmosphericQueueUiState(
            title = "Everything Is Romantic",
            artist = "Orchestra Club, Lewis Hanton",
            elapsedTime = "1:30",
            totalDuration = "2:21",
            progress = 0.64f,
            isPlaying = false,
            isFavorite = false,
            shuffleEnabled = false,
            repeatMode = Player.REPEAT_MODE_ONE,
            queue = previewTracks,
            codec = "FLAC",
            speed = 1.0f
        ),
        onPlayPauseClick = {},
        onPreviousClick = {},
        onNextClick = {},
        onSeek = {},
        onTrackSelect = {},
        onToggleFavorite = {},
        onToggleShuffle = {},
        onCycleRepeat = {},
        onOpenTheme = {},
        onCollapse = {},
        onOpenMenu = {},
        onTrackOptions = {}
    )
}
