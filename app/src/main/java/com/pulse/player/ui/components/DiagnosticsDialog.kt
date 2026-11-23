package com.pulse.player.ui.components

import android.content.pm.PackageManager
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.SdCard
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pulse.player.BuildConfig
import com.pulse.player.data.AppError
import com.pulse.player.data.CrashRecord
import com.pulse.player.data.ErrorCategory
import com.pulse.player.data.scan.audioPermission
import com.pulse.player.ui.theme.LocalPulseTokens
import com.pulse.player.ui.theme.TabularMono
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
    val crashRecords by vm.crashRecords.collectAsStateWithLifecycle()
    val missingCount by vm.missingCount.collectAsStateWithLifecycle()
    val ctx = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val tokens = LocalPulseTokens.current
    var selectedCategory by remember { mutableStateOf<ErrorCategory?>(null) }

    val filteredErrors = remember(recentErrors, selectedCategory) {
        if (selectedCategory == null) recentErrors else recentErrors.filter { it.category == selectedCategory }
    }

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
                    .navigationBarsPadding()
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
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Rounded.Build,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
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
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Rounded.Close, "Close", tint = MaterialTheme.colorScheme.onSurface)
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Scrollable content
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 0. Crash Reports (Persistent uncaught crash log)
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Rounded.BugReport,
                                    contentDescription = null,
                                    tint = if (crashRecords.isNotEmpty()) tokens.error else tokens.success,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "Crash Reports (${crashRecords.size})",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            if (crashRecords.isNotEmpty()) {
                                TextButton(onClick = vm::clearCrashLogs) {
                                    Text("Clear Crashes", color = tokens.error, style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }
                    }

                    if (crashRecords.isEmpty()) {
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
                                    Icon(Icons.Rounded.CheckCircle, null, tint = tokens.success, modifier = Modifier.size(22.dp))
                                    Spacer(Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            "No crashes recorded",
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            "Unexpected uncaught exceptions will be captured automatically here.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        items(crashRecords, key = { "crash_${it.signature}" }) { crash ->
                            CrashRecordCard(
                                crash = crash,
                                onCopy = {
                                    clipboard.setText(AnnotatedString(vm.formatCrashReport(crash)))
                                    vm.snackbarMessage.value = "Crash report copied"
                                }
                            )
                        }
                    }

                    // 1. Audio Engine & DSP Hardware
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Rounded.GraphicEq, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
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
                                    Icon(Icons.Rounded.SdCard, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(20.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Storage & Privacy", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                }

                                HardwareStatusRow("Audio Permission", if (hasAudioPermission) "Granted" else "Missing", isOk = hasAudioPermission)
                                HardwareStatusRow("Missing Tracks", if (missingCount == 0) "0 (Clean)" else "$missingCount unreachable", isOk = missingCount == 0)
                                HardwareStatusRow("Network Policy", "100% Offline (Local library)", isOk = true)
                            }
                        }
                    }

                    // 3. Error Log
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Live Error Log (${filteredErrors.size})",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                if (recentErrors.isNotEmpty()) {
                                    TextButton(onClick = vm::clearErrorLog) {
                                        Text("Clear Log", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium)
                                    }
                                }
                            }
                            if (recentErrors.isNotEmpty()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    FilterChip(
                                        selected = selectedCategory == null,
                                        onClick = { selectedCategory = null },
                                        label = { Text("All (${recentErrors.size})") }
                                    )
                                    ErrorCategory.entries.forEach { cat ->
                                        val count = recentErrors.count { it.category == cat }
                                        if (count > 0) {
                                            FilterChip(
                                                selected = selectedCategory == cat,
                                                onClick = { selectedCategory = if (selectedCategory == cat) null else cat },
                                                label = { Text("${cat.name} ($count)") }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (filteredErrors.isEmpty()) {
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
                                    Icon(Icons.Rounded.CheckCircle, null, tint = tokens.success, modifier = Modifier.size(24.dp))
                                    Spacer(Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            if (recentErrors.isEmpty()) "No errors recorded" else "No errors in this category",
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            if (recentErrors.isEmpty()) "All audio pipelines and database queries are healthy." else "Select All to inspect other recorded events.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        items(filteredErrors, key = { it.id }) { err ->
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
                    if (recentErrors.isNotEmpty() || crashRecords.isNotEmpty()) {
                        OutlinedButton(
                            onClick = {
                                val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
                                val report = buildString {
                                    appendLine("Pulse Music Player v${BuildConfig.VERSION_NAME} Diagnostics Report")
                                    appendLine("Audio Permission: $hasAudioPermission | Missing Tracks: $missingCount | Session: ${fxStatus.sessionId}")
                                    if (crashRecords.isNotEmpty()) {
                                        appendLine("--- CRASH REPORTS (${crashRecords.size}) ---")
                                        crashRecords.forEach { c ->
                                            appendLine(vm.formatCrashReport(c))
                                            appendLine()
                                        }
                                    }
                                    appendLine("--- ERROR LOG (${filteredErrors.size}) ---")
                                    filteredErrors.forEach { e ->
                                        appendLine("[${fmt.format(Date(e.timestampMs))}] [${e.category.name}] ${e.title}: ${e.message}")
                                        e.technicalDetails?.let { appendLine("  Details: ${it.take(300)}") }
                                    }
                                }
                                clipboard.setText(AnnotatedString(report))
                                vm.snackbarMessage.value = "Diagnostics report copied"
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Rounded.ContentCopy, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Copy")
                        }
                    }
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
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Text("Close", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun HardwareStatusRow(label: String, value: String, isOk: Boolean) {
    val tokens = LocalPulseTokens.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(if (isOk) tokens.success else tokens.error)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                value,
                style = MaterialTheme.typography.bodySmall.merge(TabularMono),
                fontWeight = FontWeight.SemiBold,
                color = if (isOk) MaterialTheme.colorScheme.onSurface else tokens.error
            )
        }
    }
}

@Composable
private fun ErrorItemCard(err: AppError) {
    val tokens = LocalPulseTokens.current
    val timeStr = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(err.timestampMs))
    val categoryColor = when (err.category) {
        ErrorCategory.CRASH -> tokens.error
        ErrorCategory.PLAYBACK -> tokens.error
        ErrorCategory.SCAN -> tokens.warning
        ErrorCategory.BACKUP -> MaterialTheme.colorScheme.primary
        ErrorCategory.AUDIO_FX -> MaterialTheme.colorScheme.secondary
        ErrorCategory.STORAGE -> tokens.warning
        ErrorCategory.NETWORK -> MaterialTheme.colorScheme.tertiary
        ErrorCategory.GENERAL -> MaterialTheme.colorScheme.secondary
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
                        .background(categoryColor.copy(alpha = 0.16f))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        err.category.name,
                        style = MaterialTheme.typography.labelSmall,
                        color = categoryColor,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    timeStr,
                    style = MaterialTheme.typography.labelSmall.merge(TabularMono),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
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
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (!err.technicalDetails.isNullOrBlank()) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                ) {
                    Text(
                        err.technicalDetails,
                        style = MaterialTheme.typography.labelSmall.merge(TabularMono),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(6.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun CrashRecordCard(
    crash: CrashRecord,
    onCopy: () -> Unit
) {
    val tokens = LocalPulseTokens.current
    var expanded by remember(crash.signature) { mutableStateOf(false) }
    val timeStr = remember(crash.lastSeenMs) {
        SimpleDateFormat("MMM d, HH:mm:ss", Locale.getDefault()).format(Date(crash.lastSeenMs))
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(tokens.error.copy(alpha = 0.16f))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            "CRASH",
                            style = MaterialTheme.typography.labelSmall,
                            color = tokens.error,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    if (crash.count > 1) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .padding(horizontal = 7.dp, vertical = 2.dp)
                        ) {
                            Text(
                                "Occurred ${crash.count}x",
                                style = MaterialTheme.typography.labelSmall.merge(TabularMono),
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
                Text(
                    timeStr,
                    style = MaterialTheme.typography.labelSmall.merge(TabularMono),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                crash.exceptionClass,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                crash.message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                "v${crash.versionName} (${crash.versionCode}) · Thread: ${crash.threadName}",
                style = MaterialTheme.typography.labelSmall.merge(TabularMono),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = { expanded = !expanded }) {
                    Icon(
                        if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(if (expanded) "Hide Stack Trace" else "View Stack Trace")
                }
                TextButton(onClick = onCopy) {
                    Icon(Icons.Rounded.ContentCopy, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Copy")
                }
            }

            if (expanded) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = crash.stackTrace,
                        style = MaterialTheme.typography.labelSmall.merge(TabularMono),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .horizontalScroll(rememberScrollState())
                            .padding(8.dp)
                    )
                }
            }
        }
    }
}
