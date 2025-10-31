package uc.ucworks.videosnap.domain.engine

import android.content.Context
import android.graphics.*
import androidx.core.graphics.createBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import uc.ucworks.videosnap.EffectType
import uc.ucworks.videosnap.VideoEffect
import javax.inject.Inject
import kotlin.math.max
import kotlin.math.min

class EffectsEngineImpl @Inject constructor(
    private val context: Context
) : EffectsEngine {

    override suspend fun applyEffect(frame: Bitmap, effect: VideoEffect): Bitmap =
        withContext(Dispatchers.Default) {
            if (!effect.isEnabled) return@withContext frame

            when (effect.type) {
                EffectType.BRIGHTNESS -> applyBrightness(frame, effect)
                EffectType.CONTRAST -> applyContrast(frame, effect)
                EffectType.SATURATION -> applySaturation(frame, effect)
                EffectType.GRAYSCALE -> applyGrayscale(frame)
                EffectType.SEPIA -> applySepia(frame)
                EffectType.GAUSSIAN_BLUR -> applyGaussianBlur(frame, effect)
                EffectType.INVERT -> applyInvert(frame)
                EffectType.VIGNETTE -> applyVignette(frame, effect)
                EffectType.SHARPEN -> applySharpen(frame)
                EffectType.PIXELATE -> applyPixelate(frame, effect)
                else -> frame // Return original if effect not implemented
            }
        }

    override suspend fun applyEffects(frame: Bitmap, effects: List<VideoEffect>): Bitmap =
        withContext(Dispatchers.Default) {
            var result = frame
            effects.filter { it.isEnabled }.forEach { effect ->
                result = applyEffect(result, effect)
            }
            result
        }

    override fun requiresGPU(effect: VideoEffect): Boolean {
        return when (effect.type) {
            EffectType.GAUSSIAN_BLUR,
            EffectType.MOTION_BLUR,
            EffectType.CHROMA_KEY,
            EffectType.STABILIZATION -> true
            else -> false
        }
    }

    override suspend fun getEffectPreview(frame: Bitmap, effect: VideoEffect): Bitmap =
        withContext(Dispatchers.Default) {
            // Create smaller preview for faster processing
            val previewWidth = 320
            val previewHeight = (frame.height * (previewWidth.toFloat() / frame.width)).toInt()
            val preview = Bitmap.createScaledBitmap(frame, previewWidth, previewHeight, true)
            applyEffect(preview, effect)
        }

    // Individual effect implementations

    private fun applyBrightness(bitmap: Bitmap, effect: VideoEffect): Bitmap {
        val brightness = effect.parameters["brightness"] ?: 0f // -100 to 100
        val output = createBitmap(bitmap.width, bitmap.height)
        val canvas = Canvas(output)
        val paint = Paint()
        val cm = ColorMatrix(floatArrayOf(
            1f, 0f, 0f, 0f, brightness,
            0f, 1f, 0f, 0f, brightness,
            0f, 0f, 1f, 0f, brightness,
            0f, 0f, 0f, 1f, 0f
        ))
        paint.colorFilter = ColorMatrixColorFilter(cm)
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        return output
    }

    private fun applyContrast(bitmap: Bitmap, effect: VideoEffect): Bitmap {
        val contrast = (effect.parameters["contrast"] ?: 0f) / 100f + 1f // 0 to 2
        val output = createBitmap(bitmap.width, bitmap.height)
        val canvas = Canvas(output)
        val paint = Paint()
        val translate = (-.5f * contrast + .5f) * 255f
        val cm = ColorMatrix(floatArrayOf(
            contrast, 0f, 0f, 0f, translate,
            0f, contrast, 0f, 0f, translate,
            0f, 0f, contrast, 0f, translate,
            0f, 0f, 0f, 1f, 0f
        ))
        paint.colorFilter = ColorMatrixColorFilter(cm)
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        return output
    }

    private fun applySaturation(bitmap: Bitmap, effect: VideoEffect): Bitmap {
        val saturation = (effect.parameters["saturation"] ?: 0f) / 100f + 1f // 0 to 2
        val output = createBitmap(bitmap.width, bitmap.height)
        val canvas = Canvas(output)
        val paint = Paint()
        val cm = ColorMatrix()
        cm.setSaturation(saturation)
        paint.colorFilter = ColorMatrixColorFilter(cm)
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        return output
    }

    private fun applyGrayscale(bitmap: Bitmap): Bitmap {
        val output = createBitmap(bitmap.width, bitmap.height)
        val canvas = Canvas(output)
        val paint = Paint()
        val cm = ColorMatrix()
        cm.setSaturation(0f)
        paint.colorFilter = ColorMatrixColorFilter(cm)
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        return output
    }

    private fun applySepia(bitmap: Bitmap): Bitmap {
        val output = createBitmap(bitmap.width, bitmap.height)
        val canvas = Canvas(output)
        val paint = Paint()
        val cm = ColorMatrix()
        cm.setScale(1f, 1f, 1f, 1f)
        val sepia = ColorMatrix(floatArrayOf(
            0.393f, 0.769f, 0.189f, 0f, 0f,
            0.349f, 0.686f, 0.168f, 0f, 0f,
            0.272f, 0.534f, 0.131f, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        ))
        cm.postConcat(sepia)
        paint.colorFilter = ColorMatrixColorFilter(cm)
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        return output
    }

    private fun applyGaussianBlur(bitmap: Bitmap, effect: VideoEffect): Bitmap {
        val radius = effect.parameters["radius"]?.toInt() ?: 10
        // Simple box blur approximation (real Gaussian blur would use RenderScript or OpenGL)
        return applyBoxBlur(bitmap, radius)
    }

    private fun applyBoxBlur(bitmap: Bitmap, radius: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val output = createBitmap(width, height)
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        val blurred = IntArray(width * height)
        val r = radius.coerceIn(1, 50)

        for (y in 0 until height) {
            for (x in 0 until width) {
                var totalR = 0
                var totalG = 0
                var totalB = 0
                var count = 0

                for (dy in -r..r) {
                    for (dx in -r..r) {
                        val nx = (x + dx).coerceIn(0, width - 1)
                        val ny = (y + dy).coerceIn(0, height - 1)
                        val pixel = pixels[ny * width + nx]
                        totalR += Color.red(pixel)
                        totalG += Color.green(pixel)
                        totalB += Color.blue(pixel)
                        count++
                    }
                }

                blurred[y * width + x] = Color.rgb(
                    totalR / count,
                    totalG / count,
                    totalB / count
                )
            }
        }

        output.setPixels(blurred, 0, width, 0, 0, width, height)
        return output
    }

    private fun applyInvert(bitmap: Bitmap): Bitmap {
        val output = createBitmap(bitmap.width, bitmap.height)
        val canvas = Canvas(output)
        val paint = Paint()
        val cm = ColorMatrix(floatArrayOf(
            -1f, 0f, 0f, 0f, 255f,
            0f, -1f, 0f, 0f, 255f,
            0f, 0f, -1f, 0f, 255f,
            0f, 0f, 0f, 1f, 0f
        ))
        paint.colorFilter = ColorMatrixColorFilter(cm)
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        return output
    }

    private fun applyVignette(bitmap: Bitmap, effect: VideoEffect): Bitmap {
        val strength = effect.parameters["strength"] ?: 0.5f
        val output = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(output)
        val centerX = bitmap.width / 2f
        val centerY = bitmap.height / 2f
        val radius = Math.sqrt((centerX * centerX + centerY * centerY).toDouble()).toFloat()

        val gradient = RadialGradient(
            centerX, centerY, radius,
            intArrayOf(Color.TRANSPARENT, Color.BLACK),
            floatArrayOf(0.5f, 1f),
            Shader.TileMode.CLAMP
        )

        val paint = Paint()
        paint.shader = gradient
        paint.alpha = (strength * 255).toInt()
        canvas.drawRect(0f, 0f, bitmap.width.toFloat(), bitmap.height.toFloat(), paint)
        return output
    }

    private fun applySharpen(bitmap: Bitmap): Bitmap {
        // Simple sharpening using convolution kernel
        val output = createBitmap(bitmap.width, bitmap.height)
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        val kernel = arrayOf(
            intArrayOf(0, -1, 0),
            intArrayOf(-1, 5, -1),
            intArrayOf(0, -1, 0)
        )

        val sharpened = IntArray(width * height)

        for (y in 1 until height - 1) {
            for (x in 1 until width - 1) {
                var r = 0
                var g = 0
                var b = 0

                for (ky in -1..1) {
                    for (kx in -1..1) {
                        val pixel = pixels[(y + ky) * width + (x + kx)]
                        val weight = kernel[ky + 1][kx + 1]
                        r += Color.red(pixel) * weight
                        g += Color.green(pixel) * weight
                        b += Color.blue(pixel) * weight
                    }
                }

                sharpened[y * width + x] = Color.rgb(
                    r.coerceIn(0, 255),
                    g.coerceIn(0, 255),
                    b.coerceIn(0, 255)
                )
            }
        }

        output.setPixels(sharpened, 0, width, 0, 0, width, height)
        return output
    }

    private fun applyPixelate(bitmap: Bitmap, effect: VideoEffect): Bitmap {
        val pixelSize = effect.parameters["pixelSize"]?.toInt() ?: 10
        val width = bitmap.width
        val height = bitmap.height
        val output = createBitmap(width, height)
        val canvas = Canvas(output)

        for (y in 0 until height step pixelSize) {
            for (x in 0 until width step pixelSize) {
                val pixel = bitmap.getPixel(x, y)
                val paint = Paint().apply { color = pixel }
                canvas.drawRect(
                    x.toFloat(),
                    y.toFloat(),
                    min(x + pixelSize, width).toFloat(),
                    min(y + pixelSize, height).toFloat(),
                    paint
                )
            }
        }

        return output
    }
}
