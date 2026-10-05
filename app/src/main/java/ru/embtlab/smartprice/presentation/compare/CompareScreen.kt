package ru.embtlab.smartprice.presentation.compare

import android.Manifest
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.launch
import ru.embtlab.smartprice.domain.model.CardStylePreset
import ru.embtlab.smartprice.domain.model.DiscountType
import ru.embtlab.smartprice.domain.model.ProductUnit
import ru.embtlab.smartprice.presentation.compare.components.*
import ru.embtlab.smartprice.presentation.compare.components.dialogs.*
import ru.embtlab.smartprice.presentation.theme.icons.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun CompareScreen(
    onOpenSettings: () -> Unit = {},
    viewModel: CompareViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)

    // Состояния для показа SnackBar
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

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
                title = { Text("Нужен доступ к камере") },
                text = { Text("Камера необходима для быстрого распознавания ценников.") },
                confirmButton = {
                    Button(onClick = { cameraPermissionState.launchPermissionRequest() }) { Text("Разрешить") }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.stopScanning() }) { Text("Отмена") }
                }
            )
        }
        return
    }

    val hasDataToReset = uiState.items.size > 2 ||
            uiState.items.any { it.priceInput.isNotBlank() || it.quantityInput.isNotBlank() }
    val canSaveResult = uiState.results.any { it.isBestChoice } && !uiState.hasIncompatibleUnits

    val cardListener = remember(viewModel) {
        object : ProductCardListener {
            override fun onNameChange(id: String, name: String) = viewModel.onNameChanged(id, name)
            override fun onPriceChange(id: String, price: String) = viewModel.onPriceChanged(id, price)
            override fun onQuantityChange(id: String, quantity: String) = viewModel.onQuantityChanged(id, quantity)
            override fun onUnitChange(id: String, unit: ProductUnit) = viewModel.onUnitChanged(id, unit)
            override fun onDiscountTypeChange(id: String, type: DiscountType) = viewModel.onDiscountTypeChanged(id, type)
            override fun onCustomDiscountChange(id: String, percent: String) = viewModel.onCustomDiscountChanged(id, percent)
            override fun onScanClick(id: String) = viewModel.startScanning(id)
            override fun onDelete(id: String) = viewModel.onTrashClick(id)
        }
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.navigationBarsPadding()
            )
        },
        topBar = {
            TopAppBar(
                title = { Text("Умная Цена") },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                actions = {
                    if (canSaveResult) {
                        IconButton(onClick = { viewModel.openSaveDialog() }) {
                            Icon(imageVector = AppIcons.Default.BookmarkAdd, contentDescription = "Сохранить")
                        }
                    }
                    IconButton(onClick = { viewModel.setHistorySheetVisible(true) }) {
                        Icon(imageVector = AppIcons.Default.History, contentDescription = "История")
                    }
                    if (hasDataToReset) {
                        IconButton(onClick = {
                            viewModel.resetWithBackup()
                            coroutineScope.launch {
                                // Снимаем предыдущий снэкбар, если он еще отображался
                                snackbarHostState.currentSnackbarData?.dismiss()
                                val result = snackbarHostState.showSnackbar(
                                    message = "Все товары очищены",
                                    actionLabel = "Отменить",
                                    duration = SnackbarDuration.Short
                                )
                                if (result == SnackbarResult.ActionPerformed) {
                                    viewModel.undoReset()
                                }
                            }
                        }) {
                            Icon(imageVector = AppIcons.Default.Refresh, contentDescription = "Сброс")
                        }
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(imageVector = AppIcons.Default.Tune, contentDescription = "Настройки")
                    }
                }
            )
        },
        floatingActionButton = {
            BadgedBox(
                badge = {
                    Badge(
                        containerColor = if (uiState.canAddMore) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.error
                        },
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ) {
                        AnimatedContent(
                            targetState = uiState.items.size,
                            transitionSpec = {
                                if (targetState > initialState) {
                                    (slideInVertically { height -> height } + fadeIn()).togetherWith(
                                        slideOutVertically { height -> -height } + fadeOut()
                                    )
                                } else {
                                    (slideInVertically { height -> -height } + fadeIn()).togetherWith(
                                        slideOutVertically { height -> height } + fadeOut()
                                    )
                                }
                            },
                            label = "ItemsCountAnimation"
                        ) { count ->
                            Text(
                                text = "$count",
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 4.dp),
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            ) {
                FloatingActionButton(
                    onClick = {
                        if (uiState.canAddMore) {
                            viewModel.addProduct()
                        }
                    },
                    containerColor = if (uiState.canAddMore) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    },
                    contentColor = if (uiState.canAddMore) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                    }
                ) {
                    Icon(
                        imageVector = AppIcons.Default.Add,
                        contentDescription = if (uiState.canAddMore) "Добавить товар" else "Достигнут лимит товаров"
                    )
                }
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
            if (uiState.hasIncompatibleUnits) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Нельзя сравнивать товары разных категорий (например, вес и объём)",
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }

            items(uiState.items, key = { it.id }) { item ->
                val calcResult = uiState.results.find { it.product.id == item.id }
                when (uiState.cardStyle) {
                    CardStylePreset.CLASSIC -> {
                        ProductCard(
                            item = item,
                            calcResult = calcResult,
                            canDelete = uiState.items.size > 2,
                            listener = cardListener
                        )
                    }
                    CardStylePreset.SMART_SINGLE_FIELD -> {
                        SmartSingleFieldCard(
                            item = item,
                            calcResult = calcResult,
                            canDelete = uiState.items.size > 2,
                            listener = cardListener
                        )
                    }
                    CardStylePreset.KEYPAD_PRESETS -> {
                        KeypadPresetCard(
                            item = item,
                            calcResult = calcResult,
                            canDelete = uiState.items.size > 2,
                            listener = cardListener
                        )
                    }
                }
            }
        }

        if (uiState.isSaveDialogOpen) {
            SaveComparisonDialog(
                titleInput = uiState.saveDialogTitleInput,
                onTitleChange = { viewModel.onSaveDialogTitleChanged(it) },
                onConfirm = { viewModel.confirmSaveComparison() },
                onDismiss = { viewModel.dismissSaveDialog() }
            )
        }

        if (uiState.isHistorySheetOpen) {
            val historyList by viewModel.history.collectAsState()
            HistoryBottomSheet(
                historyList = historyList,
                onRestore = { record -> viewModel.restoreComparison(record) },
                onDelete = { viewModel.deleteHistoryItem(it) },
                onDismiss = { viewModel.setHistorySheetVisible(false) }
            )
        }
    }
}