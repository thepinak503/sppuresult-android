package pinak.sppunotify.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import pinak.sppunotify.data.local.ResultDatabase
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
        ).addMigrations(ResultDatabase.MIGRATION_1_2)
            .build()
    }
}
