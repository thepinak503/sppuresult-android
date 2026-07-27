package pinak.sppunotify.data.local

import androidx.compose.runtime.Immutable
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "removed_results")
@Immutable
data class RemovedResultEntity(
    @PrimaryKey val id: String,
    val title: String,
    val department: String,
    val removedAt: Long = System.currentTimeMillis()
)
