// presentation/compare/components/ProductCard.kt
package ru.embtlab.smartprice.presentation.compare.components

import android.app.Activity
import android.content.Context
import android.view.inputmethod.InputMethodManager
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.embtlab.smartprice.domain.model.CalculatedItem
import ru.embtlab.smartprice.domain.model.DiscountType
import ru.embtlab.smartprice.domain.model.ProductItem
import ru.embtlab.smartprice.presentation.compare.util.InputFormatters

@Composable
fun ProductCard(
    item: ProductItem,
    calcResult: CalculatedItem?,
    canDelete: Boolean,
    listener: ProductCardListener,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val currentView = LocalView.current

    val priceFocusRequester = remember { FocusRequester() }
    val quantityFocusRequester = remember { FocusRequester() }
    val discountFocusRequester = remember { FocusRequester() }
    val dummyFocusRequester = remember { FocusRequester() }

    val isDiscountOpen = item.discountType == DiscountType.PERCENT

    val isBest = calcResult?.isBestChoice == true
    val borderColor =
        if (isBest) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant

    var hasActiveFocusInCard by remember { mutableStateOf(false) }

    val dismissKeyboardAndLockFocus = {
        hasActiveFocusInCard = false
        dummyFocusRequester.requestFocus()
        focusManager.clearFocus(force = true)
        keyboardController?.hide()
        val imm = currentView.context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.hideSoftInputFromWindow(currentView.windowToken, 0)
        (currentView.context as? Activity)?.currentFocus?.clearFocus()
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .focusProperties {
                onExit = { FocusRequester.Cancel }
            },
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
            // Ловушка фокуса
            Box(
                modifier = Modifier
                    .size(0.dp)
                    .focusRequester(dummyFocusRequester)
                    .focusable()
            )

            val isFilled = item.name.isNotBlank() ||
                    item.priceInput.isNotBlank() ||
                    item.quantityInput.isNotBlank() ||
                    item.customDiscountPercentInput.isNotBlank() ||
                    item.discountType != DiscountType.NONE

            // 1. ИМЯ ТОВАРА
            ProductHeader(
                id = item.id,
                name = item.name,
                canDelete = canDelete,
                listener = listener,
                isFilled = isFilled,
                priceFocusRequester = priceFocusRequester,
                onFocused = {
                    if (!hasActiveFocusInCard) {
                        hasActiveFocusInCard = true
                        listener.onCardFocused(item.id)
                    }
                }
            )

            Spacer(modifier = Modifier.height(10.dp))

            // 2. ЦЕНА И КОЛИЧЕСТВО
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
                // Поле цены
                Box(
                    modifier = Modifier
                        .weight(1.1f)
                        .fillMaxHeight()
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (item.priceInput.isEmpty()) {
                        Text(
                            text = "Цена",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
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
                            keyboardActions = KeyboardActions(
                                onNext = {
                                    quantityFocusRequester.requestFocus()
                                }
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .focusRequester(priceFocusRequester)
                                .onFocusChanged { focusState ->
                                    if (focusState.isFocused) {
                                        if (!hasActiveFocusInCard) {
                                            hasActiveFocusInCard = true
                                            listener.onCardFocused(item.id)
                                        }
                                    }
                                }
                        )
                        if (item.priceInput.isNotEmpty()) {
                            Text(
                                text = "₽",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }

                // Разделитель
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .fillMaxHeight(0.6f)
                        .background(MaterialTheme.colorScheme.outlineVariant)
                )

                // Поле количества
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
                                text = "Кол-во",
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
                            // Если процент раскрыт — переходим дальше, иначе завершаем ввод
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Decimal,
                                imeAction = if (isDiscountOpen) ImeAction.Next else ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onNext = {
                                    if (isDiscountOpen) {
                                        discountFocusRequester.requestFocus()
                                    }
                                },
                                onDone = {
                                    dismissKeyboardAndLockFocus()
                                }
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(quantityFocusRequester)
                                .focusProperties {
                                    if (!isDiscountOpen) {
                                        next = dummyFocusRequester
                                        down = dummyFocusRequester
                                    }
                                    onExit = { FocusRequester.Cancel }
                                }
                                .onFocusChanged { focusState ->
                                    if (focusState.isFocused) {
                                        if (!hasActiveFocusInCard) {
                                            hasActiveFocusInCard = true
                                            listener.onCardFocused(item.id)
                                        }
                                    } else if (!isDiscountOpen) {
                                        hasActiveFocusInCard = false
                                    }
                                }
                        )
                    }

                    UnitDropdownMenu(
                        selectedUnit = item.unit,
                        onUnitSelect = { listener.onUnitChange(item.id, it) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 3. БЛОК СКИДКИ
            DiscountSelectorRow(
                item = item,
                listener = listener,
                discountFocusRequester = discountFocusRequester,
                dummyFocusRequester = dummyFocusRequester,
                onFocused = {
                    if (!hasActiveFocusInCard) {
                        hasActiveFocusInCard = true
                        listener.onCardFocused(item.id)
                    }
                },
                onDoneAction = {
                    dismissKeyboardAndLockFocus()
                }
            )

            // 4. ИТОГИ
            ProductCalculationFooter(unit = item.unit, calcResult = calcResult)
        }
    }
}