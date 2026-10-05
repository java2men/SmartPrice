package ru.embtlab.smartprice.presentation.compare

import androidx.compose.runtime.Immutable
import ru.embtlab.smartprice.domain.model.CalculatedItem
import ru.embtlab.smartprice.domain.model.CardStylePreset
import ru.embtlab.smartprice.domain.model.ProductItem
import ru.embtlab.smartprice.domain.model.ProductUnit

// presentation/compare/CompareUiState.kt
@Immutable
data class CompareUiState(
    val items: List<ProductItem> = listOf(
        ProductItem(name = "Товар 1", unit = ProductUnit.GRAM),
        ProductItem(name = "Товар 2", unit = ProductUnit.GRAM)
    ),
    val results: List<CalculatedItem> = emptyList(),
    val hasIncompatibleUnits: Boolean = false,
    val cardStyle: CardStylePreset = CardStylePreset.CLASSIC, // <-- Текущий пресет
    val isSettingsDialogOpen: Boolean = false,                // Диалог выбора стиля
    val isHistorySheetOpen: Boolean = false,
    val scanningProductId: String? = null,
    val isSaveDialogOpen: Boolean = false,
    val saveDialogTitleInput: String = ""
)