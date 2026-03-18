package com.juanweather.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.juanweather.data.models.AppSettings
import com.juanweather.data.local.PreferencesHelper
import com.juanweather.data.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import android.util.Log

class SettingsViewModel(
    private val preferencesHelper: PreferencesHelper,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _settings = MutableStateFlow<AppSettings?>(null)
    val settings: StateFlow<AppSettings?> = _settings.asStateFlow()

    private var currentUserId: Int = 0

    /**
     * Load user settings with dual-source sync:
     * Remote-first from Firebase, fallback to Room, sync between both
     */
    fun loadSettingsForUser(userId: Int) {
        if (userId <= 0) return

        currentUserId = userId
        Log.d("SettingsViewModel", "Loading settings for user $userId")
        viewModelScope.launch {
            // Load settings from unified repository (remote-first with Room fallback)
            settingsRepository.getSettings(userId).collect { settings ->
                _settings.value = settings
                Log.d("SettingsViewModel", "Settings loaded: $settings")
            }
        }
    }

    /**
     * Sync settings from Firestore to Room on login
     * Called after user logs in to ensure cloud settings are available offline
     */
    fun syncSettingsOnLogin(userId: Int, firebaseUid: String? = null) {
        if (userId <= 0) return

        currentUserId = userId
        Log.d("SettingsViewModel", "Syncing settings for user $userId on login")
        viewModelScope.launch {
            settingsRepository.syncFirestoreSettingsToRoom(userId, firebaseUid)
            // Then load the synced settings
            loadSettingsForUser(userId)
        }
    }

    fun updateTemperatureUnit(unit: String) {
        if (currentUserId <= 0) {
            Log.e("SettingsViewModel", "Cannot update: currentUserId is $currentUserId")
            return
        }
        Log.d("SettingsViewModel", "Updating temperature unit to $unit for user $currentUserId")
        viewModelScope.launch {
            settingsRepository.saveTemperatureUnit(currentUserId, unit)
            _settings.value = _settings.value?.copy(temperatureUnit = unit)
        }
    }

    fun updateWindSpeedUnit(unit: String) {
        if (currentUserId <= 0) {
            Log.e("SettingsViewModel", "Cannot update: currentUserId is $currentUserId")
            return
        }
        viewModelScope.launch {
            settingsRepository.saveWindSpeedUnit(currentUserId, unit)
            _settings.value = _settings.value?.copy(windSpeedUnit = unit)
        }
    }

    fun updatePressureUnit(unit: String) {
        if (currentUserId <= 0) {
            Log.e("SettingsViewModel", "Cannot update: currentUserId is $currentUserId")
            return
        }
        viewModelScope.launch {
            settingsRepository.savePressureUnit(currentUserId, unit)
            _settings.value = _settings.value?.copy(pressureUnit = unit)
        }
    }

    fun updateVisibilityUnit(unit: String) {
        if (currentUserId <= 0) {
            Log.e("SettingsViewModel", "Cannot update: currentUserId is $currentUserId")
            return
        }
        viewModelScope.launch {
            settingsRepository.saveVisibilityUnit(currentUserId, unit)
            _settings.value = _settings.value?.copy(visibilityUnit = unit)
        }
    }

    fun updateTheme(theme: String) {
        if (currentUserId <= 0) {
            Log.e("SettingsViewModel", "Cannot update: currentUserId is $currentUserId")
            return
        }
        viewModelScope.launch {
            settingsRepository.saveTheme(currentUserId, theme)
            _settings.value = _settings.value?.copy(theme = theme)
        }
    }

    fun updateNotifications(enabled: Boolean) {
        if (currentUserId <= 0) {
            Log.e("SettingsViewModel", "Cannot update: currentUserId is $currentUserId")
            return
        }
        viewModelScope.launch {
            settingsRepository.saveNotificationsEnabled(currentUserId, enabled)
            _settings.value = _settings.value?.copy(notificationsEnabled = enabled)
        }
    }

    fun updateLanguage(language: String) {
        if (currentUserId <= 0) {
            Log.e("SettingsViewModel", "Cannot update: currentUserId is $currentUserId")
            return
        }
        viewModelScope.launch {
            settingsRepository.saveLanguage(currentUserId, language)
            _settings.value = _settings.value?.copy(language = language)
        }
    }

    fun updateAllSettings(newSettings: AppSettings) {
        if (currentUserId <= 0) {
            Log.e("SettingsViewModel", "Cannot update: currentUserId is $currentUserId")
            return
        }
        viewModelScope.launch {
            settingsRepository.saveAllSettings(currentUserId, newSettings)
            _settings.value = newSettings
        }
    }
}
