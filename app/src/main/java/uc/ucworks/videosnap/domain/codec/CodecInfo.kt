package uc.ucworks.videosnap.domain.codec

/**
 * Information about available codec
 */
data class CodecInfo(
    val name: String,
    val mimeType: String,
    val isEncoder: Boolean,
    val isHardwareAccelerated: Boolean,
    val supportedColorFormats: List<Int>,
    val maxWidth: Int,
    val maxHeight: Int,
    val maxFrameRate: Int,
    val maxBitRate: Int
)

/**
 * Video metadata extracted from file
 */
data class VideoMetadata(
    val duration: Long, // microseconds
    val width: Int,
    val height: Int,
    val frameRate: Float,
    val videoCodec: String,
    val audioCodec: String?,
    val bitrate: Long,
    val fileSize: Long,
    val rotation: Int,
    val mimeType: String,
    val hasAudio: Boolean,
    val hasVideo: Boolean
)

/**
 * Represents a single decoded video frame
 */
data class VideoFrame(
    val timestamp: Long, // microseconds
    val data: ByteArray,
    val width: Int,
    val height: Int,
    val isKeyFrame: Boolean,
    val pixelFormat: PixelFormat
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as VideoFrame

        if (timestamp != other.timestamp) return false
        if (!data.contentEquals(other.data)) return false
        if (width != other.width) return false
        if (height != other.height) return false
        if (isKeyFrame != other.isKeyFrame) return false
        if (pixelFormat != other.pixelFormat) return false

        return true
    }

    override fun hashCode(): Int {
        var result = timestamp.hashCode()
        result = 31 * result + data.contentHashCode()
        result = 31 * result + width
        result = 31 * result + height
        result = 31 * result + isKeyFrame.hashCode()
        result = 31 * result + pixelFormat.hashCode()
        return result
    }
}

enum class PixelFormat {
    YUV420,
    NV12,
    NV21,
    RGB,
    RGBA
}
