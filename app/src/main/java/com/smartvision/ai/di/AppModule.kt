package com.smartvision.ai.di

import com.smartvision.ai.data.repository.*
import com.smartvision.ai.domain.usecase.*
import dagger.*
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds @Singleton abstract fun bindResultRepo(impl: ResultRepositoryImpl): ResultRepository
}
