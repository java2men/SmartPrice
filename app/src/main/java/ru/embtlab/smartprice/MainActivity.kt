package ru.embtlab.smartprice

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import dagger.hilt.android.AndroidEntryPoint
import ru.embtlab.smartprice.domain.model.AppThemeMode
import ru.embtlab.smartprice.presentation.compare.CompareScreen
import ru.embtlab.smartprice.presentation.main.MainViewModel
import ru.embtlab.smartprice.presentation.settings.SettingsScreen
import ru.embtlab.smartprice.presentation.theme.SmartPriceTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val mainViewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val settings by mainViewModel.settings.collectAsState()

            val isDarkTheme = when (settings.themeMode) {
                AppThemeMode.SYSTEM -> isSystemInDarkTheme()
                AppThemeMode.LIGHT -> false
                AppThemeMode.DARK -> true
            }

            SmartPriceTheme(
                darkTheme = isDarkTheme,
                dynamicColor = settings.dynamicColor
            ) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    var isSettingsOpen by remember { mutableStateOf(false) }

                    if (isSettingsOpen) {
                        SettingsScreen(onNavigateBack = { isSettingsOpen = false })
                    } else {
                        CompareScreen(onOpenSettings = { isSettingsOpen = true })
                    }
                }
            }
        }
    }
}