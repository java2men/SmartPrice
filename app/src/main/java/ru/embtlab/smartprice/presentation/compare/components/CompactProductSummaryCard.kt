package ru.embtlab.smartprice.presentation.compare.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ru.embtlab.smartprice.domain.model.CalculatedItem
import ru.embtlab.smartprice.domain.model.ProductItem
import ru.embtlab.smartprice.presentation.theme.icons.AppIcons
import ru.embtlab.smartprice.presentation.theme.icons.Delete
import ru.embtlab.smartprice.presentation.theme.icons.Edit
import java.util.Locale

@Composable
fun CompactProductSummaryCard(
    item: ProductItem,
    calcResult: CalculatedItem?,
    canDelete: Boolean,
    onCardClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isBest = calcResult?.isBestChoice == true
    val borderColor =
        if (isBest) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant

    val baseUnitLabel = item.unit.category.baseLabel
    val isFilled = item.priceInput.isNotBlank() || item.quantityInput.isNotBlank()

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onCardClick),
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(if (isBest) 2.dp else 1.dp, borderColor),
        colors = CardDefaults.cardColors(
            containerColor = if (isBest) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
            else MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Левая колонка: Название и исходные параметры
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
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
                        contentDescription = "Редактировать",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(16.dp)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                val infoText = if (item.priceInput.isNotBlank() && item.quantityInput.isNotBlank()) {
                    "${item.priceInput} ₽ за ${item.quantityInput} ${item.unit.label}"
                } else if (item.priceInput.isNotBlank()) {
                    "${item.priceInput} ₽"
                } else {
                    "Нажмите для ввода данных"
                }

                Text(
                    text = infoText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Правая колонка: Расчётная цена, бейдж и удаление
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (calcResult != null && calcResult.unitPrice > 0.0) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = String.format(Locale.US, "%.2f ₽", calcResult.unitPrice),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "за $baseUnitLabel",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )

                        if (isBest) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.primary,
                                shape = MaterialTheme.shapes.extraSmall
                            ) {
                                Text(
                                    text = "ВЫГОДНО",
                                    color = Color.White,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        } else if (calcResult.percentMoreExpensive > 0.0) {
                            Text(
                                text = String.format(Locale.US, "+%.1f%%", calcResult.percentMoreExpensive),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                if (canDelete || isFilled) {
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(onClick = onDeleteClick) {
                        Icon(
                            imageVector = AppIcons.Default.Delete,
                            contentDescription = if (isFilled) "Очистить" else "Удалить",
                            tint = if (isFilled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}