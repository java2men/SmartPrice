package ru.embtlab.smartprice.presentation.compare.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import ru.embtlab.smartprice.domain.model.DiscountType
import ru.embtlab.smartprice.domain.model.ProductItem
import ru.embtlab.smartprice.presentation.compare.util.InputFormatters
import ru.embtlab.smartprice.presentation.theme.icons.AppIcons
import ru.embtlab.smartprice.presentation.theme.icons.Close

@Composable
fun DiscountSelectorRow(
    item: ProductItem,
    listener: ProductCardListener,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current

    Column(modifier = modifier.fillMaxWidth()) {
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

        if (item.discountType == DiscountType.PERCENT) {
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = item.customDiscountPercentInput,
                onValueChange = {
                    val sanitized = InputFormatters.sanitizeDiscount(it, previousValue = item.customDiscountPercentInput)
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
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}