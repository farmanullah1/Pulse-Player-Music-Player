package com.pulse.player.ui.theme.immersive

import android.provider.Settings
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Forward10
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Replay10
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.Player
import com.pulse.player.playback.PlaybackUiState
import com.pulse.player.ui.components.formatTime
import com.pulse.player.ui.theme.Pulse
import kotlinx.coroutines.flow.StateFlow

internal object PulseIcons {
    val ChevronDown: ImageVector = Icons.Rounded.KeyboardArrowDown
    val ChevronUp: ImageVector = Icons.Rounded.KeyboardArrowUp
    val MoreVert: ImageVector = Icons.Rounded.MoreVert
    val Shuffle: ImageVector = Icons.Rounded.Shuffle
    val Repeat: ImageVector = Icons.Rounded.Repeat
    val RepeatOne: ImageVector = Icons.Rounded.RepeatOne
    val SkipPrevious: ImageVector = Icons.Rounded.SkipPrevious
    val SkipNext: ImageVector = Icons.Rounded.SkipNext
    val Replay10: ImageVector = Icons.Rounded.Replay10
    val Forward10: ImageVector = Icons.Rounded.Forward10
    val Play: ImageVector = Icons.Rounded.PlayArrow
    val Pause: ImageVector = Icons.Rounded.Pause
    val HeartFilled: ImageVector = Icons.Rounded.Favorite
    val Heart: ImageVector = Icons.Rounded.FavoriteBorder
    val Alarm: ImageVector = Icons.Rounded.Bedtime
    val PlaylistAdd: ImageVector = Icons.Rounded.PlaylistAdd
    val Queue: ImageVector = Icons.Rounded.QueueMusic
    val Tune: ImageVector = Icons.Rounded.Tune
    val Close: ImageVector = Icons.Rounded.Close
}

@Composable
fun rememberReducedMotion(): Boolean {
    val ctx = LocalContext.current
    return remember {
        try {
            Settings.Global.getFloat(ctx.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
        } catch (_: Exception) {
            false
        }
    }
}

fun fmt(ms: Long): String = formatTime(ms)

fun repeatLabel(mode: Int): String = when (mode) {
    Player.REPEAT_MODE_ONE -> "one"
    Player.REPEAT_MODE_ALL -> "all"
    else -> "off"
}

@Composable
fun PlayerTopBar(
    onBack: () -> Unit,
    onMore: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars)
            .height(56.dp)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
            Icon(PulseIcons.ChevronDown, contentDescription = "Close player", tint = Color.White)
        }
        Text(
            text = "Now playing",
            modifier = Modifier.weight(1f),
            color = Color.White.copy(0.85f),
            style = MaterialTheme.typography.labelLarge,
            textAlign = TextAlign.Center
        )
        IconButton(onClick = onMore, modifier = Modifier.size(48.dp)) {
            Icon(PulseIcons.MoreVert, contentDescription = "More options", tint = Color.White)
        }
    }
}

