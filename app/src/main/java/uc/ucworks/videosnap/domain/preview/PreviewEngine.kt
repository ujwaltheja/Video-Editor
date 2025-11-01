package uc.ucworks.videosnap.domain.preview

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.view.Surface
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import uc.ucworks.videosnap.domain.VideoProject
import uc.ucworks.videosnap.domain.codec.VideoMetadata
import uc.ucworks.videosnap.domain.processing.VideoProcessor
import uc.ucworks.videosnap.util.Logger
import uc.ucworks.videosnap.util.PerformanceMonitor

/**
 * Real-time preview engine for video playback with effects
 */
class PreviewEngine(private val context: Context) {

    companion object {
        private const val TAG = "PreviewEngine"
        private const val FRAME_RATE = 30 // Target frame rate
        private const val FRAME_INTERVAL_MS = 1000L / FRAME_RATE
    }

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var videoProcessor: VideoProcessor? = null
    private var performanceMonitor: PerformanceMonitor? = null

    private val _playbackState = MutableStateFlow<PlaybackState>(PlaybackState.Idle)
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()

    private var playbackJob: Job? = null
    private var currentProject: VideoProject? = null
    private var metadata: VideoMetadata? = null

    /**
     * Initialize preview with video
     */
    suspend fun initialize(uri: Uri): Result<VideoMetadata> {
        return withContext(Dispatchers.IO) {
            try {
                Logger.d(TAG, "Initializing preview engine")

                videoProcessor = VideoProcessor(context)
                performanceMonitor = PerformanceMonitor.getInstance(context)

                val result = videoProcessor!!.initialize(uri)
                if (result.isFailure) {
                    return@withContext Result.failure(result.exceptionOrNull()!!)
                }

                metadata = result.getOrNull()!!
                _playbackState.value = PlaybackState.Ready

                Logger.d(TAG, "Preview engine initialized")
                Result.success(metadata!!)
            } catch (e: Exception) {
                Logger.e(TAG, "Failed to initialize preview", e)
                _playbackState.value = PlaybackState.Error(e.message ?: "Unknown error")
                Result.failure(e)
            }
        }
    }

    /**
     * Start playback
     */
    fun play() {
        if (_playbackState.value != PlaybackState.Ready && _playbackState.value !is PlaybackState.Paused) {
            Logger.w(TAG, "Cannot play in current state: ${_playbackState.value}")
            return
        }

        _playbackState.value = PlaybackState.Playing
        startPlayback()
    }

    /**
     * Pause playback
     */
    fun pause() {
        if (_playbackState.value != PlaybackState.Playing) {
            return
        }

        playbackJob?.cancel()
        _playbackState.value = PlaybackState.Paused(_currentPosition.value)
        Logger.d(TAG, "Playback paused at ${_currentPosition.value}ms")
    }

    /**
     * Seek to position
     */
    suspend fun seekTo(positionMs: Long) {
        withContext(Dispatchers.IO) {
            val wasPlaying = _playbackState.value == PlaybackState.Playing
            if (wasPlaying) {
                pause()
            }

            _currentPosition.value = positionMs
            videoProcessor?.seekTo(positionMs * 1000) // Convert to microseconds

            if (wasPlaying) {
                play()
            }

            Logger.d(TAG, "Seeked to ${positionMs}ms")
        }
    }

    /**
     * Get frame at current position
     */
    suspend fun getCurrentFrame(): Result<Bitmap?> {
        return withContext(Dispatchers.IO) {
            try {
                val processor = videoProcessor
                    ?: return@withContext Result.failure(IllegalStateException("Not initialized"))

                val timestampUs = _currentPosition.value * 1000
                val frameResult = processor.getFrameAt(timestampUs)

                if (frameResult.isFailure) {
                    return@withContext Result.failure(frameResult.exceptionOrNull()!!)
                }

                val frame = frameResult.getOrNull()!!

                // Convert frame to bitmap
                // This is simplified - in real implementation, we'd use GPU for YUV to RGB conversion
                val bitmap = Bitmap.createBitmap(frame.width, frame.height, Bitmap.Config.ARGB_8888)

                // Apply effects from project if available
                val processedBitmap = currentProject?.let { project ->
                    applyEffects(bitmap, project, _currentPosition.value)
                } ?: bitmap

                Result.success(processedBitmap)
            } catch (e: Exception) {
                Logger.e(TAG, "Failed to get current frame", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Set project for preview
     */
    fun setProject(project: VideoProject) {
        currentProject = project
        Logger.d(TAG, "Project set for preview")
    }

    /**
     * Get video metadata
     */
    fun getMetadata(): VideoMetadata? = metadata

    /**
     * Get duration in milliseconds
     */
    fun getDuration(): Long = metadata?.duration?.div(1000) ?: 0L

    /**
     * Stop playback and cleanup
     */
    fun stop() {
        playbackJob?.cancel()
        _playbackState.value = PlaybackState.Idle
        _currentPosition.value = 0L
        Logger.d(TAG, "Playback stopped")
    }

    /**
     * Cleanup resources
     */
    fun cleanup() {
        stop()
        scope.cancel()
        videoProcessor?.cleanup()
        performanceMonitor?.stopMonitoring()
        videoProcessor = null
        metadata = null
        currentProject = null
        Logger.d(TAG, "Preview engine cleaned up")
    }

    /**
     * Start playback loop
     */
    private fun startPlayback() {
        playbackJob?.cancel()
        playbackJob = scope.launch {
            val duration = getDuration()
            var lastFrameTime = System.currentTimeMillis()

            Logger.d(TAG, "Starting playback loop")

            while (isActive && _playbackState.value == PlaybackState.Playing) {
                val now = System.currentTimeMillis()
                val elapsed = now - lastFrameTime

                if (elapsed >= FRAME_INTERVAL_MS) {
                    val newPosition = _currentPosition.value + elapsed
                    if (newPosition >= duration) {
                        // End of video
                        _currentPosition.value = 0L
                        _playbackState.value = PlaybackState.Ready
                        break
                    }

                    _currentPosition.value = newPosition
                    lastFrameTime = now

                    // Record frame for FPS tracking
                    performanceMonitor?.recordFrame()
                }

                // Small delay to prevent busy-waiting
                delay(5)
            }

            Logger.d(TAG, "Playback loop ended")
        }
    }

    /**
     * Apply effects to frame (placeholder - will be implemented with actual effect pipeline)
     */
    private suspend fun applyEffects(
        bitmap: Bitmap,
        project: VideoProject,
        timestampMs: Long
    ): Bitmap {
        // This would integrate with the effects engine
        // For now, just return the original bitmap
        return bitmap
    }

    /**
     * Playback state
     */
    sealed class PlaybackState {
        object Idle : PlaybackState()
        object Ready : PlaybackState()
        object Playing : PlaybackState()
        data class Paused(val position: Long) : PlaybackState()
        data class Error(val message: String) : PlaybackState()
    }
}
