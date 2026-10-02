package ru.embtlab.smartprice.domain.model

data class ParsedPriceTag(
    val price: String? = null,
    val quantity: String? = null,
    val unit: ProductUnit? = null
)