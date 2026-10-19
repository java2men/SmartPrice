package ru.embtlab.smartprice.presentation.compare.components.common

import androidx.compose.runtime.Immutable
import ru.embtlab.smartprice.domain.model.product.DiscountType
import ru.embtlab.smartprice.domain.model.product.ProductUnit

@Immutable
interface ProductCardListener {
    fun onNameChange(id: String, name: String)
    fun onPriceChange(id: String, price: String)
    fun onQuantityChange(id: String, quantity: String)
    fun onUnitChange(id: String, unit: ProductUnit)
    fun onDiscountTypeChange(id: String, type: DiscountType)
    fun onCustomDiscountChange(id: String, percent: String)
    fun onScanClick(id: String)
    fun onDelete(id: String)
    fun onCardFocused(id: String)
    fun onEditingFinished(id: String) {} // Вызывается при нажатии Done на клавиатуре
}