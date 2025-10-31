package uc.ucworks.videosnap.domain.rendering

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import uc.ucworks.videosnap.TimelineClip
import uc.ucworks.videosnap.domain.VideoProject
import javax.inject.Inject

class RenderingEngineImpl @Inject constructor(
    private val context: Context
) : RenderingEngine {

    private var isInitialized = false

    override suspend fun initialize() = withContext(Dispatchers.IO) {
        // Initialize GPU resources, shaders, etc.
        isInitialized = true
    }

    override suspend fun renderFrame(project: VideoProject, timestampMs: Long): Bitmap? =
        withContext(Dispatchers.IO) {
            // TODO: Implement actual frame rendering with GPU
            // This would involve:
            // 1. Determining which clips are active at timestampMs
            // 2. Decoding frames from each clip using MediaCodec
            // 3. Applying effects, transitions
            // 4. Compositing all layers
            // 5. Return final frame as Bitmap
            null
        }

    override suspend fun preprocessClip(clip: TimelineClip): ProcessedClip =
        withContext(Dispatchers.IO) {
            val metadata = getVideoMetadataFromPath(clip.mediaPath)
            val thumbnails = generateThumbnailsForClip(clip, 10)

            ProcessedClip(
                clip = clip,
                thumbnails = thumbnails,
                metadata = metadata
            )
        }

    override suspend fun generateThumbnail(
        mediaPath: String,
        timestampMs: Long,
        width: Int,
        height: Int
    ): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(mediaPath)
            val bitmap = retriever.getFrameAtTime(
                timestampMs * 1000, // Convert to microseconds
                MediaMetadataRetriever.OPTION_CLOSEST_SYNC
            )
            retriever.release()

            bitmap?.let {
                Bitmap.createScaledBitmap(it, width, height, true)
            }
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun getVideoMetadata(uri: Uri): VideoMetadata =
        withContext(Dispatchers.IO) {
            getVideoMetadataFromPath(uri.path ?: "")
        }

    private fun getVideoMetadataFromPath(path: String): VideoMetadata {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(path)

            val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            val width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0
            val height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0
            val frameRate = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CAPTURE_FRAMERATE)?.toFloatOrNull() ?: 30f
            val bitrate = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)?.toLongOrNull() ?: 0L
            val hasAudio = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_AUDIO) == "yes"

            VideoMetadata(
                duration = duration,
                width = width,
                height = height,
                frameRate = frameRate,
                bitrate = bitrate,
                codec = "H.264", // Default, could be extracted
                hasAudio = hasAudio,
                audioChannels = if (hasAudio) 2 else 0,
                audioSampleRate = if (hasAudio) 48000 else 0
            )
        } finally {
            retriever.release()
        }
    }

    private suspend fun generateThumbnailsForClip(clip: TimelineClip, count: Int): List<Bitmap> =
        withContext(Dispatchers.IO) {
            val thumbnails = mutableListOf<Bitmap>()
            val duration = clip.duration
            val interval = duration / count

            repeat(count) { i ->
                val timestamp = clip.startTime + (interval * i)
                generateThumbnail(clip.mediaPath, timestamp, 160, 90)?.let {
                    thumbnails.add(it)
                }
            }

            thumbnails
        }

    override fun release() {
        // Release GPU resources
        isInitialized = false
    }
}
