package uc.ucworks.videosnap.util

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned

class DragDropState {
    var draggedItem by mutableStateOf<Any?>(null)
    var dragOffset by mutableStateOf(Offset.Zero)
}

val LocalDragDropState = compositionLocalOf { DragDropState() }

@Composable
fun Modifier.draggable(data: Any): Modifier {
    val state = LocalDragDropState.current
    return pointerInput(Unit) {
        detectDragGesturesAfterLongPress(
            onDragStart = { offset ->
                state.draggedItem = data
            },
            onDrag = { change, dragAmount ->
                state.dragOffset += dragAmount
            },
            onDragEnd = {
                state.draggedItem = null
            }
        )
    }
}

@Composable
fun Modifier.dropTarget(onDrop: (Any) -> Unit): Modifier {
    val state = LocalDragDropState.current
    var bounds by remember { mutableStateOf<Rect?>(null) }

    return this.onGloballyPositioned { coordinates ->
        bounds = coordinates.boundsInWindow()
    }.pointerInput(Unit) {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent()
                if (state.draggedItem != null) {
                    bounds?.let {
                        if (it.contains(event.changes.first().position)) {
                            // The dragged item is over the drop target
                        }
                    }
                }
                if (event.changes.first().pressed.not()) {
                    bounds?.let {
                        if (it.contains(event.changes.first().position)) {
                            state.draggedItem?.let { item ->
                                onDrop(item)
                            }
                        }
                    }
                }
            }
        }
    }
}
