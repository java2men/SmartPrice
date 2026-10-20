package ru.embtlab.smartprice.core.distribution

import android.app.Application

class AppDistributionInitializer : DistributionInitializer {
    override fun initialize(application: Application) {
        // Никаких инициализаций и трекеров: чистый запуск
    }
}