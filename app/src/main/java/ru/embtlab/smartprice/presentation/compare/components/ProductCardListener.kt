package ru.embtlab.smartprice.presentation.compare.components

import androidx.compose.runtime.Immutable
import ru.embtlab.smartprice.domain.model.DiscountType
import ru.embtlab.smartprice.domain.model.ProductUnit

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
}