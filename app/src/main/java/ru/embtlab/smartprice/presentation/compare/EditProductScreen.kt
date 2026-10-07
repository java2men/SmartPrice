package ru.embtlab.smartprice.presentation.compare

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import ru.embtlab.smartprice.domain.model.DiscountType
import ru.embtlab.smartprice.presentation.compare.components.DiscountSelectorRow
import ru.embtlab.smartprice.presentation.compare.components.ProductCardListener
import ru.embtlab.smartprice.presentation.compare.components.UnitDropdownMenu
import ru.embtlab.smartprice.presentation.compare.components.dialogs.EditNameBottomSheet
import ru.embtlab.smartprice.presentation.compare.util.InputFormatters
import ru.embtlab.smartprice.presentation.theme.icons.*

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
    var showNameSheet by remember { mutableStateOf(false) }

    if (showNameSheet) {
        EditNameBottomSheet(
            currentName = item.name,
            recentNames = recentNames,
            onConfirm = {
                viewModel.onNameChanged(item.id, it)
                showNameSheet = false
            },
            onDeleteRecent = { viewModel.removeRecentName(it) },
            onDismiss = { showNameSheet = false }
        )
    }

    val cardListener = remember(viewModel) {
        object : ProductCardListener {
            override fun onNameChange(id: String, name: String) = viewModel.onNameChanged(id, name)
            override fun onPriceChange(id: String, price: String) = viewModel.onPriceChanged(id, price)
            override fun onQuantityChange(id: String, quantity: String) = viewModel.onQuantityChanged(id, quantity)
            override fun onUnitChange(id: String, unit: ru.embtlab.smartprice.domain.model.ProductUnit) = viewModel.onUnitChanged(id, unit)
            override fun onDiscountTypeChange(id: String, type: DiscountType) = viewModel.onDiscountTypeChanged(id, type)
            override fun onCustomDiscountChange(id: String, percent: String) = viewModel.onCustomDiscountChanged(id, percent)
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
                        Text("Готово", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
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
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Блок 1: Название товара
            Card(
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Название",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = item.name.ifBlank { "Нажмите для выбора названия" },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Button(
                        onClick = { showNameSheet = true },
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Выбрать")
                    }
                }
            }

            // Блок 2: Цена и Количество
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
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
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                    modifier = Modifier.weight(1f)
                )

                OutlinedTextField(
                    value = item.quantityInput,
                    onValueChange = {
                        val sanitized = InputFormatters.sanitizeQuantity(it, item.quantityInput)
                        viewModel.onQuantityChanged(item.id, sanitized)
                    },
                    label = { Text("Количество") },
                    placeholder = { Text("0") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                    modifier = Modifier.weight(1f)
                )

                UnitDropdownMenu(
                    selectedUnit = item.unit,
                    onUnitSelect = { viewModel.onUnitChanged(item.id, it) }
                )
            }

            // Блок 3: Акция и скидка
            DiscountSelectorRow(
                item = item,
                listener = cardListener
            )
        }
    }
}