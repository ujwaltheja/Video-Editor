package uc.ucworks.videosnap.domain.opengl

import android.graphics.Bitmap
import uc.ucworks.videosnap.util.Logger

/**
 * Base class for GPU-accelerated effects
 */
abstract class GPUEffect {

    companion object {
        private const val TAG = "GPUEffect"
    }

    protected var shader: ShaderProgram? = null
    protected var renderer: GLRenderer? = null
    protected var isInitialized = false

    /**
     * Initialize effect with renderer
     */
    open fun initialize(renderer: GLRenderer): Result<Unit> {
        return try {
            this.renderer = renderer
            shader = createShader()
            val compileResult = shader?.compile()

            if (compileResult?.isFailure == true) {
                return Result.failure(compileResult.exceptionOrNull()!!)
            }

            isInitialized = true
            Logger.d(TAG, "${this::class.simpleName} initialized")
            Result.success(Unit)
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to initialize effect", e)
            Result.failure(e)
        }
    }

    /**
     * Create shader for this effect
     */
    abstract fun createShader(): ShaderProgram

    /**
     * Apply effect to texture
     */
    abstract fun apply(
        inputTexture: Int,
        outputFramebuffer: Int,
        width: Int,
        height: Int,
        parameters: Map<String, Any> = emptyMap()
    )

    /**
     * Cleanup resources
     */
    open fun cleanup() {
        shader?.cleanup()
        shader = null
        renderer = null
        isInitialized = false
    }
}

/**
 * Brightness/Contrast GPU effect
 */
class GPUBrightnessContrastEffect : GPUEffect() {

    override fun createShader(): ShaderProgram {
        return ShaderProgram(
            ShaderProgram.DEFAULT_VERTEX_SHADER,
            Shaders.BRIGHTNESS_CONTRAST_SHADER
        )
    }

    override fun apply(
        inputTexture: Int,
        outputFramebuffer: Int,
        width: Int,
        height: Int,
        parameters: Map<String, Any>
    ) {
        val brightness = parameters["brightness"] as? Float ?: 0f
        val contrast = parameters["contrast"] as? Float ?: 1f

        shader?.use()
        shader?.setUniform1f("uBrightness", brightness)
        shader?.setUniform1f("uContrast", contrast)

        renderer?.renderTexture(shader!!, inputTexture, outputFramebuffer, width, height)
    }
}

/**
 * Saturation GPU effect
 */
class GPUSaturationEffect : GPUEffect() {

    override fun createShader(): ShaderProgram {
        return ShaderProgram(
            ShaderProgram.DEFAULT_VERTEX_SHADER,
            Shaders.SATURATION_SHADER
        )
    }

    override fun apply(
        inputTexture: Int,
        outputFramebuffer: Int,
        width: Int,
        height: Int,
        parameters: Map<String, Any>
    ) {
        val saturation = parameters["saturation"] as? Float ?: 1f

        shader?.use()
        shader?.setUniform1f("uSaturation", saturation)

        renderer?.renderTexture(shader!!, inputTexture, outputFramebuffer, width, height)
    }
}

/**
 * Hue GPU effect
 */
class GPUHueEffect : GPUEffect() {

    override fun createShader(): ShaderProgram {
        return ShaderProgram(
            ShaderProgram.DEFAULT_VERTEX_SHADER,
            Shaders.HUE_SHADER
        )
    }

    override fun apply(
        inputTexture: Int,
        outputFramebuffer: Int,
        width: Int,
        height: Int,
        parameters: Map<String, Any>
    ) {
        val hue = parameters["hue"] as? Float ?: 0f

        shader?.use()
        shader?.setUniform1f("uHue", hue)

        renderer?.renderTexture(shader!!, inputTexture, outputFramebuffer, width, height)
    }
}

/**
 * Blur GPU effect
 */
class GPUBlurEffect : GPUEffect() {

    override fun createShader(): ShaderProgram {
        return ShaderProgram(
            ShaderProgram.DEFAULT_VERTEX_SHADER,
            Shaders.BLUR_SHADER
        )
    }

    override fun apply(
        inputTexture: Int,
        outputFramebuffer: Int,
        width: Int,
        height: Int,
        parameters: Map<String, Any>
    ) {
        val blurSize = parameters["blurSize"] as? Float ?: 1f

        shader?.use()
        shader?.setUniform2f("uResolution", width.toFloat(), height.toFloat())
        shader?.setUniform1f("uBlurSize", blurSize)

        renderer?.renderTexture(shader!!, inputTexture, outputFramebuffer, width, height)
    }
}

/**
 * Grayscale GPU effect
 */
class GPUGrayscaleEffect : GPUEffect() {

    override fun createShader(): ShaderProgram {
        return ShaderProgram(
            ShaderProgram.DEFAULT_VERTEX_SHADER,
            Shaders.GRAYSCALE_SHADER
        )
    }

    override fun apply(
        inputTexture: Int,
        outputFramebuffer: Int,
        width: Int,
        height: Int,
        parameters: Map<String, Any>
    ) {
        shader?.use()
        renderer?.renderTexture(shader!!, inputTexture, outputFramebuffer, width, height)
    }
}

