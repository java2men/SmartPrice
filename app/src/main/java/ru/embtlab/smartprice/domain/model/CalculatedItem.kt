package ru.embtlab.smartprice.domain.model

data class CalculatedItem(
    val product: ProductItem,
    val unitPrice: Double,         // Цена за 1 кг / 1 л / 1 шт
    val isBestChoice: Boolean = false,
    val percentMoreExpensive: Double = 0.0 // На сколько % этот товар дороже лидера
)