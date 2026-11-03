package com.pulse.player.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import com.pulse.player.ui.viewmodel.TranscriptionState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TranscribeScreen(vm: MainViewModel) {
    val ctx = LocalContext.current
    val state by vm.transcriptionState.collectAsStateWithLifecycle()
    val tracks by vm.tracks.collectAsStateWithLifecycle()
    val currentTrackState by vm.player.state.collectAsStateWithLifecycle()

    var customPrompt by remember { mutableStateOf("") }
    var showTrackPicker by remember { mutableStateOf(false) }
    var selectedTrackForLyrics by remember { mutableStateOf<Track?>(null) }
    var showApiKeyDialog by remember { mutableStateOf(false) }
    var tempApiKey by remember { mutableStateOf("") }

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
                Icon(Icons.Rounded.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onSurface)
            }
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = "Audio & Lyrics Transcribe",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
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
            // Microphone capture is intentionally unavailable. C13 keeps the
            // The offline player does not request microphone access; local
            // library transcription remains available through the picker.
            // remains available through the track picker below.
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Transcribe local audio",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Choose a track from your library. Microphone capture is not included in this build.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Pulse.TextMid
                        )

                        Spacer(Modifier.height(24.dp))

                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(96.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Icon(Icons.Rounded.Mic, contentDescription = "Microphone capture unavailable", tint = Pulse.TextMid, modifier = Modifier.size(40.dp))
                        }

                        Spacer(Modifier.height(16.dp))
                        OutlinedButton(
                            onClick = { showTrackPicker = true },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
                            modifier = Modifier.testTag("pick_track_transcribe_button")
                        ) {
                            Icon(Icons.Rounded.LibraryMusic, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Choose from library")
                        }
                    }
                }
            }

            // Optional Prompt Guidance
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            "Transcription Guidance (Optional)",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface,
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
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
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
                                    color = MaterialTheme.colorScheme.onSurface,
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
                                        color = MaterialTheme.colorScheme.onSurface,
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
                                        Text("Check API Key", color = MaterialTheme.colorScheme.onSurface)
                                    }
                                    TextButton(onClick = { vm.resetTranscriptionState() }) {
                                        Text("Dismiss", color = MaterialTheme.colorScheme.onSurface)
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
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
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
                                            color = MaterialTheme.colorScheme.onSurface,
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
                                        color = MaterialTheme.colorScheme.onSurface,
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
                Text("Select Song to Transcribe", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
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
                                    Text(t.title, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    // Modal API Key Dialog
    if (showApiKeyDialog) {
        AlertDialog(
            onDismissRequest = { showApiKeyDialog = false },
            title = {
                Text("Gemini API Key", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
            },
            text = {
                Column {
                    Text(
                        "Set your Gemini API key in Pulse Settings to use online transcription.",
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
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurface)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }
}
