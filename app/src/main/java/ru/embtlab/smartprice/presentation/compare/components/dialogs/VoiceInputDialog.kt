package ru.embtlab.smartprice.presentation.compare.components.dialogs

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.delay
import ru.embtlab.smartprice.domain.usecase.ParseVoiceInputUseCase
import ru.embtlab.smartprice.presentation.compare.util.VoiceInputManager
import ru.embtlab.smartprice.presentation.compare.util.VoiceRecognitionState
import ru.embtlab.smartprice.presentation.theme.icons.AppIcons
import ru.embtlab.smartprice.presentation.theme.icons.Mic

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VoiceInputDialog(
    onResult: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val voiceManager = remember { VoiceInputManager(context) }
    val parser = remember { ParseVoiceInputUseCase() }
    val state by voiceManager.state.collectAsState()

    var liveParsedPreview by remember { mutableStateOf(parser("")) }
    var activeSpokenText by remember { mutableStateOf("") }

    DisposableEffect(Unit) {
        voiceManager.startListening()
        onDispose { voiceManager.stopListening() }
    }

    // Реакция на изменение распознаваемого текста на лету
    LaunchedEffect(state) {
        when (val s = state) {
            is VoiceRecognitionState.Listening -> {
                if (s.partialText.isNotBlank()) {
                    activeSpokenText = s.partialText
                    liveParsedPreview = parser(s.partialText)
                }
            }
            is VoiceRecognitionState.Success -> {
                activeSpokenText = s.text
                liveParsedPreview = parser(s.text)
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                delay(350) // Небольшая задержка, чтобы пользователь увидел финальный чипс
                onResult(s.text)
                onDismiss()
            }
            else -> {}
        }
    }

    val rms = (state as? VoiceRecognitionState.Listening)?.rmsDb ?: 0f
    val scale by animateFloatAsState(
        targetValue = 1f + (rms / 10f).coerceIn(0f, 0.45f),
        label = "MicPulse"
    )

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Голосовой ввод",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                // Пульсирующая иконка микрофона
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .scale(scale)
                        .background(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = AppIcons.Default.Mic,
                        contentDescription = "Микрофон",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(34.dp)
                    )
                }

                // Живая строка расшифровки речи
                Text(
                    text = when {
                        state is VoiceRecognitionState.Error -> (state as VoiceRecognitionState.Error).message
                        activeSpokenText.isNotBlank() -> "«$activeSpokenText»"
                        state is VoiceRecognitionState.Ready -> "Слушаю... Говорите цену или вес"
                        else -> "Инициализация микрофона..."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (activeSpokenText.isNotBlank()) FontWeight.SemiBold else FontWeight.Normal,
                    textAlign = TextAlign.Center,
                    color = if (state is VoiceRecognitionState.Error) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                // Живые чипсы распознанных слотов
                AnimatedVisibility(
                    visible = liveParsedPreview.price != null || liveParsedPreview.quantity != null || liveParsedPreview.name != null,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        liveParsedPreview.name?.let { name ->
                            SuggestionChip(
                                onClick = {},
                                label = { Text("Товар: $name", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                                colors = SuggestionChipDefaults.suggestionChipColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                                )
                            )
                        }
                        liveParsedPreview.price?.let { price ->
                            SuggestionChip(
                                onClick = {},
                                label = { Text("Цена: $price ₽", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                                colors = SuggestionChipDefaults.suggestionChipColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer
                                )
                            )
                        }
                        liveParsedPreview.quantity?.let { qty ->
                            val unitLabel = liveParsedPreview.unit?.label ?: "г"
                            SuggestionChip(
                                onClick = {},
                                label = { Text("Вес: $qty $unitLabel", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                                colors = SuggestionChipDefaults.suggestionChipColors(
                                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Отмена")
                    }

                    if (activeSpokenText.isNotBlank()) {
                        Button(
                            onClick = {
                                onResult(activeSpokenText)
                                onDismiss()
                            }
                        ) {
                            Text("Применить")
                        }
                    }
                }
            }
        }
    }
}