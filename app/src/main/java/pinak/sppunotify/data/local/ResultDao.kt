package pinak.sppunotify.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ResultDao {
    @Query("""
        SELECT * FROM results 
        ORDER BY 
          CASE WHEN publishedTimestamp > 0 THEN publishedTimestamp ELSE 0 END DESC,
          substr(publishedDate, 7, 4) DESC,
          CASE substr(publishedDate, 4, 3)
            WHEN 'Jan' THEN 1 WHEN 'Feb' THEN 2 WHEN 'Mar' THEN 3 WHEN 'Apr' THEN 4
            WHEN 'May' THEN 5 WHEN 'Jun' THEN 6 WHEN 'Jul' THEN 7 WHEN 'Aug' THEN 8
            WHEN 'Sep' THEN 9 WHEN 'Oct' THEN 10 WHEN 'Nov' THEN 11 WHEN 'Dec' THEN 12
            WHEN '- M' THEN 5 WHEN '- A' THEN 4
          END DESC,
          substr(publishedDate, 1, 2) DESC
    """)
    fun getAllResults(): Flow<List<ResultEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertResults(results: List<ResultEntity>)

    @Query("SELECT id FROM results")
    suspend fun getAllResultIds(): List<String>

    @Query("SELECT COUNT(*) FROM results")
    suspend fun getCount(): Int

    @Query("DELETE FROM results")
    suspend fun clearAll()
}
