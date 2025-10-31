package uc.ucworks.videosnap

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

/**
 * A composable that displays a seek bar.
 *
 * @param progress The current progress of the seek bar.
 * @param onSeek A callback that is invoked when the user seeks the seek bar.
 */
@Composable
fun SeekBar(progress: Float, onSeek: (Float) -> Unit) {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .pointerInput(Unit) {
                detectDragGestures {
                    change, dragAmount ->
                    val newProgress = (change.position.x / size.width).coerceIn(0f, 1f)
                    onSeek(newProgress)
                }
            }
    ) {
        // Draw the timeline bar
        drawLine(
            color = Color.White,
            start = Offset(0f, size.height / 2),
            end = Offset(size.width, size.height / 2),
            strokeWidth = 4f
        )

        // Draw the thumb
        drawCircle(
            color = Color.Red,
            radius = 20f,
            center = Offset(progress * size.width, size.height / 2)
        )
    }
}
