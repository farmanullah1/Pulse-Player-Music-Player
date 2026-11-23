package com.pulse.player.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pulse.player.ui.components.Artwork
import com.pulse.player.ui.components.BulkSelectionBar
import com.pulse.player.ui.components.LocalBottomOverlayPadding
import com.pulse.player.ui.components.TrackRow
import com.pulse.player.ui.theme.TabularMono
import com.pulse.player.ui.viewmodel.MainViewModel
import com.pulse.player.ui.viewmodel.SearchScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlin.random.Random

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SearchScreen(vm: MainViewModel) {
    val query by vm.searchQuery.collectAsStateWithLifecycle()
    val scope by vm.searchScope.collectAsStateWithLifecycle()
    val results by vm.searchResults.collectAsStateWithLifecycle()
    val artistMatches by vm.searchArtistResults.collectAsStateWithLifecycle()
    val albumMatches by vm.searchAlbumResults.collectAsStateWithLifecycle()
    val folderMatches by vm.searchFolderResults.collectAsStateWithLifecycle()
    val scopeCounts by vm.searchScopeCounts.collectAsStateWithLifecycle()
    val recentSearches by vm.recentSearches.collectAsStateWithLifecycle()
    val selected by vm.selected.collectAsStateWithLifecycle()
    val selectMode by vm.selectMode.collectAsStateWithLifecycle()
    val sortField by vm.sortField.collectAsStateWithLifecycle()
    val sortAsc by vm.sortAscending.collectAsStateWithLifecycle()
    val bottomPad = LocalBottomOverlayPadding.current
    val focusManager = LocalFocusManager.current

    val playbackStatus by remember(vm) {
        vm.player.state.map { it.mediaId to it.isPlaying }.distinctUntilChanged()
    }.collectAsStateWithLifecycle(initialValue = null to false)
    val (currentMediaId, isPlaying) = playbackStatus

    BackHandler {
        if (selectMode) vm.clearSelect() else vm.back()
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { if (selectMode) vm.clearSelect() else vm.back() },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back", tint = MaterialTheme.colorScheme.onSurface)
                }
                OutlinedTextField(
                    value = query,
                    onValueChange = { vm.searchQuery.value = it },
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp)
                        .testTag("search_input"),
                    placeholder = { Text("Search songs, artists, albums", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    leadingIcon = { Icon(Icons.Rounded.Search, null, tint = MaterialTheme.colorScheme.primary) },
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            IconButton(
                                onClick = { vm.searchQuery.value = "" },
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("search_clear_button")
                            ) {
                                Icon(Icons.Rounded.Clear, "Clear search", tint = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(
                        onSearch = {
                            vm.commitRecentSearch()
                            focusManager.clearFocus()
                        }
                    ),
                    shape = CircleShape,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }

            val chipColors = FilterChipDefaults.filterChipColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                labelColor = MaterialTheme.colorScheme.onSurface,
                iconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimaryContainer
            )

            // Scope filter chips + Sort trigger with live match counts
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = false,
                    onClick = { vm.openSortFilterSheet() },
                    leadingIcon = { Icon(Icons.Rounded.Tune, contentDescription = "Sort", modifier = Modifier.size(16.dp)) },
                    label = { Text("${sortField.label} ${if (sortAsc) "↑" else "↓"}") },
                    colors = chipColors,
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = false,
                        borderColor = MaterialTheme.colorScheme.outlineVariant
                    )
                )
                SearchScope.entries.forEach { s ->
                    val isSelected = scope == s
                    val count = scopeCounts[s]
                    val label = if (count != null && (query.isNotBlank() || s == SearchScope.FAVORITES)) {
                        "${s.label} ($count)"
                    } else {
                        s.label
                    }
                    FilterChip(
                        selected = isSelected,
                        onClick = { vm.setSearchScope(s) },
                        label = { Text(label) },
                        colors = chipColors,
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = MaterialTheme.colorScheme.outlineVariant,
                            selectedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.testTag("search_scope_${s.name.lowercase()}")
                    )
                }
            }
            HorizontalDivider(
                modifier = Modifier.padding(top = 4.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
            )

            val showArtists = (scope == SearchScope.ALL || scope == SearchScope.ARTISTS) && artistMatches.isNotEmpty()
            val showAlbums = (scope == SearchScope.ALL || scope == SearchScope.ALBUMS) && albumMatches.isNotEmpty()
            val showFolders = (scope == SearchScope.ALL || scope == SearchScope.FOLDERS) && folderMatches.isNotEmpty()
            val hasAnyResults = results.isNotEmpty() || showArtists || showAlbums || showFolders

            if (query.isBlank() && scope != SearchScope.FAVORITES) {
                if (recentSearches.isNotEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    Icons.Rounded.History,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    "Recent Searches",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            TextButton(
                                onClick = { vm.clearRecentSearches() },
                                modifier = Modifier.testTag("search_clear_history")
                            ) {
                                Text("Clear all")
                            }
                        }
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            recentSearches.forEach { term ->
                                InputChip(
                                    selected = false,
                                    onClick = {
                                        vm.searchQuery.value = term
                                        focusManager.clearFocus()
                                    },
                                    label = { Text(term, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                    trailingIcon = {
                                        Icon(
                                            imageVector = Icons.Rounded.Close,
                                            contentDescription = "Remove $term",
                                            modifier = Modifier
                                                .size(16.dp)
                                                .clickable { vm.removeRecentSearch(term) }
                                        )
                                    },
                                    colors = InputChipDefaults.inputChipColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                        labelColor = MaterialTheme.colorScheme.onSurface
                                    ),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f))
                                )
                            }
                        }
                    }
                } else {
                    Column(
                        Modifier
                            .fillMaxSize()
                            .padding(40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically)
                    ) {
                        Icon(
                            Icons.Rounded.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            "Find any track",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            "Search multi-word titles, artists, albums, or folders, or tap Favourites above.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else if (!hasAnyResults) {
                Column(
                    Modifier
                        .fillMaxSize()
                        .padding(40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically)
                ) {
                    Icon(
                        Icons.Rounded.SearchOff,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        text = if (query.isBlank()) "No favourite tracks yet" else "No results for \"$query\"",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        "Try switching the filter chip above or adjusting your keywords.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    state = vm.lazyListStateFor("search"),
                    modifier = Modifier
                        .fillMaxSize()
                        .imePadding(),
                    contentPadding = PaddingValues(
                        top = 8.dp,
                        bottom = if (selectMode) bottomPad + 80.dp else bottomPad
                    )
                ) {
                    if (showAlbums) {
                        item(key = "albums_section") {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    "Albums (${albumMatches.size})",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    items(albumMatches, key = { it.name }) { album ->
                                        Column(
                                            modifier = Modifier
                                                .width(108.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .clickable {
                                                    vm.commitRecentSearch()
                                                    vm.go(Screen.Detail("album", album.name))
                                                }
                                                .padding(4.dp)
                                        ) {
                                            Artwork(album.artUri, 100.dp, radius = 12.dp, px = 256)
                                            Text(
                                                album.name,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier.padding(top = 6.dp)
                                            )
                                            Text(
                                                "${album.count} songs",
                                                style = MaterialTheme.typography.labelSmall.merge(TabularMono),
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (showArtists) {
                        item(key = "artists_section") {
                            Column(
                                modifier = Modifier.padding(top = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    "Artists (${artistMatches.size})",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(artistMatches, key = { it.name }) { artist ->
                                        Row(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(14.dp))
                                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                                .clickable {
                                                    vm.commitRecentSearch()
                                                    vm.go(Screen.Detail("artist", artist.name))
                                                }
                                                .padding(horizontal = 12.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Rounded.Person, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                            Spacer(Modifier.width(8.dp))
                                            Column {
                                                Text(artist.name, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                                                Text("${artist.trackCount} songs", style = MaterialTheme.typography.labelSmall.merge(TabularMono), color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (showFolders) {
                        item(key = "folders_section") {
                            Column(
                                modifier = Modifier.padding(top = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    "Folders (${folderMatches.size})",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(folderMatches, key = { it.path }) { folder ->
                                        Row(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(14.dp))
                                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                                .clickable {
                                                    vm.commitRecentSearch()
                                                    vm.go(Screen.Detail("folder", folder.path))
                                                }
                                                .padding(horizontal = 12.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Rounded.Folder, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                            Spacer(Modifier.width(8.dp))
                                            Column {
                                                Text(folder.name, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                                                Text("${folder.count} songs", style = MaterialTheme.typography.labelSmall.merge(TabularMono), color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (results.isNotEmpty()) {
                        item(key = "tracks_header") {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Songs (${results.size})",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    AssistChip(
                                        onClick = {
                                            vm.commitRecentSearch()
                                            vm.playQueue(results, 0)
                                        },
                                        label = { Text("Play All") },
                                        leadingIcon = {
                                            Icon(Icons.Rounded.PlayArrow, null, modifier = Modifier.size(16.dp))
                                        },
                                        colors = AssistChipDefaults.assistChipColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                            labelColor = MaterialTheme.colorScheme.onSurface,
                                            leadingIconContentColor = MaterialTheme.colorScheme.primary
                                        ),
                                        modifier = Modifier.testTag("search_play_all")
                                    )
                                    AssistChip(
                                        onClick = {
                                            vm.commitRecentSearch()
                                            vm.playQueue(results, Random.nextInt(results.size), shuffle = true)
                                        },
                                        label = { Text("Shuffle") },
                                        leadingIcon = {
                                            Icon(Icons.Rounded.Shuffle, null, modifier = Modifier.size(16.dp))
                                        },
                                        colors = AssistChipDefaults.assistChipColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                            labelColor = MaterialTheme.colorScheme.onSurface,
                                            leadingIconContentColor = MaterialTheme.colorScheme.primary
                                        ),
                                        modifier = Modifier.testTag("search_shuffle_all")
                                    )
                                    IconButton(
                                        onClick = { vm.toggleSelectMode() },
                                        modifier = Modifier
                                            .size(48.dp)
                                            .testTag("search_select_mode_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Checklist,
                                            contentDescription = "Select matching songs",
                                            tint = if (selectMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                        itemsIndexed(results, key = { _, t -> t.id }, contentType = { _, _ -> "track" }) { index, track ->
                            TrackRow(
                                t = track,
                                current = track.id == currentMediaId,
                                playing = isPlaying,
                                selected = track.id in selected,
                                selectMode = selectMode,
                                onSwipeRightQueue = {
                                    vm.playNext(track)
                                    vm.snackbarMessage.value = "Queued next: ${track.title}"
                                },
                                onSwipeLeftFavorite = {
                                    vm.toggleFavorite(track.id)
                                    vm.snackbarMessage.value = if (!track.isFavorite) "Added to favourites" else "Removed from favourites"
                                },
                                onClick = {
                                    if (selectMode) {
                                        vm.toggleSelect(track.id)
                                    } else {
                                        vm.commitRecentSearch()
                                        vm.playQueue(results, index)
                                    }
                                },
                                onLongClick = { vm.toggleSelect(track.id) },
                                onMore = { vm.openTrackMenu(track) }
                            )
                        }
                    }
                }
            }
        }

        if (selectMode) {
            BulkSelectionBar(
                selectedCount = selected.size,
                onSelectAll = { vm.selectAll(results.map { it.id }) },
                onAddToPlaylist = { vm.openBatchAddToPlaylist() },
                onBatchEditTags = { vm.openBatchTagEditor() },
                onPlayNext = { vm.playSelectedNext() },
                onFavorite = { vm.favoriteSelected() },
                onClose = { vm.clearSelect() },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = bottomPad, start = 16.dp, end = 16.dp)
            )
        }
    }
}
