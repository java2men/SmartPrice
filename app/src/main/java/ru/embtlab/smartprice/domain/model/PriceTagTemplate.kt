package ru.embtlab.smartprice.domain.model

data class PriceTagLayoutProfile(
    val centsHeightRatio: Float = 0.40f,      // отношение высоты шрифта копеек к рублям
    val centsHorizontalOffsetRatio: Float = 0.50f, // сдвиг копеек вправо относительно ширины рублей
    val isCentsSuperscript: Boolean = true    // верхний индекс
)