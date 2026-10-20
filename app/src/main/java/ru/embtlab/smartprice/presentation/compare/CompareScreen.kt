package ru.embtlab.smartprice.presentation.compare

import android.Manifest
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.launch
import ru.embtlab.smartprice.BuildConfig
import ru.embtlab.smartprice.core.ad.AdConfig
import ru.embtlab.smartprice.domain.model.settings.CardStylePreset
import ru.embtlab.smartprice.domain.model.product.DiscountType
import ru.embtlab.smartprice.domain.model.product.ProductUnit
import ru.embtlab.smartprice.core.ui.components.AutoScrollLazyColumn
import ru.embtlab.smartprice.core.ui.components.ad.VkBannerAd
import ru.embtlab.smartprice.presentation.scanner.CameraOcrScanner
import ru.embtlab.smartprice.presentation.compare.components.cards.CompactProductSummaryCard
import ru.embtlab.smartprice.presentation.compare.components.cards.ProductCard
import ru.embtlab.smartprice.presentation.compare.components.common.ProductCardListener
import ru.embtlab.smartprice.presentation.compare.components.cards.SmartSingleFieldCard
import ru.embtlab.smartprice.presentation.compare.components.buttons.VoiceHoldToTalkButton
import ru.embtlab.smartprice.presentation.compare.components.dialogs.HistoryBottomSheet
import ru.embtlab.smartprice.presentation.compare.components.dialogs.SaveComparisonDialog
import ru.embtlab.smartprice.presentation.theme.icons.Add
import ru.embtlab.smartprice.presentation.theme.icons.AppIcons
import ru.embtlab.smartprice.presentation.theme.icons.BookmarkAdd
import ru.embtlab.smartprice.presentation.theme.icons.History
import ru.embtlab.smartprice.presentation.theme.icons.Refresh
import ru.embtlab.smartprice.presentation.theme.icons.Tune

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalPermissionsApi::class,
    ExperimentalLayoutApi::class
)
@Composable
fun CompareScreen(
    onOpenSettings: () -> Unit = {},
    onEditProduct: (String) -> Unit = {},
    viewModel: CompareViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val recentNames by viewModel.recentProductNames.collectAsState()
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)
    val recordAudioPermissionState = rememberPermissionState(Manifest.permission.RECORD_AUDIO)

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val focusManager = LocalFocusManager.current
    val isKeyboardOpen = WindowInsets.isImeVisible

    val focusedCardId = uiState.focusedCardId
    var focusTrigger by remember { mutableLongStateOf(0L) }
    var wasKeyboardOpen by remember { mutableStateOf(false) }

    LaunchedEffect(focusedCardId) {
        focusTrigger = System.currentTimeMillis()
    }

    LaunchedEffect(isKeyboardOpen) {
        if (isKeyboardOpen) {
            wasKeyboardOpen = true
        } else if (wasKeyboardOpen) {
            wasKeyboardOpen = false
            focusManager.clearFocus()
        }
    }

    val focusedIndex = remember(focusedCardId, uiState.items, uiState.hasIncompatibleUnits) {
        val targetId = focusedCardId ?: return@remember null
        val rawIndex = uiState.items.indexOfFirst { it.id == targetId }
        if (rawIndex == -1) null else (if (uiState.hasIncompatibleUnits) rawIndex + 1 else rawIndex)
    }

    // Режим OCR-сканера ценников
    if (uiState.scanningProductId != null) {
        if (cameraPermissionState.status.isGranted) {
            CameraOcrScanner(
                onParsed = { parsed ->
                    val id = uiState.scanningProductId ?: return@CameraOcrScanner
                    parsed.price?.let { viewModel.onPriceChanged(id, it) }
                    parsed.quantity?.let { viewModel.onQuantityChanged(id, it) }
                    parsed.unit?.let { viewModel.onUnitChanged(id, it) }
                    viewModel.advanceFocusIfCompleted(id)
                },
                onClose = { viewModel.stopScanning() }
            )
        } else {
            AlertDialog(
                onDismissRequest = { viewModel.stopScanning() },
                title = { Text("Требуется доступ к камере") },
                text = { Text("Разрешите приложению использовать камеру для распознавания ценников.") },
                confirmButton = {
                    Button(onClick = { cameraPermissionState.launchPermissionRequest() }) {
                        Text("Разрешить")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.stopScanning() }) { Text("Отмена") }
                }
            )
        }
        return
    }

    val hasDataToReset = uiState.items.size > 2 ||
            uiState.items.mapIndexed { index, item ->
                val defaultName = "Товар ${index + 1}"
                val isNameChanged =
                    item.name.isNotBlank() && !item.name.equals(defaultName, ignoreCase = true)
                isNameChanged ||
                        item.priceInput.isNotBlank() ||
                        item.quantityInput.isNotBlank() ||
                        item.customDiscountPercentInput.isNotBlank() ||
                        item.discountType != DiscountType.NONE
            }.any { it }

    val canSaveResult = uiState.results.any { it.isBestChoice } && !uiState.hasIncompatibleUnits

    val cardListener = remember(viewModel) {
        object : ProductCardListener {
            override fun onNameChange(id: String, name: String) = viewModel.onNameChanged(id, name)
            override fun onPriceChange(id: String, price: String) =
                viewModel.onPriceChanged(id, price)
            override fun onQuantityChange(id: String, quantity: String) =
                viewModel.onQuantityChanged(id, quantity)
            override fun onUnitChange(id: String, unit: ProductUnit) =
                viewModel.onUnitChanged(id, unit)
            override fun onDiscountTypeChange(id: String, type: DiscountType) =
                viewModel.onDiscountTypeChanged(id, type)
            override fun onCustomDiscountChange(id: String, percent: String) =
                viewModel.onCustomDiscountChanged(id, percent)
            override fun onScanClick(id: String) = viewModel.startScanning(id)
            override fun onDelete(id: String) = viewModel.onTrashClick(id)
            override fun onCardFocused(id: String) {
                viewModel.onCardFocused(id)
            }
            override fun onEditingFinished(id: String) {
                viewModel.advanceFocusIfCompleted(id)
            }
        }
    }

    val targetCardTitle = remember(focusedCardId, uiState.items) {
        if (!focusedCardId.isNullOrBlank()) {
            val item = uiState.items.find { it.id == focusedCardId }
            item?.name.orEmpty().ifBlank { "Товар" }
        } else {
            "Товар"
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            // Тап по свободной области экрана сбрасывает фокус с клавиатуры и карточек
            .pointerInput(Unit) {
                detectTapGestures(onTap = {
                    focusManager.clearFocus()
                    viewModel.onCardFocused(null)
                })
            },
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.navigationBarsPadding()
            )
        },
        topBar = {
            TopAppBar(
                title = { Text("Умная цена") },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                actions = {
                    if (canSaveResult) {
                        IconButton(onClick = { viewModel.openSaveDialog() }) {
                            Icon(
                                imageVector = AppIcons.Default.BookmarkAdd,
                                contentDescription = "Сохранить результат"
                            )
                        }
                    }
                    IconButton(onClick = { viewModel.setHistorySheetVisible(true) }) {
                        Icon(imageVector = AppIcons.Default.History, contentDescription = "История")
                    }
                    if (hasDataToReset) {
                        IconButton(onClick = {
                            viewModel.resetWithBackup()
                            coroutineScope.launch {
                                snackbarHostState.currentSnackbarData?.dismiss()
                                val result = snackbarHostState.showSnackbar(
                                    message = "Список очищен",
                                    actionLabel = "Отменить",
                                    duration = SnackbarDuration.Short
                                )
                                if (result == SnackbarResult.ActionPerformed) {
                                    viewModel.undoReset()
                                }
                            }
                        }) {
                            Icon(
                                imageVector = AppIcons.Default.Refresh,
                                contentDescription = "Сбросить"
                            )
                        }
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(imageVector = AppIcons.Default.Tune, contentDescription = "Настройки")
                    }
                }
            )
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = !isKeyboardOpen,
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Кнопка микрофона доступна в режимах CLASSIC и SMART_SINGLE_FIELD
                    if (uiState.cardStyle != CardStylePreset.FULL_SCREEN_EDITOR && uiState.isVoiceInputEnabled) {
                        VoiceHoldToTalkButton(
                            targetCardTitle = targetCardTitle,
                            hasAudioPermission = recordAudioPermissionState.status.isGranted,
                            onRequireAudioPermission = { recordAudioPermissionState.launchPermissionRequest() },
                            onStartRecording = { viewModel.startPushToTalk(focusedCardId) },
                            onStopRecording = { text -> viewModel.onVoicePhraseCompleted(text) },
                            onCancelRecording = { viewModel.cancelPushToTalk() }
                        )
                    }

                    // Кнопка добавления товара [+]
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
                                    coroutineScope.launch {
                                        listState.animateScrollToItem(uiState.items.size)
                                    }
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
                                contentDescription = if (uiState.canAddMore) "Добавить товар" else "Лимит достигнут"
                            )
                        }
                    }
                }
            }
        },
        bottomBar = {
            // В сборке free BuildConfig.ADS_ENABLED равен false — код баннера даже не вызывается
            if (BuildConfig.ADS_ENABLED && AdConfig.isBannerEnabled) {
                AnimatedVisibility(
                    visible = !isKeyboardOpen,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding(),
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        VkBannerAd(modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        },
    ) { innerPadding ->

        // Если настройки еще не вычитаны из DataStore — ждем первый кадр, не рендеря неверный стиль
        if (!uiState.isSettingsLoaded) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            )
            return@Scaffold
        }

        AutoScrollLazyColumn(
            state = listState,
            focusedIndex = focusedIndex,
            scrollTrigger = focusTrigger,
            onScrollFinished = { /* no-op */ },
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            if (uiState.hasIncompatibleUnits) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Разные категории единиц измерения. Сравнение невозможно.",
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }

            items(uiState.items, key = { it.id }) { item ->
                val calcResult = uiState.results.find { it.product.id == item.id }
                val isTarget = (item.id == focusedCardId)
                val itemIndex = uiState.items.indexOf(item)
                val defaultName = "Товар ${itemIndex + 1}"
                val isItemFilled =
                    (item.name.isNotBlank() && !item.name.equals(defaultName, ignoreCase = true)) ||
                            item.priceInput.isNotBlank() ||
                            item.quantityInput.isNotBlank() ||
                            item.customDiscountPercentInput.isNotBlank() ||
                            item.discountType != DiscountType.NONE

                when (uiState.cardStyle) {
                    CardStylePreset.CLASSIC -> {
                        ProductCard(
                            item = item,
                            calcResult = calcResult,
                            canDelete = uiState.items.size > 2,
                            listener = cardListener,
                            recentNames = recentNames,
                            onDeleteRecentName = { viewModel.removeRecentName(it) },
                            isVoiceTarget = isTarget,
                            isFilledExplicit = isItemFilled,
                            showScanButton = uiState.isOcrScannerEnabled,
                        )
                    }
                    CardStylePreset.SMART_SINGLE_FIELD -> {
                        SmartSingleFieldCard(
                            item = item,
                            calcResult = calcResult,
                            canDelete = uiState.items.size > 2,
                            listener = cardListener,
                            recentNames = recentNames,
                            onDeleteRecentName = { viewModel.removeRecentName(it) },
                            isVoiceTarget = isTarget,
                            isFilledExplicit = isItemFilled,
                            showScanButton = uiState.isOcrScannerEnabled,
                        )
                    }
                    CardStylePreset.FULL_SCREEN_EDITOR -> {
                        CompactProductSummaryCard(
                            item = item,
                            calcResult = calcResult,
                            canDelete = uiState.items.size > 2,
                            onCardClick = { onEditProduct(item.id) },
                            onDeleteClick = { viewModel.onTrashClick(item.id) },
                            isFilledExplicit = isItemFilled
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