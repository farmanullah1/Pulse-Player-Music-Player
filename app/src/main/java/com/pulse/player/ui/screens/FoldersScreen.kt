package com.pulse.player.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pulse.player.ui.theme.Pulse
import com.pulse.player.ui.viewmodel.MainViewModel

@Composable
fun FoldersScreen(vm: MainViewModel) {
    val tracks by vm.tracks.collectAsStateWithLifecycle()
    val folders = remember(tracks) {
        tracks.groupBy { it.folder.ifBlank { "Music" } }
            .map { (folder, list) -> folder to list.size }
            .sortedBy { it.first.lowercase() }
    }
    val safLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) vm.addSafFolder(uri)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("${folders.size} Folders", style = MaterialTheme.typography.titleLarge, color = Color.White)
                Button(
                    onClick = { safLauncher.launch(null) },
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = Pulse.Elevated, contentColor = Pulse.Cyan)
                ) {
                    Icon(Icons.Rounded.FolderOpen, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Add Folder", fontWeight = FontWeight.Bold)
                }
            }
        }

        if (folders.isEmpty()) {
            item {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No audio folders found", color = Pulse.TextMid)
                }
            }
        } else {
            items(folders, key = { it.first }) { (folderName, count) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Pulse.Surface)
                        .clickable { vm.go(Screen.Detail("folder", folderName)) }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Pulse.Elevated),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.Folder, null, tint = Pulse.Cyan, modifier = Modifier.size(28.dp))
                    }
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = folderName.substringAfterLast('/'),
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White
                        )
                        Text(
                            text = "$count songs · $folderName",
                            style = MaterialTheme.typography.bodySmall,
                            color = Pulse.TextMid
                        )
                    }
                }
            }
        }
    }
}
