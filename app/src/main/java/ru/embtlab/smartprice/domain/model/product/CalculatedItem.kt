package ru.embtlab.smartprice.domain.model.product

import androidx.compose.runtime.Immutable

@Immutable
data class CalculatedItem(
    val product: ProductItem,
    val unitPrice: Double,
    val isBestChoice: Boolean = false,
    val percentMoreExpensive: Double = 0.0
)