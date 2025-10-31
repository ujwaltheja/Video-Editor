package uc.ucworks.videosnap.domain.export

import android.net.Uri
import kotlinx.coroutines.flow.Flow
import uc.ucworks.videosnap.domain.VideoProject

/**
 * Export engine for rendering and exporting videos
 */
interface ExportEngine {
    /**
     * Export project with preset
     */
    suspend fun exportProject(
        project: VideoProject,
        preset: ExportPreset,
        outputPath: String
    ): Flow<ExportProgress>

    /**
     * Export project with custom settings
     */
    suspend fun exportProjectCustom(
        project: VideoProject,
        settings: ExportSettings,
        outputPath: String
    ): Flow<ExportProgress>

    /**
     * Get available export presets
     */
    fun getAvailablePresets(): List<ExportPreset>

    /**
     * Cancel ongoing export
     */
    fun cancelExport()
}

data class ExportPreset(
    val id: String,
    val name: String,
    val description: String,
    val platform: ExportPlatform,
    val resolution: Resolution,
    val bitrate: Int, // kbps
    val frameRate: Int,
    val codec: VideoCodec,
    val audioCodec: AudioCodec,
    val audioBitrate: Int, // kbps
    val format: ContainerFormat
)

enum class ExportPlatform {
    YOUTUBE,
    INSTAGRAM_FEED,
    INSTAGRAM_STORY,
    INSTAGRAM_REEL,
    TIKTOK,
    FACEBOOK,
    TWITTER,
    LINKEDIN,
    CUSTOM
}

data class Resolution(
    val width: Int,
    val height: Int
) {
    override fun toString(): String = "${width}x${height}"

    companion object {
        val HD_720P = Resolution(1280, 720)
        val FHD_1080P = Resolution(1920, 1080)
        val QHD_1440P = Resolution(2560, 1440)
        val UHD_4K = Resolution(3840, 2160)
        val INSTAGRAM_SQUARE = Resolution(1080, 1080)
        val INSTAGRAM_STORY = Resolution(1080, 1920)
        val TIKTOK = Resolution(1080, 1920)
    }
}

enum class VideoCodec {
    H264,
    H265_HEVC,
    VP9,
    AV1
}

enum class AudioCodec {
    AAC,
    MP3,
    OPUS,
    FLAC
}

enum class ContainerFormat {
    MP4,
    MOV,
    WEBM,
    AVI,
    MKV
}

data class ExportSettings(
    val resolution: Resolution,
    val frameRate: Int,
    val videoBitrate: Int,
    val videoCodec: VideoCodec,
    val audioCodec: AudioCodec,
    val audioBitrate: Int,
    val audioSampleRate: Int = 48000,
    val format: ContainerFormat,
    val hardwareAcceleration: Boolean = true
)

data class ExportProgress(
    val currentFrame: Int,
    val totalFrames: Int,
    val progressPercent: Float,
    val elapsedTimeMs: Long,
    val estimatedTimeRemainingMs: Long,
    val status: ExportStatus
)

enum class ExportStatus {
    PREPARING,
    ENCODING,
    FINALIZING,
    COMPLETED,
    ERROR,
    CANCELLED
}
