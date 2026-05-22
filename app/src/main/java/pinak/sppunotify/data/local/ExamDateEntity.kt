package pinak.sppunotify.data.local

import androidx.compose.runtime.Immutable
import androidx.room.Entity
import androidx.room.PrimaryKey

@Immutable
@Entity(tableName = "exam_dates")
data class ExamDateEntity(
    @PrimaryKey val courseName: String,
    val status: String,
    val startDate: String,
    val endDateWithoutLateFee: String,
    val endDateWithLateFee: String,
    val fetchedAt: Long = System.currentTimeMillis()
)
