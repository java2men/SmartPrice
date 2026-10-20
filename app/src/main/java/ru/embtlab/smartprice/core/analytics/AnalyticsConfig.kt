package ru.embtlab.smartprice.core.analytics

object AnalyticsConfig {
    const val MY_TRACKER_SDK_KEY: String = "68878970625945961690"

    val isTrackerEnabled: Boolean
        get() = MY_TRACKER_SDK_KEY.isNotBlank()
}