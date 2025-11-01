package uc.ucworks.videosnap.domain.processing

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import uc.ucworks.videosnap.domain.codec.VideoDecoder
import uc.ucworks.videosnap.domain.codec.VideoFrame
import uc.ucworks.videosnap.domain.codec.VideoMetadata
import uc.ucworks.videosnap.domain.memory.FramePool
import uc.ucworks.videosnap.util.Logger
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.*

/**
 * High-level video processing pipeline
 */
class VideoProcessor(private val context: Context) {

    companion object {
        private const val TAG = "VideoProcessor"
        private const val FRAME_CACHE_SIZE = 30 // Cache 1 second at 30fps
    }

    private var decoder: VideoDecoder? = null
    private var metadata: VideoMetadata? = null
    private var framePool: FramePool? = null

    private val frameCache = LinkedHashMap<Long, VideoFrame>(FRAME_CACHE_SIZE, 0.75f, true)
    private val isCancelled = AtomicBoolean(false)

    private val processingScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    /**
     * Initialize processor with video
     */
    suspend fun initialize(uri: Uri): Result<VideoMetadata> = withContext(Dispatchers.IO) {
        try {
            Logger.d(TAG, "Initializing video processor")

            decoder = VideoDecoder(context)
            val result = decoder!!.initialize(uri)

            if (result.isFailure) {
                return@withContext Result.failure(result.exceptionOrNull()!!)
            }

            metadata = result.getOrNull()!!

            // Calculate frame size for pool (YUV420 = width * height * 1.5)
            val frameSize = (metadata!!.width * metadata!!.height * 1.5).toInt()
            framePool = FramePool(frameSize)

            Logger.d(TAG, "Video processor initialized: ${metadata!!.width}x${metadata!!.height}")
            Result.success(metadata!!)
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to initialize processor", e)
            Result.failure(e)
        }
    }

    /**
     * Get frame at specific timestamp with caching
     */
    suspend fun getFrameAt(timestampUs: Long): Result<VideoFrame> = withContext(Dispatchers.IO) {
        try {
            // Check cache first
            frameCache[timestampUs]?.let {
                Logger.d(TAG, "Frame cache hit at ${timestampUs}us")
                return@withContext Result.success(it)
            }

            // Extract frame
            val decoder = this@VideoProcessor.decoder
                ?: return@withContext Result.failure(IllegalStateException("Decoder not initialized"))

            val result = decoder.extractFrameAt(timestampUs)
            if (result.isFailure) {
                return@withContext Result.failure(result.exceptionOrNull()!!)
            }

            val frame = result.getOrNull()!!

            // Cache frame
            cacheFrame(timestampUs, frame)

            Result.success(frame)
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to get frame at ${timestampUs}us", e)
            Result.failure(e)
        }
    }

    /**
     * Extract frames in range
     */
    suspend fun extractFrameRange(
        startUs: Long,
        endUs: Long,
        onProgress: (Int) -> Unit = {}
    ): Result<List<VideoFrame>> = withContext(Dispatchers.IO) {
        try {
            val metadata = this@VideoProcessor.metadata
                ?: return@withContext Result.failure(IllegalStateException("Not initialized"))

            val decoder = this@VideoProcessor.decoder
                ?: return@withContext Result.failure(IllegalStateException("Decoder not initialized"))

            val frames = mutableListOf<VideoFrame>()
            val frameInterval = (1_000_000.0 / metadata.frameRate).toLong()
            val totalFrames = ((endUs - startUs) / frameInterval).toInt()

            Logger.d(TAG, "Extracting ${totalFrames} frames from ${startUs}us to ${endUs}us")

            var currentTime = startUs
            var frameCount = 0

            decoder.seekTo(startUs)

            while (currentTime <= endUs && !isCancelled.get()) {
                val result = decoder.decodeNextFrame()
                if (result.isFailure) {
                    Logger.w(TAG, "Failed to decode frame", result.exceptionOrNull())
                    break
                }

                val frame = result.getOrNull()
                if (frame == null) {
                    break // End of stream
                }

                if (frame.timestamp >= currentTime) {
                    frames.add(frame)
                    cacheFrame(frame.timestamp, frame)
                    currentTime += frameInterval
                    frameCount++

                    val progress = (frameCount * 100) / totalFrames
                    onProgress(progress)
                }
            }

            Logger.d(TAG, "Extracted ${frames.size} frames")
            Result.success(frames)
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to extract frame range", e)
            Result.failure(e)
        }
    }

    /**
     * Seek to timestamp
     */
    suspend fun seekTo(timestampUs: Long): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val decoder = this@VideoProcessor.decoder
                ?: return@withContext Result.failure(IllegalStateException("Decoder not initialized"))

            decoder.seekTo(timestampUs)
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to seek", e)
            Result.failure(e)
        }
    }

    /**
     * Get next frame (for sequential playback)
     */
    suspend fun getNextFrame(): Result<VideoFrame?> = withContext(Dispatchers.IO) {
        try {
            val decoder = this@VideoProcessor.decoder
                ?: return@withContext Result.failure(IllegalStateException("Decoder not initialized"))

            decoder.decodeNextFrame()
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to get next frame", e)
            Result.failure(e)
        }
    }

    /**
     * Get video metadata
     */
    fun getMetadata(): VideoMetadata? = metadata

    /**
     * Cache frame with LRU eviction
     */
    private fun cacheFrame(timestampUs: Long, frame: VideoFrame) {
        if (frameCache.size >= FRAME_CACHE_SIZE) {
            // Remove oldest entry
            val oldestKey = frameCache.keys.first()
            frameCache.remove(oldestKey)
        }
        frameCache[timestampUs] = frame
    }

    /**
     * Clear frame cache
     */
    fun clearCache() {
        frameCache.clear()
        Logger.d(TAG, "Frame cache cleared")
    }

    /**
     * Cancel ongoing operations
     */
    fun cancel() {
        isCancelled.set(true)
        Logger.d(TAG, "Processing cancelled")
    }

    /**
     * Cleanup resources
     */
    fun cleanup() {
        processingScope.cancel()
        decoder?.cleanup()
        framePool?.clear()
        frameCache.clear()
        decoder = null
        metadata = null
        framePool = null
        isCancelled.set(false)
        Logger.d(TAG, "Video processor cleaned up")
    }
}
