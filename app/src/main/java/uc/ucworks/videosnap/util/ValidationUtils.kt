package uc.ucworks.videosnap.util

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap

/**
 * Validation utilities for format and content validation
 */
object ValidationUtils {

    private val SUPPORTED_VIDEO_FORMATS = setOf(
        "video/mp4",
        "video/3gpp",
        "video/webm",
        "video/x-matroska",
        "video/avi",
        "video/quicktime"
    )

    private val SUPPORTED_AUDIO_FORMATS = setOf(
        "audio/mpeg",
        "audio/mp4",
        "audio/aac",
        "audio/wav",
        "audio/x-wav",
        "audio/flac",
        "audio/ogg"
    )

    private val SUPPORTED_IMAGE_FORMATS = setOf(
        "image/jpeg",
        "image/jpg",
        "image/png",
        "image/webp"
    )

    /**
     * Validate if video format is supported
     */
    fun isVideoFormatSupported(mimeType: String?): Boolean {
        return mimeType?.lowercase() in SUPPORTED_VIDEO_FORMATS
    }

    /**
     * Validate if audio format is supported
     */
    fun isAudioFormatSupported(mimeType: String?): Boolean {
        return mimeType?.lowercase() in SUPPORTED_AUDIO_FORMATS
    }

    /**
     * Validate if image format is supported
     */
    fun isImageFormatSupported(mimeType: String?): Boolean {
        return mimeType?.lowercase() in SUPPORTED_IMAGE_FORMATS
    }

    /**
     * Get MIME type from URI
     */
    fun getMimeType(context: Context, uri: Uri): String? {
        return if (uri.scheme == "content") {
            context.contentResolver.getType(uri)
        } else {
            val extension = MimeTypeMap.getFileExtensionFromUrl(uri.toString())
            MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.lowercase())
        }
    }

    /**
     * Validate video file
     */
    fun validateVideoFile(context: Context, uri: Uri): ValidationResult {
        val mimeType = getMimeType(context, uri)

        if (mimeType == null) {
            return ValidationResult.Error("Cannot determine file type")
        }

        if (!isVideoFormatSupported(mimeType)) {
            return ValidationResult.Error("Unsupported video format: $mimeType")
        }

        val fileSize = FileHelper.getFileSize(context, uri)
        if (fileSize == 0L) {
            return ValidationResult.Error("File is empty or unreadable")
        }

        // Check file size limit (500MB)
        val maxSize = 500 * 1024 * 1024L
        if (fileSize > maxSize) {
            return ValidationResult.Error("File too large (max 500MB)")
        }

        return ValidationResult.Success(mimeType, fileSize)
    }

    /**
     * Validate audio file
     */
    fun validateAudioFile(context: Context, uri: Uri): ValidationResult {
        val mimeType = getMimeType(context, uri)

        if (mimeType == null) {
            return ValidationResult.Error("Cannot determine file type")
        }

        if (!isAudioFormatSupported(mimeType)) {
            return ValidationResult.Error("Unsupported audio format: $mimeType")
        }

        val fileSize = FileHelper.getFileSize(context, uri)
        if (fileSize == 0L) {
            return ValidationResult.Error("File is empty or unreadable")
        }

        return ValidationResult.Success(mimeType, fileSize)
    }

    /**
     * Validate image file
     */
    fun validateImageFile(context: Context, uri: Uri): ValidationResult {
        val mimeType = getMimeType(context, uri)

        if (mimeType == null) {
            return ValidationResult.Error("Cannot determine file type")
        }

        if (!isImageFormatSupported(mimeType)) {
            return ValidationResult.Error("Unsupported image format: $mimeType")
        }

        val fileSize = FileHelper.getFileSize(context, uri)
        if (fileSize == 0L) {
            return ValidationResult.Error("File is empty or unreadable")
        }

        return ValidationResult.Success(mimeType, fileSize)
    }

    /**
     * Validate project name
     */
    fun validateProjectName(name: String): Boolean {
        return name.isNotBlank() &&
               name.length <= 100 &&
               !name.contains(Regex("[<>:\"/\\\\|?*]")) // Invalid file name characters
    }

    /**
     * Validate duration (in milliseconds)
     */
    fun isValidDuration(durationMs: Long): Boolean {
        return durationMs > 0 && durationMs <= 10 * 60 * 60 * 1000L // Max 10 hours
    }

    /**
     * Validate resolution
     */
    fun isValidResolution(width: Int, height: Int): Boolean {
        return width > 0 && height > 0 &&
               width <= 7680 && height <= 4320 && // Max 8K
               width >= 144 && height >= 144 // Min reasonable size
    }

    /**
     * Validate frame rate
     */
    fun isValidFrameRate(fps: Int): Boolean {
        return fps in 1..120
    }

    sealed class ValidationResult {
        data class Success(val mimeType: String, val fileSize: Long) : ValidationResult()
        data class Error(val message: String) : ValidationResult()

        fun isSuccess(): Boolean = this is Success
        fun isError(): Boolean = this is Error
    }
}
