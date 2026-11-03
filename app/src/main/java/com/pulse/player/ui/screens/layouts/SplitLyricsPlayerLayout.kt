package com.pulse.player.ui.screens.layouts

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.Player
import com.pulse.player.playback.PlaybackUiState
import com.pulse.player.ui.components.Artwork
import com.pulse.player.ui.components.HeartButton
import com.pulse.player.ui.screens.LyricsView
import com.pulse.player.ui.screens.Screen
import com.pulse.player.ui.screens.SeekBar
import com.pulse.player.ui.theme.Pulse
import com.pulse.player.ui.viewmodel.MainViewModel

@Composable
fun SplitLyricsPlayerLayout(
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
        // Upper Half: Synchronized Scrolling Lyrics
        Box(
            modifier = Modifier
                .weight(1.3f)
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            LyricsView(
                vm = vm,
                mediaId = s.mediaId,
                modifier = Modifier.fillMaxSize()
            )
        }

        Spacer(Modifier.height(8.dp))

        // Lower Half: Persistent Track Info & Compact Console
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .pointerInput(s.mediaId) {
                    detectTapGestures(
                        onDoubleTap = { vm.toggleFavoriteCurrent() },
                        onLongPress = { onOpenPicker() }
                    )
                },
            shape = RoundedCornerShape(24.dp),
            color = Color.Black.copy(alpha = 0.45f),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Track header row with artwork thumbnail
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Artwork(
                        uri = s.artworkUri,
                        size = 52.dp,
                        radius = 12.dp,
                        px = 128
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = s.title.ifBlank { "No track selected" },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = s.artist.ifBlank { "Unknown artist" },
                            style = MaterialTheme.typography.bodySmall,
                            color = Pulse.TextMid,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    HeartButton(
                        on = fav,
                        onToggle = { vm.toggleFavoriteCurrent() }
                    )
                }

                Spacer(Modifier.height(10.dp))

                SeekBar(
                    position = vm.player.position,
                    duration = s.durationMs,
                    onSeek = vm.player::seekTo
                )

                Spacer(Modifier.height(8.dp))

                // Transport Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = vm.player::toggleShuffle) {
                        Icon(
                            imageVector = Icons.Rounded.Shuffle,
                            contentDescription = "Shuffle",
                            tint = if (s.shuffle) Pulse.Cyan else Pulse.TextMuted,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    IconButton(onClick = vm.player::previous) {
                        Icon(
                            imageVector = Icons.Rounded.SkipPrevious,
                            contentDescription = "Previous",
                            tint = Color.White,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                    FilledIconButton(
                        onClick = vm.player::togglePlay,
                        modifier = Modifier.size(56.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = Color.White,
                            contentColor = Color.Black
                        )
                    ) {
                        AnimatedContent(
                            targetState = s.isPlaying,
                            transitionSpec = { (scaleIn() + fadeIn()) togetherWith (scaleOut() + fadeOut()) },
                            label = "split_play"
                        ) { playing ->
                            Icon(
                                imageVector = if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                contentDescription = if (playing) "Pause" else "Play",
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                    IconButton(onClick = vm.player::next) {
                        Icon(
                            imageVector = Icons.Rounded.SkipNext,
                            contentDescription = "Next",
                            tint = Color.White,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                    IconButton(onClick = vm.player::cycleRepeat) {
                        Icon(
                            imageVector = if (s.repeat == Player.REPEAT_MODE_ONE) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
                            contentDescription = "Repeat",
                            tint = if (s.repeat != Player.REPEAT_MODE_OFF) Pulse.Cyan else Pulse.TextMuted,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}
