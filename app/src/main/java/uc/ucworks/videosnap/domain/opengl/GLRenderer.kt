package uc.ucworks.videosnap.domain.opengl

import android.graphics.Bitmap
import android.opengl.GLES20
import android.opengl.GLUtils
import uc.ucworks.videosnap.util.Logger
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

/**
 * OpenGL ES 2.0 renderer for video frames
 */
class GLRenderer {

    companion object {
        private const val TAG = "GLRenderer"

        // Vertex coordinates (full screen quad)
        private val VERTEX_COORDS = floatArrayOf(
            -1.0f, -1.0f,  // Bottom left
             1.0f, -1.0f,  // Bottom right
            -1.0f,  1.0f,  // Top left
             1.0f,  1.0f   // Top right
        )

        // Texture coordinates
        private val TEXTURE_COORDS = floatArrayOf(
            0.0f, 1.0f,   // Bottom left
            1.0f, 1.0f,   // Bottom right
            0.0f, 0.0f,   // Top left
            1.0f, 0.0f    // Top right
        )

        private const val COORDS_PER_VERTEX = 2
        private const val VERTEX_STRIDE = COORDS_PER_VERTEX * 4 // 4 bytes per float
    }

    private var vertexBuffer: FloatBuffer
    private var textureBuffer: FloatBuffer

    private val textureHandles = mutableMapOf<String, Int>()
    private val framebufferHandles = mutableMapOf<String, Int>()

    init {
        // Initialize vertex buffer
        vertexBuffer = ByteBuffer.allocateDirect(VERTEX_COORDS.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .put(VERTEX_COORDS)
        vertexBuffer.position(0)

        // Initialize texture buffer
        textureBuffer = ByteBuffer.allocateDirect(TEXTURE_COORDS.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .put(TEXTURE_COORDS)
        textureBuffer.position(0)
    }

    /**
     * Initialize OpenGL context
     */
    fun initialize() {
        // Set clear color
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)

        // Enable blending for transparency
        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)

        Logger.d(TAG, "OpenGL initialized")
        checkGLError("initialize")
    }

    /**
     * Create texture from bitmap
     */
    fun createTextureFromBitmap(bitmap: Bitmap): Int {
        val textureHandle = IntArray(1)
        GLES20.glGenTextures(1, textureHandle, 0)

        if (textureHandle[0] == 0) {
            Logger.e(TAG, "Failed to generate texture")
            return 0
        }

        // Bind texture
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureHandle[0])

        // Set texture parameters
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)

        // Load bitmap into texture
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0)

        checkGLError("createTextureFromBitmap")
        return textureHandle[0]
    }

    /**
     * Create empty texture
     */
    fun createTexture(width: Int, height: Int): Int {
        val textureHandle = IntArray(1)
        GLES20.glGenTextures(1, textureHandle, 0)

        if (textureHandle[0] == 0) {
            Logger.e(TAG, "Failed to generate texture")
            return 0
        }

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureHandle[0])
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)

        GLES20.glTexImage2D(
            GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA,
            width, height, 0, GLES20.GL_RGBA,
            GLES20.GL_UNSIGNED_BYTE, null
        )

        checkGLError("createTexture")
        return textureHandle[0]
    }

    /**
     * Create framebuffer object (FBO)
     */
    fun createFramebuffer(textureHandle: Int): Int {
        val framebufferHandle = IntArray(1)
        GLES20.glGenFramebuffers(1, framebufferHandle, 0)

        if (framebufferHandle[0] == 0) {
            Logger.e(TAG, "Failed to generate framebuffer")
            return 0
        }

        // Bind framebuffer and attach texture
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, framebufferHandle[0])
        GLES20.glFramebufferTexture2D(
            GLES20.GL_FRAMEBUFFER,
            GLES20.GL_COLOR_ATTACHMENT0,
            GLES20.GL_TEXTURE_2D,
            textureHandle,
            0
        )

        // Check framebuffer status
        val status = GLES20.glCheckFramebufferStatus(GLES20.GL_FRAMEBUFFER)
        if (status != GLES20.GL_FRAMEBUFFER_COMPLETE) {
            Logger.e(TAG, "Framebuffer not complete: $status")
            return 0
        }

        // Unbind
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0)

        checkGLError("createFramebuffer")
        return framebufferHandle[0]
    }

    /**
     * Render texture with shader
     */
    fun renderTexture(
        shader: ShaderProgram,
        textureHandle: Int,
        framebufferHandle: Int = 0,
        width: Int,
        height: Int
    ) {
        // Bind framebuffer (0 = screen)
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, framebufferHandle)

        // Set viewport
        GLES20.glViewport(0, 0, width, height)

        // Clear
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)

        // Use shader
        shader.use()

        // Bind texture
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureHandle)
        shader.setUniform1i("uTexture", 0)

        // Set vertex attributes
        shader.enableVertexAttribArray("aPosition")
        vertexBuffer.position(0)
        shader.setVertexAttribPointer(
            "aPosition", COORDS_PER_VERTEX,
            GLES20.GL_FLOAT, false, VERTEX_STRIDE, vertexBuffer
        )

        shader.enableVertexAttribArray("aTexCoord")
        textureBuffer.position(0)
        shader.setVertexAttribPointer(
            "aTexCoord", COORDS_PER_VERTEX,
            GLES20.GL_FLOAT, false, VERTEX_STRIDE, textureBuffer
        )

        // Draw
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)

        // Unbind
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0)

        checkGLError("renderTexture")
    }

    /**
     * Delete texture
     */
    fun deleteTexture(textureHandle: Int) {
        val textures = intArrayOf(textureHandle)
        GLES20.glDeleteTextures(1, textures, 0)
    }

    /**
     * Delete framebuffer
     */
    fun deleteFramebuffer(framebufferHandle: Int) {
        val framebuffers = intArrayOf(framebufferHandle)
        GLES20.glDeleteFramebuffers(1, framebuffers, 0)
    }

    /**
     * Cleanup all resources
     */
    fun cleanup() {
        textureHandles.values.forEach { deleteTexture(it) }
        framebufferHandles.values.forEach { deleteFramebuffer(it) }
        textureHandles.clear()
        framebufferHandles.clear()
        Logger.d(TAG, "GLRenderer cleaned up")
    }

    /**
     * Check for OpenGL errors
     */
    private fun checkGLError(operation: String) {
        val error = GLES20.glGetError()
        if (error != GLES20.GL_NO_ERROR) {
            Logger.e(TAG, "GL error in $operation: $error")
        }
    }
}
