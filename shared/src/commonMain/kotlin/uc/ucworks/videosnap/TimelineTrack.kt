package uc.ucworks.videosnap

/**
 * A data class that represents a timeline track with comprehensive features.
 *
 * @property id Unique identifier for the track.
 * @property name Name of the track.
 * @property clips A list of timeline clips.
 * @property type Type of track (VIDEO, AUDIO, TEXT, OVERLAY).
 * @property isLocked Whether the track is locked from editing.
 * @property isMuted Whether the track audio is muted.
 * @property isVisible Whether the track is visible in preview.
 * @property height Height of the track in the timeline UI (in dp).
 * @property volume Master volume for the track (0.0 to 1.0).
 * @property order Display order in timeline (lower = top).
 */
data class TimelineTrack(
    val id: String = System.currentTimeMillis().toString(),
    val name: String = "Track",
    val clips: List<TimelineClip> = emptyList(),
    val type: TrackType = TrackType.VIDEO,
    val isLocked: Boolean = false,
    val isMuted: Boolean = false,
    val isVisible: Boolean = true,
    val height: Int = 80,
    val volume: Float = 1.0f,
    val order: Int = 0
)

/**
 * Track type enumeration.
 */
enum class TrackType {
    VIDEO,
    AUDIO,
    TEXT,
    OVERLAY
}
