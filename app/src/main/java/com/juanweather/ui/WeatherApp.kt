package com.juanweather.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.juanweather.ui.screens.AddLocationScreen
import com.juanweather.ui.screens.PlaceholderScreen
import com.juanweather.ui.screens.SettingsScreen
import com.juanweather.ui.screens.WeatherDashboardScreen

/**
 * Enumeration for different app screens
 */
enum class AppScreen {
    Dashboard,
    AddLocation,
    Settings,
    WeatherPreferences,
    EmergencyContact,
    SOSSettings,
    AboutSupport
}

/**
 * Navigation controller that manages a backstack of screens
 * Supports forward navigation (push) and backward navigation (pop)
 */
class NavigationController {
    private var backStack = mutableListOf(AppScreen.Dashboard)

    fun navigate(screen: AppScreen) {
        backStack.add(screen)
    }

    fun navigateBack() {
        if (backStack.size > 1) {
            backStack.removeAt(backStack.size - 1)
        }
    }

    fun getCurrentScreen(): AppScreen {
        return backStack.lastOrNull() ?: AppScreen.Dashboard
    }

    fun getBackStackState(): List<AppScreen> {
        return backStack.toList()
    }
}

/**
 * Main app navigation composable
 * Handles navigation between Dashboard, AddLocation, and Settings screens
 * using a two-way backstack system
 */
@Composable
fun WeatherApp() {
    val navigationController = remember { NavigationController() }
    val currentScreen = remember { mutableStateOf(AppScreen.Dashboard) }

    when (currentScreen.value) {
        AppScreen.Dashboard -> {
            WeatherDashboardScreen(
                onNavigateToAddLocation = {
                    navigationController.navigate(AppScreen.AddLocation)
                    currentScreen.value = AppScreen.AddLocation
                },
                onNavigateToSettings = {
                    navigationController.navigate(AppScreen.Settings)
                    currentScreen.value = AppScreen.Settings
                }
            )
        }

        AppScreen.AddLocation -> {
            AddLocationScreen(
                onBack = {
                    navigationController.navigateBack()
                    currentScreen.value = navigationController.getCurrentScreen()
                }
            )
        }

        AppScreen.Settings -> {
            SettingsScreen(
                onBack = {
                    navigationController.navigateBack()
                    currentScreen.value = navigationController.getCurrentScreen()
                },
                onNavigateToWeatherPreferences = {
                    navigationController.navigate(AppScreen.WeatherPreferences)
                    currentScreen.value = AppScreen.WeatherPreferences
                },
                onNavigateToEmergencyContact = {
                    navigationController.navigate(AppScreen.EmergencyContact)
                    currentScreen.value = AppScreen.EmergencyContact
                },
                onNavigateToSOSSettings = {
                    navigationController.navigate(AppScreen.SOSSettings)
                    currentScreen.value = AppScreen.SOSSettings
                },
                onNavigateToAboutSupport = {
                    navigationController.navigate(AppScreen.AboutSupport)
                    currentScreen.value = AppScreen.AboutSupport
                }
            )
        }

        AppScreen.WeatherPreferences -> {
            // Placeholder for Weather Preferences screen
            PlaceholderScreen(
                title = "Weather Preferences",
                onBack = {
                    navigationController.navigateBack()
                    currentScreen.value = navigationController.getCurrentScreen()
                }
            )
        }

        AppScreen.EmergencyContact -> {
            // Placeholder for Emergency Contact screen
            PlaceholderScreen(
                title = "Emergency Contact",
                onBack = {
                    navigationController.navigateBack()
                    currentScreen.value = navigationController.getCurrentScreen()
                }
            )
        }

        AppScreen.SOSSettings -> {
            // Placeholder for SOS Settings screen
            PlaceholderScreen(
                title = "SOS Settings",
                onBack = {
                    navigationController.navigateBack()
                    currentScreen.value = navigationController.getCurrentScreen()
                }
            )
        }

        AppScreen.AboutSupport -> {
            // Placeholder for About & Support screen
            PlaceholderScreen(
                title = "About & Support",
                onBack = {
                    navigationController.navigateBack()
                    currentScreen.value = navigationController.getCurrentScreen()
                }
            )
        }
    }
}
