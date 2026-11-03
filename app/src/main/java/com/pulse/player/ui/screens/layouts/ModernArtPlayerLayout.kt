package com.pulse.player.ui.screens.layouts

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pulse.player.playback.PlaybackUiState
import com.pulse.player.ui.components.HeartButton
import com.pulse.player.ui.screens.PlayerThemeHost
import com.pulse.player.ui.screens.Screen
import com.pulse.player.ui.screens.SeekBar
import com.pulse.player.ui.screens.TransportRow
import com.pulse.player.ui.theme.Pulse
import com.pulse.player.ui.viewmodel.MainViewModel

@Composable
fun ModernArtPlayerLayout(
    s: PlaybackUiState,
    vm: MainViewModel,
    dominantColor: Color,
    onOpenPicker: () -> Unit,
    modifier: Modifier = Modifier
) {
    val fav by vm.isCurrentFavorite.collectAsStateWithLifecycle()

    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .pointerInput(s.mediaId) {
                    detectTapGestures(
                        onDoubleTap = { vm.toggleFavoriteCurrent() },
                        onLongPress = { onOpenPicker() }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            PlayerThemeHost(vm, s, dominantColor)
        }

        Spacer(Modifier.height(16.dp))

        Text(
            text = s.title.ifBlank { "No track selected" },
            style = MaterialTheme.typography.titleLarge,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = s.artist.ifBlank { "Unknown artist" },
            style = MaterialTheme.typography.bodyMedium,
            color = Pulse.TextMid,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(Modifier.height(20.dp))

        SeekBar(
            position = vm.player.position,
            duration = s.durationMs,
            onSeek = vm.player::seekTo
        )

        Spacer(Modifier.height(16.dp))

        TransportRow(s = s, p = vm.player)

        Spacer(Modifier.height(20.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            HeartButton(
                on = fav,
                onToggle = { vm.toggleFavoriteCurrent() }
            )
            IconButton(onClick = { vm.openSleepSheet() }) {
                Icon(Icons.Rounded.Bedtime, "Sleep timer", tint = Color.White)
            }
            IconButton(onClick = { vm.openAddToPlaylist() }) {
                Icon(Icons.Rounded.PlaylistAdd, "Add to playlist", tint = Color.White)
            }
            IconButton(onClick = { vm.openQueue() }) {
                Icon(Icons.Rounded.QueueMusic, "Queue", tint = Color.White)
            }
            IconButton(onClick = { vm.go(Screen.Equalizer) }) {
                Icon(Icons.Rounded.Equalizer, "Equalizer", tint = Color.White)
            }
        }
    }
}
