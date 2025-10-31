package uc.ucworks.videosnap

import androidx.compose.ui.graphics.Color

actual fun Color.toArgb(): Int {
    return this.value.toLong().toInt()
}
