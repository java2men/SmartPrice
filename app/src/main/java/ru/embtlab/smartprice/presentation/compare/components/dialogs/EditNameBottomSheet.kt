package ru.embtlab.smartprice.presentation.compare.components.dialogs

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditNameBottomSheet(
    currentName: String,
    recentNames: List<String> = emptyList(),
    onConfirm: (String) -> Unit,
    onDeleteRecent: (String) -> Unit = {}, // <-- Колбэк удаления из истории
    onDismiss: () -> Unit
) {
    var tempName by remember { mutableStateOf(currentName) }
    val focusManager = LocalFocusManager.current
    val verticalScrollState = rememberScrollState()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val allCatalogPresets = remember {
        listOf(
            "Молоко", "Сливочное масло", "Сыр", "Сметана", "Творог", "Йогурт", "Кефир", "Сливки", "Сгущенка",
            "Яйца", "Хлеб", "Макароны", "Рис", "Гречка", "Овсянка", "Мука", "Сахар", "Соль", "Подсолнечное масло", "Оливковое масло",
            "Курица", "Фарш", "Говядина", "Свинина", "Индейка", "Сосиски", "Колбаса", "Рыба", "Креветки",
            "Кофе", "Чай", "Шоколад", "Печенье", "Конфеты", "Сок", "Вода", "Газировка", "Мороженое",
            "Яблоки", "Бананы", "Картофель", "Помидоры", "Огурцы", "Лук", "Морковь", "Орехи",
            "Пельмени", "Вареники", "Пицца", "Чипсы", "Снеки",
            "Стиральный порошок", "Гель для стирки", "Кондиционер для белья", "Таблетки для ПММ",
            "Средство для посуды", "Мыло", "Шампунь", "Зубная паста", "Туалетная бумага", "Бумажные полотенца", "Влажные салфетки", "Мусорные пакеты"
        )
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
                .verticalScroll(verticalScrollState)
                .padding(bottom = 20.dp)
        ) {
            Text(
                text = "Название товара",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 20.dp)
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
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            )

            // РАЗДЕЛ 1: Личная история (с возможностью удаления)
            if (recentNames.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "ВЫ ЧАСТО ВЫБИРАЕТЕ",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    recentNames.forEach { recentItem ->
                        val isSelected = tempName.equals(recentItem, ignoreCase = true)
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                tempName = recentItem
                                onConfirm(recentItem)
                            },
                            label = { Text("⚡ $recentItem") },
                            trailingIcon = {
                                IconButton(
                                    onClick = { onDeleteRecent(recentItem) },
                                    modifier = Modifier.size(16.dp)
                                ) {
                                    Icon(
                                        imageVector = AppIcons.Default.Close,
                                        contentDescription = "Удалить из недавних",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            },
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

            // РАЗДЕЛ 2: Каталог товаров
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "КАТАЛОГ ТОВАРОВ",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                allCatalogPresets.forEach { preset ->
                    val isSelected = tempName.equals(preset, ignoreCase = true)
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            tempName = preset
                            onConfirm(preset)
                        },
                        label = { Text(preset) },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.focusProperties { canFocus = false }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = { onConfirm("") },
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