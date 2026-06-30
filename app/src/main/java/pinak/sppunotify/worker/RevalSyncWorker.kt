package pinak.sppunotify.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.ExistingWorkPolicy
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import pinak.sppunotify.R
import pinak.sppunotify.data.local.NotificationHistoryDao
import pinak.sppunotify.data.repository.RevalRepository
import pinak.sppunotify.util.NotificationHelper
import kotlinx.coroutines.flow.first

@HiltWorker
class RevalSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val repository: RevalRepository,
    private val preferenceManager: pinak.sppunotify.data.local.PreferenceManager,
    private val syncLogDao: pinak.sppunotify.data.local.SyncLogDao,
    private val notificationHistoryDao: NotificationHistoryDao
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        setForeground(createForegroundInfo())

        if (!isNetworkAvailable()) {
            Log.w(TAG, "No network — skipping reval sync")
            return Result.success()
        }

        return try {
            val currentPrefs = preferenceManager.preferencesFlow.first()
            if (!currentPrefs.notificationsEnabled) return Result.success()

            val newCourses = repository.checkForNewCourses()
            if (newCourses.isNotEmpty()) {
                val helper = NotificationHelper(applicationContext, notificationHistoryDao)
                helper.showRevalNotification(newCourses.size)
            }
            syncLogDao.insertLog(pinak.sppunotify.data.local.SyncLogEntity(type = "REVAL", status = "SUCCESS", message = "${newCourses.size} new reval courses"))
            rescheduleIfNeeded()
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Reval sync failed", e)
            syncLogDao.insertLog(pinak.sppunotify.data.local.SyncLogEntity(type = "REVAL", status = "FAILED", message = e.message ?: "Unknown error"))
            if (isServerDown(e) || runAttemptCount >= 3) {
                rescheduleIfNeeded()
                Result.success()
            } else {
                Result.retry()
            }
        }
    }

    private suspend fun rescheduleIfNeeded() {
        val prefs = preferenceManager.preferencesFlow.first()
        if (prefs.notificationsEnabled && prefs.syncInterval < 15) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            val request = OneTimeWorkRequestBuilder<RevalSyncWorker>()
                .setInitialDelay(prefs.syncInterval.toLong(), TimeUnit.MINUTES)
                .setConstraints(constraints)
                .build()
            WorkManager.getInstance(applicationContext).enqueueUniqueWork(
                WorkManagerHelper.REVAL_SYNC_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                request
            )
        }
    }

    private fun isNetworkAvailable(): Boolean {
        val cm = applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        return cm.getNetworkCapabilities(cm.activeNetwork)
            ?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
    }

    private fun isServerDown(e: Exception): Boolean {
        val msg = e.message?.lowercase() ?: return false
        return msg.contains("502") || msg.contains("503") || msg.contains("504") ||
               msg.contains("timeout") || msg.contains("unreachable") ||
               !isNetworkAvailable()
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val mgr = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (mgr.getNotificationChannel(NotificationHelper.CHANNEL_REVAL) == null) {
                mgr.createNotificationChannel(NotificationChannel(
                    NotificationHelper.CHANNEL_REVAL, "Revaluation Updates", NotificationManager.IMPORTANCE_HIGH
                ).apply { description = "Notifies when new revaluation courses are added" })
            }
        }
    }

    private fun createForegroundInfo(): ForegroundInfo {
        ensureChannel()
        val notification = NotificationCompat.Builder(applicationContext, NotificationHelper.CHANNEL_REVAL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Checking Revaluation Results")
            .setContentText("SPPU Result Watch is monitoring revaluation")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setSilent(true)
            .build()
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(3, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(3, notification)
        }
    }

    companion object {
        private const val TAG = "RevalSyncWorker"
    }
}
