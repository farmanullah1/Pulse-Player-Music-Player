package com.pulse.player.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pulse.player.playback.Track
import com.pulse.player.ui.theme.LocalReducedMotion
import com.pulse.player.ui.theme.Pulse
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun Modifier.interactionSource(s: MutableInteractionSource): Modifier =
    this.indication(s, ripple())

fun Modifier.pressScale(enabled: Boolean = true): Modifier = composed {
    val reduced = LocalReducedMotion.current
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val s by animateFloatAsState(
        targetValue = if (pressed && enabled && !reduced) 0.96f else 1f,
        animationSpec = spring(0.55f, Spring.StiffnessMedium),
        label = "press"
    )
    this.interactionSource(src).graphicsLayer {
        scaleX = s
        scaleY = s
    }
}

fun Color.darkened(f: Float): Color =
    Color(red * (1 - f), green * (1 - f), blue * (1 - f), alpha)

@Composable
fun AuroraBackground(
    accent: Color,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val reduced = LocalReducedMotion.current
    val t by if (reduced) {
        remember { mutableFloatStateOf(0f) }
    } else {
        rememberInfiniteTransition(label = "aurora").animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(14000, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "t"
        )
    }
    val a1 by animateColorAsState(accent.darkened(0.55f), tween(500), label = "a1")
    val a2 by animateColorAsState(Pulse.Violet.copy(alpha = .55f), tween(500), label = "a2")

    Box(
        modifier = modifier
            .background(Color.Black)
            .drawBehind {
                val w = size.width
                val h = size.height
                if (w > 0f && h > 0f) {
                    val r1 = (w * 0.9f).coerceAtLeast(1f)
                    val r2 = (w * 0.8f).coerceAtLeast(1f)
                    drawRect(
                        brush = Brush.radialGradient(
                            colors = listOf(a1, Color.Transparent),
                            center = Offset(w * (0.2f + 0.3f * t), h * 0.18f),
                            radius = r1
                        )
                    )
                    drawRect(
                        brush = Brush.radialGradient(
                            colors = listOf(a2.copy(alpha = .35f), Color.Transparent),
                            center = Offset(w * (0.9f - 0.3f * t), h * 0.55f),
                            radius = r2
                        )
                    )
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black),
                            startY = h * 0.55f
                        )
                    )
                }
            },
        content = content
    )
}

@Composable
fun GlowArtwork(
    uri: String?,
    size: Dp,
    glow: Color,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .matchParentSize()
                .padding(8.dp)
                .drawBehind {
                    val minDim = this.size.minDimension
                    if (minDim > 0f) {
                        val r = (minDim * 0.75f).coerceAtLeast(1f)
                        drawCircle(
                            Brush.radialGradient(
                                colors = listOf(glow.copy(alpha = 0.55f), Color.Transparent),
                                radius = r
                            ),
                            radius = r
                        )
                    }
                }
        )
        Artwork(uri, size * 0.9f, radius = 20.dp, px = 512)
    }
}

@Composable
fun GradientPill(
    text: String,
    icon: ImageVector,
    colors: List<Color>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .height(52.dp)
            .clip(CircleShape)
            .background(Brush.horizontalGradient(colors))
            .pressScale()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(icon, null, tint = Color.White)
        Spacer(Modifier.width(8.dp))
        Text(text, color = Color.White, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun HeartButton(
    on: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scale = remember { Animatable(1f) }
    LaunchedEffect(on) {
        if (on) {
            scale.animateTo(1.35f, spring(Spring.DampingRatioMediumBouncy))
            scale.animateTo(1f, spring())
        }
    }
    IconButton(onClick = onToggle, modifier = modifier.size(48.dp)) {
        Icon(
            imageVector = if (on) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
            contentDescription = if (on) "Remove from favourites" else "Add to favourites",
            tint = if (on) Pulse.Cyan else Pulse.TextMid,
            modifier = Modifier.scale(scale.value)
        )
    }
}

@Composable
fun PlayingBars(
    color: Color = Pulse.Cyan,
    active: Boolean = true,
    modifier: Modifier = Modifier
) {
    val t = rememberInfiniteTransition(label = "bars")
    val reduced = LocalReducedMotion.current
    Row(
        modifier = modifier.size(20.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        listOf(500, 650, 800).forEach { d ->
            val h by if (active && !reduced) {
                t.animateFloat(
                    initialValue = 0.25f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(tween(d), RepeatMode.Reverse),
                    label = "b"
                )
            } else {
                remember { mutableFloatStateOf(0.6f) }
            }
            Box(
                Modifier
                    .width(4.dp)
                    .fillMaxHeight(h)
                    .clip(RoundedCornerShape(2.dp))
                    .background(color)
            )
        }
    }
}

@Composable
fun PillButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.White,
            contentColor = Color.Black
        )
    ) {
        Icon(icon, null, Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text(text, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun WidgetTile(
    title: String,
    value: String,
    icon: ImageVector,
    colors: List<Color>,
    modifier: Modifier,
    subtitle: String? = null,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(96.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.linearGradient(colors.map { it.copy(alpha = 0.85f) }))
            .pressScale()
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.9f),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(24.dp)
        )
        Column(Modifier.align(Alignment.BottomStart)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.85f)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White
            )
            subtitle?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
fun SquareCard(
    t: Track,
    size: Dp,
    onClick: () -> Unit
) {
    Column(
        Modifier
            .width(size)
            .pressScale()
            .clickable(onClick = onClick)
    ) {
        Artwork(t.uri, size, radius = 14.dp, px = 384)
        Spacer(Modifier.height(8.dp))
        Text(
            text = t.title,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Text(
            text = t.artist,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodySmall,
            color = Pulse.TextMid
        )
    }
}

@Composable
fun SectionHeader(
    text: String,
    badge: String? = null,
    action: String? = null,
    onAction: () -> Unit = {}
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(text, style = MaterialTheme.typography.titleLarge, color = Color.White)
        badge?.let {
            Spacer(Modifier.width(8.dp))
            Box(
                Modifier
                    .clip(CircleShape)
                    .background(Pulse.Magenta)
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(it, style = MaterialTheme.typography.bodySmall, color = Color.White)
            }
        }
        Spacer(Modifier.weight(1f))
        action?.let {
            TextButton(onAction) {
                Text(it, color = Pulse.Cyan)
            }
        }
    }
}

fun formatDate(ms: Long): String =
    SimpleDateFormat("MM-dd", Locale.getDefault()).format(Date(ms))

fun formatTime(ms: Long): String {
    val s = (ms / 1000).toInt()
    return "%d:%02d".format(s / 60, s % 60)
}
