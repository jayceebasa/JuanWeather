package com.juanweather.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_settings")
data class AppSettings(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val userId: Int = 0,                    // FK to User.id — each setting belongs to a user
    val temperatureUnit: String = "C", // C or F
    val windSpeedUnit: String = "km/h", // km/h or mph
    val pressureUnit: String = "mb", // mb or inHg
    val visibilityUnit: String = "km", // km or mi
    val notificationsEnabled: Boolean = true,
    val theme: String = "light", // light or dark
    val language: String = "en"
)
