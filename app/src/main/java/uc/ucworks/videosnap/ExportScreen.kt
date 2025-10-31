package uc.ucworks.videosnap

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.NotificationCompat
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf

/**
 * A composable that displays the export screen.
 *
 * @param presets A list of export presets.
 * @param onExport A callback that is invoked when the user selects an export preset.
 */
@Composable
fun ExportScreen(presets: List<ExportPreset>, onExport: (ExportPreset) -> Unit) {
    val context = LocalContext.current
    var showProgress by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf(0f) }

    Column {
        Text(text = "Export Video")
        LazyColumn {
            items(presets) {
                preset ->
                Row {
                    Text(text = preset.name)
                    Button(onClick = {
                        onExport(preset)
                        showProgress = true

                        val workRequest = OneTimeWorkRequestBuilder<ExportWorker>()
                            .setInputData(workDataOf("inPath" to "/sdcard/video.mp4", "outPath" to "/sdcard/exported.mp4", "preset" to preset.name))
                            .addTag("export")
                            .build()

                        val workManager = WorkManager.getInstance(context)
                        workManager.enqueue(workRequest)

                        // TODO: Observe work progress using LaunchedEffect instead
                    }) {
                        Text(text = "Export")
                    }
                }
            }
        }
        if (showProgress) {
            LinearProgressIndicator(progress = progress)
        }
    }
}

/**
 * Shows a notification when the export is complete.
 *
 * @param context The context.
 */
fun showExportNotification(context: Context) {
    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channel = NotificationChannel("export_channel", "Export", NotificationManager.IMPORTANCE_DEFAULT)
        notificationManager.createNotificationChannel(channel)
    }

    val notification = NotificationCompat.Builder(context, "export_channel")
        .setContentTitle("Export Complete")
        .setContentText("Your video has been exported.")
        .setSmallIcon(R.drawable.ic_launcher_foreground)
        .build()

    notificationManager.notify(1, notification)
}
