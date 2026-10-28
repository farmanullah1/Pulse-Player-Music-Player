package com.pulse.player.ui.screens

sealed interface Screen {
    data object Home : Screen
    data object Settings : Screen
    data object Equalizer : Screen
    data object Recap : Screen
    data object SkinTheme : Screen
    data object PlayerTheme : Screen
    data object Backup : Screen
    data object Search : Screen
    data object Duplicates : Screen
    data class Detail(val kind: String, val key: String) : Screen // album / artist / folder / playlist / system
}
