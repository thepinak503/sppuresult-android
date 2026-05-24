package pinak.sppunotify.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import pinak.sppunotify.data.remote.CircularRssItem
import pinak.sppunotify.data.repository.CircularRepository
import javax.inject.Inject

import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.ExperimentalCoroutinesApi

@HiltViewModel
class CircularsViewModel @Inject constructor(
    private val repository: CircularRepository
) : ViewModel() {

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing = _isRefreshing.asStateFlow()

    private val _errorMsg = MutableStateFlow("")
    val errorMsg = _errorMsg.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val circulars: StateFlow<List<CircularRssItem>> = _searchQuery
        .flatMapLatest { query ->
            if (query.isBlank()) {
                repository.getCachedCirculars()
            } else {
                repository.searchCirculars(query)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        refresh()
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            _errorMsg.value = ""
            try {
                repository.fetchAllCirculars()
            } catch (e: Exception) {
                _errorMsg.value = "Failed to load circulars. Pull to retry."
            }
            _isRefreshing.value = false
        }
    }
}
