package com.juanweather.ui.models

/**
 * Data classes for the Weather Dashboard
 */

data class HourlyForecastItem(
    val time: String,
    val iconType: String, // "sun", "cloud", "rain", "drizzle"
    val temperature: String
)

data class DailyForecastItem(
    val day: String,
    val iconType: String // "sun", "cloud", "rain", "drizzle"
)

data class Metric(
    val label: String,
    val value: String
)
