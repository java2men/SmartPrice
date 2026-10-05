package ru.embtlab.smartprice.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import ru.embtlab.smartprice.domain.repository.ComparisonHistoryRepository
import ru.embtlab.smartprice.domain.usecase.CalculateComparisonUseCase
import ru.embtlab.smartprice.domain.usecase.GetHistoryUseCase
import ru.embtlab.smartprice.domain.usecase.SaveComparisonUseCase
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DomainModule {

    @Provides
    @Singleton
    fun provideCalculateComparisonUseCase(): CalculateComparisonUseCase {
        return CalculateComparisonUseCase()
    }

    @Provides
    @Singleton
    fun provideGetHistoryUseCase(repository: ComparisonHistoryRepository): GetHistoryUseCase {
        return GetHistoryUseCase(repository)
    }

    @Provides
    @Singleton
    fun provideSaveComparisonUseCase(repository: ComparisonHistoryRepository): SaveComparisonUseCase {
        return SaveComparisonUseCase(repository)
    }
}