package com.pulse.player.ui.screens.layouts

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddCircleOutline
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Checkroom
import androidx.compose.material.icons.rounded.DashboardCustomize
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import com.pulse.player.ui.components.AtmosphericBackground

/** Queue item model used by the stateless atmospheric player. */
data class Track(
    val id: String,
    val title: String,
    val artist: String,
    val duration: String,
    val isPlaying: Boolean = false
)

data class AtmosphericQueueUiState(
    val title: String,
    val artist: String,
    val elapsedTime: String,
    val totalDuration: String,
    val progress: Float,
    val isPlaying: Boolean,
    val isFavorite: Boolean,
    val shuffleEnabled: Boolean,
    val repeatMode: Int = Player.REPEAT_MODE_OFF,
    val queue: List<Track> = emptyList()
)

/**
 * Stateless, responsive split player. Playback, settings and navigation stay
 * outside this composable so changing layouts never interrupts the player.
 */
@Composable
fun AtmosphericQueuePlayerLayout(
    uiState: AtmosphericQueueUiState,
    onPlayPauseClick: () -> Unit,
    onPreviousClick: () -> Unit,
    onNextClick: () -> Unit,
    onSeek: (Float) -> Unit,
    onTrackSelect: (Track) -> Unit,
    onToggleFavorite: () -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    onOpenTheme: () -> Unit,
    onOpenPicker: () -> Unit,
    onCollapse: () -> Unit,
    onOpenMenu: () -> Unit = {},
    onTrackOptions: (Track) -> Unit = {},
    onOpenSleep: () -> Unit = {},
    onAddToPlaylist: () -> Unit = {},
    onOpenQueue: () -> Unit = {},
    onOpenEqualizer: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFF020706))
        ) {
            AtmosphericUpperSection(
                uiState = uiState,
                onPlayPauseClick = onPlayPauseClick,
                onPreviousClick = onPreviousClick,
                onNextClick = onNextClick,
                onSeek = onSeek,
                onToggleFavorite = onToggleFavorite,
                onToggleShuffle = onToggleShuffle,
                onCycleRepeat = onCycleRepeat,
                onOpenTheme = onOpenTheme,
                onOpenPicker = onOpenPicker,
                onOpenMenu = onOpenMenu,
                onCollapse = onCollapse,
                onOpenSleep = onOpenSleep,
                onAddToPlaylist = onAddToPlaylist,
                onOpenQueue = onOpenQueue,
                onOpenEqualizer = onOpenEqualizer,
                modifier = Modifier.weight(1.2f)
            )
            QueueSection(
                tracks = uiState.queue,
                onTrackSelect = onTrackSelect,
                onTrackOptions = onTrackOptions,
                modifier = Modifier.weight(0.95f)
            )
        }
    }
}

