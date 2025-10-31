package uc.ucworks.videosnap

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.sp

@Composable
fun TextOverlay(
    text: String,
    x: Int,
    y: Int,
    fontSize: Int,
    color: Color
) {
    Box(modifier = Modifier.offset { IntOffset(x, y) }) {
        Text(
            text = text,
            fontSize = fontSize.sp,
            color = color
        )
    }
}
