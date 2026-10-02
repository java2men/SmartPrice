// presentation/compare/components/ProductCard.kt
package ru.embtlab.smartprice.presentation.compare.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import ru.embtlab.smartprice.domain.model.CalculatedItem
import ru.embtlab.smartprice.domain.model.DiscountType
import ru.embtlab.smartprice.domain.model.ProductItem
import ru.embtlab.smartprice.domain.model.ProductUnit
import ru.embtlab.smartprice.domain.model.UnitCategory
import ru.embtlab.smartprice.presentation.theme.icons.AppIcons
import ru.embtlab.smartprice.presentation.theme.icons.Camera
import ru.embtlab.smartprice.presentation.theme.icons.Delete
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductCard(
    item: ProductItem,
    calcResult: CalculatedItem?,
    canDelete: Boolean,
    onPriceChange: (String) -> Unit,
    onQuantityChange: (String) -> Unit,
    onUnitChange: (ProductUnit) -> Unit,
    onDiscountTypeChange: (DiscountType) -> Unit,
    onCustomDiscountChange: (String) -> Unit,
    onDelete: () -> Unit,
    onScanClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isBest = calcResult?.isBestChoice == true
    val borderColor = if (isBest) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant

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
            // Верхняя строка: Название и кнопка удаления
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.name.ifBlank { "Товар" },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Row {
                    // Кнопка вызова камеры для этого товара
                    IconButton(onClick = onScanClick) {
                        Icon(
                            imageVector = AppIcons.Default.Camera,
                            contentDescription = "Сканировать ценник",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    if (canDelete) {
                        IconButton(onClick = onDelete) {
                            Icon(
                                imageVector = AppIcons.Default.Delete,
                                contentDescription = "Удалить позицию",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Поля ввода: Цена, Количество и Единица
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = item.priceInput,
                    onValueChange = onPriceChange,
                    label = { Text("Цена (₽)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )

                OutlinedTextField(
                    value = item.quantityInput,
                    onValueChange = onQuantityChange,
                    label = { Text("Кол-во") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )

                var expanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded },
                    modifier = Modifier.weight(0.9f)
                ) {
                    OutlinedTextField(
                        value = item.unit.label,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        ProductUnit.entries.forEach { unit ->
                            DropdownMenuItem(
                                text = { Text(unit.label) },
                                onClick = {
                                    onUnitChange(unit)
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Блок выбора акции (Чипы)
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
                        onClick = { onDiscountTypeChange(type) },
                        label = { Text(type.label, style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }

            // Дополнительное поле ввода, если выбран процент
            if (item.discountType == DiscountType.PERCENT) {
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = item.customDiscountPercentInput,
                    onValueChange = onCustomDiscountChange,
                    label = { Text("Размер скидки (%)") },
                    placeholder = { Text("например, 20") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Блок результатов
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
                            text = String.format(Locale.US, "%.2f ₽ / %s", calcResult.unitPrice, baseUnitLabel),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold
                        )
                        if (!isBest && calcResult.percentMoreExpensive > 0.0) {
                            Text(
                                text = String.format(Locale.US, "+%.1f%% дороже", calcResult.percentMoreExpensive),
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