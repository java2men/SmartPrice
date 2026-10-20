package ru.embtlab.smartprice.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ru.embtlab.smartprice.core.analytics.AnalyticsEvents
import ru.embtlab.smartprice.core.analytics.AnalyticsManager
import ru.embtlab.smartprice.domain.model.settings.AppSettings
import ru.embtlab.smartprice.domain.model.settings.AppThemeMode
import ru.embtlab.smartprice.domain.model.settings.CardStylePreset
import ru.embtlab.smartprice.domain.repository.SettingsRepository
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: SettingsRepository
) : ViewModel() {

    val settings: StateFlow<AppSettings?> = repository.settingsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    fun onPresetChanged(preset: CardStylePreset) {
        AnalyticsManager.trackEvent(
            AnalyticsEvents.PRESET_CHANGED,
            mapOf(AnalyticsEvents.Params.PRESET_NAME to preset.name)
        )

        viewModelScope.launch { repository.setCardPreset(preset) }
    }

    fun onThemeModeChanged(mode: AppThemeMode) {
        viewModelScope.launch { repository.setThemeMode(mode) }
    }

    fun onDynamicColorChanged(enabled: Boolean) {
        viewModelScope.launch { repository.setDynamicColor(enabled) }
    }

    fun onVoiceInputEnabledChanged(enabled: Boolean) {
        viewModelScope.launch { repository.setVoiceInputEnabled(enabled) }
    }

    fun onOcrScannerEnabledChanged(enabled: Boolean) {
        viewModelScope.launch { repository.setOcrScannerEnabled(enabled) }
    }
}