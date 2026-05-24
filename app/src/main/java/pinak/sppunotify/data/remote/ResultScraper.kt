package pinak.sppunotify.data.remote

import android.annotation.SuppressLint
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.jsoup.Connection
import org.jsoup.Jsoup
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

@SuppressLint("CustomX509TrustManager", "TrustAllX509TrustManager")
@Singleton
class ResultScraper @Inject constructor() {

    companion object {
        private const val BASE_URL = "https://onlineresults.unipune.ac.in"
        private const val DASHBOARD_URL = "$BASE_URL/Result/Dashboard/Default"
        private const val TAG = "ResultScraper"
        private const val TIMEOUT_MS = 15000
        private const val HEALTH_CHECK_TIMEOUT_MS = 8000
        
        private val trustAllContext: SSLContext by lazy {
            val sslContext = SSLContext.getInstance("TLS")
            sslContext.init(null, arrayOf<TrustManager>(object : X509TrustManager {
                override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
            }), SecureRandom())
            sslContext
        }
        
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
        
        private val DATE_PATTERNS = arrayOf(
            "d MMM yyyy",
            "d MMMM yyyy",
            "dd MM yyyy",
            "dd/MM/yyyy",
            "dd-MM-yyyy",
            "yyyy-MM-dd"
        )

        /**
         * Parses a date string into a timestamp with high tolerance for malformed strings.
         * Handles various separators and month names.
         */
        fun parseDateToTimestamp(dateStr: String): Long {
            if (dateStr.isBlank()) return 0L
            
            // Normalize separators and extra whitespace: "18- May- 2026" -> "18 May 2026"
            val clean = dateStr.trim()
                .replace(Regex("[\\s-]+"), " ")
                .replace(Regex("(?i)\\b([0-9])\\b"), "0$1") // pad single digits: "5" -> "05"
            
            // Try explicit patterns first
                for (pattern in DATE_PATTERNS) {
                    try {
                        val sdf = java.text.SimpleDateFormat(pattern, Locale.ENGLISH)
                        sdf.isLenient = true
                        val date = sdf.parse(clean)
                        if (date != null) {
                            val cal = java.util.Calendar.getInstance()
                            cal.time = date
                            // Handle 2-digit years or weird offsets
                            if (cal.get(java.util.Calendar.YEAR) < 100) {
                                cal.add(java.util.Calendar.YEAR, 2000)
                            }
                            // Reset time to start of day for consistent sorting
                            cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
                            cal.set(java.util.Calendar.MINUTE, 0)
                            cal.set(java.util.Calendar.SECOND, 0)
                            cal.set(java.util.Calendar.MILLISECOND, 0)
                            return cal.timeInMillis
                        }
                    } catch (e: Exception) {
                        // Continue to next pattern
                    }
                }

            // Robust fallback using regex extraction
            return fallbackRegexParse(clean)
        }

        private fun fallbackRegexParse(clean: String): Long {
            return try {
                val currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
                val year = FOUR_DIGIT_YEAR_REGEX.find(clean)?.groupValues?.get(1)?.toIntOrNull() ?: currentYear
                val month = MONTH_ABBREV_REGEX.find(clean)?.groupValues?.get(1)?.lowercase()?.let { MONTH_MAP[it] }
                val day = TWO_TO_ONE_DIGIT_DAY_REGEX.findAll(clean)
                    .mapNotNull { it.groupValues[1].toIntOrNull() }
                    .firstOrNull { it in 1..31 } ?: 1
                
                if (month != null) {
                    createTimestamp(year, month, day)
                } else {
                    0L
                }
            } catch (e: Exception) {
                0L
            }
        }

        private fun createTimestamp(y: Int, m: Int, d: Int): Long {
            return java.util.Calendar.getInstance(Locale.ENGLISH).apply {
                set(y, m, d, 0, 0, 0)
                set(java.util.Calendar.MILLISECOND, 0)
            }.timeInMillis
        }
    }

    private val userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36"

    /**
     * Creates a new Jsoup session with global configuration.
     */
    private fun newSession() = Jsoup.newSession()
        .userAgent(userAgent)
        .referrer(DASHBOARD_URL)
        .timeout(TIMEOUT_MS)
        .sslSocketFactory(trustAllContext.socketFactory)
        .followRedirects(true)

    /**
     * High-reliability server health check using manual connection pooling.
     */
    suspend fun checkServerHealth(): ServerStatus = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        val urls = listOf(DASHBOARD_URL, DASHBOARD_URL.replace("https://", "http://"), BASE_URL)
        
        for (url in urls) {
            val status = tryConnect(url, startTime)
            if (status.isOnline) return@withContext status
        }
        
