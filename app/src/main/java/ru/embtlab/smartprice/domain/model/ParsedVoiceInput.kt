package ru.embtlab.smartprice.domain.model

data class ParsedVoiceInput(
    val name: String? = null,
    val price: String? = null,
    val quantity: String? = null,
    val unit: ProductUnit? = null,
    val discountType: DiscountType? = null,
    val discountPercent: String? = null
)