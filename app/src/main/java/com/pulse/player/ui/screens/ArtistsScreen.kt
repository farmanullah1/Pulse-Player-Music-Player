package com.pulse.player.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pulse.player.ui.theme.Pulse
import com.pulse.player.ui.viewmodel.MainViewModel

data class ArtistItem(val name: String, val trackCount: Int, val albumCount: Int)

@Composable
fun ArtistsScreen(vm: MainViewModel) {
    val tracks by vm.tracks.collectAsStateWithLifecycle()
    val artists = remember(tracks) {
        tracks.groupBy { it.artist.ifBlank { "Unknown Artist" } }
            .map { (name, list) ->
                val albums = list.map { it.album }.distinct().size
                ArtistItem(name, list.size, albums)
            }
            .sortedBy { it.name.lowercase() }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("${artists.size} Artists", style = MaterialTheme.typography.titleLarge, color = Color.White)
        }

        if (artists.isEmpty()) {
            item {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No artists found", color = Pulse.TextMid)
                }
            }
        } else {
            items(artists, key = { it.name }) { artist ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { vm.go(Screen.Detail("artist", artist.name)) }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val initial = artist.name.firstOrNull { it.isLetterOrDigit() }?.uppercaseChar() ?: '?'
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(Pulse.Violet, Pulse.Magenta))),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = initial.toString(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = artist.name,
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White
                        )
                        Text(
                            text = "${artist.trackCount} songs · ${artist.albumCount} albums",
                            style = MaterialTheme.typography.bodySmall,
                            color = Pulse.TextMid
                        )
                    }
                }
            }
        }
    }
}
