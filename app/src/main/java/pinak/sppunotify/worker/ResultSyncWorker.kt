package pinak.sppunotify.worker

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
import pinak.sppunotify.data.local.PreferenceManager
import pinak.sppunotify.data.repository.ResultRepository
import pinak.sppunotify.data.repository.RemoteConfigRepository
import pinak.sppunotify.util.NotificationHelper
import kotlinx.coroutines.flow.first
import androidx.glance.appwidget.updateAll

@HiltWorker
class ResultSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val repository: ResultRepository,
    private val remoteConfigRepository: RemoteConfigRepository,
    private val preferenceManager: PreferenceManager,
    private val updateManager: pinak.sppunotify.util.UpdateManager,
    private val syncLogDao: pinak.sppunotify.data.local.SyncLogDao,
    private val notificationHistoryDao: NotificationHistoryDao
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        setForeground(createForegroundInfo())
        val prefs = preferenceManager.preferencesFlow.first()
        val notificationHelper = NotificationHelper(applicationContext, notificationHistoryDao)
        
        if (!isNetworkAvailable()) {
            Log.w(TAG, "No network — skipping sync")
            if (prefs.developerMode) {
                notificationHelper.showNewsNotification("Sync Skipped", "No internet connection detected.", "ANNOUNCEMENT")
            }
            return Result.success()
        }
        
        return try {
            // 1. Sync Results & Auto-Cleanup
            val newResults = repository.fetchResults()
            if (newResults.isNotEmpty() && prefs.notificationsEnabled) {
                val subscribedDepartments = prefs.subscribedDepartments
                val keywords = prefs.watchlistKeywords
                val priorityKeywords = prefs.priorityKeywords
                
                val priorityMatched = newResults.filter { result ->
                    priorityKeywords.isNotEmpty() && priorityKeywords.any { keyword ->
                        result.title.contains(keyword, ignoreCase = true)
                    }
                }
                
                // A result qualifies for a normal notification if:
                // 1. It belongs to a subscribed department, OR
                // 2. It matches a watchlist keyword
                // If nothing is subscribed (no depts, no keywords), NO notifications are sent.
                val normalMatched = newResults.filter { result ->
                    val matchesDepartment = subscribedDepartments.isNotEmpty() &&
                        result.department in subscribedDepartments
                    val matchesKeyword = keywords.isNotEmpty() && keywords.any { keyword ->
                        result.title.contains(keyword, ignoreCase = true)
                    }
                    (matchesDepartment || matchesKeyword) && priorityMatched.none { it.id == result.id }
                }
                
                if (priorityMatched.isNotEmpty()) {
                    notificationHelper.showPriorityResultNotification(
                        priorityMatched.map { pinak.sppunotify.util.NotificationResult("⭐ PRIORITY RESULT ⭐", it.title, it.id) }
                    )
                }
                
                if (normalMatched.isNotEmpty()) {
                    notificationHelper.showResultNotification(
                        normalMatched.map { pinak.sppunotify.util.NotificationResult("New Result Published!", it.title, it.id) }
                    )
                }
            }

            // 2. Remote Config & App Updates
            try {
                updateManager.checkAndNotifyUpdate(notificationHelper)
                
                val config = remoteConfigRepository.fetchAppConfig()
                if (config != null) {
                    // Check for Announcements
                    config.announcement?.let { announcement ->
                        if (announcement.id != prefs.lastAnnouncementId) {
                            notificationHelper.showNewsNotification(
                                title = announcement.title,
                                message = announcement.message,
                                type = announcement.type
                            )
                            preferenceManager.updateLastAnnouncementId(announcement.id)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Config sync failed", e)
            }

            // Update Glance Widget State with result list
            try {
                val results = repository.results.first()
                val status = repository.serverStatus.value
                val now = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date())
                
                val items = results.take(12).map { result ->
                    pinak.sppunotify.widget.WidgetResultItem(
                        id = result.id,
                        title = result.title,
                        date = result.publishedDate,
                        department = result.department,
                    )
                }
                val encoded = items.map { it.encode() }.toSet()

                val manager = androidx.glance.appwidget.GlanceAppWidgetManager(applicationContext)
                val glanceIds = manager.getGlanceIds(pinak.sppunotify.widget.GlanceServerStatusWidget::class.java)
                
                glanceIds.forEach { glanceId ->
                    androidx.glance.appwidget.state.updateAppWidgetState(applicationContext, glanceId) { prefs ->
                        prefs.toMutablePreferences().apply {
                            this[pinak.sppunotify.widget.GlanceWidgetKeys.statusLevel] = status?.statusLevel?.name ?: "HEALTHY"
                            this[pinak.sppunotify.widget.GlanceWidgetKeys.responseTime] = status?.responseTimeMs ?: 0L
                            this[pinak.sppunotify.widget.GlanceWidgetKeys.lastUpdated] = now
                            this[pinak.sppunotify.widget.GlanceWidgetKeys.totalResults] = results.size.toLong()
                            this[pinak.sppunotify.widget.GlanceWidgetKeys.resultItems] = encoded
                        }
                    }
                }
                pinak.sppunotify.widget.GlanceServerStatusWidget().updateAll(applicationContext)
            } catch (e: Exception) {
                Log.e(TAG, "Glance update failed", e)
            }

            // 3. Server Recovery Check
            try {
                repository.updateServerStatus()
                val currentStatus = repository.serverStatus.value
                if (currentStatus?.isOnline == true && prefs.wasServerDown) {
                    notificationHelper.showNewsNotification(
                        title = "Server Recovered!",
                        message = "SPPU Result portal is back online.",
                        type = "ANNOUNCEMENT"
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Health check failed", e)
            }

            if (prefs.developerMode) {
                notificationHelper.showNewsNotification("Sync Success", "Results checked at ${java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date())}", "ANNOUNCEMENT")
            }
            
            syncLogDao.insertLog(pinak.sppunotify.data.local.SyncLogEntity(type = "RESULTS", status = "SUCCESS", message = "${newResults.size} results found"))
            rescheduleIfNeeded()
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "ResultSyncWorker failed", e)
            syncLogDao.insertLog(pinak.sppunotify.data.local.SyncLogEntity(type = "RESULTS", status = "FAILED", message = e.message ?: "Unknown error"))
            repository.updateServerStatus() // Mark as down if failed
            if (prefs.developerMode) {
                notificationHelper.showNewsNotification("Sync Failed", e.message ?: "Unknown error", "ALERT")
            }
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
        if (prefs.notificationsEnabled && prefs.syncInterval < 15) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            val request = OneTimeWorkRequestBuilder<ResultSyncWorker>()
                .setInitialDelay(prefs.syncInterval.toLong(), TimeUnit.MINUTES)
                .setConstraints(constraints)
                .build()
            WorkManager.getInstance(applicationContext).enqueueUniqueWork(
                WorkManagerHelper.RESULT_SYNC_WORK_NAME,
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
