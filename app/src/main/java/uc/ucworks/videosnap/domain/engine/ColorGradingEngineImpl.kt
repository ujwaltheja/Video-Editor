package uc.ucworks.videosnap.domain.engine

import android.graphics.Bitmap
import android.graphics.Color
import javax.inject.Inject
import kotlin.math.*

/**
 * Professional implementation of color grading engine
 * Provides industry-standard color correction tools
 */
class ColorGradingEngineImpl @Inject constructor() : ColorGradingEngine {

    override fun applyColorWheels(
        bitmap: Bitmap,
        shadows: ColorWheelAdjustment,
        midtones: ColorWheelAdjustment,
        highlights: ColorWheelAdjustment
    ): Bitmap {
        val result = bitmap.copy(bitmap.config, true)
        val width = result.width
        val height = result.height

        for (y in 0 until height) {
            for (x in 0 until width) {
                val pixel = result.getPixel(x, y)

                var r = Color.red(pixel) / 255f
                var g = Color.green(pixel) / 255f
                var b = Color.blue(pixel) / 255f

                // Calculate luminance to determine tonal range
                val luminance = 0.299f * r + 0.587f * g + 0.114f * b

                // Calculate weights for each tonal range (smooth transitions)
                val shadowWeight = calculateShadowWeight(luminance)
                val midtoneWeight = calculateMidtoneWeight(luminance)
                val highlightWeight = calculateHighlightWeight(luminance)

                // Apply each color wheel with weighted blending
                val (r1, g1, b1) = applyColorWheelToPixel(r, g, b, shadows, shadowWeight)
                val (r2, g2, b2) = applyColorWheelToPixel(r1, g1, b1, midtones, midtoneWeight)
                val (r3, g3, b3) = applyColorWheelToPixel(r2, g2, b2, highlights, highlightWeight)

                // Clamp and convert back to int
                val newR = (r3 * 255).toInt().coerceIn(0, 255)
                val newG = (g3 * 255).toInt().coerceIn(0, 255)
                val newB = (b3 * 255).toInt().coerceIn(0, 255)

                result.setPixel(x, y, Color.rgb(newR, newG, newB))
            }
        }

        return result
    }

    override fun applyCurves(bitmap: Bitmap, curves: CurvesAdjustment): Bitmap {
        val result = bitmap.copy(bitmap.config, true)
        val width = result.width
        val height = result.height

        // Build lookup tables for each channel
        val masterLUT = buildCurveLUT(curves.master)
        val redLUT = buildCurveLUT(curves.red)
        val greenLUT = buildCurveLUT(curves.green)
        val blueLUT = buildCurveLUT(curves.blue)

        for (y in 0 until height) {
            for (x in 0 until width) {
                val pixel = result.getPixel(x, y)

                val r = Color.red(pixel)
                val g = Color.green(pixel)
                val b = Color.blue(pixel)

                // Apply master curve first
                var newR = (masterLUT[r] * 255).toInt()
                var newG = (masterLUT[g] * 255).toInt()
                var newB = (masterLUT[b] * 255).toInt()

                // Then apply individual channel curves
                newR = (redLUT[newR] * 255).toInt().coerceIn(0, 255)
                newG = (greenLUT[newG] * 255).toInt().coerceIn(0, 255)
                newB = (blueLUT[newB] * 255).toInt().coerceIn(0, 255)

                result.setPixel(x, y, Color.rgb(newR, newG, newB))
            }
        }

        return result
    }

    override fun applyLUT(bitmap: Bitmap, lutData: LUT3D): Bitmap {
        val result = bitmap.copy(bitmap.config, true)
        val width = result.width
        val height = result.height

        for (y in 0 until height) {
            for (x in 0 until width) {
                val pixel = result.getPixel(x, y)

                val r = Color.red(pixel) / 255f
                val g = Color.green(pixel) / 255f
                val b = Color.blue(pixel) / 255f

                // Sample LUT with trilinear interpolation
                val (newR, newG, newB) = lutData.sample(r, g, b)

                val finalR = (newR * 255).toInt().coerceIn(0, 255)
                val finalG = (newG * 255).toInt().coerceIn(0, 255)
                val finalB = (newB * 255).toInt().coerceIn(0, 255)

                result.setPixel(x, y, Color.rgb(finalR, finalG, finalB))
            }
        }

        return result
    }

