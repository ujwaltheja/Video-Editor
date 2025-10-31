package uc.ucworks.videosnap

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import kotlinx.coroutines.delay
import uc.ucworks.videosnap.MltHelper
import uc.ucworks.videosnap.DefaultExportPresets

/**
 * A worker that exports a video in the background.
 *
 * @param appContext The application context.
 * @param workerParams The worker parameters.
 */
class ExportWorker(appContext: Context, workerParams: WorkerParameters) : CoroutineWorker(appContext, workerParams) {

    /**
     * The work to be done in the background.
     */
    override suspend fun doWork(): Result {
        val inPath = inputData.getString("inPath")!!
        val outPath = inputData.getString("outPath")!!
        val preset = inputData.getString("preset")!!

        // This is a simplified progress update. A real implementation would get progress from MLT.
        for (i in 1..100) {
            setProgress(workDataOf("progress" to i))
            delay(100)
        }

        MltHelper.exportVideo(inPath, outPath, DefaultExportPresets.presets.first { it.name == preset })

        return Result.success()
    }
}
