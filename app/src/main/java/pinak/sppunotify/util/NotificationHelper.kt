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

            val revalChannel = NotificationChannel(
                CHANNEL_REVAL,
                "Revaluation Updates",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifies when new revaluation courses are added"
            }

            val examDateChannel = NotificationChannel(
                CHANNEL_EXAM_DATES,
                "Exam Form Dates",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifies when new exam form dates are updated"
            }

            val newsChannel = NotificationChannel(
                CHANNEL_NEWS,
                "App Announcements",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "News and updates about the app"
            }
            
            notificationManager.createNotificationChannel(resultChannel)
            notificationManager.createNotificationChannel(downloadChannel)
            notificationManager.createNotificationChannel(revalChannel)
            notificationManager.createNotificationChannel(examDateChannel)
            notificationManager.createNotificationChannel(newsChannel)
        }
    }

    fun showNewsNotification(title: String, message: String, type: String = "ANNOUNCEMENT") {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(context, CHANNEL_NEWS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(if (type == "ALERT") NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .apply {
                if (type == "ALERT") {
                    setCategory(NotificationCompat.CATEGORY_ALARM)
                    setVibrate(longArrayOf(0, 500, 200, 500))
                }
            }
            .build()
        notificationManager.notify(NEWS_NOTIFICATION_ID, notification)
    }

    fun showRevalNotification(count: Int) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_REVAL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("New Revaluation Courses")
            .setContentText("$count new revaluation course(s) published")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        notificationManager.notify(REVAL_NOTIFICATION_ID, notification)
    }

    fun showResultNotification(results: List<Pair<String, String>>) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        if (results.size == 1) {
            val (title, message) = results.first()
            val notification = NotificationCompat.Builder(context, CHANNEL_RESULTS)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setGroup(GROUP_RESULTS)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build()
            notificationManager.notify(message.hashCode(), notification)
        } else {
            val summaryText = "${results.size} new result(s) published"
            results.forEach { (title, message) ->
                val notification = NotificationCompat.Builder(context, CHANNEL_RESULTS)
                    .setSmallIcon(R.drawable.ic_notification)
                    .setContentTitle(title)
                    .setContentText(message)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setGroup(GROUP_RESULTS)
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true)
                    .build()
                notificationManager.notify(message.hashCode(), notification)
            }

            val summary = NotificationCompat.Builder(context, CHANNEL_RESULTS)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("New Results Available")
                .setContentText(summaryText)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setGroup(GROUP_RESULTS)
                .setGroupSummary(true)
                .setAutoCancel(true)
                .build()

            notificationManager.notify(SUMMARY_ID, summary)
        }
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

    fun showExamDateNotification(newDates: List<String>) {
        if (newDates.isEmpty()) return
        
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_IMMUTABLE
        )
        
        val contentText = if (newDates.size == 1) {
            newDates.first()
        } else {
            "${newDates.size} new exam form dates updated"
        }

        val bigText = newDates.joinToString("\n")

        val notification = NotificationCompat.Builder(context, CHANNEL_EXAM_DATES)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("New Exam Form Dates")
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        notificationManager.notify(EXAM_DATE_NOTIFICATION_ID, notification)
    }

    companion object {
        const val CHANNEL_RESULTS = "result_notifications"
        const val CHANNEL_DOWNLOADS = "download_notifications"
        const val CHANNEL_REVAL = "reval_notifications"
        const val CHANNEL_EXAM_DATES = "exam_date_notifications"
        const val CHANNEL_NEWS = "news_notifications"
        const val GROUP_RESULTS = "pinak.sppunotify.RESULTS"
        const val SUMMARY_ID = 0
        const val REVAL_NOTIFICATION_ID = 100
        const val EXAM_DATE_NOTIFICATION_ID = 101
        const val NEWS_NOTIFICATION_ID = 102
    }
}
