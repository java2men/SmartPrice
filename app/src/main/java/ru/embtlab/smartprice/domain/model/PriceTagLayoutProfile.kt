package ru.embtlab.smartprice.domain.model

data class PriceTagLayoutProfile(
    val centsHeightRatio: Float = 0.40f,      // Отношение высоты копеек к высоте рублей
    val centsOffsetRatio: Float = 0.50f,      // Горизонтальный сдвиг копеек вправо относительно ширины рублей
    val samplesCount: Int = 0                 // Количество подтвержденных образцов от пользователя
) {
    /**
     * Плавное обновление профиля по методу экспоненциального скользящего среднего (EMA).
     */
    fun updateWithSample(newHeightRatio: Float, newOffsetRatio: Float): PriceTagLayoutProfile {
        // На первых образцах учимся быстрее (вес 0.5), при накоплении опыта стабилизируем (вес 0.2)
        val alpha = if (samplesCount < 5) 0.5f else 0.2f
        val updatedHeight = (centsHeightRatio * (1f - alpha)) + (newHeightRatio * alpha)
        val updatedOffset = (centsOffsetRatio * (1f - alpha)) + (newOffsetRatio * alpha)
        return copy(
            centsHeightRatio = updatedHeight.coerceIn(0.15f, 0.85f),
            centsOffsetRatio = updatedOffset.coerceIn(0.10f, 1.20f),
            samplesCount = samplesCount + 1
        )
    }
}