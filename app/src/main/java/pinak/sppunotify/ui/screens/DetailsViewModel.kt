package pinak.sppunotify.ui.screens

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import pinak.sppunotify.data.local.ResultEntity
import pinak.sppunotify.data.repository.ResultRepository
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
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

    @kotlin.OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val departmentStats: StateFlow<Map<String, Int>> = result.flatMapLatest { res ->
        if (res == null) flowOf(emptyMap())
        else repository.results.map { all ->
            all.filter { it.department == res.department }
                .groupBy { it.publishedDate.split(" ").lastOrNull() ?: "Unknown" }
                .mapValues { it.value.size }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    fun markAsViewed() {
        viewModelScope.launch {
            repository.markAsViewed(resultId)
        }
    }

    fun toggleBookmark() {
        viewModelScope.launch {
            repository.toggleBookmark(resultId)
        }
    }
}
