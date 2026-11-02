package com.pulse.player.ui.screens

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DashboardCustomize
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Splitscreen
import androidx.compose.ui.graphics.vector.ImageVector
import com.pulse.player.ui.themes.PlayerLayout

/**
 * Stable identifiers for layouts shown by the in-player layout picker.
 *
 * The existing theme registry remains the source of persistence. This small
 * model gives screen code and tests a stable, descriptive API for the modes
 * that can be changed while playback continues.
 */
enum class PlayerLayoutMode(
    val id: String,
    val displayName: String,
    val subtitle: String,
    val icon: ImageVector,
    val layout: PlayerLayout
) {
    CLASSIC(
        id = "classic",
        displayName = "Classic",
        subtitle = "Artwork, controls & progress",
        icon = Icons.Rounded.GridView,
        layout = PlayerLayout.CLASSIC
    ),
    MOUNTAIN("mountain", "Mountain Horizon", "Warm sunset artwork", Icons.Rounded.GridView, PlayerLayout.SHEET_QUEUE),
    MESH("mesh", "Mesh Glass", "Magenta glass atmosphere", Icons.Rounded.GridView, PlayerLayout.MESH_GLASS),
    MINIMAL("minimal", "Minimal Mood", "Focused dark controls", Icons.Rounded.GridView, PlayerLayout.MINIMAL_DARK),
    ACOUSTIC("acoustic", "Acoustic", "Warm disc artwork", Icons.Rounded.GridView, PlayerLayout.ACOUSTIC),
    VINYL("vinyl", "Vinyl", "Rotating record artwork", Icons.Rounded.GridView, PlayerLayout.VINYL),
    COAST("coast", "Coastal Ocean", "Layered wave atmosphere", Icons.Rounded.GridView, PlayerLayout.COAST),
    STARRY("starry", "Starry Sky", "Night sky artwork", Icons.Rounded.GridView, PlayerLayout.STARRY),
    IMMERSIVE("immersive", "Immersive Artwork Player", "Blurred artwork queue", Icons.Rounded.GridView, PlayerLayout.IMMERSIVE),
    PIXEL(
        id = "pixel",
        displayName = "Pixel Expressive",
        subtitle = "Material You controls & wavy seekbar",
        icon = Icons.Rounded.DashboardCustomize,
        layout = PlayerLayout.PIXEL
    ),
    NAMIDA(
        id = "namida",
        displayName = "Namida Waveform",
        subtitle = "Waveform seekbar & amber details",
        icon = Icons.Rounded.DashboardCustomize,
        layout = PlayerLayout.NAMIDA
    ),
    ATMOSPHERIC_QUEUE(
        id = "atmospheric_queue",
        displayName = "Atmospheric Queue",
        subtitle = "Dark mist overlay & light queue card",
        icon = Icons.Rounded.Splitscreen,
        layout = PlayerLayout.ATMOSPHERIC_QUEUE
    );

    companion object {
        /** New installations open with the premier split queue mode. */
        fun fromId(id: String?): PlayerLayoutMode =
            entries.firstOrNull { it.id == id } ?: ATMOSPHERIC_QUEUE
    }
}
