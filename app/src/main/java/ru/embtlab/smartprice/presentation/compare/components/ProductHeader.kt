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
import ru.embtlab.smartprice.presentation.compare.components.dialogs.EditNameDialog
import ru.embtlab.smartprice.presentation.theme.icons.AppIcons
import ru.embtlab.smartprice.presentation.theme.icons.Camera
import ru.embtlab.smartprice.presentation.theme.icons.Delete
import ru.embtlab.smartprice.presentation.theme.icons.Edit

@Composable
fun ProductHeader(
    id: String,
    name: String,
    canDelete: Boolean,
    listener: ProductCardListener,
    modifier: Modifier = Modifier,
    isFilled: Boolean = false,
) {
    var showDialog by remember { mutableStateOf(false) }

    if (showDialog) {
        EditNameDialog(
            currentName = name,
            onConfirm = { listener.onNameChange(id, it) },
            onDismiss = { showDialog = false }
        )
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Название с кликом для редактирования
        Row(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(8.dp))
                .clickable { showDialog = true }
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = name.ifBlank { "Товар" },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Icon(
                imageVector = AppIcons.Default.Edit,
                contentDescription = "Изменить имя",
                tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.7f),
                modifier = Modifier.size(16.dp)
            )
        }

        // Кнопки действий: Камера и Умная корзина
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { listener.onScanClick(id) }) {
                Icon(
                    imageVector = AppIcons.Default.Camera,
                    contentDescription = "Сканировать",
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            // Показываем корзину, если можно удалить (товаров > 2) ИЛИ если есть что очистить
            if (canDelete || isFilled) {
                IconButton(onClick = { listener.onDelete(id) }) {
                    Icon(
                        imageVector = AppIcons.Default.Delete,
                        contentDescription = if (isFilled) "Очистить данные" else "Удалить товар",
                        tint = if (isFilled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}