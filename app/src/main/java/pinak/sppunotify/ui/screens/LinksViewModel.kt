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
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class LinksViewModel @Inject constructor() : ViewModel() {

    private val _examTimeTableLinks = MutableStateFlow<List<SppuLink>>(emptyList())
    val examTimeTableLinks: StateFlow<List<SppuLink>> = _examTimeTableLinks.asStateFlow()

    private val _archiveQpLinks = MutableStateFlow<List<SppuLink>>(emptyList())
    val archiveQpLinks: StateFlow<List<SppuLink>> = _archiveQpLinks.asStateFlow()

    private val _isDiscovering = MutableStateFlow(false)
    val isDiscovering: StateFlow<Boolean> = _isDiscovering.asStateFlow()

    private val _discoveryMessage = MutableStateFlow("")
    val discoveryMessage: StateFlow<String> = _discoveryMessage.asStateFlow()

    init {
        discoverExamLinks()
    }

    /** Generates plausible exam timetable URLs and Archive QP links and discovers which exist */
    fun discoverExamLinks() {
        viewModelScope.launch {
            _isDiscovering.value = true
            _discoveryMessage.value = "Scanning for exam documents..."
            
            // Verified folder names from university server source
            val timetableFolders = listOf(
                "MARAPR%202025" to "Time Tables MAR/APR 2025",
                "Time%20Tables%20APRMAY%202026" to "Time Tables APR/MAY 2026",
                "Time%20Tables%20OCTNOV%202025" to "Time Tables OCT/NOV 2025",
                "Time%20Tables%20OCTNOV%202024" to "Time Tables OCT/NOV 2024",
                "Time%20Tables%20MARAPR%202024" to "Time Tables MAR/APR 2024",
                "Time%20Tables%20OCTNOV%202023" to "Time Tables OCT/NOV 2023",
                "Timetables%20MARAPR%202023" to "Timetables MAR/APR 2023",
                "Timetable%20OCTNOV%202022" to "Timetables OCT/NOV 2022",
                "Timetable%20MARAPR%202022%20Scheduled%20in%20June%202022" to "Timetable MAR/APR 2022 (June 2022)",
                "Timetable%20OCTNOV%202021%20Scheduled%20in%20FebMarch%202022" to "Timetable OCT/NOV 2021 (Feb 2022)",
                "Timetable%20AprilMay%202021%20Scheduled%20in%20JulyAugust%2020" to "Timetable April/May 2021 (July 2021)",
                "OctNov%202020%20Scheduled%20in%20AprilMay%202021" to "Timetable Oct/Nov 2020 (April 2021)",
                "FinalYearBacklogFirstHalf2020" to "Final Year & Backlog First Half 2020",
                "Time%20Table%20First%20Half%202020" to "Time Table First Half 2020",
                "Time%20Table%202nd%20Half%202019" to "Time Table Second Half 2019",
                "Time%20Table%20First%20Half%202019" to "Time Table First Half 2019",
                "Examination%20Time%20Table%202018%202nd%20half" to "Examination Time Table 2018 2nd Half",
                "Examination%20Time%20Table%202018" to "Examination Time Table 2018 1st Half"
            )

            // Archive folders based on sidebar list
            val archiveFolders = listOf(
                "APRIL%20-%202025" to "April 2025",
                "November%202024" to "November 2024",
                "April-2024" to "April 2024",
                "OCTOBER%20-%202023" to "October 2023",
                "APRIL%20-%202023" to "April 2023",
                "OCTOBER%20-%202022" to "October 2022",
                "APRIL-2022" to "April 2022",
                "APRIL-2019" to "April 2019",
                "OCTOBER-2018" to "October 2018",
                "April%202018" to "April 2018",
                "October%202017" to "October 2017",
                "April%202017" to "April 2017",
                "October%202016" to "October 2016",
                "April/May%202016" to "April/May 2016",
                "April/May%202015" to "April/May 2015",
                "November/December%202015" to "Nov/Dec 2015"
            )

            val discoveredTimetables = withContext(Dispatchers.IO) {
                timetableFolders.filter { urlExists("http://collegecirculars.unipune.ac.in/sites/examdocs/${it.first}/Forms/AllItems.aspx") }
                    .map { SppuLink(it.second, "http://collegecirculars.unipune.ac.in/sites/examdocs/${it.first}/Forms/AllItems.aspx", "Exam Time Tables") }
            }

            val discoveredArchives = withContext(Dispatchers.IO) {
                archiveFolders.filter { urlExists("http://collegecirculars.unipune.ac.in/sites/examdocs/Archive%20Question%20Papers/Forms/AllItems.aspx?RootFolder=%2fsites%2fexamdocs%2fArchive%20Question%20Papers%2f${it.first}") }
                    .map { SppuLink("${it.second} Archive", "http://collegecirculars.unipune.ac.in/sites/examdocs/Archive%20Question%20Papers/Forms/AllItems.aspx?RootFolder=%2fsites%2fexamdocs%2fArchive%20Question%20Papers%2f${it.first}", "Archive Question Papers") }
            }

            _examTimeTableLinks.value = discoveredTimetables
            _archiveQpLinks.value = discoveredArchives
            
            val total = discoveredTimetables.size + discoveredArchives.size
            _discoveryMessage.value = if (total > 0) "Discovered $total exam documents"
                                     else "No new documents found"
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
