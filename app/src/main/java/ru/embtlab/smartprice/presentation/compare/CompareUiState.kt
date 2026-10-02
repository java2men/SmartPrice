package ru.embtlab.smartprice.presentation.compare

import ru.embtlab.smartprice.domain.model.CalculatedItem
import ru.embtlab.smartprice.domain.model.ProductItem
import ru.embtlab.smartprice.domain.model.ProductUnit

data class CompareUiState(
    // Бизнес-данные
    val items: List<ProductItem> = listOf(
        ProductItem(name = "Товар 1", unit = ProductUnit.GRAM),
        ProductItem(name = "Товар 2", unit = ProductUnit.GRAM)
    ),
    val results: List<CalculatedItem> = emptyList(),
    val hasIncompatibleUnits: Boolean = false,

    // Состояние экранов, диалогов и шторок
    val isHistorySheetOpen: Boolean = false,
    val scanningProductId: String? = null,
    val isSaveDialogOpen: Boolean = false,
    val saveDialogTitleInput: String = ""
)