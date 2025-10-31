package uc.ucworks.videosnap.domain.rendering

import android.graphics.Bitmap
import android.net.Uri
import uc.ucworks.videosnap.TimelineClip
import uc.ucworks.videosnap.domain.VideoProject

/**
 * Rendering engine interface for GPU-accelerated video processing
 */
interface RenderingEngine {
    /**
     * Initialize the rendering engine
     */
    suspend fun initialize()

    /**
     * Render a single frame at the given timestamp
     */
    suspend fun renderFrame(project: VideoProject, timestampMs: Long): Bitmap?

    /**
     * Pre-process clip for real-time playback (generate thumbnails, proxy if needed)
     */
    suspend fun preprocessClip(clip: TimelineClip): ProcessedClip

    /**
     * Generate thumbnail for a clip at specific time
     */
    suspend fun generateThumbnail(mediaPath: String, timestampMs: Long, width: Int, height: Int): Bitmap?

    /**
     * Extract video metadata
     */
    suspend fun getVideoMetadata(uri: Uri): VideoMetadata

    /**
     * Release resources
     */
    fun release()
}

data class ProcessedClip(
    val clip: TimelineClip,
    val thumbnails: List<Bitmap>,
    val metadata: VideoMetadata,
    val proxyPath: String? = null
)

data class VideoMetadata(
    val duration: Long,
    val width: Int,
    val height: Int,
    val frameRate: Float,
    val bitrate: Long,
    val codec: String,
    val hasAudio: Boolean,
    val audioChannels: Int = 0,
    val audioSampleRate: Int = 0
)
