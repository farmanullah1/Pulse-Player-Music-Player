package com.pulse.player.ui.theme.immersive

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pulse.player.data.db.LyricsEntity
import com.pulse.player.data.lyrics.LrcParser
import com.pulse.player.playback.PlaybackUiState
import com.pulse.player.ui.components.Artwork
import com.pulse.player.ui.screens.Screen
import com.pulse.player.ui.theme.Pulse
import com.pulse.player.ui.themes.PlayerThemes
import com.pulse.player.ui.viewmodel.MainViewModel

@Composable
fun LyricsContent(
    lyrics: LyricsEntity?,
    posMs: Long,
    onImport: () -> Unit,
    modifier: Modifier = Modifier
) {
    when {
        lyrics == null -> {
            Column(
                modifier = modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "No lyrics for this song",
                    color = Color.White.copy(0.8f),
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(Modifier.height(16.dp))
                FilledTonalButton(onClick = onImport) {
                    Text("Import .lrc or .txt")
                }
            }
        }
        lyrics.synced -> {
            val lines = remember(lyrics.trackId, lyrics.importedAtMs) {
                LrcParser.parse(lyrics.text).orEmpty()
            }
            val active = LrcParser.activeIndex(lines, posMs)
            val st = rememberLazyListState()

            LaunchedEffect(active) {
                if (active >= 0) {
                    st.animateScrollToItem((active - 2).coerceAtLeast(0))
                }
            }

            LazyColumn(
                modifier = modifier,
                state = st,
                contentPadding = PaddingValues(vertical = 80.dp)
            ) {
                itemsIndexed(lines) { i, l ->
                    Text(
                        text = l.text.ifBlank { "♪" },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 28.dp, vertical = 8.dp),
                        textAlign = TextAlign.Center,
                        color = if (i == active) Color.White else Color.White.copy(0.45f),
                        fontSize = if (i == active) 22.sp else 18.sp,
                        fontWeight = if (i == active) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
        else -> {
            LazyColumn(modifier = modifier, contentPadding = PaddingValues(24.dp)) {
                item {
                    Text(
                        text = lyrics.text,
                        color = Color.White.copy(0.9f),
                        lineHeight = 26.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
fun ErrorCard(
    error: String,
    onRetry: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.padding(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xD92A1010)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = error,
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = onRetry,
                    colors = ButtonDefaults.buttonColors(containerColor = Pulse.Orange)
                ) {
                    Text("Retry")
                }
                OutlinedButton(
                    onClick = onSkip,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                ) {
                    Text("Skip")
                }
            }
        }
    }
}

@Composable
fun Foreground(
    s: PlaybackUiState,
    showLyrics: Boolean,
    onToggleLyrics: (Boolean) -> Unit,
    lyrics: LyricsEntity?,
    onImportLyrics: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pos by com.pulse.player.ui.theme.immersive.LocalPlaybackPosition.current.collectAsStateWithLifecycle(0L)

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = !showLyrics,
                onClick = { onToggleLyrics(false) },
                label = { Text("Artwork") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Pulse.Orange,
                    selectedLabelColor = Color.White,
                    containerColor = Color.White.copy(0.12f),
                    labelColor = Color.White.copy(0.8f)
                )
            )
            FilterChip(
                selected = showLyrics,
                onClick = { onToggleLyrics(true) },
                label = { Text("Lyrics") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Pulse.Orange,
                    selectedLabelColor = Color.White,
                    containerColor = Color.White.copy(0.12f),
                    labelColor = Color.White.copy(0.8f)
                )
            )
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            if (showLyrics) {
                LyricsContent(
                    lyrics = lyrics,
                    posMs = pos,
                    onImport = onImportLyrics,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(260.dp)
                        .shadow(16.dp, RoundedCornerShape(20.dp))
                        .clip(RoundedCornerShape(20.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Artwork(
                        uri = s.artworkUri,
                        size = 260.dp,
                        radius = 20.dp,
                        px = 512
                    )
                }
            }
        }
    }
}

internal val LocalPlaybackPosition = androidx.compose.runtime.compositionLocalOf<kotlinx.coroutines.flow.StateFlow<Long>> {
    error("No position flow provided")
}

@Composable
fun Controls(
    s: PlaybackUiState,
    vm: MainViewModel,
    canSeek: Boolean,
    fav: Boolean,
    reduced: Boolean,
    onOpenQueue: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        SongInformation(
            s = s,
            onShuffle = vm::toggleShuffle,
            onRepeat = vm::cycleRepeat,
            reducedMotion = reduced
        )
        Spacer(Modifier.height(8.dp))
        PlaybackSeekBar(
            s = s,
            pos = vm.player.position,
            canSeek = canSeek,
            onSeek = vm.player::seekTo
        )
        Spacer(Modifier.height(8.dp))
        PrimaryPlaybackControls(
            s = s,
            canSeek = canSeek,
            onPrev = vm.player::previous,
            onBack10 = { vm.player.skipBy(-10_000L) },
            onToggle = vm.player::togglePlayPause,
            onFwd10 = { vm.player.skipBy(10_000L) },
            onNext = vm.player::next
        )
        Spacer(Modifier.height(12.dp))
        SecondaryPlayerActions(
            isFav = fav,
            sleepActive = vm.sleepOpen.value,
            onFav = { s.mediaId?.let { vm.toggleFavorite(it) } },
            onSleep = vm::openSleepSheet,
            onAddToPlaylist = { vm.openAddToPlaylist() },
            onQueue = onOpenQueue,
            onTune = { vm.go(Screen.Equalizer) }
        )
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
fun ImmersivePlayerLayout(
    s: PlaybackUiState,
    vm: MainViewModel,
    onBack: () -> Unit
) {
    val reduced = rememberReducedMotion()
    val cfg = LocalConfiguration.current
    val wide = cfg.screenWidthDp >= 600
    val short = cfg.screenHeightDp < 640

    var showLyrics by rememberSaveable { mutableStateOf(false) }
    var queueOpen by rememberSaveable { mutableStateOf(false) }

    val queue by vm.queueItems.collectAsStateWithLifecycle()
    val fav by vm.isFavorite(s.mediaId).collectAsStateWithLifecycle(false)
    val lyrics by vm.lyrics(s.mediaId).collectAsStateWithLifecycle(null)
    val canSeek = s.canSeek && s.durationMs > 0

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { s.mediaId?.let { id -> vm.importLyrics(id, uri) } }
    }

    BackHandler(enabled = queueOpen) {
        queueOpen = false
    }

    androidx.compose.runtime.CompositionLocalProvider(
        LocalPlaybackPosition provides vm.player.position
    ) {
        Box(Modifier.fillMaxSize()) {
            val themeColors = PlayerThemes.firstOrNull { it.id == "immersive" }?.colors
                ?: listOf(Color(0xFF1B1B22), Color.Black)
            ArtworkBackground(
                art = s.artRef,
                fallback = themeColors,
                modifier = Modifier.fillMaxSize()
            )

            if (wide) {
                Row(Modifier.fillMaxSize()) {
                    Column(
                        Modifier
                            .weight(0.55f)
                            .fillMaxHeight()
                    ) {
                        PlayerTopBar(onBack = onBack, onMore = vm::openTrackOptions)
                        Foreground(
                            s = s,
                            showLyrics = showLyrics,
                            onToggleLyrics = { showLyrics = it },
                            lyrics = lyrics,
                            onImportLyrics = {
                                importLauncher.launch(
                                    arrayOf("text/plain", "application/octet-stream")
                                )
                            },
                            modifier = Modifier.weight(1f)
                        )
                        Controls(
                            s = s,
                            vm = vm,
                            canSeek = canSeek,
                            fav = fav,
                            reduced = reduced,
                            onOpenQueue = { queueOpen = !queueOpen }
                        )
                    }
                    QueuePanel(
                        queue = queue,
                        currentIndex = s.index,
                        playing = s.isPlaying,
                        expanded = true,
                        onToggle = {},
                        onPlayAt = vm::playAt,
                        onRemove = vm::removeAt,
                        modifier = Modifier
                            .weight(0.45f)
                            .fillMaxHeight()
                    )
                }
            } else {
                Column(
                    Modifier
                        .fillMaxSize()
                        .padding(bottom = if (queueOpen) 0.dp else 160.dp)
                ) {
                    PlayerTopBar(onBack = onBack, onMore = vm::openTrackOptions)
                    if (!short) {
                        Foreground(
                            s = s,
                            showLyrics = showLyrics,
                            onToggleLyrics = { showLyrics = it },
                            lyrics = lyrics,
                            onImportLyrics = {
                                importLauncher.launch(
                                    arrayOf("text/plain", "application/octet-stream")
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .padding(vertical = 8.dp)
                        )
                    } else {
                        Spacer(Modifier.weight(1f))
                    }
                    Controls(
                        s = s,
                        vm = vm,
                        canSeek = canSeek,
                        fav = fav,
                        reduced = reduced,
                        onOpenQueue = { queueOpen = !queueOpen }
                    )
                }

                val panelHeight by animateDpAsState(
                    targetValue = if (queueOpen) (cfg.screenHeightDp * 0.7f).dp else 160.dp,
                    animationSpec = if (reduced) snap() else tween(300),
                    label = "queue_height"
                )

                QueuePanel(
                    queue = queue,
                    currentIndex = s.index,
                    playing = s.isPlaying,
                    expanded = queueOpen,
                    onToggle = { queueOpen = !queueOpen },
                    onPlayAt = vm::playAt,
                    onRemove = vm::removeAt,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(panelHeight)
                        .navigationBarsPadding()
                )
            }

            s.error?.let { err ->
                ErrorCard(
                    error = err,
                    onRetry = vm::retry,
                    onSkip = vm::skip,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }
}

