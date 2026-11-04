package com.pulse.player.ui.buttons

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pulse.player.ui.theme.LocalPulseTokens
import com.pulse.player.ui.theme.LocalReducedMotion

/**
 * The shared playback action state consumed by every preset.
 * All 12 button layouts render the SAME set of actions — only their visual arrangement differs.
 */
data class PlaybackActions(
    val isPlaying: Boolean,
    val isFavorite: Boolean,
    val isShuffled: Boolean,
    val repeatMode: Int,           // 0=Off 1=All 2=One
    val onPlayPause: () -> Unit,
    val onPrevious: () -> Unit,
    val onNext: () -> Unit,
    val onShuffle: () -> Unit,
    val onRepeat: () -> Unit,
    val onFavorite: () -> Unit,
    val onMore: () -> Unit,        // Opens overflow sheet
    // Optional secondary actions; callers may pass no-ops
    val onSleepTimer: () -> Unit = {},
    val onQueue: () -> Unit = {},
    val onLyrics: () -> Unit = {},
    val onEqualizer: () -> Unit = {},
    val onAddToPlaylist: () -> Unit = {}
)

/**
 * Entry point: renders the correct [ButtonLayout] composable and animates transitions.
 * Switching presets never restarts playback — it only changes the composable structure.
 */
@Composable
fun ButtonArrangement(
    preset: ButtonPreset,
    actions: PlaybackActions,
    modifier: Modifier = Modifier
) {
    val reduced = LocalReducedMotion.current
    AnimatedContent(
        targetState = preset.layout,
        transitionSpec = {
            if (reduced) fadeIn() togetherWith fadeOut()
            else fadeIn() togetherWith fadeOut()
        },
        modifier = modifier,
        label = "ButtonArrangement"
    ) { layout ->
        when (layout) {
            ButtonLayout.CENTERED       -> CenteredArrangement(actions)
            ButtonLayout.SPLIT          -> SplitArrangement(actions)
            ButtonLayout.MINIMAL        -> MinimalArrangement(actions)
            ButtonLayout.WAVEFORM_BELOW -> WaveformBelowArrangement(actions)
            ButtonLayout.QUEUE_INLINE   -> QueueInlineArrangement(actions)
            ButtonLayout.EXPANDED       -> ExpandedArrangement(actions)
            ButtonLayout.COMPACT        -> CompactArrangement(actions)
            ButtonLayout.FLOATING       -> FloatingArrangement(actions)
            ButtonLayout.SYMMETRIC      -> SymmetricArrangement(actions)
            ButtonLayout.ASYMMETRIC     -> AsymmetricArrangement(actions)
            ButtonLayout.TWO_ROW        -> TwoRowArrangement(actions)
            ButtonLayout.RADIAL_INSPIRED -> RadialInspiredArrangement(actions)
            ButtonLayout.THUMB_ZONE     -> ThumbZoneArrangement(actions)
            ButtonLayout.SIDE_RAIL      -> SideRailArrangement(actions)
            ButtonLayout.CHIPS          -> ChipsArrangement(actions)
        }
    }
}

// ─── Shared sub-components ────────────────────────────────────────────────────

@Composable
private fun PlayPauseButton(
    isPlaying: Boolean,
    onClick: () -> Unit,
    size: Dp = 72.dp,
    modifier: Modifier = Modifier
) {
    val tokens = LocalPulseTokens.current
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(tokens.accent)
            .clickable(onClick = onClick)
            .semantics { contentDescription = if (isPlaying) "Pause" else "Play" },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
            contentDescription = null,
            tint = Color.Black,
            modifier = Modifier.size(size * 0.55f)
        )
    }
}

@Composable
private fun TransportButton(icon: androidx.compose.ui.graphics.vector.ImageVector, cd: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    IconButton(onClick = onClick, modifier = modifier.size(48.dp).semantics { contentDescription = cd }) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(28.dp))
    }
}

