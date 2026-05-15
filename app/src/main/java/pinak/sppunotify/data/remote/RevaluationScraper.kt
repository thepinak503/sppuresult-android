package pinak.sppunotify.data.remote

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
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RevaluationScraper @Inject constructor() {

    companion object {
        private const val REVAL_URL = "https://pun.unipune.ac.in/revalresult/"
        private const val TAG = "RevaluationScraper"
    }

    private val userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36"

    private fun fetch(url: String, cookies: MutableMap<String, String>, formData: Map<String, String>? = null): String {
        val conn = URL(url).openConnection() as HttpURLConnection
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

    suspend fun scrapeCourses(): List<RevalCourse> = withContext(Dispatchers.IO) {
        val allCourses = mutableListOf<RevalCourse>()
        val cookies = mutableMapOf<String, String>()

        try {
            var html = fetch(REVAL_URL, cookies)
            if (html.isEmpty()) return@withContext allCourses
            var doc = Jsoup.parse(html)

            val seenKeys = mutableSetOf<String>()
            var currentPage = 1

            while (currentPage <= 100) {
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
                            allCourses.add(RevalCourse(course, subject, eventTarget))
                        }
                    }
                }

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
}
