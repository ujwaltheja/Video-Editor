package uc.ucworks.videosnap.domain

import android.content.Context
import uc.ucworks.videosnap.util.MltHelper

class VideoProcessingEngine(private val context: Context) {

    fun trimVideo(inPath: String, outPath: String, start: Double, end: Double): Result<Unit> {
        return MltHelper.trimVideo(inPath, outPath, start, end)
    }

    fun splitVideo(inPath: String, outPath1: String, outPath2: String, splitPoint: Double): Result<Unit> {
        return MltHelper.splitVideo(inPath, outPath1, outPath2, splitPoint)
    }

    fun applyEffect(inPath: String, outPath: String, effect: String): Result<Unit> {
        return MltHelper.applyEffect(inPath, outPath, effect)
    }

    fun exportVideo(inPath: String, outPath: String, preset: ExportPreset): Result<Unit> {
        return MltHelper.exportVideo(inPath, outPath, preset)
    }
}
