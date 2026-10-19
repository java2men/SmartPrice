package ru.embtlab.smartprice.domain.model.ocr

import ru.embtlab.smartprice.domain.model.product.DiscountType
import ru.embtlab.smartprice.domain.model.product.ProductUnit

data class ParsedVoiceInput(
    val name: String? = null,
    val price: String? = null,
    val quantity: String? = null,
    val unit: ProductUnit? = null,
    val discountType: DiscountType? = null,
    val discountPercent: String? = null
)