/**
 * Sepia GPU effect
 */
class GPUSepiaEffect : GPUEffect() {

    override fun createShader(): ShaderProgram {
        return ShaderProgram(
            ShaderProgram.DEFAULT_VERTEX_SHADER,
            Shaders.SEPIA_SHADER
        )
    }

    override fun apply(
        inputTexture: Int,
        outputFramebuffer: Int,
        width: Int,
        height: Int,
        parameters: Map<String, Any>
    ) {
        shader?.use()
        renderer?.renderTexture(shader!!, inputTexture, outputFramebuffer, width, height)
    }
}

/**
 * Vignette GPU effect
 */
class GPUVignetteEffect : GPUEffect() {

    override fun createShader(): ShaderProgram {
        return ShaderProgram(
            ShaderProgram.DEFAULT_VERTEX_SHADER,
            Shaders.VIGNETTE_SHADER
        )
    }

    override fun apply(
        inputTexture: Int,
        outputFramebuffer: Int,
        width: Int,
        height: Int,
        parameters: Map<String, Any>
    ) {
        val intensity = parameters["intensity"] as? Float ?: 0.5f

        shader?.use()
        shader?.setUniform1f("uIntensity", intensity)

        renderer?.renderTexture(shader!!, inputTexture, outputFramebuffer, width, height)
    }
}

/**
 * Sharpen GPU effect
 */
class GPUSharpenEffect : GPUEffect() {

    override fun createShader(): ShaderProgram {
        return ShaderProgram(
            ShaderProgram.DEFAULT_VERTEX_SHADER,
            Shaders.SHARPEN_SHADER
        )
    }

    override fun apply(
        inputTexture: Int,
        outputFramebuffer: Int,
        width: Int,
        height: Int,
        parameters: Map<String, Any>
    ) {
        val amount = parameters["amount"] as? Float ?: 1f

        shader?.use()
        shader?.setUniform2f("uResolution", width.toFloat(), height.toFloat())
        shader?.setUniform1f("uAmount", amount)

        renderer?.renderTexture(shader!!, inputTexture, outputFramebuffer, width, height)
    }
}

/**
 * Temperature GPU effect
 */
class GPUTemperatureEffect : GPUEffect() {

    override fun createShader(): ShaderProgram {
        return ShaderProgram(
            ShaderProgram.DEFAULT_VERTEX_SHADER,
            Shaders.TEMPERATURE_SHADER
        )
    }

    override fun apply(
        inputTexture: Int,
        outputFramebuffer: Int,
        width: Int,
        height: Int,
        parameters: Map<String, Any>
    ) {
        val temperature = parameters["temperature"] as? Float ?: 0f

        shader?.use()
        shader?.setUniform1f("uTemperature", temperature)

        renderer?.renderTexture(shader!!, inputTexture, outputFramebuffer, width, height)
    }
}

/**
 * Chroma Key (Green Screen) GPU effect
 */
class GPUChromaKeyEffect : GPUEffect() {

    override fun createShader(): ShaderProgram {
        return ShaderProgram(
            ShaderProgram.DEFAULT_VERTEX_SHADER,
            Shaders.CHROMA_KEY_SHADER
        )
    }

    override fun apply(
        inputTexture: Int,
        outputFramebuffer: Int,
        width: Int,
        height: Int,
        parameters: Map<String, Any>
    ) {
        val keyColor = parameters["keyColor"] as? FloatArray ?: floatArrayOf(0f, 1f, 0f) // Default green
        val threshold = parameters["threshold"] as? Float ?: 0.4f
        val smoothness = parameters["smoothness"] as? Float ?: 0.1f

        shader?.use()
        shader?.setUniform3f("uKeyColor", keyColor[0], keyColor[1], keyColor[2])
        shader?.setUniform1f("uThreshold", threshold)
        shader?.setUniform1f("uSmoothness", smoothness)

        renderer?.renderTexture(shader!!, inputTexture, outputFramebuffer, width, height)
    }
}

/**
 * Color Overlay GPU effect
 */
class GPUColorOverlayEffect : GPUEffect() {

    override fun createShader(): ShaderProgram {
        return ShaderProgram(
            ShaderProgram.DEFAULT_VERTEX_SHADER,
            Shaders.COLOR_OVERLAY_SHADER
        )
    }

    override fun apply(
        inputTexture: Int,
        outputFramebuffer: Int,
        width: Int,
        height: Int,
        parameters: Map<String, Any>
    ) {
        val overlayColor = parameters["overlayColor"] as? FloatArray ?: floatArrayOf(1f, 1f, 1f, 1f)
        val intensity = parameters["intensity"] as? Float ?: 0.5f

        shader?.use()
        shader?.setUniform4f("uOverlayColor", overlayColor[0], overlayColor[1], overlayColor[2], overlayColor[3])
        shader?.setUniform1f("uIntensity", intensity)

        renderer?.renderTexture(shader!!, inputTexture, outputFramebuffer, width, height)
    }
}
