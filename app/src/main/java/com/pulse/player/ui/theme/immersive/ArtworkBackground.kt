package com.pulse.player.ui.theme.immersive

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import com.pulse.player.data.scan.ArtRef
import com.pulse.player.data.scan.ArtworkLoader

@Composable
fun ArtworkBackground(
    art: ArtRef?,
    fallback: List<Color>,
    dim: Float = 0.45f,
    modifier: Modifier = Modifier
) {
    val ctx = LocalContext.current
    val reduced = rememberReducedMotion()
    val bmp by produceState<ImageBitmap?>(null, art?.stableKey, art?.modifiedMs) {
        value = art?.let {
            ArtworkLoader.load(ctx, it, 96)?.let { b ->
                BlurCache.get("${it.stableKey}|${it.modifiedMs}", b).asImageBitmap()
            }
        }
    }

    Box(
        modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(fallback))
    ) {
        Crossfade(
            targetState = bmp,
            animationSpec = tween(if (reduced) 0 else 450),
            label = "blurbg"
        ) { b ->
            if (b != null) {
                Image(
                    bitmap = b,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Black.copy(dim * 0.6f),
                            Color.Black.copy(dim),
                            Color.Black.copy(0.85f)
                        )
                    )
                )
        )
    }
}

