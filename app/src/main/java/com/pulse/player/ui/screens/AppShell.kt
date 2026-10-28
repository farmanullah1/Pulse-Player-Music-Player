package com.pulse.player.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Checkroom
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pulse.player.ui.components.MiniPlayer
import com.pulse.player.ui.theme.AppBackground
import com.pulse.player.ui.theme.Pulse
import com.pulse.player.ui.viewmodel.MainViewModel
import kotlinx.coroutines.launch

@Composable
fun AppShell(vm: MainViewModel, windowSize: WindowSizeClass) {
    val expandedPlayer by vm.nowPlayingOpen.collectAsStateWithLifecycle()
    val screen by vm.screen.collectAsStateWithLifecycle()
    val ps by vm.player.state.collectAsStateWithLifecycle()
    val wide = windowSize.widthSizeClass != WindowWidthSizeClass.Compact
    val dock = windowSize.widthSizeClass == WindowWidthSizeClass.Expanded
    val drawer = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val themeMode by vm.themeMode.collectAsStateWithLifecycle()
    val wallpaperPath by vm.wallpaperPath.collectAsStateWithLifecycle()

    // Sheet states
    val queueOpen by vm.queueOpen.collectAsStateWithLifecycle()
    val sleepOpen by vm.sleepOpen.collectAsStateWithLifecycle()
    val addToPlaylistTrack by vm.addToPlaylistOpen.collectAsStateWithLifecycle()
    val trackMenuTrack by vm.trackMenuTrack.collectAsStateWithLifecycle()
    val createPlaylistOpen by vm.createPlaylistOpen.collectAsStateWithLifecycle()
    val importPreview by vm.importPreview.collectAsStateWithLifecycle()
    val songDetailsTrack by vm.songDetailsTrack.collectAsStateWithLifecycle()

    BackHandler(expandedPlayer) { vm.closeNowPlaying() }
    BackHandler(!expandedPlayer && screen !is Screen.Home) { vm.back() }

    AppBackground(themeMode, wallpaperPath) {
        Row(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
        ) {
            if (wide) {
                PulseRail(screen, vm::go)
            }

            Box(Modifier.weight(1f)) {
                ModalNavigationDrawer(
                    drawerContent = {
                        PulseDrawer(vm::go) { scope.launch { drawer.close() } }
                    },
                    drawerState = drawer,
                    gesturesEnabled = !wide
                ) {
                    AnimatedContent(
                        targetState = screen,
                        transitionSpec = {
                            (fadeIn(tween(240)) + slideInHorizontally(tween(240)) { it / 8 })
                                .togetherWith(fadeOut(tween(160)))
                        },
                        label = "screen"
                    ) { s ->
                        when (s) {
                            Screen.Home -> HomeTabsScreen(
                                vm = vm,
                                onMenu = { scope.launch { drawer.open() } },
                                showMenu = !wide
                            )
                            Screen.Settings -> SettingsScreen(vm)
                            Screen.Equalizer -> EqualizerScreen(vm)
                            Screen.Recap -> RecapScreen(vm)
                            Screen.SkinTheme -> SkinThemeScreen(vm)
                            Screen.PlayerTheme -> PlayerThemeScreen(vm)
                            Screen.Backup -> BackupScreen(vm)
                            Screen.Search -> SearchScreen(vm)
                            Screen.Duplicates -> DuplicatesScreen(vm)
                            is Screen.Detail -> DetailScreen(vm, s)
                        }
                    }
                }

                if (ps.mediaId != null && !dock) {
                    MiniPlayer(
                        s = ps,
                        pos = vm.player.position,
                        onToggle = vm.player::togglePlay,
                        onQueue = { vm.openQueue() },
                        onExpand = { vm.openNowPlaying() },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .navigationBarsPadding()
                            .padding(bottom = 8.dp)
                    )
                }
            }

            if (dock && ps.mediaId != null) {
                NowPlayingPanel(
                    vm = vm,
                    modifier = Modifier
                        .width(380.dp)
                        .fillMaxHeight()
                )
            }
        }

        AnimatedVisibility(
            visible = expandedPlayer && !dock,
            enter = slideInVertically(tween(300)) { it } + fadeIn(),
            exit = slideOutVertically(tween(260)) { it } + fadeOut()
        ) {
            NowPlayingScreen(vm)
        }

        // Active Bottom Sheets and Dialogs
        if (queueOpen) {
            QueueSheet(vm)
        }
        if (sleepOpen) {
            SleepSheet(vm)
        }
        addToPlaylistTrack?.let { track ->
            AddToPlaylistSheet(vm, track)
        }
        trackMenuTrack?.let { track ->
            TrackMenuSheet(vm, track)
        }
        if (createPlaylistOpen) {
            CreatePlaylistDialog(vm)
        }
        importPreview?.let { preview ->
            ImportPreviewDialog(vm, preview)
        }
        songDetailsTrack?.let { track ->
            SongDetailsDialog(track) { vm.songDetailsTrack.value = null }
        }
    }
}

