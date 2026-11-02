package com.pulse.player.ui.themes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pulse.player.ui.theme.Pulse

enum class PlayerLayout {
    CLASSIC,
    SHEET_QUEUE,
    MESH_GLASS,
    MINIMAL_DARK,
    VINYL,
    ACOUSTIC,
    COAST,
    STARRY,
    IMMERSIVE,
    PIXEL,
    NAMIDA,
    ATMOSPHERIC_QUEUE
}

data class PlayerTheme(
    val id: String,
    val name: String,
    val layout: PlayerLayout,
    val colors: List<Color>,
    val special: Boolean = false
)

val PlayerThemes = listOf(
    PlayerTheme("classic", "Classic", PlayerLayout.CLASSIC, listOf(Pulse.PlumTop, Pulse.PlumBottom)),
    PlayerTheme("mountain", "Mountain Horizon", PlayerLayout.SHEET_QUEUE, listOf(Color(0xFFFF8C5A), Color(0xFF6B2C5E), Color(0xFF1B1030)), special = true),
    PlayerTheme("mesh", "Mesh Glass", PlayerLayout.MESH_GLASS, listOf(Pulse.Magenta, Color(0xFF051937))),
    PlayerTheme("minimal", "Minimal Mood", PlayerLayout.MINIMAL_DARK, listOf(Color(0xFF2A2A2E), Color.Black)),
    PlayerTheme("acoustic", "Acoustic", PlayerLayout.ACOUSTIC, listOf(Color(0xFF6B3E26), Color(0xFF1E0F08)), special = true),
    PlayerTheme("vinyl", "Vinyl", PlayerLayout.VINYL, listOf(Color(0xFF3A2A7A), Color(0xFF0E0A24)), special = true),
    PlayerTheme("coast", "Coastal Ocean", PlayerLayout.COAST, listOf(Color(0xFF0F4C75), Color(0xFF051937))),
    PlayerTheme("starry", "Starry Sky", PlayerLayout.STARRY, listOf(Color(0xFF0A1030), Color(0xFF2A3350))),
    PlayerTheme("immersive", "Immersive Artwork Player", PlayerLayout.IMMERSIVE, listOf(Color(0xFF1B1B22), Color.Black)),
    PlayerTheme("pixel", "Pixel Expressive", PlayerLayout.PIXEL, listOf(Color(0xFF295A7E), Color(0xFF132F47), Color.Black), special = true),
    PlayerTheme("namida", "Namida Waveform", PlayerLayout.NAMIDA, listOf(Color(0xFF463E35), Color(0xFF1E1A17), Color.Black), special = true),
    PlayerTheme(
        "atmospheric_queue",
        "Atmospheric Queue",
        PlayerLayout.ATMOSPHERIC_QUEUE,
        listOf(Color(0xFF102D2B), Color(0xFF020706)),
        special = true
    )
)

