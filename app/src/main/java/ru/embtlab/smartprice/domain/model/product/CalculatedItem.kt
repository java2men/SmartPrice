package ru.embtlab.smartprice.domain.model.product

data class CalculatedItem(
    val product: ProductItem,
    val unitPrice: Double,
    val isBestChoice: Boolean = false,
    val percentMoreExpensive: Double = 0.0
)