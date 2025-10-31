package uc.ucworks.videosnap.domain

import androidx.compose.ui.graphics.Color

data class TextOverlayData(
    val text: String,
    val x: Int,
    val y: Int,
    val fontSize: Int,
    val color: Color
)
