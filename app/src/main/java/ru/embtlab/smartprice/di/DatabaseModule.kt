package ru.embtlab.smartprice.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import ru.embtlab.smartprice.data.local.datastore.SettingsDataStore
import ru.embtlab.smartprice.data.local.db.AppDatabase
import ru.embtlab.smartprice.data.local.db.dao.ComparisonHistoryDao
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return AppDatabase.getDatabase(context)
    }

    @Provides
    @Singleton
    fun provideComparisonHistoryDao(database: AppDatabase): ComparisonHistoryDao {
        return database.historyDao()
    }

    @Provides
    @Singleton
    fun provideSettingsDataStore(@ApplicationContext context: Context): SettingsDataStore {
        return SettingsDataStore(context)
    }
}