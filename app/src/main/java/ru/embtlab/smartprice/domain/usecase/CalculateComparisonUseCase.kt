package ru.embtlab.smartprice.domain.usecase

import ru.embtlab.smartprice.domain.model.CalculatedItem
import ru.embtlab.smartprice.domain.model.ProductItem

class CalculateComparisonUseCase {

    operator fun invoke(items: List<ProductItem>): Pair<List<CalculatedItem>, Boolean> {
        val validItems = items.filter { it.price > 0.0 && it.quantity > 0.0 }
        if (validItems.isEmpty()) return Pair(emptyList(), false)

        // 1. Считаем цену за базовую единицу для всех заполненных товаров
        val rawCalculated = validItems.map { item ->
            val effectivePrice = item.price * (1.0 - item.effectiveDiscountPercent / 100.0)
            val baseQuantity = item.quantity * item.unit.factorToBase
            val unitPrice = if (baseQuantity > 0.0) effectivePrice / baseQuantity else 0.0
            CalculatedItem(product = item, unitPrice = unitPrice)
        }

        // Если заполнен только 1 товар — просто показываем его цену за единицу БЕЗ плашки "ВЫГОДНО"
        if (validItems.size < 2) {
            val singleItemResults = rawCalculated.map {
                it.copy(isBestChoice = false, percentMoreExpensive = 0.0)
            }
            return Pair(singleItemResults, false)
        }

        // 2. Проверка несовместимых категорий (например, кг и л)
        val uniqueCategories = validItems.map { it.unit.category }.distinct()
        val hasIncompatibleUnits = uniqueCategories.size > 1
        if (hasIncompatibleUnits) {
            val nonComparedResults = rawCalculated.map {
                it.copy(isBestChoice = false, percentMoreExpensive = 0.0)
            }
            return Pair(nonComparedResults, true)
        }

        // 3. Вычисляем победителя (только когда товаров 2 и более)
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