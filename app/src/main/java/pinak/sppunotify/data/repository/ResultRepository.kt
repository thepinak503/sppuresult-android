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

@Singleton
class ResultRepository @Inject constructor(
    private val scraper: ResultScraper,
    private val db: ResultDatabase,
    private val preferenceManager: pinak.sppunotify.data.local.PreferenceManager
) {
    // Expose local database flow for offline-first support
    val results: Flow<List<ResultEntity>> = db.dao.getAllResults()

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

    suspend fun fetchResults(): List<ResultDto> = withContext(Dispatchers.IO) {
        // Auto-cleanup: remove results older than 6 months
        val sixMonthsAgo = System.currentTimeMillis() - (180L * 24 * 60 * 60 * 1000)
        db.dao.deleteOldResults(sixMonthsAgo)

        val scrapedResults = scraper.scrapeLatestResults()
        
        // Calculate newly found results for notification purposes BEFORE inserting
        val existingIds = db.dao.getAllResultIds().toSet()
        val newResults = scrapedResults.filter { it.id !in existingIds }
        
        val entities = scrapedResults.map { it.toEntity() }
        db.dao.insertResults(entities)
        
        newResults
    }

    suspend fun getCachedCount(): Int = withContext(Dispatchers.IO) {
        db.dao.getCount()
    }

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
