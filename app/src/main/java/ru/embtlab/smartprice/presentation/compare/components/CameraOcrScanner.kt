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
import androidx.compose.foundation.BorderStroke
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
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.launch
import ru.embtlab.smartprice.data.local.SettingsDataStore
import ru.embtlab.smartprice.data.local.TesseractManager
import ru.embtlab.smartprice.domain.model.ParsedPriceTag
import ru.embtlab.smartprice.domain.model.PriceTagLayoutProfile
import ru.embtlab.smartprice.domain.model.ProductUnit
import ru.embtlab.smartprice.domain.usecase.ParsePriceTagUseCase

private enum class SelectedTargetSlot { NAME, PRICE, QUANTITY }

@Composable
fun CameraOcrScanner(
    onParsed: (ParsedPriceTag) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()
    val settingsDataStore = remember { SettingsDataStore(context) }

    val parser = remember { ParsePriceTagUseCase() }
    val recognizer = remember { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }
    val tesseract = remember { TesseractManager(context) }

    LaunchedEffect(Unit) {
        tesseract.init()
    }

    val profileState by settingsDataStore.priceTagProfileFlow.collectAsState(initial = PriceTagLayoutProfile())
    var currentLearnedProfile by remember { mutableStateOf(PriceTagLayoutProfile()) }

    LaunchedEffect(profileState) {
        currentLearnedProfile = profileState
    }

    var cameraInstance by remember { mutableStateOf<Camera?>(null) }
    var previewViewRef by remember { mutableStateOf<PreviewView?>(null) }
    var isFlashOn by remember { mutableStateOf(false) }

    var screenSize by remember { mutableStateOf(IntSize.Zero) }
    var frameRectOnScreen by remember { mutableStateOf<Rect?>(null) }

    var isFrozen by remember { mutableStateOf(false) }
    var fullFrozenBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isProcessing by remember { mutableStateOf(false) }
    var isWordsLoading by remember { mutableStateOf(false) }

    var lastVisionText by remember { mutableStateOf<Text?>(null) }

    var detectedName by remember { mutableStateOf<String?>(null) }
    var selectedPrice by remember { mutableStateOf("") }
    var selectedQuantity by remember { mutableStateOf("") }
    var selectedUnit by remember { mutableStateOf(ProductUnit.GRAM) }

    var activeSlot by remember { mutableStateOf(SelectedTargetSlot.PRICE) }
    var detectedNumbersList by remember { mutableStateOf<List<String>>(emptyList()) }
    var detectedWordsList by remember { mutableStateOf<List<String>>(emptyList()) }

    DisposableEffect(Unit) {
        onDispose {
            recognizer.close()
            tesseract.close()
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
                    lastVisionText = visionText

                    val autoParsed = parser.parseFromVisionText(
                        visionText = visionText,
                        frameWidth = croppedBitmap.width,
                        frameHeight = croppedBitmap.height,
                        learnedProfile = currentLearnedProfile
                    )

                    if (detectedName == null) {
                        detectedName = autoParsed.name
                    }
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

                    if (selectedPrice.isNotBlank() && selectedQuantity.isBlank()) {
                        activeSlot = SelectedTargetSlot.QUANTITY
                    } else if (selectedPrice.isBlank()) {
                        activeSlot = SelectedTargetSlot.PRICE
                    }
                }
                .addOnCompleteListener {
                    isProcessing = false
                }

            // Двуязычное распознавание Tesseract (кириллица + латиница)
            coroutineScope.launch {
                isWordsLoading = true
                try {
                    val ocrMixedText = tesseract.recognizeText(croppedBitmap)
                    val wordsAndPhrases = mutableListOf<String>()

                    if (ocrMixedText.isNotBlank()) {
                        val rawLines = ocrMixedText.lines()
                            .map { it.trim() }
                            .filter { it.length >= 3 && !it.contains(Regex("""\d{10,}""")) }

                        for (line in rawLines) {
                            // Разрешаем кириллицу, латиницу и дефис
                            val clean = line.replace(Regex("""[^а-яА-ЯёЁa-zA-Z\s\-]"""), " ").trim()
                            val tokens = clean.split(Regex("""\s+""")).filter { it.length >= 2 }

                            // Собираем фразу целиком (например, "Coca-Cola" или "Шоколад Milka")
                            if (tokens.size >= 2) {
                                wordsAndPhrases.add(tokens.take(3).joinToString(" ") { token ->
                                    if (token.contains('-')) {
                                        token.split('-').joinToString("-") { part ->
                                            part.lowercase().replaceFirstChar { it.uppercase() }
                                        }
                                    } else {
                                        token.lowercase().replaceFirstChar { it.uppercase() }
                                    }
                                })
                            }
                            // Собираем отдельные слова
                            tokens.forEach { token ->
                                val formatted = if (token.contains('-')) {
                                    token.split('-').joinToString("-") { part ->
                                        part.lowercase().replaceFirstChar { it.uppercase() }
                                    }
                                } else {
                                    token.lowercase().replaceFirstChar { it.uppercase() }
                                }
                                wordsAndPhrases.add(formatted)
                            }
                        }
                    }

                    val cleanList = wordsAndPhrases.distinct().take(12)
                    if (cleanList.isNotEmpty()) {
                        detectedWordsList = cleanList
                        if (detectedName.isNullOrBlank()) {
                            detectedName = cleanList.firstOrNull()
                        }
                    } else {
                        detectedWordsList = listOf("Компот", "Сок", "Печенье", "Шоколад", "Конфеты", "Сыр", "Молоко", "Чай", "Кофе")
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    detectedWordsList = listOf("Компот", "Сок", "Печенье", "Шоколад", "Конфеты", "Сыр", "Молоко", "Чай", "Кофе")
                } finally {
                    isWordsLoading = false
                }
            }
        }
    }

    val unfreeze: () -> Unit = {
        isFrozen = false
        fullFrozenBitmap = null
        lastVisionText = null
        detectedName = null
        selectedPrice = ""
        selectedQuantity = ""
        selectedUnit = ProductUnit.GRAM
        detectedNumbersList = emptyList()
        detectedWordsList = emptyList()
        isWordsLoading = false
        activeSlot = SelectedTargetSlot.PRICE
    }

    val learnFromUserSelection: (String) -> Unit = { finalPrice ->
        val vision = lastVisionText
        if (vision != null && finalPrice.contains('.')) {
            val rublePart = finalPrice.substringBefore('.')
            val centsPart = finalPrice.substringAfter('.')

            val allLines = vision.textBlocks.flatMap { it.lines }
            val rubleBox = allLines.find { it.text.contains(rublePart) }?.boundingBox
            val centsBox = allLines.find { it.text.contains(centsPart) }?.boundingBox

            if (rubleBox != null && centsBox != null && rubleBox != centsBox) {
                val observedHeightRatio = centsBox.height().toFloat() / rubleBox.height().coerceAtLeast(1)
                val observedOffsetRatio = (centsBox.left - rubleBox.right).toFloat() / rubleBox.width().coerceAtLeast(1)

                val updatedProfile = currentLearnedProfile.updateWithSample(
                    newHeightRatio = observedHeightRatio,
                    newOffsetRatio = observedOffsetRatio
                )
                currentLearnedProfile = updatedProfile

                coroutineScope.launch {
                    settingsDataStore.savePriceTagProfile(updatedProfile)
                }
            }
        }
    }

    val onKeyClick: (String) -> Unit = { key ->
        when (activeSlot) {
            SelectedTargetSlot.NAME -> {
                if (key == "⌫") {
                    val current = detectedName ?: ""
                    detectedName = if (current.isNotEmpty()) current.dropLast(1) else null
                } else if (key == "✕") {
                    detectedName = null
                }
            }
            SelectedTargetSlot.PRICE, SelectedTargetSlot.QUANTITY -> {
                val currentVal = if (activeSlot == SelectedTargetSlot.PRICE) selectedPrice else selectedQuantity
                val newVal = when (key) {
                    "⌫" -> if (currentVal.isNotEmpty()) currentVal.dropLast(1) else ""
                    "✕" -> ""
                    "." -> {
                        if (currentVal.contains('.')) currentVal
                        else if (currentVal.isEmpty()) "0."
                        else "$currentVal."
                    }
                    ".99" -> {
                        val integerPart = currentVal.substringBefore('.')
                        if (integerPart.isNotEmpty()) "$integerPart.99" else "0.99"
                    }
                    else -> {
                        if (currentVal.length < 7) currentVal + key else currentVal
                    }
                }

                if (activeSlot == SelectedTargetSlot.PRICE) {
                    selectedPrice = newVal
                } else {
                    selectedQuantity = newVal
                }
            }
        }
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
                        text = if (currentLearnedProfile.samplesCount > 0) {
                            "Профиль: обучен (${currentLearnedProfile.samplesCount})"
                        } else {
                            "Кадр зафиксирован"
                        },
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
                .background(Color.Black.copy(alpha = 0.92f))
                .navigationBarsPadding()
                .padding(14.dp),
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
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .border(3.dp, Color.Black, CircleShape)
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        onClick = { activeSlot = SelectedTargetSlot.NAME },
                        shape = RoundedCornerShape(10.dp),
                        color = if (activeSlot == SelectedTargetSlot.NAME) MaterialTheme.colorScheme.primaryContainer else Color.DarkGray,
                        border = if (activeSlot == SelectedTargetSlot.NAME) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                        modifier = Modifier.weight(1.2f)
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text(
                                text = if (activeSlot == SelectedTargetSlot.NAME) "👉 ТОВАР" else "ТОВАР",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (activeSlot == SelectedTargetSlot.NAME) MaterialTheme.colorScheme.primary else Color.LightGray
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = detectedName?.ifBlank { "—" } ?: "—",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                color = if (activeSlot == SelectedTargetSlot.NAME) MaterialTheme.colorScheme.onPrimaryContainer else Color.White
                            )
                        }
                    }

                    Surface(
                        onClick = { activeSlot = SelectedTargetSlot.PRICE },
                        shape = RoundedCornerShape(10.dp),
                        color = if (activeSlot == SelectedTargetSlot.PRICE) MaterialTheme.colorScheme.primaryContainer else Color.DarkGray,
                        border = if (activeSlot == SelectedTargetSlot.PRICE) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text(
                                text = if (activeSlot == SelectedTargetSlot.PRICE) "👉 ЦЕНА" else "ЦЕНА",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (activeSlot == SelectedTargetSlot.PRICE) MaterialTheme.colorScheme.primary else Color.LightGray
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (selectedPrice.isNotBlank()) "$selectedPrice ₽" else "— ₽",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (activeSlot == SelectedTargetSlot.PRICE) MaterialTheme.colorScheme.onPrimaryContainer else Color.White
                            )
                        }
                    }

                    Surface(
                        onClick = { activeSlot = SelectedTargetSlot.QUANTITY },
                        shape = RoundedCornerShape(10.dp),
                        color = if (activeSlot == SelectedTargetSlot.QUANTITY) MaterialTheme.colorScheme.primaryContainer else Color.DarkGray,
                        border = if (activeSlot == SelectedTargetSlot.QUANTITY) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text(
                                text = if (activeSlot == SelectedTargetSlot.QUANTITY) "👉 ВЕС" else "ВЕС",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (activeSlot == SelectedTargetSlot.QUANTITY) MaterialTheme.colorScheme.primary else Color.LightGray
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (selectedQuantity.isNotBlank()) "$selectedQuantity ${selectedUnit.label}" else "—",
                                fontSize = 16.sp,
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
                            colors = ButtonDefaults.filledTonalButtonColors(containerColor = Color.DarkGray.copy(alpha = 0.8f)),
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .padding(horizontal = 1.5.dp)
                        ) {
                            Text(digit, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
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
                        modifier = Modifier.weight(1f).height(36.dp)
                    ) {
                        Text("• Точка", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }

                    if (activeSlot == SelectedTargetSlot.PRICE) {
                        FilledTonalButton(
                            onClick = { onKeyClick(".99") },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).height(36.dp)
                        ) {
                            Text(".99", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    FilledTonalButton(
                        onClick = { onKeyClick("⌫") },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f).height(36.dp)
                    ) {
                        Text("⌫ Стереть", fontSize = 13.sp)
                    }

                    OutlinedButton(
                        onClick = { onKeyClick("✕") },
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(0.dp),
                        modifier = Modifier.width(44.dp).height(36.dp)
                    ) {
                        Text("✕", fontSize = 13.sp, color = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (activeSlot == SelectedTargetSlot.NAME) {
                    Text(
                        text = "Слова с ценника (кликните, чтобы задать название):",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.LightGray,
                        modifier = Modifier.align(Alignment.Start)
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    if (isWordsLoading) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Распознавание текста...",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.LightGray
                            )
                        }
                    } else if (detectedWordsList.isNotEmpty()) {
                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(detectedWordsList) { wordPhrase ->
                                FilterChip(
                                    selected = detectedName == wordPhrase,
                                    onClick = {
                                        detectedName = wordPhrase
                                        if (selectedPrice.isBlank()) activeSlot = SelectedTargetSlot.PRICE
                                    },
                                    label = { Text(wordPhrase, fontWeight = FontWeight.Bold) }
                                )
                            }
                        }
                    } else {
                        Text(
                            text = "Слова не распознаны",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                } else {
                    Text(
                        text = if (activeSlot == SelectedTargetSlot.PRICE) "Числа с ценника (кликните для ЦЕНЫ):" else "Числа с ценника (кликните для ВЕСА):",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.LightGray,
                        modifier = Modifier.align(Alignment.Start)
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    if (detectedNumbersList.isNotEmpty()) {
                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(detectedNumbersList) { number ->
                                val isSelected = when (activeSlot) {
                                    SelectedTargetSlot.PRICE -> selectedPrice == number
                                    SelectedTargetSlot.QUANTITY -> selectedQuantity == number
                                    else -> false
                                }

                                FilterChip(
                                    selected = isSelected,
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
                    }
                }

                if (activeSlot == SelectedTargetSlot.QUANTITY) {
                    Spacer(modifier = Modifier.height(6.dp))
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
                                modifier = Modifier.height(30.dp)
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
                            learnFromUserSelection(selectedPrice)
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
                        enabled = selectedPrice.isNotBlank() || selectedQuantity.isNotBlank() || !detectedName.isNullOrBlank(),
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