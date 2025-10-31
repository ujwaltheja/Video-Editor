package uc.ucworks.videosnap.domain.engine

import android.graphics.Bitmap
import android.graphics.Color
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * Professional color grading engine with industry-standard tools
 * Implements features found in DaVinci Resolve, Adobe Premiere, Final Cut Pro
 */
interface ColorGradingEngine {
    /**
     * Apply color wheels adjustments (shadows, midtones, highlights)
     */
    fun applyColorWheels(
        bitmap: Bitmap,
        shadows: ColorWheelAdjustment,
        midtones: ColorWheelAdjustment,
        highlights: ColorWheelAdjustment
    ): Bitmap

    /**
     * Apply RGB curves adjustment
     */
    fun applyCurves(
        bitmap: Bitmap,
        curves: CurvesAdjustment
    ): Bitmap

    /**
     * Apply 3D LUT (Look-Up Table) for color grading
     */
    fun applyLUT(
        bitmap: Bitmap,
        lutData: LUT3D
    ): Bitmap

    /**
     * Apply HSL (Hue, Saturation, Lightness) adjustment
     */
    fun applyHSL(
        bitmap: Bitmap,
        hueShift: Float,     // -180 to 180 degrees
        saturation: Float,   // 0 to 2 (1 = no change)
        lightness: Float     // -1 to 1 (0 = no change)
    ): Bitmap

    /**
     * Apply temperature and tint adjustment
     */
    fun applyTemperatureTint(
        bitmap: Bitmap,
        temperature: Float,  // -100 to 100 (warm to cool)
        tint: Float          // -100 to 100 (green to magenta)
    ): Bitmap

    /**
     * Apply film emulation presets
     */
    fun applyFilmEmulation(
        bitmap: Bitmap,
        preset: FilmPreset
    ): Bitmap

    /**
     * Apply log to linear conversion (for RAW/LOG footage)
     */
    fun logToLinear(
        bitmap: Bitmap,
        logFormat: LogFormat = LogFormat.LOG_C
    ): Bitmap

    /**
     * Export LUT from current grading settings
     */
    fun exportLUT(
        adjustments: ColorGradingAdjustments,
        size: Int = 33 // Standard LUT size
    ): LUT3D
}

/**
 * Color wheel adjustment for a specific tonal range
 */
data class ColorWheelAdjustment(
    val lift: Float = 0f,         // Brightness offset (-1 to 1)
    val gamma: Float = 1f,        // Midpoint (0.1 to 10)
    val gain: Float = 1f,         // Brightness multiplier (0 to 2)
    val hue: Float = 0f,          // Hue shift (-180 to 180)
    val saturation: Float = 1f    // Saturation (0 to 2)
)

/**
 * Bezier curves for each channel
 */
data class CurvesAdjustment(
    val master: List<CurvePoint> = listOf(
        CurvePoint(0f, 0f),
        CurvePoint(1f, 1f)
    ),
    val red: List<CurvePoint> = listOf(
        CurvePoint(0f, 0f),
        CurvePoint(1f, 1f)
    ),
    val green: List<CurvePoint> = listOf(
        CurvePoint(0f, 0f),
        CurvePoint(1f, 1f)
    ),
    val blue: List<CurvePoint> = listOf(
        CurvePoint(0f, 0f),
        CurvePoint(1f, 1f)
    )
)

data class CurvePoint(
    val input: Float,   // 0 to 1
    val output: Float   // 0 to 1
)

/**
 * 3D Look-Up Table for color transformations
 */
