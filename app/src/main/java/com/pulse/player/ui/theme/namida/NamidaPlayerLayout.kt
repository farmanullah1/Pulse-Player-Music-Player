package com.pulse.player.ui.theme.namida

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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Equalizer
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.Player
import com.pulse.player.playback.PlaybackUiState
import com.pulse.player.ui.components.Artwork
import com.pulse.player.ui.components.AudioSignalPathBadge
import com.pulse.player.ui.components.WaveformSeekBar
import com.pulse.player.ui.components.formatTime
import com.pulse.player.ui.screens.Screen
import com.pulse.player.ui.theme.Pulse
import com.pulse.player.ui.viewmodel.MainViewModel

/**
 * Namida Waveform & Widescreen Player Layout adapted from Namida.
 * Features Namida amber theme, audio waveform seekbar, audio signal path inspector,
 * and responsive two-column tablet/foldable mode.
 */
@Composable
fun NamidaPlayerLayout(
    s: PlaybackUiState,
    vm: MainViewModel,
    onBack: () -> Unit
) {
    val fav by vm.isFavorite(s.mediaId).collectAsStateWithLifecycle(false)
    val pos by vm.player.position.collectAsStateWithLifecycle()
    val canSeek = s.canSeek && s.durationMs > 0
    val currentTrack = vm.tracks.collectAsStateWithLifecycle().value.firstOrNull { it.id == s.mediaId }

    val frac = if (s.durationMs > 0) (pos.toFloat() / s.durationMs).coerceIn(0f, 1f) else 0f

    val cfg = LocalConfiguration.current
    val isWide = cfg.screenWidthDp >= 600

    val amberColor = Color(0xFFF1D6B6)
    val obsidianDark = Color(0xFF1E1A17)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF382F26),
                        obsidianDark,
                        Color(0xFF0F0D0B)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(horizontal = 24.dp, vertical = 8.dp)
        ) {
            // Top Bar
            Row(
                modifier = Modifier.fillMaxWidth().height(56.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Rounded.KeyboardArrowDown, "Back", tint = amberColor)
                }
                Text(
                    text = "Namida Waveform",
                    modifier = Modifier.weight(1f),
                    color = amberColor.copy(alpha = 0.85f),
                    style = MaterialTheme.typography.titleSmall,
                    textAlign = TextAlign.Center
                )
                IconButton(onClick = vm::openTrackOptions, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Rounded.MoreVert, "More", tint = amberColor)
                }
            }

            if (isWide) {
                // Wide Screen Tablet / Foldable Mode (Two Columns)
                Row(
                    modifier = Modifier.fillMaxSize().padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(32.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left: Artwork
                    Box(
                        modifier = Modifier.weight(0.45f).fillMaxHeight(),
                        contentAlignment = Alignment.Center
                    ) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth(0.9f)
                                .aspectRatio(1f)
                                .shadow(24.dp, RoundedCornerShape(28.dp)),
                            shape = RoundedCornerShape(28.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White.copy(0.06f))
                        ) {
                            Artwork(uri = s.artworkUri, size = 320.dp, radius = 28.dp, px = 512)
                        }
                    }

                    // Right: Track info + Waveform + Controls
                    Column(
                        modifier = Modifier.weight(0.55f).fillMaxHeight(),
                        verticalArrangement = Arrangement.Center
                    ) {
                        TrackHeader(s, currentTrack, fav, amberColor) {
                            s.mediaId?.let { vm.toggleFavorite(it) }
                        }
                        Spacer(Modifier.height(24.dp))
                        WaveformSeekBar(
                            value = frac,
                            onValueChange = { targetFrac ->
                                if (canSeek) vm.player.seekTo((targetFrac * s.durationMs).toLong())
                            },
                            enabled = canSeek,
                            isPlaying = s.isPlaying,
                            seedKey = s.mediaId ?: "default",
                            activeColor = amberColor,
                            inactiveColor = amberColor.copy(alpha = 0.25f)
                        )
                        DurationRow(pos, s.durationMs, amberColor)
                        Spacer(Modifier.height(24.dp))
                        PlaybackControls(s, vm, amberColor)
                    }
                }
            } else {
                // Portrait Handheld Phone Mode
                Spacer(Modifier.height(8.dp))
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .aspectRatio(1f)
                        .align(Alignment.CenterHorizontally)
                        .shadow(20.dp, RoundedCornerShape(24.dp)),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(0.06f))
                ) {
                    Box(Modifier.fillMaxSize(), Alignment.Center) {
                        Artwork(uri = s.artworkUri, size = 300.dp, radius = 24.dp, px = 512)
                    }
                }

                Spacer(Modifier.height(24.dp))

                TrackHeader(s, currentTrack, fav, amberColor) {
                    s.mediaId?.let { vm.toggleFavorite(it) }
                }

                Spacer(Modifier.height(16.dp))

                // Interactive Audio Waveform Seekbar from Namida
                WaveformSeekBar(
                    value = frac,
                    onValueChange = { targetFrac ->
                        if (canSeek) vm.player.seekTo((targetFrac * s.durationMs).toLong())
                    },
                    enabled = canSeek,
                    isPlaying = s.isPlaying,
                    seedKey = s.mediaId ?: "default",
                    activeColor = amberColor,
                    inactiveColor = amberColor.copy(alpha = 0.25f)
                )

                DurationRow(pos, s.durationMs, amberColor)

                Spacer(Modifier.height(16.dp))

                PlaybackControls(s, vm, amberColor)

                Spacer(Modifier.weight(1f))

                // Bottom Action Bar: Queue + Equalizer
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(amberColor.copy(alpha = 0.12f))
                            .clickable { vm.openQueue() }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.QueueMusic, null, tint = amberColor, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Queue", color = amberColor, style = MaterialTheme.typography.labelMedium)
                        }
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(amberColor.copy(alpha = 0.12f))
                            .clickable { vm.go(Screen.Equalizer) }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Equalizer, null, tint = amberColor, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Sound FX", color = amberColor, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TrackHeader(
    s: PlaybackUiState,
    track: com.pulse.player.playback.Track?,
    fav: Boolean,
    accentColor: Color,
    onToggleFav: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = s.title.ifBlank { "Unknown Title" },
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = accentColor,
                maxLines = 1,
                modifier = Modifier.basicMarquee()
            )
            Spacer(Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = s.artist.ifBlank { "Unknown Artist" },
                    style = MaterialTheme.typography.bodyMedium,
                    color = accentColor.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.width(8.dp))
                AudioSignalPathBadge(track)
            }
        }
        IconButton(onClick = onToggleFav) {
            Icon(
                imageVector = if (fav) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                contentDescription = "Favorite",
                tint = if (fav) Pulse.Orange else accentColor
            )
        }
    }
}

