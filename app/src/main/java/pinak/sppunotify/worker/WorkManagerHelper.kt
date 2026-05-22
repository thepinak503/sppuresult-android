package pinak.sppunotify.worker

import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import pinak.sppunotify.data.local.UserPreferences
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkManagerHelper @Inject constructor(
    private val workManager: WorkManager
) {
    companion object {
        const val RESULT_SYNC_WORK_NAME = "ResultSyncWork"
        const val REVAL_SYNC_WORK_NAME = "RevalSyncWork"
        const val EXAM_DATE_SYNC_WORK_NAME = "ExamDateSyncWork"
    }

    fun updateSyncWork(preferences: UserPreferences) {
        if (preferences.notificationsEnabled) {
            scheduleResultSync(preferences.resultSyncInterval)
            scheduleRevalSync(preferences.revalSyncInterval)
            scheduleExamDateSync(preferences.resultSyncInterval) // Using resultSyncInterval for now
        } else {
            cancelAllSync()
        }
    }

    private fun scheduleResultSync(intervalMinutes: Int) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = PeriodicWorkRequestBuilder<ResultSyncWorker>(
            intervalMinutes.toLong(), TimeUnit.MINUTES
        ).setConstraints(constraints).build()

        workManager.enqueueUniquePeriodicWork(
            RESULT_SYNC_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    private fun scheduleRevalSync(intervalMinutes: Int) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = PeriodicWorkRequestBuilder<RevalSyncWorker>(
            intervalMinutes.toLong(), TimeUnit.MINUTES
        ).setConstraints(constraints).build()

        workManager.enqueueUniquePeriodicWork(
            REVAL_SYNC_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    private fun scheduleExamDateSync(intervalMinutes: Int) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = PeriodicWorkRequestBuilder<ExamDateSyncWorker>(
            intervalMinutes.toLong(), TimeUnit.MINUTES
        ).setConstraints(constraints).build()

        workManager.enqueueUniquePeriodicWork(
            EXAM_DATE_SYNC_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    fun cancelAllSync() {
        workManager.cancelUniqueWork(RESULT_SYNC_WORK_NAME)
        workManager.cancelUniqueWork(REVAL_SYNC_WORK_NAME)
        workManager.cancelUniqueWork(EXAM_DATE_SYNC_WORK_NAME)
    }
}
