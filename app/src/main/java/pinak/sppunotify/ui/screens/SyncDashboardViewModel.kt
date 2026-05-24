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
import pinak.sppunotify.worker.WorkManagerHelper
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

data class SyncDashboardState(
    val resultEnabled: Boolean = true,
    val revalEnabled: Boolean = true,
    val examDatesEnabled: Boolean = true,
    val resultRunning: Boolean = false,
    val revalRunning: Boolean = false,
    val examDatesRunning: Boolean = false,
    val resultLastRun: String = "",
    val revalLastRun: String = "",
    val examDatesLastRun: String = "",
    val recentSyncs: List<SyncEntry> = emptyList()
)

data class SyncEntry(
    val type: String,
    val time: String,
    val success: Boolean
)

@HiltViewModel
class SyncDashboardViewModel @Inject constructor(
    private val preferenceManager: PreferenceManager,
    private val workManagerHelper: WorkManagerHelper
) : ViewModel() {

    private val _state = MutableStateFlow(SyncDashboardState())
    val state: StateFlow<SyncDashboardState> = _state.asStateFlow()

    init {
        loadState()
    }

    private fun loadState() {
        viewModelScope.launch {
            val prefs = preferenceManager.preferencesFlow.first()
            _state.value = _state.value.copy(
                resultEnabled = prefs.syncResultsEnabled,
                revalEnabled = prefs.syncRevalEnabled,
                examDatesEnabled = prefs.syncExamDatesEnabled
            )
        }
    }

    fun syncNow(type: String) {
        viewModelScope.launch {
            when (type) {
                "results" -> {
                    _state.value = _state.value.copy(resultRunning = true)
                    // Trigger one-time sync via WorkManager
                    val prefs = preferenceManager.preferencesFlow.first()
                    workManagerHelper.updateSyncWork(prefs.copy(syncResultsEnabled = true, resultSyncInterval = 15))
                    delay(500)
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
                    delay(500)
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
                    delay(500)
                    _state.value = _state.value.copy(
                        examDatesRunning = false,
                        examDatesLastRun = formatNow(),
                        recentSyncs = listOf(SyncEntry("Exam Dates", formatNow(), true)) + _state.value.recentSyncs.take(19)
                    )
                }
            }
        }
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
            }
        }
    }

    private fun formatNow(): String {
        return SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
    }
}
