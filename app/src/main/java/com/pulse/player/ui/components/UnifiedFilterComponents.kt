package com.pulse.player.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.HighQuality
import androidx.compose.material.icons.rounded.HourglassEmpty
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pulse.player.ui.screens.Screen
import com.pulse.player.ui.viewmodel.EntitySortMode
import com.pulse.player.ui.viewmodel.MainViewModel
import com.pulse.player.ui.viewmodel.SortField

@Composable
fun UnifiedFilterBar(
    vm: MainViewModel,
    modifier: Modifier = Modifier,
    isTrackTab: Boolean = true,
    leadingContent: @Composable RowScope.() -> Unit = {},
    trailingContent: @Composable RowScope.() -> Unit = {}
) {
    val filter by vm.filterState.collectAsStateWithLifecycle()
    val sortField by vm.sortField.collectAsStateWithLifecycle()
    val asc by vm.sortAscending.collectAsStateWithLifecycle()
    val entitySort by vm.entitySortMode.collectAsStateWithLifecycle()

    val sortLabel = if (isTrackTab) {
        "${sortField.label} ${if (asc) "↑" else "↓"}"
    } else {
        entitySort.label
    }

    val chipColors = FilterChipDefaults.filterChipColors(
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        labelColor = MaterialTheme.colorScheme.onSurface,
        iconColor = MaterialTheme.colorScheme.onSurfaceVariant,
        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
        selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimaryContainer
    )
    val chipBorder = FilterChipDefaults.filterChipBorder(
        enabled = true,
        selected = false,
        borderColor = MaterialTheme.colorScheme.outlineVariant,
        selectedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        leadingContent()

        FilterChip(
            selected = filter.activeCount > 0,
            onClick = { vm.openSortFilterSheet() },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Rounded.Tune,
                    contentDescription = "Sort and filter",
                    modifier = Modifier.size(18.dp)
                )
            },
            label = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(sortLabel, style = MaterialTheme.typography.labelLarge)
                    if (filter.activeCount > 0) {
                        Spacer(Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                                .padding(horizontal = 6.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = "${filter.activeCount}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            },
            colors = chipColors,
            border = FilterChipDefaults.filterChipBorder(
                enabled = true,
                selected = filter.activeCount > 0,
                borderColor = MaterialTheme.colorScheme.outlineVariant,
                selectedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
            )
        )

        FilterChip(
            selected = filter.favoritesOnly,
            onClick = { vm.toggleFilterFavorites() },
            leadingIcon = {
                Icon(Icons.Rounded.Favorite, contentDescription = null, modifier = Modifier.size(16.dp))
            },
            label = { Text("Favourites") },
            colors = chipColors,
            border = FilterChipDefaults.filterChipBorder(
                enabled = true,
                selected = filter.favoritesOnly,
                borderColor = MaterialTheme.colorScheme.outlineVariant,
                selectedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
            )
        )

        FilterChip(
            selected = filter.hiResOnly,
            onClick = { vm.toggleFilterHiRes() },
            leadingIcon = {
                Icon(Icons.Rounded.HighQuality, contentDescription = null, modifier = Modifier.size(16.dp))
            },
            label = { Text("Hi-Res") },
            colors = chipColors,
            border = FilterChipDefaults.filterChipBorder(
                enabled = true,
                selected = filter.hiResOnly,
                borderColor = MaterialTheme.colorScheme.outlineVariant,
                selectedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
            )
        )

        FilterChip(
            selected = filter.longOnly,
            onClick = { vm.toggleFilterLong() },
            leadingIcon = {
                Icon(Icons.Rounded.Timer, contentDescription = null, modifier = Modifier.size(16.dp))
            },
            label = { Text("7m+") },
            colors = chipColors,
            border = FilterChipDefaults.filterChipBorder(
                enabled = true,
                selected = filter.longOnly,
                borderColor = MaterialTheme.colorScheme.outlineVariant,
                selectedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
            )
        )

        FilterChip(
            selected = filter.unplayedOnly,
            onClick = { vm.toggleFilterUnplayed() },
            leadingIcon = {
                Icon(Icons.Rounded.HourglassEmpty, contentDescription = null, modifier = Modifier.size(16.dp))
            },
            label = { Text("Unplayed") },
            colors = chipColors,
            border = FilterChipDefaults.filterChipBorder(
                enabled = true,
                selected = filter.unplayedOnly,
                borderColor = MaterialTheme.colorScheme.outlineVariant,
                selectedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
            )
        )

        if (filter.activeCount > 0) {
            FilterChip(
                selected = false,
                onClick = { vm.resetFilters() },
                leadingIcon = {
                    Icon(Icons.Rounded.Close, contentDescription = "Reset filters", modifier = Modifier.size(16.dp))
                },
                label = { Text("Reset") },
                colors = chipColors,
                border = chipBorder
            )
        }

        trailingContent()
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SortFilterBottomSheet(vm: MainViewModel) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val screen by vm.screen.collectAsStateWithLifecycle()
    val activeTab by vm.activeHomeTab.collectAsStateWithLifecycle()
    val sortField by vm.sortField.collectAsStateWithLifecycle()
    val asc by vm.sortAscending.collectAsStateWithLifecycle()
    val entitySort by vm.entitySortMode.collectAsStateWithLifecycle()
    val filter by vm.filterState.collectAsStateWithLifecycle()

    val isEntityMode = screen is Screen.Home &&
        (activeTab.equals("Albums", true) ||
            activeTab.equals("Artists", true) ||
            activeTab.equals("Folders", true) ||
            activeTab.equals("Playlists", true))

    val sheetChipColors = FilterChipDefaults.filterChipColors(
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        labelColor = MaterialTheme.colorScheme.onSurface,
        iconColor = MaterialTheme.colorScheme.onSurfaceVariant,
        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
        selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimaryContainer
    )

    ModalBottomSheet(
        onDismissRequest = { vm.closeSortFilterSheet() },
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Sort & Filter",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (isEntityMode) "Group ordering & track filters" else "Track ordering & quick filters",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = { vm.closeSortFilterSheet() }, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Rounded.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurface)
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            if (isEntityMode) {
                Text(
                    text = "Sort Groups By",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    EntitySortMode.entries.forEach { mode ->
                        FilterChip(
                            selected = entitySort == mode,
                            onClick = { vm.setEntitySortMode(mode) },
                            label = { Text(mode.label) },
                            colors = sheetChipColors
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Sort Songs By",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    OutlinedButton(
                        onClick = { vm.toggleSortOrder() },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = if (asc) Icons.Rounded.ArrowUpward else Icons.Rounded.ArrowDownward,
                            contentDescription = "Sort direction",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(if (asc) "Ascending" else "Descending")
                    }
                }
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SortField.entries.forEach { field ->
                        FilterChip(
                            selected = sortField == field,
                            onClick = { vm.setSortField(field) },
                            label = { Text(field.label) },
                            colors = sheetChipColors
                        )
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Filter Tracks",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                if (filter.activeCount > 0) {
                    TextButton(onClick = { vm.resetFilters() }) {
                        Text("Reset (${filter.activeCount})")
                    }
                }
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = filter.favoritesOnly,
                    onClick = { vm.toggleFilterFavorites() },
                    leadingIcon = { Icon(Icons.Rounded.Favorite, null, Modifier.size(16.dp)) },
                    label = { Text("Favourites Only") },
                    colors = sheetChipColors
                )
                FilterChip(
                    selected = filter.hiResOnly,
                    onClick = { vm.toggleFilterHiRes() },
                    leadingIcon = { Icon(Icons.Rounded.HighQuality, null, Modifier.size(16.dp)) },
                    label = { Text("Lossless / Hi-Res") },
                    colors = sheetChipColors
                )
                FilterChip(
                    selected = filter.longOnly,
                    onClick = { vm.toggleFilterLong() },
                    leadingIcon = { Icon(Icons.Rounded.Timer, null, Modifier.size(16.dp)) },
                    label = { Text("Long (7 min+)") },
                    colors = sheetChipColors
                )
                FilterChip(
                    selected = filter.unplayedOnly,
                    onClick = { vm.toggleFilterUnplayed() },
                    leadingIcon = { Icon(Icons.Rounded.HourglassEmpty, null, Modifier.size(16.dp)) },
                    label = { Text("Unplayed") },
                    colors = sheetChipColors
                )
            }

            Spacer(Modifier.height(4.dp))

            Button(
                onClick = { vm.closeSortFilterSheet() },
                modifier = Modifier.fillMaxWidth(),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text("Done", fontWeight = FontWeight.Bold)
            }
        }
    }
}
