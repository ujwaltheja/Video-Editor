package uc.ucworks.videosnap.domain.codec

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.net.Uri
import uc.ucworks.videosnap.util.Logger
import java.nio.ByteBuffer
import java.util.concurrent.LinkedBlockingQueue

/**
 * Video decoder for extracting frames using hardware-accelerated MediaCodec
 */
class VideoDecoder(private val context: Context) {

    companion object {
        private const val TAG = "VideoDecoder"
        private const val TIMEOUT_US = 10000L
    }

    private var extractor: MediaExtractor? = null
    private var codecWrapper: MediaCodecWrapper? = null
    private var videoTrackIndex = -1
    private var videoFormat: MediaFormat? = null
    private var metadata: VideoMetadata? = null

    /**
     * Initialize decoder with video URI
     */
    fun initialize(uri: Uri): Result<VideoMetadata> {
        return try {
            // Extract metadata
            val metadataResult = extractMetadata(uri)
            if (metadataResult.isFailure) {
                return Result.failure(metadataResult.exceptionOrNull()!!)
            }
            metadata = metadataResult.getOrNull()!!

            // Setup extractor
            extractor = MediaExtractor().apply {
                setDataSource(context, uri, null)
            }

            // Find video track
            videoTrackIndex = findVideoTrack(extractor!!)
            if (videoTrackIndex < 0) {
                return Result.failure(IllegalStateException("No video track found"))
            }

            extractor?.selectTrack(videoTrackIndex)
            videoFormat = extractor?.getTrackFormat(videoTrackIndex)

            // Create decoder
            codecWrapper = MediaCodecWrapper()
            val createResult = codecWrapper?.createDecoder(videoFormat!!)
            if (createResult?.isFailure == true) {
                return Result.failure(createResult.exceptionOrNull()!!)
            }

            codecWrapper?.start()

            Logger.d(TAG, "Decoder initialized: ${metadata?.width}x${metadata?.height} @ ${metadata?.frameRate}fps")
            Result.success(metadata!!)
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to initialize decoder", e)
            cleanup()
            Result.failure(e)
        }
    }

