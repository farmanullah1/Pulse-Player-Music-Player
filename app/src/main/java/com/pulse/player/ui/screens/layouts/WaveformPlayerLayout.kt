package com.pulse.player.ui.screens.layouts

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pulse.player.playback.PlaybackUiState
import com.pulse.player.ui.components.Artwork
import com.pulse.player.ui.components.HeartButton
import com.pulse.player.ui.screens.Screen
import com.pulse.player.ui.screens.SeekBar
import com.pulse.player.ui.screens.TransportRow
import com.pulse.player.ui.theme.Pulse
import com.pulse.player.ui.viewmodel.MainViewModel
import kotlin.math.sin

@Composable
fun WaveformPlayerLayout(
    s: PlaybackUiState,
    vm: MainViewModel,
    dominantColor: Color,
    onOpenPicker: () -> Unit,
    modifier: Modifier = Modifier
) {
    val fav by vm.isCurrentFavorite.collectAsStateWithLifecycle()

    val infiniteTransition = rememberInfiniteTransition(label = "waveform_anim")
    val wavePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.28318f, // 2 * PI
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Waveform Visualizer & Artwork Deck
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
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Circular album artwork with neon glow ring
                Box(
                    modifier = Modifier
                        .size(160.dp)
                        .shadow(24.dp, CircleShape)
                        .clip(CircleShape)
                        .border(3.dp, Brush.sweepGradient(listOf(Pulse.Cyan, Pulse.Magenta, Pulse.Cyan)), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Artwork(
                        uri = s.artworkUri,
                        size = 160.dp,
                        radius = 80.dp,
                        px = 320
                    )
                }

                Spacer(Modifier.height(20.dp))

                // Multi-bar Live Waveform Canvas
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(90.dp)
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(Modifier.fillMaxSize()) {
                        val barCount = 36
                        val totalSpacing = size.width / barCount
                        val barWidth = totalSpacing * 0.65f
                        val midY = size.height / 2f
                        val maxAmp = size.height * 0.44f

                        val isPlaying = s.isPlaying

                        for (i in 0 until barCount) {
                            val norm = i / barCount.toFloat()
                            val barPhase = wavePhase + norm * 4f

                            // Combined sinusoidal modulation to simulate live audio frequency spectrum
                            val harmonic = (sin(barPhase) * 0.5f +
                                    sin(barPhase * 2.3f + 1.2f) * 0.35f +
                                    sin(barPhase * 0.7f + norm * 3f) * 0.15f)

                            val amplitude = if (isPlaying) {
                                (0.18f + (harmonic + 1f) * 0.41f).coerceIn(0.12f, 1f) * maxAmp
                            } else {
                                maxAmp * 0.15f
                            }

                            val x = i * totalSpacing + (totalSpacing - barWidth) / 2f
                            val topY = midY - amplitude
                            val height = amplitude * 2f

                            val barColor = when {
                                i < barCount * 0.33f -> Pulse.Cyan
                                i < barCount * 0.66f -> Pulse.Magenta
                                else -> Color(0xFF9D4EDD)
                            }

                            drawRoundRect(
                                color = if (isPlaying) barColor else Pulse.TextMuted.copy(alpha = 0.3f),
                                topLeft = Offset(x, topY),
                                size = Size(barWidth, height),
                                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.Black.copy(alpha = 0.35f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Pulse.Cyan.copy(alpha = 0.3f))
                ) {
                    Text(
                        text = if (s.isPlaying) "SPECTRUM ANALYZER • ACTIVE" else "SPECTRUM ANALYZER • IDLE",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (s.isPlaying) Pulse.Cyan else Pulse.TextMuted,
                        letterSpacing = 1.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontSize = 9.sp,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

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

        Spacer(Modifier.height(16.dp))

        SeekBar(
            position = vm.player.position,
            duration = s.durationMs,
            onSeek = vm.player::seekTo
        )

        Spacer(Modifier.height(14.dp))

        TransportRow(s = s, p = vm.player)

        Spacer(Modifier.height(16.dp))

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
            IconButton(onClick = { vm.openQueue() }) {
                Icon(Icons.Rounded.QueueMusic, "Queue", tint = Color.White)
            }
            IconButton(onClick = { vm.go(Screen.Equalizer) }) {
                Icon(Icons.Rounded.Equalizer, "Equalizer", tint = Color.White)
            }
        }
    }
}
