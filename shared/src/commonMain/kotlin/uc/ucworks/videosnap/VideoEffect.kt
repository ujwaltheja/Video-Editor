package uc.ucworks.videosnap

/**
 * Represents a video effect that can be applied to clips.
 *
 * @property id Unique identifier for the effect.
 * @property name Display name of the effect.
 * @property type Type of effect.
 * @property parameters Map of effect parameters and their values.
 * @property isEnabled Whether the effect is currently enabled.
 */
data class VideoEffect(
    val id: String = System.currentTimeMillis().toString(),
    val name: String,
    val type: EffectType,
    val parameters: Map<String, Float> = emptyMap(),
    val isEnabled: Boolean = true
)

/**
 * Types of video effects available.
 */
enum class EffectType {
    // Color Effects
    BRIGHTNESS,
    CONTRAST,
    SATURATION,
    HUE,
    GRAYSCALE,
    SEPIA,
    INVERT,
    COLOR_GRADING,
    WHITE_BALANCE,
    EXPOSURE,
    HIGHLIGHTS,
    SHADOWS,
    VIGNETTE,

    // Blur Effects
    GAUSSIAN_BLUR,
    BOX_BLUR,
    MOTION_BLUR,
    RADIAL_BLUR,

    // Stylistic Effects
    SHARPEN,
    NOISE,
    GRAIN,
    PIXELATE,
    EDGE_DETECT,
    EMBOSS,
    OIL_PAINT,
    CARTOON,
    SKETCH,

    // Chroma Key / Green Screen
    CHROMA_KEY,
    LUMA_KEY,

    // Transform Effects
    SCALE,
    ROTATE,
    FLIP_HORIZONTAL,
    FLIP_VERTICAL,
    PERSPECTIVE,
    DISTORT,

    // Advanced Effects
    STABILIZATION,
    SLOW_MOTION,
    TIME_LAPSE,
    REVERSE,
    FREEZE_FRAME,

    // Audio Effects
    FADE_IN_AUDIO,
    FADE_OUT_AUDIO,
    NORMALIZE_AUDIO,
    EQUALIZER,
    REVERB,
    ECHO,

    // Composite Effects
    PICTURE_IN_PICTURE,
    SPLIT_SCREEN,
    MASK,
    BLEND_MODE
}

/**
 * Color grading parameters.
 */
data class ColorGrading(
    val temperature: Float = 0f,      // -100 to 100
    val tint: Float = 0f,             // -100 to 100
    val exposure: Float = 0f,         // -2 to 2
    val contrast: Float = 0f,         // -100 to 100
    val highlights: Float = 0f,       // -100 to 100
    val shadows: Float = 0f,          // -100 to 100
    val whites: Float = 0f,           // -100 to 100
    val blacks: Float = 0f,           // -100 to 100
    val saturation: Float = 0f,       // -100 to 100
    val vibrance: Float = 0f          // -100 to 100
)

/**
 * Chroma key (green screen) parameters.
 */
data class ChromaKeyParams(
    val keyColor: Int = 0x00FF00,     // RGB color to key out
    val similarity: Float = 0.3f,      // 0.0 to 1.0
    val smoothness: Float = 0.1f,      // 0.0 to 1.0
    val spillReduction: Float = 0.5f   // 0.0 to 1.0
)

/**
 * Blur effect parameters.
 */
data class BlurParams(
    val radius: Float = 10f,           // Blur radius
    val quality: BlurQuality = BlurQuality.MEDIUM
)

enum class BlurQuality {
    LOW,
    MEDIUM,
    HIGH,
    ULTRA
}
