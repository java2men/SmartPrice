package ru.embtlab.smartprice.presentation.edit

import android.Manifest
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
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import ru.embtlab.smartprice.domain.model.product.DiscountType
import ru.embtlab.smartprice.domain.model.product.ProductConstants
import ru.embtlab.smartprice.domain.usecase.voice.ParseVoiceInputUseCase
import ru.embtlab.smartprice.presentation.scanner.CameraOcrScanner
import ru.embtlab.smartprice.presentation.compare.components.common.DiscountSelectorRow
import ru.embtlab.smartprice.presentation.compare.components.common.ProductCardListener
import ru.embtlab.smartprice.core.ui.components.UnitDropdownMenu
import ru.embtlab.smartprice.core.ui.dialogs.VoiceRecognitionBottomSheet
import ru.embtlab.smartprice.core.util.InputFormatters
import ru.embtlab.smartprice.domain.model.product.ProductUnit
import ru.embtlab.smartprice.presentation.compare.CompareViewModel
import ru.embtlab.smartprice.presentation.theme.icons.AppIcons
import ru.embtlab.smartprice.presentation.theme.icons.ArrowBack
import ru.embtlab.smartprice.presentation.theme.icons.Camera
import ru.embtlab.smartprice.presentation.theme.icons.Close
import ru.embtlab.smartprice.presentation.theme.icons.Mic

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
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

    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)
    val audioPermissionState = rememberPermissionState(Manifest.permission.RECORD_AUDIO)
    val parseVoiceInputUseCase = remember { ParseVoiceInputUseCase() }

    val allCatalogPresets = ProductConstants.CATALOG_PRODUCT_PRESETS

    var isVoiceDialogOpen by remember { mutableStateOf(false) }

    // Если открыт голосовой диалог — рендерим встроенную шторку
    if (isVoiceDialogOpen) {
        VoiceRecognitionBottomSheet(
            targetTitle = item.name.ifBlank { "Товар" },
            onResult = { spokenText ->
                val parsed = parseVoiceInputUseCase(spokenText)
                parsed.name?.let { viewModel.onNameChanged(productId, it) }
                parsed.price?.let { viewModel.onPriceChanged(productId, it) }
                parsed.quantity?.let { viewModel.onQuantityChanged(productId, it) }
                parsed.unit?.let { viewModel.onUnitChanged(productId, it) }
                parsed.discountType?.let { viewModel.onDiscountTypeChanged(productId, it) }
                parsed.discountPercent?.let { viewModel.onCustomDiscountChanged(productId, it) }
                viewModel.advanceFocusIfCompleted(productId)
            },
            onDismiss = { isVoiceDialogOpen = false }
        )
    }

    // Режим OCR-сканера
    if (uiState.scanningProductId == productId) {
        if (cameraPermissionState.status.isGranted) {
            CameraOcrScanner(
                onParsed = { parsed ->
                    parsed.price?.let { viewModel.onPriceChanged(productId, it) }
                    parsed.quantity?.let { viewModel.onQuantityChanged(productId, it) }
                    parsed.unit?.let { viewModel.onUnitChanged(productId, it) }
                    viewModel.advanceFocusIfCompleted(productId)
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
            override fun onCardFocused(id: String) {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(item.name.ifBlank { "Товар" }) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = AppIcons.Default.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    // Микрофон
                    if (uiState.isVoiceInputEnabled) {
                        IconButton(
                            onClick = {
                                if (!audioPermissionState.status.isGranted) {
                                    audioPermissionState.launchPermissionRequest()
                                } else {
                                    isVoiceDialogOpen = true
                                }
                            }
                        ) {
                            Icon(
                                imageVector = AppIcons.Default.Mic,
                                contentDescription = "Голосовой ввод",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    // Сканер ценников (OCR)
                    if (uiState.isOcrScannerEnabled) {
                        IconButton(onClick = { viewModel.startScanning(item.id) }) {
                            Icon(
                                imageVector = AppIcons.Default.Camera,
                                contentDescription = "Камера",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
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
            // Блок 1: Название
            Card(
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
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
                            if (it.length <= ProductConstants.MAX_PRODUCT_NAME_LENGTH) {
                                viewModel.onNameChanged(item.id, it)
                            }
                        },
                        placeholder = { Text("Название (например, Сыр)") },
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
                                                contentDescription = "Удалить",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
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

            // Блок 2: Стоимость и объём
            Card(
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
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
                                val sanitized = InputFormatters.sanitizeQuantity(it, item.quantityInput)
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

            // Блок 3: Акции и скидки
            Card(
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
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