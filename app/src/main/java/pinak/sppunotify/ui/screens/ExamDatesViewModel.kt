package pinak.sppunotify.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import pinak.sppunotify.data.local.ExamDateEntity
import pinak.sppunotify.data.repository.ExamDateRepository
import javax.inject.Inject

@HiltViewModel
class ExamDatesViewModel @Inject constructor(
    private val repository: ExamDateRepository
) : ViewModel() {

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing = _isRefreshing.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _selectedStatus = MutableStateFlow("All")
    val selectedStatus = _selectedStatus.asStateFlow()

    val examDates: StateFlow<List<ExamDateEntity>> = combine(
        repository.examDates,
        _searchQuery,
        _selectedStatus
    ) { dates, query, status ->
        val filtered = if (status == "All") {
            dates
        } else {
            dates.filter { it.status.contains(status, ignoreCase = true) }
        }

        if (query.isBlank()) {
            filtered
        } else {
            filtered.filter { it.courseName.contains(query, ignoreCase = true) }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val serverStatus = repository.serverStatus

    init {
        refresh()
        checkServerStatus()
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun onStatusSelected(status: String) {
        _selectedStatus.value = status
    }

    fun checkServerStatus() {
        viewModelScope.launch {
            try {
                repository.updateServerStatus()
            } catch (_: Exception) {}
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                repository.refreshExamDates()
            } catch (e: Exception) {
                // Handle error
            } finally {
                _isRefreshing.value = false
                checkServerStatus()
            }
        }
    }
}
