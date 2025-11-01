package uc.ucworks.videosnap.domain.engine

import android.graphics.*
import android.text.StaticLayout
import android.text.TextPaint
import uc.ucworks.videosnap.util.Logger
import java.util.*
import kotlin.math.*
import javax.inject.Inject

/**
 * Implementation of professional text engine with animations and effects
 */
class TextEngineImpl @Inject constructor() : TextEngine {

    companion object {
        private const val TAG = "TextEngineImpl"
    }

    private val templates = mutableListOf<TextTemplate>()

    init {
        // Load all templates
        templates.addAll(TextPresets.getYouTubeTemplates())
        templates.addAll(TextPresets.getInstagramTemplates())
        templates.addAll(TextPresets.getTikTokTemplates())
        templates.addAll(TextPresets.getBroadcastTemplates())
    }

    override fun renderText(
        bitmap: Bitmap,
        textElement: TextElement,
        timestampMs: Long
    ): Bitmap {
        // Check if text should be visible at this timestamp
        if (timestampMs < textElement.startTime || timestampMs > textElement.endTime) {
            return bitmap
        }

        val canvas = Canvas(bitmap)

        // Calculate animation progress
        val animProgress = if (textElement.animation != null) {
            val elapsed = timestampMs - textElement.startTime
            min(elapsed.toFloat() / textElement.animationDuration, 1f)
        } else {
            1f
        }

        // Apply animation transformations
        val transform = calculateAnimationTransform(textElement, animProgress, timestampMs)

        // Save canvas state
        canvas.save()

        // Apply transform
        val matrix = Matrix()
        matrix.postTranslate(textElement.x, textElement.y)
        matrix.postRotate(textElement.rotation + transform.rotation, textElement.x + textElement.width / 2, textElement.y + textElement.height / 2)
        matrix.postScale(textElement.scaleX * transform.scaleX, textElement.scaleY * transform.scaleY, textElement.x + textElement.width / 2, textElement.y + textElement.height / 2)
        matrix.postTranslate(transform.translateX, transform.translateY)

        canvas.concat(matrix)

        // Draw background if specified
        textElement.backgroundColor?.let { bgColor ->
            val bgPaint = Paint().apply {
                color = bgColor
                alpha = (textElement.opacity * 255 * transform.alpha).toInt()
            }
            canvas.drawRect(0f, 0f, textElement.width, textElement.height, bgPaint)
        }

        // Create text paint
        val textPaint = createTextPaint(textElement, transform.alpha)

        // Apply effects
        applyTextEffects(textPaint, textElement.effects, timestampMs)

        // Get visible text (for typewriter effect)
        val visibleText = getVisibleText(textElement, animProgress)

        // Draw text
        drawStyledText(canvas, visibleText, textElement, textPaint)

        // Restore canvas
        canvas.restore()

        return bitmap
    }

    override fun createFromTemplate(template: TextTemplate, text: String): TextElement {
        val position = calculatePosition(template.position, 1920f, 1080f, 400f, 100f)

        return TextElement(
            id = UUID.randomUUID().toString(),
            text = text,
            x = position.first,
            y = position.second,
            width = 400f,
            height = 100f,
            fontFamily = template.fontFamily,
            fontSize = template.fontSize,
            fontWeight = template.fontWeight,
            textColor = template.textColor,
            backgroundColor = template.backgroundColor,
            strokeWidth = template.strokeWidth,
            strokeColor = template.strokeColor,
            shadowRadius = template.shadowRadius,
            effects = template.effects,
            animation = template.animation
        )
    }

    override fun getTemplates(): List<TextTemplate> = templates

    override fun applyAnimation(element: TextElement, animation: TextAnimation): TextElement {
        return element.copy(animation = animation)
    }

