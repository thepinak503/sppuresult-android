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
    private val removedDao: pinak.sppunotify.data.local.RemovedResultDao,
    private val preferenceManager: pinak.sppunotify.data.local.PreferenceManager
) {
    // Expose local database flow for offline-first support
    val results: Flow<List<ResultEntity>> = db.dao.getAllResults()
    val removedResults: Flow<List<pinak.sppunotify.data.local.RemovedResultEntity>> = removedDao.getAllRemovedResults()

    data class SyncResult(
        val newResults: List<ResultDto> = emptyList(),
        val removedResults: List<ResultEntity> = emptyList()
    )

    /**
     * Scrapes results from the SPPU website and saves them to the local database.
     */
    private val _serverStatus = MutableStateFlow<ServerStatus?>(null)
    val serverStatus = _serverStatus.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing = _isSyncing.asStateFlow()

    suspend fun updateServerStatus() {
        val status = scraper.checkServerHealth()
        _serverStatus.value = status
        
        // Use standard preference flow to update history
        preferenceManager.updateWasServerDown(!status.isOnline)
    }

    suspend fun hardRefresh(): SyncResult = withContext(Dispatchers.IO) {
        _isSyncing.value = true
        try {
            // Save bookmarked & viewed IDs before clearing
            val bookmarkedIds = db.dao.getBookmarkedIds()
            val viewedIds = db.dao.getAllResults().first()
                .filter { it.isViewed }
                .map { it.id }
            
            db.dao.clearAll()
            
            val syncResult = fetchResultsInternal()
            
            // Restore bookmarks & viewed status on matching new results
            if (bookmarkedIds.isNotEmpty()) {
                db.dao.restoreBookmarks(bookmarkedIds)
            }
            if (viewedIds.isNotEmpty()) {
                db.dao.restoreViewed(viewedIds)
            }
            
            syncResult
        } finally {
            _isSyncing.value = false
        }
    }

    suspend fun fetchResults(): SyncResult = withContext(Dispatchers.IO) {
        _isSyncing.value = true
        try {
            fetchResultsInternal()
        } finally {
            _isSyncing.value = false
        }
    }

    private suspend fun fetchResultsInternal(): SyncResult {
        // Auto-cleanup: remove results older than 6 months
        val sixMonthsAgo = System.currentTimeMillis() - (180L * 24 * 60 * 60 * 1000)
        db.dao.deleteOldResults(sixMonthsAgo)

        val scrapePeriodResults = scraper.scrapeLatestResults()
        
        // 1. Flatten all results for insertion
        val allScrapedDto = scrapePeriodResults.flatMap { it.results }.distinctBy { it.id }
        
        // 2. Identify new results for notification
        val existingIds = db.dao.getAllResultIds().toSet()
        val newResults = allScrapedDto.filter { it.id !in existingIds }.map {
            it.copy(department = DepartmentClassifier.classify(it.title))
        }

        // 3. Robust Removal Detection
        // Only consider periods that were successfully scraped and are NOT period 0 (summary)
        val successfulPeriods = scrapePeriodResults
            .filter { it.isSuccess && it.period != 0 }
            .map { it.period }
            .toSet()

        val removedResults = if (successfulPeriods.isNotEmpty()) {
            val scrapedIdsInSuccessfulPeriods = scrapePeriodResults
                .filter { it.period in successfulPeriods }
                .flatMap { it.results }
                .map { it.id }
                .toSet()

            // Fetch current DB results belonging to successful periods
            val resultsInDbForSuccessfulPeriods = db.dao.getAllResults().first()
                .filter { it.examPeriod in successfulPeriods }

            // A result is removed if its period was verified but it's missing from scrape
            resultsInDbForSuccessfulPeriods.filter { it.id !in scrapedIdsInSuccessfulPeriods }
        } else emptyList()
        
        if (removedResults.isNotEmpty()) {
            val removedEntities = removedResults.map {
                pinak.sppunotify.data.local.RemovedResultEntity(
                    id = it.id,
                    title = it.title,
                    department = it.department
                )
            }
            removedDao.insertRemovedResults(removedEntities)
        }

        // 4. Update Database
        val entities = allScrapedDto.map { it.toEntity() }
        db.dao.insertResults(entities)
        
        return SyncResult(newResults, removedResults)
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
        examPeriod = examPeriod,
        fetchedAt = System.currentTimeMillis(),
    )
}
