package ru.embtlab.smartprice.presentation.compare.components

import android.view.KeyEvent as AndroidKeyEvent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
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
import ru.embtlab.smartprice.presentation.compare.util.KeyboardHelper

@Composable
fun SmartSingleFieldCard(
    item: ProductItem,
    calcResult: CalculatedItem?,
    canDelete: Boolean,
    listener: ProductCardListener,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val currentView = LocalView.current

    val fieldFocusRequester = remember { FocusRequester() }
    val discountFocusRequester = remember { FocusRequester() }
    val dummyFocusRequester = remember { FocusRequester() }

    val isDiscountOpen = item.discountType == DiscountType.PERCENT
    val isBest = calcResult?.isBestChoice == true
    val borderColor =
        if (isBest) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant

    val dismissKeyboardAndFocus = {
        dummyFocusRequester.requestFocus()
        focusManager.clearFocus(force = true)
        keyboardController?.hide()
        KeyboardHelper.forceClose(currentView)
    }

    var isFieldFocused by remember { mutableStateOf(false) }

    // Локальное строковое состояние для бесшовного ввода
    var rawText by remember {
        mutableStateOf(
            if (item.priceInput.isNotEmpty() && item.quantityInput.isNotEmpty())
                "${item.priceInput} / ${item.quantityInput}"
            else item.priceInput
        )
    }

    // Синхронизируем строку, только когда поле не в фокусе (например, при сбросе или сканировании ценника)
    LaunchedEffect(item.priceInput, item.quantityInput, isFieldFocused) {
        if (!isFieldFocused) {
            rawText = if (item.priceInput.isNotEmpty() && item.quantityInput.isNotEmpty()) {
                "${item.priceInput} / ${item.quantityInput}"
            } else if (item.priceInput.isNotEmpty()) {
                item.priceInput
            } else {
                ""
            }
        }
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
            // Нода-ловушка фокуса при закрытии ввода
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

            // 1. Заголовок товара с переименованием и чипсами как в классическом пресете
            ProductHeader(
                id = item.id,
                name = item.name,
                canDelete = canDelete,
                isFilled = isFilled,
                listener = listener
            )

            Spacer(modifier = Modifier.height(10.dp))

            // 2. Строка экспресс-ввода: только цифры, точка, запятая, слэш и пробел
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
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (rawText.isEmpty()) {
                        Text(
                            text = "Цена и кол-во (напр. 189 850)",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                    BasicTextField(
                        value = rawText,
                        onValueChange = { input ->
                            // 1. Разрешаем строго цифры, разделители (. , / пробел)
                            val filtered = input.filter { it.isDigit() || it == '.' || it == ',' || it == ' ' || it == '/' }
                            rawText = filtered

                            // 2. Разбираем строку на токены без затирания при вводе разделителя
                            val tokens = filtered.split(Regex("""[\s/]+""")).filter { it.isNotEmpty() }

                            if (tokens.isEmpty()) {
                                listener.onPriceChange(item.id, "")
                                listener.onQuantityChange(item.id, "")
                            } else {
                                // Первая часть — цена
                                val cleanPrice = InputFormatters.sanitizePrice(tokens[0], item.priceInput)
                                listener.onPriceChange(item.id, cleanPrice)

                                // Вторая часть — количество
                                if (tokens.size > 1) {
                                    val cleanQuantity = InputFormatters.sanitizeQuantity(tokens[1], item.quantityInput)
                                    listener.onQuantityChange(item.id, cleanQuantity)
                                } else {
                                    // Если второго числа еще нет, сбрасываем количество, не стирая цену
                                    listener.onQuantityChange(item.id, "")
                                }
                            }
                        },
                        textStyle = TextStyle(
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        singleLine = true,
                        // KeyboardType.Number / Phone открывает строго цифровую клавиатуру
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Phone,
                            imeAction = if (isDiscountOpen) ImeAction.Next else ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = {
                                if (isDiscountOpen) {
                                    discountFocusRequester.requestFocus()
                                } else {
                                    dismissKeyboardAndFocus()
                                }
                            },
                            onDone = {
                                dismissKeyboardAndFocus()
                            }
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(fieldFocusRequester)
                            .focusProperties {
                                if (!isDiscountOpen) {
                                    next = dummyFocusRequester
                                    down = dummyFocusRequester
                                }
                                onExit = { FocusRequester.Cancel }
                            }
                            .onFocusChanged { focusState ->
                                isFieldFocused = focusState.isFocused
                                if (focusState.isFocused) {
                                    listener.onCardFocused(item.id)
                                }
                            }
                            .onKeyEvent { keyEvent ->
                                if (keyEvent.type == KeyEventType.KeyUp &&
                                    (keyEvent.key == Key.Enter || keyEvent.key == Key.NumPadEnter ||
                                            keyEvent.nativeKeyEvent.keyCode == AndroidKeyEvent.KEYCODE_NUMPAD_ENTER)
                                ) {
                                    if (!isDiscountOpen) {
                                        dismissKeyboardAndFocus()
                                        return@onKeyEvent true
                                    }
                                }
                                false
                            }
                    )
                }

                UnitDropdownMenu(
                    selectedUnit = item.unit,
                    onUnitSelect = { listener.onUnitChange(item.id, it) }
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 3. Выбор скидки
            DiscountSelectorRow(
                item = item,
                listener = listener,
                discountFocusRequester = discountFocusRequester,
                dummyFocusRequester = dummyFocusRequester,
                onFocused = {
                    listener.onCardFocused(item.id)
                },
                onDoneAction = {
                    dismissKeyboardAndFocus()
                }
            )

            // 4. Футер расчета
            ProductCalculationFooter(unit = item.unit, calcResult = calcResult)
        }
    }
}