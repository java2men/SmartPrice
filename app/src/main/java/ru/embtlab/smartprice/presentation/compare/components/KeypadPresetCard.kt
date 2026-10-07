package ru.embtlab.smartprice.presentation.compare.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.embtlab.smartprice.domain.model.CalculatedItem
import ru.embtlab.smartprice.domain.model.DiscountType
import ru.embtlab.smartprice.domain.model.ProductItem
import ru.embtlab.smartprice.domain.model.ProductUnit
import ru.embtlab.smartprice.presentation.compare.util.InputFormatters

@Composable
fun KeypadPresetCard(
    item: ProductItem,
    calcResult: CalculatedItem?,
    canDelete: Boolean,
    listener: ProductCardListener,
    modifier: Modifier = Modifier,
    recentNames: List<String> = emptyList() // <-- Добавлен параметр
) {
    val focusManager = LocalFocusManager.current
    val quantityFocusRequester = remember { FocusRequester() }
    val isBest = calcResult?.isBestChoice == true
    val borderColor =
        if (isBest) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant

    val weightPresets = listOf("180", "400", "800", "900", "1000")
    val volumePresets = listOf("450", "900", "1000", "1500")
    val activePresets =
        if (item.unit == ProductUnit.MILLILITER || item.unit == ProductUnit.LITER) volumePresets else weightPresets

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(if (isBest) 2.dp else 1.dp, borderColor),
        colors = CardDefaults.cardColors(
            containerColor = if (isBest) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
            else MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            val isFilled = item.priceInput.isNotBlank() ||
                    item.quantityInput.isNotBlank() ||
                    item.customDiscountPercentInput.isNotBlank() ||
                    item.discountType != DiscountType.NONE

            ProductHeader(
                id = item.id,
                name = item.name,
                canDelete = canDelete,
                isFilled = isFilled,
                listener = listener,
                recentNames = recentNames
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant,
                        RoundedCornerShape(12.dp)
                    )
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .weight(1.1f)
                        .fillMaxHeight()
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (item.priceInput.isEmpty()) {
                        Text(
                            "Цена",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                    BasicTextField(
                        value = item.priceInput,
                        onValueChange = {
                            listener.onPriceChange(
                                item.id,
                                InputFormatters.sanitizePrice(it, item.priceInput)
                            )
                        },
                        textStyle = TextStyle(
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(onNext = { quantityFocusRequester.requestFocus() }),
                        modifier = Modifier
                            .fillMaxWidth()
                            .onFocusChanged {
                                if (it.isFocused) listener.onCardFocused(item.id)
                            }
                    )
                }

                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .fillMaxHeight(0.6f)
                        .background(MaterialTheme.colorScheme.outlineVariant)
                )

                Row(
                    modifier = Modifier
                        .weight(1.3f)
                        .fillMaxHeight()
                        .padding(start = 12.dp, end = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (item.quantityInput.isEmpty()) {
                            Text(
                                "Кол-во",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                        BasicTextField(
                            value = item.quantityInput,
                            onValueChange = {
                                listener.onQuantityChange(
                                    item.id,
                                    InputFormatters.sanitizeQuantity(it, item.quantityInput)
                                )
                            },
                            textStyle = TextStyle(
                                fontSize = 17.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Decimal,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(quantityFocusRequester)
                                .onFocusChanged {
                                    if (it.isFocused) listener.onCardFocused(item.id)
                                }
                        )
                    }
                    UnitDropdownMenu(
                        selectedUnit = item.unit,
                        onUnitSelect = { listener.onUnitChange(item.id, it) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Пресеты значений фасовок
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                activePresets.forEach { presetVal ->
                    SuggestionChip(
                        onClick = {
                            listener.onQuantityChange(item.id, presetVal)
                            if (presetVal == "1000" && item.unit == ProductUnit.GRAM) {
                                listener.onUnitChange(item.id, ProductUnit.KILOGRAM)
                                listener.onQuantityChange(item.id, "1")
                            }
                        },
                        label = {
                            Text(
                                "$presetVal ${item.unit.label}",
                                style = MaterialTheme.typography.labelSmall
                            )
                        },
                        modifier = Modifier.focusProperties { canFocus = false }
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            DiscountSelectorRow(item = item, listener = listener)
            ProductCalculationFooter(unit = item.unit, calcResult = calcResult)
        }
    }
}