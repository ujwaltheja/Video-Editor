package uc.ucworks.videosnap

/**
 * A data class that represents a timeline clip with comprehensive editing features.
 *
 * @property id Unique identifier for the clip.
 * @property mediaPath The path to the media file.
 * @property startTime The start time of the clip in milliseconds.
 * @property endTime The end time of the clip in milliseconds.
 * @property offsetX Horizontal offset on the timeline in milliseconds.
 * @property offsetY Vertical offset for position in video frame (0.0 to 1.0).
 * @property trimStart Trim start point in the original media (milliseconds).
 * @property trimEnd Trim end point in the original media (milliseconds).
 * @property volume Audio volume (0.0 to 1.0).
 * @property speed Playback speed multiplier (0.1 to 10.0).
 * @property rotation Rotation angle in degrees (0, 90, 180, 270).
 * @property opacity Opacity level (0.0 to 1.0).
 * @property effects List of effect names applied to this clip.
 * @property transitions List of transition effects.
 * @property keyframes List of keyframe animations.
 * @property mediaType Type of media (VIDEO, AUDIO, IMAGE, TEXT).
 * @property zIndex Layer order (higher = on top).
 */
data class TimelineClip(
    val id: String = System.currentTimeMillis().toString(),
    val mediaPath: String,
    val startTime: Long,
    val endTime: Long,
    val offsetX: Long = 0L,
    val offsetY: Float = 0f,
    val trimStart: Long = 0L,
    val trimEnd: Long = -1L,
    val volume: Float = 1.0f,
    val speed: Float = 1.0f,
    val rotation: Int = 0,
    val opacity: Float = 1.0f,
    val effects: List<String> = emptyList(),
    val transitions: List<TransitionEffect> = emptyList(),
    val keyframes: List<Keyframe> = emptyList(),
    val mediaType: MediaType = MediaType.VIDEO,
    val zIndex: Int = 0
) {
    /**
     * Duration of the clip in milliseconds.
     */
    val duration: Long
        get() = endTime - startTime
}

/**
 * Media type enumeration.
 */
enum class MediaType {
    VIDEO,
    AUDIO,
    IMAGE,
    TEXT
}

/**
 * Transition effect data class.
 *
 * @property type Type of transition (FADE, DISSOLVE, WIPE, SLIDE, etc.).
 * @property duration Duration of transition in milliseconds.
 * @property position Position of transition (START, END).
 */
data class TransitionEffect(
    val type: TransitionType,
    val duration: Long,
    val position: TransitionPosition
)

enum class TransitionType {
    FADE,
    DISSOLVE,
    WIPE_LEFT,
    WIPE_RIGHT,
    WIPE_UP,
    WIPE_DOWN,
    SLIDE_LEFT,
    SLIDE_RIGHT,
    ZOOM_IN,
    ZOOM_OUT,
    CIRCLE_OPEN,
    CIRCLE_CLOSE
}

enum class TransitionPosition {
    START,
    END
}