@Composable
private fun SecondaryButton(icon: androidx.compose.ui.graphics.vector.ImageVector, cd: String, onClick: () -> Unit, active: Boolean = false, modifier: Modifier = Modifier) {
    val tokens = LocalPulseTokens.current
    IconButton(onClick = onClick, modifier = modifier.size(40.dp).semantics { contentDescription = cd }) {
        Icon(icon, contentDescription = null, tint = if (active) tokens.accent else tokens.textSecondary, modifier = Modifier.size(22.dp))
    }
}

// ─── 12 preset implementations ────────────────────────────────────────────────

/** 1. Centered: shuffle | prev | PLAY | next | repeat, favorites + more row below */
@Composable
private fun CenteredArrangement(a: PlaybackActions) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SecondaryButton(Icons.Rounded.Shuffle, "Shuffle", a.onShuffle, active = a.isShuffled)
            TransportButton(Icons.Rounded.SkipPrevious, "Previous", a.onPrevious)
            PlayPauseButton(a.isPlaying, a.onPlayPause)
            TransportButton(Icons.Rounded.SkipNext, "Next", a.onNext)
            RepeatButton(a.repeatMode, a.onRepeat)
        }
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SecondaryButton(if (a.isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder, "Favourite", a.onFavorite, active = a.isFavorite)
            SecondaryButton(Icons.Rounded.MoreHoriz, "More options", a.onMore)
        }
    }
}

/** 2. Split: [shuffle | prev] PLAY [next | repeat] */
@Composable
private fun SplitArrangement(a: PlaybackActions) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SecondaryButton(Icons.Rounded.Shuffle, "Shuffle", a.onShuffle, active = a.isShuffled)
                Spacer(Modifier.width(4.dp))
                TransportButton(Icons.Rounded.SkipPrevious, "Previous", a.onPrevious)
            }
            PlayPauseButton(a.isPlaying, a.onPlayPause, size = 64.dp)
            Row(verticalAlignment = Alignment.CenterVertically) {
                TransportButton(Icons.Rounded.SkipNext, "Next", a.onNext)
                Spacer(Modifier.width(4.dp))
                RepeatButton(a.repeatMode, a.onRepeat)
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            SecondaryButton(if (a.isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder, "Favourite", a.onFavorite, active = a.isFavorite)
            SecondaryButton(Icons.Rounded.MoreHoriz, "More options", a.onMore)
        }
    }
}

/** 3. Minimal: prev | PLAY | next  + overflow button for all secondary actions */
@Composable
private fun MinimalArrangement(a: PlaybackActions) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        TransportButton(Icons.Rounded.SkipPrevious, "Previous", a.onPrevious)
        PlayPauseButton(a.isPlaying, a.onPlayPause)
        TransportButton(Icons.Rounded.SkipNext, "Next", a.onNext)
        SecondaryButton(Icons.Rounded.MoreHoriz, "More options", a.onMore)
    }
}

/** 4. Waveform Below: transport row first, then secondary in next row */
@Composable
private fun WaveformBelowArrangement(a: PlaybackActions) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TransportButton(Icons.Rounded.SkipPrevious, "Previous", a.onPrevious)
            PlayPauseButton(a.isPlaying, a.onPlayPause, size = 64.dp)
            TransportButton(Icons.Rounded.SkipNext, "Next", a.onNext)
        }
        Spacer(Modifier.height(4.dp))
        // Secondary row beneath — waveform seekbar is rendered by the layout host above this component
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            SecondaryButton(Icons.Rounded.Shuffle, "Shuffle", a.onShuffle, active = a.isShuffled)
            SecondaryButton(if (a.isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder, "Favourite", a.onFavorite, active = a.isFavorite)
            RepeatButton(a.repeatMode, a.onRepeat, size = 22.dp)
            SecondaryButton(Icons.Rounded.MoreHoriz, "More options", a.onMore)
        }
    }
}

