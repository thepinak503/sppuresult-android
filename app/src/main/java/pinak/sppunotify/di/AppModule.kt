package pinak.sppunotify.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import pinak.sppunotify.data.local.CircularDao
import pinak.sppunotify.data.local.RevalCourseDao
import pinak.sppunotify.data.local.ResultDatabase
import pinak.sppunotify.data.local.DownloadedResultDao
import pinak.sppunotify.data.local.NotificationHistoryDao
import androidx.work.WorkManager
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideResultDatabase(@ApplicationContext context: Context): ResultDatabase {
        return Room.databaseBuilder(
            context,
            ResultDatabase::class.java,
            "results.db"
        ).addMigrations(
            ResultDatabase.MIGRATION_1_2,
            ResultDatabase.MIGRATION_2_3,
            ResultDatabase.MIGRATION_3_4,
            ResultDatabase.MIGRATION_4_5,
            ResultDatabase.MIGRATION_5_6,
            ResultDatabase.MIGRATION_6_7,
            ResultDatabase.MIGRATION_7_8,
            ResultDatabase.MIGRATION_8_9,
            ResultDatabase.MIGRATION_9_10,
            ResultDatabase.MIGRATION_10_11,
            ResultDatabase.MIGRATION_11_12,
            ResultDatabase.MIGRATION_12_13
        ).build()
    }

    @Provides
    @Singleton
    fun provideResultDao(db: ResultDatabase): pinak.sppunotify.data.local.ResultDao = db.dao

    @Provides
    @Singleton
    fun provideRemovedResultDao(db: ResultDatabase): pinak.sppunotify.data.local.RemovedResultDao = db.removedResultDao

    @Provides
    @Singleton
    fun provideRevalCourseDao(db: ResultDatabase): RevalCourseDao = db.revalDao

    @Provides
    @Singleton
    fun provideDownloadedResultDao(db: ResultDatabase): DownloadedResultDao = db.downloadedDao

    @Provides
    @Singleton
    fun provideCircularDao(db: ResultDatabase): CircularDao = db.circularDao

    @Provides
    @Singleton
    fun provideExamDateDao(db: ResultDatabase): pinak.sppunotify.data.local.ExamDateDao = db.examDateDao

    @Provides
    @Singleton
    fun provideNotificationHistoryDao(db: ResultDatabase): NotificationHistoryDao = db.notificationHistoryDao

    @Provides
    @Singleton
    fun provideSyncLogDao(db: ResultDatabase): pinak.sppunotify.data.local.SyncLogDao = db.syncLogDao

    @Provides
    @Singleton
    fun provideWorkManager(@ApplicationContext context: Context): WorkManager {
        return WorkManager.getInstance(context)
    }
}
