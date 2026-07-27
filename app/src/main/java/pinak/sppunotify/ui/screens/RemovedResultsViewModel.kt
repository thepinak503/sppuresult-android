package pinak.sppunotify.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import pinak.sppunotify.data.local.RemovedResultDao
import pinak.sppunotify.data.local.RemovedResultEntity
import javax.inject.Inject

@HiltViewModel
class RemovedResultsViewModel @Inject constructor(
    private val dao: RemovedResultDao
) : ViewModel() {

    val removedResults: StateFlow<List<RemovedResultEntity>> = dao.getAllRemovedResults()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun clearAll() {
        viewModelScope.launch {
            dao.clearAll()
        }
    }
}
