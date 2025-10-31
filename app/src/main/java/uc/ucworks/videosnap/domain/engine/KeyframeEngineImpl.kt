package uc.ucworks.videosnap.domain.engine

import uc.ucworks.videosnap.Keyframe
import javax.inject.Inject
import kotlin.math.pow

class KeyframeEngineImpl @Inject constructor() : KeyframeEngine {

    override fun interpolate(keyframes: List<Keyframe>, time: Double): Float {
        if (keyframes.isEmpty()) return 0f
        if (keyframes.size == 1) return keyframes[0].value

        val sorted = keyframes.sortedBy { it.time }

        // Before first keyframe
        if (time <= sorted.first().time) return sorted.first().value

        // After last keyframe
        if (time >= sorted.last().time) return sorted.last().value

        // Find surrounding keyframes
        var leftIndex = 0
        var rightIndex = sorted.size - 1

        for (i in 0 until sorted.size - 1) {
            if (time >= sorted[i].time && time <= sorted[i + 1].time) {
                leftIndex = i
                rightIndex = i + 1
                break
            }
        }

        val left = sorted[leftIndex]
        val right = sorted[rightIndex]

        // Linear interpolation
        val progress = ((time - left.time) / (right.time - left.time)).toFloat()
        return left.value + (right.value - left.value) * progress
    }

    override fun interpolateWithEasing(
        keyframes: List<Keyframe>,
        time: Double,
        easingFunction: EasingFunction
    ): Float {
        if (keyframes.isEmpty()) return 0f
        if (keyframes.size == 1) return keyframes[0].value

        val sorted = keyframes.sortedBy { it.time }

        // Before first keyframe
        if (time <= sorted.first().time) return sorted.first().value

        // After last keyframe
        if (time >= sorted.last().time) return sorted.last().value

        // Find surrounding keyframes
        var leftIndex = 0
        var rightIndex = sorted.size - 1

        for (i in 0 until sorted.size - 1) {
            if (time >= sorted[i].time && time <= sorted[i + 1].time) {
                leftIndex = i
                rightIndex = i + 1
                break
            }
        }

        val left = sorted[leftIndex]
        val right = sorted[rightIndex]

        // Calculate progress (0 to 1)
        val linearProgress = ((time - left.time) / (right.time - left.time)).toFloat()

        // Apply easing function
        val easedProgress = applyEasing(linearProgress, easingFunction)

        return left.value + (right.value - left.value) * easedProgress
    }

    override fun addKeyframe(keyframes: List<Keyframe>, newKeyframe: Keyframe): List<Keyframe> {
        val mutable = keyframes.toMutableList()

        // Remove existing keyframe at same time
        mutable.removeAll { it.time == newKeyframe.time }

        // Add new keyframe
        mutable.add(newKeyframe)

        // Sort by time
        return mutable.sortedBy { it.time }
    }

    override fun removeKeyframe(keyframes: List<Keyframe>, time: Double): List<Keyframe> {
        return keyframes.filter { it.time != time }
    }

    private fun applyEasing(t: Float, easingFunction: EasingFunction): Float {
        return when (easingFunction) {
            EasingFunction.LINEAR -> t

            EasingFunction.EASE_IN -> t * t

            EasingFunction.EASE_OUT -> t * (2 - t)

            EasingFunction.EASE_IN_OUT -> {
                if (t < 0.5f) {
                    2 * t * t
                } else {
                    -1 + (4 - 2 * t) * t
                }
            }

            EasingFunction.EASE_IN_CUBIC -> t * t * t

            EasingFunction.EASE_OUT_CUBIC -> {
                val t1 = t - 1
                t1 * t1 * t1 + 1
            }

            EasingFunction.EASE_IN_OUT_CUBIC -> {
                if (t < 0.5f) {
                    4 * t * t * t
                } else {
                    val t1 = t - 1
                    (t1) * (2 * t - 2).pow(2) + 1
                }
            }

            EasingFunction.BEZIER -> {
                // Cubic bezier with control points (0.42, 0, 0.58, 1)
                cubicBezier(t, 0.42f, 0f, 0.58f, 1f)
            }
        }
    }

    private fun cubicBezier(t: Float, p1x: Float, p1y: Float, p2x: Float, p2y: Float): Float {
        // Simplified cubic bezier calculation
        val u = 1 - t
        return 3 * u * u * t * p1y +
                3 * u * t * t * p2y +
                t * t * t
    }
}
