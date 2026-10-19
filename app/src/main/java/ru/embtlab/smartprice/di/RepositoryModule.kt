package ru.embtlab.smartprice.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import ru.embtlab.smartprice.data.repository.ComparisonHistoryRepositoryImpl
import ru.embtlab.smartprice.data.repository.SettingsRepositoryImpl
import ru.embtlab.smartprice.domain.repository.ComparisonHistoryRepository
import ru.embtlab.smartprice.domain.repository.SettingsRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindComparisonHistoryRepository(
        impl: ComparisonHistoryRepositoryImpl
    ): ComparisonHistoryRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(
        impl: SettingsRepositoryImpl
    ): SettingsRepository
}