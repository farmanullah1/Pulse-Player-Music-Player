package com.pulse.player.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pulse.player.ui.theme.Pulse
import com.pulse.player.ui.viewmodel.MainViewModel
import kotlinx.coroutines.launch

val homeTabs = listOf("For you", "Songs", "Playlists", "Folders", "Albums", "Artists")

@Composable
fun HomeTabsScreen(
    vm: MainViewModel,
    onMenu: () -> Unit,
    showMenu: Boolean
) {
    val pager = rememberPagerState { homeTabs.size }
    val scope = rememberCoroutineScope()

    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (showMenu) {
                IconButton(onClick = onMenu) {
                    Icon(Icons.Rounded.Menu, "Open navigation menu", tint = Color.White)
                }
            }
            ScrollableTabRow(
                selectedTabIndex = pager.currentPage,
                modifier = Modifier.weight(1f),
                containerColor = Color.Transparent,
                edgePadding = 8.dp,
                divider = {},
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[pager.currentPage]),
                        color = Pulse.Cyan
                    )
                }
            ) {
                homeTabs.forEachIndexed { index, tabTitle ->
                    Tab(
                        selected = pager.currentPage == index,
                        onClick = { scope.launch { pager.animateScrollToPage(index) } },
                        text = {
                            Text(
                                text = tabTitle,
                                fontWeight = if (pager.currentPage == index) FontWeight.Bold else FontWeight.Medium,
                                color = if (pager.currentPage == index) Color.White else Pulse.TextMid
                            )
                        }
                    )
                }
            }

            IconButton(onClick = { vm.go(Screen.SkinTheme) }) {
                Icon(Icons.Rounded.AutoAwesome, "Skin theme", tint = Color.White)
            }
            IconButton(onClick = { vm.go(Screen.Search) }) {
                Icon(Icons.Rounded.Search, "Search", tint = Color.White)
            }
            IconButton(onClick = { vm.go(Screen.Settings) }) {
                Icon(Icons.Rounded.Settings, "Settings", tint = Color.White)
            }
        }

        HorizontalPager(
            state = pager,
            modifier = Modifier.weight(1f),
            beyondViewportPageCount = 1,
            key = { homeTabs[it] }
        ) { page ->
            when (page) {
                0 -> ForYouScreen(vm)
                1 -> SongsScreen(vm)
                2 -> PlaylistsScreen(vm)
                3 -> FoldersScreen(vm)
                4 -> AlbumsScreen(vm)
                else -> ArtistsScreen(vm)
            }
        }
    }
}
