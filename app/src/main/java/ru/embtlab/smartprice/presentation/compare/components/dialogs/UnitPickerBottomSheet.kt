package ru.embtlab.smartprice.presentation.compare.components.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.embtlab.smartprice.domain.model.ProductUnit
import ru.embtlab.smartprice.domain.model.UnitCategory
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

    // Жестко заданный порядок категорий: Фасовка строго в самом конце
    val categoryOrder = listOf(
        UnitCategory.WEIGHT,
        UnitCategory.VOLUME,
        UnitCategory.LENGTH,
        UnitCategory.AREA,
        UnitCategory.PACKAGING
    )

    val groupedUnits = remember(filteredUnits) {
        val groups = filteredUnits.groupBy { it.category }
        categoryOrder.mapNotNull { category ->
            groups[category]?.let { units -> category to units }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 28.dp)
        ) {
            Text(
                text = "Единица измерения",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(10.dp))

            // Поле поиска с иконками
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Поиск: рулон, капсулы, чай, мл...") },
                leadingIcon = {
                    Icon(
                        imageVector = AppIcons.Default.Search,
                        contentDescription = "Поиск"
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(
                                imageVector = AppIcons.Default.Close,
                                contentDescription = "Очистить"
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(14.dp))

            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
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
                                    },
                                    modifier = Modifier.focusProperties { canFocus = false }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}