package uc.ucworks.videosnap.domain.engine

import android.graphics.Bitmap
import uc.ucworks.videosnap.TransitionType

/**
 * Transition engine for applying transitions between clips
 */
interface TransitionEngine {
    /**
     * Apply transition between two frames
     * @param from Starting frame
     * @param to Ending frame
     * @param transitionType Type of transition
     * @param progress Progress from 0.0 to 1.0
     */
    suspend fun applyTransition(
        from: Bitmap,
        to: Bitmap,
        transitionType: TransitionType,
        progress: Float
    ): Bitmap

    /**
     * Get available transitions
     */
    fun getAvailableTransitions(): List<TransitionType>
}
