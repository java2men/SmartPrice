package ru.embtlab.smartprice.domain.model.product

enum class DiscountType(val label: String) {
    NONE("Нет"),
    PERCENT("%"),
    ONE_PLUS_ONE("1+1"),
    TWO_PLUS_ONE("2+1")
}