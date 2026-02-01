package com.juanweather.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.juanweather.ui.screens.AddLocationScreen
import com.juanweather.ui.screens.AboutSupportScreen
import com.juanweather.ui.screens.EmergencyContactScreen
import com.juanweather.ui.screens.LoginScreen
import com.juanweather.ui.screens.PlaceholderScreen
import com.juanweather.ui.screens.SettingsScreen
import com.juanweather.ui.screens.SOSSettingsScreen
import com.juanweather.ui.screens.WeatherDashboardScreen
import com.juanweather.ui.screens.WeatherPreferencesScreen

/**
 * Enumeration for different app screens
 */
enum class AppScreen {
    Login,
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
    private var backStack = mutableListOf(AppScreen.Login)

    fun navigate(screen: AppScreen) {
        backStack.add(screen)
    }

    fun navigateBack() {
        if (backStack.size > 1) {
            backStack.removeAt(backStack.size - 1)
        }
    }

    fun logout() {
        backStack.clear()
        backStack.add(AppScreen.Login)
    }

    fun getCurrentScreen(): AppScreen {
        return backStack.lastOrNull() ?: AppScreen.Login
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
    val currentScreen = remember { mutableStateOf(AppScreen.Login) }

    when (currentScreen.value) {
        AppScreen.Login -> {
            LoginScreen(
                onLoginSuccess = {
                    navigationController.navigate(AppScreen.Dashboard)
                    currentScreen.value = AppScreen.Dashboard
                }
            )
        }

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
                onLogout = {
                    navigationController.logout()
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
            // Weather Preferences screen with functional UI
            WeatherPreferencesScreen(
                onBack = {
                    navigationController.navigateBack()
                    currentScreen.value = navigationController.getCurrentScreen()
                }
            )
        }

        AppScreen.EmergencyContact -> {
            EmergencyContactScreen(
                onBack = {
                    navigationController.navigateBack()
                    currentScreen.value = navigationController.getCurrentScreen()
                }
            )
        }

        AppScreen.SOSSettings -> {
            SOSSettingsScreen(
                onBack = {
                    navigationController.navigateBack()
                    currentScreen.value = navigationController.getCurrentScreen()
                }
            )
        }

        AppScreen.AboutSupport -> {
            AboutSupportScreen(
                onBack = {
                    navigationController.navigateBack()
                    currentScreen.value = navigationController.getCurrentScreen()
                }
            )
        }
    }
}
