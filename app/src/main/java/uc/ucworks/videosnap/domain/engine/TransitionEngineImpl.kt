package uc.ucworks.videosnap.domain.engine

import android.graphics.*
import androidx.core.graphics.createBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import uc.ucworks.videosnap.TransitionType
import javax.inject.Inject
import kotlin.math.min

class TransitionEngineImpl @Inject constructor() : TransitionEngine {

    override suspend fun applyTransition(
        from: Bitmap,
        to: Bitmap,
        transitionType: TransitionType,
        progress: Float
    ): Bitmap = withContext(Dispatchers.Default) {
        val p = progress.coerceIn(0f, 1f)

        when (transitionType) {
            TransitionType.FADE -> applyFade(from, to, p)
            TransitionType.DISSOLVE -> applyDissolve(from, to, p)
            TransitionType.WIPE_LEFT -> applyWipe(from, to, p, WipeDirection.LEFT)
            TransitionType.WIPE_RIGHT -> applyWipe(from, to, p, WipeDirection.RIGHT)
            TransitionType.WIPE_UP -> applyWipe(from, to, p, WipeDirection.UP)
            TransitionType.WIPE_DOWN -> applyWipe(from, to, p, WipeDirection.DOWN)
            TransitionType.SLIDE_LEFT -> applySlide(from, to, p, SlideDirection.LEFT)
            TransitionType.SLIDE_RIGHT -> applySlide(from, to, p, SlideDirection.RIGHT)
            TransitionType.ZOOM_IN -> applyZoom(from, to, p, true)
            TransitionType.ZOOM_OUT -> applyZoom(from, to, p, false)
            TransitionType.CIRCLE_OPEN -> applyCircle(from, to, p, true)
            TransitionType.CIRCLE_CLOSE -> applyCircle(from, to, p, false)
        }
    }

    override fun getAvailableTransitions(): List<TransitionType> {
        return TransitionType.values().toList()
    }

    private fun applyFade(from: Bitmap, to: Bitmap, progress: Float): Bitmap {
        val output = createBitmap(from.width, from.height)
        val canvas = Canvas(output)

        // Draw 'from' frame
        val paintFrom = Paint().apply {
            alpha = ((1f - progress) * 255).toInt()
        }
        canvas.drawBitmap(from, 0f, 0f, paintFrom)

        // Draw 'to' frame on top
        val paintTo = Paint().apply {
            alpha = (progress * 255).toInt()
        }
        canvas.drawBitmap(to, 0f, 0f, paintTo)

        return output
    }

    private fun applyDissolve(from: Bitmap, to: Bitmap, progress: Float): Bitmap {
        // Dissolve is similar to fade
        return applyFade(from, to, progress)
    }

    private enum class WipeDirection { LEFT, RIGHT, UP, DOWN }

    private fun applyWipe(from: Bitmap, to: Bitmap, progress: Float, direction: WipeDirection): Bitmap {
        val output = createBitmap(from.width, from.height)
        val canvas = Canvas(output)

        // Draw 'from' frame as base
        canvas.drawBitmap(from, 0f, 0f, null)

        // Calculate wipe rect based on direction
        val rect = when (direction) {
            WipeDirection.LEFT -> Rect(
                0, 0,
                (from.width * progress).toInt(),
                from.height
            )
            WipeDirection.RIGHT -> Rect(
                (from.width * (1f - progress)).toInt(), 0,
                from.width,
                from.height
            )
            WipeDirection.UP -> Rect(
                0, 0,
                from.width,
                (from.height * progress).toInt()
            )
            WipeDirection.DOWN -> Rect(
                0, (from.height * (1f - progress)).toInt(),
                from.width,
                from.height
            )
        }

        // Draw 'to' frame in the wipe rect
        canvas.save()
        canvas.clipRect(rect)
        canvas.drawBitmap(to, 0f, 0f, null)
        canvas.restore()

        return output
    }

    private enum class SlideDirection { LEFT, RIGHT }

    private fun applySlide(from: Bitmap, to: Bitmap, progress: Float, direction: SlideDirection): Bitmap {
        val output = createBitmap(from.width, from.height)
        val canvas = Canvas(output)

        when (direction) {
            SlideDirection.LEFT -> {
                // 'from' slides out to left, 'to' slides in from right
                canvas.drawBitmap(from, -(from.width * progress), 0f, null)
                canvas.drawBitmap(to, from.width * (1f - progress), 0f, null)
            }
            SlideDirection.RIGHT -> {
                // 'from' slides out to right, 'to' slides in from left
                canvas.drawBitmap(from, from.width * progress, 0f, null)
                canvas.drawBitmap(to, -(from.width * (1f - progress)), 0f, null)
            }
        }

        return output
    }

    private fun applyZoom(from: Bitmap, to: Bitmap, progress: Float, zoomIn: Boolean): Bitmap {
        val output = createBitmap(from.width, from.height)
        val canvas = Canvas(output)

        // Draw base frame
        canvas.drawBitmap(from, 0f, 0f, null)

        // Calculate zoom scale and position
        val scale = if (zoomIn) progress else (1f - progress)
        val scaledWidth = to.width * scale
        val scaledHeight = to.height * scale
        val left = (to.width - scaledWidth) / 2f
        val top = (to.height - scaledHeight) / 2f

        val destRect = RectF(left, top, left + scaledWidth, top + scaledHeight)
        val srcRect = Rect(0, 0, to.width, to.height)

        val paint = Paint().apply {
            alpha = (progress * 255).toInt()
        }
        canvas.drawBitmap(to, srcRect, destRect, paint)

        return output
    }

    private fun applyCircle(from: Bitmap, to: Bitmap, progress: Float, open: Boolean): Bitmap {
        val output = createBitmap(from.width, from.height)
        val canvas = Canvas(output)

        // Draw base frame
        canvas.drawBitmap(from, 0f, 0f, null)

        // Calculate circle radius
        val maxRadius = Math.sqrt(
            (from.width * from.width + from.height * from.height).toDouble()
        ).toFloat() / 2f
        val radius = if (open) maxRadius * progress else maxRadius * (1f - progress)

        // Draw 'to' frame with circular mask
        canvas.save()
        val path = Path().apply {
            addCircle(
                from.width / 2f,
                from.height / 2f,
                radius,
                Path.Direction.CW
            )
        }
        canvas.clipPath(path)
        canvas.drawBitmap(to, 0f, 0f, null)
        canvas.restore()

        return output
    }
}
