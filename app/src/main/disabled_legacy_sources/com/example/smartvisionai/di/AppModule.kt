package com.example.smartvisionai.di

import android.content.Context
import com.example.smartvisionai.data.local.AppDatabase
import com.example.smartvisionai.data.local.ScanHistoryDao
import com.example.smartvisionai.data.local.SettingsPreferences
import com.example.smartvisionai.ml.GeminiRepository
import com.example.smartvisionai.ml.MLKitRepository
import com.example.smartvisionai.ml.TFLiteRepository
import com.example.smartvisionai.repository.ScanHistoryRepository
import com.example.smartvisionai.utils.NetworkUtils
import com.example.smartvisionai.utils.VoiceManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides @Singleton
    fun provideDatabase(@ApplicationContext ctx: Context): AppDatabase =
        AppDatabase.getInstance(ctx)

    @Provides @Singleton
    fun provideScanHistoryDao(db: AppDatabase): ScanHistoryDao = db.scanHistoryDao()

    @Provides @Singleton
    fun provideScanHistoryRepository(dao: ScanHistoryDao) = ScanHistoryRepository(dao)

    @Provides @Singleton
    fun provideMLKitRepository() = MLKitRepository()

    @Provides @Singleton
    fun provideGeminiRepository() = GeminiRepository()

    @Provides @Singleton
    fun provideTFLiteRepository(@ApplicationContext ctx: Context) = TFLiteRepository(ctx)

    @Provides @Singleton
    fun provideSettingsPreferences(@ApplicationContext ctx: Context) = SettingsPreferences(ctx)

    @Provides @Singleton
    fun provideVoiceManager(@ApplicationContext ctx: Context) = VoiceManager(ctx)

    @Provides @Singleton
    fun provideNetworkUtils(@ApplicationContext ctx: Context) = NetworkUtils(ctx)
}
