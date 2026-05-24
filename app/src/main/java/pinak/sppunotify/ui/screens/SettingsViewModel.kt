package pinak.sppunotify.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import pinak.sppunotify.data.local.PreferenceManager
import pinak.sppunotify.data.local.UserPreferences
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferenceManager: PreferenceManager
) : ViewModel() {

    val userPreferences: StateFlow<UserPreferences> = preferenceManager.preferencesFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = UserPreferences(true, 15, 60, 180, emptySet(), emptyList(), null)
    )

    fun updateNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferenceManager.updateNotificationsEnabled(enabled)
        }
    }

    fun updateResultSyncInterval(minutes: Int) {
        viewModelScope.launch {
            preferenceManager.updateResultSyncInterval(minutes)
        }
    }

    fun updateRevalSyncInterval(minutes: Int) {
        viewModelScope.launch {
            preferenceManager.updateRevalSyncInterval(minutes)
        }
    }

    fun updateExamDateSyncInterval(minutes: Int) {
        viewModelScope.launch {
            preferenceManager.updateExamDateSyncInterval(minutes)
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
}
