package uc.ucworks.videosnap.domain.engine

import android.graphics.Bitmap
import uc.ucworks.videosnap.VideoEffect

/**
 * Effects engine for applying video effects
 */
interface EffectsEngine {
    /**
     * Apply effect to a bitmap frame
     */
    suspend fun applyEffect(frame: Bitmap, effect: VideoEffect): Bitmap

    /**
     * Apply multiple effects in sequence
     */
    suspend fun applyEffects(frame: Bitmap, effects: List<VideoEffect>): Bitmap

    /**
     * Check if effect requires GPU
     */
    fun requiresGPU(effect: VideoEffect): Boolean

    /**
     * Get effect preview (low quality, fast)
     */
    suspend fun getEffectPreview(frame: Bitmap, effect: VideoEffect): Bitmap
}
