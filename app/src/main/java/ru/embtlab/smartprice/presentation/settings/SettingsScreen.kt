package ru.embtlab.smartprice.presentation.settings

import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import ru.embtlab.smartprice.BuildConfig
import ru.embtlab.smartprice.domain.model.settings.AppThemeMode
import ru.embtlab.smartprice.presentation.settings.dialogs.SettingsPresetDialog
import ru.embtlab.smartprice.presentation.settings.components.SettingsItemRow
import ru.embtlab.smartprice.presentation.settings.components.SettingsSection
import ru.embtlab.smartprice.presentation.theme.icons.AppIcons
import ru.embtlab.smartprice.presentation.theme.icons.ArrowBack

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val settingsState by viewModel.settings.collectAsState()

    var showPresetDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }

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
        val currentSettings = settingsState

        if (currentSettings == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            )
            return@Scaffold
        }

        if (showPresetDialog) {
            SettingsPresetDialog(
                currentStyle = currentSettings.cardPreset,
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
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = currentSettings.themeMode == mode,
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
                    TextButton(onClick = { showThemeDialog = false }) { Text("Закрыть") }
                }
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            // Блок 1: Способы ввода (Голос и Сканер)
            item {
                SettingsSection(title = "Быстрый ввод") {
                    SettingsItemRow(
                        title = "Голосовой ввод",
                        subtitle = "Кнопка диктовки параметров товара",
                        trailing = {
                            Switch(
                                checked = currentSettings.isVoiceInputEnabled,
                                onCheckedChange = { viewModel.onVoiceInputEnabledChanged(it) }
                            )
                        }
                    )
                    HorizontalDivider()
                    SettingsItemRow(
                        title = "Сканер ценников",
                        subtitle = "Распознавание цены и веса через камеру",
                        trailing = {
                            Switch(
                                checked = currentSettings.isOcrScannerEnabled,
                                onCheckedChange = { viewModel.onOcrScannerEnabledChanged(it) }
                            )
                        }
                    )
                }
            }

            // Блок 2: Внешний вид карточек
            item {
                SettingsSection(title = "Интерфейс сравнения") {
                    SettingsItemRow(
                        title = "Стиль карточек товаров",
                        subtitle = "${currentSettings.cardPreset.title} — ${currentSettings.cardPreset.description}",
                        onClick = { showPresetDialog = true }
                    )
                }
            }

            // Блок 3: Оформление приложения
            item {
                SettingsSection(title = "Внешний вид") {
                    SettingsItemRow(
                        title = "Тема оформления",
                        subtitle = currentSettings.themeMode.title,
                        onClick = { showThemeDialog = true }
                    )
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        HorizontalDivider()
                        SettingsItemRow(
                            title = "Динамические цвета (Material You)",
                            subtitle = "Использовать палитру вашей системы",
                            trailing = {
                                Switch(
                                    checked = currentSettings.dynamicColor,
                                    onCheckedChange = { viewModel.onDynamicColorChanged(it) }
                                )
                            }
                        )
                    }
                }
            }

            // Блок 4: О приложении
            item {
                SettingsSection(title = "О приложении") {
                    SettingsItemRow(
                        title = "Версия",
                        subtitle = "Версия ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})"
                    )
                    HorizontalDivider()
                    SettingsItemRow(
                        title = "Разработчик",
                        subtitle = "EmbtLab, ${BuildConfig.BUILD_YEAR}"
                    )
                }
            }
        }
    }
}