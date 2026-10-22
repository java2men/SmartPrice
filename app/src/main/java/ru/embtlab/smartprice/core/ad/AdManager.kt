package ru.embtlab.smartprice.core.ad

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object AdManager {
    private val _isSdkInitialized = MutableStateFlow(false)
    val isSdkInitialized: StateFlow<Boolean> = _isSdkInitialized.asStateFlow()

    fun markSdkInitialized() {
        _isSdkInitialized.value = true
    }
}