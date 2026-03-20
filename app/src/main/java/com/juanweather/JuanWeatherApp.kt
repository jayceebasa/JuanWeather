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
    val preferencesHelper by lazy { PreferencesHelper(this) }
    val settingsRepository by lazy { SettingsRepository(appSettingsDao) }
    val hybridEmergencyContactRepository by lazy { HybridEmergencyContactRepository(emergencyContactDao) }

    companion object {
        lateinit var instance: JuanWeatherApp
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        seedAdminAccount()
    }

    // Seed the admin account on a background thread every app start.
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
}
