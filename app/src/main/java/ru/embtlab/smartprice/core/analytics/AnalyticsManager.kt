package ru.embtlab.smartprice.core.analytics

import android.app.Application
import android.util.Log
import com.my.tracker.MyTracker

object AnalyticsManager {

    private const val TAG = "AnalyticsManager"

    fun init(application: Application) {
        if (!AnalyticsConfig.isTrackerEnabled) {
            Log.d(TAG, "MyTracker отключен: SDK_KEY не задан")
            return
        }

        val trackerConfig = MyTracker.getTrackerConfig().apply {
            isTrackingLaunchEnabled = true
        }

        MyTracker.initTracker(AnalyticsConfig.MY_TRACKER_SDK_KEY, application)
        Log.d(TAG, "MyTracker успешно инициализирован")
    }

    fun trackEvent(name: String, params: Map<String, String>? = null) {
        if (!AnalyticsConfig.isTrackerEnabled) return
        if (params != null) {
            MyTracker.trackEvent(name, params)
        } else {
            MyTracker.trackEvent(name)
        }
    }
}