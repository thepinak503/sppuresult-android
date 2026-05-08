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
import javax.inject.Inject

enum class SortOrder { NEWEST_FIRST, OLDEST_FIRST }

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

    val departments = listOf(
        "All", "FE", "SE", "TE", "BE",
        "MBA", "MCA", "M.Sc", "M.A./M.Com",
        "B.Sc", "B.Com", "BBA/BCA", "B.A.",
        "B.Pharm", "Other UG", "Other PG",
        "Law", "Diploma",
    )

    private fun classifyDept(title: String): String {
        val n = title.replace(Regex("^(FIRST|SECOND|THIRD|FOURTH|FINAL)\\s+YEAR\\s+", RegexOption.IGNORE_CASE), "")
            .replace(Regex("^FirstYear\\s+"), "")
        val nu = n.uppercase()
        // FE
        if (Regex("^F\\.?\\s*E\\.?\\s*(\\(|\\d)", RegexOption.IGNORE_CASE).containsMatchIn(title) ||
            Regex("^FIRST\\s+YEAR\\s+ENGINEERING", RegexOption.IGNORE_CASE).containsMatchIn(title)
        ) return "FE"
        // SE
        if (Regex("^S\\.?\\s*E\\.?\\s*(\\(|\\d)", RegexOption.IGNORE_CASE).containsMatchIn(title)) return "SE"
        // TE
        if (Regex("^T\\.?\\s*E\\.?\\s*(\\(|\\d)", RegexOption.IGNORE_CASE).containsMatchIn(title)) return "TE"
        // BE
        if (Regex("^B\\.?\\s*E\\.?\\s*(\\(|\\d)", RegexOption.IGNORE_CASE).containsMatchIn(title)) return "BE"
        // MBA
        if (Regex("^M\\.?\\s*B\\.?\\s*A", RegexOption.IGNORE_CASE).containsMatchIn(n) ||
            Regex("\\bMBA\\b").containsMatchIn(nu) ||
            Regex("MASTER\\s+OF\\s+BUSINESS\\s+ADMINISTRATION", RegexOption.IGNORE_CASE).containsMatchIn(nu)
        ) return "MBA"
        // MCA / MCS
        if (Regex("^M\\.?(CA|CS)\\b", RegexOption.IGNORE_CASE).containsMatchIn(n) ||
            Regex("MASTER\\s+OF\\s+COMPUTER\\s+(?:APPLICATION|APPLICATIONS)", RegexOption.IGNORE_CASE).containsMatchIn(nu)
        ) return "MCA"
        // Other Masters (Pharm, Arch, Ed, etc.)
        if (Regex("^M\\.?\\s*(?:PHARM|ARCH|ED|E\\.?)\\b", RegexOption.IGNORE_CASE).containsMatchIn(n) ||
            Regex("^MASTER\\s+OF\\s+(?:LIBRARY|EDUCATION|HOSPITAL|PHARMACY|ARCHITECTURE|ENGINEERING)", RegexOption.IGNORE_CASE).containsMatchIn(nu)
        ) return "Other PG"
        // M.Sc
        if (Regex("^M\\.?\\s*SC\\b", RegexOption.IGNORE_CASE).containsMatchIn(n)) return "M.Sc"
        // M.A. / M.Com
        if (Regex("^M\\.?\\s*(?:A|COM)\\b", RegexOption.IGNORE_CASE).containsMatchIn(n) ||
            Regex("^MASTER\\s+OF\\s+(?:COMMERCE|ARTS)", RegexOption.IGNORE_CASE).containsMatchIn(nu)
        ) return "M.A./M.Com"
        // Catch-all Master of
        if (Regex("^MASTER\\s+OF\\b", RegexOption.IGNORE_CASE).containsMatchIn(n) ||
            Regex("^MASTERS?\\s+IN\\b", RegexOption.IGNORE_CASE).containsMatchIn(n)
        ) return "Other PG"
        // Law
        if (Regex("^LL[BMD]\\b").containsMatchIn(title) ||
            Regex("^B\\.?\\s*A\\.?\\s*LL", RegexOption.IGNORE_CASE).containsMatchIn(title) ||
            Regex("^LL[BMD]\\b").containsMatchIn(n) ||
            Regex("^B\\.?\\s*A\\.?\\s*LL", RegexOption.IGNORE_CASE).containsMatchIn(n)
        ) return "Law"
        // Diploma
        if (Regex("^DIPLOMA\\b", RegexOption.IGNORE_CASE).containsMatchIn(title) ||
            Regex("^POST\\s+GRADUATE\\s+DIPLOMA", RegexOption.IGNORE_CASE).containsMatchIn(title)
        ) return "Diploma"
        if (Regex("^POST\\s+GRADUATE\\s+", RegexOption.IGNORE_CASE).containsMatchIn(title)) return "Diploma"
        // Year-prefixed UG programs
        if (Regex("^(FIRST|SECOND|THIRD|FOURTH|FINAL)\\s+YEAR\\s+", RegexOption.IGNORE_CASE).containsMatchIn(title) ||
            Regex("^FirstYear\\s+").containsMatchIn(title)
        ) {
            if (Regex("BACHELOR\\s+OF\\s+SCIENCE", RegexOption.IGNORE_CASE).containsMatchIn(nu) ||
                Regex("B\\.?\\s*SC", RegexOption.IGNORE_CASE).containsMatchIn(n)
            ) return "B.Sc"
            if (Regex("BACHELOR\\s+OF\\s+COMMERCE", RegexOption.IGNORE_CASE).containsMatchIn(nu) ||
                Regex("B\\.?\\s*COM\\b", RegexOption.IGNORE_CASE).containsMatchIn(n)
            ) return "B.Com"
            if (Regex("BACHELOR\\s+OF\\s+ARTS", RegexOption.IGNORE_CASE).containsMatchIn(nu) ||
                Regex("B\\.?\\s*A\\b(?!\\.?\\s*LL)", RegexOption.IGNORE_CASE).containsMatchIn(n)
            ) return "B.A."
            if (Regex("BACHELOR\\s+OF\\s+BUSINESS\\s+ADMINISTRATION", RegexOption.IGNORE_CASE).containsMatchIn(nu) ||
                Regex("B\\.?\\s*B\\.?\\s*A", RegexOption.IGNORE_CASE).containsMatchIn(n) ||
                Regex("BACHELOR\\s+OF\\s+COMPUTER\\s+APPLICATION", RegexOption.IGNORE_CASE).containsMatchIn(nu) ||
                Regex("B\\.?\\s*C\\.?\\s*A", RegexOption.IGNORE_CASE).containsMatchIn(n)
            ) return "BBA/BCA"
            return "Other UG"
        }
        // Direct UG codes
        if (Regex("^B\\.?\\s*SC\\b", RegexOption.IGNORE_CASE).containsMatchIn(n)) return "B.Sc"
        if (Regex("^B\\.?\\s*COM\\b", RegexOption.IGNORE_CASE).containsMatchIn(n)) return "B.Com"
        if (Regex("^B\\.?(?:BA|B\\.?\\s*A)\\b", RegexOption.IGNORE_CASE).containsMatchIn(n)) return "BBA/BCA"
        if (Regex("^B\\.?\\s*CA\\b", RegexOption.IGNORE_CASE).containsMatchIn(n)) return "BBA/BCA"
        if (Regex("^B\\.?\\s*A\\b(?!\\.?\\s*LL)", RegexOption.IGNORE_CASE).containsMatchIn(n)) return "B.A."
        if (Regex("^B\\.?(?:PHARM|ARCH|ED|HMCT)\\b", RegexOption.IGNORE_CASE).containsMatchIn(n)) return "Other UG"
        if (Regex("^BACHELOR\\s+(?:OF|IN)\\s+", RegexOption.IGNORE_CASE).containsMatchIn(nu)) {
            if (Regex("SCIENCE", RegexOption.IGNORE_CASE).containsMatchIn(nu)) return "B.Sc"
            if (Regex("COMMERCE", RegexOption.IGNORE_CASE).containsMatchIn(nu)) return "B.Com"
            if (Regex("ARTS", RegexOption.IGNORE_CASE).containsMatchIn(nu)) return "B.A."
            if (Regex("BUSINESS\\s+ADMINISTRATION", RegexOption.IGNORE_CASE).containsMatchIn(nu) ||
                Regex("COMPUTER\\s+APPLICATION", RegexOption.IGNORE_CASE).containsMatchIn(nu)
            ) return "BBA/BCA"
            return "Other UG"
        }
        // LLM
        if (Regex("^LLM\\b").containsMatchIn(title) || Regex("^LLM\\b").containsMatchIn(n)) return "Law"
        // B.Pharm
        if (Regex("B\\.?PHR?ARM", RegexOption.IGNORE_CASE).containsMatchIn(n)) return "B.Pharm"
        return "Other UG"
    }

    val results: StateFlow<List<ResultEntity>> = combine(
        repository.results,
        _searchQuery,
        _selectedDepartment,
        _sortOrder,
    ) { results, query, dept, sortOrder ->
        _totalCount.value = results.size
        val filtered = results.filter { result ->
            val matchesQuery = if (query.isBlank()) true else {
                fuzzyMatch(result.title, query) ||
                fuzzyMatch(result.patternName, query) ||
                fuzzyMatch(result.publishedDate, query) ||
                fuzzyMatch(result.url, query)
            }
            val matchesDept = dept == "All" || classifyDept(result.title) == dept
            matchesQuery && matchesDept
        }
        val dateSorted = if (sortOrder == SortOrder.NEWEST_FIRST) filtered else filtered.reversed()
        if (query.isBlank()) dateSorted else dateSorted.sortedByDescending { rankResult(it, query) }
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

    fun toggleSortOrder() {
        _sortOrder.value = when (_sortOrder.value) {
            SortOrder.NEWEST_FIRST -> SortOrder.OLDEST_FIRST
            SortOrder.OLDEST_FIRST -> SortOrder.NEWEST_FIRST
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                val newResults = repository.fetchResults()
                if (newResults.isEmpty()) {
                    // Check if we already have cached data
                    val cachedCount = repository.getCachedCount()
                    if (cachedCount == 0) {
                        _uiEvent.send(UiEvent.ShowSnackbar("No results loaded. Pull down to retry."))
                    }
                } else {
                    _uiEvent.send(UiEvent.ShowSnackbar("${newResults.size} new result(s) found"))
                }
            } catch (e: Exception) {
                val msg = when {
                    e.message?.contains("502") == true -> "SPPU server is down (502 Bad Gateway). Please try again later."
                    e.message?.contains("503") == true -> "SPPU server is busy (503 Service Unavailable). Try again later."
                    e.message?.contains("504") == true -> "SPPU server timed out (504 Gateway Time-out). The server didn't respond in time."
                    e.message?.contains("timeout", ignoreCase = true) == true -> "Connection timed out. SPPU server may be slow."
                    e.message?.contains("SSL", ignoreCase = true) == true -> "SSL connection error. SPPU certificate issue detected."
                    e.message?.contains("refused", ignoreCase = true) == true -> "Connection refused. SPPU server may be down."
                    e.message?.contains("reset", ignoreCase = true) == true -> "Connection reset by server. Try again."
                    e.message?.contains("unreachable", ignoreCase = true) == true -> "Host unreachable. Check your internet connection."
                    e.message?.contains("DNS", ignoreCase = true) == true -> "DNS lookup failed. Check your internet connection."
                    else -> "Something went wrong: ${e.message ?: "Unknown error"}"
                }
                _uiEvent.send(UiEvent.ShowErrorDialog("Warning", msg))
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    private fun fuzzyMatch(target: String, query: String): Boolean {
        if (query.isBlank()) return true
        val targetLower = target.lowercase()
        val tokens = query.lowercase().trim().split(Regex("\\s+"))
        return tokens.all { token -> tokenFuzzyMatch(targetLower, token) }
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

    private fun rankResult(result: ResultEntity, query: String): Int {
        val q = query.lowercase().trim()
        val tokens = q.split(Regex("\\s+"))
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
}
