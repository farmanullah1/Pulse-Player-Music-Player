package com.pulse.player.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pulse.player.playback.PlaybackUiState
import com.pulse.player.ui.theme.LocalReducedMotion
import com.pulse.player.ui.theme.Pulse
import kotlinx.coroutines.flow.StateFlow

@Composable
fun MiniPlayer(
    s: PlaybackUiState,
    pos: StateFlow<Long>,
    onToggle: () -> Unit,
    onQueue: () -> Unit,
    onExpand: () -> Unit,
    modifier: Modifier = Modifier
) {
    val rot = remember { Animatable(0f) }
    val reduced = LocalReducedMotion.current
    LaunchedEffect(s.isPlaying, reduced) {
        if (s.isPlaying && !reduced) {
            while (true) rot.animateTo(rot.value + 360f, tween(8000, easing = LinearEasing))
        }
    }
    val position by pos.collectAsStateWithLifecycle()

    Surface(
        modifier = modifier
            .padding(horizontal = 12.dp)
            .fillMaxWidth()
            .height(64.dp)
            .clickable(onClick = onExpand),
        shape = RoundedCornerShape(32.dp),
        color = Color(0xCC1B1829),
        tonalElevation = 6.dp
    ) {
        Box {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Artwork(
                    s.artworkUri,
                    48.dp,
                    Modifier.graphicsLayer { rotationZ = rot.value },
                    radius = 24.dp,
                    px = 128
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = s.title.ifBlank { "Nothing playing" },
                        maxLines = 1,
                        modifier = Modifier.basicMarquee(),
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = s.artist.ifBlank { "Select a song to start" },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodySmall,
                        color = Pulse.TextMid
                    )
                }
                IconButton(onClick = onToggle) {
                    AnimatedContent(s.isPlaying, label = "pp") { p ->
                        Icon(
                            imageVector = if (p) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = if (p) "Pause" else "Play",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
                IconButton(onClick = onQueue) {
                    Icon(
                        imageVector = Icons.Rounded.QueueMusic,
                        contentDescription = "Queue",
                        tint = Color.White
                    )
                }
            }
            LinearProgressIndicator(
                progress = {
                    if (s.durationMs > 0) (position / s.durationMs.toFloat()).coerceIn(0f, 1f) else 0f
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 28.dp)
                    .fillMaxWidth()
                    .height(2.dp),
                color = Pulse.Cyan,
                trackColor = Color.Transparent
            )
        }
    }
}
