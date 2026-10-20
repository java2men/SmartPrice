package ru.embtlab.smartprice.core.analytics

import ru.embtlab.smartprice.BuildConfig

object AnalyticsConfig {
    val MY_TRACKER_SDK_KEY: String = BuildConfig.MY_TRACKER_KEY

    val isTrackerEnabled: Boolean
        get() = MY_TRACKER_SDK_KEY.isNotBlank()
}