package com.pulse.player.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Checkroom
import androidx.compose.material.icons.rounded.DashboardCustomize
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Subtitles
import androidx.compose.material.icons.rounded.ViewAgenda
import androidx.compose.material.icons.rounded.ViewColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pulse.player.data.settings.ColorSource
import com.pulse.player.data.settings.ThemeMode
import com.pulse.player.ui.buttons.ButtonLayout
import com.pulse.player.ui.buttons.ButtonPreset
import com.pulse.player.ui.buttons.ButtonPresets
import com.pulse.player.ui.theme.Pulse
import com.pulse.player.ui.theme.Skins
import com.pulse.player.ui.themes.PlayerThemes
import com.pulse.player.ui.viewmodel.MainViewModel

@Composable
fun PlayerCustomizeScreen(vm: MainViewModel) {
    BackHandler { vm.go(Screen.Home) }

    var selectedTab by remember { mutableIntStateOf(0) }
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
    val currentThemeMode by vm.themeMode.collectAsStateWithLifecycle()
    val currentColorSource by vm.colorSource.collectAsStateWithLifecycle()
    val playerState by vm.player.state.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { vm.go(Screen.Home) },
                modifier = Modifier.testTag("customize_back_button")
            ) {
                Icon(
                    Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            Column(Modifier.weight(1f)) {
                Text(
                    text = "Customize Player",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Live in-player customizer · Audio continues seamlessly",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(
                onClick = { showResetDialog = true },
                modifier = Modifier.testTag("customize_reset_button")
            ) {
                Icon(
                    Icons.Rounded.RestartAlt,
                    contentDescription = "Reset Customization",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        // Live Mini State Header
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Pulse.Cyan.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Rounded.MusicNote,
                        contentDescription = null,
                        tint = Pulse.Cyan,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = playerState.title.ifBlank { "Now Playing Preview" },
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Layout: ${currentLayout.displayName} · Theme: $currentThemeId · Buttons: $currentButtonPresetId",
                        style = MaterialTheme.typography.labelSmall,
                        color = Pulse.Cyan,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // Scrollable Tabs
        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            edgePadding = 16.dp,
            containerColor = MaterialTheme.colorScheme.background,
            contentColor = Pulse.Cyan
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
                    onSelect = vm::setPlayerLayout
                )
                1 -> ThemesTab(
                    currentId = currentThemeId,
                    onSelect = vm::applyPlayerTheme
                )
                2 -> ButtonsTab(
                    currentId = currentButtonPresetId,
                    onSelect = vm::setButtonPreset,
                    recommendation = recommendationNotice,
                    onRecommend = { goal, presetId ->
                        vm.setButtonPreset(presetId)
                        recommendationNotice = "Applied '$goal' recommendation: switched button preset to '$presetId'."
                    }
                )
                3 -> ElementsTab()
                4 -> ColorsAndSkinsTab(
                    themeMode = currentThemeMode,
                    skinId = currentSkinId,
                    colorSource = currentColorSource,
                    onSetThemeMode = vm::setThemeMode,
                    onSetSkin = vm::setSkin,
                    onSetColorSource = vm::setColorSource
                )
                5 -> HomeAndLibraryTab(vm = vm)
            }
        }
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Reset Customization", fontWeight = FontWeight.Bold) },
            text = { Text("Would you like to reset only the current tab, or restore all player customizations to their default settings?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        when (selectedTab) {
                            0 -> vm.setPlayerLayout(PlayerLayoutMode.ATMOSPHERIC_QUEUE)
                            1 -> vm.applyPlayerTheme("classic")
                            2 -> vm.setButtonPreset("centered")
                            4 -> {
                                vm.setThemeMode(ThemeMode.DARK)
                                vm.setSkin("plum")
                                vm.setColorSource(ColorSource.PULSE_PALETTE)
                            }
                            5 -> {
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
                            vm.applyPlayerTheme("classic")
                            vm.setButtonPreset("centered")
                            vm.setThemeMode(ThemeMode.DARK)
                            vm.setSkin("plum")
                            vm.setColorSource(ColorSource.PULSE_PALETTE)
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
    onSelect: (PlayerLayoutMode) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
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
                border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, Pulse.Cyan) else null
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
                            .background(if (isSelected) Pulse.Cyan else MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            mode.icon,
                            contentDescription = null,
                            tint = if (isSelected) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant,
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
                                    color = Pulse.Cyan
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
        contentPadding = PaddingValues(16.dp),
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
                text = "All 9 original themes with ambient visuals and disc/mesh art.",
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
                border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, Pulse.Cyan) else null
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
                                Brush.linearGradient(theme.colors.ifEmpty { listOf(Pulse.Cyan, Color.Black) })
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
                            text = if (theme.special) "Special visual effect atmosphere" else "Standard theme rendering",
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
private fun ButtonsTab(
    currentId: String,
    onSelect: (String) -> Unit,
    recommendation: String?,
    onRecommend: (goal: String, presetId: String) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    val placedButtons = remember {
        mutableStateListOf(
            "Shuffle", "Previous", "Play/Pause", "Next", "Repeat", "Favorite", "Queue", "More"
        )
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Recommendations Section
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Pulse.Violet.copy(alpha = 0.12f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.AutoAwesome, null, tint = Pulse.Violet, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Recommend Layout", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Choose your interaction goal to optimize playback controls:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = false,
                            onClick = { onRecommend("Minimal", "minimal") },
                            label = { Text("Minimal") }
                        )
                        FilterChip(
                            selected = false,
                            onClick = { onRecommend("One-Handed", "thumb_zone") },
                            label = { Text("One-Handed") }
                        )
                        FilterChip(
                            selected = false,
                            onClick = { onRecommend("Queue", "queue_inline") },
                            label = { Text("Queue") }
                        )
                        FilterChip(
                            selected = false,
                            onClick = { onRecommend("Accessible", "expanded") },
                            label = { Text("Accessible") }
                        )
                    }
                    if (recommendation != null) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = recommendation,
                            style = MaterialTheme.typography.labelSmall,
                            color = Pulse.Mint,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // 15 Presets Header
        item {
            Text(
                text = "15 Button Presets",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Select any of the 15 verified button presets without interrupting audio.",
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
                border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, Pulse.Cyan) else null
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
                            .background(if (isSelected) Pulse.Cyan else MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            preset.icon,
                            contentDescription = null,
                            tint = if (isSelected) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant,
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
                                    color = Pulse.Cyan,
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
                Column {
                    Text(
                        text = "Custom Button Order & Add Button",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Rearrange, hide, or restore action buttons.",
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
                    Text("Add Button")
                }
            }
        }

        items(placedButtons) { btn ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.GraphicEq, null, tint = Pulse.Cyan, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(10.dp))
                        Text(btn, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                    }
                    Row {
                        TextButton(
                            onClick = {
                                val idx = placedButtons.indexOf(btn)
                                if (idx > 0) {
                                    placedButtons.removeAt(idx)
                                    placedButtons.add(idx - 1, btn)
                                }
                            }
                        ) { Text("Up", style = MaterialTheme.typography.labelSmall) }
                        TextButton(
                            onClick = {
                                val idx = placedButtons.indexOf(btn)
                                if (idx < placedButtons.size - 1) {
                                    placedButtons.removeAt(idx)
                                    placedButtons.add(idx + 1, btn)
                                }
                            }
                        ) { Text("Down", style = MaterialTheme.typography.labelSmall) }
                        TextButton(
                            onClick = { placedButtons.remove(btn) }
                        ) { Text("Hide", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall) }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        val availableToAdd = listOf(
            "Sleep Timer", "Equalizer", "Add to Playlist", "Lyrics", "Speed / Pitch", "Track Info"
        ).filter { it !in placedButtons }

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
                                placedButtons.add(action)
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
private fun ElementsTab() {
    val elements = remember {
        mutableStateMapOf(
            "artwork" to (true to ("Artwork / Album Cover" to "Shows local album cover with contrast styling")),
            "title_artist" to (true to ("Title & Artist Info" to "Track title with marquee scroll and artist label")),
            "waveform_seek" to (true to ("Waveform Seekbar" to "Real audio waveform progress bar with seek controls")),
            "lyrics_preview" to (true to ("Synced Lyrics Preview" to "Displays active synchronized lyric line")),
            "queue_preview" to (true to ("Up Next Queue Preview" to "Compact preview strip of next songs in queue")),
            "signal_path" to (true to ("Audio Signal Path Badge" to "Format, sample rate (44.1/48kHz), and bit depth")),
            "sleep_timer" to (true to ("Sleep Timer Chip" to "Countdown chip when sleep timer is active")),
            "favorite_heart" to (true to ("Favorite Heart Button" to "Direct one-tap toggle for song favorites"))
        )
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
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
                text = "Show or hide specific content blocks within the active layout.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        items(elements.keys.toList()) { key ->
            val (enabled, info) = elements[key] ?: (true to ("" to ""))
            val (title, description) = info
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
                    Column(Modifier.weight(1f)) {
                        Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                        Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = enabled,
                        onCheckedChange = { checked ->
                            elements[key] = (checked to info)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Pulse.Cyan,
                            checkedTrackColor = Pulse.Cyan.copy(alpha = 0.3f)
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun ColorsAndSkinsTab(
    themeMode: ThemeMode,
    skinId: String,
    colorSource: ColorSource,
    onSetThemeMode: (ThemeMode) -> Unit,
    onSetSkin: (String) -> Unit,
    onSetColorSource: (ColorSource) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "Theme Mode",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(ThemeMode.SYSTEM to "System", ThemeMode.DARK to "Dark", ThemeMode.AMOLED to "AMOLED", ThemeMode.LIGHT to "Light").forEach { (mode, label) ->
                    FilterChip(
                        selected = themeMode == mode,
                        onClick = { onSetThemeMode(mode) },
                        label = { Text(label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Pulse.Cyan.copy(alpha = 0.2f),
                            selectedLabelColor = Pulse.Cyan
                        )
                    )
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
                text = "8 distinct color identities designed for high contrast and vibrance.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        items(Skins.entries, key = { it.id }) { skin ->
            val isSelected = skin.id == skinId
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSetSkin(skin.id) },
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(14.dp),
                border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, Pulse.Cyan) else null
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
                            .background(skin.accent)
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(skin.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                        Text("Signature accent with custom dark background", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (isSelected) {
                        Text("ACTIVE", style = MaterialTheme.typography.labelSmall, color = Pulse.Cyan, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        item {
            Text(
                text = "Dynamic Color Source",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            listOf(
                ColorSource.PULSE_PALETTE to ("Static Skin Palette" to "Uses the active Pulse skin colors"),
                ColorSource.ARTWORK_DRIVEN to ("Artwork-Driven Dynamic Colors" to "Extracts vibrant tones from the current track's album art"),
                ColorSource.MATERIAL_YOU to ("Material You / Wallpaper" to "Derives colors from the Android system wallpaper")
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
                    border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, Pulse.Cyan) else null
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
                            Icon(Icons.Rounded.Check, null, tint = Pulse.Cyan, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeAndLibraryTab(vm: MainViewModel) {
    val sections = remember {
        mutableStateListOf(
            "Quick Picks (Jump Back In)" to true,
            "Recently Played Tracks" to true,
            "Most Played & Top Artists" to true,
            "Moods & Vibes Playlists" to true,
            "Forgotten Favorites" to true,
            "Library Navigation Shortcuts" to true
        )
    }

    val gridCols by vm.albumGridColumns.collectAsStateWithLifecycle()
    val density by vm.listDensity.collectAsStateWithLifecycle()

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "Home Screen Section Arrangement",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Reorder or hide sections on the For You / Home screen.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        items(sections) { item ->
            val (name, enabled) = item
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(name, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = {
                                val idx = sections.indexOf(item)
                                if (idx > 0) {
                                    sections.removeAt(idx)
                                    sections.add(idx - 1, item)
                                }
                            }
                        ) { Text("Up", style = MaterialTheme.typography.labelSmall) }
                        IconButton(
                            onClick = {
                                val idx = sections.indexOf(item)
                                if (idx < sections.size - 1) {
                                    sections.removeAt(idx)
                                    sections.add(idx + 1, item)
                                }
                            }
                        ) { Text("Down", style = MaterialTheme.typography.labelSmall) }
                        Switch(
                            checked = enabled,
                            onCheckedChange = { chk ->
                                val idx = sections.indexOf(item)
                                if (idx >= 0) {
                                    sections[idx] = name to chk
                                }
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = Pulse.Cyan)
                        )
                    }
                }
            }
        }

        item {
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Library Grid & Density",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Configure album grid columns and list item spacing.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
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
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(0 to "Compact List", 1 to "Normal List", 2 to "Comfortable List").forEach { (d, label) ->
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
