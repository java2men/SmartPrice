package ru.embtlab.smartprice.presentation.compare

import ru.embtlab.smartprice.domain.model.CalculatedItem
import ru.embtlab.smartprice.domain.model.CardStylePreset
import ru.embtlab.smartprice.domain.model.ComparisonConfig
import ru.embtlab.smartprice.domain.model.ProductItem

data class CompareUiState(
    val items: List<ProductItem> = listOf(
        ProductItem(name = "Товар 1"),
        ProductItem(name = "Товар 2")
    ),
    val results: List<CalculatedItem> = emptyList(),
    val hasIncompatibleUnits: Boolean = false,
    val cardStyle: CardStylePreset = CardStylePreset.CLASSIC,
    val isHistorySheetOpen: Boolean = false,
    val isSaveDialogOpen: Boolean = false,
    val saveDialogTitleInput: String = "",
    val scanningProductId: String? = null
) {
    val canAddMore: Boolean
        get() = items.size < ComparisonConfig.MAX_PRODUCTS_LIMIT
}