package ru.embtlab.smartprice.presentation.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ru.embtlab.smartprice.data.local.SettingsDataStore
import ru.embtlab.smartprice.domain.model.AppSettings
import ru.embtlab.smartprice.domain.model.AppThemeMode
import ru.embtlab.smartprice.domain.model.CardStylePreset

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val dataStore = SettingsDataStore(application)

    val settings: StateFlow<AppSettings> = dataStore.settingsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AppSettings()
    )

    fun onPresetChanged(preset: CardStylePreset) {
        viewModelScope.launch { dataStore.setCardPreset(preset) }
    }

    fun onThemeModeChanged(mode: AppThemeMode) {
        viewModelScope.launch { dataStore.setThemeMode(mode) }
    }

    fun onDynamicColorChanged(enabled: Boolean) {
        viewModelScope.launch { dataStore.setDynamicColor(enabled) }
    }
}