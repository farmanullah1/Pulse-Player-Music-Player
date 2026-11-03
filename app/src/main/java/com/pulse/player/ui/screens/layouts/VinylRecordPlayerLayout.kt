package com.pulse.player.ui.screens.layouts

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
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

@Composable
fun VinylRecordPlayerLayout(
    s: PlaybackUiState,
    vm: MainViewModel,
    dominantColor: Color,
    onOpenPicker: () -> Unit,
    modifier: Modifier = Modifier
) {
    val fav by vm.isCurrentFavorite.collectAsStateWithLifecycle()

    val infiniteTransition = rememberInfiniteTransition(label = "vinyl_spin")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )
    val currentRotation = if (s.isPlaying) rotation else 0f

    val tonearmAngle by animateFloatAsState(
        targetValue = if (s.isPlaying) 24f else 0f,
        animationSpec = tween(500),
        label = "tonearm_angle"
    )

    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Turntable Deck Stage
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .size(310.dp)
                    .pointerInput(s.mediaId) {
                        detectTapGestures(
                            onTap = { vm.player.togglePlay() },
                            onDoubleTap = { vm.toggleFavoriteCurrent() },
                            onLongPress = { onOpenPicker() }
                        )
                    },
                shape = RoundedCornerShape(28.dp),
                color = Color(0xFF141218),
                tonalElevation = 8.dp,
                shadowElevation = 12.dp
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    // Brushed platter background
                    Canvas(Modifier.fillMaxSize()) {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(Color(0xFF23202A), Color(0xFF110F15)),
                                radius = size.minDimension / 1.8f
                            )
                        )
                    }

                    // Spinning Vinyl Record
                    Box(
                        modifier = Modifier
                            .size(260.dp)
                            .shadow(16.dp, CircleShape)
                            .clip(CircleShape)
                            .background(Color(0xFF0D0B10))
                            .rotate(currentRotation),
                        contentAlignment = Alignment.Center
                    ) {
                        // Vinyl Grooves & Reflection
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val center = Offset(size.width / 2f, size.height / 2f)
                            val maxR = size.minDimension / 2f
                            val stroke = Stroke(width = 1.2f)

                            for (r in 55..120 step 6) {
                                drawCircle(
                                    color = Color.White.copy(alpha = 0.07f),
                                    radius = (r * maxR) / 130f,
                                    center = center,
                                    style = stroke
                                )
                            }
                            // Stylized vinyl luster sheen
                            drawCircle(
                                brush = Brush.sweepGradient(
                                    listOf(
                                        Color.White.copy(alpha = 0.02f),
                                        Color.White.copy(alpha = 0.12f),
                                        Color.White.copy(alpha = 0.02f),
                                        Color.White.copy(alpha = 0.14f),
                                        Color.White.copy(alpha = 0.02f)
                                    )
                                )
                            )
                        }

                        // Center Album Art Label
                        Box(
                            modifier = Modifier
                                .size(96.dp)
                                .clip(CircleShape)
                                .border(3.dp, Color(0xFF332D3F), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Artwork(
                                uri = s.artworkUri,
                                size = 96.dp,
                                radius = 48.dp,
                                px = 256
                            )
                            // Spindle hole
                            Box(
                                modifier = Modifier
                                    .size(14.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF0B0910))
                                    .border(2.dp, Color(0xFFD4AF37), CircleShape)
                            )
                        }
                    }

                    // Tonearm Visual
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 18.dp, end = 20.dp)
                            .size(width = 60.dp, height = 150.dp)
                            .rotate(tonearmAngle),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        Canvas(Modifier.fillMaxSize()) {
                            // Tonearm base pivot
                            drawCircle(
                                color = Color(0xFFD4AF37),
                                radius = 10.dp.toPx(),
                                center = Offset(size.width / 2f, 12.dp.toPx())
                            )
                            drawCircle(
                                color = Color(0xFF4A4232),
                                radius = 6.dp.toPx(),
                                center = Offset(size.width / 2f, 12.dp.toPx())
                            )
                            // Tonearm metallic wand
                            drawLine(
                                color = Color(0xFFC0C0C0),
                                start = Offset(size.width / 2f, 12.dp.toPx()),
                                end = Offset(size.width * 0.35f, size.height - 18.dp.toPx()),
                                strokeWidth = 3.dp.toPx()
                            )
                            // Headshell cartridge
                            drawRoundRect(
                                color = Color(0xFFD4AF37),
                                topLeft = Offset(size.width * 0.22f, size.height - 20.dp.toPx()),
                                size = androidx.compose.ui.geometry.Size(16.dp.toPx(), 20.dp.toPx()),
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(3.dp.toPx())
                            )
                        }
                    }

                    // Turntable badge indicator
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(14.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = Color.Black.copy(alpha = 0.5f)
                    ) {
                        Text(
                            text = if (s.isPlaying) "VINYL 33 ⅓ RPM • STEREO" else "PAUSED",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (s.isPlaying) Color(0xFFD4AF37) else Pulse.TextMuted,
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(14.dp))

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