        ServerStatus(isOnline = false, responseTimeMs = -1)
    }

    private fun tryConnect(url: String, startTime: Long): ServerStatus {
        var connection: HttpURLConnection? = null
        return try {
            connection = (URL(url).openConnection() as HttpURLConnection).apply {
                if (this is HttpsURLConnection) {
                    sslSocketFactory = trustAllContext.socketFactory
                    hostnameVerifier = HostnameVerifier { _, _ -> true }
                }
                requestMethod = "GET"
                connectTimeout = HEALTH_CHECK_TIMEOUT_MS
                readTimeout = HEALTH_CHECK_TIMEOUT_MS
                instanceFollowRedirects = true
                
                // Essential headers to avoid being blocked
                setRequestProperty("User-Agent", userAgent)
                setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                setRequestProperty("Accept-Language", "en-US,en;q=0.5")
                setRequestProperty("Connection", "close")
            }
            
            val responseCode = connection.responseCode
            val duration = System.currentTimeMillis() - startTime
            
            ServerStatus(isOnline = true, statusCode = responseCode, responseTimeMs = duration)
        } catch (e: Exception) {
            Log.w(TAG, "Ping failed for $url: ${e.message}")
            ServerStatus(isOnline = false, responseTimeMs = -1)
        } finally {
            connection?.disconnect()
        }
    }

    suspend fun scrapeLatestResults(): List<ResultDto> = withContext(Dispatchers.IO) {
        val initialSes = newSession()
        val sessionPeriods = fetchSessionPeriods(initialSes)

        coroutineScope {
            sessionPeriods.map { period ->
                async {
                    scrapeResultsForPeriod(period)
                }
            }.awaitAll().flatten().distinctBy { it.id }
        }
    }

    private suspend fun scrapeResultsForPeriod(period: Int): List<ResultDto> = withContext(Dispatchers.IO) {
        val results = mutableListOf<ResultDto>()
        try {
            val url = if (period == 0) DASHBOARD_URL else "$BASE_URL/Result/Dashboard/session?Exam_Period=$period"
            val doc = newSession().url(url).get()
            
            doc.select("#tblRVList tr").drop(1).forEach { row ->
                val cols = row.select("td")
                if (cols.size < 4) return@forEach

                val title = cols[1].wholeText().trim()
                val date = cols[2].wholeText().trim()
                if (title.isEmpty() || date.isEmpty()) return@forEach

                val actionHtml = cols[3].html()
                val patternName = extractPatternParam(actionHtml, 0)
                val patternId = extractPatternParam(actionHtml, 1)

                val id = (title + date).hashCode().toString()
                val viewUrl = buildViewUrl(patternName, patternId)

                results.add(ResultDto(id, title, viewUrl, date, patternName, patternId))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Scrape failed for session $period: ${e.message}")
        }
        results
    }

    private fun buildViewUrl(name: String, id: String): String {
        return if (name.isNotEmpty() && id.isNotEmpty()) {
            "$DASHBOARD_URL?PatternName=${URLEncoder.encode(name, "UTF-8")}&PatternID=${URLEncoder.encode(id, "UTF-8")}"
        } else DASHBOARD_URL
    }

    private suspend fun fetchSessionPeriods(ses: Connection): List<Int> = withContext(Dispatchers.IO) {
        val periods = mutableListOf(0)
        try {
            val resp = ses.url("$BASE_URL/Result/Dashboard/GetSession")
                .header("X-Requested-With", "XMLHttpRequest")
                .ignoreContentType(true).execute()

            val raw = resp.body().trim()
            if (raw.isNotEmpty() && (raw.startsWith("[") || raw.startsWith("{"))) {
                val arr = org.json.JSONArray(raw)
                for (i in 0 until arr.length()) {
                    val p = arr.optJSONObject(i)?.optInt("Exam_Period", 0) ?: 0
                    if (p != 0) periods.add(p)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Session fetch failed: ${e.message}")
        }
        periods.distinct()
    }

    private fun extractPatternParam(html: String, index: Int): String {
        return try {
            val match = Regex("Enterdetails\\(([^)]+)\\)").find(html)
            val onclick = match?.groupValues?.getOrNull(1) ?: return ""
            val parts = onclick.split(",")
            if (index >= parts.size) "" else parts[index].trim().removeSurrounding("'")
        } catch (e: Exception) {
            ""
        }
    }

    data class CaptchaData(val imageBase64: String, val orgCaptchaText: String)
    data class SubmitResult(val bytes: ByteArray, val mimeType: String)

    suspend fun fetchCaptcha(): CaptchaData? = withContext(Dispatchers.IO) {
        val ses = newSession()
        try {
            ses.url("$BASE_URL/Result/Dashboard/Default").get()

            val resp = ses.url("$BASE_URL/Result/Dashboard/RFCTLN")
                .header("X-Requested-With", "XMLHttpRequest")
                .method(Connection.Method.POST).ignoreContentType(true).execute()

            val body = resp.body()
            if (body.isNotEmpty()) {
                val json = org.json.JSONObject(body)
                val img = json.optString("CaptchaImageSTR", "")
                val txt = json.optString("OrgCaptchaText", "")
                if (img.isNotEmpty() && txt.isNotEmpty()) CaptchaData(img, txt) else null
            } else null
        } catch (e: Exception) {
            Log.e(TAG, "Captcha fetch failed: ${e.message}", e)
            null
        }
    }

    suspend fun validateCaptcha(userText: String, orgText: String): Boolean = withContext(Dispatchers.IO) {
        val ses = newSession()
        try {
            val resp = ses.url("$BASE_URL/Result/Dashboard/VALCHCT")
                .header("X-Requested-With", "XMLHttpRequest")
                .method(Connection.Method.POST)
                .data("ctxt", userText).data("hct", orgText)
                .ignoreContentType(true).execute()
            val body = resp.body().trim().removeSurrounding("\"")
            body == "1" || body == "2"
        } catch (e: Exception) {
            Log.e(TAG, "VALCHCT failed: ${e.message}", e)
            false
        }
    }

    suspend fun submitResult(
        patternName: String, patternId: String, seatNo: String, motherName: String,
        captchaText: String, orgCaptchaText: String, captchaImageStr: String,
    ): SubmitResult? = withContext(Dispatchers.IO) {
        val ses = newSession()
        try {
            val resp = ses.url("$BASE_URL/SPPU%20ONLINE%20RESULT%20DISPLAY")
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
            Log.e(TAG, "submitResult error: ${e.message}", e)
            null
        }
    }
}
