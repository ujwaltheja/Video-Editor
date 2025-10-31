package uc.ucworks.videosnap.ui.preview

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.ui.PlayerView
import androidx.media3.common.Player
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer

@Composable
fun VideoPreview(
    videoPath: String?,
    isPlaying: Boolean,
    currentPosition: Long,
    onPlayPauseToggle: () -> Unit,
    onSeek: (Long) -> Unit,
    modifier: Modifier
) {
    val context = LocalContext.current
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build()
    }

    if (videoPath != null) {
        val mediaItem = MediaItem.fromUri(videoPath)
        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()
    }

    exoPlayer.playWhenReady = isPlaying
    exoPlayer.seekTo(currentPosition)

    AndroidView(
        factory = {
            PlayerView(it).apply {
                player = exoPlayer
            }
        },
        modifier = modifier
    )
}