/** 5. Queue Inline: compact; PLAY is prominent, queue count shown inline */
@Composable
private fun QueueInlineArrangement(a: PlaybackActions) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SecondaryButton(Icons.Rounded.Shuffle, "Shuffle", a.onShuffle, active = a.isShuffled)
            TransportButton(Icons.Rounded.SkipPrevious, "Previous", a.onPrevious)
            PlayPauseButton(a.isPlaying, a.onPlayPause, size = 64.dp)
            TransportButton(Icons.Rounded.SkipNext, "Next", a.onNext)
            RepeatButton(a.repeatMode, a.onRepeat)
        }
    }
}

/** 6. Expanded: large controls + all secondary actions visible */
@Composable
private fun ExpandedArrangement(a: PlaybackActions) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SecondaryButton(Icons.Rounded.Shuffle, "Shuffle", a.onShuffle, active = a.isShuffled)
            TransportButton(Icons.Rounded.SkipPrevious, "Previous", a.onPrevious, modifier = Modifier.size(52.dp))
            PlayPauseButton(a.isPlaying, a.onPlayPause, size = 84.dp)
            TransportButton(Icons.Rounded.SkipNext, "Next", a.onNext, modifier = Modifier.size(52.dp))
            RepeatButton(a.repeatMode, a.onRepeat)
        }
        Spacer(Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            SecondaryButton(if (a.isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder, "Favourite", a.onFavorite, active = a.isFavorite)
            SecondaryButton(Icons.Rounded.MoreHoriz, "Queue", a.onQueue)
            SecondaryButton(Icons.Rounded.MoreHoriz, "More options", a.onMore)
        }
    }
}

/** 7. Compact: small controls, single tight row */
@Composable
private fun CompactArrangement(a: PlaybackActions) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        SecondaryButton(Icons.Rounded.Shuffle, "Shuffle", a.onShuffle, active = a.isShuffled, modifier = Modifier.size(36.dp))
        TransportButton(Icons.Rounded.SkipPrevious, "Previous", a.onPrevious, modifier = Modifier.size(40.dp))
        PlayPauseButton(a.isPlaying, a.onPlayPause, size = 56.dp)
        TransportButton(Icons.Rounded.SkipNext, "Next", a.onNext, modifier = Modifier.size(40.dp))
        RepeatButton(a.repeatMode, a.onRepeat, size = 20.dp)
        SecondaryButton(if (a.isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder, "Favourite", a.onFavorite, active = a.isFavorite, modifier = Modifier.size(36.dp))
    }
}

/** 8. Floating: elevated/shadowed central PLAY */
@Composable
private fun FloatingArrangement(a: PlaybackActions) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SecondaryButton(Icons.Rounded.Shuffle, "Shuffle", a.onShuffle, active = a.isShuffled)
            TransportButton(Icons.Rounded.SkipPrevious, "Previous", a.onPrevious)
            Box(modifier = Modifier.shadow(8.dp, CircleShape)) {
                PlayPauseButton(a.isPlaying, a.onPlayPause, size = 80.dp)
            }
            TransportButton(Icons.Rounded.SkipNext, "Next", a.onNext)
            RepeatButton(a.repeatMode, a.onRepeat)
        }
        Spacer(Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            SecondaryButton(if (a.isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder, "Favourite", a.onFavorite, active = a.isFavorite)
            SecondaryButton(Icons.Rounded.MoreHoriz, "More options", a.onMore)
        }
    }
}

