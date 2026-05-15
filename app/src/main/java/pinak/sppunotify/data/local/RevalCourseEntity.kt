package pinak.sppunotify.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reval_courses")
data class RevalCourseEntity(
    @PrimaryKey val eventTarget: String,
    val course: String,
    val subject: String,
    val firstSeenAt: Long = System.currentTimeMillis(),
)
