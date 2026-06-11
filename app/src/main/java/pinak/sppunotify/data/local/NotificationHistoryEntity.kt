package pinak.sppunotify.data.local

import androidx.compose.runtime.Immutable
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notification_history")
@Immutable
data class NotificationHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val message: String,
    val type: String, // "RESULT", "REVAL", "EXAM_DATE", "NEWS"
    val timestamp: Long = System.currentTimeMillis()
)
