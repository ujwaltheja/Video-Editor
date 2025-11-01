package uc.ucworks.videosnap.domain.sync

import uc.ucworks.videosnap.util.Logger
import kotlin.math.abs

/**
 * Audio-Video synchronization engine
 */
class AVSyncEngine {

    companion object {
        private const val TAG = "AVSyncEngine"
        private const val MAX_AUDIO_DRIFT_MS = 50L // Maximum allowed drift
        private const val SYNC_THRESHOLD_MS = 20L // Threshold to trigger sync correction
    }

    private var audioClockMs: Long = 0
    private var videoClockMs: Long = 0
    private var masterClock: ClockType = ClockType.VIDEO
    private var lastSyncTime: Long = 0

    /**
     * Set the master clock (which clock to sync to)
     */
    fun setMasterClock(clock: ClockType) {
        masterClock = clock
        Logger.d(TAG, "Master clock set to: $clock")
    }

    /**
     * Update audio clock position
     */
    fun updateAudioClock(timestampMs: Long) {
        audioClockMs = timestampMs
    }

    /**
     * Update video clock position
     */
    fun updateVideoClock(timestampMs: Long) {
        videoClockMs = timestampMs
    }

    /**
     * Get current drift between audio and video
     */
    fun getDrift(): Long {
        return audioClockMs - videoClockMs
    }

    /**
     * Check if synchronization is needed
     */
    fun needsSync(): Boolean {
        val drift = abs(getDrift())
        return drift > SYNC_THRESHOLD_MS
    }

    /**
     * Get sync action to take
     */
    fun getSyncAction(): SyncAction {
        val drift = getDrift()

        return when {
            abs(drift) <= SYNC_THRESHOLD_MS -> {
                SyncAction.NoAction
            }
            drift > 0 -> {
                // Audio is ahead of video
                when (masterClock) {
                    ClockType.VIDEO -> SyncAction.SlowDownAudio(drift)
                    ClockType.AUDIO -> SyncAction.SpeedUpVideo(drift)
                    ClockType.EXTERNAL -> SyncAction.NoAction
                }
            }
            else -> {
                // Video is ahead of audio
                val absDrift = abs(drift)
                when (masterClock) {
                    ClockType.VIDEO -> SyncAction.SpeedUpAudio(absDrift)
                    ClockType.AUDIO -> SyncAction.SlowDownVideo(absDrift)
                    ClockType.EXTERNAL -> SyncAction.NoAction
                }
            }
        }
    }

    /**
     * Calculate delay for video frame to maintain sync
     */
    fun calculateVideoDelay(nominalDelayMs: Long): Long {
        if (!needsSync()) {
            return nominalDelayMs
        }

        val drift = getDrift()
        val correction = when {
            drift > SYNC_THRESHOLD_MS -> {
                // Audio ahead, slow down video
                nominalDelayMs + (drift / 2)
            }
            drift < -SYNC_THRESHOLD_MS -> {
                // Video ahead, speed up video
                (nominalDelayMs - (abs(drift) / 2)).coerceAtLeast(0)
            }
            else -> nominalDelayMs
        }

        Logger.d(TAG, "Video delay: $nominalDelayMs -> $correction (drift: ${drift}ms)")
        return correction
    }

    /**
     * Calculate delay for audio to maintain sync
     */
    fun calculateAudioDelay(nominalDelayMs: Long): Long {
        if (!needsSync()) {
            return nominalDelayMs
        }

        val drift = getDrift()
        val correction = when {
            drift < -SYNC_THRESHOLD_MS -> {
                // Video ahead, slow down audio
                nominalDelayMs + (abs(drift) / 2)
            }
            drift > SYNC_THRESHOLD_MS -> {
                // Audio ahead, speed up audio
                (nominalDelayMs - (drift / 2)).coerceAtLeast(0)
            }
            else -> nominalDelayMs
        }

        Logger.d(TAG, "Audio delay: $nominalDelayMs -> $correction (drift: ${drift}ms)")
        return correction
    }

    /**
     * Reset sync state
     */
    fun reset() {
        audioClockMs = 0
        videoClockMs = 0
        lastSyncTime = 0
        Logger.d(TAG, "Sync state reset")
    }

    /**
     * Get sync statistics
     */
    fun getSyncStats(): SyncStats {
        return SyncStats(
            audioClockMs = audioClockMs,
            videoClockMs = videoClockMs,
            driftMs = getDrift(),
            needsSync = needsSync(),
            masterClock = masterClock
        )
    }

    /**
     * Clock type enum
     */
    enum class ClockType {
        VIDEO,      // Sync audio to video
        AUDIO,      // Sync video to audio
        EXTERNAL    // Sync both to external clock
    }

    /**
     * Sync action sealed class
     */
    sealed class SyncAction {
        object NoAction : SyncAction()
        data class SpeedUpVideo(val driftMs: Long) : SyncAction()
        data class SlowDownVideo(val driftMs: Long) : SyncAction()
        data class SpeedUpAudio(val driftMs: Long) : SyncAction()
        data class SlowDownAudio(val driftMs: Long) : SyncAction()
    }

    /**
     * Sync statistics
     */
    data class SyncStats(
        val audioClockMs: Long,
        val videoClockMs: Long,
        val driftMs: Long,
        val needsSync: Boolean,
        val masterClock: ClockType
    )
}
