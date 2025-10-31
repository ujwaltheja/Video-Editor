package uc.ucworks.videosnap.ui.timeline

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uc.ucworks.videosnap.TimelineClip
import uc.ucworks.videosnap.TimelineTrack
import uc.ucworks.videosnap.TrackType
import kotlin.math.roundToInt

/**
 * Multi-track timeline view with drag-and-drop support.
 */
@Composable
fun TimelineView(
    tracks: List<TimelineTrack>,
    currentPosition: Long,
    zoomLevel: Float,
    onSeek: (Long) -> Unit,
    onClipMoved: (trackId: String, clipId: String, newStartTime: Long) -> Unit,
    onClipSelected: (trackId: String, clipId: String) -> Unit,
    onClipResized: (trackId: String, clipId: String, newStartTime: Long, newEndTime: Long) -> Unit,
    onTrackAdded: (TrackType) -> Unit,
    selectedClipId: String? = null,
    modifier: Modifier = Modifier
) {
    val horizontalScroll = rememberScrollState()
    val verticalScroll = rememberScrollState()

    Column(modifier = modifier) {
        // Timeline ruler
        TimelineRuler(
            duration = tracks.maxOfOrNull { track ->
                track.clips.maxOfOrNull { it.endTime } ?: 0L
            } ?: 10000L,
            currentPosition = currentPosition,
            zoomLevel = zoomLevel,
            onSeek = onSeek,
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .horizontalScroll(horizontalScroll)
        )

        Divider()

        // Tracks
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(verticalScroll)
                .horizontalScroll(horizontalScroll)
        ) {
            tracks.sortedBy { it.order }.forEach { track ->
                TimelineTrackView(
                    track = track,
                    currentPosition = currentPosition,
                    zoomLevel = zoomLevel,
                    onClipMoved = { clipId, newStartTime ->
                        onClipMoved(track.id, clipId, newStartTime)
                    },
                    onClipSelected = { clipId ->
                        onClipSelected(track.id, clipId)
                    },
                    onClipResized = { clipId, newStart, newEnd ->
                        onClipResized(track.id, clipId, newStart, newEnd)
                    },
                    selectedClipId = selectedClipId,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(track.height.dp)
                )
                Divider()
            }

            // Add track button
            AddTrackButton(onTrackAdded = onTrackAdded)
        }
    }
}

/**
 * Timeline ruler showing time markers.
 */
@Composable
fun TimelineRuler(
    duration: Long,
    currentPosition: Long,
    zoomLevel: Float,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val pixelsPerMs = zoomLevel * 0.1f
    val widthPx = (duration * pixelsPerMs).dp

    Box(
        modifier = modifier
            .width(widthPx)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val timeMs = (offset.x / pixelsPerMs).toLong()
                    onSeek(timeMs.coerceIn(0, duration))
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val height = size.height
            val markers = (duration / 1000).toInt() // One marker per second

            for (i in 0..markers) {
                val x = i * 1000 * pixelsPerMs
                if (x > size.width) break

                // Draw marker line
                drawLine(
                    color = Color.Gray,
                    start = Offset(x, height * 0.6f),
                    end = Offset(x, height),
                    strokeWidth = 2f
                )
            }

            // Draw playhead
            val playheadX = currentPosition * pixelsPerMs
            drawLine(
                color = Color.Red,
                start = Offset(playheadX, 0f),
                end = Offset(playheadX, height),
                strokeWidth = 3f
            )
        }

        // Time labels
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val markers = (duration / 1000).toInt()
            for (i in 0..markers step 5) {
                Text(
                    text = formatTime(i * 1000L),
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.offset(x = (i * 1000 * pixelsPerMs).dp)
                )
            }
        }
    }
}

/**
 * Single track view.
 */
