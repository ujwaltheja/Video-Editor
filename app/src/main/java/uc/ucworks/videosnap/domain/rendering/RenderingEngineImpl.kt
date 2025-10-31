package uc.ucworks.videosnap.domain.rendering

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import uc.ucworks.videosnap.TimelineClip
import uc.ucworks.videosnap.TimelineTrack
import uc.ucworks.videosnap.domain.VideoProject
import uc.ucworks.videosnap.domain.engine.EffectsEngine
import uc.ucworks.videosnap.domain.engine.TransitionEngine
import javax.inject.Inject
import java.nio.ByteBuffer

class RenderingEngineImpl @Inject constructor(
    private val context: Context,
    private val effectsEngine: EffectsEngine,
    private val transitionEngine: TransitionEngine
) : RenderingEngine {

    private var isInitialized = false

    // Cache for video decoders to avoid recreating them for each frame
    private val decoderCache = mutableMapOf<String, VideoDecoder>()

    private data class VideoDecoder(
        val extractor: MediaExtractor,
        val decoder: MediaCodec,
        val videoTrackIndex: Int
    )

    companion object {
        private const val DECODER_TIMEOUT_US = 10000L
    }

    override suspend fun initialize() = withContext(Dispatchers.IO) {
        // Initialize GPU resources, shaders, etc.
        isInitialized = true
    }

    override suspend fun renderFrame(project: VideoProject, timestampMs: Long): Bitmap? =
        withContext(Dispatchers.IO) {
            try {
                // Create output bitmap with project resolution
                val outputBitmap = Bitmap.createBitmap(
                    project.resolution.width,
                    project.resolution.height,
                    Bitmap.Config.ARGB_8888
                )
                val canvas = Canvas(outputBitmap)
                val paint = Paint().apply {
                    isAntiAlias = true
                    isFilterBitmap = true
                }

                // Render each track from bottom to top
                project.tracks.sortedBy { it.index }.forEach { track ->
                    renderTrack(track, timestampMs, canvas, paint, project)
                }

                outputBitmap
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }

    private fun renderTrack(
        track: TimelineTrack,
        timestampMs: Long,
        canvas: Canvas,
        paint: Paint,
        project: VideoProject
    ) {
        // Find active clips at this timestamp
        val activeClips = track.clips.filter { clip ->
            timestampMs >= clip.startTime && timestampMs < (clip.startTime + clip.duration)
        }

        activeClips.forEach { clip ->
            renderClip(clip, timestampMs, canvas, paint, project)
        }
    }

    private fun renderClip(
        clip: TimelineClip,
        timestampMs: Long,
        canvas: Canvas,
        paint: Paint,
        project: VideoProject
    ) {
        // Calculate timestamp within the clip
        val clipLocalTime = timestampMs - clip.startTime + clip.trimStart

        // Decode frame from clip
        val frameBitmap = decodeFrameFromClip(clip, clipLocalTime) ?: return

        // Apply clip transformations (scale, rotation, position)
        var transformedBitmap = applyTransformations(frameBitmap, clip)

        // Apply effects
        clip.effects.forEach { effect ->
            transformedBitmap = effectsEngine.applyEffect(transformedBitmap, effect)
        }

        // Apply opacity
        paint.alpha = (clip.opacity * 255).toInt()

        // Draw to canvas
        val matrix = Matrix().apply {
            postTranslate(clip.x, clip.y)
            postScale(clip.scaleX, clip.scaleY, clip.x, clip.y)
            postRotate(clip.rotation, clip.x + transformedBitmap.width / 2f, clip.y + transformedBitmap.height / 2f)
        }

        canvas.drawBitmap(transformedBitmap, matrix, paint)

        // Reset paint alpha
        paint.alpha = 255

        // Recycle intermediate bitmap if different from original
        if (transformedBitmap != frameBitmap) {
            transformedBitmap.recycle()
        }
    }

    private fun decodeFrameFromClip(clip: TimelineClip, timestampMs: Long): Bitmap? {
        return try {
            // Try to use MediaMetadataRetriever for simplicity
            // For production, should use MediaCodec with frame-accurate seeking
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(clip.mediaPath)
            val bitmap = retriever.getFrameAtTime(
                timestampMs * 1000, // Convert to microseconds
                MediaMetadataRetriever.OPTION_CLOSEST
            )
            retriever.release()
            bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun decodeFrameWithMediaCodec(clip: TimelineClip, timestampMs: Long): Bitmap? {
        // Get or create decoder for this clip
        val decoder = getOrCreateDecoder(clip) ?: return null

        try {
            // Seek to timestamp
            decoder.extractor.seekTo(timestampMs * 1000, MediaExtractor.SEEK_TO_CLOSEST_SYNC)

            val bufferInfo = MediaCodec.BufferInfo()
            var outputBitmap: Bitmap? = null

            // Decode frames until we reach the desired timestamp
            while (true) {
                val inputBufferIndex = decoder.decoder.dequeueInputBuffer(DECODER_TIMEOUT_US)
                if (inputBufferIndex >= 0) {
                    val inputBuffer = decoder.decoder.getInputBuffer(inputBufferIndex)
                    val sampleSize = decoder.extractor.readSampleData(inputBuffer!!, 0)

                    if (sampleSize < 0) {
                        decoder.decoder.queueInputBuffer(
                            inputBufferIndex, 0, 0, 0,
                            MediaCodec.BUFFER_FLAG_END_OF_STREAM
                        )
                        break
                    } else {
                        val presentationTimeUs = decoder.extractor.sampleTime
                        decoder.decoder.queueInputBuffer(
                            inputBufferIndex, 0, sampleSize,
                            presentationTimeUs, 0
                        )
                        decoder.extractor.advance()
                    }
                }

                val outputBufferIndex = decoder.decoder.dequeueOutputBuffer(bufferInfo, DECODER_TIMEOUT_US)
                if (outputBufferIndex >= 0) {
                    // Get decoded frame
                    // Note: This requires using MediaCodec with Surface output for proper image extraction
                    // For now, fallback to MediaMetadataRetriever
                    decoder.decoder.releaseOutputBuffer(outputBufferIndex, true)

                    // Check if we've reached the desired timestamp
                    if (bufferInfo.presentationTimeUs >= timestampMs * 1000) {
                        break
                    }
                }
            }

            return outputBitmap
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    private fun getOrCreateDecoder(clip: TimelineClip): VideoDecoder? {
        return decoderCache.getOrPut(clip.mediaPath) {
            try {
                val extractor = MediaExtractor()
                extractor.setDataSource(clip.mediaPath)

                // Find video track
                var videoTrackIndex = -1
                for (i in 0 until extractor.trackCount) {
                    val format = extractor.getTrackFormat(i)
                    val mime = format.getString(MediaFormat.KEY_MIME)
                    if (mime?.startsWith("video/") == true) {
                        videoTrackIndex = i
                        break
                    }
                }

                if (videoTrackIndex < 0) {
                    extractor.release()
                    return@getOrPut null
                }

                extractor.selectTrack(videoTrackIndex)
                val format = extractor.getTrackFormat(videoTrackIndex)
                val mime = format.getString(MediaFormat.KEY_MIME)!!

                val decoder = MediaCodec.createDecoderByType(mime)
                decoder.configure(format, null, null, 0)
                decoder.start()

                VideoDecoder(extractor, decoder, videoTrackIndex)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    private fun applyTransformations(bitmap: Bitmap, clip: TimelineClip): Bitmap {
        // Basic scaling if needed
        val targetWidth = (bitmap.width * clip.scaleX).toInt()
        val targetHeight = (bitmap.height * clip.scaleY).toInt()

        return if (targetWidth != bitmap.width || targetHeight != bitmap.height) {
            Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)
        } else {
            bitmap
        }
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
        // Release all cached decoders
        decoderCache.values.forEach { decoder ->
            try {
                decoder.decoder.stop()
                decoder.decoder.release()
                decoder.extractor.release()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        decoderCache.clear()

        // Release GPU resources
        isInitialized = false
    }
}
