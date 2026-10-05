package ru.embtlab.smartprice.presentation.compare.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.embtlab.smartprice.domain.model.CalculatedItem
import ru.embtlab.smartprice.domain.model.ProductItem

@Composable
fun SmartSingleFieldCard(
    item: ProductItem,
    calcResult: CalculatedItem?,
    canDelete: Boolean,
    listener: ProductCardListener,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    val isBest = calcResult?.isBestChoice == true
    val borderColor = if (isBest) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant

    // Собираем общее отображение
    var rawText by remember(item.priceInput, item.quantityInput) {
        mutableStateOf(
            if (item.priceInput.isNotEmpty() && item.quantityInput.isNotEmpty())
                "${item.priceInput} / ${item.quantityInput}"
            else item.priceInput
        )
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(if (isBest) 2.dp else 1.dp, borderColor),
        colors = CardDefaults.cardColors(
            containerColor = if (isBest) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
            else MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
            ProductHeader(id = item.id, name = item.name, canDelete = canDelete, listener = listener)
            Spacer(modifier = Modifier.height(10.dp))

            // Одно широкое поле ввода
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                    if (rawText.isEmpty()) {
                        Text(
                            text = "Цена / Вес (напр. 189 850)",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                    BasicTextField(
                        value = rawText,
                        onValueChange = { input ->
                            rawText = input
                            // Парсим разделители: пробел, слэш, дефис
                            val tokens = input.trim().split(Regex("""[\s/\\-]+"""))
                            if (tokens.isNotEmpty()) {
                                listener.onPriceChange(item.id, tokens[0])
                            }
                            if (tokens.size > 1) {
                                listener.onQuantityChange(item.id, tokens[1])
                            }
                        },
                        textStyle = TextStyle(
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                UnitDropdownMenu(selectedUnit = item.unit, onUnitSelect = { listener.onUnitChange(item.id, it) })
            }

            Spacer(modifier = Modifier.height(10.dp))
            DiscountSelectorRow(item = item, listener = listener)
            ProductCalculationFooter(unit = item.unit, calcResult = calcResult)
        }
    }
}