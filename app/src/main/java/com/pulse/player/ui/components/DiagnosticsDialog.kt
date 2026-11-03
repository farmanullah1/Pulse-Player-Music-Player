package com.pulse.player.ui.components

import android.content.pm.PackageManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.SdCard
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pulse.player.data.AppError
import com.pulse.player.data.ErrorCategory
import com.pulse.player.data.scan.audioPermission
import com.pulse.player.ui.theme.Pulse
import com.pulse.player.ui.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DiagnosticsDialog(
    vm: MainViewModel,
    onDismiss: () -> Unit
) {
    val fxStatus by vm.fxStatus.collectAsStateWithLifecycle()
    val recentErrors by vm.recentErrors.collectAsStateWithLifecycle()
    val missingCount by vm.missingCount.collectAsStateWithLifecycle()
    val ctx = LocalContext.current

    val hasAudioPermission = ContextCompat.checkSelfPermission(
        ctx,
        audioPermission()
    ) == PackageManager.PERMISSION_GRANTED

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.85f)
                .clip(RoundedCornerShape(24.dp))
                .testTag("diagnostics_dialog"),
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Pulse.Cyan.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Rounded.Build,
                                contentDescription = null,
                                tint = Pulse.Cyan,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                "System Diagnostics",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                "Hardware status & error events",
                                style = MaterialTheme.typography.bodySmall,
                                color = Pulse.TextMid
                            )
                        }
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Rounded.Close, "Close", tint = MaterialTheme.colorScheme.onSurface)
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Scrollable content
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 1. Audio Engine & DSP Hardware
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Rounded.GraphicEq, null, tint = Pulse.Cyan, modifier = Modifier.size(20.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Audio Hardware & DSP", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                }

                                HardwareStatusRow("Audio Session", if (fxStatus.sessionId > 0) "Session #${fxStatus.sessionId}" else "Idle / Standby", isOk = fxStatus.sessionId > 0)
                                HardwareStatusRow("Hardware Equalizer", if (fxStatus.eqSupported) "Supported / Ready" else "Unsupported by audio HAL", isOk = fxStatus.eqSupported)
                                HardwareStatusRow("Bass Boost Effect", if (fxStatus.bassSupported) "Supported / Ready" else "Unsupported", isOk = fxStatus.bassSupported)
                                HardwareStatusRow("Virtualizer Effect", if (fxStatus.virtSupported) "Supported / Ready" else "Unsupported", isOk = fxStatus.virtSupported)
                                HardwareStatusRow("Preset Reverb Effect", if (fxStatus.reverbSupported) "Supported / Ready" else "Unsupported", isOk = fxStatus.reverbSupported)
                            }
                        }
                    }

                    // 2. Storage & Scanner
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Rounded.SdCard, null, tint = Pulse.Violet, modifier = Modifier.size(20.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Storage & Privacy", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                }

                                HardwareStatusRow("Audio Permission", if (hasAudioPermission) "Granted" else "Missing", isOk = hasAudioPermission)
                                HardwareStatusRow("Missing Tracks", if (missingCount == 0) "0 (Clean)" else "$missingCount unreachable", isOk = missingCount == 0)
                                HardwareStatusRow("Network Policy", "100% Offline (Zero permissions)", isOk = true)
                            }
                        }
                    }

                    // 3. Error Log
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Live Error Log (${recentErrors.size})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (recentErrors.isNotEmpty()) {
                                TextButton(onClick = vm::clearErrorLog) {
                                    Text("Clear Log", color = Pulse.Cyan, style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }
                    }

                    if (recentErrors.isEmpty()) {
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Rounded.CheckCircle, null, tint = Pulse.Mint, modifier = Modifier.size(24.dp))
                                    Spacer(Modifier.width(12.dp))
                                    Column {
                                        Text("No errors recorded", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                        Text("All audio pipelines and database queries are healthy.", style = MaterialTheme.typography.bodySmall, color = Pulse.TextMid)
                                    }
                                }
                            }
                        }
                    } else {
                        items(recentErrors, key = { it.id }) { err ->
                            ErrorItemCard(err)
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Bottom actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (missingCount > 0) {
                        OutlinedButton(
                            onClick = { vm.cleanMissingTracks() },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Rounded.Delete, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Clean ($missingCount)")
                        }
                    }
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Pulse.Cyan)
                    ) {
                        Text("Close", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun HardwareStatusRow(label: String, value: String, isOk: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = Pulse.TextMid)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(if (isOk) Pulse.Mint else Pulse.Coral)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                value,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = if (isOk) MaterialTheme.colorScheme.onSurface else Pulse.Coral
            )
        }
    }
}

@Composable
private fun ErrorItemCard(err: AppError) {
    val timeStr = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(err.timestampMs))
    val categoryColor = when (err.category) {
        ErrorCategory.PLAYBACK -> Pulse.Coral
        ErrorCategory.SCAN -> Pulse.Sun
        ErrorCategory.BACKUP -> Pulse.Cyan
        ErrorCategory.AUDIO_FX -> Pulse.Purple
        ErrorCategory.STORAGE -> Pulse.Orange
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(categoryColor.copy(alpha = 0.2f))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        err.category.name,
                        style = MaterialTheme.typography.labelSmall,
                        color = categoryColor,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(timeStr, style = MaterialTheme.typography.labelSmall, color = Pulse.TextMid)
            }

            Text(
                err.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                err.message,
                style = MaterialTheme.typography.bodySmall,
                color = Pulse.TextMid
            )

            if (!err.technicalDetails.isNullOrBlank()) {
                Surface(
                    color = Color.Black.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                ) {
                    Text(
                        err.technicalDetails,
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                        color = Pulse.TextMid,
                        modifier = Modifier.padding(6.dp)
                    )
                }
            }
        }
    }
}
