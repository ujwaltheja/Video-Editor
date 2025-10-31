package uc.ucworks.videosnap.ui.timeline

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uc.ucworks.videosnap.domain.TimelineClip
import uc.ucworks.videosnap.domain.TimelineTrack
import uc.ucworks.videosnap.domain.TrackType
import uc.ucworks.videosnap.util.draggable
import uc.ucworks.videosnap.util.dropTarget

@Composable
fun TimelineView(
    tracks: List<TimelineTrack>,
    currentPosition: Long,
    zoomLevel: Float,
    onSeek: (Long) -> Unit,
    onClipMoved: (String, String, Long) -> Unit,
    onClipSelected: (String, String) -> Unit,
    onClipResized: (String, String, Long, Long) -> Unit,
    onTrackAdded: (TrackType) -> Unit,
    selectedClipId: String?,
    modifier: Modifier
) {
    Column(modifier = modifier) {
        TimelineHeader(duration = 30000, currentPosition = currentPosition, zoomLevel = zoomLevel)
        LazyColumn {
            items(tracks) {
                track ->
                TrackView(track = track, onClipSelected = onClipSelected, selectedClipId = selectedClipId, zoomLevel = zoomLevel, onClipMoved = onClipMoved)
            }
        }
    }
}

@Composable
fun TimelineHeader(duration: Long, currentPosition: Long, zoomLevel: Float) {
    Canvas(modifier = Modifier.fillMaxWidth().height(30.dp)) {
        val width = size.width
        val height = size.height
        val pixelsPerSecond = width / (duration / 1000f)

        for (i in 0 until duration.toInt() step 1000) {
            val x = (i / 1000f) * pixelsPerSecond * zoomLevel
            drawLine(
                color = Color.Gray,
                start = Offset(x, 0f),
                end = Offset(x, height)
            )
            // TODO: Draw time labels
        }

        val indicatorX = (currentPosition / 1000f) * pixelsPerSecond * zoomLevel
        drawLine(
            color = Color.Red,
            start = Offset(indicatorX, 0f),
            end = Offset(indicatorX, height),
            strokeWidth = 2f
        )
    }
}

@Composable
fun TrackView(
    track: TimelineTrack, 
    onClipSelected: (String, String) -> Unit, 
    selectedClipId: String?, 
    zoomLevel: Float, 
    onClipMoved: (String, String, Long) -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth().height(50.dp).background(MaterialTheme.colorScheme.surfaceVariant).dropTarget {
        if (it is TimelineClip) {
            onClipMoved(track.id, it.id, 0L) // TODO: Calculate correct start time
        }
    }) {
        Text(text = track.type.name, fontSize = 12.sp, modifier = Modifier.padding(4.dp))
        track.clips.forEach { clip ->
            ClipView(clip = clip, zoomLevel = zoomLevel, onClipSelected = { onClipSelected(track.id, clip.id) })
        }
    }
}

@Composable
fun ClipView(clip: TimelineClip, zoomLevel: Float, onClipSelected: () -> Unit) {
    Box(modifier = Modifier.width((clip.duration * zoomLevel / 1000f).dp).background(MaterialTheme.colorScheme.primary).draggable(data = clip)) {
        Text(text = clip.mediaPath, fontSize = 10.sp)
    }
}
