package pinak.sppunotify.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadedResultDao {
    @Query("SELECT * FROM downloaded_results ORDER BY downloadDate DESC")
    fun getAllDownloadedResults(): Flow<List<DownloadedResultEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDownloadedResult(result: DownloadedResultEntity)

    @Delete
    suspend fun deleteDownloadedResult(result: DownloadedResultEntity)

    @Query("SELECT * FROM downloaded_results WHERE resultId = :resultId")
    suspend fun getDownloadedResultByResultId(resultId: String): DownloadedResultEntity?

    @Query("SELECT resultId FROM downloaded_results")
    suspend fun getAllDownloadedResultIds(): List<String>
}
