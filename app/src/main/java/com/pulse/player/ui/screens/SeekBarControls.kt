package com.pulse.player.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.Player
import com.pulse.player.data.settings.SeekStylePreset
import com.pulse.player.playback.PlaybackUiState
import com.pulse.player.playback.PlayerController
import com.pulse.player.ui.components.WaveformSeekBar
import com.pulse.player.ui.components.WavyMusicSlider
import com.pulse.player.ui.components.formatTime
import com.pulse.player.ui.theme.TabularMono
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.abs

enum class SeekStyle { WAVEFORM, WAVY, LINEAR }

private fun SeekStylePreset.toSeekStyle(): SeekStyle = when (this) {
    SeekStylePreset.WAVEFORM -> SeekStyle.WAVEFORM
    SeekStylePreset.WAVY -> SeekStyle.WAVY
    SeekStylePreset.LINEAR -> SeekStyle.LINEAR
}

private fun SeekStyle.toPreset(): SeekStylePreset = when (this) {
    SeekStyle.WAVEFORM -> SeekStylePreset.WAVEFORM
    SeekStyle.WAVY -> SeekStylePreset.WAVY
    SeekStyle.LINEAR -> SeekStylePreset.LINEAR
}

@Composable
fun SeekBar(
    position: StateFlow<Long>,
    duration: Long,
    isPlaying: Boolean = true,
    trackKey: String = "default",
    seekStyle: SeekStylePreset? = null,
    onSelectStyle: ((SeekStylePreset) -> Unit)? = null,
    showLabels: Boolean = true,
    seekBarHeight: Dp = 44.dp,
    abStartFraction: Float? = null,
    abEndFraction: Float? = null,
    onSeek: (Long) -> Unit
) {
    val pos by position.collectAsStateWithLifecycle()
    var drag by remember { mutableStateOf<Float?>(null) }
    var lastDetent by remember { mutableStateOf<String?>(null) }
    var localStyle by remember { mutableStateOf(SeekStyle.WAVEFORM) }
    val haptics = LocalHapticFeedback.current
    val currentStyle = seekStyle?.toSeekStyle() ?: localStyle
    val rawProgress = if (duration > 0) pos / duration.toFloat() else 0f
    val safeProgress = if (rawProgress.isNaN() || rawProgress.isInfinite()) 0f else rawProgress.coerceIn(0f, 1f)
    val value = drag ?: safeProgress

    fun handleDragChange(nextFrac: Float) {
        val clamped = nextFrac.coerceIn(0f, 1f)
        drag = clamped
        val detent = when {
            clamped <= 0.008f -> "start"
            clamped >= 0.992f -> "end"
            abStartFraction != null && abs(clamped - abStartFraction) < 0.015f -> "ab_a"
            abEndFraction != null && abs(clamped - abEndFraction) < 0.015f -> "ab_b"
            else -> null
        }
        if (detent != null && detent != lastDetent) {
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
        lastDetent = detent
    }

    Box(Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth()) {
            when (currentStyle) {
                SeekStyle.WAVEFORM -> {
                    WaveformSeekBar(
                        value = value,
                        onValueChange = ::handleDragChange,
                        onValueChangeFinished = {
                            drag?.let { onSeek((it * duration).toLong()) }
                            drag = null
                            lastDetent = null
                        },
                        isPlaying = isPlaying,
                        seedKey = trackKey,
                        abStartFraction = abStartFraction,
                        abEndFraction = abEndFraction,
                        modifier = Modifier.fillMaxWidth().height(seekBarHeight)
                    )
                }
                SeekStyle.WAVY -> {
                    WavyMusicSlider(
                        value = value,
                        onValueChange = ::handleDragChange,
                        onValueChangeFinished = {
                            drag?.let { onSeek((it * duration).toLong()) }
                            drag = null
                            lastDetent = null
                        },
                        isPlaying = isPlaying,
                        abStartFraction = abStartFraction,
                        abEndFraction = abEndFraction,
                        modifier = Modifier.fillMaxWidth().height(seekBarHeight)
                    )
                }
                SeekStyle.LINEAR -> {
                    Slider(
                        value = value,
                        onValueChange = ::handleDragChange,
                        onValueChangeFinished = {
                            drag?.let { onSeek((it * duration).toLong()) }
                            drag = null
                            lastDetent = null
                        },
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary,
                            inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.fillMaxWidth().height(seekBarHeight)
                    )
                }
            }
            if (showLabels) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        formatTime(((drag ?: value) * duration).toLong()),
                        color = if (drag != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (drag != null) FontWeight.Bold else FontWeight.Normal,
                        style = MaterialTheme.typography.bodySmall.merge(TabularMono)
                    )
                    Spacer(Modifier.weight(1f))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                val next = when (currentStyle) {
                                    SeekStyle.WAVEFORM -> SeekStyle.WAVY
                                    SeekStyle.WAVY -> SeekStyle.LINEAR
                                    SeekStyle.LINEAR -> SeekStyle.WAVEFORM
                                }
                                localStyle = next
                                onSelectStyle?.invoke(next.toPreset())
                            }
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                            .testTag("seek_style_toggle")
                    ) {
                        Text(
                            text = when (currentStyle) {
                                SeekStyle.WAVEFORM -> "Waveform"
                                SeekStyle.WAVY -> "Wavy"
                                SeekStyle.LINEAR -> "Classic"
                            },
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    Text(
                        formatTime(duration),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall.merge(TabularMono)
                    )
                }
            }
        }

        val activeDrag = drag
        AnimatedVisibility(
            visible = activeDrag != null && duration > 0L,
            enter = fadeIn(tween(120)) + scaleIn(initialScale = 0.9f, animationSpec = tween(120)),
            exit = fadeOut(tween(140)) + scaleOut(targetScale = 0.9f, animationSpec = tween(140)),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 0.dp)
        ) {
            val targetMs = ((activeDrag ?: safeProgress) * duration).toLong()
            val deltaMs = targetMs - pos
            val deltaPrefix = if (deltaMs >= 0L) "+" else "-"
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)),
                tonalElevation = 6.dp
            ) {
                Text(
                    text = "${formatTime(targetMs)}  ($deltaPrefix${formatTime(abs(deltaMs))})",
                    style = MaterialTheme.typography.labelMedium.merge(TabularMono),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
fun TransportRow(s: PlaybackUiState, p: PlayerController) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = p::toggleShuffle, modifier = Modifier.size(48.dp)) {
            Icon(
                imageVector = Icons.Rounded.Shuffle,
                contentDescription = "Shuffle ${if (s.shuffle) "on" else "off"}",
                tint = if (s.shuffle) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        IconButton(onClick = p::previous, modifier = Modifier.size(56.dp)) {
            Icon(
                imageVector = Icons.Rounded.SkipPrevious,
                contentDescription = "Previous",
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(36.dp)
            )
        }
        FilledIconButton(
            onClick = p::togglePlay,
            modifier = Modifier.size(76.dp),
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            AnimatedContent(
                targetState = s.isPlaying,
                transitionSpec = { (scaleIn() + fadeIn()) togetherWith (scaleOut() + fadeOut()) },
                label = "main_play"
            ) { playing ->
                Icon(
                    imageVector = if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = if (playing) "Pause" else "Play",
                    modifier = Modifier.size(40.dp)
                )
            }
        }
        IconButton(onClick = p::next, modifier = Modifier.size(56.dp)) {
            Icon(
                imageVector = Icons.Rounded.SkipNext,
                contentDescription = "Next",
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(36.dp)
            )
        }
        IconButton(onClick = p::cycleRepeat, modifier = Modifier.size(48.dp)) {
            Icon(
                imageVector = if (s.repeat == Player.REPEAT_MODE_ONE) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
                contentDescription = "Repeat mode",
                tint = if (s.repeat != Player.REPEAT_MODE_OFF) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
