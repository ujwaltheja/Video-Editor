package uc.ucworks.videosnap.util

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Professional drag and drop system with magnetic snapping for video timeline
 */
class DragDropState {
    var draggedItem by mutableStateOf<Any?>(null)
        private set

    var dragOffset by mutableStateOf(Offset.Zero)
        private set

    var isDragging by mutableStateOf(false)
        private set

    private val dropTargets = mutableStateMapOf<String, DropTargetInfo>()

    data class DropTargetInfo(
        val bounds: Rect,
        val onDrop: (Any) -> Unit,
        val onHover: (Boolean) -> Unit
    )

    fun startDrag(item: Any, initialOffset: Offset = Offset.Zero) {
        draggedItem = item
        dragOffset = initialOffset
        isDragging = true
    }

    fun updateDragOffset(offset: Offset) {
        dragOffset = offset

        // Check for hover states
        val currentPosition = offset
        dropTargets.forEach { (id, target) ->
            val isHovering = target.bounds.contains(currentPosition)
            target.onHover(isHovering)
        }
    }

    fun endDrag(): Any? {
        val item = draggedItem
        draggedItem = null
        dragOffset = Offset.Zero
        isDragging = false

        // Reset all hover states
        dropTargets.values.forEach { it.onHover(false) }

        return item
    }

    fun registerDropTarget(id: String, bounds: Rect, onDrop: (Any) -> Unit, onHover: (Boolean) -> Unit) {
        dropTargets[id] = DropTargetInfo(bounds, onDrop, onHover)
    }

    fun unregisterDropTarget(id: String) {
        dropTargets.remove(id)
    }

    fun findDropTarget(position: Offset): DropTargetInfo? {
        return dropTargets.values.firstOrNull { it.bounds.contains(position) }
    }
}

val LocalDragDropState = compositionLocalOf { DragDropState() }

/**
 * Professional draggable modifier with magnetic snapping
 *
 * @param data The data to be transferred during drag
 * @param enabled Whether dragging is enabled
 * @param magneticSnapThreshold Distance in pixels to enable snapping (default 10px)
 * @param snapToFrames Whether to snap to frame boundaries
 * @param onDragStart Callback when drag starts
 * @param onDragEnd Callback when drag ends
 */
@Composable
fun Modifier.draggable(
    data: Any,
    enabled: Boolean = true,
    magneticSnapThreshold: Float = 10f,
    snapToFrames: Boolean = true,
    onDragStart: () -> Unit = {},
    onDragEnd: (Boolean) -> Unit = {} // Boolean indicates if dropped on valid target
): Modifier = composed {
    val state = LocalDragDropState.current
    var isDraggingThis by remember { mutableStateOf(false) }
    var localOffset by remember { mutableStateOf(Offset.Zero) }

    // Smooth elevation animation
    val elevation by animateFloatAsState(
        targetValue = if (isDraggingThis) 12f else 2f,
        label = "dragElevation"
    )

    // Smooth alpha animation for drag feedback
    val alpha by animateFloatAsState(
        targetValue = if (isDraggingThis) 0.6f else 1f,
        label = "dragAlpha"
    )

    // Smooth scale animation
    val scale by animateFloatAsState(
        targetValue = if (isDraggingThis) 1.05f else 1f,
        label = "dragScale"
    )

    this
        .shadow(elevation.dp)
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
            this.alpha = alpha
        }
        .offset {
            if (isDraggingThis) {
                IntOffset(localOffset.x.roundToInt(), localOffset.y.roundToInt())
            } else {
                IntOffset.Zero
            }
        }
        .pointerInput(enabled, data) {
            if (!enabled) return@pointerInput

            detectDragGestures(
                onDragStart = { offset ->
                    isDraggingThis = true
                    localOffset = Offset.Zero
                    state.startDrag(data, offset)
                    onDragStart()
                },
                onDrag = { change, dragAmount ->
                    change.consume()

                    // Calculate new offset
                    val newOffset = localOffset + dragAmount

                    // Apply magnetic snapping if enabled
                    val snappedOffset = if (magneticSnapThreshold > 0 && snapToFrames) {
                        Offset(
                            x = applyMagneticSnap(newOffset.x, magneticSnapThreshold),
                            y = applyMagneticSnap(newOffset.y, magneticSnapThreshold)
                        )
                    } else {
                        newOffset
                    }

                    localOffset = snappedOffset
                    state.updateDragOffset(snappedOffset)
                },
                onDragEnd = {
                    isDraggingThis = false

                    // Check if dropped on valid target
                    val dropTarget = state.findDropTarget(localOffset)
                    val droppedOnTarget = dropTarget != null

                    if (droppedOnTarget) {
                        state.draggedItem?.let { item ->
                            dropTarget?.onDrop(item)
                        }
                    }

                    localOffset = Offset.Zero
                    state.endDrag()
                    onDragEnd(droppedOnTarget)
                },
                onDragCancel = {
                    isDraggingThis = false
                    localOffset = Offset.Zero
                    state.endDrag()
                    onDragEnd(false)
                }
            )
        }
}

