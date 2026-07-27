package pinak.sppunotify.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class LinksViewModel @Inject constructor() : ViewModel() {

    private val _examTimeTableLinks = MutableStateFlow<List<SppuLink>>(emptyList())
    val examTimeTableLinks: StateFlow<List<SppuLink>> = _examTimeTableLinks.asStateFlow()

    private val _isDiscovering = MutableStateFlow(false)
    val isDiscovering: StateFlow<Boolean> = _isDiscovering.asStateFlow()

    private val _discoveryMessage = MutableStateFlow("")
    val discoveryMessage: StateFlow<String> = _discoveryMessage.asStateFlow()

    init {
        discoverExamLinks()
    }

    /** Generates plausible exam timetable URLs for current + next year and discovers which exist */
    fun discoverExamLinks() {
        viewModelScope.launch {
            _isDiscovering.value = true
            _discoveryMessage.value = "Scanning for exam timetables..."
            
            val currentYear = Calendar.getInstance().get(Calendar.YEAR)
            // Extensive search from 2019 to currentYear + 2
            val years = (2019..currentYear + 2).toList()
            val sessions = listOf(
                "APRMAY" to "April/May",
                "OCTNOV" to "Oct/Nov"
            )

            val discovered = withContext(Dispatchers.IO) {
                val links = mutableListOf<SppuLink>()
                for (year in years) {
                    for ((code, label) in sessions) {
                        val url = "http://collegecirculars.unipune.ac.in/sites/examdocs/Time%20Tables%20$code%20$year/Forms/AllItems.aspx"
                        val exists = urlExists(url)
                        if (exists) {
                            links.add(SppuLink(
                                title = "Time Table $label $year",
                                url = url,
                                category = "Exam Time Tables"
                            ))
                        }
                    }
                }
                links
            }

            _examTimeTableLinks.value = discovered
            val count = discovered.size
            _discoveryMessage.value = if (count > 0) "Found $count exam timetable(s)"
                                     else "No new exam timetables found"
            _isDiscovering.value = false
        }
    }

    /** Quick HEAD request to check if a URL is reachable */
    private fun urlExists(urlString: String): Boolean {
        return try {
            val url = URL(urlString)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "HEAD"
            conn.connectTimeout = 5000
            conn.readTimeout = 5000
            conn.setRequestProperty("User-Agent", "Mozilla/5.0")
            val code = conn.responseCode
            conn.disconnect()
            code in 200..399
        } catch (_: Exception) {
            false
        }
    }
}
