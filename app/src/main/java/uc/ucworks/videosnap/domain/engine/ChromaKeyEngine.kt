package uc.ucworks.videosnap.domain.engine

import android.graphics.Bitmap
import android.graphics.Color
import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Professional chroma key (green screen) engine
 * Supports multiple keying algorithms and advanced spill suppression
 */
interface ChromaKeyEngine {
    /**
     * Apply chroma key to remove background
     */
    fun applyChromaKey(
        bitmap: Bitmap,
        settings: ChromaKeySettings
    ): Bitmap

    /**
     * Generate transparency matte for preview
     */
    fun generateMatte(
        bitmap: Bitmap,
        settings: ChromaKeySettings
    ): Bitmap

    /**
     * Auto-detect best key color from sample area
     */
    fun detectKeyColor(
        bitmap: Bitmap,
        sampleX: Int,
        sampleY: Int,
        sampleRadius: Int = 10
    ): Int

    /**
     * Apply spill suppression to remove color cast
     */
    fun suppressSpill(
        bitmap: Bitmap,
        keyColor: Int,
        amount: Float
    ): Bitmap
}

/**
 * Chroma key settings
 */
data class ChromaKeySettings(
    // Key color
    val keyColor: Int = 0xFF00FF00.toInt(), // Green by default

    // Keying algorithm
    val algorithm: KeyingAlgorithm = KeyingAlgorithm.COLOR_DIFFERENCE,

    // Tolerance settings
    val threshold: Float = 0.3f,        // Main threshold (0-1)
    val tolerance: Float = 0.2f,        // Edge tolerance (0-1)
    val softness: Float = 0.1f,         // Edge softness (0-1)

    // Advanced settings
    val spillSuppression: Float = 0.5f, // Spill removal strength (0-1)
    val edgeBlur: Float = 0f,           // Edge blur radius in pixels
    val lightWrap: Float = 0f,          // Light wrap amount (0-1)

    // Color correction on foreground
    val despill: Boolean = true,         // Remove color spill from foreground
    val preserveLuminance: Boolean = true,

    // Screen type presets
    val screenType: ScreenType = ScreenType.GREEN_SCREEN
)

/**
 * Keying algorithms
 */
enum class KeyingAlgorithm {
    /**
     * Simple color distance in RGB space
     * Fast but less accurate
     */
    COLOR_DISTANCE,

    /**
     * Color difference keying (industry standard)
     * Good balance of quality and speed
     */
    COLOR_DIFFERENCE,

    /**
     * HSL-based keying
     * Better for uneven lighting
     */
    HSL_KEY,

    /**
     * Luma key (brightness-based)
     * Used for keying based on brightness
     */
    LUMA_KEY,

    /**
     * Advanced edge-aware keying
     * Best quality but slower
     */
    ADVANCED_EDGE
}

/**
 * Screen type presets
 */
enum class ScreenType {
    GREEN_SCREEN,   // #00FF00 green
    BLUE_SCREEN,    // #0000FF blue
    RED_SCREEN,     // #FF0000 red (rare)
    CUSTOM          // User-defined color
}

/**
 * Implementation of chroma key engine
 */
class ChromaKeyEngineImpl @Inject constructor() : ChromaKeyEngine {

    override fun applyChromaKey(
        bitmap: Bitmap,
        settings: ChromaKeySettings
    ): Bitmap {
        val result = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val width = result.width
        val height = result.height

        // Extract key color components
        val keyR = Color.red(settings.keyColor) / 255f
        val keyG = Color.green(settings.keyColor) / 255f
        val keyB = Color.blue(settings.keyColor) / 255f

        for (y in 0 until height) {
            for (x in 0 until width) {
                val pixel = result.getPixel(x, y)
                val r = Color.red(pixel) / 255f
                val g = Color.green(pixel) / 255f
                val b = Color.blue(pixel) / 255f

                // Calculate alpha based on algorithm
                val alpha = when (settings.algorithm) {
                    KeyingAlgorithm.COLOR_DISTANCE -> calculateColorDistance(r, g, b, keyR, keyG, keyB, settings)
                    KeyingAlgorithm.COLOR_DIFFERENCE -> calculateColorDifference(r, g, b, keyR, keyG, keyB, settings)
                    KeyingAlgorithm.HSL_KEY -> calculateHSLKey(r, g, b, keyR, keyG, keyB, settings)
                    KeyingAlgorithm.LUMA_KEY -> calculateLumaKey(r, g, b, settings)
                    KeyingAlgorithm.ADVANCED_EDGE -> calculateAdvancedKey(r, g, b, keyR, keyG, keyB, settings, x, y, width, height, result)
                }

                // Apply despill if enabled
                val (finalR, finalG, finalB) = if (settings.despill && alpha > 0.1f) {
                    despillColor(r, g, b, keyR, keyG, keyB, settings.spillSuppression)
                } else {
                    Triple(r, g, b)
                }

                // Set pixel with new alpha
                val newR = (finalR * 255).toInt().coerceIn(0, 255)
                val newG = (finalG * 255).toInt().coerceIn(0, 255)
                val newB = (finalB * 255).toInt().coerceIn(0, 255)
                val newA = (alpha * 255).toInt().coerceIn(0, 255)

                result.setPixel(x, y, Color.argb(newA, newR, newG, newB))
            }
        }

        // Apply edge blur if specified
        return if (settings.edgeBlur > 0) {
            applyEdgeBlur(result, settings.edgeBlur)
        } else {
            result
        }
    }

