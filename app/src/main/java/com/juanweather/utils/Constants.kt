package com.juanweather.utils

object Constants {
    // API Keys
    const val WEATHER_API_KEY = "010b4a1c55cc416ba4b114408260903"
    const val WEATHER_BASE_URL = "https://api.weatherapi.com/v1/"

    // Preferences
    const val PREFERENCES_NAME = "juanweather_prefs"
    const val PREF_TEMP_UNIT = "temp_unit"
    const val PREF_WIND_UNIT = "wind_unit"
    const val PREF_THEME = "theme"

    // Default Values
    const val DEFAULT_TEMP_UNIT = "C"
    const val DEFAULT_WIND_UNIT = "km/h"
    const val DEFAULT_THEME = "light"

    // Weather Icons (map condition to drawable)
    val WEATHER_ICONS = mapOf(
        "clear_sky" to android.R.drawable.ic_menu_view,
        "rain" to android.R.drawable.ic_menu_view,
        "clouds" to android.R.drawable.ic_menu_view
    )
}
