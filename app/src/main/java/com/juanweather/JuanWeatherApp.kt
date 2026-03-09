package com.juanweather

import android.app.Application
import com.juanweather.data.local.AppDatabase
import com.juanweather.data.models.User
import com.juanweather.data.repository.UserRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class JuanWeatherApp : Application() {

    val database by lazy { AppDatabase.getInstance(this) }
    val userRepository by lazy { UserRepository(database.userDao()) }

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
