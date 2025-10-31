package uc.ucworks.videosnap.domain.engine

import uc.ucworks.videosnap.TimelineClip

/**
 * Audio engine for processing audio tracks
 */
interface AudioEngine {
    /**
     * Generate waveform data for visualization
     */
    suspend fun generateWaveform(audioPath: String, samplesPerSecond: Int = 100): FloatArray

    /**
     * Apply volume adjustment
     */
    suspend fun adjustVolume(audioPath: String, volume: Float): ByteArray

    /**
     * Apply fade in/out
     */
    suspend fun applyFade(
        audioPath: String,
        fadeInDuration: Long,
        fadeOutDuration: Long
    ): ByteArray

    /**
     * Mix multiple audio clips
     */
    suspend fun mixAudio(clips: List<Pair<TimelineClip, Long>>): ByteArray

    /**
     * Apply EQ (Equalizer)
     */
    suspend fun applyEQ(
        audioPath: String,
        lowGain: Float,
        midGain: Float,
        highGain: Float
    ): ByteArray

    /**
     * Apply noise reduction
     */
    suspend fun applyNoiseReduction(audioPath: String, strength: Float): ByteArray

    /**
     * Normalize audio levels
     */
    suspend fun normalizeAudio(audioPath: String): ByteArray

    /**
     * Extract audio from video
     */
    suspend fun extractAudio(videoPath: String): String // Returns path to extracted audio
}
