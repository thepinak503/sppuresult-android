package pinak.sppunotify.data.remote

import android.util.Log
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Connection
import org.jsoup.Jsoup
import java.net.URLEncoder
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ResultScraper @Inject constructor() {

    companion object {
        private const val BASE_URL = "https://onlineresults.unipune.ac.in"
        private const val DASHBOARD_URL = "$BASE_URL/Result/Dashboard/Default"
        private const val TAG = "ResultScraper"
        
        private val MONTH_MAP = mapOf(
            "jan" to 0, "january" to 0,
            "feb" to 1, "february" to 1,
            "mar" to 2, "march" to 2,
            "apr" to 3, "april" to 3,
            "may" to 4,
            "jun" to 5, "june" to 5,
            "jul" to 6, "july" to 6,
            "aug" to 7, "august" to 7,
            "sep" to 8, "sept" to 8, "september" to 8,
            "oct" to 9, "october" to 9,
            "nov" to 10, "november" to 10,
            "dec" to 11, "december" to 11,
        )
        
        private val FOUR_DIGIT_YEAR_REGEX = Regex("\\b(\\d{4})\\b")
        private val TWO_TO_ONE_DIGIT_DAY_REGEX = Regex("\\b(\\d{1,2})\\b")
        private val MONTH_ABBREV_REGEX = Regex("\\b([A-Za-z]{3,9})\\b", RegexOption.IGNORE_CASE)
        
        fun parseDateToTimestamp(dateStr: String): Long {
            val clean = dateStr.trim()
            
            val yearMatch = FOUR_DIGIT_YEAR_REGEX.find(clean)
            val year = yearMatch?.groupValues?.get(1)?.toIntOrNull() ?: run {
                Log.w(TAG, "Failed to extract year from: $dateStr")
                return 0L
            }
            
            val monthMatch = MONTH_ABBREV_REGEX.find(clean)
            val monthStr = monthMatch?.groupValues?.get(1)?.lowercase() ?: run {
                Log.w(TAG, "Failed to extract month from: $dateStr")
                return 0L
            }
            val month = MONTH_MAP[monthStr] ?: run {
                Log.w(TAG, "Unknown month: $monthStr from: $dateStr")
                return 0L
            }
            
            val dayMatches = TWO_TO_ONE_DIGIT_DAY_REGEX.findAll(clean).toList()
            val day = dayMatches
                .mapNotNull { it.groupValues[1].toIntOrNull() }
                .filter { it in 1..31 }
                .firstOrNull() ?: run {
                Log.w(TAG, "Failed to extract day from: $dateStr")
                return 0L
            }
            
            val cal = java.util.Calendar.getInstance(Locale.ENGLISH)
            cal.set(java.util.Calendar.YEAR, year)
            cal.set(java.util.Calendar.MONTH, month)
            cal.set(java.util.Calendar.DAY_OF_MONTH, day)
            cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
            cal.set(java.util.Calendar.MINUTE, 0)
            cal.set(java.util.Calendar.SECOND, 0)
            cal.set(java.util.Calendar.MILLISECOND, 0)
            
            return cal.timeInMillis
        }
    }

    private val userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36"

    private val session = Jsoup.newSession()
        .userAgent(userAgent)
        .referrer(DASHBOARD_URL)
        .timeout(30000)
        .followRedirects(true)

    suspend fun scrapeLatestResults(): List<ResultDto> = withContext(Dispatchers.IO) {
        val allResults = mutableListOf<ResultDto>()
        val seenIds = mutableSetOf<String>()

        val sessionPeriods = fetchSessionPeriods()

        for (period in sessionPeriods) {
            try {
                val url = if (period == 0) DASHBOARD_URL else "$BASE_URL/Result/Dashboard/session?Exam_Period=$period"
                Log.d(TAG, "Fetching: $url")

                val doc = session.url(url).header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                    .header("Accept-Language", "en-US,en;q=0.5").get()
                val rows = doc.select("#tblRVList tr")

                for ((i, row) in rows.withIndex()) {
                    if (i == 0) continue
                    val cols = row.select("td")
                    if (cols.size < 4) continue

                    val title = cols[1].wholeText().trim()
                    val date = cols[2].wholeText().trim()
                    if (title.isEmpty() || date.isEmpty()) continue

                    val actionHtml = cols[3].html()
                    val patternName = extractPatternParam(actionHtml, 0)
                    val patternId = extractPatternParam(actionHtml, 1)

                    val id = (title + date).hashCode().toString()
                    if (id in seenIds) continue
                    seenIds.add(id)

                    val viewUrl = if (patternName.isNotEmpty() && patternId.isNotEmpty())
                        "$DASHBOARD_URL?PatternName=${URLEncoder.encode(patternName, "UTF-8")}&PatternID=${URLEncoder.encode(patternId, "UTF-8")}"
                    else DASHBOARD_URL

                    allResults.add(ResultDto(id, title, viewUrl, date, patternName, patternId))
                }

                Log.d(TAG, "Session $period: ${rows.size - 1} data rows parsed")
            } catch (e: Exception) {
                Log.e(TAG, "Failed session $period: ${e.message}")
            }
        }

        Log.d(TAG, "Total unique results: ${allResults.size}")
        allResults
    }

    private suspend fun fetchSessionPeriods(): List<Int> = withContext(Dispatchers.IO) {
        val periods = mutableListOf(0)
        try {
            val resp = session.url("$BASE_URL/Result/Dashboard/GetSession")
                .header("X-Requested-With", "XMLHttpRequest")
                .ignoreContentType(true).execute()

            val raw = resp.body().trim()
            if (raw.startsWith("[")) {
                val arr = org.json.JSONArray(raw)
                for (i in 0 until arr.length()) {
                    val p = arr.getJSONObject(i).optInt("Exam_Period", 0)
                    if (p != 0) periods.add(p)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Session fetch failed: ${e.message}")
        }
        periods.distinct()
    }

    private fun extractPatternParam(html: String, index: Int): String {
        val match = Regex("Enterdetails\\(([^)]+)\\)").find(html)
        val onclick = match?.groupValues?.getOrNull(1) ?: return ""
        val parts = onclick.split(",")
        if (index >= parts.size) return ""
        return parts[index].trim().removeSurrounding("'")
    }

    data class CaptchaData(val imageBase64: String, val orgCaptchaText: String)
    data class SubmitResult(val bytes: ByteArray, val mimeType: String)

    suspend fun fetchCaptcha(): CaptchaData? = withContext(Dispatchers.IO) {
        try {
            session.url("$BASE_URL/Result/Dashboard/Default").get()

            val resp = session.url("$BASE_URL/Result/Dashboard/RFCTLN")
                .header("X-Requested-With", "XMLHttpRequest")
                .method(Connection.Method.POST).ignoreContentType(true).execute()

            val json = org.json.JSONObject(resp.body())
            val img = json.optString("CaptchaImageSTR", "")
            val txt = json.optString("OrgCaptchaText", "")
            if (img.isNotEmpty() && txt.isNotEmpty()) CaptchaData(img, txt) else null
        } catch (e: Exception) {
            Log.e(TAG, "Captcha fetch failed: ${e.message}", e); null
        }
    }

    suspend fun validateCaptcha(userText: String, orgText: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val resp = session.url("$BASE_URL/Result/Dashboard/VALCHCT")
                .header("X-Requested-With", "XMLHttpRequest")
                .method(Connection.Method.POST)
                .data("ctxt", userText).data("hct", orgText)
                .ignoreContentType(true).execute()
            val body = resp.body().trim().removeSurrounding("\"")
            body == "1" || body == "2"
        } catch (e: Exception) { Log.e(TAG, "VALCHCT failed: ${e.message}", e); false }
    }

    suspend fun submitResult(
        patternName: String, patternId: String, seatNo: String, motherName: String,
        captchaText: String, orgCaptchaText: String, captchaImageStr: String,
    ): SubmitResult? = withContext(Dispatchers.IO) {
        try {
            val resp = session.url("$BASE_URL/SPPU%20ONLINE%20RESULT%20DISPLAY")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .method(Connection.Method.POST)
                .data("PatternID", patternId)
                .data("PatternName", patternName)
                .data("SeatNo", seatNo)
                .data("MotherName", motherName)
                .data("CaptchaText", captchaText)
                .data("OrgCaptchaText", orgCaptchaText)
                .data("CaptchaImageSTR", captchaImageStr)
                .ignoreContentType(true).execute()

            if (resp.statusCode() == 200) {
                val ct = resp.contentType() ?: "application/octet-stream"
                val body = resp.bodyAsBytes()
                SubmitResult(body, ct)
            } else {
                Log.e(TAG, "submitResult HTTP ${resp.statusCode()}")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "submitResult error: ${e.message}", e); null
        }
    }
}
