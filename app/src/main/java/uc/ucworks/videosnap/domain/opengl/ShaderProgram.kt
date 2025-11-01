package uc.ucworks.videosnap.domain.opengl

import android.opengl.GLES20
import uc.ucworks.videosnap.util.Logger
import java.nio.FloatBuffer

/**
 * OpenGL shader program wrapper
 */
class ShaderProgram(
    private val vertexShaderCode: String,
    private val fragmentShaderCode: String
) {
    companion object {
        private const val TAG = "ShaderProgram"

        // Standard vertex shader for texture rendering
        const val DEFAULT_VERTEX_SHADER = """
            attribute vec4 aPosition;
            attribute vec2 aTexCoord;
            varying vec2 vTexCoord;

            void main() {
                gl_Position = aPosition;
                vTexCoord = aTexCoord;
            }
        """

        // Passthrough fragment shader
        const val PASSTHROUGH_FRAGMENT_SHADER = """
            precision mediump float;
            varying vec2 vTexCoord;
            uniform sampler2D uTexture;

            void main() {
                gl_FragColor = texture2D(uTexture, vTexCoord);
            }
        """
    }

    private var programHandle = 0
    private var vertexShaderHandle = 0
    private var fragmentShaderHandle = 0

    private val attributeLocations = mutableMapOf<String, Int>()
    private val uniformLocations = mutableMapOf<String, Int>()

    var isCompiled = false
        private set

    /**
     * Compile and link shader program
     */
    fun compile(): Result<Unit> {
        return try {
            // Compile vertex shader
            vertexShaderHandle = compileShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode)
            if (vertexShaderHandle == 0) {
                return Result.failure(RuntimeException("Failed to compile vertex shader"))
            }

            // Compile fragment shader
            fragmentShaderHandle = compileShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode)
            if (fragmentShaderHandle == 0) {
                GLES20.glDeleteShader(vertexShaderHandle)
                return Result.failure(RuntimeException("Failed to compile fragment shader"))
            }

            // Create program
            programHandle = GLES20.glCreateProgram()
            if (programHandle == 0) {
                cleanup()
                return Result.failure(RuntimeException("Failed to create program"))
            }

            // Attach shaders
            GLES20.glAttachShader(programHandle, vertexShaderHandle)
            GLES20.glAttachShader(programHandle, fragmentShaderHandle)

            // Link program
            GLES20.glLinkProgram(programHandle)

            // Check link status
            val linkStatus = IntArray(1)
            GLES20.glGetProgramiv(programHandle, GLES20.GL_LINK_STATUS, linkStatus, 0)
            if (linkStatus[0] == 0) {
                val log = GLES20.glGetProgramInfoLog(programHandle)
                cleanup()
                return Result.failure(RuntimeException("Failed to link program: $log"))
            }

            isCompiled = true
            Logger.d(TAG, "Shader program compiled successfully")
            Result.success(Unit)
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to compile shader", e)
            cleanup()
            Result.failure(e)
        }
    }

    /**
     * Use this shader program
     */
    fun use() {
        if (!isCompiled) {
            Logger.w(TAG, "Attempting to use uncompiled shader")
            return
        }
        GLES20.glUseProgram(programHandle)
    }

    /**
     * Get attribute location
     */
    fun getAttributeLocation(name: String): Int {
        return attributeLocations.getOrPut(name) {
            val location = GLES20.glGetAttribLocation(programHandle, name)
            if (location == -1) {
                Logger.w(TAG, "Attribute not found: $name")
            }
            location
        }
    }

    /**
     * Get uniform location
     */
    fun getUniformLocation(name: String): Int {
        return uniformLocations.getOrPut(name) {
            val location = GLES20.glGetUniformLocation(programHandle, name)
            if (location == -1) {
                Logger.w(TAG, "Uniform not found: $name")
            }
            location
        }
    }

    /**
     * Set uniform float value
     */
    fun setUniform1f(name: String, value: Float) {
        val location = getUniformLocation(name)
        if (location != -1) {
            GLES20.glUniform1f(location, value)
        }
    }

    /**
     * Set uniform int value
     */
    fun setUniform1i(name: String, value: Int) {
        val location = getUniformLocation(name)
        if (location != -1) {
            GLES20.glUniform1i(location, value)
        }
    }

    /**
     * Set uniform vec2
     */
    fun setUniform2f(name: String, x: Float, y: Float) {
        val location = getUniformLocation(name)
        if (location != -1) {
            GLES20.glUniform2f(location, x, y)
        }
    }

    /**
     * Set uniform vec3
     */
    fun setUniform3f(name: String, x: Float, y: Float, z: Float) {
        val location = getUniformLocation(name)
        if (location != -1) {
            GLES20.glUniform3f(location, x, y, z)
        }
    }

    /**
     * Set uniform vec4
     */
    fun setUniform4f(name: String, x: Float, y: Float, z: Float, w: Float) {
        val location = getUniformLocation(name)
        if (location != -1) {
            GLES20.glUniform4f(location, x, y, z, w)
        }
    }

    /**
     * Enable vertex attribute array
     */
    fun enableVertexAttribArray(name: String) {
        val location = getAttributeLocation(name)
        if (location != -1) {
            GLES20.glEnableVertexAttribArray(location)
        }
    }

    /**
     * Set vertex attribute pointer
     */
    fun setVertexAttribPointer(
        name: String,
        size: Int,
        type: Int,
        normalized: Boolean,
        stride: Int,
        buffer: FloatBuffer
    ) {
        val location = getAttributeLocation(name)
        if (location != -1) {
            GLES20.glVertexAttribPointer(location, size, type, normalized, stride, buffer)
        }
    }

    /**
     * Cleanup resources
     */
    fun cleanup() {
        if (vertexShaderHandle != 0) {
            GLES20.glDeleteShader(vertexShaderHandle)
            vertexShaderHandle = 0
        }
        if (fragmentShaderHandle != 0) {
            GLES20.glDeleteShader(fragmentShaderHandle)
            fragmentShaderHandle = 0
        }
        if (programHandle != 0) {
            GLES20.glDeleteProgram(programHandle)
            programHandle = 0
        }
        isCompiled = false
        attributeLocations.clear()
        uniformLocations.clear()
        Logger.d(TAG, "Shader program cleaned up")
    }

    /**
     * Compile shader
     */
    private fun compileShader(type: Int, source: String): Int {
        val shader = GLES20.glCreateShader(type)
        if (shader == 0) {
            return 0
        }

        GLES20.glShaderSource(shader, source)
        GLES20.glCompileShader(shader)

        val compileStatus = IntArray(1)
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compileStatus, 0)

        if (compileStatus[0] == 0) {
            val log = GLES20.glGetShaderInfoLog(shader)
            Logger.e(TAG, "Shader compilation error: $log")
            GLES20.glDeleteShader(shader)
            return 0
        }

        return shader
    }
}
