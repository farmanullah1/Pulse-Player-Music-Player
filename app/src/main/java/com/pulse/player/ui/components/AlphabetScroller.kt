package com.pulse.player.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pulse.player.ui.theme.Pulse
import kotlinx.coroutines.launch

fun firstLetter(title: String): Char =
    title.trim().firstOrNull()?.uppercaseChar()?.let { if (it in 'A'..'Z') it else '#' } ?: '#'

@Composable
fun AlphaScroller(
    firstIndex: Map<Char, Int>,
    listState: LazyListState,
    modifier: Modifier = Modifier
) {
    val letters = remember(firstIndex) {
        ('A'..'Z').filter { it in firstIndex } + if ('#' in firstIndex) listOf('#') else emptyList()
    }
    if (letters.isEmpty()) return

    val scope = rememberCoroutineScope()
    var active by remember { mutableStateOf<Char?>(null) }
    var h by remember { mutableIntStateOf(1) }

    fun jump(y: Float) {
        if (letters.isEmpty()) return
        val i = ((y / h) * letters.size).toInt().coerceIn(0, letters.lastIndex)
        val c = letters[i]
        if (c != active) {
            active = c
            scope.launch {
                firstIndex[c]?.let { targetIdx ->
                    listState.scrollToItem(targetIdx)
                }
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxHeight()
            .width(28.dp)
            .onSizeChanged { h = it.height.coerceAtLeast(1) }
            .pointerInput(letters) {
                detectVerticalDragGestures(
                    onDragStart = { jump(it.y) },
                    onDragEnd = { active = null },
                    onDragCancel = { active = null }
                ) { change, _ ->
                    jump(change.position.y)
                }
            }
            .pointerInput(letters) {
                detectTapGestures {
                    jump(it.y)
                    active = null
                }
            }
    ) {
        Column(
            Modifier.fillMaxHeight(),
            verticalArrangement = Arrangement.SpaceEvenly,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            letters.forEach { c ->
                Text(
                    text = c.toString(),
                    fontSize = 10.sp,
                    color = if (c == active) Pulse.Cyan else Pulse.TextMid,
                    fontWeight = if (c == active) FontWeight.Bold else FontWeight.Normal
                )
            }
        }

        active?.let { letter ->
            Box(
                Modifier
                    .align(Alignment.CenterStart)
                    .offset(x = (-64).dp)
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(Pulse.GradPrimary)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = letter.toString(),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
fun AlphabetScroller(
    titles: List<String>,
    listState: LazyListState,
    modifier: Modifier = Modifier
) {
    val map = remember(titles) {
        titles.withIndex()
            .groupBy { firstLetter(it.value) }
            .mapValues { it.value.first().index }
    }
    AlphaScroller(firstIndex = map, listState = listState, modifier = modifier)
}
