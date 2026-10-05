package ru.embtlab.smartprice.presentation.compare

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import ru.embtlab.smartprice.data.local.SettingsDataStore
import ru.embtlab.smartprice.domain.model.*
import ru.embtlab.smartprice.domain.repository.ComparisonHistoryRepository
import ru.embtlab.smartprice.domain.usecase.CalculateComparisonUseCase
import ru.embtlab.smartprice.domain.usecase.GetHistoryUseCase
import ru.embtlab.smartprice.domain.usecase.SaveComparisonUseCase
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@HiltViewModel
class CompareViewModel @Inject constructor(
    private val calculateComparisonUseCase: CalculateComparisonUseCase,
    private val getHistoryUseCase: GetHistoryUseCase,
    private val saveComparisonUseCase: SaveComparisonUseCase,
    private val repository: ComparisonHistoryRepository,
    private val settingsDataStore: SettingsDataStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(CompareUiState())
    val uiState = _uiState.asStateFlow()

    val history: StateFlow<List<SavedComparison>> = getHistoryUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private var lastBackupState: List<ProductItem>? = null

    init {
        viewModelScope.launch {
            settingsDataStore.settingsFlow.collect { settings ->
                _uiState.update { it.copy(cardStyle = settings.cardPreset) }
            }
        }
    }

    fun onPriceChanged(productId: String, newPrice: String) {
        updateProduct(productId) { it.copy(priceInput = newPrice) }
    }

    fun onQuantityChanged(productId: String, newQuantity: String) {
        updateProduct(productId) { it.copy(quantityInput = newQuantity) }
    }

    fun onUnitChanged(productId: String, newUnit: ProductUnit) {
        updateProduct(productId) { it.copy(unit = newUnit) }
    }

    fun onDiscountTypeChanged(productId: String, newType: DiscountType) {
        updateProduct(productId) { it.copy(discountType = newType) }
    }

    fun onCustomDiscountChanged(productId: String, newPercent: String) {
        updateProduct(productId) { it.copy(customDiscountPercentInput = newPercent) }
    }

    fun onNameChanged(productId: String, newName: String) {
        updateProduct(productId) { it.copy(name = newName) }
    }

    fun addProduct() {
        if (_uiState.value.items.size >= ComparisonConfig.MAX_PRODUCTS_LIMIT) return

        val nextIndex = _uiState.value.items.size + 1
        val newItem = ProductItem(name = "Товар $nextIndex")
        _uiState.update { state ->
            val updatedList = state.items + newItem
            val (results, hasConflict) = calculateComparisonUseCase(updatedList)
            state.copy(items = updatedList, results = results, hasIncompatibleUnits = hasConflict)
        }
    }

    fun removeProduct(productId: String) {
        if (_uiState.value.items.size <= 2) return
        _uiState.update { state ->
            val updatedList = state.items.filterNot { it.id == productId }
            val (results, hasConflict) = calculateComparisonUseCase(updatedList)
            state.copy(items = updatedList, results = results, hasIncompatibleUnits = hasConflict)
        }
    }

    fun reset() {
        _uiState.value = CompareUiState(cardStyle = _uiState.value.cardStyle)
    }

    fun setHistorySheetVisible(isOpen: Boolean) {
        _uiState.update { it.copy(isHistorySheetOpen = isOpen) }
    }

    fun startScanning(productId: String) {
        _uiState.update { it.copy(scanningProductId = productId) }
    }

    fun stopScanning() {
        _uiState.update { it.copy(scanningProductId = null) }
    }

    fun openSaveDialog() {
        val names = _uiState.value.items.mapNotNull { it.name.ifBlank { null } }
        val defaultTitle = if (names.isNotEmpty()) names.joinToString(" vs ") else ""
        _uiState.update {
            it.copy(isSaveDialogOpen = true, saveDialogTitleInput = defaultTitle)
        }
    }

    fun onSaveDialogTitleChanged(newTitle: String) {
        _uiState.update { it.copy(saveDialogTitleInput = newTitle) }
    }

    fun dismissSaveDialog() {
        _uiState.update { it.copy(isSaveDialogOpen = false) }
    }

    fun confirmSaveComparison() {
        val title = _uiState.value.saveDialogTitleInput
        saveCurrentComparison(title)
        _uiState.update { it.copy(isSaveDialogOpen = false) }
    }

    fun restoreComparison(record: SavedComparison) {
        if (record.items.isEmpty()) return

        val (results, hasConflict) = calculateComparisonUseCase(record.items)
        _uiState.update { state ->
            state.copy(
                items = record.items,
                results = results,
                hasIncompatibleUnits = hasConflict,
                isHistorySheetOpen = false // Сворачиваем шторку после восстановления
            )
        }
    }

    private fun saveCurrentComparison(customTitle: String? = null) {
        val best = _uiState.value.results.find { it.isBestChoice } ?: return
        val secondBest = _uiState.value.results.filterNot { it.isBestChoice }.minByOrNull { it.unitPrice }

        // Берем базовую единицу напрямую из категории единицы измерения:
        val baseUnit = best.product.unit.category.baseLabel

        val savings = if (secondBest != null && secondBest.percentMoreExpensive > 0) {
            String.format(Locale.US, "Выгода %.1f%%", secondBest.percentMoreExpensive)
        } else {
            "Выгодная позиция"
        }

        val defaultTitle = "Сравнение " + SimpleDateFormat("dd.MM HH:mm", Locale.getDefault()).format(Date())
        val finalTitle = if (!customTitle.isNullOrBlank()) customTitle else defaultTitle

        val record = SavedComparison(
            title = finalTitle,
            bestProductName = best.product.name.ifBlank { "Товар" },
            bestUnitPriceFormatted = String.format(Locale.US, "%.2f ₽ / %s", best.unitPrice, baseUnit),
            savingsInfo = savings,
            items = _uiState.value.items
        )

        viewModelScope.launch {
            saveComparisonUseCase(record)
        }
    }

    fun deleteHistoryItem(id: Long) {
        viewModelScope.launch {
            repository.deleteComparison(id)
        }
    }

    private fun updateProduct(productId: String, transform: (ProductItem) -> ProductItem) {
        _uiState.update { state ->
            val updatedList = state.items.map { item ->
                if (item.id == productId) transform(item) else item
            }
            val (results, hasConflict) = calculateComparisonUseCase(updatedList)
            state.copy(items = updatedList, results = results, hasIncompatibleUnits = hasConflict)
        }
    }

    fun onTrashClick(productId: String) {
        val item = _uiState.value.items.find { it.id == productId } ?: return
        val isFilled = item.priceInput.isNotBlank() ||
                item.quantityInput.isNotBlank() ||
                item.customDiscountPercentInput.isNotBlank() ||
                item.discountType != DiscountType.NONE

        if (isFilled) {
            // Шаг 1: если в карточке есть данные — сбрасываем их
            updateProduct(productId) {
                it.copy(
                    priceInput = "",
                    quantityInput = "",
                    customDiscountPercentInput = "",
                    discountType = DiscountType.NONE
                )
            }
        } else {
            // Шаг 2: если карточка уже пуста и товаров больше двух — удаляем её
            if (_uiState.value.items.size > 2) {
                removeProduct(productId)
            }
        }
    }

    fun resetWithBackup() {
        // Сохраняем снимок текущих карточек перед очисткой
        lastBackupState = _uiState.value.items

        // Сбрасываем к исходному состоянию из 2 пустых товаров
        _uiState.update { state ->
            CompareUiState(
                cardStyle = state.cardStyle,
                items = listOf(
                    ProductItem(name = "Товар 1"),
                    ProductItem(name = "Товар 2")
                )
            )
        }
    }

    fun undoReset() {
        val backup = lastBackupState ?: return
        val (results, hasConflict) = calculateComparisonUseCase(backup)
        _uiState.update { state ->
            state.copy(
                items = backup,
                results = results,
                hasIncompatibleUnits = hasConflict
            )
        }
        lastBackupState = null
    }

}