@Composable
fun SongInformation(
    s: PlaybackUiState,
    onShuffle: () -> Unit,
    onRepeat: () -> Unit,
    reducedMotion: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onShuffle, modifier = Modifier.size(48.dp)) {
                Icon(
                    PulseIcons.Shuffle,
                    contentDescription = if (s.shuffle) "Shuffle on" else "Shuffle off",
                    tint = if (s.shuffle) Pulse.Orange else Color.White.copy(0.8f)
                )
            }
            Text(
                text = s.title.ifBlank { "Unknown title" },
                modifier = Modifier
                    .weight(1f)
                    .then(
                        if (reducedMotion) Modifier
                        else Modifier.basicMarquee(
                            iterations = Int.MAX_VALUE,
                            initialDelayMillis = 1500,
                            velocity = 36.dp
                        )
                    ),
                color = Color.White,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                maxLines = 1,
                overflow = if (reducedMotion) TextOverflow.Ellipsis else TextOverflow.Clip,
                textAlign = TextAlign.Center
            )
            IconButton(onClick = onRepeat, modifier = Modifier.size(48.dp)) {
                Icon(
                    if (s.repeat == Player.REPEAT_MODE_ONE) PulseIcons.RepeatOne else PulseIcons.Repeat,
                    contentDescription = "Repeat ${repeatLabel(s.repeat)}",
                    tint = if (s.repeat != Player.REPEAT_MODE_OFF) Pulse.Orange else Color.White.copy(0.8f)
                )
            }
        }
        Text(
            text = listOf(s.artist, s.album).filter { it.isNotBlank() }.joinToString(" · ").ifBlank { "Unknown artist" },
            color = Color.White.copy(0.7f),
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun PlaybackSeekBar(
    s: PlaybackUiState,
    pos: StateFlow<Long>,
    canSeek: Boolean,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val p by pos.collectAsStateWithLifecycle()
    var drag by remember { mutableStateOf<Float?>(null) }
    val dur = s.durationMs
    val frac = drag ?: if (dur > 0) (p.toFloat() / dur).coerceIn(0f, 1f) else 0f

    Column(modifier = modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
        Slider(
            value = frac,
            onValueChange = { if (canSeek && dur > 0) drag = it },
            onValueChangeFinished = {
                drag?.let { onSeek((it * dur).toLong()) }
                drag = null
            },
            enabled = canSeek && dur > 0,
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = Color.White,
                inactiveTrackColor = Color.White.copy(0.25f),
                disabledThumbColor = Color.White.copy(0.5f),
                disabledActiveTrackColor = Color.White.copy(0.35f)
            ),
            modifier = Modifier.semantics {
                stateDescription = "${fmt(p)} of ${if (dur > 0) fmt(dur) else "unknown length"}"
            }
        )
        Row(Modifier.fillMaxWidth()) {
            Text(
                text = fmt(((drag ?: frac) * dur).toLong().takeIf { dur > 0 } ?: p),
                color = Color.White.copy(0.8f),
                style = MaterialTheme.typography.labelMedium
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = if (dur > 0) fmt(dur) else "--:--",
                color = Color.White.copy(0.8f),
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}

@Composable
fun PrimaryPlaybackControls(
    s: PlaybackUiState,
    canSeek: Boolean,
    onPrev: () -> Unit,
    onBack10: () -> Unit,
    onToggle: () -> Unit,
    onFwd10: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPrev, modifier = Modifier.size(56.dp)) {
            Icon(PulseIcons.SkipPrevious, contentDescription = "Previous song", modifier = Modifier.size(34.dp), tint = Color.White)
        }
        IconButton(onClick = onBack10, modifier = Modifier.size(48.dp), enabled = canSeek) {
            Icon(PulseIcons.Replay10, contentDescription = "Back 10 seconds", modifier = Modifier.size(28.dp), tint = Color.White.copy(if (canSeek) 0.9f else 0.35f))
        }
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(Color.White)
                .clickable(
                    onClickLabel = if (s.playWhenReady) "Pause" else "Play",
                    role = Role.Button
                ) { onToggle() },
            contentAlignment = Alignment.Center
        ) {
            if (s.buffering && s.playWhenReady) {
                CircularProgressIndicator(
                    modifier = Modifier.size(38.dp),
                    color = Color(0xFF14141A),
                    strokeWidth = 3.dp
                )
            } else {
                Crossfade(targetState = s.playWhenReady, animationSpec = tween(120), label = "pp") { playing ->
                    Icon(
                        imageVector = if (playing) PulseIcons.Pause else PulseIcons.Play,
                        contentDescription = null,
                        modifier = Modifier.size(38.dp),
                        tint = Color(0xFF14141A)
                    )
                }
            }
        }
        IconButton(onClick = onFwd10, modifier = Modifier.size(48.dp), enabled = canSeek) {
            Icon(PulseIcons.Forward10, contentDescription = "Forward 10 seconds", modifier = Modifier.size(28.dp), tint = Color.White.copy(if (canSeek) 0.9f else 0.35f))
        }
        IconButton(onClick = onNext, modifier = Modifier.size(56.dp)) {
            Icon(PulseIcons.SkipNext, contentDescription = "Next song", modifier = Modifier.size(34.dp), tint = Color.White)
        }
    }
}

@Composable
fun SecondaryPlayerActions(
    isFav: Boolean,
    sleepActive: Boolean,
    onFav: () -> Unit,
    onSleep: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onQueue: () -> Unit,
    onTune: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        ActionIcon(
            icon = if (isFav) PulseIcons.HeartFilled else PulseIcons.Heart,
            label = if (isFav) "Remove from favourites" else "Add to favourites",
            tint = if (isFav) Pulse.Orange else Color.White,
            onClick = onFav
        )
        ActionIcon(
            icon = PulseIcons.Alarm,
            label = if (sleepActive) "Sleep timer active" else "Sleep timer",
            tint = if (sleepActive) Pulse.Orange else Color.White,
            onClick = onSleep
        )
        ActionIcon(
            icon = PulseIcons.PlaylistAdd,
            label = "Add to playlist",
            tint = Color.White,
            onClick = onAddToPlaylist
        )
        ActionIcon(
            icon = PulseIcons.Queue,
            label = "Open queue",
            tint = Color.White,
            onClick = onQueue
        )
        ActionIcon(
            icon = PulseIcons.Tune,
            label = "Equalizer and sound",
            tint = Color.White,
            onClick = onTune
        )
    }
}

@Composable
private fun ActionIcon(icon: ImageVector, label: String, tint: Color, onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.size(48.dp)) {
        Icon(icon, contentDescription = label, tint = tint)
    }
}

