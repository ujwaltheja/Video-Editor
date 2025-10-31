package uc.ucworks.videosnap.workers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.*
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.last
import uc.ucworks.videosnap.data.repository.ProjectRepository
import uc.ucworks.videosnap.domain.export.*

@HiltWorker
class ExportWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val repository: ProjectRepository,
    private val exportEngine: ExportEngine
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val projectId = inputData.getString(KEY_PROJECT_ID) ?: return Result.failure()
        val presetId = inputData.getString(KEY_PRESET_ID) ?: return Result.failure()
        val outputPath = inputData.getString(KEY_OUTPUT_PATH) ?: return Result.failure()

        createNotificationChannel()

        return try {
            val project = repository.getProjectByIdSuspend(projectId)
                ?: return Result.failure(
                    workDataOf(KEY_ERROR to "Project not found")
                )

            val preset = exportEngine.getAvailablePresets()
                .find { it.id == presetId }
                ?: return Result.failure(
                    workDataOf(KEY_ERROR to "Preset not found")
                )

            setForeground(createForegroundInfo(0))

            val finalProgress = exportEngine.exportProject(project, preset, outputPath)
                .collect { progress ->
                    setProgress(
                        workDataOf(
                            KEY_PROGRESS to progress.progressPercent.toInt(),
                            KEY_STATUS to progress.status.name
                        )
                    )

                    updateNotification(progress)

                    when (progress.status) {
                        ExportStatus.COMPLETED -> {
                            showCompletionNotification(outputPath)
                            return@collect
                        }
                        ExportStatus.ERROR -> {
                            showErrorNotification()
                            return Result.failure(
                                workDataOf(KEY_ERROR to "Export failed")
                            )
                        }
                        ExportStatus.CANCELLED -> {
                            return Result.failure(
                                workDataOf(KEY_ERROR to "Export cancelled")
                            )
                        }
                        else -> {} // Continue
                    }
                }

            Result.success(
                workDataOf(
                    KEY_OUTPUT_PATH to outputPath,
                    KEY_PROGRESS to 100
                )
            )
        } catch (e: Exception) {
            showErrorNotification()
            Result.failure(
                workDataOf(KEY_ERROR to e.message)
            )
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Video Export",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notifications for video export progress"
            }

            val notificationManager =
                applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createForegroundInfo(progress: Int): ForegroundInfo {
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setContentTitle("Exporting Video")
            .setContentText("Export in progress: $progress%")
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setProgress(100, progress, false)
            .setOngoing(true)
            .build()

        return ForegroundInfo(NOTIFICATION_ID, notification)
    }

    private fun updateNotification(progress: ExportProgress) {
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setContentTitle("Exporting Video")
            .setContentText(getStatusText(progress))
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setProgress(100, progress.progressPercent.toInt(), false)
            .setOngoing(true)
            .build()

        val notificationManager =
            applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun showCompletionNotification(outputPath: String) {
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setContentTitle("Export Complete")
            .setContentText("Video saved successfully")
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setAutoCancel(true)
            .build()

        val notificationManager =
            applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID + 1, notification)
    }

    private fun showErrorNotification() {
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setContentTitle("Export Failed")
            .setContentText("Failed to export video")
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setAutoCancel(true)
            .build()

        val notificationManager =
            applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID + 2, notification)
    }

    private fun getStatusText(progress: ExportProgress): String {
        return when (progress.status) {
            ExportStatus.PREPARING -> "Preparing export..."
            ExportStatus.ENCODING -> "Encoding: ${progress.progressPercent.toInt()}%"
            ExportStatus.FINALIZING -> "Finalizing..."
            ExportStatus.COMPLETED -> "Complete"
            ExportStatus.ERROR -> "Error"
            ExportStatus.CANCELLED -> "Cancelled"
        }
    }

    companion object {
        private const val CHANNEL_ID = "video_export_channel"
        private const val NOTIFICATION_ID = 1001

        const val KEY_PROJECT_ID = "project_id"
        const val KEY_PRESET_ID = "preset_id"
        const val KEY_OUTPUT_PATH = "output_path"
        const val KEY_PROGRESS = "progress"
        const val KEY_STATUS = "status"
        const val KEY_ERROR = "error"

        fun createWorkRequest(
            projectId: String,
            presetId: String,
            outputPath: String
        ): OneTimeWorkRequest {
            val inputData = workDataOf(
                KEY_PROJECT_ID to projectId,
                KEY_PRESET_ID to presetId,
                KEY_OUTPUT_PATH to outputPath
            )

            return OneTimeWorkRequestBuilder<ExportWorker>()
                .setInputData(inputData)
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .build()
        }
    }
}
