package pinak.sppunotify.data.repository

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.jsoup.Jsoup
import pinak.sppunotify.data.local.PreferenceManager
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RemoteConfigRepository @Inject constructor(
    private val preferenceManager: PreferenceManager
) {
    companion object {
        private const val REPO_URL = "https://raw.githubusercontent.com/thepinak503/sppuresult-android/main"
        private const val CONFIG_URL = "$REPO_URL/config.json"
    }

    data class Announcement(
        val id: String,
        val title: String,
        val message: String,
        val type: String // FEATURE, ANNOUNCEMENT, ALERT
    )

    data class AppConfig(
        val latestVersionCode: Int,
        val latestVersionName: String,
        val updateUrl: String,
        val announcement: Announcement?
    )

    suspend fun fetchAppConfig(): AppConfig? = withContext(Dispatchers.IO) {
        try {
            val response = Jsoup.connect(CONFIG_URL)
                .ignoreContentType(true)
                .timeout(5000)
                .execute()
            
            val json = JSONObject(response.body())
            
            val announcementJson = json.optJSONObject("latest_announcement")
            val announcement = announcementJson?.let {
                Announcement(
                    id = it.optString("id"),
                    title = it.optString("title"),
                    message = it.optString("message"),
                    type = it.optString("type", "ANNOUNCEMENT")
                )
            }
            
            AppConfig(
                latestVersionCode = json.optInt("latest_version_code", 1),
                latestVersionName = json.optString("latest_version_name", "1.0"),
                updateUrl = json.optString("update_url", ""),
                announcement = announcement
            )
        } catch (e: Exception) {
            Log.e("RemoteConfig", "Failed to fetch remote config: ${e.message}")
            null
        }
    }
}
