package ru.embtlab.smartprice.domain.model.ocr

import ru.embtlab.smartprice.domain.model.product.ProductUnit

data class ParsedPriceTag(
    val price: String? = null,
    val quantity: String? = null,
    val unit: ProductUnit? = null
)