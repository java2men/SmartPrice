// data/local/entity/ComparisonHistoryEntity.kt
package ru.embtlab.smartprice.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import ru.embtlab.smartprice.domain.model.SavedComparison

@Entity(tableName = "comparison_history")
data class ComparisonHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val bestProductName: String,
    val bestUnitPriceFormatted: String,
    val savingsInfo: String,
    val timestamp: Long
) {
    fun toDomain(): SavedComparison = SavedComparison(
        id = id,
        title = title,
        bestProductName = bestProductName,
        bestUnitPriceFormatted = bestUnitPriceFormatted,
        savingsInfo = savingsInfo,
        timestamp = timestamp
    )

    companion object {
        fun fromDomain(domain: SavedComparison): ComparisonHistoryEntity = ComparisonHistoryEntity(
            id = domain.id,
            title = domain.title,
            bestProductName = domain.bestProductName, // <-- Исправлено здесь
            bestUnitPriceFormatted = domain.bestUnitPriceFormatted,
            savingsInfo = domain.savingsInfo,
            timestamp = domain.timestamp
        )
    }
}