package com.pulse.player.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pulse.player.data.db.TrackEntity
import com.pulse.player.playback.Track
import com.pulse.player.ui.components.formatTime
import com.pulse.player.ui.theme.Pulse
import com.pulse.player.ui.viewmodel.MainViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class DupGroupWithMembers(
    val title: String,
    val artist: String,
    val count: Int,
    val members: List<TrackEntity>
)

@Composable
fun DuplicatesScreen(vm: MainViewModel) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var loading by remember { mutableStateOf(true) }
    var groups by remember { mutableStateOf<List<DupGroupWithMembers>>(emptyList()) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val db = vm.db
            val rawGroups = db.tracks().dupGroups()
            val list = rawGroups.map { g ->
                val members = db.tracks().dupMembers(g.normTitle, g.normArtist)
                val displayTitle = members.firstOrNull()?.title.orEmpty().ifBlank { g.normTitle }
                val displayArtist = members.firstOrNull()?.artist.orEmpty().ifBlank { g.normArtist }
                DupGroupWithMembers(
                    title = displayTitle,
                    artist = displayArtist,
                    count = g.c,
                    members = members
                )
            }
            withContext(Dispatchers.Main) {
                groups = list
                loading = false
            }
        }
    }

    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { vm.back() }) {
                Icon(Icons.Rounded.ArrowBack, "Back", tint = Color.White)
            }
            Spacer(Modifier.width(8.dp))
            Text(
                "Duplicate Finder",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }

        if (loading) {
            Box(Modifier.fillMaxSize(), Alignment.Center) {
                CircularProgressIndicator(color = Pulse.Cyan)
            }
        } else if (groups.isEmpty()) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        Icons.Rounded.Info,
                        null,
                        tint = Pulse.Cyan,
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        "No duplicate songs found",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Your library is clean. Every title and artist combination is unique.",
                        color = Pulse.TextMid,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 120.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Pulse.Surface),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            Modifier.padding(16.dp),
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                Icons.Rounded.Info,
                                null,
                                tint = Pulse.Cyan,
                                modifier = Modifier.size(24.dp)
                            )
                            Column {
                                Text(
                                    "Found ${groups.size} groups of possible duplicates",
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "Pulse Player never modifies or deletes your audio files. Use the copy button to inspect paths and remove unwanted copies in your device file manager, then rescan.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Pulse.TextMid
                                )
                            }
                        }
                    }
                }

                items(groups) { group ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Pulse.Surface),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        text = group.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = group.artist,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Pulse.TextMid
                                    )
                                }
                                Box(
                                    Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Pulse.Elevated)
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        "${group.count} copies",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Pulse.Cyan,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            group.members.forEach { track ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Pulse.Elevated)
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        Text(
                                            text = track.displayName,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                        Spacer(Modifier.height(2.dp))
                                        Text(
                                            text = "${formatTime(track.durationMs)} · ${track.sizeBytes / 1024 / 1024} MB · ${track.folderPath}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Pulse.TextMid
                                        )
                                    }

                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        IconButton(
                                            onClick = {
                                                vm.player.playTrack(
                                                    Track(
                                                        id = track.id,
                                                        uri = track.uri,
                                                        title = track.title,
                                                        artist = track.artist,
                                                        album = track.album,
                                                        durationMs = track.durationMs,
                                                        folder = track.folderPath,
                                                        isFavorite = false
                                                    )
                                                )
                                            }
                                        ) {
                                            Icon(Icons.Rounded.PlayArrow, "Play", tint = Pulse.Cyan)
                                        }

                                        IconButton(
                                            onClick = {
                                                val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                val clip = ClipData.newPlainText("track_path", track.dataPath ?: track.folderPath)
                                                cm.setPrimaryClip(clip)
                                                Toast.makeText(ctx, "Path copied", Toast.LENGTH_SHORT).show()
                                            }
                                        ) {
                                            Icon(Icons.Rounded.ContentCopy, "Copy path", tint = Color.White)
                                        }

                                        IconButton(
                                            onClick = {
                                                runCatching {
                                                    val share = Intent(Intent.ACTION_SEND).apply {
                                                        type = track.mimeType ?: "audio/*"
                                                        putExtra(Intent.EXTRA_STREAM, Uri.parse(track.uri))
                                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                    }
                                                    ctx.startActivity(Intent.createChooser(share, "Share ${track.title}"))
                                                }
                                            }
                                        ) {
                                            Icon(Icons.Rounded.Share, "Share", tint = Color.White)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

