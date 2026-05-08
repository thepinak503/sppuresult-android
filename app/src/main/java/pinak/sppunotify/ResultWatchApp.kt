package pinak.sppunotify

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.HiltAndroidApp
import pinak.sppunotify.worker.ResultSyncWorker
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltAndroidApp
class ResultWatchApp : Application(), Configuration.Provider {

    companion object {
        const val CHANNEL_RESULTS = "result_notifications"
        const val CHANNEL_DOWNLOADS = "download_notifications"
    }

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
        setupRecurringWork()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(NotificationManager::class.java)

            val existingResultChannel = notificationManager.getNotificationChannel(CHANNEL_RESULTS)
            if (existingResultChannel == null) {
                val resultChannel = NotificationChannel(
                    CHANNEL_RESULTS,
                    "Result Updates",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Notifies when new results are published"
                }
                notificationManager.createNotificationChannel(resultChannel)
            }

            val existingDownloadChannel = notificationManager.getNotificationChannel(CHANNEL_DOWNLOADS)
            if (existingDownloadChannel == null) {
                val downloadChannel = NotificationChannel(
                    CHANNEL_DOWNLOADS,
                    "Result Downloads",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Status of result downloads"
                }
                notificationManager.createNotificationChannel(downloadChannel)
            }
        }
    }

    private fun setupRecurringWork() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val request = PeriodicWorkRequestBuilder<ResultSyncWorker>(
            15, TimeUnit.MINUTES
        ).setConstraints(constraints).build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "ResultSyncWork",
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }
}
