package ru.embtlab.smartprice.domain.model

import androidx.compose.runtime.Immutable
import java.util.UUID

@Immutable
data class ProductItem(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val priceInput: String = "",
    val quantityInput: String = "",
    val unit: ProductUnit = ProductUnit.GRAM,
    val discountType: DiscountType = DiscountType.NONE,
    val customDiscountPercentInput: String = ""
) {
    val price: Double get() = priceInput.replace(',', '.').toDoubleOrNull() ?: 0.0
    val quantity: Double get() = quantityInput.replace(',', '.').toDoubleOrNull() ?: 0.0

    val effectiveDiscountPercent: Double
        get() = when (discountType) {
            DiscountType.NONE -> 0.0
            DiscountType.ONE_PLUS_ONE -> 50.0
            DiscountType.TWO_PLUS_ONE -> 33.333333333333336
            DiscountType.PERCENT -> {
                val input = customDiscountPercentInput.replace(',', '.').toDoubleOrNull() ?: 0.0
                input.coerceIn(0.0, 99.9)
            }
        }
}