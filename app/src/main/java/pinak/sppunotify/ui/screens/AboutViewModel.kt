package pinak.sppunotify.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import pinak.sppunotify.data.repository.RemoteConfigRepository
import pinak.sppunotify.util.NotificationHelper
import pinak.sppunotify.util.UpdateManager
import javax.inject.Inject

@HiltViewModel
class AboutViewModel @Inject constructor(
    private val remoteConfigRepository: RemoteConfigRepository,
    private val updateManager: UpdateManager
) : ViewModel() {

    private val _isChecking = MutableStateFlow(false)
    val isChecking: StateFlow<Boolean> = _isChecking.asStateFlow()

    private val _updateStatus = MutableStateFlow<String?>(null)
    val updateStatus: StateFlow<String?> = _updateStatus.asStateFlow()

    fun checkForUpdates(notificationHelper: NotificationHelper) {
        viewModelScope.launch {
            _isChecking.value = true
            _updateStatus.value = "Checking for updates..."
            try {
                updateManager.checkAndNotifyUpdate(notificationHelper)
                _updateStatus.value = "Check complete. If an update is available, you will receive a notification."
            } catch (e: Exception) {
                _updateStatus.value = "Failed to check for updates: ${e.message}"
            } finally {
                _isChecking.value = false
            }
        }
    }

    fun clearStatus() {
        _updateStatus.value = null
    }
}
