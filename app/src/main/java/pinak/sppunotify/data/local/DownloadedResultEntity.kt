package pinak.sppunotify.data.local

import androidx.compose.runtime.Immutable
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "downloaded_results")
@Immutable
data class DownloadedResultEntity(
    @PrimaryKey(autoGenerate = true) val uid: Long = 0,
    val resultId: String,
    val title: String,
    val profileName: String,
    val fileName: String,
    val filePath: String,
    val mimeType: String,
    val downloadDate: Long = System.currentTimeMillis()
)
