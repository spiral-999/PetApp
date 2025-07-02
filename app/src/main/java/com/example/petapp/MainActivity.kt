package com.example.petapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.example.petapp.data.SettingsDataStore
import com.example.petapp.data.ThemePreferences
import com.example.petapp.notifications.NotificationHelper
import com.example.petapp.ui.navigation.AppNavHost
import com.example.petapp.ui.theme.PetAppTheme
import com.example.petapp.ui.theme.ThemeVariant

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // A única responsabilidade aqui é criar os canais de notificação.
        NotificationHelper(applicationContext).createNotificationChannels()

        setContent {
            val settingsDataStore = SettingsDataStore(applicationContext)
            val isDarkThemeEnabled by settingsDataStore.darkModeEnabled.collectAsState(initial = false)

            val themePreferences = ThemePreferences(applicationContext)
            val selectedThemeVariant by themePreferences.themeVariant.collectAsState(initial = ThemeVariant.MONOCHROME)

            PetAppTheme(
                darkTheme = isDarkThemeEnabled,
                themeVariant = selectedThemeVariant
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavHost(navController = rememberNavController())
                }
            }
        }
    }
}