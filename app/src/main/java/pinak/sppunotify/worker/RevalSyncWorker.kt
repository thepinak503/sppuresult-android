package pinak.sppunotify.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import pinak.sppunotify.R
import pinak.sppunotify.data.repository.RevalRepository
import pinak.sppunotify.util.NotificationHelper

@HiltWorker
class RevalSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val repository: RevalRepository,
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        setForeground(createForegroundInfo())
        return try {
            val newCourses = repository.checkForNewCourses()
            if (newCourses.isNotEmpty()) {
                val helper = NotificationHelper(applicationContext)
                helper.showRevalNotification(newCourses.size)
            }
            Result.success()
        } catch (e: Exception) {
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
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
}
