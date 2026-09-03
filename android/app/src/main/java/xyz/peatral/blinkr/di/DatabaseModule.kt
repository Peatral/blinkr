package xyz.peatral.blinkr.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import xyz.peatral.blinkr.data.room.AppDatabase
import xyz.peatral.blinkr.data.room.SessionDao
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "blinkr_database"
        ).build()
    }

    @Provides
    fun provideSessionDao(database: AppDatabase): SessionDao = database.sessionDao()
}