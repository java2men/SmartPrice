// domain/usecase/CalculateComparisonUseCase.kt
package ru.embtlab.smartprice.domain.usecase

import ru.embtlab.smartprice.domain.model.CalculatedItem
import ru.embtlab.smartprice.domain.model.ProductItem

class CalculateComparisonUseCase {

    operator fun invoke(items: List<ProductItem>): Pair<List<CalculatedItem>, Boolean> {
        val validItems = items.filter { it.price > 0.0 && it.quantity > 0.0 }
        if (validItems.isEmpty()) return Pair(emptyList(), false)

        // 1. Расчет цены с учетом акции
        val rawCalculated = validItems.map { item ->
            val effectivePrice = item.price * (1.0 - item.effectiveDiscountPercent / 100.0)
            val baseQuantity = item.quantity * item.unit.factorToBase
            val unitPrice = if (baseQuantity > 0.0) effectivePrice / baseQuantity else 0.0
            CalculatedItem(product = item, unitPrice = unitPrice)
        }

        val uniqueCategories = validItems.map { it.unit.category }.distinct()
        val hasIncompatibleUnits = uniqueCategories.size > 1

        if (hasIncompatibleUnits) {
            val nonComparedResults = rawCalculated.map {
                it.copy(isBestChoice = false, percentMoreExpensive = 0.0)
            }
            return Pair(nonComparedResults, true)
        }

        val minPrice = rawCalculated.minOfOrNull { it.unitPrice } ?: 0.0

        val comparedResults = rawCalculated.map { item ->
            val isBest = item.unitPrice == minPrice
            val percentDiff = if (minPrice > 0.0 && !isBest) {
                ((item.unitPrice - minPrice) / minPrice) * 100.0
            } else {
                0.0
            }
            item.copy(
                isBestChoice = isBest,
                percentMoreExpensive = percentDiff
            )
        }

        return Pair(comparedResults, false)
    }
}