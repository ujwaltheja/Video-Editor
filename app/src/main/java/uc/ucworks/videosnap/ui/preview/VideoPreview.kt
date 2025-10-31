package uc.ucworks.videosnap.ui.preview

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

/**
 * Video preview component with playback controls.
 * Uses ExoPlayer for high-performance video playback with GPU acceleration.
 */
@Composable
fun VideoPreview(
    videoPath: String?,
    isPlaying: Boolean,
    currentPosition: Long,
    onPlayPauseToggle: () -> Unit,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            playWhenReady = isPlaying
        }
    }

    // Update video source when path changes
    LaunchedEffect(videoPath) {
        videoPath?.let {
            val mediaItem = MediaItem.fromUri(it)
            exoPlayer.setMediaItem(mediaItem)
            exoPlayer.prepare()
        }
    }

    // Update playback state
    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            exoPlayer.play()
        } else {
            exoPlayer.pause()
        }
    }

    // Seek to position
    LaunchedEffect(currentPosition) {
        if (!isPlaying) {
            exoPlayer.seekTo(currentPosition)
        }
    }

    // Listen to playback position
    LaunchedEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                // Handle playback state changes
            }

            override fun onIsPlayingChanged(isPlayingNow: Boolean) {
                // Sync playing state
            }
        }
        exoPlayer.addListener(listener)
    }

    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Video player view
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            if (videoPath != null) {
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            player = exoPlayer
                            layoutParams = FrameLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            useController = false // Use custom controls
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                // Placeholder when no video
                Text(
                    text = "No video loaded",
                    color = Color.White,
                    style = MaterialTheme.typography.headlineSmall
                )
            }
        }

        // Playback controls
        PlaybackControls(
            isPlaying = isPlaying,
            currentPosition = currentPosition,
            duration = exoPlayer.duration.coerceAtLeast(0),
            onPlayPauseToggle = onPlayPauseToggle,
            onSeek = { position ->
                exoPlayer.seekTo(position)
                onSeek(position)
            },
            onSkipBackward = {
                val newPosition = (exoPlayer.currentPosition - 5000).coerceAtLeast(0)
                exoPlayer.seekTo(newPosition)
                onSeek(newPosition)
            },
            onSkipForward = {
                val newPosition = (exoPlayer.currentPosition + 5000).coerceAtMost(exoPlayer.duration)
                exoPlayer.seekTo(newPosition)
                onSeek(newPosition)
            },
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
                .padding(16.dp)
        )
    }
}

/**
 * Playback controls component.
 */
@Composable
fun PlaybackControls(
    isPlaying: Boolean,
    currentPosition: Long,
    duration: Long,
    onPlayPauseToggle: () -> Unit,
    onSeek: (Long) -> Unit,
    onSkipBackward: () -> Unit,
    onSkipForward: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        // Seek bar
        Slider(
            value = if (duration > 0) currentPosition.toFloat() / duration else 0f,
            onValueChange = { progress ->
                val newPosition = (progress * duration).toLong()
                onSeek(newPosition)
            },
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Time display
            Text(
                text = "${formatTime(currentPosition)} / ${formatTime(duration)}",
                style = MaterialTheme.typography.bodySmall
            )

            // Control buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onSkipBackward) {
                    Icon(Icons.Default.Replay5, "Skip backward 5s")
                }

                FilledIconButton(
                    onClick = onPlayPauseToggle,
                    modifier = Modifier.size(56.dp)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        modifier = Modifier.size(32.dp)
                    )
                }

                IconButton(onClick = onSkipForward) {
                    Icon(Icons.Default.Forward5, "Skip forward 5s")
                }
            }

            // Spacer for balance
            Spacer(modifier = Modifier.width(100.dp))
        }
    }
}

/**
 * Format time in mm:ss format.
 */
private fun formatTime(ms: Long): String {
    if (ms < 0) return "00:00"
    val seconds = (ms / 1000).toInt()
    val minutes = seconds / 60
    val secs = seconds % 60
    return String.format("%02d:%02d", minutes, secs)
}
