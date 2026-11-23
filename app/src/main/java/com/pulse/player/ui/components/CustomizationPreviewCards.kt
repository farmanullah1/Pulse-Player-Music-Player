package com.pulse.player.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.OpenInFull
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pulse.player.data.settings.ColorSource
import com.pulse.player.data.settings.FontStylePreset
import com.pulse.player.data.settings.SeekStylePreset
import com.pulse.player.data.settings.ThemeMode
import com.pulse.player.playback.PlaybackUiState
import com.pulse.player.ui.buttons.ButtonLayout
import com.pulse.player.ui.buttons.ButtonPresetRegistry
import com.pulse.player.ui.screens.PlayerLayoutMode
import com.pulse.player.ui.theme.AppBackground
import com.pulse.player.ui.theme.LocalReducedMotion
import com.pulse.player.ui.theme.Pulse
import com.pulse.player.ui.theme.PulseTheme
import com.pulse.player.ui.theme.Skins
import com.pulse.player.ui.theme.TabularMono
import com.pulse.player.ui.themes.MountainSunset
import com.pulse.player.ui.themes.PlayerLayout
import com.pulse.player.ui.themes.PlayerThemes
import com.pulse.player.ui.themes.StarField
import com.pulse.player.ui.themes.WavesCanvas

enum class CustomizePreviewTarget(val label: String) {
    PLAYER_STAGE("Player Stage"),
    BUTTONS("Buttons & Controls"),
    ELEMENTS("Visible Elements"),
    COLORS_AND_SKINS("App Surface & Skins"),
    HOME_AND_LIBRARY("Home & Library")
}

private val PreviewAmber = Color(0xFFE89B25)

/**
 * Sticky collapsible preview header for Customize Player.
 * Automatically adapts what it displays to the active customization tab and live track.
 */
