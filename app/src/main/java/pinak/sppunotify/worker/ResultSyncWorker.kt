package pinak.sppunotify.worker

import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import pinak.sppunotify.R
import pinak.sppunotify.data.local.PreferenceManager
import pinak.sppunotify.data.repository.ResultRepository
import pinak.sppunotify.util.NotificationHelper
import kotlinx.coroutines.flow.first

@HiltWorker
class ResultSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val repository: ResultRepository,
    private val preferenceManager: PreferenceManager
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        setForeground(createForegroundInfo())
        
        return try {
            val newResults = repository.fetchResults()
            val prefs = preferenceManager.preferencesFlow.first()
            
            if (newResults.isNotEmpty() && prefs.notificationsEnabled) {
                val notificationHelper = NotificationHelper(applicationContext)
                val keywords = prefs.watchlistKeywords
                
                val matched = newResults.filter { result ->
                    keywords.isEmpty() || keywords.any { keyword ->
                        result.title.contains(keyword, ignoreCase = true)
                    }
                }
                
                if (matched.isNotEmpty()) {
                    notificationHelper.showResultNotification(
                        matched.map { "New Result Published!" to it.title }
                    )
                }
            }
            
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "ResultSyncWorker failed", e)
            Result.retry()
        }
    }

    companion object {
        private const val TAG = "ResultSyncWorker"
    }

    private fun createForegroundInfo(): ForegroundInfo {
        val notification = NotificationCompat.Builder(applicationContext, NotificationHelper.CHANNEL_RESULTS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Checking for Results")
            .setContentText("SPPU Result Watch is running in background")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setSilent(true)
            .build()

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(1, notification)
        }
    }
}
