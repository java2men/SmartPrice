package ru.embtlab.smartprice.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import ru.embtlab.smartprice.domain.model.AppSettings
import ru.embtlab.smartprice.domain.model.AppThemeMode
import ru.embtlab.smartprice.domain.model.CardStylePreset

private val Context.dataStore by preferencesDataStore(name = "smart_price_settings")

class SettingsDataStore(private val context: Context) {

    private object Keys {
        val CARD_PRESET = stringPreferencesKey("card_preset")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
    }

    val settingsFlow: Flow<AppSettings> = context.dataStore.data.map { preferences ->
        val presetName = preferences[Keys.CARD_PRESET] ?: CardStylePreset.CLASSIC.name
        val themeName = preferences[Keys.THEME_MODE] ?: AppThemeMode.SYSTEM.name
        val dynamicColor = preferences[Keys.DYNAMIC_COLOR] ?: true

        AppSettings(
            cardPreset = runCatching { CardStylePreset.valueOf(presetName) }.getOrDefault(CardStylePreset.CLASSIC),
            themeMode = runCatching { AppThemeMode.valueOf(themeName) }.getOrDefault(AppThemeMode.SYSTEM),
            dynamicColor = dynamicColor
        )
    }

    suspend fun setCardPreset(preset: CardStylePreset) {
        context.dataStore.edit { it[Keys.CARD_PRESET] = preset.name }
    }

    suspend fun setThemeMode(mode: AppThemeMode) {
        context.dataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun setDynamicColor(enabled: Boolean) {
        context.dataStore.edit { it[Keys.DYNAMIC_COLOR] = enabled }
    }
}