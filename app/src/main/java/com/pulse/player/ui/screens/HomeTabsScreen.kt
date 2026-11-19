package com.pulse.player.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pulse.player.ui.viewmodel.MainViewModel

val defaultHomeTabs = listOf("For you", "Songs", "Playlists", "Folders", "Albums", "Artists")

@Composable
fun HomeTabsScreen(
    vm: MainViewModel,
    onMenu: () -> Unit,
    showMenu: Boolean
) {
    val activeTabs by vm.homeTabsList.collectAsStateWithLifecycle()
    val activeHomeTab by vm.activeHomeTab.collectAsStateWithLifecycle()
    val tabs = if (activeTabs.isNotEmpty()) activeTabs else defaultHomeTabs
    val pager = rememberPagerState(
        initialPage = tabs.indexOfFirst { it.equals(activeHomeTab, ignoreCase = true) }.coerceAtLeast(0)
    ) { tabs.size }

    LaunchedEffect(activeHomeTab, tabs) {
        val targetIdx = tabs.indexOfFirst { it.equals(activeHomeTab, ignoreCase = true) }
        if (targetIdx >= 0 && targetIdx != pager.currentPage) {
            pager.animateScrollToPage(targetIdx)
        }
    }

    LaunchedEffect(pager.settledPage, tabs) {
        val currentName = tabs.getOrNull(pager.settledPage)
        if (currentName != null && !currentName.equals(vm.activeHomeTab.value, ignoreCase = true)) {
            vm.activeHomeTab.value = currentName
        }
    }

    val currentTitle = tabs.getOrElse(pager.currentPage) { activeHomeTab }.ifBlank { "For you" }

    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .heightIn(min = 60.dp)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (showMenu) {
                IconButton(onClick = onMenu, modifier = Modifier.size(48.dp)) {
                    Icon(
                        imageVector = Icons.Rounded.Menu,
                        contentDescription = "Open navigation menu",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            } else {
                Spacer(Modifier.width(12.dp))
            }

            Text(
                text = currentTitle,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp)
            )

            IconButton(onClick = { vm.go(Screen.Search) }, modifier = Modifier.size(48.dp)) {
                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = "Search library",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            IconButton(onClick = { vm.go(Screen.Settings) }, modifier = Modifier.size(48.dp)) {
                Icon(
                    imageVector = Icons.Rounded.Settings,
                    contentDescription = "Open settings",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))

        HorizontalPager(
            state = pager,
            modifier = Modifier.weight(1f),
            beyondViewportPageCount = 1,
            key = { index -> tabs.getOrElse(index) { "$index" } }
        ) { page ->
            val tabName = tabs.getOrElse(page) { "" }
            when {
                tabName.equals("For you", ignoreCase = true) -> ForYouScreen(vm)
                tabName.equals("Songs", ignoreCase = true) -> SongsScreen(vm)
                tabName.equals("Playlists", ignoreCase = true) -> PlaylistsScreen(vm)
                tabName.equals("Folders", ignoreCase = true) -> FoldersScreen(vm)
                tabName.equals("Albums", ignoreCase = true) -> AlbumsScreen(vm)
                tabName.equals("Artists", ignoreCase = true) -> ArtistsScreen(vm)
                else -> SongsScreen(vm)
            }
        }
    }
}
