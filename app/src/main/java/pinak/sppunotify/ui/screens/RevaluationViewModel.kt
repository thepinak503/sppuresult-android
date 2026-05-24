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
import pinak.sppunotify.data.remote.RevalCourse
import pinak.sppunotify.data.remote.ServerStatus
import pinak.sppunotify.data.repository.RevalRepository
import javax.inject.Inject

enum class RevalSort(val label: String) {
    DEFAULT("Default Order"),
    COURSE_A_Z("Course A-Z"),
    COURSE_Z_A("Course Z-A"),
    SUBJECT_A_Z("Subject A-Z"),
    SUBJECT_Z_A("Subject Z-A"),
}

@HiltViewModel
class RevaluationViewModel @Inject constructor(
    private val repository: RevalRepository
) : ViewModel() {

    private val _courses = MutableStateFlow<List<RevalCourse>>(emptyList())
    val courses = _courses.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _errorMsg = MutableStateFlow("")
    val errorMsg = _errorMsg.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _selectedDept = MutableStateFlow("All")
    val selectedDept = _selectedDept.asStateFlow()

    private val _sortOrder = MutableStateFlow(RevalSort.DEFAULT)
    val sortOrder = _sortOrder.asStateFlow()

    private val _totalCount = MutableStateFlow(0)
    val totalCount = _totalCount.asStateFlow()

    val serverStatus = repository.serverStatus

    /** Dynamically derived department list from course data */
    val departments: List<String> get() {
        val codes = _courses.value.mapNotNull { extractDeptCode(it.course) }.distinct()
        return listOf("All") + codes.sorted()
    }

    /** Filtered + sorted courses derived from all state */
    val filteredCourses: StateFlow<List<RevalCourse>> = combine(
        _courses,
        _searchQuery,
        _selectedDept,
        _sortOrder,
    ) { courses, query, dept, sort ->
        // 1. Department filter — extract dept code from course name and match exactly
        val deptFiltered = if (dept == "All") courses
        else courses.filter { extractDeptCode(it.course) == dept }

        // 2. Search filter — match against course name + subject
        val q = query.trim().lowercase()
        val searched = if (q.isEmpty()) deptFiltered
        else {
            val tokens = q.split(Regex("\\s+"))
            deptFiltered.filter { course ->
                val target = "${course.course} ${course.subject}".lowercase()
                tokens.all { token -> target.contains(token) }
            }
        }

        // 3. Sort
        val sorted = when (sort) {
            RevalSort.DEFAULT -> searched
            RevalSort.COURSE_A_Z -> searched.sortedBy { it.course.lowercase() }
            RevalSort.COURSE_Z_A -> searched.sortedByDescending { it.course.lowercase() }
            RevalSort.SUBJECT_A_Z -> searched.sortedBy { it.subject.lowercase() }
            RevalSort.SUBJECT_Z_A -> searched.sortedByDescending { it.subject.lowercase() }
        }

        _totalCount.value = sorted.size
        sorted
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _uiEvent = Channel<UiEvent>()
    val uiEvent = _uiEvent.receiveAsFlow()

    init {
        loadCourses()
        checkServerStatus()
    }

    fun checkServerStatus() {
        viewModelScope.launch {
            try {
                repository.updateServerStatus()
            } catch (_: Exception) {}
        }
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun onDepartmentSelected(dept: String) {
        _selectedDept.value = dept
    }

    fun setSortOrder(order: RevalSort) {
        _sortOrder.value = order
    }

    fun loadCourses() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMsg.value = ""
            try {
                val cached = repository.getAllCourses()
                if (cached.isNotEmpty()) {
                    _courses.value = cached
                }
                val fresh = repository.fetchAndCacheAllCourses()
                _courses.value = fresh
                _uiEvent.send(UiEvent.ShowSnackbar("${fresh.size} courses loaded"))
            } catch (e: Exception) {
                _errorMsg.value = e.message ?: "Failed to load"
            }
            _isLoading.value = false
            checkServerStatus()
        }
    }

    sealed class UiEvent {
        data class ShowSnackbar(val message: String) : UiEvent()
    }

    companion object {
        /** Extract department code from a course name like "BE_COMPUTER" → "BE" or "B.E. (COMPUTER)" → "BE" */
        fun extractDeptCode(courseName: String): String? {
            val name = courseName.trim()

            // Pattern 1: "BE_COMPUTER", "SE_CIVIL" — prefix before underscore
            val underscoreMatch = Regex("^([A-Za-z._/]+)[_\\s]").find(name)
            if (underscoreMatch != null) {
                return normalizeDeptCode(underscoreMatch.groupValues[1])
            }

            // Pattern 2: "B.E. (COMPUTER)" — extract the degree code
            val parenMatch = Regex("^([A-Za-z._/]+)\\s*(?:\\(|\\d)").find(name)
            if (parenMatch != null) {
                return normalizeDeptCode(parenMatch.groupValues[1])
            }

            // Pattern 3: Just take the first word (handles "MBA", "MCA", etc.)
            val firstWord = name.split(Regex("[\\s_]+")).firstOrNull()
            if (firstWord != null && firstWord.length in 1..8) {
                return normalizeDeptCode(firstWord)
            }

            return null
        }

        /** Normalize abbreviated codes to canonical form */
        private fun normalizeDeptCode(code: String): String {
            return code
                .replace(".", "")
                .uppercase()
                .trim()
        }
    }
}
