package ru.embtlab.smartprice.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import ru.embtlab.smartprice.domain.repository.ComparisonHistoryRepository
import ru.embtlab.smartprice.domain.usecase.comparison.CalculateComparisonUseCase
import ru.embtlab.smartprice.domain.usecase.comparison.DeleteComparisonUseCase
import ru.embtlab.smartprice.domain.usecase.comparison.GetHistoryUseCase
import ru.embtlab.smartprice.domain.usecase.comparison.SaveComparisonUseCase
import ru.embtlab.smartprice.domain.usecase.ocr.ParsePriceTagUseCase
import ru.embtlab.smartprice.domain.usecase.voice.ParseVoiceInputUseCase
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
    fun provideGetHistoryUseCase(
        repository: ComparisonHistoryRepository
    ): GetHistoryUseCase {
        return GetHistoryUseCase(repository)
    }

    @Provides
    @Singleton
    fun provideSaveComparisonUseCase(
        repository: ComparisonHistoryRepository
    ): SaveComparisonUseCase {
        return SaveComparisonUseCase(repository)
    }

    @Provides
    @Singleton
    fun provideDeleteComparisonUseCase(
        repository: ComparisonHistoryRepository
    ): DeleteComparisonUseCase {
        return DeleteComparisonUseCase(repository)
    }

    @Provides
    @Singleton
    fun provideParseVoiceInputUseCase(): ParseVoiceInputUseCase {
        return ParseVoiceInputUseCase()
    }

    @Provides
    @Singleton
    fun provideParsePriceTagUseCase(): ParsePriceTagUseCase {
        return ParsePriceTagUseCase()
    }
}