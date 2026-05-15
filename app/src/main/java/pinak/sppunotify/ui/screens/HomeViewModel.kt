package pinak.sppunotify.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import pinak.sppunotify.data.local.ResultEntity
import pinak.sppunotify.data.repository.ResultRepository
import pinak.sppunotify.util.DepartmentClassifier
import javax.inject.Inject

enum class SortOrder(val label: String) {
    NEWEST_FIRST("Newest First"),
    OLDEST_FIRST("Oldest First"),
    NAME_A_Z("Name A-Z"),
    NAME_Z_A("Name Z-A"),
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: ResultRepository,
) : ViewModel() {

    private val _searchQuery = MutableStateFlow(value = "")
    val searchQuery = _searchQuery.asStateFlow()

    private val _selectedDepartment = MutableStateFlow(value = "All")
    val selectedDepartment = _selectedDepartment.asStateFlow()

    private val _isRefreshing = MutableStateFlow(value = false)
    val isRefreshing = _isRefreshing.asStateFlow()

    private val _sortOrder = MutableStateFlow(value = SortOrder.NEWEST_FIRST)
    val sortOrder = _sortOrder.asStateFlow()

    private val _totalCount = MutableStateFlow(value = 0)
    val totalCount = _totalCount.asStateFlow()

    private val _uiEvent = Channel<UiEvent>()
    val uiEvent = _uiEvent.receiveAsFlow()

    val departments = DepartmentClassifier.departments

    val results: StateFlow<List<ResultEntity>> = combine(
        repository.results,
        _searchQuery,
        _selectedDepartment,
        _sortOrder,
    ) { results, query, dept, sortOrder ->
        _totalCount.value = results.size

        val trimmedQuery = query.trim().lowercase()
        val tokens = if (trimmedQuery.isEmpty()) emptyList() else trimmedQuery.split(WHITESPACE_REGEX)

        val filtered = results.filter { result ->
            val matchesDept = dept == "All" || result.department == dept
            if (!matchesDept) return@filter false

            if (tokens.isEmpty()) return@filter true

            val targetLower = result.title.lowercase() + " " +
                              result.patternName.lowercase() + " " +
                              result.publishedDate.lowercase()

            tokens.all { token -> tokenFuzzyMatch(targetLower, token) }
        }

        if (tokens.isEmpty()) {
            when (sortOrder) {
                SortOrder.NEWEST_FIRST -> filtered
                SortOrder.OLDEST_FIRST -> filtered.reversed()
                SortOrder.NAME_A_Z -> filtered.sortedBy { it.title.lowercase() }
                SortOrder.NAME_Z_A -> filtered.sortedByDescending { it.title.lowercase() }
            }
        } else {
            filtered.sortedByDescending { rankResult(it, tokens) }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        refresh()
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun onDepartmentSelected(dept: String) {
        _selectedDepartment.value = dept
    }

    fun setSortOrder(order: SortOrder) {
        _sortOrder.value = order
    }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                val newResults = repository.fetchResults()
                if (newResults.isEmpty()) {
                    if (repository.getCachedCount() == 0) {
                        _uiEvent.send(UiEvent.ShowSnackbar("No results loaded. Pull down to retry."))
                    }
                } else {
                    _uiEvent.send(UiEvent.ShowSnackbar("${newResults.size} new result(s) found"))
                }
            } catch (e: Exception) {
                val msg = when {
                    e.message?.contains("502") == true -> "SPPU server is down (502 Bad Gateway)."
                    e.message?.contains("503") == true -> "SPPU server is busy (503 Service Unavailable)."
                    e.message?.contains("504") == true -> "SPPU server timed out (504 Gateway Time-out)."
                    e.message?.contains("timeout", ignoreCase = true) == true -> "Connection timed out."
                    else -> "Network error: ${e.message ?: "Unknown"}"
                }
                _uiEvent.send(UiEvent.ShowErrorDialog("Warning", msg))
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    private fun tokenFuzzyMatch(targetLower: String, token: String): Boolean {
        if (token.isEmpty()) return true
        if (targetLower.contains(token)) return true

        var ti = 0
        var qi = 0
        while (ti < targetLower.length && qi < token.length) {
            if (targetLower[ti] == token[qi]) qi++
            ti++
        }
        return qi == token.length
    }

    private fun rankResult(result: ResultEntity, tokens: List<String>): Int {
        return scoreField(result.title.lowercase(), tokens, 100) +
               scoreField(result.patternName.lowercase(), tokens, 50) +
               scoreField(result.publishedDate.lowercase(), tokens, 25)
    }

    private fun scoreField(field: String, tokens: List<String>, mul: Int): Int {
        var score = 0
        for (token in tokens) {
            val idx = field.indexOf(token)
            if (idx >= 0) {
                score += mul * (10 + (10 - idx / 3).coerceAtLeast(0))
            } else if (tokenFuzzyMatch(field, token)) {
                score += mul * 3
            }
        }
        return score
    }

    companion object {
        private val WHITESPACE_REGEX = Regex("\\s+")
    }

    sealed class UiEvent {
        data class ShowSnackbar(val message: String) : UiEvent()
        data class ShowErrorDialog(val title: String, val message: String) : UiEvent()
    }
}