/** 9. Symmetric: exactly mirrored — [shuffle | prev] PLAY [next | repeat] with fav+more centered */
@Composable
private fun SymmetricArrangement(a: PlaybackActions) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SecondaryButton(Icons.Rounded.Shuffle, "Shuffle", a.onShuffle, active = a.isShuffled)
            TransportButton(Icons.Rounded.SkipPrevious, "Previous", a.onPrevious)
            PlayPauseButton(a.isPlaying, a.onPlayPause)
            TransportButton(Icons.Rounded.SkipNext, "Next", a.onNext)
            RepeatButton(a.repeatMode, a.onRepeat)
        }
        Spacer(Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            SecondaryButton(if (a.isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder, "Favourite", a.onFavorite, active = a.isFavorite)
            Spacer(Modifier.width(24.dp))
            SecondaryButton(Icons.Rounded.MoreHoriz, "More options", a.onMore)
        }
    }
}

/** 10. Asymmetric: large PLAY on left, controls on right */
@Composable
private fun AsymmetricArrangement(a: PlaybackActions) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        PlayPauseButton(a.isPlaying, a.onPlayPause, size = 80.dp)
        Spacer(Modifier.width(16.dp))
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TransportButton(Icons.Rounded.SkipPrevious, "Previous", a.onPrevious)
                TransportButton(Icons.Rounded.SkipNext, "Next", a.onNext)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                SecondaryButton(Icons.Rounded.Shuffle, "Shuffle", a.onShuffle, active = a.isShuffled)
                RepeatButton(a.repeatMode, a.onRepeat)
                SecondaryButton(if (a.isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder, "Favourite", a.onFavorite, active = a.isFavorite)
                SecondaryButton(Icons.Rounded.MoreHoriz, "More options", a.onMore)
            }
        }
    }
}

/** 11. Two Row: row 1 = transport, row 2 = secondary actions */
@Composable
private fun TwoRowArrangement(a: PlaybackActions) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TransportButton(Icons.Rounded.SkipPrevious, "Previous", a.onPrevious)
            PlayPauseButton(a.isPlaying, a.onPlayPause, size = 72.dp)
            TransportButton(Icons.Rounded.SkipNext, "Next", a.onNext)
        }
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SecondaryButton(Icons.Rounded.Shuffle, "Shuffle", a.onShuffle, active = a.isShuffled)
            SecondaryButton(if (a.isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder, "Favourite", a.onFavorite, active = a.isFavorite)
            RepeatButton(a.repeatMode, a.onRepeat)
            SecondaryButton(Icons.Rounded.MoreHoriz, "More options", a.onMore)
        }
    }
}

/** 12. Radial Inspired: circular feel with accessible left/right transport */
@Composable
private fun RadialInspiredArrangement(a: PlaybackActions) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        // Top arc: shuffle and repeat flanking the title area (visual only)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            SecondaryButton(Icons.Rounded.Shuffle, "Shuffle", a.onShuffle, active = a.isShuffled)
            RepeatButton(a.repeatMode, a.onRepeat)
        }
        // Middle: prev | large PLAY | next
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TransportButton(Icons.Rounded.SkipPrevious, "Previous", a.onPrevious, modifier = Modifier.size(52.dp))
            PlayPauseButton(a.isPlaying, a.onPlayPause, size = 88.dp)
            TransportButton(Icons.Rounded.SkipNext, "Next", a.onNext, modifier = Modifier.size(52.dp))
        }
        // Bottom arc: favorite and more
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            SecondaryButton(if (a.isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder, "Favourite", a.onFavorite, active = a.isFavorite)
            SecondaryButton(Icons.Rounded.MoreHoriz, "More options", a.onMore)
        }
    }
}

/** 13. Thumb Zone: everything in the bottom area for easy one-handed access */
@Composable
private fun ThumbZoneArrangement(a: PlaybackActions) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SecondaryButton(Icons.Rounded.Shuffle, "Shuffle", a.onShuffle, active = a.isShuffled)
            RepeatButton(a.repeatMode, a.onRepeat)
            SecondaryButton(Icons.Rounded.MoreHoriz, "More options", a.onMore)
        }
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SecondaryButton(
                if (a.isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                "Favourite",
                a.onFavorite,
                active = a.isFavorite
            )
            TransportButton(Icons.Rounded.SkipPrevious, "Previous", a.onPrevious, modifier = Modifier.size(52.dp))
            PlayPauseButton(a.isPlaying, a.onPlayPause, size = 80.dp)
            TransportButton(Icons.Rounded.SkipNext, "Next", a.onNext, modifier = Modifier.size(52.dp))
            IconButton(onClick = a.onQueue, modifier = Modifier.size(40.dp)) {
                Icon(Icons.Rounded.MoreHoriz, "Queue", tint = LocalPulseTokens.current.textSecondary)
            }
        }
    }
}

