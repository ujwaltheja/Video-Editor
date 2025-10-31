package uc.ucworks.videosnap

/**
 * A data class that represents a keyframe.
 *
 * @property time The time of the keyframe in seconds.
 * @property value The value of the keyframe.
 */
data class Keyframe(
    val time: Double,
    val value: Float
)
