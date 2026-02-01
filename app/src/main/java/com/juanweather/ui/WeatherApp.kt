package com.juanweather.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.juanweather.ui.screens.AddLocationScreen
import com.juanweather.ui.screens.SettingsScreen
import com.juanweather.ui.screens.WeatherDashboardScreen

/**
 * Enumeration for different app screens
 */
enum class AppScreen {
    Dashboard,
    AddLocation,
    Settings
}

/**
 * Main app navigation composable
 * Handles navigation between Dashboard, AddLocation, and Settings screens
 */
@Composable
fun WeatherApp() {
    val currentScreen = remember { mutableStateOf(AppScreen.Dashboard) }

    when (currentScreen.value) {
        AppScreen.Dashboard -> {
            WeatherDashboardScreen(
                onNavigateToAddLocation = { currentScreen.value = AppScreen.AddLocation },
                onNavigateToSettings = { currentScreen.value = AppScreen.Settings }
            )
        }

        AppScreen.AddLocation -> {
            AddLocationScreen(
                onBack = { currentScreen.value = AppScreen.Dashboard }
            )
        }

        AppScreen.Settings -> {
            SettingsScreen(
                onBack = { currentScreen.value = AppScreen.Dashboard }
            )
        }
    }
}
