package uc.ucworks.videosnap

import android.media.MediaExtractor
import android.media.MediaFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * An object that generates audio waveform data from an audio file.
 */
object AudioWaveformGenerator {

    /**
     * Generates audio waveform data from an audio file.
     *
     * @param filePath The path to the audio file.
     * @return A list of integers representing the waveform data.
     */
    suspend fun generateWaveformData(filePath: String): List<Int> = withContext(Dispatchers.IO) {
        val waveform = mutableListOf<Int>()
        val extractor = MediaExtractor()
        try {
            extractor.setDataSource(filePath)

            val trackIndex = findAudioTrack(extractor)
            if (trackIndex == -1) {
                return@withContext emptyList()
            }

            extractor.selectTrack(trackIndex)

            val buffer = ByteBuffer.allocate(1024 * 1024).order(ByteOrder.nativeOrder())

            while (true) {
                val sampleSize = extractor.readSampleData(buffer, 0)
                if (sampleSize <= 0) {
                    break
                }
                buffer.limit(sampleSize)
                buffer.rewind()

                val amplitude = getAmplitude(buffer)
                waveform.add(amplitude)

                if (!extractor.advance()) {
                    break
                }
            }
        } catch (e: Exception) {
            e.printStackTrace() // In a real app, log this properly
        } finally {
            extractor.release()
        }

        waveform
    }

    /**
     * Finds the first audio track in a media file.
     *
     * @param extractor The media extractor.
     * @return The index of the audio track, or -1 if no audio track was found.
     */
    private fun findAudioTrack(extractor: MediaExtractor): Int {
        for (i in 0 until extractor.trackCount) {
            val format = extractor.getTrackFormat(i)
            val mime = format.getString(MediaFormat.KEY_MIME)
            if (mime?.startsWith("audio/") == true) {
                return i
            }
        }
        return -1
    }

    /**
     * Gets the maximum amplitude from a byte buffer.
     *
     * @param buffer The byte buffer.
     * @return The maximum amplitude.
     */
    private fun getAmplitude(buffer: ByteBuffer): Int {
        var maxAmplitude = 0
        val shortBuffer = buffer.asShortBuffer()
        for (i in 0 until shortBuffer.limit()) {
            val amplitude = Math.abs(shortBuffer.get(i).toInt())
            if (amplitude > maxAmplitude) {
                maxAmplitude = amplitude
            }
        }
        return maxAmplitude
    }
}
