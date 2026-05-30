package pinak.sppunotify.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import pinak.sppunotify.data.local.PreferenceManager
import pinak.sppunotify.data.local.SyncLogDao
import pinak.sppunotify.worker.WorkManagerHelper
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

data class SyncDashboardState(
    val resultEnabled: Boolean = true,
    val revalEnabled: Boolean = true,
    val examDatesEnabled: Boolean = true,
    val circularsEnabled: Boolean = true,
    val resultRunning: Boolean = false,
    val revalRunning: Boolean = false,
    val examDatesRunning: Boolean = false,
    val circularsRunning: Boolean = false,
    val resultLastRun: String = "",
    val revalLastRun: String = "",
    val examDatesLastRun: String = "",
    val circularsLastRun: String = "",
    val recentSyncs: List<SyncEntry> = emptyList(),
    val successCount: Int = 0,
    val failedCount: Int = 0,
    val uptimePercent: Int = 100
)

data class SyncEntry(
    val type: String,
    val time: String,
    val success: Boolean,
    val message: String = ""
)

@HiltViewModel
class SyncDashboardViewModel @Inject constructor(
    private val preferenceManager: PreferenceManager,
    private val workManagerHelper: WorkManagerHelper,
    private val syncLogDao: SyncLogDao
) : ViewModel() {

    private val _state = MutableStateFlow(SyncDashboardState())
    val state: StateFlow<SyncDashboardState> = _state.asStateFlow()

    init {
        loadState()
        observeLogs()
    }

    private fun observeLogs() {
        viewModelScope.launch {
            syncLogDao.getRecentLogs().collect { logs ->
                val entries = logs.map { log ->
                    SyncEntry(
                        type = log.type,
                        time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(log.timestamp)),
                        success = log.status == "SUCCESS",
                        message = log.message
                    )
                }
                
                val last24h = System.currentTimeMillis() - (24 * 60 * 60 * 1000)
                val success = syncLogDao.getSuccessCount(last24h)
                val failed = syncLogDao.getFailedCount(last24h)
                val total = success + failed
                val uptime = if (total > 0) (success * 100) / total else 100

                _state.value = _state.value.copy(
                    recentSyncs = entries,
                    successCount = success,
                    failedCount = failed,
                    uptimePercent = uptime
                )
            }
        }
    }

    private fun loadState() {
        viewModelScope.launch {
            val prefs = preferenceManager.preferencesFlow.first()
            _state.value = _state.value.copy(
                resultEnabled = prefs.syncResultsEnabled,
                revalEnabled = prefs.syncRevalEnabled,
                examDatesEnabled = prefs.syncExamDatesEnabled,
                circularsEnabled = prefs.syncCircularsEnabled
            )
        }
    }

    fun syncNow(type: String) {
        viewModelScope.launch {
            when (type) {
                "results" -> {
                    _state.value = _state.value.copy(resultRunning = true)
                    val prefs = preferenceManager.preferencesFlow.first()
                    workManagerHelper.updateSyncWork(prefs.copy(syncResultsEnabled = true, resultSyncInterval = 15))
                    delay(1200)
                    _state.value = _state.value.copy(
                        resultRunning = false,
                        resultLastRun = formatNow(),
                        recentSyncs = listOf(SyncEntry("Results", formatNow(), true)) + _state.value.recentSyncs.take(19)
                    )
                }
                "reval" -> {
                    _state.value = _state.value.copy(revalRunning = true)
                    val prefs = preferenceManager.preferencesFlow.first()
                    workManagerHelper.updateSyncWork(prefs.copy(syncRevalEnabled = true, revalSyncInterval = 15))
                    delay(1200)
                    _state.value = _state.value.copy(
                        revalRunning = false,
                        revalLastRun = formatNow(),
                        recentSyncs = listOf(SyncEntry("Revaluation", formatNow(), true)) + _state.value.recentSyncs.take(19)
                    )
                }
                "examDates" -> {
                    _state.value = _state.value.copy(examDatesRunning = true)
                    val prefs = preferenceManager.preferencesFlow.first()
                    workManagerHelper.updateSyncWork(prefs.copy(syncExamDatesEnabled = true, examDateSyncInterval = 15))
                    delay(1200)
                    _state.value = _state.value.copy(
                        examDatesRunning = false,
                        examDatesLastRun = formatNow(),
                        recentSyncs = listOf(SyncEntry("Exam Dates", formatNow(), true)) + _state.value.recentSyncs.take(19)
                    )
                }
                "circulars" -> {
                    _state.value = _state.value.copy(circularsRunning = true)
                    val prefs = preferenceManager.preferencesFlow.first()
                    workManagerHelper.updateSyncWork(prefs.copy(syncCircularsEnabled = true, circularSyncInterval = 15))
                    delay(1200)
                    _state.value = _state.value.copy(
                        circularsRunning = false,
                        circularsLastRun = formatNow(),
                        recentSyncs = listOf(SyncEntry("Circulars", formatNow(), true)) + _state.value.recentSyncs.take(19)
                    )
                }
            }
        }
    }

    fun syncAll() {
        syncNow("results")
        syncNow("reval")
        syncNow("examDates")
        syncNow("circulars")
    }

    fun toggleSync(type: String, enabled: Boolean) {
        viewModelScope.launch {
            when (type) {
                "results" -> {
                    preferenceManager.updateSyncResultsEnabled(enabled)
                    _state.value = _state.value.copy(resultEnabled = enabled)
                }
                "reval" -> {
                    preferenceManager.updateSyncRevalEnabled(enabled)
                    _state.value = _state.value.copy(revalEnabled = enabled)
                }
                "examDates" -> {
                    preferenceManager.updateSyncExamDatesEnabled(enabled)
                    _state.value = _state.value.copy(examDatesEnabled = enabled)
                }
                "circulars" -> {
                    preferenceManager.updateSyncCircularsEnabled(enabled)
                    _state.value = _state.value.copy(circularsEnabled = enabled)
                }
            }
        }
    }

    private fun formatNow(): String {
        return SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
    }
}
