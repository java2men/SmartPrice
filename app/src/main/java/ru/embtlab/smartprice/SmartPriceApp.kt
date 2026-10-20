// SmartPriceApp.kt
package ru.embtlab.smartprice

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import ru.embtlab.smartprice.core.distribution.AppDistributionInitializer

@HiltAndroidApp
class SmartPriceApp : Application() {

    override fun onCreate() {
        super.onCreate()
        AppDistributionInitializer().initialize(this)
    }
}