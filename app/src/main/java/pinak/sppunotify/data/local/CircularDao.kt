package pinak.sppunotify.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CircularDao {
    @Query("SELECT * FROM circulars ORDER BY cachedAt DESC")
    fun getAllCirculars(): Flow<List<CircularEntity>>

    @Query("SELECT * FROM circulars WHERE title LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%' ORDER BY cachedAt DESC")
    fun searchCirculars(query: String): Flow<List<CircularEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCirculars(circulars: List<CircularEntity>)

    @Query("DELETE FROM circulars")
    suspend fun clearAll()
}
