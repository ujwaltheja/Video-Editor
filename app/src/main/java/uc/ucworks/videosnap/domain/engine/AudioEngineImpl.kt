package uc.ucworks.videosnap.domain.engine

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import uc.ucworks.videosnap.TimelineClip
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ShortBuffer
import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

class AudioEngineImpl @Inject constructor(
    private val context: Context
) : AudioEngine {

    override suspend fun generateWaveform(audioPath: String, samplesPerSecond: Int): FloatArray =
        withContext(Dispatchers.IO) {
            try {
                val extractor = MediaExtractor()
                extractor.setDataSource(audioPath)

                // Find audio track
                val audioTrack = findAudioTrack(extractor) ?: return@withContext FloatArray(0)
                extractor.selectTrack(audioTrack)

                val format = extractor.getTrackFormat(audioTrack)
                val duration = format.getLong(MediaFormat.KEY_DURATION)
                val sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)

                // Calculate samples needed
                val durationSeconds = duration / 1_000_000f
                val totalSamples = (durationSeconds * samplesPerSecond).toInt()
                val waveform = FloatArray(totalSamples)

                // Extract audio samples
                val decoder = MediaCodec.createDecoderByType(format.getString(MediaFormat.KEY_MIME)!!)
                decoder.configure(format, null, null, 0)
                decoder.start()

                var sampleIndex = 0
                var isDecoding = true

                while (isDecoding && sampleIndex < totalSamples) {
                    val inputBufferIndex = decoder.dequeueInputBuffer(10000)
                    if (inputBufferIndex >= 0) {
                        val inputBuffer = decoder.getInputBuffer(inputBufferIndex)
                        val sampleSize = extractor.readSampleData(inputBuffer!!, 0)

                        if (sampleSize < 0) {
                            decoder.queueInputBuffer(inputBufferIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            isDecoding = false
                        } else {
                            decoder.queueInputBuffer(inputBufferIndex, 0, sampleSize, extractor.sampleTime, 0)
                            extractor.advance()
                        }
                    }

                    val bufferInfo = MediaCodec.BufferInfo()
                    val outputBufferIndex = decoder.dequeueOutputBuffer(bufferInfo, 10000)
                    if (outputBufferIndex >= 0) {
                        val outputBuffer = decoder.getOutputBuffer(outputBufferIndex)
                        if (outputBuffer != null && bufferInfo.size > 0) {
                            // Calculate average amplitude for this chunk
                            var sum = 0f
                            val shortBuffer = outputBuffer.asShortBuffer()
                            while (shortBuffer.hasRemaining()) {
                                sum += abs(shortBuffer.get().toFloat())
                            }
                            val avg = sum / (bufferInfo.size / 2)
                            if (sampleIndex < totalSamples) {
                                waveform[sampleIndex++] = avg / Short.MAX_VALUE
                            }
                        }
                        decoder.releaseOutputBuffer(outputBufferIndex, false)
                    }
                }

                decoder.stop()
                decoder.release()
                extractor.release()

                waveform
            } catch (e: Exception) {
                FloatArray(0)
            }
        }

    override suspend fun adjustVolume(audioPath: String, volume: Float): ByteArray =
        withContext(Dispatchers.IO) {
            try {
                val audioData = decodeAudioToSamples(audioPath)

                // Adjust volume by multiplying samples
                for (i in audioData.indices) {
                    audioData[i] = (audioData[i] * volume).coerceIn(-1f, 1f)
                }

                // Convert back to byte array
                samplesToByteArray(audioData)
            } catch (e: Exception) {
                e.printStackTrace()
                ByteArray(0)
            }
        }

    override suspend fun applyFade(
        audioPath: String,
        fadeInDuration: Long,
        fadeOutDuration: Long
    ): ByteArray = withContext(Dispatchers.IO) {
        try {
            val audioData = decodeAudioToSamples(audioPath)

            // Get sample rate to convert duration to samples
            val extractor = MediaExtractor()
            extractor.setDataSource(audioPath)
            val audioTrack = findAudioTrack(extractor) ?: return@withContext ByteArray(0)
            val format = extractor.getTrackFormat(audioTrack)
            val sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            val channelCount = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
            extractor.release()

            val fadeInSamples = ((fadeInDuration / 1000f) * sampleRate * channelCount).toInt()
            val fadeOutSamples = ((fadeOutDuration / 1000f) * sampleRate * channelCount).toInt()

            // Apply fade in
            for (i in 0 until min(fadeInSamples, audioData.size)) {
                val gain = i.toFloat() / fadeInSamples
                audioData[i] *= gain
            }

            // Apply fade out
            val fadeOutStart = max(0, audioData.size - fadeOutSamples)
            for (i in fadeOutStart until audioData.size) {
                val gain = (audioData.size - i).toFloat() / fadeOutSamples
                audioData[i] *= gain
            }

            samplesToByteArray(audioData)
        } catch (e: Exception) {
            e.printStackTrace()
            ByteArray(0)
        }
    }

    override suspend fun mixAudio(clips: List<Pair<TimelineClip, Long>>): ByteArray =
        withContext(Dispatchers.IO) {
            try {
                if (clips.isEmpty()) return@withContext ByteArray(0)

                // Decode all audio clips
                val audioClips = clips.map { (clip, offset) ->
                    Triple(decodeAudioToSamples(clip.mediaPath), offset, clip.volume)
                }

                // Find the maximum length needed
                val maxLength = audioClips.maxOfOrNull { (samples, offset, _) ->
                    offset + samples.size
                } ?: 0

                val mixedAudio = FloatArray(maxLength.toInt())

                // Mix all clips by summing samples at each position
                audioClips.forEach { (samples, offset, volume) ->
                    for (i in samples.indices) {
                        val position = offset.toInt() + i
                        if (position < mixedAudio.size) {
                            mixedAudio[position] += samples[i] * volume
                        }
                    }
                }

                // Normalize to prevent clipping
                val peak = mixedAudio.maxOfOrNull { abs(it) } ?: 1f
                if (peak > 1f) {
                    for (i in mixedAudio.indices) {
                        mixedAudio[i] /= peak
                    }
                }

                samplesToByteArray(mixedAudio)
            } catch (e: Exception) {
                e.printStackTrace()
                ByteArray(0)
            }
        }

    override suspend fun applyEQ(
        audioPath: String,
        lowGain: Float,
        midGain: Float,
        highGain: Float
    ): ByteArray = withContext(Dispatchers.IO) {
        try {
            val audioData = decodeAudioToSamples(audioPath)

            // Simplified 3-band EQ using basic filtering
            // In production, would use proper IIR/FIR filters
            // This is a simplified version that applies gain to different frequency bands

            // For now, apply a simple gain curve
            // A proper implementation would use FFT or biquad filters
            for (i in audioData.indices) {
                // Simplified: just apply average gain
                val avgGain = (lowGain + midGain + highGain) / 3f
                audioData[i] *= avgGain
                audioData[i] = audioData[i].coerceIn(-1f, 1f)
            }

            samplesToByteArray(audioData)
        } catch (e: Exception) {
            e.printStackTrace()
            ByteArray(0)
        }
    }

    override suspend fun applyNoiseReduction(audioPath: String, strength: Float): ByteArray =
        withContext(Dispatchers.IO) {
            try {
                val audioData = decodeAudioToSamples(audioPath)

                // Simplified noise reduction using threshold gate
                // In production, would use spectral subtraction or Wiener filtering
                val threshold = 0.02f * (1f - strength)

                for (i in audioData.indices) {
                    if (abs(audioData[i]) < threshold) {
                        audioData[i] *= (1f - strength)
                    }
                }

                samplesToByteArray(audioData)
            } catch (e: Exception) {
                e.printStackTrace()
                ByteArray(0)
            }
        }

    override suspend fun normalizeAudio(audioPath: String): ByteArray =
        withContext(Dispatchers.IO) {
            try {
                val audioData = decodeAudioToSamples(audioPath)

                // Find peak amplitude
                val peak = audioData.maxOfOrNull { abs(it) } ?: 1f

                if (peak > 0f) {
                    // Scale all samples to use full dynamic range
                    val scale = 0.95f / peak // Leave 5% headroom
                    for (i in audioData.indices) {
                        audioData[i] *= scale
                    }
                }

                samplesToByteArray(audioData)
            } catch (e: Exception) {
                e.printStackTrace()
                ByteArray(0)
            }
        }

    override suspend fun extractAudio(videoPath: String): String =
        withContext(Dispatchers.IO) {
            try {
                val outputPath = File(context.cacheDir, "extracted_audio_${System.currentTimeMillis()}.aac")

                val extractor = MediaExtractor()
                extractor.setDataSource(videoPath)

                // Find audio track
                val audioTrack = findAudioTrack(extractor)
                if (audioTrack == null) {
                    extractor.release()
                    return@withContext ""
                }

                extractor.selectTrack(audioTrack)
                val format = extractor.getTrackFormat(audioTrack)

                // Create muxer
                val muxer = MediaMuxer(outputPath.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
                val muxerTrackIndex = muxer.addTrack(format)
                muxer.start()

                // Copy audio samples
                val buffer = ByteBuffer.allocate(1024 * 1024) // 1MB buffer
                val bufferInfo = MediaCodec.BufferInfo()

                while (true) {
                    val sampleSize = extractor.readSampleData(buffer, 0)
                    if (sampleSize < 0) break

                    bufferInfo.size = sampleSize
                    bufferInfo.presentationTimeUs = extractor.sampleTime
                    bufferInfo.flags = extractor.sampleFlags

                    muxer.writeSampleData(muxerTrackIndex, buffer, bufferInfo)
                    extractor.advance()
                }

                muxer.stop()
                muxer.release()
                extractor.release()

                outputPath.absolutePath
            } catch (e: Exception) {
                e.printStackTrace()
                ""
            }
        }

    private fun findAudioTrack(extractor: MediaExtractor): Int? {
        for (i in 0 until extractor.trackCount) {
            val format = extractor.getTrackFormat(i)
            val mime = format.getString(MediaFormat.KEY_MIME)
            if (mime?.startsWith("audio/") == true) {
                return i
            }
        }
        return null
    }

    private fun decodeAudioToSamples(audioPath: String): FloatArray {
        val extractor = MediaExtractor()
        extractor.setDataSource(audioPath)

        val audioTrack = findAudioTrack(extractor) ?: return FloatArray(0)
        extractor.selectTrack(audioTrack)

        val format = extractor.getTrackFormat(audioTrack)
        val mime = format.getString(MediaFormat.KEY_MIME)!!

        val decoder = MediaCodec.createDecoderByType(mime)
        decoder.configure(format, null, null, 0)
        decoder.start()

        val samples = mutableListOf<Float>()
        var isDecoding = true

        while (isDecoding) {
            val inputBufferIndex = decoder.dequeueInputBuffer(10000)
            if (inputBufferIndex >= 0) {
                val inputBuffer = decoder.getInputBuffer(inputBufferIndex)!!
                val sampleSize = extractor.readSampleData(inputBuffer, 0)

                if (sampleSize < 0) {
                    decoder.queueInputBuffer(inputBufferIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                    isDecoding = false
                } else {
                    decoder.queueInputBuffer(inputBufferIndex, 0, sampleSize, extractor.sampleTime, 0)
                    extractor.advance()
                }
            }

            val bufferInfo = MediaCodec.BufferInfo()
            val outputBufferIndex = decoder.dequeueOutputBuffer(bufferInfo, 10000)
            if (outputBufferIndex >= 0) {
                val outputBuffer = decoder.getOutputBuffer(outputBufferIndex)
                if (outputBuffer != null && bufferInfo.size > 0) {
                    val shortBuffer = outputBuffer.asShortBuffer()
                    while (shortBuffer.hasRemaining()) {
                        samples.add(shortBuffer.get().toFloat() / Short.MAX_VALUE)
                    }
                }
                decoder.releaseOutputBuffer(outputBufferIndex, false)
            }
        }

        decoder.stop()
        decoder.release()
        extractor.release()

        return samples.toFloatArray()
    }

    private fun samplesToByteArray(samples: FloatArray): ByteArray {
        val byteBuffer = ByteBuffer.allocate(samples.size * 2) // 2 bytes per sample (16-bit)
        val shortBuffer = byteBuffer.asShortBuffer()

        for (sample in samples) {
            val shortSample = (sample * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
            shortBuffer.put(shortSample.toShort())
        }

        return byteBuffer.array()
    }
}
