package com.smartvision.ai.di

import android.content.Context
import androidx.room.Room
import com.smartvision.ai.data.db.AppDatabase
import com.smartvision.ai.data.db.ScanHistoryDao
import com.smartvision.ai.data.local.HistoryDao
import com.smartvision.ai.data.local.OcrScanDao
import com.smartvision.ai.data.local.StudentHelperHistoryDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
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
            AppDatabase.DATABASE_NAME
        )
            .addMigrations(AppDatabase.MIGRATION_2_3)
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideScanHistoryDao(database: AppDatabase): ScanHistoryDao {
        return database.scanHistoryDao()
    }

    @Provides
    fun provideHistoryDao(database: AppDatabase): HistoryDao {
        return database.historyDao()
    }

    @Provides
    @Singleton
    fun provideGeminiCacheDao(database: AppDatabase): com.smartvision.ai.data.db.GeminiCacheDao {
        return database.geminiCacheDao()
    }

    @Provides
    fun provideOcrScanDao(database: AppDatabase): OcrScanDao {
        return database.ocrScanDao()
    }

    @Provides
    fun provideStudentHelperHistoryDao(database: AppDatabase): StudentHelperHistoryDao {
        return database.studentHelperHistoryDao()
    }
}
