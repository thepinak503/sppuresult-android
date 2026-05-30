package pinak.sppunotify.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sync_logs")
data class SyncLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val type: String, // RESULTS, REVAL, EXAM_DATES, CIRCULAR
    val status: String, // SUCCESS, FAILED
    val message: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
