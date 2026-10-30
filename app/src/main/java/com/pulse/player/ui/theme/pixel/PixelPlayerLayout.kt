package com.pulse.player.ui.theme.pixel

import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.Player
import com.pulse.player.playback.PlaybackUiState
import com.pulse.player.ui.components.Artwork
import com.pulse.player.ui.components.WavyMusicSlider
import com.pulse.player.ui.components.formatTime
import com.pulse.player.ui.theme.Pulse
import com.pulse.player.ui.viewmodel.MainViewModel

/**
 * Pixel Expressive Player Layout adapted from PixelPlayer.
 * Features Material 3 squiggly wavy slider, expressive pill controls, and dynamic album card.
 */
@Composable
fun PixelPlayerLayout(
    s: PlaybackUiState,
    vm: MainViewModel,
    onBack: () -> Unit
) {
    val fav by vm.isFavorite(s.mediaId).collectAsStateWithLifecycle(false)
    val pos by vm.player.position.collectAsStateWithLifecycle()
    val canSeek = s.canSeek && s.durationMs > 0

    val frac = if (s.durationMs > 0) (pos.toFloat() / s.durationMs).coerceIn(0f, 1f) else 0f

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF1E293B),
                        Color(0xFF0F172A),
                        Color(0xFF020617)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(horizontal = 24.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Bar
            Row(
                modifier = Modifier.fillMaxWidth().height(56.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Rounded.KeyboardArrowDown, "Back", tint = Color.White)
                }
                Text(
                    text = "Pixel Expressive",
                    modifier = Modifier.weight(1f),
                    color = Color.White.copy(0.8f),
                    style = MaterialTheme.typography.titleSmall,
                    textAlign = TextAlign.Center
                )
                IconButton(onClick = vm::openTrackOptions, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Rounded.MoreVert, "More", tint = Color.White)
                }
            }

            Spacer(Modifier.height(12.dp))

            // Album Artwork with Material 3 Expressive Card Shape
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.82f)
                    .aspectRatio(1f)
                    .shadow(24.dp, RoundedCornerShape(32.dp)),
                shape = RoundedCornerShape(32.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(0.06f))
            ) {
                Box(Modifier.fillMaxSize(), Alignment.Center) {
                    Artwork(uri = s.artworkUri, size = 300.dp, radius = 32.dp, px = 512)
                }
            }

            Spacer(Modifier.height(28.dp))

            // Song Title & Artist
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = s.title.ifBlank { "Unknown Title" },
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color.White,
                        maxLines = 1,
                        modifier = Modifier.basicMarquee()
                    )
                    Text(
                        text = s.artist.ifBlank { "Unknown Artist" },
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                IconButton(onClick = { s.mediaId?.let { vm.toggleFavorite(it) } }) {
                    Icon(
                        imageVector = if (fav) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (fav) Pulse.Orange else Color.White
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Expressive Wavy Slider from PixelPlayer
            WavyMusicSlider(
                value = frac,
                onValueChange = { targetFrac ->
                    if (canSeek) {
                        vm.player.seekTo((targetFrac * s.durationMs).toLong())
                    }
                },
                enabled = canSeek,
                isPlaying = s.isPlaying,
                activeTrackColor = Color.White,
                inactiveTrackColor = Color.White.copy(alpha = 0.25f),
                thumbColor = Color.White
            )

            // Duration timestamps
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatTime(pos),
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.75f)
                )
                Text(
                    text = if (s.durationMs > 0) formatTime(s.durationMs) else "--:--",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.75f)
                )
            }

            Spacer(Modifier.height(16.dp))

            // Expressive Primary Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = vm::toggleShuffle) {
                    Icon(
                        Icons.Rounded.Shuffle,
                        "Shuffle",
                        tint = if (s.shuffle) Pulse.Orange else Color.White.copy(0.7f)
                    )
                }
                IconButton(onClick = vm.player::previous, modifier = Modifier.size(52.dp)) {
                    Icon(Icons.Rounded.SkipPrevious, "Previous", tint = Color.White, modifier = Modifier.size(36.dp))
                }

                // Center Play/Pause 76dp button
                FilledIconButton(
                    onClick = vm.player::togglePlayPause,
                    modifier = Modifier.size(76.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = Color.White,
                        contentColor = Color(0xFF0F172A)
                    ),
                    shape = CircleShape
                ) {
                    Icon(
                        imageVector = if (s.playWhenReady) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = if (s.playWhenReady) "Pause" else "Play",
                        modifier = Modifier.size(40.dp)
                    )
                }

                IconButton(onClick = vm.player::next, modifier = Modifier.size(52.dp)) {
                    Icon(Icons.Rounded.SkipNext, "Next", tint = Color.White, modifier = Modifier.size(36.dp))
                }
                IconButton(onClick = vm::cycleRepeat) {
                    Icon(
                        if (s.repeat == Player.REPEAT_MODE_ONE) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
                        "Repeat",
                        tint = if (s.repeat != Player.REPEAT_MODE_OFF) Pulse.Orange else Color.White.copy(0.7f)
                    )
                }
            }

            Spacer(Modifier.weight(1f))

            // Bottom Queue Quick Trigger
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.White.copy(alpha = 0.08f))
                    .clickable { vm.openQueue() }
                    .padding(horizontal = 20.dp, vertical = 10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.QueueMusic, null, tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(8.dp))
                    Text("Up Next in Queue", color = Color.White, style = MaterialTheme.typography.labelMedium)
                }
            }

            Spacer(Modifier.height(12.dp))
        }
    }
}

