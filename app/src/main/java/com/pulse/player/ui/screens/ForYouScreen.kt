package com.pulse.player.ui.screens

import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pulse.player.playback.Track
import com.pulse.player.ui.components.Artwork
import com.pulse.player.ui.components.PillButton
import com.pulse.player.ui.components.SectionHeader
import com.pulse.player.ui.components.SquareCard
import com.pulse.player.ui.components.WidgetTile
import com.pulse.player.ui.components.formatTime
import com.pulse.player.ui.theme.Pulse
import com.pulse.player.ui.viewmodel.MainViewModel
import kotlin.random.Random

@Composable
fun ForYouScreen(vm: MainViewModel) {
    val tracks by vm.tracks.collectAsStateWithLifecycle()
    val ps by vm.player.state.collectAsStateWithLifecycle()
    val today by vm.todayMinutes.collectAsStateWithLifecycle()
    val streak by vm.streakDays.collectAsStateWithLifecycle()
    val favCount by vm.favoriteCount.collectAsStateWithLifecycle()
    val recent = remember(tracks) { tracks.sortedByDescending { it.dateAddedMs }.take(12) }
    val top by vm.topPicks.collectAsStateWithLifecycle()
    val resumeTrack by vm.resume.collectAsStateWithLifecycle()

    if (tracks.isEmpty()) {
        EmptyLibraryCard(vm)
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        resumeTrack?.let { r ->
            item {
                ResumeBanner(r) { vm.playQueue(listOf(r), 0) }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                PillButton(
                    text = "Shuffle",
                    icon = Icons.Rounded.Shuffle,
                    onClick = {
                        if (tracks.isNotEmpty()) {
                            vm.playQueue(tracks, Random.nextInt(tracks.size), shuffle = true)
                        }
                    },
                    modifier = Modifier.weight(1f)
                )
                PillButton(
                    text = "Play",
                    icon = Icons.Rounded.PlayArrow,
                    onClick = {
                        if (tracks.isNotEmpty()) {
                            vm.playQueue(tracks, 0)
                        }
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    WidgetTile(
                        title = "Today",
                        value = "$today min",
                        subtitle = if (streak > 0) "$streak day streak" else null,
                        icon = Icons.Rounded.Headphones,
                        colors = listOf(Pulse.Violet, Pulse.Magenta),
                        modifier = Modifier.weight(1f)
                    ) {
                        vm.go(Screen.Recap)
                    }
                    WidgetTile(
                        title = "Favourites",
                        value = "$favCount songs",
                        icon = Icons.Rounded.Favorite,
                        colors = listOf(Pulse.Cyan, Pulse.Violet),
                        modifier = Modifier.weight(1f)
                    ) {
                        vm.go(Screen.Detail("system", "favorites"))
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    WidgetTile(
                        title = "Sleep timer",
                        value = if (ps.sleepRemainingMs > 0) formatTime(ps.sleepRemainingMs) else "Off",
                        icon = Icons.Rounded.Bedtime,
                        colors = listOf(Pulse.Coral, Pulse.Pink),
                        modifier = Modifier.weight(1f)
                    ) {
                        vm.openSleepSheet()
                    }
                    WidgetTile(
                        title = "Equalizer",
                        value = "Tune sound",
                        icon = Icons.Rounded.Equalizer,
                        colors = listOf(Pulse.Mint, Pulse.Cyan),
                        modifier = Modifier.weight(1f)
                    ) {
                        vm.go(Screen.Equalizer)
                    }
                }
            }
        }

        if (recent.isNotEmpty()) {
            item {
                SectionHeader("Recently added", badge = "New", action = "Play all") {
                    vm.playQueue(recent, 0)
                }
            }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(recent, key = { it.id }) { t ->
                        SquareCard(t, 132.dp) {
                            vm.playQueue(recent, recent.indexOf(t))
                        }
                    }
                }
            }
        }

        if (top.isNotEmpty()) {
            item {
                SectionHeader("Top picks for you")
            }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    items(top, key = { it.id }) { t ->
                        SquareCard(t, 168.dp) {
                            vm.playQueue(top, top.indexOf(t))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ResumeBanner(track: Track, onPlay: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.linearGradient(listOf(Pulse.PlumTop, Pulse.Elevated)))
            .clickable(onClick = onPlay)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Artwork(track.uri, 54.dp, radius = 12.dp, px = 160)
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = "Continue listening",
                    style = MaterialTheme.typography.bodySmall,
                    color = Pulse.Cyan
                )
                Text(
                    text = track.title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )
                Text(
                    text = track.artist,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmall,
                    color = Pulse.TextMid
                )
            }
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.PlayArrow,
                    contentDescription = "Play",
                    tint = Color.Black,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

@Composable
fun EmptyLibraryCard(vm: MainViewModel) {
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            vm.scan()
        }
    }
    val safLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            vm.addSafFolder(uri)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(Pulse.Surface)
                .padding(24.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(Pulse.Elevated),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Headphones, null, tint = Pulse.Cyan, modifier = Modifier.size(32.dp))
            }
            Text(
                text = "Your music library is empty",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Pulse Player plays local audio stored on your phone or SD card. Grant audio access or choose a Music folder to begin.",
                style = MaterialTheme.typography.bodyMedium,
                color = Pulse.TextMid,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = { vm.seedSampleTrack() },
                modifier = Modifier.fillMaxWidth(),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = Pulse.Cyan, contentColor = Color.Black)
            ) {
                Icon(Icons.Rounded.PlayArrow, null)
                Spacer(Modifier.width(8.dp))
                Text("Load Sample Track (Pulse Horizon)", fontWeight = FontWeight.Bold)
            }
            Button(
                onClick = {
                    val perm = if (Build.VERSION.SDK_INT >= 33) {
                        android.Manifest.permission.READ_MEDIA_AUDIO
                    } else {
                        android.Manifest.permission.READ_EXTERNAL_STORAGE
                    }
                    permissionLauncher.launch(perm)
                },
                modifier = Modifier.fillMaxWidth(),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black)
            ) {
                Icon(Icons.Rounded.Refresh, null)
                Spacer(Modifier.width(8.dp))
                Text("Scan Music Library", fontWeight = FontWeight.Bold)
            }
            OutlinedButton(
                onClick = { safLauncher.launch(null) },
                modifier = Modifier.fillMaxWidth(),
                shape = CircleShape
            ) {
                Icon(Icons.Rounded.FolderOpen, null, tint = Pulse.Cyan)
                Spacer(Modifier.width(8.dp))
                Text("Choose Music Folder", color = Color.White)
            }
        }
    }
}