    override fun generateMatte(bitmap: Bitmap, settings: ChromaKeySettings): Bitmap {
        val matte = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val width = matte.width
        val height = matte.height

        val keyR = Color.red(settings.keyColor) / 255f
        val keyG = Color.green(settings.keyColor) / 255f
        val keyB = Color.blue(settings.keyColor) / 255f

        for (y in 0 until height) {
            for (x in 0 until width) {
                val pixel = matte.getPixel(x, y)
                val r = Color.red(pixel) / 255f
                val g = Color.green(pixel) / 255f
                val b = Color.blue(pixel) / 255f

                val alpha = calculateColorDifference(r, g, b, keyR, keyG, keyB, settings)

                // Show matte as grayscale
                val gray = (alpha * 255).toInt()
                matte.setPixel(x, y, Color.rgb(gray, gray, gray))
            }
        }

        return matte
    }

    override fun detectKeyColor(bitmap: Bitmap, sampleX: Int, sampleY: Int, sampleRadius: Int): Int {
        var totalR = 0L
        var totalG = 0L
        var totalB = 0L
        var count = 0

        val startX = max(0, sampleX - sampleRadius)
        val endX = min(bitmap.width - 1, sampleX + sampleRadius)
        val startY = max(0, sampleY - sampleRadius)
        val endY = min(bitmap.height - 1, sampleY + sampleRadius)

        for (y in startY..endY) {
            for (x in startX..endX) {
                val pixel = bitmap.getPixel(x, y)
                totalR += Color.red(pixel)
                totalG += Color.green(pixel)
                totalB += Color.blue(pixel)
                count++
            }
        }

        val avgR = (totalR / count).toInt()
        val avgG = (totalG / count).toInt()
        val avgB = (totalB / count).toInt()

        return Color.rgb(avgR, avgG, avgB)
    }

    override fun suppressSpill(bitmap: Bitmap, keyColor: Int, amount: Float): Bitmap {
        val result = bitmap.copy(bitmap.config, true)
        val width = result.width
        val height = result.height

        val keyR = Color.red(keyColor) / 255f
        val keyG = Color.green(keyColor) / 255f
        val keyB = Color.blue(keyColor) / 255f

        for (y in 0 until height) {
            for (x in 0 until width) {
                val pixel = result.getPixel(x, y)
                val r = Color.red(pixel) / 255f
                val g = Color.green(pixel) / 255f
                val b = Color.blue(pixel) / 255f
                val a = Color.alpha(pixel)

                val (newR, newG, newB) = despillColor(r, g, b, keyR, keyG, keyB, amount)

                result.setPixel(x, y, Color.argb(a,
                    (newR * 255).toInt().coerceIn(0, 255),
                    (newG * 255).toInt().coerceIn(0, 255),
                    (newB * 255).toInt().coerceIn(0, 255)
                ))
            }
        }

        return result
    }

    // Private helper methods

    private fun calculateColorDistance(
        r: Float, g: Float, b: Float,
        keyR: Float, keyG: Float, keyB: Float,
        settings: ChromaKeySettings
    ): Float {
        val distance = sqrt(
            (r - keyR) * (r - keyR) +
            (g - keyG) * (g - keyG) +
            (b - keyB) * (b - keyB)
        )

        return smoothstep(settings.threshold, settings.threshold + settings.tolerance, distance)
    }

    private fun calculateColorDifference(
        r: Float, g: Float, b: Float,
        keyR: Float, keyG: Float, keyB: Float,
        settings: ChromaKeySettings
    ): Float {
        // Industry-standard color difference keying
        // More accurate than simple distance

        val colorDiff = when (settings.screenType) {
            ScreenType.GREEN_SCREEN -> g - max(r, b)
            ScreenType.BLUE_SCREEN -> b - max(r, g)
            ScreenType.RED_SCREEN -> r - max(g, b)
            ScreenType.CUSTOM -> {
                // Use brightest channel of key color
                when {
                    keyG > keyR && keyG > keyB -> g - max(r, b)
                    keyB > keyR && keyB > keyG -> b - max(r, g)
                    else -> r - max(g, b)
                }
            }
        }

        val normalized = colorDiff / (1f - settings.threshold)
        return smoothstep(0f, settings.tolerance, normalized).coerceIn(0f, 1f)
    }

