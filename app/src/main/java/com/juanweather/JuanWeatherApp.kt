package com.juanweather

import android.app.Application

class JuanWeatherApp : Application() {

    companion object {
        lateinit var instance: JuanWeatherApp
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }
}
