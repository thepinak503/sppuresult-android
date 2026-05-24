package pinak.sppunotify.worker

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.ExistingWorkPolicy
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import pinak.sppunotify.data.local.PreferenceManager
import pinak.sppunotify.data.repository.ExamDateRepository
import pinak.sppunotify.util.NotificationHelper
import kotlinx.coroutines.flow.first

@HiltWorker
class ExamDateSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val repository: ExamDateRepository,
    private val preferenceManager: PreferenceManager
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        if (!isNetworkAvailable()) {
            Log.w(TAG, "No network — skipping exam date sync")
            return Result.success()
        }

        return try {
            val newDates = repository.refreshExamDates()
            val prefs = preferenceManager.preferencesFlow.first()
            
            if (newDates.isNotEmpty() && prefs.notificationsEnabled) {
                val notificationHelper = NotificationHelper(applicationContext)
                notificationHelper.showExamDateNotification(newDates.map { it.courseName })
            }
            rescheduleIfNeeded()
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Sync failed", e)
            if (isServerDown(e)) {
                rescheduleIfNeeded()
                Result.success()
            } else {
                Result.retry()
            }
        }
    }

    private suspend fun rescheduleIfNeeded() {
        val prefs = preferenceManager.preferencesFlow.first()
        if (prefs.notificationsEnabled && prefs.examDateSyncInterval < 15) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            val request = OneTimeWorkRequestBuilder<ExamDateSyncWorker>()
                .setInitialDelay(prefs.examDateSyncInterval.toLong(), TimeUnit.MINUTES)
                .setConstraints(constraints)
                .build()
            WorkManager.getInstance(applicationContext).enqueueUniqueWork(
                WorkManagerHelper.EXAM_DATE_SYNC_WORK_NAME,
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

    companion object {
        private const val TAG = "ExamDateSyncWorker"
    }
}
