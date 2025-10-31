package uc.ucworks.videosnap

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

class AudioWaveformGeneratorTest {

    @Test
    fun testGenerateWaveformData() = runBlocking {
        val dummyFile = createDummyWavFile()
        val waveform = AudioWaveformGenerator.generateWaveformData(dummyFile.absolutePath)

        // Assert that the waveform is not empty and has a reasonable size
        assertFalse(waveform.isEmpty())
        assertTrue(waveform.size > 10)

        dummyFile.delete()
    }

    @Test
    fun testGenerateWaveformData_withInvalidFile() = runBlocking {
        val waveform = AudioWaveformGenerator.generateWaveformData("non_existent_file.mp3")
        assertTrue(waveform.isEmpty())
    }

    private fun createDummyWavFile(): File {
        val file = File.createTempFile("test_audio", ".wav")
        val sampleRate = 44100
        val duration = 1 // second
        val numSamples = duration * sampleRate
        val numChannels = 1
        val bitsPerSample = 16
        val byteRate = sampleRate * numChannels * bitsPerSample / 8
        val blockAlign = numChannels * bitsPerSample / 8
        val dataSize = numSamples * numChannels * bitsPerSample / 8
        val fileSize = 36 + dataSize

        val fos = FileOutputStream(file)
        val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)

        // RIFF header
        header.put('R'.code.toByte())
        header.put('I'.code.toByte())
        header.put('F'.code.toByte())
        header.put('F'.code.toByte())
        header.putInt(fileSize)
        header.put('W'.code.toByte())
        header.put('A'.code.toByte())
        header.put('V'.code.toByte())
        header.put('E'.code.toByte())

        // fmt chunk
        header.put('f'.code.toByte())
        header.put('m'.code.toByte())
        header.put('t'.code.toByte())
        header.put(' '.code.toByte())
        header.putInt(16) // Sub-chunk size
        header.putShort(1) // Audio format (1 for PCM)
        header.putShort(numChannels.toShort())
        header.putInt(sampleRate)
        header.putInt(byteRate)
        header.putShort(blockAlign.toShort())
        header.putShort(bitsPerSample.toShort())

        // data chunk
        header.put('d'.code.toByte())
        header.put('a'.code.toByte())
        header.put('t'.code.toByte())
        header.put('a'.code.toByte())
        header.putInt(dataSize)

        fos.write(header.array())

        // Audio data (sine wave)
        val data = ByteBuffer.allocate(dataSize).order(ByteOrder.LITTLE_ENDIAN)
        for (i in 0 until numSamples) {
            val angle = 2.0 * Math.PI * i.toDouble() / (sampleRate / 440.0) // 440 Hz tone
            val sample = (Math.sin(angle) * 32767.0).toInt().toShort()
            data.putShort(sample)
        }

        fos.write(data.array())
        fos.close()
        return file
    }
}
