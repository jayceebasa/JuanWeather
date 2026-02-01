package com.juanweather.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.juanweather.data.models.AppSettings
import com.juanweather.data.local.PreferencesHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SettingsViewModel(private val preferencesHelper: PreferencesHelper) : ViewModel() {

    private val _settings = MutableStateFlow<AppSettings?>(null)
    val settings: StateFlow<AppSettings?> = _settings.asStateFlow()

    init {
        viewModelScope.launch {
            _settings.value = preferencesHelper.getSettings()
        }
    }

    fun updateTemperatureUnit(unit: String) {
        viewModelScope.launch {
            preferencesHelper.saveTemperatureUnit(unit)
            _settings.value = _settings.value?.copy(temperatureUnit = unit)
        }
    }

    fun updateTheme(theme: String) {
        viewModelScope.launch {
            preferencesHelper.saveTheme(theme)
            _settings.value = _settings.value?.copy(theme = theme)
        }
    }

    fun updateNotifications(enabled: Boolean) {
        viewModelScope.launch {
            preferencesHelper.saveNotificationsEnabled(enabled)
            _settings.value = _settings.value?.copy(notificationsEnabled = enabled)
        }
    }
}
