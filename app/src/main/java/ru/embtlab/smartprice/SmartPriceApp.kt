// SmartPriceApp.kt
package ru.embtlab.smartprice

import android.app.Application
import com.my.target.common.MyTargetConfig
import com.my.target.common.MyTargetManager
import com.my.tracker.MyTracker
import dagger.hilt.android.HiltAndroidApp
import ru.embtlab.smartprice.core.ad.AdConfig
import ru.embtlab.smartprice.core.analytics.AnalyticsManager

@HiltAndroidApp
class SmartPriceApp : Application() {

    override fun onCreate() {
        super.onCreate()

        // 1. Инициализация MyTracker через фасад аналитики
        AnalyticsManager.init(this)

        if (BuildConfig.DEBUG) {
            MyTargetManager.setDebugMode(true)

            if (AdConfig.testDeviceIds.isNotEmpty()) {
                val config = MyTargetConfig.Builder()
                    .withTestDevices(*AdConfig.testDeviceIds.toTypedArray())
                    .build()
                MyTargetManager.setSdkConfig(config)
            }
        }

        // 2. Инициализация SDK VK Рекламы
        MyTargetManager.initSdk(this)
    }
}