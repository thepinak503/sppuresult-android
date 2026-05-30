package pinak.sppunotify.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import pinak.sppunotify.data.local.NotificationHistoryDao
import pinak.sppunotify.data.local.PreferenceManager
import pinak.sppunotify.data.repository.CircularRepository
import pinak.sppunotify.util.NotificationHelper

@HiltWorker
class CircularSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val repository: CircularRepository,
    private val preferenceManager: PreferenceManager,
    private val syncLogDao: pinak.sppunotify.data.local.SyncLogDao,
    private val notificationHistoryDao: NotificationHistoryDao
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val oldCirculars = repository.getCachedCirculars().first()
            val newCirculars = repository.fetchAllCirculars()
            
            val freshCount = newCirculars.count { new -> 
                oldCirculars.none { old -> old.link == new.link }
            }

            if (freshCount > 0) {
                val prefs = preferenceManager.preferencesFlow.first()
                if (prefs.notificationsEnabled && prefs.syncCircularsEnabled) {
                    val notificationHelper = NotificationHelper(applicationContext, notificationHistoryDao)
                    notificationHelper.showCircularNotification(freshCount)
                }
            }
            syncLogDao.insertLog(pinak.sppunotify.data.local.SyncLogEntity(type = "CIRCULAR", status = "SUCCESS", message = "$freshCount new circulars"))
            Result.success()
        } catch (e: Exception) {
            Log.e("CircularSyncWorker", "Circular sync failed", e)
            syncLogDao.insertLog(pinak.sppunotify.data.local.SyncLogEntity(type = "CIRCULAR", status = "FAILED", message = e.message ?: "Unknown error"))
            Result.retry()
        }
    }
}
