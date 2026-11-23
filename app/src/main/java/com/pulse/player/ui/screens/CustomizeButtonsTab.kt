package com.pulse.player.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pulse.player.ui.buttons.ButtonPresets
import com.pulse.player.ui.components.LocalBottomOverlayPadding

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ButtonsTab(
    listState: LazyListState,
    currentId: String,
    placedButtons: List<String>,
    onSelect: (String) -> Unit,
    onUpdateOrder: (List<String>) -> Unit,
    recommendation: String?,
    onRecommend: (goal: String, presetId: String) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }

    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = LocalBottomOverlayPadding.current + 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
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
                        FilterChip(selected = currentId == "minimal", onClick = { onRecommend("Minimal", "minimal") }, label = { Text("Minimal") })
                        FilterChip(selected = currentId == "thumb_zone", onClick = { onRecommend("One-Handed", "thumb_zone") }, label = { Text("One-Handed") })
                        FilterChip(selected = currentId == "queue_inline", onClick = { onRecommend("Queue", "queue_inline") }, label = { Text("Queue") })
                        FilterChip(selected = currentId == "expanded", onClick = { onRecommend("Accessible", "expanded") }, label = { Text("Accessible") })
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
internal fun ElementsTab(
    listState: LazyListState,
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
        state = listState,
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