@Composable
fun ThemePreviewCard(
    theme: PlayerTheme,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(9f / 16f),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(theme.colors))
        ) {
            when (theme.layout) {
                PlayerLayout.STARRY -> StarField(Modifier.fillMaxSize())
                PlayerLayout.COAST -> WavesCanvas(Modifier.fillMaxSize())
                PlayerLayout.SHEET_QUEUE -> MountainSunset(Modifier.fillMaxSize())
                PlayerLayout.IMMERSIVE -> ImmersiveCardBackground(Modifier.fillMaxSize())
                PlayerLayout.PIXEL -> PixelCardBackground(Modifier.fillMaxSize())
                PlayerLayout.NAMIDA -> NamidaCardBackground(Modifier.fillMaxSize())
                PlayerLayout.ATMOSPHERIC_QUEUE -> AtmosphericQueueCardBackground(Modifier.fillMaxSize())
                else -> Unit
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.4f))
                    )
                    Text(
                        text = theme.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.4f))
                    )
                }

                Box(
                    modifier = Modifier
                        .size(130.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.MusicNote,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.size(48.dp)
                    )
                }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .width(120.dp)
                            .height(10.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.8f))
                    )
                    Spacer(Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .width(80.dp)
                            .height(8.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.4f))
                    )
                    Spacer(Modifier.height(16.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.8f)
                            .height(4.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.3f))
                    )
                    Spacer(Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(0.7f),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.SkipPrevious, null, tint = Color.White, modifier = Modifier.size(20.dp))
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color.White),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.PlayArrow, null, tint = Color.Black, modifier = Modifier.size(20.dp))
                        }
                        Icon(Icons.Rounded.SkipNext, null, tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun ImmersiveCardBackground(modifier: Modifier = Modifier) {
    androidx.compose.foundation.Canvas(modifier = modifier) {
        val cx = size.width * 0.5f
        val cy = size.height * 0.4f
        drawCircle(
            color = Color(0xFFFF9500).copy(alpha = 0.25f),
            radius = size.width * 0.45f,
            center = androidx.compose.ui.geometry.Offset(cx - 30f, cy - 20f)
        )
        drawCircle(
            color = Color(0xFFFF2A6D).copy(alpha = 0.20f),
            radius = size.width * 0.5f,
            center = androidx.compose.ui.geometry.Offset(cx + 40f, cy + 30f)
        )
        drawCircle(
            color = Color(0xFF9B7BFF).copy(alpha = 0.22f),
            radius = size.width * 0.4f,
            center = androidx.compose.ui.geometry.Offset(cx, cy + 10f)
        )
    }
}

@Composable
private fun PixelCardBackground(modifier: Modifier = Modifier) {
    androidx.compose.foundation.Canvas(modifier = modifier) {
        val barWidth = size.width * 0.12f
        for (i in 0..6) {
            val x = i * (size.width / 6f)
            val h = size.height * (0.30f + (i % 3) * 0.20f)
            drawRoundRect(
                color = Color(0xFF295A7E).copy(alpha = 0.25f + i * 0.04f),
                topLeft = androidx.compose.ui.geometry.Offset(x, size.height - h),
                size = androidx.compose.ui.geometry.Size(barWidth, h),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(barWidth / 2f)
            )
        }
    }
}

@Composable
private fun NamidaCardBackground(modifier: Modifier = Modifier) {
    androidx.compose.foundation.Canvas(modifier = modifier) {
        val barCount = 12
        val slotW = size.width / barCount.toFloat()
        val barW = slotW * 0.6f
        for (i in 0 until barCount) {
            val x = i * slotW + (slotW - barW) / 2f
            val phase = i * 0.8f
            val sinVal = kotlin.math.sin(phase.toDouble()).toFloat()
            val h = size.height * (0.20f + kotlin.math.abs(sinVal) * 0.60f)
            drawRoundRect(
                color = Color(0xFFF1D6B6).copy(alpha = 0.35f),
                topLeft = androidx.compose.ui.geometry.Offset(x, size.height - h),
                size = androidx.compose.ui.geometry.Size(barW, h),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(barW / 2f)
            )
        }
    }
}

@Composable
private fun AtmosphericQueueCardBackground(modifier: Modifier = Modifier) {
    androidx.compose.foundation.Canvas(modifier = modifier) {
        drawRect(
            brush = Brush.verticalGradient(
                listOf(Color(0xFF183B38), Color(0xFF071312), Color.Black)
            )
        )
        for (i in 0..5) {
            val x = size.width * (i / 5f)
            drawLine(
                color = Color(0xFF8EAEA0).copy(alpha = 0.18f),
                start = androidx.compose.ui.geometry.Offset(x, size.height * 0.18f),
                end = androidx.compose.ui.geometry.Offset(x - size.width * 0.16f, size.height),
                strokeWidth = size.width * 0.035f
            )
        }
        drawOval(
            brush = Brush.radialGradient(listOf(Color(0xFFBBD8C8).copy(alpha = 0.18f), Color.Transparent)),
            topLeft = androidx.compose.ui.geometry.Offset(size.width * 0.18f, size.height * 0.10f),
            size = androidx.compose.ui.geometry.Size(size.width * 0.65f, size.height * 0.34f)
        )
    }
}
