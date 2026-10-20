package ru.embtlab.smartprice.core.analytics

import android.app.Application

object AnalyticsManager {
    fun init(application: Application) {
        // В свободной сборке аналитика полностью отключена
    }

    fun trackEvent(name: String, params: Map<String, String>? = null) {
        // Заглушка: события никуда не отправляются
    }
}