    /**
     * Extract video metadata
     */
    fun extractMetadata(uri: Uri): Result<VideoMetadata> {
        return try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(context, uri)

            val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            val width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0
            val height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0
            val rotation = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toIntOrNull() ?: 0
            val mimeType = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE) ?: "video/mp4"
            val bitrate = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)?.toLongOrNull() ?: 0L

            val hasVideo = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_VIDEO) == "yes"
            val hasAudio = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_AUDIO) == "yes"

            retriever.release()

            // Calculate frame rate (default to 30 if not available)
            val frameRate = 30f // We'll calculate this more accurately later if needed

            val metadata = VideoMetadata(
                duration = duration * 1000, // Convert to microseconds
                width = width,
                height = height,
                frameRate = frameRate,
                videoCodec = mimeType,
                audioCodec = if (hasAudio) "aac" else null,
                bitrate = bitrate,
                fileSize = 0L, // We'll get this from URI if needed
                rotation = rotation,
                mimeType = mimeType,
                hasAudio = hasAudio,
                hasVideo = hasVideo
            )

            Result.success(metadata)
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to extract metadata", e)
            Result.failure(e)
        }
    }

    /**
     * Find video track in extractor
     */
    private fun findVideoTrack(extractor: MediaExtractor): Int {
        for (i in 0 until extractor.trackCount) {
            val format = extractor.getTrackFormat(i)
            val mime = format.getString(MediaFormat.KEY_MIME) ?: continue
            if (mime.startsWith("video/")) {
                Logger.d(TAG, "Found video track at index $i: $mime")
                return i
            }
        }
        return -1
    }

    /**
     * Seek to specific timestamp
     */
    fun seekTo(timestampUs: Long): Result<Unit> {
        return try {
            extractor?.seekTo(timestampUs, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)
            codecWrapper?.flush()
            Logger.d(TAG, "Seeked to ${timestampUs}us")
            Result.success(Unit)
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to seek", e)
            Result.failure(e)
        }
    }

    /**
     * Decode next frame
     */
    fun decodeNextFrame(): Result<VideoFrame?> {
        try {
            val codecWrapper = this.codecWrapper ?: return Result.failure(IllegalStateException("Decoder not initialized"))

            // Feed input to decoder
            val inputBufferIndex = codecWrapper.dequeueInputBuffer(TIMEOUT_US)
            if (inputBufferIndex >= 0) {
                val inputBuffer = codecWrapper.getInputBuffer(inputBufferIndex)
                val sampleSize = extractor?.readSampleData(inputBuffer!!, 0) ?: -1

                if (sampleSize < 0) {
                    // End of stream
                    codecWrapper.queueInputBuffer(
                        inputBufferIndex, 0, 0, 0,
                        MediaCodec.BUFFER_FLAG_END_OF_STREAM
                    )
                } else {
                    val presentationTimeUs = extractor?.sampleTime ?: 0
                    codecWrapper.queueInputBuffer(
                        inputBufferIndex, 0, sampleSize, presentationTimeUs, 0
                    )
                    extractor?.advance()
                }
            }

            // Get output from decoder
            val bufferInfo = MediaCodec.BufferInfo()
            val outputBufferIndex = codecWrapper.dequeueOutputBuffer(bufferInfo, TIMEOUT_US)

            when {
                outputBufferIndex >= 0 -> {
                    val outputBuffer = codecWrapper.getOutputBuffer(outputBufferIndex)

                    if (outputBuffer != null && bufferInfo.size > 0) {
                        // Extract frame data
                        val frameData = ByteArray(bufferInfo.size)
                        outputBuffer.position(bufferInfo.offset)
                        outputBuffer.get(frameData, 0, bufferInfo.size)

                        val frame = VideoFrame(
                            timestamp = bufferInfo.presentationTimeUs,
                            data = frameData,
                            width = metadata?.width ?: 0,
                            height = metadata?.height ?: 0,
                            isKeyFrame = (bufferInfo.flags and MediaCodec.BUFFER_FLAG_KEY_FRAME) != 0,
                            pixelFormat = PixelFormat.YUV420
                        )

                        codecWrapper.releaseOutputBuffer(outputBufferIndex, false)
                        return Result.success(frame)
                    }

                    codecWrapper.releaseOutputBuffer(outputBufferIndex, false)
                }
                outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                    val newFormat = codecWrapper.getOutputFormat()
                    Logger.d(TAG, "Output format changed: $newFormat")
                }
                outputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER -> {
                    // No output available yet
                }
            }

            if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                return Result.success(null) // End of stream
            }

            return Result.success(null) // No frame available yet
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to decode frame", e)
            return Result.failure(e)
        }
    }

    /**
     * Extract frame at specific timestamp
     */
    fun extractFrameAt(timestampUs: Long): Result<VideoFrame> {
        val seekResult = seekTo(timestampUs)
        if (seekResult.isFailure) {
            return Result.failure(seekResult.exceptionOrNull()!!)
        }

        // Decode frames until we find the one we want
        var attempts = 0
        val maxAttempts = 100

        while (attempts < maxAttempts) {
            val frameResult = decodeNextFrame()
            if (frameResult.isFailure) {
                return Result.failure(frameResult.exceptionOrNull()!!)
            }

            val frame = frameResult.getOrNull()
            if (frame != null && frame.timestamp >= timestampUs) {
                return Result.success(frame)
            }

            attempts++
        }

        return Result.failure(IllegalStateException("Could not find frame at $timestampUs"))
    }

    /**
     * Get video metadata
     */
    fun getMetadata(): VideoMetadata? = metadata

    /**
     * Cleanup resources
     */
    fun cleanup() {
        try {
            codecWrapper?.stop()
            codecWrapper?.release()
            extractor?.release()

            codecWrapper = null
            extractor = null
            videoTrackIndex = -1
            videoFormat = null
            metadata = null

            Logger.d(TAG, "Decoder cleaned up")
        } catch (e: Exception) {
            Logger.e(TAG, "Error during cleanup", e)
        }
    }
}
