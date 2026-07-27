package pinak.sppunotify.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import pinak.sppunotify.data.local.PreferenceManager
import pinak.sppunotify.data.repository.ResultRepository
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: ResultRepository,
    private val preferenceManager: PreferenceManager
) : ViewModel() {

    val isSyncing: StateFlow<Boolean> = repository.isSyncing

    init {
        startForegroundSync()
    }

    private fun startForegroundSync() {
        viewModelScope.launch {
            // Initial sync on app open if needed
            repository.fetchResults()
            
            preferenceManager.preferencesFlow.collectLatest { prefs ->
                if (prefs.notificationsEnabled) {
                    val intervalMillis = prefs.syncInterval.toLong() * 60 * 1000
                    while (true) {
                        delay(intervalMillis)
                        repository.fetchResults()
                    }
                }
            }
        }
    }
    
    fun manualSync() {
        viewModelScope.launch {
            repository.fetchResults()
        }
    }
}
