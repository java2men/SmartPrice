package ru.embtlab.smartprice.presentation.compare

import ru.embtlab.smartprice.domain.model.product.CalculatedItem
import ru.embtlab.smartprice.domain.model.settings.CardStylePreset
import ru.embtlab.smartprice.domain.model.product.ComparisonConfig
import ru.embtlab.smartprice.domain.model.product.ProductItem

data class CompareUiState(
    val items: List<ProductItem> = listOf(
        ProductItem(name = "Товар 1"),
        ProductItem(name = "Товар 2")
    ),
    val results: List<CalculatedItem> = emptyList(),
    val hasIncompatibleUnits: Boolean = false,
    val cardStyle: CardStylePreset = CardStylePreset.CLASSIC,
    val isSettingsLoaded: Boolean = false,
    val isVoiceInputEnabled: Boolean = true,
    val isOcrScannerEnabled: Boolean = true,
    val isHistorySheetOpen: Boolean = false,
    val isSaveDialogOpen: Boolean = false,
    val saveDialogTitleInput: String = "",
    val scanningProductId: String? = null,
    val isVoiceRecordingActive: Boolean = false,
    val focusedCardId: String? = null
) {
    val canAddMore: Boolean
        get() = items.size < ComparisonConfig.MAX_PRODUCTS_LIMIT
}