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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.BrightnessAuto
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.Contrast
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.DashboardCustomize
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.FolderOff
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pulse.player.BuildConfig
import com.pulse.player.data.ErrorDispatcher
import com.pulse.player.data.settings.ThemeMode
import com.pulse.player.ui.components.AudioControlsSheet
import com.pulse.player.ui.components.BatchTagEditorSheet
import com.pulse.player.ui.components.DiagnosticsDialog
import com.pulse.player.ui.components.LocalBottomOverlayPadding
import com.pulse.player.ui.components.MiniPlayer
import com.pulse.player.ui.components.PlayerLayoutBottomSheet
import com.pulse.player.ui.components.SortFilterBottomSheet
import com.pulse.player.ui.components.TagEditorSheet
import com.pulse.player.ui.components.formatTime
import com.pulse.player.ui.theme.AppBackground
import com.pulse.player.ui.theme.TabularMono
import com.pulse.player.ui.viewmodel.MainViewModel
import kotlinx.coroutines.launch

@Composable
fun AppShell(vm: MainViewModel, windowSize: WindowSizeClass) {
    val expandedPlayer by vm.nowPlayingOpen.collectAsStateWithLifecycle()
    val screen by vm.screen.collectAsStateWithLifecycle()
    val activeHomeTab by vm.activeHomeTab.collectAsStateWithLifecycle()
    val ps by vm.player.state.collectAsStateWithLifecycle()
    val wide = windowSize.widthSizeClass != WindowWidthSizeClass.Compact
    val dock = windowSize.widthSizeClass == WindowWidthSizeClass.Expanded
    val drawer = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val themeMode by vm.themeMode.collectAsStateWithLifecycle()
    val wallpaperPath by vm.wallpaperPath.collectAsStateWithLifecycle()
    val artworkAccent by vm.artworkAccent.collectAsStateWithLifecycle()
    val isCurrentFavorite by vm.isCurrentFavorite.collectAsStateWithLifecycle()
    val excludedFolders by vm.excludedFolders.collectAsStateWithLifecycle()
    val hiddenFavorites by vm.hiddenFavorites.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val diagnosticsOpen by vm.diagnosticsOpen.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        ErrorDispatcher.events.collect { err ->
            val result = snackbarHostState.showSnackbar(
                message = "${err.title}: ${err.message}",
                actionLabel = if (err.actionable) "Diagnostics" else null,
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) {
                vm.openDiagnostics()
            }
        }
    }

    val snackbarMsg by vm.snackbarMessage.collectAsStateWithLifecycle()
    LaunchedEffect(snackbarMsg) {
        val msg = snackbarMsg
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(message = msg, duration = SnackbarDuration.Short)
            vm.clearSnackbarMessage()
        }
    }

    // Sheet states
    val queueOpen by vm.queueOpen.collectAsStateWithLifecycle()
    val sleepOpen by vm.sleepOpen.collectAsStateWithLifecycle()
    val audioControlsOpen by vm.audioControlsOpen.collectAsStateWithLifecycle()
    val sortFilterSheetOpen by vm.sortFilterSheetOpen.collectAsStateWithLifecycle()
    val addToPlaylistTrack by vm.addToPlaylistOpen.collectAsStateWithLifecycle()
    val trackMenuTrack by vm.trackMenuTrack.collectAsStateWithLifecycle()
    val createPlaylistOpen by vm.createPlaylistOpen.collectAsStateWithLifecycle()
    val importPreview by vm.importPreview.collectAsStateWithLifecycle()
    val songDetailsTrack by vm.songDetailsTrack.collectAsStateWithLifecycle()
    val tagEditorTrack by vm.tagEditorTrack.collectAsStateWithLifecycle()
    val batchTagTracks by vm.batchTagTracksOpen.collectAsStateWithLifecycle()
    val playerLayoutPickerOpen by vm.playerLayoutPickerOpen.collectAsStateWithLifecycle()

    BackHandler(expandedPlayer) { vm.closeNowPlaying() }
    BackHandler(!expandedPlayer && screen !is Screen.Home) { vm.back() }

    val showBottomBar = !wide && screen is Screen.Home
    val hasMiniPlayer = ps.mediaId != null && !dock
    val navBarBottomPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val bottomOverlayPadding = 16.dp + (if (showBottomBar || hasMiniPlayer) 0.dp else navBarBottomPadding)

    AppBackground(themeMode, wallpaperPath = wallpaperPath) {
        Row(Modifier.fillMaxSize()) {
            if (wide) {
                PulseRail(screen, vm::go)
            }

            Column(Modifier.weight(1f).fillMaxHeight()) {
                Box(Modifier.weight(1f).fillMaxWidth()) {
                    CompositionLocalProvider(LocalBottomOverlayPadding provides bottomOverlayPadding) {
                        ModalNavigationDrawer(
                            drawerContent = {
                                PulseDrawer(
                                    currentScreen = screen,
                                    activeHomeTab = activeHomeTab,
                                    themeMode = themeMode,
                                    sleepRemainingMs = ps.sleepRemainingMs,
                                    excludedFolderCount = excludedFolders.size,
                                    hiddenFavoriteCount = hiddenFavorites.size,
                                    onThemeMode = vm::setThemeMode,
                                    onSelectHomeTab = { tab ->
                                        vm.selectHomeTab(tab)
                                        vm.go(Screen.Home)
                                    },
                                    onNavigate = vm::go,
                                    onSleepTimer = vm::openSleepSheet,
                                    onAudioControls = vm::openAudioControls,
                                    onDiagnostics = vm::openDiagnostics
                                ) { scope.launch { drawer.close() } }
                            },
                            drawerState = drawer,
                            gesturesEnabled = !wide
                        ) {
                            AnimatedContent(
                                targetState = screen,
                                transitionSpec = {
                                    (fadeIn(tween(220)) + slideInHorizontally(tween(220)) { it / 10 })
                                        .togetherWith(fadeOut(tween(150)))
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
                                    Screen.ExcludedFolders -> ExcludedFoldersScreen(vm)
                                    Screen.Transcribe -> TranscribeScreen(vm)
                                    Screen.GenerateMusic -> GenerateMusicScreen(vm)
                                    Screen.Customize -> PlayerCustomizeScreen(vm)
                                    is Screen.Detail -> DetailScreen(vm, s)
                                }
                            }
                        }
                    }

                    SnackbarHost(
                        hostState = snackbarHostState,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .then(if (showBottomBar || hasMiniPlayer) Modifier else Modifier.navigationBarsPadding())
                            .padding(bottom = 12.dp)
                    ) { data ->
                        Snackbar(
                            snackbarData = data,
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            contentColor = MaterialTheme.colorScheme.onSurface,
                            actionColor = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(14.dp)
                        )
                    }
                }

                if (hasMiniPlayer) {
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .then(if (showBottomBar) Modifier else Modifier.navigationBarsPadding())
                        ) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            MiniPlayer(
                                s = ps,
                                pos = vm.player.position,
                                onToggle = vm.player::togglePlay,
                                onNext = vm.player::next,
                                onPrevious = vm.player::previous,
                                onQueue = { vm.openQueue() },
                                onExpand = { vm.openNowPlaying() },
                                artworkAccent = artworkAccent,
                                isFavorite = isCurrentFavorite,
                                onToggleFavorite = vm::toggleFavoriteCurrent,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    }
                }

                if (showBottomBar) {
                    PulseBottomBar(
                        activeTab = activeHomeTab,
                        showTopDivider = !hasMiniPlayer,
                        onSelectTab = vm::selectHomeTab,
                        onOpenMore = { scope.launch { drawer.open() } }
                    )
                }
            }

            if (dock && ps.mediaId != null) {
                NowPlayingPanel(
                    vm = vm,
                    modifier = Modifier
                        .widthIn(min = 320.dp, max = 420.dp)
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
        if (audioControlsOpen) {
            AudioControlsSheet(vm)
        }
        if (sortFilterSheetOpen) {
            SortFilterBottomSheet(vm)
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
        tagEditorTrack?.let { track ->
            TagEditorSheet(
                track = track,
                vm = vm,
                onDismiss = vm::closeTagEditor
            )
        }
        batchTagTracks?.let { targets ->
            BatchTagEditorSheet(
                tracks = targets,
                vm = vm,
                onDismiss = vm::closeBatchTagEditor
            )
        }
        if (playerLayoutPickerOpen) {
            PlayerLayoutBottomSheet(vm)
        }
        if (diagnosticsOpen) {
            DiagnosticsDialog(vm = vm, onDismiss = vm::closeDiagnostics)
        }
    }
}

@Composable
private fun PulseBottomBar(
    activeTab: String,
    showTopDivider: Boolean = true,
    onSelectTab: (String) -> Unit,
    onOpenMore: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        if (showTopDivider) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
        }
        NavigationBar(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
            tonalElevation = 0.dp
        ) {
            val navItems = listOf(
                Triple("For you", "For you", Icons.Rounded.Home),
                Triple("Songs", "Songs", Icons.Rounded.LibraryMusic),
                Triple("Playlists", "Playlists", Icons.Rounded.QueueMusic),
                Triple("Albums", "Albums", Icons.Rounded.Album)
            )
            navItems.forEach { (tabKey, label, icon) ->
                val selected = activeTab.equals(tabKey, ignoreCase = true)
                NavigationBarItem(
                    selected = selected,
                    onClick = { onSelectTab(tabKey) },
                    icon = { Icon(icon, contentDescription = label) },
                    label = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
            val isSecondaryHomeTab = navItems.none { (tabKey, _, _) -> activeTab.equals(tabKey, ignoreCase = true) }
            NavigationBarItem(
                selected = isSecondaryHomeTab,
                onClick = onOpenMore,
                icon = { Icon(Icons.Rounded.Menu, contentDescription = "More options") },
                label = { Text("More", maxLines = 1) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}

@Composable
fun PulseDrawer(
    currentScreen: Screen,
    activeHomeTab: String = "For you",
    themeMode: ThemeMode,
    sleepRemainingMs: Long = 0L,
    excludedFolderCount: Int = 0,
    hiddenFavoriteCount: Int = 0,
    onThemeMode: (ThemeMode) -> Unit,
    onSelectHomeTab: (String) -> Unit = {},
    onNavigate: (Screen) -> Unit,
    onSleepTimer: () -> Unit,
    onAudioControls: () -> Unit = {},
    onDiagnostics: () -> Unit = {},
    onClose: () -> Unit
) {
    ModalDrawerSheet(
        drawerContainerColor = MaterialTheme.colorScheme.surface,
        drawerContentColor = MaterialTheme.colorScheme.onSurface,
        drawerShape = RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp)
    ) {
        Column(
            Modifier
                .width(304.dp)
                .fillMaxHeight()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 16.dp)
        ) {
            // Brand header with pinned 1-tap Settings button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Rounded.MusicNote,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        "Pulse Player",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        "Local Hi-Fi Audio",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(
                    onClick = {
                        onClose()
                        onNavigate(Screen.Settings)
                    },
                    modifier = Modifier.size(48.dp),
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = if (currentScreen == Screen.Settings) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerHigh
                        },
                        contentColor = if (currentScreen == Screen.Settings) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                    )
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Settings,
                        contentDescription = "Open settings",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Quick Appearance Switcher (System / Light / Dark / AMOLED)
            Text(
                "Appearance",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val options = listOf(
                    Triple(ThemeMode.SYSTEM, "Auto", Icons.Rounded.BrightnessAuto),
                    Triple(ThemeMode.LIGHT, "Light", Icons.Rounded.LightMode),
                    Triple(ThemeMode.DARK, "Dark", Icons.Rounded.DarkMode),
                    Triple(ThemeMode.AMOLED, "AMOLED", Icons.Rounded.Contrast)
                )
                options.forEach { (mode, label, icon) ->
                    val selected = themeMode == mode
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (selected) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceContainerHigh
                            )
                            .clickable { onThemeMode(mode) }
                            .padding(vertical = 8.dp, horizontal = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(
                            icon,
                            contentDescription = label,
                            tint = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            val sleepBadge = if (sleepRemainingMs > 0L) formatTime(sleepRemainingMs) else null
            val excludedBadge = when {
                hiddenFavoriteCount > 0 -> "$excludedFolderCount · ${hiddenFavoriteCount}!"
                excludedFolderCount > 0 -> "$excludedFolderCount"
                else -> null
            }

            val drawerGroups: List<Pair<String, List<DrawerItemData>>> = buildList {
                add(
                    "LIBRARY" to listOf(
                        DrawerItemData("Home", Icons.Rounded.Home, Screen.Home) { onSelectHomeTab("For you") },
                        DrawerItemData("Favourites", Icons.Rounded.Favorite, Screen.Detail("system", "favorites")),
                        DrawerItemData("Folders", Icons.Rounded.Folder, null, selectedTab = "Folders") { onSelectHomeTab("Folders") },
                        DrawerItemData("Artists", Icons.Rounded.Person, null, selectedTab = "Artists") { onSelectHomeTab("Artists") },
                        DrawerItemData("Excluded Folders", Icons.Rounded.FolderOff, Screen.ExcludedFolders, badge = excludedBadge),
                        DrawerItemData("Duplicate Finder", Icons.Rounded.ContentCopy, Screen.Duplicates)
                    )
                )
                add(
                    "SOUND & PLAYBACK" to listOf(
                        DrawerItemData("Equalizer", Icons.Rounded.Equalizer, Screen.Equalizer),
                        DrawerItemData("Audio & Loop Controls", Icons.Rounded.Speed, null) { onAudioControls() },
                        DrawerItemData("Sleep Timer", Icons.Rounded.Bedtime, null, badge = sleepBadge) { onSleepTimer() }
                    )
                )
                val appearanceItems = buildList {
                    add(DrawerItemData("Customize Player", Icons.Rounded.DashboardCustomize, Screen.Customize))
                    add(DrawerItemData("Player Theme", Icons.Rounded.AutoAwesome, Screen.PlayerTheme))
                    add(DrawerItemData("Skin Theme", Icons.Rounded.Palette, Screen.SkinTheme))
                    add(DrawerItemData("Monthly Recap", Icons.Rounded.History, Screen.Recap))
                    add(DrawerItemData("Backup & Transfer", Icons.Rounded.Backup, Screen.Backup))
                    if (BuildConfig.NETWORK_FEATURES_INCLUDED) {
                        add(DrawerItemData("Voice & Lyrics", Icons.Rounded.Mic, Screen.Transcribe))
                        add(DrawerItemData("Music Studio", Icons.Rounded.AutoAwesome, Screen.GenerateMusic))
                    }
                }
                add("PERSONALIZE & TOOLS" to appearanceItems)
                add(
                    "SYSTEM" to listOf(
                        DrawerItemData("Diagnostics & Log", Icons.Rounded.Build, null) { onDiagnostics() },
                        DrawerItemData("Settings", Icons.Rounded.Settings, Screen.Settings)
                    )
                )
            }

            drawerGroups.forEach { (groupTitle, groupItems) ->
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f),
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                Text(
                    text = groupTitle,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )
                groupItems.forEach { item ->
                    val isSelected = when {
                        item.selectedTab != null -> currentScreen is Screen.Home && activeHomeTab.equals(item.selectedTab, ignoreCase = true)
                        item.screen == Screen.Home -> currentScreen is Screen.Home && !activeHomeTab.equals("Folders", true) && !activeHomeTab.equals("Artists", true)
                        item.screen != null -> currentScreen == item.screen
                        else -> false
                    }
                    NavigationDrawerItem(
                        label = { Text(item.label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        icon = { Icon(item.icon, null) },
                        badge = item.badge?.let { b ->
                            {
                                Text(
                                    text = b,
                                    style = MaterialTheme.typography.labelSmall.merge(TabularMono),
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        },
                        selected = isSelected,
                        onClick = {
                            onClose()
                            if (item.customAction != null) {
                                item.customAction.invoke()
                            } else {
                                item.screen?.let(onNavigate)
                            }
                        },
                        colors = NavigationDrawerItemDefaults.colors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedTextColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            unselectedContainerColor = Color.Transparent,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurface,
                            unselectedIconColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }
        }
    }
}

data class DrawerItemData(
    val label: String,
    val icon: ImageVector,
    val screen: Screen?,
    val badge: String? = null,
    val selectedTab: String? = null,
    val customAction: (() -> Unit)? = null
)

@Composable
fun PulseRail(
    currentScreen: Screen,
    onNavigate: (Screen) -> Unit
) {
    NavigationRail(
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Spacer(Modifier.height(16.dp))
        NavigationRailItem(
            selected = currentScreen is Screen.Home,
            onClick = { onNavigate(Screen.Home) },
            icon = { Icon(Icons.Rounded.Home, "Home") },
            label = { Text("Home") },
            colors = NavigationRailItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.primary,
                selectedTextColor = MaterialTheme.colorScheme.primary
            )
        )
        NavigationRailItem(
            selected = currentScreen is Screen.Search,
            onClick = { onNavigate(Screen.Search) },
            icon = { Icon(Icons.Rounded.Search, "Search") },
            label = { Text("Search") },
            colors = NavigationRailItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.primary,
                selectedTextColor = MaterialTheme.colorScheme.primary
            )
        )
        NavigationRailItem(
            selected = currentScreen is Screen.Equalizer,
            onClick = { onNavigate(Screen.Equalizer) },
            icon = { Icon(Icons.Rounded.Equalizer, "Equalizer") },
            label = { Text("EQ") },
            colors = NavigationRailItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.primary,
                selectedTextColor = MaterialTheme.colorScheme.primary
            )
        )
        NavigationRailItem(
            selected = currentScreen is Screen.PlayerTheme,
            onClick = { onNavigate(Screen.PlayerTheme) },
            icon = { Icon(Icons.Rounded.AutoAwesome, "Themes") },
            label = { Text("Themes") },
            colors = NavigationRailItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.primary,
                selectedTextColor = MaterialTheme.colorScheme.primary
            )
        )
        NavigationRailItem(
            selected = currentScreen is Screen.Settings,
            onClick = { onNavigate(Screen.Settings) },
            icon = { Icon(Icons.Rounded.Settings, "Settings") },
            label = { Text("Settings") },
            colors = NavigationRailItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.primary,
                selectedTextColor = MaterialTheme.colorScheme.primary
            )
        )
    }
}