    override fun applyHSL(
        bitmap: Bitmap,
        hueShift: Float,
        saturation: Float,
        lightness: Float
    ): Bitmap {
        val result = bitmap.copy(bitmap.config, true)
        val width = result.width
        val height = result.height

        for (y in 0 until height) {
            for (x in 0 until width) {
                val pixel = result.getPixel(x, y)

                val r = Color.red(pixel) / 255f
                val g = Color.green(pixel) / 255f
                val b = Color.blue(pixel) / 255f

                // Convert to HSL
                val (h, s, l) = ColorSpace.rgbToHSL(r, g, b)

                // Apply adjustments
                var newH = (h + hueShift) % 360f
                if (newH < 0) newH += 360f

                val newS = (s * saturation).coerceIn(0f, 1f)
                val newL = (l + lightness).coerceIn(0f, 1f)

                // Convert back to RGB
                val (newR, newG, newB) = ColorSpace.hslToRGB(newH, newS, newL)

                val finalR = (newR * 255).toInt().coerceIn(0, 255)
                val finalG = (newG * 255).toInt().coerceIn(0, 255)
                val finalB = (newB * 255).toInt().coerceIn(0, 255)

                result.setPixel(x, y, Color.rgb(finalR, finalG, finalB))
            }
        }

        return result
    }

    override fun applyTemperatureTint(
        bitmap: Bitmap,
        temperature: Float,
        tint: Float
    ): Bitmap {
        val result = bitmap.copy(bitmap.config, true)
        val width = result.width
        val height = result.height

        // Convert temperature/tint to RGB multipliers
        // Temperature: warm (red-yellow) vs cool (blue)
        // Tint: green vs magenta
        val tempScale = temperature / 100f
        val tintScale = tint / 100f

        for (y in 0 until height) {
            for (x in 0 until width) {
                val pixel = result.getPixel(x, y)

                var r = Color.red(pixel) / 255f
                var g = Color.green(pixel) / 255f
                var b = Color.blue(pixel) / 255f

                // Apply temperature (affects red-blue balance)
                if (tempScale > 0) {
                    // Warm: increase red, decrease blue
                    r = (r * (1f + tempScale * 0.3f)).coerceIn(0f, 1f)
                    b = (b * (1f - tempScale * 0.3f)).coerceIn(0f, 1f)
                } else {
                    // Cool: decrease red, increase blue
                    r = (r * (1f + tempScale * 0.3f)).coerceIn(0f, 1f)
                    b = (b * (1f - tempScale * 0.3f)).coerceIn(0f, 1f)
                }

                // Apply tint (affects green-magenta balance)
                if (tintScale > 0) {
                    // Magenta: decrease green, increase red/blue slightly
                    g = (g * (1f - abs(tintScale) * 0.2f)).coerceIn(0f, 1f)
                    r = (r * (1f + tintScale * 0.1f)).coerceIn(0f, 1f)
                    b = (b * (1f + tintScale * 0.1f)).coerceIn(0f, 1f)
                } else {
                    // Green: increase green
                    g = (g * (1f + abs(tintScale) * 0.3f)).coerceIn(0f, 1f)
                }

                val finalR = (r * 255).toInt().coerceIn(0, 255)
                val finalG = (g * 255).toInt().coerceIn(0, 255)
                val finalB = (b * 255).toInt().coerceIn(0, 255)

                result.setPixel(x, y, Color.rgb(finalR, finalG, finalB))
            }
        }

        return result
    }

    override fun applyFilmEmulation(bitmap: Bitmap, preset: FilmPreset): Bitmap {
        // Generate LUT for the film preset and apply it
        val lut = generateFilmLUT(preset)
        return applyLUT(bitmap, lut)
    }

    override fun logToLinear(bitmap: Bitmap, logFormat: LogFormat): Bitmap {
        val result = bitmap.copy(bitmap.config, true)
        val width = result.width
        val height = result.height

        for (y in 0 until height) {
            for (x in 0 until width) {
                val pixel = result.getPixel(x, y)

                val r = Color.red(pixel) / 255f
                val g = Color.green(pixel) / 255f
                val b = Color.blue(pixel) / 255f

                // Apply log-to-linear conversion based on format
                val (newR, newG, newB) = when (logFormat) {
                    LogFormat.LOG_C -> logCToLinear(r, g, b)
                    LogFormat.S_LOG3 -> sLog3ToLinear(r, g, b)
                    LogFormat.LOG_3_G10 -> log3G10ToLinear(r, g, b)
                    LogFormat.V_LOG -> vLogToLinear(r, g, b)
                    LogFormat.CINEON_LOG -> cineonLogToLinear(r, g, b)
                    LogFormat.LINEAR -> Triple(r, g, b)
                }

                val finalR = (newR * 255).toInt().coerceIn(0, 255)
                val finalG = (newG * 255).toInt().coerceIn(0, 255)
                val finalB = (newB * 255).toInt().coerceIn(0, 255)

                result.setPixel(x, y, Color.rgb(finalR, finalG, finalB))
            }
        }

        return result
    }

