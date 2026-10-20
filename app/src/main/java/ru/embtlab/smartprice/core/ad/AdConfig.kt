package ru.embtlab.smartprice.core.ad

import ru.embtlab.smartprice.BuildConfig

object AdConfig {
    // Автоматически подставит нужный SLOT_ID для текущего варианта сборки
    val VK_BANNER_SLOT_ID: Int = BuildConfig.VK_BANNER_SLOT_ID

    val isBannerEnabled: Boolean
        get() = BuildConfig.ADS_ENABLED && VK_BANNER_SLOT_ID > 0

    val testDeviceIds: List<String> = listOf(
        "3208f898-616b-460b-975c-0743d525cdc6",
        "de8b79dc-1854-4e6e-b732-4a752b1a4f4d",
        "48b14dd7-6c92-45cb-8de3-a84aa0e02078",
    )
}