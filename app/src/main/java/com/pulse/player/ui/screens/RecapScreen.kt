package com.pulse.player.ui.screens

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pulse.player.app
import com.pulse.player.data.db.ArtistStat
import com.pulse.player.data.db.TrackEntity
import com.pulse.player.ui.components.Artwork
import com.pulse.player.ui.theme.Pulse
import com.pulse.player.ui.viewmodel.MainViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter

data class RecapData(
    val totalMs: Long,
    val topTracks: List<Pair<TrackEntity, Int>>,
    val topArtists: List<ArtistStat>,
    val badges: List<String>
)

@Composable
fun RecapScreen(vm: MainViewModel) {
    val ctx = LocalContext.current
    val db = ctx.app.db
    val streak by vm.streakDays.collectAsStateWithLifecycle()
    var currentMonth by remember { mutableStateOf(YearMonth.now()) }
    var recap by remember { mutableStateOf<RecapData?>(null) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(currentMonth) {
        loading = true
        withContext(Dispatchers.IO) {
            val zone = ZoneId.systemDefault()
            val from = currentMonth.atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
            val to = currentMonth.plusMonths(1).atDay(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1
            val total = db.stats().totalMs(from, to)
            if (total > 0) {
                val top = db.stats().topTracks(from, to, 5)
                val tracksMap = db.tracks().byIds(top.map { it.trackId }).associateBy { it.id }
                val badges = buildList {
                    if (total >= 100 * 3_600_000L) add("100 Hours Club")
                    else if (total >= 10 * 3_600_000L) add("10+ Hours Club")
                    if (db.stats().nightMs(from, to) >= total * 0.4) add("Night Owl")
                    if ((top.firstOrNull()?.plays ?: 0) >= 30) add("On Repeat")
                    else if ((top.firstOrNull()?.plays ?: 0) >= 15) add("Repeat Fan")
                    val distinctCount = db.stats().topTracks(from, to, 101).size
                    if (distinctCount >= 100) add("Explorer")
                }
                recap = RecapData(
                    totalMs = total,
                    topTracks = top.mapNotNull { t -> tracksMap[t.trackId]?.let { it to t.plays } },
                    topArtists = db.stats().topArtists(from, to, 5),
                    badges = badges
                )
            } else {
                recap = null
            }
            loading = false
        }
    }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = vm::back) {
                Icon(Icons.Rounded.ArrowBack, "Back", tint = Color.White)
            }
            Spacer(Modifier.width(8.dp))
            Text("Monthly Recap", style = MaterialTheme.typography.titleLarge, color = Color.White)
        }

        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { currentMonth = currentMonth.minusMonths(1) }) {
                Icon(Icons.Rounded.ChevronLeft, "Previous month", tint = Color.White)
            }
            Text(
                text = currentMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Pulse.Cyan
            )
            IconButton(onClick = { currentMonth = currentMonth.plusMonths(1) }) {
                Icon(Icons.Rounded.ChevronRight, "Next month", tint = Color.White)
            }
        }

        val r = recap
        if (r == null || r.totalMs <= 0) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(Pulse.Elevated),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.Headphones, null, tint = Pulse.Cyan, modifier = Modifier.size(32.dp))
                    }
                    Text(
                        "No listening history for this month",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        "Listen to some tracks and your statistics, top songs, and listening badges will appear here.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Pulse.TextMid,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Pulse.Surface),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .background(Brush.linearGradient(listOf(Pulse.PlumTop, Pulse.Surface)))
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            val hours = (r.totalMs / 3_600_000L).toInt()
                            val minutes = ((r.totalMs % 3_600_000L) / 60_000L).toInt()
                            Text("Total Listening Time", style = MaterialTheme.typography.bodySmall, color = Pulse.TextMid)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = if (hours > 0) "$hours h $minutes min" else "$minutes minutes",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            if (streak > 0) {
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = "$streak day active streak 🔥",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Pulse.Cyan,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                if (r.badges.isNotEmpty()) {
                    item {
                        Text("Badges Earned", style = MaterialTheme.typography.titleMedium, color = Color.White)
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            r.badges.forEach { badge ->
                                Box(
                                    Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Brush.linearGradient(listOf(Pulse.Violet, Pulse.Magenta)))
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Rounded.WorkspacePremium, null, tint = Color.White, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text(badge, style = MaterialTheme.typography.bodySmall, color = Color.White, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }

                if (r.topTracks.isNotEmpty()) {
                    item {
                        Text("Top Songs", style = MaterialTheme.typography.titleMedium, color = Color.White)
                    }
                    itemsIndexed(r.topTracks) { idx, (track, plays) ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("${idx + 1}", fontWeight = FontWeight.Bold, color = Pulse.Cyan, modifier = Modifier.width(28.dp))
                            Artwork(track.uri, 44.dp, radius = 8.dp, px = 128)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(track.title, maxLines = 1, color = Color.White, fontWeight = FontWeight.Bold)
                                Text("${track.artist} · $plays plays", maxLines = 1, color = Pulse.TextMid, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }

                if (r.topArtists.isNotEmpty()) {
                    item {
                        Text("Top Artists", style = MaterialTheme.typography.titleMedium, color = Color.White)
                    }
                    itemsIndexed(r.topArtists) { idx, artistStat ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("${idx + 1}", fontWeight = FontWeight.Bold, color = Pulse.Cyan, modifier = Modifier.width(28.dp))
                            Column(Modifier.weight(1f)) {
                                Text(artistStat.artist, maxLines = 1, color = Color.White, fontWeight = FontWeight.Bold)
                                Text("${artistStat.plays} plays", maxLines = 1, color = Pulse.TextMid, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }

                item {
                    Button(
                        onClick = {
                            val bitmap = Bitmap.createBitmap(800, 600, Bitmap.Config.ARGB_8888)
                            val canvas = Canvas(bitmap)
                            canvas.drawColor(0xFF14101F.toInt())
                            val paint = Paint().apply {
                                color = android.graphics.Color.WHITE
                                textSize = 40f
                                isAntiAlias = true
                            }
                            canvas.drawText("Pulse Player - ${currentMonth.format(DateTimeFormatter.ofPattern("MMM yyyy"))} Recap", 50f, 100f, paint)
                            paint.textSize = 30f
                            canvas.drawText("Total listening: ${r.totalMs / 60000} minutes", 50f, 180f, paint)
                            val file = File(ctx.cacheDir, "recap.png")
                            file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 95, it) }
                            val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.files", file)
                            val share = Intent(Intent.ACTION_SEND).apply {
                                type = "image/png"
                                putExtra(Intent.EXTRA_STREAM, uri)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            ctx.startActivity(Intent.createChooser(share, "Share Monthly Recap"))
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(containerColor = Pulse.Cyan, contentColor = Color.Black)
                    ) {
                        Icon(Icons.Rounded.Share, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Share Recap Card", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
