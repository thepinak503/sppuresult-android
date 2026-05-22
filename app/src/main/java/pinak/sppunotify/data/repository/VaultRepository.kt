package pinak.sppunotify.data.repository

import pinak.sppunotify.data.local.DownloadedResultDao
import pinak.sppunotify.data.local.DownloadedResultEntity
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

@Singleton
class VaultRepository @Inject constructor(
    private val dao: DownloadedResultDao
) {
    val downloadedResults: Flow<List<DownloadedResultEntity>> = dao.getAllDownloadedResults()

    suspend fun saveDownloadedResult(result: DownloadedResultEntity) {
        dao.insertDownloadedResult(result)
    }

    suspend fun deleteResult(result: DownloadedResultEntity) {
        dao.deleteDownloadedResult(result)
    }
}
