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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import pinak.sppunotify.data.local.PreferenceManager
import pinak.sppunotify.worker.ResultSyncWorker
import pinak.sppunotify.worker.RevalSyncWorker
import pinak.sppunotify.worker.WorkManagerHelper
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltAndroidApp
class ResultWatchApp : Application(), Configuration.Provider {

    companion object {
        const val CHANNEL_RESULTS = "result_notifications"
        const val CHANNEL_DOWNLOADS = "download_notifications"
        const val CHANNEL_SYNC_SERVICE = "background_sync"
        const val CHANNEL_REVAL = "reval_notifications"
        const val CHANNEL_EXAM_DATES = "exam_date_notifications"
    }

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var preferenceManager: PreferenceManager

    @Inject
    lateinit var workManagerHelper: WorkManagerHelper

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
        observePreferences()
    }

    private fun observePreferences() {
        applicationScope.launch {
            preferenceManager.preferencesFlow.collectLatest { preferences ->
                workManagerHelper.updateSyncWork(preferences)
            }
        }
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

            val existingSyncChannel = notificationManager.getNotificationChannel(CHANNEL_SYNC_SERVICE)
            if (existingSyncChannel == null) {
                val syncChannel = NotificationChannel(
                    CHANNEL_SYNC_SERVICE,
                    "Background Sync",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Background result sync service"
                    setShowBadge(false)
                }
                notificationManager.createNotificationChannel(syncChannel)
            }

            val existingRevalChannel = notificationManager.getNotificationChannel(CHANNEL_REVAL)
            if (existingRevalChannel == null) {
                val revalChannel = NotificationChannel(
                    CHANNEL_REVAL,
                    "Revaluation Updates",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Notifies when new revaluation courses are added"
                }
                notificationManager.createNotificationChannel(revalChannel)
            }

            val existingExamDateChannel = notificationManager.getNotificationChannel(CHANNEL_EXAM_DATES)
            if (existingExamDateChannel == null) {
                val examDateChannel = NotificationChannel(
                    CHANNEL_EXAM_DATES,
                    "Exam Form Dates",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Notifies when new exam form dates are updated"
                }
                notificationManager.createNotificationChannel(examDateChannel)
            }
        }
    }
}
