package pinak.sppunotify.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.inject.Inject
import javax.inject.Singleton
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import android.util.Log
import java.net.HttpURLConnection
import java.net.URL
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.HostnameVerifier

@Singleton
class ExamDateScraper @Inject constructor() {

    private val url = "https://examform.unipune.ac.in/Support/StuExDates.aspx"
    private val userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36"

    private val trustAllContext: SSLContext by lazy {
        val sslContext = SSLContext.getInstance("TLS")
        sslContext.init(null, arrayOf<TrustManager>(object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
            override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
            override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
        }), SecureRandom())
        sslContext
    }

    suspend fun scrapeExamDates(): List<ExamDateDto> = withContext(Dispatchers.IO) {
        val examDates = mutableListOf<ExamDateDto>()
        try {
            val doc = Jsoup.connect(url)
                .userAgent(userAgent)
                .timeout(30000)
                .sslSocketFactory(trustAllContext.socketFactory)
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

    suspend fun checkServerHealth(): ServerStatus = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        var connection: HttpURLConnection? = null
        try {
            connection = (URL(url).openConnection() as HttpURLConnection).apply {
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
            Log.w("ExamDateScraper", "Exam dates health check failed: ${e.message}")
            ServerStatus(isOnline = false, responseTimeMs = -1)
        } finally {
            connection?.disconnect()
        }
    }
}
