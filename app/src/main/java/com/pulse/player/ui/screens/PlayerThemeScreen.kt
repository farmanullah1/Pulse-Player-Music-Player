package com.pulse.player.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pulse.player.ui.theme.Pulse
import com.pulse.player.ui.themes.PlayerThemes
import com.pulse.player.ui.themes.ThemePreviewCard
import com.pulse.player.ui.viewmodel.MainViewModel
import kotlin.math.absoluteValue

@Composable
fun PlayerThemeScreen(vm: MainViewModel) {
    val used by vm.playerThemeId.collectAsStateWithLifecycle()
    val initialPage = PlayerThemes.indexOfFirst { it.id == used }.coerceAtLeast(0)
    val pager = rememberPagerState(initialPage = initialPage) { PlayerThemes.size }
    val cur = PlayerThemes[pager.currentPage]
    val bgTop by animateColorAsState(cur.colors.first(), tween(500), label = "bg")

    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(bgTop.copy(alpha = 0.85f), Color.Black)))
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
        ) {
            Row(
                Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = vm::back) {
                    Icon(Icons.Rounded.ArrowBack, "Back", tint = Color.White)
                }
                Spacer(Modifier.width(8.dp))
                Text("Player theme", style = MaterialTheme.typography.titleLarge, color = Color.White)
            }

            HorizontalPager(
                state = pager,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 56.dp),
                pageSpacing = 16.dp
            ) { page ->
                val offset = (pager.currentPage - page) + pager.currentPageOffsetFraction
                val scale = lerp(0.85f, 1.05f, 1f - offset.absoluteValue.coerceIn(0f, 1f))
                val alpha = lerp(0.6f, 1f, 1f - offset.absoluteValue.coerceIn(0f, 1f))
                ThemePreviewCard(
                    theme = PlayerThemes[page],
                    modifier = Modifier.graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        this.alpha = alpha
                    }
                )
            }

            val isUsing = cur.id == used
            Button(
                onClick = { vm.applyPlayerTheme(cur.id) },
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(24.dp)
                    .height(52.dp)
                    .widthIn(min = 220.dp),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isUsing) Color.White.copy(0.18f) else Pulse.Orange,
                    contentColor = Color.White
                )
            ) {
                if (isUsing) {
                    Icon(Icons.Rounded.Check, null)
                    Spacer(Modifier.width(8.dp))
                    Text("USING", fontWeight = FontWeight.Bold)
                } else {
                    if (cur.special) {
                        Icon(Icons.Rounded.WorkspacePremium, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                    }
                    Text("APPLY", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
