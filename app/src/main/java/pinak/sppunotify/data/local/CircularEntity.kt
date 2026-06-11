package pinak.sppunotify.data.local

import androidx.compose.runtime.Immutable
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "circulars")
@Immutable
data class CircularEntity(
    @PrimaryKey val link: String,
    val title: String,
    val description: String,
    val pubDate: String,
    val feedSource: String = "",
    val cachedAt: Long = System.currentTimeMillis()
)
