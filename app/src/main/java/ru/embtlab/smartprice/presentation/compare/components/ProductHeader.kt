package ru.embtlab.smartprice.presentation.compare.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ru.embtlab.smartprice.presentation.compare.components.dialogs.EditNameBottomSheet
import ru.embtlab.smartprice.presentation.theme.icons.AppIcons
import ru.embtlab.smartprice.presentation.theme.icons.Camera
import ru.embtlab.smartprice.presentation.theme.icons.Delete

@Composable
fun ProductHeader(
    id: String,
    name: String,
    canDelete: Boolean,
    listener: ProductCardListener,
    modifier: Modifier = Modifier,
    isFilled: Boolean = false,
    recentNames: List<String> = emptyList(),
    onDeleteRecentName: (String) -> Unit = {} // <-- Добавлен колбэк
) {
    var showSheet by remember { mutableStateOf(false) }

    if (showSheet) {
        EditNameBottomSheet(
            currentName = name,
            recentNames = recentNames,
            onConfirm = { newName ->
                listener.onNameChange(id, newName)
                showSheet = false
            },
            onDeleteRecent = onDeleteRecentName, // <-- Передаем в шторку
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
                    text = name.ifBlank { "Название товара" },
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
            IconButton(onClick = { listener.onScanClick(id) }) {
                Icon(
                    imageVector = AppIcons.Default.Camera,
                    contentDescription = "Сканировать",
                    tint = MaterialTheme.colorScheme.primary
                )
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