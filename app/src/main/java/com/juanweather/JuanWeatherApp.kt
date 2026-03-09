package com.juanweather

import android.app.Application
import com.juanweather.data.local.AppDatabase
import com.juanweather.data.repository.UserRepository

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
    }
}
