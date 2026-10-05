package ru.embtlab.smartprice.presentation.compare.components.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.embtlab.smartprice.domain.model.ProductUnit
import ru.embtlab.smartprice.presentation.theme.icons.AppIcons
import ru.embtlab.smartprice.presentation.theme.icons.Close
import ru.embtlab.smartprice.presentation.theme.icons.Search

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun UnitPickerBottomSheet(
    selectedUnit: ProductUnit,
    onUnitSelected: (ProductUnit) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }

    val filteredUnits = remember(searchQuery) {
        if (searchQuery.isBlank()) {
            ProductUnit.entries
        } else {
            val query = searchQuery.trim().lowercase()
            ProductUnit.entries.filter { unit ->
                unit.label.lowercase().contains(query) ||
                        unit.fullName.lowercase().contains(query) ||
                        unit.searchTags.any { it.contains(query) }
            }
        }
    }

    // Группируем отфильтрованные единицы по категориям
    val groupedUnits = remember(filteredUnits) {
        filteredUnits.groupBy { it.category }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp)
        ) {
            Text(
                text = "Единица измерения",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(10.dp))

            // Поле поиска
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Поиск: рулон, капсулы, чай, мл...") },
                leadingIcon = { Icon(AppIcons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(AppIcons.Default.Close, contentDescription = "Очистить")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Список с категориями и чипсами
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                groupedUnits.forEach { (category, units) ->
                    item(key = category.name) {
                        Text(
                            text = category.title.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            units.forEach { unit ->
                                val isSelected = unit == selectedUnit
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        onUnitSelected(unit)
                                        onDismiss()
                                    },
                                    label = {
                                        Text(
                                            text = "${unit.label} (${unit.fullName})",
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}