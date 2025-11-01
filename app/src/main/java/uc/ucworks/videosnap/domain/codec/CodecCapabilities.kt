package uc.ucworks.videosnap.domain.codec

import android.media.MediaCodecInfo
import android.media.MediaCodecList
import android.media.MediaFormat
import uc.ucworks.videosnap.util.Logger

/**
 * Utility for checking codec capabilities
 */
object CodecCapabilities {

    private const val TAG = "CodecCapabilities"

    /**
     * Check if H.264 encoding is supported
     */
    fun supportsH264Encoding(width: Int, height: Int, frameRate: Int): Boolean {
        return checkEncoderSupport(MediaFormat.MIMETYPE_VIDEO_AVC, width, height, frameRate)
    }

    /**
     * Check if H.265/HEVC encoding is supported
     */
    fun supportsHEVCEncoding(width: Int, height: Int, frameRate: Int): Boolean {
        return checkEncoderSupport(MediaFormat.MIMETYPE_VIDEO_HEVC, width, height, frameRate)
    }

    /**
     * Check if VP9 encoding is supported
     */
    fun supportsVP9Encoding(width: Int, height: Int, frameRate: Int): Boolean {
        return checkEncoderSupport(MediaFormat.MIMETYPE_VIDEO_VP9, width, height, frameRate)
    }

