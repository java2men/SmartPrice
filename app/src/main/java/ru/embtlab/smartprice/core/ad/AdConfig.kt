package ru.embtlab.smartprice.core.ad

object AdConfig {
    /**
     * Численный ID слота баннера VK Рекламы (ads.vk.ru).
     * Значение <= 0 означает, что слот ещё не задан.
     */
    const val VK_BANNER_SLOT_ID: Int = 2093874

    val isBannerEnabled: Boolean
        get() = VK_BANNER_SLOT_ID > 0

    val testDeviceIds: List<String> = listOf(
        "3208f898-616b-460b-975c-0743d525cdc6",
        "de8b79dc-1854-4e6e-b732-4a752b1a4f4d",
    )
}