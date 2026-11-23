package com.pulse.player.ui.screens

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas as ComposeCanvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pulse.player.app
import com.pulse.player.data.ErrorCategory
import com.pulse.player.data.ErrorDispatcher
import com.pulse.player.data.db.ArtistStat
import com.pulse.player.data.db.TrackEntity
import com.pulse.player.library.RecapAggregator
import com.pulse.player.library.RecapAnalytics
import com.pulse.player.ui.components.Artwork
import com.pulse.player.ui.components.LocalBottomOverlayPadding
import com.pulse.player.ui.components.PulseTopBar
import com.pulse.player.ui.theme.Pulse
import com.pulse.player.ui.theme.TabularMono
import com.pulse.player.ui.viewmodel.MainViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter

data class RecapData(
    val totalMs: Long,
    val topTracks: List<Pair<TrackEntity, Int>>,
    val topArtists: List<ArtistStat>,
    val badges: List<String>,
    val analytics: RecapAnalytics
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RecapScreen(vm: MainViewModel) {
    BackHandler { vm.back() }

    val ctx = LocalContext.current
    val db = ctx.app.db
    val scope = rememberCoroutineScope()
    val streak by vm.streakDays.collectAsStateWithLifecycle()
    var currentMonth by remember { mutableStateOf(YearMonth.now()) }
    var recap by remember { mutableStateOf<RecapData?>(null) }
    var loading by remember { mutableStateOf(true) }
    var sharingCard by remember { mutableStateOf(false) }

    LaunchedEffect(currentMonth) {
        loading = true
        withContext(Dispatchers.IO) {
            val zone = ZoneId.systemDefault()
            val from = currentMonth.atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
            val to = currentMonth.plusMonths(1).atDay(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1
            val total = db.stats().totalMs(from, to)
            if (total > 0) {
                val top = db.stats().topTracks(from, to, 5)
                val events = db.stats().eventsBetween(from, to)
                val allIds = (top.map { it.trackId } + events.map { it.trackId }).distinct()
                val tracksMap = if (allIds.isEmpty()) {
                    emptyMap()
                } else {
                    allIds.chunked(400).flatMap { db.tracks().byIds(it) }.associateBy { it.id }
                }
                val analytics = RecapAggregator.aggregate(currentMonth, events, tracksMap, zone)
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
                    badges = badges,
                    analytics = analytics
                )
            } else {
                recap = null
            }
            loading = false
        }
    }

    val bottomInset = LocalBottomOverlayPadding.current

    Column(Modifier.fillMaxSize()) {
        PulseTopBar(title = "Monthly Recap", onBack = vm::back)

        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { currentMonth = currentMonth.minusMonths(1) }, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Rounded.ChevronLeft, "Previous month", tint = MaterialTheme.colorScheme.onSurface)
            }
            Text(
                text = currentMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            IconButton(onClick = { currentMonth = currentMonth.plusMonths(1) }, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Rounded.ChevronRight, "Next month", tint = MaterialTheme.colorScheme.onSurface)
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
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.Headphones, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                    }
                    Text(
                        if (loading) "Loading recap…" else "No listening history for this month",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        "Listen to some tracks and your statistics, top songs, and listening badges will appear here.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = bottomInset + 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item(key = "hero_recap_card") {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .background(
                                    Brush.linearGradient(
                                        listOf(
                                            MaterialTheme.colorScheme.primaryContainer,
                                            MaterialTheme.colorScheme.surfaceContainerHigh
                                        )
                                    )
                                )
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            val hours = (r.totalMs / 3_600_000L).toInt()
                            val minutes = ((r.totalMs % 3_600_000L) / 60_000L).toInt()
                            Text("Total Listening Time", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = if (hours > 0) "$hours h $minutes min" else "$minutes minutes",
                                style = MaterialTheme.typography.headlineMedium.merge(TabularMono),
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            if (streak > 0) {
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = "$streak day active streak",
                                    style = MaterialTheme.typography.bodyMedium.merge(TabularMono),
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                if (r.analytics.dailyPoints.any { it.totalMs > 0L }) {
                    item(key = "recap_daily_chart") {
                        val dailyPoints = r.analytics.dailyPoints
                        val maxDailyMs = remember(dailyPoints) {
                            (dailyPoints.maxOfOrNull { it.totalMs } ?: 1L).coerceAtLeast(1L)
                        }
                        val barPrimary = MaterialTheme.colorScheme.primary
                        val barTrack = MaterialTheme.colorScheme.surfaceVariant
                        val gridLine = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("recap_daily_activity_card"),
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f))
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            "Daily Listening Activity",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = buildString {
                                                append("${r.analytics.activeDaysCount} active days")
                                                r.analytics.peakDayOfMonth?.let { day ->
                                                    append(" · Peak Day $day (${(r.analytics.peakDayMs / 60_000L).coerceAtLeast(1L)}m)")
                                                }
                                            },
                                            style = MaterialTheme.typography.bodySmall.merge(TabularMono),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                ComposeCanvas(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(132.dp)
                                        .testTag("recap_daily_bar_canvas")
                                ) {
                                    val count = dailyPoints.size.coerceAtLeast(1)
                                    val slotW = size.width / count.toFloat()
                                    val barW = (slotW * 0.62f).coerceAtLeast(3f)
                                    val topPad = 8.dp.toPx()
                                    val bottomPad = 6.dp.toPx()
                                    val usableH = (size.height - topPad - bottomPad).coerceAtLeast(12f)

                                    drawLine(
                                        color = gridLine,
                                        start = Offset(0f, size.height - bottomPad),
                                        end = Offset(size.width, size.height - bottomPad),
                                        strokeWidth = 1.dp.toPx()
                                    )
                                    drawLine(
                                        color = gridLine,
                                        start = Offset(0f, topPad + usableH * 0.5f),
                                        end = Offset(size.width, topPad + usableH * 0.5f),
                                        strokeWidth = 0.75.dp.toPx()
                                    )

                                    dailyPoints.forEachIndexed { idx, pt ->
                                        val x = idx * slotW + (slotW - barW) / 2f
                                        drawRoundRect(
                                            color = barTrack.copy(alpha = 0.35f),
                                            topLeft = Offset(x, topPad),
                                            size = Size(barW, usableH),
                                            cornerRadius = CornerRadius(barW / 2f, barW / 2f)
                                        )
                                        if (pt.totalMs > 0L) {
                                            val frac = (pt.totalMs.toFloat() / maxDailyMs.toFloat()).coerceIn(0.08f, 1f)
                                            val h = usableH * frac
                                            val isPeak = pt.dayOfMonth == r.analytics.peakDayOfMonth
                                            drawRoundRect(
                                                color = if (isPeak) barPrimary else barPrimary.copy(alpha = 0.76f),
                                                topLeft = Offset(x, size.height - bottomPad - h),
                                                size = Size(barW, h),
                                                cornerRadius = CornerRadius(barW / 2f, barW / 2f)
                                            )
                                        }
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Day 1", style = MaterialTheme.typography.labelSmall.merge(TabularMono), color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("Day ${dailyPoints.size / 2}", style = MaterialTheme.typography.labelSmall.merge(TabularMono), color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("Day ${dailyPoints.size}", style = MaterialTheme.typography.labelSmall.merge(TabularMono), color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }

                if (r.analytics.circadian.totalMs > 0L || r.analytics.topGenres.isNotEmpty()) {
                    item(key = "recap_rhythm_genres") {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("recap_rhythm_card"),
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f))
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                if (r.analytics.circadian.totalMs > 0L) {
                                    val c = r.analytics.circadian
                                    val totalCircadian = c.totalMs.coerceAtLeast(1L)
                                    Text(
                                        "Listening Rhythm",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        c.dominantLabel(),
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    listOf(
                                        "Morning (5AM–12PM)" to c.morningMs,
                                        "Afternoon (12–5PM)" to c.afternoonMs,
                                        "Evening (5–10PM)" to c.eveningMs,
                                        "Night (10PM–5AM)" to c.nightMs
                                    ).forEach { (slotLabel, ms) ->
                                        val frac = (ms.toFloat() / totalCircadian.toFloat()).coerceIn(0f, 1f)
                                        val mins = ms / 60_000L
                                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(slotLabel, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
                                                Text(
                                                    "${(frac * 100).toInt()}% · ${mins}m",
                                                    style = MaterialTheme.typography.labelSmall.merge(TabularMono),
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            LinearProgressIndicator(
                                                progress = { frac.coerceAtLeast(if (ms > 0L) 0.04f else 0f) },
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(6.dp)
                                                    .clip(CircleShape),
                                                color = MaterialTheme.colorScheme.primary,
                                                trackColor = MaterialTheme.colorScheme.surface
                                            )
                                        }
                                    }
                                }

                                if (r.analytics.topGenres.isNotEmpty()) {
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        "Top Genres",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontWeight = FontWeight.Bold
                                    )
                                    FlowRow(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        r.analytics.topGenres.forEach { g ->
                                            Surface(
                                                shape = CircleShape,
                                                color = MaterialTheme.colorScheme.secondaryContainer
                                            ) {
                                                Text(
                                                    text = "${g.genre} · ${g.plays} plays",
                                                    style = MaterialTheme.typography.labelSmall.merge(TabularMono),
                                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                                    fontWeight = FontWeight.SemiBold,
                                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                if (r.badges.isNotEmpty()) {
                    item(key = "recap_badges") {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Badges Earned", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
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
                }

                if (r.topTracks.isNotEmpty()) {
                    item(key = "recap_top_tracks") {
                        val maxTrackPlays = remember(r.topTracks) {
                            (r.topTracks.maxOfOrNull { it.second } ?: 1).coerceAtLeast(1)
                        }
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f))
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text("Top Songs", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                                r.topTracks.forEachIndexed { idx, (track, plays) ->
                                    val fraction = (plays.toFloat() / maxTrackPlays.toFloat()).coerceIn(0.08f, 1f)
                                    Row(
                                        Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            "${idx + 1}",
                                            style = MaterialTheme.typography.titleSmall.merge(TabularMono),
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.width(26.dp)
                                        )
                                        Artwork(track.uri, 44.dp, radius = 8.dp, px = 128)
                                        Spacer(Modifier.width(12.dp))
                                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Text(
                                                text = track.title,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "${track.artist} · $plays plays",
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                style = MaterialTheme.typography.bodySmall.merge(TabularMono)
                                            )
                                            LinearProgressIndicator(
                                                progress = { fraction },
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(4.dp)
                                                    .clip(CircleShape),
                                                color = MaterialTheme.colorScheme.primary,
                                                trackColor = MaterialTheme.colorScheme.surface
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                if (r.topArtists.isNotEmpty()) {
                    item(key = "recap_top_artists") {
                        val maxArtistPlays = remember(r.topArtists) {
                            (r.topArtists.maxOfOrNull { it.plays } ?: 1).coerceAtLeast(1)
                        }
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f))
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text("Top Artists", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                                r.topArtists.forEachIndexed { idx, artistStat ->
                                    val fraction = (artistStat.plays.toFloat() / maxArtistPlays.toFloat()).coerceIn(0.08f, 1f)
                                    Row(
                                        Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            "${idx + 1}",
                                            style = MaterialTheme.typography.titleSmall.merge(TabularMono),
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.width(26.dp)
                                        )
                                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Row(
                                                Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = artistStat.artist,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.weight(1f)
                                                )
                                                Spacer(Modifier.width(8.dp))
                                                Text(
                                                    text = "${artistStat.plays} plays",
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    style = MaterialTheme.typography.bodySmall.merge(TabularMono)
                                                )
                                            }
                                            LinearProgressIndicator(
                                                progress = { fraction },
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(4.dp)
                                                    .clip(CircleShape),
                                                color = MaterialTheme.colorScheme.secondary,
                                                trackColor = MaterialTheme.colorScheme.surface
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                item(key = "share_recap") {
                    Button(
                        enabled = !sharingCard,
                        onClick = {
                            if (sharingCard) return@Button
                            sharingCard = true
                            scope.launch {
                                val res = withContext(Dispatchers.IO) {
                                    runCatching {
                                        val file = renderRecapSharePng(ctx, currentMonth, r, streak)
                                        FileProvider.getUriForFile(ctx, "${ctx.packageName}.files", file)
                                    }
                                }
                                sharingCard = false
                                res.onSuccess { uri ->
                                    val share = Intent(Intent.ACTION_SEND).apply {
                                        type = "image/png"
                                        putExtra(Intent.EXTRA_STREAM, uri)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    ctx.startActivity(Intent.createChooser(share, "Share Monthly Recap"))
                                }.onFailure { err ->
                                    ErrorDispatcher.post(
                                        ErrorCategory.STORAGE,
                                        "Unable to share recap card",
                                        err.message ?: "Could not generate or share recap image.",
                                        err.stackTraceToString()
                                    )
                                    vm.snackbarMessage.value = "Unable to share recap card"
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .padding(vertical = 8.dp)
                            .testTag("share_recap_button"),
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Icon(Icons.Rounded.Share, null)
                        Spacer(Modifier.width(8.dp))
                        Text(if (sharingCard) "Preparing Card…" else "Share Recap Card", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

private fun renderRecapSharePng(
    ctx: android.content.Context,
    month: YearMonth,
    r: RecapData,
    streak: Int
): File {
    val w = 1080
    val h = 1350
    val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val bgPaint = Paint().apply {
        shader = LinearGradient(
            0f, 0f, w.toFloat(), h.toFloat(),
            intArrayOf(0xFF141022.toInt(), 0xFF1E1736.toInt(), 0xFF121826.toInt()),
            floatArrayOf(0f, 0.55f, 1f),
            Shader.TileMode.CLAMP
        )
    }
    canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), bgPaint)

    val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x1AFFFFFF
    }
    val accentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFE89B25.toInt()
    }
    val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        textSize = 46f
        isFakeBoldText = true
    }
    val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFB8C0D0.toInt()
        textSize = 28f
    }
    val heroPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFF5B64C.toInt()
        textSize = 84f
        isFakeBoldText = true
    }
    val itemPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        textSize = 32f
    }

    canvas.drawText("PULSE MUSIC RECAP", 72f, 108f, labelPaint)
    canvas.drawText(month.format(DateTimeFormatter.ofPattern("MMMM yyyy")), 72f, 166f, titlePaint)

    val hours = (r.totalMs / 3_600_000L).toInt()
    val minutes = ((r.totalMs % 3_600_000L) / 60_000L).toInt()
    val totalStr = if (hours > 0) "${hours}h ${minutes}m" else "${minutes} min"
    canvas.drawRoundRect(RectF(64f, 208f, w - 64f, 430f), 36f, 36f, cardPaint)
    canvas.drawText("Total Listening Time", 96f, 268f, labelPaint)
    canvas.drawText(totalStr, 96f, 358f, heroPaint)
    val metaLine = buildString {
        if (streak > 0) append("$streak-day streak  ·  ")
        append(r.analytics.circadian.dominantLabel())
    }
    canvas.drawText(metaLine, 96f, 402f, labelPaint)

    // Sparkline bars for daily listening
    val pts = r.analytics.dailyPoints
    val maxDay = (pts.maxOfOrNull { it.totalMs } ?: 1L).coerceAtLeast(1L)
    if (pts.isNotEmpty()) {
        val chartLeft = 72f
        val chartRight = w - 72f
        val chartBottom = 570f
        val chartHeight = 96f
        val slot = (chartRight - chartLeft) / pts.size
        val bw = (slot * 0.6f).coerceAtLeast(6f)
        pts.forEachIndexed { idx, pt ->
            val x = chartLeft + idx * slot
            val barH = if (pt.totalMs > 0L) (chartHeight * (pt.totalMs.toFloat() / maxDay)).coerceAtLeast(10f) else 6f
            accentPaint.alpha = if (pt.totalMs > 0L) 235 else 55
            canvas.drawRoundRect(RectF(x, chartBottom - barH, x + bw, chartBottom), bw / 2f, bw / 2f, accentPaint)
        }
        accentPaint.alpha = 255
    }

    var y = 650f
    canvas.drawText("TOP SONGS", 72f, y, labelPaint)
    y += 44f
    r.topTracks.take(3).forEachIndexed { idx, (track, plays) ->
        val line = "${idx + 1}. ${track.title.take(32)} — $plays plays"
        canvas.drawText(line, 72f, y, itemPaint)
        y += 50f
    }

    y += 38f
    canvas.drawText("TOP ARTISTS", 72f, y, labelPaint)
    y += 44f
    r.topArtists.take(3).forEachIndexed { idx, artistStat ->
        val line = "${idx + 1}. ${artistStat.artist.take(32)} — ${artistStat.plays} plays"
        canvas.drawText(line, 72f, y, itemPaint)
        y += 50f
    }

    val file = File(ctx.cacheDir, "recap.png")
    file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 95, it) }
    bitmap.recycle()
    return file
}
