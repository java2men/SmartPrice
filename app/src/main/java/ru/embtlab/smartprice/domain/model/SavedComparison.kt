package ru.embtlab.smartprice.domain.model

data class SavedComparison(
    val id: Long = 0,
    val title: String,
    val bestProductName: String,
    val bestUnitPriceFormatted: String,
    val savingsInfo: String,
    val items: List<ProductItem> = emptyList(),
    val timestamp: Long = System.currentTimeMillis()
)