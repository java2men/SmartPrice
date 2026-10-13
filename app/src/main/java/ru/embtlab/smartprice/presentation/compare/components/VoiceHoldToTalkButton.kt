package ru.embtlab.smartprice.presentation.compare.components

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import ru.embtlab.smartprice.presentation.theme.icons.AppIcons
import ru.embtlab.smartprice.presentation.theme.icons.Mic

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

    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spokenText = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()

            if (!spokenText.isNullOrBlank()) {
                onStopRecording(spokenText)
            } else {
                onCancelRecording()
            }
        } else {
            onCancelRecording()
        }
    }

    FloatingActionButton(
        onClick = {
            if (!hasAudioPermission) {
                onRequireAudioPermission()
                return@FloatingActionButton
            }

            onStartRecording()

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                )
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ru-RU")
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "ru-RU")
                putExtra(
                    RecognizerIntent.EXTRA_PROMPT,
                    "Заполняем: $targetCardTitle\n(Например: молоко 1 литр 50 рублей)"
                )
            }

            try {
                speechLauncher.launch(intent)
            } catch (e: Exception) {
                Toast.makeText(context, "Голосовой ввод недоступен на устройстве", Toast.LENGTH_SHORT).show()
                onCancelRecording()
            }
        },
        shape = CircleShape,
        containerColor = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp),
        modifier = modifier
    ) {
        Icon(
            imageVector = AppIcons.Default.Mic,
            contentDescription = "Голосовой ввод",
            modifier = Modifier.size(26.dp)
        )
    }
}