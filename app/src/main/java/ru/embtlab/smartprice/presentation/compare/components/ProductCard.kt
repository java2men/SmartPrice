package ru.embtlab.smartprice.presentation.compare.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.embtlab.smartprice.domain.model.CalculatedItem
import ru.embtlab.smartprice.domain.model.DiscountType
import ru.embtlab.smartprice.domain.model.ProductItem
import ru.embtlab.smartprice.domain.model.ProductUnit
import ru.embtlab.smartprice.domain.model.UnitCategory
import ru.embtlab.smartprice.presentation.compare.util.InputFormatters
import ru.embtlab.smartprice.presentation.theme.icons.AppIcons
import ru.embtlab.smartprice.presentation.theme.icons.Camera
import ru.embtlab.smartprice.presentation.theme.icons.Close
import ru.embtlab.smartprice.presentation.theme.icons.Delete
import ru.embtlab.smartprice.presentation.theme.icons.Edit
import java.util.Locale

@Composable
fun ProductCard(
    item: ProductItem,
    calcResult: CalculatedItem?,
    canDelete: Boolean,
    listener: ProductCardListener,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    val quantityFocusRequester = remember { FocusRequester() }

    var showEditNameDialog by remember { mutableStateOf(false) }
    var tempNameInput by remember { mutableStateOf("") }
    var unitMenuExpanded by remember { mutableStateOf(false) }

    val isBest = calcResult?.isBestChoice == true
    val borderColor =
        if (isBest) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant

    // Диалог редактирования имени (текст не ломает строку)
    if (showEditNameDialog) {
        AlertDialog(
            onDismissRequest = { showEditNameDialog = false },
            title = { Text("Название товара") },
            text = {
                OutlinedTextField(
                    value = tempNameInput,
                    onValueChange = { if (it.length <= 30) tempNameInput = it },
                    label = { Text("Название") },
                    placeholder = { Text("например, Молоко") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        listener.onNameChange(item.id, tempNameInput.trim())
                        showEditNameDialog = false
                    }
                ) {
                    Text("Готово")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditNameDialog = false }) {
                    Text("Отмена")
                }
            }
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
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            // 1. Верхняя строка: Фиксированный заголовок и действия
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            tempNameInput = item.name
                            showEditNameDialog = true
                        }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.name.ifBlank { "Товар" },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = AppIcons.Default.Edit,
                        contentDescription = "Изменить имя",
                        tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.7f),
                        modifier = Modifier.size(16.dp)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { listener.onScanClick(item.id) }) {
                        Icon(
                            imageVector = AppIcons.Default.Camera,
                            contentDescription = "Сканировать ценник",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    if (canDelete) {
                        IconButton(onClick = { listener.onDelete(item.id) }) {
                            Icon(
                                imageVector = AppIcons.Default.Delete,
                                contentDescription = "Удалить",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 2. Сплит-блок ввода: Единый монолитный контейнер
            // Внутри ProductCard.kt — Сплит-блок ввода:

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
                // 1. Секция Цены
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
                                val sanitized = InputFormatters.sanitizePrice(
                                    it,
                                    previousValue = item.priceInput
                                )
                                listener.onPriceChange(item.id, sanitized)
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
                                onNext = { quantityFocusRequester.requestFocus() }
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        if (item.priceInput.isNotEmpty()) {
                            Text(
                                text = "₽",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.padding(start = 2.dp)
                            )
                        }
                    }
                }

                // Единственный разделитель (между Ценой и Количеством)
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .fillMaxHeight(0.6f)
                        .background(MaterialTheme.colorScheme.outlineVariant)
                )

                // 2. Секция Количества с пристыкованным выбором меры (без второй перегородки)
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
                                val sanitized = InputFormatters.sanitizeQuantity(
                                    it,
                                    previousValue = item.quantityInput
                                )
                                listener.onQuantityChange(item.id, sanitized)
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
                            keyboardActions = KeyboardActions(
                                onDone = { focusManager.clearFocus() }
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(quantityFocusRequester)
                        )
                    }

                    // Интегрированная кнопка единиц измерения (смотрится как естественный суффикс)
                    Box {
                        Surface(
                            onClick = { unitMenuExpanded = true },
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
                            modifier = Modifier.height(38.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = item.unit.label,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "▼",
                                    fontSize = 8.sp,
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = unitMenuExpanded,
                            onDismissRequest = { unitMenuExpanded = false }
                        ) {
                            ProductUnit.entries.forEach { unit ->
                                DropdownMenuItem(
                                    text = { Text(unit.label) },
                                    onClick = {
                                        listener.onUnitChange(item.id, unit)
                                        unitMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 3. Блок акций
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Акция:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                DiscountType.entries.forEach { type ->
                    FilterChip(
                        selected = item.discountType == type,
                        onClick = { listener.onDiscountTypeChange(item.id, type) },
                        label = { Text(type.label, style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }

            // Поле для ввода процентов со скидкой
            if (item.discountType == DiscountType.PERCENT) {
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = item.customDiscountPercentInput,
                    onValueChange = {
                        val sanitized = InputFormatters.sanitizeDiscount(it)
                        listener.onCustomDiscountChange(item.id, sanitized)
                    },
                    label = { Text("Размер скидки (%)") },
                    placeholder = { Text("например, 20") },
                    trailingIcon = {
                        if (item.customDiscountPercentInput.isNotEmpty()) {
                            IconButton(onClick = { listener.onCustomDiscountChange(item.id, "") }) {
                                Icon(
                                    imageVector = AppIcons.Default.Close,
                                    contentDescription = "Стереть",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { focusManager.clearFocus() }
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // 4. Результат подсчета
            if (calcResult != null && calcResult.unitPrice > 0.0) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))

                val baseUnitLabel = when (item.unit.category) {
                    UnitCategory.WEIGHT -> "кг"
                    UnitCategory.VOLUME -> "л"
                    UnitCategory.PIECES -> "шт"
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = String.format(
                                Locale.US,
                                "%.2f ₽ / %s",
                                calcResult.unitPrice,
                                baseUnitLabel
                            ),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold
                        )
                        if (!isBest && calcResult.percentMoreExpensive > 0.0) {
                            Text(
                                text = String.format(
                                    Locale.US,
                                    "+%.1f%% дороже",
                                    calcResult.percentMoreExpensive
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }

                    if (isBest) {
                        Surface(
                            color = MaterialTheme.colorScheme.primary,
                            shape = MaterialTheme.shapes.small
                        ) {
                            Text(
                                text = "ВЫГОДНО",
                                color = Color.White,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}