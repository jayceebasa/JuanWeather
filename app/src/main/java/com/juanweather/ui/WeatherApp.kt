package com.juanweather.ui

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.ViewModelProvider
import com.juanweather.JuanWeatherApp
import com.juanweather.ui.screens.AddLocationScreen
import com.juanweather.ui.screens.AboutSupportScreen
import com.juanweather.ui.screens.EmergencyContactScreen
import com.juanweather.ui.screens.LoginScreen
import com.juanweather.ui.screens.RegisterScreen
import com.juanweather.ui.screens.SettingsScreen
import com.juanweather.ui.screens.SOSSettingsScreen
import com.juanweather.ui.screens.UserManagementScreen
import com.juanweather.ui.screens.WeatherDashboardScreen
import com.juanweather.ui.screens.WeatherPreferencesScreen
import com.juanweather.viewmodel.AuthViewModel
import com.juanweather.viewmodel.LocationViewModel
import com.juanweather.viewmodel.WeatherViewModel
import com.juanweather.viewmodel.EmergencyContactViewModel

/**
 * Enumeration for different app screens
 */
enum class AppScreen {
    Login,
    Register,
    Dashboard,
    AddLocation,
    Settings,
    WeatherPreferences,
    EmergencyContact,
    SOSSettings,
    AboutSupport,
    UserManagement
}

/**
 * Navigation controller that manages a backstack of screens
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

    fun canGoBack(): Boolean {
        return backStack.size > 1
    }
}

/**
 * Main app navigation composable
 * Handles navigation between Dashboard, AddLocation, and Settings screens
 * using a two-way backstack system
 */