/**
 * Professional drop target modifier with hover feedback
 *
 * @param enabled Whether the drop target is enabled
 * @param acceptType Optional type filter - only accept items of this type
 * @param onDrop Callback when an item is dropped
 * @param onHoverStart Callback when drag enters the drop zone
 * @param onHoverEnd Callback when drag leaves the drop zone
 */
@Composable
fun Modifier.dropTarget(
    enabled: Boolean = true,
    acceptType: Class<*>? = null,
    onDrop: (Any) -> Unit,
    onHoverStart: () -> Unit = {},
    onHoverEnd: () -> Unit = {}
): Modifier = composed {
    val state = LocalDragDropState.current
    var bounds by remember { mutableStateOf<Rect?>(null) }
    var isHovering by remember { mutableStateOf(false) }
    val targetId = remember { java.util.UUID.randomUUID().toString() }

    // Smooth hover animation
    val hoverScale by animateFloatAsState(
        targetValue = if (isHovering) 1.02f else 1f,
        label = "hoverScale"
    )

    val hoverAlpha by animateFloatAsState(
        targetValue = if (isHovering) 0.8f else 1f,
        label = "hoverAlpha"
    )

    LaunchedEffect(enabled) {
        if (enabled) {
            bounds?.let { b ->
                state.registerDropTarget(
                    id = targetId,
                    bounds = b,
                    onDrop = { item ->
                        // Type checking if specified
                        if (acceptType == null || acceptType.isInstance(item)) {
                            onDrop(item)
                        }
                    },
                    onHover = { hovering ->
                        if (hovering != isHovering) {
                            isHovering = hovering
                            if (hovering) onHoverStart() else onHoverEnd()
                        }
                    }
                )
            }
        }
    }

    DisposableEffect(targetId) {
        onDispose {
            state.unregisterDropTarget(targetId)
        }
    }

    this
        .onGloballyPositioned { coordinates ->
            bounds = coordinates.boundsInWindow()
            bounds?.let { b ->
                if (enabled) {
                    state.registerDropTarget(
                        id = targetId,
                        bounds = b,
                        onDrop = { item ->
                            if (acceptType == null || acceptType.isInstance(item)) {
                                onDrop(item)
                            }
                        },
                        onHover = { hovering ->
                            if (hovering != isHovering) {
                                isHovering = hovering
                                if (hovering) onHoverStart() else onHoverEnd()
                            }
                        }
                    )
                }
            }
        }
        .graphicsLayer {
            scaleX = hoverScale
            scaleY = hoverScale
            alpha = hoverAlpha
        }
}

/**
 * Apply magnetic snapping to a value
 * Snaps to the nearest grid point if within threshold
 *
 * @param value Current value
 * @param threshold Maximum distance to snap
 * @param gridSize Grid size (default 5px for frame-accurate snapping)
 * @return Snapped value
 */
private fun applyMagneticSnap(
    value: Float,
    threshold: Float,
    gridSize: Float = 5f
): Float {
    val nearestGrid = (value / gridSize).roundToInt() * gridSize
    val distance = abs(value - nearestGrid)

    return if (distance <= threshold) {
        nearestGrid
    } else {
        value
    }
}

/**
 * Timeline-specific utilities
 */
object TimelineUtils {
    /**
     * Convert pixel position to time in milliseconds
     *
     * @param pixelX X position in pixels
     * @param zoomLevel Current zoom level (1.0 = normal)
     * @param timelineDuration Total timeline duration in ms
     * @param timelineWidth Width of timeline in pixels
     * @return Time in milliseconds
     */
    fun pixelToTime(
        pixelX: Float,
        zoomLevel: Float,
        timelineDuration: Long,
        timelineWidth: Float
    ): Long {
        val pixelsPerMs = (timelineWidth / timelineDuration) * zoomLevel
        return (pixelX / pixelsPerMs).toLong().coerceIn(0, timelineDuration)
    }

    /**
     * Convert time to pixel position
     *
     * @param timeMs Time in milliseconds
     * @param zoomLevel Current zoom level (1.0 = normal)
     * @param timelineDuration Total timeline duration in ms
     * @param timelineWidth Width of timeline in pixels
     * @return X position in pixels
     */
    fun timeToPixel(
        timeMs: Long,
        zoomLevel: Float,
        timelineDuration: Long,
        timelineWidth: Float
    ): Float {
        val pixelsPerMs = (timelineWidth / timelineDuration) * zoomLevel
        return timeMs * pixelsPerMs
    }

    /**
     * Snap time to nearest frame
     *
     * @param timeMs Time in milliseconds
     * @param frameRate Frame rate (fps)
     * @param threshold Maximum distance in ms to snap
     * @return Snapped time in milliseconds
     */
    fun snapToFrame(
        timeMs: Long,
        frameRate: Int = 30,
        threshold: Long = 33 // ~1 frame at 30fps
    ): Long {
        val frameDuration = 1000L / frameRate
        val nearestFrame = (timeMs / frameDuration).roundToInt() * frameDuration
        val distance = abs(timeMs - nearestFrame)

        return if (distance <= threshold) {
            nearestFrame
        } else {
            timeMs
        }
    }
}
