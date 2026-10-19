package ru.embtlab.smartprice.domain.model.settings

data class AppSettings(
    val cardPreset: CardStylePreset = CardStylePreset.CLASSIC,
    val themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    val dynamicColor: Boolean = true,
    val isVoiceInputEnabled: Boolean = true,
    val isOcrScannerEnabled: Boolean = true,
)