package pinak.sppunotify.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [ResultEntity::class, RevalCourseEntity::class], version = 4, exportSchema = false)
abstract class ResultDatabase : RoomDatabase() {
    abstract val dao: ResultDao
    abstract val revalDao: RevalCourseDao

    companion object {
        val MIGRATION_1_2 = Migration(1, 2) { db ->
            db.execSQL("ALTER TABLE results ADD COLUMN patternName TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE results ADD COLUMN patternId TEXT NOT NULL DEFAULT ''")
        }
        
        val MIGRATION_2_3 = Migration(2, 3) { db ->
            db.execSQL("ALTER TABLE results ADD COLUMN department TEXT NOT NULL DEFAULT 'Other UG'")
        }

        val MIGRATION_3_4 = Migration(3, 4) { db ->
            db.execSQL("CREATE TABLE IF NOT EXISTS reval_courses (eventTarget TEXT NOT NULL PRIMARY KEY, course TEXT NOT NULL, subject TEXT NOT NULL, firstSeenAt INTEGER NOT NULL DEFAULT 0)")
        }
    }
}
