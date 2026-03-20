package com.juanweather

import android.app.Application
import com.juanweather.data.local.AppDatabase
import com.juanweather.data.local.PreferencesHelper
import com.juanweather.data.models.User
import com.juanweather.data.remote.ApiClient
import com.juanweather.data.repository.UserRepository
import com.juanweather.data.repository.WeatherRepository
import com.juanweather.data.repository.SettingsRepository
import com.juanweather.data.repository.HybridEmergencyContactRepository
import com.juanweather.data.repository.SOSRepository
import com.juanweather.data.repository.HybridSOSRepository
import com.juanweather.utils.TwilioConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class JuanWeatherApp : Application() {

    val database by lazy { AppDatabase.getInstance(this) }
    val userRepository by lazy { UserRepository(database.userDao()) }
    val weatherRepository by lazy { WeatherRepository(ApiClient.getWeatherService()) }
    val userLocationDao by lazy { database.userLocationDao() }
    val appSettingsDao by lazy { database.appSettingsDao() }
    val emergencyContactDao by lazy { database.emergencyContactDao() }
    val sosSettingsDao by lazy { database.sosSettingsDao() }
    val preferencesHelper by lazy { PreferencesHelper(this) }
    val settingsRepository by lazy { SettingsRepository(appSettingsDao) }
    val hybridEmergencyContactRepository by lazy { HybridEmergencyContactRepository(emergencyContactDao) }
    val hybridSOSRepository by lazy { HybridSOSRepository(sosSettingsDao) }
    val twilioConfig by lazy { TwilioConfig(this) }

    companion object {
        lateinit var instance: JuanWeatherApp
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        seedAdminAccount()
        initializeTwilio()
    }

    // This initializes Twilio credentials from encrypted storage
    // Credentials can be set via twilioConfig.saveCredentials() after user provides them
    private fun initializeTwilio() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Check if credentials already exist in encrypted storage
                if (twilioConfig.isConfigured()) {
                    // Credentials already set, nothing to do
                    return@launch
                }
            } catch (e: Exception) {
                // First time setup or error reading - safe to ignore
            }
        }
    }

    // INSERT OR IGNORE means it only ever inserts once — safe to call repeatedly.
    private fun seedAdminAccount() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val dao = database.userDao()
                val existing = dao.getUserByEmail("admin")
                if (existing == null) {
                    dao.insertUser(
                        User(
                            name = "Administrator",
                            email = "admin",
                            password = "admin123",
                            role = "admin"
                        )
                    )
                }
            } catch (e: Exception) {
                // Admin may already exist — safe to ignore
            }
        }
    }

    /**
     * Configure Twilio credentials with Messaging Service and phone number
     * @param accountSid Your Twilio Account SID
     * @param authToken Your Twilio Auth Token
     * @param messagingServiceSid Your Twilio Messaging Service SID
     * @param phoneNumber Your Twilio phone number (for "From" field)
     */
    fun configureTwilio(accountSid: String, authToken: String, messagingServiceSid: String, phoneNumber: String = "") {
        twilioConfig.saveCredentials(accountSid, authToken, messagingServiceSid, phoneNumber)
    }

    /**
     * Get Twilio configuration status
     */
    fun isTwilioConfigured(): Boolean {
        return twilioConfig.isConfigured()
    }

    /**
     * Clear Twilio configuration (e.g., on logout)
     */
    fun clearTwilioConfig() {
        twilioConfig.clearCredentials()
    }
}
