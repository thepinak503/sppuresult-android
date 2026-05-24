package pinak.sppunotify.worker

import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
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
            // Schedule per-screen based on individual toggles
            if (preferences.syncResultsEnabled) {
                scheduleResultSync(preferences.resultSyncInterval)
            } else {
                workManager.cancelUniqueWork(RESULT_SYNC_WORK_NAME)
            }
            
            if (preferences.syncRevalEnabled) {
                scheduleRevalSync(preferences.revalSyncInterval)
            } else {
                workManager.cancelUniqueWork(REVAL_SYNC_WORK_NAME)
            }
            
            if (preferences.syncExamDatesEnabled) {
                scheduleExamDateSync(preferences.examDateSyncInterval)
            } else {
                workManager.cancelUniqueWork(EXAM_DATE_SYNC_WORK_NAME)
            }
        } else {
            cancelAllSync()
        }
    }

    private fun scheduleResultSync(intervalMinutes: Int) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        workManager.cancelUniqueWork(RESULT_SYNC_WORK_NAME)

        if (intervalMinutes < 15) {
            val request = OneTimeWorkRequestBuilder<ResultSyncWorker>()
                .setInitialDelay(intervalMinutes.toLong(), TimeUnit.MINUTES)
                .setConstraints(constraints)
                .build()
            workManager.enqueueUniqueWork(
                RESULT_SYNC_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                request
            )
        } else {
            val request = PeriodicWorkRequestBuilder<ResultSyncWorker>(
                intervalMinutes.toLong(), TimeUnit.MINUTES
            ).setConstraints(constraints).build()
            workManager.enqueueUniquePeriodicWork(
                RESULT_SYNC_WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
        }
    }

    private fun scheduleRevalSync(intervalMinutes: Int) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        workManager.cancelUniqueWork(REVAL_SYNC_WORK_NAME)

        if (intervalMinutes < 15) {
            val request = OneTimeWorkRequestBuilder<RevalSyncWorker>()
                .setInitialDelay(intervalMinutes.toLong(), TimeUnit.MINUTES)
                .setConstraints(constraints)
                .build()
            workManager.enqueueUniqueWork(
                REVAL_SYNC_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                request
            )
        } else {
            val request = PeriodicWorkRequestBuilder<RevalSyncWorker>(
                intervalMinutes.toLong(), TimeUnit.MINUTES
            ).setConstraints(constraints).build()
            workManager.enqueueUniquePeriodicWork(
                REVAL_SYNC_WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
        }
    }

    private fun scheduleExamDateSync(intervalMinutes: Int) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        workManager.cancelUniqueWork(EXAM_DATE_SYNC_WORK_NAME)

        if (intervalMinutes < 15) {
            val request = OneTimeWorkRequestBuilder<ExamDateSyncWorker>()
                .setInitialDelay(intervalMinutes.toLong(), TimeUnit.MINUTES)
                .setConstraints(constraints)
                .build()
            workManager.enqueueUniqueWork(
                EXAM_DATE_SYNC_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                request
            )
        } else {
            val request = PeriodicWorkRequestBuilder<ExamDateSyncWorker>(
                intervalMinutes.toLong(), TimeUnit.MINUTES
            ).setConstraints(constraints).build()
            workManager.enqueueUniquePeriodicWork(
                EXAM_DATE_SYNC_WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
        }
    }

    fun cancelAllSync() {
        workManager.cancelUniqueWork(RESULT_SYNC_WORK_NAME)
        workManager.cancelUniqueWork(REVAL_SYNC_WORK_NAME)
        workManager.cancelUniqueWork(EXAM_DATE_SYNC_WORK_NAME)
    }

    /** Get sync status info for dashboard */
    fun getSyncStatus(): SyncStatus {
        return SyncStatus(
            isResultSyncScheduled = workManager.getWorkInfosForUniqueWork(RESULT_SYNC_WORK_NAME).get().isNotEmpty(),
            isRevalSyncScheduled = workManager.getWorkInfosForUniqueWork(REVAL_SYNC_WORK_NAME).get().isNotEmpty(),
            isExamDateSyncScheduled = workManager.getWorkInfosForUniqueWork(EXAM_DATE_SYNC_WORK_NAME).get().isNotEmpty()
        )
    }

    data class SyncStatus(
        val isResultSyncScheduled: Boolean,
        val isRevalSyncScheduled: Boolean,
        val isExamDateSyncScheduled: Boolean
    )
}
