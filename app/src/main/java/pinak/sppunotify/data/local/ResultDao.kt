package pinak.sppunotify.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ResultDao {
    @Query("SELECT * FROM results ORDER BY publishedTimestamp DESC, fetchedAt DESC")
    fun getAllResults(): Flow<List<ResultEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertResults(results: List<ResultEntity>)

    @Query("SELECT id FROM results")
    suspend fun getAllResultIds(): List<String>

    @Query("SELECT COUNT(*) FROM results")
    suspend fun getCount(): Int

    @Query("DELETE FROM results")
    suspend fun clearAll()

    @Query("DELETE FROM results WHERE fetchedAt < :timestamp")
    suspend fun deleteOldResults(timestamp: Long)
}
