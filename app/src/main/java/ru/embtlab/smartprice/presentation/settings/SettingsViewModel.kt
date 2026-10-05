package ru.embtlab.smartprice.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ru.embtlab.smartprice.data.local.SettingsDataStore
import ru.embtlab.smartprice.domain.model.AppSettings
import ru.embtlab.smartprice.domain.model.AppThemeMode
import ru.embtlab.smartprice.domain.model.CardStylePreset
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val dataStore: SettingsDataStore
) : ViewModel() {

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