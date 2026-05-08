package pinak.sppunotify.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import pinak.sppunotify.MainActivity
import pinak.sppunotify.R

class NotificationHelper(private val context: Context) {

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val resultChannel = NotificationChannel(
                CHANNEL_RESULTS,
                "Result Updates",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifies when new results are published"
            }
            
            val downloadChannel = NotificationChannel(
                CHANNEL_DOWNLOADS,
                "Result Downloads",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Status of result downloads"
            }
            
            notificationManager.createNotificationChannel(resultChannel)
            notificationManager.createNotificationChannel(downloadChannel)
        }
    }

    fun showResultNotification(title: String, message: String) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_RESULTS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setGroup(GROUP_RESULTS)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val summary = NotificationCompat.Builder(context, CHANNEL_RESULTS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("New Results Available")
            .setContentText("Check out the latest SPPU announcements")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setGroup(GROUP_RESULTS)
            .setGroupSummary(true)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(message.hashCode(), notification)
        notificationManager.notify(SUMMARY_ID, summary)
    }

    fun showDownloadNotification(success: Boolean, fileName: String) {
        val notification = NotificationCompat.Builder(context, CHANNEL_DOWNLOADS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(if (success) "Download Successful" else "Download Failed")
            .setContentText(if (success) "Saved: $fileName" else "Failed to save: $fileName")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(fileName.hashCode(), notification)
    }

    companion object {
        const val CHANNEL_RESULTS = "result_notifications"
        const val CHANNEL_DOWNLOADS = "download_notifications"
        const val GROUP_RESULTS = "pinak.sppunotify.RESULTS"
        const val SUMMARY_ID = 0
    }
}
