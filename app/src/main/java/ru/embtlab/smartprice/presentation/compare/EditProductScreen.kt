package ru.embtlab.smartprice.presentation.compare

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import ru.embtlab.smartprice.domain.model.DiscountType
import ru.embtlab.smartprice.domain.model.ProductConstants
import ru.embtlab.smartprice.presentation.compare.components.DiscountSelectorRow
import ru.embtlab.smartprice.presentation.compare.components.ProductCardListener
import ru.embtlab.smartprice.presentation.compare.components.UnitDropdownMenu
import ru.embtlab.smartprice.presentation.compare.util.InputFormatters
import ru.embtlab.smartprice.presentation.theme.icons.AppIcons
import ru.embtlab.smartprice.presentation.theme.icons.ArrowBack
import ru.embtlab.smartprice.presentation.theme.icons.Camera
import ru.embtlab.smartprice.presentation.theme.icons.Close

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProductScreen(
    productId: String,
    viewModel: CompareViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val recentNames by viewModel.recentProductNames.collectAsState()
    val item = uiState.items.find { it.id == productId }

    if (item == null) {
        LaunchedEffect(Unit) { onNavigateBack() }
        return
    }

    val focusManager = LocalFocusManager.current

    val allCatalogPresets = ProductConstants.CATALOG_PRODUCT_PRESETS

    val cardListener = remember(viewModel) {
        object : ProductCardListener {
            override fun onNameChange(id: String, name: String) = viewModel.onNameChanged(id, name)
            override fun onPriceChange(id: String, price: String) =
                viewModel.onPriceChanged(id, price)

            override fun onQuantityChange(id: String, quantity: String) =
                viewModel.onQuantityChanged(id, quantity)

            override fun onUnitChange(
                id: String,
                unit: ru.embtlab.smartprice.domain.model.ProductUnit
            ) = viewModel.onUnitChanged(id, unit)

            override fun onDiscountTypeChange(id: String, type: DiscountType) =
                viewModel.onDiscountTypeChanged(id, type)

            override fun onCustomDiscountChange(id: String, percent: String) =
                viewModel.onCustomDiscountChanged(id, percent)

            override fun onScanClick(id: String) = viewModel.startScanning(id)
            override fun onDelete(id: String) = viewModel.onTrashClick(id)
            override fun onCardFocused(id: String) {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(item.name.ifBlank { "Редактирование товара" }) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = AppIcons.Default.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.startScanning(item.id) }) {
                        Icon(
                            imageVector = AppIcons.Default.Camera,
                            contentDescription = "Сканировать ценник",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            )
        },
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                tonalElevation = 3.dp
            ) {
                Box(
                    modifier = Modifier
                        .navigationBarsPadding()
                        .padding(16.dp)
                ) {
                    Button(
                        onClick = onNavigateBack,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    ) {
                        Text(
                            "Готово",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // БЛОК 1: НАЗВАНИЕ И ЧИПСЫ БЫСТРОГО ВЫБОРА
            Card(
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(
                        alpha = 0.45f
                    )
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "НАЗВАНИЕ ТОВАРА",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = item.name,
                        onValueChange = {
                            if (it.length <= ProductConstants.MAX_PRODUCT_NAME_LENGTH) viewModel.onNameChanged(
                                item.id,
                                it
                            )
                        },
                        placeholder = { Text("Введите название (напр. Сметана)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Sentences,
                            imeAction = ImeAction.Next
                        ),
                        trailingIcon = {
                            if (item.name.isNotEmpty()) {
                                IconButton(onClick = { viewModel.onNameChanged(item.id, "") }) {
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

                    // Раздел недавних товаров
                    if (recentNames.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "Вы часто выбираете",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            recentNames.forEach { recentItem ->
                                val isSelected = item.name.equals(recentItem, ignoreCase = true)
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { viewModel.onNameChanged(item.id, recentItem) },
                                    label = { Text("⚡ $recentItem") },
                                    trailingIcon = {
                                        IconButton(
                                            onClick = { viewModel.removeRecentName(recentItem) },
                                            modifier = Modifier.size(16.dp)
                                        ) {
                                            Icon(
                                                imageVector = AppIcons.Default.Close,
                                                contentDescription = "Удалить из недавних",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                                    alpha = 0.7f
                                                ),
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

                    // Раздел каталога товаров
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Каталог товаров",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        allCatalogPresets.forEach { preset ->
                            val isSelected = item.name.equals(preset, ignoreCase = true)
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.onNameChanged(item.id, preset) },
                                label = { Text(preset) },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.focusProperties { canFocus = false }
                            )
                        }
                    }
                }
            }

            // БЛОК 2: ПАРАМЕТРЫ ЦЕНЫ И КОЛИЧЕСТВА
            Card(
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(
                        alpha = 0.45f
                    )
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "СТОИМОСТЬ И ОБЪЁМ",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = item.priceInput,
                            onValueChange = {
                                val sanitized = InputFormatters.sanitizePrice(it, item.priceInput)
                                viewModel.onPriceChanged(item.id, sanitized)
                            },
                            label = { Text("Цена (₽)") },
                            placeholder = { Text("0") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Decimal,
                                imeAction = ImeAction.Next
                            ),
                            modifier = Modifier.weight(1.1f)
                        )

                        OutlinedTextField(
                            value = item.quantityInput,
                            onValueChange = {
                                val sanitized =
                                    InputFormatters.sanitizeQuantity(it, item.quantityInput)
                                viewModel.onQuantityChanged(item.id, sanitized)
                            },
                            label = { Text("Количество") },
                            placeholder = { Text("0") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Decimal,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                            modifier = Modifier.weight(1.1f)
                        )

                        UnitDropdownMenu(
                            selectedUnit = item.unit,
                            onUnitSelect = { viewModel.onUnitChanged(item.id, it) }
                        )
                    }
                }
            }

            // БЛОК 3: АКЦИИ И СКИДКИ
            Card(
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(
                        alpha = 0.45f
                    )
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "АКЦИИ И СКИДКИ",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    DiscountSelectorRow(
                        item = item,
                        listener = cardListener
                    )
                }
            }
        }
    }
}