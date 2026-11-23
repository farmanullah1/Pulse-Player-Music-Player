package com.pulse.player.ui.screens

import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.BookmarkAdd
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pulse.player.data.ErrorCategory
import com.pulse.player.data.ErrorDispatcher
import com.pulse.player.playback.PlaybackUiState
import com.pulse.player.playback.RingtoneHelper
import com.pulse.player.ui.components.Artwork
import com.pulse.player.ui.components.GlowArtwork
import com.pulse.player.ui.theme.LocalReducedMotion
import com.pulse.player.ui.themes.AcousticDisc
import com.pulse.player.ui.themes.MountainSunset
import com.pulse.player.ui.themes.PlayerLayout
import com.pulse.player.ui.themes.PlayerThemes
import com.pulse.player.ui.themes.StarField
import com.pulse.player.ui.themes.VinylDisc
import com.pulse.player.ui.themes.WavesCanvas
import com.pulse.player.ui.viewmodel.MainViewModel
import kotlin.math.abs
import kotlin.math.sin

@Composable
fun PlayerThemeHost(vm: MainViewModel, s: PlaybackUiState, glowColor: Color = MaterialTheme.colorScheme.primary) {
    val id by vm.playerThemeId.collectAsStateWithLifecycle()
    val coverShape by vm.playerCoverShape.collectAsStateWithLifecycle()
    val artworkGlow by vm.playerArtworkGlow.collectAsStateWithLifecycle()
    val swipeSkip by vm.playerSwipeSkip.collectAsStateWithLifecycle()
    val cornerRadiusDp by vm.cornerRadiusDp.collectAsStateWithLifecycle()
    val theme = PlayerThemes.firstOrNull { it.id == id } ?: PlayerThemes[0]
    val haptic = LocalHapticFeedback.current
    var dragAccumulated by remember { mutableFloatStateOf(0f) }

    val artRadius = when (coverShape) {
        "CIRCLE", "VINYL_DISC" -> 140.dp
        "SQUARE" -> 4.dp
        "POSTER" -> 12.dp
        else -> cornerRadiusDp.coerceIn(8, 28).dp
    }

    val gestureMod = if (swipeSkip) {
        Modifier.pointerInput(s.mediaId, swipeSkip) {
            detectHorizontalDragGestures(
                onDragStart = { dragAccumulated = 0f },
                onDragEnd = {
                    if (dragAccumulated < -80f) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        vm.player.next()
                    } else if (dragAccumulated > 80f) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        vm.player.previous()
                    }
                    dragAccumulated = 0f
                },
                onDragCancel = { dragAccumulated = 0f },
                onHorizontalDrag = { _, dragAmount -> dragAccumulated += dragAmount }
            )
        }
    } else {
        Modifier
    }

    Box(modifier = gestureMod, contentAlignment = Alignment.Center) {
        Crossfade(theme, animationSpec = tween(400), label = "ptheme") { t ->
            when (t.layout) {
                PlayerLayout.VINYL -> VinylDisc(s)
                PlayerLayout.ACOUSTIC -> AcousticDisc(s)
                PlayerLayout.VINYL_TURNTABLE -> {
                    Surface(
                        shape = RoundedCornerShape(28.dp),
                        color = Color(0xFF23150C).copy(alpha = 0.85f),
                        border = BorderStroke(1.5.dp, Color(0xFFE89B25).copy(alpha = 0.45f)),
                        modifier = Modifier.padding(4.dp)
                    ) {
                        Box(Modifier.padding(14.dp), contentAlignment = Alignment.Center) {
                            VinylDisc(s, modifier = Modifier.size(260.dp))
                        }
                    }
                }
                PlayerLayout.MINIMAL_TYPOGRAPHY -> {
                    Surface(
                        shape = RoundedCornerShape(artRadius),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.24f),
                        border = BorderStroke(1.dp, glowColor.copy(alpha = if (artworkGlow) 0.45f else 0.16f)),
                        modifier = Modifier.size(276.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize().padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = s.album.ifBlank { "Single Release" }.uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                color = glowColor,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Artwork(s.artworkUri, 124.dp, radius = artRadius, px = 384)
                            Text(
                                text = s.title.ifBlank { "Untitled Track" },
                                style = MaterialTheme.typography.headlineSmall.copy(fontFamily = FontFamily.Serif),
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
                PlayerLayout.SPLIT_ARTWORK_LEFT -> {
                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.30f),
                        border = BorderStroke(1.dp, glowColor.copy(alpha = if (artworkGlow) 0.4f else 0.15f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Artwork(s.artworkUri, 140.dp, radius = artRadius, px = 448)
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(s.album.ifBlank { "Now Playing" }, style = MaterialTheme.typography.labelMedium, color = glowColor, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                Text(s.title.ifBlank { "No track selected" }, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                Text(s.artist.ifBlank { "Unknown artist" }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
                PlayerLayout.COMPACT_CARD -> {
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.85f),
                        tonalElevation = 6.dp,
                        border = BorderStroke(1.dp, glowColor.copy(alpha = if (artworkGlow) 0.5f else 0.18f))
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Artwork(s.artworkUri, 204.dp, radius = artRadius, px = 512)
                            Text(s.album.ifBlank { "Pulse Music Player" }, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
                PlayerLayout.WAVEFORM_STUDIO -> {
                    val reducedMotion = LocalReducedMotion.current
                    val animPhase by if (s.isPlaying && !reducedMotion) {
                        rememberInfiniteTransition(label = "waveform_studio_bars").animateFloat(
                            initialValue = 0f,
                            targetValue = 6.2831855f,
                            animationSpec = infiniteRepeatable(animation = tween(2200, easing = LinearEasing), repeatMode = RepeatMode.Restart),
                            label = "phase"
                        )
                    } else {
                        remember { mutableFloatStateOf(0f) }
                    }
                    Box(Modifier.size(284.dp), Alignment.Center) {
                        Canvas(Modifier.fillMaxSize()) {
                            val count = 24
                            val slot = size.width / count
                            val barW = slot * 0.45f
                            val midY = size.height / 2f
                            for (i in 0 until count) {
                                val wave = abs(sin(i * 0.55f + animPhase))
                                val h = size.height * (0.24f + wave * 0.58f)
                                drawRoundRect(
                                    color = glowColor.copy(alpha = 0.24f + wave * 0.14f),
                                    topLeft = Offset(i * slot + (slot - barW) / 2f, midY - h / 2f),
                                    size = Size(barW, h),
                                    cornerRadius = CornerRadius(barW / 2f)
                                )
                            }
                        }
                        if (artworkGlow) GlowArtwork(uri = s.artworkUri, size = 210.dp, glow = glowColor)
                        else Artwork(s.artworkUri, 200.dp, radius = artRadius, px = 512)
                    }
                }
                PlayerLayout.COAST -> Box(Modifier.size(280.dp), Alignment.Center) {
                    WavesCanvas(Modifier.fillMaxSize())
                    Artwork(s.artworkUri, 220.dp, radius = artRadius, px = 512)
                }
                PlayerLayout.STARRY -> Box(Modifier.size(280.dp), Alignment.Center) {
                    StarField(Modifier.fillMaxSize())
                    Artwork(s.artworkUri, 220.dp, radius = artRadius, px = 512)
                }
                PlayerLayout.SHEET_QUEUE -> Box(Modifier.size(280.dp), Alignment.Center) {
                    MountainSunset(Modifier.fillMaxSize())
                    Artwork(s.artworkUri, 220.dp, radius = artRadius, px = 512)
                }
                else -> if (artworkGlow && coverShape == "ROUNDED") {
                    GlowArtwork(uri = s.artworkUri, size = 280.dp, glow = glowColor)
                } else {
                    Artwork(
                        uri = s.artworkUri,
                        size = 264.dp,
                        radius = artRadius,
                        px = 512,
                        modifier = if (artworkGlow) Modifier.border(2.dp, glowColor.copy(alpha = 0.5f), RoundedCornerShape(artRadius)) else Modifier
                    )
                }
            }
        }
    }
}

@Composable
fun SongLyricsSegment(showLyrics: Boolean, onSelect: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(22.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f))
            .padding(4.dp)
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(18.dp))
                .background(if (!showLyrics) MaterialTheme.colorScheme.primary else Color.Transparent)
                .clickable { onSelect(false) }
                .padding(horizontal = 16.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "Song",
                fontWeight = FontWeight.Bold,
                color = if (!showLyrics) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall
            )
        }
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(18.dp))
                .background(if (showLyrics) MaterialTheme.colorScheme.primary else Color.Transparent)
                .clickable { onSelect(true) }
                .padding(horizontal = 16.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "Lyrics",
                fontWeight = FontWeight.Bold,
                color = if (showLyrics) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
fun PlayerTopMenu(vm: MainViewModel) {
    var expanded by remember { mutableStateOf(false) }
    val ctx = LocalContext.current
    val s by vm.player.state.collectAsStateWithLifecycle()

    Box {
        IconButton(onClick = { expanded = true }, modifier = Modifier.size(48.dp)) {
            Icon(Icons.Rounded.MoreVert, "More actions", tint = MaterialTheme.colorScheme.onSurface)
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(MaterialTheme.colorScheme.surfaceContainerHigh)
        ) {
            DropdownMenuItem(
                text = { Text("Driving Mode", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold) },
                leadingIcon = { Icon(Icons.Rounded.DirectionsCar, null, tint = MaterialTheme.colorScheme.primary) },
                onClick = {
                    expanded = false
                    vm.openDrivingMode()
                }
            )
            DropdownMenuItem(
                text = { Text("Play Similar Mix", color = MaterialTheme.colorScheme.onSurface) },
                leadingIcon = { Icon(Icons.Rounded.AutoAwesome, null, tint = MaterialTheme.colorScheme.primary) },
                onClick = {
                    expanded = false
                    vm.currentPlayingTrack()?.let { vm.playSimilarMix(it) }
                        ?: run { vm.snackbarMessage.value = "Play a track to generate a mix" }
                }
            )
            DropdownMenuItem(
                text = { Text("Bookmark Current Moment", color = MaterialTheme.colorScheme.onSurface) },
                leadingIcon = { Icon(Icons.Rounded.BookmarkAdd, null, tint = MaterialTheme.colorScheme.primary) },
                onClick = {
                    expanded = false
                    val id = s.mediaId
                    if (!id.isNullOrBlank()) {
                        vm.addBookmark(id, vm.player.position.value)
                    } else {
                        vm.snackbarMessage.value = "Play a track to save a bookmark"
                    }
                }
            )
            DropdownMenuItem(
                text = { Text("Bookmarks, Speed & Loop Controls", color = MaterialTheme.colorScheme.onSurface) },
                onClick = { expanded = false; vm.openAudioControls() }
            )
            DropdownMenuItem(
                text = { Text("Edit Track Tags & Art", color = MaterialTheme.colorScheme.onSurface) },
                onClick = { expanded = false; vm.openTagEditor() }
            )
            DropdownMenuItem(
                text = { Text("Custom Player Studio", color = MaterialTheme.colorScheme.onSurface) },
                onClick = { expanded = false; vm.closeNowPlaying(); vm.go(Screen.Customize) }
            )
            DropdownMenuItem(
                text = { Text("Equalizer", color = MaterialTheme.colorScheme.onSurface) },
                onClick = { expanded = false; vm.go(Screen.Equalizer) }
            )
            DropdownMenuItem(
                text = { Text("Set as Ringtone", color = MaterialTheme.colorScheme.onSurface) },
                onClick = {
                    expanded = false
                    val audioUri = vm.currentPlayingTrack()?.uri ?: s.artworkUri
                    if (!audioUri.isNullOrBlank()) {
                        runCatching {
                            RingtoneHelper.setRingtone(ctx, Uri.parse(audioUri), RingtoneManager.TYPE_RINGTONE)
                        }.onSuccess { ok ->
                            vm.snackbarMessage.value = if (ok) "Ringtone updated" else "Could not set ringtone for this file"
                        }.onFailure { err ->
                            ErrorDispatcher.post(ErrorCategory.STORAGE, "Unable to set ringtone", err.message ?: "Ringtone settings permission or file access failed.", err.stackTraceToString())
                            vm.snackbarMessage.value = "Unable to set ringtone"
                        }
                    } else {
                        vm.snackbarMessage.value = "Select a track first"
                    }
                }
            )
            DropdownMenuItem(
                text = { Text("Share Track", color = MaterialTheme.colorScheme.onSurface) },
                onClick = {
                    expanded = false
                    val audioUri = vm.currentPlayingTrack()?.uri ?: s.artworkUri
                    if (!audioUri.isNullOrBlank()) {
                        runCatching {
                            val share = Intent(Intent.ACTION_SEND).apply {
                                type = "audio/*"
                                putExtra(Intent.EXTRA_STREAM, Uri.parse(audioUri))
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            ctx.startActivity(Intent.createChooser(share, "Share ${s.title}"))
                        }.onFailure { err ->
                            ErrorDispatcher.post(ErrorCategory.STORAGE, "Unable to share track", err.message ?: "No compatible app found to share audio.", err.stackTraceToString())
                            vm.snackbarMessage.value = "Unable to share track"
                        }
                    } else {
                        vm.snackbarMessage.value = "Select a track first"
                    }
                }
            )
        }
    }
}
