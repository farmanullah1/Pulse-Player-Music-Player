package com.pulse.player.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pulse.player.playback.Track
import com.pulse.player.ui.components.formatTime
import com.pulse.player.ui.theme.Pulse
import com.pulse.player.ui.viewmodel.MainViewModel
import com.pulse.player.ui.viewmodel.MusicGenState
import kotlin.math.sin

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun GenerateMusicScreen(vm: MainViewModel) {
    val ctx = LocalContext.current
    val genState by vm.musicGenState.collectAsStateWithLifecycle()
    val allTracks by vm.tracks.collectAsStateWithLifecycle()
    val aiTracks = remember(allTracks) {
        allTracks.filter { it.artist.contains("Lyria", ignoreCase = true) || it.album.contains("AI Generated", ignoreCase = true) }
    }

    var selectedModel by remember { mutableStateOf("lyria-3-clip-preview") }
    var promptText by remember { mutableStateOf("") }
    var showApiKeyDialog by remember { mutableStateOf(false) }
    var tempApiKey by remember { mutableStateOf("") }

    val inspirations = listOf(
        "Synthwave 80s neon bassline with retro electronic drums",
        "Lo-Fi chill hip-hop beat with rainy piano melody",
        "Cinematic orchestral soundtrack with epic percussion",
        "Acoustic warm folk guitar fingerpicking with soft strings",
        "Funky slap bass groove with 70s disco rhythm",
        "Dark ambient cyberpunk drone with futuristic synthesizers"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Pulse.Bg)
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { vm.back() },
                modifier = Modifier.testTag("gen_music_back_button")
            ) {
                Icon(Icons.Rounded.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = "AI Music Generator",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Powered by Google Lyria ($selectedModel)",
                    style = MaterialTheme.typography.bodySmall,
                    color = Pulse.Cyan
                )
            }
            IconButton(
                onClick = { showApiKeyDialog = true },
                modifier = Modifier.testTag("gen_music_api_key_button")
            ) {
                Icon(Icons.Rounded.Key, contentDescription = "Configure API Key", tint = Pulse.Cyan)
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Model Selector Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Pulse.Surface)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            text = "Lyria Model Selection",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            FilterChip(
                                selected = selectedModel == "lyria-3-clip-preview",
                                onClick = { selectedModel = "lyria-3-clip-preview" },
                                label = { Text("lyria-3-clip-preview (Clip ~30s)") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Pulse.Cyan,
                                    selectedLabelColor = Color.Black
                                ),
                                modifier = Modifier.testTag("model_clip_chip")
                            )
                            FilterChip(
                                selected = selectedModel == "lyria-3-pro-preview",
                                onClick = { selectedModel = "lyria-3-pro-preview" },
                                label = { Text("lyria-3-pro-preview (Full)") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Pulse.Cyan,
                                    selectedLabelColor = Color.Black
                                ),
                                modifier = Modifier.testTag("model_pro_chip")
                            )
                        }
                    }
                }
            }

            // Prompt Studio Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Pulse.Surface)
                ) {
                    Column(Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Describe your Music",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            if (promptText.isNotBlank()) {
                                TextButton(onClick = { promptText = "" }) {
                                    Text("Clear", color = Pulse.Cyan, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }

                        Spacer(Modifier.height(8.dp))

                        OutlinedTextField(
                            value = promptText,
                            onValueChange = { promptText = it },
                            placeholder = {
                                Text(
                                    "Describe instruments, mood, tempo, genre or melody...",
                                    color = Pulse.TextMid,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Pulse.Cyan,
                                unfocusedBorderColor = Pulse.Outline,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .testTag("music_prompt_input")
                        )

                        Spacer(Modifier.height(14.dp))

                        Text(
                            text = "Quick Inspiration:",
                            style = MaterialTheme.typography.labelMedium,
                            color = Pulse.TextMid,
                            fontWeight = FontWeight.SemiBold
                        )

                        Spacer(Modifier.height(8.dp))

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            inspirations.forEach { tag ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Pulse.Elevated,
                                    modifier = Modifier.clickable { promptText = tag }
                                ) {
                                    Text(
                                        text = tag,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Pulse.Cyan,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(20.dp))

                        val isGenerating = genState is MusicGenState.Generating

                        Button(
                            onClick = {
                                if (promptText.isBlank()) {
                                    Toast.makeText(ctx, "Please enter a music prompt first.", Toast.LENGTH_SHORT).show()
                                } else {
                                    vm.generateMusic(promptText, selectedModel)
                                }
                            },
                            enabled = !isGenerating,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("generate_music_submit_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = Pulse.Cyan),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            if (isGenerating) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    color = Color.Black,
                                    strokeWidth = 2.dp
                                )
                                Spacer(Modifier.width(10.dp))
                                Text("Composing with Google Lyria...", color = Color.Black, fontWeight = FontWeight.Bold)
                            } else {
                                Icon(Icons.Rounded.AutoAwesome, null, tint = Color.Black)
                                Spacer(Modifier.width(8.dp))
                                Text("Generate Music", color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Generation Progress Animation
            when (val s = genState) {
                is MusicGenState.Generating -> {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = Pulse.Surface)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(28.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                AnimatedEqualizerBars(Modifier.size(160.dp, 60.dp))
                                Spacer(Modifier.height(16.dp))
                                Text(
                                    "Composing musical audio...",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "Synthesizing audio stems using ${s.model}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Pulse.Cyan
                                )
                            }
                        }
                    }
                }

                is MusicGenState.Error -> {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF3B151E))
                        ) {
                            Column(Modifier.padding(20.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Rounded.Close, null, tint = Pulse.Magenta)
                                    Spacer(Modifier.width(10.dp))
                                    Text(
                                        "Generation Failed",
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                }
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    s.message,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFFFB4AB)
                                )
                                Spacer(Modifier.height(12.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = { showApiKeyDialog = true },
                                        colors = ButtonDefaults.buttonColors(containerColor = Pulse.Magenta)
                                    ) {
                                        Text("Check API Key", color = Color.White)
                                    }
                                    TextButton(onClick = { vm.resetMusicGenState() }) {
                                        Text("Dismiss", color = Color.White)
                                    }
                                }
                            }
                        }
                    }
                }

                is MusicGenState.Success -> {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = Pulse.Surface)
                        ) {
                            Column(Modifier.padding(20.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Rounded.Check, null, tint = Pulse.Cyan)
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            "Song Generated!",
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = Pulse.Cyan.copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            s.result.model,
                                            color = Pulse.Cyan,
                                            style = MaterialTheme.typography.labelSmall,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }

                                Spacer(Modifier.height(16.dp))

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(Pulse.Bg)
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(54.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(
                                                Brush.linearGradient(listOf(Pulse.Magenta, Pulse.Cyan))
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Rounded.MusicNote, null, tint = Color.White, modifier = Modifier.size(28.dp))
                                    }
                                    Spacer(Modifier.width(16.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(
                                            s.result.title,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            "Google Lyria • ${formatTime(s.track.durationMs)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Pulse.TextMid
                                        )
                                    }
                                }

                                Spacer(Modifier.height(16.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = { vm.playQueue(listOf(s.track), 0) },
                                        colors = ButtonDefaults.buttonColors(containerColor = Pulse.Cyan),
                                        modifier = Modifier
                                            .weight(1.2f)
                                            .testTag("play_generated_track_button")
                                    ) {
                                        Icon(Icons.Rounded.PlayArrow, null, tint = Color.Black)
                                        Spacer(Modifier.width(6.dp))
                                        Text("Play Now", color = Color.Black, fontWeight = FontWeight.Bold)
                                    }

                                    OutlinedButton(
                                        onClick = { vm.openAddToPlaylist(s.track) },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Rounded.PlaylistAdd, null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Add", maxLines = 1)
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                                type = "audio/mp3"
                                                putExtra(Intent.EXTRA_STREAM, Uri.fromFile(s.result.file))
                                            }
                                            ctx.startActivity(Intent.createChooser(shareIntent, "Share Generated Song"))
                                        },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Rounded.Share, null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Share", maxLines = 1)
                                    }
                                }
                            }
                        }
                    }
                }

                else -> Unit
            }

            // History of AI Generated Tracks in library
            if (aiTracks.isNotEmpty()) {
                item {
                    Text(
                        "Your AI Creations (${aiTracks.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                items(aiTracks) { track ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { vm.playQueue(listOf(track), 0) },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Pulse.Surface)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Pulse.Cyan.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Rounded.MusicNote, null, tint = Pulse.Cyan)
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    track.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    "${track.artist} • ${formatTime(track.durationMs)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Pulse.TextMid
                                )
                            }
                            IconButton(onClick = { vm.playQueue(listOf(track), 0) }) {
                                Icon(Icons.Rounded.PlayArrow, "Play", tint = Pulse.Cyan)
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal API Key Dialog
    if (showApiKeyDialog) {
        AlertDialog(
            onDismissRequest = { showApiKeyDialog = false },
            title = {
                Text("Gemini / Lyria API Key", color = Color.White, fontWeight = FontWeight.Bold)
            },
            text = {
                Column {
                    Text(
                        "Set your Google API key to generate music with Lyria. You can also configure GEMINI_API_KEY in the Secrets panel in AI Studio.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Pulse.TextMid
                    )
                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(
                        value = tempApiKey,
                        onValueChange = { tempApiKey = it },
                        placeholder = { Text("AIzaSy...", color = Pulse.TextMid) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Pulse.Cyan,
                            unfocusedBorderColor = Pulse.Outline,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (tempApiKey.isNotBlank()) {
                            vm.setGeminiApiKey(tempApiKey)
                            Toast.makeText(ctx, "API Key saved!", Toast.LENGTH_SHORT).show()
                        }
                        showApiKeyDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Pulse.Cyan)
                ) {
                    Text("Save Key", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showApiKeyDialog = false }) {
                    Text("Cancel", color = Color.White)
                }
            },
            containerColor = Pulse.Surface
        )
    }
}

@Composable
fun AnimatedEqualizerBars(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "eq")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    Canvas(modifier = modifier) {
        val barCount = 12
        val totalSpacing = (barCount - 1) * 6.dp.toPx()
        val barWidth = ((size.width - totalSpacing) / barCount).coerceAtLeast(3f)
        for (i in 0 until barCount) {
            val fraction = (sin(phase + i * 0.6f) + 1f) / 2f
            val barHeight = (size.height * (0.2f + fraction * 0.8f)).coerceAtLeast(4f)
            val x = i * (barWidth + 6.dp.toPx())
            val y = size.height - barHeight
            drawRoundRect(
                brush = Brush.verticalGradient(
                    listOf(Pulse.Magenta, Pulse.Cyan),
                    startY = y,
                    endY = size.height
                ),
                topLeft = Offset(x, y),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
            )
        }
    }
}