@Composable
fun TimelineTrackView(
    track: TimelineTrack,
    currentPosition: Long,
    zoomLevel: Float,
    onClipMoved: (clipId: String, newStartTime: Long) -> Unit,
    onClipSelected: (clipId: String) -> Unit,
    onClipResized: (clipId: String, newStart: Long, newEnd: Long) -> Unit,
    selectedClipId: String?,
    modifier: Modifier = Modifier
) {
    val pixelsPerMs = zoomLevel * 0.1f

    Box(
        modifier = modifier
            .background(getTrackColor(track.type).copy(alpha = 0.1f))
    ) {
        // Track label
        Text(
            text = track.name,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(8.dp)
                .width(100.dp),
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        // Clips
        Box(modifier = Modifier.padding(start = 110.dp)) {
            track.clips.forEach { clip ->
                TimelineClipView(
                    clip = clip,
                    isSelected = clip.id == selectedClipId,
                    pixelsPerMs = pixelsPerMs,
                    trackColor = getTrackColor(track.type),
                    onMoved = { newStartTime ->
                        onClipMoved(clip.id, newStartTime)
                    },
                    onSelected = {
                        onClipSelected(clip.id)
                    },
                    onResized = { newStart, newEnd ->
                        onClipResized(clip.id, newStart, newEnd)
                    },
                    modifier = Modifier
                        .offset(x = (clip.startTime * pixelsPerMs).dp)
                        .width((clip.duration * pixelsPerMs).dp)
                        .fillMaxHeight(0.8f)
                )
            }
        }
    }
}

/**
 * Individual clip view with drag-and-drop support.
 */
@Composable
fun TimelineClipView(
    clip: TimelineClip,
    isSelected: Boolean,
    pixelsPerMs: Float,
    trackColor: Color,
    onMoved: (newStartTime: Long) -> Unit,
    onSelected: () -> Unit,
    onResized: (newStart: Long, newEnd: Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var offsetX by remember { mutableStateOf(0f) }

    Box(
        modifier = modifier
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(
                if (isSelected) trackColor.copy(alpha = 0.9f)
                else trackColor.copy(alpha = 0.7f)
            )
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { onSelected() },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        offsetX += dragAmount.x
                    },
                    onDragEnd = {
                        val newStartTime = clip.startTime + (offsetX / pixelsPerMs).toLong()
                        onMoved(newStartTime.coerceAtLeast(0))
                        offsetX = 0f
                    }
                )
            }
    ) {
        // Clip name and waveform preview
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(4.dp)
        ) {
            Text(
                text = clip.mediaPath.substringAfterLast("/"),
                fontSize = 10.sp,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Simplified waveform visualization
            Canvas(modifier = Modifier.fillMaxSize()) {
                val barCount = 20
                val barWidth = size.width / barCount
                for (i in 0 until barCount) {
                    val height = (0.3f + Math.random().toFloat() * 0.7f) * size.height
                    drawLine(
                        color = Color.White.copy(alpha = 0.3f),
                        start = Offset(i * barWidth + barWidth / 2, size.height - height),
                        end = Offset(i * barWidth + barWidth / 2, size.height),
                        strokeWidth = barWidth * 0.5f
                    )
                }
            }
        }

        // Selection border
        if (isSelected) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.Transparent)
                    .background(Color.Yellow.copy(alpha = 0.3f))
            )
        }
    }
}

/**
 * Add track button.
 */
@Composable
fun AddTrackButton(
    onTrackAdded: (TrackType) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier.padding(8.dp)) {
        Button(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("+ Add Track")
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            TrackType.values().forEach { type ->
                DropdownMenuItem(
                    text = { Text("Add ${type.name} Track") },
                    onClick = {
                        onTrackAdded(type)
                        expanded = false
                    }
                )
            }
        }
    }
}

/**
 * Get color for track type.
 */
private fun getTrackColor(type: TrackType): Color {
    return when (type) {
        TrackType.VIDEO -> Color(0xFF4CAF50)
        TrackType.AUDIO -> Color(0xFF2196F3)
        TrackType.TEXT -> Color(0xFFFF9800)
        TrackType.OVERLAY -> Color(0xFF9C27B0)
    }
}

/**
 * Format time in mm:ss format.
 */
private fun formatTime(ms: Long): String {
    val seconds = (ms / 1000).toInt()
    val minutes = seconds / 60
    val secs = seconds % 60
    return String.format("%02d:%02d", minutes, secs)
}