@Composable
fun PulseDrawer(
    onNavigate: (Screen) -> Unit,
    onClose: () -> Unit
) {
    ModalDrawerSheet(
        drawerContainerColor = Pulse.Surface,
        drawerShape = RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp)
    ) {
        Column(
            Modifier
                .width(300.dp)
                .fillMaxHeight()
                .padding(20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.MusicNote, null, tint = Pulse.Cyan, modifier = Modifier.size(32.dp))
                Spacer(Modifier.width(12.dp))
                Text(
                    "Pulse Player",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            Spacer(Modifier.height(24.dp))

            val items = listOf(
                DrawerItemData("Home", Icons.Rounded.Home, Screen.Home),
                DrawerItemData("Equalizer", Icons.Rounded.Equalizer, Screen.Equalizer),
                DrawerItemData("Sleep Timer", Icons.Rounded.Bedtime, null) { /* opens sheet */ },
                DrawerItemData("Monthly Recap", Icons.Rounded.History, Screen.Recap),
                DrawerItemData("Favourites", Icons.Rounded.Favorite, Screen.Detail("system", "favorites")),
                DrawerItemData("Backup & Transfer", Icons.Rounded.Backup, Screen.Backup),
                DrawerItemData("Skin Theme", Icons.Rounded.Checkroom, Screen.SkinTheme),
                DrawerItemData("Player Theme", Icons.Rounded.AutoAwesome, Screen.PlayerTheme),
                DrawerItemData("Settings", Icons.Rounded.Settings, Screen.Settings)
            )

            items.forEach { item ->
                NavigationDrawerItem(
                    label = { Text(item.label, color = Color.White) },
                    icon = { Icon(item.icon, null, tint = Pulse.Cyan) },
                    selected = false,
                    onClick = {
                        onClose()
                        item.screen?.let(onNavigate) ?: item.customAction?.invoke()
                    },
                    colors = NavigationDrawerItemDefaults.colors(
                        unselectedContainerColor = Color.Transparent
                    )
                )
            }
        }
    }
}

data class DrawerItemData(
    val label: String,
    val icon: ImageVector,
    val screen: Screen?,
    val customAction: (() -> Unit)? = null
)

@Composable
fun PulseRail(
    currentScreen: Screen,
    onNavigate: (Screen) -> Unit
) {
    NavigationRail(
        containerColor = Pulse.Surface,
        contentColor = Color.White
    ) {
        Spacer(Modifier.height(16.dp))
        NavigationRailItem(
            selected = currentScreen is Screen.Home,
            onClick = { onNavigate(Screen.Home) },
            icon = { Icon(Icons.Rounded.Home, "Home") },
            label = { Text("Home") },
            colors = NavigationRailItemDefaults.colors(selectedIconColor = Pulse.Cyan, selectedTextColor = Pulse.Cyan)
        )
        NavigationRailItem(
            selected = currentScreen is Screen.Search,
            onClick = { onNavigate(Screen.Search) },
            icon = { Icon(Icons.Rounded.Search, "Search") },
            label = { Text("Search") },
            colors = NavigationRailItemDefaults.colors(selectedIconColor = Pulse.Cyan, selectedTextColor = Pulse.Cyan)
        )
        NavigationRailItem(
            selected = currentScreen is Screen.Equalizer,
            onClick = { onNavigate(Screen.Equalizer) },
            icon = { Icon(Icons.Rounded.Equalizer, "Equalizer") },
            label = { Text("EQ") },
            colors = NavigationRailItemDefaults.colors(selectedIconColor = Pulse.Cyan, selectedTextColor = Pulse.Cyan)
        )
        NavigationRailItem(
            selected = currentScreen is Screen.PlayerTheme,
            onClick = { onNavigate(Screen.PlayerTheme) },
            icon = { Icon(Icons.Rounded.AutoAwesome, "Themes") },
            label = { Text("Themes") },
            colors = NavigationRailItemDefaults.colors(selectedIconColor = Pulse.Cyan, selectedTextColor = Pulse.Cyan)
        )
        NavigationRailItem(
            selected = currentScreen is Screen.Settings,
            onClick = { onNavigate(Screen.Settings) },
            icon = { Icon(Icons.Rounded.Settings, "Settings") },
            label = { Text("Settings") },
            colors = NavigationRailItemDefaults.colors(selectedIconColor = Pulse.Cyan, selectedTextColor = Pulse.Cyan)
        )
    }
}
