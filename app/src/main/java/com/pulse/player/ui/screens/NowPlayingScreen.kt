package com.pulse.player.ui.screens

import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.Player
import com.pulse.player.data.lyrics.Lrc
import com.pulse.player.data.lyrics.LrcLine
import com.pulse.player.data.scan.ArtworkLoader
import com.pulse.player.playback.PlaybackUiState
import com.pulse.player.playback.PlayerController
import com.pulse.player.playback.RingtoneHelper
import com.pulse.player.ui.components.Artwork
import com.pulse.player.ui.components.AuroraBackground
import com.pulse.player.ui.components.GlowArtwork
import com.pulse.player.ui.components.HeartButton
import com.pulse.player.ui.components.formatTime
import com.pulse.player.ui.theme.Pulse
import com.pulse.player.ui.themes.AcousticDisc
import com.pulse.player.ui.themes.MountainSunset
import com.pulse.player.ui.themes.PlayerLayout
import com.pulse.player.ui.themes.PlayerThemes
import com.pulse.player.ui.themes.StarField
import com.pulse.player.ui.themes.VinylDisc
import com.pulse.player.ui.themes.WavesCanvas
import com.pulse.player.ui.viewmodel.MainViewModel
import java.io.File
import kotlinx.coroutines.flow.StateFlow

