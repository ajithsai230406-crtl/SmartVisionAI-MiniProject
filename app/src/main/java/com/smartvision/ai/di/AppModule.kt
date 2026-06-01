package com.smartvision.ai.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    // Database and DAO provisions moved to DatabaseModule.kt to resolve conflicts.
}
