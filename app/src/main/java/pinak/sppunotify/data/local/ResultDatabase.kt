package pinak.sppunotify.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration

@Database(entities = [ResultEntity::class, RevalCourseEntity::class, DownloadedResultEntity::class, CircularEntity::class, ExamDateEntity::class, NotificationHistoryEntity::class], version = 10, exportSchema = true)
abstract class ResultDatabase : RoomDatabase() {
    abstract val dao: ResultDao
    abstract val revalDao: RevalCourseDao
    abstract val downloadedDao: DownloadedResultDao
    abstract val circularDao: CircularDao
    abstract val examDateDao: ExamDateDao
    abstract val notificationHistoryDao: NotificationHistoryDao

    companion object {
        val MIGRATION_7_8 = Migration(7, 8) { db ->
            db.execSQL("CREATE TABLE IF NOT EXISTS exam_dates (courseName TEXT NOT NULL PRIMARY KEY, status TEXT NOT NULL, startDate TEXT NOT NULL, endDateWithoutLateFee TEXT NOT NULL, endDateWithLateFee TEXT NOT NULL, paymentLinkEndDate TEXT NOT NULL, fetchedAt INTEGER NOT NULL DEFAULT 0)")
        }

        val MIGRATION_8_9 = Migration(8, 9) { db ->
            db.execSQL("CREATE TABLE exam_dates_new (courseName TEXT NOT NULL PRIMARY KEY, status TEXT NOT NULL, startDate TEXT NOT NULL, endDateWithoutLateFee TEXT NOT NULL, endDateWithLateFee TEXT NOT NULL, fetchedAt INTEGER NOT NULL DEFAULT 0)")
            db.execSQL("INSERT INTO exam_dates_new (courseName, status, startDate, endDateWithoutLateFee, endDateWithLateFee, fetchedAt) SELECT courseName, status, startDate, endDateWithoutLateFee, endDateWithLateFee, fetchedAt FROM exam_dates")
            db.execSQL("DROP TABLE exam_dates")
            db.execSQL("ALTER TABLE exam_dates_new RENAME TO exam_dates")
        }
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

        val MIGRATION_4_5 = Migration(4, 5) { db ->
            db.execSQL("CREATE TABLE IF NOT EXISTS downloaded_results (id TEXT NOT NULL PRIMARY KEY, title TEXT NOT NULL, profileName TEXT NOT NULL, fileName TEXT NOT NULL, filePath TEXT NOT NULL, mimeType TEXT NOT NULL, downloadDate INTEGER NOT NULL DEFAULT 0)")
        }

        val MIGRATION_5_6 = Migration(5, 6) { db ->
            db.execSQL("DROP TABLE IF EXISTS downloaded_results")
            db.execSQL("CREATE TABLE IF NOT EXISTS downloaded_results (uid INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, resultId TEXT NOT NULL, title TEXT NOT NULL, profileName TEXT NOT NULL, fileName TEXT NOT NULL, filePath TEXT NOT NULL, mimeType TEXT NOT NULL, downloadDate INTEGER NOT NULL DEFAULT 0)")
        }

        val MIGRATION_6_7 = Migration(6, 7) { db ->
            db.execSQL("CREATE TABLE IF NOT EXISTS circulars (link TEXT NOT NULL PRIMARY KEY, title TEXT NOT NULL, description TEXT NOT NULL, pubDate TEXT NOT NULL, feedSource TEXT NOT NULL DEFAULT '', cachedAt INTEGER NOT NULL DEFAULT 0)")
        }

        val MIGRATION_9_10 = Migration(9, 10) { db ->
            db.execSQL("ALTER TABLE results ADD COLUMN isViewed INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE results ADD COLUMN isBookmarked INTEGER NOT NULL DEFAULT 0")
            db.execSQL("CREATE TABLE IF NOT EXISTS notification_history (id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, title TEXT NOT NULL, message TEXT NOT NULL, type TEXT NOT NULL, timestamp INTEGER NOT NULL DEFAULT 0)")
        }
    }
}
