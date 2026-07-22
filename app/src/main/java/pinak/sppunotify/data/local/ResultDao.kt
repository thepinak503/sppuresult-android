package pinak.sppunotify.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ResultDao {
    @Query("SELECT * FROM results ORDER BY isBookmarked DESC, publishedTimestamp DESC, fetchedAt DESC")
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

    @Query("UPDATE results SET isViewed = 1 WHERE id = :resultId")
    suspend fun markAsViewed(resultId: String)

    @Query("UPDATE results SET isBookmarked = CASE WHEN isBookmarked = 1 THEN 0 ELSE 1 END WHERE id = :resultId")
    suspend fun toggleBookmark(resultId: String)

    @Query("SELECT * FROM results WHERE isBookmarked = 1 ORDER BY publishedTimestamp DESC")
    fun getBookmarkedResults(): Flow<List<ResultEntity>>

    @Query("SELECT * FROM results WHERE isBookmarked = 1 ORDER BY publishedTimestamp DESC")
    suspend fun getAllBookmarks(): List<ResultEntity>

    @Query("SELECT isBookmarked FROM results WHERE id = :resultId")
    suspend fun isBookmarked(resultId: String): Boolean?

    @Query("SELECT id FROM results WHERE isBookmarked = 1")
    suspend fun getBookmarkedIds(): List<String>

    @Query("UPDATE results SET isBookmarked = 1 WHERE id IN (:ids)")
    suspend fun restoreBookmarks(ids: List<String>)

    @Query("UPDATE results SET isViewed = 1 WHERE id IN (:ids)")
    suspend fun restoreViewed(ids: List<String>)
}
