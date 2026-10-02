package ru.embtlab.smartprice.presentation.compare

import android.Manifest
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import ru.embtlab.smartprice.domain.model.DiscountType
import ru.embtlab.smartprice.domain.model.ProductUnit
import ru.embtlab.smartprice.presentation.compare.components.CameraOcrScanner
import ru.embtlab.smartprice.presentation.compare.components.ProductCard
import ru.embtlab.smartprice.presentation.compare.components.ProductCardListener
import ru.embtlab.smartprice.presentation.theme.icons.Add
import ru.embtlab.smartprice.presentation.theme.icons.AppIcons
import ru.embtlab.smartprice.presentation.theme.icons.BookmarkAdd
import ru.embtlab.smartprice.presentation.theme.icons.Delete
import ru.embtlab.smartprice.presentation.theme.icons.History
import ru.embtlab.smartprice.presentation.theme.icons.Refresh

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun CompareScreen(
    viewModel: CompareViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)

    // 1. Полноэкранный сканер камеры через CameraX
    if (uiState.scanningProductId != null) {
        if (cameraPermissionState.status.isGranted) {
            CameraOcrScanner(
                onParsed = { parsed ->
                    val id = uiState.scanningProductId ?: return@CameraOcrScanner
                    parsed.price?.let { viewModel.onPriceChanged(id, it) }
                    parsed.quantity?.let { viewModel.onQuantityChanged(id, it) }
                    parsed.unit?.let { viewModel.onUnitChanged(id, it) }
                },
                onClose = { viewModel.stopScanning() }
            )
        } else {
            AlertDialog(
                onDismissRequest = { viewModel.stopScanning() },
                title = { Text("Требуется доступ к камере") },
                text = { Text("Чтобы распознавать ценники прямо в магазине, приложению необходим доступ к камере.") },
                confirmButton = {
                    Button(onClick = { cameraPermissionState.launchPermissionRequest() }) {
                        Text("Предоставить")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.stopScanning() }) {
                        Text("Отмена")
                    }
                }
            )
        }
        return
    }

    val hasDataToReset = uiState.items.size > 2 ||
            uiState.items.any { it.priceInput.isNotBlank() || it.quantityInput.isNotBlank() }

    val canSaveResult = uiState.results.any { it.isBestChoice } && !uiState.hasIncompatibleUnits

    // Создаем единственный стабильный слушатель для всех карточек:
    val cardListener = remember(viewModel) {
        object : ProductCardListener {
            override fun onNameChange(id: String, name: String) = viewModel.onNameChanged(id, name)
            override fun onPriceChange(id: String, price: String) = viewModel.onPriceChanged(id, price)
            override fun onQuantityChange(id: String, quantity: String) = viewModel.onQuantityChanged(id, quantity)
            override fun onUnitChange(id: String, unit: ProductUnit) = viewModel.onUnitChanged(id, unit)
            override fun onDiscountTypeChange(id: String, type: DiscountType) = viewModel.onDiscountTypeChanged(id, type)
            override fun onCustomDiscountChange(id: String, percent: String) = viewModel.onCustomDiscountChanged(id, percent)
            override fun onScanClick(id: String) = viewModel.startScanning(id)
            override fun onDelete(id: String) = viewModel.removeProduct(id)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Умная Цена") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                actions = {
                    // Кнопка открытия диалога сохранения
                    if (canSaveResult) {
                        IconButton(onClick = { viewModel.openSaveDialog() }) {
                            Icon(
                                imageVector = AppIcons.Default.BookmarkAdd,
                                contentDescription = "Сохранить в историю"
                            )
                        }
                    }

                    // Кнопка открытия шторки истории
                    IconButton(onClick = { viewModel.setHistorySheetVisible(true) }) {
                        Icon(
                            imageVector = AppIcons.Default.History,
                            contentDescription = "История сравнений"
                        )
                    }

                    // Кнопка сброса
                    if (hasDataToReset) {
                        IconButton(onClick = { viewModel.reset() }) {
                            Icon(
                                imageVector = AppIcons.Default.Refresh,
                                contentDescription = "Очистить всё"
                            )
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.addProduct() },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    imageVector = AppIcons.Default.Add,
                    contentDescription = "Добавить товар"
                )
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 88.dp)
        ) {
            // Предупреждение о несовместимости единиц
            if (uiState.hasIncompatibleUnits) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        ),
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Нельзя корректно сравнить разные меры (например, литры и килограммы). Выберите одинаковую категорию единиц.",
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }



            // Карточки товаров
            items(uiState.items, key = { it.id }) { item ->
                val calcResult = uiState.results.find { it.product.id == item.id }

                ProductCard(
                    item = item,
                    calcResult = calcResult,
                    canDelete = uiState.items.size > 2,
                    listener = cardListener
                )
            }
        }

        // 2. Диалог сохранения в историю
        if (uiState.isSaveDialogOpen) {
            AlertDialog(
                onDismissRequest = { viewModel.dismissSaveDialog() },
                title = { Text("Сохранить сравнение") },
                text = {
                    Column {
                        Text(
                            text = "Задайте понятное название для поиска в истории:",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = uiState.saveDialogTitleInput,
                            onValueChange = { viewModel.onSaveDialogTitleChanged(it) },
                            label = { Text("Название") },
                            placeholder = { Text("например, Молоко в Магните") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(onClick = { viewModel.confirmSaveComparison() }) {
                        Text("Сохранить")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.dismissSaveDialog() }) {
                        Text("Отмена")
                    }
                }
            )
        }
        
        // 3. Шторка истории расчётов (Room)
        if (uiState.isHistorySheetOpen) {
            // ВСТАВЬТЕ СЮДА: история подгружается только когда пользователь реально открыл шторку
            val historyList by viewModel.history.collectAsState()

            ModalBottomSheet(
                onDismissRequest = { viewModel.setHistorySheetVisible(false) }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 32.dp)
                ) {
                    Text(
                        text = "История расчетов",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    if (historyList.isEmpty()) {
                        Text(
                            text = "История пока пуста",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 24.dp)
                        )
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(historyList, key = { it.id }) { record ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = record.title,
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Text(
                                                text = "${record.bestProductName}: ${record.bestUnitPriceFormatted}",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Text(
                                                text = record.savingsInfo,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.secondary
                                            )
                                        }
                                        IconButton(onClick = { viewModel.deleteHistoryItem(record.id) }) {
                                            Icon(
                                                imageVector = AppIcons.Default.Delete,
                                                contentDescription = "Удалить запись",
                                                tint = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

    }
}