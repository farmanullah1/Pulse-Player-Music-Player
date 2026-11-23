package com.pulse.player.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pulse.player.data.settings.ColorSource
import com.pulse.player.data.settings.FontStylePreset
import com.pulse.player.data.settings.ThemeMode
import com.pulse.player.ui.components.LocalBottomOverlayPadding
import com.pulse.player.ui.theme.LocalDarkTheme
import com.pulse.player.ui.theme.SeedPalettePresets
import com.pulse.player.ui.theme.Skins
import com.pulse.player.ui.theme.TabularMono
import com.pulse.player.ui.viewmodel.MainViewModel
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ColorsAndSkinsTab(
    listState: LazyListState,
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
        state = listState,
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
                        border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
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
                border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
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
internal fun HomeAndLibraryTab(
    listState: LazyListState,
    vm: MainViewModel
) {
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
        state = listState,
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
