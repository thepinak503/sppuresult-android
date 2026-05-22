package pinak.sppunotify.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
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
        return try {
            val newDates = repository.refreshExamDates()
            val prefs = preferenceManager.preferencesFlow.first()
            
            if (newDates.isNotEmpty() && prefs.notificationsEnabled) {
                val notificationHelper = NotificationHelper(applicationContext)
                notificationHelper.showExamDateNotification(newDates.map { it.courseName })
            }
            
            Result.success()
        } catch (e: Exception) {
            Log.e("ExamDateSyncWorker", "Sync failed", e)
            Result.retry()
        }
    }
}