@Composable
private fun DurationRow(pos: Long, dur: Long, accentColor: Color) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = formatTime(pos),
            style = MaterialTheme.typography.labelSmall,
            color = accentColor.copy(alpha = 0.7f)
        )
        Text(
            text = if (dur > 0) formatTime(dur) else "--:--",
            style = MaterialTheme.typography.labelSmall,
            color = accentColor.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun PlaybackControls(
    s: PlaybackUiState,
    vm: MainViewModel,
    accentColor: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = vm::toggleShuffle) {
            Icon(
                Icons.Rounded.Shuffle,
                "Shuffle",
                tint = if (s.shuffle) Pulse.Orange else accentColor.copy(0.7f)
            )
        }
        IconButton(onClick = vm.player::previous, modifier = Modifier.size(52.dp)) {
            Icon(Icons.Rounded.SkipPrevious, "Previous", tint = accentColor, modifier = Modifier.size(34.dp))
        }

        FilledIconButton(
            onClick = vm.player::togglePlayPause,
            modifier = Modifier.size(72.dp),
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = accentColor,
                contentColor = Color(0xFF1E1A17)
            ),
            shape = CircleShape
        ) {
            Icon(
                imageVector = if (s.playWhenReady) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                contentDescription = if (s.playWhenReady) "Pause" else "Play",
                modifier = Modifier.size(38.dp)
            )
        }

        IconButton(onClick = vm.player::next, modifier = Modifier.size(52.dp)) {
            Icon(Icons.Rounded.SkipNext, "Next", tint = accentColor, modifier = Modifier.size(34.dp))
        }
        IconButton(onClick = vm::cycleRepeat) {
            Icon(
                if (s.repeat == Player.REPEAT_MODE_ONE) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
                "Repeat",
                tint = if (s.repeat != Player.REPEAT_MODE_OFF) Pulse.Orange else accentColor.copy(0.7f)
            )
        }
    }
}

