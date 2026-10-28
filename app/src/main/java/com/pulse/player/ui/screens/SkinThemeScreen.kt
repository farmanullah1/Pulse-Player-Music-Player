package com.pulse.player.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pulse.player.ui.theme.Pulse
import com.pulse.player.ui.theme.Skins
import com.pulse.player.ui.themes.MountainSunset
import com.pulse.player.ui.themes.StarField
import com.pulse.player.ui.themes.WavesCanvas
import com.pulse.player.ui.viewmodel.MainViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun SkinThemeScreen(vm: MainViewModel) {
    val skinId by vm.skinId.collectAsStateWithLifecycle()
    val wallpaperPath by vm.wallpaperPath.collectAsStateWithLifecycle()
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()

    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch(Dispatchers.IO) {
                val out = File(ctx.filesDir, "wallpaper.jpg")
                ctx.contentResolver.openInputStream(uri)?.use { input ->
                    val opts = BitmapFactory.Options().apply { inSampleSize = 2 }
                    val bmp = BitmapFactory.decodeStream(input, null, opts)
                    if (bmp != null) {
                        out.outputStream().use { o -> bmp.compress(Bitmap.CompressFormat.JPEG, 85, o) }
                        vm.setWallpaper(out.absolutePath)
                    }
                }
            }
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
            Text("Skin & Wallpaper", style = MaterialTheme.typography.titleLarge, color = Color.White)
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item {
                Text("Color Skins", style = MaterialTheme.typography.titleMedium, color = Color.White)
                Spacer(Modifier.height(12.dp))
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(64.dp),
                    modifier = Modifier.height(160.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(Skins, key = { it.id }) { s ->
                        val isSelected = s.id == skinId && wallpaperPath == null
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(Brush.linearGradient(s.colors))
                                .clickable {
                                    vm.setSkin(s.id)
                                    vm.setWallpaper(null)
                                }
                                .then(
                                    if (isSelected) Modifier.border(3.dp, Color.White, CircleShape)
                                    else Modifier
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(Icons.Rounded.Check, null, tint = Color.White, modifier = Modifier.size(24.dp))
                            }
                        }
                    }
                }
            }

            item {
                Text("Procedural Wallpapers", style = MaterialTheme.typography.titleMedium, color = Color.White)
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    WallpaperThumb(
                        name = "Starry Sky",
                        selected = wallpaperPath == "preset:starry",
                        modifier = Modifier.weight(1f),
                        onClick = { vm.setWallpaper("preset:starry") }
                    ) {
                        StarField(Modifier.fillMaxSize())
                    }
                    WallpaperThumb(
                        name = "Ocean Waves",
                        selected = wallpaperPath == "preset:waves",
                        modifier = Modifier.weight(1f),
                        onClick = { vm.setWallpaper("preset:waves") }
                    ) {
                        WavesCanvas(Modifier.fillMaxSize())
                    }
                    WallpaperThumb(
                        name = "Sunset",
                        selected = wallpaperPath == "preset:mountain",
                        modifier = Modifier.weight(1f),
                        onClick = { vm.setWallpaper("preset:mountain") }
                    ) {
                        MountainSunset(Modifier.fillMaxSize())
                    }
                }
            }

            item {
                Text("Custom Wallpaper", style = MaterialTheme.typography.titleMedium, color = Color.White)
                Spacer(Modifier.height(12.dp))
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(88.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .clickable {
                            picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        },
                    colors = CardDefaults.cardColors(containerColor = Pulse.Surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(Pulse.Elevated),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.Add, null, tint = Pulse.Cyan)
                        }
                        Spacer(Modifier.width(16.dp))
                        Column {
                            Text("Choose from gallery", fontWeight = FontWeight.Bold, color = Color.White)
                            Text("Applies as backdrop with AMOLED dimming", style = MaterialTheme.typography.bodySmall, color = Pulse.TextMid)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WallpaperThumb(
    name: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    Column(
        modifier = modifier.clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(3f / 4f)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF0F0B1A))
                .then(
                    if (selected) Modifier.border(3.dp, Pulse.Cyan, RoundedCornerShape(16.dp))
                    else Modifier
                )
        ) {
            content()
            if (selected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(Pulse.Cyan),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.Check, null, tint = Color.Black, modifier = Modifier.size(16.dp))
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(name, style = MaterialTheme.typography.bodySmall, color = Color.White)
    }
}
