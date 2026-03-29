package com.juanweather.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sos_settings")
data class SOSSettings(
    @PrimaryKey
    val id: String = "sos_settings",
    val enableLocationSharing: Boolean = false,
    val messageTemplate: String = "I need help. This is an emergency SOS alert from JuanWeather.",
    val lastSentTime: Long = 0
)
