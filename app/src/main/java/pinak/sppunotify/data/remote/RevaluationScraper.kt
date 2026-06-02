package pinak.sppunotify.data.remote

import android.annotation.SuppressLint
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.inject.Inject
import javax.inject.Singleton
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

@SuppressLint("CustomX509TrustManager", "TrustAllX509TrustManager")
@Singleton
class RevaluationScraper @Inject constructor() {

    companion object {
        private const val REVAL_URL = "https://pun.unipune.ac.in/revalresult/"
        private const val TAG = "RevaluationScraper"

        private val trustAllContext: SSLContext by lazy {
            val sslContext = SSLContext.getInstance("TLS")
            sslContext.init(null, arrayOf<TrustManager>(object : X509TrustManager {
                override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
            }), SecureRandom())
            sslContext
        }
    }

    private val userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36"

    private fun fetch(url: String, cookies: MutableMap<String, String>, formData: Map<String, String>? = null): String {
        val conn = URL(url).openConnection() as HttpURLConnection
        if (conn is HttpsURLConnection) {
            conn.sslSocketFactory = trustAllContext.socketFactory
            conn.hostnameVerifier = HostnameVerifier { _, _ -> true }
        }
        conn.requestMethod = if (formData != null) "POST" else "GET"
        conn.setRequestProperty("User-Agent", userAgent)
        conn.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
        conn.setRequestProperty("Referer", REVAL_URL)
        conn.setRequestProperty("Connection", "keep-alive")
        conn.instanceFollowRedirects = true
        conn.connectTimeout = 30000
        conn.readTimeout = 30000

        if (cookies.isNotEmpty()) {
            conn.setRequestProperty("Cookie", cookies.entries.joinToString("; ") { "${it.key}=${it.value}" })
        }

        if (formData != null) {
            conn.doOutput = true
            val body = formData.entries.joinToString("&") { (k, v) ->
                "${URLEncoder.encode(k, "UTF-8")}=${URLEncoder.encode(v, "UTF-8")}"
            }
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            OutputStreamWriter(conn.outputStream).use { it.write(body) }
        }

        val status = conn.responseCode
        if (status != 200) {
            val errBody = BufferedReader(InputStreamReader(conn.errorStream)).readText()
            Log.w(TAG, "HTTP $status for $url: ${errBody.take(200)}")
            return ""
        }

        // Save cookies
        conn.headerFields?.get("Set-Cookie")?.forEach { cookie ->
            val parts = cookie.split(";")[0].split("=", limit = 2)
            if (parts.size == 2) cookies[parts[0]] = parts[1]
        }

        return BufferedReader(InputStreamReader(conn.inputStream)).readText()
    }

