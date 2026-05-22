package pinak.sppunotify.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ExamDateDao {
    @Query("SELECT * FROM exam_dates ORDER BY courseName ASC")
    fun getAllExamDates(): Flow<List<ExamDateEntity>>

    @Query("SELECT courseName FROM exam_dates")
    suspend fun getAllCourseNames(): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExamDates(examDates: List<ExamDateEntity>)

    @Query("DELETE FROM exam_dates")
    suspend fun clearAll()
}