@Composable
private fun AtmosphericUpperSection(
    uiState: AtmosphericQueueUiState,
    onPlayPauseClick: () -> Unit,
    onPreviousClick: () -> Unit,
    onNextClick: () -> Unit,
    onSeek: (Float) -> Unit,
    onToggleFavorite: () -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    onOpenTheme: () -> Unit,
    onOpenPicker: () -> Unit,
    onOpenMenu: () -> Unit,
    onCollapse: () -> Unit,
    onOpenSleep: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onOpenQueue: () -> Unit,
    onOpenEqualizer: () -> Unit,
    modifier: Modifier
) {
    AtmosphericBackground(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().height(56.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onCollapse) {
                    Icon(Icons.Rounded.KeyboardArrowDown, "Collapse player", tint = Color.White)
                }
                SongLyricsTabs(Modifier.weight(1f))
                IconButton(onClick = onOpenTheme) {
                    Icon(Icons.Rounded.Checkroom, "Open skin themes", tint = Color.White)
                }
                IconButton(onClick = onOpenPicker) {
                    Icon(Icons.Rounded.DashboardCustomize, "Change player layout", tint = Color.White)
                }
                IconButton(onClick = onOpenMenu) {
                    Icon(Icons.Rounded.MoreVert, "More player actions", tint = Color.White)
                }
            }

            Column(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = uiState.title.ifBlank { "No track selected" },
                    color = Color.White,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = uiState.artist.ifBlank { "Unknown artist" },
                    color = Color.White.copy(alpha = 0.72f),
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(uiState.elapsedTime, color = Color.White.copy(alpha = 0.68f), style = MaterialTheme.typography.labelSmall)
                    Slider(
                        value = uiState.progress.coerceIn(0f, 1f),
                        onValueChange = onSeek,
                        modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                        colors = SliderDefaults.colors(
                            thumbColor = Color.White,
                            activeTrackColor = Color.White,
                            inactiveTrackColor = Color.White.copy(alpha = 0.28f)
                        )
                    )
                    Text(uiState.totalDuration, color = Color.White.copy(alpha = 0.68f), style = MaterialTheme.typography.labelSmall)
                }
            }

            PlaybackControls(
                uiState = uiState,
                onPlayPauseClick = onPlayPauseClick,
                onPreviousClick = onPreviousClick,
                onNextClick = onNextClick,
                onToggleShuffle = onToggleShuffle,
                onCycleRepeat = onCycleRepeat
            )
            QuickActions(
                uiState = uiState,
                onToggleFavorite = onToggleFavorite,
                onOpenSleep = onOpenSleep,
                onAddToPlaylist = onAddToPlaylist,
                onOpenQueue = onOpenQueue,
                onOpenEqualizer = onOpenEqualizer
            )
        }
    }
}

@Composable
private fun SongLyricsTabs(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Song", color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.width(18.dp))
        Text("Lyrics", color = Color.White.copy(alpha = 0.48f), style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun PlaybackControls(
    uiState: AtmosphericQueueUiState,
    onPlayPauseClick: () -> Unit,
    onPreviousClick: () -> Unit,
    onNextClick: () -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onToggleShuffle) {
            Icon(Icons.Rounded.Shuffle, "Shuffle", tint = if (uiState.shuffleEnabled) Color(0xFFD4AF37) else Color.White.copy(alpha = 0.72f))
        }
        IconButton(onClick = onPreviousClick, modifier = Modifier.size(52.dp)) {
            Icon(Icons.Rounded.SkipPrevious, "Previous track", tint = Color.White, modifier = Modifier.size(30.dp))
        }
        IconButton(
            onClick = onPlayPauseClick,
            modifier = Modifier.size(64.dp),
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = Color.Black.copy(alpha = 0.52f),
                contentColor = Color.White
            )
        ) {
            Icon(
                if (uiState.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                if (uiState.isPlaying) "Pause" else "Play",
                modifier = Modifier.size(34.dp)
            )
        }
        IconButton(onClick = onNextClick, modifier = Modifier.size(52.dp)) {
            Icon(Icons.Rounded.SkipNext, "Next track", tint = Color.White, modifier = Modifier.size(30.dp))
        }
        IconButton(onClick = onCycleRepeat) {
            Icon(
                if (uiState.repeatMode == Player.REPEAT_MODE_ONE) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
                "Repeat",
                tint = if (uiState.repeatMode != Player.REPEAT_MODE_OFF) Color(0xFFD4AF37) else Color.White.copy(alpha = 0.72f)
            )
        }
    }
}

@Composable
private fun QuickActions(
    uiState: AtmosphericQueueUiState,
    onToggleFavorite: () -> Unit,
    onOpenSleep: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onOpenQueue: () -> Unit,
    onOpenEqualizer: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 2.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onToggleFavorite) {
            Icon(if (uiState.isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder, "Favorite", tint = if (uiState.isFavorite) Color(0xFFFFB4AB) else Color.White.copy(alpha = 0.82f))
        }
        IconButton(onClick = onOpenSleep) { Icon(Icons.Rounded.Bedtime, "Sleep timer", tint = Color.White.copy(alpha = 0.82f)) }
        IconButton(onClick = onAddToPlaylist) { Icon(Icons.Rounded.AddCircleOutline, "Add to playlist", tint = Color.White.copy(alpha = 0.82f)) }
        IconButton(onClick = onOpenQueue) { Icon(Icons.Rounded.QueueMusic, "Open queue", tint = Color.White.copy(alpha = 0.82f)) }
        IconButton(onClick = onOpenEqualizer) { Icon(Icons.Rounded.Equalizer, "Equalizer", tint = Color.White.copy(alpha = 0.82f)) }
    }
}

