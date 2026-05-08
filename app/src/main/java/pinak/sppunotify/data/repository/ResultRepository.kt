package pinak.sppunotify.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import pinak.sppunotify.data.local.ResultDatabase
import pinak.sppunotify.data.local.ResultEntity
import pinak.sppunotify.data.remote.ResultDto
import pinak.sppunotify.data.remote.ResultScraper
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ResultRepository @Inject constructor(
    private val scraper: ResultScraper,
    private val db: ResultDatabase,
) {
    // Expose local database flow for offline-first support
    val results: Flow<List<ResultEntity>> = db.dao.getAllResults()

    /**
     * Scrapes results from the SPPU website and saves them to the local database.
     */
    suspend fun fetchResults(): List<ResultDto> = withContext(Dispatchers.IO) {
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
        fetchedAt = System.currentTimeMillis(),
    )
}
