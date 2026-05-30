package pinak.sppunotify.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SyncLogDao {
    @Insert
    suspend fun insertLog(log: SyncLogEntity)

    @Query("SELECT * FROM sync_logs ORDER BY timestamp DESC LIMIT 20")
    fun getRecentLogs(): Flow<List<SyncLogEntity>>

    @Query("SELECT COUNT(*) FROM sync_logs WHERE status = 'SUCCESS' AND timestamp > :since")
    suspend fun getSuccessCount(since: Long): Int

    @Query("SELECT COUNT(*) FROM sync_logs WHERE status = 'FAILED' AND timestamp > :since")
    suspend fun getFailedCount(since: Long): Int

    @Query("DELETE FROM sync_logs WHERE timestamp < :timestamp")
    suspend fun deleteOldLogs(timestamp: Long)
}
