package pinak.sppunotify.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import pinak.sppunotify.data.remote.RevalCourse
import pinak.sppunotify.data.repository.RevalRepository
import javax.inject.Inject

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

    init {
        loadCourses()
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
            } catch (e: Exception) {
                _errorMsg.value = e.message ?: "Failed to load"
            }
            _isLoading.value = false
        }
    }
}