    override fun exportLUT(adjustments: ColorGradingAdjustments, size: Int): LUT3D {
        val data = FloatArray(size * size * size * 3)

        for (b in 0 until size) {
            for (g in 0 until size) {
                for (r in 0 until size) {
                    // Normalized RGB values
                    var nr = r.toFloat() / (size - 1)
                    var ng = g.toFloat() / (size - 1)
                    var nb = b.toFloat() / (size - 1)

                    // Apply all adjustments in order
                    // 1. Temperature/Tint (would need to implement on floats)
                    // 2. HSL
                    val (h, s, l) = ColorSpace.rgbToHSL(nr, ng, nb)
                    val newH = (h + adjustments.hsl.hueShift) % 360f
                    val newS = (s * adjustments.hsl.saturation).coerceIn(0f, 1f)
                    val newL = (l + adjustments.hsl.lightness).coerceIn(0f, 1f)

                    val (r2, g2, b2) = ColorSpace.hslToRGB(newH, newS, newL)
                    nr = r2
                    ng = g2
                    nb = b2

                    // 3. Color wheels (simplified for LUT generation)
                    // ... (would apply color wheel logic here)

                    // Store in LUT
                    val index = (b * size * size + g * size + r) * 3
                    data[index] = nr.coerceIn(0f, 1f)
                    data[index + 1] = ng.coerceIn(0f, 1f)
                    data[index + 2] = nb.coerceIn(0f, 1f)
                }
            }
        }

        return LUT3D(size, data)
    }

    // Helper functions

    private fun applyColorWheelToPixel(
        r: Float, g: Float, b: Float,
        adjustment: ColorWheelAdjustment,
        weight: Float
    ): Triple<Float, Float, Float> {
        if (weight == 0f) return Triple(r, g, b)

        // Apply lift-gamma-gain
        var nr = r
        var ng = g
        var nb = b

        // Lift (offset)
        nr = (nr + adjustment.lift * weight).coerceIn(0f, 1f)
        ng = (ng + adjustment.lift * weight).coerceIn(0f, 1f)
        nb = (nb + adjustment.lift * weight).coerceIn(0f, 1f)

        // Gamma
        val gamma = adjustment.gamma
        nr = ColorSpace.applyGamma(nr, gamma)
        ng = ColorSpace.applyGamma(ng, gamma)
        nb = ColorSpace.applyGamma(nb, gamma)

        // Gain (multiplier)
        nr = (nr * (1f + (adjustment.gain - 1f) * weight)).coerceIn(0f, 1f)
        ng = (ng * (1f + (adjustment.gain - 1f) * weight)).coerceIn(0f, 1f)
        nb = (nb * (1f + (adjustment.gain - 1f) * weight)).coerceIn(0f, 1f)

        // Apply saturation adjustment
        if (adjustment.saturation != 1f) {
            val lum = 0.299f * nr + 0.587f * ng + 0.114f * nb
            nr = lum + (nr - lum) * adjustment.saturation
            ng = lum + (ng - lum) * adjustment.saturation
            nb = lum + (nb - lum) * adjustment.saturation
        }

        return ColorSpace.clampRGB(nr, ng, nb)
    }

    private fun calculateShadowWeight(luminance: Float): Float {
        // Smooth curve for shadow range (0-0.33)
        return when {
            luminance < 0.33f -> 1f - (luminance / 0.33f)
            else -> 0f
        }
    }

    private fun calculateMidtoneWeight(luminance: Float): Float {
        // Smooth curve for midtone range (0.33-0.66)
        return when {
            luminance < 0.33f -> luminance / 0.33f
            luminance < 0.66f -> 1f
            else -> 1f - ((luminance - 0.66f) / 0.34f)
        }
    }

    private fun calculateHighlightWeight(luminance: Float): Float {
        // Smooth curve for highlight range (0.66-1.0)
        return when {
            luminance > 0.66f -> (luminance - 0.66f) / 0.34f
            else -> 0f
        }
    }

    private fun buildCurveLUT(curve: List<CurvePoint>): FloatArray {
        val lut = FloatArray(256)

        for (i in 0..255) {
            val input = i / 255f
            lut[i] = evaluateCurve(curve, input)
        }

        return lut
    }

    private fun evaluateCurve(curve: List<CurvePoint>, input: Float): Float {
        if (curve.size < 2) return input

        // Find surrounding points
        val sorted = curve.sortedBy { it.input }

        // Clamp to curve range
        if (input <= sorted.first().input) return sorted.first().output
        if (input >= sorted.last().input) return sorted.last().output

        // Find bracketing points
        for (i in 0 until sorted.size - 1) {
            val p1 = sorted[i]
            val p2 = sorted[i + 1]

            if (input >= p1.input && input <= p2.input) {
                // Linear interpolation (could use Catmull-Rom spline for smoother curves)
                val t = (input - p1.input) / (p2.input - p1.input)
                return p1.output + (p2.output - p1.output) * t
            }
        }

        return input
    }

