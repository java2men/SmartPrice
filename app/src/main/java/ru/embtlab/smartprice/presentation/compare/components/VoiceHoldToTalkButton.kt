package ru.embtlab.smartprice.presentation.compare.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.embtlab.smartprice.presentation.compare.util.VoiceInputManager
import ru.embtlab.smartprice.presentation.compare.util.VoiceRecognitionState
import ru.embtlab.smartprice.presentation.theme.icons.AppIcons
import ru.embtlab.smartprice.presentation.theme.icons.Close
import ru.embtlab.smartprice.presentation.theme.icons.Mic
import kotlin.math.roundToInt

@Composable
fun VoiceHoldToTalkButton(
    targetCardTitle: String,
    onStartRecording: () -> Unit,
    onStopRecording: (String) -> Unit,
    onCancelRecording: () -> Unit,
    onRequireAudioPermission: () -> Unit,
    hasAudioPermission: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val voiceManager = remember { VoiceInputManager(context) }
    val state by voiceManager.state.collectAsState()

    var isHolding by remember { mutableStateOf(false) }
    var dragOffsetX by remember { mutableFloatStateOf(0f) }
    var isCancelledBySwipe by remember { mutableStateOf(false) }
    var displayedSpokenText by remember { mutableStateOf("") }

    val cancelThresholdPx = -160f

    DisposableEffect(Unit) {
        onDispose { voiceManager.destroy() }
    }

    // Слушаем состояние: когда движок завершил распознавание — отправляем результат!
    LaunchedEffect(state) {
        when (val s = state) {
            is VoiceRecognitionState.Listening -> {
                displayedSpokenText = s.partialText
            }
            is VoiceRecognitionState.Success -> {
                displayedSpokenText = s.text
                if (!isCancelledBySwipe && s.text.isNotBlank()) {
                    onStopRecording(s.text)
                }
                voiceManager.resetState()
            }
            else -> {}
        }
    }

    val rms = (state as? VoiceRecognitionState.Listening)?.rmsDb ?: 0f
    val micScale by animateFloatAsState(
        targetValue = if (isHolding) 1f + (rms / 12f).coerceIn(0f, 0.45f) else 1f,
        label = "MicPulse"
    )

    Box(
        modifier = modifier,
        contentAlignment = Alignment.BottomEnd
    ) {
        AnimatedVisibility(
            visible = isHolding,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut(),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(y = (-68).dp)
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = if (isCancelledBySwipe) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceVariant,
                tonalElevation = 6.dp,
                shadowElevation = 8.dp,
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .padding(horizontal = 16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(if (isCancelledBySwipe) MaterialTheme.colorScheme.error else Color.Red, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isCancelledBySwipe) {
                                "Отпустите для отмены"
                            } else if (displayedSpokenText.isNotBlank()) {
                                displayedSpokenText
                            } else {
                                "Заполняем: $targetCardTitle..."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isCancelledBySwipe) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "⌫ Влево — отмена",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }

        FloatingActionButton(
            onClick = {
                if (!hasAudioPermission) {
                    onRequireAudioPermission()
                }
            },
            shape = CircleShape,
            containerColor = if (isHolding) {
                if (isCancelledBySwipe) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.secondaryContainer
            },
            contentColor = if (isHolding) {
                if (isCancelledBySwipe) MaterialTheme.colorScheme.error else Color.White
            } else {
                MaterialTheme.colorScheme.onSecondaryContainer
            },
            elevation = FloatingActionButtonDefaults.elevation(
                defaultElevation = if (isHolding) 10.dp else 4.dp
            ),
            modifier = Modifier
                .offset { IntOffset(dragOffsetX.roundToInt(), 0) }
                .scale(micScale)
                .pointerInput(hasAudioPermission) {
                    if (!hasAudioPermission) return@pointerInput

                    detectDragGesturesAfterLongPress(
                        onDragStart = {
                            isHolding = true
                            isCancelledBySwipe = false
                            dragOffsetX = 0f
                            displayedSpokenText = ""
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onStartRecording()
                            voiceManager.startListening()
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            dragOffsetX = (dragOffsetX + dragAmount.x).coerceAtMost(0f)
                            if (dragOffsetX < cancelThresholdPx && !isCancelledBySwipe) {
                                isCancelledBySwipe = true
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            } else if (dragOffsetX >= cancelThresholdPx && isCancelledBySwipe) {
                                isCancelledBySwipe = false
                            }
                        },
                        onDragEnd = {
                            isHolding = false
                            dragOffsetX = 0f
                            if (isCancelledBySwipe) {
                                voiceManager.cancel()
                                onCancelRecording()
                            } else {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                // Говорим менеджеру остановиться. Результат придёт в LaunchedEffect(state)
                                voiceManager.stopListeningAndCommit()
                            }
                        },
                        onDragCancel = {
                            isHolding = false
                            dragOffsetX = 0f
                            voiceManager.cancel()
                            onCancelRecording()
                        }
                    )
                }
        ) {
            Icon(
                imageVector = if (isCancelledBySwipe) AppIcons.Default.Close else AppIcons.Default.Mic,
                contentDescription = "Удерживайте для голосового ввода",
                modifier = Modifier.size(26.dp)
            )
        }
    }
}