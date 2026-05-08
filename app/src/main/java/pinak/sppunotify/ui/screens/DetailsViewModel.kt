package pinak.sppunotify.ui.screens

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import pinak.sppunotify.data.local.ResultEntity
import pinak.sppunotify.data.repository.ResultRepository
import javax.inject.Inject

@HiltViewModel
class DetailsViewModel @Inject constructor(
    private val repository: ResultRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val resultId: String = checkNotNull(savedStateHandle["resultId"])

    val result: StateFlow<ResultEntity?> = repository.results
        .map { results -> results.find { it.id == resultId } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
}
