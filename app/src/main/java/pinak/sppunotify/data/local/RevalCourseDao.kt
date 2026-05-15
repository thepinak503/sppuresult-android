package pinak.sppunotify.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface RevalCourseDao {
    @Query("SELECT eventTarget FROM reval_courses")
    suspend fun getAllEventTargets(): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCourses(courses: List<RevalCourseEntity>)

    @Query("DELETE FROM reval_courses")
    suspend fun clearAll()
}
