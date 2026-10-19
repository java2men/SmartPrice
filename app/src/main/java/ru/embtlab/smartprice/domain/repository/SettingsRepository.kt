package ru.embtlab.smartprice.domain.repository

import kotlinx.coroutines.flow.Flow
import ru.embtlab.smartprice.domain.model.settings.AppSettings
import ru.embtlab.smartprice.domain.model.settings.AppThemeMode
import ru.embtlab.smartprice.domain.model.settings.CardStylePreset

interface SettingsRepository {
    val settingsFlow: Flow<AppSettings>
    val recentProductNamesFlow: Flow<List<String>>

    suspend fun addRecentProductName(name: String)
    suspend fun removeRecentProductName(name: String)
    suspend fun setCardPreset(preset: CardStylePreset)
    suspend fun setThemeMode(mode: AppThemeMode)
    suspend fun setDynamicColor(enabled: Boolean)
    suspend fun setVoiceInputEnabled(enabled: Boolean)
    suspend fun setOcrScannerEnabled(enabled: Boolean)
}