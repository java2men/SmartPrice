package ru.embtlab.smartprice.presentation.compare.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.embtlab.smartprice.domain.model.ProductUnit
import ru.embtlab.smartprice.presentation.compare.components.dialogs.UnitPickerBottomSheet

@Composable
fun UnitDropdownMenu(
    selectedUnit: ProductUnit,
    onUnitSelect: (ProductUnit) -> Unit,
    modifier: Modifier = Modifier
) {
    var showSheet by remember { mutableStateOf(false) }

    if (showSheet) {
        UnitPickerBottomSheet(
            selectedUnit = selectedUnit,
            onUnitSelected = onUnitSelect,
            onDismiss = { showSheet = false }
        )
    }

    Surface(
        onClick = { showSheet = true },
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
        modifier = modifier.height(38.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = selectedUnit.label,
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
}