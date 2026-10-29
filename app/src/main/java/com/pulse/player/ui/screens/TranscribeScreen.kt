package com.pulse.player.ui.screens

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pulse.player.playback.Track
import com.pulse.player.ui.components.formatTime
import com.pulse.player.ui.theme.Pulse
import com.pulse.player.ui.viewmodel.MainViewModel
import com.pulse.player.ui.viewmodel.TranscriptionState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TranscribeScreen(vm: MainViewModel) {
    val ctx = LocalContext.current
    val isRecording by vm.isRecording.collectAsStateWithLifecycle()
    val durationSec by vm.recordingDurationSec.collectAsStateWithLifecycle()
    val amplitude by vm.recordingAmplitude.collectAsStateWithLifecycle()
    val state by vm.transcriptionState.collectAsStateWithLifecycle()
    val tracks by vm.tracks.collectAsStateWithLifecycle()
    val currentTrackState by vm.player.state.collectAsStateWithLifecycle()

    var customPrompt by remember { mutableStateOf("") }
    var showTrackPicker by remember { mutableStateOf(false) }
    var selectedTrackForLyrics by remember { mutableStateOf<Track?>(null) }
    var showApiKeyDialog by remember { mutableStateOf(false) }
    var tempApiKey by remember { mutableStateOf("") }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            vm.startAudioRecording()
        } else {
            Toast.makeText(ctx, "Microphone permission is required to record audio.", Toast.LENGTH_SHORT).show()
        }
    }

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
                modifier = Modifier.testTag("transcribe_back_button")
            ) {
                Icon(Icons.Rounded.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = "Audio & Lyrics Transcribe",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Powered by gemini-3.5-transcribe",
                    style = MaterialTheme.typography.bodySmall,
                    color = Pulse.Cyan
                )
            }
            IconButton(
                onClick = { showApiKeyDialog = true },
                modifier = Modifier.testTag("transcribe_api_key_button")
            ) {
                Icon(Icons.Rounded.Key, contentDescription = "Configure API Key", tint = Pulse.Cyan)
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Live Recording Studio Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Pulse.Surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (isRecording) "Recording Audio..." else "Record Speech or Song",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = if (isRecording) "Speak or sing into the microphone" else "Capture spoken thoughts, speech, or vocal melodies",
                            style = MaterialTheme.typography.bodySmall,
                            color = Pulse.TextMid
                        )

                        Spacer(Modifier.height(24.dp))

                        // Mic Visualizer / Pulsing Button
                        val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                        val pulseScale by infiniteTransition.animateFloat(
                            initialValue = 1f,
                            targetValue = if (isRecording) 1.15f else 1f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(600, easing = FastOutSlowInEasing),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "pulseScale"
                        )

                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(130.dp)
                        ) {
                            if (isRecording) {
                                Box(
                                    modifier = Modifier
                                        .size(130.dp)
                                        .scale(pulseScale)
                                        .clip(CircleShape)
                                        .background(Pulse.Magenta.copy(alpha = 0.25f))
                                )
                            }
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(96.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.linearGradient(
                                            if (isRecording) listOf(Pulse.Magenta, Pulse.Violet)
                                            else listOf(Pulse.Cyan, Pulse.Violet)
                                        )
                                    )
                                    .clickable {
                                        if (isRecording) {
                                            vm.stopAndTranscribeAudio(customPrompt)
                                        } else {
                                            val hasPerm = ContextCompat.checkSelfPermission(
                                                ctx,
                                                Manifest.permission.RECORD_AUDIO
                                            ) == PackageManager.PERMISSION_GRANTED
                                            if (hasPerm) {
                                                vm.startAudioRecording()
                                            } else {
                                                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                            }
                                        }
                                    }
                                    .testTag("record_mic_toggle_button")
                            ) {
                                Icon(
                                    imageVector = if (isRecording) Icons.Rounded.Stop else Icons.Rounded.Mic,
                                    contentDescription = if (isRecording) "Stop Recording" else "Start Recording",
                                    tint = Color.White,
                                    modifier = Modifier.size(44.dp)
                                )
                            }
                        }

                        Spacer(Modifier.height(16.dp))

                        // Live Timer & Amplitude
                        Text(
                            text = formatTime(durationSec * 1000L),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isRecording) Pulse.Magenta else Color.White
                        )

                        if (isRecording) {
                            Spacer(Modifier.height(8.dp))
                            // Amplitude visualizer bars
                            Row(
                                modifier = Modifier.height(28.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val normalized = (amplitude / 32767f).coerceIn(0.1f, 1f)
                                for (i in 0 until 12) {
                                    val factor = ((i * 3 + (amplitude % 7)) % 10) / 10f
                                    val barHeight = (28.dp * (normalized * 0.7f + factor * 0.3f)).coerceAtLeast(4.dp)
                                    Box(
                                        modifier = Modifier
                                            .width(4.dp)
                                            .height(barHeight)
                                            .clip(RoundedCornerShape(2.dp))
                                            .background(if (i % 2 == 0) Pulse.Magenta else Pulse.Cyan)
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(20.dp))

                        // Recording Controls
                        if (isRecording) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedButton(
                                    onClick = { vm.cancelAudioRecording() },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                    modifier = Modifier.testTag("cancel_recording_button")
                                ) {
                                    Icon(Icons.Rounded.Close, null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Cancel")
                                }

                                Button(
                                    onClick = { vm.stopAndTranscribeAudio(customPrompt) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Pulse.Cyan),
                                    modifier = Modifier.testTag("transcribe_recording_button")
                                ) {
                                    Icon(Icons.Rounded.AutoAwesome, null, tint = Color.Black, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Transcribe", color = Color.Black, fontWeight = FontWeight.Bold)
                                }
                            }
                        } else {
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Button(
                                    onClick = {
                                        val hasPerm = ContextCompat.checkSelfPermission(
                                            ctx,
                                            Manifest.permission.RECORD_AUDIO
                                        ) == PackageManager.PERMISSION_GRANTED
                                        if (hasPerm) {
                                            vm.startAudioRecording()
                                        } else {
                                            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Pulse.Cyan),
                                    modifier = Modifier.testTag("start_recording_button")
                                ) {
                                    Icon(Icons.Rounded.Mic, null, tint = Color.Black, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Record Speech", color = Color.Black, fontWeight = FontWeight.Bold)
                                }

                                OutlinedButton(
                                    onClick = { showTrackPicker = true },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                    modifier = Modifier.testTag("pick_track_transcribe_button")
                                ) {
                                    Icon(Icons.Rounded.LibraryMusic, null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("From Library")
                                }
                            }
                        }
                    }
                }
            }

            // Optional Prompt Guidance
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Pulse.Surface)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            "Transcription Guidance (Optional)",
                            style = MaterialTheme.typography.titleSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = customPrompt,
                            onValueChange = { customPrompt = it },
                            placeholder = {
                                Text(
                                    "e.g., 'Transcribe as synchronized song lyrics with timestamps', or 'Transcribe in Spanish'",
                                    color = Pulse.TextMid,
                                    style = MaterialTheme.typography.bodySmall
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
                                .testTag("transcription_prompt_input"),
                            maxLines = 3
                        )
                    }
                }
            }

            // State Display: Loading, Result or Error
            when (val s = state) {
                is TranscriptionState.Transcribing -> {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = Pulse.Surface)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                CircularProgressIndicator(color = Pulse.Cyan)
                                Spacer(Modifier.height(16.dp))
                                Text(
                                    s.progressText,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "Calling gemini-3.5-transcribe...",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Pulse.TextMid
                                )
                            }
                        }
                    }
                }

                is TranscriptionState.Error -> {
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
                                        "Transcription Failed",
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
                                    TextButton(onClick = { vm.resetTranscriptionState() }) {
                                        Text("Dismiss", color = Color.White)
                                    }
                                }
                            }
                        }
                    }
                }

                is TranscriptionState.Success -> {
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
                                            "Transcription Complete",
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
                                            text = s.result.modelUsed,
                                            color = Pulse.Cyan,
                                            style = MaterialTheme.typography.labelSmall,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }

                                Spacer(Modifier.height(16.dp))

                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = Pulse.Bg,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = s.result.text,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.White,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp)
                                            .testTag("transcription_result_text")
                                    )
                                }

                                Spacer(Modifier.height(16.dp))

                                // Quick Actions
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            val clipboard = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                            clipboard.setPrimaryClip(ClipData.newPlainText("Transcription", s.result.text))
                                            Toast.makeText(ctx, "Copied transcript to clipboard", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("copy_transcription_button")
                                    ) {
                                        Icon(Icons.Rounded.ContentCopy, null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Copy", maxLines = 1)
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            val intent = Intent(Intent.ACTION_SEND).apply {
                                                type = "text/plain"
                                                putExtra(Intent.EXTRA_TEXT, s.result.text)
                                            }
                                            ctx.startActivity(Intent.createChooser(intent, "Share Transcription"))
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("share_transcription_button")
                                    ) {
                                        Icon(Icons.Rounded.Share, null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Share", maxLines = 1)
                                    }

                                    Button(
                                        onClick = {
                                            val targetId = currentTrackState.mediaId ?: tracks.firstOrNull()?.id
                                            if (targetId != null) {
                                                val saved = vm.saveLyricsForTrack(targetId, s.result.text)
                                                if (saved) {
                                                    Toast.makeText(ctx, "Saved as lyrics for current track!", Toast.LENGTH_SHORT).show()
                                                } else {
                                                    Toast.makeText(ctx, "Failed to save lyrics.", Toast.LENGTH_SHORT).show()
                                                }
                                            } else {
                                                Toast.makeText(ctx, "Play or select a track first to attach lyrics.", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Pulse.Cyan),
                                        modifier = Modifier
                                            .weight(1.3f)
                                            .testTag("save_lyrics_button")
                                    ) {
                                        Icon(Icons.Rounded.RecordVoiceOver, null, tint = Color.Black, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Save Lyrics", color = Color.Black, fontWeight = FontWeight.Bold, maxLines = 1)
                                    }
                                }
                            }
                        }
                    }
                }

                else -> Unit
            }
        }
    }

    // Modal track picker to transcribe song from library
    if (showTrackPicker) {
        AlertDialog(
            onDismissRequest = { showTrackPicker = false },
            title = {
                Text("Select Song to Transcribe", color = Color.White, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(Modifier.height(320.dp)) {
                    Text(
                        "Choose a track from your library to generate lyrics with gemini-3.5-transcribe:",
                        style = MaterialTheme.typography.bodySmall,
                        color = Pulse.TextMid
                    )
                    Spacer(Modifier.height(12.dp))
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(tracks.take(30)) { t ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable {
                                        showTrackPicker = false
                                        vm.transcribeTrack(t, customPrompt)
                                    }
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Rounded.LibraryMusic, null, tint = Pulse.Cyan, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(10.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(t.title, color = Color.White, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(t.artist, color = Pulse.TextMid, style = MaterialTheme.typography.bodySmall, maxLines = 1)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showTrackPicker = false }) {
                    Text("Close", color = Pulse.Cyan)
                }
            },
            containerColor = Pulse.Surface
        )
    }

    // Modal API Key Dialog
    if (showApiKeyDialog) {
        AlertDialog(
            onDismissRequest = { showApiKeyDialog = false },
            title = {
                Text("Gemini API Key", color = Color.White, fontWeight = FontWeight.Bold)
            },
            text = {
                Column {
                    Text(
                        "Set your Gemini API key to power audio transcription (gemini-3.5-transcribe) and music generation (lyria-3-clip-preview). You can also configure GEMINI_API_KEY in the Secrets panel in AI Studio.",
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
