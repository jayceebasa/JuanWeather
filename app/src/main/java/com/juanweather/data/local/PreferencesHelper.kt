package com.juanweather.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.juanweather.data.models.AppSettings
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.firstOrNull

val Context.dataStore by preferencesDataStore(name = "app_settings")

class PreferencesHelper(private val context: Context) {

    private object PreferenceKeys {
        val TEMP_UNIT = stringPreferencesKey("temp_unit")
        val WIND_UNIT = stringPreferencesKey("wind_unit")
        val THEME = stringPreferencesKey("theme")
        val NOTIFICATIONS = stringPreferencesKey("notifications_enabled")
    }

    suspend fun getSettings(): AppSettings {
        return context.dataStore.data.map { preferences ->
            AppSettings(
                temperatureUnit = preferences[PreferenceKeys.TEMP_UNIT] ?: "C",
                windSpeedUnit = preferences[PreferenceKeys.WIND_UNIT] ?: "km/h",
                theme = preferences[PreferenceKeys.THEME] ?: "light",
                notificationsEnabled = preferences[PreferenceKeys.NOTIFICATIONS] != "false"
            )
        }.firstOrNull() ?: AppSettings()
    }

    suspend fun saveTemperatureUnit(unit: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferenceKeys.TEMP_UNIT] = unit
        }
    }

    suspend fun saveTheme(theme: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferenceKeys.THEME] = theme
        }
    }

    suspend fun saveNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferenceKeys.NOTIFICATIONS] = enabled.toString()
        }
    }
}
