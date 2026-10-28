package com.pulse.player.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Upload
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pulse.player.ui.theme.Pulse
import com.pulse.player.ui.viewmodel.MainViewModel
import java.time.LocalDate

@Composable
fun BackupScreen(vm: MainViewModel) {
    val favCount by vm.favoriteCount.collectAsStateWithLifecycle()
    val exportStatus by vm.exportStatus.collectAsStateWithLifecycle()
    val ctx = LocalContext.current

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            vm.exportFavorites(uri)
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val fileName = uri.lastPathSegment?.substringAfterLast('/') ?: "PulsePlayer_Favorites.json"
            vm.previewFavoritesImport(uri, fileName)
        }
    }

    val safFolderLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            vm.addSafFolder(uri)
            Toast.makeText(ctx, "Folder added to music library", Toast.LENGTH_SHORT).show()
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
            Text("Backup & Transfer", style = MaterialTheme.typography.titleLarge, color = Color.White)
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            if (exportStatus != null) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Pulse.Elevated),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            text = exportStatus.orEmpty(),
                            color = Pulse.Cyan,
                            modifier = Modifier.padding(16.dp),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Pulse.Surface),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Upload, null, tint = Pulse.Cyan, modifier = Modifier.size(28.dp))
                            Spacer(Modifier.width(12.dp))
                            Text("Export Favourites", style = MaterialTheme.typography.titleMedium, color = Color.White)
                        }
                        Text(
                            text = "Save your current $favCount favourites into a lightweight JSON file. You can easily restore or transfer them to another device.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Pulse.TextMid
                        )
                        Button(
                            onClick = {
                                exportLauncher.launch("PulsePlayer_Favorites_${LocalDate.now()}.json")
                            },
                            enabled = favCount > 0,
                            modifier = Modifier.fillMaxWidth(),
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(containerColor = Pulse.Cyan, contentColor = Color.Black)
                        ) {
                            Text(if (favCount > 0) "Export $favCount Favourites" else "No Favourites Yet", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Pulse.Surface),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Download, null, tint = Pulse.Mint, modifier = Modifier.size(28.dp))
                            Spacer(Modifier.width(12.dp))
                            Text("Import Favourites", style = MaterialTheme.typography.titleMedium, color = Color.White)
                        }
                        Text(
                            text = "Pick a PulsePlayer_Favorites.json file. Pulse Player will fuzzy-match songs by filename, title, artist, and duration even if file paths changed.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Pulse.TextMid
                        )
                        Button(
                            onClick = {
                                importLauncher.launch(arrayOf("application/json", "*/*"))
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(containerColor = Pulse.Mint, contentColor = Color.Black)
                        ) {
                            Text("Choose JSON File to Import", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Pulse.Surface),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.FolderOpen, null, tint = Pulse.Violet, modifier = Modifier.size(28.dp))
                            Spacer(Modifier.width(12.dp))
                            Text("Dedicated Music Folder", style = MaterialTheme.typography.titleMedium, color = Color.White)
                        }
                        Text(
                            text = "Select a folder on your phone or SD card where you keep copied music files. Pulse Player will scan and index it automatically.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Pulse.TextMid
                        )
                        OutlinedButton(
                            onClick = { safFolderLauncher.launch(null) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = CircleShape
                        ) {
                            Text("Choose Music Folder", color = Color.White)
                        }
                    }
                }
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Pulse.Elevated),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("How to move to a new device:", fontWeight = FontWeight.Bold, color = Color.White)
                        Text("1. On old phone: Tap 'Export Favourites' above.", style = MaterialTheme.typography.bodySmall, color = Pulse.TextMid)
                        Text("2. Copy your music files and the exported JSON file to your new device (via USB, SD card, Quick Share, etc.).", style = MaterialTheme.typography.bodySmall, color = Pulse.TextMid)
                        Text("3. On new phone: Install Pulse Player and allow audio scan.", style = MaterialTheme.typography.bodySmall, color = Pulse.TextMid)
                        Text("4. Open this screen and tap 'Import Favourites' to restore all your favorites.", style = MaterialTheme.typography.bodySmall, color = Pulse.TextMid)
                    }
                }
            }
        }
    }
}
