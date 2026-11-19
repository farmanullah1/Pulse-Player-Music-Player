package com.pulse.player.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Checkroom
import androidx.compose.material.icons.rounded.DashboardCustomize
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.ViewAgenda
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pulse.player.data.settings.ColorSource
import com.pulse.player.data.settings.FontStylePreset
import com.pulse.player.data.settings.ThemeMode
import com.pulse.player.ui.buttons.ButtonPresets
import com.pulse.player.ui.components.CustomizePreviewTarget
import com.pulse.player.ui.components.LiveCustomizePreviewCard
import com.pulse.player.ui.components.LocalBottomOverlayPadding
import com.pulse.player.ui.components.PulseTopBar
import com.pulse.player.ui.theme.LocalDarkTheme
import com.pulse.player.ui.theme.SeedPalettePresets
import com.pulse.player.ui.theme.Skins
import com.pulse.player.ui.theme.TabularMono
import com.pulse.player.ui.themes.PlayerThemes
import com.pulse.player.ui.viewmodel.MainViewModel
import kotlin.math.roundToInt

@Composable
fun PlayerCustomizeScreen(vm: MainViewModel) {
    BackHandler { vm.back() }

    var selectedTab by remember { mutableIntStateOf(0) }
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
            title = "Customize Player",
            subtitle = "Live visual editor · Playback continues uninterrupted",
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

        // Sticky Collapsible Auto-Adapting Live Preview
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
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )

        // Scrollable Tabs
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
                        selectedTab = index
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

        // Tab Content
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            when (selectedTab) {
                0 -> LayoutsTab(
                    current = currentLayout,
                    splitUpperRatio = splitUpperRatio,
                    onSelect = vm::setPlayerLayout,
                    onSplitRatioChange = vm::setSplitPlayerUpperRatio
                )
                1 -> ThemesTab(
                    currentId = currentThemeId,
                    onSelect = vm::applyPlayerTheme
                )
                2 -> ButtonsTab(
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
                    hiddenKeys = hiddenElements,
                    onToggle = vm::setPlayerElementVisible
                )
                4 -> ColorsAndSkinsTab(
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
                5 -> HomeAndLibraryTab(vm = vm)
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

@Composable
private fun LayoutsTab(
    current: PlayerLayoutMode,
    splitUpperRatio: Float,
    onSelect: (PlayerLayoutMode) -> Unit,
    onSplitRatioChange: (Float) -> Unit
) {
    val upperPercent = (splitUpperRatio.coerceIn(0.35f, 0.72f) * 100f).roundToInt()
    val queuePercent = 100 - upperPercent

    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = LocalBottomOverlayPadding.current + 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = "Split-Screen Queue Ratio",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Or drag the bottom sheet handle directly in Now Playing",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = "$upperPercent% / $queuePercent%",
                            style = MaterialTheme.typography.labelLarge.merge(TabularMono),
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Slider(
                        value = splitUpperRatio.coerceIn(0.35f, 0.72f),
                        onValueChange = onSplitRatioChange,
                        valueRange = 0.35f..0.72f,
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary,
                            inactiveTrackColor = MaterialTheme.colorScheme.outlineVariant
                        ),
                        modifier = Modifier.testTag("split_ratio_slider")
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            0.40f to "40 / 60 (Default)",
                            0.50f to "50 / 50",
                            0.60f to "60 / 40"
                        ).forEach { (presetRatio, label) ->
                            FilterChip(
                                selected = kotlin.math.abs(splitUpperRatio - presetRatio) < 0.02f,
                                onClick = { onSplitRatioChange(presetRatio) },
                                label = { Text(label) }
                            )
                        }
                    }
                }
            }
        }
        item {
            Text(
                text = "Choose Player Structure",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Defines how artwork, progress, buttons, and queue are structured.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        items(PlayerLayoutMode.entries, key = { it.id }) { mode ->
            val isSelected = mode == current
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelect(mode) }
                    .testTag("layout_card_${mode.id}"),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(16.dp),
                border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            mode.icon,
                            contentDescription = null,
                            tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = mode.displayName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (isSelected) {
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "ACTIVE",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        Text(
                            text = mode.subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ThemesTab(
    currentId: String,
    onSelect: (String) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = LocalBottomOverlayPadding.current + 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "Player Artwork & Background Themes",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Ambient artwork and disc visual atmospheres for Now Playing.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        items(PlayerThemes, key = { it.id }) { theme ->
            val isSelected = theme.id == currentId
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelect(theme.id) }
                    .testTag("theme_card_${theme.id}"),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(16.dp),
                border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(theme.colors.ifEmpty { listOf(MaterialTheme.colorScheme.primary, Color.Black) })
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Icon(Icons.Rounded.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = theme.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (theme.special) "Atmospheric visual backdrop" else "Standard theme rendering",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ButtonsTab(
    currentId: String,
    placedButtons: List<String>,
    onSelect: (String) -> Unit,
    onUpdateOrder: (List<String>) -> Unit,
    recommendation: String?,
    onRecommend: (goal: String, presetId: String) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }

    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = LocalBottomOverlayPadding.current + 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Quick Recommendations Section
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.AutoAwesome, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Quick Ergonomic Presets", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Pick your listening style to optimize transport button placement:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = currentId == "minimal",
                            onClick = { onRecommend("Minimal", "minimal") },
                            label = { Text("Minimal") }
                        )
                        FilterChip(
                            selected = currentId == "thumb_zone",
                            onClick = { onRecommend("One-Handed", "thumb_zone") },
                            label = { Text("One-Handed") }
                        )
                        FilterChip(
                            selected = currentId == "queue_inline",
                            onClick = { onRecommend("Queue", "queue_inline") },
                            label = { Text("Queue") }
                        )
                        FilterChip(
                            selected = currentId == "expanded",
                            onClick = { onRecommend("Accessible", "expanded") },
                            label = { Text("Accessible") }
                        )
                    }
                    if (recommendation != null) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = recommendation,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // Button Presets Header
        item {
            Text(
                text = "Button Presets",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Switch transport & action button arrangements cleanly.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        items(ButtonPresets, key = { it.id }) { preset ->
            val isSelected = preset.id == currentId
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelect(preset.id) }
                    .testTag("button_preset_${preset.id}"),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(14.dp),
                border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            preset.icon,
                            contentDescription = null,
                            tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = preset.name,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (isSelected) {
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "SELECTED",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Text(
                            text = preset.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Custom Buttons & Add Button Section
        item {
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "Custom Button Order",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Persisted order for customizable player action buttons.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                TextButton(
                    onClick = { showAddDialog = true },
                    modifier = Modifier.testTag("add_button_action")
                ) {
                    Icon(Icons.Rounded.Add, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Add")
                }
            }
        }

        items(placedButtons, key = { it }) { btn ->
            val idx = placedButtons.indexOf(btn)
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Rounded.GraphicEq, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(10.dp))
                        Text(btn, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = {
                                if (idx > 0) {
                                    val next = placedButtons.toMutableList()
                                    next.removeAt(idx)
                                    next.add(idx - 1, btn)
                                    onUpdateOrder(next)
                                }
                            },
                            enabled = idx > 0,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(Icons.Rounded.KeyboardArrowUp, "Move $btn up")
                        }
                        IconButton(
                            onClick = {
                                if (idx in 0 until placedButtons.lastIndex) {
                                    val next = placedButtons.toMutableList()
                                    next.removeAt(idx)
                                    next.add(idx + 1, btn)
                                    onUpdateOrder(next)
                                }
                            },
                            enabled = idx in 0 until placedButtons.lastIndex,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(Icons.Rounded.KeyboardArrowDown, "Move $btn down")
                        }
                        TextButton(
                            onClick = {
                                val next = placedButtons.toMutableList()
                                next.remove(btn)
                                onUpdateOrder(next)
                            }
                        ) {
                            Text("Hide", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        val catalog = listOf(
            "Shuffle", "Previous", "Play/Pause", "Next", "Repeat", "Favorite", "Queue", "More",
            "Sleep Timer", "Equalizer", "Add to Playlist", "Lyrics", "Speed / Pitch", "Track Info"
        )
        val availableToAdd = catalog.filter { it !in placedButtons }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add Button to Player") },
            text = {
                Column {
                    Text("Select a playback tool or library action from the catalog:")
                    Spacer(Modifier.height(12.dp))
                    availableToAdd.forEach { action ->
                        TextButton(
                            onClick = {
                                onUpdateOrder(placedButtons + action)
                                showAddDialog = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(action, modifier = Modifier.fillMaxWidth())
                        }
                    }
                    if (availableToAdd.isEmpty()) {
                        Text("All catalog actions are already placed.")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAddDialog = false }) { Text("Done") }
            }
        )
    }
}

@Composable
private fun ElementsTab(
    hiddenKeys: Set<String>,
    onToggle: (key: String, visible: Boolean) -> Unit
) {
    val elementDefs = remember {
        listOf(
            Triple("signal_path", "Audio Signal Path Badge", "Format, sample rate (44.1/48 kHz), and bit depth"),
            Triple("sleep_timer", "Sleep Timer Chip", "Countdown pill when sleep timer is active"),
            Triple("favorite_heart", "Favorite Heart Button", "Direct one-tap toggle for song favorites"),
            Triple("waveform_seek", "Waveform / Scrubber Labels", "Audio progress bar and elapsed/total timestamps"),
            Triple("lyrics_preview", "Lyrics Pill / Segmented Tab", "Quick switch between artwork and lyrics in player"),
            Triple("queue_preview", "Up Next Queue Preview", "Compact strip of upcoming tracks inside Now Playing")
        )
    }

    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = LocalBottomOverlayPadding.current + 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text(
                text = "Player Content Elements",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Show or hide individual badges and strips inside Now Playing.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        items(elementDefs, key = { it.first }) { (key, title, description) ->
            val enabled = key !in hiddenKeys
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(14.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f).padding(end = 12.dp)) {
                        Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                        Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = enabled,
                        onCheckedChange = { checked -> onToggle(key, checked) },
                        colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.primary)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ColorsAndSkinsTab(
    themeMode: ThemeMode,
    skinId: String,
    colorSource: ColorSource,
    seedColor: Int,
    cornerRadiusDp: Int,
    fontStyle: FontStylePreset,
    onSetThemeMode: (ThemeMode) -> Unit,
    onSetSkin: (String) -> Unit,
    onSetColorSource: (ColorSource) -> Unit,
    onSetSeedColor: (Int) -> Unit,
    onSetCornerRadius: (Int) -> Unit,
    onSetFontStyle: (FontStylePreset) -> Unit
) {
    val isDark = LocalDarkTheme.current

    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = LocalBottomOverlayPadding.current + 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Theme Mode",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    ThemeMode.SYSTEM to "System",
                    ThemeMode.LIGHT to "Light",
                    ThemeMode.DARK to "Dark",
                    ThemeMode.AMOLED to "AMOLED"
                ).forEach { (mode, label) ->
                    FilterChip(
                        selected = themeMode == mode,
                        onClick = { onSetThemeMode(mode) },
                        label = { Text(label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
            }
        }

        item {
            Text(
                text = "Typography Style",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Switch heading and UI voice across the entire app.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FontStylePreset.entries.forEach { preset ->
                    FilterChip(
                        selected = fontStyle == preset,
                        onClick = { onSetFontStyle(preset) },
                        label = { Text(preset.label) }
                    )
                }
            }
        }

        item {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Shape Corner Radius",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${cornerRadiusDp}dp",
                    style = MaterialTheme.typography.labelLarge.merge(TabularMono),
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
            Slider(
                value = cornerRadiusDp.toFloat(),
                onValueChange = { onSetCornerRadius(it.roundToInt()) },
                valueRange = 4f..28f,
                steps = 5,
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = MaterialTheme.colorScheme.outlineVariant
                )
            )
        }

        item {
            Text(
                text = "Dynamic Accent Source",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(6.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    ColorSource.PULSE_PALETTE to ("Active Skin Accent" to "Matches the chosen Pulse skin palette"),
                    ColorSource.ARTWORK_DRIVEN to ("Artwork-Driven Palette" to "Extracts vibrant tones from current album art"),
                    ColorSource.MATERIAL_YOU to ("Material You / Wallpaper" to "Follows Android dynamic wallpaper colors"),
                    ColorSource.CUSTOM_SEED to ("Custom Accent Swatch" to "Pick a signature color for both Light and Dark mode")
                ).forEach { (src, info) ->
                    val (title, desc) = info
                    val isSelected = colorSource == src
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSetColorSource(src) },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface
                        ),
                        shape = RoundedCornerShape(12.dp),
                        border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                                Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            if (isSelected) {
                                Icon(Icons.Rounded.Check, contentDescription = "Selected", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
            }
        }

        item {
            Text(
                text = "Custom Accent Swatches",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Tapping a swatch switches accent source to Custom Accent.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(10.dp))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SeedPalettePresets.forEach { preset ->
                    val previewArgb = if (isDark) preset.darkArgb else preset.lightArgb
                    val previewColor = Color(previewArgb)
                    val selected = colorSource == ColorSource.CUSTOM_SEED && (seedColor == previewArgb || seedColor == preset.darkArgb || seedColor == preset.lightArgb)
                    val checkTint = if (previewColor.luminance() > 0.45f) Color(0xFF141122) else Color.White
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .width(64.dp)
                            .clickable {
                                onSetSeedColor(previewArgb)
                                onSetColorSource(ColorSource.CUSTOM_SEED)
                            }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(previewColor)
                                .border(
                                    width = if (selected) 3.dp else 1.dp,
                                    color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outlineVariant,
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (selected) {
                                Icon(Icons.Rounded.Check, contentDescription = preset.name, tint = checkTint, modifier = Modifier.size(18.dp))
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = preset.name,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        item {
            Text(
                text = "App Skin Palettes",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Daylight & atmospheric background palettes tuned for both Light and Dark modes.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        items(Skins, key = { it.id }) { skin ->
            val isSelected = skin.id == skinId
            val previewColors = if (isDark) skin.colors else skin.lightColors
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onSetSkin(skin.id)
                        if (skin.isDaylight && isDark) onSetThemeMode(ThemeMode.LIGHT)
                    },
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(14.dp),
                border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(previewColors))
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(skin.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                        Text(
                            text = if (skin.isDaylight) "Daylight light-mode palette" else "Adaptive light & dark gradient palette",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (isSelected) {
                        Text("ACTIVE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeAndLibraryTab(vm: MainViewModel) {
    val sectionOrder by vm.forYouSectionOrder.collectAsStateWithLifecycle()
    val hiddenSections by vm.hiddenForYouSections.collectAsStateWithLifecycle()
    val gridCols by vm.albumGridColumns.collectAsStateWithLifecycle()
    val density by vm.listDensity.collectAsStateWithLifecycle()

    val sectionLabels = remember {
        mapOf(
            "resume" to "Continue Listening Banner",
            "actions" to "Shuffle & Play Quick Buttons",
            "widgets" to "Overview & Tool Tiles",
            "recent" to "Recently Added Carousel",
            "top_picks" to "Top Picks For You Carousel"
        )
    }

    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = LocalBottomOverlayPadding.current + 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "For You Section Arrangement",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Reorder or hide blocks on the For You screen. Changes save immediately.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        items(sectionOrder, key = { it }) { key ->
            val idx = sectionOrder.indexOf(key)
            val enabled = key !in hiddenSections
            val label = sectionLabels[key] ?: key
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = {
                                if (idx > 0) {
                                    val next = sectionOrder.toMutableList()
                                    next.removeAt(idx)
                                    next.add(idx - 1, key)
                                    vm.setForYouSectionOrder(next)
                                }
                            },
                            enabled = idx > 0,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(Icons.Rounded.KeyboardArrowUp, "Move $label up")
                        }
                        IconButton(
                            onClick = {
                                if (idx in 0 until sectionOrder.lastIndex) {
                                    val next = sectionOrder.toMutableList()
                                    next.removeAt(idx)
                                    next.add(idx + 1, key)
                                    vm.setForYouSectionOrder(next)
                                }
                            },
                            enabled = idx in 0 until sectionOrder.lastIndex,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(Icons.Rounded.KeyboardArrowDown, "Move $label down")
                        }
                        Switch(
                            checked = enabled,
                            onCheckedChange = { chk -> vm.setForYouSectionVisible(key, chk) },
                            colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.primary)
                        )
                    }
                }
            }
        }

        item {
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Library Grid & List Density",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Controls album art grid density and song row spacing.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(2 to "2 Columns", 3 to "3 Columns", 4 to "4 Columns").forEach { (cols, label) ->
                    FilterChip(
                        selected = gridCols == cols,
                        onClick = { vm.setAlbumGridColumns(cols) },
                        label = { Text(label) }
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(0 to "Compact List", 1 to "Standard List", 2 to "Comfortable List").forEach { (d, label) ->
                    FilterChip(
                        selected = density == d,
                        onClick = { vm.setListDensity(d) },
                        label = { Text(label) }
                    )
                }
            }
        }
    }
}