    /**
     * Create text paint with styling
     */
    private fun createTextPaint(element: TextElement, alpha: Float): TextPaint {
        return TextPaint().apply {
            color = element.textColor
            this.alpha = (alpha * 255 * element.opacity).toInt()
            textSize = element.fontSize
            typeface = createTypeface(element.fontFamily, element.fontWeight, element.fontStyle)
            isAntiAlias = true
            letterSpacing = element.letterSpacing

            // Apply stroke
            if (element.strokeWidth > 0) {
                style = Paint.Style.FILL_AND_STROKE
                strokeWidth = element.strokeWidth
                strokeJoin = Paint.Join.ROUND
                strokeCap = Paint.Cap.ROUND
            }

            // Apply shadow
            if (element.shadowRadius > 0) {
                setShadowLayer(
                    element.shadowRadius,
                    element.shadowDx,
                    element.shadowDy,
                    element.shadowColor
                )
            }

            // Text alignment
            textAlign = when (element.textAlign) {
                TextAlign.LEFT -> Paint.Align.LEFT
                TextAlign.CENTER -> Paint.Align.CENTER
                TextAlign.RIGHT -> Paint.Align.RIGHT
                TextAlign.JUSTIFY -> Paint.Align.LEFT // Android doesn't support justify directly
            }
        }
    }

    /**
     * Create typeface from font settings
     */
    private fun createTypeface(family: String, weight: FontWeight, style: FontStyle): Typeface {
        val androidStyle = when {
            weight >= FontWeight.BOLD && style == FontStyle.ITALIC -> Typeface.BOLD_ITALIC
            weight >= FontWeight.BOLD -> Typeface.BOLD
            style == FontStyle.ITALIC -> Typeface.ITALIC
            else -> Typeface.NORMAL
        }

        return when (family.lowercase()) {
            "serif" -> Typeface.create(Typeface.SERIF, androidStyle)
            "monospace" -> Typeface.create(Typeface.MONOSPACE, androidStyle)
            else -> Typeface.create(Typeface.SANS_SERIF, androidStyle)
        }
    }

    /**
     * Draw styled text on canvas
     */
    private fun drawStyledText(
        canvas: Canvas,
        text: String,
        element: TextElement,
        paint: TextPaint
    ) {
        val x = when (element.textAlign) {
            TextAlign.LEFT -> 0f
            TextAlign.CENTER -> element.width / 2
            TextAlign.RIGHT -> element.width
            TextAlign.JUSTIFY -> 0f
        }

        val y = when (element.verticalAlign) {
            VerticalAlign.TOP -> paint.textSize
            VerticalAlign.MIDDLE -> element.height / 2 + paint.textSize / 3
            VerticalAlign.BOTTOM -> element.height - paint.descent()
        }

        // Draw stroke first if needed
        if (element.strokeWidth > 0) {
            val strokePaint = TextPaint(paint).apply {
                style = Paint.Style.STROKE
                color = element.strokeColor
            }
            canvas.drawText(text, x, y, strokePaint)
        }

        // Draw fill
        canvas.drawText(text, x, y, paint)
    }

