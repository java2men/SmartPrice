// presentation/compare/CompareViewModel.kt
package ru.embtlab.smartprice.presentation.compare

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import ru.embtlab.smartprice.data.local.AppDatabase
import ru.embtlab.smartprice.data.repository.ComparisonHistoryRepositoryImpl
import ru.embtlab.smartprice.domain.model.*
import ru.embtlab.smartprice.domain.usecase.CalculateComparisonUseCase
import java.text.SimpleDateFormat
import java.util.*

class CompareViewModel(application: Application) : AndroidViewModel(application) {

    private val calculateComparisonUseCase = CalculateComparisonUseCase()
    private val database = AppDatabase.getDatabase(application)
    private val repository = ComparisonHistoryRepositoryImpl(database.historyDao())

    private val _uiState = MutableStateFlow(CompareUiState())
    val uiState = _uiState.asStateFlow()

    // Поток истории для экрана/шторки истории
    val history: StateFlow<List<SavedComparison>> = repository.getAllHistory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val settingsDataStore = ru.embtlab.smartprice.data.local.SettingsDataStore(application)

    init {
        // Подтягиваем пресет карточки из сохраненных настроек
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

    fun addProduct() {
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
        _uiState.value = CompareUiState()
    }

    // Сохранение текущего расчета в Room
    fun saveCurrentComparison(customTitle: String? = null) {
        val best = _uiState.value.results.find { it.isBestChoice } ?: return
        val secondBest = _uiState.value.results.filterNot { it.isBestChoice }.minByOrNull { it.unitPrice }

        val baseUnit = when (best.product.unit.category) {
            UnitCategory.WEIGHT -> "кг"
            UnitCategory.VOLUME -> "л"
            UnitCategory.PIECES -> "шт"
        }

        val savings = if (secondBest != null && secondBest.percentMoreExpensive > 0) {
            String.format(Locale.US, "Выгоднее на %.1f%%", secondBest.percentMoreExpensive)
        } else {
            "Выгодная покупка"
        }

        val defaultTitle = "Сравнение " + SimpleDateFormat("dd.MM HH:mm", Locale.getDefault()).format(Date())

        // Вот эта строка в исправленном виде:
        val finalTitle = if (!customTitle.isNullOrBlank()) customTitle else defaultTitle

        val record = SavedComparison(
            title = finalTitle,
            bestProductName = best.product.name.ifBlank { "Товар" },
            bestUnitPriceFormatted = String.format(Locale.US, "%.2f ₽ / %s", best.unitPrice, baseUnit),
            savingsInfo = savings
        )

        viewModelScope.launch {
            repository.saveComparison(record)
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

    fun onNameChanged(productId: String, newName: String) {
        updateProduct(productId) { it.copy(name = newName) }
    }

    // Управление шторкой истории
    fun setHistorySheetVisible(isOpen: Boolean) {
        _uiState.update { it.copy(isHistorySheetOpen = isOpen) }
    }

    // Управление камерой
    fun startScanning(productId: String) {
        _uiState.update { it.copy(scanningProductId = productId) }
    }

    fun stopScanning() {
        _uiState.update { it.copy(scanningProductId = null) }
    }

    // Управление диалогом сохранения
    fun openSaveDialog() {
        val names = _uiState.value.items.mapNotNull { it.name.ifBlank { null } }
        val defaultTitle = if (names.isNotEmpty()) names.joinToString(" vs ") else ""
        _uiState.update {
            it.copy(
                isSaveDialogOpen = true,
                saveDialogTitleInput = defaultTitle
            )
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

    // presentation/compare/CompareViewModel.kt
    fun setCardStyle(preset: CardStylePreset) {
        _uiState.update { it.copy(cardStyle = preset, isSettingsDialogOpen = false) }
        // При желании здесь можно сохранить выбор в SharedPreferences или DataStore
    }

    fun openSettingsDialog() {
        _uiState.update { it.copy(isSettingsDialogOpen = true) }
    }

    fun dismissSettingsDialog() {
        _uiState.update { it.copy(isSettingsDialogOpen = false) }
    }

}