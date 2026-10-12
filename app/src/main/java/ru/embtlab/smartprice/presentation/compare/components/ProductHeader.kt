package ru.embtlab.smartprice.presentation.compare.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ru.embtlab.smartprice.presentation.compare.components.dialogs.EditNameBottomSheet
import ru.embtlab.smartprice.presentation.theme.icons.*

@Composable
fun ProductHeader(
    id: String,
    name: String,
    canDelete: Boolean,
    listener: ProductCardListener,
    modifier: Modifier = Modifier,
    isFilled: Boolean = false,
    recentNames: List<String> = emptyList(),
    onDeleteRecentName: (String) -> Unit = {}
) {
    var showSheet by remember { mutableStateOf(false) }
    var isCaptureMenuOpen by remember { mutableStateOf(false) }

    if (showSheet) {
        EditNameBottomSheet(
            currentName = name,
            recentNames = recentNames,
            onConfirm = { newName ->
                listener.onNameChange(id, newName)
                showSheet = false
            },
            onDeleteRecent = onDeleteRecentName,
            onDismiss = { showSheet = false }
        )
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier
                .weight(1f)
                .height(38.dp)
                .clip(RoundedCornerShape(8.dp))
                .clickable { showSheet = true }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = name.ifBlank { "Товар" },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = if (name.isBlank()) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            // Единая кнопка захвата (Камера + Микрофон)
            Box {
                IconButton(onClick = { isCaptureMenuOpen = true }) {
                    Icon(
                        imageVector = AppIcons.Default.Camera,
                        contentDescription = "Быстрый ввод (Камера / Голос)",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                DropdownMenu(
                    expanded = isCaptureMenuOpen,
                    onDismissRequest = { isCaptureMenuOpen = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("📷 Скан ценника") },
                        onClick = {
                            isCaptureMenuOpen = false
                            listener.onScanClick(id)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("🎙️ Сказать голосом") },
                        onClick = {
                            isCaptureMenuOpen = false
                            listener.onVoiceClick(id)
                        }
                    )
                }
            }

            if (canDelete || isFilled) {
                IconButton(onClick = { listener.onDelete(id) }) {
                    Icon(
                        imageVector = AppIcons.Default.Delete,
                        contentDescription = if (isFilled) "Очистить" else "Удалить",
                        tint = if (isFilled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}