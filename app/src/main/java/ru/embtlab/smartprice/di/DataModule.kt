package ru.embtlab.smartprice.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import ru.embtlab.smartprice.data.local.AppDatabase
import ru.embtlab.smartprice.data.local.SettingsDataStore
import ru.embtlab.smartprice.data.local.dao.ComparisonHistoryDao
import ru.embtlab.smartprice.data.repository.ComparisonHistoryRepositoryImpl
import ru.embtlab.smartprice.domain.repository.ComparisonHistoryRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return AppDatabase.getDatabase(context)
    }

    @Provides
    @Singleton
    fun provideHistoryDao(database: AppDatabase): ComparisonHistoryDao {
        return database.historyDao()
    }

    @Provides
    @Singleton
    fun provideHistoryRepository(dao: ComparisonHistoryDao): ComparisonHistoryRepository {
        return ComparisonHistoryRepositoryImpl(dao)
    }

    @Provides
    @Singleton
    fun provideSettingsDataStore(@ApplicationContext context: Context): SettingsDataStore {
        return SettingsDataStore(context)
    }
}