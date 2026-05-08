package pinak.sppunotify.data.local

import androidx.compose.runtime.Immutable
import androidx.room.Entity
import androidx.room.PrimaryKey

@Immutable
@Entity(tableName = "results")
data class ResultEntity(
    @PrimaryKey val id: String,
    val title: String,
    val url: String,
    val publishedDate: String,
    val publishedTimestamp: Long = 0L,
    val patternName: String = "",
    val patternId: String = "",
    val department: String = "Other UG",
    val fetchedAt: Long = System.currentTimeMillis()
)
