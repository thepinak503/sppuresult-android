package pinak.sppunotify.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface RemovedResultDao {
    @Query("SELECT * FROM removed_results ORDER BY removedAt DESC")
    fun getAllRemovedResults(): Flow<List<RemovedResultEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRemovedResults(results: List<RemovedResultEntity>)

    @Query("DELETE FROM removed_results WHERE id = :resultId")
    suspend fun deleteById(resultId: String)

    @Query("DELETE FROM removed_results")
    suspend fun clearAll()
}
