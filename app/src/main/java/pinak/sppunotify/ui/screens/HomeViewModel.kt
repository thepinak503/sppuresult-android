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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import pinak.sppunotify.data.local.ResultEntity
import pinak.sppunotify.data.remote.ServerStatus
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

    private val _lastUpdated = MutableStateFlow("")
    val lastUpdated = _lastUpdated.asStateFlow()
    val serverStatus = repository.serverStatus

    private val _uiEvent = Channel<UiEvent>()
    val uiEvent = _uiEvent.receiveAsFlow()

    private val _showBookmarks = MutableStateFlow(false)
    val showBookmarks = _showBookmarks.asStateFlow()

    val bookmarkedResults: StateFlow<List<ResultEntity>> = repository.bookmarkedResults
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val bookmarkCount: StateFlow<Int> = repository.bookmarkedResults
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val departments = DepartmentClassifier.departments

    val results: StateFlow<List<ResultEntity>> = combine(
        repository.results,
        _searchQuery,
        _selectedDepartment,
        _sortOrder,
    ) { results, query, dept, sortOrder ->
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

        _totalCount.value = filtered.size

        if (tokens.isEmpty()) {
            when (sortOrder) {
                SortOrder.NEWEST_FIRST -> filtered.sortedByDescending { it.publishedTimestamp }
                SortOrder.OLDEST_FIRST -> filtered.sortedBy { it.publishedTimestamp }
                SortOrder.NAME_A_Z -> filtered.sortedBy { it.title.lowercase() }
                SortOrder.NAME_Z_A -> filtered.sortedByDescending { it.title.lowercase() }
            }
        } else {
            filtered.sortedByDescending { rankResult(it, tokens) }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        refresh()
        checkServerStatus()
    }

    fun checkServerStatus() {
        viewModelScope.launch {
            repository.updateServerStatus()
        }
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
                val syncResult = repository.fetchResults()
                _lastUpdated.value = formatTimestamp(System.currentTimeMillis())
                val newCount = syncResult.newResults.size
                val removedCount = syncResult.removedResults.size
                if (newCount == 0 && removedCount == 0) {
                    if (repository.getCachedCount() == 0) {
                        _uiEvent.send(UiEvent.ShowSnackbar("No results loaded. Pull down to retry."))
                    }
                } else {
                    val parts = mutableListOf<String>()
                    if (newCount > 0) parts.add("$newCount new")
                    if (removedCount > 0) parts.add("$removedCount removed")
                    _uiEvent.send(UiEvent.ShowSnackbar("Sync complete: ${parts.joinToString(", ")}"))
                }
            } catch (e: Exception) {
                checkServerStatus()
                val status = serverStatus.value
                val msg = if (status != null && !status.isOnline) {
                    "SPPU server is not responding (last checked: ${status.responseTimeMs}ms)"
                } else {
                    deriveErrorMessage(e)
                }
                _uiEvent.send(UiEvent.ShowErrorDialog("Warning", msg))
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    fun hardRefresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                repository.hardRefresh()
                _lastUpdated.value = formatTimestamp(System.currentTimeMillis())
                checkServerStatus()
                _uiEvent.send(UiEvent.ShowSnackbar("🗑️ Cache cleared. All results reloaded from SPPU."))
            } catch (e: Exception) {
                val msg = deriveErrorMessage(e)
                _uiEvent.send(UiEvent.ShowErrorDialog("Hard Refresh Failed", msg))
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    private fun deriveErrorMessage(e: Exception): String {
        val msg = e.message?.lowercase() ?: return "Network error: Unknown"
        return when {
            msg.contains("502") -> "SPPU server is down (502 Bad Gateway)."
            msg.contains("503") -> "SPPU server is busy (503 Service Unavailable)."
            msg.contains("504") -> "SPPU server timed out (504 Gateway Time-out)."
            msg.contains("timeout") || msg.contains("timed out") -> "Connection timed out."
            msg.contains("unreachable") -> "Server unreachable."
            else -> "Network error: ${e.message}"
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

    sealed class UiEvent {
        data class ShowSnackbar(val message: String) : UiEvent()
        data class ShowErrorDialog(val title: String, val message: String) : UiEvent()
    }

    fun toggleShowBookmarks() {
        _showBookmarks.value = !_showBookmarks.value
    }

    fun dismissBookmarks() {
        _showBookmarks.value = false
    }

    fun toggleBookmark(resultId: String) {
        viewModelScope.launch {
            repository.toggleBookmark(resultId)
        }
    }

    fun markAsViewed(resultId: String) {
        viewModelScope.launch {
            repository.markAsViewed(resultId)
        }
    }

    companion object {
        private val WHITESPACE_REGEX = Regex("\\s+")
    }
}

private fun formatTimestamp(millis: Long): String {
    val sdf = java.text.SimpleDateFormat("dd MMM yyyy, HH:mm", java.util.Locale.getDefault())
    return sdf.format(java.util.Date(millis))
}
