package ru.embtlab.smartprice.domain.model

import androidx.compose.runtime.Immutable

@Immutable
data class AppSettings(
    val cardPreset: CardStylePreset = CardStylePreset.CLASSIC,
    val themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    val dynamicColor: Boolean = true
)