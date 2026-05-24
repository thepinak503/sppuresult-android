package pinak.sppunotify.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificationHistoryDao {
    @Query("SELECT * FROM notification_history ORDER BY timestamp DESC")
    fun getAll(): Flow<List<NotificationHistoryEntity>>

    @Insert
    suspend fun insert(entry: NotificationHistoryEntity)

    @Query("DELETE FROM notification_history WHERE timestamp < :before")
    suspend fun deleteOld(before: Long)

    @Query("DELETE FROM notification_history")
    suspend fun clearAll()
}