    suspend fun scrapeCourses(stopPredicate: ((RevalCourse) -> Boolean)? = null): List<RevalCourse> = withContext(Dispatchers.IO) {
        val allCourses = mutableListOf<RevalCourse>()
        val cookies = mutableMapOf<String, String>()

        try {
            var html = fetch(REVAL_URL, cookies)
            if (html.isEmpty()) return@withContext allCourses
            var doc = Jsoup.parse(html)

            val seenKeys = mutableSetOf<String>()
            var currentPage = 1
            var shouldStop = false

            while (currentPage <= 100) {
                if (shouldStop) break
                val rows = doc.select("table#grdColleges tr")

                for ((i, row) in rows.withIndex()) {
                    if (i == 0) continue
                    val rowClass = row.className()
                    if (rowClass != "GridViewRowStyle" && rowClass != "GridViewAlternatingRowStyle") continue
                    val cols = row.select("td")
                    if (cols.size < 3) continue

                    val course = cols[0].text().trim()
                    val subject = cols[1].text().trim()
                    if (course.isEmpty()) continue

                    val link = cols[2].select("a").first()
                    val href = link?.attr("href") ?: ""
                    val onclick = link?.attr("onclick") ?: ""

                    var eventTarget = extractEventTarget(href)
                    if (eventTarget.isEmpty()) {
                        eventTarget = extractEventTarget(onclick)
                    }

                    if (eventTarget.isNotEmpty()) {
                        val key = "$course|$subject|$eventTarget"
                        if (seenKeys.add(key)) {
                            val revalCourse = RevalCourse(course, subject, eventTarget)
                            if (stopPredicate?.invoke(revalCourse) == true) {
                                shouldStop = true
                                break
                            }
                            allCourses.add(revalCourse)
                        }
                    }
                }

                if (shouldStop) break

                // Numeric pagination: find the lowest page number > currentPage
                val pagerLinks = doc.select("tr.GridViewPagerStyle a")
                var nextPage = Int.MAX_VALUE
                var nextTarget = ""
                var nextArg = ""
                for (link in pagerLinks) {
                    val h = link.attr("href")
                    val arg = extractEventArgument(h)
                    val pageMatch = Regex("Page\\$(\\d+)").find(arg)
                    if (pageMatch != null) {
                        val pageNum = pageMatch.groupValues[1].toIntOrNull()
                        if (pageNum != null && pageNum > currentPage && pageNum < nextPage) {
                            nextPage = pageNum
                            nextTarget = extractEventTarget(h)
                            nextArg = arg
                        }
                    }
                }

                if (nextPage == Int.MAX_VALUE) break

                val formData = mutableMapOf<String, String>()
                formData["__EVENTTARGET"] = nextTarget
                formData["__EVENTARGUMENT"] = nextArg
                for (inp in doc.select("input[type=hidden]")) {
                    val name = inp.attr("name")
                    val value = inp.attr("value")
                    if (name.isNotEmpty() && name != "__EVENTTARGET" && name != "__EVENTARGUMENT") {
                        formData[name] = value
                    }
                }

                html = fetch(REVAL_URL, cookies, formData)
                if (html.isEmpty()) break
                doc = Jsoup.parse(html)
                currentPage = nextPage
            }
        } catch (e: Exception) {
            Log.e(TAG, "scrapeCourses failed: ${e.message}", e)
        }
        allCourses
    }

    /**
     * Searches for a specific revaluation result by seat number or PRN
     */
    suspend fun searchRevaluation(
        eventTarget: String,
        searchBy: String, // "Seat No" or "PRN No"
        searchValue: String
    ): String = withContext(Dispatchers.IO) {
        val cookies = mutableMapOf<String, String>()
        try {
            // 1. Initial GET to get cookies and hidden fields
            var html = fetch(REVAL_URL, cookies)
            if (html.isEmpty()) return@withContext ""
            var doc = Jsoup.parse(html)

            // 2. Select the course (trigger __doPostBack for the course)
            var formData = mutableMapOf<String, String>()
            formData["__EVENTTARGET"] = eventTarget
            formData["__EVENTARGUMENT"] = ""
            for (inp in doc.select("input[type=hidden]")) {
                formData[inp.attr("name")] = inp.attr("value")
            }

            html = fetch(REVAL_URL, cookies, formData)
            if (html.isEmpty()) return@withContext ""
            doc = Jsoup.parse(html)

            // 3. Fill search type and value
            // Extract the action URL if it's different
            val form = doc.select("form").first()
            val action = form?.attr("action") ?: ""
            val targetUrl = if (action.isEmpty() || action == "." || action == "./") {
                REVAL_URL
            } else if (action.startsWith("http")) {
                action
            } else if (action.startsWith("/")) {
                "https://pun.unipune.ac.in$action"
            } else {
                "https://pun.unipune.ac.in/revalresult/$action"
            }

            val examVal = doc.select("#cboExamName option[selected]").attr("value").ifEmpty {
                doc.select("#cboExamName option").first()?.attr("value") ?: ""
            }

            formData = mutableMapOf()
            formData["__EVENTTARGET"] = ""
            formData["__EVENTARGUMENT"] = ""
            formData["cboExamName"] = examVal
            formData["cboSearchBy"] = searchBy
            formData["txtSearch"] = searchValue
            formData["btnShow"] = "Submit"

            for (inp in doc.select("input[type=hidden]")) {
                formData[inp.attr("name")] = inp.attr("value")
            }

            html = fetch(targetUrl, cookies, formData)
            if (html.isEmpty()) return@withContext ""
            doc = Jsoup.parse(html)

            // 4. Check if we got a list with a "Result" link (common for some courses)
            val resultLink = doc.select("a[href*='__doPostBack']").find { it.text().contains("Result", ignoreCase = true) }
            if (resultLink != null) {
                val href = resultLink.attr("href")
                val linkTarget = extractEventTarget(href)
                
                val nextFormData = mutableMapOf<String, String>()
                nextFormData["__EVENTTARGET"] = linkTarget
                nextFormData["__EVENTARGUMENT"] = ""
                for (inp in doc.select("input[type=hidden]")) {
                    nextFormData[inp.attr("name")] = inp.attr("value")
                }
                
                html = fetch(targetUrl, cookies, nextFormData)
                if (html.isEmpty()) return@withContext ""
                doc = Jsoup.parse(html)
            }

            // 5. Extract and clean the result
            return@withContext extractAndCleanResult(doc.html())
        } catch (e: Exception) {
            Log.e(TAG, "searchRevaluation failed: ${e.message}")
            ""
        }
    }

