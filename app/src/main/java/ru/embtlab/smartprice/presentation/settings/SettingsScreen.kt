package ru.embtlab.smartprice.presentation.settings

import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import ru.embtlab.smartprice.domain.model.AppThemeMode
import ru.embtlab.smartprice.presentation.compare.components.dialogs.SettingsPresetDialog
import ru.embtlab.smartprice.presentation.settings.components.SettingsItemRow
import ru.embtlab.smartprice.presentation.settings.components.SettingsSection
import ru.embtlab.smartprice.presentation.theme.icons.AppIcons
import ru.embtlab.smartprice.presentation.theme.icons.ArrowBack

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel() // <-- Инжектируется через Hilt
) {
    val settings by viewModel.settings.collectAsState()

    var showPresetDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }

    if (showPresetDialog) {
        SettingsPresetDialog(
            currentStyle = settings.cardPreset,
            onStyleSelected = {
                viewModel.onPresetChanged(it)
                showPresetDialog = false
            },
            onDismiss = { showPresetDialog = false }
        )
    }

    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text("Тема оформления") },
            text = {
                Column {
                    AppThemeMode.entries.forEach { mode ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = settings.themeMode == mode,
                                onClick = {
                                    viewModel.onThemeModeChanged(mode)
                                    showThemeDialog = false
                                }
                            )
                            Text(
                                text = mode.title,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showThemeDialog = false }) { Text("Отмена") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Настройки") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = AppIcons.Default.ArrowBack, contentDescription = "Назад")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            // Секция 1: Вид карточек
            item {
                SettingsSection(title = "Карточка товара") {
                    SettingsItemRow(
                        title = "Вид карточки",
                        subtitle = "${settings.cardPreset.title} — ${settings.cardPreset.description}",
                        onClick = { showPresetDialog = true }
                    )
                }
            }

            // Секция 2: Внешний вид
            item {
                SettingsSection(title = "Внешний вид") {
                    SettingsItemRow(
                        title = "Тема",
                        subtitle = settings.themeMode.title,
                        onClick = { showThemeDialog = true }
                    )

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        HorizontalDivider()
                        SettingsItemRow(
                            title = "Динамические цвета (Material You)",
                            subtitle = "Подстраивать цвета под обои устройства",
                            trailing = {
                                Switch(
                                    checked = settings.dynamicColor,
                                    onCheckedChange = { viewModel.onDynamicColorChanged(it) }
                                )
                            }
                        )
                    }
                }
            }

            // Секция 3: О приложении
            item {
                SettingsSection(title = "О приложении") {
                    SettingsItemRow(
                        title = "Умная Цена",
                        subtitle = "Версия 1.0.0"
                    )
                    HorizontalDivider()
                    SettingsItemRow(
                        title = "Назначение",
                        subtitle = "Быстрый расчет честной стоимости за 1 кг, 1 л или 1 штуку у полки магазина с защитой от скрытой шринкфляции."
                    )
                }
            }
        }
    }
}