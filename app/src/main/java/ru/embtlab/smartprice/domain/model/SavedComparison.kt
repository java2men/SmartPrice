// domain/model/SavedComparison.kt
package ru.embtlab.smartprice.domain.model

data class SavedComparison(
    val id: Long = 0,
    val title: String,                // Название сравнения (например, "Молоко" или дата)
    val bestProductName: String,       // Какой товар победил
    val bestUnitPriceFormatted: String,// Выгодная цена (например, "89.90 ₽ / л")
    val savingsInfo: String,           // Разница (например, "Выгоднее на 18.5%")
    val timestamp: Long = System.currentTimeMillis()
)