    private fun extractAndCleanResult(html: String): String {
        val doc = Jsoup.parse(html)
        
        // Remove scripts, styles, etc.
        doc.select("script, style, link, input[type=hidden], img").remove()
        
        // Find tables that look like results
        val tables = doc.select("table")
        var bestTable: org.jsoup.nodes.Element? = null
        
        for (table in tables) {
            val text = table.text()
            if (text.contains("SubCode", ignoreCase = true) || text.contains("SubName", ignoreCase = true) || 
                text.contains("Obt", ignoreCase = true) || text.contains("Marks", ignoreCase = true)) {
                if (bestTable == null || table.text().length > bestTable.text().length) {
                    bestTable = table
                }

            }
        }
        
        if (bestTable != null) {
            val studentInfo = StringBuilder()
            val fullText = doc.text()
            
            val seatMatch = Regex("Seat\\s*No\\s*:\\s*(\\S+)", RegexOption.IGNORE_CASE).find(fullText)
            seatMatch?.let { studentInfo.append("Seat No: ${it.groupValues[1]}<br>") }
            
            val nameMatch = Regex("Name\\s*:\\s*([A-Z][a-zA-Z\\s]+)").find(fullText)
            nameMatch?.let { studentInfo.append("Name: ${it.groupValues[1].trim()}<br>") }
            
            val prnMatch = Regex("PRN\\s*:\\s*(\\S+)", RegexOption.IGNORE_CASE).find(fullText)
            prnMatch?.let { studentInfo.append("PRN: ${it.groupValues[1]}") }
            
            return """
                <div class="rv-student-info">
                    ${if (studentInfo.isNotEmpty()) studentInfo.toString() else ""}
                </div>
                ${bestTable.outerHtml()}
            """.trimIndent()
        }
        
        // Fallback: just return the body content without scripts/styles
        return doc.body().html()
    }



    private fun extractEventTarget(href: String): String {
        if (href.isBlank()) return ""
        val match = Regex("__doPostBack\\(['\"]([^'\"]+)['\"]").find(href)
        return match?.groupValues?.getOrNull(1) ?: ""
    }

    private fun extractEventArgument(href: String): String {
        if (href.isBlank()) return ""
        val match = Regex("__doPostBack\\(['\"][^'\"]+['\"],\\s*['\"]([^'\"]+)['\"]").find(href)
        return match?.groupValues?.getOrNull(1) ?: ""
    }

    suspend fun checkServerHealth(): ServerStatus = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        var connection: HttpURLConnection? = null
        try {
            connection = (URL(REVAL_URL).openConnection() as HttpURLConnection).apply {
                if (this is HttpsURLConnection) {
                    sslSocketFactory = trustAllContext.socketFactory
                    hostnameVerifier = HostnameVerifier { _, _ -> true }
                }
                requestMethod = "GET"
                connectTimeout = 8000
                readTimeout = 8000
                instanceFollowRedirects = true
                setRequestProperty("User-Agent", userAgent)
                setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                setRequestProperty("Connection", "close")
            }
            val responseCode = connection.responseCode
            val duration = System.currentTimeMillis() - startTime
            ServerStatus(isOnline = true, statusCode = responseCode, responseTimeMs = duration)
        } catch (e: Exception) {
            Log.w(TAG, "Reval health check failed: ${e.message}")
            ServerStatus(isOnline = false, responseTimeMs = -1)
        } finally {
            connection?.disconnect()
        }
    }
}
