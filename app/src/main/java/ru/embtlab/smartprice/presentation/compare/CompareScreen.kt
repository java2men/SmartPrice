package ru.embtlab.smartprice.presentation.compare

import android.Manifest
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import ru.embtlab.smartprice.presentation.compare.components.CameraOcrScanner
import ru.embtlab.smartprice.presentation.compare.components.ProductCard
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
    val historyList by viewModel.history.collectAsState()

    var showHistorySheet by remember { mutableStateOf(false) }

    // ID товара, для которого сейчас открыта камера (null — камера закрыта)
    var scanningProductId by remember { mutableStateOf<String?>(null) }

    // Состояние запроса разрешения на камеру
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)

    // Если открыт режим сканирования камеры
    if (scanningProductId != null) {
        if (cameraPermissionState.status.isGranted) {
            CameraOcrScanner(
                onParsed = { parsed ->
                    val id = scanningProductId ?: return@CameraOcrScanner
                    parsed.price?.let { viewModel.onPriceChanged(id, it) }
                    parsed.quantity?.let { viewModel.onQuantityChanged(id, it) }
                    parsed.unit?.let { viewModel.onUnitChanged(id, it) }
                },
                onClose = { scanningProductId = null }
            )
        } else {
            // Диалог запроса прав на камеру
            AlertDialog(
                onDismissRequest = { scanningProductId = null },
                title = { Text("Требуется доступ к камере") },
                text = { Text("Чтобы распознавать ценники прямо в магазине, приложению необходим доступ к камере.") },
                confirmButton = {
                    Button(
                        onClick = { cameraPermissionState.launchPermissionRequest() }
                    ) {
                        Text("Предоставить")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { scanningProductId = null }) {
                        Text("Отмена")
                    }
                }
            )
        }
        return
    }

    // Проверяем, есть ли что сбрасывать
    val hasDataToReset = uiState.items.size > 2 ||
            uiState.items.any { it.priceInput.isNotBlank() || it.quantityInput.isNotBlank() }

    // Есть ли готовый расчет победителя для сохранения
    val canSaveResult = uiState.results.any { it.isBestChoice } && !uiState.hasIncompatibleUnits

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Умная Цена") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                actions = {
                    // Кнопка сохранения в историю
                    if (canSaveResult) {
                        IconButton(onClick = { viewModel.saveCurrentComparison() }) {
                            Icon(
                                imageVector = AppIcons.Default.BookmarkAdd,
                                contentDescription = "Сохранить в историю"
                            )
                        }
                    }

                    // Кнопка просмотра истории
                    IconButton(onClick = { showHistorySheet = true }) {
                        Icon(
                            imageVector = AppIcons.Default.History,
                            contentDescription = "История сравнений"
                        )
                    }

                    // Кнопка быстрой очистки
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
            // Баннер предупреждения при конфликте категорий (кг против литров)
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

            // Список карточек товаров
            items(uiState.items, key = { it.id }) { item ->
                val calcResult = uiState.results.find { it.product.id == item.id }

                ProductCard(
                    item = item,
                    calcResult = calcResult,
                    canDelete = uiState.items.size > 2,
                    onPriceChange = { newPrice -> viewModel.onPriceChanged(item.id, newPrice) },
                    onQuantityChange = { newQty -> viewModel.onQuantityChanged(item.id, newQty) },
                    onUnitChange = { newUnit -> viewModel.onUnitChanged(item.id, newUnit) },
                    onDiscountTypeChange = { newType -> viewModel.onDiscountTypeChanged(item.id, newType) },
                    onCustomDiscountChange = { newPercent -> viewModel.onCustomDiscountChanged(item.id, newPercent) },
                    onScanClick = { scanningProductId = item.id },
                    onDelete = { viewModel.removeProduct(item.id) }
                )
            }
        }

        // Шторка с историей расчетов (Room)
        if (showHistorySheet) {
            ModalBottomSheet(
                onDismissRequest = { showHistorySheet = false }
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