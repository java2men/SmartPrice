package ru.embtlab.smartprice.presentation.compare.components

import android.graphics.Bitmap
import android.graphics.Rect
import android.view.ViewGroup
import androidx.activity.compose.BackHandler
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import ru.embtlab.smartprice.domain.model.ParsedPriceTag
import ru.embtlab.smartprice.domain.model.ProductUnit
import ru.embtlab.smartprice.domain.usecase.ParsePriceTagUseCase

private enum class SelectedTargetSlot { PRICE, QUANTITY }

@Composable
fun CameraOcrScanner(
    onParsed: (ParsedPriceTag) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onClose)

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val parser = remember { ParsePriceTagUseCase() }
    val recognizer = remember { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }

    var cameraInstance by remember { mutableStateOf<Camera?>(null) }
    var previewViewRef by remember { mutableStateOf<PreviewView?>(null) }
    var isFlashOn by remember { mutableStateOf(false) }

    var screenSize by remember { mutableStateOf(IntSize.Zero) }
    var frameRectOnScreen by remember { mutableStateOf<Rect?>(null) }

    var isFrozen by remember { mutableStateOf(false) }
    var fullFrozenBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isProcessing by remember { mutableStateOf(false) }

    var selectedPrice by remember { mutableStateOf("") }
    var selectedQuantity by remember { mutableStateOf("") }
    var selectedUnit by remember { mutableStateOf(ProductUnit.GRAM) }

    var activeSlot by remember { mutableStateOf(SelectedTargetSlot.PRICE) }
    var detectedNumbersList by remember { mutableStateOf<List<String>>(emptyList()) }

    // Универсальный флаг ожидания ввода дробной части (копеек для цены или долей для кол-ва)
    var isAwaitingDecimals by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onDispose { recognizer.close() }
    }

    val onKeyClick: (String) -> Unit = { key ->
        val currentVal = if (activeSlot == SelectedTargetSlot.PRICE) selectedPrice else selectedQuantity
        val maxDecimals = if (activeSlot == SelectedTargetSlot.PRICE) 2 else 3

        val newVal = when (key) {
            "⌫" -> {
                val res = if (currentVal.isNotEmpty()) currentVal.dropLast(1) else ""
                if (!res.contains('.')) isAwaitingDecimals = false
                res
            }
            "C" -> {
                isAwaitingDecimals = false
                ""
            }
            "." -> {
                if (currentVal.contains('.')) {
                    currentVal
                } else {
                    isAwaitingDecimals = true
                    if (currentVal.isEmpty()) "0." else "$currentVal."
                }
            }
            ".99" -> {
                isAwaitingDecimals = false
                val integerPart = currentVal.substringBefore('.')
                if (integerPart.isNotEmpty()) "$integerPart.99" else "0.99"
            }
            else -> {
                if (isAwaitingDecimals) {
                    val base = currentVal.substringBefore('.')
                    val frac = currentVal.substringAfter('.', "") + key
                    if (frac.length >= maxDecimals) isAwaitingDecimals = false
                    "$base.${frac.take(maxDecimals)}"
                } else {
                    if (currentVal.length < 7) currentVal + key else currentVal
                }
            }
        }

        if (activeSlot == SelectedTargetSlot.PRICE) {
            selectedPrice = newVal
        } else {
            selectedQuantity = newVal
        }
    }

    val captureAndFreeze: () -> Unit = {
        val previewView = previewViewRef
        val bitmap = previewView?.bitmap
        val rect = frameRectOnScreen

        if (bitmap != null && rect != null && !isProcessing) {
            isProcessing = true
            fullFrozenBitmap = bitmap
            isFrozen = true
            isAwaitingDecimals = false
            activeSlot = SelectedTargetSlot.PRICE

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
                    selectedPrice = autoParsed.price ?: ""
                    selectedQuantity = autoParsed.quantity ?: ""
                    if (autoParsed.unit != null) {
                        selectedUnit = autoParsed.unit
                    }

                    val rawText = visionText.text
                    val allNumbers = mutableListOf<String>()

                    if (selectedPrice.isNotBlank()) {
                        allNumbers.add(selectedPrice)
                    }

                    val regexWithSeparators = Regex("""(\d{1,5})\s*[\.,■▪•·\-–—]\s*(\d{2})""")
                    regexWithSeparators.findAll(rawText).forEach { match ->
                        allNumbers.add("${match.groupValues[1]}.${match.groupValues[2]}")
                    }

                    val rawMatches = Regex("""(\d+(?:[.,]\d+)?)""").findAll(rawText)
                        .map { it.groupValues[1].replace(',', '.') }
                        .filter {
                            val num = it.toDoubleOrNull() ?: 0.0
                            num > 0 && it.length <= 6
                        }
                        .toList()

                    allNumbers.addAll(rawMatches)
                    detectedNumbersList = allNumbers.distinct().take(10)
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
        isAwaitingDecimals = false
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .onGloballyPositioned { screenSize = it.size }
    ) {
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
                previewViewRef = previewView

                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }
                    try {
                        cameraProvider.unbindAll()
                        cameraInstance = cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            CameraSelector.DEFAULT_BACK_CAMERA,
                            preview
                        )
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }, ContextCompat.getMainExecutor(ctx))
                previewView
            },
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(isFrozen) {
                    if (!isFrozen) {
                        detectTapGestures(
                            onTap = { offset ->
                                val view = previewViewRef ?: return@detectTapGestures
                                val factory = view.meteringPointFactory
                                val point = factory.createPoint(offset.x, offset.y)
                                val action = FocusMeteringAction.Builder(point).build()
                                cameraInstance?.cameraControl?.startFocusAndMetering(action)
                            }
                        )
                    }
                }
        )

        if (isFrozen && fullFrozenBitmap != null) {
            Image(
                bitmap = fullFrozenBitmap!!.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.40f)))
        }

        Box(
            modifier = Modifier
                .size(width = 310.dp, height = 150.dp)
                .align(Alignment.Center)
                .onGloballyPositioned { coords ->
                    val pos = coords.positionInRoot()
                    frameRectOnScreen = Rect(
                        pos.x.toInt(),
                        pos.y.toInt(),
                        (pos.x + coords.size.width).toInt(),
                        (pos.y + coords.size.height).toInt()
                    )
                }
                .border(
                    width = 2.dp,
                    color = if (isFrozen) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.85f),
                    shape = RoundedCornerShape(12.dp)
                )
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilledTonalIconButton(
                onClick = onClose,
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
            } else {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = "Кадр зафиксирован",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }

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
                    // Слот ЦЕНА
                    Surface(
                        onClick = {
                            if (activeSlot != SelectedTargetSlot.PRICE) {
                                activeSlot = SelectedTargetSlot.PRICE
                                isAwaitingDecimals = false
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        color = if (activeSlot == SelectedTargetSlot.PRICE) MaterialTheme.colorScheme.primaryContainer else Color(0xFF2C2C2E),
                        border = if (activeSlot == SelectedTargetSlot.PRICE) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = if (activeSlot == SelectedTargetSlot.PRICE) {
                                    if (isAwaitingDecimals) "👉 КОПЕЙКИ" else "👉 ВЫБОР ЦЕНЫ"
                                } else "ЦЕНА",
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

                    // Слот КОЛИЧЕСТВО
                    Surface(
                        onClick = {
                            if (activeSlot != SelectedTargetSlot.QUANTITY) {
                                activeSlot = SelectedTargetSlot.QUANTITY
                                isAwaitingDecimals = false
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        color = if (activeSlot == SelectedTargetSlot.QUANTITY) MaterialTheme.colorScheme.primaryContainer else Color(0xFF2C2C2E),
                        border = if (activeSlot == SelectedTargetSlot.QUANTITY) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = if (activeSlot == SelectedTargetSlot.QUANTITY) {
                                    if (isAwaitingDecimals) "👉 ДРОБНАЯ ЧАСТЬ" else "👉 ВЫБОР КОЛ-ВА"
                                } else "КОЛ-ВО",
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

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0").forEach { digit ->
                        FilledTonalButton(
                            onClick = { onKeyClick(digit) },
                            contentPadding = PaddingValues(0.dp),
                            shape = RoundedCornerShape(6.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = Color(0xFF3A3A3C),
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .padding(horizontal = 1.5.dp)
                        ) {
                            Text(digit, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilledTonalButton(
                        onClick = { onKeyClick(".") },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = if (isAwaitingDecimals) MaterialTheme.colorScheme.primary else Color(0xFF3A3A3C),
                            contentColor = if (isAwaitingDecimals) MaterialTheme.colorScheme.onPrimary else Color.White
                        ),
                        modifier = Modifier.weight(1f).height(36.dp)
                    ) {
                        Text("• Точка", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }

                    if (activeSlot == SelectedTargetSlot.PRICE) {
                        FilledTonalButton(
                            onClick = { onKeyClick(".99") },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(containerColor = Color(0xFF3A3A3C), contentColor = Color.White),
                            modifier = Modifier.weight(1f).height(36.dp)
                        ) {
                            Text(".99", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    FilledTonalButton(
                        onClick = { onKeyClick("⌫") },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(containerColor = Color(0xFF3A3A3C), contentColor = Color.White),
                        modifier = Modifier.weight(1f).height(36.dp)
                    ) {
                        Text("⌫ Стереть", fontSize = 13.sp)
                    }

                    FilledTonalButton(
                        onClick = { onKeyClick("C") },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(containerColor = Color(0xFF48484A), contentColor = Color.White),
                        modifier = Modifier.weight(0.9f).height(36.dp)
                    ) {
                        Text("Очистить", fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Динамическая подсказка в зависимости от слота и режима точки
                val promptText = when {
                    activeSlot == SelectedTargetSlot.PRICE && isAwaitingDecimals -> "Тапните число для копеек (после точки):"
                    activeSlot == SelectedTargetSlot.QUANTITY && isAwaitingDecimals -> "Тапните число для дробной части (после точки):"
                    activeSlot == SelectedTargetSlot.PRICE -> "Тапните число для цены:"
                    else -> "Тапните число для количества:"
                }

                Text(
                    text = promptText,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isAwaitingDecimals) MaterialTheme.colorScheme.primary else Color.LightGray,
                    fontWeight = if (isAwaitingDecimals) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier.align(Alignment.Start)
                )
                Spacer(modifier = Modifier.height(6.dp))

                if (detectedNumbersList.isNotEmpty()) {
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(detectedNumbersList) { number ->
                            val isSelected = when (activeSlot) {
                                SelectedTargetSlot.PRICE -> {
                                    if (isAwaitingDecimals) {
                                        selectedPrice.substringAfter('.', "") == number.filter { it.isDigit() }.take(2)
                                    } else {
                                        selectedPrice == number
                                    }
                                }
                                SelectedTargetSlot.QUANTITY -> {
                                    if (isAwaitingDecimals) {
                                        selectedQuantity.substringAfter('.', "") == number.filter { it.isDigit() }.take(3)
                                    } else {
                                        selectedQuantity == number
                                    }
                                }
                            }

                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    if (isAwaitingDecimals) {
                                        val digitsOnly = number.filter { it.isDigit() }
                                        if (activeSlot == SelectedTargetSlot.PRICE) {
                                            val centsPart = digitsOnly.take(2)
                                            val rublesPart = selectedPrice.substringBefore('.')
                                            selectedPrice = "$rublesPart.$centsPart"
                                        } else {
                                            val fracPart = digitsOnly.take(3)
                                            val wholePart = selectedQuantity.substringBefore('.')
                                            selectedQuantity = "$wholePart.$fracPart"
                                        }
                                        isAwaitingDecimals = false
                                    } else {
                                        if (activeSlot == SelectedTargetSlot.PRICE) {
                                            selectedPrice = number
                                        } else {
                                            selectedQuantity = number
                                        }
                                    }
                                },
                                label = {
                                    Text(
                                        text = if (activeSlot == SelectedTargetSlot.PRICE && !isAwaitingDecimals) "$number ₽" else number,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = Color(0xFF2C2C2E),
                                    labelColor = Color.White,
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = isSelected,
                                    borderColor = Color(0xFF545458),
                                    selectedBorderColor = MaterialTheme.colorScheme.primary
                                )
                            )
                        }
                    }
                } else {
                    Text(
                        text = "Числа не обнаружены",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }

                if (activeSlot == SelectedTargetSlot.QUANTITY) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.Start),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Единица:", style = MaterialTheme.typography.labelSmall, color = Color.LightGray)
                        listOf(
                            ProductUnit.GRAM,
                            ProductUnit.KILOGRAM,
                            ProductUnit.MILLILITER,
                            ProductUnit.LITER,
                            ProductUnit.PIECE
                        ).forEach { unit ->
                            FilterChip(
                                selected = selectedUnit == unit,
                                onClick = { selectedUnit = unit },
                                label = { Text(unit.label, fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = Color(0xFF2C2C2E),
                                    labelColor = Color.White,
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = selectedUnit == unit,
                                    borderColor = Color(0xFF545458),
                                    selectedBorderColor = MaterialTheme.colorScheme.primary
                                ),
                                modifier = Modifier.height(32.dp)
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