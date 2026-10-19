package ru.embtlab.smartprice.data.repository

import kotlinx.coroutines.flow.Flow
import ru.embtlab.smartprice.data.local.datastore.SettingsDataStore
import ru.embtlab.smartprice.domain.model.settings.AppSettings
import ru.embtlab.smartprice.domain.model.settings.AppThemeMode
import ru.embtlab.smartprice.domain.model.settings.CardStylePreset
import ru.embtlab.smartprice.domain.repository.SettingsRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val dataStore: SettingsDataStore
) : SettingsRepository {

    override val settingsFlow: Flow<AppSettings> = dataStore.settingsFlow

    override val recentProductNamesFlow: Flow<List<String>> = dataStore.recentProductNamesFlow

    override suspend fun addRecentProductName(name: String) {
        dataStore.addRecentProductName(name)
    }

    override suspend fun removeRecentProductName(name: String) {
        dataStore.removeRecentProductName(name)
    }

    override suspend fun setCardPreset(preset: CardStylePreset) {
        dataStore.setCardPreset(preset)
    }

    override suspend fun setThemeMode(mode: AppThemeMode) {
        dataStore.setThemeMode(mode)
    }

    override suspend fun setDynamicColor(enabled: Boolean) {
        dataStore.setDynamicColor(enabled)
    }

    override suspend fun setVoiceInputEnabled(enabled: Boolean) {
        dataStore.setVoiceInputEnabled(enabled)
    }

    override suspend fun setOcrScannerEnabled(enabled: Boolean) {
        dataStore.setOcrScannerEnabled(enabled)
    }
}