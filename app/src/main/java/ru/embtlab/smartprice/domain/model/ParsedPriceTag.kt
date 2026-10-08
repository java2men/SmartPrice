package ru.embtlab.smartprice.domain.model

data class ParsedPriceTag(
    val name: String? = null,
    val price: String? = null,
    val quantity: String? = null,
    val unit: ProductUnit? = null
)