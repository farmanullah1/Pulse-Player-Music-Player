package com.pulse.player.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pulse.player.playback.Track
import com.pulse.player.ui.components.Artwork
import com.pulse.player.ui.components.PillButton
import com.pulse.player.ui.components.TrackRow
import com.pulse.player.ui.theme.Pulse
import com.pulse.player.ui.viewmodel.MainViewModel
import kotlin.random.Random

@Composable
fun DetailScreen(vm: MainViewModel, detail: Screen.Detail) {
    val allTracks by vm.tracks.collectAsStateWithLifecycle()
    val neverPlayed by vm.neverPlayed.collectAsStateWithLifecycle()
    val forgottenFavs by vm.forgottenFavorites.collectAsStateWithLifecycle()
    val longTracks by vm.longTracks.collectAsStateWithLifecycle()
    val ps by vm.player.state.collectAsStateWithLifecycle()

    val (title, subtitle, list) = remember(detail, allTracks, neverPlayed, forgottenFavs, longTracks) {
        when (detail.kind) {
            "album" -> {
                val matches = allTracks.filter { it.album.equals(detail.key, ignoreCase = true) }
                Triple(detail.key, "${matches.size} songs · ${matches.firstOrNull()?.artist.orEmpty()}", matches)
            }
            "artist" -> {
                val matches = allTracks.filter { it.artist.equals(detail.key, ignoreCase = true) }
                Triple(detail.key, "${matches.size} songs", matches)
            }
            "folder" -> {
                val matches = allTracks.filter { it.folder.equals(detail.key, ignoreCase = true) }
                Triple(detail.key.substringAfterLast('/'), "${matches.size} songs in ${detail.key}", matches)
            }
            "system" -> {
                when (detail.key) {
                    "favorites" -> {
                        val matches = allTracks.filter { it.isFavorite }
                        Triple("My Favourites", "${matches.size} songs", matches)
                    }
                    "recent_added" -> {
                        val matches = allTracks.sortedByDescending { it.dateAddedMs }.take(50)
                        Triple("Recently Added", "${matches.size} songs", matches)
                    }
                    "never_played" -> {
                        Triple("Never Played", "${neverPlayed.size} songs", neverPlayed)
                    }
                    "forgotten_favorites" -> {
                        Triple("Forgotten Favourites (30 days)", "${forgottenFavs.size} songs", forgottenFavs)
                    }
                    "long_tracks" -> {
                        Triple("Long Tracks (7 min+)", "${longTracks.size} songs", longTracks)
                    }
                    else -> Triple("Playlist", "${allTracks.size} songs", allTracks)
                }
            }
            else -> Triple("Playlist", "${allTracks.size} songs", allTracks)
        }
    }

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
            Spacer(Modifier.width(8.dp))
            Text(title, style = MaterialTheme.typography.titleLarge, color = Color.White, maxLines = 1)
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 120.dp)
        ) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Artwork(list.firstOrNull()?.uri, 180.dp, radius = 20.dp, px = 512)
                    Spacer(Modifier.height(16.dp))
                    Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Color.White)
                    Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = Pulse.TextMid)
                    Spacer(Modifier.height(20.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        PillButton(
                            text = "Play",
                            icon = Icons.Rounded.PlayArrow,
                            onClick = { vm.player.playQueue(list, 0) }
                        )
                        PillButton(
                            text = "Shuffle",
                            icon = Icons.Rounded.Shuffle,
                            onClick = {
                                if (list.isNotEmpty()) {
                                    val shuffled = list.shuffled(Random(System.currentTimeMillis()))
                                    vm.player.playQueue(shuffled, 0)
                                }
                            }
                        )
                    }
                }
            }

            itemsIndexed(list, key = { _, it -> it.id }) { index, track ->
                TrackRow(
                    t = track,
                    current = track.id == ps.mediaId,
                    playing = ps.isPlaying,
                    onClick = { vm.playQueue(list, index) },
                    onLongClick = { },
                    onMore = { vm.openTrackMenu(track) }
                )
            }
        }
    }
}
