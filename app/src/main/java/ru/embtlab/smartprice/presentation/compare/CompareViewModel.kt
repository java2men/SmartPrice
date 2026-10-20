package ru.embtlab.smartprice.presentation.compare

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.embtlab.smartprice.core.analytics.AnalyticsEvents
import ru.embtlab.smartprice.core.analytics.AnalyticsManager
import ru.embtlab.smartprice.domain.model.history.SavedComparison
import ru.embtlab.smartprice.domain.model.product.ComparisonConfig
import ru.embtlab.smartprice.domain.model.product.DiscountType
import ru.embtlab.smartprice.domain.model.product.ProductConstants
import ru.embtlab.smartprice.domain.model.product.ProductItem
import ru.embtlab.smartprice.domain.model.product.ProductUnit
import ru.embtlab.smartprice.domain.repository.SettingsRepository
import ru.embtlab.smartprice.domain.usecase.comparison.CalculateComparisonUseCase
import ru.embtlab.smartprice.domain.usecase.comparison.DeleteComparisonUseCase
import ru.embtlab.smartprice.domain.usecase.comparison.GetHistoryUseCase
import ru.embtlab.smartprice.domain.usecase.comparison.SaveComparisonUseCase
import ru.embtlab.smartprice.domain.usecase.voice.ParseVoiceInputUseCase
import ru.embtlab.smartprice.domain.util.FuzzyMatcher
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class CompareViewModel @Inject constructor(
    private val calculateComparisonUseCase: CalculateComparisonUseCase,
    private val getHistoryUseCase: GetHistoryUseCase,
    private val saveComparisonUseCase: SaveComparisonUseCase,
    private val deleteComparisonUseCase: DeleteComparisonUseCase,
    private val parseVoiceInputUseCase: ParseVoiceInputUseCase,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CompareUiState())
    val uiState: StateFlow<CompareUiState> = _uiState.asStateFlow()

    val history: StateFlow<List<SavedComparison>> = getHistoryUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentProductNames: StateFlow<List<String>> = settingsRepository.recentProductNamesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private var lastBackupState: List<ProductItem>? = null
    private var nextProductNumber = ProductConstants.INITIAL_NEXT_PRODUCT_INDEX

    init {
        _uiState.update { it.copy(focusedCardId = it.items.firstOrNull()?.id) }
        viewModelScope.launch {
            settingsRepository.settingsFlow.collect { settings ->
                _uiState.update {
                    it.copy(
                        cardStyle = settings.cardPreset,
                        isVoiceInputEnabled = settings.isVoiceInputEnabled,
                        isOcrScannerEnabled = settings.isOcrScannerEnabled,
                        isSettingsLoaded = true,
                    )
                }
            }
        }
    }

    // ==========================================
    // УПРАВЛЕНИЕ ПОЛЯМИ КАРТОЧЕК
    // ==========================================

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
        if (newName.isNotBlank()) {
            viewModelScope.launch {
                settingsRepository.addRecentProductName(newName)
            }
        }
    }

    fun removeRecentName(name: String) {
        viewModelScope.launch {
            settingsRepository.removeRecentProductName(name)
        }
    }

    // ==========================================
    // ФОКУСИРОВКА
    // ==========================================

    fun onCardFocused(productId: String?) {
        _uiState.update { it.copy(focusedCardId = productId) }
    }

    fun advanceFocusIfCompleted(currentProductId: String) {
        val currentItems = _uiState.value.items
        val currentItem = currentItems.find { it.id == currentProductId } ?: return
        val isCardCompleted = currentItem.priceInput.isNotBlank() && currentItem.quantityInput.isNotBlank()
        if (isCardCompleted) {
            val nextEmptyId = findNextIncompleteCardId(currentItems, excludeId = currentProductId)
            _uiState.update { it.copy(focusedCardId = nextEmptyId) }
        }
    }

    private fun findNextIncompleteCardId(items: List<ProductItem>, excludeId: String?): String? {
        val currentIndex = items.indexOfFirst { it.id == excludeId }
        if (currentIndex != -1) {
            for (i in (currentIndex + 1) until items.size) {
                if (isIncomplete(items[i])) return items[i].id
            }
            for (i in 0 until currentIndex) {
                if (isIncomplete(items[i])) return items[i].id
            }
            return null
        }
        return items.firstOrNull { isIncomplete(it) }?.id
    }

    private fun isIncomplete(item: ProductItem): Boolean {
        return item.priceInput.isBlank() || item.quantityInput.isBlank()
    }

    // ==========================================
    // ОПЕРАЦИИ СО СПИСКОМ ТОВАРОВ
    // ==========================================

    fun addProduct() {
        if (_uiState.value.items.size >= ComparisonConfig.MAX_PRODUCTS_LIMIT) return
        val nextIndex = nextProductNumber++
        val newItem = ProductItem(name = "Товар $nextIndex")

        AnalyticsManager.trackEvent(
            AnalyticsEvents.PRODUCT_ADDED,
            mapOf(AnalyticsEvents.Params.ITEMS_COUNT to (_uiState.value.items.size + 1).toString())
        )

        _uiState.update { state ->
            val updatedList = state.items + newItem
            val (results, hasConflict) = calculateComparisonUseCase(updatedList)
            state.copy(
                items = updatedList,
                results = results,
                hasIncompatibleUnits = hasConflict,
                focusedCardId = newItem.id
            )
        }
    }

    fun removeProduct(productId: String) {
        if (_uiState.value.items.size <= ProductConstants.MIN_PRODUCTS_COUNT) return
        _uiState.update { state ->
            val updatedList = state.items.filterNot { it.id == productId }
            val (results, hasConflict) = calculateComparisonUseCase(updatedList)
            val newFocusedId = if (state.focusedCardId == productId) {
                findNextIncompleteCardId(updatedList, null)
            } else {
                state.focusedCardId
            }
            state.copy(
                items = updatedList,
                results = results,
                hasIncompatibleUnits = hasConflict,
                focusedCardId = newFocusedId
            )
        }
    }

    fun onTrashClick(productId: String) {
        val index = _uiState.value.items.indexOfFirst { it.id == productId }
        if (index == -1) return
        val item = _uiState.value.items[index]
        val defaultName = "Товар ${index + 1}"
        val isModified = isItemModified(item, index)
        if (isModified) {
            updateProduct(productId) {
                it.copy(
                    name = defaultName,
                    priceInput = "",
                    quantityInput = "",
                    customDiscountPercentInput = "",
                    discountType = DiscountType.NONE
                )
            }
            _uiState.update { it.copy(focusedCardId = productId) }
        } else {
            if (_uiState.value.items.size > ProductConstants.MIN_PRODUCTS_COUNT) {
                removeProduct(productId)
            }
        }
    }

    private fun isItemModified(item: ProductItem, index: Int): Boolean {
        val defaultName = "Товар ${index + 1}"
        val isNameChanged = item.name.isNotBlank() && !item.name.equals(defaultName, ignoreCase = true)
        val hasPriceOrQuantity = item.priceInput.isNotBlank() || item.quantityInput.isNotBlank()
        val hasDiscount = item.customDiscountPercentInput.isNotBlank() || item.discountType != DiscountType.NONE
        return isNameChanged || hasPriceOrQuantity || hasDiscount
    }

    fun reset() {
        _uiState.value = CompareUiState(cardStyle = _uiState.value.cardStyle)
    }

    fun resetWithBackup() {
        lastBackupState = _uiState.value.items
        nextProductNumber = ProductConstants.INITIAL_NEXT_PRODUCT_INDEX
        val initialItems = listOf(
            ProductItem(name = "Товар 1"),
            ProductItem(name = "Товар 2")
        )
        _uiState.update { state ->
            CompareUiState(
                cardStyle = state.cardStyle,
                items = initialItems,
                focusedCardId = initialItems.first().id
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
                hasIncompatibleUnits = hasConflict,
                focusedCardId = findNextIncompleteCardId(backup, null)
            )
        }
        lastBackupState = null
    }

    // ==========================================
    // OCR-СКАНЕР
    // ==========================================

    fun startScanning(productId: String) {
        _uiState.update { it.copy(scanningProductId = productId) }
    }

    fun stopScanning() {
        _uiState.update { it.copy(scanningProductId = null) }
    }

    // ==========================================
    // ГОЛОСОВОЙ ВВОД
    // ==========================================

    fun startPushToTalk(focusedCardId: String?) {
        val targetId = resolveTargetCardId(focusedCardId)
        AnalyticsManager.trackEvent(AnalyticsEvents.VOICE_INPUT_STARTED)
        _uiState.update {
            it.copy(
                isVoiceRecordingActive = true,
                focusedCardId = targetId
            )
        }
    }

    fun cancelPushToTalk() {
        _uiState.update {
            it.copy(isVoiceRecordingActive = false)
        }
    }

    fun onVoicePhraseCompleted(rawText: String) {
        if (rawText.isBlank()) {

            AnalyticsManager.trackEvent(
                AnalyticsEvents.VOICE_INPUT_CANCELLED,
                mapOf("reason" to "empty_speech")
            )

            cancelPushToTalk()
            return
        }
        val parsed = parseVoiceInputUseCase(rawText)
        var targetId = _uiState.value.focusedCardId
        val hasPrice = !parsed.price.isNullOrBlank()
        val hasQuantity = !parsed.quantity.isNullOrBlank()

        AnalyticsManager.trackEvent(
            AnalyticsEvents.VOICE_INPUT_COMPLETED,
            mapOf(
                AnalyticsEvents.Params.SUCCESS to (hasPrice && hasQuantity).toString(),
                AnalyticsEvents.Params.HAS_DISCOUNT to (parsed.discountType != null).toString(),
                AnalyticsEvents.Params.HAS_CUSTOM_NAME to (!parsed.name.isNullOrBlank()).toString()
            )
        )

        if (!parsed.name.isNullOrBlank()) {
            val matchedItem = _uiState.value.items.find { item ->
                FuzzyMatcher.similarity(item.name, parsed.name) >= 0.72
            }
            if (matchedItem != null) {
                targetId = matchedItem.id
            }
        }

        if (targetId == null) {
            if (_uiState.value.items.size < ComparisonConfig.MAX_PRODUCTS_LIMIT) {
                val nextIndex = nextProductNumber++
                val newItem = ProductItem(name = parsed.name ?: "Товар $nextIndex")
                val updatedList = _uiState.value.items + newItem
                val (results, hasConflict) = calculateComparisonUseCase(updatedList)
                _uiState.update {
                    it.copy(items = updatedList, results = results, hasIncompatibleUnits = hasConflict)
                }
                targetId = newItem.id
            } else {
                targetId = _uiState.value.items.lastOrNull()?.id
            }
        }

        targetId?.let { id ->
            parsed.name?.let { onNameChanged(id, it) }
            parsed.price?.let { onPriceChanged(id, it) }
            parsed.quantity?.let { onQuantityChanged(id, it) }
            parsed.unit?.let { onUnitChanged(id, it) }
            parsed.discountType?.let { onDiscountTypeChanged(id, it) }
            parsed.discountPercent?.let { onCustomDiscountChanged(id, it) }
            advanceFocusIfCompleted(id)
        }
        _uiState.update {
            it.copy(isVoiceRecordingActive = false)
        }
    }

    private fun resolveTargetCardId(focusedCardId: String?): String? {
        val items = _uiState.value.items
        if (!focusedCardId.isNullOrBlank() && items.any { it.id == focusedCardId }) {
            return focusedCardId
        }
        if (items.size < ComparisonConfig.MAX_PRODUCTS_LIMIT) {
            return null
        }
        return items.lastOrNull()?.id
    }

    // ==========================================
    // ИСТОРИЯ
    // ==========================================

    fun setHistorySheetVisible(isOpen: Boolean) {
        _uiState.update { it.copy(isHistorySheetOpen = isOpen) }
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
        val best = _uiState.value.results.find { it.isBestChoice }

        AnalyticsManager.trackEvent(
            AnalyticsEvents.COMPARISON_SAVED,
            mapOf(
                AnalyticsEvents.Params.ITEMS_COUNT to _uiState.value.items.size.toString(),
                AnalyticsEvents.Params.UNIT_CATEGORY to (best?.product?.unit?.category?.name ?: "UNKNOWN")
            )
        )

        saveCurrentComparison(title)
        _uiState.update { it.copy(isSaveDialogOpen = false) }
    }

    fun restoreComparison(record: SavedComparison) {
        if (record.items.isEmpty()) return

        AnalyticsManager.trackEvent(
            AnalyticsEvents.HISTORY_RESTORED,
            mapOf(AnalyticsEvents.Params.ITEMS_COUNT to record.items.size.toString())
        )

        val (results, hasConflict) = calculateComparisonUseCase(record.items)
        _uiState.update { state ->
            state.copy(
                items = record.items,
                results = results,
                hasIncompatibleUnits = hasConflict,
                isHistorySheetOpen = false,
                focusedCardId = findNextIncompleteCardId(record.items, null)
            )
        }
    }

    private fun saveCurrentComparison(customTitle: String? = null) {
        val best = _uiState.value.results.find { it.isBestChoice } ?: return
        val secondBest = _uiState.value.results.filterNot { it.isBestChoice }.minByOrNull { it.unitPrice }
        val baseUnit = best.product.unit.category.baseLabel
        val savings = if (secondBest != null && secondBest.percentMoreExpensive > 0) {
            String.format(Locale.US, "Выгоднее на %.1f%%", secondBest.percentMoreExpensive)
        } else {
            ""
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
            deleteComparisonUseCase(id) // <-- Теперь через Use Case
        }
    }

    private fun updateProduct(productId: String, transform: (ProductItem) -> ProductItem) {
        _uiState.update { state ->
            val updatedList = state.items.map { item ->
                if (item.id == productId) transform(item) else item
            }
            val (results, hasConflict) = calculateComparisonUseCase(updatedList)

            // Аналитика: предупреждение о несовместимости мер
            if (hasConflict && !state.hasIncompatibleUnits) {
                AnalyticsManager.trackEvent(AnalyticsEvents.INCOMPATIBLE_UNITS_WARNING)
            }

            // Аналитика: успешный расчет дельты цен
            val best = results.find { it.isBestChoice }
            val secondBest = results.filterNot { it.isBestChoice }.minByOrNull { it.unitPrice }
            if (best != null && secondBest != null && !hasConflict) {
                AnalyticsManager.trackEvent(
                    AnalyticsEvents.COMPARISON_CALCULATED,
                    mapOf(
                        AnalyticsEvents.Params.ITEMS_COUNT to updatedList.size.toString(),
                        AnalyticsEvents.Params.UNIT_CATEGORY to best.product.unit.category.name,
                        AnalyticsEvents.Params.SAVINGS_PERCENT to String.format(Locale.US, "%.1f", secondBest.percentMoreExpensive)
                    )
                )
            }

            state.copy(items = updatedList, results = results, hasIncompatibleUnits = hasConflict)
        }
    }
}