package pinak.sppunotify.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [ResultEntity::class], version = 3, exportSchema = false)
abstract class ResultDatabase : RoomDatabase() {
    abstract val dao: ResultDao

    companion object {
        val MIGRATION_1_2 = Migration(1, 2) { db ->
            db.execSQL("ALTER TABLE results ADD COLUMN patternName TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE results ADD COLUMN patternId TEXT NOT NULL DEFAULT ''")
        }
        
        val MIGRATION_2_3 = Migration(2, 3) { db ->
            db.execSQL("ALTER TABLE results ADD COLUMN department TEXT NOT NULL DEFAULT 'Other UG'")
        }
    }
}
