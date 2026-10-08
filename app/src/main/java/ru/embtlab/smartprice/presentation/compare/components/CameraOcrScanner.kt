package ru.embtlab.smartprice.presentation.compare.components

import android.graphics.Bitmap
import android.graphics.Rect
import android.view.ViewGroup
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

    // Данные для полей
    var detectedName by remember { mutableStateOf<String?>(null) }
    var selectedPrice by remember { mutableStateOf("") }
    var selectedQuantity by remember { mutableStateOf("") }
    var selectedUnit by remember { mutableStateOf(ProductUnit.GRAM) }

    // Активный слот: куда отправлять число при клике на чип
    var activeSlot by remember { mutableStateOf(SelectedTargetSlot.PRICE) }

    // Единый список всех найденных чисел на ценнике
    var detectedNumbersList by remember { mutableStateOf<List<String>>(emptyList()) }

    DisposableEffect(Unit) {
        onDispose { recognizer.close() }
    }

    val captureAndFreeze: () -> Unit = {
        val previewView = previewViewRef
        val bitmap = previewView?.bitmap
        val rect = frameRectOnScreen

        if (bitmap != null && rect != null && !isProcessing) {
            isProcessing = true
            fullFrozenBitmap = bitmap
            isFrozen = true

            val scaleX = bitmap.width.toFloat() / screenSize.width.coerceAtLeast(1)
            val scaleY = bitmap.height.toFloat() / screenSize.height.coerceAtLeast(1)

            val cropLeft = ((rect.left * scaleX) - (bitmap.width * 0.04f)).toInt().coerceAtLeast(0)
            val cropTop = ((rect.top * scaleY) - (bitmap.height * 0.04f)).toInt().coerceAtLeast(0)
            val cropWidth = ((rect.width() * scaleX) + (bitmap.width * 0.08f)).toInt().coerceAtMost(bitmap.width - cropLeft)
            val cropHeight = ((rect.height() * scaleY) + (bitmap.height * 0.08f)).toInt().coerceAtMost(bitmap.height - cropTop)

            val croppedBitmap = try {
                Bitmap.createBitmap(bitmap, cropLeft, cropTop, cropWidth, cropHeight)
            } catch (e: Exception) {
                bitmap
            }

            val inputImage = InputImage.fromBitmap(croppedBitmap, 0)
            recognizer.process(inputImage)
                .addOnSuccessListener { visionText ->
                    // 1. Автоматический разбор с нечётким поиском и склейкой надстрочных копеек
                    val autoParsed = parser.parseFromVisionText(visionText)
                    detectedName = autoParsed.name
                    selectedPrice = autoParsed.price ?: ""
                    selectedQuantity = autoParsed.quantity ?: ""
                    if (autoParsed.unit != null) {
                        selectedUnit = autoParsed.unit
                    }

                    val rawText = visionText.text

                    // 2. Сбор всех чисел ценника в единую ленту
                    val allNumbers = mutableListOf<String>()

                    // Первым кандидатом выставляем склеенную цену (например, 229.99)
                    if (selectedPrice.isNotBlank()) {
                        allNumbers.add(selectedPrice)
                    }

                    // Числа со спец-разделителями (квадратные точки, тире, запятые)
                    val regexWithSeparators = Regex("""(\d{1,5})\s*[\.,■•·\-–]\s*(\d{2})""")
                    regexWithSeparators.findAll(rawText).forEach { match ->
                        allNumbers.add("${match.groupValues[1]}.${match.groupValues[2]}")
                    }

                    // Любые изолированные последовательности цифр
                    val extractedNumbers = Regex("""(\d+(?:[.,]\d+)?)""")
                        .findAll(rawText)
                        .map { it.groupValues[1].replace(',', '.') }
                        .filter {
                            val num = it.toDoubleOrNull() ?: 0.0
                            num > 0 && it.length <= 6
                        }
                        .toList()

                    allNumbers.addAll(extractedNumbers)

                    detectedNumbersList = allNumbers.distinct().take(10)

                    // Переключаем фокус на вес, если цена определена автоматически
                    if (selectedPrice.isNotBlank() && selectedQuantity.isBlank()) {
                        activeSlot = SelectedTargetSlot.QUANTITY
                    } else if (selectedPrice.isBlank()) {
                        activeSlot = SelectedTargetSlot.PRICE
                    }
                }
                .addOnCompleteListener { isProcessing = false }
        }
    }

    val unfreeze: () -> Unit = {
        isFrozen = false
        fullFrozenBitmap = null
        detectedName = null
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
        // Видоискатель CameraX
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

        // Замороженный стоп-кадр
        if (isFrozen && fullFrozenBitmap != null) {
            Image(
                bitmap = fullFrozenBitmap!!.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)))
        }

        // Рамка сканирования по центру экрана
        Box(
            modifier = Modifier
                .size(width = 320.dp, height = 160.dp)
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

        // Верхняя панель: закрыть, фонарик, статус
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

        // Нижняя панель действий и сопоставления
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.90f))
                .navigationBarsPadding()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (!isFrozen) {
                Text(
                    text = "Поместите ценник в рамку и нажмите съемку",
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
                // Слоты: ЦЕНА и КОЛИЧЕСТВО / ВЕС
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        onClick = { activeSlot = SelectedTargetSlot.PRICE },
                        shape = RoundedCornerShape(10.dp),
                        color = if (activeSlot == SelectedTargetSlot.PRICE) MaterialTheme.colorScheme.primaryContainer else Color.DarkGray,
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = if (activeSlot == SelectedTargetSlot.PRICE) "👉 ВЫБОР ЦЕНЫ" else "ЦЕНА",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (activeSlot == SelectedTargetSlot.PRICE) MaterialTheme.colorScheme.primary else Color.LightGray
                            )
                            Text(
                                text = if (selectedPrice.isNotBlank()) "$selectedPrice ₽" else "—",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (activeSlot == SelectedTargetSlot.PRICE) MaterialTheme.colorScheme.onPrimaryContainer else Color.White
                            )
                        }
                    }

                    Surface(
                        onClick = { activeSlot = SelectedTargetSlot.QUANTITY },
                        shape = RoundedCornerShape(10.dp),
                        color = if (activeSlot == SelectedTargetSlot.QUANTITY) MaterialTheme.colorScheme.primaryContainer else Color.DarkGray,
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = if (activeSlot == SelectedTargetSlot.QUANTITY) "👉 ВЫБОР ВЕСА" else "ВЕС / КОЛ-ВО",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (activeSlot == SelectedTargetSlot.QUANTITY) MaterialTheme.colorScheme.primary else Color.LightGray
                            )
                            Text(
                                text = if (selectedQuantity.isNotBlank()) "$selectedQuantity ${selectedUnit.label}" else "—",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (activeSlot == SelectedTargetSlot.QUANTITY) MaterialTheme.colorScheme.onPrimaryContainer else Color.White
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Лента найденных чисел
                Text(
                    text = if (activeSlot == SelectedTargetSlot.PRICE) "Нажмите на число, чтобы задать ЦЕНУ:" else "Нажмите на число, чтобы задать ВЕС:",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.LightGray,
                    modifier = Modifier.align(Alignment.Start)
                )
                Spacer(modifier = Modifier.height(6.dp))

                if (detectedNumbersList.isNotEmpty()) {
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(detectedNumbersList) { number ->
                            val isSelectedInCurrentSlot = when (activeSlot) {
                                SelectedTargetSlot.PRICE -> selectedPrice == number
                                SelectedTargetSlot.QUANTITY -> selectedQuantity == number
                            }

                            FilterChip(
                                selected = isSelectedInCurrentSlot,
                                onClick = {
                                    if (activeSlot == SelectedTargetSlot.PRICE) {
                                        selectedPrice = number
                                        if (selectedQuantity.isBlank()) {
                                            activeSlot = SelectedTargetSlot.QUANTITY
                                        }
                                    } else {
                                        selectedQuantity = number
                                    }
                                },
                                label = {
                                    Text(
                                        text = if (activeSlot == SelectedTargetSlot.PRICE) "$number ₽" else number,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }
                            )
                        }
                    }
                } else {
                    Text(
                        text = "Числа не найдены. Попробуйте переснять ближе.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }

                // Переключатели единиц измерения для слота веса
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
                                label = { Text(unit.label, fontSize = 11.sp) },
                                modifier = Modifier.height(32.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Кнопки управления
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
                                    name = detectedName,
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