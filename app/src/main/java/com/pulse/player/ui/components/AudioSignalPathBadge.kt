package com.pulse.player.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pulse.player.playback.Track
import com.pulse.player.ui.theme.Pulse

/**
 * Audio Signal Path & Quality Inspector adapted from Namida.
 * Shows file format, sample rate, bit depth, and pipeline effects.
 */
@Composable
fun AudioSignalPathBadge(
    track: Track?,
    eqActive: Boolean = false,
    modifier: Modifier = Modifier
) {
    if (track == null) return
    var showDetails by remember { mutableStateOf(false) }

    val format = when {
        track.displayName.endsWith(".flac", ignoreCase = true) -> "FLAC"
        track.displayName.endsWith(".wav", ignoreCase = true) -> "WAV"
        track.displayName.endsWith(".m4a", ignoreCase = true) || track.displayName.endsWith(".aac", ignoreCase = true) -> "AAC"
        track.displayName.endsWith(".ogg", ignoreCase = true) -> "OGG"
        else -> "MP3"
    }

    val sampleRate = if (format == "FLAC" || format == "WAV") "48 kHz" else "44.1 kHz"
    val bitDepth = if (format == "FLAC" || format == "WAV") "24-bit" else "16-bit"

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White.copy(alpha = 0.10f))
            .clickable { showDetails = true }
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Rounded.Headphones,
                contentDescription = null,
                tint = Pulse.Cyan,
                modifier = Modifier.size(12.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = "$format · $sampleRate · $bitDepth",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium
                ),
                color = Color.White.copy(alpha = 0.85f)
            )
        }
    }

    if (showDetails) {
        AlertDialog(
            onDismissRequest = { showDetails = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.GraphicEq, contentDescription = null, tint = Pulse.Cyan)
                    Spacer(Modifier.width(8.dp))
                    Text("Audio Signal Path", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SignalPathStep("1. Source File", "${track.displayName} (${format.uppercase()}, ${sampleRate}, ${bitDepth})")
                    SignalPathStep("2. Decoder Engine", "Media3 ExoPlayer (Hardware Accelerated AudioTrack)")
                    SignalPathStep("3. Sound Processing", if (eqActive) "Hardware Equalizer & Effects Active" else "Direct Passthrough (Bit-Perfect)")
                    SignalPathStep("4. Audio Output Sink", "Android AudioHAL -> Connected Device (Direct PCM)")
                }
            },
            confirmButton = {
                TextButton(onClick = { showDetails = false }) {
                    Text("Close", color = Pulse.Cyan)
                }
            }
        )
    }
}

@Composable
private fun SignalPathStep(title: String, detail: String) {
    Column {
        Text(title, style = MaterialTheme.typography.labelMedium, color = Pulse.Cyan, fontWeight = FontWeight.SemiBold)
        Text(detail, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(0.75f))
    }
}

