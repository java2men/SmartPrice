package ru.embtlab.smartprice.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import ru.embtlab.smartprice.domain.model.AppSettings
import ru.embtlab.smartprice.domain.model.AppThemeMode
import ru.embtlab.smartprice.domain.model.CardStylePreset
import ru.embtlab.smartprice.domain.model.PriceTagLayoutProfile
import ru.embtlab.smartprice.domain.model.ProductConstants

private val Context.dataStore by preferencesDataStore(name = "smart_price_settings")

class SettingsDataStore(private val context: Context) {

    private object Keys {
        val CARD_PRESET = stringPreferencesKey("card_preset")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val RECENT_PRODUCT_NAMES = stringPreferencesKey("recent_product_names")

        // Ключи адаптивного профиля ценников
        val CENTS_HEIGHT_RATIO = floatPreferencesKey("cents_height_ratio")
        val CENTS_OFFSET_RATIO = floatPreferencesKey("cents_offset_ratio")
        val SAMPLES_COUNT = intPreferencesKey("profile_samples_count")
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

    val recentProductNamesFlow: Flow<List<String>> = context.dataStore.data.map { preferences ->
        val raw = preferences[Keys.RECENT_PRODUCT_NAMES] ?: ""
        if (raw.isBlank()) emptyList()
        else raw.split("||").filter { it.isNotBlank() }
    }

    // Поток адаптивного профиля геометрических пропорций
    val priceTagProfileFlow: Flow<PriceTagLayoutProfile> = context.dataStore.data.map { preferences ->
        PriceTagLayoutProfile(
            centsHeightRatio = preferences[Keys.CENTS_HEIGHT_RATIO] ?: 0.40f,
            centsOffsetRatio = preferences[Keys.CENTS_OFFSET_RATIO] ?: 0.50f,
            samplesCount = preferences[Keys.SAMPLES_COUNT] ?: 0
        )
    }

    suspend fun savePriceTagProfile(profile: PriceTagLayoutProfile) {
        context.dataStore.edit { preferences ->
            preferences[Keys.CENTS_HEIGHT_RATIO] = profile.centsHeightRatio
            preferences[Keys.CENTS_OFFSET_RATIO] = profile.centsOffsetRatio
            preferences[Keys.SAMPLES_COUNT] = profile.samplesCount
        }
    }

    suspend fun addRecentProductName(name: String) {
        val clean = name.trim()
        if (clean.isBlank()) return
        context.dataStore.edit { preferences ->
            val raw = preferences[Keys.RECENT_PRODUCT_NAMES] ?: ""
            val currentList = if (raw.isBlank()) emptyList() else raw.split("||").filter { it.isNotBlank() }
            val updated = (listOf(clean) + currentList.filterNot { it.equals(clean, ignoreCase = true) })
                .take(ProductConstants.MAX_RECENT_PRODUCTS_COUNT)
            preferences[Keys.RECENT_PRODUCT_NAMES] = updated.joinToString("||")
        }
    }

    suspend fun removeRecentProductName(name: String) {
        context.dataStore.edit { preferences ->
            val raw = preferences[Keys.RECENT_PRODUCT_NAMES] ?: ""
            val currentList = if (raw.isBlank()) emptyList() else raw.split("||").filter { it.isNotBlank() }
            val updated = currentList.filterNot { it.equals(name, ignoreCase = true) }
            preferences[Keys.RECENT_PRODUCT_NAMES] = updated.joinToString("||")
        }
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