@Composable
private fun QueueSection(
    tracks: List<Track>,
    onTrackSelect: (Track) -> Unit,
    onTrackOptions: (Track) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Color(0xFFF9F9FB),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(Modifier.fillMaxSize().padding(top = 16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Up Next", color = Color(0xFF1C1B1F), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(8.dp))
                BadgedBox(badge = { Badge { Text(tracks.size.toString()) } }) {}
            }
            if (tracks.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Your queue is empty", color = Color(0xFF6F6B72), style = MaterialTheme.typography.bodyMedium)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(top = 4.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 12.dp)
                ) {
                    items(tracks, key = { it.id }) { track ->
                        QueueTrackRow(
                            track = track,
                            onClick = { onTrackSelect(track) },
                            onOptions = { onTrackOptions(track) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QueueTrackRow(track: Track, onClick: () -> Unit, onOptions: () -> Unit) {
    val activeColor = Color(0xFFD4AF37)
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(52.dp).clip(RoundedCornerShape(8.dp)).background(
                Brush.linearGradient(listOf(Color(0xFF294B49), Color(0xFF112120)))
            ),
            contentAlignment = Alignment.Center
        ) {
            if (track.isPlaying) EqualizerIndicator(activeColor) else Icon(Icons.Rounded.MusicNote, "", tint = Color.White.copy(alpha = 0.68f))
        }
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(
                text = track.title,
                color = if (track.isPlaying) activeColor else Color(0xFF1C1B1F),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${track.artist}  •  ${track.duration}",
                color = Color(0xFF77737A),
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        IconButton(onClick = onOptions, modifier = Modifier.size(48.dp)) {
            Icon(Icons.Rounded.MoreVert, "Track options", tint = Color(0xFF5E5A62))
        }
    }
}

@Composable
private fun EqualizerIndicator(color: Color) {
    val transition = rememberInfiniteTransition(label = "queue-equalizer")
    Row(
        modifier = Modifier.size(24.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        listOf(420, 620, 520).forEachIndexed { index, duration ->
            val height by transition.animateFloat(
                initialValue = 0.28f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(tween(duration), RepeatMode.Reverse),
                label = "bar-$index"
            )
            Box(Modifier.width(4.dp).height((20 * height).dp).clip(RoundedCornerShape(2.dp)).background(color))
        }
    }
}

private val previewTracks = listOf(
    Track("1", "Hurts So Good (Lyric...)", "AstridSofficialVEVO", "4:22", true),
    Track("2", "Dreams", "Fleetwood Mac", "4:17"),
    Track("3", "Midnight City", "M83", "4:03"),
    Track("4", "The Sound of Silence", "Disturbed", "4:08")
)

@Preview(showBackground = true, device = "spec:width=412dp,height=915dp,dpi=420")
@Composable
private fun AtmosphericQueuePlayerLayoutPreview() {
    AtmosphericQueuePlayerLayout(
        uiState = AtmosphericQueueUiState(
            title = "Hurts So Good (Lyric...)",
            artist = "AstridSofficialVEVO",
            elapsedTime = "0:06",
            totalDuration = "4:22",
            progress = 0.03f,
            isPlaying = true,
            isFavorite = true,
            shuffleEnabled = false,
            queue = previewTracks
        ),
        onPlayPauseClick = {},
        onPreviousClick = {},
        onNextClick = {},
        onSeek = {},
        onTrackSelect = {},
        onToggleFavorite = {},
        onToggleShuffle = {},
        onCycleRepeat = {},
        onOpenTheme = {},
        onOpenPicker = {},
        onCollapse = {},
        onOpenMenu = {},
        onTrackOptions = {}
    )
}
