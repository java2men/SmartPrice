// data/local/entity/ComparisonHistoryEntity.kt
package ru.embtlab.smartprice.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import ru.embtlab.smartprice.data.local.converter.ProductListConverter
import ru.embtlab.smartprice.domain.model.SavedComparison

@Entity(tableName = "comparison_history")
data class ComparisonHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val bestProductName: String,
    val bestUnitPriceFormatted: String,
    val savingsInfo: String,
    val itemsJson: String = "[]",
    val timestamp: Long
) {
    fun toDomain(): SavedComparison {
        val converter = ProductListConverter()
        return SavedComparison(
            id = id,
            title = title,
            bestProductName = bestProductName,
            bestUnitPriceFormatted = bestUnitPriceFormatted,
            savingsInfo = savingsInfo,
            items = converter.toProductList(itemsJson),
            timestamp = timestamp
        )
    }

    companion object {
        fun fromDomain(domain: SavedComparison): ComparisonHistoryEntity {
            val converter = ProductListConverter()
            return ComparisonHistoryEntity(
                id = domain.id,
                title = domain.title,
                bestProductName = domain.bestProductName,
                bestUnitPriceFormatted = domain.bestUnitPriceFormatted,
                savingsInfo = domain.savingsInfo,
                itemsJson = converter.fromProductList(domain.items),
                timestamp = domain.timestamp
            )
        }
    }
}