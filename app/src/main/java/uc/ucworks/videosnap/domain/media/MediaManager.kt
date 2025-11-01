package uc.ucworks.videosnap.domain.media

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import uc.ucworks.videosnap.domain.codec.VideoDecoder
import uc.ucworks.videosnap.domain.codec.VideoMetadata
import uc.ucworks.videosnap.util.FileHelper
import uc.ucworks.videosnap.util.Logger
import uc.ucworks.videosnap.util.ValidationUtils
import java.io.File
import java.io.FileOutputStream

/**
 * Manages media import, validation, and thumbnail generation
 */
class MediaManager(private val context: Context) {

    companion object {
        private const val TAG = "MediaManager"
        private const val THUMBNAILS_DIR = "thumbnails"
    }

    /**
     * Import video with validation
     */
    fun importVideo(uri: Uri): Result<VideoImportResult> {
        Logger.d(TAG, "Importing video: $uri")

        return try {
            // Validate format
            val validationResult = ValidationUtils.validateVideoFile(context, uri)
            if (validationResult.isError()) {
                val error = validationResult as ValidationUtils.ValidationResult.Error
                return Result.failure(IllegalArgumentException(error.message))
            }

            val validation = validationResult as ValidationUtils.ValidationResult.Success

            // Extract metadata
            val decoder = VideoDecoder(context)
            val metadataResult = decoder.extractMetadata(uri)
            decoder.cleanup()

            if (metadataResult.isFailure) {
                return Result.failure(metadataResult.exceptionOrNull()!!)
            }

            val metadata = metadataResult.getOrNull()!!

            // Validate resolution
            if (!ValidationUtils.isValidResolution(metadata.width, metadata.height)) {
                return Result.failure(
                    IllegalArgumentException(
                        "Invalid resolution: ${metadata.width}x${metadata.height}"
                    )
                )
            }

            // Generate thumbnail
            val thumbnailResult = generateThumbnail(uri, 0)
            val thumbnailPath = thumbnailResult.getOrNull()

            val result = VideoImportResult(
                uri = uri,
                metadata = metadata,
                mimeType = validation.mimeType,
                fileSize = validation.fileSize,
                thumbnailPath = thumbnailPath,
                isValid = true
            )

            Logger.d(TAG, "Video imported successfully: ${metadata.width}x${metadata.height}")
            Result.success(result)
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to import video", e)
            Result.failure(e)
        }
    }

    /**
     * Generate thumbnail at specific timestamp
     */
    fun generateThumbnail(
        uri: Uri,
        timestampUs: Long,
        width: Int = 320,
        height: Int = 180
    ): Result<String> {
        return try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(context, uri)

            val bitmap = retriever.getFrameAtTime(
                timestampUs,
                MediaMetadataRetriever.OPTION_CLOSEST_SYNC
            ) ?: return Result.failure(IllegalStateException("Cannot extract thumbnail"))

            retriever.release()

            // Scale bitmap
            val scaledBitmap = Bitmap.createScaledBitmap(bitmap, width, height, true)
            if (scaledBitmap != bitmap) {
                bitmap.recycle()
            }

            // Save to file
            val thumbnailsDir = File(context.cacheDir, THUMBNAILS_DIR)
            FileHelper.ensureDirectoryExists(thumbnailsDir)

            val thumbnailFile = File(thumbnailsDir, "thumb_${System.currentTimeMillis()}.jpg")
            FileOutputStream(thumbnailFile).use { out ->
                scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
            }
            scaledBitmap.recycle()

            Logger.d(TAG, "Thumbnail generated: ${thumbnailFile.path}")
            Result.success(thumbnailFile.absolutePath)
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to generate thumbnail", e)
            Result.failure(e)
        }
    }

    /**
     * Generate multiple thumbnails for timeline
     */
    fun generateTimelineThumbnails(
        uri: Uri,
        count: Int = 10,
        width: Int = 160,
        height: Int = 90
    ): Result<List<String>> {
        return try {
            val decoder = VideoDecoder(context)
            val metadataResult = decoder.extractMetadata(uri)
            decoder.cleanup()

            if (metadataResult.isFailure) {
                return Result.failure(metadataResult.exceptionOrNull()!!)
            }

            val metadata = metadataResult.getOrNull()!!
            val interval = metadata.duration / count

            val thumbnails = mutableListOf<String>()
            for (i in 0 until count) {
                val timestamp = i * interval
                val result = generateThumbnail(uri, timestamp, width, height)
                result.getOrNull()?.let { thumbnails.add(it) }
            }

            Logger.d(TAG, "Generated ${thumbnails.size} timeline thumbnails")
            Result.success(thumbnails)
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to generate timeline thumbnails", e)
            Result.failure(e)
        }
    }

    /**
     * Extract audio from video
     */
    fun extractAudio(uri: Uri): Result<String> {
        // This would use MediaMuxer to extract audio track
        // For now, returning placeholder
        return Result.failure(NotImplementedError("Audio extraction not yet implemented"))
    }

    /**
     * Check if format is supported
     */
    fun isFormatSupported(uri: Uri): Boolean {
        val mimeType = ValidationUtils.getMimeType(context, uri)
        return ValidationUtils.isVideoFormatSupported(mimeType)
    }

    /**
     * Get video metadata
     */
    fun getMetadata(uri: Uri): Result<VideoMetadata> {
        val decoder = VideoDecoder(context)
        val result = decoder.extractMetadata(uri)
        decoder.cleanup()
        return result
    }

    /**
     * Clean up old thumbnails
     */
    fun cleanupThumbnails(maxAgeMs: Long = 7 * 24 * 60 * 60 * 1000L) {
        val thumbnailsDir = File(context.cacheDir, THUMBNAILS_DIR)
        if (!thumbnailsDir.exists()) return

        val now = System.currentTimeMillis()
        var deletedCount = 0

        thumbnailsDir.listFiles()?.forEach { file ->
            if (file.isFile && (now - file.lastModified()) > maxAgeMs) {
                if (FileHelper.deleteFile(file)) {
                    deletedCount++
                }
            }
        }

        Logger.d(TAG, "Cleaned up $deletedCount thumbnails")
    }
}

/**
 * Result of video import operation
 */
data class VideoImportResult(
    val uri: Uri,
    val metadata: VideoMetadata,
    val mimeType: String,
    val fileSize: Long,
    val thumbnailPath: String?,
    val isValid: Boolean
)
