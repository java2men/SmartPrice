package ru.embtlab.smartprice.presentation.scanner

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.Rect
import androidx.activity.compose.BackHandler
import androidx.camera.core.Camera
import androidx.camera.core.FocusMeteringAction
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import ru.embtlab.smartprice.core.analytics.AnalyticsEvents
import ru.embtlab.smartprice.core.analytics.AnalyticsManager
import ru.embtlab.smartprice.domain.model.ocr.ParsedPriceTag
import ru.embtlab.smartprice.domain.model.product.ProductUnit
import ru.embtlab.smartprice.domain.usecase.ocr.ParsePriceTagUseCase
import ru.embtlab.smartprice.presentation.scanner.components.CameraPreviewView
import ru.embtlab.smartprice.presentation.scanner.components.ScannerNumPad
import ru.embtlab.smartprice.presentation.scanner.components.ScannerOverlayBox

private enum class SelectedTargetSlot { PRICE, QUANTITY }

@Composable
fun CameraOcrScanner(
    onParsed: (ParsedPriceTag) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Аналитика: открытие экрана сканера ценников
    LaunchedEffect(Unit) {
        AnalyticsManager.trackEvent(AnalyticsEvents.OCR_SCANNER_OPENED)
    }

    val context = LocalContext.current
    val parser = remember { ParsePriceTagUseCase() }
    val recognizer = remember { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }

    var cameraInstance by remember { mutableStateOf<Camera?>(null) }
    var previewViewRef by remember { mutableStateOf<PreviewView?>(null) }
    var isFlashOn by remember { mutableStateOf(false) }

    var screenSize by remember { mutableStateOf(IntSize.Zero) }
    var frameRectOnScreen by remember { mutableStateOf<Rect?>(null) }

    // Состояния заморозки кадра
    var isFrozen by remember { mutableStateOf(false) }
    var fullFrozenBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isProcessing by remember { mutableStateOf(false) }

    // Теперь isFrozen уже объявлен и доступен для захвата в лямбду
    val handleClose = {
        AnalyticsManager.trackEvent(
            AnalyticsEvents.OCR_SCANNER_CLOSED,
            mapOf("was_frozen" to isFrozen.toString())
        )
        onClose()
    }

    BackHandler(onBack = handleClose)

    var selectedPrice by remember { mutableStateOf("") }
    var selectedQuantity by remember { mutableStateOf("") }
    var selectedUnit by remember { mutableStateOf(ProductUnit.GRAM) }
    var activeSlot by remember { mutableStateOf(SelectedTargetSlot.PRICE) }
    var detectedNumbersList by remember { mutableStateOf<List<String>>(emptyList()) }

    val isAppDarkTheme = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    DisposableEffect(isAppDarkTheme) {
        val window = (context as? Activity)?.window
        val insetsController = window?.let { WindowCompat.getInsetsController(it, it.decorView) }
        insetsController?.isAppearanceLightStatusBars = false
        onDispose {
            insetsController?.isAppearanceLightStatusBars = !isAppDarkTheme
            recognizer.close()
        }
    }

    val onKeyClick: (String) -> Unit = { key ->
        val currentVal = if (activeSlot == SelectedTargetSlot.PRICE) selectedPrice else selectedQuantity
        val newVal = when (key) {
            "⌫" -> if (currentVal.isNotEmpty()) currentVal.dropLast(1) else ""
            "C" -> ""
            "." -> if (currentVal.contains('.')) currentVal else if (currentVal.isEmpty()) "0." else "$currentVal."
            ".99" -> {
                val integerPart = currentVal.substringBefore('.')
                if (integerPart.isNotEmpty()) "$integerPart.99" else "0.99"
            }
            else -> if (currentVal.length < 7) currentVal + key else currentVal
        }
        if (activeSlot == SelectedTargetSlot.PRICE) selectedPrice = newVal else selectedQuantity = newVal
    }

    val captureAndFreeze: () -> Unit = {
        val bitmap = previewViewRef?.bitmap
        val rect = frameRectOnScreen
        if (bitmap != null && rect != null && !isProcessing) {
            isProcessing = true
            fullFrozenBitmap = bitmap
            isFrozen = true

            val scaleX = bitmap.width.toFloat() / screenSize.width.coerceAtLeast(1)
            val scaleY = bitmap.height.toFloat() / screenSize.height.coerceAtLeast(1)

            val cropLeft = ((rect.left * scaleX) - (bitmap.width * 0.03f)).toInt().coerceAtLeast(0)
            val cropTop = ((rect.top * scaleY) - (bitmap.height * 0.03f)).toInt().coerceAtLeast(0)
            val cropWidth = ((rect.width() * scaleX) + (bitmap.width * 0.06f)).toInt().coerceAtMost(bitmap.width - cropLeft)
            val cropHeight = ((rect.height() * scaleY) + (bitmap.height * 0.06f)).toInt().coerceAtMost(bitmap.height - cropTop)

            val croppedBitmap = try {
                Bitmap.createBitmap(bitmap, cropLeft, cropTop, cropWidth, cropHeight)
            } catch (e: Exception) {
                bitmap
            }

            val inputImage = InputImage.fromBitmap(croppedBitmap, 0)
            recognizer.process(inputImage)
                .addOnSuccessListener { visionText ->
                    val autoParsed = parser.parseFromVisionText(
                        visionText = visionText,
                        frameWidth = croppedBitmap.width,
                        frameHeight = croppedBitmap.height
                    )
                    selectedPrice = autoParsed.price.orEmpty()
                    selectedQuantity = autoParsed.quantity.orEmpty()
                    autoParsed.unit?.let { selectedUnit = it }

                    val rawText = visionText.text
                    val allNumbers = mutableListOf<String>()
                    if (selectedPrice.isNotBlank()) allNumbers.add(selectedPrice)

                    val regexWithSeparators = Regex("""(\d{1,5})\s*[\., ]\s*(\d{2})""")
                    regexWithSeparators.findAll(rawText).forEach { match ->
                        allNumbers.add("${match.groupValues[1]}.${match.groupValues[2]}")
                    }

                    val rawMatches = Regex("""(\d+(?:[.,]\d+)?)""").findAll(rawText)
                        .map { it.groupValues[1].replace(',', '.') }
                        .filter { (it.toDoubleOrNull() ?: 0.0) > 0 && it.length <= 6 }
                        .toList()

                    allNumbers.addAll(rawMatches)
                    detectedNumbersList = allNumbers.distinct().take(10)

                    // Аналитика: фиксация стоп-кадра и результат распознавания
                    AnalyticsManager.trackEvent(
                        AnalyticsEvents.OCR_FRAME_CAPTURED,
                        mapOf(
                            "detected_numbers_count" to detectedNumbersList.size.toString(),
                            "has_auto_price" to selectedPrice.isNotBlank().toString(),
                            "has_auto_quantity" to selectedQuantity.isNotBlank().toString()
                        )
                    )
                }
                .addOnCompleteListener { isProcessing = false }
        }
    }

    val unfreeze: () -> Unit = {
        isFrozen = false
        fullFrozenBitmap = null
        selectedPrice = ""
        selectedQuantity = ""
        selectedUnit = ProductUnit.GRAM
        detectedNumbersList = emptyList()
        activeSlot = SelectedTargetSlot.PRICE
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .onGloballyPositioned { screenSize = it.size }
    ) {
        CameraPreviewView(
            isFrozen = isFrozen,
            onCameraBound = { camera, previewView ->
                cameraInstance = camera
                previewViewRef = previewView
            },
            modifier = Modifier.pointerInput(isFrozen) {
                if (!isFrozen) {
                    detectTapGestures(
                        onTap = { offset ->
                            val view = previewViewRef ?: return@detectTapGestures
                            val point = view.meteringPointFactory.createPoint(offset.x, offset.y)
                            val action = FocusMeteringAction.Builder(point).build()
                            cameraInstance?.cameraControl?.startFocusAndMetering(action)
                        }
                    )
                }
            }
        )

        ScannerOverlayBox(
            isFrozen = isFrozen,
            frozenBitmap = fullFrozenBitmap,
            onFramePositioned = { frameRectOnScreen = it }
        )

        // Верхний бар
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilledTonalIconButton(
                onClick = handleClose,
                shape = CircleShape,
                colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = Color.Black.copy(alpha = 0.5f))
            ) {
                Text("✕", color = Color.White, fontWeight = FontWeight.Bold)
            }

            if (!isFrozen) {
                FilledTonalIconButton(
                    onClick = {
                        isFlashOn = !isFlashOn
                        cameraInstance?.cameraControl?.enableTorch(isFlashOn)
                    },
                    shape = CircleShape,
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = if (isFlashOn) MaterialTheme.colorScheme.primary else Color.Black.copy(alpha = 0.5f)
                    )
                ) {
                    Text(if (isFlashOn) "💡 Вкл" else "💡", color = Color.White)
                }
            }
        }

        // Нижняя панель действий
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.94f))
                .navigationBarsPadding()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (!isFrozen) {
                Text(
                    text = "Поместите ценник в рамку и нажмите спуск",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.LightGray
                )
                Spacer(modifier = Modifier.height(14.dp))
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .clickable { captureAndFreeze() },
                    contentAlignment = Alignment.Center
                ) {
                    Box(modifier = Modifier.size(56.dp).clip(CircleShape).border(3.dp, Color.Black, CircleShape))
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        onClick = { activeSlot = SelectedTargetSlot.PRICE },
                        shape = RoundedCornerShape(10.dp),
                        color = if (activeSlot == SelectedTargetSlot.PRICE) MaterialTheme.colorScheme.primaryContainer else Color(0xFF2C2C2E),
                        border = if (activeSlot == SelectedTargetSlot.PRICE) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = if (activeSlot == SelectedTargetSlot.PRICE) "👉 ВЫБОР ЦЕНЫ" else "ЦЕНА",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (activeSlot == SelectedTargetSlot.PRICE) MaterialTheme.colorScheme.primary else Color.LightGray
                            )
                            Text(
                                text = if (selectedPrice.isNotBlank()) "$selectedPrice ₽" else "— ₽",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (activeSlot == SelectedTargetSlot.PRICE) MaterialTheme.colorScheme.onPrimaryContainer else Color.White
                            )
                        }
                    }

                    Surface(
                        onClick = { activeSlot = SelectedTargetSlot.QUANTITY },
                        shape = RoundedCornerShape(10.dp),
                        color = if (activeSlot == SelectedTargetSlot.QUANTITY) MaterialTheme.colorScheme.primaryContainer else Color(0xFF2C2C2E),
                        border = if (activeSlot == SelectedTargetSlot.QUANTITY) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = if (activeSlot == SelectedTargetSlot.QUANTITY) "👉 ВЫБОР ВЕСА" else "ВЕС / КОЛ-ВО",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (activeSlot == SelectedTargetSlot.QUANTITY) MaterialTheme.colorScheme.primary else Color.LightGray
                            )
                            Text(
                                text = if (selectedQuantity.isNotBlank()) "$selectedQuantity ${selectedUnit.label}" else "—",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (activeSlot == SelectedTargetSlot.QUANTITY) MaterialTheme.colorScheme.onPrimaryContainer else Color.White
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                ScannerNumPad(
                    show99Button = activeSlot == SelectedTargetSlot.PRICE,
                    onKeyClick = onKeyClick
                )

                Spacer(modifier = Modifier.height(8.dp))

                if (detectedNumbersList.isNotEmpty()) {
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(detectedNumbersList) { number ->
                            val isSelected = when (activeSlot) {
                                SelectedTargetSlot.PRICE -> selectedPrice == number
                                SelectedTargetSlot.QUANTITY -> selectedQuantity == number
                            }
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    if (activeSlot == SelectedTargetSlot.PRICE) {
                                        selectedPrice = number
                                        if (selectedQuantity.isBlank()) activeSlot = SelectedTargetSlot.QUANTITY
                                    } else {
                                        selectedQuantity = number
                                    }
                                },
                                label = {
                                    Text(
                                        text = if (activeSlot == SelectedTargetSlot.PRICE) "$number ₽" else number,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = Color(0xFF2C2C2E),
                                    labelColor = Color.White,
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                )
                            )
                        }
                    }
                }

                if (activeSlot == SelectedTargetSlot.QUANTITY) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        listOf(ProductUnit.GRAM, ProductUnit.KILOGRAM, ProductUnit.MILLILITER, ProductUnit.LITER, ProductUnit.PIECE).forEach { unit ->
                            FilterChip(
                                selected = selectedUnit == unit,
                                onClick = { selectedUnit = unit },
                                label = { Text(unit.label, fontSize = 12.sp) }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = unfreeze,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Переснять", color = Color.White)
                    }

                    Button(
                        onClick = {
                            // Аналитика: перенос данных в форму
                            AnalyticsManager.trackEvent(
                                AnalyticsEvents.OCR_DATA_APPLIED,
                                mapOf(
                                    "has_price" to selectedPrice.isNotBlank().toString(),
                                    "has_quantity" to selectedQuantity.isNotBlank().toString(),
                                    AnalyticsEvents.Params.UNIT_CATEGORY to selectedUnit.category.name
                                )
                            )
                            onParsed(
                                ParsedPriceTag(
                                    price = selectedPrice.ifBlank { null },
                                    quantity = selectedQuantity.ifBlank { null },
                                    unit = if (selectedQuantity.isNotBlank()) selectedUnit else null
                                )
                            )
                            onClose()
                        },
                        enabled = selectedPrice.isNotBlank() || selectedQuantity.isNotBlank(),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Применить")
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = isProcessing,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
    }
}