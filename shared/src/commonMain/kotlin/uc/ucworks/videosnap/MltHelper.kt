package uc.ucworks.videosnap

/**
 * A helper object for interacting with the MLT framework.
 */
object MltHelper {
    init {
        System.loadLibrary("mlt")
    }

    /**
     * Gets video information from a file.
     *
     * @param filePath The path to the video file.
     * @return A [Result] containing the video information as a string, or an exception if an error occurred.
     */
    fun getVideoInfo(filePath: String): Result<String> {
        return try {
            Result.success(getVideoInfoNative(filePath))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Trims a video to the specified start and end times.
     *
     * @param inPath The path to the input video file.
     * @param outPath The path to the output video file.
     * @param start The start time in seconds.
     * @param end The end time in seconds.
     * @return A [Result] containing [Unit] if the operation was successful, or an exception if an error occurred.
     */
    fun trimVideo(inPath: String, outPath: String, start: Double, end: Double): Result<Unit> {
        return try {
            trimVideoNative(inPath, outPath, start, end)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Splits a video at the specified split point.
     *
     * @param inPath The path to the input video file.
     * @param outPath1 The path to the first output video file.
     * @param outPath2 The path to the second output video file.
     * @param splitPoint The split point in seconds.
     * @return A [Result] containing [Unit] if the operation was successful, or an exception if an error occurred.
     */
    fun splitVideo(inPath: String, outPath1: String, outPath2: String, splitPoint: Double): Result<Unit> {
        return try {
            splitVideoNative(inPath, outPath1, outPath2, splitPoint)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Applies an effect to a video.
     *
     * @param inPath The path to the input video file.
     * @param outPath The path to the output video file.
     * @param effect The name of the effect to apply.
     * @return A [Result] containing [Unit] if the operation was successful, or an exception if an error occurred.
     */
    fun applyEffect(inPath: String, outPath: String, effect: String): Result<Unit> {
        return try {
            applyEffectNative(inPath, outPath, effect)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Exports a video with the specified preset.
     *
     * @param inPath The path to the input video file.
     * @param outPath The path to the output video file.
     * @param preset The export preset to use.
     * @return A [Result] containing [Unit] if the operation was successful, or an exception if an error occurred.
     */
    fun exportVideo(inPath: String, outPath: String, preset: ExportPreset): Result<Unit> {
        return try {
            exportVideoNative(inPath, outPath, preset)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Mixes multiple audio tracks into a single audio track.
     *
     * @param inPaths A list of paths to the input audio files.
     * @param outPath The path to the output audio file.
     * @return A [Result] containing [Unit] if the operation was successful, or an exception if an error occurred.
     */
    fun mixAudio(inPaths: List<String>, outPath: String): Result<Unit> {
        return try {
            mixAudioNative(inPaths.toTypedArray(), outPath)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Applies a keyframe effect to a video.
     *
     * @param inPath The path to the input video file.
     * @param outPath The path to the output video file.
     * @param property The name of the property to animate.
     * @param keyframes A list of keyframes.
     * @return A [Result] containing [Unit] if the operation was successful, or an exception if an error occurred.
     */
    fun applyKeyframeEffect(inPath: String, outPath: String, property: String, keyframes: List<Keyframe>): Result<Unit> {
        return try {
            applyKeyframeEffectNative(inPath, outPath, property, keyframes.toTypedArray())
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Applies a text overlay to a video.
     *
     * @param inPath The path to the input video file.
     * @param outPath The path to the output video file.
     * @param textOverlay The text overlay to apply.
     * @return A [Result] containing [Unit] if the operation was successful, or an exception if an error occurred.
     */
    fun applyTextOverlay(inPath: String, outPath: String, textOverlay: TextOverlayData): Result<Unit> {
        return try {
            applyTextOverlayNative(inPath, outPath, textOverlay.text, textOverlay.x, textOverlay.y, textOverlay.fontSize, textOverlay.color.toArgb())
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Applies a Picture-in-Picture (PiP) effect to a video.
     *
     * @param backgroundPath The path to the background video file.
     * @param foregroundPath The path to the foreground video file.
     * @param outPath The path to the output video file.
     * @param x The x-coordinate of the PiP window.
     * @param y The y-coordinate of the PiP window.
     * @param width The width of the PiP window.
     * @param height The height of the PiP window.
     * @return A [Result] containing [Unit] if the operation was successful, or an exception if an error occurred.
     */
    fun applyPiP(backgroundPath: String, foregroundPath: String, outPath: String, x: Int, y: Int, width: Int, height: Int): Result<Unit> {
        return try {
            applyPiPNative(backgroundPath, foregroundPath, outPath, x, y, width, height)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Applies a speed ramp to a video.
     *
     * @param inPath The path to the input video file.
     * @param outPath The path to the output video file.
     * @param speedMap A string representing the speed map.
     * @return A [Result] containing [Unit] if the operation was successful, or an exception if an error occurred.
     */
    fun applySpeedRamp(inPath: String, outPath: String, speedMap: String): Result<Unit> {
        return try {
            applySpeedRampNative(inPath, outPath, speedMap)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Tracks the motion of an object in a video.
     *
     * @param inPath The path to the input video file.
     * @param outPath The path to the output file for the tracking data.
     * @param x The x-coordinate of the bounding box.
     * @param y The y-coordinate of the bounding box.
     * @param width The width of the bounding box.
     * @param height The height of the bounding box.
     * @return A [Result] containing the tracking data as a string, or an exception if an error occurred.
     */
    fun trackMotion(inPath: String, outPath: String, x: Int, y: Int, width: Int, height: Int): Result<String> {
        return try {
            Result.success(trackMotionNative(inPath, outPath, x, y, width, height))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private external fun getVideoInfoNative(filePath: String): String
    private external fun trimVideoNative(inPath: String, outPath: String, start: Double, end: Double)
    private external fun splitVideoNative(inPath: String, outPath1: String, outPath2: String, splitPoint: Double)
    private external fun applyEffectNative(inPath: String, outPath: String, effect: String)
    private external fun exportVideoNative(inPath: String, outPath: String, preset: ExportPreset)
    private external fun mixAudioNative(inPaths: Array<String>, outPath: String)
    private external fun applyKeyframeEffectNative(inPath: String, outPath: String, property: String, keyframes: Array<Keyframe>)
    private external fun applyTextOverlayNative(inPath: String, outPath: String, text: String, x: Int, y: Int, fontSize: Int, color: Int)
    private external fun applyPiPNative(backgroundPath: String, foregroundPath: String, outPath: String, x: Int, y: Int, width: Int, height: Int)
    private external fun applySpeedRampNative(inPath: String, outPath: String, speedMap: String)
    private external fun trackMotionNative(inPath: String, outPath: String, x: Int, y: Int, width: Int, height: Int): String
}
