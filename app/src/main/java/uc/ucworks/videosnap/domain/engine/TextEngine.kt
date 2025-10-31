package uc.ucworks.videosnap.domain.engine

import android.graphics.Bitmap
import android.graphics.Typeface

/**
 * Professional text and titles engine
 * Supports animations, templates, effects, and keyframing
 */
interface TextEngine {
    /**
     * Render text onto bitmap with all effects and animations
     */
    fun renderText(
        bitmap: Bitmap,
        textElement: TextElement,
        timestampMs: Long
    ): Bitmap

    /**
     * Create text from template
     */
    fun createFromTemplate(template: TextTemplate, text: String): TextElement

    /**
     * Get available templates
     */
    fun getTemplates(): List<TextTemplate>

    /**
     * Apply text animation
     */
    fun applyAnimation(element: TextElement, animation: TextAnimation): TextElement
}

/**
 * Text element with full configuration
 */
data class TextElement(
    val id: String,
    val text: String,
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,

    // Typography
    val fontFamily: String = "sans-serif",
    val fontSize: Float = 48f,
    val fontWeight: FontWeight = FontWeight.NORMAL,
    val fontStyle: FontStyle = FontStyle.NORMAL,
    val letterSpacing: Float = 0f,
    val lineHeight: Float = 1.2f,

    // Styling
    val textColor: Int = 0xFFFFFFFF.toInt(),
    val backgroundColor: Int? = null,
    val opacity: Float = 1f,

    // Stroke/Outline
    val strokeWidth: Float = 0f,
    val strokeColor: Int = 0xFF000000.toInt(),

    // Shadow
    val shadowRadius: Float = 0f,
    val shadowDx: Float = 0f,
    val shadowDy: Float = 0f,
    val shadowColor: Int = 0x80000000.toInt(),

    // Alignment
    val textAlign: TextAlign = TextAlign.CENTER,
    val verticalAlign: VerticalAlign = VerticalAlign.TOP,

    // Effects
    val effects: List<TextEffect> = emptyList(),

    // Animation
    val animation: TextAnimation? = null,
    val animationDuration: Long = 1000,

    // Transform
    val rotation: Float = 0f,
    val scaleX: Float = 1f,
    val scaleY: Float = 1f,
    val skewX: Float = 0f,
    val skewY: Float = 0f,

    // Timing
    val startTime: Long = 0,
    val endTime: Long = Long.MAX_VALUE
)

enum class FontWeight {
    THIN,           // 100
    EXTRA_LIGHT,    // 200
    LIGHT,          // 300
    NORMAL,         // 400
    MEDIUM,         // 500
    SEMI_BOLD,      // 600
    BOLD,           // 700
    EXTRA_BOLD,     // 800
    BLACK           // 900
}

enum class FontStyle {
    NORMAL,
    ITALIC,
    OBLIQUE
}

enum class TextAlign {
    LEFT,
    CENTER,
    RIGHT,
    JUSTIFY
}

enum class VerticalAlign {
    TOP,
    MIDDLE,
    BOTTOM
}

/**
 * Text effects
 */
sealed class TextEffect {
    data class Glow(
        val radius: Float,
        val color: Int,
        val intensity: Float = 1f
    ) : TextEffect()

    data class Gradient(
        val startColor: Int,
        val endColor: Int,
        val angle: Float = 0f
    ) : TextEffect()

    data class Wave(
        val amplitude: Float,
        val frequency: Float,
        val speed: Float
    ) : TextEffect()

    data class Typewriter(
        val speed: Float = 50f // characters per second
    ) : TextEffect()

    data class Glitch(
        val intensity: Float,
        val frequency: Float
    ) : TextEffect()

    object Neon : TextEffect()

    data class Outline3D(
        val depth: Float,
        val angle: Float,
        val color: Int
    ) : TextEffect()
}

/**
 * Text animations
 */
sealed class TextAnimation {
    // Entrance animations
    object FadeIn : TextAnimation()
    object SlideFromLeft : TextAnimation()
    object SlideFromRight : TextAnimation()
    object SlideFromTop : TextAnimation()
    object SlideFromBottom : TextAnimation()
    object ZoomIn : TextAnimation()
    object RotateIn : TextAnimation()
    data class TypeOn(val speed: Float = 50f) : TextAnimation()
    object Bounce : TextAnimation()

    // Exit animations
    object FadeOut : TextAnimation()
    object SlideToLeft : TextAnimation()
    object SlideToRight : TextAnimation()
    object SlideToTop : TextAnimation()
    object SlideToBottom : TextAnimation()
    object ZoomOut : TextAnimation()
    object RotateOut : TextAnimation()

    // Continuous animations
    data class Pulse(val scale: Float = 1.1f, val duration: Long = 1000) : TextAnimation()
    data class Float(val amplitude: Float = 10f, val duration: Long = 2000) : TextAnimation()
    data class Rotate(val speed: Float = 360f) : TextAnimation() // degrees per second
    data class Scale(val min: Float = 0.9f, val max: Float = 1.1f, val duration: Long = 1000) : TextAnimation()
    data class Shake(val intensity: Float = 5f, val speed: Float = 10f) : TextAnimation()

    // Letter-by-letter animations
    data class WaveText(val amplitude: Float = 5f, val frequency: Float = 2f) : TextAnimation()
    data class RainbowText(val speed: Float = 1f) : TextAnimation()
    object LetterDrop : TextAnimation()
    object LetterSpin : TextAnimation()
}

/**
 * Text templates for quick creation
 */
