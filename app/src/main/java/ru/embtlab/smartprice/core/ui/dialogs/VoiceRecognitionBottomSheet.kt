package ru.embtlab.smartprice.core.ui.dialogs

import android.content.Intent
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ru.embtlab.smartprice.presentation.theme.icons.AppIcons
import ru.embtlab.smartprice.presentation.theme.icons.Mic

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceRecognitionBottomSheet(
    targetTitle: String,
    onResult: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var recognizedText by remember { mutableStateOf("") }
    var rmsLevel by remember { mutableFloatStateOf(0f) }
    var isListening by remember { mutableStateOf(false) }
    var statusText by remember { mutableStateOf("Слушаю...") }

    // Анимация пульсации кольца вокруг микрофона
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    // Плавный отклик на громкость голоса
    val waveScale = remember { Animatable(1f) }
    LaunchedEffect(rmsLevel) {
        val target = (1f + (rmsLevel / 12f).coerceIn(0f, 0.6f))
        waveScale.animateTo(target, animationSpec = tween(80, easing = LinearEasing))
    }

    DisposableEffect(Unit) {
        var speechRecognizer: SpeechRecognizer? = null
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        isListening = true
                        statusText = "Говорите..."
                        // Мягкий сигнал готовности микрофона
                        try {
                            val toneGen = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 65)
                            toneGen.startTone(ToneGenerator.TONE_PROP_BEEP, 100)
                        } catch (_: Exception) {}
                    }

                    override fun onBeginningOfSpeech() {
                        statusText = "Записываю..."
                    }

                    override fun onRmsChanged(rmsdB: Float) {
                        rmsLevel = if (rmsdB > 0) rmsdB else 0f
                    }

                    override fun onBufferReceived(buffer: ByteArray?) {}

                    override fun onEndOfSpeech() {
                        isListening = false
                        statusText = "Обработка..."
                    }

                    override fun onError(error: Int) {
                        isListening = false
                        statusText = "Не удалось распознать"
                        coroutineScope.launch {
                            delay(600)
                            onDismiss()
                        }
                    }

                    override fun onResults(results: Bundle?) {
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val text = matches?.firstOrNull().orEmpty()
                        if (text.isNotBlank()) {
                            recognizedText = text
                            statusText = "Готово!"
                            // Чистый аккорд подтверждения успеха без обрывов
                            try {
                                val toneGen = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 80)
                                toneGen.startTone(ToneGenerator.TONE_PROP_ACK, 180)
                            } catch (_: Exception) {}

                            coroutineScope.launch {
                                delay(350)
                                onResult(text)
                                onDismiss()
                            }
                        } else {
                            onDismiss()
                        }
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        matches?.firstOrNull()?.let {
                            if (it.isNotBlank()) recognizedText = it
                        }
                    }

                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
            }

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ru-RU")
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "ru-RU")
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            }
            speechRecognizer.startListening(intent)
        } else {
            statusText = "Распознавание речи недоступно"
        }

        onDispose {
            speechRecognizer?.stopListening()
            speechRecognizer?.destroy()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Заполняем: $targetTitle",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Например: «молоко 1 литр 60 рублей»",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Визуализация микрофона со звуковыми волнами
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(110.dp)
            ) {
                // Внешний пульсирующий ореол
                Box(
                    modifier = Modifier
                        .size(90.dp)
                        .scale(if (isListening) pulseScale * waveScale.value else 1f)
                        .background(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            shape = CircleShape
                        )
                )

                // Внутренний ореол реакции на голос
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .scale(if (isListening) waveScale.value else 1f)
                        .background(
                            MaterialTheme.colorScheme.primaryContainer,
                            shape = CircleShape
                        )
                )

                // Основная кнопка/иконка
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(MaterialTheme.colorScheme.primary, shape = CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = AppIcons.Default.Mic,
                        contentDescription = "Микрофон",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = statusText,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.outline
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Поле промежуточного / финального распознанного текста
            Text(
                text = recognizedText.ifBlank { "..." },
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 17.sp),
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}