package com.pulse.player.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pulse.player.ui.components.TrackRow
import com.pulse.player.ui.theme.Pulse
import com.pulse.player.ui.viewmodel.MainViewModel

@Composable
fun SearchScreen(vm: MainViewModel) {
    val query by vm.searchQuery.collectAsStateWithLifecycle()
    val results by vm.searchResults.collectAsStateWithLifecycle()
    val ps by vm.player.state.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { vm.back() }) {
                Icon(Icons.Rounded.ArrowBack, "Back", tint = Color.White)
            }
            OutlinedTextField(
                value = query,
                onValueChange = { vm.searchQuery.value = it },
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp),
                placeholder = { Text("Search songs, artists, albums", color = Pulse.TextMid) },
                leadingIcon = { Icon(Icons.Rounded.Search, null, tint = Pulse.Cyan) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { vm.searchQuery.value = "" }) {
                            Icon(Icons.Rounded.Clear, "Clear search", tint = Color.White)
                        }
                    }
                },
                singleLine = true,
                shape = CircleShape,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Pulse.Cyan,
                    unfocusedBorderColor = Pulse.Outline,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedContainerColor = Pulse.Surface,
                    unfocusedContainerColor = Pulse.Surface
                )
            )
        }

        if (query.isBlank()) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(40.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Type something to search your library", color = Pulse.TextMid)
            }
        } else if (results.isEmpty()) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(40.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("No matching songs found for \"$query\"", color = Pulse.TextMid)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 120.dp)
            ) {
                itemsIndexed(results, key = { _, t -> t.id }) { index, track ->
                    TrackRow(
                        t = track,
                        current = track.id == ps.mediaId,
                        playing = ps.isPlaying,
                        onClick = { vm.playQueue(results, index) },
                        onLongClick = { vm.toggleSelect(track.id) },
                        onMore = { vm.openTrackMenu(track) }
                    )
                }
            }
        }
    }
}