data class TextTemplate(
    val id: String,
    val name: String,
    val category: TextCategory,
    val thumbnail: String? = null,

    // Default styling
    val fontSize: Float = 48f,
    val fontFamily: String = "sans-serif",
    val fontWeight: FontWeight = FontWeight.BOLD,
    val textColor: Int = 0xFFFFFFFF.toInt(),
    val backgroundColor: Int? = null,

    // Effects
    val strokeWidth: Float = 0f,
    val strokeColor: Int = 0xFF000000.toInt(),
    val shadowRadius: Float = 0f,

    // Default animation
    val animation: TextAnimation? = null,

    // Effects
    val effects: List<TextEffect> = emptyList(),

    // Position
    val position: TextPosition = TextPosition.CENTER_CENTER
)

enum class TextCategory {
    TITLE,          // Main titles
    SUBTITLE,       // Subtitles/captions
    LOWER_THIRD,    // Lower third graphics
    END_CARD,       // End screen text
    SOCIAL_MEDIA,   // Social media text overlays
    CREDITS,        // Credits/scrolling text
    COUNTDOWN,      // Countdown timers
    CALL_TO_ACTION  // CTA buttons/text
}

enum class TextPosition {
    TOP_LEFT, TOP_CENTER, TOP_RIGHT,
    CENTER_LEFT, CENTER_CENTER, CENTER_RIGHT,
    BOTTOM_LEFT, BOTTOM_CENTER, BOTTOM_RIGHT
}

/**
 * Scrolling credits configuration
 */
data class ScrollingCredits(
    val lines: List<CreditLine>,
    val speed: Float = 30f, // pixels per second
    val startDelay: Long = 1000,
    val endDelay: Long = 1000
)

data class CreditLine(
    val text: String,
    val role: String? = null,
    val fontSize: Float = 24f,
    val spacing: Float = 10f
)

/**
 * Countdown timer configuration
 */
data class CountdownTimer(
    val startValue: Int,
    val endValue: Int = 0,
    val format: String = "mm:ss",
    val fontSize: Float = 72f,
    val fontFamily: String = "monospace",
    val color: Int = 0xFFFFFFFF.toInt(),
    val showMilliseconds: Boolean = false
)

/**
 * Text preset library
 */
object TextPresets {
    /**
     * YouTube-style templates
     */
    fun getYouTubeTemplates(): List<TextTemplate> = listOf(
        TextTemplate(
            id = "yt_title",
            name = "YouTube Title",
            category = TextCategory.TITLE,
            fontSize = 72f,
            fontWeight = FontWeight.BLACK,
            textColor = 0xFFFFFFFF.toInt(),
            strokeWidth = 4f,
            strokeColor = 0xFF000000.toInt(),
            shadowRadius = 8f,
            animation = TextAnimation.SlideFromBottom,
            position = TextPosition.BOTTOM_CENTER
        ),
        TextTemplate(
            id = "yt_subscribe",
            name = "Subscribe CTA",
            category = TextCategory.CALL_TO_ACTION,
            fontSize = 48f,
            fontWeight = FontWeight.BOLD,
            textColor = 0xFFFF0000.toInt(),
            backgroundColor = 0xFFFFFFFF.toInt(),
            effects = listOf(TextEffect.Glow(10f, 0xFFFF0000.toInt())),
            animation = TextAnimation.Pulse(1.1f, 800),
            position = TextPosition.BOTTOM_RIGHT
        )
    )

    /**
     * Instagram-style templates
     */
    fun getInstagramTemplates(): List<TextTemplate> = listOf(
        TextTemplate(
            id = "ig_story",
            name = "Instagram Story",
            category = TextCategory.SOCIAL_MEDIA,
            fontSize = 56f,
            fontWeight = FontWeight.BOLD,
            textColor = 0xFFFFFFFF.toInt(),
            strokeWidth = 2f,
            effects = listOf(
                TextEffect.Gradient(
                    startColor = 0xFFFD1D1D.toInt(),
                    endColor = 0xFFFCAF45.toInt(),
                    angle = 45f
                )
            ),
            animation = TextAnimation.ZoomIn,
            position = TextPosition.TOP_CENTER
        )
    )

    /**
     * TikTok-style templates
     */
    fun getTikTokTemplates(): List<TextTemplate> = listOf(
        TextTemplate(
            id = "tiktok_caption",
            name = "TikTok Caption",
            category = TextCategory.SUBTITLE,
            fontSize = 48f,
            fontWeight = FontWeight.BLACK,
            textColor = 0xFFFFFFFF.toInt(),
            strokeWidth = 6f,
            strokeColor = 0xFF000000.toInt(),
            animation = TextAnimation.TypeOn(30f),
            position = TextPosition.CENTER_CENTER
        )
    )

    /**
     * Professional broadcast templates
     */
    fun getBroadcastTemplates(): List<TextTemplate> = listOf(
        TextTemplate(
            id = "lower_third",
            name = "Lower Third",
            category = TextCategory.LOWER_THIRD,
            fontSize = 32f,
            fontWeight = FontWeight.SEMI_BOLD,
            textColor = 0xFFFFFFFF.toInt(),
            backgroundColor = 0xCC000000.toInt(),
            animation = TextAnimation.SlideFromLeft,
            position = TextPosition.BOTTOM_LEFT
        ),
        TextTemplate(
            id = "breaking_news",
            name = "Breaking News Banner",
            category = TextCategory.TITLE,
            fontSize = 40f,
            fontWeight = FontWeight.BOLD,
            textColor = 0xFFFFFFFF.toInt(),
            backgroundColor = 0xFFCC0000.toInt(),
            animation = TextAnimation.SlideFromTop,
            effects = listOf(TextEffect.Glow(5f, 0xFFFF0000.toInt())),
            position = TextPosition.TOP_CENTER
        )
    )
}
