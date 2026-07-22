package pinak.sppunotify.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import pinak.sppunotify.data.local.ResultDatabase
import pinak.sppunotify.data.local.ResultEntity
import pinak.sppunotify.data.remote.ResultDto
import pinak.sppunotify.data.remote.ResultScraper
import pinak.sppunotify.util.DepartmentClassifier
import pinak.sppunotify.data.remote.ServerStatus
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first

@Singleton
class ResultRepository @Inject constructor(
    private val scraper: ResultScraper,
    private val db: ResultDatabase,
    private val preferenceManager: pinak.sppunotify.data.local.PreferenceManager
) {
    // Expose local database flow for offline-first support
    val results: Flow<List<ResultEntity>> = db.dao.getAllResults()

    data class SyncResult(
        val newResults: List<ResultDto> = emptyList(),
        val removedResults: List<ResultEntity> = emptyList()
    )

    /**
     * Scrapes results from the SPPU website and saves them to the local database.
     */
    private val _serverStatus = MutableStateFlow<ServerStatus?>(null)
    val serverStatus = _serverStatus.asStateFlow()

    suspend fun updateServerStatus() {
        val status = scraper.checkServerHealth()
        _serverStatus.value = status
        
        // Use standard preference flow to update history
        preferenceManager.updateWasServerDown(!status.isOnline)
    }

    suspend fun hardRefresh(): SyncResult = withContext(Dispatchers.IO) {
        // Save bookmarked & viewed IDs before clearing
        val bookmarkedIds = db.dao.getBookmarkedIds()
        val viewedIds = db.dao.getAllResults().first()
            .filter { it.isViewed }
            .map { it.id }
        
        db.dao.clearAll()
        
        val syncResult = fetchResults()
        
        // Restore bookmarks & viewed status on matching new results
        if (bookmarkedIds.isNotEmpty()) {
            db.dao.restoreBookmarks(bookmarkedIds)
        }
        if (viewedIds.isNotEmpty()) {
            db.dao.restoreViewed(viewedIds)
        }
        
        syncResult
    }

    suspend fun fetchResults(): SyncResult = withContext(Dispatchers.IO) {
        // Auto-cleanup: remove results older than 6 months
        val sixMonthsAgo = System.currentTimeMillis() - (180L * 24 * 60 * 60 * 1000)
        db.dao.deleteOldResults(sixMonthsAgo)

        val scrapedResults = scraper.scrapeLatestResults()
        
        // Calculate newly found results for notification purposes BEFORE inserting
        val existingIds = db.dao.getAllResultIds().toSet()
        val newResults = scrapedResults.filter { it.id !in existingIds }.map {
            it.copy(department = DepartmentClassifier.classify(it.title))
        }

        // Detect removed results: in DB but not in scraped list
        val scrapedIds = scrapedResults.map { it.id }.toSet()
        val removedResults = if (scrapedResults.isNotEmpty()) {
             // Only detect removals if we actually got a successful scrape
             // This avoids false "removed" alerts if the scrape partially failed
             val allResults = db.dao.getAllResults().first()
             allResults.filter { it.id !in scrapedIds }
        } else emptyList()
        
        val entities = scrapedResults.map { it.toEntity() }
        db.dao.insertResults(entities)
        
        SyncResult(newResults, removedResults)
    }

    suspend fun getCachedCount(): Int = withContext(Dispatchers.IO) {
        db.dao.getCount()
    }

    suspend fun markAsViewed(resultId: String) = withContext(Dispatchers.IO) {
        db.dao.markAsViewed(resultId)
    }

    suspend fun toggleBookmark(resultId: String) = withContext(Dispatchers.IO) {
        db.dao.toggleBookmark(resultId)
    }

    suspend fun isBookmarked(resultId: String): Boolean = withContext(Dispatchers.IO) {
        db.dao.isBookmarked(resultId) ?: false
    }

    val bookmarkedResults: Flow<List<ResultEntity>> = db.dao.getBookmarkedResults()

    private fun ResultDto.toEntity() = ResultEntity(
        id = id,
        title = title,
        url = url,
        publishedDate = published,
        publishedTimestamp = ResultScraper.parseDateToTimestamp(published),
        patternName = patternName,
        patternId = patternId,
        department = DepartmentClassifier.classify(title),
        fetchedAt = System.currentTimeMillis(),
    )
}
