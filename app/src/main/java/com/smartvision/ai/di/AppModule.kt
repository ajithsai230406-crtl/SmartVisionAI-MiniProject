package com.smartvision.ai.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    // This module can be used for other project-wide providers.
    // UseCase bindings are in UseCaseModule.kt
}