@Composable
fun LiveCustomizePreviewCard(
    target: CustomizePreviewTarget,
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
    onOpenFullPlayer: () -> Unit,
    layoutMode: PlayerLayoutMode,
    playerThemeId: String,
    buttonPresetId: String,
    customButtonOrder: List<String>,
    hiddenPlayerElements: Set<String>,
    seekStyle: SeekStylePreset,
    themeMode: ThemeMode,
    skinId: String,
    wallpaperPath: String?,
    colorSource: ColorSource,
    seedColor: Int,
    artworkAccent: Color,
    cornerRadiusDp: Int,
    fontStyle: FontStylePreset,
    forYouSectionOrder: List<String>,
    hiddenForYouSections: Set<String>,
    albumGridColumns: Int,
    listDensity: Int,
    playbackState: PlaybackUiState,
    isFavorite: Boolean,
    splitUpperRatio: Float = 0.40f,
    coverShape: String = "ROUNDED",
    backdropBlur: Float = 0.60f,
    backdropDim: Float = 0.55f,
    artworkGlow: Boolean = true,
    seekbarHeight: String = "MEDIUM",
    modifier: Modifier = Modifier
) {
    val trackTitle = playbackState.title.ifBlank { "Pulse Horizon" }
    val trackArtist = playbackState.artist.ifBlank { "Pulse Studio" }
    val activePlayerTheme = remember(playerThemeId) {
        PlayerThemes.firstOrNull { it.id == playerThemeId } ?: PlayerThemes.first()
    }
    val reducedMotion = LocalReducedMotion.current

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("customize_live_preview_card"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Sticky Preview Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 4.dp, top = 2.dp, bottom = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                )
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "LIVE PREVIEW",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "· ${target.label}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (!expanded) {
                        Text(
                            text = "$trackTitle · ${layoutMode.displayName} · ${fontStyle.label}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                TextButton(
                    onClick = onOpenFullPlayer,
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("preview_open_player_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.OpenInFull,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("Open Player", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }

                IconButton(
                    onClick = onToggleExpanded,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("customize_preview_toggle")
                ) {
                    Icon(
                        imageVector = if (expanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                        contentDescription = if (expanded) "Collapse preview" else "Expand preview",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 12.dp, end = 12.dp, bottom = 12.dp)
                ) {
                    AnimatedContent(
                        targetState = target,
                        transitionSpec = {
                            if (reducedMotion) fadeIn() togetherWith fadeOut()
                            else fadeIn() togetherWith fadeOut()
                        },
                        label = "customize-preview-mode"
                    ) { mode ->
                        when (mode) {
                            CustomizePreviewTarget.PLAYER_STAGE,
                            CustomizePreviewTarget.BUTTONS,
                            CustomizePreviewTarget.ELEMENTS -> {
                                PlayerStagePreview(
                                    layoutMode = layoutMode,
                                    themeColors = activePlayerTheme.colors,
                                    buttonPresetId = buttonPresetId,
                                    customButtonOrder = customButtonOrder,
                                    hiddenPlayerElements = hiddenPlayerElements,
                                    seekStyle = seekStyle,
                                    cornerRadiusDp = cornerRadiusDp,
                                    trackTitle = trackTitle,
                                    trackArtist = trackArtist,
                                    artworkUri = playbackState.artworkUri,
                                    isPlaying = playbackState.isPlaying,
                                    isFavorite = isFavorite,
                                    splitUpperRatio = splitUpperRatio,
                                    coverShape = coverShape,
                                    backdropBlur = backdropBlur,
                                    backdropDim = backdropDim,
                                    artworkGlow = artworkGlow,
                                    seekbarHeight = seekbarHeight,
                                    highlightMode = mode
                                )
                            }

                            CustomizePreviewTarget.COLORS_AND_SKINS -> {
                                SurfaceSkinPreview(
                                    themeMode = themeMode,
                                    skinId = skinId,
                                    wallpaperPath = wallpaperPath,
                                    colorSource = colorSource,
                                    seedColor = seedColor,
                                    artworkAccent = artworkAccent,
                                    cornerRadiusDp = cornerRadiusDp,
                                    fontStyle = fontStyle,
                                    trackTitle = trackTitle,
                                    trackArtist = trackArtist,
                                    artworkUri = playbackState.artworkUri,
                                    isPlaying = playbackState.isPlaying
                                )
                            }

                            CustomizePreviewTarget.HOME_AND_LIBRARY -> {
                                LibraryLayoutPreview(
                                    sectionOrder = forYouSectionOrder,
                                    hiddenSections = hiddenForYouSections,
                                    gridColumns = albumGridColumns,
                                    listDensity = listDensity,
                                    cornerRadiusDp = cornerRadiusDp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Miniature Now Playing stage shared by Customize Player and PlayerLayoutBottomSheet.
 * Reflects PlayerLayoutMode, PlayerTheme colors, ButtonPreset / Custom Button Order,
 * hiddenPlayerElements, SeekStylePreset, cornerRadiusDp, and active track state.
 */
@Composable
fun PlayerStagePreview(
    layoutMode: PlayerLayoutMode,
    themeColors: List<Color>,
    buttonPresetId: String,
    customButtonOrder: List<String>,
    hiddenPlayerElements: Set<String>,
    seekStyle: SeekStylePreset,
    cornerRadiusDp: Int,
    trackTitle: String,
    trackArtist: String,
    artworkUri: String?,
    isPlaying: Boolean,
    isFavorite: Boolean,
    modifier: Modifier = Modifier,
    splitUpperRatio: Float = 0.40f,
    coverShape: String = "ROUNDED",
    backdropBlur: Float = 0.60f,
    backdropDim: Float = 0.55f,
    artworkGlow: Boolean = true,
    seekbarHeight: String = "MEDIUM",
    highlightMode: CustomizePreviewTarget = CustomizePreviewTarget.PLAYER_STAGE
) {
    val buttonPreset = remember(buttonPresetId) { ButtonPresetRegistry.fromId(buttonPresetId) }
    val cardRadius = cornerRadiusDp.coerceIn(6, 24).dp
    val boundedSplit = splitUpperRatio.coerceIn(0.35f, 0.72f)
    val upperPercent = (boundedSplit * 100f).toInt().coerceIn(35, 72)
    val sheetPercent = 100 - upperPercent
    val showLyricsTab = "lyrics_preview" !in hiddenPlayerElements
    val showSignalPath = "signal_path" !in hiddenPlayerElements
    val showSleep = "sleep_timer" !in hiddenPlayerElements
    val showFav = "favorite_heart" !in hiddenPlayerElements
    val showSeekLabels = "waveform_seek" !in hiddenPlayerElements
    val showQueue = "queue_preview" !in hiddenPlayerElements
    val backdropColors = themeColors.ifEmpty { listOf(Color(0xFF183B38), Color(0xFF071312)) }
    val splitLightSheet = Color.White
    val splitTextPrimary = Color(0xFF18181B)
    val artRadiusDp = when (coverShape) {
        "CIRCLE", "VINYL_DISC" -> 50.dp
        "SQUARE" -> 2.dp
        "POSTER" -> 6.dp
        else -> (cornerRadiusDp / 2).coerceIn(4, 14).dp
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(172.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left Miniature Phone Frame (9:16-inspired preview stage)
        Box(
            modifier = Modifier
                .width(126.dp)
                .fillMaxHeight()
                .clip(RoundedCornerShape(16.dp))
                .background(Brush.verticalGradient(backdropColors))
                .border(1.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(16.dp))
        ) {
            when (layoutMode.layout) {
                PlayerLayout.STARRY -> StarField(Modifier.fillMaxSize())
                PlayerLayout.COAST -> WavesCanvas(Modifier.fillMaxSize())
                PlayerLayout.SHEET_QUEUE -> MountainSunset(Modifier.fillMaxSize())
                else -> Unit
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                backdropColors.first().copy(alpha = (backdropBlur.coerceIn(0f, 1f) * 0.25f)),
                                Color.Black.copy(alpha = (backdropDim.coerceIn(0.15f, 0.85f) * 0.42f))
                            )
                        )
                    )
            )

            Column(modifier = Modifier.fillMaxSize()) {
                val isSplitLayout = layoutMode.layout == PlayerLayout.ATMOSPHERIC_QUEUE || layoutMode.layout == PlayerLayout.SHEET_QUEUE
                val upperWeight = if (isSplitLayout) boundedSplit else 1f
                val isCompactMiniSplit = isSplitLayout && boundedSplit < 0.48f
                Column(
                    modifier = Modifier
                        .weight(upperWeight)
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = if (isCompactMiniSplit) 3.dp else 5.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Mini Top Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(5.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.55f))
                        )
                        if (showLyricsTab) {
                            Text(
                                text = "Song | Lyrics",
                                color = Color.White.copy(alpha = 0.85f),
                                fontSize = 6.5.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        } else {
                            Spacer(Modifier.width(20.dp))
                        }
                        Box(
                            modifier = Modifier
                                .size(5.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.55f))
                        )
                    }

                    // Layout-specific center artwork focal element (hidden in compact <=47% top split)
                    if (!isCompactMiniSplit) {
                        val glowBorder = if (artworkGlow) PreviewAmber.copy(alpha = 0.55f) else Color.White.copy(alpha = 0.14f)
                        when (layoutMode.layout) {
                            PlayerLayout.VINYL, PlayerLayout.ACOUSTIC, PlayerLayout.VINYL_TURNTABLE -> {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(if (layoutMode.layout == PlayerLayout.VINYL_TURNTABLE) Color(0xFF26170D) else Color(0xFF141418))
                                        .border(2.dp, if (layoutMode.layout == PlayerLayout.VINYL_TURNTABLE) PreviewAmber.copy(alpha = 0.65f) else Color.White.copy(alpha = 0.25f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Artwork(uri = artworkUri, size = 20.dp, radius = 10.dp, px = 64)
                                }
                            }

                            PlayerLayout.MINIMAL_DARK, PlayerLayout.MINIMAL_TYPOGRAPHY -> {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                    if (layoutMode.layout == PlayerLayout.MINIMAL_TYPOGRAPHY) {
                                        Text("STUDIO", color = Color.White.copy(alpha = 0.65f), fontSize = 6.sp, fontWeight = FontWeight.Bold)
                                    }
                                    if (showSignalPath) {
                                        MiniPill(text = "FLAC", accent = PreviewAmber)
                                    }
                                }
                            }

                            PlayerLayout.SPLIT_ARTWORK_LEFT -> {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color.White.copy(alpha = 0.10f))
                                        .padding(4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Artwork(uri = artworkUri, size = 28.dp, radius = artRadiusDp, px = 96)
                                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Text(trackTitle, color = Color.White, fontSize = 6.5.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        if (showSignalPath) MiniPill(text = "HI-FI", accent = PreviewAmber)
                                    }
                                }
                            }

                            PlayerLayout.COMPACT_CARD -> {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color.White.copy(alpha = 0.12f),
                                    border = BorderStroke(1.dp, glowBorder)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Artwork(uri = artworkUri, size = 30.dp, radius = artRadiusDp, px = 96)
                                        MiniPill(text = "CARD", accent = PreviewAmber)
                                    }
                                }
                            }

                            PlayerLayout.WAVEFORM_STUDIO -> {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .border(1.dp, glowBorder, RoundedCornerShape(10.dp))
                                        .padding(4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Artwork(uri = artworkUri, size = 32.dp, radius = artRadiusDp, px = 96)
                                        MiniPill(text = "WAVE", accent = PreviewAmber)
                                    }
                                }
                            }

                            else -> {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Artwork(
                                        uri = artworkUri,
                                        size = if (layoutMode.layout == PlayerLayout.ATMOSPHERIC_QUEUE) 28.dp else 36.dp,
                                        radius = artRadiusDp,
                                        px = 96,
                                        modifier = Modifier.border(1.dp, glowBorder, RoundedCornerShape(artRadiusDp))
                                    )
                                    if (showSignalPath || showSleep) {
                                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                            if (showSignalPath) {
                                                MiniPill(text = "HQ", accent = PreviewAmber)
                                            }
                                            if (showSleep) {
                                                MiniPill(text = "15m", accent = Color(0xFF7BE0AD))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Mini Title + Scrubber + Transport
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            if (isCompactMiniSplit) {
                                Artwork(
                                    uri = artworkUri,
                                    size = 11.dp,
                                    radius = 3.dp,
                                    px = 48
                                )
                            }
                            Text(
                                text = trackTitle,
                                color = Color.White,
                                fontSize = if (isCompactMiniSplit) 7.5.sp else 8.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        if (showSeekLabels) {
                            Spacer(Modifier.height(if (isCompactMiniSplit) 2.dp else 3.dp))
                            MiniSeekGraphic(
                                style = seekStyle,
                                accent = if (layoutMode.layout == PlayerLayout.NAMIDA) PreviewAmber else Color.White
                            )
                        }
                        Spacer(Modifier.height(if (isCompactMiniSplit) 2.dp else 4.dp))
                        MiniTransportVisual(
                            layout = buttonPreset.layout,
                            isPlaying = isPlaying,
                            showFavorite = showFav,
                            isFavorite = isFavorite,
                            customButtons = customButtonOrder
                        )
                    }
                }

                // Bottom Queue Sheet slice for split-screen layouts
                if (isSplitLayout) {
                    Box(
                        modifier = Modifier
                            .weight((1f - upperWeight).coerceAtLeast(0.28f))
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                            .background(splitLightSheet)
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.CenterHorizontally)
                                    .width(18.dp)
                                    .height(2.dp)
                                    .clip(CircleShape)
                                    .background(splitTextPrimary.copy(alpha = 0.25f))
                            )
                            if (showQueue) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(PreviewAmber.copy(alpha = 0.14f))
                                        .padding(horizontal = 5.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(12.dp)
                                            .clip(RoundedCornerShape(3.dp))
                                            .background(PreviewAmber.copy(alpha = 0.28f))
                                    )
                                    Spacer(Modifier.width(5.dp))
                                    Text(
                                        text = trackTitle,
                                        color = PreviewAmber,
                                        fontSize = 7.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            } else {
                                Text(
                                    text = "Queue preview hidden",
                                    color = splitTextPrimary.copy(alpha = 0.45f),
                                    fontSize = 7.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // Right Live Config Spec Breakdown
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = layoutMode.displayName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "$trackTitle · $trackArtist",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            when (highlightMode) {
                CustomizePreviewTarget.BUTTONS -> {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Preset: ${buttonPreset.name}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            customButtonOrder.take(5).forEach { name ->
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = buttonIconForName(name),
                                        contentDescription = name,
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(13.dp)
                                    )
                                }
                            }
                        }
                        Text(
                            text = "${customButtonOrder.size} custom buttons ordered",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                CustomizePreviewTarget.ELEMENTS -> {
                    val visibleCount = 6 - listOf(
                        "signal_path", "sleep_timer", "favorite_heart",
                        "waveform_seek", "lyrics_preview", "queue_preview"
                    ).count { it in hiddenPlayerElements }
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "$visibleCount of 6 elements visible",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            ElementStatusDot("Art/Sig", showSignalPath)
                            ElementStatusDot("Heart", showFav)
                            ElementStatusDot("Seek", showSeekLabels)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            ElementStatusDot("Lyrics", showLyricsTab)
                            ElementStatusDot("Sleep", showSleep)
                            ElementStatusDot("Queue", showQueue)
                        }
                    }
                }

                else -> {
                    val isSplit = layoutMode.layout == PlayerLayout.ATMOSPHERIC_QUEUE || layoutMode.layout == PlayerLayout.SHEET_QUEUE
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        val activeThemeName = PlayerThemes.firstOrNull { it.layout == layoutMode.layout }?.name ?: layoutMode.displayName
                        MiniSpecBadge(
                            text = if (isSplit) "Split: $upperPercent% / $sheetPercent% · $activeThemeName" else "Theme: $activeThemeName",
                            shapeRadius = cardRadius
                        )
                        MiniSpecBadge("Buttons: ${buttonPreset.name} · Seek: ${seekStyle.label}", shapeRadius = cardRadius)
                    }
                }
            }
        }
    }
}

/**
 * Live App Surface, Skin Gradient, Wallpaper, Typography, and Corner Radius preview
 * shared between Customize Player (Colors & Skins tab) and Skins & Wallpaper screen.
 */
@Composable
fun SurfaceSkinPreview(
    themeMode: ThemeMode,
    skinId: String,
    wallpaperPath: String?,
    colorSource: ColorSource,
    seedColor: Int,
    artworkAccent: Color,
    cornerRadiusDp: Int,
    fontStyle: FontStylePreset,
    trackTitle: String,
    trackArtist: String,
    artworkUri: String?,
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    val skin = remember(skinId) { Skins.firstOrNull { it.id == skinId } ?: Skins.first() }
    val safeRadius = cornerRadiusDp.coerceIn(4, 28)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(164.dp)
            .clip(RoundedCornerShape(safeRadius.dp))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(safeRadius.dp))
    ) {
        PulseTheme(
            mode = themeMode,
            skin = skin,
            colorSource = colorSource,
            seedColor = seedColor,
            artworkAccent = artworkAccent,
            cornerRadiusDp = safeRadius,
            fontStyle = fontStyle
        ) {
            AppBackground(
                mode = themeMode,
                wallpaperPath = wallpaperPath,
                modifier = Modifier.fillMaxSize()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Simulated Top Bar + Accent Chip
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "For You",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = "${fontStyle.label} · ${safeRadius}dp",
                                style = MaterialTheme.typography.labelSmall.merge(TabularMono),
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    // Simulated Content Card using live corner radius and skin colors
                    Surface(
                        shape = RoundedCornerShape(safeRadius.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
                        tonalElevation = 2.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Artwork(
                                uri = artworkUri,
                                size = 40.dp,
                                radius = (safeRadius / 2).coerceIn(4, 16).dp,
                                px = 128
                            )
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = trackTitle,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = trackArtist,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    // Mode & Skin caption strip
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Skin: ${skin.name}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                        val modeLabel = when (themeMode) {
                            ThemeMode.SYSTEM -> "System"
                            ThemeMode.LIGHT -> "Light"
                            ThemeMode.DARK -> "Dark"
                            ThemeMode.AMOLED -> "AMOLED"
                        }
                        Text(
                            text = if (wallpaperPath != null) "$modeLabel · Wallpaper" else "$modeLabel · ${colorSource.name.lowercase().replace('_', ' ')}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.72f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

/**
 * Miniature Home & Library skeleton preview reacting to For You section order,
 * hidden sections, album grid columns (2/3/4), and list row density.
 */
@Composable
fun LibraryLayoutPreview(
    sectionOrder: List<String>,
    hiddenSections: Set<String>,
    gridColumns: Int,
    listDensity: Int,
    cornerRadiusDp: Int,
    modifier: Modifier = Modifier
) {
    val visibleSections = remember(sectionOrder, hiddenSections) {
        sectionOrder.filter { it !in hiddenSections }
    }
    val sectionShortNames = remember {
        mapOf(
            "resume" to "Continue",
            "actions" to "Quick Play",
            "widgets" to "Tools",
            "recent" to "Recent",
            "top_picks" to "Top Picks"
        )
    }
    val cols = gridColumns.coerceIn(2, 4)
    val rowHeight = when (listDensity) {
        0 -> 14.dp
        2 -> 24.dp
        else -> 19.dp
    }
    val radius = (cornerRadiusDp / 2).coerceIn(4, 14).dp

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(148.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Left: For You Section Stack Order
        Surface(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "For You Order (${visibleSections.size})",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                if (visibleSections.isEmpty()) {
                    Text(
                        text = "All sections hidden",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    visibleSections.forEachIndexed { idx, key ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(radius))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${idx + 1}.",
                                style = MaterialTheme.typography.labelSmall.merge(TabularMono),
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = sectionShortNames[key] ?: key,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        // Right: Album Grid Columns & Song List Density
        Surface(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(10.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${cols}-Col Grid · ${when (listDensity) { 0 -> "Compact"; 2 -> "Comfortable"; else -> "Standard" }}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )

                // Mini Album Grid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    repeat(cols) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(26.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f))
                        )
                    }
                }

                // Mini Song Rows reflecting listDensity
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    repeat(3) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(rowHeight)
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f))
                                .padding(horizontal = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size((rowHeight.value * 0.55f).dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                            )
                            Spacer(Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.65f)
                                    .height(4.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f))
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MiniSeekGraphic(style: SeekStylePreset, accent: Color) {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(10.dp)
    ) {
        val midY = size.height / 2f
        when (style) {
            SeekStylePreset.WAVEFORM -> {
                val count = 14
                val slot = size.width / count
                val barW = slot * 0.5f
                for (i in 0 until count) {
                    val h = size.height * (0.35f + ((i * 3) % 5) * 0.14f)
                    drawRoundRect(
                        color = if (i < 8) accent else Color.White.copy(alpha = 0.28f),
                        topLeft = Offset(i * slot, midY - h / 2f),
                        size = Size(barW, h),
                        cornerRadius = CornerRadius(barW / 2f)
                    )
                }
            }

            SeekStylePreset.WAVY -> {
                drawLine(
                    color = Color.White.copy(alpha = 0.28f),
                    start = Offset(0f, midY),
                    end = Offset(size.width, midY),
                    strokeWidth = 2.dp.toPx(),
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = accent,
                    start = Offset(0f, midY),
                    end = Offset(size.width * 0.6f, midY),
                    strokeWidth = 3.dp.toPx(),
                    cap = StrokeCap.Round
                )
                drawCircle(accent, radius = 3.5.dp.toPx(), center = Offset(size.width * 0.6f, midY))
            }

            SeekStylePreset.LINEAR -> {
                drawLine(
                    color = Color.White.copy(alpha = 0.28f),
                    start = Offset(0f, midY),
                    end = Offset(size.width, midY),
                    strokeWidth = 2.dp.toPx(),
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = accent,
                    start = Offset(0f, midY),
                    end = Offset(size.width * 0.6f, midY),
                    strokeWidth = 2.dp.toPx(),
                    cap = StrokeCap.Round
                )
                drawCircle(accent, radius = 3.dp.toPx(), center = Offset(size.width * 0.6f, midY))
            }
        }
    }
}

@Composable
private fun MiniTransportVisual(
    layout: ButtonLayout,
    isPlaying: Boolean,
    showFavorite: Boolean,
    isFavorite: Boolean,
    customButtons: List<String>
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = when (layout) {
                ButtonLayout.SPLIT -> Arrangement.SpaceBetween
                ButtonLayout.ASYMMETRIC -> Arrangement.End
                else -> Arrangement.SpaceEvenly
            },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Rounded.SkipPrevious, null, tint = Color.White, modifier = Modifier.size(11.dp))
            Box(
                modifier = Modifier
                    .size(if (layout == ButtonLayout.EXPANDED || layout == ButtonLayout.FLOATING) 20.dp else 17.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.22f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(11.dp)
                )
            }
            Icon(Icons.Rounded.SkipNext, null, tint = Color.White, modifier = Modifier.size(11.dp))
        }

        if (layout != ButtonLayout.MINIMAL) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (showFavorite) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                        contentDescription = null,
                        tint = if (isFavorite) Color(0xFFFF5E7E) else Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(8.dp)
                    )
                }
                customButtons.filter { it != "Play/Pause" && it != "Previous" && it != "Next" }.take(3).forEach { btn ->
                    Icon(
                        imageVector = buttonIconForName(btn),
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.72f),
                        modifier = Modifier.size(8.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun MiniPill(text: String, accent: Color) {
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.14f))
            .padding(horizontal = 5.dp, vertical = 1.dp)
    ) {
        Text(
            text = text,
            color = accent,
            fontSize = 6.5.sp,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.labelSmall.merge(TabularMono)
        )
    }
}

@Composable
private fun MiniSpecBadge(text: String, shapeRadius: androidx.compose.ui.unit.Dp) {
    Surface(
        shape = RoundedCornerShape(shapeRadius),
        color = MaterialTheme.colorScheme.surface
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun ElementStatusDot(label: String, enabled: Boolean) {
    Surface(
        shape = CircleShape,
        color = if (enabled) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(5.dp)
                    .clip(CircleShape)
                    .background(if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline)
            )
            Text(
                text = label,
                fontSize = 9.sp,
                color = if (enabled) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun buttonIconForName(name: String): ImageVector = when (name) {
    "Shuffle" -> Icons.Rounded.Shuffle
    "Previous" -> Icons.Rounded.SkipPrevious
    "Play/Pause" -> Icons.Rounded.PlayArrow
    "Next" -> Icons.Rounded.SkipNext
    "Repeat" -> Icons.Rounded.Repeat
    "Favorite" -> Icons.Rounded.Favorite
    "Queue" -> Icons.AutoMirrored.Rounded.QueueMusic
    "Sleep Timer" -> Icons.Rounded.Bedtime
    "Equalizer" -> Icons.Rounded.Equalizer
    "Add to Playlist" -> Icons.AutoMirrored.Rounded.PlaylistAdd
    "Lyrics" -> Icons.Rounded.Lyrics
    "Speed / Pitch" -> Icons.Rounded.Speed
    else -> Icons.Rounded.MoreHoriz
}
