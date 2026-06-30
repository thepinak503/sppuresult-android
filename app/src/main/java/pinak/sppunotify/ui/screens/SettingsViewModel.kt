package pinak.sppunotify.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.channels.Channel
import pinak.sppunotify.data.local.PreferenceManager
import pinak.sppunotify.data.local.UserPreferences
import pinak.sppunotify.worker.WorkManagerHelper
import pinak.sppunotify.util.BackupManager
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferenceManager: PreferenceManager,
    private val workManagerHelper: WorkManagerHelper,
    private val backupManager: BackupManager
) : ViewModel() {

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _uiEvent = Channel<SettingsUiEvent>()
    val uiEvent = _uiEvent.receiveAsFlow()

    val userPreferences: StateFlow<UserPreferences> = preferenceManager.preferencesFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = UserPreferences(true, 30, emptySet(), emptySet(), emptyList(), null)
    )

    fun syncAll() {
        viewModelScope.launch {
            _isSyncing.value = true
            val currentPrefs = preferenceManager.preferencesFlow.first()
            workManagerHelper.updateSyncWork(currentPrefs)
            delay(1500) // Simulation for visual feedback
            _isSyncing.value = false
        }
    }

    fun updateNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferenceManager.updateNotificationsEnabled(enabled)
        }
    }

    fun updateSyncInterval(minutes: Int) {
        viewModelScope.launch {
            preferenceManager.updateSyncInterval(minutes)
        }
    }

    fun addKeyword(keyword: String) {
        if (keyword.isBlank()) return
        viewModelScope.launch {
            preferenceManager.addKeyword(keyword)
        }
    }

    fun removeKeyword(keyword: String) {
        viewModelScope.launch {
            preferenceManager.removeKeyword(keyword)
        }
    }

    fun toggleDepartmentSubscription(department: String) {
        viewModelScope.launch {
            preferenceManager.toggleSubscribedDepartment(department)
        }
    }

    fun addPriorityKeyword(keyword: String) {
        if (keyword.isBlank()) return
        viewModelScope.launch {
            preferenceManager.addPriorityKeyword(keyword)
        }
    }

    fun removePriorityKeyword(keyword: String) {
        viewModelScope.launch {
            preferenceManager.removePriorityKeyword(keyword)
        }
    }

    fun addProfile(name: String, seatNo: String, motherName: String) {
        viewModelScope.launch {
            preferenceManager.saveProfile(name, seatNo, motherName)
        }
    }

    fun deleteProfile(id: String) {
        viewModelScope.launch {
            preferenceManager.deleteProfile(id)
        }
    }

    fun setActiveProfile(id: String) {
        viewModelScope.launch {
            preferenceManager.setActiveProfile(id)
        }
    }

    fun updateThemeMode(themeMode: String) {
        viewModelScope.launch {
            preferenceManager.updateThemeMode(themeMode)
        }
    }

    fun updateAppLanguage(languageCode: String) {
        viewModelScope.launch {
            preferenceManager.updateAppLanguage(languageCode)
        }
    }

    fun updateAutoUpdateEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferenceManager.updateAutoUpdateEnabled(enabled)
        }
    }

    fun exportBackup() {
        viewModelScope.launch {
            try {
                val json = backupManager.createBackup()
                _uiEvent.send(SettingsUiEvent.SaveBackup(json))
            } catch (e: Exception) {
                _uiEvent.send(SettingsUiEvent.ShowError("Failed to create backup: ${e.message}"))
            }
        }
    }

    fun importBackup(jsonContent: String) {
        viewModelScope.launch {
            val success = backupManager.restoreBackup(jsonContent)
            if (success) {
                _uiEvent.send(SettingsUiEvent.ShowMessage("Backup restored successfully!"))
            } else {
                _uiEvent.send(SettingsUiEvent.ShowError("Invalid backup file or restore failed."))
            }
        }
    }
}

sealed class SettingsUiEvent {
    data class SaveBackup(val json: String) : SettingsUiEvent()
    data class ShowMessage(val message: String) : SettingsUiEvent()
    data class ShowError(val message: String) : SettingsUiEvent()
}
