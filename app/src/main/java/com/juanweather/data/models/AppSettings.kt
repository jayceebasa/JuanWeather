package com.juanweather.data.models

data class AppSettings(
    val temperatureUnit: String = "C", // C or F
    val windSpeedUnit: String = "km/h", // km/h or mph
    val pressureUnit: String = "mb", // mb or inHg
    val visibilityUnit: String = "km", // km or mi
    val notificationsEnabled: Boolean = true,
    val theme: String = "light", // light or dark
    val language: String = "en"
)
