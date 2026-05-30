package pinak.sppunotify.util

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import pinak.sppunotify.data.local.PreferenceManager
import pinak.sppunotify.data.local.ResultDao
import pinak.sppunotify.data.local.ResultEntity
import pinak.sppunotify.data.local.UserProfile
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class AppBackup(
    val profiles: List<UserProfile>,
    val watchlistKeywords: Set<String>,
    val priorityKeywords: Set<String>,
    val bookmarks: List<ResultEntity>,
    val timestamp: Long = System.currentTimeMillis(),
    val appVersion: String = "1.1.0"
)

@Singleton
class BackupManager @Inject constructor(
    private val preferenceManager: PreferenceManager,
    private val resultDao: ResultDao
) {
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }

    suspend fun createBackup(): String = withContext(Dispatchers.IO) {
        val prefs = preferenceManager.preferencesFlow.first()
        val bookmarks = resultDao.getAllBookmarks()
        
        val backup = AppBackup(
            profiles = prefs.profiles,
            watchlistKeywords = prefs.watchlistKeywords,
            priorityKeywords = prefs.priorityKeywords,
            bookmarks = bookmarks
        )
        
        json.encodeToString(backup)
    }

    suspend fun restoreBackup(jsonContent: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val backup = json.decodeFromString<AppBackup>(jsonContent)
            
            // Restore Preferences and Profiles via bulk update
            preferenceManager.restoreFromBackup(backup)
            
            // Restore Bookmarks
            if (backup.bookmarks.isNotEmpty()) {
                val marked = backup.bookmarks.map { it.copy(isBookmarked = true) }
                resultDao.insertResults(marked)
            }
            
            true
        } catch (e: Exception) {
            false
        }
    }
}
