package pinak.sppunotify.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.TaskStackBuilder
import androidx.core.net.toUri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import pinak.sppunotify.MainActivity
import pinak.sppunotify.R
import pinak.sppunotify.data.local.NotificationHistoryDao
import pinak.sppunotify.data.local.NotificationHistoryEntity

/**
 * A single result item passed from workers to the notification helper.
 * Includes the result ID for deep linking to the detail screen.
 */
data class NotificationResult(
    val title: String,
    val message: String,
    val resultId: String = "",
)

private const val DEEP_LINK_SCHEME = "sppuwatch"
private const val DEEP_LINK_HOST = "notify"

class NotificationHelper(
    private val context: Context,
    private val historyDao: NotificationHistoryDao? = null
) {
    private val notificationScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

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
                setShowBadge(true)
            }

            val priorityChannel = NotificationChannel(
                CHANNEL_PRIORITY,
                "Priority Watchlist",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "High-priority alerts for starred keywords"
                enableLights(true)
                lightColor = android.graphics.Color.RED
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 200, 500)
                setSound(android.provider.Settings.System.DEFAULT_NOTIFICATION_URI, null)
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

            val circularChannel = NotificationChannel(
                CHANNEL_CIRCULARS,
                "University Circulars",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifies when new university circulars are published!"
            }

            val newsChannel = NotificationChannel(
                CHANNEL_NEWS,
                "App Announcements",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "News and updates about the app"
            }
            
            notificationManager.createNotificationChannel(resultChannel)
            notificationManager.createNotificationChannel(priorityChannel)
            notificationManager.createNotificationChannel(downloadChannel)
            notificationManager.createNotificationChannel(revalChannel)
            notificationManager.createNotificationChannel(examDateChannel)
            notificationManager.createNotificationChannel(circularChannel)
            notificationManager.createNotificationChannel(newsChannel)
        }
    }

    // ── Deep link helpers ────────────────────────────────────────────────────

    private fun createDeepLinkIntent(uri: String): Intent {
        return Intent(
            Intent.ACTION_VIEW,
            "$DEEP_LINK_SCHEME://$DEEP_LINK_HOST/$uri".toUri(),
            context,
            MainActivity::class.java,
        )
    }

    private fun createDeepLinkPendingIntent(
        uri: String,
        requestCode: Int = 0,
    ): PendingIntent? {
        val intent = createDeepLinkIntent(uri)
        return TaskStackBuilder.create(context).run {
            addNextIntentWithParentStack(intent)
            getPendingIntent(requestCode, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        }
    }

    // ── News / Announcements ─────────────────────────────────────────────────

    fun showNewsNotification(title: String, message: String, type: String = "ANNOUNCEMENT") {
        val pendingIntent = createDeepLinkPendingIntent("home")

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
        trackNotification("NEWS", title, message)
    }

    // ── Revaluation → opens Revaluation tab ──────────────────────────────────

    fun showRevalNotification(count: Int) {
        val pendingIntent = createDeepLinkPendingIntent("reval")

        val notification = NotificationCompat.Builder(context, CHANNEL_REVAL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("New Revaluation Courses")
            .setContentText("$count new revaluation course(s) published")
            .setStyle(NotificationCompat.BigTextStyle().bigText("$count new revaluation course(s) have been published. Tap to view."))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        notificationManager.notify(REVAL_NOTIFICATION_ID, notification)
        trackNotification("REVAL", "New Revaluation Courses", "$count course(s)")
    }

    // ── Results → opens result detail screen ─────────────────────────────────

    fun showResultNotification(results: List<NotificationResult>) {
        showGenericResultNotification(results, CHANNEL_RESULTS, GROUP_RESULTS, isPriority = false)
    }

    fun showPriorityResultNotification(results: List<NotificationResult>) {
        showGenericResultNotification(results, CHANNEL_PRIORITY, GROUP_PRIORITY, isPriority = true)
    }

    private fun showGenericResultNotification(
        results: List<NotificationResult>,
        channelId: String,
        groupKey: String,
        isPriority: Boolean,
    ) {
        if (results.size == 1) {
            val item = results.first()
            val uri = if (item.resultId.isNotEmpty()) "details/${item.resultId}" else "home"
            val pendingIntent = createDeepLinkPendingIntent(uri, item.resultId.hashCode())

            val bigText = if (isPriority) {
                "⭐ STARRED MATCH ⭐\n\n${item.message}"
            } else {
                "📄 ${item.message}\n🔗 Tap to view details"
            }

            val notification = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(item.title)
                .setContentText(item.message)
                .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setGroup(groupKey)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .apply {
                    if (isPriority) {
                        setCategory(NotificationCompat.CATEGORY_ALARM)
                        setDefaults(NotificationCompat.DEFAULT_ALL)
                    }
                }
                .build()
            notificationManager.notify(item.message.hashCode(), notification)
            trackNotification(if (isPriority) "PRIORITY" else "RESULT", item.title, item.message)
        } else {
            val summaryText = "${results.size} new result(s) published"
            results.forEach { item ->
                val uri = if (item.resultId.isNotEmpty()) "details/${item.resultId}" else "home"
                val pendingIntent = createDeepLinkPendingIntent(uri, item.resultId.hashCode())

                val bigText = if (isPriority) {
                    "⭐ STARRED MATCH ⭐\n\n${item.message}"
                } else {
                    "📄 ${item.message}\n🔗 Tap to view details"
                }

                val notification = NotificationCompat.Builder(context, channelId)
                    .setSmallIcon(R.drawable.ic_notification)
                    .setContentTitle(item.title)
                    .setContentText(item.message)
                    .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setGroup(groupKey)
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true)
                    .build()
                notificationManager.notify(item.message.hashCode(), notification)
                trackNotification(if (isPriority) "PRIORITY" else "RESULT", item.title, item.message)
            }

            val summaryPendingIntent = createDeepLinkPendingIntent("home", SUMMARY_ID)
            val summary = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(if (isPriority) "Priority Results Available" else "New Results Available")
                .setContentText(summaryText)
                .setStyle(NotificationCompat.BigTextStyle().bigText("$summaryText\n\n${results.joinToString("\n") { it.message }}"))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setGroup(groupKey)
                .setGroupSummary(true)
                .setContentIntent(summaryPendingIntent)
                .setAutoCancel(true)
                .build()

            notificationManager.notify(if (isPriority) PRIORITY_SUMMARY_ID else SUMMARY_ID, summary)
        }
    }

    // ── Downloads ────────────────────────────────────────────────────────────

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

    // ── Exam Dates → opens Exam Dates tab ────────────────────────────────────

    fun showExamDateNotification(newDates: List<String>) {
        if (newDates.isEmpty()) return

        val pendingIntent = createDeepLinkPendingIntent("exam_dates")

        val contentText = if (newDates.size == 1) {
            newDates.first()
        } else {
            "${newDates.size} new exam form dates updated"
        }

        val bigText = "📅 Exam Form Dates:\n" + newDates.joinToString("\n")

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
        trackNotification("EXAM_DATE", "New Exam Form Dates", "${newDates.size} date(s)")
    }

    // ── Circulars → opens Circulars tab ──────────────────────────────────────

    fun showCircularNotification(count: Int) {
        val pendingIntent = createDeepLinkPendingIntent("circulars")

        val notification = NotificationCompat.Builder(context, CHANNEL_CIRCULARS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("New University Circulars")
            .setContentText("$count new circular(s) published")
            .setStyle(NotificationCompat.BigTextStyle().bigText("$count new circular(s) have been published on the university website. Tap to view."))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        notificationManager.notify(CIRCULAR_NOTIFICATION_ID, notification)
        trackNotification("CIRCULAR", "New University Circulars", "$count circular(s)")
    }

    // ── Updates ──────────────────────────────────────────────────────────────

    fun showUpdateNotification(versionName: String, updateUrl: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(updateUrl)).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_NEWS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Update Available: v$versionName")
            .setContentText("A new version of SPPU Result Watch is available. Tap to update.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .build()
        
        notificationManager.notify(UPDATE_NOTIFICATION_ID, notification)
    }

    // ── History tracking ─────────────────────────────────────────────────────

    private fun trackNotification(type: String, title: String, message: String) {
        historyDao?.let { dao ->
            notificationScope.launch {
                dao.insert(
                    NotificationHistoryEntity(
                        title = title,
                        message = message,
                        type = type
                    )
                )
                val weekAgo = System.currentTimeMillis() - (7L * 24 * 60 * 60 * 1000)
                dao.deleteOld(weekAgo)
            }
        }
    }

    companion object {
        const val CHANNEL_RESULTS = "result_notifications"
        const val CHANNEL_DOWNLOADS = "download_notifications"
        const val CHANNEL_REVAL = "reval_notifications"
        const val CHANNEL_EXAM_DATES = "exam_date_notifications"
        const val CHANNEL_CIRCULARS = "circular_notifications"
        const val CHANNEL_PRIORITY = "priority_notifications"
        const val CHANNEL_NEWS = "news_notifications"
        const val GROUP_RESULTS = "pinak.sppunotify.RESULTS"
        const val GROUP_PRIORITY = "pinak.sppunotify.PRIORITY"
        const val SUMMARY_ID = 0
        const val PRIORITY_SUMMARY_ID = 1
        const val REVAL_NOTIFICATION_ID = 100
        const val EXAM_DATE_NOTIFICATION_ID = 101
        const val CIRCULAR_NOTIFICATION_ID = 103
        const val UPDATE_NOTIFICATION_ID = 104
        const val NEWS_NOTIFICATION_ID = 102
        const val RESULT_BASE_ID = 1000
    }
}
