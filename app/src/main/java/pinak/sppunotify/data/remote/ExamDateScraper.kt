package pinak.sppunotify.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExamDateScraper @Inject constructor() {

    private val url = "https://examform.unipune.ac.in/Support/StuExDates.aspx"
    private val userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36"

    suspend fun scrapeExamDates(): List<ExamDateDto> = withContext(Dispatchers.IO) {
        val examDates = mutableListOf<ExamDateDto>()
        try {
            val doc = Jsoup.connect(url)
                .userAgent(userAgent)
                .timeout(30000)
                .get()

            val rows = doc.select("table tr")
            
            for (row in rows) {
                val cols = row.select("td")
                if (cols.size >= 5) {
                    val courseName = cols[0].text().trim()
                    val status = cols[1].text().trim()
                    val startDate = cols[2].text().trim()
                    val endDateWithoutLateFee = cols[3].text().trim()
                    val endDateWithLateFee = cols[4].text().trim()
                    
                    if (courseName.isNotEmpty() && !courseName.contains("Course Name", ignoreCase = true)) {
                        examDates.add(
                            ExamDateDto(
                                courseName = courseName,
                                status = status,
                                startDate = startDate,
                                endDateWithoutLateFee = endDateWithoutLateFee,
                                endDateWithLateFee = endDateWithLateFee
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        examDates
    }
}
