package ru.embtlab.smartprice.presentation.compare.components.dialogs

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import ru.embtlab.smartprice.presentation.compare.util.VoiceInputManager
import ru.embtlab.smartprice.presentation.compare.util.VoiceRecognitionState
import ru.embtlab.smartprice.presentation.theme.icons.AppIcons
import ru.embtlab.smartprice.presentation.theme.icons.Mic

@Composable
fun VoiceInputDialog(
    onResult: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val voiceManager = remember { VoiceInputManager(context) }
    val state by voiceManager.state.collectAsState()

    DisposableEffect(Unit) {
        voiceManager.startListening()
        onDispose { voiceManager.stopListening() }
    }

    LaunchedEffect(state) {
        if (state is VoiceRecognitionState.Success) {
            onResult((state as VoiceRecognitionState.Success).text)
            onDismiss()
        }
    }

    val rms = (state as? VoiceRecognitionState.Listening)?.rmsDb ?: 0f
    val scale by animateFloatAsState(targetValue = 1f + (rms / 12f).coerceIn(0f, 0.45f), label = "MicScale")

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
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Голосовой ввод",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Box(
                    modifier = Modifier
                        .size(80.dp)
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
                        modifier = Modifier.size(36.dp)
                    )
                }

                Text(
                    text = when (state) {
                        VoiceRecognitionState.Idle, VoiceRecognitionState.Ready -> "Слушаю... Произнесите цену и вес"
                        is VoiceRecognitionState.Listening -> "Говорите (например, «189 рублей 900 грамм»)"
                        is VoiceRecognitionState.Error -> (state as VoiceRecognitionState.Error).message
                        is VoiceRecognitionState.Success -> (state as VoiceRecognitionState.Success).text
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = if (state is VoiceRecognitionState.Error) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Отмена")
                    }
                }
            }
        }
    }
}