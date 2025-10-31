package uc.ucworks.videosnap

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Tooltip(
    text: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    var showTooltip by remember { mutableStateOf(false) }
    var itemPosition by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = modifier
            .onGloballyPositioned { coordinates ->
                itemPosition = coordinates.positionInWindow()
            }
            .combinedClickable(
                onLongClick = { showTooltip = true },
                onClick = { showTooltip = false }
            )
    ) {
        content()

        if (showTooltip) {
            Popup(offset = itemPosition.toDpOffset()) {
                Card {
                    Text(text = text, modifier = Modifier.padding(8.dp), color = Color.Black)
                }
            }
        }
    }
}

private fun Offset.toDpOffset(): androidx.compose.ui.unit.IntOffset {
    return androidx.compose.ui.unit.IntOffset(x.toInt(), y.toInt())
}
