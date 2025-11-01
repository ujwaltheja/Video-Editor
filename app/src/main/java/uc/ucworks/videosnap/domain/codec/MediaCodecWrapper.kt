package uc.ucworks.videosnap.domain.codec

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaCodecList
import android.media.MediaExtractor
import android.media.MediaFormat
import uc.ucworks.videosnap.util.Logger
import java.nio.ByteBuffer

/**
 * Wrapper around MediaCodec for hardware-accelerated video encoding/decoding
 */
class MediaCodecWrapper {

    companion object {
        private const val TAG = "MediaCodecWrapper"
        private const val TIMEOUT_US = 10000L

        /**
         * Get all available codecs for a MIME type
         */
        fun getAvailableCodecs(mimeType: String, isEncoder: Boolean): List<CodecInfo> {
            val codecList = MediaCodecList(MediaCodecList.REGULAR_CODECS)
            val codecInfos = mutableListOf<CodecInfo>()

            for (codecInfo in codecList.codecInfos) {
                if (codecInfo.isEncoder != isEncoder) continue

                val supportedTypes = codecInfo.supportedTypes
                if (!supportedTypes.contains(mimeType)) continue

                try {
                    val capabilities = codecInfo.getCapabilitiesForType(mimeType)
                    val videoCapabilities = capabilities.videoCapabilities ?: continue
                    val colorFormats = capabilities.colorFormats.toList()

                    codecInfos.add(
                        CodecInfo(
                            name = codecInfo.name,
                            mimeType = mimeType,
                            isEncoder = isEncoder,
                            isHardwareAccelerated = isHardwareAccelerated(codecInfo),
                            supportedColorFormats = colorFormats,
                            maxWidth = videoCapabilities.supportedWidths.upper,
                            maxHeight = videoCapabilities.supportedHeights.upper,
                            maxFrameRate = videoCapabilities.supportedFrameRates.upper.toInt(),
                            maxBitRate = capabilities.videoCapabilities.bitrateRange.upper
                        )
                    )
                } catch (e: Exception) {
                    Logger.w(TAG, "Failed to get capabilities for ${codecInfo.name}", e)
                }
            }

            return codecInfos.sortedByDescending { it.isHardwareAccelerated }
        }

        /**
         * Check if codec is hardware accelerated
         */
        private fun isHardwareAccelerated(codecInfo: MediaCodecInfo): Boolean {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                return codecInfo.isHardwareAccelerated
            }

            // For older versions, check codec name
            val name = codecInfo.name.lowercase()
            return !name.contains("omx.google") &&
                   !name.contains("c2.android") &&
                   (name.contains("qcom") || name.contains("mtk") ||
                    name.contains("exynos") || name.contains("hisi"))
        }

        /**
         * Get best decoder for MIME type (prefers hardware-accelerated)
         */
        fun getBestDecoder(mimeType: String): String? {
            val codecs = getAvailableCodecs(mimeType, isEncoder = false)
            return codecs.firstOrNull { it.isHardwareAccelerated }?.name
                ?: codecs.firstOrNull()?.name
        }

        /**
         * Get best encoder for MIME type (prefers hardware-accelerated)
         */
        fun getBestEncoder(mimeType: String): String? {
            val codecs = getAvailableCodecs(mimeType, isEncoder = true)
            return codecs.firstOrNull { it.isHardwareAccelerated }?.name
                ?: codecs.firstOrNull()?.name
        }
    }

    private var codec: MediaCodec? = null
    private var isConfigured = false

    /**
     * Create decoder with MediaFormat
     */
    fun createDecoder(format: MediaFormat): Result<Unit> {
        return try {
            val mimeType = format.getString(MediaFormat.KEY_MIME)
                ?: return Result.failure(IllegalArgumentException("No MIME type in format"))

            val codecName = getBestDecoder(mimeType)
                ?: return Result.failure(IllegalStateException("No decoder found for $mimeType"))

            Logger.d(TAG, "Creating decoder: $codecName for $mimeType")

            codec = MediaCodec.createByCodecName(codecName)
            codec?.configure(format, null, null, 0)
            isConfigured = true

            Logger.d(TAG, "Decoder created and configured")
            Result.success(Unit)
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to create decoder", e)
            Result.failure(e)
        }
    }

    /**
     * Create encoder with MediaFormat
     */
    fun createEncoder(format: MediaFormat): Result<Unit> {
        return try {
            val mimeType = format.getString(MediaFormat.KEY_MIME)
                ?: return Result.failure(IllegalArgumentException("No MIME type in format"))

            val codecName = getBestEncoder(mimeType)
                ?: return Result.failure(IllegalStateException("No encoder found for $mimeType"))

            Logger.d(TAG, "Creating encoder: $codecName for $mimeType")

            codec = MediaCodec.createByCodecName(codecName)
            codec?.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            isConfigured = true

            Logger.d(TAG, "Encoder created and configured")
            Result.success(Unit)
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to create encoder", e)
            Result.failure(e)
        }
    }

    /**
     * Start codec
     */
    fun start(): Result<Unit> {
        return try {
            if (!isConfigured) {
                return Result.failure(IllegalStateException("Codec not configured"))
            }
            codec?.start()
            Logger.d(TAG, "Codec started")
            Result.success(Unit)
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to start codec", e)
            Result.failure(e)
        }
    }

    /**
     * Get input buffer index
     */
    fun dequeueInputBuffer(timeoutUs: Long = TIMEOUT_US): Int {
        return codec?.dequeueInputBuffer(timeoutUs) ?: -1
    }

    /**
     * Get input buffer
     */
    fun getInputBuffer(index: Int): ByteBuffer? {
        return codec?.getInputBuffer(index)
    }

    /**
     * Queue input buffer
     */
    fun queueInputBuffer(
        index: Int,
        offset: Int,
        size: Int,
        presentationTimeUs: Long,
        flags: Int
    ) {
        codec?.queueInputBuffer(index, offset, size, presentationTimeUs, flags)
    }

    /**
     * Get output buffer index
     */
    fun dequeueOutputBuffer(
        bufferInfo: MediaCodec.BufferInfo,
        timeoutUs: Long = TIMEOUT_US
    ): Int {
        return codec?.dequeueOutputBuffer(bufferInfo, timeoutUs) ?: -1
    }

    /**
     * Get output buffer
     */
    fun getOutputBuffer(index: Int): ByteBuffer? {
        return codec?.getOutputBuffer(index)
    }

    /**
     * Release output buffer
     */
    fun releaseOutputBuffer(index: Int, render: Boolean) {
        codec?.releaseOutputBuffer(index, render)
    }

    /**
     * Get output format
     */
    fun getOutputFormat(): MediaFormat? {
        return codec?.outputFormat
    }

    /**
     * Stop codec
     */
    fun stop() {
        try {
            codec?.stop()
            Logger.d(TAG, "Codec stopped")
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to stop codec", e)
        }
    }

    /**
     * Release codec resources
     */
    fun release() {
        try {
            codec?.release()
            codec = null
            isConfigured = false
            Logger.d(TAG, "Codec released")
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to release codec", e)
        }
    }

    /**
     * Flush codec
     */
    fun flush() {
        try {
            codec?.flush()
            Logger.d(TAG, "Codec flushed")
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to flush codec", e)
        }
    }
}