data class LUT3D(
    val size: Int,              // Typically 17, 33, or 65
    val data: FloatArray        // size^3 * 3 RGB values
) {
    /**
     * Sample LUT at given RGB coordinates
     */
    fun sample(r: Float, g: Float, b: Float): Triple<Float, Float, Float> {
        // Trilinear interpolation
        val scaledR = r * (size - 1)
        val scaledG = g * (size - 1)
        val scaledB = b * (size - 1)

        val r0 = scaledR.toInt().coerceIn(0, size - 2)
        val g0 = scaledG.toInt().coerceIn(0, size - 2)
        val b0 = scaledB.toInt().coerceIn(0, size - 2)

        val r1 = r0 + 1
        val g1 = g0 + 1
        val b1 = b0 + 1

        val dr = scaledR - r0
        val dg = scaledG - g0
        val db = scaledB - b0

        // Interpolate
        val c000 = getLUTValue(r0, g0, b0)
        val c001 = getLUTValue(r0, g0, b1)
        val c010 = getLUTValue(r0, g1, b0)
        val c011 = getLUTValue(r0, g1, b1)
        val c100 = getLUTValue(r1, g0, b0)
        val c101 = getLUTValue(r1, g0, b1)
        val c110 = getLUTValue(r1, g1, b0)
        val c111 = getLUTValue(r1, g1, b1)

        val c00 = lerp(c000, c001, db)
        val c01 = lerp(c010, c011, db)
        val c10 = lerp(c100, c101, db)
        val c11 = lerp(c110, c111, db)

        val c0 = lerp(c00, c01, dg)
        val c1 = lerp(c10, c11, dg)

        return lerp(c0, c1, dr)
    }

    private fun getLUTValue(r: Int, g: Int, b: Int): Triple<Float, Float, Float> {
        val index = (b * size * size + g * size + r) * 3
        return Triple(
            data[index],
            data[index + 1],
            data[index + 2]
        )
    }

    private fun lerp(
        a: Triple<Float, Float, Float>,
        b: Triple<Float, Float, Float>,
        t: Float
    ): Triple<Float, Float, Float> {
        return Triple(
            a.first + (b.first - a.first) * t,
            a.second + (b.second - a.second) * t,
            a.third + (b.third - a.third) * t
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as LUT3D

        if (size != other.size) return false
        if (!data.contentEquals(other.data)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = size
        result = 31 * result + data.contentHashCode()
        return result
    }
}

/**
 * Film emulation presets
 */
enum class FilmPreset {
    KODAK_2383,          // Classic film look
    KODAK_VISION3_500T,  // Modern cinema
    FUJI_ETERNA,         // Muted, cinematic
    KODAK_PORTRA_400,    // Portrait film
    ILFORD_HP5,          // Black and white
    CINEON_LOG,          // Log encoding
    ALEXA_LOG_C,         // ARRI Log C
    RED_LOG_3_G10,       // RED Log
    SONY_S_LOG3          // Sony S-Log3
}

/**
 * Log encoding formats
 */
enum class LogFormat {
    LOG_C,      // ARRI Alexa Log C
    S_LOG3,     // Sony S-Log3
    LOG_3_G10,  // RED Log3G10
    V_LOG,      // Panasonic V-Log
    CINEON_LOG, // Cineon/DPX Log
    LINEAR      // Linear (no log)
}

/**
 * Complete color grading adjustment set
 */
data class ColorGradingAdjustments(
    val colorWheels: ColorWheels = ColorWheels(),
    val curves: CurvesAdjustment = CurvesAdjustment(),
    val hsl: HSLAdjustment = HSLAdjustment(),
    val temperatureTint: TemperatureTintAdjustment = TemperatureTintAdjustment(),
    val lut: LUT3D? = null,
    val filmPreset: FilmPreset? = null
)

data class ColorWheels(
    val shadows: ColorWheelAdjustment = ColorWheelAdjustment(),
    val midtones: ColorWheelAdjustment = ColorWheelAdjustment(),
    val highlights: ColorWheelAdjustment = ColorWheelAdjustment()
)

data class HSLAdjustment(
    val hueShift: Float = 0f,
    val saturation: Float = 1f,
    val lightness: Float = 0f
)

data class TemperatureTintAdjustment(
    val temperature: Float = 0f,
    val tint: Float = 0f
)

/**
 * Color space utilities
 */
object ColorSpace {
    /**
     * Convert RGB to HSL
     */
    fun rgbToHSL(r: Float, g: Float, b: Float): Triple<Float, Float, Float> {
        val max = maxOf(r, g, b)
        val min = minOf(r, g, b)
        val delta = max - min

        // Lightness
        val l = (max + min) / 2f

        if (delta == 0f) {
            return Triple(0f, 0f, l) // Grayscale
        }

        // Saturation
        val s = if (l < 0.5f) {
            delta / (max + min)
        } else {
            delta / (2f - max - min)
        }

        // Hue
        val h = when (max) {
            r -> ((g - b) / delta + if (g < b) 6f else 0f) / 6f
            g -> ((b - r) / delta + 2f) / 6f
            else -> ((r - g) / delta + 4f) / 6f
        }

        return Triple(h * 360f, s, l)
    }

    /**
     * Convert HSL to RGB
     */
    fun hslToRGB(h: Float, s: Float, l: Float): Triple<Float, Float, Float> {
        if (s == 0f) {
            return Triple(l, l, l) // Grayscale
        }

        val hue = h / 360f

        val q = if (l < 0.5f) l * (1f + s) else l + s - l * s
        val p = 2f * l - q

        val r = hueToRGB(p, q, hue + 1f / 3f)
        val g = hueToRGB(p, q, hue)
        val b = hueToRGB(p, q, hue - 1f / 3f)

        return Triple(r, g, b)
    }

    private fun hueToRGB(p: Float, q: Float, t: Float): Float {
        var t = t
        if (t < 0f) t += 1f
        if (t > 1f) t -= 1f

        return when {
            t < 1f / 6f -> p + (q - p) * 6f * t
            t < 1f / 2f -> q
            t < 2f / 3f -> p + (q - p) * (2f / 3f - t) * 6f
            else -> p
        }
    }

    /**
     * Apply gamma correction
     */
    fun applyGamma(value: Float, gamma: Float): Float {
        return value.pow(1f / gamma).coerceIn(0f, 1f)
    }

    /**
     * Clamp RGB values to valid range
     */
    fun clampRGB(r: Float, g: Float, b: Float): Triple<Float, Float, Float> {
        return Triple(
            r.coerceIn(0f, 1f),
            g.coerceIn(0f, 1f),
            b.coerceIn(0f, 1f)
        )
    }
}
