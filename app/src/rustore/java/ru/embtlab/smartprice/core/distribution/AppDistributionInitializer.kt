package ru.embtlab.smartprice.core.distribution

import android.app.Application
import com.my.target.common.MyTargetConfig
import com.my.target.common.MyTargetManager
import ru.embtlab.smartprice.BuildConfig
import ru.embtlab.smartprice.core.ad.AdConfig
import ru.embtlab.smartprice.core.ad.AdManager
import ru.embtlab.smartprice.core.analytics.AnalyticsManager

class AppDistributionInitializer : DistributionInitializer {
    override fun initialize(application: Application) {
        //AnalyticsManager.init(application)
        if (BuildConfig.DEBUG) {
            MyTargetManager.setDebugMode(true)
            if (AdConfig.testDeviceIds.isNotEmpty()) {

                val config = MyTargetConfig.Builder()
                    .withTestDevices(*AdConfig.testDeviceIds.toTypedArray())
                    .build()
                MyTargetManager.setSdkConfig(config)
            }
        }
//        MyTargetManager.initSdk(application)

//        AdManager.markSdkInitialized()
    }
}