    private fun generateFilmLUT(preset: FilmPreset): LUT3D {
        val size = 33
        val data = FloatArray(size * size * size * 3)

        for (b in 0 until size) {
            for (g in 0 until size) {
                for (r in 0 until size) {
                    val nr = r.toFloat() / (size - 1)
                    val ng = g.toFloat() / (size - 1)
                    val nb = b.toFloat() / (size - 1)

                    // Apply film-specific color transformations
                    val (fr, fg, fb) = when (preset) {
                        FilmPreset.KODAK_2383 -> applyKodak2383(nr, ng, nb)
                        FilmPreset.KODAK_VISION3_500T -> applyVision3(nr, ng, nb)
                        FilmPreset.FUJI_ETERNA -> applyFujiEterna(nr, ng, nb)
                        else -> Triple(nr, ng, nb) // Simplified for other presets
                    }

                    val index = (b * size * size + g * size + r) * 3
                    data[index] = fr.coerceIn(0f, 1f)
                    data[index + 1] = fg.coerceIn(0f, 1f)
                    data[index + 2] = fb.coerceIn(0f, 1f)
                }
            }
        }

        return LUT3D(size, data)
    }

    private fun applyKodak2383(r: Float, g: Float, b: Float): Triple<Float, Float, Float> {
        // Kodak 2383 print film characteristics
        // Warm highlights, crushed blacks, slightly desaturated
        val gamma = 0.95f
        val nr = r.pow(gamma) * 1.05f
        val ng = g.pow(gamma) * 0.98f
        val nb = b.pow(gamma) * 0.95f

        return Triple(nr, ng, nb)
    }

    private fun applyVision3(r: Float, g: Float, b: Float): Triple<Float, Float, Float> {
        // Kodak Vision3 characteristics
        // Clean, neutral, wide dynamic range
        return Triple(r, g, b * 1.02f) // Slight blue lift
    }

    private fun applyFujiEterna(r: Float, g: Float, b: Float): Triple<Float, Float, Float> {
        // Fuji Eterna characteristics
        // Muted colors, teal shadows, warm highlights
        val lum = 0.299f * r + 0.587f * g + 0.114f * b
        val sat = 0.85f // Desaturate

        var nr = lum + (r - lum) * sat
        var ng = lum + (g - lum) * sat
        var nb = lum + (b - lum) * sat

        // Teal shadows
        if (lum < 0.5f) {
            ng = ng * 1.05f
            nb = nb * 1.1f
        }

        return Triple(nr, ng, nb)
    }

    // Log-to-linear conversions

    private fun logCToLinear(r: Float, g: Float, b: Float): Triple<Float, Float, Float> {
        // ARRI LogC to linear
        fun convert(x: Float): Float {
            return if (x > 0.1496) {
                (10f.pow((x - 0.385537f) / 0.2471896f) - 0.052272f) / 5.555556f
            } else {
                (x - 0.092809f) / 5.367655f
            }
        }

        return Triple(convert(r), convert(g), convert(b))
    }

    private fun sLog3ToLinear(r: Float, g: Float, b: Float): Triple<Float, Float, Float> {
        // Sony S-Log3 to linear
        fun convert(x: Float): Float {
            return if (x >= 171.2102946929f / 1023f) {
                (10f.pow((x * 1023f - 420f) / 261.5f) - 0.01125f) / (0.9f - 0.01125f)
            } else {
                (x * 1023f - 95f) / (171.2102946929f - 95f) * 0.01125f / (0.9f - 0.01125f)
            }
        }

        return Triple(convert(r), convert(g), convert(b))
    }

    private fun log3G10ToLinear(r: Float, g: Float, b: Float): Triple<Float, Float, Float> {
        // RED Log3G10 to linear (simplified)
        fun convert(x: Float): Float {
            return (10f.pow(x) - 1f) / 1023f
        }

        return Triple(convert(r), convert(g), convert(b))
    }

    private fun vLogToLinear(r: Float, g: Float, b: Float): Triple<Float, Float, Float> {
        // Panasonic V-Log to linear (simplified)
        fun convert(x: Float): Float {
            return (10f.pow((x - 0.125f) / 0.241514f) - 1f) / 9f
        }

        return Triple(convert(r), convert(g), convert(b))
    }

    private fun cineonLogToLinear(r: Float, g: Float, b: Float): Triple<Float, Float, Float> {
        // Cineon/DPX Log to linear
        fun convert(x: Float): Float {
            return 10f.pow((x * 1023f - 685f) / 300f) * 0.18f
        }

        return Triple(convert(r), convert(g), convert(b))
    }
}
