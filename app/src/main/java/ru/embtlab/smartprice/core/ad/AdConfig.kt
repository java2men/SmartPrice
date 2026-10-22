package ru.embtlab.smartprice.core.ad

import ru.embtlab.smartprice.BuildConfig

object AdConfig {
    // Автоматически подставит нужный SLOT_ID для текущего варианта сборки
    const val VK_BANNER_SLOT_ID: Int = BuildConfig.VK_BANNER_SLOT_ID

    val isBannerEnabled: Boolean
        get() = BuildConfig.ADS_ENABLED && VK_BANNER_SLOT_ID > 0

    val testDeviceIds: List<String> = listOf(
//        "3208f898-616b-460b-975c-0743d525cdc6",
//        "de8b79dc-1854-4e6e-b732-4a752b1a4f4d",
//        "48b14dd7-6c92-45cb-8de3-a84aa0e02078",
//        "592df47b-66db-4770-b2dc-72067d4f51f3",
//        "784d19e9-5c97-457b-b98c-6b3e2659efbf",
        "3208f898-616b-460b-975c-0743d525cdc6",
        "784d19e9-5c97-457b-b98c-6b3e2659efbf",
        "a45297ad-1c2e-4678-8a50-fdb047ee4e2e",
//        "1d718b07-1869-4a3d-b8a3-fb0daff1f526",
    )
}