/** 14. Side Rail: vertical/edge rail styling for wide screens & landscape */
@Composable
private fun SideRailArrangement(a: PlaybackActions) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SecondaryButton(Icons.Rounded.Shuffle, "Shuffle", a.onShuffle, active = a.isShuffled)
            SecondaryButton(
                if (a.isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                "Favourite",
                a.onFavorite,
                active = a.isFavorite
            )
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TransportButton(Icons.Rounded.SkipPrevious, "Previous", a.onPrevious, modifier = Modifier.size(52.dp))
            PlayPauseButton(a.isPlaying, a.onPlayPause, size = 76.dp)
            TransportButton(Icons.Rounded.SkipNext, "Next", a.onNext, modifier = Modifier.size(52.dp))
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            RepeatButton(a.repeatMode, a.onRepeat)
            SecondaryButton(Icons.Rounded.MoreHoriz, "More options", a.onMore)
        }
    }
}

/** 15. Chips: interactive labelled chips plus large central play button */
@Composable
private fun ChipsArrangement(a: PlaybackActions) {
    val tokens = LocalPulseTokens.current
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                onClick = a.onShuffle,
                shape = CircleShape,
                color = if (a.isShuffled) tokens.accent.copy(alpha = 0.2f) else tokens.bgCard,
                contentColor = if (a.isShuffled) tokens.accent else tokens.textPrimary
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.Shuffle, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Shuffle", style = MaterialTheme.typography.labelSmall)
                }
            }
            Surface(
                onClick = a.onFavorite,
                shape = CircleShape,
                color = if (a.isFavorite) tokens.accent.copy(alpha = 0.2f) else tokens.bgCard,
                contentColor = if (a.isFavorite) tokens.accent else tokens.textPrimary
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(if (a.isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Like", style = MaterialTheme.typography.labelSmall)
                }
            }
            Surface(
                onClick = a.onRepeat,
                shape = CircleShape,
                color = if (a.repeatMode != 0) tokens.accent.copy(alpha = 0.2f) else tokens.bgCard,
                contentColor = if (a.repeatMode != 0) tokens.accent else tokens.textPrimary
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        if (a.repeatMode == 2) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
                        null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("Repeat", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TransportButton(Icons.Rounded.SkipPrevious, "Previous", a.onPrevious, modifier = Modifier.size(52.dp))
            PlayPauseButton(a.isPlaying, a.onPlayPause, size = 80.dp)
            TransportButton(Icons.Rounded.SkipNext, "Next", a.onNext, modifier = Modifier.size(52.dp))
        }
    }
}

// ─── Shared repeat button (cycles Off → All → One) ───────────────────────────

@Composable
private fun RepeatButton(mode: Int, onClick: () -> Unit, size: Dp = 22.dp, modifier: Modifier = Modifier) {
    val icon = when (mode) {
        2    -> Icons.Rounded.RepeatOne
        else -> Icons.Rounded.Repeat
    }
    val cd = when (mode) {
        0    -> "Repeat: Off"
        1    -> "Repeat: All"
        else -> "Repeat: One"
    }
    val tokens = LocalPulseTokens.current
    IconButton(onClick = onClick, modifier = modifier.size(40.dp).semantics { contentDescription = cd }) {
        Icon(icon, contentDescription = null, tint = if (mode != 0) tokens.accent else tokens.textSecondary, modifier = Modifier.size(size))
    }
}
