package com.pulse.player.ui.buttons

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pulse.player.ui.theme.LocalPulseTokens

/**
 * Bottom sheet picker for the live button arrangement switcher.
 *
 * Opens from the player or Settings → Player → Button layout.
 * Switching a preset applies immediately without restarting playback (BTN-14).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ButtonPresetPickerSheet(
    current: ButtonPreset,
    onSelect: (ButtonPreset) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    val tokens = LocalPulseTokens.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Button Layout",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                TextButton(
                    onClick = { onSelect(ButtonPresetRegistry.default); onDismiss() }
                ) {
                    Text("Reset")
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Choose how playback controls are arranged. Music keeps playing.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(16.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(ButtonPresets, key = { it.id }) { preset ->
                    ButtonPresetCard(
                        preset = preset,
                        isSelected = preset.id == current.id,
                        onClick = { onSelect(preset) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ButtonPresetCard(
    preset: ButtonPreset,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val tokens = LocalPulseTokens.current
    val shape = RoundedCornerShape(16.dp)
    val borderColor = if (isSelected) tokens.accent else tokens.outline.copy(alpha = 0.5f)
    val bgColor = if (isSelected) tokens.accent.copy(alpha = 0.10f) else tokens.bgCard

    Column(
        modifier = Modifier
            .clip(shape)
            .background(bgColor)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = borderColor,
                shape = shape
            )
            .clickable(onClick = onClick)
            .padding(12.dp)
            .semantics { contentDescription = "${preset.name} button layout${if (isSelected) ", selected" else ""}" },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(contentAlignment = Alignment.TopEnd, modifier = Modifier.fillMaxWidth()) {
            Icon(
                imageVector = preset.icon,
                contentDescription = null,
                tint = if (isSelected) tokens.accent else tokens.textSecondary,
                modifier = Modifier.size(28.dp).align(Alignment.Center)
            )
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .clip(RoundedCornerShape(50))
                        .background(tokens.accent),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Rounded.Check,
                        contentDescription = null,
                        tint = androidx.compose.ui.graphics.Color.Black,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = preset.name,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) tokens.accent else MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = preset.description,
            style = MaterialTheme.typography.labelSmall,
            color = tokens.textSecondary
        )
    }
}
