package com.juanweather.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.juanweather.data.models.AppSettings
import com.juanweather.data.local.PreferencesHelper
import com.juanweather.data.repository.FirestoreSettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val preferencesHelper: PreferencesHelper,
    private val firestoreRepository: FirestoreSettingsRepository = FirestoreSettingsRepository()
) : ViewModel() {

    private val _settings = MutableStateFlow<AppSettings?>(null)
    val settings: StateFlow<AppSettings?> = _settings.asStateFlow()

    init {
        viewModelScope.launch {
            // Load settings from Firestore (cloud-synced)
            firestoreRepository.getSettings().collect { firestoreSettings ->
                _settings.value = firestoreSettings
            }
        }
    }

    fun updateTemperatureUnit(unit: String) {
        viewModelScope.launch {
            // Save to both Firestore (cloud) and local storage (offline fallback)
            firestoreRepository.saveTemperatureUnit(unit)
            preferencesHelper.saveTemperatureUnit(unit)
            _settings.value = _settings.value?.copy(temperatureUnit = unit)
        }
    }

    fun updateWindSpeedUnit(unit: String) {
        viewModelScope.launch {
            firestoreRepository.saveWindSpeedUnit(unit)
            _settings.value = _settings.value?.copy(windSpeedUnit = unit)
        }
    }

    fun updatePressureUnit(unit: String) {
        viewModelScope.launch {
            firestoreRepository.savePressureUnit(unit)
            _settings.value = _settings.value?.copy(pressureUnit = unit)
        }
    }

    fun updateVisibilityUnit(unit: String) {
        viewModelScope.launch {
            firestoreRepository.saveVisibilityUnit(unit)
            _settings.value = _settings.value?.copy(visibilityUnit = unit)
        }
    }

    fun updateTheme(theme: String) {
        viewModelScope.launch {
            // Save to both Firestore (cloud) and local storage (offline fallback)
            firestoreRepository.saveTheme(theme)
            preferencesHelper.saveTheme(theme)
            _settings.value = _settings.value?.copy(theme = theme)
        }
    }

    fun updateNotifications(enabled: Boolean) {
        viewModelScope.launch {
            // Save to both Firestore (cloud) and local storage (offline fallback)
            firestoreRepository.saveNotificationsEnabled(enabled)
            preferencesHelper.saveNotificationsEnabled(enabled)
            _settings.value = _settings.value?.copy(notificationsEnabled = enabled)
        }
    }

    fun updateLanguage(language: String) {
        viewModelScope.launch {
            firestoreRepository.saveLanguage(language)
            _settings.value = _settings.value?.copy(language = language)
        }
    }

    fun updateAllSettings(newSettings: AppSettings) {
        viewModelScope.launch {
            firestoreRepository.saveAllSettings(newSettings)
            _settings.value = newSettings
        }
    }
}
