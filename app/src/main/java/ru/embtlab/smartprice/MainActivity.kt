package ru.embtlab.smartprice

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import ru.embtlab.smartprice.data.local.SettingsDataStore
import ru.embtlab.smartprice.domain.model.AppSettings
import ru.embtlab.smartprice.domain.model.AppThemeMode
import ru.embtlab.smartprice.presentation.compare.CompareScreen
import ru.embtlab.smartprice.presentation.settings.SettingsScreen
import ru.embtlab.smartprice.presentation.theme.SmartPriceTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val settingsDataStore = SettingsDataStore(this)


        setContent {

            val settings by settingsDataStore.settingsFlow.collectAsState(initial = AppSettings())

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
