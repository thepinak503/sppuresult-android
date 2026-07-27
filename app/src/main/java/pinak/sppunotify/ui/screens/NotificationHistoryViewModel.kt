package pinak.sppunotify.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import pinak.sppunotify.data.local.NotificationHistoryDao
import pinak.sppunotify.data.remote.NotificationHistoryEntry
import javax.inject.Inject

@HiltViewModel
class NotificationHistoryViewModel @Inject constructor(
    private val dao: NotificationHistoryDao
) : ViewModel() {

    val entries: StateFlow<List<NotificationHistoryEntry>> = dao.getAll().map { entities ->
        entities.map { entity ->
            NotificationHistoryEntry(
                id = entity.id,
                title = entity.title,
                message = entity.message,
                type = entity.type,
                targetUri = entity.targetUri,
                timestamp = entity.timestamp
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun clearAll() {
        viewModelScope.launch {
            dao.clearAll()
        }
    }
}
