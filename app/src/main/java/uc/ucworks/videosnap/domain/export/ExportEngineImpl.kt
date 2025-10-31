package uc.ucworks.videosnap.domain.export

import android.content.Context
import android.media.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import uc.ucworks.videosnap.domain.VideoProject
import uc.ucworks.videosnap.domain.engine.AudioEngine
import uc.ucworks.videosnap.domain.engine.EffectsEngine
import uc.ucworks.videosnap.domain.rendering.RenderingEngine
import java.io.File
import javax.inject.Inject

class ExportEngineImpl @Inject constructor(
    private val context: Context,
    private val renderingEngine: RenderingEngine,
    private val effectsEngine: EffectsEngine,
    private val audioEngine: AudioEngine
) : ExportEngine {

    private var isCancelled = false

    private data class EncoderContext(
        val videoEncoder: MediaCodec,
        val audioEncoder: MediaCodec?,
        val muxer: MediaMuxer,
        var videoTrackIndex: Int = -1,
        var audioTrackIndex: Int = -1,
        var muxerStarted: Boolean = false
    )

    companion object {
        private const val TIMEOUT_USEC = 10000L // 10ms timeout
    }

    override suspend fun exportProject(
        project: VideoProject,
        preset: ExportPreset,
        outputPath: String
    ): Flow<ExportProgress> = flow {
        val settings = ExportSettings(
            resolution = preset.resolution,
            frameRate = preset.frameRate,
            videoBitrate = preset.bitrate,
            videoCodec = preset.codec,
            audioCodec = preset.audioCodec,
            audioBitrate = preset.audioBitrate,
            format = preset.format
        )

        exportProjectCustom(project, settings, outputPath).collect { progress ->
            emit(progress)
        }
    }

    override suspend fun exportProjectCustom(
        project: VideoProject,
        settings: ExportSettings,
        outputPath: String
    ): Flow<ExportProgress> = flow {
        isCancelled = false
        val startTime = System.currentTimeMillis()

        try {
            // Preparing phase
            emit(ExportProgress(0, 1, 0f, 0, 0, ExportStatus.PREPARING))

            val totalFrames = calculateTotalFrames(project, settings.frameRate)
            val frameDurationUs = 1_000_000L / settings.frameRate

            // Setup encoder
            val encoderCtx = setupEncoder(settings, outputPath)
            encoderCtx.videoEncoder.start()
            encoderCtx.audioEncoder?.start()

            // Encoding phase
            emit(ExportProgress(0, totalFrames, 0f, 0, 0, ExportStatus.ENCODING))

            var currentFrame = 0
            var currentTimeMs = 0L
            val bufferInfo = MediaCodec.BufferInfo()

            while (currentTimeMs < project.duration && !isCancelled) {
                // Render frame
                val frame = renderingEngine.renderFrame(project, currentTimeMs)

                if (frame != null) {
                    // Encode frame using MediaCodec
                    encodeFrame(encoderCtx, frame, currentFrame * frameDurationUs, bufferInfo)
                    currentFrame++

                    val elapsed = System.currentTimeMillis() - startTime
                    val progressPercent = (currentFrame.toFloat() / totalFrames) * 100f
                    val estimatedTotal = if (progressPercent > 0) (elapsed / progressPercent) * 100 else 0
                    val remaining = estimatedTotal - elapsed

                    emit(
                        ExportProgress(
                            currentFrame = currentFrame,
                            totalFrames = totalFrames,
                            progressPercent = progressPercent,
                            elapsedTimeMs = elapsed,
                            estimatedTimeRemainingMs = remaining.toLong(),
                            status = ExportStatus.ENCODING
                        )
                    )
                }

                currentTimeMs += (frameDurationUs / 1000)
            }

            if (isCancelled) {
                emit(ExportProgress(currentFrame, totalFrames, 0f, 0, 0, ExportStatus.CANCELLED))
                encoderCtx.videoEncoder.release()
                encoderCtx.audioEncoder?.release()
                encoderCtx.muxer.release()
                return@flow
            }

            // Finalizing - signal end of stream and drain encoders
            emit(ExportProgress(totalFrames, totalFrames, 100f, 0, 0, ExportStatus.FINALIZING))

            signalEndOfStream(encoderCtx.videoEncoder)
            drainEncoder(encoderCtx, bufferInfo, isEndOfStream = true)

            // Release encoder and muxer
            encoderCtx.videoEncoder.stop()
            encoderCtx.videoEncoder.release()
            encoderCtx.audioEncoder?.stop()
            encoderCtx.audioEncoder?.release()

            if (encoderCtx.muxerStarted) {
                encoderCtx.muxer.stop()
            }
            encoderCtx.muxer.release()

            // Completed
            emit(
                ExportProgress(
                    totalFrames,
                    totalFrames,
                    100f,
                    System.currentTimeMillis() - startTime,
                    0,
                    ExportStatus.COMPLETED
                )
            )

        } catch (e: Exception) {
            emit(
                ExportProgress(
                    0,
                    1,
                    0f,
                    0,
                    0,
                    ExportStatus.ERROR
                )
            )
        }
    }

    override fun getAvailablePresets(): List<ExportPreset> {
        return listOf(
            // YouTube Presets
            ExportPreset(
                id = "youtube_1080p",
                name = "YouTube 1080p",
                description = "Optimized for YouTube Full HD",
                platform = ExportPlatform.YOUTUBE,
                resolution = Resolution.FHD_1080P,
                bitrate = 8000,
                frameRate = 30,
                codec = VideoCodec.H264,
                audioCodec = AudioCodec.AAC,
                audioBitrate = 192,
                format = ContainerFormat.MP4
            ),
            ExportPreset(
                id = "youtube_4k",
                name = "YouTube 4K",
                description = "Optimized for YouTube 4K/UHD",
                platform = ExportPlatform.YOUTUBE,
                resolution = Resolution.UHD_4K,
                bitrate = 35000,
                frameRate = 30,
                codec = VideoCodec.H264,
                audioCodec = AudioCodec.AAC,
                audioBitrate = 192,
                format = ContainerFormat.MP4
            ),
            ExportPreset(
                id = "youtube_720p",
                name = "YouTube 720p",
                description = "Optimized for YouTube HD",
                platform = ExportPlatform.YOUTUBE,
                resolution = Resolution.HD_720P,
                bitrate = 5000,
                frameRate = 30,
                codec = VideoCodec.H264,
                audioCodec = AudioCodec.AAC,
                audioBitrate = 128,
                format = ContainerFormat.MP4
            ),

            // Instagram Presets
            ExportPreset(
                id = "instagram_feed",
                name = "Instagram Feed (Square)",
                description = "1:1 aspect ratio for Instagram feed",
                platform = ExportPlatform.INSTAGRAM_FEED,
                resolution = Resolution.INSTAGRAM_SQUARE,
                bitrate = 5000,
                frameRate = 30,
                codec = VideoCodec.H264,
                audioCodec = AudioCodec.AAC,
                audioBitrate = 128,
                format = ContainerFormat.MP4
            ),
            ExportPreset(
                id = "instagram_story",
                name = "Instagram Story",
                description = "9:16 vertical for Instagram Stories",
                platform = ExportPlatform.INSTAGRAM_STORY,
                resolution = Resolution.INSTAGRAM_STORY,
                bitrate = 5000,
                frameRate = 30,
                codec = VideoCodec.H264,
                audioCodec = AudioCodec.AAC,
                audioBitrate = 128,
                format = ContainerFormat.MP4
            ),
            ExportPreset(
                id = "instagram_reel",
                name = "Instagram Reel",
                description = "9:16 vertical for Instagram Reels",
                platform = ExportPlatform.INSTAGRAM_REEL,
                resolution = Resolution.INSTAGRAM_STORY,
                bitrate = 6000,
                frameRate = 30,
                codec = VideoCodec.H264,
                audioCodec = AudioCodec.AAC,
                audioBitrate = 192,
                format = ContainerFormat.MP4
            ),

            // TikTok Preset
            ExportPreset(
                id = "tiktok",
                name = "TikTok",
                description = "9:16 vertical for TikTok",
                platform = ExportPlatform.TIKTOK,
                resolution = Resolution.TIKTOK,
                bitrate = 6000,
                frameRate = 30,
                codec = VideoCodec.H264,
                audioCodec = AudioCodec.AAC,
                audioBitrate = 192,
                format = ContainerFormat.MP4
            ),

            // Facebook Presets
            ExportPreset(
                id = "facebook_hd",
                name = "Facebook HD",
                description = "Optimized for Facebook",
                platform = ExportPlatform.FACEBOOK,
                resolution = Resolution.FHD_1080P,
                bitrate = 5000,
                frameRate = 30,
                codec = VideoCodec.H264,
                audioCodec = AudioCodec.AAC,
                audioBitrate = 128,
                format = ContainerFormat.MP4
            ),

            // Twitter Preset
            ExportPreset(
                id = "twitter",
                name = "Twitter",
                description = "Optimized for Twitter/X",
                platform = ExportPlatform.TWITTER,
                resolution = Resolution.HD_720P,
                bitrate = 5000,
                frameRate = 30,
                codec = VideoCodec.H264,
                audioCodec = AudioCodec.AAC,
                audioBitrate = 128,
                format = ContainerFormat.MP4
            ),

            // LinkedIn Preset
            ExportPreset(
                id = "linkedin",
                name = "LinkedIn",
                description = "Professional quality for LinkedIn",
                platform = ExportPlatform.LINKEDIN,
                resolution = Resolution.FHD_1080P,
                bitrate = 5000,
                frameRate = 30,
                codec = VideoCodec.H264,
                audioCodec = AudioCodec.AAC,
                audioBitrate = 192,
                format = ContainerFormat.MP4
            ),

            // High Quality Preset
            ExportPreset(
                id = "high_quality",
                name = "High Quality",
                description = "Maximum quality export",
                platform = ExportPlatform.CUSTOM,
                resolution = Resolution.FHD_1080P,
                bitrate = 15000,
                frameRate = 60,
                codec = VideoCodec.H265_HEVC,
                audioCodec = AudioCodec.AAC,
                audioBitrate = 320,
                format = ContainerFormat.MP4
            )
        )
    }

    override fun cancelExport() {
        isCancelled = true
    }

    private fun calculateTotalFrames(project: VideoProject, frameRate: Int): Int {
        return ((project.duration / 1000f) * frameRate).toInt()
    }

    private fun encodeFrame(
        encoderCtx: EncoderContext,
        frame: android.graphics.Bitmap,
        presentationTimeUs: Long,
        bufferInfo: MediaCodec.BufferInfo
    ) {
        val encoder = encoderCtx.videoEncoder

        // Get input buffer
        val inputBufferIndex = encoder.dequeueInputBuffer(TIMEOUT_USEC)
        if (inputBufferIndex >= 0) {
            val inputBuffer = encoder.getInputBuffer(inputBufferIndex)
            inputBuffer?.let {
                // Convert bitmap to YUV420 and copy to buffer
                val yuvData = bitmapToYUV420(frame)
                it.clear()
                it.put(yuvData)

                encoder.queueInputBuffer(
                    inputBufferIndex,
                    0,
                    yuvData.size,
                    presentationTimeUs,
                    0
                )
            }
        }

        // Drain encoder
        drainEncoder(encoderCtx, bufferInfo, isEndOfStream = false)
    }

    private fun drainEncoder(
        encoderCtx: EncoderContext,
        bufferInfo: MediaCodec.BufferInfo,
        isEndOfStream: Boolean
    ) {
        val encoder = encoderCtx.videoEncoder
        val timeout = if (isEndOfStream) TIMEOUT_USEC else 0L

        while (true) {
            val outputBufferIndex = encoder.dequeueOutputBuffer(bufferInfo, timeout)

            when {
                outputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER -> {
                    if (!isEndOfStream) break
                }
                outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                    // Add track to muxer
                    if (!encoderCtx.muxerStarted) {
                        encoderCtx.videoTrackIndex = encoderCtx.muxer.addTrack(encoder.outputFormat)

                        // Check if we should start muxer (start when all tracks added)
                        val shouldStart = encoderCtx.audioEncoder == null || encoderCtx.audioTrackIndex >= 0
                        if (shouldStart) {
                            encoderCtx.muxer.start()
                            encoderCtx.muxerStarted = true
                        }
                    }
                }
                outputBufferIndex >= 0 -> {
                    val outputBuffer = encoder.getOutputBuffer(outputBufferIndex)
                    outputBuffer?.let {
                        if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) {
                            // Codec config data - ignore
                            bufferInfo.size = 0
                        }

                        if (bufferInfo.size != 0 && encoderCtx.muxerStarted) {
                            // Write to muxer
                            it.position(bufferInfo.offset)
                            it.limit(bufferInfo.offset + bufferInfo.size)
                            encoderCtx.muxer.writeSampleData(encoderCtx.videoTrackIndex, it, bufferInfo)
                        }

                        encoder.releaseOutputBuffer(outputBufferIndex, false)

                        if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                            break
                        }
                    }
                }
            }
        }
    }

    private fun signalEndOfStream(encoder: MediaCodec) {
        val inputBufferIndex = encoder.dequeueInputBuffer(TIMEOUT_USEC)
        if (inputBufferIndex >= 0) {
            encoder.queueInputBuffer(
                inputBufferIndex,
                0,
                0,
                0,
                MediaCodec.BUFFER_FLAG_END_OF_STREAM
            )
        }
    }

    private fun bitmapToYUV420(bitmap: android.graphics.Bitmap): ByteArray {
        val width = bitmap.width
        val height = bitmap.height
        val yuvSize = width * height * 3 / 2

        val yuv = ByteArray(yuvSize)
        val argb = IntArray(width * height)

        bitmap.getPixels(argb, 0, width, 0, 0, width, height)

        var yIndex = 0
        var uvIndex = width * height

        for (j in 0 until height) {
            for (i in 0 until width) {
                val index = j * width + i
                val R = (argb[index] shr 16) and 0xFF
                val G = (argb[index] shr 8) and 0xFF
                val B = argb[index] and 0xFF

                // Convert RGB to YUV
                val Y = ((66 * R + 129 * G + 25 * B + 128) shr 8) + 16
                val U = ((-38 * R - 74 * G + 112 * B + 128) shr 8) + 128
                val V = ((112 * R - 94 * G - 18 * B + 128) shr 8) + 128

                yuv[yIndex++] = Y.coerceIn(0, 255).toByte()

                // Sample U and V every 2x2 pixels
                if (j % 2 == 0 && i % 2 == 0) {
                    yuv[uvIndex++] = U.coerceIn(0, 255).toByte()
                    yuv[uvIndex++] = V.coerceIn(0, 255).toByte()
                }
            }
        }

        return yuv
    }

    private fun setupEncoder(settings: ExportSettings, outputPath: String): EncoderContext {
        val mimeType = when (settings.videoCodec) {
            VideoCodec.H264 -> MediaFormat.MIMETYPE_VIDEO_AVC
            VideoCodec.H265_HEVC -> MediaFormat.MIMETYPE_VIDEO_HEVC
            VideoCodec.VP9 -> MediaFormat.MIMETYPE_VIDEO_VP9
            VideoCodec.AV1 -> "video/av01" // MediaFormat.MIMETYPE_VIDEO_AV1 for API 29+
        }

        // Setup video encoder
        val videoFormat = MediaFormat.createVideoFormat(
            mimeType,
            settings.resolution.width,
            settings.resolution.height
        ).apply {
            setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
            setInteger(MediaFormat.KEY_BIT_RATE, settings.videoBitrate * 1000) // Convert kbps to bps
            setInteger(MediaFormat.KEY_FRAME_RATE, settings.frameRate)
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1) // I-frame every 1 second

            // Enable hardware acceleration if requested
            if (settings.hardwareAcceleration) {
                setInteger(MediaFormat.KEY_PROFILE, when (settings.videoCodec) {
                    VideoCodec.H264 -> MediaCodecInfo.CodecProfileLevel.AVCProfileHigh
                    VideoCodec.H265_HEVC -> MediaCodecInfo.CodecProfileLevel.HEVCProfileMain
                    else -> 0
                })
            }
        }

        val videoEncoder = if (settings.hardwareAcceleration) {
            try {
                MediaCodec.createEncoderByType(mimeType)
            } catch (e: Exception) {
                // Fallback to software encoder
                MediaCodec.createByCodecName(findSoftwareCodec(mimeType) ?: throw e)
            }
        } else {
            MediaCodec.createEncoderByType(mimeType)
        }

        videoEncoder.configure(videoFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)

        // Setup audio encoder (AAC)
        val audioEncoder = try {
            val audioMimeType = when (settings.audioCodec) {
                AudioCodec.AAC -> MediaFormat.MIMETYPE_AUDIO_AAC
                AudioCodec.MP3 -> MediaFormat.MIMETYPE_AUDIO_MPEG
                AudioCodec.OPUS -> MediaFormat.MIMETYPE_AUDIO_OPUS
                AudioCodec.FLAC -> MediaFormat.MIMETYPE_AUDIO_FLAC
            }

            val audioFormat = MediaFormat.createAudioFormat(
                audioMimeType,
                settings.audioSampleRate,
                2 // Stereo
            ).apply {
                setInteger(MediaFormat.KEY_BIT_RATE, settings.audioBitrate * 1000)
                setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
            }

            val encoder = MediaCodec.createEncoderByType(audioMimeType)
            encoder.configure(audioFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            encoder
        } catch (e: Exception) {
            null // Audio encoding optional
        }

        // Setup muxer
        val muxerFormat = when (settings.format) {
            ContainerFormat.MP4 -> MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4
            ContainerFormat.WEBM -> MediaMuxer.OutputFormat.MUXER_OUTPUT_WEBM
            else -> MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4
        }

        val muxer = MediaMuxer(outputPath, muxerFormat)

        return EncoderContext(
            videoEncoder = videoEncoder,
            audioEncoder = audioEncoder,
            muxer = muxer
        )
    }

    private fun findSoftwareCodec(mimeType: String): String? {
        val codecList = MediaCodecList(MediaCodecList.ALL_CODECS)
        return codecList.codecInfos
            .filter { !it.isHardwareAccelerated && it.isEncoder }
            .firstOrNull { codecInfo ->
                codecInfo.supportedTypes.any { it.equals(mimeType, ignoreCase = true) }
            }?.name
    }
}
