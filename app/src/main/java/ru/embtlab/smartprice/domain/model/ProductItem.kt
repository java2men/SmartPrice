// domain/model/ProductItem.kt
package ru.embtlab.smartprice.domain.model

import java.util.UUID

data class ProductItem(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val priceInput: String = "",
    val quantityInput: String = "",
    val unit: ProductUnit = ProductUnit.GRAM,
    val discountType: DiscountType = DiscountType.NONE,
    val customDiscountPercentInput: String = "" // Для ручного ввода процента, например "25"
) {
    val price: Double get() = priceInput.replace(',', '.').toDoubleOrNull() ?: 0.0
    val quantity: Double get() = quantityInput.replace(',', '.').toDoubleOrNull() ?: 0.0

    // Эффективный процент скидки в зависимости от выбранного режима
    val effectiveDiscountPercent: Double
        get() = when (discountType) {
            DiscountType.NONE -> 0.0
            DiscountType.ONE_PLUS_ONE -> 50.0 // 2 шт по цене 1 = скидка 50%
            DiscountType.TWO_PLUS_ONE -> 33.333333333333336 // 3 шт по цене 2 = скидка 33.33%
            DiscountType.PERCENT -> {
                val input = customDiscountPercentInput.replace(',', '.').toDoubleOrNull() ?: 0.0
                input.coerceIn(0.0, 99.9) // Защита от отрицательных чисел и 100%
            }
        }
}