    private fun calculateHSLKey(
        r: Float, g: Float, b: Float,
        keyR: Float, keyG: Float, keyB: Float,
        settings: ChromaKeySettings
    ): Float {
        val (h, s, l) = ColorSpace.rgbToHSL(r, g, b)
        val (keyH, keyS, keyL) = ColorSpace.rgbToHSL(keyR, keyG, keyB)

        // Hue difference (circular)
        val hueDiff = min(abs(h - keyH), 360f - abs(h - keyH))
        val satDiff = abs(s - keyS)

        val diff = sqrt(hueDiff * hueDiff + satDiff * satDiff * 100f) / 100f

        return smoothstep(settings.threshold, settings.threshold + settings.tolerance, diff)
    }

    private fun calculateLumaKey(
        r: Float, g: Float, b: Float,
        settings: ChromaKeySettings
    ): Float {
        val luma = 0.299f * r + 0.587f * g + 0.114f * b

        return if (luma < settings.threshold) {
            0f // Transparent
        } else if (luma < settings.threshold + settings.tolerance) {
            smoothstep(settings.threshold, settings.threshold + settings.tolerance, luma)
        } else {
            1f // Opaque
        }
    }

    private fun calculateAdvancedKey(
        r: Float, g: Float, b: Float,
        keyR: Float, keyG: Float, keyB: Float,
        settings: ChromaKeySettings,
        x: Int, y: Int,
        width: Int, height: Int,
        bitmap: Bitmap
    ): Float {
        // Advanced edge-aware keying
        // Combines color difference with edge detection

        val baseAlpha = calculateColorDifference(r, g, b, keyR, keyG, keyB, settings)

        // Edge detection (simple Sobel)
        if (x > 0 && x < width - 1 && y > 0 && y < height - 1) {
            val neighbors = arrayOf(
                bitmap.getPixel(x - 1, y - 1), bitmap.getPixel(x, y - 1), bitmap.getPixel(x + 1, y - 1),
                bitmap.getPixel(x - 1, y), bitmap.getPixel(x + 1, y),
                bitmap.getPixel(x - 1, y + 1), bitmap.getPixel(x, y + 1), bitmap.getPixel(x + 1, y + 1)
            )

            val avgR = neighbors.map { Color.red(it) }.average().toFloat() / 255f
            val avgG = neighbors.map { Color.green(it) }.average().toFloat() / 255f
            val avgB = neighbors.map { Color.blue(it) }.average().toFloat() / 255f

            val edgeStrength = sqrt(
                (r - avgR) * (r - avgR) +
                (g - avgG) * (g - avgG) +
                (b - avgB) * (b - avgB)
            )

            // Increase softness at edges
            return if (edgeStrength > 0.1f) {
                baseAlpha * (1f - edgeStrength * settings.softness)
            } else {
                baseAlpha
            }
        }

        return baseAlpha
    }

    private fun despillColor(
        r: Float, g: Float, b: Float,
        keyR: Float, keyG: Float, keyB: Float,
        amount: Float
    ): Triple<Float, Float, Float> {
        // Remove color spill from foreground

        val luma = 0.299f * r + 0.587f * g + 0.114f * b

        var newR = r
        var newG = g
        var newB = b

        // Suppress the key color channel
        when {
            keyG > keyR && keyG > keyB -> {
                // Green screen - suppress green
                val spill = max(0f, g - max(r, b))
                newG = g - spill * amount
            }
            keyB > keyR && keyB > keyG -> {
                // Blue screen - suppress blue
                val spill = max(0f, b - max(r, g))
                newB = b - spill * amount
            }
            else -> {
                // Red screen - suppress red
                val spill = max(0f, r - max(g, b))
                newR = r - spill * amount
            }
        }

        return Triple(newR, newG, newB)
    }

    private fun applyEdgeBlur(bitmap: Bitmap, radius: Float): Bitmap {
        // Simple box blur on alpha channel only
        val result = bitmap.copy(bitmap.config, true)
        val width = result.width
        val height = result.height
        val r = radius.toInt()

        for (y in 0 until height) {
            for (x in 0 until width) {
                var totalAlpha = 0
                var count = 0

                for (dy in -r..r) {
                    for (dx in -r..r) {
                        val nx = (x + dx).coerceIn(0, width - 1)
                        val ny = (y + dy).coerceIn(0, height - 1)

                        totalAlpha += Color.alpha(bitmap.getPixel(nx, ny))
                        count++
                    }
                }

                val avgAlpha = totalAlpha / count
                val pixel = result.getPixel(x, y)

                result.setPixel(x, y, Color.argb(
                    avgAlpha,
                    Color.red(pixel),
                    Color.green(pixel),
                    Color.blue(pixel)
                ))
            }
        }

        return result
    }

    private fun smoothstep(edge0: Float, edge1: Float, x: Float): Float {
        val t = ((x - edge0) / (edge1 - edge0)).coerceIn(0f, 1f)
        return t * t * (3f - 2f * t)
    }
}
