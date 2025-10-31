package uc.ucworks.videosnap

/**
 * A data class that represents a timeline clip.
 *
 * @property mediaPath The path to the media file.
 * @property startTime The start time of the clip in milliseconds.
 * @property endTime The end time of the clip in milliseconds.
 */
data class TimelineClip(
    val mediaPath: String,
    val startTime: Long,
    val endTime: Long
)
