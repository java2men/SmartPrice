package ru.embtlab.smartprice.presentation.compare.components.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import ru.embtlab.smartprice.presentation.theme.icons.AppIcons
import ru.embtlab.smartprice.presentation.theme.icons.Close

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EditNameBottomSheet(
    currentName: String,
    recentNames: List<String> = emptyList(),
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var tempName by remember { mutableStateOf(currentName) }
    val focusManager = LocalFocusManager.current
    val scrollState = rememberScrollState()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Базовый список, если истории ещё мало или нужного товара нет в недавних
    val defaultPresets = listOf(
        "Молоко", "Масло", "Сыр", "Сметана",
        "Творог", "Кофе", "Чай", "Шоколад",
        "Яйца", "Хлеб", "Порошок", "Бумага"
    )

    // Исключаем из дефолтных пресетов те, которые уже отображаются в блоке недавних
    val filteredDefaults = defaultPresets.filterNot { defaultItem ->
        recentNames.any { it.equals(defaultItem, ignoreCase = true) }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp)
                .padding(bottom = 20.dp)
        ) {
            Text(
                text = "Название товара",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = tempName,
                onValueChange = { if (it.length <= 40) tempName = it },
                placeholder = { Text("Например: Масло 82.5%") },
                supportingText = {
                    if (tempName.length > 25) {
                        Text(
                            text = "${tempName.length}/40",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        focusManager.clearFocus()
                        onConfirm(tempName.trim())
                    }
                ),
                trailingIcon = {
                    if (tempName.isNotEmpty()) {
                        IconButton(onClick = { tempName = "" }) {
                            Icon(
                                imageVector = AppIcons.Default.Close,
                                contentDescription = "Очистить",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            // БЛОК 1: ДИНАМИЧЕСКИЕ ЧИПСЫ ПОЛЬЗОВАТЕЛЯ (если история уже есть)
            if (recentNames.isNotEmpty()) {
                Spacer(modifier = Modifier.height(18.dp))
                Text(
                    text = "ВЫ ЧАСТО ВЫБИРАЕТЕ",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    recentNames.forEach { recentItem ->
                        val isSelected = tempName.equals(recentItem, ignoreCase = true)
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                tempName = recentItem
                                onConfirm(recentItem) // Мгновенный выбор в 1 тап
                            },
                            label = { Text("⚡ $recentItem") },
                            shape = RoundedCornerShape(10.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            modifier = Modifier.focusProperties { canFocus = false }
                        )
                    }
                }
            }

            // БЛОК 2: БАЗОВЫЕ ПОПУЛЯРНЫЕ ТОВАРЫ
            Spacer(modifier = Modifier.height(18.dp))
            Text(
                text = if (recentNames.isNotEmpty()) "ДРУГИЕ ТОВАРЫ" else "БЫСТРЫЙ ВЫБОР",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                filteredDefaults.forEach { preset ->
                    val isSelected = tempName.equals(preset, ignoreCase = true)
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            tempName = preset
                            onConfirm(preset) // Мгновенный выбор в 1 тап
                        },
                        label = { Text(preset) },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.focusProperties { canFocus = false }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = { onConfirm("") }, // Сбросить к "Товар N"
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Сбросить")
                }

                Button(
                    onClick = { onConfirm(tempName.trim()) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Готово")
                }
            }
        }
    }
}