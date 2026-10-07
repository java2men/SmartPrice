// presentation/compare/components/DiscountSelectorRow.kt
package ru.embtlab.smartprice.presentation.compare.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
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
    modifier: Modifier = Modifier,
    discountFocusRequester: FocusRequester? = null,
    dummyFocusRequester: FocusRequester? = null,
    onFocused: () -> Unit = {},
    onDoneAction: () -> Unit = {}
) {
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
                    label = { Text(type.label, style = MaterialTheme.typography.labelSmall) },
                    modifier = Modifier.focusProperties { canFocus = false },
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
                placeholder = { Text("Например, 20") },
                trailingIcon = {
                    if (item.customDiscountPercentInput.isNotEmpty()) {
                        IconButton(onClick = { listener.onCustomDiscountChange(item.id, "") }) {
                            Icon(
                                imageVector = AppIcons.Default.Close,
                                contentDescription = "Очистить скидку",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Done // Всегда закрывает цепочку
                ),
                keyboardActions = KeyboardActions(
                    onDone = { onDoneAction() }
                ),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (discountFocusRequester != null) Modifier.focusRequester(discountFocusRequester) else Modifier)
                    .focusProperties {
                        // Запрещаем переход дальше на другие карточки
                        dummyFocusRequester?.let {
                            next = it
                            down = it
                        }
                        onExit = { FocusRequester.Cancel }
                    }
                    .onFocusChanged { focusState ->
                        if (focusState.isFocused) {
                            onFocused()
                        }
                    }
            )
        }
    }
}