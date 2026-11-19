package com.pulse.player.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DashboardCustomize
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pulse.player.data.ErrorCategory
import com.pulse.player.data.ErrorDispatcher
import com.pulse.player.data.settings.ThemeMode
import com.pulse.player.ui.components.LocalBottomOverlayPadding
import com.pulse.player.ui.components.PulseTopBar
import com.pulse.player.ui.components.SurfaceSkinPreview
import com.pulse.player.ui.theme.LocalDarkTheme
import com.pulse.player.ui.theme.Skin
import com.pulse.player.ui.theme.Skins
import com.pulse.player.ui.themes.MountainSunset
import com.pulse.player.ui.themes.StarField
import com.pulse.player.ui.themes.WavesCanvas
import com.pulse.player.ui.viewmodel.MainViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SkinThemeScreen(vm: MainViewModel) {
    val skinId by vm.skinId.collectAsStateWithLifecycle()
    val wallpaperPath by vm.wallpaperPath.collectAsStateWithLifecycle()
    val themeMode by vm.themeMode.collectAsStateWithLifecycle()
    val colorSource by vm.colorSource.collectAsStateWithLifecycle()
    val seedColor by vm.seedColor.collectAsStateWithLifecycle()
    val artworkAccent by vm.artworkAccent.collectAsStateWithLifecycle()
    val cornerRadiusDp by vm.cornerRadiusDp.collectAsStateWithLifecycle()
    val fontStyle by vm.fontStyle.collectAsStateWithLifecycle()
    val playerState by vm.player.state.collectAsStateWithLifecycle()
    var previewExpanded by remember { mutableStateOf(true) }
    val isDark = LocalDarkTheme.current
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()

    BackHandler { vm.back() }

    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch(Dispatchers.IO) {
                runCatching {
                    val out = File(ctx.filesDir, "wallpaper.jpg")
                    val bmp = ctx.contentResolver.openInputStream(uri)?.use { input ->
                        val opts = BitmapFactory.Options().apply { inSampleSize = 2 }
                        BitmapFactory.decodeStream(input, null, opts)
                    }
                    if (bmp != null) {
                        out.outputStream().use { o -> bmp.compress(Bitmap.CompressFormat.JPEG, 85, o) }
                        vm.setWallpaper(out.absolutePath)
                        vm.snackbarMessage.value = "Wallpaper updated"
                    } else {
                        ErrorDispatcher.post(
                            ErrorCategory.STORAGE,
                            "Could not decode selected wallpaper image",
                            "The selected image file could not be decoded."
                        )
                        vm.snackbarMessage.value = "Selected image could not be decoded"
                    }
                }.onFailure { err ->
                    ErrorDispatcher.post(
                        ErrorCategory.STORAGE,
                        "Unable to save custom wallpaper",
                        err.message ?: "Failed to save custom wallpaper image.",
                        err.stackTraceToString()
                    )
                    vm.snackbarMessage.value = "Unable to apply custom wallpaper"
                }
            }
        }
    }

    val daylightSkins = Skins.filter { it.isDaylight }
    val darkSkins = Skins.filter { !it.isDaylight }
    val bottomInset = LocalBottomOverlayPadding.current

    Column(Modifier.fillMaxSize()) {
        PulseTopBar(title = "Skin & Wallpaper", onBack = vm::back)

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .testTag("skin_live_preview_card"),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 12.dp, end = 4.dp, top = 2.dp, bottom = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "LIVE SKIN PREVIEW",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(
                        onClick = { vm.go(Screen.Customize) },
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("skin_open_customize_button")
                    ) {
                        Icon(Icons.Rounded.DashboardCustomize, null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Customize", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                    IconButton(
                        onClick = { previewExpanded = !previewExpanded },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("skin_preview_toggle")
                    ) {
                        Icon(
                            imageVector = if (previewExpanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                            contentDescription = if (previewExpanded) "Collapse preview" else "Expand preview"
                        )
                    }
                }
                AnimatedVisibility(
                    visible = previewExpanded,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    SurfaceSkinPreview(
                        themeMode = themeMode,
                        skinId = skinId,
                        wallpaperPath = wallpaperPath,
                        colorSource = colorSource,
                        seedColor = seedColor,
                        artworkAccent = artworkAccent,
                        cornerRadiusDp = cornerRadiusDp,
                        fontStyle = fontStyle,
                        trackTitle = playerState.title.ifBlank { "Pulse Horizon" },
                        trackArtist = playerState.artist.ifBlank { "Pulse Studio" },
                        artworkUri = playerState.artworkUri,
                        isPlaying = playerState.isPlaying,
                        modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 12.dp)
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = bottomInset + 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item {
                Text("Daylight Skins", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                Text(
                    "Soft daytime tones paired with high-contrast accents",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    daylightSkins.forEach { s ->
                        SkinSwatchItem(
                            skin = s,
                            showLightPreview = true,
                            isSelected = s.id == skinId && wallpaperPath == null,
                            onClick = {
                                vm.setSkin(s.id)
                                vm.setWallpaper(null)
                                if (isDark) vm.setThemeMode(ThemeMode.LIGHT)
                            }
                        )
                    }
                }
            }

            item {
                Text("Atmospheric Skins", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                Text(
                    "Adapts smoothly between Light, Dark, and AMOLED modes",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    darkSkins.forEach { s ->
                        SkinSwatchItem(
                            skin = s,
                            showLightPreview = !isDark,
                            isSelected = s.id == skinId && wallpaperPath == null,
                            onClick = {
                                vm.setSkin(s.id)
                                vm.setWallpaper(null)
                            }
                        )
                    }
                }
            }

            item {
                Text("Live Wallpapers", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
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
                Text("Custom Wallpaper", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(12.dp))
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(88.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .clickable {
                            picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
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
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.Add, "Choose wallpaper", tint = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                        Spacer(Modifier.width(16.dp))
                        Column {
                            Text("Choose from gallery", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            Text("Applies with adaptive contrast scrim", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SkinSwatchItem(
    skin: Skin,
    showLightPreview: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val swatchColors = if (showLightPreview) skin.lightColors else skin.colors
    val primaryColor = swatchColors.firstOrNull() ?: MaterialTheme.colorScheme.surface
    val checkTint = if (primaryColor.luminance() > 0.45f) Color(0xFF141122) else Color.White

    Column(
        modifier = Modifier
            .width(68.dp)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(58.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(swatchColors))
                .border(
                    width = if (isSelected) 3.dp else 1.dp,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isSelected) {
                Icon(Icons.Rounded.Check, contentDescription = "Selected", tint = checkTint, modifier = Modifier.size(22.dp))
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = skin.name,
            style = MaterialTheme.typography.labelSmall,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
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
    val primary = MaterialTheme.colorScheme.primary
    val onPrimary = MaterialTheme.colorScheme.onPrimary
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
                    if (selected) Modifier.border(3.dp, primary, RoundedCornerShape(16.dp))
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
                        .background(primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.Check, contentDescription = "Selected", tint = onPrimary, modifier = Modifier.size(16.dp))
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(name, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
    }
}