    /**
     * Check if AV1 encoding is supported
     */
    fun supportsAV1Encoding(width: Int, height: Int, frameRate: Int): Boolean {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            checkEncoderSupport(MediaFormat.MIMETYPE_VIDEO_AV1, width, height, frameRate)
        } else {
            false
        }
    }

    /**
     * Get supported video encoders
     */
    fun getSupportedVideoEncoders(): List<String> {
        val encoders = mutableListOf<String>()
        val codecList = MediaCodecList(MediaCodecList.REGULAR_CODECS)

        for (codecInfo in codecList.codecInfos) {
            if (!codecInfo.isEncoder) continue

            codecInfo.supportedTypes.forEach { type ->
                if (type.startsWith("video/") && type !in encoders) {
                    encoders.add(type)
                }
            }
        }

        return encoders
    }

    /**
     * Get supported audio encoders
     */
    fun getSupportedAudioEncoders(): List<String> {
        val encoders = mutableListOf<String>()
        val codecList = MediaCodecList(MediaCodecList.REGULAR_CODECS)

        for (codecInfo in codecList.codecInfos) {
            if (!codecInfo.isEncoder) continue

            codecInfo.supportedTypes.forEach { type ->
                if (type.startsWith("audio/") && type !in encoders) {
                    encoders.add(type)
                }
            }
        }

        return encoders
    }

    /**
     * Get maximum supported resolution for codec
     */
    fun getMaxResolution(mimeType: String): Pair<Int, Int>? {
        val codecList = MediaCodecList(MediaCodecList.REGULAR_CODECS)

        for (codecInfo in codecList.codecInfos) {
            if (!codecInfo.isEncoder) continue
            if (!codecInfo.supportedTypes.contains(mimeType)) continue

            try {
                val capabilities = codecInfo.getCapabilitiesForType(mimeType)
                val videoCapabilities = capabilities.videoCapabilities ?: continue

                return Pair(
                    videoCapabilities.supportedWidths.upper,
                    videoCapabilities.supportedHeights.upper
                )
            } catch (e: Exception) {
                Logger.w(TAG, "Failed to get capabilities for $mimeType", e)
            }
        }

        return null
    }

    /**
     * Get maximum supported frame rate for codec
     */
    fun getMaxFrameRate(mimeType: String, width: Int, height: Int): Int? {
        val codecList = MediaCodecList(MediaCodecList.REGULAR_CODECS)

        for (codecInfo in codecList.codecInfos) {
            if (!codecInfo.isEncoder) continue
            if (!codecInfo.supportedTypes.contains(mimeType)) continue

            try {
                val capabilities = codecInfo.getCapabilitiesForType(mimeType)
                val videoCapabilities = capabilities.videoCapabilities ?: continue

                if (videoCapabilities.isSizeSupported(width, height)) {
                    return videoCapabilities.getSupportedFrameRatesFor(width, height).upper.toInt()
                }
            } catch (e: Exception) {
                Logger.w(TAG, "Failed to get max frame rate for $mimeType", e)
            }
        }

        return null
    }

    /**
     * Get supported color formats for encoder
     */
    fun getSupportedColorFormats(mimeType: String): List<Int> {
        val codecList = MediaCodecList(MediaCodecList.REGULAR_CODECS)
        val colorFormats = mutableListOf<Int>()

        for (codecInfo in codecList.codecInfos) {
            if (!codecInfo.isEncoder) continue
            if (!codecInfo.supportedTypes.contains(mimeType)) continue

            try {
                val capabilities = codecInfo.getCapabilitiesForType(mimeType)
                colorFormats.addAll(capabilities.colorFormats.toList())
            } catch (e: Exception) {
                Logger.w(TAG, "Failed to get color formats for $mimeType", e)
            }
        }

        return colorFormats.distinct()
    }

    /**
     * Check if hardware acceleration is available
     */
    fun hasHardwareAcceleration(mimeType: String): Boolean {
        val codecList = MediaCodecList(MediaCodecList.REGULAR_CODECS)

        for (codecInfo in codecList.codecInfos) {
            if (!codecInfo.supportedTypes.contains(mimeType)) continue

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                if (codecInfo.isHardwareAccelerated) {
                    return true
                }
            } else {
                // For older versions, check codec name
                val name = codecInfo.name.lowercase()
                if (!name.contains("omx.google") && !name.contains("c2.android")) {
                    return true
                }
            }
        }

        return false
    }

    /**
     * Get recommended encoder for mime type
     */
    fun getRecommendedEncoder(mimeType: String, preferHardware: Boolean = true): String? {
        val codecs = MediaCodecWrapper.getAvailableCodecs(mimeType, isEncoder = true)

        return if (preferHardware) {
            codecs.firstOrNull { it.isHardwareAccelerated }?.name
                ?: codecs.firstOrNull()?.name
        } else {
            codecs.firstOrNull { !it.isHardwareAccelerated }?.name
                ?: codecs.firstOrNull()?.name
        }
    }

    /**
     * Validate export settings
     */
    fun validateExportSettings(
        mimeType: String,
        width: Int,
        height: Int,
        frameRate: Int,
        bitrate: Int
    ): ValidationResult {
        val errors = mutableListOf<String>()

        // Check if encoder exists
        val encoder = getRecommendedEncoder(mimeType)
        if (encoder == null) {
            errors.add("No encoder found for $mimeType")
            return ValidationResult(false, errors)
        }

        // Check resolution
        val maxRes = getMaxResolution(mimeType)
        if (maxRes != null) {
            if (width > maxRes.first || height > maxRes.second) {
                errors.add("Resolution ${width}x${height} exceeds maximum ${maxRes.first}x${maxRes.second}")
            }
        }

        // Check frame rate
        val maxFps = getMaxFrameRate(mimeType, width, height)
        if (maxFps != null && frameRate > maxFps) {
            errors.add("Frame rate $frameRate exceeds maximum $maxFps")
        }

        // Check if size is supported
        val codecList = MediaCodecList(MediaCodecList.REGULAR_CODECS)
        var sizeSupported = false

        for (codecInfo in codecList.codecInfos) {
            if (!codecInfo.isEncoder) continue
            if (!codecInfo.supportedTypes.contains(mimeType)) continue

            try {
                val capabilities = codecInfo.getCapabilitiesForType(mimeType)
                val videoCapabilities = capabilities.videoCapabilities
                if (videoCapabilities?.isSizeSupported(width, height) == true) {
                    sizeSupported = true
                    break
                }
            } catch (e: Exception) {
                // Continue checking other codecs
            }
        }

        if (!sizeSupported) {
            errors.add("Resolution ${width}x${height} not supported by any encoder")
        }

        return ValidationResult(errors.isEmpty(), errors)
    }

    /**
     * Check encoder support for specific parameters
     */
    private fun checkEncoderSupport(
        mimeType: String,
        width: Int,
        height: Int,
        frameRate: Int
    ): Boolean {
        val codecList = MediaCodecList(MediaCodecList.REGULAR_CODECS)

        for (codecInfo in codecList.codecInfos) {
            if (!codecInfo.isEncoder) continue
            if (!codecInfo.supportedTypes.contains(mimeType)) continue

            try {
                val capabilities = codecInfo.getCapabilitiesForType(mimeType)
                val videoCapabilities = capabilities.videoCapabilities ?: continue

                if (videoCapabilities.isSizeSupported(width, height)) {
                    val supportedFps = videoCapabilities.getSupportedFrameRatesFor(width, height)
                    if (frameRate <= supportedFps.upper) {
                        return true
                    }
                }
            } catch (e: Exception) {
                Logger.w(TAG, "Error checking encoder support", e)
            }
        }

        return false
    }

    /**
     * Validation result
     */
    data class ValidationResult(
        val isValid: Boolean,
        val errors: List<String>
    )
}
