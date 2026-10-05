package ru.embtlab.smartprice.presentation.compare.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ru.embtlab.smartprice.domain.model.CalculatedItem
import ru.embtlab.smartprice.domain.model.ProductUnit
import ru.embtlab.smartprice.domain.model.UnitCategory
import java.util.Locale

@Composable
fun ProductCalculationFooter(
    unit: ProductUnit,
    calcResult: CalculatedItem?,
    modifier: Modifier = Modifier
) {
    if (calcResult == null || calcResult.unitPrice <= 0.0) return

    val isBest = calcResult.isBestChoice
    val baseUnitLabel = when (unit.category) {
        UnitCategory.WEIGHT -> "кг"
        UnitCategory.VOLUME -> "л"
        UnitCategory.PIECES -> "шт"
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Spacer(modifier = Modifier.height(10.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(8.dp))

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