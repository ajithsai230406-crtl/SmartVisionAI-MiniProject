package com.smartvision.ai.di

import com.smartvision.ai.data.repository.*
import com.smartvision.ai.domain.usecase.*
import dagger.*
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class UseCaseModule {

    @Binds @Singleton
    abstract fun bindDetectObjects(impl: DetectObjectsUseCaseImpl): DetectObjectsUseCase

    @Binds @Singleton
    abstract fun bindOcr(impl: OcrUseCaseImpl): OcrUseCase

    @Binds @Singleton
    abstract fun bindQrScan(impl: ScanQrUseCaseImpl): ScanQrUseCase

    @Binds @Singleton
    abstract fun bindStudentHelper(impl: StudentHelperUseCaseImpl): StudentHelperUseCase

    @Binds @Singleton
    abstract fun bindWasteClassifier(impl: ClassifyWasteUseCaseImpl): ClassifyWasteUseCase

    @Binds @Singleton
    abstract fun bindMedicalScan(impl: MedicalScanUseCaseImpl): MedicalScanUseCase

    @Binds @Singleton
    abstract fun bindResultRepository(impl: ResultRepositoryImpl): ResultRepository

    // HistoryRepository is a class with @Inject constructor, so it doesn't need a @Binds method.
    // The previous bindHistoryRepository(impl: HistoryRepositoryImpl) was failing because HistoryRepositoryImpl didn't exist.
}
