package uc.ucworks.videosnap.domain.engine

import uc.ucworks.videosnap.Keyframe

/**
 * Keyframe engine for interpolating animated values
 */
interface KeyframeEngine {
    /**
     * Interpolate value at given time using keyframes
     */
    fun interpolate(keyframes: List<Keyframe>, time: Double): Float

    /**
     * Interpolate with easing
     */
    fun interpolateWithEasing(
        keyframes: List<Keyframe>,
        time: Double,
        easingFunction: EasingFunction
    ): Float

    /**
     * Add keyframe to list (sorted by time)
     */
    fun addKeyframe(keyframes: List<Keyframe>, newKeyframe: Keyframe): List<Keyframe>

    /**
     * Remove keyframe at specific time
     */
    fun removeKeyframe(keyframes: List<Keyframe>, time: Double): List<Keyframe>
}

enum class EasingFunction {
    LINEAR,
    EASE_IN,
    EASE_OUT,
    EASE_IN_OUT,
    EASE_IN_CUBIC,
    EASE_OUT_CUBIC,
    EASE_IN_OUT_CUBIC,
    BEZIER
}
