package com.pulse.player.ui.buttons

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.SpaceBar
import androidx.compose.material.icons.rounded.TableRows
import androidx.compose.material.icons.rounded.ViewAgenda
import androidx.compose.material.icons.rounded.ViewColumn
import androidx.compose.material.icons.rounded.ViewComfy
import androidx.compose.material.icons.rounded.ViewModule
import androidx.compose.material.icons.rounded.ViewStream
import androidx.compose.material.icons.rounded.ViewWeek
import androidx.compose.material.icons.rounded.WidthFull
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Describes the visual and interaction structure of the playback control area.
 *
 * A button preset defines HOW the same underlying actions are arranged on screen —
 * it never changes WHICH actions are available or what they do. Switching presets
 * must not restart playback (LAY-08 requirement).
 *
 * The 12+ presets fulfill the live button arrangement switcher requirement (§6, BTN-01–BTN-13).
 */
data class ButtonPreset(
    val id: String,
    val name: String,
    val description: String,
    val icon: ImageVector,
    val layout: ButtonLayout
)

/**
 * Structural layout category for button arrangements.
 * These map to different composable implementations in [ButtonArrangement].
 */
enum class ButtonLayout {
    /** Primary play/pause centered, transport controls balanced symmetrically around it. */
    CENTERED,
    /** Transport controls separated into left and right groups with play/pause in center. */
    SPLIT,
    /** Essential transport controls only; secondary actions in an overflow/expand area. */
    MINIMAL,
    /** Progress/waveform occupies its own row below the primary controls. */
    WAVEFORM_BELOW,
    /** Compact queue preview integrated into the control area. */
    QUEUE_INLINE,
    /** Larger controls with all secondary actions visible. */
    EXPANDED,
    /** Reduced vertical footprint for small/short screens. */
    COMPACT,
    /** Visually elevated central play/pause control with drop shadow / card lift. */
    FLOATING,
    /** Mirrored left/right control placement. */
    SYMMETRIC,
    /** Primary action emphasized with secondary controls offset to one side. */
    ASYMMETRIC,
    /** Transport row plus separate secondary action row below. */
    TWO_ROW,
    /** Circular visual arrangement adapted to touch accessibility (accessible alternative to radial). */
    RADIAL_INSPIRED
}

/** The canonical ordered list of all button presets. */
val ButtonPresets: List<ButtonPreset> = listOf(
    ButtonPreset(
        id = "centered",
        name = "Centered",
        description = "Play/pause centered, transport balanced",
        icon = Icons.Rounded.GridView,
        layout = ButtonLayout.CENTERED
    ),
    ButtonPreset(
        id = "split",
        name = "Split",
        description = "Transport separated left and right",
        icon = Icons.Rounded.ViewColumn,
        layout = ButtonLayout.SPLIT
    ),
    ButtonPreset(
        id = "minimal",
        name = "Minimal",
        description = "Essential controls only with overflow",
        icon = Icons.Rounded.SpaceBar,
        layout = ButtonLayout.MINIMAL
    ),
    ButtonPreset(
        id = "waveform_below",
        name = "Waveform Below",
        description = "Waveform row below primary controls",
        icon = Icons.Rounded.ViewStream,
        layout = ButtonLayout.WAVEFORM_BELOW
    ),
    ButtonPreset(
        id = "queue_inline",
        name = "Queue Inline",
        description = "Compact queue preview in control area",
        icon = Icons.Rounded.ViewAgenda,
        layout = ButtonLayout.QUEUE_INLINE
    ),
    ButtonPreset(
        id = "expanded",
        name = "Expanded",
        description = "Larger controls, all actions visible",
        icon = Icons.Rounded.WidthFull,
        layout = ButtonLayout.EXPANDED
    ),
    ButtonPreset(
        id = "compact",
        name = "Compact",
        description = "Reduced vertical footprint",
        icon = Icons.Rounded.ViewComfy,
        layout = ButtonLayout.COMPACT
    ),
    ButtonPreset(
        id = "floating",
        name = "Floating",
        description = "Elevated central play/pause control",
        icon = Icons.Rounded.ViewModule,
        layout = ButtonLayout.FLOATING
    ),
    ButtonPreset(
        id = "symmetric",
        name = "Symmetric",
        description = "Mirrored left/right placement",
        icon = Icons.Rounded.ViewWeek,
        layout = ButtonLayout.SYMMETRIC
    ),
    ButtonPreset(
        id = "asymmetric",
        name = "Asymmetric",
        description = "Primary action emphasized, secondary offset",
        icon = Icons.Rounded.TableRows,
        layout = ButtonLayout.ASYMMETRIC
    ),
    ButtonPreset(
        id = "two_row",
        name = "Two Row",
        description = "Transport row + secondary action row",
        icon = Icons.Rounded.ViewStream,
        layout = ButtonLayout.TWO_ROW
    ),
    ButtonPreset(
        id = "radial_inspired",
        name = "Radial",
        description = "Circular arrangement, touch accessible",
        icon = Icons.Rounded.RadioButtonUnchecked,
        layout = ButtonLayout.RADIAL_INSPIRED
    )
)

object ButtonPresetRegistry {
    private val index = ButtonPresets.associateBy { it.id }

    val default: ButtonPreset get() = ButtonPresets.first { it.id == "centered" }

    /** Returns the preset for [id], or [default] if unknown (no dead buttons). */
    fun fromId(id: String?): ButtonPreset = index[id] ?: default
}