@Composable
fun NowPlayingScreen(vm: MainViewModel) {
    val s by vm.player.state.collectAsStateWithLifecycle()
    val ctx = LocalContext.current
    val fav by vm.isCurrentFavorite.collectAsStateWithLifecycle()
    var dominant by remember { mutableIntStateOf(0xFF2A0826.toInt()) }
    var showLyrics by remember { mutableStateOf(false) }

    LaunchedEffect(s.mediaId) {
        s.artworkUri?.let { u ->
            ArtworkLoader.load(ctx, u, 128)?.let {
                dominant = ArtworkLoader.dominant(it)
            }
        }
    }

    AuroraBackground(
        accent = Color(dominant),
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = vm::closeNowPlaying) {
                    Icon(
                        imageVector = Icons.Rounded.KeyboardArrowDown,
                        contentDescription = "Collapse player",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
                Spacer(Modifier.weight(1f))
                SongLyricsSegment(showLyrics) { showLyrics = it }
                Spacer(Modifier.weight(1f))
                PlayerTopMenu(vm)
            }

            Spacer(Modifier.height(16.dp))

            if (showLyrics) {
                LyricsView(
                    vm = vm,
                    mediaId = s.mediaId,
                    modifier = Modifier.weight(1f)
                )
            } else {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .pointerInput(s.mediaId) {
                            detectTapGestures(
                                onDoubleTap = {
                                    vm.toggleFavoriteCurrent()
                                }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    PlayerThemeHost(vm, s, Color(dominant))
                }
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
                IconButton(onClick = { vm.openSleep() }) {
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
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
fun NowPlayingPanel(vm: MainViewModel, modifier: Modifier = Modifier) {
    val s by vm.player.state.collectAsStateWithLifecycle()
    val fav by vm.isCurrentFavorite.collectAsStateWithLifecycle()

    Surface(
        modifier = modifier,
        color = Pulse.Surface,
        tonalElevation = 8.dp
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Now Playing", style = MaterialTheme.typography.titleLarge, color = Color.White)
            Spacer(Modifier.height(24.dp))
            Box(Modifier.weight(1f), Alignment.Center) {
                PlayerThemeHost(vm, s, Pulse.Cyan)
            }
            Spacer(Modifier.height(16.dp))
            Text(
                text = s.title,
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(text = s.artist, style = MaterialTheme.typography.bodySmall, color = Pulse.TextMid)
            Spacer(Modifier.height(16.dp))
            SeekBar(vm.player.position, s.durationMs, onSeek = vm.player::seekTo)
            Spacer(Modifier.height(8.dp))
            TransportRow(s, vm.player)
            Spacer(Modifier.height(8.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                HeartButton(fav, { vm.toggleFavoriteCurrent() })
                IconButton(onClick = { vm.openQueue() }) {
                    Icon(Icons.Rounded.QueueMusic, "Queue", tint = Color.White)
                }
                IconButton(onClick = { vm.go(Screen.Equalizer) }) {
                    Icon(Icons.Rounded.Equalizer, "Equalizer", tint = Color.White)
                }
            }
        }
    }
}

@Composable
fun PlayerThemeHost(vm: MainViewModel, s: PlaybackUiState, glowColor: Color = Pulse.Cyan) {
    val id by vm.playerThemeId.collectAsStateWithLifecycle()
    val theme = PlayerThemes.firstOrNull { it.id == id } ?: PlayerThemes[0]

    Crossfade(theme, animationSpec = tween(400), label = "ptheme") { t ->
        when (t.layout) {
            PlayerLayout.VINYL -> VinylDisc(s)
            PlayerLayout.ACOUSTIC -> AcousticDisc(s)
            PlayerLayout.COAST -> Box(Modifier.size(280.dp), Alignment.Center) {
                WavesCanvas(Modifier.fillMaxSize())
                Artwork(s.artworkUri, 220.dp, radius = 16.dp, px = 512)
            }
            PlayerLayout.STARRY -> Box(Modifier.size(280.dp), Alignment.Center) {
                StarField(Modifier.fillMaxSize())
                Artwork(s.artworkUri, 220.dp, radius = 16.dp, px = 512)
            }
            PlayerLayout.SHEET_QUEUE -> Box(Modifier.size(280.dp), Alignment.Center) {
                MountainSunset(Modifier.fillMaxSize())
                Artwork(s.artworkUri, 220.dp, radius = 16.dp, px = 512)
            }
            else -> GlowArtwork(
                uri = s.artworkUri,
                size = 280.dp,
                glow = glowColor
            )
        }
    }
}

@Composable
fun SongLyricsSegment(showLyrics: Boolean, onSelect: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Pulse.Elevated)
            .padding(4.dp)
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(if (!showLyrics) Color.White else Color.Transparent)
                .clickable { onSelect(false) }
                .padding(horizontal = 14.dp, vertical = 6.dp)
        ) {
            Text(
                "Song",
                fontWeight = FontWeight.Bold,
                color = if (!showLyrics) Color.Black else Pulse.TextMid,
                style = MaterialTheme.typography.bodySmall
            )
        }
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(if (showLyrics) Color.White else Color.Transparent)
                .clickable { onSelect(true) }
                .padding(horizontal = 14.dp, vertical = 6.dp)
        ) {
            Text(
                "Lyrics",
                fontWeight = FontWeight.Bold,
                color = if (showLyrics) Color.Black else Pulse.TextMid,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
fun LyricsView(
    vm: MainViewModel,
    mediaId: String,
    modifier: Modifier = Modifier
) {
    val ctx = LocalContext.current
    var lrcLines by remember { mutableStateOf<List<LrcLine>>(emptyList()) }
    val listState = rememberLazyListState()
    val currentPosMs by vm.player.position.collectAsStateWithLifecycle()

    fun loadStoredLyrics(id: String) {
        val safeId = id.replace(Regex("[^a-zA-Z0-9._-]"), "_")
        val lrcFile = File(File(ctx.filesDir, "lyrics"), "$safeId.lrc")
        if (lrcFile.exists() && lrcFile.length() > 0) {
            runCatching {
                val text = lrcFile.readText()
                val parsed = Lrc.parse(text)
                lrcLines = if (parsed.isNotEmpty()) {
                    parsed
                } else {
                    text.lineSequence()
                        .map { it.trim() }
                        .filter { it.isNotBlank() }
                        .map { LrcLine(-1L, it) }
                        .toList()
                }
            }
        } else {
            lrcLines = emptyList()
        }
    }

    LaunchedEffect(mediaId) {
        loadStoredLyrics(mediaId)
    }

    val lrcPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            runCatching {
                ctx.contentResolver.openInputStream(uri)?.use { stream ->
                    val text = stream.bufferedReader().use { it.readText().take(512 * 1024) }
                    val dir = File(ctx.filesDir, "lyrics").apply { mkdirs() }
                    val safeId = mediaId.replace(Regex("[^a-zA-Z0-9._-]"), "_")
                    File(dir, "$safeId.lrc").writeText(text)
                    val parsed = Lrc.parse(text)
                    lrcLines = if (parsed.isNotEmpty()) {
                        parsed
                    } else {
                        text.lineSequence()
                            .map { it.trim() }
                            .filter { it.isNotBlank() }
                            .map { LrcLine(-1L, it) }
                            .toList()
                    }
                }
            }
        }
    }

    val activeIdx = remember(lrcLines, currentPosMs) {
        if (lrcLines.isNotEmpty() && lrcLines.first().timeMs >= 0) {
            Lrc.activeIndex(lrcLines, currentPosMs)
        } else {
            -1
        }
    }

    LaunchedEffect(activeIdx) {
        if (activeIdx >= 0 && !listState.isScrollInProgress) {
            listState.animateScrollToItem(activeIdx)
        }
    }

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        if (lrcLines.isEmpty()) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("No lyrics found for this track", color = Pulse.TextMid)
                Button(
                    onClick = { lrcPicker.launch(arrayOf("*/*", "text/*")) },
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Pulse.Elevated,
                        contentColor = Pulse.Cyan
                    )
                ) {
                    Text("Choose .lrc file")
                }
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                itemsIndexed(lrcLines) { idx, line ->
                    val isActive = idx == activeIdx
                    Text(
                        text = line.text,
                        style = if (isActive) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
                        color = if (isActive) Pulse.Cyan else Pulse.TextLow,
                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = line.timeMs >= 0) {
                                vm.player.seekTo(line.timeMs)
                            }
                            .padding(vertical = 8.dp, horizontal = 24.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun PlayerTopMenu(vm: MainViewModel) {
    var expanded by remember { mutableStateOf(false) }
    val ctx = LocalContext.current
    val s by vm.player.state.collectAsStateWithLifecycle()

    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(Icons.Rounded.MoreVert, "More actions", tint = Color.White)
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(Pulse.Elevated)
        ) {
            DropdownMenuItem(
                text = { Text("Player Theme") },
                onClick = {
                    expanded = false
                    vm.go(Screen.PlayerTheme)
                }
            )
            DropdownMenuItem(
                text = { Text("Equalizer") },
                onClick = {
                    expanded = false
                    vm.go(Screen.Equalizer)
                }
            )
            DropdownMenuItem(
                text = { Text("Set as Ringtone") },
                onClick = {
                    expanded = false
                    s.artworkUri?.let {
                        RingtoneHelper.setRingtone(ctx, Uri.parse(it), RingtoneManager.TYPE_RINGTONE)
                    }
                }
            )
            DropdownMenuItem(
                text = { Text("Share Track") },
                onClick = {
                    expanded = false
                    s.artworkUri?.let {
                        val share = Intent(Intent.ACTION_SEND).apply {
                            type = "audio/*"
                            putExtra(Intent.EXTRA_STREAM, Uri.parse(it))
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        ctx.startActivity(Intent.createChooser(share, "Share ${s.title}"))
                    }
                }
            )
        }
    }
}

@Composable
fun SeekBar(position: StateFlow<Long>, duration: Long, onSeek: (Long) -> Unit) {
    val pos by position.collectAsStateWithLifecycle()
    var drag by remember { mutableStateOf<Float?>(null) }
    val value = drag ?: if (duration > 0) (pos / duration.toFloat()).coerceIn(0f, 1f) else 0f

    Column {
        Slider(
            value = value.coerceIn(0f, 1f),
            onValueChange = { drag = it },
            onValueChangeFinished = {
                drag?.let { onSeek((it * duration).toLong()) }
                drag = null
            },
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = Color.White,
                inactiveTrackColor = Color.White.copy(0.25f)
            )
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp)
        ) {
            Text(
                formatTime(((drag ?: value) * duration).toLong()),
                color = Pulse.TextMid,
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(Modifier.weight(1f))
            Text(
                formatTime(duration),
                color = Pulse.TextMid,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
fun TransportRow(s: PlaybackUiState, p: PlayerController) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = p::toggleShuffle) {
            Icon(
                imageVector = Icons.Rounded.Shuffle,
                contentDescription = "Shuffle ${if (s.shuffle) "on" else "off"}",
                tint = if (s.shuffle) Pulse.Cyan else Color.White
            )
        }
        IconButton(onClick = p::previous, modifier = Modifier.size(56.dp)) {
            Icon(
                imageVector = Icons.Rounded.SkipPrevious,
                contentDescription = "Previous",
                tint = Color.White,
                modifier = Modifier.size(36.dp)
            )
        }
        FilledIconButton(
            onClick = p::togglePlay,
            modifier = Modifier.size(76.dp),
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = Color.White,
                contentColor = Color.Black
            )
        ) {
            AnimatedContent(
                targetState = s.isPlaying,
                transitionSpec = { (scaleIn() + fadeIn()) togetherWith (scaleOut() + fadeOut()) },
                label = "main_play"
            ) { playing ->
                Icon(
                    imageVector = if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = if (playing) "Pause" else "Play",
                    modifier = Modifier.size(40.dp)
                )
            }
        }
        IconButton(onClick = p::next, modifier = Modifier.size(56.dp)) {
            Icon(
                imageVector = Icons.Rounded.SkipNext,
                contentDescription = "Next",
                tint = Color.White,
                modifier = Modifier.size(36.dp)
            )
        }
        IconButton(onClick = p::cycleRepeat) {
            Icon(
                imageVector = if (s.repeat == Player.REPEAT_MODE_ONE) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
                contentDescription = "Repeat mode",
                tint = if (s.repeat != Player.REPEAT_MODE_OFF) Pulse.Cyan else Color.White
            )
        }
    }
}