    /**
     * Calculate animation transform
     */
    private fun calculateAnimationTransform(
        element: TextElement,
        progress: Float,
        timestampMs: Long
    ): Transform {
        val animation = element.animation ?: return Transform()

        return when (animation) {
            is TextAnimation.FadeIn -> Transform(alpha = progress)
            is TextAnimation.FadeOut -> Transform(alpha = 1f - progress)

            is TextAnimation.SlideFromLeft -> Transform(
                translateX = -element.width * (1f - progress)
            )
            is TextAnimation.SlideFromRight -> Transform(
                translateX = element.width * (1f - progress)
            )
            is TextAnimation.SlideFromTop -> Transform(
                translateY = -element.height * (1f - progress)
            )
            is TextAnimation.SlideFromBottom -> Transform(
                translateY = element.height * (1f - progress)
            )

            is TextAnimation.ZoomIn -> Transform(
                scaleX = progress,
                scaleY = progress
            )
            is TextAnimation.ZoomOut -> Transform(
                scaleX = 1f + (1f - progress),
                scaleY = 1f + (1f - progress),
                alpha = 1f - progress
            )

            is TextAnimation.RotateIn -> Transform(
                rotation = -360f * (1f - progress),
                alpha = progress
            )
            is TextAnimation.RotateOut -> Transform(
                rotation = 360f * progress,
                alpha = 1f - progress
            )

            is TextAnimation.Pulse -> {
                val t = (timestampMs % animation.duration).toFloat() / animation.duration
                val scale = 1f + (animation.scale - 1f) * sin(t * PI.toFloat() * 2)
                Transform(scaleX = scale, scaleY = scale)
            }

            is TextAnimation.Float -> {
                val t = (timestampMs % animation.duration).toFloat() / animation.duration
                val offset = sin(t * PI.toFloat() * 2) * animation.amplitude
                Transform(translateY = offset)
            }

            is TextAnimation.Rotate -> {
                val elapsed = timestampMs - element.startTime
                val rotation = (elapsed / 1000f) * animation.speed
                Transform(rotation = rotation % 360f)
            }

            is TextAnimation.Shake -> {
                val t = (timestampMs / 100f) * animation.speed
                val offsetX = sin(t) * animation.intensity
                val offsetY = cos(t * 1.3f) * animation.intensity
                Transform(translateX = offsetX, translateY = offsetY)
            }

            is TextAnimation.Bounce -> {
                val bounceProgress = if (progress < 0.8f) {
                    val t = progress / 0.8f
                    abs(sin(t * PI.toFloat() * 4)) * (1f - t)
                } else 0f
                Transform(translateY = -bounceProgress * 100f)
            }

            else -> Transform()
        }
    }

    /**
     * Get visible text (for typewriter effect)
     */
    private fun getVisibleText(element: TextElement, progress: Float): String {
        return when (element.animation) {
            is TextAnimation.TypeOn -> {
                val charCount = (element.text.length * progress).toInt()
                element.text.take(charCount)
            }
            else -> element.text
        }
    }

    /**
     * Apply text effects to paint
     */
    private fun applyTextEffects(paint: TextPaint, effects: List<TextEffect>, timestampMs: Long) {
        effects.forEach { effect ->
            when (effect) {
                is TextEffect.Glow -> {
                    paint.setShadowLayer(
                        effect.radius * effect.intensity,
                        0f, 0f,
                        effect.color
                    )
                }
                is TextEffect.Neon -> {
                    paint.setShadowLayer(15f, 0f, 0f, paint.color)
                }
                else -> {
                    // Other effects require more complex rendering
                }
            }
        }
    }

    /**
     * Calculate position from template position enum
     */
    private fun calculatePosition(
        position: TextPosition,
        screenWidth: Float,
        screenHeight: Float,
        textWidth: Float,
        textHeight: Float
    ): Pair<Float, Float> {
        return when (position) {
            TextPosition.TOP_LEFT -> Pair(20f, 20f)
            TextPosition.TOP_CENTER -> Pair((screenWidth - textWidth) / 2, 20f)
            TextPosition.TOP_RIGHT -> Pair(screenWidth - textWidth - 20f, 20f)
            TextPosition.CENTER_LEFT -> Pair(20f, (screenHeight - textHeight) / 2)
            TextPosition.CENTER_CENTER -> Pair((screenWidth - textWidth) / 2, (screenHeight - textHeight) / 2)
            TextPosition.CENTER_RIGHT -> Pair(screenWidth - textWidth - 20f, (screenHeight - textHeight) / 2)
            TextPosition.BOTTOM_LEFT -> Pair(20f, screenHeight - textHeight - 20f)
            TextPosition.BOTTOM_CENTER -> Pair((screenWidth - textWidth) / 2, screenHeight - textHeight - 20f)
            TextPosition.BOTTOM_RIGHT -> Pair(screenWidth - textWidth - 20f, screenHeight - textHeight - 20f)
        }
    }

    /**
     * Transform data class
     */
    private data class Transform(
        val translateX: Float = 0f,
        val translateY: Float = 0f,
        val scaleX: Float = 1f,
        val scaleY: Float = 1f,
        val rotation: Float = 0f,
        val alpha: Float = 1f
    )
}
