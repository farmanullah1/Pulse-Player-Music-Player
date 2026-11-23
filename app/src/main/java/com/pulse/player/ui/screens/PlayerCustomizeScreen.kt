package com.pulse.player.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Checkroom
import androidx.compose.material.icons.rounded.DashboardCustomize
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.ViewAgenda
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pulse.player.data.settings.ColorSource
import com.pulse.player.data.settings.FontStylePreset
import com.pulse.player.data.settings.ThemeMode
import com.pulse.player.ui.components.CustomizePreviewTarget
import com.pulse.player.ui.components.LiveCustomizePreviewCard
import com.pulse.player.ui.components.PulseTopBar
import com.pulse.player.ui.viewmodel.MainViewModel

@Composable
fun PlayerCustomizeScreen(vm: MainViewModel) {
    BackHandler { vm.back() }

    val selectedTab by vm.customizeSelectedTab.collectAsStateWithLifecycle()
    var previewExpanded by remember { mutableStateOf(true) }
    var showResetDialog by remember { mutableStateOf(false) }
    var recommendationNotice by remember { mutableStateOf<String?>(null) }

    val tabs = listOf(
        "Layouts" to Icons.Rounded.DashboardCustomize,
        "Themes" to Icons.Rounded.Palette,
        "Buttons" to Icons.Rounded.GridView,
        "Elements" to Icons.Rounded.Layers,
        "Colors & Skins" to Icons.Rounded.Checkroom,
        "Home & Library" to Icons.Rounded.ViewAgenda
    )

    val currentLayout by vm.activePlayerLayout.collectAsStateWithLifecycle()
    val currentThemeId by vm.playerThemeId.collectAsStateWithLifecycle()
    val currentButtonPresetId by vm.buttonPresetId.collectAsStateWithLifecycle()
    val currentSkinId by vm.skinId.collectAsStateWithLifecycle()
    val currentWallpaperPath by vm.wallpaperPath.collectAsStateWithLifecycle()
    val currentThemeMode by vm.themeMode.collectAsStateWithLifecycle()
    val currentColorSource by vm.colorSource.collectAsStateWithLifecycle()
    val currentSeedColor by vm.seedColor.collectAsStateWithLifecycle()
    val currentArtworkAccent by vm.artworkAccent.collectAsStateWithLifecycle()
    val currentCornerRadius by vm.cornerRadiusDp.collectAsStateWithLifecycle()
    val currentFontStyle by vm.fontStyle.collectAsStateWithLifecycle()
    val currentSeekStyle by vm.seekStyle.collectAsStateWithLifecycle()
    val hiddenElements by vm.hiddenPlayerElements.collectAsStateWithLifecycle()
    val customButtonOrder by vm.customButtonOrder.collectAsStateWithLifecycle()
    val forYouSectionOrder by vm.forYouSectionOrder.collectAsStateWithLifecycle()
    val hiddenForYouSections by vm.hiddenForYouSections.collectAsStateWithLifecycle()
    val albumGridColumns by vm.albumGridColumns.collectAsStateWithLifecycle()
    val listDensity by vm.listDensity.collectAsStateWithLifecycle()
    val splitUpperRatio by vm.splitPlayerUpperRatio.collectAsStateWithLifecycle()
    val coverShape by vm.playerCoverShape.collectAsStateWithLifecycle()
    val backdropBlur by vm.playerBackdropBlur.collectAsStateWithLifecycle()
    val backdropDim by vm.playerBackdropDim.collectAsStateWithLifecycle()
    val artworkGlow by vm.playerArtworkGlow.collectAsStateWithLifecycle()
    val swipeSkip by vm.playerSwipeSkip.collectAsStateWithLifecycle()
    val seekbarHeight by vm.playerSeekbarHeight.collectAsStateWithLifecycle()
    val playerState by vm.player.state.collectAsStateWithLifecycle()
    val isFavorite by vm.isCurrentFavorite.collectAsStateWithLifecycle()

    val previewTarget = remember(selectedTab) {
        when (selectedTab) {
            2 -> CustomizePreviewTarget.BUTTONS
            3 -> CustomizePreviewTarget.ELEMENTS
            4 -> CustomizePreviewTarget.COLORS_AND_SKINS
            5 -> CustomizePreviewTarget.HOME_AND_LIBRARY
            else -> CustomizePreviewTarget.PLAYER_STAGE
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        PulseTopBar(
            title = "Custom Player Studio",
            subtitle = "${PlayerLayoutMode.entries.size} layouts · themes, artwork, gestures & controls",
            onBack = { vm.back() },
            backTestTag = "customize_back_button",
            actions = {
                IconButton(
                    onClick = { showResetDialog = true },
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("customize_reset_button")
                ) {
                    Icon(
                        Icons.Rounded.RestartAlt,
                        contentDescription = "Reset Customization",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        )

        LiveCustomizePreviewCard(
            target = previewTarget,
            expanded = previewExpanded,
            onToggleExpanded = { previewExpanded = !previewExpanded },
            onOpenFullPlayer = { vm.openNowPlaying() },
            layoutMode = currentLayout,
            playerThemeId = currentThemeId,
            buttonPresetId = currentButtonPresetId,
            customButtonOrder = customButtonOrder,
            hiddenPlayerElements = hiddenElements,
            seekStyle = currentSeekStyle,
            themeMode = currentThemeMode,
            skinId = currentSkinId,
            wallpaperPath = currentWallpaperPath,
            colorSource = currentColorSource,
            seedColor = currentSeedColor,
            artworkAccent = currentArtworkAccent,
            cornerRadiusDp = currentCornerRadius,
            fontStyle = currentFontStyle,
            forYouSectionOrder = forYouSectionOrder,
            hiddenForYouSections = hiddenForYouSections,
            albumGridColumns = albumGridColumns,
            listDensity = listDensity,
            playbackState = playerState,
            isFavorite = isFavorite,
            splitUpperRatio = splitUpperRatio,
            coverShape = coverShape,
            backdropBlur = backdropBlur,
            backdropDim = backdropDim,
            artworkGlow = artworkGlow,
            seekbarHeight = seekbarHeight,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )

        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            edgePadding = 16.dp,
            containerColor = MaterialTheme.colorScheme.background,
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            tabs.forEachIndexed { index, (title, icon) ->
                Tab(
                    selected = selectedTab == index,
                    onClick = {
                        vm.customizeSelectedTab.value = index
                        recommendationNotice = null
                    },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(title)
                        }
                    }
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            when (selectedTab) {
                0 -> LayoutsTab(
                    listState = vm.lazyListStateFor("customize_0"),
                    current = currentLayout,
                    splitUpperRatio = splitUpperRatio,
                    coverShape = coverShape,
                    backdropBlur = backdropBlur,
                    backdropDim = backdropDim,
                    artworkGlow = artworkGlow,
                    swipeSkip = swipeSkip,
                    seekbarHeight = seekbarHeight,
                    onSelect = vm::setPlayerLayout,
                    onSplitRatioChange = vm::setSplitPlayerUpperRatio,
                    onCoverShapeChange = vm::setPlayerCoverShape,
                    onBackdropBlurChange = vm::setPlayerBackdropBlur,
                    onBackdropDimChange = vm::setPlayerBackdropDim,
                    onArtworkGlowChange = vm::setPlayerArtworkGlow,
                    onSwipeSkipChange = vm::setPlayerSwipeSkip,
                    onSeekbarHeightChange = vm::setPlayerSeekbarHeight
                )
                1 -> ThemesTab(
                    listState = vm.lazyListStateFor("customize_1"),
                    currentId = currentThemeId,
                    onSelect = vm::applyPlayerTheme
                )
                2 -> ButtonsTab(
                    listState = vm.lazyListStateFor("customize_2"),
                    currentId = currentButtonPresetId,
                    placedButtons = customButtonOrder,
                    onSelect = vm::setButtonPreset,
                    onUpdateOrder = vm::setCustomButtonOrder,
                    recommendation = recommendationNotice,
                    onRecommend = { goal, presetId ->
                        vm.setButtonPreset(presetId)
                        recommendationNotice = "Applied '$goal' button preset."
                    }
                )
                3 -> ElementsTab(
                    listState = vm.lazyListStateFor("customize_3"),
                    hiddenKeys = hiddenElements,
                    onToggle = vm::setPlayerElementVisible
                )
                4 -> ColorsAndSkinsTab(
                    listState = vm.lazyListStateFor("customize_4"),
                    themeMode = currentThemeMode,
                    skinId = currentSkinId,
                    colorSource = currentColorSource,
                    seedColor = currentSeedColor,
                    cornerRadiusDp = currentCornerRadius,
                    fontStyle = currentFontStyle,
                    onSetThemeMode = vm::setThemeMode,
                    onSetSkin = vm::setSkin,
                    onSetColorSource = vm::setColorSource,
                    onSetSeedColor = vm::setSeedColor,
                    onSetCornerRadius = vm::setCornerRadius,
                    onSetFontStyle = vm::setFontStyle
                )
                5 -> HomeAndLibraryTab(
                    listState = vm.lazyListStateFor("customize_5"),
                    vm = vm
                )
            }
        }
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Reset Customization", fontWeight = FontWeight.Bold) },
            text = { Text("Reset just the active tab or restore every appearance and player customization to defaults?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        when (selectedTab) {
                            0 -> {
                                vm.setPlayerLayout(PlayerLayoutMode.ATMOSPHERIC_QUEUE)
                                vm.setSplitPlayerUpperRatio(0.40f)
                                vm.setPlayerCoverShape("ROUNDED")
                                vm.setPlayerBackdropBlur(0.60f)
                                vm.setPlayerBackdropDim(0.55f)
                                vm.setPlayerArtworkGlow(true)
                                vm.setPlayerSwipeSkip(true)
                                vm.setPlayerSeekbarHeight("MEDIUM")
                            }
                            1 -> vm.applyPlayerTheme("atmospheric_queue")
                            2 -> {
                                vm.setButtonPreset("centered")
                                vm.setCustomButtonOrder(listOf("Shuffle", "Previous", "Play/Pause", "Next", "Repeat", "Favorite", "Queue", "More"))
                            }
                            3 -> vm.clearHiddenPlayerElements()
                            4 -> {
                                vm.setThemeMode(ThemeMode.DARK)
                                vm.setSkin("studio_amber")
                                vm.setColorSource(ColorSource.PULSE_PALETTE)
                                vm.setSeedColor(0)
                                vm.setCornerRadius(16)
                                vm.setFontStyle(FontStylePreset.MODERN)
                            }
                            5 -> {
                                vm.resetForYouLayout()
                                vm.setListDensity(1)
                                vm.setAlbumGridColumns(2)
                            }
                        }
                        showResetDialog = false
                    }
                ) { Text("Reset This Tab") }
            },
            dismissButton = {
                Row {
                    TextButton(
                        onClick = {
                            vm.setPlayerLayout(PlayerLayoutMode.ATMOSPHERIC_QUEUE)
                            vm.setSplitPlayerUpperRatio(0.40f)
                            vm.setPlayerCoverShape("ROUNDED")
                            vm.setPlayerBackdropBlur(0.60f)
                            vm.setPlayerBackdropDim(0.55f)
                            vm.setPlayerArtworkGlow(true)
                            vm.setPlayerSwipeSkip(true)
                            vm.setPlayerSeekbarHeight("MEDIUM")
                            vm.applyPlayerTheme("atmospheric_queue")
                            vm.setButtonPreset("centered")
                            vm.setCustomButtonOrder(listOf("Shuffle", "Previous", "Play/Pause", "Next", "Repeat", "Favorite", "Queue", "More"))
                            vm.clearHiddenPlayerElements()
                            vm.setThemeMode(ThemeMode.DARK)
                            vm.setSkin("studio_amber")
                            vm.setColorSource(ColorSource.PULSE_PALETTE)
                            vm.setSeedColor(0)
                            vm.setCornerRadius(16)
                            vm.setFontStyle(FontStylePreset.MODERN)
                            vm.resetForYouLayout()
                            vm.setListDensity(1)
                            vm.setAlbumGridColumns(2)
                            showResetDialog = false
                        }
                    ) { Text("Reset All", color = MaterialTheme.colorScheme.error) }
                    TextButton(onClick = { showResetDialog = false }) { Text("Cancel") }
                }
            }
        )
    }
}
