package com.juanweather.data.repository

import com.juanweather.data.local.SOSSettingsDao
import com.juanweather.data.models.SOSSettings
import kotlinx.coroutines.flow.Flow

class SOSRepository(
    private val sosSettingsDao: SOSSettingsDao
) {

    fun getSettings(): Flow<SOSSettings?> = sosSettingsDao.getSettings()

    suspend fun getSettingsOnce(): SOSSettings? = sosSettingsDao.getSettingsOnce()

    suspend fun updateSettings(settings: SOSSettings) {
        sosSettingsDao.updateSettings(settings)
    }

    suspend fun insertSettings(settings: SOSSettings) {
        sosSettingsDao.insertSettings(settings)
    }

    suspend fun deleteAllSettings() {
        sosSettingsDao.deleteAllSettings()
    }

    suspend fun updateLocationSharing(enabled: Boolean) {
        val currentSettings = getSettingsOnce() ?: SOSSettings()
        updateSettings(currentSettings.copy(enableLocationSharing = enabled))
    }

    suspend fun updateMessageTemplate(message: String) {
        val currentSettings = getSettingsOnce() ?: SOSSettings()
        updateSettings(currentSettings.copy(messageTemplate = message))
    }

    suspend fun updateLastSentTime() {
        val currentSettings = getSettingsOnce() ?: SOSSettings()
        updateSettings(currentSettings.copy(lastSentTime = System.currentTimeMillis()))
    }
}
