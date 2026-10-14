package ru.embtlab.smartprice.presentation.compare.components

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ru.embtlab.smartprice.presentation.compare.components.dialogs.VoiceRecognitionBottomSheet
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
    var showDialog by remember { mutableStateOf(false) }

    if (showDialog) {
        VoiceRecognitionBottomSheet(
            targetTitle = targetCardTitle,
            onResult = { text ->
                onStopRecording(text)
            },
            onDismiss = {
                showDialog = false
                onCancelRecording()
            }
        )
    }

    FloatingActionButton(
        onClick = {
            if (!hasAudioPermission) {
                onRequireAudioPermission()
                return@FloatingActionButton
            }
            onStartRecording()
            showDialog = true
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