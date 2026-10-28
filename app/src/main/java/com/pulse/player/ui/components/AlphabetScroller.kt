package com.pulse.player.ui.components

import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pulse.player.ui.theme.Pulse
import kotlinx.coroutines.launch

private val alphabet = listOf("#") + ('A'..'Z').map { it.toString() }

@Composable
fun AlphabetScroller(
    titles: List<String>,
    listState: LazyListState,
    modifier: Modifier = Modifier
) {
    if (titles.size < 50) return
    val scope = rememberCoroutineScope()

    fun scroll(char: String) {
        val index = if (char == "#") {
            0
        } else {
            titles.indexOfFirst { it.startsWith(char, ignoreCase = true) }
        }
        if (index >= 0) {
            scope.launch {
                listState.scrollToItem(index + 1)
            }
        }
    }

    Column(
        modifier = modifier
            .padding(end = 4.dp)
            .pointerInput(titles) {
                detectVerticalDragGestures { change, _ ->
                    val total = alphabet.size
                    val fraction = (change.position.y / size.height).coerceIn(0f, 1f)
                    val idx = (fraction * (total - 1)).toInt().coerceIn(0, total - 1)
                    scroll(alphabet[idx])
                }
            },
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        alphabet.forEach { letter ->
            Text(
                text = letter,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                fontWeight = FontWeight.Bold,
                color = Pulse.TextMid,
                modifier = Modifier.padding(vertical = 1.dp)
            )
        }
    }
}
