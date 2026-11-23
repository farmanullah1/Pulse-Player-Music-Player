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
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Equalizer
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
import com.pulse.player.ui.components.PlaybackErrorBanner
import com.pulse.player.ui.screens.PlayerThemeHost
import com.pulse.player.ui.screens.Screen
import com.pulse.player.ui.screens.SeekBar
import com.pulse.player.ui.screens.TransportRow
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
    val seekStyle by vm.seekStyle.collectAsStateWithLifecycle()
    val abStartFrac = s.abLoopStartMs?.takeIf { s.durationMs > 0L }?.let { (it.toFloat() / s.durationMs).coerceIn(0f, 1f) }
    val abEndFrac = s.abLoopEndMs?.takeIf { s.durationMs > 0L }?.let { (it.toFloat() / s.durationMs).coerceIn(0f, 1f) }

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

        s.trackError?.let { err ->
            PlaybackErrorBanner(
                error = err,
                onRetry = vm.player::retry,
                onSkip = vm.player::skip,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        Spacer(Modifier.height(16.dp))

        Text(
            text = s.title.ifBlank { "No track selected" },
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = s.artist.ifBlank { "Unknown artist" },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(Modifier.height(20.dp))

        SeekBar(
            position = vm.player.position,
            duration = s.durationMs,
            isPlaying = s.isPlaying,
            trackKey = s.mediaId ?: "default",
            seekStyle = seekStyle,
            onSelectStyle = vm::setSeekStyle,
            abStartFraction = abStartFrac,
            abEndFraction = abEndFrac,
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
            IconButton(onClick = { vm.openSleepSheet() }, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Rounded.Bedtime, "Sleep timer", tint = MaterialTheme.colorScheme.onSurface)
            }
            IconButton(onClick = { vm.openAddToPlaylist() }, modifier = Modifier.size(48.dp)) {
                Icon(Icons.AutoMirrored.Rounded.PlaylistAdd, "Add to playlist", tint = MaterialTheme.colorScheme.onSurface)
            }
            IconButton(onClick = { vm.openQueue() }, modifier = Modifier.size(48.dp)) {
                Icon(Icons.AutoMirrored.Rounded.QueueMusic, "Queue", tint = MaterialTheme.colorScheme.onSurface)
            }
            IconButton(onClick = { vm.go(Screen.Equalizer) }, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Rounded.Equalizer, "Equalizer", tint = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}