@Composable
fun WeatherApp() {
    val context = LocalContext.current
    val app = context.applicationContext as JuanWeatherApp

    // ...existing ViewModels initialization...
    val authViewModel: AuthViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                return AuthViewModel(app.userRepository, context) as T
            }
        }
    )

    val weatherViewModel: WeatherViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                return WeatherViewModel(app.weatherRepository) as T
            }
        }
    )

    val locationViewModel: LocationViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                return LocationViewModel(
                    app.userLocationDao,
                    app.weatherRepository,
                    com.juanweather.data.repository.FirestoreUserLocationRepository()
                ) as T
            }
        }
    )

    val settingsViewModel: com.juanweather.viewmodel.SettingsViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                return com.juanweather.viewmodel.SettingsViewModel(
                    app.preferencesHelper,
                    app.settingsRepository
                ) as T
            }
        }
    )

    val emergencyContactViewModel: EmergencyContactViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                return EmergencyContactViewModel(
                    app.hybridEmergencyContactRepository
                ) as T
            }
        }
    )

    val sosViewModel: com.juanweather.viewmodel.SOSViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                return com.juanweather.viewmodel.SOSViewModel(
                    app.hybridSOSRepository,
                    com.juanweather.utils.FmcSmsService(),
                    com.juanweather.utils.FmcSmsConfig(),
                    com.juanweather.utils.LocationManager(context)
                ) as T
            }
        }
    )

    val navigationController = remember { NavigationController() }
    val currentScreen = remember { mutableStateOf(AppScreen.Login) }
    val backPressedOnDashboard = remember { mutableStateOf(false) }

    // RBAC — observe admin status reactively
    val isAdmin by authViewModel.isAdmin.collectAsState()
    val loggedInUser by authViewModel.loggedInUser.collectAsState()

    // Reset backPressedOnDashboard after 2 seconds
    LaunchedEffect(backPressedOnDashboard.value, currentScreen.value) {
        if (backPressedOnDashboard.value && currentScreen.value == AppScreen.Dashboard) {
            kotlinx.coroutines.delay(2000)
            backPressedOnDashboard.value = false
        }
    }

    // Handle system back button press
    BackHandler {
        when (currentScreen.value) {
            AppScreen.Dashboard -> {
                // On dashboard, show toast and require second back press to exit
                if (backPressedOnDashboard.value) {
                    // Second back press - exit app
                    (context as? android.app.Activity)?.finish()
                } else {
                    // First back press - show message
                    backPressedOnDashboard.value = true
                    Toast.makeText(context, "Press back button again to close the application", Toast.LENGTH_SHORT).show()
                }
            }
            else -> {
                // On other screens, navigate back normally
                if (navigationController.canGoBack()) {
                    navigationController.navigateBack()
                    currentScreen.value = navigationController.getCurrentScreen()
                    backPressedOnDashboard.value = false
                }
            }
        }
    }

    // Reset backPressedOnDashboard when leaving dashboard
    LaunchedEffect(currentScreen.value) {
        if (currentScreen.value != AppScreen.Dashboard) {
            backPressedOnDashboard.value = false
        }
    }

    when (currentScreen.value) {
        AppScreen.Login -> {
            LoginScreen(
                authViewModel = authViewModel,
                onLoginSuccess = {
                    // Load this user's saved locations and settings
                    val userId = authViewModel.loggedInUser.value?.id ?: 0
                    val firebaseUid = authViewModel.firebaseUid.value

                    // Sync settings from Firebase to Room on login
                    settingsViewModel.syncSettingsOnLogin(userId, firebaseUid)

                    // Sync SOS settings from Firebase to Room on login
                    sosViewModel.syncSOSSettingsOnLogin(firebaseUid)

                    // Sync emergency contacts from Firestore to Room on login (async background task)
                    if (!firebaseUid.isNullOrBlank()) {
                        Thread {
                            try {
                                kotlinx.coroutines.runBlocking {
                                    app.hybridEmergencyContactRepository.syncFirestoreContactsToRoomOnLogin(firebaseUid)
                                    android.util.Log.d("WeatherApp", "Emergency contacts synced on login")
                                }
                            } catch (e: Exception) {
                                android.util.Log.e("WeatherApp", "Error syncing emergency contacts: ${e.message}", e)
                            }
                        }.start()
                    }

                    locationViewModel.loadLocationsForUser(userId, firebaseUid) { firstLocationCity ->
                        // Auto-load the first location on the homepage
                        weatherViewModel.fetchWeatherByCity(firstLocationCity)
                    }
                    navigationController.navigate(AppScreen.Dashboard)
                    currentScreen.value = AppScreen.Dashboard
                },
                onNavigateToRegister = {
                    navigationController.navigate(AppScreen.Register)
                    currentScreen.value = AppScreen.Register
                }
            )
        }

        AppScreen.Register -> {
            RegisterScreen(
                authViewModel = authViewModel,
                onRegisterSuccess = {
                    // Initialize default settings for new user
                    val userId = authViewModel.loggedInUser.value?.id ?: 0
                    if (userId > 0) {
                        settingsViewModel.loadSettingsForUser(userId)
                    }
                    navigationController.navigateBack()
                    currentScreen.value = navigationController.getCurrentScreen()
                },
                onNavigateToLogin = {
                    navigationController.navigateBack()
                    currentScreen.value = navigationController.getCurrentScreen()
                }
            )
        }

        AppScreen.Dashboard -> {

            WeatherDashboardScreen(
                weatherViewModel = weatherViewModel,
                settingsViewModel = settingsViewModel,
                onNavigateToAddLocation = {
                    navigationController.navigate(AppScreen.AddLocation)
                    currentScreen.value = AppScreen.AddLocation
                },
                onNavigateToSettings = {
                    navigationController.navigate(AppScreen.Settings)
                    currentScreen.value = AppScreen.Settings
                },
                onNavigateToUserManagement = {
                    navigationController.navigate(AppScreen.UserManagement)
                    currentScreen.value = AppScreen.UserManagement
                },
                isAdmin = isAdmin
            )
        }

        AppScreen.AddLocation -> {
            val settings by settingsViewModel.settings.collectAsState()
            AddLocationScreen(
                onBack = {
                    navigationController.navigateBack()
                    currentScreen.value = navigationController.getCurrentScreen()
                },
                locationViewModel = locationViewModel,
                userId = authViewModel.loggedInUser.value?.id ?: 0,
                temperatureUnit = settings?.temperatureUnit ?: "C",
                onLocationSelected = { selectedLocation ->
                    // Use the original cityName for the API call, display the API-normalized city
                    val cityToFetch = selectedLocation.cityName.ifBlank { selectedLocation.city }
                    weatherViewModel.fetchWeatherByCity(cityToFetch)
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
                    // Clear all user data from ViewModels before logout
                    weatherViewModel.clearData()
                    locationViewModel.clearData()
                    authViewModel.logout()
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
            WeatherPreferencesScreen(
                settingsViewModel = settingsViewModel,
                loggedInUser = loggedInUser,
                onBack = {
                    navigationController.navigateBack()
                    currentScreen.value = navigationController.getCurrentScreen()
                }
            )
        }

        AppScreen.EmergencyContact -> {
            EmergencyContactScreen(
                viewModel = emergencyContactViewModel,
                onBack = {
                    navigationController.navigateBack()
                    currentScreen.value = navigationController.getCurrentScreen()
                }
            )
        }

        AppScreen.SOSSettings -> {
            val emergencyContacts = emergencyContactViewModel.contacts.collectAsState(emptyList()).value

            // Set the user name in SOSViewModel when screen is shown
            LaunchedEffect(loggedInUser) {
                loggedInUser?.let { user ->
                    sosViewModel.setUserName(user.name)
                }
            }

            SOSSettingsScreen(
                viewModel = sosViewModel,
                emergencyContacts = emergencyContacts,
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

        AppScreen.UserManagement -> {
            UserManagementScreen(
                authViewModel = authViewModel,
                onBack = {
                    navigationController.navigateBack()
                    currentScreen.value = navigationController.getCurrentScreen()
                }
            )
        }
    }
}
