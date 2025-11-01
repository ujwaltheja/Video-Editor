package uc.ucworks.videosnap.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import uc.ucworks.videosnap.R

/**
 * Helper for progress notifications
 */
object NotificationHelper {

    private const val CHANNEL_ID = "video_editor_progress"
    private const val CHANNEL_NAME = "Video Processing"
    private const val EXPORT_NOTIFICATION_ID = 1001

    /**
     * Create notification channel (Android 8.0+)
     */
    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notifications for video export progress"
                setSound(null, null)
            }

            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    /**
     * Show export progress notification
     */
    fun showExportProgress(
        context: Context,
        progress: Int,
        max: Int = 100,
        title: String = "Exporting video..."
    ): NotificationCompat.Builder {
        createNotificationChannel(context)

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle(title)
            .setContentText("$progress%")
            .setProgress(max, progress, false)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setSound(null)

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(EXPORT_NOTIFICATION_ID, builder.build())

        return builder
    }

    /**
     * Update export progress
     */
    fun updateExportProgress(context: Context, progress: Int) {
        showExportProgress(context, progress)
    }

    /**
     * Show export complete notification
     */
    fun showExportComplete(context: Context, outputPath: String) {
        createNotificationChannel(context)

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle("Export complete")
            .setContentText("Video saved to $outputPath")
            .setProgress(0, 0, false)
            .setOngoing(false)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(EXPORT_NOTIFICATION_ID, builder.build())
    }

    /**
     * Show export error notification
     */
    fun showExportError(context: Context, errorMessage: String) {
        createNotificationChannel(context)

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setContentTitle("Export failed")
            .setContentText(errorMessage)
            .setProgress(0, 0, false)
            .setOngoing(false)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(EXPORT_NOTIFICATION_ID, builder.build())
    }

    /**
     * Cancel notification
     */
    fun cancelNotification(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.cancel(EXPORT_NOTIFICATION_ID)
    }
}
